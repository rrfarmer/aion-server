# Phase 6AM Completion Handoff

**Created**: May 22, 2026
**Status**: Phase 6 remains in progress; this handoff follows 6AL and covers Sessions 315-318.
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, persistence behavior, side effects, movement math, and naming.
**Workflow rule**: Do one focused unit of work, validate it, commit it, then repeat for as long as useful work remains.
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 587 tests.

---

## Recent Work Completed

- Added `WorldNpcRandomWalkService` as the live bridge for Java `WalkManager.startRandomWalking` / `chooseNextRandomPoint`: movement config guard, `Walking` / `WalkRandom` AI state, Java delay window, random target math around `SpawnLocation`, and NPC `SM_MOVE` broadcast.
- Wired random walking into `WorldNpcSpawnService` startup and hourly temporary-spawn paths before route walking, matching Java `startRandomWalking(...) || startRouteWalking(...)` short-circuit behavior for dual random/path metadata NPCs.
- Added random-walk target arrival scheduling and 200 ms interpolation ticks using Java `NpcMoveController.moveToLocation` distance-fraction math; arrival now updates live `WorldNpc.Position`, clears the target, keeps `WalkRandom`, and schedules the next random target.
- Added `NpcRiftSpawnTable` as the first rift-anchor static-data bridge for Java `SpawnsData.addRiftSpawns` / `RiftManager.addRiftSpawnTemplate`, including the pooled-group first-template anchor rule.
- Kept `docs/PHASE-6-PROGRESS.md` current through Session 318 and refreshed the validation baseline.

---

## Commits In This Handoff

- `0981f3b06` - `Add random walk target scheduling`
- `24e6ed606` - `Start random walking before route walking`
- `f3a9d854d` - `Loop random walk targets after arrival`
- `d4410bdec` - `Load rift spawn anchor templates`

---

## Important Limits

- Random-walk and route interpolation are still straight-line, non-geo movement. Java `GeoService.getClosestCollision`, geo Z correction, cliff/unreachable handling, move-validate events, zone-update fanout, and destination-change packet nuances remain pending.
- Random-walk arrival scheduling requires positive `NpcTemplateSummary.RunSpeed`; no-speed NPCs still broadcast the target but do not schedule timed arrival/interpolation.
- The new rift anchor registry is static data only. C# still lacks the active Java `RiftManager` runtime that resolves master/slave anchors, spawns rift/vortex NPC pairs, tracks spawned rifts by world, removes rifts, and sends rift informer packets.
- The AI state service remains intentionally narrow and movement-focused. It is not the full Java NPC AI event machine.
- Walker route state and random-walk state remain world-map scoped rather than true `WorldMapInstance` scoped.
- Walker/formation swap primitives are still not invoked by NPC death, respawn, or future variant-change callbacks.
- Combat/life-stat systems still do not call the ordinary NPC death lifecycle, and registered-drop discovery is not wired into decay timing.
- Respawn parity still lacks Java event-end deferral, instance-existence checks, pooled respawn replacement, rift update callbacks, temporary-special object respawns, and full instance-aware pool state.
- NPC visibility remains a first-pass map/distance known-list proxy without Java region buckets, hide/see states, instance IDs, or persistent known-list membership.

---

## Suggested Next Units

1. Build the first active rift runtime on top of `NpcRiftSpawnTable`: port enough Java `RiftEnum` / anchor master-slave resolution to spawn paired rift NPCs into the C# world and track spawned rifts by world.
2. Add the rift removal/update boundary from Java `RVController` / `RiftManager.removeSpawnedRift`, then layer rift informer packet fanout once the packet models exist.
3. Deepen random-walk parity with Java `GeoService.getClosestCollision` when `GameServerOptions.Geodata.Enabled` and `NpcMoveEnabled` are true.
4. Deepen route and random-walk interpolation with Java `NpcMoveController` geo Z correction, unreachable point handling, move-validate events, and future zone-update fanout.
5. Broaden `WorldNpcAiStateService` only as concrete Java event callers require it; keep each expansion tied to a specific AI handler path.
6. Wire walker/formation variant swaps into the future NPC death/variant-change callback once that caller boundary exists.
7. Connect future combat/life-stat NPC death callers to `WorldNpcSpawnService.TryScheduleWorldNpcDeath`, keeping Java `NpcController.onDie` ordering.
8. Add registered-drop discovery so decay can choose Java no-drop (`2s`) versus with-drop (`5m`) timing from real drop state.
9. Continue respawn parity with Java event-end deferral, instance-existence checks, pooled respawn replacement, rift `updateSpawned`, and temporary-spawn unregister behavior for object ID changes.
10. Keep the broader Phase 6 backlog in `docs/PHASE-6-PROGRESS.md` as the source of the next non-NPC-spawn priorities.
