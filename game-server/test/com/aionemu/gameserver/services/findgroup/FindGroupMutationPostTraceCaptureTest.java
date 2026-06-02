package com.aionemu.gameserver.services.findgroup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.lang.reflect.Field;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.account.Account;
import com.aionemu.gameserver.model.account.PlayerAccountData;
import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.findGroup.GroupApplication;
import com.aionemu.gameserver.model.gameobjects.findGroup.GroupRecruitment;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerAppearance;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.world.WorldPosition;

import sun.misc.Unsafe;

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
	private static final FindGroupMutationPostTraceCaptureScenarioBuilder.Scenario RECRUITMENT_SCENARIO =
		FindGroupMutationPostTraceCaptureScenarioBuilder.recruitmentScenario();
	private static final FindGroupMutationPostTraceCaptureScenarioBuilder.Scenario APPLICATION_SCENARIO =
		FindGroupMutationPostTraceCaptureScenarioBuilder.applicationScenario();

	@Test
	public void captureFlagDefaultsToDisabled() {
		String captureFlag = FindGroupMutationPostTraceCaptureInstrumentation.CAPTURE_FLAG;
		String original = System.getProperty(captureFlag);
		try {
			System.clearProperty(captureFlag);

			assertFalse(captureEnabled());
			assertTrue(fixtureArtifactWriterImplemented());
			assertTrue(productionHookIntegrationImplemented());
			assertFalse(productionHookArtifactOutputEnabled());
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
		assertEquals(enabled, FindGroupMutationPostTraceCaptureHooks.captureEnabled());
		assertTrue(productionHookIntegrationImplemented());
		assertFalse(productionHookArtifactOutputEnabled());
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
	public void hookPlacementPreflightNamesProductionJavaStatementsWithNoArtifactOutput() {
		List<FindGroupMutationPostTraceCaptureHookPlacementPreflight.Placement> placements =
			FindGroupMutationPostTraceCaptureHookPlacementPreflight.placements();

		assertEquals(2, placements.size());
		assertTrue(placements.stream().anyMatch(placement ->
			placement.action() == 2
				&& placement.mutationKind().equals("Recruitment")
				&& placement.mutationStatement().contains("recruitments.put")
				&& placement.stateMutationHookStatement().contains("recordRecruitmentStateMutation")
				&& placement.postedSystemMessageHookStatement().contains("recordRecruitmentPostedMessageSend")
				&& placement.postedSystemMessageStatement().contains("STR_PARTY_MATCH_OFFER_PARTY_POSTED")
				&& placement.refreshCallStatement().equals("showRecruitments(player);")
				&& placement.refreshedListHookStatement().contains("recordRecruitmentRefreshedListSend")
				&& placement.refreshedListSendStatement().contains("new SM_FIND_GROUP(0, recruitments)")));
		assertTrue(placements.stream().anyMatch(placement ->
			placement.action() == 6
				&& placement.mutationKind().equals("Application")
				&& placement.mutationStatement().contains("applications.put")
				&& placement.stateMutationHookStatement().contains("recordApplicationStateMutation")
				&& placement.postedSystemMessageHookStatement().contains("recordApplicationPostedMessageSend")
				&& placement.postedSystemMessageStatement().contains("STR_PARTY_MATCH_SEEK_PARTY_POSTED")
				&& placement.refreshCallStatement().equals("showApplications(player);")
				&& placement.refreshedListHookStatement().contains("recordApplicationRefreshedListSend")
				&& placement.refreshedListSendStatement().contains("new SM_FIND_GROUP(4, applications)")));
		assertTrue(productionHookIntegrationImplemented());
		assertFalse(productionHookArtifactOutputEnabled());
		assertFalse(runtimeInstrumentationImplemented());
	}

	@Test
	public void hookPlacementPreflightConfirmsJavaMutationBeforePostedBeforeRefreshOrdering() throws IOException {
		FindGroupMutationPostTraceCaptureHookPlacementPreflight.Report report =
			FindGroupMutationPostTraceCaptureHookPlacementPreflight.inspectDefaultSource();

		assertTrue(report.allPlacementsPreserveJavaMutationPostOrdering());
		assertTrue(report.allProductionHooksPlacedBeforeObservedSends());
		assertTrue(report.productionHooksIntegrated());
		assertFalse(report.artifactOutputEnabled());
		assertEquals(List.of(2, 6), report.rows().stream().map(FindGroupMutationPostTraceCaptureHookPlacementPreflight.Row::action).toList());
		assertTrue(report.rows().stream().allMatch(row ->
			row.mutationIndex() >= 0
				&& row.stateMutationHookIndex() > row.mutationIndex()
				&& row.postedSystemMessageHookIndex() > row.stateMutationHookIndex()
				&& row.postedSystemMessageIndex() > row.postedSystemMessageHookIndex()
				&& row.postedSystemMessageIndex() > row.mutationIndex()
				&& row.refreshCallIndex() > row.postedSystemMessageIndex()
				&& row.refreshedListSendIndex() >= 0
				&& row.refreshedListSendIndex() > row.refreshedListHookIndex()
				&& row.mutationBeforePostedBeforeRefresh()
				&& row.productionHooksPlacedBeforeObservedSends()));
	}

	@Test
	public void productionHooksNoOpWithoutArtifactOutputEvenWhenCaptureFlagIsEnabled() {
		String captureFlag = FindGroupMutationPostTraceCaptureHooks.CAPTURE_FLAG;
		String original = System.getProperty(captureFlag);
		try {
			System.setProperty(captureFlag, "true");

			assertTrue(FindGroupMutationPostTraceCaptureHooks.captureEnabled());
			assertFalse(FindGroupMutationPostTraceCaptureHooks.artifactOutputEnabled());
			FindGroupMutationPostTraceCaptureHooks.recordRecruitmentPostedMessageSend(null);
			FindGroupMutationPostTraceCaptureHooks.recordRecruitmentRefreshedListSend(null, List.of());
			FindGroupMutationPostTraceCaptureHooks.recordApplicationPostedMessageSend(null);
			FindGroupMutationPostTraceCaptureHooks.recordApplicationRefreshedListSend(null, List.of());
			assertFalse(runtimeInstrumentationImplemented());
		} finally {
			if (original == null)
				System.clearProperty(captureFlag);
			else
				System.setProperty(captureFlag, original);
		}
	}

	@Test
	public void productionHooksDoNotPopulateInMemoryRowsWhenCaptureFlagIsDisabled() throws Exception {
		String captureFlag = FindGroupMutationPostTraceCaptureHooks.CAPTURE_FLAG;
		String original = System.getProperty(captureFlag);
		try {
			System.clearProperty(captureFlag);
			FindGroupMutationPostTraceCaptureHooks.clearInMemoryTraceRows();
			Player player = simplePlayer(2002, "Recruiter", Race.ELYOS);
			GroupRecruitment recruitment = new GroupRecruitment(player, "Recruit", 3);

			FindGroupMutationPostTraceCaptureHooks.recordRecruitmentStateMutation(player, recruitment);
			FindGroupMutationPostTraceCaptureHooks.recordRecruitmentPostedMessageSend(player);
			FindGroupMutationPostTraceCaptureHooks.recordRecruitmentRefreshedListSend(player, List.of(recruitment));

			assertTrue(FindGroupMutationPostTraceCaptureHooks.traceRows().isEmpty());
			assertTrue(FindGroupMutationPostTraceCaptureHooks.drainTraceRows().isEmpty());
			assertFalse(productionHookArtifactOutputEnabled());
		} finally {
			FindGroupMutationPostTraceCaptureHooks.clearInMemoryTraceRows();
			if (original == null)
				System.clearProperty(captureFlag);
			else
				System.setProperty(captureFlag, original);
		}
	}

	@Test
	public void productionHooksAssembleMutationPostRowsInMemoryWithoutWritingArtifacts() throws Exception {
		String captureFlag = FindGroupMutationPostTraceCaptureHooks.CAPTURE_FLAG;
		String timestampProperty = FindGroupMutationPostTraceCaptureHooks.SERVER_EPOCH_SECONDS_PROPERTY;
		String original = System.getProperty(captureFlag);
		String originalTimestamp = System.getProperty(timestampProperty);
		try {
			System.setProperty(captureFlag, "true");
			System.clearProperty(timestampProperty);
			FindGroupMutationPostTraceCaptureHooks.clearInMemoryTraceRows();
			Player recruiter = simplePlayer(2002, "Recruiter", Race.ELYOS);
			GroupRecruitment recruitment = new GroupRecruitment(recruiter, "Recruit", 3);
			GroupRecruitment visibleRecruitment = new GroupRecruitment(simplePlayer(3003, "VisibleRecruit", Race.ELYOS), "Other", 4);
			Player applicant = simplePlayer(4004, "Applicant", Race.ASMODIANS);
			GroupApplication application = new GroupApplication(applicant, "Apply", 5, 7, 45);

			FindGroupMutationPostTraceCaptureHooks.recordRecruitmentStateMutation(recruiter, recruitment);
			FindGroupMutationPostTraceCaptureHooks.recordRecruitmentPostedMessageSend(recruiter);
			FindGroupMutationPostTraceCaptureHooks.recordRecruitmentRefreshedListSend(recruiter, List.of(recruitment, visibleRecruitment));
			FindGroupMutationPostTraceCaptureHooks.recordApplicationStateMutation(applicant, application);
			FindGroupMutationPostTraceCaptureHooks.recordApplicationPostedMessageSend(applicant);
			FindGroupMutationPostTraceCaptureHooks.recordApplicationRefreshedListSend(applicant, List.of(application));

			List<FindGroupMutationPostTraceCaptureHooks.TraceRow> rows = FindGroupMutationPostTraceCaptureHooks.drainTraceRows();

			assertEquals(2, rows.size());
			FindGroupMutationPostTraceCaptureHooks.TraceRow recruitmentRow = rows.get(0);
			assertEquals(2, recruitmentRow.action());
			assertEquals("Recruitment", recruitmentRow.mutationKind());
			assertEquals(2002, recruitmentRow.activePlayerObjectId());
			assertEquals("ELYOS", recruitmentRow.activePlayerRace());
			assertEquals(recruitment.getLastUpdate(), recruitmentRow.serverEpochSeconds());
			assertEquals(2002, recruitmentRow.mutatedEntryObjectId());
			assertEquals(2002, recruitmentRow.postedSystemMessageRecipientObjectId());
			assertEquals("SmSystemMessage", recruitmentRow.postedSystemMessageType());
			assertEquals(1400392, recruitmentRow.postedSystemMessageId());
			assertEquals(2002, recruitmentRow.refreshedListRecipientObjectId());
			assertEquals("SmFindGroup", recruitmentRow.refreshedListPacketType());
			assertEquals(0, recruitmentRow.refreshedListAction());
			assertEquals(List.of(2002, 3003), recruitmentRow.visibleEntryObjectIdsAfterMutation());
			assertTrue(recruitmentRow.boundaryAccepted());
			assertTrue(recruitmentRow.stateMutationRecordedBeforeDirectPackets());
			assertFalse(recruitmentRow.executorInvokedFromBoundary());
			assertFalse(recruitmentRow.registrySendsObservedInOrder());
			assertEquals(0, recruitmentRow.worldBroadcastCount());
			assertEquals(0, recruitmentRow.inviteDispatchCount());

			FindGroupMutationPostTraceCaptureHooks.TraceRow applicationRow = rows.get(1);
			assertEquals(6, applicationRow.action());
			assertEquals("Application", applicationRow.mutationKind());
			assertEquals(4004, applicationRow.activePlayerObjectId());
			assertEquals("ASMODIANS", applicationRow.activePlayerRace());
			assertEquals(application.getLastUpdate(), applicationRow.serverEpochSeconds());
			assertEquals(4004, applicationRow.mutatedEntryObjectId());
			assertEquals(1400393, applicationRow.postedSystemMessageId());
			assertEquals(4, applicationRow.refreshedListAction());
			assertEquals(List.of(4004), applicationRow.visibleEntryObjectIdsAfterMutation());
			assertFalse(productionHookArtifactOutputEnabled());
			assertTrue(FindGroupMutationPostTraceCaptureHooks.traceRows().isEmpty());
		} finally {
			FindGroupMutationPostTraceCaptureHooks.clearInMemoryTraceRows();
			if (original == null)
				System.clearProperty(captureFlag);
			else
				System.setProperty(captureFlag, original);
			if (originalTimestamp == null)
				System.clearProperty(timestampProperty);
			else
				System.setProperty(timestampProperty, originalTimestamp);
		}
	}

	@Test
	public void productionHooksCanUseDeterministicServerEpochSecondsOverrideForCaptureRows() throws Exception {
		String captureFlag = FindGroupMutationPostTraceCaptureHooks.CAPTURE_FLAG;
		String timestampProperty = FindGroupMutationPostTraceCaptureHooks.SERVER_EPOCH_SECONDS_PROPERTY;
		String original = System.getProperty(captureFlag);
		String originalTimestamp = System.getProperty(timestampProperty);
		try {
			System.setProperty(captureFlag, "true");
			System.setProperty(timestampProperty, "1700000000");
			FindGroupMutationPostTraceCaptureHooks.clearInMemoryTraceRows();
			captureRecruitmentAndApplicationRows();

			List<FindGroupMutationPostTraceCaptureHooks.TraceRow> rows = FindGroupMutationPostTraceCaptureHooks.drainTraceRows();

			assertEquals(2, rows.size());
			assertEquals(1700000000, rows.get(0).serverEpochSeconds());
			assertEquals(1700000000, rows.get(1).serverEpochSeconds());
			assertFalse(productionHookArtifactOutputEnabled());
			assertFalse(runtimeInstrumentationImplemented());
		} finally {
			FindGroupMutationPostTraceCaptureHooks.clearInMemoryTraceRows();
			if (original == null)
				System.clearProperty(captureFlag);
			else
				System.setProperty(captureFlag, original);
			if (originalTimestamp == null)
				System.clearProperty(timestampProperty);
			else
				System.setProperty(timestampProperty, originalTimestamp);
		}
	}

	@Test
	public void deterministicServerEpochSecondsOverrideRejectsInvalidValues() {
		String timestampProperty = FindGroupMutationPostTraceCaptureHooks.SERVER_EPOCH_SECONDS_PROPERTY;
		String originalTimestamp = System.getProperty(timestampProperty);
		try {
			System.setProperty(timestampProperty, "not-an-int");

			assertThrows(NumberFormatException.class, () -> FindGroupMutationPostTraceCaptureHooks.serverEpochSeconds(123));
		} finally {
			if (originalTimestamp == null)
				System.clearProperty(timestampProperty);
			else
				System.setProperty(timestampProperty, originalTimestamp);
		}
	}

	@Test
	public void inMemoryArtifactBridgeDoesNotWriteWhenCaptureFlagIsDisabled(@TempDir Path tempDirectory) throws Exception {
		String captureFlag = FindGroupMutationPostTraceCaptureHooks.CAPTURE_FLAG;
		String original = System.getProperty(captureFlag);
		try {
			System.clearProperty(captureFlag);
			FindGroupMutationPostTraceCaptureHooks.clearInMemoryTraceRows();

			List<Path> writtenPaths = FindGroupMutationPostTraceCaptureInMemoryArtifactBridge.writeDrainedRows(tempDirectory);

			assertTrue(writtenPaths.isEmpty());
			assertFalse(Files.exists(FindGroupMutationPostTraceCaptureArtifactWriter.artifactPathForAction(tempDirectory, 2)));
			assertFalse(Files.exists(FindGroupMutationPostTraceCaptureArtifactWriter.artifactPathForAction(tempDirectory, 6)));
			assertTrue(FindGroupMutationPostTraceCaptureHooks.traceRows().isEmpty());
		} finally {
			FindGroupMutationPostTraceCaptureHooks.clearInMemoryTraceRows();
			if (original == null)
				System.clearProperty(captureFlag);
			else
				System.setProperty(captureFlag, original);
		}
	}

	@Test
	public void inMemoryArtifactBridgeWritesExplicitRootArtifactsFromDrainedRows(@TempDir Path tempDirectory) throws Exception {
		String captureFlag = FindGroupMutationPostTraceCaptureHooks.CAPTURE_FLAG;
		String original = System.getProperty(captureFlag);
		try {
			System.setProperty(captureFlag, "true");
			FindGroupMutationPostTraceCaptureHooks.clearInMemoryTraceRows();
			captureRecruitmentAndApplicationRows();

			List<Path> writtenPaths = FindGroupMutationPostTraceCaptureInMemoryArtifactBridge.writeDrainedRows(tempDirectory);
			FindGroupMutationPostTraceCaptureArtifactValidator.ValidationReport report =
				FindGroupMutationPostTraceCaptureArtifactValidator.validateExpectedArtifacts(tempDirectory);

			assertEquals(List.of(
				FindGroupMutationPostTraceCaptureArtifactWriter.artifactPathForAction(tempDirectory, 2),
				FindGroupMutationPostTraceCaptureArtifactWriter.artifactPathForAction(tempDirectory, 6)), writtenPaths);
			assertEquals(FindGroupMutationPostTraceCaptureArtifactValidator.DirectoryStatus.ALL_EXPECTED_ARTIFACTS_SHAPE_VALID, report.status());
			assertTrue(report.hasAllExpectedFiles());
			assertTrue(report.hasOnlyShapeValidArtifacts());
			assertFalse(report.readyForRuntimeComparison());
			assertTrue(FindGroupMutationPostTraceCaptureHooks.traceRows().isEmpty());
		} finally {
			FindGroupMutationPostTraceCaptureHooks.clearInMemoryTraceRows();
			if (original == null)
				System.clearProperty(captureFlag);
			else
				System.setProperty(captureFlag, original);
		}
	}

	@Test
	public void guardedArtifactRootPropertyDoesNotWriteWhenPropertyIsMissing() throws Exception {
		String captureFlag = FindGroupMutationPostTraceCaptureHooks.CAPTURE_FLAG;
		String artifactRootProperty = FindGroupMutationPostTraceCaptureInMemoryArtifactBridge.ARTIFACT_ROOT_PROPERTY;
		String originalCaptureFlag = System.getProperty(captureFlag);
		String originalArtifactRoot = System.getProperty(artifactRootProperty);
		try {
			System.setProperty(captureFlag, "true");
			System.clearProperty(artifactRootProperty);
			FindGroupMutationPostTraceCaptureHooks.clearInMemoryTraceRows();
			captureRecruitmentAndApplicationRows();

			List<Path> writtenPaths = FindGroupMutationPostTraceCaptureInMemoryArtifactBridge.tryWriteDrainedRowsFromArtifactRootProperty();

			assertTrue(writtenPaths.isEmpty());
			assertEquals(2, FindGroupMutationPostTraceCaptureHooks.traceRows().size());
			assertFalse(productionHookArtifactOutputEnabled());
		} finally {
			FindGroupMutationPostTraceCaptureHooks.clearInMemoryTraceRows();
			if (originalCaptureFlag == null)
				System.clearProperty(captureFlag);
			else
				System.setProperty(captureFlag, originalCaptureFlag);
			if (originalArtifactRoot == null)
				System.clearProperty(artifactRootProperty);
			else
				System.setProperty(artifactRootProperty, originalArtifactRoot);
		}
	}

	@Test
	public void guardedArtifactRootPropertyWritesOnlyToSuppliedRoot(@TempDir Path tempDirectory) throws Exception {
		String captureFlag = FindGroupMutationPostTraceCaptureHooks.CAPTURE_FLAG;
		String artifactRootProperty = FindGroupMutationPostTraceCaptureInMemoryArtifactBridge.ARTIFACT_ROOT_PROPERTY;
		String originalCaptureFlag = System.getProperty(captureFlag);
		String originalArtifactRoot = System.getProperty(artifactRootProperty);
		try {
			System.setProperty(captureFlag, "true");
			System.setProperty(artifactRootProperty, tempDirectory.toString());
			FindGroupMutationPostTraceCaptureHooks.clearInMemoryTraceRows();
			captureRecruitmentAndApplicationRows();

			List<Path> writtenPaths = FindGroupMutationPostTraceCaptureInMemoryArtifactBridge.tryWriteDrainedRowsFromArtifactRootProperty();
			FindGroupMutationPostTraceCaptureArtifactValidator.ValidationReport report =
				FindGroupMutationPostTraceCaptureArtifactValidator.validateExpectedArtifacts(tempDirectory);

			assertEquals(2, writtenPaths.size());
			assertTrue(writtenPaths.stream().allMatch(path -> path.startsWith(tempDirectory)));
			assertEquals(FindGroupMutationPostTraceCaptureArtifactValidator.DirectoryStatus.ALL_EXPECTED_ARTIFACTS_SHAPE_VALID, report.status());
			assertFalse(report.readyForRuntimeComparison());
			assertTrue(FindGroupMutationPostTraceCaptureHooks.traceRows().isEmpty());
		} finally {
			FindGroupMutationPostTraceCaptureHooks.clearInMemoryTraceRows();
			if (originalCaptureFlag == null)
				System.clearProperty(captureFlag);
			else
				System.setProperty(captureFlag, originalCaptureFlag);
			if (originalArtifactRoot == null)
				System.clearProperty(artifactRootProperty);
			else
				System.setProperty(artifactRootProperty, originalArtifactRoot);
		}
	}

	@Test
	public void commandSuppliedArtifactRootPropertyWritesGuardedArtifacts() throws Exception {
		String captureFlag = FindGroupMutationPostTraceCaptureHooks.CAPTURE_FLAG;
		String artifactRootProperty = FindGroupMutationPostTraceCaptureInMemoryArtifactBridge.ARTIFACT_ROOT_PROPERTY;
		String artifactRoot = System.getProperty(artifactRootProperty);
		assumeTrue(artifactRoot != null && !artifactRoot.isBlank(), "artifact root property not supplied");
		String originalCaptureFlag = System.getProperty(captureFlag);
		try {
			System.setProperty(captureFlag, "true");
			FindGroupMutationPostTraceCaptureHooks.clearInMemoryTraceRows();
			Path artifactRootPath = Path.of(artifactRoot);
			captureRecruitmentAndApplicationRows();

			List<Path> writtenPaths = FindGroupMutationPostTraceCaptureInMemoryArtifactBridge.tryWriteDrainedRowsFromArtifactRootProperty();
			FindGroupMutationPostTraceCaptureArtifactValidator.ValidationReport report =
				FindGroupMutationPostTraceCaptureArtifactValidator.validateExpectedArtifacts(artifactRootPath);

			assertEquals(List.of(
				FindGroupMutationPostTraceCaptureArtifactWriter.artifactPathForAction(artifactRootPath, 2),
				FindGroupMutationPostTraceCaptureArtifactWriter.artifactPathForAction(artifactRootPath, 6)), writtenPaths);
			assertEquals(FindGroupMutationPostTraceCaptureArtifactValidator.DirectoryStatus.ALL_EXPECTED_ARTIFACTS_SHAPE_VALID, report.status());
			assertFalse(report.readyForRuntimeComparison());
			assertTrue(FindGroupMutationPostTraceCaptureHooks.traceRows().isEmpty());
		} finally {
			FindGroupMutationPostTraceCaptureHooks.clearInMemoryTraceRows();
			if (originalCaptureFlag == null)
				System.clearProperty(captureFlag);
			else
				System.setProperty(captureFlag, originalCaptureFlag);
		}
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
	public void scenarioBuildersExposeDeterministicJavaPayloadAndMutationRows() {
		assertEquals(2, RECRUITMENT_SCENARIO.action());
		assertEquals(2002, RECRUITMENT_SCENARIO.playerOrTeamId());
		assertEquals("Recruitment fixture message", RECRUITMENT_SCENARIO.message());
		assertEquals(3, RECRUITMENT_SCENARIO.groupType());
		assertEquals(0, RECRUITMENT_SCENARIO.classId());
		assertEquals(0, RECRUITMENT_SCENARIO.level());
		assertEquals("Recruitment", RECRUITMENT_SCENARIO.mutationKind());
		assertEquals(1400392, RECRUITMENT_SCENARIO.postedSystemMessageId());
		assertEquals(0, RECRUITMENT_SCENARIO.refreshedListAction());
		assertEquals("cm-find-group-direct-mutation-post-boundary-action-2-java.json", RECRUITMENT_SCENARIO.artifactFileName());
		assertTrue(RECRUITMENT_SCENARIO.javaSource().contains("addRecruitment"));

		assertEquals(6, APPLICATION_SCENARIO.action());
		assertEquals(4004, APPLICATION_SCENARIO.playerOrTeamId());
		assertEquals("Application fixture message", APPLICATION_SCENARIO.message());
		assertEquals(5, APPLICATION_SCENARIO.groupType());
		assertEquals(7, APPLICATION_SCENARIO.classId());
		assertEquals(45, APPLICATION_SCENARIO.level());
		assertEquals("Application", APPLICATION_SCENARIO.mutationKind());
		assertEquals(1400393, APPLICATION_SCENARIO.postedSystemMessageId());
		assertEquals(4, APPLICATION_SCENARIO.refreshedListAction());
		assertEquals("cm-find-group-direct-mutation-post-boundary-action-6-java.json", APPLICATION_SCENARIO.artifactFileName());
		assertTrue(APPLICATION_SCENARIO.javaSource().contains("addApplication"));
	}

	@Test
	public void serializerNoOpsWhenCaptureFlagIsDisabled() {
		String captureFlag = FindGroupMutationPostTraceCaptureInstrumentation.CAPTURE_FLAG;
		String original = System.getProperty(captureFlag);
		try {
			System.clearProperty(captureFlag);

			Optional<String> artifact = FindGroupMutationPostTraceCaptureSerializer.trySerializeArtifact(List.of(
				RECRUITMENT_SCENARIO.traceRow()));

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
				RECRUITMENT_SCENARIO.traceRowWithVisibleEntryObjectIds(List.of(2002, 3003))))
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
			APPLICATION_SCENARIO.traceRow()));

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
				List.of(RECRUITMENT_SCENARIO.traceRow()));

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
				List.of(RECRUITMENT_SCENARIO.traceRow()))
				.orElseThrow();
			Path actionSixPath = FindGroupMutationPostTraceCaptureArtifactWriter.tryWriteArtifact(
				tempDirectory,
				6,
				List.of(APPLICATION_SCENARIO.traceRow()))
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
				List.of(APPLICATION_SCENARIO.traceRow())));

		assertEquals("Artifact for action 2 must contain a matching mutation-post trace row.", exception.getMessage());
	}

	@Test
	public void artifactValidatorReportsMissingExpectedFiles(@TempDir Path tempDirectory) throws IOException {
		FindGroupMutationPostTraceCaptureArtifactValidator.ValidationReport report =
			FindGroupMutationPostTraceCaptureArtifactValidator.validateExpectedArtifacts(tempDirectory);

		assertEquals(FindGroupMutationPostTraceCaptureArtifactValidator.DirectoryStatus.MISSING_EXPECTED_FILES, report.status());
		assertFalse(report.hasAllExpectedFiles());
		assertFalse(report.hasOnlyShapeValidArtifacts());
		assertFalse(report.readyForRuntimeComparison());
		assertEquals(List.of(2, 6), report.files().stream().map(FindGroupMutationPostTraceCaptureArtifactValidator.FileValidation::action).toList());
		assertTrue(report.files().stream().allMatch(file ->
			file.status() == FindGroupMutationPostTraceCaptureArtifactValidator.FileStatus.MISSING_FILE));
	}

	@Test
	public void artifactValidatorAcceptsFixtureWriterShapeOnlyArtifacts(@TempDir Path tempDirectory) throws IOException {
		String captureFlag = FindGroupMutationPostTraceCaptureInstrumentation.CAPTURE_FLAG;
		String original = System.getProperty(captureFlag);
		try {
			System.setProperty(captureFlag, "true");
			writeActionTwoAndSixFixtureArtifacts(tempDirectory);

			FindGroupMutationPostTraceCaptureArtifactValidator.ValidationReport report =
				FindGroupMutationPostTraceCaptureArtifactValidator.validateExpectedArtifacts(tempDirectory);

			assertEquals(FindGroupMutationPostTraceCaptureArtifactValidator.DirectoryStatus.ALL_EXPECTED_ARTIFACTS_SHAPE_VALID, report.status());
			assertTrue(report.hasAllExpectedFiles());
			assertTrue(report.hasOnlyShapeValidArtifacts());
			assertFalse(report.readyForRuntimeComparison());
			assertTrue(report.files().stream().allMatch(file ->
				file.status() == FindGroupMutationPostTraceCaptureArtifactValidator.FileStatus.SHAPE_VALID
					&& file.notes().contains("runtime comparison remains blocked")));
			assertFalse(runtimeInstrumentationImplemented());
		} finally {
			if (original == null)
				System.clearProperty(captureFlag);
			else
				System.setProperty(captureFlag, original);
		}
	}

	@Test
	public void artifactValidatorRejectsFixtureFileWithWrongActionMapping(@TempDir Path tempDirectory) throws IOException {
		String captureFlag = FindGroupMutationPostTraceCaptureInstrumentation.CAPTURE_FLAG;
		String original = System.getProperty(captureFlag);
		try {
			System.setProperty(captureFlag, "true");
			writeActionTwoAndSixFixtureArtifacts(tempDirectory);
			Path actionSixPath = FindGroupMutationPostTraceCaptureArtifactWriter.artifactPathForAction(tempDirectory, 6);
			Files.writeString(actionSixPath, Files.readString(actionSixPath).replace("\"postedSystemMessageId\": 1400393", "\"postedSystemMessageId\": 1400392"));

			FindGroupMutationPostTraceCaptureArtifactValidator.ValidationReport report =
				FindGroupMutationPostTraceCaptureArtifactValidator.validateExpectedArtifacts(tempDirectory);

			assertEquals(FindGroupMutationPostTraceCaptureArtifactValidator.DirectoryStatus.INVALID_ARTIFACTS, report.status());
			assertTrue(report.hasAllExpectedFiles());
			assertFalse(report.hasOnlyShapeValidArtifacts());
			assertTrue(report.files().stream().anyMatch(file ->
				file.action() == 6
					&& file.status() == FindGroupMutationPostTraceCaptureArtifactValidator.FileStatus.INVALID_ARTIFACT
					&& file.notes().contains("\"postedSystemMessageId\": 1400393")));
			assertFalse(report.readyForRuntimeComparison());
		} finally {
			if (original == null)
				System.clearProperty(captureFlag);
			else
				System.setProperty(captureFlag, original);
		}
	}

	private static boolean captureEnabled() {
		return FindGroupMutationPostTraceCaptureInstrumentation.captureEnabled();
	}

	private static boolean runtimeInstrumentationImplemented() {
		return false;
	}

	private static boolean productionHookIntegrationImplemented() {
		return true;
	}

	private static boolean productionHookArtifactOutputEnabled() {
		return FindGroupMutationPostTraceCaptureHooks.artifactOutputEnabled();
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

	private static void writeActionTwoAndSixFixtureArtifacts(Path artifactRoot) throws IOException {
		FindGroupMutationPostTraceCaptureArtifactWriter.tryWriteArtifact(
			artifactRoot,
			2,
			List.of(RECRUITMENT_SCENARIO.traceRow()))
			.orElseThrow();
		FindGroupMutationPostTraceCaptureArtifactWriter.tryWriteArtifact(
			artifactRoot,
			6,
			List.of(APPLICATION_SCENARIO.traceRow()))
			.orElseThrow();
	}

	private static void captureRecruitmentAndApplicationRows() throws Exception {
		Player recruiter = simplePlayer(2002, "Recruiter", Race.ELYOS);
		GroupRecruitment recruitment = new GroupRecruitment(recruiter, "Recruit", 3);
		GroupRecruitment visibleRecruitment = new GroupRecruitment(simplePlayer(3003, "VisibleRecruit", Race.ELYOS), "Other", 4);
		Player applicant = simplePlayer(4004, "Applicant", Race.ASMODIANS);
		GroupApplication application = new GroupApplication(applicant, "Apply", 5, 7, 45);

		FindGroupMutationPostTraceCaptureHooks.recordRecruitmentStateMutation(recruiter, recruitment);
		FindGroupMutationPostTraceCaptureHooks.recordRecruitmentPostedMessageSend(recruiter);
		FindGroupMutationPostTraceCaptureHooks.recordRecruitmentRefreshedListSend(recruiter, List.of(recruitment, visibleRecruitment));
		FindGroupMutationPostTraceCaptureHooks.recordApplicationStateMutation(applicant, application);
		FindGroupMutationPostTraceCaptureHooks.recordApplicationPostedMessageSend(applicant);
		FindGroupMutationPostTraceCaptureHooks.recordApplicationRefreshedListSend(applicant, List.of(application));
	}

	private static Player simplePlayer(int objectId, String name, Race race) throws Exception {
		PlayerCommonData commonData = new PlayerCommonData(objectId);
		commonData.setName(name);
		commonData.setRace(race);
		commonData.setPlayerClass(race == Race.ELYOS ? PlayerClass.GLADIATOR : PlayerClass.ASSASSIN);
		PlayerAppearance appearance = new PlayerAppearance();
		appearance.setHeight(1);
		PlayerAccountData accountData = new PlayerAccountData(commonData, appearance);
		Account account = new Account(1);

		Player player = (Player) unsafe().allocateInstance(Player.class);
		setAionObjectId(player, objectId);
		setField(player, "playerAccountData", accountData);
		setField(player, "playerAccount", account);
		setField(player, "position", new WorldPosition(300110000));
		return player;
	}

	private static void setAionObjectId(AionObject object, int objectId) throws Exception {
		Field field = AionObject.class.getDeclaredField("objectId");
		unsafe().putInt(object, unsafe().objectFieldOffset(field), objectId);
	}

	private static Unsafe unsafe() throws Exception {
		Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
		unsafeField.setAccessible(true);
		return (Unsafe) unsafeField.get(null);
	}

	private static void setField(Object target, String name, Object value) throws Exception {
		Field field = findField(target.getClass(), name);
		field.setAccessible(true);
		field.set(target, value);
	}

	private static Field findField(Class<?> type, String name) throws NoSuchFieldException {
		Class<?> current = type;
		while (current != null) {
			try {
				return current.getDeclaredField(name);
			} catch (NoSuchFieldException ignored) {
				current = current.getSuperclass();
			}
		}
		throw new NoSuchFieldException(name);
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
