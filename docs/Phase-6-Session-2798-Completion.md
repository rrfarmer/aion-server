# Phase 6 Session 2798 Completion

## Unit Of Work

`[Phase 6][UOW-2798] Wire live quest finish class-selectable item rewards`

## Runtime Progress Gate

- Deferred/live behavior advanced: live self-target reportable auto-reward quest finish now grants Java class-selectable item rewards for supported selected reward actions.
- Java source of truth: `QuestService.getRewardItems` class-selectable branch, `QuestTemplate` `use_class_reward` behavior, `DialogAction.SELECTED_QUEST_REWARD1..15`, and `QuestService.finishQuest` item fanout through `ItemService.addItem`.
- C# runtime artifact wired: `GameServerConnection.TryCreateQuestFinishAutoRewards` now admits `QuestFinishRewardItemSource.ClassSelectable` descriptors after existing projection resolves the player class and selected reward index.
- Client-visible/state/persistence effect changed: completing a supported last-repeat class-selectable quest now adds the selected class reward item to live player inventory, sends the existing inventory add/cube update packet chain, and then sends `SmQuestAction.Update`.
- Why this is not preview-only/test-only/documentation-only: the socket handler now mutates live inventory state and sends real item reward packets for a previously refused class-selectable reward path.

## Java Parity Notes

- Java `QuestService.getRewardItems` uses class-selectable rewards when `(isLastRepeat && template.isSingleTimeClassReward()) || template.isClassRewardOnEveryRepeat()`.
- C# projection already encoded that branch as `QuestFinishRewardItemSource.ClassSelectable` with the resolved `PlayerClass` and `SelectableIndex`.
- The live handler now allows that descriptor source to flow through the existing `InventoryAddService.CreateAddItemPlan`, inventory persistence, `SmInventoryAddItem`, and `SmCubeUpdate` path.
- This UOW validates the `use_class_reward="2"` last-repeat branch. `use_class_reward="1"` every-repeat behavior uses the same descriptor source and remains covered at projection level, but not by the new live socket fixture.

## C# Artifacts Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionQuestFinishDialogBoundaryTests.cs`

## Validation Decision

- Changed surface: live item reward allow-list.
- Specific behavior/contract: Java class-selectable reward selection grants the item for the player's class and selected index before quest completion.
- Focused C# command: `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~QuestFinishRewardPlanServiceTests|FullyQualifiedName~QuestFinishStaticRewardProjectionCompositionTests"`
- Focused Java/Maven command: not run; no narrow Java fixture exists for `CM_DIALOG_SELECT` class-selectable auto-reward packet execution and inventory packet fanout.
- Broad-validation trigger: none.
- Broad .NET decision: skipped; the filtered command compiled the affected project and exercised the edited live socket boundary plus adjacent reward projection/composition tests.
- Why this scope is sufficient: the test drives `HandleDialogSelectAsync` on the last repeat of a `use_class_reward="2"` quest and observes a live class reward item add plus quest completion.

## Tests Run

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~QuestFinishRewardPlanServiceTests|FullyQualifiedName~QuestFinishStaticRewardProjectionCompositionTests"
```

Result: Passed, 39 total, 0 failed, 0 skipped.

Java/Maven: not run; no narrow Java fixture exists for `CM_DIALOG_SELECT` plus `QuestService.finishQuest/getRewardItems` socket-side class-selectable reward item fanout.

## Test Evidence

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleDialogSelectAsync_ReportableAutoRewardQuestAddsClassSelectedItemOnLastRepeatAndCompletesQuest` | Regression | Java source review: `QuestService.getRewardItems` class-selectable branch | Live socket selected auto-reward grants last-repeat class-selectable item, sends inventory packets, and completes quest | Filtered C# boundary test with live inventory mutation and packet ordering | Does not cover every-repeat class rewards |
| `CreateRewardItemProjection_UsesClassSelectableRewardsOnLastRepeat` | Unit | Java source review: `QuestService.getRewardItems` | Projection chooses class reward instead of regular selectable reward on last repeat | Existing focused C# unit test | Projection evidence only |
| `StaticClassSelectableRewardProjection_ComposesOnLastRepeatInsteadOfRegularSelectableReward` | Unit | Java source review: reward projection order | Static operation composition keeps class-selectable reward before quest state mutation | Existing focused C# composition test | Some descriptors remain non-live |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `QuestService.getRewardItems` class-selectable branch | `QuestFinishRewardPlanService.CreateRewardItemProjection` plus `GameServerConnection.TryCreateQuestFinishAutoRewards` | Runtime item reward selection | Partial | Unit Tested / Regression Tested | Partial Parity | Class-selectable descriptors now flow into live item grant execution. |
| `QuestService.finishQuest` item fanout | `GameServerConnection.TryApplyQuestFinishItemRewardAsync` | Runtime inventory mutation | Partial | Regression Tested | Partial Parity | Fixed, regular selectable, and class-selectable item rewards now use live inventory add/persistence/packet path. |

## Parity Status

Partial parity improved for the self-target/reportable auto-reward quest finish branch. XP, kinah, fixed item rewards, regular selectable item rewards, class-selectable item rewards, work-item removal, title rewards, AP rewards, DP rewards, GP rewards, cube expansion rewards, and warehouse expansion rewards now run live for the guarded branch. Extended selectable rewards, bonus rewards, challenge tasks, quest completion callbacks, NPC faction completion, NPC-target dialog paths, direct quest-finish reward persistence for some non-item state, and broad nearby quest fanout remain incomplete.
