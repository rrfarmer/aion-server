package com.aionemu.gameserver.utils.stats;

import java.io.IOException;
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

import com.aionemu.gameserver.configs.main.FallDamageConfig;
import com.aionemu.gameserver.dataholders.AbsoluteStatsData;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.controllers.movement.PlayableMoveController.MovementModifierDirection;
import com.aionemu.gameserver.controllers.movement.PlayerMoveController;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.SkillElement;
import com.aionemu.gameserver.model.account.Account;
import com.aionemu.gameserver.model.account.PlayerAccountData;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerAppearance;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.stats.calc.AdditionStat;
import com.aionemu.gameserver.model.stats.calc.Stat2;
import com.aionemu.gameserver.model.stats.container.CreatureGameStats;
import com.aionemu.gameserver.model.stats.container.PlayerLifeStats;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.model.templates.stats.StatsTemplate;

/**
 * Phase A2 / §5-A2 of the Port Fidelity & Remediation Plan: the PLAYER-FORMULA golden harness.
 *
 * <p>Companion to {@link GoldenCombatFormulaFixtureGeneratorTest} (the Creature harness). This generator exercises
 * {@link StatFunctions} methods that take a live {@link Player} and read player-only sub-state
 * ({@code getLifeStats()}, {@code getMoveController().getMovementDirection()}, {@code instanceof Player}).
 * To make this DETERMINISTIC and bilaterally reproducible we build a minimal {@link HarnessPlayer}: it extends the
 * faithful {@link Player}, supplies a no-op {@link PlayerCommonData}/{@link Account} so the base constructor runs, and
 * overrides every getter the target formulas touch — {@code getLevel()/getRace()/getGameStats()/getLifeStats()/
 * getMoveController()} — to return fixed harness stubs (the same {@link HarnessStats} seam as the Creature harness,
 * plus a {@link HarnessLifeStats} for fixed currentHp/maxHp and a {@link HarnessMoveController} for a fixed movement
 * direction). The C# asserter (GoldenPlayerFormulaFixtureTests) rebuilds the structurally identical player from the
 * SAME fixture and asserts the formula returns the identical value. Java is the single source of truth.</p>
 *
 * <p>SCOPE: only deterministic-value formulas (no {@code Rnd}). Covered here:
 * {@code calculateFallDamage} (maxHp + currentHp + distance, full branch table),
 * {@code calculateMagicalResistRate} Player-vs-Player {@code min(500, ...)} branch,
 * {@code adjustStatByMovementModifier} Player movement-direction branches (FORWARD/SIDEWAYS/BACKWARD/NONE).
 * Fall-damage config statics are pinned to their {@code @Property} default values on both sides so the test is
 * independent of any loaded properties file.</p>
 *
 * Regenerate with:
 *   mvn -pl game-server -am test -Dtest=GoldenPlayerFormulaFixtureGeneratorTest -Dmaven.test.skip=false -Dsurefire.failIfNoSpecifiedTests=false
 */
public class GoldenPlayerFormulaFixtureGeneratorTest {

	// Pin the fall-damage config statics to their @Property defaults so the harness is independent of any loaded
	// properties file. The C# asserter pins the identical values.
	static final float FALL_DAMAGE_PERCENTAGE = 1.0f;
	static final int MINIMUM_DISTANCE_DAMAGE = 10;
	static final int MAXIMUM_DISTANCE_DAMAGE = 50;

	@Test
	public void generateGoldenPlayerFormulaFixtures() throws IOException {
		Path outDir = repoRoot().resolve("parity-artifacts/golden/formulas");
		Files.createDirectories(outDir);

		FallDamageConfig.FALL_DAMAGE_PERCENTAGE = FALL_DAMAGE_PERCENTAGE;
		FallDamageConfig.MINIMUM_DISTANCE_DAMAGE = MINIMUM_DISTANCE_DAMAGE;
		FallDamageConfig.MAXIMUM_DISTANCE_DAMAGE = MAXIMUM_DISTANCE_DAMAGE;

		// The base Player ctor builds an AbsoluteStatOwner that reads DataManager.ABSOLUTE_STATS_DATA. Install an
		// empty holder so getTemplate(0) returns null (handled benignly); no game data is loaded in this unit test.
		if (DataManager.ABSOLUTE_STATS_DATA == null)
			DataManager.ABSOLUTE_STATS_DATA = new AbsoluteStatsData();

		generateCalculateFallDamage(outDir);
		generateCalculateMagicalResistRatePvP(outDir);
		generateAdjustStatByMovementModifier(outDir);
	}

	// StatFunctions.calculateFallDamage(Player, float): maxHp/currentHp + distance branch table.
	private void generateCalculateFallDamage(Path outDir) throws IOException {
		List<Case> cases = new ArrayList<>();
		// { maxHp, currentHp, distance }
		Object[][] inputs = {
			{ 10000, 7500, 5f },     // < MINIMUM (10) -> 0
			{ 10000, 7500, 10f },    // == MINIMUM -> (int)(10 * 10000*1.0/100) = 1000
			{ 10000, 7500, 25.5f },  // mid: (int)(25.5 * 100.0f) = 2550
			{ 12345, 6000, 30f },    // (int)(30 * 12345*1.0/100) = (int)(30*123.45) = 3703
			{ 10000, 7500, 50f },    // == MAXIMUM -> currentHp = 7500
			{ 10000, 3000, 80f },    // > MAXIMUM -> currentHp = 3000
			{ 9999, 4321, 33.3f },   // truncation: dmgPerMeter = 9999*1f/100 = 99.99; 33.3*99.99 -> (int)3329.667
			{ 10000, 7500, 9.9999f },// just below MINIMUM (10) -> 0
		};
		for (Object[] in : inputs) {
			int maxHp = (Integer) in[0];
			int currentHp = (Integer) in[1];
			float distance = (Float) in[2];
			HarnessPlayer p = player(50, Race.ELYOS, MovementModifierDirection.NONE, statMaxHp(maxHp), currentHp);
			Map<String, Object> args = new LinkedHashMap<>();
			args.put("player", playerJson(p));
			args.put("distance", floatRepr(distance));
			cases.add(Case.ofLong(args, StatFunctions.calculateFallDamage(p, distance)));
		}
		writeFixture(outDir.resolve("StatFunctions.calculateFallDamage.json"),
			"StatFunctions.calculateFallDamage",
			"int calculateFallDamage(Player player, float distance)",
			cases);
	}

	// StatFunctions.calculateMagicalResistRate Player-vs-Player branch: returns Math.min(500, resistRate).
	private void generateCalculateMagicalResistRatePvP(Path outDir) throws IOException {
		List<Case> cases = new ArrayList<>();
		// { attackerLvl, attackedLvl, mResist(attacked), mAccuracy(attacker), accMod }
		int[][] inputs = {
			{ 50, 50, 0, 0, 0 },        // 0 -> min(500,0) = 0
			{ 50, 50, 300, 100, 0 },    // 300-100 = 200 -> 200
			{ 50, 50, 1000, 100, 0 },   // 900 -> min(500,900) = 500 (cap)
			{ 50, 56, 200, 50, 0 },     // levelDiff 6 >4, mResi>0 -> 150 + 200 = 350
			{ 50, 60, 500, 0, 0 },      // levelDiff 10 -> 500 + 600 = 1100 -> min(500,1100) = 500
			{ 50, 50, 100, 300, 0 },    // negative -> -200 (min keeps -200; no floor in PvP branch)
			{ 50, 56, 0, 50, 0 },       // mResi 0 -> no level bonus -> -50
		};
		for (int[] in : inputs) {
			HarnessPlayer attacker = player(in[0], Race.ELYOS, MovementModifierDirection.NONE, stat(StatEnum.MAGICAL_ACCURACY, in[3]), 1000);
			HarnessPlayer attacked = player(in[1], Race.ASMODIANS, MovementModifierDirection.NONE, stat(StatEnum.MAGICAL_RESIST, in[2]), 1000);
			Map<String, Object> args = new LinkedHashMap<>();
			args.put("attacker", playerJson(attacker));
			args.put("attacked", playerJson(attacked));
			args.put("accMod", in[4]);
			args.put("element", quote(SkillElement.NONE.name()));
			cases.add(Case.ofLong(args, StatFunctions.calculateMagicalResistRate(attacker, attacked, in[4], SkillElement.NONE)));
		}
		writeFixture(outDir.resolve("StatFunctions.calculateMagicalResistRate.pvp.json"),
			"StatFunctions.calculateMagicalResistRate",
			"int calculateMagicalResistRate(Creature attacker, Creature attacked, int accMod, SkillElement element) [Player-vs-Player min(500,..) branch]",
			cases);
	}

	// StatFunctions.adjustStatByMovementModifier(Creature, StatEnum, float): Player movement-direction branches.
	private void generateAdjustStatByMovementModifier(Path outDir) throws IOException {
		List<Case> cases = new ArrayList<>();
		// { direction, stat, value }
		Object[][] inputs = {
			// FORWARD
			{ MovementModifierDirection.FORWARD, "PHYSICAL_ATTACK", 1000f },  // *1.1 = 1100
			{ MovementModifierDirection.FORWARD, "MAGICAL_ATTACK", 999f },    // *1.1 = 1098.9
			{ MovementModifierDirection.FORWARD, "FIRE_RESISTANCE", 200f },   // -50 = 150
			{ MovementModifierDirection.FORWARD, "DARK_RESISTANCE", 30f },    // -50 = -20
			{ MovementModifierDirection.FORWARD, "MAGICAL_DEFEND", 500f },    // *0.8 = 400
			{ MovementModifierDirection.FORWARD, "PHYSICAL_DEFENSE", 1234f }, // *0.8 = 987.2
			{ MovementModifierDirection.FORWARD, "EVASION", 777f },           // no FORWARD case -> 777
			// SIDEWAYS
			{ MovementModifierDirection.SIDEWAYS, "PHYSICAL_ATTACK", 1000f }, // *0.8 = 800
			{ MovementModifierDirection.SIDEWAYS, "SPEED", 6000f },           // *0.8 = 4800
			{ MovementModifierDirection.SIDEWAYS, "EVASION", 100f },          // +300 = 400
			{ MovementModifierDirection.SIDEWAYS, "PARRY", 100f },            // no SIDEWAYS case -> 100
			// BACKWARD
			{ MovementModifierDirection.BACKWARD, "MAGICAL_ATTACK", 1000f },  // *0.8 = 800
			{ MovementModifierDirection.BACKWARD, "SPEED", 6000f },           // *0.6 = 3600
			{ MovementModifierDirection.BACKWARD, "PARRY", 250f },            // +500 = 750
			{ MovementModifierDirection.BACKWARD, "BLOCK", 0f },              // +500 = 500
			{ MovementModifierDirection.BACKWARD, "EVASION", 333f },          // no BACKWARD case -> 333
			// NONE -> pass-through
			{ MovementModifierDirection.NONE, "PHYSICAL_ATTACK", 1000f },     // NONE -> 1000
		};
		for (Object[] in : inputs) {
			MovementModifierDirection dir = (MovementModifierDirection) in[0];
			StatEnum stat = StatEnum.valueOf((String) in[1]);
			float value = (Float) in[2];
			HarnessPlayer p = player(50, Race.ELYOS, dir, noStats(), 1000);
			Map<String, Object> args = new LinkedHashMap<>();
			args.put("player", playerJson(p));
			args.put("direction", quote(dir.name()));
			args.put("stat", quote(stat.name()));
			args.put("value", floatRepr(value));
			cases.add(Case.ofFloat(args, StatFunctions.adjustStatByMovementModifier(p, stat, value)));
		}
		writeFixture(outDir.resolve("StatFunctions.adjustStatByMovementModifier.player.json"),
			"StatFunctions.adjustStatByMovementModifier",
			"float adjustStatByMovementModifier(Creature creature, StatEnum stat, float value) [Player movement-direction branches]",
			cases);
	}

	// ---- harness construction ----

	private static TreeMap<StatEnum, Integer> noStats() {
		return new TreeMap<>();
	}

	private static TreeMap<StatEnum, Integer> stat(StatEnum s, int v) {
		TreeMap<StatEnum, Integer> m = new TreeMap<>();
		m.put(s, v);
		return m;
	}

	private static TreeMap<StatEnum, Integer> statMaxHp(int maxHp) {
		return stat(StatEnum.MAXHP, maxHp);
	}

	private static HarnessPlayer player(int level, Race race, MovementModifierDirection dir,
		TreeMap<StatEnum, Integer> statMap, int currentHp) {
		return new HarnessPlayer((byte) level, race, dir, statMap, currentHp);
	}

	// Serializes a player into the fixture so the C# side rebuilds the identical player.
	private static String playerJson(HarnessPlayer p) {
		StringBuilder sb = new StringBuilder();
		sb.append("{ \"level\": ").append(p.getLevel());
		sb.append(", \"race\": \"").append(p.getRace().name()).append("\"");
		sb.append(", \"direction\": \"").append(p.direction.name()).append("\"");
		sb.append(", \"currentHp\": ").append(p.currentHp);
		sb.append(", \"stats\": {");
		int j = 0;
		for (Map.Entry<StatEnum, Integer> e : p.statMap.entrySet()) {
			if (j++ > 0)
				sb.append(", ");
			sb.append("\"").append(e.getKey().name()).append("\": ").append(e.getValue());
		}
		sb.append("} }");
		return sb.toString();
	}

	private static PlayerAccountData minimalAccountData() {
		PlayerCommonData common = new PlayerCommonData(1);
		// A non-null PlayerClass is required: PlayerGameStats(owner) runs updateStatsTemplate() during the base
		// Player ctor -> owner.getPlayerClass().createStatsTemplate(level) (pure math, no DataManager).
		common.setPlayerClass(PlayerClass.WARRIOR);
		return new PlayerAccountData(common, new PlayerAppearance());
	}

	/**
	 * Minimal deterministic Player: the faithful base ctor runs (so {@code instanceof Player} and all the
	 * player plumbing exist), but every getter the target formulas touch is overridden to return fixed harness stubs.
	 */
	static final class HarnessPlayer extends Player {
		final byte level;
		final Race race;
		final MovementModifierDirection direction;
		final TreeMap<StatEnum, Integer> statMap;
		final int currentHp;

		HarnessPlayer(byte level, Race race, MovementModifierDirection direction,
			TreeMap<StatEnum, Integer> statMap, int currentHp) {
			super(minimalAccountData(), new Account(1));
			this.level = level;
			this.race = race;
			this.direction = direction;
			this.statMap = statMap;
			this.currentHp = currentHp;
			// Replace the base ctor's real stat containers / move controller with deterministic harness ones.
			// (We cannot override the getters with backing fields: those getters are called from the base Player
			// ctor before our fields are assigned, so we install the stubs here instead, post-super.)
			setGameStats(new HarnessStats(this, statMap));
			setLifeStats(new HarnessLifeStats(this, currentHp));
			this.moveController = new HarnessMoveController(this, direction);
		}

		@Override
		public byte getLevel() { return level; }

		@Override
		public Race getRace() { return race; }
	}

	// Same seam as the Creature harness: resolve a fixed value per StatEnum (else use the passed base).
	// Typed as PlayerGameStats so HarnessPlayer.getGameStats() can return it (Player declares PlayerGameStats).
	static final class HarnessStats extends com.aionemu.gameserver.model.stats.container.PlayerGameStats {
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

	// Fixed currentHp; maxHp resolves through HarnessStats (StatEnum.MAXHP) via the base getMaxHp() delegation.
	static final class HarnessLifeStats extends PlayerLifeStats {
		private final int fixedCurrentHp;

		HarnessLifeStats(Player owner, int currentHp) {
			super(owner);
			this.fixedCurrentHp = currentHp;
		}

		@Override
		public int getCurrentHp() { return fixedCurrentHp; }
	}

	// Fixed movement direction, no world/timing.
	static final class HarnessMoveController extends PlayerMoveController {
		private final MovementModifierDirection dir;

		HarnessMoveController(Player owner, MovementModifierDirection dir) {
			super(owner);
			this.dir = dir;
		}

		@Override
		public MovementModifierDirection getMovementDirection() { return dir; }
	}

	// ---- fixture writing (mirrors GoldenCombatFormulaFixtureGeneratorTest) ----

	private static String quote(String s) {
		return "\"" + s + "\"";
	}

	private static String floatRepr(float f) {
		return Float.toString(f);
	}

	private static void writeFixture(Path file, String formula, String signature, List<Case> cases) throws IOException {
		StringBuilder sb = new StringBuilder();
		sb.append("{\n");
		sb.append("  \"schemaVersion\": 1,\n");
		sb.append("  \"formula\": \"").append(formula).append("\",\n");
		sb.append("  \"signature\": \"").append(signature).append("\",\n");
		sb.append("  \"source\": \"Java\",\n");
		sb.append("  \"cases\": [\n");
		for (int i = 0; i < cases.size(); i++) {
			Case c = cases.get(i);
			sb.append("    { \"inputs\": {");
			int j = 0;
			for (Map.Entry<String, Object> e : c.inputs.entrySet()) {
				if (j++ > 0)
					sb.append(", ");
				sb.append("\"").append(e.getKey()).append("\": ").append(e.getValue());
			}
			sb.append("}, ").append(c.resultJson()).append(" }");
			sb.append(i + 1 < cases.size() ? "," : "").append("\n");
		}
		sb.append("  ]\n");
		sb.append("}\n");
		Files.write(file, sb.toString().getBytes(StandardCharsets.UTF_8));
	}

	private static Path repoRoot() {
		Path dir = Paths.get("").toAbsolutePath();
		while (dir != null && !Files.isDirectory(dir.resolve("parity-artifacts"))) {
			dir = dir.getParent();
		}
		return dir != null ? dir : Paths.get("").toAbsolutePath();
	}

	private static final class Case {
		final Map<String, Object> inputs;
		final Long numericResult;
		final Float floatResult;

		private Case(Map<String, Object> inputs, Long numericResult, Float floatResult) {
			this.inputs = inputs;
			this.numericResult = numericResult;
			this.floatResult = floatResult;
		}

		static Case ofLong(Map<String, Object> inputs, long result) {
			return new Case(inputs, result, null);
		}

		static Case ofFloat(Map<String, Object> inputs, float result) {
			return new Case(inputs, null, result);
		}

		String resultJson() {
			if (floatResult != null)
				return "\"resultFloat\": \"" + Float.toString(floatResult) + "\"";
			return "\"result\": " + numericResult;
		}
	}
}
