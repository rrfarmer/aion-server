# Phase 6 Session 2415 Completion - Logout Dirty Storage Persistence

## Scope
- Audited Java dirty item collection during logout persistence.
- Added focused C# regression coverage for dirty/deleted cube, warehouse, and account-warehouse rows being collected before dirty tracking is cleared.
- Kept changes test-only; no production persistence behavior changed.

## Java Source Reviewed
- `game-server/src/com/aionemu/gameserver/services/player/PlayerLeaveWorldService.java`
  - `leaveWorld(Player player)`
- `game-server/src/com/aionemu/gameserver/services/player/PlayerService.java`
  - `storePlayer(Player player)`
- `game-server/src/com/aionemu/gameserver/dao/InventoryDAO.java`
  - `store(Player player)`
  - `store(List<Item> items, Integer playerId, Integer accountId, Integer legionId)`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/Player.java`
  - `getDirtyItemsToUpdate()`
- `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
  - `getItemsWithKinah()`
  - `getDeletedItems()`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/Persistable.java`
  - `PersistentState`

## Implemented
- Added `LeaveWorld_PersistsDirtyStorageRowsBeforeClearingLikeJavaInventoryStore`.
- Extended `PlayerEnterWorldServiceTests.CapturingEnterWorldRepository` with opt-in dirty-item capture and persist simulation for logout.
- Added a small test helper for storage-location row creation using Java/C# persisted storage ids:
  - cube: `0`
  - warehouse: `1`
  - account warehouse: `2`

## Behavior Pinned
- Java `InventoryDAO.store(player)` calls `player.getDirtyItemsToUpdate()`.
- Java `Player.getDirtyItemsToUpdate()` includes all current rows plus deleted rows for storages whose persistent state is `UPDATE_REQUIRED`, then marks those storages `UPDATED`.
- C# `Player.GetDirtyItemsToUpdate()` mirrors that storage-level behavior for the currently modeled cube, warehouse, and account warehouse lists.
- C# `Player.MarkDirtyItemsPersisted()` normalizes item persistent state and clears deleted-row tracking after the repository has persisted the dirty snapshot.

## Migration Parity Table
| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.player.PlayerLeaveWorldService` | `Aion.GameServer.Services.PlayerEnterWorldService` | Logout service | Partial | Regression Tested | Partial Parity | Logout delegates persistence to the repository; this UOW pins dirty storage row visibility at the service/repository boundary. Full logout ordering remains partial. |
| `com.aionemu.gameserver.services.player.PlayerService` | `Aion.GameServer.Data.PlayerEnterWorldRepository` | Persistence service/repository | Partial | Regression Tested | Partial Parity | C# repository owns player logout persistence, including dirty item collection and cleanup. Java `PlayerService.storePlayer` has more stored subsystems than currently modeled. |
| `com.aionemu.gameserver.dao.InventoryDAO` | `Aion.GameServer.Data.PlayerEnterWorldRepository` | Repository | Partial | Unit Tested / Regression Tested | Partial Parity | Dirty/deleted item state collection is covered through service-boundary tests and existing item-stone row tests; full database mutation parity still needs opt-in integration evidence when SQL changes. |
| `com.aionemu.gameserver.model.gameobjects.player.Player` | `Aion.GameServer.Model.GameObjects.Player` | Model | Partial | Regression Tested | Partial Parity | `GetDirtyItemsToUpdate()` and `MarkDirtyItemsPersisted()` are pinned for cube/warehouse/account-warehouse current and deleted rows. Pet bags, cabinets, and Java account object ownership are not modeled. |
| `com.aionemu.gameserver.model.items.storage.Storage` | `Aion.GameServer.Model.GameObjects.Player` | Storage model | Partial | Regression Tested | Partial Parity | C# has no `Storage` wrapper, but the modeled storage lists include current plus deleted rows when dirty. Kinah inclusion depends on item rows already present in the C# list model. |
| `com.aionemu.gameserver.model.gameobjects.Persistable` | `Aion.GameServer.Model.GameObjects.InventoryItemPersistentState` | State enum | Partial | Regression Tested | Partial Parity | NEW / UPDATE_REQUIRED / UPDATED / DELETED / NOACTION transition behavior is represented for modeled inventory items. |

## Tests Updated
| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `LeaveWorld_PersistsDirtyStorageRowsBeforeClearingLikeJavaInventoryStore` | Regression | Java `InventoryDAO.store(player)` and `Player.getDirtyItemsToUpdate()`. | Logout repository capture sees current and deleted dirty rows from cube, warehouse, and account warehouse before dirty tracking is cleared. | Focused service-boundary test using Java storage location ids and C# dirty tracking APIs. | Does not hit live MySQL SQL; repository database integration remains opt-in. |

## Validation Decision
- Changed surface: test-only around logout persistence and model dirty-state behavior.
- Specific behavior/contract: logout persistence gathers dirty/deleted cube, warehouse, and account-warehouse rows before clearing dirty tracking.
- Focused C# command: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~PlayerEnterWorldRepositoryItemStonePersistenceTests" --no-restore`
- Focused Java/Maven command: skipped; no targeted Java fixture exists and no Java source changed.
- Broad-validation trigger: none; no production persistence code changed.
- Broad .NET decision: skipped because the test-only change was validated with the edited service tests and adjacent item persistence helper tests.
- Why this scope is sufficient: the new regression drives the service/repository boundary and uses the same C# dirty-state APIs the production repository calls.

## Validation Result
- First focused run failed to compile because the test helper referenced private C# `Player.StorageLocation`.
- Fixed the test helper to use persisted storage ids directly.
- Rerun:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~PlayerEnterWorldRepositoryItemStonePersistenceTests" --no-restore`
  - Passed: 45 tests, 0 failed, 0 skipped.
  - Pre-existing nullable/analyzer warnings were emitted during the compile-producing run.

## Known Remaining Gaps
- Live MySQL logout item mutation parity was not re-run because production repository SQL did not change.
- C# does not model Java pet bags, cabinets, or a storage owner wrapper.
- C# account warehouse behavior remains list-based; Java account object ownership remains unmodeled.
- Full Java logout persistence ordering still has unpinned subsystems outside item dirty/deleted state.

## Summary Metrics
- Total Java artifacts reviewed in this UOW: 6.
- Total C# artifacts changed in this UOW: 1.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification / partial parity: 6.
- Total blocked artifacts: 0.
- Estimated overall migration completion: Phase 6 remains in progress; no estimate change.
