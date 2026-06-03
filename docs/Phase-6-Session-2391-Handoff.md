# Phase 6 Session 2391 Handoff

## Current Phase
- Phase 6: Port Game Core.

## Last Completed UOW
- `UOW-2391`: Added a live Java-style autogroup penalty refresh scheduler/dedupe adapter.

## Files Changed
- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupPenaltyRefreshSchedulerService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupInstanceLeaveRuntimeService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupLookingPartyRegistrationService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientSocketServer.cs`
- `dotnetConversion/src/Aion.GameServer/Program.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/AutoGroupInstanceLeaveRuntimeServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionAutoGroupTests.cs`
- `docs/Phase-6-Session-2391-Completion.md`
- `docs/Phase-6-Session-2391-Handoff.md`

## Java Artifacts Touched
- `com.aionemu.gameserver.services.AutoGroupService`
- `com.aionemu.gameserver.services.instance.PeriodicInstanceManager`

## C# Artifacts Touched
- `Aion.GameServer.Services.AutoGroupPenaltyRefreshSchedulerService`
- `Aion.GameServer.Services.AutoGroupInstanceLeaveRuntimeService`
- `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService`
- `Aion.GameServer.Services.PeriodicInstanceRegistrationService`
- `Aion.GameServer.Network.Aion.GameServerConnection`
- `Aion.GameServer.Network.Aion.GameClientSocketServer`

## What Changed
- Added a scheduler service that mirrors Java's `penalties` set by accepting only one pending delayed refresh per player object id.
- The scheduled callback removes the pending marker and refreshes open-registration icon packets for the online player.
- `GameServerConnection` now schedules penalty refresh intents from cancel-registration, ready-match cleanup, open quick-entry cleanup, cancel-enter, and teleport-leave refill cleanup.
- Cancel-enter runtime results now expose the same 10000 ms penalty refresh intent used by other autogroup cleanup paths.

## Tests Run
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~PeriodicInstanceRegistrationServiceTests|FullyQualifiedName~AutoGroupInstanceLeaveRuntimeServiceTests" --no-restore`
- Result: 77 passed, 0 failed, 0 skipped.
- Java/Maven: skipped because no targeted Java fixture exists and no Java source changed.
- Broad .NET: skipped after focused validation; live scheduler/connection dispatch was covered by the targeted auto-group slice.

## Migration Parity Table
| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.AutoGroupService` | `Aion.GameServer.Services.AutoGroupPenaltyRefreshSchedulerService` | Service | Partial | Unit Tested | Partial Parity | Java `penalties.add(objectId)` dedupe and 10000 ms delayed refresh are represented in the live adapter. |
| `com.aionemu.gameserver.services.AutoGroupService` | `Aion.GameServer.Network.Aion.GameServerConnection` | Packet Handler | Partial | Integration Tested | Partial Parity | Main C# autogroup cleanup paths now dispatch penalty refresh intents to the scheduler. |
| `com.aionemu.gameserver.services.instance.PeriodicInstanceManager` | `Aion.GameServer.Services.PeriodicInstanceRegistrationService` | Service | Partial | Unit Tested | Partial Parity | Existing C# open-registration packet creator is reused by the scheduler execution path. |

## Known Gaps
- Java `AutoGroupService.onLogout(...)` remains incomplete.
- Delayed scheduler execution was validated without sleeping for Java's full 10000 ms delay.
- Additional live coverage could be added for ready-match and quick-entry cleanup scheduling if future changes touch those call sites.

## Next Recommended UOW
- `UOW-2392`: Port `AutoGroupService.onLogout(...)` search-entry cleanup as a focused planner/runtime slice.

## Suggested Discovery For UOW-2392
- Java:
  - `AutoGroupService.onLogout(Player player)`
  - `AutoGroupService.cancelRegistration(...)`
  - `AutoGroupService.searchAndRemoveAdditionalRegistrations(int objectId)`
- C#:
  - `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService`
  - `Aion.GameServer.Network.Aion.GameServerConnection` logout/disconnect handling
  - `PlayerGroupRuntime` and `PlayerAllianceRuntime` if logout cleanup needs team facts

## Focused Validation Recipe For Next UOW
- Specific behavior to validate: logging out while registered in autogroup search removes the player's search entry and applies Java-equivalent party/member cleanup semantics.
- Focused C# command:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~GameServerConnectionAutoGroupTests" --no-restore`
- Focused Java/Maven command: not expected unless a targeted Java fixture is added; use Java source review by default.
- Broad-validation trigger: connection logout/disconnect dispatch if live logout wiring is enabled.

## Safe Candidate UOWs
- Add tests for stop-registration close behavior confirming it sends cancel windows without penalty refresh intents.
- Improve cancel-enter destroy-if-possible modeling if online-inside-player facts become available.
- Add live scheduling coverage for ready-match/open-quick-entry cleanup if those paths become riskier.
