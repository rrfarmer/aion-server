# Phase 6 Session 2383 Completion - Track Autogroup Ready Enter Start Time

## Scope

Added explicit ready-enter timestamp state for autogroup ready matches, matching the Java `LookingForParty.setStartEnterTime()` call made during `AutoGroupService.createNewInstance(...)`.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/LookingForParty.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoInstance.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoPvpInstance.java`

Java behavior used:

- `createNewInstance(...)` removes each matched `LookingForParty`, calls `lfp.setStartEnterTime()`, removes additional registrations, and sends ready-enter window `4`.
- `LookingForParty.isOnStartEnterTask()` treats the timestamp as a 120-second ready-enter window.
- The current C# ready-match bridge stores matched entries in runtime state after removing them from the queue, so the timestamp belongs on the runtime registration/snapshot until full Java auto-instance state is represented.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

- `ApplyReadyMatchPlanAsync(...)` now stamps ready runtime registrations with `ReadyEnterStartTime`, defaulting to `DateTimeOffset.UtcNow`.
- Added an optional deterministic `readyEnterStartTime` parameter to `ApplyReadyMatchPlanAsync(...)` for focused parity tests.
- `AutoGroupInstanceRuntimeRegistration`, `AutoGroupInstanceRuntimeState`, and `AutoGroupInstanceRuntimeSnapshot` now carry `ReadyEnterStartTime`.
- Updated ready-match and runtime tests to assert that the timestamp survives the apply and runtime snapshot boundaries.
- Updated the ready-match Java-source note to no longer list `startEnterTime` as deferred for this partial slice.

Known limitations:

- This UOW tracks the timestamp but does not yet enforce the 120-second expiry.
- The ready-match runtime still uses a synthetic instance id and does not allocate a real Java-equivalent `WorldMapInstance`.
- `AutoInstance.onInstanceCreate(instance)`, port-to-start-position, penalty scheduling, quick-entry refill, and full lifecycle remain missing.

## Validation Decision

- Changed surface: ready-match apply result and shared auto-instance runtime state.
- Specific behavior/contract: a ready match records the Java `startEnterTime` equivalent before window `4` delivery, and the runtime snapshot preserves that value through press-enter state.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~AutoGroupInstanceLeaveRuntimeServiceTests|FullyQualifiedName~GameServerConnectionAutoGroupTests" --no-restore
```

Result: passed 52, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

- Focused Java/Maven command: skipped. No targeted Java fixture exists for this narrow timestamp-carrying bridge; Java source review supplied the `setStartEnterTime()` and `isOnStartEnterTask()` behavior.
- Broad-validation trigger: shared auto-instance runtime state changed.
- Broad .NET decision: skipped full project/solution validation. The focused command compiled the affected project and directly covered ready-match service output, runtime press/cancel/leave behavior, and the edited connection ready-match path.
- Why this scope is sufficient: the UOW only added state carried by auto-group ready-match/runtime APIs; packet serialization, scheduler behavior, persistence, and world allocation were not changed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.AutoGroupService.createNewInstance(...)` | `AutoGroupLookingPartyRegistrationService.ApplyReadyMatchPlanAsync(...)` / `AutoGroupInstanceRuntimeRegistration` | Service/Runtime | Partial | Unit Tested | Partial Parity | Ready-match runtime registrations now carry the Java `lfp.setStartEnterTime()` equivalent before window `4` delivery. Real world allocation remains missing. |
| `com.aionemu.gameserver.model.autogroup.LookingForParty` | `AutoGroupInstanceRuntimeRegistration.ReadyEnterStartTime` / `AutoGroupInstanceRuntimeSnapshot.ReadyEnterStartTime` | Runtime Model | Partial | Unit Tested | Partial Parity | `startEnterTime` is represented as runtime state after matched parties leave the queue. Expiry enforcement remains missing. |
| `com.aionemu.gameserver.model.autogroup.AutoInstance` | `AutoGroupInstanceRuntimeState` | Runtime Model | Partial | Unit Tested | Partial Parity | Runtime state preserves matched players and ready-enter timestamp. Java instance reference, start instance time, and handler callbacks remain partial. |
| `com.aionemu.gameserver.services.AutoGroupService.pressEnter(...)` | `AutoGroupInstanceLeaveRuntimeService.PressEnter(...)` | Runtime Service | Partial | Unit Tested | Partial Parity | Press-enter snapshots expose ready-enter timestamp state while preserving prior group/alliance cleanup behavior. Port-to-start-position remains missing. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `AutoGroupLookingPartyRegistrationServiceTests.ApplyReadyMatchPlan_RemovesMatchedQueuesAndSendsCleanupBeforeReadyWindowLikeJava` | Unit | Java source review | Ready-match application emits runtime registration with the modeled `setStartEnterTime()` timestamp. | Focused C# service test. | No expiry enforcement or real world allocation. |
| `AutoGroupInstanceLeaveRuntimeServiceTests.PressEnter_RemovesGroupAndKeepsRegisteredPlayerLikeJavaAutoGroupService` | Unit | Java source review | Runtime snapshots preserve `ReadyEnterStartTime` through press-enter while keeping registered players. | Focused C# runtime test. | Does not teleport/port player into a real instance. |
| `GameServerConnectionAutoGroupTests.ProcessPacketAsync_AutoGroupReadyMatchSendsWindowFourAndRemovesQueuesLikeJava` | Unit | Prior Java source review | Existing live ready-match window `4` and window `102` behavior remains covered after adding timestamp state. | Adjacent focused connection test. | Timestamp is not asserted through connection-level clock injection. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported or extended in this UOW: 4
- Total artifacts with verified parity: 0
- Total artifacts needing verification or remaining partial: 4
- Total blocked artifacts: 0
- Estimated overall migration completion: Phase 6 remains in progress; ready-enter timestamp state is now represented, while real allocation and expiry/lifecycle behavior remain incomplete.

## Remaining Gaps

- Real Java `InstanceService.getNextAvailableInstance(...)` allocation remains missing.
- `AutoInstance.onInstanceCreate(instance)` and real `WorldMapInstance` registration remain missing.
- Java `AutoPvpInstance.onPressEnter(...)` port-to-start-position remains missing.
- The 120-second `LookingForParty.isOnStartEnterTask()` expiry is not enforced yet.
- Penalty scheduling, delayed open-registration refresh, and member-cleanup queue recheck remain missing.
- Java `checkInstancesForOpenQuickEntries(lfp, maskId)` and quick-entry refill remain missing.

## Commit

Commit message:

```text
[Phase 6][UOW-2383] Track autogroup ready enter start time
```
