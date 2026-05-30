# Phase 6 Session 1782 Completion - Live CM_TUNE Dispatch Slice

Date: 2026-05-30
Unit of Work: UOW-1782
Status: Complete

## Scope

Port the narrow live `GameServerConnection` dispatch slice for Java `CM_TUNE.runImpl`, using the already-ported planners to drive the identify, audit, guard-denial, and executable retuning branches without broadening into `CM_TUNE_RESULT` runtime work yet.

## Completed Work

- Updated `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`:
  - registered live `CmTune` dispatch in `HandleInfrastructurePacketAsync`
  - added `HandleTuneAsync`
  - added `ScheduleIdentifyItemAsync`
  - added `CompleteIdentifyItemAsync`
  - added `ScheduleTuningActionAsync`
  - added `CompleteTuningActionAsync`
  - added pending-item-use cancel routing for:
    - `ItemIdentify`
    - `ItemReidentify`
- Added focused live packet-path coverage:
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionTuneTests.cs`

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionTuneTests|FullyQualifiedName~CmTuneTests|FullyQualifiedName~CmTuneRuntimePlanServiceTests|FullyQualifiedName~TuningActionExecutionPlanServiceTests|FullyQualifiedName~TuningActionGuardPlanServiceTests"`
- `dotnet test dotnetConversion\AionServer.slnx`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate"`
- `dotnet test dotnetConversion\AionServer.slnx`

Result:

- Focused retuning validation passed with 25 tests.
- The first full-suite attempt timed out at the command boundary and is not counted as a completed run.
- The next two full-suite runs each failed in the same pre-existing transient `GameServerConnectionInventoryExpansionUseItemTests.ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate` flake zone, but with different failure shapes:
  - cleanup-seal assertion mismatch
  - `WaitUntilAsync` timeout
- The isolated rerun of `ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate` then passed with 1 test.
- This unit is therefore documented as focused-green with repeated unrelated full-suite transient failures, not as a clean all-green full-suite pass.

## Java Artifacts Reviewed

- `com.aionemu.gameserver.network.aion.clientpackets.CM_TUNE`
- `com.aionemu.gameserver.services.item.ItemActionService.identifyItem`
- `com.aionemu.gameserver.model.templates.item.actions.TuningAction`

## Migration Parity Table - UOW-1782

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_TUNE.runImpl` runtime dispatch | `Aion.GameServer.Network.Aion.GameServerConnection.HandleTuneAsync` | Client Packet Runtime Dispatch | Partial | Integration Tested | Partial Parity | C# now performs live inventory lookup, preserves Java branch order, and drives identify / silent-return / audit / guard / execute branches through `ProcessPacketAsync`. |
| `com.aionemu.gameserver.services.item.ItemActionService.identifyItem` live delayed execution | `Aion.GameServer.Network.Aion.GameServerConnection.ScheduleIdentifyItemAsync` + `CompleteIdentifyItemAsync` | Runtime Item Action Bridge | Partial | Integration Tested | Partial Parity | C# now sends Java-shaped identify animations, mutates the target item, sends `SM_INVENTORY_UPDATE_ITEM`, and sends `STR_MSG_ITEM_IDENTIFY_SUCCEED`. Dedicated observer/task persistence lifecycle remains future work. |
| `com.aionemu.gameserver.model.templates.item.actions.TuningAction.act` live delayed execution boundary | `Aion.GameServer.Network.Aion.GameServerConnection.ScheduleTuningActionAsync` + `CompleteTuningActionAsync` | Runtime Item Action Bridge | Partial | Integration Tested | Partial Parity | C# now sends Java-shaped retuning animations, consumes the scroll, stamps pending preview state, and sends `SM_TUNE_RESULT` plus `STR_MSG_ITEM_REIDENTIFY_SUCCEED`. `CM_TUNE_RESULT` runtime dispatch remains future work. |
| `com.aionemu.gameserver.services.item.ItemActionService.identifyItem.abort()` + `com.aionemu.gameserver.model.templates.item.actions.TuningAction.act.abort()` | `Aion.GameServer.Network.Aion.GameServerConnection.PendingItemUseCancelMessage.ItemIdentify` / `.ItemReidentify` | Pending Item Use Cancellation Mapping | Complete | Integration Tested | Verified Parity | Live cancellation routing now maps to the Java identify and reidentify cancel messages. |

## Tests Added

| Test Name | What It Validates | Java-Equivalent Evidence | Test Type | Limitations |
|---|---|---|---|---|
| `ProcessPacketAsync_CmTune_IdentifyBranchBroadcastsAndUpdatesIdentifiedItem` | Unidentified `CM_TUNE` drives live identify animations, inventory update, and identify-success message. | Java `CM_TUNE.runImpl` + `ItemActionService.identifyItem` source | Integration | No Java runtime capture artifact |
| `ProcessPacketAsync_CmTune_GuardDeniedSendsJavaSystemMessage` | Guard failure sends the expected Java denial message without inventory mutation. | Java `CM_TUNE.runImpl` + `TuningAction.canAct` source | Integration | No audit-log assertion |
| `ProcessPacketAsync_CmTune_ExecutableActionConsumesScrollAndSendsTunePreview` | Executable retuning consumes the scroll, stamps pending preview state, and sends `SM_TUNE_RESULT` plus success message. | Java `CM_TUNE.runImpl` + `TuningAction.act` source | Integration | `CM_TUNE_RESULT` live accept/cancel flow not covered |

## Risks / Gaps

- `CmTuneResult` still does not dispatch live in `GameServerConnection`.
- This unit does not introduce a dedicated persistence write boundary for the new live identify/retuning runtime mutations; future work still needs to prove the Java `UPDATE_REQUIRED` lifecycle end to end.
- Full-suite validation remains noisy because of the unrelated composite-stones transient noted above.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped rows in this unit.
- Total artifacts ported: 1 live `CmTune` dispatch path, 4 runtime helpers, 2 cancel-message mappings, and 3 focused live integration tests.
- Total artifacts with verified parity: 1 grouped row.
- Total artifacts needing verification: 3 grouped rows.
- Total blocked artifacts: live `CmTuneResult` dispatch and end-to-end retuning apply/cancel runtime parity.
- Estimated overall migration completion: Phase 6 remains about 72%.

## Next Recommended Unit of Work

- Port the narrow `GameServerConnection` dispatch slice for `CmTuneResult` so the existing Java-shaped retuning planners can complete the live retuning loop.
- Safe alternatives if a different isolated slice is preferred:
  - wire only the accepted-apply `CM_TUNE_RESULT` branch first
  - `CraftService.finishCrafting` product selection
  - `DropRegistrationService.calculateBoostDropRate`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionTuneTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1782-Completion.md`
- `docs/Phase-6-Session-1782-Handoff.md`
