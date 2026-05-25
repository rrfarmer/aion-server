# Phase 6UO Completion - UOW-1049 Level-Up Action Animation Constant

Date: May 25, 2026

## Current Phase

Phase 6 remains active. Java is the source of truth. This handoff continues after `docs/Phase-6UN-Completion.md`.

## Last Completed Unit

- UOW-1049: `[Phase 6][UOW-1049] Add level-up action animation constant`
- Recent commits before this unit:
  - `bb3ca5c5d [Phase 6][UOW-1048] Stage quest XP level-change plan`
  - `7034fb641 [Phase 6][UOW-1047] Bridge quest XP message metadata`
  - `fd3974ada [Phase 6][UOW-1046] Add quest XP system messages`
- UOW-1049 status: complete; code, tests, progress notes, parity table, and this next handoff are included in this unit.

## Summary

UOW-1049 adds the named C# packet constant for Java `ActionAnimation.LEVEL_UP(0)` and regression-tests the `SM_ACTION_ANIMATION` level-up payload shape. It also updates the staged XP execution plan note so the level-up descriptor points to the concrete C# prerequisite. No live XP mutation or level-up broadcast was enabled.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Level-up animation constant/test | `ActionAnimation.LEVEL_UP`, `SM_ACTION_ANIMATION`, `PlayerController.onLevelChange` | `SmActionAnimation.cs`, `GamePacketTests.cs`, XP execution test/note | Packet Helper | Yes if isolated | Low-Medium | Clear Java id `0`; isolated packet prerequisite for future level-up broadcast. |
| B | Visual stats/update-player sub-plan | `PlayerController.upgradePlayer` | new service/test files | Service Port | Not with shared docs | Medium | Descriptor-only possible, but broader dependency set. |
| C | NPC faction level-up audit | `NpcFactions.onLevelUp` | read-only/report docs | Java Analysis | Yes read-only | Medium | Useful next analysis, no implementation required. |
| D | Offline GP opt-in adapter | `GloryPointsService.addGp`, `AbyssRankDAO.addGp` | GP service/repository files | Service Port | Yes code-wise | Medium | Separate from XP but docs still serialized. |

## Selected Work

The orchestrator implemented Candidate A sequentially. No sub-agent was spawned because the code change was smaller than the coordination surface and shared documentation remained orchestrator-owned.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmActionAnimation.cs`
- `dotnetConversion/src/Aion.GameServer/Services/QuestXpExecutionPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestXpExecutionPlanServiceTests.cs`
- `docs/QuestXpReward-Audit.md`
- `docs/QuestRewardSideEffects-Audit.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6UO-Completion.md`

## What Changed

- Added `SmActionAnimation.LevelUp = 0`.
- Added a level-up animation serialization assertion using `new SmActionAnimation(1001, SmActionAnimation.LevelUp, 27)`.
- Updated the XP execution-plan level-up descriptor to reference `SmActionAnimation.LevelUp`.
- Updated the XP execution-plan focused test to assert the descriptor now points to the named constant.

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CharacterSelectionServerPackets_WriteJavaShapedPayloads|FullyQualifiedName~QuestXpExecutionPlanServiceTests" --nologo` | Passed: 4 |
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` | Passed: 1831 |

## Migration Parity Table - UOW-1049

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.animations.ActionAnimation.LEVEL_UP` | `Aion.GameServer.Network.Aion.ServerPackets.SmActionAnimation.LevelUp` | Enum Constant / Packet Constant | Complete | Regression Tested | Partial Parity | Java id `0` is now named in C# and serialized through `SM_ACTION_ANIMATION` regression coverage. No Java golden-byte runtime comparison. The broader `ActionAnimation` enum values `UNK`, `REPAIR_GATE`, `CRAFT_LEVEL_UP`, and `CLASS_CHANGE` are still not all named in C#. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ACTION_ANIMATION` | `Aion.GameServer.Network.Aion.ServerPackets.SmActionAnimation` | Packet | Partial | Regression Tested | Partial Parity | Packet payload shape `D/H/D` is regression-tested for both bind-kisk and level-up inputs. No Java golden-byte runtime comparison; live level-up broadcast is not wired from XP execution. |
| `com.aionemu.gameserver.controllers.PlayerController.onLevelChange` | `Aion.GameServer.Services.QuestXpExecutionPlanService` level-up descriptor | Controller Dependency | Partial | Unit Tested as metadata | Needs Verification | Descriptor now references the concrete C# `SmActionAnimation.LevelUp` constant. It still does not broadcast live, execute surrounding level-change side effects, or verify Java runtime packet order. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GamePacketTests.CharacterSelectionServerPackets_WriteJavaShapedPayloads` | Regression | `ActionAnimation.LEVEL_UP`; `SM_ACTION_ANIMATION.writeImpl`; `PlayerController.onLevelChange` | `SmActionAnimation.LevelUp` serializes Java action id `0` with target object id and `newLevel` in `D/H/D` order. | Source-reviewed Java enum id and packet writer, serialized through C# packet writer. | No Java golden-byte runtime comparison and no live broadcast. |
| `QuestXpExecutionPlanServiceTests.CreatePlan_StagesJavaLevelChangeSideEffectsBeforeStatAndXpPackets` | Unit | `PlayerController.onLevelChange`; `ActionAnimation.LEVEL_UP` | XP execution metadata references the concrete `SmActionAnimation.LevelUp` prerequisite. | Source-reviewed Java level-up call plus C# packet constant coverage. | Metadata only; no level-change side effects execute live. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- Level-up animation is still a packet prerequisite only; live XP level-change execution and broadcast are disabled.
- Other Java `ActionAnimation` values remain only partially represented in C#.
- Java level-change side effects are descriptors only: ratio updates, stat template updates, max repose, salvation reset, HP/MP sync, visual stats, team/legion fanout, NPC faction level-up, quest callbacks, nearby refresh, guide HTML, skill auto-learn, custom rewards, and starter kits remain unexecuted.
- Future live XP integration must preserve that level-up side effects happen before quest completion state mutation in `QuestService.finishQuest`.

## Summary Metrics

- Total Java artifacts discovered: 3 in this unit
- Total artifacts ported: 1 named packet constant prerequisite plus 1 descriptor-note bridge
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 3
- Total blocked artifacts: 4 blocked/partial categories: Java runtime comparison, live XP execution, live level-up broadcast, and remaining level-change side-effect dependencies
- Estimated overall migration completion: Phase 6 remains about 71% complete; this unit removes one packet-prerequisite gap without enabling live XP mutation.

## Next Work Options

### Recommended Sequential Task

- Task: Add a focused non-live side-effect sub-plan behind the staged XP execution plan.
- Why: XP reward planning now has rate/message/packet metadata, staged execution order, and the level-up animation packet prerequisite. The next blocker is modeling one Java `PlayerController.onLevelChange` dependency in more detail without live mutation.
- Suggested first slice: `PlayerController.upgradePlayer` metadata for HP/MP sync, visual stats update, team stat update, and legion member update.
- Likely files: a new service/test file or `QuestXpExecutionPlanService.cs` if kept as descriptors only, `QuestXpReward-Audit.md`, `QuestRewardSideEffects-Audit.md`, progress/handoff docs.

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Upgrade-player sub-plan | new service/test files or exclusive `QuestXpExecutionPlanService.cs` | Medium | Keep non-live and descriptor-only; avoid live packet sends. |
| B | NPC faction level-up audit | read-only Java/docs | Medium | Good sidecar analysis before implementation. |
| C | QuestEngine level-change audit | read-only Java/docs | Medium | Analyze handler dispatch order without code edits. |
| D | Offline GP opt-in adapter | GP service/repository files | Medium | Code-isolated from XP files; docs serialized. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Implement upgrade-player sub-plan or next selected code unit | Exact selected C# service/test files plus docs | Any files assigned to a sidecar agent |
| Sidecar A | Read-only NPC faction level-up audit | Read-only Java/C# inspection only | All writes |
| Sidecar B | Read-only QuestEngine level-change audit | Read-only Java/C# inspection only | All writes |

Use sidecars only if the next implementation scope does not require the same files. Shared docs remain orchestrator-owned.

## Do Not Parallelize

- `QuestXpExecutionPlanService.cs`: shared XP execution order.
- `QuestRewardService.cs`: shared reward helper surface.
- `GamePacketTests.cs`: shared packet regression file.
- `docs/PHASE-6-PROGRESS.md` and handoff docs: orchestrator-owned.

## Context Needed By Next Session

Start with this file, `docs/QuestXpReward-Audit.md`, `docs/QuestRewardSideEffects-Audit.md`, `docs/PHASE-6-PROGRESS.md` Session 1049, `QuestXpExecutionPlanService.cs`, `SmActionAnimation.cs`, `QuestXpExecutionPlanServiceTests.cs`, and the level-change findings in `docs/Phase-6UL-Completion.md`.
