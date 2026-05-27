# Phase 6AKJ Completion - Depleted Kisk Removal Order

Date: 2026-05-27
Unit of Work: UOW-1460
Status: Complete after validation.

## Scope

Harden depleted-kisk revive cleanup ordering against the reviewed Java flow without changing production behavior.

## Completed Work

- Re-read the existing C# kisk revive workflow and kisk removal cleanup path.
- Extended the test-only kisk revive connection registry with an operation-order log for direct sends, visible-player broadcasts, and NPC visibility refreshes.
- Updated `HandleReviveAsync_DepletedKiskRunsRegistryCleanupFanout` to assert the Java-derived ordering around depleted kisk update, delete cleanup, revive emotion, and teleport delete.
- Left production code unchanged because the existing C# path already followed the reviewed order.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~GameServerConnectionKiskReviveWorkflowTests"`.
- Result: passed 9 tests.

## Migration Parity Table - UOW-1460

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.player.PlayerReviveService` | `GameServerConnection.HandleReviveAsync` | Service / Connection Workflow | Partial | Regression Tested | Partial Parity | Depleted kisk revive now has a regression for the reviewed Java sequence around `kisk.resurrectionUsed()`, kisk removal cleanup, revive emotion, and teleport continuation. Player-owned aggro cleanup, soul sickness, flying-before-death restore, and broader teleport lifecycle remain missing or partial. |
| `com.aionemu.gameserver.model.gameobjects.Kisk` | `PlayerKiskRuntimeState` | Runtime State | Partial | Regression Tested | Partial Parity | Test asserts depleted charge flow emits kisk update fanout before delete cleanup. Java known-list membership and `broadcastKiskUpdate` visibility precision remain broader than the C# fixture. |
| `com.aionemu.gameserver.services.KiskService` | `PlayerKiskRemovalRuntimeCleanupService` | Service / Cleanup | Partial | Regression Tested | Partial Parity | Test asserts creator final kisk update occurs before member bind-point reset, death-option refresh, and NPC visibility refresh, matching reviewed `KiskService.removeKisk` order. Offline bind cleanup and exact Java packet bytes remain unverified. |
| `com.aionemu.gameserver.controllers.VisibleObjectController` | `GameServerConnection.RemoveRuntimeKiskAsync` / `PlayerKiskLifetimeService.DespawnExpiredKisk` | Controller / World Removal | Partial | Regression Tested | Partial Parity | Production code unchanged; regression documents that world/registry removal cleanup completes before revive continuation packets. C# still lacks the full Java controller hierarchy. |
| `com.aionemu.gameserver.services.teleport.TeleportService` | `GameServerConnection.TeleportPlayerToKiskPositionAsync` | Service / Teleport Workflow | Partial | Regression Tested | Partial Parity | Test asserts player teleport delete broadcast happens after revive emotion in the depleted-kisk path. Full map-change, protection, instance, and legion-leave side effects remain queued. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleReviveAsync_DepletedKiskRunsRegistryCleanupFanout` | Regression / workflow | `PlayerReviveService.kiskRevive`, `Kisk.resurrectionUsed`, `KiskService.removeKisk`, `TeleportService.teleportTo` | Depleted kisk update broadcast precedes deletion cleanup; creator final update precedes member bind/death refresh; NPC visibility refresh precedes revive emotion; teleport delete follows revive emotion. | Deterministic C# operation-order assertions from reviewed Java ordering. | Does not byte-compare Java packets or cover offline bind cleanup, exact known-list fanout, or full teleport side effects. |
| Existing `GameServerConnectionKiskReviveWorkflowTests` | Existing Regression | Kisk revive workflow | Existing revive restore, target cleanup, movement update, kisk cleanup, and id release tests remained stable. | Focused 9-test suite passed. | Java runtime artifacts unavailable locally. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Operation-order logging is test instrumentation only; it does not prove exact socket flush ordering in a real server under concurrency.
- Exact `SM_KISK_UPDATE`, `SM_BIND_POINT_INFO`, `SM_DIE`, `SM_EMOTION`, and `SM_DELETE` byte payload parity was not checked in this unit.
- C# still lacks Java's full known-list/visibility lifecycle, full controller deletion hierarchy, and complete `TeleportService.teleportTo` side effects.
- Player-owned aggro cleanup remains blocked by the missing C# `PlayerAggroList` equivalent.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 0 production artifacts in this unit; 1 regression assertion slice added
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: Java runtime artifact generation, full known-list fanout comparison, full teleport side effects, player-owned aggro model
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: continue kisk lifecycle with viewer-specific kisk visibility refresh and deletion packet ordering.
- Alternative: start the dedicated player-owned aggro model design UOW needed before revive aggro cleanup can be wired safely.

## Suggested Acceptance Criteria

- If continuing kisk visibility, inspect Java known-list/world removal callers before adding broader socket-order tests.
- Keep shared kisk revive and flight-zone fixtures sequential.
- If designing player aggro, keep it separate from revive live wiring.
- Update the Migration Parity Table for every Java artifact touched.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Viewer-specific kisk visibility/delete order audit | kisk revive or flight-zone tests | Medium | Sequential if touching shared fixtures. |
| B | Player aggro model design | docs/model design, future service skeleton | High | Do not combine with revive live wiring. |
| C | Read-only Java known-list removal audit | Java world/controller/known-list files only | Low | Safe read-only sub-agent candidate if tools are available. |

## Do Not Parallelize

- Shared `GameServerConnection.cs` kisk removal/revive edits.
- Shared kisk workflow or flight-zone fixtures.
- Shared kisk runtime state files.
- Shared progress/handoff docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1460] Harden depleted kisk removal order`.
- Files changed in UOW-1460:
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionKiskReviveWorkflowTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AKJ-Completion.md`
