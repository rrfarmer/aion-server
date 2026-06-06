# Phase 6 Session 2807 Completion

## Unit Of Work

`[Phase 6][UOW-2807] Wire NPC-target QUEST_ACCEPT_1 start follow-up page`

## Runtime Progress Gate

- Deferred/live behavior advanced: live NPC-target `CmDialogSelect` with Java `QUEST_ACCEPT_1` can now start a registered NPC quest and send the start follow-up dialog page.
- Java source of truth: `AbstractQuestHandler.sendQuestStartDialog` calls `QuestService.startQuest(env)` for `QUEST_ACCEPT_1`, then sends `SM_DIALOG_WINDOW(npc.getObjectId(), 1003, questId)` when the visible object is an NPC.
- C# runtime artifact wired: `GameServerConnection.TryHandleNpcTargetQuestStartAcceptAsync` now handles `CmDialogSelect.QuestAccept1` in addition to `QuestAcceptSimple`.
- Client-visible/state/persistence effect changed: live NPC-target `QUEST_ACCEPT_1` mutates `player.Quests`, persists through `PlayerEnterWorldService.PersistQuestStartAsync` when present, sends `SmQuestAction`, sends `SmDialogWindow(npcObjectId, 1003, questId)`, and refreshes nearby quest markers.
- Why this is not preview-only/test-only/documentation-only: it mutates live quest state and sends real server packets from the live socket handler.

## Java Parity Notes

- `QUEST_ACCEPT_1` shares the same quest-start validation and mutation path as `QUEST_ACCEPT_SIMPLE`, but Java sends page `1003` instead of closing the dialog.
- The live path remains gated by world NPC target resolution, optional known-NPC validation, and runtime-loaded Java/XML NPC-start registrations.
- Full dynamic quest handler execution remains incomplete; this is a directly wired slice of the default Java quest-start helper.

## C# Artifacts Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmDialogSelect.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionQuestFinishDialogBoundaryTests.cs`

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

## Test Evidence

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleDialogSelectAsync_NpcTargetQuestAccept1StartsQuestAndSendsStartPage` | Regression | Java source review: `AbstractQuestHandler.sendQuestStartDialog`, `QuestService.startQuest` | Live NPC-target `QUEST_ACCEPT_1` starts a registered quest, sends `SmQuestAction.ADD`, and sends page `1003` with quest id | Filtered C# socket boundary test with real world NPC target, player quest mutation, and serialized packet assertions | Does not execute arbitrary quest handler bodies |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.questEngine.handlers.AbstractQuestHandler.sendQuestStartDialog` `QUEST_ACCEPT_1` | `Aion.GameServer.Network.Aion.GameServerConnection.TryHandleNpcTargetQuestStartAcceptAsync` | Quest dialog handler slice | Partial | Regression Tested | Partial Parity | Starts registered NPC-target quests and sends Java follow-up page `1003`; other handler-specific start logic remains incomplete. |
| `com.aionemu.gameserver.services.QuestService.startQuest` | `Aion.GameServer.Network.Aion.GameServerConnection.TryHandleNpcTargetQuestStartAcceptAsync` | Quest mutation and packet send | Partial | Regression Tested | Partial Parity | Reuses existing start-condition, persistence, quest-action packet, and nearby refresh behavior; challenge task and NPC faction start side effects remain blocked. |
| `com.aionemu.gameserver.model.DialogAction` | `Aion.GameServer.Network.Aion.ClientPackets.CmDialogSelect` | Dialog action constants | Partial | Regression Tested | Partial Parity | Adds live-consumed constants for `QUEST_ACCEPT_1` and `QUEST_ACCEPT_SIMPLE`; this is not a complete DialogAction port. |

## Known Gaps

- Full Java NPC controller, AI `onDialogSelect`, dynamic `QuestEngine.onDialog`, and arbitrary quest handler bodies remain incomplete.
- `QUEST_ACCEPT` is not wired in this UOW.
- Challenge task accept side effects and NPC faction start side effects remain blocked on clearer live C# services/state.
- The NPC-target interaction guard uses currently modeled C# facts; summon-owner, siege/zone, live abyss ranking, and dominion calculation restrictions still need fuller runtime parity.
