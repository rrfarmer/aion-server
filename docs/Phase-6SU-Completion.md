# Phase 6SU Completion - UOW-1003 Nearby Combine Skill

Date: May 25, 2026

## Scope

Implemented staged Java `QuestService.checkCombineSkill` behavior for nearby start checks.

This unit intentionally did not enable live nearby quest packet sends, `CM_LEVEL_READY` dispatch, delayed NPC-spawn refresh, production `StaticData` loading, NPC faction predicates, or production `CM_ITEM_PURIFICATION` quest refresh dispatch.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|
| A | Combine-skill nearby predicate | `QuestService.checkCombineSkill`, `QuestTemplate`, `QuestCategory` | `NearbyQuestTemplateTable.cs`, `NearbyQuestTemplateXmlExtractor.cs`, `NearbyQuestStartConditionService.cs`, nearby tests | Service Port | No with other predicate edits | Medium | Central predicate and template files need one writer. |
| B | NPC faction predicate audit | `NpcFactions`, `NpcFaction`, `PlayerNpcFactionsDAO`, NPC faction branch in `QuestService` | Read-only report | Java Analysis | Yes | Low | Independent analysis can run without writes. |
| C | Broader archetype audit | `PlayerController.updateNearbyQuests`, real `quest_data.xml` | Existing real-data audit tests/docs | Test Creation | Not with A in this unit | Medium | Predicate changes affect baselines. |
| D | Quest-finish repeat reset | `QuestService.calculateRepeatDate`, `QuestState.setNextRepeatTime` | Future quest finish services/repository files | Java Analysis / Service Port | Read-only only | Medium | Implementation touches date/time and quest mutation surfaces. |

Selected work:
- Orchestrator implemented staged combine-skill predicates.
- Read-only sub-agent analyzed NPC faction behavior and made no edits; the sub-agent was closed.

## Java Source Breadcrumbs

- `com.aionemu.gameserver.services.QuestService.checkCombineSkill(QuestEnv, boolean)`
- `com.aionemu.gameserver.model.templates.QuestTemplate.getCombineSkill()`
- `com.aionemu.gameserver.model.templates.QuestTemplate.getCombineSkillPoint()`
- `com.aionemu.gameserver.model.templates.QuestTemplate.getCategory()`
- `com.aionemu.gameserver.model.templates.quest.QuestCategory`

Java behavior implemented in this slice:
- `combineskill = 0` means no combine-skill gate.
- `combineskill = -1` checks essence/aether tapping plus crafting/construction skills.
- NPC faction ids 12 and 13 exclude essence/aether tapping from the `-1` any-skill set.
- Explicit `combineskill` checks only that skill id.
- A skill passes when `PlayerSkillEntry.skillLevel >= combine_skillpoint`.
- `QuestCategory.TASK` rejects work-order skills where `skillLevel - 40 > combine_skillpoint`.
- Nearby checks use `warn = false`, so warning packets are outside this staged path.

## Implementation

- Added `CombineSkillPoint` and `QuestCategory` to `NearbyQuestTemplateSummary`.
- Extended `NearbyQuestTemplateXmlExtractor` to parse `combine_skillpoint` and default missing `category` to Java's `QUEST`.
- Extended `NearbyQuestStartConditionService` to evaluate combine-skill requirements against `Player.Skills`.
- Added `NearbyQuestStartConditionFailure.CombineSkill`.
- Removed combine-skill from `NearbyQuestRefreshPlan.HasUnsupportedDependencies`.
- Updated the real-data supported projection audit to stop treating combine-skill as an unsupported dependency category.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Dataholders/NearbyQuestTemplateTable.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/NearbyQuestTemplateXmlExtractor.cs`
- `dotnetConversion/src/Aion.GameServer/Services/NearbyQuestStartConditionService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/NearbyQuestRefreshPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/NearbyQuestStartConditionServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/NearbyQuestTemplateXmlExtractorTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestNpcStartRegistrationSourceRealDataAuditTests.cs`
- `docs/PHASE-6-PROGRESS.md`

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~NearbyQuestStartConditionServiceTests|FullyQualifiedName~NearbyQuestTemplateXmlExtractorTests|FullyQualifiedName~NearbyQuestRefreshPlanServiceTests|FullyQualifiedName~NearbyQuestMarkerProjectionServiceTests|FullyQualifiedName~QuestNpcStartRegistrationSourceRealDataAuditTests"` | Passed, 25 tests |
| `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` | Passed, 1709 tests |

## Migration Parity Table - UOW-1003

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.QuestService.checkCombineSkill` | `Aion.GameServer.Services.NearbyQuestStartConditionService` | Service / Quest Predicate | Partial | Unit Tested | Partial Parity | Implements staged nearby combine-skill filtering for explicit skill ids, `-1` any-skill sentinel, NPC faction 12/13 tapping exclusion, and `QuestCategory.TASK` upper-bound skip. Warning packet behavior is intentionally absent because nearby checks use `warn = false`. |
| `com.aionemu.gameserver.model.templates.QuestTemplate` | `Aion.GameServer.Dataholders.NearbyQuestTemplateSummary`; `NearbyQuestTemplateXmlExtractor` | Dataholder / XML Extractor | Partial | Unit Tested | Partial Parity | Adds `CombineSkillPoint` and `QuestCategory` to the existing staged summary. Production JAXB/static-data loading and Java runtime comparison remain unwired. |
| `com.aionemu.gameserver.model.templates.quest.QuestCategory` | `Aion.GameServer.Dataholders.NearbyQuestTemplateSummary.QuestCategory` | Enum-like XML Dependency | Partial | Unit Tested | Partial Parity | C# preserves the category token as a string for the nearby `TASK` branch only. Full enum modeling and validation of all category values remain out of scope. |
| `com.aionemu.gameserver.model.gameobjects.player.PlayerSkillList`; `com.aionemu.gameserver.model.skill.PlayerSkillEntry` | `Aion.GameServer.Model.GameObjects.Player.Skills`; `PlayerSkill` | Player Skill Dependency | Partial | Unit Tested | Partial Parity | Existing player skill DTO supplies `SkillId` and `SkillLevel` for the combine predicate. Skill persistence already exists, but Java runtime comparison for this quest branch remains missing. |
| `com.aionemu.gameserver.model.gameobjects.player.npcFaction.NpcFactions`; `NpcFaction`; `PlayerNpcFactionsDAO` | Future `PlayerNpcFactionState` / snapshot predicate | Player NPC Faction Dependency | Not Started | Manual Only | Needs Verification | Read-only sub-agent mapped Java behavior. C# still lacks faction state models, repository hydration, active mentor/non-mentor slot reconstruction, and slot-level cooldown checks. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `NearbyQuestStartConditionServiceTests.CheckNearbyStartConditions_AppliesJavaCombineSkillGate` | Unit | Java `QuestService.checkCombineSkill` | Validates explicit skill id, missing skill point failure, any-skill `-1`, NPC faction 12/13 tapping exclusion, and `TASK` upper-bound behavior. | Deterministic C# test from source-reviewed Java predicate. | Does not compare against a running Java server or emit warning packets. |
| `NearbyQuestTemplateXmlExtractorTests.Extract_ReadsNearbyPredicateQuestTemplateFieldsLikeJavaQuestTemplate` | Unit | Java `QuestTemplate` JAXB attributes | Validates `combine_skillpoint` and `category` extraction. | Deterministic C# XML extractor test. | No Java JAXB runtime comparison. |
| `NearbyQuestTemplateXmlExtractorTests.Extract_AppliesJavaQuestTemplateDefaultsForMissingOptionalFields` | Unit | Java `QuestTemplate` field defaults | Validates `CombineSkillPoint = 0` and `QuestCategory = QUEST` defaults. | Deterministic C# test from source-reviewed Java defaults. | Full `QuestCategory` enum is not modeled. |
| Read-only NPC faction sub-agent analysis | Manual | Java `QuestService`, `NpcFactions`, `NpcFaction`, `PlayerNpcFactionsDAO`, `QuestsData`, `NpcFactionTemplate` | Documents exact NPC faction nearby predicate behavior and next C# slice. | Source-reviewed report; sub-agent made no edits and was closed. | No C# NPC faction implementation yet. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- Combine-skill support is staged and unit-tested, but production static-data integration is absent.
- Java warning packets are not modeled because nearby checks call with `warn = false`; non-nearby quest acquisition remains outside this staged service.
- Master-crafting XML required-condition-count adjustment still needs configurable `CraftConfig.MAX_MASTER_CRAFTING_SKILLS` plumbing before non-default configs can be claimed.
- NPC faction predicates remain unsupported; Java uses active exact faction rows plus mentor/non-mentor slot `timeLimit`, not just per-row cooldown.
- Quest-finish repeat-date calculation, server-timezone DB verification, packet sends, `CM_LEVEL_READY`, NPC-spawn delayed refresh, production player-controller refresh, and ItemPurification dispatch remain disabled.

## Summary Metrics

- Total Java artifacts discovered: 6 in this unit
- Total artifacts ported: 2 staged partial artifacts in this unit: combine-skill predicate support and combine/category XML field extraction
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6
- Total blocked artifacts: 5 blocked/not-started categories: NPC faction predicates, configurable master-crafting condition adjustment, production static-data integration, quest-finish repeat-date calculation/timezone verification, and live nearby send triggers
- Estimated overall migration completion: Phase 6 remains about 70% complete; this unit removes the combine-skill nearby predicate blocker without enabling live nearby quest refresh.

## Next Recommended Unit Of Work

Implement a staged NPC faction state snapshot for nearby checks:

- Model active faction rows and exact faction active checks.
- Model non-time-based mentor/non-mentor slot cooldown behavior using Java `NpcFactions.canStartQuest`.
- Preserve the Java behavior where time-based NPC faction quests skip the daily cooldown gate but still require the exact active faction.
- Add deterministic tests for missing faction, inactive faction, wrong active faction, cooldown not elapsed, cooldown elapsed, and time-based cooldown skip.

Keep repository hydration, live sends, production integration, and ItemPurification dispatch disabled until the model tests are green.

## Safe Parallel Candidates For Next Session

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | NPC faction state model | New model/test files plus central predicate files if implemented by orchestrator | Medium | Central predicate files should have one owner. |
| B | Read-only master-crafting adjustment audit | Java `QuestTemplate.getRequiredConditionCount`, `CraftConfig` | Low | Can run beside NPC faction implementation without writes. |
| C | Broader real-data archetype audit | Existing real-data audit tests/docs | Medium | Should wait until predicate change is stable because baselines may change. |

## Do Not Parallelize

- `NearbyQuestStartConditionService.cs`, `NearbyQuestTemplateTable.cs`, and `NearbyQuestTemplateXmlExtractor.cs`: central nearby predicate/template ownership.
- Progress, parity, and handoff docs: orchestrator-owned.
