# Phase 6 Session 2182 Completion - FindGroup Mutation Java Artifact Capture Runbook

Date: 2026-06-02
Unit of Work: UOW-2182
Status: Completed

## Scope

This unit added non-live Java artifact capture runbook metadata for future `CM_FIND_GROUP` action `2` and `6` mutation-post trace artifacts.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

This UOW does not change Java source, does not create the Java fixture, does not run Maven, does not generate artifacts, and does not execute runtime comparison.

## Changes

- Added `FindGroupMutationPostJavaArtifactCaptureRunbookService`.
- The runbook records:
  - planned Java fixture class `FindGroupMutationPostTraceCaptureTest`,
  - capture flag `aion.findGroupMutationPost.capture`,
  - Java payload/runImpl/mutation hook targets,
  - action `2` recruitment hooks for message id `1400392` and refreshed action `0`,
  - action `6` application hooks for message id `1400393` and refreshed action `4`,
  - trace serializer shape using all 22 mutation-post schema fields,
  - exact action `2`/`6` artifact paths,
  - focused Maven command to run only after the fixture exists,
  - validator and artifact comparison preflight flow.
- The focused Maven command is documented as design-only:

```powershell
mvn -pl game-server -am test "-Dtest=FindGroupMutationPostTraceCaptureTest" "-Daion.findGroupMutationPost.capture=true" "-Dsurefire.failIfNoSpecifiedTests=false"
```

- Added focused tests for blocked/non-live status, stable runbook step order, action-specific hooks, artifact paths, serializer/validator flow, design-only Maven command, and comparison-preflight ending state.
- Updated live-dispatch design notes to include the capture runbook and blockers.
- Left `docs/PHASE-6-PROGRESS.md` untouched.

## Validation

Validation decision:

- Changed surface: focused non-live Java artifact capture runbook metadata service, focused tests, and non-live design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostJavaArtifactCaptureRunbookServiceTests|FullyQualifiedName~FindGroupMutationPostJavaInstrumentationDesignReportServiceTests|FullyQualifiedName~FindGroupMutationPostJavaTraceArtifactSchemaReportServiceTests|FullyQualifiedName~FindGroupMutationPostJavaTraceArtifactFileReportServiceTests|FullyQualifiedName~FindGroupMutationPostArtifactComparisonPreflightServiceTests" --no-restore
```

- Focused Java/Maven command: not run. The runbook names the planned Maven command, but the Java fixture, Java instrumentation, trace serializer, and generated artifacts do not exist yet.
- Broad-validation trigger: none. No live connection dispatch, live side effects, packet primitive, common runtime base, persistence, shared infrastructure, Java source, or broad behavior surface changed.
- Broad .NET decision: skipped intentionally.
- Why this scope is sufficient: the filtered tests cover the new runbook metadata and the immediate instrumentation, schema, artifact target, and preflight dependencies; the filtered command builds the affected project/dependencies.

Result:

- Passed: 25
- Failed: 0
- Skipped: 0
- Existing unrelated nullable/analyzer warnings were emitted.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Services.FindGroupMutationPostJavaArtifactCaptureRunbookService` | Java Artifact Capture Runbook | Blocked | Unit Tested | Partial Parity | The planned action `2`/`6` capture fixture and Maven command are named, but no Java fixture, instrumentation, serializer, generated artifacts, or runtime comparison exists. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `Aion.GameServer.Services.FindGroupMutationPostJavaArtifactCaptureRunbookService`; `Aion.GameServer.Services.FindGroupMutationPostJavaInstrumentationDesignReportService`; `Aion.GameServer.Services.FindGroupMutationPostJavaTraceArtifactFileReportService` | Java Artifact Capture Runbook / Instrumentation Plan | Partial | Unit Tested | Partial Parity | The runbook preserves Java mutation-before-posted-message-before-refreshed-list ordering and action-specific packet ids, but it is design metadata only. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupMutationPostJavaArtifactCaptureRunbookServiceTests.Create_KeepsRunbookBlockedAndNonLive` | Unit | Java fixture/runbook blockers | Runbook remains blocked and non-live. | Focused metadata assertion. | No Java fixture. |
| `FindGroupMutationPostJavaArtifactCaptureRunbookServiceTests.Create_ListsStableStepOrderAndJavaSources` | Unit | Java `CM_FIND_GROUP.readImpl/runImpl` | Stable runbook steps and Java sources are listed. | Focused runbook assertion. | No Java code changed. |
| `FindGroupMutationPostJavaArtifactCaptureRunbookServiceTests.Create_RecordsActionTwoAndSixMutationHooks` | Unit | Java `FindGroupService.addRecruitment`; `FindGroupService.addApplication` | Action-specific mutation hooks preserve message ids and refreshed list actions. | Focused Java-derived hook assertion. | No instrumentation. |
| `FindGroupMutationPostJavaArtifactCaptureRunbookServiceTests.Create_DefinesArtifactPathsSerializerAndValidatorFlow` | Unit | Trace schema and artifact target reports | Exact artifact paths plus serializer/validator flow are recorded. | Focused runbook assertion. | No generated files. |
| `FindGroupMutationPostJavaArtifactCaptureRunbookServiceTests.Create_NamesFocusedMavenCommandButMarksItDesignOnly` | Unit | Planned Java fixture command | Maven command is named and marked design-only. | Focused command metadata assertion. | Command not run because fixture missing. |
| `FindGroupMutationPostJavaArtifactCaptureRunbookServiceTests.Create_EndsWithComparisonPreflightWithoutClaimingParity` | Unit | Artifact comparison preflight contract | Runbook feeds preflight and does not claim readiness. | Focused runbook assertion. | No comparison executed. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 2
- Total artifacts ported in this UOW: 0
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 2
- Total blocked artifacts: 1 live `CM_FIND_GROUP` boundary and 1 Java artifact capture/comparison gap
- Estimated overall migration completion: unchanged; Phase 6 remains in progress.

## Remaining Risks

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- The Java fixture, Java instrumentation, Java serializer, generated Java artifacts, C# live runtime rows, registry-send observation, encrypted socket capture, and deterministic comparison are still missing.
- The capture runbook is non-live metadata and cannot prove Java/C# parity.
- World-broadcast fanout, action `12` invite dispatch, shared singleton interleavings, and runtime/socket comparison remain non-live or missing.

## Next Recommended Unit of Work

Next sequential task:

- Add a C# live trace-row fixture plan for action `2`/`6` mutation-post traces, naming how future live `CmFindGroup` rows will be captured without enabling dispatch prematurely.

Safe candidates:

- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Implement the targeted Java/Maven fixture only when ready to edit Java source and capture artifacts.
- Add a live direct-packet boundary test or trace for action `0`/`4` before mutation actions if a lower-risk live observation path is preferred.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostJavaArtifactCaptureRunbookService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostJavaArtifactCaptureRunbookServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2182-Completion.md`
- `docs/Phase-6-Session-2182-Handoff.md`
