# Phase 6 Session 2797 Completion

## Unit Of Work

`[Phase 6][UOW-2797] Wire live quest finish regular selectable item rewards`

## Runtime Progress Gate

- Deferred/live behavior advanced: live self-target reportable auto-reward quest finish now grants Java regular selectable item rewards selected through `SELECTED_QUEST_AUTO_REWARD1..15`.
- Java source of truth: `CM_DIALOG_SELECT` self-target `SELECTED_QUEST_AUTO_REWARD*` branch calls `QuestService.finishQuest`, and `QuestService.getRewardItems` resolves selectable reward indices from `DialogAction.SELECTED_QUEST_REWARD1..15` before `QuestService.finishQuest` calls `ItemService.addItem`.
- C# runtime artifact wired: `QuestFinishSocketInputAssemblyPlanService` normalizes auto-reward dialog actions `110..124` back to Java reward-index actions `8..22`, and `GameServerConnection.TryCreateQuestFinishAutoRewards` now admits `QuestFinishRewardItemSource.RegularSelectable` descriptors into the existing live item reward grant path.
- Client-visible/state/persistence effect changed: selecting a supported regular selectable quest reward now adds the selected item to live player inventory, sends the existing inventory add/cube update packet chain, and then sends `SmQuestAction.Update`.
- Why this is not preview-only/test-only/documentation-only: the socket handler now mutates live inventory state and sends real item reward packets for a previously refused selectable reward path.

## Java Parity Notes

- Java `DialogAction` defines `SELECTED_QUEST_REWARD1..15` as `8..22` and `SELECTED_QUEST_AUTO_REWARD1..15` as `110..124`; the self-target auto-reward branch calls `QuestService.finishQuest` directly.
- Java reward selection ultimately indexes the selectable reward list using the selected reward action family. C# projection code already modeled that index math, but the socket assembly passed raw auto-reward ids, so selectable descriptors were not produced for live auto-reward packets.
- C# now normalizes the live packet's selected auto-reward action before projection lookup, then allows regular selectable item descriptors to flow through `InventoryAddService.CreateAddItemPlan` and the existing inventory reward persistence/packet path.
- Extended selectable and class-selectable item rewards remain deferred to separate runtime UOWs.

## C# Artifacts Changed

- `dotnetConversion/src/Aion.GameServer/Services/QuestFinishSocketInputAssemblyPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionQuestFinishDialogBoundaryTests.cs`

## Validation Decision

- Changed surface: socket-side quest finish reward input assembly plus live item reward allow-list.
- Specific behavior/contract: Java selected auto-reward option resolves to the selected regular selectable reward item and grants it before quest completion.
- Focused C# command: `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~QuestFinishRewardPlanServiceTests|FullyQualifiedName~QuestFinishSocketInputAssemblyPlanServiceTests|FullyQualifiedName~QuestFinishStaticRewardProjectionCompositionTests"`
- Focused Java/Maven command: not run; no narrow Java fixture exists for `CM_DIALOG_SELECT` selectable auto-reward packet execution and inventory packet fanout.
- Broad-validation trigger: none.
- Broad .NET decision: skipped; the filtered command compiled the affected project and exercised the edited live socket boundary plus adjacent reward projection/assembly tests.
- Why this scope is sufficient: the test drives `HandleDialogSelectAsync` with `SELECTED_QUEST_AUTO_REWARD1`, observes a live inventory item add for the selected reward, and verifies quest completion.

## Tests Run

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~QuestFinishRewardPlanServiceTests|FullyQualifiedName~QuestFinishSocketInputAssemblyPlanServiceTests|FullyQualifiedName~QuestFinishStaticRewardProjectionCompositionTests"
```

Result: Passed, 44 total, 0 failed, 0 skipped.

Java/Maven: not run; no narrow Java fixture exists for `CM_DIALOG_SELECT` plus `QuestService.finishQuest/getRewardItems` socket-side selectable reward item fanout.

## Test Evidence

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleDialogSelectAsync_ReportableAutoRewardQuestAddsSelectedItemAndCompletesQuest` | Regression | Java source review: `CM_DIALOG_SELECT`, `DialogAction`, `QuestService.getRewardItems`, `QuestService.finishQuest` | Live socket selected auto-reward grants selected regular selectable item, sends inventory packets, and completes quest | Filtered C# boundary test with live inventory mutation and packet ordering | Does not cover extended/class selectable rewards |
| `CreateRewardItemProjection_*` selectable tests | Unit | Java source review: `QuestService.getRewardItems` | Projection chooses regular selectable items from selected reward index | Existing focused C# unit tests | Projection evidence only |
| `StaticClassSelectableRewardProjection_*` / static composition tests | Unit | Java source review: reward projection order | Adjacent projection composition remains stable after socket normalization | Existing focused C# tests | Some paths remain non-live |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CM_DIALOG_SELECT` self-target auto-reward selected option branch | `QuestFinishSocketInputAssemblyPlanService.CreatePlan` | Socket input assembly | Partial | Regression Tested | Partial Parity | Auto-reward selected option ids now normalize to Java reward ids for projection lookup. |
| `QuestService.getRewardItems` regular selectable reward branch | `QuestFinishRewardPlanService.CreateRewardItemProjection` plus `GameServerConnection.TryCreateQuestFinishAutoRewards` | Runtime item reward selection | Partial | Unit Tested / Regression Tested | Partial Parity | Regular selectable descriptors now flow into live item grant execution. |
| `QuestService.finishQuest` item fanout | `GameServerConnection.TryApplyQuestFinishItemRewardAsync` | Runtime inventory mutation | Partial | Regression Tested | Partial Parity | Fixed and regular selectable item rewards now use live inventory add/persistence/packet path. |

## Parity Status

Partial parity improved for the self-target/reportable auto-reward quest finish branch. XP, kinah, fixed item rewards, regular selectable item rewards, work-item removal, title rewards, AP rewards, DP rewards, GP rewards, cube expansion rewards, and warehouse expansion rewards now run live for the guarded branch. Class-selectable rewards, extended selectable rewards, challenge tasks, quest completion callbacks, NPC faction completion, NPC-target dialog paths, direct quest-finish reward persistence for some non-item state, and broad nearby quest fanout remain incomplete.
