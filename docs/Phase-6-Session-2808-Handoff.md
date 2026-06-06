# Phase 6 Session 2808 Handoff

## Current Phase

Phase 6: Port Game Core

## Latest Completed UOW

`[Phase 6][UOW-2808] Wire NPC-target QUEST_ACCEPT start follow-up page`

Commit made in this session:

- `[Phase 6][UOW-2808] Wire NPC-target QUEST_ACCEPT start follow-up page`

## Current State

- `GameServerConnection.HandleDialogSelectAsync` handles supported self-target and NPC-target reportable auto-reward quest finish paths.
- The same live NPC-target handler sends `SmDialogWindow(targetObjectId, dialogActionId, questId)` for unhandled quest/page fallback actions after existing special cases do not consume the packet.
- `StaticData.QuestNpcStarts` loads Java/XML NPC-start registrations and gates live NPC-target quest starts.
- Live NPC-target `QUEST_ACCEPT`, `QUEST_ACCEPT_1`, and `QUEST_ACCEPT_SIMPLE` now start registered quests, send `SmQuestAction`, and send the Java follow-up dialog page or close-dialog packet.

## Java Artifacts Touched

- `com.aionemu.gameserver.questEngine.handlers.AbstractQuestHandler.sendQuestStartDialog`.
- `com.aionemu.gameserver.services.QuestService.startQuest`.
- `com.aionemu.gameserver.model.DialogAction`.

## C# Artifacts Touched

- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmDialogSelect.cs`.
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`.
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionQuestFinishDialogBoundaryTests.cs`.
- `docs/Phase-6-Session-2808-Completion.md`.
- `docs/Phase-6-Session-2808-Handoff.md`.

## Validation Decision

- Changed surface: live connection dispatch, quest-state mutation, optional persistence, and server packet send.
- Specific behavior/contract: Java `QUEST_ACCEPT` starts a registered NPC quest and sends `SM_DIALOG_WINDOW(npcObjectId, 1003, questId)`.
- Focused C# command: `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~NearbyQuestStartConditionServiceTests|FullyQualifiedName~QuestNpcStart"`
- Focused Java/Maven command: not run; no narrow Java fixture exists for socket-side NPC-target `QUEST_ACCEPT`.
- Broad-validation trigger: live connection dispatch, quest-state mutation, and packet send.
- Broad .NET decision: skipped after focused validation; the filtered command compiled the edited project and exercised the live socket boundary plus adjacent NPC-start/start-condition coverage.

## Tests Run

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~NearbyQuestStartConditionServiceTests|FullyQualifiedName~QuestNpcStart"
```

Result: Passed, 55 total, 0 failed, 0 skipped.

Java/Maven: not run; no narrow Java fixture exists for this socket-side branch.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `AbstractQuestHandler.sendQuestStartDialog` `QUEST_ACCEPT` | `GameServerConnection.TryHandleNpcTargetQuestStartAcceptAsync` | Quest dialog handler slice | Partial | Regression Tested | Partial Parity | Starts registered NPC-target quests and sends Java follow-up page `1003`; full dynamic handler execution remains incomplete. |
| `QuestService.startQuest` | `GameServerConnection.TryHandleNpcTargetQuestStartAcceptAsync` | Quest mutation and packet send | Partial | Regression Tested | Partial Parity | Reuses existing start-condition, persistence, quest-action packet, and nearby refresh behavior; challenge task and NPC faction start side effects remain blocked. |
| `DialogAction` | `CmDialogSelect` | Dialog action constants | Partial | Regression Tested | Partial Parity | Adds live-consumed `QUEST_ACCEPT`; this is not a complete DialogAction port. |

## Known Gaps

- Full Java NPC controller, AI `onDialogSelect`, dynamic `QuestEngine.onDialog`, and arbitrary quest handler bodies remain incomplete.
- Refuse dialog branches are not wired in live C# NPC quest-start dialog handling yet.
- Live quest finish still does not support bonus rewards, challenge task completion, arbitrary quest completion callback handler bodies, mentor NPC faction title/flag side effects, or broad nearby quest refresh fanout.
- Challenge task accept side effects and NPC faction start side effects remain blocked on clearer live C# services/state.
- The NPC-target interaction guard uses currently modeled C# facts; summon-owner, siege/zone, live abyss ranking, and dominion calculation restrictions still need fuller runtime parity.

## Runtime Progress Gate For Next UOW

Recommended next UOW: `[Phase 6][UOW-2809] Wire NPC-target quest refuse dialog pages`

- Deferred/live behavior to advance: execute the Java default refuse branches for registered NPC quest dialogs.
- Java source of truth: `AbstractQuestHandler.sendQuestStartDialog` handles `QUEST_REFUSE_1` and `QUEST_REFUSE_2` by sending page `1004`, while `QUEST_REFUSE_SIMPLE` closes the dialog.
- C# runtime artifact to wire/fix: add live NPC-target refuse handling in `GameServerConnection.HandleDialogSelectAsync`, gated by runtime NPC-start registration and world NPC target resolution.
- Client-visible/state/persistence effect expected: live NPC-target refuse packets should send `SmDialogWindow(npcObjectId, 1004, questId)` or close dialog page `0` without mutating quest state.
- Why this is not preview-only/test-only/documentation-only: it wires a deferred client packet path and sends real server packets from live code.

## Focused Validation Recipe For Next UOW

- Specific behavior/contract to prove: registered NPC-target refuse packets send Java-equivalent dialog pages and do not mutate quest state.
- Focused C# command: start with `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~QuestNpcStart"`.
- Focused Java/Maven command: run only if a narrow Java fixture exists or is added for the selected handler/action.
- Broad-validation trigger: live connection dispatch and server packet send.
- Broad .NET decision: do not run unless focused evidence exposes wider risk or shared packet serialization code is edited.

## Safe Runtime Candidates

- Wire NPC-target quest refuse dialog pages for registered NPC-start quests.
- Wire one concrete arbitrary quest completion callback only if its Java body can be ported directly and has clear live state or packet effects.
- Wire challenge task accept/completion only after adding or finding real C# runtime services for Java challenge task mutation/persistence/packet side effects.
- Wire mentor NPC faction quest finish or quest start side effects only if the required live mentor flag state and title packet artifacts exist.

## Summary Metrics

- Total Java artifacts touched/discovered in latest UOW: 3.
- Total artifacts ported or wired in latest UOW: 1 live NPC-target `QUEST_ACCEPT` quest start branch.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification/partial parity: 3.
- Total blocked artifacts: 3 challenge task accept/completion, NPC faction start side effects, and mentor NPC faction side effects.
- Estimated overall migration completion: unchanged conservatively; Phase 6 remains in progress.
