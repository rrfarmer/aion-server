# Phase 6ARC Completion - Region Layout Precreation

Date: 2026-05-28
Unit of Work: UOW-1635
Status: Complete after focused unit tests

## Scope

This unit added a non-live region layout model for Java `WorldMap2DInstance.initMapRegions` and `WorldMap3DInstance.initMapRegions`. It records deterministic region ids and neighbour ids only; it does not create live `MapRegion` objects or attach players/objects.

Live object storage, zone filtering, region activation/deactivation, `WorldPosition.mapRegion`, and nearby dispatch remain disabled.

## Completed Work

- Added `WorldMapRegionLayoutService`.
- Added `WorldMapRegionLayout`.
- Modeled Java 2D inclusive x/y precreation loops.
- Modeled Java 3D inclusive x/y and exclusive rounded-z precreation loops.
- Modeled Java 2D and 3D neighbour-id scans.
- Preserved Java's 3D neighbour loop condition `z2 < z + regionSize`.
- Added focused tests for 2D layout, 3D layout, 3D neighbour Z-loop behavior, and Java-rounded maxZ.
- Updated `docs/PHASE-6-PROGRESS.md` with discovery, parity table, tests, risks, metrics, and next-unit guidance.

## Java Parity Notes

- Java 2D regions are created with `for (x = 0; x <= size; x += regionSize)` and `for (y = 0; y <= size; y += regionSize)`.
- Java 3D regions use the same inclusive x/y loops, but z is `for (z = 0; z < maxZ; z += regionSize)`.
- Java computes `maxZ` as `Math.round((float) size / regionSize) * regionSize`.
- Java 3D neighbour scans use `z2 < z + regionSize`, not `z2 <= z + regionSize`.
- C# preserves these loop shapes in a deterministic non-live DTO. Java's `parallelStream`, synchronized live map writes, and `MapRegion` object graph remain unported.

## Validation

Ran:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldMapRegionLayoutServiceTests|FullyQualifiedName~WorldRegionKeyProjectionServiceTests|FullyQualifiedName~WorldRegionIdServiceTests"
```

Result: passed 27 tests.

Full game-server suite was not rerun in this unit.

## Parallel Work Discovery

| Candidate | Files / Area | Risk | Selected | Notes |
| --- | --- | --- | --- | --- |
| Region precreation/neighbour-id model | new layout service/tests | Low | Yes | Deterministic non-live prerequisite before live region storage. |
| Charge-all DB rollback integration planning | repository integration tests | Medium | No | Independent safe alternative. |
| Nearby packet golden gap audit | packet tests/docs | Low | No | Useful later. |
| Live region storage | production world services | High | No | Deferred until layout and snapshot adapters are stable. |

No sub-agent was spawned for UOW-1635 because the selected implementation and tests touched one new helper surface and docs remained orchestrator-owned.

## Tests Added Or Updated

| Test | Change | Validates | Java Comparison |
| --- | --- | --- | --- |
| `CreateLayout_For2DMap_PrecreatesInclusiveXYRegionsAndNeighbours` | Added | 2D region ids and neighbour ids for a 256-size map match inclusive x/y loops. | Static source review of Java `WorldMap2DInstance.initMapRegions`; no Java runtime comparison. |
| `CreateLayout_For3DMap_PrecreatesInclusiveXYAndExclusiveRoundedZRegions` | Added | 3D region ids use inclusive x/y, exclusive rounded z, and omit z at maxZ. | Static source review of Java `WorldMap3DInstance.initMapRegions`; no Java runtime comparison. |
| `CreateLayout_For3DMap_MatchesJavaNeighbourZLoopShape` | Added | 3D neighbour ids preserve Java's `z2 < z + regionSize` shape. | Static source review of Java neighbour loops. |
| `CreateLayout_UsesJavaRoundedMaxZForNonDivisibleWorldSize` | Added | Non-divisible world sizes use Java-style positive rounding before z-loop creation. | Static source review of Java `Math.round((float) size / regionSize) * regionSize`. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.world.WorldMap2DInstance.initMapRegions` | `WorldMapRegionLayoutService.CreateLayout(..., TwoDimensional)` | Region Layout / Utility | Partial | Unit Tested | Partial Parity | C# precomputes the same 2D region ids and neighbour ids for deterministic world sizes using inclusive x/y loops. It does not create `MapRegion` objects, call `filterZones`, store regions in a live map, or maintain player/object membership. |
| `com.aionemu.gameserver.world.WorldMap3DInstance.initMapRegions` | `WorldMapRegionLayoutService.CreateLayout(..., ThreeDimensional)` | Region Layout / Utility | Partial | Unit Tested | Partial Parity | C# precomputes 3D region ids using inclusive x/y, Java-rounded maxZ, exclusive z loop, and Java's neighbour z-loop condition. Java `parallelStream` creation, synchronized region map writes, live regions, and zone filtering remain unported. |
| `com.aionemu.gameserver.world.RegionUtil` | `WorldRegionIdService` consumed by `WorldMapRegionLayoutService` | Utility Dependency | Complete | Unit Tested | Partial Parity | Existing Java-equivalent region formulas are reused for layout ids and neighbour ids. Region-size config binding, negative-coordinate edge cases, and Java runtime comparison remain unverified. |
| `com.aionemu.gameserver.world.MapRegion` | `WorldMapRegionLayout` region/neighbour id DTO | Region Storage / Boundary DTO | Partial | Unit Tested | Needs Verification | DTO captures ids and neighbour-id relationships only. Live object maps, activation/deactivation, synchronized player counts, zone revalidation, parent instance references, and `addNeighbourRegion(MapRegion)` object links remain unported. |
| `com.aionemu.gameserver.world.WorldMapInstance` | `WorldMapRegionLayout` as non-live prerequisite | Abstract Runtime Instance | Partial | Unit Tested | Needs Verification | Layout captures region initialization prerequisites but not instance handlers, `regions` map lifecycle, `forEachObject`, add/remove object behavior, or registration/player counts. |

## Remaining Risks

- Region layout is still non-live and does not create or attach `MapRegion` objects.
- Java `filterZones`, zone bounds, activation/deactivation, player/object membership, and synchronized live region map writes remain unported.
- Java `parallelStream` behavior is intentionally reduced to deterministic id ordering; this is a model/test convenience, not live threading parity.
- The 3D neighbour z-loop shape is preserved from Java source, but broader gameplay implications need verification before live storage.
- Region-size config override is not wired into runtime config.
- Charge-all DB rollback remains a future gap: fake-repository tests prove runtime no-mutation/no-packet behavior, not MySQL transaction rollback.
- `docs/commit-conventions.md` is still missing.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped rows.
- Total artifacts ported: 1 non-live layout helper plus 4 focused tests.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification: 2 grouped rows explicitly marked Needs Verification; remaining rows are Partial Parity with known gaps.
- Total blocked artifacts: live C# MapRegion storage, zone filtering/revalidation, object membership, `parallelStream` runtime parity, config-bound region size, Java runtime comparison, charge-all MySQL rollback regression.
- Estimated overall migration completion: about 72%.

## Next Recommended Unit Of Work

Next best unit:

| Candidate | Files / Area | Notes |
| --- | --- | --- |
| Layout-backed position resolver | layout/projection service and tests | Compose selector-driven projection with precreated layout ids so positions can resolve to a Java-style existing region plus neighbour ids. |
| Charge-all DB rollback integration regression | gated DB integration tests | Prove actual MySQL rollback/no-DB-mutation on partial charge-all save failure. |
| Nearby packet golden gap audit | packet tests/docs | Keep read-only unless adding isolated packet tests. |

## Next Work Options

## Recommended Sequential Task

- Task: compose `WorldMapRegionLayoutService` with selector-driven projection.
- Why: region ids can now be derived and precreated separately; next step is resolving a `WorldPosition` against a finite Java-style layout before live storage.
- Files: likely `WorldMapRegionLayoutService.cs`, projection/layout tests, docs.
- Java source to read: `WorldMap2DInstance.getRegion`, `WorldMap3DInstance.getRegion`, `WorldMap2DInstance.initMapRegions`, `WorldMap3DInstance.initMapRegions`.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
| --- | --- | --- | --- | --- |
| A | Charge-all DB rollback integration test planning | repository integration tests/fixtures | Medium | Independent from region layout files. |
| B | Nearby packet golden gap audit | packet tests/docs read-only unless assigned | Low | Avoid live send work. |
| C | Zone filtering source audit | read-only Java `WorldMapInstance.filterZones` and zone files | Low | Useful before live region objects. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
| --- | --- | --- | --- |
| Orchestrator | Layout-backed position resolver | layout/projection service/tests, docs | Java writes, live nearby dispatch |
| Read-only Agent | Charge-all DB rollback test planning | read-only repository integration tests/fixtures | all writes |

## Do Not Parallelize

- Java source files: read-only only.
- Live nearby dispatch: still high risk and intentionally disabled.
- Shared region layout/projection helper changes: one owner only if implementation begins.

## Continuation Context

Current unit commit message:

```text
[Phase 6][UOW-1635] Model Java region layout ids
```

Files changed in this unit:

- `dotnetConversion/src/Aion.GameServer/Services/WorldMapRegionLayoutService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldMapRegionLayoutServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ARC-Completion.md`

Required startup reading for the next continuation:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parallelization-strategy.md`
- `docs/parity-verification.md`
- `docs/PHASE-6-PROGRESS.md`
- Latest completion handoff, currently this file

Note: `docs/commit-conventions.md` is still missing.
