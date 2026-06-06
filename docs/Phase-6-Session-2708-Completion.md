# Phase 6 Session 2708 Completion

## UOW

[Phase 6] UOW-2708: Persist account warehouse same-storage slots.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: live CM_MOVE_ITEM and same-storage CM_REPLACE_ITEM now persist restored account warehouse slot changes against the account-owned inventory row.
- Java source/runtime path: ItemMoveService.moveInSameStorage and switchItemsInStorages same-storage branch -> item.setEquipmentSlot -> InventoryDAO.store/getItemOwnerId.
- C# runtime artifact wired: GameServerConnection.HandleMoveItemAsync, GameServerConnection.HandleReplaceItemAsync, PlayerEnterWorldService.SaveInventoryItemSlotAsync, and MySqlPlayerEnterWorldRepository.SaveInventoryItemSlotAsync.
- Client-visible/state/persistence effect: account warehouse item slots mutate in runtime state and persist with item_owner = accountId; no response packets are sent for same-storage reorder, matching the Java/client UI flow.
- Why this is runtime progress: this changes live client packet handling and database persistence for restored account warehouse state.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/item/ItemMoveService.java`
  - `moveItem` dispatches same-storage slot movement to `moveInSameStorage` when the destination storage matches the source storage.
  - `moveInSameStorage` updates the item's equipment slot and marks the item for persistence without sending a packet.
  - `switchItemsInStorages` swaps only slot values when both items are in the same storage; it does not send delete/add packets.
- `game-server/src/com/aionemu/gameserver/dao/InventoryDAO.java`
  - Account warehouse rows are stored with account id, while cube and regular warehouse rows use player id.

## C# Changes

- `HandleMoveItemAsync` now passes the storage type into slot persistence so account warehouse slot moves save against account id.
- `HandleReplaceItemAsync` same-storage switch now persists both slot changes with the source/replacement storage type.
- `PlayerEnterWorldService.SaveInventoryItemSlotAsync` resolves `item_owner` from storage type: account id for storage `2`, player object id otherwise.
- `MySqlPlayerEnterWorldRepository.SaveInventoryItemSlotAsync` now treats the owner id as a row owner rather than assuming player object id.
- The empty repository test double now captures slot persistence owner/object/slot tuples.

## Tests Added / Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleMoveItemAsync_AccountWarehouseSameStorageSlotPersistsWithAccountOwnerLikeJava` | Unit / live connection handler | `ItemMoveService.moveInSameStorage`, `InventoryDAO.getItemOwnerId` source review | Same-storage account warehouse move mutates the restored row slot, persists with account owner id, and sends no packets. | Socket-backed handler fixture, runtime slot/owner assertion, repository capture, packet silence assertion. | Does not cover full account warehouse capacity/fullness or other storage families. |
| `HandleReplaceItemAsync_AccountWarehouseSameStorageSwitchPersistsSlotsWithAccountOwnerLikeJava` | Unit / live connection handler | `ItemMoveService.switchItemsInStorages` same-storage branch plus `InventoryDAO.getItemOwnerId` | Same-storage account warehouse replace swaps runtime slots for both restored rows, persists both rows with account owner id, avoids cross-storage mutation, and sends no packets. | Socket-backed handler fixture, runtime slot/list assertions, repository capture, packet silence assertion. | Does not cover cross-storage replace beyond existing adjacent regression tests. |

## Validation Decision

```text
- Changed surface: live CM_MOVE_ITEM same-storage path, live CM_REPLACE_ITEM same-storage path, and shared slot persistence owner selection.
- Specific behavior/contract: Java same-storage move/replace mutates equipmentSlot and persists account warehouse rows with account id without response packets.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleMoveItemAsync_AccountWarehouseSameStorageSlotPersistsWithAccountOwnerLikeJava|FullyQualifiedName~HandleReplaceItemAsync_AccountWarehouseSameStorageSwitchPersistsSlotsWithAccountOwnerLikeJava|FullyQualifiedName~HandleReplaceItemAsync_CrossStorageSwitchDeletesThenAddsLikeJava|FullyQualifiedName~HandleReplaceItemAsync_ShutdownSoonUnlocksBothItemsLikeJava|FullyQualifiedName~HandleReplaceItemAsync_StorageSwitchSaveFailureRollsBackBothItems|FullyQualifiedName~HandleReplaceItemAsync_AccountWarehouseSaveFailureRollsBackListsAndOwners" --logger "console;verbosity=minimal"
- Result: passed; 6 tests passed.
- Focused Java/Maven command: not run; Java source was reviewed unchanged, and no narrow Java fixture exists for this branch in this checkout.
- Broad-validation trigger: none. The implementation is isolated to owner selection for existing slot persistence and the two live same-storage packet paths that call it.
- Broad .NET decision: skipped; the filtered command built the affected projects and proved new account warehouse behavior plus adjacent replace regressions.
- Why this scope is sufficient: the new tests exercise account-owned restored rows through the live handlers and assert the observable Java contract: state mutation, owner-aware persistence, and no packets.
```

Repository hygiene:

```powershell
git diff --check
```

Result: passed with only Git CRLF working-copy warnings for touched files.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemMoveService.moveInSameStorage` | `GameServerConnection.HandleMoveItemAsync` | Service / live inventory mutation | Partial | Unit Tested | Partial Parity | Same-storage account warehouse move now mutates runtime slot and persists with account id. Other storage-family edge cases remain incomplete. |
| `com.aionemu.gameserver.services.item.ItemMoveService.switchItemsInStorages` | `GameServerConnection.HandleReplaceItemAsync` | Service / live inventory mutation | Partial | Unit Tested | Partial Parity | Same-storage account warehouse replace now swaps slots, persists both account-owned rows, and sends no packets. Cross-storage account warehouse behavior was covered in UOW-2707; full storage parity remains open. |
| `com.aionemu.gameserver.dao.InventoryDAO.store/updateItems/getItemOwnerId` | `PlayerEnterWorldService.SaveInventoryItemSlotAsync` / `MySqlPlayerEnterWorldRepository.SaveInventoryItemSlotAsync` | Repository / persistence | Partial | Unit Tested via repository capture | Partial Parity | Slot persistence now uses account id for account warehouse and player id otherwise. Full Java dirty-item store semantics are not claimed. |
| `com.aionemu.gameserver.model.gameobjects.player.Player.getStorage` | `GameServerConnection.GetMoveStorageItems` | Runtime storage lookup | Partial | Unit Tested indirectly | Partial Parity | Account warehouse same-storage paths operate on restored `Player.AccountWarehouseItems`. Regular warehouse restored-list parity remains unresolved. |

## Known Gaps

- Account warehouse merge-into-existing-stack should be inspected and only continued if source review finds a live mismatch beyond existing owner-aware persistence.
- Account warehouse kinah split/move still needs runtime source review; proceed only if a live owner/list/persistence mismatch is found.
- Account warehouse capacity/fullness is not fully wired.
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

1. Inspect account warehouse kinah split/move in `CM_SPLIT_ITEM`/`CM_MOVE_ITEM` and fix any live owner/list/persistence mismatch found.
2. Inspect account warehouse merge-into-existing-stack and proceed only if code, not just coverage, is missing.
3. Scope account warehouse capacity/fullness against Java storage limits and wire a live rejection path if missing.
