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

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.model.templates.npc.NpcRating;

/**
 * Phase A2 of the Port Fidelity & Remediation Plan, extended to pure formulas.
 *
 * Runs real Java calculation methods (e.g. {@link StatFunctions}) for chosen inputs and
 * writes the results to shared fixtures under {@code parity-artifacts/golden/formulas/}.
 * The C# side (GoldenFormulaFixtureTests) reads the SAME fixtures and asserts its formula
 * methods return identical values. The Java result is the single source of truth, so the
 * exact Java semantics (including int/double compound-assignment truncation) are pinned.
 *
 * Regenerate with:
 *   mvn -pl game-server -am test -Dtest=GoldenFormulaFixtureGeneratorTest -Dmaven.test.skip=false -Dsurefire.failIfNoSpecifiedTests=false
 *
 * Only pure, primitive/enum-driven methods belong here (no Player/Creature/world context):
 * the body must read ONLY its arguments (no statics/config/random). Enum args are serialized
 * by NAME; enum-typed results are serialized by NAME too (see {@code resultName}).
 */
public class GoldenFormulaFixtureGeneratorTest {

	@Test
	public void generateGoldenFormulaFixtures() throws IOException {
		Path outDir = repoRoot().resolve("parity-artifacts/golden/formulas");
		Files.createDirectories(outDir);

		generateAdjustPvpDpGained(outDir);
		generateCalculateRatingMultiplier(outDir);
		generateGetApNpcRating(outDir);
		generateXpRewardFrom(outDir);
		generateDropRewardFrom(outDir);
		generateGetExpLoss(outDir);
		generateAbyssGetRankForPoints(outDir);
		generateAbyssGetRankById(outDir);
		generateLimit(outDir);
		generateAbyssRankTableGetters(outDir);
	}

	// AbyssRankEnum per-rank table getters — each reads ONLY the enum constant's own immutable fields
	// (no Player/Race/config/random). Captures every rank for each getter so the entire abyss-rank
	// table is pinned bilaterally. The 'rank' input is the enum NAME; the C# side resolves it via
	// Enum.Parse<AbyssRankEnum> and calls the matching extension getter.
	// NOTE: getGpLossPerDay()/getQuota() are intentionally EXCLUDED — they read RankingConfig static
	// maps (not pure), which would make the fixture config-dependent.
	private static void generateAbyssRankTableGetters(Path outDir) throws IOException {
		String[][] getters = {
			{ "getId",          "AbyssRankEnum.getId",          "int getId()" },
			{ "getPointsLost",  "AbyssRankEnum.getPointsLost",  "int getPointsLost()" },
			{ "getPointsGained","AbyssRankEnum.getPointsGained","int getPointsGained()" },
			{ "getRequiredAP",  "AbyssRankEnum.getRequiredAP",  "int getRequiredAP()" },
			{ "getRequiredGP",  "AbyssRankEnum.getRequiredGP",  "int getRequiredGP()" },
		};
		for (String[] g : getters) {
			List<Case> cases = new ArrayList<>();
			for (AbyssRankEnum r : AbyssRankEnum.values()) {
				Map<String, Object> args = new LinkedHashMap<>();
				args.put("rank", quote(r.name()));
				long value;
				switch (g[0]) {
					case "getId": value = r.getId(); break;
					case "getPointsLost": value = r.getPointsLost(); break;
					case "getPointsGained": value = r.getPointsGained(); break;
					case "getRequiredAP": value = r.getRequiredAP(); break;
					case "getRequiredGP": value = r.getRequiredGP(); break;
					default: throw new IllegalStateException(g[0]);
				}
				cases.add(Case.ofLong(args, value));
			}
			writeFixture(outDir.resolve(g[1] + ".json"), g[1], g[2], cases);
		}
	}

	// StatFunctions.limit(StatEnum, float) — Math.min(StatCapUtil.getDifferenceLimit(stat), value).
	// Pure: reads only the arg + the static difference-limit table. Covers every distinct table
	// bucket (500/900/300/400/2900/Integer.MAX_VALUE) on both the capped and uncapped sides, plus
	// the int->float promotion edge (Integer.MAX_VALUE -> 2.14748365E9f loses precision identically
	// in Java and C#).
	private static void generateLimit(Path outDir) throws IOException {
		List<Case> cases = new ArrayList<>();
		Object[][] inputs = {
			// stat,                       value           -> exercises which side of Math.min
			{ StatEnum.BLOCK,              200f },         // table 500: value < limit -> value
			{ StatEnum.BLOCK,              900f },         // table 500: value > limit -> 500
			{ StatEnum.BLOCK,              500f },         // table 500: equal -> 500
			{ StatEnum.PHYSICAL_CRITICAL,  10000f },      // table 500 -> 500
			{ StatEnum.MAGICAL_CRITICAL,  -123.5f },      // table 500: negative value -> value
			{ StatEnum.MAGICAL_RESIST,     1000f },       // table 900 -> 900
			{ StatEnum.MAGICAL_RESIST,     850.25f },     // table 900: value < limit -> value
			{ StatEnum.EVASION,            500f },        // table 300 -> 300
			{ StatEnum.EVASION,            299.99f },     // table 300: value < limit -> value
			{ StatEnum.PARRY,              1000f },       // table 400 -> 400
			{ StatEnum.BOOST_MAGICAL_SKILL, 5000f },      // table 2900 -> 2900
			{ StatEnum.BOOST_MAGICAL_SKILL, 1234.5f },    // table 2900: value < limit -> value
			{ StatEnum.MAXHP,              123456.75f },  // no table entry -> Integer.MAX_VALUE -> value
			{ StatEnum.MAXHP,              3.0E9f },      // uncapped: value > 2.14748365E9f promotion -> 2.14748365E9f
		};
		for (Object[] in : inputs) {
			StatEnum stat = (StatEnum) in[0];
			float value = (Float) in[1];
			Map<String, Object> args = new LinkedHashMap<>();
			args.put("statEnum", quote(stat.name()));
			args.put("value", floatJson(value));
			cases.add(Case.ofFloat(args, StatFunctions.limit(stat, value)));
		}
		writeFixture(outDir.resolve("StatFunctions.limit.json"),
			"StatFunctions.limit",
			"float limit(StatEnum statEnum, float value)",
			cases);
	}

	// StatFunctions.adjustPvpDpGained(int points, int defeatedLvl, int killerLvl)
	// Cases exercise every branch plus int<-double truncation.
	private static void generateAdjustPvpDpGained(Path outDir) throws IOException {
		List<Case> cases = new ArrayList<>();
		int[][] inputs = {
			{ 1000, 50, 50 },   // difference 0      -> unchanged
			{ 1000, 50, 55 },   // difference 5      -> points - points*5*0.1
			{ 999, 50, 53 },    // difference 3      -> truncation: 999 - 299.7 -> 699
			{ 1000, 50, 60 },   // difference 10     -> 0
			{ 1000, 50, 62 },   // difference 12     -> 0
			{ 1000, 65, 50 },   // difference -15    -> points*1.1
			{ 1000, 53, 50 },   // difference -3     -> points + points*3*0.01
			{ 999, 52, 50 },    // difference -2     -> truncation: 999 + 19.98 -> 1018
			{ 12345, 40, 49 },  // difference 9      -> 12345 - 12345*9*0.1 -> truncation
			{ 7777, 60, 50 },   // difference -10    -> points*1.1 -> truncation
		};
		for (int[] in : inputs) {
			Map<String, Object> args = new LinkedHashMap<>();
			args.put("points", in[0]);
			args.put("defeatedLvl", in[1]);
			args.put("killerLvl", in[2]);
			cases.add(Case.ofLong(args, StatFunctions.adjustPvpDpGained(in[0], in[1], in[2])));
		}
		writeFixture(outDir.resolve("StatFunctions.adjustPvpDpGained.json"),
			"StatFunctions.adjustPvpDpGained",
			"int adjustPvpDpGained(int points, int defeatedLvl, int killerLvl)",
			cases);
	}

	// StatFunctions.calculateRatingMultiplier(NpcRating)
	private static void generateCalculateRatingMultiplier(Path outDir) throws IOException {
		List<Case> cases = new ArrayList<>();
		for (NpcRating r : NpcRating.values()) {
			Map<String, Object> args = new LinkedHashMap<>();
			args.put("npcRating", quote(r.name()));
			cases.add(Case.ofLong(args, StatFunctions.calculateRatingMultiplier(r)));
		}
		writeFixture(outDir.resolve("StatFunctions.calculateRatingMultiplier.json"),
			"StatFunctions.calculateRatingMultiplier",
			"int calculateRatingMultiplier(NpcRating npcRating)",
			cases);
	}

	// StatFunctions.getApNpcRating(NpcRating)
	private static void generateGetApNpcRating(Path outDir) throws IOException {
		List<Case> cases = new ArrayList<>();
		for (NpcRating r : NpcRating.values()) {
			Map<String, Object> args = new LinkedHashMap<>();
			args.put("npcRating", quote(r.name()));
			cases.add(Case.ofLong(args, StatFunctions.getApNpcRating(r)));
		}
		writeFixture(outDir.resolve("StatFunctions.getApNpcRating.json"),
			"StatFunctions.getApNpcRating",
			"int getApNpcRating(NpcRating npcRating)",
			cases);
	}

	// XPRewardEnum.xpRewardFrom(int levelDifference) — full table + clamp edges.
	private static void generateXpRewardFrom(Path outDir) throws IOException {
		List<Case> cases = new ArrayList<>();
		int[] diffs = { -50, -12, -11, -10, -5, -1, 0, 1, 2, 3, 4, 5, 100 };
		for (int d : diffs) {
			Map<String, Object> args = new LinkedHashMap<>();
			args.put("levelDifference", d);
			cases.add(Case.ofLong(args, XPRewardEnum.xpRewardFrom(d)));
		}
		writeFixture(outDir.resolve("XPRewardEnum.xpRewardFrom.json"),
			"XPRewardEnum.xpRewardFrom",
			"int xpRewardFrom(int levelDifference)",
			cases);
	}

	// DropRewardEnum.dropRewardFrom(int levelDifference) — full table + clamp edges.
	private static void generateDropRewardFrom(Path outDir) throws IOException {
		List<Case> cases = new ArrayList<>();
		int[] diffs = { -50, -11, -10, -9, -8, -7, -6, -5, -4, 0, 100 };
		for (int d : diffs) {
			Map<String, Object> args = new LinkedHashMap<>();
			args.put("levelDifference", d);
			cases.add(Case.ofLong(args, DropRewardEnum.dropRewardFrom(d)));
		}
		writeFixture(outDir.resolve("DropRewardEnum.dropRewardFrom.json"),
			"DropRewardEnum.dropRewardFrom",
			"int dropRewardFrom(int levelDifference)",
			cases);
	}

	// XPLossEnum.getExpLoss(int level, long expNeed) — below-6 guard, bucket boundaries, Math.round on integer-div.
	private static void generateGetExpLoss(Path outDir) throws IOException {
		List<Case> cases = new ArrayList<>();
		long[][] inputs = {
			{ 5, 1000000 },     // < 6 -> 0
			{ 6, 1000000 },     // LEVEL_6 param 1.0
			{ 30, 1000000 },    // LEVEL_30 param 1.0
			{ 31, 1000000 },    // LEVEL_40 param 0.35
			{ 40, 1234567 },    // LEVEL_40 param 0.35 with rounding
			{ 45, 999999 },     // LEVEL_50 param 0.25
			{ 50, 1000000 },    // LEVEL_50 param 0.25
			{ 65, 7777777 },    // LEVEL_65 param 0.25
			{ 66, 1000000 },    // > 65 -> falls through -> 0
		};
		for (long[] in : inputs) {
			Map<String, Object> args = new LinkedHashMap<>();
			args.put("level", (int) in[0]);
			args.put("expNeed", in[1]);
			cases.add(Case.ofLong(args, XPLossEnum.getExpLoss((int) in[0], in[1])));
		}
		writeFixture(outDir.resolve("XPLossEnum.getExpLoss.json"),
			"XPLossEnum.getExpLoss",
			"long getExpLoss(int level, long expNeed)",
			cases);
	}

	// AbyssRankEnum.getRankForPoints(int ap, int gp) — enum result serialized by NAME.
	private static void generateAbyssGetRankForPoints(Path outDir) throws IOException {
		List<Case> cases = new ArrayList<>();
		int[][] inputs = {
			{ 0, 0 },           // GRADE9_SOLDIER (floor)
			{ 1200, 0 },        // GRADE8_SOLDIER
			{ 150800, 0 },      // GRADE1_SOLDIER (last AP-gated)
			{ 1000000, 1244 },  // STAR1_OFFICER (GP-gated)
			{ 1000000, 12437 }, // SUPREME_COMMANDER (max)
			{ 50000, 0 },       // GRADE4_SOLDIER (42780 <= 50000 < 69700)
		};
		for (int[] in : inputs) {
			Map<String, Object> args = new LinkedHashMap<>();
			args.put("ap", in[0]);
			args.put("gp", in[1]);
			cases.add(Case.ofName(args, AbyssRankEnum.getRankForPoints(in[0], in[1]).name()));
		}
		writeFixture(outDir.resolve("AbyssRankEnum.getRankForPoints.json"),
			"AbyssRankEnum.getRankForPoints",
			"AbyssRankEnum getRankForPoints(int ap, int gp)",
			cases);
	}

	// AbyssRankEnum.getRankById(int id) — enum result serialized by NAME.
	private static void generateAbyssGetRankById(Path outDir) throws IOException {
		List<Case> cases = new ArrayList<>();
		int[] ids = { 1, 9, 10, 14, 18 };
		for (int id : ids) {
			Map<String, Object> args = new LinkedHashMap<>();
			args.put("id", id);
			cases.add(Case.ofName(args, AbyssRankEnum.getRankById(id).name()));
		}
		writeFixture(outDir.resolve("AbyssRankEnum.getRankById.json"),
			"AbyssRankEnum.getRankById",
			"AbyssRankEnum getRankById(int id)",
			cases);
	}

	private static String quote(String s) {
		return "\"" + s + "\"";
	}

	// Serialize a float as its raw IEEE-754 bits (a JSON int) so it round-trips BIT-EXACT
	// across Java and C# with no decimal-parse rounding. The C# side reads it via
	// BitConverter.Int32BitsToSingle. The companion human-readable decimal is emitted only as a
	// comment-free sibling field is unnecessary; bits alone are authoritative.
	private static String floatJson(float f) {
		return Integer.toString(Float.floatToRawIntBits(f));
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
		final Long numericResult;     // for int/long-returning methods
		final String nameResult;      // for enum-returning methods (serialized by name)
		final Integer floatBitsResult; // for float-returning methods (raw IEEE-754 bits)

		private Case(Map<String, Object> inputs, Long numericResult, String nameResult, Integer floatBitsResult) {
			this.inputs = inputs;
			this.numericResult = numericResult;
			this.nameResult = nameResult;
			this.floatBitsResult = floatBitsResult;
		}

		static Case ofLong(Map<String, Object> inputs, long result) {
			return new Case(inputs, result, null, null);
		}

		static Case ofName(Map<String, Object> inputs, String name) {
			return new Case(inputs, null, name, null);
		}

		// Float result serialized as raw IEEE-754 bits inside a tagged object so the C# reader
		// can distinguish it from a plain numeric/enum result.
		static Case ofFloat(Map<String, Object> inputs, float result) {
			return new Case(inputs, null, null, Float.floatToRawIntBits(result));
		}

		String resultJson() {
			if (nameResult != null)
				return "\"result\": \"" + nameResult + "\"";
			if (floatBitsResult != null)
				return "\"result\": { \"floatBits\": " + floatBitsResult + " }";
			return "\"result\": " + numericResult;
		}
	}
}
