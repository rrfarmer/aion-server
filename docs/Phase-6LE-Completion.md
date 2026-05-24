# Phase 6LE Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6LD and covers Session 805.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerSummonSkillExecutionServiceTests|StaticDataNpcSkillTests"`
  - Result: Passed, 44 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1394 tests.

## Recent Work Completed

### Session 805 - Represented Condition Target Fact Bridge

- Re-inspected represented `PlayerSummonKnownObject`, `ProjectMercenaryNpcSkillConditionTarget`, `HELP_FRIEND` candidate metadata, and carved-signet readiness inputs against Java `NpcSkillTemplateEntry.conditionReady`.
- Extended represented `PlayerSummonKnownObject` with target facts needed by Java condition readiness: visibility, creature-ness, dead/about-to-die state, HP percentage, flying state, physical-class flag, and active carved-signet effect states.
- Added a known-object overload of `ProjectMercenaryNpcSkillConditionTarget`.
- Added `ProjectMercenaryNpcSkillHelpFriendCandidate`.
- Modeled current target object id projection, represented Creature versus non-creature mapping, abnormal-state projection, HP/range/visibility/death/relation/geo facts used by `HELP_FRIEND`, and active carved-signet levels used by carved-signet readiness.
- Kept live current-target lookup, live `KnownObject`/`VisibleObject` identity, live `Creature` and `LifeStats`, live `EffectController`, real player class lookup, live `TribeRelationService`, live `GeoService`, target mutation, packets, threading, serialization, and live-client validation unwired.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.skill.NpcSkillTemplateEntry.conditionReady` | `PlayerSummonSkillExecutionService.ProjectMercenaryNpcSkillConditionTarget(PlayerSummonKnownObject, ...)` and `EvaluateMercenaryNpcSkillConditionReadiness` | Service | Partial | Regression Tested | Needs Verification | C# now has a represented bridge from known-object target facts into condition readiness. It does not read live `creature.getTarget()`, live object identity, or runtime Java behavior. |
| `com.aionemu.gameserver.model.gameobjects.VisibleObject` | `Aion.GameServer.Model.GameObjects.PlayerSummonKnownObject` target-fact fields | World Object DTO | Partial | Regression Tested | Needs Verification | C# carries represented object id and visibility. Live `VisibleObject`, template identity, world/instance coordinates, visibility recalculation, reflection, threading, and serialization remain unverified. |
| `com.aionemu.gameserver.model.gameobjects.Creature` | `PlayerSummonKnownObject.IsCreature`, `IsDead`, `IsAboutToDie`, `HpPercentage`, `IsFlying`, `IsPhysicalClass` | Creature DTO | Partial | Regression Tested | Needs Verification | C# carries represented creature/life/class/flying facts for readiness projections. Live `Creature`, `LifeStats`, Java HP rounding/precision, player-class lookup, object identity, threading, and packets remain unverified. |
| `com.aionemu.gameserver.controllers.effect.EffectController` | `PlayerSummonKnownObject.AbnormalState` and `CarvedSignets` | Effect Controller DTO | Partial | Regression Tested as metadata only | Needs Verification | C# carries represented abnormal bitmasks and carved-signet states. It does not inspect locked effect maps, live `Effect` instances, effect lifecycle, mutation/removal, reflection, threading, serialization, or packet fanout. |
| `com.aionemu.gameserver.world.knownlist.KnownObject` | `ProjectMercenaryNpcSkillHelpFriendCandidate(PlayerSummonKnownObject, ...)` | Known-list Adapter | Partial | Regression Tested | Needs Verification | C# converts represented known-object facts into `HELP_FRIEND` candidate metadata. Live known-list traversal, concurrent mutation, Java map ordering, relation lookup, geo lookup, and target mutation remain unverified. |
| `com.aionemu.gameserver.services.TribeRelationService` / `GeoService` | represented `isSupport`, `isFriend`, and `geoCanSee` inputs | Service Dependency | Not Started | Regression Tested as metadata only | Needs Verification | C# still consumes precomputed booleans. Tribe data, Panesterra rules, line-of-sight maps, z offsets, instance ids, threading, and live-client behavior remain missing. |

## Tests Added Or Updated

- `PlayerSummonSkillExecutionServiceTests.ProjectMercenaryNpcSkillConditionTarget_AdaptsRepresentedKnownObjectFacts`
  - Validates represented known-object projection into condition-target metadata.
  - Validates `HELP_FRIEND` candidate metadata.
  - Validates abnormal, flying, class, death, HP, range, relation, and geo facts.
  - Validates carved-signet target-state handoff.
  - Validates non-creature visible-object mapping.
- These tests are source-derived from Java. They do not compare against Java runtime execution, live current-target lookup, live effect-controller map behavior, live relation/geo behavior, reflection behavior, threading behavior, serialization behavior, date/time behavior, precision/rounding behavior, packets, or live-client validation.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 6
- Total artifacts ported or partially modeled in this handoff window: 1 represented condition-target/effect-state bridge slice.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 6
- Total blocked/not-started artifacts: live `creature.getTarget`, live `KnownObject`/`VisibleObject`, live `Creature`, live `LifeStats`, Java HP precision/rounding, player-class lookup, live `EffectController`, effect lifecycle/removal, live `TribeRelationService`, live `GeoService`, Java map ordering/concurrent mutation, target mutation, packets, threading/serialization, reflection behavior, and live-client validation.
- Estimated overall migration completion: 66%

## Remaining Risks

- The bridge consumes represented facts only; it still does not query live current targets, known lists, life stats, player classes, tribe relations, geo visibility, or effect controllers.
- Live object identity, mutable target state, Java HP precision/rounding, effect lifecycle/removal, packets, persistence, threading, serialization, date/time behavior, reflection behavior, and live-client behavior remain missing or unverified.

## Next Recommended Unit of Work

Continue NPC skill readiness parity by wiring represented current-target facts into one higher-level mercenary NPC skill selection preview path, so static candidate metadata plus represented current target/effect facts can produce condition readiness without per-candidate manual target injection. Keep live `creature.getTarget`, real known-list/target identity, relation/geo services, effect controllers, packets, threading, serialization, and live-client validation explicit until supported.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6LD-Completion.md`
   - this handoff
3. Inspect `ProjectMercenaryNpcSkillCandidate`, `ProjectMercenaryNpcSkillCandidateList`, and selection-preview helpers.
4. Choose one higher-level represented preview entry point that can accept current-target known-object facts and condition-distance/relation/geo inputs.
5. Keep unsupported live `creature.getTarget`, relation/geo services, and effect-controller reads explicit.
6. Add focused tests that state what is source-derived and what remains unverified.
7. Run focused tests, then full GameServer tests.
8. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
9. Create the next handoff document and commit the unit.
