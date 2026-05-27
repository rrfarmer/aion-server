# Phase 6 AIR Completion - Idian Exhausted Cleanup-Seal Metadata

Date: 2026-05-27
Unit of Work: UOW-1416
Status: idian polish exhausted full update packet planning now consumes cleanup-seal static-data context.

## Completed

- Updated `IdianPolishBurnApplicationService.ApplyBurnPlan` to accept optional `ItemRestrictionCleanupTable`.
- Passed cleanup/seal flag `3` into `SmInventoryUpdateItem` only for `IdianPolishBurnUpdateKind.Exhausted`.
- Preserved Java low-charge threshold behavior by leaving `POLISH_CHARGE` packets as compact polish-charge blobs with no cleanup/seal flag.
- Expanded the focused idian application test to parse the exhausted full update item blob and assert `GENERAL_INFO` cleanup/seal flag `3`.
- Updated progress, cleanup/seal audit, and C# blob gap audit.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~IdianPolishBurnApplicationServiceTests.ApplyBurnPlan_UpdatesInventoryAndCreatesLowAndExhaustedPackets|FullyQualifiedName~IdianPolishBurnApplicationServiceTests.ApplyBurnPlan_UpdatesInventoryButSkipsPacketWhenTemplateMissing|FullyQualifiedName~IdianPolishBurnApplicationServiceTests.ApplyBurnPlan_NoOpsWhenPlanHasNoChanges|FullyQualifiedName~GamePacketTests.SmInventoryUpdateItem_WritesCleanupSealFlagInItemBlobLikeJava"`.
- Result: passed 4 tests.

## Migration Parity Table - UOW-1416

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.items.IdianStone.decreasePolishCharge` | `Aion.GameServer.Services.IdianPolishBurnApplicationService.ApplyBurnPlan` | Model Helper / Packet Caller Helper | Partial | Regression Tested | Partial Parity | Exhausted branch now consumes optional cleanup static-data context and passes flag `3` into the full update packet when the exhausted idian item is restricted. Java mutation ordering, synchronization, DAO flush timing, and live observer fanout remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM` default branch | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem` via idian exhausted branch | Packet | Partial | Regression Tested | Partial Parity | Focused test asserts the exhausted full decrease packet carries cleanup/seal flag `3`. Java runtime byte comparison remains unavailable. |
| `com.aionemu.gameserver.services.item.ItemPacketService.ItemUpdateType.POLISH_CHARGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem.PolishCharge` | Packet Update Type / Enum Equivalent | Partial | Regression Tested | Partial Parity | Low-charge threshold updates remain compact polish-charge blobs and do not receive cleanup/seal metadata. |
| `com.aionemu.gameserver.dataholders.ItemRestrictionCleanupData` | `Aion.GameServer.Dataholders.ItemRestrictionCleanupTable` consumed by idian application | Dataholder Context | Partial | Regression Tested | Partial Parity | Consumes the existing cleanup table; static-data XML parsing was not changed. |
| `com.aionemu.gameserver.network.aion.iteminfo.GeneralInfoBlobEntry` cleanup/seal flag | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteGeneralInfoBlob` | Serialization Entry | Partial | Regression Tested | Partial Parity | Idian exhausted full update packets can now feed the Java-shaped field. Temporary-exchange and time-dependent fields remain unresolved. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `IdianPolishBurnApplicationServiceTests.ApplyBurnPlan_UpdatesInventoryAndCreatesLowAndExhaustedPackets` | Regression / packet serialization | `IdianStone.decreasePolishCharge`, `SM_INVENTORY_UPDATE_ITEM`, `GeneralInfoBlobEntry` | Restricted exhausted item update carries cleanup/seal flag `3`; low-charge packet remains compact `POLISH_CHARGE`. | C# packet parsing against deterministic Java source field order and static-data predicate. | No Java runtime bytes; live observer cleanup-table propagation remains separate. |
| `IdianPolishBurnApplicationServiceTests.ApplyBurnPlan_UpdatesInventoryButSkipsPacketWhenTemplateMissing` | Regression / failure guard | Template-gated packet construction boundary | Missing templates suppress packet creation while preserving in-memory mutation. | Source-derived C# behavior. | Does not inspect cleanup/seal flag. |
| `IdianPolishBurnApplicationServiceTests.ApplyBurnPlan_NoOpsWhenPlanHasNoChanges` | Regression / no-op guard | Empty burn-plan boundary | Empty plans still avoid inventory and packet mutation. | Source-derived C# behavior. | Does not inspect packet serialization. |
| `GamePacketTests.SmInventoryUpdateItem_WritesCleanupSealFlagInItemBlobLikeJava` | Regression / packet serialization | `SM_INVENTORY_UPDATE_ITEM`, `GeneralInfoBlobEntry` | Existing wrapper-level full update cleanup/seal packet coverage still passes. | Deterministic C# packet parsing. | No Java runtime bytes. |

## Remaining Risks

- Live equipment observer workflow/fanout still needs cleanup-table propagation before runtime skill/combat idian exhausted packets carry the flag.
- Remaining `GameServerConnection` full-blob update callers still need cleanup/seal flag wiring.
- `EQUIP_UNEQUIP`, `CHARGE`, and `POLISH_CHARGE` packet modes need packet-shape parity attention rather than cleanup/seal flag wiring.
- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Temporary-exchange remaining seconds, runtime conditioning presence, and time-normalized expiration/dye comparison remain unresolved item-blob risks.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 C# idian exhausted update packet caller changed
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: live observer cleanup-table propagation, remaining `GameServerConnection` full-blob update caller flag sources, Java runtime artifact generation, temporary-exchange model/template/drop/task fields, time-normalized expiration/dye comparison
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Work Options

## Recommended Sequential Task

- Task: cleanup-table propagation through equipment observer burn runtime flow.
- Scope:
  - inspect where `StaticData.ItemRestrictionCleanups` is available for `WorldNpcSkillDamageService`, `EquipmentObserverBurnFanoutService`, and `EquipmentObserverBurnWorkflowService`;
  - thread optional `ItemRestrictionCleanupTable` only where static-data context is already available;
  - add a focused workflow or skill-damage test proving exhausted idian runtime packets carry flag `3`.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Equipment observer cleanup-table propagation audit | `WorldNpcSkillDamageService.cs`, `EquipmentObserverBurnWorkflowService.cs`, fanout services, related tests | Medium | Keep read-only until the static-data source is clear. |
| B | `GameServerConnection` stigma update cleanup-seal design | read-only `GameServerConnection.cs`, Java stigma source | Medium | Good alternative if observer propagation is broad. |
| C | Equip/unequip packet-shape parity audit | `SmInventoryUpdateItem.cs` read-only plus Java packet source | Medium | Separate from cleanup-seal flags because Java writes partial equipped-slot blob. |

## Do Not Parallelize

- Do not edit the same workflow/fanout files and their tests from multiple workers at once.
- Do not edit shared progress/handoff/parity docs from sub-agents; orchestrator owns docs.
- Do not claim verified parity until Java runtime packet evidence or deterministic generated artifact comparison exists.

## Context For Next Session

- Current phase: Phase 6, cleanup/seal full item-blob flag propagation across live or staged inventory-producing callers.
- Last completed UOW: UOW-1416.
- Last commit planned: `[Phase 6][UOW-1416] Wire idian exhausted cleanup seal metadata`.
- Key changed files in this handoff:
  - `dotnetConversion/src/Aion.GameServer/Services/IdianPolishBurnApplicationService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/IdianPolishBurnApplicationServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageCleanupSealStaticDataAudit.md`
  - `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageCSharpBlobGapAudit.md`
  - `docs/Phase-6AIR-Completion.md`
