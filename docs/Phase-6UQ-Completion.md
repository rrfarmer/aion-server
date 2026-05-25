# Phase 6UQ Completion - UOW-1051 NPC Faction Level-Up Plan

Date: May 25, 2026

## Current Phase

Phase 6 remains active. Java is the source of truth. This handoff continues after `docs/Phase-6UP-Completion.md`.

## Last Completed Unit

- UOW-1051: `[Phase 6][UOW-1051] Stage NPC faction level-up plan`
- Recent commits before this unit:
  - `4bb8f37b1 [Phase 6][UOW-1050] Stage upgrade-player level-change plan`
  - `1a5b88e20 [Phase 6][UOW-1049] Add level-up action animation constant`
  - `bb3ca5c5d [Phase 6][UOW-1048] Stage quest XP level-change plan`
- UOW-1051 status: complete; code, tests, progress notes, parity table, and this next handoff are included in this unit.

## Summary

UOW-1051 adds a non-live C# plan for Java `NpcFactions.onLevelUp`, the next Java-order dependency under `PlayerController.onLevelChange` after the level-up animation broadcast. The planner scans active non-mentor and mentor slots in Java order, plans over-level faction deactivation, records START-state quest abandon intent, records the Java level-limit system-message id, and keeps live XP/faction mutation disabled.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | NPC faction level-up planner | `NpcFactions.onLevelUp`, `NpcFaction`, `NpcFactionsData`, `QuestService.abandonQuest`, `SM_SYSTEM_MESSAGE.STR_FACTION_LEAVE_BY_LEVEL_LIMIT` | `PlayerNpcFactionState.cs`, new planner service, snapshot tests, docs | Service Plan | Selected sequential | Medium | Concrete Java side effect with existing C# snapshot/static-data support. |
| B | QuestEngine level-change audit | `QuestEngine.onLevelChanged` | read-only Java/C# inspection and docs | Java Analysis | Yes read-only | Medium | Good next sidecar, but docs overlap with selected UOW. |
| C | System-message helper | `SM_SYSTEM_MESSAGE.STR_FACTION_LEAVE_BY_LEVEL_LIMIT` | `SmSystemMessage.cs`, packet tests | Packet Helper | Yes if isolated | Low-Medium | Useful future packet prerequisite, but this UOW records metadata only. |
| D | Compose upgrade/NPC faction sub-plans into XP execution metadata | `PlayerController.onLevelChange` | `QuestXpExecutionPlanService.cs`, tests | Plan Composition | No | Medium | Shared XP execution order should remain orchestrator-owned. |

## Selected Work

The orchestrator implemented Candidate A sequentially. No sub-agent was spawned because the selected code touched shared faction snapshot behavior and shared migration docs. Read-only sidecar audits were deferred.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/PlayerNpcFactionState.cs`
- `dotnetConversion/src/Aion.GameServer/Services/NpcFactionLevelUpPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerNpcFactionsSnapshotTests.cs`
- `docs/QuestXpReward-Audit.md`
- `docs/QuestRewardSideEffects-Audit.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6UQ-Completion.md`

## What Changed

- Added `PlayerNpcFactionsSnapshot.GetActiveFaction(bool isMentor)`.
- Added `NpcFactionLevelUpPlanService.CreatePlan`.
- Added metadata for:
  - active slot scan order,
  - max-level retention,
  - over-level deactivation,
  - START-state quest abandon intent,
  - Java system-message id `1400770`,
  - missing snapshot/table/template blockers.
- Added focused unit coverage for applied, no-change, no-active, and missing-template branches.

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "PlayerNpcFactionsSnapshotTests" --nologo` | Passed: 9 |
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` | Passed: 1835 |

## Migration Parity Table - UOW-1051

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.gameobjects.player.npcFaction.NpcFactions.onLevelUp` | `Aion.GameServer.Services.NpcFactionLevelUpPlanService.CreatePlan`; `NpcFactionLevelUpPlan` | Model Side-Effect Plan | Partial | Unit Tested | Partial Parity | Non-live plan scans active non-mentor then mentor slots, deactivates over-level active factions, records START-state quest abandon intent, records Java system-message id `1400770`, and resets state to `Noting`. It does not mutate the live player, call `QuestService.abandonQuest`, send packets, or persist faction rows. |
| `com.aionemu.gameserver.model.gameobjects.player.npcFaction.NpcFactions.getActiveNpcFaction` | `Aion.GameServer.Model.GameObjects.PlayerNpcFactionsSnapshot.GetActiveFaction` | Model Helper | Complete | Unit Tested | Partial Parity | C# exposes the active mentor/non-mentor slot used by Java. The snapshot remains immutable and does not model Java's mutable active-slot array side effects live. |
| `com.aionemu.gameserver.model.gameobjects.player.npcFaction.NpcFaction` | `Aion.GameServer.Model.GameObjects.PlayerNpcFactionState` | Model / DTO | Partial | Unit Tested | Partial Parity | Existing C# state now supports level-up planning by cloning records with `IsActive=false` and `State=Noting`. Java persistent-state transitions (`UPDATE_REQUIRED`) are not modeled in the DTO; persistence remains a later plan. |
| `com.aionemu.gameserver.dataholders.NpcFactionsData`; `com.aionemu.gameserver.model.templates.factions.NpcFactionTemplate` | `Aion.GameServer.Dataholders.NpcFactionTable`; `NpcFactionSummary` | Static Data / Template | Partial | Unit Tested via planner and existing static-data tests | Needs Verification | Planner uses `GetNpcFactionById` and `MaxLevel` like Java. Missing templates are blocked metadata in C# instead of Java's likely null dereference; this is conservative non-live behavior and needs runtime confirmation before live execution. |
| `com.aionemu.gameserver.services.QuestService.abandonQuest` | `NpcFactionLevelUpDescriptor.QuestIdToAbandon` | Service Dependency | Not Started | Unit Tested as metadata | Needs Verification | Java abandons the faction quest only when the over-level faction state is `START`. C# records the quest id but does not dispatch abandon logic or compare quest-state side effects. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_FACTION_LEAVE_BY_LEVEL_LIMIT` | `NpcFactionLevelUpPlanService.FactionLeaveByLevelLimitSystemMessageId` | Packet Dependency | Partial | Unit Tested as metadata | Needs Verification | Java message id `1400770` is recorded with faction template name metadata, but no `SmSystemMessage` helper, packet serialization assertion, or live send exists yet. |
| `com.aionemu.gameserver.controllers.PlayerController.onLevelChange` | `QuestXpExecutionPlanService`; `NpcFactionLevelUpPlanService` | Controller Dependency | Partial | Unit Tested as metadata | Needs Verification | UOW-1048 staged the full level-change order; UOW-1051 adds a dedicated NPC-faction sub-plan. The sub-plan is not composed into XP execution and live XP level-change execution remains disabled. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `PlayerNpcFactionsSnapshotTests.LevelUpPlan_DeactivatesOverLevelActiveFactionAndRecordsJavaSideEffects` | Unit | `NpcFactions.onLevelUp`; `NpcFactionTemplate.getMaxLevel`; `QuestService.abandonQuest`; `SM_SYSTEM_MESSAGE.STR_FACTION_LEAVE_BY_LEVEL_LIMIT` | Over-level active non-mentor faction is planned inactive, state resets to `Noting`, START-state quest id is recorded for abandon, system-message id `1400770` is recorded, mentor slot remains active when within level limit, and descriptors are non-live. | Source-reviewed Java method order and C# static faction summaries. | No live quest abandon, packet send, persistence, Java runtime comparison, or active-slot mutation. |
| `PlayerNpcFactionsSnapshotTests.LevelUpPlan_RecordsNoChangesMissingTemplateAndNoActiveBranches` | Unit | `NpcFactions.onLevelUp`; `NpcFactions.getActiveNpcFaction`; `DataManager.NPC_FACTIONS_DATA.getNpcFactionById` | Boundary outcomes for exact max level retention, missing template/table blockers, no active factions, and missing snapshot. | Source-reviewed Java guards and template lookup. | Missing-template behavior is conservative C# metadata; Java runtime null-template behavior is not executed. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- The NPC faction level-up plan is not composed into `QuestXpExecutionPlan` and is not connected to quest-finish execution.
- Live `QuestService.abandonQuest` side effects are not modeled or dispatched.
- `STR_FACTION_LEAVE_BY_LEVEL_LIMIT` is metadata only; no C# system-message helper or packet serialization assertion exists.
- Java `NpcFaction` persistent-state transitions are not represented in `PlayerNpcFactionState`; future live execution must compose with `NpcFactionPersistencePlanService`.
- Missing static faction templates are treated as blocked metadata, while Java would likely throw if static data were inconsistent; this needs a runtime or startup-data invariant before live parity can be claimed.
- Java level-change side effects outside NPC faction level-up remain descriptors only: quest callbacks, nearby refresh, guide HTML, skill auto-learn, custom rewards, and starter kits.

## Summary Metrics

- Total Java artifacts discovered: 7 in this unit
- Total artifacts ported: 1 non-live NPC faction level-up side-effect sub-plan plus 1 active-slot snapshot helper
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 7
- Total blocked artifacts: 5 blocked/partial categories: Java runtime comparison, live XP composition, live quest abandon, system-message packet helper/send, and faction persistence-state integration
- Estimated overall migration completion: Phase 6 remains about 71% complete; this unit adds one more level-change side-effect prerequisite without enabling live XP mutation.

## Next Work Options

### Recommended Sequential Task

- Task: Audit or stage Java `QuestEngine.onLevelChanged` callback dispatch.
- Why: `PlayerController.onLevelChange` now has concrete metadata prerequisites for level-up animation, upgrade-player, and NPC faction level-up. The next Java-order blocker is callback dispatch into dynamic quest handlers.
- Suggested first slice: read Java `QuestEngine.onLevelChanged`, handler dispatch surfaces, quest-env inputs, C# dynamic quest runtime equivalents, and produce either an audit or a non-live descriptor plan.
- Likely files: read-only Java/C# inspection plus `docs/QuestXpReward-Audit.md`, `docs/QuestRewardSideEffects-Audit.md`, progress/handoff docs. Keep live XP execution disabled.

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | QuestEngine level-change audit | read-only Java/C# inspection, docs owned by orchestrator | Medium | Best next Java-order dependency. |
| B | Faction leave system-message helper | `SmSystemMessage.cs`, packet tests | Low-Medium | Isolated packet prerequisite for future live NPC faction plan execution. |
| C | Compose existing sub-plans into XP metadata | `QuestXpExecutionPlanService.cs`, XP tests | Medium | Sequential only; shared XP execution order. |
| D | Nearby quest refresh audit | read-only Java/C# inspection | Medium | Useful after callbacks are understood. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Audit/stage `QuestEngine.onLevelChanged` | Exact selected docs/code files | Any files assigned to a sidecar agent |
| Sidecar A | Read-only `SM_SYSTEM_MESSAGE.STR_FACTION_LEAVE_BY_LEVEL_LIMIT` packet helper audit | Read-only Java/C# inspection only | All writes |
| Sidecar B | Read-only nearby quest refresh audit | Read-only Java/C# inspection only | All writes |

Use sidecars only when the implementation scope does not require the same files. Shared docs remain orchestrator-owned.

## Do Not Parallelize

- `QuestXpExecutionPlanService.cs`: shared XP execution order.
- `NpcFactionLevelUpPlanService.cs`: new sub-plan and likely future composition point.
- `PlayerNpcFactionState.cs`: shared faction snapshot model.
- `docs/PHASE-6-PROGRESS.md`, audit docs, and handoff docs: orchestrator-owned.

## Context Needed By Next Session

Start with this file, `docs/QuestXpReward-Audit.md`, `docs/QuestRewardSideEffects-Audit.md`, `docs/PHASE-6-PROGRESS.md` Session 1051, `NpcFactionLevelUpPlanService.cs`, `PlayerNpcFactionState.cs`, `PlayerNpcFactionsSnapshotTests.cs`, `QuestXpExecutionPlanService.cs`, and Java `QuestEngine.onLevelChanged`.
