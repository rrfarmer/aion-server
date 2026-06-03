# Phase 6 Session 2399 Completion - Logout Ready-Match Additional Cleanup Coverage

## Scope
- Added logout-specific integration evidence for Java `AutoGroupService.onLogout(...)` member cleanup flowing into `checkQueueForNewMatches(...)`.
- Verified that a logout-driven ready match applies Java `createNewInstance(...)` side effects, including `searchAndRemoveAdditionalRegistrations(id)` for a ready player with another queued registration.

## Java Source Reviewed
- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
  - `onLogout(Player player)`
  - `checkQueueForNewMatches(int maskId)`
  - `createNewInstance(AutoInstance autoInstance, AutoGroupType agt, List<LookingForParty> filteredParties, int maskId)`
  - `searchAndRemoveAdditionalRegistrations(int objectId)`

## Implemented
- Added `LeavePlayerWorldAsync_AutoGroupLogoutQueueRecheckCleansAdditionalRegistrationsLikeJava`.
- The new test logs out an overflow queued member, rechecks the queue into a ready match, allocates the auto-instance runtime/world state, removes matched registrations, removes one ready player's additional queued party, sends cleanup cancel windows before ready windows, and schedules penalty-refresh timers for the cleaned-up party.

## Migration Parity Table
| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.player.PlayerLeaveWorldService` | `Aion.GameServer.Network.Aion.GameServerConnection` | Connection / Logout Handler | Partial | Integration Tested | Partial Parity | Logout queued-member cleanup now has direct connection evidence for ready-match dispatch and additional-registration cleanup. Overall leave-world ordering remains partial. |
| `com.aionemu.gameserver.services.AutoGroupService` | `Aion.GameServer.Network.Aion.GameServerConnection` / `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService` / `Aion.GameServer.Services.AutoGroupInstanceLeaveRuntimeService` | Service / Runtime | Partial | Integration Tested | Partial Parity | `onLogout -> checkQueueForNewMatches -> createNewInstance -> searchAndRemoveAdditionalRegistrations` is covered for one leader-party cleanup case. Other logout branches remain partial. |

## Tests Updated
| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `LeavePlayerWorldAsync_AutoGroupLogoutQueueRecheckCleansAdditionalRegistrationsLikeJava` | Integration | Java `AutoGroupService.onLogout`, `checkQueueForNewMatches`, `createNewInstance`, and `searchAndRemoveAdditionalRegistrations` | Logout removes a queued member, creates a ready match, removes a ready player's extra queued party, sends cancel window `2`, sends ready window `4`, registers runtime instance state, and schedules 10-second penalty refreshes. | Source-reviewed Java branch with C# connection/search/runtime/world-state/scheduler assertions. | Covers leader-party additional cleanup only; member cleanup during connection logout remains covered by service-level tests. |

## Validation Decision
- Changed surface: connection-level autogroup logout test only.
- Specific behavior/contract: logout member cleanup must recheck the queue, create a ready match, invoke additional-registration cleanup for ready players, send cleanup cancel windows before ready windows, and schedule cleanup penalty refreshes.
- Focused C# command: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests" --no-restore`
- Focused Java/Maven command: skipped; no targeted Java fixture exists and no Java source changed.
- Broad-validation trigger: live connection dispatch and scheduler intents, but this UOW is test-only and the focused command covers the edited connection evidence plus the adjacent registration-service contract.
- Broad .NET decision: skipped after focused validation.
- Why this scope is sufficient: the new integration test exercises the Java-reviewed branch through `LeavePlayerWorldAsync(...)`, and the adjacent registration-service tests cover the ready-match cleanup mechanics it depends on.

## Validation Result
- Passed: 64 tests, 0 failed, 0 skipped.
- A narrower connection-class precheck also passed: 20 tests, 0 failed, 0 skipped.
- Pre-existing nullable/analyzer warnings appeared on the first compile-producing run; no new warnings were introduced by this test-only change.

## Known Remaining Gaps
- Logout ready-match additional cleanup through the member-removal sub-branch is still covered at service level, not by a separate connection-level test.
- Stop-registration close behavior still needs window/cancellation tests.
- Multiple queued quick-entry candidates and failed quick-entry refill capacity ordering remain untested in the logout path.
- Overall Java `PlayerLeaveWorldService.leaveWorld(...)` ordering remains partial.

## Summary Metrics
- Total Java artifacts reviewed in this UOW: 1.
- Total C# artifacts changed in this UOW: 1 test file.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification / partial parity: 2.
- Total blocked artifacts: 0.
