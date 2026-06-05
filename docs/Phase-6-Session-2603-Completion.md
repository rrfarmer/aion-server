# Phase 6 Session 2603 Completion

## UOW

[Phase 6] UOW-2603: Interleave private-store buyer item fanout live

## Status

Completed and validated with focused live packet coverage. Private-store buyer item add/update packets now fan out from
the live `CM_BUY_ITEM` handler before the corresponding seller sale message for applied bought items, matching the Java
loop point for covered sold-out and multi-row purchases.

## Runtime Progress Gate

```text
Runtime progress gate:
- Deferred/live behavior being advanced: buyer item add/update packets are emitted before each seller sale message from the same bought-item loop point Java uses.
- Java source method or runtime path: PrivateStoreService.sellStoreItem -> decreaseItemFromPlayer -> ItemService.addItem(buyer, item, count) -> PacketSendUtility.sendPacket(seller, STR_MSG_PERSONAL_SHOP_SELL_ITEM[_MULTI]) -> buyer.decreaseKinah -> seller.increaseKinah.
- C# runtime artifact wired or fixed: PrivateStorePurchasePlanService per-bought-item buyer packet grouping and GameServerConnection.TryExecutePrivateStorePurchaseAsync buyer/seller fanout order.
- Client-visible/state effect changed: live CM_BUY_ITEM emits seller item packet(s), buyer item add/update packet(s), then seller sale message for each applied bought item, while final buyer/seller kinah packets remain after item/message fanout.
- Why this is not preview-only/test-only/documentation-only: it changes real server packets emitted by live CM_BUY_ITEM; the new metadata exists only to drive that live fanout.
```

## Java Source Reviewed

- `PrivateStoreService.sellStoreItem` decreases the seller item, adds the item to the buyer, then sends the seller sale
  system message inside the per-bought-item loop.
- `ItemService.addItem` delegates to inventory/storage add/update behavior before `sellStoreItem` sends the sale
  message.
- Buyer and seller kinah mutation happens after the bought-item loop.

## C# Changes

- Added `PrivateStorePurchaseBuyerItemFanout` so private-store purchase plans retain buyer added/updated item packets by
  bought item.
- Changed `GameServerConnection.TryExecutePrivateStorePurchaseAsync` to consume per-bought-item buyer fanout between the
  seller item delete/update packet and the seller sale message.
- Preserved the existing aggregate buyer item fallback for older manually constructed plans without fanout groups.
- Kept buyer kinah, seller kinah, and seller close fanout after all item/message packets.
- Updated live handler tests to assert the combined active/direct/visible packet timeline.

## Known Gaps

- Inventory/kinah/store persistence is still not wired for private-store purchases.
- Java exchange-log/audit writes remain unported.
- Full Java `PrivateStore` object behavior remains unported.
- Buyer existing-stack update fanout is supported by the grouped runtime path but does not yet have a focused live
  timeline test with an existing stack branch.
- Complex add plans that produce multiple buyer packet rows per bought item are supported by grouping but only covered
  through one added item per bought row in the focused live tests.
- Real client validation was not run.

## Validation Decision

```text
Validation decision:
- Changed surface: live CM_BUY_ITEM private-store packet fanout plus runtime plan metadata used by that fanout.
- Specific behavior/contract: buyer item add/update packets are emitted before the corresponding seller sale message for applied bought items; buyer/seller kinah packets remain after item/message fanout.
- Focused C# command: dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~GamePacketTests" --no-restore -> 311/311 passed.
- Focused Java/Maven command: none; no narrow Java fixture exists, and behavior was taken from reviewed Java source.
- Broad-validation trigger: live packet output changed.
- Broad .NET decision: skipped after focused live handler and packet serializer coverage; the filter built Aion.GameServer and directly covered the modified ProcessPacketAsync branch.
- Why this scope is sufficient: the passing live tests dispatch encoded CM_BUY_ITEM through the handler and assert the Java-derived cross-recipient packet timeline for covered branches.
```

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.PrivateStoreService#sellStoreItem` | `Aion.GameServer.Network.Aion.GameServerConnection.TryExecutePrivateStorePurchaseAsync` | Live handler | Partial | Unit Tested | Partial Parity | Covered item/message/kinah ordering now follows the Java loop for sold-out and multi-row purchases. |
| `com.aionemu.gameserver.services.item.ItemService#addItem` | `Aion.GameServer.Services.PrivateStorePurchaseBuyerItemFanout` plus live handler fanout | Runtime inventory fanout | Partial | Unit Tested | Partial Parity | Per-bought-item buyer add/update groups drive live packets before seller messages; persistence and full storage semantics remain incomplete. |
| `com.aionemu.gameserver.services.item.ItemPacketService#sendItemPacket` / `sendStorageUpdatePacket` | `SmInventoryAddItem`, `SmInventoryUpdateItem`, `SmCubeUpdate` from `GameServerConnection` | Packet/fanout | Partial | Unit Tested | Partial Parity | Covered buyer add/cube packets are interleaved before seller sale messages. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_CmBuyItemPlayerPrivateStoreExecutesSingleItemPurchaseAndClosesStore` | Unit/live handler | `PrivateStoreService.sellStoreItem` | Sold-out purchase sends seller delete/cube, buyer add/cube, seller sale message, then kinah packets | Java source review + combined packet event assertion | Does not cover buyer existing-stack update. |
| `ProcessPacketAsync_CmBuyItemPlayerPrivateStoreMultiAddUsesPerItemCubeSnapshots` | Unit/live handler | `PrivateStoreService.sellStoreItem` | Multi-row purchases repeat seller item, buyer item, seller message order per bought item | Java source review + combined packet event assertion | Covers one buyer added item per bought row. |

## Summary Metrics

- Focused UOW validation: 311 tests passed.
- Runtime progress: live private-store buyer item packets now precede corresponding seller sale messages.
- Total Java artifacts touched/discovered this UOW: 4.
- Total C# artifacts touched: 3.
- Artifacts with verified parity: 0.
- Artifacts needing verification / partial parity: 3.
- Blocked artifacts: persistence, exchange logging, full Java `PrivateStore` object, real client verification.
- Estimated overall Phase 6 completion: incremental; still far from replacement readiness.

## Remaining Risks

- Private-store purchase persistence and transaction boundaries remain disabled.
- Existing-stack buyer update ordering still needs a runtime branch if discovery finds a live mismatch beyond the grouped
  fanout support added here.
- Real client behavior was not validated against a running Aion client.

## Next Runtime Candidate

UOW-2604 candidate: wire private-store purchase persistence for live inventory/kinah mutations using existing C# database
shape, if discovery finds a safe repository/service surface.

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: live private-store purchase inventory, kinah, and store mutations should be persisted/restored instead of remaining memory-only.
- Java source method or runtime path: PrivateStoreService.sellStoreItem inventory/kinah mutations through Storage/Inventory persistence paths.
- C# runtime artifact to wire or fix: GameServerConnection.TryExecutePrivateStorePurchaseAsync and the existing inventory/character persistence repository or service discovered in dotnetConversion.
- Client-visible/state/persistence effect expected: bought/sold item and kinah changes survive the existing save/reload path or write through the existing database rows used by runtime inventory.
- Why this is not preview-only/test-only/documentation-only if feasible: it writes live handler mutations through the existing runtime persistence path.
```
