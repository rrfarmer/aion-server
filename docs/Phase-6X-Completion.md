# Phase 6X Completion Handoff

**Created**: May 22, 2026  
**Status**: Phase 6 remains in progress; this handoff follows 6W and covers Sessions 247-250.  
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, persistence behavior, side effects, and naming.  
**Workflow rule**: Do one focused unit of work, validate it, commit it, then repeat for as long as useful work remains.  
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.  
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 482 tests.

---

## Recent Work Completed

- Parsed Java item-template `<houseobject>` and `<housedeco>` actions into `ItemTemplateSummary`, preserving the Java `DecorateAction` case where a present tag with no `id` still means an action exists with template ID `0`.
- Implemented `CM_HOUSE_EDIT` action `3` register-from-inventory for cube furniture and decor items: C# now allocates the registered ID, deletes the inventory row, inserts `player_registered_items`, refreshes the cached registry/world-house snapshot, and sends Java-shaped `SM_HOUSE_EDIT(3, 1, object)` or `SM_HOUSE_EDIT(3, 2, decor)`.
- Registered and implemented `CM_HOUSE_DECORATE` opcode `75`, including Java `PartType.getForLineNr` line mapping, default-revert mutations, apply-registered-decor mutations, DB update/delete, duplicate Java action-4 responses for nonzero applies, and `SM_HOUSE_UPDATE` appearance broadcasts.
- Updated house render/update packet decor lines so used registered decorations overlay building defaults in Java `PartType.values()` room order.
- Added `CM_USE_HOUSE_OBJECT` opcode `224`, `CM_RELEASE_OBJECT` opcode `225`, `SM_USE_OBJECT` opcode `197`, and `SM_OBJECT_USE_UPDATE` opcode `264` packet surfaces.
- Expanded `HousingObjectTemplateSummary` with Java `UseItemAction` runtime metadata: `check_type`, `remove_count`, `reward_id`, and `final_reward_id`.
- Implemented `CM_HOUSE_EDIT` action `16` renovation: C# consumes the Java race/type-specific renovation coupon, persists `houses.building_id`, reloads the registry against the new building, refreshes cached player/world-house state, and broadcasts `SM_HOUSE_UPDATE`.
- Kept `docs/PHASE-6-PROGRESS.md` current through Session 250 and updated the next-step queue.

---

## Commits In This Handoff

- `cb38beb95` - `Register house items from inventory`
- `15c68d6b5` - `Apply house decorations`
- `7e717a808` - `Add house object use packet surface`
- `9445b79` - `Handle house renovation edits`

---

## Important Limits

- `CM_USE_HOUSE_OBJECT` and `CM_RELEASE_OBJECT` now parse and have packet serializers, but runtime behavior is still pending: visibility/talk-range guards, occupant tracking, owner-only/storage/postbox behavior, required-item checks, delayed reward/remove-count mutation, use-count persistence, cooldown mutation, cancel/release handling, and object deletion on final use.
- Object/decor ID release after registered-item deletion is not modeled yet.
- Object placement/move/despawn still lacks Java quest callbacks and complete live side effects for already sighted non-owner players.
- House-object known-list delivery still uses the current world-house distance scan rather than full region-bucketed Java `KnownList` membership.
- Studio addresses `2001`/`3001` remain excluded from global world-house load; Java spawns studios per personal instance and that flow remains pending.
- GeoService door state updates, live-DB orphan-house validation, and visitor kick side effects remain pending.
- Renovation reloads the registry against the new building and broadcasts appearance, but exact object respawn/despawn side effects for already known spawned house objects still need the fuller known-list/object model.

---

## Suggested Next Units

1. Implement `CM_USE_HOUSE_OBJECT`/`CM_RELEASE_OBJECT` runtime now that packet/action metadata is present: start with `UseableItemObject` owner/visitor guards, use-count/reward/final-reward paths, cooldown mutation, `SM_USE_OBJECT`, and `SM_OBJECT_USE_UPDATE`.
2. Add storage/postbox house-object runtime branches: owner-only storage denial/open update, postbox dialog/mailbox-state behavior, occupant release, and cancel messages.
3. Persist use-count and final-use object deletion through `player_registered_items`, then clear `house_object_cooldowns` for deleted useable objects.
4. Add Java placement/edit quest callback coverage for house item use events.
5. Continue world/known-list fidelity for house objects: broadcast placement/move/despawn/use side effects to already sighted non-owner players and eventually replace the distance scan with a fuller region/known-list model.
6. Add per-instance studio spawning once a C# personal-instance/home entry flow exists.
7. Add GeoService door update hooks for house door-state changes.
8. Continue visitor kick side effects with house-zone membership, friend/legion filtering, recipient messages, and teleport-out to loaded exit coordinates.
9. Continue AP/skill side effects only when a real C# home exists for legion contribution, siege callbacks, or passive `SkillEngine` effect fanout.
