# Phase 6 Session 2711 Handoff

## Completed UOW

[Phase 6] UOW-2711: Move restored regular warehouse items.

## Runtime Progress Gate Result

```text
- Deferred/live behavior advanced: live CM_MOVE_ITEM can now move a restored regular warehouse row from Player.WarehouseItems to cube.
- Java source/runtime path: ItemMoveService.moveItem -> Player.getStorage(REGULAR_WAREHOUSE) -> Storage.remove/add -> ItemPacketService sendItemDeletePacket/sendStorageUpdatePacket -> InventoryDAO.store.
- C# runtime artifact wired: GameServerConnection regular warehouse storage lookup/removal/count helpers, HandleMoveItemAsync, Player.WarehouseItems, SaveItemCrossStorageMoveMutationAsync.
- Client-visible/state/persistence effect: restored warehouse rows are removed from Player.WarehouseItems, added to Player.InventoryItems, warehouse delete/cube add packets are sent with Java storage sizes, and the existing inventory row location/slot is persisted.
- Why this is runtime progress: it changes live packet handling, runtime warehouse/inventory list mutation, packets, and persistence for restored database state.
```

## Commit

`[Phase 6][UOW-2711] Move restored regular warehouse items`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/Phase-6-Session-2711-Completion.md`
- `docs/Phase-6-Session-2711-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/services/item/ItemMoveService.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/Player.java`
- `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
- `game-server/src/com/aionemu/gameserver/dao/InventoryDAO.java`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection.GetMoveStorageItems`
- `Aion.GameServer.Network.Aion.GameServerConnection.RemoveMoveStorageItem`
- `Aion.GameServer.Network.Aion.GameServerConnection.AddMoveStorageItem`
- `Aion.GameServer.Network.Aion.GameServerConnection.SetMoveStorageItems`
- `Aion.GameServer.Network.Aion.GameServerConnection.CreateStorageSizePacket`
- `Aion.GameServer.Network.Aion.GameServerConnection.HandleMoveItemAsync`
- `Aion.GameServer.Model.GameObjects.Player.WarehouseItems`
- `Aion.GameServer.Tests.GameServerConnectionInventoryExpansionUseItemTests`

## Validation

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleMoveItemAsync_RegularWarehouseSourceMovesRestoredItemToCubeLikeJava|FullyQualifiedName~HandleMoveItemAsync_CrossStorageWarehouseCubeUpdatesUseJavaStorageSize|FullyQualifiedName~HandleMoveItemAsync_FullWarehouseDestinationSendsJavaStorageFullMessageAndUnlocksSource" --logger "console;verbosity=minimal"
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

- `CM_MOVE_ITEM` regular warehouse source lookup now has partial runtime parity for restored `Player.WarehouseItems` rows moving to cube.
- Full storage-1 parity is not claimed. Destination add, same-storage reorder, split, replace, and full Java storage object behavior remain partial.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemMoveService.moveItem` | `GameServerConnection.HandleMoveItemAsync` | Service / live inventory mutation | Partial | Unit Tested | Partial Parity | Restored regular warehouse source rows can now move to cube. Regular warehouse destination add, same-storage reorder, split, and replace paths remain partial. |
| `com.aionemu.gameserver.model.gameobjects.player.Player.getStorage` | `GameServerConnection.GetMoveStorageItems` | Runtime storage lookup | Partial | Unit Tested indirectly | Partial Parity | Storage 1 now prefers restored `Player.WarehouseItems` when present, with fallback to legacy flattened rows for incomplete older paths. Full Java storage object parity is not claimed. |
| `com.aionemu.gameserver.model.items.storage.Storage.remove/add` | `RemoveMoveStorageItem` / `AddMoveStorageItem` / `SendStorageUpdatePacketAsync` | Storage mutation / packet fanout | Partial | Unit Tested indirectly | Partial Parity | Restored regular warehouse source removal plus cube add are covered. Destination regular warehouse add remains fallback-dependent. |
| `com.aionemu.gameserver.dao.InventoryDAO.store/updateItems/getItemOwnerId` | `MySqlPlayerEnterWorldRepository.SaveItemCrossStorageMoveMutationAsync` | Repository / persistence | Partial | Unit Tested via repository capture | Partial Parity | Regular warehouse to cube move persists player-owned old/new locations and slot through existing row update. Broader dirty-item store semantics remain incomplete. |

## Known Gaps / Watchouts

- Regular warehouse destination moves still use legacy flattened fallback when no `Player.WarehouseItems` list is active.
- Regular warehouse same-storage slot reorder and replace/split restored-list parity remain incomplete.
- Account warehouse split merge-into-existing-stack should be inspected before any runtime fix.
- Account warehouse kinah split/move still needs source review for owner/list/persistence behavior.
- Legion warehouse permissions/history, pet bags, and house storage remain deferred.

## Next Recommended Runtime UOW

Recommended candidate: regular warehouse destination add for live `CM_MOVE_ITEM`, starting with cube-to-regular warehouse into restored `Player.WarehouseItems`.

Runtime progress gate for that candidate:

```text
- Deferred/live behavior to advance: live CM_MOVE_ITEM should add cube source rows to Player.WarehouseItems for regular warehouse destinations, matching Java Player.getStorage(REGULAR_WAREHOUSE).
- Java source/runtime path: ItemMoveService.moveItem -> Player.getStorage(REGULAR_WAREHOUSE) -> Storage.remove/add -> ItemPacketService sendItemDeletePacket/sendStorageUpdatePacket -> InventoryDAO.store.
- C# runtime artifact likely involved: GameServerConnection.AddMoveStorageItem/SetMoveStorageItems/GetMoveStorageItems, HandleMoveItemAsync, Player.WarehouseItems, and SaveItemCrossStorageMoveMutationAsync.
- Client-visible/state/persistence effect expected: moving a cube item to regular warehouse removes it from Player.InventoryItems, adds it to Player.WarehouseItems with player owner/location/slot, sends cube delete plus warehouse add packets, and persists location/slot through the existing inventory table.
- Why this is runtime progress: it changes live packet handling, runtime inventory/warehouse list mutation, packets, and persistence for restored warehouse state.
```

Suggested focused validation starting point if implemented:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleMoveItemAsync_CubeSourceMovesItemToRestoredRegularWarehouseLikeJava|FullyQualifiedName~HandleMoveItemAsync_RegularWarehouseSourceMovesRestoredItemToCubeLikeJava|FullyQualifiedName~HandleMoveItemAsync_CrossStorageWarehouseCubeUpdatesUseJavaStorageSize" --logger "console;verbosity=minimal"
```

Java/Maven is not expected unless a narrow Java fixture is added or Java source changes. Broad-validation trigger: none unless storage helper changes are broadened beyond live inventory/warehouse move paths.

## Other Safe Runtime Candidates

- Scope regular warehouse same-storage slot persistence against restored `Player.WarehouseItems`.
- Inspect account warehouse split merge-into-existing-stack and proceed only if code, not just coverage, is missing.
- Inspect account warehouse kinah split/move and fix any live owner/list/persistence mismatch found.

## Context Needed By Next Session

- Startup must reread `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, the matching completion doc, and `git status --short`.
- Latest completed commits before this UOW:
  - `b1ff8d5dd [Phase 6][UOW-2710] Merge account warehouse move stacks`
  - `aa483b09e [Phase 6][UOW-2709] Reject full account warehouse destinations`
  - `7f11e6495 [Phase 6][UOW-2708] Persist account warehouse slots`
- Ignore preview, readiness, metadata, and test-only recommendations unless explicitly requested or immediately tied to same-session runtime behavior.
