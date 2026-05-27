# Phase 6ALE Completion - Flying-Before-Death Revive State Cleanup

Date: 2026-05-27
Unit of Work: UOW-1481
Status: Complete after validation.

## Scope

Port the narrow Java `PlayerController.onBeforeSpawn` state branch that treats flying-before-death players differently during revive restore.

## Completed Work

- Re-audited Java `PlayerController.onBeforeSpawn`, `PlayerController.onDie`, `CreatureController.onDie`, `Player.getIsFlyingBeforeDeath`, and `PlayerReviveService` revive callers.
- Added `Player.IsFlyingBeforeDeath`.
- Updated `PlayerReviveRestoreService.ApplyReviveRestore` so flying-before-death revive clears `FloatingCorpse`; non-flying revive still clears `Dead`.
- Added kisk revive restore coverage for the floating-corpse branch.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --no-restore --filter "FullyQualifiedName~PlayerReviveRestoreServiceTests|FullyQualifiedName~GameServerConnectionKiskReviveWorkflowTests"`.
- Result: passed 17 tests.

## Migration Parity Table - UOW-1481

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.PlayerController` | `PlayerReviveRestoreService` | Controller / Restore Service | Partial | Unit Tested | Partial Parity | Modeled the `onBeforeSpawn` branch that clears `FLOATING_CORPSE` for flying-before-death players and `DEAD` otherwise, then sets `ACTIVE`. Hit-time boost reset, Panesterra faction clearing, protection task scheduling, and full controller hooks remain pending. |
| `com.aionemu.gameserver.model.gameobjects.player.Player` | `Aion.GameServer.Model.GameObjects.Player` | Model | Partial | Unit Tested | Needs Verification | Added `IsFlyingBeforeDeath` flag. Java flag lifecycle from death through non-kisk revive callers remains broader; C# currently preserves the flag for kisk revive like Java. |
| `com.aionemu.gameserver.controllers.CreatureController` | `PlayerCreatureState` / `PlayerReviveRestoreService` | Controller / State | Partial | Unit Tested | Needs Verification | Java death uses `FLOATING_CORPSE` instead of `DEAD` when `isFlyingBeforeDeath` is true. This unit validates the revive-side cleanup branch, not the full death transition. |
| `com.aionemu.gameserver.services.player.PlayerReviveService` | `PlayerReviveRestoreService` / `GameServerConnection.HandleReviveAsync` | Service / Connection Flow | Partial | Unit Tested / Regression Tested | Partial Parity | Kisk revive restore now observes Java flying-before-death state cleanup. Other revive types that reset `isFlyingBeforeDeath` after fly handling are not ported in this unit. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ApplyKiskReviveRestoreClearsFloatingCorpseForFlyingBeforeDeathLikeOnBeforeSpawn` | Unit / restore state | `PlayerController.onBeforeSpawn`, `PlayerReviveService.kiskRevive` | Flying-before-death kisk revive clears `FloatingCorpse`, sets `Active`, preserves unrelated state, restores HP/MP, and does not reset `IsFlyingBeforeDeath`. | Deterministic C# state assertion from Java source audit. | Does not execute Java runtime or broader fly-controller restoration. |
| Existing `PlayerReviveRestoreServiceTests` and `GameServerConnectionKiskReviveWorkflowTests` | Regression | `PlayerReviveService.revive`, `CM_REVIVE` kisk path | Existing resource restore, no-penalty restore, live aggro clear, movement, and teleport workflow stayed stable. | Focused 17-test validation passed. | Full suite still has unrelated cleanup-seal failures. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Full game-server test suite is still known to have two unrelated stable failures in `GameServerConnectionInventoryExpansionUseItemTests`: `ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate` and `HandleUseItemAsync_ExpExtractMergesRestrictedRewardWithCleanupSealFlag`.
- C# now models the revive-side flying-before-death state branch, but not the full death-side transition that sets `IsFlyingBeforeDeath` and `FloatingCorpse`.
- Non-kisk revive flows that restart flying or reset `isFlyingBeforeDeath` remain unported.
- `PlayerController.onBeforeSpawn` hit-time boost reset, Panesterra faction clearing, `VisibleObjectController.onBeforeSpawn` static geo spawn, and protection active task scheduling remain unsupported.
- State flags are represented in C# bit flags; Java exact state composition around `DEAD`, `ACTIVE`, and `FLOATING_CORPSE` still needs runtime comparison.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped artifact rows in this unit
- Total artifacts ported: 1 player flag plus 1 revive restore state branch
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: Java runtime artifact generation, death-side flying flag transition, non-kisk revive fly restoration/reset, hit-time boost/Panesterra/static-geo/protection task side effects, unrelated full-suite cleanup-seal failures
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: continue `PlayerController.onBeforeSpawn` parity with a non-live protection active task planner.
- Model `TaskId.PROTECTION_ACTIVE`, 60000 ms delay, blinking visual state, cast/target cancellation, and `SM_PLAYER_STATE` fanout from Java `startProtectionActiveTask`.

## Suggested Acceptance Criteria

- Re-audit Java `PlayerController.startProtectionActiveTask` and `stopProtectionActiveTask`.
- Keep live scheduling disabled unless C# task ownership and packet fanout are ready.
- Add focused planner tests and update parity docs.
- Re-run `PlayerStateTests`, any new planner tests, and `GameServerConnectionKiskReviveWorkflowTests` if production revive code is touched.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Protection active task planner | new service/tests plus docs | Low | Can be test/planner-only. |
| B | Death-side flying flag transition | player death model/tests | Medium | Requires auditing current death bridge and packet fanout. |
| C | Inventory cleanup-seal failure triage | inventory item-use tests/services | Medium | Separate current full-suite blocker. |

## Do Not Parallelize

- Shared revive restore service if production wiring is changed.
- Shared kisk revive workflow fixture edits.
- Shared progress/handoff docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1481] Align flying death revive state`.
- Files changed in UOW-1481:
  - `dotnetConversion/src/Aion.GameServer/Model/GameObjects/Player.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerReviveRestoreService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerReviveRestoreServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6ALE-Completion.md`
