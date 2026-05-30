# Phase 6 Session 1774 Completion - TuningAction Guard Planner

Date: 2026-05-30
Unit of Work: UOW-1774
Status: Complete

## Scope

Port the deterministic Java `TuningAction.canAct` guard chain into a non-live C# planner with objective tests, and add the missing retuning denial `SM_SYSTEM_MESSAGE` factories that the Java guard path emits.

## Completed Work

- Added `dotnetConversion/src/Aion.GameServer/Services/TuningActionGuardPlanService.cs`.
- Added `TuningActionTargetType`, `TuningActionGuardPlanStatus`, and `TuningActionGuardPlan`.
- Modeled the Java `TuningAction.canAct` guard order exactly:
  - equipped-target silent `false`
  - unidentified-target denial
  - untunable-target denial
  - wrong weapon/armor target denial
  - higher-level target denial
  - final max-tune-count silent `false`
  - `shouldNotReduceTuneCount` bypass of the final guard
- Added the missing system-message factories in `SmSystemMessage.cs`:
  - `ItemReidentifyWrongSelect`
  - `ItemReidentifyWrongLevel`
  - `ItemReidentifyCannotReidentify`
  - `ItemReidentifyDidntIdentify`
- Added focused unit coverage in `dotnetConversion/tests/Aion.GameServer.Tests/TuningActionGuardPlanServiceTests.cs`.
- Extended `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs` with packet regressions for the four retuning denial factories.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~TuningActionGuardPlanServiceTests|FullyQualifiedName~GamePacketTests"`
- `dotnet test dotnetConversion\AionServer.slnx`

Result:

- Focused validation passed with 248 tests.
- Full solution validation passed with 4714 tests total.

## Java Artifacts Reviewed

- `com.aionemu.gameserver.model.templates.item.actions.TuningAction.canAct`
- `com.aionemu.gameserver.model.templates.item.actions.UseTarget`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_MSG_ITEM_REIDENTIFY_WRONG_SELECT`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_MSG_ITEM_REIDENTIFY_WRONG_LEVEL`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_MSG_ITEM_REIDENTIFY_CANNOT_REIDENTIFY`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_MSG_ITEM_REIDENTIFY_DIDNT_IDENTIFY`

## Migration Parity Table - UOW-1774

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.templates.item.actions.TuningAction.canAct` | `Aion.GameServer.Services.TuningActionGuardPlanService.CreatePlan` | Service Boundary / Guard Planner | Partial | Unit Tested | Partial Parity | C# mirrors the Java guard order and denial-message selection for the non-live `canAct` boundary. Live packet dispatch, XML action binding, and delayed `act(...)` flow remain outside this unit. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_MSG_ITEM_REIDENTIFY_WRONG_SELECT` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage.ItemReidentifyWrongSelect` | System Message Factory | Complete | Regression Tested | Verified Parity | Java source reviewed; tests cover message id `1401633` and two parameters. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_MSG_ITEM_REIDENTIFY_WRONG_LEVEL` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage.ItemReidentifyWrongLevel` | System Message Factory | Complete | Regression Tested | Verified Parity | Java source reviewed; tests cover message id `1401635` and two parameters. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_MSG_ITEM_REIDENTIFY_CANNOT_REIDENTIFY` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage.ItemReidentifyCannotReidentify` | System Message Factory | Complete | Regression Tested | Verified Parity | Java source reviewed; tests cover message id `1401636` and one parameter. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_MSG_ITEM_REIDENTIFY_DIDNT_IDENTIFY` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage.ItemReidentifyDidntIdentify` | System Message Factory | Complete | Regression Tested | Verified Parity | Java source reviewed; tests cover message id `1401637` and one parameter. |
| `com.aionemu.gameserver.model.templates.item.actions.UseTarget` | `Aion.GameServer.Services.TuningActionTargetType` | Enum / Planner Input | Partial | Unit Tested through planner | Partial Parity | Planner-only modeling so far; no live XML/action binding parity is claimed. |

## Tests Added

| Test Name | What It Validates | Java-Equivalent Evidence | Test Type | Limitations |
|---|---|---|---|---|
| `CreatePlan_AllowsJavaHappyPath` | Valid identified tunable target passes the Java guard chain. | Reviewed Java `TuningAction.canAct` | Unit | No live action invocation |
| `CreatePlan_FollowsJavaGuardOrder` | Equipped-target guard returns first and suppresses later branches. | Reviewed Java `TuningAction.canAct` | Unit | Silent branch only; no packet send |
| `CreatePlan_UsesJavaSystemMessagesForTargetStateFailures` | Unidentified and untunable targets emit the correct message ids. | Reviewed Java `TuningAction.canAct` + `SM_SYSTEM_MESSAGE` source | Unit | No encrypted runtime capture |
| `CreatePlan_UsesJavaWrongSelectMessageForTargetMismatch` | Weapon/armor mismatch emits `WRONG_SELECT`. | Reviewed Java `TuningAction.canAct` | Unit | No live XML binding |
| `CreatePlan_UsesJavaWrongLevelMessageForHigherLevelTarget` | Higher-level target emits `WRONG_LEVEL`. | Reviewed Java `TuningAction.canAct` | Unit | No runtime dispatch |
| `CreatePlan_FinalTuneCountGuardIsSilentLikeJava` | Max tune count rejects without a denial packet. | Reviewed Java `TuningAction.canAct` | Unit | No live item action |
| `CreatePlan_ShouldNotReduceTuneCountBypassesJavaFinalGuard` | `no_reduce=true` bypasses the final tune-count guard. | Reviewed Java `TuningAction.canAct` | Unit | No live XML binding |
| `GamePacketTests` retuning factory assertions | All four retuning denial factories serialize correct ids and parameters. | Reviewed Java `SM_SYSTEM_MESSAGE` source | Regression | No Java runtime/encrypted frame capture |

## Risks / Gaps

- `TuningAction.act` remains out of scope: delayed item-use animation, abort observer, source consumption, pending tune result, and `SM_TUNE_RESULT` still need parity work.
- `TuningActionTargetType` is planner-only and is not yet bound from live item action metadata.
- No Java runtime or golden packet capture was produced for the retuning denial branches.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped rows in this unit.
- Total artifacts ported: 1 guard planner, 1 planner enum, 1 planner record, 4 system-message factories, and 8 focused test updates/additions.
- Total artifacts with verified parity: 4 grouped rows.
- Total artifacts needing verification: 2 grouped rows.
- Total blocked artifacts: 0 in this unit.
- Estimated overall migration completion: Phase 6 remains about 72%.

## Next Recommended Unit of Work

- Port the adjacent non-live Java `TuningAction.act` boundary next: item-use animation order, abort branch, source-scroll consumption, pending tune result mutation, `SM_TUNE_RESULT`, and success/cancel system-message composition.
- Safe alternatives if a different isolated slice is preferred:
  - `CraftService.finishCrafting` product selection
  - `DropRegistrationService.calculateBoostDropRate`
  - `PlayerReviveService.rebirthRevive`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/src/Aion.GameServer/Services/TuningActionGuardPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/TuningActionGuardPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1774-Completion.md`
- `docs/Phase-6-Session-1774-Handoff.md`
