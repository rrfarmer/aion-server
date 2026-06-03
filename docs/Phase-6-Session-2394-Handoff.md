# Phase 6 Session 2394 Handoff

## Current Phase
- Phase 6: Port Game Core.

## Last Completed UOW
- `UOW-2394`: Applied autogroup logout queue-recheck plans through the live ready-match dispatch path.

## Files Changed
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionAutoGroupTests.cs`
- `docs/Phase-6-Session-2394-Completion.md`
- `docs/Phase-6-Session-2394-Handoff.md`

## Java Artifacts Touched
- `com.aionemu.gameserver.services.AutoGroupService`

## C# Artifacts Touched
- `Aion.GameServer.Network.Aion.GameServerConnection`
- `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService`
- `Aion.GameServer.Tests.GameServerConnectionAutoGroupTests`

## What Changed
- Extracted the existing ready-match application logic into `ApplyAutoGroupReadyMatchPlanAsync(...)`.
- `LeavePlayerWorldAsync(...)` now applies ready queue-recheck plans from autogroup logout cleanup.
- A focused connection test proves that logging out an over-capacity member can make the queue ready, send ready windows, register the runtime instance, and avoid penalty refreshes for the logout member cleanup.

## Tests Run
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests" --no-restore`
- Result: 57 passed, 0 failed, 0 skipped.
- Java/Maven: skipped because no targeted Java fixture exists and no Java source changed.
- Broad .NET: skipped after focused validation; broad trigger was live connection dispatch and scheduler intents, but the changed path was isolated and directly covered by focused autogroup connection/service tests.

## Migration Parity Table
| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.AutoGroupService` | `Aion.GameServer.Network.Aion.GameServerConnection` | Connection / Logout Handler | Partial | Integration Tested | Partial Parity | Logout member queue rechecks can now apply ready-match dispatch. Start-enter `cancelEnter` and auto-instance destroy-if-possible branches remain incomplete. |
| `com.aionemu.gameserver.services.AutoGroupService` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService` | Service | Partial | Unit Tested | Partial Parity | Logout cleanup result plans are now consumed live for ready matches. Other logout branches remain partial. |

## Known Gaps
- Java `LookingForParty.isOnStartEnterTask()` is not modeled for queued C# registrations.
- Java `onLogout` start-enter branch delegates to `cancelEnter`; this remains incomplete.
- Java auto-instance `destroyIfPossible` on logout remains incomplete.
- Additional-registration cleanup during logout-triggered ready matches is covered through shared ready-match tests, not a logout-specific cleanup scenario.

## Next Recommended UOW
- `UOW-2395`: Model Java `onLogout` start-enter `cancelEnter` delegation as a planner/result slice before attempting live wiring.

## Suggested Discovery For UOW-2395
- Java:
  - `AutoGroupService.onLogout(Player player)`
  - `LookingForParty.isOnStartEnterTask()`
  - `AutoGroupService.cancelEnter(Player player, int instanceMaskId)`
- C#:
  - `AutoGroupLookingPartyRegistration`
  - `AutoGroupLookingPartyRegistrationService.CleanupSearchEntriesOnLogout(...)`
  - `AutoGroupInstanceLeaveRuntimeService.CancelEnter(...)`
  - `GameServerConnection.LeavePlayerWorldAsync(...)`

## Focused Validation Recipe For Next UOW
- Specific behavior to validate: a queued registration marked as start-enter causes logout cleanup to surface Java-style cancel-enter intents instead of leader/member search cleanup.
- Focused C# command:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~AutoGroupInstanceLeaveRuntimeServiceTests" --no-restore`
- Focused Java/Maven command: not expected unless a targeted Java fixture is added; use Java source review by default.
- Broad-validation trigger: none for planner/result modeling; live connection dispatch if the planner is wired in the same UOW.

## Safe Candidate UOWs
- Add a logout-specific additional-registration cleanup test for ready-match dispatch.
- Add tests for stop-registration close behavior confirming it sends cancel windows without penalty refresh intents.
- Improve cancel-enter destroy-if-possible modeling if online-inside-player facts become available.
