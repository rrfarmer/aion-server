# Quest Finish Reward and Work-Item Audit

Date: May 25, 2026
Unit of Work: UOW-1017; updated by UOW-1018, UOW-1019, and UOW-1030

## Purpose

This audit records Java reward, challenge-task, and quest-work-item behavior inside `QuestService.finishQuest`.

Java remains the source of truth. This unit is documentation-only and does not implement C# reward mutation, inventory changes, challenge-task side effects, work-item removal, packets, DAO writes, or live nearby refresh.

## Java Source Breadcrumbs

- `game-server/src/com/aionemu/gameserver/services/QuestService.java`
- `game-server/src/com/aionemu/gameserver/model/templates/quest/Rewards.java`
- `game-server/src/com/aionemu/gameserver/model/templates/quest/QuestItems.java`
- `game-server/src/com/aionemu/gameserver/model/templates/quest/QuestWorkItems.java`
- `game-server/src/com/aionemu/gameserver/model/DialogAction.java`

## Finish-Quest Reward Ordering

Inside `QuestService.finishQuest`, reward and work-item side effects occur before quest-state completion:

1. `validateAndFixRewardGroup(qs, id)`.
2. Build an initially empty `questItems` list.
3. If extended rewards exist and this is the last repeat before `rewardRepeatCount`, append extended reward items.
4. If regular rewards or bonus exist:
   - append regular reward items for the selected reward group,
   - set the `rewards` object to the selected regular reward group when `rewardGroup != null`.
5. For every collected `QuestItems`, call `ItemService.addItem(player, itemId, count, true)`.
6. Call `giveReward(env, rewards)`.
7. Call `giveReward(env, extendedRewards)`.
8. If category is `CHALLENGE_TASK`, call `ChallengeTaskService.onChallengeQuestFinish(player, id)`.
9. Call `removeQuestWorkItems(player, qs)`.
10. Mutate quest state to `COMPLETE`.

## Reward Group Validation

`validateAndFixRewardGroup` only acts when the quest state exists and status is `REWARD`.

Behavior:

- If `rewardGroup` is non-null but quest data has no rewards, logs and clears reward group.
- If `rewardGroup` is out of range, logs and sets it to the last reward group index.
- If `rewardGroup` is null and rewards exist:
  - logs if more than one reward group exists,
  - sets reward group to `0`.

This mutation marks Java `QuestState` update-required through `setRewardGroup`.

## Item Reward Selection

`getRewardItems` returns a mutable list of `QuestItems`.

Extended rewards:

- Always include `extendedRewards.reward_item`.
- If dialog action is `SELECTED_QUEST_NOREWARD` and selectable extended rewards exist, use `extendedRewardIndex`.
- It first tries `extendedRewardIndex - 8`, then `extendedRewardIndex - 1`.
- If neither index is valid, logs a warning and adds no selectable extended reward.

Regular rewards:

- If `rewardGroup != null`, use `template.getRewards().get(rewardGroup)`.
- Always include that group's fixed `reward_item` entries.
- `DialogAction.SELECTED_QUEST_REWARD1` through `SELECTED_QUEST_REWARD15` map to selectable reward indexes `0` through `14`.
- On last repeat with `singleTimeClassReward`, or on every repeat with `classRewardOnEveryRepeat`, selectable rewards come from class-specific reward data.
- Otherwise selectable rewards come from the selected reward group's `selectable_reward_item` list.
- `SELECTED_QUEST_NOREWARD` can still select class reward data using `extendedRewardIndex - 8`.

Bonus rewards:

- If the template has a bonus, Java calls `QuestEngine.onBonusApplyEvent`.
- If the handler result is not `FAILED`, Java calls `BonusService.getQuestBonus`.
- A non-null bonus item is appended to `questItems`.

UOW-1030 adds a non-live C# reward item projection scaffold for the source-reviewed `getRewardItems` branches:

- `QuestFinishRewardItemTemplateProjection.RewardGroups` projects Java `QuestTemplate.getRewards()`.
- `QuestFinishRewardGroupProjection.FixedRewardItems` projects Java `Rewards.getRewardItem()`.
- `QuestFinishRewardGroupProjection.SelectableRewardItems` projects Java `Rewards.getSelectableRewardItem()`.
- `QuestFinishRewardItemTemplateProjection.ExtendedRewards` projects Java `QuestTemplate.getExtendedRewards()`.
- `QuestFinishRewardItemTemplateProjection.ClassSelectableRewards` projects Java `QuestTemplate.getSelectableRewardByClass(PlayerClass)`.
- `QuestFinishRewardItemProjectionInput.DialogActionId` uses the Java dialog action constants directly: `SELECTED_QUEST_REWARD1` is `8`, `SELECTED_QUEST_REWARD15` is `22`, and `SELECTED_QUEST_NOREWARD` is `23`.
- Projection descriptors are metadata only and keep `IsLive = false`; they do not call `ItemService.addItem`.
- Bonus rewards are represented as an explicit `BonusHandlerNotProjected` warning because Java delegates that behavior to handlers and `BonusService`.

Known UOW-1030 item projection limits:

- No JAXB/XML quest reward loading is wired.
- Player class is represented as a projected string key rather than the Java `PlayerClass` enum.
- Java warning/log side effects are represented as warning descriptors, not log output.
- No inventory mutation, stack handling, overflow handling, packets, or persistence is executed.
- No Java runtime comparison artifact exists; behavior is source-reviewed and unit-tested in C# only.

## Non-Item Rewards

`giveReward` applies these side effects:

- Kinah through `player.getInventory().increaseKinah(Rates.QUEST_KINAH.calcResult(...), INC_KINAH_QUEST)`.
- Experience through `player.getCommonData().addExp(rewards.getExp(), Rates.XP_QUEST, npcL10nOrNull)`.
- Title through `player.getTitleList().addTitle(title, true, 0)`.
- Abyss points through `AbyssPointsService.addAp`; AP is rate-scaled unless quest category is `NON_COUNT`.
- Divine points through `player.getCommonData().addDp`.
- Glory points through `GloryPointsService.addGp(playerObjectId, Rates.GP.calcResult(...))`.
- Cube expansion when `extend_inventory == 1`.
- Warehouse expansion when `extend_inventory == 2`.

`Rewards.extend_stigma`, `ccheck`, and `icheck` exist on the XML model but are not applied by this `giveReward` method.

## Quest Work Items

`removeQuestWorkItems`:

- Loads `QuestWorkItems` from the quest template.
- For each `quest_work_item`, checks the player's current inventory count by item id.
- If count is greater than zero, removes the entire owned count with `player.getInventory().decreaseByItemId(itemId, count, qs.getStatus())`.

Important parity note: Java removes all owned items with the quest work item id, not the `QuestItems.count` value from XML.

Because `finishQuest` calls `removeQuestWorkItems` before setting status to `COMPLETE`, the status passed to inventory decrease is still the pre-completion status, normally `REWARD`.

## Current C# State

- `QuestFinishOperationPlanService` has reward and work-item placeholder descriptors.
- UOW-1018 adds `QuestFinishRewardPlanService`, a pure non-live planner for reward-group correction, item reward placeholders, non-item reward placeholders, challenge-task notification placeholders, and quest work-item removal descriptors.
- `QuestFinishRewardPlanService.CorrectRewardGroup` preserves the Java reward-state guard, reward-group defaulting, out-of-range clamping, and the odd empty-list behavior where `rewardGroups.size() - 1` becomes `-1`.
- The C# reward plan intentionally exposes a nullable reward-group count so the source-audited Java null branch can be represented, even though `QuestTemplate.getRewards()` normally returns `Collections.emptyList()` when XML rewards are absent.
- UOW-1019 composes the reward plan into `QuestFinishOperationPlanService` through an optional projection so reward correction and detailed reward/work-item descriptors occur before staged quest-state mutation.
- UOW-1030 adds `QuestFinishRewardPlanService.CreateRewardItemProjection`, a pure non-live item reward projection for extended fixed/selectable rewards, regular fixed/selectable rewards, class-specific selectable rewards, Java dialog reward-index mapping, and explicit bonus-handler projection warnings.
- C# has inventory, AP, title, exp, DP, GP, cube, and warehouse-related surfaces in various partial states, but no composed quest-finish reward mutation plan.
- C# quest-finish state mutation currently starts at status/var/repeat updates and composes reward/work-item descriptors only as non-live operation metadata.
- C# does not parse real quest XML `Rewards`, `QuestItems`, extended reward data, class-specific selectable rewards, or quest work items for quest finish.

## Recommended Implementation Slices

1. Add staged DTOs or projection records for full quest-finish reward groups and quest work items, sourced from existing/static quest template loading only after XML fields are available.
2. Expand item reward planning to distinguish fixed, selectable, extended, class-specific, and bonus item descriptors.
3. Defer live inventory/AP/XP/title/cube/warehouse mutation until each side effect has a tested C# home.
4. Audit callback and persistence dependencies before live quest-finish execution.

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- Reward selection depends on dialog action ids, extended reward indexes, repeat count, class-specific reward data, bonus handlers, and static quest template data not fully loaded in C#.
- Work-item removal removes all owned matching item ids, which can be surprising if a player has extra copies.
- Non-item reward side effects fan out to several partially ported systems.
- Java logging/warning behavior for malformed reward selection is not modeled.
- Java reward mutation is not transactional with later quest state persistence.
- UOW-1018 descriptors do not prove full parity for reward item selection, class-specific rewards, bonus handlers, live inventory mutation, or runtime packet/callback ordering.
- UOW-1019 operation-plan composition is still non-live and does not parse real quest XML reward data.
- UOW-1030 item projection is source-reviewed and unit-tested but not composed into live quest finish, not backed by real XML loading, and not Java runtime-verified.
