# Phase 6TE Completion - UOW-1013 Staged NPC Faction Completion

## Scope

UOW-1013 stages the NPC faction completion subset used by Java `QuestService.finishQuest` after a quest is completed. It adds a pure daily reset calculator for `NpcFactions.getNextTime()` and a snapshot helper that marks the active mentor/non-mentor faction slot complete.

This unit does not wire the helper into production quest completion. It does not send title packets, set mentor flag time, persist `player_npc_factions`, assign daily quests, abandon quests, or refresh nearby quests.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Staged NPC faction completion helper | `NpcFactions.completeQuest`, `NpcFactions.getNextTime`, `ENpcFactionQuestState` | `PlayerNpcFactionState.cs`, `NpcFactionDailyResetService.cs`, focused tests, docs | Service / State Port | Selected sequential | Medium | Direct follow-up to staged quest-finish mutation. Touches shared faction snapshot state. |
| B | Mentor-title side-effect audit | `CommonData.setMentorFlagTime`, `SM_TITLE_INFO` | docs/read-only | Java Analysis | Yes | Medium | Deferred because packet/common-data side effects need separate production homes. |
| C | Faction DAO write boundary | `PlayerNpcFactionsDAO.storeNpcFactions` | future repository contract/tests | Repository Port | No | High | Requires write ordering and rollback policy for quest completion. |
| D | Daily quest assignment lifecycle | `NpcFactions.sendDailyQuest`, `QuestService.startQuest` | future service/tests | Service Port | No | High | Random quest assignment and packets are broader than completion state mutation. |

Selected batch: local-only A. No sub-agent was spawned because the selected work modified shared player faction state.

## Java Breadcrumbs

- `QuestService.finishQuest` calls `player.getNpcFactions().completeQuest(template)` after quest-completed callbacks.
- `NpcFactions.completeQuest` chooses the active slot by `questTemplate.isMentor()`, not by direct `npcfaction_id`.
- If no active faction exists in that slot, Java returns without mutation.
- On success, Java sets faction time to `getNextTime()`, sets state to `ENpcFactionQuestState.COMPLETE`, and updates the slot `timeLimit`.
- `NpcFactions.getNextTime()` uses `ServerTime.now()` and chooses today at 09:00 only when current server hour is less than 9; exactly 09:00 advances to tomorrow.
- Mentor quests additionally set mentor flag time and send title packets; those side effects remain out of scope.

## Implementation

- Added `NpcFactionDailyResetService.GetNextResetEpochSeconds`.
- Added `PlayerNpcFactionsSnapshot.CompleteActiveQuest`.
- Added `PlayerNpcFactionCompletionStatus` and `PlayerNpcFactionCompletionResult`.
- Added `TryGetFaction` and a read-only `Factions` projection for staged helper/test visibility.
- The helper returns a new immutable snapshot instead of mutating existing faction objects in place.

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter FullyQualifiedName~PlayerNpcFactionsSnapshotTests` | Passed, 7 tests |
| `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` | Passed, 1740 tests |

## Migration Parity Table - UOW-1013

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.gameobjects.player.npcFaction.NpcFactions.completeQuest` | `Aion.GameServer.Model.GameObjects.PlayerNpcFactionsSnapshot.CompleteActiveQuest` | Player State / Service Helper | Partial | Unit Tested | Partial Parity | Stages active-slot completion, state change to complete, time update, and slot cooldown behavior. Not wired to `QuestFinishStateMutationService` or production quest completion. Mentor flag/title packets, DAO writes, and daily assignment remain unported. |
| `com.aionemu.gameserver.model.gameobjects.player.npcFaction.NpcFactions.getNextTime` | `Aion.GameServer.Services.NpcFactionDailyResetService.GetNextResetEpochSeconds` | Utility / Time Reset | Partial | Unit Tested | Partial Parity | Implements server-time 09:00 daily reset and Java's `hour >= 9` tomorrow boundary. DST, invalid timezone, and Java runtime comparison remain unverified. |
| `com.aionemu.gameserver.model.gameobjects.player.npcFaction.NpcFaction` | `Aion.GameServer.Model.GameObjects.PlayerNpcFactionState` | DTO / Player State | Partial | Unit Tested | Partial Parity | C# immutable record returns an updated copy; Java mutates `NpcFaction` and marks persistent state update-required. C# persistence state tracking and writes remain missing. |
| `com.aionemu.gameserver.model.gameobjects.player.npcFaction.ENpcFactionQuestState` | `Aion.GameServer.Model.GameObjects.PlayerNpcFactionQuestState` | Enum | Partial | Unit Tested | Needs Verification | C# preserves the Java typo as `Noting` and now consumes `Complete` in the staged completion helper. Serialization to Java DB names is read-only today; write parity remains unverified. |
| `com.aionemu.gameserver.dao.PlayerNpcFactionsDAO.storeNpcFactions` | Future C# NPC faction repository write path | Repository | Not Started | Manual Only | Needs Verification | Java writes active/time/state/quest_id when persistent state is new or update-required. C# has read hydration only. |

## Tests Added

| Test Name | Type | What It Validates | Java Comparison |
|---|---|---|---|
| `PlayerNpcFactionsSnapshotTests.CompleteActiveQuest_MatchesJavaNpcFactionCompletionForActiveSlot` | Unit | Active daily slot becomes complete, time updates, quest id remains, cooldown blocks until after reset. | Source-reviewed Java `NpcFactions.completeQuest`. |
| `PlayerNpcFactionsSnapshotTests.CompleteActiveQuest_UsesMentorSlotLikeJavaQuestTemplate` | Unit | Mentor completion updates the mentor slot and leaves the normal slot unchanged. | Source-reviewed Java `questTemplate.isMentor()` slot selection. |
| `PlayerNpcFactionsSnapshotTests.CompleteActiveQuest_ReturnsNoActiveFactionWhenSlotIsEmpty` | Unit | Empty active slot returns no-op status. | Source-reviewed Java null return. |
| `PlayerNpcFactionsSnapshotTests.NpcFactionDailyResetService_AppliesJavaNineAmBoundary` | Unit | 08:59 resets today; 09:00 and 10:00 reset tomorrow. | Source-reviewed Java `now.getHour() >= 9` behavior. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- The staged helper is not called from `QuestFinishStateMutationService` or any production quest-finish path.
- C# immutable snapshots differ from Java in-place mutation; production owner assignment semantics still need verification.
- Mentor flag time and `SM_TITLE_INFO` packet fanout are not ported.
- `PlayerNpcFactionsDAO.storeNpcFactions` write behavior, persistent-state flags, and transaction ordering are not ported.
- Daily quest assignment, random selection, and `SM_QUEST_ACTION` assignment packets remain unported.

## Summary Metrics

- Total Java artifacts discovered: 5
- Total artifacts ported: 3 staged partial artifacts in this unit
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 5
- Total blocked artifacts: 5 blocked/not-started categories
- Estimated overall migration completion: Phase 6 remains about 71%

## Next Recommended Unit Of Work

Audit packet and persistence ordering for Java quest completion: `SM_QUEST_ACTION`, daily/weekly reset messages, `QuestEngine.onQuestCompleted`, `PlayerQuestListDAO.store`, and `PlayerNpcFactionsDAO.storeNpcFactions`. Keep production quest completion, DAO writes, live nearby refresh, and ItemPurification dispatch disabled.

## Next Unit Handoff

Start with this file, `docs/Phase-6TD-Completion.md`, `docs/QuestRepeatDate-Audit.md`, and `docs/PHASE-6-PROGRESS.md` Session 1013.

Recommended next slice:

1. Source-audit Java packet and persistence ordering around `QuestService.finishQuest`.
2. Document exactly when quest state, NPC faction state, packets, callbacks, and nearby refresh occur.
3. Decide the next staged helper boundary without wiring live sends.
4. Add/update the Migration Parity Table for every Java artifact touched.
5. Run focused tests and the full `Aion.GameServer.Tests` project before committing.
