# Phase 6 AIE Completion - Composition Reward Cleanup-Seal Flag Source

Date: 2026-05-27
Unit of Work: UOW-1403
Status: live composition reward packet construction now computes cleanup/seal flag context from static data.

## Completed

- Updated `GameServerConnection.CompleteCompositeStonesAsync` to pass `StaticData.ItemRestrictionCleanups` into composition reward packet construction.
- Updated `SendCompositionRewardPacketsAsync` so both stacked reward updates and newly added reward items compute Java's cleanup/seal general-info flag through `GetGeneralInfoWarehouseRestrictionFlag`.
- Preserved default-zero behavior through the existing helper when cleanup static data or a matching cleanup row is unavailable.
- Reused existing focused packet/helper coverage for explicit cleanup/seal flag `3`.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~GamePacketTests.SmInventoryAddItem_WritesCleanupSealFlagInItemBlobLikeJava|FullyQualifiedName~GamePacketTests.SmInventoryUpdateItem_WritesCleanupSealFlagInItemBlobLikeJava|FullyQualifiedName~GamePacketTests.GameServerConnection_GetGeneralInfoWarehouseRestrictionFlag_UsesJavaCleanupPredicate"`.
- Result: passed 3 tests.
- Ran `git diff --check`; only line-ending warnings were reported.

## Migration Parity Table - UOW-1403

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.templates.item.actions.CompositionAction` | `Aion.GameServer.Network.Aion.GameServerConnection.CompleteCompositeStonesAsync` and `SendCompositionRewardPacketsAsync` | Service / Packet Orchestration | Partial | Regression Tested | Partial Parity | Live C# composition reward packets now compute cleanup/seal flag context from `StaticData.ItemRestrictionCleanups` for updated and added reward items. The delayed action and inventory mutation flow were already present; socket-level composition packet output is not yet tested against Java runtime bytes. |
| `com.aionemu.gameserver.services.item.ItemService.addItem` / `com.aionemu.gameserver.model.items.storage.Storage.add` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryAddItem.CreateItemCollect` and `SmInventoryUpdateItem` as consumed by composition rewards | Service / Packet Orchestration | Partial | Regression Tested | Partial Parity | Composition reward add/update sends now supply the explicit Java-shaped flag to existing packet wrappers. Broader `Storage.add` side effects such as quest callbacks, cube update fanout, and all caller families remain incomplete. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_ADD_ITEM` item-collect branch | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryAddItem.CreateItemCollect` | Packet | Partial | Regression Tested | Partial Parity | Existing item-collect packet coverage verifies explicit cleanup/seal flag `3`; this unit wires the composition reward add caller into that path. Java runtime byte comparison remains unavailable. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM` full-blob branch | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem` | Packet | Partial | Regression Tested | Partial Parity | Existing full-blob inventory-update coverage verifies explicit cleanup/seal flag `3`; this unit wires stacked composition rewards into that path. Partial update families such as charge/polish remain intentionally excluded. |
| `com.aionemu.gameserver.dataholders.ItemRestrictionCleanupData` | `Aion.GameServer.Dataholders.ItemRestrictionCleanupTable` and `GameServerConnection.GetGeneralInfoWarehouseRestrictionFlag` | Dataholder / Utility | Partial | Unit Tested | Partial Parity | The shared helper is reused by composition rewards; missing static data still falls back to flag `0`. |
| `com.aionemu.gameserver.network.aion.iteminfo.GeneralInfoBlobEntry` cleanup/seal flag | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteGeneralInfoBlob` | Serialization Entry | Partial | Regression Tested | Partial Parity | Composition rewards can now feed the Java-shaped field through inventory add/update packets. Temporary-exchange remaining seconds and time-dependent fields remain unresolved. |
| `com.aionemu.gameserver.model.gameobjects.UseableItemObject` / `com.aionemu.gameserver.services.drop.DropService` reward add paths | future `GameServerConnection.HandleUseUseableItemObjectAsync` and `WorldNpcLootService.RequestDropItem` flag inputs | Service / Packet Orchestration | Partial | No Tests | Needs Verification | Parallel read-only audits found these as next cleanup/seal caller families. House-use rewards already have static data in `GameServerConnection`; world loot needs `ItemRestrictionCleanupTable` passed into `WorldNpcLootService` before add/update loot packets can compute the flag. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GamePacketTests.SmInventoryAddItem_WritesCleanupSealFlagInItemBlobLikeJava` | Regression / packet byte layout | `SM_INVENTORY_ADD_ITEM.writeItemInfo`, `GeneralInfoBlobEntry` | Existing guard that item-collect inventory add packets write cleanup/seal field `3` when supplied by the runtime caller. | C# packet-byte assertion against deterministic Java source order/field value. | Does not execute `CM_COMPOSITE_STONES` through a socket fixture and does not compare Java runtime bytes. |
| `GamePacketTests.SmInventoryUpdateItem_WritesCleanupSealFlagInItemBlobLikeJava` | Regression / packet byte layout | `SM_INVENTORY_UPDATE_ITEM.writeImpl`, `GeneralInfoBlobEntry` | Existing guard that normal full-blob inventory update packets write cleanup/seal field `3` when supplied by the runtime caller. | C# packet-byte assertion against deterministic Java source order/field value. | Does not execute the composition stacked-reward branch through a socket fixture. |
| `GamePacketTests.GameServerConnection_GetGeneralInfoWarehouseRestrictionFlag_UsesJavaCleanupPredicate` | Unit | `ItemRestrictionCleanupData.hasAccountOrLegionWhStorabilityDisabled` | Guards the shared live caller flag decision reused by composition rewards. | Existing source-derived C# assertions. | Not a Java runtime comparison. |

## Remaining Risks

- House-use rewards, assembly rewards, extraction rewards, XP extraction rewards, loot, quest/custom reward paths, other inventory add/update, warehouse-add, and future pet-feed/live callers still need cleanup-table or precomputed cleanup/seal context.
- No socket-level composition integration test yet asserts outbound `SmInventoryAddItem` or `SmInventoryUpdateItem` through `GameServerConnection`.
- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Temporary-exchange remaining seconds still serialize as zero.
- Runtime conditioning presence and wall-clock expiration/dye fields can still differ from Java.

## Summary Metrics

- Total Java artifacts discovered: 7 grouped artifact rows in this unit
- Total artifacts ported: 2 C# caller/packet surfaces changed or consumed
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 7 grouped rows
- Total blocked artifacts: house-use reward cleanup flag source, assembly/extract/XP extraction reward cleanup flag sources, world loot service cleanup flag source, remaining inventory add/update caller flag sources, warehouse-add/pet-feed live flag source, Java runtime artifact generation, temporary-exchange model/template/drop/task fields, time-normalized expiration/dye comparison
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Work Options

## Recommended Sequential Task

- Task: wire `GameServerConnection.HandleUseUseableItemObjectAsync` reward add/update cleanup/seal flag sourcing.
- Scope:
  - method already resolves `staticData`;
  - stack merge/update reward send currently uses `SmInventoryUpdateItem` with default cleanup flag;
  - new reward item send currently uses `SmInventoryAddItem.CreateItemCollect` with default cleanup flag;
  - compute flags with `GetGeneralInfoWarehouseRestrictionFlag(..., staticData.ItemRestrictionCleanups)`;
  - add focused connection-level house-use reward tests if an existing fixture can exercise both add and stacked-update branches, otherwise run existing packet/helper guards and document the socket-test gap.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Assembly/extract/XP extraction helper audit | read-only `GameServerConnection.cs` reward helpers | Low | Adjacent to the composition helper changed in UOW-1403; do not edit in parallel with another `GameServerConnection.cs` worker. |
| B | World loot cleanup/seal implementation prep | `WorldNpcLootService.cs`, `WorldNpcLootServiceTests` | Medium | Needs optional `ItemRestrictionCleanupTable?` threaded through `RequestDropItem` and focused add/update loot tests. |
| C | Temporary exchange model UOW | `InventoryItem.cs`, `ItemTemplateTable.cs`, static-data tests | Medium | Separate from cleanup/seal; do not mix with runtime caller plumbing. |

## Do Not Parallelize

- Do not edit `GameServerConnection.cs` from multiple workers at once.
- Do not edit shared progress/handoff/parity docs from sub-agents; orchestrator owns docs.
- Do not claim verified parity for composition rewards until there is socket-level output comparison or generated Java runtime packet evidence.
