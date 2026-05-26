# Phase 6VZ Completion - UOW-1086 Static Reward Projection Composition

Date: May 26, 2026

## Unit Of Work

UOW-1086: `[Phase 6][UOW-1086] Compose static quest reward projection`

## Starting Point

- Continue from `docs/Phase-6VY-Completion.md`.
- UOW-1085 added static default regular non-item reward projection from Java quest XML.
- Production quest-finish, reward mutation, XP mutation, custom reward execution, mail execution, persistence, and packet sends remain disabled.

## Summary

UOW-1086 adds a focused composition regression proving XML-derived quest reportability and default regular non-item reward projection can flow through the non-live dialog guard and quest-finish operation planner.

The test keeps production wiring disabled and uses a Java-shaped XML fixture, not hand-built reward projection data.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Guard plus static non-item reward operation-plan composition | `CM_DIALOG_SELECT`; `QuestTemplate`; `Rewards`; `QuestService.finishQuest`; `QuestService.giveReward` | new focused composition test | Test Creation | Yes, isolated | Low/Medium | Selected. Proves UOW-1083 through UOW-1085 compose without production wiring. |
| B | Regular item/selectable reward XML projection | `QuestItems`; `QuestService.getRewardItems` | `QuestFinishRewardTemplateXmlProjectionExtractor.cs`; dedicated tests | Implementation | Yes later | Medium | Next likely unit; touches extractor contract. |
| C | Extended/class selectable reward analysis | `QuestTemplate.getExtendedRewards`; `getSelectableRewardByClass` | read-only | Java Analysis | Yes | Low | Safe support task before broadening extraction. |
| D | Mail packet splitting regression | mail packet classes | mail tests | Test Creation | Yes | Low | Independent fallback if quest projection files are busy. |

Selected batch: orchestrator-only Candidate A. Sub-agents were not spawned because the work is a single isolated test plus orchestrator-owned docs.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/QuestFinishStaticRewardProjectionCompositionTests.cs`
- `docs/QuestFinishProductionCallSite-Audit.md`
- `docs/QuestFinishRuntimeInput-Audit.md`
- `docs/QuestXpReward-Audit.md`
- `docs/QuestRewardSideEffects-Audit.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6VZ-Completion.md`

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "QuestFinishStaticRewardProjectionCompositionTests" --nologo` | Passed: 1 test. |
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` | Passed: 1,934 tests. |

## Migration Parity Table - UOW-1086

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT` | `QuestDialogAutoRewardGuardPlanService.CreatePlanFromTemplateSummary`; `QuestFinishStaticRewardProjectionCompositionTests` | Socket Guard / Test Composition | Partial | Unit Tested | Partial Parity | Test proves XML-derived reportable summary data can feed the non-live self auto-reward guard and then operation planning. Production `GameServerConnection.HandleDialogSelectAsync`, NPC dispatch, packet ordering, and threading remain unwired. |
| `com.aionemu.gameserver.model.templates.QuestTemplate` | `NearbyQuestTemplateXmlExtractor`; `QuestFinishRewardTemplateXmlProjectionExtractor`; `NearbyQuestTemplateSummary` | Static Quest Template Projection | Partial | Unit Tested | Partial Parity | Same Java-shaped XML fixture supplies both reportability and default regular non-item reward projection. Full reward groups, item rewards, selectable/class rewards, extended rewards, bonus handlers, quest work items, target NPC context, JAXB lifecycle behavior, and production loader integration remain incomplete. |
| `com.aionemu.gameserver.model.templates.quest.Rewards` | `QuestFinishRewardNonItemTemplateProjection`; `QuestFinishOperationDescriptor.RewardNonItemProjection` | Reward DTO Projection / Operation Metadata | Partial | Unit Tested | Partial Parity | XML-derived `gold`, `exp`, and `ap` become non-live operation descriptors. `extend_stigma` and `ccheck` surface as warnings because Java `giveReward` ignores them. Precision, serialization, and runtime Java comparison remain unverified. |
| `com.aionemu.gameserver.services.QuestService.giveReward` | `QuestFinishRewardPlanService.CreateNonItemRewardProjection`; `QuestFinishOperationPlanService.CreatePlan` | Reward Side-Effect Planner | Partial | Unit Tested as non-live composition | Needs Verification | Composition uses static projection data and emits non-live metadata only. It does not apply rates, mutate inventory/XP/title/AP/DP/GP/cube/warehouse, resolve target NPC names, send packets, or execute side effects. |
| `com.aionemu.gameserver.services.QuestService.finishQuest` | `QuestFinishOperationPlanService.CreatePlan` | Quest Finish Planner | Partial | Unit Tested | Needs Verification | Operation plan replaces the coarse reward mutation placeholder with detailed metadata before quest-state mutation, matching Java ordering at a planning level. Live quest-state mutation, callbacks, persistence, player-thread ordering, and packet sends remain disabled/unverified. |

## Tests Added Or Updated

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `QuestFinishStaticRewardProjectionCompositionTests.StaticNonItemRewardProjection_ComposesThroughGuardAndOperationPlanWithoutLiveSideEffects` | XML-derived reportability and non-item reward projection compose through guard and operation planning while all descriptors remain non-live. | Source-reviewed from `CM_DIALOG_SELECT.runImpl`, `QuestTemplate`, `Rewards`, `QuestService.finishQuest`, and `QuestService.giveReward`; no Java runtime comparison. |

## Remaining Risks

- Production `GameServerConnection.HandleDialogSelectAsync` still does not call the guard planner.
- The composition regression is test-only and uses fixture XML, not runtime static-data services.
- Item rewards, selectable rewards, class selectable rewards, extended rewards, bonus handler rewards, quest work items, target NPC context, and reward-group correction integration remain missing.
- Java `PlayerCommonData.setExp` live mutation remains absent, so custom rewards must stay disabled.
- Custom reward receipt/mail execution, packet ordering, and persistence remain gated and unverified.
- Java runtime comparison remains blocked locally by Java 8 and missing Maven.

## Summary Metrics

- Total Java artifacts discovered: 5 in this unit
- Total artifacts ported: 0 new production artifacts; 1 static reward/guard/operation composition regression
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5
- Total blocked artifacts: 6 blocked/partial categories: production socket integration, item reward projection, extended reward projection, live quest finish, live XP mutation, and custom reward/mail execution
- Estimated overall migration completion: Phase 6 remains about 71% complete.

## Next Recommended Unit Of Work

Extend `QuestFinishRewardTemplateXmlProjectionExtractor` to parse regular `reward_item` and `selectable_reward_item` lists for the selected regular reward group.

Keep class rewards, extended rewards, bonus handlers, and production quest-finish/custom reward execution disabled.

## Suggested Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Regular item/selectable reward XML projection | `QuestFinishRewardTemplateXmlProjectionExtractor.cs`; dedicated tests | Medium | Next linear task; use exclusive ownership of extractor. |
| B | Read-only extended/class selectable analysis | no writes | Low | Safe support task before a broader extractor slice. |
| C | Mail packet splitting regression | mail tests | Low | Independent fallback. |

## Do Not Parallelize

- `QuestFinishRewardTemplateXmlProjectionExtractor.cs`: shared extractor contract.
- `QuestFinishOperationPlanServiceTests.cs`: large shared test file; prefer new focused test files.
- Phase 6 progress/handoff/audit docs: orchestrator-owned.
