# Phase 6 AIT Completion - Stigma Charge Cleanup-Seal Metadata

Date: 2026-05-27
Unit of Work: UOW-1418
Status: stigma charge full update connection packets now consume cleanup-seal static-data context.

## Completed

- Performed parallel read-only discovery over Java stigma packet shape and C# connection test surfaces.
- Confirmed Java stigma equip/remove use partial `EQUIP_UNEQUIP` packets and should not receive cleanup/seal metadata.
- Confirmed Java stigma charge success default update and nonzero `DEC_STIGMA_USE` updates write full `SM_INVENTORY_UPDATE_ITEM` blobs.
- Updated `GameServerConnection.CompleteStigmaChargeAsync` so stigma charge source, success target, and failed nonzero target full update packets pass `StaticData.ItemRestrictionCleanups`.
- Added focused connection-level packet coverage for restricted stigma source and target full update blobs.
- Updated progress, cleanup/seal audit, and C# blob gap audit.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~GameServerConnectionStigmaChargeTests.CompleteStigmaChargeAsync_WritesCleanupSealFlagForRestrictedFullUpdates|FullyQualifiedName~StigmaServiceTests.CreateChargePlan_SuccessConsumesStoneAndRaisesEquippedStigmaSkillLevel|FullyQualifiedName~GamePacketTests.SmInventoryUpdateItem_WritesCleanupSealFlagInItemBlobLikeJava"`.
- Result: passed 3 tests.

## Migration Parity Table - UOW-1418

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.StigmaService.chargeStigma` | `Aion.GameServer.Network.Aion.GameServerConnection.CompleteStigmaChargeAsync` | Service / Connection Packet Caller | Partial | Regression Tested | Partial Parity | Non-deleted stigma charge source updates, success target updates, and failed nonzero target updates now consume cleanup static-data context for full update blobs. Java delayed task scheduling, observer abort behavior, RNG, persistence-state marking, DAO timing, and runtime byte comparison remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM` default / `DEC_STIGMA_USE` branches | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem` via stigma charge completion | Packet | Partial | Regression Tested | Partial Parity | Java source confirms default `DEC_ITEM_USE` and `DEC_STIGMA_USE` use `ItemInfoBlob.getFullBlob`; focused C# test asserts cleanup/seal flag `3` reaches source and target full blobs. |
| `com.aionemu.gameserver.services.item.ItemPacketService.ItemUpdateType.EQUIP_UNEQUIP` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem.EquipUnequip` | Packet Partial Mode | Partial | Manual Only | Partial Parity | Java stigma equip/remove paths use partial `EQUIP_UNEQUIP`, so this unit intentionally did not wire cleanup/seal metadata there. Broader equip/unequip packet-shape parity remains separate. |
| `com.aionemu.gameserver.dataholders.ItemRestrictionCleanupData` | `Aion.GameServer.Dataholders.ItemRestrictionCleanupTable` consumed by stigma charge connection path | Dataholder Context | Partial | Regression Tested | Partial Parity | This unit consumes the existing cleanup table through `StaticData.ItemRestrictionCleanups`; loader behavior was not changed. |
| `com.aionemu.gameserver.network.aion.iteminfo.GeneralInfoBlobEntry` cleanup/seal flag | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteGeneralInfoBlob` via stigma charge full updates | Serialization Entry | Partial | Regression Tested | Partial Parity | Stigma charge full update packets can now feed the Java-shaped field. Temporary-exchange and time-dependent fields remain unresolved. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GameServerConnectionStigmaChargeTests.CompleteStigmaChargeAsync_WritesCleanupSealFlagForRestrictedFullUpdates` | Regression / connection packet serialization | `StigmaService.chargeStigma`, `ItemPacketService.sendItemUpdatePacket`, `SM_INVENTORY_UPDATE_ITEM`, `GeneralInfoBlobEntry` | Restricted stigma source `DEC_STIGMA_USE` update and success target `DEC_ITEM_USE` update both emit full blobs with cleanup/seal flag `3`. | C# packet parsing against reviewed Java full-blob packet shape and supplied static-data predicate. | Does not execute Java delayed task or compare Java runtime bytes. |
| `StigmaServiceTests.CreateChargePlan_SuccessConsumesStoneAndRaisesEquippedStigmaSkillLevel` | Regression / service plan | `StigmaService.chargeStigma` success branch | Existing service-plan behavior remains stable after connection packet wiring. | Source-derived C# assertions. | Does not inspect packets. |
| `GamePacketTests.SmInventoryUpdateItem_WritesCleanupSealFlagInItemBlobLikeJava` | Regression / packet serialization | `SM_INVENTORY_UPDATE_ITEM`, `GeneralInfoBlobEntry` | Existing full update packet cleanup/seal wrapper coverage still passes. | Deterministic C# packet parsing. | No Java runtime bytes. |

## Remaining Risks

- Remaining `GameServerConnection` full-blob update callers still need cleanup/seal flag wiring: enchant/socket/remodel/dye/polish-success and item-use/decrease helper paths.
- Stigma equip/remove remain partial `EQUIP_UNEQUIP` paths and need separate packet-shape parity attention, not cleanup/seal flag wiring.
- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Temporary-exchange remaining seconds, runtime conditioning presence, and time-normalized expiration/dye comparison remain unresolved item-blob risks.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 C# connection stigma charge packet caller changed
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: remaining `GameServerConnection` full-blob update caller flag sources, Java runtime artifact generation, temporary-exchange model/template/drop/task fields, time-normalized expiration/dye comparison
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Work Options

## Recommended Sequential Task

- Task: next confirmed full-blob `GameServerConnection` item-info-change cleanup-seal wiring.
- Scope:
  - inspect Java enchant/socket/remodel/dye/polish-success item update calls;
  - confirm which branches use full `SM_INVENTORY_UPDATE_ITEM` rather than partial/delete-only packets;
  - wire only confirmed full update packets with `GameServerConnection.GetGeneralInfoWarehouseRestrictionFlag`;
  - add focused connection-level packet tests for restricted updated items.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Java enchant/socket/remodel/dye packet-shape audit | read-only Java services/client packets | Low | Best first parallel task before editing more connection calls. |
| B | C# connection test-surface audit for item-info-change paths | read-only `GameServerConnection.cs`, existing tests | Low | Identify smallest test seam for next caller. |
| C | Equip/unequip packet-shape parity audit | `SmInventoryUpdateItem.cs` read-only plus Java packet source | Medium | Separate from cleanup-seal metadata. |

## Do Not Parallelize

- Do not edit `GameServerConnection.cs` from multiple workers at once.
- Do not edit shared progress/handoff/parity docs from sub-agents; orchestrator owns docs.
- Do not wire cleanup/seal metadata into `EQUIP_UNEQUIP`, `CHARGE`, or `POLISH_CHARGE` unless Java source proves a full blob is written.
- Do not claim verified parity until Java runtime packet evidence or deterministic generated artifact comparison exists.

## Context For Next Session

- Current phase: Phase 6, cleanup/seal full item-blob flag propagation across live or staged inventory-producing callers.
- Last completed UOW: UOW-1418.
- Last commit planned: `[Phase 6][UOW-1418] Wire stigma charge cleanup seal metadata`.
- Key changed files in this handoff:
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionStigmaChargeTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageCleanupSealStaticDataAudit.md`
  - `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageCSharpBlobGapAudit.md`
  - `docs/Phase-6AIT-Completion.md`
