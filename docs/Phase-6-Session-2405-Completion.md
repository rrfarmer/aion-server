# Phase 6 Session 2405 Completion - Nested Recheck Penalty Scheduling Order

## Scope
- Audited Java penalty scheduling order for additional-registration cleanup during ready-match dispatch.
- Moved C# cleanup penalty refresh scheduling for connection-driven ready matches into the cleanup delivery callback so non-leader member cleanup schedules after the cancel window and before nested queue recheck dispatch.
- Added an event-order assertion for the nested ready-match regression.

## Java Source Reviewed
- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
  - `searchAndRemoveAdditionalRegistrations(int objectId)`
  - `penalisePlayerAndScheduleRemoval(int objectId)`
  - `penaliseParty(LookingForParty lfp)`
  - `createNewInstance(AutoInstance autoInstance, AutoGroupType agt, List<LookingForParty> filteredParties, int maskId)`

## Implemented
- `GameServerConnection.ApplyAutoGroupReadyMatchPlanAsync(...)` now schedules cleanup penalty refreshes from the cleanup callback before recursive `CreateQueueMatchPlan(...)` rechecks run.
- Added per-apply dedupe so cleanup intents scheduled immediately are not scheduled again from the final apply result.
- Added `CreateAutoGroupPenaltyRefreshIntents(...)` to translate cleanup intents back into Java-shaped penalty refresh intents at the connection boundary.
- Extended `LeavePlayerWorldAsync_AutoGroupLogoutMemberCleanupRechecksQueueAndCreatesNestedReadyMatchLikeJava` to assert cancel window, schedule, nested ready windows, then original ready windows.

## Migration Parity Table
| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.AutoGroupService` | `Aion.GameServer.Network.Aion.GameServerConnection` | Connection dispatch | Partial | Regression Tested | Partial Parity | Non-leader member cleanup now schedules penalty refresh after cancel window `2` and before nested ready recheck dispatch, matching Java's member branch order. |
| `com.aionemu.gameserver.services.AutoGroupService` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService` | Service | Partial | Unit Tested | Partial Parity | Service still reports cleanup penalty intents; connection now consumes callback timing for Java-like scheduler order. Leader-party pre-cancel penalty ordering remains a separate gap. |
| `com.aionemu.gameserver.services.AutoGroupService` | `Aion.GameServer.Services.AutoGroupPenaltyRefreshSchedulerService` | Scheduler service | Partial | Unit Tested | Partial Parity | Existing scheduler dedupe remains in place; this UOW changes when connection dispatch calls it for cleanup intents. |

## Tests Updated
| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `LeavePlayerWorldAsync_AutoGroupLogoutMemberCleanupRechecksQueueAndCreatesNestedReadyMatchLikeJava` | Regression | Java `searchAndRemoveAdditionalRegistrations` member branch calls cancel window, `penalisePlayerAndScheduleRemoval`, then `checkQueueForNewMatches` | Event order is cancel window `2`, schedule penalty refresh, nested mask `108` ready windows, then original mask `107` ready windows. | Focused connection event-order assertions using packet sends and scheduler observations. | Leader-party cleanup scheduling still occurs via callback after the first cancel delivery rather than before all cancel deliveries. |

## Validation Decision
- Changed surface: production connection dispatch scheduling order and connection regression test helper.
- Specific behavior/contract: nested ready recheck dispatch schedules the member-cleanup penalty refresh after the cleanup cancel window and before nested ready windows.
- Focused C# command: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests" --no-restore`
- Focused Java/Maven command: skipped; no targeted Java fixture exists and no Java source changed.
- Broad-validation trigger: live connection dispatch/scheduler intents; covered with focused connection/service tests.
- Broad .NET decision: skipped after focused validation.
- Why this scope is sufficient: the changed connection helper and existing nested ready-match regression exercise the exact Java member-cleanup order, while adjacent service tests preserve cleanup intent contract coverage.

## Validation Result
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests" --no-restore`
  - Passed: 69 tests, 0 failed, 0 skipped.
  - Pre-existing nullable/analyzer warnings were emitted during the compile-producing run.

## Known Remaining Gaps
- Leader-party cleanup penalty scheduling is still not separately pinned to Java's exact pre-cancel ordering.
- Recursive recheck stress cases with repeated same-mask cascades remain guarded but not exhaustively modeled.
- Overall Java `PlayerLeaveWorldService.leaveWorld(...)` ordering remains partial outside autogroup.

## Summary Metrics
- Total Java artifacts reviewed in this UOW: 1.
- Total C# artifacts changed in this UOW: 2.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification / partial parity: 3.
- Total blocked artifacts: 0.
- Estimated overall migration completion: Phase 6 remains in progress; no estimate change.
