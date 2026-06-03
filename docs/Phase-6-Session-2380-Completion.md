# Phase 6 Session 2380 Completion - Port Autogroup Ready Match Side-Effect Planning

## Scope

Ported a non-live ready-match side-effect planner for Java `AutoGroupService.createNewInstance(...)` follow-up behavior.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/LookingForParty.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoInstance.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoPvpInstance.java`
- `game-server/src/com/aionemu/gameserver/services/autogroup/AutoGroupUtility.java`

Java behavior used:

- When `checkQueueForNewMatches(maskId)` reaches `AGQuestion.READY`, Java calls `createNewInstance(...)`.
- `createNewInstance(...)` removes the matched queue entries, sets start-enter time, calls `searchAndRemoveAdditionalRegistrations(id)` for each matched member, then sends `SM_AUTO_GROUP(maskId, 4)` to each matched online player.
- `searchAndRemoveAdditionalRegistrations(id)` removes additional registrations differently for leaders and members:
  - leader: remove whole additional party, penalise party, notify all members with window `2`;
  - member: unregister only that member, notify that member with window `2`, penalise player, and re-check the queue.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

- Added `AutoGroupLookingPartyRegistrationService.CreateReadyMatchPlan(...)`.
- Added `AutoGroupReadyMatchPlan`, `AutoGroupReadyMatchPlanStatus`, `AutoGroupAdditionalRegistrationCleanupIntent`, and `AutoGroupAdditionalRegistrationCleanupType`.
- The ready-match plan now lists:
  - matched parties from the ready queue plan;
  - window `4` ready-enter recipient object ids;
  - additional-registration cleanup intents for leader-party removals and member removals;
  - penalty and queue-recheck intents needed by later live mutation.
- The planner simulates Java cleanup ordering on a copied queue snapshot, so matched entries are removed from the simulation before additional registrations are searched, while the live C# queues remain unchanged.

Known limitations:

- This UOW does not allocate a world instance or register an `AutoInstance`.
- This UOW does not mutate live queues, set Java `startEnterTime`, send ready-enter window `4`, send cancel window `2`, apply penalties, or re-run matching after member cleanup.
- Java `checkInstancesForOpenQuickEntries(lfp, maskId)` remains missing.

## Validation Decision

- Changed surface: non-live autogroup service planning records and focused service tests; adjacent connection tests were included to prove existing successful-registration dispatch remains unchanged.
- Specific behavior/contract: a ready queue plan should produce Java-derived ready-enter recipients and additional-registration cleanup intents without mutating the current queues.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~GameServerConnectionAutoGroupTests" --no-restore
```

Result: passed 39, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

- Focused Java/Maven command: skipped. No targeted Java fixture exists for this `AutoGroupService.createNewInstance(...)` planning slice; Java source review identified the mutation and notification ordering.
- Broad-validation trigger: none. This UOW added non-live service planning and did not change live connection dispatch, packet primitives, shared instance runtime state, persistence, or scheduling.
- Broad .NET decision: skipped. The focused filtered command compiled affected projects and covered service planning plus adjacent ingress success-fanout behavior.
- Why this scope is sufficient: the edit is intentionally non-live; focused tests assert ready recipients, leader/member cleanup semantics, and the non-mutating boundary.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.AutoGroupService.createNewInstance(...)` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService.CreateReadyMatchPlan(...)` / `AutoGroupReadyMatchPlan` | Service/Planner | Partial | Unit Tested | Partial Parity | Ready-enter recipients and additional-registration cleanup side effects are planned. Instance allocation, live queue mutation, start-enter time, and packet dispatch remain deferred. |
| `com.aionemu.gameserver.services.AutoGroupService.searchAndRemoveAdditionalRegistrations(int)` | `AutoGroupAdditionalRegistrationCleanupIntent` / planner simulation | Service/Planner | Partial | Unit Tested | Partial Parity | Leader whole-party removal and member-only removal intents are modeled with window `2`, penalty flags, and queue-recheck flag. Live mutation and penalty application remain missing. |
| `com.aionemu.gameserver.services.AutoGroupService.checkQueueForNewMatches(...)` | `AutoGroupQueueMatchPlan` / `AutoGroupReadyMatchPlan` | Service/Planner | Partial | Unit Tested | Partial Parity | Ready queue matches can now feed a non-live create-new-instance side-effect plan. |
| `com.aionemu.gameserver.model.autogroup.LookingForParty` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistration` | DTO/Queue Entry | Partial | Unit Tested | Partial Parity | Queue entries support member/leader cleanup planning. Java `startEnterTime`, AGPlayer class/name data, and live runtime state remain partial. |
| `com.aionemu.gameserver.services.autogroup.AutoGroupUtility.sendWindowToPlayerIfOnline(...)` | `AutoGroupReadyMatchPlan.ReadyWindowRecipientObjectIds` / `AutoGroupAdditionalRegistrationCleanupIntent.NotifiedMemberObjectIds` | Utility/Dispatch Planner | Partial | Unit Tested | Partial Parity | Window `4` and window `2` recipients are planned, not sent. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `AutoGroupLookingPartyRegistrationServiceTests.CreateReadyMatchPlan_PlansWindowFourRecipientsAndLeaderCleanupLikeJavaCreateNewInstance` | Unit | Java source review | Ready queue plan lists all matched members for window `4`, plans leader additional-registration whole-party cleanup with window `2`, and does not mutate live queues. | Focused C# service test. | Does not allocate instance or send packets. |
| `AutoGroupLookingPartyRegistrationServiceTests.CreateReadyMatchPlan_PlansMemberCleanupAndQueueRecheckLikeJavaSearchAndRemoveAdditionalRegistrations` | Unit | Java source review | Member additional-registration cleanup is planned as member-only removal, player penalty, window `2`, and queue recheck intent. | Focused C# service test. | Does not apply penalty or re-run matching live. |
| `AutoGroupLookingPartyRegistrationServiceTests.CreateReadyMatchPlan_NotReadyDoesNotPlanCreateNewInstanceSideEffects` | Unit | Java source review | Non-ready queue plans produce no create-new-instance side-effect plan. | Focused C# service test. | Does not cover later live instance path. |
| `GameServerConnectionAutoGroupTests.ProcessPacketAsync_AutoGroupSuccessfulRegistrationSendsJavaFanoutPackets` | Unit | Prior Java source review | Existing successful-registration live fanout remains unchanged. | Adjacent focused ingress test. | Queue plan is still not consumed live. |
| `GameServerConnectionAutoGroupTests.ProcessPacketAsync_AutoGroupSuccessfulRegistrationFansOutOnlyToOnlineMembersLikeJava` | Unit | Prior Java source review | Existing online-member filtering remains unchanged. | Adjacent focused ingress test. | No ready-enter window `4` fanout yet. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 5
- Total artifacts ported or extended in this UOW: 5
- Total artifacts with verified parity: 0
- Total artifacts needing verification or remaining partial: 5
- Total blocked artifacts: 0
- Estimated overall migration completion: Phase 6 remains in progress; autogroup ready-match side effects are now planned but live lifecycle parity remains partial.

## Remaining Gaps

- Java `createNewInstance(...)` live mutation remains missing, including instance allocation, auto-instance registry update, queue removals, `startEnterTime`, and packet fanout.
- Java `checkInstancesForOpenQuickEntries(lfp, maskId)` remains missing.
- Java penalty application and delayed cancel-enter removal remain missing.
- Quick-entry refill and full auto-instance lifecycle remain missing.
- Harmony, FFA/solo/glory, and recruitable auto-instance matching remain partial or missing.
- Periodic registration real cron callback scheduling and close task handles remain partial.

## Commit

Commit message:

```text
[Phase 6][UOW-2380] Port autogroup ready match side-effect planning
```
