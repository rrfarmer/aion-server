# Phase 6KG Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6KF and covers Session 781.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerSummonCastSpellServiceTests|GameServerConnectionCastSpellTests|PlayerSummonSkillExecutionServiceTests"`
  - Result: Passed, 50 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1374 tests.

## Recent Work Completed

### Session 781 - Represented NPC Skill Action Target Selection

- Re-inspected Java `SkillAttackManager.skillAction` target-selection mutation.
- Added represented target-selection DTOs:
  - `PlayerSummonKnownObjectNpcSkillActionTargetSelection`;
  - `PlayerSummonKnownObjectNpcSkillActionTargetSelectionStatus`;
  - `PlayerSummonKnownObjectNpcSkillActionTargetSource`.
- Added `SelectMercenaryNpcSkillActionTarget` on `PlayerSummonSkillExecutionService`.
- Modeled:
  - skill first-target `ME` override to owner;
  - NPC target `ME`;
  - friend, hated-order, random, random-except-current, and none target attributes;
  - selected, missing-target, and not-required results;
  - whether represented metadata would set owner target.
- Kept actual `owner.setTarget`, known-list scanning, aggro-list lookup, random target selection, `PositionUtil` range, `GeoService.canSee`, controller execution, effects, and packets unwired.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.ai.manager.SkillAttackManager.skillAction` target selection block | `PlayerSummonSkillExecutionService.SelectMercenaryNpcSkillActionTarget` | AI Skill Target Projection | Partial | Regression Tested | Needs Verification | Represents target-selection outcomes and first-target self override; does not mutate live owner target. |
| `com.aionemu.gameserver.skillengine.properties.FirstTargetAttribute.ME` | `skillFirstTargetIsSelf` input / `ActionTargetSource.Owner` | Skill Property Gate Projection | Partial | Regression Tested | Needs Verification | Models Java override where skill first-target `ME` selects owner before NPC target attribute logic. |
| `com.aionemu.gameserver.model.templates.npcskill.NpcSkillTargetAttribute` | `PlayerSummonKnownObjectNpcSkillTargetAttribute` plus action target selection sources | Enum / Target Projection | Partial | Regression Tested | Needs Verification | Represents all Java enum branches as source metadata. Live lookup remains missing. |
| `com.aionemu.gameserver.controllers.attack.AggroTarget` used by `AggroList.getTarget` | explicit target-availability booleans for hated/random sources | Aggro Target Dependency | Not Started | Manual Only as input branches | Needs Verification | Does not query live aggro lists or random target ranges. |
| `com.aionemu.gameserver.world.geo.GeoService` / `PositionUtil` in `FRIEND` target lookup | explicit `hasFriendTarget` input | Known-List / Geometry Dependency | Not Started | Manual Only as input branch | Needs Verification | Friend target selection still lacks visible NPC scan, support/friend predicate, range geometry, geo visibility, threading, and live target mutation. |

## Tests Added Or Updated

- `PlayerSummonSkillExecutionServiceTests.SelectMercenaryNpcSkillActionTarget_ProjectsJavaSkillActionTargetMutation`
  - Validates first-target self override.
  - Validates NPC target `ME`.
  - Validates friend selected/missing.
  - Validates most/second/third hated, random, random-except-current, and `NONE`.
  - Validates whether represented metadata would set owner target.
- These tests are source-derived from Java. They do not compare against Java runtime execution, live target mutation, known-list scans, aggro-list lookup, random selection, geometry, geo visibility, reflection behavior, threading behavior, serialization behavior, date/time runtime behavior, precision/rounding behavior, or live-client validation.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 5
- Total artifacts ported or partially modeled in this handoff window: 1 represented NPC skill action target-selection slice.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 5
- Total blocked/not-started artifacts: live owner target mutation, live `SkillTemplate.Properties` binding, known-list friend scans, support/friend predicate, aggro-list lookup, random target selection, Java geometry, geo visibility, controller execution, Java runtime comparison, threading, and live-client validation.
- Estimated overall migration completion: 66%

## Remaining Risks

- Target selection is represented metadata and does not mutate a live owner target.
- Known-list friend selection, aggro-list selection, random target selection, range geometry, and geo visibility remain caller-supplied/missing.
- Live `SkillTemplate.Properties`, first-target range, target type, controller execution, effects, and packets remain unwired.
- Java RNG/shuffle, reflection, threading, serialization, date/time runtime, precision/rounding, Java runtime, geometry, and live-client behavior remain unverified.

## Next Recommended Unit of Work

Continue by adding a represented `skillAction` pre-use projection that composes target validity, aggro-range abort, abnormal/transform skill-use blocks, target-selection metadata, and controller use-skill result into Java source-order outcomes. Keep live owner target mutation, controller execution, effects, packets, Java geometry, RNG, scheduler/date-time behavior, and live-client validation explicit until supported.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6KF-Completion.md`
   - this handoff
3. Inspect Java `SkillAttackManager.skillAction`, target validation, abnormal/transform gates, target selection, and current C# summon skill execution service.
4. Inspect C# target-selection records, `SelectMercenaryNpcSkillActionTarget`, `EvaluateMercenarySkillReadiness`, target-range helpers, and execution tests.
5. Implement one narrow represented `skillAction` pre-use projection with Java breadcrumbs.
6. Add focused tests that state what is source-derived and what remains unverified.
7. Run focused tests, then full GameServer tests.
8. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
9. Create the next handoff document and commit the unit.
