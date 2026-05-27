# Phase 6AMX Completion - Protection Scheduler Callback Readiness Aggregate Composition

Date: 2026-05-27
Unit of Work: UOW-1526
Status: Complete after validation.

## Scope

Compose the non-live `PlayerProtectionActiveTaskSchedulerCallbackPlanService` output into `PlayerProtectionActiveTaskReadinessAggregateService`, preserving Java source breadcrumbs for `PlayerController.startProtectionActiveTask`, `ThreadPoolManager.schedule`, and `CreatureController.addTask` while keeping live scheduler execution blocked.

## Completed Work

- Updated `PlayerProtectionActiveTaskReadinessAggregateRequest` to accept an optional `PlayerProtectionActiveTaskSchedulerCallbackPlan`.
- Aggregate now emits scheduler callback plan rows for:
  - start-branch observation;
  - 60000 ms delayed-stop schedule metadata;
  - callback target `stopProtectionActiveTask`;
  - task-map storage target;
  - Java runtime comparison blocker;
  - already-protected branch skip metadata.
- Aggregate row notes explicitly include:
  - `DelayMilliseconds=60000`;
  - `InvokesScheduler=False`;
  - `InvokesCallback=False`.
- `HasStartStorageEvidence` now treats scheduler callback task-map storage metadata as non-live storage evidence.
- Added a readiness aggregate regression for the already-protected start guard so the aggregate records a skipped scheduler callback and no schedule-call row.
- No C# `ThreadPoolManager.Schedule`, production controller task-map owner, lifecycle hook, socket fanout, known-list mutation, AI move notification, or protection bridge execution was invoked.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --no-restore --filter "FullyQualifiedName~PlayerProtectionActiveTaskReadinessAggregateServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSchedulerCallbackPlanServiceTests"`.
- Result: passed 7 tests.
- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --no-restore --filter "FullyQualifiedName~PlayerProtectionActiveTaskSchedulerCallbackPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskControllerTaskMapOwnerPrototypeServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskMapOwnerSelectionServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskReadinessAggregateServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskMapLifecycleCleanupServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskScheduledTaskHandleAdapterTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskMapSimulationServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskMapAdapterServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskMapAuditServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskLiveReadinessServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskExecutionSummaryServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskOperationPlanServiceTests|FullyQualifiedName~PlayerProtectionAttackUtilRecipientPlannerServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSideEffectOperationPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskExecutionBridgeServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSightedRecipientSocketExecutorServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSightedRecipientTraceServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskReportServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskFanoutPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskAdapterServiceTests|FullyQualifiedName~PlayerStateTests"`.
- Result: passed 123 tests.

## Migration Parity Table - UOW-1526

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.PlayerController` | `Aion.GameServer.Services.PlayerProtectionActiveTaskReadinessAggregateService` / scheduler callback plan composition | Controller / Readiness Aggregate | Partial | Unit Tested Metadata | Partial Parity | Aggregate records start-branch scheduler metadata, 60000 ms delayed-stop callback target, and already-protected skip evidence. It does not invoke the C# scheduler or delayed callback. |
| `com.aionemu.gameserver.controllers.CreatureController` | `PlayerProtectionActiveTaskReadinessAggregateService` scheduler task-map rows | Controller / Task Map Readiness | Partial | Unit Tested Metadata | Needs Verification | Aggregate records `addTask(TaskId.PROTECTION_ACTIVE, scheduledFuture)` storage metadata through the scheduler callback plan. No production controller owner is wired. |
| `com.aionemu.gameserver.utils.ThreadPoolManager` | aggregate scheduler callback rows | Scheduler / Callback Readiness | Partial | Unit Tested Metadata | Needs Verification | Aggregate records `ThreadPoolManager.schedule(..., 60000)` metadata and explicitly blocks live scheduler execution. No runtime scheduler comparison exists. |
| `com.aionemu.gameserver.model.TaskId` | aggregate scheduler storage metadata | Enum / Task Key Readiness | Partial | Unit Tested Metadata | Needs Verification | Aggregate metadata still targets only `PROTECTION_ACTIVE`; full Java enum behavior remains incomplete. |
| `java.util.concurrent.ScheduledFuture` / `Future` | aggregate scheduler runtime blocker rows | Future / Callback Readiness | Partial | Unit Tested Metadata | Needs Verification | Aggregate carries future cancellation/runtime-comparison blockers. Java `ScheduledFuture`/`Future.cancel(false)` behavior remains unverified. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Create_StartAggregatesReadinessAuditSimulationAndRuntimeBlockers` | Unit / readiness aggregate metadata | `PlayerController.startProtectionActiveTask`, `ThreadPoolManager.schedule`, `CreatureController.addTask`, scheduler callback plan | Start aggregate includes delayed-stop scheduler metadata, `InvokesScheduler = false`, and a live scheduler blocker. | Deterministic C# aggregate over reviewed Java-source-shaped metadata. | No Java runtime scheduler/future comparison. |
| `Create_AlreadyProtectedStartIncludesSkippedSchedulerCallbackMetadata` | Unit / readiness aggregate metadata | `PlayerController.startProtectionActiveTask` already-protected guard | Already-protected start aggregate records skipped scheduler callback metadata and no schedule-call row. | Deterministic Java source-derived guard assertion. | No runtime comparison. |
| `Create_StopAggregatesCancellationAndAiMoveBlockers` | Unit / readiness aggregate metadata | `PlayerController.stopProtectionActiveTask`, `CreatureController.cancelTask` | Existing stop aggregate remains compatible with the new optional scheduler callback plan input. | Regression coverage through focused and protection-slice test runs. | No live AI move notification or socket fanout. |
| `Create_LifecycleCleanupPrerequisitesRemainLiveBlockers` | Unit / readiness aggregate metadata | `CreatureController.onDelete -> cancelAllTasks` | Existing lifecycle aggregate remains compatible with scheduler callback plan composition. | Regression coverage through focused and protection-slice test runs. | No production lifecycle hook. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Full game-server test suite is still known to have two unrelated stable failures in `GameServerConnectionInventoryExpansionUseItemTests`: `ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate` and `HandleUseItemAsync_ExpExtractMergesRestrictedRewardWithCleanupSealFlag`.
- Scheduler callback plan composition is metadata-only and is not consumed by production code.
- No production C# controller task-map owner, delete/logout hook, scheduler callback, delayed stop invocation, socket fanout, known-list mutation, or AI move notification integration exists.
- Java `Future.cancel(false)`, `ScheduledFuture`, and `ConcurrentHashMap` race behavior remains unverified by runtime comparison.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: scheduler callback plan composition into 1 non-live readiness aggregate service and updated focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: Java runtime artifact generation, production controller owner wiring, production lifecycle hook, live scheduler callback, live delayed stop invocation, socket fanout, known-list mutations, AI move notification, Java/C# future/concurrent-map race comparison, unrelated full-suite cleanup-seal failures
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: add a non-live delayed-stop callback execution preview/report.
- Compose scheduler callback metadata with a stop-path `PlayerProtectionActiveTaskTaskOperationPlan`.
- Use the owner prototype to model `cancelTask(TaskId.PROTECTION_ACTIVE)` cancellation metadata.
- Keep the preview non-live: do not invoke the callback, scheduler, socket fanout, known-list mutation, or AI move notification.

## Suggested Acceptance Criteria

- New preview/report records Java callback source `this::stopProtectionActiveTask`.
- Preview composes a stop-path task-operation plan and owner-prototype cancellation evidence.
- Preview distinguishes metadata-only delayed callback planning from actual scheduled callback execution.
- Tests cover:
  - planned delayed callback preview with stored owner task;
  - missing owner task/no-op cancellation branch;
  - skipped preview when scheduler plan did not schedule delayed stop.
- Existing scheduler callback plan, owner prototype, readiness aggregate, lifecycle cleanup, scheduled-handle, task-map simulation, adapter, audit, readiness, summary, task-operation, planner, side-effect, bridge, fanout, trace, executor, report, plan, and player-state tests continue to pass.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Delayed-stop callback execution preview | new preview service/tests | Medium | Safe if no production callback or scheduler invocation is wired. |
| B | Java task-map runtime comparison design | docs/read-only Java sources | Low | Can be parallel if docs ownership is isolated. |
| C | Inventory cleanup-seal failure triage | inventory item-use tests/services | Medium | Separate current full-suite blocker. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Add delayed-stop callback preview/report and tests | new `PlayerProtectionActiveTaskDelayedStopCallbackPreviewService.cs`, new tests, progress/handoff docs | production lifecycle hooks, actual scheduler invocation, protection bridge execution path, shared scheduler implementation |

No subagent is required unless choosing read-only Java analysis in parallel; shared docs should remain orchestrator-owned.

## Do Not Parallelize

- Production lifecycle/delete/logout hooks.
- Shared scheduler implementation.
- Production protection bridge/adapter execution paths.
- Shared progress/handoff docs.
- Any task editing the same new preview service/test pair.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1526] Compose protection scheduler callback into readiness aggregate`.
- Files changed in UOW-1526:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerProtectionActiveTaskReadinessAggregateService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerProtectionActiveTaskReadinessAggregateServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AMX-Completion.md`
- Latest prior commits:
  - `18154a1b4 [Phase 6][UOW-1525] Add protection scheduler callback plan`
  - `76c5c4ed1 [Phase 6][UOW-1524] Compose protection owner prototype into readiness aggregate`
  - `a2f77cf6f [Phase 6][UOW-1523] Add protection controller task map owner prototype`
- Required startup remains:
  - read `docs/csharp-port.md`;
  - read `docs/PHASE-6-PROGRESS.md`;
  - read latest `docs/Phase-6*-Completion.md`;
  - read `docs/orchestration-rules.md`;
  - read `docs/parallelization-strategy.md`;
  - read `docs/parity-verification.md`;
  - note `docs/commit-conventions.md` is still missing.
