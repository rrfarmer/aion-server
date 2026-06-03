# Phase 6 Session 2402 Completion - Recursive Ready-Match Recheck

## Scope
- Implemented Java-style recursive queue recheck after additional-registration member cleanup during ready-match dispatch.
- Added connection-level parity coverage for the case where removing a matched player from another queued party makes that other mask ready immediately.

## Java Source Reviewed
- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
  - `searchAndRemoveAdditionalRegistrations(int objectId)`
  - `checkQueueForNewMatches(int maskId)`
  - `createNewInstance(AutoInstance autoInstance, AutoGroupType agt, List<LookingForParty> filteredParties, int maskId)`

## Implemented
- Added an optional `afterCleanupWindowDeliveryAsync` hook to `AutoGroupLookingPartyRegistrationService.ApplyReadyMatchPlanAsync(...)`.
- `GameServerConnection.ApplyAutoGroupReadyMatchPlanAsync(...)` now uses that hook to run `CreateQueueMatchPlan(...)` for cleanup intents with `WouldRecheckQueueForNewMatches`.
- Added a per-dispatch visited-mask guard to avoid runaway recursive rechecks.
- Updated ready-match dispatch callers to pass `InstanceCooltimeTable` so nested rechecks can use the same Java capacity source as normal queue matching.
- Added `LeavePlayerWorldAsync_AutoGroupLogoutMemberCleanupRechecksQueueAndCreatesNestedReadyMatchLikeJava`.

## Migration Parity Table
| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.AutoGroupService` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService` | Service | Partial | Unit Tested | Partial Parity | Ready-match apply now exposes a cleanup delivery hook so the connection layer can mirror Java's nested `checkQueueForNewMatches(maskId)` call after member cleanup. |
| `com.aionemu.gameserver.services.AutoGroupService` | `Aion.GameServer.Network.Aion.GameServerConnection` | Connection dispatch | Partial | Regression Tested | Partial Parity | Connection ready-match dispatch now applies recursive rechecks for member cleanup masks and preserves cancel-before-nested-ready-before-original-ready packet ordering. |

## Tests Added
| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `LeavePlayerWorldAsync_AutoGroupLogoutMemberCleanupRechecksQueueAndCreatesNestedReadyMatchLikeJava` | Regression | Java source review of `searchAndRemoveAdditionalRegistrations`, `checkQueueForNewMatches`, and `createNewInstance` | Removing `1001` from mask `108` triggers a nested ready match for `3001`/`4001`, sends cancel window `2` before nested ready windows, then resumes original mask `107` ready windows. | Focused C# connection test with Java-derived packet ordering, queue removal, penalty refresh, and runtime registration assertions. | Recursive rechecks beyond one nested mask are guarded, not exhaustively stress-tested. |

## Validation Decision
- Changed surface: production connection dispatch, service callback hook, and connection regression test.
- Specific behavior/contract: member cleanup from an additional registration invokes Java-style `checkQueueForNewMatches(maskId)` before the original ready window continues.
- Focused C# command: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests" --no-restore`
- Focused Java/Maven command: skipped; no targeted Java fixture exists and no Java source changed.
- Broad-validation trigger: live connection dispatch and scheduler/runtime side effects; addressed with focused connection plus adjacent service tests rather than unfiltered project validation.
- Broad .NET decision: skipped after focused validation; no shared packet primitive, persistence, or infrastructure changed.
- Why this scope is sufficient: the new connection test exercises the live logout, cleanup, recursive recheck, scheduler, packet dispatch, and runtime-registration path together, while adjacent service tests guard the underlying cleanup intent contract.

## Validation Result
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionAutoGroupTests" --no-restore`
  - Passed: 22 tests, 0 failed, 0 skipped.
  - Pre-existing nullable/analyzer warnings were emitted during the compile-producing run.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests" --no-restore`
  - Passed: 66 tests, 0 failed, 0 skipped.

## Known Remaining Gaps
- Multiple queued quick-entry candidates and failed quick-entry refill capacity ordering remain untested in the logout/open-runtime path.
- The recheck guard is intentionally conservative and prevents repeated rechecks for the same mask within one dispatch chain.
- Overall Java `PlayerLeaveWorldService.leaveWorld(...)` ordering remains partial outside autogroup.

## Summary Metrics
- Total Java artifacts reviewed in this UOW: 1.
- Total C# artifacts changed in this UOW: 3.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification / partial parity: 2.
- Total blocked artifacts: 0.
- Estimated overall migration completion: Phase 6 remains in progress; no estimate change.
