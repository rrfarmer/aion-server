# Phase 6UP Completion - UOW-1050 Upgrade-Player Level-Change Plan

Date: May 25, 2026

## Current Phase

Phase 6 remains active. Java is the source of truth. This handoff continues after `docs/Phase-6UO-Completion.md`.

## Last Completed Unit

- UOW-1050: `[Phase 6][UOW-1050] Stage upgrade-player level-change plan`
- Recent commits before this unit:
  - `1a5b88e20 [Phase 6][UOW-1049] Add level-up action animation constant`
  - `bb3ca5c5d [Phase 6][UOW-1048] Stage quest XP level-change plan`
  - `7034fb641 [Phase 6][UOW-1047] Bridge quest XP message metadata`
- UOW-1050 status: complete; code, tests, progress notes, parity table, and this next handoff are included in this unit.

## Summary

UOW-1050 adds a non-live C# plan for Java `PlayerController.upgradePlayer`. The new service records the Java order for HP/MP/FP synchronization, visual stats update, team stat update, and legion member update, including conservative skipped/blocked statuses for missing max stats, dead players, no team, and no legion. It does not enable live XP mutation, live level-up execution, packet sends, or team/legion fanout.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Upgrade-player sub-plan | `PlayerController.upgradePlayer`, `PlayerLifeStats.synchronizeWithMaxStats`, `PlayerGameStats.updateStatsVisually`, `TeamStatUpdater`, `LegionService.updateMemberInfo` | new service/test files plus XP docs | Service Plan | Selected sequential | Medium | Concrete next dependency under `PlayerController.onLevelChange`; non-live planner keeps risk contained. |
| B | NPC faction level-up audit | `NpcFactions.onLevelUp` | read-only Java/C# inspection and docs | Java Analysis | Yes read-only | Medium | Useful next side-effect analysis after `upgradePlayer`, but not needed to complete this slice. |
| C | QuestEngine level-change audit | `QuestEngine.onLevelChanged` | read-only Java/C# inspection and docs | Java Analysis | Yes read-only | Medium | Clarifies callback ordering before live XP composition. |
| D | Offline GP opt-in adapter | `GloryPointsService.addGp`, `AbyssRankDAO.addGp` | GP service/repository files | Service Port | Yes code-wise | Medium | Separate from XP files, but shared docs remain serialized. |

## Selected Work

The orchestrator implemented Candidate A sequentially. No sub-agent was spawned because the code change was modest, while progress/audit/handoff documentation remained shared and orchestrator-owned.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/PlayerLevelChangeUpgradePlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerLevelChangeUpgradePlanServiceTests.cs`
- `docs/QuestXpReward-Audit.md`
- `docs/QuestRewardSideEffects-Audit.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6UP-Completion.md`

## What Changed

- Added `PlayerLevelChangeUpgradePlanService.CreatePlan`.
- Added plan records for Java `upgradePlayer` order:
  - `PlayerLifeStats.synchronizeWithMaxStats`
  - `PlayerGameStats.updateStatsVisually`
  - `TeamStatUpdater.add`
  - `LegionService.updateMemberInfo`
- Added status metadata for missing max stats, missing life stats, dead players, no team, and no legion.
- Added packet-intent notes for future `SM_STATS_INFO`, `SM_GROUP_MEMBER_INFO`, `SM_ALLIANCE_MEMBER_INFO`, and `SM_LEGION_UPDATE_MEMBER` execution.
- Updated XP/reward audit docs to record this as a non-live level-change prerequisite.

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "PlayerLevelChangeUpgradePlanServiceTests" --nologo` | Passed: 2 |
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` | Passed: 1833 |

## Migration Parity Table - UOW-1050

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.PlayerController.upgradePlayer` | `Aion.GameServer.Services.PlayerLevelChangeUpgradePlanService.CreatePlan`; `PlayerLevelChangeUpgradePlan` | Controller Side-Effect Plan | Partial | Unit Tested | Partial Parity | Non-live plan records Java ordering for life-stat sync, visual stats update, team stat task, and legion member update. It does not mutate player state, send packets, update live team/legion members, or calculate max stats from game stat templates. |
| `com.aionemu.gameserver.model.stats.container.PlayerLifeStats.synchronizeWithMaxStats`; `com.aionemu.gameserver.model.stats.container.CreatureLifeStats.synchronizeWithMaxStats` | `PlayerLevelChangeUpgradeAction.LifeStatsSynchronize`; `PlayerLevelChangeUpgradeStats`; `PlayerLifeStats` | Model Side-Effect Plan | Partial | Unit Tested | Partial Parity | Plan can project current HP/MP/FP to supplied max values and preserves Java dead-player skip. C# max HP/MP/FP calculation is caller-supplied and not verified here; spawned-state HP/MP/FP packet sends remain unported. |
| `com.aionemu.gameserver.model.stats.container.PlayerGameStats.updateStatsVisually` | `PlayerLevelChangeUpgradeAction.VisualStatsUpdate`; existing `PlayerVisualStatsUpdateService`/`SmStatsInfo` | Packet/Stats Dependency Plan | Partial | Unit Tested as metadata | Needs Verification | Plan records visual stats update after life-stat sync. It does not invoke `PlayerVisualStatsUpdateService`, create/send `SmStatsInfo`, compare Java bytes, or verify stat template recalculation. |
| `com.aionemu.gameserver.taskmanager.tasks.TeamStatUpdater.add`; `PlayerGroupService.updateGroup`; `PlayerAllianceService.updateAlliance` | `PlayerLevelChangeUpgradeAction.TeamStatUpdate`; `PlayerTeamMembership` | Team Side-Effect Plan | Partial | Unit Tested as metadata | Needs Verification | Plan distinguishes no-team, group, and alliance branches and records Java packet intent (`SM_GROUP_MEMBER_INFO`/`SM_ALLIANCE_MEMBER_INFO`). It does not queue a task, resolve live members, or send group/alliance packets. Threading/timer behavior remains unverified. |
| `com.aionemu.gameserver.services.LegionService.updateMemberInfo`; `com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_UPDATE_MEMBER` | `PlayerLevelChangeUpgradeAction.LegionMemberUpdate`; `Player.LegionId` | Legion Side-Effect Plan | Partial | Unit Tested as metadata | Needs Verification | Plan records no-legion vs legion-member branch and the `SM_LEGION_UPDATE_MEMBER` broadcast intent. It does not refresh a live `LegionMember`, broadcast to legion, or verify Java packet serialization. |
| `com.aionemu.gameserver.controllers.PlayerController.onLevelChange` | `QuestXpExecutionPlanService`; `PlayerLevelChangeUpgradePlanService` | Controller Dependency | Partial | Unit Tested as metadata | Needs Verification | UOW-1048 staged the full level-change order and UOW-1050 adds a dedicated upgrade-player sub-plan. The sub-plan is not yet composed into `QuestXpExecutionPlan`; live XP execution remains disabled. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `PlayerLevelChangeUpgradePlanServiceTests.CreatePlan_StagesJavaUpgradePlayerOrderWithTeamAndLegionDependencies` | Unit | `PlayerController.upgradePlayer`; `PlayerLifeStats.synchronizeWithMaxStats`; `PlayerGameStats.updateStatsVisually`; `TeamStatUpdater`; `LegionService.updateMemberInfo` | Java descriptor order, HP/MP/FP projection with supplied max stats, alliance team packet intent, legion packet intent, and non-live descriptors. | Source-reviewed Java method order and C# model snapshots. | No live mutation, packet send, Java runtime comparison, or max-stat calculation. |
| `PlayerLevelChangeUpgradePlanServiceTests.CreatePlan_RecordsMissingMaxStatsDeadAndNoTeamLegionBranches` | Unit | `PlayerLifeStats.synchronizeWithMaxStats`; `TeamStatUpdater.callTask`; `LegionService.updateMemberInfo` | Missing max stats, dead-player skip, no-team/no-legion skips, group packet intent, and missing-player guard. | Source-reviewed Java dead-player guard and conditional team/legion branches. | Spawned-state HP/MP/FP packet sends and Java task timing are not covered. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- Upgrade-player plan is not composed into `QuestXpExecutionPlan` and is not connected to quest-finish execution.
- C# max-stat calculation is not supplied by this plan; HP/MP/FP synchronization uses caller-provided max values only.
- Java spawned-state resource packet sends from `PlayerLifeStats.synchronizeWithMaxStats` remain metadata only.
- Visual stats, team stat, and legion update packet sends remain disabled.
- Java level-change side effects outside `upgradePlayer` remain descriptors only: ratio updates, stat template update, max repose, salvation reset, level-up animation broadcast, NPC faction level-up, quest callbacks, nearby refresh, guide HTML, skill auto-learn, custom rewards, and starter kits.
- Threading/timer behavior for `TeamStatUpdater` and live legion broadcast fanout remain unverified.

## Summary Metrics

- Total Java artifacts discovered: 6 in this unit
- Total artifacts ported: 1 non-live upgrade-player side-effect sub-plan
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6
- Total blocked artifacts: 5 blocked/partial categories: Java runtime comparison, max-stat calculation, live resource/stat packet sends, team/legion fanout, and live XP execution composition
- Estimated overall migration completion: Phase 6 remains about 71% complete; this unit adds one level-change side-effect prerequisite without enabling live XP mutation.

## Next Work Options

### Recommended Sequential Task

- Task: Run a read-only audit for the next Java level-change dependency after the animation broadcast: `NpcFactions.onLevelUp`, or audit `QuestEngine.onLevelChanged` immediately after it.
- Why: `upgradePlayer` now has a non-live sub-plan. The next unknowns are callback-heavy side effects that should be understood before composing live XP execution.
- Suggested first slice: document Java `NpcFactions.onLevelUp` behavior, C# faction model coverage, missing methods, packet/persistence effects, and whether a non-live planner should come next.
- Likely files: read-only Java/C# inspection plus `docs/QuestXpReward-Audit.md`, `docs/QuestRewardSideEffects-Audit.md`, progress/handoff docs. Keep live XP execution disabled.

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | NPC faction level-up audit | read-only Java/C# inspection, docs owned by orchestrator | Medium | Best next Java-order dependency after level-up animation. |
| B | QuestEngine level-change audit | read-only Java/C# inspection, docs owned by orchestrator | Medium | Useful sidecar if no writes overlap. |
| C | Visual stats live-adapter planning | `PlayerVisualStatsUpdateService` tests/docs | Medium | Do not enable live XP; just assess existing stats packet service. |
| D | Offline GP opt-in adapter | GP service/repository files | Medium | Code-isolated from XP, but shared docs remain serialized. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Audit `NpcFactions.onLevelUp` or compose the next selected plan | Exact selected docs/code files | Any files assigned to a sidecar agent |
| Sidecar A | Read-only QuestEngine level-change audit | Read-only Java/C# inspection only | All writes |
| Sidecar B | Read-only visual stats/team/legion live-surface audit | Read-only Java/C# inspection only | All writes |

Use sidecars only when the implementation scope does not require the same files. Shared docs remain orchestrator-owned.

## Do Not Parallelize

- `QuestXpExecutionPlanService.cs`: shared XP execution order.
- `PlayerLevelChangeUpgradePlanService.cs`: new sub-plan owned by this unit and likely next composition point.
- `docs/PHASE-6-PROGRESS.md`, audit docs, and handoff docs: orchestrator-owned.

## Context Needed By Next Session

Start with this file, `docs/QuestXpReward-Audit.md`, `docs/QuestRewardSideEffects-Audit.md`, `docs/PHASE-6-PROGRESS.md` Session 1050, `PlayerLevelChangeUpgradePlanService.cs`, `PlayerLevelChangeUpgradePlanServiceTests.cs`, `QuestXpExecutionPlanService.cs`, and the level-change findings in `docs/Phase-6UN-Completion.md`.
