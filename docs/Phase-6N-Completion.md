# Phase 6N Completion Handoff

**Created**: May 21, 2026
**Status**: Phase 6 remains in progress; this handoff follows the 6M delayed-item-use and emotion continuation.
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, persistence behavior, side effects, and naming.
**Workflow rule**: Do one focused unit of work, validate it, commit it, then repeat for as long as useful work remains.
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 415 tests.

---

## Recent Work Completed

- Added Java `Equipment.soulBindItem` invalid-stance guard/message parity before the soul-bind question path: dead, ride, chair, resting, gliding, flying, and weapon-equipped checks now emit `STR_SOUL_BOUND_INVALID_STANCE(ChatUtil.l10n(...))`.
- Added Java `ItemUseLimits.usedelay/usedelayid` parsing plus C# player item-cooldown/using-item cleanup support in the shared delayed item-use scheduler; idian `PolishAction` removes its cooldown on movement abort while `ChargeAction` keeps Java's no-remove behavior.
- Added Java `CM_EMOTION` opcode `43` / `SM_EMOTION` opcode `37` foundation and wired power-shard on/off side effects: equipped-shard guard/message, `CreatureState.POWERSHARD` toggle, item-use cancel, and visible broadcast including self.
- Extended Java `CM_EMOTION` state-only branches for sit/stand, chair sit/up, weapon draw/sheath, and walk/run, including Java exact-match multibit creature-state semantics for chair/private-shop and chair replacement behavior.
- Wired Java `EmotionList.canUse` custom-emote fanout and then replaced the temporary range shortcut with exact `EmotionLearnAction.isLearnable` parity by parsing `<learnemotion emotionid="...">` item actions into `ItemTemplateTable.LearnableEmotionIds`.
- Added Java `Equipment.unEquipItem` power-shard side-effect parity: successful power-shard unequip unsets `CreatureState.POWERSHARD` and sends owner-only `SM_EMOTION(POWERSHARD_OFF)` after C# persistence succeeds.
- Added Java `CM_EMOTION` no-mutation branch parity for `SELECT_TARGET`, `JUMP`, `OPEN_DOOR`, and `CLOSE_DOOR`, including item-use cancellation semantics and the drawn-weapon jump guard.
- Added the first Java `FlyController.startFly/endFly` state side effects to `CM_EMOTION`: `FLY` sets `FLYING` and ride-mode `FLOATING_CORPSE`, while `LAND` clears `FLYING`, `GLIDING`, and `FLOATING_CORPSE`.
- Kept `docs/PHASE-6-PROGRESS.md` current through Session 179.

---

## Important Limits

- Broader `CM_EMOTION` behavior still needs abnormal-state/fear/confuse guards, stance-denial messages, full fly/land eligibility/cooldown/FP/stat-controller behavior, fly-teleport/sprint controller behavior, sit observers, quest/summon observers, and exact movement/attack speed fanout.
- Broader `CM_USE_ITEM` action routing still needs Java cooldown application as additional item actions are ported beyond the current polish/charge subset.
- Full Java `SkillEngine` effect application after temporary skill mutations is still pending, as are broader stat-container lifecycle and equip/unequip recompute fanout.
- Charge and idian burn observers remain unwired into combat or skill execution: `ChargeInfo`, `PolishChargeCondition`, and `IdianStone.onEquip` attack/defend hooks are still future slices.
- Power-shard burn-out and automatic same-stack replacement from Java `Equipment.usePowerShard` remain future combat/observer work.
- The delayed item-use plans are precomputed at start time, matching the current C# service shape; broader live revalidation of source/target item state at completion remains future hardening if Java parity requires it.
- Manastone removal still has only first-pass NPC validation by current target object; Java talk-range, known-list, template-function, and audit checks remain future work.
- Godstone socketing has persistence and packet foundation, but combat proc activation and future SkillEngine hooks are not ported.
- Real-client validation remains deferred until the end-of-port readiness pass.

---

## Suggested Next Units

1. Continue `CM_EMOTION` only if the next slice first introduces one missing support model: abnormal effect flags, stance state/messages, fly-zone/cooldown/FP timers, ride/sprint data, observers, or reusable stat-speed calculation.
2. Continue the remaining stigma/effect slice: full `SkillEngine` effect apply/remove for temporary skills and corresponding stat/effect removal fanout.
3. Wire charge, power-shard, and idian burn triggers into future skill/combat observer paths: `ChargeInfo`, `PolishChargeCondition`, `Equipment.usePowerShard`, low-charge update packets, zero-charge deletion, and stat refresh fanout.
4. Continue persistent known-list/NPC/dialog validation, housing maintenance/settlement/sign/appearance flows, or broader stat-container lifecycle when the next slice should stay out of item-use timing.
5. Broaden Java `CM_USE_ITEM` action routing and cooldown application as additional item actions are ported beyond the current polish/charge subset.
