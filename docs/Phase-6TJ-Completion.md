# Phase 6TJ Completion - UOW-1018 Quest Finish Reward Descriptor Plan

## Scope

UOW-1018 stages a pure, non-live reward/work-item planner for `QuestService.finishQuest`.

The Java implementation remains the source of truth. This unit does not grant items, remove inventory, mutate AP/XP/title/cube/warehouse state, send packets, run callbacks, persist DAOs, or trigger live nearby refresh.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Reward-group correction and descriptors | `QuestService.validateAndFixRewardGroup`, `getRewardItems`, `giveReward`, `removeQuestWorkItems` | new service + tests | Service Port | Selected | Medium | Direct continuation from UOW-1017, but isolated from the main operation plan. |
| B | Quest callback dispatcher audit | `QuestEngine.onQuestCompleted`, handlers | docs-only | Java Analysis | Yes | Medium | Safe later if documentation file is separate. |
| C | Persistence contract analysis | quest/faction DAOs | docs-only | Java Analysis | Yes | Medium | Safe later if documentation file is separate. |
| D | Full reward XML projection | `QuestTemplate`, `Rewards`, class rewards, bonus handlers | dataholders/services/tests | Service/Data Port | No | High | Requires static-data model expansion and would overlap future reward planning. |

Selected batch: A only. File ownership was new service/test files plus Orchestrator-owned docs.

## Java Breadcrumbs

- `QuestService.finishQuest` calls `validateAndFixRewardGroup` before reward item selection.
- `validateAndFixRewardGroup` only acts when status is `REWARD`.
- A missing reward group defaults to `0` when Java reward groups exist.
- An out-of-range reward group is clamped to `rewardGroups.size() - 1`; for an empty list this is `-1`.
- Java work-item removal later removes all owned matching item ids, not the XML count.

## Deliverables

- Added `Aion.GameServer.Services.QuestFinishRewardPlanService`.
- Added `QuestFinishRewardTemplateProjection`, `QuestFinishRewardWorkItem`, descriptor records, and correction status enums.
- Added focused unit tests for reward-group correction and non-live descriptor ordering.
- Updated reward/work-item, quest-finish ordering, nearby audit, and Phase 6 progress docs.

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter QuestFinishRewardPlanServiceTests --nologo` | Passed: 8 |
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` | Passed: 1754 |

## Migration Parity Table - UOW-1018

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.QuestService.validateAndFixRewardGroup` | `Aion.GameServer.Services.QuestFinishRewardPlanService.CorrectRewardGroup` | Service / Reward Guard | Partial | Unit Tested | Partial Parity | Source-reviewed Java branch behavior is covered for reward-state guard, default-to-zero, clear-null-projection, and out-of-range clamping including empty-list `-1`. Java runtime comparison is still blocked, Java logging side effects are not modeled, and the Java null-reward branch is represented through a C# projection even though `QuestTemplate.getRewards()` usually returns an empty list. |
| `com.aionemu.gameserver.services.QuestService.getRewardItems` | `Aion.GameServer.Services.QuestFinishRewardPlanService` | Service / Reward Planner | Partial | Unit Tested | Needs Verification | C# emits a non-live item reward placeholder only. Fixed, selectable, class-specific, extended, bonus, dialog-action, repeat-boundary, and handler-bonus item selection are not implemented. |
| `com.aionemu.gameserver.services.QuestService.giveReward` | `Aion.GameServer.Services.QuestFinishRewardPlanService` | Service / Non-Item Reward Planner | Partial | Unit Tested | Needs Verification | C# emits a non-live non-item reward placeholder only. Kinah, XP, title, AP, DP, GP, cube, warehouse, rate scaling, and category-specific AP behavior are not implemented. |
| `com.aionemu.gameserver.services.QuestService.removeQuestWorkItems` | `Aion.GameServer.Services.QuestFinishRewardPlanService`; `Aion.GameServer.Services.QuestFinishRewardWorkItem` | Service / Inventory Mutation Plan | Partial | Unit Tested | Needs Verification | C# records item id and XML count as descriptor metadata, but does not remove inventory. Java removes all owned matching item ids using pre-completion quest status. |
| `com.aionemu.gameserver.model.templates.quest.QuestItems` | `Aion.GameServer.Services.QuestFinishRewardWorkItem` | DTO / XML Projection | Partial | Unit Tested | Partial Parity | Default count `1` is represented. This DTO is only a planner projection and is not wired to JAXB/XML quest data. |
| `com.aionemu.gameserver.model.templates.quest.Rewards` | `Aion.GameServer.Services.QuestFinishRewardTemplateProjection` | DTO / XML Projection | Partial | Unit Tested | Needs Verification | C# projection only carries reward group count and coarse reward-presence flags. Full Java reward fields are not ported. |
| `com.aionemu.gameserver.model.templates.quest.QuestWorkItems` | `Aion.GameServer.Services.QuestFinishRewardTemplateProjection.WorkItems` | DTO / XML Projection | Partial | Unit Tested | Needs Verification | C# can carry projected work items, but quest XML loading and live inventory behavior are missing. |

## Tests Added

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `CorrectRewardGroup_DefaultsMissingRewardGroupToFirstJavaRewardGroup` | Missing reward group defaults to `0` when rewards exist. | Source-reviewed from `validateAndFixRewardGroup`. |
| `CorrectRewardGroup_ClearsRewardGroupWhenJavaRewardsAreMissing` | Null projected rewards clear an existing reward group. | Source-reviewed Java null branch; runtime reachability needs verification because `getRewards()` returns empty list. |
| `CorrectRewardGroup_ClampsOutOfRangeRewardGroupLikeJava` | Negative, too-large, and empty-list reward groups clamp to `size - 1`. | Source-reviewed from Java branch. |
| `CorrectRewardGroup_IgnoresNonRewardStateLikeJavaGuard` | Non-`REWARD` status is ignored. | Source-reviewed from Java guard. |
| `CreatePlan_EmitsNoDescriptorsWhenJavaRewardGuardFails` | Non-`REWARD` status emits no reward/work-item descriptors even when projection flags are set. | Source-reviewed from Java `finishQuest` guard. |
| `CreatePlan_EmitsNonLiveRewardAndWorkItemDescriptors` | Descriptor order and non-live flags for correction, item reward, non-item reward, challenge task, and work-item removal placeholders. | Source-reviewed finish ordering; no Java runtime capture. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- Reward item selection remains coarse and does not inspect dialog actions, extended reward index, repeat boundary, class-specific rewards, or bonus handlers.
- Non-item reward mutation is descriptor-only; precision, rate scaling, title/AP/DP/GP/cube/warehouse behavior is unported.
- Work-item removal is descriptor-only; Java's all-owned-count removal and pre-completion status argument are not live.
- The planner is not yet composed into `QuestFinishOperationPlanService`.
- Java logging/warning side effects for malformed reward groups are not represented.

## Summary Metrics

- Total Java artifacts discovered: 7 in this unit
- Total artifacts ported: 1 partial service plus 3 projection/descriptor DTO shapes in this unit
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 7
- Total blocked artifacts: 5 blocked/partial categories: full reward XML projection, item selection, non-item mutation, live work-item removal, and Java runtime comparison
- Estimated overall migration completion: Phase 6 remains about 71%

## Next Recommended Unit Of Work

Compose `QuestFinishRewardPlanService` into `QuestFinishOperationPlanService` as the first pre-state-mutation sub-plan while keeping all descriptors non-live. Preserve Java ordering, ensure reward-group correction feeds the later state mutation, and keep inventory/reward side effects disabled.

## Next Unit Handoff

Start with this file, `docs/QuestFinishRewardWorkItem-Audit.md`, `docs/QuestFinishOrdering-Audit.md`, and `docs/PHASE-6-PROGRESS.md` Session 1018.

Recommended next slice:

1. Extend `QuestFinishOperationPlanService.CreatePlan` inputs with an optional `QuestFinishRewardTemplateProjection`.
2. Invoke `QuestFinishRewardPlanService.CreatePlan` before `QuestFinishStateMutationService.ApplyRewardCompletion`.
3. Feed the corrected `PlayerQuestState` into state completion.
4. Preserve descriptor order: reward correction/items/non-item rewards/challenge/work-item removal before quest-state mutation.
5. Add focused tests proving reward correction changes the completed state reward group and guard-failure paths do not emit reward descriptors.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Compose reward plan into operation plan | `QuestFinishOperationPlanService`, tests | Medium | Sequential with any operation-plan changes. |
| B | Callback dispatcher audit | new docs-only audit | Low | Can run in parallel if docs file is separate. |
| C | Persistence contract analysis | new docs-only audit | Medium | Can run in parallel if docs file is separate. |

## Do Not Parallelize

- Main quest-finish operation plan composition with reward live adapters.
- Inventory/AP/common-data/title/cube/warehouse mutation implementation.
- Phase progress/completion docs.
