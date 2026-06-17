package com.aionemu.gameserver.network.aion.serverpackets;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import sun.misc.Unsafe;

import com.aionemu.gameserver.controllers.NpcController;
import com.aionemu.gameserver.controllers.movement.MovementMask;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.HouseData;
import com.aionemu.gameserver.dataholders.ItemData;
import com.aionemu.gameserver.dataholders.ItemRestrictionCleanupData;
import com.aionemu.gameserver.dataholders.NpcSkillData;
import com.aionemu.gameserver.dataholders.QuestsData;
import com.aionemu.gameserver.dataholders.SkillData;
import com.aionemu.gameserver.dataholders.TradeListData;
import com.aionemu.gameserver.dataholders.WorldMapsData;
import com.aionemu.gameserver.model.CreatureType;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.StorageType;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.model.templates.item.enums.ItemGroup;
import com.aionemu.gameserver.services.item.ItemPacketService.ItemAddType;
import com.aionemu.gameserver.services.item.ItemPacketService.ItemUpdateType;
import com.aionemu.gameserver.model.animations.TeleportAnimation;
import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.templates.VisibleObjectTemplate;
import com.aionemu.gameserver.model.templates.gather.GatherableTemplate;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.world.WorldPosition;
import com.aionemu.gameserver.model.gameobjects.Persistable.PersistentState;
import com.aionemu.gameserver.model.account.PlayerAccountData;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.items.PendingTuneResult;
import com.aionemu.gameserver.model.skill.PlayerSkillEntry;
import com.aionemu.gameserver.model.items.ChargeInfo;
import com.aionemu.gameserver.model.items.GodStone;
import com.aionemu.gameserver.model.items.ItemMask;
import com.aionemu.gameserver.model.items.ManaStone;
import com.aionemu.gameserver.model.templates.BoundRadius;
import com.aionemu.gameserver.model.templates.QuestTemplate;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.model.templates.npc.NpcTemplateType;
import com.aionemu.gameserver.model.templates.quest.QuestExtraCategory;
import com.aionemu.gameserver.model.templates.spawns.SpawnGroup;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.model.templates.stats.StatsTemplate;
import com.aionemu.gameserver.model.templates.tradelist.TradeListTemplate;
import com.aionemu.gameserver.model.templates.tradelist.TradeListTemplate.TradeTab;
import com.aionemu.gameserver.model.templates.tradelist.TradeNpcType;
import com.aionemu.gameserver.model.templates.world.WorldMapTemplate;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;

/**
 * INTEGRATION golden harness for the World-reading SM_* family — increment 1 of the deferred integration-harness
 * sub-project (docs/next-slop-targets.md). This is the FIRST golden seam that drives a packet through the
 * DataManager.WORLD_MAPS_DATA world-map holder rather than only scalar/ctor state. It serializes
 * SM_TELEPORT_LOC.writeImpl byte-for-byte; the C# asserter (GoldenWorldPacketFixtureTests) rebuilds the
 * structurally identical WorldMapsData holder + packet and asserts identical bytes. Java is the single source of truth.
 *
 * <p>Seam: DataManager.WORLD_MAPS_DATA is reflectively populated with exactly the two map templates SM_TELEPORT_LOC
 * reads (one non-instance, one instance). SM_TELEPORT_LOC.writeImpl is pure scalar; its ONLY non-ctor read is the
 * ctor's DataManager.WORLD_MAPS_DATA.getTemplate(mapId).isInstance() branch, which selects between writing the
 * instanceId (instance map) or the mapId (regular map) for the channel field. No live World/WorldMapInstance/
 * ZoneService/Player is needed — this is the bounded WORLD_MAPS_DATA holder seam, deliberately NOT the live
 * World.getInstance() graph (that requires the heavier integration harness; see the report for the blocker).</p>
 *
 * Regenerate with:
 *   mvn -pl game-server -am test -Dtest=GoldenWorldPacketFixtureGeneratorTest -Dmaven.test.skip=false -Dsurefire.failIfNoSpecifiedTests=false
 */
public class GoldenWorldPacketFixtureGeneratorTest {

	private static final char[] HEX = "0123456789ABCDEF".toCharArray();

	// The two map ids the SM_TELEPORT_LOC fixture exercises (identical on both sides). One regular (non-instance)
	// world and one instance world, so the ctor's WORLD_MAPS_DATA.getTemplate(mapId).isInstance() branch is covered
	// both ways. Morheim (220020000) is a regular field map; Draupnir Cave (320080000) is an instance.
	private static final int REGULAR_MAP_ID = 220020000;
	private static final int INSTANCE_MAP_ID = 320080000;

	@Test
	public void generateGoldenWorldPacketFixtures() throws IOException {
		Path outDir = repoRoot().resolve("parity-artifacts/golden/packets");
		Files.createDirectories(outDir);

		installWorldMapsSeam();

		List<Case> cases = new ArrayList<>();
		// Regular (non-instance) map -> writeImpl writes mapId for the channel field.
		cases.add(teleportCase("teleportRegularMap", REGULAR_MAP_ID, 5, 1500.5f, 2500.25f, 412.125f, (byte) 30,
			TeleportAnimation.FADE_OUT));
		// Instance map -> writeImpl writes instanceId for the channel field.
		cases.add(teleportCase("teleportInstanceMap", INSTANCE_MAP_ID, 7, 100.5f, 200.25f, 300.75f, (byte) 90,
			TeleportAnimation.JUMP_IN_GATE));
		// Regular map with the NONE animation + instanceId 0 (the common bind/obelisk teleport shape).
		cases.add(teleportCase("teleportRegularNoneAnim", REGULAR_MAP_ID, 0, 0.0f, 0.0f, 0.0f, (byte) 0,
			TeleportAnimation.NONE));

		writeFixture(outDir.resolve("SM_TELEPORT_LOC.json"), "SM_TELEPORT_LOC", cases);
	}

	// The skill id + cooldown (raw cooldown attr, *100 = duration millis on the wire) the SM_SKILL_COOLDOWN fixture
	// reads from the SKILL_DATA holder. Identical on both sides.
	private static final int COOLDOWN_SKILL_ID = 1968;
	private static final int COOLDOWN_RAW = 250; // getCooldown() raw; writeImpl writes cooldown*100 = 25000

	@Test
	public void generateGoldenSkillCooldownFixture() throws IOException {
		Path outDir = repoRoot().resolve("parity-artifacts/golden/packets");
		Files.createDirectories(outDir);

		installSkillDataSeam();

		List<Case> cases = new ArrayList<>();
		// Scalar ctor (skillId, expirationTimeMillis). expiration 0 -> getRemainingSeconds()==0 (no System.currentTimeMillis),
		// so the packet is deterministic; getDurationMillis() reads SKILL_DATA.getSkillTemplate(skillId).getCooldown()*100.
		cases.add(skillCooldownCase("skillCooldownZeroExpiration", COOLDOWN_SKILL_ID, 0L));

		writeFixture(outDir.resolve("SM_SKILL_COOLDOWN.json"), "SM_SKILL_COOLDOWN", cases);
	}

	private static Case skillCooldownCase(String name, int skillId, long expirationTimeMillis) {
		String inputs = "{\"skillId\":" + skillId + ",\"expirationTimeMillis\":" + expirationTimeMillis + "}";
		return new Case(name, inputs, capture(new SM_SKILL_COOLDOWN(skillId, expirationTimeMillis), null));
	}

	// The quest ids the SM_QUEST_ACTION fixture reads from the QUEST_DATA holder. One NONE-category quest (full
	// payload written) and one extra-category quest (writeImpl early-returns -> empty payload). Identical both sides.
	private static final int QUEST_ID_NONE = 1006;
	private static final int QUEST_ID_EXTRA = 1007;

	@Test
	public void generateGoldenQuestActionFixture() throws IOException {
		Path outDir = repoRoot().resolve("parity-artifacts/golden/packets");
		Files.createDirectories(outDir);

		installQuestDataSeam();

		List<Case> cases = new ArrayList<>();
		// Scalar ctors only (no live QuestState). UNK / TIMER / SHARE branches + the extra-category early-return.
		cases.add(questActionUnkCase("questActionUnk", QUEST_ID_NONE));
		cases.add(questActionTimerCase("questActionTimer", QUEST_ID_NONE, 900));
		cases.add(questActionShareCase("questActionShareAlliance", QUEST_ID_NONE, 1234, true));
		cases.add(questActionShareCase("questActionShareGroup", QUEST_ID_NONE, 1234, false));
		// Extra-category quest: questTemplate.getExtraCategory() != NONE -> writeImpl returns early (empty payload).
		cases.add(questActionUnkCase("questActionExtraCategoryEmpty", QUEST_ID_EXTRA));

		writeFixture(outDir.resolve("SM_QUEST_ACTION.json"), "SM_QUEST_ACTION", cases);
	}

	private static Case questActionUnkCase(String name, int questId) {
		String inputs = "{\"ctor\":\"unk\",\"questId\":" + questId + "}";
		return new Case(name, inputs, capture(new SM_QUEST_ACTION(questId), null));
	}

	private static Case questActionTimerCase(String name, int questId, int timer) {
		String inputs = "{\"ctor\":\"timer\",\"questId\":" + questId + ",\"timer\":" + timer + "}";
		return new Case(name, inputs, capture(new SM_QUEST_ACTION(questId, timer), null));
	}

	private static Case questActionShareCase(String name, int questId, int sharerId, boolean shareInAlliance) {
		String inputs = "{\"ctor\":\"share\",\"questId\":" + questId + ",\"sharerId\":" + sharerId
			+ ",\"shareInAlliance\":" + shareInAlliance + "}";
		return new Case(name, inputs, capture(new SM_QUEST_ACTION(questId, sharerId, shareInAlliance), null));
	}

	// ---- batch 15: SM_GATHERABLE_INFO (real VisibleObject seam) + SM_QUEST_COMPLETED_LIST (QUEST_DATA holder seam) ----

	private static final int GATHER_OBJECT_ID = 0x12345678;
	private static final int GATHER_STATIC_ID = 7700;
	private static final int GATHER_TEMPLATE_ID = 700001;
	private static final int GATHER_L10N_ID = 350123;
	private static final float GATHER_X = 1234.5f;
	private static final float GATHER_Y = 678.25f;
	private static final float GATHER_Z = 91.5f;
	private static final byte GATHER_HEADING = 42;
	private static final int GATHER_MAP_ID = 210010000;

	// Quest ids for SM_QUEST_COMPLETED_LIST. Both NON-time-based (repeatCycle == null -> isTimeBased() false ->
	// canRepeat() never reads the clock). REPEATABLE has maxRepeatCount 255 -> canRepeat() == true (writeC 0);
	// EXHAUSTED has completeCount(2) >= maxRepeatCount(1) && != 255 -> canRepeat() == false (writeC 1).
	private static final int QC_REPEATABLE_ID = 2001;
	private static final int QC_REPEATABLE_MAX = 255;
	private static final int QC_REPEATABLE_COUNT = 3;
	private static final int QC_EXHAUSTED_ID = 2002;
	private static final int QC_EXHAUSTED_MAX = 1;
	private static final int QC_EXHAUSTED_COUNT = 2;

	/**
	 * SM_GATHERABLE_INFO reads ONLY the VisibleObject's scalar/template state: getX/Y/Z (WorldPosition), getObjectId(),
	 * getSpawn().getStaticId()/getHeading() (SpawnTemplate), getObjectTemplate().getTemplateId()/getL10nId(). The object
	 * is NOT a StaticDoor -> writeH(1). No con, no live World/DataManager/time. Built through a minimal concrete
	 * VisibleObject (the established harness precedent — like HarnessAttackCreature) so the ctor pulls in no
	 * DataManager/IDFactory/KnownList cascade. Identical both sides; Java is the oracle.
	 *
	 * mvn -q -pl game-server -am test -Dtest=GoldenWorldPacketFixtureGeneratorTest#generateGoldenGatherableInfoFixture -Dmaven.test.skip=false -Dsurefire.failIfNoSpecifiedTests=false
	 */
	@Test
	public void generateGoldenGatherableInfoFixture() throws Exception {
		Path outDir = repoRoot().resolve("parity-artifacts/golden/packets");
		Files.createDirectories(outDir);

		VisibleObject obj = buildHarnessGatherable();

		List<Case> cases = new ArrayList<>();
		String inputs = "{\"objectId\":" + GATHER_OBJECT_ID + ",\"staticId\":" + GATHER_STATIC_ID + ",\"templateId\":"
			+ GATHER_TEMPLATE_ID + ",\"l10nId\":" + GATHER_L10N_ID + ",\"x\":" + GATHER_X + ",\"y\":" + GATHER_Y + ",\"z\":"
			+ GATHER_Z + ",\"heading\":" + GATHER_HEADING + "}";
		cases.add(new Case("gatherable", inputs, capture(new SM_GATHERABLE_INFO(obj), null)));

		writeFixture(outDir.resolve("SM_GATHERABLE_INFO.json"), "SM_GATHERABLE_INFO", cases);
	}

	/** Minimal concrete (non-StaticDoor) VisibleObject carrying the position / spawn / template the packet reads. */
	private static VisibleObject buildHarnessGatherable() throws Exception {
		GatherableTemplate template = new GatherableTemplate();
		setField(template, "id", GATHER_TEMPLATE_ID);
		setField(template, "nameId", GATHER_L10N_ID);
		SpawnGroup spawnGroup = new SpawnGroup(GATHER_MAP_ID, 0, 0, null);
		SpawnTemplate spawn = new SpawnTemplate(spawnGroup, GATHER_X, GATHER_Y, GATHER_Z, GATHER_HEADING, 0, null,
			GATHER_STATIC_ID);
		WorldPosition position = new WorldPosition(GATHER_MAP_ID, GATHER_X, GATHER_Y, GATHER_Z, GATHER_HEADING);
		VisibleObject obj = new VisibleObject(GATHER_OBJECT_ID, null, spawn, (VisibleObjectTemplate) template, position, false) {
		};
		return obj;
	}

	/**
	 * SM_QUEST_COMPLETED_LIST writeImpl: writeC(1) writeC(updateMode) writeH(-size & 0xFFFF) + per QuestState
	 * writeD(getQuestId()) writeC(min(completeCount,255)) writeC(canRepeat() ? 0 : 1). canRepeat() reads
	 * DataManager.QUEST_DATA.getQuestById(questId) -> the bounded QUEST_DATA holder seam (both quests NON-time-based so
	 * no clock). Quests chosen to cover both canRepeat branches. Identical both sides; Java is the oracle.
	 *
	 * mvn -q -pl game-server -am test -Dtest=GoldenWorldPacketFixtureGeneratorTest#generateGoldenQuestCompletedListFixture -Dmaven.test.skip=false -Dsurefire.failIfNoSpecifiedTests=false
	 */
	@Test
	public void generateGoldenQuestCompletedListFixture() throws Exception {
		Path outDir = repoRoot().resolve("parity-artifacts/golden/packets");
		Files.createDirectories(outDir);

		installQuestCompletedDataSeam();

		java.util.List<QuestState> states = new ArrayList<>();
		states.add(questState(QC_REPEATABLE_ID, QC_REPEATABLE_COUNT)); // canRepeat true -> writeC 0
		states.add(questState(QC_EXHAUSTED_ID, QC_EXHAUSTED_COUNT));   // canRepeat false -> writeC 1

		List<Case> cases = new ArrayList<>();
		String inputs = "{\"updateMode\":0,\"quests\":[[" + QC_REPEATABLE_ID + "," + QC_REPEATABLE_COUNT + ",0],["
			+ QC_EXHAUSTED_ID + "," + QC_EXHAUSTED_COUNT + ",1]]}";
		cases.add(new Case("rewriteAll", inputs, capture(new SM_QUEST_COMPLETED_LIST(0, states), null)));
		String inputsIns = "{\"updateMode\":1,\"quests\":[[" + QC_REPEATABLE_ID + "," + QC_REPEATABLE_COUNT + ",0],["
			+ QC_EXHAUSTED_ID + "," + QC_EXHAUSTED_COUNT + ",1]]}";
		cases.add(new Case("insert", inputsIns, capture(new SM_QUEST_COMPLETED_LIST(1, states), null)));

		writeFixture(outDir.resolve("SM_QUEST_COMPLETED_LIST.json"), "SM_QUEST_COMPLETED_LIST", cases);
	}

	/** QuestState with the COMPLETE status + reflect-set completeCount (the only fields the packet reads). */
	private static QuestState questState(int questId, int completeCount) throws Exception {
		QuestState qs = new QuestState(questId, QuestStatus.COMPLETE, 0, 0, completeCount, null, null, null);
		return qs;
	}

	/** Populate DataManager.QUEST_DATA with the two NON-time-based quest templates SM_QUEST_COMPLETED_LIST reads. */
	private void installQuestCompletedDataSeam() {
		try {
			QuestsData data = new QuestsData();
			@SuppressWarnings("unchecked")
			Map<Integer, QuestTemplate> byId = (Map<Integer, QuestTemplate>) getField(data, "questTemplates");
			byId.clear();
			byId.put(QC_REPEATABLE_ID, questCompletedTemplate(QC_REPEATABLE_ID, QC_REPEATABLE_MAX));
			byId.put(QC_EXHAUSTED_ID, questCompletedTemplate(QC_EXHAUSTED_ID, QC_EXHAUSTED_MAX));
			DataManager.QUEST_DATA = data;
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Failed to install QUEST_DATA seam", e);
		}
	}

	/** QuestTemplate with reflect-set id + maxRepeatCount; repeatCycle left null -> isTimeBased() false (no clock). */
	private static QuestTemplate questCompletedTemplate(int id, int maxRepeatCount) throws ReflectiveOperationException {
		Constructor<QuestTemplate> ctor = QuestTemplate.class.getDeclaredConstructor();
		ctor.setAccessible(true);
		QuestTemplate t = ctor.newInstance();
		setField(t, "id", id);
		setField(t, "maxRepeatCount", maxRepeatCount);
		return t;
	}

	// ---- live-Npc object seam (SM_TRADE_IN_LIST) ----

	// The live Npc objectId + the trade-in (sellback) list the SM_TRADE_IN_LIST fixture reads. Identical both sides.
	// SM_TRADE_IN_LIST.writeImpl reads ONLY npc.getObjectId() from the live object (no template/stats/AI/World), plus
	// the directly-constructed TradeListTemplate scalars — so the bounded live-Npc object is built WITHOUT the heavy
	// Npc(controller,spawn,template) ctor (which would pull in setupStatContainers -> NpcGameStats/NpcLifeStats/AI):
	// it is allocated uninitialized (Unsafe.allocateInstance, the established harness precedent) with only the final
	// AionObject.objectId pinned. This is the FIRST golden seam that drives a packet through a live Npc game-object.
	private static final int TRADE_NPC_OBJECT_ID = 700123;
	private static final int TRADE_LIST_NPC_ID = 798001;

	@Test
	public void generateGoldenTradeInListFixture() throws IOException {
		Path outDir = repoRoot().resolve("parity-artifacts/golden/packets");
		Files.createDirectories(outDir);

		Npc npc = newUninitializedNpc(TRADE_NPC_OBJECT_ID);

		List<Case> cases = new ArrayList<>();
		// Full list: NORMAL type (index 1), 3 trade tabs -> full payload (objectId + type + modifiers + count + tab ids).
		cases.add(tradeInListCase("tradeInListNormal", npc, TRADE_LIST_NPC_ID, TradeNpcType.NORMAL, 80,
			new int[] { 1, 2, 3 }));
		// Different npc type (ABYSS -> index 2) + a single tab + a different buy modifier.
		cases.add(tradeInListCase("tradeInListAbyssSingleTab", npc, TRADE_LIST_NPC_ID, TradeNpcType.ABYSS, 100,
			new int[] { 42 }));
		// Guard: count == 0 (empty tab list) -> writeImpl early-returns (empty payload).
		cases.add(tradeInListCase("tradeInListEmptyCount", npc, TRADE_LIST_NPC_ID, TradeNpcType.NORMAL, 80, new int[0]));
		// Guard: npcId == 0 -> writeImpl early-returns (empty payload).
		cases.add(tradeInListCase("tradeInListZeroNpcId", npc, 0, TradeNpcType.NORMAL, 80, new int[] { 1 }));

		writeFixture(outDir.resolve("SM_TRADE_IN_LIST.json"), "SM_TRADE_IN_LIST", cases);
	}

	private static Case tradeInListCase(String name, Npc npc, int npcId, TradeNpcType type, int buyPriceModifier,
			int[] tabIds) {
		StringBuilder tabs = new StringBuilder("[");
		for (int i = 0; i < tabIds.length; i++)
			tabs.append(i > 0 ? "," : "").append(tabIds[i]);
		tabs.append("]");
		String inputs = "{\"objectId\":" + npc.getObjectId() + ",\"npcId\":" + npcId + ",\"npcType\":\"" + type.name()
			+ "\",\"buyPriceModifier\":" + buyPriceModifier + ",\"tabIds\":" + tabs + "}";
		TradeListTemplate tlist = tradeListTemplate(npcId, type, tabIds);
		return new Case(name, inputs, capture(new SM_TRADE_IN_LIST(npc, tlist, buyPriceModifier), null));
	}

	private static TradeListTemplate tradeListTemplate(int npcId, TradeNpcType type, int[] tabIds) {
		try {
			TradeListTemplate t = new TradeListTemplate();
			setField(t, "npcId", npcId);
			setField(t, "tradeNpcType", type);
			List<TradeTab> tabs = new ArrayList<>();
			for (int id : tabIds)
				tabs.add(tradeTab(id));
			setField(t, "tradeTablist", tabs);
			return t;
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Failed to build TradeListTemplate", e);
		}
	}

	private static TradeTab tradeTab(int id) throws ReflectiveOperationException {
		Constructor<TradeTab> ctor = TradeTab.class.getDeclaredConstructor();
		ctor.setAccessible(true);
		TradeTab tab = ctor.newInstance();
		setField(tab, "id", id);
		return tab;
	}

	/**
	 * Allocate an Npc WITHOUT running any constructor (Unsafe.allocateInstance — the established harness precedent for
	 * AionConnection/AbyssRank), then pin only the final AionObject.objectId. SM_TRADE_IN_LIST reads nothing else from
	 * the live object, so no template/stats/AI/World graph is needed.
	 */
	private static Npc newUninitializedNpc(int objectId) {
		try {
			Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
			theUnsafe.setAccessible(true);
			Unsafe unsafe = (Unsafe) theUnsafe.get(null);
			Npc npc = (Npc) unsafe.allocateInstance(Npc.class);
			Field idField = AionObject.class.getDeclaredField("objectId");
			idField.setAccessible(true);
			idField.set(npc, objectId);
			return npc;
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Failed to allocate uninitialized Npc", e);
		}
	}

	// ---- real-Npc-ctor object seam (SM_NPC_INFO) ----

	// The real-Npc-ctor seam: SM_NPC_INFO is the maximal Npc-reading packet. Unlike SM_TRADE_IN_LIST (objectId only),
	// its writeImpl reads the live Npc's stat containers (getLifeStats().getHpPercentage(), getGameStats().getMaxHp(),
	// getGameStats().getMovementSpeedFloat()), the move controller (getTargetX2/Y2/Z2/getMovementMask), the
	// NpcTemplate (templateId/l10nId/titleId/boundRadius/height/attackSpeed/level/npcTemplateType) and TownService.
	// So this drives a REAL Npc(controller, spawn, template) ctor — the bounded next live-Npc increment.
	//
	// Why the real ctor is bounded (no World/Knownlist/SkillEngine/DataManager-cascade):
	//   * Npc ctor -> Creature ctor -> AIEngine.newAI(aiName, this). With BOTH NpcTemplate.ai == null AND
	//     SpawnTemplate.aiName == null, newAI(null) returns a DummyAI (no AIEngine registration needed). DummyAI's
	//     modifyOwnerStat(Stat2) is the AbstractAI base no-op.
	//   * setupStatContainers() -> NpcGameStats + NpcLifeStats. NpcLifeStats ctor eagerly reads
	//     getGameStats().getMaxHp().getCurrent() -> CreatureGameStats.getStat(MAXHP, statsTemplate.getMaxHp()).
	//     The stats function map is empty (no effects), so getStat returns the raw base value (no StatCapUtil pass,
	//     no time, no random) and DummyAI.modifyOwnerStat is a no-op. So a populated StatsTemplate.maxHp is all that's
	//     needed; movementSpeed reads statsTemplate.getRunSpeed() which is 0 when speeds == null (deterministic).
	//   * NpcSkillList(this) reads DataManager.NPC_SKILL_DATA.getNpcSkillList(npcId); an empty holder returns null ->
	//     empty skill list. TownService.getInstance().getTownIdByPosition(npc) returns 0 (npc not spawned, plain
	//     SpawnTemplate), but the singleton ctor reads DataManager.HOUSE_DATA.getLands() -> seed an empty HouseData.
	//   * objectId comes from IDFactory.nextId() (non-deterministic) -> overwrite the final AionObject.objectId field
	//     with a pinned value after construction (mirrored on the C# side).
	//   * getType(player) is computed in the ctor; pinning the npc.type field makes it deterministic and lets the
	//     player arg be null (TribeRelationService never reached). isFlag()==true (FLAG template type) makes the
	//     isNewSpawn() time-dependent byte unreachable (writeC writes 0x13).
	private static final int NPC_INFO_OBJECT_ID = 740555;
	private static final int NPC_INFO_NPC_ID = 215220; // npc_id / template id (hp-gauge + appearance refs)
	private static final int NPC_INFO_WORLD_ID = 220020000;
	private static final int NPC_INFO_NAME_ID = 350123; // l10nId
	private static final int NPC_INFO_TITLE_ID = 4242;
	private static final byte NPC_INFO_LEVEL = (byte) 55;
	private static final int NPC_INFO_MAX_HP = 123456;
	private static final int NPC_INFO_ATTACK_SPEED = 1500;
	private static final float NPC_INFO_HEIGHT = 1.75f;
	private static final float NPC_INFO_BR_FRONT = 1.25f;
	private static final float NPC_INFO_BR_SIDE = 0.95f;
	private static final float NPC_INFO_BR_UPPER = 2.5f;
	private static final float NPC_INFO_X = 1450.5f;
	private static final float NPC_INFO_Y = 1602.25f;
	private static final float NPC_INFO_Z = 250.125f;
	private static final byte NPC_INFO_HEADING = (byte) 60;

	@Test
	public void generateGoldenNpcInfoFixture() throws IOException {
		Path outDir = repoRoot().resolve("parity-artifacts/golden/packets");
		Files.createDirectories(outDir);

		installDbStub();
		installHouseDataSeam();
		installNpcSkillDataSeam();
		ensureIdFactory();

		List<Case> cases = new ArrayList<>();
		// PEACE-type FLAG npc: isFlag()==true -> the isNewSpawn() time-dependent byte is unreachable (writeC 0x13).
		cases.add(npcInfoCase("npcInfoPeaceFlag", CreatureType.PEACE));
		// ATTACKABLE-type FLAG npc: same packet shape, different creatureType id byte.
		cases.add(npcInfoCase("npcInfoAttackableFlag", CreatureType.ATTACKABLE));

		writeFixture(outDir.resolve("SM_NPC_INFO.json"), "SM_NPC_INFO", cases);
	}

	private static Case npcInfoCase(String name, CreatureType type) {
		Npc npc = buildRealNpc(NPC_INFO_OBJECT_ID, type);
		String inputs = "{\"objectId\":" + NPC_INFO_OBJECT_ID + ",\"npcId\":" + NPC_INFO_NPC_ID + ",\"worldId\":"
			+ NPC_INFO_WORLD_ID + ",\"nameId\":" + NPC_INFO_NAME_ID + ",\"titleId\":" + NPC_INFO_TITLE_ID + ",\"level\":"
			+ NPC_INFO_LEVEL + ",\"maxHp\":" + NPC_INFO_MAX_HP + ",\"attackSpeed\":" + NPC_INFO_ATTACK_SPEED
			+ ",\"height\":" + NPC_INFO_HEIGHT + ",\"brFront\":" + NPC_INFO_BR_FRONT + ",\"brSide\":" + NPC_INFO_BR_SIDE
			+ ",\"brUpper\":" + NPC_INFO_BR_UPPER + ",\"x\":" + NPC_INFO_X + ",\"y\":" + NPC_INFO_Y + ",\"z\":"
			+ NPC_INFO_Z + ",\"heading\":" + NPC_INFO_HEADING + ",\"creatureType\":\"" + type.name() + "\"}";
		// player arg is null: npc.type is pinned so getType(player) short-circuits without dereferencing the player.
		return new Case(name, inputs, capture(new SM_NPC_INFO(npc, null), null));
	}

	/**
	 * Build a REAL Npc through the full Npc(controller, spawn, template) ctor, then make it deterministic:
	 * overwrite the IDFactory-assigned objectId with a pinned value and pin the npc.type field (so getType(player)
	 * short-circuits). The template carries a populated StatsTemplate (maxHp) + FLAG type so the packet's
	 * stat/template reads are deterministic. Mirrored 1:1 on the C# asserter side.
	 */
	private static Npc buildRealNpc(int objectId, CreatureType type) {
		try {
			NpcTemplate template = buildNpcTemplate();
			SpawnGroup spawnGroup = new SpawnGroup(NPC_INFO_WORLD_ID, NPC_INFO_NPC_ID, 0, null);
			// staticId 0 -> SM_NPC_INFO writes getSpawn().getStaticId() == 0.
			SpawnTemplate spawn = new SpawnTemplate(spawnGroup, NPC_INFO_X, NPC_INFO_Y, NPC_INFO_Z, NPC_INFO_HEADING, 0,
				null, 0);
			NpcController controller = new NpcController();
			Npc npc = new Npc(controller, spawn, template);
			// Pin objectId (IDFactory-assigned is non-deterministic).
			Field idField = AionObject.class.getDeclaredField("objectId");
			idField.setAccessible(true);
			idField.set(npc, objectId);
			// Pin the npc.type field so getType(player) short-circuits (player can be null).
			Field typeField = Npc.class.getDeclaredField("type");
			typeField.setAccessible(true);
			typeField.set(npc, type);
			return npc;
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Failed to build real Npc", e);
		}
	}

	private static NpcTemplate buildNpcTemplate() throws ReflectiveOperationException {
		Constructor<NpcTemplate> ctor = NpcTemplate.class.getDeclaredConstructor();
		ctor.setAccessible(true);
		NpcTemplate t = ctor.newInstance();
		setField(t, "npcId", NPC_INFO_NPC_ID);
		setField(t, "nameId", NPC_INFO_NAME_ID);
		setField(t, "titleId", NPC_INFO_TITLE_ID);
		setField(t, "level", NPC_INFO_LEVEL);
		setField(t, "height", NPC_INFO_HEIGHT);
		setField(t, "attackSpeed", NPC_INFO_ATTACK_SPEED);
		setField(t, "npcTemplateType", NpcTemplateType.FLAG); // isFlag() == true -> deterministic spawn-flag byte
		// rating NORMAL -> getCongenitalSeeState() == NORMAL (id 0); needed by Npc.getSeeState() (SM_PLAYER_STATE) and
		// harmless to SM_NPC_INFO (which reads getVisualState() only, never getRating()).
		setField(t, "rating", com.aionemu.gameserver.model.templates.npc.NpcRating.NORMAL);
		setField(t, "boundRadius", new BoundRadius(NPC_INFO_BR_FRONT, NPC_INFO_BR_SIDE, NPC_INFO_BR_UPPER));
		setField(t, "statsTemplate", buildStatsTemplate());
		// ai left null -> DummyAI (no AIEngine registration needed).
		return t;
	}

	private static StatsTemplate buildStatsTemplate() throws ReflectiveOperationException {
		Constructor<StatsTemplate> ctor = StatsTemplate.class.getDeclaredConstructor();
		ctor.setAccessible(true);
		StatsTemplate s = ctor.newInstance();
		setField(s, "maxHp", NPC_INFO_MAX_HP);
		// speeds left null -> getRunSpeed() == 0 -> getMovementSpeedFloat() == 0.0f (deterministic).
		return s;
	}

	// ---- additional real-Npc-ctor packets (reuse the SM_NPC_INFO seam) ----
	//
	// Two more SM_* packets whose writeImpl reads ONLY the live Npc's scalar/base state (no live Player/World/Legion/
	// Summon beyond what the SM_NPC_INFO seam already stubs), so they reuse buildRealNpc(...) verbatim:
	//   * SM_MOVE      : objectId + x/y/z/heading (the un-spawned Npc's WorldPosition == 0) + movementMask; the
	//                    NpcMoveController is a plain CreatureMoveController (pmc==null) so the POSITION|MANUAL branch
	//                    writes getTargetX2/Y2/Z2 (TargetDest* default 0). No glide/vehicle bits set -> unreachable.
	//   * SM_SELL_ITEM : objectId + the vendor purchase TradeListTemplate (npc type/buy-rate/trade tabs) from a bounded
	//                    TRADE_LIST_DATA holder seam + the npc's CanSell/CanBuy/CanPurchase dialog-action flags + the
	//                    PricesService vendor-sell config default (when no purchase template -> NORMAL/getVendorSellModifier).
	// (SM_PLAYER_STATE/SM_SKILL_CANCEL/SM_EMOTION etc. are ALREADY golden'd via the PacketHarnessCreature harness — the
	//  Npc/Creature-reading family is otherwise exhausted; see docs/next-slop-targets.md.)

	@Test
	public void generateGoldenMoveFixture() throws IOException {
		Path outDir = repoRoot().resolve("parity-artifacts/golden/packets");
		Files.createDirectories(outDir);

		installDbStub();
		installHouseDataSeam();
		installNpcSkillDataSeam();
		ensureIdFactory();

		Npc npc = buildRealNpc(NPC_INFO_OBJECT_ID, CreatureType.PEACE);

		List<Case> cases = new ArrayList<>();
		// mask 0 -> only objectId + position + heading + mask byte (no position/glide/vehicle branch).
		cases.add(moveCase("moveMaskZero", npc, (byte) 0));
		// POSITION|MANUAL|ABSOLUTE -> pmc==null so the else branch writes getTargetX2/Y2/Z2 (TargetDest* default 0).
		byte posManualAbs = (byte) (MovementMask.POSITION | MovementMask.MANUAL | MovementMask.ABSOLUTE);
		cases.add(moveCase("movePositionManualAbsolute", npc, posManualAbs));

		writeFixture(outDir.resolve("SM_MOVE.json"), "SM_MOVE", cases);
	}

	private static Case moveCase(String name, Npc npc, byte movementMask) {
		String inputs = "{\"objectId\":" + NPC_INFO_OBJECT_ID + ",\"movementMask\":" + (movementMask & 0xFF) + "}";
		return new Case(name, inputs, capture(new SM_MOVE(npc, movementMask), null));
	}

	// SM_SELL_ITEM purchase-template scalars (== C# side). The npc template carries NO talkInfo -> SupportsAction(..)
	// is false -> canSell()/canBuy()/canPurchase() are all false (showBuyTab=showSellTab=0). A purchase template is
	// installed under the npc id so tradeList != null -> tradeNpcType/buyPriceRate/tabs come from the template (the
	// PricesService.getVendorSellModifier() config default is deliberately NOT reached, since config files aren't loaded).
	private static final int SELL_BUY_PRICE_RATE = 115;

	@Test
	public void generateGoldenSellItemFixture() throws IOException {
		Path outDir = repoRoot().resolve("parity-artifacts/golden/packets");
		Files.createDirectories(outDir);

		installDbStub();
		installHouseDataSeam();
		installNpcSkillDataSeam();
		ensureIdFactory();
		installTradeListDataSeam();

		Npc npc = buildRealNpc(NPC_INFO_OBJECT_ID, CreatureType.PEACE);

		List<Case> cases = new ArrayList<>();
		String inputs = "{\"objectId\":" + NPC_INFO_OBJECT_ID + ",\"npcId\":" + NPC_INFO_NPC_ID + ",\"npcType\":\"NORMAL\""
			+ ",\"buyPriceRate\":" + SELL_BUY_PRICE_RATE + ",\"tabIds\":[7,8],\"showBuyTab\":false,\"showSellTab\":false}";
		cases.add(new Case("sellItemPurchaseTemplate", inputs, capture(new SM_SELL_ITEM(npc), null)));

		writeFixture(outDir.resolve("SM_SELL_ITEM.json"), "SM_SELL_ITEM", cases);
	}

	/**
	 * Populate DataManager.TRADE_LIST_DATA with one purchase template (npcId NPC_INFO_NPC_ID, NORMAL type,
	 * SELL_BUY_PRICE_RATE, two trade tabs). Built WITHOUT JAXB by reflectively setting the npcPurchaseTemplateData index
	 * — the bounded holder seam (mirrors WORLD_MAPS/SKILL/QUEST seams). The tradelist/trade-in indices stay empty.
	 */
	private void installTradeListDataSeam() {
		try {
			TradeListData data = new TradeListData();
			@SuppressWarnings("unchecked")
			Map<Integer, TradeListTemplate> purchase = (Map<Integer, TradeListTemplate>) getField(data,
				"npcPurchaseTemplateData");
			purchase.clear();
			purchase.put(NPC_INFO_NPC_ID, purchaseTemplate(NPC_INFO_NPC_ID, SELL_BUY_PRICE_RATE, new int[] { 7, 8 }));
			DataManager.TRADE_LIST_DATA = data;
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Failed to install TRADE_LIST_DATA seam", e);
		}
	}

	private static TradeListTemplate purchaseTemplate(int npcId, int buyPriceRate, int[] tabIds)
			throws ReflectiveOperationException {
		TradeListTemplate t = new TradeListTemplate();
		setField(t, "npcId", npcId);
		setField(t, "tradeNpcType", TradeNpcType.NORMAL);
		setField(t, "buyPriceRate", buyPriceRate);
		List<TradeTab> tabs = new ArrayList<>();
		for (int id : tabIds)
			tabs.add(tradeTab(id));
		setField(t, "tradeTablist", tabs);
		return t;
	}

	// ---- item / ItemInfoBlob seam (SM_INVENTORY_UPDATE_ITEM) ----
	//
	// The FIRST golden seam that drives a packet through a live Item game-object + the ItemInfoBlob blob-writer family.
	// SM_INVENTORY_UPDATE_ITEM(player, item) defaults to ItemUpdateType.DEC_ITEM_USE -> ItemInfoBlob.getFullBlob(player,
	// item). The seam pins the item template to a NON-EQUIPPABLE group (ItemGroup.NONE -> getValidEquipmentSlots()==0,
	// isWeapon()/isArmor() false, isTwoHandWeapon() false) with no fusion / packCount 0 / not STIGMA_SHARD, so getFullBlob
	// adds EXACTLY ONE blob entry: GENERAL_INFO. That entry reads ONLY Item + ItemTemplate scalars
	// (getItemMask()->template.getMask(), getItemCount(), getItemCreator(), secondsUntilExpiration() [expireTime 0 -> 0,
	// no clock], getTemporaryExchangeTimeRemaining() [0], getItemId()->template.getTemplateId()) plus
	// DataManager.ITEM_CLEAN_UP.hasAccountOrLegionWhStorabilityDisabled(itemId) (empty bplist -> false). The host packet
	// additionally writes item.getObjectId() + template.getL10n() (ChatUtil.l10n(desc), pure scalar) + the DEC_ITEM_USE
	// mask. NO live Player deref (getFullBlob only stashes the player as blob owner; GENERAL_INFO never reads it), NO
	// World/Knownlist/stones/enchant/godstone cascade, NO DataManager beyond the bounded ITEM_CLEAN_UP holder seam.
	//
	// Why the simple Item(objId, template) ctor is deterministic: expireTime==0 (no System.currentTimeMillis), getEnchantType()==0
	// (isAmplified false), getImprovement()==null (calculateMaxChargeLevel 0 -> no ChargeInfo). maxTuneCount is pinned to 0
	// (mirrors what afterUnmarshal would set for a slot-0 item) so canTune() is false on BOTH sides identically.
	private static final int ITEM_OBJECT_ID = 268500001;
	private static final int ITEM_TEMPLATE_ID = 161000001;
	private static final int ITEM_MASK = 0x1A2B; // arbitrary mask scalar -> GENERAL_INFO writeH
	private static final int ITEM_DESC_L10N = 350123; // desc -> getL10n() = ChatUtil.l10n(350123)
	private static final long ITEM_COUNT = 7L;
	private static final String ITEM_CREATOR = "Daeva";

	@Test
	public void generateGoldenInventoryUpdateItemFixture() throws IOException {
		Path outDir = repoRoot().resolve("parity-artifacts/golden/packets");
		Files.createDirectories(outDir);

		installItemCleanupSeam();

		List<Case> cases = new ArrayList<>();
		// Non-equippable item with a creator name + count 7 -> single GENERAL_INFO blob (DEC_ITEM_USE mask written).
		cases.add(inventoryUpdateItemCase("invUpdateGeneralWithCreator", ITEM_CREATOR));
		// Same item with a null creator -> getItemCreator() returns "" (empty writeS), exercising the size-0-creator path.
		cases.add(inventoryUpdateItemCase("invUpdateGeneralNullCreator", null));

		writeFixture(outDir.resolve("SM_INVENTORY_UPDATE_ITEM.json"), "SM_INVENTORY_UPDATE_ITEM", cases);
	}

	private static Case inventoryUpdateItemCase(String name, String creator) {
		Item item = buildSimpleItem(ITEM_OBJECT_ID, ITEM_TEMPLATE_ID, ITEM_MASK, ITEM_DESC_L10N, ITEM_COUNT, creator);
		String inputs = "{\"objectId\":" + ITEM_OBJECT_ID + ",\"itemId\":" + ITEM_TEMPLATE_ID + ",\"mask\":" + ITEM_MASK
			+ ",\"desc\":" + ITEM_DESC_L10N + ",\"itemCount\":" + ITEM_COUNT + ",\"itemCreator\":"
			+ (creator == null ? "null" : "\"" + creator + "\"") + ",\"updateType\":\"DEC_ITEM_USE\"}";
		// player arg null: getFullBlob only stashes it as blob owner; GENERAL_INFO never dereferences it.
		return new Case(name, inputs, capture(new SM_INVENTORY_UPDATE_ITEM(null, item, ItemUpdateType.DEC_ITEM_USE), null));
	}

	/**
	 * Build a minimal non-equippable Item via the simple Item(objId, itemTemplate) ctor. The template is directly
	 * constructed (no JAXB) with itemId/mask/desc set, itemGroup NONE (no equip slots) and maxTuneCount pinned to 0 (what
	 * afterUnmarshal would set for a slot-0 item). The objectId is passed to the ctor directly (no IDFactory). Mirrored
	 * 1:1 on the C# asserter side.
	 */
	private static Item buildSimpleItem(int objectId, int itemId, int mask, int desc, long itemCount, String creator) {
		try {
			ItemTemplate template = new ItemTemplate();
			setField(template, "itemId", itemId);
			setField(template, "mask", mask);
			setField(template, "description", desc);
			setField(template, "itemGroup", ItemGroup.NONE);
			setField(template, "maxTuneCount", 0); // canTune() == false (deterministic, identical both sides)
			Item item = new Item(objectId, template);
			item.setItemCount(itemCount);
			item.setItemCreator(creator);
			return item;
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Failed to build simple Item", e);
		}
	}

	/**
	 * Seed DataManager.ITEM_CLEAN_UP with an empty (non-null) cleanup list so
	 * hasAccountOrLegionWhStorabilityDisabled(itemId) streams an empty list -> false (no NPE on the null default). The
	 * bounded holder seam (mirrors the WORLD_MAPS/SKILL/QUEST/TRADE_LIST seams).
	 */
	private void installItemCleanupSeam() {
		try {
			ItemRestrictionCleanupData data = new ItemRestrictionCleanupData();
			setField(data, "bplist", new ArrayList<>());
			DataManager.ITEM_CLEAN_UP = data;
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Failed to install ITEM_CLEAN_UP seam", e);
		}
	}

	/** Seed an empty HouseData so the TownService singleton ctor's getLands() loop is a no-op (no DB, no NPC_DATA). */
	private void installHouseDataSeam() {
		try {
			HouseData data = new HouseData();
			setField(data, "lands", new ArrayList<>());
			DataManager.HOUSE_DATA = data;
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Failed to install HOUSE_DATA seam", e);
		}
	}

	/** Seed an empty NpcSkillData so NpcSkillList(owner) finds no skills (getNpcSkillList(npcId) -> null). */
	private void installNpcSkillDataSeam() {
		DataManager.NPC_SKILL_DATA = new NpcSkillData();
	}

	/**
	 * Force IDFactory singleton init (lazy SingletonHolder). The real IDFactory ctor reads PlayerDAO.getUsedIDs() etc;
	 * the empty-ResultSet JDBC stub installed by installDbStub() makes each return an empty int[] (NOT null), so the
	 * real ctor completes with 0 used IDs. The objectId nextId() returns is irrelevant (overwritten with a pinned value).
	 */
	private void ensureIdFactory() {
		com.aionemu.gameserver.utils.idfactory.IDFactory.getInstance();
	}

	/**
	 * Inject a stub DataSource into DatabaseFactory so DAOs invoked by the faithful Npc ctor path (IDFactory.getUsedIDs
	 * across the player/inventory/legion/... tables, TownDAO.load) resolve a deterministic EMPTY ResultSet with no real
	 * DB. getConnection() returns a Connection proxy whose PreparedStatement.executeQuery returns an empty ResultSet, so
	 * getUsedIDs() yields int[0] (not null -> no NPE in IDFactory.lockIds) and TownDAO.load yields an empty map.
	 * Java-test-only; never touches src DB code.
	 */
	private void installDbStub() {
		try {
			Class<?> dbFactory = Class.forName("com.aionemu.commons.database.DatabaseFactory");
			Field dataSourceField = dbFactory.getDeclaredField("dataSource");
			dataSourceField.setAccessible(true);
			if (dataSourceField.get(null) == null) {
				Object stub = java.lang.reflect.Proxy.newProxyInstance(
					getClass().getClassLoader(),
					new Class<?>[] { javax.sql.DataSource.class },
					(proxy, method, args) -> {
						switch (method.getName()) {
							case "getConnection":
								return emptyConnectionStub();
							case "toString":
								return "HarnessStubDataSource";
							case "hashCode":
								return System.identityHashCode(proxy);
							case "equals":
								return proxy == args[0];
							default:
								return defaultReturn(method.getReturnType());
						}
					});
				dataSourceField.set(null, stub);
			}
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Failed to install DB stub", e);
		}
	}

	/** A Connection proxy whose prepareStatement/createStatement yield Statements producing an empty ResultSet. */
	private static java.sql.Connection emptyConnectionStub() {
		return (java.sql.Connection) java.lang.reflect.Proxy.newProxyInstance(
			GoldenWorldPacketFixtureGeneratorTest.class.getClassLoader(),
			new Class<?>[] { java.sql.Connection.class },
			(proxy, method, args) -> {
				switch (method.getName()) {
					case "prepareStatement":
					case "createStatement":
						return emptyStatementStub();
					case "close":
						return null;
					case "toString":
						return "HarnessStubConnection";
					case "hashCode":
						return System.identityHashCode(proxy);
					case "equals":
						return proxy == args[0];
					default:
						return defaultReturn(method.getReturnType());
				}
			});
	}

	/** A Statement/PreparedStatement proxy whose executeQuery yields an empty ResultSet. */
	private static Object emptyStatementStub() {
		return java.lang.reflect.Proxy.newProxyInstance(
			GoldenWorldPacketFixtureGeneratorTest.class.getClassLoader(),
			new Class<?>[] { java.sql.PreparedStatement.class },
			(proxy, method, args) -> {
				switch (method.getName()) {
					case "executeQuery":
						return emptyResultSetStub();
					case "execute":
						return false;
					case "executeUpdate":
						return 0;
					case "close":
						return null;
					case "toString":
						return "HarnessStubStatement";
					case "hashCode":
						return System.identityHashCode(proxy);
					case "equals":
						return proxy == args[0];
					default:
						return defaultReturn(method.getReturnType());
				}
			});
	}

	/** An empty ResultSet proxy: next()/last() return false, getRow() returns 0, beforeFirst() is a no-op. */
	private static java.sql.ResultSet emptyResultSetStub() {
		return (java.sql.ResultSet) java.lang.reflect.Proxy.newProxyInstance(
			GoldenWorldPacketFixtureGeneratorTest.class.getClassLoader(),
			new Class<?>[] { java.sql.ResultSet.class },
			(proxy, method, args) -> {
				switch (method.getName()) {
					case "next":
					case "last":
					case "first":
						return false;
					case "getRow":
						return 0;
					case "close":
					case "beforeFirst":
						return null;
					case "toString":
						return "HarnessStubResultSet";
					case "hashCode":
						return System.identityHashCode(proxy);
					case "equals":
						return proxy == args[0];
					default:
						return defaultReturn(method.getReturnType());
				}
			});
	}

	private static Object defaultReturn(Class<?> rt) {
		if (rt == boolean.class)
			return false;
		if (rt == int.class || rt == short.class || rt == byte.class)
			return 0;
		if (rt == long.class)
			return 0L;
		if (rt == float.class)
			return 0f;
		if (rt == double.class)
			return 0d;
		if (rt == char.class)
			return (char) 0;
		return null;
	}

	// ---- equippable item / ItemInfoBlob seam (SM_INVENTORY_ADD_ITEM, weapon blob path) ----
	//
	// Extends the item/ItemInfoBlob seam to the EQUIPPABLE-item blob path. SM_INVENTORY_ADD_ITEM writes per-item
	// item.getObjectId() + template.getTemplateId() + template.getL10n(), then ItemInfoBlob.getFullBlob(player, item).writeMe(),
	// then (item.getEquipmentSlot() & 0xFFFF) + (template.isCloth() ? 1 : 0). For an EQUIPPABLE item
	// (itemGroup.getValidEquipmentSlots() != 0) getFullBlob adds, in order:
	//   EQUIPPED_SLOT (writeQ isEquipped ? equipmentSlot : 0) + the per-type blob + ENCHANT_INFO + PREMIUM_OPTION + GENERAL_INFO.
	// The seam pins itemGroup to SWORD: a ONE_HAND weapon (getEquipType()==WEAPON, isWeapon()==true, isTwoHandWeapon()==false,
	// no fusion) so the per-type blob is SLOTS_WEAPON (writeQ MAIN_HAND mask, writeQ SUB_HAND mask via getSlotsFor(MAIN_OR_SUB)
	// -> [MAIN_HAND, SUB_HAND], two-slot non-2H else branch). It is NOT WING/SHIELD/PLUME/armor/accessory, so only SLOTS_WEAPON
	// is added. conditioningInfo == null (no CONDITIONING_INFO), isCanPolish() false (mask has no CAN_POLISH bit -> no POLISH_INFO),
	// modifiers null (no STAT_BONUSES), packCount 0 (no WRAP_INFO), not STIGMA_SHARD, not COMPOSITE (no fusion / not 2H).
	//
	// All ENCHANT_INFO / PREMIUM_OPTION reads are deterministic on the bare simple-ctor Item (mirroring the GENERAL_INFO seam):
	// isSoulBound() false, getEnchantLevel() 0, getItemSkinTemplate()==itemTemplate (skin null) -> getTemplateId(),
	// isIdentified() true (maxTuneCount 0 -> tuneCount stays 0) -> getOptionalSockets()/getEnchantBonus()/getBonusStatsId()/
	// getTuneCount() all 0, hasManaStones() false, getGodStoneId() 0, getColorTimeLeft() 0 (colorExpireTime 0, no clock) ->
	// writeDyeInfo(itemColor==null), getIdianStone() null, getTempering() 0 (not PLUME), isAmplified() false (enchantType 0),
	// getBuffSkill() 0. isCloth() false (weapon, not armor). No live Player deref, no stones/godstone/idian/conditioning/fusion
	// cascade. Mirrored 1:1 on the C# asserter side.
	private static final int EQ_ITEM_OBJECT_ID = 268700001;
	private static final int EQ_ITEM_TEMPLATE_ID = 100000855; // a 1H sword template id
	private static final int EQ_ITEM_MASK = 0x2C4D; // arbitrary mask scalar (no CAN_POLISH bit) -> GENERAL_INFO writeH
	private static final int EQ_ITEM_DESC_L10N = 350456; // desc -> getL10n() = ChatUtil.l10n(350456)
	private static final long EQ_ITEM_COUNT = 1L;
	private static final String EQ_ITEM_CREATOR = "Smith";

	@Test
	public void generateGoldenInventoryAddItemEquippableFixture() throws IOException {
		Path outDir = repoRoot().resolve("parity-artifacts/golden/packets");
		Files.createDirectories(outDir);

		installItemCleanupSeam(); // GENERAL_INFO reads DataManager.ITEM_CLEAN_UP.hasAccountOrLegionWhStorabilityDisabled

		List<Case> cases = new ArrayList<>();
		// Equippable 1H sword (unequipped) bought from an npc (ItemAddType.BUY -> mask 0x1C, no ITEM_COLLECT slot branch).
		cases.add(inventoryAddItemEquippableCase("invAddEquippableWeaponBuy", ItemAddType.BUY));

		writeFixture(outDir.resolve("SM_INVENTORY_ADD_ITEM.json"), "SM_INVENTORY_ADD_ITEM", cases);
	}

	private static Case inventoryAddItemEquippableCase(String name, ItemAddType addType) {
		Item item = buildEquippableWeapon(EQ_ITEM_OBJECT_ID, EQ_ITEM_TEMPLATE_ID, EQ_ITEM_MASK, EQ_ITEM_DESC_L10N,
			EQ_ITEM_COUNT, EQ_ITEM_CREATOR);
		List<Item> items = new ArrayList<>();
		items.add(item);
		String inputs = "{\"objectId\":" + EQ_ITEM_OBJECT_ID + ",\"itemId\":" + EQ_ITEM_TEMPLATE_ID + ",\"mask\":"
			+ EQ_ITEM_MASK + ",\"desc\":" + EQ_ITEM_DESC_L10N + ",\"itemCount\":" + EQ_ITEM_COUNT + ",\"itemCreator\":\""
			+ EQ_ITEM_CREATOR + "\",\"itemGroup\":\"SWORD\",\"addType\":\"" + addType.name() + "\"}";
		// player arg null: getFullBlob only stashes it as blob owner; the weapon/equipped/enchant/premium/general writers
		// never dereference it.
		return new Case(name, inputs, capture(new SM_INVENTORY_ADD_ITEM(items, null, addType), null));
	}

	/**
	 * Build a minimal EQUIPPABLE 1H-sword Item via the simple Item(objId, itemTemplate) ctor. The template is directly
	 * constructed (no JAXB) with itemId/mask/desc set, itemGroup SWORD (ONE_HAND weapon, valid equip slots) and maxTuneCount
	 * pinned to 0 (so canTune() false -> isIdentified() true). Item is left UNEQUIPPED with no stones/godstone/idian/dye/
	 * tempering/fusion, so the equippable blob path is deterministic. Mirrored 1:1 on the C# asserter side.
	 */
	private static Item buildEquippableWeapon(int objectId, int itemId, int mask, int desc, long itemCount, String creator) {
		try {
			ItemTemplate template = new ItemTemplate();
			setField(template, "itemId", itemId);
			setField(template, "mask", mask);
			setField(template, "description", desc);
			setField(template, "itemGroup", ItemGroup.SWORD);
			setField(template, "maxTuneCount", 0); // canTune() == false -> isIdentified() == true (deterministic)
			Item item = new Item(objectId, template);
			item.setItemCount(itemCount);
			item.setItemCreator(creator);
			return item;
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Failed to build equippable weapon Item", e);
		}
	}

	// ---- equippable item / ItemInfoBlob seam: more per-type variants (SM_INVENTORY_ADD_ITEM) ----
	//
	// Reuses the equippable-item seam above to cover the OTHER per-type blob writers selected by getFullBlob's itemGroup
	// branch. Each is a DISTINCT fixture case with a DISTINCT objectId (so it never clobbers the weapon fixture):
	//   * ARMOR (itemGroup PL_TORSO, a PLATE torso): isArmor()==true, armorType != ACCESSORY -> SLOTS_ARMOR. Its writer
	//     writeThisBlob = writeQ(getSlotFor(getItemSlot()).getSlotIdMask()) [PL_TORSO -> ItemSlot.TORSO, single slot],
	//     writeQ(0), writeDyeInfo(getItemColor()). Two armor cases: one UNDYED (itemColor null -> writeDyeInfo skips 4
	//     zero bytes) and one DYED (itemColor pinned, colorExpireTime 0 so getColorTimeLeft()==0 -> NO clock read; the
	//     dye-populated branch fires in BOTH the SLOTS_ARMOR writer AND ENCHANT_INFO's writeDyeInfo). isCloth()==true
	//     for a non-accessory armor -> the host packet's trailing isCloth byte is 1.
	//   * ACCESSORY (itemGroup RING): isArmor()==true, armorType == ACCESSORY -> SLOTS_ACCESSORY. Its writer reads
	//     getSlotsFor(getItemSlot()) [RING -> RING_LEFT|RING_RIGHT, length 2] -> writeQ(slots[0]) + writeQ(slots[1])
	//     (the two-slot branch). isCloth()==false (accessory) -> trailing isCloth byte 0.
	// All other blob reads are identical to the weapon seam (ENCHANT_INFO/PREMIUM_OPTION/GENERAL_INFO deterministic on the
	// bare simple-ctor Item; no live Player deref, no stones/godstone/idian/conditioning/fusion). Mirrored 1:1 on C#.
	private static final int EQ_ARMOR_OBJECT_ID = 268700002; // distinct from the weapon EQ_ITEM_OBJECT_ID
	private static final int EQ_ARMOR_TEMPLATE_ID = 110000777; // a plate torso template id
	private static final int EQ_ARMOR_MASK = 0x33AA;
	private static final int EQ_ARMOR_DESC_L10N = 350789;
	private static final int EQ_ACCESSORY_OBJECT_ID = 268700003; // distinct
	private static final int EQ_ACCESSORY_TEMPLATE_ID = 115000333; // a ring template id
	private static final int EQ_ACCESSORY_MASK = 0x44BB;
	private static final int EQ_ACCESSORY_DESC_L10N = 350790;
	private static final int EQ_DYED_ARMOR_OBJECT_ID = 268700004; // distinct
	private static final int EQ_DYED_ARMOR_COLOR = 0x3399CC; // r=0x33 g=0x99 b=0xCC (no alpha), colorExpireTime 0

	@Test
	public void generateGoldenInventoryAddItemEquippableVariantsFixture() throws IOException {
		Path outDir = repoRoot().resolve("parity-artifacts/golden/packets");
		Files.createDirectories(outDir);

		installItemCleanupSeam(); // GENERAL_INFO reads DataManager.ITEM_CLEAN_UP.hasAccountOrLegionWhStorabilityDisabled

		List<Case> cases = new ArrayList<>();
		// (a) Plate torso armor (unequipped, undyed) -> SLOTS_ARMOR (single slot + 4 zero dye bytes), isCloth byte 1.
		cases.add(equippableVariantCase("invAddEquippableArmorPlateUndyed", EQ_ARMOR_OBJECT_ID, EQ_ARMOR_TEMPLATE_ID,
			EQ_ARMOR_MASK, EQ_ARMOR_DESC_L10N, "Armorsmith", ItemGroup.PL_TORSO, null, ItemAddType.BUY));
		// (b) Ring accessory (unequipped) -> SLOTS_ACCESSORY (two-slot branch RING_LEFT|RING_RIGHT), isCloth byte 0.
		cases.add(equippableVariantCase("invAddEquippableAccessoryRing", EQ_ACCESSORY_OBJECT_ID, EQ_ACCESSORY_TEMPLATE_ID,
			EQ_ACCESSORY_MASK, EQ_ACCESSORY_DESC_L10N, "Jeweler", ItemGroup.RING, null, ItemAddType.BUY));
		// (c) DYED plate torso armor (colorExpireTime 0 -> deterministic) -> dye-populated branch in SLOTS_ARMOR + ENCHANT_INFO.
		cases.add(equippableVariantCase("invAddEquippableArmorPlateDyed", EQ_DYED_ARMOR_OBJECT_ID, EQ_ARMOR_TEMPLATE_ID,
			EQ_ARMOR_MASK, EQ_ARMOR_DESC_L10N, "Armorsmith", ItemGroup.PL_TORSO, EQ_DYED_ARMOR_COLOR, ItemAddType.BUY));

		writeFixture(outDir.resolve("SM_INVENTORY_ADD_ITEM_VARIANTS.json"), "SM_INVENTORY_ADD_ITEM", cases);
	}

	// ---- equippable item / ItemInfoBlob seam: per-type SHIELD / WING / PLUME blob writers (SM_INVENTORY_ADD_ITEM) ----
	//
	// Reuses the equippable-item seam above to cover the THREE per-type blob writers that getFullBlob selects BEFORE the
	// isArmor()/isWeapon() branches (itemGroup == WING / SHIELD / PLUME). Each is a DISTINCT fixture case with a DISTINCT
	// objectId in a NEW fixture file (never clobbers the weapon/armor/accessory fixtures):
	//   * SHIELD (itemGroup SHIELD): getValidEquipmentSlots()==SUB_HAND mask -> getFullBlob adds EQUIPPED_SLOT + SLOTS_SHIELD
	//     + ENCHANT_INFO + PREMIUM_OPTION + GENERAL_INFO. ShieldInfoBlobEntry.writeThisBlob = writeQ(getSlotFor(getItemSlot())
	//     .getSlotIdMask()) [SHIELD -> ItemSlot.SUB_HAND, single slot], writeQ(0), writeDyeInfo(getItemColor()) [null -> 4 zero
	//     bytes]. SHIELD subType -> ArmorType.GENERAL -> isArmor()==true, armorType != ACCESSORY, not BELT -> isCloth()==true ->
	//     trailing isCloth byte 1.
	//   * WING (itemGroup WING): getValidEquipmentSlots()==WINGS mask -> EQUIPPED_SLOT + SLOTS_WING + ENCHANT_INFO + ...
	//     WingInfoBlobEntry.writeThisBlob = writeQ(getSlotFor(getItemSlot()).getSlotIdMask()) [WING -> ItemSlot.WINGS], writeQ(0)
	//     (no dye). WING subType -> ArmorType.GENERAL -> isArmor()==true -> isCloth()==true -> trailing byte 1.
	//   * PLUME (itemGroup PLUME): getValidEquipmentSlots()==PLUME mask -> EQUIPPED_SLOT + PLUME_INFO + ENCHANT_INFO + ...
	//     PlumeInfoBlobEntry.writeThisBlob = writeQ(getSlotFor(getItemSlot()).getSlotIdMask()) [PLUME -> ItemSlot.PLUME],
	//     writeQ(0x100000), writeD(0)x4. PLUME subType -> EquipType.PLUME (NOT armor) -> isArmor()==false -> isCloth()==false ->
	//     trailing byte 0.
	//
	// Plus a TEMPERED-plume case to exercise the ENCHANT_INFO branch (item.getTempering() > 0 && itemGroup == PLUME): it reads
	// getItemTemplate().getTemperingName() (".equals("TSHIRT_PHYSICAL") ? PLUM_PHISICAL_ATTACK : PLUM_BOOST_MAGICAL_SKILL"),
	// then writes PLUM_HP.getId()/PLUM_HP.getBoostValue()*tempering for the 1st stat and stat.getId()/stat.getBoostValue()*
	// tempering + getRndPlumeBonusValue() for the 2nd. Two sub-cases: temperingName "TSHIRT_PHYSICAL" (-> PLUM_PHISICAL_ATTACK)
	// and a non-match name (-> PLUM_BOOST_MAGICAL_SKILL), each with a pinned tempering level + rndPlumeBonusValue, so BOTH
	// PlumStatEnum branches are covered. All other blob reads are identical to the equippable seam (PREMIUM_OPTION/GENERAL_INFO
	// deterministic; no live Player deref, no stones/godstone/idian/conditioning/fusion). Mirrored 1:1 on the C# asserter side.
	private static final int EQ_SHIELD_OBJECT_ID = 268700101; // distinct from weapon/armor/accessory ids
	private static final int EQ_SHIELD_TEMPLATE_ID = 120000111;
	private static final int EQ_SHIELD_MASK = 0x55CC;
	private static final int EQ_SHIELD_DESC_L10N = 350801;
	private static final int EQ_WING_OBJECT_ID = 268700102; // distinct
	private static final int EQ_WING_TEMPLATE_ID = 125000222;
	private static final int EQ_WING_MASK = 0x66DD;
	private static final int EQ_WING_DESC_L10N = 350802;
	private static final int EQ_PLUME_OBJECT_ID = 268700103; // distinct
	private static final int EQ_PLUME_TEMPLATE_ID = 130000333;
	private static final int EQ_PLUME_MASK = 0x77EE;
	private static final int EQ_PLUME_DESC_L10N = 350803;
	private static final int EQ_PLUME_PHYS_OBJECT_ID = 268700104; // distinct (tempered, TSHIRT_PHYSICAL)
	private static final int EQ_PLUME_MAGIC_OBJECT_ID = 268700105; // distinct (tempered, non-match name)
	private static final int EQ_PLUME_TEMPERING = 5; // tempering level (> 0) -> ENCHANT_INFO plume branch
	private static final int EQ_PLUME_RND_BONUS = 17; // getRndPlumeBonusValue() addend on the 2nd stat value

	@Test
	public void generateGoldenInventoryAddItemPerTypeFixture() throws IOException {
		Path outDir = repoRoot().resolve("parity-artifacts/golden/packets");
		Files.createDirectories(outDir);

		installItemCleanupSeam(); // GENERAL_INFO reads DataManager.ITEM_CLEAN_UP.hasAccountOrLegionWhStorabilityDisabled

		List<Case> cases = new ArrayList<>();
		// (a) Shield (unequipped, undyed) -> SLOTS_SHIELD (SUB_HAND slot + writeQ(0) + 4 zero dye bytes), isCloth byte 1.
		cases.add(equippableVariantCase("invAddEquippableShield", EQ_SHIELD_OBJECT_ID, EQ_SHIELD_TEMPLATE_ID,
			EQ_SHIELD_MASK, EQ_SHIELD_DESC_L10N, "Smith", ItemGroup.SHIELD, null, ItemAddType.BUY));
		// (b) Wing (unequipped) -> SLOTS_WING (WINGS slot + writeQ(0)), isCloth byte 1.
		cases.add(equippableVariantCase("invAddEquippableWing", EQ_WING_OBJECT_ID, EQ_WING_TEMPLATE_ID,
			EQ_WING_MASK, EQ_WING_DESC_L10N, "Tailor", ItemGroup.WING, null, ItemAddType.BUY));
		// (c) Plume (unequipped, NOT tempered) -> PLUME_INFO (PLUME slot + writeQ(0x100000) + writeD(0)x4), isCloth byte 0.
		cases.add(equippableVariantCase("invAddEquippablePlume", EQ_PLUME_OBJECT_ID, EQ_PLUME_TEMPLATE_ID,
			EQ_PLUME_MASK, EQ_PLUME_DESC_L10N, "Plumer", ItemGroup.PLUME, null, ItemAddType.BUY));
		// (d) TEMPERED plume, temperingName "TSHIRT_PHYSICAL" -> ENCHANT_INFO plume branch (PLUM_HP + PLUM_PHISICAL_ATTACK).
		cases.add(temperedPlumeCase("invAddEquippablePlumeTemperedPhysical", EQ_PLUME_PHYS_OBJECT_ID, EQ_PLUME_TEMPLATE_ID,
			EQ_PLUME_MASK, EQ_PLUME_DESC_L10N, "Plumer", "TSHIRT_PHYSICAL", EQ_PLUME_TEMPERING, EQ_PLUME_RND_BONUS));
		// (e) TEMPERED plume, non-match temperingName -> ENCHANT_INFO plume branch (PLUM_HP + PLUM_BOOST_MAGICAL_SKILL).
		cases.add(temperedPlumeCase("invAddEquippablePlumeTemperedMagical", EQ_PLUME_MAGIC_OBJECT_ID, EQ_PLUME_TEMPLATE_ID,
			EQ_PLUME_MASK, EQ_PLUME_DESC_L10N, "Plumer", "TSHIRT_MAGICAL", EQ_PLUME_TEMPERING, EQ_PLUME_RND_BONUS));

		writeFixture(outDir.resolve("SM_INVENTORY_ADD_ITEM_PERTYPE.json"), "SM_INVENTORY_ADD_ITEM", cases);
	}

	private static Case temperedPlumeCase(String name, int objectId, int itemId, int mask, int desc, String creator,
			String temperingName, int tempering, int rndPlumeBonusValue) {
		Item item = buildTemperedPlume(objectId, itemId, mask, desc, creator, temperingName, tempering, rndPlumeBonusValue);
		List<Item> items = new ArrayList<>();
		items.add(item);
		String inputs = "{\"objectId\":" + objectId + ",\"itemId\":" + itemId + ",\"mask\":" + mask + ",\"desc\":" + desc
			+ ",\"itemCount\":1,\"itemCreator\":\"" + creator + "\",\"itemGroup\":\"PLUME\",\"temperingName\":\""
			+ temperingName + "\",\"tempering\":" + tempering + ",\"rndPlumeBonusValue\":" + rndPlumeBonusValue
			+ ",\"addType\":\"BUY\"}";
		return new Case(name, inputs, capture(new SM_INVENTORY_ADD_ITEM(items, null, ItemAddType.BUY), null));
	}

	/**
	 * Build a minimal EQUIPPABLE TEMPERED plume Item via the simple Item(objId, itemTemplate) ctor. itemGroup PLUME (PLUME
	 * slot, isArmor false) + temperingName set (so getTemperingName().equals(..) doesn't NPE) + tempering level + a random
	 * plume bonus value, so the ENCHANT_INFO plume branch fires. maxTuneCount 0 (canTune() false -> isIdentified() true).
	 * No stones/godstone/idian/dye/conditioning/fusion. Mirrored 1:1 on the C# asserter side.
	 */
	private static Item buildTemperedPlume(int objectId, int itemId, int mask, int desc, String creator,
			String temperingName, int tempering, int rndPlumeBonusValue) {
		try {
			ItemTemplate template = new ItemTemplate();
			setField(template, "itemId", itemId);
			setField(template, "mask", mask);
			setField(template, "description", desc);
			setField(template, "itemGroup", ItemGroup.PLUME);
			setField(template, "temperingName", temperingName);
			setField(template, "maxTuneCount", 0); // canTune() == false -> isIdentified() == true (deterministic)
			Item item = new Item(objectId, template);
			item.setItemCount(1L);
			item.setItemCreator(creator);
			item.setTempering(tempering);
			item.setRndPlumeBonusValue(rndPlumeBonusValue);
			return item;
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Failed to build tempered plume Item", e);
		}
	}

	// ---- equippable item / ItemInfoBlob seam: ENCHANT_INFO SUB-OBJECT writers (socketed ManaStone / GodStone) ----
	//
	// Reuses the equippable-weapon seam (1H SWORD base -> SLOTS_WEAPON, already byte-validated) and POPULATES the
	// ENCHANT_INFO sub-object slots that the bare-item seam left null/empty. Each is a DISTINCT case with a DISTINCT
	// objectId in a NEW fixture file (never clobbers the weapon/armor/accessory/shield/wing/plume fixtures):
	//   * socketedManastones: item.getItemStones() carries two ManaStones (slot 0 + slot 2, distinct itemIds).
	//     EnchantInfoBlobEntry's createManastoneMap builds a slot->stone map; the Item.MAX_BASIC_STONES (6) loop writes
	//     stone.getItemId() at the populated slots and 0 elsewhere. ManaStone(itemObjId, itemId, slot, NEW) ctor reads
	//     DataManager.ITEM_DATA.getItemTemplate(itemId) (empty holder -> null, tolerated) -> only the ItemStone scalars
	//     (slot/itemId) are read by the writer. No modifiers loaded.
	//   * godStone: item.setGodStone(new GodStone(item, 0, godStoneId, null, NEW)) -> getGodStoneId() == godStoneId.
	//     The GodStone ctor takes godstoneInfo directly (null OK; the writer only reads getItemId()), DataManager-free.
	//   * manastonesAndGodStone: BOTH branches populated on one item (two manastones + a godstone).
	// All other ENCHANT_INFO/SLOTS_WEAPON/PREMIUM_OPTION/GENERAL_INFO reads are identical to the weapon seam (no
	// idian/dye/tempering/fusion). Bounded DataManager: ITEM_DATA seeded EMPTY (ManaStone ctor tolerance) + the existing
	// ITEM_CLEAN_UP seam. Mirrored 1:1 on the C# asserter side. Java is the oracle.
	private static final int EQ_MS_OBJECT_ID = 268700201; // weapon w/ socketed manastones (distinct)
	private static final int EQ_GS_OBJECT_ID = 268700202; // weapon w/ godstone (distinct)
	private static final int EQ_MSGS_OBJECT_ID = 268700203; // weapon w/ manastones + godstone (distinct)
	private static final int EQ_SUBOBJ_TEMPLATE_ID = 100000855; // same 1H sword template id as the weapon seam
	private static final int EQ_SUBOBJ_MASK = 0x2C4D;
	private static final int EQ_SUBOBJ_DESC_L10N = 350456;
	private static final int EQ_MS_SLOT0_ITEM_ID = 167000001; // manastone item id at socket slot 0
	private static final int EQ_MS_SLOT2_ITEM_ID = 167000002; // manastone item id at socket slot 2
	private static final int EQ_GS_ITEM_ID = 168000123; // godstone item id

	@Test
	public void generateGoldenInventoryAddItemSubObjectFixture() throws IOException {
		Path outDir = repoRoot().resolve("parity-artifacts/golden/packets");
		Files.createDirectories(outDir);

		installItemCleanupSeam(); // GENERAL_INFO reads DataManager.ITEM_CLEAN_UP.hasAccountOrLegionWhStorabilityDisabled
		installItemDataSeam(); // ManaStone ctor reads DataManager.ITEM_DATA.getItemTemplate(itemId) (empty -> null)

		List<Case> cases = new ArrayList<>();
		// (a) Weapon with two socketed manastones (slots 0 and 2).
		cases.add(subObjectCase("invAddEquippableWeaponSocketedManastones", EQ_MS_OBJECT_ID, true, false));
		// (b) Weapon with a godstone.
		cases.add(subObjectCase("invAddEquippableWeaponGodStone", EQ_GS_OBJECT_ID, false, true));
		// (c) Weapon with both manastones and a godstone.
		cases.add(subObjectCase("invAddEquippableWeaponManastonesAndGodStone", EQ_MSGS_OBJECT_ID, true, true));

		writeFixture(outDir.resolve("SM_INVENTORY_ADD_ITEM_SUBOBJECT.json"), "SM_INVENTORY_ADD_ITEM", cases);
	}

	private static Case subObjectCase(String name, int objectId, boolean withManastones, boolean withGodStone) {
		Item item = buildSubObjectWeapon(objectId, withManastones, withGodStone);
		List<Item> items = new ArrayList<>();
		items.add(item);
		String inputs = "{\"objectId\":" + objectId + ",\"itemId\":" + EQ_SUBOBJ_TEMPLATE_ID + ",\"mask\":" + EQ_SUBOBJ_MASK
			+ ",\"desc\":" + EQ_SUBOBJ_DESC_L10N + ",\"itemCount\":1,\"itemCreator\":\"Smith\",\"itemGroup\":\"SWORD\""
			+ ",\"withManastones\":" + withManastones + ",\"manastoneSlot0ItemId\":" + EQ_MS_SLOT0_ITEM_ID
			+ ",\"manastoneSlot2ItemId\":" + EQ_MS_SLOT2_ITEM_ID + ",\"withGodStone\":" + withGodStone
			+ ",\"godStoneItemId\":" + EQ_GS_ITEM_ID + ",\"addType\":\"BUY\"}";
		return new Case(name, inputs, capture(new SM_INVENTORY_ADD_ITEM(items, null, ItemAddType.BUY), null));
	}

	/**
	 * Build a 1H-sword Item (same base as buildEquippableWeapon) and POPULATE the ENCHANT_INFO sub-objects: optional
	 * socketed manastones at slots 0/2 (via getItemStones().add(new ManaStone(...))) and/or a godstone (via
	 * setGodStone(new GodStone(...))). The ManaStone ctor reads DataManager.ITEM_DATA.getItemTemplate(itemId) (empty
	 * holder -> null, tolerated); the GodStone ctor takes godstoneInfo directly (null OK). Mirrored 1:1 on the C# side.
	 */
	private static Item buildSubObjectWeapon(int objectId, boolean withManastones, boolean withGodStone) {
		try {
			ItemTemplate template = new ItemTemplate();
			setField(template, "itemId", EQ_SUBOBJ_TEMPLATE_ID);
			setField(template, "mask", EQ_SUBOBJ_MASK);
			setField(template, "description", EQ_SUBOBJ_DESC_L10N);
			setField(template, "itemGroup", ItemGroup.SWORD);
			setField(template, "maxTuneCount", 0); // canTune() == false -> isIdentified() == true (deterministic)
			Item item = new Item(objectId, template);
			item.setItemCount(1L);
			item.setItemCreator("Smith");
			if (withManastones) {
				item.getItemStones().add(new ManaStone(objectId, EQ_MS_SLOT0_ITEM_ID, 0, PersistentState.NEW));
				item.getItemStones().add(new ManaStone(objectId, EQ_MS_SLOT2_ITEM_ID, 2, PersistentState.NEW));
			}
			if (withGodStone)
				item.setGodStone(new GodStone(item, 0, EQ_GS_ITEM_ID, null, PersistentState.NEW));
			return item;
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Failed to build sub-object weapon Item", e);
		}
	}

	/** Seed DataManager.ITEM_DATA with an empty (non-null) ItemData so the ManaStone ctor's getItemTemplate(itemId) returns null. */
	private void installItemDataSeam() {
		DataManager.ITEM_DATA = new ItemData();
	}

	// ---- equippable item / ItemInfoBlob seam: more sub-object blob entries (CONDITIONING_INFO / COMPOSITE_ITEM / POLISH_INFO) ----
	//
	// Reuses the equippable-weapon seam (1H SWORD base -> SLOTS_WEAPON, already byte-validated) and POPULATES the three
	// remaining BOUNDED sub-object blob entries that getFullBlob can add (one per case, distinct objectIds, in a NEW
	// fixture file — never clobbers the weapon/armor/accessory/shield/wing/plume/manastone/godstone fixtures):
	//   * CONDITIONING_INFO (getFullBlob adds it inside the equippable block when item.getConditioningInfo() != null, AFTER
	//     ENCHANT_INFO): ConditioningInfoBlobEntry.writeThisBlob = writeD(ownerItem.getChargePoints()). The conditioningInfo
	//     ChargeInfo is built directly (ChargeInfo(chargePoints, item); its ctor reads item.getImprovement() which is null on
	//     the bare item -> attackBurn/defendBurn 0, deterministic) and pinned on the private Item.conditioningInfo field. The
	//     writer reads ONLY getChargePoints() (== the ctor arg). So getFullBlob = EQUIPPED_SLOT + SLOTS_WEAPON + ENCHANT_INFO
	//     + CONDITIONING_INFO + PREMIUM_OPTION + GENERAL_INFO.
	//   * COMPOSITE_ITEM (getFullBlob adds it FIRST, before EQUIPPED_SLOT, when item.hasFusionedItem()): CompositeItemBlobEntry
	//     .writeThisBlob = writeD(getFusionedItemId()) + writeFusionStones [no fusion stones -> skip MAX_BASIC_STONES*4 zero
	//     bytes] + writeC(getFusionedItemOptionalSockets()) + writeC(getFusionedItemBonusStatsId()). setFusionedItem(template,
	//     bonusStatsId=0, optionalSockets) wires the fusioned template; bonusStatsId 0 -> setFusionedItemBonusStats short-
	//     circuits (NO fusionedItemTemplate.getStatBonusSetId() deref) -> getFusionedItemBonusStatsId() == 0. So getFullBlob =
	//     COMPOSITE_ITEM + EQUIPPED_SLOT + SLOTS_WEAPON + ENCHANT_INFO + PREMIUM_OPTION + GENERAL_INFO.
	//   * POLISH_INFO (getFullBlob adds it inside the equippable block when itemTemplate.isCanPolish(), i.e. the CAN_POLISH
	//     mask bit (1<<17) is set, BEFORE PREMIUM_OPTION): PolishInfoBlobEntry.writeThisBlob = writeD(stone == null ? 0 :
	//     stone.getPolishCharge()); idianStone is null (IdianStone is unbounded for the unit harness — its ctor NREs on an
	//     empty ItemData), so it writes 0 deterministically WITHOUT an IdianStone. So getFullBlob = EQUIPPED_SLOT + SLOTS_WEAPON
	//     + ENCHANT_INFO + POLISH_INFO + PREMIUM_OPTION + GENERAL_INFO.
	// All other ENCHANT_INFO/SLOTS_WEAPON/PREMIUM_OPTION/GENERAL_INFO reads are identical to the weapon seam (no idian/dye/
	// tempering/manastone/godstone). Bounded DataManager: the existing ITEM_CLEAN_UP seam (GENERAL_INFO) only. Mirrored 1:1
	// on the C# asserter side. Java is the oracle.
	private static final int EQ_COND_OBJECT_ID = 268700301; // weapon w/ conditioning (distinct)
	private static final int EQ_COMP_OBJECT_ID = 268700302; // weapon w/ fusioned (composite) item (distinct)
	private static final int EQ_POLISH_OBJECT_ID = 268700303; // weapon w/ CAN_POLISH mask, null idian (distinct)
	private static final int EQ_SUBOBJ2_TEMPLATE_ID = 100000855; // same 1H sword template id as the weapon seam
	private static final int EQ_SUBOBJ2_MASK = 0x2C4D; // base mask (no CAN_POLISH bit) -> GENERAL_INFO writeH
	private static final int EQ_SUBOBJ2_DESC_L10N = 350456;
	private static final int EQ_COND_CHARGE_POINTS = 432109; // getChargePoints() -> CONDITIONING_INFO writeD
	private static final int EQ_COMP_FUSIONED_TEMPLATE_ID = 100000999; // fusioned item template id -> getFusionedItemId()
	private static final int EQ_COMP_OPTIONAL_SOCKETS = 3; // getFusionedItemOptionalSockets() -> writeC
	private static final int EQ_POLISH_MASK = 0x2C4D | ItemMask.CAN_POLISH; // CAN_POLISH bit set -> isCanPolish() true

	@Test
	public void generateGoldenInventoryAddItemSubObject2Fixture() throws IOException {
		Path outDir = repoRoot().resolve("parity-artifacts/golden/packets");
		Files.createDirectories(outDir);

		installItemCleanupSeam(); // GENERAL_INFO reads DataManager.ITEM_CLEAN_UP.hasAccountOrLegionWhStorabilityDisabled

		List<Case> cases = new ArrayList<>();
		// (a) Weapon with a conditioning (charge) info -> CONDITIONING_INFO blob (writeD chargePoints).
		cases.add(conditioningCase("invAddEquippableWeaponConditioning", EQ_COND_OBJECT_ID, EQ_COND_CHARGE_POINTS));
		// (b) Weapon with a fusioned (composite) item, bonusStatsId 0 -> COMPOSITE_ITEM blob (fusionedId + 24 zero bytes + sockets + 0).
		cases.add(compositeCase("invAddEquippableWeaponComposite", EQ_COMP_OBJECT_ID, EQ_COMP_FUSIONED_TEMPLATE_ID,
			EQ_COMP_OPTIONAL_SOCKETS));
		// (c) Weapon with the CAN_POLISH mask bit + null idian stone -> POLISH_INFO blob (writeD 0).
		cases.add(polishCase("invAddEquippableWeaponPolishNoIdian", EQ_POLISH_OBJECT_ID));

		writeFixture(outDir.resolve("SM_INVENTORY_ADD_ITEM_SUBOBJECT2.json"), "SM_INVENTORY_ADD_ITEM", cases);
	}

	private static Case conditioningCase(String name, int objectId, int chargePoints) {
		Item item = buildSubObject2Weapon(objectId, EQ_SUBOBJ2_MASK);
		setConditioningInfo(item, chargePoints);
		List<Item> items = new ArrayList<>();
		items.add(item);
		String inputs = "{\"objectId\":" + objectId + ",\"itemId\":" + EQ_SUBOBJ2_TEMPLATE_ID + ",\"mask\":" + EQ_SUBOBJ2_MASK
			+ ",\"desc\":" + EQ_SUBOBJ2_DESC_L10N + ",\"itemCount\":1,\"itemCreator\":\"Smith\",\"itemGroup\":\"SWORD\""
			+ ",\"subObject\":\"conditioning\",\"chargePoints\":" + chargePoints + ",\"addType\":\"BUY\"}";
		return new Case(name, inputs, capture(new SM_INVENTORY_ADD_ITEM(items, null, ItemAddType.BUY), null));
	}

	private static Case compositeCase(String name, int objectId, int fusionedItemId, int optionalSockets) {
		Item item = buildSubObject2Weapon(objectId, EQ_SUBOBJ2_MASK);
		// setFusionedItem(template, bonusStatsId=0, optionalSockets): bonusStatsId 0 -> getFusionedItemBonusStatsId() == 0.
		item.setFusionedItem(fusionedItemTemplate(fusionedItemId), 0, optionalSockets);
		List<Item> items = new ArrayList<>();
		items.add(item);
		String inputs = "{\"objectId\":" + objectId + ",\"itemId\":" + EQ_SUBOBJ2_TEMPLATE_ID + ",\"mask\":" + EQ_SUBOBJ2_MASK
			+ ",\"desc\":" + EQ_SUBOBJ2_DESC_L10N + ",\"itemCount\":1,\"itemCreator\":\"Smith\",\"itemGroup\":\"SWORD\""
			+ ",\"subObject\":\"composite\",\"fusionedItemId\":" + fusionedItemId + ",\"optionalSockets\":" + optionalSockets
			+ ",\"bonusStatsId\":0,\"addType\":\"BUY\"}";
		return new Case(name, inputs, capture(new SM_INVENTORY_ADD_ITEM(items, null, ItemAddType.BUY), null));
	}

	private static Case polishCase(String name, int objectId) {
		// CAN_POLISH mask bit set -> isCanPolish() true; idian stone left null -> writeD 0.
		Item item = buildSubObject2Weapon(objectId, EQ_POLISH_MASK);
		List<Item> items = new ArrayList<>();
		items.add(item);
		String inputs = "{\"objectId\":" + objectId + ",\"itemId\":" + EQ_SUBOBJ2_TEMPLATE_ID + ",\"mask\":" + EQ_POLISH_MASK
			+ ",\"desc\":" + EQ_SUBOBJ2_DESC_L10N + ",\"itemCount\":1,\"itemCreator\":\"Smith\",\"itemGroup\":\"SWORD\""
			+ ",\"subObject\":\"polish\",\"addType\":\"BUY\"}";
		return new Case(name, inputs, capture(new SM_INVENTORY_ADD_ITEM(items, null, ItemAddType.BUY), null));
	}

	/**
	 * Build a 1H-sword Item (same base as buildEquippableWeapon) with the given mask. itemGroup SWORD -> SLOTS_WEAPON;
	 * maxTuneCount 0 (canTune() false -> isIdentified() true). No stones/godstone/idian/dye/tempering by default.
	 */
	private static Item buildSubObject2Weapon(int objectId, int mask) {
		try {
			ItemTemplate template = new ItemTemplate();
			setField(template, "itemId", EQ_SUBOBJ2_TEMPLATE_ID);
			setField(template, "mask", mask);
			setField(template, "description", EQ_SUBOBJ2_DESC_L10N);
			setField(template, "itemGroup", ItemGroup.SWORD);
			setField(template, "maxTuneCount", 0); // canTune() == false -> isIdentified() == true (deterministic)
			Item item = new Item(objectId, template);
			item.setItemCount(1L);
			item.setItemCreator("Smith");
			return item;
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Failed to build sub-object2 weapon Item", e);
		}
	}

	/**
	 * Pin the private Item.conditioningInfo field to a ChargeInfo with the given chargePoints. The ChargeInfo ctor reads
	 * item.getImprovement() (null on the bare item -> attackBurn/defendBurn 0); the CONDITIONING_INFO writer reads only
	 * getChargePoints() (== the ctor arg). Mirrored 1:1 on the C# side.
	 */
	private static void setConditioningInfo(Item item, int chargePoints) {
		try {
			Field f = Item.class.getDeclaredField("conditioningInfo");
			f.setAccessible(true);
			f.set(item, new ChargeInfo(chargePoints, item));
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Failed to set conditioningInfo", e);
		}
	}

	/** Build a fusioned ItemTemplate carrying only the templateId (getFusionedItemId() reads getTemplateId()). */
	private static ItemTemplate fusionedItemTemplate(int itemId) {
		try {
			ItemTemplate t = new ItemTemplate();
			setField(t, "itemId", itemId);
			return t;
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Failed to build fusioned ItemTemplate", e);
		}
	}

	private static Case equippableVariantCase(String name, int objectId, int itemId, int mask, int desc, String creator,
			ItemGroup itemGroup, Integer itemColor, ItemAddType addType) {
		Item item = buildEquippableVariant(objectId, itemId, mask, desc, creator, itemGroup, itemColor);
		List<Item> items = new ArrayList<>();
		items.add(item);
		String inputs = "{\"objectId\":" + objectId + ",\"itemId\":" + itemId + ",\"mask\":" + mask + ",\"desc\":" + desc
			+ ",\"itemCount\":1,\"itemCreator\":\"" + creator + "\",\"itemGroup\":\"" + itemGroup.name() + "\",\"itemColor\":"
			+ (itemColor == null ? "null" : itemColor) + ",\"addType\":\"" + addType.name() + "\"}";
		return new Case(name, inputs, capture(new SM_INVENTORY_ADD_ITEM(items, null, addType), null));
	}

	/**
	 * Build a minimal EQUIPPABLE non-weapon Item (armor or accessory) via the simple Item(objId, itemTemplate) ctor.
	 * itemGroup selects the per-type blob (PL_TORSO -> SLOTS_ARMOR, RING -> SLOTS_ACCESSORY). maxTuneCount pinned to 0
	 * (canTune() false -> isIdentified() true). Optionally dyed via setItemColor (colorExpireTime stays 0 ->
	 * getColorTimeLeft() == 0, no clock read). No stones/godstone/idian/tempering/fusion. Mirrored 1:1 on the C# side.
	 */
	private static Item buildEquippableVariant(int objectId, int itemId, int mask, int desc, String creator,
			ItemGroup itemGroup, Integer itemColor) {
		try {
			ItemTemplate template = new ItemTemplate();
			setField(template, "itemId", itemId);
			setField(template, "mask", mask);
			setField(template, "description", desc);
			setField(template, "itemGroup", itemGroup);
			setField(template, "maxTuneCount", 0); // canTune() == false -> isIdentified() == true (deterministic)
			Item item = new Item(objectId, template);
			item.setItemCount(1L);
			item.setItemCreator(creator);
			if (itemColor != null)
				item.setItemColor(itemColor); // colorExpireTime stays 0 -> getColorTimeLeft() == 0 (deterministic)
			return item;
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Failed to build equippable variant Item", e);
		}
	}

	// ---- SM_VIEW_PLAYER_DETAILS (reuse the equippable-item / ItemInfoBlob seam) ----
	//
	// SM_VIEW_PLAYER_DETAILS(items, player) is bounded by the SAME seam as SM_INVENTORY_ADD_ITEM: the ctor reads ONLY
	// player.getObjectId() (a single AionObject scalar) + items.size(); writeImpl writes targetObjId + the constant 11 +
	// itemSize, then per item: writeD(0) + template.getTemplateId() + template.getL10n() + ItemInfoBlob.getFullBlob(player,
	// item).writeMe(). The player is passed to getFullBlob ONLY as the blob owner (the equippable weapon/armor/accessory/
	// enchant/premium/general writers never dereference it for these deterministic items — identical to the existing
	// SM_INVENTORY_ADD_ITEM seam). So no live Player/Legion/appearance/equipment-iteration is needed: the bounded live
	// Player is allocated uninitialized (Unsafe.allocateInstance, the established harness precedent — see SM_REPURCHASE/
	// SM_FIND_GROUP) with ONLY the final AionObject.objectId pinned. The items reuse the seam's exact builders, so the
	// per-item bytes are byte-identical to the weapon/armor/accessory fixtures already validated. Java is the oracle.
	private static final int VIEW_DETAILS_PLAYER_OBJECT_ID = 268900001;

	@Test
	public void generateGoldenViewPlayerDetailsFixture() throws IOException {
		Path outDir = repoRoot().resolve("parity-artifacts/golden/packets");
		Files.createDirectories(outDir);

		installItemCleanupSeam(); // GENERAL_INFO reads DataManager.ITEM_CLEAN_UP.hasAccountOrLegionWhStorabilityDisabled

		Player player = newUninitializedPlayer(VIEW_DETAILS_PLAYER_OBJECT_ID);

		List<Case> cases = new ArrayList<>();
		// Two-item view: the equippable 1H sword + the plate-torso armor (undyed), reusing the seam's exact builders, so
		// the per-item blobs are byte-identical to the SM_INVENTORY_ADD_ITEM weapon/armor fixtures.
		List<Item> items = new ArrayList<>();
		items.add(buildEquippableWeapon(EQ_ITEM_OBJECT_ID, EQ_ITEM_TEMPLATE_ID, EQ_ITEM_MASK, EQ_ITEM_DESC_L10N,
			EQ_ITEM_COUNT, EQ_ITEM_CREATOR));
		items.add(buildEquippableVariant(EQ_ARMOR_OBJECT_ID, EQ_ARMOR_TEMPLATE_ID, EQ_ARMOR_MASK, EQ_ARMOR_DESC_L10N,
			"Armorsmith", ItemGroup.PL_TORSO, null));
		String inputs = "{\"playerObjectId\":" + VIEW_DETAILS_PLAYER_OBJECT_ID + ",\"itemCount\":2}";
		cases.add(new Case("viewPlayerDetailsWeaponAndArmor", inputs, capture(new SM_VIEW_PLAYER_DETAILS(items, player), null)));

		writeFixture(outDir.resolve("SM_VIEW_PLAYER_DETAILS.json"), "SM_VIEW_PLAYER_DETAILS", cases);
	}

	// ---- item / ItemInfoBlob seam: more host packets (SM_EXCHANGE_ADD_ITEM, SM_WAREHOUSE_INFO/UPDATE_ITEM, SM_REPURCHASE) ----
	//
	// These four host packets wrap the SAME, already-byte-validated ItemInfoBlob seam (GENERAL_INFO on a non-equippable
	// item, EQUIPPED/SLOTS_WEAPON/ENCHANT/PREMIUM/GENERAL on the equippable weapon). Each writeImpl was verified to read
	// the Player ONLY as the getFullBlob blob owner (never dereferenced for these deterministic items) — NO con deref,
	// NO live World/Legion/Group. SM_LOOT_ITEMLIST is EXCLUDED here: its writeImpl reads con.getActivePlayer() (T2 audit).
	//   * SM_EXCHANGE_ADD_ITEM(action, item, player): writeC(action) + template id/objId + L10n + getFullBlob.writeMe.
	//   * SM_WAREHOUSE_INFO(items, warehouseType, expandLvl, firstPacket, player): the warehouse header (type/firstPacket/
	//     expand + REGULAR_WAREHOUSE-vs-other size prefix) then per item objId/templateId/writeC(0)/L10n/getFullBlob/
	//     equipmentSlot. Two cases pin both header branches (REGULAR_WAREHOUSE id=1 with items -> writeC(1)+writeC(0);
	//     other type -> writeH(0)).
	//   * SM_WAREHOUSE_UPDATE_ITEM(player, item, warehouseType, updateType): objId + warehouseType + L10n + a GENERAL_INFO-
	//     only blob (new ItemInfoBlob(player,item).addBlobEntry(GENERAL_INFO)) + (updateType.isSendable() ? writeH(mask)).
	//     DEC_ITEM_USE -> sendable, mask 0x16 (deterministic).
	//   * SM_REPURCHASE(player, npcId): its CTOR pulls the item collection from the RepurchaseService singleton, so the
	//     packet is allocated uninitialized (Unsafe.allocateInstance — the SM_VIEW_PLAYER_DETAILS/SM_FIND_GROUP precedent)
	//     and its final fields (player/targetObjectId/items) are reflectively pinned, dodging the singleton. writeImpl
	//     writes targetObjId + writeD(1) + size, then per item objId/templateId/L10n/getFullBlob + writeQ(repurchasePrice).
	// All built IDENTICALLY to the C# asserter side; Java is the oracle.
	private static final int EXCHANGE_ITEM_OBJECT_ID = 268510001;
	private static final int WH_INFO_ITEM_OBJECT_ID = 268510002;
	private static final int WH_UPDATE_ITEM_OBJECT_ID = 268510003;
	private static final int REPURCHASE_ITEM_OBJECT_ID = 268510004;
	private static final int REPURCHASE_NPC_OBJECT_ID = 700100200;
	private static final long REPURCHASE_PRICE = 123456789L;

	@Test
	public void generateGoldenExchangeAddItemFixture() throws IOException {
		Path outDir = repoRoot().resolve("parity-artifacts/golden/packets");
		Files.createDirectories(outDir);
		installItemCleanupSeam();

		List<Case> cases = new ArrayList<>();
		// action 0 (self) + action 1 (other), non-equippable item -> single GENERAL_INFO blob. player null (blob owner only).
		Item item = buildSimpleItem(EXCHANGE_ITEM_OBJECT_ID, ITEM_TEMPLATE_ID, ITEM_MASK, ITEM_DESC_L10N, ITEM_COUNT, ITEM_CREATOR);
		cases.add(new Case("exchangeAddItemSelf",
			"{\"action\":0,\"objectId\":" + EXCHANGE_ITEM_OBJECT_ID + ",\"itemId\":" + ITEM_TEMPLATE_ID + "}",
			capture(new SM_EXCHANGE_ADD_ITEM(0, item, null), null)));
		cases.add(new Case("exchangeAddItemOther",
			"{\"action\":1,\"objectId\":" + EXCHANGE_ITEM_OBJECT_ID + ",\"itemId\":" + ITEM_TEMPLATE_ID + "}",
			capture(new SM_EXCHANGE_ADD_ITEM(1, item, null), null)));

		writeFixture(outDir.resolve("SM_EXCHANGE_ADD_ITEM.json"), "SM_EXCHANGE_ADD_ITEM", cases);
	}

	@Test
	public void generateGoldenWarehouseInfoFixture() throws IOException {
		Path outDir = repoRoot().resolve("parity-artifacts/golden/packets");
		Files.createDirectories(outDir);
		installItemCleanupSeam();

		int regularType = StorageType.REGULAR_WAREHOUSE.getId();
		int otherType = StorageType.ACCOUNT_WAREHOUSE.getId();

		List<Case> cases = new ArrayList<>();
		// (a) REGULAR_WAREHOUSE with one item -> header writeC(1)+writeC(0); firstPacket true, expand 3.
		Item item = buildSimpleItem(WH_INFO_ITEM_OBJECT_ID, ITEM_TEMPLATE_ID, ITEM_MASK, ITEM_DESC_L10N, ITEM_COUNT, ITEM_CREATOR);
		List<Item> oneItem = new ArrayList<>();
		oneItem.add(item);
		cases.add(new Case("warehouseInfoRegularOneItem",
			"{\"warehouseType\":" + regularType + ",\"expandLvl\":3,\"firstPacket\":true,\"itemCount\":1}",
			capture(new SM_WAREHOUSE_INFO(oneItem, regularType, 3, true, null), null)));
		// (b) non-regular warehouse type, empty list -> header writeH(0); firstPacket false, expand 0.
		cases.add(new Case("warehouseInfoOtherEmpty",
			"{\"warehouseType\":" + otherType + ",\"expandLvl\":0,\"firstPacket\":false,\"itemCount\":0}",
			capture(new SM_WAREHOUSE_INFO(new ArrayList<>(), otherType, 0, false, null), null)));

		writeFixture(outDir.resolve("SM_WAREHOUSE_INFO.json"), "SM_WAREHOUSE_INFO", cases);
	}

	@Test
	public void generateGoldenWarehouseUpdateItemFixture() throws IOException {
		Path outDir = repoRoot().resolve("parity-artifacts/golden/packets");
		Files.createDirectories(outDir);
		installItemCleanupSeam();

		int regularType = StorageType.REGULAR_WAREHOUSE.getId();
		Item item = buildSimpleItem(WH_UPDATE_ITEM_OBJECT_ID, ITEM_TEMPLATE_ID, ITEM_MASK, ITEM_DESC_L10N, ITEM_COUNT, ITEM_CREATOR);

		List<Case> cases = new ArrayList<>();
		// DEC_ITEM_USE -> sendable, mask 0x16 -> trailing writeH(0x16). General-info-only blob. player null (blob owner only).
		cases.add(new Case("warehouseUpdateItemDecUse",
			"{\"warehouseType\":" + regularType + ",\"objectId\":" + WH_UPDATE_ITEM_OBJECT_ID + ",\"updateType\":\"DEC_ITEM_USE\"}",
			capture(new SM_WAREHOUSE_UPDATE_ITEM(null, item, regularType, ItemUpdateType.DEC_ITEM_USE), null)));

		writeFixture(outDir.resolve("SM_WAREHOUSE_UPDATE_ITEM.json"), "SM_WAREHOUSE_UPDATE_ITEM", cases);
	}

	@Test
	public void generateGoldenRepurchaseFixture() throws IOException {
		Path outDir = repoRoot().resolve("parity-artifacts/golden/packets");
		Files.createDirectories(outDir);
		installItemCleanupSeam();

		Item item = buildSimpleItem(REPURCHASE_ITEM_OBJECT_ID, ITEM_TEMPLATE_ID, ITEM_MASK, ITEM_DESC_L10N, ITEM_COUNT, ITEM_CREATOR);
		item.setRepurchasePrice(REPURCHASE_PRICE);

		List<Case> cases = new ArrayList<>();
		cases.add(new Case("repurchaseSingleItem",
			"{\"targetObjectId\":" + REPURCHASE_NPC_OBJECT_ID + ",\"objectId\":" + REPURCHASE_ITEM_OBJECT_ID
				+ ",\"repurchasePrice\":" + REPURCHASE_PRICE + ",\"itemCount\":1}",
			capture(buildRepurchasePacket(REPURCHASE_NPC_OBJECT_ID, Collections.singletonList(item)), null)));

		writeFixture(outDir.resolve("SM_REPURCHASE.json"), "SM_REPURCHASE", cases);
	}

	/**
	 * Allocate SM_REPURCHASE WITHOUT running its ctor (whose body pulls items from the RepurchaseService singleton —
	 * Unsafe.allocateInstance, the established harness precedent), then reflectively pin only the three final fields the
	 * writeImpl reads (player blob-owner null, targetObjectId, items). Mirrored 1:1 on the C# asserter side.
	 */
	private static SM_REPURCHASE buildRepurchasePacket(int targetObjectId, Collection<Item> items) {
		try {
			Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
			theUnsafe.setAccessible(true);
			Unsafe unsafe = (Unsafe) theUnsafe.get(null);
			SM_REPURCHASE packet = (SM_REPURCHASE) unsafe.allocateInstance(SM_REPURCHASE.class);
			setField(packet, "targetObjectId", targetObjectId);
			setField(packet, "items", items);
			return packet;
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Failed to allocate uninitialized SM_REPURCHASE", e);
		}
	}

	/**
	 * Allocate a Player WITHOUT running any constructor (Unsafe.allocateInstance — the established harness precedent for
	 * SM_REPURCHASE/SM_FIND_GROUP/SM_GROUP_MEMBER_INFO), then pin only the final AionObject.objectId. SM_VIEW_PLAYER_DETAILS
	 * reads nothing else from the live Player (getFullBlob only stashes it as blob owner).
	 */
	private static Player newUninitializedPlayer(int objectId) {
		try {
			Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
			theUnsafe.setAccessible(true);
			Unsafe unsafe = (Unsafe) theUnsafe.get(null);
			Player player = (Player) unsafe.allocateInstance(Player.class);
			Field idField = AionObject.class.getDeclaredField("objectId");
			long offset = unsafe.objectFieldOffset(idField);
			unsafe.putInt(player, offset, objectId);
			return player;
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Failed to allocate uninitialized Player", e);
		}
	}

	// ---- SM_TUNE_RESULT (item / ItemInfoBlob EnchantInfoBlobEntry.writeInfo seam) ----
	//
	// SM_TUNE_RESULT.writeImpl reads: targetItem.getObjectId(), the scalar tuningScrollItemId, result.getStatBonusId()
	// (PendingTuneResult is a pure DTO), then EnchantInfoBlobEntry.writeInfo(buf, targetItem, optionalSockets, enchantBonus)
	// — the ALREADY byte-validated ENCHANT_INFO writer on a bare simple-ctor Item (no skin/dye/stones/godstone/idian/
	// tempering) so every read is deterministic, and writeInfo's last two args come straight from the PendingTuneResult
	// scalars (not from the item's identify state). Then writeC(showManastoneSlots?0:1) + writeC(tuneCancelPossible?0:1),
	// both == !result.isAttributeOnly(). No con read, no live Player (the item-seam ITEM_CLEAN_UP is NOT touched —
	// writeInfo does not call hasAccountOrLegionWhStorabilityDisabled). Mirrored 1:1 on the C# asserter side.
	private static final int TUNE_ITEM_OBJECT_ID = 268520001;

	@Test
	public void generateGoldenTuneResultFixture() throws IOException {
		Path outDir = repoRoot().resolve("parity-artifacts/golden/packets");
		Files.createDirectories(outDir);

		List<Case> cases = new ArrayList<>();
		// (a) attributeOnly=false -> showManastoneSlots/tuneCancelPossible true -> writeC(0)/writeC(0); sockets 2, bonus 5, statBonusId 3.
		cases.add(tuneResultCase("tuneResultFull", 7551, 2, 5, 3, false));
		// (b) attributeOnly=true -> both false -> writeC(1)/writeC(1); sockets 0, bonus 0, statBonusId 9.
		cases.add(tuneResultCase("tuneResultAttributeOnly", 7552, 0, 0, 9, true));

		writeFixture(outDir.resolve("SM_TUNE_RESULT.json"), "SM_TUNE_RESULT", cases);
	}

	private static Case tuneResultCase(String name, int tuningScrollItemId, int optionalSockets, int enchantBonus,
			int statBonusId, boolean attributeOnly) {
		Item item = buildSimpleItem(TUNE_ITEM_OBJECT_ID, ITEM_TEMPLATE_ID, ITEM_MASK, ITEM_DESC_L10N, ITEM_COUNT, ITEM_CREATOR);
		PendingTuneResult result = new PendingTuneResult(optionalSockets, enchantBonus, statBonusId, attributeOnly);
		String inputs = "{\"objectId\":" + TUNE_ITEM_OBJECT_ID + ",\"tuningScrollItemId\":" + tuningScrollItemId
			+ ",\"optionalSockets\":" + optionalSockets + ",\"enchantBonus\":" + enchantBonus + ",\"statBonusId\":"
			+ statBonusId + ",\"attributeOnly\":" + attributeOnly + "}";
		return new Case(name, inputs, capture(new SM_TUNE_RESULT(item, tuningScrollItemId, result), null));
	}

	// ---- SM_SKILL_LIST (silent ctor, DataManager-free) ----
	//
	// SM_SKILL_LIST(List<PlayerSkillEntry>) is the silent ctor (silentUpdate=true, messageId=0) -> NO DataManager.SKILL_DATA
	// read (only the (skill,messageId) ctor reads SKILL_DATA.getSkillTemplate). writeImpl: writeH(size), writeC(1), per entry
	// SkillEntryWriter.writeSkillEntry (writeH skillId, writeH isNormalSkill?1:skillLevel, writeC 0, writeC professionSkillBarSize,
	// writeD isProfessionSkill?professionFlag:getFlag(), writeC skillType), then writeD(0) (messageId 0 -> no trailer).
	// Entries built via the DataManager-free PlayerSkillEntry(skillId, skillLvl, skillType, persistentState) ctor with skillType>0
	// (STIGMA) so isNormalSkill()==false -> getFlag()==0 (NO System.currentTimeMillis clock) and isProfessionSkill()==false
	// (skillId<30000) -> professionSkillBarSize 0. Fully deterministic. Mirrored 1:1 on the C# asserter side.
	@Test
	public void generateGoldenSkillListFixture() throws IOException {
		Path outDir = repoRoot().resolve("parity-artifacts/golden/packets");
		Files.createDirectories(outDir);

		List<Case> cases = new ArrayList<>();
		// Two stigma entries (skillType 1 and 3) + one normal-but-stigma-typed entry: all skillType>0 -> getFlag()==0.
		List<PlayerSkillEntry> skills = new ArrayList<>();
		skills.add(new PlayerSkillEntry(1001, 5, 1, PersistentState.NOACTION)); // stigma
		skills.add(new PlayerSkillEntry(1002, 12, 3, PersistentState.NOACTION)); // linked stigma
		String inputs = "{\"skills\":[{\"skillId\":1001,\"skillLvl\":5,\"skillType\":1},"
			+ "{\"skillId\":1002,\"skillLvl\":12,\"skillType\":3}]}";
		cases.add(new Case("skillListSilentStigma", inputs, capture(new SM_SKILL_LIST(skills), null)));

		writeFixture(outDir.resolve("SM_SKILL_LIST.json"), "SM_SKILL_LIST", cases);
	}

	// ---- SM_WAREHOUSE_ADD_ITEM (item / ItemInfoBlob seam, player == blob owner only) ----
	//
	// SM_WAREHOUSE_ADD_ITEM(item, warehouseType, player, addType).writeImpl reads ONLY: writeC(warehouseType),
	// writeH(addType.getMask()) (enum scalar), writeH(items.size()), then per item writeD(objectId)+writeD(templateId)+
	// writeC(0)+writeS(L10n)+getFullBlob(player,item).writeMe()+writeH(equipmentSlot&0xFFFF). The player is passed to
	// getFullBlob ONLY as the blob owner (never dereferenced for the deterministic non-equippable item) -> player null.
	// Same GENERAL_INFO-only item seam as SM_EXCHANGE_ADD_ITEM. Mirrored 1:1 on the C# asserter side.
	private static final int WH_ADD_ITEM_OBJECT_ID = 268520002;

	@Test
	public void generateGoldenWarehouseAddItemFixture() throws IOException {
		Path outDir = repoRoot().resolve("parity-artifacts/golden/packets");
		Files.createDirectories(outDir);
		installItemCleanupSeam();

		int regularType = StorageType.REGULAR_WAREHOUSE.getId();
		Item item = buildSimpleItem(WH_ADD_ITEM_OBJECT_ID, ITEM_TEMPLATE_ID, ITEM_MASK, ITEM_DESC_L10N, ITEM_COUNT, ITEM_CREATOR);

		List<Case> cases = new ArrayList<>();
		// ItemAddType.ALL_SLOT (mask 0x16) into a REGULAR_WAREHOUSE, single non-equippable item. player null (blob owner only).
		cases.add(new Case("warehouseAddItemAllSlot",
			"{\"warehouseType\":" + regularType + ",\"objectId\":" + WH_ADD_ITEM_OBJECT_ID + ",\"addType\":\"ALL_SLOT\"}",
			capture(new SM_WAREHOUSE_ADD_ITEM(item, regularType, null, ItemAddType.ALL_SLOT), null)));

		writeFixture(outDir.resolve("SM_WAREHOUSE_ADD_ITEM.json"), "SM_WAREHOUSE_ADD_ITEM", cases);
	}

	// ---- SM_INVENTORY_INFO (player-scalar seam: npc/quest/item expands) ----
	//
	// SM_INVENTORY_INFO(isFirstPacket, items, player).writeImpl reads: writeC(isFirstPacket?1:0), writeC(player.getNpcExpands()),
	// writeC(player.getQuestExpands()), writeC(player.getItemExpands()), writeH(items.size()), then per item
	// writeD(objectId)+writeD(templateId)+writeS(L10n)+getFullBlob(player,item).writeMe()+writeH(equipmentSlot&0xFFFF)+
	// writeC(isCloth?1:0). The ONLY live-Player reads are the three int cube-expand scalars (via getCommonData() ->
	// playerAccountData.getPlayerCommonData()); getFullBlob uses player only as blob owner. So the player is allocated
	// uninitialized (the established harness precedent) with ONLY its playerAccountData field pinned to a PlayerAccountData
	// whose playerCommonData carries the three pinned expand scalars. Non-equippable GENERAL_INFO-only item. Java is oracle.
	private static final int INV_INFO_ITEM_OBJECT_ID = 268520003;
	private static final int INV_INFO_NPC_EXPANDS = 3;
	private static final int INV_INFO_QUEST_EXPANDS = 2;
	private static final int INV_INFO_ITEM_EXPANDS = 4;

	@Test
	public void generateGoldenInventoryInfoFixture() throws IOException {
		Path outDir = repoRoot().resolve("parity-artifacts/golden/packets");
		Files.createDirectories(outDir);
		installItemCleanupSeam();

		Player player = newPlayerWithExpands(INV_INFO_NPC_EXPANDS, INV_INFO_QUEST_EXPANDS, INV_INFO_ITEM_EXPANDS);

		List<Case> cases = new ArrayList<>();
		// firstPacket true, one non-equippable item.
		Item item = buildSimpleItem(INV_INFO_ITEM_OBJECT_ID, ITEM_TEMPLATE_ID, ITEM_MASK, ITEM_DESC_L10N, ITEM_COUNT, ITEM_CREATOR);
		List<Item> oneItem = new ArrayList<>();
		oneItem.add(item);
		cases.add(new Case("inventoryInfoFirstOneItem",
			"{\"firstPacket\":true,\"npcExpands\":" + INV_INFO_NPC_EXPANDS + ",\"questExpands\":" + INV_INFO_QUEST_EXPANDS
				+ ",\"itemExpands\":" + INV_INFO_ITEM_EXPANDS + ",\"objectId\":" + INV_INFO_ITEM_OBJECT_ID + ",\"itemCount\":1}",
			capture(new SM_INVENTORY_INFO(true, oneItem, player), null)));
		// firstPacket false, empty item list (writeH 0).
		cases.add(new Case("inventoryInfoNotFirstEmpty",
			"{\"firstPacket\":false,\"npcExpands\":" + INV_INFO_NPC_EXPANDS + ",\"questExpands\":" + INV_INFO_QUEST_EXPANDS
				+ ",\"itemExpands\":" + INV_INFO_ITEM_EXPANDS + ",\"itemCount\":0}",
			capture(new SM_INVENTORY_INFO(false, new ArrayList<>(), player), null)));

		writeFixture(outDir.resolve("SM_INVENTORY_INFO.json"), "SM_INVENTORY_INFO", cases);
	}

	/**
	 * Allocate a Player WITHOUT running any constructor (Unsafe.allocateInstance — the established harness precedent), then
	 * pin ONLY its playerAccountData field to a PlayerAccountData (also allocated uninitialized to dodge the appearance-
	 * deref ctor) whose playerCommonData carries the three pinned cube-expand scalars. SM_INVENTORY_INFO reads nothing else
	 * from the live Player. Mirrored 1:1 on the C# asserter side.
	 */
	private static Player newPlayerWithExpands(int npcExpands, int questExpands, int itemExpands) {
		try {
			Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
			theUnsafe.setAccessible(true);
			Unsafe unsafe = (Unsafe) theUnsafe.get(null);

			PlayerCommonData pcd = new PlayerCommonData(INV_INFO_ITEM_OBJECT_ID);
			pcd.setNpcExpands(npcExpands);
			pcd.setQuestExpands(questExpands);
			pcd.setItemExpands(itemExpands);

			PlayerAccountData accountData = (PlayerAccountData) unsafe.allocateInstance(PlayerAccountData.class);
			setField(accountData, "playerCommonData", pcd);

			Player player = (Player) unsafe.allocateInstance(Player.class);
			setField(player, "playerAccountData", accountData);
			return player;
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Failed to build Player with expands", e);
		}
	}

	// ---- SKILL_DATA holder seam ----

	/**
	 * Populate DataManager.SKILL_DATA with exactly the one skill template SM_SKILL_COOLDOWN reads (id + raw cooldown).
	 * Built WITHOUT JAXB by reflectively setting the skillTemplateById index, so no XML/file is touched — the bounded
	 * holder seam (mirrors the WORLD_MAPS_DATA seam above).
	 */
	private void installSkillDataSeam() {
		try {
			SkillData data = new SkillData();
			@SuppressWarnings("unchecked")
			Map<Integer, SkillTemplate> byId = (Map<Integer, SkillTemplate>) getField(data, "skillTemplateById");
			byId.clear();
			byId.put(COOLDOWN_SKILL_ID, skillTemplate(COOLDOWN_SKILL_ID, COOLDOWN_RAW));
			DataManager.SKILL_DATA = data;
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Failed to install SKILL_DATA seam", e);
		}
	}

	private static SkillTemplate skillTemplate(int skillId, int cooldown) throws ReflectiveOperationException {
		Constructor<SkillTemplate> ctor = SkillTemplate.class.getDeclaredConstructor();
		ctor.setAccessible(true);
		SkillTemplate t = ctor.newInstance();
		setField(t, "skillId", skillId);
		setField(t, "cooldown", cooldown);
		return t;
	}

	// ---- QUEST_DATA holder seam ----

	/**
	 * Populate DataManager.QUEST_DATA with exactly the two quest templates SM_QUEST_ACTION reads (one NONE category,
	 * one extra category). Built WITHOUT JAXB by reflectively setting the questTemplates index — the bounded holder seam.
	 */
	private void installQuestDataSeam() {
		try {
			QuestsData data = new QuestsData();
			@SuppressWarnings("unchecked")
			Map<Integer, QuestTemplate> byId = (Map<Integer, QuestTemplate>) getField(data, "questTemplates");
			byId.clear();
			byId.put(QUEST_ID_NONE, questTemplate(QUEST_ID_NONE, QuestExtraCategory.NONE));
			byId.put(QUEST_ID_EXTRA, questTemplate(QUEST_ID_EXTRA, QuestExtraCategory.COIN_QUEST));
			DataManager.QUEST_DATA = data;
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Failed to install QUEST_DATA seam", e);
		}
	}

	private static QuestTemplate questTemplate(int id, QuestExtraCategory extraCategory)
			throws ReflectiveOperationException {
		Constructor<QuestTemplate> ctor = QuestTemplate.class.getDeclaredConstructor();
		ctor.setAccessible(true);
		QuestTemplate t = ctor.newInstance();
		setField(t, "id", id);
		setField(t, "extraCategory", extraCategory);
		return t;
	}

	private static Case teleportCase(String name, int mapId, int instanceId, float x, float y, float z, byte heading,
			TeleportAnimation portAnimation) {
		String inputs = "{\"mapId\":" + mapId + ",\"instanceId\":" + instanceId + ",\"x\":" + x + ",\"y\":" + y
			+ ",\"z\":" + z + ",\"heading\":" + heading + ",\"portAnimation\":" + portAnimation.getId() + "}";
		return new Case(name, inputs,
			capture(new SM_TELEPORT_LOC(mapId, instanceId, x, y, z, heading, portAnimation), null));
	}

	// ---- World/instance DataManager seam ----

	/**
	 * Populate DataManager.WORLD_MAPS_DATA with exactly the two map templates SM_TELEPORT_LOC reads (one non-instance,
	 * one instance). Built WITHOUT JAXB by reflectively setting the mapsById index, so no XML/file/ZoneService is
	 * touched — the bounded holder seam.
	 */
	private void installWorldMapsSeam() {
		try {
			WorldMapsData data = new WorldMapsData();
			@SuppressWarnings("unchecked")
			Map<Integer, WorldMapTemplate> mapsById = (Map<Integer, WorldMapTemplate>) getField(data, "mapsById");
			mapsById.clear();
			mapsById.put(REGULAR_MAP_ID, worldMapTemplate(REGULAR_MAP_ID, "MORHEIM", false));
			mapsById.put(INSTANCE_MAP_ID, worldMapTemplate(INSTANCE_MAP_ID, "DRAUPNIR_CAVE", true));
			DataManager.WORLD_MAPS_DATA = data;
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Failed to install WORLD_MAPS_DATA seam", e);
		}
	}

	private static WorldMapTemplate worldMapTemplate(int mapId, String name, boolean instance)
			throws ReflectiveOperationException {
		Constructor<WorldMapTemplate> ctor = WorldMapTemplate.class.getDeclaredConstructor();
		ctor.setAccessible(true);
		WorldMapTemplate t = ctor.newInstance();
		setField(t, "mapId", mapId);
		setField(t, "name", name);
		setField(t, "instance", instance);
		return t;
	}

	private static Object getField(Object target, String name) throws ReflectiveOperationException {
		Field f = target.getClass().getDeclaredField(name);
		f.setAccessible(true);
		return f.get(target);
	}

	private static void setField(Object target, String name, Object value) throws ReflectiveOperationException {
		Field f = target.getClass().getDeclaredField(name);
		f.setAccessible(true);
		f.set(target, value);
	}

	/** Capture the payload bytes a packet's writeImpl produces (no opcode, no crypt) for the given connection. */
	private static String capture(AionServerPacket packet, AionConnection con) {
		try {
			ByteBuffer buffer = ByteBuffer.allocate(8192).order(ByteOrder.LITTLE_ENDIAN);
			packet.setBuf(buffer);
			Method writeImpl = AionServerPacket.class.getDeclaredMethod("writeImpl", AionConnection.class);
			writeImpl.setAccessible(true);
			writeImpl.invoke(packet, con);
			byte[] payload = new byte[buffer.position()];
			buffer.flip();
			buffer.get(payload);
			return toHex(payload);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Failed to capture " + packet.getClass().getSimpleName(), e);
		}
	}

	// ---- fixture writing ----

	private static void writeFixture(Path file, String packet, List<Case> cases) throws IOException {
		StringBuilder sb = new StringBuilder();
		sb.append("{\n");
		sb.append("  \"schemaVersion\": 1,\n");
		sb.append("  \"packet\": \"").append(packet).append("\",\n");
		sb.append("  \"opcode\": null,\n");
		sb.append("  \"source\": \"Java\",\n");
		sb.append("  \"cases\": [\n");
		for (int i = 0; i < cases.size(); i++) {
			Case c = cases.get(i);
			sb.append("    {\n");
			sb.append("      \"name\": \"").append(c.name).append("\",\n");
			sb.append("      \"inputs\": ").append(c.inputsJson).append(",\n");
			sb.append("      \"payloadHex\": \"").append(c.payloadHex).append("\"\n");
			sb.append("    }").append(i + 1 < cases.size() ? "," : "").append("\n");
		}
		sb.append("  ]\n");
		sb.append("}\n");
		Files.write(file, sb.toString().getBytes(StandardCharsets.UTF_8));
	}

	private static String toHex(byte[] bytes) {
		char[] out = new char[bytes.length * 2];
		for (int i = 0; i < bytes.length; i++) {
			out[i * 2] = HEX[(bytes[i] >> 4) & 0xF];
			out[i * 2 + 1] = HEX[bytes[i] & 0xF];
		}
		return new String(out);
	}

	private static Path repoRoot() {
		Path dir = Paths.get("").toAbsolutePath();
		while (dir != null && !Files.isDirectory(dir.resolve("parity-artifacts"))) {
			dir = dir.getParent();
		}
		return dir != null ? dir : Paths.get("").toAbsolutePath();
	}

	private static final class Case {
		final String name;
		final String inputsJson;
		final String payloadHex;

		Case(String name, String inputsJson, String payloadHex) {
			this.name = name;
			this.inputsJson = inputsJson;
			this.payloadHex = payloadHex;
		}
	}
}
