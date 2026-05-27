# Phase 6AMF Completion - Protection AttackUtil Recipient Planner

Date: 2026-05-27
Unit of Work: UOW-1508
Status: Complete after validation.

## Scope

Add a non-live `AttackUtil` protection recipient planner for `cancelCastOn(getOwner())` and `removeTargetFrom(getOwner())` using explicit known-object facts instead of live known-list traversal.

## Completed Work

- Added `PlayerProtectionAttackUtilRecipientPlannerService`.
- Planner projects `AttackUtil.cancelCastOn(target)` candidates from supplied facts:
  - known object is a creature;
  - creature target is the protected player;
  - creature has a casting skill;
  - casting skill first target is the protected player.
- Planner projects `AttackUtil.removeTargetFrom(target, validateSee)` candidates from supplied facts:
  - known object is a player;
  - player target is the protected player;
  - protection start uses `validateSee=false`, so `canSee` does not filter target clears.
- Duplicate known-object ids collapse to the last supplied fact, matching the existing non-live known-list snapshot convention.
- All cast cancellation and target clearing remain disabled.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --no-restore --filter "FullyQualifiedName~PlayerProtectionAttackUtilRecipientPlannerServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSideEffectOperationPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskExecutionBridgeServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSightedRecipientSocketExecutorServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSightedRecipientTraceServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskReportServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskFanoutPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskAdapterServiceTests|FullyQualifiedName~PlayerStateTests"`.
- Result: passed 64 tests.

## Migration Parity Table - UOW-1508

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.attack.AttackUtil` | `Aion.GameServer.Services.PlayerProtectionAttackUtilRecipientPlannerService` | Utility / Combat Side-Effect Planner | Partial | Unit Tested | Partial Parity | Non-live planner projects recipients for `cancelCastOn` and `removeTargetFrom`. It does not traverse live known lists, cancel skills, clear targets, or compare against Java runtime output. |
| `com.aionemu.gameserver.world.knownlist.KnownList` | `PlayerProtectionAttackUtilKnownObjectFact` / planner projections | Visibility / Known-Object Dependency | Partial | Unit Tested Plan Only | Needs Verification | Planner uses supplied known-object facts in place of Java `target.getKnownList().forEachObject/forEachPlayer`. Production known-list object fact generation remains unproven. |
| `com.aionemu.gameserver.model.gameobjects.Creature` | `PlayerProtectionAttackUtilKnownObjectFact` | Model Dependency / Casting Target Fact | Not Started | Unit Tested Plan Only | Needs Verification | Planner models creature target, casting state, and first-target predicates as DTO facts. No live `Creature`, casting skill, or controller cancellation integration. |
| `com.aionemu.gameserver.model.gameobjects.player.Player` | `PlayerProtectionAttackUtilKnownObjectFact` | Model Dependency / Player Target Fact | Partial | Unit Tested Plan Only | Needs Verification | Planner treats players as creatures for cast-cancel eligibility and as players for target-clear eligibility. No live `Player.setTarget(null)` mutation. |
| `com.aionemu.gameserver.skillengine.model.Skill` | `PlayerProtectionAttackUtilKnownObjectFact.CastingSkillFirstTargetObjectId` | Skill / Casting Dependency | Not Started | Unit Tested Plan Only | Needs Verification | Planner models `getCastingSkill().getFirstTarget().equals(target)` through object-id facts. No live skill object comparison or null behavior comparison beyond projected facts. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `CreatePlan_ProjectsEligibleCastCancellationCreatures` | Unit / planner | `AttackUtil.cancelCastOn` | Creature/player known objects targeting the protected player and casting at that player are projected for cast cancellation. | Deterministic Java predicate assertion. | No live cancellation or Java runtime comparison. |
| `CreatePlan_SkipsIneligibleCastCancellationCandidates` | Unit / planner | `AttackUtil.cancelCastOn` | Non-creatures, target mismatch, not-casting, and first-target mismatch are skipped. | Deterministic Java branch assertion. | No live known-list traversal. |
| `CreatePlan_ProjectsTargetClearPlayersWithoutValidateSeeFilter` | Unit / planner | `AttackUtil.removeTargetFrom(target)` | Protection start default `validateSee=false` clears players targeting the protected player regardless of `canSee`. | Deterministic Java overload assertion. | No live target mutation. |
| `CreatePlan_AppliesValidateSeeOnlyWhenRequested` | Unit / planner | `AttackUtil.removeTargetFrom(target, validateSee)` | Optional `validateSee=true` skips players that can still see the target and keeps players that cannot. | Deterministic Java predicate assertion for the shared utility overload. | Protection start uses false; true branch is future-proofed only. |
| `CreatePlan_CollapsesDuplicateKnownObjectIdsUsingLastFact` | Unit / planner | Java known-object id map behavior, modeled snapshot convention | Duplicate known-object ids collapse to the last supplied fact. | Deterministic C# snapshot convention assertion. | Java `ConcurrentHashMap` traversal order remains not runtime-compared. |
| `CreatePlan_EmptyKnownListInputProducesNoRecipients` | Unit / planner | `KnownList.forEachObject/forEachPlayer` empty traversal | Null/empty facts produce no recipients and no live mutations. | Deterministic empty traversal assertion. | No live known-list comparison. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Full game-server test suite is still known to have two unrelated stable failures in `GameServerConnectionInventoryExpansionUseItemTests`: `ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate` and `HandleUseItemAsync_ExpExtractMergesRestrictedRewardWithCleanupSealFlag`.
- Planner is not yet composed into side-effect or execution bridge results.
- Production known-object/casting/target facts are not generated.
- No live cast cancellation, target clearing, known-list traversal, skill first-target comparison, or target mutation exists.
- Duplicate handling follows the existing C# non-live snapshot convention; Java runtime ordering is not verified.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 non-live `AttackUtil` protection recipient planner and 1 focused test suite
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: Java runtime artifact generation, live known-list object facts, live cast cancellation, live target mutation, production planner composition, unrelated full-suite cleanup-seal failures
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: compose `PlayerProtectionAttackUtilRecipientPlan` into the protection side-effect operation plan or execution bridge result.
- Accept optional known-object facts in the chosen request shape.
- Existing callers without facts must remain deterministic and no-send/no-mutation.
- Keep cast cancellation and target clearing disabled.

## Suggested Acceptance Criteria

- Side-effect or bridge result exposes the `AttackUtil` recipient plan.
- Existing bridge tests still pass without supplying facts.
- New test supplies known-object facts and verifies composed cast-cancel and target-clear projections are observable through the selected result.
- No production caller performs live cast cancellation or target clearing.
- Re-run the protection planner/side-effect/bridge/adapter/fanout/trace/executor/report/plan/player-state tests.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Compose `AttackUtil` planner into side-effect or bridge result | selected shared service/tests | Medium | Sequential because it changes result shape. |
| B | Scheduler/task-map readiness audit | read-only Java/C# scheduler/task files | Low | Can run in parallel if read-only. |
| C | Inventory cleanup-seal failure triage | inventory item-use tests/services | Medium | Separate current full-suite blocker. |

## Do Not Parallelize

- Shared protection bridge/side-effect files if changing result shapes.
- Shared progress/handoff docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1508] Add protection AttackUtil planner`.
- Files changed in UOW-1508:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerProtectionAttackUtilRecipientPlannerService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerProtectionAttackUtilRecipientPlannerServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AMF-Completion.md`
