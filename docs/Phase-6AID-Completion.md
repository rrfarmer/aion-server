# Phase 6 AID Completion - Broker Settlement Cleanup-Seal Flag Source

Date: 2026-05-27
Unit of Work: UOW-1402
Status: live broker settlement returned-item packet construction now computes cleanup/seal flag context from static data.

## Completed

- Updated `GameServerConnection.HandleBrokerSettleAccountAsync` to resolve `StaticData` once and reuse `ItemTemplates` plus `ItemRestrictionCleanups`.
- Passed `GetGeneralInfoWarehouseRestrictionFlag(returnedItem.ReturnedItem.ItemId, staticData?.ItemRestrictionCleanups)` into `SmInventoryAddItem.CreateItemCollect` for returned non-sold settlement items.
- Left settlement kinah updates unchanged because kinah update packets do not need cleanup/seal item-blob context.
- Reused existing focused item-collect packet coverage for explicit cleanup/seal flag `3`.

## Validation

- First targeted run caught a compile mispatch where `staticData` was not introduced in the settlement method; corrected before proceeding.
- Reran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~GamePacketTests.SmInventoryAddItem_WritesCleanupSealFlagInItemBlobLikeJava|FullyQualifiedName~GamePacketTests.GameServerConnection_GetGeneralInfoWarehouseRestrictionFlag_UsesJavaCleanupPredicate"`.
- Result: passed 2 tests.
- Ran `git diff --check`; only line-ending warnings were reported.

## Migration Parity Table - UOW-1402

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.broker.BrokerService.settleAccount` returned-item path | `Aion.GameServer.Network.Aion.GameServerConnection.HandleBrokerSettleAccountAsync` | Service / Packet Orchestration | Partial | Regression Tested | Partial Parity | Live C# broker settlement returns now compute cleanup/seal flag context from static data for returned non-sold items and pass it to the item-collect inventory-add packet. Kinah settlement updates are intentionally unchanged. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_ADD_ITEM` item-collect branch | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryAddItem.CreateItemCollect` | Packet | Partial | Regression Tested | Partial Parity | Existing item-collect packet coverage verifies explicit cleanup/seal flag `3`; this unit wires a new live caller into that path. Java runtime byte comparison remains unavailable. |
| `com.aionemu.gameserver.dataholders.ItemRestrictionCleanupData` | `Aion.GameServer.Dataholders.ItemRestrictionCleanupTable` and `GameServerConnection.GetGeneralInfoWarehouseRestrictionFlag` | Dataholder / Utility | Partial | Unit Tested | Partial Parity | UOW-1399 helper is reused by broker settlement; missing static data still falls back to flag `0`. |
| `com.aionemu.gameserver.network.aion.iteminfo.GeneralInfoBlobEntry` cleanup/seal flag | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteGeneralInfoBlob` | Serialization Entry | Partial | Regression Tested | Partial Parity | Broker settlement returns can now feed the Java-shaped field through `SmInventoryAddItem`. Temporary-exchange remaining seconds and time-dependent fields remain unresolved. |
| `com.aionemu.gameserver.model.items.storage.Storage.add` / broader item add/update paths | remaining C# inventory add/update live callers | Service / Packet Orchestration | Partial | No Tests | Needs Verification | Loot, use-item rewards, quest/custom reward paths, and other full-blob inventory callers still need individual cleanup/seal flag audits. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GamePacketTests.SmInventoryAddItem_WritesCleanupSealFlagInItemBlobLikeJava` | Regression / packet byte layout | `SM_INVENTORY_ADD_ITEM.writeItemInfo`, `GeneralInfoBlobEntry` | Existing guard that item-collect inventory add packets write cleanup/seal field `3` when supplied by the runtime caller. | C# packet-byte assertion against deterministic Java source order/field value. | Does not execute `HandleBrokerSettleAccountAsync` through a socket fixture and does not compare Java runtime bytes. |
| `GamePacketTests.GameServerConnection_GetGeneralInfoWarehouseRestrictionFlag_UsesJavaCleanupPredicate` | Unit | `ItemRestrictionCleanupData.hasAccountOrLegionWhStorabilityDisabled` | Guards the shared live caller flag decision reused by broker settlement. | Existing source-derived C# assertions. | Not a Java runtime comparison. |

## Remaining Risks

- Loot, use-item rewards, quest/custom reward paths, other inventory add/update, warehouse-add, and future pet-feed/live callers still need cleanup-table or precomputed cleanup/seal context.
- No socket-level broker settlement integration test yet asserts the outbound `SmInventoryAddItem` through `GameServerConnection`.
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

- Task: audit and wire the next single full-blob inventory add/update caller with `StaticData.ItemRestrictionCleanups` already available.
- Scope:
  - likely candidates are item-use reward or loot item-collect paths;
  - avoid charge/polish partial updates because they do not write `GENERAL_INFO`;
  - compute the flag with `GetGeneralInfoWarehouseRestrictionFlag`;
  - add focused packet/caller coverage or reuse an existing packet guard when the packet branch is already covered.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Item-use reward caller audit | read-only `GameServerConnection.cs` around item-use reward sends | Low | Several rewards already have static data and item templates in scope. |
| B | World loot caller audit | read-only `WorldNpcLootService.cs`, loot tests | Low | Service has item add/update packets but may need static-data dependency review. |
| C | Temporary exchange model UOW | `InventoryItem.cs`, `ItemTemplateTable.cs`, static-data tests | Medium | Separate from cleanup/seal; do not mix with runtime caller plumbing. |

## Do Not Parallelize

- Do not edit `GameServerConnection.cs` from multiple workers at once.
- Do not edit shared progress/handoff/parity docs from sub-agents; orchestrator owns docs.
- Do not change `SmInventoryInfo.cs` for cleanup/seal while runtime caller flag sourcing is still being finished.
