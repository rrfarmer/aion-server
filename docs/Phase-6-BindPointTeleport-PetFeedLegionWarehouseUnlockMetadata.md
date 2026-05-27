# Phase 6 - Pet Feed Legion Warehouse Unlock Metadata

Date: May 27, 2026
Unit of Work: UOW-1330

## Scope

This unit completes the non-live rejected-food unlock metadata coverage for Java legion warehouse storage paths.

Java source of truth:

- `com.aionemu.gameserver.services.item.ItemPacketService.sendStorageUpdatePacket`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_WAREHOUSE_ADD_ITEM`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_EDIT`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE`
- `com.aionemu.gameserver.model.items.storage.StorageType`

## Implemented

- Added `SmCubeUpdate.LegionWarehouseSizeSnapshot(...)`.
- Extended `PetFeedUnlockPacketContext` with legion kinah metadata.
- Extended `PetFeedPacketMetadataBridge` so supplied ordinary legion warehouse item context constructs:
  1. `SmWarehouseAddItem` with warehouse type `3` and `ALL_SLOT = 0x13`
  2. `SmCubeUpdate.LegionWarehouseSizeSnapshot(...)`
- Extended `PetFeedPacketMetadataBridge` so supplied legion warehouse kinah context constructs:
  1. `SmLegionEdit.WarehouseKinah(...)`
  2. `SmCubeUpdate.LegionWarehouseSizeSnapshot(...)`

## Not Implemented

- No live legion lookup.
- No live item-vs-kinah storage routing.
- No live warehouse mutation.
- No live packet dispatch.
- No Java runtime packet byte comparison.
- No legion permission or lock validation.

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "PetFeedPacketMetadataBridge|SmLegionEdit|SmWarehouseAddItem|SmCubeUpdate"` passed 20 tests.

## Migration Parity Table - UOW-1330

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemPacketService.sendStorageUpdatePacket` legion warehouse item branch | `Aion.GameServer.Services.ToyPet.PetFeedPacketMetadataBridge.ConstructLegionWarehouseItemUnlock` + `SmWarehouseAddItem` | Packet Metadata Composition | Partial | Unit Tested | Partial Parity | Builds non-sending ordinary legion item unlock metadata using supplied snapshots. Live legion warehouse mutation and sends remain disabled. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendStorageUpdatePacket` legion warehouse kinah branch | `PetFeedPacketMetadataBridge.ConstructLegionWarehouseItemUnlock` + `SmLegionEdit.WarehouseKinah` | Packet Metadata Composition | Partial | Unit Tested | Partial Parity | Preserves Java special case that uses `SM_LEGION_EDIT(0x04, legion)` for kinah instead of `SM_WAREHOUSE_ADD_ITEM`. Live legion lookup and kinah amount hydration remain unwired. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE.cubeSize(StorageType.LEGION_WAREHOUSE, Player)` | `Aion.GameServer.Network.Aion.ServerPackets.SmCubeUpdate.LegionWarehouseSizeSnapshot` | Server Packet Helper | Partial | Unit Tested | Partial Parity | Preserves action `0`, ordinal `3`, supplied legion warehouse size, and warehouse expansion count. Snapshot correctness remains caller-owned. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_EDIT` type `0x04` | `Aion.GameServer.Network.Aion.ServerPackets.SmLegionEdit.WarehouseKinah` | Server Packet | Partial | Unit Tested | Partial Parity | Existing packet helper is now consumed by pet-feed unlock metadata. No live Java byte capture or legion object comparison was performed. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_WAREHOUSE_ADD_ITEM` legion warehouse type `3` | `Aion.GameServer.Network.Aion.ServerPackets.SmWarehouseAddItem.CreateAllSlot` | Server Packet | Partial | Unit Tested | Partial Parity | Reuses UOW-1329 packet serializer with warehouse type `3`. Deep item blob parity remains unverified. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Construct_RejectedFoodWithLegionWarehouseItemUnlockContextBuildsWarehousePacketsLikeJava` | Unit | `ItemPacketService.sendStorageUpdatePacket`, `SM_WAREHOUSE_ADD_ITEM`, `SM_CUBE_UPDATE` | Ordinary legion warehouse item unlock emits `SmWarehouseAddItem` type `3` and legion cube update. | Source-derived C# serialization assertions. | No Java runtime vector; supplied snapshots may be stale. |
| `Construct_RejectedFoodWithLegionWarehouseKinahUnlockContextBuildsLegionEditLikeJava` | Unit | `ItemPacketService.sendStorageUpdatePacket`, `SM_LEGION_EDIT`, `SM_CUBE_UPDATE` | Legion warehouse kinah unlock emits `SmLegionEdit` type `4` with kinah amount and legion cube update. | Source-derived C# serialization assertions. | No live legion object or warehouse kinah hydration. |

## Remaining Risks

- Java runtime packet bytes have not been captured.
- Live `StorageType.getStorageTypeById`, item-template lookup, legion lookup, and item-vs-kinah routing are not wired.
- Supplied legion warehouse size/expansion/kinah snapshots can be stale.
- No socket dispatch, storage mutation, scheduler, reward creation, DAO writes, or feed task execution is enabled.
- No legion warehouse permission/lock behavior is modeled.

## Next Recommended Unit of Work

Add a non-live rejected-food unlock context assembler design that maps supplied item location ids to cube, regular warehouse, account warehouse, and legion warehouse packet context statuses. Keep live player inventory/warehouse mutation and sends disabled until repository/storage ownership surfaces are ready.
