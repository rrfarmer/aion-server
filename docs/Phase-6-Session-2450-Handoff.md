# Phase 6 Session 2450 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2450: Port online Vortex invader kick packet and teleport side effects

## Commits Made

- `[Phase 6][UOW-2450] Port Vortex invader kick effects`

## Summary

UOW-2450 ported the online invader branch of Java `Invasion.kickPlayer`. C# Vortex invader removal now returns the invader kick packet intent `1401452`, and when the removed online invader is still in the invasion world it also returns portal-out packet intent `1401474` and moves the player to the Vortex home point.

This is still partial Vortex parity. The C# runtime returns packets as intents rather than sending them over a live connection, and Java alliance removal/disband behavior in `Invasion.kickPlayer` is not fully wired into the Vortex runtime yet.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/VortexInvasionRuntime.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerTeleportService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `docs/Phase-6-Session-2450-Completion.md`
- `docs/Phase-6-Session-2450-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.services.vortex.Invasion`
- `com.aionemu.gameserver.services.vortex.Invasion.kickPlayer`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE`
- `com.aionemu.gameserver.services.teleport.TeleportService`
- `com.aionemu.gameserver.services.teleport.TeleportService.teleportTo(Player, WorldPosition)`

## C# Artifacts Touched

- `Aion.GameServer.Services.VortexInvasionRuntime`
- `Aion.GameServer.Services.VortexInvaderRemovalResult`
- `Aion.GameServer.Services.PlayerTeleportService`
- `Aion.GameServer.Services.PlayerTeleportResult`
- `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage`

## Validation Completed

Validation target: Java `Invasion.kickPlayer` online-invader message and home-teleport branch.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~Vortex|FullyQualifiedName~GamePacketTests" --no-restore
```

- Passed: 292 tests.
- Existing nullable/analyzer warnings were emitted.

```powershell
git diff --check
```

- Passed with expected LF-to-CRLF working-tree warnings.

Java/Maven validation was skipped because no Java source or fixtures changed and the source-of-truth behavior was reviewed directly in `Invasion.kickPlayer`. Broad-validation trigger was present because the runtime now mutates player position through `PlayerTeleportService`; full project/solution validation was skipped after focused validation because the mutation is isolated to Vortex removal and packet primitives/connection dispatch were not changed.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.vortex.Invasion.kickPlayer` | `Aion.GameServer.Services.VortexInvasionRuntime.RemoveInvaderPlayer` | Vortex participant mutation | Partial | Unit Tested | Partial Parity | Invader removal, passed-player cleanup, online invader message `1401452`, portal-out message `1401474`, and home teleport are covered. Defender message `1401476`, alliance removal/disband, sync packet fanout, and live socket dispatch remain incomplete. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` | Packet | Partial | Unit Tested | Partial Parity | Added factories for Vortex invader kick `1401452` and direct portal-out compulsion `1401474`; packet primitive shape was unchanged. |
| `com.aionemu.gameserver.services.teleport.TeleportService.teleportTo(Player, WorldPosition)` | `Aion.GameServer.Services.PlayerTeleportService.TeleportToWorldPosition` | Service helper | Partial | Unit Tested | Partial Parity | Immediate position/movement reset is represented for the Vortex branch. Java cross-world live spawn/channel/player-info packet fanout remains outside this helper. |

## Known Gaps

- Vortex removal packet intents are not yet sent over a live client connection.
- Java alliance removal/disband logic inside `Invasion.kickPlayer` is still incomplete in C# Vortex runtime.
- Defender kick path, defender prompt/alliance path, zone leave delayed kick, and full Vortex start/stop spawn lifecycle remain incomplete.
- `VortexLocation.onEnterZone/onLeaveZone` live zone handler wiring is still missing.

## Remaining Risks

- Future live dispatch work must decide where `VortexInvaderRemovalResult.SystemMessages` are sent without coupling runtime state directly to sockets.
- `PlayerTeleportService.TeleportToWorldPosition` currently models the immediate position/movement side effect needed by Vortex but not the full Java teleport packet fanout.
- Full Vortex lifecycle work must keep active invaders, passed portal state, rift passed-count state, alliance timeout cleanup, and home teleport consistent.

## Next Recommended UOW

[Phase 6] UOW-2451: Add focused Vortex removal dispatch executor for packet intents

The next smallest safe task is to inspect existing C# packet-send intent/executor patterns around group/alliance leave and portal services, then add a narrow executor that consumes `VortexInvaderRemovalResult.SystemMessages` for online players without changing Vortex state rules. Keep this separate from defender prompts and full Vortex lifecycle.

Alternative safe candidate: port the defender message `1401476` metadata branch in Vortex removal, but that likely touches alliance membership semantics and should be approached only after reviewing Java defender alliance behavior.

## Suggested Files To Inspect

- `game-server/src/com/aionemu/gameserver/services/vortex/Invasion.java`
- `game-server/src/com/aionemu/gameserver/utils/PacketSendUtility.java`
- `dotnetConversion/src/Aion.GameServer/Services/VortexInvasionRuntime.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceOfflineTimeoutDispatchService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerAllianceOfflineTimeoutDispatchServiceTests.cs`

## Suggested Validation

For Vortex removal packet dispatch parity:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests|FullyQualifiedName~PlayerAllianceOfflineTimeoutDispatchServiceTests|FullyQualifiedName~GamePacketTests" --no-restore
```

Narrow further after discovery if the executor gets its own test class. Java/Maven is not expected unless Java fixtures change or a narrow Java packet fixture is added. Broad-validation trigger is present if live socket dispatch is enabled; otherwise none if the UOW only adds executor/intention metadata and tests.
