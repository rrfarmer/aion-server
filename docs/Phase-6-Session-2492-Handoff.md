# Phase 6 Session 2492 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2492: Compose Vortex kickPlayer removal metadata

## Commits Made

- `[Phase 6][UOW-2492] Compose Vortex kick removal metadata`

## Summary

UOW-2492 added a non-live kick/removal planner for Java `Invasion.kickPlayer`. The C# planner records participant removal, alliance removal/message intent, disbanded-alliance reference clearing, invader direct-portal message/teleport intent, passed-player removal, and `syncPassed(true)` metadata while preserving disabled live side effects.

This remains partial parity. Live participant maps, alliance mutation, packet dispatch, teleport, passed-player mutation, sync writes, and production player/location adapters remain disabled.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/VortexKickPlayerRemovalPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`
- `docs/Phase-6-Session-2492-Completion.md`
- `docs/Phase-6-Session-2492-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.services.vortex.Invasion.kickPlayer`
- `com.aionemu.gameserver.model.team.alliance.PlayerAlliance`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE`
- `com.aionemu.gameserver.services.teleport.TeleportService.teleportTo`

## C# Artifacts Touched

- `Aion.GameServer.Services.VortexKickPlayerRemovalPlanService`
- `Aion.GameServer.Services.VortexKickPlayerRemovalPlan`
- `Aion.GameServer.Services.VortexKickPlayerRemovalPlanStatus`
- `Aion.GameServer.Services.VortexKickPlayerSnapshot`
- `Aion.GameServer.Services.VortexKickPlayerAllianceSnapshot`

## Validation Completed

Validation target: C# kickPlayer removal metadata mirrors Java participant removal, alliance-member removal/message intent, disbanded-alliance nulling, invader direct-portal teleport/message intent, passed-player removal, `syncPassed(true)` intent, and no-live-mutation flags.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests" --no-restore
```

- Passed: 90 tests.
- Existing nullable/analyzer warnings were emitted from unrelated files.

```powershell
git diff --check
git diff --cached --check
```

- `git diff --check` passed. Git reported an expected CRLF conversion warning for the edited test file.
- `git diff --cached --check` passed after staging. Git reported expected CRLF conversion warnings for edited/new files.

Java/Maven validation was skipped because no Java source or fixtures changed and no narrow Java Vortex kick/removal fixture exists. Java source was reviewed directly. Broad-validation trigger was `none` because this UOW only adds non-live composition metadata and tests.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.vortex.Invasion.kickPlayer` | `Aion.GameServer.Services.VortexKickPlayerRemovalPlanService` | Runtime planner | Partial | Unit Tested | Partial Parity | C# composes kick/removal branches as metadata. Live participant, alliance, packet, teleport, passed-player, and sync mutation remain disabled. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAlliance` | `Aion.GameServer.Services.VortexKickPlayerAllianceSnapshot` | Runtime metadata | Partial | Unit Tested | Partial Parity | C# records only alliance existence, membership, and disband-after-removal snapshots needed by kickPlayer branches. Full alliance runtime behavior remains elsewhere. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Services.VortexKickPlayerRemovalPlan` | Runtime metadata | Partial | Unit Tested | Partial Parity | C# records Java message ids `1401452`, `1401476`, and `1401474` as send intent only. Live packet dispatch remains disabled. |
| `com.aionemu.gameserver.services.teleport.TeleportService.teleportTo` | `Aion.GameServer.Services.VortexKickPlayerRemovalPlan` | Runtime metadata | Partial | Unit Tested | Partial Parity | C# records invader home-teleport intent only when online in the invasion world. Live teleport remains disabled. |

## Known Gaps

- Live Vortex `start`, `spawn`, `despawn`, and `stop` behavior remains unimplemented.
- Production Vortex creature/zone lifecycle adapter remains absent.
- Live Vortex zone-player and invader-Kisk membership maps remain disabled.
- Defender request storage, packet dispatch, request-handler callbacks, add-player transition, and alliance mutation remain metadata-only.
- Invader participant and alliance mutation remain metadata-only.
- Kick/removal metadata is not wired into production runtime paths.
- Full production XML coverage for Vortex spawns has not been compared against Java output.

## Remaining Risks

- Future live kick/removal work must preserve Java ordering: participant removal, optional alliance removal/message, optional invader direct-portal message/teleport, passed-player removal, then `syncPassed(true)`.
- Java concurrent map behavior is represented only by supplied snapshots.
- Java alliance disbanding after removal is represented only by supplied metadata.
- Current composition planners still need production adapters before they can observe real `Player`, alliance, passed-player, and location state.

## Next Recommended UOW

[Phase 6] UOW-2493: Compose Vortex stopInvasion side-effect metadata with kick removal

The next smallest safe task is to connect existing `stopInvasion` side-effect metadata with the new non-live kick/removal planner. Java source: `Invasion.stopInvasion` clears active vortex, kills invader Kisks, kicks online invaders, despawns invasion state, and respawns peace state. The C# target should record per-invader kick/removal metadata for stop-time online invaders without enabling live packet dispatch, teleport, alliance mutation, passed-player mutation, Kisk death, despawn, spawn, or sync writes.

Safe alternative candidates:

- Add a production Vortex lifecycle adapter preview if real player/location snapshots become the next target.
- Compose Vortex `startInvasion` metadata further if start/stop orchestration becomes higher priority.

## Suggested Files To Inspect

- `game-server/src/com/aionemu/gameserver/services/vortex/Invasion.java`
- `dotnetConversion/src/Aion.GameServer/Services/VortexStopInvasionSideEffectPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexStopInvasionCoordinatorService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexKickPlayerRemovalPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`

## Suggested Validation

For Vortex stopInvasion/kick-removal composition metadata:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests" --no-restore
```

Validation target: C# stopInvasion metadata composes Java-shaped stop side-effect ordering with per-online-invader kick/removal intent while preserving no-live packet, teleport, alliance, participant, passed-player, sync, Kisk death, despawn, or spawn mutation flags.

Java/Maven is not expected unless Java source or fixtures change or a narrow Java Vortex stop/removal fixture is added. Broad-validation trigger should be `none` if the UOW only adds non-live composition metadata and tests; it becomes present if live packet dispatch, teleport, alliance mutation, participant mutation, passed-player mutation, sync writes, scheduler dispatch, Kisk death, despawn, spawn, or zone-player/Kisk map mutation is enabled.
