# Phase 6F Completion Handoff

**Created**: May 21, 2026
**Status**: Phase 6 remains in progress; this is the next handoff after the 6E gameplay/stat continuation.
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, persistence behavior, side effects, and naming.
**Workflow rule**: Do one focused unit of work, validate it, commit it, then repeat for as long as useful work remains.
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 352 tests.

---

## Recent Work Completed

- Ported Java `CM_CHARGE_ITEM` opcode `78` for selected cube-item conditioning, including static `<improve>`/rank metadata, Java `ItemChargeService` price math, kinah/AP mutation, charge persistence, charge update blobs, success/all-complete messages, and `SM_STATS_INFO` refresh.
- Ported Java idian polish item-use branch from `CM_USE_ITEM` opcode `37`: static `<idian>`/`<polish>` parsing, weighted POLISH random-bonus selection, source idian consumption, target idian `item_stones` replacement, inventory update packets, polish messages, item-use completion, and a reusable polish-charge decrease helper.
- Ported Java charge consumable item-use flow from `<actions><charge capacity="..."/>`, consuming the source item and conditioning matching equipped items with persistent charge updates.
- Added Java `CM_DIALOG_SELECT` opcode `54` and the narrow charge-all dialog actions `CHARGE_ITEM_MULTI` / `CHARGE_ITEM_MULTI2`, including confirmation windows, pending response state, single payment mutation, equipped-item charge updates, and stat refresh.
- Parsed Java armor mastery skill effects and applied a limited `ArmorMasteryEffect` / `StatArmorMasteryFunction` bridge to `SM_STATS_INFO`.
- Parsed Java `player_titles.xml` into `TitleTemplateTable` and applied Java bonus-title stat modifiers through `SM_STATS_INFO`.
- Parsed Java `wpnmastery` and `shieldmastery` skill effects, then applied a limited `WeaponMasteryEffect` / `StatWeaponMasteryFunction` and `ShieldMasteryEffect` / `StatShieldMasteryFunction` bridge to current stats.
- Parsed Java `wpndual` effect metadata into `SkillTemplateSummary`, preserving the fields needed by future `WeaponDualEffect.hasDualWieldEffect` equip gates and attack stat behavior.
- Ported a first-pass Java `CM_EQUIP_ITEM` opcode `38` path: parser/registration, equip/unequip/switch action routing, `WeaponDualEffect.hasDualWieldEffect` gate from parsed `wpndual` metadata, slot collision mutation, full-inventory rejection for the modeled cases, `inventory.is_equipped`/`slot` persistence, `SM_INVENTORY_UPDATE_ITEM`, `SM_STATS_INFO`, and `SM_UPDATE_PLAYER_APPEARANCE`.
- Extended the `CM_EQUIP_ITEM` path with parsed Java `restrict`, `restrict_max`, `<uselimits gender>`, and rank metadata plus class, required-level, max-level, race, and gender guards mapped to Java `SM_SYSTEM_MESSAGE` IDs.
- Extended the `CM_EQUIP_ITEM` path with Java `ItemUseLimits.verifyRank` AP-rank rejection and Java `AbyssRankEnum.getRankL10n`/`ChatUtil.l10n` token generation for `STR_CANNOT_USE_ITEM_INVALID_RANK`.
- Kept `docs/PHASE-6-PROGRESS.md` current through Session 135.

---

## Important Limits

- The stat work is still a packet-facing bridge, not the full Java `CreatureGameStats` / `Stat2` container.
- Most new stat behavior affects the current-stat half of `SM_STATS_INFO`; full Java base/current ordering and owner-scoped stat lifetimes remain pending.
- Exact Java item-use delay/cancel observers, cooldown plumbing, identify/attack-mode guards, and combat/skill burn trigger integration remain pending.
- Charge and idian burn observers are not wired into combat or skill execution yet.
- `CM_EQUIP_ITEM` now covers the slot mutation path and Java class/level/max-level/race/gender/AP-rank guards, but required equip skills, stigma handling, soul-bind confirmation, identified-item checks, power-shard emotion side effects, quest/summon observers, exact speed/emotion fanout, and full Java stat lifecycle remain pending.
- `CM_DIALOG_SELECT` charge-all still lacks full NPC known-list lookup, NPC template action/function validation, distance/protection/audit checks, and generalized response-request state.
- Bonus title stat application is present on stat packet creation, but title learn/remove expiration side effects and dynamic stat recompute fanout remain pending.

---

## Suggested Next Units

1. Finish full `CM_EQUIP_ITEM` parity beyond the current mutation/basic-guard path: required equip skills, stigma handling, soul-bind confirmation, identify checks, power-shard emotion side effects, quest/summon observers, and exact speed/emotion fanout.
2. Wire charge and idian burn triggers into future combat/skill observer paths: `ChargeInfo`, `PolishChargeCondition`, `IdianStone.onEquip` attack/defend observers, low-charge update packets, zero-charge deletion, and stat refresh fanout.
3. Choose the next passive skill/effect stat slice beyond mastery/title modifiers: generic passive `BufEffect` stat parsing, transform/title movement-speed serialization, or a fuller Java `Stat2` bridge.
4. Continue housing auction settlement/maintenance/sign/appearance flows or persistent known-list membership when the next slice should stay out of the stat engine.
5. Fill in full NPC/dialog known-list/function validation around the parsed `CM_DIALOG_SELECT` surface.
