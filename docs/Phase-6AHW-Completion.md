# Phase 6 AHW Completion - Warehouse-Add Cleanup/Seal Flag Input

Date: 2026-05-27
Unit of Work: UOW-1395
Status: `SmWarehouseAddItem` now supports explicit cleanup/seal general-info flag input and has focused packet coverage.

## Completed

- Added `generalInfoWarehouseRestrictionFlag` input to `SmWarehouseAddItem.CreateAllSlot`.
- Added `GeneralInfoWarehouseRestrictionFlag` to `SmWarehouseAddItem.WarehousePacketItem`.
- Updated `SmWarehouseAddItem` to pass the flag into `SmInventoryInfo.WriteItemInfoBlob`.
- Added focused packet coverage proving a warehouse-add item can write cleanup/seal flag `3` inside the nested `GENERAL_INFO` blob.
- Added a warehouse item blob reader helper for packet tests.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GamePacketTests.SmWarehouseAddItem_WritesCleanupSealFlagInItemBlobLikeJava|FullyQualifiedName~GamePacketTests.SmInventoryInfo_WritesCleanupSealFlagFromRestrictionTableLikeJava" --no-restore`.
- Result: passed 2 tests.
- Ran `git diff --check`.

## Migration Parity Table - UOW-1395

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.serverpackets.SM_WAREHOUSE_ADD_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmWarehouseAddItem` | Packet | Partial | Regression Tested | Partial Parity | C# warehouse-add packets can now carry an explicit cleanup/seal flag into the shared item blob and focused tests verify flag `3`. Factory callers still need cleanup-table plumbing. |
| `com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteItemInfoBlob` | Serialization Utility | Partial | Regression Tested | Partial Parity | Reuses UOW-1394 explicit flag input. This unit does not change shared serializer behavior. |
| `com.aionemu.gameserver.network.aion.iteminfo.GeneralInfoBlobEntry` cleanup/seal flag | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteGeneralInfoBlob` | Serialization Entry | Partial | Regression Tested | Partial Parity | Warehouse-add packet coverage now confirms nested `GENERAL_INFO` flag `3`. Temporary-exchange and time-dependent fields remain unresolved. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendStorageUpdatePacket` non-cube branch | `Aion.GameServer.Services.ToyPet.PetFeedPacketMetadataBridge` warehouse-add metadata paths | Service / Packet Bridge | Partial | Manual Only | Needs Verification | Existing metadata bridge calls still use default zero because cleanup-table context is not present. Future unit should pass deterministic cleanup flag through metadata contexts. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GamePacketTests.SmWarehouseAddItem_WritesCleanupSealFlagInItemBlobLikeJava` | Regression / packet byte layout | `SM_WAREHOUSE_ADD_ITEM.writeItemInfo`, `ItemInfoBlob.getFullBlob`, `GeneralInfoBlobEntry` | `SM_WAREHOUSE_ADD_ITEM` wraps an item blob whose `GENERAL_INFO` cleanup/seal field is `3` when explicit flag input is supplied. | Deterministic Java packet/source behavior and C# packet-byte assertion. | Does not compare generated Java bytes and does not wire service call sites. |
| `GamePacketTests.SmInventoryInfo_WritesCleanupSealFlagFromRestrictionTableLikeJava` | Regression / packet byte layout | `GeneralInfoBlobEntry` and `ItemRestrictionCleanupData` | Guards the shared item-blob cleanup flag path used by warehouse add. | Existing C# packet-byte assertion. | Not a Java runtime comparison. |

## Remaining Risks

- `SmWarehouseAddItem.CreateAllSlot` callers still default the flag to zero unless they supply it explicitly.
- Pet-feed unusual-storage metadata bridge paths do not yet carry cleanup-table or precomputed cleanup flag context.
- Inventory add/update and mail attached item packets still default cleanup/seal flag to zero.
- Temporary-exchange remaining seconds still serialize as zero.
- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped artifact rows in this unit
- Total artifacts ported: 2 C# surfaces changed
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: pet-feed/warehouse-add cleanup flag context plumbing, inventory add/update cleanup flag context, mail attachment cleanup flag context, Java runtime artifact generation, temporary-exchange model/template fields, time-normalized expiration/dye comparison
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Work Options

## Recommended Sequential Task

- Task: pass cleanup/seal flag context through pet-feed unusual-storage metadata bridge warehouse-add paths.
- Scope:
  - identify the metadata context DTO that can carry a precomputed `GeneralInfoWarehouseRestrictionFlag`;
  - keep packet serialization deterministic by passing an int/boolean rather than static data into the packet writer;
  - update the non-sending warehouse-add metadata tests to assert restricted item flag `3`;
  - keep Java runtime artifact byte comparison guarded.

## Safe Parallel Candidates

- Inventory add/update cleanup flag unit: add explicit flag fields to `SmInventoryAddItem` and `SmInventoryUpdateItem` with focused tests.
- Mail attached-item cleanup flag unit: add explicit flag/context support to `SmMailService` attached item serialization.
- Broker plume bridge unit: add template-aware `SmBrokerService` enchant-info calls and broker plume packet tests.

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Agent A | Pet-feed warehouse-add cleanup context | `PetFeedPacketMetadataBridge.cs`, pet-feed tests | `SmInventoryInfo.cs` |
| Agent B | Inventory add/update cleanup flag | `SmInventoryAddItem.cs`, `SmInventoryUpdateItem.cs`, focused packet tests | `PetFeedPacketMetadataBridge.cs` |
| Agent C | Broker plume bridge | `SmBrokerService.cs`, broker packet tests | `SmInventoryInfo.cs` |

## Do Not Parallelize

- Do not edit `SmInventoryInfo.cs` in parallel with packet-wrapper units; it is the shared serializer.
