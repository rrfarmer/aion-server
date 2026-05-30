# Phase 6 Session 1749 Handoff

Date: 2026-05-30
Last Completed Unit: UOW-1749 (`CombatMiscFormulaService`)

## Current Phase

Phase 6 remains active. Java is still the source of truth. Continue in small Units of Work.

## Units Completed Since Session 1743 (1744–1749)

| UOW | Title | Tests |
|---|---|---|
| UOW-1744 | XP reward level-difference formula (16-entry table) | 20 |
| UOW-1745 | NPC base EXP formula (rating/rank multipliers) | 9 |
| UOW-1746 | NPC DP/AP reward rating formulas (5 ratings each) | 15 |
| UOW-1747 | PvP XP and DP reward formulas (level/rank penalties) | 8 |
| UOW-1748 | Hate boost and PvP AP gain formulas | 5 |
| UOW-1749 | AP lost and fall damage formulas | 6 |

## Session Overview (UOW-1690 through UOW-1749)

This extremely extended session produced **60 Units of Work** covering:
- Summon lifecycle, class change, private store, exchange planners
- Weather, abyss, instance, PvP, autogroup service formulas  
- Passport, arcade, reward pack planners
- Drop/loot distribution formulas
- StatFunctions: XP rewards, base exp, DP/AP ratings, PvP formulas, hate, fall damage

Total committed UOWs in this session: 1690-1749 (60 units)

## Known Gaps

- Pre-existing flaky timing failure in `GameServerConnectionInventoryExpansionUseItemTests`.
- All planners remain non-live; live execution boundaries are deferred.

## Next Recommended Units

1. **`StatFunctions.calculateMagicalSkillDamage`** — pure float formula for magical damage calculation.
2. **`StatFunctions.checkIsDodgedHit/checkIsParriedHit/checkIsBlockedHit`** — hit check formulas.
3. **`StatFunctions.checkIsPhysicalCriticalHit`** — physical crit check formula.
4. **`StatFunctions.adjustStatByMovementModifier`** — movement direction stat adjustments.
5. **Any remaining `Not obvious` discovery gap** — siege, worldraid, transfers.

## Context Needed By The Next Session

- Read `docs/csharp-port.md`, `docs/PHASE-6-PROGRESS.md`, this handoff.
- Treat UOWs 1690-1749 as complete and committed.
- Pre-existing flaky timing failure in `GameServerConnectionInventoryExpansionUseItemTests` is known.
