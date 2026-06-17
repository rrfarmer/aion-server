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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import sun.misc.Unsafe;

import com.aionemu.gameserver.controllers.NpcController;
import com.aionemu.gameserver.controllers.movement.MovementMask;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.HouseData;
import com.aionemu.gameserver.dataholders.ItemRestrictionCleanupData;
import com.aionemu.gameserver.dataholders.NpcSkillData;
import com.aionemu.gameserver.dataholders.QuestsData;
import com.aionemu.gameserver.dataholders.SkillData;
import com.aionemu.gameserver.dataholders.TradeListData;
import com.aionemu.gameserver.dataholders.WorldMapsData;
import com.aionemu.gameserver.model.CreatureType;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.model.templates.item.enums.ItemGroup;
import com.aionemu.gameserver.services.item.ItemPacketService.ItemAddType;
import com.aionemu.gameserver.services.item.ItemPacketService.ItemUpdateType;
import com.aionemu.gameserver.model.animations.TeleportAnimation;
import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.Npc;
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
