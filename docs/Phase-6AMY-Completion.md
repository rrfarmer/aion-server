# Phase 6AMY Completion - Protection Delayed Stop Callback Preview

Date: 2026-05-27
Unit of Work: UOW-1527
Status: Complete after validation.

## Scope

Add a non-live delayed-stop callback execution preview that composes scheduler callback metadata with stop-path task cancellation metadata for Java `this::stopProtectionActiveTask`, without invoking the scheduler, callback, socket fanout, visual mutation, or AI move notification paths.

## Completed Work

- Added `PlayerProtectionActiveTaskDelayedStopCallbackPreviewService`.
- Added delayed-stop callback preview request/report/row record types.
- Preview composes:
  - `PlayerProtectionActiveTaskSchedulerCallbackPlan`;
  - stop-path `PlayerProtectionActiveTaskTaskOperationPlan`;
  - non-live `PlayerProtectionActiveTaskControllerTaskMapOwnerPrototypeService`.
- Preview records:
  - scheduled callback prerequisite;
  - callback target `this::stopProtectionActiveTask`;
  - composed stop-path `cancelTask(TaskId.PROTECTION_ACTIVE)` plan;
  - owner-prototype cancellation result;
  - missing owner task no-op branch;
  - live side-effect boundary for visual mutation, socket fanout, and AI move notification;
  - runtime comparison blocker.
- Preview explicitly reports:
  - `InvokesScheduler = false`;
  - `InvokesCallback = false`;
  - `InvokesSocketFanout = false`;
  - `InvokesAiMoveNotification = false`.
- No production C# `ThreadPoolManager.Schedule`, callback invocation, controller task-map owner, lifecycle hook, socket fanout, known-list mutation, AI move notification, or protection bridge execution was wired.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --no-restore --filter "FullyQualifiedName~PlayerProtectionActiveTaskDelayedStopCallbackPreviewServiceTests"`.
- Result: passed 3 tests.
- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --no-restore --filter "FullyQualifiedName~PlayerProtectionActiveTaskDelayedStopCallbackPreviewServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSchedulerCallbackPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskControllerTaskMapOwnerPrototypeServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskMapOwnerSelectionServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskReadinessAggregateServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskMapLifecycleCleanupServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskScheduledTaskHandleAdapterTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskMapSimulationServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskMapAdapterServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskMapAuditServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskLiveReadinessServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskExecutionSummaryServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskOperationPlanServiceTests|FullyQualifiedName~PlayerProtectionAttackUtilRecipientPlannerServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSideEffectOperationPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskExecutionBridgeServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSightedRecipientSocketExecutorServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSightedRecipientTraceServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskReportServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskFanoutPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskAdapterServiceTests|FullyQualifiedName~PlayerStateTests"`.
- Result: passed 126 tests.

## Migration Parity Table - UOW-1527

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.PlayerController` | `Aion.GameServer.Services.PlayerProtectionActiveTaskDelayedStopCallbackPreviewService` | Controller / Delayed Callback Preview | Partial | Unit Tested Metadata | Partial Parity | Preview composes `this::stopProtectionActiveTask` callback target with stop-path task-operation metadata. It does not invoke the callback, mutate visual state, broadcast packets, or notify AI. |
| `com.aionemu.gameserver.controllers.CreatureController` | `PlayerProtectionActiveTaskDelayedStopCallbackPreviewService` / owner prototype cancellation | Controller / Task Map Cancellation Preview | Partial | Unit Tested Metadata | Partial Parity | Preview uses the non-live owner prototype to model `cancelTask(TaskId.PROTECTION_ACTIVE)` removed/canceled and missing-task no-op branches. Production controller owner remains unwired. |
| `com.aionemu.gameserver.utils.ThreadPoolManager` | delayed-stop callback preview scheduler boundary | Scheduler / Callback Boundary | Partial | Unit Tested Metadata | Needs Verification | Preview depends on scheduler callback metadata but explicitly does not invoke scheduler or callback. Java scheduled timing remains unverified. |
| `com.aionemu.gameserver.model.TaskId` | delayed-stop callback preview task key metadata | Enum / Task Key Preview | Partial | Unit Tested Metadata | Needs Verification | Preview targets only `PROTECTION_ACTIVE` through existing owner prototype/task-operation metadata. Full enum behavior remains incomplete. |
| `java.util.concurrent.ScheduledFuture` / `Future` | delayed-stop callback preview runtime blocker rows | Future / Callback Preview | Partial | Unit Tested Metadata | Needs Verification | Preview records future cancellation/runtime-comparison risk. Java `Future.cancel(false)` and scheduled callback race behavior remain unverified. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Create_PlannedDelayedCallbackComposesStopPlanAndCancelsOwnerTask` | Unit / delayed callback metadata | `PlayerController.startProtectionActiveTask`, `PlayerController.stopProtectionActiveTask`, `CreatureController.cancelTask` | Scheduled callback preview composes stop plan, cancels the non-live owner task, and records live side-effect boundaries. | Deterministic C# metadata over reviewed Java-source-shaped behavior. | No actual scheduler callback execution or Java runtime comparison. |
| `Create_MissingOwnerTaskRecordsNoOpCancellationBranch` | Unit / delayed callback metadata | `CreatureController.cancelTask` missing-task branch | Preview records missing owner task cancellation as a no-op. | Deterministic Java source-derived branch assertion. | No production controller task map. |
| `Create_SkipsPreviewWhenSchedulerPlanDidNotScheduleDelayedStop` | Unit / delayed callback metadata | `PlayerController.startProtectionActiveTask` already-protected guard | Preview skips callback composition when scheduler plan did not schedule delayed stop. | Deterministic Java source-derived guard assertion. | No runtime comparison. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Full game-server test suite is still known to have two unrelated stable failures in `GameServerConnectionInventoryExpansionUseItemTests`: `ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate` and `HandleUseItemAsync_ExpExtractMergesRestrictedRewardWithCleanupSealFlag`.
- Delayed-stop callback preview is metadata-only and is not consumed by production code.
- No production C# controller task-map owner, delete/logout hook, scheduler callback execution, socket fanout, known-list mutation, or AI move notification integration exists.
- Java `Future.cancel(false)`, `ScheduledFuture`, callback timing, and `ConcurrentHashMap` race behavior remain unverified by runtime comparison.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 non-live delayed-stop callback preview service and 1 focused test suite
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: Java runtime artifact generation, production controller owner wiring, production lifecycle hook, live scheduler callback execution, live delayed stop side effects, socket fanout, known-list mutations, AI move notification, Java/C# future/concurrent-map race comparison, unrelated full-suite cleanup-seal failures
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: compose `PlayerProtectionActiveTaskDelayedStopCallbackPreviewService` into the readiness aggregate.
- Add optional delayed-stop callback preview input to the aggregate request.
- Add aggregate rows showing delayed callback stop-path cancellation evidence and live side-effect blockers.
- Keep production scheduling/callback execution disabled.

## Suggested Acceptance Criteria

- Aggregate request accepts a delayed-stop callback preview.
- Aggregate rows include:
  - callback target evidence;
  - stop task-operation composition evidence;
  - owner-prototype cancellation or missing-task no-op evidence;
  - live visual/socket/AI side-effect blocker evidence.
- Aggregate tests cover stored-task cancellation preview and skipped/no-delayed-stop preview.
- Existing delayed callback preview, scheduler callback, owner prototype, readiness aggregate, lifecycle cleanup, scheduled-handle, task-map simulation, adapter, audit, readiness, summary, task-operation, planner, side-effect, bridge, fanout, trace, executor, report, plan, and player-state tests continue to pass.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Compose delayed callback preview into aggregate | aggregate service/tests only | Medium | Safe if no production callback or scheduler invocation is wired. |
| B | Java task-map runtime comparison design | docs/read-only Java sources | Low | Can be parallel if docs ownership is isolated. |
| C | Inventory cleanup-seal failure triage | inventory item-use tests/services | Medium | Separate current full-suite blocker. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Compose delayed callback preview into readiness aggregate | `PlayerProtectionActiveTaskReadinessAggregateService.cs`, `PlayerProtectionActiveTaskReadinessAggregateServiceTests.cs`, progress/handoff docs | production lifecycle hooks, actual scheduler invocation, protection bridge execution path, shared scheduler implementation |

No subagent is required because the next unit modifies the aggregate service and its focused tests plus shared docs.

## Do Not Parallelize

- Production lifecycle/delete/logout hooks.
- Shared scheduler implementation.
- Production protection bridge/adapter execution paths.
- Shared progress/handoff docs.
- Aggregate composition with another task editing the same aggregate service/tests.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1527] Add protection delayed stop callback preview`.
- Files changed in UOW-1527:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerProtectionActiveTaskDelayedStopCallbackPreviewService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerProtectionActiveTaskDelayedStopCallbackPreviewServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AMY-Completion.md`
- Latest prior commits:
  - `123299a43 [Phase 6][UOW-1526] Compose protection scheduler callback into readiness aggregate`
  - `18154a1b4 [Phase 6][UOW-1525] Add protection scheduler callback plan`
  - `76c5c4ed1 [Phase 6][UOW-1524] Compose protection owner prototype into readiness aggregate`
- Required startup remains:
  - read `docs/csharp-port.md`;
  - read `docs/PHASE-6-PROGRESS.md`;
  - read latest `docs/Phase-6*-Completion.md`;
  - read `docs/orchestration-rules.md`;
  - read `docs/parallelization-strategy.md`;
  - read `docs/parity-verification.md`;
  - note `docs/commit-conventions.md` is still missing.
