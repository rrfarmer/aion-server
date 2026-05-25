# Phase 6TO Completion - UOW-1023 NPC-Faction Persistence Operation Plan

## Scope

UOW-1023 adds a pure, non-live NPC-faction persistence operation planner for Java `PlayerNpcFactionsDAO.storeNpcFactions`.

This unit does not write to the database, change logout persistence, compose persistence plans into quest finish, or alter Java's one-connection-per-row persistence behavior.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | NPC-faction persistence plan | `PlayerNpcFactionsDAO.storeNpcFactions`, `NpcFaction`, `NpcFactions`, `Persistable` | new service + tests | Service Port | Selected | Medium | Direct sibling to UOW-1022; isolated files and no live DB writes. |
| B | Compose quest and NPC-faction persistence plans | `QuestService.finishQuest`, `PlayerService.storePlayer` | operation plan + tests | Service Composition | Yes later | Medium | Safer after both persistence planners exist. |
| C | Callback dispatch planning | `QuestEngine.onQuestCompleted`, handler loader | new service/docs + tests | Service Port | Yes later | Medium | Separate from persistence files, but needs handler-order decisions. |
| D | Live DAO write implementation | `PlayerQuestListDAO`, `PlayerNpcFactionsDAO` equivalents | repository code/tests | Repository Port | No | High | Requires operation-plan composition and failure-ordering policy first. |

Selected batch: A only. File ownership was new NPC-faction persistence service/test files plus Orchestrator-owned docs.

## Java Breadcrumbs

- `PlayerNpcFactionsDAO.storeNpcFactions` iterates `player.getNpcFactions().getNpcFactions()`, backed by `HashMap.values()`.
- `PersistentState.NEW` calls `insertNpcFaction`.
- `PersistentState.UPDATE_REQUIRED` calls `updateNpcFaction`.
- `UPDATED`, `DELETED`, and `NOACTION` are ignored.
- Each insert/update opens its own connection, executes one statement with default auto-commit, catches/logs exceptions, and does not mark the faction `UPDATED` after success.
- Persisted payload is `active`, `time`, `state`, and `quest_id` with `player_id`/`faction_id` keys.

## Deliverables

- Added `Aion.GameServer.Services.NpcFactionPersistencePlanService`.
- Added explicit Java-shaped `NpcFactionPersistenceState` values for planning without requiring C# snapshots to own Java `PersistentState`.
- Added insert/update operation descriptors with Java source breadcrumbs.
- Added focused tests for state filters, payload retention, caller-order preservation, and no-op states.
- Updated persistence audit, quest-finish ordering, nearby audit, and Phase 6 progress docs.

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter NpcFactionPersistencePlanServiceTests --nologo` | Passed: 4 |
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` | Passed: 1763 |

## Migration Parity Table - UOW-1023

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.dao.PlayerNpcFactionsDAO.storeNpcFactions` | `Aion.GameServer.Services.NpcFactionPersistencePlanService.CreatePlan` | Repository Plan / Service | Partial | Unit Tested | Partial Parity | C# plans Java insert/update filters with non-live descriptors and preserves caller order for Java `HashMap.values()` risk. It does not execute SQL, open per-row connections, catch/log DAO errors, or compare against Java runtime. |
| `com.aionemu.gameserver.dao.PlayerNpcFactionsDAO.insertNpcFaction` | `Aion.GameServer.Services.NpcFactionPersistenceOperationDescriptor` | Repository Plan / Insert Descriptor | Partial | Unit Tested | Partial Parity | C# carries faction id, active, mentor flag, time, state, and quest id. Java also binds player id externally and writes with default auto-commit; C# has no live write. |
| `com.aionemu.gameserver.dao.PlayerNpcFactionsDAO.updateNpcFaction` | `Aion.GameServer.Services.NpcFactionPersistenceOperationDescriptor` | Repository Plan / Update Descriptor | Partial | Unit Tested | Partial Parity | C# carries update payload fields and Java source breadcrumb. No SQL write, row-count check, exception logging, or Java runtime comparison exists. |
| `com.aionemu.gameserver.model.gameobjects.player.npcFaction.NpcFactions` | `Aion.GameServer.Model.GameObjects.PlayerNpcFactionsSnapshot`; `NpcFactionPersistencePlanService` | Model / Persistence Collection Plan | Partial | Unit Tested | Needs Verification | C# snapshot stores factions in a dictionary and planner preserves caller order, but live `HashMap` value ordering, owner side effects, and synchronized mutations are not modeled. |
| `com.aionemu.gameserver.model.gameobjects.player.npcFaction.NpcFaction` | `Aion.GameServer.Model.GameObjects.PlayerNpcFactionState`; `NpcFactionPersistenceStateEntry` | Model / NPC Faction Row Plan | Partial | Unit Tested | Partial Parity | C# represents persisted row fields plus mentor flag. Java constructor static-data lookup and mutator-driven `PersistentState` transitions are not fully modeled. |
| `com.aionemu.gameserver.model.gameobjects.Persistable` | `Aion.GameServer.Services.NpcFactionPersistenceState` | Interface / Persistence State Projection | Partial | Unit Tested | Needs Verification | C# enum projects Java states for planning only. It is not attached to live faction snapshots and does not implement Java transition semantics or no-reset-after-write behavior beyond documentation. |

## Tests Added

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `CreatePlan_EmitsInsertAndUpdateOperationsLikeJavaNpcFactionDao` | `NEW` rows become inserts, `UPDATE_REQUIRED` rows become updates, and ignored states emit no operation. | Source-reviewed `PlayerNpcFactionsDAO.storeNpcFactions`. |
| `CreatePlan_PreservesFactionPayloadForInsertAndUpdate` | Active flag, mentor flag, time epoch seconds, state, quest id, and object identity survive descriptor planning. | Source-reviewed insert/update SQL bind fields. |
| `CreatePlan_PreservesCallerOrderBecauseJavaUsesHashMapValues` | Planner does not sort descriptors, preserving caller-provided order for Java `HashMap.values()` risk. | Source-reviewed `NpcFactions.getNpcFactions`. |
| `CreatePlan_ReturnsNoChangesForIgnoredJavaPersistentStates` | `UPDATED`, `DELETED`, and `NOACTION` rows emit no descriptors. | Source-reviewed Java switch cases. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- No live database writes exist for NPC-faction persistence.
- Java opens one connection per row with default auto-commit; C# planner is descriptor-only.
- Java does not mark NPC-faction rows `UPDATED` after successful insert/update; C# planner does not mutate input state.
- Java `HashMap.values()` ordering remains unverified and caller-provided in C#.
- Quest/NPC-faction persistence planners are not yet composed into quest-finish operation planning.

## Summary Metrics

- Total Java artifacts discovered: 6 in this unit
- Total artifacts ported: 1 partial non-live NPC-faction persistence plan plus descriptor/state DTOs
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 6
- Total blocked artifacts: 5 blocked/partial categories: live NPC-faction writes, per-row connection behavior, input state mutation/no-reset behavior, `HashMap` ordering, and Java runtime comparison
- Estimated overall migration completion: Phase 6 remains about 71%

## Next Recommended Unit Of Work

Compose the non-live quest and NPC-faction persistence plans into `QuestFinishOperationPlanService` as detailed persistence descriptors, while keeping live repository writes disabled.

## Next Unit Handoff

Start with this file, `docs/QuestPersistenceContract-Audit.md`, and `docs/PHASE-6-PROGRESS.md` Session 1023.

Recommended next slice:

1. Extend quest-finish operation planning to optionally accept quest and NPC-faction persistence projections.
2. Keep descriptor positions aligned with Java: quest update packet, callback, optional NPC-faction completion, nearby refresh, then deferred persistence descriptors.
3. Preserve non-live status and Java source breadcrumbs.
4. Add tests proving persistence descriptor composition does not move reward, state-mutation, packet, callback, NPC-faction, or nearby-refresh ordering.
5. Keep live DAO writes disabled.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Compose persistence planners into quest finish | `QuestFinishOperationPlanService` + tests | Medium | Best next implementation slice. |
| B | Callback dispatch plan | new callback service + tests/docs | Medium | Safe if it avoids operation-plan files. |
| C | Reward XML projection planning | new docs-only/static-data audit | Medium | Safe separately if it avoids operation-plan files. |

## Do Not Parallelize

- Live DAO write implementation.
- Quest-finish operation plan edits from multiple workers.
- Phase progress/completion docs.
