# Phase 6 Session 2387 Completion - Wire Autogroup Open Quick-Entry Start-Looking

## Scope

Wired Java `AutoGroupService.startLooking(...)` open quick-entry attachment into the C# start-looking flow.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoInstance.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoPvpInstance.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/LookingForParty.java`

Java behavior used:

- `startLooking(...)` adds the `LookingForParty`, sends successful registration/fanout, optionally announces battleground registration, then calls `checkInstancesForOpenQuickEntries(lfp, maskId)`.
- If an existing auto-instance accepts the quick-entry party, Java removes the just-added search entry, sets the start-enter time, sends `SM_AUTO_GROUP(maskId, 4)` to the leader, and calls `searchAndRemoveAdditionalRegistrations(leaderObjId)`.
- If the open quick-entry check does not add the party, Java falls back to `checkQueueForNewMatches(maskId)`.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

- `AutoGroupLookingPartyRegistrationService.StartLooking(...)` now accepts an optional open quick-entry runtime callback.
- Successful quick-entry start-looking attempts can attach to an already registered runtime auto-instance before queue matching.
- Accepted open quick entries:
  - remove the just-added search entry;
  - record a ready-enter start timestamp in the returned attachment plan;
  - plan leader window `4`;
  - apply additional-registration cleanup for the leader and plan cleanup window `2` deliveries.
- Runtime rejection preserves the prior queue-match fallback path.
- `GameServerConnection` now passes the live runtime quick-entry gate into start-looking and sends the resulting open quick-entry window deliveries.
- Added focused service and live connection tests for attach, cleanup, packet order, and fallback.

Known limitations:

- `checkQueueForQuickEntries(autoInstance)` refill after cancel/leave remains missing.
- Accepted open quick-entry ready-enter timestamps are exposed in the service attachment result but not persisted as a full Java `LookingForParty` lifecycle object.
- Java penalty scheduling for cleanup remains modeled as intents only.
- Java score-ended registration rejection, instance spawn content, and press-enter port-to-start-position remain incomplete.

## Validation Decision

- Changed surface: live connection dispatch, start-looking service flow, and shared autogroup runtime state integration.
- Specific behavior/contract: Java `startLooking(...)` checks existing open quick-entry runtime instances before queue matching, sends window `4` to the accepted leader, removes the just-added search entry, cleans additional registrations, and falls back to queue matching when runtime attachment rejects.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~AutoGroupInstanceLeaveRuntimeServiceTests|FullyQualifiedName~GameServerConnectionAutoGroupTests" --no-restore
```

Result: passed 58, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

- Focused Java/Maven command: skipped. No targeted Java fixture exists for this start-looking/open quick-entry bridge; Java source review supplied the source-of-truth branch behavior.
- Broad-validation trigger: live connection dispatch and shared autogroup runtime state integration changed.
- Broad .NET decision: skipped full project/solution validation. The focused command compiled the affected project and directly covered the edited service, runtime gate, and live packet boundary.
- Why this scope is sufficient: the UOW only changed `CM_AUTO_GROUP` window `100` autogroup dispatch and the directly related service result shape; packet primitives, persistence, spawn engine, scheduler, and static-data parsing were not changed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.AutoGroupService.startLooking(...)` | `GameServerConnection.HandleAutoGroupAsync(...)` / `AutoGroupLookingPartyRegistrationService.StartLooking(...)` | Service/Connection | Partial | Unit Tested | Partial Parity | Window `100` now tries open quick-entry runtime attachment before queue matching after successful registration. Penalties and full scheduled lifecycle remain partial. |
| `com.aionemu.gameserver.services.AutoGroupService.checkInstancesForOpenQuickEntries(...)` | `AutoGroupLookingPartyRegistrationService.TryAttachOpenQuickEntry(...)` / `AutoGroupInstanceLeaveRuntimeService.TryAddOpenQuickEntry(...)` | Service/Runtime | Partial | Unit Tested | Partial Parity | Accepted quick entries remove the search entry, plan window `4`, and clean leader additional registrations. Queue refill caller remains missing. |
| `com.aionemu.gameserver.services.AutoGroupService.searchAndRemoveAdditionalRegistrations(...)` | `AutoGroupLookingPartyRegistrationService.ApplyAdditionalRegistrationCleanup(...)` | Service | Partial | Unit Tested | Partial Parity | Leader cleanup windows are exercised for open quick-entry attachment. Penalty side effects remain intent-only, and member cleanup recheck remains only modeled. |
| `com.aionemu.gameserver.model.autogroup.LookingForParty` | `AutoGroupLookingPartyRegistration` / `AutoGroupOpenQuickEntryAttachment` | Model | Partial | Unit Tested | Partial Parity | Start-enter time is represented on the attachment result for accepted open quick entries, but not as a persistent full lifecycle model. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `AutoGroupLookingPartyRegistrationServiceTests.StartLooking_AttachesQuickEntryToOpenRuntimeInstanceBeforeQueueMatchingLikeJava` | Unit | Java source review | Accepted quick entry is removed from the search queue, runtime state gains the player, leader window `4` is planned before additional cleanup windows, and leader additional registration is removed. | Focused C# unit test. | Does not execute live packet serialization. |
| `AutoGroupLookingPartyRegistrationServiceTests.StartLooking_FallsBackToQueueMatchWhenOpenRuntimeQuickEntryRejectsLikeJava` | Unit | Java source review | Runtime rejection leaves the quick-entry party queued and returns the normal queue-match plan. | Focused C# unit test. | Rejection source is modeled by runtime max-join expiry. |
| `GameServerConnectionAutoGroupTests.ProcessPacketAsync_AutoGroupQuickEntryAttachesToOpenRuntimeInstanceLikeJava` | Unit | Java source review | Live window `100` quick-entry sends successful registration fanout, then window `4`, then cleanup windows, and mutates runtime/search state. | Focused C# connection test. | Does not validate real client rendering or Java runtime execution. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported or extended in this UOW: 4
- Total artifacts with verified parity: 0
- Total artifacts needing verification or remaining partial: 4
- Total blocked artifacts: 0
- Estimated overall migration completion: Phase 6 remains in progress; open quick-entry start-looking attachment is now wired, but refill after cancel/leave and full auto-instance lifecycle remain partial.

## Remaining Gaps

- Implement `AutoGroupService.checkQueueForQuickEntries(autoInstance)` refill after cancel-enter and leave flows.
- Persist or model ready-enter/start-enter state more fully for queued refill candidates.
- Add Java penalty scheduling side effects beyond current cleanup intents.
- Add instance score-ended registration rejection.
- Java `AutoPvpInstance.onPressEnter(...)` port-to-start-position remains missing.
- Java `SpawnEngine.spawnInstance(...)` and event spawn behavior remain outside the autogroup allocation bridge.

## Commit

Commit message:

```text
[Phase 6][UOW-2387] Wire autogroup open quick-entry start-looking
```
