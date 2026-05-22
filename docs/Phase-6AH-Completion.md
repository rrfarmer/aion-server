# Phase 6AH Completion Handoff

**Created**: May 22, 2026
**Status**: Phase 6 remains in progress; this handoff follows 6AG and covers Sessions 294-297.
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, persistence behavior, side effects, movement math, and naming.
**Workflow rule**: Do one focused unit of work, validate it, commit it, then repeat for as long as useful work remains.
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 552 tests.

---

## Recent Work Completed

- Added `WorldNpcWalkerSpawnPlanCacheService` as the first live-world bridge for Java `spawnengine/InstanceWalkerFormations`, scoped by world map until true `WorldMapInstance` support exists.
- Wired `WorldNpcSpawnService` to refresh cached walker plans after batch spawns, ordinary despawns, shutdown, and scheduled respawns when static walker tables are loaded.
- Added `WorldNpcWalkerPlacementPlanService` so selected spawn plans now expose active placements and inactive version-variant object IDs.
- Carried selected single-walker heading through `WorldNpcWalkerSpawnCandidate`, matching Java `ClusteredNpc.spawn` use of `SpawnTemplate.getHeading()`.
- Added `WorldNpc.SpawnPosition` / `SpawnLocation` so C# preserves Java `SpawnTemplate` coordinates after runtime NPC positions change.
- Updated walker organization, formation math, spawn candidates, and placement plans to use `SpawnLocation` for Java `ClusteredNpc` position hashes, formation origins, selected walker coordinates, and spawn Z/heading.
- Added `WorldNpcWalkerPlacementApplicationService` and a world-container update primitive, then wired `WorldNpcSpawnService` to apply active placements to live `WorldNpc.Position` after selected walker plans refresh.
- Selected walker formations now update live NPC positions to the C# `WalkerGroup.form` output while keeping original spawn metadata intact for later refreshes and respawns.
- Kept `docs/PHASE-6-PROGRESS.md` current through Session 297 and refreshed the validation baseline.

---

## Commits In This Handoff

- `2c3ce3305` - `Cache world NPC walker spawn plans`
- `2e2d7e9c8` - `Model world NPC walker placements`
- `a2a872803` - `Preserve world NPC spawn positions`
- `7c1fcbf57` - `Apply world NPC walker placements`

---

## Important Limits

- Active selected walker placements are now applied to live NPC positions, but unselected version variants are still visible in the world. Java keeps unselected variants unspawned until a later variant swap.
- The cache is world-map scoped, not true `WorldMapInstance` scoped. Instance IDs and per-instance walker pools still need a real model.
- Movement state has not started yet: there is no current-step state, rest-time scheduling, target-reached callback, group wait/all-arrived synchronization, or move-controller equivalent.
- NPC `SM_MOVE` broadcasting remains pending. `SM_MOVE` currently has a player-shaped surface only.
- Random-walk and anchor behavior are still metadata only.
- Combat/life-stat systems still do not call the ordinary NPC death lifecycle, and registered-drop discovery is not wired into decay timing.
- Respawn parity still lacks Java event-end deferral, instance-existence checks, pooled respawn replacement, rift update callbacks, temporary-special object respawns, and full instance-aware pool state.
- NPC visibility remains a first-pass map/distance known-list proxy. It does not model Java region buckets, hide/see states, instance IDs, or persistent known-list membership yet.

---

## Suggested Next Units

1. Hide inactive walker version variants from the world without losing enough object/spawn metadata to support future `changeCluster` / `changeWalker` swaps.
2. Add a runtime model for active walker movement state: current step index, target step, rest-time delay, group step, member shift metadata, and spawned/not-spawned variant status.
3. Extend movement packet support so NPC route-step target changes can broadcast Java-shaped `SM_MOVE` payloads through the visible-player registry.
4. Implement the first `WalkManager.startRouteWalking` bridge for spawned world NPCs using `WorldNpcWalkerRouteStepService` and the active placement cache.
5. Add `targetReached` and rest-time scheduling for single walkers, then group walkers: wait state, all-arrived checks, step synchronization, and last-step wrap.
6. Add random-walk and anchor runtime behavior for NPCs without walker routes but with preserved `random_walk` / `anchor` spawn metadata.
7. Connect future combat/life-stat NPC death callers to `WorldNpcSpawnService.TryScheduleWorldNpcDeath`, keeping Java `NpcController.onDie` order.
8. Add registered-drop discovery so decay can choose Java no-drop (`2s`) versus with-drop (`5m`) timing from real drop state.
9. Continue respawn parity with Java event-end deferral, instance-existence checks, pooled respawn replacement, rift `updateSpawned`, and temporary-spawn unregister behavior for object ID changes.
10. Keep the broader Phase 6 backlog in `docs/PHASE-6-PROGRESS.md` as the source of the next non-walker priorities.
