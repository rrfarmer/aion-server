# Phase 6 Session 2707 Completion

## UOW

[Phase 6] UOW-2707: Replace account warehouse items.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: live CM_REPLACE_ITEM can now switch cube items with restored account warehouse rows.
- Java source/runtime path: ItemMoveService.switchItemsInStorages, Player.getStorage(ACCOUNT_WAREHOUSE), Storage.remove/add, ItemPacketService.sendItemDeletePacket/sendStorageUpdatePacket, and InventoryDAO.store/getItemOwnerId.
- C# runtime artifact wired: GameServerConnection.HandleReplaceItemAsync account-aware lookup/list mutation and PlayerEnterWorldService/MySqlPlayerEnterWorldRepository SaveItemStorageSwitchMutationAsync owner-aware persistence.
- Client-visible/state/persistence effect: switch emits both source delete packets before both destination add packets, moves items between InventoryItems and AccountWarehouseItems, rewrites OwnerId playerId <-> accountId, and persists item_owner/location/slot for both rows.
- Why this is runtime progress: this changes a live client packet handler, mutates runtime inventory/account warehouse state, sends server packets, and persists through the existing inventory table.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/item/ItemMoveService.java`
  - `switchItemsInStorages` resolves both storages via `player.getStorage`.
  - It checks restrictions/trading/shutdown, unlocks both items on rejection, swaps slots, removes both items, sends two delete packets, then adds each item to the opposite storage.
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/Player.java`
  - `getStorage(ACCOUNT_WAREHOUSE)` returns account warehouse.
- `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
  - `add` sets the item's location to the destination storage and sends the storage update packet.
- `game-server/src/com/aionemu/gameserver/dao/InventoryDAO.java`
  - Account warehouse rows are persisted with account id; cube and regular player storages use player id.

## C# Changes

- `HandleReplaceItemAsync` now finds source and replacement rows through the account-aware storage helper.
- Cross-storage switch now removes both items from their old runtime lists before swapping location/slot/owner and adding them to their new lists.
- Save failure rolls back both runtime lists, locations, slots, and owners.
- `SaveItemStorageSwitchMutationAsync` now receives account id plus old/new locations for both rows and persists each row with the Java owner rule.
- Existing regular warehouse/cube switch coverage was updated to assert the wider owner-aware persistence capture.

## Tests Added / Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleReplaceItemAsync_AccountWarehouseSwitchMovesRestoredRowsAndOwnersLikeJava` | Unit / live connection handler | `ItemMoveService.switchItemsInStorages`, `Storage.add`, `InventoryDAO.getItemOwnerId` source review | Cube/account warehouse switch moves restored rows between runtime lists, swaps slots, rewrites owners, emits Java delete/add order, and records owner-aware persistence. | Socket-backed handler fixture, decoded packets, runtime list/owner assertions, repository capture. | Does not cover same-storage account warehouse reorder, account warehouse restriction-from branch, or legion/pet/house storage. |
| `HandleReplaceItemAsync_AccountWarehouseSaveFailureRollsBackListsAndOwners` | Unit / live connection handler | Same Java switch path plus persistence failure safety | Failed persistence restores original runtime list membership, owners, locations, and slots before any packets are emitted. | Socket-backed handler fixture and repository failure capture. | Rollback behavior is C# safety around Java-equivalent state mutation; Java DB failure behavior is not separately runtime-compared. |

## Validation Decision

```text
- Changed surface: live CM_REPLACE_ITEM account warehouse cross-storage switch and switch persistence owner selection.
- Specific behavior/contract: Java removes both old storage rows, sends delete packets, adds both rows to opposite storages, and stores account warehouse rows with account id.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleReplaceItemAsync_AccountWarehouseSwitchMovesRestoredRowsAndOwnersLikeJava|FullyQualifiedName~HandleReplaceItemAsync_AccountWarehouseSaveFailureRollsBackListsAndOwners|FullyQualifiedName~HandleReplaceItemAsync_CrossStorageSwitchDeletesThenAddsLikeJava|FullyQualifiedName~HandleReplaceItemAsync_StorageSwitchSaveFailureRollsBackBothItems" --logger "console;verbosity=minimal"
- Result: passed; 4 tests passed.
- Focused Java/Maven command: not run; Java source was reviewed unchanged, and no narrow Java fixture exists for this branch in this checkout.
- Broad-validation trigger: none. The change is isolated to live replace handling plus the directly used switch persistence helper.
- Broad .NET decision: skipped; the filtered command built the affected projects and proved account switch, rollback, and adjacent regular warehouse/cube switch behavior.
- Why this scope is sufficient: the regressions exercise the missing restored account warehouse row path and both owner transition directions through the switch persistence capture.
```

Repository hygiene:

```powershell
git diff --check
```

Result: passed with only Git CRLF working-copy warnings for touched files.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemMoveService.switchItemsInStorages` | `GameServerConnection.HandleReplaceItemAsync` | Service / live inventory mutation | Partial | Unit Tested | Partial Parity | Cube/account warehouse cross-storage switch now resolves restored account rows, mutates runtime lists/owners/slots, emits Java delete/add order, and persists both rows with owner-aware location updates. Same-storage account warehouse reorder and full storage-family parity remain incomplete. |
| `com.aionemu.gameserver.dao.InventoryDAO.store/updateItems/getItemOwnerId` | `MySqlPlayerEnterWorldRepository.SaveItemStorageSwitchMutationAsync` | Repository / persistence | Partial | Unit Tested via repository capture | Partial Parity | Switch persistence now uses account id for account warehouse old/new locations and player id otherwise. Full dirty-item store parity and other storage families remain incomplete. |
| `com.aionemu.gameserver.model.gameobjects.player.Player.getStorage` | `GameServerConnection.GetMoveStorageItems` | Runtime storage lookup | Partial | Unit Tested indirectly | Partial Parity | Replace handling now uses account warehouse runtime storage list. Regular warehouse restored-list parity remains unresolved. |
| `com.aionemu.gameserver.model.items.storage.Storage.add/remove` | `RemoveMoveStorageItem` / `AddMoveStorageItem` / `SendStorageUpdatePacketAsync` | Storage mutation / packet fanout | Partial | Unit Tested indirectly | Partial Parity | Cross-storage replace now removes both old rows and adds each item to the opposite runtime list before packet fanout. Full Java storage state and handler side effects are not fully covered. |

## Known Gaps

- Same-storage account warehouse slot reorder still uses player-owner slot persistence and remains unwired.
- Account warehouse merge-into-existing-stack has owner-aware persistence support but still needs explicit packet/state tests.
- Account warehouse kinah split/move needs focused coverage.
- Account warehouse capacity/fullness remains unwired.
- Regular warehouse restored-list parity remains inconsistent with older flattened tests.
- Legion warehouse permissions/history, pet bags, and house storage remain deferred.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported or extended in this UOW: 4
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 42%

## Next Runtime UOW Candidates

1. Wire same-storage account warehouse slot reorder with owner-aware slot persistence.
2. Add runtime coverage and any missing list fixes for account warehouse merge-into-existing-stack in `CM_SPLIT_ITEM`.
3. Add account warehouse kinah split/move coverage and fix any owner/list gaps found there.
