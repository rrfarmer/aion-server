package com.aionemu.gameserver.services.findgroup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

final class FindGroupMutationPostTraceCaptureArtifactValidator {

	private FindGroupMutationPostTraceCaptureArtifactValidator() {
	}

	static ValidationReport validateExpectedArtifacts(Path artifactRoot) throws IOException {
		List<FileValidation> files = List.of(validateFile(artifactRoot, 2), validateFile(artifactRoot, 6));
		boolean allExpectedFiles = files.stream().allMatch(file -> file.status() != FileStatus.MISSING_FILE);
		boolean allShapeValid = allExpectedFiles && files.stream().allMatch(file -> file.status() == FileStatus.SHAPE_VALID);
		return new ValidationReport(
			allShapeValid ? DirectoryStatus.ALL_EXPECTED_ARTIFACTS_SHAPE_VALID
				: allExpectedFiles ? DirectoryStatus.INVALID_ARTIFACTS : DirectoryStatus.MISSING_EXPECTED_FILES,
			files,
			allExpectedFiles,
			allShapeValid,
			false);
	}

	private static FileValidation validateFile(Path artifactRoot, int action) throws IOException {
		Path path = FindGroupMutationPostTraceCaptureArtifactWriter.artifactPathForAction(artifactRoot, action);
		if (!Files.exists(path))
			return new FileValidation(action, path, FileStatus.MISSING_FILE, "Expected generated Java mutation-post artifact file is missing.");

		String json = Files.readString(path);
		String missingFragment = firstMissingFragment(json, expectedFragments(action));
		if (missingFragment != null)
			return new FileValidation(action, path, FileStatus.INVALID_ARTIFACT, "Missing or out-of-order required fragment: " + missingFragment);

		return new FileValidation(action, path, FileStatus.SHAPE_VALID, "Generated Java artifact is shape-valid only; runtime comparison remains blocked.");
	}

	private static List<String> expectedFragments(int action) {
		return switch (action) {
			case 2 -> List.of(
				"\"schemaVersion\": 1",
				"\"traceName\": \"cm-find-group-direct-mutation-post-boundary\"",
				"\"traces\": [",
				"\"schemaVersion\": 1",
				"\"traceName\": \"cm-find-group-direct-mutation-post-boundary\"",
				"\"traceSource\": \"Java\"",
				"\"action\": 2",
				"\"boundaryAccepted\": true",
				"\"mutationKind\": \"Recruitment\"",
				"\"stateMutationRecordedBeforeDirectPackets\": true",
				"\"postedSystemMessageType\": \"SmSystemMessage\"",
				"\"postedSystemMessageId\": 1400392",
				"\"refreshedListPacketType\": \"SmFindGroup\"",
				"\"refreshedListAction\": 0",
				"\"visibleEntryObjectIdsAfterMutation\": [",
				"\"worldBroadcastCount\": 0",
				"\"inviteDispatchCount\": 0");
			case 6 -> List.of(
				"\"schemaVersion\": 1",
				"\"traceName\": \"cm-find-group-direct-mutation-post-boundary\"",
				"\"traces\": [",
				"\"schemaVersion\": 1",
				"\"traceName\": \"cm-find-group-direct-mutation-post-boundary\"",
				"\"traceSource\": \"Java\"",
				"\"action\": 6",
				"\"boundaryAccepted\": true",
				"\"mutationKind\": \"Application\"",
				"\"stateMutationRecordedBeforeDirectPackets\": true",
				"\"postedSystemMessageType\": \"SmSystemMessage\"",
				"\"postedSystemMessageId\": 1400393",
				"\"refreshedListPacketType\": \"SmFindGroup\"",
				"\"refreshedListAction\": 4",
				"\"visibleEntryObjectIdsAfterMutation\": [",
				"\"worldBroadcastCount\": 0",
				"\"inviteDispatchCount\": 0");
			default -> throw new IllegalArgumentException("Unsupported mutation-post action " + action);
		};
	}

	private static String firstMissingFragment(String text, List<String> fragments) {
		int currentIndex = -1;
		for (String fragment : fragments) {
			int nextIndex = text.indexOf(fragment, currentIndex + 1);
			if (nextIndex <= currentIndex)
				return fragment;
			currentIndex = nextIndex;
		}
		return null;
	}

	enum DirectoryStatus {
		MISSING_EXPECTED_FILES,
		INVALID_ARTIFACTS,
		ALL_EXPECTED_ARTIFACTS_SHAPE_VALID
	}

	enum FileStatus {
		MISSING_FILE,
		INVALID_ARTIFACT,
		SHAPE_VALID
	}

	record ValidationReport(
		DirectoryStatus status,
		List<FileValidation> files,
		boolean hasAllExpectedFiles,
		boolean hasOnlyShapeValidArtifacts,
		boolean readyForRuntimeComparison) {
	}

	record FileValidation(
		int action,
		Path path,
		FileStatus status,
		String notes) {
	}
}
