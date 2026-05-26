# Quest Reward Side-Effects Audit

Date: May 25, 2026
Unit of Work: UOW-1034, updated by UOW-1035, UOW-1037, UOW-1038, UOW-1039, UOW-1040, UOW-1041, UOW-1042, UOW-1043, UOW-1044, UOW-1045, UOW-1046, UOW-1047, UOW-1048, UOW-1049, UOW-1050, UOW-1051, UOW-1052, UOW-1053, UOW-1054, UOW-1055, UOW-1056, UOW-1057, UOW-1058, UOW-1059, UOW-1060, UOW-1061, UOW-1062, UOW-1063, and UOW-1064

## Purpose

This audit records Java `QuestService.giveReward` live side effects that must be understood before C# quest-finish reward descriptors can become live mutation.

Java remains the source of truth. This audit is read-only and does not enable live kinah, XP, title, AP, DP, GP, cube, warehouse, packet, or persistence behavior.

## Java Source Breadcrumbs

- `game-server/src/com/aionemu/gameserver/services/QuestService.java#giveReward`
- `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java#increaseKinah`
- `game-server/src/com/aionemu/gameserver/model/items/storage/PlayerStorage.java#increaseKinah`
- `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java#ItemUpdateType`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/Rates.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/PlayerCommonData.java#addExp`
- `game-server/src/com/aionemu/gameserver/services/HTMLService.java#sendGuideHtml`
- `game-server/src/com/aionemu/gameserver/dataholders/GuideHtmlData.java#getTemplatesFor`
- `game-server/src/com/aionemu/gameserver/dao/GuideDAO.java#saveGuide`
- `game-server/src/com/aionemu/gameserver/services/SkillLearnService.java#learnNewSkills`
- `game-server/src/com/aionemu/gameserver/model/skill/PlayerSkillList.java#addSkill`
- `game-server/src/com/aionemu/gameserver/services/reward/StarterKitService.java#onLevelUp`
- `game-server/src/com/aionemu/gameserver/services/BonusPackService.java#addPlayerCustomReward`
- `game-server/src/com/aionemu/gameserver/services/FactionPackService.java#addPlayerCustomReward`
- `game-server/src/com/aionemu/gameserver/dao/BonusPackDAO.java`
- `game-server/src/com/aionemu/gameserver/dao/FactionPackDAO.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/PlayerCommonData.java#addDp`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/title/TitleList.java#addTitle`
- `game-server/src/com/aionemu/gameserver/services/abyss/AbyssPointsService.java#addAp`
- `game-server/src/com/aionemu/gameserver/services/abyss/GloryPointsService.java#addGp`
- `game-server/src/com/aionemu/gameserver/services/CubeExpandService.java#questExpand`
- `game-server/src/com/aionemu/gameserver/services/WarehouseService.java#expand`

## Java Reward Ordering

`QuestService.giveReward` applies non-item rewards in this order:

1. Kinah with `Rates.QUEST_KINAH`, then `Inventory.increaseKinah(..., INC_KINAH_QUEST)`.
2. XP with `Rates.XP_QUEST`, using target NPC l10n when available.
3. Quest title through `TitleList.addTitle(title, true, 0)`.
4. AP through `AbyssPointsService.addAp`; `QuestCategory.NON_COUNT` bypasses `Rates.AP_QUEST`.
5. DP through `PlayerCommonData.addDp`.
6. GP through `GloryPointsService.addGp(playerObjectId, Rates.GP.calcResult(...))`.
7. Cube expansion for `extend_inventory == 1`.
8. Warehouse expansion for `extend_inventory == 2`.

These live side effects occur during quest finish before quest-state completion mutation and before the later `SM_QUEST_ACTION` update packet.

## Kinah And XP

Kinah:

- Java only enters the kinah branch when `Rewards.getKinah() != 0`.
- `Rates.QUEST_KINAH.calcResult(player, rewards.getKinah())` applies membership/server rate with Java `float` precision and truncation.
- `Storage.increaseKinah` creates a zero-count kinah item if missing.
- Positive amounts mutate the kinah item count, send `SM_INVENTORY_UPDATE_ITEM`, and mark item/storage state update-required.
- Quest reward kinah uses `ItemUpdateType.INC_KINAH_QUEST`; agent audit found this packet mask as `0x32`.
- C# UOW-1035 added non-live `QuestRewardService.CreateKinahRewardPlan`, `GameServerRateOptions.QuestKinahRates`, and `SmInventoryUpdateItem.IncreaseKinahQuest = 0x32`.
- The C# helper is intentionally not wired into live quest finish. It plans Java branch behavior for raw zero skip, missing kinah item creation, positive-only count increase, Java `float` rate truncation, and `Item.increaseItemCount` max-stack cap remainder.
- Runtime packet sends, item/storage persistent-state flags, object id generation through the production ID factory, and DAO persistence remain disabled and need live composition work.

XP:

- Java only enters the XP branch when `Rewards.getExp() != 0`.
- Java looks up `DataManager.NPC_DATA.getNpcTemplate(env.getTargetId())` and passes `npcTemplate.getL10n()` or null to `PlayerCommonData.addExp`.
- `addExp` can early-return for no-exp state or special world restrictions, applies `Rates.XP_QUEST`, handles repose/salvation bonus messages, and calls `setExp`.
- `setExp` can level change, update stats, trigger quest/skill/bonus hooks, update nearby quests, and send `SM_STATUPDATE_EXP`.
- C# has `SmStatUpdateExp`, `SmStatsInfo`, `NpcTemplateTable`, `PlayerExperienceTable`, and player models, but no quest XP live helper equivalent to `PlayerCommonData.addExp`.
- UOW-1044 adds `GameServerRateOptions.XpQuestRates`, `QuestRewardService.ApplyQuestXpRate`, and non-live `QuestRewardService.CreateXpRewardPlan`.
- The XP plan records Java guard outcomes, Java `float` rate/boost/legion truncation, repose and salvation bonus metadata, capped resulting XP, display level, max repose energy, ascension-limit message intent, XP message kind, `SM_STATUPDATE_EXP` intent, and level-change side-effect intent.
- UOW-1045 composes the XP plan into quest-finish operation metadata immediately after the XP non-item projection when `QuestFinishRewardSideEffectContext.ExperienceTable` is supplied.
- UOW-1046 adds concrete XP reward `SmSystemMessage` helpers for Java `PlayerCommonData.addExp` message variants and the level-9 ascension-limit warning, with packet serialization regression coverage.
- UOW-1047 adds non-live `QuestRewardService.CreateXpSystemMessagePackets`, which maps XP plan message kinds to the concrete XP packet helpers and appends the ascension-limit warning after the XP gain message.
- UOW-1048 adds non-live `QuestXpExecutionPlanService.CreatePlan`, which expands the coarse level-change intent into Java-order execution descriptors and places `SM_STATUPDATE_EXP`, XP system-message metadata, and optional ascension-warning metadata after those level-change descriptors.
- UOW-1049 adds `SmActionAnimation.LevelUp = 0` with packet serialization regression coverage and updates the XP execution descriptor note to reference the concrete C# constant.
- UOW-1050 adds non-live `PlayerLevelChangeUpgradePlanService.CreatePlan`, which stages Java `PlayerController.upgradePlayer` metadata for life-stat synchronization, visual stats update, team stat update, and legion member update.
- UOW-1051 adds non-live `NpcFactionLevelUpPlanService.CreatePlan`, which stages Java `NpcFactions.onLevelUp` metadata for over-level faction deactivation, START-state quest abandon intent, and `STR_FACTION_LEAVE_BY_LEVEL_LIMIT` message id `1400770`.
- UOW-1052 adds non-live `QuestLevelChangedCallbackPlanService.CreatePlan`, which stages Java `QuestEngine.onLevelChanged` race-scoped registered callback dispatch, COMPLETE-state skips, missing-handler skips, duplicate suppression, and non-COMPLETE dispatch metadata.
- UOW-1053 corrects `NearbyQuestRefreshPlan.WouldSendPacket` so empty nearby quest marker sets still report `SM_NEARBY_QUESTS` packet intent, matching Java `PlayerController.updateNearbyQuests`.
- UOW-1054 adds non-live `GuideHtmlLevelChangePlanService.CreatePlan`, which stages Java guide config/spawn gates, inclusive level iteration, `GuideHtmlData.getTemplatesFor` template ordering, inactive-template skips, and future `SM_QUESTIONNAIRE` plus `GuideDAO.saveGuide` intent.
- UOW-1055 adds non-live `SkillLearnService.CreateAutoLearnPlan`, which stages Java reverse level iteration, starting-class backfill below level 10, autolearn filtering, human-gathering skip for advanced classes, projected skill add/upgrade/remove state, Daeva gathering upgrade, and `onLearnSkill` packet/effect/recipe/nearby-refresh intent.
- UOW-1056 adds non-live `StarterKitLevelChangePlanService.CreatePlan`, which stages Java starter-kit config gating, inclusive level iteration, fixed reward bucket ordering, and future express system-mail intent.
- UOW-1057 adds non-live `CustomLevelRewardPlanService.CreateBonusPackPlan` and `CreateFactionPackPlan`, which stage Java bonus/faction custom reward level-65 gates, mailbox capacity, account DAO load/store outcomes, faction creation windows, opposite-race item-template skips, and future express system-mail intent.
- UOW-1058 adds optional `QuestXpLevelChangeCompositionContext` metadata to `QuestXpExecutionPlanService`, so already-created non-live level-change sub-plans can be attached to XP execution summaries in Java order without executing them.
- UOW-1059 adds non-live `QuestXpLevelChangeContextFactoryService.CreateContext`, which builds the UOW-1058 composition context from a supplied player and explicit runtime/static-data/DAO-result inputs without executing live side effects.
- UOW-1060 composes optional `QuestXpExecutionPlan` metadata into quest-finish XP side-effect descriptors when `QuestFinishRewardSideEffectContext` supplies a `QuestXpLevelChangeContextFactoryInput`.
- UOW-1061 adds `ICustomLevelRewardRepository`, `MySqlCustomLevelRewardRepository`, `EmptyCustomLevelRewardRepository`, and `CustomLevelRewardReceiptRepositoryPlan` for Java `BonusPackDAO` / `FactionPackDAO` load/store receipt SQL.
- UOW-1062 adds `SystemMailRewardPlanService`, which shapes starter/custom reward descriptors into non-live Java-style `PlayerMail` and mailbox-location attachment metadata while applying `SystemMailService.sendMail` guard/truncation rules.
- UOW-1063 adds `CustomLevelRewardExecutionService`, an opt-in boundary that preserves Java bonus/faction custom reward ordering: static guards, receipt DAO load, receipt DAO store, then non-live system-mail payload planning for deliverable rewards.
- UOW-1064 lets XP level-change composition metadata prefer explicitly supplied `CustomLevelRewardExecutionResult` objects for bonus/faction rewards, so opt-in execution-boundary outcomes can be surfaced in Java-order XP execution summaries without invoking them by default.
- UOW-1065 adds `SystemMailRewardPersistencePlanService`, which stages non-live `SystemMailService.sendMail` persistence/fanout metadata from planned system-mail payloads: `MailDAO.storeLetter`, optional `InventoryDAO.store`, offline mailbox counter update, online mailbox insertion, `SM_MAIL_SERVICE`, optional `MailService.sendMailList`, and express `STR_POSTMAN_NOTIFY`.
- UOW-1066 adds `SystemMailRewardPersistenceExecutionService`, a disabled-by-default execution boundary that can run staged mail persistence operations only when an explicit opt-in option and injected executor are supplied.
- UOW-1067 adds `SystemMailRewardPersistenceOperationExecutor` plus system-mail-specific `IMailRepository` methods for `MailDAO.storeLetter`, `InventoryDAO.store`, and offline mailbox counter updates, while delegating online recipient fanout to `IGameClientConnectionRegistry.NotifyMailReceivedAsync`.
- UOW-1068 adds Java-shaped `SystemMailRepositoryPlan` command metadata and routes the system-mail repository methods through it for deterministic SQL/parameter review coverage.
- UOW-1069 adds opt-in `SystemMailRepositoryDatabaseIntegrationTests` for the system-mail letter, attached-item, and offline mailbox-counter repository methods against `game-server/sql/aion_gs.sql`.
- UOW-1070 extends the opt-in system-mail DB integration coverage with an attached-item failure-ordering scenario: the mail row is stored first, duplicate attached-item persistence fails, and the offline mailbox counter remains unchanged when the DB gate is enabled.
- UOW-1071 extends the opt-in system-mail DB integration coverage with a store-letter failure-ordering scenario: duplicate mail persistence fails before attached-item or offline-counter work when the DB gate is enabled.
- UOW-1072 adds online system-mail fanout packet-order regression coverage for `GameClientSocketServer.NotifyMailReceivedAsync`: mailbox state packet, optional open-mailbox list packet, then express postman notification.
- The XP plan intentionally does not mutate player XP/level/repose, send packets, call level-change hooks, update nearby quests, or persist state.

## Title, Cube, And Warehouse

Title:

- Java calls `player.getTitleList().addTitle(title, true, 0)`.
- `TitleList.addTitle` validates title template and race, mutates the in-memory title map, registers expirable data, calls `PlayerTitleListDAO.storeTitles`, sends a quest-title system message, and sends full `SM_TITLE_INFO`.
- Duplicate title sends `STR_TOOLTIP_LEARNED_TITLE` and returns false.
- Invalid title can throw during validation/template lookup.
- C# has `TitleAddService`, `PlayerTitle`, and `SmTitleInfo` for item-title-style support, but no live quest-title grant path or quest-title system message equivalent.
- UOW-1037 adds non-live `QuestRewardSideEffectPlanService.CreateTitleRewardPlan`, which records invalid-title throw intent, owner-null return, race failure plain-text message, duplicate title tooltip intent, successful permanent title creation, expirable registration, immediate persistence, quest-title message intent, and full-title-info intent.
- UOW-1038 composes `CreateTitleRewardPlan` into `QuestFinishOperationPlanService` through `QuestFinishRewardSideEffectContext`, after the matching non-item title projection and before the coarse Java non-item placeholder. The descriptor is metadata only and keeps live title mutation, DAO write, expirable registration, and packet send disabled.
- UOW-1039 adds concrete `SmSystemMessage.QuestGetRewardTitle(titleName)` packet support for Java `STR_QUEST_GET_REWARD_TITLE(String)` message id `1300035`, covered by packet serialization tests. Quest finish still does not send it live.

Cube:

- Java `extend_inventory == 1` calls `CubeExpandService.questExpand(player)`.
- It checks `CustomConfig.CUBE_EXPANSION_LIMIT`, increments `questExpands`, recalculates cube limit, sends inventory-size system message, and sends `SM_CUBE_UPDATE.cubeSize(CUBE, player)`.
- C# has inventory capacity and cube update packet helpers, but no quest cube expansion executor.
- UOW-1037 adds non-live `CreateCubeExpansionPlan`, which records Java `canExpand` boundary behavior, next `QuestExpands`, slot-limit delta, required player persistence, inventory-size message intent, and cube update intent without mutating the player.
- UOW-1038 composes `CreateCubeExpansionPlan` into quest-finish operation metadata after the cube non-item projection and before the coarse Java non-item placeholder. It remains non-live.

Warehouse:

- Java `extend_inventory == 2` calls `WarehouseService.expand(player, false)`.
- It checks fixed `MAX_EXPAND = 11`, increments `whBonusExpands`, recalculates regular warehouse limit, sends warehouse-size system message, and sends regular/account warehouse info packets.
- C# has `StorageExpansionNpcService`, `InventoryExpansionService`, `SmWarehouseInfo`, and persistence fields, but no quest reward warehouse expansion executor.
- UOW-1037 adds non-live `CreateWarehouseExpansionPlan`, which records Java `canExpand` boundary behavior, next `WarehouseBonusExpands`, slot-limit delta, required player persistence, warehouse-size message intent, and regular warehouse info intent without mutating the player.
- UOW-1038 composes `CreateWarehouseExpansionPlan` into quest-finish operation metadata after the warehouse non-item projection and before the coarse Java non-item placeholder. It remains non-live and can carry Java cannot-expand boundary plans.

Other `extend_inventory` values are ignored by Java `giveReward`. C# non-item projection records unsupported values as warning descriptors.

## AP, DP, And GP

AP:

- Java applies `Rates.AP_QUEST` unless the quest category is `NON_COUNT`.
- Java calls `AbyssPointsService.addAp`, which mutates abyss rank, sends AP gain/loss system messages, sends `SM_ABYSS_RANK` when AP/rank changes, broadcasts rank updates on rank change, checks rank-limited gear, refreshes abyss skills, and adds legion contribution for positive actual AP.
- C# has `QuestRewardService.ApplyApReward` and `AbyssPointsService.AddAp`, but quest finish only carries descriptor metadata and does not call the live helper.
- Existing C# AP helper covers quest AP rate and `NON_COUNT` bypass in isolation, but operation-plan/live ordering and persistence remain disabled.

DP:

- Java calls `PlayerCommonData.addDp`, which delegates to `setDp`.
- Starting classes are skipped.
- Online players cap DP to the online max DP.
- Java broadcasts `SM_DP_INFO`, updates stats/speed visually, then sends `SM_STATUPDATE_DP`.
- C# has `QuestRewardService.ApplyDpRewardAsync` and `WorldNpcResourceStatsService.AddPlayerDpAsync`, but quest finish does not call them.
- C# helper is async/packeted; future composition must preserve Java packet ordering.

GP:

- Java calls `GloryPointsService.addGp(playerObjectId, Rates.GP.calcResult(player, rewards.getGp()))`.
- Online players mutate current GP and positive daily/weekly GP, clamp current GP at zero, send GP gain/loss system messages, and send `SM_ABYSS_RANK` when actual GP changes.
- Offline players update directly through `AbyssRankDAO.addGp`.
- UOW-1040 adds `GameServerRateOptions.GpRates`, `GloryPointsService`, `PlayerAbyssRank.AddGp`, GP gain/loss system-message helpers, and `QuestRewardService.ApplyGpReward`.
- UOW-1041 composes a non-live GP side-effect plan into `QuestFinishOperationPlanService` after the matching GP non-item projection and before the coarse non-item placeholder. It applies `Rates.GP` and uses `GloryPointsService.CreateAddGpPlan` without mutating the player.
- UOW-1042 adds an offline GP repository boundary and `AbyssRankGpUpdatePlan` for Java `AbyssRankDAO.addGp` SQL/parameter parity.
- UOW-1043 adds `GloryPointsService.ExecuteOfflineDaoUpdateAsync`, an explicit gated method that executes `GloryPointsAddPlan.OfflineDaoUpdateRequired` through `IAbyssRankRepository` with Java argument ordering.
- Offline `AbyssRankDAO.addGp` still is not wired into quest finish or automatic gameplay; callers must explicitly opt into the gated execution method.

## Persistence And Failure Ordering

- Java reward side effects are not transactional with quest-state completion.
- Kinah and XP mutate player/item state and rely on later deferred persistence.
- Title grant writes through `PlayerTitleListDAO.storeTitles` during reward grant; logged DB failure does not obviously roll back in-memory title or stop quest finish.
- Online AP/DP/GP mutate memory and rely on later persistence paths. Offline GP writes immediately through `AbyssRankDAO`.
- Cube/warehouse expansion mutate player expansion counters and rely on player persistence.
- Packet sends happen during reward mutation before quest-state completion and before the quest update packet.

## Current C# State

- `QuestFinishRewardPlanService.CreateNonItemRewardProjection` records kinah, XP, title, AP, DP, GP, cube, and warehouse metadata only.
- `QuestFinishOperationPlanService` composes these descriptors before quest-state mutation.
- UOW-1038 adds optional `QuestFinishRewardSideEffectContext`; when supplied, title/cube/warehouse non-item reward projections gain adjacent `NonItemRewardSideEffectPlan` descriptors carrying `QuestTitleRewardPlan` or `QuestExpansionRewardPlan`. UOW-1041 extends this to GP through `QuestGpRewardResult`. UOW-1045 extends this to XP through `QuestXpRewardPlan` when an experience table is supplied. The default planner path is unchanged when the context is absent.
- Existing live helpers:
  - `QuestRewardService.ApplyApReward`
  - `QuestRewardService.ApplyDpRewardAsync`
  - `QuestRewardService.ApplyGpReward`
  - `AbyssPointsService.AddAp`
  - `GloryPointsService.AddGp`
  - `GloryPointsService.ExecuteOfflineDaoUpdateAsync`
  - `WorldNpcResourceStatsService.AddPlayerDpAsync`
- Existing repository boundaries:
  - `IAbyssRankRepository.AddGpAsync`
- Missing live homes include:
  - quest title reward helper,
  - quest cube expansion helper,
  - quest warehouse expansion helper,
  - offline quest GP DAO update.
- Existing non-live XP helper:
  - `QuestRewardService.CreateXpRewardPlan`
  - `QuestRewardService.ApplyQuestXpRate`
  - `QuestRewardService.CreateXpSystemMessagePackets`
  - `QuestXpExecutionPlanService.CreatePlan`
- Existing XP level-up packet prerequisite:
  - `SmActionAnimation.LevelUp`
- Existing XP level-change sub-plan prerequisite:
  - `PlayerLevelChangeUpgradePlanService.CreatePlan`
  - `NpcFactionLevelUpPlanService.CreatePlan`
  - `QuestLevelChangedCallbackPlanService.CreatePlan`
  - `GuideHtmlLevelChangePlanService.CreatePlan`
  - `SkillLearnService.CreateAutoLearnPlan`
  - `StarterKitLevelChangePlanService.CreatePlan`
  - `CustomLevelRewardPlanService.CreateBonusPackPlan`
  - `CustomLevelRewardPlanService.CreateFactionPackPlan`
  - `QuestXpLevelChangeCompositionContext`
  - `QuestXpLevelChangeContextFactoryService.CreateContext`
  - `QuestXpExecutionPlan` on quest-finish XP side-effect descriptors
  - `CustomLevelRewardReceiptRepositoryPlan`
  - `SystemMailRewardPlanService.CreatePlan`
  - `CustomLevelRewardExecutionService`
  - `QuestXpLevelChangeCompositionContext.BonusPackExecutionResult`
  - `QuestXpLevelChangeCompositionContext.FactionPackExecutionResult`
  - `SystemMailRewardPersistencePlanService.CreatePlan`
  - `SystemMailRewardPersistenceExecutionService.ExecuteAsync`
  - `SystemMailRewardPersistenceOperationExecutor.ExecuteAsync`
  - `SystemMailRepositoryPlan`
  - `SystemMailRepositoryDatabaseIntegrationTests`
  - `GameClientSocketServerMailFanoutTests`
- UOW-1035 staged a non-composed quest kinah planner on `QuestRewardService`; quest finish still does not execute it.
- UOW-1037 staged title/cube/warehouse reward planners on `QuestRewardSideEffectPlanService`; UOW-1038 composes them into quest-finish metadata but still does not execute them.
- UOW-1040 adds a quest GP helper and live online GP planner/mutator; UOW-1041 composes non-live GP metadata into quest finish but still does not execute it.

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- Precision and overflow for Java `Rates.XP_QUEST` are now source-reviewed and unit-tested in the non-live helper, but lack Java runtime comparison and live quest-finish composition. `Rates.GP` is source-reviewed and unit-tested for membership fallback and int-overflow fallback, but lacks Java runtime comparison.
- `Rates.QUEST_KINAH` precision/truncation is unit-tested in C# from source-reviewed Java behavior, but it still lacks Java runtime comparison.
- C# AP rate helper intentionally omits Java overflow logging.
- Packet masks and packet ordering are incomplete for quest kinah, cube expansion, warehouse expansion, and XP. XP now has concrete reward system-message helpers and a non-live plan-to-packet bridge, but no live quest-finish send. Quest title and GP have concrete system-message helpers, but no live quest-finish send or Java golden-byte comparison.
- C# quest finish still does not execute any reward mutation.
- Title/cube/warehouse planners are now visible in quest-finish operation metadata when a side-effect context is supplied, but remain metadata only; quest title DAO writes, expirable registration, cube update sends, warehouse info sends, and player expansion counter persistence are not live.
- GP planner metadata is now visible in quest-finish operation metadata when a side-effect context is supplied, but live GP mutation, offline DAO writes, and deferred persistence are not live by default.
- XP planner metadata is visible in quest-finish operation metadata when an experience table is supplied, and UOW-1048 can stage the Java live level-up order as descriptors. UOW-1050 now has a non-live `upgradePlayer` sub-plan for life-stat sync, visual stats, team, and legion metadata. UOW-1051 adds a non-live NPC faction level-up sub-plan for over-level faction deactivation, abandon intent, and system-message metadata. UOW-1052 adds a non-live QuestEngine level-change callback dispatch plan. UOW-1053 aligns nearby quest refresh packet intent for empty marker sets. UOW-1054 adds a non-live guide HTML level-change plan for config/spawn gates, template ordering, inactive skips, questionnaire-send intent, and guide persistence intent. UOW-1055 adds a non-live skill auto-learn plan for reverse level iteration, starting-class backfill, Daeva gathering conversion, and skill packet/effect/recipe side-effect intent. UOW-1056 adds a non-live starter-kit level-change plan for fixed reward buckets and express system-mail intent. UOW-1057 adds non-live custom bonus/faction reward planning for DAO gates, account creation windows, item-template race skips, and express system-mail intent. UOW-1058 lets XP execution summaries compose those already-created sub-plan statuses/counts in Java order when the XP plan changes level. UOW-1059 adds a non-live factory for building that composition context from explicit snapshot inputs. UOW-1060 composes the resulting XP execution metadata into quest-finish side-effect descriptors behind explicit inputs. UOW-1061 adds concrete custom reward receipt repository SQL for the bonus/faction one-per-account gates. UOW-1062 adds non-live system-mail payload planning for starter/custom reward descriptors. UOW-1063 bridges those into an opt-in custom reward execution boundary. UOW-1064 surfaces supplied execution-boundary results in XP level-change sub-plan metadata. UOW-1065 stages system-mail persistence/fanout operation metadata. UOW-1066 adds a disabled-by-default execution boundary for those operations. UOW-1067 adds a concrete opt-in operation executor and repository methods. UOW-1068 adds Java-shaped repository command metadata for deterministic SQL/parameter tests. UOW-1069 adds opt-in DB integration coverage for those repository methods. UOW-1070 adds opt-in attached-item failure-ordering DB coverage. UOW-1071 adds opt-in store-letter failure-ordering DB coverage. UOW-1072 adds online mailbox fanout packet-order regression coverage, but default live XP/level-change paths still do not invoke these boundaries and automatic reward mail delivery remains disabled. UOW-1046 read-only analysis confirmed the Java live level-up path runs before quest-state completion and includes visual stats, level-up animation, NPC faction level-up, quest level-change callbacks, nearby refresh, guide HTML, skill auto-learn, custom rewards, starter kit, `SM_STATUPDATE_EXP`, XP gain message, and optional ascension-limit warning.
- Offline GP repository SQL and the gated execution call are tested in isolation, but no quest-finish gameplay path invokes them yet.
- Live reward mutation needs an explicit failure-ordering policy before composition.
- Threading assumptions differ: Java mutates live player state directly; C# must preserve per-player execution order once live execution is enabled.

## Recommended Next Units

1. Add a disabled-by-default runtime adapter that gathers custom reward execution inputs for XP level-change composition, or run the opt-in system-mail DB integration suite against a live MySQL schema when available.
2. Add opt-in integration tests/adapter plumbing for the gated offline GP execution path before enabling siege/offline GP callers.
3. Compose AP/DP/Kinah side-effect metadata only if it remains non-live and preserves Java reward ordering.
