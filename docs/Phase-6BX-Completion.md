# Phase 6BX Completion Handoff

**Created**: May 23, 2026  
**Status**: Phase 6 remains in progress; this handoff follows 6BW and covers Sessions 487-491.  
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, world/known-list behavior, persistence behavior, scheduling, stat formulas, observer side effects, and combat math.  
**Workflow rule**: Do one focused unit of work, validate it, update the migration parity table, commit it, then repeat for as long as useful work remains.  
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.  
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx` passes with 1039 tests.

---

## Recent Work Completed

- Wired `WorldNpcRandomWalkService.TargetReachedAsync` to revalidate PVP/FORT counters after random-walk NPC arrival position updates.
- Wired `WorldNpcWalkerRouteWalkingService.TargetReachedAsync` to revalidate PVP/FORT counters after route-walker arrival position updates, using the shared boundary for single walkers and formation members.
- Stabilized a race-prone random-walk interpolation test by removing an exact assertion on a transient pending movement-tick task.
- Wired walker-version placement and swap lifecycle hooks so inactive variants clear counters when parked and activated variants revalidate counters after being spawned/moved into the world.
- Wired rift-owned NPC direct spawn/close/rollback paths in `RiftManagerService` and `RiftService` to revalidate and clear PVP/FORT counters outside `WorldNpcSpawnService`.
- Wired kisk item-use spawn to revalidate PVP/FORT counters after the kisk NPC is inserted into the world, and wired save-failure rollback cleanup to clear counters if the inserted kisk is removed.
- Updated `docs/PHASE-6-PROGRESS.md` through Session 491 with migration parity tables, risks, metrics, validation, and next recommended work.

---

## Commits In This Handoff

- `6aadd0b1b` - `Revalidate pvp zones after random walk arrival`
- `14470e874` - `Revalidate pvp zones after route walker arrival`
- `42cfd9ed7` - `Clear pvp counters for walker variant swaps`
- `4acaee129` - `Clean pvp counters for rift npc removals`
- `4808692b3` - `Revalidate pvp zones for spawned kisks`

---

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|
| `WalkManager` random-walk target arrival | `WorldNpcRandomWalkService.TargetReachedAsync` | Partial | Integration Tested | Partial Parity | Random-walk NPCs now revalidate PVP/FORT counters after authoritative arrival position mutation. Java AI event machine, geo/collision correction, and queued zone scheduler remain open. |
| `WalkManager` route target arrival / `WalkerGroup.targetReached` | `WorldNpcWalkerRouteWalkingService.TargetReachedAsync` | Partial | Integration Tested | Partial Parity | Route-walker arrival revalidates counters before next route-step selection or formation group handling. Dedicated formation PVP/SIEGE geometry regression remains open. |
| `InstanceWalkerFormations` active/inactive placement | `WorldNpcSpawnService.RefreshWalkerSpawnPlans` | Partial | Regression Tested | Partial Parity | Inactive variants removed after initial spawn clear counters; active variants moved by placement revalidate counters. Full instance-specific callbacks remain incomplete. |
| `InstanceWalkerFormations.changeWalker/changeCluster` | `WorldNpcSpawnService.TrySwapInactiveWalkerVariant` / `TrySwapInactiveWalkerFormationVariant` | Partial | Integration Tested | Partial Parity | Single and formation variant swaps clear parked counters and revalidate activated variants. Trigger callers from death/AI/instance callbacks remain future work. |
| `RiftManager.spawnRift` | `RiftManagerService.SpawnRift` | Partial | Integration Tested | Partial Parity | Rift portal NPCs now revalidate counters on direct manager spawn and clear counters during partial rollback. Full Java rift handlers and live instance side effects remain incomplete. |
| `RiftService.openRifts/closeRift` | `RiftService.TrySpawnRiftOwnedNpc` / `CloseRifts` | Partial | Integration Tested | Partial Parity | Rift guard NPCs now enter counters on spawn and clear on close or stale tracked removal. SIEGE/FORT rift coverage and reused-id stress remain open. |
| `ToyPetSpawnAction.act` / `VisibleObjectSpawner.spawnKisk` | `GameServerConnection.CompleteToyPetSpawnUseItemAsync` | Partial | Integration Tested | Partial Parity | Kisk item-use spawn now revalidates counters after world insertion and rollback cleanup is wired. Dedicated save-failure rollback regression remains open. |
| `World.spawn` / `World.despawn` direct visible-object paths | Direct hooks in movement, spawn, rift, walker, and kisk boundaries | Partial | Unit + Integration Tested | Intentional Difference | C# calls modeled counter services immediately where full Java map regions/zone instances are not ported. Java zone handlers/controller callbacks are still missing. |
| `Creature.revalidateZones` / `MapRegion.revalidateZones` | `CreaturePvpZoneRevalidationService.Revalidate` | Partial | Unit + Integration Tested | Partial Parity | PVP/FORT polygon membership now feeds players, regular NPCs, movement paths, rifts, walker variants, and spawned kisks. Full zone priority, full-map zones, neighboring regions, and scheduler parity remain incomplete. |
| `PvPZoneInstance.onEnter/onLeave` | `CreaturePvpZoneCounterService` | Partial | Unit + Integration Tested | Partial Parity | Tests cover PVP counter entry/leave for several player and NPC boundaries. Java handler ordering, controller callbacks, and combat enforcement remain open. |

Metrics from this handoff window:

- Total focused sessions covered: 5
- Total commits covered: 5
- Current full validation baseline: 1039 tests passing
- Total artifacts with verified parity newly promoted in this window: 0
- Total blocked artifacts: queued zone scheduler/levels, Java zone handlers/controller callbacks, geo/collision validation, formation PVP/SIEGE geometry regressions, SIEGE/FORT rift/kisk coverage, kisk save-failure rollback test fixture, remaining direct `World.TryRemoveObject` cleanup, postman/player-enter rollback removals, live socket ordering, and dedicated kisk controller/combat behavior
- Estimated overall migration completion: Phase 6 remains about 56% complete as a conservative game-core estimate; remaining kisk revive cleanup, live team membership wiring, production socket-order validation, broader teleport/map-change zone revalidation, remaining direct-removal cleanup, generic visible-object cleanup, full dedicated kisk controller/AI, full NPC/dialog AI, resurrection skill/effect callers, per-zone bind membership, live option mutation callers, admin option consumers, world-map instance ownership, object iteration, full movement-controller parity, full audit subsystem, full stat/effect runtime, combat, loot distribution, dynamic handlers, instances, and quests remain broad open areas.

---

## Important Limits

- Revalidation remains immediate in C#; Java movement and controller zone updates use `ZoneUpdateService` FIFO periodic scheduling. Timing and batching parity are not verified.
- C# direct counter cleanup prevents stale modeled state but does not execute Java `onLeaveZone` controller callbacks, quest/material handlers, fortress observers, instance callbacks, or conqueror/protector side effects.
- Most new regressions use synthetic PVP geometry for compact fixtures; random-walk and kisk spawn tests use real Java static PVP data. SIEGE/FORT movement, rift, kisk, and variant coverage remains thin.
- Formation route and formation variant hooks are wired through shared helper calls, but direct formation PVP/SIEGE geometry assertions still need a dedicated compact fixture.
- Kisk item-use save-failure rollback cleanup is wired, but the failure branch lacks a dedicated regression because the connection currently depends on concrete `PlayerEnterWorldService` persistence behavior.
- Direct `World.TryRemoveObject` callers still needing review include postman removal and player-enter rollback/removal paths. Some player leave cleanup is already covered.
- Full Java zone service behavior remains much broader than the current counter model: priorities, handlers, full-map zones, zone levels, neighboring region checks, and zone-specific side effects are not ported.

---

## Next Unit Of Work

Recommended next unit: continue direct removal parity by reviewing postman removal and player-enter rollback paths for modeled creature-zone cleanup, then add a dedicated kisk save-failure rollback regression once the persistence dependency can be isolated cleanly.

1. Re-read Java:
   - `game-server/src/com/aionemu/gameserver/world/World.java`
   - `game-server/src/com/aionemu/gameserver/world/MapRegion.java`
   - `game-server/src/com/aionemu/gameserver/world/zone/ZoneInstance.java`
   - `game-server/src/com/aionemu/gameserver/world/zone/PvPZoneInstance.java`
   - `game-server/src/com/aionemu/gameserver/model/templates/item/actions/ToyPetSpawnAction.java`
   - `game-server/src/com/aionemu/gameserver/spawnengine/VisibleObjectSpawner.java`
   - `game-server/src/com/aionemu/gameserver/services/player/PlayerEnterWorldService.java`
2. Re-read C#:
   - `GameServerConnection`
   - `PlayerEnterWorldService`
   - `PostmanNpc`
   - `CreaturePvpZoneRevalidationService`
   - `CreaturePvpZoneCounterService`
3. Keep the first next unit narrow:
   - choose one direct world-removal path
   - clear modeled PVP/FORT counters only when the removed object is the expected creature
   - preserve existing packet/order behavior
   - add a focused regression and update `docs/PHASE-6-PROGRESS.md`

Strong alternatives:

1. Add a compact formation route-walker or formation variant PVP/SIEGE regression now that shared hooks are wired.
2. Wire remaining teleport/map-change completion paths outside kisk revive.
3. Add broader socket-order tests around viewer-specific kisk `SmNpcInfo` followed by loot-status/deletion packets.
4. Continue dedicated `KiskController`/AI/dialog/death layering beyond the generic death bridge.

Useful validation commands:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionFlightZoneFanoutTests|FullyQualifiedName~PlayerKiskSpawnServiceTests|FullyQualifiedName~PlayerKiskRemovalRuntimeCleanupServiceTests|FullyQualifiedName~CreaturePvpZoneRevalidationServiceTests|FullyQualifiedName~CreaturePvpZoneCounterServiceTests"
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldNpcSpawnServiceTests|FullyQualifiedName~WorldNpcWalkerRouteWalkingServiceTests|FullyQualifiedName~WorldNpcRandomWalkServiceTests|FullyQualifiedName~CreaturePvpZoneCounterServiceTests"
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~RiftManagerServiceTests|FullyQualifiedName~RiftServiceTests|FullyQualifiedName~CreaturePvpZoneRevalidationServiceTests|FullyQualifiedName~CreaturePvpZoneCounterServiceTests"
dotnet test dotnetConversion\AionServer.slnx
```

---

## Resume Checklist

- Start from branch `4.8`.
- Confirm `git status --short` is clean.
- Read `docs/csharp-port.md`, `docs/PHASE-6-PROGRESS.md` Sessions 487-491, `docs/Phase-6BW-Completion.md`, and this handoff.
- Continue with one focused Java-parity unit.
- Update the migration parity table before committing.
- Commit the unit, then repeat until the next handoff is needed.
