# Phase 6UC Completion - UOW-1037 Quest Title Cube Warehouse Reward Plans

Date: May 25, 2026

## Current Phase

Phase 6: Port Game Core

## Last Completed Unit Of Work

UOW-1037 added non-live quest reward side-effect plans for title grants, cube expansion rewards, and warehouse expansion rewards.

## Commits Made

- UOW-1037: `[Phase 6][UOW-1037] Stage quest title expansion reward plans`

## Parallel Work Discovery

| Candidate | Scope | Java Source | C# Target | Type | Selected | Risk | Notes |
|---|---|---|---|---|---|---|---|
| A | Quest title/cube/warehouse plan | `TitleList.addTitle`, `CubeExpandService.questExpand`, `WarehouseService.expand` | new planner and tests | Service Plan | Yes | Medium | One local implementation owner avoids shaping conflicts. |
| B | GP helper audit | `GloryPointsService.addGp`, `Rates.GP` | read-only report | Java Analysis | Yes | Low | Spawned as read-only sidecar, then shut down before it produced a report; no output integrated. |
| C | Quest XP helper design | `PlayerCommonData.addExp`, `Rates.XP_QUEST` | future docs/service/tests | Java Analysis / Service Plan | No | Medium | XP has broad level/stat/nearby effects. |
| D | Live quest kinah composition | `QuestService.giveReward`, `Storage.increaseKinah` | quest finish live path | Integration Fix | No | High | Still too early for live mutation and packet/persistence effects. |

## Selected Batch

| Agent | Assigned Task | Task Type | Allowed Files | Forbidden Files | Dependencies | Expected Result |
|---|---|---|---|---|---|---|
| Orchestrator | Title/cube/warehouse non-live planner | Service Plan | new planner/test files, progress/handoff docs | live quest finish execution, DAO writes | Java source review | code, tests, docs, commit |
| Explorer Volta | GP helper audit | Java Analysis | read-only inspection | all writes | none | no integrated output; agent was shut down |

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/QuestRewardSideEffectPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestRewardSideEffectPlanServiceTests.cs`
- `docs/QuestRewardSideEffects-Audit.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6UC-Completion.md`

## Completed

- Added `QuestRewardSideEffectPlanService.CreateTitleRewardPlan`.
- Added `QuestRewardSideEffectPlanService.CreateCubeExpansionPlan`.
- Added `QuestRewardSideEffectPlanService.CreateWarehouseExpansionPlan`.
- Added title reward plan statuses for success, missing player, invalid title, invalid race, and duplicate title.
- Added expansion reward plan statuses for success, missing player, and cannot-expand boundaries.
- Added packet-intent metadata for quest title system message, full title info, duplicate title tooltip, race failure plain text, inventory-size message, cube update, warehouse-size message, regular warehouse info, and cannot-expand system message.
- Added focused unit tests proving planner metadata and non-mutation behavior.

## Java Artifacts Touched

- `com.aionemu.gameserver.model.gameobjects.player.title.TitleList.addTitle`
- `com.aionemu.gameserver.dao.PlayerTitleListDAO.storeTitles`
- `com.aionemu.gameserver.taskmanager.tasks.ExpireTimerTask.registerExpirable`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_QUEST_GET_REWARD_TITLE`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_TITLE_INFO`
- `com.aionemu.gameserver.services.CubeExpandService.questExpand`
- `com.aionemu.gameserver.services.WarehouseService.expand`

## C# Artifacts Touched

- `Aion.GameServer.Services.QuestRewardSideEffectPlanService`
- `Aion.GameServer.Services.QuestTitleRewardPlan`
- `Aion.GameServer.Services.QuestExpansionRewardPlan`
- `Aion.GameServer.Services.QuestRewardPacketIntent`
- Existing dependencies referenced but not modified:
  - `TitleTemplateTable`
  - `PlayerTitle`
  - `InventoryCapacity`

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter QuestRewardSideEffectPlanServiceTests --nologo` | Passed: 6 |
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` | Passed: 1805 |

## Migration Parity Table - Session 1037

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.gameobjects.player.title.TitleList.addTitle` | `Aion.GameServer.Services.QuestRewardSideEffectPlanService.CreateTitleRewardPlan` | Service / Reward Planner | Partial | Unit Tested | Partial Parity | Source-reviewed planner covers invalid title throw intent, owner-null return, race mismatch, duplicate title, and successful quest-title metadata. No live title mutation, expirable registration, immediate DAO write, quest-title packet send, full `SM_TITLE_INFO` send, threading, serialization, or Java runtime comparison. |
| `com.aionemu.gameserver.dao.PlayerTitleListDAO.storeTitles` | `QuestTitleRewardPlan.RequiresImmediatePersistence` | DAO Dependency Metadata | Not Started | Unit Tested | Needs Verification | Immediate persistence requirement is recorded only as metadata. SQL write behavior, transaction/error handling, and in-memory rollback behavior are not ported. |
| `com.aionemu.gameserver.taskmanager.tasks.ExpireTimerTask.registerExpirable` | `QuestTitleRewardPlan.RequiresExpireRegistration` | Scheduler Dependency Metadata | Not Started | Unit Tested | Needs Verification | Expirable registration requirement is recorded only as metadata. No scheduler registration occurs. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_QUEST_GET_REWARD_TITLE` | `QuestRewardPacketIntent.QuestTitleSystemMessage` | Packet Intent | Not Started | Unit Tested | Needs Verification | Packet intent and title l10n token are recorded, but no C# concrete quest-title system-message factory or packet golden test exists. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_TITLE_INFO` | `QuestRewardPacketIntent.FullTitleInfo`; existing `SmTitleInfo` | Packet Intent / Packet | Partial | Unit Tested | Needs Verification | Full title-info intent is recorded. Existing packet exists, but this unit does not create/send a quest-title packet or compare Java bytes. |
| `com.aionemu.gameserver.services.CubeExpandService.questExpand` | `QuestRewardSideEffectPlanService.CreateCubeExpansionPlan` | Service / Expansion Planner | Partial | Unit Tested | Partial Parity | Source-reviewed planner covers Java `canExpand`, next `questExpands`, slot limit delta, message/update intents, and persistence requirement. No live player mutation, `SM_CUBE_UPDATE` send, system message send, persistent-state tracking, or Java runtime comparison. |
| `com.aionemu.gameserver.services.WarehouseService.expand` | `QuestRewardSideEffectPlanService.CreateWarehouseExpansionPlan` | Service / Expansion Planner | Partial | Unit Tested | Partial Parity | Source-reviewed planner covers Java `canExpand`, next `whBonusExpands`, slot limit delta, warehouse message/info intents, and persistence requirement. No live player mutation, warehouse packet send, persistent-state tracking, account warehouse packet tail, or Java runtime comparison. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `QuestRewardSideEffectPlanServiceTests.CreateTitleRewardPlan_PlansQuestTitlePersistenceAndPackets` | Unit | `TitleList.addTitle(title, true, 0)` | Successful quest-title reward metadata: permanent title, l10n title name, immediate persistence, expirable registration, quest-title message intent, and full title-info intent. | Source-reviewed Java behavior. | No live mutation, DAO write, concrete packet send, or Java runtime capture. |
| `QuestRewardSideEffectPlanServiceTests.CreateTitleRewardPlan_PreservesInvalidRaceDuplicateAndInvalidTemplateBranches` | Unit | `TitleList.addTitle` | Invalid race, duplicate title, and missing template branches. | Source-reviewed Java branch order. | Missing owner branch is represented but not separately tested; no plain-text packet implementation. |
| `QuestRewardSideEffectPlanServiceTests.CreateCubeExpansionPlan_PlansQuestExpandWithoutMutatingPlayer` | Unit | `CubeExpandService.questExpand` | Quest cube expansion count/slot delta and packet intents without player mutation. | Source-reviewed Java behavior plus existing C# capacity helper. | No concrete packet bytes or live persistence. |
| `QuestRewardSideEffectPlanServiceTests.CreateCubeExpansionPlan_RecordsJavaCannotExpandBoundary` | Unit | `CubeExpandService.canExpand` | Cube expansion cap rejection metadata. | Source-reviewed Java cap behavior. | No system-message packet send. |
| `QuestRewardSideEffectPlanServiceTests.CreateWarehouseExpansionPlan_PlansBonusExpansionAndPackets` | Unit | `WarehouseService.expand(player, false)` | Bonus warehouse expansion count/slot delta and packet intents without player mutation. | Source-reviewed Java behavior plus existing C# capacity helper. | Account-warehouse tail packet intent is not separately modeled. |
| `QuestRewardSideEffectPlanServiceTests.CreateWarehouseExpansionPlan_RecordsJavaCannotExpandBoundary` | Unit | `WarehouseService.canExpand` | Warehouse expansion cap rejection metadata. | Source-reviewed Java cap behavior. | No system-message packet send. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- Quest title/cube/warehouse plans are not composed into quest finish and are not live.
- Quest title concrete `STR_QUEST_GET_REWARD_TITLE` packet factory is still missing.
- Title immediate DAO write, expirable registration, and duplicate/race failure packet delivery remain metadata only.
- Cube/warehouse system messages and update/info packets remain metadata only in this quest reward path.
- Threading, reflection/dynamic handler behavior, serialization, date/time, and transaction semantics remain unverified.

## Summary Metrics

- Total Java artifacts discovered: 7 in this unit
- Total artifacts ported: 1 new non-live planner plus 3 partial planning surfaces
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 7
- Total blocked artifacts: 6 blocked/partial categories: Java runtime comparison, live quest-finish composition, title DAO/expirable execution, concrete quest-title packets, cube live mutation/packets, and warehouse live mutation/packets
- Estimated overall migration completion: Phase 6 remains about 71% complete; this unit stages title/cube/warehouse reward planning without enabling live reward mutation.

## Next Work Options

### Recommended Sequential Task

- Task: Compose `QuestRewardSideEffectPlanService` title/cube/warehouse plans into quest-finish operation metadata.
- Why: The side-effect plans exist but are not yet visible in `QuestFinishOperationPlanService`; composition should preserve Java `giveReward` ordering and keep execution non-live.
- Files: likely `QuestFinishOperationPlanService.cs`, `QuestFinishOperationPlanServiceTests.cs`, progress/handoff docs.

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Compose title/cube/warehouse side-effect metadata | operation plan service/tests | Medium | Shared operation-plan ordering; orchestrator should own this. |
| B | GP helper audit | dedicated audit doc or read-only report | Low | Independent if read-only or dedicated doc. Previous sidecar was shut down before output. |
| C | Quest title concrete packet helper | `SmSystemMessage.cs`, packet tests | Low-Medium | Can be separate from operation-plan composition if file ownership is strict. |
| D | Quest XP helper design | dedicated audit doc | Medium | Read-only first; XP side effects are broad. |

### Suggested Parallel Batch

For the next session, prefer one implementation task plus one read-only sidecar:

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Compose title/cube/warehouse plans into operation metadata | `QuestFinishOperationPlanService.cs`, `QuestFinishOperationPlanServiceTests.cs`, progress/handoff docs | packet helpers, GP docs unless assigned |
| Explorer A | GP helper audit | read-only Java/C# inspection or dedicated audit doc if assigned | production code, shared progress/handoff docs |

### Do Not Parallelize

- `QuestFinishOperationPlanService.cs`: shared operation ordering.
- `QuestFinishRewardPlanService.cs`: shared reward projection contract.
- `QuestRewardSideEffectPlanService.cs`: new side-effect planner.
- `QuestRewardService.cs`: AP/DP/kinah helper surface.
- `docs/PHASE-6-PROGRESS.md` and handoff docs: orchestrator-owned.

## Context Needed By Next Session

Start with this file, `docs/QuestRewardSideEffects-Audit.md`, `docs/QuestFinishOrdering-Audit.md`, `docs/PHASE-6-PROGRESS.md` Session 1037, and current `QuestRewardSideEffectPlanServiceTests`.
