# Phase 6 Session 2394 Completion - Apply Autogroup Logout Queue Rechecks

## Scope
- Applied Java-style queue rechecks produced by autogroup logout member cleanup through the existing live ready-match dispatch path.
- Kept this UOW focused on queued search entries; start-enter `cancelEnter` delegation and auto-instance `destroyIfPossible` remain deferred.

## Java Source Reviewed
- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
  - `onLogout(Player player)`
  - `checkQueueForNewMatches(int maskId)`
  - `createNewInstance(...)`
  - `searchAndRemoveAdditionalRegistrations(int objectId)`

## Implemented
- Extracted `GameServerConnection.ApplyAutoGroupReadyMatchPlanAsync(...)` from the existing auto-group registration ready-match block.
- Reused that helper for both:
  - normal start-looking queue match dispatch;
  - logout cleanup queue-recheck plans.
- `LeavePlayerWorldAsync(...)` now applies ready queue-recheck plans from `CleanupSearchEntriesOnLogout(...)` when a logging-out member removal makes the queue eligible.
- Preserved existing ready-match behavior: runtime registration, instance allocation materialization, ready-window delivery, and penalty-refresh scheduling only for additional-registration cleanup intents.

## Migration Parity Table
| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.AutoGroupService` | `Aion.GameServer.Network.Aion.GameServerConnection` | Connection / Logout Handler | Partial | Integration Tested | Partial Parity | Logout member cleanup now applies ready queue-recheck plans through the same live ready-match path used by registration. Start-enter and auto-instance destroy branches remain incomplete. |
| `com.aionemu.gameserver.services.AutoGroupService` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService` | Service | Partial | Unit Tested | Partial Parity | Queue-recheck plans from logout cleanup are now consumed live when ready. Other `onLogout` branches remain partial. |

## Tests Updated
| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GameServerConnectionAutoGroupTests.LeavePlayerWorldAsync_AutoGroupLogoutQueueRecheckAppliesReadyMatchLikeJava` | Integration | Java `onLogout` member branch -> `checkQueueForNewMatches` -> `createNewInstance` | Removing an over-capacity logout member makes a queue ready, sends ready windows, registers the runtime instance, and avoids penalty refreshes for the logout removal itself. | Source-reviewed Java branch with focused C# connection/runtime assertions. | Does not cover additional-registration cleanup during logout-triggered ready match. |

## Validation Decision
- Changed surface: live connection dispatch and ready-match scheduling intent path.
- Specific behavior/contract: Java logout member cleanup calls `checkQueueForNewMatches(maskId)` and can create a ready autogroup instance after the logging-out member is removed.
- Focused C# command: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests" --no-restore`
- Focused Java/Maven command: skipped; no targeted Java fixture exists and no Java source changed.
- Broad-validation trigger: live connection dispatch and scheduler intents.
- Broad .NET decision: skipped after focused validation; the changed live path is isolated to autogroup ready-match dispatch and covered directly by connection-level autogroup tests plus adjacent service tests.
- Why this scope is sufficient: the helper reuses the existing ready-match implementation, and the new test proves the previously missing Java logout queue-recheck side effect at the connection/runtime boundary.

## Validation Result
- Passed: 57 tests, 0 failed, 0 skipped.
- Pre-existing nullable/analyzer warnings remain in unrelated files.

## Known Remaining Gaps
- Java `LookingForParty.isOnStartEnterTask()` and the logout `cancelEnter` branch are not modeled for queued entries.
- Java `onLogout` auto-instance `destroyIfPossible(autoInstance)` branch remains incomplete.
- Additional-registration cleanup during a logout-triggered ready match uses the shared ready-match path but has no separate logout-specific integration test.
- Overall Java `PlayerLeaveWorldService.leaveWorld(...)` ordering is only partially represented in C#.

## Summary Metrics
- Total Java artifacts discovered in this UOW: 1.
- Total artifacts ported or extended in this UOW: 2 C# surfaces.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification / partial parity: 2.
- Total blocked artifacts: 0.
- Estimated overall Phase 6 completion: unchanged materially; this is a narrow autogroup logout queue-recheck slice.
