# Phase 6AD Completion Handoff

**Created**: May 22, 2026
**Status**: Phase 6 remains in progress; this handoff follows 6AC and covers Sessions 276-281.
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, persistence behavior, side effects, and naming.
**Workflow rule**: Do one focused unit of work, validate it, commit it, then repeat for as long as useful work remains.
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 520 tests.

---

## Recent Work Completed

- Extended direct NPC spawn summaries and static-data loading for Java `SpawnSpotTemplate` metadata: `random_walk`, `anchor`, spot `state`, spot `ai`, and group `difficult_id`.
- Added Java NPC template `state` and `ai` parsing to `NpcTemplateSummary`.
- Updated `WorldNpcSpawnService` to honor Java difficulty filtering (`difficult_id != 0 && difficult_id != requested`) before ordinary NPC materialization.
- Added Java-style initial NPC state resolution for materialized `WorldNpc` objects and express postmen: spawn state overrides template state, otherwise ordinary NPCs default to `ACTIVE | WALK_MODE` (`65`).
- Updated `SM_NPC_INFO` to write the modeled NPC state instead of a hard-coded active state.
- Added Java-style AI-name resolution for runtime NPCs: template AI by default, spawn-spot AI override when present, and `SpawnTemplate.NO_AI` (`__NO_AI__`) mapped to an empty runtime AI name.
- Extended runtime `WorldNpc` objects with Java `SpawnTemplate` metadata needed by future movement/AI/spawn systems: respawn seconds, static ID, random-walk range, walker ID/index, and anchor.
- Updated `SM_NPC_INFO` to write materialized NPC spawn static IDs in the Java packet slot (`npc.getSpawn().getStaticId()`).
- Kept `docs/PHASE-6-PROGRESS.md` current through Session 281 and refreshed the validation baseline.

---

## Commits In This Handoff

- `4c053cba5` - `Preserve NPC spawn spot metadata`
- `0cccff73f` - `Honor NPC spawn difficulty`
- `5942fddb6` - `Emit spawned NPC state`
- `a081a9bc6` - `Carry world NPC AI names`
- `3e7b17892` - `Carry world NPC spawn metadata`
- `4aadfff6a` - `Write NPC spawn static IDs`

---

## Important Limits

- Spawn support is still limited to ordinary non-instance NPCs. Static objects, gatherables, rifts, siege/base/vortex/town groups, and full instance spawning remain pending.
- Temporary spawn tracking still covers ordinary NPC groups only. Exact Java instance-id registration, per-instance pool state, respawn cancellation, and temporary special object types still need follow-up.
- NPC visibility is still a first-pass map/distance known-list proxy. It does not model Java region buckets, hide/see states, instance IDs, or persistent known-list membership yet.
- Runtime NPCs now carry state, AI-name, static ID, walker, random-walk, anchor, and respawn metadata, but actual AI engine behavior, walker formation, random walking, anchor semantics, static placeable GeoService hooks, and respawn scheduling are not implemented yet.
- House kick handling still lacks actual visitor selection, friend filtering, house-zone membership checks, and teleport-out side effects.

---

## Suggested Next Units

1. Implement the next runtime consumer for the newly carried NPC spawn metadata: walker formation, random walking, anchor semantics, static placeable GeoService hooks, or respawn scheduling.
2. Deepen spawn parity beyond ordinary non-instance NPCs: static handler objects, gatherables, rifts, siege/base/vortex/town groups, and instance-map spawning.
3. Make NPC and house visibility instance-aware, then move the current first-pass distance deltas toward persistent Java `KnownList` membership.
4. Complete `TemporarySpawnEngine` parity for instance registrations, special object models, per-instance pool state, and respawn interactions.
5. Add visitor kick side effects for `CM_HOUSE_KICK`, owner/settings/building changes, and ownership transfer, matching Java `HouseController.kickVisitors`.
6. Wire studio spawn calls from the future instance/teleport `registeredId` path so studios can enter the world with correct owner-instance boundaries.
7. Continue AP/abyss side effects where C# has supporting homes: legion contribution fanout and `SiegeService.onAbyssPointsAdded`.
8. Continue the stigma/effect slice with full `SkillEngine` passive effect apply/remove fanout after temporary skill mutations.
9. Wire charge, idian, and power-shard burn callers into future combat/skill observer paths.
10. Real-client validate scheduled item-use ordering, NPC visibility, broker targeting, and temporary spawn behavior once the readiness pass begins.
