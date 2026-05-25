# Phase 6TP Completion - UOW-1024 Quest-Finish Persistence Plan Composition

## Scope

UOW-1024 composes the non-live quest and NPC-faction persistence planners into `QuestFinishOperationPlanService`.

This unit does not write to the database, send packets, execute callbacks, mutate live persistence state, or change Java failure-ordering behavior.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Compose persistence planners into quest finish | `QuestService.finishQuest`, `PlayerService.storePlayer`, `PlayerQuestListDAO`, `PlayerNpcFactionsDAO` | `QuestFinishOperationPlanService` + tests | Service Composition | Selected | Medium | Direct handoff from UOW-1023; uses existing planners and stays non-live. |
| B | Callback dispatch plan | `QuestEngine.onQuestCompleted`, handler loader | new service/docs + tests | Service Port | Yes later | Medium | Separate files, but ordering relation must remain documented by Orchestrator. |
| C | Reward XML projection planning | `QuestTemplate`, `Rewards`, `QuestWorkItems` | docs/static-data projections | Java Analysis | Yes later | Medium | Does not need operation-plan files. |
| D | Live DAO write implementation | DAO equivalents | repository code/tests | Repository Port | No | High | Requires persistence composition and failure-ordering policy first. |

Selected batch: A only. File ownership was `QuestFinishOperationPlanService`, its focused tests, and Orchestrator-owned docs.

## Java Breadcrumbs

- `QuestService.finishQuest` performs reward/work-item handling before quest-state mutation.
- It sends `SM_QUEST_ACTION(ActionType.UPDATE, qs)` before `QuestEngine.onQuestCompleted`.
- NPC-faction completion occurs after callbacks and before nearby quest refresh.
- Persistence is later driven by `PlayerService.storePlayer`, with `PlayerQuestListDAO.store` before inventory and `PlayerNpcFactionsDAO.storeNpcFactions` after inventory/cooldowns/mailbox.
- C# still keeps persistence descriptor-only; no DAO writes are invoked.

## Deliverables

- Extended `QuestFinishOperationDescriptor` with optional `QuestPersistenceOperation` and `NpcFactionPersistenceOperation` payloads.
- Extended `QuestFinishOperationPlanService.CreatePlan` with optional `QuestPersistencePlan` and `NpcFactionPersistencePlan` inputs.
- Preserved old placeholder behavior when persistence plans are not supplied.
- Added detailed persistence descriptors after nearby refresh when plans are supplied.
- Added focused tests for detailed persistence composition and empty-plan behavior.
- Updated persistence audit, quest-finish ordering, nearby audit, and Phase 6 progress docs.

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter QuestFinishOperationPlanServiceTests --nologo` | Passed: 9 |
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` | Passed: 1765 |

## Migration Parity Table - UOW-1024

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.QuestService.finishQuest` | `Aion.GameServer.Services.QuestFinishOperationPlanService.CreatePlan` | Service / Operation Plan | Partial | Unit Tested | Partial Parity | C# composes optional quest and NPC-faction persistence operation descriptors after nearby-refresh planning while preserving prior reward/state/packet/callback/NPC-faction ordering. It remains non-live and does not run Java callbacks, send packets, or mutate repositories. |
| `com.aionemu.gameserver.dao.PlayerQuestListDAO.store` | `Aion.GameServer.Services.QuestPersistencePlanService`; `QuestFinishOperationDescriptor.QuestPersistenceOperation` | Repository Plan / Service Composition | Partial | Unit Tested | Partial Parity | C# carries detailed quest persistence descriptors into quest-finish planning. No SQL writes, helper-level commits, rollback/failure behavior, state reset, or Java runtime comparison exists. |
| `com.aionemu.gameserver.dao.PlayerNpcFactionsDAO.storeNpcFactions` | `Aion.GameServer.Services.NpcFactionPersistencePlanService`; `QuestFinishOperationDescriptor.NpcFactionPersistenceOperation` | Repository Plan / Service Composition | Partial | Unit Tested | Partial Parity | C# carries detailed NPC-faction descriptors into quest-finish planning. No per-row connection writes, auto-commit behavior, no-reset-after-success behavior, or Java runtime comparison exists. |
| `com.aionemu.gameserver.services.player.PlayerService.storePlayer` | `Aion.GameServer.Services.QuestFinishOperationPlanService` persistence descriptor ordering | Service / Persistence Orchestration Reference | Not Started | Unit Tested for descriptor placement | Needs Verification | C# references later persistence from quest finish only as non-live descriptors. It does not model full logout-store ordering, inventory relation, or actual repository calls. |
| `com.aionemu.gameserver.model.gameobjects.Persistable` | `QuestPersistenceState`; `NpcFactionPersistenceState` through operation-plan descriptors | Interface / Persistence State Projection | Partial | Unit Tested | Needs Verification | C# states remain explicit inputs and are not attached to live quest/faction snapshots. Java transition rules, threading behavior, and post-store state mutation are not fully modeled. |

## Tests Added

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `CreatePlan_ComposesDetailedPersistencePlansAfterNearbyRefresh` | Detailed quest and NPC-faction persistence descriptors are appended after nearby refresh without moving reward, state mutation, update packet, callback, or NPC-faction completion. | Source-reviewed `QuestService.finishQuest`, `PlayerService.storePlayer`, and DAO planner handoffs. |
| `CreatePlan_UsesProvidedEmptyPersistencePlansWithoutLegacyPlaceholders` | Supplied no-op persistence plans remove legacy placeholders instead of implying writes. | Source-reviewed DAO early/no-op filters; no Java runtime capture. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- Quest-finish persistence is still descriptor-only; no repository writes are enabled.
- Java persistence failure behavior and post-store state mutation are not modeled.
- Full `PlayerService.storePlayer` ordering is not executed by quest-finish planning.
- Callback dispatch remains a placeholder; real callbacks can mutate quest/faction state before persistence.
- Live packet sends and nearby quest refresh remain disabled.

## Summary Metrics

- Total Java artifacts discovered: 5 in this unit
- Total artifacts ported: 1 partial operation-plan composition update
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 5
- Total blocked artifacts: 6 blocked/partial categories: live quest writes, live NPC-faction writes, full store-player ordering, callback runtime, failure-ordering behavior, and Java runtime comparison
- Estimated overall migration completion: Phase 6 remains about 71%

## Next Recommended Unit Of Work

Add a non-live completion callback dispatch plan that models Java `QuestEngine.onQuestCompleted` handler registration order, shared `QuestEnv`, and exception-stop behavior without executing handlers.

## Next Unit Handoff

Start with this file, `docs/QuestFinishOrdering-Audit.md`, `docs/QuestCompletionCallback-Audit.md`, and `docs/PHASE-6-PROGRESS.md` Session 1024.

Recommended next slice:

1. Add a pure callback dispatch plan service and DTOs.
2. Model handler registration order as caller-provided because Java reflection/script load order is not deterministic from C#.
3. Represent shared `QuestEnv(null, player, questId)` metadata without executing handlers.
4. Document Java exception behavior: a thrown handler stops remaining callbacks after logging.
5. Add focused tests and keep live callback execution disabled.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Callback dispatch plan | new callback service + tests/docs | Medium | Best next implementation slice. |
| B | Reward XML projection planning | new docs/static-data audit | Medium | Safe if it avoids operation-plan files. |
| C | Persistence failure-ordering policy audit | docs-only | Medium | Useful before live DAO writes. |

## Do Not Parallelize

- Live DAO write implementation.
- Live callback execution.
- Quest-finish operation plan edits from multiple workers.
- Phase progress/completion docs.
