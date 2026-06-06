# Phase 6 Session 2797 Handoff

## Current Phase

Phase 6: Port Game Core

## Latest Completed UOW

`[Phase 6][UOW-2797] Wire live quest finish regular selectable item rewards`

Commit made in this session:

- `[Phase 6][UOW-2797] Wire live quest finish regular selectable item rewards`

## Current State

- `GameServerConnection.HandleDialogSelectAsync` handles the Java self-target/reportable quest auto-reward branch for supported XP, kinah, fixed item rewards, regular selectable item rewards, work-item removal, title rewards, AP rewards, DP rewards, GP rewards, cube expansion rewards, and warehouse expansion rewards.
- `QuestFinishSocketInputAssemblyPlanService` now normalizes selected auto-reward action ids `110..124` to selected reward action ids `8..22` before reward projection lookup.
- Live regular selectable item rewards are now admitted by the live item reward descriptor allow-list and granted through the existing inventory add, persistence, and packet path.
- Challenge task completion was inspected but not selected for this UOW because the current C# `ChallengeTaskService` covers loading/listing/legion level gating, not Java-equivalent live `onChallengeQuestFinish` mutation and packet/persistence side effects.

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT` self-target `SELECTED_QUEST_AUTO_REWARD*` branch.
- `com.aionemu.gameserver.model.DialogAction`.
- `com.aionemu.gameserver.services.QuestService.finishQuest`.
- `com.aionemu.gameserver.services.QuestService.getRewardItems`.
- `com.aionemu.gameserver.services.item.ItemService.addItem`.

## C# Artifacts Touched

- `dotnetConversion/src/Aion.GameServer/Services/QuestFinishSocketInputAssemblyPlanService.cs`.
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`.
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionQuestFinishDialogBoundaryTests.cs`.
- `docs/Phase-6-Session-2797-Completion.md`.
- `docs/Phase-6-Session-2797-Handoff.md`.

## Validation Decision

- Changed surface: socket-side quest finish reward input assembly plus live item reward allow-list.
- Specific behavior/contract: Java selected auto-reward option resolves to the selected regular selectable reward item and grants it before quest completion.
- Focused C# command: `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~QuestFinishRewardPlanServiceTests|FullyQualifiedName~QuestFinishSocketInputAssemblyPlanServiceTests|FullyQualifiedName~QuestFinishStaticRewardProjectionCompositionTests"`
- Focused Java/Maven command: not run; no narrow Java fixture exists for `CM_DIALOG_SELECT` plus `QuestService.finishQuest/getRewardItems` live selectable reward packet fanout.
- Broad-validation trigger: none.
- Broad .NET decision: skipped; the filtered command compiled the affected project and exercised the edited live socket boundary plus adjacent reward projection/assembly tests.
- Why this scope is sufficient: the test drives `HandleDialogSelectAsync` with `SELECTED_QUEST_AUTO_REWARD1`, observes a live inventory item add for the selected reward, and verifies quest completion.

## Tests Run

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~QuestFinishRewardPlanServiceTests|FullyQualifiedName~QuestFinishSocketInputAssemblyPlanServiceTests|FullyQualifiedName~QuestFinishStaticRewardProjectionCompositionTests"
```

Result: Passed, 44 total, 0 failed, 0 skipped.

Java/Maven: not run; no narrow Java fixture exists for `CM_DIALOG_SELECT` plus `QuestService.finishQuest/getRewardItems` socket-side selectable reward item fanout.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CM_DIALOG_SELECT` self-target auto-reward selected option branch | `QuestFinishSocketInputAssemblyPlanService.CreatePlan` | Socket input assembly | Partial | Regression Tested | Partial Parity | Auto-reward selected option ids now normalize to Java reward ids for projection lookup. |
| `QuestService.getRewardItems` regular selectable reward branch | `QuestFinishRewardPlanService.CreateRewardItemProjection` plus `GameServerConnection.TryCreateQuestFinishAutoRewards` | Runtime item reward selection | Partial | Unit Tested / Regression Tested | Partial Parity | Regular selectable descriptors now flow into live item grant execution. |
| `QuestService.finishQuest` item fanout | `GameServerConnection.TryApplyQuestFinishItemRewardAsync` | Runtime inventory mutation | Partial | Regression Tested | Partial Parity | Fixed and regular selectable item rewards now use live inventory add/persistence/packet path. |

## Known Gaps

- Live quest finish still does not support class-selectable rewards, extended selectable rewards, bonus rewards, challenge task completion, quest-completed callbacks, NPC faction completion, NPC-target dialog quest paths, or broad nearby quest refresh fanout.
- Direct reward persistence is not newly wired in quest finish for AP/DP/GP/cube/warehouse expansions. Existing later player-save behavior may persist mutated runtime state, but these UOWs did not add direct quest-finish persistence calls.
- Challenge task completion needs real C# runtime mutation/persistence work before it can be wired safely; current challenge support is not enough for Java `ChallengeTaskService.onChallengeQuestFinish` parity.
- No Java runtime/golden fixture exists for quest finish socket selectable reward behavior.

## Runtime Progress Gate For Next UOW

Recommended next UOW: `[Phase 6][UOW-2798] Wire live quest finish class-selectable item rewards`

- Deferred/live behavior advanced: execute Java class-selectable quest item rewards from the live self-target/reportable auto-reward path.
- Java source of truth: `QuestService.getRewardItems` class-selectable branch and `QuestTemplate` `use_class_reward` behavior, with selected reward index from `DialogAction.SELECTED_QUEST_REWARD1..15`.
- C# runtime artifact to wire/fix: `GameServerConnection.TryCreateQuestFinishAutoRewards` should admit `QuestFinishRewardItemSource.ClassSelectable` after existing projection resolves player class and selected index.
- Client-visible/state/persistence effect expected: completing a supported last-repeat or every-repeat class-selectable quest grants the selected class reward item, sends inventory add/cube packets, and then sends `SmQuestAction.Update`.
- Why this is not preview-only/test-only/documentation-only: it will mutate live inventory and send real reward packets for a currently blocked Java reward path.

## Focused Validation Recipe For Next UOW

- Specific behavior/contract to prove: Java class-selectable reward selection grants the item for the player's class and selected index before quest completion.
- Focused C# command: start with `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~QuestFinishRewardPlanServiceTests|FullyQualifiedName~QuestFinishStaticRewardProjectionCompositionTests"`.
- Focused Java/Maven command: not expected unless a narrow Java fixture is added.
- Broad-validation trigger: run broader tests only if implementation touches shared item grant, class projection extraction, or persistence code.
- Broad .NET decision: do not run unless focused evidence exposes wider risk or a broad trigger is introduced by the implementation.

## Safe Runtime Candidates

- Wire class-selectable quest finish rewards through the existing projection descriptors and live item grant path.
- Wire extended selectable rewards after confirming the auto-reward no-reward dialog id and `extendedRewardIndex` mapping against Java/client packet behavior.
- Wire challenge task completion only after adding or finding a real C# runtime service for Java `onChallengeQuestFinish` mutation/persistence/packet side effects.

## Summary Metrics

- Total Java artifacts touched/discovered in latest UOW: 5.
- Total artifacts ported or wired in latest UOW: 1 live socket branch extension for regular selectable item rewards plus selected-action normalization.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification/partial parity: 3.
- Total blocked artifacts: 1 challenge task completion runtime path.
- Estimated overall migration completion: unchanged conservatively; Phase 6 remains in progress.
