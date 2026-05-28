# Phase 6ARE Completion - Region Zone Filtering Prerequisite

Date: 2026-05-28
Unit of Work: UOW-1637
Status: Complete after focused unit tests

## Scope

This unit modeled Java `WorldMapInstance.filterZones` as a non-live prerequisite for future `MapRegion` creation. It creates region bounds from existing layout metadata and filters supplied zone candidates by Java-style area intersection rules.

This remains a DTO/model layer. It does not instantiate `ZoneInstance`, create live `MapRegion` objects, attach handlers, revalidate creatures, or dispatch nearby updates.

## Completed Work

- Added `WorldMapRegionZoneFilterService`.
- Added `WorldMapRegionZoneFilterResult`, `WorldMapRegionZoneCandidate`, `WorldMapRegionBounds`, and zone-area DTOs.
- Added Java-style 2D and 3D region-bound creation from `WorldMapRegionLayout`.
- Modeled polygon, cylinder, sphere, semisphere, rectangle, and dummy-zone filter behavior.
- Preserved Java `RectangleArea.intersectsRectangle` TODO stub behavior as no intersection.
- Modeled Java dummy-zone log branch as explicit `DummyZonesMissingWholeMapIntersection` metadata.
- Updated `docs/PHASE-6-PROGRESS.md` with discovery, parity table, tests, risks, metrics, and next-unit guidance.

## Java Parity Notes

- Java `WorldMapInstance.filterZones` creates `RegionZone(startX, startY, minZ, maxZ)`.
- Java keeps zones whose `zoneInstance.getAreaTemplate().intersectsRectangle(regionZone)` returns true.
- Java logs an error for dummy zones that do not intersect, but still excludes them.
- Java `RectangleArea.intersectsRectangle` currently returns `false`; C# preserves this instead of improving it.
- Java `CylinderArea`, `SphereArea`, and `SemisphereArea` use region rectangle distance checks; C# models those checks deterministically.
- Java polygon intersection delegates to `Polygon2D.intersects`; C# uses an explicit polygon/rectangle intersection helper and marks broader parity as needing verification.

## Validation

Ran:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldMapRegionZoneFilterServiceTests|FullyQualifiedName~WorldMapRegionLayoutServiceTests|FullyQualifiedName~WorldRegionKeyProjectionServiceTests|FullyQualifiedName~WorldRegionIdServiceTests"
```

Result: passed 35 tests.

Full game-server suite was not rerun in this unit.

## Parallel Work Discovery

| Candidate | Files / Area | Risk | Selected | Notes |
| --- | --- | --- | --- | --- |
| Zone-filtering prerequisite model | new zone filter service/tests | Medium | Yes | Shared geometry helper and tests are tightly coupled. |
| Charge-all DB rollback integration planning | repository integration tests | Medium | No | Independent safe alternative. |
| Nearby packet golden gap audit | packet tests/docs | Low | No | Useful later. |
| Live region storage | production world services | High | No | Deferred until region creation prerequisites are stable. |

No sub-agent was spawned for UOW-1637 because the selected implementation and tests touched one new helper surface and docs remained orchestrator-owned.

## Tests Added Or Updated

| Test | Change | Validates | Java Comparison |
| --- | --- | --- | --- |
| `CreateRegionBounds_For2DLayout_UsesJava2DCreateMapRegionZRange` | Added | 2D region zone bounds use decoded x/y, minZ `0`, and layout maxZ. | Static source review of Java `WorldMap2DInstance.createMapRegion`; no Java runtime comparison. |
| `CreateRegionBounds_For3DLayout_UsesJava3DCreateMapRegionZRange` | Added | 3D region zone bounds use decoded x/y/z and `z + regionSize`. | Static source review of Java `WorldMap3DInstance.createMapRegion`; no Java runtime comparison. |
| `FilterZones_KeepsPolygonCylinderAndSphereIntersectionsForMap` | Added | Matching-map polygon/cylinder/sphere candidates are retained and other-map/z-miss candidates are excluded. | Static source review of Java `WorldMapInstance.filterZones`, `PolyArea`, `CylinderArea`, and `SphereArea`. |
| `FilterZones_PreservesJavaRectangleAreaNoIntersectionStubAndDummyMissReport` | Added | Rectangle candidates do not intersect and dummy misses are reported. | Static source review of Java `RectangleArea.intersectsRectangle` and dummy log branch. |
| `FilterZones_PreservesJavaSemisphereIntersectionCondition` | Added | Semisphere candidate uses Java's source z-clause and distance rule. | Static source review of Java `SemisphereArea.intersectsRectangle`; no runtime comparison. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.world.WorldMapInstance.filterZones` | `WorldMapRegionZoneFilterService.FilterZones` | Region Zone Filter / Utility | Partial | Unit Tested | Partial Parity | C# models non-live filtering with Java-source breadcrumbs and dummy-miss reporting. It does not instantiate `ZoneInstance`, log through Java logger, attach handlers, or return live zone arrays. Collection ordering follows supplied candidate order; Java stream over `zones.values()` depends on map iteration order. |
| `com.aionemu.gameserver.world.zone.RegionZone` | `WorldMapRegionBounds` | Region Boundary DTO | Partial | Unit Tested | Partial Parity | C# captures region rectangle min/max XY and min/max Z. Java `RegionZone` uses `WorldConfig.WORLD_REGION_SIZE`; C# accepts layout region size. Runtime config binding remains unverified. |
| `com.aionemu.gameserver.world.WorldMap2DInstance.createMapRegion` | `WorldMapRegionZoneFilterService.CreateRegionBounds` on 2D layouts | Region Creation Prerequisite | Partial | Unit Tested | Partial Parity | Tests confirm 2D region bounds use decoded X/Y, minZ `0`, and Java-rounded map maxZ. Live `MapRegion` construction, zone array attachment, and parent instance storage remain unported. |
| `com.aionemu.gameserver.world.WorldMap3DInstance.createMapRegion` | `WorldMapRegionZoneFilterService.CreateRegionBounds` on 3D layouts | Region Creation Prerequisite | Partial | Unit Tested | Partial Parity | Tests confirm 3D bounds use decoded X/Y/Z and `startZ + regionSize`. Live `MapRegion` construction, zone array attachment, and parent instance storage remain unported. |
| `com.aionemu.gameserver.model.geometry.PolyArea` | `WorldMapPolygonZoneArea` filtered by `WorldMapRegionZoneFilterService` | Geometry Area | Partial | Unit Tested | Needs Verification | C# implements deterministic polygon/rectangle intersection suitable for focused tests. Java delegates to `Polygon2D.intersects`; edge/boundary semantics are not runtime-compared. |
| `com.aionemu.gameserver.model.geometry.CylinderArea` | `WorldMapCylinderZoneArea` filtered by `WorldMapRegionZoneFilterService` | Geometry Area | Partial | Unit Tested | Partial Parity | C# preserves Java z-overlap guard and rectangle distance `< radius` rule. Floating precision and edge cases are not runtime-compared. |
| `com.aionemu.gameserver.model.geometry.SphereArea` | `WorldMapSphereZoneArea` filtered by `WorldMapRegionZoneFilterService` | Geometry Area | Partial | Unit Tested | Partial Parity | C# preserves Java rectangle 3D distance `<= radius` rule. Floating precision and edge cases are not runtime-compared. |
| `com.aionemu.gameserver.model.geometry.SemisphereArea` | `WorldMapSemisphereZoneArea` filtered by `WorldMapRegionZoneFilterService` | Geometry Area | Partial | Unit Tested | Partial Parity | C# preserves the Java source condition including its `||` z-clause. Floating precision and broader gameplay implications remain unverified. |
| `com.aionemu.gameserver.model.geometry.RectangleArea` | `WorldMapRectangleZoneArea` filtered by `WorldMapRegionZoneFilterService` | Geometry Area | Partial | Unit Tested | Intentional Difference | C# intentionally mirrors Java's current `intersectsRectangle` TODO stub returning false. This may be surprising if future Java fixes the stub; documented as a parity-preserving behavior, not a geometry improvement. |
| `com.aionemu.gameserver.world.zone.ZoneInstance` | `WorldMapRegionZoneCandidate` | Zone DTO / Runtime Boundary | Partial | Unit Tested | Needs Verification | Candidate captures id, map id, class name, and area only. Creature membership, handlers, flags, canFly/canGlide option resolution, synchronized enter/leave, and callbacks remain unported. |

## Remaining Risks

- Zone filtering is still non-live and returns DTOs, not Java `ZoneInstance[]`.
- Java logger side effects for dummy misses are modeled as data, not emitted logs.
- Polygon/rectangle intersection is a C# deterministic implementation of Java intent, but not a runtime comparison against `Polygon2D.intersects`.
- Java `ZoneInstance` handlers, creature membership, flags, canFly/canGlide/canRide option resolution, and synchronized enter/leave callbacks remain unported.
- Region-size config override is not wired into runtime config.
- Charge-all DB rollback remains a future gap: fake-repository tests prove runtime no-mutation/no-packet behavior, not MySQL transaction rollback.
- `docs/commit-conventions.md` is still missing.

## Summary Metrics

- Total Java artifacts discovered: 10 grouped rows.
- Total artifacts ported: 1 non-live zone filter helper plus 5 focused tests.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification: 3 grouped rows explicitly marked Needs Verification; remaining rows are Partial Parity or Intentional Difference with known gaps.
- Total blocked artifacts: live C# MapRegion storage, live `ZoneInstance` arrays, zone handlers/callbacks, creature membership, option flag resolution, config-bound region size, Java runtime geometry comparison, charge-all MySQL rollback regression.
- Estimated overall migration completion: about 72%.

## Next Recommended Unit Of Work

Next best unit:

| Candidate | Files / Area | Notes |
| --- | --- | --- |
| Region creation snapshot composition | layout/resolver/filter helpers and tests | Compose region id, bounds, neighbours, and filtered zone ids for one Java-style region without live `MapRegion` objects. |
| Charge-all DB rollback integration regression | gated DB integration tests | Prove actual MySQL rollback/no-DB-mutation on partial charge-all save failure. |
| Nearby packet golden gap audit | packet tests/docs | Keep read-only unless adding isolated packet tests. |

## Next Work Options

## Recommended Sequential Task

- Task: compose the non-live layout, position resolver, and zone-filter helper into a region creation snapshot.
- Why: region ids, bounds, neighbours, and zone filtering now exist separately; the next prerequisite is the shape Java `createMapRegion` would need before live storage.
- Files: likely a new region creation snapshot helper/tests plus docs.
- Java source to read: `WorldMap2DInstance.createMapRegion`, `WorldMap3DInstance.createMapRegion`, `MapRegion` constructor.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
| --- | --- | --- | --- | --- |
| A | Charge-all DB rollback integration test planning | repository integration tests/fixtures | Medium | Independent from region creation files. |
| B | Nearby packet golden gap audit | packet tests/docs read-only unless assigned | Low | Avoid live send work. |
| C | Zone handler source audit | read-only Java zone handler files | Low | Useful before live zone callbacks. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
| --- | --- | --- | --- |
| Orchestrator | Region creation snapshot composition | new helper/tests, docs | Java writes, live nearby dispatch |
| Read-only Agent | Charge-all DB rollback test planning | read-only repository integration tests/fixtures | all writes |

## Do Not Parallelize

- Java source files: read-only only.
- Live nearby dispatch and live `MapRegion` storage: still high risk and intentionally disabled.
- Shared region layout/filter helper changes: one owner only if implementation begins.

## Continuation Context

Current unit commit message:

```text
[Phase 6][UOW-1637] Model Java region zone filtering
```

Files changed in this unit:

- `dotnetConversion/src/Aion.GameServer/Services/WorldMapRegionZoneFilterService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldMapRegionZoneFilterServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ARE-Completion.md`

Required startup reading for the next continuation:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parallelization-strategy.md`
- `docs/parity-verification.md`
- `docs/PHASE-6-PROGRESS.md`
- Latest completion handoff, currently this file

Note: `docs/commit-conventions.md` is still missing.
