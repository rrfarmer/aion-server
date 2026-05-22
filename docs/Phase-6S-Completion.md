# Phase 6S Completion Handoff

**Created**: May 22, 2026
**Status**: Phase 6 remains in progress; this handoff follows 6R and covers Sessions 217-221.
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, persistence behavior, side effects, and naming.
**Workflow rule**: Do one focused unit of work, validate it, commit it, then repeat for as long as useful work remains.
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 466 tests.

---

## Recent Work Completed

- Added a C# planner for Java `services/EnchantService.breakItem`: source/target cube lookup, weapon/armor guard, Java effective-level calculation, alpha/beta/gamma/delta/epsilon stone selection, weapon +5 level bump, Java random reward-count ranges, target deletion, source consume/decrement, and reward insertion through the reusable item-add planner.
- Wired Java `<extract/>` item-use runtime through `CM_USE_ITEM`: self-only 5s `SM_ITEM_USAGE_ANIMATION`, Java cancel message/end state, canAct failure messages, DB-backed target/source/reward mutation, target/source inventory packets, reward update/add packets, dice-inventory failure behavior, and expirable reward registration.
- Loaded AP extraction target metadata from Java item templates: `ItemMask.CAN_AP_EXTRACT` and `<acquisition ap="...">` now live on `ItemTemplateSummary`.
- Ported Java `ApExtractAction.canAct + act`: source/target validation, target mask, tool level/quality/target-type checks, AP calculation from Java rate and required AP, target deletion, source consume/decrement, AP rank mutation persistence, AP gain system message, and owner `SM_ABYSS_RANK`.
- Added C# `SM_ABYSS_RANK_UPDATE` opcode `136` parity and wired AP extraction rank changes to broadcast action `0` to visible players after owner rank refresh.
- Kept `docs/PHASE-6-PROGRESS.md` current through Session 221, with validation and next-step queue updated.

---

## Commits In This Handoff

- `7bb831193` - `Port break item extraction planner`
- `0a97968f5` - `Wire extract item action runtime`
- `44af10b3c` - `Load AP extraction item metadata`
- `d7849c132` - `Port AP extraction item action`
- `f457f0146` - `Port abyss rank update packet`

---

## Important Limits

- Real-client validation remains deferred until the end-of-port readiness pass.
- Extract/AP extraction still lack Java quest item-removed observer callbacks and real-client ordering validation for delayed item-use packet sequences.
- AP extraction now sends owner `SM_ABYSS_RANK` plus visible `SM_ABYSS_RANK_UPDATE`, but still lacks Java `Equipment.checkRankLimitItems`, `AbyssSkillService.updateSkills`, legion contribution fanout, and siege callback handling.
- XP extraction still lacks Java `PlayerCommonData.setExp` level-change side effects and quest item-removed observer callbacks.
- Assembly/composition still lack Java quest item-removed observer callbacks and real-client ordering validation of scheduled packet sequences.
- Expirable pet and house-object rows remain pending until their models and persistence surfaces exist.
- Casting/protection interruption, full item-use observer behavior, stance observers, quest/summon observers, power-shard side effects, charge/idian burn triggers, full SkillEngine effect apply/remove fanout, persistent known-list membership, and full NPC/dialog validation remain pending.

---

## Suggested Next Units

1. Finish AP rank-change side effects beyond the current packets: rank-limited equipment checks, Abyss skill updates, legion contribution fanout, and siege callback coverage once those supporting systems have C# homes.
2. Broaden the expirable lifecycle bridge to Java's remaining registered expirable types, pets and house objects, once the missing pet/house-object models and persistence surfaces exist.
3. Resume the stigma/effect slice with full `SkillEngine` effect apply/remove behavior and the corresponding stat/effect packet fanout.
4. Continue `CM_EMOTION` only after introducing one missing support model such as fly-zone/cooldown/FP timers, stance observers, sit observers, quest/summon observers, or reusable stat-speed calculation.
5. Wire charge, power-shard, and idian burn triggers into the future skill/combat observer paths.
6. Continue housing auction settlement, maintenance, sign, and appearance flows when the next slice should stay out of the stat engine.
7. Add persistent known-list membership and full NPC/dialog function validation, then revisit dialog-owned item services such as remodel preview, NPC-paid expansion, and vendor-like flows.
