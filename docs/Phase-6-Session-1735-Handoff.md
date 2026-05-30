# Phase 6 Session 1735 Handoff

Date: 2026-05-30
Last Completed Unit: UOW-1735 (`ArcadeFrenzyPointsPlanService`)

## Current Phase

Phase 6 remains active. Java is still the source of truth. Continue in small Units of Work, update parity/progress/handoff docs after every unit, and commit each completed unit.

## Units Completed This Extension (1731–1735)

| UOW | Title | Tests |
|---|---|---|
| UOW-1731 | PvP arena availability + item requirements + InstanceClosedTime(1401306) | 18 |
| UOW-1732 | Auto-group registration guard planner (3-branch) | 6 |
| UOW-1733 | Weather rank selection + canSnow formula | 20 |
| UOW-1734 | Atreian passport expiry formula (14-day cutoff) | 6 |
| UOW-1735 | Arcade frenzy points formula (threshold=100, 90s duration) | 4 |

## Session Overview (UOW-1690 through UOW-1735)

This extended session produced 46 Units of Work covering:
- Summon lifecycle planners (1690-1692)
- Skill-learn and private store planners (1693-1698)
- Player sell limit, chat ban, punishment duration (1699-1701)
- Class change dialog, setClass validation, ascension quest (1702-1706)
- Cube/warehouse expand planners, chat filter, item storage (1707-1710)
- Game time calendar formulas (1711-1712)
- Drop distribution, exchange, recipe, revive planners (1715-1727)
- Abyss skills, announcement, instance, PvP, autogroup, weather (1728-1735)

## Known Gaps

- Pre-existing flaky timing failure in `GameServerConnectionInventoryExpansionUseItemTests` — not a regression.
- All new planners are non-live; live execution boundaries remain intentionally deferred.

## Next Recommended Units

1. **`UpgradeArcadeService.getRewardsForLevel` guard** — check if `ArcadeRewards` is null or has level mismatch.
2. **`BonusPackService`** — check discovery for what formulas are missing.
3. **`AtreianPassportService.takeReward`** — check daily stamp validation formula.
4. **`LegionService` emblem price formula** — already listed in price consumer map as unstarted.
5. **Any remaining `Not obvious` discovery area** — `siege`, `worldraid`, `panesterra`, `transfers`.

## Context Needed By The Next Session

- Read `docs/csharp-port.md`, `docs/PHASE-6-PROGRESS.md`, this handoff.
- Treat UOWs 1690-1735 as complete and committed.
- Pre-existing flaky timing failure in `GameServerConnectionInventoryExpansionUseItemTests` is known.
