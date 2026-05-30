# Phase 6 Session 1703 Handoff

Date: 2026-05-30
Last Completed Unit: UOW-1703 (`ClassChangeSetClassValidationPlanService`)

## Current Phase

Phase 6 remains active. Java is still the source of truth. Continue in small Units of Work, update parity/progress/handoff docs after every unit, and commit each completed unit.

## Units Completed This Session (Both Sessions Combined)

| UOW | Title | Key Artifacts | Tests |
|---|---|---|---|
| UOW-1690 | Summon mode change planner | `SummonModeChangePlanService`, 4 `SmSystemMessage` factories | 257 focused |
| UOW-1691 | Summon doMode dispatch planner | `SummonDoModePlanService` | 32 focused |
| UOW-1692 | Summon createSummon packet planner | `SummonCreatePlanService`, `SmEmotion` creature constructor | 38 focused |
| UOW-1693 | Skill-learn notification planner | `SkillLearnNotificationPlanService`, `SmActionAnimation.CraftLevelUp=4` | 22 focused |
| UOW-1694 | Private store open guard planner | `PrivateStoreOpenGuardPlanService`, 6 `SmSystemMessage` factories | 12 focused |
| UOW-1695 | Private store item validation planner | `PrivateStoreItemValidationPlanService`, 10 `SmSystemMessage` factories | 27 focused |
| UOW-1696 | Private store open + `SM_PRIVATE_STORE_NAME` | `SmPrivateStoreName` (opcode 145), `PrivateStoreOpenPlanService` | 5 focused |
| UOW-1697 | Private store close planner | `PrivateStoreClosePlanService` | 4 focused |
| UOW-1698 | Private store sell notification planner | `PrivateStoreSellNotificationPlanService`, 2 factories | 8 focused |
| UOW-1699 | Player sell limit formula planner | `PlayerSellLimitPlanService`, `DayCannotSellNpc` factory | 11 focused |
| UOW-1700 | Chat ban remaining minutes planner | `ChatBanRemainingMinutesPlanService`, `CanChatNow` factory | 7 focused |
| UOW-1701 | Punishment duration formula planner | `PunishmentDurationPlanService`, `CaptchaRecovered` factory | 9 focused |
| UOW-1702 | Class change dialog lookup planner | `ClassChangeDialogPlanService` (12+22 lookup entries) | 38 focused |
| UOW-1703 | Class change setClass validation planner | `ClassChangeSetClassValidationPlanService` (17 class IDs) | 23 focused |

## Validation State

- All focused test slices pass for each UOW.
- Full suite pre-existing flaky timing failure in `GameServerConnectionInventoryExpansionUseItemTests` — not a regression.

## Known Gaps

- Live summon lifecycle (spawn, delete, cooldown, `doMode` dispatch, mode side effects) remains deferred.
- Private store `createStoreWithItems`, `sellStoreItem` full live flow, and `getBoughtItems` remain non-live/unported.
- `ClassChangeService.showClassChangeDialog`, `changeClassToSelection`, `completeAscensionQuest`, and live `setClass` execution remain deferred.
- `SkillLearnService.onLearnSkill` passive-effect application and recipe auto-learn remain deferred.
- `PlayerLimitService` live map write (sell limit deduction) remains deferred.
- `ChatBanService` live task scheduling and ChatServer gag packet remain deferred.
- `PunishmentService` live prison/ban execution remains deferred.

## Next Recommended Unit of Work

Good candidates (all are small isolated planners or formulas):

1. **`ClassChangeService.showClassChangeDialog` planner** (UOW-1704) — level >= 9 + isStartingClass guard, `SM_DIALOG_WINDOW` page ID from already-ported lookup, ascension quest ID by race (1006=Elyos, 2008=Asmodians).

2. **`ClassChangeService.completeAscensionQuest` planner** — quest state add-or-update intent without live state mutation; `SM_QUEST_ACTION(ADD)` and `SM_QUEST_ACTION(UPDATE)` packet intents.

3. **`UpgradeArcadeService`** — check if it has small formula-based methods worth porting.

4. **`LimitedItemTradeService`** — check if C# has a gap in buy-count persistence.

5. **Continue private store area** — `sellStoreItem` pre-validation (`getBoughtItems` logic: inventory-full check, price total, `DEC_KINAH` intent).

## Files Changed This Session

(See session 1696 handoff for UOW-1690-1696; this session added UOW-1697-1703)

New files this session:
- `dotnetConversion/src/Aion.GameServer/Services/ClassChangeDialogPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/ClassChangeSetClassValidationPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/ChatBanRemainingMinutesPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerSellLimitPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PrivateStoreClosePlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PrivateStoreSellNotificationPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PunishmentDurationPlanService.cs`
- Plus corresponding test files
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs` (multiple new factories)
- `docs/PHASE-6-PROGRESS.md`

## Context Needed By The Next Session

- Read `docs/csharp-port.md`, `docs/PHASE-6-PROGRESS.md`, this handoff.
- Keep Java as source of truth.
- Treat UOWs 1690-1703 as complete and committed.
- Pre-existing flaky timing failure in `GameServerConnectionInventoryExpansionUseItemTests` is documented as known.
- `ClassChangeService` still has `showClassChangeDialog`, `changeClassToSelection`, `completeAscensionQuest`, and live `setClass` execution unported.
- `PrivateStoreService` still has `createStoreWithItems`, `sellStoreItem`, and `getBoughtItems` unported.
