# Phase 6 AIF Completion - House-Use Reward Cleanup-Seal Flag Source

Date: 2026-05-27
Unit of Work: UOW-1404
Status: live house-use reward packet construction now computes cleanup/seal flag context from static data.

## Completed

- Updated `GameServerConnection.CompleteUseableHouseObjectAsync` so both stacked reward updates and newly added reward items compute Java's cleanup/seal general-info flag through `GetGeneralInfoWarehouseRestrictionFlag`.
- Preserved default-zero behavior through the existing helper when cleanup static data or a matching cleanup row is unavailable.
- Reused existing focused packet/helper coverage for explicit cleanup/seal flag `3`.
- Ran read-only parallel discovery for house-use reward test fixtures and adjacent reward helpers.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~GameServerConnectionHouseObjectTalkRangeTests|FullyQualifiedName~GamePacketTests.SmInventoryAddItem_WritesCleanupSealFlagInItemBlobLikeJava|FullyQualifiedName~GamePacketTests.SmInventoryUpdateItem_WritesCleanupSealFlagInItemBlobLikeJava|FullyQualifiedName~GamePacketTests.GameServerConnection_GetGeneralInfoWarehouseRestrictionFlag_UsesJavaCleanupPredicate"`.
- Result: passed 5 tests.
- Ran `git diff --check`; only line-ending warnings were reported.

## Migration Parity Table - UOW-1404

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.gameobjects.UseableItemObject` | `Aion.GameServer.Network.Aion.GameServerConnection.CompleteUseableHouseObjectAsync` | Service / Packet Orchestration | Partial | Regression Tested | Partial Parity | Live C# house-use reward packets now compute cleanup/seal flag context from `StaticData.ItemRestrictionCleanups` for updated and added reward items. Java final-reward, cooldown, broadcast, and house-object deletion flow remains only partially covered by existing tests, and no socket-level reward packet test exists yet. |
| `com.aionemu.gameserver.services.item.ItemService.addItem` / `com.aionemu.gameserver.model.items.storage.Storage.add` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryAddItem.CreateItemCollect` and `SmInventoryUpdateItem` as consumed by house-use rewards | Service / Packet Orchestration | Partial | Regression Tested | Partial Parity | House-use reward add/update sends now supply the explicit Java-shaped flag to existing packet wrappers. Broader `Storage.add` side effects such as quest callbacks, cube update fanout, and all caller families remain incomplete. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_ADD_ITEM` item-collect branch | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryAddItem.CreateItemCollect` | Packet | Partial | Regression Tested | Partial Parity | Existing item-collect packet coverage verifies explicit cleanup/seal flag `3`; this unit wires the house-use reward add caller into that path. Java runtime byte comparison remains unavailable. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM` full-blob branch | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem` | Packet | Partial | Regression Tested | Partial Parity | Existing full-blob inventory-update coverage verifies explicit cleanup/seal flag `3`; this unit wires stacked house-use rewards into that path. Partial update families such as charge/polish remain intentionally excluded. |
| `com.aionemu.gameserver.dataholders.ItemRestrictionCleanupData` | `Aion.GameServer.Dataholders.ItemRestrictionCleanupTable` and `GameServerConnection.GetGeneralInfoWarehouseRestrictionFlag` | Dataholder / Utility | Partial | Unit Tested | Partial Parity | The shared helper is reused by house-use rewards; missing static data still falls back to flag `0`. |
| `com.aionemu.gameserver.network.aion.iteminfo.GeneralInfoBlobEntry` cleanup/seal flag | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteGeneralInfoBlob` | Serialization Entry | Partial | Regression Tested | Partial Parity | House-use rewards can now feed the Java-shaped field through inventory add/update packets. Temporary-exchange remaining seconds and time-dependent fields remain unresolved. |
| `com.aionemu.gameserver.model.templates.item.actions.AssemblyItemAction` | future `GameServerConnection.SendAssemblyRewardPacketsAsync` flag input | Service / Packet Orchestration | Partial | No Tests | Needs Verification | Read-only audit found this as the next smallest adjacent reward helper. The caller already has `StaticData`, but the helper still defaults cleanup/seal flags to `0`. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GamePacketTests.SmInventoryAddItem_WritesCleanupSealFlagInItemBlobLikeJava` | Regression / packet byte layout | `SM_INVENTORY_ADD_ITEM.writeItemInfo`, `GeneralInfoBlobEntry` | Existing guard that item-collect inventory add packets write cleanup/seal field `3` when supplied by the runtime caller. | C# packet-byte assertion against deterministic Java source order/field value. | Does not execute house-use reward emission through a socket fixture and does not compare Java runtime bytes. |
| `GamePacketTests.SmInventoryUpdateItem_WritesCleanupSealFlagInItemBlobLikeJava` | Regression / packet byte layout | `SM_INVENTORY_UPDATE_ITEM.writeImpl`, `GeneralInfoBlobEntry` | Existing guard that normal full-blob inventory update packets write cleanup/seal field `3` when supplied by the runtime caller. | C# packet-byte assertion against deterministic Java source order/field value. | Does not execute the house-use stacked-reward branch through a socket fixture. |
| `GamePacketTests.GameServerConnection_GetGeneralInfoWarehouseRestrictionFlag_UsesJavaCleanupPredicate` | Unit | `ItemRestrictionCleanupData.hasAccountOrLegionWhStorabilityDisabled` | Guards the shared live caller flag decision reused by house-use rewards. | Existing source-derived C# assertions. | Not a Java runtime comparison. |
| `GameServerConnectionHouseObjectTalkRangeTests.TryCreateHouseObjectUseTarget_UsesJavaHouseObjectTalkRange` | Unit | `UseableHouseObject.getUseDistance` / house-object use targeting | Existing guard that house-object target range behavior remains stable while this caller family is edited. | Source-derived C# assertions. | Does not verify reward packet emission. |

## Remaining Risks

- Assembly rewards, extraction rewards, XP extraction rewards, decompose rewards, loot, quest/custom reward paths, other inventory add/update, warehouse-add, and future pet-feed/live callers still need cleanup-table or precomputed cleanup/seal context.
- No socket-level house-use reward integration test yet asserts outbound `SmInventoryAddItem` or `SmInventoryUpdateItem` through `GameServerConnection`.
- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Temporary-exchange remaining seconds still serialize as zero.
- Runtime conditioning presence and wall-clock expiration/dye fields can still differ from Java.

## Summary Metrics

- Total Java artifacts discovered: 7 grouped artifact rows in this unit
- Total artifacts ported: 2 C# caller/packet surfaces changed or consumed
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 7 grouped rows
- Total blocked artifacts: assembly/extract/XP extraction/decompose reward cleanup flag sources, world loot service cleanup flag source, remaining inventory add/update caller flag sources, warehouse-add/pet-feed live flag source, Java runtime artifact generation, temporary-exchange model/template/drop/task fields, time-normalized expiration/dye comparison
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Work Options

## Recommended Sequential Task

- Task: wire `GameServerConnection.SendAssemblyRewardPacketsAsync` reward add/update cleanup/seal flag sourcing.
- Scope:
  - call site is inside `CompleteAssemblyUseItemAsync(..., StaticData staticData, ...)`;
  - add `ItemRestrictionCleanupTable? itemRestrictionCleanups` to `SendAssemblyRewardPacketsAsync`;
  - pass `staticData.ItemRestrictionCleanups`;
  - compute flags with `GetGeneralInfoWarehouseRestrictionFlag` for updated and added rewards;
  - reuse existing packet/helper guards unless adding a narrow assembly socket fixture stays small.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | XP extraction reward helper audit/implementation prep | read-only `GameServerConnection.cs` reward helper region | Low | Adjacent but should not edit concurrently with assembly because both touch `GameServerConnection.cs`. |
| B | World loot cleanup/seal implementation prep | `WorldNpcLootService.cs`, `WorldNpcLootServiceTests` | Medium | Needs optional `ItemRestrictionCleanupTable?` threaded through `RequestDropItem` and focused add/update loot tests. |
| C | House-use reward socket test design | `GameServerConnectionHouseObjectUseTests.cs` or `GameServerConnectionHouseObjectTalkRangeTests.cs` | Medium | Requires small static-data and private-handler fixture; useful but not required for assembly helper wiring. |

## Do Not Parallelize

- Do not edit `GameServerConnection.cs` from multiple workers at once.
- Do not edit shared progress/handoff/parity docs from sub-agents; orchestrator owns docs.
- Do not claim verified parity for house-use rewards until there is socket-level output comparison or generated Java runtime packet evidence.
