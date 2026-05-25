# Quest Finish Ordering Audit

Date: May 25, 2026
Unit of Work: UOW-1014

## Purpose

This audit records Java quest completion packet, callback, nearby-refresh, and persistence ordering for future C# quest-finish wiring.

Java remains the source of truth. This unit is documentation-only and does not enable production quest completion, packet sends, DAO writes, NPC faction writes, or ItemPurification dispatch.

## Java Source Breadcrumbs

- `game-server/src/com/aionemu/gameserver/services/QuestService.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_QUEST_ACTION.java`
- `game-server/src/com/aionemu/gameserver/questEngine/QuestEngine.java`
- `game-server/src/com/aionemu/gameserver/dao/PlayerQuestListDAO.java`
- `game-server/src/com/aionemu/gameserver/dao/PlayerNpcFactionsDAO.java`
- `game-server/src/com/aionemu/gameserver/services/player/PlayerService.java`

## Java Runtime Ordering

`QuestService.finishQuest` performs the synchronous runtime work in this order:

1. Reject missing quest state or non-`REWARD` state.
2. Load the `QuestTemplate`.
3. Reject repeat mission completion.
4. Validate/fix reward group.
5. Compute reward items and extended rewards.
6. Add item rewards and give configured rewards.
7. Notify challenge-task service when applicable.
8. Remove quest work items.
9. Mutate quest state:
   - `qs.setStatus(QuestStatus.COMPLETE)`
   - `qs.setQuestVar(0)`
   - `qs.setNextRepeatTime(calculateRepeatDate(...))` only when `template.isTimeBased()`
10. Send `SM_QUEST_ACTION(ActionType.UPDATE, qs)`.
11. Call `QuestEngine.getInstance().onQuestCompleted(player, id)`.
12. If `template.getNpcFactionId() != 0`, call `player.getNpcFactions().completeQuest(template)`.
13. Call `player.getController().updateNearbyQuests()`.
14. Return `true`.

## Packet Notes

`SM_QUEST_ACTION(ActionType.UPDATE, qs)` serializes:

- action id `2`
- quest id
- quest status value
- zero padding byte
- `questVars | flags << 24`
- trailing zero short

`SM_QUEST_ACTION.writeImpl` silently returns without writing a packet body when the quest template exists and `extraCategory != NONE`.

Daily/weekly repeat reset system messages are sent inside `QuestService.calculateRepeatDate`, before `SM_QUEST_ACTION(ActionType.UPDATE, qs)` is sent by `finishQuest`.

## Callback Notes

`QuestEngine.onQuestCompleted` builds a new `QuestEnv(null, player, questId)` and iterates `questOnCompleted`, calling each registered handler's `onQuestCompletedEvent`.

The callback runs after the completion update packet has already been sent, but before NPC faction completion and nearby quest refresh.

## Persistence Notes

`QuestService.finishQuest` does not call `PlayerQuestListDAO.store` or `PlayerNpcFactionsDAO.storeNpcFactions`.

Persistence is deferred to `PlayerService.storePlayer`, whose relevant order is:

1. `PlayerQuestListDAO.store(player)`
2. inventory and other player systems
3. `PlayerNpcFactionsDAO.storeNpcFactions(player)`

`PlayerQuestListDAO.store`:

- Opens one connection with `autoCommit(false)`.
- Deletes removed quests first.
- Inserts new quests second.
- Updates changed quests third.
- Each delete/insert/update helper commits independently when it executes.
- After the try/catch, marks every current quest state `PersistentState.UPDATED`.

`PlayerQuestListDAO.UPDATE_QUERY` persists:

- `status`
- `quest_vars`
- `flags`
- `complete_count`
- `next_repeat_time`
- `reward`
- `complete_time`

`PlayerNpcFactionsDAO.storeNpcFactions` iterates factions and only writes:

- `PersistentState.NEW` through insert
- `PersistentState.UPDATE_REQUIRED` through update

The NPC faction update persists:

- `active`
- `time`
- `state`
- `quest_id`

## Current C# State

- UOW-1012 stages quest state completion in `QuestFinishStateMutationService`, including status, quest vars, complete count/time, and next repeat time.
- UOW-1013 stages NPC faction completion in `PlayerNpcFactionsSnapshot.CompleteActiveQuest`.
- UOW-1015 adds `QuestFinishOperationPlanService`, a non-live operation plan that composes staged quest-state mutation and optional NPC faction completion with Java-ordered descriptors for future packet, callback, nearby-refresh, and persistence work.
- UOW-1016 adds `SmQuestAction.Update`, a non-sending C# serializer for the Java `SM_QUEST_ACTION(ActionType.UPDATE, qs)` payload body plus explicit extra-category suppression.
- UOW-1017 adds `docs/QuestFinishRewardWorkItem-Audit.md`, documenting Java reward-group correction, item reward selection, non-item rewards, challenge-task notification, and work-item removal before quest-state mutation.
- UOW-1018 adds `QuestFinishRewardPlanService`, a non-live reward/work-item descriptor planner with Java-shaped reward-group correction and work-item removal metadata.
- UOW-1019 composes `QuestFinishRewardPlanService` into `QuestFinishOperationPlanService` through an optional reward projection, keeping detailed reward/work-item descriptors before staged quest-state mutation.
- UOW-1020 adds `docs/QuestCompletionCallback-Audit.md`, documenting Java completion callback registration, dynamic handler loading, dispatch ordering, and follow-up quest side effects.
- UOW-1021 adds `docs/QuestPersistenceContract-Audit.md`, documenting logout/store ordering, quest DAO commit behavior, NPC-faction write behavior, and Java persistent-state gaps.
- UOW-1022 adds `QuestPersistencePlanService`, a non-live quest persistence operation planner for Java delete/insert/update DAO phases.
- UOW-1023 adds `NpcFactionPersistencePlanService`, a non-live NPC-faction persistence operation planner for Java insert/update DAO filters.
- UOW-1024 composes optional quest/NPC-faction persistence plans into `QuestFinishOperationPlanService` as detailed non-live descriptors after nearby refresh.
- UOW-1025 adds `QuestCompletionCallbackPlanService`, a non-live planner for Java completion callback registration order, shared env metadata, and exception-stop behavior.
- UOW-1026 composes optional callback dispatch plans into `QuestFinishOperationPlanService` as detailed non-live descriptors after the quest update packet and before NPC-faction completion.
- UOW-1027 adds `QuestCompletionFollowUpPlanService`, a non-live planner for default callback follow-up `LOCKED`/`START` quest result and callback-triggered quest-action packet intent.
- UOW-1028 composes optional follow-up result plans into callback descriptors.
- UOW-1029 adds regression coverage proving nested follow-up result payloads survive through quest-finish callback composition.
- UOW-1030 adds `QuestFinishRewardPlanService.CreateRewardItemProjection`, a non-live projection scaffold for Java `QuestService.getRewardItems` fixed/selectable/class/extended item choices. It is not yet composed into production quest finish or live inventory mutation.
- UOW-1031 composes detailed non-live item reward projection descriptors and projection-warning descriptors into `QuestFinishOperationPlanService` before the existing coarse item reward placeholder and before quest-state mutation.
- UOW-1032 adds a non-live `QuestService.giveReward` projection scaffold for kinah, XP, title, AP, DP, GP, cube expansion, and warehouse expansion. It is not yet composed into the operation plan.
- UOW-1033 composes non-live non-item reward projection descriptors and warning descriptors into `QuestFinishOperationPlanService` after item reward projection and before the existing coarse non-item reward placeholder.
- C# has quest and NPC faction read hydration, but no corresponding write persistence path for these completion mutations.
- C# has packet serializers for quest list/completed-list shapes and a non-sending `SM_QUEST_ACTION(ActionType.UPDATE, qs)` body. No production quest-finish send path is wired.
- C# has no `QuestEngine.onQuestCompleted` equivalent or production nearby-refresh send trigger wired to quest completion.

## Recommended Implementation Slices

1. Decide persistence failure-ordering policy before replacing detailed descriptors with repository writes.
2. Wire live sends, live callbacks, and DAO writes only behind explicit opt-in tests.

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- Reward calculation and inventory mutation ordering are source-audited and partially composed as non-live descriptors. Item reward projection now covers fixed/selectable/class/extended descriptor categories and can be nested under quest-finish operation planning, but real XML loading, bonus handlers, live `ItemService.addItem`, and non-item mutation remain missing.
- Non-item reward projection can now describe Java `giveReward` fields and rate dependencies, but those descriptors are not yet nested under quest-finish operation planning or live reward services.
- Non-item reward descriptors now survive quest-finish operation planning, but still do not execute live reward services.
- `SM_QUEST_ACTION` extra-category suppression is currently an explicit C# flag rather than a production static-data lookup.
- Java callback handlers can perform additional quest mutations and packet sends; C# has no quest handler runtime yet.
- Java completion callback registration order depends on dynamic script class loading and reflection; C# has no equivalent ordering source.
- Persistence is deferred and split across player-store phases, so immediate quest completion is not transactional with later DAO writes.
- `PlayerQuestListDAO.store` commits delete/insert/update phases separately and does not rollback on helper-level SQL errors; matching or intentionally changing this behavior needs a deliberate persistence design.
- The C# operation plan uses descriptors for side effects; callers must not treat descriptor presence as live execution.
- C# lacks Java `PersistentState` tracking for quest and NPC-faction snapshots, so future write plans need explicit operation inputs or a state-tracking model.
