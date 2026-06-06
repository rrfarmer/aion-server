# Phase 6 Session 2801 Completion

## Unit Of Work

`[Phase 6][UOW-2801] Wire live quest finish default completion follow-ups`

## Runtime Progress Gate

- Deferred/live behavior advanced: live self-target reportable auto-reward quest finish now executes the common Java `defaultOnQuestCompletedEvent` follow-up start path for loaded Java quest handlers.
- Java source of truth: `QuestService.finishQuest` calls `QuestEngine.getInstance().onQuestCompleted(player, id)` after `SM_QUEST_ACTION.UPDATE`; `QuestEngine.onQuestCompleted` invokes registered handlers; `AbstractQuestHandler.defaultOnQuestCompletedEvent` starts the handler quest when pre-quests and start conditions are satisfied.
- C# runtime artifact wired: `StaticData` now loads `QuestCompletionFollowUpTable` from Java handler source files, and `GameServerConnection.TryHandleQuestFinishAutoRewardAsync` applies loaded default follow-up starts after the completed quest update and before NPC faction completion/nearby refresh.
- Client-visible/state/persistence effect changed: completing a supported quest can now add or update a follow-up quest to `START`, persist it through the existing player quest table shape, and send a real `SmQuestAction` ADD/UPDATE packet.
- Why this is not preview-only/test-only/documentation-only: the live socket handler now mutates `player.Quests`, persists the follow-up quest state when a repository service is available, and sends a server packet.

## Java Parity Notes

- This UOW covers the default follow-up start branch for handlers that call `qe.registerOnQuestCompleted(questId)` and `defaultOnQuestCompletedEvent(env, ...)` with literal pre-quest ids or a simple int array.
- Java also supports mission locking when only part of a chain is complete and arbitrary custom `onQuestCompletedEvent` handler bodies. Those remain deferred.
- Java dispatches all registered completion handlers for every completed quest. The C# implementation similarly scans all loaded default follow-ups and applies only those whose pre-quests are already complete and whose start conditions pass.

## C# Artifacts Changed

- `dotnetConversion/src/Aion.GameServer/Dataholders/QuestCompletionFollowUpTable.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/StaticData.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestCompletionFollowUpJavaHandlerExtractorTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionQuestFinishDialogBoundaryTests.cs`

## Validation Decision

- Changed surface: runtime Java handler-source loading and live quest finish quest-state mutation/packet send.
- Specific behavior/contract: Java default completion callback starts the handler quest after the completed quest update when the configured pre-quest is complete.
- Focused C# command: `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~QuestCompletionFollowUpJavaHandlerExtractorTests|FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests"`
- Focused Java/Maven command: not run; no narrow Java fixture exists for socket quest finish plus default completion follow-up dispatch.
- Broad-validation trigger: live quest-finish side effect and static-data runtime loading changed.
- Broad .NET decision: skipped after focused validation; the filtered command compiled the affected project and exercised both the new loader and live handler boundary.
- Why this scope is sufficient: the boundary test loads a Java handler file through the production static-data path, finishes a quest through `HandleDialogSelectAsync`, verifies the completed quest update packet is sent first, then verifies the follow-up quest is added live with `SmQuestAction.ADD`.

## Tests Run

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~QuestCompletionFollowUpJavaHandlerExtractorTests|FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests"
```

Result: Passed, 19 total, 0 failed, 0 skipped.

Java/Maven: not run; no narrow Java fixture exists for the socket-side default completion follow-up path.

## Test Evidence

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleDialogSelectAsync_ReportableAutoRewardQuestStartsDefaultCompletionFollowUpAfterQuestUpdate` | Regression | Java source review: `QuestService.finishQuest`, `QuestEngine.onQuestCompleted`, `AbstractQuestHandler.defaultOnQuestCompletedEvent` | Live quest finish loads a Java handler registration, completes the source quest, starts the follow-up quest, and sends quest packets in Java order | Filtered C# boundary test with live quest mutation and packet assertions | Does not cover mission LOCKED follow-ups or custom handler bodies |
| `Extract_ReadsLiteralDefaultOnQuestCompletedFollowUp` | Unit | Java handler source pattern review | Literal pre-quest registrations are loaded from Java handler files | Focused C# unit test | Parser intentionally limited |
| `Extract_ReadsArrayDefaultOnQuestCompletedFollowUps` | Unit | Java handler source pattern review | Simple int-array pre-quest registrations are loaded from Java handler files | Focused C# unit test | Does not resolve arbitrary expressions |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `QuestEngine.onQuestCompleted` | `GameServerConnection.ApplyQuestCompletionFollowUpsAsync` | Runtime quest callback dispatch slice | Partial | Regression Tested | Partial Parity | Supports loaded default follow-up starts; arbitrary handlers and exception behavior remain missing. |
| `AbstractQuestHandler.defaultOnQuestCompletedEvent` | `QuestCompletionFollowUpTable` plus `GameServerConnection.ApplyQuestCompletionFollowUpsAsync` | Runtime quest state mutation | Partial | Unit Tested / Regression Tested | Partial Parity | Starts follow-up quests when pre-quests are complete and start conditions pass; mission LOCKED branch remains deferred. |
| Java quest handler `registerOnQuestCompleted` source declarations | `QuestCompletionFollowUpJavaHandlerExtractor` | Runtime static-data loading | Partial | Unit Tested / Regression Tested | Partial Parity | Loads literal and simple int-array default follow-up registrations from Java handler source. |

## Parity Status

Partial parity improved for the self-target/reportable auto-reward quest finish branch. Supported reward and finish side effects now include XP, kinah, fixed item rewards, regular selectable item rewards, class-selectable item rewards, extended selectable item rewards, work-item removal, title rewards, AP rewards, DP rewards, GP rewards, cube expansion rewards, warehouse expansion rewards, default quest completion follow-up starts, and non-mentor NPC faction completion. Bonus rewards, challenge tasks, arbitrary quest completion callbacks, mission follow-up LOCKED state, mentor NPC faction title/flag side effects, NPC-target dialog quest paths, direct quest-finish reward persistence for some non-item state, and broad nearby quest fanout remain incomplete.
