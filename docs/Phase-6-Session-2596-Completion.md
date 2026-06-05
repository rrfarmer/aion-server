# Phase 6 Session 2596 Completion

## UOW

[Phase 6] UOW-2596: Execute private-store purchase from live packet

## Status

Completed and validated with focused live packet/state coverage. The C# `CM_BUY_ITEM` player-target action `0` branch now
executes a valid private-store purchase from live packet dispatch instead of stopping at disabled purchase diagnostics.

## Runtime Progress Gate

```text
Runtime progress gate:
- Deferred/live behavior being advanced: CM_BUY_ITEM action 0 against a player private store executes a valid purchase instead of only building disabled purchase/outcome plans.
- Java source method or runtime path: CM_BUY_ITEM.runImpl -> PrivateStoreService.sellStoreItem -> getBoughtItems -> decreaseItemFromPlayer -> ItemService.addItem -> kinah transfer -> seller notifications -> closePrivateStore.
- C# runtime artifact wired or fixed: GameServerConnection CmBuyItem branch, Player.PrivateStoreItems, buyer/seller InventoryItems/kinah rows, SmInventoryAddItem/SmInventoryUpdateItem/SmDeleteItem/SmSystemMessage, and private-store close fanout.
- Client-visible/state effect changed: a valid single-item purchase transfers the item to the buyer, transfers kinah to the seller, deletes the sold seller item, sends buyer/seller inventory packets and seller notification, and closes the seller store when sold out.
- Why this is not preview-only/test-only/documentation-only: it runs from encoded live client packet dispatch, mutates live buyer/seller state, and emits real server packets/broadcasts.
```

## Java Source Reviewed

- `CM_BUY_ITEM.runImpl` reads action `0`, resolves the target player, and calls `PrivateStoreService.sellStoreItem(targetPlayer, player, tradeList)`.
- `PrivateStoreService.sellStoreItem` validates buyer/seller state, race, inventory space, total price, buyer kinah, seller item counts, then mutates seller items, buyer items, and kinah.
- `PrivateStoreService.getBoughtItems` maps client private-store indexes to store items.
- `PrivateStoreService.decreaseItemFromPlayer` removes or decrements the seller inventory item and decrements packed-store counts when applicable.
- `PrivateStoreService.closePrivateStore` is called when the seller store becomes empty.

## C# Changes

- Changed `CmBuyItem` dispatch to await `HandleBuyItemAsync` so live packet handling can send private-store purchase packets.
- Added a live private-store purchase gate for player-target action `0` while preserving existing diagnostic observers.
- Added `TryExecutePrivateStorePurchaseAsync` to apply `PrivateStorePurchasePlan` output to live buyer/seller state.
- Mutated seller inventory deletes/updates, buyer item adds/updates, and buyer/seller kinah rows from the live handler.
- Updated seller `PrivateStoreItems` for non-closing purchases and reused `HandleClosePrivateStoreAsync` for sold-out purchases.
- Sent buyer inventory packets, seller inventory packets, seller private-store notification messages, and close-private-shop emotion fanout from live code.
- Extended `GameServerConnectionBuyItemTests` with registry packet capture and a live sold-out purchase assertion.

## Known Gaps

- Inventory/kinah/store persistence is not wired; this UOW mutates in-memory runtime state only.
- Only the single-item sold-out happy path is live-tested.
- Multi-item purchases, partial-stack purchases that leave the store open, inventory-full denial, insufficient-kinah denial, stale seller item counts, missing item templates, and packed-item decrement still need live handler coverage.
- Seller delete packet delete type was not asserted against Java `ItemPacketService` behavior.
- Java exchange-log/audit writes are not ported.
- C# still uses direct `Player.PrivateStoreItems`/`Player.PrivateStoreMessage` fields instead of Java's full `PrivateStore` object.
- Real client validation was not run.

## Validation Decision

```text
Validation decision:
- Changed surface: live CM_BUY_ITEM packet handling for player-target action 0 private-store purchases.
- Specific behavior/contract: a valid one-item purchase transfers item and kinah state, sends buyer/seller inventory packets, sends seller notification, and closes the seller private store when sold out.
- Focused C# command: dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~PrivateStoreBoughtItemsPlanServiceTests|FullyQualifiedName~PrivateStorePurchasePlanServiceTests|FullyQualifiedName~PrivateStoreSellNotificationPlanServiceTests" --no-restore -> 43/43 passed.
- Focused Java/Maven command: none; no narrow Java fixture exists, and behavior was taken from reviewed Java source.
- Broad-validation trigger: live player inventory/kinah/store state and packet output changed.
- Broad .NET decision: skipped after focused live packet/state coverage; the filter built Aion.GameServer and directly covered the modified ProcessPacketAsync branch plus purchase-plan dependencies.
- Why this scope is sufficient: the passing test dispatches an encoded CM_BUY_ITEM packet and asserts live buyer/seller state mutation, direct seller packet fanout, buyer packet output, and close-store broadcast.
```

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CM_BUY_ITEM.runImpl` player action `0` | `HandleBuyItemAsync` / `TryExecutePrivateStorePurchaseAsync` | Live handler | Partial | Unit Tested | Partial Parity | Valid sold-out private-store purchase now executes live. |
| `PrivateStoreService.sellStoreItem` valid purchase | `PrivateStorePurchasePlanService` applied by live handler | State/packets | Partial | Unit Tested | Partial Parity | Covers item/kinah transfer, seller notification, and close-store branch. |
| `PrivateStoreService.getBoughtItems` | `PrivateStoreBoughtItemsPlanService` from live handler | Runtime mapping | Partial | Unit Tested | Partial Parity | Existing planner now feeds a live execution path for valid row indexes. |
| `PrivateStoreService.closePrivateStore` sold-out branch | Existing `HandleClosePrivateStoreAsync` reused after purchase | Live state/fanout | Partial | Unit Tested | Partial Parity | Sold-out purchase clears store state and broadcasts close-private-shop emotion. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_CmBuyItemPlayerPrivateStoreExecutesSingleItemPurchaseAndClosesStore` | Unit/live handler | `CM_BUY_ITEM.runImpl` + `PrivateStoreService.sellStoreItem` | Encoded buy packet mutates buyer/seller inventory and kinah, sends live packets, and closes sold-out store | Source-reviewed Java + live C# handler assertions | Does not cover partial-stack/non-closing store updates or rejection branches. |

## Summary Metrics

- Focused UOW validation: 43 tests passed.
- Runtime progress: private-store purchase now has one live item/kinah transfer branch from `CM_BUY_ITEM`.
- Total Java artifacts touched/discovered this UOW: 5.
- Total C# artifacts touched: 4.
- Artifacts with verified parity: 0.
- Artifacts needing verification / partial parity: 4.
- Blocked artifacts: persistence, exchange logging, full Java `PrivateStore` object model, remaining private-store purchase branches.
- Estimated overall Phase 6 completion: incremental; still far from replacement readiness.

## Remaining Risks

- The live branch depends on existing purchase-plan services, so gaps in those services can now affect real runtime behavior.
- Seller direct packets require a connection registry when the seller is not the active connection; the test covers this with a capturing registry.
- Persistence absence means purchase effects can be lost across restart or reload.

## Next Runtime Candidate

UOW-2597 candidate: port the private-store partial-stack/non-closing purchase branch from `CM_BUY_ITEM` action `0`.

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: a private-store purchase that leaves seller inventory/store count remaining should update live counts without closing the store.
- Java source method or runtime path: PrivateStoreService.sellStoreItem -> decreaseItemFromPlayer with remaining item count and store packCount decrement.
- C# runtime artifact to wire or fix: TryExecutePrivateStorePurchaseAsync / UpdateSellerPrivateStoreItems / buyer-seller inventory and kinah updates.
- Client-visible/state effect expected: seller item count and PrivateStoreItems count decrease, buyer receives item and kinah update, seller receives item/kinah/notification packets, and no close-private-shop broadcast is sent.
- Why this is not preview-only/test-only/documentation-only if feasible: it must execute from live CM_BUY_ITEM and mutate live store/inventory/kinah state while keeping the store open.
```
