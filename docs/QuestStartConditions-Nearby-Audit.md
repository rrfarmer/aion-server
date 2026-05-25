# Nearby Quest Start Conditions Audit

Date: May 25, 2026
Unit of Work: UOW-991, updated by UOW-992 through UOW-1007

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
- `dotnetConversion/src/Aion.GameServer/Services/NearbyQuestMarkerProjectionService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientSocketServer.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/NearbyQuestTemplateXmlExtractorTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/NearbyQuestStartConditionServiceTests.cs`
- `docs/NearbyQuestRefresh-SendBoundary-Audit.md`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestNpcStartRegistrationSourceRealDataAuditTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/NearbyQuestRefreshPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/NearbyQuestRefreshPlanServiceTests.cs`
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

UOW-998 read-only XML dependency expansion adds:

- A condition block containing `finished` is optional; non-`finished` blocks are mandatory. `QuestTemplate.getRequiredConditionCount()` requires all mandatory rows plus one optional row when optional rows exist.
- `finished` reward matching treats a missing player reward group as failure when XML specifies `reward >= 0`.
- Repeatable finished prequests require `completeCount == maxRepeatCount`, except max repeat `255`.
- `acquired` treats a `COMPLETE` quest as acquired because it only fails missing state or `LOCKED`.
- `required_title` is not gated by `warn`; it must be implemented before XML conditions are treated as supported for nearby markers.

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
- UOW-995 adds `NearbyQuestMarkerProjectionService`, a staged bridge from world quest ids through the partial predicate into marker DTOs and rejection reasons, without packet sends.
- UOW-992 adds `NearbyQuestStartConditionService.CheckNearbyStartConditions` for the nearby UI call shape. It handles missing templates, active/reward quest state, conservative repeat-count checks, race, min/max level with Java's two-level nearby grace, class, gender, abyss rank, and explicit unsupported-dependency failures.
- UOW-992 adds `NearbyQuestStartConditionService.GetLevelRequirementDiff`, matching Java's missing-template `99` and `minlevel_permitted - playerLevel` behavior.
- C# has staged `PlayerQuestState` status, complete-count, reward-group, next-repeat-time, and complete-time storage. UOW-1002 implements the nearby `QuestState.canRepeat()` subset for max-repeat exhaustion and `nextRepeatTime` comparisons, but quest-finish repeat-date calculation and full persistence/update behavior remain unported.
- C# does not have a production `QuestTemplate` dataholder for start-condition fields such as race, class, gender, min/max level, rank, inventory items, XML start conditions, NPC faction, repeat count, or full category enum behavior.
- C# does not have `XMLStartCondition` predicate logic.
- C# now has a staged NPC faction snapshot for nearby checks, but repository hydration from `player_npc_factions`, `npc_factions.xml` mentor metadata loading, daily assignment, and mutation behavior remain missing.
- C# now has staged combine-skill lookup for this nearby quest path, but production static-data integration and Java runtime comparison remain missing.
- C# has `SmNearbyQuests` packet serialization, staged world quest-id projection, a staged early-gate predicate, and a level-diff projector, but no production player-controller refresh method or packet send path.
- UOW-996 documents the future send boundary: Java sends nearby markers immediately from `CM_LEVEL_READY` and schedules a debounced 1500 ms instance-wide refresh from `WorldMapInstance.addObject(Npc)`. C# does not yet implement either send trigger.
- UOW-997 adds a real-data staged marker projection audit for templates with no currently unsupported nearby dependencies. Current repository data yields 2072 supported projected quest ids; a level-65 Elyos male Gladiator receives 920 staged markers and 1152 supported early-gate rejections, with no unsupported dependency failures.
- UOW-998 adds `NearbyQuestRefreshPlanService`, a non-sending plan composer that fails closed for missing world instance/template data, reports world quest-id count, marker DTOs, rejected quest ids, rejection counts, and unsupported-dependency presence. It is not wired to `CM_LEVEL_READY`, NPC-spawn refresh, or ItemPurification dispatch.
- UOW-998 also adds read-only XML start-condition dependency findings from a sub-agent: implement `finished`, `unfinished`, `noacquired`, `acquired`, and `required_title` before treating XML start conditions as supported; preserve Java nearby behavior that `equipped` passes when `warn = false`.
- UOW-999 adds staged XML start-condition DTO/extractor/predicate support for `finished`, `unfinished`, `noacquired`, `acquired`, `equipped` with Java nearby `warn = false` no-op behavior, and `required_title`. Unknown XML start-condition children still fail closed as `UnsupportedXmlStartConditions`.
- UOW-999 adds nullable reward-group storage to `PlayerQuestState` so `finished reward="..."` can be evaluated. Existing DB loading does not yet hydrate reward groups, so production/runtime reward-group parity still needs repository follow-up before live sends.
- UOW-1000 extends `MySqlPlayerEnterWorldRepository.LoadPlayerQuestsAsync` to select the nullable Java `player_quests.reward` column into `PlayerQuestState.RewardGroup`, with a gated Java-schema DB integration assertion.
- UOW-1001 adds staged inventory item precondition support. `NearbyQuestTemplateXmlExtractor` now preserves `<inventory_item item_id="..." count="...">` rows, and `NearbyQuestStartConditionService` matches Java `QuestService.inventoryItemCheck(..., warn=false)` by checking only item-id presence in the player's inventory. The XML `count` value is intentionally parsed for traceability but not enforced in this gate.
- UOW-1001 read-only repeat-timing analysis confirms the next narrow slice needs `PlayerQuestState.NextRepeatTime`, `CompleteTime`, preserved `repeat_cycle` values, and server-timezone-aware comparisons before replacing `UnsupportedRepeatTiming`.
- UOW-1002 adds that narrow repeat-timing slice for nearby start checks: `NextRepeatTime`, `CompleteTime`, repository hydration for `next_repeat_time`/`complete_time`, preserved `repeat_cycle` tokens, and deterministic tests for Java `QuestState.canRepeat()` edge cases.
- UOW-1003 adds staged combine-skill support: explicit `combineskill`, `combine_skillpoint`, `combineskill = -1` any-skill behavior, NPC faction 12/13 tapping exclusion, `QuestCategory.TASK` upper-bound skip, and category default parsing.
- UOW-1004 adds staged NPC faction support: active exact faction checks, mentor/non-mentor slot cooldown via `NpcFactions.canStartQuest`, time-based cooldown skip, and `mentor_type` parsing for slot selection.
- UOW-1005 adds configurable master-crafting XML required-count support, matching Java `QuestTemplate.getRequiredConditionCount` adjustment for `combine_skillpoint == 499` and `CraftConfig.MAX_MASTER_CRAFTING_SKILLS`.
- UOW-1006 adds staged NPC faction static-data loading, matching Java `NpcFactionsData` id/NPC-id indexing and `NpcFactionTemplate.isMentor()` category behavior.
- UOW-1007 hydrates Java-schema `player_npc_factions` rows into `Player.NpcFactions` during enter-world when `GameServerRuntimeContext.DataManager.StaticData.NpcFactions` is available. It derives the mentor slot from static `npc_factions.xml` metadata like Java `NpcFaction`, but daily assignment, join/leave mutation, future `QuestService.startQuest` assigned-quest wiring, and faction persistence writes remain unported.
- UOW-1008 stages the assigned NPC faction quest guard as `PlayerNpcFactionsSnapshot.CanStartAssignedQuest`. This is intentionally not called from the nearby predicate because Java enforces the `faction.getQuestId() == id` check in `QuestService.startQuest`, after the nearby `checkStartConditions` path.
- UOW-1009 adds `docs/QuestRepeatDate-Audit.md`, a read-only audit for Java `QuestService.calculateRepeatDate`. The current nearby repeat check can consume loaded `next_repeat_time`, but C# still does not calculate that value on quest completion.
- UOW-1010 adds `QuestRepeatDateService.CalculateNextRepeatTime`, a pure Java 09:00 daily/weekly reset calculator. It is not wired to quest completion or persistence.
- UOW-1011 adds the configured timezone resolver and repeat-date options overload, so future quest-finish work can use `gameserver.timezone` like Java `ServerTime`.
- UOW-1012 adds a staged quest-finish state mutation service that completes `REWARD` states, clears quest vars, increments complete count, stamps complete time, and calculates `NextRepeatTime` for time-based quests. It is not wired to production rewards, packets, persistence, NPC faction completion, callbacks, or nearby refresh.
- UOW-1013 adds staged NPC faction completion state and the Java `NpcFactions.getNextTime()` daily 09:00 reset helper. It is not wired to production quest completion, mentor title side effects, faction DAO writes, daily assignment, packets, or nearby refresh.
- UOW-1014 adds `docs/QuestFinishOrdering-Audit.md`, documenting Java quest-finish packet, callback, NPC faction completion, nearby-refresh, and deferred persistence ordering. It does not change runtime code.
- UOW-1015 adds a staged `QuestFinishOperationPlanService` that composes quest-state mutation, optional NPC faction completion, and ordered non-live descriptors for packet update, callback dispatch, nearby refresh, and deferred persistence. It does not send packets or write DAOs.
- UOW-1016 adds non-sending `SmQuestAction.Update` byte serialization for Java `SM_QUEST_ACTION(ActionType.UPDATE, qs)`, including explicit extra-category body suppression. It is not wired to live quest completion.
- UOW-1017 adds `docs/QuestFinishRewardWorkItem-Audit.md`, documenting Java reward-group correction, item/non-item rewards, challenge-task notification, and quest work-item removal. It does not change runtime code.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.QuestService.checkStartConditions`; `QuestService.inventoryItemCheck`; `QuestState.canRepeat`; `QuestService.checkCombineSkill`; `NpcFactions.canStartQuest` | `Aion.GameServer.Services.NearbyQuestStartConditionService` | Service / Quest Predicate | Partial | Unit Tested | Partial Parity | Stages nearby UI early gates, XML subset, inventory item-id presence checks, repeat timing for completed quests, combine-skill checks, and UOW-1004 NPC faction checks. Warning packets, exception/log behavior, production static-data loading, and live send triggers remain unsupported. |
| `com.aionemu.gameserver.services.QuestService.getLevelRequirementDiff` | `Aion.GameServer.Services.NearbyQuestStartConditionService.GetLevelRequirementDiff` | Utility / Quest Predicate | Partial | Unit Tested | Partial Parity | UOW-992 tests Java's missing-template `99` and `minlevel_permitted - playerLevel` behavior. Production quest template loading and packet send integration remain unwired. |
| `com.aionemu.gameserver.model.templates.QuestTemplate`; `com.aionemu.gameserver.dataholders.QuestsData`; `com.aionemu.gameserver.model.templates.quest.QuestRepeatCycle`; `QuestCategory`; `QuestMentorType` | `Aion.GameServer.Dataholders.NearbyQuestTemplateSummary`; `Aion.GameServer.Dataholders.NearbyQuestTemplateTable`; `Aion.GameServer.Dataholders.NearbyQuestTemplateXmlExtractor` | Dataholder / DTO / XML Extractor | Partial | Unit Tested | Needs Verification | Staged DTO and extractor cover nearby predicate fields, XML start-condition subset data, inventory rows, repeat-cycle tokens, combine-skill point, category token, and mentor slot flag. Production XML/JAXB loading, full enum mapping, configurable master-crafting adjustment, collect item details, and quest-finish repeat-date calculation remain unported. |
| `com.aionemu.gameserver.model.gameobjects.player.npcFaction.NpcFactions`; `NpcFaction`; `ENpcFactionQuestState` | `Aion.GameServer.Model.GameObjects.PlayerNpcFactionsSnapshot`; `PlayerNpcFactionState`; `PlayerNpcFactionQuestState` | Player State / DTO / Enum | Partial | Unit Tested | Partial Parity | Staged snapshot covers exact active faction and mentor/non-mentor slot cooldowns for nearby checks. Repository hydration, `npc_factions.xml`, daily assignment, mutation behavior, and persistence states remain missing. |
| `com.aionemu.gameserver.model.gameobjects.player.npcFaction.NpcFactions.completeQuest`; `NpcFactions.getNextTime` | `PlayerNpcFactionsSnapshot.CompleteActiveQuest`; `Aion.GameServer.Services.NpcFactionDailyResetService` | Player State / Faction Completion Helper | Partial | Unit Tested | Partial Parity | UOW-1013 stages active-slot completion and the Java `hour >= 9` reset boundary. It is not wired to `QuestFinishStateMutationService`, production quest completion, mentor-title side effects, daily assignment, or faction DAO writes. |
| `com.aionemu.gameserver.dataholders.NpcFactionsData`; `NpcFactionTemplate`; `FactionCategory` | `Aion.GameServer.Dataholders.NpcFactionTable`; `NpcFactionSummary`; `StaticData.NpcFactions` | Static Data / DTO / Enum-like Dependency | Partial | Unit Tested; Regression Tested | Partial Parity | UOW-1006 loads faction templates by id and NPC id, preserving category/race tokens and Java `maxLevel = 99` default. Full enum modeling and Java JAXB runtime comparison remain missing. |
| `com.aionemu.gameserver.model.templates.quest.InventoryItems`; `com.aionemu.gameserver.model.templates.quest.InventoryItem` | `Aion.GameServer.Dataholders.NearbyQuestInventoryItem`; `NearbyQuestTemplateXmlExtractor` | DTO / XML Predicate Dependency | Partial | Unit Tested | Partial Parity | UOW-1001 parses `inventory_item.item_id` and optional `count`. The nearby predicate intentionally checks item-id presence only, matching Java `inventoryItemCheck`; count enforcement belongs to other collect-item paths and remains out of scope. Production JAXB/static-data integration remains unwired. |
| `com.aionemu.gameserver.model.templates.quest.XMLStartCondition`; `QuestTemplate.getRequiredConditionCount`; `CraftConfig.MAX_MASTER_CRAFTING_SKILLS` | `Aion.GameServer.Dataholders.NearbyQuestXmlStartCondition`; `Aion.GameServer.Services.NearbyQuestStartConditionService` | Dataholder / Predicate | Partial | Unit Tested | Partial Parity | UOW-999 implements the nearby `warn = false` supported subset and UOW-1005 adds configurable master-crafting required-count adjustment. Unknown XML children still fail closed. Production static-data loading and Java runtime comparison remain unported. |
| `com.aionemu.gameserver.model.templates.quest.FinishedQuestCond` | `Aion.GameServer.Dataholders.NearbyQuestFinishedCondition` | DTO / XML Predicate Dependency | Partial | Unit Tested | Partial Parity | UOW-999 parses `quest_id` and default/explicit `reward`. Reward-group matching is unit-tested with staged `PlayerQuestState.RewardGroup`; production quest-state repository hydration is not yet verified. Repeatable prerequisite exact max-complete-count behavior is unit-tested for staged templates. |
| `com.aionemu.gameserver.model.gameobjects.player.QuestStateList`; `com.aionemu.gameserver.questEngine.model.QuestState`; `com.aionemu.gameserver.dao.PlayerQuestListDAO.load` | `Aion.GameServer.Model.GameObjects.PlayerQuestState`; `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository` | Player Quest State / Repository | Partial | Unit Tested; Integration Tested when DB flag enabled | Partial Parity | C# now stores status, vars, flags, complete count, nullable reward group, `NextRepeatTime`, and `CompleteTime`. Repository hydration covers `reward`, `next_repeat_time`, and `complete_time` when DB integration is enabled; timezone interpretation of unspecified MySQL timestamps still needs live verification. |
| `com.aionemu.gameserver.services.QuestService.finishQuest`; `com.aionemu.gameserver.questEngine.model.QuestState` | `Aion.GameServer.Services.QuestFinishStateMutationService`; `Aion.GameServer.Model.GameObjects.PlayerQuestState` | Service / Quest Completion Boundary | Partial | Unit Tested | Partial Parity | UOW-1012 stages the state-only completion subset: missing/non-REWARD rejection, repeated mission guard, status completion, quest-var clear, complete-count/time update, and time-based repeat reset calculation. Production rewards, packets, callbacks, NPC faction completion, persistence writes, and nearby refresh remain unported. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_LEVEL_READY` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleLevelReadyAsync` | Client Packet Handler / Nearby Send Trigger | Partial | Manual Only | Needs Verification | UOW-996 source-audits Java's immediate level-ready call to `updateNearbyQuests`. C# intentionally omits the nearby marker send until production candidate sources and predicates are safe. |
| `com.aionemu.gameserver.world.WorldMapInstance.addObject` | Future C# NPC-spawn delayed nearby refresh scheduler | World Instance / Delayed Refresh Trigger | Not Started | Manual Only | Needs Verification | UOW-996 source-audits Java's 1500 ms one-pending-task debounce for NPC-spawn quest-id changes. C# has staged quest-id storage only; no live scheduler/fanout exists. |
| `com.aionemu.gameserver.controllers.PlayerController.updateNearbyQuests` | `Aion.GameServer.Services.NearbyQuestMarkerProjectionService`; `QuestNpcStartRegistrationSourceRealDataAuditTests` | Controller / Quest UI Projection Audit | Partial | Regression Tested | Partial Parity | UOW-997 pins one supported-template real-data projection slice. It excludes unsupported XML/inventory/combine-skill/NPC-faction/time-based templates and still does not send packets or verify Java `HashMap` order. |
| `com.aionemu.gameserver.controllers.PlayerController.updateNearbyQuests` | `Aion.GameServer.Services.NearbyQuestRefreshPlanService` | Controller / Quest UI Refresh Plan | Partial | Unit Tested | Partial Parity | UOW-998 composes staged marker projection into a non-sending plan with explicit readiness/failure states. It does not resolve live map-region parents, send `SM_NEARBY_QUESTS`, schedule NPC-spawn refresh, or claim Java `HashMap` ordering parity. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_ACTION`; `QuestEngine.onQuestCompleted`; `PlayerQuestListDAO.store`; `PlayerNpcFactionsDAO.storeNpcFactions` | Future C# quest-finish operation plan / packet / persistence boundaries | Packet / Callback / Repository Dependency | Not Started | Manual Only | Needs Verification | UOW-1014 documents Java ordering: update packet before callbacks, callbacks before NPC faction completion, nearby refresh last, and DAO writes deferred to `PlayerService.storePlayer`. No C# composed operation plan or live side effects exist yet. |
| `com.aionemu.gameserver.services.QuestService.finishQuest`; `SM_QUEST_ACTION`; `QuestEngine.onQuestCompleted`; `NpcFactions.completeQuest` | `Aion.GameServer.Services.QuestFinishOperationPlanService` | Service / Operation Plan | Partial | Unit Tested | Partial Parity | UOW-1019 composes optional non-live reward/work-item descriptors before staged quest-state mutation, then packet/callback/NPC-faction/nearby/persistence descriptors in Java order. Full reward mutation, live packets, callback runtime, DAO writes, and production nearby refresh remain unported. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_ACTION` | `Aion.GameServer.Network.Aion.ServerPackets.SmQuestAction` | Packet | Partial | Unit Tested | Partial Parity | UOW-1016 ports the non-sending `UPDATE` body shape and explicit extra-category suppression. Other action types and production static-data suppression lookup remain unported. |
| `com.aionemu.gameserver.services.QuestService.validateAndFixRewardGroup`; `getRewardItems`; `giveReward`; `removeQuestWorkItems` | `Aion.GameServer.Services.QuestFinishRewardPlanService` | Service / Reward and Inventory Dependency | Partial | Unit Tested | Partial Parity | UOW-1018 stages reward-group correction and non-live reward/work-item descriptors. Full item selection, class/extended/bonus rewards, non-item mutation, live inventory removal, and Java runtime comparison remain missing. |
| `com.aionemu.gameserver.model.templates.quest.XMLStartCondition` | Future C# XML start-condition predicate | Dataholder / Predicate | Not Started | Manual Only | Needs Verification | UOW-998 read-only analysis clarifies optional `finished` rows, mandatory non-finished rows, reward-group matching, repeatable prerequisite completion, `acquired` COMPLETE behavior, `equipped` warn gating, and `required_title` enforcement. No C# predicate code added yet. |

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

UOW-995 adds:

- `NearbyQuestMarkerProjectionServiceTests.ProjectMarkers_FiltersWorldQuestIdsThroughStagedNearbyPredicateWithoutSendingPacket`
- `NearbyQuestMarkerProjectionServiceTests.ProjectMarkers_PreservesPositiveAndNegativeLevelDiffsForPacketMarkerRule`

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
- `docs/NearbyQuestRefresh-SendBoundary-Audit.md` manual source audit for send triggers and safety gates
- `QuestNpcStartRegistrationSourceRealDataAuditTests.RealDataAudit_ProjectsSupportedNearbyMarkersWithoutProductionSendWiring`
- `NearbyQuestRefreshPlanServiceTests.CreatePlan_FailsClosedWithoutWorldInstanceOrQuestTemplates`
- `NearbyQuestRefreshPlanServiceTests.CreatePlan_ReturnsNoWorldQuestIdsWithoutSending`
- `NearbyQuestRefreshPlanServiceTests.CreatePlan_ComposesMarkersAndRejectionReasonsWithoutSending`
- `NearbyQuestRefreshPlanServiceTests.CreatePlan_ReturnsNoMarkersWhenAllQuestIdsAreRejected`
- `NearbyQuestTemplateXmlExtractorTests.Extract_MarksUnknownXmlStartConditionChildrenUnsupported`
- `NearbyQuestStartConditionServiceTests.CheckNearbyStartConditions_AppliesSupportedJavaXmlStartConditions`
- `NearbyQuestStartConditionServiceTests.CheckNearbyStartConditions_AppliesJavaXmlStartConditionFailures`
- `NearbyQuestStartConditionServiceTests.CheckNearbyStartConditions_RequiresAllMandatoryAndOneOptionalXmlBlockLikeJava`
- `NearbyQuestStartConditionServiceTests.CheckNearbyStartConditions_FailsClosedForUnknownXmlStartConditionChildren`
- `PlayerEnterWorldRepositoryDatabaseIntegrationTests.LoadPlayerQuests_HydratesRewardGroupAndRepeatTimesAgainstJavaSchema_WhenEnabled`
- `NearbyQuestStartConditionServiceTests.CheckNearbyStartConditions_AppliesJavaInventoryItemPresenceGate`
- UOW-1001 read-only repeat-timing sub-agent analysis for `QuestState.canRepeat`, `QuestTemplate.repeat_cycle`, `QuestRepeatCycle`, `PlayerQuestListDAO`, and nearby repeat callers
- UOW-1002 updates `NearbyQuestStartConditionServiceTests.CheckNearbyStartConditions_AppliesJavaQuestStateAndRepeatGatesConservatively` for null/future/equal `nextRepeatTime` and max repeat `255`
- UOW-1003 adds `NearbyQuestStartConditionServiceTests.CheckNearbyStartConditions_AppliesJavaCombineSkillGate`
- UOW-1003 read-only NPC faction sub-agent analysis for `NpcFactions`, `NpcFaction`, `PlayerNpcFactionsDAO`, `QuestsData`, and `NpcFactionTemplate`
- UOW-1004 adds `NearbyQuestStartConditionServiceTests.CheckNearbyStartConditions_AppliesJavaNpcFactionGate`
- UOW-1004 read-only master-crafting sub-agent analysis for `QuestTemplate.getRequiredConditionCount` and `CraftConfig.MAX_MASTER_CRAFTING_SKILLS`
- UOW-1005 adds `NearbyQuestStartConditionServiceTests.CheckNearbyStartConditions_AppliesJavaMasterCraftingRequiredConditionAdjustment`
- UOW-1006 adds `StaticDataLoadingTests.StaticData_LoadsNpcFactionTemplatesLikeJavaDataholder` and extends `DataManager_LoadsRealJavaStaticDataManifestCounts`

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- C# has only a staged partial predicate for nearby `checkStartConditions`; production dispatch remains disabled.
- Quest template production static data loading for start-condition fields is not ported; only a staged XML extractor and real-data audit exist.
- Repeatability start-check handling is partial; staged quest-finish repeat reset calculation exists, but production wiring, reset notification packets, DAO writes, and timestamp timezone verification remain incomplete.
- XML start-condition semantics are partial: the nearby supported subset and configurable master-crafting adjustment are unit-tested, but production static-data loading, unknown future XML children, and Java runtime comparison remain unverified.
- `PlayerQuestState.RewardGroup` is now hydrated by the MySQL enter-world repository when DB integration is enabled, but broader persistence/update behavior and Java runtime comparison remain unverified.
- NPC faction repository hydration and staged completion exist, but abyss-rank/title/class/race/gender enum mapping, exception/log behavior, mentor-title side effects, daily assignment, persistence writes, and production static-data/JAXB comparison need C# homes before runtime candidate filtering can be claimed.
- Packet sends and production ItemPurification dispatch must remain disabled.
- Level-ready and NPC-spawn send triggers remain documented only; no C# runtime send path exists.
- The supported-template real-data audit uses one synthetic player archetype and excludes unsupported dependency categories rather than proving full Java predicate parity.
- The non-sending plan service has no production caller and intentionally does not send packets.
- XML start-condition dependency analysis is documentation only; predicate implementation remains absent.

## Summary Metrics

- Total Java artifacts discovered: 9 in this unit
- Total artifacts ported: 18 staged partial artifacts across UOW-992 through UOW-1016 (`NearbyQuestTemplateTable`, `NearbyQuestStartConditionService`, `NearbyQuestTemplateXmlExtractor`, `NearbyQuestMarkerProjectionService`, `NearbyQuestRefreshPlanService`, `NearbyQuestXmlStartCondition`, `NearbyQuestFinishedCondition`, `NearbyQuestInventoryItem`, `PlayerQuestState` reward/repeat fields, repository hydration, combine-skill predicate support, NPC faction snapshot support, master required-count support, NPC faction static-data loading, staged quest-finish state mutation, staged NPC faction completion, staged quest-finish operation planning, and non-sending quest action update packet)
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 14
- Total blocked artifacts: 6 blocked/not-started categories, including production quest template loading/config plumbing, quest-finish packet/persistence/callback wiring, NPC faction mentor-title/daily-assignment side effects, timestamp timezone verification, broader refresh-plan archetype audits, and production send triggers
- Estimated overall migration completion: Phase 6 remains about 70% complete; this unit clarifies the next predicate blocker without enabling live nearby quest refresh.

## Next Recommended Unit Of Work

Stage reward/work-item operation descriptors for quest finish, starting with reward-group correction edge cases. Keep packet sends, production integration, DAO writes, live nearby refresh, live inventory mutation, and production ItemPurification dispatch disabled until each dependency has tests.
