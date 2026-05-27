# Phase 6 - Pet Feed Storage Id Ordinal Resolver

Date: May 27, 2026
Unit of Work: UOW-1341

## Scope

This unit adds a non-live Java storage-id to enum-ordinal resolver for `SM_CUBE_UPDATE` metadata. It builds on UOW-1340, which added zero-count cube-update serialization for supplied Java ordinals.

Java source of truth:

- `com.aionemu.gameserver.model.items.storage.StorageType`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE.cubeSize`

C# artifacts:

- `Aion.GameServer.Network.Aion.ServerPackets.SmCubeUpdate`
- `Aion.GameServer.Tests.GamePacketTests`

## Java Behavior Reviewed

Java `StorageType.getStorageTypeById(int id)` resolves known storage ids by scanning enum declaration order. `SM_CUBE_UPDATE.cubeSize` then writes `StorageType.ordinal()` as the action value.

Known mappings covered in this unit:

| Storage Id | Java StorageType Range | Java Ordinal |
|---:|---|---:|
| `0` | `CUBE` | `0` |
| `1` | `REGULAR_WAREHOUSE` | `1` |
| `2` | `ACCOUNT_WAREHOUSE` | `2` |
| `3` | `LEGION_WAREHOUSE` | `3` |
| `32` through `43` | pet bags | `4` through `15` |
| `60` through `79` | house storage | `16` through `35` |
| `126` | `BROKER` | `36` |
| `127` | `MAILBOX` | `37` |

## Implemented

- Added `SmCubeUpdate.TryGetJavaStorageOrdinal(int storageTypeId, out int storageTypeOrdinal)`.
- Added `SmCubeUpdate.ZeroSizeForJavaStorageId(int storageTypeId)`.
- Added tests for every modeled storage id mapping.
- Added tests proving unusual storage ids write Java ordinals, not storage ids, into `SM_CUBE_UPDATE`.
- Added unknown-id guard tests for holes around known ranges.

This remains non-live packet helper work. It does not enable pet/house/broker/mailbox rejected-food unlock metadata, storage mutation, or packet dispatch.

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "SmCubeUpdate_TryGetJavaStorageOrdinal|SmCubeUpdate_ZeroSizeForJavaStorageId|SmCubeUpdate_ZeroSizeForJavaStorageOrdinal"` passed 83 tests.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "PetFeedSubtype7JavaVectorArtifactReader|PetFeedRejectedFoodMetadataComposition|PetFeedUnlockPacketContextAssembler|PetFeedPacketMetadataBridge|PetFeedServiceOperationPlanner|PetFeedEvaluation|PetFeedXmlProjection|PetFoodTypeLookup|PetFeedPlanner|PetFeedCalculator|PetFeedProgress|PetHungryLevel|SmPet|SmEmotion|SmSystemMessage|SmInventoryAddItem|SmWarehouseAddItem|SmLegionEdit|SmCubeUpdate"` passed 257 tests.
- `dotnet build dotnetConversion/src/Aion.GameServer/Aion.GameServer.csproj` passed with 0 warnings and 0 errors.

## Migration Parity Table - UOW-1341

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.items.storage.StorageType.getStorageTypeById` | `SmCubeUpdate.TryGetJavaStorageOrdinal` | Enum / Mapping Helper | Partial | Unit Tested | Partial Parity | Resolver covers known storage ids needed by cube-update metadata. It returns false for unmodeled ids instead of returning Java `null`; caller behavior must preserve Java no-send boundaries where appropriate. |
| `com.aionemu.gameserver.model.items.storage.StorageType` pet bag ids | `SmCubeUpdate.TryGetJavaStorageOrdinal`; `GamePacketTests` | Enum / Packet Metadata | Partial | Unit Tested | Partial Parity | Tests cover ids `32` through `43` mapping to ordinals `4` through `15`. Live pet bag ownership remains unsupported. |
| `com.aionemu.gameserver.model.items.storage.StorageType` house storage ids | `SmCubeUpdate.TryGetJavaStorageOrdinal`; `GamePacketTests` | Enum / Packet Metadata | Partial | Unit Tested | Partial Parity | Tests cover ids `60` through `79` mapping to ordinals `16` through `35`. Live house storage ownership remains unsupported. |
| `com.aionemu.gameserver.model.items.storage.StorageType.BROKER` / `MAILBOX` | `SmCubeUpdate.TryGetJavaStorageOrdinal`; `GamePacketTests` | Enum / Packet Metadata | Partial | Unit Tested | Partial Parity | Tests cover broker id `126` to ordinal `36` and mailbox id `127` to ordinal `37`. Runtime reachability is unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE.cubeSize` unusual storage fallback | `SmCubeUpdate.ZeroSizeForJavaStorageId` | Packet Helper | Partial | Unit Tested | Partial Parity | Helper resolves Java storage id to ordinal and writes zero count/expand fields. It does not compare Java runtime bytes or construct preceding add/unlock packets. |

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

## Next Recommended Unit of Work

Add a non-live unusual-storage unlock packet metadata design or guarded bridge slice that pairs `SM_WAREHOUSE_ADD_ITEM(storageType.getId())` with `SmCubeUpdate.ZeroSizeForJavaStorageId(storageTypeId)` without enabling live pet/house/broker/mailbox dispatch.
