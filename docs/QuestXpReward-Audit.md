# Quest XP Reward Audit - UOW-1044/UOW-1045

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

## Known Gaps

- No Java runtime comparison was generated because local Java tooling is still blocked.
- XP live mutation is not wired into quest finish; UOW-1045 only composes non-live operation metadata.
- `SM_SYSTEM_MESSAGE` XP helper ids and parameter order are not concretely ported in this unit; only message-kind metadata exists.
- `SM_STATUPDATE_EXP` is represented as a packet intent only; no live send is performed.
- Level-change hooks are represented as a coarse `LevelChangeSideEffects` intent only. Stat recalculation, nearby quest refresh, quest engine callbacks, skills, guide, starter-kit, and NPC faction effects remain unported.
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

## Next Recommendation

Port concrete XP system-message helpers and/or deepen the level-change side-effect audit before enabling live XP execution. Keep live XP mutation disabled until message ids, packet ordering, stat updates, nearby quest refresh, quest callbacks, skill learning, and persistence behavior are modeled.
