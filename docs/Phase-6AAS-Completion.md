# Phase 6AAS Completion Handoff

Date: May 26, 2026
Latest Unit of Work: UOW-1209
Status: Phase 6 continues; bind-point teleport now has parser, action selection, operation/control packet intents, fanout intents, request-level composition, runtime task/cooldown state semantics, scheduled Kinah decrement/failure intent, final movement gate intent, and scheduled callback composition metadata. Live connection dispatch, real scheduler/cooldown mutation, live inventory mutation, persistent known-list parity, and movement remain disabled.

## Session Summary

UOW-1209 added `BindPointTeleportScheduledCallbackPlanService`, a source-derived non-live composition layer for Java's delayed `TaskId.SKILL_USE` callback in `BindPointTeleportService.teleport`. It preserves Java callback order: failed Kinah decrement sends not-enough-fee and returns; successful Kinah decrement adds cooldown, broadcasts action `3`, schedules the final movement gate, and includes movement only when that gate passes.

Files changed:

- `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportScheduledCallbackPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/BindPointTeleportScheduledCallbackPlanServiceTests.cs`
- `docs/Phase-6-BindPointTeleport-LiveFanout-Audit.md`
- `docs/Phase-6-PricesService-Consumer-Map.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6AAS-Completion.md`

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "BindPointTeleportScheduledCallbackPlanServiceTests" --nologo` passed 3 tests.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport" --nologo` passed 58 tests.
- No Java runtime comparison was run, so parity remains `Needs Verification`.

## Migration Parity Table - UOW-1209

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` delayed `TaskId.SKILL_USE` callback | `Aion.GameServer.Services.BindPointTeleportScheduledCallbackPlanService` | Service / Scheduled Callback Planner | Partial | Unit Tested | Needs Verification | Non-live composition preserves Java callback ordering around Kinah failure, cooldown insert, action `3` fanout, final task scheduling, and movement gate. No live scheduler, inventory mutation, cooldown mutation, packet send, or movement is executed. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.addCooldown` | `Aion.GameServer.Services.BindPointTeleportRuntimeStatePlanService` composed by `BindPointTeleportScheduledCallbackPlanService` | Runtime State Dependency | Partial | Unit Tested | Needs Verification | Callback composition includes cooldown insert intent after Kinah success only. Static map mutation and threading remain unported. |
| `com.aionemu.gameserver.utils.PacketSendUtility.broadcastPacket(Player,AionServerPacket,boolean)` | `Aion.GameServer.Services.BindPointTeleportFanoutPlanService` composed by `BindPointTeleportScheduledCallbackPlanService` | Fanout Dependency | Partial | Unit Tested | Needs Verification | Callback composition includes action `3` include-source fanout intent after cooldown insert. Live known-list fanout remains unverified. |
| `com.aionemu.gameserver.services.teleport.TeleportService.teleportTo(Player,int,float,float,float)` | `Aion.GameServer.Services.BindPointTeleportFinalMovementPlanService` composed by `BindPointTeleportScheduledCallbackPlanService` | Movement Dependency | Partial | Unit Tested | Needs Verification | Callback composition includes final movement gate metadata. Live teleport side effects and packet order remain unported. |

Tests added:

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `BindPointTeleportScheduledCallbackPlanServiceTests.CreatePlan_KinahFailureStopsBeforeCooldownFanoutAndMovement` | Failed `tryDecreaseKinah` returns before cooldown, fanout, and movement. | Source-derived only. |
| `BindPointTeleportScheduledCallbackPlanServiceTests.CreatePlan_KinahSuccessComposesCooldownFanoutAndMovementInJavaOrder` | Java callback success order: Kinah, cooldown, action `3` fanout, final gate, movement intent. | Source-derived only. |
| `BindPointTeleportScheduledCallbackPlanServiceTests.CreatePlan_FinalMovementBlockedStillStoresCooldownAndBroadcastsLikeJava` | Final movement gate can block movement while earlier cooldown/fanout intents remain. | Source-derived only. |

## Summary Metrics

- Total Java artifacts discovered: 4 grouped artifact rows in this unit
- Total artifacts ported: 1 non-live scheduled callback composition planner plus focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: 5 grouped categories: live connection dispatch, live scheduler/cooldown ownership, live inventory mutation/packets, persistent known-list parity, and live teleport side effects
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- Live `GameServerConnection` still does not dispatch `CmBindPointTeleport`.
- Scheduled callback composition is pure metadata and does not execute real delayed tasks, inventory mutation, cooldown mutation, packet sends, or movement.
- Live `TeleportService.teleportTo` side-effect ordering remains unaudited for hotspot final movement.
- Java static cooldown map and known-list fanout threading remain unverified.
- No Java runtime comparison was executed. Reflection, serialization, date/time, threading, persistence, and movement parity remain unverified for live bind-point teleport.

## Next Work Options

### Recommended Sequential Task

- Task: Audit concrete `TeleportService.teleportTo` packet/order side effects for hotspot final movement before live movement wiring.
- Why: All non-live bind-point callback branches are now modeled, but `TeleportService.teleportTo` itself performs broader action aborts, despawn/spawn, packet sends, and world/instance updates. That side-effect order must be mapped before live movement is enabled.
- Suggested files:
  - `docs/Phase-6-BindPointTeleport-TeleportTo-Audit.md`
  - existing progress/handoff docs

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | `TeleportService.teleportTo` hotspot side-effect audit | new audit doc only | Low/Medium | Best next step before live movement. |
| B | Concrete system-message packet support audit for bind-point failure messages | read-only packet/system-message files plus optional doc | Low/Medium | Useful before live sends. |
| C | Handler-level live-dispatch readiness checklist | new doc only | Medium | Keep read-only; no `GameServerConnection` edits yet. |
| D | Continue `SM_SELL_ITEM` live-readiness fact assembly | new/non-overlapping sell-item adapter files and tests | Medium | Avoid shared dialog routing unless owned exclusively. |

### Do Not Parallelize

- `GameServerConnection` live bind-point dispatch: wait until teleport side-effect audit and live ownership design are complete.
- Shared progress/handoff/parity docs: Orchestrator should own final edits.
- `PlayerTeleportService` movement wiring: defer until packet ordering and movement side effects are modeled.

## Context Needed By Next Session

- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/services/teleport/BindPointTeleportService.java`
  - `game-server/src/com/aionemu/gameserver/services/teleport/TeleportService.java`
  - `game-server/src/com/aionemu/gameserver/world/World.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_TELEPORT_LOC.java`
- Latest completed commits:
  - `2397ef2b0 [Phase 6][UOW-1208] Add bind point teleport final movement plan`
  - next commit should be `[Phase 6][UOW-1209] Add bind point teleport scheduled callback plan`
- Keep live bind-point behavior disabled until teleport side-effect ordering, cooldown mutation/fanout execution, live inventory mutation/packets, live movement packet ordering, and live known-list fanout each have focused parity slices.
