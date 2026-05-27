# Phase 6AKN Completion - Kisk Visibility Delete Skip

Date: 2026-05-27
Unit of Work: UOW-1464
Status: Complete after validation.

## Scope

Cover the socket-server no-send branch for kisk/NPC visibility deletion when a registered connection no longer has an active player.

## Completed Work

- Re-read Java `PlayerController.notSee`, which skips deletion packets when the owner is not spawned.
- Re-read Java `KnownList.del`, which only calls `notSee` when a known object was visible.
- Added `RefreshNpcVisibilityAsync_SkipsDeleteWhenRegisteredConnectionHasNoActivePlayer`.
- The test primes a registered active connection with a known kisk/NPC, clears the active player, refreshes with an empty NPC snapshot, and verifies no `SmDelete` is emitted.
- Left production code unchanged.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~GameClientSocketServerNpcVisibilityTests"`.
- Result: passed 7 tests.

## Migration Parity Table - UOW-1464

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.PlayerController` | `GameClientSocketServer.RefreshNpcVisibilityAsync` | Controller / Packet Fanout | Partial | Integration Tested | Partial Parity | Test covers the C# no-active-player guard as an approximation of Java `PlayerController.notSee` skipping deletion packets when the owner is not spawned. C# does not yet model a dedicated player spawned flag in this socket refresh path. |
| `com.aionemu.gameserver.world.knownlist.KnownList` | `NpcVisibilityService` | Service / Known List | Partial | Integration Tested | Partial Parity | Known-list state can be primed while active and later not flushed to packets when the registered connection has no active player. Java `KnownList.del` still clears visibility state and calls `notKnow`; C# no-active-player skip does not exercise peer known-list cleanup callbacks. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_DELETE` | `SmDelete` | Packet | Partial | Integration Tested | Partial Parity | Test asserts no delete packet is emitted in the no-active-player branch. It does not compare Java runtime output or cover all object delete packet variants. |
| `com.aionemu.gameserver.model.gameobjects.Kisk` | `WorldNpc` / `PlayerKiskRuntimeState` | Runtime State / World Object | Partial | Integration Tested | Partial Parity | Test uses a kisk NPC id as the known object. Kisk-specific runtime registry cleanup is not part of this skip-branch unit. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `RefreshNpcVisibilityAsync_SkipsDeleteWhenRegisteredConnectionHasNoActivePlayer` | Integration / socket-server | `PlayerController.notSee`, `KnownList.del` | A registered connection that had known the kisk/NPC but no longer has an active player receives no `SmDelete` when the visibility snapshot is empty. | Deterministic C# active-connection packet capture informed by Java spawned-player no-send guard. | C# active-player null is not identical to Java `!player.isSpawned()`; full teleporting/unspawned state remains unmodeled. |
| Existing `GameClientSocketServerNpcVisibilityTests` | Existing Unit / Integration | `PlayerController.see/notSee`, kisk `SM_NPC_INFO`, `SM_DELETE` | Existing kisk creature-type, appeared/delete ordering, removal delete, and payload assertions remained stable. | Focused 7-test suite passed. | Multi-viewer cleanup and exact Java runtime artifacts remain broader. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- C# no-active-player skip is a narrower approximation of Java `!getOwner().isSpawned()`; there is no explicit spawned/teleporting flag in this socket-server refresh branch.
- Java `KnownList.del` also calls `notifyNotKnow`; C# NPC visibility service only tracks object ids and packet deltas.
- Multi-viewer cleanup and objects known but not visible remain partially covered at service level, not full active socket integration.
- Player-owned aggro cleanup remains blocked by the missing C# `PlayerAggroList` equivalent.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped artifact rows in this unit
- Total artifacts ported: 0 production artifacts in this unit; 1 no-send integration regression added
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: Java runtime artifact generation, explicit spawned/teleporting state in socket visibility refresh, full Java known-list callback lifecycle, player-owned aggro model
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: continue kisk visibility/deletion parity with multi-viewer known-list cleanup or objects-known-but-not-visible active integration coverage.
- Alternative: switch to the dedicated player-owned aggro model design UOW to unblock revive aggro cleanup.

## Suggested Acceptance Criteria

- If continuing visibility deletion, prove delete fanout remains viewer-specific across multiple active connections, or prove objects known but not visible do not emit delete packets when removed.
- Keep active connection fixture changes sequential.
- Avoid production changes unless a test exposes a mismatch.
- Update the Migration Parity Table for every Java artifact touched.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Multi-viewer kisk visibility cleanup | `GameClientSocketServerNpcVisibilityTests.cs` | Medium | Sequential if using shared active fixture. |
| B | Known-but-not-visible service regression | `NpcVisibilityServiceTests.cs` | Low | Could be isolated from active socket fixture. |
| C | Player aggro model design | future docs/model/service files | High | Do not combine with revive live wiring. |
| D | Read-only Java known-list visibility-state audit | Java known-list/controller files only | Low | Safe read-only sub-agent candidate. |

## Do Not Parallelize

- Shared active connection fixture changes.
- Shared kisk workflow or flight-zone fixtures.
- Shared progress/handoff docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1464] Cover kisk visibility delete skip`.
- Files changed in UOW-1464:
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameClientSocketServerNpcVisibilityTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AKN-Completion.md`
