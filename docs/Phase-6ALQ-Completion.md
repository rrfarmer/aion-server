# Phase 6ALQ Completion - Player Death State Phase Metadata

Date: 2026-05-27
Unit of Work: UOW-1493
Status: Complete after validation.

## Scope

Refine `PlayerDeathStateTransitionService` metadata so Java `PlayerController.onDie` pre-super cleanup and Java `CreatureController.onDie` death-state selection are represented as distinct phases.

## Completed Work

- Re-audited Java `PlayerController.onDie` pre-super state cleanup and Java `CreatureController.onDie` death-state selection.
- Extended `PlayerDeathStateTransitionResult` with `PhasePlans`.
- Added `PlayerDeathStateTransitionPhase` and `PlayerDeathStateTransitionPhasePlan`.
- Separated phase metadata:
  - `PlayerControllerPreSuperCleanup`: flying-before-death detection, flag set, ride/rest/floating cleanup, flying/gliding state cleanup, and fly-state cleanup.
  - `CreatureControllerDeathStateSelection`: `FLOATING_CORPSE` versus `DEAD` branch after Java core side effects.
- Kept existing live mutation behavior unchanged.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --no-restore --filter "FullyQualifiedName~PlayerDeathStateTransitionServiceTests|FullyQualifiedName~PlayerDeathWorkflowAdapterServiceTests|FullyQualifiedName~PlayerDeathWorkflowPlanServiceTests|FullyQualifiedName~PlayerReviveRestoreServiceTests"`.
- Result: passed 17 tests.

## Migration Parity Table - UOW-1493

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.PlayerController` | `Aion.GameServer.Services.PlayerDeathStateTransitionService` | Controller / State Service | Partial | Unit Tested | Partial Parity | Phase metadata now separates PlayerController pre-super cleanup from CreatureController state selection. Live behavior still performs the same combined state mutation. Missing cancel-current-skill, rebirth scan, duel handling, summon release, scheduler/callback/reward/quest behavior. |
| `com.aionemu.gameserver.controllers.CreatureController` | `Aion.GameServer.Services.PlayerDeathStateTransitionService` | Controller / State Service | Partial | Unit Tested | Partial Parity | Phase metadata now identifies CreatureController death-state selection (`FLOATING_CORPSE`/`DEAD`) separately from PlayerController cleanup. Movement abort, casting clear, effect removal, observers, packet fanout, and aggro cleanup remain separate non-live metadata elsewhere. |
| `com.aionemu.gameserver.model.gameobjects.player.Player` | `Aion.GameServer.Model.GameObjects.Player` | Model | Partial | Unit Tested | Partial Parity | Existing live state outcomes are preserved: flying-before-death flag, ride/rest/fly/glide cleanup, fly-state cleanup, and final `FLOATING_CORPSE`/`DEAD` state. Exact Java runtime state/HP composition remains unverified. |
| `com.aionemu.gameserver.model.gameobjects.state.CreatureState` | `Aion.GameServer.Model.GameObjects.PlayerCreatureState` | Enum / State Flags | Partial | Unit Tested | Needs Verification | Phase tests cover relevant state flags, but no Java runtime state-bit comparison was generated in this unit. Multibit `DEAD`/`FLOATING_CORPSE` semantics remain carefully tracked. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Apply_FlyingPlayerSetsFlyingBeforeDeathAndFloatingCorpseLikeJava` | Unit / state transition | `PlayerController.onDie`, `CreatureController.onDie` | Existing flying death outcome is preserved and phase metadata exposes pre-super cleanup separately from `FLOATING_CORPSE` selection. | Deterministic Java source audit and C# state assertion. | No runtime Java comparison or packet/observer/effect side effects. |
| `Apply_NonFlyingPlayerSetsDeadState` | Unit / state transition | `CreatureController.onDie` | Existing ordinary death outcome is preserved; non-flying phase metadata omits flying flag set and selects `DEAD`. | Deterministic Java branch assertion. | No live core side effects. |
| `Apply_PreviouslyFlyingBeforeDeathUsesFloatingCorpseEvenAfterFlyingStateWasCleared` | Unit / state transition | `CreatureController.onDie` | Existing pre-flagged floating-corpse outcome is preserved and phase metadata records CreatureController `FLOATING_CORPSE` branch. | Deterministic Java branch assertion. | Runtime reachability still depends on caller ordering. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Full game-server test suite is still known to have two unrelated stable failures in `GameServerConnectionInventoryExpansionUseItemTests`: `ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate` and `HandleUseItemAsync_ExpExtractMergesRestrictedRewardWithCleanupSealFlag`.
- Phase metadata is not yet composed into `PlayerDeathWorkflowPlanService`.
- Live mutation is still combined in one service call; this unit only separates metadata and test assertions.
- Exact Java state flag composition still needs runtime comparison, especially around multibit `DEAD`, `ACTIVE`, and `FLOATING_CORPSE` interactions.
- Ride cleanup is modeled by clearing C# ride state, but Java `PlayerMode.RIDE` side effects and packet fanout are broader.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped artifact rows in this unit
- Total artifacts ported: phase-specific death state metadata in 1 existing service plus focused test updates
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: Java runtime artifact generation, production death wiring, state byte/runtime comparison, broader death side effects, unrelated full-suite cleanup-seal failures
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: compose `PlayerDeathStateTransitionResult.PhasePlans` into `PlayerDeathWorkflowPlanService` / `PlayerDeathWorkflowAdapterService` metadata.
- Workflow plans should be able to name PlayerController pre-super cleanup separately from CreatureController death-state selection.
- Keep live mutation behavior unchanged.

## Suggested Acceptance Criteria

- Workflow metadata exposes distinct state phase plans when Java reaches `super.onDie`.
- Duel opponent early-return plans leave state phase metadata absent.
- Adapter exposes phase metadata while still only mutating state when opt-in live state mutation is enabled.
- Existing state transition tests continue to pass.
- Re-run death state transition, workflow planner, adapter, and revive restore tests.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Compose state phase metadata into workflow | death workflow planner/adapter/tests | Medium | Sequential because it touches shared workflow result shape. |
| B | Protection packet fanout bridge analysis or planner | read-only first, then new service/tests if scoped | Low-Medium | Keep separate from death workflow files. |
| C | Inventory cleanup-seal failure triage | inventory item-use tests/services | Medium | Separate current full-suite blocker. |

## Do Not Parallelize

- Shared death workflow/state transition/scheduler/fanout/core-effect fixtures when changing workflow result shape.
- Shared progress/handoff docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1493] Split death state phase metadata`.
- Files changed in UOW-1493:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerDeathStateTransitionService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerDeathStateTransitionServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6ALQ-Completion.md`
