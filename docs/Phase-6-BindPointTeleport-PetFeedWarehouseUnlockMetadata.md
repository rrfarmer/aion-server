# Phase 6 - Pet Feed Warehouse Unlock Metadata

Date: May 27, 2026
Unit of Work: UOW-1329

## Scope

This unit extends the rejected-food unlock metadata boundary from normal cube storage into Java's regular and account warehouse paths.

Java source of truth:

- `com.aionemu.gameserver.services.item.ItemPacketService.sendItemUnlockPacket`
- `com.aionemu.gameserver.services.item.ItemPacketService.sendStorageUpdatePacket`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_WAREHOUSE_ADD_ITEM`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE`
- `com.aionemu.gameserver.model.items.storage.StorageType`

## Implemented

- Added `SmWarehouseAddItem` for the Java `SM_WAREHOUSE_ADD_ITEM` packet body.
- Preserved Java packet order: warehouse type byte, add mask, item count, object id, template id, info byte, l10n name, full item blob, equipment slot.
- Added `SmWarehouseAddItem.AllSlot = 0x13` through the existing `SmInventoryAddItem.AllSlot` constant.
- Added `SmCubeUpdate.RegularWarehouseSizeSnapshot(...)`.
- Extended `PetFeedPacketMetadataBridge` to construct non-sending rejected-food unlock metadata for:
  - regular warehouse: `SmWarehouseAddItem` then `SmCubeUpdate.RegularWarehouseSizeSnapshot(...)`
  - account warehouse: `SmWarehouseAddItem` then `SmCubeUpdate.AccountWarehouseSize()`
- Kept legion warehouse blocked because Java routes legion-warehouse kinah through `SM_LEGION_EDIT(0x04, legion)` and needs a separate item-vs-kinah and legion snapshot boundary.

## Not Implemented

- No live storage lookup from `item.getItemLocation()`.
- No live warehouse mutation.
- No live packet sends.
- No legion warehouse unlock metadata.
- No Java runtime packet byte comparison.
- No client UI validation.

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "PetFeedPacketMetadataBridge|SmWarehouseAddItem|SmCubeUpdate"` passed 10 tests.

## Migration Parity Table - UOW-1329

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.serverpackets.SM_WAREHOUSE_ADD_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmWarehouseAddItem` | Server Packet | Partial | Unit Tested | Partial Parity | Implements source-derived packet body for supplied item/template snapshots. No Java runtime byte comparison exists. Item blob parity depends on existing `SmInventoryInfo.WriteItemInfoBlob`. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendStorageUpdatePacket` regular warehouse branch | `PetFeedPacketMetadataBridge.ConstructWarehouseItemUnlock` + `SmWarehouseAddItem` + `SmCubeUpdate.RegularWarehouseSizeSnapshot` | Packet Metadata Composition | Partial | Unit Tested | Partial Parity | Builds non-sending `SM_WAREHOUSE_ADD_ITEM` then regular-warehouse `SM_CUBE_UPDATE` metadata using supplied post-mutation count/expand snapshots. Live storage mutation and sends are disabled. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendStorageUpdatePacket` account warehouse branch | `PetFeedPacketMetadataBridge.ConstructWarehouseItemUnlock` + `SmWarehouseAddItem` + `SmCubeUpdate.AccountWarehouseSize` | Packet Metadata Composition | Partial | Unit Tested | Partial Parity | Builds non-sending account warehouse add packet and Java-shaped zero cube-update payload for account warehouse. Live account storage hydration is not wired. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE.cubeSize(StorageType.REGULAR_WAREHOUSE, Player)` | `Aion.GameServer.Network.Aion.ServerPackets.SmCubeUpdate.RegularWarehouseSizeSnapshot` | Server Packet Helper | Partial | Unit Tested | Partial Parity | Preserves action `0`, ordinal `1`, supplied item count, warehouse NPC expands, and warehouse bonus expands. Snapshot correctness remains caller-owned. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE.cubeSize(StorageType.ACCOUNT_WAREHOUSE, Player)` | `Aion.GameServer.Network.Aion.ServerPackets.SmCubeUpdate.AccountWarehouseSize` | Server Packet Helper | Partial | Unit Tested | Partial Parity | Confirms Java fall-through behavior: action `0`, ordinal `2`, zero count/expands. No live player/account comparison. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_EDIT` legion warehouse kinah branch | `Aion.GameServer.Network.Aion.ServerPackets.SmLegionEdit.WarehouseKinah` / blocked bridge path | Packet Dependency | Not Started | Unit Tested as blocked | Needs Verification | Existing C# packet helper was identified, but bridge keeps legion warehouse blocked until item-vs-kinah routing and legion warehouse snapshots are modeled. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Construct_RejectedFoodWithRegularWarehouseUnlockContextBuildsWarehousePacketsBeforeSmPetLikeJava` | Unit | `ItemPacketService.sendStorageUpdatePacket`, `SM_WAREHOUSE_ADD_ITEM`, `SM_CUBE_UPDATE` | Regular warehouse unlock metadata emits `SmWarehouseAddItem` with `ALL_SLOT`, then regular warehouse cube update, before `SmPet` end metadata. | Source-derived C# serialization assertions. | No Java runtime vector; supplied snapshots may be stale. |
| `Construct_RejectedFoodWithAccountWarehouseUnlockContextUsesJavaZeroCubeUpdate` | Unit | `SM_CUBE_UPDATE.cubeSize(StorageType.ACCOUNT_WAREHOUSE, Player)` | Account warehouse unlock metadata emits warehouse add and the Java fall-through zero cube-update payload. | Source-derived C# serialization assertion. | No live account warehouse hydration. |
| `Construct_RejectedFoodUnlockKeepsLegionWarehouseStorageBlockedUntilLegionPacketBehaviorExists` | Unit | `ItemPacketService.sendStorageUpdatePacket` legion special case | Legion warehouse unlock stays blocked instead of skipping the kinah `SM_LEGION_EDIT` branch. | Explicit blocked boundary assertion. | Does not model legion item/kinah split yet. |

## Remaining Risks

- No Java runtime packet bytes have been captured.
- Live storage lookup and mutation are not wired.
- Supplied warehouse counts/expands can be stale if captured before Java-equivalent mutation.
- Legion warehouse item unlock and kinah special-case routing remain blocked.
- `SmWarehouseAddItem` uses the existing C# item blob writer; deep blob parity remains unverified here.
- No socket dispatch, scheduler, reward creation, DAO writes, or feed task execution is enabled.

## Next Recommended Unit of Work

Model the legion warehouse rejected-food unlock boundary in a non-live way: distinguish ordinary legion warehouse items from kinah, compose `SmWarehouseAddItem` or `SmLegionEdit.WarehouseKinah(...)` as Java does, and add a legion warehouse `SmCubeUpdate` snapshot helper. Keep live legion state, permissions, storage mutation, and socket dispatch disabled.
