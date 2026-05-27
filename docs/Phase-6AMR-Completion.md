# Phase 6AMR Completion - Protection Task Map Readiness Aggregate

Date: 2026-05-27
Unit of Work: UOW-1520
Status: Complete after validation.

## Scope

Add a non-live production-readiness aggregate report for the protection task-map stack. The aggregate must combine existing readiness, audit, simulation, scheduled-handle, and lifecycle cleanup outputs into a single ordered blocker checklist before any production scheduler or lifecycle wiring is enabled.

## Completed Work

- Added `PlayerProtectionActiveTaskReadinessAggregateService`.
- Added readiness aggregate request/report/row record types.
- The aggregate consumes:
  - `PlayerProtectionActiveTaskExecutionSummary`;
  - `PlayerProtectionActiveTaskLiveReadinessReport`;
  - `PlayerProtectionActiveTaskTaskMapAuditReport`;
  - one or more `PlayerProtectionActiveTaskTaskMapSimulationReport` values;
  - `PlayerProtectionActiveTaskTaskMapLifecycleCleanupReport`;
  - scheduled-task handle adapter availability.
- Added checklist rows for:
  - branch observation;
  - visual mutation;
  - known-list cast cancellation;
  - known-list target clearing;
  - packet construction and fanout;
  - scheduler callback and task-map storage/cancellation;
  - scheduled-task handle adapter evidence;
  - lifecycle cleanup hook;
  - AI move notification;
  - production task-map owner selection;
  - Java runtime comparison blockers.
- No production lifecycle hooks, scheduler wiring, protection bridge execution, socket fanout, AI move notification, or task-map owner was added.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --no-restore --filter "FullyQualifiedName~PlayerProtectionActiveTaskReadinessAggregateServiceTests"`.
- Result: passed 3 tests.
- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --no-restore --filter "FullyQualifiedName~PlayerProtectionActiveTaskReadinessAggregateServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskMapLifecycleCleanupServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskScheduledTaskHandleAdapterTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskMapSimulationServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskMapAdapterServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskMapAuditServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskLiveReadinessServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskExecutionSummaryServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskOperationPlanServiceTests|FullyQualifiedName~PlayerProtectionAttackUtilRecipientPlannerServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSideEffectOperationPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskExecutionBridgeServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSightedRecipientSocketExecutorServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSightedRecipientTraceServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskReportServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskFanoutPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskAdapterServiceTests|FullyQualifiedName~PlayerStateTests"`.
- Result: passed 110 tests.

## Migration Parity Table - UOW-1520

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.PlayerController` | `Aion.GameServer.Services.PlayerProtectionActiveTaskReadinessAggregateService` | Controller / Readiness Aggregate | Partial | Unit Tested Metadata | Partial Parity | Aggregate combines start/stop protection summary and readiness facts. It does not execute live scheduler, packet fanout, known-list mutations, or AI move notification. |
| `com.aionemu.gameserver.controllers.CreatureController` | `PlayerProtectionActiveTaskReadinessAggregateService` plus task-map audit/simulation/cleanup reports | Controller / Task Map Readiness | Partial | Unit Tested Metadata | Partial Parity | Aggregate records task-map storage, cancellation, owner, and lifecycle cleanup blockers. Production controller task map is still not wired. |
| `com.aionemu.gameserver.utils.ThreadPoolManager` | `PlayerProtectionActiveTaskReadinessAggregateService` / `PlayerProtectionActiveTaskScheduledTaskHandleAdapter` | Scheduler / Readiness Checklist | Partial | Unit Tested Metadata | Needs Verification | Aggregate records scheduled-handle adapter availability as non-live evidence. It does not create production scheduled protection callbacks. |
| `java.util.concurrent.Future` / `ScheduledFuture` | `IPlayerProtectionActiveTaskTaskHandle` and scheduled-handle adapter readiness row | Future / Cancellation Readiness | Partial | Unit Tested Metadata | Needs Verification | Aggregate keeps Java `Future.cancel(false)` runtime comparison as a blocker. No Java runtime artifact is available locally. |
| `java.util.concurrent.ConcurrentHashMap` | task-map adapter reports consumed by readiness aggregate | Concurrency / Collection Readiness | Partial | Unit Tested Metadata | Needs Verification | Aggregate consumes non-live lock-based simulation evidence but does not verify Java concurrent map race behavior. |
| `com.aionemu.gameserver.model.TaskId` | aggregate task-map/readiness metadata | Enum / Task Key Readiness | Partial | Unit Tested Metadata | Needs Verification | Aggregate preserves `PROTECTION_ACTIVE` task-map checklist context; full Java enum mapping remains incomplete. |
| `com.aionemu.gameserver.utils.PacketSendUtility` | aggregate packet-fanout readiness rows | Utility / Packet Fanout Readiness | Partial | Unit Tested Metadata | Partial Parity | Aggregate records disabled `broadcastToSightedPlayers` fanout as a blocker. No live socket fanout is enabled. |
| `com.aionemu.gameserver.taskmanager.tasks.MovementNotifyTask` | aggregate AI move readiness rows | AI / Movement Notification Readiness | Partial | Unit Tested Metadata | Needs Verification | Aggregate records disabled `notifyAIOnMove`/movement notification integration as a blocker for spawned stop. No live AI move notification is wired. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Create_StartAggregatesReadinessAuditSimulationAndRuntimeBlockers` | Unit / readiness aggregate metadata | `PlayerController.startProtectionActiveTask`, `CreatureController.addTask`, `ThreadPoolManager.schedule` | Start aggregate records known-list, packet fanout, scheduler, owner, runtime comparison, storage simulation, and scheduled-handle rows. | Deterministic C# aggregate over Java-shaped non-live reports. | No Java runtime comparison or live scheduler. |
| `Create_StopAggregatesCancellationAndAiMoveBlockers` | Unit / readiness aggregate metadata | `PlayerController.stopProtectionActiveTask`, `CreatureController.cancelTask`, `MovementNotifyTask.add` | Stop aggregate records cancellation simulation and AI move blocker while not inventing start-only known-list blockers. | Deterministic C# aggregate over Java-shaped stop reports. | No live AI move notification or socket fanout. |
| `Create_LifecycleCleanupPrerequisitesRemainLiveBlockers` | Unit / readiness aggregate metadata | `CreatureController.onDelete -> cancelAllTasks` | Lifecycle cleanup prerequisites remain blockers and Java runtime comparison stays Needs Verification. | Deterministic C# aggregate over cleanup planner report. | No production lifecycle hook or Java concurrency runtime comparison. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Full game-server test suite is still known to have two unrelated stable failures in `GameServerConnectionInventoryExpansionUseItemTests`: `ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate` and `HandleUseItemAsync_ExpExtractMergesRestrictedRewardWithCleanupSealFlag`.
- Readiness aggregate is metadata-only and not consumed by production code.
- No production C# task-map owner, delete/logout hook, scheduler callback, delayed stop invocation, socket fanout, known-list mutation, or AI move notification integration exists.
- Java `Future.cancel(false)`, `ScheduledFuture`, and `ConcurrentHashMap` race behavior remains unverified by runtime comparison.

## Summary Metrics

- Total Java artifacts discovered: 8 grouped artifact rows in this unit
- Total artifacts ported: 1 non-live readiness aggregate service and 1 focused test suite
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 8 grouped rows
- Total blocked artifacts: Java runtime artifact generation, live task-map owner, production lifecycle hook, live scheduler callback, live delayed stop invocation, socket fanout, known-list mutations, AI move notification, Java/C# future/concurrent-map race comparison, unrelated full-suite cleanup-seal failures
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: add a non-live production task-map owner selection/design report.
- Compare controller-owned versus player-owned C# storage options against Java `CreatureController.tasks`.
- Identify lifecycle ownership, locking, keying, replacement/cancel ordering, and cleanup requirements.
- Emit a conservative recommendation without wiring production scheduling.

## Suggested Acceptance Criteria

- Report rows cover Java `CreatureController.tasks`, `hasTask`, `hasScheduledTask`, `getAndRemoveTask`, `cancelTask`, `cancelTaskIfPresent`, `addTask`, `cancelAllTasks`, and `onDelete`.
- C# candidate options include at least controller-owned, player-owned, and external service-owned storage.
- Recommendation explains why any selected owner best preserves Java lifecycle and concurrency semantics.
- Tests verify the recommendation stays non-live and keeps scheduler/lifecycle hooks blocked.
- Re-run protection readiness aggregate, lifecycle cleanup, scheduled-handle, task-map simulation, adapter, audit, readiness, summary, task-operation, planner, side-effect, bridge, fanout, trace, executor, report, plan, and player-state tests.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Task-map owner selection/design report | new service/tests | Medium | Safe if non-live and no production hooks are wired. |
| B | Java task-map runtime comparison design | docs/read-only Java sources | Low | Can be parallel if docs ownership is isolated. |
| C | Inventory cleanup-seal failure triage | inventory item-use tests/services | Medium | Separate current full-suite blocker. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Add task-map owner selection report and tests | new service/tests, progress/handoff docs | production lifecycle hooks, scheduler wiring, protection bridge execution path |

No subagent is required unless read-only Java runtime comparison design is split off.

## Do Not Parallelize

- Production lifecycle/delete/logout hooks.
- Shared scheduler implementation.
- Production protection bridge/adapter execution paths.
- Shared progress/handoff docs.
- Task-map owner service and tests with another task-map implementation task.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1520] Add protection task map readiness aggregate`.
- Files changed in UOW-1520:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerProtectionActiveTaskReadinessAggregateService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerProtectionActiveTaskReadinessAggregateServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AMR-Completion.md`
- Latest prior commits:
  - `58a3e9bfc [Phase 6][UOW-1519] Add protection task map lifecycle cleanup`
  - `cb7f315f9 [Phase 6][UOW-1518] Compose protection scheduled handle simulation`
  - `d9243b310 [Phase 6][UOW-1517] Add protection scheduled task handle adapter`
- Required startup remains:
  - read `docs/csharp-port.md`;
  - read `docs/PHASE-6-PROGRESS.md`;
  - read latest `docs/Phase-6*-Completion.md`;
  - read `docs/orchestration-rules.md`;
  - read `docs/parallelization-strategy.md`;
  - read `docs/parity-verification.md`;
  - note `docs/commit-conventions.md` is still missing.
