# Phase 6ARU Completion - Material Zone Handler Behavior

Date: 2026-05-28
Unit of Work: UOW-1653
Status: Complete after focused unit tests

## Scope

This unit added a non-live behavior plan for Java `MaterialZoneHandler`.

The C# code models branch decisions and side-effect intent for enter/leave behavior, but it does not create observers, mutate creature controllers, run collision checks, schedule material skill tasks, send packets, or apply skills.

## Completed Work

- Added `WorldMapRegionMaterialZoneHandlerPlanService`.
- Added handler context, skill snapshot, enter plan, leave plan, race, creature-kind, skill-target, check-type, and status DTOs/enums.
- Modeled owner-race derivation from `BU_AB_DARKSP` and `BU_AB_LIGHTSP` geometry names.
- Modeled material target matching for `ALL`, `NPC`, `PLAYER`, and `PLAYER_WITH_PET`.
- Modeled observer registration side effects for matching skills.
- Modeled `CheckType.PASS` for material ids `14..16`, otherwise `TOUCH`.
- Modeled staff debug-message metadata for enter and leave.
- Modeled leave cleanup metadata for observed actor removal, observer removal, and actor abort.
- Updated `docs/PHASE-6-PROGRESS.md` with discovery, file ownership, parity table, tests, risks, metrics, and next-unit guidance.

## Java Parity Notes

- Java `MaterialZoneHandler` stores observed actors in a `ConcurrentHashMap`.
- Owner race is inferred from geometry name prefix.
- Same-race creatures are ignored for Abyss dark/light spire material zones.
- Enter behavior filters material skills through `MaterialTarget.matches`.
- Material ids `14`, `15`, and `16` use collision `CheckType.PASS`; all others use `TOUCH`.
- Java creates `ZoneCollisionMaterialActor`, adds it to the creature observe controller, stores it by creature object id, optionally sends staff debug text, then calls `actor.moved()`.
- Leave behavior removes the observed actor, removes the observer, aborts the actor, and optionally sends staff debug text.

## Validation

Ran:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldMapRegionMaterialZoneHandlerPlanServiceTests|FullyQualifiedName~WorldMapRegionMaterialZoneSerializationPlanServiceTests|FullyQualifiedName~WorldMapRegionMaterialZoneSavePlanServiceTests|FullyQualifiedName~WorldMapRegionMaterialZoneConstructionServiceTests"
```

Result: passed 21 tests.

Full game-server suite was not rerun in this unit.

## Parallel Work Discovery

| Candidate | Files / Area | Risk | Selected | Notes |
| --- | --- | --- | --- | --- |
| MaterialZoneHandler behavior plan | new handler-plan service/tests | Medium | Yes | Next material-zone side-effect boundary after serialization modeling. |
| Charge-all DB rollback integration planning | repository integration tests | Medium | No | Independent safe alternative. |
| Nearby packet golden gap audit | packet tests/docs | Low | No | Useful later. |
| Broader zone-handler source audit | Java zone handler files | Low | No | Useful before live callbacks. |

## File Ownership Map

| Agent | Scope | Allowed Files | Forbidden Files | Expected Output |
| --- | --- | --- | --- | --- |
| Orchestrator | MaterialZoneHandler behavior helper, tests, docs, commit | `WorldMapRegionMaterialZoneHandlerPlanService.cs`, `WorldMapRegionMaterialZoneHandlerPlanServiceTests.cs`, progress/handoff docs | Java source writes, live actor mutation, unrelated services/tests | Implemented and documented UOW-1653. |
| Sub-agents | None | None | All files | Not spawned because selected work was small and Orchestrator-owned. |

No sub-agent was spawned for UOW-1653 because selected work was small, self-contained, and docs remained Orchestrator-owned.

## Tests Added Or Updated

| Test | Change | Validates | Java Comparison |
| --- | --- | --- | --- |
| `CreateEnterPlan_OwnerRaceGeometrySkipsRegistration` | Added | Dark-spire geometry maps to Asmodians and skips same-race creature registration. | Static source review of Java `MaterialZoneHandler` constructor and owner-race guard. |
| `CreateEnterPlan_FiltersMaterialSkillsAndUsesPassForLandingShieldMaterials` | Added | Matching skill ids, `PASS` check type, observer side effects, and staff debug metadata. | Static source review of Java target matching and material id `14..16` branch. |
| `CreateEnterPlan_NoMatchingSkillsSkipsObserverRegistration` | Added | No matching skills returns without observer side effects. | Static source review of Java matching-skills empty guard. |
| `CreateLeavePlan_RemovesObservedActorAndReportsStaffDebugMessage` | Added | Observed actor removal, observer removal, abort metadata, and staff debug leave metadata. | Static source review of Java `MaterialZoneHandler.onLeaveZone`. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.world.zone.handler.MaterialZoneHandler` | `WorldMapRegionMaterialZoneHandlerPlanService` | Zone Handler Behavior Plan | Partial | Unit Tested Metadata | Partial Parity | C# models owner-race guard, material target matching, observer registration metadata, PASS/TOUCH selection, staff debug metadata, and leave cleanup metadata. It does not create live observers, mutate creature controllers, or run material skill tasks. |
| `com.aionemu.gameserver.model.templates.materials.MaterialTarget` | `WorldMapRegionMaterialZoneSkillTarget` | Enum / Predicate Boundary | Partial | Unit Tested | Partial Parity | C# models `ALL`, `NPC`, `PLAYER`, and `PLAYER_WITH_PET` matching, including summon-with-master behavior. It does not use Java runtime type checks. |
| `com.aionemu.gameserver.controllers.observer.ZoneCollisionMaterialActor` | `WorldMapRegionMaterialZoneCollisionCheckType`; enter side effects | Observer Boundary | Not Started | Unit Tested Metadata | Needs Verification | C# records actor creation, `actor.moved`, and check-type selection only. Collision results, touch/untouch transitions, `act`, and `abort` runtime behavior remain unported. |
| `com.aionemu.gameserver.controllers.observer.AbstractMaterialSkillActor` | Handler enter/leave side-effect metadata | Periodic Skill Actor Boundary | Not Started | Unit Tested Metadata | Needs Verification | C# does not schedule tasks, evaluate weather/time conditions, check protection/dead/spawned state, or invoke `SkillEngine.applyEffectDirectly`. |
| `com.aionemu.gameserver.configs.main.GeoDataConfig.GEO_MATERIALS_SHOWDETAILS` | `WorldMapRegionMaterialZoneHandlerContext.ShowDetailsToStaff` | Config Boundary DTO | Partial | Unit Tested Metadata | Needs Verification | C# records debug message intent only. Packet send behavior and staff/player runtime checks remain unported. |
| `com.aionemu.gameserver.world.zone.handler.ZoneHandler` | `CreateEnterPlan`; `CreateLeavePlan` | Interface Boundary | Partial | Unit Tested Metadata | Partial Parity | C# models the enter/leave contract for material zones only. Other zone handlers remain separate work. |

## Remaining Risks

- Handler planning is non-live and does not mutate creature observe controllers or `ConcurrentHashMap` state.
- `ZoneCollisionMaterialActor.onMoved`, collision results, touch/untouch transitions, and material skill task scheduling remain unported.
- `AbstractMaterialSkillActor` weather/time conditions, frequency timing, spawned/dead/protection guards, and `SkillEngine.applyEffectDirectly` remain unported.
- Packet debug messages are metadata only; no `PacketSendUtility` parity yet.
- Java runtime type checks are represented by supplied DTO enums, not real class hierarchy checks.
- Live dynamic zone handlers and live C# `MapRegion`/`ZoneInstance` storage remain disabled.
- Charge-all DB rollback remains a future gap: fake-repository tests prove runtime no-mutation/no-packet behavior, not MySQL transaction rollback.
- `docs/commit-conventions.md` is still missing.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped rows.
- Total artifacts ported: 1 non-live material-zone handler behavior helper plus 4 focused tests.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification: 3 grouped rows explicitly marked Needs Verification; remaining rows are Partial Parity or Not Started metadata with known gaps.
- Total blocked artifacts: live observer mutation, `ConcurrentHashMap` observed state, collision result transitions, periodic material skill tasks, weather/time material conditions, `SkillEngine.applyEffectDirectly`, packet debug sending, live Java type hierarchy checks, dynamic zone handlers, live C# MapRegion/ZoneInstance storage, charge-all MySQL rollback regression.
- Estimated overall migration completion: about 72%.

## Next Recommended Unit Of Work

Next best unit:

| Candidate | Files / Area | Notes |
| --- | --- | --- |
| Material collision actor task plan | new helper/tests | Model touch/untouch transitions, condition matching, frequency gates, and `SkillEngine.applyEffectDirectly` metadata. |
| Charge-all DB rollback integration regression | gated DB integration tests | Prove actual MySQL rollback/no-DB-mutation on partial charge-all save failure. |
| Nearby packet golden gap audit | packet tests/docs | Keep read-only unless adding isolated packet tests. |

## Next Work Options

## Recommended Sequential Task

- Task: add a non-live `ZoneCollisionMaterialActor` / `AbstractMaterialSkillActor` task plan.
- Why: handler enter/leave now records actor lifecycle intent; the next Java behavior is collision touch/untouch and periodic material skill application.
- Files: likely new helper/tests plus docs.
- Java source to read: `ZoneCollisionMaterialActor`, `AbstractMaterialSkillActor`, `MaterialActCondition`, `GameTimeService`, `WeatherService`, and `SkillEngine.applyEffectDirectly`.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
| --- | --- | --- | --- | --- |
| A | Charge-all DB rollback integration test planning | repository integration tests/fixtures | Medium | Independent from region lifecycle files. |
| B | Nearby packet golden gap audit | packet tests/docs read-only unless assigned | Low | Avoid live send work. |
| C | Zone handler source audit | read-only Java zone handler files | Low | Useful before live zone callbacks. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
| --- | --- | --- | --- |
| Orchestrator | Material collision actor task plan | new helper/tests, docs | Java writes, live actor mutation |
| Read-only Agent | Material actor source audit | Java source reads only | all writes |

## Do Not Parallelize

- Java source files: read-only only.
- Live actor mutation, live generated-zone writes, and live nearby dispatch: still high risk and intentionally disabled.
- Shared material helper files: one owner only if implementation begins.

## Continuation Context

Current unit commit message:

```text
[Phase 6][UOW-1653] Model material zone handler behavior
```

Files changed in this unit:

- `dotnetConversion/src/Aion.GameServer/Services/WorldMapRegionMaterialZoneHandlerPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldMapRegionMaterialZoneHandlerPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ARU-Completion.md`

Required startup reading for the next continuation:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parallelization-strategy.md`
- `docs/parity-verification.md`
- `docs/PHASE-6-PROGRESS.md`
- Latest completion handoff, currently this file

Note: `docs/commit-conventions.md` is still missing.
