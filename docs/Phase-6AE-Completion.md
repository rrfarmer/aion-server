# Phase 6AE Completion Handoff

**Created**: May 22, 2026
**Status**: Phase 6 remains in progress; this handoff follows 6AD and covers Sessions 282-284.
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, persistence behavior, side effects, and naming.
**Workflow rule**: Do one focused unit of work, validate it, commit it, then repeat for as long as useful work remains.
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 522 tests.

---

## Recent Work Completed

- Added `StaticPlaceableStateService` as the first C# runtime bridge for Java `GeoService.spawnPlaceableObject` / `despawnPlaceableObject`, tracking active NPC spawn static IDs by world map until full collision geometry exists.
- Registered the static-placeable bridge in GameServer DI and wired `WorldNpcSpawnService` so successful ordinary NPC materialization activates the static ID and temporary despawn clears it.
- Aligned `NpcDialogTargetingService.ValidateTargetingNpcWithFunction` with Java `Player.isTargetingNpcWithFunction`: the helper now validates the current target and supported NPC function without adding a C#-only visibility/distance gate.
- Added `WorldNpcSpawnService.TryDespawnWorldNpc` as a service-owned ordinary NPC removal boundary for Java `VisibleObjectController.delete` style cleanup.
- Routed temporary NPC despawns through the new despawn boundary so world removal, static-placeable cleanup, and object-ID release happen in one place for future death/respawn callers.
- Kept `docs/PHASE-6-PROGRESS.md` current through Session 284 and refreshed the validation baseline.

---

## Commits In This Handoff

- `ceb49fe13` - `Track NPC static placeables`
- `79978cf17` - `Align NPC function targeting`
- `2d23c971b` - `Centralize world NPC despawn cleanup`

---

## Important Limits

- Static placeable state is only an in-memory parity bridge today. Full Java `GeoService` collision integration, geometry-backed static objects, and real client collision behavior are still pending.
- Ordinary world NPC despawn now has one cleanup boundary, but callers for NPC death/decay and Java `RespawnService.scheduleRespawn` are not implemented yet.
- Temporary spawn tracking still covers ordinary non-instance NPC groups only. Exact Java instance-id registration, static/gatherable/rift/siege/vortex/town temporary groups, respawn cancellation, and richer pool state remain pending.
- NPC visibility remains a first-pass map/distance known-list proxy. It does not model Java region buckets, hide/see states, instance IDs, or persistent known-list membership yet.
- Runtime NPCs carry state, AI-name, static ID, walker, random-walk, anchor, and respawn metadata, but walker formation, random walking, anchor semantics, AI engine behavior, and respawn scheduling remain future runtime work.
- House kick handling still lacks actual visitor selection, friend filtering, house-zone membership checks, and teleport-out side effects.

---

## Suggested Next Units

1. Wire another caller into `WorldNpcSpawnService.TryDespawnWorldNpc`: NPC death/decay cleanup or the first Java `RespawnService.scheduleRespawn` bridge.
2. Continue runtime consumers for the preserved NPC spawn metadata: walker formation, random-walk radius, anchor semantics, and AI-name handoff into the future AI engine.
3. Deepen static placeable parity from the current count-tracking bridge toward real GeoService collision integration and static object models.
4. Make NPC and house visibility instance-aware, then move the current distance deltas toward persistent Java `KnownList` membership.
5. Broaden `TemporarySpawnEngine` parity beyond ordinary non-instance NPCs: static handler objects, gatherables, rifts, siege/base/vortex/town groups, per-instance pool state, and respawn interactions.
6. Wire studio spawn calls from the future instance/teleport `registeredId` path so studios can enter the world with correct owner-instance boundaries.
7. Add visitor kick side effects for `CM_HOUSE_KICK`, owner/settings/building changes, and ownership transfer, matching Java `HouseController.kickVisitors`.
8. Continue AP/abyss side effects where C# has supporting homes: legion contribution fanout and `SiegeService.onAbyssPointsAdded`.
9. Continue the stigma/effect slice with full `SkillEngine` passive effect apply/remove fanout after temporary skill mutations.
10. Wire charge, idian, and power-shard burn callers into future combat/skill observer paths.
11. Real-client validate scheduled item-use ordering, NPC visibility, broker targeting, and temporary spawn behavior once the readiness pass begins.
