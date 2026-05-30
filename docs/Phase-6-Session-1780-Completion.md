# Phase 6 Session 1780 Completion - Pending Retuning Preview Ownership Surface

Date: 2026-05-30
Unit of Work: UOW-1780
Status: Complete

## Scope

Port the missing Java-shaped item-owned `pendingTuneResult` surface so the C# retuning planners store, read, and clear preview state on `InventoryItem` instead of carrying it only as detached planner input.

## Completed Work

- Added `dotnetConversion/src/Aion.GameServer/Model/GameObjects/InventoryItem.cs` property:
  - `PendingTuneResult? PendingTuneResult`
- Updated `dotnetConversion/src/Aion.GameServer/Services/TuningActionExecutionPlanService.cs` so completed retuning plans attach the generated preview to the target-item update snapshot.
- Updated `dotnetConversion/src/Aion.GameServer/Services/TuneResultApplicationPlanService.cs` so apply plans read from `InventoryItem.PendingTuneResult` and clear it on the applied snapshot.
- Updated `dotnetConversion/src/Aion.GameServer/Services/CmTuneResultPlanService.cs` so accept/cancel logic derives from `targetItem.PendingTuneResult` and clears that state on cancel.
- Updated `dotnetConversion/src/Aion.GameServer/Services/IdentifyItemExecutionPlanService.cs` to preserve pending-preview state when copying inventory snapshots.
- Updated focused tests in:
  - `dotnetConversion/tests/Aion.GameServer.Tests/TuningActionExecutionPlanServiceTests.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/TuneResultApplicationPlanServiceTests.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/CmTuneResultPlanServiceTests.cs`

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~TuneResultApplicationPlanServiceTests|FullyQualifiedName~CmTuneResultPlanServiceTests|FullyQualifiedName~TuningActionExecutionPlanServiceTests|FullyQualifiedName~IdentifyItemExecutionPlanServiceTests|FullyQualifiedName~GamePacketTests"`
- `dotnet test dotnetConversion\AionServer.slnx` with the default command timeout
- `dotnet test dotnetConversion\AionServer.slnx`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate"`
- `dotnet test dotnetConversion\AionServer.slnx`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate|FullyQualifiedName~HandleUseItemAsync_ExpExtractMergesRestrictedRewardWithCleanupSealFlag"`
- `dotnet test dotnetConversion\AionServer.slnx`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_CompositeStonesWritesCleanupSealFlagsForRemainingConsumedInputs"`

Result:

- Focused validation passed with 255 tests.
- The first full-solution attempt timed out at the command boundary before completion and is not counted as a completed validation run.
- Three subsequent full-solution runs each failed in the same pre-existing `GameServerConnectionInventoryExpansionUseItemTests` flake zone, but on different tests:
  - `ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate`
  - `HandleUseItemAsync_ExpExtractMergesRestrictedRewardWithCleanupSealFlag`
  - `ProcessPacketAsync_CompositeStonesWritesCleanupSealFlagsForRemainingConsumedInputs`
- Each failing test passed when rerun in isolation afterward (`1`, `2`, and `1` tests respectively).

## Java Artifacts Reviewed

- `com.aionemu.gameserver.model.gameobjects.Item.pendingTuneResult`
- `com.aionemu.gameserver.model.gameobjects.Item.getPendingTuneResult`
- `com.aionemu.gameserver.model.gameobjects.Item.setPendingTuneResult`
- `com.aionemu.gameserver.model.templates.item.actions.TuningAction.act`
- `com.aionemu.gameserver.services.item.ItemActionService.applyTuneResult`
- `com.aionemu.gameserver.network.aion.clientpackets.CM_TUNE_RESULT.runImpl`
- `com.aionemu.gameserver.services.item.ItemActionService.identifyItem`

## Migration Parity Table - UOW-1780

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.gameobjects.Item.pendingTuneResult` + getter/setter | `Aion.GameServer.Model.GameObjects.InventoryItem.PendingTuneResult` | Model State Surface | Complete | Unit Tested | Verified Parity | Java source reviewed; C# now carries the retuning preview directly on the item snapshot instead of only as detached planner input. Planner tests exercise set/read/clear ownership. |
| `com.aionemu.gameserver.model.templates.item.actions.TuningAction.act` pending-preview ownership | `Aion.GameServer.Services.TuningActionExecutionPlanService` | Service Boundary / Execution Planner | Partial | Unit Tested | Partial Parity | C# now mirrors Java `targetItem.setPendingTuneResult(result)` by attaching the generated preview to the target-item update snapshot. Live runtime wiring still remains out of scope. |
| `com.aionemu.gameserver.services.item.ItemActionService.applyTuneResult` | `Aion.GameServer.Services.TuneResultApplicationPlanService` | Service Boundary / Application Planner | Partial | Unit Tested | Partial Parity | The planner now reads from and clears `InventoryItem.PendingTuneResult` instead of taking a detached preview argument. No live persistence or send path is claimed. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_TUNE_RESULT.runImpl` pending-preview branches | `Aion.GameServer.Services.CmTuneResultPlanService` | Runtime Decision Planner | Partial | Unit Tested | Partial Parity | Accept/cancel logic now derives from the item-owned preview and clears it on cancel. No live packet registration or dispatch exists yet. |
| `com.aionemu.gameserver.services.item.ItemActionService.identifyItem` copied-item state preservation | `Aion.GameServer.Services.IdentifyItemExecutionPlanService` | Service Boundary / Execution Planner | Partial | Unit Tested | Partial Parity | The planner now preserves existing item-owned preview state when copying inventory snapshots. Live identify runtime wiring remains future work. |

## Tests Added Or Updated

| Test Name | What It Validates | Java-Equivalent Evidence | Test Type | Limitations |
|---|---|---|---|---|
| `CreateCompletionPlan_AttributeOnlyReusesTuneStateAndSetsJavaFlags` | Generated preview is attached to the attribute-only target-item snapshot. | Java `TuningAction.act` source | Unit | No live scheduler or packet dispatch |
| `CreateCompletionPlan_NormalTuneIncrementsCountAndMarksInventoryUpdateRequired` | Generated preview is attached to the normal retuning target-item snapshot. | Java `TuningAction.act` source | Unit | No live scroll decrease |
| `CreatePlan_AuditsWhenPendingTuneResultIsMissing` | Missing item-owned preview audits directly from the target item state. | Java `applyTuneResult` source | Unit | No live audit sink |
| `CreatePlan_AppliesPendingTuneResultToInventoryItem` | Applying a preview reads from and clears `InventoryItem.PendingTuneResult`. | Java `applyTuneResult` source | Unit | No live persistence/write path |
| `CreatePlan_AcceptedBranchAppliesPendingTuneResultAndBuildsInventoryUpdate` | Accepted reidentify consumes the item-owned preview. | Java `CM_TUNE_RESULT.runImpl` source | Unit | No live packet dispatch |
| `CreatePlan_CancelBranchClearsPreviewAndSendsApplyNo` | Cancel clears the item-owned preview before the inventory update. | Java `CM_TUNE_RESULT.runImpl` source | Unit | No live packet registration/dispatch |

## Risks / Gaps

- `CM_TUNE` / `CM_TUNE_RESULT` are still not registered client packet classes in C#.
- `GameClientPacketFactory` and `GameServerConnection` still do not dispatch the retuning packet flow live.
- The validation record for this unit includes repeated unrelated full-suite transients in the existing inventory-expansion test zone, so this unit should be treated as focused-green rather than full-suite clean.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped rows in this unit.
- Total artifacts ported: 1 model-state surface plus 4 planner updates and 6 focused regression updates.
- Total artifacts with verified parity: 1 grouped row.
- Total artifacts needing verification: 4 grouped rows.
- Total blocked artifacts: live `CM_TUNE` / `CM_TUNE_RESULT` packet registration and connection wiring, plus live scheduler/observer integration for identify/tuning execution.
- Estimated overall migration completion: Phase 6 remains about 72%.

## Next Recommended Unit of Work

- Port the narrow live packet-model and registration slice for `CM_TUNE` / `CM_TUNE_RESULT` through `GameClientPacketFactory`, now that the item-owned preview state exists.
- Safe alternatives if a different isolated slice is preferred:
  - add `CM_TUNE` connection dispatch only for the identify/audit/no-scroll branches first
  - `CraftService.finishCrafting` product selection
  - `DropRegistrationService.calculateBoostDropRate`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/InventoryItem.cs`
- `dotnetConversion/src/Aion.GameServer/Services/TuningActionExecutionPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/TuneResultApplicationPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/CmTuneResultPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/IdentifyItemExecutionPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/TuningActionExecutionPlanServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/TuneResultApplicationPlanServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmTuneResultPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1780-Completion.md`
- `docs/Phase-6-Session-1780-Handoff.md`
