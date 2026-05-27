# Phase 6ANF Completion - Protection Item/Dialog Stop Trigger Audit

Date: 2026-05-27
Unit of Work: UOW-1534
Status: Complete after validation.

## Scope

Continue the non-live action-packet stop-trigger audit with `CM_USE_ITEM`, `CM_SHOW_DIALOG`, and `CM_DIALOG_SELECT`, capturing Java guard order before `stopProtectionActiveTask` while keeping production packet handlers disabled.

## Completed Work

- Updated `PlayerProtectionActiveTaskFirstActionStopTriggerAuditService`.
- Added opt-in detailed `CM_USE_ITEM` evaluation.
- Added opt-in detailed `CM_SHOW_DIALOG` evaluation.
- Added opt-in detailed `CM_DIALOG_SELECT` evaluation.
- Preserved default pending-caller catalog behavior for callers that are not explicitly evaluated.
- Kept `CM_COMPOSITE_STONES` and `CM_EMOTION` as pending detailed audits.
- A read-only explorer inspected Java `CM_COMPOSITE_STONES` and `CM_EMOTION`, then was closed. No subagent edits were integrated.
- No production C# packet handler wiring, inventory/dialog runtime change, scheduler callback execution, controller task-map owner, socket fanout, known-list mutation, AI move notification, inbound-damage guard, or protection bridge execution was wired.

## Java Source Notes

`CM_USE_ITEM.runImpl`:

```java
Player player = getConnection().getActivePlayer();

if (player.isProtectionActive())
	player.getController().stopProtectionActiveTask();

Item item = player.getInventory().getItemByObjId(uniqueItemId);
if (item == null)
	return;
```

Important `CM_USE_ITEM` ordering:
- Protection stop happens before source item lookup.
- Stop also happens before target item/equipment/house-object lookup, casting cancellation, `PlayerRestrictions.canUseItem`, quest item-use callback, cooldown registration, observer notification, and item action execution.

`CM_SHOW_DIALOG.runImpl`:

```java
Player player = getConnection().getActivePlayer();
if (player.isProtectionActive())
	player.getController().stopProtectionActiveTask();

if (player.isTrading())
	return;
```

Important `CM_SHOW_DIALOG` ordering:
- Protection stop happens before trading guard.
- Stop also happens before known-list NPC lookup, hidden-state removal, and `onDialogRequest`.

`CM_DIALOG_SELECT.runImpl`:

```java
Player player = getConnection().getActivePlayer();
if (player.isProtectionActive())
	player.getController().stopProtectionActiveTask();

if (player.isTrading())
	return;
```

Important `CM_DIALOG_SELECT` ordering:
- Protection stop happens before trading guard.
- Stop also happens before admin dialog-info message, action-name lookup, self/quest handling, NPC validation, and controller `onDialogSelect`.

Explorer notes for next work:
- `CM_COMPOSITE_STONES` has only `player == null` before stop; protection stop is early and occurs before casting cancellation, inventory lookup, restrictions, validation, and scheduling.
- `CM_EMOTION` stops late at the end after many guards and possible side effects; many early-return paths do not stop protection.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter PlayerProtectionActiveTaskFirstActionStopTriggerAuditServiceTests`.
- Result: passed 31 tests.
- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "PlayerProtectionActiveTaskFirstActionStopTriggerAuditServiceTests|PlayerProtectionActiveTaskControllerTaskMapWiringIntentReportServiceTests|PlayerProtectionActiveTaskLifecycleClosureReportServiceTests|PlayerProtectionActiveTaskDelayedStopCallbackPreviewServiceTests|PlayerProtectionActiveTaskSchedulerCallbackPlanServiceTests|PlayerProtectionActiveTaskReadinessAggregateServiceTests|PlayerProtectionActiveTaskControllerTaskMapOwnerPrototypeServiceTests|PlayerProtectionActiveTaskTaskMapSimulationServiceTests|PlayerProtectionActiveTaskTaskMapAdapterServiceTests|PlayerProtectionActiveTaskTaskOperationPlanServiceTests|PlayerProtectionActiveTaskAdapterServiceTests|PlayerProtectionActiveTaskExecutionBridgeServiceTests|PlayerProtectionActiveTaskExecutionSummaryServiceTests|PlayerProtectionActiveTaskReportServiceTests|PlayerProtectionActiveTaskPlanServiceTests|PlayerProtectionActiveTaskFanoutServiceTests|PlayerProtectionActiveTaskSightedRecipientTraceServiceTests|PlayerProtectionActiveTaskLiveReadinessServiceTests|PlayerStateTests"`.
- Result: passed 131 tests.

## Migration Parity Table - UOW-1534

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_USE_ITEM` | `Aion.GameServer.Services.PlayerProtectionActiveTaskFirstActionStopTriggerAuditService` | Packet Handler / Item-Use Stop Trigger Audit | Partial | Unit Tested Metadata | Partial Parity | Audit models protection stop immediately after active-player resolution and before item lookup, target lookup, casting cancellation, restrictions, quest callbacks, cooldowns, observers, and action execution. Production use-item handler behavior is not changed. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_SHOW_DIALOG` | `Aion.GameServer.Services.PlayerProtectionActiveTaskFirstActionStopTriggerAuditService` | Packet Handler / Show-Dialog Stop Trigger Audit | Partial | Unit Tested Metadata | Partial Parity | Audit models protection stop before trading guard, NPC lookup, hide removal, and `onDialogRequest`. Production show-dialog handler behavior is not changed. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT` | `Aion.GameServer.Services.PlayerProtectionActiveTaskFirstActionStopTriggerAuditService` | Packet Handler / Dialog-Select Stop Trigger Audit | Partial | Unit Tested Metadata | Partial Parity | Audit models protection stop before trading guard, admin dialog-info message, action-name lookup, quest/self handling, NPC validation, and controller `onDialogSelect`. Production dialog-select handler behavior is not changed. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_COMPOSITE_STONES` | first-action stop trigger audit pending row / explorer notes | Packet Handler / Composition Stop Trigger | Not Started | Manual Only | Needs Verification | Explorer confirmed only `player == null` guard before an early protection stop; detailed audit rows and tests remain pending. Stop happens before casting cancellation, inventory lookup, restrictions, validation, and scheduling. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_EMOTION` | first-action stop trigger audit pending row / explorer notes | Packet Handler / Emotion Stop Trigger | Not Started | Manual Only | Needs Verification | Explorer confirmed late stop after emotion processing, optional broadcast, and many early-return exclusions. Detailed audit rows and tests remain pending. |
| `com.aionemu.gameserver.controllers.PlayerController` | first-action stop trigger audit inventory/dialog rows | Controller / Stop Protection Boundary | Partial | Unit Tested Metadata | Needs Verification | Audit records future calls into `stopProtectionActiveTask` only. Production controller task-map cancellation, visual/socket/AI side effects, threading, and scheduler interactions remain unwired. |
| `com.aionemu.gameserver.model.gameobjects.player.Player` | first-action stop trigger audit request flags | Model / State Boundary | Partial | Unit Tested Metadata | Needs Verification | Audit represents protection-active state only. Java item/dialog handlers also depend on inventory, equipment, active house, casting, restrictions, trading, admin access, known-list, quest, NPC, and dialog-action state not implemented here. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Create_UseItemAndDialogPacketsStopBeforeJavaValidation` | Unit / audit metadata | `CM_USE_ITEM.runImpl`, `CM_SHOW_DIALOG.runImpl`, `CM_DIALOG_SELECT.runImpl` | Active protection stop is recorded before Java validation/lookup branches for use-item, show-dialog, and dialog-select packets. | Deterministic Java source-derived ordering assertion. | No live packet handler or Java runtime comparison. |
| `Create_UseItemAndDialogPacketsInactiveProtectionSkipsStop` | Unit / audit metadata | `CM_USE_ITEM.runImpl`, `CM_SHOW_DIALOG.runImpl`, `CM_DIALOG_SELECT.runImpl` | Inactive protection skips stop and continues into later Java validation branches. | Deterministic Java source-derived branch assertion. | Does not model all downstream validation outcomes. |
| `Create_RemainingCompositeAndEmotionCallersStayPendingAfterUseItemAndDialogAudit` | Unit / audit metadata | direct `stopProtectionActiveTask` caller catalog plus explorer notes | `CM_COMPOSITE_STONES` and `CM_EMOTION` remain explicit pending detailed audits. | Deterministic catalog assertion. | Detailed rows/tests deferred to next UOW. |
| Existing movement, air-movement, attack, cast, and pending-caller audit tests | Unit / regression metadata | `CM_MOVE`, `CM_MOVE_IN_AIR`, `CM_ATTACK`, `CM_CASTSPELL`, direct caller search | Existing detailed audit behavior remains stable. | Regression coverage in 31 focused tests and 131 protection-slice tests. | Production wiring remains disabled. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Full game-server test suite is still known to have two unrelated stable failures in `GameServerConnectionInventoryExpansionUseItemTests`: `ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate` and `HandleUseItemAsync_ExpExtractMergesRestrictedRewardWithCleanupSealFlag`.
- First-action stop trigger audit is metadata-only and is not consumed by production code.
- `CM_USE_ITEM`, `CM_SHOW_DIALOG`, and `CM_DIALOG_SELECT` parity is source-derived and unit-tested in C# metadata only; none are verified against Java runtime packet traces.
- Detailed `CM_COMPOSITE_STONES` and `CM_EMOTION` audit rows remain pending. Their stop timing differs significantly: composition is early after null-player guard; emotion is late after many guards and side effects.
- No production C# packet handler stop hook, inventory/dialog runtime, quest/dialog service live dispatch, known-list lookup, controller task-map owner, delete/logout hook, scheduler callback execution, socket fanout, known-list mutation, AI move notification, inbound-damage guard, aggro suppression, target/skill rejection, or material-skill suppression integration exists.
- Java `Future.cancel(false)`, `ScheduledFuture`, callback timing, action-packet stop-trigger runtime ordering, and `ConcurrentHashMap` race behavior remain unverified by runtime comparison.

## Summary Metrics

- Total Java artifacts discovered: 7 grouped artifact rows in this unit
- Total artifacts ported: 1 non-live first-action stop-trigger audit enhancement and focused test coverage
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 7 grouped rows
- Total blocked artifacts: Java runtime artifact generation, production packet handler stop hooks, production inventory/dialog/runtime dispatch, production controller owner wiring, production lifecycle hook, live scheduler callback execution, live delayed stop side effects, inbound-damage guard, aggro/target/skill suppression, socket fanout, known-list mutations, AI move notification, Java/C# future/concurrent-map/action-packet runtime comparison, unrelated full-suite cleanup-seal failures
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: continue the non-live action-packet stop-trigger audit with detailed `CM_COMPOSITE_STONES` and `CM_EMOTION` rows.
- Capture early-after-null composition stop separately from late-after-emotion-processing stop.
- Preserve `CM_EMOTION` early-return exclusions instead of modeling it as an unconditional stop packet.
- Keep production packet handlers disabled.

## Suggested Acceptance Criteria

- Audit distinguishes `CM_COMPOSITE_STONES` from pending rows:
  - null-player guard skips stop;
  - active protection stops before casting cancellation, inventory lookup, restrictions, validation, and scheduling;
  - inactive protection skips stop and proceeds into later composition flow.
- Audit distinguishes `CM_EMOTION` from pending rows:
  - at least dead/abnormal/private-shop/select-target/stance or equivalent representative early-return exclusions are modeled as no stop;
  - successful late path can stop after emotion processing/broadcast metadata;
  - row notes make clear that many side effects can happen before stop.
- Existing first-action audit, wiring intent, closure, delayed callback preview, scheduler callback, owner prototype, readiness aggregate, lifecycle cleanup, scheduled-handle, task-map simulation, adapter, audit, readiness, summary, task-operation, planner, side-effect, bridge, fanout, trace, executor, report, plan, and player-state tests continue to pass.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | `CM_COMPOSITE_STONES`/`CM_EMOTION` audit | first-action audit service/tests | Medium | Keep metadata-only and avoid production packet handlers. |
| B | Read-only Java observer/runtime comparison design | `PlayerController.stopProtectionActiveTask`, `CreatureController`, packet callers | Low | Can run in parallel if docs remain orchestrator-owned. |
| C | Inventory cleanup-seal failure triage | inventory item-use tests/services | Medium | Separate current full-suite blocker. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Add `CM_COMPOSITE_STONES`/`CM_EMOTION` detailed stop ordering audit and tests | first-action audit service/tests, progress/handoff docs | production packet handlers, emotion/composition runtime, scheduler implementation |
| Explorer | Read-only runtime comparison design notes | read-only Java source inspection only | all writes, shared docs, C# files |

Shared docs remain orchestrator-owned. Do not assign multiple agents to the same audit service/test pair.

## Do Not Parallelize

- Production packet handler wiring.
- Emotion/composition runtime changes.
- Shared scheduler implementation.
- Production protection bridge/adapter execution paths.
- Shared progress/handoff docs.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1534] Add protection item dialog stop audit`.
- Files changed in UOW-1534:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerProtectionActiveTaskFirstActionStopTriggerAuditService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerProtectionActiveTaskFirstActionStopTriggerAuditServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6ANF-Completion.md`
- Latest prior commits:
  - `0756f0e16 [Phase 6][UOW-1533] Add protection action stop audit`
  - `abadcda57 [Phase 6][UOW-1532] Deepen protection air-move stop audit`
  - `b1b58e464 [Phase 6][UOW-1531] Add protection first-action stop trigger audit`
- Required startup remains:
  - read `docs/csharp-port.md`;
  - read `docs/PHASE-6-PROGRESS.md`;
  - read latest `docs/Phase-6*-Completion.md`;
  - read `docs/orchestration-rules.md`;
  - read `docs/parallelization-strategy.md`;
  - read `docs/parity-verification.md`;
  - note `docs/commit-conventions.md` is still missing.
