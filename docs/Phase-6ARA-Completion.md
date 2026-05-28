# Phase 6ARA Completion - Region Key Projection

Date: 2026-05-28
Unit of Work: UOW-1633
Status: Complete after focused unit tests

## Scope

This unit connected the non-live Java region-id helper from UOW-1632 to existing nearby and known-list region-key DTOs. It adds explicit 2D and 3D projection from `WorldPosition` into `NearbyQuestRegionKey` and `PlayerKnownListRegionKey`.

Live map-region storage, map-type runtime selection, neighbour arrays, zone filtering, and nearby dispatch remain disabled.

## Completed Work

- Added `WorldRegionKeyProjectionService`.
- Added `WorldRegionKeyProjectionServiceTests`.
- Added 2D and 3D nearby region-key projection from `WorldPosition`.
- Added 2D and 3D known-list region-key projection from `WorldPosition`.
- Covered derived nearby keys feeding `NearbyQuestRegionSnapshotService`.
- Covered derived known-list keys feeding `PlayerKnownListRegionSnapshotService`.
- Updated `docs/PHASE-6-PROGRESS.md` with discovery, parity table, tests, risks, metrics, and next-unit guidance.

## Java Parity Notes

- Java `WorldMap2DInstance.getRegion` delegates to `RegionUtil.get2dRegionId(x, y)` and ignores Z.
- Java `WorldMap3DInstance.getRegion` delegates to `RegionUtil.get3dRegionId(x, y, z)` and includes Z.
- Java `WorldPosition` carries the actual mutable `MapRegion`; C# still derives only region-key DTOs from coordinate data.
- C# callers must still choose 2D or 3D projection explicitly. A future unit should model the Java map-instance dimension choice.

## Validation

Ran:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldRegionKeyProjectionServiceTests|FullyQualifiedName~WorldRegionIdServiceTests|FullyQualifiedName~NearbyQuestRegionSnapshotServiceTests|FullyQualifiedName~PlayerKnownListRegionSnapshotServiceTests"
```

Result: passed 26 tests.

Full game-server suite was not rerun in this unit.

## Parallel Work Discovery

| Candidate | Files / Area | Risk | Selected | Notes |
| --- | --- | --- | --- | --- |
| Region-key derivation adapter coverage | `WorldRegionKeyProjectionService.cs`, projection tests | Low | Yes | Connects region-id math to existing snapshot planners. |
| Charge-all DB rollback integration planning | repository integration tests | Medium | No | Independent safe alternative. |
| Nearby packet golden gap audit | packet tests/docs | Low | No | Useful later. |
| Live nearby dispatch | world/connection services | High | No | Still blocked by live region storage. |

No sub-agent was spawned for UOW-1633 because the selected implementation and tests are tightly coupled and docs remain orchestrator-owned.

## Tests Added Or Updated

| Test | Change | Validates | Java Comparison |
| --- | --- | --- | --- |
| `CreateNearby2DRegionKey_DerivesJava2DRegionIdFromWorldPosition` | Added | Nearby 2D key preserves world/instance and derives region id while ignoring Z. | Static source review of Java `WorldMap2DInstance.getRegion` and `RegionUtil.get2dRegionId`; no Java runtime comparison. |
| `CreateNearby3DRegionKey_DerivesJava3DRegionIdFromWorldPosition` | Added | Nearby 3D key preserves world/instance and derives region id including Z. | Static source review of Java `WorldMap3DInstance.getRegion` and `RegionUtil.get3dRegionId`; no Java runtime comparison. |
| `CreateKnownListRegionKeys_UseSameJavaRegionMathAsNearbyKeys` | Added | Known-list 2D and 3D keys use the same formulas as nearby keys. | Static source review of Java shared `RegionUtil` math. |
| `BuildSnapshot_UsesDerivedNearbyRegionKeysForSameInstanceFiltering` | Added | Derived nearby keys feed existing snapshot filtering for same-instance versus other-instance players. | Composition test grounded in Java delayed nearby region dependency. |
| `BuildSnapshot_UsesDerivedKnownListRegionKeysForNeighbourScan` | Added | Derived known-list keys feed existing neighbour scan ordering and owner exclusion. | Composition test grounded in Java known-list region scan shape. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.world.RegionUtil` | `WorldRegionIdService` consumed by `WorldRegionKeyProjectionService` | Utility Dependency | Complete | Unit Tested | Partial Parity | Java 2D/3D formulas now feed nearby and known-list key DTOs. Config override, negative coordinates, and Java runtime comparison remain unverified. |
| `com.aionemu.gameserver.world.WorldPosition` | `Aion.GameServer.World.WorldPosition` consumed by `WorldRegionKeyProjectionService` | Position Model | Partial | Unit Tested | Needs Verification | C# record position can derive world id, instance id, and 2D/3D region id. Java mutable `mapRegion` pointer, spawned flag, and live parent lookup remain unported. |
| `com.aionemu.gameserver.world.WorldMap2DInstance.getRegion` | `WorldRegionKeyProjectionService.CreateNearby2DRegionKey`; `CreateKnownList2DRegionKey` | Region Lookup Adapter | Partial | Unit Tested | Partial Parity | Tests confirm 2D key derivation ignores Z like Java 2D maps. Live region existence, precreated region bounds, neighbour arrays, owner/personal instance state, and zone filtering are not ported. |
| `com.aionemu.gameserver.world.WorldMap3DInstance.getRegion` | `WorldRegionKeyProjectionService.CreateNearby3DRegionKey`; `CreateKnownList3DRegionKey` | Region Lookup Adapter | Partial | Unit Tested | Partial Parity | Tests confirm 3D key derivation includes Z and composes with existing snapshot planners. Live region existence, precreated Z bounds, neighbour arrays, and `parallelStream` creation remain unported. |
| `com.aionemu.gameserver.world.MapRegion` | `NearbyQuestRegionKey`; `PlayerKnownListRegionKey` created by `WorldRegionKeyProjectionService` | Region Boundary DTO | Partial | Unit Tested | Needs Verification | Existing DTOs can now be derived from coordinates, but live object maps, activation/deactivation, synchronized player counts, zone revalidation, and parent instance storage remain snapshot-only or unported. |
| `com.aionemu.gameserver.world.knownlist.KnownList.findVisibleObjects` | `PlayerKnownListRegionSnapshotService` using derived `PlayerKnownListRegionKey` in tests | Known-list Region Scan | Partial | Unit Tested | Partial Parity | Test proves derived region ids can drive existing non-live neighbour scan. Java range/canSee/two-way aware-list mutation and live MapRegion neighbours remain unported. |

## Remaining Risks

- Region-key projection is non-live and does not attach a `MapRegion` to `WorldPosition`.
- C# does not yet know whether a map is 2D or 3D from live world template/runtime metadata at this boundary.
- Java map region precreation, neighbour linking, zone filtering, activation/deactivation, and object membership remain unported.
- Region-size config override is not wired into the helper path.
- Charge-all DB rollback remains a future gap: fake-repository tests prove runtime no-mutation/no-packet behavior, not MySQL transaction rollback.
- `docs/commit-conventions.md` is still missing.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped rows.
- Total artifacts ported: 1 projection helper plus 5 focused tests.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification: 3 grouped rows explicitly marked Needs Verification; remaining rows are Partial Parity with known gaps.
- Total blocked artifacts: live C# MapRegion storage, 2D/3D map-type runtime selection, region precreation/neighbour model, zone filtering/revalidation, config-bound region size, Java runtime comparison, charge-all MySQL rollback regression.
- Estimated overall migration completion: about 72%.

## Next Recommended Unit Of Work

Next best unit:

| Candidate | Files / Area | Notes |
| --- | --- | --- |
| Map-dimension selection model | region projection service/tests or a new small selector | Document and test explicit 2D/3D map dimension choice before live region storage. |
| Charge-all DB rollback integration regression | gated DB integration tests | Prove actual MySQL rollback/no-DB-mutation on partial charge-all save failure. |
| Nearby packet golden gap audit | packet tests/docs | Keep read-only unless adding isolated packet tests. |

## Next Work Options

## Recommended Sequential Task

- Task: add a non-live map-dimension selection model for region-key projection.
- Why: projection works, but callers must currently choose 2D or 3D manually.
- Files: likely `WorldRegionKeyProjectionService.cs` and projection tests, or a new small selector/helper.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
| --- | --- | --- | --- | --- |
| A | Charge-all DB rollback integration test planning | repository integration tests/fixtures | Medium | Independent from nearby files. |
| B | Nearby packet golden gap audit | packet tests/docs read-only unless assigned | Low | Avoid live send work. |
| C | Region precreation analysis | read-only Java `WorldMap2DInstance`/`WorldMap3DInstance` and C# world files | Low | Useful before live storage. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
| --- | --- | --- | --- |
| Orchestrator | Map-dimension selection model | region projection service/tests, docs | Java writes, live nearby dispatch |
| Read-only Agent | Charge-all DB rollback test planning | read-only repository integration tests/fixtures | all writes |

## Do Not Parallelize

- Java source files: read-only only.
- Live nearby dispatch: still high risk and intentionally disabled.
- Shared region projection helper changes: one owner only if implementation begins.

## Continuation Context

Current unit commit message:

```text
[Phase 6][UOW-1633] Derive region keys from positions
```

Files changed in this unit:

- `dotnetConversion/src/Aion.GameServer/Services/WorldRegionKeyProjectionService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldRegionKeyProjectionServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ARA-Completion.md`

Required startup reading for the next continuation:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parallelization-strategy.md`
- `docs/parity-verification.md`
- `docs/PHASE-6-PROGRESS.md`
- Latest completion handoff, currently this file

Note: `docs/commit-conventions.md` is still missing.
