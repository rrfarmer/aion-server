# Phase 6W Completion Handoff

**Created**: May 22, 2026  
**Status**: Phase 6 remains in progress; this handoff follows 6V and covers Sessions 241-246.  
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, persistence behavior, side effects, and naming.  
**Workflow rule**: Do one focused unit of work, validate it, commit it, then repeat for as long as useful work remains.  
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.  
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 480 tests.

---

## Recent Work Completed

- Loaded Java `player_registered_items` rows into C# `HouseRegistrySummary`, separating `area='DECOR'` decorations from placeable house objects and preserving Java invalid-decor filtering for building-tag mismatch and non-palace room rows.
- Parsed Java `housing/house_parts.xml` and `Building.parts_match`, allowing C# to validate registered decor rows against the active building like `HousePart.isForBuilding`.
- Wired decoration-mode entry to lazy-load/cached active-house registries and send Java-shaped `SM_HOUSE_REGISTRY(1)` not-spawned objects plus `SM_HOUSE_REGISTRY(2)` default parts and unused decorations.
- Added spawned house-object visibility: `SM_DELETE_HOUSE_OBJECT` opcode `269`, owner `CM_LEVEL_READY` compact `SM_HOUSE_OBJECTS`, and known-list appearance/disappearance `SM_HOUSE_OBJECT`/`SM_DELETE_HOUSE_OBJECT` tied to world-house visibility.
- Loaded/stored Java `house_object_cooldowns` and used receiver-specific cooldown seconds in `SM_HOUSE_REGISTRY` and `SM_HOUSE_OBJECT`.
- Expanded `SM_HOUSE_EDIT` packet parity for actions `3`, `4`, `5`, and `7`, and implemented registered-object placement/move/despawn persistence through `player_registered_items` placement columns.
- Parsed Java `UseItemAction.check_type` and wrote the nonzero useable-object usage-data tail.
- Implemented `CM_HOUSE_EDIT` action `4` delete/unregister for already registered objects, deleting the DB row, updating cached registry/world-house state, and echoing Java's duplicate action-4 responses.
- Kept `docs/PHASE-6-PROGRESS.md` current through Session 246, including the updated next-step queue.

---

## Commits In This Handoff

- `cb9743513` - `Load house registry rows`
- `c4f65aa66` - `Emit spawned house object packets`
- `99c63d543` - `Load house object cooldowns`
- `bcc25e371` - `Handle house object placement edits`
- `ee5a069e8` - `Write house use action check type`
- `711389223` - `Delete registered house objects`

---

## Important Limits

- `CM_HOUSE_EDIT` action `3` register-from-inventory is still pending; it needs item action parsing, ID allocation, inventory deletion, decor/object registry insertion, and Java-shaped add responses.
- Decoration set-used/delete behavior is not ported yet, so registered decor rows can load and display but cannot be applied to rooms/building parts through C# mutation paths.
- Renovation action `16` is still pending: coupon lookup/removal, building switch, registry default reset/reload, and `SM_HOUSE_UPDATE` broadcast are not implemented.
- House-object known-list delivery still uses the current world-house distance scan rather than full region-bucketed Java `KnownList` membership.
- Already sighted non-owner players do not yet receive all live object placement/move/despawn side effects after owner edit actions.
- `CM_USE_HOUSE_OBJECT` and `CM_RELEASE_OBJECT` are still pending, including reward/item checks, occupant/release behavior, cooldown mutation, and object-use update packets.
- Studio addresses `2001`/`3001` remain excluded from global world-house load; Java spawns studios per personal instance and that flow remains pending.
- GeoService door state updates, visitor zone filtering/teleport-out, and live-DB orphan-house validation remain pending.

---

## Suggested Next Units

1. Implement `CM_HOUSE_EDIT` action `3` register-from-inventory for placeable house objects and decor: parse item `DecorateAction`/`SummonHouseObjectAction` metadata, allocate IDs, delete the inventory item, insert `player_registered_items`, update registry cache, and send Java `SM_HOUSE_EDIT(3, storeId, objectId)` responses.
2. Add registered decoration mutation support: `HouseRegistry.setUsed`, room validation, default-part discard behavior, DB update/delete, and updated `SM_HOUSE_UPDATE`/`SM_HOUSE_RENDER` decor lines that reflect selected registered decor.
3. Add renovation action `16`: coupon lookup/removal by race/house type, building switch, registry reset/reload, default decor refresh, and `SM_HOUSE_UPDATE` broadcast.
4. Add `CM_USE_HOUSE_OBJECT`/`CM_RELEASE_OBJECT` shells once the useable-object behavior slice starts: owner/visitor checks, required-item/check-type handling, use counts, reward/final-reward paths, cooldown updates, and `SM_OBJECT_USE_UPDATE`.
5. Continue world/known-list fidelity for house objects: broadcast placement/move/despawn side effects to already sighted non-owner players and eventually replace the distance scan with a fuller region/known-list model.
6. Add per-instance studio spawning once a C# personal-instance/home entry flow exists.
7. Add GeoService door update hooks for house door-state changes.
8. Continue visitor kick side effects with house-zone membership, friend/legion filtering, recipient messages, and teleport-out to loaded exit coordinates.
9. Continue AP/skill side effects only when a real C# home exists for legion contribution, siege callbacks, or passive `SkillEngine` effect fanout.
