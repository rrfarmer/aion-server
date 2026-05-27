# Phase 6AND Completion - Protection Air-Move Stop Trigger Audit

Date: 2026-05-27
Unit of Work: UOW-1532
Status: Complete after validation.

## Scope

Deepen the non-live first-action stop-trigger audit for `CM_MOVE_IN_AIR`, including spawned/flying guard ordering and the unconditional protection stop before `World.updatePosition`, while keeping production packet handlers disabled.

## Completed Work

- Updated `PlayerProtectionActiveTaskFirstActionStopTriggerAuditService`.
- Added opt-in detailed `CM_MOVE_IN_AIR` evaluation.
- Preserved default pending-caller catalog behavior for broader first-action audit reports.
- Modeled Java `CM_MOVE_IN_AIR.runImpl` ordering:
  - not spawned returns before flying/protection/distance/world-position handling;
  - not flying returns before protection stop, distance update, and world-position handling;
  - spawned + flying + inactive protection skips stop;
  - spawned + flying + active protection stops unconditionally;
  - stop happens before `World.updatePosition`, `onMoveFromClient`, and `onMove`.
- Added report evidence flag `HasCmMoveInAirOrderingEvidence`.
- No production C# packet handler wiring, movement runtime change, scheduler callback execution, controller task-map owner, socket fanout, known-list mutation, AI move notification, inbound-damage guard, or protection bridge execution was wired.

## Java Source Notes

`CM_MOVE_IN_AIR.runImpl` order:

```java
if (!player.isSpawned())
	return;
if (!player.isInState(CreatureState.FLYING))
	return;

if (player.getFlightPath() != null)
	player.getFlightPath().setDistance(distance);

if (player.isProtectionActive())
	player.getController().stopProtectionActiveTask();

World.getInstance().updatePosition(player, x, y, z, heading);
player.getMoveController().onMoveFromClient();
player.getController().onMove();
```

Important parity notes:
- The stop condition has no coordinate threshold.
- The stop is unconditional once spawned/flying/protection-active checks pass.
- The flight-path distance update occurs before protection stop when a flight path exists.
- The stop occurs before world-position mutation and movement callbacks.
- Future C# production wiring must preserve guard ordering and side-effect ordering.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter PlayerProtectionActiveTaskFirstActionStopTriggerAuditServiceTests`.
- Result: passed 15 tests.
- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "PlayerProtectionActiveTaskFirstActionStopTriggerAuditServiceTests|PlayerProtectionActiveTaskControllerTaskMapWiringIntentReportServiceTests|PlayerProtectionActiveTaskLifecycleClosureReportServiceTests|PlayerProtectionActiveTaskDelayedStopCallbackPreviewServiceTests|PlayerProtectionActiveTaskSchedulerCallbackPlanServiceTests|PlayerProtectionActiveTaskReadinessAggregateServiceTests|PlayerProtectionActiveTaskControllerTaskMapOwnerPrototypeServiceTests|PlayerProtectionActiveTaskTaskMapSimulationServiceTests|PlayerProtectionActiveTaskTaskMapAdapterServiceTests|PlayerProtectionActiveTaskTaskOperationPlanServiceTests|PlayerProtectionActiveTaskAdapterServiceTests|PlayerProtectionActiveTaskExecutionBridgeServiceTests|PlayerProtectionActiveTaskExecutionSummaryServiceTests|PlayerProtectionActiveTaskReportServiceTests|PlayerProtectionActiveTaskPlanServiceTests|PlayerProtectionActiveTaskFanoutServiceTests|PlayerProtectionActiveTaskSightedRecipientTraceServiceTests|PlayerProtectionActiveTaskLiveReadinessServiceTests|PlayerStateTests"`.
- Result: passed 115 tests.

## Migration Parity Table - UOW-1532

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_MOVE_IN_AIR` | `Aion.GameServer.Services.PlayerProtectionActiveTaskFirstActionStopTriggerAuditService` | Packet Handler / Air Movement Stop Trigger Audit | Partial | Unit Tested Metadata | Partial Parity | Audit now models spawned/flying guard ordering and unconditional protection stop before `World.updatePosition`, `onMoveFromClient`, and `onMove`. Production packet handler and real movement runtime remain unwired. |
| `com.aionemu.gameserver.controllers.PlayerController` | first-action stop trigger audit `CM_MOVE_IN_AIR` stop row | Controller / Stop Protection Boundary | Partial | Unit Tested Metadata | Needs Verification | Audit records future call into `stopProtectionActiveTask`; production controller task-map cancellation, visual/socket/AI side effects, threading, and scheduler interactions remain unwired. |
| `com.aionemu.gameserver.world.World` | first-action stop trigger audit ordering note | World Position Update Boundary | Not Started | Unit Tested Metadata | Needs Verification | Audit documents that Java stops protection before `World.updatePosition`; no C# world-position mutation or packet movement runtime was changed. |
| `com.aionemu.gameserver.controllers.movement.PlayerMoveController` | first-action stop trigger audit ordering note | Movement Controller Boundary | Not Started | Unit Tested Metadata | Needs Verification | Audit documents that Java stops protection before `onMoveFromClient`; no C# movement controller behavior was implemented. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Create_CmMoveInAirSpawnedFlyingProtectedStopsBeforeWorldUpdate` | Unit / audit metadata | `CM_MOVE_IN_AIR.runImpl` | Spawned/flying/protected path reaches unconditional stop before `World.updatePosition`. | Deterministic Java source-derived ordering assertion. | No live C# packet handler or Java runtime comparison. |
| `Create_CmMoveInAirNotSpawnedSkipsBeforeFlyingAndProtection` | Unit / audit metadata | `CM_MOVE_IN_AIR.runImpl` spawned guard | Not spawned returns before flying/protection/distance/world-position handling. | Deterministic Java source-derived guard assertion. | No live movement packet replay. |
| `Create_CmMoveInAirNotFlyingSkipsBeforeProtectionStop` | Unit / audit metadata | `CM_MOVE_IN_AIR.runImpl` flying guard | Not flying skips protection stop and later movement handling. | Deterministic Java source-derived guard assertion. | No live movement packet replay. |
| `Create_CmMoveInAirInactiveProtectionSkipsAfterSpawnedFlyingGuards` | Unit / audit metadata | `CM_MOVE_IN_AIR.runImpl` protection guard | Spawned/flying/inactive-protection path skips `stopProtectionActiveTask`. | Deterministic Java source-derived guard assertion. | No runtime comparison. |
| Existing `CM_MOVE` and pending-caller audit tests | Unit / regression metadata | `CM_MOVE.runImpl` and caller search | Existing `CM_MOVE` threshold and pending action caller behavior remains stable. | Regression coverage in 15 focused tests and 115 protection-slice tests. | Production wiring remains disabled. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Full game-server test suite is still known to have two unrelated stable failures in `GameServerConnectionInventoryExpansionUseItemTests`: `ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate` and `HandleUseItemAsync_ExpExtractMergesRestrictedRewardWithCleanupSealFlag`.
- First-action stop trigger audit is metadata-only and is not consumed by production code.
- `CM_MOVE_IN_AIR` parity is source-derived and unit-tested in C# metadata only; it is not verified against Java runtime packet traces.
- Flight-path distance update ordering is documented but not yet separately modeled as a row.
- No production C# packet handler stop hook, movement runtime, world position update, movement controller callback, controller task-map owner, delete/logout hook, scheduler callback execution, socket fanout, known-list mutation, AI move notification, inbound-damage guard, aggro suppression, target/skill rejection, or material-skill suppression integration exists.
- Java `Future.cancel(false)`, `ScheduledFuture`, callback timing, movement stop-trigger runtime ordering, and `ConcurrentHashMap` race behavior remain unverified by runtime comparison.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped artifact rows in this unit
- Total artifacts ported: 1 non-live first-action stop-trigger audit enhancement and focused test coverage
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: Java runtime artifact generation, production packet handler stop hooks, production movement/world update runtime, production controller owner wiring, production lifecycle hook, live scheduler callback execution, live delayed stop side effects, inbound-damage guard, aggro/target/skill suppression, socket fanout, known-list mutations, AI move notification, Java/C# future/concurrent-map/movement runtime comparison, unrelated full-suite cleanup-seal failures
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: add a non-live action-packet stop-trigger ordering audit for `CM_ATTACK` and `CM_CASTSPELL`.
- Capture Java guard ordering before `stopProtectionActiveTask`.
- Keep production packet handlers disabled.
- Prefer extending the existing first-action audit only if it stays readable; otherwise add a companion action-packet audit service.

## Suggested Acceptance Criteria

- Audit distinguishes `CM_ATTACK` from generic pending action rows:
  - dead-player guard before stop;
  - protection-active guard before attack execution.
- Audit distinguishes `CM_CASTSPELL` from generic pending action rows:
  - dead-player guard;
  - zero-spell guard;
  - pet-order/template/passive checks before protection stop as verified from Java source.
- Tests cover:
  - `CM_ATTACK` active-protection stop after dead guard;
  - `CM_ATTACK` dead-player skip;
  - `CM_CASTSPELL` stop after reviewed preconditions;
  - representative `CM_CASTSPELL` precondition skips;
  - remaining action packet callers still listed as pending.
- Existing first-action audit, wiring intent, closure, delayed callback preview, scheduler callback, owner prototype, readiness aggregate, lifecycle cleanup, scheduled-handle, task-map simulation, adapter, audit, readiness, summary, task-operation, planner, side-effect, bridge, fanout, trace, executor, report, plan, and player-state tests continue to pass.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | `CM_ATTACK`/`CM_CASTSPELL` action stop audit | first-action audit or companion audit service/tests | Medium | Keep metadata-only and avoid production packet handlers. |
| B | Read-only Java ordering for remaining action callers | `CM_COMPOSITE_STONES`, `CM_DIALOG_SELECT`, `CM_EMOTION`, `CM_SHOW_DIALOG`, `CM_USE_ITEM` | Low | Can run in parallel if docs remain orchestrator-owned. |
| C | Inventory cleanup-seal failure triage | inventory item-use tests/services | Medium | Separate current full-suite blocker. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Add `CM_ATTACK`/`CM_CASTSPELL` action stop ordering audit and tests | first-action audit or new companion service/tests, progress/handoff docs | production packet handlers, combat/cast runtime, scheduler implementation |
| Explorer | Read-only remaining action caller ordering analysis | read-only Java source inspection only | all writes, shared docs, C# files |

Shared docs remain orchestrator-owned. Do not assign multiple agents to the same audit service/test pair.

## Do Not Parallelize

- Production packet handler wiring.
- Combat/cast runtime changes.
- Shared scheduler implementation.
- Production protection bridge/adapter execution paths.
- Shared progress/handoff docs.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1532] Deepen protection air-move stop audit`.
- Files changed in UOW-1532:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerProtectionActiveTaskFirstActionStopTriggerAuditService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerProtectionActiveTaskFirstActionStopTriggerAuditServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AND-Completion.md`
- Latest prior commits:
  - `b1b58e464 [Phase 6][UOW-1531] Add protection first-action stop trigger audit`
  - `2d87c25fa [Phase 6][UOW-1530] Add protection wiring intent report`
  - `65bb4d411 [Phase 6][UOW-1529] Add protection lifecycle closure report`
- Required startup remains:
  - read `docs/csharp-port.md`;
  - read `docs/PHASE-6-PROGRESS.md`;
  - read latest `docs/Phase-6*-Completion.md`;
  - read `docs/orchestration-rules.md`;
  - read `docs/parallelization-strategy.md`;
  - read `docs/parity-verification.md`;
  - note `docs/commit-conventions.md` is still missing.
