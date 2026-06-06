# Phase 6 Session 2802 Completion

## Unit Of Work

`[Phase 6][UOW-2802] Wire live quest finish default completion follow-up locks`

## Runtime Progress Gate

- Deferred/live behavior advanced: live self-target reportable auto-reward quest finish now executes the Java `defaultOnQuestCompletedEvent` mission follow-up `LOCKED` branch for partially completed pre-quest chains.
- Java source of truth: `AbstractQuestHandler.defaultOnQuestCompletedEvent` calls `QuestService.addOrUpdateQuest(player, questId, QuestStatus.LOCKED)` when a mission follow-up is not startable yet but at least one configured pre-quest in the chain is complete.
- C# runtime artifact wired: `GameServerConnection.ApplyQuestCompletionFollowUpsAsync` now chooses `LOCKED` or `START` for loaded default completion follow-up registrations instead of only starting quests.
- Client-visible/state/persistence effect changed: completing a supported quest can now add a follow-up mission quest in `LOCKED` state, persist it through the existing player quest table shape, and send a real `SmQuestAction.ADD` packet.
- Why this is not preview-only/test-only/documentation-only: the live socket handler mutates `player.Quests`, persists the follow-up state when a repository service is available, and sends a server packet.

## Java Parity Notes

- This UOW covers the pre-quest-chain lock branch for mission follow-ups and also preserves a mission level-window lock path for all-prequest-complete cases below the mission minimum level.
- Existing `LOCKED` follow-up quests remain eligible to transition to `START` later when all pre-quests and start conditions pass, matching Java's null-or-LOCKED guard.
- Java also supports recursive XML-start-condition locking through `hasAnyPreQuestFinished`. That deeper branch remains deferred until C# has a tighter runtime model for those recursive XML conditions.

## C# Artifacts Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionQuestFinishDialogBoundaryTests.cs`

## Validation Decision

- Changed surface: live quest-finish side effect and quest state packet selection.
- Specific behavior/contract: Java default completion callback adds a mission follow-up quest as `LOCKED` when the handler pre-quest list is partially complete.
- Focused C# command: `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~QuestCompletionFollowUpJavaHandlerExtractorTests|FullyQualifiedName~QuestCompletionFollowUpPlanServiceTests"`
- Focused Java/Maven command: not run; no narrow Java fixture exists for `defaultOnQuestCompletedEvent` mission locking.
- Broad-validation trigger: live quest-finish side effect.
- Broad .NET decision: skipped after focused validation; the filtered command compiled the affected project and exercised the edited live socket boundary plus adjacent follow-up loader/planner tests.
- Why this scope is sufficient: the new boundary test loads a Java handler file through the production static-data path, finishes a quest through `HandleDialogSelectAsync`, verifies the completed quest update packet is sent first, then verifies the follow-up mission is added live with `SmQuestAction.ADD` and status `LOCKED`.

## Tests Run

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~QuestCompletionFollowUpJavaHandlerExtractorTests|FullyQualifiedName~QuestCompletionFollowUpPlanServiceTests"
```

Result: Passed, 25 total, 0 failed, 0 skipped.

Java/Maven: not run; no narrow Java fixture exists for the socket-side default completion follow-up lock path.

## Test Evidence

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleDialogSelectAsync_ReportableAutoRewardQuestLocksDefaultCompletionFollowUpMissionForPartialChain` | Regression | Java source review: `AbstractQuestHandler.defaultOnQuestCompletedEvent` pre-quest-chain lock branch | Live quest finish locks a mission follow-up from a loaded Java handler registration and sends `SmQuestAction.ADD` after the completed quest update | Filtered C# boundary test with live quest mutation and packet assertions | Does not cover recursive XML-start-condition lock branch |
| Existing default follow-up start boundary test | Regression | Java source review: `QuestService.finishQuest`, `QuestEngine.onQuestCompleted`, `AbstractQuestHandler.defaultOnQuestCompletedEvent` | Existing live follow-up `START` behavior remains intact while adding lock support | Focused C# boundary test | Does not cover custom handler bodies |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `AbstractQuestHandler.defaultOnQuestCompletedEvent` mission lock branch | `GameServerConnection.GetQuestCompletionFollowUpStatus` | Runtime quest state mutation | Partial | Regression Tested | Partial Parity | Handles loaded default follow-up mission locks for partially complete handler pre-quest chains; recursive XML locks remain deferred. |
| `QuestService.addOrUpdateQuest` | `GameServerConnection.ApplyQuestCompletionFollowUpsAsync` | Runtime quest mutation and packet send | Partial | Regression Tested | Partial Parity | Uses ADD for new `LOCKED` follow-ups and keeps UPDATE/ADD selection for later START transitions. |

## Parity Status

Partial parity improved for the self-target/reportable auto-reward quest finish branch. Supported reward and finish side effects now include XP, kinah, fixed item rewards, regular selectable item rewards, class-selectable item rewards, extended selectable item rewards, work-item removal, title rewards, AP rewards, DP rewards, GP rewards, cube expansion rewards, warehouse expansion rewards, default quest completion follow-up starts and mission locks, and non-mentor NPC faction completion. Bonus rewards, challenge tasks, arbitrary quest completion callbacks, recursive XML follow-up locks, mentor NPC faction title/flag side effects, NPC-target dialog quest paths, direct quest-finish reward persistence for some non-item state, and broad nearby quest fanout remain incomplete.
