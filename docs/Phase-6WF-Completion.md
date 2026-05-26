# Phase 6WF Completion - UOW-1092 Class Selectable Reward Composition

Date: May 26, 2026

## Unit Of Work

UOW-1092: `[Phase 6][UOW-1092] Compose class quest reward projection`

## Summary

UOW-1092 adds non-live composition regressions proving XML-derived class-selectable quest rewards flow through the existing item reward planner and operation planner.

No production execution path was enabled.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/QuestFinishRewardTemplateXmlProjectionExtractor.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestFinishStaticRewardProjectionCompositionTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestFinishRewardTemplateXmlProjectionExtractorTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6WF-Completion.md`

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "QuestFinishStaticRewardProjectionCompositionTests|QuestFinishRewardTemplateXmlProjectionExtractorTests|QuestFinishRewardPlanServiceTests" --nologo` | Passed: 28 tests. |
| `dotnet test dotnetConversion/AionServer.slnx --nologo` | Passed: 2,147 tests. |

## Migration Parity Table - UOW-1092

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.QuestService.getRewardItems` | `QuestFinishRewardPlanService.CreateRewardItemProjection`; `QuestFinishOperationPlanService` item descriptors | Reward Item Planner | Partial | Unit Tested | Partial Parity | Tests cover Java single-time class reward selection on the last repeat, every-repeat no-reward class selection from `extendedRewardIndex - 8`, and class selection replacing regular selectable rewards. Warning logs, bonus additions, live `ItemService.addItem`, and Java runtime comparison remain incomplete. |
| `com.aionemu.gameserver.services.QuestService.finishQuest` | `QuestFinishOperationPlanService.CreatePlan`; `QuestFinishStaticRewardProjectionCompositionTests` | Quest Finish Planner | Partial | Unit Tested | Partial Parity | Class reward descriptors compose before the item placeholder and quest-state mutation. Production finish, live item mutation, callback dispatch, persistence, player-thread ordering, and packet sends remain disabled/unverified. |
| `com.aionemu.gameserver.model.templates.QuestTemplate` | `QuestFinishRewardTemplateXmlProjectionExtractor`; `QuestFinishRewardTemplateProjection` | Static Quest Template Projection | Partial | Unit Tested / Regression Tested | Partial Parity | Extractor now preserves an empty regular group when class rewards require Java's regular-branch anchor. Extended non-item rewards, bonus handlers, quest work items, all reward groups, target NPC context, JAXB lifecycle behavior, serialization, and production loader integration remain incomplete. |
| `com.aionemu.gameserver.model.PlayerClass` | `QuestFinishRewardTemplateXmlProjectionExtractor` class reward map; `QuestFinishRewardItemProjectionDescriptor.PlayerClass` | Enum / Static Mapping Dependency | Partial | Unit Tested | Partial Parity | Tests exercise `RANGER` and `CLERIC` mappings through operation descriptors. Other mapped advanced classes are covered by extractor fixture/audit only; starting-class empty behavior remains source-reviewed. |
| `com.aionemu.gameserver.model.templates.quest.QuestItems` | `Aion.GameServer.Services.QuestFinishRewardItem`; `QuestFinishOperationDescriptor.ItemId/Count` | Reward Item DTO | Partial | Unit Tested | Partial Parity | Class reward item IDs/counts become non-live operation metadata. Missing/invalid `item_id`, JAXB primitive defaulting, serialization, reflection, threading, and live inventory behavior remain unverified. |

## Tests Added Or Updated

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `QuestFinishStaticRewardProjectionCompositionTests.StaticClassSelectableRewardProjection_ComposesOnLastRepeatInsteadOfRegularSelectableReward` | Last repeat with `use_class_reward="2"` uses the class selectable item index from dialog action and does not emit the regular selectable item. | Source-reviewed from `QuestService.getRewardItems`, `QuestTemplate.isSingleTimeClassReward`, and `getSelectableRewardByClass`; no Java runtime comparison. |
| `QuestFinishStaticRewardProjectionCompositionTests.StaticClassSelectableRewardProjection_ComposesNoRewardEveryRepeatSelectionFromExtendedIndex` | Every-repeat class reward uses `extendedRewardIndex - 8` under `SELECTED_QUEST_NOREWARD`, even before last repeat. | Source-reviewed from `QuestService.getRewardItems` and `QuestTemplate.isClassRewardOnEveryRepeat`; no Java runtime comparison. |
| `QuestFinishRewardTemplateXmlProjectionExtractorTests.RealDataAudit_LoadsDefaultRegularNonItemRewardProjectionWithoutProductionWiring` | Keeps regular item count scoped to groups with actual regular item children after empty class-anchor groups were added. | Real Java XML loaded through the C# extractor; not a Java object/runtime comparison. |

## Remaining Risks

- Class reward warnings for missing player class or out-of-range selection are covered in planner unit tests but not yet in XML composition tests.
- Six real templates have class-selectable rows without `use_class_reward` set to `1` or `2`; Java would not choose those rows through the class branch.
- Extended non-item reward projection is still missing for 82 real XML templates.
- Production `GameServerConnection.HandleDialogSelectAsync` still does not call the guard or reward projection planners.
- Live `ItemService.addItem`, inventory capacity/stacking, packet emission, persistence, and rollback behavior are not implemented in this path.
- Bonus handler additions, all reward groups, quest work items, and target NPC context remain incomplete.
- Java runtime comparison remains blocked locally by Java 8 and missing Maven.

## Summary Metrics

- Total Java artifacts discovered: 5 in this unit
- Total artifacts ported: 0 new production artifacts; 1 static class reward composition regression slice
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5
- Total blocked artifacts: 8 blocked/partial categories: production socket integration, live item reward mutation, extended non-item projection, all-group item projection, class reward warning composition, bonus handler rewards, live XP mutation, and custom reward/mail execution
- Estimated overall migration completion: Phase 6 remains about 71% complete.

## Next Recommended Unit Of Work

Analyze and model extended non-item rewards by adding a second non-item projection slot or an explicit regular/extended grouping model.

Keep bonus handlers and production execution disabled.

## Suggested Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Extended non-item reward model analysis | read-only or docs only | Low/Medium | Recommended next step before changing projection contracts. |
| B | Class reward warning XML composition tests | focused composition tests | Low | Can be done without production changes. |
| C | Mail packet splitting regression | mail tests | Low | Independent fallback. |

## Do Not Parallelize

- `QuestFinishRewardTemplateXmlProjectionExtractor.cs`: shared extractor contract.
- `QuestFinishRewardPlanService.cs`: shared reward planner contract.
- `QuestFinishOperationPlanService.cs`: shared operation planner contract.
- Phase 6 progress/handoff docs: orchestrator-owned.
