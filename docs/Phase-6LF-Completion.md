# Phase 6LF Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6LE and covers Session 806.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerSummonSkillExecutionServiceTests|StaticDataNpcSkillTests"`
  - Result: Passed, 45 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1395 tests.

## Recent Work Completed

### Session 806 - Represented Current-Target Selection Preview

- Re-inspected `PreviewMercenaryNextNpcSkillSelectionFromCandidateMetadata`, candidate-list projection, and Java `NpcSkillTemplateEntry.conditionReady` current-target behavior.
- Added `PreviewMercenaryNextNpcSkillSelectionFromRepresentedCurrentTarget`.
- Added an internal adapter that applies represented current-target facts to candidate metadata lacking an explicit condition target before the existing candidate-list and selection pipeline runs.
- Modeled high-level static candidate metadata selection with represented current-target facts, current target abnormal-state facts, target object id and creature/death/HP/range/relation/geo facts flowing through condition-target metadata, and manual candidate `ConditionTarget` preservation.
- Kept live `creature.getTarget`, real current-target object identity, live known-list target lookup, relation/geo service calls, effect-controller reads, target mutation, packets, threading, serialization, and live-client validation unwired.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.ai.manager.SkillAttackManager.chooseNextSkill` | `PlayerSummonSkillExecutionService.PreviewMercenaryNextNpcSkillSelectionFromRepresentedCurrentTarget` | Service | Partial | Regression Tested | Needs Verification | C# can run represented selection preview from static candidates plus represented current-target facts. It does not run live AI, mutate queued skill state, call live Java services, or compare runtime behavior. |
| `com.aionemu.gameserver.model.skill.NpcSkillTemplateEntry.conditionReady` | `ApplyRepresentedCurrentTarget` plus `EvaluateMercenaryNpcSkillConditionReadiness` | Service | Partial | Regression Tested | Needs Verification | C# injects represented current-target facts before condition evaluation when candidate metadata lacks an explicit target. Live `creature.getTarget`, target identity, effect-controller reads, and target mutation remain missing. |
| `com.aionemu.gameserver.model.gameobjects.VisibleObject` / `Creature` | `PlayerSummonKnownObject` current-target input | DTO / World Object Dependency | Partial | Regression Tested | Needs Verification | C# consumes represented target facts for object id, creature mapping, death, hp, abnormal state, class, flying, and carved signets. Live object model, life stats, Java HP precision/rounding, threading, and serialization remain unverified. |
| `com.aionemu.gameserver.controllers.effect.EffectController` | represented `PlayerSummonKnownObject.AbnormalState` / `CarvedSignets` through preview pipeline | Effect Controller Dependency | Partial | Regression Tested as metadata only | Needs Verification | C# passes represented effect facts into condition checks. Locked effect maps, live effects, effect lifecycle/removal, packets, threading, and serialization remain missing. |
| `com.aionemu.gameserver.services.TribeRelationService` / `GeoService` | represented current-target relation and geo booleans | Service Dependency | Not Started | Regression Tested as metadata only | Needs Verification | C# still consumes precomputed booleans for relation/geo facts. Live tribe data, Panesterra rules, geo maps/z offsets, instance ids, and live-client validation remain missing. |

## Tests Added Or Updated

- `PlayerSummonSkillExecutionServiceTests.PreviewMercenaryNextNpcSkillSelectionFromRepresentedCurrentTarget_AppliesTargetFacts`
  - Validates high-level selection from static candidate metadata using represented current-target abnormal facts.
  - Validates target metadata propagation.
  - Validates represented relation/geo facts.
  - Validates preservation of an explicitly supplied candidate condition target.
- These tests are source-derived from Java. They do not compare against Java runtime execution, live AI selection, live target lookup, relation/geo service behavior, effect-controller map behavior, reflection behavior, threading behavior, serialization behavior, date/time behavior, precision/rounding behavior, packets, or live-client validation.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 5
- Total artifacts ported or partially modeled in this handoff window: 1 represented current-target high-level selection preview slice.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 5
- Total blocked/not-started artifacts: live `SkillAttackManager`, live `creature.getTarget`, real current-target object identity, live known-list target lookup, live `Creature`/`LifeStats`, Java HP precision/rounding, live `EffectController`, live `TribeRelationService`, live `GeoService`, target mutation, packets, threading/serialization, reflection behavior, date/time behavior, and live-client validation.
- Estimated overall migration completion: 66%

## Remaining Risks

- The new preview entry point still requires represented current-target facts supplied by the caller; it does not obtain them from live AI or `creature.getTarget`.
- Live object identity, known-list state, current-target mutation, relation/geo services, effect-controller lifecycle, Java HP precision/rounding, packets, persistence, threading, serialization, reflection behavior, date/time behavior, and live-client behavior remain missing or unverified.

## Next Recommended Unit of Work

Continue NPC skill readiness parity by adding represented `HELP_FRIEND` known-list candidate injection to a higher-level preview path, so caller-supplied represented known-list facts can evaluate support/friend target search without manually preparing each candidate. Keep live `KnownList.findObject`, tribe relation service, geo service, target mutation, threading, serialization, packets, and live-client validation explicit until supported.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6LE-Completion.md`
   - this handoff
3. Inspect `HELP_FRIEND` readiness, `ProjectMercenaryNpcSkillHelpFriendCandidate`, and `PreviewMercenaryNextNpcSkillSelectionFromRepresentedCurrentTarget`.
4. Add one represented high-level preview path or overload that can accept known-list candidate facts for `HELP_FRIEND`.
5. Keep unsupported live `KnownList.findObject`, tribe relation, geo, target mutation, and live AI behavior explicit.
6. Add focused tests that state what is source-derived and what remains unverified.
7. Run focused tests, then full GameServer tests.
8. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
9. Create the next handoff document and commit the unit.
