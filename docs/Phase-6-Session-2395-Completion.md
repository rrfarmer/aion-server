# Phase 6 Session 2395 Completion - Model Autogroup Start-Enter Logout Cancellation

## Scope
- Modeled Java `AutoGroupService.onLogout(...)` start-enter handling as a registration cleanup planner/result slice.
- Kept live `GameServerConnection` dispatch deferred; this UOW exposes cancel-enter intents but does not call runtime `CancelEnter(...)` during logout.

## Java Source Reviewed
- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
  - `onLogout(Player player)`
  - `cancelEnter(Player player, int instanceMaskId)`
  - quick-entry paths that call `LookingForParty.setStartEnterTime()`
- `game-server/src/com/aionemu/gameserver/model/autogroup/LookingForParty.java`
  - `setStartEnterTime()`
  - `isOnStartEnterTask()`

## Implemented
- Added nullable `StartEnterTime` to `AutoGroupLookingPartyRegistration`.
- Added `AutoGroupLookingPartyRegistration.IsOnStartEnterTask(DateTimeOffset now)` with Java's inclusive `<= 120000` ms window.
- Extended `RegisterLookingParty(...)` with an optional `startEnterTime` test/planner hook.
- Extended `CleanupSearchEntriesOnLogout(...)` with optional active auto-instance mask IDs and an injectable clock.
- Added `AutoGroupLogoutStartEnterCancelIntent` and `AutoGroupLogoutSearchCleanupType.StartEnterCancelEnter`.
- Start-enter logout entries now:
  - plan one cancel-enter intent for each supplied active auto-instance mask;
  - skip leader promotion/removal and member removal;
  - skip queue recheck planning;
  - leave the search registration untouched, matching the Java branch's delegation behavior.

## Migration Parity Table
| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.autogroup.LookingForParty` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistration` | Model | Partial | Unit Tested | Partial Parity | Start-enter timing is modeled with an injectable clock and the inclusive 120000 ms Java window. |
| `com.aionemu.gameserver.services.AutoGroupService` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService` | Service / Planner | Partial | Unit Tested | Partial Parity | Logout start-enter branch now surfaces cancel-enter intents instead of mutating search entries. Live dispatch remains deferred. |

## Tests Updated
| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `CleanupSearchEntriesOnLogout_StartEnterEntryPlansCancelEnterForActiveMasksLikeJava` | Unit | `AutoGroupService.onLogout` start-enter branch | Start-enter logout plans cancel-enter for every active mask and preserves the queued registration. | Source-reviewed branch plus C# planner assertions. | Does not execute runtime `CancelEnter(...)`. |
| `CleanupSearchEntriesOnLogout_StartEnterBoundaryIncludesExactlyTwoMinutesLikeJava` | Unit | `LookingForParty.isOnStartEnterTask()` | Exactly 120000 ms still counts as start-enter. | Pins Java's inclusive `<= 120000` boundary. | None for planner timing. |
| `CleanupSearchEntriesOnLogout_ExpiredStartEnterFallsBackToLeaderCleanupLikeJava` | Unit | `AutoGroupService.onLogout` branch ordering | Expired start-enter entries use normal leader cleanup and do not emit cancel-enter intents. | Demonstrates branch fallback after 120000 ms. | None for planner fallback. |

## Validation Decision
- Changed surface: autogroup looking-party registration model and logout cleanup planner.
- Specific behavior/contract: Java `LookingForParty.isOnStartEnterTask()` controls whether logout delegates `cancelEnter(...)` for every active `AutoInstance` instead of performing leader/member search cleanup.
- Focused C# command: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~AutoGroupInstanceLeaveRuntimeServiceTests" --no-restore`
- Focused Java/Maven command: skipped; no targeted Java fixture exists and no Java source changed.
- Broad-validation trigger: none; this UOW is planner/result modeling only and did not wire live connection dispatch.
- Why this scope is sufficient: focused service tests cover the new timing predicate, intent emission, non-mutation behavior, and expired-window fallback while adjacent cancel-enter runtime tests remain in the focused filter.

## Validation Result
- Passed: 56 tests, 0 failed, 0 skipped.
- Pre-existing nullable/analyzer warnings remain in unrelated files.

## Known Remaining Gaps
- Live logout does not yet consume `StartEnterCancelIntents`.
- Runtime auto-instance mask enumeration is not exposed to `GameServerConnection` for Java-style `for (AutoInstance autoInstance : autoInstances.values())`.
- Java `onLogout` auto-instance `destroyIfPossible(autoInstance)` branch remains incomplete.
- Overall Java `PlayerLeaveWorldService.leaveWorld(...)` ordering is only partially represented in C#.

## Summary Metrics
- Total Java artifacts discovered in this UOW: 2.
- Total artifacts ported or extended in this UOW: 2 C# surfaces.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification / partial parity: 2.
- Total blocked artifacts: 0.
- Estimated overall Phase 6 completion: unchanged materially; this is a narrow autogroup logout planner slice.
