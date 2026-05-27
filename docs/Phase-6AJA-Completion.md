# Phase 6AJA Completion - Item Amplification Cleanup Seal Metadata

Date: 2026-05-27
Unit of Work: UOW-1425
Status: Complete pending commit after validation.

## Scope

Continue Phase 6 cleanup/seal full item-blob propagation by wiring the Java-confirmed item amplification material, tool, and target full update connection packets. Java remains the source of truth; this unit does not claim runtime byte parity.

## Completed Work

- Selected the item amplification material/tool/target full update path.
- Spawned read-only explorers for Java amplification packet shape and the C# amplification test seam. The Java amplification report was integrated; the C# seam explorer was closed before output.
- Extracted successful amplification packet fanout into `GameServerConnection.CompleteAmplifyItemAsync`.
- Wired remaining-stack material/tool item updates and target amplification full item updates to pass cleanup/seal context into `SmInventoryUpdateItem`.
- Corrected the target update mask from `0` to `SmInventoryUpdateItem.DecreaseItemUse`, matching Java's default `ItemPacketService.updateItemAfterInfoChange(player, targetItem)` path.
- Kept consumed material/tool deletion, cube-size updates, persistence side effects, and failure messages out of this narrow cleanup/seal unit.
- Updated `docs/PHASE-6-PROGRESS.md`, `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageCleanupSealStaticDataAudit.md`, `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageCSharpBlobGapAudit.md`, and corrected the latest prior handoff status wording.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~GameServerConnectionAmplificationTests.CompleteAmplifyItemAsync_WritesCleanupSealFlagForRestrictedMaterialToolAndTargetFullUpdates|FullyQualifiedName~EnchantServiceTests.CreateAmplificationPlan_AmplifiesTargetAndConsumesSources|FullyQualifiedName~GamePacketTests.SmInventoryUpdateItem_WritesCleanupSealFlagInItemBlobLikeJava"`.
- Result: passed 3 tests.

## Migration Parity Table - UOW-1425

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_MANASTONE` action type `8` | `Aion.GameServer.Network.Aion.ClientPackets.CmManastone` / `Aion.GameServer.Network.Aion.GameServerConnection.HandleAmplifyItemAsync` | Client Packet / Handler | Partial | Regression Tested | Partial Parity | Java maps packet supplement id to material and stone id to tool before calling `EnchantService.amplifyItem`. This unit tests the helper fanout, not the full packet parser/handler path. |
| `com.aionemu.gameserver.services.EnchantService.amplifyItem` success branch | `Aion.GameServer.Services.EnchantService.CreateAmplificationPlan` plus `Aion.GameServer.Network.Aion.GameServerConnection.CompleteAmplifyItemAsync` | Service / Connection Packet Caller | Partial | Regression Tested | Partial Parity | Remaining-stack material/tool updates and target full update packets now consume cleanup/seal static-data context and use Java default `DEC_ITEM_USE` mask. Java no-rollback behavior after material consume/tool failure is not executed in this helper test. |
| `com.aionemu.gameserver.model.items.storage.Storage.decreaseByObjectId` material/tool consume path | `Aion.GameServer.Network.Aion.GameServerConnection.SendItemUseMutationAsync` via amplification fanout | Storage / Packet Caller | Partial | Regression Tested | Partial Parity | Remaining-stack material/tool updates now carry cleanup/seal flag `3`; consumed material/tool still use `SmDeleteItem` and carry no item blob. Java also sends cube-size update after source delete, which remains outside this narrow full-blob metadata unit. |
| `com.aionemu.gameserver.services.item.ItemPacketService.updateItemAfterInfoChange` default branch | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem` via amplification target update | Packet Update Type | Partial | Regression Tested | Partial Parity | Java source confirms target amplification update uses full item blob plus default `DEC_ITEM_USE`; focused C# test asserts update type `0x16` and cleanup/seal flag `3`. No Java runtime byte capture was available. |
| `com.aionemu.gameserver.network.aion.iteminfo.EnchantInfoBlobEntry` amplified flag | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteEnchantInfo` | Serialization Entry | Partial | Regression Tested | Needs Verification | Existing C# serializer writes `item.IsAmplified`; this unit routes the amplified target through the full blob but does not compare Java runtime enchant-info bytes. |
| `com.aionemu.gameserver.dataholders.ItemRestrictionCleanupData` | `Aion.GameServer.Dataholders.ItemRestrictionCleanupTable` consumed by amplification connection path | Dataholder Context | Partial | Regression Tested | Partial Parity | This unit consumes the existing cleanup table through `StaticData.ItemRestrictionCleanups` / helper context; loader behavior was not changed. Missing/static-data absence still falls back to flag `0`. |
| `com.aionemu.gameserver.network.aion.iteminfo.GeneralInfoBlobEntry` cleanup/seal flag | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteGeneralInfoBlob` via amplification material/tool/target full updates | Serialization Entry | Partial | Regression Tested | Partial Parity | Amplification material/tool/target full update packets can now feed the Java-shaped field. Temporary-exchange remaining seconds, runtime conditioning presence, amplified enchant-info byte-level comparison, and Java runtime byte comparison remain unresolved. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GameServerConnectionAmplificationTests.CompleteAmplifyItemAsync_WritesCleanupSealFlagForRestrictedMaterialToolAndTargetFullUpdates` | Regression / connection packet serialization | `CM_MANASTONE` action type `8`, `EnchantService.amplifyItem`, `Storage.decreaseByObjectId`, `ItemPacketService.updateItemAfterInfoChange`, `SM_INVENTORY_UPDATE_ITEM`, `GeneralInfoBlobEntry` | Success packet order for remaining-stack material/tool is material update, tool update, success system message, target full update; all updates use Java default `DEC_ITEM_USE` and carry cleanup/seal flag `3`. | C# packet parsing against reviewed Java full-blob packet shape, update mask, order, and supplied static-data predicate. | Does not execute Java runtime, consumed source delete/cube-size branches, material-success/tool-failure no-rollback behavior, full handler validation, or compare Java runtime bytes. |
| `EnchantServiceTests.CreateAmplificationPlan_AmplifiesTargetAndConsumesSources` | Regression / service plan | `EnchantService.amplifyItem` normal success branch | Existing amplification plan target amplified flag and source consumption behavior remains stable after packet fanout extraction. | Source-derived C# assertions. | Does not inspect packets or Java no-rollback failure edge. |
| `GamePacketTests.SmInventoryUpdateItem_WritesCleanupSealFlagInItemBlobLikeJava` | Regression / packet serialization | `SM_INVENTORY_UPDATE_ITEM`, `GeneralInfoBlobEntry` | Existing full update packet cleanup/seal wrapper coverage still passes. | Deterministic C# packet parsing. | No Java runtime bytes. |

## Remaining Risks

- Remaining `GameServerConnection` full-blob update callers still need cleanup/seal flag wiring: enchant item source/target/supplement, unwrap/pack/tune, armsfusion, admin dye, house-object dye source consume, and other item-action paths.
- Amplification consumed material/tool branches may still need Java cube-size packet parity; this unit intentionally covered only remaining-stack full update metadata.
- Java's material-consumed/tool-failure no-rollback edge is documented but not modeled or tested in this unit.
- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Temporary-exchange remaining seconds, runtime conditioning presence, amplified enchant-info bytes, and time-normalized expiration/dye comparison remain unresolved item-blob risks.

## Summary Metrics

- Total Java artifacts discovered: 7 grouped artifact rows in this unit
- Total artifacts ported: 1 C# connection amplification packet fanout changed
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 7 grouped rows
- Total blocked artifacts: remaining `GameServerConnection` full-blob update caller flag sources, Java runtime artifact generation, consumed material/tool cube-size parity for amplification, material-success/tool-failure no-rollback edge, temporary-exchange model/template/drop/task fields, time-normalized expiration/dye comparison
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: another Java-confirmed full-blob item-info-change cleanup-seal caller.
- Scope:
  - inspect enchant item source/target/supplement updates or unwrap/pack/tune item-action updates;
  - wire only confirmed full update packets with `GameServerConnection.GetGeneralInfoWarehouseRestrictionFlag` or optional helper context;
  - keep kinah, deletion, and partial packet modes out of cleanup/seal wiring unless explicitly scoped;
  - add focused connection-level packet tests for restricted updated items;
  - update all parity/progress/handoff docs and commit the completed unit.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Enchant item Java packet shape | read-only Java enchant/storage/packet source | Medium | Larger branch with source, target, supplement, failure, buff-skill, and announce side effects. |
| B | Unwrap/pack item-action Java audit | read-only Java item-action/client-packet sources | Low | Smaller item-action candidates; confirm full blobs and masks before wiring. |
| C | Tune result C# seam | read-only connection/service/tests | Medium | Requires checking packet source and whether target update is full blob. |

## Do Not Parallelize

- Do not edit `GameServerConnection.cs` from multiple workers at once.
- Do not edit shared progress/handoff/parity docs from sub-agents; orchestrator owns docs.
- Do not wire cleanup/seal metadata into `EQUIP_UNEQUIP`, `CHARGE`, or `POLISH_CHARGE` unless Java source proves a full blob is written.
- Do not claim verified parity until Java runtime packet evidence or deterministic generated artifact comparison exists.

## Context For Next Session

- Current phase: Phase 6, cleanup/seal full item-blob flag propagation across live or staged inventory-producing callers.
- Last completed UOW: UOW-1425.
- Commit: `[Phase 6][UOW-1425] Wire item amplification cleanup seal metadata`.
- Key changed files:
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionAmplificationTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageCleanupSealStaticDataAudit.md`
  - `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageCSharpBlobGapAudit.md`
  - `docs/Phase-6AIZ-Completion.md`
  - `docs/Phase-6AJA-Completion.md`
