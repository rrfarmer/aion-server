# Phase 6 AHV Completion - Cleanup/Seal General-Info Packet Flag

Date: 2026-05-27
Unit of Work: UOW-1394
Status: Cleanup/seal general-info flag serialization implemented for explicit item-blob input and login inventory/warehouse paths. Service-produced add/update/mail packets remain on the default-zero path.

## Completed

- Added an explicit `generalInfoWarehouseRestrictionFlag` input to `SmInventoryInfo.WriteItemInfoBlob`.
- Updated `SmInventoryInfo.WriteGeneralInfoBlob` to write that flag into the Java cleanup/seal `H` field.
- Preserved default-zero behavior for existing item-blob callers that do not yet pass cleanup data.
- Updated `SmInventoryInfo.CreateLoginPackets` to accept `ItemRestrictionCleanupTable` and compute Java's `3/0` flag per item.
- Updated `SmWarehouseInfo.CreateLoginPackets` and `CreateRegularWarehouseUpdatePackets` to accept `ItemRestrictionCleanupTable` and compute the same flag for warehouse item blobs.
- Wired enter-world inventory and warehouse login packet creation through `StaticData.ItemRestrictionCleanups` in `GameServerConnection`.
- Added focused packet coverage for an item id restricted by `ItemRestrictionCleanupTable`, asserting the `GENERAL_INFO` cleanup/seal field is `3`.
- Spawned and closed a read-only call-site audit sub-agent. Its output identified remaining default-zero packet families: inventory add/update, warehouse add, mail attached item, and service-produced packet contexts.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GamePacketTests.SmInventoryInfo_WritesCleanupSealFlagFromRestrictionTableLikeJava|FullyQualifiedName~GamePacketTests.SmInventoryInfo_WritesStatBonusBlobsAfterPremiumOptionLikeJava|FullyQualifiedName~GamePacketTests.SmInventoryInfo_WritesItemStoneAndIdianDetailsInItemBlobs|FullyQualifiedName~StaticDataLoadingTests.StaticData_LoadsItemRestrictionCleanupFlagsLikeJava" --no-restore`.
- Result: passed 4 tests.
- Ran `git diff --check`.

## Migration Parity Table - UOW-1394

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.iteminfo.GeneralInfoBlobEntry` cleanup/seal flag | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteGeneralInfoBlob` | Serialization Entry | Partial | Regression Tested | Partial Parity | C# can now write Java's cleanup/seal `H` value when packet construction passes the flag. Temporary-exchange and time-dependent expiration/dye fields remain unresolved. |
| `com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteItemInfoBlob` | Serialization Utility | Partial | Regression Tested | Partial Parity | Added explicit cleanup/seal flag input with default zero. This avoids global static-data reads in the serializer. Remaining callers must be wired individually. |
| `com.aionemu.gameserver.dataholders.ItemRestrictionCleanupData` | `Aion.GameServer.Dataholders.ItemRestrictionCleanupTable` | Dataholder | Complete for packet predicate | Unit Tested | Partial Parity | Existing UOW-1393 table supplies `HasAccountOrLegionWarehouseStorabilityDisabled`; this unit consumes it for login inventory/warehouse packet construction. Java template-mask cleanup mutation remains outside scope. |
| `com.aionemu.gameserver.services.player.PlayerEnterWorldService` inventory/warehouse item info sends | `Aion.GameServer.Network.Aion.GameServerConnection` enter-world packet flow | Service / Packet Orchestration | Partial | Regression Tested | Partial Parity | Enter-world inventory and warehouse login packet creation now receives `StaticData.ItemRestrictionCleanups`. Runtime comparison against Java enter-world packets is still missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_INFO` item body | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo` | Packet | Partial | Regression Tested | Partial Parity | Focused test verifies a restricted item writes cleanup/seal flag `3` in `GENERAL_INFO`. Kinah/default items remain zero. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_WAREHOUSE_INFO` item body | `Aion.GameServer.Network.Aion.ServerPackets.SmWarehouseInfo` | Packet | Partial | Compile Tested | Needs Verification | Login/regular warehouse update factories now accept cleanup data and pass flags to item blobs, but no dedicated warehouse blob assertion was added in this unit. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GamePacketTests.SmInventoryInfo_WritesCleanupSealFlagFromRestrictionTableLikeJava` | Regression / packet byte layout | `GeneralInfoBlobEntry` and `ItemRestrictionCleanupData` source review | Restricted item id `188053996` writes cleanup/seal flag `3` inside `GENERAL_INFO`; surrounding general-info fields remain unchanged. | Deterministic Java source behavior and C# packet-byte assertion. | Does not compare generated Java packet bytes and does not cover warehouse/add/update/mail packets. |
| `GamePacketTests.SmInventoryInfo_WritesStatBonusBlobsAfterPremiumOptionLikeJava` | Regression | Existing item-blob Java source review | Ensures stat-bonus ordering remains stable after the general-info flag change. | C# regression over Java-shaped blob order. | Not a full Java runtime comparison. |
| `GamePacketTests.SmInventoryInfo_WritesItemStoneAndIdianDetailsInItemBlobs` | Regression | Existing enchant/polish Java source review | Ensures existing enchant/id/polish fields and 138-byte layout remain stable. | C# regression over Java-shaped blob offsets. | Not a full Java runtime comparison. |
| `StaticDataLoadingTests.StaticData_LoadsItemRestrictionCleanupFlagsLikeJava` | Unit | `ItemRestrictionCleanupData` and XML schema | Confirms the cleanup table predicate feeding this flag still matches Java defaults. | Deterministic Java source/schema behavior. | Not a packet test. |

## Sidecar Audit Result

The read-only call-site audit confirmed the explicit scalar input is safe for the central serializer and that login inventory/warehouse paths have static data available. It also identified remaining default-zero packet families that need later focused units: `SmInventoryAddItem`, `SmInventoryUpdateItem`, `SmWarehouseAddItem`, `SmMailService`, and service-produced packet contexts such as pet-feed unusual-storage metadata bridge paths.

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling, so no runtime Java packet-byte comparison exists.
- Temporary-exchange remaining seconds still serialize as zero.
- Expiration/dye remaining seconds remain wall-clock dependent.
- Inventory add/update, warehouse add, mail attached item, and service-produced item blob packets still default cleanup/seal flag to zero.
- Warehouse login/update cleanup flag flow compiles but lacks a dedicated byte assertion.
- Broker plume template context remains a separate packet parity gap.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 4 C# surfaces changed
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: Java runtime artifact generation, temporary-exchange model/template fields, service-produced cleanup flag plumbing, warehouse-add/mail/inventory-update cleanup tests, broker plume template context, runtime conditioning presence, time-normalized expiration/dye comparison
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Work Options

## Recommended Sequential Task

- Task: extend cleanup/seal flag plumbing to service-produced item blob packets, starting with `SmWarehouseAddItem` because it is part of the unusual-storage rejected-food packet path.
- Scope:
  - add an explicit cleanup/seal flag field to `SmWarehouseAddItem.WarehousePacketItem` or factory input;
  - update the pet-feed unusual-storage metadata bridge only if it can supply deterministic cleanup data;
  - add a focused warehouse-add packet test for restricted item id `188053996`;
  - keep temporary-exchange and time-normalized fields separate.

## Safe Parallel Candidates

- Broker plume bridge unit: add template-aware `SmBrokerService` enchant-info calls and broker plume packet tests.
- Temporary exchange model/static source unit: load `temp_exchange_time` into `ItemTemplateSummary` and add an `InventoryItem` runtime absolute epoch field with injectable remaining-time helper tests.
- Warehouse login byte assertion: add a dedicated `SmWarehouseInfo` cleanup flag packet test without changing production code.

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Agent A | Warehouse-add cleanup flag plumbing | `SmWarehouseAddItem.cs`, focused packet tests, pet-feed metadata bridge if needed | `SmInventoryInfo.cs` unless a bug is found |
| Agent B | Broker plume bridge | `SmBrokerService.cs`, broker tests, `GameServerConnection.cs` if needed | `SmInventoryInfo.cs` |
| Agent C | Temporary exchange source/model planning | read-only Java/C# model/static-data files | all writes |

## Do Not Parallelize

- `SmInventoryInfo.cs`: shared item-blob serializer; use exclusive ownership for any future changes.
