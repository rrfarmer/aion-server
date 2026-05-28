# Phase 6ARM Completion - Zone Capability Option Plan

Date: 2026-05-28
Unit of Work: UOW-1645
Status: Complete after focused unit tests

## Scope

This unit modeled Java `ZoneInstance` capability and world-option flag resolution as a non-live plan. It covers fly, glide, kisk, recall, return-to-battle, ride, fly-ride, PVP, and duel option checks.

This remains a DTO/model layer. It does not read live `World.getInstance()`, mutate world options, load live `ZoneTemplate` objects, or execute zone handlers.

## Completed Work

- Added `WorldMapRegionZoneCapabilityService`.
- Added `WorldMapRegionZoneCapabilityContext`.
- Added `WorldMapRegionZoneCapabilityPlan`.
- Modeled Java flag fallback for `canFly`, `canGlide`, `canPutKisk`, `canRecall`, `canRide`, and `canFlyRide`.
- Modeled `canReturnToBattle` as a world-map-only result.
- Modeled PVP and duel special branches.
- Added five focused tests.
- Updated `docs/PHASE-6-PROGRESS.md` with discovery, file ownership, parity table, tests, risks, metrics, and next-unit guidance.

## Java Parity Notes

- Java uses world-map option state when zone flags are `-1` or `0`.
- Java also uses world-map option state when `WorldMap.hasOverridenOption(...)` reports an override.
- Otherwise Java uses the relevant bit from `ZoneTemplate.flags`.
- `canReturnToBattle` delegates to the world map and ignores zone flags.
- PVP zones use the zone PVP flag; non-PVP zones delegate to the world map.
- DUEL zones use duel flags unless flags are zero or overridden; non-DUEL zones delegate to the world map.

## Validation

Ran:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldMapRegionZoneCapabilityServiceTests|FullyQualifiedName~WorldMapRegionZoneScanPlanServiceTests|FullyQualifiedName~WorldMapRegionZoneSortServiceTests|FullyQualifiedName~WorldMapRegionRuntimeSnapshotServiceTests|FullyQualifiedName~WorldMapRegionLifecyclePlanServiceTests|FullyQualifiedName~WorldMapRegionCreationSnapshotServiceTests|FullyQualifiedName~WorldMapRegionZoneFilterServiceTests|FullyQualifiedName~WorldMapRegionLayoutServiceTests|FullyQualifiedName~WorldRegionKeyProjectionServiceTests|FullyQualifiedName~WorldRegionIdServiceTests"
```

Result: passed 69 tests.

Full game-server suite was not rerun in this unit.

## Parallel Work Discovery

| Candidate | Files / Area | Risk | Selected | Notes |
| --- | --- | --- | --- | --- |
| Zone capability/options plan | new capability service/tests | Medium | Yes | Self-contained helper over existing world-map flag DTOs. |
| Charge-all DB rollback integration planning | repository integration tests | Medium | No | Independent safe alternative. |
| Nearby packet golden gap audit | packet tests/docs | Low | No | Useful later. |
| Zone-handler source audit | Java zone handler files | Low | No | Useful before live callbacks. |

## File Ownership Map

| Agent | Scope | Allowed Files | Forbidden Files | Expected Output |
| --- | --- | --- | --- | --- |
| Orchestrator | Zone capability/options helper, tests, docs, commit | `WorldMapRegionZoneCapabilityService.cs`, `WorldMapRegionZoneCapabilityServiceTests.cs`, progress/handoff docs | Java source writes, unrelated services/tests | Implemented and documented UOW-1645. |
| Sub-agents | None | None | All files | Not spawned because implementation and docs were small and Orchestrator-owned. |

No sub-agent was spawned for UOW-1645 because the selected helper and tests were small and progress/handoff docs remained Orchestrator-owned.

## Tests Added Or Updated

| Test | Change | Validates | Java Comparison |
| --- | --- | --- | --- |
| `CreatePlan_UsesWorldMapOptionsWhenZoneFlagsAreMinusOneOrZero` | Added | `-1` and `0` zone flags delegate to world-map option state. | Static source review of Java `ZoneInstance` fallback branches. |
| `CreatePlan_UsesZoneFlagsUnlessWorldMapOptionWasOverridden` | Added | Nonzero zone flags decide unless world map option state is overridden. | Static source review of Java `hasOverridenOption` branches. |
| `CreatePlan_ReturnToBattleAlwaysUsesWorldMapNoReturnBattleFlag` | Added | Return-to-battle ignores zone flags and delegates to world map. | Static source review of Java `ZoneInstance.canReturnToBattle`. |
| `CreatePlan_PvpZoneUsesZonePvpFlagAndNonPvpUsesWorldMapFlag` | Added | PVP zones use zone PVP flag; non-PVP zones use world-map PVP state. | Static source review of Java `ZoneInstance.isPvpAllowed`. |
| `CreatePlan_DuelZonesUseFlagsUnlessZeroOrWorldMapOverride` | Added | DUEL zones use flags unless zero or override; non-DUEL zones use world state. | Static source review of Java duel checks. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.world.zone.ZoneInstance.canFly` | `WorldMapRegionZoneCapabilityService.CreatePlan` / `CanFly` | Zone Capability Utility | Partial | Unit Tested | Partial Parity | C# models Java flag/world-option fallback for FLY. It does not read live `World.getInstance()` or live `ZoneTemplate` storage. |
| `com.aionemu.gameserver.world.zone.ZoneInstance.canGlide` | `WorldMapRegionZoneCapabilityService.CreatePlan` / `CanGlide` | Zone Capability Utility | Partial | Unit Tested | Partial Parity | C# models Java GLIDE fallback/flag behavior using `WorldMapSummary` and current world flags. |
| `com.aionemu.gameserver.world.zone.ZoneInstance.canPutKisk` | `WorldMapRegionZoneCapabilityService.CreatePlan` / `CanPutKisk` | Zone Capability Utility | Partial | Unit Tested | Partial Parity | C# maps Java BIND flag behavior. Live world-map mutation and template reads remain unverified. |
| `com.aionemu.gameserver.world.zone.ZoneInstance.canRecall` | `WorldMapRegionZoneCapabilityService.CreatePlan` / `CanRecall` | Zone Capability Utility | Partial | Unit Tested | Partial Parity | C# maps Java RECALL fallback/flag behavior. |
| `com.aionemu.gameserver.world.zone.ZoneInstance.canReturnToBattle` | `WorldMapRegionZoneCapabilityService.CreatePlan` / `CanReturnToBattle` | Zone Capability Utility | Partial | Unit Tested | Partial Parity | Java always delegates to world map. C# delegates to `WorldMapSummary.CanReturnToBattle` over current flags. |
| `com.aionemu.gameserver.world.zone.ZoneInstance.canRide` | `WorldMapRegionZoneCapabilityService.CreatePlan` / `CanRide` | Zone Capability Utility | Partial | Unit Tested | Partial Parity | C# maps Java RIDE fallback/flag behavior. |
| `com.aionemu.gameserver.world.zone.ZoneInstance.canFlyRide` | `WorldMapRegionZoneCapabilityService.CreatePlan` / `CanFlyRide` | Zone Capability Utility | Partial | Unit Tested | Partial Parity | C# maps Java FLY_RIDE fallback/flag behavior. |
| `com.aionemu.gameserver.world.zone.ZoneInstance.isPvpAllowed` | `WorldMapRegionZoneCapabilityService.CreatePlan` / `IsPvpAllowed` | Zone Capability Utility | Partial | Unit Tested | Partial Parity | C# models Java `ZoneClassName.PVP` special branch: PVP zones use the zone flag, non-PVP zones use world-map state. |
| `com.aionemu.gameserver.world.zone.ZoneInstance.isSameRaceDuelsAllowed` | `WorldMapRegionZoneCapabilityService.CreatePlan` / `IsSameRaceDuelAllowed` | Zone Capability Utility | Partial | Unit Tested | Partial Parity | C# models Java DUEL branch, zero flags fallback, and world override fallback. |
| `com.aionemu.gameserver.world.zone.ZoneInstance.isOtherRaceDuelsAllowed` | `WorldMapRegionZoneCapabilityService.CreatePlan` / `IsOtherRaceDuelAllowed` | Zone Capability Utility | Partial | Unit Tested | Partial Parity | C# models Java DUEL branch, zero flags fallback, and world override fallback. |
| `com.aionemu.gameserver.world.zone.ZoneAttributes` | `WorldZoneAttributes` | Enum / Flags | Partial | Unit Tested | Needs Verification | Existing C# flag values match Java bit positions used by this unit. XML enum serialization names and full static-data binding remain unverified. |
| `com.aionemu.gameserver.world.WorldMap` | `WorldMapSummary` | World Option Boundary DTO | Partial | Unit Tested | Needs Verification | C# uses immutable current flags and summary metadata instead of Java mutable world options and `World.getInstance()` lookup. Threading/live mutation parity remains unverified. |

## Remaining Risks

- Capability planning is non-live and uses supplied world/zone metadata.
- Live `World.getInstance()`, mutable world options, live `ZoneTemplate.flags`, XML enum serialization, and thread visibility remain unported or unverified.
- C# `WorldMapSummary` uses immutable current flags; Java mutates world options in a live world map.
- Live `ZoneInstance` storage, creature membership, zone handlers, scheduler behavior, AI notifications, and dynamic handler side effects remain disabled.
- Charge-all DB rollback remains a future gap: fake-repository tests prove runtime no-mutation/no-packet behavior, not MySQL transaction rollback.
- `docs/commit-conventions.md` is still missing.

## Summary Metrics

- Total Java artifacts discovered: 12 grouped rows.
- Total artifacts ported: 1 non-live capability helper plus 5 focused tests.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification: 2 grouped rows explicitly marked Needs Verification; remaining rows are Partial Parity with known gaps.
- Total blocked artifacts: live `World.getInstance()` option reads, mutable world option threading parity, live `ZoneTemplate.flags`, XML enum serialization, live C# MapRegion/ZoneInstance storage, dynamic zone handlers, charge-all MySQL rollback regression.
- Estimated overall migration completion: about 72%.

## Next Recommended Unit Of Work

Next best unit:

| Candidate | Files / Area | Notes |
| --- | --- | --- |
| Non-live zone identity/accessor plan | new/extended zone helper tests | Model `ZoneInstance.getTownId`, `isDominionZone`, `forEach`, and handler-registration metadata. |
| Charge-all DB rollback integration regression | gated DB integration tests | Prove actual MySQL rollback/no-DB-mutation on partial charge-all save failure. |
| Nearby packet golden gap audit | packet tests/docs | Keep read-only unless adding isolated packet tests. |

## Next Work Options

## Recommended Sequential Task

- Task: add a non-live zone identity/accessor plan for Java `ZoneInstance`.
- Why: remaining lightweight `ZoneInstance` accessors can be documented before live storage/handlers.
- Files: likely new/extended zone helper service/tests plus docs.
- Java source to read: `ZoneInstance.getTownId`, `forEach`, `isDominionZone`, `addHandler`, and `ZoneService` handler attachment.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
| --- | --- | --- | --- | --- |
| A | Charge-all DB rollback integration test planning | repository integration tests/fixtures | Medium | Independent from region lifecycle files. |
| B | Nearby packet golden gap audit | packet tests/docs read-only unless assigned | Low | Avoid live send work. |
| C | Zone handler source audit | read-only Java zone handler files | Low | Useful before live zone callbacks. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
| --- | --- | --- | --- |
| Orchestrator | Non-live zone identity/accessor plan | new/extended zone helper service/tests, docs | Java writes, live nearby dispatch |
| Read-only Agent | Zone handler source audit | Java source reads only | all writes |

## Do Not Parallelize

- Java source files: read-only only.
- Live nearby dispatch and live `MapRegion` storage: still high risk and intentionally disabled.
- Shared zone helper files: one owner only if implementation begins.

## Continuation Context

Current unit commit message:

```text
[Phase 6][UOW-1645] Model zone capability options
```

Files changed in this unit:

- `dotnetConversion/src/Aion.GameServer/Services/WorldMapRegionZoneCapabilityService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldMapRegionZoneCapabilityServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ARM-Completion.md`

Required startup reading for the next continuation:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parallelization-strategy.md`
- `docs/parity-verification.md`
- `docs/PHASE-6-PROGRESS.md`
- Latest completion handoff, currently this file

Note: `docs/commit-conventions.md` is still missing.
