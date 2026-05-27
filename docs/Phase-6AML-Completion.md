# Phase 6AML Completion - Protection Task Map Audit

Date: 2026-05-27
Unit of Work: UOW-1514
Status: Complete after validation.

## Scope

Add a read-only/non-live protection task-map design audit that compares Java `CreatureController` task-map behavior, `TaskId.PROTECTION_ACTIVE`, Java `ThreadPoolManager.schedule`, and the current C# scheduler primitive.

## Completed Work

- Reviewed Java sources:
  - `CreatureController` task map methods;
  - `PlayerController.startProtectionActiveTask` / `stopProtectionActiveTask`;
  - `TaskId`;
  - `ThreadPoolManager.schedule`.
- Reviewed C# `Aion.GameServer.Utils.ThreadPoolManager` and `ScheduledTask`.
- Added `PlayerProtectionActiveTaskTaskMapAuditService`.
- Added audit area/status/report/row record types.
- Audit rows now record:
  - `TaskId.PROTECTION_ACTIVE` ordinal 3;
  - 60000 ms delayed `stopProtectionActiveTask` scheduling;
  - Java `ConcurrentHashMap<Integer, Future<?>>` task storage keyed by `taskId.ordinal()`;
  - Java atomic `tasks.compute` replacement plus old `Future.cancel(false)`;
  - Java remove-before-cancel `cancelTask`;
  - Java missing-task cancel no-op;
  - discovered `cancelTaskIfPresent` dependency;
  - `cancelAllTasks` / `onDelete` cleanup;
  - C# `ThreadPoolManager.Schedule` / `ScheduledTask.Cancel()` primitive;
  - missing C# protection task-map adapter;
  - readiness-gate linkage when `SchedulerTaskMap` is blocked.
- No live scheduler callback, task map, or cancellation behavior was enabled.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --no-restore --filter "FullyQualifiedName~PlayerProtectionActiveTaskTaskMapAuditServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskLiveReadinessServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskExecutionSummaryServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskOperationPlanServiceTests|FullyQualifiedName~PlayerProtectionAttackUtilRecipientPlannerServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSideEffectOperationPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskExecutionBridgeServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSightedRecipientSocketExecutorServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSightedRecipientTraceServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskReportServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskFanoutPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskAdapterServiceTests|FullyQualifiedName~PlayerStateTests"`.
- Result: passed 86 tests.

## Migration Parity Table - UOW-1514

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.CreatureController` | `Aion.GameServer.Services.PlayerProtectionActiveTaskTaskMapAuditService` | Controller / Task Map Audit | Partial | Unit Tested Metadata | Needs Verification | Audit records `ConcurrentHashMap<Integer, Future<?>>`, `hasTask`, `hasScheduledTask`, `getAndRemoveTask`, `cancelTask`, `cancelTaskIfPresent`, `addTask`, and `cancelAllTasks` behavior. No live C# task-map adapter exists. |
| `com.aionemu.gameserver.controllers.PlayerController` | `PlayerProtectionActiveTaskTaskMapAuditService` | Controller / Protection Scheduler Audit | Partial | Unit Tested Metadata | Needs Verification | Audit records protection start schedule/add and stop cancel flow. No live delayed callback invokes stop protection. |
| `com.aionemu.gameserver.model.TaskId` | `PlayerProtectionActiveTaskTaskMapAuditService` / protection task metadata | Enum / Task Key Audit | Partial | Unit Tested Metadata | Needs Verification | Audit records `PROTECTION_ACTIVE` ordinal 3 and warns full enum/key mapping is not complete. |
| `com.aionemu.gameserver.utils.ThreadPoolManager` | `PlayerProtectionActiveTaskTaskMapAuditService` / `Aion.GameServer.Utils.ThreadPoolManager` | Scheduler / Primitive Audit | Partial | Unit Tested Metadata | Needs Verification | Audit compares Java `ScheduledFuture<?> schedule(..., 60000)` with C# `ScheduledTask` returned by `ThreadPoolManager.Schedule`. Runtime cancellation/execution parity is not compared. |
| `java.util.concurrent.Future` | `PlayerProtectionActiveTaskTaskMapAuditService` / `Aion.GameServer.Utils.ScheduledTask` | Future / Cancellation Audit | Partial | Unit Tested Metadata | Needs Verification | Audit records Java `cancel(false)` replacement/cancel semantics and current cooperative C# cancellation primitive. No runtime race comparison. |
| `java.util.concurrent.ConcurrentHashMap` | future task-map adapter / audit metadata | Concurrency / Collection Dependency | Not Started | Unit Tested Metadata | Needs Verification | Audit identifies need for atomic replace-and-cancel, remove-before-cancel, and cleanup strategy. No C# map implementation exists for protection tasks. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Create_RecordsJavaTaskIdAndScheduleRequirements` | Unit / audit metadata | `TaskId.java`, `PlayerController.startProtectionActiveTask`, `ThreadPoolManager.schedule` | Audit rows capture `PROTECTION_ACTIVE` ordinal 3 and 60000 ms delayed schedule requirements. | Static Java source review encoded in deterministic metadata. | No runtime scheduling. |
| `Create_RecordsAddReplaceAndCancelSemantics` | Unit / audit metadata | `CreatureController.addTask/cancelTask` | Audit rows capture atomic replace/cancel, remove-before-cancel, and missing-task no-op. | Static Java source review encoded in deterministic metadata. | No live map behavior. |
| `Create_RecordsLifecycleAndConditionalCancelDependencies` | Unit / audit metadata | `CreatureController.cancelTaskIfPresent/cancelAllTasks/onDelete` | Audit rows include discovered conditional cancel and lifecycle cleanup dependencies. | Static Java source review encoded in deterministic metadata. | Protection active task does not directly call conditional cancel; future adapter scope remains open. |
| `Create_RecordsCSharpSchedulerPrimitiveAndTaskMapGap` | Unit / audit metadata | Java `ThreadPoolManager`, C# `ThreadPoolManager` source review | Audit rows record C# `ScheduledTask.Cancel()` primitive and missing task-map adapter gap. | Static Java/C# source review encoded in deterministic metadata. | No runtime parity comparison. |
| `Create_LinksBlockedReadinessReportToTaskMapAudit` | Unit / audit metadata | UOW-1513 readiness gate | Audit adds a readiness link when `SchedulerTaskMap` is blocked. | Deterministic bridge from readiness report to audit metadata. | No live enablement. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Full game-server test suite is still known to have two unrelated stable failures in `GameServerConnectionInventoryExpansionUseItemTests`: `ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate` and `HandleUseItemAsync_ExpExtractMergesRestrictedRewardWithCleanupSealFlag`.
- Task-map audit is metadata only and not consumed by production code.
- No C# protection task-map owner, key shape, lock/concurrency strategy, scheduler callback, delayed stop invocation, or lifecycle cleanup hook exists yet.
- Java `tasks.compute` atomicity and `Future.cancel(false)` race behavior remain unverified at runtime.
- C# `ScheduledTask.Cancel()` uses cooperative cancellation; equivalence to Java non-interrupt cancel remains only reasoned, not runtime-compared.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 1 non-live protection task-map audit service and 1 focused test suite
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: Java runtime artifact generation, live task-map adapter, live scheduler callback, live future cancellation/race comparison, lifecycle cleanup hook, unrelated full-suite cleanup-seal failures
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: add a non-live protection task-map adapter contract/prototype.
- Model Java task-map operations against supplied fake scheduled-task handles without invoking `ThreadPoolManager`.
- Preserve:
  - atomic replacement;
  - remove-before-cancel;
  - missing no-op;
  - conditional cancel;
  - cancel-all semantics.
- Keep the prototype disconnected from production protection execution.

## Suggested Acceptance Criteria

- Adapter has narrow in-memory handle abstraction so tests can observe cancel calls without scheduler threads.
- Tests cover add/store, replacement cancel, stop cancel, missing cancel no-op, conditional cancel match/mismatch, and cancel-all cleanup.
- Audit/readiness rows remain non-live and do not imply production enablement.
- Re-run protection task-map audit, readiness, summary, task-operation, planner, side-effect, bridge, adapter, fanout, trace, executor, report, plan, and player-state tests.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Protection task-map adapter prototype | new service/tests | Medium | Safe if disconnected from production execution. |
| B | Java task-map runtime comparison design | docs/read-only Java sources | Low | Can run in parallel if no writes overlap. |
| C | Inventory cleanup-seal failure triage | inventory item-use tests/services | Medium | Separate current full-suite blocker. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Add non-live task-map adapter prototype and tests | new adapter service/tests, progress/handoff docs | production scheduler wiring, bridge execution path |

No subagent is required unless a read-only runtime-comparison design note is split off.

## Do Not Parallelize

- Shared scheduler implementation.
- Production protection bridge/adapter execution paths.
- Shared progress/handoff docs.
- Any live delayed stop scheduling.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1514] Add protection task map audit`.
- Files changed in UOW-1514:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerProtectionActiveTaskTaskMapAuditService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerProtectionActiveTaskTaskMapAuditServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AML-Completion.md`
- Latest prior commits:
  - `c0abe27fb [Phase 6][UOW-1512] Add protection execution summary`
  - `3d0498afc [Phase 6][UOW-1513] Add protection live readiness gate`
- Required startup remains:
  - read `docs/csharp-port.md`;
  - read `docs/PHASE-6-PROGRESS.md`;
  - read latest `docs/Phase-6*-Completion.md`;
  - read `docs/orchestration-rules.md`;
  - read `docs/parallelization-strategy.md`;
  - read `docs/parity-verification.md`;
  - note `docs/commit-conventions.md` is still missing.
