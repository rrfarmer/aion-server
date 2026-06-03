# Phase 6 Session 2401 Handoff

## Current Phase
- Phase 6: Port Game Core.

## Last Completed UOW
- `UOW-2401`: Added connection-level coverage for logout ready-match additional-registration member cleanup.

## Commits Made
- `[Phase 6][UOW-2401] Cover logout ready-match member cleanup`

## Files Changed
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionAutoGroupTests.cs`
- `docs/Phase-6-Session-2401-Completion.md`
- `docs/Phase-6-Session-2401-Handoff.md`

## Java Artifacts Touched
- `com.aionemu.gameserver.services.AutoGroupService`

## C# Artifacts Touched
- `Aion.GameServer.Network.Aion.GameServerConnection`
- `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService`
- `Aion.GameServer.Tests.GameServerConnectionAutoGroupTests`

## What Changed
- Added `LeavePlayerWorldAsync_AutoGroupLogoutQueueRecheckCleansAdditionalMemberRegistrationLikeJava`.
- The test covers the Java non-leader member branch of `searchAndRemoveAdditionalRegistrations(int objectId)` through the C# logout connection path.
- It proves mask `108` keeps leader `3001`, removes ready player `1001`, schedules only `1001` for penalty refresh, sends only `1001` cancel window `2`, and still dispatches ready windows plus runtime registration for the mask `107` match.

## Tests Run
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionAutoGroupTests" --no-restore`
  - Result: 21 passed, 0 failed, 0 skipped.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests" --no-restore`
  - Result: 65 passed, 0 failed, 0 skipped.
- Java/Maven: skipped because no targeted Java fixture exists and no Java source changed.
- Broad .NET: skipped; focused connection/service filters covered the live dispatch and scheduler-intent risk without changing shared infrastructure or packet primitives.

## Migration Parity Table
| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.AutoGroupService` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService` | Service | Partial | Unit Tested | Partial Parity | Additional-registration cleanup has service-level coverage for leader and non-leader member branches. Recursive recheck side effects remain a gap. |
| `com.aionemu.gameserver.services.AutoGroupService` | `Aion.GameServer.Network.Aion.GameServerConnection` | Connection dispatch | Partial | Regression Tested | Partial Parity | Logout queue recheck now covers ready-match creation, leader additional cleanup, and member additional cleanup through live dispatch and scheduler intents. |

## Known Gaps
- Recursive `checkQueueForNewMatches(maskId)` after member cleanup is not yet isolated at connection level.
- Multiple queued quick-entry candidates and failed capacity-refill ordering for open runtime quick-entry refill remain untested.
- Overall Java `PlayerLeaveWorldService.leaveWorld(...)` ordering remains partial outside autogroup.
- Replacement readiness still needs broader real-client gameplay coverage beyond this autogroup slice.

## Next Recommended UOW
- `UOW-2402`: Cover recursive member-cleanup queue recheck when removing a matched player from an additional queued party makes that additional mask ready, preserving Java's cleanup-before-ready-window ordering.

## Suggested Discovery For UOW-2402
- Java:
  - `AutoGroupService.searchAndRemoveAdditionalRegistrations(int objectId)`
  - `AutoGroupService.checkQueueForNewMatches(int maskId)`
  - `AutoGroupService.createNewInstance(...)`
- C#:
  - `AutoGroupLookingPartyRegistrationService.ApplyAdditionalRegistrationCleanup(...)`
  - `AutoGroupLookingPartyRegistrationService.CreateReadyMatchPlan(...)`
  - `AutoGroupLookingPartyRegistrationService.ApplyReadyMatchPlanAsync(...)`
  - `GameServerConnection.ApplyAutoGroupReadyMatchPlanAsync(...)`
  - `GameServerConnectionAutoGroupTests`

## Focused Validation Recipe For Next UOW
- Specific behavior to validate: member cleanup from an additional registration requests a queue recheck, and when the remaining additional queue can form a ready match, C# preserves Java-style cancel window `2`, ready window `4`, queue removal, penalty refresh, and runtime registration ordering for both the original and rechecked masks.
- Focused C# command:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests" --no-restore`
- Focused Java/Maven command: not expected unless a targeted Java fixture is added; use Java source review by default.
- Broad-validation trigger: live connection dispatch and scheduler intents if a connection-level recursive recheck test is added; still start with the focused connection/service filters.

## Safe Candidate UOWs
- Cover multiple queued quick-entry candidates and failed capacity-refill ordering for open runtime quick-entry refill.
- Continue broader `PlayerLeaveWorldService.leaveWorld(...)` ordering slices outside autogroup.
- Add a targeted service-level test if recursive queue recheck is easier to isolate before another connection-level dispatch test.
