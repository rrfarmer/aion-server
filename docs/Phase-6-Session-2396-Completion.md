# Phase 6 Session 2396 Completion - Wire Autogroup Start-Enter Logout Cancellation

## Scope
- Wired Java `AutoGroupService.onLogout(...)` start-enter `cancelEnter(...)` delegation into live C# logout.
- Kept the remaining Java `destroyIfPossible(autoInstance)` logout branch deferred.

## Java Source Reviewed
- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
  - `onLogout(Player player)`
  - `cancelEnter(Player player, int instanceMaskId)`
  - `getAutoInstance(Player player, int instanceMaskId)`

## Implemented
- Added `AutoGroupInstanceLeaveRuntimeService.GetActiveInstanceMaskIds()` as the C# snapshot equivalent for Java `autoInstances.values()` mask iteration.
- `GameServerConnection.LeavePlayerWorldAsync(...)` now:
  - passes active runtime instance masks into `CleanupSearchEntriesOnLogout(...)`;
  - applies each start-enter cancel intent through live runtime `CancelEnter(...)`;
  - schedules existing cancel-enter penalty refresh intents;
  - sends Java-style `SM_AUTO_GROUP(mask, 2)` cancel windows when the player was registered in the matching runtime instance;
  - leaves the start-enter looking-party registration untouched and avoids queue-recheck cleanup for that entry.
- Extracted `ApplyAutoGroupCancelEnterAsync(...)` so client window-103 cancel-enter and logout-driven cancel-enter share the same runtime, quick-refill, penalty-refresh, and cancel-window behavior.

## Migration Parity Table
| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.AutoGroupService` | `Aion.GameServer.Network.Aion.GameServerConnection` | Connection / Logout Handler | Partial | Integration Tested | Partial Parity | Logout now consumes start-enter cancel intents and applies runtime cancel-enter side effects. Auto-instance `destroyIfPossible(autoInstance)` after search cleanup remains incomplete. |
| `com.aionemu.gameserver.services.AutoGroupService` | `Aion.GameServer.Services.AutoGroupInstanceLeaveRuntimeService` | Runtime Service | Partial | Unit Tested | Partial Parity | Runtime now exposes active auto-instance mask snapshots for Java `autoInstances.values()` logout iteration. Full Java map/value ordering is not behaviorally asserted beyond mask coverage. |

## Tests Updated
| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GetActiveInstanceMaskIds_ReturnsRegisteredAutoInstanceMasksLikeJavaValuesLoop` | Unit | Java `AutoGroupService.onLogout` `autoInstances.values()` loop | Runtime exposes active auto-instance mask IDs used to plan logout cancel-enter calls. | Source-reviewed Java iteration plus C# runtime assertion. | Does not assert Java `HashMap` ordering because the behavior only requires attempting each active mask. |
| `LeavePlayerWorldAsync_StartEnterLogoutConsumesCancelEnterIntentsLikeJava` | Integration | Java `onLogout` start-enter branch -> `cancelEnter` -> `penalisePlayerAndScheduleRemoval` -> `SM_AUTO_GROUP(mask, 2)` | Logout start-enter path unregisters the player from the matching runtime instance, schedules penalty refresh, sends cancel window 2, preserves the search registration, and ignores unrelated active masks where the player is not registered. | Source-reviewed Java branch with live C# connection/runtime/scheduler assertions. | Does not cover quick-entry refill during logout cancel-enter; that remains covered through the shared client cancel-enter path. |

## Validation Decision
- Changed surface: live connection dispatch, runtime state snapshot, and scheduler intents.
- Specific behavior/contract: Java logout start-enter entries call `cancelEnter(player, maskId)` for every active auto instance; only a matching registered runtime instance unregisters the player, schedules penalty refresh, refills quick entries if applicable, and sends `SM_AUTO_GROUP(mask, 2)`.
- Focused C# command: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~AutoGroupInstanceLeaveRuntimeServiceTests" --no-restore`
- Focused Java/Maven command: skipped; no targeted Java fixture exists and no Java source changed.
- Broad-validation trigger: live connection dispatch and scheduler intents.
- Broad .NET decision: skipped after focused validation; the changed live path is isolated to autogroup logout/cancel-enter and covered by connection-level autogroup tests plus adjacent registration/runtime service tests.
- Why this scope is sufficient: the refactored cancel-enter helper reuses the existing packet-handler behavior, and the new logout test proves the previously missing Java start-enter side effect at the connection/runtime/scheduler boundary.

## Validation Result
- Passed: 74 tests, 0 failed, 0 skipped.
- Pre-existing nullable/analyzer warnings remain in unrelated files.

## Known Remaining Gaps
- Java `onLogout` auto-instance `destroyIfPossible(autoInstance)` branch remains incomplete after search cleanup.
- Quick-entry refill during logout-driven cancel-enter relies on the shared cancel-enter helper and existing client cancel-enter coverage; no separate logout refill test exists.
- Overall Java `PlayerLeaveWorldService.leaveWorld(...)` ordering is only partially represented in C#.

## Summary Metrics
- Total Java artifacts discovered in this UOW: 1.
- Total artifacts ported or extended in this UOW: 2 C# surfaces.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification / partial parity: 2.
- Total blocked artifacts: 0.
- Estimated overall Phase 6 completion: unchanged materially; this is a narrow autogroup logout live-dispatch slice.
