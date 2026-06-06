# Phase 6 Session 2816 Completion

## Unit Of Work

`[Phase 6][UOW-2816] Wire selected reward post-finish active quest selection`

## Runtime Progress Gate

- Deferred/live behavior advanced: after live NPC-target selected reward completion, the server can keep the NPC conversation on Java selection page `10` when another same-NPC registered talk quest is active.
- Java source of truth: `AbstractQuestHandler.sendQuestEndDialog` scans `QuestNpc.getOnTalkEvent()` after `QuestService.finishQuest(env)`, records `npcHasActiveQuest`, and returns `sendQuestSelectionDialog(env)` when active/new candidates remain.
- C# runtime artifact wired: `GameServerConnection.CreateNpcSelectedRewardPostFinishDialog` now detects active same-NPC talk quests from `StaticData.QuestNpcStarts.OnTalkEvent` and live `PlayerQuestState`.
- Client-visible effect changed: selected reward completion can now send `SmDialogWindow(targetObjectId, 10, 0)` instead of always closing when a same-NPC active talk quest remains.
- Why this is not preview-only/test-only/documentation-only: it changes the real server packet emitted after live quest reward completion.

## Java Parity Notes

- Java gives same-NPC `REWARD` quests precedence by re-entering the reward branch before returning page `10`; C# preserves that ordering.
- Java marks active talk candidates when the player's quest state is `START` and the quest is not merely a normal quest-start registration for the same NPC.
- Java also treats same-NPC mission start registrations at `questVars == 0` as active; C# includes that safe branch using loaded quest template category data.
- Java's new startable quest and pre-quest continuation branches remain incomplete in C#.

## C# Artifacts Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionQuestFinishDialogBoundaryTests.cs`

## Validation Decision

- Changed surface: live connection dispatch and server packet send.
- Specific behavior/contract: selected reward completion sends page `10` for a same-NPC active registered talk quest, while existing next-reward and no-candidate branches remain covered by the same boundary suite.
- Focused C# commands:
  - `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests"`
  - `git diff --check`
- Focused Java/Maven command: not run; no narrow Java fixture exists for this socket-side branch. Java source was reviewed directly.
- Broad-validation trigger: live connection dispatch and server packet send.
- Broad .NET decision: skipped after focused validation; no shared parser, serializer, database schema, scheduler, or common world-state model was changed.

## Tests Run

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests"
```

Result: Passed, 34 total, 0 failed, 0 skipped.

Java/Maven: not run; no narrow Java fixture exists for this socket-side branch.

```powershell
git diff --check
```

Result: Passed; only normal workspace CRLF conversion warnings were reported.

## Test Evidence

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleDialogSelectAsync_NpcTargetSelectedRewardOpensSelectionPageForActiveRegisteredTalkQuest` | Regression | Java source review: `AbstractQuestHandler.sendQuestEndDialog` active quest scan | Selected reward completion opens selection page `10` when another same-NPC `START` quest is registered on talk and no reward quest takes precedence | Filtered C# socket boundary test with Java handler fixture, live inventory/quest mutation, and serialized packet assertions | Does not cover new startable quest selection or pre-quest continuation |
| `HandleDialogSelectAsync_NpcTargetSelectedRewardOpensNextRegisteredRewardPage` | Regression | Java source review: reward branch precedence in `sendQuestEndDialog` | Same-NPC `REWARD` quests still open reward page before active page `10` fallback | Existing focused C# boundary test remained green | Does not prove arbitrary handler body parity |
| `HandleDialogSelectAsync_NpcTargetSelectedRewardAddsSelectedItemCompletesQuestAndClosesDialog` | Regression | Java source review: close fallback after no remaining candidate | Selected reward completion still closes when no same-NPC candidate remains | Existing focused C# boundary test remained green | Does not cover new quest discovery |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `AbstractQuestHandler.sendQuestEndDialog` post-finish active quest scan | `GameServerConnection.CreateNpcSelectedRewardPostFinishDialog` | Quest dialog handler slice | Partial | Regression Tested | Partial Parity | Wires reward precedence and active talk selection page `10`; new startable quests and pre-quest continuation remain incomplete. |
| `QuestNpc.getOnTalkEvent()` active quest lookup | `QuestNpcStartRegistration.OnTalkEvent` consumed by live selected reward finish | Runtime static-data usage | Partial | Regression Tested | Partial Parity | Uses already-loaded Java handler talk registrations to change live packet output. |

## Known Gaps

- Full Java NPC controller, AI `onDialogSelect`, dynamic `QuestEngine.onDialog`, and arbitrary quest handler bodies remain incomplete.
- Java post-finish selected reward logic for new startable quests and follow-up pre-quest start dialogs is still incomplete.
- Dynamic Java handler registration expressions that the extractor cannot resolve remain intentionally absent from the C# talk/start table.
- `QuestNpc` kill/attack/distance/aggro registrations remain unmodeled in this C# table.
- Live quest finish still does not support bonus rewards, challenge task completion, arbitrary quest completion callback handler bodies, mentor NPC faction title/flag side effects, or broad nearby quest refresh fanout.
