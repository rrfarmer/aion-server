# Phase 6 Session 2601 Completion

## UOW

[Phase 6] UOW-2601: Send private-store seller kinah add fanout live

## Status

Completed and validated with focused live packet coverage. Private-store purchases now model Java's seller
`increaseKinah` branch when the seller starts without an existing kinah row: the live handler sends a zero-count kinah
add packet and cube update before the seller kinah increase update.

## Runtime Progress Gate

```text
Runtime progress gate:
- Deferred/live behavior being advanced: private-store seller kinah gain when the seller has no existing kinah item emits Java's kinah add/cube packet pair before the kinah increase update.
- Java source method or runtime path: PrivateStoreService.sellStoreItem -> seller.getInventory().increaseKinah(price) -> Storage.increaseKinah -> Storage.add(ItemFactory.newItem(KINAH, 0)) -> ItemPacketService.sendStorageUpdatePacket -> Storage.increaseItemCount -> ItemPacketService.sendItemPacket.
- C# runtime artifact wired or fixed: PrivateStorePurchasePlanService seller kinah creation metadata and GameServerConnection.TryExecutePrivateStorePurchaseAsync seller kinah packet fanout.
- Client-visible/state effect changed: sellers who had no kinah row receive SmInventoryAddItem(count 0), SmCubeUpdate, then SmInventoryUpdateItem IncreaseKinahCollect from live CM_BUY_ITEM.
- Why this is not preview-only/test-only/documentation-only: it changes real server packets emitted by live CM_BUY_ITEM and records the live seller inventory branch that caused them.
```

## Java Source Reviewed

- `PrivateStoreService.sellStoreItem` transfers kinah after the per-item sale loop by calling
  `seller.getInventory().increaseKinah(price)`.
- `Storage.increaseKinah` creates a kinah item with count `0` through `Storage.add` when `kinahItem == null`.
- `Storage.add` calls `ItemPacketService.sendStorageUpdatePacket`, which sends `SM_INVENTORY_ADD_ITEM` followed by
  `SM_CUBE_UPDATE.cubeSize`.
- `Storage.increaseItemCount` then sends the normal update packet through `ItemPacketService.sendItemPacket`.
- `ItemPacketService.sendItemPacket` sends `SM_INVENTORY_UPDATE_ITEM` for kinah because kinah is not deleted at count
  `0` and uses the provided `INC_KINAH_COLLECT` update type.

## C# Changes

- Added `PrivateStorePurchasePlan.SellerKinahWasCreated` to preserve Java's `kinahItem == null` branch through the live
  executor.
- Set `SellerKinahWasCreated` when the seller has no existing cube kinah row before the private-store purchase.
- Changed `GameServerConnection.TryExecutePrivateStorePurchaseAsync` to send zero-count seller kinah
  `SmInventoryAddItem.CreateItemCollect` and `SmCubeUpdate.CubeSizeSnapshot` before the seller
  `SmInventoryUpdateItem.IncreaseKinahCollect` packet when the flag is true.
- Strengthened live private-store buy tests to assert the zero-count kinah add, cube update, and update-only behavior
  when the seller already had kinah.

## Known Gaps

- Seller notification ordering is still not fully Java-equivalent: Java sends seller sale messages inside the per-item
  loop before buyer/seller kinah transfer; C# still sends seller kinah packets before seller messages.
- Inventory/kinah/store persistence is still not wired for private-store purchases.
- Java exchange-log/audit writes remain unported.
- Full Java `PrivateStore` object behavior remains unported.
- Buyer inventory-full, insufficient-kinah, stale seller-count, missing-template, and some mixed-item branches still need
  live coverage or fixes.
- Real client validation was not run.

## Validation Decision

```text
Validation decision:
- Changed surface: live CM_BUY_ITEM private-store seller kinah packet fanout and purchase-plan metadata.
- Specific behavior/contract: seller without an existing kinah row receives zero-count kinah add/cube before IncreaseKinahCollect; seller with existing kinah remains update-only.
- Focused C# command: dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~GamePacketTests" --no-restore -> 311/311 passed.
- Focused Java/Maven command: none; no narrow Java fixture exists, and behavior was taken from reviewed Java source.
- Broad-validation trigger: live packet output changed.
- Broad .NET decision: skipped after focused live handler and packet serializer coverage; the filter built Aion.GameServer and directly covered the modified ProcessPacketAsync branch.
- Why this scope is sufficient: the passing live tests dispatch encoded CM_BUY_ITEM through the handler and assert both the missing-kinah and existing-kinah seller packet contracts.
```

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.items.storage.Storage#increaseKinah` | `Aion.GameServer.Services.PrivateStorePurchasePlanService` | Service/plan | Partial | Unit Tested | Partial Parity | Plan now records Java's `kinahItem == null` branch for private-store seller kinah gain; broader inventory service behavior remains incomplete. |
| `com.aionemu.gameserver.model.items.storage.Storage#add` | `Aion.GameServer.Network.Aion.GameServerConnection.TryExecutePrivateStorePurchaseAsync` seller kinah fanout | Packet/fanout | Partial | Unit Tested | Partial Parity | Missing seller kinah row now sends zero-count add and cube update before increase update. |
| `com.aionemu.gameserver.services.item.ItemPacketService#sendStorageUpdatePacket` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryAddItem` / `SmCubeUpdate` | Packet/fanout | Partial | Unit Tested | Partial Parity | Covered for the private-store seller kinah add branch only; other storage families and add types are outside this UOW. |
| `com.aionemu.gameserver.services.item.ItemPacketService#sendItemPacket` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem` | Packet/fanout | Partial | Unit Tested | Partial Parity | Covered for seller kinah `INC_KINAH_COLLECT` after a newly created kinah row. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_CmBuyItemPlayerPrivateStoreExecutesSingleItemPurchaseAndClosesStore` | Unit/live handler | `Storage.increaseKinah` + `ItemPacketService` | Seller without kinah receives zero-count kinah add, cube update, and kinah increase update | Java source review + live C# handler assertion | Seller message ordering remains different. |
| `ProcessPacketAsync_CmBuyItemPlayerPrivateStoreMultiAddUsesPerItemCubeSnapshots` | Unit/live handler | `Storage.increaseKinah` + `ItemPacketService` | Multi-row seller without kinah receives zero-count kinah add/cube before update | Java source review + live C# handler assertion | Does not verify real client behavior. |
| `ProcessPacketAsync_CmBuyItemPlayerPrivateStorePartialStackKeepsStoreOpenAndDecrementsPackCount` | Unit/live handler | `Storage.increaseKinah` + `ItemPacketService` | Non-closing sale still sends missing-seller-kinah add/cube/update | Java source review + live C# handler assertion | Seller message ordering remains different. |
| `ProcessPacketAsync_CmBuyItemPlayerPrivateStoreMissingSellerInventoryKeepsStoreItemAndTransfersKinah` | Unit/live handler | `Storage.increaseKinah` | Seller with existing kinah remains update-only | Java source review + live C# handler assertion | Branch is mixed missing-item, not a normal sold item. |

## Summary Metrics

- Focused UOW validation: 311 tests passed.
- Runtime progress: live private-store seller kinah fanout now includes Java's missing-row add/cube packet pair.
- Total Java artifacts touched/discovered this UOW: 4.
- Total C# artifacts touched: 4.
- Artifacts with verified parity: 0.
- Artifacts needing verification / partial parity: 4.
- Blocked artifacts: persistence, exchange logging, full Java `PrivateStore` object, seller notification ordering.
- Estimated overall Phase 6 completion: incremental; still far from replacement readiness.

## Remaining Risks

- Seller sale message order is still likely mismatched relative to Java's per-item loop.
- Private-store purchase persistence and transaction boundaries remain disabled.
- Real client behavior was not validated against a running Aion client.

## Next Runtime Candidate

UOW-2602 candidate: fix seller private-store notification ordering from live `CM_BUY_ITEM`.

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: seller sale notification packets should be sent from the same per-bought-item point Java uses, before the buyer/seller kinah transfer packets.
- Java source method or runtime path: PrivateStoreService.sellStoreItem per-item loop -> decreaseItemFromPlayer -> ItemService.addItem -> PacketSendUtility.sendPacket(seller, STR_MSG_PERSONAL_SHOP_SELL_ITEM[_MULTI]) -> buyer.decreaseKinah -> seller.increaseKinah.
- C# runtime artifact to wire or fix: GameServerConnection.TryExecutePrivateStorePurchaseAsync seller packet fanout ordering for seller messages and kinah packets.
- Client-visible/state effect expected: seller receives sale system messages before seller kinah add/update packets for live private-store purchases, with multi-row messages preserving bought-item order.
- Why this is not preview-only/test-only/documentation-only if feasible: it changes real server packets emitted by live CM_BUY_ITEM.
```
