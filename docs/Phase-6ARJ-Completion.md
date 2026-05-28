# Phase 6ARJ Completion - Sorted Zone Snapshot Composition

Date: 2026-05-28
Unit of Work: UOW-1642
Status: Complete after focused unit tests

## Scope

This unit composed UOW-1641 zone sort metadata into the non-live creation/runtime snapshot path. Snapshots now distinguish filtered zone ids from Java constructor-ordered zone ids, and they record missing sort metadata explicitly.

This remains a DTO/model layer. It does not instantiate `MapRegion`, create a live `ZoneInstance[]`, load XML `ZoneTemplate` data, or execute zone handlers.

## Completed Work

- Updated `WorldMapRegionCreationSnapshotService` to accept optional `WorldMapRegionZoneSortCandidate` metadata.
- Added `ConstructorOrderedZoneIds` to creation snapshots.
- Added `MissingZoneSortIds` to creation snapshots.
- Updated `WorldMapRegionRuntimeSnapshotService` and runtime snapshots to carry both fields forward.
- Preserved filtered `ZoneIds` separately from constructor order.
- Added a partial-sort-metadata regression test that blocks constructor-order output when any matched zone is missing sort keys.
- Updated existing creation/runtime snapshot tests for sorted-zone metadata.
- Updated `docs/PHASE-6-PROGRESS.md` with discovery, parity table, tests, risks, metrics, and next-unit guidance.

## Java Parity Notes

- Java `WorldMap2DInstance.createMapRegion` and `WorldMap3DInstance.createMapRegion` pass filtered zones into `new MapRegion(...)`.
- Java `MapRegion` stores the same array and immediately sorts it with `zoneComparator`.
- C# snapshots now preserve both phases:
  - `ZoneIds`: zones retained by region filtering.
  - `ConstructorOrderedZoneIds`: matched zones after Java comparator ordering, only when all matched zones have sort metadata.
- Partial sort metadata is treated as a parity gap instead of an assumed order.

## Validation

Ran:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldMapRegionZoneSortServiceTests|FullyQualifiedName~WorldMapRegionRuntimeSnapshotServiceTests|FullyQualifiedName~WorldMapRegionLifecyclePlanServiceTests|FullyQualifiedName~WorldMapRegionCreationSnapshotServiceTests|FullyQualifiedName~WorldMapRegionZoneFilterServiceTests|FullyQualifiedName~WorldMapRegionLayoutServiceTests|FullyQualifiedName~WorldRegionKeyProjectionServiceTests|FullyQualifiedName~WorldRegionIdServiceTests"
```

Result: passed 57 tests.

Full game-server suite was not rerun in this unit.

## Parallel Work Discovery

| Candidate | Files / Area | Risk | Selected | Notes |
| --- | --- | --- | --- | --- |
| Sorted-zone snapshot composition | creation/runtime snapshot services/tests | Low | Yes | Connects the zone sort helper to existing non-live snapshots. |
| Charge-all DB rollback integration planning | repository integration tests | Medium | No | Independent safe alternative. |
| Nearby packet golden gap audit | packet tests/docs | Low | No | Useful later. |
| Zone-handler source audit | Java zone handler files | Low | No | Useful before live callbacks. |

No sub-agent was spawned for UOW-1642 because the selected work touched shared snapshot contracts that should have one writer.

## Tests Added Or Updated

| Test | Change | Validates | Java Comparison |
| --- | --- | --- | --- |
| `CreateSnapshot_For2DRegion_ComposesBoundsNeighboursAndFilteredZones` | Updated | Creation snapshots expose filtered zone ids and Java constructor-ordered zone ids when sort metadata is complete. | Static source review of Java create-region flow and comparator. |
| `CreateSnapshot_For3DRegion_ComposesZBoundsAndFilteredZones` | Updated | Missing sort metadata is explicit for matched 3D zones. | Static source review; no Java runtime comparison. |
| `CreateSnapshot_WithPartialSortMetadata_DoesNotAssumeConstructorOrder` | Added | Partial sort metadata prevents constructor-order output and records missing zone ids. | Deterministic C# regression to avoid false parity assumptions. |
| `CreateSnapshot_ComposesCreationPrerequisitesWithLifecycleState` | Updated | Runtime snapshots carry filtered ids, constructor-ordered ids, lifecycle state, and missing live pieces. | Static source review of Java constructor/runtime fields. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.world.WorldMap2DInstance.createMapRegion` | `WorldMapRegionCreationSnapshotService.CreateSnapshot` | Region Creation Snapshot | Partial | Unit Tested | Partial Parity | C# now carries filtered zone ids plus Java constructor-ordered zone ids when complete sort metadata is provided. It still does not construct live `MapRegion` objects or store live zones. |
| `com.aionemu.gameserver.world.WorldMap3DInstance.createMapRegion` | `WorldMapRegionCreationSnapshotService.CreateSnapshot` | Region Creation Snapshot | Partial | Unit Tested | Partial Parity | 3D snapshots preserve missing-sort metadata when zone sort keys are absent. Java parallel creation and synchronized map writes remain unported. |
| `com.aionemu.gameserver.world.MapRegion.<init>` | `WorldMapRegionCreationSnapshot.ConstructorOrderedZoneIds`; `WorldMapRegionRuntimeSnapshot.ConstructorOrderedZoneIds` | Constructor Readiness DTO | Partial | Unit Tested | Partial Parity | C# exposes constructor-order ids based on UOW-1641 comparator metadata but does not instantiate `MapRegion` or sort a live `ZoneInstance[]`. |
| `com.aionemu.gameserver.world.MapRegion.zoneComparator` | `WorldMapRegionZoneSortService` consumed by snapshot services | Comparator Dependency | Partial | Unit Tested | Partial Parity | Snapshot composition uses the Java-style comparator only when all matched zones have sort metadata. Partial metadata blocks constructor-order output and records missing zone ids. |
| `com.aionemu.gameserver.world.MapRegion.getZoneCount` | `WorldMapRegionRuntimeSnapshot.ZoneIds`; `ConstructorOrderedZoneIds` | Zone Count/Boundary DTO | Partial | Unit Tested | Needs Verification | C# preserves both filtered count/order and constructor order metadata. Live `ZoneInstance[]` count and handler-backed zone semantics remain unverified. |
| `com.aionemu.gameserver.world.zone.ZoneInstance` | `WorldMapRegionZoneCandidate`; `WorldMapRegionZoneSortCandidate` | Zone Runtime Boundary DTO | Not Started | Unit Tested Metadata | Needs Verification | C# still splits geometry/filter data from sort metadata. Java object identity, `ZoneTemplate` references, handlers, creature membership, and callbacks remain unported. |
| `com.aionemu.gameserver.model.templates.zone.ZoneTemplate` | `WorldMapRegionZoneCandidate`; `WorldMapRegionZoneSortCandidate` | Template Boundary DTO | Partial | Unit Tested | Needs Verification | C# uses DTO projections for map id, area, type, priority, and name id. XML binding, defaults, flags, siege/town ids, and serialization remain unverified. |

## Remaining Risks

- Constructor-ordered zone ids are metadata only; no live `ZoneInstance[]` exists.
- Sort metadata must be supplied manually until real `ZoneTemplate` loading/projection is ported.
- Partial sort metadata blocks constructor order by design; future callers must decide whether to fail fast or continue non-live.
- Java `ZoneTemplate` XML binding defaults, flags, siege/town ids, serialization, and enum string formats remain unverified.
- Java `ZoneName` cache/missing logging/fallback behavior remains unported beyond hash generation.
- Live object storage, parent instance references, neighbour object references, synchronized/volatile state, scheduler behavior, AI notifications, zone revalidation, death callbacks, item-use zone checks, and handler callbacks remain disabled.
- Charge-all DB rollback remains a future gap: fake-repository tests prove runtime no-mutation/no-packet behavior, not MySQL transaction rollback.
- `docs/commit-conventions.md` is still missing.

## Summary Metrics

- Total Java artifacts discovered: 7 grouped rows.
- Total artifacts ported: 2 snapshot contract updates plus 1 focused test and 3 updated tests.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification: 3 grouped rows explicitly marked Needs Verification; remaining rows are Partial Parity or Not Started metadata with known gaps.
- Total blocked artifacts: live C# MapRegion storage, live `ZoneInstance[]`, real zone-template projection into sort metadata, zone-template XML loading/serialization, zone-name cache/logging, scheduler execution, synchronization/volatile runtime parity, AI notifications, zone revalidation, charge-all MySQL rollback regression.
- Estimated overall migration completion: about 72%.

## Next Recommended Unit Of Work

Next best unit:

| Candidate | Files / Area | Notes |
| --- | --- | --- |
| Non-live zone query/revalidation plan | new service/tests | Model `MapRegion.revalidateZones`, `findZones`, and `isInsideZone` using constructor-ordered zone metadata before live handlers. |
| Charge-all DB rollback integration regression | gated DB integration tests | Prove actual MySQL rollback/no-DB-mutation on partial charge-all save failure. |
| Nearby packet golden gap audit | packet tests/docs | Keep read-only unless adding isolated packet tests. |

## Next Work Options

## Recommended Sequential Task

- Task: add a non-live region zone query/revalidation plan model.
- Why: Java zone scans depend on the sorted constructor array, especially priority-zone behavior in `revalidateZones`.
- Files: likely a new zone query/revalidation service/tests plus docs.
- Java source to read: `MapRegion.revalidateZones`, `MapRegion.findZones`, `MapRegion.isInsideZone`, `MapRegion.isInsideItemUseZone`.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
| --- | --- | --- | --- | --- |
| A | Charge-all DB rollback integration test planning | repository integration tests/fixtures | Medium | Independent from region lifecycle files. |
| B | Nearby packet golden gap audit | packet tests/docs read-only unless assigned | Low | Avoid live send work. |
| C | Zone handler source audit | read-only Java zone handler files | Low | Useful before live zone callbacks. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
| --- | --- | --- | --- |
| Orchestrator | Non-live zone query/revalidation plan | new zone query service/tests, docs | Java writes, live nearby dispatch |
| Read-only Agent | Zone handler source audit | Java source reads only | all writes |

## Do Not Parallelize

- Java source files: read-only only.
- Live nearby dispatch and live `MapRegion` storage: still high risk and intentionally disabled.
- Shared creation/runtime snapshot contract changes: one owner only if implementation begins.

## Continuation Context

Current unit commit message:

```text
[Phase 6][UOW-1642] Carry sorted zone ids in snapshots
```

Files changed in this unit:

- `dotnetConversion/src/Aion.GameServer/Services/WorldMapRegionCreationSnapshotService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/WorldMapRegionRuntimeSnapshotService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldMapRegionCreationSnapshotServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldMapRegionRuntimeSnapshotServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ARJ-Completion.md`

Required startup reading for the next continuation:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parallelization-strategy.md`
- `docs/parity-verification.md`
- `docs/PHASE-6-PROGRESS.md`
- Latest completion handoff, currently this file

Note: `docs/commit-conventions.md` is still missing.
