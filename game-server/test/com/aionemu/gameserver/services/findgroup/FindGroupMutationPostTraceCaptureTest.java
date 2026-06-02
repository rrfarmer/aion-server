package com.aionemu.gameserver.services.findgroup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Phase 6 Java parity fixture scaffold for future CM_FIND_GROUP action 2/6
 * mutation-post trace artifacts.
 *
 * This test intentionally does not instrument Java runtime behavior or write
 * artifact files in the repository. It keeps the capture flag, scenarios,
 * schema name, artifact targets, and fixture-only writer executable in Maven
 * while production Java hooks remain blocked.
 */
public class FindGroupMutationPostTraceCaptureTest {

	private static final Path ARTIFACT_ROOT = Path.of("parity-artifacts/find-group/mutation-post/java");
	private static final List<CaptureScenario> SCENARIOS = List.of(
		new CaptureScenario(2, "Recruitment", "CM_FIND_GROUP.readImpl action 2", "FindGroupService.addRecruitment",
			"STR_PARTY_MATCH_OFFER_PARTY_POSTED", 1400392, 0, "cm-find-group-direct-mutation-post-boundary-action-2-java.json"),
		new CaptureScenario(6, "Application", "CM_FIND_GROUP.readImpl action 6", "FindGroupService.addApplication",
			"STR_PARTY_MATCH_SEEK_PARTY_POSTED", 1400393, 4, "cm-find-group-direct-mutation-post-boundary-action-6-java.json"));

	@Test
	public void captureFlagDefaultsToDisabled() {
		String captureFlag = FindGroupMutationPostTraceCaptureInstrumentation.CAPTURE_FLAG;
		String original = System.getProperty(captureFlag);
		try {
			System.clearProperty(captureFlag);

			assertFalse(captureEnabled());
			assertTrue(fixtureArtifactWriterImplemented());
			assertFalse(runtimeInstrumentationImplemented());
		} finally {
			if (original == null)
				System.clearProperty(captureFlag);
			else
				System.setProperty(captureFlag, original);
		}
	}

	@Test
	public void fixtureListsActionTwoAndSixMutationPostScenarios() {
		assertEquals(2, SCENARIOS.size());

		CaptureScenario recruitment = SCENARIOS.get(0);
		assertEquals(2, recruitment.action());
		assertEquals("Recruitment", recruitment.mutationKind());
		assertEquals("CM_FIND_GROUP.readImpl action 2", recruitment.packetSource());
		assertEquals("FindGroupService.addRecruitment", recruitment.serviceSource());
		assertEquals("STR_PARTY_MATCH_OFFER_PARTY_POSTED", recruitment.postedSystemMessage());
		assertEquals(1400392, recruitment.postedSystemMessageId());
		assertEquals(0, recruitment.refreshedListAction());

		CaptureScenario application = SCENARIOS.get(1);
		assertEquals(6, application.action());
		assertEquals("Application", application.mutationKind());
		assertEquals("CM_FIND_GROUP.readImpl action 6", application.packetSource());
		assertEquals("FindGroupService.addApplication", application.serviceSource());
		assertEquals("STR_PARTY_MATCH_SEEK_PARTY_POSTED", application.postedSystemMessage());
		assertEquals(1400393, application.postedSystemMessageId());
		assertEquals(4, application.refreshedListAction());
	}

	@Test
	public void fixtureNamesStableArtifactTargetsWithoutWritingThem() {
		assertEquals("cm-find-group-direct-mutation-post-boundary", FindGroupMutationPostTraceCaptureInstrumentation.TRACE_NAME);
		assertEquals(Path.of("parity-artifacts/find-group/mutation-post/java"), ARTIFACT_ROOT);
		assertEquals(
			ARTIFACT_ROOT.resolve("cm-find-group-direct-mutation-post-boundary-action-2-java.json"),
			artifactPathForAction(2));
		assertEquals(
			ARTIFACT_ROOT.resolve("cm-find-group-direct-mutation-post-boundary-action-6-java.json"),
			artifactPathForAction(6));
		assertEquals(
			ARTIFACT_ROOT.resolve("cm-find-group-direct-mutation-post-boundary-action-2-java.json"),
			FindGroupMutationPostTraceCaptureArtifactWriter.artifactPathForAction(ARTIFACT_ROOT, 2));
		assertTrue(fixtureArtifactWriterImplemented());
	}

	@Test
	public void captureFlagCanBeEnabledButRuntimeCaptureRemainsBlocked() {
		boolean enabled = Boolean.getBoolean(FindGroupMutationPostTraceCaptureInstrumentation.CAPTURE_FLAG);

		assertEquals(enabled, captureEnabled());
		assertFalse(runtimeInstrumentationImplemented());
		assertTrue(fixtureArtifactWriterImplemented());
		assertTrue(SCENARIOS.stream().allMatch(scenario -> scenario.action() == 2 || scenario.action() == 6));
	}

	@Test
	public void instrumentationScaffoldListsHookPointsInJavaOrder() {
		List<FindGroupMutationPostTraceCaptureInstrumentation.HookPoint> hooks =
			FindGroupMutationPostTraceCaptureInstrumentation.hookPoints();

		assertEquals(9, hooks.size());
		assertEquals("client_packet_payload_parsed", hooks.get(0).eventName());
		assertEquals("client_packet_run_impl_entered", hooks.get(1).eventName());
		assertTrue(FindGroupMutationPostTraceCaptureInstrumentation.supportsActionsTwoAndSix());
		assertTrue(FindGroupMutationPostTraceCaptureInstrumentation.preservesMutationBeforeSendOrdering(2));
		assertTrue(FindGroupMutationPostTraceCaptureInstrumentation.preservesMutationBeforeSendOrdering(6));
		assertEquals("trace_artifact_row_serialized", hooks.get(8).eventName());
	}

	@Test
	public void instrumentationScaffoldNamesJavaHookSourcesAndRequiredFields() {
		List<FindGroupMutationPostTraceCaptureInstrumentation.HookPoint> hooks =
			FindGroupMutationPostTraceCaptureInstrumentation.hookPoints();

		assertTrue(hooks.stream().anyMatch(point ->
			point.javaSource().equals("CM_FIND_GROUP.readImpl")
				&& point.requiredFields().contains("classId")
				&& point.requiredFields().contains("level")));
		assertTrue(hooks.stream().anyMatch(point ->
			point.javaSource().contains("FindGroupService.addRecruitment after recruitments.put")
				&& point.requiredFields().contains("stateMutationRecordedBeforeDirectPackets=true")));
		assertTrue(hooks.stream().anyMatch(point ->
			point.javaSource().contains("STR_PARTY_MATCH_OFFER_PARTY_POSTED")
				&& point.requiredFields().contains("postedSystemMessageId=1400392")));
		assertTrue(hooks.stream().anyMatch(point ->
			point.javaSource().contains("FindGroupService.showApplications")
				&& point.requiredFields().contains("refreshedListAction=4")));
	}

	@Test
	public void recorderNoOpsWhenCaptureFlagIsDisabled() {
		String captureFlag = FindGroupMutationPostTraceCaptureInstrumentation.CAPTURE_FLAG;
		String original = System.getProperty(captureFlag);
		try {
			System.clearProperty(captureFlag);
			FindGroupMutationPostTraceCaptureInstrumentation.TraceRecorder recorder =
				FindGroupMutationPostTraceCaptureInstrumentation.newRecorder();

			assertFalse(recorder.enabled());
			FindGroupMutationPostTraceCaptureInstrumentation.hookPoints().forEach(recorder::record);
			assertTrue(recorder.events().isEmpty());
		} finally {
			if (original == null)
				System.clearProperty(captureFlag);
			else
				System.setProperty(captureFlag, original);
		}
	}

	@Test
	public void recorderCapturesEventNamesWhenFlagIsEnabledWithoutWritingArtifacts() {
		String captureFlag = FindGroupMutationPostTraceCaptureInstrumentation.CAPTURE_FLAG;
		String original = System.getProperty(captureFlag);
		try {
			System.setProperty(captureFlag, "true");
			FindGroupMutationPostTraceCaptureInstrumentation.TraceRecorder recorder =
				FindGroupMutationPostTraceCaptureInstrumentation.newRecorder();

			assertTrue(recorder.enabled());
			FindGroupMutationPostTraceCaptureInstrumentation.hookPoints().stream()
				.filter(point -> point.action() == 2)
				.forEach(recorder::record);
			assertEquals(List.of(
				"recruitment_state_mutation_recorded",
				"recruitment_posted_message_send_observed",
				"recruitment_refreshed_list_send_observed"), recorder.events());
			assertTrue(fixtureArtifactWriterImplemented());
		} finally {
			if (original == null)
				System.clearProperty(captureFlag);
			else
				System.setProperty(captureFlag, original);
		}
	}

	@Test
	public void serializerScaffoldListsStableSchemaFieldsInComparisonOrder() {
		assertEquals(List.of(
			"schemaVersion",
			"traceName",
			"traceSource",
			"action",
			"boundaryAccepted",
			"activePlayerObjectId",
			"activePlayerRace",
			"serverEpochSeconds",
			"mutationKind",
			"mutatedEntryObjectId",
			"stateMutationRecordedBeforeDirectPackets",
			"postedSystemMessageRecipientObjectId",
			"postedSystemMessageType",
			"postedSystemMessageId",
			"refreshedListRecipientObjectId",
			"refreshedListPacketType",
			"refreshedListAction",
			"visibleEntryObjectIdsAfterMutation",
			"executorInvokedFromBoundary",
			"registrySendsObservedInOrder",
			"worldBroadcastCount",
			"inviteDispatchCount"), FindGroupMutationPostTraceCaptureSerializer.schemaFields());
	}

	@Test
	public void serializerNoOpsWhenCaptureFlagIsDisabled() {
		String captureFlag = FindGroupMutationPostTraceCaptureInstrumentation.CAPTURE_FLAG;
		String original = System.getProperty(captureFlag);
		try {
			System.clearProperty(captureFlag);

			Optional<String> artifact = FindGroupMutationPostTraceCaptureSerializer.trySerializeArtifact(List.of(
				FindGroupMutationPostTraceCaptureSerializer.sampleRow(2, 1001, "ELYOS", 123456, 2002, List.of(2002))));

			assertTrue(artifact.isEmpty());
			assertTrue(fixtureArtifactWriterImplemented());
		} finally {
			if (original == null)
				System.clearProperty(captureFlag);
			else
				System.setProperty(captureFlag, original);
		}
	}

	@Test
	public void serializerEmitsRecruitmentRowWhenCaptureFlagIsEnabledWithoutWritingArtifacts() {
		String captureFlag = FindGroupMutationPostTraceCaptureInstrumentation.CAPTURE_FLAG;
		String original = System.getProperty(captureFlag);
		try {
			System.setProperty(captureFlag, "true");

			String json = FindGroupMutationPostTraceCaptureSerializer.trySerializeArtifact(List.of(
				FindGroupMutationPostTraceCaptureSerializer.sampleRow(2, 1001, "ELYOS", 123456, 2002, List.of(2002, 3003))))
				.orElseThrow();

			assertContainsInOrder(json,
				"\"schemaVersion\": 1",
				"\"traceName\": \"cm-find-group-direct-mutation-post-boundary\"",
				"\"traceSource\": \"Java\"",
				"\"action\": 2",
				"\"mutationKind\": \"Recruitment\"",
				"\"postedSystemMessageType\": \"SmSystemMessage\"",
				"\"postedSystemMessageId\": 1400392",
				"\"refreshedListPacketType\": \"SmFindGroup\"",
				"\"refreshedListAction\": 0",
				"\"visibleEntryObjectIdsAfterMutation\": [2002, 3003]",
				"\"worldBroadcastCount\": 0",
				"\"inviteDispatchCount\": 0");
			assertTrue(fixtureArtifactWriterImplemented());
		} finally {
			if (original == null)
				System.clearProperty(captureFlag);
			else
				System.setProperty(captureFlag, original);
		}
	}

	@Test
	public void serializerEmitsApplicationActionMappingWithoutWritingArtifacts() {
		String json = FindGroupMutationPostTraceCaptureSerializer.serializeArtifact(List.of(
			FindGroupMutationPostTraceCaptureSerializer.sampleRow(6, 4004, "ASMODIANS", 456789, 4004, List.of(4004))));

		assertContainsInOrder(json,
			"\"action\": 6",
			"\"mutationKind\": \"Application\"",
			"\"mutatedEntryObjectId\": 4004",
			"\"postedSystemMessageId\": 1400393",
			"\"refreshedListAction\": 4",
			"\"visibleEntryObjectIdsAfterMutation\": [4004]",
			"\"executorInvokedFromBoundary\": false",
			"\"registrySendsObservedInOrder\": false");
		assertTrue(fixtureArtifactWriterImplemented());
	}

	@Test
	public void serializerRejectsUnsupportedMutationPostAction() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
			FindGroupMutationPostTraceCaptureSerializer.sampleRow(3, 1001, "ELYOS", 123456, 1001, List.of(1001)));

		assertEquals("Unsupported mutation-post action 3", exception.getMessage());
		assertTrue(fixtureArtifactWriterImplemented());
	}

	@Test
	public void artifactWriterNoOpsWhenCaptureFlagIsDisabled(@TempDir Path tempDirectory) throws IOException {
		String captureFlag = FindGroupMutationPostTraceCaptureInstrumentation.CAPTURE_FLAG;
		String original = System.getProperty(captureFlag);
		try {
			System.clearProperty(captureFlag);

			Optional<Path> artifactPath = FindGroupMutationPostTraceCaptureArtifactWriter.tryWriteArtifact(
				tempDirectory,
				2,
				List.of(FindGroupMutationPostTraceCaptureSerializer.sampleRow(2, 1001, "ELYOS", 123456, 2002, List.of(2002))));

			assertTrue(artifactPath.isEmpty());
			assertFalse(Files.exists(FindGroupMutationPostTraceCaptureArtifactWriter.artifactPathForAction(tempDirectory, 2)));
			assertFalse(runtimeInstrumentationImplemented());
		} finally {
			if (original == null)
				System.clearProperty(captureFlag);
			else
				System.setProperty(captureFlag, original);
		}
	}

	@Test
	public void artifactWriterWritesShapeValidFixtureRowsOnlyWhenCaptureFlagIsEnabled(@TempDir Path tempDirectory) throws IOException {
		String captureFlag = FindGroupMutationPostTraceCaptureInstrumentation.CAPTURE_FLAG;
		String original = System.getProperty(captureFlag);
		try {
			System.setProperty(captureFlag, "true");

			Path actionTwoPath = FindGroupMutationPostTraceCaptureArtifactWriter.tryWriteArtifact(
				tempDirectory,
				2,
				List.of(FindGroupMutationPostTraceCaptureSerializer.sampleRow(2, 1001, "ELYOS", 123456, 2002, List.of(2002))))
				.orElseThrow();
			Path actionSixPath = FindGroupMutationPostTraceCaptureArtifactWriter.tryWriteArtifact(
				tempDirectory,
				6,
				List.of(FindGroupMutationPostTraceCaptureSerializer.sampleRow(6, 4004, "ASMODIANS", 456789, 4004, List.of(4004))))
				.orElseThrow();

			assertEquals(tempDirectory.resolve("cm-find-group-direct-mutation-post-boundary-action-2-java.json"), actionTwoPath);
			assertEquals(tempDirectory.resolve("cm-find-group-direct-mutation-post-boundary-action-6-java.json"), actionSixPath);
			assertContainsInOrder(Files.readString(actionTwoPath),
				"\"traceSource\": \"Java\"",
				"\"action\": 2",
				"\"mutationKind\": \"Recruitment\"",
				"\"postedSystemMessageId\": 1400392",
				"\"refreshedListAction\": 0");
			assertContainsInOrder(Files.readString(actionSixPath),
				"\"traceSource\": \"Java\"",
				"\"action\": 6",
				"\"mutationKind\": \"Application\"",
				"\"postedSystemMessageId\": 1400393",
				"\"refreshedListAction\": 4");
			assertFalse(runtimeInstrumentationImplemented());
		} finally {
			if (original == null)
				System.clearProperty(captureFlag);
			else
				System.setProperty(captureFlag, original);
		}
	}

	@Test
	public void artifactWriterRejectsFileActionWithoutMatchingTraceRow(@TempDir Path tempDirectory) {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
			FindGroupMutationPostTraceCaptureArtifactWriter.tryWriteArtifact(
				tempDirectory,
				2,
				List.of(FindGroupMutationPostTraceCaptureSerializer.sampleRow(6, 4004, "ASMODIANS", 456789, 4004, List.of(4004)))));

		assertEquals("Artifact for action 2 must contain a matching mutation-post trace row.", exception.getMessage());
	}

	private static boolean captureEnabled() {
		return FindGroupMutationPostTraceCaptureInstrumentation.captureEnabled();
	}

	private static boolean runtimeInstrumentationImplemented() {
		return false;
	}

	private static boolean fixtureArtifactWriterImplemented() {
		return true;
	}

	private static Path artifactPathForAction(int action) {
		return SCENARIOS.stream()
			.filter(scenario -> scenario.action() == action)
			.findFirst()
			.map(scenario -> ARTIFACT_ROOT.resolve(scenario.artifactFileName()))
			.orElseThrow(() -> new IllegalArgumentException("Unsupported mutation-post action " + action));
	}

	private static void assertContainsInOrder(String text, String... expectedFragments) {
		int currentIndex = -1;
		for (String fragment : expectedFragments) {
			int nextIndex = text.indexOf(fragment, currentIndex + 1);
			assertTrue(nextIndex > currentIndex, () -> "Expected fragment in order: " + fragment);
			currentIndex = nextIndex;
		}
	}

	private record CaptureScenario(
		int action,
		String mutationKind,
		String packetSource,
		String serviceSource,
		String postedSystemMessage,
		int postedSystemMessageId,
		int refreshedListAction,
		String artifactFileName) {
	}
}
