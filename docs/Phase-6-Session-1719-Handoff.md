# Phase 6 Session 1719 Handoff

Date: 2026-05-30
Last Completed Unit: UOW-1719 (`WarehouseExpandNotificationPlanService`)

## Current Phase

Phase 6 remains active. Java is still the source of truth. Continue in small Units of Work, update parity/progress/handoff docs after every unit, and commit each completed unit.

## Units Completed This Extended Session (1704–1719)

| UOW | Title | Tests |
|---|---|---|
| UOW-1704 | Class change show dialog planner | 15 |
| UOW-1705 | Ascension quest completion planner | 7 |
| UOW-1706 | Class change setClass broadcast planner | 4 |
| UOW-1707 | Cube expand guard planner | 7 |
| UOW-1708 | Warehouse expand guard planner | 7 |
| UOW-1709 | Chat message forbidden word filter | 9 |
| UOW-1710 | Item storage restriction planner | 11 |
| UOW-1711 | Game time day-calculation service | 19 |
| UOW-1712 | Game time calendar year/month/day | 27 |
| UOW-1713 | Item split early guard planner | 6 |
| UOW-1714 | NPC decay/respawn delay planner | 8 |
| UOW-1715 | Drop bid normalization + 9 message factories | 7 |
| UOW-1716 | Drop roll notification planner | 4 |
| UOW-1717 | Drop loot winner selection planner | 4 |
| UOW-1718 | Cube expand notification planner | 5 |
| UOW-1719 | Warehouse expand notification planner | 4 |

## Known Gaps

- Pre-existing flaky timing failure in `GameServerConnectionInventoryExpansionUseItemTests` — not a regression.
- `DropRegistrationService` drop lifecycle and `DropService.canDistribute` remain live/unported.
- Cube/warehouse live expand execution (counter increments, size refresh) remain deferred.
- `DropDistributionService.handleRollOrBid` dispatch and `SM_GROUP_LOOT` packet remain unported.

## Next Recommended Units

1. **`SM_GROUP_LOOT` packet** — `DropDistributionService.handleRoll` and `handleBid` send this to all in-range players; add opcode/payload, connect to roll/bid plans.

2. **`RecipeService.autoLearnRecipes`** — iterate auto-learn recipes for skill/level; pure loop over static data.

3. **`PlayerReviveService`** — check if simple formula boundaries exist.

4. **`CubeExpandService.expandCube` NPC dialog guard** — min/max expansion level validation with NPC template system messages.

## Context Needed By The Next Session

- Read `docs/csharp-port.md`, `docs/PHASE-6-PROGRESS.md`, this handoff.
- Keep Java as source of truth. Treat UOWs 1704-1719 as complete and committed.
- Pre-existing flaky timing failure in `GameServerConnectionInventoryExpansionUseItemTests` is known.
