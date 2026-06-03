# Phase 6 Session 2543-2544 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2544: Port SM_WINDSTREAM_ANNOUNCE, WindstreamTable data loading, and CM_LEVEL_READY announce fanout

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

## UOW-2544 Summary

Ported windstream data loading and the SM_WINDSTREAM_ANNOUNCE server packet. Players entering windstream maps now receive the correct announce packets telling the client about windstream locations.

### Java Source Reviewed (UOW-2544)

- `game-server/src/com/aionemu/gameserver/dataholders/WindstreamData.java`
- `game-server/src/com/aionemu/gameserver/model/templates/windstreams/WindstreamTemplate.java`
- `game-server/src/com/aionemu/gameserver/model/templates/windstreams/Location2D.java`
- `game-server/src/com/aionemu/gameserver/model/flypath/FlyPathType.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_WINDSTREAM_ANNOUNCE.java`
- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_LEVEL_READY.java` (windstream announce section)
- `game-server/src/com/aionemu/gameserver/network/aion/ServerPacketsOpcodes.java` (opcode 164)

### Files Changed (UOW-2544)

| File | Change |
|------|--------|
| `dotnetConversion/src/Aion.GameServer/Dataholders/WindstreamTable.cs` | New: WindstreamTable + WindstreamLocationSummary record |
| `dotnetConversion/src/Aion.GameServer/Dataholders/StaticData.cs` | Parse `<windstream>` + `<location>` elements; add WindstreamLocations property |
| `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmWindstreamAnnounce.cs` | New: opcode 164; writeD(flyPathId) writeD(mapId) writeD(streamId) writeC(state) |
| `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs` | HandleLevelReadyAsync sends SmWindstreamAnnounce for player's map |
| `dotnetConversion/tests/Aion.GameServer.Tests/CmWindstreamTests.cs` | Add SmWindstreamAnnounce and WindstreamTable tests |

### Validation Decision (UOW-2544)

- Changed surface: production-code (new data table, new server packet, StaticData parser extension, level-ready handler update)
- Specific behavior: windstream announce sent per location for player's current map
- Focused C# command: `dotnet test --filter "FullyQualifiedName~CmWindstreamTests"` → 18/18 passed
- Adjacent regression: `--filter "FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~CmWindstreamTests"` → 75/75 passed
- Flight zone fanout regression: `--filter "FullyQualifiedName~GameServerConnectionFlightZoneFanoutTests"` → 22/22 passed
- Java/Maven: not available; FlyPathType id mapping verified from enum source
- Broad-validation trigger: none
- Broad .NET decision: skipped (focused evidence sufficient)

### Migration Parity Table (UOW-2544)

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `WindstreamData` | `WindstreamTable` | DataHolder | Complete | Unit Tested | Verified Parity | Keyed by mapId; GetByMapId matches getStreamTemplate |
| `WindstreamTemplate` + `Location2D` | `WindstreamLocationSummary` | Model | Complete | Unit Tested | Verified Parity | Flat record; FlyPathType id mapping 0/1/2 per Java enum |
| `SM_WINDSTREAM_ANNOUNCE` | `SmWindstreamAnnounce` | ServerPacket | Complete | Unit Tested | Verified Parity | Opcode 164; all 4 fields write-verified |
| `CM_LEVEL_READY` windstream announce loop | `HandleLevelReadyAsync` windstream section | Handler | Complete | No Tests | Partial Parity | Sends announces; `null` static data safely skipped |

## Next Recommended UOW

**UOW-2545: Assess CM_LEVEL_READY remaining gaps or port CM_ABYSS_RANKING_PLAYERS/LEGIONS skeleton**

Options:
1. **CM_LEVEL_READY audit** — verify which remaining packets (SM_INSTANCE_COUNT_INFO, SM_RIFT_ANNOUNCE, SM_CONQUEROR_PROTECTOR) are deferred vs. silently absent. Port the simplest missing one.
2. **CM_ABYSS_RANKING_PLAYERS/LEGIONS skeleton** — these require an AbyssRankingCache; assess if the C# abyss rank data service can provide ranking list data.
3. **Legion warehouse items** — port CM_MOVE_ITEM and CM_SPLIT_ITEM for legion WH (storageType==3).
4. **updateNearbyQuests on title change** — `TitleList.setDisplayTitle` calls `updateNearbyQuests()`; check if `NearbyQuestTemplates` is sufficient without full quest engine.

Focused validation recipe for UOW-2545 (if CM_LEVEL_READY audit):
- Behavior: document or port one more level-ready packet
- Focused C# command: `dotnet test --filter "FullyQualifiedName~GameServerConnectionFlightZoneFanoutTests"` (existing level-ready coverage)
- Java/Maven: not expected unless Java level-ready fixtures exist
- Broad-validation trigger: none

## Remaining Risks

- Legion warehouse operations (CM_MOVE_ITEM, CM_SPLIT_ITEM) all deferred.
- Transform system not yet ported — SM_TRANSFORM broadcast on windstream exit missing.
- QuestEngine.onEnterWindStream (state 1) deferred — windstream quest triggers will not fire.
- `TitleList.setDisplayTitle` calls `updateNearbyQuests()` — not ported, quest start conditions near the player won't re-evaluate on title change.
- `SmViewPlayerDetails` sends to any world player; Java restricts to known-list (parity gap from UOW-2531).
- Kinah split: only cube↔account warehouse; regular warehouse kinah not handled.
- CM_LEVEL_READY: SM_INSTANCE_COUNT_INFO, SM_RIFT_ANNOUNCE, SM_CONQUEROR_PROTECTOR, quest/effect updates all deferred.

## Context Needed By Next Session

### Windstream state context (UOWs 2543-2544)
- `HandleWindstreamAsync` live for all states (0/1/2/3/4/7/8); state 1 quest hook deferred.
- `SmWindstream` opcode 163 (state + result); `SmWindstreamAnnounce` opcode 164 (flyPathId + mapId + streamId + state).
- `StaticData.WindstreamLocations.GetByMapId(worldId)` returns locations for a map.
- `HandleLevelReadyAsync` now sends SmWindstreamAnnounce for all locations in player's map.

### Item system context (UOWs 2531-2542)
- Same-storage and cross-storage move/split/merge/kinah live; legion WH deferred.
- `HandleTitleSetAsync` and `HandleSetNoteAsync` are live; both save to DB on logout.

### UOW-2543 tests summary
- 14 tests in `CmWindstreamTests.cs` cover windstream handler states + SmWindstream packet parity.
- 4 new tests added in UOW-2544: SmWindstreamAnnounce bytes, opcode, WindstreamTable lookup, count.
