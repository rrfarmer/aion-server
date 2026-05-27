# Phase 6AMP Completion - Protection Scheduled Handle Simulation Composition

Date: 2026-05-27
Unit of Work: UOW-1518
Status: Complete after validation.

## Scope

Compose `PlayerProtectionActiveTaskScheduledTaskHandleAdapter` into the non-live task-map simulation through controlled test scenarios, proving wrapped C# `ScheduledTask` handles can be stored/replaced/canceled without wiring production protection scheduling.

## Completed Work

- Extended `PlayerProtectionActiveTaskTaskMapSimulationServiceTests`.
- Added controlled C# `ThreadPoolManager` scheduled-task scenarios using `NullLogger`.
- Added tests proving simulation can:
  - store a wrapped C# `ScheduledTask` through a start plan;
  - replace and cancel an existing wrapped C# `ScheduledTask`;
  - cancel an existing wrapped C# `ScheduledTask` through a stop plan.
- Cancellation tests confirm scheduled callbacks do not run when canceled through the simulation path.
- No production protection scheduling, bridge execution, task-map owner, or lifecycle hook was wired.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --no-restore --filter "FullyQualifiedName~PlayerProtectionActiveTaskScheduledTaskHandleAdapterTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskMapSimulationServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskMapAdapterServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskMapAuditServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskLiveReadinessServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskExecutionSummaryServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskOperationPlanServiceTests|FullyQualifiedName~PlayerProtectionAttackUtilRecipientPlannerServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSideEffectOperationPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskExecutionBridgeServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSightedRecipientSocketExecutorServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSightedRecipientTraceServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskReportServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskFanoutPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskAdapterServiceTests|FullyQualifiedName~PlayerStateTests"`.
- Result: passed 104 tests.

## Migration Parity Table - UOW-1518

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.PlayerController` | `PlayerProtectionActiveTaskTaskMapSimulationServiceTests` | Controller / Protection Task Simulation Tests | Partial | Unit Tested Simulation | Partial Parity | Tests drive start/stop task-operation plans through simulation with wrapped C# scheduled-task handles. No live controller callback is invoked. |
| `com.aionemu.gameserver.controllers.CreatureController` | `PlayerProtectionActiveTaskTaskMapSimulationService` / tests | Controller / Task Map Simulation Tests | Partial | Unit Tested Simulation | Partial Parity | Tests verify Java add/replace/cancel behavior can operate on wrapped C# scheduled-task handles. No production task-map owner or lifecycle hook exists. |
| `com.aionemu.gameserver.utils.ThreadPoolManager` | `Aion.GameServer.Utils.ThreadPoolManager` + `PlayerProtectionActiveTaskScheduledTaskHandleAdapter` | Scheduler / Test Boundary | Partial | Unit Tested Boundary | Needs Verification | Tests use controlled C# scheduled tasks to ensure callbacks do not run after cancellation. Java scheduler runtime behavior is not compared. |
| `java.util.concurrent.Future` / `ScheduledFuture` | `PlayerProtectionActiveTaskScheduledTaskHandleAdapter` in simulation tests | Future / Handle Boundary | Partial | Unit Tested Boundary | Needs Verification | Wrapped C# handles are stored/replaced/canceled by the simulation. Java `Future.cancel(false)` race behavior remains unverified. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Create_StartStoresWrappedScheduledTaskHandle` | Unit / simulation boundary | `PlayerController.startProtectionActiveTask`, `CreatureController.addTask` | Start simulation stores a wrapped C# scheduled task and does not run the delayed callback. | C# boundary evidence over Java-shaped task-map operation. | No Java runtime comparison. |
| `Create_StartReplacementCancelsExistingWrappedScheduledTaskHandle` | Unit / simulation boundary | `CreatureController.addTask` replacement branch | Existing wrapped scheduled task is canceled during replacement and callback does not run. | C# boundary evidence over Java-shaped replacement operation. | No Java runtime race comparison. |
| `Create_StopCancelsExistingWrappedScheduledTaskHandle` | Unit / simulation boundary | `CreatureController.cancelTask` | Stop simulation cancels existing wrapped scheduled task and clears the map. | C# boundary evidence over Java-shaped cancel operation. | No production controller task map. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Full game-server test suite is still known to have two unrelated stable failures in `GameServerConnectionInventoryExpansionUseItemTests`: `ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate` and `HandleUseItemAsync_ExpExtractMergesRestrictedRewardWithCleanupSealFlag`.
- Scheduled-handle simulation remains test-only and is not consumed by production protection bridge/adapter code.
- No live scheduler callback, delayed stop invocation, production task-map owner, or lifecycle cleanup hook exists.
- Java `Future.cancel(false)` race behavior remains unverified by runtime comparison.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped artifact rows in this unit
- Total artifacts ported: scheduled-handle simulation composition tests over existing non-live services
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: Java runtime artifact generation, live task-map owner, live scheduler callback, live delayed stop invocation, lifecycle cleanup hook, Java/C# future race comparison, unrelated full-suite cleanup-seal failures
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: add a non-live protection task-map lifecycle cleanup planner/report.
- Compose Java `CreatureController.cancelAllTasks` semantics with player/controller deletion or logout prerequisites.
- Keep it metadata/report only and do not wire production lifecycle hooks.

## Suggested Acceptance Criteria

- Planner/report identifies Java cleanup trigger `CreatureController.onDelete -> cancelAllTasks`.
- It composes adapter cancel-all semantics and reports pending protection task cancellation.
- Tests cover cleanup with no tasks, cleanup with a pending protection task, and cleanup after replacement.
- Remaining prerequisites explicitly include production owner selection, deletion/logout hook, and Java runtime race comparison.
- Re-run protection scheduled-handle, task-map simulation, adapter, audit, readiness, summary, task-operation, planner, side-effect, bridge, adapter, fanout, trace, executor, report, plan, and player-state tests.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Lifecycle cleanup planner/report | new service/tests | Medium | Safe if metadata-only and no lifecycle hook is wired. |
| B | Java lifecycle cleanup analysis | read-only `CreatureController`, player logout/delete sources | Low | Can be parallel if no docs overlap. |
| C | Inventory cleanup-seal failure triage | inventory item-use tests/services | Medium | Separate current full-suite blocker. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Add lifecycle cleanup planner/report and tests | new service/tests, progress/handoff docs | production lifecycle hooks, scheduler wiring, protection bridge execution path |

No subagent is required unless read-only lifecycle analysis is split off.

## Do Not Parallelize

- Production lifecycle/delete/logout hooks.
- Shared scheduler implementation.
- Production protection bridge/adapter execution paths.
- Shared progress/handoff docs.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1518] Compose protection scheduled handle simulation`.
- Files changed in UOW-1518:
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerProtectionActiveTaskTaskMapSimulationServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AMP-Completion.md`
- Latest prior commits:
  - `d9243b310 [Phase 6][UOW-1517] Add protection scheduled task handle adapter`
- Required startup remains:
  - read `docs/csharp-port.md`;
  - read `docs/PHASE-6-PROGRESS.md`;
  - read latest `docs/Phase-6*-Completion.md`;
  - read `docs/orchestration-rules.md`;
  - read `docs/parallelization-strategy.md`;
  - read `docs/parity-verification.md`;
  - note `docs/commit-conventions.md` is still missing.
