# Phase 6 Session 2183 Completion - FindGroup Mutation CSharp Live Trace Row Fixture Plan

Date: 2026-06-02
Unit of Work: UOW-2183
Status: Completed

## Scope

This unit added a non-live C# live trace-row fixture plan for future `CM_FIND_GROUP` action `2` and `6` mutation-post traces.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

This UOW does not wire live `CmFindGroup` dispatch, does not emit runtime rows, does not observe registry sends, does not generate Java artifacts, and does not execute runtime comparison.

## Changes

- Added `FindGroupMutationPostCSharpLiveTraceRowFixturePlanService`.
- The plan records:
  - planned fixture class `GameServerConnectionFindGroupMutationPostLiveTraceRowFixture`,
  - explicit live-dispatch guard preserving disabled production `CmFindGroup` dispatch,
  - boundary acceptance trace fields,
  - shared singleton mutation trace fields,
  - direct packet intent trace fields,
  - boundary executor evidence,
  - registry-send observation evidence,
  - runtime row serialization fields,
  - artifact comparison preflight input.
- Preserved Java action-specific evidence:
  - action `2`: recruitment state, `SmSystemMessage` id `1400392`, refreshed `SmFindGroup` action `0`.
  - action `6`: application state, `SmSystemMessage` id `1400393`, refreshed `SmFindGroup` action `4`.
- Kept the plan blocked until live boundary fixture, live emitter, registry observation, generated Java artifacts, and comparison execution exist.
- Added focused tests for blocked/non-live status, action coverage, dispatch guard, real-boundary acceptance, Java-specific mutation/direct packet rows, executor/registry ordering, and comparison-preflight ending state.
- Updated live-dispatch design notes to include the C# live trace-row fixture plan and blockers.
- Left `docs/PHASE-6-PROGRESS.md` untouched.

## Validation

Validation decision:

- Changed surface: focused non-live C# live trace-row fixture plan service, focused tests, and non-live design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostCSharpLiveTraceRowFixturePlanServiceTests|FullyQualifiedName~FindGroupMutationPostCSharpTraceEmitterDesignReportServiceTests|FullyQualifiedName~FindGroupMutationPostRegistryObservationTraceContractServiceTests|FullyQualifiedName~FindGroupDirectPacketMutationPostBoundaryTraceSchemaServiceTests|FullyQualifiedName~FindGroupMutationPostArtifactComparisonPreflightServiceTests" --no-restore
```

- Focused Java/Maven command: not run. No Java source changed, no Java fixture/instrumentation/serializer exists, and no generated Java artifacts exist.
- Broad-validation trigger: none. No live connection dispatch, live side effects, packet primitive, common runtime base, persistence, shared infrastructure, Java source, or broad behavior surface changed.
- Broad .NET decision: skipped intentionally.
- Why this scope is sufficient: the filtered tests cover the new fixture plan and the immediate C# emitter design, registry contract, trace schema, and artifact preflight dependencies; the filtered command builds the affected project/dependencies.

Result:

- Passed: 27
- Failed: 0
- Skipped: 0
- Existing unrelated nullable/analyzer warnings were emitted.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Services.FindGroupMutationPostCSharpLiveTraceRowFixturePlanService` | C# Live Trace Row Fixture Plan | Blocked | Unit Tested | Partial Parity | The planned action `2`/`6` live row fixture is named, but live boundary fixture, live emitter, generated Java rows, registry observation, and comparison execution are missing. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `Aion.GameServer.Services.FindGroupMutationPostCSharpLiveTraceRowFixturePlanService`; `Aion.GameServer.Services.FindGroupMutationPostCSharpTraceEmitterDesignReportService`; `Aion.GameServer.Services.FindGroupMutationPostRegistryObservationTraceContractService` | C# Live Trace Row Fixture Plan / Registry Observation | Partial | Unit Tested | Partial Parity | The plan preserves Java mutation-before-posted-message-before-refreshed-list ordering and action-specific packet ids, but it is design metadata only and does not prove runtime parity. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupMutationPostCSharpLiveTraceRowFixturePlanServiceTests.Create_KeepsPlanBlockedNonLiveAndDispatchDisabled` | Unit | Java/C# live-boundary blockers | Plan remains blocked, non-live, and dispatch-disabled. | Focused plan assertion. | No live fixture. |
| `FindGroupMutationPostCSharpLiveTraceRowFixturePlanServiceTests.Create_CoversActionTwoAndSixOnlyWithStableStepOrder` | Unit | Java action `2` and `6` branches | Plan covers only mutation-post actions with stable step order. | Focused action coverage assertion. | No runtime dispatch. |
| `FindGroupMutationPostCSharpLiveTraceRowFixturePlanServiceTests.Create_PreservesLiveDispatchGuard` | Unit | Deferred `CmFindGroup` boundary | Production live dispatch remains guarded. | Focused guard assertion. | No live wiring. |
| `FindGroupMutationPostCSharpLiveTraceRowFixturePlanServiceTests.Create_RequiresBoundaryAcceptanceFromRealConnectionBoundary` | Unit | Java `CM_FIND_GROUP.runImpl`; C# boundary design | Boundary acceptance must come from real connection boundary, not disabled plan projection. | Focused boundary assertion. | No boundary row emitted. |
| `FindGroupMutationPostCSharpLiveTraceRowFixturePlanServiceTests.Create_RequiresJavaSpecificMutationPostDirectPacketRows` | Unit | Java `FindGroupService.addRecruitment`; `addApplication` | Action-specific mutation and direct packet row evidence is required. | Focused Java-derived row assertion. | No live row emitted. |
| `FindGroupMutationPostCSharpLiveTraceRowFixturePlanServiceTests.Create_RequiresExecutorAndRegistryObservationBeforeRowSerialization` | Unit | Java direct send ordering | Executor and registry evidence must precede row serialization. | Focused ordering assertion. | No registry observation. |
| `FindGroupMutationPostCSharpLiveTraceRowFixturePlanServiceTests.Create_EndsAtArtifactComparisonPreflightWithoutClaimingComparison` | Unit | Artifact comparison preflight | Plan feeds preflight without claiming comparison readiness. | Focused preflight assertion. | No comparison executed. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 2
- Total artifacts ported in this UOW: 0
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 2
- Total blocked artifacts: 1 live `CM_FIND_GROUP` boundary and 1 C# live trace-row fixture/comparison gap
- Estimated overall migration completion: unchanged; Phase 6 remains in progress.

## Remaining Risks

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- The C# live trace-row fixture, live emitter, Java fixture, Java instrumentation, Java serializer, generated Java artifacts, registry-send observation, encrypted socket capture, and deterministic comparison are still missing.
- The fixture plan is non-live metadata and cannot prove Java/C# parity.
- World-broadcast fanout, action `12` invite dispatch, shared singleton interleavings, and runtime/socket comparison remain non-live or missing.

## Next Recommended Unit of Work

Next sequential task:

- Add a mutation-post trace row readiness aggregate that combines the Java capture runbook, C# live trace-row fixture plan, registry observation contract, and artifact comparison preflight into one conservative blocked report.

Safe candidates:

- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Implement the targeted Java/Maven fixture only when ready to edit Java source and capture artifacts.
- Add a live direct-packet boundary test or trace for action `0`/`4` before mutation actions if a lower-risk live observation path is preferred.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostCSharpLiveTraceRowFixturePlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostCSharpLiveTraceRowFixturePlanServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2183-Completion.md`
- `docs/Phase-6-Session-2183-Handoff.md`
