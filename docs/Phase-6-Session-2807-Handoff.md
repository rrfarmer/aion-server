# Phase 6 Session 2807 Handoff

## Current Phase

Phase 6: Port Game Core

## Latest Completed UOW

`[Phase 6][UOW-2807] Wire NPC-target QUEST_ACCEPT_1 start follow-up page`

Commit made in this session:

- `[Phase 6][UOW-2807] Wire NPC-target QUEST_ACCEPT_1 start follow-up page`

## Current State

- `GameServerConnection.HandleDialogSelectAsync` handles supported self-target and NPC-target reportable auto-reward quest finish paths.
- The same live NPC-target handler sends `SmDialogWindow(targetObjectId, dialogActionId, questId)` for unhandled quest/page fallback actions after existing special cases do not consume the packet.
- `StaticData.QuestNpcStarts` loads Java/XML NPC-start registrations and gates live NPC-target quest starts.
- Live NPC-target `QUEST_ACCEPT_SIMPLE` starts a registered quest, sends `SmQuestAction`, closes the dialog with `SmDialogWindow(npcObjectId, 0, 0)`, and refreshes nearby quest markers.
- Live NPC-target `QUEST_ACCEPT_1` starts a registered quest, sends `SmQuestAction`, sends `SmDialogWindow(npcObjectId, 1003, questId)`, and refreshes nearby quest markers.

## Java Artifacts Touched

- `com.aionemu.gameserver.questEngine.handlers.AbstractQuestHandler.sendQuestStartDialog`.
- `com.aionemu.gameserver.services.QuestService.startQuest`.
- `com.aionemu.gameserver.model.DialogAction`.

## C# Artifacts Touched

- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmDialogSelect.cs`.
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`.
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionQuestFinishDialogBoundaryTests.cs`.
- `docs/Phase-6-Session-2807-Completion.md`.
- `docs/Phase-6-Session-2807-Handoff.md`.

## Validation Decision

- Changed surface: live connection dispatch, quest-state mutation, optional persistence, and server packet send.
- Specific behavior/contract: Java `QUEST_ACCEPT_1` starts a registered NPC quest and sends `SM_DIALOG_WINDOW(npcObjectId, 1003, questId)`.
- Focused C# command: `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~NearbyQuestStartConditionServiceTests|FullyQualifiedName~QuestNpcStart"`
- Focused Java/Maven command: not run; no narrow Java fixture exists for socket-side NPC-target `QUEST_ACCEPT_1`.
- Broad-validation trigger: live connection dispatch, quest-state mutation, and packet send.
- Broad .NET decision: skipped after focused validation; the filtered command compiled the edited project and exercised the live socket boundary plus adjacent NPC-start/start-condition coverage.

## Tests Run

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~NearbyQuestStartConditionServiceTests|FullyQualifiedName~QuestNpcStart"
```

Result: Passed, 54 total, 0 failed, 0 skipped.

Java/Maven: not run; no narrow Java fixture exists for this socket-side branch.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `AbstractQuestHandler.sendQuestStartDialog` `QUEST_ACCEPT_1` | `GameServerConnection.TryHandleNpcTargetQuestStartAcceptAsync` | Quest dialog handler slice | Partial | Regression Tested | Partial Parity | Starts registered NPC-target quests and sends Java follow-up page `1003`; full dynamic handler execution remains incomplete. |
| `QuestService.startQuest` | `GameServerConnection.TryHandleNpcTargetQuestStartAcceptAsync` | Quest mutation and packet send | Partial | Regression Tested | Partial Parity | Reuses existing start-condition, persistence, quest-action packet, and nearby refresh behavior; challenge task and NPC faction start side effects remain blocked. |
| `DialogAction` | `CmDialogSelect` | Dialog action constants | Partial | Regression Tested | Partial Parity | Adds live-consumed constants for `QUEST_ACCEPT_1` and `QUEST_ACCEPT_SIMPLE`; this is not a complete DialogAction port. |

## Known Gaps

- Full Java NPC controller, AI `onDialogSelect`, dynamic `QuestEngine.onDialog`, and arbitrary quest handler bodies remain incomplete.
- `QUEST_ACCEPT` is not wired in this UOW.
- Live quest finish still does not support bonus rewards, challenge task completion, arbitrary quest completion callback handler bodies, mentor NPC faction title/flag side effects, or broad nearby quest refresh fanout.
- Challenge task accept side effects and NPC faction start side effects remain blocked on clearer live C# services/state.
- The NPC-target interaction guard uses currently modeled C# facts; summon-owner, siege/zone, live abyss ranking, and dominion calculation restrictions still need fuller runtime parity.

## Runtime Progress Gate For Next UOW

Recommended next UOW: `[Phase 6][UOW-2808] Wire NPC-target QUEST_ACCEPT start follow-up page`

- Deferred/live behavior to advance: execute the remaining Java default accept branch that starts a registered NPC quest and then sends the start follow-up dialog page.
- Java source of truth: `AbstractQuestHandler.sendQuestStartDialog` branch for `QUEST_ACCEPT`, which calls `QuestService.startQuest(env)` and then `sendQuestDialog(env, 1003)` when the visible object is an NPC.
- C# runtime artifact to wire/fix: extend `GameServerConnection.TryHandleNpcTargetQuestStartAcceptAsync` and `CmDialogSelect` to support `QUEST_ACCEPT`.
- Client-visible/state/persistence effect expected: live NPC-target `QUEST_ACCEPT` starts the quest, persists when the service is available, sends `SmQuestAction`, and sends `SmDialogWindow(npcObjectId, 1003, questId)`.
- Why this is not preview-only/test-only/documentation-only: it would mutate live quest state and send real server packets from the socket handler.

## Focused Validation Recipe For Next UOW

- Specific behavior/contract to prove: a registered NPC-target `QUEST_ACCEPT` packet starts the quest and sends `SmDialogWindow(npcObjectId, 1003, questId)`.
- Focused C# command: start with `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~NearbyQuestStartConditionServiceTests|FullyQualifiedName~QuestNpcStart"`.
- Focused Java/Maven command: run only if a narrow Java fixture exists or is added for the selected handler/action.
- Broad-validation trigger: live connection dispatch, quest-state mutation, and packet send.
- Broad .NET decision: do not run unless focused evidence exposes wider risk or shared quest-state/packet serialization code is edited.

## Safe Runtime Candidates

- Wire NPC-target `QUEST_ACCEPT` quest start with Java page `1003`.
- Wire one concrete arbitrary quest completion callback only if its Java body can be ported directly and has clear live state or packet effects.
- Wire challenge task accept/completion only after adding or finding real C# runtime services for Java challenge task mutation/persistence/packet side effects.
- Wire mentor NPC faction quest finish or quest start side effects only if the required live mentor flag state and title packet artifacts exist.

## Summary Metrics

- Total Java artifacts touched/discovered in latest UOW: 3.
- Total artifacts ported or wired in latest UOW: 1 live NPC-target `QUEST_ACCEPT_1` quest start branch.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification/partial parity: 3.
- Total blocked artifacts: 3 challenge task accept/completion, NPC faction start side effects, and mentor NPC faction side effects.
- Estimated overall migration completion: unchanged conservatively; Phase 6 remains in progress.
