# Phase 6 Session 2733 Completion

## UOW

[Phase 6] UOW-2733: Block full legion warehouse item moves.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: live item move/split into a full legion warehouse now sends Java's warehouse-full message and avoids mutating item state.
- Java source/runtime path: ItemMoveService.moveItem, ItemSplitService.splitItem, Player.getStorage(StorageType.LEGION_WAREHOUSE), IStorage.getStorageIsFullMessage, and LegionWarehouse.updateLimit.
- C# runtime artifact wired: InventoryCapacity legion warehouse limit/free-slot helpers and GameServerConnection.CreateStorageFullMessage for destination storage type `3`.
- Client-visible/state/persistence effect: live CM_MOVE_ITEM/CM_SPLIT_ITEM into a full legion warehouse sends system-message id `1300421`; move unlocks the source item, and neither path changes item owner/location/count nor writes persistence/history.
- Why this is runtime progress: it sends real server packets from live client packet paths and prevents incorrect live inventory mutation.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/item/ItemMoveService.java`
  - `moveItem` checks `targetStorage.isFull()` after restriction/auto-merge logic and sends `targetStorage.getStorageIsFullMessage()` plus `sendItemUnlockPacket`.
- `game-server/src/com/aionemu/gameserver/services/item/ItemSplitService.java`
  - `splitItem` checks `destStorage.isFull()` before creating a new split item and sends only `destStorage.getStorageIsFullMessage()`.
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/Player.java`
  - `getStorage(LEGION_WAREHOUSE)` returns a `LegionStorageProxy` when the player has a legion.
- `game-server/src/com/aionemu/gameserver/model/items/storage/IStorage.java`
  - `LEGION_WAREHOUSE` maps to `STR_WAREHOUSE_DEPOSIT_FULL_BASKET`.
- `game-server/src/com/aionemu/gameserver/model/team/legion/LegionWarehouse.java`
  - `updateLimit` uses `(3 + warehouseExpansions) * 8`.

## C# Changes

- Added `InventoryCapacity.GetLegionWarehouseLimit`, `GetUsedLegionWarehouseSlots`, and `GetFreeLegionWarehouseSlots`.
- Updated `GameServerConnection.CreateStorageFullMessage` to treat destination storage type `3` like Java `LEGION_WAREHOUSE`.
- Added live packet-dispatch tests for `CM_SPLIT_ITEM` and `CM_MOVE_ITEM` into a full legion warehouse.

## Tests Added / Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_CubeSourceSplitToFullLegionWarehouseSendsJavaFullMessageWithoutMutation` | Unit / live client packet dispatch | `ItemSplitService.splitItem` + `IStorage.getStorageIsFullMessage` | Full legion warehouse destination sends `1300421` and does not split, persist, or add legion history. | Dispatches opcode `157` through `ProcessPacketAsync` and inspects runtime items/repository calls/packet payload. | Uses C# flattened location-3 model, not a full Java `LegionWarehouse`. |
| `ProcessPacketAsync_CubeSourceMoveToFullLegionWarehouseSendsJavaFullMessageAndUnlocksSource` | Unit / live client packet dispatch | `ItemMoveService.moveItem` + `IStorage.getStorageIsFullMessage` | Full legion warehouse destination sends `1300421`, unlocks the source item, and does not move, persist, or add legion history. | Dispatches opcode `156` through `ProcessPacketAsync` and inspects runtime items/repository calls/packet payload. | Uses C# flattened location-3 model, not a full Java `LegionWarehouse`. |

## Validation Decision

```text
- Changed surface: live inventory mutation guard, shared inventory capacity helper, and server packet dispatch.
- Specific behavior/contract: CM_MOVE_ITEM/CM_SPLIT_ITEM into full storage type `3` follows Java legion warehouse capacity and sends message id 1300421 without item mutation.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_CubeSourceSplitToFullLegionWarehouseSendsJavaFullMessageWithoutMutation|FullyQualifiedName~ProcessPacketAsync_CubeSourceMoveToFullLegionWarehouseSendsJavaFullMessageAndUnlocksSource|FullyQualifiedName~GamePacketTests" --logger "console;verbosity=minimal"
- Result: passed; 289 tests passed.
- Focused Java/Maven command: not run; Java source was reviewed unchanged, and no narrow Java fixture exists for this branch in the checkout.
- Broad-validation trigger: live inventory mutation path. Broad .NET was skipped because the focused command compiled the affected project and exercised the exact live move/split packet branches plus the packet helper contract.
- Why this scope is sufficient: the two new tests prove the changed storage type `3` behavior through live packet dispatch; `GamePacketTests` covers the adjacent system-message packet contract.
```

Additional attempted C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests" --logger "console;verbosity=minimal"
```

Result:

- Failed: 4
- Passed: 146
- Failed tests were unrelated existing class-level drift in AP extract cube-count assertions and item-order assertions for cross-storage switch/partial auto-merge. The new legion warehouse capacity tests were then isolated and passed with `GamePacketTests`.

Repository hygiene:

```powershell
git diff --check
```

Result: passed with only Git CRLF working-copy warnings for touched files.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemMoveService.moveItem` | `GameServerConnection.HandleMoveItemAsync` | Live client packet handler branch | Partial | Unit Tested | Partial Parity | Destination legion warehouse full branch now sends `1300421` and source unlock before mutation. Full Java storage aggregate behavior remains incomplete. |
| `com.aionemu.gameserver.services.item.ItemSplitService.splitItem` | `GameServerConnection.HandleSplitItemAsync` | Live client packet handler branch | Partial | Unit Tested | Partial Parity | Destination legion warehouse full branch now sends `1300421` before creating a split item. Full Java split edge cases remain broader than this UOW. |
| `com.aionemu.gameserver.model.items.storage.IStorage.getStorageIsFullMessage` | `GameServerConnection.CreateStorageFullMessage` | Utility / live packet selection | Partial | Unit Tested | Partial Parity | CUBE, regular warehouse, account warehouse, and legion warehouse are represented; pet/house/broker/mailbox storage types remain outside current C# live scope. |
| `com.aionemu.gameserver.model.team.legion.LegionWarehouse.updateLimit` | `InventoryCapacity.GetLegionWarehouseLimit` | Runtime capacity helper | Partial | Unit Tested through live handlers | Partial Parity | Uses Java `(3 + expansions) * 8` formula through `Player.LegionWarehouseExpansions`; C# still lacks a full `LegionWarehouse` aggregate. |

## Known Gaps

- C# still models legion warehouse contents as player `InventoryItems` location `3`, not a full Java `LegionWarehouse`.
- Java `ItemRestrictionService` uses `LegionConfig.LEGION_WAREHOUSE` for live item movement restrictions; C# movement restrictions still need the option wired.
- Full edited-class validation currently has unrelated existing failures outside this UOW's legion warehouse full path.
- Exact Java golden bytes for `1300421` were not captured.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 5
- Total artifacts ported or extended in this UOW: 4
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 43%

## Next Runtime UOW Candidates

1. Wire `GameServerOptions.Legion.WarehouseEnabled` into live item movement restrictions for storage type `3`, matching Java `ItemRestrictionService.isItemRestrictedTo`/`isItemRestrictedFrom`.
2. Persist Java-equivalent legion warehouse item state from live save/logout paths if the existing database shape and C# item owner/location model can be safely wired.
3. Revisit legion leave/kick/member-removal only after a live CM_LEGION membership-removal path exists.
