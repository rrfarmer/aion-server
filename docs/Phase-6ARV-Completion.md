# Phase 6ARV Completion - Material Zone Actor Task Plan

Date: 2026-05-28
Unit of Work: UOW-1654
Status: Complete after focused unit tests

## Scope

This unit added a non-live actor/task plan for Java `ZoneCollisionMaterialActor` and `AbstractMaterialSkillActor`.

The C# code models touch/untouch transitions, task scheduling intent, periodic skill-task gates, material act condition selection, and `SkillEngine.applyEffectDirectly` metadata. It does not mutate live observers, schedule real tasks, query live game time/weather, send packets, or apply effects.

## Completed Work

- Added `WorldMapRegionMaterialZoneActorPlanService`.
- Added actor move/tick contexts, skill snapshots, move/tick plans, move/tick statuses, act condition enum, and day-time enum.
- Modeled Java collision-result touch-state transitions.
- Modeled Java `act()` scheduling side effects and `abort()` cancellation side effects.
- Modeled Java staff debug messages for touched/untouched geometry.
- Modeled Java material task guard order: frequency, touched state, spawned/dead state, player protection, and matching condition.
- Modeled `SUNNY` and `NIGHT` material act conditions.
- Modeled Java `SkillEngine.applyEffectDirectly(..., Effect.ForceType.MATERIAL_SKILL)` as metadata.
- Updated `docs/PHASE-6-PROGRESS.md` with discovery, file ownership, parity table, tests, risks, metrics, and next-unit guidance.

## Java Parity Notes

- `ZoneCollisionMaterialActor.onMoved` sets `isTouched` from whether collision results are non-empty.
- `onMoved` calls `act()` only when the state changes to touched and calls `abort()` only when it changes to untouched.
- Staff debug messages use the closest collision geometry for touch and the actor geometry for untouch.
- `AbstractMaterialSkillActor.act` schedules a fixed-rate task every 1000ms only when skills are non-empty and the creature controller lacks `ZONE_MATERIAL_ACTION`.
- `MaterialSkillTask.run` gates by previous skill frequency, touch state, spawned/dead state, player protection, matching conditions, then applies a material skill directly to the creature.
- `SUNNY` means the weather is not raining or the weather entry is in a before state.

## Validation

Ran:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldMapRegionMaterialZoneActorPlanServiceTests|FullyQualifiedName~WorldMapRegionMaterialZoneHandlerPlanServiceTests|FullyQualifiedName~WorldMapRegionMaterialZoneSerializationPlanServiceTests|FullyQualifiedName~WorldMapRegionMaterialZoneSavePlanServiceTests|FullyQualifiedName~WorldMapRegionMaterialZoneConstructionServiceTests"
```

Result: passed 26 tests.

Full game-server suite was not rerun in this unit.

## Parallel Work Discovery

| Candidate | Files / Area | Risk | Selected | Notes |
| --- | --- | --- | --- | --- |
| Material collision actor task plan | new actor-plan service/tests | Medium | Yes | Next material-zone behavior boundary after handler lifecycle metadata. |
| Charge-all DB rollback integration planning | repository integration tests | Medium | Read-only sidecar | Independent safe candidate; sidecar inspected while Orchestrator implemented actor plan. |
| Nearby packet golden gap audit | packet tests/docs | Low | No | Useful later. |
| Broader zone-handler source audit | Java zone handler files | Low | No | Useful before live callbacks. |

## File Ownership Map

| Agent | Scope | Allowed Files | Forbidden Files | Expected Output |
| --- | --- | --- | --- | --- |
| Orchestrator | Material collision actor/task helper, tests, docs, commit | `WorldMapRegionMaterialZoneActorPlanService.cs`, `WorldMapRegionMaterialZoneActorPlanServiceTests.cs`, progress/handoff docs | Java source writes, live actor mutation, unrelated services/tests | Implemented and documented UOW-1654. |
| Descartes the 2nd | Read-only charge-all DB rollback candidate audit | Read-only inspection of docs/Java/C# tests | All writes | Future-work notes only; no integration dependency for UOW-1654. |

No writable sub-agent work was spawned for UOW-1654 because selected implementation edits a new helper/test pair but shared docs remain Orchestrator-owned. The sidecar was read-only and independent.

Sidecar charge-all rollback notes:

- Relevant Java artifacts: `ItemChargeService.startChargingEquippedItems`, `ItemChargeService.chargeItems`, `CM_QUESTION_RESPONSE`, `DialogAction.CHARGE_ITEM_MULTI`/`CHARGE_ITEM_MULTI2`, and `InventoryDAO`.
- Relevant C# artifacts: `ItemChargeService.CreateChargeAllPlans`, `GameServerConnection` charge-all response handling, and `PlayerEnterWorldRepository.SaveItemChargeAllMutationAsync`.
- Suggested future gated test: `SaveItemChargeAllMutation_RollsBackPriorChargeUpdatesWhenLaterChargeUpdateFailsAgainstJavaSchema_WhenEnabled`.
- Suggested test home: `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldRepositoryDatabaseIntegrationTests.cs`.
- Requires `AION_GAMESERVER_DB_INTEGRATION=1`; normal test runs only cover disabled guards and fake-repository behavior.
- This is a C# transaction regression guard, not full Java runtime parity, because Java DAO commit boundaries appear different.

## Tests Added Or Updated

| Test | Change | Validates | Java Comparison |
| --- | --- | --- | --- |
| `CreateMovePlan_TouchTransitionSchedulesTaskAndReportsClosestGeometry` | Added | Touch-start transition, task scheduling metadata, closest-collision debug message. | Static source review of Java `ZoneCollisionMaterialActor.onMoved` and `AbstractMaterialSkillActor.act`. |
| `CreateMovePlan_UntouchTransitionAbortsTaskAndReportsMaterialGeometry` | Added | Untouch transition, task cancellation metadata, own-geometry debug message. | Static source review of Java `ZoneCollisionMaterialActor.onMoved` and `AbstractMaterialSkillActor.abort`. |
| `CreateTickPlan_HonorsJavaFrequencyTouchAndProtectionGuards` | Added | Frequency gate, not-touched skip, inactive creature skip, and protection skip. | Static source review of Java `MaterialSkillTask.run`. |
| `CreateTickPlan_SelectsFirstSkillWithMatchingConditionAndAppliesMaterialSkill` | Added | First matching condition, sunny weather-before handling, debug metadata, and material skill application metadata. | Static source review of Java condition matching and `SkillEngine.applyEffectDirectly`. |
| `CreateTickPlan_SkipsWhenNoMaterialActConditionMatches` | Added | No matching sunny/night conditions skips skill application. | Static source review of Java `matchActConditions`. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.controllers.observer.ZoneCollisionMaterialActor` | `WorldMapRegionMaterialZoneActorPlanService.CreateMovePlan` | Collision Actor Behavior Plan | Partial | Unit Tested Metadata | Partial Parity | C# models touch/untouch transitions from collision result count, staff debug text, and act/abort side-effect metadata. It does not inspect real collision results, mutate `isTouched`, or call live `act`/`abort`. |
| `com.aionemu.gameserver.controllers.observer.AbstractMaterialSkillActor` | `WorldMapRegionMaterialZoneActorPlanService.CreateTickPlan`; `CreateDiedPlan` | Periodic Skill Actor Plan | Partial | Unit Tested Metadata | Partial Parity | C# models scheduling metadata, frequency gate, touch/spawn/dead/protection guards, condition selection, death abort metadata, and skill-application metadata. It does not schedule `Future` tasks, use `AtomicReference`, or mutate creature controller tasks. |
| `com.aionemu.gameserver.model.templates.materials.MaterialSkill` | `WorldMapRegionMaterialZoneActorSkillSnapshot` | Material Skill DTO | Partial | Unit Tested Metadata | Partial Parity | C# carries id, level, frequency, and act conditions needed by actor task logic. It does not model target matching here, XML defaults, or mutable synchronized skill list behavior. |
| `com.aionemu.gameserver.model.templates.materials.MaterialActCondition` | `WorldMapRegionMaterialZoneActCondition` | Enum / Condition Boundary | Partial | Unit Tested | Partial Parity | C# models `SUNNY` and `NIGHT`, including Java's sunny-as-not-raining-or-weather-before behavior. It does not call live `GameTimeService` or `WeatherService`. |
| `com.aionemu.gameserver.services.GameTimeService` | `WorldMapRegionMaterialZoneActorTickContext.DayTime` | Time Service Boundary DTO | Not Started | Unit Tested Metadata | Needs Verification | C# receives day/night as supplied metadata. Live game-time lookup and timezone/tick behavior remain unported. |
| `com.aionemu.gameserver.services.WeatherService` | `WorldMapRegionMaterialZoneActorTickContext.WeatherName`; `WeatherIsBefore` | Weather Service Boundary DTO | Not Started | Unit Tested Metadata | Needs Verification | C# receives weather metadata and models Java string-prefix logic. Live weather lookup, null weather entry behavior, and weather timing remain unported. |
| `com.aionemu.gameserver.skillengine.SkillEngine.applyEffectDirectly` | `WorldMapRegionMaterialZoneActorTickPlan` skill-application metadata | Skill Engine Boundary | Not Started | Unit Tested Metadata | Needs Verification | C# records skill id, level, and `MATERIAL_SKILL` force type only. Live skill effect application and packet/stat side effects remain unported. |

## Remaining Risks

- Actor planning is non-live and does not mutate collision observers, `AtomicReference<Future<?>>`, creature controller tasks, or volatile `isTouched`.
- Real collision result geometry, closest-collision selection beyond supplied first name, and collision intention bytes remain unported.
- Live `ThreadPoolManager.scheduleAtFixedRate`, task cancellation, and concurrent compare-and-set behavior remain unverified.
- Live `GameTimeService`, `WeatherService`, null weather entries, and weather timing remain unported.
- Live `SkillEngine.applyEffectDirectly`, effect force handling, stat/packet side effects, and material skill conditions beyond sunny/night remain unported.
- Java synchronized skill-list iteration is represented by immutable supplied DTOs.
- Packet debug messages are metadata only; no `PacketSendUtility` parity yet.
- Live dynamic zone handlers and live C# `MapRegion`/`ZoneInstance` storage remain disabled.
- Charge-all DB rollback remains a future gap: fake-repository tests prove runtime no-mutation/no-packet behavior, not MySQL transaction rollback.
- `docs/commit-conventions.md` is still missing.

## Summary Metrics

- Total Java artifacts discovered: 7 grouped rows.
- Total artifacts ported: 1 non-live material-zone actor/task helper plus 5 focused tests.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification: 3 grouped rows explicitly marked Needs Verification; remaining rows are Partial Parity or Not Started metadata with known gaps.
- Total blocked artifacts: live collision observer mutation, `AtomicReference<Future<?>>` scheduling, `ThreadPoolManager`, concurrent task ownership, live game-time/weather services, live `SkillEngine.applyEffectDirectly`, packet debug sending, collision intention bytes, dynamic zone handlers, live C# MapRegion/ZoneInstance storage, charge-all MySQL rollback regression.
- Estimated overall migration completion: about 72%.

## Next Recommended Unit Of Work

Next best unit:

| Candidate | Files / Area | Notes |
| --- | --- | --- |
| Material time/weather boundary model | new helper/tests | Model `GameTimeService`, `WeatherService`, null weather handling, and condition edge cases for material actors. |
| Charge-all DB rollback integration regression | gated DB integration tests | Prove actual MySQL rollback/no-DB-mutation on partial charge-all save failure. |
| Nearby packet golden gap audit | packet tests/docs | Keep read-only unless adding isolated packet tests. |

## Next Work Options

## Recommended Sequential Task

- Task: add a non-live material skill condition/time/weather service boundary model.
- Why: actor task behavior now records condition decisions from supplied metadata; the next Java boundary is live time/weather lookup and condition edge cases.
- Files: likely new helper/tests plus docs.
- Java source to read: `GameTimeService`, `WeatherService`, `WeatherEntry`, `DayTime`, `MaterialActCondition`, and `AbstractMaterialSkillActor.matchActConditions`.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
| --- | --- | --- | --- | --- |
| A | Charge-all DB rollback integration test planning | repository integration tests/fixtures | Medium | Independent from material-zone actor files. |
| B | Nearby packet golden gap audit | packet tests/docs read-only unless assigned | Low | Avoid live send work. |
| C | Zone handler source audit | read-only Java zone handler files | Low | Useful before live zone callbacks. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
| --- | --- | --- | --- |
| Orchestrator | Material time/weather boundary model | new helper/tests, docs | Java writes, live actor mutation |
| Read-only Agent | Charge-all DB rollback candidate audit | read-only docs/Java/C# test inspection | all writes |

## Do Not Parallelize

- Java source files: read-only only.
- Live actor mutation, live generated-zone writes, and live nearby dispatch: still high risk and intentionally disabled.
- Shared material helper files: one owner only if implementation begins.

## Continuation Context

Current unit commit message:

```text
[Phase 6][UOW-1654] Model material zone actor task behavior
```

Files changed in this unit:

- `dotnetConversion/src/Aion.GameServer/Services/WorldMapRegionMaterialZoneActorPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldMapRegionMaterialZoneActorPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ARV-Completion.md`

Required startup reading for the next continuation:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parallelization-strategy.md`
- `docs/parity-verification.md`
- `docs/PHASE-6-PROGRESS.md`
- Latest completion handoff, currently this file

Note: `docs/commit-conventions.md` is still missing.
