# Phase 6 Session 2381 Completion - Apply Autogroup Ready Match Windows

## Scope

Consumed the ready-match plan for live queue mutation and `SM_AUTO_GROUP(maskId, 4)` ready-enter fanout while keeping real world instance allocation deferred.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/LookingForParty.java`
- `game-server/src/com/aionemu/gameserver/services/autogroup/AutoGroupUtility.java`

Java behavior used:

- `AutoGroupService.createNewInstance(...)` removes matched queue entries, calls `searchAndRemoveAdditionalRegistrations(id)`, then sends `SM_AUTO_GROUP(maskId, 4)` for each matched member.
- `searchAndRemoveAdditionalRegistrations(id)` sends cleanup window `2` before the ready window `4` for that same matched member.
- Leader cleanup removes the whole additional party and notifies all members; member cleanup removes only the matched member and asks Java to re-check the queue.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

- Added `AutoGroupLookingPartyRegistrationService.ApplyReadyMatchPlanAsync(...)`.
- Added `AutoGroupApplyReadyMatchResult`, `AutoGroupApplyReadyMatchStatus`, and `AutoGroupWindowDeliveryIntent`.
- `ApplyReadyMatchPlanAsync(...)` now:
  - removes matched parties from live looking-party queues;
  - applies additional-registration leader/member cleanup to live queues;
  - sends cleanup window `2` and ready-enter window `4` packets through `IGameClientConnectionRegistry`;
  - preserves Java delivery order for cleanup-before-ready-window within each matched member;
  - reports penalty and queue-recheck intents that still require future live implementation.
- `GameServerConnection` now consumes a ready queue match after successful registration fanout and battleground announcement planning.
- Test fixtures can now provide `InstanceCooltimeTable` data for connection-level auto-group ready matching.

Known limitations:

- This UOW still does not allocate a real `WorldMapInstance`, call `AutoInstance.onInstanceCreate`, register an `AutoInstance`, or set Java `LookingForParty.startEnterTime`.
- Penalty scheduling and member-cleanup queue recheck are reported as result flags but not executed.
- Java `checkInstancesForOpenQuickEntries(lfp, maskId)` remains missing.

## Validation Decision

- Changed surface: live connection dispatch plus autogroup service queue mutation.
- Specific behavior/contract: a ready match after successful registration removes matched queues, sends cleanup window `2` before ready window `4`, sends window `4` only to matched online players, and preserves existing successful-registration fanout ordering.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~GameServerConnectionAutoGroupTests" --no-restore
```

Result: passed 43, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

- Focused Java/Maven command: skipped. No targeted Java fixture exists for `AutoGroupService.createNewInstance(...)`; Java source review supplied the ordering and mutation behavior.
- Broad-validation trigger: live connection dispatch changed.
- Broad .NET decision: skipped full project/solution validation. The focused command compiled the affected project and covered both the edited service mutation path and the directly edited connection dispatch path; packet primitives, serialization helpers, persistence, scheduler, and shared world state were not changed.
- Why this scope is sufficient: the UOW enabled a narrow `CM_AUTO_GROUP` window `100` ready-match branch and included a connection-level test that exercises the live dispatch path end to end with the service queue mutation.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.AutoGroupService.createNewInstance(...)` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService.ApplyReadyMatchPlanAsync(...)` | Service | Partial | Unit Tested | Partial Parity | Matched queue removal and window `4` fanout are live. Instance allocation, auto-instance registration, start-enter time, and penalty scheduling remain missing. |
| `com.aionemu.gameserver.services.AutoGroupService.searchAndRemoveAdditionalRegistrations(int)` | `ApplyAdditionalRegistrationCleanup(...)` / `AutoGroupAdditionalRegistrationCleanupIntent` | Service | Partial | Unit Tested | Partial Parity | Leader whole-party cleanup and member-only cleanup now mutate queues and send window `2`; penalty application and queue recheck remain deferred. |
| `com.aionemu.gameserver.services.autogroup.AutoGroupUtility.sendWindowToPlayerIfOnline(...)` | `IGameClientConnectionRegistry.SendPacketToPlayerAsync(...)` with `SmAutoGroup` | Utility/Dispatch | Partial | Unit Tested | Partial Parity | Ready and cleanup windows are sent only through the online-player registry. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_AUTO_GROUP` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleAutoGroupAsync(...)` | Packet Handler | Partial | Unit Tested | Partial Parity | Window `100` now consumes ready-match plans after successful registration. Real instance creation and quick-entry attachment remain missing. |
| `com.aionemu.gameserver.model.autogroup.LookingForParty` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistration` | DTO/Queue Entry | Partial | Unit Tested | Partial Parity | Queue entries support live removal and member cleanup. Java `startEnterTime` and AGPlayer class/name data remain partial. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `AutoGroupLookingPartyRegistrationServiceTests.ApplyReadyMatchPlan_RemovesMatchedQueuesAndSendsCleanupBeforeReadyWindowLikeJava` | Unit | Java source review | Matched entries are removed, leader additional registration is removed, cleanup window `2` precedes ready window `4`, and queues are empty afterward. | Focused C# service test. | Does not allocate instance or schedule penalties. |
| `AutoGroupLookingPartyRegistrationServiceTests.ApplyReadyMatchPlan_MutatesMemberCleanupAndKeepsLeaderEntryLikeJava` | Unit | Java source review | Member cleanup removes only matched members from an additional registration, keeps the leader queued, and reports queue-recheck intent. | Focused C# service test. | Does not execute queue recheck. |
| `AutoGroupLookingPartyRegistrationServiceTests.ApplyReadyMatchPlan_NotReadyDoesNotMutateOrSendPackets` | Unit | Java source review | Not-ready plans have no live side effect. | Focused C# service test. | Does not cover later instance path. |
| `GameServerConnectionAutoGroupTests.ProcessPacketAsync_AutoGroupReadyMatchSendsWindowFourAndRemovesQueuesLikeJava` | Unit | Java source review | The live `CM_AUTO_GROUP` window `100` path sends successful-registration packets first, then ready window `4`, and removes matched queues. | Focused C# connection test. | Does not register an auto-instance or support press-enter for the ready match yet. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 5
- Total artifacts ported or extended in this UOW: 5
- Total artifacts with verified parity: 0
- Total artifacts needing verification or remaining partial: 5
- Total blocked artifacts: 0
- Estimated overall migration completion: Phase 6 remains in progress; autogroup ready-match live queue/window behavior is partial, with instance runtime parity still incomplete.

## Remaining Gaps

- Real Java `createNewInstance(...)` instance allocation and `autoInstances` registry update remain missing.
- Java `startEnterTime` remains missing.
- Penalty scheduling and delayed open-registration refresh remain missing.
- Member-cleanup queue recheck remains missing.
- Java `checkInstancesForOpenQuickEntries(lfp, maskId)` remains missing.
- Quick-entry refill and full auto-instance lifecycle remain missing.
- Harmony, FFA/solo/glory, and recruitable auto-instance matching remain partial or missing.

## Commit

Commit message:

```text
[Phase 6][UOW-2381] Apply autogroup ready match windows
```
