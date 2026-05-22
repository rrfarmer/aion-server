# Phase 6AC Completion Handoff

**Created**: May 22, 2026
**Status**: Phase 6 remains in progress; this handoff follows 6AB and covers Sessions 268-275.
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, persistence behavior, side effects, and naming.
**Workflow rule**: Do one focused unit of work, validate it, commit it, then repeat for as long as useful work remains.
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 517 tests.

---

## Recent Work Completed

- Added typed direct NPC spawn loading from Java `static_data/spawns`, preserving map, NPC id, coordinates, heading, respawn, pool, handler, static id, walker id/index, custom flags, and group/spot temporary schedules while still excluding town and nested special spawn groups.
- Added `WorldNpcSpawnService` as the first ordinary non-instance NPC `SpawnEngine.spawnAll` bridge, materializing supported direct NPC spots into `WorldNpc` objects with Java `IDFactory` object IDs and static NPC templates.
- Added `NpcVisibilityService`, generalized `SM_NPC_INFO` to `IWorldNpcObject`, exposed map-scoped `World.GetNpcs(worldId)`, and wired enter/move refreshes for first-pass NPC known-list deltas.
- Removed the broker object-id compatibility fallback. Broker operations now require a spawned, visible, template-backed NPC with Java `DialogAction.OPEN_VENDOR` (`33`).
- Added Java pool behavior for ordinary NPC direct spawns: valid pools activate a unique random subset, invalid pools fall back to all spots.
- Added typed `TemporarySpawnSchedule` support for Java `temporary_spawn` windows, including wildcard time parts, `/n` expressions, weekday masks, and startup `TemporarySpawn.isInSpawnTime` filtering against persisted game time.
- Added a first ordinary-NPC `TemporarySpawnEngine.onHourChange` bridge: game-time hour changes despawn tracked group-temporary NPCs, spawn newly eligible temporary spots, and refresh NPC known-list deltas.
- Registered and routed Java `CM_HOUSE_KICK` opcode `72`, including owner notification messages for kick-non-friends and kick-all requests.
- Kept `docs/PHASE-6-PROGRESS.md` current through Session 275 and refreshed the Phase 6 next-step queue.

---

## Commits In This Handoff

- `f912bed1b` - `Populate regular world NPC spawns`
- `f33e10dda` - `Send spawned NPC known-list deltas`
- `6101db402` - `Require spawned broker NPC targeting`
- `dabcda35f` - `Support pooled world NPC spawns`
- `ae37e791a` - `Defer temporary NPC spawns`
- `ed88d3844` - `Evaluate temporary spawn windows`
- `30110af94` - `Run temporary NPC spawn hours`
- `9836ab13d` - `Parse housing kick requests`

---

## Important Limits

- Spawn support is still limited to ordinary non-instance NPCs. Static objects, gatherables, rifts, siege/base/vortex/town groups, walker movement, and richer AI state remain pending.
- Temporary spawn tracking currently covers group-temporary ordinary NPCs only. Exact Java instance-id registration, per-instance pool state, respawn cancellation, and temporary special object types still need follow-up.
- NPC visibility is still a first-pass map/distance known-list proxy. It does not yet model Java region buckets, hide/see states, instance IDs, or persistent known-list membership.
- House kick handling parses and routes `CM_HOUSE_KICK` and sends owner notification messages, but actual visitor selection, friend filtering, house-zone membership checks, and teleport-out side effects are still pending.
- Studio house spawning remains cached/on-demand only; future instance/teleport code still needs to call `HousingWorldService.TrySpawnStudio` at the Java `registeredId` spawn point.

---

## Suggested Next Units

1. Deepen spawn parity beyond ordinary non-instance NPCs: static handler objects, gatherables, rifts, siege/base/vortex/town groups, walker movement, and respawn release/cancellation.
2. Make NPC and house visibility instance-aware, then move the current first-pass distance deltas toward persistent Java `KnownList` membership.
3. Complete `TemporarySpawnEngine` parity for instance registrations, special object models, per-instance pool state, and respawn interactions.
4. Add visitor kick side effects for `CM_HOUSE_KICK`, owner/settings/building changes, and ownership transfer, matching Java `HouseController.kickVisitors`.
5. Wire studio spawn calls from the future instance/teleport `registeredId` path so studios can enter the world with correct owner-instance boundaries.
6. Continue AP/abyss side effects where C# has supporting homes: legion contribution fanout and `SiegeService.onAbyssPointsAdded`.
7. Continue the stigma/effect slice with full `SkillEngine` passive effect apply/remove fanout after temporary skill mutations.
8. Wire charge, idian, and power-shard burn callers into future combat/skill observer paths.
9. Real-client validate scheduled item-use ordering, NPC visibility, broker targeting, and temporary spawn behavior once the readiness pass begins.
