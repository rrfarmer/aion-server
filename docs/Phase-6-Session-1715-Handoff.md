# Phase 6 Session 1715 Handoff

Date: 2026-05-30
Last Completed Unit: UOW-1715 (`DropBidNormalizationPlanService` + dice/bid message factories)

## Current Phase

Phase 6 remains active. Java is still the source of truth. Continue in small Units of Work, update parity/progress/handoff docs after every unit, and commit each completed unit.

## Units Completed This Session (1704–1715)

| UOW | Title | Tests |
|---|---|---|
| UOW-1704 | Class change show dialog planner | 15 focused |
| UOW-1705 | Ascension quest completion planner (`SmQuestAction.Add`/`Abandon` constants) | 7 focused |
| UOW-1706 | Class change `setClass` broadcast planner (`SmActionAnimation.ClassChange=4`) | 4 focused |
| UOW-1707 | Cube expand guard planner | 7 focused |
| UOW-1708 | Warehouse expand guard planner (quest IDs 1987/2985) | 7 focused |
| UOW-1709 | Chat message forbidden word filter service | 9 focused |
| UOW-1710 | Item storage restriction planner + 5 message factories | 11 focused |
| UOW-1711 | Game time day-calculation service (hour/minute/`calculateDayTime`) | 19 focused |
| UOW-1712 | Game time calendar year/month/day formulas | 27 focused |
| UOW-1713 | Item split early guard planner | 6 focused |
| UOW-1714 | NPC decay/respawn delay formula planner | 8 focused |
| UOW-1715 | Drop bid normalization + 9 dice/bid message factories | 7 focused |

## Validation State

- All focused test slices pass for every UOW.
- Pre-existing flaky timing failure in `GameServerConnectionInventoryExpansionUseItemTests` — not a regression.

## Next Recommended Unit of Work

Good candidates (all small, isolated formulas or planners):

1. **`DropDistributionService.handleRoll` notification planner** — select dice self/other message by `roll == 0` or `roll > 0`, use already-ported dice factories.

2. **`DropDistributionService.distributeLoot` winner selection formula** — `luckyPlayer > highestValue` comparison returns winner or null; pure formula.

3. **`DropRegistrationService` drop expiry formula** — `WITH_DROP_DECAY`-based expiry check.

4. **`RecipeService.autoLearnRecipes`** — iterate recipes and add those matching skill/level.

5. **`AnnouncementService` schedule calculation** — cron-based next fire time formula.

## Context Needed By The Next Session

- Read `docs/csharp-port.md`, `docs/PHASE-6-PROGRESS.md`, this handoff.
- Keep Java as source of truth. Treat UOWs 1704-1715 as complete and committed.
- Pre-existing flaky timing failure in `GameServerConnectionInventoryExpansionUseItemTests` is known.
- New areas with formulas to port: `DropDistributionService`, `CubeExpandService.expand` notification, `WarehouseService.expand` notification.
