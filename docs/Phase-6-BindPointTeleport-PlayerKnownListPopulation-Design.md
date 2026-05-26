# Phase 6 Bind-Point Teleport Player Known-List Population Design

Date: May 26, 2026
Unit of Work: UOW-1255
Scope: Documentation/design audit for Java-equivalent player known-list population before any live fanout wiring.
Source of truth: Java project.

## Summary

UOW-1255 confirms that current C# player known-list work is useful metadata, but it is not full Java known-list parity. Java known-list population is owned by central world lifecycle methods, region-backed object storage, bidirectional `KnownList` mutation, cached visible state, and controller packet side effects.

Do not wire the registry refresh adapter or bind-point known-list socket executor into live scheduled callback dispatch yet. The next safe work is the world/region/player known-list population foundation.

## Java Source Findings

Java `World.spawn(VisibleObject)` establishes the lifecycle order:

1. `object.getController().onBeforeSpawn()`
2. `position.setIsSpawned(true)`
3. `position.getMapRegion().getParent().addObject(object)`
4. `position.getMapRegion().add(object)`
5. `object.getController().onAfterSpawn()`
6. `object.updateKnownlist()`

Java `World.updatePosition(...)` only moves spawned objects. It resolves old/new `MapRegion`, updates XYZH, revalidates zones when the region changes, removes the object from the old region, adds it to the new region, sets the new region, then calls `object.updateKnownlist()` when `updateKnownList` is true.

Java `World.despawn(object, animation)` calls `onDespawn()`, marks the position unspawned, removes the object from `WorldMapInstance` and `MapRegion`, revalidates zones for creatures, then calls `object.clearKnownlist(animation)`.

Java `KnownList.update()` is synchronized and ordered:

1. `forgetObjectsOrUpdateVisibility()`
2. `findVisibleObjects()`

`KnownList.findVisibleObjects()` has several parity-critical details:

- if the owner is not spawned, it returns immediately;
- player owners first scan flag NPCs across the whole `WorldMapInstance`;
- normal discovery scans `position.getMapRegion().getNeighbours()`;
- owner/null candidates are rejected by `isAwareOf`;
- known candidates are skipped;
- range uses the maximum of both objects' visible distances;
- membership is bidirectional, and `newObject.getKnownList().add(owner)` must succeed before `owner.add(newObject)`.

`KnownList.clear(animation)` removes owner-side knowledge with `ObjectDeleteAnimation.NONE`, then removes the owner from each other object's known-list using the supplied animation. Because `World.despawn` marks the despawning object unspawned before clear, delete packets are skipped to the despawning player while other players can still receive delete side effects.

Java `KnownObject.visible` is separate from membership. A known object can remain known while toggling visible/invisible through `owner.canSee(object)`, hide/search behavior, and `updateVisibleObject`.

## Packet Side Effects

Java known-list population is not only data structure mutation. `KnownList.updateVisibility` calls controller hooks:

- `PlayerController.see(Player)` sends `SM_PLAYER_INFO`, `SM_MOTION`, ride `SM_EMOTION`, and stance `SM_PLAYER_STANCE`.
- `PlayerController.notSee(...)` sends `SM_DELETE` for most objects while the viewer remains spawned.
- `PlayerController.see(Npc)` sends `SM_NPC_INFO`, kisk updates, quest distance callbacks, drop visibility, and abnormal effects where applicable.
- Pet, house, house-object, gatherable, summon, and static-object packet paths are also controller side effects.
- `notKnow` is currently empty on `VisibleObjectController`, but creature controllers can use not-know removal semantics such as aggro cleanup.

These side effects mean a live C# port cannot claim Java parity by only updating `PlayerKnownListMembershipService`.

## Current C# State

Current C# surfaces:

- `Aion.GameServer.World.World` stores objects by id and supports `TryAddObject`, `TryRemoveObject`, and `TryUpdateObject`, but it does not own Java-equivalent spawned state, `WorldMapInstance.addObject`, `MapRegion.add/remove`, or `KnownList.update`.
- `WorldMapRuntimeState` and `WorldMapInstanceRuntimeState` track instance/player/quest ids, but not visible objects by map region or neighbor collections.
- `WorldVisibility.IsVisibleTo` is a same-world/default-distance helper. It does not account for instance id, region neighbors, max visible distance across both objects, hidden state, `canSee`, or awareness modifiers.
- `PlayerKnownListMembershipService` models player-player membership metadata and visible flags, but all snapshots are non-live.
- `PlayerKnownListMembershipRefreshService` refreshes from supplied online players plus `WorldVisibility`; it is explicitly not Java `MapRegion` parity.
- `PlayerKnownListMembershipRegistryRefreshAdapterService` reads `IGameClientConnectionRegistry.ForEachOnlinePlayer` only when enabled; it remains disabled and unwired.
- Bind-point known-list fanout planning/execution metadata exists, but it depends on precomputed membership snapshots and is not connected to live scheduled callbacks.
- `GameServerConnection` still uses distance-visible player broadcasts in enter/logout paths and does not perform Java known-list delta packet dispatch.

## Required Staging Before Live Wiring

1. Add region/instance object indexing for visible objects, players, NPCs, and neighbor regions keyed by world id, instance id, and region.
2. Introduce central C# lifecycle methods that mirror Java `spawn`, `updatePosition`, and `despawn` ordering without immediately changing live dispatch.
3. Implement a player-player `KnownObject`/`KnownList` foundation with bidirectional add/remove, owner exclusion, known vs visible state, and Java update order.
4. Add visibility recomputation with explicit `Needs Verification` handling for hidden/search/can-see behavior.
5. Add packet side-effect plan/dispatcher surfaces for `see`, `notSee`, and `notKnow` before sending live packets from known-list deltas.
6. Route player enter/logout/teleport/move through disabled or opt-in lifecycle adapters before replacing existing distance broadcasts.
7. Only after that, evaluate live bind-point action `3` fanout through known-list membership rather than registry distance scans.

## Validation

- No production code changed in this unit.
- No tests were added or run for this documentation-only audit.
- Validation was a read-only source audit of the Java and current C# lifecycle surfaces.
- No Java runtime comparison was executed.

## Migration Parity Table - UOW-1255

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.world.World.spawn` / `World.updatePosition` / `World.despawn` | `Aion.GameServer.World.World`; future central lifecycle adapter | World Lifecycle | Not Started | Manual Only | Needs Verification | Current C# object container does not mirror Java spawned state, map-instance add/remove, region add/remove, update-known-list order, despawn clear order, or zone revalidation coupling. |
| `com.aionemu.gameserver.world.WorldMapInstance` / `com.aionemu.gameserver.world.MapRegion` | `Aion.GameServer.World.WorldMapRuntimeState`; future region index | World Region Storage | Not Started | Manual Only | Needs Verification | Current runtime state tracks instance/player ids but not visible-object region buckets, neighbor scans, or instance-aware known-list discovery. |
| `com.aionemu.gameserver.world.knownlist.KnownList.update` / `findVisibleObjects` / `forgetObjectsOrUpdateVisibility` / `clear` | `PlayerKnownListMembershipService`; `PlayerKnownListMembershipRefreshService`; `PlayerKnownListMembershipRegistryRefreshAdapterService` | Known-List Lifecycle | Partial | Unit Tested | Partial Parity | C# has non-live player-player metadata and registry/distance approximation. Missing Java region scans, bidirectional world-object mutation, synchronized update ordering, clear packet side effects, hidden visibility retention, and live wiring. |
| `com.aionemu.gameserver.world.knownlist.KnownObject` | `Aion.GameServer.Services.PlayerKnownListMembershipEntry` | Known-Object / Visibility Cache | Partial | Unit Tested | Needs Verification | C# entry has `IsVisibleToOwner`, but no Java `owner.canSee` recomputation, pet visibility cascade, or all-object membership. |
| `com.aionemu.gameserver.controllers.PlayerController.see` / `notSee` / `VisibleObjectController.notKnow` | future known-list packet side-effect dispatcher | Controller / Packet Side Effects | Not Started | Manual Only | Needs Verification | Missing player spawn/delete side-effect dispatcher, ride/stance motion ordering, NPC/drop/quest side effects, and aggro/not-know cleanup behavior. |
| `com.aionemu.gameserver.utils.PacketSendUtility.broadcastPacket(Player,AionServerPacket,boolean)` / `BindPointTeleportService.teleport` action `3` | bind-point known-list fanout metadata stack plus disabled socket executor | Service / Fanout | Partial | Regression Tested | Needs Verification | Fanout execution can preserve source-first and precomputed membership order in tests, but live population and scheduled callback dispatch remain disabled. |

## Tests Added

No tests were added in UOW-1255. This unit intentionally produced the design audit that should guide the next executable region/known-list population slice.

## Remaining Risks

- Region-backed world object storage is not ported.
- Full bidirectional Java known-list mutation is not ported.
- Hidden/invisible-but-known retention is not ported.
- Controller packet side effects are not ported as known-list deltas.
- Current registry/distance fanout is not equivalent to Java known-list fanout.
- Threading differs: Java combines `ConcurrentHashMap` with synchronized update/clear, while C# currently uses per-owner dictionary locks in a metadata service.
- Serialization did not change in this unit, but live packet ordering remains unverified.
- Date/time and precision/rounding behavior did not change.
- Reflection behavior did not change.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 0 code artifacts; 1 design audit document
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: 1 region-backed world storage path, 1 central lifecycle path, 1 full bidirectional known-list path, 1 controller packet side-effect path, 1 live bind-point scheduled callback path, and 1 Java runtime capture path
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a small, test-first region/player snapshot model for Java known-list population prerequisites. Keep it disabled/unwired, but make it capable of representing world id, instance id, region id, owner exclusion, neighbor candidate ordering, and player object ids so `PlayerKnownListMembershipRefreshService` can later move away from flat registry snapshots.

## Update After UOW-1256

`PlayerKnownListRegionSnapshotService` now provides that disabled prerequisite model. It can project player candidate ids from an owner region plus supplied neighbor regions while preserving owner exclusion, same-world/instance filtering, spawned filtering, deduplication, and first-seen region ordering metadata.

It is still not Java region known-list parity because no live region object store, distance/range check, `canSee` visibility recomputation, already-known check, two-way add/remove, flag-NPC scan, or controller packet side effects execute.

## Update After UOW-1257

`PlayerKnownListRegionMembershipAdapterService` now adapts a supplied region snapshot into non-live membership metadata. This reduces the gap between region-shaped candidate projection and bind-point known-list fanout metadata, but full Java parity still requires two-way membership mutation, live region object ownership, visibility recomputation, and controller packet side effects.

## Update After UOW-1258

`PlayerKnownListTwoWayOperationPlanService` now models Java two-way known-list add/remove/clear ordering as non-live metadata. It still does not execute live membership mutation, `canSee`, range checks, region scans, packet side effects, or cross-list locking.

## Update After UOW-1259

`PlayerKnownListTwoWayMembershipAdapterService` now applies operation-plan membership steps to `PlayerKnownListMembershipService` only when explicitly enabled. This still does not create Java live known-list parity because range/visibility, region object storage, controller side effects, and live locking remain missing.

## Update After UOW-1260

`PlayerKnownListVisibilityRangePlanService` now models Java max visible-distance range and caller-supplied `canSee` inputs as non-live metadata. The next safe step is a disabled composition layer over region snapshots, visibility/range plans, two-way operation plans, and membership application.

## Update After UOW-1261

`PlayerKnownListPopulationPlanService` now provides that disabled composition layer. It remains non-live and makes the next blocker clearer: packet/controller side-effect descriptors for player `see` and `notSee` transitions.

## Update After UOW-1262

`PlayerKnownListPlayerSideEffectPlanService` now models player-player `see` and `notSee` packet side effects as descriptors: `SM_PLAYER_INFO`, `SM_MOTION`, optional ride `SM_EMOTION`, optional `SM_PLAYER_STANCE`, optional `SM_ABNORMAL_EFFECT`, and spawned-viewer `SM_DELETE`. It remains non-live. The planner also records that `SmPlayerInfo` lacks Java enemy/aggro flag behavior and that `SmPlayerStance` and `SmAbnormalEffect` are missing C# packet classes.

## Update After UOW-1263

`PlayerKnownListOperationSideEffectAttachmentService` now attaches those player packet descriptors to two-way known-list operation steps. This reduces the gap between membership operation planning and controller packet intent, but live known-list callbacks remain blocked on packet serializer gaps, runtime player facts, and `GameServerConnection` dispatch.

## Update After UOW-1264

`PlayerKnownListPopulationPlanService` now preserves operation side-effect attachments in its candidate results. The end-to-end non-live population chain can now carry region, range, operation, membership, and controller packet-intent metadata, but it remains disabled from live world lifecycle and socket dispatch.

## Update After UOW-1265

`SmPlayerInfo` now supports Java's enemy/aggro creature-type flag as a focused packet prerequisite for future player `see` dispatch. Full player-info parity still needs active-viewer race projection, neutral custom-state handling, and live controller packet execution.

## Update After UOW-1266

`SmPlayerInfo` now models Java's viewer-sensitive race byte from supplied scalar facts: active-player race, active-player-is-enemy-to-visible-player, and neutral-to-all override. Population planning still carries only descriptors and supplied packet facts; it does not hydrate live active-player context, custom player states, or controller sends.

## Update After UOW-1267

`SmPlayerStance` now covers Java's object-id plus one-byte state payload and the player side-effect descriptor marks stance support available. Population planning is still metadata-only and does not hydrate live stance controller state or send packets.

## Update After UOW-1268

`SmAbnormalEffect` now covers Java-shaped player and non-player abnormal-effect payloads from supplied facts, including slot filtering. Population planning remains metadata-only and does not hydrate live effect-controller masks/effects or send packets.

## Update After UOW-1269

`PlayerKnownListPlayerSideEffectPacketConstructionService` can construct packet metadata for one supplied player side-effect plan. Population planning still needs a separate attachment-level bridge before candidate side-effect plans can carry packet construction metadata end to end.
