# Phase 6ARS Completion - Material Zone Save Plan

Date: 2026-05-28
Unit of Work: UOW-1651
Status: Complete after focused unit tests

## Scope

This unit added a non-live save plan for Java `ZoneService.saveMaterialZones`.

The C# code models which generated material-zone templates would be handed to Java `ZoneData.saveData`, but it does not marshal XML, validate the schema, instantiate live Java template objects, or write `generated_zones.xml`.

## Completed Work

- Added `WorldMapRegionMaterialZoneSavePlanService`.
- Added material-zone save context, map-zone snapshots, zone-info snapshots, template snapshots, and save-plan DTOs.
- Modeled Java world-map scan order.
- Modeled Java skip behavior for maps with no `zoneByMapIdMap` areas.
- Modeled `collidableHandlers.containsKey(zone.getArea().getZoneName())` filtering.
- Modeled `ZoneTemplate.getMapid` ordering for generated templates.
- Added `ZoneData.saveData` persistence path metadata.
- Updated `docs/PHASE-6-PROGRESS.md` with discovery, file ownership, parity table, tests, risks, metrics, and next-unit guidance.

## Java Parity Notes

- Java iterates `DataManager.WORLD_MAPS_DATA` first and looks up `zoneByMapIdMap.get(map.getMapId())`.
- Missing area collections are skipped.
- Only zones whose area zone name exists in `collidableHandlers` are persisted.
- Java sorts collected templates by `ZoneTemplate.getMapid` before constructing `ZoneData`.
- Java delegates actual persistence to `ZoneData.saveData`, which writes `./data/static_data/zones/generated_zones.xml` through JAXB with schema validation.

## Validation

Ran:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldMapRegionMaterialZoneSavePlanServiceTests|FullyQualifiedName~WorldMapRegionMaterialZoneConstructionServiceTests|FullyQualifiedName~WorldMapRegionZoneConstructionServiceTests|FullyQualifiedName~WorldMapRegionZoneIdentityServiceTests|FullyQualifiedName~WorldMapRegionZoneCapabilityServiceTests|FullyQualifiedName~WorldMapRegionZoneScanPlanServiceTests|FullyQualifiedName~WorldMapRegionZoneSortServiceTests|FullyQualifiedName~WorldMapRegionRuntimeSnapshotServiceTests|FullyQualifiedName~WorldMapRegionLifecyclePlanServiceTests|FullyQualifiedName~WorldMapRegionCreationSnapshotServiceTests|FullyQualifiedName~WorldMapRegionZoneFilterServiceTests|FullyQualifiedName~WorldMapRegionLayoutServiceTests|FullyQualifiedName~WorldRegionKeyProjectionServiceTests"
```

Result: passed 96 tests.

Full game-server suite was not rerun in this unit.

## Parallel Work Discovery

| Candidate | Files / Area | Risk | Selected | Notes |
| --- | --- | --- | --- | --- |
| Material-zone save plan | new save-plan service/tests | Medium | Yes | Next material-zone boundary after construction and geometry metadata. |
| Charge-all DB rollback integration planning | repository integration tests | Medium | No | Independent safe alternative. |
| Nearby packet golden gap audit | packet tests/docs | Low | No | Useful later. |
| Zone-handler source audit | Java zone handler files | Low | No | Useful before live callbacks. |

## File Ownership Map

| Agent | Scope | Allowed Files | Forbidden Files | Expected Output |
| --- | --- | --- | --- | --- |
| Orchestrator | Material-zone save plan helper, tests, docs, commit | `WorldMapRegionMaterialZoneSavePlanService.cs`, `WorldMapRegionMaterialZoneSavePlanServiceTests.cs`, progress/handoff docs | Java source writes, unrelated services/tests | Implemented and documented UOW-1651. |
| Sub-agents | None | None | All files | Not spawned because selected work was small and Orchestrator-owned. |

No sub-agent was spawned for UOW-1651 because selected work was small, self-contained, and docs remained Orchestrator-owned.

## Tests Added Or Updated

| Test | Change | Validates | Java Comparison |
| --- | --- | --- | --- |
| `CreatePlan_FiltersZonesWithoutCollidableHandlersAndSortsTemplatesByMapId` | Added | Only handled area zone names are included, templates sort by map id, scan order is recorded, and persistence path is exposed. | Static source review of Java `ZoneService.saveMaterialZones`. |
| `CreatePlan_SkipsWorldMapsWithoutZoneInfoAndPreservesSameMapOrder` | Added | Missing map entries are skipped and templates with the same map id retain insertion order. | Static source review of Java null skip and stable `List.sort`. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.world.zone.ZoneService.saveMaterialZones` | `WorldMapRegionMaterialZoneSavePlanService.CreatePlan` | Material Zone Save Plan | Partial | Unit Tested | Partial Parity | C# models world-map scan order, missing-map skip behavior, collidable handler filtering, map-id sorting, and persistence-boundary metadata. It does not instantiate `ZoneData` or write XML. |
| `com.aionemu.gameserver.dataholders.ZoneData.saveData` | `WorldMapRegionMaterialZoneSavePlan.PersistencePath` | Persistence Boundary | Not Started | Unit Tested Metadata | Needs Verification | C# records the generated-zone output path and Java source boundary only. JAXB schema validation, marshalling, logging, and filesystem writes remain unported. |
| `com.aionemu.gameserver.model.templates.zone.ZoneTemplate.getMapid` | `WorldMapRegionMaterialZoneTemplateSnapshot.MapId` | Zone Template DTO | Partial | Unit Tested | Partial Parity | C# uses supplied template map ids for ordering. Live template objects, area shape serialization, priority, flags, and JAXB fields remain unported. |
| `com.aionemu.gameserver.world.zone.ZoneService.collidableHandlers` | `WorldMapRegionMaterialZoneSaveContext.CollidableHandlerZoneNames` | Handler Registry Boundary | Partial | Unit Tested | Needs Verification | C# models `containsKey` filtering using supplied zone names. Live `ZoneName` identity/cache and handler registry mutation remain unverified. |
| `com.aionemu.gameserver.dataholders.DataManager.WORLD_MAPS_DATA` | `WorldMapRegionMaterialZoneSaveContext.WorldMapIds` | Data Manager Boundary DTO | Not Started | Unit Tested Metadata | Needs Verification | C# scans supplied map ids in Java world-map order. Live data manager iteration order and loaded template set remain unverified. |

## Remaining Risks

- Save planning is non-live and records selected templates/metadata only.
- JAXB `ZoneData.saveData`, XSD validation, logging, and filesystem writes remain unported.
- Live `ZoneTemplate` shape fields, priority, flags, XML names, and area serialization remain unported.
- Live `ZoneName` identity/cache and collidable handler registry mutation remain unverified.
- Runtime `DataManager.WORLD_MAPS_DATA` iteration order is supplied metadata here, not read live.
- Live `MaterialZoneHandler` behavior, dynamic zone handlers, and live C# `MapRegion`/`ZoneInstance` storage remain disabled.
- Charge-all DB rollback remains a future gap: fake-repository tests prove runtime no-mutation/no-packet behavior, not MySQL transaction rollback.
- `docs/commit-conventions.md` is still missing.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped rows.
- Total artifacts ported: 1 non-live material-zone save-plan helper plus 2 focused tests.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification: 3 grouped rows explicitly marked Needs Verification; remaining rows are Partial Parity or Not Started metadata with known gaps.
- Total blocked artifacts: live JAXB `ZoneData.saveData`, XSD validation, generated-zone filesystem writes, live `ZoneTemplate` serialization fields, `ZoneName` identity/cache, live collidable handler registry mutation, runtime `DataManager.WORLD_MAPS_DATA` loading/order, material skill/observer behavior, dynamic zone handlers, live C# MapRegion/ZoneInstance storage, charge-all MySQL rollback regression.
- Estimated overall migration completion: about 72%.

## Next Recommended Unit Of Work

Next best unit:

| Candidate | Files / Area | Notes |
| --- | --- | --- |
| Non-live ZoneData save boundary | new serialization-boundary helper/tests | Model generated material-zone XML save inputs, shape-specific required fields, JAXB/XSD path, and error metadata without writing files. |
| Charge-all DB rollback integration regression | gated DB integration tests | Prove actual MySQL rollback/no-DB-mutation on partial charge-all save failure. |
| Nearby packet golden gap audit | packet tests/docs | Keep read-only unless adding isolated packet tests. |

## Next Work Options

## Recommended Sequential Task

- Task: add a non-live `ZoneData.saveData` XML serialization boundary model for generated material zones.
- Why: save planning now selects and orders templates; the next Java boundary is JAXB/schema/file-output behavior.
- Files: likely new helper/tests plus docs.
- Java source to read: `ZoneData.saveData`, JAXB annotations on `ZoneData` and `ZoneTemplate`, shape fields on `Cylinder`, `Sphere`, and `Semisphere`.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
| --- | --- | --- | --- | --- |
| A | Charge-all DB rollback integration test planning | repository integration tests/fixtures | Medium | Independent from region lifecycle files. |
| B | Nearby packet golden gap audit | packet tests/docs read-only unless assigned | Low | Avoid live send work. |
| C | Zone handler source audit | read-only Java zone handler files | Low | Useful before live zone callbacks. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
| --- | --- | --- | --- |
| Orchestrator | Non-live ZoneData save boundary | new helper/tests, docs | Java writes, live generated-zone writes |
| Read-only Agent | Shape/JAXB source audit | Java source reads only | all writes |

## Do Not Parallelize

- Java source files: read-only only.
- Live generated-zone writes and live nearby dispatch: still high risk and intentionally disabled.
- Shared material construction/save helper files: one owner only if implementation begins.

## Continuation Context

Current unit commit message:

```text
[Phase 6][UOW-1651] Model material zone save plan
```

Files changed in this unit:

- `dotnetConversion/src/Aion.GameServer/Services/WorldMapRegionMaterialZoneSavePlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldMapRegionMaterialZoneSavePlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ARS-Completion.md`

Required startup reading for the next continuation:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parallelization-strategy.md`
- `docs/parity-verification.md`
- `docs/PHASE-6-PROGRESS.md`
- Latest completion handoff, currently this file

Note: `docs/commit-conventions.md` is still missing.
