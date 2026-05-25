# Quest Repeat Date Audit

Date: May 25, 2026
Unit of Work: UOW-1009

## Purpose

This audit records Java quest completion repeat-date behavior that still blocks full nearby repeat-timing parity and future quest-finish mutation wiring.

Java remains the source of truth. This document is read-only and does not implement C# quest-finish behavior or claim runtime parity.

## Java Source Breadcrumbs

- `game-server/src/com/aionemu/gameserver/services/QuestService.java`
- `game-server/src/com/aionemu/gameserver/questEngine/model/QuestState.java`
- `game-server/src/com/aionemu/gameserver/model/templates/QuestTemplate.java`
- `game-server/src/com/aionemu/gameserver/model/templates/quest/QuestRepeatCycle.java`
- `game-server/src/com/aionemu/gameserver/utils/time/ServerTime.java`
- `game-server/src/com/aionemu/gameserver/dao/PlayerQuestListDAO.java`

## Java Behavior

`QuestService.finishQuest`:

- Requires the existing quest state to be `REWARD`.
- Calls `qs.setStatus(QuestStatus.COMPLETE)`.
- Calls `qs.setQuestVar(0)`.
- If `template.isTimeBased()` is true, calls `qs.setNextRepeatTime(calculateRepeatDate(player, template))`.
- Sends `SM_QUEST_ACTION(ActionType.UPDATE, qs)`.
- Calls `QuestEngine.onQuestCompleted(player, id)`.
- Calls `player.getNpcFactions().completeQuest(template)` for NPC faction quests.
- Calls `player.getController().updateNearbyQuests()`.

`QuestState.setStatus(QuestStatus.COMPLETE)`:

- When transitioning from a non-complete status and `updateCompleteCountAndTime` is true, sets `completeTime` to current `System.currentTimeMillis()` and increments `completeCount`.
- Marks the state `UPDATE_REQUIRED`.

`calculateRepeatDate`:

- Uses `ServerTime.now()`, which is `ZonedDateTime.now(GSConfig.TIME_ZONE_ID)`.
- Starts from today at server-time 09:00.
- If the current server time is after that 09:00 instant, advances the reset candidate by one day.
- If `template.isDaily()` is true, returns that candidate and sends `STR_MSG_QUEST_LIMIT_START_DAILY(9)`.
- Otherwise, treats the quest as weekly, sorts `template.getRepeatCycle()` by weekday, selects the first configured day whose value is greater than or equal to the candidate weekday, or wraps to the first configured day next week.
- Sends `STR_MSG_QUEST_LIMIT_START_WEEK(nextRepeatDay.getL10n(), 9)`.
- Stores the result as a SQL `Timestamp` created from `repeatDate.toEpochSecond() * 1000`.

`QuestRepeatCycle` weekday values:

| Token | Day Value | Name Id |
|---|---:|---:|
| `ALL` | 0 | 0 |
| `MON` | 1 | 900331 |
| `TUE` | 2 | 900332 |
| `WED` | 3 | 900333 |
| `THU` | 4 | 900334 |
| `FRI` | 5 | 900335 |
| `SAT` | 6 | 900336 |
| `SUN` | 7 | 900330 |

`QuestTemplate` repeat classification:

- `isTimeBased()` is true when `repeatCycle != null`.
- `isDaily()` is true when `repeatCycle` contains `ALL`.
- `isWeekly()` is true when time-based and not daily.

## Current C# State

- `NearbyQuestTemplateSummary` preserves `RepeatCycle` tokens and `IsTimeBased`.
- `PlayerQuestState` preserves nullable `NextRepeatTime` and `CompleteTime`.
- `MySqlPlayerEnterWorldRepository.LoadPlayerQuestsAsync` hydrates `next_repeat_time` and `complete_time`.
- `NearbyQuestStartConditionService` uses loaded `NextRepeatTime` for the already-completed nearby repeat check.
- UOW-1010 adds `QuestRepeatDateService.CalculateNextRepeatTime`, a pure calculator for Java server-time 09:00 daily/weekly reset selection. It is not wired to quest completion yet.
- UOW-1011 exposes the loaded `gameserver.timezone` value through `GameServerCoreOptions.GetTimeZone()` and adds a `QuestRepeatDateService` overload that consumes `GameServerOptions`.

## C# Gaps

- No C# quest-finish service calls the pure calculator or persists `next_repeat_time`.
- No C# packet bridge sends Java daily/weekly reset system messages.
- No C# quest completion path increments `CompleteCount`, sets `CompleteTime`, clears vars, sends `SM_QUEST_ACTION`, calls quest-completed handlers, updates NPC faction completion state, or triggers nearby quest refresh.
- Existing C# `DateTimeOffset` hydration needs server-timezone comparison before parity can be claimed.

## Recommended Implementation Slice

1. Wire `QuestRepeatDateService.CalculateNextRepeatTime` into a future quest-finish service using `GameServerOptions.Core.GetTimeZone()`.
2. Keep quest-finish mutation and packets out of scope until the pure calculator is integrated behind a staged quest-completion boundary.

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- `GSConfig.TIME_ZONE_ID` may differ from local machine timezone; C# now has an option resolver, but runtime integration still needs coverage.
- SQL `Timestamp` interpretation and MySQL connector timezone behavior need a dedicated DB/runtime check.
- Daily `ALL` plus other weekday token combinations should be treated exactly like Java `contains(ALL)` daily behavior.
