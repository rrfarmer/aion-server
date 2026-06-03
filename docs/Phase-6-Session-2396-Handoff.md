# Phase 6 Session 2396 Handoff

## Current Phase
- Phase 6: Port Game Core.

## Last Completed UOW
- `UOW-2396`: Wired Java autogroup logout start-enter `cancelEnter` delegation into live C# logout.

## Commits Made
- `[Phase 6][UOW-2396] Wire autogroup start-enter logout cancellation`

## Files Changed
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupInstanceLeaveRuntimeService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/AutoGroupInstanceLeaveRuntimeServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionAutoGroupTests.cs`
- `docs/Phase-6-Session-2396-Completion.md`
- `docs/Phase-6-Session-2396-Handoff.md`

## Java Artifacts Touched
- `com.aionemu.gameserver.services.AutoGroupService`

## C# Artifacts Touched
- `Aion.GameServer.Network.Aion.GameServerConnection`
- `Aion.GameServer.Services.AutoGroupInstanceLeaveRuntimeService`
- `Aion.GameServer.Tests.AutoGroupInstanceLeaveRuntimeServiceTests`
- `Aion.GameServer.Tests.GameServerConnectionAutoGroupTests`

## What Changed
- Runtime service now exposes active auto-instance mask IDs for Java-style `autoInstances.values()` logout iteration.
- `LeavePlayerWorldAsync(...)` passes those masks into start-enter logout cleanup and applies every planned cancel-enter intent.
- Client cancel-enter window 103 and logout-driven cancel-enter now share `ApplyAutoGroupCancelEnterAsync(...)`.
- Start-enter logout now unregisters matching runtime auto-instance membership, schedules penalty refresh, sends cancel window 2, and keeps the queued search entry intact.

## Tests Run
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~AutoGroupInstanceLeaveRuntimeServiceTests" --no-restore`
- Result: 74 passed, 0 failed, 0 skipped.
- Java/Maven: skipped because no targeted Java fixture exists and no Java source changed.
- Broad .NET: skipped after focused validation; broad trigger was live connection dispatch and scheduler intents, but the changed path is isolated and directly covered by focused autogroup connection/service/runtime tests.

## Migration Parity Table
| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.AutoGroupService` | `Aion.GameServer.Network.Aion.GameServerConnection` | Connection / Logout Handler | Partial | Integration Tested | Partial Parity | Start-enter logout now applies runtime cancel-enter side effects live. Auto-instance destroy-if-possible branch remains incomplete. |
| `com.aionemu.gameserver.services.AutoGroupService` | `Aion.GameServer.Services.AutoGroupInstanceLeaveRuntimeService` | Runtime Service | Partial | Unit Tested | Partial Parity | Active auto-instance masks are exposed for logout cancel-enter planning; ordering is intentionally not relied on. |

## Known Gaps
- Java `onLogout` auto-instance `destroyIfPossible(autoInstance)` remains incomplete.
- Logout-specific quick-entry refill after cancel-enter is not separately tested, though the shared cancel-enter helper uses existing client cancel-enter behavior.
- Additional-registration cleanup during logout-triggered ready matches is covered through shared ready-match tests, not a logout-specific cleanup scenario.

## Next Recommended UOW
- `UOW-2397`: Model Java `AutoGroupService.onLogout(...)` auto-instance `destroyIfPossible(autoInstance)` branch for players already registered inside the current auto instance.

## Suggested Discovery For UOW-2397
- Java:
  - `AutoGroupService.onLogout(Player player)`
  - `AutoGroupService.destroyIfPossible(AutoInstance autoInstance)`
  - `AutoInstance.getRegisteredAGPlayers()`
- C#:
  - `AutoGroupInstanceLeaveRuntimeService`
  - `AutoGroupInstanceLeavePlanService`
  - `GameServerConnection.LeavePlayerWorldAsync(...)`
  - `AutoGroupInstanceLeaveRuntimeServiceTests`
  - `GameServerConnectionAutoGroupTests`

## Focused Validation Recipe For Next UOW
- Specific behavior to validate: after search cleanup, logout checks the player's current world-map auto instance and destroys/removes the runtime instance only when Java `destroyIfPossible` conditions are satisfied.
- Focused C# command:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~AutoGroupInstanceLeaveRuntimeServiceTests|FullyQualifiedName~AutoGroupInstanceLeavePlanServiceTests" --no-restore`
- Focused Java/Maven command: not expected unless a targeted Java fixture is added; use Java source review by default.
- Broad-validation trigger: live connection dispatch and instance destroy workflow if `LeavePlayerWorldAsync(...)` is wired to invoke destroy behavior.

## Safe Candidate UOWs
- Add a logout-specific quick-entry refill test for start-enter cancel-enter.
- Add a logout-specific additional-registration cleanup test for ready-match dispatch.
- Add tests for stop-registration close behavior confirming it sends cancel windows without penalty refresh intents.
