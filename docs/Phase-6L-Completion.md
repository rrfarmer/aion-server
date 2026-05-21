# Phase 6L Completion Handoff

**Created**: May 21, 2026
**Status**: Phase 6 remains in progress; this handoff follows the 6K enchant-stone and delayed-item-use continuation.
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, persistence behavior, side effects, and naming.
**Workflow rule**: Do one focused unit of work, validate it, commit it, then repeat for as long as useful work remains.
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 404 tests.

---

## Recent Work Completed

- Ported Java non-stigma `CM_MANASTONE` action `1` enchant-stone handling through `EnchantService.CreateEnchantItemPlan`: `EnchantItemAction.canAct` guard order, `EnchantmentStone.getByItemId` Alpha/Beta/Gamma/Delta/Epsilon/Omega mapping, base/amplified rate config, level/quality/enchant modifiers, supplement chance/count handling, +1/+2/+3 crit rolls, success caps, failure downgrade/amplified reset/destruction behavior, source/supplement consume/delete, target enchant/tune/amplified persistence, Java-shaped system messages, and +15/+20 race-filtered announce fanout.
- Parsed Java `ItemTemplate.exceed_enchant_skill` and added Java `EnchantService.setEnchantLevel` / `getEquipBuff` parity for +20 enchant buff skills: `inventory.buff_skill` persistence, equipped temporary skill add/remove packets, and `STR_MSG_EXCEED_SKILL_ENCHANT`.
- Added a Java `ThreadPoolManager.schedule` one-shot scheduler foundation with cancellable `ScheduledTask` handles beside the existing fixed-rate scheduler.
- Migrated `CM_MANASTONE` action `1` enchant-stone completion to Java 4s delayed `TaskId.ITEM_USE` scheduling, with movement cancellation sending Java end-state `3` item-use animation and `STR_ENCHANT_ITEM_CANCELED`.
- Migrated `CM_MANASTONE` action `2` manastone socketing completion to Java 2s delayed `TaskId.ITEM_USE` scheduling, with movement cancellation sending Java end-state `3` item-use animation and `STR_GIVE_ITEM_OPTION_CANCELED`.
- Kept `docs/PHASE-6-PROGRESS.md` current through Session 163.

---

## Important Limits

- Stigma charge, item charge, idian polish, soul-bind, godstone socketing, and amplification still need migration onto the one-shot `TaskId.ITEM_USE` scheduler where Java delays/cancels them.
- Item cooldown abort cleanup is not yet wired into delayed item-use cancellation.
- Manastone removal still has only first-pass NPC validation by current target object; Java talk-range, known-list, template-function, and audit checks remain future work.
- Godstone socketing has persistence and packet foundation, but combat proc activation and future SkillEngine hooks are not ported.
- Full Java `SkillEngine` effect application after temporary skill mutations is still pending, as are broader stat-container lifecycle and equip/unequip recompute fanout.
- Charge and idian burn observers remain unwired into combat or skill execution: `ChargeInfo`, `PolishChargeCondition`, and `IdianStone.onEquip` attack/defend hooks are still future slices.
- Real-client validation remains deferred until the end-of-port readiness pass.

---

## Suggested Next Units

1. Continue migrating delayed item-use paths onto the new one-shot scheduler, starting with stigma charge or godstone socketing because both already have isolated mutation plans and packet flows.
2. Add item-use cancellation cleanup for item cooldowns and shared pending-use state once more delayed branches use the scheduler.
3. Continue the remaining stigma/effect slice: full `SkillEngine` effect apply/remove for temporary skills and corresponding stat/effect removal fanout.
4. Wire charge and idian burn triggers into future skill/combat observer paths: `ChargeInfo`, `PolishChargeCondition`, low-charge update packets, zero-charge deletion, and stat refresh fanout.
5. Continue persistent known-list/NPC/dialog validation, housing maintenance/settlement/sign/appearance flows, or broader stat-container lifecycle when the next slice should stay out of enchant handling.
