# Phase 6 Session 2711 Completion

## UOW

[Phase 6] UOW-2711: Move restored regular warehouse items.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: live CM_MOVE_ITEM can now move a restored regular warehouse row from Player.WarehouseItems to cube.
- Java source/runtime path: ItemMoveService.moveItem -> Player.getStorage(REGULAR_WAREHOUSE) -> Storage.remove/add -> ItemPacketService.sendItemDeletePacket/sendStorageUpdatePacket -> InventoryDAO.store.
- C# runtime artifact wired: GameServerConnection regular warehouse storage lookup/removal/count helpers, HandleMoveItemAsync, Player.WarehouseItems, and SaveItemCrossStorageMoveMutationAsync.
- Client-visible/state/persistence effect: restored warehouse rows are removed from Player.WarehouseItems, added to Player.InventoryItems, warehouse delete/cube add packets are sent with Java storage sizes, and the existing inventory row location/slot is persisted.
- Why this is runtime progress: this changes live packet handling, runtime warehouse/inventory list mutation, packets, and persistence for restored database state.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/item/ItemMoveService.java`
  - `moveItem` resolves both source and destination through `player.getStorage`.
  - Cross-storage move removes the item from source storage, sends delete packet for the source storage, sets the slot, then adds it to target storage.
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/Player.java`
  - `getStorage(REGULAR_WAREHOUSE)` returns the regular warehouse storage object, not the cube inventory.
- `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
  - `remove` and `add` operate on the resolved storage object and drive packet fanout through item packet services.
- `game-server/src/com/aionemu/gameserver/dao/InventoryDAO.java`
  - Regular warehouse and cube rows use player id as owner; moving between them updates the same row's location/slot.

## C# Changes

- `GetMoveStorageItems` now prefers restored `Player.WarehouseItems` for storage `1` when those rows are present.
- `RemoveMoveStorageItem` removes storage-1 rows from `Player.WarehouseItems` when the restored row was used.
- `AddMoveStorageItem`/`SetMoveStorageItems` now preserve restored regular warehouse list usage when a regular warehouse list is active.
- Regular warehouse free-slot and size packet counts now use restored `WarehouseItems` when present, falling back to legacy flattened `InventoryItems` rows for older paths/tests.
- Added focused live-handler coverage for moving a restored regular warehouse row to cube.

## Tests Added / Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleMoveItemAsync_RegularWarehouseSourceMovesRestoredItemToCubeLikeJava` | Unit / live connection handler | `ItemMoveService.moveItem`, `Player.getStorage(REGULAR_WAREHOUSE)`, `Storage.remove/add`, `InventoryDAO.store` source review | A restored regular warehouse source row is found in `Player.WarehouseItems`, removed from that list, added to cube inventory, persisted with player owner/location/slot, and emits Java warehouse delete/cube add packet order. | Socket-backed handler fixture, decoded packets, runtime list/slot/location assertions, repository capture. | Storage-1 destination add and split paths still retain legacy flattened fallback behavior and need separate runtime scoping. |

## Validation Decision

```text
- Changed surface: live CM_MOVE_ITEM regular warehouse storage lookup/removal/count helpers.
- Specific behavior/contract: Java resolves regular warehouse through Player.getStorage(REGULAR_WAREHOUSE), so restored warehouse rows must be moved from Player.WarehouseItems to cube with warehouse delete then cube add packets.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleMoveItemAsync_RegularWarehouseSourceMovesRestoredItemToCubeLikeJava|FullyQualifiedName~HandleMoveItemAsync_CrossStorageWarehouseCubeUpdatesUseJavaStorageSize|FullyQualifiedName~HandleMoveItemAsync_FullWarehouseDestinationSendsJavaStorageFullMessageAndUnlocksSource" --logger "console;verbosity=minimal"
- Result: passed; 3 tests passed.
- Focused Java/Maven command: not run; Java source was reviewed unchanged, and no narrow Java fixture exists for this branch in this checkout.
- Broad-validation trigger: none. The change is isolated to move/split storage helper behavior and validated against restored plus adjacent legacy regular warehouse move paths.
- Broad .NET decision: skipped; the filtered command built the affected project and proved restored source behavior plus adjacent regular warehouse regressions.
- Why this scope is sufficient: the tests exercise the exact restored regular warehouse source path changed by this UOW and assert packet order, runtime state, and persistence capture.
```

Repository hygiene:

```powershell
git diff --check
```

Result: passed with only Git CRLF working-copy warnings for touched files.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemMoveService.moveItem` | `GameServerConnection.HandleMoveItemAsync` | Service / live inventory mutation | Partial | Unit Tested | Partial Parity | Restored regular warehouse source rows can now move to cube. Regular warehouse destination add, same-storage reorder, split, and replace paths remain partial. |
| `com.aionemu.gameserver.model.gameobjects.player.Player.getStorage` | `GameServerConnection.GetMoveStorageItems` | Runtime storage lookup | Partial | Unit Tested indirectly | Partial Parity | Storage 1 now prefers restored `Player.WarehouseItems` when present, with fallback to legacy flattened rows for incomplete older paths. Full Java storage object parity is not claimed. |
| `com.aionemu.gameserver.model.items.storage.Storage.remove/add` | `RemoveMoveStorageItem` / `AddMoveStorageItem` / `SendStorageUpdatePacketAsync` | Storage mutation / packet fanout | Partial | Unit Tested indirectly | Partial Parity | Restored regular warehouse source removal plus cube add are covered. Destination regular warehouse add remains fallback-dependent. |
| `com.aionemu.gameserver.dao.InventoryDAO.store/updateItems/getItemOwnerId` | `MySqlPlayerEnterWorldRepository.SaveItemCrossStorageMoveMutationAsync` | Repository / persistence | Partial | Unit Tested via repository capture | Partial Parity | Regular warehouse to cube move persists player-owned old/new locations and slot through existing row update. Broader dirty-item store semantics remain incomplete. |

## Known Gaps

- Regular warehouse destination moves still use legacy flattened fallback when no `Player.WarehouseItems` list is active.
- Regular warehouse same-storage slot reorder and replace/split restored-list parity remain incomplete.
- Account warehouse split merge-into-existing-stack should be inspected before any runtime fix.
- Account warehouse kinah split/move still needs source review for owner/list/persistence behavior.
- Legion warehouse permissions/history, pet bags, and house storage remain deferred.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported or extended in this UOW: 4
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 42%

## Next Runtime UOW Candidates

1. Wire regular warehouse destination add to restored `Player.WarehouseItems` for a narrow `CM_MOVE_ITEM` cube-to-regular path.
2. Scope regular warehouse same-storage slot persistence against restored `Player.WarehouseItems`.
3. Inspect account warehouse split merge-into-existing-stack and proceed only if code, not just coverage, is missing.
