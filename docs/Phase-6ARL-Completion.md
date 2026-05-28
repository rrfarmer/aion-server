# Phase 6ARL Completion - Region Death Zone Plan

Date: 2026-05-28
Unit of Work: UOW-1644
Status: Complete after focused unit tests

## Scope

This unit modeled Java `MapRegion.onDie` and `ZoneInstance.onDie` as a non-live death-zone plan over constructor-ordered zone metadata.

This remains a DTO/model layer. It does not call live `ZoneInstance.onDie`, execute `AdvancedZoneHandler` implementations, mutate creature membership, send packets, schedule revives, or instantiate `MapRegion`.

## Completed Work

- Extended `WorldMapRegionZoneScanPlanService` with `CreateDeathPlan`.
- Added `WorldMapRegionZoneDeathPlan`.
- Added `WorldMapRegionZoneDeathAction`.
- Added `WorldMapRegionZoneDeathActionType`.
- Added `DeathHandlerHandles` metadata to `WorldMapRegionZoneScanCandidate`.
- Modeled Java sorted scan behavior: skip outside zones, scan inside zones in constructor order, and stop on the first handled death.
- Added tests for first-handler short-circuiting and unhandled inside-zone scans.
- Updated `docs/PHASE-6-PROGRESS.md` with discovery, file ownership, parity table, tests, risks, metrics, and next-unit guidance.

## Java Parity Notes

- Java `MapRegion.onDie` scans `zonesSortedByTypeAndPriority`.
- It calls `zone.onDie(attacker, target)` only if `zone.isInsideCreature(target)` is true.
- Java `ZoneInstance.onDie` first verifies the target is in the zone creature map.
- It only invokes handlers implementing `AdvancedZoneHandler`.
- It returns true immediately when the first advanced handler handles the death event.
- C# models those decisions using explicit metadata; live handler side effects remain unported.

## Validation

Ran:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldMapRegionZoneScanPlanServiceTests|FullyQualifiedName~WorldMapRegionZoneSortServiceTests|FullyQualifiedName~WorldMapRegionRuntimeSnapshotServiceTests|FullyQualifiedName~WorldMapRegionLifecyclePlanServiceTests|FullyQualifiedName~WorldMapRegionCreationSnapshotServiceTests|FullyQualifiedName~WorldMapRegionZoneFilterServiceTests|FullyQualifiedName~WorldMapRegionLayoutServiceTests|FullyQualifiedName~WorldRegionKeyProjectionServiceTests|FullyQualifiedName~WorldRegionIdServiceTests"
```

Result: passed 64 tests.

Full game-server suite was not rerun in this unit.

## Parallel Work Discovery

| Candidate | Files / Area | Risk | Selected | Notes |
| --- | --- | --- | --- | --- |
| Death-zone plan | zone scan service/tests | Medium | Yes | Extends shared scan helper and test file, so one writer is safest. |
| Charge-all DB rollback integration planning | repository integration tests | Medium | No | Independent safe alternative. |
| Nearby packet golden gap audit | packet tests/docs | Low | No | Useful later. |
| Zone-handler source audit | Java zone handler files | Low | No | Useful before live callbacks. |

## File Ownership Map

| Agent | Scope | Allowed Files | Forbidden Files | Expected Output |
| --- | --- | --- | --- | --- |
| Orchestrator | Death-zone plan implementation, tests, docs, commit | `WorldMapRegionZoneScanPlanService.cs`, `WorldMapRegionZoneScanPlanServiceTests.cs`, progress/handoff docs | Java source writes, unrelated services/tests | Implemented and documented UOW-1644. |
| Sub-agents | None | None | All files | Not spawned because selected work edits shared helper/test/docs. |

No sub-agent was spawned for UOW-1644 because the selected implementation modifies the same helper/test files and Orchestrator-owned docs.

## Tests Added Or Updated

| Test | Change | Validates | Java Comparison |
| --- | --- | --- | --- |
| `CreateDeathPlan_ScansOnlyInsideZonesAndStopsOnFirstHandledDeath` | Added | Outside zones are skipped, inside unhandled zones are scanned, and the first handled zone short-circuits later zones. | Static source review of Java `MapRegion.onDie` and `ZoneInstance.onDie`. |
| `CreateDeathPlan_ReturnsUnhandledWhenInsideZonesDoNotHandleDeath` | Added | Inside zones that do not handle death are recorded and final result is unhandled. | Static source review of Java `MapRegion.onDie` and `ZoneInstance.onDie`. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.world.MapRegion.onDie` | `WorldMapRegionZoneScanPlanService.CreateDeathPlan` | Death Zone Plan | Partial | Unit Tested | Partial Parity | C# models sorted scan order, skips zones where the target is not inside, and short-circuits on the first handled death. It does not call live `ZoneInstance.onDie` or mutate handlers/creatures. |
| `com.aionemu.gameserver.world.zone.ZoneInstance.onDie` | `WorldMapRegionZoneScanCandidate.DeathHandlerHandles`; `WorldMapRegionZoneDeathAction` | Zone Runtime Boundary DTO | Partial | Unit Tested | Partial Parity | C# models the `creatures.containsKey(target)` gate via `IsInsideCreature` and the advanced-handler handled/unhandled result via metadata. It does not iterate real handler instances. |
| `com.aionemu.gameserver.world.zone.handler.AdvancedZoneHandler.onDie` | `WorldMapRegionZoneScanCandidate.DeathHandlerHandles` | Handler Boundary | Not Started | Unit Tested Metadata | Needs Verification | Advanced handler execution is represented as a supplied boolean. Handler side effects such as revive scheduling, teleport, quest progress, packet sends, and broadcasts remain unported. |
| `zone.pvpZones.PvPZone.onDie` | `WorldMapRegionZoneDeathPlan` metadata only | Dynamic Handler Boundary | Not Started | Manual Source Review | Needs Verification | Java PVP handler sends system messages, schedules revive/teleport, and broadcasts. C# does not execute dynamic zone handlers yet. Newly discovered dependency for future live handler work. |
| `com.aionemu.gameserver.world.zone.ZoneInstance.isInsideCreature` | `WorldMapRegionZoneScanCandidate.IsInsideCreature` | Zone Membership Boundary DTO | Partial | Unit Tested | Needs Verification | C# uses supplied membership metadata for `onDie` and `findZones`; live creature membership map remains unported. |

## Remaining Risks

- Death-zone planning is non-live and reports handler results from supplied metadata.
- Live `ZoneInstance.onDie`, `AdvancedZoneHandler` implementations, PVP revive/teleport scheduling, quest/instance death handling, packet sends, broadcasts, and creature membership maps remain unported.
- Java handler side effects may require dynamic handler loading and scheduler parity before live replacement.
- Java priority and death behavior are modeled over caller-provided constructor order; real enforcement awaits live sorted zones or template projection.
- Live object storage, parent instance references, neighbour object references, synchronized/volatile state, scheduler behavior, AI notifications, and handler callbacks remain disabled.
- Charge-all DB rollback remains a future gap: fake-repository tests prove runtime no-mutation/no-packet behavior, not MySQL transaction rollback.
- `docs/commit-conventions.md` is still missing.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped rows.
- Total artifacts ported: 1 non-live death-zone plan extension plus 2 focused tests.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification: 3 grouped rows explicitly marked Needs Verification; remaining rows are Partial Parity or Not Started metadata with known gaps.
- Total blocked artifacts: live `ZoneInstance.onDie`, dynamic advanced zone handlers, PVP revive/teleport scheduling, quest/instance death side effects, live creature membership, live C# MapRegion storage, scheduler execution, synchronization/volatile runtime parity, charge-all MySQL rollback regression.
- Estimated overall migration completion: about 72%.

## Next Recommended Unit Of Work

Next best unit:

| Candidate | Files / Area | Notes |
| --- | --- | --- |
| Non-live zone capability/options plan | new/extended zone option service tests | Model `ZoneInstance` flag/world-option fallback behavior for fly/glide/kisk/recall/ride style checks. |
| Charge-all DB rollback integration regression | gated DB integration tests | Prove actual MySQL rollback/no-DB-mutation on partial charge-all save failure. |
| Nearby packet golden gap audit | packet tests/docs | Keep read-only unless adding isolated packet tests. |

## Next Work Options

## Recommended Sequential Task

- Task: add a non-live zone capability/options plan for Java `ZoneInstance`.
- Why: option checks are the next live `ZoneInstance` behavior cluster after scan/death flows.
- Files: likely new service/tests or extension of existing flight/world option helpers plus docs.
- Java source to read: `ZoneInstance.canFly`, `canGlide`, `canPutKisk`, `canRecall`, `canReturnToBattle`, `canRide`, `canFlyRide`, `isPvpAllowed`, and `isDuelAllowed`.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
| --- | --- | --- | --- | --- |
| A | Charge-all DB rollback integration test planning | repository integration tests/fixtures | Medium | Independent from region lifecycle files. |
| B | Nearby packet golden gap audit | packet tests/docs read-only unless assigned | Low | Avoid live send work. |
| C | Zone handler source audit | read-only Java zone handler files | Low | Useful before live zone callbacks. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
| --- | --- | --- | --- |
| Orchestrator | Non-live zone capability/options plan | new/extended zone option service/tests, docs | Java writes, live nearby dispatch |
| Read-only Agent | Zone handler source audit | Java source reads only | all writes |

## Do Not Parallelize

- Java source files: read-only only.
- Live nearby dispatch and live `MapRegion` storage: still high risk and intentionally disabled.
- Shared zone option helpers: one owner only if implementation begins.

## Continuation Context

Current unit commit message:

```text
[Phase 6][UOW-1644] Model region death zone plans
```

Files changed in this unit:

- `dotnetConversion/src/Aion.GameServer/Services/WorldMapRegionZoneScanPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldMapRegionZoneScanPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ARL-Completion.md`

Required startup reading for the next continuation:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parallelization-strategy.md`
- `docs/parity-verification.md`
- `docs/PHASE-6-PROGRESS.md`
- Latest completion handoff, currently this file

Note: `docs/commit-conventions.md` is still missing.
