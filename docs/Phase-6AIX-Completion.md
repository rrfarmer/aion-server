# Phase 6AIX Completion - Manastone Removal Cleanup Seal Metadata

Date: 2026-05-27
Unit of Work: UOW-1422
Status: Complete and committed after validation.

## Scope

Continue Phase 6 cleanup/seal full item-blob propagation by wiring the Java-confirmed manastone removal target full update connection packet. Java remains the source of truth; this unit does not claim runtime byte parity.

## Completed Work

- Selected the manastone removal target update path from the Java-confirmed full item-info-change list.
- Spawned read-only explorers for manastone removal and dye item-action seams. The manastone removal report was integrated; the dye explorer was closed unused before integration.
- Extracted the remove-manastone success packet body into `GameServerConnection.CompleteRemoveManastoneAsync` for direct packet-sequence coverage.
- Updated the target item update to pass cleanup/seal context into `SmInventoryUpdateItem`.
- Corrected the target update mask from `0` to `SmInventoryUpdateItem.DecreaseItemUse`, matching Java's default `ItemPacketService.updateItemAfterInfoChange(player, item)` path.
- Kept kinah payment as `DecreaseKinahBuy`; kinah is not treated as this unit's cleanup/seal target.
- Updated `docs/PHASE-6-PROGRESS.md`, `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageCleanupSealStaticDataAudit.md`, `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageCSharpBlobGapAudit.md`, and corrected the latest prior handoff status wording.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~GameServerConnectionManastoneRemovalTests.CompleteRemoveManastoneAsync_WritesCleanupSealFlagForRestrictedTargetFullUpdate|FullyQualifiedName~ItemSocketServiceTests.RemoveManastone_RemovesNormalStoneAndChargesKinah|FullyQualifiedName~GamePacketTests.SmInventoryUpdateItem_WritesCleanupSealFlagInItemBlobLikeJava"`.
- Result: passed 3 tests.

## Migration Parity Table - UOW-1422

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_MANASTONE` action type `3` | `Aion.GameServer.Network.Aion.ClientPackets.CmManastone` / `GameServerConnection.HandleRemoveManastoneAsync` | Client Packet / Connection Handler | Partial | Regression Tested | Partial Parity | C# packet read shape matches Java action type `3`; success completion now delegates to a testable helper. Java NPC type and talk-range validation remain weaker in C# and need a separate unit. |
| `com.aionemu.gameserver.services.item.ItemSocketService.removeManastone` | `Aion.GameServer.Network.Aion.GameServerConnection.CompleteRemoveManastoneAsync` | Service / Connection Packet Caller | Partial | Regression Tested | Partial Parity | Target full update now consumes cleanup static-data context and uses Java default `DEC_ITEM_USE` mask. Java DAO row-delete timing, target NPC range validation, exact system-message payload bytes, and runtime byte comparison remain unverified. |
| `com.aionemu.gameserver.services.item.ItemPacketService.updateItemAfterInfoChange` default branch | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem` via manastone removal target update | Packet Update Type | Partial | Regression Tested | Partial Parity | Java source confirms default target update uses full item blob plus `DEC_ITEM_USE`; focused C# test asserts update type `0x16` and cleanup/seal flag `3`. No Java runtime byte capture was available. |
| `com.aionemu.gameserver.model.items.storage.Storage.tryDecreaseKinah` / `decreaseKinah` | `Aion.GameServer.Network.Aion.GameServerConnection.CompleteRemoveManastoneAsync` kinah update | Storage / Packet Caller | Partial | Regression Tested | Partial Parity | Kinah update remains `DEC_KINAH_BUY` and is sent before success message, matching reviewed Java order. Cleanup/seal flag is not asserted for kinah because this unit targets item-info-change metadata on the removed-stone target item. |
| `com.aionemu.gameserver.dataholders.ItemRestrictionCleanupData` | `Aion.GameServer.Dataholders.ItemRestrictionCleanupTable` consumed by manastone removal connection path | Dataholder Context | Partial | Regression Tested | Partial Parity | This unit consumes the existing cleanup table through `StaticData.ItemRestrictionCleanups`; loader behavior was not changed. Missing/static-data absence still falls back to flag `0`. |
| `com.aionemu.gameserver.network.aion.iteminfo.GeneralInfoBlobEntry` cleanup/seal flag | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteGeneralInfoBlob` via manastone removal full update | Serialization Entry | Partial | Regression Tested | Partial Parity | Manastone removal target full update packets can now feed the Java-shaped field. Temporary-exchange remaining seconds, runtime conditioning presence, manastone entry byte-level comparison, and time-normalized expiration/dye fields remain unresolved. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GameServerConnectionManastoneRemovalTests.CompleteRemoveManastoneAsync_WritesCleanupSealFlagForRestrictedTargetFullUpdate` | Regression / connection packet serialization | `CM_MANASTONE`, `ItemSocketService.removeManastone`, `Storage.tryDecreaseKinah`, `ItemPacketService.updateItemAfterInfoChange`, `SM_INVENTORY_UPDATE_ITEM`, `GeneralInfoBlobEntry` | Success packet order is kinah update, success system message, target full update; target update uses Java default `DEC_ITEM_USE` and carries cleanup/seal flag `3`. | C# packet parsing against reviewed Java full-blob packet shape, update mask, order, and supplied static-data predicate. | Does not execute Java runtime NPC/talk-range validation, DAO delete timing, or compare Java runtime bytes. |
| `ItemSocketServiceTests.RemoveManastone_RemovesNormalStoneAndChargesKinah` | Regression / service plan | `ItemSocketService.removeManastone` success branch | Existing removal plan behavior remains stable after connection packet extraction. | Source-derived C# assertions. | Does not inspect packets. |
| `GamePacketTests.SmInventoryUpdateItem_WritesCleanupSealFlagInItemBlobLikeJava` | Regression / packet serialization | `SM_INVENTORY_UPDATE_ITEM`, `GeneralInfoBlobEntry` | Existing full update packet cleanup/seal wrapper coverage still passes. | Deterministic C# packet parsing. | No Java runtime bytes. |

## Remaining Risks

- Remaining `GameServerConnection` full-blob update callers still need cleanup/seal flag wiring: enchant item source/target/supplement, remodel, dye, unwrap/pack/tune, armsfusion, amplification, and other item-action paths.
- `HandleRemoveManastoneAsync` still has weaker Java guard coverage: it checks NPC object id equality but not target object type or talk-range parity.
- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Temporary-exchange remaining seconds, runtime conditioning presence, dynamic manastone entry bytes, and time-normalized expiration/dye comparison remain unresolved item-blob risks.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 1 C# connection manastone removal packet caller changed
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: remaining `GameServerConnection` full-blob update caller flag sources, Java runtime artifact generation, NPC/talk-range guard parity for manastone removal, temporary-exchange model/template/drop/task fields, time-normalized expiration/dye comparison
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: dye item action source/target cleanup-seal wiring.
- Scope:
  - inspect `DyeAction.dyeItem` and `GameServerConnection.HandleDyeUseItemAsync`;
  - wire only source remaining-stack and target full update packets;
  - keep source deletion, appearance fanout, house-object dye, admin dye, and time-dependent dye byte comparison out of the narrow cleanup/seal unit unless explicitly scoped;
  - add focused connection-level packet tests for restricted source/target items;
  - update all parity/progress/handoff docs and commit the completed unit.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Dye item action C# seam | read-only connection/service/tests | Medium | Time-dependent dye expiration remains a known byte-comparison risk. |
| B | Enchant item C# seam | read-only connection/service/tests | Medium | Larger branch with source, target, supplement, failure, and buff-skill side effects. |
| C | Manastone removal guard audit | read-only Java/C# NPC target and talk-range code | Low | Separate from cleanup-seal metadata; could close the weaker guard gap. |

## Do Not Parallelize

- Do not edit `GameServerConnection.cs` from multiple workers at once.
- Do not edit shared progress/handoff/parity docs from sub-agents; orchestrator owns docs.
- Do not wire cleanup/seal metadata into `EQUIP_UNEQUIP`, `CHARGE`, or `POLISH_CHARGE` unless Java source proves a full blob is written.
- Do not claim verified parity until Java runtime packet evidence or deterministic generated artifact comparison exists.

## Context For Next Session

- Current phase: Phase 6, cleanup/seal full item-blob flag propagation across live or staged inventory-producing callers.
- Last completed UOW: UOW-1422.
- Commit: `[Phase 6][UOW-1422] Wire manastone removal cleanup seal metadata`.
- Key changed files:
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionManastoneRemovalTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageCleanupSealStaticDataAudit.md`
  - `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageCSharpBlobGapAudit.md`
  - `docs/Phase-6AIW-Completion.md`
  - `docs/Phase-6AIX-Completion.md`
