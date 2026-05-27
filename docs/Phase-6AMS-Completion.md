# Phase 6AMS Completion - Protection Task Map Owner Selection Report

Date: 2026-05-27
Unit of Work: UOW-1521
Status: Complete after validation.

## Scope

Add a non-live production task-map owner selection/design report that compares controller-owned versus player-owned versus external-service-owned C# storage options against Java `CreatureController.tasks`.

## Completed Work

- Added `PlayerProtectionActiveTaskTaskMapOwnerSelectionService`.
- Added owner selection request/report/row record types.
- Report rows cover the Java task-map contract:
  - `CreatureController.tasks`;
  - `hasTask`;
  - `hasScheduledTask`;
  - `getAndRemoveTask`;
  - `cancelTask`;
  - `cancelTaskIfPresent`;
  - `addTask`;
  - `cancelAllTasks`;
  - `onDelete`.
- Candidate owner rows compare:
  - controller-owned storage;
  - player-model-owned storage;
  - external-service-owned storage.
- The report recommends controller-owned storage as the parity target because Java storage lives on `CreatureController` and is cleaned by `onDelete`.
- Production scheduling remains blocked even if a future controller owner exists, because Java `Future.cancel(false)` and `ConcurrentHashMap` runtime behavior still need comparison.
- No production scheduler, task-map owner, player model storage, lifecycle hook, or protection bridge execution was wired.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --no-restore --filter "FullyQualifiedName~PlayerProtectionActiveTaskTaskMapOwnerSelectionServiceTests"`.
- Result: passed 3 tests.
- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --no-restore --filter "FullyQualifiedName~PlayerProtectionActiveTaskTaskMapOwnerSelectionServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskReadinessAggregateServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskMapLifecycleCleanupServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskScheduledTaskHandleAdapterTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskMapSimulationServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskMapAdapterServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskMapAuditServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskLiveReadinessServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskExecutionSummaryServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskOperationPlanServiceTests|FullyQualifiedName~PlayerProtectionAttackUtilRecipientPlannerServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSideEffectOperationPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskExecutionBridgeServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSightedRecipientSocketExecutorServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSightedRecipientTraceServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskReportServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskFanoutPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskAdapterServiceTests|FullyQualifiedName~PlayerStateTests"`.
- Result: passed 113 tests.

## Migration Parity Table - UOW-1521

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.CreatureController` | `Aion.GameServer.Services.PlayerProtectionActiveTaskTaskMapOwnerSelectionService` | Controller / Task Map Owner Design | Partial | Unit Tested Metadata | Partial Parity | Report models `CreatureController.tasks` and all shared task-map methods as future owner requirements. No production C# controller owner exists. |
| `com.aionemu.gameserver.controllers.PlayerController` | `PlayerProtectionActiveTaskTaskMapOwnerSelectionService` | Controller / Protection Scheduling Owner Design | Partial | Unit Tested Metadata | Needs Verification | Report links protection scheduling to inherited `CreatureController.addTask/cancelTask` semantics. Live `startProtectionActiveTask` scheduling remains disabled. |
| `com.aionemu.gameserver.model.TaskId` | owner-selection task-key metadata | Enum / Task Key Owner Design | Partial | Unit Tested Metadata | Needs Verification | Report depends on task-id ordinal keying but does not port the full Java enum or all task owners. |
| `java.util.concurrent.Future` / `ScheduledFuture` | owner-selection cancellation requirements | Future / Cancellation Owner Design | Partial | Unit Tested Metadata | Needs Verification | Report preserves `cancel(false)` as owner contract. Runtime race behavior is not compared. |
| `java.util.concurrent.ConcurrentHashMap` | owner-selection concurrency requirements | Concurrency / Owner Design | Partial | Unit Tested Metadata | Needs Verification | Report requires Java-shaped compute/remove/remove(key,value)/iteration-clear semantics. C# implementation is not wired and runtime comparison is pending. |
| `com.aionemu.gameserver.model.gameobjects.player.Player` | rejected `Aion.GameServer.Model.GameObjects.Player` storage candidate | Model Candidate | Not Started | Unit Tested Metadata | Intentional Difference | Report intentionally rejects storing scheduled task handles on the player model to avoid persistence/model leakage and because Java storage is controller-local. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService` | `Aion.GameServer.Services.BindPointTeleportRuntimeStateOwner` external-service-owned candidate reference | Service Candidate | Refactored | Manual Only / Metadata | Intentional Difference | Existing external owner pattern is treated as a staged/narrow adapter example, not the recommended default for protection task parity because Java protection task methods are controller instance methods. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Create_RecordsFullJavaCreatureControllerTaskContract` | Unit / owner-selection metadata | `CreatureController.tasks`, `hasTask`, `hasScheduledTask`, `getAndRemoveTask`, `cancelTask`, `cancelTaskIfPresent`, `addTask`, `cancelAllTasks`, `onDelete` | Owner report includes every required Java task-map contract row. | Deterministic Java source-derived checklist. | No production owner implementation or runtime comparison. |
| `Create_RecommendsControllerOwnedAndRejectsModelAndExternalDefaults` | Unit / owner-selection metadata | Java controller-local task map and inherited `PlayerController` task calls | Controller-owned storage is recommended; player-model and external-service defaults are rejected with reasons. | Deterministic owner-selection assertions from reviewed Java source. | No live implementation. |
| `Create_WithConcreteControllerOwnerStillRequiresRuntimeComparisonBeforeLiveScheduling` | Unit / owner-selection metadata | `ConcurrentHashMap` and `Future.cancel(false)` runtime-sensitive semantics | Even a future concrete owner remains non-live until runtime comparison exists. | Conservative blocker assertion. | Java artifact generation remains blocked locally. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Full game-server test suite is still known to have two unrelated stable failures in `GameServerConnectionInventoryExpansionUseItemTests`: `ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate` and `HandleUseItemAsync_ExpExtractMergesRestrictedRewardWithCleanupSealFlag`.
- Owner-selection report is metadata-only and not consumed by production code or the readiness aggregate yet.
- No production C# controller task-map owner, delete/logout hook, scheduler callback, delayed stop invocation, socket fanout, known-list mutation, or AI move notification integration exists.
- Java `Future.cancel(false)`, `ScheduledFuture`, and `ConcurrentHashMap` race behavior remains unverified by runtime comparison.

## Summary Metrics

- Total Java artifacts discovered: 7 grouped artifact rows in this unit
- Total artifacts ported: 1 non-live owner-selection/design report service and 1 focused test suite
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows plus 2 intentional C# candidate differences
- Total blocked artifacts: Java runtime artifact generation, live task-map owner implementation, production lifecycle hook, live scheduler callback, live delayed stop invocation, socket fanout, known-list mutations, AI move notification, Java/C# future/concurrent-map race comparison, unrelated full-suite cleanup-seal failures
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: compose `PlayerProtectionActiveTaskTaskMapOwnerSelectionService` into `PlayerProtectionActiveTaskReadinessAggregateService`.
- Add owner-selection input to the aggregate request.
- Add aggregate rows for the recommended controller-owned parity target and rejected player/external defaults.
- Keep live enablement blocked until production owner implementation and runtime comparison exist.

## Suggested Acceptance Criteria

- Aggregate request accepts an owner-selection report.
- Aggregate rows include controller-owned recommendation and owner-selection blockers.
- Aggregate tests assert start/stop reports include the owner recommendation without enabling live scheduling.
- Existing owner-selection tests continue to pass.
- Re-run protection owner-selection, readiness aggregate, lifecycle cleanup, scheduled-handle, task-map simulation, adapter, audit, readiness, summary, task-operation, planner, side-effect, bridge, fanout, trace, executor, report, plan, and player-state tests.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Compose owner selection into aggregate | aggregate service/tests only | Medium | Safe if no production hooks are wired. |
| B | Java task-map runtime comparison design | docs/read-only Java sources | Low | Can be parallel if docs ownership is isolated. |
| C | Inventory cleanup-seal failure triage | inventory item-use tests/services | Medium | Separate current full-suite blocker. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Compose owner-selection report into readiness aggregate | `PlayerProtectionActiveTaskReadinessAggregateService.cs`, `PlayerProtectionActiveTaskReadinessAggregateServiceTests.cs`, progress/handoff docs | production lifecycle hooks, scheduler wiring, protection bridge execution path, owner-selection service semantics unless a test requires it |

No subagent is required because the next unit modifies the aggregate service and its focused tests plus shared docs.

## Do Not Parallelize

- Production lifecycle/delete/logout hooks.
- Shared scheduler implementation.
- Production protection bridge/adapter execution paths.
- Shared progress/handoff docs.
- Aggregate composition with another task editing the same aggregate service/tests.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1521] Add protection task map owner selection report`.
- Files changed in UOW-1521:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerProtectionActiveTaskTaskMapOwnerSelectionService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerProtectionActiveTaskTaskMapOwnerSelectionServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AMS-Completion.md`
- Latest prior commits:
  - `79112ee90 [Phase 6][UOW-1520] Add protection task map readiness aggregate`
  - `58a3e9bfc [Phase 6][UOW-1519] Add protection task map lifecycle cleanup`
  - `cb7f315f9 [Phase 6][UOW-1518] Compose protection scheduled handle simulation`
- Required startup remains:
  - read `docs/csharp-port.md`;
  - read `docs/PHASE-6-PROGRESS.md`;
  - read latest `docs/Phase-6*-Completion.md`;
  - read `docs/orchestration-rules.md`;
  - read `docs/parallelization-strategy.md`;
  - read `docs/parity-verification.md`;
  - note `docs/commit-conventions.md` is still missing.
