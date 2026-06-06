# Phase 6 Session 2815 Completion

## Unit Of Work

`[Phase 6][UOW-2815] Wire selected reward post-finish reward page`

## Runtime Progress Gate

- Deferred/live behavior advanced: after live NPC-target selected reward completion, the server can open the next same-NPC registered reward page instead of always closing.
- Java source of truth: `AbstractQuestHandler.sendQuestEndDialog` scans `QuestNpc.getOnTalkEvent()` after `QuestService.finishQuest(env)` and re-enters the reward dialog for another `REWARD` quest.
- C# runtime artifact wired: `QuestNpcStartTable` now stores `OnTalkEvent`, `QuestNpcStartJavaHandlerExtractor` loads Java `addOnTalkEvent` registrations into runtime static data, and `GameServerConnection.CreateNpcSelectedRewardPostFinishDialog` chooses the follow-up packet.
- Client-visible/runtime-loading effect changed: Java handler `addOnTalkEvent` registrations are loaded into C# runtime structures, and selected reward completion can now send `SmDialogWindow(target, rewardPage, nextQuestId)` for the next registered reward quest.
- Why this is not preview-only/test-only/documentation-only: it loads Java handler registration data into live C# static data and changes a real server packet emitted after live quest completion.

## Java Parity Notes

- Java `QuestNpc.addOnTalkEvent` stores each quest id once and preserves insertion order in a list.
- Java post-finish selected reward logic skips completed quests naturally because the just-finished quest is no longer in `QuestStatus.REWARD`.
- This UOW wires only the next-registered reward quest branch. Java's active/new quest selection and pre-quest continuation branches are still incomplete in C#.

## C# Artifacts Changed

- `dotnetConversion/src/Aion.GameServer/Dataholders/QuestNpcStartJavaHandlerExtractor.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/QuestNpcStartTable.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/StaticData.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionQuestFinishDialogBoundaryTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestNpcStartJavaHandlerExtractorTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestNpcStartTableTests.cs`

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
- Why this scope is sufficient: the boundary test loads a Java handler fixture into runtime static data, completes a live selected reward quest, and asserts the serialized follow-up dialog page for the next reward quest.

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

## Test Evidence

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleDialogSelectAsync_NpcTargetSelectedRewardOpensNextRegisteredRewardPage` | Regression | Java source review: `AbstractQuestHandler.sendQuestEndDialog` and `QuestNpc.addOnTalkEvent` | Selected reward completion opens reward page `5` for another same-NPC `REWARD` quest registered on talk | Filtered C# socket boundary test with Java handler fixture, live inventory/quest mutation, and serialized packet assertions | Does not cover Java active/new quest selection branches |
| `ExtractsLiteralNpcIdWithInheritedQuestId` and adjacent extractor tests | Unit | Java source review: handler `registerQuestNpc(...).addOnTalkEvent(...)` registrations | Java handler talk registrations are extracted alongside start registrations | Filtered dataholder tests | Dynamic expressions remain unresolved by design |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.templates.quest.QuestNpc.addOnTalkEvent/getOnTalkEvent` | `Aion.GameServer.Dataholders.QuestNpcStartRegistration.AddOnTalkEvent/OnTalkEvent` | Runtime static-data table | Partial | Unit Tested | Partial Parity | Stores unique talk quest ids in insertion order; other QuestNpc events remain unmodeled. |
| Java quest handler `registerQuestNpc(...).addOnTalkEvent(...)` calls | `Aion.GameServer.Dataholders.QuestNpcStartJavaHandlerExtractor` and `StaticData.LoadQuestNpcStarts` | Runtime static-data loader | Partial | Unit Tested | Partial Parity | Loads direct/simple Java handler talk registrations into runtime data; dynamic expressions remain unresolved. |
| `AbstractQuestHandler.sendQuestEndDialog` post-finish reward scan | `GameServerConnection.CreateNpcSelectedRewardPostFinishDialog` | Quest dialog handler slice | Partial | Regression Tested | Partial Parity | Wires next registered reward-page branch after selected completion; active/new quest selection and pre-quest continuation remain incomplete. |

## Known Gaps

- Full Java NPC controller, AI `onDialogSelect`, dynamic `QuestEngine.onDialog`, and arbitrary quest handler bodies remain incomplete.
- Java post-finish selected reward logic for active quests, new startable quests, and follow-up pre-quest start dialogs is still incomplete.
- `QuestNpc` kill/attack/distance/aggro registrations remain unmodeled in this C# table.
- Live quest finish still does not support bonus rewards, challenge task completion, arbitrary quest completion callback handler bodies, mentor NPC faction title/flag side effects, or broad nearby quest refresh fanout.
