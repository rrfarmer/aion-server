# Phase 6 AHX Completion - Pet-Feed Cleanup/Seal Warehouse-Add Context

Date: 2026-05-27
Unit of Work: UOW-1396
Status: pet-feed warehouse-add metadata paths now carry explicit cleanup/seal general-info flag context.

## Completed

- Added `GeneralInfoWarehouseRestrictionFlag` to `PetFeedUnlockPacketContext`.
- Added `GeneralInfoWarehouseRestrictionFlag` to `PetFeedUnlockPacketContextAssemblerInput` and propagated it to the bridge context.
- Updated regular warehouse, account warehouse, legion warehouse item, and guarded unusual-storage pet-feed metadata construction to pass the explicit flag into `SmWarehouseAddItem.CreateAllSlot`.
- Preserved default-zero behavior for callers without cleanup/seal context.
- Added focused bridge packet coverage proving restricted item id `188053996` writes nested `GENERAL_INFO` cleanup/seal flag `3`.
- Added assembler coverage proving precomputed cleanup/seal flags survive context assembly.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PetFeedPacketMetadataBridgeTests.Construct_RejectedFoodWithCleanupSealContextWritesWarehouseAddFlagLikeJava|FullyQualifiedName~PetFeedUnlockPacketContextAssemblerTests.Assemble_WarehouseLocationsPreservePrecomputedCleanupSealFlag|FullyQualifiedName~GamePacketTests.SmWarehouseAddItem_WritesCleanupSealFlagInItemBlobLikeJava" --no-restore`.
- Result: passed 8 tests.
- Ran `git diff --check`.

## Migration Parity Table - UOW-1396

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemUnlockPacket` | `Aion.GameServer.Services.ToyPet.PetFeedUnlockPacketContextAssembler` | Service / Context Assembler | Partial | Unit Tested | Partial Parity | C# now carries a precomputed cleanup/seal flag through the non-live unlock packet context. Java computes the flag inside `GeneralInfoBlobEntry` from `DataManager.ITEM_CLEAN_UP`; C# intentionally keeps the bridge deterministic by passing a scalar. Unsupported unusual storage ids remain guarded. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendStorageUpdatePacket` non-cube branch | `Aion.GameServer.Services.ToyPet.PetFeedPacketMetadataBridge` | Service / Packet Bridge | Partial | Regression Tested | Partial Parity | Regular, account, legion item, and guarded unusual-storage warehouse-add metadata paths now pass the explicit cleanup/seal flag into `SmWarehouseAddItem`. Kinah legion branch remains a `SmLegionEdit` special case and has no item blob. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_WAREHOUSE_ADD_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmWarehouseAddItem` | Packet | Partial | Regression Tested | Partial Parity | Existing explicit flag input is now consumed by pet-feed metadata contexts. Runtime Java artifact byte comparison remains blocked by missing generated artifacts and unresolved temporary-exchange/time-dependent fields. |
| `com.aionemu.gameserver.network.aion.iteminfo.GeneralInfoBlobEntry` cleanup/seal flag | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteGeneralInfoBlob` | Serialization Entry | Partial | Regression Tested | Partial Parity | New bridge tests assert the nested `GENERAL_INFO` cleanup/seal `H` field is `3` for restricted item id `188053996`. Temporary-exchange, runtime conditioning presence, and time-normalized expiration/dye behavior remain unresolved. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `PetFeedPacketMetadataBridgeTests.Construct_RejectedFoodWithCleanupSealContextWritesWarehouseAddFlagLikeJava` | Regression / packet byte layout | `ItemPacketService.sendStorageUpdatePacket`, `SM_WAREHOUSE_ADD_ITEM.writeItemInfo`, `GeneralInfoBlobEntry` | Regular warehouse, account warehouse, legion warehouse item, and guarded unusual-storage metadata packets pass explicit cleanup/seal flag `3` into the nested warehouse-add item blob. | Deterministic Java source behavior and C# packet-byte assertion. | Does not compare generated Java runtime bytes and does not cover temporary-exchange or wall-clock fields. |
| `PetFeedUnlockPacketContextAssemblerTests.Assemble_WarehouseLocationsPreservePrecomputedCleanupSealFlag` | Unit | `ItemPacketService.sendItemUnlockPacket` storage context flow plus cleanup/seal source audit | Precomputed cleanup/seal flag context survives assembler creation for modeled warehouse locations. | Deterministic C# context propagation based on reviewed Java source boundary. | The assembler does not compute the flag from `ItemRestrictionCleanupTable`; callers must supply it. |
| `GamePacketTests.SmWarehouseAddItem_WritesCleanupSealFlagInItemBlobLikeJava` | Regression / packet byte layout | `SM_WAREHOUSE_ADD_ITEM.writeItemInfo`, `ItemInfoBlob.getFullBlob`, `GeneralInfoBlobEntry` | Guards the packet-level explicit cleanup/seal flag path consumed by pet-feed metadata. | Existing C# packet-byte assertion. | Not a Java runtime comparison. |

## Remaining Risks

- Runtime pet-feed/live service callers still need a source for the precomputed cleanup/seal flag when creating `PetFeedUnlockPacketContextAssemblerInput`.
- Inventory add/update and mail attached item packets still default cleanup/seal flag to zero.
- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling, so warehouse-add byte comparison remains guarded.
- Temporary-exchange remaining seconds still serialize as zero.
- Runtime conditioning presence and wall-clock expiration/dye fields can still differ from Java.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped artifact rows in this unit
- Total artifacts ported: 2 C# context/bridge surfaces changed
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: runtime cleanup flag source for live pet-feed context assembly, inventory add/update cleanup flag context, mail attachment cleanup flag context, Java runtime artifact generation, temporary-exchange model/template fields, time-normalized expiration/dye comparison
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Work Options

## Recommended Sequential Task

- Task: extend cleanup/seal flag plumbing to inventory add/update item blob packets.
- Scope:
  - add explicit cleanup/seal flag input/fields to `SmInventoryAddItem` and `SmInventoryUpdateItem`;
  - preserve default-zero behavior for callers without cleanup data;
  - add focused packet tests for restricted item id `188053996` expecting nested `GENERAL_INFO` flag `3`;
  - avoid changes to `SmInventoryInfo` unless a test proves the shared serializer needs adjustment.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Inventory add/update cleanup flag | `SmInventoryAddItem.cs`, `SmInventoryUpdateItem.cs`, focused packet tests | Medium | Shares packet-test file with many existing tests; keep edits tight. |
| B | Mail attached-item cleanup flag | `SmMailService.cs`, focused mail packet tests | Medium | Independent from inventory wrappers but still consumes shared item-blob serializer. |
| C | Broker plume bridge | `SmBrokerService.cs`, broker packet tests | Medium | Separate from cleanup/seal flag work; avoid `SmInventoryInfo.cs`. |
| D | Temporary exchange source model audit | read-only Java/C# analysis | Low | Good sidecar analysis if no code changes are needed. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Agent A | Inventory add/update cleanup flag | `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmInventoryAddItem.cs`, `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmInventoryUpdateItem.cs`, focused tests | `SmInventoryInfo.cs`, pet-feed files, docs |
| Agent B | Mail attached-item cleanup flag analysis | read-only `SmMailService.cs`, Java mail packet source | all writes |
| Agent C | Broker plume bridge analysis | read-only `SmBrokerService.cs`, Java broker packet source | all writes |

## Do Not Parallelize

- Do not edit `SmInventoryInfo.cs` concurrently with wrapper packet units; it is the shared item-blob serializer.
- Do not edit shared progress/handoff/parity docs from sub-agents; orchestrator owns docs.
