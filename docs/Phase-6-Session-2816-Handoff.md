# Phase 6 Session 2816 Handoff

## Current Phase

Phase 6: Port Game Core

## Latest Completed UOW

`[Phase 6][UOW-2816] Wire selected reward post-finish active quest selection`

Commit made in this session:

- `[Phase 6][UOW-2816] Wire selected reward post-finish active quest selection`

## Current State

- `GameServerConnection.HandleDialogSelectAsync` handles supported self-target and NPC-target reportable auto-reward quest finish paths.
- Live NPC-target selected reward actions `8..23` can finish `REWARD` quests using existing reward projection, item/non-item reward application, quest completion mutation, persistence hook, and `SmQuestAction.Update`.
- Java handler `addOnTalkEvent` registrations load into `StaticData.QuestNpcStarts`.
- After selected reward completion, C# now mirrors the safe Java branch that opens the next same-NPC registered reward page when another `REWARD` quest is present.
- After selected reward completion, C# now mirrors the safe Java active quest fallback that sends `SmDialogWindow(targetObjectId, 10, 0)` when another same-NPC active talk quest remains.
- Live NPC-target reward-page actions send Java reward selection pages for `REWARD` quests and apply Java-equivalent reward-group correction in memory.
- Live NPC-target `SET_SUCCEED` sends Java close-dialog page `0` for reward-state pre-end report paths.

## Java Artifacts Touched

- `com.aionemu.gameserver.questEngine.handlers.AbstractQuestHandler.sendQuestEndDialog`.
- `com.aionemu.gameserver.model.templates.quest.QuestNpc`.

## C# Artifacts Touched

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`.
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionQuestFinishDialogBoundaryTests.cs`.
- `docs/Phase-6-Session-2816-Completion.md`.
- `docs/Phase-6-Session-2816-Handoff.md`.

## Validation Decision

- Changed surface: live connection dispatch and server packet send.
- Specific behavior/contract: selected reward completion sends page `10` for a same-NPC active registered talk quest, while same-NPC reward quests still take reward-page precedence and no-candidate cases still close.
- Focused C# commands:
  - `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests"`
  - `git diff --check`
- Focused Java/Maven command: not run; no narrow Java fixture exists for this socket-side branch. Java source was reviewed directly.
- Broad-validation trigger: live connection dispatch and server packet send.
- Broad .NET decision: skipped after focused validation; no shared parser, serializer, database schema, scheduler, or common world-state model was changed.

## Tests Run

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests"
```

Result: Passed, 34 total, 0 failed, 0 skipped.

Java/Maven: not run; no narrow Java fixture exists for this socket-side branch.

```powershell
git diff --check
```

Result: Passed; only normal workspace CRLF conversion warnings were reported.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `QuestNpc.addOnTalkEvent/getOnTalkEvent` | `QuestNpcStartRegistration.AddOnTalkEvent/OnTalkEvent` | Runtime static-data table | Partial | Unit Tested | Partial Parity | Stores unique talk quest ids in insertion order; other QuestNpc events remain unmodeled. |
| Java quest handler `registerQuestNpc(...).addOnTalkEvent(...)` calls | `QuestNpcStartJavaHandlerExtractor` and `StaticData.LoadQuestNpcStarts` | Runtime static-data loader | Partial | Unit Tested | Partial Parity | Loads direct/simple Java handler talk registrations into runtime data; dynamic expressions remain unresolved. |
| `AbstractQuestHandler.sendQuestEndDialog` post-finish reward scan | `GameServerConnection.CreateNpcSelectedRewardPostFinishDialog` | Quest dialog handler slice | Partial | Regression Tested | Partial Parity | Wires next registered reward-page branch and active talk selection page `10`; new startable quest selection and pre-quest continuation remain incomplete. |

## Known Gaps

- Full Java NPC controller, AI `onDialogSelect`, dynamic `QuestEngine.onDialog`, and arbitrary quest handler bodies remain incomplete.
- Java post-finish selected reward logic for new startable quests and follow-up pre-quest start dialogs is still incomplete.
- Dynamic Java handler registration expressions that the extractor cannot resolve remain intentionally absent from the C# talk/start table.
- `QuestNpc` kill/attack/distance/aggro registrations remain unmodeled in this C# table.
- Reward-group correction from the reward-selection page path remains in-memory unless normal quest persistence later saves it.
- Live quest finish still does not support bonus rewards, challenge task completion, arbitrary quest completion callback handler bodies, mentor NPC faction title/flag side effects, or broad nearby quest refresh fanout.

## Runtime Progress Gate For Next UOW

Recommended next UOW: `[Phase 6][UOW-2817] Wire selected reward post-finish new quest selection`

- Deferred/live behavior to advance: after NPC-target selected reward completion, send Java selection page `10` when the same NPC has a new startable quest candidate and no same-NPC reward or active quest took precedence.
- Java source of truth: `AbstractQuestHandler.sendQuestEndDialog` scans `QuestNpc.getOnQuestStart()` after `QuestService.finishQuest(env)`, evaluates `QuestService.checkStartConditions(env, true)`, and returns `sendQuestSelectionDialog(env)` when `npcHasNewQuest` is true.
- C# runtime artifact to wire/fix: extend `GameServerConnection.CreateNpcSelectedRewardPostFinishDialog` to consult `QuestNpcStartRegistration.OnQuestStart` plus the existing nearby quest start-condition service for safe new quest candidates after selected reward completion.
- Client-visible/state effect expected: live selected reward completion should send `SmDialogWindow(targetObjectId, 10, 0)` instead of close page `0` when a same-NPC new quest is startable.
- Why this is not preview-only/test-only/documentation-only: it would change a live server packet emitted after quest completion from the selected reward branch.

## Focused Validation Recipe For Next UOW

- Specific behavior/contract to prove: after selected reward completion, a same-NPC new startable quest sends selection page `10`, while same-NPC reward quests and active talk quests still take precedence and no-candidate cases still close.
- Focused C# command: `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests"`.
- Add adjacent start-condition service tests only if the UOW expands or changes that service rather than simply consuming it.
- Focused Java/Maven command: run only if a narrow Java fixture exists or is added for `sendQuestEndDialog` new quest post-finish selection; otherwise document Java source review.
- Broad-validation trigger: live connection dispatch and server packet send.
- Broad .NET decision: start focused; do not run broad validation unless focused evidence exposes wider risk.

## Safe Runtime Candidates

- Wire selected reward post-finish new quest selection page `10` using already-loaded `OnQuestStart` registrations and existing start-condition checks.
- Wire selected reward post-finish pre-quest continuation only if the UOW can safely reuse or port Java's `QuestStartConditions.checkPreConditions` equivalent into live code.
- Wire challenge task accept/completion only after adding or finding real C# runtime services for Java challenge task mutation/persistence/packet side effects.

## Summary Metrics

- Total Java artifacts touched/discovered in latest UOW: 2.
- Total artifacts ported or wired in latest UOW: 1 live selected reward post-finish active quest selection branch.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification/partial parity: 3.
- Total blocked artifacts: 3 new post-finish quest selection/pre-quest continuation, challenge task accept/completion, and mentor NPC faction side effects.
- Estimated overall migration completion: unchanged conservatively; Phase 6 remains in progress.
