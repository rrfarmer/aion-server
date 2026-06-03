# Phase 6 Session 2550 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2550: Register 5 missing client packet parsers (exchange, loot, replace-item)

## Full Session Summary (UOWs 2543–2550)

| UOW | Summary |
|-----|---------|
| 2543 | CM_WINDSTREAM handler live; all 8 states; SmWindstream (opcode 163); 14 tests |
| 2544 | SmWindstreamAnnounce (164) + WindstreamTable XML data; level-ready announces |
| 2545 | SmInstanceCountInfo (147); level-ready instance-entry packet |
| 2546 | Wire NearbyQuestRefreshPlanService into HandleLevelReadyAsync |
| 2547 | Wire updateNearbyQuests into HandleTitleSetAsync |
| 2548 | Stop protection active on movement + SmPlayerState broadcast |
| 2549 | Register CM_GATHER (opcode 19) parser + deferred handler; 4 tests |
| 2550 | Register 5 missing client parsers: exchange add item/kinah, replace item, group loot, distribution settings |

## Missing Opcode Registration Gap Closed (UOWs 2549–2550)

The following Java opcodes were unregistered in C# (packets silently dropped):

| Opcode | Java packet | C# parser | Handler status |
|--------|-------------|-----------|----------------|
| 19 | CM_GATHER | CmGather | Deferred: gathering controller |
| 64 | CM_EXCHANGE_ADD_ITEM | CmExchangeAddItem | Deferred: exchange basket |
| 66 | CM_EXCHANGE_ADD_KINAH | CmExchangeAddKinah | Deferred: exchange basket |
| 178 | CM_REPLACE_ITEM | CmReplaceItem | Deferred: ItemMoveService.switchItemsInStorages |
| 184 | CM_GROUP_LOOT | CmGroupLoot | Deferred: DropDistributionService |
| 185 | CM_DISTRIBUTION_SETTINGS | CmDistributionSettings | Deferred: loot rules on group/alliance |

Still unregistered (Java opcodes without C# parsers):
- 7: CM_CHARACTER_EDIT (character name/rename coupon)
- 14: CM_CAPTCHA (captcha response)
- 16: CM_LEGION_SEND_EMBLEM_INFO
- 29: CM_LEGION_DOMINION_REQUEST_RANKING
- 41: CM_BUILDER_COMMAND
- 42: CM_BUILDER_CONTROL
- 47: CM_LEGION_SEND_EMBLEM
- 55: CM_LEGION_HISTORY
- 59: CM_LEGION_MODIFY_EMBLEM
- 159: CM_PLAYER_SEARCH
- 160: CM_LEGION_UPLOAD_INFO
- 161: CM_LEGION_UPLOAD_EMBLEM
- 164: CM_QUEST_SHARE
- 180: CM_DEBUG_COMMAND
- 222: CM_HOUSE_TELEPORT
- 226: CM_HOUSE_OPEN_DOOR

## updateNearbyQuests Wiring Status

| Call site | C# status |
|-----------|-----------|
| CM_LEVEL_READY | Live (UOW-2546) |
| TitleList.setDisplayTitle | Live (UOW-2547) |
| Level up | Deferred (XP/level-up system not ported) |
| Item use (quest items) | Deferred |
| Quest completion | Deferred |
| Skill learning | Deferred |
| NPC spawn (WorldMapInstance) | Deferred |

## Validation Decisions

| UOW | Command | Result | Trigger |
|-----|---------|--------|---------|
| 2548 | `--filter "CmWindstreamTests\|NpcDialogSideEffect"` | 29/29 | none |
| 2549 | `--filter "CmGatherTests"` | 4/4 | none |
| 2550 | `--filter "CmGatherTests"` (compile proof) | 4/4 | none |

## Migration Parity Table

| Java Artifact | C# Artifact | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|
| CM_GATHER (opcode 19) | CmGather | Partial | Unit Tested | Partial Parity | Parser live; handler deferred |
| CM_EXCHANGE_ADD_ITEM (64) | CmExchangeAddItem | Partial | No Tests | Partial Parity | Parser live; handler deferred |
| CM_EXCHANGE_ADD_KINAH (66) | CmExchangeAddKinah | Partial | No Tests | Partial Parity | Parser live; handler deferred |
| CM_REPLACE_ITEM (178) | CmReplaceItem | Partial | No Tests | Partial Parity | Parser live; handler deferred |
| CM_GROUP_LOOT (184) | CmGroupLoot | Partial | No Tests | Partial Parity | Parser live; handler deferred |
| CM_DISTRIBUTION_SETTINGS (185) | CmDistributionSettings | Partial | No Tests | Partial Parity | Parser live; handler deferred |
| CM_MOVE isProtectionActive check | HandleMoveAsync | Complete | Manual Only | Partial Parity | Timer cancel not live |

## Context Needed By Next Session

### Missing registrations (6 unregistered, post-UOW-2550)
The most gameplay-critical remaining unregistered packets:
- **222: CM_HOUSE_TELEPORT** — house teleport from housing system
- **159: CM_PLAYER_SEARCH** — social search panel (/who command)
- **164: CM_QUEST_SHARE** — quest sharing with group members

### Exchange system state
- Exchange request/lock/ok/cancel are live.
- CM_EXCHANGE_ADD_ITEM (64) and CM_EXCHANGE_ADD_KINAH (66) are now parsed but handlers deferred.
- `ExchangeAddKinahPlanService` exists but `IsLive = false`.
- `PlayerExchangeRequestService` tracks `CurrentExchangePartnerObjectId`, `IsExchangeLocked`, `IsExchangeConfirmed`.
- The exchange basket (items + kinah amounts) is NOT tracked on Player yet — needed before add-item/kinah can go live.

### Previous context (unchanged)
- WindstreamTable, SmWindstream, SmWindstreamAnnounce all live.
- HandleLevelReadyAsync now sends: SmInstanceCountInfo → SmPlayerInfo → SmAccountProperties → SmMotion → SmWindstreamAnnounce(s) → flight notify → SmNearbyQuests → SmCubeUpdate.
- Protection active stops on movement (z-threshold +0.5f allows fall drift).

## Next Recommended UOW

**UOW-2551: Port exchange basket model + live CM_EXCHANGE_ADD_KINAH handler**

The kinah add path is straightforward with `ExchangeAddKinahPlanService` already available:
1. Add `ExchangeKinah` tracking to `Player` model (long, mutable)
2. Wire `HandleExchangeAddKinahAsync` using `ExchangeAddKinahPlanService.CreatePlan`
3. Send `SmExchangeAddKinah` to self and partner via connection registry
4. Mark `ExchangeAddKinahPlanService.IsLive = true`

Alternative next UOWs:
1. **Register CM_HOUSE_TELEPORT (222)** — adds house teleport to the registered opcode list
2. **Register CM_PLAYER_SEARCH (159)** — player search panel
3. **Port CM_GATHER handler** — start/cancel gathering against known gatherable objects (requires gatherable world object model)
4. **Port CM_REPLACE_ITEM handler** — slot-swap items (ItemMoveService.switchItemsInStorages equivalent)

Focused validation recipe for UOW-2551 (if exchange kinah live):
- Behavior: SM_EXCHANGE_ADD_KINAH sent to self (action=0) and partner (action=1) with correct kinah amount
- Focused C# command: `dotnet test --filter "FullyQualifiedName~ExchangeAddKinahPlanService"` (existing test)
- Java/Maven: not expected
- Broad-validation trigger: none

## Remaining Risks

- Legion warehouse operations (CM_MOVE_ITEM, CM_SPLIT_ITEM, CM_LEGION_WH_KINAH) all deferred.
- Transform system not yet ported — SM_TRANSFORM broadcast on windstream exit missing.
- QuestEngine.onEnterWindStream (windstream state 1) deferred.
- XP/level-up system not ported — updateNearbyQuests on level-up deferred.
- Protection active timer not implemented — only visual state mutation live.
- Exchange basket (items + kinah) not tracked on Player model.
- 16 Java client opcodes still unregistered in C# (silently dropped).
- CM_LEVEL_READY: SM_SHIELD_EFFECT, SM_RIFT_ANNOUNCE, SM_CONQUEROR_PROTECTOR, quest/effect updates deferred.
