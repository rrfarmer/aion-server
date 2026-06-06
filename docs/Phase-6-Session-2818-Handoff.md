# Phase 6 Session 2818 Handoff

## Current Phase

Phase 6: Port Game Core

## Latest Completed UOW

`[Phase 6][UOW-2818] Wire selected reward post-finish pre-quest continuation start page`

Commit made in this session:

- `[Phase 6][UOW-2818] Wire selected reward post-finish pre-quest continuation start page`

## Current State

- `GameServerConnection.HandleDialogSelectAsync` handles supported self-target and NPC-target reportable auto-reward quest finish paths.
- Live NPC-target selected reward actions `8..23` can finish `REWARD` quests using existing reward projection, item/non-item reward application, quest completion mutation, persistence hook, and `SmQuestAction.Update`.
- Java handler `addOnTalkEvent` registrations load into `StaticData.QuestNpcStarts`.
- After selected reward completion, C# mirrors the safe Java branch that opens the next same-NPC registered reward page when another `REWARD` quest is present.
- After selected reward completion, C# mirrors the safe Java active quest fallback that sends `SmDialogWindow(targetObjectId, 10, 0)` when another same-NPC active talk quest remains.
- After selected reward completion, C# mirrors the Java new quest fallback that sends `SmDialogWindow(targetObjectId, 10, 0)` when another same-NPC start quest passes modeled start conditions.
- After selected reward completion, C# now sends the modeled default follow-up start page `SmDialogWindow(targetObjectId, 1011, followUpQuestId)` when a same-NPC startable quest has the completed quest in XML finished preconditions and passes the modeled Java acceptability check.
- `NearbyQuestStartConditionService.CheckNearbyStartConditions` lets callers choose the allowed min-level diff, preserving nearby refresh diff `2` while supporting Java's exact diff `0` for `sendQuestEndDialog`.
- Live NPC-target reward-page actions send Java reward selection pages for `REWARD` quests and apply Java-equivalent reward-group correction in memory.
- Live NPC-target `SET_SUCCEED` sends Java close-dialog page `0` for reward-state pre-end report paths.

## Java Artifacts Touched

- `com.aionemu.gameserver.questEngine.handlers.AbstractQuestHandler.sendQuestEndDialog`.
- `com.aionemu.gameserver.questEngine.handlers.AbstractQuestHandler.isAcceptableQuest`.
- `com.aionemu.gameserver.model.templates.quest.QuestNpc`.
- `com.aionemu.gameserver.services.QuestService.checkStartConditions`.

## C# Artifacts Touched

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`.
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionQuestFinishDialogBoundaryTests.cs`.
- `docs/Phase-6-Session-2818-Completion.md`.
- `docs/Phase-6-Session-2818-Handoff.md`.

## Validation Decision

- Changed surface: live connection dispatch and server packet send.
- Specific behavior/contract: selected reward completion sends follow-up start page `1011` for a same-NPC new quest whose XML finished precondition matches the completed quest, while generic new startable quests still send page `10`.
- Focused C# command: `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~NearbyQuestStartConditionServiceTests"`
- Focused Java/Maven command: not run; no narrow Java fixture exists for this socket-side branch. Java source was reviewed directly.
- Broad-validation trigger: live connection dispatch and server packet send.
- Broad .NET decision: skipped after focused validation; no shared parser, serializer, database schema, scheduler, or common world-state model was changed.

## Tests Run

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~NearbyQuestStartConditionServiceTests"
```

Result: Passed, 49 total, 0 failed, 0 skipped.

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
| `AbstractQuestHandler.sendQuestEndDialog` post-finish reward/new quest/pre-quest scan | `GameServerConnection.CreateNpcSelectedRewardPostFinishDialog` | Quest dialog handler slice | Partial | Regression Tested | Partial Parity | Wires next registered reward-page branch, active talk page `10`, new startable quest page `10`, and modeled default pre-quest continuation page `1011`; custom handler bodies remain incomplete. |
| `AbstractQuestHandler.isAcceptableQuest` | `GameServerConnection.IsNpcSelectedRewardPostFinishAcceptableQuest` | Helper | Partial | Regression Tested | Partial Parity | Uses modeled reward projections and quest drops to reject empty rewards; depends on currently loaded C# reward/drop data. |

## Known Gaps

- Full Java NPC controller, AI `onDialogSelect`, dynamic `QuestEngine.onDialog`, and arbitrary quest handler bodies remain incomplete.
- Handler-specific follow-up quest start pages and quest item side effects remain incomplete.
- Dynamic Java handler registration expressions that the extractor cannot resolve remain intentionally absent from the C# talk/start table.
- `QuestNpc` kill/attack/distance/aggro registrations remain unmodeled in this C# table.
- Reward-group correction from the reward-selection page path remains in-memory unless normal quest persistence later saves it.
- Live quest finish still does not support bonus rewards, challenge task completion, arbitrary quest completion callback handler bodies, mentor NPC faction title/flag side effects, or broad nearby quest refresh fanout.

## Runtime Progress Gate For Next UOW

Recommended next UOW: `[Phase 6][UOW-2819] Persist reward group correction from NPC reward page`

- Deferred/live behavior to advance: when an NPC reward-page request corrects an invalid reward group before sending the reward page, persist the corrected live quest state instead of leaving the fix only in memory.
- Java source of truth: `AbstractQuestHandler.sendQuestEndDialog` calls `QuestService.validateAndFixRewardGroup(qs, questId)` before `sendQuestDialog(...)`; Java quest state updates are expected to flow through the normal quest state persistence path.
- C# runtime artifact to wire/fix: update `GameServerConnection.TryHandleNpcTargetQuestRewardSelectionPageAsync` so the existing reward-group correction path persists the corrected `PlayerQuestState` through `PlayerEnterWorldService.PersistQuestStartAsync` when that persistence service is available.
- Client-visible/state/persistence effect expected: malformed or stale reward group state corrected during live NPC reward page selection should be durable in the existing quest persistence shape.
- Why this is not preview-only/test-only/documentation-only: it changes live quest state persistence for a packet handler path.

## Focused Validation Recipe For Next UOW

- Specific behavior/contract to prove: NPC reward-page selection with an invalid reward group corrects the in-memory state, sends the Java reward page, and calls quest persistence for the corrected state when a persistence service is available.
- Focused C# command: `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests"`.
- Add adjacent persistence fake tests only if the existing fixture cannot observe persistence calls.
- Focused Java/Maven command: run only if a narrow Java fixture exists for `QuestService.validateAndFixRewardGroup`; otherwise document Java source review.
- Broad-validation trigger: live connection dispatch and persistence side effect.
- Broad .NET decision: start focused; do not run broad validation unless focused evidence exposes wider risk.

## Safe Runtime Candidates

- Persist reward-group correction from NPC reward page selection.
- Wire challenge task accept/completion only after adding or finding real C# runtime services for Java challenge task mutation/persistence/packet side effects.
- Wire mentor NPC faction title/flag side effects only after locating the Java runtime mutation and persistence path.

## Summary Metrics

- Total Java artifacts touched/discovered in latest UOW: 4.
- Total artifacts ported or wired in latest UOW: 1 live selected reward post-finish pre-quest continuation branch.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification/partial parity: 4.
- Total blocked artifacts: 3 custom handler-specific start dialog behavior, challenge task accept/completion, and mentor NPC faction side effects.
- Estimated overall migration completion: unchanged conservatively; Phase 6 remains in progress.
