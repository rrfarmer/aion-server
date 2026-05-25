# Phase 6US Completion - UOW-1053 Nearby Quest Empty-Packet Intent

Date: May 25, 2026

## Current Phase

Phase 6 remains active. Java is the source of truth. This handoff continues after `docs/Phase-6UR-Completion.md`.

## Last Completed Unit

- UOW-1053: `[Phase 6][UOW-1053] Align nearby quest empty packet intent`
- Recent commits before this unit:
  - `4cd70dd3c [Phase 6][UOW-1052] Stage QuestEngine level-change callbacks`
  - `a381c58bf [Phase 6][UOW-1051] Stage NPC faction level-up plan`
  - `4bb8f37b1 [Phase 6][UOW-1050] Stage upgrade-player level-change plan`
- UOW-1053 status: complete; code, tests, progress notes, parity table, and this next handoff are included in this unit.

## Summary

UOW-1053 tightens C# parity for Java `PlayerController.updateNearbyQuests`. Java always sends `SM_NEARBY_QUESTS` after filtering world quest ids, even when the resulting map is empty. C# now records packet-send intent for empty-world and all-rejected nearby quest plans while keeping missing context as guarded non-live blockers.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Nearby quest empty packet intent | `PlayerController.updateNearbyQuests`, `SM_NEARBY_QUESTS` | `NearbyQuestRefreshPlanService.cs`, nearby refresh tests, docs | Service Plan Correction | Selected sequential | Low-Medium | Direct Java divergence: Java sends empty packet maps, C# previously suppressed packet intent. |
| B | Guide HTML audit | `HTMLService.sendGuideHtml` | read-only Java/C# inspection and docs | Java Analysis | Yes read-only | Medium | Next Java-order dependency after nearby refresh. |
| C | Skill auto-learn audit | `SkillLearnService.learnNewSkills` | read-only Java/C# inspection and docs | Java Analysis | Yes read-only | Medium | Important next side effect but broader than this unit. |
| D | Compose existing sub-plans into XP execution metadata | `PlayerController.onLevelChange` | `QuestXpExecutionPlanService.cs`, XP tests | Plan Composition | No | Medium | Shared XP execution order should remain orchestrator-owned. |

## Selected Work

The orchestrator implemented Candidate A sequentially. No sub-agent was spawned because the selected work touched shared nearby quest planning behavior and shared migration docs.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/NearbyQuestRefreshPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/NearbyQuestRefreshPlanServiceTests.cs`
- `docs/QuestXpReward-Audit.md`
- `docs/QuestRewardSideEffects-Audit.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6US-Completion.md`

## What Changed

- Updated `NearbyQuestRefreshPlan.WouldSendPacket`.
- `Ready`, `NoWorldQuestIds`, and `NoMarkers` now report send intent.
- Missing world instance and missing quest template table remain non-sending blockers.
- Added a dedicated regression for Java empty nearby quest packet intent.

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "NearbyQuestRefreshPlanServiceTests" --nologo` | Passed: 5 |
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` | Passed: 1839 |

## Migration Parity Table - UOW-1053

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.PlayerController.updateNearbyQuests` | `Aion.GameServer.Services.NearbyQuestRefreshPlanService.CreatePlan`; `NearbyQuestRefreshPlan.WouldSendPacket` | Controller Packet-Intent Plan | Partial | Unit Tested | Partial Parity | C# now records packet-send intent for Java empty-map sends when there are no world quest ids or no accepted markers. It still does not send live, resolve player map region directly, or compose into XP level-change execution. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_NEARBY_QUESTS` | `Aion.GameServer.Network.Aion.ServerPackets.SmNearbyQuests`; `NearbyQuestMarker` | Packet | Partial | Regression Tested from prior units; Unit Tested as planner intent here | Partial Parity | Existing packet writer covers empty and marker payloads. This unit aligns planner intent with Java's unconditional send. No Java golden-byte runtime comparison. |
| `com.aionemu.gameserver.services.QuestService.checkStartConditions` | `NearbyQuestStartConditionService.CheckNearbyStartConditions` | Service Dependency | Partial | Unit Tested via nearby refresh tests | Needs Verification | Existing C# start-condition coverage is reused. Unsupported XML/inventory/repeat timing dependencies remain explicit rejection reasons. |
| `com.aionemu.gameserver.services.QuestService.getLevelRequirementDiff` | `NearbyQuestStartConditionService.GetLevelRequirementDiff` | Service Dependency | Partial | Unit Tested via nearby refresh tests | Needs Verification | C# computes marker transparency diff for accepted markers. Edge cases still depend on quest template parity and real Java runtime comparison. |
| `com.aionemu.gameserver.world.WorldMapInstance.getQuestIds` | `Aion.GameServer.World.WorldMapInstanceRuntimeState.QuestIds` | World Runtime Dependency | Partial | Unit Tested in existing runtime tests | Needs Verification | Existing runtime state supplies quest ids to the planner. Live map-region lookup from `player.getPosition().getMapRegion().getParent()` is not wired into this non-live plan. |
| `com.aionemu.gameserver.controllers.PlayerController.onLevelChange` | `QuestXpExecutionPlanService`; `NearbyQuestRefreshPlanService` | Controller Dependency | Partial | Unit Tested as metadata | Needs Verification | UOW-1048 staged the full level-change order; UOW-1053 aligns nearby refresh packet intent. The plan is not composed into XP execution and live XP level-change execution remains disabled. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `NearbyQuestRefreshPlanServiceTests.CreatePlan_ReturnsNoWorldQuestIdsWithoutSending` | Unit update | `PlayerController.updateNearbyQuests`; `SM_NEARBY_QUESTS.writeImpl` | No world quest ids now still report packet intent with an empty marker list. | Source-reviewed Java unconditional packet send and existing C# packet writer. | No live socket send or Java runtime packet trace. |
| `NearbyQuestRefreshPlanServiceTests.CreatePlan_ReturnsNoMarkersWhenAllQuestIdsAreRejected` | Unit update | `PlayerController.updateNearbyQuests`; `QuestService.checkStartConditions` | All rejected quest ids now still report packet intent with an empty marker list. | Source-reviewed Java empty-map send after filtering. | Rejection reasons are C# metadata; no Java runtime comparison. |
| `NearbyQuestRefreshPlanServiceTests.CreatePlan_MatchesJavaEmptyNearbyQuestPacketIntent` | Unit | `PlayerController.updateNearbyQuests`; `SM_NEARBY_QUESTS` | Dedicated coverage for both empty-world and all-rejected empty-packet intent. | Source-reviewed Java method plus deterministic C# plan assertions. | Does not send live packet or validate map-region lookup. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- Nearby quest refresh remains a non-live plan and is not composed into `QuestXpExecutionPlan`.
- Missing world instance and missing quest template table are guarded C# planner blockers; Java assumes live position/map-region/static quest data are available and would fail differently if they were missing.
- Unsupported XML start conditions, inventory item conditions, repeat timing, and broader `QuestService.checkStartConditions` details remain partial.
- Live `SM_NEARBY_QUESTS` sends, socket ordering, and map-region lookup remain disabled.
- Java level-change side effects outside nearby refresh remain descriptors only: guide HTML, skill auto-learn, custom rewards, and starter kits.

## Summary Metrics

- Total Java artifacts discovered: 6 in this unit
- Total artifacts ported: 1 nearby quest refresh packet-intent parity correction
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6
- Total blocked artifacts: 5 blocked/partial categories: Java runtime comparison, live XP composition, live nearby packet send, full QuestService start-condition parity, and live map-region lookup
- Estimated overall migration completion: Phase 6 remains about 71% complete; this unit tightens one nearby-refresh prerequisite without enabling live XP mutation.

## Next Work Options

### Recommended Sequential Task

- Task: Audit/stage the next Java-order level-change dependency: `HTMLService.sendGuideHtml` or `SkillLearnService.learnNewSkills`.
- Why: nearby quest packet intent is now aligned at the planning layer. The remaining Java-order level-change dependencies are guide HTML, skill auto-learn, bonus/faction custom rewards, and starter kit.
- Suggested first slice: inspect guide HTML config/spawned-state behavior or skill auto-learn side effects, then add an audit or non-live descriptor plan.
- Likely files: read-only Java/C# inspection plus `docs/QuestXpReward-Audit.md`, `docs/QuestRewardSideEffects-Audit.md`, progress/handoff docs. Keep live XP execution disabled.

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Guide HTML audit | read-only Java/C# inspection, docs owned by orchestrator | Medium | Next Java-order dependency after nearby refresh. |
| B | Skill auto-learn audit | read-only Java/C# inspection, docs owned by orchestrator | Medium | Broad dependency; likely needs a planner later. |
| C | Compose existing sub-plans into XP metadata | `QuestXpExecutionPlanService.cs`, XP tests | Medium | Sequential only; shared XP execution order. |
| D | Faction leave system-message helper | `SmSystemMessage.cs`, packet tests | Low-Medium | Isolated packet prerequisite for future live NPC faction plan execution. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Audit/stage guide HTML or skill auto-learn | Exact selected docs/code files | Any files assigned to a sidecar agent |
| Sidecar A | Read-only guide HTML behavior audit | Read-only Java/C# inspection only | All writes |
| Sidecar B | Read-only skill auto-learn behavior audit | Read-only Java/C# inspection only | All writes |

Use sidecars only when the implementation scope does not require the same files. Shared docs remain orchestrator-owned.

## Do Not Parallelize

- `QuestXpExecutionPlanService.cs`: shared XP execution order.
- `NearbyQuestRefreshPlanService.cs`: shared nearby refresh planner.
- `docs/PHASE-6-PROGRESS.md`, audit docs, and handoff docs: orchestrator-owned.

## Context Needed By Next Session

Start with this file, `docs/QuestXpReward-Audit.md`, `docs/QuestRewardSideEffects-Audit.md`, `docs/PHASE-6-PROGRESS.md` Session 1053, `NearbyQuestRefreshPlanService.cs`, `NearbyQuestRefreshPlanServiceTests.cs`, `QuestXpExecutionPlanService.cs`, Java `HTMLService.sendGuideHtml`, and Java `SkillLearnService.learnNewSkills`.
