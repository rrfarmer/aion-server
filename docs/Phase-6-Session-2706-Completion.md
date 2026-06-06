# Phase 6 Session 2706 Completion

## UOW

[Phase 6] UOW-2706: Split account warehouse items.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: live CM_SPLIT_ITEM can now split non-kinah stack items between cube and restored account warehouse storage rows.
- Java source/runtime path: ItemSplitService.splitItem targetItem == null path, Storage.decreaseItemCount, Storage.add, ItemSplitService.mergeStacks, Player.getStorage(ACCOUNT_WAREHOUSE), and InventoryDAO.store owner resolution.
- C# runtime artifact wired: GameServerConnection.HandleSplitItemAsync account-aware source/target lookup and destination list mutation; MySqlPlayerEnterWorldRepository SaveItemSplitMutationAsync/SaveItemMergeMutationAsync owner-aware count/insert/delete persistence.
- Client-visible/state/persistence effect: account warehouse split sources send warehouse update packets and storage-size packets; split destination rows are added to InventoryItems or AccountWarehouseItems with playerId/accountId owner; source counts and new rows persist under the correct item_owner.
- Why this is runtime progress: this executes a live client packet path, mutates inventory/account warehouse runtime state, sends real packets, and persists through the existing inventory table.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/item/ItemSplitService.java`
  - `splitItem` resolves source and destination via `player.getStorage`.
  - Empty-slot splits decrease the source, send `SM_CUBE_UPDATE.cubeSize`, and add a new item to destination storage.
  - Cross-storage split rows do not set the requested slot; same-storage splits do.
  - `mergeStacks` updates target first and decreases or deletes source.
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/Player.java`
  - `getStorage(ACCOUNT_WAREHOUSE)` returns account warehouse.
- `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
  - `decreaseItemCount` and `add` send item update/add packets and update storage state.
- `game-server/src/com/aionemu/gameserver/dao/InventoryDAO.java`
  - Account warehouse rows are owned by account id; cube and regular player storages are owned by player id.

## C# Changes

- `HandleSplitItemAsync` now finds source and target rows through the account-aware storage helper.
- Empty-slot split destination rows are added to `AccountWarehouseItems` when destination storage is account warehouse.
- New split rows use account id as `OwnerId` when created in account warehouse and player id otherwise.
- Split rollback removes the new item from the correct runtime storage list.
- Merge source deletion removes depleted sources from the correct runtime storage list.
- `HandleKinahMoveAsync` destination lookup now uses account-aware storage lists.
- `SaveItemSplitMutationAsync` persists source count by `sourceItem.OwnerId` and inserts the new row with `newItem.OwnerId`.
- `SaveItemMergeMutationAsync` updates/deletes source by `sourceItem.OwnerId` and updates target by `targetItem.OwnerId`.

## Tests Added / Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleSplitItemAsync_AccountWarehouseSourceSplitsRestoredItemToCubeLikeJava` | Unit / live connection handler | `ItemSplitService.splitItem`, `Storage.decreaseItemCount`, `Storage.add`, `InventoryDAO.getItemOwnerId` source review | Restored account warehouse source count decreases, new cube row gets player owner/location, account warehouse update and cube add packets are sent, and split persistence captures correct owner state. | Socket-backed handler fixture, decoded packets, runtime list assertions, repository persistence capture. | Does not cover account warehouse merge target, same-storage account split, capacity/fullness, kinah move, or replace. |
| `HandleSplitItemAsync_CubeSourceSplitsItemToAccountWarehouseOwnerLikeJava` | Unit / live connection handler | Same Java split path | Cube source count decreases, new account warehouse row gets account owner/location, cube update and account warehouse add packets are sent, and split persistence captures correct owner state. | Socket-backed handler fixture, decoded packets, runtime list assertions, repository persistence capture. | Same gaps as above. |

## Validation Decision

```text
- Changed surface: live CM_SPLIT_ITEM account warehouse source/destination handling and split/merge persistence owner selection.
- Specific behavior/contract: Java resolves account warehouse storage separately, creates split rows in the destination storage, emits source decrease plus destination add packets, and stores account warehouse rows with account id.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleSplitItemAsync_AccountWarehouseSourceSplitsRestoredItemToCubeLikeJava|FullyQualifiedName~HandleSplitItemAsync_CubeSourceSplitsItemToAccountWarehouseOwnerLikeJava|FullyQualifiedName~HandleSplitItemAsync_CrossStorageEmptySlotUsesJavaStorageSize|FullyQualifiedName~HandleSplitItemAsync_FullSourceMergeDeletesSourceStackLikeJava" --logger "console;verbosity=minimal"
- Result: passed; 4 tests passed.
- Focused Java/Maven command: not run; Java source was reviewed unchanged, and no narrow Java fixture exists for this branch in this checkout.
- Broad-validation trigger: none. The change is isolated to live split handling plus directly used item count/insert/delete persistence helpers.
- Broad .NET decision: skipped; the filtered command built the affected projects and proved both account split directions plus adjacent regular split/merge behavior.
- Why this scope is sufficient: the regressions exercise the newly wired account warehouse source/destination storage lists and owner rules, and the adjacent tests guard existing split/merge packet behavior.
```

Repository hygiene:

```powershell
git diff --check
```

Result: passed with only Git CRLF working-copy warnings for touched files.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemSplitService.splitItem` | `GameServerConnection.HandleSplitItemAsync` | Service / live inventory mutation | Partial | Unit Tested | Partial Parity | Non-kinah empty-slot splits now work across cube/account warehouse with correct runtime lists, owner ids, source updates, destination adds, and persistence captures. Merge path now uses owner-aware persistence but account merge target coverage remains incomplete. |
| `com.aionemu.gameserver.services.item.ItemSplitService.mergeStacks` | `GameServerConnection.HandleSplitItemAsync` / `MySqlPlayerEnterWorldRepository.SaveItemMergeMutationAsync` | Service / persistence | Partial | Unit Tested indirectly | Partial Parity | Count/delete persistence now uses each item's owner id, allowing future account warehouse stack merges to save correctly. Account merge packet/state tests remain needed. |
| `com.aionemu.gameserver.dao.InventoryDAO.store/updateItems/getItemOwnerId` | `MySqlPlayerEnterWorldRepository.SaveItemSplitMutationAsync` / `SaveItemMergeMutationAsync` | Repository / persistence | Partial | Unit Tested via repository capture | Partial Parity | Split source counts and new rows use item owner ids. Full dirty-item store parity, switch owner transitions, and same-storage account slot updates remain incomplete. |
| `com.aionemu.gameserver.model.gameobjects.player.Player.getStorage` | `GameServerConnection.GetMoveStorageItems` | Runtime storage lookup | Partial | Unit Tested indirectly | Partial Parity | Split handling now uses account warehouse runtime storage list. Regular warehouse restored-list parity remains unresolved. |

## Known Gaps

- Account warehouse merge-into-existing-stack needs explicit packet/state tests.
- Account warehouse kinah split/move lookup was improved, but no focused kinah account warehouse test was added in this UOW.
- Same-storage account warehouse slot reorder still needs owner-aware slot persistence.
- `CM_REPLACE_ITEM` still does not use restored account warehouse rows or owner-aware switch persistence.
- Account warehouse destination fullness/capacity remains unwired.
- Regular warehouse restored-list parity remains inconsistent with older flattened tests.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported or extended in this UOW: 4
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 42%

## Next Runtime UOW Candidates

1. Wire and test account warehouse merge-into-existing-stack behavior for `CM_SPLIT_ITEM`, including source deletion from account storage.
2. Wire `CM_REPLACE_ITEM` account warehouse rows with owner-aware two-item switch persistence.
3. Wire same-storage account warehouse slot reorder with owner-aware slot persistence.
