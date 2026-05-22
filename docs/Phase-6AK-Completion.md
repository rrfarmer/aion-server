# Phase 6AK Completion Handoff

**Created**: May 22, 2026
**Status**: Phase 6 remains in progress; this handoff follows 6AJ and covers Sessions 305-309.
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, persistence behavior, side effects, movement math, and naming.
**Workflow rule**: Do one focused unit of work, validate it, commit it, then repeat for as long as useful work remains.
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 577 tests.

---

## Recent Work Completed

- Added `WorldNpcWalkerRouteWalkingService` as the first live C# bridge for Java `WalkManager.startRouteWalking`.
- Route walking now resolves live `WorldNpc` objects, static Java walker route data, and cached active walker/formation spawn plans before creating movement state and broadcasting NPC `SM_MOVE`.
- Added single-walker `TargetReachedAsync` handling for Java `WalkManager.targetReached` / `chooseNextRouteStep`, including rest-time scheduling, last-step wrap, and loop-type `NONE` stop behavior.
- Added per-formation runtime state for Java `WalkerGroup.targetReached`: members wait until all have arrived, then the group advances together with projected member targets and per-member delayed broadcasts when `rest_time` is present.
- Added the first target-reached world-position advancement boundary: live `WorldNpc.Position` updates to the reached route target before the next movement target is broadcast, while `SpawnLocation` remains intact.
- Added `StartWorldRouteWalkingAsync` as a world-level hook for future AI/bootstrap callers and added Java-style already-walking protection to avoid duplicate route starts or duplicate movement broadcasts.
- Hardened scheduled walker tests with thread-safe broadcast capture and pending rest-task cleanup on both scheduled action completion and task completion.
- Kept `docs/PHASE-6-PROGRESS.md` current through Session 309 and refreshed the validation baseline.

---

## Commits In This Handoff

- `32bd29e1f` - `Start world NPC walker routes`
- `0a65cc4ed` - `Schedule walker target reached movement`
- `9c6f275b1` - `Synchronize walker formation arrivals`
- `4b7447d78` - `Advance walker position on target reached`
- `60c823667` - `Add world walker route start hook`

---

## Important Limits

- The new world-level route start hook is not yet called from a real NPC AI owner lifecycle or startup timer.
- `TargetReachedAsync` is still test-driven; no movement timer or AI callback invokes it when a live NPC reaches a destination.
- Movement interpolation between route targets is still absent. C# only advances runtime position at the target-reached boundary.
- AI state/substate parity (`WALK_PATH`, `WALK_WAIT_GROUP`, stop/idle transitions, active movement config guards) is not modeled yet.
- Random-walk and anchor behavior remain metadata only.
- Walker/formation swap primitives are not yet invoked by NPC death, respawn, or future variant-change callbacks.
- The walker cache remains world-map scoped, not true `WorldMapInstance` scoped.
- Combat/life-stat systems still do not call the ordinary NPC death lifecycle, and registered-drop discovery is not wired into decay timing.
- Respawn parity still lacks Java event-end deferral, instance-existence checks, pooled respawn replacement, rift update callbacks, temporary-special object respawns, and full instance-aware pool state.
- NPC visibility remains a first-pass map/distance known-list proxy without Java region buckets, hide/see states, instance IDs, or persistent known-list membership.

---

## Suggested Next Units

1. Connect `WorldNpcWalkerRouteWalkingService.StartWorldRouteWalkingAsync` to the first honest runtime caller: either a narrow NPC AI lifecycle shim or a bootstrap/timer bridge that can later be replaced by full Java AI state.
2. Add a target-reached timer/interpolation slice: track movement duration or speed enough to call `TargetReachedAsync` after a route target should be reached, while keeping Java breadcrumbs for `NpcMoveController.moveToLocation` / `updatePosition`.
3. Model the minimal NPC AI walking state/substate surface needed by walker routes: `WALK_PATH`, `WALK_WAIT_GROUP`, already-walking guards, stop/idle results, and active movement gating.
4. Add random-walk runtime behavior for NPCs with preserved `random_walk` spawn metadata, including Java delay bounds and future geo hook placeholders.
5. Add anchor runtime behavior for NPCs with preserved `anchor` spawn metadata once the first AI movement state exists.
6. Wire walker/formation variant swaps into the future NPC death/variant-change callback once that caller boundary exists.
7. Connect future combat/life-stat NPC death callers to `WorldNpcSpawnService.TryScheduleWorldNpcDeath`, keeping Java `NpcController.onDie` ordering.
8. Add registered-drop discovery so decay can choose Java no-drop (`2s`) versus with-drop (`5m`) timing from real drop state.
9. Continue respawn parity with Java event-end deferral, instance-existence checks, pooled respawn replacement, rift `updateSpawned`, and temporary-spawn unregister behavior for object ID changes.
10. Keep the broader Phase 6 backlog in `docs/PHASE-6-PROGRESS.md` as the source of the next non-walker priorities.
