# Phase 6ALP Completion - Player Death Core Workflow Composition

Date: 2026-05-27
Unit of Work: UOW-1492
Status: Complete after validation.

## Scope

Compose the non-live death core side-effect plan into the player death workflow planner and adapter metadata.

## Completed Work

- Extended `PlayerDeathWorkflowPlan` with nullable `CoreSideEffectPlan`.
- Composed `PlayerDeathCoreSideEffectPlanService` into workflow metadata whenever Java reaches `CreatureController.onDie`.
- Preserved Java duel opponent early-return behavior by leaving `CoreSideEffectPlan` null when the workflow returns before `super.onDie`.
- Adapter results expose the composed core side-effect plan through `result.Plan` while still reporting no live movement, casting, or effect mutation.
- Documented a boundary nuance: existing `PlayerDeathStateTransitionService` bundles PlayerController pre-super state cleanup with CreatureController death-state selection, so exact split-phase ordering remains a future refinement before live wiring.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --no-restore --filter "FullyQualifiedName~PlayerDeathCoreSideEffectPlanServiceTests|FullyQualifiedName~PlayerDeathEmotionFanoutPlanServiceTests|FullyQualifiedName~PlayerDeathWorkflowAdapterServiceTests|FullyQualifiedName~PlayerDeathWorkflowPlanServiceTests|FullyQualifiedName~PlayerDeathResurrectionOptionsPlanServiceTests"`.
- Result: passed 17 tests.

## Migration Parity Table - UOW-1492

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.PlayerController` | `Aion.GameServer.Services.PlayerDeathWorkflowPlanService` / `PlayerDeathWorkflowAdapterService` | Controller / Workflow Planner | Partial | Unit Tested | Partial Parity | Workflow metadata now exposes the core `CreatureController.onDie` side-effect plan when Java reaches `super.onDie`; duel opponent early return omits it. Existing `PlayerDeathStateTransitionService` still bundles PlayerController pre-super state cleanup with CreatureController death-state selection, so exact split-phase ordering remains a documented gap. |
| `com.aionemu.gameserver.controllers.CreatureController` | `Aion.GameServer.Services.PlayerDeathWorkflowPlanService` / `PlayerDeathCoreSideEffectPlanService` | Controller / Workflow Planner | Partial | Unit Tested | Partial Parity | Workflow metadata now includes abort move, clear casting, and remove effects intent before observer/fanout metadata. These side effects remain non-live. |
| `com.aionemu.gameserver.controllers.movement.CreatureMoveController` | `Aion.GameServer.Services.PlayerDeathCoreSideEffectPlanService` / composed workflow metadata | Service Dependency / Movement Metadata | Not Started | Unit Tested via planner metadata | Needs Verification | Movement abort remains intent metadata only. No live movement controller mutation or movement packet side effects. |
| `com.aionemu.gameserver.model.gameobjects.Creature` | `Aion.GameServer.Services.PlayerDeathCoreSideEffectPlanService` / composed workflow metadata | Model Dependency / Casting Metadata | Partial | Unit Tested via planner metadata | Needs Verification | Java `setCasting(null)` remains intent metadata only. C# casting ownership and live cancellation behavior are not implemented here. |
| `com.aionemu.gameserver.controllers.effect.EffectController` | `Aion.GameServer.Services.PlayerDeathCoreSideEffectPlanService` / composed workflow metadata | Service Dependency / Effect Metadata | Not Started | Unit Tested via planner metadata | Needs Verification | Java `removeAllEffects()` remains intent metadata only. Effect stat recalculation, observer consequences, packet fanout, and persistence side effects are unsupported. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `CreatePlan_FlyingPlayerOrdersJavaSideEffectsAroundStateTransition` | Unit / planner | `PlayerController.onDie`, `CreatureController.onDie` | Ordinary death workflow includes a composed core side-effect plan and orders abort move, clear casting, remove effects before observer notification. | Deterministic Java source-order assertion within current composed workflow model. | Existing state-transition service bundles pre-super and super state pieces. |
| `CreatePlan_DuelOpponentKillReturnsBeforeSummonAndSuperOnDieLikeJava` | Unit / planner | `PlayerController.onDie` duel branch | Duel opponent early return leaves `CoreSideEffectPlan` null. | Deterministic Java branch assertion. | No live duel service. |
| `CreatePlan_InstanceHandlerReturnStopsBeforeMapRewardAndQuest` | Unit / planner | `PlayerController.onDie`, `CreatureController.onDie` | Instance return includes core side-effect metadata because Java returns after `super.onDie`. | Deterministic Java branch assertion. | No live instance callback or movement/effect mutation. |
| `CreatePlan_MapRegionReturnStopsBeforeRewardAndQuest` | Unit / planner | `PlayerController.onDie`, `CreatureController.onDie` | Map return includes core side-effect metadata before map early return. | Deterministic Java branch assertion. | Runtime facts remain caller supplied. |
| `Apply_DisabledExposesWorkflowPlanWithoutMutatingPlayer` | Unit / adapter | `PlayerController.onDie`, `CreatureController.onDie` | Disabled adapter exposes composed core side-effect metadata without mutating player state. | Deterministic C# no-mutation assertion. | No live side effects. |
| `Apply_LiveStateOnlyMutationAppliesDeathTransitionAndLeavesSideEffectsPlanned` | Unit / adapter | `PlayerController.onDie`, `CreatureController.onDie` | Opt-in adapter exposes core side-effect metadata while mutating only player death state. | Deterministic C# state mutation plus planner metadata. | No live movement, casting, effects, packets, callbacks, aggro cleanup, rewards, or quest. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Full game-server test suite is still known to have two unrelated stable failures in `GameServerConnectionInventoryExpansionUseItemTests`: `ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate` and `HandleUseItemAsync_ExpExtractMergesRestrictedRewardWithCleanupSealFlag`.
- Composition is non-live and not wired into production player damage/death flow.
- Existing death state transition service collapses Java's PlayerController pre-super state cleanup and CreatureController death-state selection into one state operation; exact phase split still needs refinement before live wiring.
- Movement abort, casting clear, and effect removal runtime surfaces remain unsupported.
- Effect removal can trigger broader Java stat, observer, packet, and persistence side effects not represented beyond metadata.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: composed death core side-effect metadata in 2 existing death workflow surfaces plus focused test updates
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: Java runtime artifact generation, live movement/casting/effect runtime surfaces, production death wiring, split-phase death state refinement, unrelated full-suite cleanup-seal failures
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: refine the death state planning boundary.
- Split current bundled `PlayerDeathStateTransitionService` metadata into distinct Java phases:
  - PlayerController pre-super flying/ride/rest cleanup.
  - CreatureController post-effect `FLOATING_CORPSE`/`DEAD` selection.
- Keep live mutation behavior unchanged until tests prove the split metadata preserves current state outcomes.

## Suggested Acceptance Criteria

- Add phase-specific plan/result metadata without changing existing live state outcomes.
- Existing `PlayerDeathStateTransitionServiceTests` continue to pass.
- New tests prove ordinary/flying/pre-flagged state outcomes are preserved while exposing separate Java phase steps.
- Workflow planner can refer to phase-specific metadata in a later UOW.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Split death state metadata phases | death state transition service/tests | Medium | Sequential because it changes shared state result shape. |
| B | Protection packet fanout bridge analysis or planner | read-only first, then new service/tests if scoped | Low-Medium | Keep separate from death workflow files. |
| C | Inventory cleanup-seal failure triage | inventory item-use tests/services | Medium | Separate current full-suite blocker. |

## Do Not Parallelize

- Shared death workflow/state transition/scheduler/fanout/core-effect fixtures when changing state result shape.
- Shared progress/handoff docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1492] Compose death core side-effect plan`.
- Files changed in UOW-1492:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerDeathWorkflowPlanService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerDeathWorkflowPlanServiceTests.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerDeathWorkflowAdapterServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6ALP-Completion.md`
