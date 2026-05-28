# Phase 6ARD Completion - Layout Backed Position Resolution

Date: 2026-05-28
Unit of Work: UOW-1636
Status: Complete after focused unit tests

## Scope

This unit composed selector-driven projection with the non-live region layout model. A `WorldPosition` can now be resolved against a Java-style finite layout to get a projected region id, an existence flag, and neighbour ids.

This remains a DTO/model layer. It does not create live `MapRegion` objects, assign `WorldPosition.mapRegion`, store objects, filter zones, or dispatch nearby updates.

## Completed Work

- Added `WorldMapRegionLayoutService.CreateLayoutForWorld`.
- Added `WorldMapRegionLayoutService.ResolvePosition`.
- Added `WorldMapRegionLayoutResolution`.
- Tested selector-driven layout creation for Reshanta versus non-Reshanta maps.
- Tested 2D position resolution against a precreated region and neighbour list.
- Tested 3D missing-region resolution when a position projects to z at `maxZ`, which Java precreation omits.
- Updated `docs/PHASE-6-PROGRESS.md` with discovery, parity table, tests, risks, metrics, and next-unit guidance.

## Java Parity Notes

- Java `WorldMap2DInstance.getRegion` computes `RegionUtil.get2dRegionId(x, y)` and returns `regions.get(regionId)`.
- Java `WorldMap3DInstance.getRegion` computes `RegionUtil.get3dRegionId(x, y, z)` and returns `regions.get(regionId)`.
- Java can return `null` from `regions.get(regionId)` when the projected id was not precreated.
- C# now models that lookup shape through `RegionExists` and an empty neighbour list for missing ids.

## Validation

Ran:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldMapRegionLayoutServiceTests|FullyQualifiedName~WorldRegionKeyProjectionServiceTests|FullyQualifiedName~WorldRegionIdServiceTests"
```

Result: passed 30 tests.

Full game-server suite was not rerun in this unit.

## Parallel Work Discovery

| Candidate | Files / Area | Risk | Selected | Notes |
| --- | --- | --- | --- | --- |
| Layout-backed position resolver | layout/projection service and tests | Low | Yes | Composes projection and finite layout before live storage. |
| Charge-all DB rollback integration planning | repository integration tests | Medium | No | Independent safe alternative. |
| Nearby packet golden gap audit | packet tests/docs | Low | No | Useful later. |
| Zone-filtering source audit | Java zone filtering and future region model | Low | No | Next likely prerequisite before live region objects. |

No sub-agent was spawned for UOW-1636 because implementation and tests touched the same layout helper and docs remained orchestrator-owned.

## Tests Added Or Updated

| Test | Change | Validates | Java Comparison |
| --- | --- | --- | --- |
| `CreateLayoutForWorld_UsesJavaDimensionSelector` | Added | Reshanta layouts are 3D and representative non-Reshanta layouts are 2D. | Static source review of Java `WorldMapInstanceFactory` and `WorldMapType.RESHANTA`; no Java runtime comparison. |
| `ResolvePosition_For2DLayout_ReturnsExistingPrecreatedRegionAndNeighbours` | Added | 2D position resolution returns an existing region id and precomputed neighbours. | Static source review of Java `WorldMap2DInstance.getRegion` and 2D init loops. |
| `ResolvePosition_For3DLayout_ReturnsMissingWhenProjectedRegionWasNotPrecreated` | Added | 3D resolution returns missing for a z-at-maxZ id that layout precreation omitted. | Static source review of Java `WorldMap3DInstance.getRegion` and exclusive z loop. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.world.WorldMapInstanceFactory` | `WorldMapRegionLayoutService.CreateLayoutForWorld` | Factory Selection / Utility | Partial | Unit Tested | Partial Parity | Layout creation now uses the Java Reshanta-only 3D selector. It still does not create live instances, handlers, or mutate `WorldMap.instances`. |
| `com.aionemu.gameserver.world.WorldMap2DInstance.getRegion` | `WorldMapRegionLayoutService.ResolvePosition` on 2D layouts | Region Lookup Adapter | Partial | Unit Tested | Partial Parity | Resolver projects x/y through the 2D formula and confirms the region exists in the precreated layout. It returns DTO metadata rather than a live `MapRegion` object. |
| `com.aionemu.gameserver.world.WorldMap3DInstance.getRegion` | `WorldMapRegionLayoutService.ResolvePosition` on 3D layouts | Region Lookup Adapter | Partial | Unit Tested | Partial Parity | Resolver projects x/y/z through the 3D formula and returns missing when the id was not precreated, matching Java's `regions.get(regionId)` null possibility. Live object lookup and Java runtime comparison remain unverified. |
| `com.aionemu.gameserver.world.MapRegion` | `WorldMapRegionLayoutResolution` | Region Boundary DTO | Partial | Unit Tested | Needs Verification | Resolution exposes region id and neighbour ids only. Parent instance, zone array, object membership, activation/deactivation, and synchronized player counts remain unported. |
| `com.aionemu.gameserver.world.WorldPosition` | `Aion.GameServer.World.WorldPosition` consumed by `ResolvePosition` | Position Model | Partial | Unit Tested | Needs Verification | Position can be resolved against a precreated layout, including missing-region outcomes. Java mutable `mapRegion`, spawned flag, and live assignment lifecycle remain unported. |

## Remaining Risks

- Resolver is still non-live and returns DTO metadata, not a Java-equivalent `MapRegion`.
- Java `WorldPosition.mapRegion` assignment lifecycle, spawned flag, and live parent instance references remain unported.
- Java `filterZones`, zone bounds, activation/deactivation, player/object membership, and synchronized live region map writes remain unported.
- Region-size config override is not wired into runtime config.
- Charge-all DB rollback remains a future gap: fake-repository tests prove runtime no-mutation/no-packet behavior, not MySQL transaction rollback.
- `docs/commit-conventions.md` is still missing.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped rows.
- Total artifacts ported: 1 resolver path plus 3 focused tests.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification: 2 grouped rows explicitly marked Needs Verification; remaining rows are Partial Parity with known gaps.
- Total blocked artifacts: live C# MapRegion storage, zone filtering/revalidation, object membership, `WorldPosition.mapRegion` lifecycle, config-bound region size, Java runtime comparison, charge-all MySQL rollback regression.
- Estimated overall migration completion: about 72%.

## Next Recommended Unit Of Work

Next best unit:

| Candidate | Files / Area | Notes |
| --- | --- | --- |
| Zone-filtering prerequisite model | Java `WorldMapInstance.filterZones`, zone DTO/model tests | Audit and model zone-bound calculations used by Java region creation before live `MapRegion` objects. |
| Charge-all DB rollback integration regression | gated DB integration tests | Prove actual MySQL rollback/no-DB-mutation on partial charge-all save failure. |
| Nearby packet golden gap audit | packet tests/docs | Keep read-only unless adding isolated packet tests. |

## Next Work Options

## Recommended Sequential Task

- Task: audit and model Java `WorldMapInstance.filterZones`.
- Why: region ids and layout resolution are now modeled, but Java `createMapRegion` also filters zones by region bounds.
- Files: likely a new zone-filtering DTO/helper and focused tests; first read Java `WorldMapInstance`.
- Java source to read: `WorldMapInstance.filterZones`, `WorldMap2DInstance.createMapRegion`, `WorldMap3DInstance.createMapRegion`, zone template/service classes.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
| --- | --- | --- | --- | --- |
| A | Charge-all DB rollback integration test planning | repository integration tests/fixtures | Medium | Independent from region layout files. |
| B | Nearby packet golden gap audit | packet tests/docs read-only unless assigned | Low | Avoid live send work. |
| C | Zone filtering source audit | read-only Java `WorldMapInstance.filterZones` and zone files | Low | Useful before implementing zone model. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
| --- | --- | --- | --- |
| Orchestrator | Zone-filtering prerequisite model | new helper/tests, docs | Java writes, live nearby dispatch |
| Read-only Agent | Charge-all DB rollback test planning | read-only repository integration tests/fixtures | all writes |

## Do Not Parallelize

- Java source files: read-only only.
- Live nearby dispatch: still high risk and intentionally disabled.
- Shared region layout/projection helper changes: one owner only if implementation begins.

## Continuation Context

Current unit commit message:

```text
[Phase 6][UOW-1636] Resolve positions against region layouts
```

Files changed in this unit:

- `dotnetConversion/src/Aion.GameServer/Services/WorldMapRegionLayoutService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldMapRegionLayoutServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ARD-Completion.md`

Required startup reading for the next continuation:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parallelization-strategy.md`
- `docs/parity-verification.md`
- `docs/PHASE-6-PROGRESS.md`
- Latest completion handoff, currently this file

Note: `docs/commit-conventions.md` is still missing.
