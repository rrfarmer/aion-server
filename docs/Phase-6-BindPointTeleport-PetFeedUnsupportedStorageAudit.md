# Phase 6 - Pet Feed Unsupported Storage Audit

Date: May 27, 2026
Unit of Work: UOW-1337

## Scope

This unit audits Java `StorageType` behavior for pet feed rejected-food unlock packets and hardens C# test coverage for known but intentionally unsupported storage ids.

Java source of truth:

- `com.aionemu.gameserver.model.items.storage.StorageType`
- `com.aionemu.gameserver.services.item.ItemPacketService.sendItemUnlockPacket`
- `com.aionemu.gameserver.services.item.ItemPacketService.sendStorageUpdatePacket`

## Java Behavior Reviewed

`ItemPacketService.sendItemUnlockPacket` maps `item.getItemLocation()` through `StorageType.getStorageTypeById`.

- If Java returns `null`, it sends nothing.
- If Java returns any `StorageType`, it calls `sendStorageUpdatePacket(player, storageType, item, ItemAddType.ALL_SLOT)`.

`sendStorageUpdatePacket` handles:

- `CUBE`: sends `SM_INVENTORY_ADD_ITEM`, then `SM_CUBE_UPDATE.cubeSize(storageType, player)`.
- `LEGION_WAREHOUSE` kinah: sends `SM_LEGION_EDIT`, then `SM_CUBE_UPDATE.cubeSize(storageType, player)`.
- all other non-cube storage types: sends `SM_WAREHOUSE_ADD_ITEM(item, storageType.getId(), player, addType)`, then `SM_CUBE_UPDATE.cubeSize(storageType, player)`.

Java `StorageType` known ids include:

- cube and warehouse ids: `0`, `1`, `2`, `3`
- pet bag ids: `32` through `43`
- house cabinet ids: `60` through `79`
- broker id: `126`
- mailbox id: `127`

## C# Boundary Decision

The current C# rejected-food unlock assembler supports only packet families that have been explicitly modeled:

- cube
- regular warehouse
- account warehouse
- legion warehouse item
- legion warehouse kinah

The assembler intentionally blocks known Java storage ids for pet bags, house cabinets, broker, and mailbox instead of treating all of them as generic warehouse packets. This is conservative because the future live adapter still lacks:

- pet bag ownership and storage object hydration
- house cabinet ownership and housing storage context
- broker/mailbox runtime ownership semantics
- Java runtime packet bytes for these unusual unlock paths
- live storage mutation/packet send ordering

## Implemented

- Expanded `PetFeedUnlockPacketContextAssemblerTests.Assemble_UnsupportedKnownStorageLocationsDoNotGuessPacketShape` into a theory.
- Added explicit coverage for:
  - all Java pet bag ids `32` through `43`
  - representative house cabinet ids `60`, `61`, `68`, `74`, and `79`
  - broker id `126`
  - mailbox id `127`
- Kept unknown storage id behavior separate: unknown ids still map to Java's null-storage no-send boundary.

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "PetFeedUnlockPacketContextAssembler"` passed 26 tests.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "PetFeedSubtype7JavaVectorArtifactReader|PetFeedRejectedFoodMetadataComposition|PetFeedUnlockPacketContextAssembler|PetFeedPacketMetadataBridge|PetFeedServiceOperationPlanner|PetFeedEvaluation|PetFeedXmlProjection|PetFoodTypeLookup|PetFeedPlanner|PetFeedCalculator|PetFeedProgress|PetHungryLevel|SmPet|SmEmotion|SmSystemMessage|SmInventoryAddItem|SmWarehouseAddItem|SmLegionEdit|SmCubeUpdate"` passed 159 tests.
- `dotnet build dotnetConversion/src/Aion.GameServer/Aion.GameServer.csproj` passed with 0 warnings and 0 errors.

## Migration Parity Table - UOW-1337

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.items.storage.StorageType` pet bag ids | `PetFeedUnlockPacketContextAssembler.GetStorageKind`; `PetFeedUnlockPacketContextAssemblerTests` | Enum / Mapping Boundary | Partial | Unit Tested | Partial Parity | C# recognizes ids `32` through `43` as known but unsupported. It intentionally does not guess packet shape or ownership semantics. |
| `com.aionemu.gameserver.model.items.storage.StorageType` house storage ids | `PetFeedUnlockPacketContextAssembler.GetStorageKind`; tests | Enum / Mapping Boundary | Partial | Unit Tested | Partial Parity | Representative ids `60`, `61`, `68`, `74`, and `79` are tested as unsupported. Full range is recognized in production mapping but not fully enumerated in tests. |
| `com.aionemu.gameserver.model.items.storage.StorageType.BROKER` / `MAILBOX` | `PetFeedUnlockPacketContextAssembler.GetStorageKind`; tests | Enum / Mapping Boundary | Partial | Unit Tested | Partial Parity | Broker `126` and mailbox `127` are known unsupported ids. Live ownership/update behavior remains unported. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemUnlockPacket` | `PetFeedUnlockPacketContextAssembler.Assemble` | Service Boundary | Partial | Unit Tested | Partial Parity | Unknown ids still produce no context, matching Java null-storage no-send behavior. Known but unsupported ids block rather than emit generic warehouse metadata. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendStorageUpdatePacket` default branch | future pet/house/broker/mailbox unlock adapter | Packet Service / Unsupported Branch | Not Started | Manual Only | Needs Verification | Java would use generic warehouse add/update for most non-cube storage types. C# does not yet model pet/house/broker/mailbox live storage context or Java runtime bytes. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Assemble_UnsupportedKnownStorageLocationsDoNotGuessPacketShape` | Unit | `StorageType` ids and `sendItemUnlockPacket` source review | Known pet-bag, house-storage, broker, and mailbox ids return `UnsupportedStorageLocation` with no packet context. | Source-derived conservative boundary assertion. | Does not execute Java generic warehouse default branch or compare runtime bytes. |

## Remaining Risks

- Java would enter the default `sendStorageUpdatePacket` branch for many known non-cube storage types, but C# intentionally blocks those until ownership/runtime context is modeled.
- House storage range is production-recognized as unsupported, but tests sample representative ids rather than every id `60` through `79`.
- Broker and mailbox unlock behavior may be unreachable in normal pet feed flow, but Java source would resolve the ids.
- No Java runtime packet capture exists for pet/house/broker/mailbox rejected-food unlock paths.
- Live storage lookup, mutation, packet dispatch, scheduler execution, reward creation, DAO writes, and socket dispatch remain disabled.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 expanded test theory covering 19 known unsupported storage ids
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: pet bag storage context, house storage context, broker/mailbox ownership, generic warehouse runtime bytes for unusual storage ids, live storage lookup/mutation/dispatch
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Either implement the inert Java serialization observer hook from UOW-1336, or add a docs-only warehouse live-adapter capture design that specifies when future C# should snapshot storage item counts/expands around rejected-food unlock execution. Keep live feed dispatch disabled.

## Follow-up After UOW-1338

`PetFeedUnlockPacketContextAssemblerTests.Assemble_UnsupportedKnownStorageLocationsDoNotGuessPacketShape` now enumerates every Java house storage id `60` through `79`, not just representative samples. The conservative unsupported boundary remains unchanged.
