# Phase 6 Session 2553 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2553: Register CM_HOUSE_TELEPORT (222), CM_HOUSE_OPEN_DOOR (226); add CM_BIND_POINT_TELEPORT dispatch stub

## Session Summary (UOWs 2549–2553)

| UOW | Summary |
|-----|---------|
| 2549 | Register CM_GATHER (opcode 19) parser + deferred handler; 4 tests |
| 2550 | Register 5 missing parsers: exchange add item/kinah, replace item, group loot, distribution settings |
| 2551 | Live CM_EXCHANGE_ADD_KINAH handler + Player.ExchangeKinah tracking; kinah add-to-exchange now works |
| 2552 | Add exchange-specific inventory constants: PlayerExchangeGet (0x21), PlayerExchangeGetBack (0x23), PutToExchange (0x25/0x26) |
| 2553 | Register CM_HOUSE_TELEPORT (222), CM_HOUSE_OPEN_DOOR (226) parsers; add CM_BIND_POINT_TELEPORT deferred dispatch |

## Exchange System Status

| Packet/handler | Status |
|----------------|--------|
| CM_EXCHANGE_REQUEST (63) | Live: sends exchange request question to partner |
| CM_EXCHANGE_ADD_KINAH (66) | Live (UOW-2551): tracks Player.ExchangeKinah, sends SmExchangeAddKinah to both parties |
| CM_EXCHANGE_ADD_ITEM (64) | Deferred: exchange item basket not yet tracked on Player |
| CM_EXCHANGE_LOCK (67) | Live: locks exchange, notifies partner |
| CM_EXCHANGE_OK (68) | Live: confirms exchange; trade execution deferred |
| CM_EXCHANGE_CANCEL (69) | Live: cancels exchange, resets both players' state |
| SmExchangeAddKinah (opcode 77) | Live |
| SmExchangeConfirmation | Live (request/lock/cancel); success (0x00) deferred pending trade execution |

### Exchange kinah flow (live)
- `Player.ExchangeKinah` tracks how much kinah this player has put into the exchange window
- Reset to 0 when exchange starts (accepted) or cancels
- `ExchangeAddKinahPlanService.CreatePlan` computes `countToAdd = min(available, requested)` per Java
- Self receives `SmExchangeAddKinah(count, 0)`, partner receives `SmExchangeAddKinah(count, 1)`
- Guard: `!IsTrading || IsExchangeLocked || CurrentExchangePartnerObjectId == 0` → return

### Trade execution gap
- When both players confirm, no items/kinah actually transfer (deferred)
- `ExchangeConfirmStatus.TradeExecutionBlocked` is returned but no success packet is sent
- Items: `PlayerExchangeGet` (0x21) and `PutToExchange` (0x25/0x26) constants added for when this is ported

## Missing Opcode Registration Status

Opcodes now registered (gap closed this session):
- 19: CM_GATHER
- 64: CM_EXCHANGE_ADD_ITEM
- 66: CM_EXCHANGE_ADD_KINAH
- 178: CM_REPLACE_ITEM
- 184: CM_GROUP_LOOT
- 185: CM_DISTRIBUTION_SETTINGS
- 222: CM_HOUSE_TELEPORT (UOW-2553)
- 226: CM_HOUSE_OPEN_DOOR (UOW-2553)

Still unregistered Java opcodes:
- 7: CM_CHARACTER_EDIT (plastic surgery, AUTHED state)
- 14: CM_CAPTCHA
- 16, 29, 47, 55, 59, 160, 161: Legion emblem/history/rank packets
- 159: CM_PLAYER_SEARCH
- 164: CM_QUEST_SHARE
- 180: CM_DEBUG_COMMAND (admin)

## Validation Decisions

| UOW | Command | Result | Trigger |
|-----|---------|--------|---------|
| 2549 | `--filter "CmGatherTests"` | 4/4 | none |
| 2550 | `--filter "CmGatherTests"` (compile proof) | 4/4 | none |
| 2551 | `--filter "ExchangeAddKinahPlanService\|PlayerExchangeRequestService"` | 25/25 | none |
| 2552 | `--filter "ExchangeAddKinahPlanService\|PlayerExchangeRequestService\|CmGatherTests"` | 29/29 | none |
| 2553 | `--filter "CmGatherTests\|ExchangeAddKinahPlanService"` | 11/11 | none |

## Context Needed By Next Session

### Exchange system
- `Player.ExchangeKinah` (long): kinah committed to active exchange; resets on start/cancel
- `Player.CurrentExchangePartnerObjectId`: object ID of trade partner
- `Player.IsTrading`, `IsExchangeLocked`, `IsExchangeConfirmed`: exchange state flags
- `ExchangeAddKinahPlanService.CreatePlan(requested, inventoryKinah, exchangeKinah)` → plan
- Exchange trade execution (both confirm) is deferred; `TradeExecutionBlocked` status means nothing executes
- `TryGetOnlinePlayerByObjectId(int)` can access partner's Player object if needed for trade execution
- `SmInventoryUpdateItem.PlayerExchangeGet = 0x21`, `PlayerExchangeGetBack = 0x23`, `PutToExchange = 0x25`
- `SmDeleteItem.PutToExchangeDeleteType = 0x26`

### Previous context (unchanged)
- Windstream: SmWindstream (163), SmWindstreamAnnounce (164), SmInstanceCountInfo (147) all live
- Level-ready: full packet sequence now includes instance count, windstream announces, nearby quests
- Protection active: stops on movement + broadcasts SmPlayerState
- updateNearbyQuests wired in level-ready and title-set

## Next Recommended UOW

**UOW-2554: Port CM_EXCHANGE_ADD_ITEM (exchange item basket)**

To port exchange item adding live:
1. Add `ExchangeItems` collection to `Player` — dictionary from itemObjId to `(ItemObjectId, Count, ItemTemplateId)` 
2. Validate item is tradeable (uses existing ItemRestrictionCleanupTable)
3. Send SmDeleteItem(PutToExchangeDeleteType) or SmInventoryUpdateItem(PutToExchange) to self
4. Send SmExchangeAddItem to both parties
5. Reset ExchangeItems on exchange start/cancel

Alternative UOWs:
1. **Exchange trade execution (kinah-only)** — when both players confirm and no exchange items on either side, transfer ExchangeKinah between players and send SmExchangeConfirmation(0). Requires inventory mutation + persistence.
2. **Legion history packet skeleton** — send empty SM_LEGION_HISTORY response to CM_LEGION_HISTORY so legion UI doesn't hang
3. **CM_PLAYER_SEARCH stub** — return empty SM_PLAYER_SEARCH response so the player search panel works (shows no results)

Focused validation recipe for UOW-2554:
- Behavior: exchange add item sends correct packets to both parties; ExchangeItems tracked
- Focused C# command: `dotnet test --filter "FullyQualifiedName~ExchangeAddItemPlanService"` (new)
- Java/Maven: not expected
- Broad-validation trigger: none

## Remaining Risks

- Exchange trade execution not yet live (kinah/items don't actually transfer on confirm)
- Exchange add item deferred (CM_EXCHANGE_ADD_ITEM handler is no-op)
- CM_REPLACE_ITEM (178), CM_GROUP_LOOT (184), CM_DISTRIBUTION_SETTINGS (185): parsers live but handlers deferred
- CM_HOUSE_TELEPORT (222), CM_HOUSE_OPEN_DOOR (226): parsers live but handlers deferred
- CM_BIND_POINT_TELEPORT (244): parser live but handler deferred
- CM_GATHER (19): parser live but handler deferred (gathering controller not ported)
- Legion warehouse operations (CM_MOVE_ITEM, CM_SPLIT_ITEM, CM_LEGION_WH_KINAH) all deferred
- Transform system not yet ported — SM_TRANSFORM broadcast missing
- XP/level-up system not ported
- Protection active timer not implemented (only visual state mutation live)
