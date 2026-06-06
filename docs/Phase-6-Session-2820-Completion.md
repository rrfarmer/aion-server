# Phase 6 Session 2820 Completion

## Unit Of Work

`[Phase 6][UOW-2820] Persist reward group correction from selected reward post-finish follow-up reward page`

## Runtime Progress Gate

- Deferred/live behavior advanced: after selected reward completion, if the same NPC has another `REWARD` quest whose reward group is corrected before opening its reward page, the corrected follow-up quest state is now persisted.
- Java source of truth: `AbstractQuestHandler.sendQuestEndDialog` completes the selected reward quest, scans `QuestNpc.getOnTalkEvent()`, and re-enters `QuestEngine.onDialog(new QuestEnv(npc, player, questId, DialogAction.USE_OBJECT))`; that reward-page path calls `QuestService.validateAndFixRewardGroup(qs, questId)`.
- C# runtime artifact wired: `GameServerConnection.CreateNpcSelectedRewardPostFinishDialog` now returns the follow-up dialog plus any corrected follow-up `PlayerQuestState`, and `TryHandleQuestFinishAutoRewardAsync` persists it through `PlayerEnterWorldService.PersistQuestStartAsync(..., false)` before sending the follow-up dialog.
- Client-visible/state/persistence effect changed: the same follow-up reward page is still sent, but stale or invalid reward-group state on that next quest becomes durable in the existing `player_quests` update shape.
- Why this is not preview-only/test-only/documentation-only: it changes live quest-state persistence in the selected reward completion packet path.

## Java Parity Notes

- Java reuses the same reward-page dialog path after selected reward completion, so the follow-up reward quest receives the same `validateAndFixRewardGroup` mutation as a direct NPC reward-page request.
- C# already sent the next reward page and corrected the follow-up quest in memory; this UOW wires that corrected state to the existing quest update persistence path.
- No narrow Java/Maven fixture exists for this chained socket-side `sendQuestEndDialog` path; Java source was reviewed directly.

## C# Artifacts Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionQuestFinishDialogBoundaryTests.cs`

## Validation Decision

- Changed surface: live selected reward completion dispatch and quest persistence side effect.
- Specific behavior/contract: selected reward completion opens the next same-NPC reward page, corrects that next quest's invalid reward group, and calls quest update persistence for the corrected next quest when `PlayerEnterWorldService` is available.
- Focused C# command: `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests"`
- Focused Java/Maven command: not run; no narrow Java fixture exists for this chained socket-side branch. Java source was reviewed directly.
- Broad-validation trigger: live selected reward completion dispatch and persistence side effect.
- Broad .NET decision: skipped after focused validation; no shared schema, serializer, parser, scheduler, or common world-state model changed.

## Tests Run

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests"
```

Result: Passed, 36 total, 0 failed, 0 skipped.

Java/Maven: not run; no narrow Java fixture exists for this socket-side branch.

## Test Evidence

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleDialogSelectAsync_NpcTargetSelectedRewardOpensNextRegisteredRewardPage` | Regression | Java source review: `AbstractQuestHandler.sendQuestEndDialog` selected reward branch re-enters `QuestEngine.onDialog(... USE_OBJECT)` | Selected reward completion still sends the next reward page and now persists the corrected next reward quest state | Filtered C# socket boundary test with `PlayerEnterWorldService` and recording repository | Does not prove Java DAO timing; no narrow Java fixture exists |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `AbstractQuestHandler.sendQuestEndDialog` selected reward post-finish reward-page branch | `GameServerConnection.CreateNpcSelectedRewardPostFinishDialog` and `TryHandleQuestFinishAutoRewardAsync` | Quest dialog handler slice | Partial | Regression Tested | Partial Parity | Follow-up reward page correction is now persisted when the persistence service is present; arbitrary handler-specific quest bodies remain incomplete. |
| `QuestService.validateAndFixRewardGroup` in chained reward-page handling | `QuestFinishRewardPlanService.CorrectRewardGroup` consumed by `GameServerConnection.CreateNpcSelectedRewardPostFinishDialog` | Quest state mutation | Partial | Regression Tested | Partial Parity | Corrects invalid follow-up reward group before page selection; uses existing C# reward projection count. |
| `PlayerQuestListDAO.updateQuests` normal quest-state persistence | `PlayerEnterWorldService.PersistQuestStartAsync(..., false)` | Quest persistence | Partial | Regression Tested | Partial Parity | Reuses existing update path for corrected follow-up reward-state row; database integration not separately exercised here. |

## Known Gaps

- Full Java NPC controller, AI `onDialogSelect`, dynamic `QuestEngine.onDialog`, and arbitrary quest handler bodies remain incomplete.
- Handler-specific follow-up quest start pages and quest item side effects remain incomplete.
- Dynamic Java handler registration expressions that the extractor cannot resolve remain intentionally absent from the C# talk/start table.
- `QuestNpc` kill/attack/distance/aggro registrations remain unmodeled in this C# table.
- Live quest finish still does not support bonus rewards, challenge task completion, arbitrary quest completion callback handler bodies, mentor NPC faction title/flag side effects, or broad nearby quest refresh fanout.
