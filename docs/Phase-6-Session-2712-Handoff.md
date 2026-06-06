# Phase 6 Session 2712 Handoff

## Completed UOW

[Phase 6] UOW-2712: Add moved items to restored regular warehouse.

## Runtime Progress Gate Result

```text
- Deferred/live behavior advanced: live CM_MOVE_ITEM now adds cube source rows to Player.WarehouseItems for regular warehouse destinations.
- Java source/runtime path: ItemMoveService.moveItem -> Player.getStorage(REGULAR_WAREHOUSE) -> Storage.remove/add -> ItemPacketService sendItemDeletePacket/sendStorageUpdatePacket -> InventoryDAO.store.
- C# runtime artifact wired: GameServerConnection.AddMoveStorageItem, HandleMoveItemAsync, Player.WarehouseItems, SaveItemCrossStorageMoveMutationAsync.
- Client-visible/state/persistence effect: moving a cube item to regular warehouse removes it from Player.InventoryItems, adds it to Player.WarehouseItems with player owner/location/slot, sends cube delete plus warehouse add packets, and persists location/slot through the existing inventory table.
- Why this is runtime progress: it changes live packet handling, runtime inventory/warehouse list mutation, packets, and persistence for regular warehouse destination moves.
```

## Commit

`[Phase 6][UOW-2712] Add moved items to regular warehouse`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/Phase-6-Session-2712-Completion.md`
- `docs/Phase-6-Session-2712-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/services/item/ItemMoveService.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/Player.java`
- `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
- `game-server/src/com/aionemu/gameserver/dao/InventoryDAO.java`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection.AddMoveStorageItem`
- `Aion.GameServer.Network.Aion.GameServerConnection.HandleMoveItemAsync`
- `Aion.GameServer.Model.GameObjects.Player.WarehouseItems`
- `Aion.GameServer.Tests.GameServerConnectionInventoryExpansionUseItemTests`

## Validation

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleMoveItemAsync_CubeSourceMovesItemToRestoredRegularWarehouseLikeJava|FullyQualifiedName~HandleMoveItemAsync_RegularWarehouseSourceMovesRestoredItemToCubeLikeJava|FullyQualifiedName~HandleMoveItemAsync_FullWarehouseDestinationSendsJavaStorageFullMessageAndUnlocksSource" --logger "console;verbosity=minimal"
```

Result:

- Passed: 3
- Failed: 0
- Skipped: 0
- Existing warnings only.

Repository hygiene:

```powershell
git diff --check
```

Result:

- Passed with only Git CRLF working-copy warnings for touched files.

Java/Maven:

- Not run. Java source was reviewed unchanged, and no narrow Java fixture exists for this branch in this checkout.

Broad .NET:

- Not run. Broad-validation trigger: none.

## Conservative Parity Status

- `CM_MOVE_ITEM` cube-to-regular warehouse and regular-to-cube moves now have partial runtime parity for restored `Player.WarehouseItems`.
- Full storage-1 parity is not claimed. Same-storage slot reorder, split, replace, and broader storage-object behavior remain partial.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemMoveService.moveItem` | `GameServerConnection.HandleMoveItemAsync` | Service / live inventory mutation | Partial | Unit Tested | Partial Parity | Cube-to-regular and regular-to-cube moves now mutate restored `Player.WarehouseItems`. Same-storage reorder, split, and replace remain partial. |
| `com.aionemu.gameserver.model.gameobjects.player.Player.getStorage` | `GameServerConnection.GetMoveStorageItems` / `AddMoveStorageItem` | Runtime storage lookup | Partial | Unit Tested indirectly | Partial Parity | Storage 1 destinations now add to `Player.WarehouseItems`; source lookup still includes fallback for legacy flattened rows. Full Java storage object parity is not claimed. |
| `com.aionemu.gameserver.model.items.storage.Storage.add` | `AddMoveStorageItem` / `SendStorageUpdatePacketAsync` | Storage mutation / packet fanout | Partial | Unit Tested indirectly | Partial Parity | Regular warehouse destination add now mutates the restored warehouse list and sends warehouse add packets. Other storage families and branches remain incomplete. |
| `com.aionemu.gameserver.dao.InventoryDAO.store/updateItems/getItemOwnerId` | `MySqlPlayerEnterWorldRepository.SaveItemCrossStorageMoveMutationAsync` | Repository / persistence | Partial | Unit Tested via repository capture | Partial Parity | Regular warehouse destination move persists player-owned location/slot through the existing row update. Broader dirty-item store parity remains incomplete. |

## Known Gaps / Watchouts

- Regular warehouse same-storage slot reorder and replace/split restored-list parity remain incomplete.
- Regular warehouse source lookup still keeps a legacy flattened fallback for older paths/tests.
- Account warehouse split merge-into-existing-stack should be inspected before any runtime fix.
- Account warehouse kinah split/move still needs source review for owner/list/persistence behavior.
- Legion warehouse permissions/history, pet bags, and house storage remain deferred.

## Next Recommended Runtime UOW

Recommended candidate: regular warehouse same-storage slot persistence against restored `Player.WarehouseItems`.

Runtime progress gate for that candidate:

```text
- Deferred/live behavior to advance: live CM_MOVE_ITEM same-storage reorder should update slots for restored Player.WarehouseItems rows when source/destination storage is regular warehouse.
- Java source/runtime path: ItemMoveService.moveInSameStorage -> item.setEquipmentSlot -> InventoryDAO.store using player owner for REGULAR_WAREHOUSE.
- C# runtime artifact likely involved: GameServerConnection.HandleMoveItemAsync same-storage path, GetMoveStorageItems, Player.WarehouseItems, PlayerEnterWorldService.SaveInventoryItemSlotAsync.
- Client-visible/state/persistence effect expected: restored regular warehouse row slot changes in runtime state and persists with player owner id; no response packets are sent, matching Java/client UI behavior.
- Why this is runtime progress: it changes live packet handling and database persistence for restored regular warehouse state.
```

Suggested focused validation starting point if implemented:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleMoveItemAsync_RegularWarehouseSameStorageSlotPersistsRestoredRowLikeJava|FullyQualifiedName~HandleMoveItemAsync_RegularWarehouseSourceMovesRestoredItemToCubeLikeJava|FullyQualifiedName~HandleMoveItemAsync_CubeSourceMovesItemToRestoredRegularWarehouseLikeJava" --logger "console;verbosity=minimal"
```

Java/Maven is not expected unless a narrow Java fixture is added or Java source changes. Broad-validation trigger: none unless storage helper changes are broadened beyond live inventory/warehouse move paths.

## Other Safe Runtime Candidates

- Scope regular warehouse replace restored-list parity for a narrow cube/warehouse switch.
- Inspect account warehouse split merge-into-existing-stack and proceed only if code, not just coverage, is missing.
- Inspect account warehouse kinah split/move and fix any live owner/list/persistence mismatch found.

## Context Needed By Next Session

- Startup must reread `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, the matching completion doc, and `git status --short`.
- Latest completed commits before this UOW:
  - `9b9957296 [Phase 6][UOW-2711] Move restored regular warehouse items`
  - `b1ff8d5dd [Phase 6][UOW-2710] Merge account warehouse move stacks`
  - `aa483b09e [Phase 6][UOW-2709] Reject full account warehouse destinations`
- Ignore preview, readiness, metadata, and test-only recommendations unless explicitly requested or immediately tied to same-session runtime behavior.
