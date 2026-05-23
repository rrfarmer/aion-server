# Phase 6BW Completion Handoff

**Created**: May 23, 2026  
**Status**: Phase 6 remains in progress; this handoff follows 6BV and covers Sessions 481-486.  
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, world/known-list behavior, persistence behavior, scheduling, stat formulas, observer side effects, and combat math.  
**Workflow rule**: Do one focused unit of work, validate it, update the migration parity table, commit it, then repeat for as long as useful work remains.  
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.  
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx` passes with 1033 tests.

---

## Recent Work Completed

- Added `CreaturePvpZoneTable`, `CreaturePvpZoneSummary`, and `CreaturePvpZoneType` to load Java static `PVP` zones plus fortress `FORT` zones that feed Java `ZoneType.SIEGE` counters.
- Added `CreaturePvpZoneRevalidationService` to evaluate loaded PVP/FORT zone geometry and feed stable Java zone-name memberships into `CreaturePvpZoneCounterService.ApplyZoneEnter` / `ApplyZoneLeave`.
- Wired player movement revalidation through the existing `GameServerConnection.RevalidatePlayerFlightZonesAsync` path, preserving current FLY/NO_FLY behavior while adding PVP/FORT counter updates.
- Added packet-level coverage proving movement-fed PVP counters flow into viewer-specific kisk `SmNpcInfo` creature-type selection.
- Wired kisk revive teleport through `TeleportPlayerToKiskPositionAsync`, revalidating PVP/FORT counters after `PlayerTeleportService.TeleportToKiskPosition`.
- Added player leave-world cleanup so movement/teleport-created PVP/FORT counters do not survive logout/disconnect.
- Wired regular `WorldNpcSpawnService` spawn/despawn to enter and clear PVP/FORT counters for materialized `WorldNpc` instances.
- Updated `docs/PHASE-6-PROGRESS.md` through Session 486 with migration parity tables, risks, metrics, validation, and next recommended work.

---

## Commits In This Handoff

- `7f98ff865` - `Load creature pvp zone table`
- `6c71fad31` - `Revalidate pvp zones on player move`
- `3438310c1` - `Test kisk npc info after pvp revalidation`
- `fcd343787` - `Revalidate pvp zones after kisk teleport`
- `e778b2e05` - `Clear pvp zone counters on player leave`
- `3455008db` - `Track pvp zones for spawned npcs`

---

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|
| `ZoneService` / zone template loading | `StaticData.CreaturePvpZones` + `CreaturePvpZoneTable` | Partial | Regression Tested | Partial Parity | Loads 132 PVP zones and 20 fortress FORT zones from Java static data. Full Java zone service, handlers, priorities, full-map zones, and non-PVP zone types remain unported. |
| `PvPZoneInstance` | `CreaturePvpZoneRevalidationService` + `CreaturePvpZoneCounterService` | Partial | Unit + Integration Tested | Partial Parity | PVP enter/leave counters now work for loaded static geometry in player movement, kisk teleport, and regular NPC spawn/despawn slices. |
| `FortressLocation` SIEGE zone side effects | `CreaturePvpZoneType.Siege` for fortress FORT zones | Partial | Unit Tested | Partial Parity | Fortress FORT zones feed SIEGE counters. Fortress shield observers, balance buffs, race/vulnerability logic, and broader siege behavior remain missing. |
| `ArtifactLocation` | No SIEGE counter mapping | Not Started | No Tests | Needs Verification | Artifact zones are not mapped to SIEGE counters because Java `ArtifactLocation` does not set `ZoneType.SIEGE`; activation/status behavior remains unported. |
| `Creature.revalidateZones()` / `MapRegion.revalidateZones` | `CreaturePvpZoneRevalidationService.Revalidate` | Partial | Unit + Integration Tested | Partial Parity | Direct C# service evaluates PVP/FORT zones. Java queued `ZoneUpdateService`, `ZoneLevelService`, sorted priorities, and handler callbacks remain open. |
| `CM_MOVE` zone update path | `GameServerConnection.RevalidatePlayerFlightZonesAsync` | Partial | Integration Tested | Intentional Difference | C# performs immediate revalidation in the existing flight-zone path; Java queues movement zone updates through `ZoneUpdateService`. |
| `SM_NPC_INFO(Npc, Player)` / `Kisk.getType(Player)` | `GameClientSocketServer.CreateNpcInfoPacketForViewer` + `PlayerKiskNpcInfoPacketService` | Partial | Packet Tested | Partial Parity | Viewer-specific kisk packet creature type responds to movement-fed PVP counters. Full encrypted socket ordering and combat enforcement remain unverified. |
| `PlayerReviveService.kiskRevive` / `TeleportService.teleportTo` | `GameServerConnection.TeleportPlayerToKiskPositionAsync` | Partial | Integration Tested | Partial Parity | Kisk revive teleport now refreshes PVP/FORT counters after position mutation. Full revive, teleport, world spawn, pet, protection, and instance/legion side effects remain incomplete. |
| `World.despawn` / `ZoneInstance.onLeave` cleanup | `LeavePlayerWorldAsync`, `TryDespawnWorldNpc`, and counter `ClearCounters` | Partial | Integration Tested | Intentional Difference | C# clears modeled PVP/SIEGE counters directly where full Java region/zone instances are not available. Controller handlers and zone leave side effects are not executed. |
| `SpawnEngine.spawnObject` / `VisibleObjectSpawner` | `WorldNpcSpawnService.SpawnNpc` | Partial | Integration Tested | Partial Parity | Regular spawned NPCs now enter PVP/FORT counters. Walker variants, kisk toy-pet creation, rift/vortex NPCs, static objects, gatherables, and direct world removals still need review. |

Metrics from this handoff window:

- Total focused sessions covered: 6
- Total commits covered: 6
- Current full validation baseline: 1033 tests passing
- Total artifacts with verified parity newly promoted in this window: 0
- Total blocked artifacts: queued zone scheduler/levels, Java zone handlers/controller callbacks, remaining teleport/map-change callers, NPC movement revalidation, generic world removal cleanup, walker/formation/rift/kisk/static spawn paths, full socket ordering, and dedicated kisk controller/combat behavior
- Estimated overall migration completion: Phase 6 remains about 56% complete as a conservative game-core estimate; remaining kisk revive cleanup, live team membership wiring, production socket-order validation, broader teleport/map-change and NPC movement revalidation wiring, generic visible-object cleanup, full dedicated kisk controller/AI, full NPC/dialog AI, resurrection skill/effect callers, per-zone bind membership, live option mutation callers, admin option consumers, world-map instance ownership, object iteration, full movement-controller parity, full audit subsystem, full stat/effect runtime, combat, loot distribution, dynamic handlers, instances, and quests remain broad open areas.

---

## Important Limits

- Revalidation is immediate in C#; Java movement uses `ZoneUpdateService` FIFO periodic scheduling. Timing and batching parity are not verified.
- The PVP/FORT table only models polygon zones discovered in current Java static data. Other zone classes and non-polygon shapes remain outside this slice.
- C# direct counter cleanup avoids stale state but does not execute Java `onLeaveZone` controller callbacks, quest/material handlers, fortress observers, instance callbacks, or conqueror/protector side effects.
- Player movement, kisk revive teleport, player leave, regular NPC spawn, and regular NPC despawn are wired. NPC movement, walker/random-walk updates, walker variant swaps, direct `World.TryRemoveObject` callers, rift/vortex NPCs, postman NPCs, kisk toy-pet creation, static objects, and gatherables remain incomplete.
- Viewer-specific kisk `SmNpcInfo` is packet-tested, but full encrypted client socket ordering is still not validated in a live session.
- SIEGE/FORT counter loading and unit behavior are covered, but real static-data spawn/despawn regression currently exercises only a PVP zone.

---

## Next Unit Of Work

Recommended next unit: continue NPC zone lifecycle parity by wiring walker/random-walk NPC movement updates into `CreaturePvpZoneRevalidationService`, or audit remaining direct `World.TryRemoveObject` NPC removal paths for PVP/FORT counter cleanup.

1. Re-read Java:
   - `game-server/src/com/aionemu/gameserver/world/World.java`
   - `game-server/src/com/aionemu/gameserver/world/MapRegion.java`
   - `game-server/src/com/aionemu/gameserver/world/zone/ZoneInstance.java`
   - `game-server/src/com/aionemu/gameserver/world/zone/PvPZoneInstance.java`
   - `game-server/src/com/aionemu/gameserver/controllers/CreatureController.java`
   - `game-server/src/com/aionemu/gameserver/taskmanager/tasks/MoveTaskManager.java`
2. Re-read C#:
   - `WorldNpcSpawnService`
   - `WorldNpcWalkerRouteWalkingService`
   - `WorldNpcRandomWalkService`
   - `WorldNpcWalkerMovementBroadcastService`
   - `CreaturePvpZoneRevalidationService`
   - `CreaturePvpZoneCounterService`
3. Keep the first unit narrow:
   - choose one concrete NPC movement path
   - revalidate PVP/FORT counters after position mutation
   - preserve existing movement broadcast ordering
   - add a real static-data PVP regression and, if practical, a SIEGE/FORT regression

Strong alternatives:

1. Audit remaining direct `World.TryRemoveObject` callers and add targeted counter cleanup where removed objects can be `WorldNpc`.
2. Add broader socket-order tests around viewer-specific kisk `SmNpcInfo` followed by loot-status/deletion packets.
3. Wire remaining teleport/map-change completion paths outside kisk revive.
4. Continue dedicated `KiskController`/AI/dialog/death layering beyond the generic death bridge.

Useful validation commands:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldNpcSpawnServiceTests|FullyQualifiedName~CreaturePvpZoneRevalidationServiceTests|FullyQualifiedName~CreaturePvpZoneCounterServiceTests|FullyQualifiedName~GameServerBootstrapTests"
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionFlightZoneFanoutTests|FullyQualifiedName~GameClientSocketServerNpcVisibilityTests|FullyQualifiedName~PlayerKiskNpcInfoPacketServiceTests"
dotnet test dotnetConversion\AionServer.slnx
```

---

## Resume Checklist

- Start from branch `4.8`.
- Confirm `git status --short` is clean.
- Read `docs/csharp-port.md`, `docs/PHASE-6-PROGRESS.md` Sessions 481-486, `docs/Phase-6BV-Completion.md`, and this handoff.
- Continue with one focused Java-parity unit.
- Update the migration parity table before committing.
- Commit the unit, then repeat until the next handoff is needed.
