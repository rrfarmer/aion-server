# Phase 6 Session 2805 Completion

## Unit Of Work

`[Phase 6][UOW-2805] Wire live NPC-target dialog fallback packet`

## Runtime Progress Gate

- Deferred/live behavior advanced: live `CmDialogSelect` packets targeting a world NPC now reach the Java `DialogService.handleQuestDialogueOrSendNextPage` fallback for unhandled quest/dialog-page actions.
- Java source of truth: `CM_DIALOG_SELECT.runImpl` resolves a non-self creature target, `NpcController.onDialogSelect` falls through AI handling, and `DialogService.handleQuestDialogueOrSendNextPage` sends `new SM_DIALOG_WINDOW(npc.getObjectId(), dialogActionId, questId)` when quest handling does not consume the action.
- C# runtime artifact wired: `GameServerConnection.HandleDialogSelectAsync` now calls `TrySendNpcTargetDialogFallbackAsync` after existing live special cases and before silently returning for non-charge dialog actions.
- Client-visible effect changed: an unhandled NPC-target quest/page dialog sends a real `SmDialogWindow(targetObjectId, dialogActionId, questId)` packet to the client.
- Why this is not preview-only/test-only/documentation-only: the live socket handler now sends an actual server packet from the NPC-target fallthrough path.

## Java Parity Notes

- C# still does not execute full NPC AI or dynamic `QuestEngine.onDialog` handlers. This UOW wires only the final Java fallback packet for cases not already consumed by existing C# live special cases.
- The fallback requires a non-self live world NPC target and honors the optional known-NPC hook when present.
- The fallback is limited to Java quest-engine fallback actions (`questId != 0`, `USE_OBJECT`, or `EXCHANGE_COIN`) to avoid broadening unrelated function-dialog behavior while the full NPC dialog engine remains incomplete.

## C# Artifacts Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionQuestFinishDialogBoundaryTests.cs`

## Validation Decision

- Changed surface: live connection dispatch and server packet send.
- Specific behavior/contract: Java NPC-target dialog fallback sends `SM_DIALOG_WINDOW` with NPC object id, dialog/page id, and quest id when no handler consumes the dialog action.
- Focused C# command: `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~QuestDialogNpcTargetBranchPlanServiceTests|FullyQualifiedName~QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests"`
- Focused Java/Maven command: not run; no narrow Java fixture exists for the socket-side `SM_DIALOG_WINDOW` fallback.
- Broad-validation trigger: live connection dispatch and server packet send.
- Broad .NET decision: skipped after focused validation; no shared packet serializer was edited, and the focused boundary test serialized the emitted `SmDialogWindow` payload.

## Tests Run

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~QuestDialogNpcTargetBranchPlanServiceTests|FullyQualifiedName~QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests"
```

Result: Passed, 51 total, 0 failed, 0 skipped.

Java/Maven: not run; no narrow Java fixture exists for the socket-side NPC-target fallback packet.

## Test Evidence

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleDialogSelectAsync_NpcTargetUnhandledQuestDialogSendsDialogWindowFallback` | Regression | Java source review: `CM_DIALOG_SELECT.runImpl`, `NpcController.onDialogSelect`, `DialogService.handleQuestDialogueOrSendNextPage` | Live NPC-target unhandled quest/page action sends `SmDialogWindow` with target object id, dialog action id, and quest id | Filtered C# socket boundary test with a real world NPC target and serialized packet-field assertions | Does not execute arbitrary NPC AI or dynamic quest handlers |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT.runImpl` NPC-target branch | `Aion.GameServer.Network.Aion.GameServerConnection.HandleDialogSelectAsync` | Client packet handler | Partial | Regression Tested | Partial Parity | Supports live NPC-target reportable auto-reward quest finish and now sends fallback dialog windows for unhandled quest/page actions. |
| `com.aionemu.gameserver.controllers.NpcController.onDialogSelect` | `Aion.GameServer.Network.Aion.GameServerConnection.TrySendNpcTargetDialogFallbackAsync` | Runtime dispatch slice | Partial | Regression Tested | Partial Parity | C# wires the final fallback packet after supported live slices, without full AI dispatch. |
| `com.aionemu.gameserver.services.DialogService.handleQuestDialogueOrSendNextPage` | `Aion.GameServer.Network.Aion.GameServerConnection.TrySendNpcTargetDialogFallbackAsync` | Dialog fallback packet path | Partial | Regression Tested | Partial Parity | Sends `SmDialogWindow(targetObjectId, dialogActionId, questId)` for unhandled NPC-target quest fallback actions. |

## Parity Status

Partial parity improved for NPC-target dialog handling. The live socket path now sends the Java fallback dialog window packet instead of silently dropping supported unhandled quest/page actions.

## Known Gaps

- Full Java NPC controller, AI `onDialogSelect`, dynamic `QuestEngine.onDialog`, and arbitrary quest handler bodies remain incomplete.
- The fallback is intentionally narrow and does not claim parity for every function-dialog branch.
- Live quest finish still does not support bonus rewards, challenge task completion, arbitrary quest completion callback handler bodies, mentor NPC faction title/flag side effects, or broad nearby quest refresh fanout.
- The NPC-target interaction guard still uses currently modeled C# facts; summon-owner, siege/zone, live abyss ranking, and dominion calculation restrictions need fuller runtime parity.
