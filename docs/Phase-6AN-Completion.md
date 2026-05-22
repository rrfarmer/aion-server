# Phase 6AN Completion Handoff

**Created**: May 22, 2026
**Status**: Phase 6 remains in progress; this handoff follows 6AM and covers Sessions 319-324.
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, persistence behavior, side effects, movement math, and naming.
**Workflow rule**: Do one focused unit of work, validate it, commit it, then repeat for as long as useful work remains.
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 601 tests.

---

## Recent Work Completed

- Added `RiftManagerService` as the first active bridge for Java `RiftManager.spawnRift(RiftLocation, boolean)`: Java `RiftEnum` metadata, master/slave anchor resolution from `NpcRiftSpawnTable`, pooled slave spot selection, slave-before-master spawning, and spawned-rift tracking by world.
- Added `RiftManagerService.RemoveSpawnedRift`, matching Java `RiftManager.removeSpawnedRift(Npc)` as a tracking-only lifecycle boundary for future `RVController.onDespawn` callers.
- Added `RiftLocationTable` / `RiftLocationSummary` and loaded Java `rift_locations/rift_location` metadata, including `has_spawns` and the Java `auto_closeable=true` default.
- Added `RiftService` as the first active-location layer for Java `RiftService.isValidId`, `openRifts`, `closeRifts`, `isRiftOpened`, and `closeAutoCloseableRifts`, including id/world-id open and close paths, active state, spawned-list clearing, world removal, and object-id release.
- Added `RiftInformerService` for Java `RiftInformer.getAnnounceData` and `getTwinId`, covering 12-slot aggregate announce math for vortex, normal, volatile, and current Java invasion aggregate behavior.
- Added `SmRiftAnnounce` for Java `SM_RIFT_ANNOUNCE` opcode 236 action 0 aggregate, action 1 Silentera flags, and action 4 despawn object-id payloads.
- Kept `docs/PHASE-6-PROGRESS.md` current through Session 324 and refreshed the validation baseline.

---

## Commits In This Handoff

- `518b80f46` - `Spawn rift NPC pairs from anchors`
- `2712f0b57` - `Track rift despawn removals`
- `0b0bc43c5` - `Load rift location templates`
- `8b7ef826a` - `Add active rift location service`
- `2d7a9e6e4` - `Calculate rift announce data`
- `a939c3194` - `Serialize rift announce packets`

---

## Important Limits

- The rift runtime is still a focused C# bridge, not the full Java `RiftService`/`RiftManager`/`RVController` stack.
- Guard NPC spawning for `has_spawns` rift locations is not implemented. Java `RiftService.openRifts` spawns those guard groups before opening the portal pair.
- `RiftService` currently opens only one world-map layer per rift. Java loops over `WorldMap.getInstanceCount()` and spawns one master/slave pair per instance.
- `RiftService.closeRifts` removes currently tracked spawned rift NPCs, but Java respawn cancellation and `RiftService.updateSpawned(oldObjectId, respawn)` replacement parity are still pending.
- `RiftInformerService` calculates aggregate announce data and twin worlds, but does not yet send packets to worlds or individual players.
- `SmRiftAnnounce` does not yet serialize action 2/3 portal-detail packets because C# still lacks `RVController`-style remain time, max/used entries, rift type, and master/slave portal state.
- Portal dialog, teleport, entry limits, invasion race restrictions, volatile rift behavior, and `syncPassed` updates are not yet modeled.
- Cron/scheduled rift opening is still absent. Java `RiftSchedule`, `RiftOpenRunnable`, and `CronService` remain future work.
- The broader NPC movement limits from 6AM still apply: route/random-walk interpolation is straight-line and non-geo, AI state is a narrow surface, and walker/random-walk state is not true `WorldMapInstance` scoped.

---

## Suggested Next Units

1. Add `RiftInformerService` world/player fanout methods using `IGameClientConnectionRegistry`, sending aggregate `SmRiftAnnounce` packets for `sendRiftsInfo(worldId)`, twin-world sync, player-targeted sync, and despawn action 4.
2. Add a focused `RiftPortalState` / `RVController` data bridge for master/slave portals: max entries, used entries, min/max level, remain time, vortex/volatile/invasion flags, destination race, and accepting state.
3. Implement `SmRiftAnnounce` action 2/3 portal-detail packet serialization once `RiftPortalState` exists.
4. Wire portal `syncPassed` behavior: increment used entries, update invasion passed-player counts, and send Java-shaped rift info packets to the master/slave worlds.
5. Add portal dialog/teleport acceptance checks from Java `RVController.onDialogRequest` / `onAccept`: spawned guard, level restrictions, vortex max-entry guard, invasion race restriction, team removal for vortex, and target teleport coordinates.
6. Implement guard NPC spawning for `RiftLocation.HasSpawns` using the existing rift spawn static data, keeping Java `RiftService.openRifts` order.
7. Add Java `RiftService.updateSpawned(oldObjectId, respawn)` and close-time respawn-cancel parity for rift-owned spawned lists.
8. Add instance-count fanout for rift spawning once the C# world has a stronger `WorldMapInstance` surface.
9. Add cron/scheduled rift opening from Java `RiftSchedule` / `RiftOpenRunnable` after the active runtime and informer fanout are stable.
10. Continue the broader Phase 6 backlog in `docs/PHASE-6-PROGRESS.md`, especially walker/random-walk geo correction, NPC death/respawn callers, registered-drop decay selection, and special spawn parity.
