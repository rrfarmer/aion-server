# Phase 6AMD Completion - Protection Side-Effect Operation Plan

Date: 2026-05-27
Unit of Work: UOW-1506
Status: Complete after validation.

## Scope

Add a non-live operation plan for the remaining `PlayerController.startProtectionActiveTask` and `stopProtectionActiveTask` side effects that are not yet safe to execute live.

## Completed Work

- Added `PlayerProtectionActiveTaskSideEffectOperationPlanService`.
- Start-side plan records:
  - active-protection condition check;
  - set BLINKING;
  - `AttackUtil.cancelCastOn(getOwner())`;
  - `AttackUtil.removeTargetFrom(getOwner())`;
  - `PacketSendUtility.broadcastToSightedPlayers(..., true)`;
  - delayed stop scheduling after 60000 ms;
  - `TaskId.PROTECTION_ACTIVE` storage.
- Stop-side plan records:
  - `cancelTask(TaskId.PROTECTION_ACTIVE)` before spawned guard;
  - spawned guard;
  - unset BLINKING;
  - packet fanout;
  - `notifyAIOnMove()`.
- Player flight-transporter/windstream guard is modeled as a skipped AI notification operation.
- Only opt-in visual mutation is marked live; all scheduler, cast, target, packet, and AI movement work remains planned-not-live.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --no-restore --filter "FullyQualifiedName~PlayerProtectionActiveTaskSideEffectOperationPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskExecutionBridgeServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSightedRecipientSocketExecutorServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSightedRecipientTraceServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskReportServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskFanoutPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskAdapterServiceTests|FullyQualifiedName~PlayerStateTests"`.
- Result: passed 58 tests.

## Migration Parity Table - UOW-1506

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.PlayerController` | `Aion.GameServer.Services.PlayerProtectionActiveTaskSideEffectOperationPlanService` | Controller / Operation Plan | Partial | Unit Tested | Partial Parity | Start/stop side-effect ordering is explicit. Only visual mutation may be live through the adapter; scheduler, cast, target, packet, and AI side effects remain non-live. |
| `com.aionemu.gameserver.controllers.attack.AttackUtil` | `PlayerProtectionActiveTaskSideEffectOperationPlanService` | Utility / Combat Side Effect | Not Started | Unit Tested Plan Only | Needs Verification | Plan records `cancelCastOn` and `removeTargetFrom` order and Java known-list predicates. No C# live traversal, casting-skill cancellation, target mutation, or runtime comparison yet. |
| `com.aionemu.gameserver.utils.ThreadPoolManager` | `PlayerProtectionActiveTaskSideEffectOperationPlanService` | Scheduler / Runtime Dependency | Not Started | Unit Tested Plan Only | Needs Verification | Plan records delayed stop scheduling with 60000 ms delay. No C# scheduler execution or task future storage is enabled. |
| `com.aionemu.gameserver.model.TaskId` | `PlayerProtectionActiveTaskPlanService` / `PlayerProtectionActiveTaskSideEffectOperationPlanService` | Enum / Task Key | Partial | Unit Tested | Needs Verification | `TaskId.PROTECTION_ACTIVE` name and ordinal are modeled from Java, and stop-side cancel is recorded even when no task fact is supplied. No live controller task map integration. |
| `com.aionemu.gameserver.controllers.PlayerController.notifyAIOnMove` | `PlayerProtectionActiveTaskSideEffectOperationPlanService` | Controller Override / AI Movement Guard | Partial | Unit Tested | Needs Verification | Plan records the flight-transporter/windstream guard before movement notification. C# movement notification is not executed. |
| `com.aionemu.gameserver.controllers.CreatureController` / `com.aionemu.gameserver.taskmanager.tasks.MovementNotifyTask` | `PlayerProtectionActiveTaskSideEffectOperationPlanService` | AI Movement Dependency | Not Started | Unit Tested Plan Only | Needs Verification | Plan records `MovementNotifyTask.add(owner)` as the eventual live dependency. No C# task manager integration or runtime comparison. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Create_StartOrdersVisualAttackFanoutAndSchedulerSideEffects` | Unit / operation plan | `PlayerController.startProtectionActiveTask`, `AttackUtil`, `ThreadPoolManager` | Start branch order: check, visual mutation, cast cancel, target cleanup, fanout, delayed schedule, task storage. | Deterministic source-order assertion. | No live scheduler, cast, target, or packet execution. |
| `Create_AlreadyProtectedStartStopsAfterConditionCheck` | Unit / operation plan | `PlayerController.startProtectionActiveTask` already-protected branch | Already-protected start stops after condition check. | Deterministic skipped-branch assertion. | No concurrency coverage. |
| `Create_StopOrdersCancelTaskSpawnedGuardVisualFanoutAndAiMove` | Unit / operation plan | `PlayerController.stopProtectionActiveTask`, movement notify path | Stop branch order: cancel task before spawned guard, visual mutation, fanout, AI movement notification. | Deterministic source-order assertion. | No live movement task integration. |
| `Create_UnspawnedStopStopsAfterCancelTaskAndSpawnedGuard` | Unit / operation plan | `PlayerController.stopProtectionActiveTask` unspawned branch | Unspawned stop still reaches `cancelTask`, then stops after spawned guard. | Corrects and makes explicit the Java call-vs-existing-task distinction. | No live task map integration. |
| `Create_StopSkipsAiMoveNotificationWhileUsingFlightPath` | Unit / operation plan | `PlayerController.notifyAIOnMove` override | Flight transporter/windstream guard skips movement notification. | Deterministic guard assertion using C# flight-path state. | No live movement task comparison. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Full game-server test suite is still known to have two unrelated stable failures in `GameServerConnectionInventoryExpansionUseItemTests`: `ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate` and `HandleUseItemAsync_ExpExtractMergesRestrictedRewardWithCleanupSealFlag`.
- Side-effect plan is non-live and not yet composed into the execution bridge result.
- No live known-list traversal exists for cast cancellation or target cleanup.
- No live scheduler/task-map integration exists for `TaskId.PROTECTION_ACTIVE`.
- No live `MovementNotifyTask` integration exists for protection stop AI notification.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 1 non-live protection side-effect operation-plan service and 1 focused test suite
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: Java runtime artifact generation, live scheduler/task storage, live AttackUtil cast/target traversal, production packet fanout, live MovementNotifyTask integration, unrelated full-suite cleanup-seal failures
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: compose `PlayerProtectionActiveTaskSideEffectOperationPlan` into `PlayerProtectionActiveTaskExecutionBridgeResult`.
- Keep packet sends disabled by default.
- Keep scheduler, cast cancellation, target mutation, and AI notification non-live.
- Goal: one bridge result should expose adapter/fanout/trace/packet/socket executor metadata plus the ordered side-effect operation plan.

## Suggested Acceptance Criteria

- Execution bridge result includes the side-effect operation plan.
- Start bridge test verifies the composed operation plan includes cast cancel, target cleanup, fanout, schedule, and task storage rows.
- Stop bridge test verifies the composed operation plan includes cancel task before spawned guard, fanout, and AI move notification or flight-path skip.
- Skipped branches still create no packet and no sends, while exposing the skipped operation plan.
- Re-run the protection operation-plan, execution-bridge, adapter, fanout, trace, executor, report, plan, and player-state tests.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Compose side-effect plan into execution bridge | execution bridge service/tests | Medium | Changes a recently added result shape; keep scoped. |
| B | AttackUtil non-live recipient planner | new service/tests | Medium | Could model cast/target known-list candidates separately after bridge composition. |
| C | Inventory cleanup-seal failure triage | inventory item-use tests/services | Medium | Separate current full-suite blocker. |

## Do Not Parallelize

- Shared protection bridge files if changing result shape.
- Shared progress/handoff docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1506] Add protection side effect plan`.
- Files changed in UOW-1506:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerProtectionActiveTaskSideEffectOperationPlanService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerProtectionActiveTaskSideEffectOperationPlanServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AMD-Completion.md`
