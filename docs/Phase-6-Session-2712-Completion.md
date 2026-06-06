# Phase 6 Session 2712 Completion

## UOW

[Phase 6] UOW-2712: Add moved items to restored regular warehouse.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: live CM_MOVE_ITEM now adds cube source rows to Player.WarehouseItems for regular warehouse destinations.
- Java source/runtime path: ItemMoveService.moveItem -> Player.getStorage(REGULAR_WAREHOUSE) -> Storage.remove/add -> ItemPacketService.sendItemDeletePacket/sendStorageUpdatePacket -> InventoryDAO.store.
- C# runtime artifact wired: GameServerConnection.AddMoveStorageItem, HandleMoveItemAsync, Player.WarehouseItems, and SaveItemCrossStorageMoveMutationAsync.
- Client-visible/state/persistence effect: moving a cube item to regular warehouse removes it from Player.InventoryItems, adds it to Player.WarehouseItems with player owner/location/slot, sends cube delete plus warehouse add packets, and persists location/slot through the existing inventory table.
- Why this is runtime progress: this changes live packet handling, runtime inventory/warehouse list mutation, packets, and persistence for regular warehouse destination moves.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/item/ItemMoveService.java`
  - Cross-storage move removes from source storage, sends source delete packet, sets the destination slot, then adds to target storage.
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/Player.java`
  - `getStorage(REGULAR_WAREHOUSE)` returns regular warehouse storage.
- `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
  - `add` mutates the target storage and sends the storage update packet.
- `game-server/src/com/aionemu/gameserver/dao/InventoryDAO.java`
  - Cube and regular warehouse rows use player id as owner; move persists location/slot without schema change.

## C# Changes

- `AddMoveStorageItem` now always appends storage `1` destinations to `Player.WarehouseItems`.
- The cube-to-regular warehouse move test now asserts Java-style restored regular warehouse state instead of legacy flattened `InventoryItems` storage.

## Tests Added / Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleMoveItemAsync_CubeSourceMovesItemToRestoredRegularWarehouseLikeJava` | Unit / live connection handler | `ItemMoveService.moveItem`, `Player.getStorage(REGULAR_WAREHOUSE)`, `Storage.add`, `InventoryDAO.store` source review | Cube-to-regular move removes the source from cube inventory, adds it to `Player.WarehouseItems`, persists player-owned location/slot, and emits Java cube delete/warehouse add packets. | Socket-backed handler fixture, decoded packets, runtime list/owner/location assertions, repository capture. | Same-storage regular warehouse reorder and split/replace restored-list parity remain incomplete. |

## Validation Decision

```text
- Changed surface: live CM_MOVE_ITEM regular warehouse destination add behavior.
- Specific behavior/contract: Java adds moved cube rows to Player.getStorage(REGULAR_WAREHOUSE), so C# must mutate Player.WarehouseItems for storage 1 destinations.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleMoveItemAsync_CubeSourceMovesItemToRestoredRegularWarehouseLikeJava|FullyQualifiedName~HandleMoveItemAsync_RegularWarehouseSourceMovesRestoredItemToCubeLikeJava|FullyQualifiedName~HandleMoveItemAsync_FullWarehouseDestinationSendsJavaStorageFullMessageAndUnlocksSource" --logger "console;verbosity=minimal"
- Result: passed; 3 tests passed.
- Focused Java/Maven command: not run; Java source was reviewed unchanged, and no narrow Java fixture exists for this branch in this checkout.
- Broad-validation trigger: none. The change is isolated to regular warehouse destination list mutation in an existing live move path.
- Broad .NET decision: skipped; the filtered command built the affected project and proved destination add, restored source move, and full destination regression behavior.
- Why this scope is sufficient: the tests exercise both directions of the regular warehouse move path now using restored warehouse state and assert packet/state/persistence effects.
```

Repository hygiene:

```powershell
git diff --check
```

Result: passed with only Git CRLF working-copy warnings for touched files.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemMoveService.moveItem` | `GameServerConnection.HandleMoveItemAsync` | Service / live inventory mutation | Partial | Unit Tested | Partial Parity | Cube-to-regular and regular-to-cube moves now mutate restored `Player.WarehouseItems`. Same-storage reorder, split, and replace remain partial. |
| `com.aionemu.gameserver.model.gameobjects.player.Player.getStorage` | `GameServerConnection.GetMoveStorageItems` / `AddMoveStorageItem` | Runtime storage lookup | Partial | Unit Tested indirectly | Partial Parity | Storage 1 destinations now add to `Player.WarehouseItems`; source lookup still includes fallback for legacy flattened rows. Full Java storage object parity is not claimed. |
| `com.aionemu.gameserver.model.items.storage.Storage.add` | `AddMoveStorageItem` / `SendStorageUpdatePacketAsync` | Storage mutation / packet fanout | Partial | Unit Tested indirectly | Partial Parity | Regular warehouse destination add now mutates the restored warehouse list and sends warehouse add packets. Other storage families and branches remain incomplete. |
| `com.aionemu.gameserver.dao.InventoryDAO.store/updateItems/getItemOwnerId` | `MySqlPlayerEnterWorldRepository.SaveItemCrossStorageMoveMutationAsync` | Repository / persistence | Partial | Unit Tested via repository capture | Partial Parity | Regular warehouse destination move persists player-owned location/slot through the existing row update. Broader dirty-item store parity remains incomplete. |

## Known Gaps

- Regular warehouse same-storage slot reorder and replace/split restored-list parity remain incomplete.
- Regular warehouse source lookup still keeps a legacy flattened fallback for older paths/tests.
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

1. Scope regular warehouse same-storage slot persistence against restored `Player.WarehouseItems`.
2. Scope regular warehouse replace restored-list parity for a narrow cube/warehouse switch.
3. Inspect account warehouse split merge-into-existing-stack and proceed only if code, not just coverage, is missing.
