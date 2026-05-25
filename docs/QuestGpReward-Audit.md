# Quest GP Reward Audit - UOW-1040

Date: May 25, 2026

## Java Source

- `game-server/src/com/aionemu/gameserver/services/abyss/GloryPointsService.java#addGp`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/AbyssRank.java#addGp`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/Rates.java#GP`
- `game-server/src/com/aionemu/gameserver/configs/main/RatesConfig.java#GP_RATES`
- `game-server/src/com/aionemu/gameserver/dao/AbyssRankDAO.java#addGp`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java#STR_MSG_GLORY_POINT_GAIN`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java#STR_MSG_GLORY_POINT_LOSE`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_ABYSS_RANK.java`

## Java Behavior Summary

`GloryPointsService.addGp(playerObjId, amount)`:

1. Returns immediately when `amount == 0`.
2. Looks up the online player from `World`.
3. Uses `addToStats = amount > 0`.
4. For offline players, calls `AbyssRankDAO.addGp(playerObjId, amount, addToStats)`.
5. For online players, records old GP, mutates `AbyssRank.addGp`, computes actual delta, sends a GP gain/loss system message based on requested `amount >= 0`, and sends `SM_ABYSS_RANK` only when actual GP changed.

`AbyssRank.addGp(amount, addToStats)`:

- Positive GP adds to current, daily, and weekly GP.
- Negative GP changes current GP only.
- Current GP clamps to zero after mutation.
- Java `int` arithmetic can overflow before the clamp.
- Rank is not recalculated here.
- Persistent state is marked update-required.

`Rates.GP.calcResult`:

- Uses `gameserver.rates.gp.gain`, default `1.0, 2.0`.
- Selects rate by membership, clamped to the last configured rate.
- Empty rate arrays return `1`.
- Java `float` arithmetic is truncated toward zero.
- The `int` overload returns the original input when the rated value cannot fit into `int`.

## C# State After UOW-1040

- Added `GameServerRateOptions.GpRates` and config load for `gameserver.rates.gp.gain`.
- Added `PlayerAbyssRank.AddGp`.
- Added `SmSystemMessage.GloryPointGain` and `SmSystemMessage.GloryPointLose`.
- Added `GloryPointsService` and `GloryPointsAddPlan`.
- Added `QuestRewardService.ApplyGpReward` and `ApplyQuestGpRate`.
- Offline GP updates are represented as `OfflineDaoUpdateRequired` metadata only; no C# DAO write exists yet.
- Quest finish still does not execute live reward mutation.

## Known Gaps

- No Java runtime golden comparison was generated.
- Offline `AbyssRankDAO.addGp` is not implemented as a repository method.
- Missing-row offline DAO behavior is not represented beyond metadata.
- Java daily/weekly rollover through `AbyssRank.doUpdate` and `last_update` is not ported.
- Positive offline GP SQL overflow/sign behavior is database-dependent and unverified.
- Siege and fortress GP callers are not wired; siege GP must remain unrated.
- Quest finish composition still carries GP as metadata only and does not call the live helper.

## Tests Added Or Updated

- `GloryPointsServiceTests.AddGp_AddsPositiveGpDailyWeeklyStatsAndRankPacketLikeJava`
- `GloryPointsServiceTests.AddGp_SubtractsGpClampsAtZeroAndSkipsDailyWeeklyStatsLikeJava`
- `GloryPointsServiceTests.AddGp_StillSendsLossZeroWhenNegativeAmountDoesNotChangeCurrentGp`
- `GloryPointsServiceTests.CreateAddGpPlan_RecordsOfflineDaoBranchAndZeroGuard`
- `GloryPointsServiceTests.AddGp_PreservesJavaIntOverflowShape`
- `QuestRewardServiceTests.ApplyGpReward_AppliesConfiguredGpRateAndAddsGpThroughPlanner`
- `QuestRewardServiceTests.ApplyGpReward_SkipsMissingPlayerAndZeroGpReward`
- `QuestRewardServiceTests.ApplyQuestGpRate_MatchesJavaMembershipFallbacksAndOverflowBehavior`
- `GamePacketTests.SmSystemMessage_WritesDialogTooFarMessages`
- `GameServerOptionsTests.LoadFromJavaConfig_ReadsCoreAndNetworkDefaults`
- `GameServerOptionsTests.LoadFromJavaConfig_AppliesMyGsOverridesLast`

## Next Recommendation

Keep GP live integration out of quest finish until offline DAO, daily/weekly rollover, and reward failure ordering are better bounded. The next small unit can either compose GP helper metadata into quest-finish operation descriptors or audit/scaffold quest XP, which has broader level/stat side effects.
