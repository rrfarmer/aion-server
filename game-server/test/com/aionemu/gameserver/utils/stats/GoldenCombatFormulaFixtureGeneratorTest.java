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

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.SkillElement;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.stats.calc.AdditionStat;
import com.aionemu.gameserver.model.stats.calc.Stat2;
import com.aionemu.gameserver.model.stats.container.CreatureGameStats;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.model.templates.stats.StatsTemplate;

/**
 * Phase A2 / §5-A2 of the Port Fidelity & Remediation Plan: the COMBAT-FORMULA golden harness.
 *
 * <p>Unlike {@link GoldenFormulaFixtureGeneratorTest} (pure primitive/enum formulas), this generator
 * exercises {@link StatFunctions} methods that take live {@link Creature}s and read
 * {@code creature.getGameStats().getStat(...)}. To make this DETERMINISTIC and bilaterally reproducible
 * we build a minimal {@link HarnessCreature} whose {@link HarnessStats game-stats container} resolves
 * every {@link StatEnum} to a fixed value (overriding {@code getStat(StatEnum, base)} to consult an
 * explicit map, defaulting to the passed base). The C# asserter
 * (GoldenCombatFormulaFixtureTests) builds the structurally identical creature from the SAME fixture and
 * asserts the formula returns the identical value. Java is the single source of truth.</p>
 *
 * <p>SCOPE: only deterministic-value formulas (no {@code Rnd}). The crit/dodge/parry/block roll methods are
 * NON-deterministic and intentionally excluded. Covered here:
 * {@code calculateHate}, {@code calculateMagicalResistRate} (NPC + level-diff + cap branches),
 * {@code adjustDamageByPvpOrPveModifiers} (PvE + PvP branches), {@code adjustStatByMovementModifier}
 * (non-Player pass-through).</p>
 *
 * Regenerate with:
 *   mvn -pl game-server -am test -Dtest=GoldenCombatFormulaFixtureGeneratorTest -Dmaven.test.skip=false -Dsurefire.failIfNoSpecifiedTests=false
 */
public class GoldenCombatFormulaFixtureGeneratorTest {

	@Test
	public void generateGoldenCombatFormulaFixtures() throws IOException {
		Path outDir = repoRoot().resolve("parity-artifacts/golden/formulas");
		Files.createDirectories(outDir);

		generateCalculateHate(outDir);
		generateCalculateMagicalResistRate(outDir);
		generateAdjustDamageByPvpOrPveModifiers(outDir);
		generateAdjustStatByMovementModifier(outDir);
	}

	// StatFunctions.calculateHate(Creature, int): (int)((long)value * (1000 + BOOST_HATE) / 1000)
	private void generateCalculateHate(Path outDir) throws IOException {
		List<Case> cases = new ArrayList<>();
		int[][] inputs = {
			// { boostHate, value }
			{ 0, 1000 },      // default-ish: no boost -> value
			{ 100, 1000 },    // +10%
			{ 250, 12345 },   // truncation
			{ -500, 1000 },   // negative boost halves
			{ 1000, 7 },      // small value, doubles
			{ 333, 999999 },  // large value truncation
		};
		for (int[] in : inputs) {
			TreeMap<StatEnum, Integer> sm = stat(StatEnum.BOOST_HATE, in[0]);
			HarnessCreature c = creature(50, Race.NPC, sm);
			Map<String, Object> args = new LinkedHashMap<>();
			args.put("creature", creatureJson(c, sm));
			args.put("value", in[1]);
			cases.add(Case.ofLong(args, StatFunctions.calculateHate(c, in[1])));
		}
		writeFixture(outDir.resolve("StatFunctions.calculateHate.json"),
			"StatFunctions.calculateHate",
			"int calculateHate(Creature creature, int value)",
			cases);
	}

	// StatFunctions.calculateMagicalResistRate(Creature attacker, Creature attacked, int accMod, SkillElement element)
	// NPC vs NPC path (no observe RESIST, not Summon, not Player-vs-Player). Exercises level-diff bonus + cap.
	private void generateCalculateMagicalResistRate(Path outDir) throws IOException {
		List<Case> cases = new ArrayList<>();
		// { attackerLvl, attackedLvl, mResist(attacked), mAccuracy(attacker), accMod }
		int[][] inputs = {
			{ 50, 50, 0, 0, 0 },        // all zero -> 0
			{ 50, 50, 300, 100, 0 },    // 300-100 = 200, levelDiff 0 -> 200
			{ 50, 50, 300, 100, 50 },   // -accMod -> 150
			{ 50, 56, 200, 50, 0 },     // levelDiff 6 >4, mResi>0 -> (200-50) + (6-4)*100 = 350
			{ 50, 56, 0, 50, 0 },       // mResi 0 -> no level bonus -> 0-50 = -50
			{ 50, 60, 500, 0, 0 },      // levelDiff 10 -> 500 + 6*100 = 1100 -> capped at 900
			{ 60, 50, 200, 100, 0 },    // negative levelDiff -> no bonus -> 100
		};
		for (int[] in : inputs) {
			HarnessCreature attacker = creature(in[0], Race.NPC, stat(StatEnum.MAGICAL_ACCURACY, in[3]));
			HarnessCreature attacked = creature(in[1], Race.NPC, stat(StatEnum.MAGICAL_RESIST, in[2]));
			Map<String, Object> args = new LinkedHashMap<>();
			args.put("attacker", creatureJson(attacker, stat(StatEnum.MAGICAL_ACCURACY, in[3])));
			args.put("attacked", creatureJson(attacked, stat(StatEnum.MAGICAL_RESIST, in[2])));
			args.put("accMod", in[4]);
			args.put("element", quote(SkillElement.NONE.name()));
			cases.add(Case.ofLong(args, StatFunctions.calculateMagicalResistRate(attacker, attacked, in[4], SkillElement.NONE)));
		}
		writeFixture(outDir.resolve("StatFunctions.calculateMagicalResistRate.json"),
			"StatFunctions.calculateMagicalResistRate",
			"int calculateMagicalResistRate(Creature attacker, Creature attacked, int accMod, SkillElement element)",
			cases);
	}

	// StatFunctions.adjustDamageByPvpOrPveModifiers(Creature attacker, Creature target, float baseDamage, int pvpDamage, boolean useTemplateDmg, SkillElement element)
	// useTemplateDmg=true makes PvP/PvE bonus aggregation be skipped (only the *0.42 PvP modifier / level-diff applies),
	// which keeps the test independent of the Influence singleton / Player level-diff. We also keep races equal in PvP.
	private void generateAdjustDamageByPvpOrPveModifiers(Path outDir) throws IOException {
		List<Case> cases = new ArrayList<>();
		// { pvpTarget(0/1), baseDamage, pvpDamage, useTemplateDmg(0/1),
		//   atkPvpAtkRatio, defPvpDefRatio, atkPvpAtkPhys, defPvpDefPhys,
		//   atkPveAtkRatio, defPveDefRatio, atkPveAtkPhys, defPveDefPhys }
		int[][] inputs = {
			// PvP, useTemplateDmg=true: damage * pvpDamage*0.01 (if>0) * 0.42, multiplier=max(1,0.1)=1
			{ 1, 1000, 0, 1, 0,0,0,0, 0,0,0,0 },     // 1000 * 0.42 = 420
			{ 1, 1000, 50, 1, 0,0,0,0, 0,0,0,0 },    // 1000*0.5*0.42 = 210
			// PvP, useTemplateDmg=false: bonus aggregation (same race so no Influence; NPC isInInstance avoided since race equal)
			{ 1, 1000, 0, 0, 200,0,100,0, 0,0,0,0 }, // attackBonus=200+100=300, def=0 -> mult 1.3 -> 1000*0.42*1.3 = 546
			{ 1, 1000, 0, 0, 0,200,0,100, 0,0,0,0 }, // defenseBonus=300 -> mult 0.7 -> 1000*0.42*0.7 = 294
			{ 1, 1000, 0, 0, 0,2000,0,0, 0,0,0,0 },  // def 2000 capped 900 -> mult 1-0.9=0.1 -> 1000*0.42*0.1 = 42
			// PvE, useTemplateDmg=false, attacker is NOT player (no level-diff reduction)
			{ 0, 1000, 0, 0, 0,0,0,0, 300,0,100,0 }, // atk=400 -> mult 1.4 -> 1400
			{ 0, 1000, 0, 0, 0,0,0,0, 0,400,0,100 }, // def=500 -> mult 0.5 -> 500
			{ 0, 1000, 0, 1, 0,0,0,0, 0,0,0,0 },     // useTemplateDmg, PvE: no level diff (not player) -> 1000
		};
		for (int[] in : inputs) {
			boolean pvpTarget = in[0] == 1;
			boolean useTemplateDmg = in[3] == 1;
			TreeMap<StatEnum, Integer> aStats = stats(
				StatEnum.PVP_ATTACK_RATIO, in[4], StatEnum.PVP_ATTACK_RATIO_PHYSICAL, in[6],
				StatEnum.PVE_ATTACK_RATIO, in[8], StatEnum.PVE_ATTACK_RATIO_PHYSICAL, in[10]);
			TreeMap<StatEnum, Integer> tStats = stats(
				StatEnum.PVP_DEFEND_RATIO, in[5], StatEnum.PVP_DEFEND_RATIO_PHYSICAL, in[7],
				StatEnum.PVE_DEFEND_RATIO, in[9], StatEnum.PVE_DEFEND_RATIO_PHYSICAL, in[11]);
			HarnessCreature attacker = creature(50, Race.NPC, aStats, pvpTarget);
			HarnessCreature target = creature(50, Race.NPC, tStats);
			Map<String, Object> args = new LinkedHashMap<>();
			args.put("attacker", creatureJson(attacker, aStats));
			args.put("target", creatureJson(target, tStats));
			args.put("baseDamage", floatRepr((float) in[1]));
			args.put("pvpDamage", in[2]);
			args.put("useTemplateDmg", useTemplateDmg);
			args.put("element", quote(SkillElement.NONE.name()));
			args.put("attackerPvpTarget", pvpTarget);
			cases.add(Case.ofFloat(args,
				StatFunctions.adjustDamageByPvpOrPveModifiers(attacker, target, (float) in[1], in[2], useTemplateDmg, SkillElement.NONE)));
		}
		writeFixture(outDir.resolve("StatFunctions.adjustDamageByPvpOrPveModifiers.json"),
			"StatFunctions.adjustDamageByPvpOrPveModifiers",
			"float adjustDamageByPvpOrPveModifiers(Creature attacker, Creature target, float baseDamage, int pvpDamage, boolean useTemplateDmg, SkillElement element)",
			cases);
	}

	// StatFunctions.adjustStatByMovementModifier(Creature, StatEnum, float): non-Player creature -> pass-through (returns value).
	private void generateAdjustStatByMovementModifier(Path outDir) throws IOException {
		List<Case> cases = new ArrayList<>();
		Object[][] inputs = {
			{ "PHYSICAL_ATTACK", 1000f },
			{ "MAGICAL_DEFEND", 500f },
			{ "EVASION", 0f },
			{ "SPEED", 6000f },
		};
		HarnessCreature c = creature(50, Race.NPC, noStats());
		for (Object[] in : inputs) {
			StatEnum stat = StatEnum.valueOf((String) in[0]);
			float value = (Float) in[1];
			Map<String, Object> args = new LinkedHashMap<>();
			args.put("creature", creatureJson(c, noStats()));
			args.put("stat", quote(stat.name()));
			args.put("value", floatRepr(value));
			cases.add(Case.ofFloat(args, StatFunctions.adjustStatByMovementModifier(c, stat, value)));
		}
		writeFixture(outDir.resolve("StatFunctions.adjustStatByMovementModifier.json"),
			"StatFunctions.adjustStatByMovementModifier",
			"float adjustStatByMovementModifier(Creature creature, StatEnum stat, float value) [non-Player pass-through]",
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

	private static TreeMap<StatEnum, Integer> stats(Object... kv) {
		TreeMap<StatEnum, Integer> m = new TreeMap<>();
		for (int i = 0; i < kv.length; i += 2)
			m.put((StatEnum) kv[i], (Integer) kv[i + 1]);
		return m;
	}

	private static HarnessCreature creature(int level, Race race, TreeMap<StatEnum, Integer> statMap) {
		return creature(level, race, statMap, false);
	}

	private static HarnessCreature creature(int level, Race race, TreeMap<StatEnum, Integer> statMap, boolean pvpTarget) {
		return new HarnessCreature((byte) level, race, statMap, pvpTarget);
	}

	// Serializes a creature into the fixture so the C# side rebuilds the identical creature.
	private static String creatureJson(HarnessCreature c, TreeMap<StatEnum, Integer> statMap) {
		StringBuilder sb = new StringBuilder();
		sb.append("{ \"level\": ").append(c.getLevel());
		sb.append(", \"race\": \"").append(c.getRace().name()).append("\"");
		sb.append(", \"pvpTarget\": ").append(c.pvpTarget);
		sb.append(", \"stats\": {");
		int j = 0;
		for (Map.Entry<StatEnum, Integer> e : statMap.entrySet()) {
			if (j++ > 0)
				sb.append(", ");
			sb.append("\"").append(e.getKey().name()).append("\": ").append(e.getValue());
		}
		sb.append("} }");
		return sb.toString();
	}

	/**
	 * Minimal deterministic Creature: no spawn/world. getStat is fully overridden in {@link HarnessStats} to
	 * resolve from an explicit stat map (default = passed base), so all combat math is reproducible.
	 */
	static final class HarnessCreature extends Creature {
		private final byte level;
		private final Race race;
		final boolean pvpTarget;
		private final CreatureGameStats<?> gs;

		HarnessCreature(byte level, Race race, TreeMap<StatEnum, Integer> statMap, boolean pvpTarget) {
			super(0, null, null, new NpcTemplate(), null, false);
			this.level = level;
			this.race = race;
			this.pvpTarget = pvpTarget;
			this.gs = new HarnessStats(this, statMap);
		}

		@Override
		public byte getLevel() { return level; }

		@Override
		public Race getRace() { return race; }

		@Override
		public CreatureGameStats<? extends Creature> getGameStats() { return gs; }

		@Override
		public boolean isPvpTarget(Creature creature) { return pvpTarget; }

		@Override
		public boolean isInInstance() { return false; }

		@Override
		public com.aionemu.gameserver.model.gameobjects.player.Player getActingCreature() { return null; }
	}

	static final class HarnessStats extends CreatureGameStats<Creature> {
		private final Map<StatEnum, Integer> statMap;

		HarnessStats(Creature owner, Map<StatEnum, Integer> statMap) {
			super(owner);
			this.statMap = statMap;
		}

		// The single seam: resolve a fixed value per StatEnum (else use the base the formula passed in).
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

	// ---- fixture writing (mirrors GoldenFormulaFixtureGeneratorTest) ----

	private static String quote(String s) {
		return "\"" + s + "\"";
	}

	// Java float -> exact decimal text; C# parses with float.Parse and compares bit-exact.
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
