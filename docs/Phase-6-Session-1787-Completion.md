# Phase 6 Session 1787 Completion - Modeled Deleted Item Tracking

Date: 2026-05-30
Unit of Work: UOW-1787
Status: Complete

## Scope

Port the minimum Java `Storage.deletedItems` behavior needed for the currently modeled player-owned storages so deleted inventory rows can survive long enough for the logout persistence boundary to flush them, without broadening into a full storage abstraction or full `InventoryDAO.store(player)` rewrite.

## Completed Work

- Updated `dotnetConversion/src/Aion.GameServer/Model/GameObjects/Player.cs`:
  - added modeled deleted-item collections for:
    - cube
    - warehouse
    - account warehouse
  - added `TrackDeletedItem`
  - updated `GetDirtyItemsToUpdate()`
  - updated `MarkDirtyItemsPersisted()`
- Updated `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`:
  - `SavePlayerLogoutAsync` now flushes tracked deleted rows before snapshot-updating current rows
  - added `DeleteInventoryItemSnapshotAsync`
  - relaxed the internal delete helper transaction parameter to nullable for the logout delete path
- Added focused coverage:
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerInventoryPersistentStateTests.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldRepositoryDatabaseIntegrationTests.cs`

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerInventoryPersistentStateTests|FullyQualifiedName~PlayerEnterWorldRepositoryDatabaseIntegrationTests|FullyQualifiedName~PlayerEnterWorldServiceTests"`
- `dotnet test dotnetConversion\AionServer.slnx`

Result:

- Focused deleted-item and logout validation passed with 42 tests.
- Full-suite validation passed cleanly on the first run with 4762 total tests:
  - `57` commons
  - `29` chat
  - `121` login
  - `4555` game

## Java Artifacts Reviewed

- `com.aionemu.gameserver.model.items.storage.Storage`
- `com.aionemu.gameserver.model.gameobjects.player.Player.getDirtyItemsToUpdate`
- `com.aionemu.gameserver.dao.InventoryDAO`
- `com.aionemu.gameserver.services.player.PlayerLeaveWorldService`
- `com.aionemu.gameserver.services.player.PlayerService`

## Migration Parity Table - UOW-1787

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.items.storage.Storage.deletedItems` + `Storage.delete(Item, ...)` | `Aion.GameServer.Model.GameObjects.Player.TrackDeletedItem` + modeled deleted-item collections | Deleted-Row Tracking | Partial | Unit Tested | Partial Parity | C# now tracks deleted rows for the currently modeled player-owned storages and preserves the Java `NEW -> DELETED => NOACTION` behavior, but queue ownership remains player-side rather than a full storage abstraction. |
| `com.aionemu.gameserver.model.gameobjects.player.Player.getDirtyItemsToUpdate` deleted-item participation | `Aion.GameServer.Model.GameObjects.Player.GetDirtyItemsToUpdate` + `MarkDirtyItemsPersisted` | Dirty-State Harvest | Partial | Unit Tested | Partial Parity | C# dirty harvest now includes tracked deleted rows and clears them after the persistence boundary. Storage-level and equipment-level dirtiness gates remain missing. |
| `com.aionemu.gameserver.dao.InventoryDAO.store(List<Item>, ...)` delete branch | `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository.SavePlayerLogoutAsync` + `DeleteInventoryItemSnapshotAsync` | Logout Delete Flush | Partial | Regression Tested | Partial Parity | C# logout now flushes tracked deleted rows through the existing inventory delete helper before snapshot-updating current rows, but it is still a delete-only slice rather than the full Java filtered store pipeline. |
| `com.aionemu.gameserver.services.player.PlayerLeaveWorldService.leaveWorld` + `PlayerService.storePlayer` deleted-item persistence boundary | `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository.SavePlayerLogoutAsync` | Logout Persistence Boundary | Partial | Regression Tested | Partial Parity | The logout path now covers both tracked deletes and current-row snapshot updates for the modeled storages. Java `ItemStoneListDAO.save(player)` and broader storage filtering remain future work. |

## Tests Added

| Test Name | What It Validates | Java-Equivalent Evidence | Test Type | Limitations |
|---|---|---|---|---|
| `GetDirtyItemsToUpdate_IncludesTrackedDeletedItemsAcrossModeledStorages` | Dirty harvest includes tracked deleted rows for cube, warehouse, and account warehouse. | Java `Player.getDirtyItemsToUpdate` + `Storage.getDeletedItems` source | Unit | No equipment/storage-state coverage |
| `TrackDeletedItem_DropsNewItemsAsNoAction` | Deleting a newly created modeled item does not queue a persisted delete row. | Java `Storage.delete` + `Item.setPersistentState` source | Unit | No broader runtime producer proof |
| `MarkDirtyItemsPersisted_ClearsTrackedDeletedItems` | Persist-reset clears modeled deleted-row queues after the save boundary. | Java `Player.getDirtyItemsToUpdate` post-store behavior | Unit | No storage-level `PersistentState` reset proof |
| `SavePlayerLogoutAsync_DeletesTrackedInventoryRowsAgainstJavaSchema_WhenEnabled` | Logout persistence removes a tracked deleted row from the Java inventory schema. | Java `PlayerLeaveWorldService.leaveWorld` + `PlayerService.storePlayer` + `InventoryDAO.store(player)` delete path | Integration | Opt-in only; requires `AION_GAMESERVER_DB_INTEGRATION=1` |

## Risks / Gaps

- Java storage-level `PersistentState` is still absent in C#.
- Only the player surface and logout boundary know about modeled deleted rows so far.
- Current inventory inserts/updates still use snapshot persistence rather than a Java-shaped filtered save pipeline.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped rows in this unit.
- Total artifacts ported: 3 modeled deleted-item collections, 1 deleted-row tracking helper, 1 logout delete-flush helper, and 4 focused tests.
- Total artifacts with verified parity: 0 grouped rows.
- Total artifacts needing verification: 4 grouped rows.
- Total blocked artifacts: storage-level persistent-state participation, broader live deleted-row producers, and a full filtered inventory store pipeline.
- Estimated overall migration completion: Phase 6 remains about 72%.

## Next Recommended Unit of Work

- Port the minimum storage-level dirty gate needed for the modeled storages so current-row updates and tracked deleted rows can be harvested more like Java `Storage.getPersistentState() == UPDATE_REQUIRED`.
- Safe alternatives if a different isolated slice is preferred:
  - execute the opt-in MySQL logout delete/retuning persistence path in an environment with `AION_GAMESERVER_DB_INTEGRATION=1`
  - `CraftService.finishCrafting` product selection
  - `DropRegistrationService.calculateBoostDropRate`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/Player.cs`
- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerInventoryPersistentStateTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldRepositoryDatabaseIntegrationTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1787-Completion.md`
- `docs/Phase-6-Session-1787-Handoff.md`
