# Phase 6AFI Completion Handoff

Date: May 27, 2026
Completed Unit of Work: UOW-1329
Latest Commit: included in the UOW-1329 unit commit
Status: Pet feed rejected-food metadata can now represent regular/account warehouse Java unlock packet pairs; legion warehouse remains blocked.

## What Changed

- Completed a read-only sub-agent audit of Java `SM_WAREHOUSE_ADD_ITEM`, `StorageType`, `SM_CUBE_UPDATE`, and the legion warehouse kinah branch.
- Added `SmWarehouseAddItem`.
- Added `SmCubeUpdate.RegularWarehouseSizeSnapshot(...)`.
- Extended `PetFeedPacketMetadataBridge` so supplied regular warehouse unlock context constructs:
  1. `SmWarehouseAddItem` with `ALL_SLOT = 0x13`
  2. `SmCubeUpdate.RegularWarehouseSizeSnapshot(...)`
- Extended `PetFeedPacketMetadataBridge` so supplied account warehouse unlock context constructs:
  1. `SmWarehouseAddItem` with `ALL_SLOT = 0x13`
  2. `SmCubeUpdate.AccountWarehouseSize()`
- Kept legion warehouse blocked because Java has an item-vs-kinah split: ordinary items use `SM_WAREHOUSE_ADD_ITEM`, but legion kinah uses `SM_LEGION_EDIT(0x04, legion)`.
- Added `docs/Phase-6-BindPointTeleport-PetFeedWarehouseUnlockMetadata.md`.
- Updated `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`.
- Updated `docs/PHASE-6-PROGRESS.md`.

## Code Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmWarehouseAddItem.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmCubeUpdate.cs`
- `dotnetConversion/src/Aion.GameServer/Services/ToyPet/PetFeedPacketMetadataBridge.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PetFeedPacketMetadataBridgeTests.cs`

## Validation Completed

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "PetFeedPacketMetadataBridge|SmWarehouseAddItem|SmCubeUpdate"` passed 10 tests.

No live storage lookup, inventory mutation, packet send, legion warehouse unlock routing, live item/template/player hydration, scheduler execution, reward item creation, DAO write, or Java runtime packet comparison was enabled.

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

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 1 warehouse add packet, 1 regular warehouse cube-update helper, regular/account warehouse unlock packet-sequence metadata, and 3 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: legion warehouse item/kinah routing, live storage lookup, live item/template/player hydration, live inventory mutation, packet dispatch, Java runtime packet comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete

# Next Work Options

## Recommended Sequential Task

- Task: Model the legion warehouse rejected-food unlock boundary in a non-live way.
- Why: Java ordinary legion warehouse items use `SM_WAREHOUSE_ADD_ITEM`, but legion warehouse kinah uses `SM_LEGION_EDIT(0x04, legion)` before `SM_CUBE_UPDATE`. The bridge should not claim non-cube parity until that split is explicit.
- Files: likely `SmCubeUpdate.cs`, `SmLegionEdit.cs` tests, `PetFeedPacketMetadataBridge.cs`, and `PetFeedPacketMetadataBridgeTests.cs`. Keep live legion state, permissions, storage mutation, packet dispatch, scheduler, DAO, and reward creation disabled.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Legion warehouse unlock metadata | `SmCubeUpdate`, `PetFeedPacketMetadataBridge`, focused tests | Medium | One exclusive writer because the bridge is fresh. |
| B | Java subtype `7` runtime-vector design | docs/read-only | Low | Still needed before stronger refeed-delay/progress parity claims due Java mutable queued packet behavior. |
| C | Java feed packet vector design | docs/read-only | Low | No runtime vectors until tooling exists, but schema/design can be prepared. |
| D | Warehouse live-adapter capture design | docs/read-only | Medium | Defines when to snapshot warehouse counts/expands relative to future inventory unlock execution. |

## Do Not Parallelize

- `PetFeedPacketMetadataBridge.cs`: fresh packet-sequence surface; one writer only.
- `SmWarehouseAddItem.cs`: new packet serializer; avoid concurrent edits.
- Shared progress/handoff docs: orchestrator-owned only.
- Live feed runtime remains blocked by inventory, item service, scheduler, persistence, packet dispatch, localization, and Java runtime validation.

## Context Files

- Java source:
  - `game-server/src/com/aionemu/gameserver/services/toypet/PetService.java`
  - `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_WAREHOUSE_ADD_ITEM.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_CUBE_UPDATE.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_LEGION_EDIT.java`
  - `game-server/src/com/aionemu/gameserver/model/items/storage/StorageType.java`
- C# source/tests:
  - `dotnetConversion/src/Aion.GameServer/Services/ToyPet/PetFeedPacketMetadataBridge.cs`
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmWarehouseAddItem.cs`
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmCubeUpdate.cs`
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmLegionEdit.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PetFeedPacketMetadataBridgeTests.cs`
- Docs:
  - `docs/csharp-port.md`
  - `docs/orchestration-rules.md`
  - `docs/parallelization-strategy.md`
  - `docs/parity-verification.md`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6-BindPointTeleport-PetFeedWarehouseUnlockMetadata.md`
  - `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`
