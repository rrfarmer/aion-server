# Phase 6 Session 2600 Completion

## UOW

[Phase 6] UOW-2600: Snapshot private-store cube updates per bought item

## Status

Completed and validated with focused live packet coverage. Multi-row private-store purchases now emit per-item cube
count snapshots for seller deletes and buyer adds instead of reusing the final post-mutation player snapshot for every
cube update.

## Runtime Progress Gate

```text
Runtime progress gate:
- Deferred/live behavior being advanced: multi-item private-store purchases emit Java-equivalent cube counts after each seller item delete and buyer new-item add.
- Java source method or runtime path: PrivateStoreService.sellStoreItem -> decreaseItemFromPlayer -> Storage.decreaseItemCount -> ItemPacketService.sendItemDeletePacket, then ItemService.addItem -> Storage.add -> ItemPacketService.sendStorageUpdatePacket inside the per-bought-item loop.
- C# runtime artifact wired or fixed: GameServerConnection.TryExecutePrivateStorePurchaseAsync seller delete and buyer add SmCubeUpdate fanout.
- Client-visible/state effect changed: live CM_BUY_ITEM private-store purchases that affect multiple cube rows send stepped cube counts, for example seller 1 then 0 and buyer 1 then 2.
- Why this is not preview-only/test-only/documentation-only: it changes real server packets emitted from the live CM_BUY_ITEM purchase execution path.
```

## Java Source Reviewed

- `PrivateStoreService.sellStoreItem` loops over bought items and calls `decreaseItemFromPlayer`, `ItemService.addItem`,
  and the seller notification before the next bought item.
- `Storage.decreaseItemCount` deletes an exhausted cube item through `ItemPacketService.sendItemDeletePacket`.
- `ItemPacketService.sendItemDeletePacket` sends `SM_DELETE_ITEM` and then `SM_CUBE_UPDATE.cubeSize`.
- `Storage.add` sends `ItemPacketService.sendStorageUpdatePacket` after each new item add.
- `ItemPacketService.sendStorageUpdatePacket` sends `SM_INVENTORY_ADD_ITEM` and then `SM_CUBE_UPDATE.cubeSize`.
- `SM_CUBE_UPDATE.cubeSize(StorageType.CUBE, player)` reads the current inventory size at the packet fanout point.

## C# Changes

- Captured seller and buyer cube counts before applying the private-store purchase mutation batch.
- Changed seller sold-out delete fanout to decrement a projected seller cube count for each deleted item and send
  `SmCubeUpdate.CubeSizeSnapshot(...)`.
- Changed buyer new-item add fanout to increment a projected buyer cube count for each added cube item and send
  `SmCubeUpdate.CubeSizeSnapshot(...)`.
- Added a live `CM_BUY_ITEM` test for a two-row private-store purchase that asserts seller cube counts `1, 0` and buyer
  cube counts `1, 2`.

## Known Gaps

- Packet interleaving across buyer and seller channels remains approximated by separate test sinks; this UOW verifies
  per-recipient packet order/counts, not a single global send timeline.
- Private-store purchase persistence is still not wired.
- Java exchange-log/audit writes remain unported.
- Seller-without-kinah behavior still differs: Java creates a zero-count kinah row through `Storage.add`, which sends an
  add/cube pair before the kinah increase update; C# currently sends only the seller kinah update.
- Full Java `PrivateStore` object behavior remains unported.
- Buyer inventory-full, insufficient-kinah, stale seller-count, missing-template, and some mixed-item branches still need
  live coverage or fixes.
- Real client validation was not run.

## Validation Decision

```text
Validation decision:
- Changed surface: live CM_BUY_ITEM private-store purchase packet fanout.
- Specific behavior/contract: multi-row private-store purchase sends per-item seller delete cube counts and buyer add cube counts matching Java fanout-point inventory sizes.
- Focused C# command: dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~GamePacketTests" --no-restore -> 311/311 passed.
- Focused Java/Maven command: none; no narrow Java fixture exists, and behavior was taken from reviewed Java source.
- Broad-validation trigger: live packet output changed.
- Broad .NET decision: skipped after focused live handler and packet serializer coverage; the filter built Aion.GameServer and directly covered the modified ProcessPacketAsync branch.
- Why this scope is sufficient: the new test dispatches encoded CM_BUY_ITEM through the live handler and asserts the Java-derived cube count payloads.
```

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.PrivateStoreService#sellStoreItem` | `Aion.GameServer.Network.Aion.GameServerConnection.TryExecutePrivateStorePurchaseAsync` | Live handler | Partial | Unit Tested | Partial Parity | Multi-row purchase cube count snapshots now follow Java per-bought-item fanout for covered seller delete and buyer add branches; global cross-recipient packet interleaving and persistence remain incomplete. |
| `com.aionemu.gameserver.model.items.storage.Storage#decreaseItemCount` | `Aion.GameServer.Network.Aion.GameServerConnection.TryExecutePrivateStorePurchaseAsync` seller deleted-item fanout | Packet/fanout | Partial | Unit Tested | Partial Parity | Exhausted seller cube rows now send stepped delete cube counts for multi-delete purchases. |
| `com.aionemu.gameserver.model.items.storage.Storage#add` | `Aion.GameServer.Network.Aion.GameServerConnection.TryExecutePrivateStorePurchaseAsync` buyer added-item fanout | Packet/fanout | Partial | Unit Tested | Partial Parity | New buyer cube rows now send stepped add cube counts for multi-add purchases. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE` | `Aion.GameServer.Network.Aion.ServerPackets.SmCubeUpdate.CubeSizeSnapshot` | Packet | Partial | Unit Tested | Partial Parity | Snapshot helper was reused to model Java fanout-point inventory size; broader storage families are outside this UOW. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_CmBuyItemPlayerPrivateStoreMultiAddUsesPerItemCubeSnapshots` | Unit/live handler | `PrivateStoreService.sellStoreItem`, `Storage.decreaseItemCount`, `Storage.add`, `ItemPacketService` | Two-row private-store purchase sends seller cube counts `1, 0` and buyer cube counts `1, 2` from live `CM_BUY_ITEM` | Java source review + live C# handler assertion | Does not verify real client behavior or global cross-recipient send interleaving. |

## Summary Metrics

- Focused UOW validation: 311 tests passed.
- Runtime progress: live private-store multi-row purchase packet fanout now uses Java-style per-item cube count snapshots.
- Total Java artifacts touched/discovered this UOW: 4.
- Total C# artifacts touched: 3.
- Artifacts with verified parity: 0.
- Artifacts needing verification / partial parity: 4.
- Blocked artifacts: persistence, exchange logging, full Java `PrivateStore` object, seller-without-kinah add/cube fanout.
- Estimated overall Phase 6 completion: incremental; still far from replacement readiness.

## Remaining Risks

- Private-store purchase persistence and transaction boundaries are still disabled.
- Seller kinah row creation packet fanout is likely still mismatched when the seller starts without kinah.
- Real client behavior was not validated against a running Aion client.

## Next Runtime Candidate

UOW-2601 candidate: fix seller-without-kinah private-store purchase fanout from live `CM_BUY_ITEM`.

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: private-store seller kinah gain when the seller has no existing kinah item should emit Java's kinah add/cube packet pair before the kinah increase update.
- Java source method or runtime path: PrivateStoreService.sellStoreItem -> seller.getInventory().increaseKinah(price) -> Storage.increaseKinah -> Storage.add(ItemFactory.newItem(KINAH, 0)) -> ItemPacketService.sendStorageUpdatePacket -> Storage.increaseItemCount -> ItemPacketService.sendItemPacket.
- C# runtime artifact to wire or fix: PrivateStorePurchasePlanService seller kinah plan metadata and GameServerConnection.TryExecutePrivateStorePurchaseAsync seller kinah packet fanout.
- Client-visible/state effect expected: seller receives the Java-equivalent kinah add/cube packets before SmInventoryUpdateItem IncreaseKinahCollect when no kinah row existed before the sale.
- Why this is not preview-only/test-only/documentation-only if feasible: it changes real server packets emitted by live CM_BUY_ITEM and mutates/uses live seller inventory state.
```
