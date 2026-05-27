# Phase 6ALG Completion - Protection Visual-State Adapter

Date: 2026-05-27
Unit of Work: UOW-1483
Status: Complete after validation.

## Scope

Add an opt-in adapter for the live-safe visual-state subset of Java protection active task behavior.

## Completed Work

- Added `PlayerProtectionActiveTaskAdapterService`.
- Live start can set `PlayerVisualStates.Blinking` only when explicitly requested.
- Live stop can call `Player.StopProtectionActive()` only when explicitly requested and the caller supplies `isSpawned = true`.
- Scheduler mutation, `SM_PLAYER_STATE` fanout, cast cancellation, target removal, and AI move notification remain planned metadata only.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --no-restore --filter "FullyQualifiedName~PlayerProtectionActiveTaskAdapterServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskPlanServiceTests|FullyQualifiedName~PlayerStateTests"`.
- Result: passed 32 tests.

## Migration Parity Table - UOW-1483

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.PlayerController` | `PlayerProtectionActiveTaskAdapterService` / `PlayerProtectionActiveTaskPlanService` | Controller / Adapter Service | Partial | Unit Tested | Partial Parity | Adapter can opt-in to Java visual-state mutation for start/stop protection, while preserving planner metadata for scheduler, cast/target cancellation, packet fanout, and AI notification gaps. |
| `com.aionemu.gameserver.model.gameobjects.player.Player` | `Aion.GameServer.Model.GameObjects.Player` | Model | Partial | Unit Tested | Partial Parity | Live adapter uses existing `SetVisualState(BLINKING)` and `StopProtectionActive()` helpers. C# still lacks live `isSpawned` ownership and full controller task map. |
| `com.aionemu.gameserver.model.TaskId` | `PlayerProtectionActiveTaskPlanService` / adapter result metadata | Enum / Task Metadata | Partial | Unit Tested | Needs Verification | Adapter explicitly reports `MutatedScheduler = false`; `TaskId.PROTECTION_ACTIVE` is still metadata only. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PLAYER_STATE` | `SmPlayerState` / adapter result metadata | Packet / Fanout Boundary | Partial | Unit Tested | Needs Verification | Adapter reports `SentPackets = false`; fanout remains planned and byte/runtime comparison was not performed. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Apply_DisabledExposesStartPlanWithoutVisualMutation` | Unit / adapter | `PlayerController.startProtectionActiveTask` | Disabled adapter exposes a start plan without mutating visual state. | Deterministic adapter assertion from Java source audit and C# planner. | No live scheduler/fanout. |
| `Apply_LiveStartSetsBlinkingButLeavesSchedulerAndPacketsPlanned` | Unit / adapter | `PlayerController.startProtectionActiveTask` | Opt-in live start sets blinking while scheduler and packet sends remain false/planned. | Deterministic C# state mutation plus planner metadata. | Does not cancel casts or remove targets. |
| `Apply_LiveStartAlreadyProtectedDoesNotMutate` | Unit / adapter | `PlayerController.startProtectionActiveTask` | Already blinking start remains a no-op. | Deterministic Java branch assertion. | No concurrency coverage. |
| `Apply_LiveStopClearsBlinkingButLeavesSchedulerAndPacketsPlanned` | Unit / adapter | `PlayerController.stopProtectionActiveTask` | Opt-in spawned stop clears blinking while scheduler and packet sends remain false/planned. | Deterministic C# state mutation plus planner metadata. | Does not send `SM_PLAYER_STATE` or notify AI. |
| `Apply_LiveStopUnspawnedDoesNotClearBlinking` | Unit / adapter | `PlayerController.stopProtectionActiveTask` | Unspawned stop leaves visual state unchanged while preserving task-cancel metadata. | Deterministic Java spawned-branch assertion. | C# lacks live `Player.isSpawned`. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Full game-server test suite is still known to have two unrelated stable failures in `GameServerConnectionInventoryExpansionUseItemTests`: `ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate` and `HandleUseItemAsync_ExpExtractMergesRestrictedRewardWithCleanupSealFlag`.
- Adapter is opt-in and not wired into production revive/teleport paths.
- Scheduler mutation is explicitly not live; there is no `TaskId.PROTECTION_ACTIVE` runtime owner yet.
- `SM_PLAYER_STATE` fanout, sighted-player recipient selection, cast cancellation, target removal, and `notifyAIOnMove` remain planned metadata only.
- Spawned-state handling is caller-supplied because C# does not yet model Java `Player.isSpawned`.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped artifact rows in this unit
- Total artifacts ported: 1 opt-in visual-state adapter and 1 focused test suite
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: Java runtime artifact generation, live task owner, `SM_PLAYER_STATE` fanout, cast/target cancellation, AI notification, spawned-state model, unrelated full-suite cleanup-seal failures
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: move to the death-side flying flag transition.
- Audit current C# death bridge and add a non-live or opt-in model that mirrors Java `PlayerController.onDie` setting `IsFlyingBeforeDeath` and Java `CreatureController.onDie` using `FLOATING_CORPSE` instead of `DEAD` for flying-before-death players.

## Suggested Acceptance Criteria

- Inspect Java `PlayerController.onDie` and `CreatureController.onDie` again before edits.
- Identify current C# death workflow files and avoid production packet fanout unless already safely modeled.
- Add focused tests for the state transition from flying/dead into `IsFlyingBeforeDeath` + `FloatingCorpse`.
- Re-run relevant player state/revive/kisk death workflow tests.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Death-side flying flag transition | player death model/service/tests | Medium | Sequential if it touches shared player state. |
| B | Protection packet fanout bridge analysis | read-only Java/C# broadcast files | Low | Analysis-only can be parallelized. |
| C | Inventory cleanup-seal failure triage | inventory item-use tests/services | Medium | Separate current full-suite blocker. |

## Do Not Parallelize

- Shared `Player` model.
- Shared death/kisk death workflow fixtures.
- Shared progress/handoff docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1483] Add protection visual adapter`.
- Files changed in UOW-1483:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerProtectionActiveTaskAdapterService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerProtectionActiveTaskAdapterServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6ALG-Completion.md`
