# Phase 6 Session 2184 Completion - FindGroup Mutation Trace Row Readiness Aggregate

Date: 2026-06-02
Unit of Work: UOW-2184
Status: Completed

## Scope

This unit added a non-live readiness aggregate for `CM_FIND_GROUP` action `2` and `6` mutation-post trace rows.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

This UOW does not wire live `CmFindGroup` dispatch, does not capture Java artifacts, does not emit C# runtime trace rows, does not observe live registry sends, and does not execute artifact comparison.

## Changes

- Added `FindGroupMutationPostTraceRowReadinessAggregateService`.
- The aggregate combines:
  - Java artifact capture runbook readiness,
  - C# live trace-row fixture plan readiness,
  - registry observation contract readiness,
  - artifact comparison preflight readiness.
- Preserved conservative status priority:
  - missing Java fixture/instrumentation/artifacts block first,
  - missing C# live rows block next,
  - missing live registry observation blocks next,
  - missing deterministic comparison blocks last.
- Kept the aggregate non-live and blocked by default.
- Added focused tests for default blocked state, stable row ordering, Java runbook evidence, C# boundary guard evidence, registry observation evidence, and artifact comparison preflight evidence.
- Updated live-dispatch design notes to include the readiness aggregate and remaining blockers.
- Left `docs/PHASE-6-PROGRESS.md` untouched.

## Validation

Validation decision:

- Changed surface: focused non-live readiness aggregate service, focused tests, and non-live design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostTraceRowReadinessAggregateServiceTests|FullyQualifiedName~FindGroupMutationPostJavaArtifactCaptureRunbookServiceTests|FullyQualifiedName~FindGroupMutationPostCSharpLiveTraceRowFixturePlanServiceTests|FullyQualifiedName~FindGroupMutationPostRegistryObservationTraceContractServiceTests|FullyQualifiedName~FindGroupMutationPostArtifactComparisonPreflightServiceTests" --no-restore
```

- Focused Java/Maven command: not run. No Java source changed, no Java fixture/instrumentation/serializer exists, and no generated Java artifacts exist.
- Broad-validation trigger: none. No live connection dispatch, live side effects, packet primitive, common runtime base, persistence, shared infrastructure, Java source, or broad behavior surface changed.
- Broad .NET decision: skipped intentionally. The filtered `dotnet test` command built the affected project and dependencies.
- Why this scope is sufficient: the filtered tests cover the new aggregate and its immediate Java runbook, C# live row fixture plan, registry contract, and artifact comparison preflight dependencies without spending time on unrelated suites.

Result:

- Passed: 30
- Failed: 0
- Skipped: 0

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Services.FindGroupMutationPostTraceRowReadinessAggregateService` | Trace Row Readiness Aggregate | Blocked | Unit Tested | Partial Parity | The aggregate records action `2`/`6` mutation-post readiness inputs, but Java capture, C# live rows, registry observation, and comparison execution are missing. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `Aion.GameServer.Services.FindGroupMutationPostTraceRowReadinessAggregateService`; `Aion.GameServer.Services.FindGroupMutationPostJavaArtifactCaptureRunbookService`; `Aion.GameServer.Services.FindGroupMutationPostCSharpLiveTraceRowFixturePlanService`; `Aion.GameServer.Services.FindGroupMutationPostRegistryObservationTraceContractService`; `Aion.GameServer.Services.FindGroupMutationPostArtifactComparisonPreflightService` | Mutation-Post Trace Row Readiness | Partial | Unit Tested | Partial Parity | The aggregate preserves Java mutation-before-posted-message-before-refreshed-list requirements for action `2`/`6`, but it is non-live metadata and proves no runtime parity. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupMutationPostTraceRowReadinessAggregateServiceTests.Create_DefaultAggregateBlocksOnJavaCaptureAndIsNonLive` | Unit | Java source review and current readiness blockers | Default aggregate remains non-live and blocked on missing Java capture inputs. | Focused aggregate assertion. | No Java fixture/artifacts or live C# rows. |
| `FindGroupMutationPostTraceRowReadinessAggregateServiceTests.Create_ListsStableRowsForEachAggregateInput` | Unit | Readiness dependency chain | Aggregate lists Java runbook, C# fixture plan, registry contract, and comparison preflight in stable order. | Focused row-order assertion. | No runtime comparison. |
| `FindGroupMutationPostTraceRowReadinessAggregateServiceTests.Create_JavaRunbookRowNamesFixtureArtifactsAndFocusedMavenCommand` | Unit | Java `CM_FIND_GROUP` and `FindGroupService` capture plan | Java row names planned fixture, artifacts, and focused Maven command. | Focused runbook evidence assertion. | Fixture is not implemented. |
| `FindGroupMutationPostTraceRowReadinessAggregateServiceTests.Create_CSharpFixtureRowKeepsBoundaryDisabled` | Unit | Deferred C# live boundary plan | C# fixture row keeps live boundary disabled and side effects off. | Focused boundary guard assertion. | No live trace row emitter. |
| `FindGroupMutationPostTraceRowReadinessAggregateServiceTests.Create_RegistryRowRequiresOrderedDirectSendsAndNoUnexpectedSideEffects` | Unit | Java direct send ordering | Registry row requires ordered direct sends and zero broadcast/invite side effects. | Focused registry contract assertion. | No live registry observation. |
| `FindGroupMutationPostTraceRowReadinessAggregateServiceTests.Create_PreflightRowReflectsDefaultMissingJavaArtifacts` | Unit | Artifact comparison preflight | Preflight row remains blocked on missing Java artifacts and does not claim verified parity. | Focused preflight assertion. | No Java/C# row comparison. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 2
- Total artifacts ported in this UOW: 0
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 2
- Total blocked artifacts: 1 live `CM_FIND_GROUP` boundary and 1 mutation-post trace-row capture/comparison chain
- Estimated overall migration completion: unchanged; Phase 6 remains in progress.

## Remaining Risks

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- Java fixture, Java instrumentation, Java serializer, generated Java artifacts, C# live fixture, live emitter, registry observation, and deterministic comparison are missing.
- The readiness aggregate is non-live metadata and cannot prove Java/C# runtime parity.
- World-broadcast fanout, action `12` invite dispatch, shared singleton interleavings, and runtime/socket comparison remain non-live or missing.

## Next Recommended Unit of Work

Next sequential task:

- Add a comparison execution result contract for projected action `2`/`6` mutation-post rows, defining how differences will be represented without executing comparison until Java/C# rows exist.

Safe candidates:

- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Implement the targeted Java/Maven fixture only when ready to edit Java source and capture artifacts.
- Add a live direct-packet boundary test or trace for action `0`/`4` before mutation actions if a lower-risk live observation path is preferred.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostTraceRowReadinessAggregateService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostTraceRowReadinessAggregateServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2184-Completion.md`
- `docs/Phase-6-Session-2184-Handoff.md`
