# Phase 6M Completion Handoff

**Created**: May 21, 2026
**Status**: Phase 6 remains in progress; this handoff follows the 6L delayed-item-use continuation.
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, persistence behavior, side effects, and naming.
**Workflow rule**: Do one focused unit of work, validate it, commit it, then repeat for as long as useful work remains.
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 404 tests.

---

## Recent Work Completed

- Migrated Java `CM_MANASTONE` action `4` godstone socketing onto the C# one-shot `TaskId.ITEM_USE` scheduler, including 2s completion delay, movement cancel end-state `3`, and `STR_MSG_GIVE_PROC_CANCEL`.
- Migrated stigma charge through `CM_MANASTONE` onto the same scheduler, including Java 5s timing, target-stigma/charge-stone cancel animation end-state `2`, generic `STR_ITEM_CANCELED`, delayed source/target persistence, temporary stigma skill updates, and stat refresh.
- Migrated item `ChargeAction` conditioning to Java 3s delayed execution, including charge-way-specific cancel messages and delayed source consume plus equipped charge updates.
- Migrated `PolishAction` idian polish to Java 5s delayed execution, including delayed source consume, target idian mutation, success/failure messaging, and generic item-use cancel behavior.
- Migrated soul-bind confirmation accept flow to Java 5s delayed execution, including start animation end-state `4`, movement cancel end-state `8`, success end-state `6`, delayed equipment mutation, and appearance/stat/skill fanout through the existing equip path.
- Added Java `Equipment.soulBindItem` invalid-stance guard/message parity before the soul-bind question path: dead, ride, chair, resting, gliding, flying, and weapon-equipped checks now emit `STR_SOUL_BOUND_INVALID_STANCE(ChatUtil.l10n(...))`.
- Confirmed Java `EnchantService.amplifyItem` is immediate and should remain immediate in C# unless a different Java branch is later identified.
- Kept `docs/PHASE-6-PROGRESS.md` current through Session 170.

---

## Important Limits

- Item cooldown abort cleanup is still not wired into the shared delayed item-use cancellation path.
- The delayed item-use plans are precomputed at start time, matching the current C# service shape; broader live revalidation of source/target item state at completion remains future hardening if Java parity requires it.
- Manastone removal still has only first-pass NPC validation by current target object; Java talk-range, known-list, template-function, and audit checks remain future work.
- Godstone socketing has persistence and packet foundation, but combat proc activation and future SkillEngine hooks are not ported.
- Full Java `SkillEngine` effect application after temporary skill mutations is still pending, as are broader stat-container lifecycle and equip/unequip recompute fanout.
- Charge and idian burn observers remain unwired into combat or skill execution: `ChargeInfo`, `PolishChargeCondition`, and `IdianStone.onEquip` attack/defend hooks are still future slices.
- Real-client validation remains deferred until the end-of-port readiness pass.

---

## Suggested Next Units

1. Add shared delayed item-use abort cleanup for item cooldowns and any per-player using-item state now that enchant, manastone, godstone, stigma charge, charge action, idian polish, and soul-bind all use the scheduler.
2. Continue remaining item-use parity around power-shard emotion side effects, quest/summon observers, and exact speed/emotion fanout.
3. Continue the remaining stigma/effect slice: full `SkillEngine` effect apply/remove for temporary skills and corresponding stat/effect removal fanout.
4. Wire charge and idian burn triggers into future skill/combat observer paths: `ChargeInfo`, `PolishChargeCondition`, low-charge update packets, zero-charge deletion, and stat refresh fanout.
5. Continue persistent known-list/NPC/dialog validation, housing maintenance/settlement/sign/appearance flows, or broader stat-container lifecycle when the next slice should stay out of item-use timing.
