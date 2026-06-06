# Phase 6 Session 2803 Completion

## Unit Of Work

`[Phase 6][UOW-2803] Wire live quest finish XML follow-up locks`

## Runtime Progress Gate

- Deferred/live behavior advanced: live self-target reportable quest finish now executes the Java `defaultOnQuestCompletedEvent` recursive XML-start-condition lock branch for loaded default completion follow-up handlers.
- Java source of truth: `AbstractQuestHandler.defaultOnQuestCompletedEvent` loops over `template.getXMLStartConditions()` and calls `hasAnyPreQuestFinished(qsl, cond)` before `QuestService.addOrUpdateQuest(player, questId, QuestStatus.LOCKED)`.
- C# runtime artifact wired: `GameServerConnection.GetQuestCompletionFollowUpStatus` now detects XML-start-condition failures for new mission follow-ups and recursively scans loaded XML `finished` chains for any completed quest.
- Client-visible/state/persistence effect changed: completing a supported quest can add a follow-up mission quest in `LOCKED` state from a recursive XML precondition chain, persist it through the existing player quest table shape when the repository service is present, and send a real `SmQuestAction.ADD` packet.
- Why this is not preview-only/test-only/documentation-only: the live socket handler mutates `player.Quests` and sends the follow-up quest packet from `HandleDialogSelectAsync`.

## Java Parity Notes

- Java `hasAnyPreQuestFinished` recursively walks XML `<finished>` preconditions until it finds any completed quest in the chain. The C# helper mirrors that behavior over the existing loaded `NearbyQuestTemplateSummary.XmlStartConditions`.
- The C# helper adds a visited set to avoid infinite recursion on malformed or cyclic static data. Java does not guard cycles explicitly.
- This UOW only applies the recursive XML lock branch after normal C# nearby start-condition evaluation fails with `XmlStartConditions`, preserving existing nearby quest marker/start behavior.

## C# Artifacts Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionQuestFinishDialogBoundaryTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestCompletionFollowUpJavaHandlerExtractorTests.cs`

## Validation Decision

- Changed surface: live quest-finish side effect and quest state packet selection.
- Specific behavior/contract: Java default completion callback locks a follow-up mission when XML start conditions are not startable yet but a recursive finished-prequest chain contains the quest just completed.
- Focused C# command: `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~NearbyQuestStartConditionServiceTests|FullyQualifiedName~QuestCompletionFollowUpJavaHandlerExtractorTests"`
- Focused Java/Maven command: not run; no narrow Java fixture exists for `defaultOnQuestCompletedEvent` XML locking, and Java source review was used as the oracle.
- Broad-validation trigger: live quest-finish side effect.
- Broad .NET decision: skipped after focused validation; the filtered command compiled the affected project and exercised the live socket boundary plus adjacent start-condition and Java handler extraction tests.
- Why this scope is sufficient: the new boundary test loads a Java handler source file through the production static-data path, completes a quest through `HandleDialogSelectAsync`, verifies the completed quest update packet is sent first, then verifies the recursive XML follow-up mission is added live with status `LOCKED`.

## Tests Run

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~NearbyQuestStartConditionServiceTests|FullyQualifiedName~QuestCompletionFollowUpJavaHandlerExtractorTests"
```

Result: Passed, 34 total, 0 failed, 0 skipped.

Java/Maven: not run; no narrow Java fixture exists for this socket-side quest completion follow-up XML lock path.

## Test Evidence

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleDialogSelectAsync_ReportableAutoRewardQuestLocksDefaultCompletionFollowUpMissionForRecursiveXmlChain` | Regression | Java source review: `AbstractQuestHandler.defaultOnQuestCompletedEvent` and `hasAnyPreQuestFinished` | Live quest finish locks a mission follow-up from a loaded no-arg Java handler registration when XML finished preconditions recurse through a completed quest | Filtered C# boundary test with live quest mutation and packet assertions | Does not cover unsupported XML elements or arbitrary callback bodies |
| `Extract_ReadsDefaultOnQuestCompletedWithoutPreQuestArguments` | Unit | Java source review: no-arg `defaultOnQuestCompletedEvent(env)` handlers in ascension quest handlers | The production extractor captures no-arg default completion handlers so XML-only chains can run live | Focused C# extractor test | Does not parse arbitrary callback expressions |
| Existing `NearbyQuestStartConditionServiceTests` | Unit | Java source review: XML start-condition evaluation | Existing XML start-condition pass/fail behavior remains covered while the live lock branch consumes the `XmlStartConditions` failure signal | Focused adjacent service tests | The recursive lock scan is intentionally in the live connection path, not the marker service |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.questEngine.handlers.AbstractQuestHandler.defaultOnQuestCompletedEvent` XML lock branch | `Aion.GameServer.Network.Aion.GameServerConnection.GetQuestCompletionFollowUpStatus` | Runtime quest state mutation | Partial | Regression Tested | Partial Parity | Handles recursive XML `finished` precondition locks for new mission follow-ups loaded from default Java handlers; arbitrary callback bodies and unsupported XML elements remain incomplete. |
| `com.aionemu.gameserver.questEngine.handlers.AbstractQuestHandler.hasAnyPreQuestFinished` | `Aion.GameServer.Network.Aion.GameServerConnection.HasAnyCompletedXmlPreQuest` | Runtime helper | Partial | Regression Tested | Partial Parity | Recurses loaded XML `finished` chains and detects any completed quest; C# adds cycle protection and skips missing templates rather than throwing. |
| `com.aionemu.gameserver.services.QuestService.addOrUpdateQuest` | `Aion.GameServer.Network.Aion.GameServerConnection.ApplyQuestCompletionFollowUpsAsync` | Runtime quest mutation and packet send | Partial | Regression Tested | Partial Parity | Uses ADD for new `LOCKED` XML follow-ups and preserves UPDATE/ADD selection for later START transitions. |

## Parity Status

Partial parity improved for the self-target/reportable auto-reward quest finish branch. Supported reward and finish side effects now include XP, kinah, fixed item rewards, regular selectable item rewards, class-selectable item rewards, extended selectable item rewards, work-item removal, title rewards, AP rewards, DP rewards, GP rewards, cube expansion rewards, warehouse expansion rewards, default quest completion follow-up starts, handler prequest mission locks, recursive XML follow-up mission locks, and non-mentor NPC faction completion.

## Known Gaps

- Live quest finish still does not support bonus rewards, challenge task completion, arbitrary quest completion callback handler bodies, mentor NPC faction title/flag side effects, NPC-target dialog quest paths, or broad nearby quest refresh fanout.
- The completion follow-up extractor intentionally skips arbitrary expressions and non-default callback bodies.
- Direct reward persistence is not newly wired in quest finish for AP/DP/GP/cube/warehouse expansions. Existing later player-save behavior may persist mutated runtime state, but this UOW did not add direct quest-finish persistence calls.
- Challenge task completion needs real C# runtime mutation/persistence work before it can be wired safely; current challenge support is not enough for Java `ChallengeTaskService.onChallengeQuestFinish` parity.
- No Java runtime/golden fixture exists for quest finish socket default completion follow-up XML lock behavior.
