# Phase 6 - Pet Feed Guarded Unusual Storage Unlock Metadata

Date: May 27, 2026
Unit of Work: UOW-1342

## Scope

This unit adds a guarded, non-live metadata bridge for Java rejected-food unlock packets when the delayed Java pet-feed check observes an item in an unusual storage location.

Java source of truth:

- `com.aionemu.gameserver.services.toypet.PetService.checkFeeding`
- `com.aionemu.gameserver.services.item.ItemPacketService.sendItemUnlockPacket`
- `com.aionemu.gameserver.services.item.ItemPacketService.sendStorageUpdatePacket`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_WAREHOUSE_ADD_ITEM`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE`

C# artifacts:

- `Aion.GameServer.Services.ToyPet.PetFeedPacketMetadataBridge`
- `Aion.GameServer.Services.ToyPet.PetFeedUnlockPacketStorageKind`
- `Aion.GameServer.Tests.PetFeedPacketMetadataBridgeTests`

## Java Behavior Reviewed

`PetService.checkFeeding` looks up the item from cube inventory before scheduling the delayed check. In ordinary client flow, rejected-food unlocks should therefore start from cube.

However, Java keeps the same mutable `Item` reference across the delayed check. If that item is moved, registered, or attached before the rejection branch runs, `sendItemUnlockPacket` uses the item's current `itemLocation`.

`ItemPacketService.sendItemUnlockPacket`:

- reads `item.getItemLocation()`
- resolves it through `StorageType.getStorageTypeById`
- sends nothing if the id is unknown
- otherwise calls `sendStorageUpdatePacket(player, storageType, item, ItemAddType.ALL_SLOT)`

For non-cube unusual storage ids, Java falls through to:

1. `SM_WAREHOUSE_ADD_ITEM(item, storageType.getId(), player, ALL_SLOT)`
2. `SM_CUBE_UPDATE.cubeSize(storageType, player)`

The warehouse add packet writes the Java storage id. The cube update writes the Java enum ordinal and zero count/expand fields for pet bags, house storage, broker, and mailbox.

## Implemented

- Added `PetFeedUnlockPacketStorageKind.UnusualWarehouse`.
- Added guarded bridge construction for manually supplied unusual-storage unlock context.
- The bridge now pairs:
  - `SmWarehouseAddItem.CreateAllSlot(context.Item.Location, ...)`
  - `SmCubeUpdate.ZeroSizeForJavaStorageId(context.Item.Location)`
- Added tests for representative unusual storage ids:
  - pet bag id `32` to cube-update ordinal `4`
  - house storage id `60` to cube-update ordinal `16`
  - broker id `126` to cube-update ordinal `36`
  - mailbox id `127` to cube-update ordinal `37`
- Added a guard test proving unknown ids still block.

The live assembler still returns `UnsupportedStorageLocation` for pet/house/broker/mailbox ids. This unit only enables non-live metadata construction when tests or future callers supply explicit guarded context.

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "PetFeedPacketMetadataBridge"` passed 16 tests.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "PetFeedSubtype7JavaVectorArtifactReader|PetFeedRejectedFoodMetadataComposition|PetFeedUnlockPacketContextAssembler|PetFeedPacketMetadataBridge|PetFeedServiceOperationPlanner|PetFeedEvaluation|PetFeedXmlProjection|PetFoodTypeLookup|PetFeedPlanner|PetFeedCalculator|PetFeedProgress|PetHungryLevel|SmPet|SmEmotion|SmSystemMessage|SmInventoryAddItem|SmWarehouseAddItem|SmLegionEdit|SmCubeUpdate"` passed 262 tests.
- `dotnet build dotnetConversion/src/Aion.GameServer/Aion.GameServer.csproj` passed with 0 warnings and 0 errors.

No live storage lookup, inventory mutation, packet send, live item/template/player/account/legion/house/pet hydration, scheduler execution, reward item creation, DAO write, or Java runtime packet comparison was enabled.

## Migration Parity Table - UOW-1342

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.toypet.PetService.checkFeeding` rejected-food delayed item reference | `PetFeedPacketMetadataBridge`; reachability audit notes | Service Flow | Partial | Manual Only | Needs Verification | Java starts from cube in normal flow, but the delayed task keeps a mutable item reference. Moved/registered/mailed items can expose unusual storage ids before rejection. No live scheduler/runtime comparison exists. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemUnlockPacket` | `PetFeedPacketMetadataBridge.ConstructUnusualWarehouseItemUnlock` | Service Boundary | Partial | Unit Tested | Partial Parity | Guarded bridge path requires explicit `UnusualWarehouse` context and known Java storage id. Unknown ids still block; live assembler still blocks unusual ids. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendStorageUpdatePacket` default branch | `PetFeedPacketMetadataBridge.ConstructUnusualWarehouseItemUnlock` | Packet Service / Metadata Bridge | Partial | Unit Tested | Partial Parity | Tests verify `SM_WAREHOUSE_ADD_ITEM` first and zero-count `SM_CUBE_UPDATE` second for representative unusual ids. No live dispatch or Java runtime byte comparison exists. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_WAREHOUSE_ADD_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmWarehouseAddItem` | Packet | Partial | Unit Tested | Partial Parity | Guarded tests verify storage id, ALL_SLOT mask, item header/blob presence, and equipment slot. Full Java item blob/runtime bytes still need artifacts. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE.cubeSize` unusual storage fallback | `Aion.GameServer.Network.Aion.ServerPackets.SmCubeUpdate.ZeroSizeForJavaStorageId` | Packet | Partial | Unit Tested | Partial Parity | Tests verify representative id-to-ordinal zero-count payloads. No Java runtime artifacts exist. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Construct_RejectedFoodWithGuardedUnusualStorageContextBuildsWarehouseAddAndZeroCubeUpdate` | Unit | `ItemPacketService.sendStorageUpdatePacket`; `SM_WAREHOUSE_ADD_ITEM`; `SM_CUBE_UPDATE.cubeSize`; `StorageType` enum order | Representative unusual storage ids construct `SmWarehouseAddItem` with storage id and trailing zero-count `SmCubeUpdate` with Java ordinal. | Source-derived packet metadata validation. | Does not compare Java runtime bytes, cover every unusual id in bridge tests, or enable live dispatch. |
| `Construct_RejectedFoodWithGuardedUnusualStorageContextRejectsUnknownStorageId` | Unit | `StorageType.getStorageTypeById` null boundary | Unknown storage id blocks guarded unusual metadata. | Source-derived guard validation. | Java would send nothing; bridge reports blocked metadata because it is not a live sender. |

## Remaining Risks

- Normal pet-feed flow starts from cube; unusual storage reachability depends on delayed mutable item references and race/crafted-client scenarios.
- Broker/mailbox paths are source-possible but not expected ordinary UI flow.
- Full Java item blob bytes for unusual storage paths are not captured.
- Live pet/house/broker/mailbox ownership, storage hydration, mutation, and dispatch remain disabled.
- The assembler still blocks unusual ids; this bridge path is intentionally guarded supplemental metadata only.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 guarded metadata bridge branch plus 2 focused tests covering 5 cases
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: live unusual-storage unlock adapter, Java runtime packet artifacts, live ownership/storage hydration, full unusual-id bridge coverage, socket dispatch
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Broaden guarded unusual-storage bridge coverage from representative ids to all known Java pet bag, house storage, broker, and mailbox ids, while keeping the live assembler blocked.
