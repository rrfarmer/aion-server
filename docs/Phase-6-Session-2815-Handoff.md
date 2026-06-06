# Phase 6 Session 2815 Handoff

## Current Phase

Phase 6: Port Game Core

## Latest Completed UOW

`[Phase 6][UOW-2815] Wire selected reward post-finish reward page`

Commit made in this session:

- `[Phase 6][UOW-2815] Wire selected reward post-finish reward page`

## Current State

- `GameServerConnection.HandleDialogSelectAsync` handles supported self-target and NPC-target reportable auto-reward quest finish paths.
- Live NPC-target selected reward actions `8..23` can finish `REWARD` quests using existing reward projection, item/non-item reward application, quest completion mutation, persistence hook, and `SmQuestAction.Update`.
- Java handler `addOnTalkEvent` registrations now load into `StaticData.QuestNpcStarts`.
- After selected reward completion, C# now mirrors the safe Java branch that opens the next same-NPC registered reward page when another `REWARD` quest is present.
- Live NPC-target reward-page actions send Java reward selection pages for `REWARD` quests and apply Java-equivalent reward-group correction in memory.
- Live NPC-target `SET_SUCCEED` sends Java close-dialog page `0` for reward-state pre-end report paths.
- `StaticData.QuestNpcStarts` loads Java/XML NPC-start registrations and Java handler talk registrations used by live NPC-target quest dialog handling.

## Java Artifacts Touched

- `com.aionemu.gameserver.questEngine.handlers.AbstractQuestHandler.sendQuestEndDialog`.
- `com.aionemu.gameserver.model.templates.quest.QuestNpc`.
- Java quest handler `registerQuestNpc(...).addOnTalkEvent(...)` registrations.

## C# Artifacts Touched

- `dotnetConversion/src/Aion.GameServer/Dataholders/QuestNpcStartJavaHandlerExtractor.cs`.
- `dotnetConversion/src/Aion.GameServer/Dataholders/QuestNpcStartTable.cs`.
- `dotnetConversion/src/Aion.GameServer/Dataholders/StaticData.cs`.
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`.
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionQuestFinishDialogBoundaryTests.cs`.
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestNpcStartJavaHandlerExtractorTests.cs`.
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestNpcStartTableTests.cs`.
- `docs/Phase-6-Session-2815-Completion.md`.
- `docs/Phase-6-Session-2815-Handoff.md`.

## Validation Decision

- Changed surface: live static-data loading, live connection dispatch, and server packet send.
- Specific behavior/contract: Java handler `addOnTalkEvent` registrations load into `StaticData.QuestNpcStarts`, and after selected reward completion a same-NPC `REWARD` quest registered on talk opens its reward page.
- Focused C# commands:
  - `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~QuestNpcStartTableTests|FullyQualifiedName~QuestNpcStartJavaHandlerExtractorTests|FullyQualifiedName~QuestNpcStartRegistrationSourceLoaderTests"`
  - `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~QuestNpcStartXmlExtractorTests|FullyQualifiedName~QuestNpcStartTableTests"`
  - `git diff --check`
- Focused Java/Maven command: not run; no narrow Java fixture exists for this socket-side branch. Java source was reviewed directly.
- Broad-validation trigger: live static-data loading and server packet send.
- Broad .NET decision: skipped after focused validation; no shared XML parser primitive, packet serializer, database schema, scheduler, or common world-state model was changed.

## Tests Run

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~QuestNpcStartTableTests|FullyQualifiedName~QuestNpcStartJavaHandlerExtractorTests|FullyQualifiedName~QuestNpcStartRegistrationSourceLoaderTests"
```

Result: Passed, 45 total, 0 failed, 0 skipped.

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~QuestNpcStartXmlExtractorTests|FullyQualifiedName~QuestNpcStartTableTests"
```

Result: Passed, 8 total, 0 failed, 0 skipped.

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
| `AbstractQuestHandler.sendQuestEndDialog` post-finish reward scan | `GameServerConnection.CreateNpcSelectedRewardPostFinishDialog` | Quest dialog handler slice | Partial | Regression Tested | Partial Parity | Wires next registered reward-page branch after selected completion; active/new quest selection and pre-quest continuation remain incomplete. |

## Known Gaps

- Full Java NPC controller, AI `onDialogSelect`, dynamic `QuestEngine.onDialog`, and arbitrary quest handler bodies remain incomplete.
- Java post-finish selected reward logic for active quests, new startable quests, and follow-up pre-quest start dialogs is still incomplete.
- Dynamic Java handler registration expressions that the extractor cannot resolve are intentionally left out of the C# talk/start table.
- `QuestNpc` kill/attack/distance/aggro registrations remain unmodeled in this C# table.
- Reward-group correction from the reward-selection page path remains in-memory unless normal quest persistence later saves it.
- Live quest finish still does not support bonus rewards, challenge task completion, arbitrary quest completion callback handler bodies, mentor NPC faction title/flag side effects, or broad nearby quest refresh fanout.

## Runtime Progress Gate For Next UOW

Recommended next UOW: `[Phase 6][UOW-2816] Wire selected reward post-finish active quest selection`

- Deferred/live behavior to advance: after NPC-target selected reward completion, send Java selection page `10` when the same NPC still has an active registered talk quest and no same-NPC reward quest took precedence.
- Java source of truth: `AbstractQuestHandler.sendQuestEndDialog` sets `npcHasActiveQuest` while scanning `QuestNpc.getOnTalkEvent()` and returns `sendQuestSelectionDialog(env)` when active/new candidates remain.
- C# runtime artifact to wire/fix: extend `GameServerConnection.CreateNpcSelectedRewardPostFinishDialog` to detect safe active same-NPC talk quests from `StaticData.QuestNpcStarts.OnTalkEvent` and live `PlayerQuestState`.
- Client-visible/state/persistence effect expected: live selected reward completion should send `SmDialogWindow(targetObjectId, 10, 0)` instead of close page `0` when another same-NPC active quest is present.
- Why this is not preview-only/test-only/documentation-only: it would change a live server packet emitted after quest completion from the selected reward branch.

## Focused Validation Recipe For Next UOW

- Specific behavior/contract to prove: after selected reward completion, a same-NPC active registered talk quest sends selection page `10`, while same-NPC reward quests still take reward-page precedence and no-candidate cases still close.
- Focused C# command: `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests"`.
- Add adjacent dataholder tests only if the UOW changes `QuestNpcStartTable` or extractor behavior again.
- Focused Java/Maven command: run only if a narrow Java fixture exists or is added for `sendQuestEndDialog` active quest post-finish selection; otherwise document Java source review.
- Broad-validation trigger: live connection dispatch and server packet send.
- Broad .NET decision: start focused; do not run broad validation unless focused evidence exposes wider risk.

## Safe Runtime Candidates

- Wire selected reward post-finish active quest selection page `10` using already-loaded `OnTalkEvent` registrations and live `PlayerQuestState.Status == "START"`.
- Wire selected reward post-finish new quest selection only if a safe start-condition service exists or is ported and consumed in live code in the same UOW.
- Wire challenge task accept/completion only after adding or finding real C# runtime services for Java challenge task mutation/persistence/packet side effects.

## Summary Metrics

- Total Java artifacts touched/discovered in latest UOW: 3.
- Total artifacts ported or wired in latest UOW: 1 live selected reward post-finish reward-page branch plus Java talk registration runtime loading.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification/partial parity: 3.
- Total blocked artifacts: 4 active/new post-finish quest selection, challenge task accept/completion, NPC faction start side effects, and mentor NPC faction side effects.
- Estimated overall migration completion: unchanged conservatively; Phase 6 remains in progress.
