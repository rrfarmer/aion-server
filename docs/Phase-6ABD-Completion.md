# Phase 6ABD Completion Handoff

Date: May 26, 2026
Latest Unit of Work: UOW-1220
Status: Phase 6 continues; bind-point teleport now has parser, action selection, operation/control packet intents, fanout intents, request-level composition, runtime task/cooldown state semantics, scheduled Kinah decrement/failure intent, final movement gate intent, scheduled callback composition metadata, a `TeleportService.teleportTo` side-effect audit, a non-live teleport side-effect planner, callback-side side-effect metadata composition, a live-adapter readiness checklist, concrete failure system-message helpers, a non-live handler composition bridge, a runtime-owner design audit, an isolated runtime owner implementation, a non-sending runtime control bridge, isolated source-included fanout for action `2`/login action `3` control intents, and a metadata-only action `1` scheduled callback bridge. Full `GameServerConnection` dispatch, live inventory mutation, callback action `3` fanout execution, persistent known-list parity, and movement remain disabled.

## Session Summary

UOW-1220 added `BindPointTeleportRuntimeScheduledCallbackBridgeService`, which schedules supplied callback metadata through `BindPointTeleportRuntimeStateOwner` for ready action `1` operation plans. It does not execute the callback side effects.

Files changed:

- `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportRuntimeScheduledCallbackBridgeService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/BindPointTeleportRuntimeScheduledCallbackBridgeServiceTests.cs`
- `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`
- `docs/Phase-6-BindPointTeleport-LiveFanout-Audit.md`
- `docs/Phase-6-PricesService-Consumer-Map.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ABD-Completion.md`

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "BindPointTeleportRuntimeScheduledCallbackBridgeServiceTests" --nologo` passed 5 tests.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport" --nologo` passed 87 tests.
- No Java runtime comparison was run, so parity remains `Needs Verification`.

## Migration Parity Table - UOW-1220

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` scheduled `TaskId.SKILL_USE` branch | `Aion.GameServer.Services.BindPointTeleportRuntimeScheduledCallbackBridgeService.ScheduleMetadataCallback` | Service / Scheduler Bridge | Partial | Unit Tested | Needs Verification | Ready operation plans can now schedule supplied callback metadata through the runtime owner. Scheduled callback side effects remain metadata-only; no Kinah, cooldown, fanout, or movement executes. |
| `com.aionemu.gameserver.controllers.CreatureController.addTask(TaskId, Future<?>)` | `BindPointTeleportRuntimeStateOwner.ScheduleSkillUseTask` consumed by scheduled callback bridge | Controller / Task Owner | Partial | Unit Tested | Needs Verification | Bridge reuses owner default 10-second delay and replacement behavior. Java `Future.cancel(false)` runtime races remain unverified. |
| `com.aionemu.gameserver.model.TaskId.SKILL_USE` | `BindPointTeleportRuntimeScheduledCallbackBridgeService` / `BindPointTeleportRuntimeStateOwner` | Enum / Scheduler Key | Partial | Unit Tested | Needs Verification | Action `1` ready operation now reaches the C# skill-use slot. Not wired to live client dispatch. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` failed operation branches | `BindPointTeleportRuntimeScheduledCallbackBridgeService` | Service / Guard Flow | Partial | Unit Tested | Needs Verification | Invalid hotspot/failed requirements do not schedule a task. System-message sends/audits remain outside this bridge. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` scheduled callback body | `BindPointTeleportScheduledCallbackPlanService` metadata observed by bridge | Service / Callback Metadata | Partial | Unit Tested | Needs Verification | Metadata callback can be observed by tests, but actual `tryDecreaseKinah`, `addCooldown`, action `3` fanout, and final movement remain disabled. |
| `com.aionemu.gameserver.utils.ThreadPoolManager.schedule` | `Aion.GameServer.Utils.ThreadPoolManager.Schedule` through runtime owner bridge | Scheduler Dependency | Partial | Unit Tested | Needs Verification | Tests cover default delay metadata, replacement, and zero-delay metadata observation. Java scheduler runtime comparison was not run. |

Tests added:

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `ScheduleMetadataCallback_OperationNotReadyDoesNotScheduleTask` | Non-ready operation plans do not schedule `TaskId.SKILL_USE`. | Source-derived only. |
| `ScheduleMetadataCallback_ReadyOperationWithoutCallbackPlanRecordsGap` | Ready operation without supplied callback metadata does not schedule. | C# staging guard only. |
| `ScheduleMetadataCallback_ReadyOperationSchedulesJavaSkillUseTaskSlot` | Ready operation schedules owner task with 10-second delay. | Source-derived only. |
| `ScheduleMetadataCallback_ReplacementCancelsExistingMetadataTask` | Replacement cancels prior metadata task and keeps one slot. | Source-derived only. |
| `ScheduleMetadataCallback_ZeroDelayInvokesMetadataOnlyCallback` | Metadata callback can be observed without executing Kinah/cooldown/fanout/movement side effects. | Source-derived only. |

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 1 metadata-only scheduled callback bridge plus 5 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: 4 grouped categories: live dispatch, live Kinah/inventory mutation, callback action `3` fanout, and final movement
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- Live `GameServerConnection` dispatch remains disabled.
- Scheduled callback body still does not perform Kinah mutation, cooldown insertion, action `3` fanout, or final movement.
- Static hotspot/live fact assembly remains missing for action `1`.
- Java scheduler timing/cancellation races, Java known-list fanout, inventory persistence, and movement packet ordering remain unverified.
- Reflection behavior did not change. Serialization behavior did not change in this unit. Threading, date/time, persistence, fanout ordering, and movement parity remain `Needs Verification`.

## Next Work Options

### Recommended Sequential Task

- Task: Add a callback-side execution bridge that consumes `BindPointTeleportScheduledCallbackPlan` and performs only cooldown insertion plus action `3` runtime fanout when supplied facts already indicate Kinah success.
- Why: Action `1` can now schedule metadata, but callback-side cooldown/fanout execution is still not bridged to the runtime owner/fanout adapter.
- Scope guard:
  - Do not mutate Kinah.
  - Do not call movement services.
  - Do not wire full `GameServerConnection` dispatch.
  - Preserve Java ordering: Kinah plan gate, cooldown insertion, action `3` fanout, final movement metadata only.

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Callback-side cooldown/fanout bridge with Kinah already supplied as success/failure metadata | new service/test pair | Medium | Best next step; keep inventory and movement disabled. |
| B | Static hotspot fact assembly audit | new doc or isolated adapter/test | Medium | Avoid shared static-data loaders unless owned exclusively. |
| C | Known-list parity audit for C# visible registry approximation | new doc only | Low/Medium | Useful before broad live fanout claims. |
| D | Continue `SM_SELL_ITEM` live-readiness fact assembly | new/non-overlapping sell-item adapter files and tests | Medium | Avoid shared dialog routing unless owned exclusively. |

### Do Not Parallelize

- `GameServerConnection` live bind-point dispatch: wait until callback cooldown/fanout, inventory, movement, and known-list prerequisites are satisfied.
- Shared progress/handoff/parity docs: Orchestrator should own final edits.
- Callback bridge and live movement adapter in the same unit: too much side-effect risk.

## Context Needed By Next Session

- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/services/teleport/BindPointTeleportService.java`
  - `game-server/src/com/aionemu/gameserver/controllers/CreatureController.java`
  - `game-server/src/com/aionemu/gameserver/model/TaskId.java`
  - `game-server/src/com/aionemu/gameserver/utils/PacketSendUtility.java`
- C# surfaces:
  - `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportRuntimeScheduledCallbackBridgeService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportScheduledCallbackPlanService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportRuntimeStateOwner.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportRuntimeFanoutService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportRuntimeControlBridgeService.cs`
- Latest completed commits:
  - `cd9572648 [Phase 6][UOW-1219] Add bind point teleport runtime fanout`
  - next commit should be `[Phase 6][UOW-1220] Add bind point teleport scheduled callback bridge`
- Keep live bind-point behavior disabled until callback-side cooldown/fanout, live Kinah mutation/packets, live movement packet ordering, and persistent known-list behavior each have focused parity slices.
