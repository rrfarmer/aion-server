# Phase 6AFU Completion Handoff

Date: May 27, 2026
Completed Unit of Work: UOW-1341
Latest Commit: 7757c0024 `[Phase 6][UOW-1341] Add storage id ordinal resolver`
Status: Non-live Java storage-id to ordinal resolution is implemented for `SM_CUBE_UPDATE`; unusual-storage unlock dispatch remains disabled.

## What Changed

- Added `SmCubeUpdate.TryGetJavaStorageOrdinal(int storageTypeId, out int storageTypeOrdinal)`.
- Added `SmCubeUpdate.ZeroSizeForJavaStorageId(int storageTypeId)`.
- Added focused tests in `GamePacketTests`.
- Covered:
  - cube/warehouse ids `0` through `3`
  - pet bag ids `32` through `43`
  - house storage ids `60` through `79`
  - broker id `126`
  - mailbox id `127`
  - unknown-id guard cases
- Added `docs/Phase-6-BindPointTeleport-PetFeedStorageIdOrdinalResolver.md`.
- Updated `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`.
- Updated `docs/PHASE-6-PROGRESS.md`.

## Code Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmCubeUpdate.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`

## Documentation Changed

- `docs/Phase-6-BindPointTeleport-PetFeedStorageIdOrdinalResolver.md`
- `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6AFU-Completion.md`

## Validation Completed

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "SmCubeUpdate_TryGetJavaStorageOrdinal|SmCubeUpdate_ZeroSizeForJavaStorageId|SmCubeUpdate_ZeroSizeForJavaStorageOrdinal"` passed 83 tests.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "PetFeedSubtype7JavaVectorArtifactReader|PetFeedRejectedFoodMetadataComposition|PetFeedUnlockPacketContextAssembler|PetFeedPacketMetadataBridge|PetFeedServiceOperationPlanner|PetFeedEvaluation|PetFeedXmlProjection|PetFoodTypeLookup|PetFeedPlanner|PetFeedCalculator|PetFeedProgress|PetHungryLevel|SmPet|SmEmotion|SmSystemMessage|SmInventoryAddItem|SmWarehouseAddItem|SmLegionEdit|SmCubeUpdate"` passed 257 tests.
- `dotnet build dotnetConversion/src/Aion.GameServer/Aion.GameServer.csproj` passed with 0 warnings and 0 errors.

No live storage lookup, inventory mutation, packet send, live item/template/player/account/legion/house/pet hydration, scheduler execution, reward item creation, DAO write, or Java runtime packet comparison was enabled.

## Migration Parity Table - UOW-1341

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.items.storage.StorageType.getStorageTypeById` | `Aion.GameServer.Network.Aion.ServerPackets.SmCubeUpdate.TryGetJavaStorageOrdinal` | Enum / Mapping Helper | Partial | Unit Tested | Partial Parity | Resolver covers known storage ids needed by cube-update metadata. It returns false for unmodeled ids instead of returning Java `null`; caller behavior must preserve Java no-send boundaries where appropriate. |
| `com.aionemu.gameserver.model.items.storage.StorageType` pet bag ids | `SmCubeUpdate.TryGetJavaStorageOrdinal`; `GamePacketTests` | Enum / Packet Metadata | Partial | Unit Tested | Partial Parity | Tests cover ids `32` through `43` mapping to ordinals `4` through `15`. Live pet bag ownership remains unsupported. |
| `com.aionemu.gameserver.model.items.storage.StorageType` house storage ids | `SmCubeUpdate.TryGetJavaStorageOrdinal`; `GamePacketTests` | Enum / Packet Metadata | Partial | Unit Tested | Partial Parity | Tests cover ids `60` through `79` mapping to ordinals `16` through `35`. Live house storage ownership remains unsupported. |
| `com.aionemu.gameserver.model.items.storage.StorageType.BROKER` / `MAILBOX` | `SmCubeUpdate.TryGetJavaStorageOrdinal`; `GamePacketTests` | Enum / Packet Metadata | Partial | Unit Tested | Partial Parity | Tests cover broker id `126` to ordinal `36` and mailbox id `127` to ordinal `37`. Runtime reachability is unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE.cubeSize` unusual storage fallback | `Aion.GameServer.Network.Aion.ServerPackets.SmCubeUpdate.ZeroSizeForJavaStorageId` | Packet Helper | Partial | Unit Tested | Partial Parity | Helper resolves Java storage id to ordinal and writes zero count/expand fields. It does not compare Java runtime bytes or construct preceding add/unlock packets. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `SmCubeUpdate_TryGetJavaStorageOrdinalMapsStorageIdsLikeJavaEnumOrder` | Unit | `StorageType` enum declaration order | Storage ids `0`, `1`, `2`, `3`, pet bag ids `32` through `43`, house ids `60` through `79`, broker `126`, and mailbox `127` map to Java ordinals. | Source-derived mapping validation. | Does not call Java at runtime. |
| `SmCubeUpdate_ZeroSizeForJavaStorageIdUsesOrdinalNotStorageId` | Unit | `SM_CUBE_UPDATE.cubeSize` | Representative unusual storage ids write ordinal action values rather than ids. | Source-derived payload validation. | No Java byte artifact comparison. |
| `SmCubeUpdate_TryGetJavaStorageOrdinalRejectsUnknownStorageIds` | Unit | `StorageType.getStorageTypeById` null boundary | Unknown ids around supported ranges fail resolver and throw from packet factory. | Source-derived guard validation. | Java exact exception behavior is not matched; caller must decide no-send vs throw. |

## Remaining Risks

- Resolver behavior is packet-helper scoped; live callers must still preserve Java unknown-storage no-send behavior.
- `SM_WAREHOUSE_ADD_ITEM` unusual-storage construction remains unimplemented.
- Live pet/house/broker/mailbox ownership and storage hydration remain unsupported.
- Java runtime packet bytes are still unavailable.
- Packet send ordering remains non-live metadata only.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 resolver helper plus 3 focused tests covering 47 new cases
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: live unusual-storage unlock adapter, Java runtime packet artifacts, live ownership/storage hydration, unusual `SM_WAREHOUSE_ADD_ITEM` wiring, socket dispatch
- Estimated overall migration completion: Phase 6 remains about 71% complete

# Next Work Options

## Recommended Sequential Task

- Task: Add a non-live unusual-storage unlock packet metadata design or guarded bridge slice.
- Why: The cube-update id/ordinal helper is now safe. The next step is pairing Java `SM_WAREHOUSE_ADD_ITEM(storageType.getId())` with `SmCubeUpdate.ZeroSizeForJavaStorageId(storageTypeId)` while still blocking live pet/house/broker/mailbox dispatch.
- Files:
  - `dotnetConversion/src/Aion.GameServer/Services/ToyPet/PetFeedPacketMetadataBridge.cs` only if implementing guarded metadata
  - focused tests in `dotnetConversion/tests/Aion.GameServer.Tests/PetFeedPacketMetadataBridgeTests.cs`
  - shared docs/handoff/progress

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Guarded unusual-storage metadata bridge slice | `PetFeedPacketMetadataBridge.cs`, focused tests | Medium | One writer only. Keep live dispatch disabled. |
| B | Docs-only runtime artifact schema for unusual-storage unlock packets | docs only | Medium | Can precede bridge implementation if runtime byte shape is still concerning. |
| C | Read-only Java reachability audit for pet feed unusual storage unlocks | read-only | Low | Useful before enabling metadata beyond tests. |
| D | Inert Java serialization observer hook | Java network core + new observer classes | High | Exclusive owner only. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Explorer A | Read-only Java reachability audit for unusual storage ids in pet feed rejected-food flow | read-only | all writes |
| Orchestrator | Guarded bridge/design work and shared docs | assigned bridge/test/docs files | Java network core |

## Do Not Parallelize

- `PetFeedPacketMetadataBridge.cs`: one writer only.
- `SmCubeUpdate.cs`: avoid concurrent edits after UOW-1340/UOW-1341.
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
  - `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/ToyPet/PetFeedPacketMetadataBridge.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PetFeedPacketMetadataBridgeTests.cs`
- Docs:
  - `docs/csharp-port.md`
  - `docs/orchestration-rules.md`
  - `docs/parallelization-strategy.md`
  - `docs/parity-verification.md`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6-BindPointTeleport-PetFeedStorageIdOrdinalResolver.md`
  - `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageCubeUpdateOrdinalHelper.md`
  - `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`
