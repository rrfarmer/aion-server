# Phase 6LB Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6LA and covers Session 802.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerSummonSkillExecutionServiceTests|StaticDataNpcSkillTests"`
  - Result: Passed, 42 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1392 tests.

## Recent Work Completed

### Session 802 - Represented HELP_FRIEND Condition Readiness

- Re-inspected Java `NpcSkillTemplateEntry.conditionReady` for `HELP_FRIEND`, plus `KnownList.findObject`, `TribeRelationService.isSupport/isFriend`, `PositionUtil.isInRange`, and `GeoService.canSee`.
- Added represented `PlayerSummonKnownObjectNpcSkillHelpFriendCandidate` metadata for known-list candidate facts.
- Extended `EvaluateMercenaryNpcSkillConditionReadiness(PlayerSummonKnownObjectNpcSkillConditionMetadata, ...)` with represented help-friend candidate input.
- Extended selected condition-target metadata with object id, visibility, creature/death state, relation flags, hp percentage, range readiness, and geo visibility.
- Added `PlayerSummonKnownObjectNpcSkillConditionReadiness.WouldSetOwnerTarget` to represent the Java `creature.setTarget(validTarget)` side effect without mutating live state.
- Modeled unsupported missing known-list input, owner-dead short-circuiting, first valid candidate selection, and all Java predicate gates used by the `HELP_FRIEND` branch.
- Kept live known-list traversal, real world object identity, live `Creature`/`LifeStats`, real tribe relation data, `GeoService`, `PositionUtil`, live target mutation, AI selection, controller/effect execution, packets, scheduler/date-time behavior, threading, serialization, and live-client validation unwired.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.skill.NpcSkillTemplateEntry.conditionReady` | `PlayerSummonSkillExecutionService.EvaluateMercenaryNpcSkillConditionReadiness(PlayerSummonKnownObjectNpcSkillConditionMetadata, ...)` | Service | Partial | Regression Tested | Needs Verification | C# models represented `HELP_FRIEND` scanning and target-retarget intent, but does not traverse live known lists, call Java relation/geo services, mutate the live owner target, or compare runtime Java behavior. |
| `com.aionemu.gameserver.world.knownlist.KnownList.findObject` | `IEnumerable<PlayerSummonKnownObjectNpcSkillHelpFriendCandidate>` input | Known-list Dependency | Not Started | Regression Tested as metadata only | Needs Verification | C# preserves first-match behavior over supplied candidates. Live map-backed iteration, object identity, visibility refreshes, concurrent mutation, and Java collection ordering remain unverified. |
| `com.aionemu.gameserver.world.knownlist.KnownObject` / `com.aionemu.gameserver.model.gameobjects.VisibleObject` | `PlayerSummonKnownObjectNpcSkillHelpFriendCandidate` and selected condition-target facts | World Object Dependency | Partial | Regression Tested as metadata only | Needs Verification | C# records represented object state only. Live `KnownObject.get()`, `VisibleObject` identity, visibility recalculation, and serialization remain missing. |
| `com.aionemu.gameserver.model.gameobjects.Creature` / `com.aionemu.gameserver.model.gameobjects.stats.CreatureLifeStats` | `PlayerSummonKnownObjectNpcSkillHelpFriendCandidate` state fields | Creature / Stats Dependency | Partial | Regression Tested as metadata only | Needs Verification | C# represents dead/about-to-die and HP percentage gates. Live `isDead`, `getLifeStats().isAboutToDie`, Java HP rounding/precision, stat observers, threading, and packets remain unverified. |
| `com.aionemu.gameserver.services.TribeRelationService` | `IsSupport` / `IsFriend` represented candidate flags | Service Dependency | Not Started | Regression Tested as metadata only | Needs Verification | C# consumes relation booleans but does not port tribe/base-tribe, Panesterra faction, or `DataManager.TRIBE_RELATIONS_DATA` lookup behavior. |
| `com.aionemu.gameserver.utils.PositionUtil` | `DistanceMeters <= conditionMetadata.RangeMeters` gate | Utility Dependency | Partial | Regression Tested | Needs Verification | C# represents range gating with supplied distance and integer range. Java coordinate math, instance/world ids, collision flags, float precision, and exact `PositionUtil.isInRange(..., false)` behavior remain unverified. |
| `com.aionemu.gameserver.world.geo.GeoService` | `GeoCanSee` represented candidate flag | Service Dependency | Not Started | Regression Tested as metadata only | Needs Verification | C# records future line-of-sight results but does not call geo maps, z offsets, ignore properties, instance ids, collision data, or live geo runtime. |

## Tests Added Or Updated

- `PlayerSummonSkillExecutionServiceTests.EvaluateMercenaryNpcSkillConditionReadiness_ProjectsHelpFriendKnownListSearch`
  - Validates unsupported missing known-list input.
  - Validates no-match results across visible, creature, dead, about-to-die, relation, hp, range, and geo predicate gates.
  - Validates first valid candidate selection in supplied order.
  - Validates selected target metadata and `WouldSetOwnerTarget`.
  - Validates owner-dead short-circuit behavior.
- These tests are source-derived from Java. They do not compare against Java runtime execution, live known-list traversal, real tribe relation data, geo visibility, target mutation, Java collection mutation/order behavior, reflection behavior, threading behavior, serialization behavior, date/time behavior, precision/rounding behavior, packets, or live-client validation.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 7
- Total artifacts ported or partially modeled in this handoff window: 1 represented `HELP_FRIEND` condition readiness slice.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 7
- Total blocked/not-started artifacts: live `KnownList.findObject`, real `KnownObject`/`VisibleObject` identity, live visibility refresh, live `Creature`/`LifeStats`, HP precision/rounding, `TribeRelationService`, `DataManager.TRIBE_RELATIONS_DATA`, Panesterra faction relation rules, `PositionUtil` coordinate math, `GeoService`, target mutation, AI selection, controller/effect execution, packets, threading/serialization, and live-client validation.
- Estimated overall migration completion: 66%

## Remaining Risks

- `HELP_FRIEND` is represented by supplied candidate facts, not by live known-list traversal.
- Real tribe relation, geo visibility, Java coordinate/range precision, HP percentage rounding, live target mutation, AI selection, controller/effect execution, packets, persistence, threading, serialization, date/time behavior, and live-client behavior remain missing or unverified.

## Next Recommended Unit of Work

Continue NPC skill readiness parity by modeling carved-signet condition families (`TARGET_HAS_CARVED_SIGNET` through level V) with explicit represented effect-state metadata, including Java `EffectController`/signet lookup semantics and level thresholds. Keep live effect controllers, abnormal/effect state mutation, target object identity, reflection, threading, serialization, packets, and live-client validation explicit until supported.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6LA-Completion.md`
   - this handoff
3. Inspect Java `NpcSkillTemplateEntry.conditionReady` carved-signet branches and the Java effect/signet APIs they call.
4. Inspect C# `EvaluateMercenaryNpcSkillConditionReadiness`, `PlayerSummonKnownObjectNpcSkillConditionTarget`, and current abnormal/effect metadata.
5. Implement one narrow represented carved-signet readiness slice.
6. Add focused tests that state what is source-derived and what remains unverified.
7. Run focused tests, then full GameServer tests.
8. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
9. Create the next handoff document and commit the unit.
