# Phase 6ALH Completion - Player Death Flying-State Transition

Date: 2026-05-27
Unit of Work: UOW-1484
Status: Complete after validation.

## Scope

Model the player state portion of Java `PlayerController.onDie` and `CreatureController.onDie`, focused on flying-before-death behavior.

## Completed Work

- Re-audited Java `PlayerController.onDie` and `CreatureController.onDie`.
- Added `PlayerDeathStateTransitionService`.
- Implemented the state transition that records `IsFlyingBeforeDeath` while still flying, clears ride/rest/floating/flying/gliding state, clears fly-state flags, and applies `FLOATING_CORPSE` versus `DEAD`.
- Added focused tests for flying, ordinary, and pre-flagged death transitions.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --no-restore --filter "FullyQualifiedName~PlayerDeathStateTransitionServiceTests|FullyQualifiedName~PlayerStateTests|FullyQualifiedName~PlayerReviveRestoreServiceTests"`.
- Result: passed 31 tests.

## Migration Parity Table - UOW-1484

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.PlayerController` | `PlayerDeathStateTransitionService` | Controller / State Service | Partial | Unit Tested | Partial Parity | Models the state portion of `onDie`: flying-before-death flag, ride/rest/floating/flying/gliding cleanup. Missing cancel-current-skill, rebirth info, duel handling, summon release, effect cleanup, resurrection option scheduling, instance/zone callbacks, rewards, XP loss, and quest callbacks. |
| `com.aionemu.gameserver.controllers.CreatureController` | `PlayerDeathStateTransitionService` | Controller / State Service | Partial | Unit Tested | Partial Parity | Models `CreatureController.onDie` state branch for `FLOATING_CORPSE` versus `DEAD`. Missing movement abort, casting clear, effect removal, death observers, `SM_EMOTION(DIE)` broadcast, and aggro `stopHating` cleanup. |
| `com.aionemu.gameserver.model.gameobjects.player.Player` | `Aion.GameServer.Model.GameObjects.Player` | Model | Partial | Unit Tested | Partial Parity | Uses existing `IsFlyingBeforeDeath`, `CreatureState`, `FlyState`, ride state, and visual state helpers. Java live `PlayerMode`, summon, controller task, known-list, and effect-controller ownership remain broader. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Apply_FlyingPlayerSetsFlyingBeforeDeathAndFloatingCorpseLikeJava` | Unit / state transition | `PlayerController.onDie`, `CreatureController.onDie` | Flying player death sets `IsFlyingBeforeDeath`, clears ride/rest/fly/glide state, clears active, and sets `FloatingCorpse` rather than `Dead`. | Deterministic C# state assertion from Java source audit. | No packet fanout, observers, effect removal, summon release, or known-list aggro cleanup. |
| `Apply_NonFlyingPlayerSetsDeadState` | Unit / state transition | `CreatureController.onDie` | Non-flying player death sets Java `DEAD` state while preserving unrelated flags. | Deterministic Java branch assertion. | Does not cover Java death side effects beyond state. |
| `Apply_PreviouslyFlyingBeforeDeathUsesFloatingCorpseEvenAfterFlyingStateWasCleared` | Unit / state transition | `CreatureController.onDie` | Existing flying-before-death flag still drives `FloatingCorpse` branch even if flying state is already cleared. | Deterministic Java branch assertion. | Runtime reachability depends on caller ordering. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Full game-server test suite is still known to have two unrelated stable failures in `GameServerConnectionInventoryExpansionUseItemTests`: `ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate` and `HandleUseItemAsync_ExpExtractMergesRestrictedRewardWithCleanupSealFlag`.
- Service is not wired into production player damage/death flow.
- Packet fanout (`SM_EMOTION DIE`, `SM_DIE` resurrection options), observer callbacks, effect removal, movement abort, casting clear, known-list aggro cleanup, summon release, duel branches, instance/zone callbacks, rewards, XP loss, and quest callbacks remain unsupported.
- Java state flag composition still needs runtime comparison, especially around exact `DEAD`, `ACTIVE`, and `FLOATING_CORPSE` combinations.
- Ride cleanup is modeled by clearing C# ride state, but Java `PlayerMode.RIDE` side effects and packet fanout are broader.

## Summary Metrics

- Total Java artifacts discovered: 3 grouped artifact rows in this unit
- Total artifacts ported: 1 death state transition service and 1 focused test suite
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 3 grouped rows
- Total blocked artifacts: Java runtime artifact generation, production death wiring, death packet fanout, observers/effects/known-list cleanup, duel/summon/reward/quest branches, unrelated full-suite cleanup-seal failures
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: add a non-live player death workflow planner that composes the state transition with planned missing side effects in Java order.
- Include: cancel current skill, rebirth info, duel/summon branches, transition state, schedule resurrection options, instance/zone callbacks, reward/XP-loss, and quest callbacks.

## Suggested Acceptance Criteria

- Keep the workflow non-live unless existing C# services can safely execute one side effect.
- Include explicit planned gaps for packet fanout, observer callbacks, effect cleanup, known-list aggro cleanup, summons, duel handling, rewards, and quest callbacks.
- Add tests verifying Java order and the flying/non-flying state transition is positioned correctly.
- Re-run `PlayerDeathStateTransitionServiceTests`, new workflow tests, and revive restore tests.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Player death workflow planner | new service/tests | Medium | Sequential if it composes shared state transition service. |
| B | Protection packet fanout bridge analysis | read-only Java/C# broadcast files | Low | Analysis-only can be parallelized. |
| C | Inventory cleanup-seal failure triage | inventory item-use tests/services | Medium | Separate current full-suite blocker. |

## Do Not Parallelize

- Shared `Player` model.
- Shared death/kisk death workflow fixtures.
- Shared progress/handoff docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1484] Add player death state transition`.
- Files changed in UOW-1484:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerDeathStateTransitionService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerDeathStateTransitionServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6ALH-Completion.md`
