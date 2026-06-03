# Phase 6 Session 2546 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2546: Wire updateNearbyQuests into HandleLevelReadyAsync

## Full Session Summary (UOWs 2543–2546)

This session completed 4 UOWs focused on the windstream system and CM_LEVEL_READY completion:

| UOW | Summary |
|-----|---------|
| 2543 | CM_WINDSTREAM handler live (`SmWindstream` opcode 163); all 8 states including guards, FP triggers, state mutations, emotion broadcasts |
| 2544 | `SmWindstreamAnnounce` (opcode 164) + `WindstreamTable` data loading; `HandleLevelReadyAsync` now sends windstream announces |
| 2545 | `SmInstanceCountInfo` (opcode 147) + level-ready instance-entry packet |
| 2546 | Wire `NearbyQuestRefreshPlanService` into `HandleLevelReadyAsync`; sends `SmNearbyQuests` when map/quest data available |

## Windstream System Parity (UOWs 2543–2544)

| Aspect | Java | C# | Status |
|--------|------|----|--------|
| CM_WINDSTREAM state 0 | unsetPlayerMode(RIDE) | `player.IsInRideMode = false` | Verified Parity |
| CM_WINDSTREAM state 1 (enter) | flight path + state + emotion + FP restore + quest hook | same (quest hook deferred) | Partial Parity |
| CM_WINDSTREAM state 2 (exit→glide) | unset FLYING, switchToGliding, announce WINDSTREAM_END | same (SM_TRANSFORM deferred) | Partial Parity |
| CM_WINDSTREAM state 3 (exit) | unset FLYING, statsAndSpeed, announce WINDSTREAM_EXIT | same (SM_TRANSFORM deferred) | Partial Parity |
| CM_WINDSTREAM state 4 | no-op | no-op | Verified Parity |
| CM_WINDSTREAM states 7/8 | boost emotions | same | Verified Parity |
| SM_WINDSTREAM (opcode 163) | writeD(state) writeC(1) | same | Verified Parity |
| SM_WINDSTREAM_ANNOUNCE (opcode 164) | writeD(flyPathId) writeD(mapId) writeD(streamId) writeC(state) | same | Verified Parity |
| WindstreamData.getStreamTemplate | WindstreamTable.GetByMapId | same | Verified Parity |

## CM_LEVEL_READY Parity Progress

| Packet | Java source | C# status |
|--------|-------------|-----------|
| SM_HOUSE_OBJECTS | activeHouse != null → spawn objects | Live (prior session) |
| SM_INSTANCE_COUNT_INFO | player.isInInstance() → send | Live (UOW-2545) |
| SM_PLAYER_INFO | always | Live (prior session) |
| SM_ACCOUNT_PROPERTIES | always | Live (prior session) |
| SM_MOTION | always | Live (prior session) |
| SM_WINDSTREAM_ANNOUNCE | per map windstream template | Live (UOW-2544) |
| Fly notify (SM_EMOTION FLY/etc.) | isInFlyState(FLYING) | Live (prior session) |
| SM_NEARBY_QUESTS | PlayerController.updateNearbyQuests | Live (UOW-2546) |
| SM_CUBE_UPDATE | always | Live (prior session) |
| SM_INSTANCE_COUNT_INFO | player.isInInstance() → send | Live (UOW-2545) |
| SM_SHIELD_EFFECT / SM_ABYSS_ARTIFACT_INFO3 | isInSiegeWorld | Deferred |
| SM_CONQUEROR_PROTECTOR | always | Deferred |
| SM_RIFT_ANNOUNCE | RiftInformer.sendRiftsInfo | Deferred |
| SM_UPGRADE_ARCADE | EventsConfig.ENABLE_EVENT_ARCADE | Deferred |
| SM_QUEST_REPEAT | updateRepeatableQuests | Deferred |
| Weather | WeatherService.loadWeather | Deferred |
| QuestEngine.onEnterWorld | quest hooks | Deferred |
| Effect icons | updatePlayerEffectIcons | Deferred |
| Pet spawn | pet != null && !pet.isSpawned | Deferred |
| Town/Event service | onEnterWorld/onEnterMap | Deferred |
| Team brands | team.sendBrands delay | Deferred |

## New Server Packets This Session

| Packet | Opcode | Java source |
|--------|--------|-------------|
| `SmWindstream` | 163 | SM_WINDSTREAM |
| `SmWindstreamAnnounce` | 164 | SM_WINDSTREAM_ANNOUNCE |
| `SmInstanceCountInfo` | 147 | SM_INSTANCE_COUNT_INFO |

## New Data Tables This Session

| Table | Loaded from | Key method |
|-------|-------------|------------|
| `WindstreamTable` | `<windstream>` in static_data merged cache | `GetByMapId(int mapId)` |
| `WindstreamLocationSummary` | `<location>` inside `<windstream>` | FlyPathId 0=GEYSER, 1=ONE_WAY, 2=TWO_WAY |

## Validation Decisions

| UOW | Focused command | Result | Broad-validation trigger |
|-----|-----------------|--------|--------------------------|
| 2543 | `--filter "CmWindstreamTests"` | 14/14 | none |
| 2543 | `--filter "CmWindstreamTests\|GameServerConnectionFlightZoneFanoutTests"` | 36/36 | none |
| 2544 | `--filter "CmWindstreamTests"` | 18/18 | none |
| 2544 | `--filter "PlayerEnterWorldServiceTests\|CmWindstreamTests"` | 75/75 | none |
| 2544 | `--filter "GameServerConnectionFlightZoneFanoutTests"` | 22/22 | none |
| 2545 | `--filter "CmWindstreamTests"` | 20/20 | none |
| 2545 | `--filter "GameServerConnectionFlightZoneFanoutTests.HandleLevelReady"` | 1/1 | none |
| 2546 | `--filter "CmWindstreamTests\|NearbyQuestRefreshPlan"` | 28/28 | none |
| 2546 | `--filter "GameServerConnectionFlightZoneFanoutTests.HandleLevelReady"` | 1/1 | none |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CM_WINDSTREAM` | `CmWindstream` + `HandleWindstreamAsync` | Handler | Complete | Unit Tested | Partial Parity | Quest hook (state 1) + SM_TRANSFORM (states 2/3) deferred |
| `SM_WINDSTREAM` | `SmWindstream` | ServerPacket | Complete | Unit Tested | Verified Parity | Opcode 163 |
| `SM_WINDSTREAM_ANNOUNCE` | `SmWindstreamAnnounce` | ServerPacket | Complete | Unit Tested | Verified Parity | Opcode 164 |
| `WindstreamData` | `WindstreamTable` | DataHolder | Complete | Unit Tested | Verified Parity | GetByMapId matches getStreamTemplate |
| `SM_INSTANCE_COUNT_INFO` | `SmInstanceCountInfo` | ServerPacket | Complete | Unit Tested | Verified Parity | Opcode 147; solo flag hardcoded to 1 |
| `PlayerController.updateNearbyQuests` | `HandleLevelReadyAsync` nearby quest wiring | Handler | Complete | Manual Only | Partial Parity | Sends SmNearbyQuests via existing NearbyQuestRefreshPlanService |
| `CM_LEVEL_READY` windstream announce | `HandleLevelReadyAsync` windstream section | Handler | Complete | Regression Tested | Verified Parity | Sends SmWindstreamAnnounce for all locations in player's map |
| `CM_LEVEL_READY` instance count info | `HandleLevelReadyAsync` instance section | Handler | Complete | Regression Tested | Verified Parity | Sends SmInstanceCountInfo when map is instance type |

## Context Needed By Next Session

### Windstream
- `HandleWindstreamAsync` live for states 0/1/2/3/4/7/8.
- State 1 MISSING: `QuestEngine.onEnterWindStream` — deferred.
- States 2/3 MISSING: `SM_TRANSFORM` broadcast when transformed — deferred.
- `SmWindstream` opcode 163; `SmWindstreamAnnounce` opcode 164; `SmInstanceCountInfo` opcode 147.
- `WindstreamTable.GetByMapId(worldId)` returns all locations for a map.

### Level-ready additions
- `HandleLevelReadyAsync` now sends (in order): SmInstanceCountInfo (if instance), SmPlayerInfo, SmAccountProperties, SmMotion, SmWindstreamAnnounce(s), then flight notify, SmNearbyQuests (if world/quest templates available), SmCubeUpdate.

### Item system context (UOWs 2531–2542, unchanged)
- Same-storage and cross-storage move/split/merge/kinah live; legion WH deferred.
- `HandleTitleSetAsync` and `HandleSetNoteAsync` live; both save to DB on logout.

## Next Recommended UOW

**UOW-2547: Port TitleSet persistence or assess title/nearby-quest interaction**

Java `TitleList.setDisplayTitle` calls `updateNearbyQuests()` after setting the title. C# `HandleTitleSetAsync` currently skips this. Wire it in using the same pattern as `HandleLevelReadyAsync`.

Alternative UOWs:
1. **TitleSet → updateNearbyQuests** — Wire `NearbyQuestRefreshPlanService` into `HandleTitleSetAsync` (same pattern, low risk, tests can reuse nearby quest test infrastructure)
2. **CM_LEVEL_READY SM_CONQUEROR_PROTECTOR** — assess if conqueror/protector system is partially available
3. **Legion warehouse kinah** (CM_LEGION_WH_KINAH) — requires LegionService; check if stub can be added
4. **CM_DELETE_QUEST skeleton** — stub the quest abandonment to at least log the request

Focused validation recipe for UOW-2547 (if TitleSet → updateNearbyQuests):
- Behavior: `updateNearbyQuests` called after title change, matching Java `TitleList.setDisplayTitle`
- Focused C# command: `dotnet test --filter "FullyQualifiedName~NearbyQuestRefreshPlanService|FullyQualifiedName~CmWindstreamTests"` (existing nearby quest coverage)
- Java/Maven: not expected; updateNearbyQuests is verified by service-level tests
- Broad-validation trigger: none

## Remaining Risks

- Legion warehouse operations (CM_MOVE_ITEM, CM_SPLIT_ITEM) all deferred.
- Transform system not yet ported — SM_TRANSFORM broadcast on windstream exit missing.
- QuestEngine.onEnterWindStream (state 1) deferred — windstream quest triggers will not fire.
- TitleList.setDisplayTitle calls updateNearbyQuests — not yet wired in HandleTitleSetAsync.
- `SmViewPlayerDetails` sends to any world player; Java restricts to known-list (parity gap).
- Kinah split: only cube↔account warehouse; regular warehouse kinah not handled.
- CM_LEVEL_READY: SM_SHIELD_EFFECT, SM_RIFT_ANNOUNCE, SM_CONQUEROR_PROTECTOR, quest/effect updates all deferred.
