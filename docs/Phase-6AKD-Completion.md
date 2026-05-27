# Phase 6AKD Completion - Kisk Revive Teleport Fanout

Date: 2026-05-27
Unit of Work: UOW-1454
Status: Complete after validation.

## Scope

Add a focused kisk revive workflow regression for registry-backed revive emotion and teleport delete fanout source metadata. This unit is test/documentation-only.

## Completed Work

- Added `HandleReviveAsync_KiskReviveBroadcastsTeleportDeleteFromPreRevivePosition`, covering the Java `PlayerReviveService.revive` then `TeleportService.teleportTo(kisk.getPosition())` ordering surface.
- Extended the local kisk revive test registry to record visible-player broadcast source metadata.
- Asserted revive emotion fanout is broadcast from the pre-teleport position with source included.
- Asserted teleport delete fanout is broadcast from the same pre-teleport position with source excluded.
- Asserted direct self packets continue with channel/spawn/info/stats/motion after the registry fanout path.
- No production code changed in this unit.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~GameServerConnectionKiskReviveWorkflowTests"`.
- Result: passed 7 tests.

## Migration Parity Table - UOW-1454

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.player.PlayerReviveService` | `GameServerConnection.HandleReviveAsync` / `BroadcastEmotionAsync` | Service / Connection Workflow | Partial | Regression Tested | Partial Parity | Registry-backed kisk revive now asserts emotion fanout comes from the pre-teleport position with source included. Exact Java known-list recipients remain proxied by the C# registry test double. |
| `com.aionemu.gameserver.services.player.PlayerReviveService.kiskRevive` | `GameServerConnection.HandleReviveAsync` / `TeleportPlayerToKiskPositionAsync` | Service / Kisk Revive | Partial | Regression Tested | Partial Parity | Test asserts direct self packets continue after registry fanout without the no-registry inline emotion packet. Prison/event-mode and skill-id message branches remain unimplemented. |
| `com.aionemu.gameserver.services.teleport.TeleportService` | `SendKiskReviveTeleportPacketsAsync` / `SmDelete` fanout | Service / Teleport Fanout | Partial | Regression Tested | Partial Parity | Test asserts teleport delete fanout uses the player's pre-revive position and excludes the source player before channel/spawn direct packets. Full Java despawn/spawn lifecycle remains broader. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_DELETE` | `SmDelete` | Packet | Partial | Regression Tested | Needs Verification | Test observes packet type and broadcast source metadata, not full Java bytes. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleReviveAsync_KiskReviveBroadcastsTeleportDeleteFromPreRevivePosition` | Regression / connection workflow | `PlayerReviveService.revive`, `PlayerReviveService.kiskRevive`, `TeleportService.teleportTo` | Revive emotion broadcasts and teleport delete use pre-teleport position; delete excludes source; direct packets continue with channel/spawn/info/stats/motion. | Deterministic C# assertions derived from reviewed Java ordering. | Registry fixture does not prove exact Java visible-recipient known-list behavior or packet bytes. |
| Existing `GameServerConnectionKiskReviveWorkflowTests` | Existing Regression | Same Java artifacts plus `Kisk.resurrectionUsed` and `KiskService.removeKisk` | Kisk revive charge, no-resurrect penalty, target cleanup, depleted cleanup, and object-id release remained stable. | 7-test focused suite passed. | Broader revive side effects remain partial. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- C# visible fanout still uses a registry abstraction/test double rather than Java region known-list internals.
- Group/alliance movement updates, aggro cleanup, prison/event-mode branches, skill-id resurrection message, and full `TeleportService.teleportTo` lifecycle remain incomplete.
- Full packet bytes remain unverified.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped artifact rows in this unit
- Total artifacts ported: 0 production artifacts changed in this unit
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: Java runtime artifact generation, Java known-list exactness, broader revive side effects, Java packet byte comparisons
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: continue kisk/revive only if a small test can cover an existing surface.
- Candidate: add a read-only/design UOW for alliance revive movement update wiring, since group revive metadata exists but connection-level kisk revive still does not emit group/alliance movement updates.

## Suggested Acceptance Criteria

- Verify existing group/alliance planner coverage before editing.
- If no narrow production-safe hook exists, document the gap instead of adding broad connection plumbing.
- Keep Java source breadcrumbs tied to `PlayerReviveService.revive`, `PlayerGroupService.updateGroup`, and `PlayerAllianceService.updateAlliance`.
- Update the Migration Parity Table for every Java artifact touched.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Alliance revive movement update design | group/alliance planner tests or docs | Low-Medium | Read existing planner coverage first. |
| B | Toy-pet lifetime schedule observability design | `ToyPetSpawnAction`, `Kisk`, connection fixture | Medium | Current fixture lacks schedule metadata; read-only first. |
| C | Kisk removal cleanup socket-order audit | kisk workflow tests / Java `KiskService.removeKisk` | Medium | Avoid overlapping shared kisk revive fixture edits. |

## Do Not Parallelize

- Shared `GameServerConnection.cs` revive changes.
- Shared kisk revive workflow fixture edits.
- Shared progress/handoff docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1454] Cover kisk revive teleport fanout`.
- C# files changed in UOW-1454:
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionKiskReviveWorkflowTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AKD-Completion.md`
