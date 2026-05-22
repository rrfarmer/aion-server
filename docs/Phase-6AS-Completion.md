# Phase 6AS Completion Handoff

**Created**: May 22, 2026
**Status**: Phase 6 remains in progress; this handoff follows 6AR and covers Sessions 345-347.
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, world/known-list behavior, persistence behavior, scheduling, and side effects.
**Workflow rule**: Do one focused unit of work, validate it, commit it, then repeat for as long as useful work remains.
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 666 tests.

---

## Recent Work Completed

- Wired the C# `WorldNpcSpawnService` respawn path back into `RiftService.UpdateSpawned`, matching Java `RespawnService.RespawnTask.respawn` calling `RiftService.updateSpawned(oldObjectId, respawn)` after `SpawnEngine.spawnObject`.
- Added a lazy DI bridge `Func<int, WorldNpc, bool>` so NPC respawn can notify rift state without introducing a constructor cycle, mirroring the existing lazy respawn-cancel bridge in the opposite direction.
- Added ordinary non-rift `CM_SHOW_DIALOG` guard handling through `NpcDialogRequestService`, covering known target NPC lookup, `talk_info` interaction gating, Java talk-range math, and dialog-vs-function NPC too-far system messages.
- Extended NPC template loading with `HasTalkInfo` / `IsDialogNpc`, matching Java `NpcTemplate.canInteract()` and `NpcTemplate.isDialogNpc()` behavior from `TalkInfo`.
- Adjusted `GameServerConnection.HandleShowDialogAsync` so rift controllers still claim portal masters first, then ordinary NPC dialog requests fall through to the shared guard surface.
- Added `WorldNpcDropRegistrationService` as the first real C# surface for Java `DropRegistrationService` maps: current drops by NPC object id plus `DropNpc`-style registrations for allowed looters, free-for-all state, remaining decay time, and drop visibility checks.
- Registered the drop service through game-server DI as both the concrete service and `IWorldNpcDropRegistrationLookup`, so `WorldNpcSpawnService` now reads a real registered-drop map for corpse decay delay once future death/drop callers register loot.
- Kept `docs/PHASE-6-PROGRESS.md` current through Session 347 and refreshed the validation baseline.

---

## Commits In This Handoff

- `cff984e80` - `Notify rift service after NPC respawn`
- `222b2cc4f` - `Handle ordinary NPC dialog requests`
- `6a4226f01` - `Add world NPC drop registration map`

---

## Important Limits

- Future combat/life-stat NPC death callers still need to invoke `WorldNpcSpawnService.TryScheduleWorldNpcDeath(objectId)` from the real death flow, then confirm drop registration, corpse decay, despawn, and respawn ordering against Java.
- The full Java loot system is still pending: drop calculators, global/quest drop registration, `SM_LOOT_STATUS`, `CM_START_LOOT`, `CM_LOOT_ITEM`, loot distribution, and drop-aware corpse cleanup.
- Ordinary NPC dialog currently covers guard and too-far behavior only. Actual `AIEventType.DIALOG_START`, quest dialog start, and full `DialogService` / NPC AI selection remain pending.
- `WorldPosition.InstanceId` and rift fanout exist, but the world container and broadcast scoping are still mostly world-id based rather than full Java `WorldMapInstance` containers.
- C# talk-range parity only uses the NPC bound radius because a Java-style player bound radius is not modeled yet.
- `PlayerTeamMembership` remains a narrow vortex accept bridge. Replace it with real group/alliance models when those systems land.

---

## Suggested Next Units

1. Connect future combat/life-stat NPC death callers into `WorldNpcSpawnService.TryScheduleWorldNpcDeath(objectId)` and verify the drop-registration/death/decay ordering against Java `NpcController.onDie` / `RespawnService`.
2. Continue loot/drop work from the new registration maps: Java drop calculators/global/quest drops, then `SM_LOOT_STATUS` / `CM_START_LOOT` / `CM_LOOT_ITEM` list and item boundaries.
3. Deepen ordinary NPC dialog from the guard surface into Java AI/dialog start flow: `AIEventType.DIALOG_START`, quest dialog start, `DialogService`, and NPC controller selection.
4. Continue world-instance work beyond rift fanout: instance-aware player/NPC visibility, broadcasts, per-instance spawn state, and house/studio visibility side effects.
5. Continue broader Phase 6 backlog in `docs/PHASE-6-PROGRESS.md`, especially walker/random-walk geo correction, temporary spawn depth, special spawn parity, pets/house-object expirables, AP/siege side effects, and skill/effect engine completion.
