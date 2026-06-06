# Phase 6 Session 2718 Completion

## UOW

[Phase 6] UOW-2718: Move cube items into legion warehouse.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: live CM_MOVE_ITEM cube -> LEGION_WAREHOUSE now proceeds past the previous successful-legion deferred return when Java restriction checks allow it.
- Java source/runtime path: CM_MOVE_ITEM.runImpl -> ItemMoveService.moveItem -> ItemRestrictionService.isItemRestrictedTo/isItemRestrictedFrom -> LegionService.addWHItemHistory -> InventoryDAO.getItemOwnerId -> ItemPacketService delete/add fanout.
- C# runtime artifact wired: GameServerConnection.HandleMoveItemAsync, GetMoveStorageOwnerId, CreateStorageSizePacket, PlayerEnterWorldService.SaveItemCrossStorageMoveMutationAsync, MySqlPlayerEnterWorldRepository.SaveItemCrossStorageMoveMutationAsync.
- Client-visible/state/persistence effect: an allowed legion warehouse deposit mutates the item to location 3 with legion owner id, persists item_owner/item_location/slot using the existing inventory table shape, sends source delete/cube update, and sends legion warehouse add/cube update.
- Why this is runtime progress: it mutates live inventory state, persists/restores through the existing database row shape, and sends real server packets from a live client packet handler.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_MOVE_ITEM.java`
  - Dispatches item object id, source storage, destination storage, and slot to `ItemMoveService.moveItem`.
- `game-server/src/com/aionemu/gameserver/services/item/ItemMoveService.java`
  - After restriction/trading/shutdown checks, successful cross-storage moves call `LegionService.addWHItemHistory` for legion warehouse involvement, remove from source, send delete, set slot, and add to target.
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/Player.java`
  - `getStorage(LEGION_WAREHOUSE)` returns a `LegionStorageProxy` when the player has a legion.
- `game-server/src/com/aionemu/gameserver/model/items/storage/LegionStorageProxy.java`
  - Delegates item mutation to the legion warehouse storage while preserving actor context.
- `game-server/src/com/aionemu/gameserver/dao/InventoryDAO.java`
  - `getItemOwnerId` writes `item_location == LEGION_WAREHOUSE` rows under the legion id when available.
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_CUBE_UPDATE.java`
  - Legion warehouse size packets use `player.getLegion().getLegionWarehouse().size()` and warehouse expansions.

## C# Changes

- `HandleMoveItemAsync` now returns only when the legion restriction branch denies the move; otherwise the Java-equivalent cross-storage mutation path runs.
- `GetMoveStorageOwnerId` maps destination storage 3 to `player.LegionId`, matching Java row ownership for legion warehouse items.
- Cross-storage persistence methods now accept and pass `legionId`, and `MySqlPlayerEnterWorldRepository.GetStorageOwnerId` uses it for location 3 rows.
- Legion warehouse size fanout now emits the current modeled legion item count instead of a zero placeholder.
- `SaveItemStorageSwitchMutationAsync` also receives `legionId` so the same DB owner rule is available to the next successful replace UOW.

## Tests Added / Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_CubeSourceMovesItemToLegionWarehouseOwnerLikeJava` | Unit / live packet dispatch | `CM_MOVE_ITEM`, `ItemMoveService.moveItem`, `InventoryDAO.getItemOwnerId`, `SM_CUBE_UPDATE` source review | Live move dispatch with legion deposit permission mutates item location/slot/owner, records legion id for persistence, and sends delete plus legion warehouse add packets. | Socket-backed connection fixture, live `ProcessPacketAsync`, decoded packet assertions, fake repository owner-id capture. | `LegionService.addWHItemHistory` is still not modeled; exact Java runtime bytes were not captured. |
| Existing legion warehouse denial tests | Unit / live packet dispatch | `ItemPacketService.sendItemUnlockPacket`, `SM_CUBE_UPDATE` source review | Denial/unlock packets now report modeled legion warehouse item count instead of zero placeholder. | Decoded packet assertions for move/split/replace denial. | Warehouse expansions remain zero until live legion expansion data is modeled. |

## Validation Decision

```text
- Changed surface: live CM_MOVE_ITEM successful legion warehouse deposit, storage owner persistence contract, and legion warehouse size packet helper.
- Specific behavior/contract: allowed cube -> LEGION_WAREHOUSE move mutates item owner/location/slot, records legion id for DB owner selection, and sends Java-shaped delete/add storage packets; adjacent denial/unlock paths still send correct packets.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_CubeSourceMovesItemToLegionWarehouseOwnerLikeJava|FullyQualifiedName~ProcessPacketAsync_MoveItemFromLegionWarehouseWithoutWithdrawalSendsNoRightLikeJava|FullyQualifiedName~ProcessPacketAsync_SplitItemFromLegionWarehouseWithoutWithdrawalSendsNoRightLikeJava|FullyQualifiedName~ProcessPacketAsync_ReplaceItemFromLegionWarehouseWithoutWithdrawalSendsNoRightLikeJava" --logger "console;verbosity=minimal"
- Result: passed; 4 tests passed.
- Focused Java/Maven command: not run; Java source was reviewed unchanged, and no narrow Java fixture exists for this branch in this checkout.
- Broad-validation trigger: none. Although this touches live dispatch and persistence owner selection, the changed behavior is isolated to item movement storage owner mapping and directly exercised by filtered live packet tests.
- Broad .NET decision: skipped; the filtered command built affected projects and validated the edited live move path plus directly adjacent legion warehouse unlock fanout.
- Why this scope is sufficient: tests prove the live mutation, packet sequence, repository owner-id contract, rollback-sensitive save call, and the shared legion warehouse size packet helper for the scoped branches.
```

Repository hygiene:

```powershell
git diff --check
```

Result: passed with only Git CRLF working-copy warnings for touched files.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_MOVE_ITEM` | `GameServerConnection.HandleMoveItemAsync` | Live client packet handler | Partial | Unit Tested | Partial Parity | Successful cube -> legion warehouse deposit is wired; source-legion successful withdrawal and merge/history gaps remain. |
| `com.aionemu.gameserver.services.item.ItemMoveService.moveItem` | `GameServerConnection.HandleMoveItemAsync` | Service / live item move | Partial | Unit Tested | Partial Parity | Restriction and one successful legion deposit path run through live code; Java `LegionService.addWHItemHistory` remains missing. |
| `com.aionemu.gameserver.dao.InventoryDAO` | `MySqlPlayerEnterWorldRepository` | Repository | Partial | Unit Tested indirectly | Partial Parity | Location 3 row owner selection now uses legion id when available; direct DB integration for legion rows was not run. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE` | `SmCubeUpdate` / `CreateStorageSizePacket` | Packet fanout | Partial | Unit Tested indirectly | Partial Parity | Legion warehouse item count is modeled from live location 3 items; warehouse expansions remain zero until legion data lands. |

## Known Gaps

- `LegionService.addWHItemHistory` / legion history persistence and fanout remain unmodeled.
- C# still stores modeled legion warehouse items in `Player.InventoryItems` with `Location = 3` rather than a dedicated shared legion warehouse aggregate.
- Successful source-legion withdrawal and replace/split success paths need additional scoped UOWs.
- Legion warehouse expansion count is not loaded, so size packet expansion fields remain zero.
- Exact Java packet bytes for the successful move sequence were not captured.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 6
- Total artifacts ported or extended in this UOW: 5
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 5
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 43%

## Next Runtime UOW Candidates

1. Wire successful source-legion-warehouse withdrawal to cube using the new legion owner persistence path and existing permission checks.
2. Wire successful cube/account/warehouse to legion replacement once storage switch owner mapping is covered by a focused live test.
3. Scope `LegionService.addWHItemHistory` persistence/fanout if the existing legion history table/repository shape can be wired without preview scaffolding.
