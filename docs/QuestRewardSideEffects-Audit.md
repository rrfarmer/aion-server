# Quest Reward Side-Effects Audit

Date: May 25, 2026
Unit of Work: UOW-1034, updated by UOW-1035, UOW-1037, UOW-1038, UOW-1039, and UOW-1040

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
- C# currently carries XP rate and NPC l10n lookup as non-live metadata only.

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
- Offline `AbyssRankDAO.addGp` remains metadata only through `GloryPointsAddPlan.OfflineDaoUpdateRequired`; no C# repository write exists.

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
- UOW-1038 adds optional `QuestFinishRewardSideEffectContext`; when supplied, title/cube/warehouse non-item reward projections gain adjacent `NonItemRewardSideEffectPlan` descriptors carrying `QuestTitleRewardPlan` or `QuestExpansionRewardPlan`. The default planner path is unchanged when the context is absent.
- Existing live helpers:
  - `QuestRewardService.ApplyApReward`
  - `QuestRewardService.ApplyDpRewardAsync`
  - `QuestRewardService.ApplyGpReward`
  - `AbyssPointsService.AddAp`
  - `GloryPointsService.AddGp`
  - `WorldNpcResourceStatsService.AddPlayerDpAsync`
- Missing live homes include:
  - quest XP helper,
  - quest title reward helper,
  - quest cube expansion helper,
  - quest warehouse expansion helper,
  - offline quest GP DAO update.
- UOW-1035 staged a non-composed quest kinah planner on `QuestRewardService`; quest finish still does not execute it.
- UOW-1037 staged title/cube/warehouse reward planners on `QuestRewardSideEffectPlanService`; UOW-1038 composes them into quest-finish metadata but still does not execute them.
- UOW-1040 adds a quest GP helper and live online GP planner/mutator, but quest finish still does not execute it.

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- Precision and overflow for Java `Rates.XP_QUEST` are not ported. `Rates.GP` is source-reviewed and unit-tested for membership fallback and int-overflow fallback, but lacks Java runtime comparison.
- `Rates.QUEST_KINAH` precision/truncation is unit-tested in C# from source-reviewed Java behavior, but it still lacks Java runtime comparison.
- C# AP rate helper intentionally omits Java overflow logging.
- Packet masks and packet ordering are incomplete for quest kinah, cube expansion, warehouse expansion, and XP. Quest title and GP now have concrete system-message helpers, but no live quest-finish send or Java golden-byte comparison.
- C# quest finish still does not execute any reward mutation.
- Title/cube/warehouse planners are now visible in quest-finish operation metadata when a side-effect context is supplied, but remain metadata only; quest title DAO writes, expirable registration, cube update sends, warehouse info sends, and player expansion counter persistence are not live.
- Live reward mutation needs an explicit failure-ordering policy before composition.
- Threading assumptions differ: Java mutates live player state directly; C# must preserve per-player execution order once live execution is enabled.

## Recommended Next Units

1. Compose GP helper metadata into quest-finish operation descriptors only if it remains non-live and preserves Java reward ordering.
2. Add the offline GP DAO update plan/repository boundary before enabling siege/offline GP callers.
3. Add a quest XP helper design or scaffold only after documenting level-up/stat/nearby-refresh side effects.
