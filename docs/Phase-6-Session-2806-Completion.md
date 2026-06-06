# Phase 6 Session 2806 Completion

## Unit Of Work

`[Phase 6][UOW-2806] Wire live NPC-target quest accept simple starts`

## Runtime Progress Gate

- Deferred/live behavior advanced: live `CmDialogSelect` packets with NPC target and Java `QUEST_ACCEPT_SIMPLE` can now start a Java/XML-registered NPC-start quest instead of falling through to a dialog page only.
- Java source of truth: `CM_DIALOG_SELECT.runImpl` NPC branch through `NpcController.onDialogSelect`, quest handler `sendQuestStartDialog`, and `QuestService.startQuest`.
- C# runtime artifact wired: `StaticData.QuestNpcStarts` now exposes Java/XML NPC-start registrations, and `GameServerConnection.HandleDialogSelectAsync` consumes that table in `TryHandleNpcTargetQuestAcceptSimpleAsync`.
- Client-visible/state/persistence effect changed: accepting a registered NPC-start quest mutates `player.Quests`, persists via `PlayerEnterWorldService.PersistQuestStartAsync` when available, sends `SmQuestAction.ADD` or `UPDATE`, sends the Java close-dialog `SmDialogWindow(npcObjectId, 0, 0)`, and refreshes nearby quest markers.
- Why this is not preview-only/test-only/documentation-only: loaded Java/XML quest-start data is used by live socket code to mutate player quest state and send real server packets.

## Java Parity Notes

- This UOW intentionally ports only `QUEST_ACCEPT_SIMPLE` from `AbstractQuestHandler.sendQuestStartDialog`. Other accept variants still require their follow-up dialog pages, such as page `1003`, before they can be claimed.
- The live path requires a world NPC target, optional known-NPC validation, and a Java/XML NPC-start registration for the NPC template id and quest id.
- C# reuses `NearbyQuestStartConditionService` and the existing quest persistence shape. Unsupported Java side effects remain blocked or deferred.

## C# Artifacts Changed

- `dotnetConversion/src/Aion.GameServer/Dataholders/StaticData.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionQuestFinishDialogBoundaryTests.cs`

## Validation Decision

- Changed surface: static data runtime loading, live connection dispatch, quest-state mutation, optional persistence, and server packet send.
- Specific behavior/contract: Java `QUEST_ACCEPT_SIMPLE` starts the quest and closes the NPC dialog when the target NPC is registered as a quest starter.
- Focused C# command: `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~NearbyQuestStartConditionServiceTests|FullyQualifiedName~QuestNpcStart"`
- Focused Java/Maven command: not run; no narrow Java fixture exists for socket-side NPC-target `QUEST_ACCEPT_SIMPLE`.
- Broad-validation trigger: live connection dispatch, quest-state mutation, static-data load, and packet send.
- Broad .NET decision: skipped after focused validation; the filter compiled the edited project, exercised the live boundary, and covered adjacent NPC-start extractor/table tests.

## Tests Run

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~NearbyQuestStartConditionServiceTests|FullyQualifiedName~QuestNpcStart"
```

Result: Passed, 53 total, 0 failed, 0 skipped.

Java/Maven: not run; no narrow Java fixture exists for the socket-side NPC-target accept-simple path.

## Test Evidence

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleDialogSelectAsync_NpcTargetQuestAcceptSimpleStartsQuestAndClosesDialog` | Regression | Java source review: `AbstractQuestHandler.sendQuestStartDialog`, `QuestService.startQuest` | Live NPC-target `QUEST_ACCEPT_SIMPLE` loads the NPC-start registration, starts the quest, sends `SmQuestAction.ADD`, and sends close-dialog `SmDialogWindow` | Filtered C# socket boundary test with real world NPC target, player quest mutation, and serialized packet assertions | Does not execute dynamic Java handler bodies or non-simple accept variants |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `QuestNpc.addOnQuestStart` registrations | `StaticData.QuestNpcStarts` | Runtime static data | Partial | Regression Tested | Partial Parity | Java/XML NPC-start registrations are now loaded into runtime static data and consumed by live code. |
| `AbstractQuestHandler.sendQuestStartDialog` `QUEST_ACCEPT_SIMPLE` | `GameServerConnection.TryHandleNpcTargetQuestAcceptSimpleAsync` | Quest dialog handler slice | Partial | Regression Tested | Partial Parity | Starts registered NPC-target quests and closes the dialog for the simple accept action. |
| `QuestService.startQuest` | `GameServerConnection.TryHandleNpcTargetQuestAcceptSimpleAsync` | Quest mutation and packet send | Partial | Regression Tested | Partial Parity | Mutates quest state, persists when the service exists, sends quest action, and refreshes nearby quests; challenge task and NPC faction side effects are still not wired. |

## Parity Status

Partial parity improved for NPC-target quest starts. The live socket path now supports one directly ported Java accept action using runtime-loaded NPC-start registrations.

## Known Gaps

- Full Java NPC controller, AI `onDialogSelect`, dynamic `QuestEngine.onDialog`, and arbitrary quest handler bodies remain incomplete.
- `QUEST_ACCEPT` and `QUEST_ACCEPT_1` are not wired in this UOW because their Java path can send follow-up start pages that need separate packet coverage.
- Challenge task accept side effects, NPC faction start side effects, and mentor-specific state are still blocked on clearer live C# services/state.
- The NPC-target interaction guard uses currently modeled C# facts; summon-owner, siege/zone, live abyss ranking, and dominion calculation restrictions still need fuller runtime parity.
