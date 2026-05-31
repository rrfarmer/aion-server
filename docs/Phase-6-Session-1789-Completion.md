# Phase 6 Session 1789 Completion - Explicit Modeled Storage Dirty-State

Date: 2026-05-30
Unit of Work: UOW-1789
Status: Complete

## Scope

Port the minimum explicit storage-state surface for the currently modeled player-owned storages so dirty harvest is keyed off modeled storage dirtiness instead of rescanning item rows, while keeping the scope inside `Player`, the logout persistence boundary, and focused tests.

## Completed Work

- Updated `dotnetConversion/src/Aion.GameServer/Model/GameObjects/Player.cs`:
  - added `StoragePersistentState`
  - added explicit modeled storage-state properties for cube, warehouse, and account warehouse
  - added `MarkStorageDirty(int location)`
  - updated the three modeled storage collections to promote explicit storage dirtiness when dirty rows are assigned
  - updated `GetDirtyItemsToUpdate()` to harvest only `UpdateRequired` modeled storages and reset those flags to `Updated` after harvest
  - updated `TrackDeletedItem` and `MarkDirtyItemsPersisted()` to participate in the explicit storage-state lifecycle
- Updated focused parity evidence in:
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerInventoryPersistentStateTests.cs`

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerInventoryPersistentStateTests|FullyQualifiedName~PlayerEnterWorldRepositoryDatabaseIntegrationTests|FullyQualifiedName~PlayerEnterWorldServiceTests"`
- `dotnet test dotnetConversion\AionServer.slnx`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerProtectionActiveTaskScheduledTaskHandleAdapterTests.Cancel_ForwardsToScheduledTaskCancelAndMarksHandleDone"`
- `dotnet test dotnetConversion\AionServer.slnx`

Result:

- Focused storage-dirty/logout validation passed with 46 tests.
- The first full-suite attempt hit the command timeout boundary before completion.
- The second full-suite attempt failed in unrelated `PlayerProtectionActiveTaskScheduledTaskHandleAdapterTests.Cancel_ForwardsToScheduledTaskCancelAndMarksHandleDone`.
- That test then passed in isolation with 1 test.
- The third full-suite attempt passed cleanly with 4766 total tests:
  - `57` commons
  - `29` chat
  - `121` login
  - `4559` game

## Java Artifacts Reviewed

- `com.aionemu.gameserver.model.gameobjects.player.Player.getDirtyItemsToUpdate`
- `com.aionemu.gameserver.model.items.storage.Storage`
- `com.aionemu.gameserver.dao.InventoryDAO`

## Migration Parity Table - UOW-1789

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.items.storage.Storage.persistentState` + `Storage.setPersistentState` for cube / regular warehouse / account warehouse | `Aion.GameServer.Model.GameObjects.StoragePersistentState` + `Player.InventoryStoragePersistentState` + `WarehouseStoragePersistentState` + `AccountWarehouseStoragePersistentState` + `MarkStorageDirty` | Modeled Storage Dirty-State Surface | Partial | Unit Tested | Partial Parity | C# now exposes explicit modeled dirty-state for the currently represented player-owned storages, but it still lives on `Player` rather than on first-class storage objects. |
| `com.aionemu.gameserver.model.gameobjects.player.Player.getDirtyItemsToUpdate` explicit storage-state gate and reset behavior | `Aion.GameServer.Model.GameObjects.Player.GetDirtyItemsToUpdate` | Dirty-State Harvest | Partial | Unit Tested | Partial Parity | C# dirty harvest now uses explicit modeled storage-state and resets that state after harvest, but still lacks the Java equipment branch. |
| `com.aionemu.gameserver.model.items.storage.Storage.delete(Item, ...)` dirty-storage side effect | `Aion.GameServer.Model.GameObjects.Player.TrackDeletedItem` | Deleted-Row Dirty-State Bridge | Partial | Unit Tested | Partial Parity | Tracked deletes now also mark the touched modeled storage dirty. |
| `com.aionemu.gameserver.dao.InventoryDAO.store(Player)` storage-driven harvest boundary | `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository.SavePlayerLogoutAsync` + `Player.GetDirtyItemsToUpdate` | Logout Persistence Boundary | Partial | Regression Tested | Partial Parity | Logout persistence now consumes a storage-state-driven harvest for the modeled storages. |

## Tests Added

| Test Name | What It Validates | Java-Equivalent Evidence | Test Type | Limitations |
|---|---|---|---|---|
| `MarkStorageDirty_HarvestsAllCurrentRowsEvenWhenTheyAreUpdated` | Explicitly dirty modeled storage harvests all current rows and resets its storage flag after harvest. | Java `Storage.setPersistentState` + `Player.getDirtyItemsToUpdate` source | Unit | No equipment branch coverage |
| `AssigningDirtyRows_PromotesTheModeledStorageState` | Dirty assigned rows promote explicit modeled storage state to `UpdateRequired`. | Java storage dirty-state intent + item dirty-row participation source | Unit | No broader producer sweep |
| `GetDirtyItemsToUpdate_ReturnsDirtyItemsAcrossModeledStorages` | Dirty modeled storages still harvest all current rows and reset explicit storage-state flags after harvest. | Java `Player.getDirtyItemsToUpdate` source | Unit | No pet bag / cabinet / legion storage coverage |
| `GetDirtyItemsToUpdate_IncludesTrackedDeletedItemsAcrossModeledStorages` | Dirty modeled storages still emit tracked deleted rows while using the explicit storage-state gate. | Java `Player.getDirtyItemsToUpdate` + `Storage.getDeletedItems` source | Unit | No equipment/delete-queue interaction proof |

## Risks / Gaps

- C# still does not port first-class Java `Storage` objects; modeled storage dirtiness still lives on `Player`.
- Java `equipment.getPersistentState()` and the equipped-item harvest branch are still absent.
- Many live mutation/removal producers still rely on assignment-driven promotion rather than direct storage-object mutations.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped rows in this unit.
- Total artifacts ported: 1 modeled storage-state enum, 3 explicit storage-state properties, 1 dirty-marking helper, 1 dirty-harvest gate/reset update, and 4 focused test updates/additions.
- Total artifacts with verified parity: 0 grouped rows.
- Total artifacts needing verification: 4 grouped rows.
- Total blocked artifacts: equipment persistent-state participation, broader live storage dirty/deleted producers, and a fuller Java-shaped inventory store pipeline.
- Estimated overall migration completion: Phase 6 remains about 72%.

## Next Recommended Unit of Work

- Port the minimum modeled `Equipment.persistentState` participation so `Player.getDirtyItemsToUpdate()` can include equipped-item harvest through a Java-shaped equipment dirty gate.
- Safe alternatives if a different isolated slice is preferred:
  - execute the opt-in MySQL logout delete/retuning persistence path in an environment with `AION_GAMESERVER_DB_INTEGRATION=1`
  - `CraftService.finishCrafting` product selection
  - `DropRegistrationService.calculateBoostDropRate`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/Player.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerInventoryPersistentStateTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1789-Completion.md`
- `docs/Phase-6-Session-1789-Handoff.md`
