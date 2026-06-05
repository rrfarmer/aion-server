# Phase 6 Session 2599 Completion

## UOW

[Phase 6] UOW-2599: Send Java private-store delete packet fanout

## Status

Completed and validated with focused live packet coverage. Sold-out private-store purchases now send the seller
`SM_DELETE_ITEM` equivalent with Java's `ItemDeleteType.USE` mask and emit Java-style cube updates for seller item
deletion and buyer new-item add.

## Runtime Progress Gate

```text
Runtime progress gate:
- Deferred/live behavior being advanced: sold-out private-store purchase sends the same seller inventory delete packet type and cube-update follow-up Java sends after inventory removal, and buyer item adds receive the Java cube-update follow-up.
- Java source method or runtime path: PrivateStoreService.sellStoreItem -> decreaseItemFromPlayer -> Storage.decreaseItemCount -> ItemPacketService.sendItemDeletePacket; ItemService.addItem -> Storage.add -> ItemPacketService.sendStorageUpdatePacket.
- C# runtime artifact wired or fixed: GameServerConnection.TryExecutePrivateStorePurchaseAsync seller SmDeleteItem construction and buyer/seller SmCubeUpdate fanout.
- Client-visible/state effect changed: seller receives SmDeleteItem delete type USE plus SmCubeUpdate; buyer receives SmInventoryAddItem plus SmCubeUpdate from live CM_BUY_ITEM.
- Why this is not preview-only/test-only/documentation-only: it changes real server packets emitted from live CM_BUY_ITEM purchase execution.
```

## Java Source Reviewed

- `Storage.decreaseItemCount(Item, long, Player)` defaults to `ItemUpdateType.DEC_ITEM_USE`.
- `ItemDeleteType.fromUpdateType(DEC_ITEM_USE)` maps to `ItemDeleteType.USE`, mask `0x17`.
- `ItemPacketService.sendItemDeletePacket` sends `SM_DELETE_ITEM(itemObjectId, deleteType)` for cube storage and then `SM_CUBE_UPDATE.cubeSize(storageType, player)`.
- `ItemService.addItem` routes new inventory items through `Storage.add`, whose packet path sends the inventory add packet and then `SM_CUBE_UPDATE.cubeSize`.

## C# Changes

- Changed private-store seller deleted-item fanout from default `SmDeleteItem` delete type `0` to `SmDeleteItem.UseDeleteType`.
- Sent `SmCubeUpdate.CubeSize(seller)` after each seller deleted item from live private-store purchases.
- Sent `SmCubeUpdate.CubeSize(buyer)` after each buyer added item from live private-store purchases.
- Added `SmDeleteItem_UseDeleteTypeMatchesJava` to pin the `0x17` delete-type constant.
- Strengthened live `GameServerConnectionBuyItemTests` assertions for sold-out and partial-stack purchase packet order.

## Known Gaps

- Inventory/kinah/store persistence is still not wired for private-store purchases.
- Buyer inventory-full denial is already wired through buyer messages but still lacks live branch coverage.
- Multi-item purchase combinations, insufficient-kinah denial, stale seller item counts, missing item templates, and mixed present/missing seller item behavior still need live handler coverage or fixes.
- Java exchange-log/audit writes are not ported.
- Java's full `PrivateStore` object is still not ported.
- Multiple buyer added-item cube snapshots use the post-mutation player snapshot; a future multi-add live test should confirm whether per-add Java cube counts need more precise snapshots.
- Real client validation was not run.

## Validation Decision

```text
Validation decision:
- Changed surface: live CM_BUY_ITEM private-store purchase packet fanout and SmDeleteItem delete-type constant coverage.
- Specific behavior/contract: sold-out purchase sends seller SmDeleteItem with USE=0x17 and seller cube update; buyer new item add sends SmInventoryAddItem and buyer cube update.
- Focused C# command: dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~GamePacketTests" --no-restore -> 310/310 passed.
- Focused Java/Maven command: none; no narrow Java fixture exists, and behavior was taken from reviewed Java source.
- Broad-validation trigger: live packet output changed.
- Broad .NET decision: skipped after focused live packet and serializer coverage; the filter built Aion.GameServer and directly covered the modified live ProcessPacketAsync branch plus SmDeleteItem constant.
- Why this scope is sufficient: the passing tests dispatch encoded CM_BUY_ITEM and assert the Java-derived delete type and cube-update packets from the live path.
```

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `ItemPacketService.ItemDeleteType.USE` | `SmDeleteItem.UseDeleteType` | Packet constant | Complete | Unit Tested | Partial Parity | Constant mask matches Java; broader delete-type enum remains represented as needed. |
| `Storage.decreaseItemCount` delete packet branch | `TryExecutePrivateStorePurchaseAsync` seller deleted-item fanout | Packet/fanout | Partial | Unit Tested | Partial Parity | Private-store sold-out branch now sends USE delete type and cube update. |
| `Storage.add` cube-update follow-up | `TryExecutePrivateStorePurchaseAsync` buyer added-item fanout | Packet/fanout | Partial | Unit Tested | Partial Parity | Buyer new-item add now sends cube update; multi-add per-step cube count still needs coverage. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `SmDeleteItem_UseDeleteTypeMatchesJava` | Unit/packet | `ItemPacketService.ItemDeleteType.USE` | C# constant is Java mask `0x17` | Source-reviewed Java + constant assertion | Does not serialize a live packet. |
| `ProcessPacketAsync_CmBuyItemPlayerPrivateStoreExecutesSingleItemPurchaseAndClosesStore` | Unit/live handler | `PrivateStoreService.sellStoreItem` + `ItemPacketService` | Sold-out purchase sends seller USE delete packet, seller cube update, buyer add, buyer cube update, kinah packets, and close broadcast | Source-reviewed Java + live C# handler assertion | Does not cover multiple added items. |
| `ProcessPacketAsync_CmBuyItemPlayerPrivateStorePartialStackKeepsStoreOpenAndDecrementsPackCount` | Unit/live handler | `ItemService.addItem` + `Storage.add` | Partial-stack new buyer item add sends buyer cube update while seller store remains open | Source-reviewed Java + live C# handler assertion | Does not cover merge into existing buyer stack. |

## Summary Metrics

- Focused UOW validation: 310 tests passed.
- Runtime progress: private-store purchase packet fanout now includes Java delete type and cube updates for covered live branches.
- Total Java artifacts touched/discovered this UOW: 4.
- Total C# artifacts touched: 3.
- Artifacts with verified parity: 0.
- Artifacts needing verification / partial parity: 3.
- Blocked artifacts: persistence, exchange logging, full Java `PrivateStore` object model, remaining private-store purchase branches.
- Estimated overall Phase 6 completion: incremental; still far from replacement readiness.

## Remaining Risks

- The live branch still does not persist private-store purchase state.
- Exact packet order/counts for multi-item purchases and multiple new buyer item stacks remain untested.
- Real client behavior was not validated against a running Aion client.

## Next Runtime Candidate

UOW-2600 candidate: inspect and, if needed, fix live private-store multi-added-item cube update snapshots from
`CM_BUY_ITEM` action `0`.

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: a multi-item private-store purchase that creates multiple buyer cube rows should emit Java-equivalent add/cube packet pairs and cube counts after each add.
- Java source method or runtime path: ItemService.addItem -> Storage.add -> ItemPacketService.sendStorageUpdatePacket after each new inventory item.
- C# runtime artifact to wire or fix: GameServerConnection.TryExecutePrivateStorePurchaseAsync buyer added-item fanout, and any snapshotting needed around SmCubeUpdate.CubeSize.
- Client-visible/state effect expected: buyer receives correct SmInventoryAddItem and SmCubeUpdate packet order/counts for each added item from live CM_BUY_ITEM.
- Why this is not preview-only/test-only/documentation-only if feasible: it changes real packets emitted from the live purchase handler; if discovery proves current snapshots are equivalent for scoped inputs, select another runtime fix instead.
```
