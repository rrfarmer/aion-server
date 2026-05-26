# Phase 6AAM Completion Handoff

Date: May 26, 2026
Latest Unit of Work: UOW-1203
Status: Phase 6 continues; `CM_BIND_POINT_TELEPORT.runImpl` dispatch is now modeled as non-live action planning, but live connection dispatch, fanout, scheduler/cooldown state, Kinah mutation, and movement remain disabled.

## Session Summary

UOW-1203 added `BindPointTeleportClientActionPlanService`, a source-derived non-live plan for Java `CM_BIND_POINT_TELEPORT.runImpl`. It consumes parsed action facts and records what Java would do without invoking live services.

Files changed:

- `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportClientActionPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/BindPointTeleportClientActionPlanServiceTests.cs`
- `docs/Phase-6-BindPointTeleport-LiveFanout-Audit.md`
- `docs/Phase-6-PricesService-Consumer-Map.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6AAM-Completion.md`

## What Changed

- Added non-live action planning for Java `CM_BIND_POINT_TELEPORT.runImpl`:
  - dead player -> no action;
  - action `1` -> teleport requested with `locId` and `kinah`;
  - action `2` -> cancel requested;
  - unknown action -> no action.
- Added focused tests for all branches.
- Updated the live fanout audit and prices map to reflect that parser and action-selection prerequisites now exist.

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "BindPointTeleportClientActionPlanServiceTests" --nologo` passed 4 tests.
- No Java runtime comparison was run, so parity remains `Needs Verification`.

## Migration Parity Table - UOW-1203

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_BIND_POINT_TELEPORT.runImpl` | `Aion.GameServer.Services.BindPointTeleportClientActionPlanService` | Client Packet / Handler Planner | Partial | Unit Tested | Needs Verification | Non-live planner covers Java dead-player early return, action `1` teleport intent, action `2` cancel intent, and unknown-action no-op. Live `GameServerConnection` dispatch remains unwired. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_BIND_POINT_TELEPORT.readImpl` | `Aion.GameServer.Network.Aion.ClientPackets.CmBindPointTeleport` | Client Packet / Parser Dependency | Complete | Unit Tested | Needs Verification | Parser fields from UOW-1202 feed this planner. No Java runtime client packet capture was compared. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` | `Aion.GameServer.Services.BindPointTeleportOperationPlanService`; selected by `BindPointTeleportClientActionPlanService` | Service / Movement Dependency | Partial | Unit Tested | Needs Verification | Action `1` selects teleport intent only. Live hotspot lookup, requirement composition, packet fanout, scheduler, cooldown, Kinah mutation, death recheck, and movement remain unported. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.cancelTeleport` | `Aion.GameServer.Services.BindPointTeleportControlPlanService`; selected by `BindPointTeleportClientActionPlanService` | Service / Control Dependency | Partial | Unit Tested | Needs Verification | Action `2` selects cancel intent only. Live task lookup/cancel and action `2` fanout remain unported. |

Tests added:

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `BindPointTeleportClientActionPlanServiceTests.CreatePlan_DeadPlayerReturnsBeforeActionDispatch` | Dead-player early return before action dispatch. | Source-derived only. |
| `BindPointTeleportClientActionPlanServiceTests.CreatePlan_ActionOneRequestsTeleportWithLocIdAndKinah` | Action `1` selects teleport intent and preserves `locId`/`kinah`. | Source-derived only. |
| `BindPointTeleportClientActionPlanServiceTests.CreatePlan_ActionTwoRequestsCancelWithParsedDefaults` | Action `2` selects cancel intent with parser-default fields. | Source-derived only. |
| `BindPointTeleportClientActionPlanServiceTests.CreatePlan_UnknownActionNoopsAfterDeadCheck` | Unknown action no-op after dead check. | Source-derived only. |

## Summary Metrics

- Total Java artifacts discovered: 4 grouped artifact rows in this unit
- Total artifacts ported: 1 non-live client action planner plus focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: 5 grouped categories: live connection dispatch, fanout semantics, cooldown/task state, Kinah mutation, and final movement/death checks
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- `GameServerConnection` still does not dispatch `CmBindPointTeleport`.
- Non-live action planning does not compose hotspot facts, price, requirements, operation, or control planners yet.
- Live packet fanout semantics, especially source-player inclusion, remain unverified.
- Static cooldown map, scheduler timing/cancellation, scheduled Kinah decrement race, final death/about-to-die recheck, and movement remain unported.
- No Java runtime comparison was executed. Reflection, serialization, date/time, threading, persistence, and movement parity remain unverified for live bind-point teleport.

## Next Work Options

### Recommended Sequential Task

- Task: Audit Java `PacketSendUtility.broadcastPacket(..., true)` and `broadcastPacketAndReceive` source-inclusion semantics against C# `IGameClientConnectionRegistry.BroadcastToVisiblePlayersAsync`.
- Why: The parser and runImpl action-selection prerequisites are now staged; the next live risk is whether action `1`, action `2`, and action `3` hotspot packets include the source player and visible players in the same way Java does.
- Files:
  - Java source: `game-server/src/com/aionemu/gameserver/utils/PacketSendUtility.java`
  - Java source: `game-server/src/com/aionemu/gameserver/services/teleport/BindPointTeleportService.java`
  - C# source: `dotnetConversion/src/Aion.GameServer/Network/Aion/IGameClientConnectionRegistry.cs`
  - C# source: `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientSocketServer.cs`
  - C# source: `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
  - docs for next unit

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Read-only fanout semantic audit | Java PacketSendUtility and C# registry files | Low | Best next step before live packet sends. |
| B | Compose action-plan output with existing non-live bind-point planners | new service/test files | Medium | Still avoid live connection handler. |
| C | Model scheduled Kinah-decrement failure intent | new planner/test files | Medium | Keep separate from live inventory mutation. |
| D | Continue `SM_SELL_ITEM` live-readiness fact assembly | new/non-overlapping sell-item adapter files and tests | Medium | Avoid shared dialog routing unless owned exclusively. |

### Do Not Parallelize

- `GameServerConnection` live bind-point dispatch: wait until fanout semantics are clear.
- Shared progress/handoff/parity docs: Orchestrator should own final edits.
- `PlayerTeleportService` movement wiring: defer until task/cooldown ownership is designed.

## Context Needed By Next Session

- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_BIND_POINT_TELEPORT.java`
  - `game-server/src/com/aionemu/gameserver/services/teleport/BindPointTeleportService.java`
  - `game-server/src/com/aionemu/gameserver/utils/PacketSendUtility.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_BIND_POINT_TELEPORT.java`
- Latest completed commits:
  - `6e7593a0e [Phase 6][UOW-1202] Add bind point teleport client packet`
  - next commit should be `[Phase 6][UOW-1203] Add bind point teleport client action plan`
- Keep live bind-point behavior disabled until parser, non-live action planning, fanout semantics, cooldown/task ownership, Kinah mutation, and final movement each have focused parity slices.
