# Phase 6 Session 1792 Completion - Preserve Observer Charge Burn Equipment Dirtiness On Save Failure

Date: 2026-05-30
Unit of Work: UOW-1792
Status: Complete

## Scope

Port the minimum Java-shaped dirty-state fallback for observer-driven equipment charge burn so the modeled equipment container remains dirty when the in-memory charge mutation applies but the immediate persistence boundary fails.

## Completed Work

- Updated `dotnetConversion/src/Aion.GameServer/Services/EquipmentObserverBurnWorkflowService.cs`:
  - observer-driven charge burn now calls `player.MarkEquipmentDirty()` when the charge-burn plan changed and the immediate charge persistence boundary returned failure
- Updated focused parity evidence in:
  - `dotnetConversion/tests/Aion.GameServer.Tests/EquipmentObserverBurnWorkflowServiceTests.cs`
    - added `ApplyObserverBurnsAsync_ChargePersistenceFailureMarksEquipmentDirty`
    - added `ApplyObserverBurnsAsync_ChargePersistenceSuccessLeavesEquipmentStateClean`

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~EquipmentObserverBurnWorkflowServiceTests|FullyQualifiedName~ItemChargeBurnApplicationServiceTests|FullyQualifiedName~PlayerInventoryPersistentStateTests|FullyQualifiedName~PlayerEnterWorldServiceTests"`
- `dotnet test dotnetConversion\AionServer.slnx`

Result:

- Focused observer-burn validation passed with 50 tests.
- Full-suite validation passed cleanly on the first run with 4772 total tests:
  - `57` commons
  - `29` chat
  - `121` login
  - `4565` game

## Java Artifacts Reviewed

- `com.aionemu.gameserver.model.items.ChargeInfo`
- `com.aionemu.gameserver.model.gameobjects.player.Equipment`

## Migration Parity Table - UOW-1792

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.items.ChargeInfo.updateChargePoints` equipment-state side effect | `Aion.GameServer.Services.EquipmentObserverBurnWorkflowService.ApplyObserverBurnsAsync` + `Player.MarkEquipmentDirty` | Observer Charge Burn Dirty-State Fallback | Partial | Unit Tested | Partial Parity | C# now preserves modeled equipment dirtiness when observer-driven charge burn applies in memory but the immediate persistence boundary fails. |
| `com.aionemu.gameserver.model.items.ChargeInfo.attack` / `attacked` / `dotattacked` | `Aion.GameServer.Services.ItemChargeBurnApplicationService` + `EquipmentObserverBurnWorkflowService` | Observer Burn Runtime Flow | Partial | Unit Tested | Partial Parity | Failed persistence no longer leaves the player modeled as equipment-clean after an observer-driven charge change. |
| `com.aionemu.gameserver.model.gameobjects.player.Equipment.setPersistentState` from charge observer flow | `Aion.GameServer.Model.GameObjects.Player.EquipmentPersistentState` via observer workflow | Modeled Equipment Dirty Producer | Partial | Unit Tested | Partial Parity | This unit only covers the observer-driven charge-burn failure branch and does not sweep other equipment mutation producers. |

## Tests Added

| Test Name | What It Validates | Java-Equivalent Evidence | Test Type | Limitations |
|---|---|---|---|---|
| `ApplyObserverBurnsAsync_ChargePersistenceFailureMarksEquipmentDirty` | When observer-driven charge burn applies and the immediate save fails, the player remains modeled as equipment-dirty. | Java `ChargeInfo.updateChargePoints` source | Unit | No first-class `Equipment` object proof |
| `ApplyObserverBurnsAsync_ChargePersistenceSuccessLeavesEquipmentStateClean` | Successful observer-burn persistence does not leave a false positive modeled equipment dirty flag in the current immediate-save C# path. | Java `ChargeInfo.updateChargePoints` source plus current C# persistence boundary review | Unit | C# still persists immediately rather than relying solely on later DAO harvest |

## Risks / Gaps

- The current observer-burn path still treats a missing `saveItemChargeBurnAsync` delegate as persisted rather than explicitly dirty; that semantics was intentionally left unchanged in this unit.
- Other Java equipment dirty producers such as charge-item service success paths, enchant, tampering, and dye remain separate slices.
- C# still does not port a first-class Java `Equipment` object; modeled equipment dirtiness still lives on `Player`.

## Summary Metrics

- Total Java artifacts discovered: 3 grouped rows in this unit.
- Total artifacts ported: 1 observer-burn dirty-state fallback branch and 2 focused workflow tests.
- Total artifacts with verified parity: 0 grouped rows.
- Total artifacts needing verification: 3 grouped rows.
- Total blocked artifacts: broader live equipment dirty producers, first-class storage/equipment container modeling, and unresolved `saveItemChargeBurnAsync == null` observer semantics.
- Estimated overall migration completion: Phase 6 remains about 72%.

## Next Recommended Unit of Work

- Inspect the next smallest Java equipment dirty producer outside the already-ported direct equip/shard and observer-burn failure paths, with `ChargeInfo` immediate-success semantics or `TamperingAction` as the most natural adjacent candidates.
- Safe alternatives if a different isolated slice is preferred:
  - execute the opt-in MySQL logout delete/retuning persistence path in an environment with `AION_GAMESERVER_DB_INTEGRATION=1`
  - `CraftService.finishCrafting` product selection
  - `DropRegistrationService.calculateBoostDropRate`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/EquipmentObserverBurnWorkflowService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/EquipmentObserverBurnWorkflowServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1792-Completion.md`
- `docs/Phase-6-Session-1792-Handoff.md`
