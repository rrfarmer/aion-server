# Phase 6 - Pet Feed Full House Storage Coverage

Date: May 27, 2026
Unit of Work: UOW-1338

## Scope

This unit closes the test coverage gap left after UOW-1337 for Java house storage ids used by rejected-food unlock packet planning.

Java source of truth:

- `com.aionemu.gameserver.model.items.storage.StorageType`
- `com.aionemu.gameserver.services.item.ItemPacketService.sendItemUnlockPacket`
- `com.aionemu.gameserver.services.item.ItemPacketService.sendStorageUpdatePacket`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_WAREHOUSE_ADD_ITEM`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE`

## Java Behavior Reviewed

`StorageType` defines house cabinet storage ids `60` through `79`:

- `HOUSE_STORAGE_01` through `HOUSE_STORAGE_08`: ids `60` through `67`, limit `9`, length `9`
- `HOUSE_STORAGE_09` through `HOUSE_STORAGE_14`: ids `68` through `73`, limit `18`, length `9`
- `HOUSE_STORAGE_15` through `HOUSE_STORAGE_20`: ids `74` through `79`, limit `27`, length `9`

`ItemPacketService.sendItemUnlockPacket` resolves `item.getItemLocation()` through `StorageType.getStorageTypeById`. If the id is known, Java calls `sendStorageUpdatePacket(player, storageType, item, ItemAddType.ALL_SLOT)`.

`sendStorageUpdatePacket` sends the unlock/add packet first, then sends `SM_CUBE_UPDATE.cubeSize(storageType, player)`. For non-cube and non-legion-kinah storage ids, Java falls through to `SM_WAREHOUSE_ADD_ITEM(item, storageType.getId(), player, addType)`.

`SM_CUBE_UPDATE.cubeSize` snapshots real item counts and expand counts only for:

- `CUBE`
- `REGULAR_WAREHOUSE`
- `LEGION_WAREHOUSE`

Account warehouse, pet bags, house storage, broker, and mailbox keep zero counts/expands but still write action `0` and `StorageType.ordinal()` as the action value.

## C# Boundary Decision

The C# rejected-food unlock assembler already recognizes all house storage ids `60` through `79` as known Java storage ids, but intentionally maps them to `UnsupportedStorageLocation`.

This unit makes that boundary explicit in tests for every Java house cabinet id rather than relying on representative samples. C# still does not emit generic warehouse metadata for house cabinets because live parity requires house ownership/runtime context and Java packet bytes for these unusual unlock paths.

## Implemented

- Expanded `PetFeedUnlockPacketContextAssemblerTests.Assemble_UnsupportedKnownStorageLocationsDoNotGuessPacketShape`.
- Added house storage ids `62` through `67`, `69` through `73`, and `75` through `78`.
- The theory now covers:
  - pet bag ids `32` through `43`
  - every house storage id `60` through `79`
  - broker id `126`
  - mailbox id `127`
- Integrated a read-only warehouse snapshot timing analysis for the next live-adapter handoff.

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "PetFeedUnlockPacketContextAssembler"` passed 41 tests.
- Broad pet-feed regression and build validation are recorded in the handoff/progress entry for this unit.

No live storage lookup, inventory mutation, packet send, live item/template/player/account/legion/house/pet hydration, scheduler execution, reward item creation, DAO write, or Java runtime packet comparison was enabled.

## Migration Parity Table - UOW-1338

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.items.storage.StorageType` house storage ids `60` through `79` | `Aion.GameServer.Services.ToyPet.PetFeedUnlockPacketContextAssembler.GetStorageKind`; `Aion.GameServer.Tests.PetFeedUnlockPacketContextAssemblerTests` | Enum / Mapping Boundary | Partial | Unit Tested | Partial Parity | All Java house cabinet ids are now tested as known but unsupported. C# intentionally blocks packet context rather than guessing generic warehouse behavior without house ownership/runtime bytes. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemUnlockPacket` | `Aion.GameServer.Services.ToyPet.PetFeedUnlockPacketContextAssembler.Assemble` | Service Boundary | Partial | Unit Tested | Partial Parity | Java null-storage sends nothing; known unsupported house ids now consistently return `UnsupportedStorageLocation`. No live send or runtime comparison exists. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendStorageUpdatePacket` default branch | future pet/house/broker/mailbox unlock adapter | Packet Service / Unsupported Branch | Not Started | Manual Only | Needs Verification | Java would send `SM_WAREHOUSE_ADD_ITEM` followed by `SM_CUBE_UPDATE` for house ids. C# still blocks until live house storage context and Java bytes exist. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_WAREHOUSE_ADD_ITEM` | future pet/house/broker/mailbox unlock adapter | Packet / Future Runtime Comparison | Not Started | Manual Only | Needs Verification | Java serializes item/template/blob/slot using the supplied item and storage id. Existing C# modeled warehouse families do not prove house-storage behavior. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE.cubeSize` unusual storage fallback | `Aion.GameServer.Network.Aion.ServerPackets.SmCubeUpdate`; future live adapter | Packet / Size Snapshot | Partial | Unit Tested for modeled families | Needs Verification | Java zero-fills counts/expands for account warehouse, pet bags, house storage, broker, and mailbox but uses `StorageType.ordinal()`. Future adapter must preserve this unless an intentional difference is documented. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Assemble_UnsupportedKnownStorageLocationsDoNotGuessPacketShape` | Unit | `StorageType` id range and `sendItemUnlockPacket` source review | Every Java house storage id `60` through `79`, plus pet bags/broker/mailbox, returns `UnsupportedStorageLocation` with no packet context. | Source-derived conservative boundary assertion. | Does not execute Java default warehouse branch, compare runtime bytes, or model live house ownership. |

## Remaining Risks

- Java would still route known house ids through the generic warehouse add/update branch; C# intentionally blocks this until runtime context exists.
- `SM_CUBE_UPDATE.cubeSize` uses `StorageType.ordinal()` rather than the storage id, so future unusual-storage packet metadata needs ordinal parity checks.
- Java packet objects hold item/player references; without runtime artifacts, serialization-time mutation risk remains unverified.
- Taking live storage counts too early could produce stale cube-size metadata; taking item fields too late could serialize mutated item state.
- Live storage lookup, mutation, packet dispatch, scheduler execution, reward creation, DAO writes, and socket dispatch remain disabled.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 expanded test theory now covering 34 known unsupported storage ids
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: house storage live context, generic warehouse runtime bytes for house ids, unusual-storage cube-update ordinal comparison, live storage lookup/mutation/dispatch
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a docs-only warehouse live-adapter capture design that defines the future synchronous snapshot boundary for rejected-food unlocks: resolve storage id at unlock entry, snapshot item/template context after restore/unlock decision and storage mutation, queue add/unlock metadata first, then immediately snapshot `SM_CUBE_UPDATE` counts/expands without yielding. Keep live feed dispatch disabled.
