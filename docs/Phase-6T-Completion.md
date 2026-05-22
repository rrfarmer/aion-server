# Phase 6T Completion Handoff

**Created**: May 22, 2026
**Status**: Phase 6 remains in progress; this handoff follows 6S and covers Sessions 222-229.
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, persistence behavior, side effects, and naming.
**Workflow rule**: Do one focused unit of work, validate it, commit it, then repeat for as long as useful work remains.
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 473 tests.

---

## Recent Work Completed

- Finished the next AP rank-change side effects after AP extraction: visible-player `SM_ABYSS_RANK_UPDATE`, rank-limited equipment cleanup, configured abyss transform skill add/remove, and Java `gameserver.topranking.xform.min_rank` config loading.
- Added login-time rank-limited equipment cleanup, matching Java `PlayerEnterWorldService` calling `Equipment.checkRankLimitItems` when a stored rank changed while the player was offline.
- Added housing appearance packet coverage and fanout for `CM_HOUSE_SETTINGS`: `SM_HOUSE_UPDATE` broadcasts now use the Java common house-info payload, including owner, door, sign, legion, type, and placeholder decor fields.
- Added house known-list packet surfaces: `SM_HOUSE_RENDER` and `SM_DELETE_HOUSE`, with a shared C# writer for Java `AbstractHouseInfoPacket.writeCommonInfo`.
- Added the owner-facing close-door visitor notices from Java `HouseController.kickVisitors` for friends-only and fully closed house settings.
- Loaded Java `HouseAddress` map coordinates and optional exit coordinates into `HousingAddressSummary`, preparing future house known-list and visitor teleport-out work to use real static data.
- Kept `docs/PHASE-6-PROGRESS.md` current through Session 229, with validation and next-step queue updated.

---

## Commits In This Handoff

- `e8bc16d75` - `Port rank-limited equipment unequip`
- `edc6427f4` - `Port abyss transform skill updates`
- `1cbf09e90` - `Load abyss transform rank config`
- `ea71dfaf4` - `Broadcast house settings appearance update`
- `0df54184d` - `Add house visibility packets`
- `878d854bd` - `Port house close visitor notices`
- `00aaa2223` - `Run rank limit equipment check on login`
- `cce16105c` - `Load housing address coordinates`

---

## Important Limits

- AP extraction still lacks Java legion contribution fanout and `SiegeService.onAbyssPointsAdded`, because the supporting legion/siege service homes are not yet ported.
- Abyss transform skill mutations still do not run passive `SkillEngine` effect apply/remove fanout; that remains part of the broader SkillEngine/stat slice.
- Housing packets and address data are ready, but C# still lacks a persistent house world object/store and real known-list membership for automatic render/delete on visibility changes.
- House close visitor behavior now sends the owner close-out notices, but still lacks house-zone membership, friend/legion visitor filtering, teleport-out movement, GeoService door updates, and recipient messages.
- Expirable pets and house objects remain pending until pet and house-object models and persistence surfaces exist.
- Scheduled item-use ordering still needs real-client validation for decompose, assembly, XP extraction, composition, extraction, and AP extraction.

---

## Suggested Next Units

1. Continue AP side effects only when a real C# home exists for legion contribution or siege callbacks.
2. Continue housing if staying out of the stat engine: build persistent house/known-list membership around the new render/delete packet surfaces and loaded address coordinates, then move into GeoService door updates and visitor teleport-out.
3. Resume the stigma/effect slice with full `SkillEngine` effect apply/remove behavior and corresponding stat/effect packet fanout.
4. Broaden the expirable lifecycle bridge to pets and house objects once those models and persistence surfaces exist.
5. Wire charge, power-shard, and idian burn triggers when the combat/skill observer paths can call them.
6. Continue `CM_EMOTION` only after introducing one missing support model such as fly-zone/cooldown/FP timers, stance observers, sit observers, quest/summon observers, or reusable stat-speed calculation.
7. Keep real-client scheduled item-use validation for the later readiness pass.
