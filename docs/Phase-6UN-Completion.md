# Phase 6UN Completion - UOW-1048 XP Execution Level-Change Plan

Date: May 25, 2026

## Current Phase

Phase 6 remains active. Java is the source of truth. This handoff continues after `docs/Phase-6UM-Completion.md`.

## Last Completed Unit

- UOW-1048: `[Phase 6][UOW-1048] Stage quest XP level-change plan`
- Recent commits before this unit:
  - `7034fb641 [Phase 6][UOW-1047] Bridge quest XP message metadata`
  - `fd3974ada [Phase 6][UOW-1046] Add quest XP system messages`
  - `35b9c7a0d [Phase 6][UOW-1045] Compose quest XP reward metadata`
- UOW-1048 status: complete; code, tests, progress notes, parity table, and this next handoff are included in this unit.

## Summary

UOW-1048 adds a non-live `QuestXpExecutionPlanService` that converts an applied `QuestXpRewardPlan` into Java-order execution descriptors. It records `PlayerCommonData.setExp`, Java `PlayerController.onLevelChange` side effects, `SM_STATUPDATE_EXP`, XP system-message metadata, and optional ascension-warning metadata without mutating or sending anything.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | XP execution/level-change plan | `PlayerCommonData.addExp`, `PlayerCommonData.setExp`, `PlayerController.onLevelChange` | new XP execution service/tests | Service Port | No with XP execution edits | Medium | Owns the shared Java reward and level-change ordering. |
| B | Level-up animation constant/test | `ActionAnimation.LEVEL_UP`, `SM_ACTION_ANIMATION` | `SmActionAnimation.cs`, packet tests | Packet Helper | Yes if isolated | Low-Medium | Useful small prerequisite, but less blocking than execution order. |
| C | NPC faction level-up audit | `NpcFactions.onLevelUp` | docs/read-only | Audit | Yes read-only | Medium | Needed later, but not required to stage ordering. |
| D | Offline GP opt-in adapter | `GloryPointsService.addGp`, `AbyssRankDAO.addGp` | GP service/repository files | Service Port | Yes if docs serialized | Medium | Code-isolated from XP, but not the current XP blocker. |

## Selected Work

The orchestrator implemented Candidate A. No sub-agent was spawned because the selected work was a narrow new service/test pair but owned the central XP ordering and shared progress docs.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/QuestXpExecutionPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestXpExecutionPlanServiceTests.cs`
- `docs/QuestXpReward-Audit.md`
- `docs/QuestRewardSideEffects-Audit.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6UN-Completion.md`

## What Changed

- Added `QuestXpExecutionPlanService.CreatePlan(QuestXpRewardPlan)`.
- Added `QuestXpExecutionPlan`, `QuestXpExecutionDescriptor`, `QuestXpExecutionPlanStatus`, and `QuestXpExecutionAction`.
- Staged Java-order level-change descriptors for ratio, stats, repose, salvation reset, upgrade player, level-up animation, NPC faction level-up, quest callbacks, nearby refresh, guide, skill auto-learn, and custom rewards.
- Preserved Java packet order by placing `SM_STATUPDATE_EXP` metadata after level-change descriptors and before XP message metadata.
- Preserved Java XP-message-before-ascension-warning order by reusing `QuestRewardService.CreateXpSystemMessagePackets`.
- Kept all descriptors non-live.

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "QuestXpExecutionPlanServiceTests" --nologo` | Passed: 3 |
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` | Passed: 1831 |

## Migration Parity Table - UOW-1048

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData.addExp` | `Aion.GameServer.Services.QuestXpExecutionPlanService.CreatePlan`; `QuestXpExecutionPlan` | Reward Execution Plan | Partial | Unit Tested | Partial Parity | Non-live plan preserves reviewed Java order after `addExp`: call `setExp`, then emit XP message metadata and optional ascension warning. It does not mutate XP/repose/salvation, send packets, or persist state. No Java runtime comparison. |
| `com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData.setExp` | `QuestXpExecutionDescriptor(QuestXpExecutionAction.SetExp)`; `QuestXpExecutionAction.StatUpdateExpPacket` | Model Side-Effect Plan | Partial | Unit Tested | Partial Parity | Plan records the Java `setExp` boundary, level change detection, `MinNewLevel`, and `SM_STATUPDATE_EXP` placement after level-change side effects. It does not update `Player.Exp`, `Player.Level`, max repose, recoverable XP, or create/send `SmStatUpdateExp`. |
| `com.aionemu.gameserver.controllers.PlayerController.onLevelChange` | `QuestXpExecutionDescriptor` level-change actions | Controller Side-Effect Plan | Partial | Unit Tested | Partial Parity | Java-order descriptors cover the reviewed side-effect chain. All are metadata only and no live hooks are invoked. |
| `com.aionemu.gameserver.controllers.PlayerController.upgradePlayer` | `QuestXpExecutionAction.UpgradePlayerLifeStats`; `VisualStatsUpdate`; `TeamStatUpdate`; `LegionMemberUpdate` | Controller Side-Effect Dependency | Partial | Unit Tested | Needs Verification | Descriptor split matches reviewed Java order inside `upgradePlayer`. HP/MP synchronization, `SM_STATS_INFO`, team/alliance update, and legion update are not executed here. |
| `com.aionemu.gameserver.model.animations.ActionAnimation.LEVEL_UP`; `com.aionemu.gameserver.network.aion.serverpackets.SM_ACTION_ANIMATION` | `QuestXpExecutionAction.LevelUpAnimationBroadcast`; existing `Aion.GameServer.Network.Aion.ServerPackets.SmActionAnimation` | Packet Dependency | Partial | Unit Tested as metadata | Needs Verification | Plan records Java level-up animation point and notes Java id `0`. C# still lacks a named `LevelUp` constant and no level-up packet serialization assertion was added in this unit. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_STATUPDATE_EXP` | `QuestXpExecutionAction.StatUpdateExpPacket`; existing `Aion.GameServer.Network.Aion.ServerPackets.SmStatUpdateExp` | Packet Dependency | Partial | Unit Tested as metadata | Needs Verification | Plan preserves packet placement after Java level-change side effects and before XP messages. It does not instantiate `SmStatUpdateExp`, compare Java bytes, or send live. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` XP reward variants | `QuestXpExecutionPlan.XpSystemMessagePackets`; `QuestRewardService.CreateXpSystemMessagePackets` | Packet Metadata | Complete helper / Partial integration | Unit Tested | Partial Parity | Plan reuses UOW-1047 packet bridge and preserves XP message before ascension-warning order. No live send or Java golden-byte runtime comparison. |
| `com.aionemu.gameserver.model.gameobjects.player.npcFaction.NpcFactions.onLevelUp` | `QuestXpExecutionAction.NpcFactionLevelUp` | Dependency | Not Started | Unit Tested as metadata | Needs Verification | Dependency is explicitly ordered. C# has faction snapshots/completion paths but no live level-up equivalent. |
| `com.aionemu.gameserver.questEngine.QuestEngine.onLevelChanged` | `QuestXpExecutionAction.QuestLevelChangedCallbacks` | Dependency | Not Started | Unit Tested as metadata | Needs Verification | Dependency is explicitly ordered. C# does not dispatch live level-change quest callbacks from XP execution. |
| `com.aionemu.gameserver.services.SkillLearnService.learnNewSkills` | `QuestXpExecutionAction.SkillAutoLearn`; existing `Aion.GameServer.Services.SkillLearnService.CreateSkillBookPlan` | Dependency | Not Started for auto-learn | Unit Tested as metadata | Needs Verification | Existing C# skill service covers skill-book planning, not Java level-up auto-learn. Missing method remains a parity gap. |
| `com.aionemu.gameserver.services.HTMLService.sendGuideHtml`; `BonusPackService.addPlayerCustomReward`; `FactionPackService.addPlayerCustomReward`; `StarterKitService.onLevelUp` | `QuestXpExecutionAction.GuideHtml`; `BonusPackReward`; `FactionPackReward`; `StarterKitReward` | Dependency | Not Started | Unit Tested as metadata | Needs Verification | Optional Java level-up dependencies are ordered as metadata only. Config checks, spawned-state checks, reward mutation, packet sends, and persistence are not ported. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `QuestXpExecutionPlanServiceTests.CreatePlan_StagesJavaLevelChangeSideEffectsBeforeStatAndXpPackets` | Unit | `PlayerCommonData.addExp`; `PlayerCommonData.setExp`; `PlayerController.onLevelChange`; `ActionAnimation.LEVEL_UP` | Level-change descriptors appear in reviewed Java order before `SM_STATUPDATE_EXP` and XP messages; descriptors are non-live; `MinNewLevel` and level-up animation metadata are recorded. | Source-reviewed Java method order and existing XP packet bridge. | No live mutation, packet send, Java runtime comparison, or real side-effect dependency execution. |
| `QuestXpExecutionPlanServiceTests.CreatePlan_KeepsNoLevelChangePlanInJavaPacketOrder` | Unit | `PlayerCommonData.setExp`; `PlayerCommonData.addExp` | No-level-change XP plans omit level-change descriptors while keeping `setExp`, stat-update descriptor, and XP system-message metadata in order. | Source-reviewed Java `onLevelChange` early return behavior. | Does not verify Java runtime edge cases where `setExp` no-ops because exp is unchanged. |
| `QuestXpExecutionPlanServiceTests.CreatePlan_AppendsAscensionWarningAfterXpMessageAndSkipsGuardedPlans` | Unit | `PlayerCommonData.addExp` guard branches and level-9 ascension warning | Ascension warning descriptor follows XP message metadata and skipped XP plans produce no execution descriptors. | Source-reviewed Java message order plus UOW-1047 packet bridge. | No live send or golden-byte comparison. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- The execution plan is non-live and not connected to quest-finish execution.
- `SM_STATUPDATE_EXP` is metadata only in the new plan; no packet object is created from staged values and no Java byte comparison exists.
- Java level-change side effects are descriptors only: ratio updates, stat template updates, max repose, salvation reset, HP/MP sync, visual stats, team/legion fanout, NPC faction level-up, quest callbacks, nearby refresh, guide HTML, skill auto-learn, custom rewards, and starter kits remain unexecuted.
- Future live XP integration must preserve that level-up side effects happen before quest completion state mutation in `QuestService.finishQuest`.

## Summary Metrics

- Total Java artifacts discovered: 11 in this unit
- Total artifacts ported: 1 non-live XP execution/level-change descriptor plan
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 11
- Total blocked artifacts: 6 blocked/partial categories: Java runtime comparison, live XP execution, `SM_STATUPDATE_EXP` packet creation/send, level-change side-effect dependencies, skill auto-learn, and quest/NPC faction callbacks
- Estimated overall migration completion: Phase 6 remains about 71% complete; this unit deepens XP execution metadata without enabling live mutation.

## Next Work Options

### Recommended Sequential Task

- Task: Add a named `SmActionAnimation.LevelUp` constant with packet regression coverage, then update the XP execution plan note to reference the named constant.
- Why: Java `ActionAnimation.LEVEL_UP` is a small, concrete prerequisite discovered by the level-change plan. It is low-risk, isolated, and strengthens the future level-up animation bridge without live XP mutation.
- Likely files: `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmActionAnimation.cs`, `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`, XP audit/progress/handoff docs.

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Level-up animation constant/test | `SmActionAnimation.cs`, packet tests | Low-Medium | Isolated packet helper prerequisite. |
| B | Visual stats/update-player sub-plan | new service/test files | Medium | Keep non-live and descriptor-only. |
| C | NPC faction level-up audit | read-only Java/docs | Medium | Sidecar analysis only. |
| D | Offline GP opt-in adapter | GP service/repository files | Medium | Code-isolated from XP files; docs serialized. |

## Do Not Parallelize

- `QuestXpExecutionPlanService.cs`: shared XP execution order.
- `QuestRewardService.cs`: shared reward helper surface.
- `docs/PHASE-6-PROGRESS.md` and handoff docs: orchestrator-owned.

## Context Needed By Next Session

Start with this file, `docs/QuestXpReward-Audit.md`, `docs/QuestRewardSideEffects-Audit.md`, `docs/PHASE-6-PROGRESS.md` Session 1048, `QuestXpExecutionPlanService.cs`, `QuestXpExecutionPlanServiceTests.cs`, and the level-change findings in `docs/Phase-6UL-Completion.md`.
