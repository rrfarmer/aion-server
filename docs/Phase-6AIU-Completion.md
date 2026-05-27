# Phase 6AIU Completion - Idian Polish Cleanup Seal Metadata

Date: 2026-05-27
Unit of Work: UOW-1419
Status: Complete and committed after validation.

## Scope

Continue Phase 6 cleanup/seal full item-blob propagation by wiring the confirmed idian polish full update connection packets. Java remains the source of truth; this unit does not claim runtime byte parity.

## Completed Work

- Reviewed Java packet-shape guidance for item-info-change callers. `SM_INVENTORY_UPDATE_ITEM` writes full item blobs for default updates, including polish success, and only uses partial payloads for `EQUIP_UNEQUIP`, `CHARGE`, and `POLISH_CHARGE`.
- Updated `GameServerConnection.ApplyIdianPolishPlanAsync` so non-deleted source material updates and successful target item updates pass `StaticData.ItemRestrictionCleanups` into `SmInventoryUpdateItem`.
- Kept source deletion on `SmDeleteItem` and kept low-charge `POLISH_CHARGE` concerns out of this cleanup/seal wiring unit.
- Added focused connection-level packet coverage proving restricted idian polish source and target full update blobs carry cleanup/seal flag `3`.
- Updated `docs/PHASE-6-PROGRESS.md`, `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageCleanupSealStaticDataAudit.md`, and `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageCSharpBlobGapAudit.md`.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~GameServerConnectionIdianPolishTests.ApplyIdianPolishPlanAsync_WritesCleanupSealFlagForRestrictedFullUpdates|FullyQualifiedName~IdianPolishServiceTests.CreatePolishPlan_AppliesSelectedBonusAndConsumesOneIdian|FullyQualifiedName~GamePacketTests.SmInventoryUpdateItem_WritesCleanupSealFlagInItemBlobLikeJava"`.
- Result: passed 3 tests.

## Migration Parity Table - UOW-1419

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.templates.item.actions.PolishAction.act` | `Aion.GameServer.Network.Aion.GameServerConnection.ApplyIdianPolishPlanAsync` | Item Action / Connection Packet Caller | Partial | Regression Tested | Partial Parity | Success target update and non-deleted source update now consume cleanup static-data context for full update blobs. Java delayed task/cooldown ordering, random-result scheduling, inventory persistence timing, and runtime byte comparison remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM` default branch | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem` via idian polish completion | Packet | Partial | Regression Tested | Partial Parity | Java source confirms polish success target uses default full `ItemInfoBlob.getFullBlob`; focused C# test asserts cleanup/seal flag `3` reaches source and target `DEC_ITEM_USE` full blobs. No Java runtime byte capture was available. |
| `com.aionemu.gameserver.model.items.storage.Storage.decreaseItemCount` remaining-stack item-use update path | `Aion.GameServer.Network.Aion.GameServerConnection.ApplyIdianPolishPlanAsync` source material update | Storage / Packet Caller | Partial | Regression Tested | Partial Parity | Non-deleted source material updates now get the cleanup flag. Zero-count source material still uses `SmDeleteItem` and carries no item blob. Java source item update was reviewed as a remaining-stack full update path, but runtime packet bytes remain unverified. |
| `com.aionemu.gameserver.dataholders.ItemRestrictionCleanupData` | `Aion.GameServer.Dataholders.ItemRestrictionCleanupTable` consumed by idian polish connection path | Dataholder Context | Partial | Regression Tested | Partial Parity | This unit consumes the existing cleanup table through `StaticData.ItemRestrictionCleanups`; loader behavior was not changed. Missing/static-data absence still falls back to flag `0`. |
| `com.aionemu.gameserver.network.aion.iteminfo.GeneralInfoBlobEntry` cleanup/seal flag | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteGeneralInfoBlob` via idian polish full updates | Serialization Entry | Partial | Regression Tested | Partial Parity | Idian polish full update packets can now feed the Java-shaped field. Temporary-exchange remaining seconds, runtime conditioning presence, and time-normalized expiration/dye fields remain unresolved. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GameServerConnectionIdianPolishTests.ApplyIdianPolishPlanAsync_WritesCleanupSealFlagForRestrictedFullUpdates` | Regression / connection packet serialization | `PolishAction.act`, `Storage.decreaseItemCount`, `SM_INVENTORY_UPDATE_ITEM`, `GeneralInfoBlobEntry` | Restricted idian polish source `DEC_ITEM_USE` update and success target `DEC_ITEM_USE` update both emit full blobs with cleanup/seal flag `3`; player inventory state reflects source count decrease and target random-bonus mutation. | C# packet parsing against reviewed Java full-blob packet shape and supplied static-data predicate. | Does not execute Java delayed action/cooldown flow or compare Java runtime bytes. |
| `IdianPolishServiceTests.CreatePolishPlan_AppliesSelectedBonusAndConsumesOneIdian` | Regression / service plan | `PolishAction.act` success branch | Existing idian polish plan behavior remains stable after connection packet wiring. | Source-derived C# assertions. | Does not inspect packets. |
| `GamePacketTests.SmInventoryUpdateItem_WritesCleanupSealFlagInItemBlobLikeJava` | Regression / packet serialization | `SM_INVENTORY_UPDATE_ITEM`, `GeneralInfoBlobEntry` | Existing full update packet cleanup/seal wrapper coverage still passes. | Deterministic C# packet parsing. | No Java runtime bytes. |

## Remaining Risks

- Remaining `GameServerConnection` full-blob update callers still need cleanup/seal flag wiring: enchant target/supplement/source, socket manastone, godstone, remodel, dye, unwrap/pack/tune, armsfusion, and other item-action paths.
- `EQUIP_UNEQUIP`, `CHARGE`, and `POLISH_CHARGE` remain partial Java packet modes and should not receive cleanup/seal metadata unless a separate unit proves a full blob is written for a specific caller.
- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Temporary-exchange remaining seconds, runtime conditioning presence, dynamic polish/conditioning state, and time-normalized expiration/dye comparison remain unresolved item-blob risks.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 C# connection idian polish packet caller changed
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: remaining `GameServerConnection` full-blob update caller flag sources, Java runtime artifact generation, temporary-exchange model/template/drop/task fields, time-normalized expiration/dye comparison
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: next confirmed full-blob `GameServerConnection` item-info-change cleanup-seal wiring.
- Scope:
  - choose one narrow Java-confirmed full update branch from enchant level, manastone socket/remove, godstone socket, remodel/remove-remodel, or dye;
  - inspect the current C# caller and test seam before editing;
  - wire only confirmed full update packets with `GameServerConnection.GetGeneralInfoWarehouseRestrictionFlag`;
  - add focused connection-level packet tests for restricted updated items;
  - update all parity/progress/handoff docs and commit the completed unit.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Java branch-specific audit for the chosen next caller | read-only Java service/action/packet sources | Low | Confirm delete-only, kinah, and partial branches before editing. |
| B | C# connection test-surface audit for the chosen next caller | read-only `GameServerConnection.cs`, existing tests | Low | Identify the smallest existing fixture/helper pattern. |
| C | Remaining full-blob caller inventory | read-only docs/source | Low | Keep the backlog current without touching shared code. |

## Do Not Parallelize

- Do not edit `GameServerConnection.cs` from multiple workers at once.
- Do not edit shared progress/handoff/parity docs from sub-agents; orchestrator owns docs.
- Do not wire cleanup/seal metadata into `EQUIP_UNEQUIP`, `CHARGE`, or `POLISH_CHARGE` unless Java source proves a full blob is written.
- Do not claim verified parity until Java runtime packet evidence or deterministic generated artifact comparison exists.

## Context For Next Session

- Current phase: Phase 6, cleanup/seal full item-blob flag propagation across live or staged inventory-producing callers.
- Last completed UOW: UOW-1419.
- Commit: `[Phase 6][UOW-1419] Wire idian polish cleanup seal metadata`.
- Key changed files:
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionIdianPolishTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageCleanupSealStaticDataAudit.md`
  - `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageCSharpBlobGapAudit.md`
  - `docs/Phase-6AIU-Completion.md`
