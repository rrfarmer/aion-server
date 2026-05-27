# Phase 6AKS Completion - Disabled Revive Cleanup Adapter

Date: 2026-05-27
Unit of Work: UOW-1469
Status: Complete after validation.

## Scope

Add a disabled/observational adapter that exposes the non-live kisk revive cleanup plan without mutating live aggro state.

## Completed Work

- Added `PlayerReviveCleanupAdapterService`.
- Added `PlayerReviveCleanupAdapterRequest`, `PlayerReviveCleanupAdapterResult`, and `PlayerReviveCleanupAdapterStatus`.
- Added `PlayerReviveCleanupAdapterServiceTests`.
- Disabled mode returns the existing `PlayerReviveCleanupPlan` and reports no live aggro mutation.
- Explicit live mutation requests return `BlockedMissingLiveAggroList`, documenting the current blocker instead of silently pretending to clear player aggro.
- Left live `GameServerConnection.HandleReviveAsync` unchanged.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~PlayerReviveCleanupAdapterServiceTests"`.
- Result: passed 2 tests.

## Migration Parity Table - UOW-1469

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.player.PlayerReviveService` | `PlayerReviveCleanupAdapterService` | Adapter / Service Boundary | Partial | Unit Tested | Partial Parity | Adapter exposes the non-live kisk revive cleanup plan in disabled mode. It is not wired into live `HandleReviveAsync`; this is intentional until live aggro state exists. |
| `com.aionemu.gameserver.controllers.attack.PlayerAggroList` | `PlayerReviveCleanupAdapterResult` / `PlayerReviveCleanupAdapterStatus.BlockedMissingLiveAggroList` | Adapter / Aggro Boundary | Partial | Unit Tested | Needs Verification | Adapter explicitly blocks live aggro mutation because there is no executable C# player-owned aggro list. Java player aggro awareness and clear planning are represented by prior non-live planners only. |
| `com.aionemu.gameserver.controllers.attack.AggroList` | `PlayerReviveCleanupPlan.AggroClearPlan` | Planner / Aggro | Partial | Unit Tested | Partial Parity | Adapter surfaces the `AggroList.clear` plan but does not execute clear-all or hate-reduction cancellation against live state. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_REVIVE` | `GameServerConnection.HandleReviveAsync` / `PlayerReviveCleanupAdapterService` | Handler Boundary | Partial | Unit Tested | Needs Verification | Live handler remains unchanged. Adapter is observational and not yet connected to packet handling to avoid unsupported live mutation. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Apply_DisabledExposesKiskReviveCleanupPlanWithoutLiveAggroMutation` | Unit / adapter | `PlayerReviveService.kiskRevive`, `PlayerReviveService.revive` | Disabled adapter returns `DisabledPlanned`, exposes the kisk revive cleanup plan, includes `ClearPlayerAggro`, and does not mutate live aggro. | Deterministic adapter status and plan assertions with Java source breadcrumb. | Does not execute live `HandleReviveAsync` or mutate aggro. |
| `Apply_LiveAggroMutationRequestReportsMissingLivePlayerAggroList` | Unit / adapter | `PlayerAggroList`, `AggroList.clear` | Explicit live mutation request returns `BlockedMissingLiveAggroList` while still exposing the plan for observation. | Deterministic blocker status and Java source assertion. | Live player aggro list implementation remains missing. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Adapter is not wired into live kisk revive handling.
- C# still lacks a live player-owned aggro list to mutate.
- Broader Java revive side effects remain queued: soul sickness, protection tasks, full teleport despawn/spawn ownership, instance/legion callbacks, and exact socket ordering.
- Kisk bind/member cleanup and aggro cleanup are still separate lanes.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped artifact rows in this unit
- Total artifacts ported: 1 disabled adapter service plus DTOs
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: Java runtime artifact generation, live player aggro mutation, live revive adapter integration, broader revive/teleport side effects
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: either add a test-only observation seam in the kisk revive workflow tests that uses the disabled adapter without changing live behavior, or return to kisk bind/member cleanup review.
- Preferred if continuing aggro: extend revive workflow coverage to assert the disabled adapter can observe the current kisk revive cleanup plan beside the live workflow, while leaving `GameServerConnection.HandleReviveAsync` unchanged.

## Suggested Acceptance Criteria

- Preserve current kisk revive packet/order tests.
- Keep live player aggro mutation disabled.
- If a workflow observation test is added, make it explicit that it observes a non-live plan rather than proving live Java parity.
- Update the Migration Parity Table for every Java artifact touched.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Kisk revive workflow observation test | `GameServerConnectionKiskReviveWorkflowTests.cs` | Medium | Sequential because it touches shared revive fixture/tests. |
| B | Kisk bind/member cleanup review | kisk service tests/docs | Medium | Independent if kept out of revive workflow fixture. |
| C | Java revive ordering audit | `PlayerReviveService.java` only | Low | Safe read-only sub-agent candidate. |
| D | Aggro damage/final-damage audit | `AggroInfo`, `DamageList`, reward handlers | Medium | Broader than revive cleanup. |

## Do Not Parallelize

- Shared revive/kisk cleanup planner files.
- Shared active connection or revive workflow fixture changes.
- Shared progress/handoff docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1469] Add disabled revive cleanup adapter`.
- Files changed in UOW-1469:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerReviveCleanupAdapterService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerReviveCleanupAdapterServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AKS-Completion.md`
