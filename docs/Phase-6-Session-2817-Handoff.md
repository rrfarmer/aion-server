# Phase 6 Session 2817 Handoff

## Current Phase

Phase 6: Port Game Core

## Latest Completed UOW

`[Phase 6][UOW-2817] Wire selected reward post-finish new quest selection`

Commit made in this session:

- `[Phase 6][UOW-2817] Wire selected reward post-finish new quest selection`

## Current State

- `GameServerConnection.HandleDialogSelectAsync` handles supported self-target and NPC-target reportable auto-reward quest finish paths.
- Live NPC-target selected reward actions `8..23` can finish `REWARD` quests using existing reward projection, item/non-item reward application, quest completion mutation, persistence hook, and `SmQuestAction.Update`.
- Java handler `addOnTalkEvent` registrations load into `StaticData.QuestNpcStarts`.
- After selected reward completion, C# mirrors the safe Java branch that opens the next same-NPC registered reward page when another `REWARD` quest is present.
- After selected reward completion, C# mirrors the safe Java active quest fallback that sends `SmDialogWindow(targetObjectId, 10, 0)` when another same-NPC active talk quest remains.
- After selected reward completion, C# mirrors the Java new quest fallback that sends `SmDialogWindow(targetObjectId, 10, 0)` when another same-NPC start quest passes modeled start conditions.
- `NearbyQuestStartConditionService.CheckNearbyStartConditions` now lets callers choose the allowed min-level diff, preserving nearby refresh diff `2` while supporting Java's exact diff `0` for `sendQuestEndDialog`.
- Live NPC-target reward-page actions send Java reward selection pages for `REWARD` quests and apply Java-equivalent reward-group correction in memory.
- Live NPC-target `SET_SUCCEED` sends Java close-dialog page `0` for reward-state pre-end report paths.

## Java Artifacts Touched

- `com.aionemu.gameserver.questEngine.handlers.AbstractQuestHandler.sendQuestEndDialog`.
- `com.aionemu.gameserver.services.QuestService.checkStartConditions`.
- `com.aionemu.gameserver.model.templates.quest.QuestNpc`.

## C# Artifacts Touched

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`.
- `dotnetConversion/src/Aion.GameServer/Services/NearbyQuestStartConditionService.cs`.
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionQuestFinishDialogBoundaryTests.cs`.
- `dotnetConversion/tests/Aion.GameServer.Tests/NearbyQuestStartConditionServiceTests.cs`.
- `docs/Phase-6-Session-2817-Completion.md`.
- `docs/Phase-6-Session-2817-Handoff.md`.

## Validation Decision

- Changed surface: live connection dispatch, server packet send, and shared start-condition service parameterization.
- Specific behavior/contract: selected reward completion sends page `10` for a same-NPC new startable quest; the close fallback remains close when same-NPC start quests are repeat-exhausted or underleveled under Java's exact min-level gate.
- Focused C# command: `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~NearbyQuestStartConditionServiceTests"`
- Focused Java/Maven command: not run; no narrow Java fixture exists for this socket-side branch. Java source was reviewed directly.
- Broad-validation trigger: live connection dispatch and server packet send.
- Broad .NET decision: skipped after focused validation; no shared parser, serializer, database schema, scheduler, or common world-state model was changed.

## Tests Run

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~NearbyQuestStartConditionServiceTests"
```

Result: Passed, 48 total, 0 failed, 0 skipped.

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
| `AbstractQuestHandler.sendQuestEndDialog` post-finish reward/new quest scan | `GameServerConnection.CreateNpcSelectedRewardPostFinishDialog` | Quest dialog handler slice | Partial | Regression Tested | Partial Parity | Wires next registered reward-page branch, active talk page `10`, and new startable quest page `10`; pre-quest continuation remains incomplete. |
| `QuestService.checkStartConditions` min-level overload behavior | `NearbyQuestStartConditionService.CheckNearbyStartConditions` | Service | Partial | Unit Tested | Partial Parity | Supports caller-selected min-level diff for modeled checks; Java skip flags and warning packets remain outside this service. |

## Known Gaps

- Full Java NPC controller, AI `onDialogSelect`, dynamic `QuestEngine.onDialog`, and arbitrary quest handler bodies remain incomplete.
- Java post-finish selected reward logic for follow-up pre-quest start dialogs is still incomplete.
- Dynamic Java handler registration expressions that the extractor cannot resolve remain intentionally absent from the C# talk/start table.
- `QuestNpc` kill/attack/distance/aggro registrations remain unmodeled in this C# table.
- Reward-group correction from the reward-selection page path remains in-memory unless normal quest persistence later saves it.
- Live quest finish still does not support bonus rewards, challenge task completion, arbitrary quest completion callback handler bodies, mentor NPC faction title/flag side effects, or broad nearby quest refresh fanout.

## Runtime Progress Gate For Next UOW

Recommended next UOW: `[Phase 6][UOW-2818] Wire selected reward post-finish pre-quest continuation start page`

- Deferred/live behavior to advance: after NPC-target selected reward completion, directly show the follow-up quest start dialog when a newly startable same-NPC quest has the just-finished quest as an acceptable XML `<finished>` precondition.
- Java source of truth: `AbstractQuestHandler.sendQuestEndDialog` scans `QuestNpc.getOnQuestStart()`, checks `QuestService.checkStartConditions(player, questId, false)`, then for matching `XMLStartCondition.getFinishedPreconditions()` sets `DialogAction.QUEST_SELECT`, `dialogContinuationFromPreQuest = true`, and re-enters `QuestEngine.onDialog(env)`.
- C# runtime artifact to wire/fix: extend `GameServerConnection.CreateNpcSelectedRewardPostFinishDialog` to detect modeled acceptable XML finished-precondition follow-ups and emit the existing quest-start dialog packet for the follow-up quest, likely `SmDialogWindow(targetObjectId, 1011, followUpQuestId)` for the default no-state quest select page.
- Client-visible/state effect expected: live selected reward completion should send the follow-up quest start page instead of generic page `10` when Java would directly open the follow-up start dialog.
- Why this is not preview-only/test-only/documentation-only: it would change a live server packet emitted after selected reward completion.

## Focused Validation Recipe For Next UOW

- Specific behavior/contract to prove: after selected reward completion, a same-NPC new quest with an acceptable XML finished precondition for the completed quest sends the follow-up start dialog page, while generic new startable quests still send page `10`.
- Focused C# command: `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~NearbyQuestStartConditionServiceTests"`.
- Add adjacent XML start-condition extractor tests only if the UOW changes XML extraction rather than consuming existing `NearbyQuestTemplateSummary.XmlStartConditions`.
- Focused Java/Maven command: run only if a narrow Java fixture exists or is added for `sendQuestEndDialog` pre-quest continuation; otherwise document Java source review.
- Broad-validation trigger: live connection dispatch and server packet send.
- Broad .NET decision: start focused; do not run broad validation unless focused evidence exposes wider risk.

## Safe Runtime Candidates

- Wire selected reward post-finish pre-quest continuation start page using existing XML finished-precondition data and existing quest-start dialog page conventions.
- Wire challenge task accept/completion only after adding or finding real C# runtime services for Java challenge task mutation/persistence/packet side effects.
- Wire mentor NPC faction title/flag side effects only after locating the Java runtime mutation and persistence path.

## Summary Metrics

- Total Java artifacts touched/discovered in latest UOW: 3.
- Total artifacts ported or wired in latest UOW: 1 live selected reward post-finish new quest selection branch plus one start-condition service parameter needed by that live branch.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification/partial parity: 4.
- Total blocked artifacts: 3 pre-quest continuation, challenge task accept/completion, and mentor NPC faction side effects.
- Estimated overall migration completion: unchanged conservatively; Phase 6 remains in progress.
