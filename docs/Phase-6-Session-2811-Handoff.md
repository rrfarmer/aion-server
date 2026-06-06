# Phase 6 Session 2811 Handoff

## Current Phase

Phase 6: Port Game Core

## Latest Completed UOW

`[Phase 6][UOW-2811] Wire NPC-target FINISH_DIALOG quest selection page`

Commit made in this session:

- `[Phase 6][UOW-2811] Wire NPC-target FINISH_DIALOG quest selection page`

## Current State

- `GameServerConnection.HandleDialogSelectAsync` handles supported self-target and NPC-target reportable auto-reward quest finish paths.
- The same live NPC-target handler sends `SmDialogWindow(targetObjectId, dialogActionId, questId)` for unhandled quest/page fallback actions after existing special cases do not consume the packet.
- `StaticData.QuestNpcStarts` loads Java/XML NPC-start registrations and gates live NPC-target quest start dialog handling.
- Live NPC-target `ASK_QUEST_ACCEPT`, `QUEST_ACCEPT`, `QUEST_ACCEPT_1`, `QUEST_ACCEPT_SIMPLE`, `QUEST_REFUSE_1`, `QUEST_REFUSE_2`, `QUEST_REFUSE_SIMPLE`, and `FINISH_DIALOG` now execute the directly wired Java start-dialog packet/state slices.

## Java Artifacts Touched

- `com.aionemu.gameserver.questEngine.handlers.AbstractQuestHandler.sendQuestStartDialog`.
- `com.aionemu.gameserver.questEngine.handlers.AbstractQuestHandler.sendQuestSelectionDialog`.
- `com.aionemu.gameserver.model.DialogAction`.

## C# Artifacts Touched

- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmDialogSelect.cs`.
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`.
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionQuestFinishDialogBoundaryTests.cs`.
- `docs/Phase-6-Session-2811-Completion.md`.
- `docs/Phase-6-Session-2811-Handoff.md`.

## Validation Decision

- Changed surface: live connection dispatch and server packet send.
- Specific behavior/contract: registered NPC-target `FINISH_DIALOG` sends Java-equivalent page `10` with quest id `0` and does not mutate quest state.
- Focused C# command: `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~QuestNpcStart"`
- Focused Java/Maven command: not run; no narrow Java fixture exists for socket-side NPC-target `FINISH_DIALOG`.
- Broad-validation trigger: live connection dispatch and server packet send.
- Broad .NET decision: skipped after focused validation; no shared packet serializer was edited, and the boundary test serializes the emitted packet fields.

## Tests Run

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~QuestNpcStart"
```

Result: Passed, 48 total, 0 failed, 0 skipped.

Java/Maven: not run; no narrow Java fixture exists for this socket-side branch.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `AbstractQuestHandler.sendQuestStartDialog` `FINISH_DIALOG` | `GameServerConnection.TryHandleNpcTargetQuestStartFinishDialogAsync` | Quest dialog handler slice | Partial | Regression Tested | Partial Parity | Sends Java-equivalent quest-selection page for registered NPC-start quests; broader handler dispatch remains incomplete. |
| `DialogAction` | `CmDialogSelect` | Dialog action constants | Partial | Regression Tested | Partial Parity | Adds live-consumed `FINISH_DIALOG`; this is not a complete DialogAction port. |

## Known Gaps

- Full Java NPC controller, AI `onDialogSelect`, dynamic `QuestEngine.onDialog`, and arbitrary quest handler bodies remain incomplete.
- Direct `sendQuestStartDialog` packet/state branches are now wired for the nearby actions, but full quest dialog parity remains incomplete.
- Reward-selection dialog pages for `QuestStatus.REWARD` are still incomplete outside the already wired auto-reward finish path.
- Broader `AbstractQuestHandler.onDialogEvent` page-only/refuse branches are not fully wired.
- Live quest finish still does not support bonus rewards, challenge task completion, arbitrary quest completion callback handler bodies, mentor NPC faction title/flag side effects, or broad nearby quest refresh fanout.
- Challenge task accept side effects and NPC faction start side effects remain blocked on clearer live C# services/state.

## Runtime Progress Gate For Next UOW

Recommended next UOW: `[Phase 6][UOW-2812] Wire NPC-target reward-selection dialog page`

- Deferred/live behavior to advance: execute the Java reward-selection branch for NPC-target quests already in `QuestStatus.REWARD`.
- Java source of truth: `AbstractQuestHandler.sendQuestEndDialog` handles `USE_OBJECT`, `QUEST_SELECT`, `SELECT_QUEST_REWARD`, `CHECK_USER_HAS_QUEST_ITEM`, and `CHECK_USER_HAS_QUEST_ITEM_SIMPLE` by validating/fixing the reward group and sending `sendQuestDialog(env, DialogPage.getRewardPageByIndex(qs.getRewardGroup()).id())`.
- C# runtime artifact to wire/fix: add live NPC-target reward-page handling in `GameServerConnection.HandleDialogSelectAsync`, using `PlayerQuestState.Status == "REWARD"`, existing `RewardGroup`, and Java-equivalent reward-page id mapping.
- Client-visible/state/persistence effect expected: live NPC-target reward-page actions send `SmDialogWindow(npcObjectId, rewardPageId, questId)` for reward quests instead of falling through to generic dialog page echo behavior.
- Why this is not preview-only/test-only/documentation-only: it would wire a deferred client packet path and send a real server packet from live code.

## Focused Validation Recipe For Next UOW

- Specific behavior/contract to prove: NPC-target reward-page actions for a `REWARD` quest send Java-equivalent reward page ids from `DialogPage.getRewardPageByIndex` and do not finish or mutate the quest yet.
- Focused C# command: start with `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~QuestFinishRewardPlanServiceTests|FullyQualifiedName~QuestFinishStaticRewardProjectionCompositionTests"`.
- Narrower fallback if slow: `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests"`.
- Focused Java/Maven command: run only if a narrow Java fixture exists or is added for `DialogPage.getRewardPageByIndex` or `sendQuestEndDialog`; otherwise document Java source review.
- Broad-validation trigger: live connection dispatch and server packet send.
- Broad .NET decision: do not run unless focused evidence exposes wider risk or shared packet serialization/reward projection code is edited.

## Safe Runtime Candidates

- Wire NPC-target reward-selection dialog page for `QuestStatus.REWARD`, after confirming reward-page id mapping and current C# `RewardGroup` semantics.
- Wire one broader `AbstractQuestHandler.onDialogEvent` page-only branch only if it can be scoped to a live registered NPC quest and does not conflict with the already wired start-dialog helper path.
- Wire one concrete arbitrary quest completion callback only if its Java body can be ported directly and has clear live state or packet effects.
- Wire challenge task accept/completion only after adding or finding real C# runtime services for Java challenge task mutation/persistence/packet side effects.

## Summary Metrics

- Total Java artifacts touched/discovered in latest UOW: 3.
- Total artifacts ported or wired in latest UOW: 1 live NPC-target finish-dialog packet path.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification/partial parity: 2.
- Total blocked artifacts: 3 challenge task accept/completion, NPC faction start side effects, and mentor NPC faction side effects.
- Estimated overall migration completion: unchanged conservatively; Phase 6 remains in progress.
