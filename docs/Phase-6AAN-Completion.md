# Phase 6AAN Completion Handoff

Date: May 26, 2026
Latest Unit of Work: UOW-1204
Status: Phase 6 continues; bind-point hotspot packet source-inclusion semantics are now modeled as non-live fanout planning, but live connection dispatch, persistent known-list parity, scheduler/cooldown state, Kinah mutation, and movement remain disabled.

## Session Summary

UOW-1204 added `BindPointTeleportFanoutPlanService`, a source-derived non-live plan for Java bind-point hotspot packet fanout. Java `broadcastPacket(player, packet, true)` and `broadcastPacketAndReceive(player, packet)` both include the source player and then Java known-list players; the C# live target should therefore use `BroadcastToVisiblePlayersAsync(..., includeSourcePlayer: true)` when live wiring is eventually safe.

Files changed:

- `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportFanoutPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/BindPointTeleportFanoutPlanServiceTests.cs`
- `docs/Phase-6-BindPointTeleport-LiveFanout-Audit.md`
- `docs/Phase-6-PricesService-Consumer-Map.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6AAN-Completion.md`

## What Changed

- Added fanout planning for Java bind-point packet call sites:
  - `BindPointTeleportService.teleport` action `1` start broadcast;
  - `BindPointTeleportService.teleport` scheduled action `3` cooldown broadcast;
  - `BindPointTeleportService.cancelTeleport` action `2` cancel broadcast;
  - `BindPointTeleportService.onLogin` action `3` cooldown broadcast-and-receive;
  - custom `PvpMapHandler` action `1`/`3` hotspot broadcasts.
- Recorded the intended C# live registry surface as `BroadcastToVisiblePlayersAsync(..., includeSourcePlayer: true)`.
- Documented that C# distance-based visibility is still only an approximation of Java persistent `KnownList.forEachPlayer` membership.

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "BindPointTeleportFanoutPlanServiceTests" --nologo` passed 7 tests.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport" --nologo` passed 33 tests.
- No Java runtime comparison was run, so parity remains `Needs Verification`.

## Migration Parity Table - UOW-1204

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.utils.PacketSendUtility.broadcastPacket(Player,AionServerPacket,boolean)` | `Aion.GameServer.Services.BindPointTeleportFanoutPlanService`; future `IGameClientConnectionRegistry.BroadcastToVisiblePlayersAsync(..., includeSourcePlayer: true)` | Fanout Utility / Planner | Partial | Unit Tested | Needs Verification | Non-live planner records Java `toSelf=true` behavior as source-player plus known-list players for bind-point hotspot packets. Live registry send remains unwired. C# visible-distance fanout approximates Java known-list membership. |
| `com.aionemu.gameserver.utils.PacketSendUtility.broadcastPacketAndReceive(VisibleObject,AionServerPacket)` | `Aion.GameServer.Services.BindPointTeleportFanoutPlanService`; future `IGameClientConnectionRegistry.BroadcastToVisiblePlayersAsync(..., includeSourcePlayer: true)` | Fanout Utility / Planner | Partial | Unit Tested | Needs Verification | Non-live planner records Java source-player inclusion for login cooldown action `3`. Live `broadcastPacketAndReceive` equivalent remains unwired. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` | `Aion.GameServer.Services.BindPointTeleportOperationPlanService` / `Aion.GameServer.Services.BindPointTeleportFanoutPlanService` | Service / Movement Dependency | Partial | Unit Tested | Needs Verification | Start action `1` and cooldown action `3` fanout are now represented as include-source-player packet intents. Scheduler, cooldown storage, Kinah mutation, death recheck, and movement remain unported. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.cancelTeleport` | `Aion.GameServer.Services.BindPointTeleportControlPlanService` / `Aion.GameServer.Services.BindPointTeleportFanoutPlanService` | Service / Control Dependency | Partial | Unit Tested | Needs Verification | Cancel action `2` fanout is now represented as include-source-player intent. Live task lookup/cancel and live fanout remain unported. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.onLogin` | `Aion.GameServer.Services.BindPointTeleportControlPlanService` / `Aion.GameServer.Services.BindPointTeleportFanoutPlanService` | Service / Login Control Dependency | Partial | Unit Tested | Needs Verification | Login cooldown action `3` fanout is represented as include-source-player intent. Static cooldown map/time-left calculation and live fanout remain unported. |
| `com.aionemu.gameserver.custom.pvpmap.PvpMapHandler` | future C# custom PvP map handler / `Aion.GameServer.Services.BindPointTeleportFanoutPlanService` | Runtime Handler Dependency | Not Started | Unit Tested | Unknown | Java custom PvP map uses the same include-source-player hotspot action `1`/`3` fanout. C# dynamic handler parity remains future work; this unit only records fanout semantics. |

Tests added:

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `BindPointTeleportFanoutPlanServiceTests.CreatePlan_BroadcastPacketTrueIncludesSourcePlayer` | Java `broadcastPacket(player, packet, true)` maps to include-source-player C# fanout intent. | Source-derived only. |
| `BindPointTeleportFanoutPlanServiceTests.CreatePlan_BroadcastPacketAndReceiveIncludesSourcePlayer` | Java `broadcastPacketAndReceive(player, packet)` maps to include-source-player C# fanout intent. | Source-derived only. |
| `BindPointTeleportFanoutPlanServiceTests.CreatePlan_RecordsSpecificJavaSourceForBindPointBranches` | Start, scheduled cooldown, and cancel source labels are preserved. | Source-derived only. |

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 1 non-live fanout planner plus focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: 5 grouped categories: live connection dispatch, persistent known-list parity, cooldown/task state, Kinah mutation, and final movement/death checks
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- C# fanout is still distance-based visible-player fanout, not Java persistent `KnownList.forEachPlayer` membership.
- Live `GameServerConnection` still does not dispatch `CmBindPointTeleport`.
- Non-live plans are not yet composed into one request-to-operation plan.
- Static cooldown map, scheduler timing/cancellation, scheduled Kinah decrement race, final death/about-to-die recheck, and movement remain unported.
- No Java runtime comparison was executed. Reflection, serialization, date/time, threading, persistence, and movement parity remain unverified for live bind-point teleport.

## Next Work Options

### Recommended Sequential Task

- Task: Compose `CmBindPointTeleport` and `BindPointTeleportClientActionPlanService` output with existing non-live bind-point price/requirement/operation/control/fanout planners into a request-level bind-point teleport composition plan.
- Why: Parser, action selection, packet serialization, operation/control planning, and fanout intent now exist as separate pieces. A request-level composition plan can prove branch wiring before any live connection handler is touched.
- Required behavior:
  - dead player -> no action;
  - action `1` -> requires hotspot/price/requirements facts and produces operation/fanout intents when ready;
  - action `2` -> uses skill-task fact and produces cancel/fanout intent when applicable;
  - unknown action -> no action;
  - no live packet send, scheduler, cooldown mutation, Kinah mutation, or movement.
- Suggested files:
  - `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportRequestPlanService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/BindPointTeleportRequestPlanServiceTests.cs`
  - docs for next unit

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Request-level non-live composition plan | new service/test files | Medium | Best next step before live connection handler. |
| B | Model scheduled Kinah-decrement failure intent | new planner/test files | Medium | Keep separate from live inventory mutation. |
| C | Audit future task/cooldown owner for Java `TaskId.SKILL_USE` | Java controller/C# scheduler files read-only | Low/Medium | Useful before live scheduler work. |
| D | Continue `SM_SELL_ITEM` live-readiness fact assembly | new/non-overlapping sell-item adapter files and tests | Medium | Avoid shared dialog routing unless owned exclusively. |

### Do Not Parallelize

- `GameServerConnection` live bind-point dispatch: wait until request-level non-live composition exists.
- Shared progress/handoff/parity docs: Orchestrator should own final edits.
- `PlayerTeleportService` movement wiring: defer until task/cooldown ownership is designed.

## Context Needed By Next Session

- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_BIND_POINT_TELEPORT.java`
  - `game-server/src/com/aionemu/gameserver/services/teleport/BindPointTeleportService.java`
  - `game-server/src/com/aionemu/gameserver/utils/PacketSendUtility.java`
  - `game-server/src/com/aionemu/gameserver/custom/pvpmap/PvpMapHandler.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_BIND_POINT_TELEPORT.java`
- Latest completed commits:
  - `6c75ba194 [Phase 6][UOW-1203] Add bind point teleport client action plan`
  - next commit should be `[Phase 6][UOW-1204] Add bind point teleport fanout plan`
- Keep live bind-point behavior disabled until request-level composition, persistent known-list/fanout semantics, cooldown/task ownership, Kinah mutation, and final movement each have focused parity slices.
