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

---

## UOW-1165 Runtime Vector Artifact Design

Date: May 26, 2026

This section refines the artifact contract for a future Java runtime capture tool covering both `SM_TRADELIST` and `SM_TRADE_IN_LIST`.

It is a tooling design only. It does not claim runtime parity and does not enable C# live sends.

### Expanded Java Source Breadcrumbs

- `game-server/src/com/aionemu/gameserver/network/aion/AionServerPacket.java`
- `game-server/src/com/aionemu/gameserver/utils/PacketSendUtility.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_TRADELIST.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_TRADE_IN_LIST.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`
- `game-server/src/com/aionemu/gameserver/services/DialogService.java`
- `game-server/src/com/aionemu/gameserver/services/LimitedItemTradeService.java`
- `game-server/src/com/aionemu/gameserver/model/templates/tradelist/TradeListTemplate.java`
- `game-server/src/com/aionemu/gameserver/model/templates/goods/GoodsList.java`

### Serialization Capture Boundary

Java `AionServerPacket.write(AionConnection, ByteBuffer)` writes this frame shape:

1. two-byte packet length placeholder
2. obfuscated opcode from `Crypt.encodeServerPacketOpcode(getOpCode())`
3. `Crypt.staticServerPacketCode`
4. bitwise complement of the obfuscated opcode
5. packet body from `writeImpl`
6. final length patched into the first two bytes
7. encryption over the slice after the length

For parity vectors, the preferred artifact should record two payload forms:

| Field | Meaning | Why It Matters |
|---|---|---|
| `wireFrameHex` | The Java bytes after `AionServerPacket.write` and encryption for a deterministic test connection. | Useful only when the capture fixture can fix the crypt seed/key and C# reproduces the same encrypted frame. |
| `canonicalPayloadHex` | A stable test-only reconstruction of opcode plus `writeImpl` body before encryption, using the same little-endian packet-buffer convention as C# packet tests. | Preferred for serializer parity because it avoids Java runtime encryption state and socket timing. |
| `bodyHex` | The `writeImpl` body without opcode/framing. | Useful to localize opcode/framing mismatches from body serialization mismatches. |

If only one byte form is feasible, capture `canonicalPayloadHex` first. Do not compare encrypted socket frames unless crypt setup is deterministic and documented.

### Artifact JSON Shape

The future tool should emit one JSON file per scenario:

```json
{
  "schemaVersion": 1,
  "javaCommit": "<git sha>",
  "scenario": "buy-sellable-normal",
  "input": {
    "dialogActionId": 2,
    "targetObjectId": 9001,
    "playerObjectId": 1001,
    "npcId": 203060,
    "questId": 0,
    "lastPage": 0,
    "extendedRewardIndex": 0
  },
  "runtimeFacts": {
    "playerLegionLevel": 0,
    "vendorBuyModifier": 100,
    "tradeSellPriceRate": 80,
    "buyPriceModifier": 80,
    "npcCanSell": true,
    "npcCanBuy": true
  },
  "packets": [
    {
      "sequence": 0,
      "packetClass": "SM_TRADELIST",
      "opcode": 149,
      "semanticKey": "trade-list",
      "canonicalPayloadHex": "<hex>",
      "bodyHex": "<hex>",
      "wireFrameHex": null,
      "decoded": {
        "targetObjId": 9001,
        "tradeNpcTypeIndex": 1,
        "buyPriceModifier": 80,
        "fixedClientModifier": 100,
        "showBuyTab": true,
        "showSellTab": true,
        "tradeTabIds": [129],
        "limitedItems": [
          { "itemId": 186000001, "buyCount": 0, "sellLimit": 5 }
        ]
      }
    }
  ],
  "notes": []
}
```

For `TRADE_IN`, use `dialogActionId = 78`, `packetClass = "SM_TRADE_IN_LIST"`, `semanticKey = "trade-in-list"`, and include decoded fields:

| Decoded Field | Java Source |
|---|---|
| `targetObjId` | `npc.getObjectId()` |
| `tradeNpcTypeIndex` | `tlist.getTradeNpcType().index()` |
| `buyPriceModifier` | fixed `100` passed by `DialogService` |
| `fixedClientModifier` | literal `100` |
| `tradeTabIds` | raw `TradeListTemplate.getTradeTablist()` order |

For no-sell scenarios, use `packetClass = "SM_SYSTEM_MESSAGE"` and include:

| Decoded Field | Java Source |
|---|---|
| `messageId` | `STR_BUY_SELL_HE_DOES_NOT_SELL_ITEM` |
| `npcNameParam` | `npc.getObjectTemplate().getL10n()` |
| `semanticKey` | `buy-no-trade-list`, `buy-no-sellable-goods`, or `trade-in-no-template` |

### Minimum Scenario Matrix

Before enabling C# live sends, capture at least:

| Scenario | Dialog Action | Expected Packet | Required Runtime Facts |
|---|---|---|---|
| `buy-sellable-normal` | `BUY` / `2` | `SM_TRADELIST` | one allowed goods tab, no limited rows |
| `buy-limited-items` | `BUY` / `2` | `SM_TRADELIST` | limited rows from `LimitedItemTradeService`, player buy count defaults |
| `buy-no-template` | `BUY` / `2` | `SM_SYSTEM_MESSAGE` | missing `TradeListData.getTradeListTemplate` |
| `buy-all-goods-missing` | `BUY` / `2` | `SM_SYSTEM_MESSAGE` | template exists, referenced goods absent |
| `buy-legion-restricted` | `BUY` / `2` | `SM_SYSTEM_MESSAGE` | template exists, goods legion level exceeds player level |
| `buy-mixed-legion-tabs` | `BUY` / `2` | `SM_TRADELIST` | filtered tab order preserves source order |
| `buy-non-default-price` | `BUY` / `2` | `SM_TRADELIST` | non-default vendor modifier and source sell price rate |
| `trade-in-sellable` | `TRADE_IN` / `78` | `SM_TRADE_IN_LIST` | fixed modifier `100`, raw tab ids, no limited rows |
| `trade-in-no-template` | `TRADE_IN` / `78` | `SM_SYSTEM_MESSAGE` | missing `TradeListData.getTradeInListTemplate` |

### Suggested Implementation Shape

Prefer a Java test utility or standalone debug runner that stays out of production runtime:

1. Build deterministic player and NPC fixtures with fixed object ids.
2. Load the same Java static-data files used by the server.
3. For each scenario, call the Java `DialogService.onDialogSelect` path or the nearest stable wrapper that still executes Java's branch logic.
4. Intercept `PacketSendUtility.sendPacket(Player, AionServerPacket)` or the player's `AionConnection.sendPacket` call.
5. For each packet, capture packet class and semantic facts.
6. Serialize canonical bytes with a test-only buffer that writes opcode/body before encryption.
7. Optionally serialize wire bytes with deterministic `AionConnection` crypt state.
8. Emit one JSON artifact per scenario under a future `parity-artifacts/trade-list/java/` directory.

Avoid editing production packet classes for instrumentation if a test-only wrapper can reflect packet fields or intercept after construction. If reflection is used, document every private field name because field renames would invalidate the artifact generator.

### C# Comparison Targets

The C# verifier should consume the JSON artifacts and compare:

| Java Artifact Field | C# Source |
|---|---|
| `canonicalPayloadHex` for `SM_TRADELIST` | `SmTradeList` serialized from `SmTradeListPacketPlan` |
| `canonicalPayloadHex` for `SM_TRADE_IN_LIST` | `SmTradeInList` serialized from `SmTradeInListPacketPlan` |
| `SM_SYSTEM_MESSAGE` no-sell payload | future concrete no-sell packet test or existing system-message serializer once scoped |
| `runtimeFacts.buyPriceModifier` | `NpcDialogTradeRuntimeFactAdapterService` and `SmTradeListPacketPlanService` |
| `decoded.tradeTabIds` | `NpcDialogTradeListFactAdapterService` / packet plan |
| `decoded.limitedItems` | `NpcDialogLimitedItemFactAdapterService` |

Parity remains Partial or Needs Verification until the artifact generator exists, artifacts are checked in or reproducibly generated, and C# tests compare against them.

## UOW-1168 Implementation Plan Link

The follow-up implementation plan lives in `docs/TradeList-Java-Vector-Generator-Implementation-Plan.md`.

That plan covers the proposed Java test-only runner package, CLI contract, scenario fixture strategy, capture-point options, canonical byte writer, artifact output layout, C# verifier follow-up, readiness gates, and known risks. It remains documentation only; no Java vectors have been generated yet.
