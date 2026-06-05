# Phase 6 Session 2597 Completion

## UOW

[Phase 6] UOW-2597: Keep partial private-store purchases live

## Status

Completed and validated with focused live packet/state coverage. The C# private-store purchase plan now carries Java's
seller-side `packCount` decrement into the live partial-stack purchase path, and encoded `CM_BUY_ITEM` dispatch keeps the
seller store open while mutating seller/buyer state.

## Runtime Progress Gate

```text
Runtime progress gate:
- Deferred/live behavior being advanced: a private-store purchase that leaves seller inventory/store count remaining now updates live counts without closing the store and preserves Java's seller pack-count decrement.
- Java source method or runtime path: PrivateStoreService.sellStoreItem -> decreaseItemFromPlayer -> if (item.getPackCount() > 0) item.setPackCount(item.getPackCount() - 1).
- C# runtime artifact wired or fixed: PrivateStorePurchasePlanService seller remainder planning, GameServerConnection live purchase application, Player.PrivateStoreItems, seller/buyer InventoryItems.
- Client-visible/state effect changed: seller item count, seller item pack count, store listed count, buyer item/kinah, seller kinah, seller notification, and inventory packets update from live CM_BUY_ITEM while the shop remains open and no close broadcast is sent.
- Why this is not preview-only/test-only/documentation-only: the fix changes the plan consumed by live CM_BUY_ITEM execution and mutates runtime seller inventory state.
```

## Java Source Reviewed

- `PrivateStoreService.sellStoreItem` calls `decreaseItemFromPlayer`, then decrements `item.packCount` when it remains positive.
- `PrivateStoreService.decreaseItemFromPlayer` decreases seller inventory count, decreases the matching store item count, and removes the store item only when count reaches zero.
- `ItemService.addItem` uses `addStackableItem` for stackable items; that Java path creates/merges stackable items without copying source-item metadata such as `packCount`.

## C# Changes

- Updated `PrivateStorePurchasePlanService.CreatePlan` so seller partial-stack updates decrement `PackCount` when the seller source item has a positive pack count.
- Extended `PrivateStorePurchasePlanServiceTests.CreatePlan_BuysPartialStackableItemAndLeavesSellerRemainder` to assert seller pack-count decrement and Java-equivalent buyer stack pack count.
- Added `GameServerConnectionBuyItemTests.ProcessPacketAsync_CmBuyItemPlayerPrivateStorePartialStackKeepsStoreOpenAndDecrementsPackCount`.
- The live test dispatches an encoded `CM_BUY_ITEM`, verifies seller store remains open, seller inventory/store counts decrement, seller pack count decrements, buyer/seller packets send, and no close-private-shop broadcast is emitted.

## Known Gaps

- Inventory/kinah/store persistence is still not wired for private-store purchases.
- Multi-item purchase combinations, buyer inventory-full denial, insufficient-kinah denial, stale seller item counts, missing item templates, and missing seller inventory item behavior still need live handler coverage or fixes.
- Seller delete packet delete type remains unverified against Java `ItemPacketService` behavior.
- Java exchange-log/audit writes are not ported.
- Java's full `PrivateStore` object is still not ported.
- Real client validation was not run.

## Validation Decision

```text
Validation decision:
- Changed surface: live CM_BUY_ITEM private-store partial-stack purchase planning and execution.
- Specific behavior/contract: partial stack purchase decrements seller inventory count, seller pack count, and seller store count while keeping the store open and sending live buyer/seller packets.
- Focused C# command: dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~PrivateStorePurchasePlanServiceTests" --no-restore -> 30/30 passed.
- Focused Java/Maven command: none; no narrow Java fixture exists, and behavior was taken from reviewed Java source.
- Broad-validation trigger: live inventory/store state and packet output changed.
- Broad .NET decision: skipped after focused live packet/state coverage; the filter built Aion.GameServer and directly covered the modified plan service plus ProcessPacketAsync branch.
- Why this scope is sufficient: the passing tests cover the exact Java-derived pack-count branch at the plan level and through encoded live CM_BUY_ITEM dispatch.
```

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `PrivateStoreService.sellStoreItem` partial-stack purchase | `PrivateStorePurchasePlanService.CreatePlan` / `TryExecutePrivateStorePurchaseAsync` | State/packets | Partial | Unit Tested | Partial Parity | Partial-stack seller inventory/store updates now stay live and keep the shop open. |
| `PrivateStoreService.decreaseItemFromPlayer` | `UpdateSellerPrivateStoreItems` plus seller item update plan | State | Partial | Unit Tested | Partial Parity | Decrements seller item count, store item count, and positive pack count; full Java `PrivateStore` object remains unported. |
| `ItemService.addItem` stackable source item branch | `InventoryAddService.CreateAddItemPlan` via private-store plan | Service dependency | Partial | Unit Tested | Partial Parity | Java stackable path does not copy source pack count; C# test now preserves that behavior. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `CreatePlan_BuysPartialStackableItemAndLeavesSellerRemainder` | Unit | `PrivateStoreService.sellStoreItem` + `ItemService.addStackableItem` | Seller remainder decrements count and pack count; buyer stack pack count remains zero | Source-reviewed Java + C# plan assertion | Does not dispatch live packet. |
| `ProcessPacketAsync_CmBuyItemPlayerPrivateStorePartialStackKeepsStoreOpenAndDecrementsPackCount` | Unit/live handler | `CM_BUY_ITEM.runImpl` + `PrivateStoreService.sellStoreItem` | Encoded packet mutates seller/buyer live state, sends packets, keeps store open, and avoids close broadcast | Source-reviewed Java + live C# handler assertion | Does not cover multi-item or denial branches. |

## Summary Metrics

- Focused UOW validation: 30 tests passed.
- Runtime progress: partial private-store purchase now preserves Java seller pack-count mutation in live state.
- Total Java artifacts touched/discovered this UOW: 3.
- Total C# artifacts touched: 3.
- Artifacts with verified parity: 0.
- Artifacts needing verification / partial parity: 3.
- Blocked artifacts: persistence, exchange logging, full Java `PrivateStore` object model, remaining private-store purchase branches.
- Estimated overall Phase 6 completion: incremental; still far from replacement readiness.

## Remaining Risks

- The C# live path still depends on planner output for runtime mutation; uncovered planner branches can now affect live behavior.
- Seller pack-count persistence is absent in this purchase path.
- Real client behavior and packet byte-level output were not validated.

## Next Runtime Candidate

UOW-2598 candidate: inspect and, if needed, wire/fix the live private-store buyer-inventory-full denial from
`CM_BUY_ITEM` action `0`.

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: buyer inventory-full private-store purchase should send Java-equivalent STR_MSG_DICE_INVEN_ERROR from live CM_BUY_ITEM without mutating buyer/seller inventory, kinah, or store state.
- Java source method or runtime path: PrivateStoreService.sellStoreItem -> buyer.getInventory().getFreeSlots() < boughtItems.size() -> PacketSendUtility.sendPacket(buyer, SM_SYSTEM_MESSAGE.STR_MSG_DICE_INVEN_ERROR()) -> return.
- C# runtime artifact to wire or fix: GameServerConnection.TryExecutePrivateStorePurchaseAsync / PrivateStorePurchasePlanService buyer-message branch.
- Client-visible/state effect expected: live buyer receives SM_SYSTEM_MESSAGE dice inventory error and all purchase state remains unchanged.
- Why this is not preview-only/test-only/documentation-only if feasible: it must execute from live CM_BUY_ITEM and send a real server packet; if discovery shows the branch is already fully live, select a different runtime fix instead of doing a test-only UOW.
```
