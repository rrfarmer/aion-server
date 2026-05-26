# Phase 6VY Completion - UOW-1085 Quest Reward Non-Item XML Projection

Date: May 26, 2026

## Unit Of Work

UOW-1085: `[Phase 6][UOW-1085] Extract quest non-item reward projection`

## Starting Point

- Continue from `docs/Phase-6VX-Completion.md`.
- UOW-1084 added a non-live adapter from `NearbyQuestTemplateSummary?` into the dialog auto-reward guard.
- Production quest-finish, reward mutation, XP mutation, custom reward execution, mail execution, persistence, and packet sends remain disabled.

## Summary

UOW-1085 adds the first static XML-to-`QuestFinishRewardTemplateProjection` bridge for Java quest rewards.

`QuestFinishRewardTemplateXmlProjectionExtractor` reads current Java quest XML and projects default regular reward-group non-item attributes into the existing C# `QuestFinishRewardNonItemTemplateProjection` record.

The unit is intentionally static and non-live. It does not wire the extractor into production socket handling or operation planning.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Default regular non-item reward XML projection | `QuestTemplate`; `Rewards`; `QuestService.giveReward` | new extractor and new tests | Implementation / Test Creation | Yes, isolated | Low/Medium | Selected. Avoids item-selection complexity and production wiring. |
| B | Regular item/selectable reward XML projection | `QuestItems`; `QuestService.getRewardItems` | future extractor/test files | Implementation | Yes later | Medium | Needs item/class selectable shape and warning coverage. |
| C | Guard plus static reward operation-plan composition | `CM_DIALOG_SELECT`; `QuestService.finishQuest` | existing operation-plan tests | Test Creation | No with A | Medium | Would touch shared existing test file and depends on extractor contract. |
| D | Mail list packet splitting tests | mail packet classes | mail tests | Test Creation | Yes | Low | Safe but less directly connected to current quest-finish reward projection. |
| E | Server-time conversion vectors | custom reward runtime input tests | time conversion tests | Test Creation | Yes | Low/Medium | Useful later; avoid named-zone claims without runtime comparison. |

Selected batch: orchestrator-only implementation of Candidate A. Sub-agents were not spawned because the new extractor contract and tests needed one coherent review path.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/QuestFinishRewardTemplateXmlProjectionExtractor.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestFinishRewardTemplateXmlProjectionExtractorTests.cs`
- `docs/QuestFinishProductionCallSite-Audit.md`
- `docs/QuestFinishRuntimeInput-Audit.md`
- `docs/QuestXpReward-Audit.md`
- `docs/QuestRewardSideEffects-Audit.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6VY-Completion.md`

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "QuestFinishRewardTemplateXmlProjectionExtractorTests" --nologo` | Passed: 4 tests. |
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` | Passed: 1,933 tests. |

## Migration Parity Table - UOW-1085

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.templates.QuestTemplate` | `Aion.GameServer.Services.QuestFinishRewardTemplateXmlProjectionExtractor` | Static Quest Reward Projection | Partial | Unit Tested / Regression Tested | Partial Parity | Extracts default regular reward group count, repeat count, challenge-task category, and non-item reward attributes from Java quest XML. It does not project item rewards, selectable rewards, class rewards, extended rewards, bonus handlers, quest work items, target NPC context, JAXB lifecycle behavior, serialization, or production loader integration. |
| `com.aionemu.gameserver.model.templates.quest.Rewards` | `Aion.GameServer.Services.QuestFinishRewardNonItemTemplateProjection` via `QuestFinishRewardTemplateXmlProjectionExtractor` | Reward DTO Projection | Partial | Unit Tested / Regression Tested | Partial Parity | Maps `gold`, `exp`, `ap`, `dp`, `gp`, `title`, `extend_inventory`, `extend_stigma`, `ccheck`, and `icheck` for the selected regular reward group. Java `giveReward` ignores `extend_stigma`, `ccheck`, and `icheck`; C# retains them so existing warning paths can surface the ignored XML fields. Precision uses `long` for kinah and `int` for other fields, matching Java types, but runtime Java comparison is absent. |
| `com.aionemu.gameserver.services.QuestService.giveReward` | `QuestFinishRewardTemplateXmlProjectionExtractor`; existing `QuestFinishRewardPlanService.CreateNonItemRewardProjection` | Reward Side-Effect Dependency | Partial | Unit Tested as static input only | Needs Verification | Static projection can feed existing non-live descriptors, but this unit does not compose it into operation planning, apply rates, mutate inventory/XP/title/AP/DP/GP/cube/warehouse, resolve target NPC names, send packets, or verify Java runtime ordering. |
| `com.aionemu.gameserver.services.QuestService.finishQuest` | Future operation planner input from extracted projection | Quest Finish Dependency | Partial | No Tests in this unit | Needs Verification | The extractor is not wired to `QuestFinishOperationPlanService`, guard plans, production socket handling, quest-state completion, callback dispatch, threading, or persistence. |

## Tests Added Or Updated

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `QuestFinishRewardTemplateXmlProjectionExtractorTests.ExtractDefaultRegularNonItemProjections_ReadsJavaRewardsAttributes` | Fixture XML maps Java non-item reward attributes to C# projection fields and keeps item rewards disabled. | Source-reviewed from `QuestTemplate#getRewards`, `Rewards`, and `QuestService.giveReward`; no Java runtime comparison. |
| `QuestFinishRewardTemplateXmlProjectionExtractorTests.CreateProjection_UsesRequestedRegularRewardGroupIndexWithoutParsingItemRewards` | Requested regular reward group selects that group's non-item fields while item reward children remain unparsed. | Source-reviewed selected reward-group behavior; no live reward execution. |
| `QuestFinishRewardTemplateXmlProjectionExtractorTests.CreateProjection_DefaultsMissingAndOutOfRangeRewardsToEmptyJavaRewards` | Missing or out-of-range selected reward groups produce no non-item projection. | Source-reviewed Java empty `Rewards` fallback shape; warning/log behavior is not modeled here. |
| `QuestFinishRewardTemplateXmlProjectionExtractorTests.RealDataAudit_LoadsDefaultRegularNonItemRewardProjectionWithoutProductionWiring` | Current real XML counts and default-group kinah/XP totals. | Real XML loaded through C# extractor; not Java runtime object comparison. |

## Real-Data Counts

- Templates: 8,043
- Default regular reward groups with non-item fields: 6,832
- Default regular reward groups carrying XML fields ignored by Java `giveReward`: 103
- Challenge-task templates: 174
- Default regular reward-group kinah total: 3,425,802,649
- Default regular reward-group XP total: 20,544,638,479

## Remaining Risks

- Production `GameServerConnection.HandleDialogSelectAsync` still does not call the guard planner.
- The new extractor is not wired into guard-to-operation planning.
- Item rewards, selectable rewards, class selectable rewards, extended rewards, bonus handler rewards, quest work items, target NPC context, and reward-group correction integration remain missing.
- Java `PlayerCommonData.setExp` live mutation remains absent, so custom rewards must stay disabled.
- Custom reward receipt/mail execution, packet ordering, and persistence remain gated and unverified.
- Java runtime comparison remains blocked locally by Java 8 and missing Maven.

## Summary Metrics

- Total Java artifacts discovered: 4 in this unit
- Total artifacts ported: 1 partial static non-item reward projection extractor
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4
- Total blocked artifacts: 6 blocked/partial categories: production socket integration, item reward projection, extended reward projection, live quest finish, live XP mutation, and custom reward/mail execution
- Estimated overall migration completion: Phase 6 remains about 71% complete.

## Next Recommended Unit Of Work

Compose `QuestFinishRewardTemplateXmlProjectionExtractor` output with the existing non-live dialog guard and operation planners in a focused test, or extend the extractor to parse regular `reward_item` and `selectable_reward_item` lists.

Keep production quest-finish/custom reward execution disabled.

## Suggested Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Compose static non-item projection through guard and operation planners | `QuestFinishOperationPlanServiceTests.cs` or a new focused test file | Medium | Touches existing planner tests if not isolated; keep production code unchanged. |
| B | Add regular item/selectable reward XML projection | `QuestFinishRewardTemplateXmlProjectionExtractor.cs`; dedicated tests | Medium | Must preserve Java selection/warning behavior; avoid class rewards until regular lists are stable. |
| C | Read-only Java analysis for extended rewards and class selectable rewards | no writes | Low | Safe support task before implementing next extractor slice. |
| D | Mail packet splitting regression | mail test file only | Low | Independent fallback if quest projection files are busy. |

## Do Not Parallelize

- `QuestFinishRewardTemplateXmlProjectionExtractor.cs`: shared extractor contract; only one implementation agent should edit it at a time.
- `QuestFinishOperationPlanServiceTests.cs`: large shared test file; use exclusive ownership or a new dedicated test file.
- Phase 6 progress/handoff/audit docs: orchestrator-owned.
