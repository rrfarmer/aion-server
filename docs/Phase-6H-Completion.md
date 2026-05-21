# Phase 6H Completion Handoff

**Created**: May 21, 2026
**Status**: Phase 6 remains in progress; this handoff follows the 6G equipment/stigma continuation.
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, persistence behavior, side effects, and naming.
**Workflow rule**: Do one focused unit of work, validate it, commit it, then repeat for as long as useful work remains.
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 362 tests.

---

## Recent Work Completed

- Added Java `Item.isIdentified()` parity from `tune_count`, including the silent `CM_EQUIP_ITEM` unidentified guard, item-info blob hiding for unresolved tuning data, and idian polish identify denial.
- Added soul-bind confirmation foundation for `CM_EQUIP_ITEM`: Java question-window code `95006`, accept/cancel handling, start/finish item-use animations, success/cancel/retry messages, `inventory.is_soul_bound` persistence, and final equip mutation.
- Added a first-pass StigmaService equip/unequip bridge: Java `<stigma>` metadata parsing, skill-template stigma attributes, skill-tree lookup helpers, regular/advanced stigma slot-count gates, Kinah equip cost, temporary normal stigma skill add/remove, `SM_SKILL_LIST` stigma learn packets, `SM_SKILL_REMOVE`, stigma system messages, and Kinah count persistence.
- Added linked stigma unlock parity for the modeled equip path, including Java's class/race/item-ID linked-stigma selection table, minimum-enchant linked skill level, and linked skill type `3` learn packets.
- Kept `docs/PHASE-6-PROGRESS.md` current through Session 142.

---

## Important Limits

- `CM_EQUIP_ITEM` now covers the mutation path, Java class/level/max-level/race/gender/AP-rank/required-skill/unidentified guards, soul-bind confirmation foundation, and a first-pass stigma equip/unequip skill/Kinah/linked-unlock bridge.
- Soul-bind confirmation has a first response/persistence/equip foundation, but exact Java 5s delayed task timing, movement cancellation observer, and stance/state denials are still pending.
- Stigma enchant/charge-stone flow, membership slot overrides, exact hidden-skill message behavior, and full SkillEngine effect application are still pending.
- Power-shard equip/unequip emotion side effects, quest/summon observers, exact speed/emotion fanout, and full Java `CreatureGameStats` lifecycle remain pending.
- Charge and idian burn observers are still not wired into combat or skill execution.
- `CM_DIALOG_SELECT` charge-all still lacks full NPC known-list lookup, NPC template action/function validation, distance/protection/audit checks, and generalized response-request state.

---

## Suggested Next Units

1. Continue `CM_EQUIP_ITEM` parity with a consciously scoped branch: stigma enchant/charge-stone flow, membership slot overrides, or exact soul-bind timing/cancel/stance behavior once player state support exists.
2. Tighten StigmaService behavior in slices: exact hidden-skill messages, linked-skill removal edge cases, and full Java SkillEngine effect application after temporary skill mutations.
3. Wire charge and idian burn triggers into future combat/skill observer paths: `ChargeInfo`, `PolishChargeCondition`, `IdianStone.onEquip` attack/defend observers, low-charge update packets, zero-charge deletion, and stat refresh fanout.
4. Choose the next passive skill/effect stat slice beyond mastery/title modifiers: generic passive `BufEffect` stat parsing, transform/title movement-speed serialization, or a fuller Java `Stat2` bridge.
5. Continue housing auction settlement/maintenance/sign/appearance flows, persistent known-list membership, or full NPC/dialog known-list/function validation when the next slice should stay out of equipment/stat internals.
