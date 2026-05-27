# Phase 6ANB Completion - Protection Controller Task-Map Wiring Intent Report

Date: 2026-05-27
Unit of Work: UOW-1530
Status: Complete after validation.

## Scope

Add a non-live controller task-map wiring intent report that maps protection lifecycle closure blockers to future production hook points without changing runtime behavior.

## Completed Work

- Added `PlayerProtectionActiveTaskControllerTaskMapWiringIntentReportService`.
- Added wiring hook/status enums, report rows, and report record.
- Report consumes `PlayerProtectionActiveTaskLifecycleClosureReport`.
- Report maps blockers to future C# hook targets:
  - start protection task storage;
  - stop protection task cancellation;
  - scheduler callback execution;
  - controller lifecycle cleanup;
  - spawned-player visual/socket/AI side effects;
  - Java runtime comparison.
- Report exposes:
  - `ReadyForImplementation`, which remains false;
  - per-hook `ShouldImplementHook`;
  - per-hook `BlocksImplementation`.
- Already-protected Java branch is handled as a skipped branch that does not request task-map storage or scheduler callback implementation.
- Missing delayed callback preview blocks stop-cancellation hook work before implementation is requested.
- A read-only explorer inspected Java hook points and was closed after reporting. No subagent edits were integrated.
- No production C# scheduler callback execution, controller task-map owner, lifecycle hook, socket fanout, known-list mutation, AI move notification, first-action stop hook, inbound-damage guard, or protection bridge execution was wired.

## Java Hook Notes From Read-Only Explorer

- `PlayerController.startProtectionActiveTask` order:
  1. guard `!isProtectionActive()`;
  2. set `BLINKING`;
  3. cancel casts on the player;
  4. remove the player from targets;
  5. broadcast `SM_PLAYER_STATE`;
  6. schedule `stopProtectionActiveTask` after 60000 ms and store under `TaskId.PROTECTION_ACTIVE`.
- `PlayerController.stopProtectionActiveTask` order:
  1. `cancelTask(TaskId.PROTECTION_ACTIVE)`;
  2. if spawned, unset `BLINKING`;
  3. broadcast `SM_PLAYER_STATE`;
  4. `notifyAIOnMove()`.
- `CreatureController.onDelete()` cancels all tasks before `super.onDelete()`.
- Java `TaskId.PROTECTION_ACTIVE` ordinal is 3.
- Java first-action stop callers include movement, air movement, attack, cast, item use, dialog/show-dialog, emotion, and composite-stone actions.
- Protection active also affects inbound damage, aggro suppression, target/skill rejection, and material-skill suppression.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --no-restore --filter "FullyQualifiedName~PlayerProtectionActiveTaskControllerTaskMapWiringIntentReportServiceTests"`.
- Result: passed 3 tests after tightening missing delayed-preview semantics.
- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --no-restore --filter "FullyQualifiedName~PlayerProtectionActiveTaskControllerTaskMapWiringIntentReportServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskLifecycleClosureReportServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskDelayedStopCallbackPreviewServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSchedulerCallbackPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskControllerTaskMapOwnerPrototypeServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskMapOwnerSelectionServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskReadinessAggregateServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskMapLifecycleCleanupServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskScheduledTaskHandleAdapterTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskMapSimulationServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskMapAdapterServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskMapAuditServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskLiveReadinessServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskExecutionSummaryServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskTaskOperationPlanServiceTests|FullyQualifiedName~PlayerProtectionAttackUtilRecipientPlannerServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSideEffectOperationPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskExecutionBridgeServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSightedRecipientSocketExecutorServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSightedRecipientTraceServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskReportServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskFanoutPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskAdapterServiceTests|FullyQualifiedName~PlayerStateTests"`.
- Result: passed 134 tests.

## Migration Parity Table - UOW-1530

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.PlayerController` | `Aion.GameServer.Services.PlayerProtectionActiveTaskControllerTaskMapWiringIntentReportService` | Controller / Wiring Intent Checklist | Partial | Unit Tested Metadata | Partial Parity | Intent report maps start/stop protection and scheduler callback blockers to future hook targets. It does not invoke scheduler/callback execution, visual mutation, packet fanout, AI notification, first-action stop hooks, or inbound-damage guard behavior. |
| `com.aionemu.gameserver.controllers.CreatureController` | wiring intent report task-map/lifecycle hook rows | Controller / Task Map Wiring Intent | Partial | Unit Tested Metadata | Needs Verification | Intent report maps future controller-owned task-map storage/cancellation and lifecycle cleanup hook targets. Production controller owner/delete/logout hooks remain unwired; Java `ConcurrentHashMap` race behavior is unverified. |
| `com.aionemu.gameserver.utils.ThreadPoolManager` | wiring intent report scheduler callback hook row | Scheduler / Wiring Intent | Partial | Unit Tested Metadata | Needs Verification | Intent report keeps live scheduler callback execution blocked. Java scheduled timing and callback/cancel races remain unverified. |
| `com.aionemu.gameserver.model.TaskId` | wiring intent report task key metadata | Enum / Task Key Wiring Intent | Partial | Unit Tested Metadata | Needs Verification | Intent report inherits `PROTECTION_ACTIVE` evidence from closure/aggregate rows only. Explorer confirmed Java ordinal 3; full enum behavior remains incomplete. |
| `java.util.concurrent.ScheduledFuture` / `Future` | wiring intent report runtime comparison row | Future / Runtime Gate Intent | Partial | Unit Tested Metadata | Needs Verification | Intent report blocks implementation on runtime comparison for `Future.cancel(false)`, scheduled callback timing, and concurrent task-map behavior. |
| `com.aionemu.gameserver.network.aion.clientpackets.*` first-action protection stop callers | wiring intent report notes only | Packet Handler / Future Hook Surface | Not Started | No Tests | Unknown | Explorer identified movement, air movement, attack, cast, item-use, dialog/show-dialog, emotion, and composite-stone stop-trigger surfaces. No C# wiring or tests were added in this unit. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Create_FullClosureMapsToBlockedProductionHooks` | Unit / wiring intent metadata | closure rows from Java protection lifecycle metadata | Full closure report maps to blocked start storage, scheduler callback, lifecycle, side-effect, and runtime hook rows. | Deterministic C# intent checklist over reviewed Java-source-shaped metadata. | No production hook execution or Java runtime comparison. |
| `Create_AlreadyProtectedPathDoesNotRequestSchedulerOrTaskStorageHooks` | Unit / wiring intent metadata | `PlayerController.startProtectionActiveTask` already-protected guard | Already-protected path marks scheduler/storage hooks skipped and not requested. | Deterministic Java source-derived guard assertion. | No runtime comparison. |
| `Create_MissingDelayedPreviewBlocksBeforeStopCancellationHookWork` | Unit / wiring intent metadata | delayed-stop callback preview prerequisite | Missing delayed preview blocks stop-cancellation hook work before requesting implementation. | Deterministic checklist assertion. | No production callback wiring. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Full game-server test suite is still known to have two unrelated stable failures in `GameServerConnectionInventoryExpansionUseItemTests`: `ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate` and `HandleUseItemAsync_ExpExtractMergesRestrictedRewardWithCleanupSealFlag`.
- Wiring intent report is metadata-only and is not consumed by production code.
- No production C# controller task-map owner, delete/logout hook, scheduler callback execution, socket fanout, known-list mutation, AI move notification, first-action stop hook, inbound-damage guard, aggro suppression, target/skill rejection, or material-skill suppression integration exists.
- Java `Future.cancel(false)`, `ScheduledFuture`, callback timing, movement stop-trigger conditions, and `ConcurrentHashMap` race behavior remain unverified by runtime comparison.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 1 non-live wiring intent report service and 1 focused test suite
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows plus 1 unknown future hook surface row
- Total blocked artifacts: Java runtime artifact generation, production controller owner wiring, production lifecycle hook, live scheduler callback execution, live delayed stop side effects, first-action stop hooks, inbound-damage guard, aggro/target/skill suppression, socket fanout, known-list mutations, AI move notification, Java/C# future/concurrent-map race comparison, unrelated full-suite cleanup-seal failures
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: add a non-live first-action protection stop-trigger surface audit.
- Start with Java `CM_MOVE` movement-threshold semantics.
- Catalog Java packet handlers/callers that invoke `stopProtectionActiveTask`.
- Keep all production handler wiring disabled.

## Suggested Acceptance Criteria

- New audit/report identifies Java caller categories and at least the `CM_MOVE` movement threshold details.
- Report distinguishes:
  - movement threshold stop trigger;
  - other packet/action stop triggers pending deeper audits;
  - production wiring blockers.
- Tests cover:
  - `CM_MOVE` x/y change trigger;
  - `CM_MOVE` z-drop threshold behavior from Java;
  - non-trigger movement branch remains skipped;
  - other discovered packet handlers are listed as pending.
- Existing wiring intent, closure, delayed callback preview, scheduler callback, owner prototype, readiness aggregate, lifecycle cleanup, scheduled-handle, task-map simulation, adapter, audit, readiness, summary, task-operation, planner, side-effect, bridge, fanout, trace, executor, report, plan, and player-state tests continue to pass.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | First-action stop-trigger surface audit | new audit service/tests | Medium | Safe if it remains metadata-only and does not alter packet handlers. |
| B | Java `CM_MOVE` deep read-only analysis | read-only Java source | Low | Can be parallel if docs ownership is isolated. |
| C | Inventory cleanup-seal failure triage | inventory item-use tests/services | Medium | Separate current full-suite blocker. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Add first-action stop-trigger surface audit and tests | new audit service/tests, progress/handoff docs | production packet handlers, movement runtime, scheduler implementation |
| Explorer | Read-only Java `CM_MOVE` and stop-trigger caller analysis | read-only Java source inspection only | all writes, shared docs, C# files |

Shared docs remain orchestrator-owned. Do not assign multiple agents to the new audit service/test pair.

## Do Not Parallelize

- Production packet handler wiring.
- Movement runtime changes.
- Shared scheduler implementation.
- Production protection bridge/adapter execution paths.
- Shared progress/handoff docs.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1530] Add protection wiring intent report`.
- Files changed in UOW-1530:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerProtectionActiveTaskControllerTaskMapWiringIntentReportService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerProtectionActiveTaskControllerTaskMapWiringIntentReportServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6ANB-Completion.md`
- Latest prior commits:
  - `65bb4d411 [Phase 6][UOW-1529] Add protection lifecycle closure report`
  - `921040c33 [Phase 6][UOW-1528] Compose protection delayed callback into readiness aggregate`
  - `81ecd491d [Phase 6][UOW-1527] Add protection delayed stop callback preview`
- Required startup remains:
  - read `docs/csharp-port.md`;
  - read `docs/PHASE-6-PROGRESS.md`;
  - read latest `docs/Phase-6*-Completion.md`;
  - read `docs/orchestration-rules.md`;
  - read `docs/parallelization-strategy.md`;
  - read `docs/parity-verification.md`;
  - note `docs/commit-conventions.md` is still missing.
