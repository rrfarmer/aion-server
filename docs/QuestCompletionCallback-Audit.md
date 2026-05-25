# Quest Completion Callback Audit

Date: May 25, 2026
Unit of Work: UOW-1020; updated by UOW-1025, UOW-1026, UOW-1027, UOW-1028, and UOW-1029

## Purpose

This audit records Java `QuestEngine.onQuestCompleted` behavior before a C# callback dispatcher or live quest-finish execution is introduced.

Java remains the source of truth. This unit is documentation-only and does not implement callback dispatch, handler loading, packet sends, quest-state mutation, persistence, or nearby-refresh behavior.

## Java Source Breadcrumbs

- `game-server/src/com/aionemu/gameserver/questEngine/QuestEngine.java`
- `game-server/src/com/aionemu/gameserver/questEngine/handlers/QuestHandlerLoader.java`
- `game-server/src/com/aionemu/gameserver/questEngine/handlers/AbstractQuestHandler.java`
- `game-server/src/com/aionemu/gameserver/questEngine/model/QuestEnv.java`
- `game-server/src/com/aionemu/gameserver/services/QuestService.java`
- Representative handlers:
  - `game-server/data/handlers/quest/poeta/_1001TheKerubThreat.java`
  - `game-server/data/handlers/quest/poeta/_1002RequestoftheElim.java`
  - `game-server/data/handlers/quest/verteron/_14015NotBlindedByVengeance.java`

## Dispatch Ordering

`QuestService.finishQuest` calls `QuestEngine.getInstance().onQuestCompleted(player, id)` after it sends `SM_QUEST_ACTION(ActionType.UPDATE, qs)` for the completed quest and before NPC faction completion and nearby quest refresh.

`QuestEngine.onQuestCompleted`:

1. Builds `new QuestEnv(null, player, questId)`.
2. Iterates the shared `questOnCompleted` `ArrayList<Integer>`.
3. Looks up each registered quest id in `questHandlers`.
4. Calls `questHandler.onQuestCompletedEvent(env)` when a handler exists.
5. Catches any exception around the whole loop, logs `QE: exception in onQuestCompleted`, and then exits the method.

Important parity detail: the same `QuestEnv` instance is passed to every handler. The completed quest id remains `env.getQuestId()` unless a handler mutates it.

## Registration Shape

Handlers are loaded by `QuestHandlerLoader.postLoad` through Java reflection:

- Invalid classes are skipped when they are abstract, interfaces, or not public.
- Valid `AbstractQuestHandler` classes are instantiated through the no-arg constructor.
- `QuestEngine.addQuestHandler` stores each handler in a `HashMap<Integer, AbstractQuestHandler>` by handler quest id.
- Duplicate handler quest ids log a warning and do not call `register()` on the duplicate.
- `questHandler.register()` is called only for the first handler for a quest id.

`registerOnQuestCompleted(int questId)` adds the quest id to `questOnCompleted` only if it is not already present. The list therefore preserves first registration order, but the exact order depends on script class loading order, which the C# port does not currently model.

Repository search found 103 `registerOnQuestCompleted` call sites in Java quest handlers and 104 `onQuestCompletedEvent` declarations across handler/default surfaces at audit time.

## Default Callback Side Effects

`AbstractQuestHandler.onQuestCompletedEvent` is empty by default.

Many campaign handlers override it by calling `defaultOnQuestCompletedEvent(env, preQuests...)`.

`defaultOnQuestCompletedEvent` can:

- Inspect the completed quest id from `env.getQuestId()`.
- Inspect and mutate the target handler quest state, not just the quest that was completed.
- Call `QuestService.checkStartConditions`.
- Add or update a follow-up quest as `LOCKED` or `START`.
- Use `QuestService.addOrUpdateQuest`, which sends `SM_QUEST_ACTION(ActionType.ADD or UPDATE, qs)`.

The default callback does not directly call `PlayerController.updateNearbyQuests`. The enclosing `finishQuest` call still performs one nearby refresh after all completion callbacks and optional NPC faction completion.

## Custom Callback Risks

Handlers can override `onQuestCompletedEvent` with arbitrary Java code. Representative scanned handlers mostly call `defaultOnQuestCompletedEvent`, but the callback surface is not restricted:

- It can mutate quest state through inherited helper methods or direct `QuestState` operations.
- It can send packets directly through helper methods or `PacketSendUtility`.
- It can inspect inventory, level, race, XML conditions, or existing quest states.
- It can depend on dynamic handler load order.

Because `QuestEngine.onQuestCompleted` catches exceptions around the whole loop, a thrown exception stops callbacks after the failing handler. C# should model this deliberately if a dispatcher is introduced; continuing after a failing callback would be an intentional behavior difference.

## Current C# State

- `QuestFinishOperationPlanService` has a `QuestCompletedCallback` descriptor in Java order after the quest update packet descriptor and before NPC faction completion.
- UOW-1025 adds `QuestCompletionCallbackPlanService`, a pure non-live planner for registered completion handlers, shared-env metadata, duplicate registration normalization, missing handler skips, and exception-stop behavior.
- UOW-1026 composes optional `QuestCompletionCallbackPlan` descriptors into `QuestFinishOperationPlanService` after the update packet and before NPC faction completion.
- UOW-1027 adds `QuestCompletionFollowUpPlanService`, a pure non-live planner for default callback follow-up `LOCKED`/`START` outcomes and `SM_QUEST_ACTION` `ADD`/`UPDATE` packet-action intent.
- UOW-1028 composes optional `QuestCompletionFollowUpPlan` payloads into `QuestCompletionCallbackDescriptor`.
- UOW-1029 adds operation-plan regression coverage proving nested follow-up payloads survive through `QuestFinishOperationPlanService` callback composition.
- C# has no runtime quest handler registry, no dynamic Java-style handler loading, and no `QuestEnv` callback object.
- C# has staged quest-state mutation and operation-plan descriptors, but no live callback dispatch or follow-up quest start/lock mutation.
- C# packet serializers include `SmQuestAction.Update`, but callback-triggered `ADD` or callback-specific packets are not wired.

## Recommended Implementation Slices

1. Keep callback execution disabled until quest handler registration, static-data preconditions, and packet side effects have tested C# homes.
2. If a live dispatcher is later introduced, decide explicitly whether to match Java's whole-loop exception catch semantics.

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- Handler registration order is dynamic and depends on script loading/reflection behavior.
- Callback methods can mutate quest state or send packets beyond the default helper behavior.
- Java's exception handling stops the remaining callback loop after the first thrown exception.
- UOW-1026 carries detailed callback descriptors in quest-finish planning when supplied, but C# still has no callback registry, callback result model, mutable `QuestEnv`, or live execution surface.
- UOW-1027 models follow-up result actions but does not execute Java start-condition checks, XML condition recursion, quest-state mutation, or packet sends.
- UOW-1028 carries follow-up result plans through callback descriptors, but no live callback execution or packet serialization exists.
- UOW-1029 verifies nested follow-up payloads through quest-finish planning, but still does not serialize or send callback follow-up packets.
