# Phase 6AKR Completion - Revive Cleanup Aggro Composition

Date: 2026-05-27
Unit of Work: UOW-1468
Status: Complete after validation.

## Scope

Compose the non-live player aggro clear plan into Java revive cleanup ordering without enabling live combat mutation.

## Completed Work

- Added `PlayerReviveCleanupPlanService`.
- Added `PlayerReviveCleanupPlanStep` and `PlayerReviveCleanupPlan`.
- Added `PlayerReviveCleanupPlanServiceTests`.
- The new planner captures the Java cleanup order inside `PlayerReviveService.revive` as used by kisk revive.
- The test verifies aggro clear is placed after HP/MP/DP/resurrection state restoration and before on-before-spawn.
- Left live `GameServerConnection.HandleReviveAsync` unchanged.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~PlayerReviveCleanupPlanServiceTests"`.
- Result: passed 1 test.

## Migration Parity Table - UOW-1468

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.player.PlayerReviveService` | `PlayerReviveCleanupPlanService` / `PlayerReviveCleanupPlan` | Planner / Service Boundary | Partial | Unit Tested | Partial Parity | Non-live planner captures revive cleanup ordering around target cleanup, resource/resurrection state, aggro clear, on-before-spawn, movement updates, and resurrect emotion. Live revive execution is unchanged. |
| `com.aionemu.gameserver.controllers.attack.AggroList` | `PlayerAggroClearPlan` within `PlayerReviveCleanupPlan` | Planner / Aggro | Partial | Unit Tested | Partial Parity | Composition includes `AggroList.clear` after restore and before spawn, including clear-all metadata. No live aggro mutation exists yet. |
| `com.aionemu.gameserver.controllers.attack.PlayerAggroList` | `PlayerAggroCleanupPlanService` | Planner / Aggro | Partial | Unit Tested | Partial Parity | Reuses UOW-1467 player-owned aggro planner; still non-live and lacks damage/target selection. |
| `com.aionemu.gameserver.controllers.PlayerController` | `PlayerReviveCleanupPlanStep.OnBeforeSpawn` | Controller Boundary | Partial | Unit Tested | Needs Verification | Step ordering records Java `onBeforeSpawn`; C# live revive currently applies state restoration but broader controller spawn lifecycle remains queued. |
| `com.aionemu.gameserver.services.player.PlayerGroupService` / `PlayerAllianceService` | `PlayerReviveCleanupPlanStep.GroupAllianceMovementUpdate` | Team Fanout Boundary | Partial | Unit Tested | Needs Verification | Step ordering records Java movement update placement. Existing workflow tests cover current packet order, but this planner does not execute fanout. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION` | `PlayerReviveCleanupPlanStep.BroadcastResurrectEmotion` | Packet Boundary | Partial | Unit Tested | Needs Verification | Planner records resurrect emotion after movement updates. Packet serialization remains covered elsewhere; Java runtime comparison remains blocked. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `CreateKiskReviveCleanupPlan_ComposesAggroClearInJavaReviveOrder` | Unit / planner | `PlayerReviveService.kiskRevive`, `PlayerReviveService.revive`, `AggroList.clear` | Kisk revive cleanup plan places aggro clear after restore and before on-before-spawn, preserving the overall Java revive cleanup order. | Deterministic step-order assertion plus embedded aggro clear plan assertion. | Non-live planner only; no live combat aggro list or Java runtime comparison. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- C# live revive path does not yet call the non-live cleanup plan.
- C# still lacks a live player-owned aggro list to mutate.
- Broader Java revive side effects remain queued: soul sickness, protection tasks, full teleport despawn/spawn ownership, instance/legion callbacks, and exact socket ordering.
- Kisk bind/member cleanup and aggro cleanup are still separate lanes.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 1 non-live planner service plus DTOs
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: Java runtime artifact generation, live player aggro mutation, live revive adapter, broader revive/teleport side effects
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: decide the next smallest live-safe step: either add a disabled revive cleanup adapter that exposes `PlayerReviveCleanupPlan` from `HandleReviveAsync` tests without mutating combat state, or return to kisk bind/member cleanup review.
- Preferred if continuing aggro: add a test-only observation seam or disabled adapter that confirms the current kisk revive workflow can produce the non-live cleanup plan, while leaving live aggro mutation disabled.

## Suggested Acceptance Criteria

- Do not add live player aggro mutation until a real C# player aggro list exists.
- Keep any adapter disabled or observational by default.
- Preserve current kisk revive packet/order tests.
- Update the Migration Parity Table for every Java artifact touched.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Disabled revive cleanup adapter | revive cleanup service/tests | Medium | Sequential with any revive planner edits. |
| B | Kisk bind/member cleanup review | kisk service tests/docs | Medium | Independent from aggro planner if files stay separate. |
| C | Java revive ordering audit | `PlayerReviveService.java` only | Low | Safe read-only sub-agent candidate. |
| D | Aggro damage/final-damage audit | `AggroInfo`, `DamageList`, reward handlers | Medium | Broader than revive cleanup. |

## Do Not Parallelize

- Shared revive/kisk cleanup planner files.
- Shared active connection fixture changes.
- Shared progress/handoff docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1468] Compose revive aggro cleanup plan`.
- Files changed in UOW-1468:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerReviveCleanupPlanService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerReviveCleanupPlanServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AKR-Completion.md`
