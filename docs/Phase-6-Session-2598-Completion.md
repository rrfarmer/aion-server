# Phase 6 Session 2598 Completion

## UOW

[Phase 6] UOW-2598: Preserve missing private-store sale rows live

## Status

Completed and validated with focused live packet/state coverage. The live C# `CM_BUY_ITEM` private-store purchase path now
matches Java's missing seller inventory item branch: it skips item/store decrement for the missing item but still applies
the Java kinah transfer after the loop.

## Runtime Progress Gate

```text
Runtime progress gate:
- Deferred/live behavior being advanced: live private-store purchase with a missing seller inventory item keeps the store row intact while still applying Java's kinah transfer.
- Java source method or runtime path: PrivateStoreService.sellStoreItem -> if (seller.getInventory().getItemByObjId(...) != null) mutate item/store, then buyer.getInventory().decreaseKinah(price) and seller.getInventory().increaseKinah(price).
- C# runtime artifact wired or fixed: PrivateStorePurchasePlanService close decision and GameServerConnection.TryExecutePrivateStorePurchaseAsync store update input.
- Client-visible/state effect changed: buyer and seller kinah mutate from live CM_BUY_ITEM; skipped missing seller items do not decrement seller store rows, do not add buyer items, and do not close the store.
- Why this is not preview-only/test-only/documentation-only: the fix changes live packet execution state mutation and packet output from encoded CM_BUY_ITEM dispatch.
```

## Java Source Reviewed

- `PrivateStoreService.sellStoreItem` calculates total price before the seller item loop.
- Inside the loop, Java mutates seller inventory, private-store row count, buyer item, and seller notification only when `seller.getInventory().getItemByObjId(...)` returns an item.
- After the loop, Java always decreases buyer kinah and increases seller kinah for the calculated price.
- Java closes the store only when `seller.getStore().getSoldItems().isEmpty()`, so a missing seller inventory item that was not removed from the store keeps the shop open.

## C# Changes

- Updated `PrivateStorePurchasePlanService.CreatePlan` so skipped missing seller items count as remaining store items for the close-store decision.
- Updated live `GameServerConnection` private-store store-count mutation to subtract only bought items that Java actually applied, excluding `SkippedMissingSellerItems`.
- Extended `PrivateStorePurchasePlanServiceTests.CreatePlan_SkipsMissingSellerItemButKeepsJavaKinahTransferIntent` to cover a live-style no-remaining-after-count calculation and still keep the store open.
- Added `GameServerConnectionBuyItemTests.ProcessPacketAsync_CmBuyItemPlayerPrivateStoreMissingSellerInventoryKeepsStoreItemAndTransfersKinah`.

## Known Gaps

- Inventory/kinah/store persistence is still not wired for private-store purchases.
- Buyer inventory-full denial is already wired through buyer messages but still lacks live branch coverage.
- Multi-item purchase combinations, insufficient-kinah denial, stale seller item counts, missing item templates, and mixed present/missing seller item behavior still need live handler coverage or fixes.
- Seller delete packet delete type remains unverified against Java `ItemPacketService` behavior.
- Java exchange-log/audit writes are not ported.
- Java's full `PrivateStore` object is still not ported.
- Real client validation was not run.

## Validation Decision

```text
Validation decision:
- Changed surface: live CM_BUY_ITEM private-store purchase planning and store-state execution for skipped missing seller inventory items.
- Specific behavior/contract: missing seller inventory item leaves the private-store row unchanged, does not add buyer item, keeps the store open, and still transfers kinah from buyer to seller.
- Focused C# command: dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~PrivateStorePurchasePlanServiceTests" --no-restore -> 31/31 passed.
- Focused Java/Maven command: none; no narrow Java fixture exists, and behavior was taken from reviewed Java source.
- Broad-validation trigger: live inventory/store state and packet output changed.
- Broad .NET decision: skipped after focused live packet/state coverage; the filter built Aion.GameServer and directly covered the modified plan service plus ProcessPacketAsync branch.
- Why this scope is sufficient: the passing tests cover the Java-derived missing-item branch at the plan level and through encoded live CM_BUY_ITEM dispatch.
```

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `PrivateStoreService.sellStoreItem` missing seller item branch | `PrivateStorePurchasePlanService.CreatePlan` / `TryExecutePrivateStorePurchaseAsync` | State/packets | Partial | Unit Tested | Partial Parity | Skipped missing seller items now leave store rows intact while kinah transfers. |
| `PrivateStoreService.closePrivateStore` post-purchase guard | `PrivateStorePurchasePlan.ShouldCloseSellerStore` / `HandleClosePrivateStoreAsync` | State/fanout | Partial | Unit Tested | Partial Parity | Missing seller item branch no longer closes a store row Java would keep. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `CreatePlan_SkipsMissingSellerItemButKeepsJavaKinahTransferIntent` | Unit | `PrivateStoreService.sellStoreItem` | Skipped missing item keeps store open even when count-based remaining calculation is empty | Source-reviewed Java + C# plan assertion | Does not dispatch live packet. |
| `ProcessPacketAsync_CmBuyItemPlayerPrivateStoreMissingSellerInventoryKeepsStoreItemAndTransfersKinah` | Unit/live handler | `CM_BUY_ITEM.runImpl` + `PrivateStoreService.sellStoreItem` | Encoded packet transfers kinah, sends kinah packets, keeps missing store row, and avoids close broadcast | Source-reviewed Java + live C# handler assertion | Does not cover mixed present/missing purchase. |

## Summary Metrics

- Focused UOW validation: 31 tests passed.
- Runtime progress: missing seller inventory item branch now preserves Java store state from live `CM_BUY_ITEM`.
- Total Java artifacts touched/discovered this UOW: 2.
- Total C# artifacts touched: 4.
- Artifacts with verified parity: 0.
- Artifacts needing verification / partial parity: 2.
- Blocked artifacts: persistence, exchange logging, full Java `PrivateStore` object model, remaining private-store purchase branches.
- Estimated overall Phase 6 completion: incremental; still far from replacement readiness.

## Remaining Risks

- Java's kinah transfer after skipped items is unusual but intentional source-of-truth behavior; no real-client confirmation was run.
- Mixed present/missing purchase is covered at plan level but not through live packet dispatch.
- Persistence absence means the fixed live state can still be lost across restart.

## Candidate Inspected But Not Selected

### Buyer Inventory Full Denial

- Java source: `PrivateStoreService.sellStoreItem -> buyer.getInventory().getFreeSlots() < boughtItems.size() -> STR_MSG_DICE_INVEN_ERROR -> return`.
- C# source: `PrivateStorePurchasePlanService` creates `BlockedBuyerInventoryFull` with `SmSystemMessage.DiceInventoryError()`, and `TryExecutePrivateStorePurchaseAsync` sends `BuyerMessages` before returning on non-created status.
- No runtime code change was needed from discovery, so this was not selected as a test-only UOW.

## Next Runtime Candidate

UOW-2599 candidate: inspect and, if needed, correct the seller item delete packet type for sold-out private-store
purchases.

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: sold-out private-store purchase should send the same seller inventory delete packet type Java sends after seller inventory removal.
- Java source method or runtime path: PrivateStoreService.sellStoreItem -> decreaseItemFromPlayer -> seller.getInventory().decreaseItemCount -> ItemPacketService delete/update behavior.
- C# runtime artifact to wire or fix: GameServerConnection.TryExecutePrivateStorePurchaseAsync seller SmDeleteItem construction.
- Client-visible/state effect expected: seller receives Java-equivalent item delete packet for sold-out private-store item.
- Why this is not preview-only/test-only/documentation-only if feasible: it must change a real server packet emitted from live CM_BUY_ITEM; if Java review shows the current packet type is already equivalent, select another runtime fix instead.
```
