# Phase 6ALO Completion - Player Death Core Side-Effect Planner

Date: 2026-05-27
Unit of Work: UOW-1491
Status: Complete after validation.

## Scope

Add a non-live planner for Java `CreatureController.onDie` pre-state/fanout operations: movement abort, casting clear, and effect removal.

## Completed Work

- Re-audited Java `CreatureController.onDie`.
- Added `PlayerDeathCoreSideEffectPlanService`.
- Modeled Java order: `getMoveController().abortMove()`, `setCasting(null)`, `getEffectController().removeAllEffects()`.
- Kept movement, casting, and effect runtime mutation disabled.
- Added focused tests for side-effect intent flags and ordering.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --no-restore --filter "FullyQualifiedName~PlayerDeathCoreSideEffectPlanServiceTests|FullyQualifiedName~PlayerDeathEmotionFanoutPlanServiceTests|FullyQualifiedName~PlayerDeathWorkflowAdapterServiceTests|FullyQualifiedName~PlayerDeathWorkflowPlanServiceTests"`.
- Result: passed 13 tests.

## Migration Parity Table - UOW-1491

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.CreatureController` | `Aion.GameServer.Services.PlayerDeathCoreSideEffectPlanService` | Controller / Side-Effect Planner | Partial | Unit Tested | Partial Parity | Models the first three Java `onDie` side effects in order: abort movement, clear casting, remove all effects. Does not execute movement abort, casting mutation, effect cleanup, state transition, observer callbacks, packet fanout, or aggro cleanup. |
| `com.aionemu.gameserver.controllers.movement.CreatureMoveController` | `Aion.GameServer.Services.PlayerDeathCoreSideEffectPlanService` | Service Dependency / Movement Metadata | Not Started | Unit Tested via planner metadata | Needs Verification | Java `getMoveController().abortMove()` is represented as intent only. C# live movement controller parity and packet consequences are not implemented here. |
| `com.aionemu.gameserver.model.gameobjects.Creature` | `Aion.GameServer.Services.PlayerDeathCoreSideEffectPlanService` | Model Dependency / Casting Metadata | Partial | Unit Tested via planner metadata | Needs Verification | Java `setCasting(null)` is represented as intent only. C# casting state ownership and live cancellation behavior are not ported in this unit. |
| `com.aionemu.gameserver.controllers.effect.EffectController` | `Aion.GameServer.Services.PlayerDeathCoreSideEffectPlanService` | Service Dependency / Effect Metadata | Not Started | Unit Tested via planner metadata | Needs Verification | Java `removeAllEffects()` is represented as intent only. Live effect removal, stat recalculation, packet fanout, and observer consequences remain unsupported. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `CreatePlan_ModelsJavaPreStateSideEffectsWithoutMutatingRuntime` | Unit / planner | `CreatureController.onDie` | Plan records movement abort, casting clear, and effect removal while reporting no live mutation. | Deterministic Java source audit and C# metadata assertion. | No live side effects. |
| `CreatePlan_OrdersAbortMoveBeforeClearCastingBeforeEffectRemoval` | Unit / planner | `CreatureController.onDie` | Java side-effect order is abort move, clear casting, remove effects. | Deterministic Java source-order assertion. | Does not compose into full workflow yet. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Full game-server test suite is still known to have two unrelated stable failures in `GameServerConnectionInventoryExpansionUseItemTests`: `ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate` and `HandleUseItemAsync_ExpExtractMergesRestrictedRewardWithCleanupSealFlag`.
- Planner is non-live and not composed into the broader player death workflow yet.
- Movement abort, casting clear, and effect removal runtime surfaces are not implemented or verified here.
- Java effect cleanup can trigger broader stat, packet, observer, and persistence side effects not represented beyond metadata.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped artifact rows in this unit
- Total artifacts ported: 1 non-live death core side-effect planner and 1 focused test suite
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: Java runtime artifact generation, live movement/casting/effect runtime surfaces, production death wiring, unrelated full-suite cleanup-seal failures
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: compose `PlayerDeathCoreSideEffectPlanService` into `PlayerDeathWorkflowPlanService` / `PlayerDeathWorkflowAdapterService` metadata.
- The core side-effect plan should appear before state transition and fanout metadata when Java reaches `CreatureController.onDie`.
- Duel opponent early-return plans should leave the core side-effect plan absent.
- Keep movement/casting/effect behavior non-live.

## Suggested Acceptance Criteria

- Workflow plan includes nullable or explicit core side-effect plan when Java reaches `super.onDie`.
- Duel opponent early-return plans do not include the core side-effect plan.
- Adapter exposes the composed core side-effect plan while reporting movement/casting/effect mutation disabled.
- Tests cover ordinary death, instance/map early returns, and duel early return.
- Re-run core side-effect planner tests plus death workflow planner/adapter tests.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Compose death core side effects into workflow | death workflow planner/adapter/tests | Medium | Sequential because it touches shared death workflow records. |
| B | Protection packet fanout bridge analysis or planner | read-only first, then new service/tests if scoped | Low-Medium | Keep separate from death workflow files. |
| C | Inventory cleanup-seal failure triage | inventory item-use tests/services | Medium | Separate current full-suite blocker. |

## Do Not Parallelize

- Shared death workflow/state transition/scheduler/fanout/core-effect fixtures if composing into workflow.
- Shared progress/handoff docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1491] Add death core side-effect planner`.
- Files changed in UOW-1491:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerDeathCoreSideEffectPlanService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerDeathCoreSideEffectPlanServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6ALO-Completion.md`
