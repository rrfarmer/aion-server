# Phase 6 Session 2393 Handoff

## Current Phase
- Phase 6: Port Game Core.

## Last Completed UOW
- `UOW-2393`: Wired autogroup queued-search logout cleanup into `GameServerConnection.LeavePlayerWorldAsync(...)`.

## Files Changed
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionAutoGroupTests.cs`
- `docs/Phase-6-Session-2393-Completion.md`
- `docs/Phase-6-Session-2393-Handoff.md`

## Java Artifacts Touched
- `com.aionemu.gameserver.services.player.PlayerLeaveWorldService`
- `com.aionemu.gameserver.services.AutoGroupService`

## C# Artifacts Touched
- `Aion.GameServer.Network.Aion.GameServerConnection`
- `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService`
- `Aion.GameServer.Tests.GameServerConnectionAutoGroupTests`

## What Changed
- `LeavePlayerWorldAsync(...)` now invokes autogroup queued-search cleanup when `GameServerOptions.AutoGroup.Enabled` is true.
- Disabled autogroup config skips the cleanup, matching Java's `AutoGroupConfig.AUTO_GROUP_ENABLE` guard.
- Focused connection tests prove live cleanup removes a queued logging-out member and does not send autogroup cancel windows or schedule penalty refreshes.

## Tests Run
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests" --no-restore`
- Result: 56 passed, 0 failed, 0 skipped.
- Java/Maven: skipped because no targeted Java fixture exists and no Java source changed.
- Broad .NET: skipped after focused validation; broad trigger was connection logout dispatch, but the hook was isolated and directly covered by connection-level autogroup tests.

## Migration Parity Table
| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.player.PlayerLeaveWorldService` | `Aion.GameServer.Network.Aion.GameServerConnection` | Connection / Logout Handler | Partial | Integration Tested | Partial Parity | Autogroup queued-search logout cleanup is now called behind the enabled guard. Overall leave-world ordering and many Java logout services remain incomplete. |
| `com.aionemu.gameserver.services.AutoGroupService` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService` | Service | Partial | Unit Tested | Partial Parity | Queued search cleanup is live. Queue-recheck application, start-enter cancellation, and destroy-if-possible branches remain incomplete. |

## Known Gaps
- Queue-recheck plans from logout member cleanup are not applied into ready-match dispatch.
- Java `LookingForParty.isOnStartEnterTask()` and the logout `cancelEnter` branch are not modeled for queued entries.
- Java auto-instance `destroyIfPossible` on logout remains incomplete.
- Overall `PlayerLeaveWorldService.leaveWorld(...)` has many Java service calls still outside the C# leave-world path.

## Next Recommended UOW
- `UOW-2394`: Apply logout member cleanup queue-recheck plans through the existing ready-match/live dispatch path, or add a planner adapter first if direct live dispatch is too wide.

## Suggested Discovery For UOW-2394
- Java:
  - `AutoGroupService.onLogout(Player player)`
  - `AutoGroupService.checkQueueForNewMatches(int maskId)`
  - `AutoGroupService.createNewInstance(...)`
- C#:
  - `AutoGroupLookingPartyRegistrationService.CleanupSearchEntriesOnLogout(...)`
  - `AutoGroupLookingPartyRegistrationService.CreateReadyMatchPlan(...)`
  - `AutoGroupLookingPartyRegistrationService.ApplyReadyMatchPlanAsync(...)`
  - `GameServerConnection.HandleAutoGroupAsync(...)` ready-match dispatch block for reuse/extraction

## Focused Validation Recipe For Next UOW
- Specific behavior to validate: logout member cleanup that makes a queue eligible applies Java-style ready-match dispatch for that mask, including runtime registration, ready windows, cleanup windows, and penalty refresh intents for additional-registration cleanup only.
- Focused C# command:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests" --no-restore`
- Focused Java/Maven command: not expected unless a targeted Java fixture is added; use Java source review by default.
- Broad-validation trigger: live connection dispatch and scheduler intents if ready-match application is enabled.

## Safe Candidate UOWs
- Model Java `onLogout` start-enter `cancelEnter` delegation as a planner before live wiring.
- Add tests for stop-registration close behavior confirming it sends cancel windows without penalty refresh intents.
- Improve cancel-enter destroy-if-possible modeling if online-inside-player facts become available.
