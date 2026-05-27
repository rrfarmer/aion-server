# Phase 6ALL Completion - Player Death Scheduler Plan Composition

Date: 2026-05-27
Unit of Work: UOW-1488
Status: Complete after validation.

## Scope

Compose the non-live resurrection-options scheduler plan into the player death workflow planner and adapter metadata.

## Completed Work

- Extended `PlayerDeathWorkflowFacts` with `HasTeleportTaskAtResurrectionOptionsCallback`.
- Extended `PlayerDeathWorkflowPlan` with nullable `ResurrectionOptionsPlan`.
- Composed `PlayerDeathResurrectionOptionsPlanService` into ordinary death, instance-return, and map-return workflow metadata whenever Java reaches `scheduleShowResurrectionOptions`.
- Preserved Java duel opponent early-return behavior by leaving `ResurrectionOptionsPlan` null when the workflow returns before summon release and `super.onDie`.
- Adapter results expose the composed scheduler plan through `result.Plan` while still reporting `ScheduledTasks = false` and `SentPackets = false`.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --no-restore --filter "FullyQualifiedName~PlayerDeathResurrectionOptionsPlanServiceTests|FullyQualifiedName~PlayerDeathWorkflowAdapterServiceTests|FullyQualifiedName~PlayerDeathWorkflowPlanServiceTests|FullyQualifiedName~PlayerReviveRestoreServiceTests"`.
- Result: passed 18 tests.

## Migration Parity Table - UOW-1488

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.PlayerController` | `Aion.GameServer.Services.PlayerDeathWorkflowPlanService` / `PlayerDeathWorkflowAdapterService` / `PlayerDeathResurrectionOptionsPlanService` | Controller / Workflow Planner | Partial | Unit Tested | Partial Parity | Workflow metadata now includes the concrete resurrection-options scheduler plan whenever Java reaches `scheduleShowResurrectionOptions`. Duel opponent early return leaves the scheduler plan absent. Live scheduler, packet send, callbacks, reward, XP, and quest behavior remain unsupported. |
| `com.aionemu.gameserver.controllers.CreatureController` | `Aion.GameServer.Services.PlayerDeathWorkflowPlanService` / `PlayerDeathWorkflowAdapterService` | Controller / Workflow Planner | Partial | Unit Tested | Partial Parity | Composition preserves the Java order: player pre-super branches, `super.onDie` side effects, then resurrection-options scheduling before instance/map/reward/quest. Non-state `CreatureController` side effects remain planned only. |
| `com.aionemu.gameserver.model.TaskId` | `PlayerDeathWorkflowFacts` / `PlayerDeathResurrectionOptionsPlanService` constants | Enum / Task Metadata | Partial | Unit Tested | Partial Parity | Workflow facts now carry teleport-task presence at the resurrection-options callback boundary. Java live controller task map is still not ported. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_DIE` | `Aion.GameServer.Network.Aion.ServerPackets.SmDie` / composed planner metadata | Packet / Planner Metadata | Partial | Unit Tested | Needs Verification | Composed workflow exposes `SM_DIE` send intent and skip reasons through `ResurrectionOptionsPlan`. No live send or byte comparison was performed. |
| `com.aionemu.gameserver.model.gameobjects.player.Player` | `Aion.GameServer.Model.GameObjects.Player` | Model | Partial | Unit Tested | Needs Verification | Tests now provide zero-HP life stats when modeling ordinary Java death plans so the composed scheduler plan follows Java's life-stat dead guard. Exact runtime state/HP ordering remains unverified. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `CreatePlan_FlyingPlayerOrdersJavaSideEffectsAroundStateTransition` | Unit / planner | `PlayerController.onDie`, `scheduleShowResurrectionOptions` | Ordinary death workflow includes a concrete `ResurrectionOptionsPlan` with `SendSmDie` status after the scheduling step. | Deterministic Java order and scheduler guard assertion. | No live timer or packet send. |
| `CreatePlan_DuelOpponentKillReturnsBeforeSummonAndSuperOnDieLikeJava` | Unit / planner | `PlayerController.onDie` duel branch | Duel opponent early return leaves `ResurrectionOptionsPlan` null. | Deterministic Java branch assertion. | No live duel service. |
| `CreatePlan_InstanceHandlerReturnStopsBeforeMapRewardAndQuest` | Unit / planner | `PlayerController.onDie` instance branch | Instance return still includes `ResurrectionOptionsPlan` because Java schedules before instance callback return. | Deterministic Java branch assertion. | No live instance callback. |
| `CreatePlan_MapRegionReturnStopsBeforeRewardAndQuest` | Unit / planner | `PlayerController.onDie` map branch | Map return includes `ResurrectionOptionsPlan`; supplied teleport-task metadata suppresses `SM_DIE`. | Deterministic Java branch assertion. | Teleport task presence remains caller-supplied. |
| `Apply_DisabledExposesWorkflowPlanWithoutMutatingPlayer` | Unit / adapter | `PlayerController.onDie` | Disabled adapter exposes the composed scheduler plan while leaving player state unchanged. | Deterministic C# no-mutation assertion. | No live side effects. |
| `Apply_LiveStateOnlyMutationAppliesDeathTransitionAndLeavesSideEffectsPlanned` | Unit / adapter | `PlayerController.onDie`, `CreatureController.onDie` | Opt-in adapter exposes the composed scheduler plan while only mutating player death state. | Deterministic C# state mutation plus planner metadata. | No live scheduler, packets, callbacks, rewards, or quest. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Full game-server test suite is still known to have two unrelated stable failures in `GameServerConnectionInventoryExpansionUseItemTests`: `ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate` and `HandleUseItemAsync_ExpExtractMergesRestrictedRewardWithCleanupSealFlag`.
- Composition is non-live and not wired into production player damage/death flow.
- Teleport-task presence is caller-supplied metadata; Java controller task ownership, future scheduling, cancellation, and callback concurrency are not ported.
- `SM_DIE` and `SM_EMOTION DIE` byte serialization and recipient ordering remain unverified.
- Live cancel-current-skill, rebirth scan, duel/summon/effect/observer/aggro cleanup, instance/map callbacks, reward, XP-loss, and quest dispatch remain unsupported.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: composed resurrection-options metadata in 2 existing death workflow surfaces plus focused test updates
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: Java runtime artifact generation, live scheduler/task-owner behavior, live packet send/fanout, production death wiring, broader death side effects, unrelated full-suite cleanup-seal failures
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: add a non-live death emotion fanout plan for Java `CreatureController.onDie`.
- Model `SM_EMOTION(owner, EmotionType.DIE, 0, owner.equals(lastAttacker) ? 0 : lastAttacker.getObjectId())`.
- Include ordering after death observers and before known-list `stopHating` cleanup.
- Keep packet broadcast and known-list mutation disabled until recipient and aggro-list surfaces are ready.

## Suggested Acceptance Criteria

- Planner records `EmotionType.DIE`, action id `0`, and attacker object id or zero for self-death.
- Planner records broadcast intent through `PacketSendUtility.broadcastPacketAndReceive`.
- Planner records known-list aggro cleanup after the death emotion broadcast.
- Tests cover ordinary attacker id, self-death attacker id zero, and explicit ordering relative to observer notification and known-list cleanup.
- Re-run new fanout plan tests plus death workflow planner/adapter tests.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Death emotion fanout/aggro cleanup planner | new service/tests | Low-Medium | New non-live metadata files; later composition will touch death workflow records. |
| B | Protection packet fanout bridge analysis | read-only Java/C# broadcast files | Low | Analysis-only can be parallelized. |
| C | Inventory cleanup-seal failure triage | inventory item-use tests/services | Medium | Separate current full-suite blocker. |

## Do Not Parallelize

- Shared death workflow/state transition/scheduler fixtures if composing fanout into workflow.
- Shared progress/handoff docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1488] Compose death resurrection plan`.
- Files changed in UOW-1488:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerDeathWorkflowPlanService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerDeathWorkflowPlanServiceTests.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerDeathWorkflowAdapterServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6ALL-Completion.md`
