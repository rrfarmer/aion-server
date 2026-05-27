# Phase 6ALJ Completion - Player Death State-Only Adapter

Date: 2026-05-27
Unit of Work: UOW-1486
Status: Complete after validation.

## Scope

Add a live-safe adapter for the player death workflow that exposes the full non-live plan but can optionally apply only the already-modeled death state transition.

## Completed Work

- Added `PlayerDeathWorkflowAdapterService`.
- Disabled mode returns `PlayerDeathWorkflowPlanService` metadata without mutating player state.
- Opt-in live mode applies only `PlayerDeathStateTransitionService.Apply` when the planned Java workflow reaches `super.onDie`.
- Duel opponent early-return plans do not mutate state, matching Java's return before summon release and `super.onDie`.
- Instance-handler early-return plans still apply state transition because Java returns after `super.onDie`.
- Packet fanout, scheduler work, external callbacks, rewards, summons, duel service, effect cleanup, aggro cleanup, and quest dispatch remain planned/not executed.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --no-restore --filter "FullyQualifiedName~PlayerDeathWorkflowAdapterServiceTests|FullyQualifiedName~PlayerDeathWorkflowPlanServiceTests|FullyQualifiedName~PlayerDeathStateTransitionServiceTests|FullyQualifiedName~PlayerReviveRestoreServiceTests"`.
- Result: passed 17 tests.

## Migration Parity Table - UOW-1486

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.PlayerController` | `Aion.GameServer.Services.PlayerDeathWorkflowAdapterService` / `PlayerDeathWorkflowPlanService` | Controller / Adapter Service | Partial | Unit Tested | Partial Parity | Adapter can opt in to only the state-transition portion of Java `onDie` after workflow planning. It respects the duel opponent early return before summon release and `super.onDie`. Live cancel-current-skill, rebirth scan, duel service, summon release, scheduler, callbacks, rewards, XP-loss, and quest dispatch remain unsupported. |
| `com.aionemu.gameserver.controllers.CreatureController` | `Aion.GameServer.Services.PlayerDeathWorkflowAdapterService` / `PlayerDeathStateTransitionService` | Controller / Adapter Service | Partial | Unit Tested | Partial Parity | Adapter invokes only the previously modeled state branch from `CreatureController.onDie` through `PlayerDeathStateTransitionService.Apply`. Movement abort, casting clear, effect removal, observers, `SM_EMOTION DIE`, and known-list `stopHating` remain planned only. |
| `com.aionemu.gameserver.model.gameobjects.player.Player` | `Aion.GameServer.Model.GameObjects.Player` | Model | Partial | Unit Tested | Partial Parity | Opt-in adapter mutates existing C# player death state (`IsFlyingBeforeDeath`, ride/rest/fly/glide cleanup, `FLOATING_CORPSE`/`DEAD`). Runtime death integration and exact Java state composition still need broader verification. |
| `com.aionemu.gameserver.services.DuelService` | `Aion.GameServer.Services.PlayerDeathWorkflowAdapterService` | Service Dependency | Not Started | Unit Tested via adapter metadata | Needs Verification | Duel opponent early return is respected so state is not mutated, but live `DuelService.loseDuel` and HP/MP restoration are not implemented. |
| `com.aionemu.gameserver.services.summons.SummonsService` | `Aion.GameServer.Services.PlayerDeathWorkflowAdapterService` | Service Dependency | Not Started | Unit Tested via adapter metadata | Needs Verification | Summon release remains planned only; adapter does not execute `SummonsService.doMode(RELEASE, ...)`. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_DIE` | `Aion.GameServer.Services.PlayerDeathWorkflowAdapterService` | Packet / Scheduler Boundary | Partial | Unit Tested via adapter metadata | Needs Verification | Adapter reports `ScheduledTasks = false` and `SentPackets = false`; Java 500ms resurrection-option scheduling and teleport-task suppression remain unimplemented. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION` | `Aion.GameServer.Services.PlayerDeathWorkflowAdapterService` | Packet / Fanout Boundary | Partial | Unit Tested via adapter metadata | Needs Verification | Death emotion packet placement remains planner metadata only; no byte or recipient-order comparison. |
| `com.aionemu.gameserver.questEngine.QuestEngine` | `Aion.GameServer.Services.PlayerDeathWorkflowAdapterService` | Service Dependency | Not Started | Unit Tested via adapter metadata | Needs Verification | Quest death callback remains planned only and is not dispatched. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Apply_DisabledExposesWorkflowPlanWithoutMutatingPlayer` | Unit / adapter | `PlayerController.onDie` | Disabled adapter returns the plan without changing flying state or `IsFlyingBeforeDeath`. | Deterministic C# no-mutation assertion around Java-planned workflow. | No live side effects. |
| `Apply_LiveStateOnlyMutationAppliesDeathTransitionAndLeavesSideEffectsPlanned` | Unit / adapter | `PlayerController.onDie`, `CreatureController.onDie` | Opt-in mode applies only death state transition and reports packets/tasks/callbacks as not executed. | Deterministic C# state mutation plus planner metadata. | No live scheduler, packets, summon, effects, rewards, XP, or quest. |
| `Apply_DuelOpponentEarlyReturnDoesNotApplyStateTransition` | Unit / adapter | `PlayerController.onDie` duel branch | Duel opponent early return does not apply state transition, preserving Java return before `super.onDie`. | Deterministic Java branch assertion. | Does not execute live duel loss or HP/MP restoration. |
| `Apply_InstanceHandlerEarlyReturnStillAppliesStateTransitionBecauseJavaReturnsAfterSuperOnDie` | Unit / adapter | `PlayerController.onDie` instance branch | Instance-handler early return still applies state transition because Java returns after `super.onDie`. | Deterministic Java branch assertion. | Instance callback is not executed; only facts drive plan status. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Full game-server test suite is still known to have two unrelated stable failures in `GameServerConnectionInventoryExpansionUseItemTests`: `ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate` and `HandleUseItemAsync_ExpExtractMergesRestrictedRewardWithCleanupSealFlag`.
- Adapter is opt-in and is not wired into production player damage/death flow.
- Only player state transition is live; cancel-current-skill, rebirth effect scan, duel handling, summon release, movement abort, casting clear, effect removal, observer callbacks, packet fanout, aggro cleanup, scheduler behavior, instance/map callbacks, rewards, XP-loss, and quest callbacks remain unsupported.
- Java threading for `ThreadPoolManager.schedule(..., 500)` and teleport-task suppression is not ported.
- Java packet serialization and recipient ordering for `SM_EMOTION DIE` and `SM_DIE` remain unverified.
- Exact Java state flag composition around `ACTIVE`, `DEAD`, `FLOATING_CORPSE`, ride mode, and fly state still needs runtime comparison.

## Summary Metrics

- Total Java artifacts discovered: 8 grouped artifact rows in this unit
- Total artifacts ported: 1 state-only player death workflow adapter and 1 focused test suite
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 8 grouped rows
- Total blocked artifacts: Java runtime artifact generation, production death wiring, live duel/summon/effect/observer/fanout/scheduler/callback/reward/quest systems, unrelated full-suite cleanup-seal failures
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: add a non-live `SM_DIE` resurrection-options scheduling plan for Java `PlayerController.scheduleShowResurrectionOptions`.
- Include the 500ms delay, dead-state guard, `TaskId.TELEPORT` suppression, and `SM_DIE` send metadata.
- Keep scheduler execution and packet sending disabled until task ownership and connection fanout surfaces are ready.

## Suggested Acceptance Criteria

- Planner records Java's 500ms delay and `SM_DIE` packet intent.
- Planner records no-send when the player is no longer dead at callback time.
- Planner records no-send when a teleport task is present at callback time.
- Tests cover ordinary send, not-dead skip, teleport-task skip, and floating-corpse/dead-state interpretation.
- Re-run new scheduler plan tests, `PlayerDeathWorkflowAdapterServiceTests`, `PlayerDeathWorkflowPlanServiceTests`, and revive restore tests.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Resurrection-options scheduling plan | new service/tests | Low | New non-live metadata files. |
| B | Protection packet fanout bridge analysis | read-only Java/C# broadcast files | Low | Analysis-only can be parallelized. |
| C | Inventory cleanup-seal failure triage | inventory item-use tests/services | Medium | Separate current full-suite blocker. |

## Do Not Parallelize

- Shared `Player` model.
- Shared death workflow/state transition fixtures.
- Shared progress/handoff docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1486] Add player death state adapter`.
- Files changed in UOW-1486:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerDeathWorkflowAdapterService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerDeathWorkflowAdapterServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6ALJ-Completion.md`
