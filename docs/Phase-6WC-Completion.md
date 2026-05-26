# Phase 6WC Completion - UOW-1089 Extended Item Reward Projection

Date: May 26, 2026

## Unit Of Work

UOW-1089: `[Phase 6][UOW-1089] Extract extended quest item reward projection`

## Starting Point

- Continue from `docs/Phase-6WB-Completion.md`.
- UOW-1088 proved XML-derived regular item rewards compose through the non-live item and operation planners.
- Production quest-finish, live inventory mutation, XP mutation, custom reward execution, mail execution, persistence, and packet sends remain disabled.

## Summary

UOW-1089 extends static quest reward extraction to include item children under Java `<extended_rewards>`.

The extractor now projects extended fixed/selectable item lists into the existing `QuestFinishRewardItemTemplateProjection.ExtendedRewards` slot. Extended non-item reward fields are still not modeled.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Extended fixed/selectable reward XML projection | `QuestTemplate.getExtendedRewards`; `Rewards`; `QuestItems`; `QuestService.getRewardItems` | `QuestFinishRewardTemplateXmlProjectionExtractor.cs`; focused tests | Implementation | No for selected unit | Medium | Selected. The shared extractor contract needs exclusive ownership. |
| B | Class selectable reward XML projection | `QuestTemplate.getSelectableRewardByClass`; `use_class_reward`; `QuestService.getRewardItems` | same extractor; focused tests | Implementation | No with A | Medium | Shares extractor and should follow after extended item composition. |
| C | Mail packet splitting regression | mail tests | mail packet classes | Test Creation | Yes later | Low | Independent fallback. |

Selected batch: orchestrator-only Candidate A. Sub-agents were not spawned because the selected slice changes the shared extractor and docs are orchestrator-owned.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/QuestFinishRewardTemplateXmlProjectionExtractor.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestFinishRewardTemplateXmlProjectionExtractorTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6WC-Completion.md`

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "QuestFinishRewardTemplateXmlProjectionExtractorTests|QuestFinishRewardPlanServiceTests|QuestFinishStaticRewardProjectionCompositionTests" --nologo` | Passed: 24 tests. |
| `dotnet test dotnetConversion/AionServer.slnx --nologo` | Passed: 2,143 tests. |

## Migration Parity Table - UOW-1089

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.templates.QuestTemplate` | `QuestFinishRewardTemplateXmlProjectionExtractor`; `QuestFinishRewardTemplateProjection` | Static Quest Reward Projection | Partial | Unit Tested / Regression Tested | Partial Parity | Now extracts selected regular item rewards plus extended item rewards. Class selectable rewards, bonus handlers, quest work items, all-group projection, target NPC context, JAXB lifecycle behavior, serialization, and production loader integration remain incomplete. |
| `com.aionemu.gameserver.model.templates.quest.Rewards` | `QuestFinishRewardGroupProjection`; `QuestFinishRewardItemTemplateProjection`; `QuestFinishRewardNonItemTemplateProjection` | Reward DTO Projection | Partial | Unit Tested / Regression Tested | Partial Parity | Extended item child lists are projected into `ExtendedRewards`; selected regular non-item fields remain the only non-item slot. Extended non-item fields are explicitly missing, including 82 real XML templates with extended non-item attributes. |
| `com.aionemu.gameserver.model.templates.quest.QuestItems` | `Aion.GameServer.Services.QuestFinishRewardItem` | Reward Item DTO | Partial | Unit Tested / Regression Tested | Partial Parity | Extended and regular item projection reuse the same `item_id` / `count` mapping with missing count default `1`. Missing/invalid `item_id`, JAXB primitive defaulting, serialization, reflection, threading, and live inventory behavior remain unverified. |
| `com.aionemu.gameserver.services.QuestService.getRewardItems` | `QuestFinishRewardTemplateXmlProjectionExtractor`; existing `QuestFinishRewardPlanService.CreateRewardItemProjection` | Reward Item Planning Dependency | Partial | Unit Tested as static input only | Partial Parity | Static projection now supplies extended item rewards that the existing planner can add before regular rewards on the last repeat. This unit does not add a new operation composition test, live `ItemService.addItem`, class selectable/no-reward branches, bonus additions, warning logs, or Java runtime comparison. |
| `com.aionemu.gameserver.services.QuestService.giveReward` | `QuestFinishRewardTemplateXmlProjectionExtractor`; `QuestFinishRewardNonItemTemplateProjection` | Non-Item Reward Dependency | Partial | Regression Tested for regular only | Needs Verification | Java calls `giveReward` for both regular and extended rewards. C# still projects only selected regular non-item rewards; extended non-item rewards remain unmodeled due to a missing second non-item projection slot. |

## Tests Added Or Updated

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `QuestFinishRewardTemplateXmlProjectionExtractorTests.CreateProjection_ReadsExtendedRewardItemsWithoutRegularRewardGroup` | Extended fixed/selectable item lists project without a regular reward group, including count defaulting. | Source-reviewed from `QuestTemplate#getExtendedRewards`, `Rewards#getRewardItem`, `Rewards#getSelectableRewardItem`, and `QuestItems`; no Java runtime comparison. |
| `QuestFinishRewardTemplateXmlProjectionExtractorTests.RealDataAudit_LoadsDefaultRegularNonItemRewardProjectionWithoutProductionWiring` | Confirms 5,592 templates with any item projection, 5,527 default regular item templates, 233 extended item templates, 245 extended fixed item rows, and 250 extended selectable item rows. | Real Java XML loaded through the C# extractor; not a Java object/runtime comparison. |

## Remaining Risks

- Extended item rewards are static metadata only; last-repeat gating and dialog-action selection remain in the non-live planner.
- Extended non-item reward projection is missing for 82 real XML templates.
- Production `GameServerConnection.HandleDialogSelectAsync` still does not call the guard or reward projection planners.
- Live `ItemService.addItem`, inventory capacity/stacking, packet emission, persistence, and rollback behavior are not implemented in this path.
- Class selectable rewards, bonus handler additions, all reward groups, quest work items, and target NPC context remain incomplete.
- Java runtime comparison remains blocked locally by Java 8 and missing Maven.

## Summary Metrics

- Total Java artifacts discovered: 5 in this unit
- Total artifacts ported: 1 partial static extended item reward projection slice
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5
- Total blocked artifacts: 8 blocked/partial categories: production socket integration, live item reward mutation, extended non-item projection, all-group item projection, class reward projection, bonus handler rewards, live XP mutation, and custom reward/mail execution
- Estimated overall migration completion: Phase 6 remains about 71% complete.

## Next Recommended Unit Of Work

Add a focused non-live composition regression for XML-derived extended item rewards on the last repeat, including extended selectable index fallback.

Keep extended non-item rewards, class selectable rewards, bonus handlers, and production execution disabled.

## Suggested Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Extended item reward composition regression | `QuestFinishStaticRewardProjectionCompositionTests.cs` | Low/Medium | Next linear task; no production runtime changes expected. |
| B | Class selectable reward Java analysis | read-only | Low | Safe support work before the class extractor slice. |
| C | Mail packet splitting regression | mail tests | Low | Independent fallback. |

## Do Not Parallelize

- `QuestFinishRewardTemplateXmlProjectionExtractor.cs`: shared extractor contract.
- `QuestFinishRewardPlanService.cs`: shared reward planner contract.
- `QuestFinishOperationPlanService.cs`: shared operation planner contract.
- Phase 6 progress/handoff docs: orchestrator-owned.
