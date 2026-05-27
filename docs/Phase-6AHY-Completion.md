# Phase 6 AHY Completion - Inventory Add/Update Cleanup-Seal Flag Input

Date: 2026-05-27
Unit of Work: UOW-1397
Status: inventory add/update packet wrappers now support explicit cleanup/seal general-info flag input.

## Completed

- Added optional `generalInfoWarehouseRestrictionFlag` inputs to `SmInventoryAddItem` factory methods.
- Added `GeneralInfoWarehouseRestrictionFlag` to `SmInventoryAddItem.InventoryPacketItem`.
- Updated `SmInventoryAddItem` to pass the explicit flag into `SmInventoryInfo.WriteItemInfoBlob`.
- Added optional `generalInfoWarehouseRestrictionFlag` input to `SmInventoryUpdateItem`.
- Updated the normal full-blob `SmInventoryUpdateItem` path to pass the explicit flag.
- Preserved charge/polish partial-blob behavior and default-zero behavior for existing callers.
- Added focused packet coverage proving restricted item id `188053996` writes cleanup/seal flag `3` in `SM_INVENTORY_ADD_ITEM` and `SM_INVENTORY_UPDATE_ITEM`.

## Validation

- First targeted run caught a test expectation issue: the synthetic template had `DescriptionId == 0`, so `GetClientName()` correctly serialized an empty string.
- Reran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GamePacketTests.SmInventoryAddItem_WritesCleanupSealFlagInItemBlobLikeJava|FullyQualifiedName~GamePacketTests.SmInventoryUpdateItem_WritesCleanupSealFlagInItemBlobLikeJava|FullyQualifiedName~GamePacketTests.SmWarehouseAddItem_WritesCleanupSealFlagInItemBlobLikeJava" --no-restore`.
- Result: passed 3 tests.
- Ran `git diff --check`.

## Migration Parity Table - UOW-1397

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_ADD_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryAddItem` | Packet | Partial | Regression Tested | Partial Parity | C# inventory-add packets can now carry an explicit cleanup/seal flag into the shared item blob and focused tests verify flag `3`. Runtime callers still need cleanup-table or precomputed context plumbing. Java partial-with-slot mask behavior for `ITEM_COLLECT` remains outside this unit. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem` | Packet | Partial | Regression Tested | Partial Parity | C# normal full-blob inventory-update packets can now carry an explicit cleanup/seal flag. Charge and polish partial-blob branches intentionally do not write general-info and remain unchanged. Runtime callers still default to zero unless they pass the flag. |
| `com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteItemInfoBlob` | Serialization Utility | Partial | Regression Tested | Partial Parity | Reuses the UOW-1394 explicit flag input. This unit does not change shared serializer behavior. |
| `com.aionemu.gameserver.network.aion.iteminfo.GeneralInfoBlobEntry` cleanup/seal flag | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteGeneralInfoBlob` | Serialization Entry | Partial | Regression Tested | Partial Parity | Inventory add/update packet coverage now confirms nested `GENERAL_INFO` flag `3`. Temporary-exchange and time-dependent fields remain unresolved. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendStorageUpdatePacket` cube branch | future runtime caller plumbing into `SmInventoryAddItem` | Service / Packet Orchestration | Partial | No Tests | Needs Verification | Wrapper packet accepts the flag, but live storage update callers do not yet compute/pass `ItemRestrictionCleanupTable` context for cube add/update sends. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GamePacketTests.SmInventoryAddItem_WritesCleanupSealFlagInItemBlobLikeJava` | Regression / packet byte layout | `SM_INVENTORY_ADD_ITEM.writeItemInfo`, `ItemInfoBlob.getFullBlob`, `GeneralInfoBlobEntry` | `SM_INVENTORY_ADD_ITEM` wraps an item blob whose `GENERAL_INFO` cleanup/seal field is `3` when explicit flag input is supplied. | Deterministic Java packet/source behavior and C# packet-byte assertion. | Does not compare generated Java bytes and does not wire service call sites. |
| `GamePacketTests.SmInventoryUpdateItem_WritesCleanupSealFlagInItemBlobLikeJava` | Regression / packet byte layout | `SM_INVENTORY_UPDATE_ITEM.writeImpl`, `ItemInfoBlob.getFullBlob`, `GeneralInfoBlobEntry` | Normal full-blob `SM_INVENTORY_UPDATE_ITEM` writes cleanup/seal field `3` when explicit flag input is supplied. | Deterministic Java packet/source behavior and C# packet-byte assertion. | Charge/polish partial blobs are intentionally not covered because Java does not write full general-info there. |
| `GamePacketTests.SmWarehouseAddItem_WritesCleanupSealFlagInItemBlobLikeJava` | Regression / packet byte layout | `SM_WAREHOUSE_ADD_ITEM.writeItemInfo`, `ItemInfoBlob.getFullBlob`, `GeneralInfoBlobEntry` | Guards the shared explicit flag path remains stable. | Existing C# packet-byte assertion. | Not a Java runtime comparison. |

## Remaining Risks

- Runtime inventory add/update callers still need cleanup-table or precomputed cleanup/seal context.
- Mail attached item packets still default cleanup/seal flag to zero.
- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Temporary-exchange remaining seconds still serialize as zero.
- Runtime conditioning presence and wall-clock expiration/dye fields can still differ from Java.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 3 C# packet/serializer surfaces changed or consumed
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: runtime cleanup flag source for inventory add/update callers, mail attachment cleanup flag context, Java runtime artifact generation, temporary-exchange model/template fields, time-normalized expiration/dye comparison
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Work Options

## Recommended Sequential Task

- Task: extend cleanup/seal flag plumbing to mail attached item serialization.
- Scope:
  - inspect Java mail attached-item packet serialization path and C# `SmMailService`;
  - add explicit cleanup/seal flag input/context for attached item blob serialization;
  - preserve default-zero behavior for existing mail callers;
  - add focused attached-item packet tests for restricted item id `188053996` expecting nested `GENERAL_INFO` flag `3`.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Mail attached-item cleanup flag | `SmMailService.cs`, focused mail packet tests | Medium | Next smallest cleanup/seal packet wrapper. |
| B | Runtime inventory context source audit | read-only `GameServerConnection.cs`, services using inventory add/update | Low | Useful before live caller plumbing; no writes. |
| C | Broker plume bridge | `SmBrokerService.cs`, broker packet tests | Medium | Separate from cleanup/seal work; avoid `SmInventoryInfo.cs`. |
| D | Temporary exchange model/source unit | `InventoryItem`, template/static-data fields, focused tests | High | Broader model impact; analyze first. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Agent A | Mail attached-item cleanup flag | `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmMailService.cs`, focused tests | `SmInventoryInfo.cs`, inventory packet files, docs |
| Agent B | Runtime inventory context source audit | read-only `GameServerConnection.cs`, inventory services, Java `ItemPacketService` callers | all writes |
| Agent C | Broker plume bridge analysis | read-only `SmBrokerService.cs`, Java broker packet source | all writes |

## Do Not Parallelize

- Do not edit `SmInventoryInfo.cs` concurrently with wrapper packet units; it is the shared item-blob serializer.
- Do not edit shared progress/handoff/parity docs from sub-agents; orchestrator owns docs.
