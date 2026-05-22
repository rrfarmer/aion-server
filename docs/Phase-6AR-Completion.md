# Phase 6AR Completion Handoff

**Created**: May 22, 2026
**Status**: Phase 6 remains in progress; this handoff follows 6AQ and covers Sessions 342-344.
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, world/known-list behavior, persistence behavior, scheduling, and side effects.
**Workflow rule**: Do one focused unit of work, validate it, commit it, then repeat for as long as useful work remains.
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 657 tests.

---

## Recent Work Completed

- Added Java `CreatureVisualState` ids on C# `Player`, including blinking/protection helpers, exact `isInAnyHide` behavior, and the narrow hide-removal mutation needed by Java `CM_SHOW_DIALOG`.
- Added `SM_PLAYER_STATE` opcode 68 with the Java payload shape: object id, visual state, see state, and blinking marker byte.
- Extended NPC template loading to parse `talk_info can_talk_invisible`, preserving Java's default-true behavior when the attribute is absent.
- Added `NpcDialogSideEffectService.ApplyShowDialogSideEffects` for the Java `CM_SHOW_DIALOG.runImpl` side effects: stop protection before the trading guard, then remove hide only for known target NPCs that cannot talk to invisible players.
- Wired show-dialog state changes through `GameServerConnection` so `SM_PLAYER_STATE` broadcasts to visible players including self before rift dialog dispatch continues.
- Added `IWorldNpcDropRegistrationLookup` and Java-shaped `WorldNpcSpawnService` overloads that select NPC corpse decay from registered-drop state rather than requiring future death callers to pass the drop boolean manually.
- Pinned Java `RespawnService.IMMEDIATE_DECAY` and `WITH_DROP_DECAY` intervals in C#: 2 seconds without registered drops, 5 minutes with registered drops.
- Added `NpcVisibilityService.IsKnownNpc` and threaded the socket server's NPC known-list cache into each `GameServerConnection`.
- Scoped rift portal dialog requests to known NPCs when the predicate is available, while keeping the world-visible fallback for standalone service/test use.
- Kept `docs/PHASE-6-PROGRESS.md` current through Session 344 and refreshed the validation baseline.

---

## Commits In This Handoff

- `9d803619f` - `Apply show-dialog state side effects`
- `9aede22d1` - `Select NPC decay delay from drop lookup`
- `c5b634fec` - `Scope rift dialogs to known NPCs`

---

## Important Limits

- `CM_SHOW_DIALOG` now has protection/hide side effects and known-list scoping for rift portals, but shared non-rift NPC controller dispatch is still pending until broader NPC dialog controller surfaces exist.
- The drop-registration lookup is a bridge only. Full loot/drop maps, drop registration, and drop-aware corpse cleanup are not implemented yet.
- `WorldNpcSpawnService` can select decay delay and schedule respawn/decay, but future combat/life-stat death callers still need to invoke the bridge from real NPC death flow.
- `WorldPosition.InstanceId` and rift instance fanout exist, but the world container and broadcast scoping are still mostly world-id based rather than full Java `WorldMapInstance` containers.
- The rift respawn replacement hook (`RiftService.UpdateSpawned`) exists, but integration with actual NPC death/respawn callers remains a future slice.
- `PlayerTeamMembership` remains a narrow vortex accept bridge. Replace it with real group/alliance models when those systems land.

---

## Suggested Next Units

1. Continue `CM_SHOW_DIALOG` beyond rift portals by introducing a shared NPC dialog dispatch surface for non-rift NPC controllers as supporting dialog systems arrive.
2. Connect future combat/life-stat NPC death callers into `WorldNpcSpawnService.TryScheduleWorldNpcDeath(objectId)` and keep rift-owned respawn replacement aligned with Java `RespawnTask.respawn`.
3. Implement the first real C# drop registration/loot map slice, then finish drop-aware corpse cleanup using the new `IWorldNpcDropRegistrationLookup` bridge.
4. Continue world-instance work beyond rift fanout: instance-aware player/NPC visibility, broadcasts, and per-instance spawn state.
5. Continue broader Phase 6 backlog in `docs/PHASE-6-PROGRESS.md`, especially walker/random-walk geo correction, temporary spawn depth, special spawn parity, pets/house-object expirables, AP/siege side effects, and skill/effect engine completion.
