# Phase 6AL Completion Handoff

**Created**: May 22, 2026
**Status**: Phase 6 remains in progress; this handoff follows 6AK and covers Sessions 310-314.
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, persistence behavior, side effects, movement math, and naming.
**Workflow rule**: Do one focused unit of work, validate it, commit it, then repeat for as long as useful work remains.
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 582 tests.

---

## Recent Work Completed

- Wired `WorldNpcWalkerRouteWalkingService.StartWorldRouteWalkingAsync` into `WorldNpcSpawnService`, so startup NPC spawning and hourly temporary-spawn changes now start selected path walkers after walker spawn-plan refresh.
- Added timer-driven route arrival scheduling: route starts, immediate advances, and post-rest broadcasts schedule `TargetReachedAsync` based on live NPC position, target distance, template run speed, Java `MOVE_OFFSET`, and Java's 200 ms move-task tick.
- Added `WorldNpcAiStateService` as a focused Java-shaped `AIState` / `AISubState` surface for walker movement, including `Walking` / `WalkPath`, formation `WalkWaitGroup`, resume-to-`WalkPath`, and stop-to-`Idle` / `None`.
- Added 200 ms route-walker interpolation ticks that update live `WorldNpc.Position` toward the active route target between `SM_MOVE` target-change broadcasts, using the same distance fraction shape as Java `NpcMoveController.moveToLocation`.
- Added `GameServerAiOptions` and loaded Java `AIConfig` properties for NPC movement enablement, random-walk delay bounds, NPC shouts, and AI handler directory.
- Kept `docs/PHASE-6-PROGRESS.md` current through Session 314 and refreshed the validation baseline.

---

## Commits In This Handoff

- `efafba7da` - `Start walker routes after NPC spawn`
- `484a43054` - `Schedule walker arrival callbacks`
- `461c3e4c0` - `Track walker NPC AI state`
- `ad78ee846` - `Interpolate walker positions between route targets`
- `8e79b815e` - `Load NPC AI movement config`

---

## Important Limits

- Random-walk and anchor runtime behavior are still not implemented; C# only preserves the spawn metadata and now loads the Java AI movement delay config needed for that work.
- Route movement interpolation is straight-line only. It does not yet model Java geodata Z correction, cliff/line-of-sight handling, move-validate events, zone update fanout, or full `NpcMoveController` destination changes.
- The new AI state service is intentionally narrow and walker-focused. It is not the full Java AI event machine, and combat/follow/returning/forced-walk states are only enum surface for now.
- Route arrival scheduling requires positive `NpcTemplateSummary.RunSpeed`; NPCs without run speed still broadcast route targets but do not schedule timed arrival/interpolation.
- Walker route state remains world-map scoped, not true `WorldMapInstance` scoped.
- Walker/formation swap primitives are still not invoked by NPC death, respawn, or future variant-change callbacks.
- Combat/life-stat systems still do not call the ordinary NPC death lifecycle, and registered-drop discovery is not wired into decay timing.
- Respawn parity still lacks Java event-end deferral, instance-existence checks, pooled respawn replacement, rift update callbacks, temporary-special object respawns, and full instance-aware pool state.
- NPC visibility remains a first-pass map/distance known-list proxy without Java region buckets, hide/see states, instance IDs, or persistent known-list membership.

---

## Suggested Next Units

1. Implement the first random-walk runtime service for NPCs with `RandomWalkRange > 0`: honor `GameServerOptions.Ai.NpcMovementEnabled`, set `Walking` / `WalkRandom`, choose a target inside the Java square around `SpawnLocation`, delay by `NpcMovementMinimumDelaySeconds` / `NpcMovementMaximumDelaySeconds`, broadcast NPC `SM_MOVE`, and add deterministic tests.
2. Wire random-walk startup from `WorldNpcSpawnService` before route-walking, matching Java `WalkManager.startWalking` order (`startRandomWalking(...) || startRouteWalking(...)`) and avoiding double-start for NPCs that have both random and route metadata.
3. Add random-walk arrival looping by reusing or generalizing the route interpolation/arrival scheduler, then call the next random target when the NPC reaches the current one.
4. Deepen route interpolation with Java `NpcMoveController` geodata behavior: Z correction, unreachable-point handling, move-validate events, and future zone update fanout.
5. Add anchor runtime behavior once the random-walk movement service exists, using the preserved `WorldNpc.Anchor` metadata and Java source as the guardrail.
6. Broaden `WorldNpcAiStateService` only as callers need it; keep each expansion tied to a concrete Java event path.
7. Wire walker/formation variant swaps into the future NPC death/variant-change callback once that caller boundary exists.
8. Connect future combat/life-stat NPC death callers to `WorldNpcSpawnService.TryScheduleWorldNpcDeath`, keeping Java `NpcController.onDie` ordering.
9. Add registered-drop discovery so decay can choose Java no-drop (`2s`) versus with-drop (`5m`) timing from real drop state.
10. Continue respawn parity with Java event-end deferral, instance-existence checks, pooled respawn replacement, rift `updateSpawned`, and temporary-spawn unregister behavior for object ID changes.
11. Keep the broader Phase 6 backlog in `docs/PHASE-6-PROGRESS.md` as the source of the next non-walker priorities.
