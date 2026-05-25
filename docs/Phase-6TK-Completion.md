# Phase 6TK Completion - UOW-1019 Quest Finish Reward Plan Composition

## Scope

UOW-1019 composes the staged reward/work-item planner into `QuestFinishOperationPlanService`.

The Java implementation remains the source of truth. This unit keeps all reward, inventory, packet, callback, nearby-refresh, and persistence side effects non-live.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Compose reward planner into operation plan | `QuestService.finishQuest`, `validateAndFixRewardGroup`, `removeQuestWorkItems` | `QuestFinishOperationPlanService`, tests | Service Port | Selected | Medium | Direct next handoff from UOW-1018 and touches shared operation-plan ordering. |
| B | Callback dispatcher audit | `QuestEngine.onQuestCompleted`, handlers | docs-only | Java Analysis | Yes | Low | Safe later if written to a separate document. |
| C | Persistence contract analysis | `PlayerQuestListDAO`, `PlayerNpcFactionsDAO`, player store | docs-only | Java Analysis | Yes | Medium | Safe later if written to a separate document. |
| D | Live reward mutation adapters | `ItemService.addItem`, `giveReward`, inventory/AP/common data | services/repositories/tests | Service Port | No | High | Requires full static reward projection and side-effect homes. |

Selected batch: A only. File ownership was the operation plan service, operation plan tests, and Orchestrator-owned docs.

## Java Breadcrumbs

- Java finish guard returns before reward processing when quest state is missing or not `REWARD`.
- Reward-group correction, reward mutation, challenge-task notification, and work-item removal occur before `QuestState.setStatus(COMPLETE)`.
- Work-item removal remains before the quest status mutation and uses the pre-completion status.
- The update packet, completion callback, NPC faction completion, nearby refresh, and deferred persistence descriptors remain after quest-state mutation in Java order.

## Deliverables

- Extended `QuestFinishOperationPlanService.CreatePlan` with an optional `QuestFinishRewardTemplateProjection`.
- Translated reward-plan descriptors into operation-plan descriptors before quest-state mutation.
- Added operation descriptor metadata for staged work item item id/count.
- Preserved legacy aggregate reward/work-item placeholders when no projection is supplied.
- Added focused tests for composed reward descriptors, corrected reward group propagation, descriptor ordering, and guard-failure behavior.

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter QuestFinishOperationPlanServiceTests --nologo` | Passed: 7 |
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` | Passed: 1755 |

## Migration Parity Table - UOW-1019

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.QuestService.finishQuest` | `Aion.GameServer.Services.QuestFinishOperationPlanService.CreatePlan` | Service / Operation Plan | Partial | Unit Tested | Partial Parity | Optional reward projection now composes reward correction, item reward placeholder, non-item reward placeholder, challenge-task placeholder, work-item removal descriptor, quest-state mutation, packet, callback, optional NPC faction completion, nearby refresh, and deferred persistence in Java order. Still no live mutation, packet send, callback runtime, DAO writes, or Java runtime comparison. |
| `com.aionemu.gameserver.services.QuestService.validateAndFixRewardGroup` | `Aion.GameServer.Services.QuestFinishRewardPlanService.CorrectRewardGroup` via operation plan | Service / Reward Guard | Partial | Unit Tested | Partial Parity | Corrected reward group now feeds the staged quest completion state. Java logging and runtime comparison remain missing. |
| `com.aionemu.gameserver.services.QuestService.getRewardItems` | `Aion.GameServer.Services.QuestFinishOperationPlanService` via `QuestFinishRewardPlanService` | Service / Reward Planner | Partial | Unit Tested | Needs Verification | Operation plan can place item reward placeholders before state mutation. Full fixed/selectable/class/extended/bonus reward selection is not ported. |
| `com.aionemu.gameserver.services.QuestService.giveReward` | `Aion.GameServer.Services.QuestFinishOperationPlanService` via `QuestFinishRewardPlanService` | Service / Non-Item Reward Planner | Partial | Unit Tested | Needs Verification | Operation plan can place non-item reward placeholders before state mutation. Kinah, XP, title, AP, DP, GP, cube, warehouse, rates, and precision behavior remain unported. |
| `com.aionemu.gameserver.services.QuestService.removeQuestWorkItems` | `Aion.GameServer.Services.QuestFinishOperationPlanService`; `QuestFinishRewardWorkItem` | Service / Inventory Mutation Plan | Partial | Unit Tested | Needs Verification | Operation descriptors carry item id/count metadata before state mutation. Java removes all owned matching item ids with pre-completion status; no live C# removal exists. |

## Tests Added Or Updated

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `CreatePlan_ComposesRewardProjectionBeforeStateMutation` | Reward correction, item reward, non-item reward, challenge-task, and work-item descriptors occur before quest-state mutation; corrected reward group survives completion; item id/count metadata is retained. | Source-reviewed Java `finishQuest` ordering. |
| `CreatePlan_ReturnsNoDescriptorsWhenQuestFinishGuardFails` | Supplying a reward projection does not bypass Java's missing/non-`REWARD` finish guard. | Source-reviewed Java early return. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- Full reward template XML projection is missing.
- Item rewards are still placeholders and do not select fixed/selectable/class-specific/extended/bonus rewards.
- Non-item rewards are still placeholders and do not mutate kinah, XP, title, AP, DP, GP, cube, or warehouse state.
- Work-item removal is still descriptor-only and does not perform Java's all-owned-count inventory decrement.
- Production packet sends, quest callbacks, nearby-refresh sends, and DAO writes remain disabled.

## Summary Metrics

- Total Java artifacts discovered: 5 in this unit
- Total artifacts ported: 1 operation-plan composition update in this unit
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 5
- Total blocked artifacts: 5 blocked/partial categories: reward XML projection, item selection, non-item mutation, live work-item removal, and Java runtime comparison
- Estimated overall migration completion: Phase 6 remains about 71%

## Next Recommended Unit Of Work

Start the next prerequisite for live quest-finish execution by auditing `QuestEngine.onQuestCompleted` handler dispatch, including handler registration shape, callback ordering, and whether callbacks can mutate quest state or send packets after `SM_QUEST_ACTION`.

## Next Unit Handoff

Start with this file, `docs/Phase-6TJ-Completion.md`, `docs/QuestFinishOrdering-Audit.md`, `docs/QuestFinishRewardWorkItem-Audit.md`, and `docs/PHASE-6-PROGRESS.md` Session 1019.

Recommended next slice:

1. Source-audit `QuestEngine.onQuestCompleted` and the relevant handler collections.
2. Document callback ordering after the quest update packet and before NPC faction completion.
3. Identify callback side effects that could affect quest state, inventory, packets, or nearby markers.
4. Do not implement live callback dispatch yet.
5. Add a conservative parity table and handoff for a future non-live callback descriptor/dispatcher plan.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Callback dispatcher audit | new docs-only audit | Low | Best next slice. |
| B | Persistence contract analysis | new docs-only audit | Medium | Can proceed separately if docs do not overlap. |
| C | Full reward XML projection planning | new docs-only/static-data audit | Medium | Avoid touching operation plan in parallel. |

## Do Not Parallelize

- Live reward/inventory mutation adapters.
- Quest-finish operation plan changes while callback or persistence descriptors are being added.
- Phase progress/completion docs.
