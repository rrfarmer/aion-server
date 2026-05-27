# Phase 6AMQ Completion - Protection Task Map Lifecycle Cleanup Planner

Date: 2026-05-27
Unit of Work: UOW-1519
Status: Complete after validation.

## Scope

Add a non-live protection task-map lifecycle cleanup planner/report that composes Java `CreatureController.onDelete -> cancelAllTasks` semantics with future player/controller deletion or logout prerequisites.

## Completed Work

- Added `PlayerProtectionActiveTaskTaskMapLifecycleCleanupService`.
- Added lifecycle cleanup request/report/row record types.
- Planner composes `cancelAllTasks` through the non-live task-map adapter.
- Cleanup scenarios cover:
  - no pending protection task;
  - pending protection task canceled by cleanup;
  - old protection task canceled by replacement, then replacement canceled by cleanup.
- Reported remaining prerequisites:
  - choose production C# task-map owner;
  - wire cleanup to future delete/logout lifecycle;
  - runtime-compare Java `cancelAllTasks` and `Future.cancel(false)` race behavior.
- No production lifecycle hooks, scheduler wiring, protection bridge execution, or task-map owner was added.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --no-restore --filter "FullyQualifiedName~PlayerProtectionActiveTaskTaskMapLifecycleCleanupServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskScheduledTaskHandleAdapterTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskMapSimulationServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskMapAdapterServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskMapAuditServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskLiveReadinessServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskExecutionSummaryServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskOperationPlanServiceTests|FullyQualifiedName~PlayerProtectionAttackUtilRecipientPlannerServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSideEffectOperationPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskExecutionBridgeServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSightedRecipientSocketExecutorServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSightedRecipientTraceServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskReportServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskFanoutPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskAdapterServiceTests|FullyQualifiedName~PlayerStateTests"`.
- Result: passed 107 tests.

## Migration Parity Table - UOW-1519

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.CreatureController` | `Aion.GameServer.Services.PlayerProtectionActiveTaskTaskMapLifecycleCleanupService` | Controller / Lifecycle Cleanup Planner | Partial | Unit Tested Metadata | Partial Parity | Planner models `onDelete -> cancelAllTasks` and adapter cleanup behavior. No production C# lifecycle hook or task-map owner exists. |
| `com.aionemu.gameserver.model.TaskId` | lifecycle cleanup planner task-map seed metadata | Enum / Task Key Cleanup | Partial | Unit Tested Metadata | Needs Verification | Planner uses `PROTECTION_ACTIVE` ordinal/name constants for cleanup scenarios. Full task-id enum mapping remains incomplete. |
| `java.util.concurrent.Future` | `IPlayerProtectionActiveTaskTaskHandle` in lifecycle cleanup tests | Future / Cleanup Handle | Partial | Unit Tested Metadata | Needs Verification | Fake handles observe `Cancel(false)` for cleanup and replacement. Java `Future.cancel(false)` runtime behavior remains unverified. |
| `java.util.concurrent.ConcurrentHashMap` | task-map adapter used by lifecycle cleanup planner | Concurrency / Cleanup Collection | Partial | Unit Tested Metadata | Needs Verification | Planner relies on adapter lock semantics for cancel-all. Java concurrent iteration/clear behavior is not runtime-compared. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Create_WithoutPendingTaskReportsNoOpCleanupAndPrerequisites` | Unit / lifecycle cleanup metadata | `CreatureController.cancelAllTasks` empty map behavior | Empty cleanup is no-op and reports production owner/hook prerequisites. | Deterministic Java cleanup assertion through adapter. | No production lifecycle hook. |
| `Create_WithPendingProtectionTaskCancelsItDuringCleanup` | Unit / lifecycle cleanup metadata | `CreatureController.cancelAllTasks` | Pending protection task is canceled with `false` and map is cleared. | Deterministic Java cleanup assertion with fake handle. | No live future. |
| `Create_AfterReplacementCancelsOldDuringReplaceAndNewDuringCleanup` | Unit / lifecycle cleanup metadata | `CreatureController.addTask/cancelAllTasks` | Replacement cancels old handle, then cleanup cancels replacement handle. | Deterministic Java replace/cleanup assertion. | No runtime race comparison. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Full game-server test suite is still known to have two unrelated stable failures in `GameServerConnectionInventoryExpansionUseItemTests`: `ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate` and `HandleUseItemAsync_ExpExtractMergesRestrictedRewardWithCleanupSealFlag`.
- Lifecycle cleanup planner is metadata-only and not consumed by production code.
- No production C# task-map owner, delete/logout hook, scheduler callback, delayed stop invocation, or lifecycle cleanup integration exists.
- Java `Future.cancel(false)` and `ConcurrentHashMap` cleanup race behavior remains unverified by runtime comparison.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped artifact rows in this unit
- Total artifacts ported: 1 non-live lifecycle cleanup planner/report service and 1 focused test suite
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: Java runtime artifact generation, live task-map owner, production lifecycle hook, live scheduler callback, live delayed stop invocation, Java/C# future race comparison, unrelated full-suite cleanup-seal failures
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: add a production-readiness aggregate report for the protection task-map stack.
- Combine audit, readiness, adapter, simulation, scheduled-handle, and lifecycle cleanup outputs.
- Emit a single ordered checklist of blockers before live enablement.
- Keep it non-live and do not wire production scheduling or lifecycle hooks.

## Suggested Acceptance Criteria

- Aggregate report consumes existing non-live reports or result summaries.
- Checklist rows include owner selection, known-list facts, scheduler callback, task-map storage, delayed stop invocation, lifecycle cleanup hook, packet/AI gates, and Java runtime comparison blockers.
- Tests cover start path, stop path, and lifecycle cleanup blocker aggregation.
- Re-run protection lifecycle cleanup, scheduled-handle, task-map simulation, adapter, audit, readiness, summary, task-operation, planner, side-effect, bridge, adapter, fanout, trace, executor, report, plan, and player-state tests.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Production-readiness aggregate report | new service/tests | Medium | Safe if non-live and no production hooks are wired. |
| B | Java lifecycle/runtime comparison design | docs/read-only sources | Low | Can be parallel if docs ownership is isolated. |
| C | Inventory cleanup-seal failure triage | inventory item-use tests/services | Medium | Separate current full-suite blocker. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Add readiness aggregate report and tests | new service/tests, progress/handoff docs | production lifecycle hooks, scheduler wiring, protection bridge execution path |

No subagent is required unless read-only lifecycle/runtime comparison design is split off.

## Do Not Parallelize

- Production lifecycle/delete/logout hooks.
- Shared scheduler implementation.
- Production protection bridge/adapter execution paths.
- Shared progress/handoff docs.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1519] Add protection task map lifecycle cleanup`.
- Files changed in UOW-1519:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerProtectionActiveTaskTaskMapLifecycleCleanupService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerProtectionActiveTaskTaskMapLifecycleCleanupServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AMQ-Completion.md`
- Latest prior commits:
  - `cb7f315f9 [Phase 6][UOW-1518] Compose protection scheduled handle simulation`
- Required startup remains:
  - read `docs/csharp-port.md`;
  - read `docs/PHASE-6-PROGRESS.md`;
  - read latest `docs/Phase-6*-Completion.md`;
  - read `docs/orchestration-rules.md`;
  - read `docs/parallelization-strategy.md`;
  - read `docs/parity-verification.md`;
  - note `docs/commit-conventions.md` is still missing.
