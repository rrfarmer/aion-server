# Phase 6AIZ Completion - Item Remodel Cleanup Seal Metadata

Date: 2026-05-27
Unit of Work: UOW-1424
Status: Complete pending commit after validation.

## Scope

Continue Phase 6 cleanup/seal full item-blob propagation by wiring the Java-confirmed item remodel extract and target full update connection packets. Java remains the source of truth; this unit does not claim runtime byte parity.

## Completed Work

- Selected the item remodel/remodel-removal source/target full update path.
- Spawned read-only explorers for Java remodel packet shape and the C# remodel test seam; both reports were integrated.
- Extracted successful remodel packet fanout into `GameServerConnection.CompleteItemRemodelAsync`.
- Wired remaining-stack extract item updates and target remodel full item updates to pass cleanup/seal context into `SmInventoryUpdateItem`.
- Kept kinah, consumed extract deletion/cube update, remodel persistence timing, and success/failure messages out of this narrow cleanup/seal unit.
- Updated `docs/PHASE-6-PROGRESS.md`, `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageCleanupSealStaticDataAudit.md`, `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageCSharpBlobGapAudit.md`, and corrected the latest prior handoff status wording.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~GameServerConnectionItemRemodelTests.CompleteItemRemodelAsync_WritesCleanupSealFlagForRestrictedExtractAndTargetFullUpdates|FullyQualifiedName~ItemRemodelServiceTests.CreateRemodelPlan_AppliesExtractedSkinAndConsumesPaymentAndExtractItem|FullyQualifiedName~GamePacketTests.SmInventoryUpdateItem_WritesCleanupSealFlagInItemBlobLikeJava"`.
- Result: passed 3 tests.

## Migration Parity Table - UOW-1424

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_ITEM_REMODEL` | `Aion.GameServer.Network.Aion.ClientPackets.CmItemRemodel` / `Aion.GameServer.Network.Aion.GameServerConnection.HandleItemRemodelAsync` | Client Packet / Handler | Partial | Regression Tested | Partial Parity | Java packet reads NPC, keep item, extract item, and unknown fields before delegating to `ItemRemodelService.remodelItem`. This unit did not execute the full client-packet path or validate NPC talk/range behavior. |
| `com.aionemu.gameserver.services.item.ItemRemodelService.remodelItem` normal and pattern-reshaper success branches | `Aion.GameServer.Services.ItemRemodelService.CreateRemodelPlan` plus `Aion.GameServer.Network.Aion.GameServerConnection.CompleteItemRemodelAsync` | Service / Connection Packet Caller | Partial | Regression Tested | Partial Parity | Remaining-stack extract and target full update packets now consume cleanup/seal static-data context and use Java default `DEC_ITEM_USE` mask. Pattern reshaper shares the same fanout when extract remains stacked. Consumed extract delete/cube update, Java runtime persistence timing, and full handler validation remain unverified. |
| `com.aionemu.gameserver.model.items.storage.Storage.decreaseKinah` / `tryDecreaseKinah` | `Aion.GameServer.Network.Aion.GameServerConnection.CompleteItemRemodelAsync` kinah update path | Storage / Packet Caller | Partial | Regression Tested | Partial Parity | Java sends kinah full update first with `DEC_KINAH_BUY`; C# packet order test keeps this first. Kinah is not a cleanup/seal target in this unit and no Java runtime byte capture was available. |
| `com.aionemu.gameserver.model.items.storage.Storage.decreaseItemCount` extract source branch | `Aion.GameServer.Network.Aion.GameServerConnection.CompleteItemRemodelAsync` extract consume path | Storage / Packet Caller | Partial | Regression Tested | Partial Parity | Remaining-stack extract updates now carry cleanup/seal flag `3`; consumed extract still uses `SmDeleteItem` and carries no item blob. Java also sends cube-size update after source delete, which remains outside this narrow full-blob metadata unit. |
| `com.aionemu.gameserver.services.item.ItemPacketService.updateItemAfterInfoChange` default branch | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem` via remodel target update | Packet Update Type | Partial | Regression Tested | Partial Parity | Java source confirms target remodel update uses full item blob plus default `DEC_ITEM_USE`; focused C# test asserts update type `0x16` and cleanup/seal flag `3`. No Java runtime byte capture was available. |
| `com.aionemu.gameserver.dataholders.ItemRestrictionCleanupData` | `Aion.GameServer.Dataholders.ItemRestrictionCleanupTable` consumed by remodel connection path | Dataholder Context | Partial | Regression Tested | Partial Parity | This unit consumes the existing cleanup table through `StaticData.ItemRestrictionCleanups` / helper context; loader behavior was not changed. Missing/static-data absence still falls back to flag `0`. |
| `com.aionemu.gameserver.network.aion.iteminfo.GeneralInfoBlobEntry` cleanup/seal flag | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteGeneralInfoBlob` via remodel extract/target full updates | Serialization Entry | Partial | Regression Tested | Partial Parity | Remodel extract and target full update packets can now feed the Java-shaped field. Temporary-exchange remaining seconds, runtime conditioning presence, item-skin/dye byte-level comparison, and Java runtime byte comparison remain unresolved. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GameServerConnectionItemRemodelTests.CompleteItemRemodelAsync_WritesCleanupSealFlagForRestrictedExtractAndTargetFullUpdates` | Regression / connection packet serialization | `CM_ITEM_REMODEL`, `ItemRemodelService.remodelItem`, `Storage.decreaseKinah`, `Storage.decreaseItemCount`, `ItemPacketService.updateItemAfterInfoChange`, `SM_INVENTORY_UPDATE_ITEM`, `GeneralInfoBlobEntry` | Success packet order for remaining-stack extract is kinah update, extract full update, target full update, success system message; extract and target updates use Java default `DEC_ITEM_USE` and carry cleanup/seal flag `3`. | C# packet parsing against reviewed Java full-blob packet shape, update mask, order, and supplied static-data predicate. | Does not execute Java runtime, consumed extract delete/cube-size branch, full handler validation, or compare Java runtime bytes. |
| `ItemRemodelServiceTests.CreateRemodelPlan_AppliesExtractedSkinAndConsumesPaymentAndExtractItem` | Regression / service plan | `ItemRemodelService.remodelItem` normal success branch | Existing remodel plan target skin/color, kinah payment, and extract stack behavior remains stable after packet fanout extraction. | Source-derived C# assertions. | Does not inspect packets or persistence. |
| `GamePacketTests.SmInventoryUpdateItem_WritesCleanupSealFlagInItemBlobLikeJava` | Regression / packet serialization | `SM_INVENTORY_UPDATE_ITEM`, `GeneralInfoBlobEntry` | Existing full update packet cleanup/seal wrapper coverage still passes. | Deterministic C# packet parsing. | No Java runtime bytes. |

## Remaining Risks

- Remaining `GameServerConnection` full-blob update callers still need cleanup/seal flag wiring: enchant item source/target/supplement, unwrap/pack/tune, armsfusion, amplification, admin dye, house-object dye source consume, and other item-action paths.
- Remodel consumed extract branch may still need Java cube-size packet parity; this unit intentionally covered only remaining-stack full update metadata.
- Full `HandleItemRemodelAsync` still needs Java-level NPC object/range validation parity if that surface becomes modeled.
- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Temporary-exchange remaining seconds, runtime conditioning presence, item-skin/dye bytes, and time-normalized expiration/dye comparison remain unresolved item-blob risks.

## Summary Metrics

- Total Java artifacts discovered: 7 grouped artifact rows in this unit
- Total artifacts ported: 1 C# connection remodel packet fanout changed
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 7 grouped rows
- Total blocked artifacts: remaining `GameServerConnection` full-blob update caller flag sources, Java runtime artifact generation, consumed extract cube-size parity for remodel, full NPC/range validation parity, temporary-exchange model/template/drop/task fields, time-normalized expiration/dye comparison
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: another Java-confirmed full-blob item-info-change cleanup-seal caller.
- Scope:
  - inspect enchant item source/target/supplement updates, amplification target/source-material updates, or unwrap/pack/tune item-action updates;
  - wire only confirmed full update packets with `GameServerConnection.GetGeneralInfoWarehouseRestrictionFlag` or optional helper context;
  - keep kinah, deletion, and partial packet modes out of cleanup/seal wiring unless explicitly scoped;
  - add focused connection-level packet tests for restricted updated items;
  - update all parity/progress/handoff docs and commit the completed unit.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Enchant item Java packet shape | read-only Java enchant/storage/packet source | Medium | Larger branch with source, target, supplement, failure, buff-skill, and announce side effects. |
| B | Amplification C# seam | read-only connection/service/tests | Medium | Adjacent connection branch has target and source-material update packets. |
| C | Unwrap/pack/tune Java audit | read-only Java item-action/client-packet sources | Low | Smaller item-action candidates; confirm full blobs and masks before wiring. |

## Do Not Parallelize

- Do not edit `GameServerConnection.cs` from multiple workers at once.
- Do not edit shared progress/handoff/parity docs from sub-agents; orchestrator owns docs.
- Do not wire cleanup/seal metadata into `EQUIP_UNEQUIP`, `CHARGE`, or `POLISH_CHARGE` unless Java source proves a full blob is written.
- Do not claim verified parity until Java runtime packet evidence or deterministic generated artifact comparison exists.

## Context For Next Session

- Current phase: Phase 6, cleanup/seal full item-blob flag propagation across live or staged inventory-producing callers.
- Last completed UOW: UOW-1424.
- Commit: `[Phase 6][UOW-1424] Wire item remodel cleanup seal metadata`.
- Key changed files:
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionItemRemodelTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageCleanupSealStaticDataAudit.md`
  - `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageCSharpBlobGapAudit.md`
  - `docs/Phase-6AIY-Completion.md`
  - `docs/Phase-6AIZ-Completion.md`
