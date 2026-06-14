package com.aionemu.gameserver.network.aion.serverpackets;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
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
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.controllers.movement.PlayerMoveController;
import com.aionemu.gameserver.dataholders.AbsoluteStatsData;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.PlayerExperienceTable;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.account.Account;
import com.aionemu.gameserver.model.account.PlayerAccountData;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerAppearance;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.stats.calc.AdditionStat;
import com.aionemu.gameserver.model.stats.calc.Stat2;
import com.aionemu.gameserver.model.stats.container.PlayerGameStats;
import com.aionemu.gameserver.model.stats.container.PlayerLifeStats;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.model.templates.stats.StatsTemplate;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * INTEGRATION golden harness for SM_STATS_INFO — the full-Player enter-world stat sheet.
 *
 * <p>This is the first golden case that needs the integration seam (DB/GameTime stub + PLAYER_EXPERIENCE_TABLE
 * fixture + raw exp/dp/repose/salvation field pinning) on top of the existing deterministic HarnessPlayer/HarnessStats
 * (the same getStat fixed-map seam used by the combat-formula golden, 0 bugs). It serializes SM_STATS_INFO.writeImpl
 * byte-for-byte; the C# asserter (GoldenStatsInfoFixtureTests) rebuilds the structurally identical player and asserts
 * identical bytes. Java is the single source of truth.</p>
 *
 * <p>Determinism seams:</p>
 * <ul>
 * <li>GameTime: SM_STATS_INFO writes GameTimeService.getInstance().getGameTime().getTime(). The lazy singleton builds
 * GameTime(ServerVariablesDAO.loadInt("time")) -> DatabaseFactory.getConnection() which NPEs on a null dataSource and
 * escapes load()'s SQLException-only catch. We inject a stub DataSource (Proxy whose getConnection throws SQLException)
 * so load() catches it and returns null -> GameTime(null) -> time 0. C# constructs its DI GameTimeService with the
 * default 0 minutes. Both write time = 0.</li>
 * <li>Stats: HarnessStats resolves a fixed value per StatEnum (else the passed base); every typed getter routes through
 * getStat, so current == base == map-value per stat, fully deterministic with no equipment/world.</li>
 * <li>Exp/level/dp/repose/salvation: pinned via reflection on PlayerCommonData (identical lowercase fields on both
 * sides) plus an IDENTICAL minimal PLAYER_EXPERIENCE_TABLE fixture (so getExpNeed/getExpShown read the same table).</li>
 * <li>currentHp/currentMp/currentFp: pinned via reflection on the life-stats.</li>
 * <li>flyState=0, movementMask=0, inventory limit=27(CUBE)/size=0: faithful base-ctor defaults.</li>
 * </ul>
 *
 * Regenerate with:
 *   mvn -pl game-server -am test -Dtest=GoldenStatsInfoFixtureGeneratorTest -Dmaven.test.skip=false -Dsurefire.failIfNoSpecifiedTests=false
 */
public class GoldenStatsInfoFixtureGeneratorTest {

	private static final char[] HEX = "0123456789ABCDEF".toCharArray();

	// Minimal, identical-on-both-sides experience table: exp[level-1] = start exp for that level.
	// 67 monotonically-increasing entries (covers max level 66 + headroom) kept within long range.
	// exp[i] = 100 * i^3 + 1000 * i  (strictly increasing, deterministic, well below Long.MAX).
	static final long[] EXP_TABLE = buildExpTable();

	private static long[] buildExpTable() {
		long[] t = new long[67];
		for (int i = 0; i < t.length; i++) {
			long n = i; // exp[0] = 0
			t[i] = 100L * n * n * n + 1000L * n;
		}
		return t;
	}

	@Test
	public void generateGoldenStatsInfoFixtures() throws IOException {
		Path outDir = repoRoot().resolve("parity-artifacts/golden/packets");
		Files.createDirectories(outDir);

		installIntegrationSeam();

		List<Case> cases = new ArrayList<>();

		// Case 1: low-level, mostly-empty player (exercises 0/default branches).
		{
			TreeMap<StatEnum, Integer> stats = new TreeMap<>();
			stats.put(StatEnum.MAXHP, 1200);
			stats.put(StatEnum.MAXMP, 800);
			stats.put(StatEnum.FLY_TIME, 60);
			stats.put(StatEnum.MAXDP, 4000);
			PlayerSpec spec = new PlayerSpec(100001, (byte) 1, Race.ELYOS, PlayerClass.WARRIOR, stats);
			spec.exp = 50L;          // level-1 start = 0 -> expShown = 50
			spec.expRecoverable = 0L;
			spec.dp = 0;
			spec.reposeCurrent = 0L;
			spec.reposeMax = 0L;
			spec.salvationPoint = 0L;
			spec.currentHp = 1200;
			spec.currentMp = 800;
			spec.currentFp = 60;
			cases.add(buildCase("lowLevelEmpty", spec));
		}

		// Case 2: mid-level, varied stats + dp + repose + salvation.
		{
			TreeMap<StatEnum, Integer> stats = new TreeMap<>();
			stats.put(StatEnum.POWER, 120);
			stats.put(StatEnum.HEALTH, 110);
			stats.put(StatEnum.ACCURACY, 90);
			stats.put(StatEnum.AGILITY, 100);
			stats.put(StatEnum.KNOWLEDGE, 100);
			stats.put(StatEnum.WILL, 90);
			stats.put(StatEnum.MAXHP, 12345);
			stats.put(StatEnum.MAXMP, 6789);
			stats.put(StatEnum.MAXDP, 4000);
			stats.put(StatEnum.FLY_TIME, 70);
			stats.put(StatEnum.PHYSICAL_ATTACK, 555);
			stats.put(StatEnum.MAGICAL_ATTACK, 444);
			stats.put(StatEnum.PHYSICAL_DEFENSE, 1500);
			stats.put(StatEnum.MAGICAL_DEFEND, 1200);
			stats.put(StatEnum.MAGICAL_RESIST, 300);
			stats.put(StatEnum.EVASION, 250);
			stats.put(StatEnum.PARRY, 260);
			stats.put(StatEnum.BLOCK, 270);
			stats.put(StatEnum.PHYSICAL_CRITICAL, 200);
			stats.put(StatEnum.PHYSICAL_ACCURACY, 1800);
			stats.put(StatEnum.MAGICAL_ACCURACY, 1700);
			stats.put(StatEnum.MAGICAL_CRITICAL, 150);
			stats.put(StatEnum.CONCENTRATION, 30);
			stats.put(StatEnum.BOOST_MAGICAL_SKILL, 40);
			stats.put(StatEnum.MAGIC_SKILL_BOOST_RESIST, 25);
			stats.put(StatEnum.HEAL_BOOST, 15);
			stats.put(StatEnum.PHYSICAL_CRITICAL_RESIST, 35);
			stats.put(StatEnum.MAGICAL_CRITICAL_RESIST, 45);
			stats.put(StatEnum.WATER_RESISTANCE, 10);
			stats.put(StatEnum.WIND_RESISTANCE, 11);
			stats.put(StatEnum.EARTH_RESISTANCE, 12);
			stats.put(StatEnum.FIRE_RESISTANCE, 13);
			stats.put(StatEnum.ELEMENTAL_RESISTANCE_LIGHT, 14);
			stats.put(StatEnum.ELEMENTAL_RESISTANCE_DARK, 15);
			stats.put(StatEnum.PHYSICAL_CRITICAL_DAMAGE_REDUCE, 5);
			stats.put(StatEnum.MAGICAL_CRITICAL_DAMAGE_REDUCE, 6);
			PlayerSpec spec = new PlayerSpec(200002, (byte) 25, Race.ASMODIANS, PlayerClass.GLADIATOR, stats);
			// level 25 start = exp[24]; pin exp above it so expShown > 0.
			spec.exp = EXP_TABLE[24] + 123456L;
			spec.expRecoverable = 1000L;
			spec.dp = 2500;
			spec.reposeCurrent = 700L;
			spec.reposeMax = 999L;
			spec.salvationPoint = 12000L; // -> getCurrentSalvationPercent = min(30, 12000/1000=12) = 12
			spec.currentHp = 9000;
			spec.currentMp = 4000;
			spec.currentFp = 50;
			cases.add(buildCase("midLevelVaried", spec));
		}

		// Case 3: high-level, salvation cap + larger exp.
		{
			TreeMap<StatEnum, Integer> stats = new TreeMap<>();
			stats.put(StatEnum.MAXHP, 30000);
			stats.put(StatEnum.MAXMP, 15000);
			stats.put(StatEnum.MAXDP, 4000);
			stats.put(StatEnum.FLY_TIME, 90);
			stats.put(StatEnum.POWER, 300);
			stats.put(StatEnum.PHYSICAL_ATTACK, 1234);
			PlayerSpec spec = new PlayerSpec(300003, (byte) 50, Race.ELYOS, PlayerClass.SORCERER, stats);
			spec.exp = EXP_TABLE[49] + 9_000_000_000L;
			spec.expRecoverable = 50000L;
			spec.dp = 4000;
			spec.reposeCurrent = 100000L;
			spec.reposeMax = 200000L;
			spec.salvationPoint = 999999L; // -> capped at 30
			spec.currentHp = 30000;
			spec.currentMp = 15000;
			spec.currentFp = 90;
			cases.add(buildCase("highLevelSalvationCap", spec));
		}

		writeFixture(outDir.resolve("SM_STATS_INFO.json"), "SM_STATS_INFO", cases);
	}

	// ---- integration seam ----

	private void installIntegrationSeam() {
		CustomConfig.BASE_FLYTIME = 60; // pin to @Property default (matches the C# initialiser)

		// Minimal AbsoluteStatsData so the faithful Player ctor's AbsoluteStatOwner(getTemplate(0)) is benign.
		if (DataManager.ABSOLUTE_STATS_DATA == null)
			DataManager.ABSOLUTE_STATS_DATA = new AbsoluteStatsData();

		// Identical minimal experience table on both sides.
		setExperienceTable(EXP_TABLE);

		installGameTimeStub();
	}

	/** Build a PlayerExperienceTable whose private long[] experience == the given fixture, install into DataManager. */
	private static void setExperienceTable(long[] table) {
		try {
			PlayerExperienceTable pxt = new PlayerExperienceTable();
			Field f = PlayerExperienceTable.class.getDeclaredField("experience");
			f.setAccessible(true);
			f.set(pxt, table.clone());
			DataManager.PLAYER_EXPERIENCE_TABLE = pxt; // public static field
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Failed to install PLAYER_EXPERIENCE_TABLE fixture", e);
		}
	}

	/**
	 * Inject a stub DataSource into DatabaseFactory so GameTimeService's lazy singleton resolves a fixed time (0) with
	 * no real DB. The stub's getConnection() throws SQLException, which ServerVariablesDAO.load() catches -> returns
	 * null -> GameTime(null) -> getTime() == 0. Java-test-only; never touches src DB code.
	 */
	private void installGameTimeStub() {
		try {
			Class<?> dbFactory = Class.forName("com.aionemu.commons.database.DatabaseFactory");
			Field dataSourceField = dbFactory.getDeclaredField("dataSource");
			dataSourceField.setAccessible(true);
			if (dataSourceField.get(null) == null) {
				Object stub = Proxy.newProxyInstance(
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
			throw new RuntimeException("Failed to install GameTime DB stub", e);
		}
	}

	// ---- case build ----

	private static Case buildCase(String name, PlayerSpec spec) {
		HarnessPlayer p = new HarnessPlayer(spec);
		return new Case(name, spec.toJson(), capture(new SM_STATS_INFO(p)));
	}

	/** Capture the payload bytes a packet's writeImpl produces (no opcode, no crypt). */
	private static String capture(AionServerPacket packet) {
		try {
			ByteBuffer buffer = ByteBuffer.allocate(8192).order(ByteOrder.LITTLE_ENDIAN);
			packet.setBuf(buffer);
			Method writeImpl = AionServerPacket.class.getDeclaredMethod("writeImpl", AionConnection.class);
			writeImpl.setAccessible(true);
			writeImpl.invoke(packet, (AionConnection) null);
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
		final PlayerClass playerClass;
		final TreeMap<StatEnum, Integer> statMap;
		long exp;
		long expRecoverable;
		int dp;
		long reposeCurrent;
		long reposeMax;
		long salvationPoint;
		int currentHp;
		int currentMp;
		int currentFp;

		PlayerSpec(int objectId, byte level, Race race, PlayerClass playerClass, TreeMap<StatEnum, Integer> statMap) {
			this.objectId = objectId;
			this.level = level;
			this.race = race;
			this.playerClass = playerClass;
			this.statMap = statMap;
		}

		String toJson() {
			Map<String, Object> m = new LinkedHashMap<>();
			StringBuilder sb = new StringBuilder();
			sb.append("{");
			sb.append("\"objectId\":").append(objectId);
			sb.append(",\"level\":").append(level);
			sb.append(",\"race\":\"").append(race.name()).append("\"");
			sb.append(",\"playerClass\":\"").append(playerClass.name()).append("\"");
			sb.append(",\"exp\":").append(exp);
			sb.append(",\"expRecoverable\":").append(expRecoverable);
			sb.append(",\"dp\":").append(dp);
			sb.append(",\"reposeCurrent\":").append(reposeCurrent);
			sb.append(",\"reposeMax\":").append(reposeMax);
			sb.append(",\"salvationPoint\":").append(salvationPoint);
			sb.append(",\"currentHp\":").append(currentHp);
			sb.append(",\"currentMp\":").append(currentMp);
			sb.append(",\"currentFp\":").append(currentFp);
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

	/**
	 * Faithful Player base ctor runs; the stat/life containers + common-data fields are pinned to the spec so
	 * SM_STATS_INFO serializes deterministically with no world/knownlist/DB.
	 */
	static final class HarnessPlayer extends Player {
		// Primitive/enum fields read by getLevel()/getRace(): the faithful base ctor calls getLevel() (via
		// PlayerGameStats.updateStatsTemplate) BEFORE our post-super assignments run, so these must be plain fields
		// with safe defaults (byte 0 / null) during super — they are overwritten to the spec values immediately after.
		private byte level;
		private Race race;

		HarnessPlayer(PlayerSpec spec) {
			super(minimalAccountData(spec), new Account(1));
			this.level = spec.level;
			this.race = spec.race;
			setGameStats(new HarnessStats(this, spec.statMap));
			HarnessLifeStats ls = new HarnessLifeStats(this);
			ls.pin(spec.currentHp, spec.currentMp, spec.currentFp);
			setLifeStats(ls);
			this.moveController = new PlayerMoveController(this); // movementMask defaults to 0
			pinCommonData(getCommonData(), spec);
		}

		@Override
		public byte getLevel() { return level; }

		@Override
		public Race getRace() { return race; }

		private static PlayerAccountData minimalAccountData(PlayerSpec spec) {
			PlayerCommonData common = new PlayerCommonData(spec.objectId);
			common.setPlayerClass(spec.playerClass);
			return new PlayerAccountData(common, new PlayerAppearance());
		}
	}

	/** Pin exp/level/dp/repose/salvation raw fields so the table-driven getters read fixed values. */
	private static void pinCommonData(PlayerCommonData pcd, PlayerSpec spec) {
		setLongField(pcd, "exp", spec.exp);
		setLongField(pcd, "expRecoverable", spec.expRecoverable);
		setIntField(pcd, "dp", spec.dp);
		setLongField(pcd, "reposeCurrent", spec.reposeCurrent);
		setLongField(pcd, "reposeMax", spec.reposeMax);
		setLongField(pcd, "salvationPoint", spec.salvationPoint);
		setIntField(pcd, "level", spec.level);
	}

	private static void setLongField(Object target, String name, long value) {
		try {
			Field f = PlayerCommonData.class.getDeclaredField(name);
			f.setAccessible(true);
			f.setLong(target, value);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Failed to pin " + name, e);
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

	// Same seam as the formula/creature harness: resolve a fixed value per StatEnum (else the passed base).
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

	// Fixed currentHp/currentMp/currentFp.
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
