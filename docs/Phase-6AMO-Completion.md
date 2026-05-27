# Phase 6AMO Completion - Protection Scheduled Task Handle Adapter

Date: 2026-05-27
Unit of Work: UOW-1517
Status: Complete after validation.

## Scope

Add a non-live protection scheduler-handle adapter that wraps `Aion.GameServer.Utils.ScheduledTask` behind `IPlayerProtectionActiveTaskTaskHandle`.

## Completed Work

- Added `PlayerProtectionActiveTaskScheduledTaskHandleAdapter`.
- `IsDone` maps to `ScheduledTask.Completion.IsCompleted`.
- `Cancel(bool mayInterruptIfRunning)` forwards to `ScheduledTask.Cancel()`.
- The interrupt flag is intentionally ignored because the Java protection task-map path calls `Future.cancel(false)` and the current C# cancellation primitive is cooperative.
- Added tests with controlled `ThreadPoolManager` scheduled tasks and `NullLogger`.
- No production protection scheduling, bridge execution, or task-map owner was wired.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --no-restore --filter "FullyQualifiedName~PlayerProtectionActiveTaskScheduledTaskHandleAdapterTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskMapSimulationServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskMapAdapterServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskMapAuditServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskLiveReadinessServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskExecutionSummaryServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskOperationPlanServiceTests|FullyQualifiedName~PlayerProtectionAttackUtilRecipientPlannerServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSideEffectOperationPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskExecutionBridgeServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSightedRecipientSocketExecutorServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSightedRecipientTraceServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskReportServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskFanoutPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskAdapterServiceTests|FullyQualifiedName~PlayerStateTests"`.
- Result: passed 101 tests.

## Migration Parity Table - UOW-1517

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.utils.ThreadPoolManager` | `Aion.GameServer.Services.PlayerProtectionActiveTaskScheduledTaskHandleAdapter` / `Aion.GameServer.Utils.ThreadPoolManager` | Scheduler / Handle Adapter | Partial | Unit Tested Boundary | Needs Verification | Adapter wraps C# `ScheduledTask` for task-map prototype use. It does not schedule protection tasks or compare Java runtime scheduling. |
| `java.util.concurrent.ScheduledFuture` | `PlayerProtectionActiveTaskScheduledTaskHandleAdapter` | Future / Scheduled Handle Adapter | Partial | Unit Tested Boundary | Needs Verification | `IsDone` maps to `Completion.IsCompleted`; `Cancel` maps to C# cooperative cancellation. Java `ScheduledFuture` runtime semantics are not compared. |
| `java.util.concurrent.Future` | `IPlayerProtectionActiveTaskTaskHandle` / scheduled-task wrapper | Future / Cancellation Adapter | Partial | Unit Tested Boundary | Needs Verification | Wrapper ignores the interrupt flag and forwards to `ScheduledTask.Cancel()`. Java `Future.cancel(false)` race behavior remains unverified. |
| `com.aionemu.gameserver.controllers.CreatureController` | task-map adapter + scheduled-task handle adapter | Controller / Task Map Dependency | Partial | Unit Tested Boundary | Needs Verification | The future task-map adapter can now accept wrapped C# scheduled tasks, but no production controller task map or lifecycle cleanup exists. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Cancel_ForwardsToScheduledTaskCancelAndMarksHandleDone` | Unit / adapter boundary | Java `Future.cancel(false)`, C# `ScheduledTask.Cancel()` source review | Long-delay scheduled task can be canceled through the wrapper, does not run, and becomes done. | C# boundary behavior evidence only. | No Java runtime comparison. |
| `IsDone_ReflectsCompletedScheduledTask` | Unit / adapter boundary | Java `Future.isDone`, C# `Task.IsCompleted` source review | Completed scheduled task is reported done and later cancel returns false. | C# boundary behavior evidence only. | No Java runtime comparison. |
| `Cancel_IgnoresMayInterruptFlagLikeJavaProtectionCancelFalseBoundary` | Unit / adapter boundary | Protection task map uses `Future.cancel(false)` | Wrapper ignores the interrupt flag and still forwards cancel. | Deterministic C# adapter assertion. | Java interrupt semantics are not modeled because protection path uses false. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Full game-server test suite is still known to have two unrelated stable failures in `GameServerConnectionInventoryExpansionUseItemTests`: `ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate` and `HandleUseItemAsync_ExpExtractMergesRestrictedRewardWithCleanupSealFlag`.
- Scheduler-handle adapter is not consumed by production protection bridge/adapter code.
- No live scheduler callback, delayed stop invocation, production task-map owner, or lifecycle cleanup hook exists.
- C# scheduled task cancellation is cooperative; equivalence to Java `Future.cancel(false)` remains unverified by Java runtime comparison.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped artifact rows in this unit
- Total artifacts ported: 1 non-live scheduled-task handle adapter and 1 focused test suite
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: Java runtime artifact generation, live task-map owner, live scheduler callback, live delayed stop invocation, lifecycle cleanup hook, Java/C# future race comparison, unrelated full-suite cleanup-seal failures
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: compose `PlayerProtectionActiveTaskScheduledTaskHandleAdapter` into the non-live task-map simulation.
- Use an opt-in controlled C# scheduled task input to prove the simulation can store/cancel a wrapped `ScheduledTask`.
- Keep the flow disconnected from production protection scheduling.

## Suggested Acceptance Criteria

- Simulation test stores a wrapped C# `ScheduledTask` through the start plan.
- Simulation test replaces/cancels an existing wrapped C# `ScheduledTask`.
- Simulation test cancels a wrapped C# `ScheduledTask` through stop/cancel plan.
- Scheduled callback does not run in cancellation tests.
- Re-run protection scheduled-handle, task-map simulation, adapter, audit, readiness, summary, task-operation, planner, side-effect, bridge, adapter, fanout, trace, executor, report, plan, and player-state tests.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Compose scheduled handle into simulation tests | simulation tests or small helper | Medium | Safe if no production scheduling is wired. |
| B | Java/C# future race comparison design | docs/read-only sources | Low | Can be parallel if docs ownership is isolated. |
| C | Inventory cleanup-seal failure triage | inventory item-use tests/services | Medium | Separate current full-suite blocker. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Compose wrapped scheduled handle into simulation tests | simulation service/tests if needed, progress/handoff docs | production scheduler wiring, protection bridge execution path |

No subagent is required unless read-only future-race design is split off.

## Do Not Parallelize

- Shared scheduler implementation unless exclusively owned.
- Production protection bridge/adapter execution paths.
- Shared progress/handoff docs.
- Any live delayed stop scheduling.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1517] Add protection scheduled task handle adapter`.
- Files changed in UOW-1517:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerProtectionActiveTaskScheduledTaskHandleAdapter.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerProtectionActiveTaskScheduledTaskHandleAdapterTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AMO-Completion.md`
- Latest prior commits:
  - `df848085b [Phase 6][UOW-1516] Add protection task map simulation`
- Required startup remains:
  - read `docs/csharp-port.md`;
  - read `docs/PHASE-6-PROGRESS.md`;
  - read latest `docs/Phase-6*-Completion.md`;
  - read `docs/orchestration-rules.md`;
  - read `docs/parallelization-strategy.md`;
  - read `docs/parity-verification.md`;
  - note `docs/commit-conventions.md` is still missing.
