# Phase 6 Session 2810 Completion

## Unit Of Work

`[Phase 6][UOW-2810] Wire NPC-target ASK_QUEST_ACCEPT dialog page`

## Runtime Progress Gate

- Deferred/live behavior advanced: live NPC-target `ASK_QUEST_ACCEPT` for registered quest-start dialogs now sends the Java ask-accept page.
- Java source of truth: `AbstractQuestHandler.sendQuestStartDialog` handles `ASK_QUEST_ACCEPT` with `sendQuestDialog(env, 4)`.
- C# runtime artifact wired: `CmDialogSelect.AskQuestAccept` and `GameServerConnection.TryHandleNpcTargetQuestStartAskAcceptAsync`.
- Client-visible effect changed: live NPC-target `ASK_QUEST_ACCEPT` sends `SmDialogWindow(npcObjectId, 4, questId)` without mutating quest state.
- Why this is not preview-only/test-only/documentation-only: it wires a deferred client packet path and sends a real server packet from live code.

## Java Parity Notes

- The ask-accept path is gated by world NPC resolution, optional known-NPC validation, and runtime-loaded Java/XML NPC-start registration.
- This UOW does not mutate quest state, matching the Java ask-accept helper branch.
- Broader `AbstractQuestHandler.onDialogEvent` behavior remains incomplete.

## C# Artifacts Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmDialogSelect.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionQuestFinishDialogBoundaryTests.cs`

## Validation Decision

- Changed surface: live connection dispatch and server packet send.
- Specific behavior/contract: registered NPC-target `ASK_QUEST_ACCEPT` sends Java-equivalent page `4` and does not mutate quest state.
- Focused C# command: `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~QuestNpcStart"`
- Focused Java/Maven command: not run; no narrow Java fixture exists for socket-side NPC-target `ASK_QUEST_ACCEPT`.
- Broad-validation trigger: live connection dispatch and server packet send.
- Broad .NET decision: skipped after focused validation; no shared packet serializer was edited, and the boundary test serializes the emitted packet fields.

## Tests Run

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~QuestNpcStart"
```

Result: Passed, 47 total, 0 failed, 0 skipped.

Java/Maven: not run; no narrow Java fixture exists for this socket-side branch.

## Test Evidence

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleDialogSelectAsync_NpcTargetAskQuestAcceptSendsAskPageWithoutStartingQuest` | Regression | Java source review: `AbstractQuestHandler.sendQuestStartDialog` | `ASK_QUEST_ACCEPT` sends page `4` and does not mutate quest state | Filtered C# socket boundary test with serialized packet assertions | Does not cover broader dynamic handler dispatch |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.questEngine.handlers.AbstractQuestHandler.sendQuestStartDialog` `ASK_QUEST_ACCEPT` | `Aion.GameServer.Network.Aion.GameServerConnection.TryHandleNpcTargetQuestStartAskAcceptAsync` | Quest dialog handler slice | Partial | Regression Tested | Partial Parity | Sends Java-equivalent ask-accept page for registered NPC-start quests; broader handler dispatch remains incomplete. |
| `com.aionemu.gameserver.model.DialogAction` | `Aion.GameServer.Network.Aion.ClientPackets.CmDialogSelect` | Dialog action constants | Partial | Regression Tested | Partial Parity | Adds live-consumed `ASK_QUEST_ACCEPT`; this is not a complete DialogAction port. |

## Known Gaps

- Full Java NPC controller, AI `onDialogSelect`, dynamic `QuestEngine.onDialog`, and arbitrary quest handler bodies remain incomplete.
- `FINISH_DIALOG` start-dialog helper branch is not wired in this live C# path yet.
- Broader `AbstractQuestHandler.onDialogEvent` pages are not fully wired.
- Challenge task accept side effects and NPC faction start side effects remain blocked on clearer live C# services/state.
