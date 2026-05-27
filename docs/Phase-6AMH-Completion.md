# Phase 6AMH Completion - Protection Task Operation Planner

Date: 2026-05-27
Unit of Work: UOW-1510
Status: Complete after validation.

## Scope

Add a non-live task-operation planner for protection active task scheduling and cancellation, based on Java `PlayerController`, `CreatureController`, `ThreadPoolManager`, `TaskId`, and `Future` behavior.

## Completed Work

- Added `PlayerProtectionActiveTaskTaskOperationPlanService`.
- Planner records start-side delayed stop scheduling:
  - `ThreadPoolManager.getInstance().schedule(this::stopProtectionActiveTask, 60000)`;
  - `addTask(TaskId.PROTECTION_ACTIVE, scheduledFuture)`.
- Planner records Java `CreatureController.addTask` replacement behavior:
  - task map key is `taskId.ordinal()`;
  - if an old future exists, Java calls `oldTask.cancel(false)`;
  - new future is stored under the task id.
- Planner records Java `CreatureController.cancelTask` behavior:
  - remove by `taskId.ordinal()`;
  - if a future exists, call `task.cancel(false)`;
  - if missing, return null and cancel nothing.
- Planner remains non-live. It does not create, store, replace, or cancel C# scheduled tasks.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --no-restore --filter "FullyQualifiedName~PlayerProtectionActiveTaskTaskOperationPlanServiceTests|FullyQualifiedName~PlayerProtectionAttackUtilRecipientPlannerServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSideEffectOperationPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskExecutionBridgeServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSightedRecipientSocketExecutorServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSightedRecipientTraceServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskReportServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskFanoutPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskAdapterServiceTests|FullyQualifiedName~PlayerStateTests"`.
- Result: passed 70 tests.

## Migration Parity Table - UOW-1510

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.PlayerController` | `Aion.GameServer.Services.PlayerProtectionActiveTaskTaskOperationPlanService` | Controller / Task Operation Planner | Partial | Unit Tested | Partial Parity | Planner models protection start delayed-stop scheduling and stop cancellation intent. It is not composed into the bridge yet and does not execute tasks. |
| `com.aionemu.gameserver.controllers.CreatureController` | `PlayerProtectionActiveTaskTaskOperationPlanService` | Controller / Task Map Dependency | Partial | Unit Tested Plan Only | Needs Verification | Planner records `addTask` replacement and `cancelTask` removal/cancel semantics from Java. No live C# task map integration. |
| `com.aionemu.gameserver.utils.ThreadPoolManager` | `PlayerProtectionActiveTaskTaskOperationPlanService` / existing `Aion.GameServer.Utils.ThreadPoolManager` | Scheduler / Runtime Dependency | Partial | Unit Tested Plan Only | Needs Verification | Planner records `schedule(..., 60000)` but does not create a `ScheduledTask`. Existing C# scheduler exists but is not wired for protection active tasks. |
| `com.aionemu.gameserver.model.TaskId` | `PlayerProtectionActiveTaskPlanService` / `PlayerProtectionActiveTaskTaskOperationPlanService` | Enum / Task Key | Partial | Unit Tested | Needs Verification | `TaskId.PROTECTION_ACTIVE` name and ordinal are carried into task-operation rows. C# still lacks a live controller task map keyed by this enum. |
| `java.util.concurrent.Future` | `PlayerProtectionActiveTaskTaskOperationPlanService` / `Aion.GameServer.Utils.ScheduledTask` | Future / Scheduled Task Handle | Partial | Unit Tested Plan Only | Needs Verification | Planner models Java `Future.cancel(false)` replacement/cancel semantics. No live future cancellation is invoked. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Create_StartPlansDelayedScheduleAndTaskStore` | Unit / task planner | `PlayerController.startProtectionActiveTask`, `CreatureController.addTask` | Start branch schedules delayed stop and stores the task under `TaskId.PROTECTION_ACTIVE`. | Deterministic Java source-order assertion. | No live scheduler or task map. |
| `Create_StartWithExistingTaskRecordsReplacementCancellation` | Unit / task planner | `CreatureController.addTask` | Existing task replacement records old future cancellation before new store. | Deterministic Java `tasks.compute` assertion. | No live future cancellation. |
| `Create_AlreadyProtectedStartHasNoTaskOperation` | Unit / task planner | `PlayerController.startProtectionActiveTask` already-protected branch | Already-protected start does not schedule/store/cancel tasks. | Deterministic skipped-branch assertion. | No concurrency coverage. |
| `Create_StopWithExistingTaskRecordsCancelBeforeSpawnedSideEffects` | Unit / task planner | `PlayerController.stopProtectionActiveTask`, `CreatureController.cancelTask` | Stop with existing task records removal and `cancel(false)` before spawned-side effects. | Deterministic Java call-order assertion. | No live task map. |
| `Create_StopWithoutExistingTaskStillRecordsCancelTaskNoOp` | Unit / task planner | `CreatureController.cancelTask` | Stop without a stored task still calls cancelTask, returns null, and cancels nothing. | Deterministic Java null/no-op assertion. | No live task map. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Full game-server test suite is still known to have two unrelated stable failures in `GameServerConnectionInventoryExpansionUseItemTests`: `ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate` and `HandleUseItemAsync_ExpExtractMergesRestrictedRewardWithCleanupSealFlag`.
- Task-operation planner is not yet composed into execution bridge results.
- No live protection task map exists in C#.
- Existing C# `ThreadPoolManager` can schedule/cancel work, but protection active task integration remains gated.
- Java concurrency semantics around `tasks.compute` and cancel races have not been runtime-compared.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 non-live protection task-operation planner and 1 focused test suite
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: Java runtime artifact generation, live protection task map, live delayed stop scheduler integration, live future cancellation, production bridge composition, unrelated full-suite cleanup-seal failures
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: compose `PlayerProtectionActiveTaskTaskOperationPlan` into `PlayerProtectionActiveTaskExecutionBridgeResult`.
- Add an optional existing-task fact to `PlayerProtectionActiveTaskExecutionBridgeRequest`.
- Existing callers should default to no existing task and remain deterministic.
- Keep live scheduler execution and task cancellation disabled.

## Suggested Acceptance Criteria

- Bridge result exposes task-operation plan.
- Existing bridge tests pass with default no-task fact.
- New bridge test covers start with an existing task fact and observes replacement metadata.
- New bridge test covers stop with and without an existing task fact.
- Re-run protection task-operation, planner, side-effect, bridge, adapter, fanout, trace, executor, report, plan, and player-state tests.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Compose task-operation plan into bridge | execution bridge service/tests | Medium | Sequential because bridge result shape changes. |
| B | Live task-map design audit | read-only scheduler/controller files | Low | Can run in parallel if read-only. |
| C | Inventory cleanup-seal failure triage | inventory item-use tests/services | Medium | Separate current full-suite blocker. |

## Do Not Parallelize

- Shared protection bridge files if changing result shapes.
- Shared progress/handoff docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1510] Add protection task operation plan`.
- Files changed in UOW-1510:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerProtectionActiveTaskTaskOperationPlanService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerProtectionActiveTaskTaskOperationPlanServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AMH-Completion.md`
