# Phase 6 Session 2581 Completion

## UOW

[Phase 6] UOW-2581: Assign random NPC-faction daily quest after abandon

## Status

Completed and validated with a focused quest-abandon/NPC-faction/static-template/start-condition/persistence filter.
The live `CM_DELETE_QUEST` path can now execute Java's `NpcFactions.sendDailyQuest()` random replacement branch after
an NPC-faction quest abort: when no reusable assigned daily quest remains, C# selects an eligible non-time-based
faction quest, assigns it to the active faction with Java's next-reset time, sends `SM_QUEST_ACTION(int questId)` /
action id 6, and persists the final `player_npc_factions` row.

## Runtime Progress Gate

```text
Runtime progress gate:
- Deferred/live behavior being advanced: NpcFactions.sendDailyQuest questId == 0 branch after abort.
- Java source method or runtime path: NpcFactions.sendDailyQuest, QuestsData.afterUnmarshal, QuestsData.getQuestsByNpcFaction, QuestEngine.isHaveHandler, QuestService.checkStartConditions, Rnd.get.
- C# runtime artifact wired or fixed: NearbyQuestTemplateTable NPC-faction index, PlayerNpcFactionsSnapshot.AssignDailyQuest, QuestAbandonService daily quest selection, GameServerConnection.HandleDeleteQuestAsync reset-time wiring, PlayerEnterWorldService.PersistQuestAbandonAsync final faction-state persistence.
- Client-visible/state/persistence effect changed: abandoning an NPC-faction quest can now assign a new eligible daily quest, send action id 6 for that quest, and update quest_id/time/state in player_npc_factions.
- Why this is not preview-only/test-only/documentation-only: live CM_DELETE_QUEST now mutates NPC-faction assignment state, emits a real server packet, and persists the updated row.
```

## Java Source Reviewed

- `NpcFactions.sendDailyQuest()`:
  - loops normal then mentor active slots;
  - skips blocked time-limit, active `START`, and future `COMPLETE`;
  - reuses future `NOTING` assignment;
  - when `questId == 0`, calls `DataManager.QUEST_DATA.getQuestsByNpcFaction`, chooses `Rnd.get(quests)`, sets quest id/time/state, and sends `SM_QUEST_ACTION`.
- `QuestsData.afterUnmarshal()`:
  - indexes only `npcfaction_id != 0` and `!quest.isTimeBased()` templates by faction id in load order.
- `QuestsData.getQuestsByNpcFaction()`:
  - filters indexed templates through `QuestEngine.isHaveHandler(questId)` and `QuestService.checkStartConditions(player, questId, false)`.
- `QuestService.checkStartConditions()`:
  - for NPC-faction quests, requires the faction start timer to have passed and the exact faction row to be active.
- `NpcFactions.getNextTime()`:
  - resets at server-time 09:00, matching the existing `NpcFactionDailyResetService`.

## C# Changes

- Added a non-time-based NPC-faction index to `NearbyQuestTemplateTable`.
- Added `PlayerNpcFactionsSnapshot.AssignDailyQuest` for Java's random branch assignment mutation.
- Extended `QuestAbandonService.Abandon` with runtime quest-table/reset-time/random/handler-filter inputs and a two-slot Java-style daily quest loop.
- `GameServerConnection.HandleDeleteQuestAsync` now passes current epoch, static quest templates, and Java-style next reset time into abandon handling.
- `QuestAbandonResult` now carries final NPC-faction persistence updates; `PlayerEnterWorldService.PersistQuestAbandonAsync` persists each final row.

## Known Gaps

- The live call currently has no real C# `QuestEngine.isHaveHandler` registry to pass as the handler predicate, so the default runtime path treats static candidates as handler-available. The predicate is covered by tests but must be wired to a live handler availability table before parity can be marked beyond partial.
- Java `Rnd.get` randomness is represented by `Random.Shared.Next` with deterministic injection only for tests; no Java RNG sequence comparison was performed.
- Quest timer task-map cancellation remains missing; timer-clear packet output is live.
- Java `QuestEngine.onItemRemoved` callback from storage deletion remains missing.

## Validation Decision

```text
Validation decision:
- Changed surface: production static-data indexing, live quest abandon packet/state output, NPC-faction state mutation, persistence, focused tests.
- Specific behavior/contract: expired/zero assigned NPC-faction abort selects an eligible non-time-based faction quest, updates player faction quest_id/time/state, persists the final row, and emits Java action id 6 packet bytes.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~QuestAbandonServiceTests|FullyQualifiedName~PlayerNpcFactionsSnapshotTests|FullyQualifiedName~NearbyQuestTemplateTableTests|FullyQualifiedName~NearbyQuestStartConditionServiceTests|FullyQualifiedName~PlayerEnterWorldServiceTests" --no-restore -> 106/106 passed.
- Focused Java/Maven command: none; no narrow Java fixture exists, and behavior was taken from reviewed Java source.
- Broad-validation trigger: live packet send, state mutation, persistence, and static-data indexing changed.
- Broad .NET decision: skipped after focused pass; the filtered command built Aion.GameServer and directly covered the edited static table, state mutation, packet output, start-condition filter, abandon service, and persistence path.
- Why this scope is sufficient: tests assert faction index filtering, random assignment mutation, action id 6 bytes, handler-predicate filtering, and final faction-row persistence.
```

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `NpcFactions.sendDailyQuest` random branch | `QuestAbandonService.Abandon` / `PlayerNpcFactionsSnapshot.AssignDailyQuest` | State/packet selection | Partial | Unit Tested | Partial Parity | Random assignment, packet send, and persistence are live; full parity still needs live `QuestEngine.isHaveHandler` wiring. |
| `QuestsData.afterUnmarshal` NPC-faction index | `NearbyQuestTemplateTable.GetQuestsByNpcFaction` | Runtime static-data index | Partial | Unit Tested | Partial Parity | Non-time-based faction indexing and load order are covered; handler/start-condition filtering remains outside the index like Java. |
| `QuestsData.getQuestsByNpcFaction` | `QuestAbandonService` candidate filter | Service | Partial | Unit Tested | Partial Parity | Applies start conditions and supports handler predicate; live handler availability currently defaults to available until a registry is wired. |
| `NpcFactions.getNextTime` | `NpcFactionDailyResetService.GetNextResetEpochSeconds` | Utility | Partial | Unit Tested | Partial Parity | Existing 09:00 boundary service is now wired into live abandon random assignment. |
| `PlayerNpcFactionsDAO.updateNpcFaction` after random assignment | `PlayerEnterWorldService.PersistQuestAbandonAsync` | Persistence | Partial | Unit Tested | Partial Parity | Persists final assigned faction row instead of only the intermediate abort row. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GetQuestsByNpcFaction_MatchesJavaNonTimeBasedFactionIndex` | Unit | `QuestsData.afterUnmarshal` | Faction quest index excludes time-based and preserves load order | Source-reviewed Java + unit assertion | Does not test real XML load counts. |
| `Abandon_NpcFactionQuestRandomlyAssignsEligibleDailyQuestLikeJava` | Unit | `NpcFactions.sendDailyQuest`, `QuestsData.getQuestsByNpcFaction`, `SM_QUEST_ACTION(int)` | Random branch assigns quest/time/state and sends action id 6 bytes | Source-reviewed Java + exact C# packet bytes | Uses deterministic random injection. |
| `Abandon_NpcFactionQuestRandomBranchHonorsHandlerAvailabilityFilter` | Unit | `QuestEngine.isHaveHandler` | Candidate list excludes quests without handler predicate support | Source-reviewed Java filter | Predicate not yet wired to live registry. |
| `PersistQuestAbandon_UpdatesRandomNpcFactionDailyAssignmentRow` | Unit | `PlayerNpcFactionsDAO.updateNpcFaction` after `NpcFactions.sendDailyQuest` mutation | Persistence writes final assigned quest id/time/state | Source-reviewed Java + repository capture | Immediate persistence remains a C# incremental step versus Java dirty-list save timing. |

## Summary Metrics

- Focused validation: 106 tests passed.
- Runtime progress: live abandon path now supports random NPC-faction daily replacement with packet send and persistence.
- Overall Phase 6 completion estimate: incremental.

## Remaining Risks

- Full real-client abandon flow has not been run.
- Live handler availability still needs a C# runtime source before random daily selection can match `QuestEngine.isHaveHandler`.
- No Java RNG sequence comparison was performed; parity target is branch behavior and candidate filtering, not deterministic RNG output.

## Next Runtime Candidate

UOW-2582: Wire live quest-handler availability into NPC-faction random daily selection.

Runtime progress gate:

```text
- Deferred/live behavior being advanced: QuestsData.getQuestsByNpcFaction filters random daily candidates through QuestEngine.isHaveHandler before assignment.
- Java source method or runtime path: QuestEngine.addQuestHandler, QuestEngine.isHaveHandler, QuestsData.getQuestsByNpcFaction, data/handlers/quest AbstractQuestHandler constructors/register calls.
- C# runtime artifact to wire or fix: runtime quest-handler availability table/source, GameServerRuntimeContext or StaticData exposure, GameServerConnection.HandleDeleteQuestAsync handler predicate passed into QuestAbandonService.
- Client-visible/state/persistence effect expected: NPC-faction random daily abandon selection will stop assigning/sending/persisting quests that have no loaded handler equivalent.
- Why this is not preview-only/test-only/documentation-only: the handler availability data will be consumed by the live abandon selector and will directly change whether packets/state/persistence occur.
```

Risks for UOW-2582:

- Existing quest handler extractor artifacts mainly cover quest-start registrations, not every registered quest handler id.
- Need to avoid making handler analysis only; the table must be loaded into runtime and used by the live selector in the same UOW.
- If no reliable handler-id source exists, document the blocker and choose another live runtime UOW.
