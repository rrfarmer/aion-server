# Phase 6ARQ Completion - Material Zone Construction Plan

Date: 2026-05-28
Unit of Work: UOW-1649
Status: Complete after focused unit tests

## Scope

This unit modeled Java `ZoneService.createMaterialZoneTemplate` as a non-live material-zone construction plan. It covers early returns, shield/material handler selection, duplicate handler reuse, existing zone-info reuse, and material area type selection.

This remains a DTO/model layer. It does not mutate `collidableHandlers`, mutate `zoneByMapIdMap`, query `DataManager`, instantiate `Spatial`/`BoundingBox`, or execute `MaterialZoneHandler`.

## Completed Work

- Added `WorldMapRegionMaterialZoneConstructionService`.
- Added material-zone construction context, plan, handler-kind, area-kind, and status types.
- Modeled `ZoneName.NONE` early return.
- Modeled shield material id `11` and null/non-null `ShieldService.tryRegisterShield` outcomes.
- Modeled missing/present material template outcomes.
- Modeled duplicate collidable handler warning/reuse behavior.
- Modeled existing `ZoneInfo` reuse and area-list creation metadata.
- Modeled `MaterialZoneTemplate` area selection by geometry name.
- Added six focused tests.
- Updated `docs/PHASE-6-PROGRESS.md` with discovery, file ownership, parity table, tests, risks, metrics, and next-unit guidance.

## Java Parity Notes

- Java returns immediately for `ZoneName.NONE`.
- Java reuses existing collidable handlers and logs duplicate material mesh warnings.
- Shield material id `11` delegates to `ShieldService.tryRegisterShield`; null returns without mutation.
- Non-shield materials require `DataManager.MATERIAL_DATA.getTemplate`.
- Java creates an area list for a world if missing, then skips new `ZoneInfo` creation when the zone already exists.
- `MaterialZoneTemplate` chooses cylinder for names containing `CYLINDER`, `CONE`, or `H_COLUME`; semisphere for names containing `SEMISPHERE`; otherwise sphere.

## Validation

Ran:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldMapRegionMaterialZoneConstructionServiceTests|FullyQualifiedName~WorldMapRegionZoneConstructionServiceTests|FullyQualifiedName~WorldMapRegionZoneIdentityServiceTests|FullyQualifiedName~WorldMapRegionZoneCapabilityServiceTests|FullyQualifiedName~WorldMapRegionZoneScanPlanServiceTests|FullyQualifiedName~WorldMapRegionZoneSortServiceTests|FullyQualifiedName~WorldMapRegionRuntimeSnapshotServiceTests|FullyQualifiedName~WorldMapRegionLifecyclePlanServiceTests|FullyQualifiedName~WorldMapRegionCreationSnapshotServiceTests|FullyQualifiedName~WorldMapRegionZoneFilterServiceTests|FullyQualifiedName~WorldMapRegionLayoutServiceTests|FullyQualifiedName~WorldRegionKeyProjectionServiceTests|FullyQualifiedName~WorldRegionIdServiceTests"
```

Result: passed 91 tests.

Full game-server suite was not rerun in this unit.

## Parallel Work Discovery

| Candidate | Files / Area | Risk | Selected | Notes |
| --- | --- | --- | --- | --- |
| Material-zone construction plan | new material construction service/tests | Medium | Yes | Self-contained model over Java material-zone branches. |
| Charge-all DB rollback integration planning | repository integration tests | Medium | No | Independent safe alternative. |
| Nearby packet golden gap audit | packet tests/docs | Low | No | Useful later. |
| Zone-handler source audit | Java zone handler files | Low | No | Useful before live callbacks. |

## File Ownership Map

| Agent | Scope | Allowed Files | Forbidden Files | Expected Output |
| --- | --- | --- | --- | --- |
| Orchestrator | Material-zone construction plan helper, tests, docs, commit | `WorldMapRegionMaterialZoneConstructionService.cs`, `WorldMapRegionMaterialZoneConstructionServiceTests.cs`, progress/handoff docs | Java source writes, unrelated services/tests | Implemented and documented UOW-1649. |
| Sub-agents | None | None | All files | Not spawned because implementation and docs were small and Orchestrator-owned. |

No sub-agent was spawned for UOW-1649 because selected work was small and progress/handoff docs remained Orchestrator-owned.

## Tests Added Or Updated

| Test | Change | Validates | Java Comparison |
| --- | --- | --- | --- |
| `CreatePlan_NoneZoneNameReturnsWithoutMutation` | Added | NONE zone name blocks mutation. | Static source review of Java `ZoneService.createMaterialZoneTemplate`. |
| `CreatePlan_ShieldMaterialRequiresShieldHandler` | Added | Shield handler null blocks; non-null registers shield handler and cylinder area. | Static source review of Java shield material branch. |
| `CreatePlan_NonShieldMaterialRequiresMaterialTemplate` | Added | Missing material template blocks; present template creates material handler metadata. | Static source review of Java material template lookup branch. |
| `CreatePlan_SelectsJavaMaterialZoneAreaKind` | Added | Geometry-name based area type selection for cylinder/cone/H_COLUME, semisphere, and sphere. | Static source review of Java `MaterialZoneTemplate`. |
| `CreatePlan_DuplicateHandlerStillChecksZoneInfoAndWarns` | Added | Duplicate handler path records warning and can still create missing zone info. | Static source review of Java duplicate handler branch. |
| `CreatePlan_ExistingZoneInfoSkipsMaterialZoneInfoCreation` | Added | Existing zone info prevents new material zone info creation while preserving list creation metadata. | Static source review of Java existing `ZoneInfo` branch. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.world.zone.ZoneService.createMaterialZoneTemplate` | `WorldMapRegionMaterialZoneConstructionService.CreatePlan` | Material Zone Construction Plan | Partial | Unit Tested | Partial Parity | C# models Java early returns, handler selection/reuse, area-list metadata, existing-zone reuse, and area-kind selection. It does not mutate live `collidableHandlers` or `zoneByMapIdMap`. |
| `com.aionemu.gameserver.model.templates.zone.MaterialZoneTemplate` | `WorldMapRegionMaterialZoneAreaKind` | Material Zone Template Boundary | Partial | Unit Tested | Partial Parity | C# models Java geometry-name area selection: cylinder for `CYLINDER`/`CONE`/`H_COLUME`, semisphere for `SEMISPHERE`, otherwise sphere. It does not calculate radii, centers, extents, flags, or XML name. |
| `com.aionemu.gameserver.world.zone.handler.MaterialZoneHandler` | `WorldMapRegionMaterialZoneHandlerKind.Material` | Handler Boundary | Not Started | Unit Tested Metadata | Needs Verification | C# records material handler creation metadata only. Live material skill matching, observer registration, staff debug messages, and collision actor behavior remain unported. |
| `com.aionemu.gameserver.services.ShieldService.tryRegisterShield` | `WorldMapRegionMaterialZoneHandlerKind.Shield`; `BlockedMissingShieldHandler` | Shield Handler Boundary | Partial | Unit Tested | Needs Verification | C# models shield material id `11` and null/non-null registration outcomes via supplied metadata. Live geo shield enable/ignore checks and registered shield mutation remain unported. |
| `com.aionemu.gameserver.world.zone.ZoneName.NONE` | `IgnoredNoneZoneName` | Zone Name Guard | Partial | Unit Tested | Partial Parity | C# returns without mutation for zone name `NONE`. It does not use Java `ZoneName` identity/cache. |
| `com.aionemu.gameserver.dataholders.DataManager.MATERIAL_DATA` | `WorldMapRegionMaterialZoneConstructionContext.MaterialTemplateExists` | Data Manager Boundary DTO | Not Started | Unit Tested Metadata | Needs Verification | C# models missing/present material template as supplied metadata. Live data lookup remains unported. |

## Remaining Risks

- Material-zone planning is non-live and records selected branches/side effects as metadata.
- Live `Spatial`, `BoundingBox`, center/radius/extents math, `MaterialZoneTemplate` flags/XML names, `DataManager` material/world lookups, shield ignore/config checks, and collidable handler mutation remain unported.
- Live `MaterialZoneHandler` behavior is not executed: material skill matching, observer registration/removal, packet debug messages, and collision actor behavior remain future work.
- `saveMaterialZones` remains unported and material zone persistence is not modeled.
- Live C# `MapRegion`/`ZoneInstance` storage and callbacks remain disabled.
- Charge-all DB rollback remains a future gap: fake-repository tests prove runtime no-mutation/no-packet behavior, not MySQL transaction rollback.
- `docs/commit-conventions.md` is still missing.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped rows.
- Total artifacts ported: 1 non-live material-zone construction helper plus 6 focused tests.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification: 3 grouped rows explicitly marked Needs Verification; remaining rows are Partial Parity or Not Started metadata with known gaps.
- Total blocked artifacts: live `Spatial`/`BoundingBox` math, live material template/world lookups, `ShieldService` config/ignore handling, collidable handler mutation, material skill/observer behavior, material zone persistence, dynamic zone handlers, live C# MapRegion/ZoneInstance storage, charge-all MySQL rollback regression.
- Estimated overall migration completion: about 72%.

## Next Recommended Unit Of Work

Next best unit:

| Candidate | Files / Area | Notes |
| --- | --- | --- |
| Material-zone numeric geometry metadata | material construction service/tests | Model sphere/cylinder/semisphere center, radius, bottom/top from Java bounding-box extents. |
| Charge-all DB rollback integration regression | gated DB integration tests | Prove actual MySQL rollback/no-DB-mutation on partial charge-all save failure. |
| Nearby packet golden gap audit | packet tests/docs | Keep read-only unless adding isolated packet tests. |

## Next Work Options

## Recommended Sequential Task

- Task: extend material-zone construction with numeric geometry metadata.
- Why: Java `MaterialZoneTemplate` area shape math feeds future zone filtering/inside checks.
- Files: likely `WorldMapRegionMaterialZoneConstructionService.cs`, tests, and docs.
- Java source to read: `MaterialZoneTemplate`, `BoundingBox`, sphere/cylinder/semisphere area constructors.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
| --- | --- | --- | --- | --- |
| A | Charge-all DB rollback integration test planning | repository integration tests/fixtures | Medium | Independent from region lifecycle files. |
| B | Nearby packet golden gap audit | packet tests/docs read-only unless assigned | Low | Avoid live send work. |
| C | Zone handler source audit | read-only Java zone handler files | Low | Useful before live zone callbacks. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
| --- | --- | --- | --- |
| Orchestrator | Material-zone numeric geometry metadata | material construction service/tests, docs | Java writes, live nearby dispatch |
| Read-only Agent | Zone handler source audit | Java source reads only | all writes |

## Do Not Parallelize

- Java source files: read-only only.
- Live nearby dispatch and live `MapRegion` storage: still high risk and intentionally disabled.
- Shared material construction helper changes: one owner only if implementation begins.

## Continuation Context

Current unit commit message:

```text
[Phase 6][UOW-1649] Model material zone construction
```

Files changed in this unit:

- `dotnetConversion/src/Aion.GameServer/Services/WorldMapRegionMaterialZoneConstructionService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldMapRegionMaterialZoneConstructionServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ARQ-Completion.md`

Required startup reading for the next continuation:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parallelization-strategy.md`
- `docs/parity-verification.md`
- `docs/PHASE-6-PROGRESS.md`
- Latest completion handoff, currently this file

Note: `docs/commit-conventions.md` is still missing.
