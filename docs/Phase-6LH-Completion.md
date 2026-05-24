# Phase 6LH Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6LG and covers Session 808.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerSummonSkillExecutionServiceTests|StaticDataNpcSkillTests"`
  - Result: Passed, 47 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1397 tests.

## Recent Work Completed

### Session 808 - Represented Target Invalidation and Delay

- Re-inspected Java `SkillAttackManager.skillAction`, `targetTooFar`, and `getNpcSkillEntryIfNotTooFarAway`.
- Added a represented-current-target overload of `EvaluateMercenaryTargetRange`.
- Modeled target-range bypass for non-creature-target modes, missing/non-creature/dead/invisible/cannot-see/out-of-range current-target invalidation, AREA range bypass, Java 5000 ms next-skill-delay metadata, and delay storage through the existing `ApplyMercenaryTargetRangeDelay` boundary.
- Kept live `owner.getTarget`, `owner.canSee`, `PositionUtil.isInRange`, first-target properties, target identity, current target mutation, packets, threading, serialization, and live-client validation unwired.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.ai.manager.SkillAttackManager.targetTooFar` | `PlayerSummonSkillExecutionService.EvaluateMercenaryTargetRange(PlayerSummonKnownObject, PlayerSummonKnownObjectSkillTargetMode, PlayerSummonKnownObject?, ...)` | Service | Partial | Regression Tested | Needs Verification | C# evaluates represented current-target invalidation states and range outcomes. It does not call live `DataManager.SKILL_DATA`, `Properties`, `owner.getTarget`, `owner.canSee`, or `PositionUtil.isInRange`. |
| `com.aionemu.gameserver.ai.manager.SkillAttackManager.getNpcSkillEntryIfNotTooFarAway` | `PlayerSummonKnownObjectTargetRangeReadiness.NextSkillDelayMilliseconds` plus `ApplyMercenaryTargetRangeDelay` | Service | Partial | Regression Tested | Needs Verification | C# preserves the represented 5000 ms next-skill-delay path and storage boundary. It does not mutate live `NpcGameStats` or compare runtime scheduler/date-time behavior. |
| `com.aionemu.gameserver.ai.manager.SkillAttackManager.skillAction` | represented target invalidation feeding existing `PreviewMercenaryNpcSkillAction` / range delay helpers | Service | Partial | Regression Tested | Needs Verification | C# models target invalidation facts used around skill action, but live `AISubState`, `AIEventType`, cast abort, `CreatureController.useSkill`, and packets remain missing. |
| `com.aionemu.gameserver.model.gameobjects.VisibleObject` / `Creature` | `PlayerSummonKnownObject` represented current target | DTO / World Object Dependency | Partial | Regression Tested | Needs Verification | C# consumes represented creature/death/visibility state. Live object identity, template identity, world coordinates, life stats, Java HP precision/rounding, threading, and serialization remain unverified. |
| `com.aionemu.gameserver.utils.PositionUtil` | represented `isInRange` input | Utility Dependency | Not Started | Regression Tested as metadata only | Needs Verification | C# accepts caller-provided range result and models AREA bypass. Java coordinate math, first-target range zero/`Integer.MAX_VALUE` handling, float precision, collision, and geo effects remain unverified. |
| `com.aionemu.gameserver.model.gameobjects.VisibleObject.canSee` / `Creature.canSee` | represented `canSeeTarget` and `IsVisible` facts | Visibility Dependency | Not Started | Regression Tested as metadata only | Needs Verification | C# consumes represented visibility/can-see facts. Live visibility, known-list membership, object concealment, polymorphic can-see rules, threading, and packets remain missing. |

## Tests Added Or Updated

- `PlayerSummonSkillExecutionServiceTests.EvaluateMercenaryTargetRange_ProjectsRepresentedCurrentTargetInvalidation`
  - Validates non-creature-target bypass.
  - Validates missing, non-creature, dead, invisible, cannot-see, and out-of-range represented current-target invalidation.
  - Validates AREA range bypass.
  - Validates 5000 ms delay metadata and delay storage through `ApplyMercenaryTargetRangeDelay`.
- These tests are source-derived from Java. They do not compare against Java runtime execution, live current-target lookup, live visibility, `PositionUtil`, scheduler/date-time behavior, reflection behavior, threading behavior, serialization behavior, packets, or live-client validation.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 6
- Total artifacts ported or partially modeled in this handoff window: 1 represented target invalidation/range-delay slice.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 6
- Total blocked/not-started artifacts: live `SkillAttackManager`, live `owner.getTarget`, live `owner.canSee`, live `PositionUtil.isInRange`, live skill `Properties`, `NpcGameStats.setNextSkillDelay`, AI substate/events, cast abort, `CreatureController.useSkill`, packets, Java date/time scheduling, threading/serialization, reflection behavior, precision/rounding, persistence, live object identity, and live-client validation.
- Estimated overall migration completion: 66%

## Remaining Risks

- Target invalidation still uses represented facts supplied by callers; it does not inspect live current targets, live skill properties, real range math, or live visibility.
- Live AI substate/events, cast aborts, controller skill use, `NpcGameStats.setNextSkillDelay`, Java date/time scheduling, packets, persistence, threading, serialization, reflection behavior, precision/rounding, and live-client behavior remain missing or unverified.

## Next Recommended Unit of Work

Continue NPC skill action parity by modeling represented `skillAction` AI side effects for target give-up, target-too-far cast abort, blocked after-use, failed use, and successful target-setting/use-skill outcomes as a higher-level action-result object. Keep live `AISubState`, `AIEventType`, `CreatureController.abortCast/useSkill`, packets, threading, serialization, and live-client validation explicit until supported.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6LG-Completion.md`
   - this handoff
3. Inspect Java `SkillAttackManager.skillAction` and existing C# `PreviewMercenaryNpcSkillAction`.
4. Add a represented higher-level action-result object or helper for Java AI side effects.
5. Keep unsupported live AI substate/events, controller abort/use, packets, threading, serialization, and live-client behavior explicit.
6. Add focused tests that state what is source-derived and what remains unverified.
7. Run focused tests, then full GameServer tests.
8. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
9. Create the next handoff document and commit the unit.
