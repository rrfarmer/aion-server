# Phase 6 Session 2578 Completion

## UOW

[Phase 6] UOW-2578: Persist abandoned quest state

## Status

Completed and validated with the focused quest-abandon/player-enter-world service filter. The live `CM_DELETE_QUEST`
path now persists Java-equivalent `player_quests` delete/update mutations after `QuestAbandonService.Abandon` mutates
the loaded `Player.Quests` collection.

## Runtime Progress Gate

```text
Runtime progress gate:
- Deferred/live behavior being advanced: QuestService.abandonQuest quest-list mutation -> PlayerQuestListDAO.store delete/update of player_quests.
- Java source method or runtime path: QuestService.abandonQuest, QuestStateList.deleteQuest, PlayerService.storePlayer, PlayerQuestListDAO.store/deleteQuest/updateQuests.
- C# runtime artifact wired or fixed: PlayerEnterWorldRepository quest delete/update SQL, PlayerEnterWorldService.PersistQuestAbandonAsync, GameServerConnection.HandleDeleteQuestAsync.
- Client-visible/state/persistence effect changed: abandoning a first-time quest now deletes the player_quests row; abandoning a repeat-completed quest now updates the row back to COMPLETE with zero vars/flags.
- Why this is not preview-only/test-only/documentation-only: the live CM_DELETE_QUEST handler now writes the existing player_quests persistence shape for real quest abandon mutations.
```

## Java Source Reviewed

- `QuestService.abandonQuest(Player, int)`:
  - deletes first-time quest states through `player.getQuestStateList().deleteQuest(questId)`;
  - resets repeat-completed quest states to `COMPLETE`, `questVar = 0`, and `flags = 0`.
- `QuestStateList.deleteQuest(int)`:
  - removes the quest from the active quest map;
  - marks the state deleted and tracks the deleted quest id.
- `PlayerQuestListDAO.store(Player)`:
  - deletes tracked deleted quest ids from `player_quests`;
  - updates changed quest states with `status`, `quest_vars`, `flags`, `complete_count`, `next_repeat_time`, `reward`, and `complete_time`.

## C# Changes

- Extended `IPlayerEnterWorldRepository` with `DeletePlayerQuestAsync` and `UpdatePlayerQuestAsync`.
- Added MySQL `player_quests` delete/update implementations matching Java `PlayerQuestListDAO` column shape.
- Added `PlayerEnterWorldService.PersistQuestAbandonAsync`, mapping `QuestAbandonStatus.Deleted` to delete and `ResetToComplete` to update.
- Wired `GameServerConnection.HandleDeleteQuestAsync` to call quest-abandon persistence from the live `CM_DELETE_QUEST` path before sending abandon side-effect packets.

## Known Gaps

- Java persists quest-list dirty state during player save/logout; this C# UOW persists immediately from the live handler because no general quest dirty-state save pipeline is currently wired.
- Quest timer task-map cancellation remains missing; timer-clear packet output is live.
- Java `NpcFactions.sendDailyQuest()` after NPC-faction abort remains missing.
- Java `QuestEngine.onItemRemoved` callback from storage deletion remains missing.

## Validation Decision

```text
Validation decision:
- Changed surface: production player_quests repository writes, PlayerEnterWorldService live mutation persistence, CM_DELETE_QUEST persistence wiring, focused tests.
- Specific behavior/contract: first-time abandon persists a DELETE for the original quest id; repeat-completed abandon persists an UPDATE with COMPLETE status, zero vars, zero flags, and preserved repeat metadata.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~QuestAbandonServiceTests" --no-restore -> 73/73 passed.
- Focused Java/Maven command: none; no narrow Java fixture exists, and behavior was taken from reviewed Java source.
- Broad-validation trigger: live quest persistence and CM_DELETE_QUEST handler changed.
- Broad .NET decision: skipped after focused pass; the filtered command built Aion.GameServer and directly covered the abandon mutation and persistence service contract.
- Why this scope is sufficient: tests prove the two Java abandon persistence outcomes over the same QuestAbandonService result used by the live handler, while the repository SQL follows the reviewed PlayerQuestListDAO schema.
```

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `QuestService.abandonQuest` quest-state branch | `QuestAbandonService.Abandon` / `PlayerEnterWorldService.PersistQuestAbandonAsync` | Service/state/persistence | Partial | Unit Tested | Partial Parity | First-time delete and repeat reset now persist; timing differs from Java save-time persistence. |
| `PlayerQuestListDAO.deleteQuest` | `MySqlPlayerEnterWorldRepository.DeletePlayerQuestAsync` | Persistence | Partial | Unit Tested via service fake | Partial Parity | Uses existing `player_quests` table shape; no live DB integration test was run. |
| `PlayerQuestListDAO.updateQuests` | `MySqlPlayerEnterWorldRepository.UpdatePlayerQuestAsync` | Persistence | Partial | Unit Tested via service fake | Partial Parity | Updates Java columns for reset-to-complete abandon states; no insert path added. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `PersistQuestAbandon_DeletesFirstTimeQuestRow` | Unit | `QuestStateList.deleteQuest` + `PlayerQuestListDAO.deleteQuest` | First-time abandon deletes loaded quest state and calls repository delete for the original quest id | C# uses the real `QuestAbandonService.Abandon` result | No MySQL fixture. |
| `PersistQuestAbandon_UpdatesRepeatedQuestRowToComplete` | Unit | `QuestService.abandonQuest` repeat reset + `PlayerQuestListDAO.updateQuests` | Repeat-completed abandon persists `COMPLETE`, `quest_vars = 0`, `flags = 0`, and preserved repeat metadata | C# uses the real `QuestAbandonService.Abandon` result | No MySQL fixture. |

## Summary Metrics

- Focused validation: 73 tests passed.
- Live abandon path now mutates quest state, NPC-faction state, quest work-item inventory state, work-order recipe state, and quest persistence state.
- Overall Phase 6 completion estimate: incremental.

## Remaining Risks

- Full real-client abandon flow has not been run.
- No database integration test was added for `player_quests`; SQL was matched by source review against Java DAO shape.
- Immediate C# persistence is a practical partial-parity step until a Java-like dirty quest-list save pipeline exists.

## Next Runtime Candidates

1. UOW-2579: Wire live quest timer task cancellation only if discovery finds or implements a real C# quest timer scheduler/task-owner path. Do not do this as a packet-only or evidence-only UOW.
2. Persist NPC-faction abort state from live quest abandon through the existing `player_npc_factions` shape, if repository write support can be scoped narrowly.
3. Wire Java `NpcFactions.sendDailyQuest()` after abort if C# can select and send the replacement daily quest packet from live static/runtime data.
