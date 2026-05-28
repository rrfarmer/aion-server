# Phase 6ARK Completion - Region Zone Scan Plan

Date: 2026-05-28
Unit of Work: UOW-1643
Status: Complete after focused unit tests

## Scope

This unit modeled Java `MapRegion` zone scans as non-live plans over constructor-ordered zone metadata. It covers `revalidateZones`, `findZones`, both `isInsideZone` overloads, and `isInsideItemUseZone`.

This remains a DTO/model layer. It does not call live `ZoneInstance` methods, mutate creature zone membership, execute zone handlers, or instantiate `MapRegion`.

## Completed Work

- Added `WorldMapRegionZoneScanPlanService`.
- Added `WorldMapRegionZoneScanCandidate`.
- Added `WorldMapRegionZoneRevalidationPlan`.
- Added `WorldMapRegionZoneRevalidationAction` and action/inside-mode enums.
- Modeled Java per-zone-type priority suppression in `revalidateZones`.
- Modeled unspawned creature leave behavior.
- Modeled `findZones`, first matching `isInsideZone`, and item-use prefix/fortress checks.
- Added five focused tests.
- Updated `docs/PHASE-6-PROGRESS.md` with discovery, parity table, tests, risks, metrics, and next-unit guidance.

## Java Parity Notes

- Java `revalidateZones` scans `zonesSortedByTypeAndPriority` and resets priority suppression whenever `ZoneClassName` changes.
- Java leaves a zone when the creature is unspawned, a prior priority zone of the same type entered, or `zone.revalidate(creature)` returns false.
- Java `findZones` returns all inside zones in sorted array order.
- Java `isInsideZone` returns the first matching `ZoneName` result.
- Java `isInsideItemUseZone` checks `FORT` zones for `_ABYSS_CASTLE_AREA_`; otherwise it uses `ZoneTemplate.getXmlName().startsWith(zoneName.toString())`.

## Validation

Ran:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldMapRegionZoneScanPlanServiceTests|FullyQualifiedName~WorldMapRegionZoneSortServiceTests|FullyQualifiedName~WorldMapRegionRuntimeSnapshotServiceTests|FullyQualifiedName~WorldMapRegionLifecyclePlanServiceTests|FullyQualifiedName~WorldMapRegionCreationSnapshotServiceTests|FullyQualifiedName~WorldMapRegionZoneFilterServiceTests|FullyQualifiedName~WorldMapRegionLayoutServiceTests|FullyQualifiedName~WorldRegionKeyProjectionServiceTests|FullyQualifiedName~WorldRegionIdServiceTests"
```

Result: passed 62 tests.

Full game-server suite was not rerun in this unit.

## Parallel Work Discovery

| Candidate | Files / Area | Risk | Selected | Notes |
| --- | --- | --- | --- | --- |
| Zone scan/revalidation plan | new zone scan service/tests | Medium | Yes | Uses sorted zone metadata without live handlers. |
| Charge-all DB rollback integration planning | repository integration tests | Medium | No | Independent safe alternative. |
| Nearby packet golden gap audit | packet tests/docs | Low | No | Useful later. |
| Zone-handler source audit | Java zone handler files | Low | No | Useful before live callbacks. |

No sub-agent was spawned for UOW-1643 because the selected implementation and tests touched one small helper surface and docs remained orchestrator-owned.

## Tests Added Or Updated

| Test | Change | Validates | Java Comparison |
| --- | --- | --- | --- |
| `CreateRevalidationPlan_LeavesLaterPriorityZonesWithinSameTypeUntilTypeChanges` | Added | One priority zone per type group can enter; later same-type zones leave until type changes. | Static source review of Java `MapRegion.revalidateZones`. |
| `CreateRevalidationPlan_UnspawnedCreatureLeavesAllZones` | Added | Unspawned creatures leave every scanned zone. | Static source review of Java `!creature.isSpawned()` branch. |
| `FindInsideZones_ReturnsEveryInsideCreatureZoneInConstructorOrder` | Added | All inside zones are returned in constructor order. | Static source review of Java `MapRegion.findZones`. |
| `IsInsideZoneByName_UsesFirstMatchingZoneNameForCreatureAndCoordinateChecks` | Added | First matching zone-name result is returned for creature and coordinate modes. | Static source review of Java `MapRegion.isInsideZone` overloads. |
| `IsInsideItemUseZone_UsesFortressSpecialCaseOrXmlNamePrefix` | Added | Fortress special case and XML-name prefix checks are modeled. | Static source review of Java `MapRegion.isInsideItemUseZone`. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.world.MapRegion.revalidateZones` | `WorldMapRegionZoneScanPlanService.CreateRevalidationPlan` | Zone Revalidation Plan | Partial | Unit Tested | Partial Parity | C# models sorted scan order, per-type priority suppression, unspawned leave behavior, and enter/leave intent reporting. It does not call live `ZoneInstance.revalidate`, `onEnter`, or `onLeave`. |
| `com.aionemu.gameserver.world.MapRegion.findZones` | `WorldMapRegionZoneScanPlanService.FindInsideZones` | Zone Query Utility | Partial | Unit Tested | Partial Parity | C# returns all candidates marked inside in constructor order. It does not call live `ZoneInstance.isInsideCreature`. |
| `com.aionemu.gameserver.world.MapRegion.isInsideZone(ZoneName, float, float, float)` | `WorldMapRegionZoneScanPlanService.IsInsideZoneByName` with `Coordinate` mode | Coordinate Zone Query | Partial | Unit Tested | Partial Parity | C# returns the first matching zone-name coordinate result. It does not run live geometry checks. |
| `com.aionemu.gameserver.world.MapRegion.isInsideZone(ZoneName, Creature)` | `WorldMapRegionZoneScanPlanService.IsInsideZoneByName` with `Creature` mode | Creature Zone Query | Partial | Unit Tested | Partial Parity | C# returns the first matching zone-name creature result. It does not call live `ZoneInstance.isInsideCreature`. |
| `com.aionemu.gameserver.world.MapRegion.isInsideItemUseZone` | `WorldMapRegionZoneScanPlanService.IsInsideItemUseZone` | Item-Use Zone Query | Partial | Unit Tested | Partial Parity | C# models `_ABYSS_CASTLE_AREA_` fortress handling and XML-name prefix checks. It does not load live item-use zone templates or execute real geometry. |
| `com.aionemu.gameserver.world.zone.ZoneInstance.revalidate` | `WorldMapRegionZoneScanCandidate.RevalidateSucceeds` | Zone Runtime Boundary DTO | Not Started | Unit Tested Metadata | Needs Verification | C# takes revalidation outcome as metadata; live membership state, synchronization, flags, handlers, and shape checks remain unported. |
| `com.aionemu.gameserver.world.zone.ZoneInstance.onEnter/onLeave` | `WorldMapRegionZoneRevalidationAction` | Handler Intent DTO | Not Started | Unit Tested Metadata | Needs Verification | C# reports intended enter/leave actions but does not run handlers or mutate creature zone membership. |
| `com.aionemu.gameserver.model.templates.zone.ZoneTemplate.getXmlName` | `WorldMapRegionZoneScanCandidate.XmlName` | Template Field DTO | Partial | Unit Tested | Needs Verification | C# uses supplied XML names for item-use prefix checks. XML loading, case handling beyond ordinal Java-style prefix checks, and serialization remain unverified. |

## Remaining Risks

- Zone scan planning is non-live and reports intents/results from supplied metadata.
- Live `ZoneInstance.revalidate`, `onEnter`, `onLeave`, `onDie`, geometry checks, creature membership, and handler side effects remain unported.
- Java priority behavior is modeled over caller-provided constructor order; real enforcement awaits live sorted zones or template projection.
- Java `String.startsWith` item-use prefix behavior is modeled with ordinal comparison; broader XML-name normalization remains unverified.
- Live object storage, parent instance references, neighbour object references, synchronized/volatile state, scheduler behavior, AI notifications, and death callbacks remain disabled.
- Charge-all DB rollback remains a future gap: fake-repository tests prove runtime no-mutation/no-packet behavior, not MySQL transaction rollback.
- `docs/commit-conventions.md` is still missing.

## Summary Metrics

- Total Java artifacts discovered: 8 grouped rows.
- Total artifacts ported: 1 non-live zone scan helper plus 5 focused tests.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification: 3 grouped rows explicitly marked Needs Verification; remaining rows are Partial Parity or Not Started metadata with known gaps.
- Total blocked artifacts: live `ZoneInstance` handlers, live creature membership, real geometry execution in query helpers, live C# MapRegion storage, live object maps, scheduler execution, synchronization/volatile runtime parity, AI notifications, charge-all MySQL rollback regression.
- Estimated overall migration completion: about 72%.

## Next Recommended Unit Of Work

Next best unit:

| Candidate | Files / Area | Notes |
| --- | --- | --- |
| Non-live death-zone plan | new/extended zone scan service tests | Model `MapRegion.onDie` sorted inside-zone scanning and first handler short-circuit behavior. |
| Charge-all DB rollback integration regression | gated DB integration tests | Prove actual MySQL rollback/no-DB-mutation on partial charge-all save failure. |
| Nearby packet golden gap audit | packet tests/docs | Keep read-only unless adding isolated packet tests. |

## Next Work Options

## Recommended Sequential Task

- Task: add a non-live death-zone plan for Java `MapRegion.onDie`.
- Why: `onDie` is the remaining sorted-zone scan in `MapRegion` before live zone handlers.
- Files: likely extend `WorldMapRegionZoneScanPlanService.cs` and tests, plus docs.
- Java source to read: `MapRegion.onDie`, `ZoneInstance.onDie`, relevant zone handler classes.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
| --- | --- | --- | --- | --- |
| A | Charge-all DB rollback integration test planning | repository integration tests/fixtures | Medium | Independent from region lifecycle files. |
| B | Nearby packet golden gap audit | packet tests/docs read-only unless assigned | Low | Avoid live send work. |
| C | Zone handler source audit | read-only Java zone handler files | Low | Useful before live zone callbacks. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
| --- | --- | --- | --- |
| Orchestrator | Non-live death-zone plan | zone scan service/tests, docs | Java writes, live nearby dispatch |
| Read-only Agent | Zone handler source audit | Java source reads only | all writes |

## Do Not Parallelize

- Java source files: read-only only.
- Live nearby dispatch and live `MapRegion` storage: still high risk and intentionally disabled.
- Shared zone scan helper changes: one owner only if implementation begins.

## Continuation Context

Current unit commit message:

```text
[Phase 6][UOW-1643] Model region zone scan plans
```

Files changed in this unit:

- `dotnetConversion/src/Aion.GameServer/Services/WorldMapRegionZoneScanPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldMapRegionZoneScanPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ARK-Completion.md`

Required startup reading for the next continuation:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parallelization-strategy.md`
- `docs/parity-verification.md`
- `docs/PHASE-6-PROGRESS.md`
- Latest completion handoff, currently this file

Note: `docs/commit-conventions.md` is still missing.
