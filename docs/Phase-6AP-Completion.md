# Phase 6AP Completion Handoff

**Created**: May 22, 2026
**Status**: Phase 6 remains in progress; this handoff follows 6AO and covers Sessions 332-336.
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, persistence behavior, side effects, movement math, and naming.
**Workflow rule**: Do one focused unit of work, validate it, commit it, then repeat for as long as useful work remains.
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 634 tests.

---

## Recent Work Completed

- Wired Java `CM_SHOW_DIALOG -> RVController.onDialogRequest -> SM_QUESTION_WINDOW -> CM_QUESTION_RESPONSE` for active master rift portals via `CmShowDialog`, `PendingRiftPortalRequest`, `RiftPortalInteractionService`, and `GameServerConnection` question-response routing.
- Accepted ordinary portal responses now clear pending state, teleport to the slave spawn, mutate used-entry state, and call `RiftInformerService.SendRiftInfoAsync` for Java action 3 entry refreshes.
- Added Java `VortexLocation` static-data support through `VortexLocationTable` / `VortexLocationService`, including `dimensional_vortex/vortex_location` parsing and Java `VortexService.getLocationByWorld` / `getLocationByRift` mappings.
- Accepted vortex portal responses now resolve real Java start points, remove the responder from a narrow group/alliance membership bridge, send `SM_SYSTEM_MESSAGE.STR_MSG_INVADE_DIRECT_PORTAL_OPEN_NOTICE`, and then refresh entry counts.
- Corrected rift portal anchor parity: real Java portal anchors come from ordinary `spawn handler="RIFT"` groups, while nested `rift_spawn` groups are now used for guard/additional NPCs.
- `RiftService.OpenRifts(..., guards: true)` now spawns nested `rift_spawn` guard NPCs before the portal pair and keeps them in `RiftLocationState` for close cleanup.
- Added `RiftService.UpdateSpawned(oldObjectId, respawn)` / `RiftLocationState.ReplaceSpawned` for Java respawn replacement callback parity.
- Kept `docs/PHASE-6-PROGRESS.md` current through Session 336 and refreshed the validation baseline.

---

## Commits In This Handoff

- `410e967bf` - `Wire rift portal question flow`
- `90f35832e` - `Resolve vortex portal destinations`
- `f5fccd22a` - `Apply vortex portal accept side effects`
- `2a8e9b0d8` - `Spawn rift guards from location data`
- `e4542e70a` - `Track rift respawn replacements`

---

## Important Limits

- Rift request wiring is still focused on active master portal lookup. It is not yet integrated with the full known-list/NPC dialog engine.
- Cron/scheduled rift opening is still absent. Java `RiftSchedule`, `RiftOpenRunnable`, and `CronService` remain future work.
- Close-time respawn cancellation parity is still partial: spawned objects are removed from the C# world/id factory, but Java `RespawnService.cancelRespawn(objectId, spawnTemplate)` has no full C# scheduled-respawn bridge here yet.
- `RiftService` still opens one master/slave pair per rift, not one pair per Java `WorldMapInstance` count.
- `PlayerTeamMembership` is a narrow bridge for vortex accept removal only. Replace it with real group/alliance models when those systems land.
- The broader NPC movement limits from earlier handoffs still apply: route/random-walk interpolation is straight-line and non-geo, AI state is a narrow surface, and walker/random-walk state is not true `WorldMapInstance` scoped.

---

## Suggested Next Units

1. Integrate rift portal dialog routing with the full known-list/NPC dialog engine instead of only active master portal lookup.
2. Add cron/scheduled rift opening from Java `RiftSchedule` / `RiftOpenRunnable`, including `prepareRiftOpening` random location selection and guard/no-guard filtering.
3. Add close-time respawn-cancel parity once the C# respawn scheduler exposes a `RespawnService.cancelRespawn(objectId, spawnTemplate)` equivalent.
4. Add instance-count fanout for rift spawning once the C# world has a stronger `WorldMapInstance` surface.
5. Continue the broader Phase 6 backlog in `docs/PHASE-6-PROGRESS.md`, especially walker/random-walk geo correction, NPC death/respawn callers, registered-drop decay selection, and special spawn parity.
