# Phase 6ARN Completion - Zone Identity Accessor Snapshot

Date: 2026-05-28
Unit of Work: UOW-1646
Status: Complete after focused unit tests

## Scope

This unit modeled lightweight Java `ZoneInstance` identity/accessor behavior as a non-live snapshot. It covers town id, Dominion classification, creature membership metadata for `forEach`, and handler append metadata for `addHandler`.

This remains a DTO/model layer. It does not instantiate live `ZoneInstance`, execute callbacks, catch/log callback exceptions, mutate handler lists, or run dynamic zone handlers.

## Completed Work

- Added `WorldMapRegionZoneIdentityService`.
- Added `WorldMapRegionZoneIdentityContext`.
- Added `WorldMapRegionZoneIdentitySnapshot`.
- Modeled `ZoneInstance.getTownId`.
- Modeled `ZoneInstance.isDominionZone`.
- Modeled creature membership metadata for `ZoneInstance.forEach`.
- Modeled handler append metadata for `ZoneInstance.addHandler`.
- Explicitly marked creature iteration order as unstable because Java stores creatures in a `HashMap`.
- Added four focused tests.
- Updated `docs/PHASE-6-PROGRESS.md` with discovery, file ownership, parity table, tests, risks, metrics, and next-unit guidance.

## Java Parity Notes

- Java `getTownId` returns `template.getZoneTemplate().getTownId()`.
- Java `isDominionZone` compares `ZoneTemplate.getZoneType()` to `ZoneClassName.DOMINION`.
- Java `addHandler` appends to a list.
- Java `forEach` iterates `HashMap` values through `CollectionUtil.forEach`; ordering is not stable and callback exceptions are caught/logged per object.
- C# records this boundary as metadata and does not execute callbacks.

## Validation

Ran:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldMapRegionZoneIdentityServiceTests|FullyQualifiedName~WorldMapRegionZoneCapabilityServiceTests|FullyQualifiedName~WorldMapRegionZoneScanPlanServiceTests|FullyQualifiedName~WorldMapRegionZoneSortServiceTests|FullyQualifiedName~WorldMapRegionRuntimeSnapshotServiceTests|FullyQualifiedName~WorldMapRegionLifecyclePlanServiceTests|FullyQualifiedName~WorldMapRegionCreationSnapshotServiceTests|FullyQualifiedName~WorldMapRegionZoneFilterServiceTests|FullyQualifiedName~WorldMapRegionLayoutServiceTests|FullyQualifiedName~WorldRegionKeyProjectionServiceTests|FullyQualifiedName~WorldRegionIdServiceTests"
```

Result: passed 73 tests.

Full game-server suite was not rerun in this unit.

## Parallel Work Discovery

| Candidate | Files / Area | Risk | Selected | Notes |
| --- | --- | --- | --- | --- |
| Zone identity/accessor snapshot | new identity service/tests | Low | Yes | Completes lightweight accessor metadata without live handlers. |
| Charge-all DB rollback integration planning | repository integration tests | Medium | No | Independent safe alternative. |
| Nearby packet golden gap audit | packet tests/docs | Low | No | Useful later. |
| Zone-handler source audit | Java zone handler files | Low | No | Useful before live callbacks. |

## File Ownership Map

| Agent | Scope | Allowed Files | Forbidden Files | Expected Output |
| --- | --- | --- | --- | --- |
| Orchestrator | Zone identity/accessor helper, tests, docs, commit | `WorldMapRegionZoneIdentityService.cs`, `WorldMapRegionZoneIdentityServiceTests.cs`, progress/handoff docs | Java source writes, unrelated services/tests | Implemented and documented UOW-1646. |
| Sub-agents | None | None | All files | Not spawned because implementation and docs were small and Orchestrator-owned. |

No sub-agent was spawned for UOW-1646 because the selected helper and tests were small and progress/handoff docs remained Orchestrator-owned.

## Tests Added Or Updated

| Test | Change | Validates | Java Comparison |
| --- | --- | --- | --- |
| `CreateSnapshot_CarriesTownIdAndDominionZoneFlag` | Added | Town id metadata and Dominion classification are carried. | Static source review of Java `ZoneInstance.getTownId` and `isDominionZone`. |
| `CreateSnapshot_NonDominionZoneDoesNotReportDominion` | Added | Non-Dominion zone types are not reported as Dominion. | Static source review of Java `isDominionZone`. |
| `CreateSnapshot_CarriesCreatureMembershipAndMarksIterationOrderUnstable` | Added | Creature membership metadata is carried and iteration order is marked unstable. | Static source review of Java `HashMap` creature storage and `CollectionUtil.forEach`. |
| `CreateSnapshot_CarriesAttachedHandlerMetadataInAppendOrder` | Added | Handler metadata is carried in append order. | Static source review of Java `ZoneInstance.addHandler`. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.world.zone.ZoneInstance.getTownId` | `WorldMapRegionZoneIdentitySnapshot.TownId` | Zone Accessor DTO | Partial | Unit Tested | Partial Parity | C# carries supplied town id metadata. It does not read live `ZoneTemplate.getTownId`. |
| `com.aionemu.gameserver.world.zone.ZoneInstance.isDominionZone` | `WorldMapRegionZoneIdentitySnapshot.IsDominionZone` | Zone Accessor DTO | Partial | Unit Tested | Partial Parity | C# compares projected zone type to `Dominion`, matching Java's `ZoneClassName.DOMINION` check. |
| `com.aionemu.gameserver.world.zone.ZoneInstance.forEach` | `WorldMapRegionZoneIdentitySnapshot.CreatureObjectIds`; `CreatureIterationOrderIsStable` | Creature Iteration Boundary DTO | Partial | Unit Tested | Needs Verification | C# carries creature ids as metadata and explicitly marks Java `HashMap` iteration order unstable. It does not execute `CollectionUtil.forEach` or catch/log per-creature callback exceptions. |
| `com.aionemu.gameserver.world.zone.ZoneInstance.addHandler` | `WorldMapRegionZoneIdentitySnapshot.HandlerNames` | Handler Registration Boundary DTO | Partial | Unit Tested | Needs Verification | C# carries handler names in append order. It does not instantiate or execute live handlers. |
| `com.aionemu.gameserver.utils.collections.CollectionUtil.forEach` | `WorldMapRegionZoneIdentitySnapshot.CreatureIterationOrderIsStable` | Utility Dependency | Not Started | Unit Tested Metadata | Needs Verification | Java catches/logs exceptions and continues iteration. C# metadata records the boundary only; callback execution and logging behavior remain unported. |
| `com.aionemu.gameserver.world.zone.ZoneService.getZoneInstancesByWorldId` | `WorldMapRegionZoneIdentitySnapshot.HandlerNames` metadata only | Zone Construction Dependency | Partial | Manual Source Review | Needs Verification | Java attaches handlers during zone instance construction. C# records handler metadata but does not reproduce `ZoneService` construction, siege/artifact attachment, invasion zone selection, or material handler behavior. |

## Remaining Risks

- Identity/accessor snapshot is non-live and uses supplied metadata.
- Live `ZoneTemplate`, creature `HashMap`, handler list mutation, `CollectionUtil.forEach` callback execution/logging, and `ZoneService` construction side effects remain unported.
- Java `HashMap` iteration order is intentionally not treated as stable.
- Siege/artifact zone attachment, invasion zone substitution, material handlers, and dynamic handler loading remain future work.
- Live C# `MapRegion`/`ZoneInstance` storage, scheduler behavior, AI notifications, and handler callbacks remain disabled.
- Charge-all DB rollback remains a future gap: fake-repository tests prove runtime no-mutation/no-packet behavior, not MySQL transaction rollback.
- `docs/commit-conventions.md` is still missing.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped rows.
- Total artifacts ported: 1 non-live identity/accessor helper plus 4 focused tests.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification: 4 grouped rows explicitly marked Needs Verification; remaining rows are Partial Parity with known gaps.
- Total blocked artifacts: live `ZoneTemplate`, live creature map, handler execution, `CollectionUtil.forEach` logging behavior, `ZoneService` construction side effects, dynamic zone handlers, live C# MapRegion/ZoneInstance storage, charge-all MySQL rollback regression.
- Estimated overall migration completion: about 72%.

## Next Recommended Unit Of Work

Next best unit:

| Candidate | Files / Area | Notes |
| --- | --- | --- |
| Non-live ZoneService construction plan | new service/tests | Model full-map zone creation and Java instance type selection before live zone storage. |
| Charge-all DB rollback integration regression | gated DB integration tests | Prove actual MySQL rollback/no-DB-mutation on partial charge-all save failure. |
| Nearby packet golden gap audit | packet tests/docs | Keep read-only unless adding isolated packet tests. |

## Next Work Options

## Recommended Sequential Task

- Task: start a non-live `ZoneService.getZoneInstancesByWorldId` construction plan.
- Why: the lightweight `ZoneInstance` helper surface is now modeled; construction/type selection is the next dependency before live zone arrays.
- Files: likely new zone construction service/tests plus docs.
- Java source to read: `ZoneService.getZoneInstancesByWorldId`, `getIZI`, `validateZone`, siege/artifact branches, PVP/FLY/NO_FLY instance selection.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
| --- | --- | --- | --- | --- |
| A | Charge-all DB rollback integration test planning | repository integration tests/fixtures | Medium | Independent from region lifecycle files. |
| B | Nearby packet golden gap audit | packet tests/docs read-only unless assigned | Low | Avoid live send work. |
| C | Zone handler source audit | read-only Java zone handler files | Low | Useful before live zone callbacks. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
| --- | --- | --- | --- |
| Orchestrator | Non-live ZoneService construction plan | new service/tests, docs | Java writes, live nearby dispatch |
| Read-only Agent | Zone handler source audit | Java source reads only | all writes |

## Do Not Parallelize

- Java source files: read-only only.
- Live nearby dispatch and live `MapRegion` storage: still high risk and intentionally disabled.
- Shared zone construction helpers: one owner only if implementation begins.

## Continuation Context

Current unit commit message:

```text
[Phase 6][UOW-1646] Model zone identity accessors
```

Files changed in this unit:

- `dotnetConversion/src/Aion.GameServer/Services/WorldMapRegionZoneIdentityService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldMapRegionZoneIdentityServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ARN-Completion.md`

Required startup reading for the next continuation:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parallelization-strategy.md`
- `docs/parity-verification.md`
- `docs/PHASE-6-PROGRESS.md`
- Latest completion handoff, currently this file

Note: `docs/commit-conventions.md` is still missing.
