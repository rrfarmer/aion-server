# Phase 6 AIB Completion - Broker-Buy Cleanup-Seal Flag Source

Date: 2026-05-27
Unit of Work: UOW-1400
Status: live broker-buy inventory-add packet construction now computes cleanup/seal flag context from static data.

## Completed

- Updated `GameServerConnection.HandleBuyBrokerItemAsync` to resolve `StaticData` once and reuse `ItemTemplates` plus `ItemRestrictionCleanups`.
- Passed `GetGeneralInfoWarehouseRestrictionFlag(boughtItem.ItemId, staticData?.ItemRestrictionCleanups)` into `SmInventoryAddItem.CreateBrokerBuy`.
- Preserved default-zero fallback when static data or cleanup rows are unavailable.
- Added focused broker-buy packet coverage proving update type `BrokerBuy` can carry cleanup/seal flag `3` inside the nested `GENERAL_INFO` blob.

## Validation

- First targeted run caught a compile mispatch where `staticData` was introduced in a nearby charge-all method instead of broker-buy; corrected before proceeding.
- Reran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~GamePacketTests.GameServerConnection_GetGeneralInfoWarehouseRestrictionFlag_UsesJavaCleanupPredicate|FullyQualifiedName~GamePacketTests.SmInventoryAddItem_BrokerBuyWritesCleanupSealFlagInItemBlobLikeJava|FullyQualifiedName~GamePacketTests.SmInventoryAddItem_WritesCleanupSealFlagInItemBlobLikeJava"`.
- Result: passed 3 tests.
- Ran `git diff --check`; only line-ending warnings were reported.

## Migration Parity Table - UOW-1400

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.broker.BrokerService.buyBrokerItem` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleBuyBrokerItemAsync` | Service / Packet Orchestration | Partial | Regression Tested | Partial Parity | Live C# broker-buy now computes cleanup/seal flag context from static data for the bought item and passes it to the broker-buy inventory-add packet. Repository transaction parity, seller notifications, and socket-level byte comparison remain outside this unit. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_ADD_ITEM` broker-buy branch | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryAddItem.CreateBrokerBuy` | Packet | Partial | Regression Tested | Partial Parity | Broker-buy update type now has focused packet coverage for explicit cleanup/seal flag `3`. Java runtime byte comparison remains unavailable. |
| `com.aionemu.gameserver.dataholders.ItemRestrictionCleanupData` | `Aion.GameServer.Dataholders.ItemRestrictionCleanupTable` and `GameServerConnection.GetGeneralInfoWarehouseRestrictionFlag` | Dataholder / Utility | Partial | Unit Tested | Partial Parity | UOW-1399 helper is reused by broker-buy; missing static data still falls back to flag `0`. |
| `com.aionemu.gameserver.network.aion.iteminfo.GeneralInfoBlobEntry` cleanup/seal flag | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteGeneralInfoBlob` | Serialization Entry | Partial | Regression Tested | Partial Parity | Broker-buy can now feed the Java-shaped field through `SmInventoryAddItem`. Temporary-exchange remaining seconds and time-dependent fields remain unresolved. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemUpdatePacket` cube update paths | remaining C# `SmInventoryUpdateItem` live callers | Service / Packet Orchestration | Partial | No Tests | Needs Verification | Many live inventory update callers still default cleanup/seal flag to zero. Charge/polish partial blobs do not write `GENERAL_INFO`; full-blob callers need individual audit/wiring. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GamePacketTests.SmInventoryAddItem_BrokerBuyWritesCleanupSealFlagInItemBlobLikeJava` | Regression / packet byte layout | `SM_INVENTORY_ADD_ITEM.writeItemInfo`, `GeneralInfoBlobEntry` | Broker-buy inventory add packets write cleanup/seal field `3` when supplied by the runtime caller. | C# packet-byte assertion against deterministic Java source order/field value. | Does not execute `HandleBuyBrokerItemAsync` through a socket fixture and does not compare Java runtime bytes. |
| `GamePacketTests.GameServerConnection_GetGeneralInfoWarehouseRestrictionFlag_UsesJavaCleanupPredicate` | Unit | `ItemRestrictionCleanupData.hasAccountOrLegionWhStorabilityDisabled` | Guards the shared live caller flag decision reused by broker-buy. | Existing source-derived C# assertions. | Not a Java runtime comparison. |
| `GamePacketTests.SmInventoryAddItem_WritesCleanupSealFlagInItemBlobLikeJava` | Regression / packet byte layout | `SM_INVENTORY_ADD_ITEM.writeItemInfo`, `GeneralInfoBlobEntry` | Guards the inventory-add explicit flag path remains stable. | Existing C# packet-byte assertion. | Not a Java runtime comparison. |

## Remaining Risks

- Other inventory add/update, warehouse-add, loot, and future pet-feed/live callers still need cleanup-table or precomputed cleanup/seal context.
- No socket-level broker-buy integration test yet asserts the outbound `SmInventoryAddItem` through `GameServerConnection`.
- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Temporary-exchange remaining seconds still serialize as zero.
- Runtime conditioning presence and wall-clock expiration/dye fields can still differ from Java.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 2 C# caller/packet surfaces changed or consumed
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: remaining inventory add/update caller flag sources, warehouse-add/pet-feed live flag source, Java runtime artifact generation, temporary-exchange model/template/drop/task fields, time-normalized expiration/dye comparison
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Work Options

## Recommended Sequential Task

- Task: audit and wire the next smallest full-blob inventory add/update live caller with cleanup/seal static data available.
- Scope:
  - avoid charge/polish partial updates because they do not write `GENERAL_INFO`;
  - prefer a single `GameServerConnection` call site or isolated service caller;
  - compute the flag with `GetGeneralInfoWarehouseRestrictionFlag`;
  - add focused packet/caller coverage.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Remaining inventory full-blob caller audit | read-only `GameServerConnection.cs`, loot/broker services | Low | Identify the next one-call runtime flag source. |
| B | Temporary exchange model UOW | `InventoryItem.cs`, `ItemTemplateTable.cs`, static-data tests | Medium | Separate from cleanup/seal; do not mix with runtime caller plumbing. |
| C | Broker-buy socket fixture feasibility audit | read-only tests and broker repo fakes | Low | Could later close the current no-socket test gap. |

## Do Not Parallelize

- Do not edit `GameServerConnection.cs` from multiple workers at once.
- Do not edit shared progress/handoff/parity docs from sub-agents; orchestrator owns docs.
- Do not change `SmInventoryInfo.cs` for cleanup/seal while runtime caller flag sourcing is still being finished.
