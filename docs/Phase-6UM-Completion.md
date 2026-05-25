# Phase 6UM Completion - UOW-1047 XP Plan Message Bridge

Date: May 25, 2026

## Current Phase

Phase 6 remains active. Java is the source of truth. This handoff continues after `docs/Phase-6UL-Completion.md`.

## Last Completed Unit

- UOW-1047: `[Phase 6][UOW-1047] Bridge quest XP message metadata`
- Recent commits:
  - `fd3974ada [Phase 6][UOW-1046] Add quest XP system messages`
  - `35b9c7a0d [Phase 6][UOW-1045] Compose quest XP reward metadata`
  - `6d78fef5b [Phase 6][UOW-1044] Add quest XP reward planner scaffold`
- UOW-1047 status: complete; code, tests, progress notes, parity table, and this next handoff are included in this unit.

## Summary

UOW-1047 adds a non-live bridge from `QuestXpRewardPlan.MessageKind` to concrete XP `SmSystemMessage` packet objects. It preserves Java `PlayerCommonData.addExp` message ordering by producing the XP gain message first and appending the level-9 ascension-limit warning after it when required. The bridge does not send packets and does not perform live XP mutation.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | XP plan-to-message bridge | `PlayerCommonData.addExp`, `SM_SYSTEM_MESSAGE` XP variants | `QuestRewardService.cs`, `QuestRewardServiceTests.cs` | Service Port | No with XP service edits | Medium | Shared XP reward helper surface; small enough for orchestrator. |
| B | Level-change executor/plan | `PlayerCommonData.setExp`, `PlayerController.onLevelChange` | new service/tests or existing XP service files | Service Port | Not with A if same service files | Medium-High | Broad side-effect order and many dependencies. |
| C | Level-up animation constant/test | `SM_ACTION_ANIMATION`, `PlayerController.onLevelChange` | `SmActionAnimation.cs`, packet tests | Packet Helper | Yes if files isolated | Low-Medium | Independent packet helper if no XP execution wiring. |
| D | Offline GP opt-in adapter | `GloryPointsService.addGp`, `AbyssRankDAO.addGp` | GP service/repository files | Service Port | Yes if docs serialized | Medium | Code-isolated from XP. |

## Selected Work

No sub-agent was used for this unit. The bridge touched the shared XP reward helper and its focused tests, so a serialized orchestrator edit was safer.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/QuestRewardService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestRewardServiceTests.cs`
- `docs/QuestXpReward-Audit.md`
- `docs/QuestRewardSideEffects-Audit.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6UM-Completion.md`

## What Changed

- Added `QuestRewardService.CreateXpSystemMessagePackets(QuestXpRewardPlan)`.
- Mapped applied XP plan message kinds to concrete `SmSystemMessage` helpers.
- Appended ascension-limit warning after the XP gain packet when required.
- Returned no packets for skipped/non-applied XP plans.

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "QuestRewardServiceTests" --nologo` | Passed: 19 |
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` | Passed: 1828 |

## Migration Parity Table - UOW-1047

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData.addExp` | `Aion.GameServer.Services.QuestRewardService.CreateXpSystemMessagePackets` | Reward/Packet Bridge | Partial | Unit Tested | Partial Parity | Non-live bridge maps applied XP plan message kinds to concrete XP system-message packets and preserves Java XP-message-before-ascension-warning order. It does not send packets, mutate XP, run `setExp`, or perform live level-change side effects. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` XP reward variants | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` via XP bridge | Packet Dependency | Complete | Unit/Regression Tested | Partial Parity | UOW-1046 message ids/parameter order are reused by the bridge and tested by message id selection. No Java golden-byte runtime comparison or live send. |
| `com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData.setExp`; `com.aionemu.gameserver.controllers.PlayerController.onLevelChange` | `QuestXpRewardPacketIntent.LevelChangeSideEffects` | Level-Change Dependency | Partial | Manual Only | Needs Verification | Bridge deliberately does not execute level mutation or level-change hooks. Java ordering from UOW-1046 remains a future staged executor/plan requirement. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `QuestRewardServiceTests.CreateXpSystemMessagePackets_MapsPlanMessageKindsAndAscensionWarningInJavaOrder` | Unit | `PlayerCommonData.addExp`; `SM_SYSTEM_MESSAGE` XP helpers | Applied named bonus plans map to concrete XP message ids; ascension warnings are appended after the XP message; skipped plans produce no packets. | Source-reviewed Java message selection/order plus UOW-1046 packet helper tests. | Does not send packets live, does not serialize parameter values in this service test, and does not execute `setExp` or level-change hooks. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- XP plan-to-packet bridge is non-live and not connected to quest-finish execution.
- `SM_STATUPDATE_EXP` remains separate and is not emitted from a live XP execution path.
- Java level-change side effects remain unported: visual stats, level-up animation, NPC faction level-up, quest level-change callbacks, nearby refresh, guide/starter-kit, skill auto-learn, custom rewards, and deferred persistence.
- Java level-up side effects run before quest completion state mutation; future C# live integration must preserve this ordering.

## Summary Metrics

- Total Java artifacts discovered: 3 in this unit
- Total artifacts ported: 1 non-live XP plan-to-packet bridge
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 3
- Total blocked artifacts: 3 blocked/partial categories: Java runtime comparison, live XP execution, and level-change side effects
- Estimated overall migration completion: Phase 6 remains about 71% complete; this unit bridges XP message metadata without enabling live reward mutation.

## Next Work Options

### Recommended Sequential Task

- Task: Stage a non-live level-change executor/plan from the UOW-1046 read-only analysis.
- Why: XP reward planning now has rates, repose/salvation, operation metadata, concrete XP messages, and a packet bridge. The next blocker before live XP execution is Java `setExp`/`onLevelChange` side-effect ordering.
- Likely files: a new level-change plan service/test file or existing XP reward service files, `QuestXpReward-Audit.md`, progress/handoff docs.

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Level-change plan scaffold | new service/test files if possible | Medium | Keep non-live and descriptor-only. |
| B | Level-up animation packet constant/test | `SmActionAnimation.cs`, packet tests | Low-Medium | Useful independent prerequisite. |
| C | NPC faction level-up audit | read-only Java/docs | Medium | Sidecar analysis only. |
| D | Offline GP opt-in adapter | GP service/repository files | Medium | Code-isolated from XP files; docs serialized. |

## Do Not Parallelize

- `QuestRewardService.cs`: shared reward helper surface.
- Any future level-change executor service: shared side-effect ordering.
- `docs/PHASE-6-PROGRESS.md` and handoff docs: orchestrator-owned.

## Context Needed By Next Session

Start with this file, `docs/QuestXpReward-Audit.md`, `docs/QuestRewardSideEffects-Audit.md`, `docs/PHASE-6-PROGRESS.md` Session 1047, `QuestRewardService.cs`, `QuestRewardServiceTests.cs`, and the level-change findings in `docs/Phase-6UL-Completion.md`.
