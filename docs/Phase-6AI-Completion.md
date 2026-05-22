# Phase 6AI Completion Handoff

**Created**: May 22, 2026
**Status**: Phase 6 remains in progress; this handoff follows 6AH and covers Sessions 298-299.
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, persistence behavior, side effects, movement math, and naming.
**Workflow rule**: Do one focused unit of work, validate it, commit it, then repeat for as long as useful work remains.
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 554 tests.

---

## Recent Work Completed

- Extended `WorldNpcWalkerPlacementApplicationService` to remove inactive version variants from the live world, matching Java `InstanceWalkerFormations` behavior where only the selected `Rnd.get` variant is spawned.
- Added a parked inactive walker-variant store to `WorldNpcSpawnService` so hidden variant `WorldNpc` objects keep their object IDs, spawn metadata, route IDs, and templates for future `changeCluster` / `changeWalker` work instead of being released like normal despawns.
- Walker spawn-plan refreshes now include both live `WorldNpc` objects and parked inactive variants, keeping version-pool metadata available after inactive variants leave the world.
- Added `WorldNpcSpawnService.TrySwapInactiveWalkerVariant` as the first C# runtime primitive for Java `InstanceWalkerFormations.changeWalker`.
- The single-walker swap path spawns a parked inactive variant back into the world at its preserved spawn location, parks the previous active variant without releasing either object ID, and updates static-placeable state around the swap.
- Kept `docs/PHASE-6-PROGRESS.md` current through Session 299 and refreshed the validation baseline.

---

## Commits In This Handoff

- `a284a1ac9` - `Hide inactive world NPC walker variants`
- `d1c7c2e63` - `Swap inactive world NPC walker variants`

---

## Important Limits

- `TrySwapInactiveWalkerVariant` is only a runtime primitive. It is not yet invoked by NPC death, respawn, or future variant-change callbacks.
- Group/formation variant swaps remain pending; Java `InstanceWalkerFormations.changeCluster` has not been modeled yet.
- The cache is world-map scoped, not true `WorldMapInstance` scoped. Instance IDs and per-instance walker pools still need a real model.
- Movement state has not started yet: there is no current-step state, rest-time scheduling, target-reached callback, group wait/all-arrived synchronization, or move-controller equivalent.
- NPC `SM_MOVE` broadcasting remains pending. `SM_MOVE` currently has a player-shaped surface only.
- Random-walk and anchor behavior are still metadata only.
- Combat/life-stat systems still do not call the ordinary NPC death lifecycle, and registered-drop discovery is not wired into decay timing.
- Respawn parity still lacks Java event-end deferral, instance-existence checks, pooled respawn replacement, rift update callbacks, temporary-special object respawns, and full instance-aware pool state.
- NPC visibility remains a first-pass map/distance known-list proxy. It does not model Java region buckets, hide/see states, instance IDs, or persistent known-list membership yet.

---

## Suggested Next Units

1. Add a group/formation variant swap primitive mirroring Java `InstanceWalkerFormations.changeCluster`: activate a parked formation variant and park/despawn the current active formation group without releasing walker object IDs.
2. Wire `TrySwapInactiveWalkerVariant` into the future NPC death/variant-change callback once that caller boundary exists.
3. Add a runtime model for active walker movement state: current step index, target step, rest-time delay, group step, member shift metadata, and spawned/not-spawned variant status.
4. Extend movement packet support so NPC route-step target changes can broadcast Java-shaped `SM_MOVE` payloads through the visible-player registry.
5. Implement the first `WalkManager.startRouteWalking` bridge for spawned world NPCs using `WorldNpcWalkerRouteStepService` and the active placement cache.
6. Add `targetReached` and rest-time scheduling for single walkers, then group walkers: wait state, all-arrived checks, step synchronization, and last-step wrap.
7. Add random-walk and anchor runtime behavior for NPCs without walker routes but with preserved `random_walk` / `anchor` spawn metadata.
8. Connect future combat/life-stat NPC death callers to `WorldNpcSpawnService.TryScheduleWorldNpcDeath`, keeping Java `NpcController.onDie` order.
9. Add registered-drop discovery so decay can choose Java no-drop (`2s`) versus with-drop (`5m`) timing from real drop state.
10. Continue respawn parity with Java event-end deferral, instance-existence checks, pooled respawn replacement, rift `updateSpawned`, and temporary-spawn unregister behavior for object ID changes.
11. Keep the broader Phase 6 backlog in `docs/PHASE-6-PROGRESS.md` as the source of the next non-walker priorities.
