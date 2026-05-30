# Phase 6 Session 1726 Handoff

Date: 2026-05-30
Last Completed Unit: UOW-1726 (`PlayerReviveNotificationPlanService`)

## Current Phase

Phase 6 remains active. Java is still the source of truth. Continue in small Units of Work, update parity/progress/handoff docs after every unit, and commit each completed unit.

## Units Completed This Extension (1720–1726)

| UOW | Title | Tests |
|---|---|---|
| UOW-1720 | SM_GROUP_LOOT packet (opcode 135) | 3 |
| UOW-1721 | Recipe learn validation planner (6-branch) | 10 |
| UOW-1722 | SM_EXCHANGE_ADD_KINAH + add-kinah formula planner | 7 |
| UOW-1723 | SM_EXCHANGE_ADD_ITEM packet (opcode 75) | 2 |
| UOW-1724 | Exchange trade validation planner | 4 |
| UOW-1725 | Player revive stats restoration planner | 5 |
| UOW-1726 | Player revive notification planner + RebirthMassageMe(1300738) | 5 |

## Known Gaps

- Pre-existing flaky timing failure in `GameServerConnectionInventoryExpansionUseItemTests` — not a regression.
- Exchange item add flow (`ExchangeService.addItem`), lock, cancel, confirm, and perform-trade remain live/unported.
- `PlayerReviveService` resurrection skill, soul sickness, and position teleport effects remain live/deferred.

## Next Recommended Units

1. **`PlayerReviveService.itemSelfRevive` item-use formula** — cooldown calculation (`useLimits.getDelayTime()`), `SM_ITEM_USAGE_ANIMATION` broadcast intent.

2. **`PlayerChatService`** — check what pure formulas it has (rate limiting, channel validation).

3. **`RecipeService.addRecipe`** — `validateNewRecipe` result dispatch plus `CraftRecipeLearn(recipeId, playerName)` message already ported.

4. **`PlayerLeaveWorldService`** — already largely ported in C#; verify against Java source.

## Context Needed By The Next Session

- Read `docs/csharp-port.md`, `docs/PHASE-6-PROGRESS.md`, this handoff.
- Treat UOWs 1704-1726 as complete and committed.
- Pre-existing flaky timing failure in `GameServerConnectionInventoryExpansionUseItemTests` is known.
