# Phase 6I Completion Handoff

**Created**: May 21, 2026
**Status**: Phase 6 remains in progress; this handoff follows the 6H stigma/equipment continuation.
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, persistence behavior, side effects, and naming.
**Workflow rule**: Do one focused unit of work, validate it, commit it, then repeat for as long as useful work remains.
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 373 tests.

---

## Recent Work Completed

- Added Java `CM_MANASTONE` opcode `74` parsing for the stigma charge-stone branch and a first-pass `StigmaService.chargeStigma` parity flow: matching charge stones consume with Java `DEC_STIGMA_USE`, success increments `inventory.enchant`, failure destroys the stigma, equipped stigma skills refresh/remove, and Java-shaped animation/system/inventory side effects are emitted.
- Added Java `MembershipConfig` stigma keys `gameserver.quest.stigma.slot` and `gameserver.autolearn.stigma`; `STIGMA_SLOT_QUEST` now gates regular/advanced stigma slot counts via `Player.AccountMembership`.
- Added Java `StigmaService.onPlayerLogin` `STIGMA_AUTOLEARN` parity: membership-qualified players learn temporary stigma skills from level 20 through current level before `SM_ENTER_WORLD_CHECK` and the initial `SM_SKILL_LIST`.
- Added Java `StigmaService.removeLinkedStigmaSkills` hidden-delete parity: linked stigma skills are removed in stack groups and emit `STR_MSG_STIGMA_DELETE_HIDDEN_SKILL` (`1402895`) through equip/unequip and stigma charge mutation paths.
- Added the non-autolearn branch of Java `StigmaService.onPlayerLogin`: normal accounts validate equipped stigma stones for slot permission, class specificity, and duplicate same-slot conflicts, persist invalid stones as unequipped, and rebuild temporary normal/linked stigma skills for valid equipped stones before initial skill serialization.
- Kept `docs/PHASE-6-PROGRESS.md` current through Session 148.

---

## Important Limits

- Stigma equip/unequip/charge/login coverage is now much closer to Java, but full Java `SkillEngine` effect application after temporary skill add/remove is still pending.
- `CM_MANASTONE` still only handles the stigma charge-stone branch; Java manastone/enchant/godstone/amplification branches remain pending.
- Item-use timing still uses first-pass immediate handling in places where Java schedules 5s delayed tasks with movement/cancel observers, item cooldown abort cleanup, and stance/state denials.
- Charge and idian burn observers are still not wired into combat or skill execution: `ChargeInfo`, `PolishChargeCondition`, and `IdianStone.onEquip` attack/defend hooks remain future slices.
- NPC/dialog validation is still broad: known-list membership, NPC template function/action gates, distance/protection/audit checks, and generalized response-request state are incomplete.
- Real-client validation remains deferred until the end-of-port readiness pass.

---

## Suggested Next Units

1. Finish the remaining stigma/effect slice: apply/remove full Java `SkillEngine` effects for temporary stigma skill mutations and line that up with stats refresh and effect fanout.
2. Continue `CM_MANASTONE` beyond stigma charge stones: Java manastone socketing/removal, enchant, godstone, and amplification branches.
3. Tighten exact Java item-use scheduling: 5s delayed tasks, movement/cancel observers, item cooldown cleanup on abort, soul-bind stance/state denials, and reusable delayed item-use state.
4. Wire charge and idian burn triggers into future skill/combat observer paths: low-charge update packets, zero-charge deletion, stat refresh fanout, and attack/defend observer integration.
5. Continue known-list/NPC/dialog validation when the next unit should stay out of the stat engine.
