# Phase 6ARI Completion - Region Zone Sort Readiness

Date: 2026-05-28
Unit of Work: UOW-1641
Status: Complete after focused unit tests

## Scope

This unit modeled Java `MapRegion` constructor zone ordering as a non-live DTO helper. Java sorts region zones by `ZoneClassName`, `ZoneTemplate.priority`, and `ZoneName.id()` before every zone scan.

This remains a model/helper layer. It does not instantiate `MapRegion`, mutate a live `ZoneInstance[]`, invoke zone handlers, or compose sorted zone ids into runtime snapshots yet.

## Completed Work

- Added `WorldMapRegionZoneSortService`.
- Added `WorldMapRegionZoneSortCandidate`.
- Added `WorldMapRegionZoneSortClassName` preserving Java `ZoneClassName` declaration order.
- Added Java `String.hashCode` support for `ZoneName.id()` using uppercase zone names.
- Added tests for Java comparator key order, equal-key stable ordering, and known Java hash values.
- Updated `docs/PHASE-6-PROGRESS.md` with discovery, parity table, tests, risks, metrics, and next-unit guidance.

## Java Parity Notes

- Java `MapRegion.zoneComparator` orders by `ZoneTemplate.getZoneType()`, then priority, then `ZoneTemplate.getName().id()`.
- Java enum comparison uses declaration order, so the C# enum explicitly assigns the same ordinal order from `DUMMY` through `DOMINION`.
- Java `ZoneName.createOrGet` uppercases names before storage; the helper uppercases before computing Java `String.hashCode`.
- Equal-key ordering is preserved explicitly with original input index. This models Java object-array sort stability but has not been runtime-compared against Java.

## Validation

First focused run failed because a test expected a larger positive Java hash to sort before a smaller one. The assertion was corrected to signed Java `int` ordering.

Re-ran:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldMapRegionZoneSortServiceTests|FullyQualifiedName~WorldMapRegionRuntimeSnapshotServiceTests|FullyQualifiedName~WorldMapRegionLifecyclePlanServiceTests|FullyQualifiedName~WorldMapRegionCreationSnapshotServiceTests|FullyQualifiedName~WorldMapRegionZoneFilterServiceTests|FullyQualifiedName~WorldMapRegionLayoutServiceTests|FullyQualifiedName~WorldRegionKeyProjectionServiceTests|FullyQualifiedName~WorldRegionIdServiceTests"
```

Result: passed 56 tests.

Full game-server suite was not rerun in this unit.

## Parallel Work Discovery

| Candidate | Files / Area | Risk | Selected | Notes |
| --- | --- | --- | --- | --- |
| Zone sorting model | new zone sort service/tests | Low | Yes | Models Java constructor comparator without live storage. |
| Charge-all DB rollback integration planning | repository integration tests | Medium | No | Independent safe alternative. |
| Nearby packet golden gap audit | packet tests/docs | Low | No | Useful later. |
| Zone-handler source audit | Java zone handler files | Low | No | Useful before live callbacks. |

No sub-agent was spawned for UOW-1641 because the selected implementation and tests touched one small helper surface and docs remained orchestrator-owned.

## Tests Added Or Updated

| Test | Change | Validates | Java Comparison |
| --- | --- | --- | --- |
| `SortByJavaMapRegionOrder_OrdersByZoneClassThenPriorityThenZoneNameId` | Added | Java comparator key ordering by type, priority, and signed Java zone-name id. | Static source review plus deterministic Java hash expected values; no Java runtime comparison. |
| `SortByJavaMapRegionOrder_PreservesInputOrderForEquivalentComparatorKeys` | Added | Stable order for equal comparator keys. | Static source review of Java object-array sort behavior; no Java runtime comparison. |
| `GetJavaZoneNameId_UsesUppercaseJavaStringHashCode` | Added | Uppercase Java hash generation for `ZoneName.id()`. | Deterministic expected values for representative names. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.world.MapRegion.zoneComparator` | `WorldMapRegionZoneSortService.SortByJavaMapRegionOrder` | Comparator / Utility | Partial | Unit Tested | Partial Parity | C# orders DTO candidates by Java zone type declaration order, priority, and signed Java zone-name id. It does not sort live `ZoneInstance[]`, mutate constructor input, or execute downstream zone checks yet. |
| `com.aionemu.gameserver.world.MapRegion.<init>` | `WorldMapRegionZoneSortService.SortByJavaMapRegionOrder` | Constructor Dependency | Partial | Unit Tested | Needs Verification | The constructor's sort rule is modeled, but C# still has no live `MapRegion` constructor, parent reference, neighbour object references, or object map. |
| `com.aionemu.gameserver.model.templates.zone.ZoneClassName` | `WorldMapRegionZoneSortClassName` | Enum | Partial | Unit Tested | Partial Parity | C# enum values preserve Java declaration order from `DUMMY` through `DOMINION`. Serialization format and broader zone-template binding remain unverified. |
| `com.aionemu.gameserver.model.templates.zone.ZoneTemplate.getPriority` | `WorldMapRegionZoneSortCandidate.Priority` | DTO Field | Partial | Unit Tested | Partial Parity | Priority participates in ascending ordering after zone type. XML defaults and template loading are not ported at this boundary. |
| `com.aionemu.gameserver.model.templates.zone.ZoneTemplate.getName` | `WorldMapRegionZoneSortCandidate.ZoneNameId` | DTO Field | Partial | Unit Tested | Needs Verification | C# accepts a precomputed zone-name id or uses helper hash generation; it does not carry Java `ZoneName` object identity. |
| `com.aionemu.gameserver.world.zone.ZoneName.id` | `WorldMapRegionZoneSortService.GetJavaZoneNameId` | Utility | Partial | Unit Tested | Partial Parity | C# computes uppercase Java `String.hashCode` values with signed `int` overflow. It does not model the concurrent `zoneNames` cache, missing-zone logging, or `NONE` fallback lookup behavior. |
| `java.util.Arrays.sort(Object[], Comparator)` | `WorldMapRegionZoneSortService.SortByJavaMapRegionOrder` | Collection Ordering Dependency | Partial | Unit Tested | Needs Verification | Equal comparator keys preserve input order explicitly. Java object-array sort stability is modeled, but no Java runtime comparison has been run. |
| `com.aionemu.gameserver.world.zone.ZoneInstance` | `WorldMapRegionZoneSortCandidate` | Zone Runtime Boundary DTO | Not Started | Unit Tested Metadata | Needs Verification | DTO captures only sort keys and zone id. Live handlers, area checks, creature membership, `ZoneTemplate` references, and synchronized enter/leave behavior remain unported. |

## Remaining Risks

- Zone sorting is non-live and operates on DTO candidates, not Java-equivalent `ZoneInstance[]`.
- Runtime snapshots still expose unsorted zone ids unless a future unit composes sort metadata into creation/runtime snapshots.
- Java `ZoneTemplate` XML binding defaults, serialization, and enum string formats remain unverified.
- Java `ZoneName` concurrent cache, missing-zone logging, and fallback lookup behavior remain unported.
- Live object storage, parent instance references, neighbour object references, synchronized/volatile state, scheduler behavior, AI notifications, zone revalidation, death callbacks, item-use zone checks, and handler callbacks remain disabled.
- Charge-all DB rollback remains a future gap: fake-repository tests prove runtime no-mutation/no-packet behavior, not MySQL transaction rollback.
- `docs/commit-conventions.md` is still missing.

## Summary Metrics

- Total Java artifacts discovered: 8 grouped rows.
- Total artifacts ported: 1 non-live zone sort helper plus 3 focused tests.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification: 4 grouped rows explicitly marked Needs Verification; remaining rows are Partial Parity or Not Started metadata with known gaps.
- Total blocked artifacts: live C# MapRegion storage, live `ZoneInstance[]`, sorted zone composition into runtime snapshots, zone-template XML loading/serialization, zone-name cache/logging, scheduler execution, synchronization/volatile runtime parity, AI notifications, zone revalidation, charge-all MySQL rollback regression.
- Estimated overall migration completion: about 72%.

## Next Recommended Unit Of Work

Next best unit:

| Candidate | Files / Area | Notes |
| --- | --- | --- |
| Compose zone sort metadata into snapshots | creation/runtime snapshot services/tests | Expose Java-ordered zone ids while retaining filtered input order before live `ZoneInstance[]` storage. |
| Charge-all DB rollback integration regression | gated DB integration tests | Prove actual MySQL rollback/no-DB-mutation on partial charge-all save failure. |
| Nearby packet golden gap audit | packet tests/docs | Keep read-only unless adding isolated packet tests. |

## Next Work Options

## Recommended Sequential Task

- Task: compose `WorldMapRegionZoneSortService` into the non-live creation/runtime snapshot path.
- Why: Java `MapRegion` stores a sorted zone array and later zone scans depend on that order.
- Files: likely `WorldMapRegionCreationSnapshotService.cs`, `WorldMapRegionRuntimeSnapshotService.cs`, related tests, plus docs.
- Java source to read: `MapRegion.zoneComparator`, `MapRegion.revalidateZones`, `MapRegion.isInsideZone`, `MapRegion.isInZone`.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
| --- | --- | --- | --- | --- |
| A | Charge-all DB rollback integration test planning | repository integration tests/fixtures | Medium | Independent from region lifecycle files. |
| B | Nearby packet golden gap audit | packet tests/docs read-only unless assigned | Low | Avoid live send work. |
| C | Zone handler source audit | read-only Java zone handler files | Low | Useful before live zone callbacks. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
| --- | --- | --- | --- |
| Orchestrator | Compose zone sort metadata into snapshots | creation/runtime snapshot services/tests, docs | Java writes, live nearby dispatch |
| Read-only Agent | Zone handler source audit | Java source reads only | all writes |

## Do Not Parallelize

- Java source files: read-only only.
- Live nearby dispatch and live `MapRegion` storage: still high risk and intentionally disabled.
- Shared creation/runtime snapshot helper changes: one owner only if implementation begins.

## Continuation Context

Current unit commit message:

```text
[Phase 6][UOW-1641] Model region zone sort order
```

Files changed in this unit:

- `dotnetConversion/src/Aion.GameServer/Services/WorldMapRegionZoneSortService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldMapRegionZoneSortServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ARI-Completion.md`

Required startup reading for the next continuation:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parallelization-strategy.md`
- `docs/parity-verification.md`
- `docs/PHASE-6-PROGRESS.md`
- Latest completion handoff, currently this file

Note: `docs/commit-conventions.md` is still missing.
