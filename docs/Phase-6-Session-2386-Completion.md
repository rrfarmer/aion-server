# Phase 6 Session 2386 Completion - Add Autogroup Open Quick-Entry Runtime Gate

## Scope

Ported the first runtime gate for Java open quick-entry registration after auto-instance creation.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoInstance.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoPvpInstance.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/LookingForParty.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoGroupType.java`

Java behavior used:

- `AutoGroupService.checkInstancesForOpenQuickEntries(lfp, maskId)` only considers `QUICK_GROUP_ENTRY` parties that are not on the 120-second start-enter task.
- `AutoInstance.isRegistrationDisabled(lfp)` permits quick entries after instance creation only while `now - startInstanceTime <= agt.getMaximumJoinTime()`.
- `AutoPvpInstance.addLookingForParty(lfp)` rejects parties that would exceed total or race-specific capacity, then registers all member object ids.
- `AutoGroupService.createNewInstance(...)` removes matched queue entries, sets `LookingForParty.startEnterTime`, and sends window `4`; this UOW only added the runtime attachment gate, not the live packet bridge.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

- Extended `AutoGroupInstanceRuntimeRegistration` and snapshots with:
  - `MaximumJoinTimeMilliseconds`
  - `MaxPlayers`
  - `RegisteredPlayerRacesByObjectId`
- Ready-match runtime registration now carries matched member race facts and the Java-derived maximum join window into the runtime registry.
- Added `AutoGroupInstanceLeaveRuntimeService.TryAddOpenQuickEntry(...)` to model the first Java open quick-entry gate:
  - rejects non-quick entries;
  - rejects parties still inside the 120-second start-enter task;
  - rejects quick entries after `MaximumJoinTimeMilliseconds`;
  - rejects total-capacity overflow;
  - rejects race-capacity overflow;
  - registers accepted quick-entry member ids and race facts.
- Added focused unit tests for accepted quick-entry runtime attachment, maximum-join expiry, start-enter task rejection, and race capacity rejection.

Known limitations:

- `StartLooking(...)` does not yet call the runtime open quick-entry gate before queue matching.
- Accepted quick entries are not yet removed from the looking-party queue, do not send live window `4`, and do not clean additional registrations.
- `checkQueueForQuickEntries(autoInstance)` refill after cancel/leave remains missing.
- Java instance score-ended checks, spawn content, and press-enter port-to-start-position remain incomplete.

## Validation Decision

- Changed surface: shared autogroup runtime state, ready-match runtime-registration metadata, and autogroup focused tests.
- Specific behavior/contract: Java open quick-entry eligibility around `checkInstancesForOpenQuickEntries`, `LookingForParty.isOnStartEnterTask()`, `AutoInstance.isRegistrationDisabled(lfp)`, and `AutoPvpInstance.addLookingForParty(lfp)` capacity gates.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~AutoGroupInstanceLeaveRuntimeServiceTests|FullyQualifiedName~GameServerConnectionAutoGroupTests" --no-restore
```

Result: passed 55, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

- Focused Java/Maven command: skipped. No targeted Java fixture exists for this open quick-entry runtime gate; Java source review supplied the source-of-truth branch behavior.
- Broad-validation trigger: shared autogroup runtime state changed.
- Broad .NET decision: skipped full project/solution validation. The focused command compiled the affected project and covered the runtime registry, looking-party ready-match bridge, and adjacent live connection autogroup behavior.
- Why this scope is sufficient: this UOW added a contained runtime gate and metadata flow; it did not alter packet serialization, database access, static XML parsing, spawn engine internals, or scheduler wiring.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.AutoGroupService.checkInstancesForOpenQuickEntries(...)` | `AutoGroupInstanceLeaveRuntimeService.TryAddOpenQuickEntry(...)` | Service/Runtime | Partial | Unit Tested | Partial Parity | Runtime acceptance/rejection gates are modeled. Start-looking live queue removal, window `4`, and additional-registration cleanup are not wired yet. |
| `com.aionemu.gameserver.model.autogroup.AutoInstance.isRegistrationDisabled(...)` | `AutoGroupInstanceRuntimeState.TryAddOpenQuickEntry(...)` | Runtime Model | Partial | Unit Tested | Partial Parity | Quick-entry maximum-join rejection is covered for modeled runtime instances. Instance score-ended behavior remains missing. |
| `com.aionemu.gameserver.model.autogroup.AutoPvpInstance.addLookingForParty(...)` | `AutoGroupInstanceRuntimeState.TryAddOpenQuickEntry(...)` | Runtime Model | Partial | Unit Tested | Partial Parity | Total and race-capacity rejection plus member registration are covered for open quick entries. Full pre-create `READY` behavior is handled elsewhere and remains partial overall. |
| `com.aionemu.gameserver.model.autogroup.LookingForParty.isOnStartEnterTask()` | `AutoGroupOpenQuickEntryRequest.IsOnStartEnterTask(...)` | Model/Runtime Helper | Partial | Unit Tested | Partial Parity | 120-second guard is represented for runtime quick-entry requests. Looking-party registrations do not yet persist start-enter state for queue refill. |
| `com.aionemu.gameserver.model.autogroup.AutoGroupType` | `AutoGroupSummary` / `AutoGroupInstanceRuntimeRegistration` | Enum Metadata/Dataholder | Partial | Unit Tested | Partial Parity | `MaximumJoinTimeMilliseconds` now flows into runtime registration and is consumed by the quick-entry gate. Full enum behavior remains partial. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `AutoGroupInstanceLeaveRuntimeServiceTests.TryAddOpenQuickEntry_AddsQuickPlayerWithinMaximumJoinWindowLikeJavaCheckInstances` | Unit | Java source review | Quick entry accepted at the Java max-join boundary and registered into runtime state with race facts. | Focused C# unit test. | Does not send window `4` or clean additional registrations. |
| `AutoGroupInstanceLeaveRuntimeServiceTests.TryAddOpenQuickEntry_RejectsAfterMaximumJoinWindowLikeJavaIsRegistrationDisabled` | Unit | Java source review | Quick entry rejected when `now - startInstanceTime` exceeds maximum join time by one millisecond. | Focused C# unit test. | Instance score-ended rejection remains missing. |
| `AutoGroupInstanceLeaveRuntimeServiceTests.TryAddOpenQuickEntry_RejectsReadyEnterTaskAndRaceOverCapacityLikeJava` | Unit | Java source review | 120-second start-enter guard and race-capacity rejection are enforced. | Focused C# unit test. | Queue refill caller is not wired yet. |
| `AutoGroupLookingPartyRegistrationServiceTests.ApplyReadyMatchPlan_RemovesMatchedQueuesAndSendsCleanupBeforeReadyWindowLikeJava` | Unit | Java source review | Ready-match runtime registration now carries max-join, total capacity, and matched member races. | Focused C# unit test. | Open quick-entry live dispatch remains missing. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 5
- Total artifacts ported or extended in this UOW: 5
- Total artifacts with verified parity: 0
- Total artifacts needing verification or remaining partial: 5
- Total blocked artifacts: 0
- Estimated overall migration completion: Phase 6 remains in progress; open quick-entry runtime gating now exists, but the live start-looking/open-registration bridge and refill lifecycle remain partial.

## Remaining Gaps

- Wire `StartLooking(...)` to try existing open quick-entry runtime instances before queue matching, matching Java `checkInstancesForOpenQuickEntries(lfp, maskId)`.
- Send window `4`, remove accepted quick-entry search entries, and apply `searchAndRemoveAdditionalRegistrations(...)` after open quick-entry attachment.
- Implement `checkQueueForQuickEntries(autoInstance)` refill after cancel/leave.
- Add instance score-ended registration rejection.
- Java `AutoPvpInstance.onPressEnter(...)` port-to-start-position remains missing.
- Java `SpawnEngine.spawnInstance(...)` and event spawn behavior remain outside the autogroup allocation bridge.

## Commit

Commit message:

```text
[Phase 6][UOW-2386] Add autogroup open quick-entry runtime gate
```
