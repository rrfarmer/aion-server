# Phase 6 Session 2559 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2559: Add alliance loot rules change + extend CM_DISTRIBUTION_SETTINGS for alliances

## Full Session Summary (UOWs 2549–2559)

| UOW | Summary |
|-----|---------|
| 2549 | Register CM_GATHER (opcode 19) |
| 2550 | Register 5 parsers: exchange add item/kinah (64/66), replace item (178), group/alliance loot (184/185) |
| 2551 | Live CM_EXCHANGE_ADD_KINAH + Player.ExchangeKinah tracking |
| 2552 | Exchange inventory constants: PlayerExchangeGet (0x21), PlayerExchangeGetBack (0x23), PutToExchange (0x25/0x26) |
| 2553 | Register CM_HOUSE_TELEPORT (222), CM_HOUSE_OPEN_DOOR (226); CM_BIND_POINT_TELEPORT dispatch stub |
| 2554 | Kinah-only exchange trade execution; SmExchangeConfirmation.Success = 0 |
| 2555 | Live CM_REPLACE_ITEM same-storage slot-swap |
| 2556 | Live CM_DISTRIBUTION_SETTINGS group loot rules via PlayerGroupRuntime.ChangeLootRules |
| 2557 | SmShowNpcOnMap (opcode 89) + live CM_OBJECT_SEARCH handler |
| 2558 | CM_EXCHANGE_ADD_ITEM: Player.ExchangeItems basket + item restore on cancel |
| 2559 | PlayerAllianceRuntime.ChangeLootRules + alliance path in CM_DISTRIBUTION_SETTINGS |

## Complete Exchange System Status

| Packet | Handler | Status |
|--------|---------|--------|
| CM_EXCHANGE_REQUEST (63) | HandleExchangeRequestAsync | Live |
| CM_EXCHANGE_ADD_KINAH (66) | HandleExchangeAddKinahAsync | Live: tracks Player.ExchangeKinah |
| CM_EXCHANGE_ADD_ITEM (64) | HandleExchangeAddItemAsync | Live: tracks Player.ExchangeItems dict |
| CM_EXCHANGE_LOCK (67) | HandleExchangeLockAsync | Live |
| CM_EXCHANGE_OK (68) | HandleExchangeOkAsync + ExecuteKinahOnlyExchangeAsync | Live: kinah transfer; item transfer deferred |
| CM_EXCHANGE_CANCEL (69) | HandleExchangeCancelAsync + RestoreExchangeItemsToInventoryUiAsync | Live: item UI restore on cancel |

### Exchange state on Player
- `Player.ExchangeKinah` (long): kinah committed to exchange
- `Player.ExchangeItems` (Dictionary<int, long>): objectId → committed count
- Both cleared on exchange accept/cancel
- `Player.CurrentExchangePartnerObjectId`: trade partner

### Exchange item basket behavior
- `HandleExchangeAddItemAsync`: validates trading/locked/tradeable guards; tracks committed count;
  sends SmDeleteItem(PutToExchangeDeleteType=0x26) for full-stack or SmInventoryUpdateItem(PutToExchange=0x25) for partial;
  sends SmExchangeAddItem(0, item, template) to self + SmExchangeAddItem(1, item, template) to partner
- `RestoreExchangeItemsToInventoryUiAsync`: on cancel, sends PlayerExchangeGetBack(0x23) for each committed item
- Trade execution with item transfer deferred (complex cross-player inventory mutation)

## Loot Rules Change Status

| System | Handler | Status |
|--------|---------|--------|
| Group loot rules | HandleDistributionSettingsAsync → PlayerGroupRuntime.ChangeLootRules | Live (UOW-2556) |
| Alliance loot rules | HandleDistributionSettingsAsync → PlayerAllianceRuntime.ChangeLootRules | Live (UOW-2559) |
| League loot rules | — | Deferred |

## New Server Packets This Session

| Packet | Opcode | Java source |
|--------|--------|-------------|
| SmShowNpcOnMap | 89 | SM_SHOW_NPC_ON_MAP |

## Validation Decisions

| UOW | Command | Result | Trigger |
|-----|---------|--------|---------|
| 2558 | `--filter "ExchangeAddKinahPlanService\|PlayerExchangeRequestService\|SmExchangeAddItemPacketTests"` | 27/27 | none |
| 2559 | `--filter "PlayerAllianceRuntime\|CmMoveItemTests"` | 38/38 | none |

## Context Needed By Next Session

### Exchange item basket
- `Player.ExchangeItems`: Dictionary objectId → committedCount; cleared on exchange start/cancel
- `HandleExchangeAddItemAsync` validates: IsTrading, IsExchangeLocked, ExchangeItems.Count<18, item in cube, template.IsTradeable
- Full-stack or non-stackable (MaxStackCount<=1): sends SmDeleteItem(PutToExchangeDeleteType=0x26)
- Partial stack: sends SmInventoryUpdateItem(PutToExchange=0x25) showing remaining count
- Sends SmExchangeAddItem to both parties with the committed-count display item
- Trade execution: when both players confirm, kinah transfers; items deferred
- Cancel: RestoreExchangeItemsToInventoryUiAsync sends PlayerExchangeGetBack(0x23) UI packets

### Alliance loot rules
- `PlayerAllianceRuntime.ChangeLootRules(allianceId, rules)` → `IReadOnlyList<PlayerAllianceInfoIntent>?`
- Returns null if alliance not found; updates descriptor + ApplySnapshot
- Leader check done in HandleDistributionSettingsAsync before calling ChangeLootRules

### NPC search
- `SmShowNpcOnMap` opcode 89; `NpcSpawnTable.GetFirstSpawnByNpcId(worldId, npcId)` used
- STR_FIND_POS_UNKNOWN_NAME message ID = 1300747

### Previous context (unchanged)
- Windstream, level-ready, protection active, updateNearbyQuests all live as documented in Session 2548 handoff
- Group loot distribution settings live for group + alliance

## Next Recommended UOW

**UOW-2560: Exchange trade execution with items**

Currently `ExecuteKinahOnlyExchangeAsync` only transfers kinah. To complete exchange:
1. For each item in player.ExchangeItems: remove from player's inventory, add to partner's inventory
2. For each item in partner.ExchangeItems: remove from partner's inventory, add to player's inventory  
3. Persist both inventory changes
4. Send SmInventoryAddItem(PlayerExchangeGet) to each player for received items
5. Reset exchange state

Complexity: cross-player inventory mutation, persistence per item, proper slot assignment.

Alternative next UOWs:
1. **CM_REPLACE_ITEM cross-storage** — Extend HandleReplaceItemAsync to handle different source/dest storage types
2. **Alliance ChangeLootRules broadcast** — Verify SmAllianceInfo is sent correctly in alliance loot change
3. **Register remaining unregistered opcodes** — Still missing: 7 (CM_CHARACTER_EDIT), 14 (CM_CAPTCHA), 16/29/47/55/59/160/161 (legion), 159 (player search), 164 (quest share), 180 (debug)
4. **CM_OPEN_STATICDOOR skeleton** — Check if static door template data can produce a basic response

Focused validation recipe for UOW-2560:
- Behavior: items move from each player's inventory to the other's on double-confirm
- Focused C# command: `dotnet test --filter "FullyQualifiedName~ExchangeAddKinahPlanService\|PlayerExchangeRequestService"` + new item test
- Java/Maven: not expected
- Broad-validation trigger: none

## Remaining Risks

- Exchange item trade execution deferred (cross-player inventory mutation complex)
- No immediate kinah persistence (saved at next logout/periodic save)
- CM_REPLACE_ITEM cross-storage deferred
- CM_GATHER handler deferred (gathering controller not ported)
- Legion warehouse operations all deferred
- Transform system not yet ported
- XP/level-up system not ported
- CM_LEVEL_READY: SM_RIFT_ANNOUNCE, SM_CONQUEROR_PROTECTOR deferred
