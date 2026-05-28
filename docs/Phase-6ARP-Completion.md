# Phase 6ARP Completion - ZoneService Full-Map Metadata

Date: 2026-05-28
Unit of Work: UOW-1648
Status: Complete after focused unit tests

## Scope

This unit extended the non-live `ZoneService.getZoneInstancesByWorldId` construction plan with Java full-map `WorldZoneTemplate` bounds/flags metadata and duplicate `ZoneName` replacement semantics.

This remains a DTO/model layer. It does not instantiate live `WorldZoneTemplate`, `ZoneName`, `ZoneInstance`, or a Java-equivalent `HashMap`.

## Completed Work

- Extended `WorldMapRegionZoneConstructionContext` with region size and world flags.
- Added `WorldMapRegionZoneFullMapBounds`.
- Added full-map bounds/flags metadata to the full-map construction entry.
- Added `FinalZoneIds` to construction plans.
- Added `ReplacedZoneIds` to construction plans.
- Modeled duplicate zone ids as last-write-wins metadata, matching Java `HashMap.put`.
- Added focused tests for full-map bounds/flags and duplicate replacement semantics.
- Updated `docs/PHASE-6-PROGRESS.md` with discovery, file ownership, parity table, tests, risks, metrics, and next-unit guidance.

## Java Parity Notes

- Java `WorldZoneTemplate` creates full-map polygon points at `-1` and `size + 1`.
- Java bottom is `-1`; top is rounded world-size/region-size maxZ plus `1`.
- Java full-map zone type is `DUMMY`, map id is the target map id, flags come from `WorldMapTemplate.getFlags`, and XML name is the map id string.
- Java `ZoneService.getZoneInstancesByWorldId` stores zones in `HashMap`, so duplicate `ZoneName` keys replace prior values.
- C# records final/replaced zone ids but does not model Java HashMap bucket iteration order.

## Validation

Ran:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldMapRegionZoneConstructionServiceTests|FullyQualifiedName~WorldMapRegionZoneIdentityServiceTests|FullyQualifiedName~WorldMapRegionZoneCapabilityServiceTests|FullyQualifiedName~WorldMapRegionZoneScanPlanServiceTests|FullyQualifiedName~WorldMapRegionZoneSortServiceTests|FullyQualifiedName~WorldMapRegionRuntimeSnapshotServiceTests|FullyQualifiedName~WorldMapRegionLifecyclePlanServiceTests|FullyQualifiedName~WorldMapRegionCreationSnapshotServiceTests|FullyQualifiedName~WorldMapRegionZoneFilterServiceTests|FullyQualifiedName~WorldMapRegionLayoutServiceTests|FullyQualifiedName~WorldRegionKeyProjectionServiceTests|FullyQualifiedName~WorldRegionIdServiceTests"
```

Result: passed 81 tests.

Full game-server suite was not rerun in this unit.

## Parallel Work Discovery

| Candidate | Files / Area | Risk | Selected | Notes |
| --- | --- | --- | --- | --- |
| Full-map/duplicate construction metadata | construction service/tests | Medium | Yes | Extends shared construction helper from UOW-1647. |
| Charge-all DB rollback integration planning | repository integration tests | Medium | No | Independent safe alternative. |
| Nearby packet golden gap audit | packet tests/docs | Low | No | Useful later. |
| Zone-handler source audit | Java zone handler files | Low | No | Useful before live callbacks. |

## File Ownership Map

| Agent | Scope | Allowed Files | Forbidden Files | Expected Output |
| --- | --- | --- | --- | --- |
| Orchestrator | Full-map and duplicate construction metadata, tests, docs, commit | `WorldMapRegionZoneConstructionService.cs`, `WorldMapRegionZoneConstructionServiceTests.cs`, progress/handoff docs | Java source writes, unrelated services/tests | Implemented and documented UOW-1648. |
| Sub-agents | None | None | All files | Not spawned because selected work edits shared construction helper/test/docs. |

No sub-agent was spawned for UOW-1648 because selected work edits shared construction helper/test/docs and needs one owner.

## Tests Added Or Updated

| Test | Change | Validates | Java Comparison |
| --- | --- | --- | --- |
| `CreatePlan_FullMapZoneCarriesJavaWorldZoneTemplateBoundsAndFlags` | Added | Full-map x/y bounds, bottom/top rounding by region size, and flags metadata are carried. | Static source review of Java `WorldZoneTemplate` constructor. |
| `CreatePlan_FinalZoneIdsPreserveJavaHashMapPutReplacementSemantics` | Added | Duplicate zone ids are recorded as replacements and final ids represent last-write-wins semantics. | Static source review of Java `HashMap.put` in `ZoneService.getZoneInstancesByWorldId`. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.model.templates.zone.WorldZoneTemplate` | `WorldMapRegionZoneFullMapBounds`; first `WorldMapRegionZoneConstructionEntry` | Full-Map Zone Template DTO | Partial | Unit Tested | Partial Parity | C# now models full-map x/y bounds, bottom/top, DUMMY type, map-id zone id, and flags metadata. It still does not model actual `Points`, JAXB binding, or `ZoneName.createOrGet` cache behavior. |
| `com.aionemu.gameserver.world.zone.ZoneService.getZoneInstancesByWorldId` | `WorldMapRegionZoneConstructionPlan.FinalZoneIds`; `ReplacedZoneIds` | Zone Construction Plan | Partial | Unit Tested | Partial Parity | C# now records final zone ids and duplicate replacements to model Java `HashMap.put`. It does not return live `Map<ZoneName, ZoneInstance>` or preserve Java hash bucket iteration behavior. |
| `java.util.HashMap.put` | `WorldMapRegionZoneConstructionPlan.FinalZoneIds`; `ReplacedZoneIds` | Collection Semantics Dependency | Partial | Unit Tested | Needs Verification | Duplicate zone ids replace previous entries in C# metadata. Java HashMap iteration order remains unmodeled and is not treated as stable. |
| `com.aionemu.gameserver.configs.main.WorldConfig.WORLD_REGION_SIZE` | `WorldMapRegionZoneConstructionContext.RegionSize` | Config Boundary | Partial | Unit Tested | Needs Verification | C# uses supplied region size to compute full-map top metadata. Runtime config wiring remains unverified. |
| `com.aionemu.gameserver.model.templates.world.WorldMapTemplate.getFlags` | `WorldMapRegionZoneConstructionContext.WorldFlags`; `WorldMapRegionZoneFullMapBounds.Flags` | World Template Boundary DTO | Partial | Unit Tested | Needs Verification | C# carries supplied world flags into full-map metadata. Live `DataManager.WORLD_MAPS_DATA` lookup remains unported at this boundary. |

## Remaining Risks

- Construction planning remains non-live and records selected types/metadata only.
- Full-map `Points`, `ZoneName.createOrGet`, JAXB/XML serialization, live world-map template lookup, and Java HashMap iteration order remain unverified.
- Live `DataManager` lookups, `ZoneInstance` subclasses, handler instantiation, siege/artifact attachment, shield service, vortex mutation, material zone creation, and dynamic handler loading remain unported.
- Live C# `MapRegion`/`ZoneInstance` storage and callbacks remain disabled.
- Charge-all DB rollback remains a future gap: fake-repository tests prove runtime no-mutation/no-packet behavior, not MySQL transaction rollback.
- `docs/commit-conventions.md` is still missing.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped rows.
- Total artifacts ported: 1 construction helper extension plus 2 focused tests.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification: 3 grouped rows explicitly marked Needs Verification; remaining rows are Partial Parity with known gaps.
- Total blocked artifacts: live `ZoneInstance` subclasses, full-map `Points` object model, `ZoneName` cache behavior, `DataManager` lookups, live handler instantiation, siege/artifact attachment, `ShieldService`, vortex mutation, material zone creation, dynamic zone handlers, live C# MapRegion/ZoneInstance storage, charge-all MySQL rollback regression.
- Estimated overall migration completion: about 72%.

## Next Recommended Unit Of Work

Next best unit:

| Candidate | Files / Area | Notes |
| --- | --- | --- |
| Non-live material-zone construction plan | new service/tests | Model `ZoneService.createMaterialZoneTemplate` duplicate mesh handling, shield/material handlers, and area-type metadata. |
| Charge-all DB rollback integration regression | gated DB integration tests | Prove actual MySQL rollback/no-DB-mutation on partial charge-all save failure. |
| Nearby packet golden gap audit | packet tests/docs | Keep read-only unless adding isolated packet tests. |

## Next Work Options

## Recommended Sequential Task

- Task: add a non-live material-zone construction plan for Java `ZoneService.createMaterialZoneTemplate`.
- Why: material zones mutate the same zone data map and handler map used by `getZoneInstancesByWorldId`.
- Files: likely new material zone construction helper/tests plus docs.
- Java source to read: `ZoneService.createMaterialZoneTemplate`, `saveMaterialZones`, `MaterialZoneTemplate`, `MaterialZoneHandler`, `ShieldService.tryRegisterShield`.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
| --- | --- | --- | --- | --- |
| A | Charge-all DB rollback integration test planning | repository integration tests/fixtures | Medium | Independent from region lifecycle files. |
| B | Nearby packet golden gap audit | packet tests/docs read-only unless assigned | Low | Avoid live send work. |
| C | Zone handler source audit | read-only Java zone handler files | Low | Useful before live zone callbacks. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
| --- | --- | --- | --- |
| Orchestrator | Non-live material-zone construction plan | new service/tests, docs | Java writes, live nearby dispatch |
| Read-only Agent | Zone handler source audit | Java source reads only | all writes |

## Do Not Parallelize

- Java source files: read-only only.
- Live nearby dispatch and live `MapRegion` storage: still high risk and intentionally disabled.
- Shared material/zone construction helpers: one owner only if implementation begins.

## Continuation Context

Current unit commit message:

```text
[Phase 6][UOW-1648] Add ZoneService full-map metadata
```

Files changed in this unit:

- `dotnetConversion/src/Aion.GameServer/Services/WorldMapRegionZoneConstructionService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldMapRegionZoneConstructionServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ARP-Completion.md`

Required startup reading for the next continuation:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parallelization-strategy.md`
- `docs/parity-verification.md`
- `docs/PHASE-6-PROGRESS.md`
- Latest completion handoff, currently this file

Note: `docs/commit-conventions.md` is still missing.
