# Phase 6 Session 2557 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2557: Port CM_OBJECT_SEARCH handler with SmShowNpcOnMap server packet

## Full Session Summary (UOWs 2549–2557)

| UOW | Summary |
|-----|---------|
| 2549 | Register CM_GATHER (opcode 19); 4 parser tests |
| 2550 | Register 5 parsers: exchange add item/kinah (64/66), replace item (178), group loot (184), distribution settings (185) |
| 2551 | Live CM_EXCHANGE_ADD_KINAH + Player.ExchangeKinah basket tracking |
| 2552 | Exchange inventory update constants: PlayerExchangeGet (0x21), PlayerExchangeGetBack (0x23), PutToExchange (0x25/0x26) |
| 2553 | Register CM_HOUSE_TELEPORT (222), CM_HOUSE_OPEN_DOOR (226); CmBindPointTeleport dispatch stub |
| 2554 | Kinah-only exchange trade execution; SmExchangeConfirmation.Success = 0 constant |
| 2555 | Live CM_REPLACE_ITEM same-storage slot-swap handler |
| 2556 | Live CM_DISTRIBUTION_SETTINGS group loot rules via PlayerGroupRuntime.ChangeLootRules |
| 2557 | SmShowNpcOnMap (opcode 89) + live CM_OBJECT_SEARCH handler using NpcSpawnTable |

## Exchange System Status (Complete Picture)

| Packet/handler | Status | Notes |
|----------------|--------|-------|
| CM_EXCHANGE_REQUEST (63) | Live | Sends question to partner |
| CM_EXCHANGE_ADD_KINAH (66) | Live | Tracks Player.ExchangeKinah; sends SmExchangeAddKinah to both |
| CM_EXCHANGE_ADD_ITEM (64) | Deferred | Item basket not yet tracked on Player |
| CM_EXCHANGE_LOCK (67) | Live | Locks + notifies partner |
| CM_EXCHANGE_OK (68) | Live | Confirms; executes kinah-only trade when both confirmed |
| CM_EXCHANGE_CANCEL (69) | Live | Resets both players' exchange state |
| Trade execution (kinah only) | Live (UOW-2554) | No immediate persistence; saved at next logout/periodic save |
| Trade execution (items) | Deferred | Item basket not tracked |

### Exchange kinah-only trade flow (all live)
1. Player1 calls `HandleExchangeAddKinahAsync` → `Player1.ExchangeKinah += countToAdd`
2. Player2 calls `HandleExchangeAddKinahAsync` → `Player2.ExchangeKinah += countToAdd`
3. Both call `HandleExchangeOkAsync` → both confirm
4. `ExecuteKinahOnlyExchangeAsync`: transfer kinah, send `SmExchangeConfirmation(0)` to both, reset state

## New Gameplay Features (This Session)

| Feature | Java source | C# handler |
|---------|-------------|------------|
| NPC search `/find` | CM_OBJECT_SEARCH → SM_SHOW_NPC_ON_MAP | HandleObjectSearchAsync + SmShowNpcOnMap |
| Exchange kinah trade | ExchangeService.addKinah + performTrade | HandleExchangeAddKinahAsync + ExecuteKinahOnlyExchangeAsync |
| Item slot swap | CM_REPLACE_ITEM → ItemMoveService.switchItemsInStorages | HandleReplaceItemAsync (same-storage) |
| Group loot rules | CM_DISTRIBUTION_SETTINGS → PlayerGroupService.changeGroupRules | HandleDistributionSettingsAsync |

## Validation Decisions

| UOW | Command | Result | Trigger |
|-----|---------|--------|---------|
| 2549 | `--filter "CmGatherTests"` | 4/4 | none |
| 2551 | `--filter "ExchangeAddKinahPlanService\|PlayerExchangeRequestService"` | 25/25 | none |
| 2554 | `--filter "ExchangeAddKinahPlanService\|PlayerExchangeRequestService"` | 25/25 | none |
| 2555 | `--filter "CmMoveItemTests\|CmSplitItemTests"` | 6/6 | none |
| 2556 | `--filter "CmMoveItemTests\|PlayerGroupRuntime"` | 54/54 | none |
| 2557 | `--filter "CmMoveItemTests\|CmGatherTests"` | 8/8 | none |

## Context Needed By Next Session

### Exchange system
- `Player.ExchangeKinah` (long): kinah in exchange; reset on accept/cancel
- `Player.CurrentExchangePartnerObjectId`: trade partner object ID
- `ExchangeAddKinahPlanService.CreatePlan(requested, inventoryKinah, exchangeKinah)` → plan
- Exchange trade executes on double-confirm via `ExecuteKinahOnlyExchangeAsync`
- Items deferred: `SmInventoryUpdateItem.PlayerExchangeGet = 0x21`, `PutToExchange = 0x25`
- `SmDeleteItem.PutToExchangeDeleteType = 0x26`

### Object Search
- `SmShowNpcOnMap` opcode 89; writes npcId + worldId + instanceId + x/y/z
- `NpcSpawnTable.GetFirstSpawnByNpcId(worldId, npcId)` used (simplified vs Java's nearest-spawn race search)
- STR_FIND_POS_UNKNOWN_NAME message ID = 1300747

### Distribution Settings
- `HandleDistributionSettingsAsync` handles group path only (alliance/league deferred)
- Calls `_playerGroupRuntime.ChangeLootRules(teamId, newRules)`
- Leader-only; LootRule int → PlayerGroupLootRuleType: 0=FreeForAll, 1=RoundRobin, 2=Leader

### Previous context (unchanged)
- Windstream: all live; level-ready sends instance count, windstream announces, nearby quests
- Protection active stops on movement + broadcasts SmPlayerState
- updateNearbyQuests wired in level-ready and title-set

## Next Recommended UOW

**UOW-2558: Port CM_EXCHANGE_ADD_ITEM (exchange item basket)**

Prerequisite for live item trading. Needs:
1. Add `ExchangeItems` dictionary to Player model: `Dictionary<int, (int ItemObjectId, long Count, int ItemTemplateId)>`
2. Reset on exchange accept/cancel (already handles ExchangeKinah reset — add items reset there)
3. HandleExchangeAddItemAsync:
   - Find source item in cube
   - Check tradeable (item restriction table)
   - Add/update ExchangeItems entry
   - Send SmDeleteItem(PutToExchangeDeleteType) for full-stack or SmInventoryUpdateItem(PutToExchange) for partial
   - Send SmExchangeAddItem to both players

Alternative UOWs:
1. **Alliance loot rules** — Add `ChangeLootRules` to `PlayerAllianceRuntime`; extend `HandleDistributionSettingsAsync` for alliance path
2. **SM_SHOW_MAP responses** — Port action 0 (ConquerorAndProtectorService.intruderScan) using existing SM_CONQUEROR_PROTECTOR plan
3. **CM_OBJECT_SEARCH race-aware search** — Improve the simplified spawn search to prefer race-appropriate worlds
4. **Register remaining unregistered opcodes** — Still missing: 7, 14, 16, 29, 47, 55, 59, 159, 164, 180

Focused validation recipe for UOW-2558:
- Behavior: exchange add item sends SmDeleteItem/SmInventoryUpdateItem to self + SmExchangeAddItem to both
- Focused C# command: `dotnet test --filter "FullyQualifiedName~ExchangeAddItemPlanService"` (new test class)
- Java/Maven: not expected
- Broad-validation trigger: none

## Remaining Risks

- Exchange item basket not yet tracked on Player — item trades not possible
- Exchange trade execution: no immediate kinah persistence (saved at logout/periodic save)
- CM_REPLACE_ITEM cross-storage deferred (same-storage only)
- CM_DISTRIBUTION_SETTINGS alliance/league paths deferred
- CM_OBJECT_SEARCH: simplified spawn search (doesn't prefer race-appropriate maps)
- CM_GATHER handler deferred (gathering controller not ported)
- Legion warehouse operations all deferred
- Transform system not yet ported
- XP/level-up system not ported
- CM_LEVEL_READY: SM_RIFT_ANNOUNCE, SM_CONQUEROR_PROTECTOR deferred
