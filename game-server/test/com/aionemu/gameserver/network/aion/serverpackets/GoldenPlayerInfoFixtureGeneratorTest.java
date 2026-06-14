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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.controllers.movement.PlayerMoveController;
import com.aionemu.gameserver.dataholders.AbsoluteStatsData;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.Gender;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.account.Account;
import com.aionemu.gameserver.model.account.PlayerAccountData;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.AbyssRank;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerAppearance;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.gameobjects.player.PlayerSettings;
import com.aionemu.gameserver.model.stats.calc.AdditionStat;
import com.aionemu.gameserver.model.stats.calc.Stat2;
import com.aionemu.gameserver.model.stats.container.PlayerGameStats;
import com.aionemu.gameserver.model.stats.container.PlayerLifeStats;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.model.templates.VisibleObjectTemplate;
import com.aionemu.gameserver.model.templates.stats.StatsTemplate;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.world.WorldPosition;

/**
 * INTEGRATION golden harness for SM_PLAYER_INFO — the enter-world visible-player definition packet (the single most
 * important and most complex player serialization). This extends the reusable SM_STATS_INFO integration seam
 * (HarnessPlayer/HarnessStats/HarnessLifeStats + DataManager bridge) with the extra deterministic stubs SM_PLAYER_INFO
 * needs and serializes SM_PLAYER_INFO.writeImpl byte-for-byte. The C# asserter (GoldenPlayerInfoFixtureTests) rebuilds
 * the structurally identical player + connection and asserts identical bytes. Java is the single source of truth.
 *
 * <p>Extra determinism seams beyond SM_STATS_INFO (all identical on both sides):</p>
 * <ul>
 * <li>active player: SM_PLAYER_INFO reads con.getActivePlayer(); we allocate an uninitialized AionConnection (no socket)
 * and set its activePlayer field to a second HarnessPlayer (same race) so isEnemy is deterministically false.</li>
 * <li>isEnemy: HarnessPlayer overrides isEnemy(Creature) -> false (avoids World/Knownlist/pvp-zone), so raceId is the
 * player's own race id.</li>
 * <li>position: a fixed WorldPosition (mapId/x/y/z/heading) so getX/getY/getZ/getHeading/getWorldId are deterministic and
 * mapId is NOT a conqueror/protector world (CP system disabled by default anyway -> cpInfo == null -> ranks 0).</li>
 * <li>objectTemplate: HarnessPlayer.getObjectTemplate returns a fixed stub (templateId == pcd.getTemplateId()) so the
 * transform model's getModelId() is deterministic with no transform active.</li>
 * <li>abyssRank: a fixed AbyssRank(rank id 1 = GRADE9_SOLDIER); playerSettings: default new PlayerSettings() (display 0,
 * deny 0); houses: empty list (getActiveHouse == null); pcd name/note/race/gender pinned; legion/store/target/flight/
 * casting/mentor all absent -> default branches.</li>
 * </ul>
 *
 * Regenerate with:
 *   mvn -pl game-server -am test -Dtest=GoldenPlayerInfoFixtureGeneratorTest -Dmaven.test.skip=false -Dsurefire.failIfNoSpecifiedTests=false
 */
public class GoldenPlayerInfoFixtureGeneratorTest {

	private static final char[] HEX = "0123456789ABCDEF".toCharArray();

	// Identical-on-both-sides experience table (same shape as the SM_STATS_INFO harness; the faithful Player ctor only
	// needs ABSOLUTE_STATS_DATA, but we register the table too so the DataManager bridge matches the C# side exactly).
	static final long[] EXP_TABLE = buildExpTable();

	private static long[] buildExpTable() {
		long[] t = new long[67];
		for (int i = 0; i < t.length; i++) {
			long n = i;
			t[i] = 100L * n * n * n + 1000L * n;
		}
		return t;
	}

	@Test
	public void generateGoldenPlayerInfoFixtures() throws IOException {
		Path outDir = repoRoot().resolve("parity-artifacts/golden/packets");
		Files.createDirectories(outDir);

		installIntegrationSeam();

		List<Case> cases = new ArrayList<>();

		// Case 1: low-level Elyos warrior, male, no equipment/legion/store, same-race active player (not enemy).
		{
			TreeMap<StatEnum, Integer> stats = new TreeMap<>();
			stats.put(StatEnum.MAXHP, 1200);
			stats.put(StatEnum.MAXMP, 800);
			stats.put(StatEnum.FLY_TIME, 60);
			stats.put(StatEnum.MAXDP, 4000);
			PlayerSpec spec = new PlayerSpec(100001, (byte) 1, Race.ELYOS, Gender.MALE, PlayerClass.WARRIOR, stats);
			spec.name = "Aionwarrior";
			spec.note = "hello";
			spec.dp = 0;
			spec.currentHp = 1200;
			spec.currentMp = 800;
			spec.currentFp = 60;
			spec.mapId = 220020000;
			spec.x = 100.5f;
			spec.y = 200.25f;
			spec.z = 300.75f;
			spec.heading = (byte) 30;
			cases.add(buildCase("lowLevelElyosNoEquip", spec));
		}

		// Case 2: mid-level Asmodian gladiator, female, dp > 0, different name/note/position.
		{
			TreeMap<StatEnum, Integer> stats = new TreeMap<>();
			stats.put(StatEnum.MAXHP, 12345);
			stats.put(StatEnum.MAXMP, 6789);
			stats.put(StatEnum.MAXDP, 4000);
			stats.put(StatEnum.FLY_TIME, 70);
			PlayerSpec spec = new PlayerSpec(200002, (byte) 25, Race.ASMODIANS, Gender.FEMALE, PlayerClass.GLADIATOR, stats);
			spec.name = "Darkgladiatrix";
			spec.note = "for the daevas";
			spec.dp = 2500;
			spec.currentHp = 9000;
			spec.currentMp = 4000;
			spec.currentFp = 50;
			spec.mapId = 210020000;
			spec.x = 1500.0f;
			spec.y = 2500.5f;
			spec.z = 412.125f;
			spec.heading = (byte) 90;
			cases.add(buildCase("midLevelAsmoFemale", spec));
		}

		writeFixture(outDir.resolve("SM_PLAYER_INFO.json"), "SM_PLAYER_INFO", cases);
	}

	// ---- integration seam ----

	private void installIntegrationSeam() {
		CustomConfig.BASE_FLYTIME = 60;
		if (DataManager.ABSOLUTE_STATS_DATA == null)
			DataManager.ABSOLUTE_STATS_DATA = new AbsoluteStatsData();
		setExperienceTable(EXP_TABLE);
		// AionConnection's static initializer builds a PacketProcessor that requires positive thread counts;
		// load the default network/thread config values before that class is referenced.
		com.aionemu.commons.configuration.ConfigurableProcessor.process(new java.util.Properties(),
			com.aionemu.gameserver.configs.network.NetworkConfig.class,
			com.aionemu.gameserver.configs.main.ThreadConfig.class,
			com.aionemu.gameserver.configs.network.PffConfig.class);
		// getName(true) reads AdminConfig.NAME_TAGS.length; account access level 0 -> index -1 out of range -> pcd name.
		// Both sides only require a non-empty array here; pin a fixed one identical to the C# asserter.
		com.aionemu.gameserver.configs.administration.AdminConfig.NAME_TAGS = new String[] { "%s" };
		installDbStub();
	}

	/**
	 * Inject a stub DataSource into DatabaseFactory so DAOs invoked by the faithful Player ctor (e.g. PlayerPetsDAO via
	 * PetList) resolve a deterministic empty result with no real DB. The stub's getConnection() throws SQLException,
	 * which the DAOs catch and treat as "no rows". Java-test-only; never touches src DB code. Same seam the SM_STATS_INFO
	 * harness installs for GameTime; the C# asserter uses its default-DI / empty-DAO path.
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
						if ("getConnection".equals(method.getName()))
							throw new java.sql.SQLException("harness stub: no database");
						if ("toString".equals(method.getName()))
							return "HarnessStubDataSource";
						if ("hashCode".equals(method.getName()))
							return System.identityHashCode(proxy);
						if ("equals".equals(method.getName()))
							return proxy == args[0];
						Class<?> rt = method.getReturnType();
						if (rt == boolean.class)
							return false;
						if (rt == int.class)
							return 0;
						return null;
					});
				dataSourceField.set(null, stub);
			}
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Failed to install DB stub", e);
		}
	}

	private static void setExperienceTable(long[] table) {
		try {
			com.aionemu.gameserver.dataholders.PlayerExperienceTable pxt = new com.aionemu.gameserver.dataholders.PlayerExperienceTable();
			Field f = com.aionemu.gameserver.dataholders.PlayerExperienceTable.class.getDeclaredField("experience");
			f.setAccessible(true);
			f.set(pxt, table.clone());
			DataManager.PLAYER_EXPERIENCE_TABLE = pxt;
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Failed to install PLAYER_EXPERIENCE_TABLE fixture", e);
		}
	}

	// ---- case build ----

	private static Case buildCase(String name, PlayerSpec spec) {
		HarnessPlayer player = new HarnessPlayer(spec);
		// second live player on the connection (same race -> not enemy via the deterministic isEnemy override).
		HarnessPlayer activePlayer = new HarnessPlayer(spec.activePlayerSpec());
		AionConnection con = newConnectionWithActivePlayer(activePlayer);
		return new Case(name, spec.toJson(), capture(new SM_PLAYER_INFO(player, false), con));
	}

	/** Allocate an uninitialized AionConnection (no socket) and pin its activePlayer field to the given player. */
	private static AionConnection newConnectionWithActivePlayer(Player activePlayer) {
		try {
			Field theUnsafe = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
			theUnsafe.setAccessible(true);
			Object unsafe = theUnsafe.get(null);
			Method allocate = unsafe.getClass().getMethod("allocateInstance", Class.class);
			AionConnection con = (AionConnection) allocate.invoke(unsafe, AionConnection.class);
			Field apField = AionConnection.class.getDeclaredField("activePlayer");
			apField.setAccessible(true);
			apField.set(con, new AtomicReference<>(activePlayer));
			return con;
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Failed to build harness AionConnection", e);
		}
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

	// ---- player spec + harness ----

	static final class PlayerSpec {
		final int objectId;
		final byte level;
		final Race race;
		final Gender gender;
		final PlayerClass playerClass;
		final TreeMap<StatEnum, Integer> statMap;
		String name;
		String note;
		int dp;
		int currentHp;
		int currentMp;
		int currentFp;
		int mapId;
		float x, y, z;
		byte heading;

		PlayerSpec(int objectId, byte level, Race race, Gender gender, PlayerClass playerClass, TreeMap<StatEnum, Integer> statMap) {
			this.objectId = objectId;
			this.level = level;
			this.race = race;
			this.gender = gender;
			this.playerClass = playerClass;
			this.statMap = statMap;
		}

		/** A deterministic same-race active player (distinct object id) used only as con.getActivePlayer(). */
		PlayerSpec activePlayerSpec() {
			TreeMap<StatEnum, Integer> s = new TreeMap<>();
			s.put(StatEnum.MAXHP, 100);
			s.put(StatEnum.MAXMP, 100);
			s.put(StatEnum.FLY_TIME, 60);
			s.put(StatEnum.MAXDP, 4000);
			PlayerSpec ap = new PlayerSpec(objectId + 1, (byte) 1, race, Gender.MALE, PlayerClass.WARRIOR, s);
			ap.name = "ActiveViewer";
			ap.note = "";
			ap.currentHp = 100;
			ap.currentMp = 100;
			ap.currentFp = 60;
			ap.mapId = mapId;
			ap.x = 0f;
			ap.y = 0f;
			ap.z = 0f;
			ap.heading = 0;
			return ap;
		}

		String toJson() {
			StringBuilder sb = new StringBuilder();
			sb.append("{");
			sb.append("\"objectId\":").append(objectId);
			sb.append(",\"level\":").append(level);
			sb.append(",\"race\":\"").append(race.name()).append("\"");
			sb.append(",\"gender\":\"").append(gender.name()).append("\"");
			sb.append(",\"playerClass\":\"").append(playerClass.name()).append("\"");
			sb.append(",\"name\":\"").append(name).append("\"");
			sb.append(",\"note\":\"").append(note).append("\"");
			sb.append(",\"dp\":").append(dp);
			sb.append(",\"currentHp\":").append(currentHp);
			sb.append(",\"currentMp\":").append(currentMp);
			sb.append(",\"currentFp\":").append(currentFp);
			sb.append(",\"mapId\":").append(mapId);
			sb.append(",\"x\":").append(x);
			sb.append(",\"y\":").append(y);
			sb.append(",\"z\":").append(z);
			sb.append(",\"heading\":").append(heading);
			sb.append(",\"stats\":{");
			int j = 0;
			for (Map.Entry<StatEnum, Integer> e : statMap.entrySet()) {
				if (j++ > 0)
					sb.append(",");
				sb.append("\"").append(e.getKey().name()).append("\":").append(e.getValue());
			}
			sb.append("}}");
			return sb.toString();
		}
	}

	static final class HarnessPlayer extends Player {
		private byte level;
		private Race race;
		private final VisibleObjectTemplate stubTemplate;

		HarnessPlayer(PlayerSpec spec) {
			super(minimalAccountData(spec), new Account(1));
			this.level = spec.level;
			this.race = spec.race;
			this.stubTemplate = new StubTemplate(getCommonData().getTemplateId());
			setGameStats(new HarnessStats(this, spec.statMap));
			HarnessLifeStats ls = new HarnessLifeStats(this);
			ls.pin(spec.currentHp, spec.currentMp, spec.currentFp);
			setLifeStats(ls);
			this.moveController = new PlayerMoveController(this); // movementMask defaults to 0
			setPosition(new WorldPosition(spec.mapId, spec.x, spec.y, spec.z, spec.heading));
			setAbyssRank(fixedAbyssRank()); // rank id 1 = GRADE9_SOLDIER (no doUpdate -> no ServerTime dependency)
			setPlayerSettings(new PlayerSettings()); // display 0, deny 0
			pinEmptyHouses(this);
			pinCommonData(getCommonData(), spec);
		}

		@Override
		public byte getLevel() { return level; }

		@Override
		public Race getRace() { return race; }

		@Override
		public VisibleObjectTemplate getObjectTemplate() { return stubTemplate; }

		// Deterministic relation: same-race viewer is never an enemy; avoids World/Knownlist/pvp-zone evaluation.
		@Override
		public boolean isEnemy(Creature creature) { return false; }

		private static PlayerAccountData minimalAccountData(PlayerSpec spec) {
			PlayerCommonData common = new PlayerCommonData(spec.objectId);
			common.setPlayerClass(spec.playerClass);
			common.setRace(spec.race);
			common.setGender(spec.gender);
			common.setName(spec.name);
			common.setNote(spec.note);
			return new PlayerAccountData(common, new PlayerAppearance());
		}
	}

	/** Minimal fixed object template so transformModel.getModelId() resolves a deterministic templateId. */
	static final class StubTemplate extends VisibleObjectTemplate {
		private final int templateId;

		StubTemplate(int templateId) { this.templateId = templateId; }

		@Override
		public int getTemplateId() { return templateId; }

		@Override
		public String getName() { return "Harness"; }

		@Override
		public int getL10nId() { return 0; }
	}

	private static void pinCommonData(PlayerCommonData pcd, PlayerSpec spec) {
		setIntField(pcd, "dp", spec.dp);
		setIntField(pcd, "level", spec.level);
	}

	/**
	 * Build an AbyssRank pinned to GRADE9_SOLDIER (id 1) WITHOUT running its ctor, whose doUpdate() depends on the
	 * server time zone (GSConfig.TIME_ZONE_ID, unconfigured in the harness). The packet only reads getRank().getId().
	 */
	private static AbyssRank fixedAbyssRank() {
		try {
			Field theUnsafe = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
			theUnsafe.setAccessible(true);
			Object unsafe = theUnsafe.get(null);
			Method allocate = unsafe.getClass().getMethod("allocateInstance", Class.class);
			AbyssRank rank = (AbyssRank) allocate.invoke(unsafe, AbyssRank.class);
			Field rankField = AbyssRank.class.getDeclaredField("rank");
			rankField.setAccessible(true);
			rankField.set(rank, com.aionemu.gameserver.utils.stats.AbyssRankEnum.GRADE9_SOLDIER);
			return rank;
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Failed to build fixed AbyssRank", e);
		}
	}

	private static void pinEmptyHouses(Player player) {
		try {
			Field f = Player.class.getDeclaredField("houses");
			f.setAccessible(true);
			f.set(player, new ArrayList<>());
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Failed to pin empty houses", e);
		}
	}

	private static void setIntField(Object target, String name, int value) {
		try {
			Field f = PlayerCommonData.class.getDeclaredField(name);
			f.setAccessible(true);
			f.setInt(target, value);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Failed to pin " + name, e);
		}
	}

	static final class HarnessStats extends PlayerGameStats {
		private final Map<StatEnum, Integer> statMap;

		HarnessStats(Player owner, Map<StatEnum, Integer> statMap) {
			super(owner);
			this.statMap = statMap;
		}

		@Override
		public Stat2 getStat(StatEnum statEnum, float base, com.aionemu.gameserver.utils.stats.CalculationType... calculationTypes) {
			float resolved = statMap.containsKey(statEnum) ? statMap.get(statEnum) : base;
			return new AdditionStat(statEnum, resolved, owner);
		}

		@Override public StatsTemplate getStatsTemplate() { return new StatsTemplate(); }
		@Override public Stat2 getAttackSpeed() { return new AdditionStat(StatEnum.ATTACK_SPEED, 1000, owner); }
		@Override public Stat2 getMovementSpeed() { return new AdditionStat(StatEnum.SPEED, 6000, owner); }
		@Override public Stat2 getAttackRange() { return new AdditionStat(StatEnum.ATTACK_RANGE, 1500, owner); }
		@Override public Stat2 getHpRegenRate() { return new AdditionStat(StatEnum.REGEN_HP, 1, owner); }
		@Override public Stat2 getMpRegenRate() { return new AdditionStat(StatEnum.REGEN_MP, 1, owner); }
	}

	static final class HarnessLifeStats extends PlayerLifeStats {
		private int hp;
		private int mp;
		private int fp;

		HarnessLifeStats(Player owner) {
			super(owner);
		}

		void pin(int hp, int mp, int fp) {
			this.hp = hp;
			this.mp = mp;
			this.fp = fp;
		}

		@Override public int getCurrentHp() { return hp; }
		@Override public int getCurrentMp() { return mp; }
		@Override public int getCurrentFp() { return fp; }

		@Override
		public void triggerRestoreTask() {
			// no-op: no world/scheduler in the harness
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
