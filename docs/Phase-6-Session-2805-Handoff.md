# Phase 6 Session 2805 Handoff

## Current Phase

Phase 6: Port Game Core

## Latest Completed UOW

`[Phase 6][UOW-2805] Wire live NPC-target dialog fallback packet`

Commit made in this session:

- `[Phase 6][UOW-2805] Wire live NPC-target dialog fallback packet`

## Current State

- `GameServerConnection.HandleDialogSelectAsync` handles supported self-target and NPC-target reportable auto-reward quest finish paths.
- The same live NPC-target handler now sends `SmDialogWindow(targetObjectId, dialogActionId, questId)` for unhandled quest/page fallback actions after existing special cases do not consume the packet.
- `StaticData.QuestCompletionFollowUps` loads Java quest handler source files from the configured quest handler directory and captures default completion follow-up registrations for literal ids, simple int arrays, and no-argument `defaultOnQuestCompletedEvent(env)` calls.
- Challenge task completion remains blocked on a missing live C# completion mutation service equivalent to Java `ChallengeTaskService.onChallengeQuestFinish`.
- Mentor NPC faction side effects remain blocked until the C# runtime has a clear Java-equivalent mentor flag time state and title packet side effects.

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT.runImpl`.
- `com.aionemu.gameserver.controllers.NpcController.onDialogSelect`.
- `com.aionemu.gameserver.services.DialogService.handleQuestDialogueOrSendNextPage`.

## C# Artifacts Touched

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`.
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionQuestFinishDialogBoundaryTests.cs`.
- `docs/Phase-6-Session-2805-Completion.md`.
- `docs/Phase-6-Session-2805-Handoff.md`.

## Validation Decision

- Changed surface: live connection dispatch and server packet send.
- Specific behavior/contract: Java NPC-target dialog fallback sends `SM_DIALOG_WINDOW` with target object id, dialog action/page id, and quest id when no live handler consumes the action.
- Focused C# command: `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~QuestDialogNpcTargetBranchPlanServiceTests|FullyQualifiedName~QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests"`
- Focused Java/Maven command: not run; no narrow Java fixture exists for the socket-side NPC-target fallback packet.
- Broad-validation trigger: live connection dispatch and server packet send.
- Broad .NET decision: skipped after focused validation; no shared packet serializer was edited, and the focused boundary test serialized the emitted packet payload.

## Tests Run

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~QuestDialogNpcTargetBranchPlanServiceTests|FullyQualifiedName~QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests"
```

Result: Passed, 51 total, 0 failed, 0 skipped.

Java/Maven: not run; no narrow Java fixture exists for the socket-side NPC-target fallback packet.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CM_DIALOG_SELECT.runImpl` NPC-target branch | `GameServerConnection.HandleDialogSelectAsync` | Client packet handler | Partial | Regression Tested | Partial Parity | Supports live NPC-target reportable auto-reward quest finish and fallback dialog window sends for unhandled quest/page actions. |
| `NpcController.onDialogSelect` | `GameServerConnection.TrySendNpcTargetDialogFallbackAsync` | Runtime dispatch slice | Partial | Regression Tested | Partial Parity | C# wires the final fallback packet after current live special cases; full NPC AI and dynamic quest handler dispatch remain incomplete. |
| `DialogService.handleQuestDialogueOrSendNextPage` | `GameServerConnection.TrySendNpcTargetDialogFallbackAsync` | Dialog fallback packet path | Partial | Regression Tested | Partial Parity | Sends the Java-equivalent `SmDialogWindow(targetObjectId, dialogActionId, questId)` for supported unhandled NPC-target fallback actions. |

## Known Gaps

- Full Java NPC controller, AI `onDialogSelect`, dynamic `QuestEngine.onDialog`, and arbitrary quest handler bodies remain incomplete.
- The fallback path is intentionally narrow and does not claim parity for every function-dialog branch.
- Live quest finish still does not support bonus rewards, challenge task completion, arbitrary quest completion callback handler bodies, mentor NPC faction title/flag side effects, or broad nearby quest refresh fanout.
- Direct reward persistence is not newly wired in quest finish for AP/DP/GP/cube/warehouse expansions. Existing later player-save behavior may persist mutated runtime state, but these UOWs did not add direct quest-finish persistence calls.
- The NPC-target interaction guard uses currently modeled C# facts; summon-owner, siege/zone, live abyss ranking, and dominion calculation restrictions still need fuller runtime parity.

## Runtime Progress Gate For Next UOW

Recommended next UOW: `[Phase 6][UOW-2806] Wire live NPC-target quest start mutation for a single Java-equivalent accept action`

- Deferred/live behavior to advance: execute one concrete NPC-target quest accept path from live `CmDialogSelect`, mutating player quest state from absent/locked to started instead of only showing pages or completing reward quests.
- Java source of truth: `CM_DIALOG_SELECT.runImpl` NPC branch through `NpcController.onDialogSelect`, `DialogService.handleQuestDialogueOrSendNextPage`, quest handler `onDialog`, and Java quest start service path used by the selected handler/action.
- C# runtime artifact to wire/fix: `GameServerConnection.HandleDialogSelectAsync` plus the existing quest-start mutation and `SmQuestAction` packet path, scoped to one directly verifiable Java accept action.
- Client-visible/state/persistence effect expected: a live NPC-target dialog accept packet should add or update the player's quest state, persist through the existing quest table shape when available, and send the real quest packet.
- Why this is not preview-only/test-only/documentation-only: it would mutate live quest state and send live server packets from the socket handler.

## Focused Validation Recipe For Next UOW

- Specific behavior/contract to prove: a selected Java quest accept action from a live NPC target starts the quest and sends the expected `SmQuestAction` packet.
- Focused C# command: start with `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~NearbyQuestStartConditionServiceTests"` and narrow or expand based on the actual service touched.
- Focused Java/Maven command: run only if a narrow Java fixture exists or is added for the selected quest handler/action.
- Broad-validation trigger: live connection dispatch, quest-state mutation, and packet send.
- Broad .NET decision: do not run unless focused evidence exposes wider risk or shared quest-state/packet serialization code is edited.

## Safe Runtime Candidates

- Wire one concrete NPC-target quest accept/start mutation with a directly ported Java handler/action.
- Wire additional Java `DialogService.handleQuestDialogueOrSendNextPage` fallback actions only if they send real packets from live code and have bounded guard behavior.
- Wire one concrete arbitrary quest completion callback only if its Java body can be ported directly and has clear live state or packet effects.
- Wire mentor NPC faction quest finish side effects only if the required live mentor flag state and title packet artifacts exist.
- Wire challenge task completion only after adding or finding a real C# runtime service for Java `onChallengeQuestFinish` mutation/persistence/packet side effects.

## Summary Metrics

- Total Java artifacts touched/discovered in latest UOW: 3.
- Total artifacts ported or wired in latest UOW: 1 live NPC-target fallback packet path.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification/partial parity: 3.
- Total blocked artifacts: 2 challenge task completion and mentor NPC faction side effects.
- Estimated overall migration completion: unchanged conservatively; Phase 6 remains in progress.
