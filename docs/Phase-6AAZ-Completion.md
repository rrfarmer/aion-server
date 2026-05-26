# Phase 6AAZ Completion Handoff

Date: May 26, 2026
Latest Unit of Work: UOW-1216
Status: Phase 6 continues; bind-point teleport now has parser, action selection, operation/control packet intents, fanout intents, request-level composition, runtime task/cooldown state semantics, scheduled Kinah decrement/failure intent, final movement gate intent, scheduled callback composition metadata, a `TeleportService.teleportTo` side-effect audit, a non-live teleport side-effect planner, callback-side side-effect metadata composition, a live-adapter readiness checklist, concrete failure system-message helpers, a non-live handler composition bridge, and a runtime-owner design audit. Live connection dispatch, real scheduler/cooldown mutation, live inventory mutation, persistent known-list parity, and movement remain disabled.

## Session Summary

UOW-1216 added `docs/Phase-6-BindPointTeleport-RuntimeOwner-Design.md`, a read-only design audit for the live runtime owner needed before bind-point dispatch can schedule callbacks or mutate cooldown state.

Files changed:

- `docs/Phase-6-BindPointTeleport-RuntimeOwner-Design.md`
- `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`
- `docs/Phase-6-BindPointTeleport-LiveFanout-Audit.md`
- `docs/Phase-6-PricesService-Consumer-Map.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6AAZ-Completion.md`

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport" --nologo` passed 68 tests.
- No Java runtime comparison was run, so parity remains `Needs Verification`.

## Migration Parity Table - UOW-1216

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.TaskId.SKILL_USE` | `Aion.GameServer.Services.BindPointTeleportRuntimeStatePlanService`; future `BindPointTeleportRuntimeStateOwner` | Enum / Scheduler Key | Partial | Unit Tested | Needs Verification | Non-live C# plan pins ordinal `16` and name `TaskId.SKILL_USE`. No live task slot exists. Reflection behavior is not involved; threading behavior remains unverified. |
| `com.aionemu.gameserver.controllers.CreatureController.addTask(TaskId, Future<?>)` | future `Aion.GameServer.Services.BindPointTeleportRuntimeStateOwner.ScheduleSkillUseTask` | Controller / Task Owner | Not Started | Manual Only | Needs Verification | Java replaces any old task in the same ordinal slot and calls `cancel(false)`. C# needs exact replace/cancel ownership before scheduling bind-point callbacks. |
| `com.aionemu.gameserver.controllers.CreatureController.cancelTask(TaskId)` | future `Aion.GameServer.Services.BindPointTeleportRuntimeStateOwner.CancelSkillUseTask` | Controller / Task Owner | Not Started | Manual Only | Needs Verification | Java removes the task before `cancel(false)`. C# owner must preserve that ordering and avoid deleting newer replacements during callback cleanup. |
| `com.aionemu.gameserver.controllers.CreatureController.hasTask(TaskId)` | future `Aion.GameServer.Services.BindPointTeleportRuntimeStateOwner.HasSkillUseTask` | Controller / Task Lookup | Not Started | Manual Only | Needs Verification | Java checks slot presence, not `Future.isDone()`, in `cancelTeleport`. C# should document whether completed-but-not-cleaned slots can be observed. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.cooldowns` | future `Aion.GameServer.Services.BindPointTeleportRuntimeStateOwner` cooldown map | Service / Runtime State | Not Started | Manual Only | Needs Verification | Java static `HashMap<Integer, Cooldown>` is keyed by player object id. C# should use singleton lifetime and an explicit concurrency policy. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.addCooldown` | `BindPointTeleportRuntimeStatePlanService.CreateAddCooldownPlan`; future runtime owner | Service / Cooldown Mutation | Partial | Unit Tested | Needs Verification | Non-live plan stores `now + 600000`. No live state mutation exists. Date/time source must be injectable for deterministic tests. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.Cooldown.getTimeLeft` | `BindPointTeleportRuntimeStatePlanService.CalculateJavaTimeLeftSeconds`; future runtime owner lookup | DTO / Date-Time Utility | Partial | Unit Tested | Needs Verification | Non-live helper mirrors whole-second truncation and non-positive-to-zero behavior. Java runtime time comparison not executed. |
| `com.aionemu.gameserver.utils.ThreadPoolManager.schedule` | `Aion.GameServer.Utils.ThreadPoolManager.Schedule` / `ScheduledTask` | Scheduler Dependency | Partial | Manual Only | Needs Verification | C# scheduler can delay and cancel through tokens, but `ScheduledTask.Cancel()` is not a Java `Future.cancel(false)` runtime comparison. Cancellation races and callback-start semantics remain unverified. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.cancelTeleport` | `BindPointTeleportControlPlanService`; future runtime owner integration | Service / Control Flow | Partial | Unit Tested | Needs Verification | Non-live cancel plan exists. Live `hasTask`/`cancelTask` owner and action `2` fanout remain unported. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.onLogin` | `BindPointTeleportControlPlanService`; future runtime owner login bridge | Service / Login Flow | Partial | Unit Tested | Needs Verification | Non-live active-cooldown packet intent exists. Live cooldown lookup and source-included fanout remain disabled. |

Tests added:

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| None in UOW-1216 | Documentation-only runtime owner requirements for Java task/cooldown state. | Manual source inspection only. |

## Summary Metrics

- Total Java artifacts discovered: 10 grouped artifact rows in this unit
- Total artifacts ported: 0 executable artifacts; 1 runtime-owner design audit completed
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 10 grouped rows
- Total blocked artifacts: 5 grouped categories: runtime task owner, cooldown owner, live dispatch, inventory mutation/persistence, and live movement/fanout
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- Live `GameServerConnection` dispatch remains disabled.
- Runtime owner implementation is still missing.
- Java `cancel(false)` and C# token cancellation are not identical; future tests must explicitly cover pending-delay cancellation and replacement cleanup.
- Java cooldown storage uses wall-clock milliseconds; C# date/time injection must avoid precision/rounding drift while preserving integer division truncation.
- Java's reviewed cooldown `HashMap` is static and unsynchronized. C# should use concurrency-safe storage as an intentional implementation difference, documented in owner tests/docs.
- Reflection behavior did not change. Threading, date/time, movement, known-list, persistence, and Java runtime packet parity remain unverified for live bind-point teleport.

## Next Work Options

### Recommended Sequential Task

- Task: Implement `BindPointTeleportRuntimeStateOwner` as an isolated service/test pair.
- Why: Handler composition and runtime-state plans exist, but live adapter work still needs real ownership for Java task-slot replace/cancel semantics and player-id cooldown lookup before any scheduling or login bridge can be safe.
- Suggested files:
  - `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportRuntimeStateOwner.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/BindPointTeleportRuntimeStateOwnerTests.cs`
- Scope guard:
  - Do not touch `GameServerConnection`.
  - Do not send packets.
  - Do not mutate inventory.
  - Do not call movement services.
  - Do not mark verified parity without Java runtime comparison.

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Runtime task/cooldown owner implementation | new service/test pair | Medium | Best next step; avoid dispatch. |
| B | Static hotspot fact assembly audit | new doc or isolated adapter/test | Medium | Avoid shared static-data loaders unless owned exclusively. |
| C | Source-included fanout live test design | new doc/test helper only | Medium | Keep registry behavior isolated. |
| D | Continue `SM_SELL_ITEM` live-readiness fact assembly | new/non-overlapping sell-item adapter files and tests | Medium | Avoid shared dialog routing unless owned exclusively. |

### Do Not Parallelize

- `GameServerConnection` live bind-point dispatch: wait until runtime owner, inventory, fanout, and movement prerequisites are satisfied.
- Shared progress/handoff/parity docs: Orchestrator should own final edits.
- Runtime owner and live movement adapter in the same unit: too much side-effect risk.

## Context Needed By Next Session

- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/services/teleport/BindPointTeleportService.java`
  - `game-server/src/com/aionemu/gameserver/controllers/CreatureController.java`
  - `game-server/src/com/aionemu/gameserver/model/TaskId.java`
- C# surfaces:
  - `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportRuntimeStatePlanService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportHandlerCompositionPlanService.cs`
  - `dotnetConversion/src/Aion.GameServer/Utils/ThreadPoolManager.cs`
- Latest completed commit before this handoff unit:
  - `d39deda88 [Phase 6][UOW-1215] Add bind point teleport handler composition`
- This handoff unit should be committed as:
  - `[Phase 6][UOW-1216] Add bind point teleport runtime owner design`
- Keep live bind-point behavior disabled until runtime owner, live inventory mutation/packets, live source-included fanout, live movement packet ordering, and live known-list behavior each have focused parity slices.
