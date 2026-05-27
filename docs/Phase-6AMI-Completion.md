# Phase 6AMI Completion - Protection Task Operation Bridge Composition

Date: 2026-05-27
Unit of Work: UOW-1511
Status: Complete after validation.

## Scope

Compose `PlayerProtectionActiveTaskTaskOperationPlan` into `PlayerProtectionActiveTaskExecutionBridgeResult`, with an optional existing-task fact and no live scheduler/task-map side effects.

## Completed Work

- Extended `PlayerProtectionActiveTaskExecutionBridgeRequest` with `ExistingProtectionTaskPresent`.
- Extended `PlayerProtectionActiveTaskExecutionBridgeResult` with `TaskOperationPlan`.
- Existing adapter-request overload defaults to no existing task and remains deterministic.
- Bridge result now exposes:
  - adapter result;
  - side-effect operation plan;
  - `AttackUtil` recipient plan;
  - task-operation plan;
  - concrete `SmPlayerState`;
  - sighted-recipient socket executor result;
  - disabled no-send metadata.
- Existing-task facts expose Java task-map intent:
  - start replacement would cancel the old future before storing the new one;
  - stop with existing task would remove and cancel it;
  - stop without existing task remains a remove-null no-op.
- Live scheduling and task cancellation remain disabled.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --no-restore --filter "FullyQualifiedName~PlayerProtectionActiveTaskTaskOperationPlanServiceTests|FullyQualifiedName~PlayerProtectionAttackUtilRecipientPlannerServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSideEffectOperationPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskExecutionBridgeServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSightedRecipientSocketExecutorServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSightedRecipientTraceServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskReportServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskFanoutPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskAdapterServiceTests|FullyQualifiedName~PlayerStateTests"`.
- Result: passed 71 tests.

## Migration Parity Table - UOW-1511

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.PlayerController` | `Aion.GameServer.Services.PlayerProtectionActiveTaskExecutionBridgeService` | Controller / Execution Bridge | Partial | Unit Tested | Partial Parity | Bridge now exposes task scheduling/cancel metadata alongside visual, packet, sighted-recipient, and `AttackUtil` projections. Live scheduler/cancel remains disabled. |
| `com.aionemu.gameserver.controllers.CreatureController` | `PlayerProtectionActiveTaskExecutionBridgeService` / `PlayerProtectionActiveTaskTaskOperationPlanService` | Controller / Task Map Dependency | Partial | Unit Tested Plan Only | Needs Verification | Bridge accepts existing-task fact and exposes `addTask` replacement or `cancelTask` removal semantics. No live C# task map integration. |
| `com.aionemu.gameserver.utils.ThreadPoolManager` | `PlayerProtectionActiveTaskExecutionBridgeService` / task-operation plan | Scheduler / Runtime Dependency | Partial | Unit Tested Plan Only | Needs Verification | Bridge exposes delayed 60000 ms schedule metadata but does not create a `ScheduledTask`. |
| `com.aionemu.gameserver.model.TaskId` | `PlayerProtectionActiveTaskExecutionBridgeService` / task-operation plan | Enum / Task Key | Partial | Unit Tested | Needs Verification | Bridge exposes `TaskId.PROTECTION_ACTIVE` name and ordinal through task-operation metadata. No live enum-keyed controller task map. |
| `java.util.concurrent.Future` | bridge-composed `PlayerProtectionActiveTaskTaskOperationPlan` / `Aion.GameServer.Utils.ScheduledTask` | Future / Scheduled Task Handle | Partial | Unit Tested Plan Only | Needs Verification | Bridge exposes Java `Future.cancel(false)` replacement/cancel intent only. No live future cancellation is invoked. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ExecuteAsync_LiveStartBuildsPlayerStatePacketAndDisabledExecutorDoesNotSend` | Unit / execution bridge | `PlayerController.startProtectionActiveTask`, `CreatureController.addTask` | Default bridge start exposes schedule/store metadata with no replacement and no live scheduler. | Deterministic Java default no-existing-task assertion. | No live scheduler/task map. |
| `ExecuteAsync_LiveSpawnedStopBuildsPlayerStatePacketAfterClearingBlinking` | Unit / execution bridge | `PlayerController.stopProtectionActiveTask`, `CreatureController.cancelTask` | Default bridge stop exposes missing-task no-op cancel metadata. | Deterministic Java missing-task assertion. | No live task map. |
| `ExecuteAsync_SkippedBranchesDoNotConstructPacketOrSend` | Unit / execution bridge | already-protected and unspawned branches | Skipped branches still expose task-operation metadata while creating no packet/no sends. | Deterministic branch assertion. | No runtime comparison. |
| `ExecuteAsync_ComposesTaskOperationPlanWithExistingTaskFact` | Unit / execution bridge | `CreatureController.addTask/cancelTask` | Existing-task fact exposes start replacement cancellation and stop existing-task cancellation metadata. | Deterministic Java task-map assertion through bridge composition. | No live `Future.cancel(false)`. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Full game-server test suite is still known to have two unrelated stable failures in `GameServerConnectionInventoryExpansionUseItemTests`: `ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate` and `HandleUseItemAsync_ExpExtractMergesRestrictedRewardWithCleanupSealFlag`.
- Bridge result shape changed again; no production caller currently consumes it.
- No live protection task map exists in C#.
- No live delayed stop scheduler integration or live future cancellation exists.
- Java concurrency semantics around `tasks.compute` and cancel races have not been runtime-compared.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: bridge composition of the protection task-operation plan plus focused bridge test updates
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: Java runtime artifact generation, live protection task map, live delayed stop scheduler integration, live future cancellation, production bridge caller, unrelated full-suite cleanup-seal failures
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: add a non-live protection execution summary/report service.
- Flatten `PlayerProtectionActiveTaskExecutionBridgeResult` into Java-order rows for:
  - visual mutation;
  - `AttackUtil.cancelCastOn`;
  - `AttackUtil.removeTargetFrom`;
  - packet construction/fanout;
  - task scheduling/storage/cancel;
  - AI move notification or flight-path skip.
- Keep all additional side effects disabled.

## Suggested Acceptance Criteria

- Summary service consumes bridge result and emits ordered rows with Java source breadcrumbs.
- Tests cover start, already-protected start, spawned stop, unspawned stop, and existing-task replacement/cancel facts.
- Rows explicitly identify live-only visual mutation and planned-not-live scheduler/cast/target/packet/AI operations.
- Re-run protection task-operation, planner, side-effect, bridge, adapter, fanout, trace, executor, report, plan, and player-state tests.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Protection execution summary/report service | new service/tests | Medium | New files; safe isolated next unit. |
| B | Live task-map design audit | read-only scheduler/controller files | Low | Can run in parallel if read-only. |
| C | Inventory cleanup-seal failure triage | inventory item-use tests/services | Medium | Separate current full-suite blocker. |

## Do Not Parallelize

- Shared protection bridge files if changing result shapes.
- Shared progress/handoff docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1511] Compose protection task operation plan`.
- Files changed in UOW-1511:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerProtectionActiveTaskExecutionBridgeService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerProtectionActiveTaskExecutionBridgeServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AMI-Completion.md`
