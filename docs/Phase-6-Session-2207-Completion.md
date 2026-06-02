# Phase 6 Session 2207 Completion - FindGroup Mutation Guarded Fixture Readiness Wiring

Date: 2026-06-02
Unit of Work: UOW-2207
Status: Completed

## Scope

This unit wired the action `2`/`6` mutation-post readiness aggregate to consume the guarded boundary fixture skeleton and disabled C# trace-row shape-input signal.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

C# source reviewed:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostTraceRowReadinessAggregateService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostGuardedLiveBoundaryFixtureSkeletonService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostArtifactComparisonPreflightService.cs`

This UOW does not wire live C# `CmFindGroup` dispatch, does not execute registry sends, does not add live runtime rows, and does not compare Java/C# rows.

## Changes

- Added guarded-boundary skeleton state to `FindGroupMutationPostTraceRowReadinessAggregate`.
- Added a readiness row for `GuardedLiveBoundaryFixtureSkeleton`.
- Propagated `HasCSharpTraceRowShapeInputs` through the aggregate while keeping disabled C# rows distinct from live rows.
- Added `NeedsGuardedBoundaryFixture` so blocker text can report the missing guarded boundary fixture separately from generic live-row absence.
- Kept readiness blocked when disabled shape inputs exist but live boundary rows, executor observation, registry observation, or comparison are missing.
- Updated testing guidance to require a named broad-validation trigger before unfiltered .NET tests or full solution builds.
- Updated live-dispatch design notes.
- Left `docs/PHASE-6-PROGRESS.md` untouched.

## Validation

Validation decision:

- Changed surface: C# service/test-only readiness aggregate wiring plus non-live design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostTraceRowReadinessAggregateServiceTests|FullyQualifiedName~FindGroupMutationPostGuardedLiveBoundaryFixtureSkeletonServiceTests|FullyQualifiedName~FindGroupMutationPostArtifactComparisonPreflightServiceTests|FullyQualifiedName~FindGroupMutationPostComparisonExecutionResultContractServiceTests|FullyQualifiedName~FindGroupMutationPostComparisonInputEnvelopeServiceTests" --no-restore
```

- Focused Java/Maven command: not run; no Java source or Java fixture file changed in this UOW.
- Broad-validation trigger: none. This unit does not modify `GameServerConnection.ProcessPacketAsync`, live dispatch, shared runtime primitives, packet primitives, persistence, common world state, or live side effects.
- Broad .NET decision: skipped intentionally. The filtered C# test command built the affected project and dependencies, and no broad trigger applied.
- Why this scope is sufficient: the edited service is a non-live readiness aggregate, and the focused filter covers the aggregate plus adjacent guarded-fixture, preflight, result-contract, and input-envelope surfaces.

Result:

- Focused C# command: passed 33, failed 0, skipped 0.
- `git diff --check`: passed. Git emitted line-ending normalization warnings for touched documentation and C# files on Windows.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` action `2`/`6` | `Aion.GameServer.Services.FindGroupMutationPostTraceRowReadinessAggregateService`; `Aion.GameServer.Services.FindGroupMutationPostGuardedLiveBoundaryFixtureSkeletonService` | Client Packet Boundary / Readiness Aggregate | Partial | Unit Tested | Partial Parity | Aggregate now distinguishes guarded fixture skeleton and disabled C# shape inputs from live boundary rows. Live boundary execution, executor observation, registry observation, and comparison remain missing. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addRecruitment` | `Aion.GameServer.Services.FindGroupMutationPostTraceRowReadinessAggregateService` | Service Mutation / Readiness Aggregate | Partial | Unit Tested | Partial Parity | Action `2` shape inputs can be recognized, but readiness still blocks until guarded live rows and observations exist. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addApplication` | `Aion.GameServer.Services.FindGroupMutationPostTraceRowReadinessAggregateService` | Service Mutation / Readiness Aggregate | Partial | Unit Tested | Partial Parity | Action `6` shape inputs can be recognized, but readiness still blocks until guarded live rows and observations exist. |
| Repository artifacts under `parity-artifacts/find-group/mutation-post/java` | `Aion.GameServer.Services.FindGroupMutationPostArtifactComparisonPreflightService`; `Aion.GameServer.Services.FindGroupMutationPostTraceRowReadinessAggregateService` | Golden Fixture Artifact / Readiness Handoff | Partial | Unit Tested | Partial Parity | Repository Java artifacts plus disabled C# shape inputs can reach readiness metadata, but live runtime comparison is still blocked. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupMutationPostTraceRowReadinessAggregateServiceTests.Create_DefaultReportPreservesStableBlockedRows` | C# unit | Java action `2`/`6` mutation-post mapping and readiness blockers | Aggregate exposes a stable row set including guarded fixture skeleton state. | Focused C# test. | No live boundary execution. |
| `Create_GuardedFixtureSkeletonRowKeepsProductionDispatchDisabled` | C# unit | Java sends must occur only after accepted boundary execution | Guarded fixture row records trace guard, production dispatch disabled, no sends, and missing executor/registry observations. | Focused C# test. | No registry send observation. |
| `Create_WithShapeInputsStillRequiresGuardedBoundaryLiveRows` | C# unit | Java artifacts plus disabled C# projection/preflight mappings | Disabled shape inputs are recognized without marking rows live or comparison-ready. | Focused C# test using checked-in Java artifacts. | No comparison execution. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 2 Java source artifacts
- Total artifacts ported in this UOW: 1 C# readiness aggregate wiring update
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 4 Java/C# trace-preparation artifacts
- Total blocked artifacts: live C# mutation-post boundary rows, executor observation, registry observation, projected-row comparison, and live `CM_FIND_GROUP` dispatch
- Estimated overall migration completion: unchanged; Phase 6 remains in progress.

## Remaining Risks

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- Disabled C# projection rows are shape inputs only and cannot prove Java/C# runtime parity.
- The guarded fixture skeleton is metadata only and cannot prove live boundary ordering.
- Live C# trace-row emitter, registry send observation, projected-row comparison, world-broadcast fanout, action `12` invite dispatch, and runtime/socket comparison remain missing.

## Next Recommended Unit of Work

Next sequential task:

- Add a guarded fixture result contract for action `2`/`6` that can later hold real boundary rows without sending packets by default.

Safe candidates:

- Add a deterministic Java artifact timestamp override if future fixture churn around `serverEpochSeconds` becomes noisy.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Add more precise readiness blocker wording for executor versus registry observations if the guarded fixture contract needs separate status rows.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostTraceRowReadinessAggregateService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostTraceRowReadinessAggregateServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostComparisonExecutionResultContractServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostComparisonInputEnvelopeServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/orchestration-rules.md`
- `docs/Phase-6-Session-2207-Completion.md`
- `docs/Phase-6-Session-2207-Handoff.md`
