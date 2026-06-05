# Phase 6 Session 2596 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2596: Execute private-store purchase from live packet. See
[Phase-6-Session-2596-Completion.md](Phase-6-Session-2596-Completion.md).

## Commits Made

- `ca69019` - `[Phase 6][UOW-2578] Persist abandoned quest state`
- `559431a` - `[Phase 6][UOW-2579] Persist NPC faction abort state`
- `5aa6cdf` - `[Phase 6][UOW-2580] Re-send assigned NPC faction daily quest`
- `d5a7adf` - `[Phase 6][UOW-2581] Assign NPC faction daily quest`
- `d84e7b53b` - `[Phase 6][UOW-2582] Filter NPC faction daily handlers`
- `ed8eabf35` - `[Phase 6][UOW-2583] Persist quest work item deletes`
- `a767ee5cb` - `[Phase 6][UOW-2584] Wire quest-start item use`
- `5f98483` - `[Phase 6][UOW-2585] Send quest-start rejection messages`
- `84c0f0b` - `[Phase 6][UOW-2586] Send quest-start condition messages`
- `d92efac` - `[Phase 6][UOW-2587] Reject quest-start items at normal quest cap`
- `7c7c3e3` - `[Phase 6][UOW-2588] Send quest-start inventory-item warning`
- `cd9ecd7` - `[Phase 6][UOW-2589] Send quest-start combine-skill warning`
- `0f1a440` - `[Phase 6][UOW-2590] Send quest-start rank warning`
- `b9af493` - `[Phase 6][UOW-2591] Wire keyless static-door open`
- `e2537af` - `[Phase 6][UOW-2592] Open keyed static doors`
- `0b9c1ce` - `[Phase 6][UOW-2593] Close private store from live packet`
- `a5b58e2` - `[Phase 6][UOW-2594] Open private store from live packet`
- `8a487a0` - `[Phase 6][UOW-2595] Set private-store name from live packet`
- Current commit - `[Phase 6][UOW-2596] Execute private-store purchase from live packet`

## Session Summary

- UOW-2593 made zero-item `CM_PRIVATE_STORE` close live.
- UOW-2594 made non-empty `CM_PRIVATE_STORE` open live.
- UOW-2595 made `CM_PRIVATE_STORE_NAME` message mutation and `SM_PRIVATE_STORE_NAME` output live.
- UOW-2596 made one valid `CM_BUY_ITEM` private-store purchase branch live.
- Private-store buy now mutates buyer/seller inventory and kinah state, sends buyer/seller packets, and closes the seller store when sold out for the covered branch.

## Files Changed In UOW-2596

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionBuyItemTests.cs`
- `docs/Phase-6-Session-2596-Completion.md`
- `docs/Phase-6-Session-2596-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_BUY_ITEM#runImpl`
- `com.aionemu.gameserver.services.PrivateStoreService#sellStoreItem`
- `com.aionemu.gameserver.services.PrivateStoreService#getBoughtItems`
- `com.aionemu.gameserver.services.PrivateStoreService#decreaseItemFromPlayer`
- `com.aionemu.gameserver.services.PrivateStoreService#closePrivateStore`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection` `CmBuyItem` dispatch
- `Aion.GameServer.Network.Aion.GameServerConnection.HandleBuyItemAsync`
- `Aion.GameServer.Network.Aion.GameServerConnection.TryExecutePrivateStorePurchaseAsync`
- `Aion.GameServer.Model.GameObjects.Player.PrivateStoreItems`
- `Aion.GameServer.Model.GameObjects.Player.InventoryItems`
- `Aion.GameServer.Tests.GameServerConnectionBuyItemTests`

## Validation

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~PrivateStoreBoughtItemsPlanServiceTests|FullyQualifiedName~PrivateStorePurchasePlanServiceTests|FullyQualifiedName~PrivateStoreSellNotificationPlanServiceTests" --no-restore
```

Result: passed, 43/43.

Java/Maven: not run. No narrow Java fixture exists; behavior was verified by source review against Java methods listed
above.

Broad .NET: skipped after focused live packet/state coverage. The filter built `Aion.GameServer` and directly covered the
modified live packet branch plus adjacent private-store purchase plan services.

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

## Known Gaps

- Inventory/kinah/store persistence is not wired; purchase effects are in-memory only.
- Only the single-item sold-out happy path is live-tested.
- Multi-item purchases, partial-stack purchases that leave the store open, inventory-full denial, insufficient-kinah denial, stale seller item counts, missing item templates, and packed-item decrement still need live handler coverage.
- Seller delete packet delete type was not asserted against Java `ItemPacketService` behavior.
- Java exchange-log/audit writes are not ported.
- Java's full `PrivateStore` object is still not ported.
- Static-door `onOpenDoor` instance callback remains unported because no concrete live C# instance-handler execution surface was found.
- Full `CM_GROUP_LOOT` roll/bid remains blocked by missing drop-distribution runtime state.
- Real client validation was not run.

## Candidate Checks Rejected This Session

### Static-Door `onOpenDoor`

- Java source: `StaticDoorService.openStaticDoor -> WorldMapInstance.getInstanceHandler().onOpenDoor(doorId)`.
- C# has lifecycle hooks for instance create/destroy/leave but no concrete `OnOpenDoor` handler surface found.
- Adding only an interface/callback would be callback-surface scaffolding, not runtime progress.

### `CM_GROUP_LOOT`

- Java source: `CM_GROUP_LOOT.runImpl -> DropDistributionService.handleRollOrBid`.
- Still blocked by missing C# distribution fields: current index, max roll, looting team id, distribution id, in-range players, per-player status, winner, and loot group rules.
- Parser-to-packet wiring would be misleading without the live distribution state.

### Additional Private-Store Purchase Planner Work

- Existing private-store purchase plan services were useful only because UOW-2596 applied them from live packet dispatch.
- More planner-only or evidence-only private-store work should be ignored unless it immediately mutates live state, sends live packets, persists state, or unblocks a live handler.

## Next Recommended Runtime UOW

**UOW-2597 candidate: port the private-store partial-stack/non-closing purchase branch from `CM_BUY_ITEM` action `0`.**

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: a private-store purchase that leaves seller inventory/store count remaining should update live counts without closing the store.
- Java source method or runtime path: PrivateStoreService.sellStoreItem -> decreaseItemFromPlayer with remaining item count and store packCount decrement.
- C# runtime artifact to wire or fix: TryExecutePrivateStorePurchaseAsync / UpdateSellerPrivateStoreItems / buyer-seller inventory and kinah updates.
- Client-visible/state effect expected: seller item count and PrivateStoreItems count decrease, buyer receives item and kinah update, seller receives item/kinah/notification packets, and no close-private-shop broadcast is sent.
- Why this is not preview-only/test-only/documentation-only if feasible: it must execute from live CM_BUY_ITEM and mutate live store/inventory/kinah state while keeping the store open.
```

## Safe Runtime Candidates

- Private-store partial-stack/non-closing purchase from live `CM_BUY_ITEM`.
- Private-store buyer inventory-full denial from live `CM_BUY_ITEM` if it sends Java-equivalent `STR_MSG_DICE_INVEN_ERROR`.
- Private-store insufficient-kinah denial from live `CM_BUY_ITEM` if it sends Java-equivalent buyer rejection.
- Another deferred `GameServerConnection` packet path with existing runtime state/repository/service surfaces.
- Java XML/static-data loading only when the data is immediately used by live code.

## Context Needed By Next Session

- `CM_PRIVATE_STORE` zero-item close is live as of UOW-2593.
- `CM_PRIVATE_STORE` non-empty open is live as of UOW-2594.
- `CM_PRIVATE_STORE_NAME` is live as of UOW-2595.
- `CM_BUY_ITEM` player action `0` sold-out single-item purchase is live as of UOW-2596.
- Live private-store C# state currently consists of `Player.PrivateStoreItems`, `Player.PrivateStoreMessage`, and `PlayerCreatureState.PrivateShop`.
- Existing disabled private-store planner services are only parity evidence when applied by a live handler.
- Avoid preview/metadata/evidence-only work. Each next UOW must mutate live state, send real packets, persist/restore live state, load runtime-used Java data, or execute a live handler path.
