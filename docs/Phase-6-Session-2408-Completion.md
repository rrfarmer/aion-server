# Phase 6 Session 2408 Completion - Offline Cleanup Recipient Scheduling

## Scope
- Audited Java online-gated window delivery for autogroup cleanup.
- Added focused C# coverage for additional-registration cleanup where one removed party member is offline.
- Confirmed failed cleanup-window delivery does not block queue cleanup, penalty scheduling, or ready-window dispatch for online matched members.

## Java Source Reviewed
- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
  - `searchAndRemoveAdditionalRegistrations(int objectId)`
  - `penaliseParty(LookingForParty lfp)`
  - `penalisePlayerAndScheduleRemoval(int objectId)`
- `game-server/src/com/aionemu/gameserver/services/autogroup/AutoGroupUtility.java`
  - `sendWindowToPlayerIfOnline(int objectId, int maskId, int windowId)`

## Implemented
- Added `LeavePlayerWorldAsync_AutoGroupLogoutQueueRecheckOfflineCleanupRecipientStillSchedulesLikeJava`.
- The regression keeps cleanup member `3001` offline while `1001` remains online:
  - both cleanup penalty refreshes are scheduled;
  - only online `1001` receives cancel window `2`;
  - online matched members still receive ready window `4`;
  - cleaned additional registrations are removed from the search queue.
- No production code changed in this UOW.

## Migration Parity Table
| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.AutoGroupService` | `Aion.GameServer.Network.Aion.GameServerConnection` | Connection dispatch | Partial | Regression Tested | Partial Parity | Offline cleanup-window recipients are now covered: failed packet delivery does not block penalty scheduling or ready-window dispatch. |
| `com.aionemu.gameserver.services.autogroup.AutoGroupUtility` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService` / `IGameClientConnectionRegistry` | Utility boundary | Partial | Regression Tested | Partial Parity | Java `sendWindowToPlayerIfOnline` packet gating is modeled through registry send failure; side effects continue around the failed send. |

## Tests Updated
| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `LeavePlayerWorldAsync_AutoGroupLogoutQueueRecheckOfflineCleanupRecipientStillSchedulesLikeJava` | Regression | Java `sendWindowToPlayerIfOnline` only sends when `World.getInstance().getPlayer(objectId)` returns non-null. | Offline cleanup recipient misses cancel window, but cleanup removal, penalty scheduling for all removed party members, and ready windows for online matched members still occur. | Focused connection regression using registry send failure and scheduler observations. | Does not execute delayed refresh after the offline player remains offline. |

## Validation Decision
- Changed surface: test-only connection regression coverage.
- Specific behavior/contract: cleanup intents still remove/search/schedule by Java rules when one cleanup-window recipient is offline and packet send returns false.
- Focused C# command: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests" --no-restore`
- Focused Java/Maven command: skipped; no targeted Java fixture exists and no Java source changed.
- Broad-validation trigger: none; this UOW changed only one focused test class.
- Broad .NET decision: skipped.
- Why this scope is sufficient: the new regression directly exercises the connection registry send-failure branch while adjacent registration-service coverage preserves cleanup intent contract behavior.

## Validation Result
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests" --no-restore`
  - Passed: 71 tests, 0 failed, 0 skipped.
  - Pre-existing nullable/analyzer warnings were emitted during the compile-producing run.

## Known Remaining Gaps
- Delayed refresh execution for a still-offline player is covered by scheduler behavior generally, but not in this autogroup cleanup scenario.
- Open-registration refresh packets after auto-group leave/cancel flows still need audit.
- Overall Java `PlayerLeaveWorldService.leaveWorld(...)` ordering remains partial outside autogroup.

## Summary Metrics
- Total Java artifacts reviewed in this UOW: 2.
- Total C# artifacts changed in this UOW: 1 test class.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification / partial parity: 2.
- Total blocked artifacts: 0.
- Estimated overall migration completion: Phase 6 remains in progress; no estimate change.
