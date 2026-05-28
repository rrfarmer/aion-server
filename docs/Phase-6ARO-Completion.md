# Phase 6ARO Completion - ZoneService Construction Plan

Date: 2026-05-28
Unit of Work: UOW-1647
Status: Complete after focused unit tests

## Scope

This unit modeled Java `ZoneService.getZoneInstancesByWorldId` as a non-live construction/type-selection plan. It covers the initial full-map zone, Java instance type selection, FORT/ARTIFACT side-effect metadata, and named invasion-zone selection through vortex availability.

This remains a DTO/model layer. It does not instantiate live `ZoneInstance` objects, return a live `Map<ZoneName, ZoneInstance>`, call `DataManager`, attach shield services, mutate siege/vortex data, or instantiate dynamic handlers.

## Completed Work

- Added `WorldMapRegionZoneConstructionService`.
- Added construction context, candidate, plan, entry, and instance-kind DTOs/enums.
- Modeled first full-map DUMMY base zone with handler attachment metadata.
- Modeled Java type selection for FLY, NO_FLY, FORT, ARTIFACT, PVP, named invasion zones, and default base zones.
- Modeled FORT siege/shield side-effect metadata.
- Modeled ARTIFACT add-zone and missing-artifact metadata.
- Modeled named invasion zones requiring vortex map metadata.
- Added six focused tests.
- Updated `docs/PHASE-6-PROGRESS.md` with discovery, file ownership, parity table, tests, risks, metrics, and next-unit guidance.

## Java Parity Notes

- Java creates a full-map `WorldZoneTemplate` and `ZoneInstance` first.
- Java then iterates map zone infos and chooses subclasses based on `ZoneClassName`.
- FORT and ARTIFACT use `SiegeZoneInstance` with siege/artifact side effects.
- PVP uses `PvPZoneInstance`.
- FLY and NO_FLY use their specialized subclasses.
- Named Brusthonin/Theobomos invasion zones use `InvasionZoneInstance` only when a matching vortex location exists; otherwise they fall back to base `ZoneInstance`.
- Every constructed instance receives a new zone handler from `getNewZoneHandler`.

## Validation

First focused run failed because test call sites used collection expressions for `IReadOnlySet<int>` parameters. The tests were corrected to pass `HashSet<int>`.

Re-ran:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldMapRegionZoneConstructionServiceTests|FullyQualifiedName~WorldMapRegionZoneIdentityServiceTests|FullyQualifiedName~WorldMapRegionZoneCapabilityServiceTests|FullyQualifiedName~WorldMapRegionZoneScanPlanServiceTests|FullyQualifiedName~WorldMapRegionZoneSortServiceTests|FullyQualifiedName~WorldMapRegionRuntimeSnapshotServiceTests|FullyQualifiedName~WorldMapRegionLifecyclePlanServiceTests|FullyQualifiedName~WorldMapRegionCreationSnapshotServiceTests|FullyQualifiedName~WorldMapRegionZoneFilterServiceTests|FullyQualifiedName~WorldMapRegionLayoutServiceTests|FullyQualifiedName~WorldRegionKeyProjectionServiceTests|FullyQualifiedName~WorldRegionIdServiceTests"
```

Result: passed 79 tests.

Full game-server suite was not rerun in this unit.

## Parallel Work Discovery

| Candidate | Files / Area | Risk | Selected | Notes |
| --- | --- | --- | --- | --- |
| ZoneService construction plan | new construction service/tests | Medium | Yes | Models construction/type selection without live storage. |
| Charge-all DB rollback integration planning | repository integration tests | Medium | No | Independent safe alternative. |
| Nearby packet golden gap audit | packet tests/docs | Low | No | Useful later. |
| Zone-handler source audit | Java zone handler files | Low | No | Useful before live callbacks. |

## File Ownership Map

| Agent | Scope | Allowed Files | Forbidden Files | Expected Output |
| --- | --- | --- | --- | --- |
| Orchestrator | ZoneService construction plan helper, tests, docs, commit | `WorldMapRegionZoneConstructionService.cs`, `WorldMapRegionZoneConstructionServiceTests.cs`, progress/handoff docs | Java source writes, unrelated services/tests | Implemented and documented UOW-1647. |
| Sub-agents | None | None | All files | Not spawned because implementation and docs were small and Orchestrator-owned. |

No sub-agent was spawned for UOW-1647 because the selected helper and tests were small and progress/handoff docs remained Orchestrator-owned.

## Tests Added Or Updated

| Test | Change | Validates | Java Comparison |
| --- | --- | --- | --- |
| `CreatePlan_AlwaysCreatesFullMapDummyZoneFirst` | Added | Full-map DUMMY base zone is first and handler metadata is attached. | Static source review of Java `WorldZoneTemplate` and full-map `ZoneInstance` creation. |
| `CreatePlan_SelectsJavaZoneInstanceKindsByZoneType` | Added | FLY, NO_FLY, PVP, and default type selection. | Static source review of Java `switch (zoneType)`. |
| `CreatePlan_FortZoneUsesSiegeInstanceAndShieldSideEffectsWhenSiegeExists` | Added | FORT selects siege kind and records siege/shield side effects when location exists. | Static source review of Java FORT branch. |
| `CreatePlan_ArtifactZoneUsesSiegeInstanceAndReportsMissingArtifacts` | Added | ARTIFACT selects siege kind, records found artifact add-zone and missing artifact metadata. | Static source review of Java ARTIFACT branch. |
| `CreatePlan_InvasionZoneNameUsesVortexBackedInvasionInstance` | Added | Known invasion names with a vortex map select invasion kind and record vortex side effect. | Static source review of Java `getIZI` / `validateZone`. |
| `CreatePlan_InvasionNameWithoutVortexFallsBackToBaseZone` | Added | Known invasion name without vortex falls back to base zone with metadata. | Static source review of Java `validateZone` null branch. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.world.zone.ZoneService.getZoneInstancesByWorldId` | `WorldMapRegionZoneConstructionService.CreatePlan` | Zone Construction Plan | Partial | Unit Tested | Partial Parity | C# models full-map zone creation, per-zone instance type selection, handler attachment metadata, and major side-effect metadata. It does not instantiate live `ZoneInstance` objects or return a `Map<ZoneName, ZoneInstance>`. |
| `com.aionemu.gameserver.model.templates.zone.WorldZoneTemplate` | first `WorldMapRegionZoneConstructionEntry` | Full-Map Zone Template Boundary | Partial | Unit Tested | Needs Verification | C# records a DUMMY full-map base zone by map id. It does not model points, maxZ rounding, world-size geometry, flags, or `ZoneName.createOrGet`. |
| `com.aionemu.gameserver.world.zone.ZoneService.getIZI` | `WorldMapRegionZoneConstructionService` invasion-name matching | Invasion Zone Selection Utility | Partial | Unit Tested | Partial Parity | C# models the hard-coded Brusthonin/Theobomos invasion zone names and vortex requirement. It does not read live `DataManager.VORTEX_DATA`. |
| `com.aionemu.gameserver.world.zone.ZoneService.validateZone` | `WorldMapRegionZoneConstructionService` vortex side-effect metadata | Invasion Zone Validation Utility | Partial | Unit Tested | Partial Parity | C# models vortex-backed `InvasionZoneInstance` selection and `vortex.addZone` metadata. It does not create live instances or mutate vortex data. |
| `com.aionemu.gameserver.world.zone.FlyZoneInstance` | `WorldMapRegionZoneInstanceKind.Fly` | Zone Instance Type | Partial | Unit Tested | Needs Verification | C# records selected type only; live enter/leave flight side effects remain modeled elsewhere/non-live. |
| `com.aionemu.gameserver.world.zone.NoFlyZoneInstance` | `WorldMapRegionZoneInstanceKind.NoFly` | Zone Instance Type | Partial | Unit Tested | Needs Verification | C# records selected type only; live enter/leave flight side effects remain unported. |
| `com.aionemu.gameserver.world.zone.SiegeZoneInstance` | `WorldMapRegionZoneInstanceKind.Siege` | Zone Instance Type | Partial | Unit Tested | Needs Verification | C# records selected type for FORT/ARTIFACT and side-effect metadata. Siege/artifact object attachment and shield service behavior remain non-live. |
| `com.aionemu.gameserver.world.zone.PvPZoneInstance` | `WorldMapRegionZoneInstanceKind.Pvp` | Zone Instance Type | Partial | Unit Tested | Needs Verification | C# records selected type only; live PvP enter/leave/death behavior remains unported. |
| `com.aionemu.gameserver.world.zone.InvasionZoneInstance` | `WorldMapRegionZoneInstanceKind.Invasion` | Zone Instance Type | Partial | Unit Tested | Needs Verification | C# records selected type only when matching name and vortex map metadata are present. Live invasion registration remains unported. |
| `com.aionemu.gameserver.services.ShieldService.attachShield` | `WorldMapRegionZoneConstructionEntry.SideEffects` | Side-Effect Boundary | Not Started | Unit Tested Metadata | Needs Verification | C# records shield attachment metadata for FORT zones with known siege locations; no live shield service call occurs. |

## Remaining Risks

- Construction planning is non-live and records selected types/side effects as metadata.
- Full-map `WorldZoneTemplate` geometry, maxZ rounding, flags, XML names, and `ZoneName` cache behavior are not modeled in this unit.
- Live `DataManager` lookups, `ZoneInstance` subclasses, handler instantiation, siege/artifact attachment, shield service, vortex mutation, material zone creation, and dynamic handler loading remain unported.
- Java `HashMap` return semantics for duplicate `ZoneName` keys are not modeled beyond entry metadata.
- Live C# `MapRegion`/`ZoneInstance` storage and callbacks remain disabled.
- Charge-all DB rollback remains a future gap: fake-repository tests prove runtime no-mutation/no-packet behavior, not MySQL transaction rollback.
- `docs/commit-conventions.md` is still missing.

## Summary Metrics

- Total Java artifacts discovered: 10 grouped rows.
- Total artifacts ported: 1 non-live construction helper plus 6 focused tests.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification: 7 grouped rows explicitly marked Needs Verification; remaining rows are Partial Parity or Not Started metadata with known gaps.
- Total blocked artifacts: live `ZoneInstance` subclasses, full-map geometry/flags, `DataManager` lookups, live handler instantiation, siege/artifact attachment, `ShieldService`, vortex mutation, material zone creation, dynamic zone handlers, live C# MapRegion/ZoneInstance storage, charge-all MySQL rollback regression.
- Estimated overall migration completion: about 72%.

## Next Recommended Unit Of Work

Next best unit:

| Candidate | Files / Area | Notes |
| --- | --- | --- |
| Full-map/duplicate construction metadata | construction service/tests | Extend construction plan for `WorldZoneTemplate` bounds/flags metadata and duplicate `ZoneName` replacement semantics. |
| Charge-all DB rollback integration regression | gated DB integration tests | Prove actual MySQL rollback/no-DB-mutation on partial charge-all save failure. |
| Nearby packet golden gap audit | packet tests/docs | Keep read-only unless adding isolated packet tests. |

## Next Work Options

## Recommended Sequential Task

- Task: extend the non-live `ZoneService` construction plan for full-map bounds/flags metadata and duplicate `ZoneName` replacement semantics.
- Why: Java full-map construction and `HashMap.put` duplicate handling affect the shape of the eventual live zone map.
- Files: likely `WorldMapRegionZoneConstructionService.cs`, tests, and docs.
- Java source to read: `WorldZoneTemplate`, `ZoneService.getZoneInstancesByWorldId`, `ZoneName`, `ZoneTemplate.setXmlName`.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
| --- | --- | --- | --- | --- |
| A | Charge-all DB rollback integration test planning | repository integration tests/fixtures | Medium | Independent from region lifecycle files. |
| B | Nearby packet golden gap audit | packet tests/docs read-only unless assigned | Low | Avoid live send work. |
| C | Zone handler source audit | read-only Java zone handler files | Low | Useful before live zone callbacks. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
| --- | --- | --- | --- |
| Orchestrator | Full-map/duplicate construction metadata | construction service/tests, docs | Java writes, live nearby dispatch |
| Read-only Agent | Zone handler source audit | Java source reads only | all writes |

## Do Not Parallelize

- Java source files: read-only only.
- Live nearby dispatch and live `MapRegion` storage: still high risk and intentionally disabled.
- Shared zone construction helper changes: one owner only if implementation begins.

## Continuation Context

Current unit commit message:

```text
[Phase 6][UOW-1647] Model ZoneService construction plan
```

Files changed in this unit:

- `dotnetConversion/src/Aion.GameServer/Services/WorldMapRegionZoneConstructionService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldMapRegionZoneConstructionServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ARO-Completion.md`

Required startup reading for the next continuation:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parallelization-strategy.md`
- `docs/parity-verification.md`
- `docs/PHASE-6-PROGRESS.md`
- Latest completion handoff, currently this file

Note: `docs/commit-conventions.md` is still missing.
