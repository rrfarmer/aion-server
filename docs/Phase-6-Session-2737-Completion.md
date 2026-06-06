# Phase 6 Session 2737 Completion

## UOW

[Phase 6] UOW-2737: Load legion warehouse rows on enter-world.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: a legion member entering world now has existing legion warehouse location `3` inventory rows restored into C# runtime state for live warehouse handlers.
- Java source/runtime path: `LegionService.loadLegionInfo` calls `InventoryDAO.loadStorage(legionId, legion.getLegionWarehouse())`, and `Player.getStorage(StorageType.LEGION_WAREHOUSE)` returns a `LegionStorageProxy` for legion members.
- C# runtime artifact wired: `IPlayerEnterWorldRepository`, `MySqlPlayerEnterWorldRepository.LoadLegionWarehouseItemsAsync`, and `PlayerEnterWorldService.EnterWorldAsync`.
- Client-visible/state/persistence effect: after enter-world, live C# move/split/kinah warehouse handlers can find existing legion-owned items/kinah by object id and location `3`; no packet is sent solely for loading.
- Why this is runtime progress: it restores runtime item state from the existing database shape for live warehouse handlers.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
  - `loadLegionInfo` loads the legion warehouse using `InventoryDAO.loadStorage(legion.getLegionId(), legion.getLegionWarehouse())`.
  - It then loads item stones for `legion.getLegionWarehouse().getItems()`.
- `game-server/src/com/aionemu/gameserver/model/team/legion/Legion.java`
  - A `Legion` owns a `LegionWarehouse` aggregate.
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/Player.java`
  - `getStorage(LEGION_WAREHOUSE)` returns a `LegionStorageProxy` when the player has a legion.

## C# Changes

- Added `LoadLegionWarehouseItemsAsync(int legionId, ...)` to `IPlayerEnterWorldRepository`.
- Implemented MySQL loading for inventory rows with `item_owner = legionId` and `item_location = 3`.
- Wired `PlayerEnterWorldService.EnterWorldAsync` to load legion warehouse rows when `player.LegionId > 0`.
- Appended loaded location-3 rows into `player.InventoryItems`, matching the existing C# live handlers that use `InventoryItems` for storage type `3`.
- Updated repository fakes and added focused enter-world coverage.

## Tests Added / Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `EnterWorldAsync_LoadsLegionWarehouseItemsForLegionMemberLikeJava` | Unit / runtime load path | `LegionService.loadLegionInfo` + `Player.getStorage(LEGION_WAREHOUSE)` | Legion members load location `3` rows by legion id and expose them in runtime `InventoryItems` for live handlers. | Invokes `EnterWorldAsync`, checks loaded legion id, cube item preservation, and runtime location-3 item state. | Uses C# flattened location-3 model, not a full Java `LegionWarehouse` aggregate. |
| `HandleMoveItemAsync_LegionWarehouseSameStorageSlotPersistsWithLegionOwnerLikeJava` | Unit / adjacent live handler persistence | `InventoryDAO.getItemOwnerId` | Adjacent live handler still sees storage type `3` as legion-owned runtime state. | Focused validation kept the prior live storage-owner branch green. | Does not prove all warehouse handlers consume loaded rows. |

## Validation Decision

```text
- Changed surface: runtime enter-world load path and repository interface.
- Specific behavior/contract: legion members load existing location `3` inventory rows with `item_owner = player.LegionId` into runtime state.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~EnterWorldAsync_LoadsLegionWarehouseItemsForLegionMemberLikeJava|FullyQualifiedName~HandleMoveItemAsync_LegionWarehouseSameStorageSlotPersistsWithLegionOwnerLikeJava" --logger "console;verbosity=minimal"
- Result: passed; 2 tests passed.
- Focused Java/Maven command: not run; Java source was reviewed unchanged, and no narrow Java fixture exists for this load branch in the checkout.
- Broad-validation trigger: runtime load path and repository interface touched.
- Broad .NET decision: skipped; focused tests compiled the affected project and proved the new load path plus adjacent live storage consumption.
- Why this scope is sufficient: the new repository contract is exercised through `EnterWorldAsync`, and the adjacent live handler test proves storage type `3` remains consumable by live movement code.
```

Repository hygiene:

```powershell
git diff --check
```

Result: passed with only Git CRLF working-copy warnings for touched C# files.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.LegionService.loadLegionInfo` | `PlayerEnterWorldService.EnterWorldAsync` | Runtime load path | Partial | Unit Tested | Partial Parity | C# now restores legion-owned location-3 rows for legion members; full Legion aggregate caching/history loading remains broader. |
| `com.aionemu.gameserver.dao.InventoryDAO.loadStorage` | `MySqlPlayerEnterWorldRepository.LoadLegionWarehouseItemsAsync` | Repository load path | Partial | Unit Tested through service fake | Partial Parity | Loads rows by legion id/location 3 through the existing storage loader; exact DB integration for this branch was not run. |
| `com.aionemu.gameserver.model.gameobjects.player.Player.getStorage` | `Player.InventoryItems` location-3 runtime model | Runtime storage access | Partial | Unit Tested | Partial Parity | C# still uses flattened `InventoryItems` for location `3`, but live handlers can now access restored rows. |

## Known Gaps

- C# still lacks a full Java `LegionWarehouse` aggregate and `LegionStorageProxy`.
- Exact DB integration for `LoadLegionWarehouseItemsAsync` was not run; the SQL shape reuses the existing `LoadStorageItemsAsync` helper.
- Enter-world loads location `3` rows for any positive `LegionId`; deeper Java disband/cache semantics remain outside this UOW.
- Periodic/logout save behavior for loaded legion warehouse rows still needs focused discovery and validation.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 3
- Total artifacts ported or extended in this UOW: 4
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 3
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 43%

## Next Runtime UOW Candidates

1. Discover and fix periodic/logout save behavior for loaded location `3` legion warehouse rows if C# currently misses them.
2. Verify live legion warehouse kinah handlers consume enter-world-loaded location `3` kinah rows; wire only if a concrete runtime gap exists.
3. Revisit legion leave/kick/member-removal warehouse cleanup only after CM_LEGION or an equivalent membership-removal path becomes live.
