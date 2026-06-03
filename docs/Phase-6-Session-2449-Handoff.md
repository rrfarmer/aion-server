# Phase 6 Session 2449 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2449: Bridge Vortex portal pass tracking into active invasion runtime

## Commits Made

- `[Phase 6][UOW-2449] Bridge Vortex portal pass runtime`

## Summary

UOW-2449 connected accepted vortex portal use to the active Vortex runtime state. Java `RVController.acceptRequest` teleports a vortex passer, records them in `passedPlayers`, and syncs the passed count. Later, Java `VortexLocation.onEnterZone` promotes a passed invader into `Invasion.addPlayer(player, true)`.

C# now mirrors that slice: `RiftPortalUseService` can record accepted vortex passers into `VortexInvasionRuntime`, and `VortexInvasionRuntime.AddInvaderFromPassedPortal` promotes only players that previously passed through the vortex portal. This remains partial Vortex parity: full start/stop spawn lifecycle, defender handling, and online kick packet/teleport behavior are still open.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/RiftPortalUseService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexInvasionRuntime.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/RiftPortalUseServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`
- `docs/Phase-6-Session-2449-Completion.md`
- `docs/Phase-6-Session-2449-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.controllers.RVController`
- `com.aionemu.gameserver.controllers.RVController.acceptRequest`
- `com.aionemu.gameserver.model.vortex.VortexLocation`
- `com.aionemu.gameserver.model.vortex.VortexLocation.onEnterZone`
- `com.aionemu.gameserver.services.VortexService`
- `com.aionemu.gameserver.services.vortex.Invasion`
- `com.aionemu.gameserver.services.vortex.Invasion.addPlayer`
- `com.aionemu.gameserver.services.vortex.DimensionalVortex`

## C# Artifacts Touched

- `Aion.GameServer.Services.RiftPortalUseService`
- `Aion.GameServer.Services.VortexInvasionRuntime`
- `Aion.GameServer.Services.VortexInvaderJoinResult`
- `Aion.GameServer.Services.VortexLocationService` (resolver dependency)
- `Aion.GameServer.Services.RiftPortalState` (adjacent existing pass count state)

## Validation Completed

Validation target: Java-like vortex portal acceptance records passed-player state and zone-entry style promotion only admits passed invaders.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~Vortex|FullyQualifiedName~RiftPortalUseServiceTests|FullyQualifiedName~RiftServiceTests|FullyQualifiedName~PlayerAllianceOfflineTimeoutDispatchServiceTests" --no-restore
```

- Passed: 34 tests.
- Existing nullable/analyzer warnings were emitted.

```powershell
git diff --check
```

- Passed with expected LF-to-CRLF working-tree warnings.

Java/Maven validation was skipped because no Java source or Java fixtures changed and no narrow Java Vortex fixture exists for this runtime slice. Broad-validation trigger was present because production-capable portal use can now mutate shared Vortex runtime state when an active Vortex runtime exists; full project/solution validation was skipped after focused validation because the changed behavior is isolated to Vortex/Rift portal pass state and adjacent timeout cleanup.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.controllers.RVController.acceptRequest` | `Aion.GameServer.Services.RiftPortalUseService.AcceptPortal` plus `VortexInvasionRuntime.RecordPortalPass` | Controller/service side effect | Partial | Unit Tested | Partial Parity | Vortex portal pass tracking is now bridged to runtime; Java team removal and open notice are handled in `RiftPortalInteractionService`; full request handler lifecycle remains partial. |
| `com.aionemu.gameserver.model.vortex.VortexLocation.onEnterZone` | `Aion.GameServer.Services.VortexInvasionRuntime.AddInvaderFromPassedPortal` | Zone/runtime side effect | Partial | Unit Tested | Partial Parity | Passed invader promotion is ported as a runtime method; no live zone handler wiring yet. |
| `com.aionemu.gameserver.services.vortex.Invasion.addPlayer` | `Aion.GameServer.Services.VortexInvasionRuntime.AddInvaderFromPassedPortal` | Vortex participant mutation | Partial | Unit Tested | Partial Parity | Invader participant insertion is covered; Java group/alliance removal and alliance create/add logic are not ported in this method. |
| `com.aionemu.gameserver.services.VortexService` | `Aion.GameServer.Services.VortexInvasionRuntime` and `VortexLocationService` | Service/runtime | Partial | Unit Tested | Partial Parity | Active invasion state exists and can track portal passers/invaders; start/stop spawn lifecycle remains unported. |

## Known Gaps

- Full Vortex active lifecycle population is not ported: `VortexService.startInvasion`, `stopInvasion`, spawn/despawn, rift generator death observer, and scheduled stop remain missing.
- Online Vortex kick behavior remains unported: Java sends Vortex kick/direct portal-out messages and teleports online invaders home.
- Defender `VortexLocation.onEnterZone` prompt/alliance path is not ported.
- Zone leave delayed kick behavior is not ported.
- `VortexInvasionRuntime` can now consume portal pass records, but live zone handler wiring does not call `AddInvaderFromPassedPortal` yet.

## Remaining Risks

- Production portal use will only mutate `VortexInvasionRuntime` when an active runtime location already exists; because full Vortex start lifecycle is still missing, production may not populate that state yet.
- Java Vortex alliance behavior is complex and currently only represented by narrow participant state.
- Future Vortex start/stop work must ensure portal pass state, active invader/defender state, rift passed-count state, and timeout cleanup stay consistent.

## Next Recommended UOW

[Phase 6] UOW-2450: Port online Vortex invader kick packet and teleport side effects

The next sequential task is to inspect Java `Invasion.kickPlayer` and C# teleport/system-message helpers, then add the online invader kick slice to `VortexInvasionRuntime.RemoveInvaderPlayer` or a focused executor around it. Keep scope narrow: invader online message `1401452`, portal-out message `1401474`, and home-point teleport when the player is online in the invasion world. Do not port defender prompts or full Vortex spawn lifecycle in the same UOW unless discovery proves a required dependency.

Alternative safe candidate: port Vortex start/stop lifecycle state in `VortexInvasionRuntime` without spawn/despawn side effects, but online kick is smaller and builds directly on the current runtime tests.

## Suggested Files To Inspect

- `game-server/src/com/aionemu/gameserver/services/vortex/Invasion.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`
- `game-server/src/com/aionemu/gameserver/services/teleport/TeleportService.java`
- `dotnetConversion/src/Aion.GameServer/Services/VortexInvasionRuntime.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerTeleportService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`

## Suggested Validation

For online Vortex invader kick parity:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~Vortex|FullyQualifiedName~GamePacketTests" --no-restore
```

Narrow to `VortexLocationServiceTests` and specific `GamePacketTests` methods after discovery if possible. Java/Maven is not expected unless Java fixtures change or a narrow Java message/teleport fixture is added. Broad-validation trigger is present if live teleport mutation or socket packet dispatch is enabled; otherwise none if the UOW only adds runtime result metadata and packet factory tests.
