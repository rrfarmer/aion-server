# Phase 6AO Completion Handoff

**Created**: May 22, 2026
**Status**: Phase 6 remains in progress; this handoff follows 6AN and covers Sessions 325-331.
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, persistence behavior, side effects, movement math, and naming.
**Workflow rule**: Do one focused unit of work, validate it, commit it, then repeat for as long as useful work remains.
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 624 tests.

---

## Recent Work Completed

- Added `RiftInformerService` world/player/despawn fanout using `IGameClientConnectionRegistry`, including Java twin-world behavior and action 4 despawn broadcast.
- Added `RiftPortalState` as the C# bridge for Java `RVController` portal metadata: master/slave NPCs, max/used entries, min/max levels, destination race, vortex/volatile/invasion flags, despawn time, remain-time calculation, and passed-player tracking for vortexes.
- Completed `SmRiftAnnounce` action 2/3 serialization from `RiftPortalState`, including Java packet lengths, field order, owner XYZ, remain time, used entries, display byte, and rift type mapping.
- Wired `RiftInformerService.GetPackets(worldId)` to append Java action 2/3 portal packets for active master portals, and corrected the Java `sendRiftsInfo(Player)` split: current-world packets direct to the player, twin-world packets broadcast to the twin world.
- Added `RiftPortalDialogService` for Java `RVController.onDialogRequest`, including invasion opposite-race gating and question-window creation for ordinary direct portals and vortex portals.
- Added `RiftPortalUseService` for the Java `RVController.acceptRequest` / `onAccept` boundary: closed/despawned guards, level restrictions, vortex entry limit checks, ordinary teleport to slave spawn, vortex destination resolver hook, and used-entry mutation.
- Added `RiftInformerService.SendRiftInfoAsync(IReadOnlyList<int>)` for Java `RiftInformer.sendRiftInfo(int[])`, broadcasting action 3 entry-refresh packets from the first world's master portals to every listed world.
- Kept `docs/PHASE-6-PROGRESS.md` current through Session 331 and refreshed the validation baseline.

---

## Commits In This Handoff

- `378ef804c` - `Fan out rift announce packets`
- `151b70eea` - `Track rift portal state`
- `7317f981a` - `Serialize rift portal announce packets`
- `726fcd64b` - `Fan out rift portal packets`
- `5e2e15d6e` - `Add rift portal dialog requests`
- `f02801d9e` - `Add rift portal use acceptance`
- `cc3a84e32` - `Refresh rift entry updates`

---

## Important Limits

- Rift dialog/use services are not wired into `CM_DIALOG_SELECT`, `CM_QUESTION_RESPONSE`, or a pending player request model yet.
- `RiftPortalUseService` mutates portal used-entry state, but does not yet call `RiftInformerService.SendRiftInfoAsync`; callers must wire the Java post-`syncPassed` refresh.
- Java `VortexLocation` data is not ported. Vortex accept requires an injected destination resolver until that table/service exists.
- Vortex team removal and `STR_MSG_INVADE_DIRECT_PORTAL_OPEN_NOTICE` system-message side effects are still pending.
- `Player.Level` now exists for Java `Player.getLevel()` parity, but active player loading/hydration should be audited before relying on it outside focused tests.
- Guard NPC spawning for `RiftLocation.HasSpawns` is not implemented. Java `RiftService.openRifts` spawns those guard groups before opening the portal pair.
- `RiftService` still opens one master/slave pair per rift, not one pair per Java `WorldMapInstance` count.
- `RiftService.closeRifts` removes currently tracked spawned rift NPCs, but Java respawn cancellation and `RiftService.updateSpawned(oldObjectId, respawn)` replacement parity are still pending.
- Cron/scheduled rift opening is still absent. Java `RiftSchedule`, `RiftOpenRunnable`, and `CronService` remain future work.
- The broader NPC movement limits from earlier handoffs still apply: route/random-walk interpolation is straight-line and non-geo, AI state is a narrow surface, and walker/random-walk state is not true `WorldMapInstance` scoped.

---

## Suggested Next Units

1. Wire `RiftPortalDialogService` / `RiftPortalUseService` into the pending-question path: target portal lookup, pending request storage, `SM_QUESTION_WINDOW` send, `CM_QUESTION_RESPONSE` accept handling, teleport application, entry sync, and `RiftInformerService.SendRiftInfoAsync`.
2. Port Java `VortexLocation` static data/service so vortex accepts can resolve real destinations without an injected resolver.
3. Add vortex-specific side effects from Java `RVController.acceptRequest`: group/alliance removal and `STR_MSG_INVADE_DIRECT_PORTAL_OPEN_NOTICE`.
4. Implement guard NPC spawning for `RiftLocation.HasSpawns` using the existing rift spawn static data, keeping Java `RiftService.openRifts` order.
5. Add Java `RiftService.updateSpawned(oldObjectId, respawn)` and close-time respawn-cancel parity for rift-owned spawned lists.
6. Add instance-count fanout for rift spawning once the C# world has a stronger `WorldMapInstance` surface.
7. Add cron/scheduled rift opening from Java `RiftSchedule` / `RiftOpenRunnable` after the active runtime and informer fanout are stable.
8. Continue the broader Phase 6 backlog in `docs/PHASE-6-PROGRESS.md`, especially walker/random-walk geo correction, NPC death/respawn callers, registered-drop decay selection, and special spawn parity.
