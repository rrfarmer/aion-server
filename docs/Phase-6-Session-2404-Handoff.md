# Phase 6 Session 2404 Handoff

## Current Phase
- Phase 6: Port Game Core.

## Last Completed UOW
- `UOW-2404`: Added quick-entry open-instance scan coverage for a first rejected runtime and later accepted runtime.

## Commits Made
- `[Phase 6][UOW-2404] Cover quick-entry open instance scan`

## Files Changed
- `dotnetConversion/tests/Aion.GameServer.Tests/AutoGroupInstanceLeaveRuntimeServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/AutoGroupLookingPartyRegistrationServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionAutoGroupTests.cs`
- `docs/Phase-6-Session-2404-Completion.md`
- `docs/Phase-6-Session-2404-Handoff.md`

## Java Artifacts Touched
- `com.aionemu.gameserver.services.AutoGroupService`
- `com.aionemu.gameserver.services.instance.AutoPvpInstance`

## C# Artifacts Touched
- `Aion.GameServer.Services.AutoGroupInstanceLeaveRuntimeService`
- `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService`
- `Aion.GameServer.Network.Aion.GameServerConnection`
- `Aion.GameServer.Tests.AutoGroupInstanceLeaveRuntimeServiceTests`
- `Aion.GameServer.Tests.AutoGroupLookingPartyRegistrationServiceTests`
- `Aion.GameServer.Tests.GameServerConnectionAutoGroupTests`

## What Changed
- Added runtime-level coverage for `TryAddOpenQuickEntry(...)` skipping a race-full matching runtime and adding to a later matching runtime.
- Added service-level `StartLooking(...)` coverage for the same Java `checkInstancesForOpenQuickEntries(...)` behavior.
- Added live `CM_AUTO_GROUP` quick-entry coverage proving successful-registration fanout precedes ready/cleanup windows and the player attaches to the later runtime instance.

## Tests Run
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~AutoGroupInstanceLeaveRuntimeServiceTests" --no-restore`
  - Result: 85 passed, 0 failed, 0 skipped.
- Java/Maven: skipped because no targeted Java fixture exists and no Java source changed.
- Broad .NET: skipped; focused runtime/service/connection filters covered the scoped risk.

## Migration Parity Table
| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.AutoGroupService` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService` | Service | Partial | Unit Tested | Partial Parity | Start-looking quick-entry now covers later-runtime attachment after an earlier matching runtime rejects. |
| `com.aionemu.gameserver.services.AutoGroupService` | `Aion.GameServer.Network.Aion.GameServerConnection` | Connection dispatch | Partial | Regression Tested | Partial Parity | Live quick-entry dispatch now covers multiple open runtime scan, registration fanout, ready window `4`, and additional cleanup windows. |
| `com.aionemu.gameserver.services.instance.AutoPvpInstance` | `Aion.GameServer.Services.AutoGroupInstanceLeaveRuntimeService` | Runtime service | Partial | Unit Tested | Partial Parity | Runtime open quick-entry add now covers rejected-first/later-accepted matching instances. |

## Known Gaps
- Recursive recheck stress cases with repeated same-mask cascades remain guarded but not exhaustively modeled.
- Penalty refresh scheduling order after nested ready rechecks has not been separately audited.
- Overall Java `PlayerLeaveWorldService.leaveWorld(...)` ordering remains partial outside autogroup.
- Replacement readiness still needs broader real-client gameplay coverage beyond this autogroup slice.

## Next Recommended UOW
- `UOW-2405`: Audit and cover penalty refresh scheduling order when nested ready rechecks occur before the original ready-match dispatch resumes.

## Suggested Discovery For UOW-2405
- Java:
  - `AutoGroupService.searchAndRemoveAdditionalRegistrations(int objectId)`
  - `AutoGroupService.penalisePlayerAndScheduleRemoval(int objectId)`
  - `AutoGroupService.penaliseParty(LookingForParty lfp)`
  - `AutoGroupService.createNewInstance(...)`
- C#:
  - `AutoGroupLookingPartyRegistrationService.ApplyReadyMatchPlanAsync(...)`
  - `GameServerConnection.ApplyAutoGroupReadyMatchPlanAsync(...)`
  - `AutoGroupPenaltyRefreshSchedulerService`
  - `GameServerConnectionAutoGroupTests`
  - `AutoGroupLookingPartyRegistrationServiceTests`

## Focused Validation Recipe For Next UOW
- Specific behavior to validate: nested ready recheck dispatch schedules penalty refresh intents for cleanup windows in Java-equivalent order, without duplicate refreshes for unaffected players and without scheduling before the relevant cleanup packet has been produced.
- Focused C# command:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests" --no-restore`
- Focused Java/Maven command: not expected unless a targeted Java fixture is added; use Java source review by default.
- Broad-validation trigger: live connection dispatch/scheduler intents if connection-level test coverage is added; still start with the focused filters above.

## Safe Candidate UOWs
- Continue broader `PlayerLeaveWorldService.leaveWorld(...)` ordering slices outside autogroup.
- Add targeted service-level stress coverage for recursive recheck same-mask guard behavior.
- Audit open-registration refresh packets after auto-group leave/cancel flows.
