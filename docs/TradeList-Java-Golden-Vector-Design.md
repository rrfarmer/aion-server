# Trade List Java Golden Vector Design

Date: May 26, 2026
Unit of Work: UOW-1156

## Purpose

This document defines the Java-side runtime artifact shape needed to compare `SM_TRADELIST` and the no-sell `SM_SYSTEM_MESSAGE.STR_BUY_SELL_HE_DOES_NOT_SELL_ITEM` against the C# port before production `BUY` packet sends are enabled.

Java remains the source of truth. This design does not generate artifacts yet and does not claim runtime parity.

## Java Source Breadcrumbs

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_DIALOG_SELECT.java`
- `game-server/src/com/aionemu/gameserver/controllers/NpcController.java`
- `game-server/src/com/aionemu/gameserver/services/DialogService.java`
- `game-server/src/com/aionemu/gameserver/services/trade/PricesService.java`
- `game-server/src/com/aionemu/gameserver/configs/main/PricesConfig.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_TRADELIST.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`
- `game-server/src/com/aionemu/gameserver/services/LimitedItemTradeService.java`
- `game-server/src/com/aionemu/gameserver/model/limiteditems/LimitedItem.java`

## Runtime Sequence To Observe

Record one complete `CM_DIALOG_SELECT` `BUY` handling sequence from packet input through all packets sent to the player:

1. `CM_DIALOG_SELECT.readImpl` input:
   - `targetObjectId`
   - `dialogActionId`
   - `lastPage`
   - `questId`
   - `extendedRewardIndex`
2. `CM_DIALOG_SELECT.runImpl` resolves the active player and target object.
3. `NpcController.onDialogSelect` validates talk range, calls NPC AI, then falls back to `DialogService.onDialogSelect` only when AI does not handle the action.
4. `DialogService.onDialogSelect` `BUY` resolves `TradeListTemplate` through `DataManager.TRADE_LIST_DATA.getTradeListTemplate(npc.getNpcId())`.
5. If no trade list exists, Java sends `SM_SYSTEM_MESSAGE.STR_BUY_SELL_HE_DOES_NOT_SELL_ITEM(npc.getObjectTemplate().getL10n())`.
6. If a trade list exists, Java calculates:
   - `tradeModifier = tradeListTemplate.getSellPriceRate()`
   - `legionLevel = player.getLegion() == null ? 0 : player.getLegion().getLegionLevel()`
   - `buyPriceModifier = PricesService.getVendorBuyModifier() * tradeModifier / 100`
7. Java scans trade tabs and treats the NPC as sellable only when at least one referenced goods list exists and its legion level is not greater than the player legion level.
8. If no goods are sellable, Java sends the same no-sell `SM_SYSTEM_MESSAGE`.
9. If goods are sellable, Java sends `new SM_TRADELIST(player, npc, tradeListTemplate, buyPriceModifier)`.
10. `SM_TRADELIST` re-filters tabs by goods-list existence and legion level, then appends limited items from `LimitedItemTradeService.getLimitedTradeNpc(tlist.getNpcId())`.

## Packet Observation Schema

Record packets in exact send order before encryption if possible:

| Field | Description |
|---|---|
| `sequence` | Monotonic zero-based packet index for this player and input. |
| `packetClass` | Java server packet class name, such as `SM_TRADELIST` or `SM_SYSTEM_MESSAGE`. |
| `opcode` | Encoded packet opcode if available from the server packet instance or serialized payload. |
| `payloadHex` | Unencrypted serialized payload bytes, including opcode in the same shape expected by C# packet tests. |
| `semanticKey` | Stable label: `trade-list`, `buy-no-trade-list`, `buy-no-sellable-goods`, or `buy-restricted-goods`. |
| `messageId` | System message id, when `packetClass` is `SM_SYSTEM_MESSAGE`. |
| `messageParams` | Ordered system-message parameters, when available. |
| `targetObjectId` | NPC object id from the dialog input and packet payload. |
| `playerObjectId` | Player object id used for limited-item buy-count lookup. |
| `npcId` | NPC template id used for trade-list and limited-item lookup. |

## Trade-List Semantic Fields

For `SM_TRADELIST`, include decoded fields alongside bytes so C# comparisons can separate byte mismatches from fact mismatches:

| Field | Java Source |
|---|---|
| `targetObjId` | `npc.getObjectId()` |
| `playerObjId` | `player.getObjectId()` used by `LimitedItem.getBuyCount(playerObjId)` |
| `tradeNpcTypeIndex` | `tradeListTemplate.getTradeNpcType().index()` |
| `vendorBuyModifier` | `PricesService.getVendorBuyModifier()` |
| `tradeSellPriceRate` | `tradeListTemplate.getSellPriceRate()` |
| `buyPriceModifier` | `vendorBuyModifier * tradeSellPriceRate / 100` |
| `fixedClientModifier` | Literal `100` written after `buyPriceModifier` |
| `showBuyTab` | `npc.canSell()` |
| `showSellTab` | `npc.canBuy()` |
| `playerLegionLevel` | `player.getLegion() == null ? 0 : player.getLegion().getLegionLevel()` |
| `tradeTabIds` | Filtered `TradeTab.getId()` values written by `SM_TRADELIST` |
| `limitedItems` | Ordered item id, buy count, and sell limit rows from `LimitedItemTradeService` |

## No-Sell Semantic Fields

For `SM_SYSTEM_MESSAGE.STR_BUY_SELL_HE_DOES_NOT_SELL_ITEM`, include:

| Field | Java Source |
|---|---|
| `messageId` | `1300336` |
| `npcNameParam` | `npc.getObjectTemplate().getL10n()` |
| `chatType` | Constructor default from `SM_SYSTEM_MESSAGE` |
| `senderObjId` | Constructor default from `SM_SYSTEM_MESSAGE` |
| `specialParams` | Empty unless Java constructor path changes |

## Required Scenarios

Minimum scenarios before live `BUY` send enablement:

| Scenario | Purpose |
|---|---|
| Sellable normal trade list with one goods tab | Confirms basic `SM_TRADELIST` bytes, opcode, trade type, tab count, and price modifier. |
| Sellable list with limited item rows | Confirms limited item item id, per-player buy count, and sell limit serialization. |
| Missing trade-list template | Confirms no-sell system message id and NPC-name parameter. |
| Existing trade list with all goods missing | Confirms Java sends no-sell instead of an empty `SM_TRADELIST`. |
| Existing trade list with goods restricted by player legion level | Confirms legion filter and no-sell branch. |
| Existing trade list with mixed restricted and allowed tabs | Confirms filtered tab order and non-empty packet emission. |
| Non-default vendor buy modifier | Confirms `PricesConfig.VENDOR_BUY_MODIFIER * sell_price_rate / 100` integer math. |
| Player with live legion level | Confirms `player.getLegion().getLegionLevel()` path instead of no-legion fallback `0`. |

## Suggested Capture Points

Preferred low-intrusion Java capture points:

- Wrap or instrument `PacketSendUtility.sendPacket(Player, AionServerPacket)` to record packet class, serialized bytes, and semantic metadata.
- Add a test-only capture helper around `DialogService.onDialogSelect` `BUY` for resolved trade list, player legion level, vendor modifier, and sellable-goods decision.
- For `SM_TRADELIST`, expose or reflect packet private fields before serialization only in test tooling; the canonical comparison remains serialized bytes.
- For limited items, snapshot `LimitedItemTradeService.getLimitedTradeNpc(npcId)` immediately before packet construction.

If byte serialization is difficult to extract at `PacketSendUtility`, record packet class and semantic fields first, then add payload bytes in a later artifact version. Without bytes, parity must remain Needs Verification or Partial Parity for packet serialization.

## Comparison Rules

- Compare packet order exactly; `BUY` should produce exactly one packet in the scoped Java fallback branch.
- Compare `payloadHex` exactly after both sides use the same unencrypted packet framing convention.
- Compare decoded semantic fields before bytes to localize differences.
- Treat trade tab order as significant because Java writes tabs in `TradeListTemplate.getTradeTablist()` order after filtering.
- Treat limited item order as significant unless Java `LimitedTradeNpc.getLimitedItems()` is proven unordered for the source collection.
- Do not normalize object ids for this artifact; use deterministic fixture player/NPC object ids in Java and C#.
- Do not mark `SM_TRADELIST`, `SM_SYSTEM_MESSAGE.STR_BUY_SELL_HE_DOES_NOT_SELL_ITEM`, `DialogService.onDialogSelect BUY`, `PricesService.getVendorBuyModifier`, or `Legion.getLegionLevel` as Verified Parity until artifacts exist and are compared.

## Current C# Comparison Targets

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/NpcDialogTradeRuntimeFactAdapterService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/NpcDialogTradeListFactAdapterService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/NpcDialogLimitedItemFactAdapterService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/SmTradeListPacketPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmTradeList.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionStorageExpansionDialogTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/SmTradeListPacketPlanServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`

## Current Blockers

- Local Java runtime capture remains blocked until the Java toolchain and fixture runner are available.
- C# `BUY` send wiring remains intentionally disabled.
- C# price config and live legion-level lookup are staged and non-live.
- C# limited-item lifecycle does not yet include Java cron resets, sell-limit mutation, or per-player buy-count mutation.
- NPC AI/controller routing is not live-wired into the production socket handler for `BUY`.
