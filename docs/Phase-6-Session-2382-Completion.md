# Phase 6 Session 2382 Completion - Register Autogroup Ready Match Runtime

## Scope

Added a minimal auto-instance runtime registration bridge for ready autogroup matches so matched players can use the existing `CM_AUTO_GROUP` window `102` press-enter path after receiving window `4`.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoInstance.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoPvpInstance.java`

Java behavior used:

- Java `createNewInstance(...)` allocates a `WorldMapInstance`, calls `autoInstance.onInstanceCreate(instance)`, stores the `AutoInstance`, removes matched queue entries, and sends ready-enter window `4`.
- Java `pressEnter(player, maskId)` later resolves the stored auto-instance by mask and registered player id, calls `AutoInstance.onPressEnter(player)`, and sends `SM_AUTO_GROUP(maskId, 5)`.
- For periodic PvP auto-groups, the matching runtime kind maps to Java `AutoPvpInstance`.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

- Added `RuntimeRegistration` to `AutoGroupApplyReadyMatchResult`.
- `ApplyReadyMatchPlanAsync(...)` now creates a minimal `AutoGroupInstanceRuntimeRegistration` for ready matches and accepts a callback to register it before ready-window deliveries are sent.
- `GameServerConnection` passes the runtime registration callback into `AutoGroupInstanceLeaveRuntimeService.RegisterInstance(...)`.
- The synthetic registration uses:
  - `WorldId` from the matched auto-group instance map id;
  - `InstanceMaskId` from the auto-group mask id;
  - `AutoGroupInstanceKind.PvpRaceInstance` for periodic PvP masks;
  - matched window `4` recipients as registered player object ids;
  - `InstanceId: 0`, which the existing runtime normalizes to instance id `1`.
- Extended connection coverage so a player can press enter after a live ready match and receive window `5`.

Known limitations:

- This is an explicit bridge, not full Java `InstanceService.getNextAvailableInstance(...)` parity.
- The synthetic instance id is not a real world instance allocation and can collide if multiple ready matches are active for the same map before real allocation is ported.
- `AutoInstance.onInstanceCreate(instance)`, real `WorldMapInstance` state, `startEnterTime`, penalty scheduling, quick-entry refill, and queue recheck remain missing.

## Validation Decision

- Changed surface: live connection dispatch and shared auto-instance runtime state.
- Specific behavior/contract: after a ready match sends window `4`, a matched player can use `CM_AUTO_GROUP` window `102`; the runtime resolves the synthetic auto-instance, removes group membership, and sends window `5`.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~AutoGroupInstanceLeaveRuntimeServiceTests|FullyQualifiedName~GameServerConnectionAutoGroupTests" --no-restore
```

Result: passed 52, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

- Focused Java/Maven command: skipped. No targeted Java fixture exists for this `createNewInstance(...)` runtime-registration bridge; Java source review supplied the auto-instance storage and press-enter lookup behavior.
- Broad-validation trigger: live connection dispatch and shared auto-instance runtime state changed.
- Broad .NET decision: skipped full project/solution validation. The focused command compiled the affected project and directly covered ready-match service output, existing runtime press/cancel behavior, and the edited connection path through window `100` then window `102`.
- Why this scope is sufficient: the UOW changed only auto-group ready-match/runtime dispatch; packet primitives, serialization helpers, persistence, scheduler, and real world allocation were not changed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.AutoGroupService.createNewInstance(...)` | `AutoGroupLookingPartyRegistrationService.ApplyReadyMatchPlanAsync(...)` / `AutoGroupInstanceLeaveRuntimeService.RegisterInstance(...)` | Service/Runtime | Partial | Unit Tested | Partial Parity | Ready matches now register matched players in a synthetic runtime instance before window `4` delivery. Real world allocation and `AutoInstance.onInstanceCreate` remain missing. |
| `com.aionemu.gameserver.services.AutoGroupService.pressEnter(...)` | `GameServerConnection.HandleAutoGroupAsync(...)` window `102` / `AutoGroupInstanceLeaveRuntimeService.PressEnter(...)` | Packet Handler/Runtime | Partial | Unit Tested | Partial Parity | Press-enter can resolve players registered by the ready-match bridge and sends window `5`. Real port-to-start-position and world instance state remain missing. |
| `com.aionemu.gameserver.model.autogroup.AutoInstance` | `AutoGroupInstanceRuntimeRegistration` / `AutoGroupInstanceRuntimeState` | Runtime Model | Partial | Unit Tested | Partial Parity | Registered player tracking is represented. Instance object, start time, max-player runtime state, and handler callbacks remain partial. |
| `com.aionemu.gameserver.model.autogroup.AutoPvpInstance` | `AutoGroupInstanceKind.PvpRaceInstance` | Runtime Model | Partial | Unit Tested | Partial Parity | Periodic PvP ready matches map to PvpRace runtime kind for leave/press-enter behavior. Race team formation on enter instance remains missing. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_AUTO_GROUP` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleAutoGroupAsync(...)` | Packet Handler | Partial | Unit Tested | Partial Parity | Window `100` now registers a runtime entry for ready matches; window `102` can consume it. Full instance allocation and quick-entry attachment remain missing. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `AutoGroupLookingPartyRegistrationServiceTests.ApplyReadyMatchPlan_RemovesMatchedQueuesAndSendsCleanupBeforeReadyWindowLikeJava` | Unit | Java source review | Ready-match application emits a PvpRace runtime registration intent with matched players before window delivery. | Focused C# service test. | Synthetic instance id only; no world allocation. |
| `GameServerConnectionAutoGroupTests.ProcessPacketAsync_AutoGroupReadyMatchSendsWindowFourAndRemovesQueuesLikeJava` | Unit | Java source review | Live window `100` ready match registers runtime state; the same player can then send window `102` and receive window `5`. | Focused C# connection test. | Does not teleport/port player into a real instance. |
| `AutoGroupInstanceLeaveRuntimeServiceTests.PressEnter_RemovesGroupAndKeepsRegisteredPlayerLikeJavaAutoGroupService` | Unit | Prior Java source review | Existing press-enter runtime behavior remains intact for registered players. | Adjacent focused runtime test. | Runtime state is synthetic until allocation is ported. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 5
- Total artifacts ported or extended in this UOW: 5
- Total artifacts with verified parity: 0
- Total artifacts needing verification or remaining partial: 5
- Total blocked artifacts: 0
- Estimated overall migration completion: Phase 6 remains in progress; autogroup ready-match press-enter reachability is partial, with real instance allocation still incomplete.

## Remaining Gaps

- Real Java `InstanceService.getNextAvailableInstance(...)` allocation remains missing.
- `AutoInstance.onInstanceCreate(instance)` and real `WorldMapInstance` registration remain missing.
- Java `AutoPvpInstance.onPressEnter(...)` port-to-start-position remains missing.
- Java `startEnterTime` remains missing.
- Penalty scheduling, delayed open-registration refresh, and member-cleanup queue recheck remain missing.
- Java `checkInstancesForOpenQuickEntries(lfp, maskId)` and quick-entry refill remain missing.

## Commit

Commit message:

```text
[Phase 6][UOW-2382] Register autogroup ready match runtime
```
