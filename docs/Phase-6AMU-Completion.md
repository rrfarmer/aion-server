# Phase 6AMU Completion - Protection Controller-Owned Task Map Owner Prototype

Date: 2026-05-27
Unit of Work: UOW-1523
Status: Complete after validation.

## Scope

Add a non-live controller-owned protection task-map owner prototype service that wraps the existing task-map adapter behind an owner-shaped API, without wiring it to `Player`, scheduler callbacks, lifecycle hooks, or production bridge execution.

## Completed Work

- Added `PlayerProtectionActiveTaskControllerTaskMapOwnerPrototypeService`.
- Added `PlayerProtectionActiveTaskControllerTaskMapOwnerPrototypeSnapshot`.
- The prototype exposes Java-shaped owner methods for `TaskId.PROTECTION_ACTIVE`:
  - `HasTask`;
  - `HasScheduledTask`;
  - `GetAndRemoveTask`;
  - `CancelTask`;
  - `CancelTaskIfPresent`;
  - `AddTask`;
  - `CancelAllTasks`.
- Snapshot records owner object id, task count, task ids/names, controller-owned status, non-live status, and Java source.
- No production controller task-map owner, `Player` model mutation, scheduler callback, lifecycle hook, socket fanout, known-list mutation, AI move notification, or protection bridge execution was wired.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --no-restore --filter "FullyQualifiedName~PlayerProtectionActiveTaskControllerTaskMapOwnerPrototypeServiceTests"`.
- Result: passed 6 tests.
- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --no-restore --filter "FullyQualifiedName~PlayerProtectionActiveTaskControllerTaskMapOwnerPrototypeServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskMapOwnerSelectionServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskReadinessAggregateServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskMapLifecycleCleanupServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskScheduledTaskHandleAdapterTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskMapSimulationServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskMapAdapterServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskMapAuditServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskLiveReadinessServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskExecutionSummaryServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskOperationPlanServiceTests|FullyQualifiedName~PlayerProtectionAttackUtilRecipientPlannerServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSideEffectOperationPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskExecutionBridgeServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSightedRecipientSocketExecutorServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSightedRecipientTraceServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskReportServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskFanoutPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskAdapterServiceTests|FullyQualifiedName~PlayerStateTests"`.
- Result: passed 119 tests.

## Migration Parity Table - UOW-1523

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.CreatureController` | `Aion.GameServer.Services.PlayerProtectionActiveTaskControllerTaskMapOwnerPrototypeService` | Controller / Task Map Owner Prototype | Partial | Unit Tested Metadata | Partial Parity | Prototype exposes controller-owned, owner-shaped methods over the non-live adapter. It is not wired to a production controller or lifecycle hook. |
| `com.aionemu.gameserver.controllers.PlayerController` | `PlayerProtectionActiveTaskControllerTaskMapOwnerPrototypeService` | Controller / Protection Task Owner Prototype | Partial | Unit Tested Metadata | Needs Verification | Prototype targets `TaskId.PROTECTION_ACTIVE` for future start/stop scheduling, but live `PlayerController` scheduling remains disabled. |
| `com.aionemu.gameserver.model.TaskId` | `PlayerProtectionActiveTaskControllerTaskMapOwnerPrototypeService` constants | Enum / Task Key Prototype | Partial | Unit Tested Metadata | Needs Verification | Prototype hard-codes `PROTECTION_ACTIVE` ordinal 3/name only. Full Java enum mapping remains incomplete. |
| `java.util.concurrent.Future` / `ScheduledFuture` | `IPlayerProtectionActiveTaskTaskHandle` through owner prototype | Future / Cancellation Prototype | Partial | Unit Tested Metadata | Needs Verification | Prototype delegates cancellation to existing handle abstraction with `Cancel(false)` semantics. Java runtime behavior remains unverified. |
| `java.util.concurrent.ConcurrentHashMap` | adapter-backed owner prototype | Concurrency / Collection Prototype | Partial | Unit Tested Metadata | Needs Verification | Prototype uses the existing lock-backed non-live adapter, not Java `ConcurrentHashMap`; runtime race behavior is not compared. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `AddTask_StoresProtectionTaskAndReportsControllerOwnedNonLiveSnapshot` | Unit / owner prototype | `CreatureController.addTask`, `hasTask`, `hasScheduledTask` | Owner-shaped prototype stores protection handle and reports non-live controller-owned snapshot. | Deterministic C# owner wrapper over Java-shaped adapter. | No production controller wiring. |
| `AddTask_ReplacesExistingProtectionTaskAndCancelsOldHandle` | Unit / owner prototype | `CreatureController.addTask` replacement branch | Replacement cancels old handle with `false`, stores new handle, and exposes removed handle. | Deterministic C# owner wrapper assertion. | No runtime race comparison. |
| `CancelTask_RemovesBeforeCancelAndMissingCancelIsNoOp` | Unit / owner prototype | `CreatureController.cancelTask` | Remove-before-cancel and missing cancel no-op are preserved through owner API. | Deterministic Java source-derived behavior. | No live delayed stop callback. |
| `CancelTaskIfPresent_CancelsOnlyMatchingProtectionHandle` | Unit / owner prototype | `CreatureController.cancelTaskIfPresent` | Conditional cancel removes only the matching handle by reference. | Deterministic Java source-derived behavior. | No Java concurrent remove runtime comparison. |
| `CancelAllTasks_CancelsProtectionTaskAndClearsOwnerMap` | Unit / owner prototype | `CreatureController.cancelAllTasks` | Cancel-all cancels stored handle and clears owner snapshot. | Deterministic Java source-derived behavior. | No lifecycle hook or concurrent iteration runtime comparison. |
| `HasScheduledTask_TreatsDoneProtectionHandleAsNotScheduled` | Unit / owner prototype | `CreatureController.hasScheduledTask` | Done handle remains present but not scheduled. | Deterministic Java source-derived behavior. | No Java `Future.isDone` runtime comparison. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Full game-server test suite is still known to have two unrelated stable failures in `GameServerConnectionInventoryExpansionUseItemTests`: `ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate` and `HandleUseItemAsync_ExpExtractMergesRestrictedRewardWithCleanupSealFlag`.
- Owner prototype is non-live and not consumed by production code or the readiness aggregate.
- No production C# controller task-map owner, delete/logout hook, scheduler callback, delayed stop invocation, socket fanout, known-list mutation, or AI move notification integration exists.
- Java `Future.cancel(false)`, `ScheduledFuture`, and `ConcurrentHashMap` race behavior remains unverified by runtime comparison.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 non-live controller-owned owner prototype service and 1 focused test suite
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: Java runtime artifact generation, production controller owner wiring, production lifecycle hook, live scheduler callback, live delayed stop invocation, socket fanout, known-list mutations, AI move notification, Java/C# future/concurrent-map race comparison, unrelated full-suite cleanup-seal failures
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: compose `PlayerProtectionActiveTaskControllerTaskMapOwnerPrototypeService` into the readiness aggregate.
- Let the aggregate distinguish owner prototype evidence from production owner readiness.
- Keep live scheduling blocked until production lifecycle wiring and runtime comparison exist.

## Suggested Acceptance Criteria

- Aggregate request accepts an owner prototype snapshot.
- Aggregate rows include non-live owner prototype evidence and still block production owner readiness.
- Tests assert aggregate reports prototype evidence without setting `CanEnableProtectionTaskMapStack`.
- Existing owner prototype tests continue to pass.
- Re-run protection owner prototype, owner-selection, readiness aggregate, lifecycle cleanup, scheduled-handle, task-map simulation, adapter, audit, readiness, summary, task-operation, planner, side-effect, bridge, fanout, trace, executor, report, plan, and player-state tests.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Compose owner prototype into aggregate | aggregate service/tests only | Medium | Safe if no production hooks are wired. |
| B | Java task-map runtime comparison design | docs/read-only Java sources | Low | Can be parallel if docs ownership is isolated. |
| C | Inventory cleanup-seal failure triage | inventory item-use tests/services | Medium | Separate current full-suite blocker. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Compose owner prototype evidence into readiness aggregate | `PlayerProtectionActiveTaskReadinessAggregateService.cs`, `PlayerProtectionActiveTaskReadinessAggregateServiceTests.cs`, progress/handoff docs | production lifecycle hooks, scheduler wiring, protection bridge execution path, owner prototype semantics unless a test requires it |

No subagent is required because the next unit modifies the aggregate service and its focused tests plus shared docs.

## Do Not Parallelize

- Production lifecycle/delete/logout hooks.
- Shared scheduler implementation.
- Production protection bridge/adapter execution paths.
- Shared progress/handoff docs.
- Aggregate composition with another task editing the same aggregate service/tests.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1523] Add protection controller task map owner prototype`.
- Files changed in UOW-1523:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerProtectionActiveTaskControllerTaskMapOwnerPrototypeService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerProtectionActiveTaskControllerTaskMapOwnerPrototypeServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AMU-Completion.md`
- Latest prior commits:
  - `16477cca2 [Phase 6][UOW-1522] Compose protection owner selection into readiness aggregate`
  - `6b2b3231d [Phase 6][UOW-1521] Add protection task map owner selection report`
  - `79112ee90 [Phase 6][UOW-1520] Add protection task map readiness aggregate`
- Required startup remains:
  - read `docs/csharp-port.md`;
  - read `docs/PHASE-6-PROGRESS.md`;
  - read latest `docs/Phase-6*-Completion.md`;
  - read `docs/orchestration-rules.md`;
  - read `docs/parallelization-strategy.md`;
  - read `docs/parity-verification.md`;
  - note `docs/commit-conventions.md` is still missing.
