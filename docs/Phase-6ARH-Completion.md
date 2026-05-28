# Phase 6ARH Completion - Region Runtime Snapshot Readiness

Date: 2026-05-28
Unit of Work: UOW-1640
Status: Complete after focused unit tests

## Scope

This unit composed region creation prerequisites with lifecycle state into a non-live runtime snapshot/readiness model. It records the data a future live `MapRegion` owner still needs before actual object storage is enabled.

This remains a DTO/model layer. It does not instantiate `MapRegion`, mutate objects, sort live `ZoneInstance[]`, notify AI, or attach regions to `WorldPosition`.

## Completed Work

- Added `WorldMapRegionRuntimeSnapshotService`.
- Added `WorldMapRegionRuntimeSnapshot`.
- Composed region creation snapshot data with lifecycle state.
- Added explicit missing-live-pieces metadata for future live `MapRegion` construction.
- Added tests for live-ready existing region snapshots.
- Added tests for non-precreated region readiness blocking.
- Added tests for pending deactivation metadata.
- Updated `docs/PHASE-6-PROGRESS.md` with discovery, parity table, tests, risks, metrics, and next-unit guidance.

## Java Parity Notes

- Java `MapRegion` constructor stores region id, parent, sorted zones, neighbours including self, object map, player count, active state, and deactivation flag.
- C# now composes available constructor/lifecycle metadata into one snapshot.
- Missing live pieces are explicit: parent instance reference, object map, sorted zone array, neighbour object references, and scheduler/AI notification behavior.
- Missing precreated region ids block live-readiness metadata, matching Java's `regions.get(regionId)` null possibility.

## Validation

First focused run failed because a test used exact collection membership for a substring check. The assertion was corrected.

Re-ran:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldMapRegionRuntimeSnapshotServiceTests|FullyQualifiedName~WorldMapRegionLifecyclePlanServiceTests|FullyQualifiedName~WorldMapRegionCreationSnapshotServiceTests|FullyQualifiedName~WorldMapRegionZoneFilterServiceTests|FullyQualifiedName~WorldMapRegionLayoutServiceTests|FullyQualifiedName~WorldRegionKeyProjectionServiceTests|FullyQualifiedName~WorldRegionIdServiceTests"
```

Result: passed 50 tests.

Full game-server suite was not rerun in this unit.

## Parallel Work Discovery

| Candidate | Files / Area | Risk | Selected | Notes |
| --- | --- | --- | --- | --- |
| Region runtime snapshot/readiness model | new runtime snapshot service/tests | Low | Yes | Composes existing helper outputs without live storage. |
| Charge-all DB rollback integration planning | repository integration tests | Medium | No | Independent safe alternative. |
| Nearby packet golden gap audit | packet tests/docs | Low | No | Useful later. |
| Zone-handler source audit | Java zone handler files | Low | No | Useful before live callbacks. |

No sub-agent was spawned for UOW-1640 because the selected implementation and tests touched one small helper surface and docs remained orchestrator-owned.

## Tests Added Or Updated

| Test | Change | Validates | Java Comparison |
| --- | --- | --- | --- |
| `CreateSnapshot_ComposesCreationPrerequisitesWithLifecycleState` | Added | Runtime snapshot carries creation metadata, lifecycle state, neighbours, zone ids, and missing live pieces. | Static source review of Java `MapRegion` constructor fields plus `isActive`/player count state; no Java runtime comparison. |
| `CreateSnapshot_BlocksLiveReadinessWhenRegionWasNotPrecreated` | Added | Missing precreated ids block live-readiness metadata. | Static source review of Java `regions.get(regionId)` null behavior. |
| `CreateSnapshot_CarriesPendingDeactivationMetadataForFutureSchedulerBoundary` | Added | Pending deactivation state is preserved for future scheduler boundary modeling. | Static source review of Java `deactivationPending` field. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.world.MapRegion` | `WorldMapRegionRuntimeSnapshot` | Region Runtime Boundary DTO | Partial | Unit Tested | Needs Verification | C# now composes constructor prerequisites plus active/player/pending lifecycle metadata. It still lacks live parent instance reference, live `ConcurrentHashMap` object storage, `ZoneInstance[]` sorting by type/priority, `MapRegion[] neighboursIncludingSelf`, synchronized/volatile state, AI notifications, and zone callbacks. |
| `com.aionemu.gameserver.world.MapRegion.<init>` | `WorldMapRegionRuntimeSnapshotService.CreateSnapshot` | Constructor Readiness Snapshot | Partial | Unit Tested | Partial Parity | Snapshot captures region id, bounds, zone ids, neighbours, and missing live pieces for future construction. It does not sort real `ZoneInstance[]` or instantiate a `MapRegion`. |
| `com.aionemu.gameserver.world.MapRegion.getObjects` | `WorldMapRegionRuntimeSnapshot.MissingLivePieces` | Object Storage Boundary | Not Started | Unit Tested Metadata | Needs Verification | C# explicitly records missing `ConcurrentHashMap<Integer, VisibleObject> objects`; no live map or object membership exists. |
| `com.aionemu.gameserver.world.MapRegion.getNeighbours` | `WorldMapRegionRuntimeSnapshot.NeighbourRegionIds` | Neighbour Boundary DTO | Partial | Unit Tested | Partial Parity | C# carries neighbour ids from layout snapshots, not live `MapRegion` object references including self. Object identity/order and `ArrayUtils.add` behavior remain unported. |
| `com.aionemu.gameserver.world.MapRegion.isActive` | `WorldMapRegionRuntimeSnapshot.IsActive` | Lifecycle State DTO | Partial | Unit Tested | Partial Parity | Active state is captured as supplied metadata and can compose with lifecycle plans. Java synchronized active-state access and live mutations remain unported. |
| `com.aionemu.gameserver.world.MapRegion.getZoneCount` | `WorldMapRegionRuntimeSnapshot.ZoneIds` | Zone Count/Boundary DTO | Partial | Unit Tested | Needs Verification | Snapshot exposes filtered zone ids, but not sorted `ZoneInstance[]`; type/priority/name ordering and handler-backed zone semantics remain unported. |

## Remaining Risks

- Runtime snapshot is non-live and does not instantiate Java-equivalent `MapRegion`.
- Live object storage, parent instance references, neighbour object references, zone array sorting, and synchronized/volatile state remain unported.
- AI activation/deactivation notifications, zone revalidation, death callbacks, item-use zone checks, and handler callbacks remain disabled.
- Region-size config override is not wired into runtime config.
- Charge-all DB rollback remains a future gap: fake-repository tests prove runtime no-mutation/no-packet behavior, not MySQL transaction rollback.
- `docs/commit-conventions.md` is still missing.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped rows.
- Total artifacts ported: 1 non-live runtime snapshot helper plus 3 focused tests.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification: 3 grouped rows explicitly marked Needs Verification; remaining rows are Partial Parity or Not Started metadata with known gaps.
- Total blocked artifacts: live C# MapRegion storage, object map mutation, parent references, neighbour object references, zone sorting, scheduler execution, synchronization/volatile runtime parity, AI notifications, zone revalidation, charge-all MySQL rollback regression.
- Estimated overall migration completion: about 72%.

## Next Recommended Unit Of Work

Next best unit:

| Candidate | Files / Area | Notes |
| --- | --- | --- |
| Zone sorting model | new zone sort helper/tests | Model Java `MapRegion` constructor sort by zone type, priority, and zone name id. |
| Charge-all DB rollback integration regression | gated DB integration tests | Prove actual MySQL rollback/no-DB-mutation on partial charge-all save failure. |
| Nearby packet golden gap audit | packet tests/docs | Keep read-only unless adding isolated packet tests. |

## Next Work Options

## Recommended Sequential Task

- Task: add a non-live zone sorting model.
- Why: runtime snapshots carry zone ids, but Java sorts `ZoneInstance[]` before all zone checks.
- Files: likely a new zone sort helper/tests plus docs.
- Java source to read: `MapRegion.zoneComparator`, `ZoneClassName`, `ZoneTemplate.getPriority`, `ZoneName.id`.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
| --- | --- | --- | --- | --- |
| A | Charge-all DB rollback integration test planning | repository integration tests/fixtures | Medium | Independent from region lifecycle files. |
| B | Nearby packet golden gap audit | packet tests/docs read-only unless assigned | Low | Avoid live send work. |
| C | Zone handler source audit | read-only Java zone handler files | Low | Useful before live zone callbacks. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
| --- | --- | --- | --- |
| Orchestrator | Zone sorting model | new helper/tests, docs | Java writes, live nearby dispatch |
| Read-only Agent | Charge-all DB rollback test planning | read-only repository integration tests/fixtures | all writes |

## Do Not Parallelize

- Java source files: read-only only.
- Live nearby dispatch and live `MapRegion` storage: still high risk and intentionally disabled.
- Shared runtime snapshot/lifecycle helper changes: one owner only if implementation begins.

## Continuation Context

Current unit commit message:

```text
[Phase 6][UOW-1640] Compose region runtime snapshots
```

Files changed in this unit:

- `dotnetConversion/src/Aion.GameServer/Services/WorldMapRegionRuntimeSnapshotService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldMapRegionRuntimeSnapshotServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ARH-Completion.md`

Required startup reading for the next continuation:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parallelization-strategy.md`
- `docs/parity-verification.md`
- `docs/PHASE-6-PROGRESS.md`
- Latest completion handoff, currently this file

Note: `docs/commit-conventions.md` is still missing.
