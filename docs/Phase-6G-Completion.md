# Phase 6G Completion Handoff

**Created**: May 21, 2026
**Status**: Phase 6 remains in progress; this is the next handoff after the 6F equipment-guard continuation.
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, persistence behavior, side effects, and naming.
**Workflow rule**: Do one focused unit of work, validate it, commit it, then repeat for as long as useful work remains.
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx --no-restore` passes with 353 tests.

---

## Recent Work Completed

- Ported a first-pass Java `CM_EQUIP_ITEM` opcode `38` path: parser/registration, equip/unequip/switch action routing, dual-wield gating from parsed `wpndual` metadata, slot collision mutation, inventory-full rejection for modeled cases, `inventory.is_equipped`/`slot` persistence, `SM_INVENTORY_UPDATE_ITEM`, `SM_STATS_INFO`, and `SM_UPDATE_PLAYER_APPEARANCE`.
- Extended static item metadata with Java `restrict`, `restrict_max`, `<uselimits gender>`, and rank min/max defaults.
- Added Java equip guards for invalid class, required level, max level, race, gender, AP rank, and missing required equip skills, preserving Java message/no-message behavior.
- Added Java-shaped system message helpers for equip guard failures, including `AbyssRankEnum.getRankL10n` / `ChatUtil.l10n` token generation for invalid-rank denial.
- Added Java `ItemGroup.getRequiredSkills` parity as an item-group skill map used by `Equipment.checkAvailableEquipSkills`.
- Kept `docs/PHASE-6-PROGRESS.md` current through Session 137.

---

## Important Limits

- `CM_EQUIP_ITEM` now covers the mutation path and Java class/level/max-level/race/gender/AP-rank/required-skill guards, but full StigmaService handling is still pending.
- Soul-bind confirmation is still pending and needs the Java response-request flow, 5s item-use animation, cancellation observers, persistence of `inventory.is_soul_bound`, and retry/stance system messages.
- Identified-item checks are still pending because item identified state is not modeled in the C# inventory item yet.
- Power-shard equip/unequip emotion side effects, quest/summon observers, exact speed/emotion fanout, and full Java `CreatureGameStats` lifecycle remain pending.
- Charge and idian burn observers are still not wired into combat or skill execution.
- `CM_DIALOG_SELECT` charge-all still lacks full NPC known-list lookup, NPC template action/function validation, distance/protection/audit checks, and generalized response-request state.

---

## Suggested Next Units

1. Continue `CM_EQUIP_ITEM` parity with a consciously scoped branch: either model identified item state and add the Java identified guard, or start soul-bind confirmation if response-request/item-use scheduling is the desired next foundation.
2. Port StigmaService equip/unequip behavior in slices: slot-open checks, stigma-shard/cost handling, linked skill add/remove, and Java stigma system messages.
3. Wire charge and idian burn triggers into future combat/skill observer paths: `ChargeInfo`, `PolishChargeCondition`, `IdianStone.onEquip` attack/defend observers, low-charge update packets, zero-charge deletion, and stat refresh fanout.
4. Choose the next passive skill/effect stat slice beyond mastery/title modifiers: generic passive `BufEffect` stat parsing, transform/title movement-speed serialization, or a fuller Java `Stat2` bridge.
5. Continue housing auction settlement/maintenance/sign/appearance flows, persistent known-list membership, or full NPC/dialog known-list/function validation when the next slice should stay out of equipment/stat internals.
