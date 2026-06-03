# Phase 6 Session 2495 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2495: Add Vortex stopInvasion runtime snapshot collector preview

## Commits Made

- `[Phase 6][UOW-2495] Collect Vortex stop snapshots`

## Summary

UOW-2495 added a non-live collector that builds `VortexStopInvasionSnapshotRequest` from a `VortexInvasionSnapshot` plus supplied player, invader-Kisk, spawned-NPC, and invader-alliance candidates. This moves stop/kick metadata one step closer to production inputs while keeping all side effects inert.

This remains partial parity. The collector does not yet read production world/location/alliance maps directly; future adapters must provide candidate objects. It does not send packets, teleport players, mutate alliances, mutate participant maps, mutate passed-player maps, sync passed-player state, kill Kisks, despawn NPCs, or spawn NPCs.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/VortexStopInvasionRuntimeSnapshotCollectorService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`
- `docs/Phase-6-Session-2495-Completion.md`
- `docs/Phase-6-Session-2495-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.services.vortex.Invasion.stopInvasion`
- `com.aionemu.gameserver.services.vortex.Invasion.kickPlayer`
- `com.aionemu.gameserver.model.vortex.VortexLocation`
- `com.aionemu.gameserver.model.gameobjects.Kisk`

## C# Artifacts Touched

- `Aion.GameServer.Services.VortexStopInvasionRuntimeSnapshotCollectorService`
- `Aion.GameServer.Services.VortexStopInvasionSnapshotRequest`
- `Aion.GameServer.Services.VortexInvasionSnapshot`
- `Aion.GameServer.Services.VortexStopInvaderSnapshot`
- `Aion.GameServer.Services.VortexStopInvaderKiskSnapshot`
- `Aion.GameServer.Services.VortexStopSpawnedNpcSnapshot`

## Validation Completed

Validation target: C# stopInvasion snapshot collection captures Java-relevant stop/kick inputs for coordinator metadata while preserving no-live packet, teleport, alliance, participant, passed-player, sync, Kisk death, despawn, or spawn mutation flags.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests" --no-restore
```

- Passed: 92 tests.
- Existing nullable/analyzer warnings were emitted from unrelated files.

```powershell
git diff --check
git diff --cached --check
```

- `git diff --check` passed. Git reported an expected CRLF conversion warning for the edited test file.
- `git diff --cached --check` passed after staging. Git reported expected CRLF conversion warnings for edited/new files.

Java/Maven validation was skipped because no Java source or fixtures changed and no narrow Java Vortex stop/removal fixture exists. Java source was reviewed directly. Broad-validation trigger was `none` because this UOW only adds non-live snapshot collection metadata and tests.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.vortex.Invasion.stopInvasion` | `Aion.GameServer.Services.VortexStopInvasionRuntimeSnapshotCollectorService` | Runtime collector | Partial | Unit Tested | Partial Parity | C# collects stop-time metadata inputs for coordinator plans. Live stop side effects remain disabled. |
| `com.aionemu.gameserver.services.vortex.Invasion.kickPlayer(Player, boolean=true)` | `Aion.GameServer.Services.VortexStopInvasionRuntimeSnapshotCollectorService` | Runtime collector | Partial | Unit Tested | Partial Parity | C# supplies invader, alliance, and passed-player snapshots needed by kick/removal metadata. Live packet, teleport, alliance, participant, passed-player, and sync mutation remain disabled. |
| `com.aionemu.gameserver.model.vortex.VortexLocation` | `Aion.GameServer.Services.VortexInvasionSnapshot` plus collector candidates | Runtime metadata | Partial | Unit Tested | Partial Parity | C# runtime snapshot provides participant and passed-player ids; Kisk, spawned-NPC, and alliance candidates still come from supplied adapter inputs. |
| `com.aionemu.gameserver.model.gameobjects.Kisk` | `Aion.GameServer.Services.VortexStopInvaderKiskSnapshot` via collector | Runtime metadata | Partial | Unit Tested | Partial Parity | C# collector carries Kisk death intent metadata only. Live Kisk controller death remains disabled. |

## Known Gaps

- Live Vortex `start`, `spawn`, `despawn`, and `stop` behavior remains unimplemented.
- Production Vortex creature/zone lifecycle adapter remains absent.
- Live Vortex zone-player and invader-Kisk membership maps remain disabled.
- Defender request storage, packet dispatch, request-handler callbacks, add-player transition, and alliance mutation remain metadata-only.
- Invader participant and alliance mutation remain metadata-only.
- Stop-time collector depends on supplied candidate collections and does not yet collect from production world/location/alliance maps.
- Full production XML coverage for Vortex spawns has not been compared against Java output.

## Remaining Risks

- Future live stop work must preserve Java ordering: active Vortex clear, Kisk controller death, online invader kick/removal, despawn, then PEACE spawn.
- Future live kick/removal work must preserve Java `kickPlayer` tail-call ordering for passed-player removal and `syncPassed(true)`.
- Passed-player count simulation still depends on supplied invader ordering and snapshots.
- Current composition planners still need production adapters before they can observe real `Player`, alliance, passed-player, Kisk, spawn, and location state.

## Next Recommended UOW

[Phase 6] UOW-2496: Add Vortex stopInvasion static PEACE spawn collector composition

The next smallest safe task is to compose the runtime snapshot collector with static PEACE spawn selection into a single non-live stop request preparation path, so a future coordinator call can gather runtime stop snapshots and append Java `spawn(VortexStateType.PEACE)` metadata from static data in one place.

Safe alternative candidates:

- Compose Vortex `startInvasion` alliance-update metadata further from Java `Invasion.startInvasion`.
- Add a narrow Java fixture/golden for Vortex stop/kick metadata if a suitable Java test seam is available.
- Continue production Vortex lifecycle adapter discovery for real world/location/alliance/Kisk/spawn inputs.

## Suggested Files To Inspect

- `game-server/src/com/aionemu/gameserver/services/vortex/Invasion.java`
- `game-server/src/com/aionemu/gameserver/model/vortex/VortexLocation.java`
- `dotnetConversion/src/Aion.GameServer/Services/VortexStopInvasionRuntimeSnapshotCollectorService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexStopInvasionCoordinatorService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexStopInvasionSideEffectPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`

## Suggested Validation

For non-live Vortex stop request preparation with runtime snapshots and static PEACE spawns:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests" --no-restore
```

Validation target: C# stopInvasion request preparation composes runtime stop/kick snapshots with Java PEACE spawn selection while preserving no-live packet, teleport, alliance, participant, passed-player, sync, Kisk death, despawn, or spawn mutation flags.

Java/Maven is not expected unless Java source or fixtures change or a narrow Java Vortex stop/removal fixture is added. Broad-validation trigger should be `none` if the UOW only adds non-live request preparation metadata and tests; it becomes present if live packet dispatch, teleport, alliance mutation, participant mutation, passed-player mutation, sync writes, scheduler dispatch, Kisk death, despawn, spawn, or zone-player/Kisk map mutation is enabled.
