# Phase 6AKL Completion - Socket NPC Refresh Delete Order

Date: 2026-05-27
Unit of Work: UOW-1462
Status: Complete after validation.

## Scope

Add an active socket-server regression for NPC/kisk visibility refresh ordering: appeared `SM_NPC_INFO` before disappeared `SM_DELETE`.

## Completed Work

- Added an active-player loopback fixture to `GameClientSocketServerNpcVisibilityTests`.
- Added `RefreshNpcVisibilityAsync_SendsAppearedNpcInfoBeforeDisappearedDelete`.
- Primed a viewer's known-list with an old kisk/NPC object, refreshed with a replacement NPC snapshot, and captured packets sent through the real `GameServerConnection.SendPacketAsync` path.
- Asserted `SmNpcInfo` is sent before `SmDelete`.
- Serialized the emitted `SmDelete` and verified it targets the old object id with the default fade-out animation.
- Left production code unchanged.

## Validation

- First focused run failed to compile because the test file lacked the `Aion.GameServer.Model` namespace for `ObjectDeleteAnimation`; added the import.
- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~GameClientSocketServerNpcVisibilityTests"`.
- Result: passed 5 tests.

## Migration Parity Table - UOW-1462

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.PlayerController` | `GameClientSocketServer.RefreshNpcVisibilityAsync` | Controller / Packet Fanout | Partial | Integration Tested | Partial Parity | Active socket-server regression now proves appeared NPC info is sent before disappeared delete packets for one refresh cycle. Java known-list update cadence and multi-object controller callbacks remain broader. |
| `com.aionemu.gameserver.world.knownlist.KnownList` | `NpcVisibilityService` | Service / Known List | Partial | Integration Tested | Partial Parity | Test drives real socket-server use of known-list deltas after priming the viewer's known NPC state. Java lock/region update behavior and non-NPC categories remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_NPC_INFO` | `SmNpcInfo` | Packet | Partial | Integration Tested | Partial Parity | Test asserts packet type ordering through `GameServerConnection.SendPacketAsync`; detailed byte parity is covered by separate packet tests, not Java runtime comparison. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_DELETE` | `SmDelete` | Packet | Partial | Integration Tested | Partial Parity | Test serializes the emitted delete packet and verifies object id plus fade-out animation. Java runtime byte artifact comparison remains blocked. |
| `com.aionemu.gameserver.model.gameobjects.Kisk` | `WorldNpc` / `PlayerKiskRuntimeState` | Runtime State / World Object | Partial | Integration Tested | Partial Parity | The old object is represented as a kisk NPC id in the visibility refresh, but the test does not require registry metadata because deletion is driven by known-list disappearance. Kisk-specific cleanup remains covered by prior units. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `RefreshNpcVisibilityAsync_SendsAppearedNpcInfoBeforeDisappearedDelete` | Integration / socket-server | `PlayerController.see`, `PlayerController.notSee`, `KnownList.updateKnownList` | Real active connection refresh sends `SmNpcInfo` for an appeared NPC before `SmDelete` for the disappeared old kisk/NPC object, and the delete payload targets the old object id with fade-out animation. | Deterministic C# active-connection packet capture plus payload inspection from reviewed Java controller packet types. | Does not compare generated Java runtime packets or model every known-list object category. |
| Existing `GameClientSocketServerNpcVisibilityTests` | Existing Unit | Kisk `SM_NPC_INFO` creature-type projection | Viewer-specific kisk packet projection remained stable. | Focused 5-test suite passed. | Full region/known-list lifecycle remains broader. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- The integration fixture uses reflection to set the active player, matching existing test patterns but not a real enter-world handshake.
- The test covers one appeared and one disappeared NPC in one refresh cycle; multi-viewer, loot-status, and region-boundary ordering remain broader.
- C# known-list service remains simpler than Java's full known-list/controller callback graph.
- Player-owned aggro cleanup remains blocked by the missing C# `PlayerAggroList` equivalent.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 0 production artifacts in this unit; 1 active socket-server regression slice added
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: Java runtime artifact generation, full Java known-list/controller lifecycle, multi-viewer refresh ordering, player-owned aggro model
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: continue kisk lifecycle by combining depleted-kisk removal cleanup with active socket-server visibility refresh in a focused integration-style regression.
- Alternative: switch to the dedicated player-owned aggro model design UOW to unblock revive aggro cleanup.

## Suggested Acceptance Criteria

- If continuing kisk removal integration, reuse the smallest possible active-player fixture and avoid production changes unless the test exposes a mismatch.
- Prove the removal refresh path can produce `SmDelete` for a previously known kisk after the kisk is removed from the world snapshot.
- Keep packet-byte claims limited to packets actually serialized in tests.
- Update the Migration Parity Table for every Java artifact touched.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Combined kisk removal + active visibility refresh regression | kisk revive / socket visibility tests | Medium | Sequential if sharing connection fixtures. |
| B | Player aggro model design | docs/model design, future service skeleton | High | Do not combine with revive live wiring. |
| C | Read-only Java controller/known-list audit | Java controller/known-list files only | Low | Safe read-only sub-agent candidate if tools are available. |

## Do Not Parallelize

- Shared active connection fixture changes.
- Shared kisk workflow or flight-zone fixtures.
- Shared progress/handoff docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1462] Cover socket npc refresh delete order`.
- Files changed in UOW-1462:
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameClientSocketServerNpcVisibilityTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AKL-Completion.md`
