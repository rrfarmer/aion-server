# Phase 6AME Completion - Protection Execution Bridge Side-Effect Composition

Date: 2026-05-27
Unit of Work: UOW-1507
Status: Complete after validation.

## Scope

Compose `PlayerProtectionActiveTaskSideEffectOperationPlan` into `PlayerProtectionActiveTaskExecutionBridgeResult` so one bridge result exposes the full staged protection workflow without enabling additional side effects.

## Completed Work

- Extended `PlayerProtectionActiveTaskExecutionBridgeResult` with `SideEffectOperationPlan`.
- Execution bridge now exposes:
  - adapter result;
  - ordered side-effect operation plan;
  - concrete `SmPlayerState` for broadcast branches;
  - sighted-recipient socket executor result;
  - disabled no-send metadata.
- Packet sends remain disabled by default.
- Scheduler, cast cancellation, target mutation, and AI notification remain non-live operation-plan rows.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --no-restore --filter "FullyQualifiedName~PlayerProtectionActiveTaskSideEffectOperationPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskExecutionBridgeServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSightedRecipientSocketExecutorServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSightedRecipientTraceServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskReportServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskFanoutPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskAdapterServiceTests|FullyQualifiedName~PlayerStateTests"`.
- Result: passed 58 tests.

## Migration Parity Table - UOW-1507

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.PlayerController` | `Aion.GameServer.Services.PlayerProtectionActiveTaskExecutionBridgeService` | Controller / Execution Bridge | Partial | Unit Tested | Partial Parity | Bridge now exposes the full staged protection workflow from one result: adapter/visual mutation, side-effect operation plan, packet construction, trace, and disabled executor metadata. |
| `com.aionemu.gameserver.controllers.attack.AttackUtil` | `PlayerProtectionActiveTaskExecutionBridgeService` / `PlayerProtectionActiveTaskSideEffectOperationPlanService` | Utility / Combat Side Effect | Not Started | Unit Tested Plan Only | Needs Verification | Cast cancellation and target cleanup remain plan rows only. No known-list recipient planner or live mutation exists yet. |
| `com.aionemu.gameserver.utils.ThreadPoolManager` | `PlayerProtectionActiveTaskExecutionBridgeService` / side-effect plan | Scheduler / Runtime Dependency | Not Started | Unit Tested Plan Only | Needs Verification | Bridge exposes delayed stop scheduling metadata but does not schedule or store a task. |
| `com.aionemu.gameserver.model.TaskId` | `PlayerProtectionActiveTaskPlanService` / bridge-composed side-effect plan | Enum / Task Key | Partial | Unit Tested | Needs Verification | Bridge exposes `TaskId.PROTECTION_ACTIVE` cancellation/storage metadata through the side-effect plan. No live task map integration. |
| `com.aionemu.gameserver.controllers.PlayerController.notifyAIOnMove` | `PlayerProtectionActiveTaskExecutionBridgeService` / side-effect plan | Controller Override / AI Movement Guard | Partial | Unit Tested | Needs Verification | Bridge exposes AI move notification or flight-path skip metadata. No live movement notification. |
| `com.aionemu.gameserver.controllers.CreatureController` / `com.aionemu.gameserver.taskmanager.tasks.MovementNotifyTask` | bridge-composed side-effect plan | AI Movement Dependency | Not Started | Unit Tested Plan Only | Needs Verification | Movement notify task integration remains unimplemented; bridge only exposes the planned dependency. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ExecuteAsync_LiveStartBuildsPlayerStatePacketAndDisabledExecutorDoesNotSend` | Unit / execution bridge | `PlayerController.startProtectionActiveTask`, `AttackUtil`, scheduler, packet fanout | Bridge result includes side-effect plan rows for cast cancel, target cleanup, fanout, schedule, and task storage while executor remains disabled. | Deterministic source-order exposure through one bridge result. | No live cast/target/scheduler execution. |
| `ExecuteAsync_LiveSpawnedStopBuildsPlayerStatePacketAfterClearingBlinking` | Unit / execution bridge | `PlayerController.stopProtectionActiveTask`, movement notify path | Bridge result includes cancel task, spawned guard, visual mutation, fanout, and AI move notification rows. | Deterministic source-order exposure through one bridge result. | No live movement notification or packet send. |
| `ExecuteAsync_SkippedBranchesDoNotConstructPacketOrSend` | Unit / execution bridge | already-protected start and unspawned stop branches | Skipped branches expose side-effect operation plans while creating no packet and no sends. | Deterministic skipped-branch assertion. | No runtime comparison. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Full game-server test suite is still known to have two unrelated stable failures in `GameServerConnectionInventoryExpansionUseItemTests`: `ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate` and `HandleUseItemAsync_ExpExtractMergesRestrictedRewardWithCleanupSealFlag`.
- Bridge result shape changed; no production caller currently consumes it.
- No live known-list traversal exists for cast cancellation or target cleanup.
- No live scheduler/task-map integration exists for `TaskId.PROTECTION_ACTIVE`.
- No live `MovementNotifyTask` integration exists for protection stop AI notification.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: bridge composition of the protection side-effect operation plan plus focused bridge test updates
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: Java runtime artifact generation, live scheduler/task storage, live AttackUtil cast/target traversal, production packet fanout, live MovementNotifyTask integration, unrelated full-suite cleanup-seal failures
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: add a non-live `AttackUtil` protection recipient planner.
- Project `cancelCastOn(getOwner())` recipients from supplied known-object/casting facts:
  - visible object is a creature;
  - visible creature target is the protected player;
  - visible creature is casting;
  - casting skill first target equals the protected player.
- Project `removeTargetFrom(getOwner())` recipients from supplied known-player facts:
  - known player target is the protected player;
  - `validateSee=false` for protection start, so no `canSee` filter is applied.
- Keep all cast cancellation and target mutation disabled.

## Suggested Acceptance Criteria

- New planner uses explicit DTO facts instead of live known-list traversal.
- Tests cover eligible/ineligible cast-cancel candidates, eligible/ineligible target-clear candidates, duplicate candidate handling, and empty known-list input.
- Planner rows link back to `AttackUtil.cancelCastOn` and `AttackUtil.removeTargetFrom` Java sources.
- Operation plan/bridge remains non-live unless a later unit composes planner metadata.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | AttackUtil non-live recipient planner | new service/tests | Medium | New files; good isolated next step. |
| B | Compose AttackUtil planner into side-effect plan | side-effect plan/tests | Later | Medium | Should wait until planner exists. |
| C | Inventory cleanup-seal failure triage | inventory item-use tests/services | Medium | Separate current full-suite blocker. |

## Do Not Parallelize

- Shared protection bridge/side-effect files if changing result shapes.
- Shared progress/handoff docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1507] Compose protection side effect plan`.
- Files changed in UOW-1507:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerProtectionActiveTaskExecutionBridgeService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerProtectionActiveTaskExecutionBridgeServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AME-Completion.md`
