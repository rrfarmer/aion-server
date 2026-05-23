# Phase 6AX Completion Handoff

**Created**: May 22, 2026
**Status**: Phase 6 remains in progress; this handoff follows 6AW and covers Sessions 362-365.
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, world/known-list behavior, persistence behavior, scheduling, and side effects.
**Workflow rule**: Do one focused unit of work, validate it, commit it, then repeat for as long as useful work remains.
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 728 tests.

---

## Recent Work Completed

- Added `WorldNpcDeathDropWorkflowService` as the staged Java `NpcController.onDie` bridge for the custom-drop slice: schedule respawn first, register generated drops and fan out loot status, then schedule drop-aware corpse decay after registration.
- Added static-placeable death cleanup through `WorldNpcSpawnService.TryDespawnStaticPlaceableForWorldNpc`, preserving the Java order after decay scheduling while leaving older death-scheduler callers intact.
- Added `QuestDropTable` and `WorldNpcQuestDropService` for the first Java `QuestService.getQuestDrop` bridge, including XML loading, START quest checks, collecting-step checks, collect-item missing-count guards, solo quest drops, and staged team-member quest drops.
- Threaded quest drops into `WorldNpcDropRegistrationWorkflowService` after custom drops, so quest-only deaths register current-drop maps and run the existing initial loot-enable/free-for-all fanout.
- Added `GlobalDropTable` and `GlobalNpcExclusionTable`, loading default Java global-drop rules and global NPC exclusion data while keeping timed event `gd_rule` entries reserved for a future event-service active-drop surface.
- Added `WorldNpcGlobalDropService` for the first Java `DropRegistrationService.addGlobalDrops` helper surface: applicable-rule filtering, global NPC exclusions, the narrowed default-global NPC guard, dynamic chance math, and item eligibility filtering by race and NPC/item level diff.
- Kept `docs/PHASE-6-PROGRESS.md` current through Session 365 and refreshed the validation baseline.

---

## Commits In This Handoff

- `966c1fce8` - `Bridge NPC death custom drops`
- `ab521f4b2` - `Add NPC quest drop bridge`
- `e9328d0f5` - `Load global drop data`
- `c1485b5e6` - `Filter global drop rules`

---

## Important Limits

- Real combat/life-stat NPC death callers still need to invoke `WorldNpcDeathDropWorkflowService`; the current bridge is staged and covered, but not yet connected to every live death path.
- Global-drop rule filtering exists, but global-drop item generation/selection is not yet registered into NPC drops.
- Max-drop weighted selection, random count rolls, kinah scaling, event active rules, member-limit distribution, and full `GlobalDropData.processRules` behavior remain pending.
- World-drop-type restrictions, NPC-group restrictions, zone restrictions, abyss/siege/base spawn restrictions, and abyss-type exclusion checks are staged until the corresponding runtime models exist.
- Handler-side quest drops, full mentor-nearby checks, and complete group/alliance quest loot rules remain pending.
- Optional sockets, temporary trade predicates, pet auto-loot/auto-sell, quality announcements, persistence save boundaries, and instance/AI `onDropRegistered` callbacks remain pending.
- Loot collection still covers only the direct solo path. Group/alliance kinah splitting, item distribution, rolls, bids, winner messages, and team-member confirmation behavior remain pending.
- Broader non-solo/drop-aware corpse cleanup is still incomplete beyond the registered-drop map and staged death/drop bridge.

---

## Suggested Next Units

1. Wire future real combat/life-stat NPC death callers into `WorldNpcDeathDropWorkflowService` where the caller surface exists, preserving Java `NpcController.onDie` order.
2. Extend global drop generation: use applicable rules, chance roll, allowed item collection, max-drop weighted selection, count rolls, kinah scaling, and member-limit distribution, then thread the generated items into `WorldNpcDropRegistrationWorkflowService` after quest drops.
3. Add the event active-drop data/service surface for timed event `gd_rule` entries currently excluded from the default global-drop table.
4. Deepen global restrictions with world-drop-type, NPC group, zone, siege/base/abyss models, abyss-type exclusions, and Java `GlobalDropData.processRules` behavior.
5. Add handler-side quest drops and mentor-nearby checks to the quest-drop bridge.
6. Deepen `CM_LOOT_ITEM` beyond direct solo collection: group/alliance kinah, item distribution, rolls/bids, winner messages, and Java team confirmation behavior.
7. Add remaining Java loot side effects: optional socket selection, temporary trade predicates, pet auto-sell/auto-loot boundaries, quality announcements, persistence save boundaries, and broader drop-aware corpse cleanup.
8. Continue the broader Phase 6 backlog in `docs/PHASE-6-PROGRESS.md`, especially world-house/NPC visibility, studio spawn callbacks, temporary spawn depth, walker/random-walk geo correction, special spawn parity, pets/house-object expirables, AP/siege side effects, and skill/effect engine completion.
