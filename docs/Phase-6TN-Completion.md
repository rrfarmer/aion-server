# Phase 6TN Completion - UOW-1022 Quest Persistence Operation Plan

## Scope

UOW-1022 adds a pure, non-live quest persistence operation planner for Java `PlayerQuestListDAO.store`.

This unit does not write to the database, change logout persistence, compose persistence plans into quest finish, or alter transaction behavior.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Quest persistence plan | `PlayerQuestListDAO.store`, `QuestStateList`, `QuestState`, `Persistable` | new service + tests | Service Port | Selected | Medium | Direct handoff from UOW-1021; isolated files and no live DB writes. |
| B | NPC-faction persistence plan | `PlayerNpcFactionsDAO`, `NpcFaction`, `NpcFactions` | new service + tests | Service Port | Yes later | Medium | Separate files, but shared docs should be updated by Orchestrator after one unit. |
| C | Reward XML projection planning | `QuestTemplate`, `Rewards`, `QuestWorkItems` | docs/dataholders | Java Analysis | Yes | Medium | Safe separately if it avoids operation-plan files. |
| D | Live DAO write implementation | `PlayerQuestListDAO` equivalent | repository code/tests | Repository Port | No | High | Requires operation plans and failure-ordering decision first. |

Selected batch: A only. File ownership was new quest persistence service/test files plus Orchestrator-owned docs.

## Java Breadcrumbs

- `PlayerQuestListDAO.store` processes deletes, then inserts, then updates.
- Current quest states come from `QuestStateList.getAllQuestState`, backed by a `TreeMap` sorted by quest id.
- Deleted quest ids come from a `HashSet`, so their iteration order is not stable.
- Insert/update payloads carry status, quest vars, flags, complete count, nullable next repeat time, nullable reward group, and nullable complete time.
- The C# planner is non-live and records descriptors only.

## Deliverables

- Added `Aion.GameServer.Services.QuestPersistencePlanService`.
- Added explicit Java-shaped `QuestPersistenceState` values for planning without requiring C# snapshots to own Java `PersistentState`.
- Added delete/insert/update operation descriptors with Java source breadcrumbs.
- Added focused tests for phase ordering, nullable fields, no-op states, and quest-id ordering.
- Updated persistence audit, quest-finish ordering, nearby audit, and Phase 6 progress docs.

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter QuestPersistencePlanServiceTests --nologo` | Passed: 4 |
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` | Passed: 1759 |

## Migration Parity Table - UOW-1022

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.dao.PlayerQuestListDAO.store` | `Aion.GameServer.Services.QuestPersistencePlanService.CreatePlan` | Repository Plan / Service | Partial | Unit Tested | Partial Parity | C# plans Java delete, insert, and update phases in order with non-live descriptors. It does not execute SQL, commit helper batches, mark states updated after failure, or model Java rollback behavior. |
| `com.aionemu.gameserver.dao.PlayerQuestListDAO.deleteQuest` | `Aion.GameServer.Services.QuestPersistenceOperationDescriptor` | Repository Plan / Delete Descriptor | Partial | Unit Tested | Needs Verification | C# emits delete descriptors for explicit deleted states and deleted quest ids. Java deleted quest id set is a `HashSet`; C# keeps caller order and documents ordering as not verified. |
| `com.aionemu.gameserver.dao.PlayerQuestListDAO.addQuests` | `Aion.GameServer.Services.QuestPersistenceOperationDescriptor` | Repository Plan / Insert Descriptor | Partial | Unit Tested | Partial Parity | C# carries full `PlayerQuestState` payload including nullable reward/time fields. No SQL or Java runtime comparison. |
| `com.aionemu.gameserver.dao.PlayerQuestListDAO.updateQuests` | `Aion.GameServer.Services.QuestPersistenceOperationDescriptor` | Repository Plan / Update Descriptor | Partial | Unit Tested | Partial Parity | C# carries full `PlayerQuestState` payload including nullable reward/time fields. No SQL or Java runtime comparison. |
| `com.aionemu.gameserver.model.gameobjects.player.QuestStateList` | `Aion.GameServer.Services.QuestPersistencePlanService`; `Aion.GameServer.Model.GameObjects.PlayerQuestState` | Model / Persistence Collection Plan | Partial | Unit Tested | Needs Verification | C# sorts current state entries by quest id to mirror Java `TreeMap`; it does not model the live deleted-id `HashSet` or synchronized state-list mutation. |
| `com.aionemu.gameserver.questEngine.model.QuestState` | `Aion.GameServer.Model.GameObjects.PlayerQuestState`; `QuestPersistenceStateEntry` | Model / Quest State Plan | Partial | Unit Tested | Partial Parity | C# represents persisted fields and explicit persistence state for planning. Java mutator side effects and constructor timestamp behavior are not fully modeled. |
| `com.aionemu.gameserver.model.gameobjects.Persistable` | `Aion.GameServer.Services.QuestPersistenceState` | Interface / Persistence State Projection | Partial | Unit Tested | Needs Verification | C# enum projects Java states for planning only. It is not attached to live quest snapshots and does not implement Java state transition semantics. |

## Tests Added

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `CreatePlan_EmitsDeleteInsertUpdatePhasesLikeJavaQuestDao` | Delete, insert, update phase order; deleted-id-set descriptor metadata; non-live flags. | Source-reviewed `PlayerQuestListDAO.store` and helpers. |
| `CreatePlan_CarriesNullableRewardAndTimeFieldsForInsertAndUpdate` | Nullable reward group, next repeat time, and complete time payloads survive descriptor planning. | Source-reviewed insert/update SQL columns. |
| `CreatePlan_ReturnsNoChangesForUpdatedNoActionAndNoDeletedIds` | `UPDATED` and `NOACTION` entries emit no operations. | Source-reviewed `Persistable.NEW`/`CHANGED`/`DELETED` filters. |
| `CreatePlan_OrdersCurrentQuestRowsByQuestIdLikeJavaTreeMap` | Current quest rows are ordered by quest id before phase descriptors. | Source-reviewed `QuestStateList` `TreeMap`. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- No live database writes exist for quest persistence.
- Java helper-level commit/no-rollback behavior is not implemented.
- Java marks current quest states `UPDATED` after store attempts, including helper failures; C# planner does not mutate input state.
- Deleted quest id `HashSet` ordering remains unverified and caller-provided in C#.
- NPC-faction persistence planning remains missing.

## Summary Metrics

- Total Java artifacts discovered: 7 in this unit
- Total artifacts ported: 1 partial non-live quest persistence plan plus descriptor/state DTOs
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 7
- Total blocked artifacts: 5 blocked/partial categories: live quest writes, failure-ordering behavior, input state mutation, deleted-id set ordering, and Java runtime comparison
- Estimated overall migration completion: Phase 6 remains about 71%

## Next Recommended Unit Of Work

Add the sibling non-live NPC-faction persistence operation plan for `PlayerNpcFactionsDAO.storeNpcFactions`: classify explicit faction rows into insert/update descriptors for Java `NEW` and `UPDATE_REQUIRED` states, preserve no-op behavior for other states, and keep writes disabled.

## Next Unit Handoff

Start with this file, `docs/QuestPersistenceContract-Audit.md`, and `docs/PHASE-6-PROGRESS.md` Session 1022.

Recommended next slice:

1. Add a pure NPC-faction persistence plan service and DTOs.
2. Use explicit persistence states, matching Java `NpcFaction` `NEW` and `UPDATE_REQUIRED` write filters.
3. Preserve insert/update descriptor payloads: faction id, active, time, state, quest id.
4. Document Java `HashMap` value-order risk and no state reset after successful writes.
5. Add focused tests and keep live repository writes disabled.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | NPC-faction persistence plan | new service + tests | Medium | Best next implementation slice. |
| B | Compose quest persistence plan into finish operation plan | operation plan + tests | Medium | Wait until NPC-faction plan exists if touching shared descriptors. |
| C | Reward XML projection planning | new docs-only/static-data audit | Medium | Safe separately if it avoids operation-plan files. |

## Do Not Parallelize

- Live DAO write implementation.
- Quest-finish operation plan persistence descriptor redesign while NPC-faction planning is in progress.
- Phase progress/completion docs.
