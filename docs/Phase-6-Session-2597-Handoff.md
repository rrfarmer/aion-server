# Phase 6 Session 2597 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2597: Keep partial private-store purchases live. See
[Phase-6-Session-2597-Completion.md](Phase-6-Session-2597-Completion.md).

## Commits Made

- `b9af493` - `[Phase 6][UOW-2591] Wire keyless static-door open`
- `e2537af` - `[Phase 6][UOW-2592] Open keyed static doors`
- `0b9c1ce` - `[Phase 6][UOW-2593] Close private store from live packet`
- `a5b58e2` - `[Phase 6][UOW-2594] Open private store from live packet`
- `8a487a0` - `[Phase 6][UOW-2595] Set private-store name from live packet`
- `03ea474` - `[Phase 6][UOW-2596] Execute private-store purchase from live packet`
- Current commit - `[Phase 6][UOW-2597] Keep partial private-store purchases live`

## Session Summary

- UOW-2596 made one valid `CM_BUY_ITEM` private-store purchase branch live for sold-out single-item purchases.
- UOW-2597 fixed Java parity for partial-stack purchases that leave the seller store open.
- Seller item count, seller store count, seller pack count, buyer item/kinah, seller kinah, seller notification, and packets now update from live `CM_BUY_ITEM` for the covered partial-stack branch.
- Java stackable buyer add behavior was reviewed; the buyer's new stack does not copy source `packCount`, matching Java `ItemService.addStackableItem`.

## Files Changed In UOW-2597

- `dotnetConversion/src/Aion.GameServer/Services/PrivateStorePurchasePlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PrivateStorePurchasePlanServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionBuyItemTests.cs`
- `docs/Phase-6-Session-2597-Completion.md`
- `docs/Phase-6-Session-2597-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.services.PrivateStoreService#sellStoreItem`
- `com.aionemu.gameserver.services.PrivateStoreService#decreaseItemFromPlayer`
- `com.aionemu.gameserver.services.item.ItemService#addStackableItem`

## C# Artifacts Touched

- `Aion.GameServer.Services.PrivateStorePurchasePlanService`
- `Aion.GameServer.Network.Aion.GameServerConnection` live `CM_BUY_ITEM` path via existing executor
- `Aion.GameServer.Tests.PrivateStorePurchasePlanServiceTests`
- `Aion.GameServer.Tests.GameServerConnectionBuyItemTests`

## Validation

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~PrivateStorePurchasePlanServiceTests" --no-restore
```

Result: passed, 30/30.

Java/Maven: not run. No narrow Java fixture exists; behavior was verified by source review against Java methods listed
above.

Broad .NET: skipped after focused live packet/state coverage. The filter built `Aion.GameServer` and directly covered the
modified plan service plus live `ProcessPacketAsync` branch.

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

## Known Gaps

- Inventory/kinah/store persistence is still not wired for private-store purchases.
- Multi-item purchase combinations, buyer inventory-full denial, insufficient-kinah denial, stale seller item counts, missing item templates, and missing seller inventory item behavior still need live handler coverage or fixes.
- Seller delete packet delete type remains unverified against Java `ItemPacketService` behavior.
- Java exchange-log/audit writes are not ported.
- Java's full `PrivateStore` object is still not ported.
- Static-door `onOpenDoor` instance callback remains unported because no concrete live C# instance-handler execution surface was found.
- Full `CM_GROUP_LOOT` roll/bid remains blocked by missing drop-distribution runtime state.
- Real client validation was not run.

## Candidate Checks Rejected This Session

### Buyer Stack Pack Count Copy

- Java source: `ItemService.addItem(player, sourceItem, count)` routes stackable items through `addStackableItem`, not `copyItemInfo`.
- `addStackableItem` creates or merges stackable items without copying source metadata.
- C# buyer stack `PackCount = 0` was kept as Java-equivalent behavior.

### Additional Private-Store Planner Work

- Planner behavior changed only where it feeds the live `CM_BUY_ITEM` execution path.
- More planner-only assertions should be rejected unless paired with a live runtime mutation/send/persistence fix.

## Next Recommended Runtime UOW

**UOW-2598 candidate: inspect and, if needed, wire/fix the live private-store buyer-inventory-full denial from
`CM_BUY_ITEM` action `0`.**

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: buyer inventory-full private-store purchase should send Java-equivalent STR_MSG_DICE_INVEN_ERROR from live CM_BUY_ITEM without mutating buyer/seller inventory, kinah, or store state.
- Java source method or runtime path: PrivateStoreService.sellStoreItem -> buyer.getInventory().getFreeSlots() < boughtItems.size() -> PacketSendUtility.sendPacket(buyer, SM_SYSTEM_MESSAGE.STR_MSG_DICE_INVEN_ERROR()) -> return.
- C# runtime artifact to wire or fix: GameServerConnection.TryExecutePrivateStorePurchaseAsync / PrivateStorePurchasePlanService buyer-message branch.
- Client-visible/state effect expected: live buyer receives SM_SYSTEM_MESSAGE dice inventory error and all purchase state remains unchanged.
- Why this is not preview-only/test-only/documentation-only if feasible: it must execute from live CM_BUY_ITEM and send a real server packet; if discovery shows the branch is already fully live, select a different runtime fix instead of doing a test-only UOW.
```

Suggested focused validation if this UOW includes a runtime fix:

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~PrivateStorePurchasePlanServiceTests" --no-restore
```

Behavior to prove: live encoded `CM_BUY_ITEM` sends the buyer inventory-full system message and leaves buyer/seller
inventory, kinah, and private-store state unchanged.

Java/Maven: not expected unless a narrow Java fixture is discovered. Broad-validation trigger: live packet output/state
guard changes if a runtime fix is required; start focused and document any broad skip.

## Safe Runtime Candidates

- Private-store buyer inventory-full denial from live `CM_BUY_ITEM` if discovery finds a runtime gap.
- Private-store missing seller item / mixed present-missing behavior if it requires a live state fix rather than planner-only coverage.
- Private-store seller delete packet type correction if Java `ItemPacketService` review identifies a packet-visible mismatch.
- Another deferred `GameServerConnection` packet path with existing runtime state/repository/service surfaces.
- Java XML/static-data loading only when the data is immediately used by live code.

## Context Needed By Next Session

- `CM_PRIVATE_STORE` zero-item close is live as of UOW-2593.
- `CM_PRIVATE_STORE` non-empty open is live as of UOW-2594.
- `CM_PRIVATE_STORE_NAME` is live as of UOW-2595.
- `CM_BUY_ITEM` player action `0` sold-out single-item purchase is live as of UOW-2596.
- `CM_BUY_ITEM` player action `0` partial-stack/non-closing purchase is live as of UOW-2597, including seller pack-count decrement.
- Live private-store C# state currently consists of `Player.PrivateStoreItems`, `Player.PrivateStoreMessage`, and `PlayerCreatureState.PrivateShop`.
- Existing disabled private-store planner services are only parity evidence when applied by a live handler.
- Avoid preview/metadata/evidence-only work. Each next UOW must mutate live state, send real packets, persist/restore live state, load runtime-used Java data, or execute a live handler path.
