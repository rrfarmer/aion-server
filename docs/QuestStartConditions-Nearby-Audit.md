# Nearby Quest Start Conditions Audit

Date: May 25, 2026
Unit of Work: UOW-991

## Purpose

This audit records the Java predicate that filters world-instance quest ids before `PlayerController.updateNearbyQuests()` sends `SM_NEARBY_QUESTS`.

Java remains the source of truth. This document does not implement a C# predicate, does not enable packet sends, and does not claim runtime parity.

## Java Source Breadcrumbs

- `game-server/src/com/aionemu/gameserver/controllers/PlayerController.java`
- `game-server/src/com/aionemu/gameserver/services/QuestService.java`
- `game-server/src/com/aionemu/gameserver/model/templates/QuestTemplate.java`
- `game-server/src/com/aionemu/gameserver/model/templates/quest/XMLStartCondition.java`
- `game-server/src/com/aionemu/gameserver/dataholders/QuestsData.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/QuestStateList.java`
- `game-server/src/com/aionemu/gameserver/questEngine/model/QuestState.java`
- `game-server/src/com/aionemu/gameserver/questEngine/model/QuestStatus.java`

## C# Source Breadcrumbs

- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/PlayerQuestState.cs`
- `dotnetConversion/src/Aion.GameServer/Services/NearbyQuestCandidateProjectionService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmNearbyQuests.cs`
- Future C# quest-template/start-condition dataholders and nearby predicate service

## Nearby Call Shape

Java `PlayerController.updateNearbyQuests()` iterates the owning `WorldMapInstance.getQuestIds()` set and calls:

```java
QuestService.checkStartConditions(player, questId, false, 2, false, false, false)
```

For every quest that passes, Java stores:

```java
questId -> QuestService.getLevelRequirementDiff(questId, player.getCommonData().getLevel())
```

Then it sends `SM_NEARBY_QUESTS(nearbyQuestList)`.

## Predicate Gates

For the nearby-UI call, `warn = false`, `allowedDiffToMinLevel = 2`, and all skip flags are false.

Java gate order in `QuestService.checkStartConditions`:

1. Existing quest state:
   - Blocks when state is `START` or `REWARD`.
   - Blocks completed quests when `QuestState.canRepeat()` is false.
   - Uses `QuestTemplate.maxRepeatCount` and `QuestState.completeCount` to distinguish max-repeat messaging from non-repeat messaging when `warn` is true.
2. Quest template lookup:
   - Loads `QuestTemplate` through `DataManager.QUEST_DATA.getQuestById(questId)`.
   - Missing template falls into the catch/log path and returns false.
3. Race:
   - Blocks when `race_permitted` is neither `PC_ALL` nor the player's race.
4. Minimum level:
   - Calculates `template.minlevel_permitted - allowedDiffToMinLevel - player.level`.
   - Nearby UI passes `2`, so quests up to two levels above the player can pass and later receive a positive level-difference marker.
5. Maximum level:
   - Blocks when `maxlevel_permitted != 0` and player level is above it.
6. Class:
   - Blocks when `class_permitted` is non-empty and does not contain the player's class.
7. Gender:
   - Blocks when `gender_permitted` is present and does not match.
8. Abyss rank:
   - Blocks when `rank` is nonzero and the player's abyss rank id is below it.
9. XML start conditions:
   - Counts how many `XMLStartCondition.check(player, warn)` calls pass.
   - Requires the count to be at least `QuestTemplate.getRequiredConditionCount()`.
   - `getRequiredConditionCount` combines mandatory and optional condition rows, with a master-crafting adjustment through `CraftConfig.MAX_MASTER_CRAFTING_SKILLS`.
10. Inventory items:
    - `inventoryItemCheck` requires each `<inventory_item>` to exist in inventory.
    - Counts are not checked here; Java comments state counts are usually 1 and collect-item checks cover larger quantities.
11. Combine skill:
    - `checkCombineSkill` enforces `combineskill` and `combine_skillpoint`.
    - `combineskill = -1` means any modeled crafting/tapping skill, with NPC faction 12/13 excluding essence/aether tapping.
    - `QuestCategory.TASK` has an extra upper-bound skip when `skillLevel - 40 > combine_skillpoint`.
12. NPC faction:
    - For `npcfaction_id != 0`, blocks non-time-based quests until `player.getNpcFactions().canStartQuest(template)` passes.
    - Also requires the faction object to exist and be active.

Any exception logs and returns false.

## XMLStartCondition Details

`XMLStartCondition.check` requires all of these subchecks to pass:

- `finished`: each listed quest must be `COMPLETE`; if a reward group is specified, the completed reward group must match. Repeatable finished quests must be completed to max repeat count unless max is 255.
- `unfinished`: each listed quest must not be complete.
- `acquired`: each listed quest must exist and must not be `LOCKED`.
- `noacquired`: each listed quest must not be `START` or `REWARD`.
- `equipped`: only checked when `warn` is true. Nearby UI passes `warn = false`, so equipped-item preconditions do not block the nearby marker path.
- `required_title`: blocks when the player's displayed title id differs.

## Level Difference Marker

`QuestService.getLevelRequirementDiff(questId, playerLevel)` returns:

```java
template == null ? 99 : template.getMinlevelPermitted() - playerLevel
```

`SM_NEARBY_QUESTS` sets bit `1 << 17` on the quest id when this value is positive. Because the nearby predicate allows a two-level grace below min level, positive values are expected for near-available grey markers.

## Current C# Gaps

- C# has `PlayerQuestState` status/complete-count storage, but no `QuestState.canRepeat()` equivalent because quest repeat metadata and repeat timing are not ported.
- C# does not have a ported `QuestTemplate` dataholder for start-condition fields such as race, class, gender, min/max level, rank, inventory items, XML start conditions, combine skill, NPC faction, repeat count, or category.
- C# does not have `XMLStartCondition` predicate logic.
- C# does not have the NPC faction quest state model needed by this predicate.
- C# does not have combine-skill lookup parity for this quest path.
- C# has `SmNearbyQuests` packet serialization and staged world quest-id projection, but no nearby predicate service, level-diff projector, player-controller refresh method, or packet send path.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.QuestService.checkStartConditions` | Future C# nearby quest start-condition service | Service / Quest Predicate | Not Started | Manual Only | Needs Verification | Source-audited for the nearby UI call shape with `allowedDiffToMinLevel = 2`. No C# implementation exists yet. Missing quest template data, repeat semantics, XML conditions, inventory preconditions, combine-skill checks, NPC faction checks, exception/log behavior, and warning packet behavior. |
| `com.aionemu.gameserver.services.QuestService.getLevelRequirementDiff` | Future C# level-diff projector | Utility / Quest Predicate | Not Started | Manual Only | Needs Verification | Source-audited. Returns `minlevel_permitted - playerLevel`, or `99` for missing templates. Needed to set the `SmNearbyQuests` positive-diff marker bit. |
| `com.aionemu.gameserver.model.templates.QuestTemplate` | Future C# quest template dataholder | Dataholder / DTO | Not Started | No Tests | Unknown | C# lacks the start-condition fields consumed by nearby filtering. Serialization/JAXB defaults, enum mapping, optional condition counting, and master-crafting adjustment remain unported. |
| `com.aionemu.gameserver.model.templates.quest.XMLStartCondition` | Future C# XML start-condition predicate | Dataholder / Predicate | Not Started | No Tests | Unknown | Finished/unfinished/acquired/noacquired/title checks are unported. Equipped-item checks intentionally do not affect nearby UI when `warn = false`, but this needs test coverage once implemented. |
| `com.aionemu.gameserver.model.gameobjects.player.QuestStateList`; `com.aionemu.gameserver.questEngine.model.QuestState` | `Aion.GameServer.Model.GameObjects.PlayerQuestState` | Player Quest State | Partial | Unit Tested elsewhere | Needs Verification | C# stores status, vars, flags, and complete count for packet serialization, but repeatability and next-repeat timing needed by `QuestState.canRepeat()` are not ported. |

## Tests Added/Updated

No tests were added in this audit-only unit. Existing relevant tests remain:

- `WorldMapRuntimeStateTests.NearbyQuestCandidateProjectionService_RegistersNpcStartQuestIdsLikeJavaWorldMapInstance`
- `QuestNpcStartRegistrationSourceRealDataAuditTests.RealDataAudit_ProjectsStagedQuestIdsIntoWorldInstanceWithoutRefreshWiring`
- `GamePacketTests.ServerPacketPayloads_MatchJavaShapes` (`SmNearbyQuests` cases)

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- No C# predicate exists for nearby `checkStartConditions`.
- Quest template static data for start-condition fields is not ported.
- Repeatability, repeat reset timing, and `QuestState.canRepeat()` are not modeled.
- XML start-condition semantics are source-audited only.
- NPC faction, combine-skill, inventory, abyss-rank, title, class/race/gender enum mapping, and exception/log behavior need C# homes before runtime candidate filtering can be claimed.
- Packet sends and production ItemPurification dispatch must remain disabled.

## Summary Metrics

- Total Java artifacts discovered: 5 in this unit
- Total artifacts ported: 0 in this audit-only unit
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5
- Total blocked artifacts: 4 blocked/not-started categories, including quest template data, XML start conditions, repeatability, and NPC faction/combine-skill dependencies
- Estimated overall migration completion: Phase 6 remains about 70% complete; this unit clarifies the next predicate blocker without enabling live nearby quest refresh.

## Next Recommended Unit Of Work

Add a staged quest-template/start-condition data boundary for nearby filtering, starting with the fields used by level and race/class/gender/rank gates plus `getLevelRequirementDiff`. Keep XML start conditions, NPC faction, combine skill, packet sends, and production ItemPurification dispatch disabled until each dependency has tests.
