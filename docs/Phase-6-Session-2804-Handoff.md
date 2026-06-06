# Phase 6 Session 2804 Handoff

## Current Phase

Phase 6: Port Game Core

## Latest Completed UOW

`[Phase 6][UOW-2804] Wire live NPC-target quest finish auto rewards`

Commit made in this session:

- `[Phase 6][UOW-2804] Wire live NPC-target quest finish auto rewards`

## Current State

- `GameServerConnection.HandleDialogSelectAsync` handles the Java self-target/reportable quest auto-reward branch for supported XP, kinah, fixed item rewards, regular selectable item rewards, class-selectable item rewards, extended selectable item rewards, work-item removal, title rewards, AP rewards, DP rewards, GP rewards, cube expansion rewards, warehouse expansion rewards, default completion follow-up starts, handler prequest mission locks, recursive XML follow-up mission locks, and non-mentor NPC faction completion.
- The same supported reportable auto-reward finish pipeline can now run for non-self live NPC targets after world NPC resolution, optional known-NPC validation, and modeled interaction guards.
- `StaticData.QuestCompletionFollowUps` loads Java quest handler source files from the configured quest handler directory and captures default completion follow-up registrations for literal ids, simple int arrays, and no-argument `defaultOnQuestCompletedEvent(env)` calls.
- Challenge task completion remains blocked on a missing live C# completion mutation service equivalent to Java `ChallengeTaskService.onChallengeQuestFinish`.
- Mentor NPC faction side effects remain blocked until the C# runtime has a clear Java-equivalent mentor flag time state and title packet side effects.

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT.runImpl`.
- `com.aionemu.gameserver.controllers.NpcController.onDialogSelect`.
- `com.aionemu.gameserver.services.DialogService.handleQuestDialogueOrSendNextPage`.
- `com.aionemu.gameserver.services.DialogService.isInteractionAllowed`.
- `com.aionemu.gameserver.services.QuestService.finishQuest`.

## C# Artifacts Touched

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`.
- `dotnetConversion/src/Aion.GameServer/Services/QuestDialogAutoRewardGuardPlanService.cs`.
- `dotnetConversion/src/Aion.GameServer/Services/QuestFinishSocketGuardedInputAssemblyPlanService.cs`.
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionQuestFinishDialogBoundaryTests.cs`.
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestDialogAutoRewardGuardPlanServiceTests.cs`.
- `docs/Phase-6-Session-2804-Completion.md`.
- `docs/Phase-6-Session-2804-Handoff.md`.

## Validation Decision

- Changed surface: live connection dispatch and quest-finish side effect.
- Specific behavior/contract: Java NPC-target quest dialog handling can complete a reportable auto-reward quest after target resolution and interaction guards.
- Focused C# command: `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~QuestDialogAutoRewardGuardPlanServiceTests|FullyQualifiedName~QuestDialogNpcTargetBranchPlanServiceTests|FullyQualifiedName~QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests"`
- Focused Java/Maven command: not run; no narrow Java fixture exists for `CM_DIALOG_SELECT` NPC-target quest reporting.
- Broad-validation trigger: live connection dispatch and quest-finish side effect.
- Broad .NET decision: skipped after focused validation; the filtered command compiled the affected project and exercised the edited live socket boundary plus adjacent guard and NPC-target branch models.
- Why this scope is sufficient: the new boundary test sends `CmDialogSelect` with a real NPC object id, verifies the live handler applies XP, sends the success system message, sends the quest update packet, and mutates the quest to `COMPLETE`.

## Tests Run

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~QuestDialogAutoRewardGuardPlanServiceTests|FullyQualifiedName~QuestDialogNpcTargetBranchPlanServiceTests|FullyQualifiedName~QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests"
```

Result: Passed, 69 total, 0 failed, 0 skipped.

Java/Maven: not run; no narrow Java fixture exists for the socket-side NPC-target quest reporting path.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CM_DIALOG_SELECT.runImpl` NPC-target branch | `GameServerConnection.HandleDialogSelectAsync` | Client packet handler | Partial | Regression Tested | Partial Parity | Supports live NPC-target reportable auto-reward quest finish after world NPC resolution and interaction guards; full controller/AI/QuestEngine dispatch remains incomplete. |
| `NpcController.onDialogSelect` | `GameServerConnection.TryHandleNpcTargetQuestFinishAutoRewardAsync` | Runtime dispatch slice | Partial | Regression Tested | Partial Parity | C# directly wires the supported quest-finish slice instead of invoking full NPC AI and dynamic quest handler dispatch. |
| `DialogService.isInteractionAllowed` | `NpcDialogInteractionAllowedPlanService` consumed by `GameServerConnection` | Runtime guard | Partial | Regression Tested | Partial Parity | Uses available subdialog, inventory, skill, abyss rank, legion presence, and level facts; summon owner, siege/zone, live ranking, and dominion calculation details remain incomplete. |
| `QuestService.finishQuest` | `GameServerConnection.TryHandleQuestFinishAutoRewardAsync` | Runtime quest mutation and packet send | Partial | Regression Tested | Partial Parity | Reused for NPC-target auto-reward completion with target NPC template passed into reward projection lookup. |

## Known Gaps

- Full Java NPC controller, AI `onDialogSelect`, dynamic `QuestEngine.onDialog`, and arbitrary quest handler bodies remain incomplete.
- Live quest finish still does not support bonus rewards, challenge task completion, arbitrary quest completion callback handler bodies, mentor NPC faction title/flag side effects, or broad nearby quest refresh fanout.
- The completion follow-up extractor intentionally skips arbitrary expressions and non-default callback bodies.
- Direct reward persistence is not newly wired in quest finish for AP/DP/GP/cube/warehouse expansions. Existing later player-save behavior may persist mutated runtime state, but these UOWs did not add direct quest-finish persistence calls.
- The NPC-target interaction guard uses currently modeled C# facts; summon-owner, siege/zone, live abyss ranking, and dominion calculation restrictions still need fuller runtime parity.

## Runtime Progress Gate For Next UOW

Recommended next UOW: `[Phase 6][UOW-2805] Wire live NPC-target dialog fallback packet`

- Deferred/live behavior advanced: execute the Java `DialogService.handleQuestDialogueOrSendNextPage` fallback that sends a dialog window when NPC-target quest/AI handling does not consume the dialog action.
- Java source of truth: `CM_DIALOG_SELECT.runImpl` target branch, `NpcController.onDialogSelect`, and `DialogService.handleQuestDialogueOrSendNextPage` final `PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(npc.getObjectId(), dialogActionId, questId))`.
- C# runtime artifact to wire/fix: `GameServerConnection.HandleDialogSelectAsync` NPC-target fallthrough after current live special cases, using live world NPC resolution and `SmDialogWindow`.
- Client-visible/state/persistence effect expected: a real NPC-target dialog packet with an unhandled quest/page action should send `SmDialogWindow(targetObjectId, dialogActionId, questId)` to the client instead of silently returning.
- Why this is not preview-only/test-only/documentation-only: it wires a deferred client packet path and sends a real server packet from live code.

## Focused Validation Recipe For Next UOW

- Specific behavior/contract to prove: Java NPC-target dialog fallback sends `SM_DIALOG_WINDOW` with target object id, dialog action/page id, and quest id when no live handler consumes the action.
- Focused C# command: start with `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~QuestDialogNpcTargetBranchPlanServiceTests|FullyQualifiedName~QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests|FullyQualifiedName~GamePacketTests"`, then remove `GamePacketTests` if no packet serialization assertion is added.
- Focused Java/Maven command: not expected unless a narrow Java fixture is added for `SM_DIALOG_WINDOW` fallback.
- Broad-validation trigger: live connection dispatch and server packet send.
- Broad .NET decision: do not run unless focused evidence exposes wider risk or shared packet serialization is edited.

## Safe Runtime Candidates

- Wire NPC-target dialog fallback packet for unhandled quest/page actions.
- Wire one concrete arbitrary quest completion callback only if its Java body can be ported directly and has clear live state or packet effects.
- Wire mentor NPC faction quest finish side effects only if the required live mentor flag state and title packet artifacts exist.
- Wire challenge task completion only after adding or finding a real C# runtime service for Java `onChallengeQuestFinish` mutation/persistence/packet side effects.
- Wire direct quest-finish persistence for non-item rewards only after checking Java save timing and the existing C# player-save/repository shape.

## Summary Metrics

- Total Java artifacts touched/discovered in latest UOW: 5.
- Total artifacts ported or wired in latest UOW: 1 live NPC-target quest finish auto-reward path.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification/partial parity: 4.
- Total blocked artifacts: 2 challenge task completion and mentor NPC faction side effects.
- Estimated overall migration completion: unchanged conservatively; Phase 6 remains in progress.
