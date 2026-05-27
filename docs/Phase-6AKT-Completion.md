# Phase 6AKT Completion - Kisk Revive Cleanup Observation

Date: 2026-05-27
Unit of Work: UOW-1470
Status: Complete after validation.

## Scope

Add a workflow-level observation test that uses the disabled revive cleanup adapter beside the current kisk revive workflow without changing live behavior.

## Completed Work

- Added `HandleReviveAsync_KiskReviveCanExposeDisabledCleanupPlanWithoutAggroMutation`.
- The test runs the current live kisk revive workflow unchanged.
- The test then uses `PlayerReviveCleanupAdapterService` to expose the non-live cleanup plan for supplied pre-revive aggro entries.
- It verifies the adapter remains disabled, carries the aggro entries into the clear plan, and does not mutate live aggro.
- Left production code unchanged.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~GameServerConnectionKiskReviveWorkflowTests"`.
- Result: passed 10 tests.

## Migration Parity Table - UOW-1470

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_REVIVE` | `GameServerConnection.HandleReviveAsync` | Handler Boundary | Partial | Integration Tested | Partial Parity | Existing live kisk revive workflow is exercised while the disabled cleanup adapter observes the planned aggro cleanup separately. Live handler still does not mutate player aggro. |
| `com.aionemu.gameserver.services.player.PlayerReviveService` | `GameServerConnection.HandleReviveAsync` / `PlayerReviveCleanupAdapterService` | Service Boundary | Partial | Integration Tested | Partial Parity | Test links current live kisk revive behavior to the non-live cleanup plan without changing runtime behavior. Full Java revive side effects remain broader. |
| `com.aionemu.gameserver.controllers.attack.AggroList` | `PlayerAggroClearPlan` | Planner / Aggro | Partial | Integration Tested | Partial Parity | Test verifies pre-revive aggro entries are carried into the observed clear plan. No live clear-all or hate-reduction cancellation is executed. |
| `com.aionemu.gameserver.controllers.attack.PlayerAggroList` | `PlayerReviveCleanupAdapterService` | Adapter / Aggro Boundary | Partial | Integration Tested | Needs Verification | Adapter remains disabled and observational because C# lacks an executable player-owned aggro list. |
| `com.aionemu.gameserver.model.gameobjects.Kisk` | `PlayerKiskRuntimeState` / `GameServerConnection.HandleReviveAsync` | Runtime State / World Object | Partial | Integration Tested | Partial Parity | Workflow continues to consume kisk revive charge, restore, and teleport while exposing the non-live cleanup plan. Bind/member cleanup remains separate. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleReviveAsync_KiskReviveCanExposeDisabledCleanupPlanWithoutAggroMutation` | Integration-style workflow / observational | `CM_REVIVE`, `PlayerReviveService.kiskRevive`, `PlayerReviveService.revive`, `AggroList.clear` | Current live kisk revive succeeds and a disabled cleanup adapter can expose the aggro clear plan with supplied entries, without live aggro mutation. | Deterministic workflow test plus adapter plan assertions. | Does not wire adapter into `HandleReviveAsync` or execute live aggro mutation. |
| Existing `GameServerConnectionKiskReviveWorkflowTests` | Existing Integration-style workflow | Kisk revive/removal/update Java sources | Existing kisk revive restore, no-penalty, target cleanup, movement update, depleted-kisk cleanup, and object-id release tests remained stable. | Focused 10-test suite passed. | Java runtime comparison remains blocked. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Disabled adapter is still not invoked by production `HandleReviveAsync`.
- C# still lacks a live player-owned aggro list to mutate.
- Broader Java revive side effects remain queued: soul sickness, protection tasks, full teleport despawn/spawn ownership, instance/legion callbacks, and exact socket ordering.
- Kisk bind/member cleanup review remains a separate recommended lane.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 0 production artifacts in this unit; 1 workflow observation regression added
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: Java runtime artifact generation, live player aggro mutation, live revive adapter integration, broader revive/teleport side effects
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: return to kisk bind/member cleanup review, or begin a live player-owned aggro list design if choosing to unblock actual aggro mutation next.
- Preferred if staying in the kisk lane: audit Java `KiskService.removeKisk`, `Kisk.removePlayer`, `Kisk.addPlayer`, and current C# registry cleanup to find the next missing bind/member edge case.

## Suggested Acceptance Criteria

- Keep shared revive workflow fixture changes sequential.
- Do not enable live aggro mutation until a C# player-owned aggro list exists.
- If reviewing kisk bind/member cleanup, explicitly compare Java offline-bound-member handling, pending bind request cleanup, member update fanout, and owner registry removal.
- Update the Migration Parity Table for every Java artifact touched.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Java kisk bind/member cleanup audit | `KiskService.java`, `Kisk.java`, related packet sources | Low | Safe read-only sub-agent candidate. |
| B | C# kisk registry cleanup tests | kisk service/runtime cleanup test files | Medium | Sequential if touching shared kisk workflow fixtures. |
| C | Live player aggro list design | future aggro model/service/test files | High | Separate from kisk bind/member cleanup. |
| D | Aggro damage/final-damage audit | `AggroInfo`, `DamageList`, reward handlers | Medium | Broader than revive cleanup. |

## Do Not Parallelize

- Shared revive/kisk cleanup planner files.
- Shared active connection or revive workflow fixture changes.
- Shared progress/handoff docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1470] Observe kisk revive cleanup plan`.
- Files changed in UOW-1470:
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionKiskReviveWorkflowTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AKT-Completion.md`
