# Phase 6V Completion Handoff

**Created**: May 22, 2026  
**Status**: Phase 6 remains in progress; this handoff follows 6U and covers Sessions 234-240.  
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, persistence behavior, side effects, and naming.  
**Workflow rule**: Do one focused unit of work, validate it, commit it, then repeat for as long as useful work remains.  
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.  
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 478 tests.

---

## Recent Work Completed

- Synthesized ownerless custom-house world snapshots for Java `HousingService.spawnHouses` behavior when static `HouseAddress` rows have no `houses` DB row, using default buildings, owner id `0`, closed doors, and `showOwnerName=true`.
- Ported Java `HousingService.revokeOwnershipOfDeletedPlayers`: custom-house rows with missing player owners are reset to ownerless/default-building state before world snapshots are built.
- Parsed Java `housing/house_buildings.xml` building part defaults into C# and updated `SM_HOUSE_UPDATE`/`SM_HOUSE_RENDER` common info to write the 19 Java `PartType` default decor lines instead of zero placeholders.
- Added `SM_HOUSE_REGISTRY` opcode `116` packet scaffolding for registered object rows and decoration/default-part rows, including unique `Building.getDefaultPartIds` support.
- Added `CM_HOUSE_EDIT` opcode `82` parsing plus simple `SM_HOUSE_EDIT` opcode `82` mode responses; entering decoration mode now sends Java's `SM_HOUSE_EDIT(1)`, `SM_HOUSE_REGISTRY(1)`, and `SM_HOUSE_REGISTRY(2)` sequence.
- Loaded Java `housing/housing_objects.xml` into `HousingObjectTemplateTable`, preserving concrete `PlaceableHouseObject.getTypeId` mappings and fields needed by registry/object work.
- Added `SM_HOUSE_OBJECT` opcode `268` and `SM_HOUSE_OBJECTS` opcode `270` packet scaffolding for spawned house-object visibility rows and compact object lists.
- Kept `docs/PHASE-6-PROGRESS.md` current through Session 240, including the updated next-step queue.

---

## Commits In This Handoff

- `0369eb7bd` - `Synthesize ownerless world houses`
- `a239fb098` - `Revoke deleted house owners on load`
- `875b9c2fe` - `Write default house decor lines`
- `325befbc1` - `Add house registry packet shell`
- `ca7130ed2` - `Wire house edit mode packets`
- `d3aba75e6` - `Load housing object templates`
- `870dcba3e` - `Add house object visibility packets`

---

## Important Limits

- Studio addresses `2001`/`3001` are still excluded from global world-house load; Java spawns studios per personal instance and that flow remains pending.
- Registry/object packets are byte-shaped and tested, but C# still does not load `player_registered_items` rows into house registries.
- `CM_HOUSE_EDIT` currently handles mode entry/exit only; add/delete/spawn/move/despawn actions and renovation coupon/building switch behavior remain pending.
- Spawned house-object packets are not emitted by a known-list path yet, and useable-object packet-tail data still needs a Java-shaped model.
- GeoService door state updates are still not ported, so door changes update packets/DB/world snapshots but not collision/geo.
- Visitor kick behavior still lacks real house-zone membership, friend/legion filtering, teleport-out movement using address exit coordinates, and visitor recipient messages.
- Deleted-owner revocation has unit/full-suite coverage, but live-DB validation with an orphaned `houses.player_id` is still an opt-in operational check.

---

## Suggested Next Units

1. Load `player_registered_items` into C# house registry summaries using `HousingObjectTemplateTable`, separating `area='DECOR'` decorations from placeable objects and preserving Java deleted/invalid decor behavior.
2. Wire spawned registry objects into world/known-list visibility so `PlayerController.see/notSee` can emit `SM_HOUSE_OBJECT`/`SM_HOUSE_OBJECTS` where Java would.
3. Implement `CM_HOUSE_EDIT` actions `3`/`4`/`5`/`6`/`7` against the registry summaries: register item, delete item, spawn object, move object, and despawn object.
4. Add renovation action `16`: coupon lookup/removal by race/house type, building switch, registry default reload/reset behavior, and `SM_HOUSE_UPDATE` broadcast.
5. Add per-instance studio spawning once a C# personal-instance/home entry flow exists.
6. Add GeoService door update hooks for house door-state changes.
7. Continue visitor kick side effects with house-zone membership, friend/legion filtering, recipient messages, and teleport-out to loaded exit coordinates.
8. Continue AP/skill side effects only when a real C# home exists for legion contribution, siege callbacks, or passive `SkillEngine` effect fanout.
