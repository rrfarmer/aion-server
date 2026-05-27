# Phase 6 AIP Completion - ItemPurification Update Cleanup-Seal Metadata

Date: 2026-05-27
Unit of Work: UOW-1414
Status: ItemPurification inventory update packet planning now consumes snapshot cleanup-seal metadata.

## Completed

- Updated `ItemPurificationPacketPlanService.CreateInventoryUpdatePacket` to pass `ItemPurificationInventoryPacketInput.GeneralInfoWarehouseRestrictionFlag` into `SmInventoryUpdateItem`.
- Expanded the concrete inventory-update packet-plan test to parse the full item blob and assert nested `GENERAL_INFO` cleanup/seal flag `3`.
- Re-ran the target-add and snapshot flag-source guards to ensure UOW-1412/UOW-1413 behavior still holds.
- Updated progress, cleanup/seal audit, and C# blob gap audit.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~ItemPurificationPacketPlanServiceTests.CreatePacketPlan_AttachesConcreteInventoryUpdatePacketWhenRuntimeItemInputProvided|FullyQualifiedName~ItemPurificationPacketPlanServiceTests.CreatePacketPlan_AttachesConcreteTargetAddPacketWhenRuntimeItemInputProvided|FullyQualifiedName~ItemPurificationPacketInputSnapshotServiceTests.CreateInputs_ComputesCleanupSealFlagFromRestrictionTable"`.
- Result: passed 3 tests.

## Migration Parity Table - UOW-1414

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemPurificationService.decreaseMaterials` | `Aion.GameServer.Services.ItemPurificationPacketPlanService.CreateInventoryUpdatePacket` | Service / Packet Plan | Partial | Regression Tested | Partial Parity | Material/base count-decrease update packets now consume the caller-provided cleanup/seal flag when building `SmInventoryUpdateItem`. Broader live mutation and Java runtime comparison remain incomplete. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemUpdatePacket` cube branch | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem` via ItemPurification packet plan | Packet | Partial | Regression Tested | Partial Parity | Focused test asserts supplied flag `3` reaches the full update-item blob. Charge/polish partial update paths remain intentionally out of scope because they do not write `GENERAL_INFO`. |
| `com.aionemu.gameserver.dataholders.ItemRestrictionCleanupData` | `Aion.GameServer.Services.ItemPurificationInventoryPacketInput.GeneralInfoWarehouseRestrictionFlag` | Dataholder Context / DTO | Partial | Unit Tested | Partial Parity | UOW-1413 computes this flag from static-data context; this unit wires the update packet consumer. |
| `com.aionemu.gameserver.network.aion.iteminfo.GeneralInfoBlobEntry` cleanup/seal flag | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteGeneralInfoBlob` | Serialization Entry | Partial | Regression Tested | Partial Parity | ItemPurification add and update packet plans can now feed the Java-shaped field through full inventory blobs. Temporary-exchange and time-dependent fields remain unresolved. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ItemPurificationPacketPlanServiceTests.CreatePacketPlan_AttachesConcreteInventoryUpdatePacketWhenRuntimeItemInputProvided` | Regression / packet-plan serialization | `ItemPacketService.sendItemUpdatePacket`, `SM_INVENTORY_UPDATE_ITEM`, `GeneralInfoBlobEntry` | Supplied update packet input with cleanup/seal flag `3` produces a full update blob carrying flag `3`. | C# packet parsing against deterministic Java source field order and supplied flag value. | Does not compare Java runtime bytes. |
| `ItemPurificationPacketPlanServiceTests.CreatePacketPlan_AttachesConcreteTargetAddPacketWhenRuntimeItemInputProvided` | Regression / packet-plan serialization | `SM_INVENTORY_ADD_ITEM`, `GeneralInfoBlobEntry` | Existing add-packet guard still passes after update-path wiring. | Deterministic C# packet parsing. | Does not compare Java runtime bytes. |
| `ItemPurificationPacketInputSnapshotServiceTests.CreateInputs_ComputesCleanupSealFlagFromRestrictionTable` | Unit / snapshot input assembly | `ItemRestrictionCleanupData` | Existing snapshot flag source still passes after update-path consumer wiring. | C# assertion against deterministic Java cleanup predicate. | Does not compare Java runtime bytes. |

## Remaining Risks

- Remaining default full-blob inventory update callers outside the already-wired reward/world-loot/ItemPurification paths still need classification.
- ItemPurification automatic CM handler dispatch remains plan-only unless explicit live/persistent opt-in seams are invoked.
- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Temporary-exchange remaining seconds, runtime conditioning presence, and time-normalized expiration/dye comparison remain unresolved item-blob risks.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped artifact rows in this unit
- Total artifacts ported: 1 C# ItemPurification update packet-plan consumer changed
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: remaining inventory update caller flag sources, automatic ItemPurification handler dispatch, Java runtime artifact generation, temporary-exchange model/template/drop/task fields, time-normalized expiration/dye comparison
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Work Options

## Recommended Sequential Task

- Task: remaining full-blob `SmInventoryUpdateItem` caller classification and smallest implementation.
- Scope:
  - classify remaining `new SmInventoryUpdateItem(...)` defaults as kinah-only, charge/polish partial, equip/unequip, already wired, or true full-blob item update needing cleanup context;
  - implement one true full-blob item update caller with deterministic static-data context and focused packet tests.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Remaining inventory update caller classification | read-only `GameServerConnection.cs`, service planners, tests | Low | Best next unit; many defaults are not eligible. |
| B | Portal/experience/storage service update-path audit | read-only service files | Low | Could identify one narrow static-data context candidate. |
| C | Quest item reward executor design audit | read-only quest reward projection/planning services and Java `QuestService` | Medium | Broad and should stay separate from tiny packet caller fixes. |

## Do Not Parallelize

- Do not edit the same packet planner and its tests from multiple workers at once.
- Do not edit shared progress/handoff/parity docs from sub-agents; orchestrator owns docs.
- Do not claim verified parity until there is Java runtime packet evidence or deterministic generated artifact comparison.

## Context For Next Session

- Current phase: Phase 6, cleanup/seal full item-blob flag propagation across live or staged inventory-producing callers.
- Last completed UOW: UOW-1414.
- Last commit planned: `[Phase 6][UOW-1414] Wire purification update cleanup seal metadata`.
- Key changed files in this handoff:
  - `dotnetConversion/src/Aion.GameServer/Services/ItemPurificationPacketPlanService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/ItemPurificationPacketPlanServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageCleanupSealStaticDataAudit.md`
  - `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageCSharpBlobGapAudit.md`
  - `docs/Phase-6AIP-Completion.md`
