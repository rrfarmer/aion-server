# Phase 6 Session 2802 Handoff

## Current Phase

Phase 6: Port Game Core

## Latest Completed UOW

`[Phase 6][UOW-2802] Wire live quest finish default completion follow-up locks`

Commit made in this session:

- `[Phase 6][UOW-2802] Wire live quest finish default completion follow-up locks`

## Current State

- `GameServerConnection.HandleDialogSelectAsync` handles the Java self-target/reportable quest auto-reward branch for supported XP, kinah, fixed item rewards, regular selectable item rewards, class-selectable item rewards, extended selectable item rewards, work-item removal, title rewards, AP rewards, DP rewards, GP rewards, cube expansion rewards, warehouse expansion rewards, default completion follow-up starts, default mission follow-up locks, and non-mentor NPC faction completion.
- `StaticData.QuestCompletionFollowUps` loads Java quest handler source files from the configured quest handler directory and captures default completion follow-up registrations for literal ids and simple int arrays.
- Live quest finish now chooses `LOCKED` or `START` for those loaded default follow-up registrations after the completed quest `SmQuestAction.UPDATE` and before NPC faction completion/nearby quest refresh.
- Challenge task completion remains blocked on a missing live C# completion mutation service equivalent to Java `ChallengeTaskService.onChallengeQuestFinish`.
- Mentor NPC faction side effects remain blocked until the C# runtime has a clear Java-equivalent mentor flag time state and title packet side effects.

## Java Artifacts Touched

- `com.aionemu.gameserver.questEngine.handlers.AbstractQuestHandler.defaultOnQuestCompletedEvent`.
- `com.aionemu.gameserver.services.QuestService.addOrUpdateQuest`.
- `com.aionemu.gameserver.services.QuestService.finishQuest`.

## C# Artifacts Touched

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`.
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionQuestFinishDialogBoundaryTests.cs`.
- `docs/Phase-6-Session-2802-Completion.md`.
- `docs/Phase-6-Session-2802-Handoff.md`.

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

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `AbstractQuestHandler.defaultOnQuestCompletedEvent` mission lock branch | `GameServerConnection.GetQuestCompletionFollowUpStatus` | Runtime quest state mutation | Partial | Regression Tested | Partial Parity | Handles loaded default follow-up mission locks for partially complete handler pre-quest chains; recursive XML locks remain deferred. |
| `QuestService.addOrUpdateQuest` | `GameServerConnection.ApplyQuestCompletionFollowUpsAsync` | Runtime quest mutation and packet send | Partial | Regression Tested | Partial Parity | Uses ADD for new `LOCKED` follow-ups and keeps UPDATE/ADD selection for later START transitions. |

## Known Gaps

- Live quest finish still does not support bonus rewards, challenge task completion, arbitrary quest completion callback handler bodies, recursive XML follow-up locks, mentor NPC faction title/flag side effects, NPC-target dialog quest paths, or broad nearby quest refresh fanout.
- The completion follow-up extractor intentionally skips arbitrary expressions and non-default callback bodies.
- Direct reward persistence is not newly wired in quest finish for AP/DP/GP/cube/warehouse expansions. Existing later player-save behavior may persist mutated runtime state, but these UOWs did not add direct quest-finish persistence calls.
- Challenge task completion needs real C# runtime mutation/persistence work before it can be wired safely; current challenge support is not enough for Java `ChallengeTaskService.onChallengeQuestFinish` parity.
- No Java runtime/golden fixture exists for quest finish socket default completion follow-up lock behavior.

## Runtime Progress Gate For Next UOW

Recommended next UOW: `[Phase 6][UOW-2803] Wire live quest finish completion follow-up XML locks`

- Deferred/live behavior advanced: execute the recursive XML-start-condition lock branch of Java `defaultOnQuestCompletedEvent` for loaded default follow-up handlers.
- Java source of truth: `AbstractQuestHandler.defaultOnQuestCompletedEvent` loop over `template.getXMLStartConditions()` and `hasAnyPreQuestFinished(qsl, cond)` before calling `QuestService.addOrUpdateQuest(player, questId, QuestStatus.LOCKED)`.
- C# runtime artifact to wire/fix: extend `GameServerConnection.GetQuestCompletionFollowUpStatus` or a small runtime helper to detect loaded follow-up mission XML-start-condition chains where any recursive finished pre-quest is complete.
- Client-visible/state/persistence effect expected: finishing a supported chain quest can add or update a follow-up quest to `LOCKED`, persist it through the existing quest table shape, and send Java-equivalent `SmQuestAction` ADD/UPDATE.
- Why this is not preview-only/test-only/documentation-only: it must mutate live quest state and send a real quest action packet from live quest finish.

## Focused Validation Recipe For Next UOW

- Specific behavior/contract to prove: Java default completion callback locks a follow-up mission when XML start conditions are not yet satisfied but a recursive pre-quest chain has at least one completed quest.
- Focused C# command: start with `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~NearbyQuestStartConditionServiceTests"`.
- Focused Java/Maven command: not expected unless a narrow Java fixture is added for `defaultOnQuestCompletedEvent` XML locking.
- Broad-validation trigger: live quest-finish side effect and shared start-condition logic if edited.
- Broad .NET decision: do not run unless focused evidence exposes wider risk or the implementation edits shared XML start-condition evaluation.

## Safe Runtime Candidates

- Wire recursive XML-start-condition default follow-up LOCKED state for campaign chains using loaded quest XML start-condition summaries.
- Wire one concrete arbitrary quest completion callback only if its Java body can be ported directly and has clear live state or packet effects.
- Wire mentor NPC faction quest finish side effects only if the required live mentor flag state and title packet artifacts exist.
- Wire challenge task completion only after adding or finding a real C# runtime service for Java `onChallengeQuestFinish` mutation/persistence/packet side effects.
- Wire NPC-target dialog quest finish paths by selecting one concrete Java `DialogService`/handler path and proving live state or packet effects.

## Summary Metrics

- Total Java artifacts touched/discovered in latest UOW: 3.
- Total artifacts ported or wired in latest UOW: 1 live default quest completion follow-up mission lock path.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification/partial parity: 2.
- Total blocked artifacts: 2 challenge task completion and mentor NPC faction side effects.
- Estimated overall migration completion: unchanged conservatively; Phase 6 remains in progress.
