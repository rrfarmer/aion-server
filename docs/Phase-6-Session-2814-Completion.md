# Phase 6 Session 2814 Completion

## Unit Of Work

`[Phase 6][UOW-2814] Wire NPC-target selected reward completion`

## Runtime Progress Gate

- Deferred/live behavior advanced: live NPC-target `SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD` dialog actions can now finish `REWARD` quests through the existing reward application path.
- Java source of truth: `AbstractQuestHandler.sendQuestEndDialog` checks selected reward actions, calls `QuestService.finishQuest(env)`, then sends the next quest/reward selection or closes the dialog.
- C# runtime artifact wired: `GameServerConnection.TryHandleNpcTargetQuestFinishSelectedRewardAsync`, `QuestFinishSocketGuardedInputAssemblyPlanService`, `QuestFinishSocketInputAssemblyPlanService`, `QuestDialogAutoRewardGuardPlanService`, and selected reward constants in `CmDialogSelect`.
- Client-visible/state/persistence effect changed: selected reward actions on a known NPC now apply selected item/non-item rewards, mutate the quest to complete, persist through the existing quest-save hook when available, send `SmQuestAction.Update`, and send a scoped close-dialog packet.
- Why this is not preview-only/test-only/documentation-only: it wires a deferred client packet path, mutates live inventory and quest state, uses existing persistence hooks, and sends real server packets from live connection code.

## Java Parity Notes

- Java selected reward IDs are `DialogAction.SELECTED_QUEST_REWARD1` through `SELECTED_QUEST_NOREWARD` (`8..23`).
- Java only enters this finish branch when the player already has the quest in `QuestStatus.REWARD`.
- This C# slice deliberately keeps existing self-target auto-reward behavior unchanged; selected reward actions are allowed only for the live NPC-target selected reward handler.
- Partial parity: after finishing, Java scans `QuestNpc.onTalkEvent` and `QuestNpc.onQuestStart` to decide whether to open another reward page, a follow-up start page, a selection page, or close. C# currently sends the close page for the scoped selected reward completion path and does not yet implement that full post-finish NPC quest scan.

## C# Artifacts Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmDialogSelect.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/QuestDialogAutoRewardGuardPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/QuestFinishSocketGuardedInputAssemblyPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/QuestFinishSocketInputAssemblyPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionQuestFinishDialogBoundaryTests.cs`

## Validation Decision

- Changed surface: live connection dispatch, reward side effects, quest state mutation, persistence hook, and server packet send.
- Specific behavior/contract: NPC-target selected reward action `8` for a `REWARD` quest applies the chosen selectable item, completes the quest, emits `SmQuestAction.Update`, and sends close dialog page `0`.
- Focused C# commands:
  - `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests"`
  - `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~QuestFinishSocketInputAssemblyPlanServiceTests|FullyQualifiedName~QuestFinishSocketGuardedInputAssemblyPlanServiceTests|FullyQualifiedName~QuestDialogAutoRewardGuardPlanServiceTests"`
  - `git diff --check`
- Focused Java/Maven command: not run; no narrow Java fixture exists for this socket-side branch. Java source was reviewed directly.
- Broad-validation trigger: live connection dispatch, reward side effects, quest state mutation, persistence hook, and server packet send.
- Broad .NET decision: skipped after focused validation; no shared packet serializer, database schema, packet primitive, scheduler, or common world-state model was changed.
- Why this scope is sufficient: the boundary test checks live packet order and serialized close dialog contents while the adjacent service tests confirm existing auto-reward guard behavior remains unchanged.

## Tests Run

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests"
```

Result: Passed, 32 total, 0 failed, 0 skipped.

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~QuestFinishSocketInputAssemblyPlanServiceTests|FullyQualifiedName~QuestFinishSocketGuardedInputAssemblyPlanServiceTests|FullyQualifiedName~QuestDialogAutoRewardGuardPlanServiceTests"
```

Result: Passed, 31 total, 0 failed, 0 skipped.

Java/Maven: not run; no narrow Java fixture exists for this socket-side branch.

```powershell
git diff --check
```

Result: Passed; only normal workspace CRLF conversion warnings were reported.

## Test Evidence

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleDialogSelectAsync_NpcTargetSelectedRewardAddsSelectedItemCompletesQuestAndClosesDialog` | Regression | Java source review: `AbstractQuestHandler.sendQuestEndDialog` selected reward branch | NPC-target selected reward action applies selected item reward, completes quest state, sends quest update, and sends close page `0` | Filtered C# socket boundary test with live inventory/quest mutation and serialized packet assertions | Does not verify Java post-finish scan for other active/new quests |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.questEngine.handlers.AbstractQuestHandler.sendQuestEndDialog` selected reward branch | `Aion.GameServer.Network.Aion.GameServerConnection.TryHandleNpcTargetQuestFinishSelectedRewardAsync` | Quest dialog handler slice | Partial | Regression Tested | Partial Parity | Wires selected reward completion and scoped close for NPC-target reward quests; full Java post-finish active/new quest selection scan is still missing. |
| `com.aionemu.gameserver.model.DialogAction` selected reward constants | `Aion.GameServer.Network.Aion.ClientPackets.CmDialogSelect` | Dialog action constants | Partial | Regression Tested | Partial Parity | Adds live-consumed selected reward constants `8..23`; this is not a complete DialogAction port. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT` NPC quest finish route | `Aion.GameServer.Services.QuestFinishSocketGuardedInputAssemblyPlanService` and `QuestFinishSocketInputAssemblyPlanService` | Socket input assembly | Partial | Regression Tested | Partial Parity | Existing auto-reward callers still reject selected reward actions by default; NPC-target selected completion explicitly enables them. |

## Known Gaps

- Full Java NPC controller, AI `onDialogSelect`, dynamic `QuestEngine.onDialog`, and arbitrary quest handler bodies remain incomplete.
- Full `QuestNpc.onTalkEvent` registration is not ported; selected completion cannot yet open another reward page after finishing one quest on the same NPC.
- Post-finish Java follow-up selection/start dialog logic is incomplete; this UOW sends a scoped close after selected completion.
- Live quest finish still does not support bonus rewards, challenge task completion, arbitrary quest completion callback handler bodies, mentor NPC faction title/flag side effects, or broad nearby quest refresh fanout.
