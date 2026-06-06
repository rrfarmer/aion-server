# Phase 6 Session 2806 Handoff

## Current Phase

Phase 6: Port Game Core

## Latest Completed UOW

`[Phase 6][UOW-2806] Wire live NPC-target quest accept simple starts`

Commit made in this session:

- `[Phase 6][UOW-2806] Wire live NPC-target quest accept simple starts`

## Current State

- `GameServerConnection.HandleDialogSelectAsync` handles supported self-target and NPC-target reportable auto-reward quest finish paths.
- The same live NPC-target handler sends `SmDialogWindow(targetObjectId, dialogActionId, questId)` for unhandled quest/page fallback actions after existing special cases do not consume the packet.
- `StaticData.QuestNpcStarts` now loads Java/XML NPC-start registrations and is consumed by live NPC-target `QUEST_ACCEPT_SIMPLE`.
- Live NPC-target `QUEST_ACCEPT_SIMPLE` can start a registered quest, persist through `PlayerEnterWorldService.PersistQuestStartAsync` when present, send `SmQuestAction.ADD` or `UPDATE`, send close-dialog `SmDialogWindow(npcObjectId, 0, 0)`, and refresh nearby quest markers.
- `StaticData.QuestCompletionFollowUps` loads Java quest handler source files from the configured quest handler directory and captures default completion follow-up registrations for literal ids, simple int arrays, and no-argument `defaultOnQuestCompletedEvent(env)` calls.

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT.runImpl`.
- `com.aionemu.gameserver.controllers.NpcController.onDialogSelect`.
- `com.aionemu.gameserver.questEngine.handlers.AbstractQuestHandler.sendQuestStartDialog`.
- `com.aionemu.gameserver.services.QuestService.startQuest`.
- `com.aionemu.gameserver.model.templates.quest.QuestNpc.addOnQuestStart`.

## C# Artifacts Touched

- `dotnetConversion/src/Aion.GameServer/Dataholders/StaticData.cs`.
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`.
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionQuestFinishDialogBoundaryTests.cs`.
- `docs/Phase-6-Session-2806-Completion.md`.
- `docs/Phase-6-Session-2806-Handoff.md`.

## Validation Decision

- Changed surface: static data runtime loading, live connection dispatch, quest-state mutation, optional persistence, and server packet send.
- Specific behavior/contract: Java `QUEST_ACCEPT_SIMPLE` starts the quest and closes the NPC dialog when the target NPC is registered as a quest starter.
- Focused C# command: `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~NearbyQuestStartConditionServiceTests|FullyQualifiedName~QuestNpcStart"`
- Focused Java/Maven command: not run; no narrow Java fixture exists for socket-side NPC-target `QUEST_ACCEPT_SIMPLE`.
- Broad-validation trigger: live connection dispatch, quest-state mutation, static-data load, and packet send.
- Broad .NET decision: skipped after focused validation; the filter compiled the edited project, exercised the live boundary, and covered adjacent NPC-start extractor/table tests.

## Tests Run

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~NearbyQuestStartConditionServiceTests|FullyQualifiedName~QuestNpcStart"
```

Result: Passed, 53 total, 0 failed, 0 skipped.

Java/Maven: not run; no narrow Java fixture exists for the socket-side NPC-target accept-simple path.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `QuestNpc.addOnQuestStart` registrations | `StaticData.QuestNpcStarts` | Runtime static data | Partial | Regression Tested | Partial Parity | Java/XML NPC-start registrations are now loaded into runtime static data and consumed by live code. |
| `AbstractQuestHandler.sendQuestStartDialog` `QUEST_ACCEPT_SIMPLE` | `GameServerConnection.TryHandleNpcTargetQuestAcceptSimpleAsync` | Quest dialog handler slice | Partial | Regression Tested | Partial Parity | Starts registered NPC-target quests and closes the dialog for the simple accept action. |
| `QuestService.startQuest` | `GameServerConnection.TryHandleNpcTargetQuestAcceptSimpleAsync` | Quest mutation and packet send | Partial | Regression Tested | Partial Parity | Mutates quest state, persists when the service exists, sends quest action, and refreshes nearby quests; challenge task and NPC faction side effects are still not wired. |

## Known Gaps

- Full Java NPC controller, AI `onDialogSelect`, dynamic `QuestEngine.onDialog`, and arbitrary quest handler bodies remain incomplete.
- `QUEST_ACCEPT` and `QUEST_ACCEPT_1` are not wired in this UOW because their Java path can send follow-up start pages that need separate packet coverage.
- Live quest finish still does not support bonus rewards, challenge task completion, arbitrary quest completion callback handler bodies, mentor NPC faction title/flag side effects, or broad nearby quest refresh fanout.
- Challenge task accept side effects and NPC faction start side effects remain blocked on clearer live C# services/state.
- The NPC-target interaction guard uses currently modeled C# facts; summon-owner, siege/zone, live abyss ranking, and dominion calculation restrictions still need fuller runtime parity.

## Runtime Progress Gate For Next UOW

Recommended next UOW: `[Phase 6][UOW-2807] Wire NPC-target QUEST_ACCEPT_1 start follow-up page`

- Deferred/live behavior to advance: execute the Java non-simple accept branch that starts a registered NPC quest and then sends the start follow-up dialog page.
- Java source of truth: `AbstractQuestHandler.sendQuestStartDialog` branch for `QUEST_ACCEPT_1`, which calls `QuestService.startQuest(env)` and then `sendQuestDialog(env, 1003)` when the visible object is an NPC.
- C# runtime artifact to wire/fix: extend `GameServerConnection.TryHandleNpcTargetQuestAcceptSimpleAsync` or split it into a shared NPC quest-start helper that supports `QUEST_ACCEPT_1` and sends `SmDialogWindow(npcObjectId, 1003, questId)`.
- Client-visible/state/persistence effect expected: live NPC-target `QUEST_ACCEPT_1` starts the quest, persists when the service is available, sends `SmQuestAction`, and sends the Java follow-up start page.
- Why this is not preview-only/test-only/documentation-only: it would mutate live quest state and send real server packets from the socket handler.

## Focused Validation Recipe For Next UOW

- Specific behavior/contract to prove: a registered NPC-target `QUEST_ACCEPT_1` packet starts the quest and sends `SmDialogWindow(npcObjectId, 1003, questId)`.
- Focused C# command: start with `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~NearbyQuestStartConditionServiceTests|FullyQualifiedName~QuestNpcStart"`.
- Focused Java/Maven command: run only if a narrow Java fixture exists or is added for the selected handler/action.
- Broad-validation trigger: live connection dispatch, quest-state mutation, static-data load, and packet send.
- Broad .NET decision: do not run unless focused evidence exposes wider risk or shared quest-state/packet serialization code is edited.

## Safe Runtime Candidates

- Wire NPC-target `QUEST_ACCEPT_1` quest start with Java page `1003`.
- Wire NPC-target `QUEST_ACCEPT` only after confirming the exact Java follow-up page and current client action mapping in this codebase.
- Wire one concrete arbitrary quest completion callback only if its Java body can be ported directly and has clear live state or packet effects.
- Wire challenge task accept/completion only after adding or finding real C# runtime services for Java challenge task mutation/persistence/packet side effects.
- Wire mentor NPC faction quest finish or quest start side effects only if the required live mentor flag state and title packet artifacts exist.

## Summary Metrics

- Total Java artifacts touched/discovered in latest UOW: 5.
- Total artifacts ported or wired in latest UOW: 1 live NPC-target `QUEST_ACCEPT_SIMPLE` quest start path plus runtime NPC-start data exposure.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification/partial parity: 3.
- Total blocked artifacts: 3 challenge task accept/completion, NPC faction start side effects, and mentor NPC faction side effects.
- Estimated overall migration completion: unchanged conservatively; Phase 6 remains in progress.
