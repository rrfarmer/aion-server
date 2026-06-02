# Phase 6 Session 2206 Completion - FindGroup Mutation Guarded Boundary Fixture Skeleton

Date: 2026-06-02
Unit of Work: UOW-2206
Status: Completed

## Scope

This unit added a guarded live-boundary fixture skeleton report for `CM_FIND_GROUP` action `2`/`6` mutation-post rows.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

C# source reviewed:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostCSharpLiveTraceRowFixturePlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostArtifactComparisonPreflightService.cs`

This UOW does not wire live C# `CmFindGroup` dispatch, does not execute registry sends, does not add live runtime rows, and does not compare Java/C# rows.

## Changes

- Added `FindGroupMutationPostGuardedLiveBoundaryFixtureSkeletonService`.
- The skeleton records:
  - explicit trace guard name,
  - production `ProcessPacketAsync` `CmFindGroup` dispatch remains deferred,
  - action `2` and action `6` guarded boundary scenarios,
  - missing boundary executor observation,
  - missing registry send observation,
  - artifact preflight handoff state.
- Added focused tests that prove:
  - the skeleton is non-live and sends no packets,
  - production dispatch remains disabled,
  - action `2` requires posted system message `1400392` and refreshed `SmFindGroup` action `0`,
  - action `6` requires posted system message `1400393` and refreshed `SmFindGroup` action `4`,
  - executor and registry observations are explicit blockers,
  - repository Java artifacts plus disabled C# shape inputs can be carried through preflight while live rows remain false.
- Updated live-dispatch design notes.
- Left `docs/PHASE-6-PROGRESS.md` untouched.

## Validation

Validation decision:

- Changed surface: C# service/test-only guarded fixture skeleton plus non-live design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostGuardedLiveBoundaryFixtureSkeletonServiceTests|FullyQualifiedName~FindGroupMutationPostCSharpLiveTraceRowFixturePlanServiceTests|FullyQualifiedName~FindGroupMutationPostArtifactComparisonPreflightServiceTests" --no-restore
```

- Focused Java/Maven command: not run; no Java source or Java fixture file changed in this UOW.
- Broad-validation trigger: none. This unit does not modify `GameServerConnection.ProcessPacketAsync`, live dispatch, shared runtime primitives, packet primitives, persistence, common world state, or live side effects.
- Broad .NET decision: skipped intentionally. The filtered C# test command built the affected project and dependencies, and no broad trigger applied.
- Why this scope is sufficient: the added service is a non-live fixture skeleton and depends on existing fixture-plan/preflight services; focused tests cover the skeleton plus both adjacent surfaces.

Result:

- Focused C# command: passed 20, failed 0, skipped 0. Existing nullable/analyzer warnings were emitted from unrelated C# files.
- `git diff --check`: passed. Git emitted line-ending normalization warnings for touched documentation files on Windows.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` action `2`/`6` | `Aion.GameServer.Services.FindGroupMutationPostGuardedLiveBoundaryFixtureSkeletonService`; `Aion.GameServer.Network.Aion.GameServerConnection` | Client Packet Boundary / Fixture Skeleton | Partial | Unit Tested | Partial Parity | Skeleton records a future guarded boundary fixture for actions `2`/`6` while keeping production `ProcessPacketAsync` dispatch disabled. No live boundary rows, executor invocation, registry observation, or comparison exists. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addRecruitment` | `Aion.GameServer.Services.FindGroupMutationPostGuardedLiveBoundaryFixtureSkeletonService` | Service Mutation / Fixture Skeleton | Partial | Unit Tested | Partial Parity | Action `2` scenario names Java recruitment mutation ordering, posted system message `1400392`, and refreshed action `0`. Evidence is planning/skeleton only. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addApplication` | `Aion.GameServer.Services.FindGroupMutationPostGuardedLiveBoundaryFixtureSkeletonService` | Service Mutation / Fixture Skeleton | Partial | Unit Tested | Partial Parity | Action `6` scenario names Java application mutation ordering, posted system message `1400393`, and refreshed action `4`. Evidence is planning/skeleton only. |
| Repository artifacts under `parity-artifacts/find-group/mutation-post/java` | `Aion.GameServer.Services.FindGroupMutationPostGuardedLiveBoundaryFixtureSkeletonService`; `Aion.GameServer.Services.FindGroupMutationPostArtifactComparisonPreflightService` | Golden Fixture Artifact / Skeleton Handoff | Partial | Unit Tested | Partial Parity | Skeleton can carry shape-valid Java artifact and disabled C# shape-input state through preflight, but live rows remain false. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupMutationPostGuardedLiveBoundaryFixtureSkeletonServiceTests.Create_KeepsSkeletonBlockedNonLiveAndProductionDispatchDisabled` | C# unit | Existing C# deferred `CmFindGroup` boundary and Java action mapping | Skeleton is non-live, sends no packets, and keeps production dispatch disabled. | Focused C# test. | No live boundary execution. |
| `Create_NamesJavaActionTwoAndSixBoundaryScenarios` | C# unit | Java `FindGroupService.addRecruitment/addApplication` system-message and refreshed-list mappings | Guarded fixture skeleton records action-specific expected mutation-post evidence. | Focused C# test. | No runtime row capture. |
| `Create_RecordsExecutorAndRegistryObservationsAsMissing` | C# unit | Java `PacketSendUtility.sendPacket` ordering requirement | Executor and registry observations are explicit blockers. | Focused C# test. | No registry send observation. |
| `Create_CanCarryPreflightShapeInputsWithoutMarkingLiveRows` | C# unit | Java artifacts plus disabled C# projection/preflight mappings | Skeleton can carry shape-valid Java artifacts and disabled C# shape inputs while live rows remain false. | Focused C# test using checked-in Java artifacts. | No comparison execution. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 2 Java source artifacts
- Total artifacts ported in this UOW: 1 C# fixture-skeleton service
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 4 Java/C# trace-preparation artifacts
- Total blocked artifacts: live C# mutation-post boundary rows, registry observation, projected-row comparison, and live `CM_FIND_GROUP` dispatch
- Estimated overall migration completion: unchanged; Phase 6 remains in progress.

## Remaining Risks

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- The skeleton is metadata only and cannot prove Java/C# parity.
- Shape-valid Java fixture artifacts and disabled C# projection rows are not runtime comparison evidence.
- Live C# trace-row emitter, registry send observation, projected-row comparison, world-broadcast fanout, action `12` invite dispatch, and runtime/socket comparison remain missing.

## Next Recommended Unit of Work

Next sequential task:

- Add readiness aggregate wiring that consumes the guarded boundary fixture skeleton and `HasCSharpTraceRowShapeInputs` for clearer blocker text while still requiring live rows, executor observation, and registry observation.

Safe candidates:

- Add a guarded fixture result contract for action `2`/`6` that can later hold real boundary rows without sending packets by default.
- Add a deterministic Java artifact timestamp override if future fixture churn around `serverEpochSeconds` becomes noisy.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostGuardedLiveBoundaryFixtureSkeletonService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostGuardedLiveBoundaryFixtureSkeletonServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2206-Completion.md`
- `docs/Phase-6-Session-2206-Handoff.md`
