# Phase 6 Session 1678 Completion - Stagger/Stumble Calculate Planner

Date: 2026-05-28
Unit of Work: UOW-1678
Status: Complete

## Scope

Add a non-live calculate-phase planner for Java `StaggerEffect.calculate` and `StumbleEffect.calculate`, covering abnormal pre-checks, base-calculate gating, sub-effect typing, heading/angle probe math, and the `GeoService.getClosestCollision` dependency.

## Completed Work

- Added `dotnetConversion/src/Aion.GameServer/Services/StaggerStumbleCalculatePlanService.cs`.
- Added planner contracts:
  - `StaggerStumbleCalculatePlanStatus`
  - `GeoCollisionSnapshot`
  - `StaggerStumbleCalculatePlanInput`
  - `StaggerStumbleCalculatePlan`
- Modeled Java calculate ordering metadata for:
  - existing forced-move abnormal pre-checks
  - `EffectTemplate.calculate` success/failure gate as input
  - non-player sub-effect type intent
  - heading toward effected using `PositionUtilService.GetHeadingTowards`
  - Java-shaped heading-to-angle conversion
  - two-meter collision probe point
  - `GeoService.getClosestCollision` output as supplied snapshot
- Added focused tests in `dotnetConversion/tests/Aion.GameServer.Tests/StaggerStumbleCalculatePlanServiceTests.cs`.

## Validation

Executed:

`dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~StaggerStumbleCalculatePlanServiceTests|FullyQualifiedName~PositionUtilServiceTests|FullyQualifiedName~ForcedMoveStartEffectPlanServiceTests"`

Result:

- 38 tests passed.
- Build succeeded.

## Java Artifacts Reviewed

- `com.aionemu.gameserver.skillengine.effect.StaggerEffect.calculate`
- `com.aionemu.gameserver.skillengine.effect.StumbleEffect.calculate`
- `com.aionemu.gameserver.utils.PositionUtil.getHeadingTowards`
- `com.aionemu.gameserver.utils.PositionUtil.convertHeadingToAngle`
- `com.aionemu.gameserver.world.geo.GeoService.getClosestCollision`
- `com.aionemu.gameserver.skillengine.effect.EffectTemplate.calculate`

## Migration Parity Table - UOW-1678

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.skillengine.effect.StaggerEffect.calculate` | `Aion.GameServer.Services.StaggerStumbleCalculatePlanService` | Effect Calculate Boundary | Partial | Unit Tested | Partial Parity | Models abnormal pre-checks, external base-calculate result, non-player sub-effect typing, heading/angle conversion, 2m collision probe, and target-location assignment when a collision result is supplied. It does not execute `EffectTemplate.calculate`, live effect state, `GeoService`, or live `effect.setTargetLoc`. |
| `com.aionemu.gameserver.skillengine.effect.StumbleEffect.calculate` | `Aion.GameServer.Services.StaggerStumbleCalculatePlanService` | Effect Calculate Boundary | Partial | Unit Tested | Partial Parity | Models the same calculate path with `STUMBLE_RESISTANCE`, `SpellStatus.STUMBLE`, and `SubEffectType.STUMBLE`. Live resistance calculation, abnormal checks against a real effect controller, geo collision, and target-location mutation remain unported. |
| `com.aionemu.gameserver.utils.PositionUtil.getHeadingTowards` / `convertHeadingToAngle` | `Aion.GameServer.Services.PositionUtilService.GetHeadingTowards`; `ConvertHeadingToAngle` | Utility Dependency | Complete utility, Partial workflow | Regression Tested | Partial Parity | Planner reuses existing source-derived heading helpers. Tests cover deterministic axis vectors, but this unit did not add Java runtime vector comparison or unusual signed-byte/precision edge cases. |
| `com.aionemu.gameserver.world.geo.GeoService.getClosestCollision` | `StaggerStumbleCalculatePlan.ShouldRequestGeoCollision`; `GeoCollisionSnapshot` | Geo Boundary | Not Started | Unit Tested boundary only | Needs Verification | C# records the collision probe point and requires a supplied collision snapshot before target location is planned. It does not run the Java/C# geo engine or verify collision behavior. |
| `com.aionemu.gameserver.skillengine.effect.EffectTemplate.calculate` | `StaggerStumbleCalculatePlanInput.BaseCalculateSucceeded` | Effect Base Boundary | Not Started | Unit Tested boundary only | Needs Verification | C# treats base calculate success/failure as an input gate. Resistance formulas, skill accuracy, stat containers, spell status side effects, and randomness remain outside this unit. |

## Tests Added

| Test Name | What It Validates | Java-Equivalent Evidence | Test Type | Limitations |
|---|---|---|---|---|
| `CreatePlan_BlocksWhenAnyForcedMoveAbnormalAlreadyExistsLikeJava` | Existing forced-move abnormal state blocks geo and target planning. | Reviewed Java abnormal pre-checks | Unit | No live effect-controller query |
| `CreatePlan_BlocksWhenBaseCalculateFailsLikeJavaResistanceGate` | Failed base-calculate input blocks geo and target planning. | Reviewed Java `super.calculate` gate | Unit | Does not execute actual `EffectTemplate.calculate` |
| `CreatePlan_ForStaggerNpcSubEffect_RequestsTwoMeterGeoProbeAndSetsSubEffectType` | `STAGGER` sub-effect type and two-meter probe point. | Reviewed Java sub-effect branch and movement math | Unit | No live `GeoService` |
| `CreatePlan_ForStumblePlayer_DoesNotSetSubEffectTypeButKeepsTargetLocation` | Player sub effects skip sub-effect type but keep target location. | Reviewed Java player branch | Unit | No live effect mutation |
| `CreatePlan_WithoutCollisionResultRecordsGeoDependencyBeforeTargetLocation` | Planner records `NeedsGeoCollision` without collision output. | Java `GeoService.getClosestCollision` dependency | Unit | Java live path calls GeoService directly |

## Risks / Gaps

- No live `StaggerEffect` or `StumbleEffect` calculate integration was added.
- `EffectTemplate.calculate`, stat resistance formulas, skill result behavior, and spell-status side effects are represented only as an input boolean.
- `GeoService.getClosestCollision` behavior is not implemented or runtime-compared in this unit.
- Position math has source-derived unit coverage but no Java runtime vector/golden comparison for this exact calculate path.
- Stumble's skill-specific no-send TODO remains unmodeled for start-effect packet planning.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/StaggerStumbleCalculatePlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/StaggerStumbleCalculatePlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1678-Completion.md`
- `docs/Phase-6-Session-1678-Handoff.md`
