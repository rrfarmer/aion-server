# Phase 6 AIC Completion - Broker-Return Cleanup-Seal Flag Source

Date: 2026-05-27
Unit of Work: UOW-1401
Status: live broker cancel-return inventory-add packet construction now computes cleanup/seal flag context from static data.

## Completed

- Updated `GameServerConnection.HandleBrokerCancelRegisteredAsync` to resolve `StaticData` once and reuse `ItemTemplates` plus `ItemRestrictionCleanups`.
- Passed `GetGeneralInfoWarehouseRestrictionFlag(returnedItem.ItemId, staticData?.ItemRestrictionCleanups)` into `SmInventoryAddItem.CreateBrokerReturn`.
- Preserved default-zero fallback when static data or cleanup rows are unavailable.
- Added focused broker-return packet coverage proving update type `BrokerReturn` can carry cleanup/seal flag `3` inside the nested `GENERAL_INFO` blob.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~GamePacketTests.SmInventoryAddItem_BrokerReturnWritesCleanupSealFlagInItemBlobLikeJava|FullyQualifiedName~GamePacketTests.SmInventoryAddItem_BrokerBuyWritesCleanupSealFlagInItemBlobLikeJava|FullyQualifiedName~GamePacketTests.GameServerConnection_GetGeneralInfoWarehouseRestrictionFlag_UsesJavaCleanupPredicate"`.
- Result: passed 3 tests.
- Ran `git diff --check`; only line-ending warnings were reported.

## Migration Parity Table - UOW-1401

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.broker.BrokerService.cancelRegisteredItem` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleBrokerCancelRegisteredAsync` | Service / Packet Orchestration | Partial | Regression Tested | Partial Parity | Live C# broker cancel-return now computes cleanup/seal flag context from static data for the returned item and passes it to the broker-return inventory-add packet. Repository transaction and socket-level Java byte comparison remain outside this unit. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_ADD_ITEM` broker-return branch | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryAddItem.CreateBrokerReturn` | Packet | Partial | Regression Tested | Partial Parity | Broker-return update type now has focused packet coverage for explicit cleanup/seal flag `3`. Java runtime byte comparison remains unavailable. |
| `com.aionemu.gameserver.dataholders.ItemRestrictionCleanupData` | `Aion.GameServer.Dataholders.ItemRestrictionCleanupTable` and `GameServerConnection.GetGeneralInfoWarehouseRestrictionFlag` | Dataholder / Utility | Partial | Unit Tested | Partial Parity | UOW-1399 helper is reused by broker-return; missing static data still falls back to flag `0`. |
| `com.aionemu.gameserver.network.aion.iteminfo.GeneralInfoBlobEntry` cleanup/seal flag | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteGeneralInfoBlob` | Serialization Entry | Partial | Regression Tested | Partial Parity | Broker-return can now feed the Java-shaped field through `SmInventoryAddItem`. Temporary-exchange remaining seconds and time-dependent fields remain unresolved. |
| `com.aionemu.gameserver.services.broker.BrokerService.settleAccount` returned-item path | future `GameServerConnection.HandleBrokerSettleAccountAsync` cleanup flag input | Service / Packet Orchestration | Partial | No Tests | Needs Verification | Broker settlement returned/collected item packets still default cleanup/seal flags to zero and need a separate audit. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GamePacketTests.SmInventoryAddItem_BrokerReturnWritesCleanupSealFlagInItemBlobLikeJava` | Regression / packet byte layout | `SM_INVENTORY_ADD_ITEM.writeItemInfo`, `GeneralInfoBlobEntry` | Broker-return inventory add packets write cleanup/seal field `3` when supplied by the runtime caller. | C# packet-byte assertion against deterministic Java source order/field value. | Does not execute `HandleBrokerCancelRegisteredAsync` through a socket fixture and does not compare Java runtime bytes. |
| `GamePacketTests.SmInventoryAddItem_BrokerBuyWritesCleanupSealFlagInItemBlobLikeJava` | Regression / packet byte layout | `SM_INVENTORY_ADD_ITEM.writeItemInfo`, `GeneralInfoBlobEntry` | Guards the broker-buy explicit flag path remains stable. | Existing C# packet-byte assertion. | Not a Java runtime comparison. |
| `GamePacketTests.GameServerConnection_GetGeneralInfoWarehouseRestrictionFlag_UsesJavaCleanupPredicate` | Unit | `ItemRestrictionCleanupData.hasAccountOrLegionWhStorabilityDisabled` | Guards the shared live caller flag decision reused by broker-return. | Existing source-derived C# assertions. | Not a Java runtime comparison. |

## Remaining Risks

- Broker settlement returned/collected items, other inventory add/update, warehouse-add, loot, and future pet-feed/live callers still need cleanup-table or precomputed cleanup/seal context.
- No socket-level broker cancel-return integration test yet asserts the outbound `SmInventoryAddItem` through `GameServerConnection`.
- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Temporary-exchange remaining seconds still serialize as zero.
- Runtime conditioning presence and wall-clock expiration/dye fields can still differ from Java.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 2 C# caller/packet surfaces changed or consumed
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: broker settlement cleanup flag source, remaining inventory add/update caller flag sources, warehouse-add/pet-feed live flag source, Java runtime artifact generation, temporary-exchange model/template/drop/task fields, time-normalized expiration/dye comparison
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Work Options

## Recommended Sequential Task

- Task: audit and wire broker settlement returned/collected item packet cleanup/seal flag sourcing.
- Scope:
  - inspect `HandleBrokerSettleAccountAsync`;
  - distinguish kinah update packets from returned item add/update packets;
  - compute the flag with `GetGeneralInfoWarehouseRestrictionFlag` only for full item blobs;
  - add focused packet/caller coverage.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Broker settlement caller audit | read-only `GameServerConnection.cs`, broker packet tests | Low | Natural next broker cleanup/seal caller. |
| B | Temporary exchange model UOW | `InventoryItem.cs`, `ItemTemplateTable.cs`, static-data tests | Medium | Separate from cleanup/seal; do not mix with runtime caller plumbing. |
| C | Socket fixture feasibility for broker paths | read-only tests and broker repo fakes | Low | Could close current no-socket integration gaps later. |

## Do Not Parallelize

- Do not edit `GameServerConnection.cs` from multiple workers at once.
- Do not edit shared progress/handoff/parity docs from sub-agents; orchestrator owns docs.
- Do not change `SmInventoryInfo.cs` for cleanup/seal while runtime caller flag sourcing is still being finished.
