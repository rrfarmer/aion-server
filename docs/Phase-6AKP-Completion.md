# Phase 6AKP Completion - Known Invisible Removal Planner

Date: 2026-05-27
Unit of Work: UOW-1466
Status: Complete after validation.

## Scope

Cover Java `KnownList.del` behavior where a known object can be removed without a `notSee` side effect when the known object was already not visible.

## Completed Work

- Added `Plan_OutOfRangeExistingInvisibleMembershipSkipsNotSeeSideEffects`.
- The test models two known players with visibility already false on both sides.
- It plans an out-of-range refresh and verifies removal steps include owner/candidate membership removal plus `notKnow` descriptors, but no owner/candidate `notSee` descriptors.
- Left production code unchanged.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~PlayerKnownListVisibilityRangePlanServiceTests"`.
- Result: passed 6 tests.

## Migration Parity Table - UOW-1466

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.world.knownlist.KnownList` | `PlayerKnownListVisibilityRangePlanService` / `PlayerKnownListTwoWayOperationPlanService` | Planner / Known List | Partial | Unit Tested | Partial Parity | Regression proves removal of a known but invisible pair schedules membership removal and `notKnow`, but omits `notSee`, matching Java `KnownList.del` only notifying not-see when the stored known object was visible. Planner is non-live. |
| `com.aionemu.gameserver.world.knownlist.KnownObject` | `PlayerKnownListVisibilityRangeObject` / `PlayerKnownListTwoWayOperationState` | DTO / State | Partial | Unit Tested | Needs Verification | C# models visible state with `CanSeeOther`; Java stores this as `KnownObject.visible`. No live Java runtime artifact comparison. |
| `com.aionemu.gameserver.controllers.PlayerController` | `PlayerKnownListTwoWayOperationStepKind.OwnerNotSeesCandidate` / `CandidateNotSeesOwner` | Controller Side-Effect Descriptor | Partial | Unit Tested | Partial Parity | Test asserts no `notSee` descriptor is produced for already invisible known objects. Actual packet sending remains disabled and unverified in this planner path. |
| `com.aionemu.gameserver.utils.PositionUtil` / `KnownList.isInRange` | `PlayerKnownListVisibilityRangePlanService` | Utility / Visibility Predicate | Partial | Unit Tested | Needs Verification | Uses existing strict range planner. Exact Java geometry/region interaction and runtime map-region traversal remain unverified. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Plan_OutOfRangeExistingInvisibleMembershipSkipsNotSeeSideEffects` | Unit / planner | `KnownList.del`, `KnownObject.visible`, `KnownList.forgetObjectsOrUpdateVisibility` | A known but already invisible pair that falls out of range plans removal and `notKnow` steps without `notSee` steps. | Deterministic planner output with explicit step sequence and negative assertion for `OwnerNotSeesCandidate` / `CandidateNotSeesOwner`. | Non-live planner only; no socket packet emission or Java runtime comparison. |
| Existing `PlayerKnownListVisibilityRangePlanServiceTests` | Existing Unit | `KnownList.isInRange`, `PositionUtil.isInRange` | Existing strict range, max visible distance, instance mismatch, visible add, and visible removal tests remained stable. | Focused 6-test suite passed. | Exact map-region traversal and full known-list mutation execution remain broader. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- This is a planner-only regression; it does not execute live C# known-list mutation or controller packet fanout.
- C# socket NPC visibility still uses snapshot/delta object ids and does not model invisible-but-known NPC state.
- Exact Java map-region visibility and lock ordering remain unverified.
- Player-owned aggro cleanup remains blocked by the missing C# `PlayerAggroList` equivalent.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped artifact rows in this unit
- Total artifacts ported: 0 production artifacts in this unit; 1 planner regression added
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: Java runtime artifact generation, live known-list mutation execution, socket NPC invisible-known state, player-owned aggro model
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: switch to the dedicated player-owned aggro model design UOW to unblock revive aggro cleanup, or continue kisk lifecycle with bind/member cleanup review if staying in the kisk lane.
- Preferred next aggro slice: inspect Java `AggroList` / `PlayerAggroList` cleanup during kisk revive and design the smallest C# non-live model or planner that can represent owner/team member aggro removal without wiring live combat.

## Suggested Acceptance Criteria

- Identify the exact Java aggro artifacts and call chain used by kisk revive cleanup.
- Add a non-live C# model/planner or a design document only if production surfaces are still missing.
- Include Java source breadcrumbs on new code or docs.
- Update the Migration Parity Table for every Java artifact touched.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Java aggro cleanup audit | Java aggro/kisk/revive files only | Low | Safe read-only sub-agent candidate. |
| B | Non-live player aggro model design | future docs/model/service/test files | Medium-High | Keep separate from socket visibility fixtures. |
| C | Kisk bind/member cleanup review | kisk service tests/docs | Medium | Separate from aggro design. |
| D | Active socket visibility fixture hardening | `GameClientSocketServerNpcVisibilityTests.cs` | Medium | Avoid concurrent edits with aggro work. |

## Do Not Parallelize

- Shared active connection fixture changes.
- Shared kisk revive/runtime cleanup fixtures.
- Shared progress/handoff docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1466] Cover known invisible removal planning`.
- Files changed in UOW-1466:
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerKnownListVisibilityRangePlanServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AKP-Completion.md`
