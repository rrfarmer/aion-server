# Phase 6AW Completion Handoff

**Created**: May 22, 2026
**Status**: Phase 6 remains in progress; this handoff follows 6AV and covers Sessions 359-361.
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, world/known-list behavior, persistence behavior, scheduling, and side effects.
**Workflow rule**: Do one focused unit of work, validate it, commit it, then repeat for as long as useful work remains.
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 714 tests.

---

## Recent Work Completed

- Added `CustomNpcDropTable` for Java `DataManager.CUSTOM_NPC_DROP`, loading `data/static_data/custom_drop/custom_drop.xml` into `NpcDrop` / `DropGroup` / `Drop` summaries with Java defaults for race, max items, min/max amount, chance, and `each_member`.
- Added `WorldNpcCustomDropService` for the first Java custom-drop calculator surface: group race gating, level-based chance reduction, boost-rate multiplication, max-item selection, nearest-successful-drop selection, inclusive count rolls, and per-member distributed `WorldNpcDropItem` creation.
- Extended `WorldNpcDropItem` with NPC object id and distribute-item breadcrumbs for future Java team distribution / winner logic without changing packet shapes.
- Added `WorldNpcDropRegistrationWorkflowService` as the first Java-ordered custom-drop registration workflow: generate custom drops, write current-drop/registration maps, send initial loot-enable fanout, then schedule free-for-all fanout.
- Added `WorldNpcDropModifierService` for Java `DropRegistrationService.createDropModifiers` / `DropRewardEnum.dropRewardFrom`, deriving drop race from the looter and level-based chance reduction from `npc.level - highestLevel`.
- Kept `docs/PHASE-6-PROGRESS.md` current through Session 361 and refreshed the validation baseline.

---

## Commits In This Handoff

- `04ba3e1d3` - `Load custom NPC drop data`
- `df788c247` - `Register custom NPC drops`
- `63d5030a2` - `Derive NPC drop modifiers`

---

## Important Limits

- Custom NPC drop generation and registration are modeled, but real NPC death/reward code still does not invoke the workflow.
- Quest drops, global/event drop rules, global NPC exclusion rules, event-service active drops, world-drop-type restrictions, and instance/AI `onDropRegistered` callbacks remain pending.
- Live boost-rate inputs are still narrowed: Java `BOOST_DROP_RATE`, `DR_BOOST`, repose/salvation, palace-house boosts, and rate config are not yet collected from full stat/rate systems.
- Optional socket selection still defaults to zero because item option-slot bonus handling is not yet connected to generated drop items.
- Loot collection still covers only the direct solo path. Group/alliance kinah splitting, item distribution, rolls, bids, winner messages, and team-member confirmation behavior remain pending.
- Pet auto-loot/auto-sell, temporary trade predicates, quality announcements, persistence save boundaries, and broader non-solo/drop-aware corpse cleanup remain pending.
- The C# corpse signal for seen-NPC loot status still comes from the registered-drop map until the real NPC death/state model is wired.

---

## Suggested Next Units

1. Invoke `WorldNpcDropRegistrationWorkflowService` from a real or staged NPC death/reward caller so Java ordering is exercised end to end: schedule respawn first, register drops, fan out loot status, then schedule drop-aware corpse decay.
2. Add the quest-drop bridge after custom drops, matching Java `QuestService.getQuestDrop(droppedItems, index, npc, groupMembers, looter)`.
3. Start global/event drop rules: parse global rule XML, model `GlobalRule` / `GlobalDropItem`, add global NPC exclusion data, and port the Java `addGlobalDrops` restrictions in small slices.
4. Deepen live drop modifiers with stat/rate inputs: `BOOST_DROP_RATE`, `DR_BOOST`, repose/salvation boosts, palace-house boost, drop-rate config, chest/group-drop classification, and world-drop-type restrictions.
5. Deepen `CM_LOOT_ITEM` beyond direct solo collection: group/alliance kinah and item distribution, rolls/bids, winner messages, and Java team confirmation behavior.
6. Add remaining Java loot side effects: optional socket selection, temporary trade predicates, pet auto-sell/auto-loot boundaries, quality announcements, persistence save boundaries, and broader drop-aware corpse cleanup.
7. Continue broader Phase 6 backlog in `docs/PHASE-6-PROGRESS.md`, especially ordinary NPC dialog AI start flow, world-instance visibility, housing/studio side effects, walker/random-walk geo correction, temporary spawn depth, special spawn parity, pets/house-object expirables, AP/siege side effects, and skill/effect engine completion.
