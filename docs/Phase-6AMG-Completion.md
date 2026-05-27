# Phase 6AMG Completion - Protection AttackUtil Bridge Composition

Date: 2026-05-27
Unit of Work: UOW-1509
Status: Complete after validation.

## Scope

Compose `PlayerProtectionAttackUtilRecipientPlan` into `PlayerProtectionActiveTaskExecutionBridgeResult` so the protection bridge exposes cast-cancel and target-clear projection metadata without enabling mutations.

## Completed Work

- Added `PlayerProtectionActiveTaskExecutionBridgeRequest`.
- Bridge request wraps the existing adapter request and optional `PlayerProtectionAttackUtilKnownObjectFact` values.
- Existing callers can still pass `PlayerProtectionActiveTaskAdapterRequest`; that overload supplies no known-object facts and remains deterministic.
- Extended `PlayerProtectionActiveTaskExecutionBridgeResult` with `AttackUtilRecipientPlan`.
- Bridge result now exposes:
  - adapter result;
  - side-effect operation plan;
  - `AttackUtil` recipient plan;
  - concrete `SmPlayerState`;
  - sighted-recipient socket executor result;
  - disabled no-send metadata.
- Cast cancellation and target clearing remain disabled.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --no-restore --filter "FullyQualifiedName~PlayerProtectionAttackUtilRecipientPlannerServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSideEffectOperationPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskExecutionBridgeServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSightedRecipientSocketExecutorServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSightedRecipientTraceServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskReportServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskFanoutPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskAdapterServiceTests|FullyQualifiedName~PlayerStateTests"`.
- Result: passed 65 tests.

## Migration Parity Table - UOW-1509

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.PlayerController` | `Aion.GameServer.Services.PlayerProtectionActiveTaskExecutionBridgeService` | Controller / Execution Bridge | Partial | Unit Tested | Partial Parity | Bridge now exposes the full staged protection workflow including `AttackUtil` recipient metadata. Production callers still do not enable packet sends, cast cancellation, target clearing, scheduler, or AI movement notification. |
| `com.aionemu.gameserver.controllers.attack.AttackUtil` | `PlayerProtectionActiveTaskExecutionBridgeService` / `PlayerProtectionAttackUtilRecipientPlannerService` | Utility / Combat Side-Effect Planner | Partial | Unit Tested | Partial Parity | Bridge composes non-live `cancelCastOn` and `removeTargetFrom` recipient projections from optional supplied facts. No live mutation or runtime comparison. |
| `com.aionemu.gameserver.world.knownlist.KnownList` | `PlayerProtectionActiveTaskExecutionBridgeRequest.KnownObjectFacts` / planner metadata | Visibility / Known-Object Dependency | Partial | Unit Tested Plan Only | Needs Verification | Bridge accepts supplied known-object facts. Production known-list object/casting/target fact generation remains unproven. |
| `com.aionemu.gameserver.model.gameobjects.Creature` | `PlayerProtectionAttackUtilKnownObjectFact` composed by bridge | Model Dependency / Casting Target Fact | Not Started | Unit Tested Plan Only | Needs Verification | Bridge exposes creature cast-cancel projections through DTO facts only. No live creature controller cancellation. |
| `com.aionemu.gameserver.model.gameobjects.player.Player` | `PlayerProtectionAttackUtilKnownObjectFact` composed by bridge | Model Dependency / Player Target Fact | Partial | Unit Tested Plan Only | Needs Verification | Bridge exposes player target-clear projections through DTO facts only. No live `Player.setTarget(null)` mutation. |
| `com.aionemu.gameserver.skillengine.model.Skill` | `PlayerProtectionAttackUtilKnownObjectFact.CastingSkillFirstTargetObjectId` composed by bridge | Skill / Casting Dependency | Not Started | Unit Tested Plan Only | Needs Verification | Bridge exposes first-target match metadata. No live skill object comparison. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ExecuteAsync_LiveStartBuildsPlayerStatePacketAndDisabledExecutorDoesNotSend` | Unit / execution bridge | `PlayerController.startProtectionActiveTask`, `AttackUtil` | Existing adapter-request bridge calls expose an empty `AttackUtil` recipient plan without supplied facts. | Deterministic default-caller assertion. | No live known-object facts. |
| `ExecuteAsync_LiveSpawnedStopBuildsPlayerStatePacketAfterClearingBlinking` | Unit / execution bridge | `PlayerController.stopProtectionActiveTask`, `AttackUtil` | Stop bridge calls expose an empty `AttackUtil` plan without supplied facts. | Deterministic default-caller assertion. | Stop-side Java does not invoke `AttackUtil`; no live mutation. |
| `ExecuteAsync_SkippedBranchesDoNotConstructPacketOrSend` | Unit / execution bridge | skipped start/stop branches | Skipped branches expose an empty `AttackUtil` plan and no packet/no-send result. | Deterministic skipped-branch assertion. | No runtime comparison. |
| `ExecuteAsync_ComposesAttackUtilRecipientPlanFromKnownObjectFacts` | Unit / execution bridge | `AttackUtil.cancelCastOn`, `AttackUtil.removeTargetFrom` | Supplied known-object facts are visible through the bridge as cast-cancel and target-clear projections while registry remains untouched. | Deterministic Java predicate assertion through bridge composition. | No live cast cancellation or target clearing. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Full game-server test suite is still known to have two unrelated stable failures in `GameServerConnectionInventoryExpansionUseItemTests`: `ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate` and `HandleUseItemAsync_ExpExtractMergesRestrictedRewardWithCleanupSealFlag`.
- Bridge result shape changed again; no production caller currently consumes it.
- Production known-object/casting/target facts are not generated.
- No live known-list traversal, cast cancellation, target clearing, scheduler/task-map integration, packet fanout, or movement notification exists for this protection workflow.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: bridge composition of the non-live `AttackUtil` recipient planner plus focused bridge test updates
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: Java runtime artifact generation, production known-object facts, live cast cancellation, live target mutation, live scheduler/task-map integration, production packet fanout, unrelated full-suite cleanup-seal failures
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: add a scheduler/task-map readiness audit or non-live task-operation bridge for protection active tasks.
- Inspect Java task-owner semantics around:
  - `addTask(TaskId.PROTECTION_ACTIVE, scheduledFuture)`;
  - replacement/cancel behavior if an existing task is present;
  - `cancelTask(TaskId.PROTECTION_ACTIVE)` when no task exists;
  - delayed `ThreadPoolManager.schedule(this::stopProtectionActiveTask, 60000)`.
- Keep live scheduling disabled until task-owner integration is safe.

## Suggested Acceptance Criteria

- Java task replacement/cancel semantics are explicitly documented.
- C# plan or audit records delayed stop scheduling and cancel behavior without executing tasks.
- Tests cover normal start scheduling metadata, already-protected no-op, stop with task fact, and stop without task fact.
- Handoff documents whether a live scheduler bridge is safe or what dependency remains missing.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Protection task-operation bridge/audit | new service/tests or docs | Medium | Prefer new files if implementation is chosen. |
| B | Scheduler/task-map Java analysis | read-only Java/C# scheduler/task files | Low | Can be a subagent if read-only. |
| C | Inventory cleanup-seal failure triage | inventory item-use tests/services | Medium | Separate current full-suite blocker. |

## Do Not Parallelize

- Shared protection bridge/side-effect files if changing result shapes.
- Shared progress/handoff docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1509] Compose protection AttackUtil plan`.
- Files changed in UOW-1509:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerProtectionActiveTaskExecutionBridgeService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerProtectionActiveTaskExecutionBridgeServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AMG-Completion.md`
