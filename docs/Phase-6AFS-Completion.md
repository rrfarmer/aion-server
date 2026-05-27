# Phase 6AFS Completion Handoff

Date: May 27, 2026
Completed Unit of Work: UOW-1339
Latest Commit: included in the UOW-1339 unit commit
Status: Rejected-food warehouse live-adapter snapshot timing and Java storage ordinal rules are documented; live unlock dispatch remains disabled.

## What Changed

- Added `docs/Phase-6-BindPointTeleport-PetFeedWarehouseLiveAdapterCaptureDesign.md`.
- Documented the future rejected-food live-adapter snapshot boundary:
  - resolve storage id at unlock entry;
  - snapshot item/template/player storage facts after the rejected-food restore/unlock decision and storage mutation;
  - construct add/unlock metadata first;
  - immediately construct `SM_CUBE_UPDATE` metadata second without yielding.
- Captured the Java id-vs-ordinal split:
  - `SM_WAREHOUSE_ADD_ITEM` uses `StorageType.getId()`.
  - `SM_CUBE_UPDATE.cubeSize` uses `StorageType.ordinal()`.
- Integrated read-only ordinal mapping for future unusual-storage cube updates:
  - cube/regular/account/legion ordinals `0` through `3`
  - pet bag ordinals `4` through `15`
  - house storage ordinals `16` through `35`
  - broker ordinal `36`
  - mailbox ordinal `37`
- Updated `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`.
- Updated `docs/PHASE-6-PROGRESS.md`.

## Code Changed

- None. This was a documentation/design unit.

## Documentation Changed

- `docs/Phase-6-BindPointTeleport-PetFeedWarehouseLiveAdapterCaptureDesign.md`
- `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6AFS-Completion.md`

## Validation Completed

- `git diff --check` passed with line-ending warnings only.

No executable tests were added or required for this docs-only unit. No live storage lookup, inventory mutation, packet send, live item/template/player/account/legion/house/pet hydration, scheduler execution, reward item creation, DAO write, or Java runtime packet comparison was enabled.

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

# Next Work Options

## Recommended Sequential Task

- Task: Add a focused non-live test/design slice for Java `StorageType.ordinal()` mapping and unusual-storage `SM_CUBE_UPDATE` zero-count metadata.
- Why: UOW-1339 documented that future unusual storage support must serialize Java enum ordinals, not storage ids. A small non-live helper/test slice can pin that behavior before any live unlock adapter uses it.
- Files:
  - likely `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmCubeUpdate.cs`
  - focused packet tests in `dotnetConversion/tests/Aion.GameServer.Tests`
  - shared docs/handoff/progress

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Non-live unusual-storage cube-update helper/tests | `SmCubeUpdate.cs`, focused tests | Medium | One writer only; do not batch with packet bridge changes. |
| B | Java runtime artifact schema for unusual-storage unlock packets | docs only | Medium | Can run alongside A if docs are orchestrator-owned or assigned exclusively. |
| C | Inert Java serialization observer hook | Java network core + new observer classes | High | Exclusive owner only. |
| D | Account warehouse byte-artifact comparator design | docs/tests separate from `SmCubeUpdate.cs` | Medium | Useful because account warehouse already uses zero-count action value `2`. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Worker A | Add non-live `SmCubeUpdate` helper/tests for Java unusual-storage ordinal zero-count payloads | `SmCubeUpdate.cs` and one focused test file only | `PetFeedPacketMetadataBridge.cs`, assembler, shared docs |
| Explorer B | Read-only review of Java packet-vector needs for unusual storage unlock paths | Read-only | All writes |
| Orchestrator | Review/integrate, run tests, update progress/handoff docs | shared docs | files owned by Worker A until complete |

## Do Not Parallelize

- `SmCubeUpdate.cs`: one writer only because it is shared packet infrastructure.
- `PetFeedPacketMetadataBridge.cs`: do not touch in the same unit unless the cube-update helper is already complete and tested.
- Java network core files: observer hook must be exclusive.
- Shared progress/handoff docs: orchestrator-owned only.

## Context Files

- Java source:
  - `game-server/src/com/aionemu/gameserver/model/items/storage/StorageType.java`
  - `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_WAREHOUSE_ADD_ITEM.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_CUBE_UPDATE.java`
- C# source/tests:
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmCubeUpdate.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/ToyPet/PetFeedUnlockPacketContextAssembler.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/ToyPet/PetFeedPacketMetadataBridge.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PetFeedPacketMetadataBridgeTests.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- Docs:
  - `docs/csharp-port.md`
  - `docs/orchestration-rules.md`
  - `docs/parallelization-strategy.md`
  - `docs/parity-verification.md`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6-BindPointTeleport-PetFeedWarehouseLiveAdapterCaptureDesign.md`
  - `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`
