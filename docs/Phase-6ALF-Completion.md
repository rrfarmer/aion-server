# Phase 6ALF Completion - Protection Active Task Planner

Date: 2026-05-27
Unit of Work: UOW-1482
Status: Complete after validation.

## Scope

Add a non-live planner for Java `PlayerController.startProtectionActiveTask` and `stopProtectionActiveTask` behavior.

## Completed Work

- Re-audited Java `PlayerController.startProtectionActiveTask`, `PlayerController.stopProtectionActiveTask`, `TaskId.PROTECTION_ACTIVE`, and `SM_PLAYER_STATE`.
- Added `PlayerProtectionActiveTaskPlanService`.
- Modeled start-protection metadata: blinking visual state, cast cancellation, target removal, `SM_PLAYER_STATE` fanout, `TaskId.PROTECTION_ACTIVE`, and 60000 ms task scheduling.
- Modeled stop-protection metadata: task cancellation and spawned-only blinking clear, `SM_PLAYER_STATE` fanout, and AI move notification.
- Kept the planner non-live; no production scheduler or broadcast wiring was changed.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --no-restore --filter "FullyQualifiedName~PlayerProtectionActiveTaskPlanServiceTests|FullyQualifiedName~PlayerStateTests|FullyQualifiedName~GameServerConnectionKiskReviveWorkflowTests"`.
- Result: passed 38 tests.

## Migration Parity Table - UOW-1482

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.PlayerController` | `Aion.GameServer.Services.PlayerProtectionActiveTaskPlanService` | Controller / Planner Service | Partial | Unit Tested | Partial Parity | Start/stop protection task side-effect order is modeled as non-live metadata. Live `ThreadPoolManager` scheduling, `AttackUtil.cancelCastOn`, `AttackUtil.removeTargetFrom`, broadcast delivery, and `notifyAIOnMove` execution remain unsupported. |
| `com.aionemu.gameserver.model.TaskId` | `PlayerProtectionActiveTaskPlanService` constants | Enum / Task Metadata | Partial | Unit Tested | Partial Parity | Captures `TaskId.PROTECTION_ACTIVE` ordinal `3` and 60000 ms delay. C# does not yet have a general Java `TaskId` enum or live task owner for this slot. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PLAYER_STATE` | `Aion.GameServer.Network.Aion.ServerPackets.SmPlayerState` / planner metadata | Packet / Fanout Metadata | Partial | Unit Tested | Needs Verification | Planner records `SmPlayerState` as the broadcast packet type for start/stop while spawned. Packet serialization exists elsewhere; this unit did not compare Java packet bytes or broadcast recipient lists. |
| `com.aionemu.gameserver.model.gameobjects.player.Player` | `Aion.GameServer.Model.GameObjects.Player` | Model | Partial | Unit Tested | Needs Verification | Existing `VisualState`, `IsProtectionActive`, and `StopProtectionActive` support the modeled blinking state. Planner does not mutate live player state in this unit. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `CreateStartPlan_ModelsJavaProtectionTaskStart` | Unit / planner | `PlayerController.startProtectionActiveTask` | Start plan records blinking visual state, cast cancellation, target removal, `SM_PLAYER_STATE` broadcast, `TaskId.PROTECTION_ACTIVE`, and 60000 ms scheduling. | Deterministic planner assertion from Java source audit. | No live task scheduling, cast cancellation, target removal, or packet fanout. |
| `CreateStartPlan_NoOpsWhenPlayerAlreadyBlinking` | Unit / planner | `PlayerController.startProtectionActiveTask` | Already protected players produce a no-op plan. | Deterministic Java branch assertion. | Does not inspect concurrent state changes. |
| `CreateStopPlan_ModelsJavaSpawnedStopAndBroadcast` | Unit / planner | `PlayerController.stopProtectionActiveTask` | Spawned stop records task cancel, blinking clear, `SM_PLAYER_STATE`, and AI move notification. | Deterministic planner assertion from Java source audit. | No live scheduler or broadcast delivery. |
| `CreateStopPlan_UnspawnedOnlyCancelsRepresentedTask` | Unit / planner | `PlayerController.stopProtectionActiveTask` | Unspawned stop records only represented task cancellation. | Deterministic Java branch assertion. | C# lacks a live `isSpawned` player model. |
| `CreateStopPlan_SpawnedStillBroadcastsWhenTaskOrBlinkingIsAlreadyMissing` | Unit / planner | `PlayerController.stopProtectionActiveTask` | Spawned stop still records Java's broadcast/AI-notify branch even when represented task/blinking is absent. | Conservative Java source audit. | Runtime relevance depends on caller reachability. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Full game-server test suite is still known to have two unrelated stable failures in `GameServerConnectionInventoryExpansionUseItemTests`: `ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate` and `HandleUseItemAsync_ExpExtractMergesRestrictedRewardWithCleanupSealFlag`.
- Planner-only metadata does not execute live `ThreadPoolManager` scheduling or task cancellation.
- `AttackUtil.cancelCastOn`, `AttackUtil.removeTargetFrom`, broadcast-to-sighted-player recipient selection, and `notifyAIOnMove` are not live.
- C# does not yet have a general `TaskId.PROTECTION_ACTIVE` runtime owner or player `isSpawned` property equivalent.
- `SM_PLAYER_STATE` byte parity and broadcast ordering were not compared against Java runtime output in this unit.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped artifact rows in this unit
- Total artifacts ported: 1 non-live protection active task planner and 1 focused test suite
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: Java runtime artifact generation, live protection task scheduling/cancellation, cast/target cancellation execution, broadcast recipient fanout, AI move notification, missing player spawned state, unrelated full-suite cleanup-seal failures
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: continue protection parity by adding a live-safe bridge or adapter that can apply the non-scheduling visual-state portion only when explicitly requested.
- Candidate live-safe scope: set `PlayerVisualStates.Blinking` on start and call `Player.StopProtectionActive()` on stop, while leaving scheduler, cast/target cancellation, packet fanout, and AI notification disabled/planned.

## Suggested Acceptance Criteria

- Keep the bridge opt-in and explicit; do not wire it into production revive/teleport automatically unless packet ordering and task ownership are ready.
- Preserve planner metadata for unsupported side effects.
- Add tests showing live state mutation is separate from scheduler/broadcast gaps.
- Re-run `PlayerProtectionActiveTaskPlanServiceTests`, `PlayerStateTests`, and any adapter tests.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Protection visual-state adapter | new adapter service/tests | Low | Can stay opt-in and avoid production connection flow. |
| B | Death-side flying flag transition | player death model/tests | Medium | Requires auditing current death bridge and packet fanout. |
| C | Inventory cleanup-seal failure triage | inventory item-use tests/services | Medium | Separate current full-suite blocker. |

## Do Not Parallelize

- Shared `Player` model if the adapter changes production state APIs.
- Shared `GameServerConnection` revive/teleport flow.
- Shared progress/handoff docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1482] Add protection task planner`.
- Files changed in UOW-1482:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerProtectionActiveTaskPlanService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerProtectionActiveTaskPlanServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6ALF-Completion.md`
