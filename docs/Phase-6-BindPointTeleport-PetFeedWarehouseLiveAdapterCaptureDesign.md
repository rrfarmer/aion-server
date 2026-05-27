# Phase 6 - Pet Feed Warehouse Live Adapter Capture Design

Date: May 27, 2026
Unit of Work: UOW-1339

## Scope

This docs-only unit defines the future live-adapter snapshot boundary for rejected-food unlock packets after UOW-1338 completed full house-storage unsupported test coverage.

Java source of truth:

- `com.aionemu.gameserver.services.item.ItemPacketService.sendItemUnlockPacket`
- `com.aionemu.gameserver.services.item.ItemPacketService.sendStorageUpdatePacket`
- `com.aionemu.gameserver.model.items.storage.StorageType`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_WAREHOUSE_ADD_ITEM`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE`

C# surfaces reviewed:

- `Aion.GameServer.Services.ToyPet.PetFeedUnlockPacketContextAssembler`
- `Aion.GameServer.Services.ToyPet.PetFeedPacketMetadataBridge`
- `Aion.GameServer.Network.Aion.ServerPackets.SmWarehouseAddItem`
- `Aion.GameServer.Network.Aion.ServerPackets.SmCubeUpdate`

## Java Behavior Reviewed

`sendItemUnlockPacket(player, item)` resolves `item.getItemLocation()` through `StorageType.getStorageTypeById`.

- If the storage id is unknown, Java sends nothing.
- If the storage id is known, Java calls `sendStorageUpdatePacket(player, storageType, item, ItemAddType.ALL_SLOT)`.

`sendStorageUpdatePacket` synchronously queues packet metadata in this order:

1. Add/unlock packet:
   - `CUBE`: `SM_INVENTORY_ADD_ITEM(Collections.singletonList(item), player, ALL_SLOT)`
   - `LEGION_WAREHOUSE` kinah: `SM_LEGION_EDIT(0x04, player.getLegion())`
   - all other non-cube storage: `SM_WAREHOUSE_ADD_ITEM(item, storageType.getId(), player, ALL_SLOT)`
2. Size packet:
   - `SM_CUBE_UPDATE.cubeSize(storageType, player)`

`SM_CUBE_UPDATE.cubeSize` snapshots counts and expands when constructed, after the add/unlock packet is queued.

It populates real counts only for:

- `CUBE`
- `REGULAR_WAREHOUSE`
- `LEGION_WAREHOUSE`

It zero-fills counts and expands for:

- `ACCOUNT_WAREHOUSE`
- pet bags
- house storage
- broker
- mailbox

In every case, the size packet writes action `0` and `StorageType.ordinal()` as the action value, not `StorageType.getId()`.

Known action values for this slice:

| Java `StorageType` | Storage Id | `ordinal()` Action Value |
|---|---:|---:|
| `CUBE` | `0` | `0` |
| `REGULAR_WAREHOUSE` | `1` | `1` |
| `ACCOUNT_WAREHOUSE` | `2` | `2` |
| `LEGION_WAREHOUSE` | `3` | `3` |
| `PET_BAG_6` through `CASH_PET_BAG_34` | `32` through `43` | `4` through `15` |
| `HOUSE_STORAGE_01` through `HOUSE_STORAGE_20` | `60` through `79` | `16` through `35` |
| `BROKER` | `126` | `36` |
| `MAILBOX` | `127` | `37` |

## Snapshot Boundary Design

The future C# live adapter should mirror Java as one synchronous fanout operation.

1. Resolve the storage kind from the item's current storage location at the rejected-food unlock boundary.
2. If the storage id is unknown, do not construct packet metadata.
3. If the storage id is known but unsupported, keep blocking until runtime ownership/context and Java bytes exist.
4. For supported ids, snapshot item/template/player storage facts after the rejected-food decision has restored/unlocked the item and after the relevant storage mutation has completed.
5. Construct the add/unlock packet first.
6. Immediately snapshot `SM_CUBE_UPDATE` counts/expands and construct the size packet second.
7. Do not await, schedule, or yield between add/unlock construction and cube-update construction unless the adapter holds the relevant player/storage consistency lock.

## Required Future Snapshot Inputs

For cube:

- item object id
- item template id/name/blob facts
- equipment slot
- cube item count
- player NPC expands
- player quest expands
- player item expands

For regular warehouse:

- item object id
- item template id/name/blob facts
- equipment slot
- regular warehouse item count
- warehouse NPC expands
- warehouse bonus expands

For account warehouse:

- item object id
- item template id/name/blob facts
- equipment slot
- storage id `2`
- zero cube-update counts/expands with action value `2`

For legion warehouse:

- ordinary item path:
  - item object id
  - item template id/name/blob facts
  - equipment slot
  - storage id `3`
  - legion warehouse item count
  - legion warehouse expansions
- kinah path:
  - current legion warehouse kinah
  - legion warehouse item count
  - legion warehouse expansions
  - no item template requirement for the kinah add packet branch

For future unusual storage support:

- storage id
- Java `StorageType.ordinal()` action value
- item object id
- item template id/name/blob facts
- equipment slot
- zero cube-update counts/expands unless Java runtime artifacts prove otherwise
- ownership/runtime context for pet bag, house cabinet, broker, or mailbox

## Current C# Gaps

- `PetFeedUnlockPacketContextAssembler` is non-live and consumes supplied snapshots.
- `PetFeedPacketMetadataBridge` is non-sending metadata only.
- `SmCubeUpdate` has explicit helpers for cube, regular warehouse, account warehouse zero update, and legion warehouse snapshots, but no generic unusual-storage ordinal helper.
- Pet bags, house storage, broker, and mailbox are known but blocked.
- No Java runtime packet artifacts exist for unusual rejected-food unlock paths.
- Java packet objects hold item/player references, so runtime serialization timing remains an artifact-generation concern.

## Future Implementation Rules

- Preserve add/unlock-before-cube-update ordering.
- Preserve `ItemAddType.ALL_SLOT` for unlock packet construction.
- Preserve `StorageType.getId()` for `SM_WAREHOUSE_ADD_ITEM` warehouse type.
- Preserve `StorageType.ordinal()` for `SM_CUBE_UPDATE` action value.
- Preserve Java zero-count fallback for account warehouse, pet bags, house storage, broker, and mailbox unless an intentional difference is approved and documented.
- Keep unknown storage ids as no-send.
- Keep known but unsupported storage ids blocked until ownership context and Java bytes exist.
- Add runtime or golden-byte comparison before claiming verified parity for any unusual storage branch.

## Migration Parity Table - UOW-1339

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemUnlockPacket` | future rejected-food live unlock adapter | Service Boundary / Design | Not Started | Manual Only | Needs Verification | Design captures Java's resolve-known-storage-or-no-send entry behavior. No C# live adapter or runtime comparison exists. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendStorageUpdatePacket` | future rejected-food live unlock adapter; `PetFeedPacketMetadataBridge` | Packet Service / Design | Partial | Manual Only | Needs Verification | Existing C# metadata bridge models supported supplied snapshots, but future live adapter must snapshot and queue add/unlock before cube update without yielding. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_WAREHOUSE_ADD_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmWarehouseAddItem`; future unusual-storage adapter | Packet / Design | Partial | Unit Tested for modeled warehouse families | Needs Verification | Existing C# warehouse packet support does not verify pet/house/broker/mailbox ids or Java runtime item-reference timing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE.cubeSize` | `Aion.GameServer.Network.Aion.ServerPackets.SmCubeUpdate`; future unusual-storage ordinal helper | Packet / Design | Partial | Unit Tested for modeled families | Needs Verification | C# has explicit modeled helpers but lacks a generic unusual-storage ordinal/zero-count helper. Java uses ordinal action values and zero counts for unhandled storage types. |
| `com.aionemu.gameserver.model.items.storage.StorageType` ordinals | future storage-id-to-ordinal mapping | Enum / Design Dependency | Not Started | Manual Only | Needs Verification | Ordinal mapping is critical for future unusual-storage `SM_CUBE_UPDATE` metadata: pet bags `4` through `15`, house storage `16` through `35`, broker `36`, mailbox `37`. Needs tests before implementation. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None | Documentation / Design | `ItemPacketService`, `SM_CUBE_UPDATE`, `SM_WAREHOUSE_ADD_ITEM`, `StorageType` source review | Defines future live-adapter snapshot order, required inputs, zero-count fallback, and ordinal-vs-id rule. | Manual source review only. | No code, tests, Java artifacts, runtime comparison, or live dispatch. |

## Remaining Risks

- Java packet serialization may observe item/player mutations after queue time because packet objects hold references.
- Future live C# code can race if it yields between add/unlock metadata and cube-update metadata.
- Account warehouse zero-count behavior is Java-source-derived but still lacks Java byte artifacts.
- Pet/house/broker/mailbox packet branches may be rare or unreachable in normal pet-feed flow; runtime artifacts are still needed.
- C# has no generic unusual-storage `SmCubeUpdate` helper yet.
- No live storage lookup, mutation, packet dispatch, scheduler execution, reward creation, DAO writes, or socket dispatch is enabled.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 0 code artifacts; 1 live-adapter capture design document
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: live rejected-food unlock adapter, unusual-storage ordinal helper, Java runtime packet artifacts, live ownership/storage hydration, socket dispatch
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a focused non-live test/design slice for Java `StorageType.ordinal()` mapping and unusual-storage `SM_CUBE_UPDATE` zero-count metadata before implementing live pet/house/broker/mailbox unlock packet construction.
