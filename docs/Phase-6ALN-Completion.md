# Phase 6ALN Completion - Player Death Fanout Workflow Composition

Date: 2026-05-27
Unit of Work: UOW-1490
Status: Complete after validation.

## Scope

Compose the non-live death emotion fanout and known-list aggro cleanup plan into the player death workflow planner and adapter metadata.

## Completed Work

- Extended `PlayerDeathWorkflowFacts` with `LastAttackerObjectId` and `KnownCreatureObjectIds` metadata.
- Extended `PlayerDeathWorkflowPlan` with nullable `DeathEmotionFanoutPlan`.
- Composed `PlayerDeathEmotionFanoutPlanService` into workflow metadata whenever Java reaches the `CreatureController.onDie` death emotion point.
- Preserved Java duel opponent early-return behavior by leaving `DeathEmotionFanoutPlan` null when the workflow returns before summon release and `super.onDie`.
- Adapter results expose the composed fanout plan through `result.Plan` while still reporting `SentPackets = false` and no live aggro mutation.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --no-restore --filter "FullyQualifiedName~PlayerDeathEmotionFanoutPlanServiceTests|FullyQualifiedName~PlayerDeathWorkflowAdapterServiceTests|FullyQualifiedName~PlayerDeathWorkflowPlanServiceTests|FullyQualifiedName~PlayerDeathResurrectionOptionsPlanServiceTests"`.
- Result: passed 15 tests.

## Migration Parity Table - UOW-1490

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.PlayerController` | `Aion.GameServer.Services.PlayerDeathWorkflowPlanService` / `PlayerDeathWorkflowAdapterService` | Controller / Workflow Planner | Partial | Unit Tested | Partial Parity | Workflow facts now carry last-attacker and known-creature metadata into the composed death fanout plan. Duel opponent early return still omits fanout metadata because Java returns before `super.onDie`. Live cancel-current-skill, duel/summon/effect/reward/quest behavior remains unsupported. |
| `com.aionemu.gameserver.controllers.CreatureController` | `Aion.GameServer.Services.PlayerDeathWorkflowPlanService` / `PlayerDeathEmotionFanoutPlanService` | Controller / Workflow Planner | Partial | Unit Tested | Partial Parity | Workflow metadata now exposes concrete observer/broadcast/known-list cleanup plan at the Java `CreatureController.onDie` point. Movement abort, casting clear, effect removal, observer callbacks, packet broadcast, and aggro mutation remain non-live. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION` | `Aion.GameServer.Network.Aion.ServerPackets.SmEmotion` / composed planner metadata | Packet / Planner Metadata | Partial | Unit Tested | Needs Verification | Composed workflow exposes `SM_EMOTION(DIE)` target id and opcode metadata. No packet byte comparison or recipient-order validation was performed. |
| `com.aionemu.gameserver.model.EmotionType` | `Aion.GameServer.Model.EmotionType` / composed planner metadata | Enum / Packet Metadata | Partial | Unit Tested | Partial Parity | Composed workflow carries `EmotionType.Die` id `18`. Full enum parity remains outside this unit. |
| `com.aionemu.gameserver.controllers.attack.AggroList` | `Aion.GameServer.Services.PlayerDeathEmotionFanoutPlanService` / composed workflow metadata | Service Dependency / Aggro Metadata | Not Started | Unit Tested via planner metadata | Needs Verification | Known-creature `stopHating(owner)` remains per-creature intent metadata supplied by workflow facts. Live known-list iteration and aggro mutation are not implemented. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `CreatePlan_FlyingPlayerOrdersJavaSideEffectsAroundStateTransition` | Unit / planner | `PlayerController.onDie`, `CreatureController.onDie` | Ordinary death workflow includes a composed fanout plan with attacker target id and known-creature cleanup intents. | Deterministic Java order and metadata assertion. | No live packet send or aggro mutation. |
| `CreatePlan_DuelOpponentKillReturnsBeforeSummonAndSuperOnDieLikeJava` | Unit / planner | `PlayerController.onDie` duel branch | Duel opponent early return leaves `DeathEmotionFanoutPlan` null. | Deterministic Java branch assertion. | No live duel service. |
| `CreatePlan_InstanceHandlerReturnStopsBeforeMapRewardAndQuest` | Unit / planner | `PlayerController.onDie`, `CreatureController.onDie` | Instance return still includes death fanout plan and self-death target id zero because Java returns after `super.onDie`. | Deterministic Java branch assertion. | No live instance callback or packet fanout. |
| `CreatePlan_MapRegionReturnStopsBeforeRewardAndQuest` | Unit / planner | `PlayerController.onDie`, `CreatureController.onDie` | Map return includes death fanout metadata before the map early return. | Deterministic Java branch assertion. | Teleport/fanout facts remain caller supplied. |
| `Apply_DisabledExposesWorkflowPlanWithoutMutatingPlayer` | Unit / adapter | `PlayerController.onDie`, `CreatureController.onDie` | Disabled adapter exposes the composed fanout plan while leaving player state unchanged. | Deterministic C# no-mutation assertion. | No live side effects. |
| `Apply_LiveStateOnlyMutationAppliesDeathTransitionAndLeavesSideEffectsPlanned` | Unit / adapter | `PlayerController.onDie`, `CreatureController.onDie` | Opt-in adapter exposes fanout metadata while only mutating player death state. | Deterministic C# state mutation plus planner metadata. | No live packets, scheduler, callbacks, aggro cleanup, rewards, or quest. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Full game-server test suite is still known to have two unrelated stable failures in `GameServerConnectionInventoryExpansionUseItemTests`: `ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate` and `HandleUseItemAsync_ExpExtractMergesRestrictedRewardWithCleanupSealFlag`.
- Composition is non-live and not wired into production player damage/death flow.
- Last-attacker object id and known-creature ids are caller-supplied metadata; live object identity, known-list filtering, and concurrency are not ported.
- `SM_EMOTION(DIE)` packet bytes and broadcast recipient ordering remain unverified.
- Live observer callbacks, packet broadcast, and `AggroList.stopHating(owner)` mutation remain unsupported.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: composed death emotion fanout metadata in 2 existing death workflow surfaces plus focused test updates
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: Java runtime artifact generation, live packet fanout, live known-list/aggro-list mutation, observer callback runtime, production death wiring, unrelated full-suite cleanup-seal failures
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: add a non-live death core side-effect plan for the remaining Java `CreatureController.onDie` pre-fanout operations.
- Include movement abort, casting clear, and effect removal before observer/fanout metadata.
- Keep all side effects non-live until movement/casting/effect runtime surfaces are ready.

## Suggested Acceptance Criteria

- Planner records Java order: abort move, clear casting, remove all effects.
- Planner composes or can later compose before state/fanout metadata in `PlayerDeathWorkflowPlanService`.
- Tests cover ordering before observer/fanout and non-live flags.
- Re-run new core side-effect plan tests plus death workflow planner/adapter tests.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Death core side-effect planner | new service/tests | Low-Medium | New non-live metadata files; later composition touches death workflow records. |
| B | Protection packet fanout bridge analysis or planner | read-only first, then new service/tests if scoped | Low-Medium | Keep separate from death workflow files. |
| C | Inventory cleanup-seal failure triage | inventory item-use tests/services | Medium | Separate current full-suite blocker. |

## Do Not Parallelize

- Shared death workflow/state transition/scheduler/fanout fixtures if composing core side effects into workflow.
- Shared progress/handoff docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1490] Compose death fanout plan`.
- Files changed in UOW-1490:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerDeathWorkflowPlanService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerDeathWorkflowPlanServiceTests.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerDeathWorkflowAdapterServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6ALN-Completion.md`
