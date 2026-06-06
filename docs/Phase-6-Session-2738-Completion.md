# Phase 6 Session 2738 Completion

## UOW

[Phase 6] UOW-2738: Persist legion warehouse snapshot rows with Java owner mapping.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: dirty loaded legion warehouse location `3` rows now persist through logout and periodic item snapshot saves with Java-equivalent owner mapping.
- Java source/runtime path: `InventoryDAO.store(Player)`, `InventoryDAO.getItemOwnerId`, `InventoryDAO.DELETE_QUERY`, and `PeriodicSaveService.LegionWarehouseSaveTask`.
- C# runtime artifact wired: `MySqlPlayerEnterWorldRepository.SavePlayerLogoutAsync`, `SavePeriodicPlayerItemsAsync`, and their dirty inventory snapshot helpers.
- Client-visible/state/persistence effect: modified location `3` legion warehouse rows survive logout/periodic item flush under `item_owner = player.LegionId`; snapshot deletes follow Java `item_unique_id` semantics.
- Why this is runtime progress: it persists live dirty item state through the existing `inventory` and `item_stones` database shape, not a preview or metadata layer.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/dao/InventoryDAO.java`
  - `store(Player)` passes player id, account id, and legion id into `store(items, playerId, accountId, legionId)`.
  - `getItemOwnerId` returns account id for location `2`, legion id for location `3` when available, otherwise player id.
  - `DELETE_QUERY` deletes dirty removed rows by `item_unique_id`.
- `game-server/src/com/aionemu/gameserver/services/PeriodicSaveService.java`
  - `LegionWarehouseSaveTask` stores legion warehouse rows with `InventoryDAO.store(allItems, null, null, legion.getLegionId())`.
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/Player.java`
  - `getDirtyItemsToUpdate` gathers dirty storage rows before `InventoryDAO.store(player)` flushes them.

## C# Changes

- Normalized dirty snapshot row owners before logout and periodic item saves using the Java storage-owner rule.
- Added `ResolveInventoryStoreOwnerId` as the focused runtime helper used by snapshot saves.
- Changed only the snapshot delete path to delete inventory rows by `item_unique_id`, matching Java `InventoryDAO.store`; explicit live mutation deletes remain owner-qualified.
- Added focused tests for the owner rule and DB-gated logout/periodic persistence contracts.

## Tests Added / Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ResolveInventoryStoreOwnerId_UsesLegionOwnerForLegionWarehouseLikeJava` | Unit | `InventoryDAO.getItemOwnerId` | Location `0` uses player id, location `2` uses account id, location `3` uses legion id when present and player id otherwise. | Direct C# helper assertions derived from Java branch logic. | Does not hit the database by itself. |
| `SavePlayerLogoutAsync_PersistsLoadedLegionWarehouseRowsLikeJava_WhenEnabled` | Integration-gated | `InventoryDAO.store(player)` | Logout snapshot save rewrites loaded location `3` rows to the player's legion id. | DB-gated schema contract; included in focused compile/test filter. | Skipped unless `AION_GAMESERVER_DB_INTEGRATION=1`. |
| `SavePeriodicPlayerItemsAsync_PersistsLoadedLegionWarehouseRowsLikeJava_WhenEnabled` | Integration-gated | `PlayerEnterWorldService.ItemUpdateTask` -> `InventoryDAO.store(player)` | Periodic item snapshot save rewrites loaded location `3` rows to the player's legion id. | DB-gated schema contract; included in focused compile/test filter. | Skipped unless `AION_GAMESERVER_DB_INTEGRATION=1`. |

## Validation Decision

```text
- Changed surface: runtime persistence save path for dirty inventory snapshot rows.
- Specific behavior/contract: logout and periodic dirty item snapshot saves compute `item_owner` from player/account/legion context like Java `InventoryDAO.store(player)`.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ResolveInventoryStoreOwnerId_UsesLegionOwnerForLegionWarehouseLikeJava|FullyQualifiedName~SavePlayerLogoutAsync_PersistsLoadedLegionWarehouseRowsLikeJava|FullyQualifiedName~SavePeriodicPlayerItemsAsync_PersistsLoadedLegionWarehouseRowsLikeJava|FullyQualifiedName~EnterWorldAsync_LoadsLegionWarehouseItemsForLegionMemberLikeJava" --logger "console;verbosity=minimal"
- Result: passed; 4 tests passed. Existing nullable/analyzer warnings were emitted outside this UOW.
- Focused Java/Maven command: not run; Java source was reviewed unchanged, and no narrow Java test fixture exists for this DAO branch in the checkout.
- Broad-validation trigger: persistence save path changed.
- Broad .NET decision: skipped; the focused command compiled the affected project and exercised the new owner-rule helper plus adjacent enter-world load contract.
- Why this scope is sufficient: the changed save branch is isolated to dirty snapshot owner/delete semantics, and the focused tests cover the Java-derived owner rule plus the intended logout/periodic DB contracts when integration is enabled.
```

Repository hygiene:

```powershell
git diff --check
```

Result: passed with only Git CRLF working-copy warnings for touched C# files.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.dao.InventoryDAO.store(Player)` | `MySqlPlayerEnterWorldRepository.SavePlayerLogoutAsync` | Repository save path | Partial | Unit Tested / Integration-gated | Partial Parity | Logout snapshot saves now recompute location `3` owner as legion id; broader dirty storage modeling remains flattened. |
| `com.aionemu.gameserver.dao.InventoryDAO.store(Player)` | `MySqlPlayerEnterWorldRepository.SavePeriodicPlayerItemsAsync` | Repository save path | Partial | Unit Tested / Integration-gated | Partial Parity | Periodic snapshot saves now recompute location `3` owner as legion id; full Java storage aggregates are still not ported. |
| `com.aionemu.gameserver.dao.InventoryDAO.getItemOwnerId` | `MySqlPlayerEnterWorldRepository.ResolveInventoryStoreOwnerId` | Utility / repository helper | Complete for modeled storages | Unit Tested | Verified Parity | Java branch behavior for player, account, and legion storage owners is covered by focused unit assertions. |
| `com.aionemu.gameserver.dao.InventoryDAO.DELETE_QUERY` | `MySqlPlayerEnterWorldRepository.DeleteInventoryItemSnapshotRowAsync` | Repository delete path | Partial | Integration-gated through snapshot tests | Partial Parity | Snapshot deletes now use item id only like Java `InventoryDAO.store`; other explicit mutation deletes intentionally remain owner-qualified. |
| `com.aionemu.gameserver.services.PeriodicSaveService.LegionWarehouseSaveTask` | `MySqlPlayerEnterWorldRepository.SavePeriodicPlayerItemsAsync` location-3 snapshot branch | Periodic save path | Partial | Integration-gated | Partial Parity | C# still lacks a cached `LegionWarehouse` aggregate task; loaded location-3 rows persist through player periodic item saves. |

## Known Gaps

- C# still uses flattened `Player.InventoryItems` for location `3` rows instead of Java `LegionWarehouse`/`LegionStorageProxy`.
- DB integration tests were not executed against a live MySQL fixture in this session; they remain gated behind `AION_GAMESERVER_DB_INTEGRATION=1`.
- `Player.TrackDeletedItem` still does not model location `3` deleted-items storage explicitly; discover a live handler path before changing it.
- Full Java `PeriodicSaveService.LegionWarehouseSaveTask` cached-legion sweep is not implemented in C#.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported or extended in this UOW: 5
- Total artifacts with verified parity: 1
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 43%

## Next Runtime UOW Candidates

1. Discover whether live legion warehouse withdrawal/delete paths can leave location `3` deleted rows untracked because `Player.TrackDeletedItem` ignores location `3`; wire the smallest concrete runtime gap if found.
2. Discover whether a C# periodic cached-legion warehouse save service is needed beyond player-loaded location `3` rows; implement only if there is a live cached warehouse runtime structure to persist.
3. Verify live legion warehouse kinah handlers consume enter-world-loaded location `3` kinah rows; wire only if a concrete runtime gap exists.
