# Phase 6 Session 1788 Completion - Dirty Storage Harvest Scope

Date: 2026-05-30
Unit of Work: UOW-1788
Status: Complete

## Scope

Port the Java dirty-storage harvest shape for the currently modeled player-owned storages so a dirty storage emits all current rows plus its tracked deleted rows, and make the logout persistence boundary consume that modeled harvest rather than snapshotting every current row unconditionally.

## Completed Work

- Updated `dotnetConversion/src/Aion.GameServer/Model/GameObjects/Player.cs`:
  - `GetDirtyItemsToUpdate()` now emits all current rows from a modeled dirty storage
  - tracked deleted rows are still included for the same dirty storage
- Updated `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`:
  - `SavePlayerLogoutAsync` now computes a dirty harvest once
  - deletes tracked deleted rows first
  - updates only the non-deleted rows from that dirty harvest
- Added or updated focused tests:
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerInventoryPersistentStateTests.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldRepositoryDatabaseIntegrationTests.cs`

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerInventoryPersistentStateTests|FullyQualifiedName~PlayerEnterWorldRepositoryDatabaseIntegrationTests|FullyQualifiedName~PlayerEnterWorldServiceTests"`
- `dotnet test dotnetConversion\AionServer.slnx`

Result:

- Focused dirty-storage/logout validation passed with 44 tests.
- Full-suite validation passed cleanly with 4764 total tests:
  - `57` commons
  - `29` chat
  - `121` login
  - `4557` game

## Java Artifacts Reviewed

- `com.aionemu.gameserver.model.gameobjects.player.Player.getDirtyItemsToUpdate`
- `com.aionemu.gameserver.model.items.storage.Storage`
- `com.aionemu.gameserver.dao.InventoryDAO`
- `com.aionemu.gameserver.services.player.PlayerLeaveWorldService`
- `com.aionemu.gameserver.services.player.PlayerService`

## Migration Parity Table - UOW-1788

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.gameobjects.player.Player.getDirtyItemsToUpdate` current-row harvest behavior | `Aion.GameServer.Model.GameObjects.Player.GetDirtyItemsToUpdate` | Dirty-State Harvest | Partial | Unit Tested | Partial Parity | C# now emits all current rows from a modeled dirty storage plus tracked deleted rows, but explicit storage/equipment state objects remain missing. |
| `com.aionemu.gameserver.model.items.storage.Storage.getItemsWithKinah` participation under a dirty storage | `Aion.GameServer.Model.GameObjects.Player.GetDirtyItemsToUpdate` modeled storage harvest | Current-Row Storage Harvest | Partial | Unit Tested | Partial Parity | C# now includes companion current rows from the same dirty storage rather than only the individually dirty row. |
| `com.aionemu.gameserver.dao.InventoryDAO.store(List<Item>, ...)` filtered current-row update scope | `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository.SavePlayerLogoutAsync` | Logout Save-Set Filter | Partial | Regression Tested | Partial Parity | C# logout now writes only non-deleted rows from the modeled dirty harvest rather than every current modeled row. |
| `com.aionemu.gameserver.services.player.PlayerLeaveWorldService.leaveWorld` + `PlayerService.storePlayer` modeled dirty-storage save boundary | `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository.SavePlayerLogoutAsync` | Logout Persistence Boundary | Partial | Regression Tested | Partial Parity | The logout path now combines modeled deleted-row flushing with dirty-storage-scoped current-row updates. |

## Tests Added

| Test Name | What It Validates | Java-Equivalent Evidence | Test Type | Limitations |
|---|---|---|---|---|
| `GetDirtyItemsToUpdate_ReturnsAllCurrentRowsWhenOneStorageItemIsDirty` | One dirty row in a modeled storage causes all current rows from that storage to be harvested. | Java `Player.getDirtyItemsToUpdate` + `Storage.getItemsWithKinah` source | Unit | No equipment storage coverage |
| `GetDirtyItemsToUpdate_ReturnsDirtyItemsAcrossModeledStorages` | Dirty modeled storages emit all current rows rather than only individually dirty rows. | Java `Player.getDirtyItemsToUpdate` source | Unit | No explicit storage-state object proof |
| `GetDirtyItemsToUpdate_IncludesTrackedDeletedItemsAcrossModeledStorages` | A dirty modeled storage emits both current rows and tracked deleted rows. | Java `Player.getDirtyItemsToUpdate` + `Storage.getDeletedItems` source | Unit | No equipment/delete-queue interaction proof |
| `SavePlayerLogoutAsync_WritesAllCurrentRowsFromDirtyInventoryStorageAgainstJavaSchema_WhenEnabled` | Logout persistence writes companion current rows from a dirty cube storage. | Java `InventoryDAO.store(player)` dirty-storage current-row scope | Integration | Opt-in only; requires `AION_GAMESERVER_DB_INTEGRATION=1` |

## Risks / Gaps

- C# still infers dirty-storage participation from item state and deleted queues instead of exposing first-class Java storage `PersistentState`.
- Only the logout boundary currently consumes the modeled dirty harvest.
- Broader live producers still need to feed or preserve modeled dirty/deleted state consistently.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped rows in this unit.
- Total artifacts ported: 1 modeled storage-dirty harvest update, 1 logout save-set filter update, and 4 focused test updates/additions.
- Total artifacts with verified parity: 0 grouped rows.
- Total artifacts needing verification: 4 grouped rows.
- Total blocked artifacts: explicit storage-state modeling, equipment storage participation, broader live producers, and a fuller filtered inventory store pipeline.
- Estimated overall migration completion: Phase 6 remains about 72%.

## Next Recommended Unit of Work

- Port the minimum explicit storage-state surface for the modeled storages so dirty-storage participation is no longer inferred solely from item states and deleted queues.
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
- `docs/Phase-6-Session-1788-Completion.md`
- `docs/Phase-6-Session-1788-Handoff.md`
