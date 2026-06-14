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

		private Case(Map<String, Object> inputs, Long numericResult, String nameResult) {
			this.inputs = inputs;
			this.numericResult = numericResult;
			this.nameResult = nameResult;
		}

		static Case ofLong(Map<String, Object> inputs, long result) {
			return new Case(inputs, result, null);
		}

		static Case ofName(Map<String, Object> inputs, String name) {
			return new Case(inputs, null, name);
		}

		String resultJson() {
			if (nameResult != null)
				return "\"result\": \"" + nameResult + "\"";
			return "\"result\": " + numericResult;
		}
	}
}
