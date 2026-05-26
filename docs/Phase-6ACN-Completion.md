# Phase 6ACN Completion Handoff

Date: May 26, 2026
Latest Unit of Work: UOW-1256
Status: Phase 6 continues; C# now has a disabled region/player snapshot prerequisite model for Java known-list population, but full region-backed known-list mutation and live bind-point dispatch remain disabled.

## Session Summary

UOW-1256 added `PlayerKnownListRegionSnapshotService`, a pure non-live model that projects player candidate ids from an owner region plus supplied neighbor regions. It records source counts, scanned region ids, owner/world/instance/unspawned/outside-region exclusions, ordering/deduplication flags, Java source breadcrumbs, and parity/live flags.

Files changed:

- `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListRegionSnapshotService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerKnownListRegionSnapshotServiceTests.cs`
- `docs/Phase-6-BindPointTeleport-KnownListRegionSnapshot.md`
- `docs/Phase-6-BindPointTeleport-PlayerKnownListPopulation-Design.md`
- `docs/Phase-6-BindPointTeleport-KnownListMembershipRefresh.md`
- `docs/Phase-6-BindPointTeleport-KnownListFanoutParity.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ACN-Completion.md`

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerKnownListRegionSnapshotServiceTests" --nologo` passed 3 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport|PlayerKnownList" --nologo` passed 220 tests.
- No Java runtime known-list comparison was run.
- No live `GameServerConnection` dispatch was enabled.

## Migration Parity Table - UOW-1256

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.world.MapRegion.getObjects` / `MapRegion.getNeighbours` | `Aion.GameServer.Services.PlayerKnownListRegionSnapshotService` | Region Snapshot / Prerequisite Model | Partial | Unit Tested | Partial Parity | C# can model owner-region and neighbor-region player candidate inputs with ordering, same-world/instance filtering, owner exclusion, spawned filtering, and deduplication. It is not backed by live region storage. |
| `com.aionemu.gameserver.world.knownlist.KnownList.findVisibleObjects` | `PlayerKnownListRegionSnapshotService.BuildSnapshot` | Known-List Population Candidate Projection | Partial | Unit Tested | Needs Verification | Models region candidate selection only. Missing range check, max visible-distance negotiation, `canSee`, already-known check, two-way `KnownList.add`, flag-NPC whole-instance scan, and controller side effects. |
| `com.aionemu.gameserver.world.WorldMapInstance` | future C# region/object index plus current snapshot request | World Instance Storage | Not Started | Unit Tested | Needs Verification | Snapshot request can carry instance id, but no live `WorldMapInstance.addObject/removeObject` or region bucket storage exists. |
| `com.aionemu.gameserver.world.knownlist.KnownList.isAwareOf` | owner exclusion in `PlayerKnownListRegionSnapshotService` | Awareness Guard | Partial | Unit Tested | Partial Parity | Owner exclusion is modeled for players. Null/non-player/all-object awareness modifiers are not modeled. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` action `3` fanout | future known-list refresh fed by region snapshot plus existing bind-point fanout metadata | Service / Fanout Prerequisite | Partial | Regression Tested | Needs Verification | Region snapshot can become a better input to membership refresh later. It is not wired to action `3`, sockets, scheduler, movement, or live callbacks. |

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 prerequisite region snapshot service plus 3 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 3 grouped rows
- Total blocked artifacts: 1 live region object store, 1 full known-list mutation engine, 1 visibility/range/can-see engine, 1 controller packet side-effect dispatcher, 1 live bind-point scheduled callback path, and 1 Java runtime capture path
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- This is a pure prerequisite model and remains unwired.
- No live region object storage exists.
- Range, max visible-distance negotiation, `canSee`, hidden visibility, existing membership, and two-way mutation remain missing.
- The Java player-owner flag-NPC whole-instance scan is not modeled here.
- Controller packet side effects remain missing.
- Threading differences remain unverified; this model is immutable-result/pure computation, while Java mutates concurrent known-list maps.
- Serialization, date/time, precision/rounding, and reflection behavior did not change.

## Next Work Options

### Recommended Sequential Task

- Task: Add a disabled adapter that converts `PlayerKnownListRegionSnapshot` candidate ids into `PlayerKnownListMembershipService` metadata.
- Scope:
  - Consume a supplied region snapshot and upsert player membership metadata for the owner.
  - Preserve `IsLive=false` and `IsJavaRegionKnownListParity=false`.
  - Report missing range/can-see/two-way mutation limitations explicitly.
  - Do not wire to `GameServerConnection`, registry refresh, sockets, scheduler, or movement.
  - Add focused tests for candidate upsert, owner exclusion already enforced by snapshot, stale removal policy, and disabled/non-live flags.

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Region snapshot to membership adapter | new service/tests/docs | Medium | Best next executable step. Keep non-live. |
| B | Known-list packet side-effect design | docs only | Low/Medium | Useful before dispatching `see`/`notSee` packets. |
| C | Region object store design audit | docs only | Low/Medium | Useful before live world lifecycle work. |
| D | Region snapshot extra edge tests | tests only | Low | Add empty/null/duplicate-world cases if avoiding adapter work. |

### Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Explorer | Inspect current membership refresh service and decide stale-removal behavior for region snapshot adapter | read-only Java/C# inspection | all writes |
| Worker | Implement disabled region snapshot membership adapter and focused tests | new service/test/doc files only | `GameServerConnection`, socket executor live wiring, world mutation paths |
| Orchestrator | Integrate docs, parity tables, progress, and handoff | docs/progress/handoff | production behavior changes outside the adapter |

### Do Not Parallelize

- Live known-list population with socket execution.
- `GameServerConnection` dispatch with approximation-only membership.
- Region object storage and controller packet side-effect dispatch in the same unit.
- Shared progress/handoff/parity docs; keep them orchestrator-owned.

## Context Needed By Next Session

- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/world/knownlist/KnownList.java`
  - `game-server/src/com/aionemu/gameserver/world/MapRegion.java`
  - `game-server/src/com/aionemu/gameserver/world/WorldMapInstance.java`
  - `game-server/src/com/aionemu/gameserver/world/World.java`
- C# surfaces:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListRegionSnapshotService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListMembershipService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListMembershipRefreshService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListMembershipRegistryRefreshAdapterService.cs`
- Latest completed commits:
  - `b2292fc68 [Phase 6][UOW-1254] Add player known-list registry refresh adapter`
  - `724d683fe [Phase 6][UOW-1255] Audit player known-list population requirements`
  - next commit should be `[Phase 6][UOW-1256] Add player known-list region snapshot model`

Keep live bind-point behavior disabled until Java-equivalent known-list membership population, source-first fanout execution, live scheduled callback dispatch, final movement packet ordering, live scheduled task ownership, and Java packet/runtime validation have focused parity slices.
