# Phase 6 Bind-Point Teleport Known-List Region Snapshot

Date: May 26, 2026
Unit of Work: UOW-1256
Scope: Add a disabled, test-first region/player snapshot model for Java known-list population prerequisites.
Source of truth: Java project.

## Summary

UOW-1256 adds `PlayerKnownListRegionSnapshotService`, a pure C# prerequisite model that can describe which player object ids would be considered from an owner's current region plus supplied neighbor regions.

This is not live Java known-list parity. It does not mutate membership, compute distance, call `canSee`, perform two-way add/remove, send packets, or wire into `GameServerConnection`. It replaces none of the current live distance broadcasts. Its purpose is to move future `PlayerKnownListMembershipRefreshService` work away from flat online registry snapshots and toward Java-shaped map-region candidate inputs.

## Java Source Findings

- Java `KnownList.findVisibleObjects()` scans `position.getMapRegion().getNeighbours()` after the player-owner flag-NPC whole-instance scan.
- Java rejects owner/null candidates through `isAwareOf`.
- Java only adds a candidate when it is in range and not already known.
- Java membership is bidirectional: `newObject.getKnownList().add(owner)` must succeed before owner-side `add(newObject)`.
- Java source objects come from `MapRegion.getObjects().values()`; C# still lacks that region object store.

## C# Implementation

Added `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListRegionSnapshotService.cs`:

- `PlayerKnownListRegionKey` records world id, instance id, and region id.
- `PlayerKnownListRegionPlayer` records a player object id, modeled region, spawned flag, and Java source breadcrumb.
- `PlayerKnownListRegionSnapshotRequest` provides owner id, owner region, neighbor region ids, and candidate players.
- `PlayerKnownListRegionSnapshot` records scanned regions, candidate player ids, exclusion counts, ordering/deduplication flags, Java source, and live/parity flags.
- `PlayerKnownListRegionSnapshotService.BuildSnapshot`:
  - always scans the owner region first;
  - appends supplied neighbor regions in first-seen order;
  - filters by same world and same instance;
  - excludes owner;
  - excludes unspawned players;
  - excludes players outside scanned regions;
  - deduplicates candidate object ids;
  - remains non-live and marks `IsJavaRegionKnownListParity=false`.

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerKnownListRegionSnapshotServiceTests" --nologo` passed 3 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport|PlayerKnownList" --nologo` passed 220 tests.
- No Java runtime comparison was executed.
- No live `GameServerConnection` dispatch was enabled.

## Migration Parity Table - UOW-1256

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.world.MapRegion.getObjects` / `MapRegion.getNeighbours` | `Aion.GameServer.Services.PlayerKnownListRegionSnapshotService` | Region Snapshot / Prerequisite Model | Partial | Unit Tested | Partial Parity | C# can model owner-region and neighbor-region player candidate inputs with ordering, same-world/instance filtering, owner exclusion, spawned filtering, and deduplication. It is not backed by live region storage. |
| `com.aionemu.gameserver.world.knownlist.KnownList.findVisibleObjects` | `PlayerKnownListRegionSnapshotService.BuildSnapshot` | Known-List Population Candidate Projection | Partial | Unit Tested | Needs Verification | Models region candidate selection only. Missing range check, max visible-distance negotiation, `canSee`, already-known check, two-way `KnownList.add`, flag-NPC whole-instance scan, and controller side effects. |
| `com.aionemu.gameserver.world.WorldMapInstance` | future C# region/object index plus current snapshot request | World Instance Storage | Not Started | Unit Tested | Needs Verification | Snapshot request can carry instance id, but no live `WorldMapInstance.addObject/removeObject` or region bucket storage exists. |
| `com.aionemu.gameserver.world.knownlist.KnownList.isAwareOf` | owner exclusion in `PlayerKnownListRegionSnapshotService` | Awareness Guard | Partial | Unit Tested | Partial Parity | Owner exclusion is modeled for players. Null/non-player/all-object awareness modifiers are not modeled. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` action `3` fanout | future known-list refresh fed by region snapshot plus existing bind-point fanout metadata | Service / Fanout Prerequisite | Partial | Regression Tested | Needs Verification | Region snapshot can become a better input to membership refresh later. It is not wired to action `3`, sockets, scheduler, movement, or live callbacks. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `BuildSnapshot_IncludesOwnerRegionAndNeighbourPlayersWhileExcludingOwner` | Unit / Region Snapshot | `KnownList.findVisibleObjects`; `KnownList.isAwareOf` | Owner region is scanned with supplied neighbors; owner is excluded; outside-region player is excluded. | Source-derived candidate selection model. | No range, live region store, or Java runtime comparison. |
| `BuildSnapshot_PreservesRegionScanOrderAndDeduplicatesCandidatePlayers` | Unit / Region Snapshot | `MapRegion.getNeighbours`; `ConcurrentHashMap` object-id membership | Owner region is first, neighbor order is first-seen, duplicate candidate object ids are deduplicated. | Source-shaped ordering/deduplication metadata. | Java actual region/object iteration ordering still needs runtime verification. |
| `BuildSnapshot_ExcludesDifferentWorldInstanceAndUnspawnedPlayers` | Unit / Region Snapshot | Java world/instance scoping and `findVisibleObjects` spawned-owner guard | Different world, different instance, and unspawned candidates are excluded. | C# prerequisite guard coverage. | Java object spawned/candidate semantics are not fully modeled. |

## Remaining Risks

- This is a pure prerequisite model and remains unwired.
- No live region object storage exists.
- Range, max visible-distance negotiation, `canSee`, hidden visibility, existing membership, and two-way mutation remain missing.
- The Java player-owner flag-NPC whole-instance scan is not modeled here.
- Controller packet side effects remain missing.
- Threading differences remain unverified; this model is immutable-result/pure computation, while Java mutates concurrent known-list maps.
- Serialization, date/time, precision/rounding, and reflection behavior did not change.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 prerequisite region snapshot service plus 3 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 3 grouped rows
- Total blocked artifacts: 1 live region object store, 1 full known-list mutation engine, 1 visibility/range/can-see engine, 1 controller packet side-effect dispatcher, 1 live bind-point scheduled callback path, and 1 Java runtime capture path
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a disabled adapter that converts `PlayerKnownListRegionSnapshot` candidate ids into `PlayerKnownListMembershipService` metadata. Keep it non-live and explicit that it still lacks Java range/can-see/two-way world-object mutation parity.

## Update After UOW-1257

`PlayerKnownListRegionMembershipAdapterService` now converts supplied region snapshot candidate ids into non-live `PlayerKnownListMembershipService` metadata with a new `RegionSnapshotRefresh` update reason. It preserves existing membership by default and can optionally remove missing snapshot candidates, but it remains an approximation without live region storage, range/can-see parity, two-way add order, or controller packet side effects.
