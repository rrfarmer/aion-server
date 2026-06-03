# Phase 6 Session 2494 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2494: Compose Vortex stopInvasion coordinator kick-removal report

## Commits Made

- `[Phase 6][UOW-2494] Report Vortex stop kick metadata`

## Summary

UOW-2494 surfaced the existing stop-time kick/removal metadata through the stop coordinator and report layer. `VortexStopInvasionSnapshotRequest` now carries invader-alliance and passed-player snapshots, the coordinator forwards them through direct, request, and static PEACE enrichment paths, and reports expose `OrderedKickRemovalPlans`.

This remains partial parity. The path records Java-shaped intent only; it does not send packets, teleport players, mutate alliances, mutate participant maps, mutate passed-player maps, sync passed-player state, kill Kisks, despawn NPCs, or spawn NPCs.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/VortexStopInvasionCoordinatorService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`
- `docs/Phase-6-Session-2494-Completion.md`
- `docs/Phase-6-Session-2494-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.services.vortex.Invasion.stopInvasion`
- `com.aionemu.gameserver.services.vortex.Invasion.kickPlayer`
- `com.aionemu.gameserver.model.team.alliance.PlayerAlliance`
- `com.aionemu.gameserver.model.vortex.VortexStateType.PEACE`

## C# Artifacts Touched

- `Aion.GameServer.Services.VortexStopInvasionCoordinatorService`
- `Aion.GameServer.Services.VortexStopInvasionSnapshotRequest`
- `Aion.GameServer.Services.VortexStopInvasionCoordinatorReport`
- `Aion.GameServer.Services.VortexKickPlayerRemovalPlan`

## Validation Completed

Validation target: C# stopInvasion coordinator metadata reports Java-shaped stop ordering and per-online-invader kick/removal intent, including snapshot-request and static PEACE enrichment paths, while preserving no-live packet, teleport, alliance, participant, passed-player, sync, Kisk death, despawn, or spawn mutation flags.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests" --no-restore
```

- Passed: 90 tests.
- Existing nullable/analyzer warnings were emitted from unrelated files.

```powershell
git diff --check
git diff --cached --check
```

- `git diff --check` passed. Git reported expected CRLF conversion warnings for the edited source and test files.
- `git diff --cached --check` passed after staging. Git reported expected CRLF conversion warnings for edited/new files.

Java/Maven validation was skipped because no Java source or fixtures changed and no narrow Java Vortex stop/removal fixture exists. Java source was reviewed directly. Broad-validation trigger was `none` because this UOW only threads non-live metadata/reporting and tests.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.vortex.Invasion.stopInvasion` | `Aion.GameServer.Services.VortexStopInvasionCoordinatorService` | Runtime coordinator | Partial | Unit Tested | Partial Parity | C# coordinator reports Java stop order and kick/removal metadata through direct, request, and static PEACE enrichment paths. Live side effects remain disabled. |
| `com.aionemu.gameserver.services.vortex.Invasion.kickPlayer(Player, boolean=true)` | `Aion.GameServer.Services.VortexStopInvasionCoordinatorReport` | Runtime report | Partial | Unit Tested | Partial Parity | C# exposes per-online-invader kick/removal plans from stop reports. Live packet, teleport, alliance, participant, passed-player, and sync mutation remain disabled. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAlliance` | `Aion.GameServer.Services.VortexStopInvasionSnapshotRequest` | Runtime metadata | Partial | Unit Tested | Partial Parity | C# request accepts supplied invader-alliance snapshots needed by Java kickPlayer branches. Full live alliance behavior remains absent. |
| `com.aionemu.gameserver.model.vortex.VortexStateType.PEACE` | `Aion.GameServer.Services.VortexStopInvasionSnapshotRequest.WithPeaceSpawns` | Runtime metadata | Partial | Unit Tested | Partial Parity | C# static PEACE enrichment preserves kick/removal snapshots while appending PEACE spawn rows. Live spawn remains disabled. |

## Known Gaps

- Live Vortex `start`, `spawn`, `despawn`, and `stop` behavior remains unimplemented.
- Production Vortex creature/zone lifecycle adapter remains absent.
- Live Vortex zone-player and invader-Kisk membership maps remain disabled.
- Defender request storage, packet dispatch, request-handler callbacks, add-player transition, and alliance mutation remain metadata-only.
- Invader participant and alliance mutation remain metadata-only.
- Stop-time kick/removal metadata depends on supplied snapshots and is not collected from production runtime objects.
- Full production XML coverage for Vortex spawns has not been compared against Java output.

## Remaining Risks

- Future live stop work must preserve Java ordering: active Vortex clear, Kisk controller death, online invader kick/removal, despawn, then PEACE spawn.
- Future live kick/removal work must preserve Java `kickPlayer` tail-call ordering for passed-player removal and `syncPassed(true)`.
- Passed-player count simulation depends on supplied invader ordering and snapshots.
- Current composition planners still need production adapters before they can observe real `Player`, alliance, passed-player, Kisk, spawn, and location state.

## Next Recommended UOW

[Phase 6] UOW-2495: Add Vortex stopInvasion runtime snapshot collector preview

The next smallest safe task is to move from caller-supplied stop metadata toward production inputs by adding a non-live snapshot collector/adapter preview for stop-time invaders, invader Kisks, spawned NPCs, passed-player ids, and alliance metadata where current C# runtime state can provide them. Keep the collector inert and do not enable live packet dispatch, teleport, alliance mutation, participant mutation, passed-player mutation, sync writes, Kisk death, despawn, or spawn.

Safe alternative candidates:

- Compose Vortex `startInvasion` alliance-update metadata further from Java `Invasion.startInvasion`.
- Add a narrow Java fixture/golden for Vortex stop/kick metadata if a suitable Java test seam is available.
- Continue production Vortex lifecycle adapter discovery if runtime state does not yet expose enough snapshot inputs.

## Suggested Files To Inspect

- `game-server/src/com/aionemu/gameserver/services/vortex/Invasion.java`
- `game-server/src/com/aionemu/gameserver/model/vortex/VortexLocation.java`
- `dotnetConversion/src/Aion.GameServer/Services/VortexInvasionRuntime.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexStopInvasionCoordinatorService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexStopInvasionSideEffectPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexKickPlayerRemovalPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`

## Suggested Validation

For a non-live Vortex stop snapshot collector preview:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests" --no-restore
```

Validation target: C# stopInvasion snapshot collection captures Java-relevant stop/kick inputs for coordinator metadata while preserving no-live packet, teleport, alliance, participant, passed-player, sync, Kisk death, despawn, or spawn mutation flags.

Java/Maven is not expected unless Java source or fixtures change or a narrow Java Vortex stop/removal fixture is added. Broad-validation trigger should be `none` if the UOW only adds non-live snapshot collection metadata and tests; it becomes present if live packet dispatch, teleport, alliance mutation, participant mutation, passed-player mutation, sync writes, scheduler dispatch, Kisk death, despawn, spawn, or zone-player/Kisk map mutation is enabled.
