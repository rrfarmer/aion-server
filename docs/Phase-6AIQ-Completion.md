# Phase 6 AIQ Completion - Portal Required-Item Cleanup-Seal Metadata

Date: 2026-05-27
Unit of Work: UOW-1415
Status: portal required-item update packet planning now consumes cleanup-seal static-data context.

## Completed

- Performed parallel read-only discovery over remaining `SmInventoryUpdateItem` default callers:
  - `GameServerConnection.cs` caller classification.
  - Standalone service caller classification.
- Updated `PortalEntryValidationService.CreateRequiredItemsAndKinahApplication` to accept optional `ItemRestrictionCleanupTable`.
- Passed the Java-shaped cleanup/seal flag into required-item and kinah `SmInventoryUpdateItem` packets.
- Expanded the focused portal required-item application test to parse full update blobs and assert restricted item flag `3` plus kinah flag `0`.
- Updated progress, cleanup/seal audit, and C# blob gap audit.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~PortalEntryValidationServiceTests.CreateRequiredItemsAndKinahApplication_AppliesJavaDeleteUpdateAndKinahPackets|FullyQualifiedName~PortalEntryValidationServiceTests.CreateRequiredItemsAndKinahApplication_ReturnsNoPacketsWhenUpdateTemplateIsMissing"`.
- Result: passed 2 tests.

## Migration Parity Table - UOW-1415

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.teleport.PortalService.checkAndRemoveRequiredItems` | `Aion.GameServer.Services.PortalEntryValidationService.CreateRequiredItemsAndKinahApplication` | Service / Packet Plan | Partial | Regression Tested | Partial Parity | Required-item and kinah update packet planning now accepts cleanup static-data context and passes the Java-shaped flag into `SmInventoryUpdateItem`. Portal transfer, live instance registration, and runtime Java packet comparison remain outside this unit. |
| `com.aionemu.gameserver.model.items.storage.Storage.decreaseByItemId` | `Aion.GameServer.Services.PortalRequirementConsumptionPlan` / `PortalRequirementConsumptionApplication` | Service / Storage Mutation Plan | Partial | Regression Tested | Partial Parity | C# still stages consumption before applying packets, but packet order and updated/deleted item fanout are source-derived. Null/static-data absence keeps flag `0`. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemUpdatePacket` cube branch | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem` via portal required-item application | Packet | Partial | Regression Tested | Partial Parity | Focused test asserts restricted required-item update blob carries flag `3`; kinah update remains `0`. Java runtime byte comparison remains unavailable. |
| `com.aionemu.gameserver.dataholders.ItemRestrictionCleanupData` | `Aion.GameServer.Dataholders.ItemRestrictionCleanupTable` consumed by portal application | Dataholder Context | Partial | Regression Tested | Partial Parity | This unit consumes the previously ported cleanup table; XML parsing behavior was not changed. |
| `com.aionemu.gameserver.network.aion.iteminfo.GeneralInfoBlobEntry` cleanup/seal flag | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteGeneralInfoBlob` | Serialization Entry | Partial | Regression Tested | Partial Parity | Portal required-item update packets can now feed the Java-shaped field through full inventory update blobs. Temporary-exchange and time-dependent fields remain unresolved. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `PortalEntryValidationServiceTests.CreateRequiredItemsAndKinahApplication_AppliesJavaDeleteUpdateAndKinahPackets` | Regression / packet-plan serialization | `PortalService.checkAndRemoveRequiredItems`, `Storage.decreaseByItemId`, `ItemPacketService.sendItemUpdatePacket`, `SM_INVENTORY_UPDATE_ITEM`, `GeneralInfoBlobEntry` | Required item stack update emits a full update blob with cleanup/seal flag `3`; kinah update keeps flag `0`; delete/cube packet order remains intact. | C# packet parsing against deterministic Java source field order and supplied static-data predicate. | Does not compare Java runtime bytes; portal live transfer remains separate. |
| `PortalEntryValidationServiceTests.CreateRequiredItemsAndKinahApplication_ReturnsNoPacketsWhenUpdateTemplateIsMissing` | Regression / failure guard | Portal staged consumption prerequisites | Existing missing-template guard still produces no packets after optional cleanup context was added. | Source-derived C# behavior. | Does not inspect cleanup/seal flag. |

## Remaining Risks

- Many `GameServerConnection` full-blob update callers still need cleanup/seal flag wiring: stigma, enchant/socket/remodel/dye/polish-success, and item-use/decrease helper paths.
- `IdianPolishBurnApplicationService` exhausted branch remains the smallest standalone service cleanup/seal candidate.
- `EQUIP_UNEQUIP`, `CHARGE`, and `POLISH_CHARGE` packet modes need separate packet-shape parity attention rather than cleanup/seal flag wiring.
- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Temporary-exchange remaining seconds, runtime conditioning presence, and time-normalized expiration/dye comparison remain unresolved item-blob risks.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 C# portal required-item update packet-plan consumer changed
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: remaining `GameServerConnection` full-blob update caller flag sources, `IdianPolishBurnApplicationService` exhausted branch, Java runtime artifact generation, temporary-exchange model/template/drop/task fields, time-normalized expiration/dye comparison
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Work Options

## Recommended Sequential Task

- Task: idian polish exhausted update cleanup-seal metadata.
- Scope:
  - update `IdianPolishBurnApplicationService.ApplyBurnPlan` to accept optional `ItemRestrictionCleanupTable`;
  - pass cleanup/seal flag only for full update packets, with `POLISH_CHARGE` low-charge behavior unchanged;
  - add focused service packet tests for restricted exhausted item flag `3` and low-charge partial packet stability.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Idian exhausted update implementation | `IdianPolishBurnApplicationService.cs`, its focused tests | Low | Best next sequential implementation. |
| B | `GameServerConnection` stigma update cleanup-seal design | read-only `GameServerConnection.cs`, Java `StigmaService` | Medium | Broad live-handler edits; keep as audit unless selected exclusively. |
| C | Equip/unequip packet-shape parity audit | `SmInventoryUpdateItem.cs` read-only plus Java packet source | Medium | Separate from cleanup-seal flags because Java writes partial equipped-slot blob. |

## Do Not Parallelize

- Do not edit the same packet planner and its tests from multiple workers at once.
- Do not edit shared progress/handoff/parity docs from sub-agents; orchestrator owns docs.
- Do not claim verified parity until there is Java runtime packet evidence or deterministic generated artifact comparison.

## Context For Next Session

- Current phase: Phase 6, cleanup/seal full item-blob flag propagation across live or staged inventory-producing callers.
- Last completed UOW: UOW-1415.
- Last commit planned: `[Phase 6][UOW-1415] Wire portal update cleanup seal metadata`.
- Key changed files in this handoff:
  - `dotnetConversion/src/Aion.GameServer/Services/PortalEntryValidationService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PortalEntryValidationServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageCleanupSealStaticDataAudit.md`
  - `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageCSharpBlobGapAudit.md`
  - `docs/Phase-6AIQ-Completion.md`
