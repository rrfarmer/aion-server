# Phase 6AJ Completion Handoff

**Created**: May 22, 2026
**Status**: Phase 6 remains in progress; this handoff follows 6AI and covers Sessions 300-304.
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, persistence behavior, side effects, movement math, and naming.
**Workflow rule**: Do one focused unit of work, validate it, commit it, then repeat for as long as useful work remains.
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 566 tests.

---

## Recent Work Completed

- Added `WorldNpcSpawnService.TrySwapInactiveWalkerFormationVariant` as the first C# primitive for Java `InstanceWalkerFormations.changeCluster`.
- Formation swaps now reactivate parked formation members at cached `WalkerGroup.form` coordinates, park the previous active members without releasing object IDs, and update static-placeable state around the swap.
- Extended walker variant choices with selected object IDs and made `WorldNpcWalkerSpawnPlanCacheService` preserve already-spawned version choices across refreshes instead of re-rolling Java `Rnd.get` every time.
- Added `WorldNpcWalkerMovementStateService` as a pure state bridge for Java `WalkManager.startRouteWalking`, `NpcMoveController.setRouteStep`, and `WalkerGroup.setStep`.
- The movement state model now carries single-walker current/target route step, rest-delay carryover, loop-none stop state, formation target projection, member shifts, and group-step updates.
- Extended `SM_MOVE` so it supports non-playable NPC movement payloads while keeping player/playable vector, geyser, and vehicle tails intact.
- Added `WorldNpcWalkerMovementBroadcastService` to bridge live `WorldNpc` objects, walker movement state, NPC `SM_MOVE`, and visible-player broadcast fanout.
- Kept `docs/PHASE-6-PROGRESS.md` current through Session 304 and refreshed the validation baseline.

---

## Commits In This Handoff

- `590978bbd` - `Swap inactive world NPC walker formations`
- `0a41b9b6b` - `Preserve world NPC walker variant state`
- `937ea88b4` - `Model world NPC walker movement state`
- `ce4037fba` - `Support NPC SM_MOVE payloads`
- `970fbabd2` - `Broadcast world NPC walker movement`

---

## Important Limits

- Walker/formation swap primitives are not yet invoked by NPC death, respawn, or future variant-change callbacks.
- The walker cache is still world-map scoped, not true `WorldMapInstance` scoped. Instance IDs and per-instance walker pools still need a real model.
- Movement state and broadcast plumbing are pure/runtime bridges only; no AI/timer caller drives them yet.
- NPC movement interpolation and live world-position advancement are still absent.
- `targetReached`, rest-time scheduling, group wait/all-arrived synchronization, and AI state/substate transitions remain pending.
- Random-walk and anchor behavior are still metadata only.
- Combat/life-stat systems still do not call the ordinary NPC death lifecycle, and registered-drop discovery is not wired into decay timing.
- Respawn parity still lacks Java event-end deferral, instance-existence checks, pooled respawn replacement, rift update callbacks, temporary-special object respawns, and full instance-aware pool state.
- NPC visibility remains a first-pass map/distance known-list proxy. It does not model Java region buckets, hide/see states, instance IDs, or persistent known-list membership yet.

---

## Suggested Next Units

1. Implement the first live `WalkManager.startRouteWalking` bridge for spawned world NPCs: resolve the route from static data, create movement state, call `WorldNpcWalkerMovementBroadcastService`, and keep the active movement state available for later target-reached work.
2. Add rest-time scheduling for single walkers: after target reached, wait `RouteStep.restTime`, choose the next step, and broadcast the next target.
3. Add single-walker `targetReached` handling with Java `LoopType.NONE` stop behavior and last-step wrap.
4. Add formation/group target-reached handling: `WALK_WAIT_GROUP`, all-arrived checks, group-step synchronization, and abort/resume behavior.
5. Add movement interpolation/world-position advancement for NPCs so broadcasts reflect current position plus target, not just target changes.
6. Wire walker/formation variant swaps into the future NPC death/variant-change callback once that caller boundary exists.
7. Add random-walk and anchor runtime behavior for NPCs without walker routes but with preserved `random_walk` / `anchor` spawn metadata.
8. Connect future combat/life-stat NPC death callers to `WorldNpcSpawnService.TryScheduleWorldNpcDeath`, keeping Java `NpcController.onDie` order.
9. Add registered-drop discovery so decay can choose Java no-drop (`2s`) versus with-drop (`5m`) timing from real drop state.
10. Continue respawn parity with Java event-end deferral, instance-existence checks, pooled respawn replacement, rift `updateSpawned`, and temporary-spawn unregister behavior for object ID changes.
11. Keep the broader Phase 6 backlog in `docs/PHASE-6-PROGRESS.md` as the source of the next non-walker priorities.
