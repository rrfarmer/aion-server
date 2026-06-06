# Phase 6 Session 2817 Completion

## Unit Of Work

`[Phase 6][UOW-2817] Wire selected reward post-finish new quest selection`

## Runtime Progress Gate

- Deferred/live behavior advanced: after live NPC-target selected reward completion, the server can keep the NPC conversation on Java selection page `10` when the same NPC has a new startable quest.
- Java source of truth: `AbstractQuestHandler.sendQuestEndDialog` scans `QuestNpc.getOnQuestStart()` after `QuestService.finishQuest(env)`, calls `QuestService.checkStartConditions(player, questId, false)`, and returns `sendQuestSelectionDialog(env)` when `npcHasNewQuest` is true.
- C# runtime artifact wired: `GameServerConnection.CreateNpcSelectedRewardPostFinishDialog` now consults `QuestNpcStartRegistration.OnQuestStart` plus `NearbyQuestStartConditionService`.
- Client-visible effect changed: selected reward completion can now send `SmDialogWindow(targetObjectId, 10, 0)` instead of close page `0` when a same-NPC new quest is startable.
- Why this is not preview-only/test-only/documentation-only: it changes the real server packet emitted after live selected reward completion.

## Java Parity Notes

- Java gives same-NPC `REWARD` quests precedence, then tracks active talk quests, then scans new startable quests.
- Java's `sendQuestEndDialog` new quest branch uses `QuestService.checkStartConditions(player, questId, false)`, which has no nearby gray-arrow level allowance.
- C# added an allowed-diff parameter to the existing start-condition service so nearby refresh callers keep diff `2` while this post-finish branch uses diff `0`.
- Java's pre-quest continuation sub-branch, which can enter the follow-up quest start dialog directly, remains incomplete.

## C# Artifacts Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/NearbyQuestStartConditionService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionQuestFinishDialogBoundaryTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/NearbyQuestStartConditionServiceTests.cs`

## Validation Decision

- Changed surface: live connection dispatch, server packet send, and shared start-condition service parameterization.
- Specific behavior/contract: selected reward completion sends page `10` for a same-NPC new startable quest; the close fallback remains close when same-NPC start quests are repeat-exhausted or underleveled under Java's exact min-level gate.
- Focused C# command: `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~NearbyQuestStartConditionServiceTests"`
- Focused Java/Maven command: not run; no narrow Java fixture exists for this socket-side branch. Java source was reviewed directly.
- Broad-validation trigger: live connection dispatch and server packet send.
- Broad .NET decision: skipped after focused validation; no shared parser, serializer, database schema, scheduler, or common world-state model was changed.
- Why this scope is sufficient: the boundary test loads runtime start registrations from XML, completes a live selected reward quest, and asserts the emitted dialog packet; the adjacent service test covers the Java min-level overload difference.

## Tests Run

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~NearbyQuestStartConditionServiceTests"
```

Result: Passed, 48 total, 0 failed, 0 skipped.

Java/Maven: not run; no narrow Java fixture exists for this socket-side branch.

```powershell
git diff --check
```

Result: Passed; only normal workspace CRLF conversion warnings were reported.

## Test Evidence

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleDialogSelectAsync_NpcTargetSelectedRewardOpensSelectionPageForNewStartableQuest` | Regression | Java source review: `AbstractQuestHandler.sendQuestEndDialog` new quest scan | Selected reward completion opens selection page `10` when the same NPC has a new startable quest | Filtered C# socket boundary test with XML-loaded start registration, live quest completion mutation, and serialized packet assertions | Does not cover direct pre-quest continuation start dialog |
| `HandleDialogSelectAsync_NpcTargetSelectedRewardAddsSelectedItemCompletesQuestAndClosesDialog` | Regression | Java source review: `QuestService.checkStartConditions(player, questId, false)` | Selected reward completion still closes when same-NPC start quests are not startable, including an underleveled candidate that would pass nearby diff `2` | Filtered C# socket boundary test with live packet assertions | Does not cover every start-condition failure kind |
| `CheckNearbyStartConditions_AllowsJavaExactMinLevelGateForNonNearbyCallers` | Unit | Java source review: `QuestService.checkStartConditions` overloads | Runtime callers can request min-level diff `0` without changing nearby refresh behavior | Filtered C# unit test | Other Java overload flags remain unsupported |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.questEngine.handlers.AbstractQuestHandler.sendQuestEndDialog` post-finish new quest scan | `Aion.GameServer.Network.Aion.GameServerConnection.CreateNpcSelectedRewardPostFinishDialog` | Quest dialog handler slice | Partial | Regression Tested | Partial Parity | Wires reward precedence, active talk page `10`, and new startable quest page `10`; pre-quest continuation start dialog remains incomplete. |
| `com.aionemu.gameserver.services.QuestService.checkStartConditions` min-level overload behavior | `Aion.GameServer.Services.NearbyQuestStartConditionService.CheckNearbyStartConditions` | Service | Partial | Unit Tested | Partial Parity | Supports caller-selected min-level diff for existing modeled checks; Java skip flags and warning packets remain outside this service. |

## Known Gaps

- Full Java NPC controller, AI `onDialogSelect`, dynamic `QuestEngine.onDialog`, and arbitrary quest handler bodies remain incomplete.
- Java post-finish selected reward logic for follow-up pre-quest start dialogs is still incomplete.
- Dynamic Java handler registration expressions that the extractor cannot resolve remain intentionally absent from the C# talk/start table.
- `QuestNpc` kill/attack/distance/aggro registrations remain unmodeled in this C# table.
- Reward-group correction from the reward-selection page path remains in-memory unless normal quest persistence later saves it.
- Live quest finish still does not support bonus rewards, challenge task completion, arbitrary quest completion callback handler bodies, mentor NPC faction title/flag side effects, or broad nearby quest refresh fanout.
