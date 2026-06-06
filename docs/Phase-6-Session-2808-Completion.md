# Phase 6 Session 2808 Completion

## Unit Of Work

`[Phase 6][UOW-2808] Wire NPC-target QUEST_ACCEPT start follow-up page`

## Runtime Progress Gate

- Deferred/live behavior advanced: live NPC-target `CmDialogSelect` with Java `QUEST_ACCEPT` can now start a registered NPC quest and send the start follow-up dialog page.
- Java source of truth: `AbstractQuestHandler.sendQuestStartDialog` calls `QuestService.startQuest(env)` for `QUEST_ACCEPT`, then sends `SM_DIALOG_WINDOW(npc.getObjectId(), 1003, questId)` when the visible object is an NPC.
- C# runtime artifact wired: `CmDialogSelect.QuestAccept` and `GameServerConnection.TryHandleNpcTargetQuestStartAcceptAsync`.
- Client-visible/state/persistence effect changed: live NPC-target `QUEST_ACCEPT` mutates `player.Quests`, persists through `PlayerEnterWorldService.PersistQuestStartAsync` when present, sends `SmQuestAction`, sends `SmDialogWindow(npcObjectId, 1003, questId)`, and refreshes nearby quest markers.
- Why this is not preview-only/test-only/documentation-only: it mutates live quest state and sends real server packets from the live socket handler.

## Java Parity Notes

- `QUEST_ACCEPT`, `QUEST_ACCEPT_1`, and `QUEST_ACCEPT_SIMPLE` now share the same registered NPC quest-start path.
- Only `QUEST_ACCEPT_SIMPLE` closes the dialog with page `0`; `QUEST_ACCEPT` and `QUEST_ACCEPT_1` send the Java follow-up page `1003`.
- Full dynamic quest handler execution remains incomplete; this is a directly wired slice of the default Java quest-start helper.

## C# Artifacts Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmDialogSelect.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionQuestFinishDialogBoundaryTests.cs`

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

## Test Evidence

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleDialogSelectAsync_NpcTargetQuestAcceptStartsQuestAndSendsStartPage` | Regression | Java source review: `AbstractQuestHandler.sendQuestStartDialog`, `QuestService.startQuest` | Live NPC-target `QUEST_ACCEPT` starts a registered quest, sends `SmQuestAction.ADD`, and sends page `1003` with quest id | Filtered C# socket boundary test with real world NPC target, player quest mutation, and serialized packet assertions | Does not execute arbitrary quest handler bodies |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.questEngine.handlers.AbstractQuestHandler.sendQuestStartDialog` `QUEST_ACCEPT` | `Aion.GameServer.Network.Aion.GameServerConnection.TryHandleNpcTargetQuestStartAcceptAsync` | Quest dialog handler slice | Partial | Regression Tested | Partial Parity | Starts registered NPC-target quests and sends Java follow-up page `1003`; full dynamic handler execution remains incomplete. |
| `com.aionemu.gameserver.services.QuestService.startQuest` | `Aion.GameServer.Network.Aion.GameServerConnection.TryHandleNpcTargetQuestStartAcceptAsync` | Quest mutation and packet send | Partial | Regression Tested | Partial Parity | Reuses existing start-condition, persistence, quest-action packet, and nearby refresh behavior; challenge task and NPC faction start side effects remain blocked. |
| `com.aionemu.gameserver.model.DialogAction` | `Aion.GameServer.Network.Aion.ClientPackets.CmDialogSelect` | Dialog action constants | Partial | Regression Tested | Partial Parity | Adds live-consumed `QUEST_ACCEPT`; this is not a complete DialogAction port. |

## Known Gaps

- Full Java NPC controller, AI `onDialogSelect`, dynamic `QuestEngine.onDialog`, and arbitrary quest handler bodies remain incomplete.
- Refuse dialog branches are not wired in live C# NPC quest-start dialog handling yet.
- Challenge task accept side effects and NPC faction start side effects remain blocked on clearer live C# services/state.
- The NPC-target interaction guard uses currently modeled C# facts; summon-owner, siege/zone, live abyss ranking, and dominion calculation restrictions still need fuller runtime parity.
