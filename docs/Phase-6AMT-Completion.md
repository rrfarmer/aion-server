# Phase 6AMT Completion - Protection Owner Selection Aggregate Composition

Date: 2026-05-27
Unit of Work: UOW-1522
Status: Complete after validation.

## Scope

Compose `PlayerProtectionActiveTaskTaskMapOwnerSelectionService` into `PlayerProtectionActiveTaskReadinessAggregateService` so the aggregate readiness checklist includes the owner recommendation and rejected-owner rationale before live enablement.

## Completed Work

- Updated `PlayerProtectionActiveTaskReadinessAggregateRequest` to accept an optional `PlayerProtectionActiveTaskTaskMapOwnerSelectionReport`.
- Added aggregate owner-selection rows for:
  - controller-owned parity recommendation;
  - rejected player-model-owned storage;
  - rejected external-service-owned storage;
  - owner-selection live blocker;
  - runtime comparison blocker.
- Updated aggregate tests to pass owner-selection reports through:
  - start path;
  - stop path;
  - lifecycle cleanup path.
- No production scheduler, task-map owner, lifecycle hook, socket fanout, known-list mutation, AI move notification, or protection bridge execution was wired.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --no-restore --filter "FullyQualifiedName~PlayerProtectionActiveTaskReadinessAggregateServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskMapOwnerSelectionServiceTests"`.
- Result: passed 6 tests.
- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --no-restore --filter "FullyQualifiedName~PlayerProtectionActiveTaskTaskMapOwnerSelectionServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskReadinessAggregateServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskMapLifecycleCleanupServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskScheduledTaskHandleAdapterTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskMapSimulationServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskMapAdapterServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskMapAuditServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskLiveReadinessServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskExecutionSummaryServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskOperationPlanServiceTests|FullyQualifiedName~PlayerProtectionAttackUtilRecipientPlannerServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSideEffectOperationPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskExecutionBridgeServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSightedRecipientSocketExecutorServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSightedRecipientTraceServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskReportServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskFanoutPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskAdapterServiceTests|FullyQualifiedName~PlayerStateTests"`.
- Result: passed 113 tests.

## Migration Parity Table - UOW-1522

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.CreatureController` | `Aion.GameServer.Services.PlayerProtectionActiveTaskReadinessAggregateService` / owner-selection report composition | Controller / Readiness Aggregate | Partial | Unit Tested Metadata | Partial Parity | Aggregate now includes the controller-owned task-map recommendation and owner blockers. Production C# controller task map remains unwired. |
| `com.aionemu.gameserver.controllers.PlayerController` | `PlayerProtectionActiveTaskReadinessAggregateService` | Controller / Protection Readiness Aggregate | Partial | Unit Tested Metadata | Needs Verification | Aggregate start/stop scenarios include owner-selection context, but live protection scheduling remains disabled. |
| `com.aionemu.gameserver.model.TaskId` | aggregate owner-selection metadata | Enum / Task Key Readiness | Partial | Unit Tested Metadata | Needs Verification | Aggregate carries owner-selection context for ordinal task-map storage but does not port full enum behavior. |
| `java.util.concurrent.Future` / `ScheduledFuture` | aggregate owner-selection runtime blocker rows | Future / Cancellation Readiness | Partial | Unit Tested Metadata | Needs Verification | Aggregate now imports owner-selection runtime comparison blocker. Java `Future.cancel(false)` behavior remains unverified. |
| `java.util.concurrent.ConcurrentHashMap` | aggregate owner-selection runtime blocker rows | Concurrency / Collection Readiness | Partial | Unit Tested Metadata | Needs Verification | Aggregate now imports owner-selection concurrency blocker. C# implementation and Java runtime comparison remain missing. |
| `com.aionemu.gameserver.model.gameobjects.player.Player` | aggregate rejected player-model owner row | Model Candidate / Readiness | Not Started | Unit Tested Metadata | Intentional Difference | Aggregate includes rationale for rejecting player-model task storage to avoid model/persistence leakage. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService` | aggregate rejected external-service owner row | Service Candidate / Readiness | Refactored | Unit Tested Metadata | Intentional Difference | Aggregate includes rationale for rejecting external-service storage as default protection owner despite existing staged patterns. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Create_StartAggregatesReadinessAuditSimulationAndRuntimeBlockers` | Unit / readiness aggregate metadata | `PlayerController.startProtectionActiveTask`, `CreatureController.addTask`, owner-selection report | Start aggregate includes owner-selection recommendation rows along with existing blockers. | Deterministic C# aggregate over Java-shaped non-live reports. | No Java runtime comparison or live scheduler. |
| `Create_StopAggregatesCancellationAndAiMoveBlockers` | Unit / readiness aggregate metadata | `PlayerController.stopProtectionActiveTask`, `CreatureController.cancelTask`, owner-selection report | Stop aggregate includes rejected player-owner rationale while preserving stop-specific blockers. | Deterministic C# aggregate over Java-shaped stop reports. | No live AI move notification or socket fanout. |
| `Create_LifecycleCleanupPrerequisitesRemainLiveBlockers` | Unit / readiness aggregate metadata | `CreatureController.onDelete -> cancelAllTasks`, owner-selection report | Lifecycle aggregate includes preferred controller-owner evidence and runtime comparison blockers. | Deterministic C# aggregate over cleanup and owner-selection reports. | No production lifecycle hook or Java concurrency runtime comparison. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Full game-server test suite is still known to have two unrelated stable failures in `GameServerConnectionInventoryExpansionUseItemTests`: `ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate` and `HandleUseItemAsync_ExpExtractMergesRestrictedRewardWithCleanupSealFlag`.
- Aggregate composition remains metadata-only and is not consumed by production code.
- No production C# controller task-map owner, delete/logout hook, scheduler callback, delayed stop invocation, socket fanout, known-list mutation, or AI move notification integration exists.
- Java `Future.cancel(false)`, `ScheduledFuture`, and `ConcurrentHashMap` race behavior remains unverified by runtime comparison.

## Summary Metrics

- Total Java artifacts discovered: 7 grouped artifact rows in this unit
- Total artifacts ported: owner-selection composition into 1 non-live readiness aggregate service and updated focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows plus 2 intentional C# candidate differences
- Total blocked artifacts: Java runtime artifact generation, live task-map owner implementation, production lifecycle hook, live scheduler callback, live delayed stop invocation, socket fanout, known-list mutations, AI move notification, Java/C# future/concurrent-map race comparison, unrelated full-suite cleanup-seal failures
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: add a non-live controller-owned protection task-map owner prototype service.
- Wrap the existing task-map adapter behind an owner-shaped API.
- Keep it disconnected from `Player`, scheduler callbacks, lifecycle hooks, and production bridge execution.
- Use the owner-selection recommendation as input/evidence.

## Suggested Acceptance Criteria

- Prototype exposes Java-shaped owner methods for protection task id: `HasTask`, `HasScheduledTask`, `GetAndRemoveTask`, `CancelTask`, `CancelTaskIfPresent`, `AddTask`, and `CancelAllTasks`.
- Prototype delegates to or mirrors `PlayerProtectionActiveTaskTaskMapAdapterService` without production wiring.
- Tests verify owner-shaped methods preserve current adapter semantics and remain non-live.
- Tests cover replacement cancel, missing cancel no-op, conditional cancel identity, and cancel-all cleanup.
- Re-run protection owner-selection, readiness aggregate, lifecycle cleanup, scheduled-handle, task-map simulation, adapter, audit, readiness, summary, task-operation, planner, side-effect, bridge, fanout, trace, executor, report, plan, and player-state tests.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Controller-owned task-map owner prototype | new service/tests | Medium | Safe if non-live and no production hooks are wired. |
| B | Java task-map runtime comparison design | docs/read-only Java sources | Low | Can be parallel if docs ownership is isolated. |
| C | Inventory cleanup-seal failure triage | inventory item-use tests/services | Medium | Separate current full-suite blocker. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Add non-live controller-owned task-map owner prototype and tests | new service/tests, progress/handoff docs | production lifecycle hooks, scheduler wiring, protection bridge execution path, `Player` model mutation |

No subagent is required unless read-only Java runtime comparison design is split off.

## Do Not Parallelize

- Production lifecycle/delete/logout hooks.
- Shared scheduler implementation.
- Production protection bridge/adapter execution paths.
- Shared progress/handoff docs.
- Owner prototype service with another task editing the task-map adapter.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1522] Compose protection owner selection into readiness aggregate`.
- Files changed in UOW-1522:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerProtectionActiveTaskReadinessAggregateService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerProtectionActiveTaskReadinessAggregateServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AMT-Completion.md`
- Latest prior commits:
  - `6b2b3231d [Phase 6][UOW-1521] Add protection task map owner selection report`
  - `79112ee90 [Phase 6][UOW-1520] Add protection task map readiness aggregate`
  - `58a3e9bfc [Phase 6][UOW-1519] Add protection task map lifecycle cleanup`
- Required startup remains:
  - read `docs/csharp-port.md`;
  - read `docs/PHASE-6-PROGRESS.md`;
  - read latest `docs/Phase-6*-Completion.md`;
  - read `docs/orchestration-rules.md`;
  - read `docs/parallelization-strategy.md`;
  - read `docs/parity-verification.md`;
  - note `docs/commit-conventions.md` is still missing.
