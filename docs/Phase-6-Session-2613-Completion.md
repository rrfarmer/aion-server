# Phase 6 Session 2613 Completion

## UOW

[Phase 6] UOW-2613: Consume NPC shop required items live.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: successful normal NPC shop buys with required item costs no longer stop at the RequiredItems guard.
- Java source/runtime path: TradeService.performBuyTransaction -> tradeList.getRequiredItems() loop -> player.getInventory().decreaseByItemId(...) before ItemService.addItem.
- C# runtime artifact wired: GameServerConnection.TryExecuteBuyFromShopPurchaseAsync plus PlayerEnterWorldService/MySqlPlayerEnterWorldRepository NPC-shop buy persistence.
- Client-visible/state/persistence effect: action 13 normal shop success now consumes required item stacks, persists required-item updates/deletes with kinah and bought rows, sends DEC_ITEM_USE or SM_DELETE_ITEM/SM_CUBE_UPDATE before reward add packets, and mutates live player inventory.
- Why this is runtime progress: this UOW changes live client packet handling, player inventory state, persistence, and success packet fanout; it is not preview-only, test-only, or documentation-only.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/TradeService.java`
  - `performBuyTransaction`
  - required-items loop around `tradeList.getRequiredItems()`
- `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
  - `decreaseByItemId`
  - `decreaseItemCount`
- `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
  - `ItemUpdateType.DEC_ITEM_USE`
  - `ItemDeleteType.fromUpdateType`
  - `sendItemDeletePacket`
  - `sendItemUpdatePacket`

## C# Changes

- `GameServerConnection.TryExecuteBuyFromShopPurchaseAsync`
  - Removed the successful-buy guard that blocked non-empty `RequiredItems` while keeping AP-cost success disabled.
  - Added Java-shaped required item consumption against unequipped cube inventory.
  - Applies kinah first, required-item consumption next, then reward add planning.
  - Sends `SmInventoryUpdateItem.DecreaseItemUse` for reduced required-item stacks.
  - Sends `SmDeleteItem.UseDeleteType` plus `SmCubeUpdate` for required-item stacks that reach zero.
- `PlayerEnterWorldService.SaveNpcShopBuyMutationAsync`
  - Now forwards required-item updates and deleted required-item object IDs.
- `IPlayerEnterWorldRepository.SaveNpcShopBuyMutationAsync`
  - Extended the contract with required-item update/delete persistence.
- `MySqlPlayerEnterWorldRepository.SaveNpcShopBuyMutationAsync`
  - Saves kinah, required-item updates, required-item deletes, bought stack updates, and bought row inserts in one transaction.
- `EmptyPlayerEnterWorldRepository.NpcShopBuyPersistenceCapture`
  - Captures required-item update/delete persistence for live handler tests.

## Tests Added/Updated

| Test | What It Proves |
|---|---|
| `ProcessPacketAsync_CmBuyItemNpcBuyFromShopConsumesRequiredItemStackBeforeRewardAdd` | Live action 13 normal shop buy persists kinah, reduces a required-item stack, adds the reward item, mutates player inventory, and sends kinah/decrease/add/cube packets in Java order. |
| `ProcessPacketAsync_CmBuyItemNpcBuyFromShopDeletesRequiredItemStackAtZero` | Live action 13 normal shop buy persists an exact-stack required-item delete, removes the item from player inventory, and sends `SM_DELETE_ITEM(USE)` plus cube refresh before reward add packets. |
| Existing NPC shop buy persistence tests | Updated through the widened persistence API. |
| `PlayerEnterWorldServiceTests` fake repository | Updated through the widened repository interface. |

## Validation

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~TradeBuyTransactionPlanServiceTests|FullyQualifiedName~PlayerEnterWorldServiceTests" --no-restore
```

Result: passed, 114/114.

Java/Maven: not run. No narrow Java fixture was discovered for this exact runtime path; Java evidence came from the source methods listed above.

Broad .NET: skipped after focused live handler/planner/service coverage because the UOW only changed the NPC-shop buy runtime slice and repository interface implementations.

## Parity Status

| Java Artifact | C# Artifact | Status | Evidence | Remaining Gap |
|---|---|---|---|---|
| `TradeService.performBuyTransaction` required-items loop | `GameServerConnection.TryExecuteBuyFromShopPurchaseAsync` | Partial parity improved | Focused live handler tests cover required-item update/delete success paths. | Successful AP-cost buys remain disabled; limited counters and non-normal shop types remain incomplete. |
| `Storage.decreaseByItemId`/`decreaseItemCount` | NPC-shop required item consumption helper in `GameServerConnection` | Partial parity | Tests cover stack reduction and deletion packet shapes. | No shared storage service abstraction; only this live buy path is wired. |
| `ItemPacketService.sendItemPacket/sendItemDeletePacket` | `SmInventoryUpdateItem`, `SmDeleteItem`, `SmCubeUpdate` fanout | Partial parity | Packet assertions cover `DEC_ITEM_USE`, `USE` delete, and cube refresh ordering. | Real client validation was not run. |
| `InventoryDAO.store` dirty required-item rows | `MySqlPlayerEnterWorldRepository.SaveNpcShopBuyMutationAsync` | Needs DB verification | Compile-covered and fake-repository capture validates mutation contract. | No live MySQL integration test executed. |

## Known Gaps

- Successful AP-cost NPC shop buys remain blocked by the live success guard.
- Limited-item counter mutation/persistence is still not wired.
- Live NPC shop buy execution remains limited to `TradeNpcType.NORMAL`, action `13`.
- `ABYSS_KINAH`, `ABYSS`, and `REWARD` buy branches remain non-live.
- SQL execution for required-item update/delete persistence was not validated against a live database fixture.
- Real client validation was not run.
