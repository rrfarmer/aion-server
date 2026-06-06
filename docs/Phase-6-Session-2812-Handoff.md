# Phase 6 Session 2812 Handoff

## Current Phase

Phase 6: Port Game Core

## Latest Completed UOW

`[Phase 6][UOW-2812] Wire NPC-target reward-selection dialog page`

Commit made in this session:

- `[Phase 6][UOW-2812] Wire NPC-target reward-selection dialog page`

## Current State

- `GameServerConnection.HandleDialogSelectAsync` handles supported self-target and NPC-target reportable auto-reward quest finish paths.
- Live NPC-target reward-page actions now send Java reward selection pages for `REWARD` quests and apply Java-equivalent reward-group correction in memory.
- The same live NPC-target handler sends `SmDialogWindow(targetObjectId, dialogActionId, questId)` for unhandled quest/page fallback actions after existing special cases do not consume the packet.
- `StaticData.QuestNpcStarts` loads Java/XML NPC-start registrations and gates live NPC-target quest start dialog handling.
- Live NPC-target `ASK_QUEST_ACCEPT`, `QUEST_ACCEPT`, `QUEST_ACCEPT_1`, `QUEST_ACCEPT_SIMPLE`, `QUEST_REFUSE_1`, `QUEST_REFUSE_2`, `QUEST_REFUSE_SIMPLE`, and `FINISH_DIALOG` execute the directly wired Java start-dialog packet/state slices.

## Java Artifacts Touched

- `com.aionemu.gameserver.questEngine.handlers.AbstractQuestHandler.sendQuestEndDialog`.
- `com.aionemu.gameserver.model.DialogPage`.
- `com.aionemu.gameserver.model.DialogAction`.

## C# Artifacts Touched

- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmDialogSelect.cs`.
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`.
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionQuestFinishDialogBoundaryTests.cs`.
- `docs/Phase-6-Session-2812-Completion.md`.
- `docs/Phase-6-Session-2812-Handoff.md`.

## Validation Decision

- Changed surface: live connection dispatch, live quest state mutation, and server packet send.
- Specific behavior/contract: NPC-target reward-page actions for a `REWARD` quest send Java-equivalent reward page ids from `DialogPage.getRewardPageByIndex` and apply Java-equivalent reward-group correction without finishing the quest.
- Focused C# command: `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~QuestFinishRewardPlanServiceTests|FullyQualifiedName~QuestFinishStaticRewardProjectionCompositionTests"`
- Focused Java/Maven command: not run; no narrow Java fixture exists for this socket-side branch, and Java source review was sufficient for the deterministic page mapping.
- Broad-validation trigger: live connection dispatch, live quest state mutation, and server packet send.
- Broad .NET decision: skipped after focused validation; no shared packet serializer or reward payout logic was edited.

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

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `AbstractQuestHandler.sendQuestEndDialog` reward-page branch | `GameServerConnection.TryHandleNpcTargetQuestRewardSelectionPageAsync` | Quest dialog handler slice | Partial | Regression Tested | Partial Parity | Wires page-send branch for reward quests and applies reward-group correction; selected reward completion remains incomplete for non-auto-reward actions. |
| `DialogPage.getRewardPageByIndex` | `GameServerConnection.GetQuestRewardSelectionDialogPageId` | Dialog page mapping | Partial | Regression Tested | Partial Parity | Implements reward-page id mapping used by the live reward-page branch; this is not a complete DialogPage port. |
| `DialogAction` | `CmDialogSelect` | Dialog action constants | Partial | Regression Tested | Partial Parity | Adds live-consumed reward-page action constants; `USE_OBJECT` is represented as unsigned `0xFFFF` in C# packet state. |

## Known Gaps

- Full Java NPC controller, AI `onDialogSelect`, dynamic `QuestEngine.onDialog`, and arbitrary quest handler bodies remain incomplete.
- Full `QuestNpc.onTalkEvent` registration is not ported; reward-page gating is currently based on known NPC, interaction allowance, reportable quest projection, and reward quest state.
- Selected reward completion for Java `SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD` remains incomplete outside the already wired auto-reward reportable path.
- Reward-group correction is in-memory only in this page UOW; normal quest persistence remains tied to existing save/update paths.
- Broader `AbstractQuestHandler.onDialogEvent` page-only/refuse branches are not fully wired.
- Live quest finish still does not support bonus rewards, challenge task completion, arbitrary quest completion callback handler bodies, mentor NPC faction title/flag side effects, or broad nearby quest refresh fanout.

## Runtime Progress Gate For Next UOW

Recommended next UOW: `[Phase 6][UOW-2813] Wire NPC-target SET_SUCCEED close dialog page`

- Deferred/live behavior to advance: execute the Java pre-end report close branch for NPC-target quests already in `QuestStatus.REWARD`.
- Java source of truth: `AbstractQuestHandler.sendQuestEndDialog` handles `SET_SUCCEED` with `closeDialogWindow(env)`, and `DialogAction.SET_SUCCEED` is `10255`.
- C# runtime artifact to wire/fix: add `CmDialogSelect.SetSucceed` and live NPC-target handling in `GameServerConnection.HandleDialogSelectAsync`, gated by known NPC, interaction allowance, reportable quest projection, and `PlayerQuestState.Status == "REWARD"`.
- Client-visible/state/persistence effect expected: live NPC-target `SET_SUCCEED` sends `SmDialogWindow(npcObjectId, 0, 0)` without finishing or mutating the quest.
- Why this is not preview-only/test-only/documentation-only: it would wire a deferred client packet path and send a real server packet from live code.

## Focused Validation Recipe For Next UOW

- Specific behavior/contract to prove: NPC-target `SET_SUCCEED` for a `REWARD` quest sends Java-equivalent close dialog page `0` and does not finish or mutate the quest.
- Focused C# command: start with `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests"`.
- Focused Java/Maven command: run only if a narrow Java fixture exists or is added for `sendQuestEndDialog`; otherwise document Java source review.
- Broad-validation trigger: live connection dispatch and server packet send.
- Broad .NET decision: do not run unless focused evidence exposes wider risk or shared packet serialization/reward finish code is edited.

## Safe Runtime Candidates

- Wire NPC-target `SET_SUCCEED` close-dialog branch for reward quests.
- Wire selected reward completion for `SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD` only after scoping item/non-item reward side effects, persistence, and follow-up dialog behavior carefully.
- Wire one broader `AbstractQuestHandler.onDialogEvent` page-only branch only if it can be scoped to a live registered NPC quest and does not conflict with the already wired start/end helper paths.
- Wire challenge task accept/completion only after adding or finding real C# runtime services for Java challenge task mutation/persistence/packet side effects.

## Summary Metrics

- Total Java artifacts touched/discovered in latest UOW: 3.
- Total artifacts ported or wired in latest UOW: 1 live NPC-target reward-page packet/state path.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification/partial parity: 3.
- Total blocked artifacts: 3 challenge task accept/completion, NPC faction start side effects, and mentor NPC faction side effects.
- Estimated overall migration completion: unchanged conservatively; Phase 6 remains in progress.
