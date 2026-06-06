# Phase 6 Session 2812 Completion

## Unit Of Work

`[Phase 6][UOW-2812] Wire NPC-target reward-selection dialog page`

## Runtime Progress Gate

- Deferred/live behavior advanced: live NPC-target reward-page actions for quests already in `REWARD` state now send Java-equivalent reward selection pages.
- Java source of truth: `AbstractQuestHandler.sendQuestEndDialog` handles `USE_OBJECT`, `QUEST_SELECT`, `SELECT_QUEST_REWARD`, `CHECK_USER_HAS_QUEST_ITEM`, and `CHECK_USER_HAS_QUEST_ITEM_SIMPLE` by calling `QuestService.validateAndFixRewardGroup(qs, questId)` and then `sendQuestDialog(env, DialogPage.getRewardPageByIndex(qs.getRewardGroup()).id())`.
- C# runtime artifact wired: `CmDialogSelect` reward-page action constants and `GameServerConnection.TryHandleNpcTargetQuestRewardSelectionPageAsync`.
- Client-visible/state effect changed: live NPC-target reward-page actions correct the in-memory `PlayerQuestState.RewardGroup` when needed and send `SmDialogWindow(npcObjectId, rewardPageId, questId)`.
- Why this is not preview-only/test-only/documentation-only: it wires a deferred client packet path, sends a real server packet from live code, and mutates live quest state when Java reward-group correction applies.

## Java Parity Notes

- Java `DialogAction.USE_OBJECT` is `-1`, but `CM_DIALOG_SELECT` carries it in an unsigned short; the C# runtime observes `0xFFFF`.
- Reward page mapping follows Java `DialogPage.getRewardPageByIndex`: groups `0..3` map to pages `5..8`, groups `4..9` map to pages `45..50`, and null/out-of-range after correction maps to page `0`.
- This UOW intentionally does not finish the quest or pay rewards. It only wires the page-selection branch before selected reward completion.
- Full Java `QuestNpc.onTalkEvent` registration is not ported; this live slice is gated by known NPC, interaction allowance, reportable quest projection, and `PlayerQuestState.Status == "REWARD"`.

## C# Artifacts Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmDialogSelect.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionQuestFinishDialogBoundaryTests.cs`

## Validation Decision

- Changed surface: live connection dispatch, live quest state mutation, and server packet send.
- Specific behavior/contract: NPC-target reward-page actions for a `REWARD` quest send Java-equivalent reward page ids from `DialogPage.getRewardPageByIndex` and apply Java-equivalent reward-group correction without finishing the quest.
- Focused C# command: `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~QuestFinishRewardPlanServiceTests|FullyQualifiedName~QuestFinishStaticRewardProjectionCompositionTests"`
- Focused Java/Maven command: not run; no narrow Java fixture exists for this socket-side branch, and Java source review was sufficient for the deterministic page mapping.
- Broad-validation trigger: live connection dispatch, live quest state mutation, and server packet send.
- Broad .NET decision: skipped after focused validation; no shared packet serializer or reward payout logic was edited.
- Why this scope is sufficient: the boundary tests serialize the emitted packet and inspect live quest state, while adjacent reward-plan tests cover the existing Java-shaped reward-group correction helper used by the new live path.

## Tests Run

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests"
```

Result: Passed, 30 total, 0 failed, 0 skipped.

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~QuestFinishRewardPlanServiceTests|FullyQualifiedName~QuestFinishStaticRewardProjectionCompositionTests"
```

Result: Passed, 56 total, 0 failed, 0 skipped.

Java/Maven: not run; no narrow Java fixture exists for this socket-side branch.

## Test Evidence

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleDialogSelectAsync_NpcTargetRewardQuestSendsRewardSelectionPage` | Regression | Java source review: `AbstractQuestHandler.sendQuestEndDialog`, `DialogPage.getRewardPageByIndex` | `USE_OBJECT` for a reward quest sends page `5` with the quest id and leaves valid reward group state unchanged | Filtered C# socket boundary test with serialized packet assertions | Does not verify full Java `QuestNpc.onTalkEvent` registration |
| `HandleDialogSelectAsync_NpcTargetRewardQuestCorrectsRewardGroupBeforeSendingPage` | Regression | Java source review: `QuestService.validateAndFixRewardGroup` and `DialogPage.getRewardPageByIndex` | Out-of-range reward group is corrected before sending reward page `5` | Filtered C# socket boundary test checks packet and live quest state mutation | Does not persist correction immediately; Java also mutates in-memory quest state before normal quest persistence |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.questEngine.handlers.AbstractQuestHandler.sendQuestEndDialog` reward-page branch | `Aion.GameServer.Network.Aion.GameServerConnection.TryHandleNpcTargetQuestRewardSelectionPageAsync` | Quest dialog handler slice | Partial | Regression Tested | Partial Parity | Wires page-send branch for reward quests and applies reward-group correction; selected reward completion remains incomplete for non-auto-reward actions. |
| `com.aionemu.gameserver.model.DialogPage.getRewardPageByIndex` | `Aion.GameServer.Network.Aion.GameServerConnection.GetQuestRewardSelectionDialogPageId` | Dialog page mapping | Partial | Regression Tested | Partial Parity | Implements reward-page id mapping used by the live reward-page branch; this is not a complete DialogPage port. |
| `com.aionemu.gameserver.model.DialogAction` | `Aion.GameServer.Network.Aion.ClientPackets.CmDialogSelect` | Dialog action constants | Partial | Regression Tested | Partial Parity | Adds live-consumed `USE_OBJECT`, `QUEST_SELECT`, `CHECK_USER_HAS_QUEST_ITEM`, `SELECT_QUEST_REWARD`, and `CHECK_USER_HAS_QUEST_ITEM_SIMPLE`; `USE_OBJECT` is represented as unsigned `0xFFFF` in C# packet state. |

## Known Gaps

- Full Java NPC controller, AI `onDialogSelect`, dynamic `QuestEngine.onDialog`, and arbitrary quest handler bodies remain incomplete.
- Full `QuestNpc.onTalkEvent` registration is not ported; reward-page gating is currently based on known NPC, interaction allowance, reportable quest projection, and reward quest state.
- Selected reward completion for Java `SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD` remains incomplete outside the already wired auto-reward reportable path.
- Reward-group correction is in-memory only in this page UOW; normal quest persistence remains tied to existing save/update paths.
- Live quest finish still does not support bonus rewards, challenge task completion, arbitrary quest completion callback handler bodies, mentor NPC faction title/flag side effects, or broad nearby quest refresh fanout.
