# Phase 6AKM Completion - Kisk Removal Visibility Delete

Date: 2026-05-27
Unit of Work: UOW-1463
Status: Complete after validation.

## Scope

Combine runtime kisk removal with active socket-server NPC visibility refresh coverage.

## Completed Work

- Spawned one read-only explorer for Java kisk/known-list removal audit; it made no file changes and was closed after reporting.
- Audited Java deletion ordering: `Kisk.resurrectionUsed` / controller delete enters `World.removeObject`, removes from map/region visibility, clears known-lists, sends player `SM_DELETE` through `PlayerController.notSee`, calls `onDelete`, then removes from the global object table.
- Added `RemoveRuntimeKiskAsync_RefreshesKnownViewerWithDeleteAfterWorldRemoval`.
- Extended the existing NPC visibility fixture with `GameServerRuntimeContext`, `World`, and `IDFactory` so `GameServerConnection.RemoveRuntimeKiskAsync` can run through real registry/world cleanup.
- Verified a viewer that previously knew the kisk receives one serialized `SmDelete` for the removed kisk object id through the real socket-server refresh path.
- Left production code unchanged.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~GameClientSocketServerNpcVisibilityTests"`.
- Result: passed 6 tests.

## Migration Parity Table - UOW-1463

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.gameobjects.Kisk` | `PlayerKiskRuntimeState` / `WorldNpc` | Runtime State / World Object | Partial | Integration Tested | Partial Parity | Test removes a registered runtime kisk and verifies a previously known viewer receives `SmDelete`. Java `Kisk.resurrectionUsed()` deletes through controller/world known-list cleanup; C# still models removal through registry/world services and snapshot refresh. |
| `com.aionemu.gameserver.controllers.VisibleObjectController` | `GameServerConnection.RemoveRuntimeKiskAsync` / `PlayerKiskLifetimeService.DespawnExpiredKisk` | Controller / World Removal | Partial | Integration Tested | Partial Parity | C# removal deletes registry/world state and invokes cleanup refresh. Java `VisibleObjectController.delete -> World.removeObject` clears map/region known-lists before global object-table removal; C# does not yet model the full controller hierarchy or retained-object known-list clear. |
| `com.aionemu.gameserver.world.World` | `World.World` | World Registry | Partial | Integration Tested | Partial Parity | C# `World.TryRemoveObject` removes the object before `RefreshNpcVisibilityAsync(world.GetNpcs(worldId))`; Java removes map/region visibility and sends `SM_DELETE` before removing from `allObjects`. The packet outcome is covered, but lifecycle ordering differs. |
| `com.aionemu.gameserver.world.knownlist.KnownList` | `NpcVisibilityService` | Service / Known List | Partial | Integration Tested | Partial Parity | Test proves known viewer deletion via disappearance delta after kisk removal. Java sends `notSee` from known-list entries that hold the object reference and skips delete if the receiver is not spawned or the object was not visible. |
| `com.aionemu.gameserver.controllers.PlayerController` | `GameClientSocketServer.RefreshNpcVisibilityAsync` | Controller / Packet Fanout | Partial | Integration Tested | Partial Parity | Active socket-server path emits `SmDelete` to the viewer after kisk removal. Java `PlayerController.notSee` is per-player and conditional on spawned/visible state; C# fixture covers one spawned known viewer only. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_DELETE` | `SmDelete` | Packet | Partial | Integration Tested | Partial Parity | Test serializes emitted delete packet and verifies object id plus default fade-out animation. Java runtime byte artifact comparison remains blocked. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `RemoveRuntimeKiskAsync_RefreshesKnownViewerWithDeleteAfterWorldRemoval` | Integration / socket-server | `VisibleObjectController.delete`, `World.removeObject`, `KnownList.clear`, `PlayerController.notSee` | A viewer that previously knew the kisk receives one `SmDelete` through the real socket-server refresh after C# runtime kisk removal removes the object from world/registry state. | Deterministic C# active-connection packet capture plus serialized delete payload, informed by Java source audit. | C# lifecycle differs: delete is produced from post-removal snapshot delta, not Java's retained-object known-list clear before global object-table removal. No Java runtime packet artifact comparison. |
| Existing `GameClientSocketServerNpcVisibilityTests` | Existing Unit / Integration | `PlayerController.see/notSee`, kisk `SM_NPC_INFO` projection | Existing viewer-specific `SmNpcInfo`, appeared-before-disappeared ordering, and packet payload tests remained stable. | Focused 6-test suite passed. | Multi-viewer, loot-status ordering, teleport/unspawned skip branch, and full known-list categories remain broader. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- C# still lacks Java's full controller/known-list cleanup graph; current regression documents packet outcome, not identical lifecycle ordering.
- Java sends `SM_DELETE` only when the object was known and visible and skips when the receiving player is not spawned; C# coverage has one spawned visible viewer.
- Actual socket flush order under real networking queues remains broader than `sentPacketObserver` capture order.
- `KiskService.removeKisk` is handled in C# cleanup service but Java audit notes it is not wired directly into generic `delete()`; broader offline bind/member state parity remains partial.
- Player-owned aggro cleanup remains blocked by the missing C# `PlayerAggroList` equivalent.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 0 production artifacts in this unit; 1 combined active removal/visibility regression added
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: Java runtime artifact generation, full Java controller/known-list cleanup lifecycle, multi-viewer and unspawned-player delete behavior, player-owned aggro model
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: continue kisk visibility/deletion parity with the Java unspawned-viewer skip branch or multi-viewer known-list cleanup regression.
- Alternative: switch to the dedicated player-owned aggro model design UOW to unblock revive aggro cleanup.

## Suggested Acceptance Criteria

- If continuing visibility deletion, prove C# does not emit `SmDelete` for a viewer that did not know the kisk or should be treated as unspawned/teleporting, matching Java `PlayerController.notSee` skip semantics as closely as current models allow.
- Keep packet-byte claims limited to packets actually serialized in tests.
- Avoid production changes unless the test exposes a current mismatch.
- Update the Migration Parity Table for every Java artifact touched.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Kisk visibility delete skip branch | `GameClientSocketServerNpcVisibilityTests.cs` or `NpcVisibilityServiceTests.cs` | Medium | Sequential if using active connection fixture. |
| B | Player aggro model design | future docs/model/service files | High | Do not combine with revive live wiring. |
| C | Read-only Java `PlayerController.notSee` / spawned-state audit | Java controller/model files only | Low | Safe read-only sub-agent candidate. |

## Do Not Parallelize

- Shared active connection fixture changes.
- Shared kisk workflow or flight-zone fixtures.
- Shared progress/handoff docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1463] Cover kisk removal visibility delete`.
- Files changed in UOW-1463:
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameClientSocketServerNpcVisibilityTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AKM-Completion.md`
