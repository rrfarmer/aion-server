# Phase 6 AIS Completion - Observer Burn Cleanup-Seal Runtime Propagation

Date: 2026-05-27
Unit of Work: UOW-1417
Status: equipment observer burn runtime flow now carries cleanup-seal static-data context into idian exhausted full update packets.

## Completed

- Updated `EquipmentObserverBurnWorkflowService.ApplyObserverBurnsAsync` to accept optional `ItemRestrictionCleanupTable`.
- Updated `EquipmentObserverBurnFanoutService.ApplyObserverBurnsAndSendPacketsAsync` to pass cleanup context into the workflow before owner packet fanout.
- Updated `PlayerIncomingDamageObserverFanoutService` to carry optional cleanup context through attacked and dot-attacked observer routes.
- Updated `WorldNpcSkillDamageService` to accept a lazy cleanup-table accessor and pass runtime cleanup data into observer burns.
- Updated `Program.cs` so production skill-damage observer burns can resolve `StaticData.ItemRestrictionCleanups`.
- Added workflow-level packet coverage proving an observer-triggered exhausted idian full update carries cleanup/seal flag `3`.
- Updated progress, cleanup/seal audit, and C# blob gap audit.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~EquipmentObserverBurnWorkflowServiceTests|FullyQualifiedName~WorldNpcDamageServiceTests.ApplyDamageEffectAsync_NotifiesAttackObserversAndAppliesEquipmentBurns|FullyQualifiedName~IdianPolishBurnApplicationServiceTests.ApplyBurnPlan_UpdatesInventoryAndCreatesLowAndExhaustedPackets"`.
- Result: passed 7 tests.

## Migration Parity Table - UOW-1417

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.items.IdianStone.decreasePolishCharge` observer exhaustion path | `Aion.GameServer.Services.EquipmentObserverBurnWorkflowService.ApplyObserverBurnsAsync` | Workflow / Packet Caller | Partial | Regression Tested | Partial Parity | Runtime observer workflow can now pass cleanup static-data context into the idian exhausted full update packet. Java observer registration, synchronization, stat/effect refresh ordering, DAO timing, and runtime Java byte comparison remain unverified. |
| `com.aionemu.gameserver.model.items.IdianStone.onEquip` attack/defend observers | `Aion.GameServer.Services.EquipmentObserverBurnFanoutService.ApplyObserverBurnsAndSendPacketsAsync` | Fanout Service | Partial | Regression Tested | Partial Parity | Fanout API carries optional cleanup context into the shared workflow before sending packets to the owner. Live socket ordering beyond captured workflow packet sequence remains unverified. |
| `com.aionemu.gameserver.skillengine.effect.DamageEffect.applyEffect` / attacked observer callback path | `Aion.GameServer.Services.WorldNpcSkillDamageService.ApplyDamageEffectAsync` | Combat Service | Partial | Regression Tested | Partial Parity | Production skill-damage service now resolves `StaticData.ItemRestrictionCleanups` lazily alongside item templates. Broader skill engine callback coverage and Java runtime comparison remain incomplete. |
| `com.aionemu.gameserver.dataholders.StaticData.itemCleanup` / `DataManager.ITEM_CLEAN_UP` | `Program.cs` runtime `StaticData.ItemRestrictionCleanups` accessor | Static Data Runtime Wiring | Partial | Manual + Regression Tested indirectly | Partial Parity | Production DI can now supply the cleanup table to skill-damage observer burns. Missing static data still falls back to flag `0`; no loader changes in this unit. |
| `com.aionemu.gameserver.network.aion.iteminfo.GeneralInfoBlobEntry` cleanup/seal flag | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteGeneralInfoBlob` via observer workflow | Serialization Entry | Partial | Regression Tested | Partial Parity | Observer idian exhausted full update packets can now feed the Java-shaped field. Temporary-exchange and time-dependent fields remain unresolved. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `EquipmentObserverBurnWorkflowServiceTests.ApplyObserverBurnsAsync_PassesCleanupSealContextToExhaustedIdianFullUpdate` | Regression / workflow packet serialization | `IdianStone.onEquip`, `IdianStone.decreasePolishCharge`, `SM_INVENTORY_UPDATE_ITEM`, `GeneralInfoBlobEntry` | Observer burn workflow with an exhausted idian sends a full `DecreaseItemUse` update packet whose `GENERAL_INFO` cleanup/seal flag is `3`. | C# packet parsing against deterministic Java source field order and supplied static-data predicate. | No Java runtime bytes; live socket order remains broader. |
| `EquipmentObserverBurnWorkflowServiceTests` suite | Regression / workflow and fanout | `IdianStone` and `ChargeInfo` observer ordering | Existing workflow/fanout behavior still passes after optional cleanup context was added. | Source-derived C# behavior. | Does not inspect every packet byte. |
| `WorldNpcDamageServiceTests.ApplyDamageEffectAsync_NotifiesAttackObserversAndAppliesEquipmentBurns` | Regression / skill-damage observer route | `DamageEffect.applyEffect` attack observer callback | Existing skill-damage observer route still applies equipment burns after runtime accessor plumbing. | Source-derived C# behavior. | Does not cover cleanup flag in this specific test. |
| `IdianPolishBurnApplicationServiceTests.ApplyBurnPlan_UpdatesInventoryAndCreatesLowAndExhaustedPackets` | Regression / packet serialization | `IdianStone.decreasePolishCharge` | Application-level cleanup/seal full update regression still passes. | Deterministic C# packet parsing. | No Java runtime bytes. |

## Remaining Risks

- Remaining `GameServerConnection` full-blob update callers still need cleanup/seal flag wiring.
- `EQUIP_UNEQUIP`, `CHARGE`, and `POLISH_CHARGE` packet modes need separate packet-shape parity attention rather than cleanup/seal flag wiring.
- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Temporary-exchange remaining seconds, runtime conditioning presence, and time-normalized expiration/dye comparison remain unresolved item-blob risks.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 4 C# observer/runtime plumbing surfaces changed
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: remaining `GameServerConnection` full-blob update caller flag sources, Java runtime artifact generation, temporary-exchange model/template/drop/task fields, time-normalized expiration/dye comparison
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Work Options

## Recommended Sequential Task

- Task: next `GameServerConnection` default full update caller cleanup-seal wiring.
- Scope:
  - inspect Java stigma update packet calls and the matching C# `GameServerConnection` stigma paths;
  - confirm they use default full `SM_INVENTORY_UPDATE_ITEM` rather than a partial packet mode;
  - pass `GameServerConnection.GetGeneralInfoWarehouseRestrictionFlag(itemId, staticData.ItemRestrictionCleanups)` into `SmInventoryUpdateItem`;
  - add focused connection-level packet tests for restricted updated stigma items.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Stigma update cleanup-seal implementation | `GameServerConnection.cs`, stigma connection tests | Medium | Likely next best full-update caller if Java packet shape is confirmed. |
| B | Enchant/socket/remodel/dye update caller audit | read-only `GameServerConnection.cs`, Java item services | Medium | Useful if stigma path is broader than expected. |
| C | Equip/unequip packet-shape parity audit | `SmInventoryUpdateItem.cs` read-only plus Java packet source | Medium | Separate from cleanup-seal flags because Java writes partial equipped-slot blob. |

## Do Not Parallelize

- Do not edit `GameServerConnection.cs` from multiple workers at once.
- Do not edit shared progress/handoff/parity docs from sub-agents; orchestrator owns docs.
- Do not claim verified parity until Java runtime packet evidence or deterministic generated artifact comparison exists.

## Context For Next Session

- Current phase: Phase 6, cleanup/seal full item-blob flag propagation across live or staged inventory-producing callers.
- Last completed UOW: UOW-1417.
- Last commit planned: `[Phase 6][UOW-1417] Propagate observer cleanup seal metadata`.
- Key changed files in this handoff:
  - `dotnetConversion/src/Aion.GameServer/Program.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/EquipmentObserverBurnFanoutService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/EquipmentObserverBurnWorkflowService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerIncomingDamageObserverFanoutService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/WorldNpcSkillDamageService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/EquipmentObserverBurnWorkflowServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageCleanupSealStaticDataAudit.md`
  - `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageCSharpBlobGapAudit.md`
  - `docs/Phase-6AIS-Completion.md`
