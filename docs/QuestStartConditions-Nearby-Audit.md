# Nearby Quest Start Conditions Audit

Date: May 25, 2026
Unit of Work: UOW-991, updated by UOW-992 through UOW-994

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
- `dotnetConversion/src/Aion.GameServer/Dataholders/NearbyQuestTemplateTable.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/NearbyQuestTemplateXmlExtractor.cs`
- `dotnetConversion/src/Aion.GameServer/Services/NearbyQuestStartConditionService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/NearbyQuestTemplateXmlExtractorTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/NearbyQuestStartConditionServiceTests.cs`
- Future production C# quest-template/start-condition dataholders and full nearby predicate service

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

- UOW-992 adds a staged `NearbyQuestTemplateTable` and `NearbyQuestTemplateSummary` boundary for the subset of fields needed by early nearby predicate gates and level-diff calculation. It is not wired into production `StaticData` or `DataManager`.
- UOW-993 adds `NearbyQuestTemplateXmlExtractor`, a staged XML extractor for `quest` attributes/elements used by `NearbyQuestTemplateSummary`. It is not wired into production `StaticData` or `DataManager`.
- UOW-994 adds a real-data audit over `game-server/data/static_data/quest_data/quest_data.xml`, pinning 8043 staged quest-template summaries and dependency-flag counts before production integration.
- UOW-992 adds `NearbyQuestStartConditionService.CheckNearbyStartConditions` for the nearby UI call shape. It handles missing templates, active/reward quest state, conservative repeat-count checks, race, min/max level with Java's two-level nearby grace, class, gender, abyss rank, and explicit unsupported-dependency failures.
- UOW-992 adds `NearbyQuestStartConditionService.GetLevelRequirementDiff`, matching Java's missing-template `99` and `minlevel_permitted - playerLevel` behavior.
- C# has `PlayerQuestState` status/complete-count storage, but no full `QuestState.canRepeat()` equivalent because next-repeat timing is not ported. The staged predicate returns `UnsupportedRepeatTiming` for completed time-based repeat quests rather than assuming parity.
- C# does not have a production `QuestTemplate` dataholder for start-condition fields such as race, class, gender, min/max level, rank, inventory items, XML start conditions, combine skill, NPC faction, repeat count, or category.
- C# does not have `XMLStartCondition` predicate logic.
- C# does not have the NPC faction quest state model needed by this predicate.
- C# does not have combine-skill lookup parity for this quest path.
- C# has `SmNearbyQuests` packet serialization, staged world quest-id projection, a staged early-gate predicate, and a level-diff projector, but no production player-controller refresh method or packet send path.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.QuestService.checkStartConditions` | `Aion.GameServer.Services.NearbyQuestStartConditionService` | Service / Quest Predicate | Partial | Unit Tested | Partial Parity | UOW-992 stages the nearby UI early gates: missing template, started/reward state, conservative repeat-count checks, race, min/max level with allowed diff 2, class, gender, and abyss rank. XML start conditions, inventory item checks, combine-skill checks, NPC faction checks, warning packets, exception/log behavior, and time-based repeat cooldowns remain unsupported. |
| `com.aionemu.gameserver.services.QuestService.getLevelRequirementDiff` | `Aion.GameServer.Services.NearbyQuestStartConditionService.GetLevelRequirementDiff` | Utility / Quest Predicate | Partial | Unit Tested | Partial Parity | UOW-992 tests Java's missing-template `99` and `minlevel_permitted - playerLevel` behavior. Production quest template loading and packet send integration remain unwired. |
| `com.aionemu.gameserver.model.templates.QuestTemplate`; `com.aionemu.gameserver.dataholders.QuestsData` | `Aion.GameServer.Dataholders.NearbyQuestTemplateSummary`; `Aion.GameServer.Dataholders.NearbyQuestTemplateTable`; `Aion.GameServer.Dataholders.NearbyQuestTemplateXmlExtractor` | Dataholder / DTO / XML Extractor | Partial | Unit Tested | Needs Verification | Staged DTO and extractor cover only early nearby predicate fields and unsupported-dependency flags. Production XML/JAXB loading, enum mapping from real static data, optional condition counting, category defaults, master-crafting adjustment, collect/inventory details, and repeat-cycle timing remain unported. |
| `com.aionemu.gameserver.model.templates.quest.XMLStartCondition` | Future C# XML start-condition predicate | Dataholder / Predicate | Not Started | No Tests | Unknown | Finished/unfinished/acquired/noacquired/title checks are unported. Equipped-item checks intentionally do not affect nearby UI when `warn = false`, but this needs test coverage once implemented. |
| `com.aionemu.gameserver.model.gameobjects.player.QuestStateList`; `com.aionemu.gameserver.questEngine.model.QuestState` | `Aion.GameServer.Model.GameObjects.PlayerQuestState` | Player Quest State | Partial | Unit Tested elsewhere | Needs Verification | C# stores status, vars, flags, and complete count for packet serialization, but repeatability and next-repeat timing needed by `QuestState.canRepeat()` are not ported. |

## Tests Added/Updated

UOW-992 adds:

- `NearbyQuestStartConditionServiceTests.CheckNearbyStartConditions_AppliesJavaBasicQuestTemplateGates`
- `NearbyQuestStartConditionServiceTests.CheckNearbyStartConditions_AppliesJavaQuestStateAndRepeatGatesConservatively`
- `NearbyQuestStartConditionServiceTests.CheckNearbyStartConditions_ReportsUnsupportedJavaDependenciesInsteadOfAssumingParity`
- `NearbyQuestStartConditionServiceTests.GetLevelRequirementDiff_MatchesJavaMissingTemplateAndMinLevelBehavior`

UOW-993 adds:

- `NearbyQuestTemplateXmlExtractorTests.Extract_ReadsNearbyPredicateQuestTemplateFieldsLikeJavaQuestTemplate`
- `NearbyQuestTemplateXmlExtractorTests.Extract_AppliesJavaQuestTemplateDefaultsForMissingOptionalFields`
- `NearbyQuestTemplateXmlExtractorTests.Extract_StreamInputFeedsNearbyQuestTemplateTableAndPredicate`

UOW-994 adds:

- `NearbyQuestTemplateXmlExtractorTests.RealDataAudit_LoadsNearbyQuestTemplateSummariesWithoutProductionWiring`

## Real-Data Staged Extractor Baseline

| Metric | Count |
|---|---:|
| Quest template summaries | 8043 |
| Templates with XML start conditions | 2427 |
| Templates with inventory item preconditions | 363 |
| Templates with combine-skill requirements | 628 |
| Templates with NPC faction requirements | 365 |
| Time-based repeat templates | 927 |
| Race-restricted templates | 7431 |
| Class-restricted templates | 483 |
| Gender-restricted templates | 18 |
| Abyss-rank restricted templates | 12 |
| Templates with nonzero min level | 8043 |
| Templates with nonzero max level | 1355 |
| Largest class-permitted list | 16 |

Existing relevant tests remain:

- `WorldMapRuntimeStateTests.NearbyQuestCandidateProjectionService_RegistersNpcStartQuestIdsLikeJavaWorldMapInstance`
- `QuestNpcStartRegistrationSourceRealDataAuditTests.RealDataAudit_ProjectsStagedQuestIdsIntoWorldInstanceWithoutRefreshWiring`
- `GamePacketTests.ServerPacketPayloads_MatchJavaShapes` (`SmNearbyQuests` cases)

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- C# has only a staged partial predicate for nearby `checkStartConditions`; production dispatch remains disabled.
- Quest template production static data loading for start-condition fields is not ported; only a staged XML extractor and real-data audit exist.
- Repeatability max-count handling is partial; repeat reset timing and full `QuestState.canRepeat()` are not modeled.
- XML start-condition semantics are source-audited only.
- NPC faction, combine-skill, inventory, abyss-rank, title, class/race/gender enum mapping, and exception/log behavior need C# homes before runtime candidate filtering can be claimed.
- Packet sends and production ItemPurification dispatch must remain disabled.

## Summary Metrics

- Total Java artifacts discovered: 5 in this unit
- Total artifacts ported: 3 staged partial artifacts across UOW-992 through UOW-994 (`NearbyQuestTemplateTable`, `NearbyQuestStartConditionService`, and `NearbyQuestTemplateXmlExtractor`)
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5
- Total blocked artifacts: 4 blocked/not-started categories, including production quest template loading, XML start conditions, repeat timing, and NPC faction/combine-skill dependencies
- Estimated overall migration completion: Phase 6 remains about 70% complete; this unit clarifies the next predicate blocker without enabling live nearby quest refresh.

## Next Recommended Unit Of Work

Add a staged bridge that combines world-instance quest ids, `NearbyQuestTemplateTable`, and `NearbyQuestStartConditionService` into candidate markers without sending `SM_NEARBY_QUESTS`. Keep XML start conditions, NPC faction, combine skill, packet sends, production integration, and production ItemPurification dispatch disabled until each dependency has tests.
