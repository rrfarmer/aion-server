# Phase 6AMW Completion - Protection Scheduler Callback Invocation Plan

Date: 2026-05-27
Unit of Work: UOW-1525
Status: Complete after validation.

## Scope

Add a non-live scheduler callback invocation plan for the controller-owned protection owner prototype, modeling Java `ThreadPoolManager.schedule(this::stopProtectionActiveTask, 60000)` as metadata only and proving the callback remains disconnected from production execution.

## Completed Work

- Added `PlayerProtectionActiveTaskSchedulerCallbackPlanService`.
- Added scheduler callback plan request/report/row record types.
- The plan records:
  - start-branch observation;
  - missing owner prototype blocker;
  - 60000 ms delayed stop schedule metadata;
  - callback target `stopProtectionActiveTask`;
  - task-map storage target;
  - runtime comparison blocker.
- The plan explicitly reports:
  - `InvokesScheduler = false`;
  - `InvokesCallback = false`;
  - `IsLive = false`.
- No C# `ThreadPoolManager.Schedule`, production controller task-map owner, lifecycle hook, socket fanout, known-list mutation, AI move notification, or protection bridge execution was invoked.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --no-restore --filter "FullyQualifiedName~PlayerProtectionActiveTaskSchedulerCallbackPlanServiceTests"`.
- Result: passed 3 tests.
- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --no-restore --filter "FullyQualifiedName~PlayerProtectionActiveTaskSchedulerCallbackPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskControllerTaskMapOwnerPrototypeServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskMapOwnerSelectionServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskReadinessAggregateServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskMapLifecycleCleanupServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskScheduledTaskHandleAdapterTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskMapSimulationServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskMapAdapterServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskMapAuditServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskLiveReadinessServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskExecutionSummaryServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskOperationPlanServiceTests|FullyQualifiedName~PlayerProtectionAttackUtilRecipientPlannerServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSideEffectOperationPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskExecutionBridgeServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSightedRecipientSocketExecutorServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSightedRecipientTraceServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskReportServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskFanoutPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskAdapterServiceTests|FullyQualifiedName~PlayerStateTests"`.
- Result: passed 122 tests.

## Migration Parity Table - UOW-1525

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.PlayerController` | `Aion.GameServer.Services.PlayerProtectionActiveTaskSchedulerCallbackPlanService` | Controller / Scheduler Callback Plan | Partial | Unit Tested Metadata | Partial Parity | Plan models `startProtectionActiveTask` delayed stop scheduling and already-protected skip. It does not invoke C# scheduler or callback. |
| `com.aionemu.gameserver.controllers.CreatureController` | `PlayerProtectionActiveTaskSchedulerCallbackPlanService` | Controller / Task Map Storage Plan | Partial | Unit Tested Metadata | Needs Verification | Plan records `addTask(TaskId.PROTECTION_ACTIVE, scheduledFuture)` storage target. No production controller owner is wired. |
| `com.aionemu.gameserver.utils.ThreadPoolManager` | `PlayerProtectionActiveTaskSchedulerCallbackPlanService` | Scheduler / Callback Plan | Partial | Unit Tested Metadata | Needs Verification | Plan records 60000 ms schedule metadata only. It intentionally does not call `ThreadPoolManager.Schedule`. |
| `com.aionemu.gameserver.model.TaskId` | scheduler callback plan metadata | Enum / Task Key Plan | Partial | Unit Tested Metadata | Needs Verification | Plan targets `PROTECTION_ACTIVE` through existing plan/owner metadata. Full enum behavior remains incomplete. |
| `java.util.concurrent.ScheduledFuture` / `Future` | scheduler callback plan runtime blocker rows | Future / Callback Plan | Partial | Unit Tested Metadata | Needs Verification | Plan records scheduled future storage and runtime comparison blocker. Java/C# future behavior remains unverified. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Create_StartWithOwnerPrototypeRecordsSixtySecondDelayedStopWithoutInvokingScheduler` | Unit / scheduler callback metadata | `PlayerController.startProtectionActiveTask`, `ThreadPoolManager.schedule`, `CreatureController.addTask` | Start plan records 60000 ms delayed stop, callback target, task-map storage, and no scheduler/callback invocation. | Deterministic C# metadata over reviewed Java source. | No Java runtime scheduler comparison. |
| `Create_StartWithoutOwnerPrototypeBlocksBeforeScheduleMetadata` | Unit / scheduler callback metadata | `PlayerController.startProtectionActiveTask`, `CreatureController.addTask` | Missing owner prototype blocks schedule metadata before live enablement. | Conservative blocker assertion. | No production owner. |
| `Create_AlreadyProtectedStartSkipsSchedulerCallbackLikeJavaGuard` | Unit / scheduler callback metadata | `PlayerController.startProtectionActiveTask` guard | Already-protected branch skips scheduler metadata like Java returns before scheduling. | Deterministic Java source-derived assertion. | No runtime comparison. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Full game-server test suite is still known to have two unrelated stable failures in `GameServerConnectionInventoryExpansionUseItemTests`: `ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate` and `HandleUseItemAsync_ExpExtractMergesRestrictedRewardWithCleanupSealFlag`.
- Scheduler callback plan is metadata-only and is not consumed by production code or the readiness aggregate yet.
- No production C# controller task-map owner, delete/logout hook, scheduler callback, delayed stop invocation, socket fanout, known-list mutation, or AI move notification integration exists.
- Java `Future.cancel(false)`, `ScheduledFuture`, and `ConcurrentHashMap` race behavior remains unverified by runtime comparison.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 non-live scheduler callback plan service and 1 focused test suite
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: Java runtime artifact generation, production controller owner wiring, production lifecycle hook, live scheduler callback, live delayed stop invocation, socket fanout, known-list mutations, AI move notification, Java/C# future/concurrent-map race comparison, unrelated full-suite cleanup-seal failures
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: compose `PlayerProtectionActiveTaskSchedulerCallbackPlanService` into the readiness aggregate.
- Add optional scheduler callback plan input to the aggregate request.
- Add aggregate rows that record 60000 ms delayed-stop metadata and still block live scheduler execution.
- Keep production scheduling disabled.

## Suggested Acceptance Criteria

- Aggregate request accepts a scheduler callback plan.
- Aggregate rows include delayed-stop metadata and `InvokesScheduler = false` evidence.
- Aggregate tests assert callback metadata appears for start path and skipped already-protected path.
- Existing scheduler callback plan tests continue to pass.
- Re-run protection scheduler callback, owner prototype, owner-selection, readiness aggregate, lifecycle cleanup, scheduled-handle, task-map simulation, adapter, audit, readiness, summary, task-operation, planner, side-effect, bridge, fanout, trace, executor, report, plan, and player-state tests.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Compose scheduler callback plan into aggregate | aggregate service/tests only | Medium | Safe if no production hooks are wired. |
| B | Java task-map runtime comparison design | docs/read-only Java sources | Low | Can be parallel if docs ownership is isolated. |
| C | Inventory cleanup-seal failure triage | inventory item-use tests/services | Medium | Separate current full-suite blocker. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Compose scheduler callback plan into readiness aggregate | `PlayerProtectionActiveTaskReadinessAggregateService.cs`, `PlayerProtectionActiveTaskReadinessAggregateServiceTests.cs`, progress/handoff docs | production lifecycle hooks, scheduler wiring, protection bridge execution path, scheduler callback plan semantics unless a test requires it |

No subagent is required because the next unit modifies the aggregate service and its focused tests plus shared docs.

## Do Not Parallelize

- Production lifecycle/delete/logout hooks.
- Shared scheduler implementation.
- Production protection bridge/adapter execution paths.
- Shared progress/handoff docs.
- Aggregate composition with another task editing the same aggregate service/tests.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1525] Add protection scheduler callback plan`.
- Files changed in UOW-1525:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerProtectionActiveTaskSchedulerCallbackPlanService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerProtectionActiveTaskSchedulerCallbackPlanServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AMW-Completion.md`
- Latest prior commits:
  - `76c5c4ed1 [Phase 6][UOW-1524] Compose protection owner prototype into readiness aggregate`
  - `a2f77cf6f [Phase 6][UOW-1523] Add protection controller task map owner prototype`
  - `16477cca2 [Phase 6][UOW-1522] Compose protection owner selection into readiness aggregate`
- Required startup remains:
  - read `docs/csharp-port.md`;
  - read `docs/PHASE-6-PROGRESS.md`;
  - read latest `docs/Phase-6*-Completion.md`;
  - read `docs/orchestration-rules.md`;
  - read `docs/parallelization-strategy.md`;
  - read `docs/parity-verification.md`;
  - note `docs/commit-conventions.md` is still missing.
