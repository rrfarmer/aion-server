# Phase 6AQ Completion Handoff

**Created**: May 22, 2026
**Status**: Phase 6 remains in progress; this handoff follows 6AP and covers Sessions 337-341.
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, scheduling, guard order, persistence behavior, side effects, movement math, world-instance behavior, and naming.
**Workflow rule**: Do one focused unit of work, validate it, commit it, then repeat for as long as useful work remains.
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 648 tests.

---

## Recent Work Completed

- Added Java `RiftService.prepareRiftOpening` parity: scheduled openings now apply the Java random skip gate, guard/non-guard filtering, auto-closeable filtering, world-specific caps, and random location removal before opening selected rifts.
- Added rift schedule XML loading and the rift-specific Quartz cron parser needed for Java `config/schedule/rift_schedule.xml` hourly and comma-day expressions.
- Added `RiftScheduleService` as the C# bridge for Java `RiftService.initRifts` / `RiftOpenRunnable.run`: startup registers scheduled openings, each run prepares the rift, schedules autoclose after `gameserver.rift.duration * 3540 * 1000`, and broadcasts rift info.
- Wired close-time respawn cancellation from `RiftService.CloseRifts` to `WorldNpcSpawnService.CancelRespawn(int)` through a lazy delegate, avoiding the game-client/rift DI cycle.
- Tightened rift close cleanup so visible world objects are only removed when they still match the stored rift-owned spawn identity, approximating Java's `npc.getSpawn() == npcSpawnTemplate` guard.
- Added `WorldPosition.InstanceId` and rift instance fanout: `RiftManagerService.SpawnRift` now spawns one slave/master pair per configured world-map instance, and `RiftService` keeps all active `RiftPortalState` entries for a location.
- Updated `RiftInformerService` to count and serialize every master portal instance for aggregate, action 2, action 3, and entry-refresh packets.
- Added a visible-world NPC guard to rift show-dialog routing, so stale active portal state no longer answers `CM_SHOW_DIALOG` after the master NPC is gone.
- Kept `docs/PHASE-6-PROGRESS.md` current through Session 341 and refreshed the validation baseline.

---

## Commits In This Handoff

- `5e5214f2d` - `Prepare scheduled rift openings`
- `3049c1580` - `Wire scheduled rift openings`
- `2db917dec` - `Cancel rift respawns on close`
- `1541a142f` - `Fan out rifts across map instances`
- `03cc7cf9f` - `Guard rift dialog by visible master`

---

## Important Limits

- Rift show-dialog routing now checks that the master portal NPC is still visible in the world, but it is not yet true Java player known-list scoping.
- Java `CM_SHOW_DIALOG` side effects for protection removal and hide removal still need supporting player/effect surfaces before they can be ported cleanly.
- `JavaQuartzCronExpression` intentionally supports the schedule shapes needed by `rift_schedule.xml`; it is not a general replacement for Java `CronService` yet.
- `WorldPosition.InstanceId` gives rifts an instance id, but the C# world still does not have a full `WorldMapInstance` container. Player/NPC visibility and broadcast scoping remain mostly world-id based.
- The respawn-cancel bridge is wired for rift close, but full rift-owned NPC death/respawn integration still depends on future combat/life-stat callers and a stronger shared spawn-template registration surface.
- `PlayerTeamMembership` remains a narrow vortex accept bridge. Replace it with real group/alliance models when those systems land.

---

## Suggested Next Units

1. Continue full known-list/NPC dialog integration: player known-list scoping, protection/hide side effects, and shared non-rift NPC controller dispatch for `CM_SHOW_DIALOG`.
2. Connect future combat/life-stat NPC death callers into `WorldNpcSpawnService` respawn/decay paths and keep the rift `UpdateSpawned` callback aligned with Java `RespawnTask.respawn`.
3. Add registered-drop decay selection and drop-aware corpse cleanup once the C# drop registration surface is ready.
4. Continue world-instance work beyond rift fanout: instance-aware player/NPC visibility, broadcasts, and per-instance spawn state.
5. Continue broader Phase 6 backlog in `docs/PHASE-6-PROGRESS.md`, especially walker/random-walk geo correction, temporary spawn depth, special spawn parity, pets/house-object expirables, and skill/effect engine completion.
