# Phase 6ARR Completion - Material Zone Geometry Metadata

Date: 2026-05-28
Unit of Work: UOW-1650
Status: Complete after focused unit tests

## Scope

This unit extended the non-live material-zone construction plan with Java `MaterialZoneTemplate` numeric geometry metadata for cylinder, sphere, and semisphere material zones.

This remains a DTO/model layer. It does not read live `Spatial`, cast `BoundingBox`, instantiate Java template shape objects, or serialize XML zone data.

## Completed Work

- Extended `WorldMapRegionMaterialZoneConstructionContext` with bounding-box center and extents.
- Added `WorldMapRegionMaterialZoneGeometry`.
- Added geometry metadata to material-zone construction plans.
- Modeled cylinder radius/top/bottom math from Java.
- Modeled sphere and semisphere radius math from Java.
- Added focused tests for cylinder and sphere/semisphere geometry.
- Updated `docs/PHASE-6-PROGRESS.md` with discovery, file ownership, parity table, tests, risks, metrics, and next-unit guidance.

## Java Parity Notes

- Java cylinder-like names contain `CYLINDER`, `CONE`, or `H_COLUME`.
- Cylinder radius is `sqrt(xExtent^2 + yExtent^2) + 1`.
- Cylinder top is `center.z + zExtent + 1`; bottom is `center.z - zExtent - 1`.
- Sphere and semisphere radius is `sqrt(xExtent^2 + yExtent^2 + zExtent^2) + 1`.
- C# receives center/extents as metadata and does not call `Spatial.getWorldBound()`.

## Validation

Ran:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldMapRegionMaterialZoneConstructionServiceTests|FullyQualifiedName~WorldMapRegionZoneConstructionServiceTests|FullyQualifiedName~WorldMapRegionZoneIdentityServiceTests|FullyQualifiedName~WorldMapRegionZoneCapabilityServiceTests|FullyQualifiedName~WorldMapRegionZoneScanPlanServiceTests|FullyQualifiedName~WorldMapRegionZoneSortServiceTests|FullyQualifiedName~WorldMapRegionRuntimeSnapshotServiceTests|FullyQualifiedName~WorldMapRegionLifecyclePlanServiceTests|FullyQualifiedName~WorldMapRegionCreationSnapshotServiceTests|FullyQualifiedName~WorldMapRegionZoneFilterServiceTests|FullyQualifiedName~WorldMapRegionLayoutServiceTests|FullyQualifiedName~WorldRegionKeyProjectionServiceTests|FullyQualifiedName~WorldRegionIdServiceTests"
```

Result: passed 94 tests.

Full game-server suite was not rerun in this unit.

## Parallel Work Discovery

| Candidate | Files / Area | Risk | Selected | Notes |
| --- | --- | --- | --- | --- |
| Material-zone numeric geometry metadata | material construction service/tests | Medium | Yes | Extends shared material construction helper from UOW-1649. |
| Charge-all DB rollback integration planning | repository integration tests | Medium | No | Independent safe alternative. |
| Nearby packet golden gap audit | packet tests/docs | Low | No | Useful later. |
| Zone-handler source audit | Java zone handler files | Low | No | Useful before live callbacks. |

## File Ownership Map

| Agent | Scope | Allowed Files | Forbidden Files | Expected Output |
| --- | --- | --- | --- | --- |
| Orchestrator | Material-zone numeric geometry metadata, tests, docs, commit | `WorldMapRegionMaterialZoneConstructionService.cs`, `WorldMapRegionMaterialZoneConstructionServiceTests.cs`, progress/handoff docs | Java source writes, unrelated services/tests | Implemented and documented UOW-1650. |
| Sub-agents | None | None | All files | Not spawned because selected work edits shared material helper/test/docs. |

No sub-agent was spawned for UOW-1650 because selected work edits shared material construction helper/test/docs and needs one owner.

## Tests Added Or Updated

| Test | Change | Validates | Java Comparison |
| --- | --- | --- | --- |
| `CreatePlan_CylinderGeometryUsesJavaHorizontalExtentRadiusAndVerticalBounds` | Added | Cylinder center, horizontal-extent radius plus one, top, and bottom are modeled. | Static source review of Java `MaterialZoneTemplate` cylinder branch. |
| `CreatePlan_SphereAndSemisphereGeometryUseJavaCornerDistanceRadius` | Added | Sphere and semisphere center/radius metadata uses Java corner-distance plus one. | Static source review of Java `calculateDistanceFromCenterToCorner`. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.model.templates.zone.MaterialZoneTemplate` | `WorldMapRegionMaterialZoneGeometry` | Material Zone Geometry DTO | Partial | Unit Tested | Partial Parity | C# now models center/radius and cylinder top/bottom calculations from Java bounding-box extents. It still does not instantiate Java `Sphere`, `Cylinder`, or `Semisphere` template objects. |
| `com.aionemu.gameserver.geoEngine.bounding.BoundingBox` | `WorldMapRegionMaterialZoneConstructionContext` center/extents | Geometry Boundary DTO | Partial | Unit Tested | Needs Verification | C# receives center/extents as supplied metadata. Live `Spatial.getWorldBound()` and cast to `BoundingBox` remain unported. |
| `com.aionemu.gameserver.model.templates.zone.Cylinder` | `WorldMapRegionMaterialZoneGeometry` with `Cylinder` area kind | Material Area DTO | Partial | Unit Tested | Partial Parity | C# computes Java radius/top/bottom for cylinder-like material names. Serialization and live area construction remain unverified. |
| `com.aionemu.gameserver.model.templates.zone.Sphere` | `WorldMapRegionMaterialZoneGeometry` with `Sphere` area kind | Material Area DTO | Partial | Unit Tested | Partial Parity | C# computes Java corner-distance radius plus one for default material geometry. Serialization and live area construction remain unverified. |
| `com.aionemu.gameserver.model.templates.zone.Semisphere` | `WorldMapRegionMaterialZoneGeometry` with `Semisphere` area kind | Material Area DTO | Partial | Unit Tested | Partial Parity | C# computes Java corner-distance radius plus one for semisphere material geometry. Serialization and live area construction remain unverified. |

## Remaining Risks

- Numeric geometry metadata is non-live and depends on supplied center/extents.
- Live `Spatial`, `BoundingBox`, template object creation, XML serialization, material/world lookups, shield ignore/config checks, and collidable handler mutation remain unported.
- Floating-point behavior is covered by simple deterministic values only; broader precision/runtime comparison remains unverified.
- Live `MaterialZoneHandler` behavior, material zone persistence, dynamic zone handlers, and live C# `MapRegion`/`ZoneInstance` storage remain disabled.
- Charge-all DB rollback remains a future gap: fake-repository tests prove runtime no-mutation/no-packet behavior, not MySQL transaction rollback.
- `docs/commit-conventions.md` is still missing.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped rows.
- Total artifacts ported: 1 material-zone construction helper extension plus 2 focused tests.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification: 1 grouped row explicitly marked Needs Verification; remaining rows are Partial Parity with known gaps.
- Total blocked artifacts: live `Spatial`/`BoundingBox`, live material template/world lookups, XML serialization, collidable handler mutation, material skill/observer behavior, material zone persistence, dynamic zone handlers, live C# MapRegion/ZoneInstance storage, charge-all MySQL rollback regression.
- Estimated overall migration completion: about 72%.

## Next Recommended Unit Of Work

Next best unit:

| Candidate | Files / Area | Notes |
| --- | --- | --- |
| Non-live saveMaterialZones plan | new service/tests | Model `ZoneService.saveMaterialZones` filtering by collidable handlers, sorting by map id, and persistence-boundary metadata. |
| Charge-all DB rollback integration regression | gated DB integration tests | Prove actual MySQL rollback/no-DB-mutation on partial charge-all save failure. |
| Nearby packet golden gap audit | packet tests/docs | Keep read-only unless adding isolated packet tests. |

## Next Work Options

## Recommended Sequential Task

- Task: add a non-live `saveMaterialZones` plan for Java `ZoneService.saveMaterialZones`.
- Why: material construction now has branch and geometry metadata; persistence filtering/sorting is the next Java material-zone boundary.
- Files: likely new save-material-zone helper/tests plus docs.
- Java source to read: `ZoneService.saveMaterialZones`, `ZoneData.saveData`, `ZoneTemplate.getMapid`, `collidableHandlers`.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
| --- | --- | --- | --- | --- |
| A | Charge-all DB rollback integration test planning | repository integration tests/fixtures | Medium | Independent from region lifecycle files. |
| B | Nearby packet golden gap audit | packet tests/docs read-only unless assigned | Low | Avoid live send work. |
| C | Zone handler source audit | read-only Java zone handler files | Low | Useful before live zone callbacks. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
| --- | --- | --- | --- |
| Orchestrator | Non-live saveMaterialZones plan | new service/tests, docs | Java writes, live nearby dispatch |
| Read-only Agent | Zone handler source audit | Java source reads only | all writes |

## Do Not Parallelize

- Java source files: read-only only.
- Live nearby dispatch and live `MapRegion` storage: still high risk and intentionally disabled.
- Shared material construction/helper files: one owner only if implementation begins.

## Continuation Context

Current unit commit message:

```text
[Phase 6][UOW-1650] Add material zone geometry metadata
```

Files changed in this unit:

- `dotnetConversion/src/Aion.GameServer/Services/WorldMapRegionMaterialZoneConstructionService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldMapRegionMaterialZoneConstructionServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ARR-Completion.md`

Required startup reading for the next continuation:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parallelization-strategy.md`
- `docs/parity-verification.md`
- `docs/PHASE-6-PROGRESS.md`
- Latest completion handoff, currently this file

Note: `docs/commit-conventions.md` is still missing.
