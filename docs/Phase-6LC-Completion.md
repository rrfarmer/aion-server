# Phase 6LC Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6LB and covers Session 803.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerSummonSkillExecutionServiceTests|StaticDataNpcSkillTests"`
  - Result: Passed, 43 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1393 tests.

## Recent Work Completed

### Session 803 - Represented Carved-Signet Condition Readiness

- Re-inspected Java `NpcSkillTemplateEntry.hasCarvedSignet`, `SignetBurstEffect.getSignet`, `EffectController.getAbnormalEffect`, and `Effect.getSkillLevel`.
- Added represented `PlayerSummonKnownObjectNpcSkillCarvedSignetState` metadata for target abnormal signet stacks and levels.
- Extended `EvaluateMercenaryNpcSkillConditionReadiness(PlayerSummonKnownObjectNpcSkillConditionMetadata, ...)` with represented skill-template signet-burst stack input.
- Modeled unsupported missing skill-template signet input, missing target, target creature/death gates, condition thresholds 0 through 4, strict `Effect.getSkillLevel() > signetLvl` behavior, case-sensitive stack matching, empty skill effects, and absent target effects.
- Kept live `SkillTemplate.getEffects()`, JAXB effect-template polymorphism, live `SignetBurstEffect`, locked `EffectController` maps, real `Effect` instances, abnormal/effect mutation, signet burst damage removal, packets, scheduler/date-time behavior, threading, serialization, and live-client validation unwired.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.skill.NpcSkillTemplateEntry.hasCarvedSignet` | `PlayerSummonSkillExecutionService.EvaluateMercenaryNpcSkillConditionReadiness(PlayerSummonKnownObjectNpcSkillConditionMetadata, ...)` carved-signet branch | Service | Partial | Regression Tested | Needs Verification | C# models represented carved-signet threshold checks for all five condition variants. It does not inspect live Java `SkillTemplate` effects, live target `EffectController`, or compare runtime Java behavior. |
| `com.aionemu.gameserver.model.templates.npcskill.NpcSkillCondition` carved-signet values | `PlayerSummonKnownObjectNpcSkillCondition.TargetHasCarvedSignet*` | Enum / Condition | Partial | Regression Tested | Needs Verification | C# handles represented thresholds 0..4 using Java's strict greater-than level rule. Runtime XML enum validation and live condition execution remain unverified. |
| `com.aionemu.gameserver.skillengine.effect.SignetBurstEffect` | `skillTemplateSignetBurstStacks` represented input | Effect-template Dependency | Not Started | Regression Tested as metadata only | Needs Verification | C# consumes represented signet stack names but does not parse effect templates, model JAXB polymorphism, `getSignetlvl`, damage calculation, sub-effects, or signet removal side effects. |
| `com.aionemu.gameserver.controllers.effect.EffectController.getAbnormalEffect` | `PlayerSummonKnownObjectNpcSkillCarvedSignetState` list on condition target | Effect Controller Dependency | Not Started | Regression Tested as metadata only | Needs Verification | C# represents abnormal-effect stack lookup with case-sensitive string matching. It does not use locked maps, live effect lifecycle, concurrent mutation, passive/abnormal map split, or packet fanout. |
| `com.aionemu.gameserver.skillengine.model.Effect.getSkillLevel` | `PlayerSummonKnownObjectNpcSkillCarvedSignetState.SkillLevel` | Effect State Dependency | Partial | Regression Tested | Needs Verification | C# compares represented skill levels using Java's strict `>` rule. Live `Effect` objects, skill stack level, carved signet counters, precision, serialization, and removal behavior remain unverified. |
| `com.aionemu.gameserver.model.gameobjects.Creature` / `CreatureLifeStats` | `PlayerSummonKnownObjectNpcSkillConditionTarget.IsCreature`, `IsDead`, `IsAboutToDie` | Creature / Stats Dependency | Partial | Regression Tested as metadata only | Needs Verification | C# models the target creature/death gates used by `hasCarvedSignet`. Live target identity, life stats, HP precision/rounding, threading, and packet side effects remain unverified. |

## Tests Added Or Updated

- `PlayerSummonSkillExecutionServiceTests.EvaluateMercenaryNpcSkillConditionReadiness_ProjectsCarvedSignetThresholds`
  - Validates unsupported missing skill-template signet input.
  - Validates missing target handling.
  - Validates base through level V strict threshold behavior.
  - Validates case-sensitive stack matching.
  - Validates empty skill effects, dead target, and non-creature target behavior.
- These tests are source-derived from Java. They do not compare against Java runtime execution, JAXB effect-template parsing, live effect-controller map behavior, concurrent mutation, reflection behavior, threading behavior, serialization behavior, date/time behavior, precision/rounding behavior, packets, or live-client validation.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 6
- Total artifacts ported or partially modeled in this handoff window: 1 represented carved-signet condition readiness slice.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 6
- Total blocked/not-started artifacts: live `SkillTemplate.getEffects`, JAXB effect-template polymorphism, live `SignetBurstEffect`, locked `EffectController` maps, real `Effect` objects, effect lifecycle/removal, signet burst damage/sub-effects, target object identity, life stats, packets, persistence, threading/serialization, reflection behavior, precision/rounding, and live-client validation.
- Estimated overall migration completion: 66%

## Remaining Risks

- Carved-signet readiness is represented by supplied skill-template stack names and target signet states, not by live `SkillTemplate` or `EffectController` inspection.
- Java JAXB effect-template polymorphism, locked effect maps, effect lifecycle/removal, signet damage/sub-effect behavior, target identity, threading, serialization, packets, persistence, date/time behavior, precision/rounding, and live-client behavior remain missing or unverified.

## Next Recommended Unit of Work

Continue NPC skill readiness parity by loading/projecting represented `SignetBurstEffect` stack names from `SkillTemplateTable` into the NPC skill candidate condition-readiness path, so carved-signet checks can consume represented skill-template data instead of caller-supplied stack lists. Keep JAXB effect-template polymorphism, live effect controllers, effect mutation/removal, packets, threading, serialization, and live-client validation explicit until supported.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6LB-Completion.md`
   - this handoff
3. Inspect C# `SkillTemplateTable` / `SkillTemplateSummary` XML loading and Java `SkillTemplate.getEffects().getEffects()` / `SignetBurstEffect`.
4. Decide the smallest represented effect-template metadata shape needed to carry signet-burst stack names.
5. Wire the represented skill-template signet stacks into NPC skill condition-readiness projection without claiming live effect parity.
6. Add focused tests that state what is source-derived and what remains unverified.
7. Run focused tests, then full GameServer tests.
8. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
9. Create the next handoff document and commit the unit.
