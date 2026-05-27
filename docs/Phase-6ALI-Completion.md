# Phase 6ALI Completion - Player Death Workflow Planner

Date: 2026-05-27
Unit of Work: UOW-1485
Status: Complete after validation.

## Scope

Add a non-live planner for the Java player death workflow that composes the UOW-1484 state transition with the surrounding `PlayerController.onDie` and `CreatureController.onDie` side-effect order.

## Completed Work

- Re-audited Java `PlayerController.onDie`, `PlayerController.scheduleShowResurrectionOptions`, and `CreatureController.onDie`.
- Added `PlayerDeathWorkflowPlanService`.
- Modeled Java ordering for cancel-current-skill, rebirth effect scan, last-attacker master resolution, duel early return, summon release, flying-before-death/state transition placement, `CreatureController.onDie` side effects, resurrection-option scheduling, instance/map-region early returns, reward, XP-loss guard, and quest callback.
- Kept the workflow non-live and explicitly listed unsupported live Java behaviors in the plan.
- Added focused unit tests for full ordinary death ordering, duel early return, instance-handler early return, and map-region early return.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --no-restore --filter "FullyQualifiedName~PlayerDeathWorkflowPlanServiceTests|FullyQualifiedName~PlayerDeathStateTransitionServiceTests|FullyQualifiedName~PlayerReviveRestoreServiceTests"`.
- Result: passed 13 tests.

## Migration Parity Table - UOW-1485

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.PlayerController` | `Aion.GameServer.Services.PlayerDeathWorkflowPlanService` | Controller / Workflow Planner | Partial | Unit Tested | Partial Parity | Models `onDie` control-flow order as non-live metadata, including cancel-current-skill, rebirth scan, duel early return, summon release, state transition placement, resurrection scheduling, instance/map returns, reward, XP-loss guard, and quest callback. Missing live controller mutation, thread scheduling, packet sends, callbacks, rewards, and quest dispatch. |
| `com.aionemu.gameserver.controllers.CreatureController` | `Aion.GameServer.Services.PlayerDeathWorkflowPlanService` / `PlayerDeathStateTransitionService` | Controller / Workflow Planner | Partial | Unit Tested | Partial Parity | Planner places `CreatureController.onDie` side effects after player state cleanup: abort move, clear casting, remove effects, death observers, `SM_EMOTION DIE`, and known-list `stopHating`. These remain planned only except the separate state-transition service. |
| `com.aionemu.gameserver.model.gameobjects.player.Player` | `Aion.GameServer.Model.GameObjects.Player` | Model | Partial | Unit Tested | Partial Parity | Planner reads existing C# player state to predict `IsFlyingBeforeDeath` and `FLOATING_CORPSE` versus `DEAD` outcome without mutating. Runtime death wiring still needs production integration. |
| `com.aionemu.gameserver.services.DuelService` | `Aion.GameServer.Services.PlayerDeathWorkflowPlanService` | Service Dependency | Not Started | Unit Tested via planner metadata | Needs Verification | Duel `loseDuel`, opponent check, and HP/MP restoration are represented as ordered plan steps only. C# live duel service parity was not implemented. |
| `com.aionemu.gameserver.services.summons.SummonsService` | `Aion.GameServer.Services.PlayerDeathWorkflowPlanService` | Service Dependency | Not Started | Unit Tested via planner metadata | Needs Verification | Java `SummonsService.doMode(RELEASE, summon, UNSPECIFIED)` is represented as a conditional plan step only. Live summon release remains unsupported. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_DIE` | `Aion.GameServer.Services.PlayerDeathWorkflowPlanService` | Packet / Scheduler Boundary | Partial | Unit Tested via planner metadata | Needs Verification | Planner records Java's 500ms resurrection-option scheduling intent but does not schedule `ThreadPoolManager` work or send `SM_DIE`; teleport-task suppression is not live. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION` | `Aion.GameServer.Services.PlayerDeathWorkflowPlanService` | Packet / Fanout Boundary | Partial | Unit Tested via planner metadata | Needs Verification | Planner records `SM_EMOTION(DIE)` broadcast placement, but no byte comparison or broadcast recipient verification was performed. |
| `com.aionemu.gameserver.questEngine.QuestEngine` | `Aion.GameServer.Services.PlayerDeathWorkflowPlanService` | Service Dependency | Not Started | Unit Tested via planner metadata | Needs Verification | Quest death callback is represented as the final full-flow step only. Live `QuestEnv` construction and handler dispatch remain unsupported. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `CreatePlan_FlyingPlayerOrdersJavaSideEffectsAroundStateTransition` | Unit / planner | `PlayerController.onDie`, `CreatureController.onDie` | Full ordinary death order with summon release, flying-before-death prediction, `CreatureController` side effects, resurrection scheduling, callbacks, reward, XP-loss, and quest callback. | Deterministic planner assertion from Java source audit. | No live packet fanout, scheduler, observer, effect, summon, reward, XP, or quest execution. |
| `CreatePlan_DuelOpponentKillReturnsBeforeSummonAndSuperOnDieLikeJava` | Unit / planner | `PlayerController.onDie` duel branch | Duel opponent kill returns after duel loss and HP/MP restoration plan, before summon release, state transition, `super.onDie`, resurrection, reward, and quest. | Deterministic Java branch assertion. | Does not execute `DuelService` or life-stat restoration. |
| `CreatePlan_InstanceHandlerReturnStopsBeforeMapRewardAndQuest` | Unit / planner | `PlayerController.onDie` instance callback branch | Instance handler return occurs after resurrection scheduling and before map-region, reward, XP-loss, and quest steps. | Deterministic Java branch assertion. | Instance handler is facts-driven and not invoked. |
| `CreatePlan_MapRegionReturnStopsBeforeRewardAndQuest` | Unit / planner | `PlayerController.onDie` map-region callback branch | Map-region return occurs after instance callback and before reward, XP-loss, and quest steps. | Deterministic Java branch assertion. | Map-region callback is facts-driven and not invoked. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Full game-server test suite is still known to have two unrelated stable failures in `GameServerConnectionInventoryExpansionUseItemTests`: `ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate` and `HandleUseItemAsync_ExpExtractMergesRestrictedRewardWithCleanupSealFlag`.
- Planner is non-live and is not wired into production player damage/death flow.
- Cancel-current-skill, rebirth effect scan, duel handling, summon release, movement abort, casting clear, effect removal, observer callbacks, packet fanout, aggro cleanup, scheduler behavior, instance/map callbacks, rewards, XP-loss, and quest callbacks remain unsupported live behavior.
- Java `ThreadPoolManager.schedule(..., 500)` and teleport-task suppression are represented only as metadata; threading behavior is not ported.
- Java packet serialization and broadcast-recipient order for `SM_EMOTION DIE` and `SM_DIE` were not compared.
- Java effect-controller scans for rebirth and no-death-penalty effects are represented by facts and metadata, not concrete C# effect runtime behavior.

## Summary Metrics

- Total Java artifacts discovered: 8 grouped artifact rows in this unit
- Total artifacts ported: 1 non-live player death workflow planner and 1 focused test suite
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 8 grouped rows
- Total blocked artifacts: Java runtime artifact generation, production death wiring, live duel/summon/effect/observer/fanout/scheduler/callback/reward/quest systems, unrelated full-suite cleanup-seal failures
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: add a live-safe player death adapter that can optionally apply only `PlayerDeathStateTransitionService.Apply` while returning the full `PlayerDeathWorkflowPlanService` metadata for unsupported Java side effects.
- Keep live execution opt-in and state-only.
- Keep packet fanout, scheduler, duel/summon/effect/reward/quest callbacks, instance/map callbacks, and aggro cleanup disabled until supporting runtime surfaces are ready.

## Suggested Acceptance Criteria

- Adapter disabled mode returns the planner result without mutating player state.
- Opt-in live mode applies only the state transition and reports all other Java side effects as planned/not executed.
- Duel opponent early-return mode must not apply state transition, matching Java's early return before summon release and `super.onDie`.
- Instance/map early-return behavior remains planner metadata only.
- Re-run `PlayerDeathWorkflowPlanServiceTests`, new adapter tests, `PlayerDeathStateTransitionServiceTests`, and revive restore tests.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Player death state-only adapter | new service/tests | Medium | Sequential if it mutates `Player` state. |
| B | Protection packet fanout bridge analysis | read-only Java/C# broadcast files | Low | Analysis-only can be parallelized. |
| C | Inventory cleanup-seal failure triage | inventory item-use tests/services | Medium | Separate current full-suite blocker. |

## Do Not Parallelize

- Shared `Player` model.
- Shared death workflow/state transition fixtures.
- Shared progress/handoff docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1485] Add player death workflow planner`.
- Files changed in UOW-1485:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerDeathWorkflowPlanService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerDeathWorkflowPlanServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6ALI-Completion.md`
