# Phase 6J Completion Handoff

**Created**: May 21, 2026
**Status**: Phase 6 remains in progress; this handoff follows the 6I stigma/CM_MANASTONE continuation.
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, persistence behavior, side effects, and naming.
**Workflow rule**: Do one focused unit of work, validate it, commit it, then repeat for as long as useful work remains.
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 385 tests.

---

## Recent Work Completed

- Added Java `CM_MANASTONE` action `3` manastone removal parity: cube-only target lookup, normal/fusion `item_stones` slot deletion, Java 650 Kinah fee, Java-shaped `STR_REMOVE_ITEM_OPTION_*` messages, target/Kinah inventory updates, and DB-backed stone delete/Kinah persistence.
- Added Java `CM_MANASTONE` action `4` godstone socketing foundation: cube-only non-equipped target validation, Java `CAN_PROC_ENCHANT` mask parity, godstone source/template validation, source consume/delete, target godstone replacement in `item_stones`, Java-shaped proc messages, and DB-backed persistence.
- Added Java `CM_MANASTONE` action `8` amplification foundation: parsed Java `max_enchant` / `can_exceed_enchant`, validated target/material/tool rules from `EnchantService.amplifyItem`, consumed material/tool items, persisted `inventory.is_amplified`, and emitted Java-shaped `STR_MSG_EXCEED_*` packets.
- Added Java `ItemSocketService.addManaStone` slot allocator foundation: parsed Java `m_slots` / `s_slots`, resolved primary/fusion socket counts with the six-basic-stone cap, selected normal vs special manastone slots, and covered full-category failure cases.
- Kept `docs/PHASE-6-PROGRESS.md` current through Session 153.

---

## Important Limits

- Non-stigma `CM_MANASTONE` action `2` is not wired yet: the slot allocator exists, but delayed use animations, success chance, source/supplement consumption, DB `item_stones` insertion, equipped stat refresh, and failure side effects remain pending.
- Non-stigma `CM_MANASTONE` action `1` enchant-stone behavior is still pending: `EnchantItemAction.canAct`, `EnchantmentStone` level/quality math, amplified/Omega handling, enchant failure rollback, buff-skill changes, and enchant announce fanout are not ported.
- Godstone and stigma/manastone item-use flows still use immediate first-pass completion where Java schedules delayed tasks with movement cancel observers, item cooldown cleanup, and cancel system messages.
- Manastone removal still has only first-pass NPC validation by current target object; Java talk-range, known-list, template-function, and audit checks remain future work.
- Full Java `SkillEngine` effect application after temporary stigma skill mutations is still pending, as are broader stat-container lifecycle and equip/unequip recompute fanout.
- Charge and idian burn observers remain unwired into combat or skill execution: `ChargeInfo`, `PolishChargeCondition`, and `IdianStone.onEquip` attack/defend hooks are still future slices.
- Real-client validation remains deferred until the end-of-port readiness pass.

---

## Suggested Next Units

1. Wire Java non-stigma `CM_MANASTONE` action `2` using the new slot allocator: no-supplement manastone chance math first, source consumption, DB `item_stones` insertion, Java-shaped animations/messages, item update packets, and equipped stat refresh.
2. Extend action `2` with supplement validation/count math once item action metadata is parsed, preserving Java `EnchantItemAction.checkSupplementLevel` and `EnchantService.socketManastone` count rules.
3. Port Java `CM_MANASTONE` action `1` enchant-stone handling: `EnchantItemAction.canAct`, `EnchantService.enchantItem`, `enchantItemAct`, item enchant persistence, buff-skill updates, and amplified/Omega restrictions.
4. Build a reusable delayed item-use/cancel foundation for the current immediate branches: 2s/4s/5s scheduling, movement observers, item cooldown abort cleanup, cancel messages, and stance/state denial hooks.
5. Continue the remaining stigma/effect and observer queue: full `SkillEngine` effect apply/remove for temporary skills, charge/idian burn triggers, persistent known-list/NPC/dialog validation, or housing maintenance/settlement flows.
