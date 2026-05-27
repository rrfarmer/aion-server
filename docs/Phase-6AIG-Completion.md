# Phase 6 AIG Completion - Assembly Reward Cleanup-Seal Flag Source

Date: 2026-05-27
Unit of Work: UOW-1405
Status: live assembly reward packet construction now computes cleanup/seal flag context from static data.

## Completed

- Updated `GameServerConnection.CompleteAssemblyUseItemAsync` to pass `StaticData.ItemRestrictionCleanups` into assembly reward packet construction.
- Updated `SendAssemblyRewardPacketsAsync` so both stacked reward updates and newly added reward items compute Java's cleanup/seal general-info flag through `GetGeneralInfoWarehouseRestrictionFlag`.
- Added connection-level assembly reward tests with a synthetic assembly recipe and restricted reward item `188053996`.
- Covered both `SmInventoryAddItem.ItemCollect` and `SmInventoryUpdateItem.IncreaseItemCollect` reward branches carrying cleanup/seal flag `3`.
- Ran read-only parallel discovery for assembly socket-test feasibility and the next XP extraction reward helper.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_Assembly|FullyQualifiedName~AssemblyItemServiceTests|FullyQualifiedName~GamePacketTests.SmInventoryAddItem_WritesCleanupSealFlagInItemBlobLikeJava|FullyQualifiedName~GamePacketTests.SmInventoryUpdateItem_WritesCleanupSealFlagInItemBlobLikeJava|FullyQualifiedName~GamePacketTests.GameServerConnection_GetGeneralInfoWarehouseRestrictionFlag_UsesJavaCleanupPredicate"`.
- Result: passed 8 tests.
- Ran `git diff --check`; only line-ending warnings were reported.

## Migration Parity Table - UOW-1405

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.templates.item.actions.AssemblyItemAction` | `Aion.GameServer.Network.Aion.GameServerConnection.CompleteAssemblyUseItemAsync` and `SendAssemblyRewardPacketsAsync` | Service / Packet Orchestration | Partial | Regression Tested | Partial Parity | Live C# assembly reward packets now compute cleanup/seal flag context from `StaticData.ItemRestrictionCleanups` for updated and added reward items. New connection tests cover reward add and stack-merge packet emission with restricted item id `188053996`; Java runtime byte comparison remains unavailable. |
| `com.aionemu.gameserver.services.item.ItemService.addItem` / `com.aionemu.gameserver.model.items.storage.Storage.add` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryAddItem.CreateItemCollect` and `SmInventoryUpdateItem` as consumed by assembly rewards | Service / Packet Orchestration | Partial | Regression Tested | Partial Parity | Assembly reward add/update sends now supply the explicit Java-shaped flag to existing packet wrappers. Broader `Storage.add` side effects such as quest callbacks, cube update fanout, and all caller families remain incomplete. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_ADD_ITEM` item-collect branch | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryAddItem.CreateItemCollect` | Packet | Partial | Regression Tested | Partial Parity | Existing packet coverage plus new connection-level assembly test verify restricted reward item-add packets carry cleanup/seal flag `3`. Java runtime byte comparison remains unavailable. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM` full-blob branch | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem` | Packet | Partial | Regression Tested | Partial Parity | Existing packet coverage plus new connection-level assembly test verify restricted stack-merge reward updates carry cleanup/seal flag `3`. Partial update families such as charge/polish remain intentionally excluded. |
| `com.aionemu.gameserver.dataholders.ItemRestrictionCleanupData` | `Aion.GameServer.Dataholders.ItemRestrictionCleanupTable` and `GameServerConnection.GetGeneralInfoWarehouseRestrictionFlag` | Dataholder / Utility | Partial | Unit Tested | Partial Parity | The shared helper is reused by assembly rewards; missing static data still falls back to flag `0`. |
| `com.aionemu.gameserver.network.aion.iteminfo.GeneralInfoBlobEntry` cleanup/seal flag | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteGeneralInfoBlob` | Serialization Entry | Partial | Regression Tested | Partial Parity | Assembly rewards can now feed the Java-shaped field through inventory add/update packets. Temporary-exchange remaining seconds and time-dependent fields remain unresolved. |
| `com.aionemu.gameserver.model.templates.item.actions.ExpExtractAction` | future `GameServerConnection.SendExpExtractRewardPacketsAsync` flag input | Service / Packet Orchestration | Partial | No Tests | Needs Verification | Read-only audit found this as the next smallest adjacent reward helper. The caller already has `StaticData`, but the helper still defaults cleanup/seal flags to `0`. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_AssemblyAddsRestrictedRewardWithCleanupSealFlag` | Integration / connection regression | `AssemblyItemAction.act`, `ItemService.addItem`, `SM_INVENTORY_ADD_ITEM`, `GeneralInfoBlobEntry` | Assembly use-item completion emits an item-collect add packet for restricted reward item `188053996` with cleanup/seal flag `3`. | C# connection-level packet assertion using Java-shaped assembly recipe and cleanup row. | Does not compare against Java runtime bytes; persistence inputs are not captured. |
| `GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_AssemblyMergesRestrictedRewardWithCleanupSealFlag` | Integration / connection regression | `AssemblyItemAction.act`, `Storage.increaseItemCount`, `SM_INVENTORY_UPDATE_ITEM`, `GeneralInfoBlobEntry` | Assembly use-item completion emits an `IncreaseItemCollect` full-blob update for an existing restricted reward stack with cleanup/seal flag `3`. | C# connection-level packet assertion using Java-shaped assembly recipe and cleanup row. | Does not compare against Java runtime bytes; persistence inputs are not captured. |
| `AssemblyItemServiceTests` | Unit | `AssemblyItemAction.canAct` and delayed action part consumption | Existing service coverage for recipe validation, part consumption, added reward, and inventory-full behavior. | Source-derived C# assertions. | Does not serialize packets. |
| `GamePacketTests.SmInventoryAddItem_WritesCleanupSealFlagInItemBlobLikeJava` | Regression / packet byte layout | `SM_INVENTORY_ADD_ITEM.writeItemInfo`, `GeneralInfoBlobEntry` | Existing guard that item-collect inventory add packets write cleanup/seal field `3` when supplied. | C# packet-byte assertion against deterministic Java source order/field value. | Not a Java runtime comparison. |
| `GamePacketTests.SmInventoryUpdateItem_WritesCleanupSealFlagInItemBlobLikeJava` | Regression / packet byte layout | `SM_INVENTORY_UPDATE_ITEM.writeImpl`, `GeneralInfoBlobEntry` | Existing guard that normal full-blob inventory update packets write cleanup/seal field `3` when supplied. | C# packet-byte assertion against deterministic Java source order/field value. | Not a Java runtime comparison. |

## Remaining Risks

- XP extraction rewards, extraction rewards, decompose rewards, loot, quest/custom reward paths, other inventory add/update, warehouse-add, and future pet-feed/live callers still need cleanup-table or precomputed cleanup/seal context.
- Assembly reward tests use synthetic C# static-data fixtures rather than generated Java runtime packet artifacts.
- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Temporary-exchange remaining seconds still serialize as zero.
- Runtime conditioning presence and wall-clock expiration/dye fields can still differ from Java.

## Summary Metrics

- Total Java artifacts discovered: 7 grouped artifact rows in this unit
- Total artifacts ported: 2 C# caller/packet surfaces changed or consumed
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 7 grouped rows
- Total blocked artifacts: XP extraction/extract/decompose reward cleanup flag sources, world loot service cleanup flag source, remaining inventory add/update caller flag sources, warehouse-add/pet-feed live flag source, Java runtime artifact generation, temporary-exchange model/template/drop/task fields, time-normalized expiration/dye comparison
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Work Options

## Recommended Sequential Task

- Task: wire `GameServerConnection.SendExpExtractRewardPacketsAsync` reward add/update cleanup/seal flag sourcing.
- Scope:
  - call site is inside `CompleteExpExtractUseItemAsync(..., StaticData staticData, ...)`;
  - add `ItemRestrictionCleanupTable? itemRestrictionCleanups` to `SendExpExtractRewardPacketsAsync`;
  - pass `staticData.ItemRestrictionCleanups`;
  - compute flags with `GetGeneralInfoWarehouseRestrictionFlag` for updated and added rewards;
  - add a focused ExpExtract connection test if fixture changes stay contained, otherwise reuse existing packet/helper guards and document the socket-test gap.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Extract reward helper audit/implementation prep | read-only `GameServerConnection.cs` reward helper region | Low | Adjacent but should not edit concurrently with XP extraction because both touch `GameServerConnection.cs`. |
| B | World loot cleanup/seal implementation prep | `WorldNpcLootService.cs`, `WorldNpcLootServiceTests` | Medium | Needs optional `ItemRestrictionCleanupTable?` threaded through `RequestDropItem` and focused add/update loot tests. |
| C | Assembly persistence-input capture test design | `GameServerConnectionInventoryExpansionUseItemTests.cs` or a new fixture file | Medium | Would require a capturing player-enter-world repository; useful but not required for cleanup/seal packet source wiring. |

## Do Not Parallelize

- Do not edit `GameServerConnection.cs` from multiple workers at once.
- Do not edit shared progress/handoff/parity docs from sub-agents; orchestrator owns docs.
- Do not claim verified parity for assembly rewards until there is Java runtime packet evidence or generated artifact comparison.
