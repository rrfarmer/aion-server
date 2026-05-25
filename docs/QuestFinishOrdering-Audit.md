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
- C# has quest and NPC faction read hydration, but no corresponding write persistence path for these completion mutations.
- C# has packet serializers for quest list/completed-list shapes and a non-sending `SM_QUEST_ACTION(ActionType.UPDATE, qs)` body. No production quest-finish send path is wired.
- C# has no `QuestEngine.onQuestCompleted` equivalent or production nearby-refresh send trigger wired to quest completion.

## Recommended Implementation Slices

1. Compose the staged reward/work-item descriptors into the main quest-finish operation plan before enabling any live quest-finish path.
2. Connect `SmQuestAction.Update` to the staged operation plan only as a non-live packet descriptor/object.
3. Add quest-state and NPC-faction persistence contracts after reward and operation planning are stable.
4. Wire live sends and DAO writes only behind explicit opt-in tests.

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- Reward calculation and inventory mutation ordering are source-audited and partially staged as non-live descriptors, but full reward selection and mutation are not ported.
- `SM_QUEST_ACTION` extra-category suppression is currently an explicit C# flag rather than a production static-data lookup.
- Java callback handlers can perform additional quest mutations and packet sends; C# has no quest handler runtime yet.
- Persistence is deferred and split across player-store phases, so immediate quest completion is not transactional with later DAO writes.
- `PlayerQuestListDAO.store` commits delete/insert/update phases separately and does not rollback on helper-level SQL errors; matching or intentionally changing this behavior needs a deliberate persistence design.
- The C# operation plan uses descriptors for side effects; callers must not treat descriptor presence as live execution.
