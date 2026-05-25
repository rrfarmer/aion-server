# Phase 6TW Completion - UOW-1031 Reward Projection Composition

Date: May 25, 2026

## Parallel Work Discovery

| Candidate | Scope | Java Source | C# Target | Type | Selected | Risk | Notes |
|---|---|---|---|---|---|---|---|
| A | Compose detailed reward item projection | `QuestService.finishQuest`; `QuestService.getRewardItems` | `QuestFinishOperationPlanService` | Service Composition | Yes | Medium | Direct continuation from UOW-1030. Keeps all item effects non-live. |
| B | Add non-item reward planner details | `QuestService.giveReward`; `Rewards` | reward planner DTOs | Service / Reward Planner | No | Medium | Good next slice, but item projection needed operation-plan composition first. |
| C | Persistence failure-ordering policy audit | `PlayerQuestListDAO`, `PlayerNpcFactionsDAO` | docs/planner policy | Java Analysis | No | Low | Still pending before DAO writes. |
| D | Live inventory item add | `ItemService.addItem` | inventory service integration | Service Port | No | High | Blocked by real XML loading, inventory edge cases, packet side effects, and failure policy. |

## Completed

- Extended `QuestFinishRewardTemplateProjection` with optional detailed item projection context:
  - `ItemProjection`
  - `DialogActionId`
  - `ExtendedRewardIndex`
  - `RewardRepeatCount`
  - `PlayerClass`
- Added operation-plan actions:
  - `ItemRewardProjection`
  - `ItemRewardProjectionWarning`
- Extended `QuestFinishOperationDescriptor` to carry:
  - `RewardItemProjection`
  - `RewardItemProjectionWarning`
- Composed `QuestFinishRewardPlanService.CreateRewardItemProjection` into `QuestFinishOperationPlanService`.
- Ensured detailed item descriptors are inserted before the existing coarse `ItemRewardPlaceholder`.
- Ensured corrected reward group feeds detailed item projection before quest-state mutation.
- Kept all descriptors non-live; no inventory mutation, bonus handler execution, packet send, or DAO write is enabled.
- Updated reward/order/nearby audit docs and `docs/PHASE-6-PROGRESS.md`.

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "QuestFinishOperationPlanServiceTests|QuestFinishRewardPlanServiceTests" --nologo` | Passed: 27 |
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` | Passed: 1788 |

## Migration Parity Table - Session 1031

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.QuestService.finishQuest` | `Aion.GameServer.Services.QuestFinishOperationPlanService.CreatePlan` | Service / Operation Plan | Partial | Unit Tested | Partial Parity | Detailed item reward projection descriptors now sit before the coarse item placeholder and before quest-state mutation, matching Java's reward-before-state ordering at descriptor level. No live `ItemService.addItem`, packet send, callback execution, persistence, or Java runtime comparison exists. |
| `com.aionemu.gameserver.services.QuestService.getRewardItems` | `QuestFinishRewardPlanService.CreateRewardItemProjection` composed through operation plan | Service / Reward Item Planner | Partial | Unit Tested | Partial Parity | Corrected reward group can feed detailed fixed/selectable/extended projection through quest-finish planning. Still no real XML loading, bonus handler execution, Java logging, inventory mutation, or runtime comparison. |
| `com.aionemu.gameserver.model.templates.quest.Rewards` | `QuestFinishRewardItemTemplateProjection`; `QuestFinishRewardTemplateProjection.ItemProjection` | DTO / XML Projection | Partial | Unit Tested | Needs Verification | Operation-plan input can carry detailed item reward group projections, but only as caller-provided metadata. Non-item reward fields remain unprojected in operation descriptors. |
| `com.aionemu.gameserver.model.templates.quest.QuestItems` | `QuestFinishRewardItemProjectionDescriptor` nested in `QuestFinishOperationDescriptor` | DTO / Operation Descriptor | Partial | Unit Tested | Partial Parity | Item id/count metadata survives operation planning. XML loading, item validation, stack behavior, and live inventory add behavior remain missing. |
| `com.aionemu.gameserver.questEngine.QuestEngine.onBonusApplyEvent` | `QuestFinishOperationDescriptor.RewardItemProjectionWarning` | Handler Dependency | Not Started | Unit Tested | Needs Verification | Bonus handler dependency now survives operation planning as a warning descriptor. No handler execution exists. |
| `com.aionemu.gameserver.services.BonusService.getQuestBonus` | `QuestFinishOperationDescriptor.RewardItemProjectionWarning` | Service Dependency | Not Started | Unit Tested | Needs Verification | Bonus item resolution remains unsupported and non-live. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `QuestFinishOperationPlanServiceTests.CreatePlan_ComposesDetailedRewardItemProjectionBeforeCoarsePlaceholder` | Unit | `QuestService.finishQuest`; `getRewardItems`; `validateAndFixRewardGroup` | Corrected reward group feeds detailed extended/fixed/selectable item descriptors before the coarse item placeholder and before state mutation. | Source-reviewed Java ordering plus UOW-1030 projection tests. | No live inventory mutation or Java runtime capture. |
| `QuestFinishOperationPlanServiceTests.CreatePlan_ComposesRewardProjectionWarningsBeforeCoarsePlaceholder` | Unit | `QuestEngine.onBonusApplyEvent`; `BonusService.getQuestBonus`; `getRewardItems` | Bonus-handler projection warning survives operation planning before the coarse item placeholder. | Source-reviewed bonus dependency. | No bonus handler runtime. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- Detailed projection is caller-provided; real quest XML reward loading is still absent.
- Operation descriptors are not live and must not be treated as item grants.
- Bonus rewards remain warning-only.
- Non-item reward mutation is still outside the operation-plan detailed descriptors.
- Java logging, threading, serialization, precision/rounding, date/time, and transaction behavior are not verified by this unit.

## Summary Metrics

- Total Java artifacts discovered: 6 in this unit
- Total artifacts ported: 3 partial composition surfaces
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6
- Total blocked artifacts: 6 blocked/partial categories: Java runtime comparison, XML quest reward loading, bonus handlers, live inventory mutation, non-item reward mutation, and reward packet/persistence side effects
- Estimated overall migration completion: Phase 6 remains about 71% complete; this unit composes detailed non-live reward item projection into quest-finish operation planning.

## Next Recommended Unit Of Work

Begin a non-item reward projection scaffold for Java `QuestService.giveReward` that describes kinah, XP, title, AP, DP, GP, cube expansion, and warehouse expansion as non-live operation metadata. Reuse existing partial AP/DP reward services only as documented dependencies unless a very narrow safe composition point exists. Keep live mutation disabled.

Start with this file, `docs/QuestFinishRewardWorkItem-Audit.md`, `docs/QuestFinishOrdering-Audit.md`, and `docs/PHASE-6-PROGRESS.md` Session 1031.

Suggested next slice:

1. Add source-reviewed `Rewards` non-item projection records for kinah/exp/title/AP/DP/GP/expand inventory/warehouse.
2. Add warnings or dependency descriptors for rate services and target NPC l10n lookup.
3. Compose descriptors before quest-state mutation only if pure and non-live.
4. Add focused tests for zero-skip and category/rate metadata without calling live services.
5. Do not enable live inventory, AP, DP, GP, XP, title, cube, or warehouse mutation.
