# Phase 6WE Completion - UOW-1091 Class Selectable Reward Projection

Date: May 26, 2026

## Unit Of Work

UOW-1091: `[Phase 6][UOW-1091] Extract class quest reward projection`

## Summary

UOW-1091 extends static quest reward extraction to include Java class-selectable reward lists and `use_class_reward` flags.

The slice is static and non-live. It does not execute class reward selection, mutate inventory, or enable production quest finish.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/QuestFinishRewardTemplateXmlProjectionExtractor.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestFinishRewardTemplateXmlProjectionExtractorTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6WE-Completion.md`

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "QuestFinishRewardTemplateXmlProjectionExtractorTests|QuestFinishRewardPlanServiceTests|QuestFinishStaticRewardProjectionCompositionTests" --nologo` | Passed: 26 tests. |
| `dotnet test dotnetConversion/AionServer.slnx --nologo` | Passed: 2,145 tests. |

## Migration Parity Table - UOW-1091

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.templates.QuestTemplate` | `QuestFinishRewardTemplateXmlProjectionExtractor`; `QuestFinishRewardTemplateProjection` | Static Quest Reward Projection | Partial | Unit Tested / Regression Tested | Partial Parity | Now extracts selected regular item rewards, extended item rewards, class-selectable reward lists, and `use_class_reward` flags. Extended non-item rewards, bonus handlers, quest work items, all-group projection, target NPC context, JAXB lifecycle behavior, serialization, and production loader integration remain incomplete. |
| `com.aionemu.gameserver.model.templates.quest.QuestItems` | `Aion.GameServer.Services.QuestFinishRewardItem` | Reward Item DTO | Partial | Unit Tested / Regression Tested | Partial Parity | Class-selectable reward rows reuse the same `item_id` / `count` mapping with missing count default `1`. Missing/invalid `item_id`, JAXB primitive defaulting, serialization, reflection, threading, and live inventory behavior remain unverified. |
| `com.aionemu.gameserver.model.PlayerClass` | `QuestFinishRewardTemplateXmlProjectionExtractor` class reward element map | Enum / Static Mapping Dependency | Partial | Unit Tested | Partial Parity | Maps Java reward element buckets to advanced class strings used by the C# player model. Starting classes such as `WARRIOR`, `SCOUT`, `MAGE`, `PRIEST`, `ENGINEER`, and `ARTIST` intentionally have no class-selectable reward bucket because Java `getSelectableRewardByClass` returns empty for them. |
| `com.aionemu.gameserver.services.QuestService.getRewardItems` | `QuestFinishRewardTemplateXmlProjectionExtractor`; existing `QuestFinishRewardPlanService.CreateRewardItemProjection` | Reward Item Planning Dependency | Partial | Unit Tested as static input only | Partial Parity | Static projection now supplies class-selectable reward lists and flags that the existing planner can consume on last/every-repeat branches. This unit does not add operation composition coverage, live `ItemService.addItem`, bonus additions, warning logs, or Java runtime comparison. |
| `com.aionemu.gameserver.services.QuestService.finishQuest` | Future operation planner input from extracted class projection | Quest Finish Dependency | Partial | No live tests in this unit | Needs Verification | `use_class_reward` metadata is projected but production finish, live item mutation, quest-state mutation ordering, callback dispatch, persistence, player-thread ordering, and packet sends remain disabled/unverified. |

## Tests Added Or Updated

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `QuestFinishRewardTemplateXmlProjectionExtractorTests.CreateProjection_ReadsClassSelectableRewardsAndUseClassRewardFlags` | Class reward XML buckets map to the same advanced class names Java returns, missing count defaults to `1`, and `use_class_reward="2"` sets single-time class rewards. | Source-reviewed from `QuestTemplate#getSelectableRewardByClass`, `QuestItems`, and `isSingleTimeClassReward`; no Java runtime comparison. |
| `QuestFinishRewardTemplateXmlProjectionExtractorTests.RealDataAudit_LoadsDefaultRegularNonItemRewardProjectionWithoutProductionWiring` | Confirms 5,643 templates with any item projection, 80 templates with class-selectable rewards, 2,324 class-selectable item rows, 69 `use_class_reward="1"` templates, and 5 `use_class_reward="2"` templates. | Real Java XML loaded through the C# extractor; not a Java object/runtime comparison. |

## Remaining Risks

- Class-selectable rewards are static metadata only; last/every-repeat gating and player-class selection remain in the non-live planner.
- Six real templates have class-selectable rows without `use_class_reward` set to `1` or `2`; Java would not choose those rows through the class branch.
- Extended non-item reward projection is still missing for 82 real XML templates.
- Production `GameServerConnection.HandleDialogSelectAsync` still does not call the guard or reward projection planners.
- Live `ItemService.addItem`, inventory capacity/stacking, packet emission, persistence, and rollback behavior are not implemented in this path.
- Bonus handler additions, all reward groups, quest work items, and target NPC context remain incomplete.
- Java runtime comparison remains blocked locally by Java 8 and missing Maven.

## Summary Metrics

- Total Java artifacts discovered: 5 in this unit
- Total artifacts ported: 1 partial static class-selectable reward projection slice
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5
- Total blocked artifacts: 8 blocked/partial categories: production socket integration, live item reward mutation, extended non-item projection, all-group item projection, class reward composition, bonus handler rewards, live XP mutation, and custom reward/mail execution
- Estimated overall migration completion: Phase 6 remains about 71% complete.

## Next Recommended Unit Of Work

Add a focused non-live composition regression for XML-derived class-selectable rewards, covering both `use_class_reward="2"` last-repeat behavior and `use_class_reward="1"` every-repeat behavior.

Keep extended non-item rewards, bonus handlers, and production execution disabled.

## Suggested Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Class selectable reward composition regression | `QuestFinishStaticRewardProjectionCompositionTests.cs` | Low/Medium | Next linear task; no production runtime changes expected. |
| B | Extended non-item reward model analysis | read-only | Low | Needed before adding a second non-item reward slot. |
| C | Mail packet splitting regression | mail tests | Low | Independent fallback. |

## Do Not Parallelize

- `QuestFinishRewardTemplateXmlProjectionExtractor.cs`: shared extractor contract.
- `QuestFinishRewardPlanService.cs`: shared reward planner contract.
- `QuestFinishOperationPlanService.cs`: shared operation planner contract.
- Phase 6 progress/handoff docs: orchestrator-owned.
