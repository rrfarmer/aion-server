# Phase 6AMM Completion - Protection Task Map Adapter Prototype

Date: 2026-05-27
Unit of Work: UOW-1515
Status: Complete after validation.

## Scope

Add a non-live protection task-map adapter contract/prototype that models Java task-map operations against supplied fake scheduled-task handles without invoking `ThreadPoolManager`.

## Completed Work

- Added `PlayerProtectionActiveTaskTaskMapAdapterService`.
- Added `IPlayerProtectionActiveTaskTaskHandle` so tests can observe Java-shaped `Future.cancel(false)` calls without scheduler threads.
- Added operation/status/result/snapshot record types.
- The in-memory adapter models:
  - `hasTask`;
  - `hasScheduledTask`;
  - `getAndRemoveTask`;
  - `cancelTask`;
  - `cancelTaskIfPresent`;
  - `addTask`;
  - `cancelAllTasks`;
  - deterministic task-map snapshots.
- The adapter preserves Java task-map rules in non-live behavior:
  - task keys use supplied ordinals/names, including `TaskId.PROTECTION_ACTIVE` ordinal 3 in tests;
  - add stores a new handle and atomically cancels/replaces an old handle under a lock;
  - cancel removes before calling `Cancel(false)`;
  - missing cancel is a no-op;
  - conditional cancel only removes/cancels matching handle instances;
  - cancel-all cancels all stored handles and clears the map.
- No live scheduler callback, task map owner, or delayed protection stop was wired.

## Validation

- First focused run exposed an incorrect conditional-cancel test expectation and nullable warnings; both were fixed.
- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --no-restore --filter "FullyQualifiedName~PlayerProtectionActiveTaskTaskMapAdapterServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskMapAuditServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskLiveReadinessServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskExecutionSummaryServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskOperationPlanServiceTests|FullyQualifiedName~PlayerProtectionAttackUtilRecipientPlannerServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSideEffectOperationPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskExecutionBridgeServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSightedRecipientSocketExecutorServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSightedRecipientTraceServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskReportServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskFanoutPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskAdapterServiceTests|FullyQualifiedName~PlayerStateTests"`.
- Result: passed 93 tests.

## Migration Parity Table - UOW-1515

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.CreatureController` | `Aion.GameServer.Services.PlayerProtectionActiveTaskTaskMapAdapterService` | Controller / Task Map Prototype | Partial | Unit Tested Prototype | Partial Parity | Prototype models Java task-map operations but is not attached to a real controller/player lifecycle. Missing production owner, deletion/logout cleanup hook, and live scheduling integration. |
| `com.aionemu.gameserver.model.TaskId` | `PlayerProtectionActiveTaskTaskMapAdapterService` caller-supplied ordinal/name | Enum / Task Key Prototype | Partial | Unit Tested Prototype | Needs Verification | Tests use `PROTECTION_ACTIVE` ordinal 3. Full enum mapping and all task ids are not represented by this prototype. |
| `java.util.concurrent.Future` | `IPlayerProtectionActiveTaskTaskHandle` | Interface / Future Handle Prototype | Partial | Unit Tested Prototype | Needs Verification | Fake handle receives `Cancel(false)` calls and exposes `IsDone`; real Java `Future` behavior and C# `ScheduledTask` race behavior are not runtime-compared. |
| `java.util.concurrent.ConcurrentHashMap` | locked `Dictionary<int, StoredTask>` inside `PlayerProtectionActiveTaskTaskMapAdapterService` | Concurrency / Collection Prototype | Partial | Unit Tested Prototype | Needs Verification | Adapter uses a lock to preserve atomic operations in tests. Java `ConcurrentHashMap.compute/remove(key,value)` concurrency has not been runtime-compared. |
| `com.aionemu.gameserver.utils.ThreadPoolManager` | not invoked; future adapter input only | Scheduler Dependency | Not Started | Unit Tested Prototype Boundary | Needs Verification | Prototype intentionally avoids creating scheduled tasks. Live `ThreadPoolManager.Schedule` integration remains blocked. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `AddTask_StoresNewHandleAndReportsPresence` | Unit / prototype | `CreatureController.addTask/hasTask/hasScheduledTask` | New task storage, presence check, scheduled check, and snapshot metadata. | Static Java source behavior encoded with fake handles. | No live scheduler/controller owner. |
| `AddTask_ReplacesExistingHandleAndCancelsOldHandle` | Unit / prototype | `CreatureController.addTask` | Replacement cancels old handle with `false` and stores new handle. | Deterministic Java replacement assertion. | No Java runtime race comparison. |
| `CancelTask_RemovesBeforeCancelAndMissingCancelIsNoOp` | Unit / prototype | `CreatureController.cancelTask` | Existing task remove/cancel and missing task no-op. | Deterministic Java cancel assertion. | No live future. |
| `GetAndRemoveTask_RemovesWithoutCanceling` | Unit / prototype | `CreatureController.getAndRemoveTask` | Removal returns the handle and does not cancel it. | Deterministic Java source assertion. | No live map. |
| `CancelTaskIfPresent_CancelsOnlyMatchingStoredHandle` | Unit / prototype | `CreatureController.cancelTaskIfPresent` | Conditional remove/cancel only when stored handle reference matches. | Deterministic Java `remove(key,value)` assertion. | No concurrent race comparison. |
| `CancelAllTasks_CancelsEveryStoredHandleAndClearsMap` | Unit / prototype | `CreatureController.cancelAllTasks` | Cancel-all invokes `Cancel(false)` on all stored handles and clears the map; empty cancel-all is no-op. | Deterministic Java cleanup assertion. | No controller lifecycle hook. |
| `HasScheduledTask_TreatsDoneHandleAsNotScheduled` | Unit / prototype | `CreatureController.hasScheduledTask` | Done handles remain present but are not scheduled. | Deterministic Java `!task.isDone()` assertion. | Fake `IsDone` only. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Full game-server test suite is still known to have two unrelated stable failures in `GameServerConnectionInventoryExpansionUseItemTests`: `ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate` and `HandleUseItemAsync_ExpExtractMergesRestrictedRewardWithCleanupSealFlag`.
- Task-map adapter is a non-live prototype and not consumed by protection bridge/adapter code.
- No live scheduler callback, delayed stop invocation, production task-map owner, or lifecycle cleanup hook exists.
- Lock-based C# atomicity has not been compared to Java `ConcurrentHashMap.compute` under concurrency.
- `IPlayerProtectionActiveTaskTaskHandle.Cancel(false)` fake behavior is not proven equivalent to Java `Future.cancel(false)` or C# `ScheduledTask.Cancel()` under runtime races.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 non-live protection task-map adapter prototype and 1 focused test suite
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: Java runtime artifact generation, live task-map owner, live scheduler callback, live delayed stop invocation, lifecycle cleanup hook, Java/C# future race comparison, unrelated full-suite cleanup-seal failures
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: compose the non-live task-map adapter prototype into a protection task-map simulation/report service.
- Consume `PlayerProtectionActiveTaskTaskOperationPlan` rows and produce adapter operation results for:
  - start schedule/store without existing task;
  - start replacement with existing task;
  - stop cancel with existing task;
  - stop missing cancel no-op;
  - cancel-all cleanup scenario.
- Keep the simulation disconnected from `ThreadPoolManager` and production protection execution.

## Suggested Acceptance Criteria

- Simulation service accepts a task-operation plan plus supplied fake handles and emits ordered adapter operation results.
- Tests cover start store, start replacement, stop cancel, missing stop cancel, and cancel-all cleanup.
- Simulation rows preserve Java source breadcrumbs from task-operation plan and adapter results.
- Re-run protection task-map adapter, audit, readiness, summary, task-operation, planner, side-effect, bridge, adapter, fanout, trace, executor, report, plan, and player-state tests.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Protection task-map simulation/report service | new service/tests | Medium | Safe if it remains non-live and avoids production bridge execution. |
| B | Java task-map runtime comparison design | docs/read-only Java sources | Low | Can be parallel if no docs overlap or orchestrator owns docs. |
| C | Inventory cleanup-seal failure triage | inventory item-use tests/services | Medium | Separate current full-suite blocker. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Add non-live task-map simulation/report service and tests | new simulation service/tests, progress/handoff docs | production scheduler wiring, protection bridge execution path |

No subagent is required unless read-only Java runtime-comparison design is split off.

## Do Not Parallelize

- Shared scheduler implementation.
- Production protection bridge/adapter execution paths.
- Shared progress/handoff docs.
- Any live delayed stop scheduling.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1515] Add protection task map adapter prototype`.
- Files changed in UOW-1515:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerProtectionActiveTaskTaskMapAdapterService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerProtectionActiveTaskTaskMapAdapterServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AMM-Completion.md`
- Latest prior commits:
  - `f3c701268 [Phase 6][UOW-1514] Add protection task map audit`
- Required startup remains:
  - read `docs/csharp-port.md`;
  - read `docs/PHASE-6-PROGRESS.md`;
  - read latest `docs/Phase-6*-Completion.md`;
  - read `docs/orchestration-rules.md`;
  - read `docs/parallelization-strategy.md`;
  - read `docs/parity-verification.md`;
  - note `docs/commit-conventions.md` is still missing.
