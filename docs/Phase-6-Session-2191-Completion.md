# Phase 6 Session 2191 Completion - FindGroup Mutation Java Trace Capture Fixture Scaffold

Date: 2026-06-02
Unit of Work: UOW-2191
Status: Completed

## Scope

This unit added a Maven-runnable Java fixture scaffold for future `CM_FIND_GROUP` action `2` and `6` mutation-post artifact capture.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
- existing Java test layout under `game-server/test`
- existing Java `CM_FIND_GROUP_ReadPayloadGoldenTest`

This UOW does not instrument Java runtime behavior, does not write Java artifacts, does not serialize trace rows, does not compare Java/C# rows, and does not wire live C# `CmFindGroup` dispatch.

## Changes

- Added `FindGroupMutationPostTraceCaptureTest` under `game-server/test/com/aionemu/gameserver/services/findgroup`.
- The fixture scaffold records:
  - capture flag `aion.findGroupMutationPost.capture`,
  - trace name `cm-find-group-direct-mutation-post-boundary`,
  - artifact root `parity-artifacts/find-group/mutation-post/java`,
  - action `2` recruitment scenario metadata,
  - action `6` application scenario metadata,
  - stable action-specific artifact file names.
- The fixture is runnable with the capture flag enabled, but reports Java instrumentation and artifact writing as not implemented.
- Updated C# runbook/readiness metadata to use the actual Maven test path `game-server/test/...` instead of `game-server/src/test/java/...`.
- Updated the planned focused Maven command to include `-Dmaven.test.skip=false`, because the parent POM defaults `maven.test.skip` to `true`.
- Advanced C# readiness status from missing fixture to missing Java instrumentation while keeping runtime comparison blocked.
- Updated the trace-row readiness aggregate to report the Java-capture row as blocked on missing Java instrumentation.
- Updated live-dispatch design notes to reflect that the fixture scaffold exists but runtime-producing capture remains blocked.
- Left `docs/PHASE-6-PROGRESS.md` untouched.

## Validation

Validation decision:

- Changed surface: focused Java test fixture scaffold, focused C# non-live readiness metadata/tests, and non-live design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostJavaArtifactCaptureImplementationReadinessServiceTests|FullyQualifiedName~FindGroupMutationPostJavaArtifactCaptureRunbookServiceTests|FullyQualifiedName~FindGroupMutationPostTraceRowReadinessAggregateServiceTests|FullyQualifiedName~FindGroupMutationPostJavaInstrumentationDesignReportServiceTests|FullyQualifiedName~FindGroupMutationPostJavaTraceArtifactSchemaReportServiceTests|FullyQualifiedName~FindGroupMutationPostJavaTraceArtifactValidatorServiceTests" --no-restore
```

- Focused Java/Maven command:

```powershell
mvn -pl game-server -am test "-Dtest=FindGroupMutationPostTraceCaptureTest" "-Daion.findGroupMutationPost.capture=true" "-Dmaven.test.skip=false" "-Dsurefire.failIfNoSpecifiedTests=false"
```

- Broad-validation trigger: none. No live connection dispatch, live side effects, packet primitive, common runtime base, persistence, shared infrastructure, production Java source, or broad behavior surface changed.
- Broad .NET decision: skipped intentionally. The filtered `dotnet test` command built the affected C# project and dependencies.
- Why this scope is sufficient: the Java command proves the new scaffold is discoverable and runnable through the real Maven test path with the capture flag enabled; the C# filter covers the updated runbook/readiness/aggregate metadata and immediate schema/validator neighbors.

Results:

- Java/Maven: passed 4, failed 0, skipped 0.
- C#: passed 35, failed 0, skipped 0.
- Existing unrelated Java `Unsafe`/deprecation warnings and C# nullable/analyzer warnings were emitted.

Validation correction:

- The first attempted Java run failed because `captureFlagDefaultsToDisabled` asserted the flag was false while the focused command intentionally passed `-Daion.findGroupMutationPost.capture=true`.
- The test was corrected to clear and restore the system property for the default-flag assertion, then the same focused Java command passed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureTest.java`; `Aion.GameServer.Services.FindGroupMutationPostJavaArtifactCaptureRunbookService`; `Aion.GameServer.Services.FindGroupMutationPostJavaArtifactCaptureImplementationReadinessService` | Java Fixture Scaffold / Readiness Metadata | Partial | Unit Tested | Partial Parity | Java packet source for actions `2` and `6` was reviewed and the fixture scaffold now names action scenarios and artifact targets, but it does not execute `readImpl/runImpl`, capture runtime rows, or prove Java/C# parity. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureTest.java`; `Aion.GameServer.Services.FindGroupMutationPostTraceRowReadinessAggregateService` | Mutation-Post Java Capture Scaffold | Partial | Unit Tested | Partial Parity | The scaffold preserves Java-derived `addRecruitment`/`addApplication` mappings for posted system message ids `1400392`/`1400393` and refreshed list actions `0`/`4`, but Java instrumentation, serializer, generated artifacts, live C# rows, registry observation, and comparison execution are missing. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupMutationPostTraceCaptureTest.captureFlagDefaultsToDisabled` | Unit | Java capture runbook and Maven property behavior | Capture flag defaults to disabled when the property is absent and runtime capture remains unimplemented. | Focused Java fixture assertion. | Does not run packet/service behavior. |
| `FindGroupMutationPostTraceCaptureTest.fixtureListsActionTwoAndSixMutationPostScenarios` | Unit | `CM_FIND_GROUP.readImpl/runImpl`; `FindGroupService.addRecruitment/addApplication` review | Fixture scenario metadata for action `2` and `6` uses the Java mutation kind, service source, posted message id, and refreshed list action. | Focused Java source-derived metadata assertion. | No runtime capture or serializer. |
| `FindGroupMutationPostTraceCaptureTest.fixtureNamesStableArtifactTargetsWithoutWritingThem` | Unit | Java artifact file target report | Trace name, artifact root, and expected action `2`/`6` artifact names are stable. | Focused artifact-target assertion. | No files written. |
| `FindGroupMutationPostTraceCaptureTest.captureFlagCanBeEnabledButRuntimeCaptureRemainsBlocked` | Unit | Capture-gated fixture design | The fixture is runnable with the capture flag enabled while instrumentation and artifact writing remain blocked. | Focused Maven-runnable scaffold assertion. | No generated Java artifacts. |
| `FindGroupMutationPostJavaArtifactCaptureRunbookServiceTests` | Unit | Java fixture scaffold and runbook metadata | Runbook now names actual `game-server/test` path and focused Maven skip override. | Focused C# metadata assertion. | Metadata only. |
| `FindGroupMutationPostJavaArtifactCaptureImplementationReadinessServiceTests` | Unit | Java fixture scaffold and readiness metadata | Readiness status advances to missing Java instrumentation and names the Maven-runnable scaffold. | Focused C# readiness assertion. | Metadata only. |
| `FindGroupMutationPostTraceRowReadinessAggregateServiceTests` | Unit | Java capture readiness chain | Aggregate reports Java capture blocked on missing instrumentation. | Focused aggregate assertion. | No Java/C# row comparison. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 2 production artifacts and 1 Java fixture scaffold
- Total artifacts ported in this UOW: 0
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 2 production artifacts plus the fixture scaffold
- Total blocked artifacts: 1 live `CM_FIND_GROUP` boundary and 1 Java artifact capture/comparison chain
- Estimated overall migration completion: unchanged; Phase 6 remains in progress.

## Remaining Risks

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- Java instrumentation, Java serializer, generated Java artifacts, C# live fixture, live emitter, registry observation, and deterministic comparison are missing.
- The Java fixture scaffold is not runtime parity evidence; it only proves the Maven-runnable capture-gated scaffold and scenario metadata.
- World-broadcast fanout, action `12` invite dispatch, shared singleton interleavings, and runtime/socket comparison remain non-live or missing.

## Next Recommended Unit of Work

Next sequential task:

- Add Java mutation-post trace instrumentation hook design-to-code scaffolding for `CM_FIND_GROUP`/`FindGroupService` behind the capture flag, still without changing send ordering or writing artifact files.

Safe candidates:

- Add a Java trace serializer design-to-code checklist before writing serializer code.
- Add Java fixture helper methods for deterministic payload/scenario construction while keeping runtime execution disabled.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.

## Files Changed

- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureTest.java`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostJavaArtifactCaptureRunbookService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostJavaArtifactCaptureImplementationReadinessService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostTraceRowReadinessAggregateService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostJavaArtifactCaptureRunbookServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostJavaArtifactCaptureImplementationReadinessServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostTraceRowReadinessAggregateServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2191-Completion.md`
- `docs/Phase-6-Session-2191-Handoff.md`
