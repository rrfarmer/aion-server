# Phase 6 Session 2389 Handoff

## Current Phase
- Phase 6: Port Game Core.

## Last Completed UOW
- `UOW-2389`: Refilled queued quick-entry registrations after Java-style autogroup `onLeaveInstance`.

## Files Changed
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInstanceCooldownTests.cs`
- `docs/Phase-6-Session-2389-Completion.md`
- `docs/Phase-6-Session-2389-Handoff.md`

## Java Artifacts Touched
- `com.aionemu.gameserver.services.AutoGroupService`
- `com.aionemu.gameserver.instance.AutoInstance`
- `com.aionemu.gameserver.instance.AutoPvpInstance`

## C# Artifacts Touched
- `Aion.GameServer.Network.Aion.GameServerConnection`
- `Aion.GameServer.Services.AutoGroupInstanceLeaveRuntimeService`
- `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService`
- `Aion.GameServer.Tests.GameServerConnectionInstanceCooldownTests`

## What Changed
- The live teleport leave adapter now mirrors Java's `destroyOrAddPlayersFromQuickEntries(autoInstance)` refill branch after `AutoGroupService.onLeaveInstance(...)`.
- When `AutoGroupInstanceLeavePlan.WouldCheckQuickEntries` is true, the adapter uses `TryRefillQueuedQuickEntry(...)` and sends the resulting ready/cancel windows to affected queued players through the connection registry.
- The normal leaving-player open-registration refresh still happens after the quick-entry refill attempt, matching Java ordering.

## Tests Run
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionInstanceCooldownTests|FullyQualifiedName~AutoGroupInstanceLeaveRuntimeServiceTests|FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests" --no-restore`
- Result: 68 passed, 0 failed, 0 skipped.
- Java/Maven: skipped because no targeted Java fixture exists for this runtime branch and Java source was not changed.
- Broad .NET: skipped after focused live adapter/runtime/registration coverage passed, despite the live-dispatch trigger, because the change did not touch packet primitives, persistence, scheduler, or shared infrastructure.

## Migration Parity Table
| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.AutoGroupService` | `Aion.GameServer.Network.Aion.GameServerConnection` / `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService` / `Aion.GameServer.Services.AutoGroupInstanceLeaveRuntimeService` | Service | Partial | Unit Tested | Partial Parity | Cancel-enter and leave-instance quick-entry refill branches are now wired. Penalty scheduling, logout behavior, and full start-enter lifecycle remain incomplete. |
| `com.aionemu.gameserver.instance.AutoInstance` | `Aion.GameServer.Services.AutoGroupInstanceLeaveRuntimeService` | Runtime State | Partial | Unit Tested | Partial Parity | Destroy/refill planning is modeled from registered count and online-player count inputs. Full Java world-player facts remain partial. |
| `com.aionemu.gameserver.instance.AutoPvpInstance` | `Aion.GameServer.Services.AutoGroupInstanceLeaveRuntimeService` | Runtime State | Partial | Unit Tested | Partial Parity | Race/capacity quick-entry admission is reused for cancel-enter and leave refill. Other subtype-specific behavior remains only partially covered. |

## Known Gaps
- `AutoGroupService.penalisePlayerAndScheduleRemoval(...)` is not ported.
- `AutoGroupService.onLogout(...)` still has unported behavior around start-enter tasks, leader transfer, member unregister, queue rematch, and active instance destroy checks.
- `LookingForParty.isOnStartEnterTask()` is still only partially represented in C# request timing data.
- Java `destroyIfPossible(...)` depends on live world-player state; C# leave has a live player-count input, but cancel-enter still has only a conservative registered-count guard.

## Next Recommended UOW
- `UOW-2390`: Port the Java cancel-enter penalty scheduling model as a focused planner/runtime slice.

## Suggested Discovery For UOW-2390
- Java:
  - `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
    - `cancelEnter(Player player, int instanceMaskId)`
    - `penalisePlayerAndScheduleRemoval(int playerObjId)`
    - `penaltyExpired(int playerObjectId)`
  - Search for `penalizedPlayers`, delayed removal, and any system messages around blocked registration.
- C#:
  - `dotnetConversion/src/Aion.GameServer/Services/AutoGroupInstanceLeaveRuntimeService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/AutoGroupLookingPartyRegistrationService.cs`
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
  - existing autogroup tests in `AutoGroupInstanceLeaveRuntimeServiceTests`, `AutoGroupLookingPartyRegistrationServiceTests`, and `GameServerConnectionAutoGroupTests`.

## Focused Validation Recipe For Next UOW
- Specific behavior to validate: cancel-enter penalty records the player as temporarily blocked/removed exactly where Java `penalisePlayerAndScheduleRemoval(...)` affects subsequent registration.
- Focused C# command:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~AutoGroupInstanceLeaveRuntimeServiceTests|FullyQualifiedName~GameServerConnectionAutoGroupTests" --no-restore`
- Focused Java/Maven command: not expected unless a targeted Java fixture is added; use Java source review by default.
- Broad-validation trigger: none for a planner-only slice; if live dispatch is changed again, name the live connection dispatch trigger before deciding whether focused coverage is enough.

## Safe Candidate UOWs
- Add service-level coverage for queued quick-entry refill rejection after maximum join time on the leave path.
- Port `AutoGroupService.onLogout(...)` search-entry cleanup as a planner before wiring live logout.
- Improve cancel-enter `destroyIfPossible(...)` modeling if online-inside-player facts become available for that path.
