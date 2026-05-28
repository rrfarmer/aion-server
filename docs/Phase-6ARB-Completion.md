# Phase 6ARB Completion - Map Dimension Projection Selection

Date: 2026-05-28
Unit of Work: UOW-1634
Status: Complete after focused unit tests

## Scope

This unit added a non-live selector that mirrors Java's map-instance dimension choice before deriving nearby and known-list region keys. Java still remains the source of truth: `WorldMapInstanceFactory.createWorldMapInstance` creates `WorldMap3DInstance` only for `WorldMapType.RESHANTA` and `WorldMap2DInstance` for every other map.

Live map-region storage, region precreation, neighbour arrays, zone filtering, instance handler construction, and nearby dispatch remain disabled.

## Completed Work

- Added `WorldMapRegionDimension`.
- Added `WorldRegionKeyProjectionService.ReshantaWorldId`.
- Added `WorldRegionKeyProjectionService.GetJavaRegionDimension`.
- Added default `CreateNearbyRegionKey(WorldPosition)` and `CreateKnownListRegionKey(WorldPosition)` overloads that select 2D or 3D from the Java rule.
- Added explicit dimension-selector overloads for nearby and known-list projection.
- Added tests covering the Reshanta-only 3D rule, default nearby projection, and explicit known-list selector projection.
- Updated `docs/PHASE-6-PROGRESS.md` with discovery, parity table, tests, risks, metrics, and next-unit guidance.

## Java Parity Notes

- Java `WorldMapInstanceFactory.createWorldMapInstance` checks `parent.getMapId() == WorldMapType.RESHANTA.getId()`.
- Java `WorldMapType.RESHANTA` is `400010000`.
- Java creates a live `WorldMap3DInstance` only for Reshanta and `WorldMap2DInstance` otherwise.
- C# now mirrors that rule only for key projection. It does not construct Java-equivalent live map instances or handlers.

## Validation

Ran:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldRegionKeyProjectionServiceTests|FullyQualifiedName~WorldRegionIdServiceTests|FullyQualifiedName~NearbyQuestRegionSnapshotServiceTests|FullyQualifiedName~PlayerKnownListRegionSnapshotServiceTests"
```

Result: passed 29 tests.

Full game-server suite was not rerun in this unit.

## Parallel Work Discovery

| Candidate | Files / Area | Risk | Selected | Notes |
| --- | --- | --- | --- | --- |
| Map-dimension selection model | `WorldRegionKeyProjectionService.cs`, projection tests | Low | Yes | Removes manual 2D/3D caller choice for Java-equivalent defaults. |
| Charge-all DB rollback integration planning | repository integration tests | Medium | No | Independent safe alternative. |
| Nearby packet golden gap audit | packet tests/docs | Low | No | Useful later. |
| Region precreation analysis | Java world-region files and future C# model | Low | No | Good next step before live storage. |

No sub-agent was spawned for UOW-1634 because the selected implementation and tests touched the same small projection helper and docs remained orchestrator-owned.

## Tests Added Or Updated

| Test | Change | Validates | Java Comparison |
| --- | --- | --- | --- |
| `GetJavaRegionDimension_SelectsThreeDimensionalOnlyForReshanta` | Added | Reshanta selects 3D and a representative non-Reshanta map selects 2D. | Static source review of Java `WorldMapInstanceFactory` and `WorldMapType.RESHANTA`; no Java runtime comparison. |
| `CreateNearbyRegionKey_UsesJavaWorldMapInstanceFactoryDimension` | Added | Default nearby projection uses 3D for Reshanta and 2D for non-Reshanta. | Static source review of Java factory selection plus 2D/3D `getRegion` methods. |
| `CreateKnownListRegionKey_UsesExplicitDimensionSelector` | Added | Known-list projection can flow through one explicit 2D/3D selector. | Static source review of Java shared `RegionUtil` math. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.world.WorldMapInstanceFactory` | `WorldRegionKeyProjectionService.GetJavaRegionDimension`; default `CreateNearbyRegionKey`; default `CreateKnownListRegionKey` | Factory Selection / Utility | Partial | Unit Tested | Partial Parity | C# mirrors the Java 3D-vs-2D selection rule for region-key projection: Reshanta uses 3D, all other maps use 2D. It does not create live `WorldMapInstance` objects, invoke handlers, increment `WorldMap.nextInstanceId`, or add instances to the map. |
| `com.aionemu.gameserver.world.WorldMapType.RESHANTA` | `WorldRegionKeyProjectionService.ReshantaWorldId` | Enum Constant / Identifier | Partial | Unit Tested | Needs Verification | Constant `400010000` was verified by Java source review and covered by tests. Broader `WorldMapType` enum, personal flags, Panesterra helpers, and map-name lookup remain unported at this boundary. |
| `com.aionemu.gameserver.world.WorldMap2DInstance` | `WorldMapRegionDimension.TwoDimensional`; selector-driven 2D projection overloads | Region Lookup Adapter | Partial | Unit Tested | Partial Parity | Default selector maps non-Reshanta worlds to 2D ids and tests confirm Z is ignored through the default path. Live region precreation, neighbour arrays, owner/personal instance state, and zone filtering remain unported. |
| `com.aionemu.gameserver.world.WorldMap3DInstance` | `WorldMapRegionDimension.ThreeDimensional`; selector-driven 3D projection overloads | Region Lookup Adapter | Partial | Unit Tested | Partial Parity | Default selector maps Reshanta to 3D ids and tests confirm Z is included through the default path. Live 3D region precreation, `parallelStream` creation, neighbour arrays, and zone filtering remain unported. |
| `com.aionemu.gameserver.world.RegionUtil` | `WorldRegionIdService` consumed through selector-driven `WorldRegionKeyProjectionService` | Utility Dependency | Complete | Unit Tested | Partial Parity | Existing Java-equivalent formulas are now reached through Java's map-dimension rule. Region-size config binding, negative-coordinate edge cases, and Java runtime comparison remain unverified. |
| `com.aionemu.gameserver.world.WorldPosition` | `Aion.GameServer.World.WorldPosition` consumed by default selector overloads | Position Model | Partial | Unit Tested | Needs Verification | Position can derive default Java-style nearby and known-list keys from world id, instance id, and coordinates. Java mutable `mapRegion`, spawned flag, and live parent instance lookup remain unported. |

## Remaining Risks

- Selector-driven projection is still non-live and does not attach a `MapRegion` to `WorldPosition`.
- Only the Reshanta-only 3D rule is modeled; the broader Java `WorldMapType` enum and personal-map metadata are not ported here.
- Java map region precreation, neighbour linking, zone filtering, activation/deactivation, and object membership remain unported.
- Region-size config override is not wired into runtime config.
- Charge-all DB rollback remains a future gap: fake-repository tests prove runtime no-mutation/no-packet behavior, not MySQL transaction rollback.
- `docs/commit-conventions.md` is still missing.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped rows.
- Total artifacts ported: 1 selector enum/constant path plus 3 focused tests.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification: 2 grouped rows explicitly marked Needs Verification; remaining rows are Partial Parity with known gaps.
- Total blocked artifacts: live C# MapRegion storage, region precreation/neighbour model, zone filtering/revalidation, full `WorldMapType`, config-bound region size, Java runtime comparison, charge-all MySQL rollback regression.
- Estimated overall migration completion: about 72%.

## Next Recommended Unit Of Work

Next best unit:

| Candidate | Files / Area | Notes |
| --- | --- | --- |
| Region precreation/neighbour-id model | new small region model/service and tests | Model Java `WorldMap2DInstance.initMapRegions` and `WorldMap3DInstance.initMapRegions` deterministically before live storage. |
| Charge-all DB rollback integration regression | gated DB integration tests | Prove actual MySQL rollback/no-DB-mutation on partial charge-all save failure. |
| Nearby packet golden gap audit | packet tests/docs | Keep read-only unless adding isolated packet tests. |

## Next Work Options

## Recommended Sequential Task

- Task: add a non-live region precreation/neighbour-id model.
- Why: projection now chooses 2D/3D correctly, but Java precreates finite region ids and neighbour links before live lookup.
- Files: likely a new `WorldMapRegionLayoutService.cs` plus focused tests.
- Java source to read: `WorldMap2DInstance.initMapRegions`, `WorldMap3DInstance.initMapRegions`, `RegionUtil`.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
| --- | --- | --- | --- | --- |
| A | Charge-all DB rollback integration test planning | repository integration tests/fixtures | Medium | Independent from nearby files. |
| B | Nearby packet golden gap audit | packet tests/docs read-only unless assigned | Low | Avoid live send work. |
| C | Region precreation source audit | read-only Java `WorldMap2DInstance`/`WorldMap3DInstance` and C# world files | Low | Useful before implementing layout model. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
| --- | --- | --- | --- |
| Orchestrator | Region precreation/neighbour-id model | new region layout service/tests, docs | Java writes, live nearby dispatch |
| Read-only Agent | Charge-all DB rollback test planning | read-only repository integration tests/fixtures | all writes |

## Do Not Parallelize

- Java source files: read-only only.
- Live nearby dispatch: still high risk and intentionally disabled.
- Shared region projection helper changes: one owner only if implementation begins.

## Continuation Context

Current unit commit message:

```text
[Phase 6][UOW-1634] Select Java map region dimension
```

Files changed in this unit:

- `dotnetConversion/src/Aion.GameServer/Services/WorldRegionKeyProjectionService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldRegionKeyProjectionServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ARB-Completion.md`

Required startup reading for the next continuation:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parallelization-strategy.md`
- `docs/parity-verification.md`
- `docs/PHASE-6-PROGRESS.md`
- Latest completion handoff, currently this file

Note: `docs/commit-conventions.md` is still missing.
