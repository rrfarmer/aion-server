# Phase 6AG Completion Handoff

**Created**: May 22, 2026
**Status**: Phase 6 remains in progress; this handoff follows 6AF and covers Sessions 290-293.
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, persistence behavior, side effects, movement math, and naming.
**Workflow rule**: Do one focused unit of work, validate it, commit it, then repeat for as long as useful work remains.
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 545 tests.

---

## Recent Work Completed

- Added `WorldNpcWalkerFormationService` as the first C# port of Java `spawnengine/WalkerGroup.form` and `WalkerGroup.getLinePoint`.
- Ported square walker formation math for line and multi-row groups, including descending walker-index ordering, row parity distances, sagittal/coronal shifts, and Java's horizontal/vertical projection quirks.
- Added `WorldNpcWalkerFormationOrganizerService` as a pure-data slice of Java `spawnengine/InstanceWalkerFormations.organizeAndSpawn`.
- The organizer resolves spawned `WorldNpc.WalkerId` values, groups candidates by route ID, mirrors Java `ClusteredNpc.getPositionHash` float-bit grouping, selects the largest aligned spawn-position cluster, models pool/alignment warnings, and separates active walkers/formations from versioned variants.
- Carried Java walker `pool` and `loop_type` through `WorldNpcWalkerRoutePlan`.
- Added `WorldNpcWalkerRouteStepService` to model Java `WalkManager.findClosestRouteStep`, `NpcMoveController.isNextRouteStepChosen`, and `NpcMoveController.setRouteStep` route target behavior.
- Route-step targets now cover closest-step selection, last-step wrap to route index `0`, single-walker `LoopType.NONE` stop-at-last behavior, and formation-member projection using stored `WalkerGroup` shifts between current and next route steps.
- Added `WorldNpcWalkerVariantSelectionService` to model Java `Rnd.get` activation of one formation variant per versioned formation pool and one walker variant per versioned single-walker pool.
- Kept `docs/PHASE-6-PROGRESS.md` current through Session 293 and refreshed the validation baseline.

---

## Commits In This Handoff

- `ac9d9b524` - `Form world NPC walker groups`
- `13a85b7f3` - `Organize world NPC walker formations`
- `6b995d37a` - `Plan world NPC walker route steps`
- `8ad8edf4f` - `Select world NPC walker variants`

---

## Important Limits

- The new walker services are pure data. They do not yet register live per-instance `InstanceWalkerFormations` caches or mutate world NPC positions.
- Version-pool selection is modeled, but runtime code does not yet call it during world/instance spawn.
- Route-step targets are modeled, but C# still lacks live NPC walker state, move controller state, timers, rest-time scheduling, `targetReached` callbacks, and group-step synchronization.
- `SM_MOVE` currently exists for player movement payloads; NPC movement broadcasting still needs a creature/NPC-compatible packet surface and known-list fanout.
- AI `WalkManager` state/substate behavior, start/stop walking, random walking, anchor behavior, return-to-spawn, and attack/think event interactions remain pending.
- Combat/life-stat systems still do not call the ordinary NPC death lifecycle, and registered-drop discovery is not wired into decay timing.
- Respawn parity still lacks Java event-end deferral, instance-existence checks, pooled respawn replacement, rift update callbacks, temporary-special object respawns, and full instance-aware pool state.
- NPC visibility remains a first-pass map/distance known-list proxy. It does not model Java region buckets, hide/see states, instance IDs, or persistent known-list membership yet.
- Static placeable state is still an in-memory bridge. Full Java `GeoService` collision integration and static object models remain pending.

---

## Suggested Next Units

1. Wire walker organization into `WorldNpcSpawnService` or a dedicated world-instance walker cache: collect spawned walker NPCs, run `WorldNpcWalkerFormationOrganizerService`, then apply `WorldNpcWalkerVariantSelectionService`.
2. Add a runtime model for active walker state: current step index, next target, rest-time delay, group step, spawned/not-spawned variant status, and member shift metadata.
3. Extend movement packet support so NPC route-step target changes can broadcast Java-shaped `SM_MOVE` payloads through the existing visible-player registry.
4. Implement the first `WalkManager.startRouteWalking` bridge for spawned world NPCs using `WorldNpcWalkerRouteStepService`, without random walking yet.
5. Add `targetReached` / rest-time scheduling behavior for single walkers, then group walkers: group wait state, all-arrived checks, step synchronization, and last-step wrap.
6. Add random-walk and anchor runtime behavior for NPCs without walker routes but with preserved `random_walk` / `anchor` spawn metadata.
7. Connect future combat/life-stat NPC death callers to `WorldNpcSpawnService.TryScheduleWorldNpcDeath`, keeping Java `NpcController.onDie` order.
8. Add registered-drop discovery so decay can choose Java no-drop (`2s`) versus with-drop (`5m`) timing from real drop state.
9. Continue respawn parity with Java event-end deferral, instance-existence checks, pooled respawn replacement, rift `updateSpawned`, and temporary-spawn unregister behavior for object ID changes.
10. Keep the broader Phase 6 backlog in `docs/PHASE-6-PROGRESS.md` as the source of the next non-walker priorities.
