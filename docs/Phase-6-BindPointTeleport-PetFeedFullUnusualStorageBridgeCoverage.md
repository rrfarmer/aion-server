# Phase 6 - Pet Feed Full Unusual Storage Bridge Coverage

Date: May 27, 2026
Unit of Work: UOW-1343

## Scope

This unit broadens the guarded unusual-storage rejected-food metadata bridge tests from representative ids to every known Java pet bag, house storage, broker, and mailbox id.

Java source of truth:

- `com.aionemu.gameserver.model.items.storage.StorageType`
- `com.aionemu.gameserver.services.item.ItemPacketService.sendStorageUpdatePacket`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_WAREHOUSE_ADD_ITEM`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE`
- `com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob`

## Implemented

- Expanded `Construct_RejectedFoodWithGuardedUnusualStorageContextBuildsWarehouseAddAndZeroCubeUpdate`.
- The bridge test now covers:
  - pet bag ids `32` through `43` mapping to cube-update action values `4` through `15`
  - house storage ids `60` through `79` mapping to cube-update action values `16` through `35`
  - broker id `126` mapping to action value `36`
  - mailbox id `127` mapping to action value `37`
- No production code changed in this unit.
- Integrated read-only item mutation timing analysis for future live work.

## Java Timing Notes

`SM_WAREHOUSE_ADD_ITEM` construction stores the warehouse type, add type, player reference, and a singleton list containing the live `Item` reference. It does not snapshot most item fields at construction time.

At serialization time, Java reads:

- `addType.getMask()`
- item object id
- item template id and localized name
- full `ItemInfoBlob`
- trailing equipment slot

`ItemInfoBlob.getFullBlob` is also built at serialization time. It can read live item state such as count, mask, creator, expiration, temporary exchange time, equipped/equipment slot, pack count, fusion, conditioning, enchant/skin/socket/godstone/idian/tempering/amplification data, dye/color, and bonus/tune info.

This matters because delayed rejected pet feed keeps the same mutable `Item` reference. C# snapshot-based metadata can diverge from Java if item count, slot, template/blob facts, or storage-derived routing changes between packet queue time and serialization time.

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "PetFeedPacketMetadataBridge"` passed 46 tests.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "PetFeedSubtype7JavaVectorArtifactReader|PetFeedRejectedFoodMetadataComposition|PetFeedUnlockPacketContextAssembler|PetFeedPacketMetadataBridge|PetFeedServiceOperationPlanner|PetFeedEvaluation|PetFeedXmlProjection|PetFoodTypeLookup|PetFeedPlanner|PetFeedCalculator|PetFeedProgress|PetHungryLevel|SmPet|SmEmotion|SmSystemMessage|SmInventoryAddItem|SmWarehouseAddItem|SmLegionEdit|SmCubeUpdate"` passed 292 tests.
- `dotnet build dotnetConversion/src/Aion.GameServer/Aion.GameServer.csproj` passed with 0 warnings and 0 errors.

No live storage lookup, inventory mutation, packet send, live item/template/player/account/legion/house/pet hydration, scheduler execution, reward item creation, DAO write, or Java runtime packet comparison was enabled.

## Migration Parity Table - UOW-1343

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.items.storage.StorageType` pet bag ids | `PetFeedPacketMetadataBridgeTests.Construct_RejectedFoodWithGuardedUnusualStorageContextBuildsWarehouseAddAndZeroCubeUpdate` | Enum / Packet Metadata | Partial | Unit Tested | Partial Parity | Bridge tests now cover pet bag ids `32` through `43` and ordinals `4` through `15`. Live pet bag ownership/storage context remains unsupported. |
| `com.aionemu.gameserver.model.items.storage.StorageType` house storage ids | `PetFeedPacketMetadataBridgeTests.Construct_RejectedFoodWithGuardedUnusualStorageContextBuildsWarehouseAddAndZeroCubeUpdate` | Enum / Packet Metadata | Partial | Unit Tested | Partial Parity | Bridge tests now cover house storage ids `60` through `79` and ordinals `16` through `35`. Live house storage ownership remains unsupported. |
| `com.aionemu.gameserver.model.items.storage.StorageType.BROKER` / `MAILBOX` | `PetFeedPacketMetadataBridgeTests.Construct_RejectedFoodWithGuardedUnusualStorageContextBuildsWarehouseAddAndZeroCubeUpdate` | Enum / Packet Metadata | Partial | Unit Tested | Partial Parity | Bridge tests cover broker `126`/ordinal `36` and mailbox `127`/ordinal `37`. Runtime reachability is defensive/source-possible but not normal UI flow. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_WAREHOUSE_ADD_ITEM` | `SmWarehouseAddItem`; bridge tests | Packet | Partial | Unit Tested | Partial Parity | Tests verify warehouse type id, ALL_SLOT, item header/blob presence, and slot for all unusual storage ids. Java reads most item/blob fields at encode time, which C# snapshots do not yet model live. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE.cubeSize` unusual storage fallback | `SmCubeUpdate.ZeroSizeForJavaStorageId`; bridge tests | Packet | Partial | Unit Tested | Partial Parity | Tests verify id-to-ordinal zero-count payloads for every unusual id. No Java runtime bytes exist. |
| `com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob` | `SmInventoryInfo.WriteItemInfoBlob`; bridge tests | Serialization Helper | Partial | Unit Tested presence only | Needs Verification | Tests assert blob presence, not byte-for-byte Java blob parity for unusual storage or encode-time item mutation behavior. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Construct_RejectedFoodWithGuardedUnusualStorageContextBuildsWarehouseAddAndZeroCubeUpdate` | Unit | `StorageType`, `SM_WAREHOUSE_ADD_ITEM`, `SM_CUBE_UPDATE.cubeSize` source review | All pet bag, house storage, broker, and mailbox ids serialize warehouse-add metadata with Java storage id and trailing zero-count cube update with Java ordinal. | Source-derived packet metadata validation for all known unusual ids. | Does not compare Java runtime bytes or encode-time mutation behavior. |

## Remaining Risks

- Java reads most `SM_WAREHOUSE_ADD_ITEM` item/blob fields at encode time, not construction time.
- C# metadata currently uses supplied snapshots and can diverge from Java if item state mutates between queue and serialization.
- Broker/mailbox paths are source-possible through delayed mutable references but not expected ordinary UI flow.
- Full Java item blob bytes for unusual storage paths are not captured.
- Live pet/house/broker/mailbox ownership, storage hydration, mutation, and dispatch remain disabled.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 1 expanded test theory covering 34 unusual storage ids
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: Java runtime packet artifacts, live ownership/storage hydration, encode-time item mutation parity, live unusual-storage unlock adapter, socket dispatch
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a docs-only runtime artifact schema for unusual-storage unlock packets that captures both construction-time route fields and encode-time item/blob fields before any live unusual-storage dispatch is enabled.
