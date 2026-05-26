# Quest Bonus Handler Exception And Failure Ordering Audit

Date: May 26, 2026

## Scope

This audit records Java behavior for `QuestEngine.onBonusApplyEvent` before C# models any dynamic quest-handler execution.

No C# production code or live handler dispatch is changed by this unit.

Java source reviewed:

- `com.aionemu.gameserver.services.QuestService.getRewardItems`
- `com.aionemu.gameserver.questEngine.QuestEngine.onBonusApplyEvent`
- `com.aionemu.gameserver.questEngine.handlers.AbstractQuestHandler.onBonusApplyEvent`
- `com.aionemu.gameserver.questEngine.handlers.HandlerResult`
- Representative event handlers under `game-server/data/handlers/quest/event_quests`

## Java Ordering

`QuestService.getRewardItems` calls `QuestEngine.getInstance().onBonusApplyEvent(env, template.getBonus().getType(), questItems)` after static/selected reward items are gathered.

Only `HandlerResult.FAILED` suppresses the later `BonusService.getQuestBonus(player, template)` call. `SUCCESS` and `UNKNOWN` both allow `BonusService`.

`QuestEngine.onBonusApplyEvent` looks up `questOnBonusApply.get(bonusType)` and iterates the registered quest ids. For the first non-null handler, it sets `env.questId` to that registered id and immediately returns `questHandler.onBonusApplyEvent(...)`.

Important consequence: Java does not continue to later registered handlers when the first loaded handler returns `UNKNOWN`. First loaded handler wins for the bonus type.

If there are no registered quest ids or no loaded handlers for the bonus type, Java returns `HandlerResult.UNKNOWN`, which allows `BonusService`.

## Exception Behavior

`QuestEngine.onBonusApplyEvent` wraps the entire lookup and handler call in a single `try/catch`.

If any exception escapes the handler call or the lookup loop, Java logs `QE: exception in onBonusApply` and returns `HandlerResult.FAILED`.

Because `QuestService.getRewardItems` only calls `BonusService` when the result is not `FAILED`, an exception suppresses `BonusService`.

## Representative Handler Behavior

`_80016EventSockHop` and `_80018EventSockItToEm` register `BonusType.MOVIE`. They:

- return `UNKNOWN` when the bonus type or handler quest id does not match;
- fetch the player quest state for their quest id;
- when status is `REWARD`, optionally add a direct hat-box item at complete count 9, play a random movie, and return `SUCCESS`;
- otherwise return `FAILED`, suppressing `BonusService`.

LUNAR handlers such as `_80034EventGeaterGlories` and RIFT handlers such as `_80137EventSealTheWarpedRift`:

- return `UNKNOWN` on mismatched bonus type or quest id;
- return `SUCCESS` when their quest state/status/var gate passes;
- return `FAILED` otherwise, suppressing `BonusService`.

The default `AbstractQuestHandler.onBonusApplyEvent` returns `UNKNOWN`.

## C# Parity Implications

Current C# `QuestBonusHandlerOutcomePlanService` already models first-loaded handler ordering, explicit loaded handler ids, handler states, `FAILED` suppression, and `UNKNOWN` allowing BonusService-style planning.

Missing behavior before live dynamic handler execution:

- dynamic Java handler registry/load order from scripts;
- exact first-loaded ordering sourced from runtime handler registration;
- exception-to-`FAILED` conversion for actual handler execution;
- direct mutation side effects such as `rewardItems.add(...)` and `playQuestMovie(...)`;
- Java random movie selection;
- Java runtime comparison for handler side effects and suppression.

## Recommended Next Boundary

Before executing dynamic handlers, add a non-live exception/failure policy descriptor or extend the existing handler outcome planner tests to assert:

- no registrations return `UNKNOWN` and allow BonusService;
- first loaded handler returning `UNKNOWN` still stops later handlers and allows BonusService;
- first loaded handler returning `FAILED` suppresses BonusService;
- thrown handler exceptions are represented as `FAILED` and suppress BonusService.

Keep production handler execution disabled until the C# handler registry can provide loaded handler order and exception behavior explicitly.

## Remaining Risks

- Java script load order and reload behavior are not runtime-compared.
- C# does not execute dynamic Java/C# quest handlers for bonus events.
- Handler exceptions are source-reviewed but not tested against a real dynamic handler execution boundary.
- Movie side effects, direct reward item mutation, Java RNG, packet sends, persistence, rollback, threading/player-ordering, serialization, and Java runtime comparison remain disabled.
