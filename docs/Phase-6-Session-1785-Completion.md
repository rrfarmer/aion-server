# Phase 6 Session 1785 Completion - Inventory Dirty-State Tracking

Date: 2026-05-30
Unit of Work: UOW-1785
Status: Complete

## Scope

Port the smallest honest Java dirty-item lifecycle slice needed by the live identify and reidentify work: add modeled item persistent state, add player-level dirty-item harvest for the currently represented storages, and wire the existing identify/reidentify mutation outputs into that state without claiming a full Java storage-state port.

## Completed Work

- Updated `dotnetConversion/src/Aion.GameServer/Model/GameObjects/InventoryItem.cs`:
  - added `InventoryItemPersistentState`
  - added `InventoryItem.PersistentState`
- Updated `dotnetConversion/src/Aion.GameServer/Model/GameObjects/Player.cs`:
  - added `GetDirtyItemsToUpdate()`
  - added `MarkDirtyItemsPersisted()`
  - limited dirty-item harvest to the currently modeled storages:
    - `InventoryItems`
    - `WarehouseItems`
    - `AccountWarehouseItems`
- Updated runtime mutation planners:
  - `dotnetConversion/src/Aion.GameServer/Services/IdentifyItemExecutionPlanService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/TuneResultApplicationPlanService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/TuningActionExecutionPlanService.cs`
  - identify completion and accepted reidentify application now mark the item `UpdateRequired`
  - tune-count consumption now marks the item `UpdateRequired`
  - attribute-preview creation preserves the existing `PersistentState`
- Updated logout persistence cleanup:
  - `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
  - `SavePlayerLogoutAsync` now calls `player.MarkDirtyItemsPersisted()` after the modeled snapshot flush completes
- Added focused dirty-state coverage:
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerInventoryPersistentStateTests.cs`

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerInventoryPersistentStateTests|FullyQualifiedName~IdentifyItemExecutionPlanServiceTests|FullyQualifiedName~TuneResultApplicationPlanServiceTests|FullyQualifiedName~TuningActionExecutionPlanServiceTests|FullyQualifiedName~PlayerEnterWorldRepositoryDatabaseIntegrationTests|FullyQualifiedName~PlayerEnterWorldServiceTests"`
- `dotnet test dotnetConversion\AionServer.slnx`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleUseItemAsync_ExpExtractMergesRestrictedRewardWithCleanupSealFlag"`
- `dotnet test dotnetConversion\AionServer.slnx`

Result:

- Focused dirty-state, retuning, and logout validation passed with 43 tests.
- The first full-suite run failed in `GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_ExpExtractMergesRestrictedRewardWithCleanupSealFlag`.
- That failing test then passed immediately in isolation with 1 test.
- The second full-suite rerun passed cleanly with 4752 total tests:
  - `57` commons
  - `29` chat
  - `121` login
  - `4545` game

## Java Artifacts Reviewed

- `com.aionemu.gameserver.model.gameobjects.Persistable`
- `com.aionemu.gameserver.model.gameobjects.Item`
- `com.aionemu.gameserver.model.items.storage.Storage`
- `com.aionemu.gameserver.model.gameobjects.player.Equipment`
- `com.aionemu.gameserver.model.gameobjects.player.Player.getDirtyItemsToUpdate`
- `com.aionemu.gameserver.dao.InventoryDAO`
- `com.aionemu.gameserver.services.player.PlayerService`
- `com.aionemu.gameserver.services.player.PlayerLeaveWorldService`
- `com.aionemu.gameserver.services.item.ItemActionService`
- `com.aionemu.gameserver.model.templates.item.actions.TuningAction`

## Migration Parity Table - UOW-1785

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.gameobjects.Persistable.PersistentState` + `com.aionemu.gameserver.model.gameobjects.Item.setPersistentState` | `Aion.GameServer.Model.GameObjects.InventoryItemPersistentState` | Item Dirty-State Model | Partial | Regression Tested | Partial Parity | C# now models the item-level persistent-state enum, but it does not yet reproduce all Java state-transition rules. |
| `com.aionemu.gameserver.model.gameobjects.player.Player.getDirtyItemsToUpdate` | `Aion.GameServer.Model.GameObjects.Player.GetDirtyItemsToUpdate` + `MarkDirtyItemsPersisted` | Dirty-State Harvest | Partial | Regression Tested | Partial Parity | C# now harvests and normalizes dirty items for the modeled storages, but Java storage/equipment state and deleted-item side lists remain unported. |
| `com.aionemu.gameserver.services.item.ItemActionService.identifyItem` + `com.aionemu.gameserver.services.item.ItemActionService.applyTuneResult` + `com.aionemu.gameserver.model.templates.item.actions.TuningAction.act` dirty-item lifecycle | `Aion.GameServer.Services.IdentifyItemExecutionPlanService` + `TuneResultApplicationPlanService` + `TuningActionExecutionPlanService` | Runtime Mutation Dirty Marking | Partial | Regression Tested | Partial Parity | C# now marks the current identify and accepted reidentify item mutations dirty and keeps attribute-preview creation non-dirty. |
| `com.aionemu.gameserver.services.player.PlayerLeaveWorldService.leaveWorld` + `com.aionemu.gameserver.services.player.PlayerService.storePlayer` post-save state normalization | `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository.SavePlayerLogoutAsync` + `Player.MarkDirtyItemsPersisted` | Logout Dirty-State Reset | Partial | Regression Tested | Partial Parity | C# now resets modeled dirty item state after the logout snapshot flush, but the underlying write path is still snapshot-based rather than Java dirty-row filtered. |

## Tests Added

| Test Name | What It Validates | Java-Equivalent Evidence | Test Type | Limitations |
|---|---|---|---|---|
| `GetDirtyItemsToUpdate_ReturnsDirtyItemsAcrossModeledStorages` | Dirty-item harvest returns only modeled items marked `New`, `UpdateRequired`, or `Deleted` across cube, warehouse, and account warehouse. | Java `Player.getDirtyItemsToUpdate` source | Unit | No equipment participation or deleted-side-list coverage |
| `MarkDirtyItemsPersisted_NormalizesDirtyItemsToUpdated` | Persisted modeled items normalize back to `Updated` without losing their data. | Java `Player.getDirtyItemsToUpdate` post-store normalization | Unit | No direct Java `Storage` state reset proof |

## Risks / Gaps

- Java `Storage`/`Equipment` persistent state and deleted-item side lists are still absent in C#.
- `InventoryItemPersistentState` does not yet implement Java `Item.setPersistentState` transition semantics for every state combination.
- Logout persistence still flushes full snapshots rather than a Java-shaped filtered `InventoryDAO.store(player)` path.
- Stronger parity evidence for the broader persistence lifecycle still depends on either a narrower filtered-save port or more DB-backed proof.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped rows in this unit.
- Total artifacts ported: 1 item persistent-state enum, 2 player dirty-state helpers, 3 runtime dirty-marking updates, and 2 focused unit tests.
- Total artifacts with verified parity: 0 grouped rows.
- Total artifacts needing verification: 4 grouped rows.
- Total blocked artifacts: Java storage/equipment dirty-state participation, delete-side-list behavior, and Java-shaped filtered persistence writes.
- Estimated overall migration completion: Phase 6 remains about 72%.

## Next Recommended Unit of Work

- Port the narrow Java `Item.setPersistentState` transition rules plus storage/equipment dirty-state participation needed to make `Player.getDirtyItemsToUpdate` source-shaped for the modeled storages.
- Safe alternatives if a different isolated slice is preferred:
  - execute the opt-in MySQL logout persistence test path in an environment with `AION_GAMESERVER_DB_INTEGRATION=1`
  - `CraftService.finishCrafting` product selection
  - `DropRegistrationService.calculateBoostDropRate`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/InventoryItem.cs`
- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/Player.cs`
- `dotnetConversion/src/Aion.GameServer/Services/IdentifyItemExecutionPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/TuneResultApplicationPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/TuningActionExecutionPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerInventoryPersistentStateTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1785-Completion.md`
- `docs/Phase-6-Session-1785-Handoff.md`
