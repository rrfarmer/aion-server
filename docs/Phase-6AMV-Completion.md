# Phase 6AMV Completion - Protection Owner Prototype Aggregate Composition

Date: 2026-05-27
Unit of Work: UOW-1524
Status: Complete after validation.

## Scope

Compose `PlayerProtectionActiveTaskControllerTaskMapOwnerPrototypeService` into the readiness aggregate so the aggregate can distinguish owner prototype evidence from production owner readiness while still blocking live scheduling.

## Completed Work

- Updated `PlayerProtectionActiveTaskReadinessAggregateRequest` to accept an optional `PlayerProtectionActiveTaskControllerTaskMapOwnerPrototypeSnapshot`.
- Added aggregate owner prototype rows for:
  - non-live controller-owned owner prototype evidence;
  - production owner readiness still blocked because the prototype is not wired to `PlayerController`, scheduler callbacks, or lifecycle cleanup.
- Updated aggregate tests to pass owner prototype snapshots through:
  - start path;
  - stop path;
  - lifecycle cleanup path.
- No production controller task-map owner, scheduler callback, lifecycle hook, socket fanout, known-list mutation, AI move notification, or protection bridge execution was wired.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --no-restore --filter "FullyQualifiedName~PlayerProtectionActiveTaskReadinessAggregateServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskControllerTaskMapOwnerPrototypeServiceTests"`.
- Result: passed 9 tests.
- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --no-restore --filter "FullyQualifiedName~PlayerProtectionActiveTaskControllerTaskMapOwnerPrototypeServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskMapOwnerSelectionServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskReadinessAggregateServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskMapLifecycleCleanupServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskScheduledTaskHandleAdapterTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskMapSimulationServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskMapAdapterServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskMapAuditServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskLiveReadinessServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskExecutionSummaryServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskOperationPlanServiceTests|FullyQualifiedName~PlayerProtectionAttackUtilRecipientPlannerServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSideEffectOperationPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskExecutionBridgeServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSightedRecipientSocketExecutorServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSightedRecipientTraceServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskReportServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskFanoutPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskAdapterServiceTests|FullyQualifiedName~PlayerStateTests"`.
- Result: passed 119 tests.

## Migration Parity Table - UOW-1524

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.CreatureController` | `Aion.GameServer.Services.PlayerProtectionActiveTaskReadinessAggregateService` / owner prototype snapshot composition | Controller / Readiness Aggregate | Partial | Unit Tested Metadata | Partial Parity | Aggregate now records non-live controller-owned owner prototype evidence while keeping production owner readiness blocked. |
| `com.aionemu.gameserver.controllers.PlayerController` | `PlayerProtectionActiveTaskReadinessAggregateService` | Controller / Protection Readiness Aggregate | Partial | Unit Tested Metadata | Needs Verification | Aggregate records that the owner prototype is not wired to `PlayerController` scheduling. Live protection scheduling remains disabled. |
| `com.aionemu.gameserver.model.TaskId` | aggregate owner prototype metadata | Enum / Task Key Readiness | Partial | Unit Tested Metadata | Needs Verification | Aggregate receives prototype snapshot for `PROTECTION_ACTIVE` only; full enum behavior remains incomplete. |
| `java.util.concurrent.Future` / `ScheduledFuture` | aggregate owner prototype blocker rows | Future / Cancellation Readiness | Partial | Unit Tested Metadata | Needs Verification | Aggregate still blocks runtime scheduling despite prototype evidence. Java `Future.cancel(false)` behavior remains unverified. |
| `java.util.concurrent.ConcurrentHashMap` | aggregate owner prototype blocker rows | Concurrency / Collection Readiness | Partial | Unit Tested Metadata | Needs Verification | Prototype evidence uses C# adapter semantics, not Java `ConcurrentHashMap` runtime behavior. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Create_StartAggregatesReadinessAuditSimulationAndRuntimeBlockers` | Unit / readiness aggregate metadata | `PlayerController.startProtectionActiveTask`, `CreatureController.addTask`, owner prototype snapshot | Start aggregate includes owner prototype evidence row while keeping production readiness blocked. | Deterministic C# aggregate over Java-shaped non-live reports. | No Java runtime comparison or live scheduler. |
| `Create_StopAggregatesCancellationAndAiMoveBlockers` | Unit / readiness aggregate metadata | `PlayerController.stopProtectionActiveTask`, `CreatureController.cancelTask`, owner prototype snapshot | Stop aggregate includes owner prototype blocker row without enabling scheduling. | Deterministic C# aggregate over Java-shaped stop reports. | No live AI move notification or socket fanout. |
| `Create_LifecycleCleanupPrerequisitesRemainLiveBlockers` | Unit / readiness aggregate metadata | `CreatureController.onDelete -> cancelAllTasks`, owner prototype snapshot | Lifecycle aggregate includes prototype evidence and runtime comparison blockers. | Deterministic C# aggregate over cleanup and owner prototype reports. | No production lifecycle hook or Java concurrency runtime comparison. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Full game-server test suite is still known to have two unrelated stable failures in `GameServerConnectionInventoryExpansionUseItemTests`: `ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate` and `HandleUseItemAsync_ExpExtractMergesRestrictedRewardWithCleanupSealFlag`.
- Aggregate prototype composition remains metadata-only and is not consumed by production code.
- No production C# controller task-map owner, delete/logout hook, scheduler callback, delayed stop invocation, socket fanout, known-list mutation, or AI move notification integration exists.
- Java `Future.cancel(false)`, `ScheduledFuture`, and `ConcurrentHashMap` race behavior remains unverified by runtime comparison.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: owner prototype evidence composition into 1 non-live readiness aggregate service and updated focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: Java runtime artifact generation, production controller owner wiring, production lifecycle hook, live scheduler callback, live delayed stop invocation, socket fanout, known-list mutations, AI move notification, Java/C# future/concurrent-map race comparison, unrelated full-suite cleanup-seal failures
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: add a non-live scheduler callback invocation plan for the controller-owned protection owner prototype.
- Model `ThreadPoolManager.schedule(this::stopProtectionActiveTask, 60000)` as metadata only.
- Prove the planned callback remains disconnected from production execution.
- Feed the callback plan into the readiness aggregate in a later unit.

## Suggested Acceptance Criteria

- New plan records 60000 ms delay, Java callback target, scheduler source, task-map owner prototype target, and non-live status.
- Tests cover start scheduling metadata, already-protected skip, and runtime-disconnected callback evidence.
- No `ThreadPoolManager.Schedule` is invoked by the new plan.
- Re-run protection owner prototype, owner-selection, readiness aggregate, lifecycle cleanup, scheduled-handle, task-map simulation, adapter, audit, readiness, summary, task-operation, planner, side-effect, bridge, fanout, trace, executor, report, plan, and player-state tests.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Non-live scheduler callback invocation plan | new service/tests | Medium | Safe if no actual scheduler call is made. |
| B | Java task-map runtime comparison design | docs/read-only Java sources | Low | Can be parallel if docs ownership is isolated. |
| C | Inventory cleanup-seal failure triage | inventory item-use tests/services | Medium | Separate current full-suite blocker. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Add non-live scheduler callback invocation plan and tests | new service/tests, progress/handoff docs | production lifecycle hooks, scheduler wiring, protection bridge execution path, owner prototype semantics unless a test requires it |

No subagent is required unless read-only Java runtime comparison design is split off.

## Do Not Parallelize

- Production lifecycle/delete/logout hooks.
- Shared scheduler implementation.
- Production protection bridge/adapter execution paths.
- Shared progress/handoff docs.
- Scheduler callback planning with any task editing the actual scheduler or bridge execution path.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1524] Compose protection owner prototype into readiness aggregate`.
- Files changed in UOW-1524:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerProtectionActiveTaskReadinessAggregateService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerProtectionActiveTaskReadinessAggregateServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AMV-Completion.md`
- Latest prior commits:
  - `a2f77cf6f [Phase 6][UOW-1523] Add protection controller task map owner prototype`
  - `16477cca2 [Phase 6][UOW-1522] Compose protection owner selection into readiness aggregate`
  - `6b2b3231d [Phase 6][UOW-1521] Add protection task map owner selection report`
- Required startup remains:
  - read `docs/csharp-port.md`;
  - read `docs/PHASE-6-PROGRESS.md`;
  - read latest `docs/Phase-6*-Completion.md`;
  - read `docs/orchestration-rules.md`;
  - read `docs/parallelization-strategy.md`;
  - read `docs/parity-verification.md`;
  - note `docs/commit-conventions.md` is still missing.
