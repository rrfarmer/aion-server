# Phase 6 Session 2818 Completion

## Unit Of Work

`[Phase 6][UOW-2818] Wire selected reward post-finish pre-quest continuation start page`

## Runtime Progress Gate

- Deferred/live behavior advanced: after live NPC-target selected reward completion, the server can directly show the follow-up quest start page when a newly startable same-NPC quest depends on the just-finished quest.
- Java source of truth: `AbstractQuestHandler.sendQuestEndDialog` scans `QuestNpc.getOnQuestStart()`, calls `QuestService.checkStartConditions(player, questId, false)`, checks XML `<finished>` preconditions for `env.getQuestId()`, verifies `isAcceptableQuest(template)`, then re-enters `QuestEngine.onDialog(env)` with `DialogAction.QUEST_SELECT`.
- C# runtime artifact wired: `GameServerConnection.CreateNpcSelectedRewardPostFinishDialog` now detects modeled XML finished-precondition follow-ups and emits the default quest start page packet for the follow-up quest.
- Client-visible effect changed: selected reward completion can now send `SmDialogWindow(targetObjectId, 1011, followUpQuestId)` instead of generic page `10` when the Java continuation branch applies.
- Why this is not preview-only/test-only/documentation-only: it changes the real server packet emitted after live selected reward completion.

## Java Parity Notes

- Java reward-page precedence remains first, followed by Java's new-quest scan. A matching acceptable finished-precondition follow-up now opens the follow-up start page before the generic page `10` fallback.
- Java `isAcceptableQuest` rejects `minlevel_permitted == 99` and quests without rewards, extended rewards, bonus, quest drops, or class-selectable rewards.
- C# uses loaded reward projections and quest-drop data to avoid treating empty `<rewards />` as acceptable.
- This UOW models the default `sendQuestNoneDialog(..., 1011)` continuation page. Arbitrary handler-specific start pages and custom quest handler bodies remain incomplete.

## C# Artifacts Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionQuestFinishDialogBoundaryTests.cs`

## Validation Decision

- Changed surface: live connection dispatch and server packet send.
- Specific behavior/contract: selected reward completion sends follow-up start page `1011` for a same-NPC new quest whose XML finished precondition matches the completed quest, while a generic new startable quest still sends page `10`.
- Focused C# command: `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~NearbyQuestStartConditionServiceTests"`
- Focused Java/Maven command: not run; no narrow Java fixture exists for this socket-side branch. Java source was reviewed directly.
- Broad-validation trigger: live connection dispatch and server packet send.
- Broad .NET decision: skipped after focused validation; no shared parser, serializer, database schema, scheduler, or common world-state model was changed.
- Why this scope is sufficient: the boundary test loads XML start registrations and XML finished preconditions, completes a live selected reward quest, and asserts the emitted dialog packet.

## Tests Run

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~NearbyQuestStartConditionServiceTests"
```

Result: Passed, 49 total, 0 failed, 0 skipped.

Java/Maven: not run; no narrow Java fixture exists for this socket-side branch.

```powershell
git diff --check
```

Result: Passed; only normal workspace CRLF conversion warnings were reported.

## Test Evidence

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleDialogSelectAsync_NpcTargetSelectedRewardOpensFollowUpStartPageForFinishedPreQuest` | Regression | Java source review: `AbstractQuestHandler.sendQuestEndDialog` pre-quest continuation branch | Selected reward completion opens page `1011` for a startable same-NPC follow-up quest with XML `<finished quest_id="completedQuest" />` | Filtered C# socket boundary test with XML-loaded start registration, live quest completion mutation, and serialized packet assertions | Does not cover custom handler-specific start pages |
| `HandleDialogSelectAsync_NpcTargetSelectedRewardOpensSelectionPageForNewStartableQuest` | Regression | Java source review: generic `npcHasNewQuest` fallback | Generic same-NPC new startable quests still send selection page `10` when the continuation branch does not apply | Filtered C# socket boundary test remained green | Does not cover all Java handler bodies |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.questEngine.handlers.AbstractQuestHandler.sendQuestEndDialog` post-finish pre-quest continuation | `Aion.GameServer.Network.Aion.GameServerConnection.CreateNpcSelectedRewardPostFinishDialog` | Quest dialog handler slice | Partial | Regression Tested | Partial Parity | Wires default follow-up start page `1011` for modeled XML finished-precondition continuations; arbitrary handler-specific start pages remain incomplete. |
| `com.aionemu.gameserver.questEngine.handlers.AbstractQuestHandler.isAcceptableQuest` | `GameServerConnection.IsNpcSelectedRewardPostFinishAcceptableQuest` | Helper | Partial | Regression Tested | Partial Parity | Uses loaded reward projections and quest drops to reject empty rewards; relies on currently modeled reward/drop data. |

## Known Gaps

- Full Java NPC controller, AI `onDialogSelect`, dynamic `QuestEngine.onDialog`, and arbitrary quest handler bodies remain incomplete.
- Handler-specific follow-up quest start pages and quest item side effects remain incomplete.
- Dynamic Java handler registration expressions that the extractor cannot resolve remain intentionally absent from the C# talk/start table.
- `QuestNpc` kill/attack/distance/aggro registrations remain unmodeled in this C# table.
- Reward-group correction from the reward-selection page path remains in-memory unless normal quest persistence later saves it.
- Live quest finish still does not support bonus rewards, challenge task completion, arbitrary quest completion callback handler bodies, mentor NPC faction title/flag side effects, or broad nearby quest refresh fanout.
