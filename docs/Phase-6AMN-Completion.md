# Phase 6AMN Completion - Protection Task Map Simulation

Date: 2026-05-27
Unit of Work: UOW-1516
Status: Complete after validation.

## Scope

Compose the non-live task-map adapter prototype into a protection task-map simulation/report service that consumes `PlayerProtectionActiveTaskTaskOperationPlan` rows and produces adapter operation results without invoking `ThreadPoolManager` or production protection execution.

## Completed Work

- Added `PlayerProtectionActiveTaskTaskMapSimulationService`.
- Added simulation request/report/row record types.
- Simulation consumes task-operation plan rows and uses `PlayerProtectionActiveTaskTaskMapAdapterService` with supplied fake handles.
- Simulation coverage includes:
  - start schedule/store without an existing task;
  - start replacement with an existing task;
  - stop cancel with an existing task;
  - stop missing cancel no-op;
  - cancel-all cleanup after a start/store scenario.
- Simulation rows preserve Java source breadcrumbs from the task-operation plan and adapter results.
- No live scheduler callback, delayed stop invocation, production task-map owner, or production bridge execution was wired.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --no-restore --filter "FullyQualifiedName~PlayerProtectionActiveTaskTaskMapSimulationServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskMapAdapterServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskMapAuditServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskLiveReadinessServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskExecutionSummaryServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskOperationPlanServiceTests|FullyQualifiedName~PlayerProtectionAttackUtilRecipientPlannerServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSideEffectOperationPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskExecutionBridgeServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSightedRecipientSocketExecutorServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSightedRecipientTraceServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskReportServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskFanoutPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskAdapterServiceTests|FullyQualifiedName~PlayerStateTests"`.
- Result: passed 98 tests.

## Migration Parity Table - UOW-1516

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.PlayerController` | `Aion.GameServer.Services.PlayerProtectionActiveTaskTaskMapSimulationService` | Controller / Protection Task Simulation | Partial | Unit Tested Simulation | Partial Parity | Simulation consumes protection task-operation plans for start/stop flows. It does not invoke a live controller or delayed stop callback. |
| `com.aionemu.gameserver.controllers.CreatureController` | `PlayerProtectionActiveTaskTaskMapSimulationService` / `PlayerProtectionActiveTaskTaskMapAdapterService` | Controller / Task Map Simulation | Partial | Unit Tested Simulation | Partial Parity | Simulation executes non-live add/cancel/cancel-all adapter operations with fake handles. No production task-map owner or lifecycle integration. |
| `com.aionemu.gameserver.model.TaskId` | task-operation plan + task-map simulation metadata | Enum / Task Key Simulation | Partial | Unit Tested Simulation | Needs Verification | Simulation uses `TaskId.PROTECTION_ACTIVE` name/ordinal from task-operation plan. Full enum mapping remains incomplete. |
| `java.util.concurrent.Future` | `IPlayerProtectionActiveTaskTaskHandle` supplied to simulation | Future / Handle Simulation | Partial | Unit Tested Simulation | Needs Verification | Fake handles observe `Cancel(false)` calls for replacement/cancel/cleanup. No Java `Future` or C# `ScheduledTask` runtime race comparison. |
| `java.util.concurrent.ConcurrentHashMap` | locked task-map adapter used by simulation | Concurrency / Collection Simulation | Partial | Unit Tested Simulation | Needs Verification | Simulation relies on adapter lock-based atomicity. Java `ConcurrentHashMap.compute/remove(key,value)` behavior under concurrency remains unverified. |
| `com.aionemu.gameserver.utils.ThreadPoolManager` | not invoked by simulation | Scheduler Dependency | Not Started | Unit Tested Boundary | Needs Verification | Simulation intentionally avoids creating scheduled tasks. Live scheduler integration remains blocked. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Create_StartWithoutExistingTaskStoresScheduledHandle` | Unit / simulation | `PlayerController.startProtectionActiveTask`, `CreatureController.addTask` | Start plan stores supplied scheduled handle with no replacement cancel. | Deterministic Java task-map operation assertion using fake handles. | No live schedule callback. |
| `Create_StartWithExistingTaskReplacesAndCancelsOldHandle` | Unit / simulation | `CreatureController.addTask` replacement branch | Existing handle is canceled with `false` before new scheduled handle remains stored. | Deterministic Java replacement assertion. | No concurrency/runtime race comparison. |
| `Create_StopWithExistingTaskCancelsAndClearsMap` | Unit / simulation | `CreatureController.cancelTask` | Stop plan removes and cancels existing handle, leaving map empty. | Deterministic Java cancel assertion. | No live task owner. |
| `Create_StopWithoutExistingTaskReportsMissingCancelNoOp` | Unit / simulation | missing `CreatureController.cancelTask` branch | Stop plan reports missing cancel as no-op and leaves map empty. | Deterministic Java no-op assertion. | No runtime comparison. |
| `Create_CancelAllAfterStartCancelsRemainingScheduledHandle` | Unit / simulation | `CreatureController.cancelAllTasks` | Optional cleanup cancels remaining stored handle and clears map. | Deterministic Java cleanup assertion. | No lifecycle hook. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Full game-server test suite is still known to have two unrelated stable failures in `GameServerConnectionInventoryExpansionUseItemTests`: `ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate` and `HandleUseItemAsync_ExpExtractMergesRestrictedRewardWithCleanupSealFlag`.
- Task-map simulation is non-live and not consumed by production protection bridge/adapter code.
- No live scheduler callback, delayed stop invocation, production task-map owner, or lifecycle cleanup hook exists.
- Fake handle cancellation remains unverified against Java `Future.cancel(false)` and C# `ScheduledTask.Cancel()` runtime races.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 1 non-live protection task-map simulation service and 1 focused test suite
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: Java runtime artifact generation, live task-map owner, live scheduler callback, live delayed stop invocation, lifecycle cleanup hook, Java/C# future race comparison, unrelated full-suite cleanup-seal failures
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: add a non-live protection scheduler-handle adapter.
- Wrap `Aion.GameServer.Utils.ScheduledTask` behind `IPlayerProtectionActiveTaskTaskHandle`.
- Validate:
  - `IsDone` mapping from scheduled task completion;
  - `Cancel(false)` invokes `ScheduledTask.Cancel()`;
  - repeated cancel after completion/done stays deterministic.
- Keep this disconnected from production protection scheduling.

## Suggested Acceptance Criteria

- Adapter is narrow and lives near the protection task-map adapter services.
- Tests use a controlled scheduled task or lightweight completion source so no long-running scheduler thread is needed.
- Tests document that C# cancellation remains cooperative and not runtime-compared to Java `Future.cancel(false)`.
- Re-run protection task-map simulation, adapter, audit, readiness, summary, task-operation, planner, side-effect, bridge, adapter, fanout, trace, executor, report, plan, and player-state tests.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Scheduler-handle adapter | new service/tests | Medium | Safe if it uses controlled tasks and does not wire production scheduling. |
| B | Java/C# future race comparison design | docs/read-only sources | Low | Could be parallel if docs ownership is isolated. |
| C | Inventory cleanup-seal failure triage | inventory item-use tests/services | Medium | Separate current full-suite blocker. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Add non-live scheduler-handle adapter and tests | new adapter service/tests, progress/handoff docs | production scheduler wiring, protection bridge execution path |

No subagent is required unless read-only future-race design is split off.

## Do Not Parallelize

- Shared scheduler implementation unless exclusively owned.
- Production protection bridge/adapter execution paths.
- Shared progress/handoff docs.
- Any live delayed stop scheduling.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1516] Add protection task map simulation`.
- Files changed in UOW-1516:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerProtectionActiveTaskTaskMapSimulationService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerProtectionActiveTaskTaskMapSimulationServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AMN-Completion.md`
- Latest prior commits:
  - `a5a66fb6b [Phase 6][UOW-1515] Add protection task map adapter prototype`
- Required startup remains:
  - read `docs/csharp-port.md`;
  - read `docs/PHASE-6-PROGRESS.md`;
  - read latest `docs/Phase-6*-Completion.md`;
  - read `docs/orchestration-rules.md`;
  - read `docs/parallelization-strategy.md`;
  - read `docs/parity-verification.md`;
  - note `docs/commit-conventions.md` is still missing.
