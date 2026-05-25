# Phase 6TM Completion - UOW-1021 Quest Persistence Contract Audit

## Scope

UOW-1021 audits Java quest and NPC-faction persistence contracts for future C# write planning.

No C# runtime code changed in this unit.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Quest/NPC-faction persistence audit | `PlayerService.storePlayer`, `PlayerQuestListDAO`, `PlayerNpcFactionsDAO` | new docs-only audit | Java Analysis | Selected | Medium | Direct handoff from UOW-1020; shared phase docs make this Orchestrator-owned. |
| B | Non-live callback dispatch plan | `QuestEngine.onQuestCompleted` | future service/tests | Service Port | Not in this batch | Medium | Should wait until persistence docs are not being edited. |
| C | Reward XML projection planning | `QuestTemplate`, `Rewards`, `QuestWorkItems` | docs/dataholders | Java Analysis | Yes | Medium | Safe separately if it avoids operation-plan and progress docs. |
| D | Live DAO write implementation | DAO services/repository code | `PlayerEnterWorldRepository`, new tests | Repository Port | No | High | Requires this audit and explicit failure-ordering decision. |

Selected batch: A, documentation-only.

## Java Breadcrumbs

- `PlayerLeaveWorldService.leaveWorld` calls `QuestEngine.onLogOut`, captures last-online, removes the player from the world, then calls `PlayerService.storePlayer`.
- `PlayerService.storePlayer` saves quest state before inventory and NPC factions after cooldowns/mailbox.
- `PlayerQuestListDAO.store` uses one connection with `autoCommit(false)` but each delete/insert/update helper commits independently.
- Quest helper-level SQL errors are caught and logged without rollback and without rethrow.
- Current quest states are marked `UPDATED` after the store attempt regardless of helper failures.
- `PlayerNpcFactionsDAO.storeNpcFactions` opens one connection per insert/update row and does not reset faction persistent state after success.

## Deliverables

- Added `docs/QuestPersistenceContract-Audit.md`.
- Updated quest-finish ordering, nearby audit, and Phase 6 progress docs.
- Updated the next handoff toward non-live quest/NPC-faction persistence plans.

## Validation

| Command | Result |
|---|---|
| Manual source audit of Java files listed in `docs/QuestPersistenceContract-Audit.md` | Completed |

## Migration Parity Table - UOW-1021

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.player.PlayerLeaveWorldService.leaveWorld` | `Aion.GameServer.Services.PlayerEnterWorldService.LeaveWorldAsync`; `Aion.GameServer.Data.PlayerEnterWorldRepository.SavePlayerLogoutAsync` | Service / Logout Persistence Orchestration | Partial | Manual Only | Needs Verification | Source-audited logout save ordering. C# has logout save support for core player/cooldowns/settings, but no quest or NPC-faction write path. Java thread/task cleanup, world deletion, chat notification, and final online marker ordering are broader than this unit. |
| `com.aionemu.gameserver.services.player.PlayerService.storePlayer` | `Aion.GameServer.Data.PlayerEnterWorldRepository.SavePlayerLogoutAsync` | Service / Persistence Orchestrator | Partial | Manual Only | Needs Verification | Java stores quests before inventory and NPC factions after cooldowns/mailbox. C# currently omits quest/NPC-faction writes and has different transaction grouping for implemented slices. |
| `com.aionemu.gameserver.dao.PlayerQuestListDAO` | Future C# quest persistence plan/repository | Repository | Not Started | Manual Only | Needs Verification | Source-audited delete/insert/update order, independently committed helper batches, nullable reward/time fields, and post-attempt `UPDATED` marking. No C# write path exists. |
| `com.aionemu.gameserver.dao.PlayerNpcFactionsDAO` | Future C# NPC-faction persistence plan/repository | Repository | Not Started | Manual Only | Needs Verification | Source-audited insert/update-only behavior and one-connection-per-row writes. No C# write path exists. |
| `com.aionemu.gameserver.model.gameobjects.player.QuestStateList` | `Aion.GameServer.Model.GameObjects.PlayerQuestState` plus future quest-state collection | Model / Persistence State | Partial | Manual Only | Needs Verification | C# has immutable quest rows but no Java `TreeMap` state list, deleted-id set, or persistent-state tracking. Load ordering is normalized by SQL `ORDER BY`; Java uses TreeMap after load. |
| `com.aionemu.gameserver.questEngine.model.QuestState` | `Aion.GameServer.Model.GameObjects.PlayerQuestState` | Model / Quest State | Partial | Unit Tested in prior units | Partial Parity | C# represents quest id/status/vars/flags/count/reward/repeat/complete time. Java `PersistentState`, mutator side effects, `setNextRepeatTime` no-state-change behavior, and constructor-time current timestamp behavior are not fully modeled. |
| `com.aionemu.gameserver.model.gameobjects.player.npcFaction.NpcFactions` | `Aion.GameServer.Model.GameObjects.PlayerNpcFactionsSnapshot` | Model / NPC Faction State | Partial | Unit Tested in prior units | Partial Parity | C# stages active-slot and completion behavior, but no Java `PersistentState`, HashMap write-order behavior, mentor title side effects, or persistence writes. |
| `com.aionemu.gameserver.model.gameobjects.player.npcFaction.NpcFaction` | `Aion.GameServer.Model.GameObjects.PlayerNpcFactionState` | Model / NPC Faction Row | Partial | Unit Tested in prior units | Needs Verification | C# row stores id/active/mentor/time/state/quest id. Java constructor and setters use `PersistentState`, and setter behavior differs for `NEW` rows. |
| `com.aionemu.gameserver.model.gameobjects.Persistable` | Future C# persistence-state model | Interface / Persistence State | Not Started | Manual Only | Needs Verification | Java `NEW`, `UPDATE_REQUIRED`, `UPDATED`, `DELETED`, `NOACTION` behavior drives DAO filters. C# snapshots do not model this yet. |

## Tests Added

No tests were added in this read-only audit unit.

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- Quest persistence failure behavior is unusual: helper-level errors do not rollback and current states are marked updated after the attempt.
- NPC faction persistence does not clear persistent state after successful insert/update.
- Java `HashSet`/`HashMap` ordering affects delete and NPC faction write ordering.
- C# lacks quest/NPC-faction write repositories and persistent-state tracking.
- Any safer C# transaction design would be an intentional difference that needs explicit tests and documentation.

## Summary Metrics

- Total Java artifacts discovered: 9 in this unit
- Total artifacts ported: 0 in this unit
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 9
- Total blocked artifacts: 5 blocked/not-started categories: quest write plan, NPC-faction write plan, persistent-state model, failure-ordering decision, and Java runtime comparison
- Estimated overall migration completion: Phase 6 remains about 71%

## Next Recommended Unit Of Work

Add a non-live quest persistence operation plan for `PlayerQuestListDAO.store`: classify deleted/current quest states into delete, insert, and update descriptors in Java order, including nullable reward/time fields and conservative failure-ordering notes. Do not write to the database yet.

## Next Unit Handoff

Start with this file, `docs/QuestPersistenceContract-Audit.md`, `docs/QuestFinishOrdering-Audit.md`, and `docs/PHASE-6-PROGRESS.md` Session 1021.

Recommended next slice:

1. Add a pure quest persistence plan service and DTOs for delete/insert/update descriptors.
2. Use explicit operation status inputs instead of assuming Java `PersistentState` exists on current C# quest snapshots.
3. Preserve Java operation order: deletes, inserts, updates.
4. Add focused tests for ordering, nullable reward/next-repeat/complete-time fields, and newly-created-then-deleted risk notes.
5. Keep live repository writes disabled.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Quest persistence plan | new service + tests | Medium | Best next implementation slice; sequential if operation plan is touched. |
| B | NPC-faction persistence plan | new service + tests | Medium | Can be parallel only if separate files and no shared docs until integration. |
| C | Reward XML projection planning | new docs-only/static-data audit | Medium | Safe separately if it avoids operation-plan files. |

## Do Not Parallelize

- Live DAO write implementation.
- Quest-finish operation plan persistence descriptor redesign with quest/NPC-faction plans.
- Phase progress/completion docs.
