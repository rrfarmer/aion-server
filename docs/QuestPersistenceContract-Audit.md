# Quest Persistence Contract Audit

Date: May 25, 2026
Unit of Work: UOW-1021; updated by UOW-1022, UOW-1023, and UOW-1024

## Purpose

This audit records Java quest and NPC-faction persistence behavior before C# live quest-finish writes are introduced.

Java remains the source of truth. This unit is documentation-only and does not implement quest or NPC-faction writes, transaction changes, live quest completion, packet sends, callbacks, inventory mutation, or nearby-refresh behavior.

## Java Source Breadcrumbs

- `game-server/src/com/aionemu/gameserver/services/player/PlayerLeaveWorldService.java`
- `game-server/src/com/aionemu/gameserver/services/player/PlayerService.java`
- `game-server/src/com/aionemu/gameserver/dao/PlayerQuestListDAO.java`
- `game-server/src/com/aionemu/gameserver/dao/PlayerNpcFactionsDAO.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/QuestStateList.java`
- `game-server/src/com/aionemu/gameserver/questEngine/model/QuestState.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/npcFaction/NpcFactions.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/npcFaction/NpcFaction.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/Persistable.java`

## Logout Store Ordering

`PlayerLeaveWorldService.leaveWorld` runs many logout side effects before persistence. The quest-relevant tail is:

1. `QuestEngine.onLogOut(new QuestEnv(null, player, 0))`.
2. Capture `lastOnline`.
3. Delete the player controller from the world.
4. Set common data online to false and last-online timestamp.
5. Update position fields from the live position.
6. Notify chat server of player logout.
7. Call `PlayerService.storePlayer(player)`.
8. Clear inventory, warehouse, and account-warehouse owners.
9. Store old character level.
10. Store last-online time.
11. `PlayerDAO.onlinePlayer(player, false)`, documented in Java as the marker that the player was fully saved and may enter world again.

`PlayerService.storePlayer` saves player systems in this order:

1. `PlayerDAO.storePlayer`
2. `PlayerSkillListDAO.storeSkills`
3. `PlayerSettingsDAO.saveSettings`
4. `PlayerQuestListDAO.store`
5. `AbyssRankDAO.storeAbyssRank`
6. prison and gather punishment stores
7. `InventoryDAO.store`
8. house saves
9. `ItemStoneListDAO.save`
10. `MailDAO.storeMailbox`
11. portal, craft, and house-object cooldown stores
12. `PlayerNpcFactionsDAO.storeNpcFactions`
13. account passport and optional headhunting stores

Important parity note: quest-state persistence happens before inventory persistence, while NPC-faction persistence happens after cooldowns/mailbox and after inventory.

## Quest State Persistence

`PlayerQuestListDAO.store`:

- Reads current quest states from `QuestStateList.getAllQuestState`, which returns values from a `TreeMap` sorted by quest id.
- Reads deleted quest ids from `QuestStateList.getDeletedQuestIds`, a `HashSet`.
- Returns early when both current and deleted sets are empty.
- Opens one connection and sets `autoCommit(false)`.
- Calls delete, insert, and update helpers in that order.
- Each helper performs a batch and calls `con.commit()` independently.
- Helper-level `SQLException` is caught and logged inside the helper, without rollback and without rethrow.
- Outer `SQLException` is caught and logged.
- After the database attempt, every current quest state is marked `PersistentState.UPDATED` regardless of whether any helper failed.

The DAO persists:

- `status`
- `quest_vars`
- `flags`
- `complete_count`
- `next_repeat_time`
- nullable `reward`
- nullable `complete_time`

The DAO does not persist callback side effects separately; it stores whatever `QuestStateList` contains when `PlayerService.storePlayer` later runs.

## Quest Persistent State Rules

`QuestState` starts as `PersistentState.NEW`.

Mutators such as `setQuestVar`, `setQuestVarById`, `setStatus`, `setCompleteCount`, `setRewardGroup`, and `setFlags` mark the quest `UPDATE_REQUIRED`. `setNextRepeatTime` does not mark persistence by itself, but in Java finish flow it follows `setStatus`/`setQuestVar`, which already marked the state changed.

`QuestState.setStatus(COMPLETE)` increments complete count and sets complete time only when the previous status was not already `COMPLETE`, unless called through the overload with `updateCompleteCountAndTime = false`.

`QuestStateList.deleteQuest` removes the quest from the sorted map, adds its id to the deleted `HashSet`, and marks the removed state `DELETED`.

`QuestState.setPersistentState(DELETED)` changes a `NEW` quest to `NOACTION` rather than `DELETED`; otherwise it becomes `DELETED`. This means newly-created-then-deleted quest state objects are not stored as deletes through the state list, though the deleted id set can still drive a delete.

## NPC Faction Persistence

`PlayerNpcFactionsDAO.storeNpcFactions` iterates `player.getNpcFactions().getNpcFactions()`, which is backed by a `HashMap` value collection.

For each faction:

- `PersistentState.NEW` calls `insertNpcFaction`.
- `PersistentState.UPDATE_REQUIRED` calls `updateNpcFaction`.
- `UPDATED`, `DELETED`, and `NOACTION` are ignored.

Each insert/update:

- Opens its own connection.
- Executes a single statement with default auto-commit behavior.
- Catches and logs exceptions.
- Does not mark the faction `UPDATED` after success.

The DAO persists:

- `active`
- `time`
- `state`
- `quest_id`

`NpcFaction` starts as `NEW`. Its setters mark `UPDATE_REQUIRED` only when the state is not `NEW`, so newly created factions remain `NEW` through multiple mutations until inserted.

`NpcFactions.completeQuest` mutates only the active mentor/non-mentor slot:

- sets `time` to the next 9:00 reset epoch seconds,
- sets `state` to `COMPLETE`,
- updates in-memory `timeLimit`,
- may set mentor title flag and send title packets for mentor quests.

The mentor title side effects are not part of `PlayerNpcFactionsDAO`; they belong to player common-data/title packet behavior.

## Current C# State

- `PlayerEnterWorldRepository.LoadPlayerQuestsAsync` reads `player_quests` fields including `reward`, `next_repeat_time`, and `complete_time`.
- `LoadPlayerQuestsAsync` orders by `quest_id`; Java load uses SQL without `ORDER BY` but then stores quests in a `TreeMap`.
- `PlayerEnterWorldRepository.LoadPlayerNpcFactionsAsync` reads NPC faction rows and builds `PlayerNpcFactionsSnapshot`.
- `SavePlayerLogoutAsync` saves life stats, cooldowns, settings, and core `players` columns, then marks `online = false`.
- UOW-1022 adds `QuestPersistencePlanService`, a pure non-live planner that accepts explicit Java-shaped persistence states and emits delete, insert, and update descriptors in Java DAO phase order.
- UOW-1023 adds `NpcFactionPersistencePlanService`, a pure non-live planner that accepts explicit Java-shaped persistence states and emits insert/update descriptors in Java DAO filter order while preserving caller order for Java `HashMap.values()` risk.
- UOW-1024 composes both persistence plans into `QuestFinishOperationPlanService` as detailed non-live descriptors after nearby-refresh planning.
- C# has no quest-state write path equivalent to `PlayerQuestListDAO.store`.
- C# has no NPC-faction write path equivalent to `PlayerNpcFactionsDAO.storeNpcFactions`.
- `QuestFinishOperationPlanService` has detailed quest and NPC-faction persistence descriptors only when explicit plans are supplied; otherwise it preserves legacy deferred placeholders.
- C# immutable quest/faction snapshots do not carry Java `PersistentState`; write planning must track changed rows explicitly or introduce a Java-shaped persistence-state model.

## Recommended Implementation Slices

1. Decide whether C# should intentionally preserve Java's helper-level commit/no-rollback behavior or use a safer transaction as an explicit intentional difference.
2. Keep live DAO writes disabled until operation plans and failure-ordering tests are in place.

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- Java quest persistence commits delete/insert/update helpers independently and marks in-memory quest states updated even after helper failure.
- Java NPC faction persistence opens one connection per row and does not mark rows `UPDATED` after insert/update.
- Java `HashSet`/`HashMap` ordering affects deleted quest id order and NPC faction write order; C# should not assume deterministic order unless it intentionally normalizes and documents the difference.
- C# lacks `PersistentState` on quest and NPC-faction snapshots, so live writes need either explicit operation inputs or new state tracking.
- UOW-1022 normalizes current quest rows by quest id like Java's `TreeMap`, but deleted quest id set ordering remains caller-provided because Java uses a `HashSet`.
- UOW-1023 preserves caller order for NPC-faction descriptors, but Java `HashMap.values()` ordering still lacks runtime verification.
- UOW-1024 composes detailed descriptors but still does not execute `PlayerService.storePlayer` or model callback mutations that can happen before persistence.
