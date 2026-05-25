# Quest GP Reward Audit - UOW-1040/UOW-1043

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

## C# State After UOW-1041

- Added non-live GP side-effect composition to `QuestFinishOperationPlanService`.
- Added `QuestRewardSideEffectPlanService.CreateGpRewardPlan`, which applies `Rates.GP` and calls `GloryPointsService.CreateAddGpPlan` without mutating the player.
- Added `QuestFinishOperationDescriptor.GpRewardPlan` metadata.
- The GP descriptor is emitted after the matching non-item GP projection and before the coarse non-item reward placeholder.
- Quest finish still does not execute live GP mutation or offline DAO writes.

## C# State After UOW-1042

- Added `IAbyssRankRepository`, `EmptyAbyssRankRepository`, and `MySqlAbyssRankRepository`.
- Added `AbyssRankGpUpdatePlan` to expose Java `AbyssRankDAO.addGp` SQL text and parameter order.
- Positive/stat-modifying GP uses `UPDATE abyss_rank SET gp = gp + ?, daily_gp = daily_gp + ?, weekly_gp = weekly_gp + ? WHERE player_id = ?`.
- Current-GP-only changes use `UPDATE abyss_rank SET gp = GREATEST(gp + ?, 0) WHERE player_id = ?`.
- `MySqlAbyssRankRepository.AddGpAsync` returns true when SQL execution completes, including zero affected rows, matching Java's no row-count check.
- The repository is not yet wired into `GloryPointsService` or quest-finish execution.

## C# State After UOW-1043

- Added `GloryPointsService.ExecuteOfflineDaoUpdateAsync` as an explicitly gated offline GP execution path.
- The method executes only `GloryPointsAddPlan` instances whose `RequiresOfflineDaoUpdate` flag is true.
- It calls `IAbyssRankRepository.AddGpAsync(plan.ObjectId, plan.Amount, plan.AddsDailyWeeklyStats)`, preserving Java `AbyssRankDAO.addGp(playerObjId, amount, addToStats)` arguments.
- Added `GloryPointsOfflineExecutionResult` and `GloryPointsOfflineExecutionStatus` to expose success, repository failure, and non-offline-plan skip behavior.
- Existing `GloryPointsService.AddGp` remains unchanged, so quest finish and other live callers still do not automatically execute offline repository writes.

## Known Gaps

- No Java runtime golden comparison was generated.
- Offline `AbyssRankDAO.addGp` is not implemented as a repository method.
- Missing-row offline DAO behavior is not represented beyond metadata.
- Java daily/weekly rollover through `AbyssRank.doUpdate` and `last_update` is not ported.
- Positive offline GP SQL overflow/sign behavior is database-dependent and unverified.
- Siege and fortress GP callers are not wired; siege GP must remain unrated.
- Quest finish composition carries GP rate/helper metadata only and does not call the live mutating helper.
- Offline repository execution exists behind an explicit service method but is not automatically called by gameplay.

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
- `QuestRewardSideEffectPlanServiceTests.CreateGpRewardPlan_AppliesRateAndPlansPacketsWithoutMutatingPlayer`
- `QuestFinishOperationPlanServiceTests.CreatePlan_ComposesGpSideEffectPlanAfterMatchingNonItemProjectionWithoutMutatingPlayer`
- `AbyssRankRepositoryTests.AbyssRankGpUpdatePlan_UsesJavaPositiveStatsSqlAndParameterOrder`
- `AbyssRankRepositoryTests.AbyssRankGpUpdatePlan_UsesJavaCurrentGpClampSqlWhenStatsAreNotModified`
- `AbyssRankRepositoryTests.EmptyAbyssRankRepository_ReportsUnavailableMutationBoundary`
- `GloryPointsServiceTests.ExecuteOfflineDaoUpdateAsync_ExecutesRepositoryWithJavaOfflineBranchArguments`
- `GloryPointsServiceTests.ExecuteOfflineDaoUpdateAsync_RecordsRepositoryFailureAndSkipsNonOfflinePlans`

## Next Recommendation

Keep GP live integration out of quest finish until daily/weekly rollover is bounded, DB behavior is integration-tested, and reward failure ordering is settled. The next small unit should either add opt-in offline GP integration tests/adapter plumbing or start the quest XP non-live planner described by the XP explorer.
