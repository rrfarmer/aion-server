# Quest XP Reward Audit - UOW-1044/UOW-1048

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
- `game-server/src/com/aionemu/gameserver/questEngine/QuestEngine.java#onLevelChanged`
- `game-server/src/com/aionemu/gameserver/services/SkillLearnService.java#learnNewSkills`

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

## Known Gaps

- No Java runtime comparison was generated because local Java tooling is still blocked.
- XP live mutation is not wired into quest finish; UOW-1045 only composes non-live operation metadata.
- `SM_SYSTEM_MESSAGE` XP helper ids and parameter order are ported for the XP reward messages used by `PlayerCommonData.addExp`, and `QuestXpRewardPlan` can now produce ordered non-live packet metadata.
- `SM_STATUPDATE_EXP` is now represented by staged execution metadata, but no packet instance is created or sent from the XP execution plan.
- Level-change hooks are represented as Java-order descriptors only. The level-up animation packet constant and upgrade-player sub-plan now exist, but stat recalculation, max-stat calculation, nearby quest refresh, quest engine callbacks, skills, guide, starter-kit, NPC faction effects, live team/alliance updates, live legion updates, ratio updates, and live animation broadcast remain unported behavior.
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
- `GamePacketTests.CharacterSelectionServerPackets_WriteJavaShapedPayloads` level-up `SM_ACTION_ANIMATION` assertion
- `PlayerLevelChangeUpgradePlanServiceTests.CreatePlan_StagesJavaUpgradePlayerOrderWithTeamAndLegionDependencies`
- `PlayerLevelChangeUpgradePlanServiceTests.CreatePlan_RecordsMissingMaxStatsDeadAndNoTeamLegionBranches`

## Next Recommendation

Add another focused non-live side-effect sub-plan or audit behind staged XP execution, such as NPC faction level-up behavior or QuestEngine level-change callback dispatch. Keep live XP mutation disabled until stat updates, nearby quest refresh, quest callbacks, skill learning, NPC factions, custom rewards, and persistence behavior are modeled.
