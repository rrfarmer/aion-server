# Quest XP Reward Audit - UOW-1044/UOW-1064

Date: May 25, 2026

## Java Source

- `game-server/src/com/aionemu/gameserver/services/QuestService.java#giveReward`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/PlayerCommonData.java#addExp`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/PlayerCommonData.java#setExp`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/Rates.java#XP_QUEST`
- `game-server/src/com/aionemu/gameserver/configs/main/RatesConfig.java#XP_QUEST_RATES`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_STATUPDATE_EXP.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java` XP reward helpers
- `game-server/src/com/aionemu/gameserver/controllers/PlayerController.java#onLevelChange`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/npcFaction/NpcFactions.java#onLevelUp`
- `game-server/src/com/aionemu/gameserver/questEngine/QuestEngine.java#onLevelChanged`
- `game-server/src/com/aionemu/gameserver/services/HTMLService.java#sendGuideHtml`
- `game-server/src/com/aionemu/gameserver/dataholders/GuideHtmlData.java#getTemplatesFor`
- `game-server/src/com/aionemu/gameserver/configs/main/HTMLConfig.java#ENABLE_GUIDES`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_QUESTIONNAIRE.java`
- `game-server/src/com/aionemu/gameserver/services/SkillLearnService.java#learnNewSkills`
- `game-server/src/com/aionemu/gameserver/services/SkillLearnService.java#autoLearnSkills`
- `game-server/src/com/aionemu/gameserver/services/SkillLearnService.java#onLearnSkill`
- `game-server/src/com/aionemu/gameserver/model/skill/PlayerSkillList.java#addSkill`
- `game-server/src/com/aionemu/gameserver/model/skill/PlayerSkillList.java#removeSkill`
- `game-server/src/com/aionemu/gameserver/dataholders/SkillTreeData.java#getTemplatesFor`
- `game-server/src/com/aionemu/gameserver/services/reward/StarterKitService.java#onLevelUp`
- `game-server/src/com/aionemu/gameserver/configs/main/CustomConfig.java#ENABLE_STARTER_KIT`
- `game-server/src/com/aionemu/gameserver/services/BonusPackService.java#addPlayerCustomReward`
- `game-server/src/com/aionemu/gameserver/services/FactionPackService.java#addPlayerCustomReward`
- `game-server/src/com/aionemu/gameserver/dao/BonusPackDAO.java`
- `game-server/src/com/aionemu/gameserver/dao/FactionPackDAO.java`

## Java Behavior Summary

`QuestService.giveReward` enters the XP branch only when `rewards.getExp() != 0`. It looks up the target NPC template and passes `npcTemplate.getL10n()` when available to `PlayerCommonData.addExp(rewards.getExp(), Rates.XP_QUEST, npcName)`.

`PlayerCommonData.addExp`:

1. Returns for no-exp state.
2. Returns for online players in world id `301200000`.
3. Applies `Rates.XP_QUEST`.
4. Applies repose bonus when the base reward is positive and repose energy is available.
5. Applies salvation bonus for level 15+ players with salvation percent.
6. Calls `setExp(exp + finalReward)`.
7. Sends an XP system message variant depending on NPC name, repose bonus, and salvation bonus.
8. Sends an ascension-limit message when a level-9 player reaches the level-10 start XP.

`PlayerCommonData.setExp` clamps XP to the current level-cap start XP, derives the displayed level from the experience table, calls level-change side effects while attached to a player, and sends `SM_STATUPDATE_EXP`.

Level-change side effects include stat template refresh, max repose recalculation, salvation reset, player upgrade, level-up animation, NPC faction level-up handling, quest level-change callbacks, nearby quest refresh, guide/starter-kit side effects, and skill auto-learn.

## C# State After UOW-1044

- Added `GameServerRateOptions.XpQuestRates`, loading `gameserver.rates.xp.quest` with Java default `1.0, 2.0`.
- Added non-live `QuestRewardService.CreateXpRewardPlan`.
- Added `QuestRewardService.ApplyQuestXpRate`.
- Added `QuestXpRewardPlan`, `QuestXpRewardStatus`, `QuestXpRewardMessageKind`, and `QuestXpRewardPacketIntent`.
- The plan records Java guard outcomes, rated base XP, repose usage/bonus, salvation bonus, final XP reward, capped resulting XP, display level, max repose energy, ascension-limit message intent, and packet/side-effect intents.
- The helper does not mutate `Player.Exp`, `Player.Level`, or `Player.ReposeEnergy`.
- Quest finish still only carries XP as non-item projection metadata; the XP plan is not composed into `QuestFinishOperationPlanService`.

## C# State After UOW-1045

- Added `QuestFinishOperationDescriptor.XpRewardPlan`.
- Extended `QuestFinishRewardSideEffectContext` with optional XP inputs: `ExperienceTable`, `TargetNpcName`, `NoExp`, `QuestXpBoostStat`, `HasLegionBonus`, `SalvationPercent`, and `IsDaeva`.
- `QuestFinishOperationPlanService` now emits a non-live `NonItemRewardSideEffectPlan` descriptor immediately after an XP non-item projection when the side-effect context includes a `PlayerExperienceTable`.
- The descriptor carries `QuestXpRewardPlan` metadata and preserves Java reward order before title, AP, DP, GP, cube, warehouse, and the coarse non-item placeholder.
- Added `QuestRewardService.CreateXpRewardPlanFromRates` so operation planning can reuse the XP planner without constructing unrelated resource-stat services.
- Quest finish still does not mutate XP, send packets, run level-change hooks, or persist player state.

## C# State After UOW-1046

- Added concrete `SmSystemMessage` helpers for the Java XP messages used by `PlayerCommonData.addExp`:
  - `STR_GET_EXP`
  - `STR_GET_EXP2`
  - `STR_GET_EXP_VITAL_BONUS`
  - `STR_GET_EXP_MAKEUP_BONUS`
  - `STR_GET_EXP_VITAL_MAKEUP_BONUS`
  - `STR_GET_EXP2_VITAL_BONUS`
  - `STR_GET_EXP2_MAKEUP_BONUS`
  - `STR_GET_EXP2_VITAL_MAKEUP_BONUS`
  - `STR_LEVEL_LIMIT_QUEST_NOT_FINISHED1`
- Added packet serialization regression assertions for the XP message ids and parameter order.
- The helpers are not wired to `QuestXpRewardPlan` or live quest-finish execution yet.
- Read-only level-change analysis confirmed Java XP level-up side effects happen before `QuestService.finishQuest` marks the quest complete, and before the later quest update/completion-callback sequence.

## C# State After UOW-1047

- Added `QuestRewardService.CreateXpSystemMessagePackets(QuestXpRewardPlan)`.
- The helper maps `QuestXpRewardPlan.MessageKind` to the concrete UOW-1046 `SmSystemMessage` XP helpers.
- It returns XP gain message metadata first and appends `LevelLimitQuestNotFinished` when `RequiresAscensionLimitMessage` is true, matching Java `PlayerCommonData.addExp` message order after `setExp`.
- It returns no packets for skipped XP plans.
- The helper is still non-live: it creates packet objects only and does not send them.

## C# State After UOW-1048

- Added non-live `QuestXpExecutionPlanService.CreatePlan(QuestXpRewardPlan)`.
- The plan stages Java `PlayerCommonData.addExp -> setExp` execution order without mutating the player:
  1. `PlayerCommonData.setExp`.
  2. Java-order `PlayerController.onLevelChange` descriptors when the XP plan changes level.
  3. `SM_STATUPDATE_EXP` descriptor when Java `setExp` would enter its mutation/send branch.
  4. XP `SM_SYSTEM_MESSAGE` packet metadata.
  5. Optional `STR_LEVEL_LIMIT_QUEST_NOT_FINISHED1` descriptor after the XP message.
- Level-change descriptors preserve the reviewed Java order: ratio update, stats template refresh, max repose update, salvation reset, upgrade-player life/stat/team/legion work, level-up animation, NPC faction level-up, quest level-change callbacks, nearby refresh, guide HTML, skill auto-learn, bonus pack, faction pack, and starter kit.
- The plan also records `MinNewLevel`, matching Java's `oldLevel < newLevel ? oldLevel + 1 : oldLevel - 1` value used by guide/skill/starter-kit callers.
- Everything remains metadata only; no live `Player.Exp`, `Player.Level`, repose/salvation, packet send, quest callback, skill, faction, team, legion, guide, custom reward, or persistence side effect is executed.

## C# State After UOW-1049

- Added `SmActionAnimation.LevelUp = 0`, matching Java `ActionAnimation.LEVEL_UP(0)`.
- Extended the packet regression coverage in `GamePacketTests.CharacterSelectionServerPackets_WriteJavaShapedPayloads` to serialize a level-up animation payload as `targetObjectId`, `LevelUp`, and `newLevel`.
- Updated the XP execution plan descriptor note to reference the named C# constant instead of documenting it as missing.
- The level-up animation remains metadata only in XP execution; no live broadcast is wired from XP level changes.

## C# State After UOW-1050

- Added `PlayerLevelChangeUpgradePlanService.CreatePlan`.
- The sub-plan stages Java `PlayerController.upgradePlayer` order without mutating the player:
  1. `PlayerLifeStats.synchronizeWithMaxStats`.
  2. `PlayerGameStats.updateStatsVisually`.
  3. Conditional `TeamStatUpdater.add` for group/alliance membership.
  4. Conditional `LegionService.updateMemberInfo` for legion membership.
- The sub-plan can record planned HP/MP/FP synchronization when a max-stat snapshot is supplied, but it does not calculate max stats itself and does not send HP/MP/FP, `SM_STATS_INFO`, group/alliance, or legion packets.
- The staged XP execution plan still records `upgradePlayer` as descriptors only; this sub-plan is a prerequisite surface, not live integration.

## C# State After UOW-1051

- Added `NpcFactionLevelUpPlanService.CreatePlan`.
- The planner stages Java `NpcFactions.onLevelUp` behavior without mutating live player state:
  1. Reads active non-mentor and mentor faction slots in Java order.
  2. Uses `NpcFactionTable.GetNpcFactionById` as the C# equivalent of `DataManager.NPC_FACTIONS_DATA.getNpcFactionById`.
  3. Leaves factions active when `template.maxLevel >= player.Level`.
  4. Plans deactivation when `template.maxLevel < player.Level`.
  5. Records `QuestService.abandonQuest` intent only for START-state factions.
  6. Records Java `SM_SYSTEM_MESSAGE.STR_FACTION_LEAVE_BY_LEVEL_LIMIT` message id `1400770`.
  7. Resets the planned faction state to `Noting`, matching Java after the optional abandon call.
- The helper is non-live and does not call quest abandon, send system messages, persist `PlayerNpcFactionsDAO`, or compose into `QuestXpExecutionPlanService`.

## C# State After UOW-1052

- Added `QuestLevelChangedCallbackPlanService.CreatePlan`.
- The planner stages Java `QuestEngine.onLevelChanged` callback dispatch without loading or invoking dynamic handlers:
  1. Applies Java `registerOnLevelChanged` race scoping: null race registers for both Elyos and Asmodians; explicit race registers only for that race.
  2. Preserves registered quest order while suppressing duplicate quest ids for the player's race list.
  3. Skips quests whose player quest state is `COMPLETE`.
  4. Skips missing quest handlers.
  5. Plans `AbstractQuestHandler.onLevelChangedEvent(player)` dispatch for missing quest states and all non-COMPLETE states.
- The helper is non-live and does not call `defaultOnLevelChangedEvent`, mutate quests to START/LOCKED, send quest packets, invoke dynamic handlers, or compose into `QuestXpExecutionPlanService`.

## C# State After UOW-1053

- Updated `NearbyQuestRefreshPlan.WouldSendPacket` to match Java `PlayerController.updateNearbyQuests`.
- Java always sends `SM_NEARBY_QUESTS` after building the nearby quest map, including when the world has no registered quest ids or every candidate is filtered out.
- C# now reports packet-send intent for `Ready`, `NoWorldQuestIds`, and `NoMarkers` plans.
- C# still treats missing world instance or missing quest template table as guarded planner blockers because live controller context is not wired.

## C# State After UOW-1054

- Added non-live `GuideHtmlLevelChangePlanService.CreatePlan`.
- The planner stages Java `PlayerController.onLevelChange -> HTMLService.sendGuideHtml` behavior without allocating ids, sending packets, writing guide rows, or rendering HTML:
  1. Applies the Java outer gates: `HTMLConfig.ENABLE_GUIDES` and `player.isSpawned()`.
  2. Preserves the Java inclusive level loop from `fromLevel` through `toLevel`.
  3. Preserves `GuideHtmlData.getTemplatesFor` order for each level: class+race, class+`PC_ALL`, all-class+race, all-class+`PC_ALL`.
  4. Records inactive `GuideTemplate.isActivated() == false` skips.
  5. Records future side-effect intent for `IDFactory.nextId`, `SM_QUESTIONNAIRE` chunk sends through `HTMLService.sendData`, and `GuideDAO.saveGuide`.
- Missing guide static data, disabled guides, unspawned players, and empty level ranges are explicit non-live planner statuses.
- The staged XP execution plan still records guide HTML as a descriptor only; this sub-plan is not composed into `QuestXpExecutionPlanService`.

## C# State After UOW-1055

- Added non-live `SkillLearnService.CreateAutoLearnPlan`.
- The planner stages Java `PlayerController.onLevelChange -> SkillLearnService.learnNewSkills` behavior without mutating `Player.Skills`, sending packets, applying effects, learning recipes, refreshing nearby quests, or persisting skills:
  1. Preserves Java reverse level iteration from `toLevel` down to `fromLevel`.
  2. Preserves switched-class starting-class backfill for levels below 10.
  3. Reuses `SkillTreeTable.GetTemplatesFor` class+race then class+`PC_ALL` ordering.
  4. Records non-autolearn template skips.
  5. Records Java human-gathering skip for advanced classes.
  6. Projects Java `PlayerSkillList.addSkill` add/upgrade/no-change behavior.
  7. Records `onLearnSkill` side-effect intent for skill-list packets, craft-level-up animation broadcasts, passive effect application, nearby quest refresh, and recipe auto-learn.
  8. Projects Daeva human gathering upgrade from skill `30001` to `30002`, then planned removal of `30001`.
- Missing skill tree/static skill templates, missing player, and empty level ranges are explicit non-live planner statuses.
- The staged XP execution plan still records skill auto-learn as a descriptor only; this sub-plan is not composed into `QuestXpExecutionPlanService`.

## C# State After UOW-1056

- Added non-live `StarterKitLevelChangePlanService.CreatePlan`.
- The planner stages Java `PlayerController.onLevelChange -> StarterKitService.onLevelUp` behavior without creating system-mail rows or sending live mail:
  1. Applies the Java caller gate equivalent to `CustomConfig.ENABLE_STARTER_KIT`.
  2. Preserves Java inclusive level iteration from `fromLevel` through `toLevel`.
  3. Preserves fixed `LinkedHashMap` reward bucket order for levels 1, 20, 25, 35, 50, and 60.
  4. Records future `SystemMailService.sendMail` intent with Java sender, title, body, item id/count, zero kinah, and express letter type.
- Disabled starter-kit config, missing player, empty level ranges, and no matching reward levels are explicit non-live planner statuses.
- The staged XP execution plan still records starter kit as a descriptor only; this sub-plan is not composed into `QuestXpExecutionPlanService`.

## C# State After UOW-1057

- Added non-live `CustomLevelRewardPlanService.CreateBonusPackPlan`.
- Added non-live `CustomLevelRewardPlanService.CreateFactionPackPlan`.
- The planner stages Java `BonusPackService.addPlayerCustomReward` and `FactionPackService.addPlayerCustomReward` without writing DAO rows or sending live mail:
  1. Applies Java level-65 and mailbox-capacity guards.
  2. Records one-per-account DAO load/store gates from `BonusPackDAO` and `FactionPackDAO`.
  3. Preserves Java bonus-pack reward item ids/counts and express system-mail metadata.
  4. Preserves Java faction-pack reward item ids/counts, Elyos/Asmodian account creation windows, and opposite-race item-template skip behavior.
  5. Records future `SystemMailService.sendMail` intent with Java sender/title/body, item id/count, zero kinah, and express letter type.
- The planner takes account creation local time, DAO results, and optional item templates as explicit inputs because the live account object, DAO, server-time conversion, and `DataManager.ITEM_DATA` dependencies are not wired into XP execution.
- The staged XP execution plan still records bonus/faction packs as descriptors only; this sub-plan is not composed into `QuestXpExecutionPlanService`.

## C# State After UOW-1058

- Added optional non-live `QuestXpLevelChangeCompositionContext` to `QuestXpExecutionPlanService.CreatePlan`.
- Added `QuestXpLevelChangeSubPlanDescriptor` metadata to `QuestXpExecutionPlan`.
- The XP execution plan can now carry already-created level-change sub-plan summaries in Java `PlayerController.onLevelChange` order:
  1. `PlayerLevelChangeUpgradePlanService`
  2. `NpcFactionLevelUpPlanService`
  3. `QuestLevelChangedCallbackPlanService`
  4. `NearbyQuestRefreshPlanService`
  5. `GuideHtmlLevelChangePlanService`
  6. `SkillLearnService.CreateAutoLearnPlan`
  7. `CustomLevelRewardPlanService` bonus plan
  8. `CustomLevelRewardPlanService` faction plan
  9. `StarterKitLevelChangePlanService`
- Composition is metadata only. It records plan status, applied flag, descriptor counts, planned descriptor counts, Java source breadcrumbs, and non-live state.
- Sub-plan metadata is only attached when the XP reward plan changes level, matching Java `PlayerController.onLevelChange` being skipped when old and new levels are equal.
- The context still requires callers to construct sub-plans explicitly; no live level-change side effects execute and no supporting DAO/mail/static-data dependencies are invoked by XP execution.

## C# State After UOW-1059

- Added non-live `QuestXpLevelChangeContextFactoryService.CreateContext`.
- Added `QuestXpLevelChangeContextFactoryInput` as a snapshot-style input object for level-change sub-plan prerequisites.
- The factory builds a `QuestXpLevelChangeCompositionContext` from a supplied player and explicit runtime/static-data inputs without executing live side effects:
  1. `PlayerLevelChangeUpgradePlanService.CreatePlan`
  2. `NpcFactionLevelUpPlanService.CreatePlan`
  3. `QuestLevelChangedCallbackPlanService.CreatePlan`
  4. `NearbyQuestRefreshPlanService.CreatePlan`
  5. `GuideHtmlLevelChangePlanService.CreatePlan`
  6. `SkillLearnService.CreateAutoLearnPlan`
  7. `CustomLevelRewardPlanService.CreateBonusPackPlan`
  8. `CustomLevelRewardPlanService.CreateFactionPackPlan`
  9. `StarterKitLevelChangePlanService.CreatePlan`
- Missing player/dependency inputs produce guarded sub-plans rather than live behavior.
- The factory still requires callers to provide DAO outcomes, account creation local time, guide/static templates, skill/static templates, NPC faction table, nearby quest runtime state, and starter-kit config. It does not read production services, send packets, write repositories, or mutate the player.

## C# State After UOW-1060

- Extended `QuestFinishRewardSideEffectContext` with optional `QuestXpLevelChangeContextFactoryInput`.
- Extended `QuestFinishOperationDescriptor` with optional `QuestXpExecutionPlan`.
- When a quest XP reward side-effect context includes both an experience table and level-change context input, quest-finish operation planning now creates:
  1. the existing non-live `QuestXpRewardPlan`;
  2. a non-live `QuestXpLevelChangeCompositionContext` through `QuestXpLevelChangeContextFactoryService`;
  3. a non-live `QuestXpExecutionPlan` carrying Java-order level-change descriptor metadata and sub-plan summaries.
- Default behavior is unchanged when no level-change context input is supplied.
- This remains metadata only; quest finish still does not mutate XP/level, execute level-change hooks, send packets, or persist state.

## C# State After UOW-1061

- Added `ICustomLevelRewardRepository` with bonus/faction receipt load/store methods.
- Added `CustomLevelRewardReceiptRepositoryPlan` to preserve Java `SELECT receiving_player` and `REPLACE INTO ... (account_id, receiving_player)` SQL for `bonus_packs` and `faction_packs`.
- Added `MySqlCustomLevelRewardRepository` with Java-compatible load failure fallback (`int.MaxValue`, matching `Integer.MAX_VALUE`) and store failure fallback (`false`).
- Registered the repository in game-server DI.
- The repository is a prerequisite only; no live custom reward mail delivery or XP level-change execution consumes it by default.

## C# State After UOW-1062

- Added `SystemMailRewardPlanService` for non-live starter/custom reward system-mail payload planning.
- The planner applies Java `SystemMailService.sendMail` guard rules for attached item count, missing item templates, recipient/sender name length, and mailbox capacity.
- The planner preserves Java title/message truncation limits, express letter type mapping, unread mail state, mailbox storage id, unequipped attached item state, and non-live `PlayerMail`/`InventoryItem` metadata.
- It does not allocate ids, write `MailDAO`, write `InventoryDAO`, update online/offline mailbox counts, send `SM_MAIL_SERVICE`, send `STR_POSTMAN_NOTIFY`, or mutate the recipient mailbox.

## C# State After UOW-1063

- Added `CustomLevelRewardExecutionService`.
- The service is an opt-in boundary for bonus/faction custom rewards that coordinates:
  1. Java static guards from `CustomLevelRewardPlanService`;
  2. receipt DAO load through `ICustomLevelRewardRepository`;
  3. receipt DAO store through `ICustomLevelRewardRepository`;
  4. non-live mail payload planning through `SystemMailRewardPlanService`.
- It preserves Java's important order: static guards run before DAO access, already-received accounts stop after load, store failures stop before mail planning, and faction opposite-race item filtering happens after receipt storage.
- It is registered for DI, but default XP/quest-finish gameplay still does not invoke it.
- Mail output remains non-live metadata; no `MailDAO`, `InventoryDAO`, mailbox fanout, packet send, or mailbox mutation is executed.

## C# State After UOW-1064

- Extended `QuestXpLevelChangeCompositionContext` with optional `BonusPackExecutionResult` and `FactionPackExecutionResult`.
- Extended `QuestXpLevelChangeContextFactoryInput` so callers can explicitly supply those execution results without the factory invoking repositories or mail planning.
- `QuestXpExecutionPlanService` now prefers supplied execution results over legacy plan-only custom reward metadata when composing bonus/faction level-change sub-plan summaries.
- This records execution-boundary status and planned mail counts in XP metadata while preserving Java `PlayerController.onLevelChange` order.
- If an execution result indicates a live receipt boundary, the sub-plan descriptor is marked live. Default factory usage still does not execute repositories or mail delivery.

## C# State After UOW-1065

- Added `SystemMailRewardPersistencePlanService` for non-live persistence and mailbox-fanout operation metadata from `SystemMailRewardPlan` outputs.
- The planner preserves Java `SystemMailService.sendMail` ordering:
  1. `MailDAO.storeLetter` / `saveLetter` insert intent with Java SQL and parameter order.
  2. Optional `InventoryDAO.store` / `insertItems` attached-item intent with Java insert SQL and parameter order.
  3. Offline `MailDAO.updateOfflineMailCounter` intent only after successful letter/item persistence.
  4. Online mailbox mutation and `SM_MAIL_SERVICE` fanout intent only when the recipient is online and has a mailbox.
  5. Open-mailbox `MailService.sendMailList` refresh intent with Java express-only flag derivation.
  6. Express `SM_SYSTEM_MESSAGE.STR_POSTMAN_NOTIFY` intent after mailbox/list fanout.
- The service remains non-live and does not execute SQL, mutate the recipient mailbox, update mailbox counters, send packets, or bridge into default XP/custom reward execution.

## C# State After UOW-1066

- Added `SystemMailRewardPersistenceExecutionService`, an explicit opt-in execution boundary for `SystemMailRewardPersistencePlan` operations.
- The execution boundary is disabled by default through `SystemMailRewardPersistenceExecutionOptions.Disabled`.
- When enabled with an injected `ISystemMailRewardPersistenceOperationExecutor`, it executes operations in the Java-order plan and stops on Java-critical failures:
  1. `MailDAO.storeLetter` failure returns a store-letter failure result and prevents item persistence and mailbox fanout.
  2. `InventoryDAO.store` failure returns an attached-item failure result and prevents mailbox counter/fanout operations.
  3. Non-critical fanout operation failures are recorded without changing the already-executed DAO ordering metadata.
- No default runtime caller or live repository executor is registered; this remains an explicit boundary for future wiring.

## C# State After UOW-1067

- Added `SystemMailRewardPersistenceOperationExecutor`, a concrete opt-in executor for staged system-mail persistence operations.
- Extended `IMailRepository` / `MySqlMailRepository` with system-mail-specific methods:
  1. `StoreSystemMailLetterAsync` for Java `MailDAO.storeLetter` / `saveLetter` style mail inserts.
  2. `StoreSystemMailAttachedItemAsync` for Java `InventoryDAO.store(attachedItem, recipientId)` style mailbox item inserts.
  3. `UpdateOfflineMailboxCounterAsync` for Java `MailDAO.updateOfflineMailCounter`.
- Staged operations now carry mail/item payloads needed by the executor while retaining Java SQL/parameter metadata.
- The executor delegates online mailbox fanout to the existing `IGameClientConnectionRegistry.NotifyMailReceivedAsync` aggregate, which performs mailbox insertion, `SM_MAIL_SERVICE`, open-mailbox list refresh, and express postman notification in one call.
- Registered `SystemMailRewardPersistenceExecutionService` and `ISystemMailRewardPersistenceOperationExecutor` in game-server DI. This does not enable automatic reward mail delivery because callers still need the explicit execution option from UOW-1066.

## C# State After UOW-1068

- Added `SystemMailRepositoryPlan` command metadata for system-mail repository writes.
- `MySqlMailRepository.StoreSystemMailLetterAsync`, `StoreSystemMailAttachedItemAsync`, and `UpdateOfflineMailboxCounterAsync` now execute through these Java-shaped command plans.
- The command plans expose Java artifact breadcrumbs, SQL, parameter names, and values for:
  1. `MailDAO.storeLetter` / `saveLetter`.
  2. `InventoryDAO.store` / `insertItems`.
  3. `MailDAO.updateOfflineMailCounter`.
- This improves deterministic SQL/parameter review coverage before any opt-in DB integration test. It still does not run live DB validation by default.

## C# State After UOW-1069

- Added opt-in `SystemMailRepositoryDatabaseIntegrationTests`.
- The new test is gated by `AION_GAMESERVER_DB_INTEGRATION=1`, matching the existing game-server DB integration pattern.
- When enabled, it initializes `game-server/sql/aion_gs.sql`, seeds a Java-schema player, and exercises `MySqlMailRepository` system-mail methods:
  1. `StoreSystemMailLetterAsync`.
  2. `StoreSystemMailAttachedItemAsync`.
  3. `UpdateOfflineMailboxCounterAsync`.
- The normal suite confirms the integration test is present and gated, but live DB parity remains unverified unless the opt-in environment is run.

## C# State After UOW-1070

- Extended opt-in `SystemMailRepositoryDatabaseIntegrationTests` with an attached-item failure-ordering scenario.
- The new test is gated by `AION_GAMESERVER_DB_INTEGRATION=1`.
- When enabled, it stores the mail letter first, forces `StoreSystemMailAttachedItemAsync` to fail with a duplicate inventory primary key, and verifies:
  1. the mail row remains persisted;
  2. the duplicate inventory row was not duplicated;
  3. `players.mailbox_letters` remains at the pre-mail value because the offline counter update is not called.
- This documents Java `SystemMailService.sendMail` ordering around `InventoryDAO.store` failure without enabling automatic reward mail delivery or proving live DB parity in the default suite.

## C# State After UOW-1071

- Extended opt-in `SystemMailRepositoryDatabaseIntegrationTests` with a store-letter failure-ordering scenario.
- The new test is gated by `AION_GAMESERVER_DB_INTEGRATION=1`.
- When enabled, it seeds a duplicate mail row, forces `StoreSystemMailLetterAsync` to fail before any item/counter step, and verifies:
  1. no attached-item row is written;
  2. `players.mailbox_letters` remains at the pre-mail value.
- This documents Java `SystemMailService.sendMail` immediate-return behavior when `MailDAO.storeLetter` fails, while keeping default reward mail delivery disabled and live DB parity unverified until the gate is run.

## C# State After UOW-1072

- Added `GameClientSocketServerMailFanoutTests` for the online recipient branch used by `SystemMailRewardPersistenceOperationExecutor`.
- The tests exercise `GameClientSocketServer.NotifyMailReceivedAsync` with a captured `GameServerConnection` send observer and verify Java `SystemMailService.updateRecipientMailbox` ordering for C# packets:
  1. mailbox state `SM_MAIL_SERVICE`;
  2. open-mailbox `MailService.sendMailList` list packet when `MailboxState != 0`;
  3. express `STR_POSTMAN_NOTIFY` after the list packet.
- The express list test verifies express-only filtering includes unread express mail in newest-first order.
- Closed normal-mail and offline-recipient tests verify no list/notify packet is sent when Java would skip those branches.
- This improves online fanout packet-order regression coverage, but default XP/custom reward gameplay still does not invoke automatic reward mail delivery.

## C# State After UOW-1073

- Added `QuestXpCustomRewardRuntimeInputAdapterService`.
- Registered the adapter in DI, but it remains disabled by default through per-call `QuestXpCustomRewardRuntimeInputAdapterOptions.Disabled`.
- When disabled, the adapter returns the supplied `QuestXpLevelChangeContextFactoryInput` unchanged and does not execute custom reward repository load/store calls.
- When explicitly enabled with a `NextObjectId` dependency, it calls `CustomLevelRewardExecutionService` in Java `PlayerController.onLevelChange` custom reward order:
  1. `BonusPackService.addPlayerCustomReward`.
  2. `FactionPackService.addPlayerCustomReward`.
- The result is a new `QuestXpLevelChangeContextFactoryInput` carrying supplied bonus/faction `CustomLevelRewardExecutionResult` metadata for existing XP level-change composition.
- Missing `NextObjectId` stops before repository execution and reports `MissingDependency`.
- This is still not automatic live XP mutation, not automatic system-mail delivery, and not Java runtime verified.

## C# State After UOW-1074

- Added `QuestFinishCustomRewardRuntimeSideEffectAdapterService`.
- Registered the quest-finish side-effect context adapter in DI.
- The adapter keeps the C# opt-in boundary explicit:
  1. disabled options return the original `QuestFinishRewardSideEffectContext` unchanged and do not require a level-change input;
  2. enabled options require `QuestFinishRewardSideEffectContext.LevelChangeContextInput` before repository access;
  3. missing `NextObjectId` is propagated from `QuestXpCustomRewardRuntimeInputAdapterService` without replacing the quest-finish context;
  4. successful execution replaces only `LevelChangeContextInput` with the enriched adapter output.
- Tests verify the enriched context can be supplied to `QuestFinishOperationPlanService` and produces XP execution sub-plan metadata from `CustomLevelRewardExecutionService` for bonus then faction custom rewards.
- This is still disabled by default and still non-live: Java `PlayerCommonData.setExp` mutation, automatic quest-finish invocation, mail persistence, packet sends, and Java runtime comparison remain pending.
- The new regression documents a current C# staging difference: Java mutates the player level before `PlayerController.onLevelChange` custom rewards run, while C# XP planning still uses an explicit snapshot and does not mutate the player.

## C# State After UOW-1075

- Added `docs/QuestFinishRuntimeInput-Audit.md`.
- The audit maps production inputs needed before `QuestFinishCustomRewardRuntimeSideEffectAdapterService` can safely be called:
  1. active player/account identity;
  2. account creation time;
  3. item templates;
  4. object-id allocation;
  5. received mail time;
  6. XP level mutation snapshot behavior;
  7. quest reward template context;
  8. explicit opt-in execution policy.
- Key finding: `AccountAuthResult.CreationDate` is read from the login server, but `GameServerConnection` does not retain it and `Player` does not expose account creation time. Java `FactionPackService` uses account creation time, not player creation time.
- Key finding: `IDFactory.NextId()` and `runtimeContext.DataManager.StaticData.ItemTemplates` are viable C# sources for future disabled runtime input assembly, but should stay guarded before repository access.
- Key finding: Java mutates level through `PlayerCommonData.setExp` before `PlayerController.onLevelChange` custom rewards; C# still needs a live XP mutation boundary before production custom-reward execution can match that timing.
- No code behavior changed in this unit.

## C# State After UOW-1076

- Added `QuestFinishCustomRewardRuntimeInputAssemblerService`.
- The assembler is a disabled-by-default options factory for future callers of `QuestFinishCustomRewardRuntimeSideEffectAdapterService`; it does not call repositories, execute mail planning, mutate players, or change quest-finish gameplay.
- Disabled input returns `QuestXpCustomRewardRuntimeInputAdapterOptions.Disabled` without requiring runtime dependencies.
- Enabled input refuses to produce executable adapter options when account creation epoch milliseconds, object-id allocation, or item templates are missing.
- The assembler converts login-server account creation epoch milliseconds through `GameServerOptions.Core.GetTimeZone()` and returns a local `DateTime` for faction-pack window comparison, following Java `ServerTime.ofEpochMilli(...).toLocalDateTime()`.
- Tests cover the UTC Asmodian faction-window start and a fixed-offset server timezone conversion.

## C# State After UOW-1077

- Added nullable `Player.AccountCreationEpochMillis`, `PlayerAccountRuntimeStateService`, and `GameServerConnection` propagation from positive login-server `AccountAuthResult.CreationDate` to the active player after enter-world.
- Missing or fake-auth creation time remains `null`, so future custom reward assembly can still treat it as a missing Java dependency instead of using Unix epoch zero.

## C# State After UOW-1078

- Added `QuestFinishCustomRewardSessionRuntimeInputAdapterService`, a disabled-by-default bridge from active player/session-shaped runtime inputs to `QuestFinishCustomRewardRuntimeInputAssemblerService`.
- The adapter carries `Player.AccountCreationEpochMillis`, `IDFactory.NextId`, received time, and item templates into assembler options, but does not execute repositories, mail persistence, level-change side effects, or quest-finish gameplay.
- Tests confirm the disabled gate does not consume ids, enabled input reports missing account creation/id-factory/item-template dependencies, and the created path preserves the Java-shaped account-creation local time.

## C# State After UOW-1079

- Added a regression test that composes session-assembled disabled options through `QuestFinishCustomRewardRuntimeSideEffectAdapterService` and into quest-finish XP execution metadata.
- The test confirms disabled-by-default custom reward input assembly does not call repositories, does not consume object ids, and does not turn custom reward XP sub-plan metadata into live `CustomLevelRewardExecutionService` work.
- This is still a non-live metadata test; production quest-finish and live XP mutation remain disabled.

## Known Gaps

- No Java runtime comparison was generated because local Java tooling is still blocked.
- XP live mutation is not wired into quest finish; UOW-1045 only composes non-live operation metadata.
- `SM_SYSTEM_MESSAGE` XP helper ids and parameter order are ported for the XP reward messages used by `PlayerCommonData.addExp`, and `QuestXpRewardPlan` can now produce ordered non-live packet metadata.
- `SM_STATUPDATE_EXP` is now represented by staged execution metadata, but no packet instance is created or sent from the XP execution plan.
- Level-change hooks are represented as Java-order descriptors, optional non-live sub-plan metadata, a non-live context factory, optional quest-finish XP operation metadata, a custom reward receipt repository prerequisite, non-live system-mail payload plans, an opt-in custom reward execution boundary, XP metadata for explicitly supplied custom execution results, non-live system-mail persistence/fanout operation metadata, a disabled-by-default system-mail persistence execution boundary, a concrete opt-in mail persistence operation executor, Java-shaped system-mail repository command plans, opt-in system-mail repository DB integration coverage, opt-in attached-item failure-ordering DB coverage, opt-in store-letter failure-ordering DB coverage, online system-mail fanout packet-order regression coverage, a disabled-by-default runtime adapter for feeding custom reward execution results into XP level-change context input, disabled-by-default quest-finish side-effect context composition for that adapter output, a runtime input audit for future production assembly, and a disabled runtime input assembler for custom reward adapter options. The level-up animation packet constant, upgrade-player sub-plan, NPC faction level-up sub-plan, QuestEngine level-change callback dispatch sub-plan, nearby quest empty-packet intent, guide HTML sub-plan, skill auto-learn sub-plan, starter-kit sub-plan, and custom bonus/faction reward sub-plan now exist, but stat recalculation, max-stat calculation, live nearby quest refresh, live quest handler execution, live skill mutation/effects/recipe learning, live guide HTML packets/persistence, live starter-kit/custom reward mail sends, live NPC faction mutation, live team/alliance updates, live legion updates, ratio updates, and live animation broadcast remain unported behavior.
- The C# plan uses the current C# `Player.Level` as the previous/display level input. Java derives and updates level through `PlayerCommonData.setExp`; this needs verification before live mutation.
- No-exp state and Daeva/non-Daeva cap are explicit method inputs because equivalent C# player state is not fully modeled.
- Repose and salvation formulas are source-reviewed and unit-tested, but edge cases around negative XP, large XP, unusual float rates, and live max-repose updates still need runtime verification.
- Threading and persistence differ from Java because this helper is non-live and does not perform per-player mutation.

## Tests Added Or Updated

- `QuestRewardServiceTests.CreateXpRewardPlan_AppliesJavaQuestRateReposeAndSalvationWithoutMutatingPlayer`
- `QuestRewardServiceTests.CreateXpRewardPlan_RecordsJavaGuardsAndNonDaevaLevelCap`
- `QuestRewardServiceTests.ApplyQuestXpRate_MatchesJavaFloatRateBoostLegionFallbacksAndOverflow`
- `GameServerOptionsTests.LoadFromJavaConfig_ReadsCoreAndNetworkDefaults`
- `GameServerOptionsTests.LoadFromJavaConfig_AppliesMyGsOverridesLast`
- `QuestFinishOperationPlanServiceTests.CreatePlan_ComposesXpSideEffectPlanAfterMatchingNonItemProjectionWithoutMutatingPlayer`
- `GamePacketTests.SmSystemMessage_WritesDialogTooFarMessages`
- `QuestRewardServiceTests.CreateXpSystemMessagePackets_MapsPlanMessageKindsAndAscensionWarningInJavaOrder`
- `QuestXpExecutionPlanServiceTests.CreatePlan_StagesJavaLevelChangeSideEffectsBeforeStatAndXpPackets`
- `QuestXpExecutionPlanServiceTests.CreatePlan_KeepsNoLevelChangePlanInJavaPacketOrder`
- `QuestXpExecutionPlanServiceTests.CreatePlan_AppendsAscensionWarningAfterXpMessageAndSkipsGuardedPlans`
- `QuestXpExecutionPlanServiceTests.CreatePlan_ComposesLevelChangeSubPlansInJavaOrderWithoutExecutingThem`
- `QuestXpExecutionPlanServiceTests.CreatePlan_DoesNotComposeLevelChangeSubPlansWhenLevelIsUnchanged`
- `QuestXpLevelChangeContextFactoryServiceTests.CreateContext_BuildsJavaLevelChangeSubPlansFromSnapshotInputs`
- `QuestXpLevelChangeContextFactoryServiceTests.CreateContext_RecordsGuardedSubPlansWhenDependenciesAreMissing`
- `QuestFinishOperationPlanServiceTests.CreatePlan_ComposesXpExecutionPlanWithLevelChangeContextMetadata`
- `CustomLevelRewardRepositoryTests.CreateLoad_UsesJavaSelectSqlAndAccountParameter`
- `CustomLevelRewardRepositoryTests.CreateStore_UsesJavaReplaceSqlAndParameterOrder`
- `CustomLevelRewardRepositoryTests.EmptyRepository_ReportsUnavailableJavaSafeFallbacks`
- `SystemMailRewardPlanServiceTests.CreatePlan_ShapesCustomRewardExpressMailWithoutSending`
- `SystemMailRewardPlanServiceTests.CreatePlan_ShapesStarterKitMailAndTruncatesJavaTitleAndMessageLimits`
- `SystemMailRewardPlanServiceTests.CreatePlan_RecordsJavaSystemMailGuardBranches`
- `CustomLevelRewardExecutionServiceTests.CreateBonusPackExecutionPlan_LoadsStoresReceiptThenPlansJavaSystemMail`
- `CustomLevelRewardExecutionServiceTests.CreateBonusPackExecutionPlan_SkipsRepositoryWhenJavaStaticGuardsFail`
- `CustomLevelRewardExecutionServiceTests.CreateBonusPackExecutionPlan_StopsAfterAlreadyReceivedLoad`
- `CustomLevelRewardExecutionServiceTests.CreateFactionPackExecutionPlan_StoresBeforeOppositeRaceFilteringAndPlansDeliverableMail`
- `CustomLevelRewardExecutionServiceTests.CreateFactionPackExecutionPlan_ReportsNoDeliverableRewardsAfterReceiptStore`
- `QuestXpExecutionPlanServiceTests.CreatePlan_ComposesSuppliedCustomRewardExecutionResultMetadata`
- `SystemMailRewardPersistencePlanServiceTests.CreatePlan_StagesOfflineItemMailPersistenceInJavaFailureOrder`
- `SystemMailRewardPersistencePlanServiceTests.CreatePlan_StagesOnlineExpressMailboxFanoutAfterPersistence`
- `SystemMailRewardPersistencePlanServiceTests.CreatePlan_SkipsUnplannedMailAndDoesNotStageDaoWork`
- `SystemMailRewardPersistenceExecutionServiceTests.ExecuteAsync_DisabledGateDoesNotExecutePlannedOperations`
- `SystemMailRewardPersistenceExecutionServiceTests.ExecuteAsync_StopsAfterJavaStoreLetterFailure`
- `SystemMailRewardPersistenceExecutionServiceTests.ExecuteAsync_StopsAfterJavaAttachedItemStoreFailureBeforeMailboxFanout`
- `SystemMailRewardPersistenceExecutionServiceTests.ExecuteAsync_CompletesAllOperationsWhenEnabledAndExecutorSucceeds`
- `SystemMailRewardPersistenceOperationExecutorTests.ExecuteAsync_StoresSystemMailLetterThroughMailRepository`
- `SystemMailRewardPersistenceOperationExecutorTests.ExecuteAsync_StoresSystemMailAttachedItemThroughMailRepository`
- `SystemMailRewardPersistenceOperationExecutorTests.ExecuteAsync_UpdatesOfflineMailboxCounterThroughMailRepository`
- `SystemMailRewardPersistenceOperationExecutorTests.ExecuteAsync_UsesConnectionRegistryForOnlineMailboxFanoutOnlyOnce`
- `SystemMailRepositoryPlanTests.StoreLetter_UsesJavaMailDaoInsertSqlAndParameterOrder`
- `SystemMailRepositoryPlanTests.StoreAttachedItem_UsesJavaInventoryInsertSqlOwnerAndSoulBoundParameterShape`
- `SystemMailRepositoryPlanTests.UpdateOfflineMailboxCounter_UsesJavaMailDaoSqlAndNameParameter`
- `SystemMailRepositoryDatabaseIntegrationTests.StoreSystemMailOperations_WriteLetterItemAndOfflineCounterAgainstJavaSchema_WhenEnabled`
- `SystemMailRepositoryDatabaseIntegrationTests.StoreSystemMailOperations_LeavesLetterAndCounterUnchangedWhenAttachedItemFails_WhenEnabled`
- `SystemMailRepositoryDatabaseIntegrationTests.StoreSystemMailOperations_DoesNotWriteItemOrCounterWhenLetterFails_WhenEnabled`
- `GameClientSocketServerMailFanoutTests.NotifyMailReceivedAsync_SendsMailboxStateListThenPostmanNotifyForOpenExpressMailbox`
- `GameClientSocketServerMailFanoutTests.NotifyMailReceivedAsync_SendsOnlyMailboxStateForClosedNormalMailbox`
- `GameClientSocketServerMailFanoutTests.NotifyMailReceivedAsync_ReturnsFalseWithoutSendingWhenRecipientOffline`
- `QuestXpCustomRewardRuntimeInputAdapterServiceTests.CreateInputAsync_DisabledGateDoesNotExecuteCustomRewardRepositories`
- `QuestXpCustomRewardRuntimeInputAdapterServiceTests.CreateInputAsync_RequiresObjectIdFactoryBeforeRepositoryExecution`
- `QuestXpCustomRewardRuntimeInputAdapterServiceTests.CreateInputAsync_CreatesBonusThenFactionExecutionResultsForXpComposition`
- `QuestFinishCustomRewardRuntimeSideEffectAdapterServiceTests.CreateContextAsync_DisabledGateKeepsQuestFinishContextUnchanged`
- `QuestFinishCustomRewardRuntimeSideEffectAdapterServiceTests.CreateContextAsync_RequiresQuestFinishLevelChangeInputBeforeRepositoryExecution`
- `QuestFinishCustomRewardRuntimeSideEffectAdapterServiceTests.CreateContextAsync_PropagatesInputAdapterDependencyGuardWithoutReplacingContext`
- `QuestFinishCustomRewardRuntimeSideEffectAdapterServiceTests.CreateContextAsync_ComposesExecutionResultsIntoQuestFinishXpPlan`
- `QuestFinishCustomRewardRuntimeInputAssemblerServiceTests.CreateOptions_DisabledGateReturnsDisabledAdapterOptionsWithoutDependencies`
- `QuestFinishCustomRewardRuntimeInputAssemblerServiceTests.CreateOptions_EnabledGateRequiresRuntimeInputsBeforeAdapterExecution`
- `QuestFinishCustomRewardRuntimeInputAssemblerServiceTests.CreateOptions_CreatesAdapterOptionsWithJavaServerTimeEpochMillisConversion`
- `QuestFinishCustomRewardRuntimeInputAssemblerServiceTests.ConvertEpochMillisToServerLocalTime_MatchesJavaServerTimeOfEpochMilliAcrossOffsets`
- `PlayerAccountRuntimeStateServiceTests.ApplyLoginAccountState_CarriesLoginServerCreationMillisToActivePlayer`
- `PlayerAccountRuntimeStateServiceTests.ApplyLoginAccountState_PreservesMissingCreationMillisAsNull`
- `QuestFinishCustomRewardSessionRuntimeInputAdapterServiceTests.CreateOptions_DisabledGateDoesNotRequireSessionDependenciesOrAllocateIds`
- `QuestFinishCustomRewardSessionRuntimeInputAdapterServiceTests.CreateOptions_EnabledGateRequiresPlayerIdFactoryAndStaticItemTemplates`
- `QuestFinishCustomRewardSessionRuntimeInputAdapterServiceTests.CreateOptions_CreatesAssemblerOptionsFromActivePlayerAndRuntimeDependencies`
- `QuestFinishCustomRewardRuntimeSideEffectAdapterServiceTests.CreateContextAsync_SessionAssembledDisabledOptionsKeepQuestFinishXpMetadataNonLive`
- `GamePacketTests.CharacterSelectionServerPackets_WriteJavaShapedPayloads` level-up `SM_ACTION_ANIMATION` assertion
- `PlayerLevelChangeUpgradePlanServiceTests.CreatePlan_StagesJavaUpgradePlayerOrderWithTeamAndLegionDependencies`
- `PlayerLevelChangeUpgradePlanServiceTests.CreatePlan_RecordsMissingMaxStatsDeadAndNoTeamLegionBranches`
- `PlayerNpcFactionsSnapshotTests.LevelUpPlan_DeactivatesOverLevelActiveFactionAndRecordsJavaSideEffects`
- `PlayerNpcFactionsSnapshotTests.LevelUpPlan_RecordsNoChangesMissingTemplateAndNoActiveBranches`
- `QuestLevelChangedCallbackPlanServiceTests.CreatePlan_DispatchesRegisteredRaceCallbacksInJavaOrderAndSkipsCompleteAndMissingHandlers`
- `QuestLevelChangedCallbackPlanServiceTests.CreatePlan_TreatsEveryNonCompleteQuestStateAsDispatchableLikeJava`
- `QuestLevelChangedCallbackPlanServiceTests.CreatePlan_RecordsNoRegisteredNoDispatchAndMissingRegistrationBranches`
- `NearbyQuestRefreshPlanServiceTests.CreatePlan_MatchesJavaEmptyNearbyQuestPacketIntent`
- `GuideHtmlLevelChangePlanServiceTests.CreatePlan_StagesJavaGuideTemplateOrderAndPersistenceIntent`
- `GuideHtmlLevelChangePlanServiceTests.CreatePlan_RecordsJavaConfigSpawnedMissingRangeAndInactiveBranches`
- `SkillAutoLearnPlanServiceTests.CreateAutoLearnPlan_StagesJavaReverseLevelLoopStartingClassBackfillAndDaevaGatheringUpgrade`
- `SkillAutoLearnPlanServiceTests.CreateAutoLearnPlan_RecordsMissingInputsNoChangesAndAlreadyKnownBranches`
- `StarterKitLevelChangePlanServiceTests.CreatePlan_StagesJavaStarterKitLevelBucketsInInclusiveOrder`
- `StarterKitLevelChangePlanServiceTests.CreatePlan_RecordsDisabledEmptyNoMatchAndMissingPlayerBranches`
- `StarterKitLevelChangePlanServiceTests.RewardBuckets_MatchJavaStarterKitStaticItems`
- `CustomLevelRewardPlanServiceTests.CreateBonusPackPlan_StagesJavaBonusPackMailRewards`
- `CustomLevelRewardPlanServiceTests.CreateBonusPackPlan_RecordsJavaGuardBranches`
- `CustomLevelRewardPlanServiceTests.CreateFactionPackPlan_StagesWindowAndOppositeRaceTemplateFiltering`
- `CustomLevelRewardPlanServiceTests.CreateFactionPackPlan_RecordsCreationWindowDaoAndCapacityBranches`

## Next Recommendation

Next, add a guarded production-call-site analysis for where a future socket/quest-finish path could build `QuestFinishRewardSideEffectContext` and session runtime options, without enabling execution. Keep live XP mutation disabled until stat updates, live nearby quest refresh, live quest handler execution, live skill mutation/effects/recipe learning, live guide HTML send/persistence, live starter-kit/custom reward mail sends, live NPC faction mutation, custom reward DAO writes, and persistence behavior are modeled.
