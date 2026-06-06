# Phase 6 Session 2811 Completion

## Unit Of Work

`[Phase 6][UOW-2811] Wire NPC-target FINISH_DIALOG quest selection page`

## Runtime Progress Gate

- Deferred/live behavior advanced: live NPC-target `FINISH_DIALOG` for registered quest-start dialogs now sends the Java quest-selection page.
- Java source of truth: `AbstractQuestHandler.sendQuestStartDialog` handles `FINISH_DIALOG` with `sendQuestSelectionDialog(env)`, and `sendQuestSelectionDialog` sends dialog page `10` with quest id `0`.
- C# runtime artifact wired: `CmDialogSelect.FinishDialog` and `GameServerConnection.TryHandleNpcTargetQuestStartFinishDialogAsync`.
- Client-visible effect changed: live NPC-target `FINISH_DIALOG` sends `SmDialogWindow(npcObjectId, 10, 0)` without mutating quest state.
- Why this is not preview-only/test-only/documentation-only: it wires a deferred client packet path and sends a real server packet from live code.

## Java Parity Notes

- The finish-dialog path is gated by world NPC resolution, optional known-NPC validation, and runtime-loaded Java/XML NPC-start registration.
- This UOW does not mutate quest state, matching the Java helper branch.
- The generic `AbstractQuestHandler.onDialogEvent` `FINISH_DIALOG` branch returns `true` without sending a packet; this UOW intentionally targets the `sendQuestStartDialog` helper path documented in the previous handoff.

## C# Artifacts Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmDialogSelect.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionQuestFinishDialogBoundaryTests.cs`

## Validation Decision

- Changed surface: live connection dispatch and server packet send.
- Specific behavior/contract: registered NPC-target `FINISH_DIALOG` sends Java-equivalent page `10` with quest id `0` and does not mutate quest state.
- Focused C# command: `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~QuestNpcStart"`
- Focused Java/Maven command: not run; no narrow Java fixture exists for socket-side NPC-target `FINISH_DIALOG`.
- Broad-validation trigger: live connection dispatch and server packet send.
- Broad .NET decision: skipped after focused validation; no shared packet serializer was edited, and the boundary test serializes the emitted packet fields.
- Why this scope is sufficient: the edited branch is a single action-id dispatch slice, gated by existing live NPC-start data and proven by the same boundary fixture used for the adjacent Java start-dialog packet paths.

## Tests Run

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~QuestNpcStart"
```

Result: Passed, 48 total, 0 failed, 0 skipped.

Java/Maven: not run; no narrow Java fixture exists for this socket-side branch.

## Test Evidence

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleDialogSelectAsync_NpcTargetFinishDialogSendsQuestSelectionPageWithoutStartingQuest` | Regression | Java source review: `AbstractQuestHandler.sendQuestStartDialog` and `sendQuestSelectionDialog` | `FINISH_DIALOG` sends page `10` with quest id `0` and does not mutate quest state | Filtered C# socket boundary test with serialized packet assertions | Does not cover broader dynamic handler dispatch or reward-selection dialog flow |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.questEngine.handlers.AbstractQuestHandler.sendQuestStartDialog` `FINISH_DIALOG` | `Aion.GameServer.Network.Aion.GameServerConnection.TryHandleNpcTargetQuestStartFinishDialogAsync` | Quest dialog handler slice | Partial | Regression Tested | Partial Parity | Sends Java-equivalent quest-selection page for registered NPC-start quests; broader handler dispatch remains incomplete. |
| `com.aionemu.gameserver.model.DialogAction` | `Aion.GameServer.Network.Aion.ClientPackets.CmDialogSelect` | Dialog action constants | Partial | Regression Tested | Partial Parity | Adds live-consumed `FINISH_DIALOG`; this is not a complete DialogAction port. |

## Known Gaps

- Full Java NPC controller, AI `onDialogSelect`, dynamic `QuestEngine.onDialog`, and arbitrary quest handler bodies remain incomplete.
- The direct `sendQuestStartDialog` packet/state branches now covered in this live path do not equal full quest dialog parity.
- Reward-selection dialog pages for `QuestStatus.REWARD` are still incomplete outside the already wired auto-reward finish path.
- Broader `AbstractQuestHandler.onDialogEvent` page-only/refuse branches are not fully wired.
- Challenge task accept side effects and NPC faction start side effects remain blocked on clearer live C# services/state.
