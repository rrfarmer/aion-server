# Phase 6WA Completion - UOW-1087 Regular Item Reward Projection

Date: May 26, 2026

## Unit Of Work

UOW-1087: `[Phase 6][UOW-1087] Extract quest item reward projection`

## Starting Point

- Continue from `docs/Phase-6VZ-Completion.md`.
- UOW-1086 proved XML-derived reportability and default regular non-item rewards can compose through the non-live guard and operation planners.
- Production quest-finish, reward mutation, XP mutation, custom reward execution, mail execution, persistence, and packet sends remain disabled.

## Summary

UOW-1087 extends the static quest reward XML extractor to parse Java regular `reward_item` and `selectable_reward_item` lists for the selected regular reward group.

The slice is deliberately static and non-live. It does not parse extended rewards, class selectable rewards, bonus handler additions, or enable production reward execution.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Regular item/selectable reward XML projection | `QuestItems`; `Rewards`; `QuestService.getRewardItems` | `QuestFinishRewardTemplateXmlProjectionExtractor.cs`; focused extractor tests | Implementation | No for selected unit | Medium | Selected. The shared extractor contract and docs need exclusive ownership. |
| B | Extended/class selectable reward analysis | `QuestTemplate.getExtendedRewards`; `getSelectableRewardByClass`; `QuestService.getRewardItems` | read-only | Java Analysis | Yes later | Low | Useful next support work before a broader extractor slice. |
| C | Mail packet splitting regression | mail packet classes | mail tests | Test Creation | Yes later | Low | Independent fallback if quest projection files are busy. |

Selected batch: orchestrator-only Candidate A. Sub-agents were not spawned because the selected work modifies the shared extractor and docs are orchestrator-owned.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/QuestFinishRewardTemplateXmlProjectionExtractor.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestFinishRewardTemplateXmlProjectionExtractorTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6WA-Completion.md`

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "QuestFinishRewardTemplateXmlProjectionExtractorTests|QuestFinishStaticRewardProjectionCompositionTests" --nologo` | Passed: 5 tests. |
| `dotnet test dotnetConversion/AionServer.slnx --nologo` | Passed: 2,141 tests. |

## Migration Parity Table - UOW-1087

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.templates.QuestTemplate` | `Aion.GameServer.Services.QuestFinishRewardTemplateXmlProjectionExtractor`; `QuestFinishRewardTemplateProjection` | Static Quest Reward Projection | Partial | Unit Tested / Regression Tested | Partial Parity | Extracts selected regular reward-group count, repeat count, challenge category, non-item fields, and now regular fixed/selectable item children. Full all-group projection, extended rewards, class rewards, bonus handlers, quest work items, target NPC context, JAXB lifecycle behavior, serialization, and production loader integration remain incomplete. |
| `com.aionemu.gameserver.model.templates.quest.Rewards` | `QuestFinishRewardGroupProjection`; `QuestFinishRewardItemTemplateProjection`; `QuestFinishRewardNonItemTemplateProjection` | Reward DTO Projection | Partial | Unit Tested / Regression Tested | Partial Parity | C# maps direct `reward_item` and `selectable_reward_item` children for the selected regular group while retaining non-item field projection. Java empty-list getter behavior is represented by empty C# lists. Extended reward fields and class reward lists are intentionally not populated. |
| `com.aionemu.gameserver.model.templates.quest.QuestItems` | `Aion.GameServer.Services.QuestFinishRewardItem` | Reward Item DTO | Partial | Unit Tested / Regression Tested | Partial Parity | Maps `item_id` to `ItemId` and `count` to `Count`; missing `count` defaults to `1`, matching Java field initialization / unmarshaller constructor behavior. Missing or invalid `item_id` throws during extraction instead of relying on JAXB primitive default `0`; this malformed-XML edge needs verification before production static-data load integration. |
| `com.aionemu.gameserver.services.QuestService.getRewardItems` | `QuestFinishRewardTemplateXmlProjectionExtractor`; existing `QuestFinishRewardPlanService.CreateRewardItemProjection` | Reward Item Planning Dependency | Partial | Unit Tested as static input only | Needs Verification | Static projection provides fixed/selectable regular reward data that can feed the existing non-live item projection service. This unit does not execute dialog-action selection, class selectable branches, extended selectable index fallback, bonus handler additions, `ItemService.addItem`, inventory mutation, packets, or Java runtime comparison. |
| `com.aionemu.gameserver.services.QuestService.finishQuest` | Future operation planner input from extracted item projection | Quest Finish Dependency | Partial | No live tests in this unit | Needs Verification | `HasItemRewards` can now trigger non-live item reward placeholders when composed, but production finish, quest-state mutation ordering, callback dispatch, persistence, player-thread ordering, and packet sends remain disabled/unverified. |

## Tests Added Or Updated

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `QuestFinishRewardTemplateXmlProjectionExtractorTests.CreateProjection_UsesRequestedRegularRewardGroupIndexForNonItemAndItemRewards` | Selected regular reward group maps fixed/selectable item lists and missing `count` defaults to `1`. | Source-reviewed from `QuestTemplate#getRewards`, `Rewards#getRewardItem`, `Rewards#getSelectableRewardItem`, and `QuestItems`; no Java runtime comparison. |
| `QuestFinishRewardTemplateXmlProjectionExtractorTests.CreateProjection_DefaultsMissingAndOutOfRangeRewardsToEmptyJavaRewards` | Missing and out-of-range selected groups do not produce item or non-item projections. | Source-reviewed Java empty reward behavior and reward-group dependency; no Java runtime comparison. |
| `QuestFinishRewardTemplateXmlProjectionExtractorTests.RealDataAudit_LoadsDefaultRegularNonItemRewardProjectionWithoutProductionWiring` | Confirms 8,043 templates, 5,527 default regular item-reward templates, 6,182 fixed reward item rows, 5,502 selectable reward item rows, and existing non-item totals. | Real Java XML loaded through the C# extractor; not a Java object/runtime comparison. |

## Remaining Risks

- Production `GameServerConnection.HandleDialogSelectAsync` still does not call the guard or reward projection planners.
- The extractor projects only the selected regular reward group.
- All-group item projection, extended rewards, class selectable rewards, bonus handler additions, quest work items, and target NPC context remain incomplete.
- Java malformed XML with missing `item_id` may JAXB-default to `0`; C# currently throws for missing or invalid `item_id`.
- Java `ItemService.addItem`, inventory capacity/stacking, packet emission, persistence, and failure behavior are not executed.
- Java `PlayerCommonData.setExp` live mutation remains absent, so custom rewards must stay disabled.
- Java runtime comparison remains blocked locally by Java 8 and missing Maven.

## Summary Metrics

- Total Java artifacts discovered: 5 in this unit
- Total artifacts ported: 1 partial static item reward projection slice
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5
- Total blocked artifacts: 7 blocked/partial categories: production socket integration, all-group item projection, extended reward projection, class reward projection, live quest finish, live XP mutation, and custom reward/mail execution
- Estimated overall migration completion: Phase 6 remains about 71% complete.

## Next Recommended Unit Of Work

Add a focused composition regression showing XML-derived regular item rewards flow through `QuestFinishRewardPlanService.CreateRewardItemProjection` and `QuestFinishOperationPlanService.CreatePlan` as non-live metadata/placeholders.

Keep extended rewards, class rewards, bonus handlers, and production quest-finish/custom reward execution disabled.

## Suggested Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Regular item reward composition regression | new focused composition test, possibly existing extractor test only if needed | Low/Medium | Next linear task; should avoid changing production runtime. |
| B | Read-only extended/class selectable analysis | no writes | Low | Safe support task before broadening the extractor. |
| C | Mail packet splitting regression | mail tests | Low | Independent fallback. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Add regular item reward composition regression and update docs | new focused test file or existing dedicated composition test; Phase 6 docs | `QuestFinishRewardTemplateXmlProjectionExtractor.cs` unless the test exposes a defect |
| Agent B (optional) | Read-only extended/class selectable Java analysis | read-only Java/C# inspection | all writes |

## Do Not Parallelize

- `QuestFinishRewardTemplateXmlProjectionExtractor.cs`: shared extractor contract.
- `QuestFinishRewardPlanService.cs`: shared reward planner contract.
- Phase 6 progress/handoff docs: orchestrator-owned.
