# Phase 6AKC Completion - Kisk Revive Target Cleanup

Date: 2026-05-27
Unit of Work: UOW-1453
Status: Complete after validation.

## Scope

Add a focused kisk revive workflow regression for Java `PlayerReviveService.revive` target cleanup. This unit is test/documentation-only.

## Completed Work

- Added `HandleReviveAsync_KiskReviveClearsVisibleTargetsBeforeTeleport`, covering Java `PlayerReviveService.revive` target cleanup before `PlayerReviveService.kiskRevive` teleports to the kisk.
- The new test registers visible, distant, and unrelated online players, then asserts only the visible player who targeted the revived player is cleared.
- The test also asserts the revived player lands at the kisk position after target cleanup.
- No production code changed in this unit.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~GameServerConnectionKiskReviveWorkflowTests"`.
- Result: passed 6 tests.

## Migration Parity Table - UOW-1453

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.player.PlayerReviveService` | `GameServerConnection.HandleReviveAsync` / `PlayerReviveTargetCleanupService.ClearKnownPlayerTargets` | Service / Connection Workflow | Partial | Regression Tested | Partial Parity | Kisk revive now has connection-level coverage that visible players targeting the revived player are cleared before teleport. Group/alliance movement updates, aggro cleanup, soul-sickness branches, and exact Java known-list membership remain partial. |
| `com.aionemu.gameserver.services.player.PlayerReviveService.kiskRevive` | `PlayerKiskReviveService.TryUseKiskRevive` / `TeleportPlayerToKiskPositionAsync` | Service / Kisk Revive | Partial | Regression Tested | Partial Parity | Test confirms cleanup happens before the player ends at the kisk position in the same workflow. Prison/event-mode pre-teleport branches and skill-id resurrection message remain unimplemented. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_REVIVE` | `CmRevive` / `GameServerConnection.HandleReviveAsync` | Packet / Handler | Partial | Regression Tested | Partial Parity | Existing packet parser reads the revive id byte like Java. This unit exercises KISK_REVIVE id routing only; other revive types remain outside this workflow. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleReviveAsync_KiskReviveClearsVisibleTargetsBeforeTeleport` | Regression / connection workflow | `PlayerReviveService.revive`, `PlayerReviveService.kiskRevive` | Visible online players targeting the revived player are cleared; distant and unrelated targeters are not; revive still teleports to the kisk. | Deterministic C# assertion from reviewed Java target-cleanup order before teleport. | Uses C# visibility distance proxy, not Java known-list internals; no runtime Java artifact. |
| Existing `GameServerConnectionKiskReviveWorkflowTests` | Existing Regression | Same Java artifacts plus `Kisk.resurrectionUsed` and `KiskService.removeKisk` | Kisk revive charge, no-resurrect penalty, depleted cleanup, object-id release remained stable. | 6-test focused suite passed. | Full socket order and broader revive side effects remain partial. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- C# uses a distance-based visibility proxy for revive target cleanup; Java uses the revived player's known list.
- Group/alliance movement updates, aggro cleanup, prison/event-mode branches, skill-id resurrection message, and exact `TeleportService.teleportTo` side effects remain incomplete.
- Full packet bytes remain unverified.

## Summary Metrics

- Total Java artifacts discovered: 3 grouped artifact rows in this unit
- Total artifacts ported: 0 production artifacts changed in this unit
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 3 grouped rows
- Total blocked artifacts: Java runtime artifact generation, Java known-list exactness, broader revive side effects, Java packet byte comparisons
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: continue kisk/revive with another narrow existing surface.
- Add service-level group/alliance movement update metadata coverage for kisk revive if not already complete.
- If that surface is already complete, add a read-only design note for toy-pet lifetime schedule observability before introducing any production hook.

## Suggested Acceptance Criteria

- Keep Java source breadcrumbs tied to `PlayerReviveService.revive`, `PlayerReviveService.kiskRevive`, `ToyPetSpawnAction`, or `Kisk` depending on chosen slice.
- Prefer tests or documentation that clarify existing behavior before production changes.
- If touching toy-pet/kisk lifetime scheduling, observe scheduled delay/order without relying on long real-time waits.
- Update the Migration Parity Table for every Java artifact touched.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Kisk revive group/alliance movement metadata | group/alliance planner service tests | Low | Existing planner may already cover revive trigger; verify before editing. |
| B | Toy-pet lifetime schedule observability design | `ToyPetSpawnAction`, `Kisk`, connection fixture | Medium | Current fixture lacks schedule metadata; read-only first. |
| C | Kisk removal cleanup socket-order audit | kisk workflow tests / Java `KiskService.removeKisk` | Medium | Ensure no overlap with connection workflow fixture edits. |

## Do Not Parallelize

- Shared `GameServerConnection.cs` revive changes.
- Shared kisk revive workflow fixture edits.
- Shared progress/handoff docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1453] Cover kisk revive target cleanup`.
- C# files changed in UOW-1453:
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionKiskReviveWorkflowTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AKC-Completion.md`
