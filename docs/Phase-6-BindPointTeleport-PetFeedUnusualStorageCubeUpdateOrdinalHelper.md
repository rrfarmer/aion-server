# Phase 6 - Pet Feed Unusual Storage Cube Update Ordinal Helper

Date: May 27, 2026
Unit of Work: UOW-1340

## Scope

This unit adds a non-live `SM_CUBE_UPDATE` helper for Java unusual storage ordinal metadata before any live pet/house/broker/mailbox unlock adapter is enabled.

Java source of truth:

- `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE.cubeSize`
- `com.aionemu.gameserver.model.items.storage.StorageType`

C# artifacts:

- `Aion.GameServer.Network.Aion.ServerPackets.SmCubeUpdate`
- `Aion.GameServer.Tests.GamePacketTests`

## Java Behavior Reviewed

`SM_CUBE_UPDATE.cubeSize(StorageType type, Player player)` writes:

- action `0`
- action value `type.ordinal()`
- `itemsCount`
- `npcExpands`
- `questExpands`
- `itemExpands`

Only `CUBE`, `REGULAR_WAREHOUSE`, and `LEGION_WAREHOUSE` populate count/expand fields. `ACCOUNT_WAREHOUSE`, pet bags, house storage, broker, and mailbox fall through with zero count/expand fields.

For this unit, the newly covered unusual storage ordinals are:

- pet bag ordinals `4` through `15`
- house storage ordinals `16` through `35`
- broker ordinal `36`
- mailbox ordinal `37`

## Implemented

- Added `SmCubeUpdate.ZeroSizeForJavaStorageOrdinal(int storageTypeOrdinal)`.
- The helper writes Java action `0`, supplied Java ordinal action value, and zero count/expand fields.
- Added byte-range guard coverage for action values below `0` and above `255`.
- Added source-breadcrumb tests that enumerate every currently known unusual-storage ordinal `4` through `37`.

This helper is intentionally non-live. It does not resolve Java storage ids, mutate storage, construct warehouse add packets, or dispatch packets.

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "SmCubeUpdate_ZeroSizeForJavaStorageOrdinal"` passed 36 tests.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "PetFeedSubtype7JavaVectorArtifactReader|PetFeedRejectedFoodMetadataComposition|PetFeedUnlockPacketContextAssembler|PetFeedPacketMetadataBridge|PetFeedServiceOperationPlanner|PetFeedEvaluation|PetFeedXmlProjection|PetFoodTypeLookup|PetFeedPlanner|PetFeedCalculator|PetFeedProgress|PetHungryLevel|SmPet|SmEmotion|SmSystemMessage|SmInventoryAddItem|SmWarehouseAddItem|SmLegionEdit|SmCubeUpdate"` passed 210 tests.
- `dotnet build dotnetConversion/src/Aion.GameServer/Aion.GameServer.csproj` passed with 0 warnings and 0 errors.

No live storage lookup, inventory mutation, packet send, live item/template/player/account/legion/house/pet hydration, scheduler execution, reward item creation, DAO write, or Java runtime packet comparison was enabled.

## Migration Parity Table - UOW-1340

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE.cubeSize` unusual storage fallback | `Aion.GameServer.Network.Aion.ServerPackets.SmCubeUpdate.ZeroSizeForJavaStorageOrdinal` | Packet Helper | Partial | Unit Tested | Partial Parity | Helper preserves Java action `0`, Java ordinal action value, and zero count/expand fields for supplied ordinals. It does not resolve Java storage ids or compare runtime bytes. |
| `com.aionemu.gameserver.model.items.storage.StorageType` pet bag ordinals | `GamePacketTests.SmCubeUpdate_ZeroSizeForJavaStorageOrdinalWritesUnusualStorageOrdinalsLikeJava` | Enum / Packet Metadata | Partial | Unit Tested | Partial Parity | Tests cover pet bag ordinals `4` through `15`. Storage id to ordinal mapping remains caller-supplied and needs a future resolver if live unusual storage is enabled. |
| `com.aionemu.gameserver.model.items.storage.StorageType` house storage ordinals | `GamePacketTests.SmCubeUpdate_ZeroSizeForJavaStorageOrdinalWritesUnusualStorageOrdinalsLikeJava` | Enum / Packet Metadata | Partial | Unit Tested | Partial Parity | Tests cover house storage ordinals `16` through `35`. Live house ownership/runtime storage remains unsupported. |
| `com.aionemu.gameserver.model.items.storage.StorageType.BROKER` / `MAILBOX` ordinals | `GamePacketTests.SmCubeUpdate_ZeroSizeForJavaStorageOrdinalWritesUnusualStorageOrdinalsLikeJava` | Enum / Packet Metadata | Partial | Unit Tested | Partial Parity | Tests cover broker ordinal `36` and mailbox ordinal `37`. Broker/mailbox live ownership and reachability remain unverified. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendStorageUpdatePacket` unusual storage default branch | future pet/house/broker/mailbox unlock adapter | Packet Service / Unsupported Branch | Not Started | No Tests | Needs Verification | This unit adds only the trailing cube-update helper. `SM_WAREHOUSE_ADD_ITEM` default-branch construction and live ordering remain future work. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `SmCubeUpdate_ZeroSizeForJavaStorageOrdinalWritesUnusualStorageOrdinalsLikeJava` | Unit | `SM_CUBE_UPDATE.cubeSize`; `StorageType` enum order | Ordinals `4` through `37` serialize action `0`, the Java ordinal action value, and zero count/expand fields. | Source-derived packet payload validation. | Does not compare Java runtime bytes or resolve storage ids. |
| `SmCubeUpdate_ZeroSizeForJavaStorageOrdinalRejectsOutOfByteRangeActionValues` | Unit | Java writes action value with `writeC` | Rejects negative and above-byte action values before packet construction. | C# guard for packet byte range. | Java exact exception behavior is not compared; this is a C# safety guard. |

## Remaining Risks

- The helper accepts ordinals, not storage ids; future live code still needs a Java storage-id to ordinal resolver if unusual storage is enabled.
- Java runtime packet bytes are still unavailable.
- `SM_WAREHOUSE_ADD_ITEM` default-branch unusual storage behavior is not wired to the helper.
- Live pet/house/broker/mailbox ownership and storage hydration are unsupported.
- Packet send ordering remains non-live metadata only.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 packet helper plus 2 focused tests covering 36 cases
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: live unusual-storage unlock adapter, storage-id-to-ordinal resolver, Java runtime packet artifacts, live ownership/storage hydration, socket dispatch
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a non-live Java storage-id to ordinal resolver for known unusual storage ids, then feed it into future rejected-food unlock metadata only after tests prove id-vs-ordinal behavior for pet bags, house storage, broker, and mailbox.
