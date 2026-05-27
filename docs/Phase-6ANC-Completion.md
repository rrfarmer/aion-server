# Phase 6ANC Completion - Protection First-Action Stop Trigger Audit

Date: 2026-05-27
Unit of Work: UOW-1531
Status: Complete after validation.

## Scope

Add a non-live first-action protection stop-trigger audit that catalogs Java packet handlers/callers which invoke `stopProtectionActiveTask`, starting with `CM_MOVE` movement-threshold semantics and keeping all production handler wiring disabled.

## Completed Work

- Added `PlayerProtectionActiveTaskFirstActionStopTriggerAuditService`.
- Added source/status/kind enums, report rows, and report record.
- Modeled Java `CM_MOVE.runImpl` stop-trigger behavior:
  - Java call is reachable only when spawned, anti-hack accepted, not teleportation-mode absolute movement, and protection is active.
  - exact `x` inequality stops protection.
  - exact `y` inequality stops protection.
  - asymmetric Z threshold stops only when old server `z > packet z + 0.5f`.
  - unchanged `x/y` with no qualifying downward Z delta keeps protection active.
- Cataloged `CM_MOVE_IN_AIR` as a pending unconditional stop caller after spawned/flying guards.
- Cataloged direct action packet callers as pending class-specific audits:
  - `CM_ATTACK`;
  - `CM_CASTSPELL`;
  - `CM_COMPOSITE_STONES`;
  - `CM_DIALOG_SELECT`;
  - `CM_EMOTION`;
  - `CM_SHOW_DIALOG`;
  - `CM_USE_ITEM`.
- Added a production boundary row that keeps all live packet-handler integration disabled.
- A read-only explorer inspected Java packet handlers and direct callers. No subagent edits were integrated.
- No production C# packet handler wiring, movement runtime change, scheduler callback execution, controller task-map owner, socket fanout, known-list mutation, AI move notification, first-action stop hook, inbound-damage guard, or protection bridge execution was wired.

## Java Source Notes

- `CM_MOVE.runImpl` stops protection only after movement is accepted and the player is spawned:

```java
if (player.isProtectionActive() && (player.getX() != x || player.getY() != y || player.getZ() > z + 0.5f))
	player.getController().stopProtectionActiveTask();
```

- Java uses exact float inequality for `x` and `y`; do not add an epsilon in a future C# packet handler unless documented as an intentional difference.
- Java Z behavior is asymmetric. Old server Z greater than packet Z plus `0.5f` stops protection; upward movement does not stop by Z alone.
- `CM_MOVE_IN_AIR.runImpl` stops unconditionally when the player is spawned, flying, and protection is active, before `World.updatePosition`.
- Direct Java caller search found `CM_ATTACK`, `CM_CASTSPELL`, `CM_COMPOSITE_STONES`, `CM_DIALOG_SELECT`, `CM_EMOTION`, `CM_SHOW_DIALOG`, `CM_USE_ITEM`, `CM_MOVE`, and `CM_MOVE_IN_AIR`.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter PlayerProtectionActiveTaskFirstActionStopTriggerAuditServiceTests`.
- Result: passed 11 tests.
- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "PlayerProtectionActiveTaskFirstActionStopTriggerAuditServiceTests|PlayerProtectionActiveTaskControllerTaskMapWiringIntentReportServiceTests|PlayerProtectionActiveTaskLifecycleClosureReportServiceTests|PlayerProtectionActiveTaskDelayedStopCallbackPreviewServiceTests|PlayerProtectionActiveTaskSchedulerCallbackPlanServiceTests|PlayerProtectionActiveTaskReadinessAggregateServiceTests|PlayerProtectionActiveTaskControllerTaskMapOwnerPrototypeServiceTests|PlayerProtectionActiveTaskTaskMapSimulationServiceTests|PlayerProtectionActiveTaskTaskMapAdapterServiceTests|PlayerProtectionActiveTaskTaskOperationPlanServiceTests|PlayerProtectionActiveTaskAdapterServiceTests|PlayerProtectionActiveTaskExecutionBridgeServiceTests|PlayerProtectionActiveTaskExecutionSummaryServiceTests|PlayerProtectionActiveTaskReportServiceTests|PlayerProtectionActiveTaskPlanServiceTests|PlayerProtectionActiveTaskFanoutServiceTests|PlayerProtectionActiveTaskSightedRecipientTraceServiceTests|PlayerProtectionActiveTaskLiveReadinessServiceTests|PlayerStateTests"`.
- Result: passed 111 tests.

## Migration Parity Table - UOW-1531

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_MOVE` | `Aion.GameServer.Services.PlayerProtectionActiveTaskFirstActionStopTriggerAuditService` | Packet Handler / Movement Stop Trigger Audit | Partial | Unit Tested Metadata | Partial Parity | Audit models Java's exact `x/y` float inequality and asymmetric `oldZ > packetZ + 0.5f` threshold after spawned/anti-hack/non-teleport/active-protection prerequisites. Production packet handler is not wired; Java runtime float comparison is not externally verified. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_MOVE_IN_AIR` | first-action stop trigger audit pending row | Packet Handler / Air Movement Stop Trigger | Partial | Unit Tested Metadata | Needs Verification | Audit catalogs unconditional stop after spawned/flying guards. Detailed C# ordering and production wiring remain pending. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_ATTACK` | first-action stop trigger audit pending row | Packet Handler / Action Stop Trigger | Not Started | Unit Tested Metadata | Unknown | Direct caller discovered and listed as pending; exact order after dead-player guard still needs class-specific parity work. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_CASTSPELL` | first-action stop trigger audit pending row | Packet Handler / Action Stop Trigger | Not Started | Unit Tested Metadata | Unknown | Direct caller discovered and listed as pending; dead, zero-spell, pet-order, template, and passive ordering still need class-specific parity work. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_COMPOSITE_STONES` | first-action stop trigger audit pending row | Packet Handler / Action Stop Trigger | Not Started | Unit Tested Metadata | Unknown | Direct caller discovered and listed as pending; exact null-player and composition ordering still need class-specific parity work. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT` | first-action stop trigger audit pending row | Packet Handler / Action Stop Trigger | Not Started | Unit Tested Metadata | Unknown | Direct caller discovered and listed as pending; trading/dialog validation ordering still needs class-specific parity work. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_EMOTION` | first-action stop trigger audit pending row | Packet Handler / Action Stop Trigger | Not Started | Unit Tested Metadata | Unknown | Direct caller discovered and listed as pending; handled-emotion flow ordering still needs class-specific parity work. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_SHOW_DIALOG` | first-action stop trigger audit pending row | Packet Handler / Action Stop Trigger | Not Started | Unit Tested Metadata | Unknown | Direct caller discovered and listed as pending; trading/NPC validation ordering still needs class-specific parity work. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_USE_ITEM` | first-action stop trigger audit pending row | Packet Handler / Action Stop Trigger | Not Started | Unit Tested Metadata | Unknown | Direct caller discovered and listed as pending; item lookup/restriction ordering still needs class-specific parity work. |
| `com.aionemu.gameserver.controllers.PlayerController` | first-action stop trigger audit production boundary row | Controller / Stop Protection Boundary | Partial | Unit Tested Metadata | Needs Verification | Audit catalogs future calls into `stopProtectionActiveTask` only. Production controller task-map cancellation, visual/socket/AI side effects, threading, and scheduler interactions remain unwired. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Create_CmMoveXChangeStopsProtection` | Unit / audit metadata | `CM_MOVE.runImpl` movement condition | Exact `x` delta marks stop trigger reached. | Deterministic Java source-derived threshold assertion. | No live C# packet handler or Java runtime comparison. |
| `Create_CmMoveYChangeStopsProtection` | Unit / audit metadata | `CM_MOVE.runImpl` movement condition | Exact `y` delta marks stop trigger reached. | Deterministic Java source-derived threshold assertion. | No live C# packet handler or Java runtime comparison. |
| `Create_CmMoveUsesJavaAsymmetricZDropThreshold` | Unit / audit metadata | `CM_MOVE.runImpl` `player.getZ() > z + 0.5f` | Z drop greater than `0.5f` stops; exactly `0.5f` and upward movement do not stop. | Deterministic Java source-derived threshold assertion. | Java float precision/runtime artifact not generated. |
| `Create_CmMoveSamePositionHeadingTurnSkipsStop` | Unit / audit metadata | `CM_MOVE.runImpl` same-position branch | Same `x/y` and no qualifying Z drop keeps protection active. | Deterministic Java source-derived skip assertion. | No live heading/movement packet replay. |
| `Create_CmMoveEarlyJavaBranchesSkipStop` | Unit / audit metadata | `CM_MOVE.runImpl` ordering before stop condition | Not spawned, failed anti-hack, teleportation absolute move, or inactive protection skip stop trigger even with x delta. | Deterministic Java source-derived ordering assertion. | Does not execute actual anti-hack or teleport code. |
| `Create_ListsMoveInAirAndActionPacketCallersAsPending` | Unit / audit metadata | direct `stopProtectionActiveTask` caller search | Catalogs `CM_MOVE_IN_AIR` and action packet callers as pending audits. | Deterministic source catalog assertion. | Class-specific order and live wiring remain pending. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Full game-server test suite is still known to have two unrelated stable failures in `GameServerConnectionInventoryExpansionUseItemTests`: `ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate` and `HandleUseItemAsync_ExpExtractMergesRestrictedRewardWithCleanupSealFlag`.
- First-action stop trigger audit is metadata-only and is not consumed by production code.
- `CM_MOVE` parity is source-derived and unit-tested in C# metadata only; it is not verified against a Java runtime trace.
- Exact Java float inequality is preserved in the audit; any future C# packet-level implementation must avoid adding epsilon unless documented as an intentional difference.
- No production C# packet handler stop hook, movement runtime, controller task-map owner, delete/logout hook, scheduler callback execution, socket fanout, known-list mutation, AI move notification, inbound-damage guard, aggro suppression, target/skill rejection, or material-skill suppression integration exists.
- Java `Future.cancel(false)`, `ScheduledFuture`, callback timing, movement stop-trigger runtime ordering, and `ConcurrentHashMap` race behavior remain unverified by runtime comparison.

## Summary Metrics

- Total Java artifacts discovered: 10 grouped artifact rows in this unit
- Total artifacts ported: 1 non-live first-action stop-trigger audit service and 1 focused test suite
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 3 grouped rows with partial/needs-verification status plus 7 pending/unknown caller rows
- Total blocked artifacts: Java runtime artifact generation, production packet handler stop hooks, production controller owner wiring, production lifecycle hook, live scheduler callback execution, live delayed stop side effects, inbound-damage guard, aggro/target/skill suppression, socket fanout, known-list mutations, AI move notification, Java/C# future/concurrent-map/movement runtime comparison, unrelated full-suite cleanup-seal failures
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: deepen the non-live first-action stop-trigger audit for `CM_MOVE_IN_AIR`.
- Model spawned and flying guard ordering.
- Model unconditional stop when protection is active.
- Record that Java stops protection before `World.updatePosition`, `onMoveFromClient`, and `onMove`.
- Keep all production handler wiring disabled.

## Suggested Acceptance Criteria

- Existing first-action audit distinguishes `CM_MOVE_IN_AIR` from pending action packet callers.
- Tests cover:
  - spawned + flying + protection active reaches unconditional stop;
  - not spawned skips before flying/protection handling;
  - not flying skips before protection handling;
  - inactive protection skips stop after spawned/flying guards;
  - stop order is documented as before `World.updatePosition`.
- Existing first-action audit, wiring intent, closure, delayed callback preview, scheduler callback, owner prototype, readiness aggregate, lifecycle cleanup, scheduled-handle, task-map simulation, adapter, audit, readiness, summary, task-operation, planner, side-effect, bridge, fanout, trace, executor, report, plan, and player-state tests continue to pass.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | `CM_MOVE_IN_AIR` audit deepening | existing first-action audit service/tests | Medium | Safe if metadata-only and production handlers remain untouched. |
| B | Java action-packet caller read-only ordering analysis | read-only Java packet sources | Low | Can run in parallel if docs remain orchestrator-owned. |
| C | Inventory cleanup-seal failure triage | inventory item-use tests/services | Medium | Separate current full-suite blocker. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Deepen `CM_MOVE_IN_AIR` first-action stop audit and tests | `PlayerProtectionActiveTaskFirstActionStopTriggerAuditService.cs`, `PlayerProtectionActiveTaskFirstActionStopTriggerAuditServiceTests.cs`, progress/handoff docs | production packet handlers, movement runtime, scheduler implementation |
| Explorer | Read-only Java action-packet stop caller ordering analysis | read-only Java source inspection only | all writes, shared docs, C# files |

Shared docs remain orchestrator-owned. Do not assign multiple agents to the audit service/test pair.

## Do Not Parallelize

- Production packet handler wiring.
- Movement runtime changes.
- Shared scheduler implementation.
- Production protection bridge/adapter execution paths.
- Shared progress/handoff docs.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1531] Add protection first-action stop trigger audit`.
- Files changed in UOW-1531:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerProtectionActiveTaskFirstActionStopTriggerAuditService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerProtectionActiveTaskFirstActionStopTriggerAuditServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6ANC-Completion.md`
- Latest prior commits:
  - `2d87c25fa [Phase 6][UOW-1530] Add protection wiring intent report`
  - `65bb4d411 [Phase 6][UOW-1529] Add protection lifecycle closure report`
  - `921040c33 [Phase 6][UOW-1528] Compose protection delayed callback into readiness aggregate`
- Required startup remains:
  - read `docs/csharp-port.md`;
  - read `docs/PHASE-6-PROGRESS.md`;
  - read latest `docs/Phase-6*-Completion.md`;
  - read `docs/orchestration-rules.md`;
  - read `docs/parallelization-strategy.md`;
  - read `docs/parity-verification.md`;
  - note `docs/commit-conventions.md` is still missing.
