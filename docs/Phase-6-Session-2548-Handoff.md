# Phase 6 Session 2548 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2548: Stop protection active on player movement in HandleMoveAsync

## Full Session Summary (UOWs 2543–2548)

| UOW | Summary |
|-----|---------|
| 2543 | CM_WINDSTREAM handler live; all 8 states; SmWindstream (opcode 163) |
| 2544 | SmWindstreamAnnounce (opcode 164) + WindstreamTable XML data loading; level-ready announces |
| 2545 | SmInstanceCountInfo (opcode 147); level-ready instance-entry packet |
| 2546 | Wire NearbyQuestRefreshPlanService into HandleLevelReadyAsync |
| 2547 | Wire updateNearbyQuests into HandleTitleSetAsync |
| 2548 | Stop protection active on movement in HandleMoveAsync + SmPlayerState broadcast |

## CM_LEVEL_READY Parity Progress

| Packet / behavior | C# status |
|-------------------|-----------|
| SM_HOUSE_OBJECTS | Live |
| SM_INSTANCE_COUNT_INFO | Live (UOW-2545) |
| SM_PLAYER_INFO | Live |
| SM_ACCOUNT_PROPERTIES | Live |
| SM_MOTION | Live |
| SM_WINDSTREAM_ANNOUNCE | Live (UOW-2544) |
| Fly notify | Live |
| SM_NEARBY_QUESTS | Live (UOW-2546) |
| SM_CUBE_UPDATE | Live |
| SM_SHIELD_EFFECT | Deferred |
| SM_CONQUEROR_PROTECTOR | Deferred |
| SM_RIFT_ANNOUNCE | Deferred |
| SM_UPGRADE_ARCADE | Deferred |
| SM_QUEST_REPEAT | Deferred |
| Weather | Deferred |
| QuestEngine.onEnterWorld | Deferred |
| Pet spawn | Deferred |

## New Server Packets (UOWs 2543–2548)

| Packet | Opcode | Java source |
|--------|--------|-------------|
| SmWindstream | 163 | SM_WINDSTREAM |
| SmWindstreamAnnounce | 164 | SM_WINDSTREAM_ANNOUNCE |
| SmInstanceCountInfo | 147 | SM_INSTANCE_COUNT_INFO |

## updateNearbyQuests Wiring Status

| Call site | Java source | C# status |
|-----------|-------------|-----------|
| CM_LEVEL_READY | PlayerController.updateNearbyQuests | Live (UOW-2546) |
| CM_TITLE_SET | TitleList.setDisplayTitle → updateNearbyQuests | Live (UOW-2547) |
| Level up | PlayerController.upgradePlayer → updateNearbyQuests | Deferred (XP/level-up system not ported) |
| Item use (quest items) | ItemService.updateNearbyQuests | Deferred |

## Protection Active Changes (UOW-2548)

Java `CM_MOVE.runImpl` checks `isProtectionActive()` before `World.updatePosition` and calls
`stopProtectionActiveTask()` if the player has actually moved (x/y changed, or z dropped > 0.5f).

C# now:
1. Checks `player.IsProtectionActive()` and compares old vs new position
2. Calls `player.StopProtectionActive()` (unsets BLINKING visual state)
3. Broadcasts `SmPlayerState` to visible players (including source player)

Missing from full Java parity:
- Cancel the scheduled protection-active timer (timer system not yet live for protection task)
- `notifyAIOnMove()` (AI notification deferred)

## Validation Decisions

| UOW | Command | Result | Trigger |
|-----|---------|--------|---------|
| 2543 | `--filter "CmWindstreamTests"` | 14/14 | none |
| 2543 | `--filter "CmWindstreamTests\|GameServerConnectionFlightZoneFanoutTests"` | 36/36 | none |
| 2544 | `--filter "CmWindstreamTests"` | 18/18 | none |
| 2544 | `--filter "PlayerEnterWorldServiceTests\|CmWindstreamTests"` | 75/75 | none |
| 2545 | `--filter "CmWindstreamTests"` | 20/20 | none |
| 2546 | `--filter "CmWindstreamTests\|NearbyQuestRefreshPlan"` | 28/28 | none |
| 2547 | `--filter "NearbyQuestRefreshPlanService\|NearbyQuestStartConditionService"` | 20/20 | none |
| 2548 | `--filter "CmWindstreamTests\|NpcDialogSideEffect"` | 29/29 | none |

## Migration Parity Table

| Java Artifact | C# Artifact | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|
| CM_WINDSTREAM all states | HandleWindstreamAsync | Complete | Unit Tested | Partial Parity | Quest hook (state 1) + SM_TRANSFORM deferred |
| SM_WINDSTREAM (163) | SmWindstream | Complete | Unit Tested | Verified Parity | writeD(state)+writeC(1) |
| SM_WINDSTREAM_ANNOUNCE (164) | SmWindstreamAnnounce | Complete | Unit Tested | Verified Parity | 4 fields per writeImpl |
| WindstreamData | WindstreamTable | Complete | Unit Tested | Verified Parity | GetByMapId matches Java |
| SM_INSTANCE_COUNT_INFO (147) | SmInstanceCountInfo | Complete | Unit Tested | Verified Parity | mapId+instanceId+1(solo) |
| PlayerController.updateNearbyQuests (level-ready) | HandleLevelReadyAsync | Complete | Regression Tested | Partial Parity | No timer scheduled for delayed refresh |
| TitleList.setDisplayTitle → updateNearbyQuests | HandleTitleSetAsync | Complete | Manual Only | Partial Parity | Same pattern as level-ready |
| CM_MOVE isProtectionActive + stopProtectionActiveTask | HandleMoveAsync | Complete | Manual Only | Partial Parity | Missing timer cancel; notifyAIOnMove deferred |

## Context Needed By Next Session

### Protection Active
- `player.IsProtectionActive()` checks BLINKING visual state
- `player.StopProtectionActive()` clears it (returns true if it was set)
- In movement, old position is captured before update, then compared with packet position
- Java z-threshold: `player.getZ() > z + 0.5f` (drops cancel protection, small fall does not)
- Broadcast `SmPlayerState` after stopping protection active to inform clients

### Windstream
- `HandleWindstreamAsync` live for states 0/1/2/3/4/7/8
- State 1 MISSING: QuestEngine.onEnterWindStream — deferred
- States 2/3 MISSING: SM_TRANSFORM broadcast when transformed — deferred
- `WindstreamTable.GetByMapId(worldId)` returns locations for a map

### Level-ready
- Full packet sequence: SmInstanceCountInfo (if instance) → SmPlayerInfo → SmAccountProperties → SmMotion → SmWindstreamAnnounce(s) → flight notify → SmNearbyQuests → SmCubeUpdate

### Item system (unchanged from UOWs 2531-2542)
- Same-storage and cross-storage move/split/merge/kinah live; legion WH deferred
- HandleTitleSetAsync and HandleSetNoteAsync live; both save to DB on logout

## Next Recommended UOW

**UOW-2549: Port updateNearbyQuests into zone/subzone transition or investigate portal location announces**

Options:
1. **SM_RIFT_ANNOUNCE skeleton** — `RiftInformer.sendRiftsInfo` in CM_LEVEL_READY; check if rift location data can produce a minimal response even without live rift objects
2. **Protection active timer** — Wire the scheduled protection-active task into `HandleLevelReadyAsync` (Java schedules 60-second timer)
3. **Movement + protection: AI notify** — `notifyAIOnMove()` is called when protection stops on move; assess if it's needed
4. **Legion WH kinah** — CM_LEGION_WH_KINAH; assess if stub is possible without LegionService
5. **Player status info (loot mode)** — HandlePlayerStatusInfoAsync assessment

Focused validation recipe for UOW-2549 (if SM_RIFT_ANNOUNCE skeleton):
- Behavior: rift location map sends a static announce for rift areas on level-ready
- Focused C# command: `dotnet test --filter "FullyQualifiedName~CmWindstreamTests"` (ensures level-ready path compiles)
- Java/Maven: not expected
- Broad-validation trigger: none

## Remaining Risks

- Legion warehouse operations (CM_MOVE_ITEM, CM_SPLIT_ITEM, CM_LEGION_WH_KINAH) deferred
- Transform system not yet ported — SM_TRANSFORM broadcast on windstream exit missing
- QuestEngine.onEnterWindStream (windstream state 1) deferred
- XP/level-up system not ported — updateNearbyQuests on level-up deferred
- Protection active timer not implemented in C# — only visual state mutation live
- CM_LEVEL_READY: SM_SHIELD_EFFECT, SM_RIFT_ANNOUNCE, SM_CONQUEROR_PROTECTOR, quest/effect updates deferred
- SmViewPlayerDetails sends to any world player; Java restricts to known-list (parity gap)
- Kinah split: only cube↔account warehouse; regular warehouse kinah not handled
