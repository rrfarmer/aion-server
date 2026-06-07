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
 * Only pure, primitive-driven methods belong here (no Player/Creature/world context).
 */
public class GoldenFormulaFixtureGeneratorTest {

	@Test
	public void generateGoldenFormulaFixtures() throws IOException {
		Path outDir = repoRoot().resolve("parity-artifacts/golden/formulas");
		Files.createDirectories(outDir);

		// StatFunctions.adjustPvpDpGained(int points, int defeatedLvl, int killerLvl)
		// Cases exercise every branch plus int<-double truncation.
		List<Case> adjustPvpDp = new ArrayList<>();
		int[][] inputs = {
			{ 1000, 50, 50 },   // difference 0      -> unchanged
			{ 1000, 50, 55 },   // difference 5      -> points - points*5*0.1
			{ 999, 50, 53 },    // difference 3      -> truncation: 999 - 299.7 -> 699
			{ 1000, 50, 60 },   // difference 10     -> 0
			{ 1000, 50, 62 },   // difference 12     -> 0
			{ 1000, 65, 50 },   // difference -15    -> points*1.1
			{ 1000, 53, 50 },   // difference -3     -> points + points*3*0.01
			{ 999, 52, 50 },    // difference -2     -> truncation: 999 + 19.98 -> 1018
		};
		for (int[] in : inputs) {
			Map<String, Object> args = new LinkedHashMap<>();
			args.put("points", in[0]);
			args.put("defeatedLvl", in[1]);
			args.put("killerLvl", in[2]);
			adjustPvpDp.add(new Case(args, StatFunctions.adjustPvpDpGained(in[0], in[1], in[2])));
		}
		writeFixture(outDir.resolve("StatFunctions.adjustPvpDpGained.json"),
			"StatFunctions.adjustPvpDpGained",
			"int adjustPvpDpGained(int points, int defeatedLvl, int killerLvl)",
			adjustPvpDp);
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
			sb.append("}, \"result\": ").append(c.result).append(" }");
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
		final long result;

		Case(Map<String, Object> inputs, long result) {
			this.inputs = inputs;
			this.result = result;
		}
	}
}
