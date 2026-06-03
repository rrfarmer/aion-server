# Phase 6 Session 2393 Completion - Wire Autogroup Logout Search Cleanup

## Scope
- Wired the C# queued-search cleanup from UOW-2392 into the live player leave-world path.
- Kept this UOW deliberately narrow: queue-recheck ready-match application, start-enter `cancelEnter` delegation, and auto-instance `destroyIfPossible` remain deferred.

## Java Source Reviewed
- `game-server/src/com/aionemu/gameserver/services/player/PlayerLeaveWorldService.java`
  - `leaveWorld(Player player)`
  - `if (AutoGroupConfig.AUTO_GROUP_ENABLE) AutoGroupService.getInstance().onLogout(player)`
- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
  - `onLogout(Player player)`
  - queued search-entry cleanup branches

## Implemented
- Added an autogroup-enabled guard in `GameServerConnection.LeavePlayerWorldAsync(...)`.
- When enabled, `LeavePlayerWorldAsync(...)` now calls `AutoGroupLookingPartyRegistrationService.CleanupSearchEntriesOnLogout(...)`.
- The live hook passes static autogroup and instance-cooltime data so the cleanup result can build queue-recheck plans.
- No cancel windows, penalty refresh scheduling, or ready-match dispatch are sent from this hook in this UOW.

## Migration Parity Table
| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.player.PlayerLeaveWorldService` | `Aion.GameServer.Network.Aion.GameServerConnection` | Connection / Logout Handler | Partial | Integration Tested | Partial Parity | C# leave-world now invokes autogroup queued-search cleanup behind the same enabled-config guard. Java leave-world ordering is only partially modeled in C# overall. |
| `com.aionemu.gameserver.services.AutoGroupService` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService` | Service | Partial | Unit Tested | Partial Parity | Live logout now reaches queued search cleanup; start-enter cancel-enter delegation, queue-recheck ready-match application, and auto-instance destroy-if-possible remain gaps. |

## Tests Updated
| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GameServerConnectionAutoGroupTests.LeavePlayerWorldAsync_AutoGroupEnabledCleansQueuedSearchEntryLikeJavaOnLogout` | Integration | Java `PlayerLeaveWorldService.leaveWorld` -> `AutoGroupService.onLogout` | Live leave-world cleanup removes a logging-out queued member and does not send autogroup cancel windows or schedule penalty refreshes. | Source-reviewed Java branch with connection-level state and scheduler assertions. | Does not apply Java queue recheck into ready-match dispatch. |
| `GameServerConnectionAutoGroupTests.LeavePlayerWorldAsync_AutoGroupDisabledLeavesQueuedSearchEntryLikeJavaConfigGuard` | Integration | Java `AutoGroupConfig.AUTO_GROUP_ENABLE` guard | Disabled autogroup config skips logout cleanup. | Source-reviewed Java guard with connection-level state assertion. | None for this branch. |

## Validation Decision
- Changed surface: live connection logout dispatch.
- Specific behavior/contract: Java leave-world invokes autogroup queued-search cleanup only when autogroup is enabled, and search cleanup itself sends no cancel windows or penalty refresh schedules.
- Focused C# command: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests" --no-restore`
- Focused Java/Maven command: skipped; no targeted Java fixture exists and no Java source changed.
- Broad-validation trigger: connection logout/disconnect dispatch.
- Broad .NET decision: skipped after focused validation; the changed live hook is isolated to autogroup state cleanup and covered directly by connection-level tests plus the adjacent service tests.
- Why this scope is sufficient: the hook does not touch packet primitives, persistence, shared serialization, or broad world-state mechanics beyond the existing leave-world method; focused tests verify the exact Java-derived side effect and disabled-config guard.

## Validation Result
- Passed: 56 tests, 0 failed, 0 skipped.
- Pre-existing nullable/analyzer warnings remain in unrelated files.

## Known Remaining Gaps
- Java logout member cleanup calls `checkQueueForNewMatches(maskId)`; C# currently creates queue-recheck plans but does not apply them live from logout.
- Java `onLogout` start-enter branch delegates to `cancelEnter`; C# does not yet model queued search entries with `isOnStartEnterTask()`.
- Java `onLogout` auto-instance `destroyIfPossible(autoInstance)` branch remains incomplete.
- Overall Java `PlayerLeaveWorldService.leaveWorld(...)` ordering is only partially represented in C#.

## Summary Metrics
- Total Java artifacts discovered in this UOW: 2.
- Total artifacts ported or extended in this UOW: 2 C# surfaces.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification / partial parity: 2.
- Total blocked artifacts: 0.
- Estimated overall Phase 6 completion: unchanged materially; this is a narrow autogroup logout live-hook slice.
