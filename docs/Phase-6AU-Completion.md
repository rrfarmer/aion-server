# Phase 6AU Completion Handoff

**Created**: May 22, 2026
**Status**: Phase 6 remains in progress; this handoff follows 6AT and covers Sessions 352-355.
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, world/known-list behavior, persistence behavior, scheduling, and side effects.
**Workflow rule**: Do one focused unit of work, validate it, commit it, then repeat for as long as useful work remains.
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 685 tests.

---

## Recent Work Completed

- Added C# `ItemTemplateSummary.IsLimitOne` from Java `ItemMask.LIMIT_ONE`, giving loot collection a template-level lore/limit-one check without changing XML shape.
- Added `SM_SYSTEM_MESSAGE.STR_CAN_NOT_GET_LORE_ITEM` (`1300422`) and wired `WorldNpcLootService.RequestDropItem` to reject limit-one drops when the player already owns that item in cube inventory or the regular warehouse, matching Java `DropService.requestDropItem -> ItemTemplate.hasLimitOne`.
- Added seen-NPC loot-enable parity for Java `DropService.see`: `WorldNpcLootService.CreateLootEnableStatusForSeenNpc` now sends `SM_LOOT_STATUS.LOOT_ENABLE` only when the player sees a registered-drop NPC and is allowed to loot it.
- Wired NPC known-list appearance refresh so newly visible registered-drop NPCs send loot-enable status alongside `SM_NPC_INFO`, while preserving existing `SM_DELETE` handling for disappearances.
- Added `WorldNpcLootService.StartFreeForAll` for the delayed body of Java `DropService.scheduleFreeForAll`: it flips the registration into free-for-all state, clears explicit looters through `DropNpc.startFreeForAll` parity, and returns the Java-shaped loot-enable status for future broadcast callers.
- Added `WorldNpcLootService.ScheduleFreeForAll` using `ThreadPoolManager.schedule` with Java's default 240000 ms delay and a focused-test callback hook.
- Added the Java friendly-NPC free-for-all broadcast filter: when the lootable NPC race is Elyos or Asmodian, same-race players are filtered out of the future loot-enable broadcast.
- Kept `docs/PHASE-6-PROGRESS.md` current through Session 355 and refreshed the validation baseline.

---

## Commits In This Handoff

- `0e7521f12` - `Reject owned lore loot items`
- `f2bd46552` - `Send loot status for seen NPC drops`
- `8d512be0c` - `Schedule loot free-for-all transition`
- `11f944d2c` - `Filter friendly NPC free-for-all loot`

---

## Important Limits

- Real Java drop generation is still pending: drop calculators, custom/global/quest drops, and registration from the actual NPC death path.
- Future combat/life-stat NPC death callers still need to invoke the world NPC death/drop bridge so drop registration, initial loot-enable packets, free-for-all scheduling, corpse decay, despawn, and respawn are ordered against Java `NpcController.onDie` / `DropRegistrationService.registerDrop` / `RespawnService`.
- The C# corpse signal for seen-NPC loot status still comes from the registered-drop map until the real NPC death/state model is wired.
- Scheduled free-for-all is modeled in `WorldNpcLootService`, but is not yet invoked from the future real drop-registration/death caller, and no live world broadcast caller has been attached to the returned packet/filter yet.
- Loot collection still covers only the direct solo path. Group/alliance kinah splitting, item distribution, rolls, bids, winner messages, and team-member confirmation behavior remain pending.
- Temporary trade predicates, pet auto-sell, quality announcements, broader non-solo/drop-aware corpse cleanup, and persistence-side inventory save boundaries still need future parity passes.
- Ordinary NPC dialog remains a guard-only surface. Actual `AIEventType.DIALOG_START`, quest dialog start, and full `DialogService` / NPC AI selection remain pending.
- `WorldPosition.InstanceId` and rift fanout exist, but the world container and broadcast scoping are still mostly world-id based rather than full Java `WorldMapInstance` containers.

---

## Suggested Next Units

1. Build the Java drop-generation bridge from real NPC death flow: custom drop calculators, quest drops, global/event drops, and registration into the current `WorldNpcDropRegistrationService` maps.
2. Connect combat/life-stat NPC death callers into the drop-registration plus `WorldNpcSpawnService.TryScheduleWorldNpcDeath(objectId)` path, and verify initial loot-enable packets, free-for-all scheduling, corpse decay, despawn, and respawn ordering against Java.
3. Attach scheduled free-for-all results to a live visible-world broadcast caller using the returned `SM_LOOT_STATUS.LOOT_ENABLE` packet and `CanBroadcastTo` race filter.
4. Deepen `CM_LOOT_ITEM` beyond direct solo collection: group/alliance kinah and item distribution, rolls/bids, winner messages, and Java team confirmation behavior.
5. Add the remaining Java loot side effects: temporary trade predicates, pet auto-sell, quality announcements, and broader drop-aware corpse cleanup.
6. Deepen ordinary NPC dialog from the guard surface into Java AI/dialog start flow: `AIEventType.DIALOG_START`, quest dialog start, `DialogService`, and NPC controller selection.
7. Continue broader Phase 6 backlog in `docs/PHASE-6-PROGRESS.md`, especially world-instance visibility, housing/studio side effects, walker/random-walk geo correction, temporary spawn depth, special spawn parity, pets/house-object expirables, AP/siege side effects, and skill/effect engine completion.
