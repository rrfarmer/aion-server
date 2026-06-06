# Phase 6 Session 2814 Handoff

## Current Phase

Phase 6: Port Game Core

## Latest Completed UOW

`[Phase 6][UOW-2814] Wire NPC-target selected reward completion`

Commit made in this session:

- `[Phase 6][UOW-2814] Wire NPC-target selected reward completion`

## Current State

- `GameServerConnection.HandleDialogSelectAsync` handles supported self-target and NPC-target reportable auto-reward quest finish paths.
- Live NPC-target selected reward actions `8..23` can now finish `REWARD` quests using existing reward projection, item/non-item reward application, quest completion mutation, persistence hook, `SmQuestAction.Update`, and a scoped close dialog.
- Live NPC-target reward-page actions send Java reward selection pages for `REWARD` quests and apply Java-equivalent reward-group correction in memory.
- Live NPC-target `SET_SUCCEED` sends Java close-dialog page `0` for reward-state pre-end report paths.
- `StaticData.QuestNpcStarts` loads Java/XML NPC-start registrations and gates live NPC-target quest start dialog handling.
- Live NPC-target `ASK_QUEST_ACCEPT`, `QUEST_ACCEPT`, `QUEST_ACCEPT_1`, `QUEST_ACCEPT_SIMPLE`, `QUEST_REFUSE_1`, `QUEST_REFUSE_2`, `QUEST_REFUSE_SIMPLE`, and `FINISH_DIALOG` execute the directly wired Java start-dialog packet/state slices.

## Java Artifacts Touched

- `com.aionemu.gameserver.questEngine.handlers.AbstractQuestHandler.sendQuestEndDialog`.
- `com.aionemu.gameserver.model.DialogAction`.
- `com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT`.

## C# Artifacts Touched

- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmDialogSelect.cs`.
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`.
- `dotnetConversion/src/Aion.GameServer/Services/QuestDialogAutoRewardGuardPlanService.cs`.
- `dotnetConversion/src/Aion.GameServer/Services/QuestFinishSocketGuardedInputAssemblyPlanService.cs`.
- `dotnetConversion/src/Aion.GameServer/Services/QuestFinishSocketInputAssemblyPlanService.cs`.
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionQuestFinishDialogBoundaryTests.cs`.
- `docs/Phase-6-Session-2814-Completion.md`.
- `docs/Phase-6-Session-2814-Handoff.md`.

## Validation Decision

- Changed surface: live connection dispatch, reward side effects, quest state mutation, persistence hook, and server packet send.
- Specific behavior/contract: NPC-target selected reward action `8` for a `REWARD` quest applies the chosen selectable item, completes the quest, emits `SmQuestAction.Update`, and sends close dialog page `0`.
- Focused C# commands:
  - `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests"`
  - `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~QuestFinishSocketInputAssemblyPlanServiceTests|FullyQualifiedName~QuestFinishSocketGuardedInputAssemblyPlanServiceTests|FullyQualifiedName~QuestDialogAutoRewardGuardPlanServiceTests"`
  - `git diff --check`
- Focused Java/Maven command: not run; no narrow Java fixture exists for this socket-side branch. Java source was reviewed directly.
- Broad-validation trigger: live connection dispatch, reward side effects, quest state mutation, persistence hook, and server packet send.
- Broad .NET decision: skipped after focused validation; no shared packet serializer, database schema, packet primitive, scheduler, or common world-state model was changed.

## Tests Run

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests"
```

Result: Passed, 32 total, 0 failed, 0 skipped.

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~QuestFinishSocketInputAssemblyPlanServiceTests|FullyQualifiedName~QuestFinishSocketGuardedInputAssemblyPlanServiceTests|FullyQualifiedName~QuestDialogAutoRewardGuardPlanServiceTests"
```

Result: Passed, 31 total, 0 failed, 0 skipped.

Java/Maven: not run; no narrow Java fixture exists for this socket-side branch.

```powershell
git diff --check
```

Result: Passed; only normal workspace CRLF conversion warnings were reported.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `AbstractQuestHandler.sendQuestEndDialog` selected reward branch | `GameServerConnection.TryHandleNpcTargetQuestFinishSelectedRewardAsync` | Quest dialog handler slice | Partial | Regression Tested | Partial Parity | Wires selected reward completion and scoped close for NPC-target reward quests; full Java post-finish active/new quest selection scan is still missing. |
| `DialogAction` selected reward constants | `CmDialogSelect` | Dialog action constants | Partial | Regression Tested | Partial Parity | Adds live-consumed selected reward constants `8..23`; this is not a complete DialogAction port. |
| `CM_DIALOG_SELECT` NPC quest finish route | `QuestFinishSocketGuardedInputAssemblyPlanService` and `QuestFinishSocketInputAssemblyPlanService` | Socket input assembly | Partial | Regression Tested | Partial Parity | Existing auto-reward callers still reject selected reward actions by default; NPC-target selected completion explicitly enables them. |

## Known Gaps

- Full Java NPC controller, AI `onDialogSelect`, dynamic `QuestEngine.onDialog`, and arbitrary quest handler bodies remain incomplete.
- Full `QuestNpc.onTalkEvent` registration is not ported; selected completion cannot yet open another reward page after finishing one quest on the same NPC.
- Post-finish Java follow-up selection/start dialog logic is incomplete; selected completion currently sends a scoped close after reward application and quest update.
- Reward-group correction from the reward-selection page path is in-memory only; normal quest persistence remains tied to existing save/update paths.
- Live quest finish still does not support bonus rewards, challenge task completion, arbitrary quest completion callback handler bodies, mentor NPC faction title/flag side effects, or broad nearby quest refresh fanout.

## Runtime Progress Gate For Next UOW

Recommended next UOW: `[Phase 6][UOW-2815] Wire selected reward post-finish quest selection`

- Deferred/live behavior to advance: after NPC-target selected reward completion, send Java-equivalent follow-up selection instead of always closing when the same NPC still has another reward/start quest candidate.
- Java source of truth: `AbstractQuestHandler.sendQuestEndDialog` lines after `QuestService.finishQuest(env)` scan `QuestNpc.getOnTalkEvent()` and `QuestNpc.getOnQuestStart()` before choosing reward page, follow-up start page, selection page, or close.
- C# runtime artifact to wire/fix: extend `GameServerConnection` selected reward completion follow-up handling, using currently available quest/NPC runtime tables where safe and documenting any missing `onTalkEvent` blocker.
- Client-visible/state/persistence effect expected: live selected reward completion should send a follow-up `SmDialogWindow` selection/reward page when a safe same-NPC candidate exists, instead of only close page `0`.
- Why this is not preview-only/test-only/documentation-only: it would change a live client packet emitted after quest completion from the selected reward branch.

## Focused Validation Recipe For Next UOW

- Specific behavior/contract to prove: after selected reward completion, a safe same-NPC follow-up candidate sends the Java page instead of close, while the no-follow-up case still closes.
- Focused C# command: start with `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests"`.
- Add adjacent static-data tests only if the UOW ports or wires `QuestNpc.onTalkEvent` runtime data.
- Focused Java/Maven command: run only if a narrow Java fixture exists or is added for `sendQuestEndDialog` post-finish follow-up selection; otherwise document Java source review.
- Broad-validation trigger: live connection dispatch and server packet send.
- Broad .NET decision: start focused; do not run broad validation unless focused evidence exposes wider risk or shared static-data/runtime quest tables are edited.

## Safe Runtime Candidates

- Wire selected reward post-finish close-vs-selection behavior for the subset that can be proven from existing C# quest/NPC runtime data.
- Port/load a minimal `QuestNpc.onTalkEvent` runtime table only if it is immediately consumed by live selected reward post-finish routing in the same UOW.
- Wire selected reward no-reward and extended reward test coverage only if paired with live behavior that is not already covered by UOW-2814.
- Wire challenge task accept/completion only after adding or finding real C# runtime services for Java challenge task mutation/persistence/packet side effects.

## Summary Metrics

- Total Java artifacts touched/discovered in latest UOW: 3.
- Total artifacts ported or wired in latest UOW: 1 live NPC-target selected reward completion path plus selected reward constants and gated input assembly support.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification/partial parity: 3.
- Total blocked artifacts: 4 selected reward post-finish NPC quest scan, challenge task accept/completion, NPC faction start side effects, and mentor NPC faction side effects.
- Estimated overall migration completion: unchanged conservatively; Phase 6 remains in progress.
