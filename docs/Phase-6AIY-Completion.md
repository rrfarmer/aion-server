# Phase 6AIY Completion - Dye Item Cleanup Seal Metadata

Date: 2026-05-27
Unit of Work: UOW-1423
Status: Complete and committed after validation.

## Scope

Continue Phase 6 cleanup/seal full item-blob propagation by wiring the Java-confirmed dye item source and target full update connection packets. Java remains the source of truth; this unit does not claim runtime byte parity.

## Completed Work

- Selected the dye item action source/target full update path.
- Spawned read-only explorers for Java dye packet shape and C# dye test seam. The Java dye report was integrated; the C# seam explorer was closed unused before integration.
- Updated `GameServerConnection.HandleDyeUseItemAsync` to accept cleanup/seal context from `StaticData.ItemRestrictionCleanups`.
- Wired non-deleted dye source item updates and target dye full item updates to pass cleanup/seal context into `SmInventoryUpdateItem`.
- Corrected the target update mask from `0` to `SmInventoryUpdateItem.DecreaseItemUse`, matching Java's default `ItemPacketService.updateItemAfterInfoChange(player, targetItem)` path.
- Kept source deletion, equipped appearance fanout, house-object dye, admin dye, and time-dependent dye byte comparison out of this narrow cleanup/seal unit.
- Updated `docs/PHASE-6-PROGRESS.md`, `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageCleanupSealStaticDataAudit.md`, `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageCSharpBlobGapAudit.md`, and corrected the latest prior handoff status wording.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~GameServerConnectionDyeItemTests.HandleDyeUseItemAsync_WritesCleanupSealFlagForRestrictedSourceAndTargetFullUpdates|FullyQualifiedName~PlayerStateTests.DyeService_MatchesJavaTargetAndExpirationRules|FullyQualifiedName~GamePacketTests.SmInventoryUpdateItem_WritesCleanupSealFlagInItemBlobLikeJava"`.
- Result: passed 3 tests.

## Migration Parity Table - UOW-1423

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.templates.item.actions.DyeAction.dyeItem` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleDyeUseItemAsync` | Item Action / Connection Packet Caller | Partial | Regression Tested | Partial Parity | Non-deleted source dye item updates and target dye full updates now consume cleanup static-data context and use Java default `DEC_ITEM_USE` mask. Source deletion, equipped appearance broadcast ordering, house-object dye, admin dye, wall-clock dye expiry drift, persistence timing, and runtime byte comparison remain unverified. |
| `com.aionemu.gameserver.model.items.storage.Storage.decreaseByObjectId` / `decreaseItemCount` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleDyeUseItemAsync` source consume path | Storage / Packet Caller | Partial | Regression Tested | Partial Parity | Source remaining-stack update now carries cleanup/seal flag `3`; consumed source still uses `SmDeleteItem` and carries no item blob. Java also sends cube-size update after source delete, which remains outside this narrow full-blob metadata unit. |
| `com.aionemu.gameserver.services.item.ItemPacketService.updateItemAfterInfoChange` default branch | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem` via dye target update | Packet Update Type | Partial | Regression Tested | Partial Parity | Java source confirms target dye update uses full item blob plus `DEC_ITEM_USE`; focused C# test asserts update type `0x16` and cleanup/seal flag `3`. No Java runtime byte capture was available. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_UPDATE_PLAYER_APPEARANCE` dye branch | `Aion.GameServer.Network.Aion.ServerPackets.SmUpdatePlayerAppearance` from equipped dye branch | Appearance Packet | Partial | Manual Only | Needs Verification | Existing C# branch still sends appearance before target inventory update when target is equipped, but this unit did not add socket-order coverage for equipped targets. Appearance packet carries color but not dye expiry. |
| `com.aionemu.gameserver.dataholders.ItemRestrictionCleanupData` | `Aion.GameServer.Dataholders.ItemRestrictionCleanupTable` consumed by dye item connection path | Dataholder Context | Partial | Regression Tested | Partial Parity | This unit consumes the existing cleanup table through `StaticData.ItemRestrictionCleanups`; loader behavior was not changed. Missing/static-data absence still falls back to flag `0`. |
| `com.aionemu.gameserver.network.aion.iteminfo.GeneralInfoBlobEntry` cleanup/seal flag | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteGeneralInfoBlob` via dye source/target full updates | Serialization Entry | Partial | Regression Tested | Partial Parity | Dye source and target full update packets can now feed the Java-shaped field. Temporary-exchange remaining seconds, runtime conditioning presence, time-normalized dye expiration/color fields, and Java runtime byte comparison remain unresolved. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GameServerConnectionDyeItemTests.HandleDyeUseItemAsync_WritesCleanupSealFlagForRestrictedSourceAndTargetFullUpdates` | Regression / connection packet serialization | `DyeAction.dyeItem`, `Storage.decreaseByObjectId`, `ItemPacketService.updateItemAfterInfoChange`, `SM_INVENTORY_UPDATE_ITEM`, `GeneralInfoBlobEntry` | Success packet order for non-equipped, non-deleted source is source update, system message, target full update; both updates use Java default `DEC_ITEM_USE` and carry cleanup/seal flag `3`. | C# packet parsing against reviewed Java full-blob packet shape, update mask, order, and supplied static-data predicate. | Does not execute Java runtime, source delete/cube-size branch, equipped appearance branch, house-object/admin dye, or compare Java runtime bytes. |
| `PlayerStateTests.DyeService_MatchesJavaTargetAndExpirationRules` | Regression / service plan | `DyeAction.canAct` and `DyeAction.dyeItem` expiry/color rules | Existing dye plan target and expiration behavior remains stable after connection packet wiring. | Source-derived C# assertions. | Does not inspect packets or compare runtime clock-normalized bytes. |
| `GamePacketTests.SmInventoryUpdateItem_WritesCleanupSealFlagInItemBlobLikeJava` | Regression / packet serialization | `SM_INVENTORY_UPDATE_ITEM`, `GeneralInfoBlobEntry` | Existing full update packet cleanup/seal wrapper coverage still passes. | Deterministic C# packet parsing. | No Java runtime bytes. |

## Remaining Risks

- Remaining `GameServerConnection` full-blob update callers still need cleanup/seal flag wiring: enchant item source/target/supplement, remodel, unwrap/pack/tune, armsfusion, amplification, admin dye, house-object dye source consume, and other item-action paths.
- Dye source delete branch may still need Java cube-size packet parity; this unit intentionally covered only remaining-stack full update metadata.
- Equipped dye appearance broadcast ordering and exact appearance bytes remain unverified.
- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Temporary-exchange remaining seconds, runtime conditioning presence, and time-normalized dye expiration/color comparison remain unresolved item-blob risks.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 1 C# connection dye item packet caller changed
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: remaining `GameServerConnection` full-blob update caller flag sources, Java runtime artifact generation, source-delete cube-size parity for dye, equipped appearance byte/order parity, temporary-exchange model/template/drop/task fields, time-normalized expiration/dye comparison
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: another Java-confirmed full-blob item-info-change cleanup-seal caller.
- Scope:
  - inspect remodel target/extract updates or enchant item source/target/supplement updates;
  - wire only confirmed full update packets with `GameServerConnection.GetGeneralInfoWarehouseRestrictionFlag` or optional helper context;
  - keep kinah, deletion, appearance fanout, and partial packet modes out of cleanup/seal wiring unless explicitly scoped;
  - add focused connection-level packet tests for restricted updated items;
  - update all parity/progress/handoff docs and commit the completed unit.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Remodel C# seam | read-only connection/service/tests | Medium | Has kinah, extract source, target update, and skin/color persistence branches. |
| B | Enchant item C# seam | read-only connection/service/tests | Medium | Larger branch with source, target, supplement, failure, and buff-skill side effects. |
| C | Dye delete/equipped appearance parity audit | read-only Java/C# source/tests | Low | Separate from cleanup-seal metadata; can map cube-size and appearance-order gaps. |

## Do Not Parallelize

- Do not edit `GameServerConnection.cs` from multiple workers at once.
- Do not edit shared progress/handoff/parity docs from sub-agents; orchestrator owns docs.
- Do not wire cleanup/seal metadata into `EQUIP_UNEQUIP`, `CHARGE`, or `POLISH_CHARGE` unless Java source proves a full blob is written.
- Do not claim verified parity until Java runtime packet evidence or deterministic generated artifact comparison exists.

## Context For Next Session

- Current phase: Phase 6, cleanup/seal full item-blob flag propagation across live or staged inventory-producing callers.
- Last completed UOW: UOW-1423.
- Commit: `[Phase 6][UOW-1423] Wire dye item cleanup seal metadata`.
- Key changed files:
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionDyeItemTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageCleanupSealStaticDataAudit.md`
  - `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageCSharpBlobGapAudit.md`
  - `docs/Phase-6AIX-Completion.md`
  - `docs/Phase-6AIY-Completion.md`
