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
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.HouseData;
import com.aionemu.gameserver.dataholders.NpcSkillData;
import com.aionemu.gameserver.dataholders.QuestsData;
import com.aionemu.gameserver.dataholders.SkillData;
import com.aionemu.gameserver.dataholders.WorldMapsData;
import com.aionemu.gameserver.model.CreatureType;
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
