# Phase 6 Session 2800 Completion

## Unit Of Work

`[Phase 6][UOW-2800] Wire live quest finish extended selectable item rewards`

## Runtime Progress Gate

- Deferred/live behavior advanced: live self-target reportable auto-reward quest finish can now grant extended selectable item rewards on the last repeat.
- Java source of truth: `CM_DIALOG_SELECT.runImpl` forwards reportable auto-reward actions to `QuestService.finishQuest`; `QuestService.getRewardItems` selects from `template.getExtendedRewards().getSelectableRewardItem()` when the effective dialog action is `SELECTED_QUEST_NOREWARD` and `QuestEnv.extendedRewardIndex` points at an extended reward.
- C# runtime artifact wired: `QuestFinishSocketInputAssemblyPlanService.NormalizeQuestRewardDialogAction` now maps plain auto-reward `108` to no-reward action `23` for reward projection, and `GameServerConnection.TryAddQuestFinishItemRewards` now allows `QuestFinishRewardItemSource.ExtendedSelectable` through the live grant path.
- Client-visible/state effect changed: completing a supported reportable quest with an extended selectable reward now mutates live inventory, sends `SmInventoryAddItem` and `SmCubeUpdate`, then completes the quest with `SmQuestAction`.
- Why this is not preview-only/test-only/documentation-only: the live socket handler now grants the selected extended item to the player inventory and sends real server packets.

## Java Parity Notes

- Java uses `extendedRewardIndex - 8` first, then `extendedRewardIndex - 1`, for extended selectable rewards. The existing C# reward projection already implemented that selection logic; this UOW wires it into live quest finish.
- Java accepts the self-target/reportable auto-reward socket actions `108` and `110..124`. C# already normalized `110..124` into regular selectable reward actions; this UOW normalizes `108` into Java's no-reward action so extended selectable projection can run.
- The new boundary test uses `extendedRewardIndex=8`, which exercises Java's first extended selectable index branch.

## C# Artifacts Changed

- `dotnetConversion/src/Aion.GameServer/Services/QuestFinishSocketInputAssemblyPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestFinishSocketInputAssemblyPlanServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionQuestFinishDialogBoundaryTests.cs`

## Validation Decision

- Changed surface: reportable quest finish socket input normalization and live item reward allow-list.
- Specific behavior/contract: Java extended selectable quest rewards are selected by `extendedRewardIndex` and granted during `QuestService.finishQuest` before quest completion.
- Focused C# command: `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~QuestFinishSocketInputAssemblyPlanServiceTests|FullyQualifiedName~QuestFinishRewardPlanServiceTests|FullyQualifiedName~QuestFinishStaticRewardProjectionCompositionTests"`
- Focused Java/Maven command: not run; no narrow Java fixture exists for `CM_DIALOG_SELECT` plus `QuestService.finishQuest` extended selectable live item grants.
- Broad-validation trigger: none.
- Broad .NET decision: skipped; the filtered command compiled the affected project and exercised the live socket boundary plus existing Java-parity reward projection cases.
- Why this scope is sufficient: the new boundary test drives `HandleDialogSelectAsync` with a real packet, verifies inventory mutation and packets, and verifies quest completion after the grant.

## Tests Run

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~QuestFinishSocketInputAssemblyPlanServiceTests|FullyQualifiedName~QuestFinishRewardPlanServiceTests|FullyQualifiedName~QuestFinishStaticRewardProjectionCompositionTests"
```

Result: Passed, 47 total, 0 failed, 0 skipped.

Java/Maven: not run; no narrow Java fixture exists for the socket-side extended selectable quest finish path.

## Test Evidence

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleDialogSelectAsync_ReportableAutoRewardQuestAddsExtendedSelectedItemOnLastRepeatAndCompletesQuest` | Regression | Java source review: `CM_DIALOG_SELECT.runImpl` and `QuestService.getRewardItems` extended branch | Live socket quest finish grants the selected extended item, sends inventory packets, and completes the quest | Filtered C# boundary test with live inventory mutation and packet assertions | Does not cover Java fallback index `extendedRewardIndex - 1` from live socket |
| `CreatePlan_PreparesRewardProjectionForReportableAutoRewardWithoutExecutingFinish` | Unit | Java source review: auto-reward no-selection path uses no-reward reward projection semantics | Plain auto-reward `108` normalizes to no-reward `23` for projection | Focused C# unit assertion | Projection-level only |
| `CreateRewardItemProjection_UsesExtendedIndexMinusEightBeforeMinusOneLikeJava` | Unit | Java source review: `QuestService.getRewardItems` extended selectable index order | Existing projection logic selects the Java-equivalent extended selectable item | Focused C# unit test | Not live handler evidence |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CM_DIALOG_SELECT.runImpl` reportable auto-reward `108` | `QuestFinishSocketInputAssemblyPlanService.NormalizeQuestRewardDialogAction` | Runtime socket input normalization | Partial | Unit Tested / Regression Tested | Partial Parity | Plain auto reward now projects as Java no-reward action for reward selection. |
| `QuestService.getRewardItems` extended selectable branch | `QuestFinishRewardPlanService.CreateRewardItemProjection` plus live allow-list | Runtime item reward selection | Partial | Unit Tested / Regression Tested | Partial Parity | Existing selection logic is now reachable from live quest finish. |
| `ItemService.addItem` from quest finish | `GameServerConnection.TryApplyQuestFinishItemRewardAsync` | Runtime inventory mutation and packets | Partial | Regression Tested | Partial Parity | Extended selectable descriptors now grant items and emit inventory packets. |

## Parity Status

Partial parity improved for the self-target/reportable auto-reward quest finish branch. XP, kinah, fixed item rewards, regular selectable item rewards, class-selectable item rewards, extended selectable item rewards, work-item removal, title rewards, AP rewards, DP rewards, GP rewards, cube expansion rewards, warehouse expansion rewards, and non-mentor NPC faction completion now run live for the guarded branch. Bonus rewards, challenge tasks, quest completion callbacks, mentor NPC faction title/flag side effects, NPC-target dialog quest paths, direct quest-finish reward persistence for some non-item state, and broad nearby quest fanout remain incomplete.
