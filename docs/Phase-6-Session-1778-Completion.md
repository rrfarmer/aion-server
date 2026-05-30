# Phase 6 Session 1778 Completion - CM_TUNE_RESULT Preview Application Planner

Date: 2026-05-30
Unit of Work: UOW-1778
Status: Complete

## Scope

Port the adjacent non-live Java `CM_TUNE_RESULT.runImpl` / `ItemActionService.applyTuneResult` boundary so the retuning planner chain covers preview acceptance, cancellation, and preview-application semantics without overstating live packet or persistence wiring.

## Completed Work

- Added `dotnetConversion/src/Aion.GameServer/Services/TuneResultApplicationPlanService.cs` to model Java `ItemActionService.applyTuneResult`:
  - missing pending preview -> audit only
  - present pending preview -> apply optional sockets, enchant bonus, and stat bonus id to the item snapshot
  - item/inventory persistence intents recorded conservatively
- Added `dotnetConversion/src/Aion.GameServer/Services/CmTuneResultPlanService.cs` to model Java `CM_TUNE_RESULT.runImpl`:
  - missing target item -> silent return
  - accepted preview -> apply-yes branch
  - accepted preview without pending state -> audit via inner apply call but still send apply-yes and inventory update
  - attribute-only cancel attempt -> audit and force apply-yes branch
  - normal cancel -> apply-no branch plus inventory update
- Added the missing reidentify-apply system-message factories in `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`:
  - `ItemReidentifyApplyYes`
  - `ItemReidentifyApplyNo`
- Extended `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs` with message regressions for ids `1401910` and `1401911`.
- Added focused test files:
  - `dotnetConversion/tests/Aion.GameServer.Tests/TuneResultApplicationPlanServiceTests.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/CmTuneResultPlanServiceTests.cs`

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~TuneResultApplicationPlanServiceTests|FullyQualifiedName~CmTuneResultPlanServiceTests|FullyQualifiedName~GamePacketTests"`
- `dotnet test dotnetConversion\AionServer.slnx`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_CompositeStonesWritesCleanupSealFlagsForRemainingConsumedInputs|FullyQualifiedName~HandleUseItemAsync_ExpExtractMergesRestrictedRewardWithCleanupSealFlag"`
- `dotnet test dotnetConversion\AionServer.slnx`

Result:

- Focused validation passed with 247 tests.
- The first full solution run reported two transient unrelated inventory-expansion failures.
- The isolated rerun of both failures passed with 2 tests.
- The second full solution rerun passed cleanly with 4733 tests total.

## Java Artifacts Reviewed

- `com.aionemu.gameserver.network.aion.clientpackets.CM_TUNE_RESULT`
- `com.aionemu.gameserver.services.item.ItemActionService.applyTuneResult`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_MSG_ITEM_REIDENTIFY_APPLY_YES`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_MSG_ITEM_REIDENTIFY_APPLY_NO`

## Migration Parity Table - UOW-1778

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemActionService.applyTuneResult` | `Aion.GameServer.Services.TuneResultApplicationPlanService` | Service Boundary / Application Planner | Partial | Unit Tested | Partial Parity | C# now models the deterministic audit-only and apply-mutation branches, but still carries pending preview state as explicit planner input instead of a live item field. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_TUNE_RESULT.runImpl` | `Aion.GameServer.Services.CmTuneResultPlanService` | Runtime Decision Planner | Partial | Unit Tested | Partial Parity | Accept/cancel branch ordering, attribute-only cancel override, and unconditional inventory-update tail are modeled. No live packet registration or connection wiring exists yet. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_MSG_ITEM_REIDENTIFY_APPLY_YES` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage.ItemReidentifyApplyYes` | System Message Factory | Complete | Regression Tested | Verified Parity | Java source reviewed; tests cover id `1401910` and single-string payload shape. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_MSG_ITEM_REIDENTIFY_APPLY_NO` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage.ItemReidentifyApplyNo` | System Message Factory | Complete | Regression Tested | Verified Parity | Java source reviewed; tests cover id `1401911` and zero-parameter payload shape. |

## Tests Added

| Test Name | What It Validates | Java-Equivalent Evidence | Test Type | Limitations |
|---|---|---|---|---|
| `CreatePlan_AuditsWhenPendingTuneResultIsMissing` | Missing preview state audits and returns without mutation. | Java `ItemActionService.applyTuneResult` source | Unit | No live audit sink |
| `CreatePlan_AppliesPendingTuneResultToInventoryItem` | Preview application updates optional sockets, enchant bonus, and stat bonus id while preserving tune count. | Java `ItemActionService.applyTuneResult` source | Unit | No live pending-state clear |
| `CreatePlan_AcceptedBranchAuditsMissingPendingButStillSendsApplyYesAndInventoryUpdate` | Accepted branch preserves Java’s inner-audit-but-still-send behavior. | Java `CM_TUNE_RESULT.runImpl` + `applyTuneResult` | Unit | No live packet dispatch |
| `CreatePlan_AcceptedBranchAppliesPendingTuneResultAndBuildsInventoryUpdate` | Accepted preview prepares the Java-shaped apply-yes and inventory-update intents. | Java `CM_TUNE_RESULT.runImpl` | Unit | No live persistence |
| `CreatePlan_AttributeOnlyCancelForcesApplyAndAudits` | Attribute-only cancel attempts are audited and forced into the apply-yes branch. | Java `CM_TUNE_RESULT.runImpl` | Unit | No live audit sink |
| `CreatePlan_CancelBranchClearsPreviewAndSendsApplyNo` | Normal cancel sends apply-no and still prepares inventory update intent. | Java `CM_TUNE_RESULT.runImpl` | Unit | Pending-preview clear is conceptual only |
| `GamePacketTests` retuning message assertions | The two reidentify-apply system messages serialize the expected ids and parameter counts. | Java `SM_SYSTEM_MESSAGE` source | Regression | No encrypted runtime frame capture |

## Risks / Gaps

- The planners are intentionally not live-wired yet, so client packets still do not traverse this path in production C#.
- No live item-owned pending-preview state exists yet, so this unit models that state as an explicit planner input instead of a runtime field.
- Java `item.setPendingTuneResult(null)` is only represented conceptually in planner outcomes for now.
- The first full-suite run again surfaced transient unrelated inventory-expansion failures; rerun evidence is documented explicitly.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped rows in this unit.
- Total artifacts ported: 2 planner services, 2 system-message factories, and 7 focused regression additions/updates.
- Total artifacts with verified parity: 2 grouped rows.
- Total artifacts needing verification: 2 grouped rows.
- Total blocked artifacts: live `CM_TUNE` / `CM_TUNE_RESULT` packet wiring, live pending-preview state ownership, and Java identify-item execution parity.
- Estimated overall migration completion: Phase 6 remains about 72%.

## Next Recommended Unit of Work

- Port the adjacent non-live `ItemActionService.identifyItem` delayed execution boundary next so the retuning planner chain covers both Java entry branches before any live packet wiring is attempted.
- Safe alternatives if a different isolated slice is preferred:
  - a narrowly scoped live `CM_TUNE` / `CM_TUNE_RESULT` packet registration + connection-dispatch slice
  - `CraftService.finishCrafting` product selection
  - `DropRegistrationService.calculateBoostDropRate`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/TuneResultApplicationPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/CmTuneResultPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/TuneResultApplicationPlanServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmTuneResultPlanServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1778-Completion.md`
- `docs/Phase-6-Session-1778-Handoff.md`
