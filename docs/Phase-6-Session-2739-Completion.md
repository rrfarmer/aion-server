# Phase 6 Session 2739 Completion

## UOW

[Phase 6] UOW-2739: Track legion warehouse deleted rows.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: location `3` legion warehouse item deletes now enter C# dirty item state and can be flushed by logout/periodic item persistence.
- Java source/runtime path: `Storage.delete`, `LegionStorageProxy.delete`, `Player.getDirtyItemsToUpdate`, and `InventoryDAO.store(player)`.
- C# runtime artifact wired: `Player.TrackDeletedItem`, `Player.GetDirtyItemsToUpdate`, and the existing `MySqlPlayerEnterWorldRepository.SavePeriodicPlayerItemsAsync` dirty snapshot path.
- Client-visible/state/persistence effect: deleted legion warehouse rows are removed from live flattened storage state, tracked as deleted location `3` rows, and deleted from the existing `inventory` table on snapshot save.
- Why this is runtime progress: it mutates live player/legion warehouse item state and persists that mutation through the existing DB schema.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
  - `delete` removes the item from current storage, marks it deleted, adds it to `deletedItems`, marks storage `UPDATE_REQUIRED`, and sends the delete packet.
- `game-server/src/com/aionemu/gameserver/model/items/storage/LegionStorageProxy.java`
  - `delete` delegates to the underlying legion warehouse storage with the acting player.
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/Player.java`
  - `getDirtyItemsToUpdate` harvests current storage rows plus deleted rows for every dirty storage.
- `game-server/src/com/aionemu/gameserver/dao/InventoryDAO.java`
  - `store(player)` deletes dirty deleted rows through the same item snapshot flow.

## C# Changes

- Added `LegionWarehouseStoragePersistentState` and `DeletedLegionWarehouseItems`.
- Split flattened `InventoryItems` dirty tracking so location `0` cube rows and location `3` legion warehouse rows have separate storage states.
- Made `TrackDeletedItem` remove the item from the current modeled storage before adding the deleted copy, matching Java `Storage.delete`.
- Extended dirty harvesting and persistence cleanup to include the location `3` deleted queue.
- Added focused unit tests plus a DB-gated persistence test for deleting a legion warehouse row through periodic item save.

## Tests Added / Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GetDirtyItemsToUpdate_IncludesTrackedDeletedItemsAcrossModeledStorages` | Unit | `Player.getDirtyItemsToUpdate` + `Storage.delete` | Deleted cube, warehouse, account warehouse, and legion warehouse rows are harvested after current-row removal. | Focused C# state assertions derived from Java storage dirty harvest. | Does not send packets. |
| `GetDirtyItemsToUpdate_IncludesTrackedDeletedLegionWarehouseItemsLikeJavaStorageDelete` | Unit | `LegionStorageProxy.delete` -> `Storage.delete` | Location `3` deletes remove the current row, track a deleted copy, and harvest once. | Direct C# state assertion for the new live storage model. | Flattened C# model still lacks full `LegionWarehouse`. |
| `TrackDeletedItem_DropsNewItemsAsNoAction` | Unit | `Persistable.setPersistentState(DELETED)` for `NEW` rows | New rows deleted before save are removed without producing a DB delete. | Existing test updated to Java remove-then-no-op semantics. | Covers current model only. |
| `MarkDirtyItemsPersisted_ClearsTrackedDeletedLegionWarehouseItems` | Unit | `Player.getDirtyItemsToUpdate` post-store cleanup | Persisted location `3` deleted queue clears after snapshot save. | Focused C# state assertion. | Repository call is covered separately. |
| `SavePeriodicPlayerItemsAsync_DeletesTrackedLegionWarehouseRowsLikeJava_WhenEnabled` | Integration-gated | `InventoryDAO.store(player)` | A tracked location `3` deleted row is removed from `inventory` by periodic item save. | DB-gated schema contract; included in focused compile/test filter. | Skipped unless `AION_GAMESERVER_DB_INTEGRATION=1`. |

## Validation Decision

```text
- Changed surface: live player inventory storage state and persistence consumption for deleted rows.
- Specific behavior/contract: Java `Storage.delete` removes current rows, queues deleted rows, and `Player.getDirtyItemsToUpdate` exposes location `3` deletes for `InventoryDAO.store`.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerInventoryPersistentStateTests|FullyQualifiedName~ResolveInventoryStoreOwnerId_UsesLegionOwnerForLegionWarehouseLikeJava|FullyQualifiedName~SavePlayerLogoutAsync_PersistsLoadedLegionWarehouseRowsLikeJava|FullyQualifiedName~SavePeriodicPlayerItemsAsync_PersistsLoadedLegionWarehouseRowsLikeJava|FullyQualifiedName~SavePeriodicPlayerItemsAsync_DeletesTrackedLegionWarehouseRowsLikeJava" --logger "console;verbosity=minimal"
- Result: passed; 22 tests passed. Existing nullable/analyzer warnings were emitted outside this UOW.
- Focused Java/Maven command: not run; Java source was reviewed unchanged, and no narrow Java fixture exists for this storage-state branch in the checkout.
- Broad-validation trigger: shared live player inventory state and persistence path changed.
- Broad .NET decision: skipped; focused tests compiled the affected project and covered the edited player model plus adjacent repository snapshot behavior.
- Why this scope is sufficient: the change is isolated to dirty item state harvesting and its existing snapshot persistence consumer; the focused tests cover current-row removal, deleted queue harvest, cleanup, and the DB-gated delete contract.
```

Repository hygiene:

```powershell
git diff --check
```

Result: passed with only Git CRLF working-copy warnings for touched C# files.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.items.storage.Storage.delete` | `Player.TrackDeletedItem` | Runtime state mutation | Partial | Unit Tested | Partial Parity | C# now removes current rows before tracking deleted rows and handles location `3`; packet emission remains handled by live callers, not the model. |
| `com.aionemu.gameserver.model.items.storage.LegionStorageProxy.delete` | `Player.TrackDeletedItem` location-3 branch | Runtime state mutation | Partial | Unit Tested / Integration-gated | Partial Parity | Flattened C# location-3 rows now behave like a dirty legion warehouse storage for deletes; no full proxy aggregate exists. |
| `com.aionemu.gameserver.model.gameobjects.player.Player.getDirtyItemsToUpdate` | `Player.GetDirtyItemsToUpdate` | Runtime dirty harvest | Partial | Unit Tested | Partial Parity | Dirty harvest now includes location-3 current/deleted rows separately from cube rows. |
| `com.aionemu.gameserver.dao.InventoryDAO.store(Player)` | `MySqlPlayerEnterWorldRepository.SavePeriodicPlayerItemsAsync` | Repository save path | Partial | Integration-gated | Partial Parity | Existing snapshot save can now consume tracked location-3 deletes; DB execution was not run in this session. |

## Known Gaps

- C# still lacks Java's full `LegionWarehouse` aggregate and `LegionStorageProxy` object model.
- Location `3` delete packet emission is still the responsibility of live callers; this UOW only fixes state and persistence tracking.
- DB integration test is gated and was not run against a live MySQL fixture in this session.
- Full cached-legion periodic warehouse sweep from Java `PeriodicSaveService.LegionWarehouseSaveTask` is still not implemented.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported or extended in this UOW: 4
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 43%

## Next Runtime UOW Candidates

1. Discover whether live legion warehouse kinah handlers consume loaded location `3` kinah rows for open/deposit/withdraw flows; wire the smallest concrete state/persistence gap if found.
2. Discover whether a C# periodic cached-legion warehouse save service is needed beyond player-loaded location `3` rows; implement only if there is a live cached warehouse runtime structure to persist.
3. Revisit legion leave/kick/member-removal warehouse cleanup only after CM_LEGION or an equivalent membership-removal path becomes live.
