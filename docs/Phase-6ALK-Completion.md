# Phase 6ALK Completion - Player Death Resurrection Options Planner

Date: 2026-05-27
Unit of Work: UOW-1487
Status: Complete after validation.

## Scope

Add a non-live planner for Java `PlayerController.scheduleShowResurrectionOptions` and `showResurrectionOptions`.

## Completed Work

- Re-audited Java `PlayerController.scheduleShowResurrectionOptions`, `PlayerController.showResurrectionOptions`, `TaskId.TELEPORT`, `CreatureLifeStats.isDead`, and C# `SmDie`.
- Added `PlayerDeathResurrectionOptionsPlanService`.
- Modeled Java's delayed callback as metadata: 500ms delay, dead guard, `TaskId.TELEPORT` suppression, and `SM_DIE` send intent.
- Kept scheduler execution and packet sends disabled.
- Added focused tests for ordinary send, alive skip, teleport-task skip, and floating-corpse plus zero-HP behavior.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --no-restore --filter "FullyQualifiedName~PlayerDeathResurrectionOptionsPlanServiceTests|FullyQualifiedName~PlayerDeathWorkflowAdapterServiceTests|FullyQualifiedName~PlayerDeathWorkflowPlanServiceTests|FullyQualifiedName~PlayerReviveRestoreServiceTests"`.
- Result: passed 18 tests.

## Migration Parity Table - UOW-1487

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.PlayerController` | `Aion.GameServer.Services.PlayerDeathResurrectionOptionsPlanService` | Controller / Scheduler Planner | Partial | Unit Tested | Partial Parity | Models `scheduleShowResurrectionOptions` and `showResurrectionOptions` as non-live metadata: delayed callback, dead guard, teleport-task guard, and `SM_DIE` send intent. Does not execute `ThreadPoolManager.schedule`, inspect a live controller task map, or send packets. |
| `com.aionemu.gameserver.model.TaskId` | `PlayerDeathResurrectionOptionsPlanService` constants | Enum / Task Metadata | Partial | Unit Tested | Partial Parity | Captures `TaskId.TELEPORT` ordinal `1` and name. C# still lacks a general Java `TaskId` enum/task-owner abstraction for this planner. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_DIE` | `Aion.GameServer.Network.Aion.ServerPackets.SmDie` / `PlayerDeathResurrectionOptionsPlanService` | Packet / Planner Metadata | Partial | Unit Tested | Needs Verification | Planner records `SmDie.PacketOpCode` and send intent. Existing C# packet serialization is not byte-compared against Java in this unit. |
| `com.aionemu.gameserver.model.stats.container.CreatureLifeStats` | `Aion.GameServer.Model.GameObjects.PlayerLifeStats` / planner guard | Model / Stats Dependency | Partial | Unit Tested | Needs Verification | Java `CreatureLifeStats.isDead()` is `currentHp == 0`. Planner uses C# `LifeStats.CurrentHp <= 0`; state fallback exists only when life stats are missing and should be treated as conservative metadata. |
| `com.aionemu.gameserver.model.gameobjects.player.Player` | `Aion.GameServer.Model.GameObjects.Player` | Model | Partial | Unit Tested | Needs Verification | Planner reads player object id, creature state, and optional life stats. Floating-corpse with zero HP plans resurrection options; exact Java runtime state/HP composition remains unverified. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `CreatePlan_DeadWithoutTeleportTaskPlansSmDieAfterJavaDelay` | Unit / planner | `PlayerController.scheduleShowResurrectionOptions`, `showResurrectionOptions` | Dead player without teleport task plans `SM_DIE` after 500ms and records `TaskId.TELEPORT` metadata. | Deterministic Java source audit and C# metadata assertion. | No live timer, task map, or packet send. |
| `CreatePlan_AliveAtCallbackSkipsSmDieLikeJavaGuard` | Unit / planner | `PlayerController.scheduleShowResurrectionOptions` | Alive player at callback time skips teleport-task check and `SM_DIE` send. | Deterministic Java branch assertion. | Uses C# life-stat snapshot, not live callback state. |
| `CreatePlan_TeleportTaskAtCallbackSuppressesSmDie` | Unit / planner | `PlayerController.scheduleShowResurrectionOptions` | Dead player with active `TaskId.TELEPORT` suppresses `SM_DIE`. | Deterministic Java branch assertion. | Teleport task presence is caller-supplied metadata. |
| `CreatePlan_FloatingCorpseWithZeroHpStillPlansResurrectionOptions` | Unit / planner | `PlayerController.scheduleShowResurrectionOptions`, `CreatureLifeStats.isDead` | Floating-corpse state with zero HP still counts as dead for resurrection options because Java checks life stats. | Deterministic Java `isDead()` source audit. | Runtime state/HP combination still needs Java comparison. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Full game-server test suite is still known to have two unrelated stable failures in `GameServerConnectionInventoryExpansionUseItemTests`: `ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate` and `HandleUseItemAsync_ExpExtractMergesRestrictedRewardWithCleanupSealFlag`.
- Planner is non-live and does not schedule a timer, inspect a live controller task map, or send `SM_DIE`.
- Teleport-task presence is caller-supplied metadata; Java's controller task ownership and concurrency behavior are not ported here.
- `SM_DIE` byte serialization and connection send behavior were not compared against Java runtime output.
- C# dead-state fallback for missing life stats is conservative metadata; Java source-of-truth dead guard is life-stat based.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 non-live resurrection-options planner and 1 focused test suite
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: Java runtime artifact generation, live scheduler/task-owner behavior, live packet send/fanout, `SM_DIE` byte comparison, unrelated full-suite cleanup-seal failures
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: compose `PlayerDeathResurrectionOptionsPlanService` into `PlayerDeathWorkflowPlanService` / `PlayerDeathWorkflowAdapterService` metadata.
- Ordinary death plans should expose the concrete resurrection-options scheduler plan instead of only a generic `ScheduleShowResurrectionOptions` step.
- Keep this non-live and avoid production death wiring.

## Suggested Acceptance Criteria

- Workflow plan includes a nullable or explicit resurrection-options plan when Java reaches `scheduleShowResurrectionOptions`.
- Duel opponent early-return plans do not include a resurrection-options plan.
- Instance/map early-return plans include the resurrection-options plan because Java schedules before those callbacks return.
- Adapter exposes the composed scheduler plan while still reporting `ScheduledTasks = false` and `SentPackets = false`.
- Re-run workflow planner tests, adapter tests, resurrection-options planner tests, and revive restore tests.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Compose resurrection scheduler into death workflow | death workflow planner/adapter/tests | Medium | Sequential because it touches recently added shared death workflow files. |
| B | Protection packet fanout bridge analysis | read-only Java/C# broadcast files | Low | Analysis-only can be parallelized. |
| C | Inventory cleanup-seal failure triage | inventory item-use tests/services | Medium | Separate current full-suite blocker. |

## Do Not Parallelize

- Shared `Player` model.
- Shared death workflow/state transition/scheduler fixtures.
- Shared progress/handoff docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1487] Add death resurrection options planner`.
- Files changed in UOW-1487:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerDeathResurrectionOptionsPlanService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerDeathResurrectionOptionsPlanServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6ALK-Completion.md`
