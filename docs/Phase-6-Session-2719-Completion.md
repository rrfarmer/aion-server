# Phase 6 Session 2719 Completion

## UOW

[Phase 6] UOW-2719: Replace cube and legion warehouse items.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: live CM_REPLACE_ITEM cube <-> LEGION_WAREHOUSE now proceeds past the previous successful-legion deferred return when Java restriction checks allow it.
- Java source/runtime path: CM_REPLACE_ITEM.runImpl -> ItemMoveService.switchItemsInStorages -> ItemRestrictionService.isItemRestrictedFrom/isItemRestrictedTo -> Player.getStorage -> LegionStorageProxy -> InventoryDAO.getItemOwnerId -> ItemPacketService delete/add fanout.
- C# runtime artifact wired: GameServerConnection.HandleReplaceItemAsync, SaveItemStorageSwitchMutationAsync owner selection from UOW-2718, SmDeleteItem/SmDeleteWarehouseItem/SmInventoryAddItem/SmWarehouseAddItem fanout.
- Client-visible/state/persistence effect: an allowed cube and legion warehouse replacement swaps both live items' locations/owners/slots, persists the storage switch using the legion id for location 3, and sends delete/add packets for both affected storages.
- Why this is runtime progress: it mutates live inventory state, persists through the existing inventory row shape, and sends real server packets from the live replace handler.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_REPLACE_ITEM.java`
  - Dispatches source and replacement storage/object ids to `ItemMoveService.switchItemsInStorages`.
- `game-server/src/com/aionemu/gameserver/services/item/ItemMoveService.java`
  - `switchItemsInStorages` performs source/replacement restriction checks, swaps slots, sends deletes for both old storages, and adds each item to the opposite storage.
- `game-server/src/com/aionemu/gameserver/services/item/ItemRestrictionService.java`
  - Legion warehouse source requires `WH_WITHDRAWAL`; legion warehouse destination requires a storable item and either deposit or withdrawal permission.
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/Player.java`
  - `getStorage(LEGION_WAREHOUSE)` returns a `LegionStorageProxy` when the player is in a legion.
- `game-server/src/com/aionemu/gameserver/model/items/storage/LegionStorageProxy.java`
  - Delegates storage mutation to the shared legion warehouse while retaining player context.
- `game-server/src/com/aionemu/gameserver/dao/InventoryDAO.java`
  - Location 3 rows are owned by the player's legion id when available.

## C# Changes

- `HandleReplaceItemAsync` now returns from the legion warehouse restriction branch only when Java-equivalent permission checks deny the replace.
- Allowed cube <-> legion warehouse replacements now fall through to the existing live storage switch path.
- The existing storage switch path updates both item owners/locations/slots, persists via `SaveItemStorageSwitchMutationAsync`, and sends the Java-shaped delete/add packet sequence for cube and warehouse storage types.
- The dedicated Java `LegionStorageProxy` / `LegionWarehouse` aggregate is still not modeled; C# continues representing scoped legion warehouse rows as `Player.InventoryItems` with `Location = 3`.

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_CubeAndLegionWarehouseReplaceSwitchesOwnersLikeJava` | Unit / live packet dispatch | `CM_REPLACE_ITEM`, `ItemMoveService.switchItemsInStorages`, `ItemRestrictionService`, `InventoryDAO.getItemOwnerId` source review | Live replace dispatch with withdrawal permission swaps cube and legion items, uses player/legion owners, records the legion id in the persistence contract, and emits delete/add packet fanout for both storages. | Socket-backed connection fixture, live `ProcessPacketAsync`, decoded packet assertions, fake repository owner-id capture. | Dedicated shared legion warehouse storage and exact Java packet bytes were not captured. |

## Validation Decision

```text
- Changed surface: live CM_REPLACE_ITEM successful legion warehouse replacement branch.
- Specific behavior/contract: allowed cube <-> LEGION_WAREHOUSE replace mutates both items' owner/location/slot state, records player/account/legion owner context for persistence, and sends Java-shaped delete/add packets for source and replacement storages.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_CubeAndLegionWarehouseReplaceSwitchesOwnersLikeJava|FullyQualifiedName~ProcessPacketAsync_ReplaceItemFromLegionWarehouseWithoutWithdrawalSendsNoRightLikeJava|FullyQualifiedName~ProcessPacketAsync_CubeSourceMovesItemToLegionWarehouseOwnerLikeJava" --logger "console;verbosity=minimal"
- Result: passed; 3 tests passed.
- Focused Java/Maven command: not run; Java source was reviewed unchanged, and no narrow Java fixture exists for this branch in this checkout.
- Broad-validation trigger: none. The changed behavior is isolated to a previously deferred successful replace branch and is directly exercised through live packet dispatch.
- Broad .NET decision: skipped; the filtered command built affected projects and validated the edited replace branch plus adjacent denial/deposit coverage.
- Why this scope is sufficient: tests prove live mutation, packet sequence, and repository owner-id contract for the scoped replace path.
```

Repository hygiene:

```powershell
git diff --check
```

Result: passed with only Git CRLF working-copy warnings for touched files.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CM_REPLACE_ITEM.runImpl` | `GameServerConnection.HandleReplaceItemAsync` | Live packet handler | Partial | Unit Tested | Partial Parity | Allowed cube <-> legion warehouse replacement is wired; exact Java bytes and shared legion storage remain gaps. |
| `ItemMoveService.switchItemsInStorages` | `GameServerConnection.HandleReplaceItemAsync` | Item storage switch service | Partial | Unit Tested | Partial Parity | Restriction ordering and successful switch mutation are represented for the scoped branch. |
| `InventoryDAO.getItemOwnerId` | `MySqlPlayerEnterWorldRepository.GetStorageOwnerId` | Repository helper | Partial | Unit Tested indirectly | Partial Parity | The replace path now records the legion id for storage 3 owner selection; direct DB integration was not run. |

## Known Gaps

- C# still models legion warehouse rows inside `Player.InventoryItems` rather than a shared `LegionStorageProxy` / `LegionWarehouse` aggregate.
- Successful legion warehouse split remains deferred behind an explicit return in `HandleSplitItemAsync`.
- `LegionService.addWHItemHistory` remains unmodeled for Java move/split paths that call it.
- Legion warehouse expansion counts are not loaded, so size packet expansion fields remain zero.
- Exact Java packet bytes for the successful replace sequence were not captured.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 6
- Total artifacts ported or extended in this UOW: 3
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 3
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 43%

## Next Runtime UOW Candidates

1. Wire successful `CM_SPLIT_ITEM` cube -> legion warehouse split using Java `ItemSplitService.splitItem`, including live count mutation/new item owner mapping and packet fanout.
2. Wire successful `CM_SPLIT_ITEM` legion warehouse -> cube split/merge after scoping source-owner persistence and rollback behavior.
3. Implement live legion warehouse history persistence for move/split if the existing schema can be wired directly without preview scaffolding.
