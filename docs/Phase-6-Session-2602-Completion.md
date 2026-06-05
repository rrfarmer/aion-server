# Phase 6 Session 2602 Completion

## UOW

[Phase 6] UOW-2602: Send private-store seller messages before kinah packets

## Status

Completed and validated with focused live packet coverage. Private-store seller sale messages now fan out from the live
`CM_BUY_ITEM` handler before seller kinah add/update packets, preserving the Java per-bought-item seller notification
order for covered sold-out, multi-row, and partial-stack purchases.

## Runtime Progress Gate

```text
Runtime progress gate:
- Deferred/live behavior being advanced: seller sale notification packets are sent from the Java per-bought-item point before buyer/seller kinah transfer packets.
- Java source method or runtime path: PrivateStoreService.sellStoreItem per-item loop -> decreaseItemFromPlayer -> ItemService.addItem -> PacketSendUtility.sendPacket(seller, STR_MSG_PERSONAL_SHOP_SELL_ITEM[_MULTI]) -> buyer.decreaseKinah -> seller.increaseKinah.
- C# runtime artifact wired or fixed: GameServerConnection.TryExecutePrivateStorePurchaseAsync seller packet fanout ordering for seller item packets, seller messages, and seller kinah packets.
- Client-visible/state effect changed: seller receives sale system messages before seller kinah add/update packets from live CM_BUY_ITEM, with multi-row messages preserving bought-item order.
- Why this is not preview-only/test-only/documentation-only: it changes real server packets emitted by live CM_BUY_ITEM.
```

## Java Source Reviewed

- `PrivateStoreService.sellStoreItem` sends the seller sale system message inside the per-bought-item loop after seller
  item decrease and buyer item add.
- `buyer.getInventory().decreaseKinah(price)` and `seller.getInventory().increaseKinah(price)` happen after the loop.
- The Java direct seller order for a sold-out item is therefore seller delete/cube, seller sale message, then seller
  kinah packets.
- For multiple bought rows, Java repeats item packet(s) and sale message per row before the kinah transfer.

## C# Changes

- Changed `GameServerConnection.TryExecutePrivateStorePurchaseAsync` to iterate applied private-store bought items for
  seller direct fanout.
- Interleaved seller delete/update packets with seller sale messages for each applied bought item.
- Moved seller sale messages before the seller kinah add/cube/update packet branch.
- Kept seller kinah transfer packets after all seller item/message fanout.
- Updated focused live handler tests to assert seller sale messages precede seller kinah packets for sold-out,
  multi-row, and partial-stack private-store purchases.

## Known Gaps

- Full cross-recipient interleaving is still not Java-equivalent: Java sends buyer item add/update before the seller sale
  message inside each bought-item loop; C# still sends all seller item/message fanout before buyer item fanout because
  the purchase plan does not yet retain per-bought-item buyer packet groups.
- Inventory/kinah/store persistence is still not wired for private-store purchases.
- Java exchange-log/audit writes remain unported.
- Full Java `PrivateStore` object behavior remains unported.
- Buyer inventory-full, insufficient-kinah, stale seller-count, missing-template, and some mixed-item branches still need
  live coverage or fixes.
- Real client validation was not run.

## Validation Decision

```text
Validation decision:
- Changed surface: live CM_BUY_ITEM private-store seller packet ordering.
- Specific behavior/contract: seller sale messages are emitted after seller item delete/update packets and before seller kinah add/update packets, preserving per-bought-item message order for covered branches.
- Focused C# command: dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~GamePacketTests" --no-restore -> 311/311 passed.
- Focused Java/Maven command: none; no narrow Java fixture exists, and behavior was taken from reviewed Java source.
- Broad-validation trigger: live packet output changed.
- Broad .NET decision: skipped after focused live handler and packet serializer coverage; the filter built Aion.GameServer and directly covered the modified ProcessPacketAsync branch.
- Why this scope is sufficient: the passing live tests dispatch encoded CM_BUY_ITEM through the handler and assert the Java-derived direct seller packet order.
```

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.PrivateStoreService#sellStoreItem` | `Aion.GameServer.Network.Aion.GameServerConnection.TryExecutePrivateStorePurchaseAsync` | Live handler | Partial | Unit Tested | Partial Parity | Direct seller item/message/kinah order now matches covered Java order; buyer item fanout is still not globally interleaved before seller messages. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` private-store sale messages | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` private-store sale messages | Packet/fanout | Partial | Unit Tested | Partial Parity | Covered only for sale message placement relative to seller item and kinah packets. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_CmBuyItemPlayerPrivateStoreExecutesSingleItemPurchaseAndClosesStore` | Unit/live handler | `PrivateStoreService.sellStoreItem` | Sold-out seller message is sent before seller kinah add/update packets | Java source review + live C# handler assertion | Does not prove global buyer/seller interleaving. |
| `ProcessPacketAsync_CmBuyItemPlayerPrivateStoreMultiAddUsesPerItemCubeSnapshots` | Unit/live handler | `PrivateStoreService.sellStoreItem` | Multi-row seller messages are interleaved with seller delete/cube rows before seller kinah packets | Java source review + live C# handler assertion | Buyer add packets still use a separate sink and are not globally interleaved before messages. |
| `ProcessPacketAsync_CmBuyItemPlayerPrivateStorePartialStackKeepsStoreOpenAndDecrementsPackCount` | Unit/live handler | `PrivateStoreService.sellStoreItem` | Partial-stack seller message is sent after seller item update and before seller kinah packets | Java source review + live C# handler assertion | Does not cover existing seller kinah normal sale. |

## Summary Metrics

- Focused UOW validation: 311 tests passed.
- Runtime progress: live private-store seller notification packets now precede seller kinah packets.
- Total Java artifacts touched/discovered this UOW: 2.
- Total C# artifacts touched: 2.
- Artifacts with verified parity: 0.
- Artifacts needing verification / partial parity: 2.
- Blocked artifacts: persistence, exchange logging, full Java `PrivateStore` object, cross-recipient buyer/seller fanout interleaving.
- Estimated overall Phase 6 completion: incremental; still far from replacement readiness.

## Remaining Risks

- Buyer item packets are not yet globally interleaved before seller messages as Java does.
- Private-store purchase persistence and transaction boundaries remain disabled.
- Real client behavior was not validated against a running Aion client.

## Next Runtime Candidate

UOW-2603 candidate: preserve per-bought-item buyer item fanout before seller messages from live `CM_BUY_ITEM`.

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: buyer item add/update packets should be emitted before each seller sale message from the same bought-item loop point Java uses.
- Java source method or runtime path: PrivateStoreService.sellStoreItem -> decreaseItemFromPlayer -> ItemService.addItem(buyer, item, count) -> PacketSendUtility.sendPacket(seller, STR_MSG_PERSONAL_SHOP_SELL_ITEM[_MULTI]).
- C# runtime artifact to wire or fix: PrivateStorePurchasePlanService per-bought-item buyer packet grouping and GameServerConnection.TryExecutePrivateStorePurchaseAsync buyer/seller fanout order.
- Client-visible/state effect expected: live CM_BUY_ITEM emits seller item packet(s), buyer item add/update packet(s), then seller sale message for each applied bought item, while keeping final kinah packets after all item/message fanout.
- Why this is not preview-only/test-only/documentation-only if feasible: it changes real server packets emitted by live CM_BUY_ITEM and may add runtime plan metadata only to drive that live fanout.
```
