# Phase 6TV Completion - UOW-1030 Reward XML Projection Scaffold

Date: May 25, 2026

## Parallel Work Discovery

| Candidate | Scope | Java Source | C# Target | Type | Selected | Risk | Notes |
|---|---|---|---|---|---|---|---|
| A | Reward XML item projection scaffold | `QuestService.getRewardItems`, `Rewards`, `QuestItems`, `QuestTemplate.getSelectableRewardByClass` | `QuestFinishRewardPlanService` | Service / DTO Projection | Yes | Medium | Direct continuation from UOW-1029 handoff. Kept descriptor-only and non-live. |
| B | Persistence failure-ordering policy audit | `PlayerQuestListDAO`, `PlayerNpcFactionsDAO` | docs/planner policy | Java Analysis | No | Low | Still useful before DAO writes, but less direct than reward projection. |
| C | Callback packet serialization audit | `SM_QUEST_ACTION`, callback follow-up sends | packet docs/tests | Packet Analysis | No | Medium | Blocked by no live callback execution. |
| D | Live reward/inventory mutation | `ItemService.addItem`, `QuestService.giveReward` | inventory/AP/XP/title/cube services | Service Port | No | High | Requires real XML loading, item service integration, transaction policy, and packet side-effect coverage. |

## Completed

- Added non-live reward item projection records to `QuestFinishRewardPlanService`:
  - `QuestFinishRewardItem`
  - `QuestFinishRewardGroupProjection`
  - `QuestFinishRewardItemProjectionInput`
  - `QuestFinishRewardItemTemplateProjection`
  - `QuestFinishRewardItemProjectionDescriptor`
  - `QuestFinishRewardItemProjectionWarningDescriptor`
- Added `QuestFinishRewardPlanService.CreateRewardItemProjection`.
- Added Java-shaped reward index mapping for dialog actions `8` through `22`.
- Modeled extended reward selection before regular reward selection.
- Modeled extended selectable index lookup using Java's `extendedRewardIndex - 8` fallback before `extendedRewardIndex - 1`.
- Modeled regular fixed/selectable rewards and class-specific selectable rewards under the Java repeat rules.
- Kept bonus reward behavior as an explicit projection warning because Java delegates bonus item calculation to handlers and `BonusService`.
- Updated reward/order/nearby audit docs and `docs/PHASE-6-PROGRESS.md`.

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter QuestFinishRewardPlanServiceTests --nologo` | Passed: 13 |
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` | Passed: 1786 |

## Migration Parity Table - Session 1030

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.QuestService.getRewardItems` | `Aion.GameServer.Services.QuestFinishRewardPlanService.CreateRewardItemProjection` | Service / Reward Item Planner | Partial | Unit Tested | Partial Parity | Source-reviewed C# projection covers fixed regular rewards, selectable regular rewards, class-specific selectable rewards, extended fixed rewards, extended selectable reward index mapping, and no-reward class-selection indexing. No Java runtime comparison, real XML loading, bonus handler execution, logging, live inventory mutation, packet sends, or persistence exists. |
| `com.aionemu.gameserver.model.templates.quest.Rewards` | `QuestFinishRewardGroupProjection`; `QuestFinishRewardItemTemplateProjection.ExtendedRewards` | DTO / XML Projection | Partial | Unit Tested | Needs Verification | Projects `reward_item` and `selectable_reward_item` lists only. Kinah, XP, title, AP, DP, GP, cube/warehouse expansion, `extend_stigma`, `ccheck`, and `icheck` remain outside this item projection scaffold. |
| `com.aionemu.gameserver.model.templates.quest.QuestItems` | `Aion.GameServer.Services.QuestFinishRewardItem` | DTO / XML Projection | Partial | Unit Tested | Partial Parity | Carries `item_id` and Java default count `1`. XML unmarshalling, item template validation, stack behavior, and inventory add side effects are not ported here. |
| `com.aionemu.gameserver.model.templates.QuestTemplate.getSelectableRewardByClass` | `QuestFinishRewardItemTemplateProjection.ClassSelectableRewards` | Template / Class Reward Projection | Partial | Unit Tested | Needs Verification | Class-specific rewards are projected by string key rather than Java `PlayerClass` enum lists. Java switch mapping, enum serialization, and real template loading remain missing. |
| `com.aionemu.gameserver.model.DialogAction` | `QuestFinishRewardPlanService.GetRewardIndex` | Utility / Constant Mapping | Partial | Unit Tested | Partial Parity | Covers `SELECTED_QUEST_REWARD1` through `SELECTED_QUEST_REWARD15` as ids `8..22` and treats other actions as `-1`. The rest of Java `DialogAction` is not ported in this utility. |
| `com.aionemu.gameserver.questEngine.QuestEngine.onBonusApplyEvent` | `QuestFinishRewardItemProjectionWarning.BonusHandlerNotProjected` | Handler Dependency | Not Started | Unit Tested | Needs Verification | Newly explicit dependency. C# only records that bonus reward resolution is not projected; it does not execute handlers or call a bonus service. |
| `com.aionemu.gameserver.services.BonusService.getQuestBonus` | `QuestFinishRewardItemProjectionWarning.BonusHandlerNotProjected` | Service Dependency | Not Started | Unit Tested | Needs Verification | Newly explicit dependency. Bonus item calculation remains unsupported and non-live. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `CreateRewardItemProjection_AddsExtendedRewardsBeforeRegularRewardsLikeJava` | Unit | `QuestService.finishQuest`; `getRewardItems` | Extended fixed item descriptors precede regular fixed and regular selectable descriptors on the last repeat. | Source-reviewed Java ordering. | No live `ItemService.addItem` or Java runtime capture. |
| `CreateRewardItemProjection_UsesExtendedIndexMinusEightBeforeMinusOneLikeJava` | Unit | `QuestService.getRewardItems` extended branch | Extended selectable index resolves `index - 8` before `index - 1`. | Source-reviewed branch with deterministic input. | No Java runtime capture or logging validation. |
| `CreateRewardItemProjection_UsesClassSelectableRewardsOnLastRepeat` | Unit | `QuestService.getRewardItems`; `QuestTemplate.getSelectableRewardByClass` | Last-repeat single-time class rewards use class-specific selectable rewards instead of group selectable rewards. | Source-reviewed Java repeat/class condition. | Player class mapping is projected as a string key. |
| `CreateRewardItemProjection_ProjectsNoRewardClassSelectionFromExtendedIndex` | Unit | `QuestService.getRewardItems` no-reward class branch | `SELECTED_QUEST_NOREWARD` can select class reward index `extendedRewardIndex - 8`. | Source-reviewed Java no-reward branch. | No real dialog/env object. |
| `CreateRewardItemProjection_LeavesBonusAsExplicitProjectionWarning` | Unit | `QuestEngine.onBonusApplyEvent`; `BonusService.getQuestBonus` | Bonus templates produce a warning instead of pretending to project handler-driven item rewards. | Source-reviewed Java handler dependency. | Bonus handler runtime is not ported. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- The projection scaffold is not wired to real JAXB/XML quest data.
- Projection descriptors are not live; they do not mutate inventory, send packets, call handlers, or persist state.
- Bonus reward behavior remains unsupported because Java delegates it through quest handlers and `BonusService`.
- Non-item reward mutation remains outside this scaffold: kinah, XP, title, AP, DP, GP, cube expansion, and warehouse expansion still need separate planning/composition.
- Java warning/log behavior for malformed reward selections is represented as warning descriptors only.
- Player class mapping is string-projected and not verified against Java `PlayerClass` switch behavior.
- Serialization, threading, date/time, precision/rounding, and transaction behavior are not touched by this unit.

## Summary Metrics

- Total Java artifacts discovered: 7 in this unit
- Total artifacts ported: 5 partial projection artifacts or helper surfaces
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 7
- Total blocked artifacts: 6 blocked/partial categories: Java runtime comparison, XML quest reward loading, bonus handlers, live inventory mutation, non-item reward mutation, and reward packet/persistence side effects
- Estimated overall migration completion: Phase 6 remains about 71% complete; this unit adds non-live reward item projection scaffolding without enabling live quest rewards.

## Next Recommended Unit Of Work

Compose `QuestFinishRewardPlanService.CreateRewardItemProjection` into `QuestFinishRewardTemplateProjection` / `QuestFinishOperationPlanService` as detailed non-live item reward descriptors before the existing coarse item placeholder. Preserve Java ordering and keep live `ItemService.addItem`, bonus handlers, non-item rewards, packets, and DAO writes disabled.

Start with this file, `docs/QuestFinishRewardWorkItem-Audit.md`, `docs/QuestFinishOrdering-Audit.md`, and `docs/PHASE-6-PROGRESS.md` Session 1030.

Suggested next slice:

1. Decide whether `QuestFinishRewardTemplateProjection` should own the detailed item projection directly or reference a nested `QuestFinishRewardItemTemplateProjection`.
2. Compose selected item descriptors into the reward operation plan before non-item reward placeholders.
3. Add tests that corrected reward group feeds detailed item projection.
4. Keep bonus rewards as warning/placeholder metadata only.
5. Do not wire live inventory mutation yet.
