# Phase 6 Session 2392 Completion - Model Autogroup Logout Search Cleanup

## Scope
- Ported the search-entry cleanup branch of Java `AutoGroupService.onLogout(Player player)` into `AutoGroupLookingPartyRegistrationService`.
- Kept this UOW non-live: no `GameServerConnection` logout/disconnect wiring was added.

## Java Source Reviewed
- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
  - `onLogout(Player player)`
  - `getSearchEntries(int playerObjectId)`
  - `removeSearchEntry(LookingForParty lfp)`
  - `checkQueueForNewMatches(int maskId)`
- `game-server/src/com/aionemu/gameserver/model/autogroup/LookingForParty.java`
  - `setLeaderObjId(int leaderObjId)`
  - `unregisterMember(Integer objectId)`
  - `isOnStartEnterTask()`

## Implemented
- Added `CleanupSearchEntriesOnLogout(...)`.
  - Removes a leader-only search entry when no replacement leader exists.
  - Promotes the first remaining member when the logging-out player is the leader, preserving Java's behavior of not removing that old leader from the member map.
  - Removes non-leader members from their search entry.
  - Emits queue-recheck plans for member removals, mirroring Java's `checkQueueForNewMatches(lfp.getMaskId())` call.
  - Emits no penalty refresh intents; Java logout search cleanup does not call `penalisePlayerAndScheduleRemoval`.
- Added result DTOs for logout cleanup status, per-entry cleanup evidence, and queue recheck plans.

## Migration Parity Table
| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.AutoGroupService` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService` | Service | Partial | Unit Tested | Partial Parity | Java `onLogout` search-entry cleanup is modeled for leader-only removal, leader promotion, and member unregister. Start-enter `cancelEnter` and auto-instance `destroyIfPossible` branches remain outside this non-live slice. |
| `com.aionemu.gameserver.model.autogroup.LookingForParty` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistration` | DTO | Partial | Unit Tested | Partial Parity | C# preserves Java's leader-promotion quirk where the old leader remains in member ids after `setLeaderObjId`; start-enter timing is not represented on queued search entries. |

## Tests Updated
| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `AutoGroupLookingPartyRegistrationServiceTests.CleanupSearchEntriesOnLogout_LeaderOnlyPartyIsRemovedLikeJavaOnLogout` | Unit | Java `onLogout` leader branch with no replacement leader | Removes the search entry and does not request queue recheck. | Source-reviewed Java branch with focused state assertions. | Live logout hook not wired. |
| `AutoGroupLookingPartyRegistrationServiceTests.CleanupSearchEntriesOnLogout_LeaderPromotesFirstRemainingMemberWithoutRemovingOldLeaderLikeJava` | Unit | Java `setLeaderObjId(first non-logging-out member)` | Promotes the first remaining member and keeps the old leader in member ids. | Source-reviewed Java branch plus follow-up cancel assertion proving the promoted leader owns the entry. | Java `HashMap` member ordering is not deterministic; C# uses current registration order for the first remaining member. |
| `AutoGroupLookingPartyRegistrationServiceTests.CleanupSearchEntriesOnLogout_MemberRemovalPlansQueueRecheckLikeJavaOnLogout` | Unit | Java non-leader `unregisterMember` branch | Removes the logging-out member and emits a queue recheck plan for the mask. | Source-reviewed Java branch with result/state assertions. | The recheck is planned, not applied into ready-match live dispatch. |
| `AutoGroupLookingPartyRegistrationServiceTests.CleanupSearchEntriesOnLogout_MissingSearchEntryIsNoOpLikeJava` | Unit | Java `getSearchEntries` empty loop | Missing search entries leave state unchanged. | Source-reviewed Java branch with focused no-op assertions. | None for this branch. |

## Validation Decision
- Changed surface: non-live service/result shape plus tests.
- Specific behavior/contract: Java `AutoGroupService.onLogout` search-entry cleanup removes/promotes queued `LookingForParty` state and requests queue recheck only for member removals.
- Focused C# command: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests" --no-restore`
- Focused Java/Maven command: skipped; no targeted Java fixture exists and no Java source changed.
- Broad-validation trigger: none.
- Broad .NET decision: skipped; focused service tests compile the affected project and directly exercise the edited service behavior.
- Why this scope is sufficient: the UOW does not enable live logout side effects, persistence, scheduler, packet, or connection dispatch; the changed contract is fully contained in the edited service and test class.

## Validation Result
- Passed: 41 tests, 0 failed, 0 skipped.
- Pre-existing nullable/analyzer warnings remain in unrelated files.

## Known Remaining Gaps
- `GameServerConnection` does not yet call `CleanupSearchEntriesOnLogout(...)` from the live logout/disconnect path.
- Java `onLogout` start-enter branch delegates to `cancelEnter`; C# live logout wiring still needs to decide how to combine search cleanup with `AutoGroupInstanceLeaveRuntimeService.CancelEnter`.
- Java `onLogout` auto-instance `destroyIfPossible(autoInstance)` branch remains incomplete.
- Member ordering during Java leader promotion comes from `HashMap.keySet().stream().findFirst()` and is not deterministic; C# currently uses registration order.

## Summary Metrics
- Total Java artifacts discovered in this UOW: 2.
- Total artifacts ported or extended in this UOW: 2 C# surfaces.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification / partial parity: 2.
- Total blocked artifacts: 0.
- Estimated overall Phase 6 completion: unchanged materially; this is a narrow autogroup logout-search cleanup slice.
