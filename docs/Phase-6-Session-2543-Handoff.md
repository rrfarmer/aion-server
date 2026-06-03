# Phase 6 Session 2543 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2543: Port CM_WINDSTREAM handler with SmWindstream server packet

## UOW-2543 Summary

Ported the windstream flight system handler, making windstream entry/exit/boost state transitions live in the C# server.

### Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_WINDSTREAM.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_WINDSTREAM.java`
- `game-server/src/com/aionemu/gameserver/controllers/FlyController.java` (switchToGliding)
- `game-server/src/com/aionemu/gameserver/network/aion/ServerPacketsOpcodes.java` (opcode 163)

### Files Changed

| File | Change |
|------|--------|
| `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmWindstream.cs` | New: SM_WINDSTREAM opcode 163 (writeD state + writeC result) |
| `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs` | Wire CmWindstream case → HandleWindstreamAsync; add HandleWindstreamAsync |
| `dotnetConversion/tests/Aion.GameServer.Tests/CmWindstreamTests.cs` | New: 14 focused tests for all windstream states |

### Handler Behavior by State

| State | Java behavior | C# behavior | Status |
|-------|--------------|-------------|--------|
| 0 | unsetPlayerMode(RIDE) | `player.IsInRideMode = false` | Verified Parity |
| 1 (enter) | set FlightPath.WINDSTREAM, unset ACTIVE/GLIDING, set FLYING, broadcast WINDSTREAM emotion, triggerFpRestore, quest hook | same + quest hook deferred | Partial Parity |
| 2 (exit→glide) | unset FLYING states, switchToGliding, setFlightPath(null), broadcast WINDSTREAM_END | same | Verified Parity |
| 3 (exit) | unset FLYING states, updateStatsAndSpeedVisually, setFlightPath(null), broadcast WINDSTREAM_EXIT | same | Verified Parity |
| 4 | no-op | no-op | Verified Parity |
| 7 (boost start) | broadcast WINDSTREAM_START_BOOST | same | Verified Parity |
| 8 (boost end) | broadcast WINDSTREAM_END_BOOST | same | Verified Parity |
| default | log warning, return | same | Verified Parity |

### Deferred (documented in handler)

- `QuestEngine.onEnterWindStream` — state 1 quest hook deferred until quest engine is ported.
- `SM_TRANSFORM` broadcast on exit states 2/3 when player is transformed — deferred until transform system is ported.

### Validation Decision

- Changed surface: production-code (new server packet, new handler, new tests)
- Specific behavior/contract: state machine for windstream entry/exit/boost with correct creature/fly state mutations and packet fanout
- Focused C# command: `dotnet test --filter "FullyQualifiedName~CmWindstreamTests"` → 14/14 passed
- Adjacent regression: `dotnet test --filter "FullyQualifiedName~CmWindstreamTests|FullyQualifiedName~GameServerConnectionFlightZoneFanoutTests"` → 36/36 passed (incl. all flight zone tests)
- Focused Java/Maven command: not available — no matching Java test for windstream state machine
- Broad-validation trigger: none
- Broad .NET decision: skipped (focused evidence sufficient for scoped change)

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CM_WINDSTREAM` | `CmWindstream` | ClientPacket | Complete | Unit Tested | Verified Parity | Parser complete from prior session; handler now live |
| `SM_WINDSTREAM` | `SmWindstream` | ServerPacket | Complete | Unit Tested | Verified Parity | opcode 163; writeD(state) + writeC(result) |
| `CM_WINDSTREAM.runImpl` states 0,2,3,4,7,8 | `HandleWindstreamAsync` | Handler | Complete | Unit Tested | Verified Parity | All states except quest hook and transform broadcast |
| `CM_WINDSTREAM.runImpl` state 1 (QuestEngine hook) | deferred | Handler | Partial | No Tests | Partial Parity | QuestEngine.onEnterWindStream deferred |
| `FlyController.switchToGliding` (windstream exit) | `Player.StartGliding()` | Service | Complete | Unit Tested | Verified Parity | Return value ignored per Java; called on state 2 only |

## Context Needed By Next Session

### Windstream state context
- `HandleWindstreamAsync` is live for all states.
- State 1 MISSING: `QuestEngine.onEnterWindStream` call — deferred.
- State 2/3 MISSING: `SM_TRANSFORM` broadcast when transformed — deferred.
- `PlayerFlightPathType.Windstream` tracks active windstream flight path on `Player.FlightPathType`.
- `Player.IsUsingFlightPath(PlayerFlightPathType.Windstream)` guards states 2/3.
- `UpdatePlayerStatsAndSpeedVisuallyAsync(player)` broadcasts stats/speed changes for state 2/3; called BEFORE the windstream emotion broadcast.
- `BroadcastEmotionAsync` wraps `BroadcastToVisiblePlayersAsync` with `includeSourcePlayer: true`.

### Previous session carry-forward (UOWs 2531–2542)
- Item system: same-storage and cross-storage move/split/merge/kinah live; legion WH deferred.
- `IsTrading` guards on CM_MOVE_ITEM cross-storage and CM_SPLIT_ITEM.
- `HandleTitleSetAsync` and `HandleSetNoteAsync` are live; both save to DB on logout via `SavePlayerLogoutAsync`.
- `SmWindstream` opcode 163; `SmWarehouseUpdateItem` opcode 171; `SmViewPlayerDetails` opcode 65; `SmUnwrapItem` opcode 289.

## Next Recommended UOW

**UOW-2544: Port CM_OPEN_STATICDOOR handler**

The static door handler (`CM_OPEN_STATICDOOR`) is currently deferred. Java dispatches `StaticDoorService.openStaticDoor`. The key question is whether the C# infrastructure for static door objects is in place.

Alternative next UOWs (roughly increasing complexity):
1. **CM_OPEN_STATICDOOR** — check if `StaticDoorService` and the door object model are available
2. **Player stats/speed on title change** — `TitleList.setDisplayTitle` calls `updateNearbyQuests()` which C# skips; check if there's a quest start-condition table that could do this without full quest engine
3. **SM_WINDSTREAM_ANNOUNCE** — the `CM_LEVEL_READY` path sends `SM_WINDSTREAM_ANNOUNCE` for windstream areas; verify this is live or port it
4. **Legion warehouse items** — port CM_MOVE_ITEM and CM_SPLIT_ITEM for legion WH storage type (storageType==3)

Focused validation recipe for UOW-2544 (if CM_OPEN_STATICDOOR):
- Behavior: static door open state mutation + packet fanout
- Focused C# command: `dotnet test --filter "FullyQualifiedName~CmOpenStaticDoorTests"` (new)
- Java/Maven: not expected unless Java door model fixtures exist
- Broad-validation trigger: none

## Remaining Risks

- Legion warehouse operations (CM_MOVE_ITEM, CM_SPLIT_ITEM) all deferred.
- Transform system not yet ported — SM_TRANSFORM broadcast on windstream exit missing.
- QuestEngine.onEnterWindStream deferred — windstream quest triggers will not fire.
- `TitleList.setDisplayTitle` calls `updateNearbyQuests()` — not ported, quest start conditions near the player won't re-evaluate on title change.
- `SmViewPlayerDetails` sends to any world player; Java restricts to known-list (parity gap from UOW-2531).
- Kinah split: only cube↔account warehouse; regular warehouse kinah not handled.
