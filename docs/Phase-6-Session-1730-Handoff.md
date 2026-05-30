# Phase 6 Session 1730 Handoff

Date: 2026-05-30
Last Completed Unit: UOW-1730 (`InstanceServiceFormulaService`)

## Current Phase

Phase 6 remains active. Java is still the source of truth. Continue in small Units of Work, update parity/progress/handoff docs after every unit, and commit each completed unit.

## Units Completed Since Session 1726 (1727–1730)

| UOW | Title | Tests |
|---|---|---|
| UOW-1727 | Chat flood detection + recipe add notification | 4 |
| UOW-1728 | Abyss skill lookup service (10 rank/race entries) | 7 |
| UOW-1729 | Abyss announcement planner + 2 message factories | 8 |
| UOW-1730 | Instance cooldown rate + destroy delay formulas | 6 |

## Known Gaps

- Pre-existing flaky timing failure in `GameServerConnectionInventoryExpansionUseItemTests` — not a regression.
- Live exchange, revive, instance, and craft execution remain deferred.

## Next Recommended Units

1. **`PeriodicInstanceManager`** — check for clean cooldown entry/removal formulas.
2. **`PvPArenaService`** — check for arena participant validation formulas.
3. **`AbyssRankUpdateService.selectAndUpdateQuotaRank`** — quota+GP threshold formula planner.
4. **Any new service area** from the `Not obvious` discovery list (ban, siege, antihack, etc.).

## Context Needed By The Next Session

- Read `docs/csharp-port.md`, `docs/PHASE-6-PROGRESS.md`, this handoff.
- Treat UOWs 1704-1730 as complete and committed.
- Pre-existing flaky timing failure in `GameServerConnectionInventoryExpansionUseItemTests` is known.
