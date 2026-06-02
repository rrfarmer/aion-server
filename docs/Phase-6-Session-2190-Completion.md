# Phase 6 Session 2190 Completion - FindGroup Mutation Java Artifact Capture Implementation Readiness

Date: 2026-06-02
Unit of Work: UOW-2190
Status: Completed

## Scope

This unit added a non-live implementation-readiness checklist for future Java `CM_FIND_GROUP` action `2` and `6` mutation-post artifact capture.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

This UOW does not edit Java source, does not implement the Java fixture, does not instrument Java runtime behavior, does not create a serializer, does not generate artifacts, does not run comparison, and does not wire live C# `CmFindGroup` dispatch.

## Changes

- Added `FindGroupMutationPostJavaArtifactCaptureImplementationReadinessService`.
- The checklist consumes the existing Java capture runbook, instrumentation design, schema report, and artifact file report.
- The checklist names concrete future Java implementation tasks:
  - fixture class,
  - fixture scenarios,
  - instrumentation hooks,
  - trace serializer,
  - artifact files,
  - artifact validation,
  - focused Maven command,
  - comparison handoff.
- The default status remains blocked on the missing Java fixture and runtime-producing Java work.
- Added focused tests for default blocked state, stable task order, fixture/scenario rows, instrumentation hook coverage, serializer/artifact targets, validator/Maven design-only state, and comparison handoff caution.
- Updated live-dispatch design notes to include the readiness checklist and remaining blocker.
- Left `docs/PHASE-6-PROGRESS.md` untouched.

## Validation

Validation decision:

- Changed surface: focused non-live Java artifact capture implementation-readiness service, focused tests, and non-live design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostJavaArtifactCaptureImplementationReadinessServiceTests|FullyQualifiedName~FindGroupMutationPostJavaArtifactCaptureRunbookServiceTests|FullyQualifiedName~FindGroupMutationPostJavaInstrumentationDesignReportServiceTests|FullyQualifiedName~FindGroupMutationPostJavaTraceArtifactSchemaReportServiceTests|FullyQualifiedName~FindGroupMutationPostJavaTraceArtifactValidatorServiceTests" --no-restore
```

- Focused Java/Maven command: not run. No Java source changed, the planned `FindGroupMutationPostTraceCaptureTest` fixture does not exist, and there is no Java serializer or generated artifact to validate.
- Broad-validation trigger: none. No live connection dispatch, live side effects, packet primitive, common runtime base, persistence, shared infrastructure, Java source, or broad behavior surface changed.
- Broad .NET decision: skipped intentionally. The filtered `dotnet test` command built the affected project and dependencies.
- Why this scope is sufficient: the filtered tests cover the new checklist and its immediate Java capture runbook, instrumentation design, schema, and validator dependencies without spending time on unrelated suites.

Result:

- Passed: 29
- Failed: 0
- Skipped: 0
- Existing unrelated nullable/analyzer warnings were emitted.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Services.FindGroupMutationPostJavaArtifactCaptureImplementationReadinessService` | Java Artifact Capture Implementation Readiness Checklist | Blocked | Unit Tested | Partial Parity | The checklist names future action `2`/`6` fixture and instrumentation tasks from reviewed Java packet behavior, but Java fixture, instrumentation, serializer, generated artifacts, live C# rows, and comparison execution are missing. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `Aion.GameServer.Services.FindGroupMutationPostJavaArtifactCaptureImplementationReadinessService`; `Aion.GameServer.Services.FindGroupMutationPostJavaArtifactCaptureRunbookService`; `Aion.GameServer.Services.FindGroupMutationPostJavaInstrumentationDesignReportService`; `Aion.GameServer.Services.FindGroupMutationPostJavaTraceArtifactValidatorService` | Mutation-Post Java Artifact Capture Readiness | Partial | Unit Tested | Partial Parity | The checklist preserves Java-derived mutation-before-posted-message-before-refreshed-list ordering requirements for `addRecruitment` and `addApplication`, but it is non-live metadata only and proves no runtime parity. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupMutationPostJavaArtifactCaptureImplementationReadinessServiceTests.Create_DefaultReadinessIsBlockedAndNonLive` | Unit | Java source review and current missing fixture/instrumentation | Default checklist is blocked, non-live, and requires Java fixture/instrumentation/serializer/artifacts. | Focused readiness assertion. | No Java runtime artifact capture. |
| `FindGroupMutationPostJavaArtifactCaptureImplementationReadinessServiceTests.Create_ListsStableImplementationTaskRows` | Unit | Runbook and implementation checklist design | Checklist rows are stable and cover the expected implementation tasks. | Focused task-order assertion. | Does not implement tasks. |
| `FindGroupMutationPostJavaArtifactCaptureImplementationReadinessServiceTests.Create_FixtureRowsNameFutureJavaTestAndActionScenarios` | Unit | `CM_FIND_GROUP.readImpl` action `2`/`6` review | Future fixture class and action scenarios are named. | Focused fixture/scenario assertion. | Fixture class does not exist. |
| `FindGroupMutationPostJavaArtifactCaptureImplementationReadinessServiceTests.Create_InstrumentationRowNamesJavaHookPointsAndOrderingGuard` | Unit | `CM_FIND_GROUP.runImpl`; `FindGroupService.addRecruitment/addApplication/showRecruitments/showApplications` review | Hook points and Java ordering guard are named. | Focused instrumentation assertion. | No Java hooks implemented. |
| `FindGroupMutationPostJavaArtifactCaptureImplementationReadinessServiceTests.Create_SerializerAndArtifactRowsUseSchemaAndStablePaths` | Unit | Mutation-post schema/file target reports | Serializer shape and expected action `2`/`6` artifact paths are named. | Focused schema/path assertion. | No serializer or artifact files. |
| `FindGroupMutationPostJavaArtifactCaptureImplementationReadinessServiceTests.Create_ValidationAndMavenRowsStayDesignOnlyUntilFixtureExists` | Unit | Validator service and runbook command | Validator and Maven rows remain design-only until the Java fixture exists. | Focused validator/command assertion. | Maven command not runnable evidence yet. |
| `FindGroupMutationPostJavaArtifactCaptureImplementationReadinessServiceTests.Create_ComparisonHandOffRefusesVerifiedParityClaim` | Unit | Comparison preflight blockers | Checklist hands off to comparison preflight and refuses verified parity claims. | Focused comparison-handoff assertion. | No Java/C# row comparison. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 2
- Total artifacts ported in this UOW: 0
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 2
- Total blocked artifacts: 1 live `CM_FIND_GROUP` boundary and 1 Java artifact capture/comparison chain
- Estimated overall migration completion: unchanged; Phase 6 remains in progress.

## Remaining Risks

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- Java fixture, Java instrumentation, Java serializer, generated Java artifacts, C# live fixture, live emitter, registry observation, and deterministic comparison are missing.
- The implementation-readiness checklist is non-live metadata and cannot prove Java/C# runtime parity.
- World-broadcast fanout, action `12` invite dispatch, shared singleton interleavings, and runtime/socket comparison remain non-live or missing.

## Next Recommended Unit of Work

Next sequential task:

- Implement the targeted Java fixture skeleton for `FindGroupMutationPostTraceCaptureTest` behind the `aion.findGroupMutationPost.capture` flag, without changing gameplay behavior or generating artifacts until the fixture/hook strategy is reviewed.

Safe candidates:

- Add a Java trace serializer design-to-code checklist if the team wants one more non-live guard before editing Java source.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Add a live direct-packet boundary test or trace for action `0`/`4` before mutation actions if a lower-risk live observation path is preferred.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostJavaArtifactCaptureImplementationReadinessService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostJavaArtifactCaptureImplementationReadinessServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2190-Completion.md`
- `docs/Phase-6-Session-2190-Handoff.md`
