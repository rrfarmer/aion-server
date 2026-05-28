# Phase 6AQU Completion - Nearby Region Snapshot Planner

Date: 2026-05-28
Unit of Work: UOW-1627
Status: Complete after focused unit tests

## Scope

This unit added a non-live C# nearby region-key and region-membership planner for Java-like map-region identity and per-player snapshot assembly. The Java source of truth is still `WorldMapInstance.forEachPlayer` feeding `PlayerController.updateNearbyQuests`, where each player resolves quest ids through its current `WorldPosition.mapRegion.parent`.

Live timers, packet sends, Java source, and production dispatcher wiring remain disabled.

## Completed Work

- Added `NearbyQuestRegionSnapshotService`.
- Added `NearbyQuestRegionKey`, `NearbyQuestRegionPlayer`, `NearbyQuestRegionSnapshotRequest`, and `NearbyQuestRegionSnapshot`.
- Built delayed refresh player inputs from spawned players in the requested world/instance.
- Preserved supplied player ordering for deterministic non-live planning.
- Counted unspawned and different-world/different-instance exclusions.
- Added tests proving the planner does not apply known-list owner/neighbour filtering.
- Updated `docs/PHASE-6-PROGRESS.md` with discovery, parity table, tests, risks, metrics, and next-unit guidance.

## Java Parity Notes

- Java delayed nearby refresh uses `WorldMapInstance.forEachPlayer(player -> player.getController().updateNearbyQuests())`.
- Java `PlayerController.updateNearbyQuests` reads `player.position.mapRegion.parent.questIds`, filters through `QuestService.checkStartConditions`, and unconditionally sends `SM_NEARBY_QUESTS`.
- The new C# planner is a prerequisite model only. It does not model Java `ConcurrentHashMap` iteration, live `MapRegion` buckets, controller calls, packet sends, or scheduler timing.
- `PlayerKnownListRegionSnapshotService` was reviewed and intentionally not reused because Java known-list semantics exclude the owner, scan neighbouring regions, and dedupe candidates. Delayed nearby refresh should iterate instance players and evaluate each player's current map-region parent.

## Validation

Ran:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~NearbyQuestRegionSnapshotServiceTests|FullyQualifiedName~NearbyQuestDelayedRefreshExecutionReportServiceTests|FullyQualifiedName~NearbyQuestRefreshInputAdapterServiceTests|FullyQualifiedName~PlayerKnownListRegionSnapshotServiceTests|FullyQualifiedName~WorldMapRuntimeStateTests"
```

Result: passed 38 tests.

Full game-server suite was not rerun in this unit.

## Parallel Work Discovery

| Candidate | Files / Area | Risk | Selected | Notes |
| --- | --- | --- | --- | --- |
| Nearby region-key/membership model | new nearby region snapshot service/tests | Low | Yes | Establishes nearby-specific player snapshot semantics before live dispatch. |
| Known-list region snapshot reuse audit | read-only known-list service/tests | Low | Partial | Reviewed during selection; semantics differ, so it was not reused. |
| ItemCharge multi-item packet/order audit | ItemCharge tests/read-only Java | Medium | No | Independent safe alternative. |
| Live nearby refresh dispatch | world/connection services | High | No | Deferred until live region storage and player iteration exist. |

No sub-agent was spawned because the work touched one new service, one test file, and orchestrator-owned docs.

## Tests Added Or Updated

| Test | Change | Validates | Java Comparison |
| --- | --- | --- | --- |
| `BuildSnapshot_AssemblesSpawnedSameInstancePlayersForDelayedRefresh` | Added | Spawned same-world/same-instance players become ordered delayed refresh inputs with position and parent instance metadata. | Static source review of `WorldMapInstance.forEachPlayer` and `PlayerController.updateNearbyQuests`; no Java runtime comparison. |
| `BuildSnapshot_ExcludesUnspawnedAndDifferentWorldOrInstancePlayers` | Added | Unspawned and different-world/different-instance players are excluded and counted. | Based on Java spawn/despawn and instance membership lifecycle; no concurrent runtime comparison. |
| `BuildSnapshot_DoesNotApplyKnownListOwnerOrNeighbourFiltering` | Added | Nearby planning keeps supplied same-instance players without known-list owner/neighbour filtering. | Confirms delayed nearby refresh is not coupled to `KnownList.findVisibleObjects` semantics. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.world.WorldMapInstance.forEachPlayer` | `Aion.GameServer.Services.NearbyQuestRegionSnapshotService.BuildSnapshot`; `NearbyQuestRegionSnapshot` | Player Iteration Prerequisite | Partial | Unit Tested | Partial Parity | Models spawned same-world/same-instance player input assembly and preserves supplied ordering. It does not execute live Java `ConcurrentHashMap` iteration, runtime ordering, or controller dispatch. |
| `com.aionemu.gameserver.world.MapRegion` | `Aion.GameServer.Services.NearbyQuestRegionKey`; `NearbyQuestMapRegionSnapshot` | Region Boundary DTO | Partial | Unit Tested | Needs Verification | Region identity is explicit metadata only. Missing live objects, neighbours, activation/deactivation, synchronized player counts, zone revalidation, and Java object membership behavior. |
| `com.aionemu.gameserver.world.WorldPosition` | `Aion.GameServer.World.WorldPosition` plus nearby snapshot position metadata | Position Model | Partial | Unit Tested | Needs Verification | Snapshot carries current C# position into the existing map-region adapter. Java mutable `mapRegion` pointer, spawned flag, null-region behavior, threading, and position mutation semantics remain unported. |
| `com.aionemu.gameserver.controllers.PlayerController.updateNearbyQuests` | `NearbyQuestDelayedRefreshPlayerInput` assembled by `NearbyQuestRegionSnapshotService` | Controller Input Prerequisite | Partial | Unit Tested | Partial Parity | Assembles inputs for the existing map-region adapter that models `player.position.mapRegion.parent.questIds`. No live controller call, `PacketSendUtility`, serialization, or socket ordering. |
| `com.aionemu.gameserver.world.knownlist.KnownList.findVisibleObjects` | `Aion.GameServer.Services.PlayerKnownListRegionSnapshotService` | Existing Region Snapshot Dependency | Partial | Existing Unit Tested + Manual Review | Needs Verification | Discovered dependency considered but intentionally not reused. Known-list owner exclusion, neighbour scans, and dedupe would be incorrect for delayed nearby refresh. Existing service remains snapshot-only and not live-backed. |

## Remaining Risks

- This is a non-live planner only.
- C# still lacks live `MapRegion` storage, Java region-id calculation, neighbour arrays, activation/deactivation, zone revalidation, object buckets, and synchronized player-count behavior.
- Java `WorldPosition` mutable map-region/spawned state is represented only through explicit C# snapshot metadata.
- Supplied ordering is deterministic in tests, but Java `ConcurrentHashMap` iteration order is not runtime-compared.
- Live controller invocation, `PacketSendUtility`, encrypted packet bytes, socket ordering, threading, and dynamic handlers remain unverified.
- `docs/commit-conventions.md` is still missing.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped rows.
- Total artifacts ported: 1 non-live planner service plus 3 focused tests.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification: 3 grouped rows explicitly marked Needs Verification.
- Total blocked artifacts: live MapRegion storage, Java region-id calculation, live world player iteration, live controller dispatch, live timer scheduling, packet send/encrypted frame comparison, Java runtime comparison.
- Estimated overall migration completion: about 72%.

## Next Recommended Unit Of Work

Next best unit:

| Candidate | Files / Area | Notes |
| --- | --- | --- |
| Planner-to-report composition | nearby region snapshot service/report tests or a small composition helper | Build `NearbyQuestRegionSnapshotService` output and feed it into `NearbyQuestDelayedRefreshExecutionReportService.CreateReportFromMapRegions`. |
| ItemCharge charge-all multi-item packet/order audit | existing ItemCharge tests/read-only Java | Independent safe implementation alternative. |
| Nearby packet golden gap audit | packet tests/docs | Keep read-only unless adding isolated packet tests. |

## Next Work Options

## Recommended Sequential Task

- Task: add a non-live composition test or helper that builds a `NearbyQuestRegionSnapshot` and passes its `PlayerInputs` into `CreateReportFromMapRegions`.
- Why: this proves the planner-to-report boundary before live delayed dispatcher wiring.
- Files: likely `NearbyQuestDelayedRefreshExecutionReportServiceTests.cs`, possibly a small helper if the existing API is awkward.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
| --- | --- | --- | --- | --- |
| A | ItemCharge multi-item ordering audit | read-only Java/C# ItemCharge files or focused tests | Medium | Independent from nearby files. |
| B | Nearby packet golden gap audit | packet tests/docs read-only unless assigned | Low | Avoid live send work. |
| C | Region-id calculation research | read-only Java world map region classes | Low | Useful before live region storage. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
| --- | --- | --- | --- |
| Orchestrator | Planner-to-report composition test/helper | nearby region snapshot/report tests, docs | live dispatch, Java writes |
| Read-only Agent | ItemCharge multi-item ordering audit | read-only Java/C# ItemCharge files | all writes |

## Do Not Parallelize

- Live `GameServerConnection` / `PacketSendUtility` sends: still high risk and intentionally disabled.
- Java source files: read-only only.
- Shared nearby report service edits: one owner only if touched.

## Continuation Context

Current unit commit message:

```text
[Phase 6][UOW-1627] Add nearby region snapshot planner
```

Files changed in this unit:

- `dotnetConversion/src/Aion.GameServer/Services/NearbyQuestRegionSnapshotService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/NearbyQuestRegionSnapshotServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6AQU-Completion.md`

Required startup reading for the next continuation:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parallelization-strategy.md`
- `docs/parity-verification.md`
- `docs/PHASE-6-PROGRESS.md`
- Latest completion handoff, currently this file

Note: `docs/commit-conventions.md` is still missing.
