# Phase 6AKQ Completion - Player Aggro Cleanup Planner

Date: 2026-05-27
Unit of Work: UOW-1467
Status: Complete after validation.

## Scope

Start the player-owned aggro cleanup blocker with a non-live planner that represents Java player aggro awareness and clear boundaries.

## Completed Work

- Audited Java `AggroList`, `PlayerAggroList`, `PlayerReviveService.revive`, and `PlayerLifeStats.onHpChanged`.
- Added `PlayerAggroCleanupPlanService`.
- Added `PlayerAggroAwarenessPlan`, `PlayerAggroClearPlan`, and supporting DTOs/enums.
- Captured Java `PlayerAggroList.isAware` as a known-list-only awareness rule.
- Captured Java `AggroList.clear()` as clearing all player-owned entries and cancelling the hate-reduction task.
- Added focused unit tests for awareness, revive clear, and full-HP clear planning.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~PlayerAggroCleanupPlanServiceTests"`.
- Result: passed 3 tests.

## Migration Parity Table - UOW-1467

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.attack.PlayerAggroList` | `PlayerAggroCleanupPlanService` / `PlayerAggroAwarenessPlan` | Planner / Aggro | Partial | Unit Tested | Partial Parity | C# planner captures Java's player-specific awareness rule: known-list membership is enough, without ordinary creature enemy/tribe/sanctuary checks. Non-live only. |
| `com.aionemu.gameserver.controllers.attack.AggroList` | `PlayerAggroClearPlan` / `PlayerAggroEntrySnapshot` | Planner / Aggro | Partial | Unit Tested | Partial Parity | C# planner captures clear-all semantics and hate-reduction task cancellation metadata. It does not model hate reduction timing, damage transfer, target selection, or final damage aggregation. |
| `com.aionemu.gameserver.services.player.PlayerReviveService` | `PlayerAggroCleanupPlanService.PlanClear(... Revive)` | Service Boundary | Partial | Unit Tested | Partial Parity | Test anchors revive cleanup to Java `player.getAggroList().clear()`. Not yet wired into live C# revive/kisk execution. |
| `com.aionemu.gameserver.model.stats.container.PlayerLifeStats` | `PlayerAggroCleanupPlanService.PlanClear(... FullHpRestore)` | Stats Boundary | Partial | Unit Tested | Partial Parity | Test anchors full-HP aggro reset source. Live HP/stat mutation adapter remains separate. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `PlanAwareness_UsesPlayerAggroListKnownOnlyRule` | Unit / planner | `PlayerAggroList.isAware` | Known attacker is accepted and unknown attacker is rejected using only known-list awareness metadata. | Deterministic planner status and Java source breadcrumb assertion. | Does not execute live known-list lookup or ordinary creature awareness rules. |
| `PlanClear_ReviveClearsAllPlayerAggroEntries` | Unit / planner | `PlayerReviveService.revive`, `AggroList.clear` | Revive clear plan records all entries, clears all, and cancels hate-reduction task metadata. | Deterministic clear plan and Java source breadcrumb assertion. | Does not mutate a live aggro list or player revive flow. |
| `PlanClear_FullHpRestoreUsesPlayerLifeStatsSource` | Unit / planner | `PlayerLifeStats.onHpChanged`, `AggroList.clear` | Full-HP restore clear plan uses the Java life-stats source and clear metadata. | Deterministic clear plan and Java source breadcrumb assertion. | HP stat execution remains separate. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Player aggro planner is non-live; revive and HP paths do not yet call it.
- C# still lacks a live `PlayerAggroList` equivalent with hate reduction, damage transfer, target selection, or final damage aggregation.
- Interaction with kisk revive teleport/stat packet ordering remains unverified.
- Ordinary creature aggro rules remain separate from the player-owned aggro cleanup blocker.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped artifact rows in this unit
- Total artifacts ported: 1 non-live planner service plus DTOs
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: Java runtime artifact generation, live player aggro mutation, revive integration, full aggro damage/target model
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: add a non-live revive cleanup composition test that includes the new player aggro clear plan alongside the existing kisk revive cleanup descriptors, without enabling live combat mutation.
- Alternative: deepen the aggro model toward `AggroInfo` / `DamageList` only if revive cleanup requires damage accounting, which current Java audit does not show.

## Suggested Acceptance Criteria

- Keep live revive/kisk execution disabled unless an existing service already has a safe non-mutating adapter.
- Assert Java ordering: `PlayerReviveService.revive` clears known player targets, applies HP/MP/DP/soul-sickness logic, resets resurrection skill, clears aggro, calls `onBeforeSpawn`, then group/alliance movement and resurrect emotion fanout.
- Include the aggro clear plan in the composition output with Java source breadcrumbs.
- Update the Migration Parity Table for every Java artifact touched.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Revive cleanup composition test | revive/kisk planner tests or new test file | Medium | Sequential if touching shared revive planner files. |
| B | Java revive ordering audit | `PlayerReviveService.java` only | Low | Safe read-only sub-agent candidate. |
| C | Aggro damage/final-damage audit | `AggroInfo`, `DamageList`, instance reward handlers | Medium | Broader than current revive cleanup. |
| D | Kisk bind/member cleanup review | kisk service tests/docs | Medium | Separate from aggro planner integration. |

## Do Not Parallelize

- Shared revive/kisk cleanup planner files.
- Shared active connection fixture changes.
- Shared progress/handoff docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1467] Add player aggro cleanup planner`.
- Files changed in UOW-1467:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerAggroCleanupPlanService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerAggroCleanupPlanServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AKQ-Completion.md`
