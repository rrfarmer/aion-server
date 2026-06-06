# Phase 6 Session 2705 Completion

## UOW

[Phase 6] UOW-2705: Move account warehouse items with owner parity.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: live CM_MOVE_ITEM can now move explicit-slot items between cube and account warehouse when the account warehouse item was restored into Player.AccountWarehouseItems.
- Java source/runtime path: Player.getStorage(StorageType.ACCOUNT_WAREHOUSE), ItemMoveService.moveItem cross-storage remove/add, Storage.add item.setItemLocation, and InventoryDAO.store/updateItems getItemOwnerId.
- C# runtime artifact wired: GameServerConnection.HandleMoveItemAsync account-storage lookup/list mutation plus PlayerEnterWorldService/MySqlPlayerEnterWorldRepository SaveItemCrossStorageMoveMutationAsync owner-aware persistence.
- Client-visible/state/persistence effect: cube <-> account warehouse moves send source delete, destination add, and storage-size packets; runtime items move between InventoryItems and AccountWarehouseItems; inventory.item_owner changes playerId <-> accountId with item_location and slot.
- Why this is runtime progress: this changes live packet handling, runtime inventory/account-warehouse state, and database persistence. It is not preview-only/test-only/documentation-only.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/Player.java`
  - `getStorage(ACCOUNT_WAREHOUSE)` returns `playerAccount.getAccountWarehouse()`.
- `game-server/src/com/aionemu/gameserver/services/item/ItemMoveService.java`
  - `moveItem` removes the item from the source storage, adds it to the destination storage, and persists through `InventoryDAO.store(item, player)`.
- `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
  - `add` sets `item.setItemLocation(storageType.getId())` and sends the storage update packet.
- `game-server/src/com/aionemu/gameserver/dao/InventoryDAO.java`
  - `getItemOwnerId` uses account id for `ACCOUNT_WAREHOUSE` rows and player id for player storages.
  - `UPDATE_QUERY` rewrites `item_owner`, `item_location`, and `slot`.

## C# Changes

- `HandleMoveItemAsync` now finds storage id `2` items in `Player.AccountWarehouseItems`.
- Cross-storage moves now move the runtime item between `InventoryItems` and `AccountWarehouseItems` for account warehouse boundaries.
- `InventoryItem.OwnerId` is mutable so live storage moves can mirror Java row owner transitions.
- `SaveItemCrossStorageMoveMutationAsync` now receives account id and old/new locations, then updates `item_owner`, `item_location`, and `slot` using Java's owner rule.
- Account warehouse stack auto-merge remains intentionally unwired in this UOW because merge persistence is still player-owner based.

## Tests Added / Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleMoveItemAsync_AccountWarehouseSourceMovesRestoredItemToCubeLikeJava` | Unit / live connection handler | Java `Player.getStorage`, `ItemMoveService.moveItem`, `InventoryDAO.getItemOwnerId` source review | Restored account warehouse item moves from `AccountWarehouseItems` to cube, owner becomes player id, persistence records account id and old/new storage, and source delete/destination add packets are sent. | Socket-backed handler fixture, decoded server packet types/payloads, runtime list assertions, repository persistence capture. | Does not cover stack auto-merge, split, replace, account warehouse capacity, or same-storage account slot reorder. |
| `HandleMoveItemAsync_CubeSourceMovesItemToAccountWarehouseOwnerLikeJava` | Unit / live connection handler | Same Java move and owner-store path | Cube item moves into `AccountWarehouseItems`, owner becomes account id, persistence records player/account owner transition, and account warehouse add packets are sent. | Socket-backed handler fixture, decoded packets, runtime list assertions, repository persistence capture. | Same gaps as above. |

## Validation Decision

```text
- Changed surface: live CM_MOVE_ITEM account warehouse runtime state and cross-storage persistence.
- Specific behavior/contract: Java stores account warehouse rows under account id and player storage rows under player id while moving the runtime item between storages and sending delete/add packets.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleMoveItemAsync_AccountWarehouseSourceMovesRestoredItemToCubeLikeJava|FullyQualifiedName~HandleMoveItemAsync_CubeSourceMovesItemToAccountWarehouseOwnerLikeJava|FullyQualifiedName~HandleMoveItemAsync_CrossStorageWarehouseCubeUpdatesUseJavaStorageSize|FullyQualifiedName~HandleMoveItemAsync_FullWarehouseDestinationSendsJavaStorageFullMessageAndUnlocksSource" --logger "console;verbosity=minimal"
- Result: passed; 4 tests passed.
- Focused Java/Maven command: not run; Java source was reviewed unchanged, and no narrow Java fixture exists for this branch in this checkout.
- Broad-validation trigger: none. The change is isolated to one live handler branch plus the directly used persistence helper; no shared packet primitive or schema changed.
- Broad .NET decision: skipped; the filtered command built the affected projects and proved both owner-transition directions plus adjacent regular warehouse move behavior.
- Why this scope is sufficient: the tests exercise the previously missing restored account-warehouse runtime list path and both playerId/accountId persistence directions.
```

Repository hygiene:

```powershell
git diff --check
```

Result: passed with only Git CRLF working-copy warnings for touched files.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemMoveService.moveItem` | `GameServerConnection.HandleMoveItemAsync` | Service / live inventory mutation | Partial | Unit Tested | Partial Parity | Explicit-slot cube/account-warehouse cross moves now mutate the correct runtime lists, send delete/add packets, and persist owner/location/slot. Auto-merge, split, replace, fullness, legion, pet, and house storage remain partial. |
| `com.aionemu.gameserver.model.gameobjects.player.Player.getStorage` | `GameServerConnection.GetMoveStorageItems` | Runtime storage lookup | Partial | Unit Tested indirectly | Partial Parity | `ACCOUNT_WAREHOUSE` lookup now uses `Player.AccountWarehouseItems` for live move handling. Regular warehouse remains represented inconsistently in older handler tests and needs future cleanup. |
| `com.aionemu.gameserver.dao.InventoryDAO.store/updateItems/getItemOwnerId` | `MySqlPlayerEnterWorldRepository.SaveItemCrossStorageMoveMutationAsync` | Repository / persistence | Partial | Unit Tested via repository capture | Partial Parity | Cross-storage move persistence now updates `item_owner` to account id for account warehouse and player id otherwise. Full dirty-item store parity and switch/split/merge owner transitions remain incomplete. |
| `com.aionemu.gameserver.model.items.storage.Storage.add` | `MoveItemBetweenRuntimeStorages` / `SendStorageUpdatePacketAsync` | Storage mutation / packet fanout | Partial | Unit Tested indirectly | Partial Parity | Account warehouse boundary moves update runtime storage membership before delete/add packet fanout. Quest item-get side effects and all storage families are not fully covered. |

## Known Gaps

- Account warehouse stack auto-merge is not wired because `SaveItemMergeMutationAsync` remains player-owner based.
- `CM_SPLIT_ITEM` and `CM_REPLACE_ITEM` still search flattened `InventoryItems` and do not yet handle restored account warehouse rows.
- Same-storage account warehouse slot reorder still needs owner-aware slot persistence.
- Account warehouse capacity/fullness is not wired for destination-full rejection.
- Regular warehouse restore/list modeling remains inconsistent: enter-world restores `WarehouseItems`, while several live inventory handlers still use flattened `InventoryItems` for storage id `1`.
- Legion warehouse permissions/history, pet bags, and house storage remain deferred.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported or extended in this UOW: 4
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 42%

## Next Runtime UOW Candidates

1. Wire `CM_SPLIT_ITEM` for restored account warehouse rows with owner-aware split/merge persistence in the same UOW.
2. Wire `CM_REPLACE_ITEM` for account warehouse rows with owner-aware two-item switch persistence.
3. Inspect same-storage account warehouse slot reorder and add owner-aware slot persistence if it can be isolated safely.
