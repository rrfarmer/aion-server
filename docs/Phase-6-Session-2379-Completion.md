# Phase 6 Session 2379 Completion - Wire Autogroup Queue Match Result Planning

## Scope

Wired the previously ported queue match planner into the successful `StartLooking` result while keeping live instance creation and packet fanout deferred.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/LookingForParty.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoInstance.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoPvpInstance.java`
- `game-server/src/com/aionemu/gameserver/services/autogroup/AutoGroupUtility.java`

Java behavior used:

- After queue registration, successful-registration fanout, and optional battleground announcement, Java calls `checkInstancesForOpenQuickEntries(lfp, maskId)`.
- If no open quick-entry instance accepts the party, Java calls `checkQueueForNewMatches(maskId)`.
- A ready queue match is only a precursor to `createNewInstance(...)`; the Java side then mutates queues, registers an auto instance, sets start-enter time, removes additional registrations, and sends window `4`.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

- Added `QueueMatchPlan` to `AutoGroupStartLookingResult`.
- `StartLooking(...)` now computes `CreateQueueMatchPlan(...)` after the queued registration and announcement planning.
- Added a focused test proving the first registration exposes a not-ready plan, the second registration exposes a ready plan, and the queue remains intact because `createNewInstance(...)` is still deferred.

Known limitations:

- This UOW does not model `checkInstancesForOpenQuickEntries`; the C# queue plan is exposed as the post-registration follow-up data, but no existing auto-instance quick-entry attachment is attempted.
- This UOW does not mutate queues, allocate/register instances, set start-enter time, remove additional registrations, or send window `4` packets.

## Validation Decision

- Changed surface: non-live autogroup service result data and focused service tests; adjacent connection tests were included to prove existing successful-registration dispatch remains unchanged.
- Specific behavior/contract: a successful registration should expose the queue match plan where Java would proceed to queue matching, while preserving existing success fanout behavior and not mutating matched queue entries yet.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~GameServerConnectionAutoGroupTests" --no-restore
```

Result: passed 36, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

- Focused Java/Maven command: skipped. No targeted Java fixture exists for this `AutoGroupService.startLooking` follow-up branch; Java source review identified branch order and deferred side effects.
- Broad-validation trigger: none. This UOW did not change live connection dispatch, packet primitives, shared instance runtime state, persistence, or scheduling.
- Broad .NET decision: skipped. The focused filtered command compiled affected projects and covered service result behavior plus adjacent ingress success-fanout behavior.
- Why this scope is sufficient: the edit only exposes a planner result and leaves live side effects untouched; focused tests assert the Java-derived placement and non-mutating boundary.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.AutoGroupService.startLooking(...)` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService.StartLooking(...)` | Service | Partial | Unit Tested | Partial Parity | Successful registration now exposes queue follow-up planning after registration/announcement. Open quick-entry checks, instance creation, and window `4` fanout remain missing. |
| `com.aionemu.gameserver.services.AutoGroupService.checkQueueForNewMatches(...)` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService.CreateQueueMatchPlan(...)` / `AutoGroupStartLookingResult.QueueMatchPlan` | Service/Planner | Partial | Unit Tested | Partial Parity | Plan is now attached to successful start-looking results. Live mutation and instance creation remain deferred. |
| `com.aionemu.gameserver.model.autogroup.LookingForParty` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistration` | DTO/Queue Entry | Partial | Unit Tested | Partial Parity | Queue entries preserve member ids, race, entry type, leader id, and registration time for result planning. Java `startEnterTime` and AGPlayer class/name data remain partial. |
| `com.aionemu.gameserver.model.autogroup.AutoPvpInstance` | `Aion.GameServer.Services.AutoGroupQueueMatchPlan` | Service/Planner | Partial | Unit Tested | Partial Parity | Periodic PvP readiness is exposed through start-looking results. Runtime callbacks and live registration remain missing. |
| `com.aionemu.gameserver.services.autogroup.AutoGroupUtility` | `Aion.GameServer.Network.Aion.GameServerConnection` / `AutoGroupStartLookingResult` | Utility/Dispatch | Partial | Unit Tested | Partial Parity | Existing success fanout remains covered; window `4` ready-enter fanout remains missing. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `AutoGroupLookingPartyRegistrationServiceTests.StartLooking_ExposesQueueMatchPlanAfterSuccessfulRegistrationLikeJavaFollowUp` | Unit | Java source review | `StartLooking` exposes not-ready then ready queue plans after successful registrations, while matched entries remain queued because instance creation is deferred. | Focused C# service test. | Does not execute Java runtime or live instance creation. |
| `GameServerConnectionAutoGroupTests.ProcessPacketAsync_AutoGroupSuccessfulRegistrationSendsJavaFanoutPackets` | Unit | Prior Java source review | Existing live success fanout remains exactly the three Java registration packets. | Adjacent focused ingress test. | Queue plan is not consumed live yet. |
| `GameServerConnectionAutoGroupTests.ProcessPacketAsync_AutoGroupSuccessfulRegistrationFansOutOnlyToOnlineMembersLikeJava` | Unit | Prior Java source review | Existing online-member filtering remains unchanged. | Adjacent focused ingress test. | No ready-enter window `4` fanout yet. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 5
- Total artifacts ported or extended in this UOW: 5
- Total artifacts with verified parity: 0
- Total artifacts needing verification or remaining partial: 5
- Total blocked artifacts: 0
- Estimated overall migration completion: Phase 6 remains in progress; autogroup queue follow-up is now exposed but live lifecycle parity remains partial.

## Remaining Gaps

- Java `checkInstancesForOpenQuickEntries(lfp, maskId)` remains missing.
- Java `createNewInstance(...)` remains missing, including instance allocation, auto-instance registry update, queue removals, `startEnterTime`, additional-registration cleanup, and window `4` fanout.
- Harmony, FFA/solo/glory, and recruitable auto-instance matching remain partial or missing.
- Java penalties, quick-entry refill, cancel-enter delayed removal, and full auto-instance lifecycle remain missing.
- Periodic registration real cron callback scheduling and close task handles remain partial.

## Commit

Commit message:

```text
[Phase 6][UOW-2379] Wire autogroup queue match result planning
```
