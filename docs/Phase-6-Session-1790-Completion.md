# Phase 6 Session 1790 Completion - Modeled Equipment Dirty-State Harvest Split

Date: 2026-05-30
Unit of Work: UOW-1790
Status: Complete

## Scope

Port the minimum modeled Java equipment dirty-state participation so logout harvest distinguishes non-equipped cube rows from equipped rows, while keeping the scope inside `Player`, the modeled dirty-harvest path, and focused tests.

## Completed Work

- Updated `dotnetConversion/src/Aion.GameServer/Model/GameObjects/Player.cs`:
  - added `EquipmentPersistentState`
  - added `MarkEquipmentDirty()`
  - split `InventoryItems` assignment promotion between non-equipped cube rows and equipped rows
  - updated `GetDirtyItemsToUpdate()` so cube storage harvest excludes equipped rows
  - added a separate modeled equipment harvest branch keyed off `EquipmentPersistentState`
  - updated `MarkDirtyItemsPersisted()` to reset the equipment dirty flag too
- Updated focused parity evidence in:
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerInventoryPersistentStateTests.cs`

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerInventoryPersistentStateTests|FullyQualifiedName~PlayerEnterWorldRepositoryDatabaseIntegrationTests|FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~EquipmentServiceTests"`
- `dotnet test dotnetConversion\AionServer.slnx`

Result:

- Focused persistent-state/equipment validation passed with 83 tests.
- Full-suite validation passed cleanly on the first run with 4769 total tests:
  - `57` commons
  - `29` chat
  - `121` login
  - `4562` game

## Java Artifacts Reviewed

- `com.aionemu.gameserver.model.gameobjects.player.Player.getDirtyItemsToUpdate`
- `com.aionemu.gameserver.model.gameobjects.player.Equipment`
- `com.aionemu.gameserver.dao.InventoryDAO`

## Migration Parity Table - UOW-1790

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.gameobjects.player.Equipment.persistentState` + `Equipment.setPersistentState` | `Aion.GameServer.Model.GameObjects.Player.EquipmentPersistentState` + `MarkEquipmentDirty` | Modeled Equipment Dirty-State Surface | Partial | Unit Tested | Partial Parity | C# now has an explicit modeled equipment dirty flag, but it still lives on `Player` rather than on a first-class `Equipment` object. |
| `com.aionemu.gameserver.model.gameobjects.player.Player.getDirtyItemsToUpdate` equipment branch | `Aion.GameServer.Model.GameObjects.Player.GetDirtyItemsToUpdate` | Dirty-State Harvest | Partial | Unit Tested | Partial Parity | Equipped rows are now harvested through a separate modeled equipment branch and the equipment flag resets after harvest. |
| `com.aionemu.gameserver.model.items.storage.Storage.getItemsWithKinah` cube-storage participation excluding equipped rows | `Aion.GameServer.Model.GameObjects.Player.GetDirtyItemsToUpdate` modeled cube-storage harvest | Cube Storage Harvest Separation | Partial | Unit Tested | Partial Parity | Cube harvest now excludes equipped rows, bringing the modeled cube-storage branch closer to Java inventory storage behavior. |
| `com.aionemu.gameserver.dao.InventoryDAO.store(Player)` inventory-plus-equipment harvest boundary | `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository.SavePlayerLogoutAsync` + `Player.GetDirtyItemsToUpdate` | Logout Persistence Boundary | Partial | Regression Tested | Partial Parity | Logout persistence now consumes a harvest that distinguishes modeled storage rows from equipped rows. |

## Tests Added

| Test Name | What It Validates | Java-Equivalent Evidence | Test Type | Limitations |
|---|---|---|---|---|
| `GetDirtyItemsToUpdate_HarvestsDirtyEquippedItemsThroughEquipmentState` | Dirty equipped rows are harvested through the modeled equipment branch rather than the cube-storage branch. | Java `Player.getDirtyItemsToUpdate` + `Equipment.getEquippedItems` source | Unit | No first-class `Equipment` object proof |
| `MarkEquipmentDirty_HarvestsEquippedRowsEvenWhenTheyAreUpdated` | Explicitly dirty modeled equipment harvests all equipped rows and resets the equipment flag after harvest. | Java `Equipment.setPersistentState` + `Player.getDirtyItemsToUpdate` source | Unit | No broader runtime producer sweep |
| `AssigningDirtyEquippedRows_PromotesEquipmentStateWithoutDirtyingInventoryStorage` | Dirty equipped rows promote modeled equipment state without incorrectly dirtying cube storage. | Java inventory/equipment separation source | Unit | No pet bag / cabinet coverage |
| `GetDirtyItemsToUpdate_ReturnsDirtyItemsAcrossModeledStorages` | Equipped rows no longer leak into the modeled cube-storage harvest. | Java `Player.getDirtyItemsToUpdate` source | Unit | No legion warehouse coverage |

## Risks / Gaps

- C# still does not port a first-class Java `Equipment` object; modeled equipment dirtiness still lives on `Player`.
- Most live equipment mutation paths still rely on assignment-driven promotion rather than explicit equipment-state mutation calls.
- Pet bag, cabinet, and legion warehouse harvest branches remain unmodeled.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped rows in this unit.
- Total artifacts ported: 1 modeled equipment-state property, 1 equipment dirty-marking helper, 1 inventory-vs-equipment harvest split update, and 4 focused test updates/additions.
- Total artifacts with verified parity: 0 grouped rows.
- Total artifacts needing verification: 4 grouped rows.
- Total blocked artifacts: broader live equipment dirty producers, first-class storage/equipment container modeling, and a fuller Java-shaped inventory store pipeline.
- Estimated overall migration completion: Phase 6 remains about 72%.

## Next Recommended Unit of Work

- Port the minimum live producer sweep for modeled `EquipmentPersistentState`, starting with the current direct `EquipmentService` and power-shard mutation surfaces so equipment dirtiness is not inferred only from reassignment.
- Safe alternatives if a different isolated slice is preferred:
  - execute the opt-in MySQL logout delete/retuning persistence path in an environment with `AION_GAMESERVER_DB_INTEGRATION=1`
  - `CraftService.finishCrafting` product selection
  - `DropRegistrationService.calculateBoostDropRate`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/Player.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerInventoryPersistentStateTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1790-Completion.md`
- `docs/Phase-6-Session-1790-Handoff.md`
