# Phase 6 Session 2813 Handoff

## Current Phase

Phase 6: Port Game Core

## Latest Completed UOW

`[Phase 6][UOW-2813] Wire NPC-target SET_SUCCEED close dialog page`

Commit made in this session:

- `[Phase 6][UOW-2813] Wire NPC-target SET_SUCCEED close dialog page`

## Current State

- `GameServerConnection.HandleDialogSelectAsync` handles supported self-target and NPC-target reportable auto-reward quest finish paths.
- Live NPC-target reward-page actions send Java reward selection pages for `REWARD` quests and apply Java-equivalent reward-group correction in memory.
- Live NPC-target `SET_SUCCEED` now sends Java close-dialog page `0` for reward-state pre-end report paths.
- The same live NPC-target handler sends `SmDialogWindow(targetObjectId, dialogActionId, questId)` for unhandled quest/page fallback actions after existing special cases do not consume the packet.
- `StaticData.QuestNpcStarts` loads Java/XML NPC-start registrations and gates live NPC-target quest start dialog handling.
- Live NPC-target `ASK_QUEST_ACCEPT`, `QUEST_ACCEPT`, `QUEST_ACCEPT_1`, `QUEST_ACCEPT_SIMPLE`, `QUEST_REFUSE_1`, `QUEST_REFUSE_2`, `QUEST_REFUSE_SIMPLE`, and `FINISH_DIALOG` execute the directly wired Java start-dialog packet/state slices.

## Java Artifacts Touched

- `com.aionemu.gameserver.questEngine.handlers.AbstractQuestHandler.sendQuestEndDialog`.
- `com.aionemu.gameserver.model.DialogAction`.

## C# Artifacts Touched

- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmDialogSelect.cs`.
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`.
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionQuestFinishDialogBoundaryTests.cs`.
- `docs/Phase-6-Session-2813-Completion.md`.
- `docs/Phase-6-Session-2813-Handoff.md`.

## Validation Decision

- Changed surface: live connection dispatch and server packet send.
- Specific behavior/contract: NPC-target `SET_SUCCEED` for a `REWARD` quest sends Java-equivalent close dialog page `0` and does not finish or mutate the quest.
- Focused C# command: `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests"`
- Focused Java/Maven command: not run; no narrow Java fixture exists for this socket-side branch.
- Broad-validation trigger: live connection dispatch and server packet send.
- Broad .NET decision: skipped after focused validation; no shared packet serializer, quest finish payout logic, or persistence code was edited.

## Tests Run

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests"
```

Result: Passed, 31 total, 0 failed, 0 skipped.

Java/Maven: not run; no narrow Java fixture exists for this socket-side branch.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `AbstractQuestHandler.sendQuestEndDialog` `SET_SUCCEED` branch | `GameServerConnection.TryHandleNpcTargetQuestSetSucceedCloseDialogAsync` | Quest dialog handler slice | Partial | Regression Tested | Partial Parity | Wires close-dialog branch for reward quests; selected reward completion remains incomplete for non-auto-reward actions. |
| `DialogAction` | `CmDialogSelect` | Dialog action constants | Partial | Regression Tested | Partial Parity | Adds live-consumed `SET_SUCCEED`; this is not a complete DialogAction port. |

## Known Gaps

- Full Java NPC controller, AI `onDialogSelect`, dynamic `QuestEngine.onDialog`, and arbitrary quest handler bodies remain incomplete.
- Full `QuestNpc.onTalkEvent` registration is not ported; end-dialog gating is currently based on known NPC, interaction allowance, reportable quest projection, and reward quest state.
- Selected reward completion for Java `SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD` remains incomplete outside the already wired auto-reward reportable path.
- Reward-group correction from UOW-2812 is in-memory only; normal quest persistence remains tied to existing save/update paths.
- Broader `AbstractQuestHandler.onDialogEvent` page-only/refuse branches are not fully wired.
- Live quest finish still does not support bonus rewards, challenge task completion, arbitrary quest completion callback handler bodies, mentor NPC faction title/flag side effects, or broad nearby quest refresh fanout.

## Runtime Progress Gate For Next UOW

Recommended next UOW: `[Phase 6][UOW-2814] Wire NPC-target selected reward completion`

- Deferred/live behavior to advance: execute Java selected reward completion for NPC-target quests already in `QuestStatus.REWARD`.
- Java source of truth: `AbstractQuestHandler.sendQuestEndDialog` handles `SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD` by calling `QuestService.finishQuest(env)` and then choosing the next reward/start/selection/close dialog.
- C# runtime artifact to wire/fix: extend `GameServerConnection.HandleDialogSelectAsync` beyond auto-reward actions to selected reward actions, using existing reward projection, item/non-item reward application, quest completion mutation, persistence, and follow-up packet helpers where safe.
- Client-visible/state/persistence effect expected: live selected reward actions pay selected rewards, complete/persist the quest, send `SmQuestAction.Update`, and send the Java-equivalent follow-up dialog packet when it can be scoped safely.
- Why this is not preview-only/test-only/documentation-only: it would wire a deferred client packet path, mutate/persist live quest and inventory/player reward state, and send real server packets from live code.

## Focused Validation Recipe For Next UOW

- Specific behavior/contract to prove: one selected reward action for a `REWARD` quest applies the chosen reward, completes the quest, and emits the expected live packets without regressing auto-reward finish.
- Focused C# command: start with `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~QuestFinishRewardPlanServiceTests|FullyQualifiedName~QuestFinishStaticRewardProjectionCompositionTests"`.
- Narrower fallback if slow: `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests"`.
- Focused Java/Maven command: run only if a narrow Java fixture exists or is added for `sendQuestEndDialog` selected reward completion; otherwise document Java source review.
- Broad-validation trigger: live connection dispatch, reward side effects, quest state mutation, persistence, and server packet send.
- Broad .NET decision: start focused; do not run broad validation unless focused evidence exposes wider risk or shared reward/persistence code is edited beyond the selected branch.

## Safe Runtime Candidates

- Wire one selected reward completion action for NPC-target reward quests, preferably a fixed/selectable item case already represented in the boundary fixture.
- Wire selected reward no-reward completion only if reward projection and follow-up close/selection behavior can be scoped without guessing.
- Wire one broader `AbstractQuestHandler.onDialogEvent` page-only branch only if it can be scoped to a live registered NPC quest and does not conflict with the already wired start/end helper paths.
- Wire challenge task accept/completion only after adding or finding real C# runtime services for Java challenge task mutation/persistence/packet side effects.

## Summary Metrics

- Total Java artifacts touched/discovered in latest UOW: 2.
- Total artifacts ported or wired in latest UOW: 1 live NPC-target `SET_SUCCEED` packet path.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification/partial parity: 2.
- Total blocked artifacts: 3 challenge task accept/completion, NPC faction start side effects, and mentor NPC faction side effects.
- Estimated overall migration completion: unchanged conservatively; Phase 6 remains in progress.
