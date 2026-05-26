# Trade-In List Java Audit

Date: May 26, 2026
Unit of Work: UOW-1158

## Purpose

This document captures the Java source behavior for `DialogService` `TRADE_IN` and `SM_TRADE_IN_LIST` before the C# port adds a concrete trade-in packet plan or serializer.

Java remains the source of truth. This audit is read-only and does not claim runtime parity.

## Java Source Breadcrumbs

- `game-server/src/com/aionemu/gameserver/services/DialogService.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_TRADE_IN_LIST.java`
- `game-server/src/com/aionemu/gameserver/network/aion/ServerPacketsOpcodes.java`
- `game-server/src/com/aionemu/gameserver/dataholders/TradeListData.java`
- `game-server/src/com/aionemu/gameserver/model/templates/tradelist/TradeListTemplate.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`

## DialogService TRADE_IN Route

Java `DialogService.onDialogSelect` handles `TRADE_IN` as follows:

1. Resolve `TradeListTemplate tradeListTemplate = DataManager.TRADE_LIST_DATA.getTradeInListTemplate(npc.getNpcId())`.
2. If the template is missing, send `SM_SYSTEM_MESSAGE.STR_BUY_SELL_HE_DOES_NOT_SELL_ITEM(npc.getObjectTemplate().getL10n())`.
3. If the template exists, send `new SM_TRADE_IN_LIST(npc, tradeListTemplate, 100)`.

Important differences from `BUY`:

- No `PricesService.getVendorBuyModifier()` call is used.
- No `sell_price_rate` multiplication is used at the `DialogService` call site.
- No goods-list lookup is used before sending.
- No player legion-level filtering is used.
- No limited-item lookup is used.
- The packet receives fixed `buyPriceModifier = 100`.

## TradeListData Dependencies

`TradeListData` has separate maps:

| Java Field | XML Element | Lookup Method | Notes |
|---|---|---|---|
| `npctlistData` | `tradelist_template` | `getTradeListTemplate(int id)` | Used by `BUY`. |
| `npcTradeInlistData` | `trade_in_list_template` | `getTradeInListTemplate(int id)` | Used by `TRADE_IN`. |
| `npcPurchaseTemplateData` | `purchase_template` | `getPurchaseTemplate(int id)` | Not part of this audit. |

`TradeListData.validateBuyLists` warns when an NPC supports `TRADE_IN` but lacks a trade-in list. This warning does not change runtime packet behavior.

## SM_TRADE_IN_LIST Write Order

Java `ServerPacketsOpcodes` registers `SM_TRADE_IN_LIST` with opcode `151`.

Java `SM_TRADE_IN_LIST.writeImpl` writes fields only when:

- `tlist != null`
- `tlist.getNpcId() != 0`
- `tlist.getCount() != 0`

When those guards pass, Java writes:

| Order | Java Write | Source |
|---|---|---|
| 1 | `writeD(npc.getObjectId())` | Runtime NPC object id. |
| 2 | `writeC(tlist.getTradeNpcType().index())` | Trade NPC type enum index. |
| 3 | `writeD(buyPriceModifier)` | Fixed `100` from `DialogService` for `TRADE_IN`. |
| 4 | `writeD(100)` | Fixed Aion 4.5 client modifier. |
| 5 | `writeH(tlist.getCount())` | Raw trade-tab count. |
| 6 | `writeD(tradeTab.getId())` for each tab | Tab ids in template order. |

There are no buy/sell tab visibility bytes and no limited-item rows in `SM_TRADE_IN_LIST`.

## C# Surfaces Already Present

Current C# already has partial non-live trade-in metadata:

- `DialogActionRegistry` maps action `78` to `TRADE_IN`.
- `TradeListTable.GetTradeInListTemplate(int npcId)` mirrors Java lookup naming.
- `NpcDialogTradeListFactAdapterService` records `HasTradeInList` and `TradeInList`.
- `NpcDialogServiceSelectPlanService` can emit a non-live `TradeInListPacket` descriptor or no-sell descriptor.
- Static-data loading already distinguishes `trade_in_list_template`.

Current C# gaps:

- No concrete `SmTradeInListPacketPlan`.
- No `SmTradeInList` serializer.
- `GameServerConnection.HandleDialogSelectAsync` does not route `TRADE_IN` through the staged dialog plan boundary.
- No packet payload tests or Java runtime golden vectors exist.

## Recommended C# Port Shape

Before live sends, add a non-live packet-plan and serializer slice:

1. `SmTradeInListPacketPlanInput`:
   - `TargetObjectId`
   - `TradeListTemplateSummary`
   - `BuyPriceModifier = 100`
2. `SmTradeInListPacketPlan`:
   - status `Ready` only when trade NPC type is known, NPC id is non-zero, and tab count is non-zero.
   - `TradeNpcTypeIndex`
   - `TradeTabIds`
   - Java source metadata.
3. `SmTradeInList` packet:
   - follows Java write order exactly.
   - rejects non-ready plans.
4. Tests:
   - ready payload order with a fixed modifier `100`.
   - empty template/tab guard.
   - unknown trade NPC type guard.
   - non-live `DialogService` descriptor remains no-sending.

## Migration Parity Checklist

Do not mark trade-in packet parity verified until:

- Java `SM_TRADE_IN_LIST.writeImpl` bytes are captured or a deterministic source-derived payload test is added and clearly marked Partial Parity.
- C# trade NPC type indexes are checked against Java `TradeNpcType.index()`.
- Empty-template and empty-tab behavior is represented.
- No-sell `TRADE_IN` missing-template branch is tested through staged planning.
- Live `TRADE_IN` send wiring remains disabled until NPC AI/controller routing and Java runtime packet vectors are ready.

## Remaining Risks

- Java `writeImpl` silently emits an empty payload body for invalid templates; the C# packet guard should decide whether to reject such plans or represent a non-ready packet-plan status.
- Trade-in purchase execution is separate from showing the trade-in list and is not covered by this audit.
- Trade-in AP/required-item formulas exist elsewhere and must not be inferred from `SM_TRADE_IN_LIST`.
- Runtime packet golden vectors are still needed even though source review pins opcode `151`.
