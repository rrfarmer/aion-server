# Phase 6SW Completion - UOW-1005 Nearby Master Required Count

## Scope

UOW-1005 adds configurable master-crafting XML required-count parity to the staged nearby quest predicate. This remains offline: no live nearby sends, production player-controller refresh, production `StaticData` integration, NPC faction repository/static-data hydration, or production `CM_ITEM_PURIFICATION` dispatch is enabled.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|
| A | Master-crafting XML required-count parity | `QuestTemplate.getRequiredConditionCount`, `QuestTemplate.isMaster`, `CraftConfig.MAX_MASTER_CRAFTING_SKILLS` | `NearbyQuestStartConditionService.cs`, nearby tests, docs | Predicate/config hook | No with other nearby predicate edits | Low | Narrow central predicate change. |
| B | NPC faction repository hydration audit | `PlayerNpcFactionsDAO`, SQL schema, `NpcFaction` constructor | Read-only report | Java/C# data analysis | Yes | Low | Independent of XML required count. |
| C | Broader archetype real-data projection audit | Java nearby predicate/data | real-data audit tests | Test creation | Not with A today | Medium | Baselines may shift with predicate changes. |
| D | Quest-finish repeat-date calculation audit | `QuestService.calculateRepeatDate`, `QuestState.setNextRepeatTime` | Read-only report | Java analysis | Yes | Medium | Larger date/time surface. |

Selected batch: local-only A. The UOW-1004 sidecar had already completed the Java audit, so no sub-agent was needed.

## Java Breadcrumbs

- `QuestTemplate.getRequiredConditionCount()` returns mandatory XML blocks plus at most one optional block.
- `XMLStartCondition.isOptional()` is true when the block contains `<finished>`.
- `QuestTemplate.isMaster()` is true when `combine_skillpoint == 499`.
- Master quests adjust the required count by `1 - CraftConfig.MAX_MASTER_CRAFTING_SKILLS`.

## Implementation

- Added `maxMasterCraftingSkills` to `NearbyQuestStartConditionService.CheckNearbyStartConditions`, defaulting to `CraftSkillUpdateService.DefaultMaxMasterCraftingSkills`.
- Threaded the cap through XML start-condition evaluation.
- Applied the Java master-crafting adjustment when `CombineSkillPoint == 499`.
- Mirrored Java's edge behavior where high master caps can reduce the required count to zero or lower.

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter FullyQualifiedName~NearbyQuestStartConditionServiceTests` | Passed, 12 tests |
| `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` | Passed, 1711 tests |

## Migration Parity Table - UOW-1005

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.templates.QuestTemplate.getRequiredConditionCount` | `Aion.GameServer.Services.NearbyQuestStartConditionService` | Quest Predicate / XML Required Count | Partial | Unit Tested | Partial Parity | Implements mandatory/optional XML count plus Java master-crafting adjustment for staged nearby checks. Production JAXB/static-data loading and Java runtime comparison remain unwired. |
| `com.aionemu.gameserver.model.templates.QuestTemplate.isMaster` | `Aion.GameServer.Dataholders.NearbyQuestTemplateSummary.CombineSkillPoint` | Template Predicate Dependency | Partial | Unit Tested | Partial Parity | Uses Java's `combine_skillpoint == 499` rule for master XML required-count adjustment. Does not model full `QuestTemplate` behavior. |
| `com.aionemu.gameserver.configs.main.CraftConfig.MAX_MASTER_CRAFTING_SKILLS` | `NearbyQuestStartConditionService.CheckNearbyStartConditions(..., maxMasterCraftingSkills)`; `CraftSkillUpdateService.DefaultMaxMasterCraftingSkills` | Config / Predicate Parameter | Partial | Unit Tested | Partial Parity | Defaults to the existing C# master cap of 1 and accepts explicit non-default caps for staged parity tests. Production option plumbing remains missing. |

## Tests Added Or Updated

| Test Name | Type | What It Validates | Java Comparison |
|---|---|---|---|
| `NearbyQuestStartConditionServiceTests.CheckNearbyStartConditions_AppliesJavaMasterCraftingRequiredConditionAdjustment` | Unit | Default cap, relaxed cap, non-master guard, and zero-required-count edge. | Source-reviewed Java formula; no runtime Java comparison. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- Master-crafting cap is staged as a method parameter, not production option plumbing.
- Production static-data/JAXB loading remains unwired.
- NPC faction repository/static-data hydration, daily assignment, mutation behavior, and start-quest assigned-id guard remain unported.
- Live nearby sends and ItemPurification dispatch remain disabled.

## Summary Metrics

- Total Java artifacts discovered: 3
- Total artifacts ported: 1 staged partial predicate/config hook
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 3
- Total blocked artifacts: 5 blocked/not-started categories
- Estimated overall migration completion: Phase 6 remains about 70%

## Next Recommended Unit Of Work

Add NPC faction repository/static-data hydration or broaden refresh-plan audits across representative player archetypes now that nearby XML/inventory/repeat/combine/NPC/master predicate slices are staged. Keep packet sends, production integration, and production ItemPurification dispatch disabled.
