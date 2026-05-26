# Phase 6AAP Completion Handoff

Date: May 26, 2026
Latest Unit of Work: UOW-1206
Status: Phase 6 continues; bind-point teleport now has parser, action selection, operation/control packet intents, fanout intents, request-level composition, and non-live runtime task/cooldown state semantics. Live connection dispatch, real scheduler/cooldown mutation, scheduled Kinah mutation, persistent known-list parity, and movement remain disabled.

## Session Summary

UOW-1206 added `BindPointTeleportRuntimeStatePlanService`, a source-derived non-live planner for Java bind-point `TaskId.SKILL_USE` task-slot behavior and static cooldown map facts. It records `SKILL_USE` ordinal `16`, Java `CreatureController.addTask` replacement behavior, `cancelTask` remove-then-`cancel(false)` behavior, the 10 second skill-use delay, 600 second cooldown insertion, and Java whole-second cooldown time-left truncation.

Files changed:

- `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportRuntimeStatePlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/BindPointTeleportRuntimeStatePlanServiceTests.cs`
- `docs/Phase-6-BindPointTeleport-LiveFanout-Audit.md`
- `docs/Phase-6-PricesService-Consumer-Map.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6AAP-Completion.md`

## What Changed

- Added non-live task state planning:
  - schedule new `TaskId.SKILL_USE` task with Java 10 second delay;
  - replace existing task and record old-task cancel behavior;
  - cancel existing task by remove-then-`cancel(false)`;
  - no-op when action `2` has no skill-use task.
- Added non-live cooldown fact planning:
  - `addCooldown` stores `now + 600000`;
  - lookup is keyed by player object id;
  - active cooldown seconds use Java whole-second truncation;
  - expired/non-positive cooldown returns zero.
- Kept all behavior non-live and side-effect free.

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "BindPointTeleportRuntimeStatePlanServiceTests" --nologo` passed 8 tests.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport" --nologo` passed 48 tests.
- No Java runtime comparison was run, so parity remains `Needs Verification`.

## Migration Parity Table - UOW-1206

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.TaskId.SKILL_USE` | `Aion.GameServer.Services.BindPointTeleportRuntimeStatePlanService.SkillUseTaskIdName` / `SkillUseTaskIdOrdinal` | Enum / Scheduler Key Dependency | Partial | Unit Tested | Needs Verification | Non-live planner records Java enum name and ordinal `16` used by `CreatureController` task map. No live task map stores this key yet. |
| `com.aionemu.gameserver.controllers.CreatureController.addTask` | `Aion.GameServer.Services.BindPointTeleportRuntimeStatePlanService.CreateScheduleSkillUseTaskPlan` | Scheduler Dependency | Partial | Unit Tested | Needs Verification | Models Java task replacement and old-task `cancel(false)` when adding a bind-point `TaskId.SKILL_USE` task. No real `ThreadPoolManager.Schedule` or `ScheduledTask` instance is created. Threading and cancellation races remain unverified. |
| `com.aionemu.gameserver.controllers.CreatureController.cancelTask` / `hasTask` | `Aion.GameServer.Services.BindPointTeleportRuntimeStatePlanService.CreateCancelSkillUseTaskPlan` | Scheduler Dependency | Partial | Unit Tested | Needs Verification | Models Java no-op when missing and remove-then-`cancel(false)` when present. Live task lookup/cancel remains unported. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.addCooldown` | `Aion.GameServer.Services.BindPointTeleportRuntimeStatePlanService.CreateAddCooldownPlan` | Runtime State / Date-Time Dependency | Partial | Unit Tested | Needs Verification | Models Java static player-id cooldown insertion as `now + 600000`. It does not mutate a real static map; date/time parity is source-derived only. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.Cooldown.getTimeLeft` / `getCooldown` | `Aion.GameServer.Services.BindPointTeleportRuntimeStatePlanService.CreateLookupCooldownPlan` | Runtime State / Date-Time Dependency | Partial | Unit Tested | Needs Verification | Models player-id keyed lookup, whole-second truncation, and expired/non-positive zero behavior. No Java runtime clock comparison or concurrent map behavior was tested. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` scheduled task | `Aion.GameServer.Services.BindPointTeleportRuntimeStatePlanService` plus staged operation/request planners | Service / Movement Dependency | Partial | Unit Tested | Needs Verification | Task/cooldown state semantics are now modeled, but scheduled Kinah decrement, failure packet, cooldown mutation, cooldown fanout execution, final death check, and final movement remain unported. |

Tests added:

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `BindPointTeleportRuntimeStatePlanServiceTests.CreateScheduleSkillUseTaskPlan_RecordsJavaDelayAndTaskId` | Java action `1` stores a 10 second `TaskId.SKILL_USE` delayed task and records ordinal `16`. | Source-derived only. |
| `BindPointTeleportRuntimeStatePlanServiceTests.CreateScheduleSkillUseTaskPlan_ReplacesExistingJavaTaskSlot` | Java `CreatureController.addTask` cancels/replaces an existing task in the same slot. | Source-derived only. |
| `BindPointTeleportRuntimeStatePlanServiceTests.CreateCancelSkillUseTaskPlan_NoopsWhenTaskMissing` | Java `cancelTeleport` no-op when `TaskId.SKILL_USE` is absent. | Source-derived only. |
| `BindPointTeleportRuntimeStatePlanServiceTests.CreateCancelSkillUseTaskPlan_RemovesThenCancelsJavaTask` | Java `CreatureController.cancelTask` removes task then calls `cancel(false)`. | Source-derived only. |
| `BindPointTeleportRuntimeStatePlanServiceTests.CreateAddCooldownPlan_StoresJavaTenMinuteCooldown` | Java `addCooldown` stores `now + 600000` for the player. | Source-derived only. |
| `BindPointTeleportRuntimeStatePlanServiceTests.CreateLookupCooldownPlan_ActiveCooldownUsesJavaWholeSecondTruncation` | Java cooldown `getTimeLeft` whole-second truncation. | Source-derived only. |
| `BindPointTeleportRuntimeStatePlanServiceTests.CreateLookupCooldownPlan_MissingOrWrongPlayerFactReturnsNoCooldown` | Java player-id keyed cooldown map lookup. | Source-derived only. |
| `BindPointTeleportRuntimeStatePlanServiceTests.CreateLookupCooldownPlan_ExpiredCooldownReturnsZeroLikeJava` | Java expired/non-positive cooldown time returns zero. | Source-derived only. |

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 1 non-live runtime-state planner plus focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: 5 grouped categories: live connection dispatch, live scheduler/cooldown ownership, Kinah mutation/failure packet, persistent known-list parity, and final movement/death checks
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- Live `GameServerConnection` still does not dispatch `CmBindPointTeleport`.
- Runtime-state plans do not create or cancel real scheduled tasks and do not mutate a real static cooldown map.
- Java uses a plain static `HashMap` for cooldowns; C# live ownership/threading choice is still undecided and must avoid introducing unsafe shared mutable state.
- Scheduled Kinah decrement, not-enough-fee failure packet, cooldown fanout execution, final death/about-to-die recheck, and movement remain unported.
- No Java runtime comparison was executed. Reflection, serialization, date/time, threading, persistence, and movement parity remain unverified for live bind-point teleport.

## Next Work Options

### Recommended Sequential Task

- Task: Model scheduled Kinah-decrement failure intent for Java `player.getInventory().tryDecreaseKinah(price, ItemPacketService.ItemUpdateType.DEC_KINAH_FLY)` inside the bind-point skill-use task.
- Why: The scheduled task branch now has task/cooldown state metadata, but the first live side effect inside the callback is Kinah decrement. The failure branch sends `STR_CANNOT_MOVE_TO_AIRPORT_NOT_ENOUGH_FEE` and returns before cooldown/fanout/final movement, so it should be isolated before live scheduler execution.
- Required behavior:
  - input required price and current Kinah fact;
  - successful decrement intent uses `DEC_KINAH_FLY`;
  - failed decrement intent produces not-enough-fee system-message intent and stops;
  - no live inventory mutation, no packet send, no cooldown mutation, no fanout, and no movement.
- Suggested files:
  - `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportScheduledKinahPlanService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/BindPointTeleportScheduledKinahPlanServiceTests.cs`
  - docs for next unit

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Scheduled Kinah-decrement failure intent | new service/test files | Medium | Best next step before live scheduler callback. |
| B | Final death/about-to-die movement gate audit | read-only Java/C# movement/life stat files | Low/Medium | Useful after Kinah/cooldown branch. |
| C | Compose runtime-state facts into request plan metadata | existing request/runtime tests | Medium | Avoid live dispatch. |
| D | Continue `SM_SELL_ITEM` live-readiness fact assembly | new/non-overlapping sell-item adapter files and tests | Medium | Avoid shared dialog routing unless owned exclusively. |

### Do Not Parallelize

- `GameServerConnection` live bind-point dispatch: wait until scheduled Kinah failure and final movement gates are modeled.
- Shared progress/handoff/parity docs: Orchestrator should own final edits.
- `PlayerTeleportService` movement wiring: defer until scheduled callback branches are fully modeled.

## Context Needed By Next Session

- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/services/teleport/BindPointTeleportService.java`
  - `game-server/src/com/aionemu/gameserver/model/TaskId.java`
  - `game-server/src/com/aionemu/gameserver/controllers/CreatureController.java`
  - `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`
- Latest completed commits:
  - `e412743b4 [Phase 6][UOW-1205] Add bind point teleport request plan`
  - next commit should be `[Phase 6][UOW-1206] Add bind point teleport runtime state plan`
- Keep live bind-point behavior disabled until scheduled Kinah mutation/failure, cooldown mutation/fanout execution, final death/about-to-die movement, and live known-list fanout each have focused parity slices.
