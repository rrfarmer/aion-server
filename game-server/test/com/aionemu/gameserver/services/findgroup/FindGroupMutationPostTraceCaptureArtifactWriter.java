package com.aionemu.gameserver.services.findgroup;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

final class FindGroupMutationPostTraceCaptureArtifactWriter {

	static final Path DEFAULT_ARTIFACT_ROOT = Path.of("parity-artifacts/find-group/mutation-post/java");

	private FindGroupMutationPostTraceCaptureArtifactWriter() {
	}

	static String fileNameForAction(int action) {
		validateSupportedAction(action);
		return "cm-find-group-direct-mutation-post-boundary-action-" + action + "-java.json";
	}

	static Path artifactPathForAction(Path artifactRoot, int action) {
		return artifactRoot.resolve(fileNameForAction(action));
	}

	static Optional<Path> tryWriteArtifact(
		Path artifactRoot,
		int action,
		List<FindGroupMutationPostTraceCaptureSerializer.TraceRow> rows) throws IOException {
		validateExpectedActionRow(action, rows);
		Optional<String> artifactJson = FindGroupMutationPostTraceCaptureSerializer.trySerializeArtifact(rows);
		if (artifactJson.isEmpty())
			return Optional.empty();

		Files.createDirectories(artifactRoot);
		Path artifactPath = artifactPathForAction(artifactRoot, action);
		Files.writeString(artifactPath, artifactJson.get(), StandardCharsets.UTF_8);
		return Optional.of(artifactPath);
	}

	private static void validateExpectedActionRow(
		int action,
		List<FindGroupMutationPostTraceCaptureSerializer.TraceRow> rows) {
		validateSupportedAction(action);
		if (rows.stream().noneMatch(row -> row.action() == action))
			throw new IllegalArgumentException("Artifact for action " + action + " must contain a matching mutation-post trace row.");
	}

	private static void validateSupportedAction(int action) {
		if (action != 2 && action != 6)
			throw new IllegalArgumentException("Unsupported mutation-post action " + action);
	}
}
