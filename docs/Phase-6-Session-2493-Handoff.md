# Phase 6 Session 2493 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2493: Compose Vortex stopInvasion side-effect metadata with kick removal

## Commits Made

- `[Phase 6][UOW-2493] Compose Vortex stop kick metadata`

## Summary

UOW-2493 connected the non-live Java-shaped `stopInvasion` planner with the kick/removal planner from UOW-2492. Stop-time online invaders now carry attached `VortexKickPlayerRemovalPlan` metadata, including alliance-message intent, optional direct-portal/home-teleport intent, passed-player removal intent, and `syncPassed(true)` count metadata.

This remains partial parity. The C# path still records intent only; it does not send packets, teleport players, mutate alliances, mutate participant maps, mutate passed-player maps, sync passed-player state, kill Kisks, despawn NPCs, or spawn NPCs.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/VortexStopInvasionSideEffectPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`
- `docs/Phase-6-Session-2493-Completion.md`
- `docs/Phase-6-Session-2493-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.services.vortex.Invasion.stopInvasion`
- `com.aionemu.gameserver.services.vortex.Invasion.kickPlayer`
- `com.aionemu.gameserver.model.gameobjects.Kisk`
- `com.aionemu.gameserver.model.vortex.VortexStateType.PEACE`

## C# Artifacts Touched

- `Aion.GameServer.Services.VortexStopInvasionSideEffectPlanService`
- `Aion.GameServer.Services.VortexStopInvasionSideEffectPlan`
- `Aion.GameServer.Services.VortexStopInvasionSideEffectStep`
- `Aion.GameServer.Services.VortexKickPlayerRemovalPlan`
- `Aion.GameServer.Services.VortexKickPlayerRemovalPlanService`

## Validation Completed

Validation target: C# `stopInvasion` metadata composes Java stop side-effect ordering with per-online-invader kick/removal intent while preserving no-live packet, teleport, alliance, participant, passed-player, sync, Kisk death, despawn, or spawn mutation flags.

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

Java/Maven validation was skipped because no Java source or fixtures changed and no narrow Java Vortex stop/removal fixture exists. Java source was reviewed directly. Broad-validation trigger was `none` because this UOW only composes non-live metadata and tests.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.vortex.Invasion.stopInvasion` | `Aion.GameServer.Services.VortexStopInvasionSideEffectPlanService` | Runtime planner | Partial | Unit Tested | Partial Parity | C# composes Java stop order and online-invader kick metadata. Live side effects remain disabled. |
| `com.aionemu.gameserver.services.vortex.Invasion.kickPlayer(Player, boolean=true)` | `Aion.GameServer.Services.VortexKickPlayerRemovalPlan` via stop plan | Runtime planner | Partial | Unit Tested | Partial Parity | C# attaches per-online-invader kick/removal metadata. Live packet, teleport, alliance, participant, passed-player, and sync mutation remain disabled. |
| `com.aionemu.gameserver.model.gameobjects.Kisk` | `Aion.GameServer.Services.VortexStopInvaderKiskSnapshot` | Runtime metadata | Partial | Unit Tested | Partial Parity | C# records Kisk death intent only. Live Kisk controller death remains disabled. |
| `com.aionemu.gameserver.model.vortex.VortexStateType.PEACE` | `Aion.GameServer.Services.VortexStopPeaceSpawnSnapshot` | Runtime metadata | Partial | Unit Tested | Partial Parity | C# records PEACE spawn metadata only. Live spawn remains disabled. |

## Known Gaps

- Live Vortex `start`, `spawn`, `despawn`, and `stop` behavior remains unimplemented.
- Production Vortex creature/zone lifecycle adapter remains absent.
- Live Vortex zone-player and invader-Kisk membership maps remain disabled.
- Defender request storage, packet dispatch, request-handler callbacks, add-player transition, and alliance mutation remain metadata-only.
- Invader participant and alliance mutation remain metadata-only.
- Stop-time kick/removal metadata is not wired into production runtime paths.
- Full production XML coverage for Vortex spawns has not been compared against Java output.

## Remaining Risks

- Future live stop work must preserve Java ordering: active Vortex clear, Kisk controller death, online invader kick/removal, despawn, then PEACE spawn.
- Future live kick/removal work must preserve Java `kickPlayer` tail-call ordering for passed-player removal and `syncPassed(true)`.
- Passed-player count simulation depends on supplied invader ordering and snapshots.
- Current composition planners still need production adapters before they can observe real `Player`, alliance, passed-player, Kisk, spawn, and location state.

## Next Recommended UOW

[Phase 6] UOW-2494: Compose Vortex stopInvasion coordinator kick-removal report

The next smallest safe task is to surface stop-time `OrderedKickRemovalPlans` through the coordinator/reporting layer, including the static PEACE spawn enrichment path, without enabling live execution. This would make coordinator-level consumers see the same kick/removal metadata currently attached to the side-effect plan.

Safe alternative candidates:

- Add a production Vortex lifecycle adapter preview if real player/location snapshots become the next target.
- Compose Vortex `startInvasion` metadata further if start/stop orchestration becomes higher priority.

## Suggested Files To Inspect

- `game-server/src/com/aionemu/gameserver/services/vortex/Invasion.java`
- `dotnetConversion/src/Aion.GameServer/Services/VortexStopInvasionCoordinatorService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexStopInvasionSideEffectPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexKickPlayerRemovalPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`

## Suggested Validation

For Vortex stopInvasion coordinator kick/removal reporting:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests" --no-restore
```

Validation target: C# stopInvasion coordinator metadata reports Java-shaped stop ordering and per-online-invader kick/removal intent while preserving no-live packet, teleport, alliance, participant, passed-player, sync, Kisk death, despawn, or spawn mutation flags.

Java/Maven is not expected unless Java source or fixtures change or a narrow Java Vortex stop/removal fixture is added. Broad-validation trigger should be `none` if the UOW only adds non-live composition/reporting metadata and tests; it becomes present if live packet dispatch, teleport, alliance mutation, participant mutation, passed-player mutation, sync writes, scheduler dispatch, Kisk death, despawn, spawn, or zone-player/Kisk map mutation is enabled.
