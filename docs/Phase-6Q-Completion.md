# Phase 6Q Completion Handoff

**Created**: May 22, 2026
**Status**: Phase 6 remains in progress; this handoff follows 6P and covers Sessions 199-207.
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, persistence behavior, side effects, and naming.
**Workflow rule**: Do one focused unit of work, validate it, commit it, then repeat for as long as useful work remains.
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 445 tests.

---

## Recent Work Completed

- Added a C# `ExpireTimerTask` bridge for temporary social unlocks: loaded/new emotions, titles, and motions register on enter-world/use, unregister on logout, expire with Java's `remainingSeconds < 0` threshold, clean up persistence, and send Java timeout packets/messages.
- Ported Java `CM_APPEARANCE` type `2` cosmetic item actions: cosmetic static data and item `<cosmetic>` metadata load, race/gender/ride guards run, supported appearance mutations persist, the cosmetic item is deleted, and visible players receive a refreshed `SM_PLAYER_INFO`.
- Loaded Java `decomposable_items.xml` into a typed C# holder and parsed item-template `<decompose/>` markers.
- Ported Java decomposition runtime for `CM_USE_ITEM` and `CM_SELECT_DECOMPOSABLE`: selectable UI packets, normal 3s use animation/cancel path, fixed/random reward selection, source consumption, success/failure messages, reward persistence, and Java decompose packet IDs are implemented.
- Added reusable Java-shaped item-add helpers: `InventoryItemFactory` and `InventoryAddService` now merge stackable rewards, split new stacks by `max_stack_count`, report remaining count for non-overflow callers, and can use template metadata for normal vs special cube slots.
- Parsed Java item-template `<inventory id="...">` metadata and tightened decompose canAct to separate normal cube fullness from full special-cube reward checks.
- Parsed Java item-template `<remodel>` metadata and opcode `90` `CM_ITEM_REMODEL`.
- Ported first-pass Java `ItemRemodelService.remodelItem` runtime: level/Kinah/gender/type/remodelable guards, pattern-reshaper removal, target `item_skin`/color updates, extract item consumption, Kinah mutation, DB persistence, inventory packets, and remodel system messages.
- Kept `docs/PHASE-6-PROGRESS.md` current through Session 207, with the next-step queue updated.

---

## Important Limits

- Real-client validation remains deferred until the end-of-port readiness pass.
- Remodel runtime still lacks Java NPC distance/function validation, full preview/cosmetic restrictions, and timed remodel-skin expiration from `RemodelAction.minutes`.
- The item-add planner still needs richer Java partial-add caller behavior and broader reuse outside decompose/remodel-adjacent flows.
- The expirable bridge currently covers temporary social unlocks only. Java also registers expirable inventory/equipment items, pets, and house objects.
- Cosmetic action covers type `2`; character rename and legion rename coupon branches remain pending.
- Decompose random reward support covers the Java branches currently mirrored in `DecomposeAction`, but richer add-failure and full special-cube behavior should be revisited as item services broaden.
- Passive `SkillEngine` effect application, full stat/effect removal fanout, profession side effects, quest/summon observers, stance observers, power-shard side effects, charge/idian burn triggers, persistent known-list membership, and full NPC/dialog validation remain pending.

---

## Suggested Next Units

1. Tighten reusable item-add partial-add behavior and start reusing it in the next reward/item service that creates inventory rows.
2. Broaden `ExpireTimerTask` coverage to Java's expirable inventory/equipment items, pets, and house objects.
3. Resume the stigma/effect slice with full `SkillEngine` effect apply/remove behavior and the corresponding stat/effect packet fanout.
4. Add NPC/dialog known-list and function validation, then revisit dialog-owned item services such as remodel preview, NPC-paid expansion, and vendor-like flows.
5. Continue `CM_EMOTION` only after introducing one missing support model such as fly-zone/cooldown/FP timers, stance observers, sit observers, quest/summon observers, or reusable stat-speed calculation.
6. Wire charge, power-shard, and idian burn triggers into the future skill/combat observer paths.
7. Continue housing auction settlement, maintenance, sign, and appearance flows when the next slice should stay out of the stat engine.
