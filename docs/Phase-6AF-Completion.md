# Phase 6AF Completion Handoff

**Created**: May 22, 2026
**Status**: Phase 6 remains in progress; this handoff follows 6AE and covers Sessions 285-289.
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, persistence behavior, side effects, and naming.
**Workflow rule**: Do one focused unit of work, validate it, commit it, then repeat for as long as useful work remains.
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 529 tests.

---

## Recent Work Completed

- Added ordinary-world-NPC respawn scheduling to `WorldNpcSpawnService`, matching Java `VisibleObjectController.deleteAndScheduleRespawn` / `RespawnService.scheduleRespawn` for positive `respawn_time` spawns.
- Tracked original `NpcSpawnSummary` and `NpcTemplateSummary` registrations for materialized world NPCs so scheduled respawns can re-materialize from Java spawn metadata.
- Added a first ordinary-NPC death lifecycle entry point matching Java `NpcController.onDie`: schedule respawn while the corpse remains spawned, clear static-placeable state immediately, then schedule delayed decay deletion.
- Added Java `RespawnService.scheduleDecayTask` timing constants: no-drop immediate decay (`2s`) and with-drop decay (`5m`).
- Added `WalkerTemplateTable` support for Java `npc_walker` route templates, including `route_id`, `pool`, `formation`, `rows`, `loop_type`, route-step coordinates/rest time, `WALK_BACK` expansion, pool-2 square formation, square row parsing, and missing-row fallback.
- Added `WalkerVersionTable` support for Java `walker_versions.xml`, mapping each route-version ID to its parent route ID like `WalkerVersionsData.afterUnmarshal`.
- Added `WorldNpcWalkerRouteService` as the first runtime consumer for loaded walker data, resolving spawned `WorldNpc.WalkerId` values into no-walker, missing-route, or ready route plans with version, formation, rows, and route steps.
- Kept `docs/PHASE-6-PROGRESS.md` current through Session 289 and refreshed the validation baseline.

---

## Commits In This Handoff

- `c23cd87f6` - `Schedule world NPC respawns`
- `0f8744207` - `Model world NPC death decay`
- `e57038031` - `Load NPC walker templates`
- `2035624be` - `Load NPC walker versions`
- `bd1a0b98f` - `Resolve world NPC walker routes`

---

## Important Limits

- Combat and life-stat systems still do not call the new NPC death lifecycle. The bridge is ready, but no real damage/death path invokes it yet.
- Registered drop discovery is not wired into decay selection, so the with-drop `5m` path is modeled but not yet selected by a drop service caller.
- Respawn parity still lacks Java event-end deferral, instance-existence checks, pooled respawn replacement, rift update callbacks, temporary-special object respawns, and full instance-aware pool state.
- Walker route data and version grouping now load, and spawned NPC walker IDs can resolve to route plans, but C# still lacks `InstanceWalkerFormations`, `WalkerGroup`, group shifts, route-step advancement, movement broadcasts, random-walk target selection, anchor behavior, and AI `WalkManager` callbacks.
- NPC visibility remains a first-pass map/distance known-list proxy. It does not model Java region buckets, hide/see states, instance IDs, or persistent known-list membership yet.
- Static placeable state is still an in-memory bridge. Full Java `GeoService` collision integration and static object models remain pending.
- House kick handling still lacks actual visitor selection, friend filtering, house-zone membership checks, and teleport-out side effects.

---

## Suggested Next Units

1. Wire the future combat/life-stat NPC death caller into `WorldNpcSpawnService.TryScheduleWorldNpcDeath`, keeping Java `NpcController.onDie` order: respawn first, reward/AI/quest events, then decay.
2. Add registered-drop discovery to choose Java `RespawnService.scheduleDecayTask` no-drop (`2s`) versus with-drop (`5m`) timing once the drop registry surface exists.
3. Continue respawn parity with Java event-end deferral, instance-existence checks, pooled respawn replacement, rift `updateSpawned`, and temporary-spawn unregister behavior for object ID changes.
4. Implement the next walker slice: `InstanceWalkerFormations` grouping from route version IDs, or `WalkerGroup` shift calculations for square/point formations.
5. Build route-step movement on top of `WorldNpcWalkerRouteService`, then connect it to AI `WalkManager` behavior, movement broadcasts, and known-list updates.
6. Add random-walk and anchor runtime behavior for NPCs without walker routes but with preserved `random_walk` / `anchor` spawn metadata.
7. Make NPC and house visibility instance-aware, then move the current distance deltas toward persistent Java `KnownList` membership.
8. Broaden `TemporarySpawnEngine` parity beyond ordinary non-instance NPCs: static handler objects, gatherables, rifts, siege/base/vortex/town groups, per-instance pool state, and respawn interactions.
9. Wire studio spawn calls from the future instance/teleport `registeredId` path so studios can enter the world with correct owner-instance boundaries.
10. Add visitor kick side effects for `CM_HOUSE_KICK`, owner/settings/building changes, and ownership transfer, matching Java `HouseController.kickVisitors`.
11. Continue AP/abyss, stigma/effect, charge/idian/power-shard, and real-client validation items from `PHASE-6-PROGRESS.md`.
