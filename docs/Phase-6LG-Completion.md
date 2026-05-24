# Phase 6LG Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6LF and covers Session 807.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerSummonSkillExecutionServiceTests|StaticDataNpcSkillTests"`
  - Result: Passed, 46 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1396 tests.

## Recent Work Completed

### Session 807 - Represented HELP_FRIEND Known-List Preview Injection

- Re-inspected represented `HELP_FRIEND` readiness, `ProjectMercenaryNpcSkillHelpFriendCandidate`, and `PreviewMercenaryNextNpcSkillSelectionFromRepresentedCurrentTarget`.
- Extended `PlayerSummonKnownObjectNpcSkillCandidateMetadata` with optional represented `HELP_FRIEND` candidate facts.
- Wired `ProjectMercenaryNpcSkillCandidate` to pass represented help-friend candidates into `EvaluateMercenaryNpcSkillConditionReadiness`.
- Extended the represented current-target high-level preview path to accept caller-supplied represented known-list candidate facts and inject them into `HELP_FRIEND` candidates.
- Modeled high-level selection where `HELP_FRIEND` can win from represented known-list facts, fallback behavior when represented known-list facts are absent, first valid support/friend candidate selection through existing readiness logic, and preservation of non-`HELP_FRIEND` behavior.
- Kept live `KnownList.findObject`, Java map iteration under mutation, live `KnownObject`/`VisibleObject` identity, real `TribeRelationService`, real `GeoService`, `creature.setTarget`, AI state mutation, packets, threading, serialization, and live-client validation unwired.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.skill.NpcSkillTemplateEntry.conditionReady` `HELP_FRIEND` branch | `EvaluateMercenaryNpcSkillConditionReadiness` via candidate `HelpFriendCandidates` | Service | Partial | Regression Tested | Needs Verification | C# can evaluate represented `HELP_FRIEND` facts through the candidate and preview pipeline. It does not call live `KnownList.findObject`, relation/geo services, or mutate the owner target. |
| `com.aionemu.gameserver.ai.manager.SkillAttackManager.chooseNextSkill` | `PreviewMercenaryNextNpcSkillSelectionFromRepresentedCurrentTarget` with `helpFriendCandidates` | Service | Partial | Regression Tested | Needs Verification | C# high-level represented selection can choose a `HELP_FRIEND` skill when supplied known-list facts satisfy Java predicates. Live AI state, queued skill mutation, real target setting, and runtime Java behavior remain unverified. |
| `com.aionemu.gameserver.world.knownlist.KnownList.findObject` | `IEnumerable<PlayerSummonKnownObjectNpcSkillHelpFriendCandidate>` preview input | Known-list Dependency | Not Started | Regression Tested as metadata only | Needs Verification | C# consumes supplied candidate facts in order. Live map-backed iteration, object identity, visibility refresh, concurrent mutation, and Java map ordering remain missing. |
| `com.aionemu.gameserver.world.knownlist.KnownObject` / `VisibleObject` / `Creature` | `PlayerSummonKnownObjectNpcSkillHelpFriendCandidate` | World Object DTO | Partial | Regression Tested | Needs Verification | C# represents visibility, creature/death state, HP percentage, distance, relation flags, and geo visibility. Live objects, life stats, Java HP precision/rounding, coordinates, threading, and serialization remain unverified. |
| `com.aionemu.gameserver.services.TribeRelationService` / `GeoService` | represented `IsSupport`, `IsFriend`, and `GeoCanSee` candidate flags | Service Dependency | Not Started | Regression Tested as metadata only | Needs Verification | C# still requires precomputed booleans. Tribe/base-tribe data, Panesterra rules, geo maps, z offsets, instance ids, line-of-sight precision, packets, and live-client validation remain missing. |

## Tests Added Or Updated

- `PlayerSummonSkillExecutionServiceTests.PreviewMercenaryNextNpcSkillSelectionFromRepresentedCurrentTarget_AppliesHelpFriendKnownListFacts`
  - Validates high-level represented selection fallback without known-list facts.
  - Validates `HELP_FRIEND` selection with supplied represented known-list facts.
  - Validates first valid support/friend target metadata.
  - Validates target-retarget intent.
- These tests are source-derived from Java. They do not compare against Java runtime execution, live known-list traversal, tribe relation behavior, geo behavior, target mutation, reflection behavior, threading behavior, serialization behavior, date/time behavior, precision/rounding behavior, packets, or live-client validation.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 5
- Total artifacts ported or partially modeled in this handoff window: 1 represented high-level `HELP_FRIEND` known-list injection slice.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 5
- Total blocked/not-started artifacts: live `KnownList.findObject`, live `KnownObject`/`VisibleObject`, live `Creature`/`LifeStats`, Java HP precision/rounding, real `TribeRelationService`, real `GeoService`, Java map ordering/concurrent mutation, `creature.setTarget`, live AI state mutation, packets, persistence, threading/serialization, reflection behavior, date/time behavior, and live-client validation.
- Estimated overall migration completion: 66%

## Remaining Risks

- The high-level preview still consumes represented known-list facts from callers; it does not discover them from live known lists.
- Real relation and geo behavior, target mutation, Java map ordering/concurrent mutation, live object identity, life stats, HP precision/rounding, packets, persistence, threading, serialization, reflection behavior, date/time behavior, and live-client behavior remain missing or unverified.

## Next Recommended Unit of Work

Continue NPC skill readiness parity by modeling `SkillAttackManager.skillAction` target invalidation and next-skill-delay behavior around represented current target death/visibility/range outcomes, including Java's 5000 ms delay path. Keep live `owner.canSee`, `isTargetTooFar`, `setNextSkillDelay`, packets, threading, serialization, and live-client validation explicit until supported.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6LF-Completion.md`
   - this handoff
3. Inspect Java `SkillAttackManager.skillAction` target invalidation and target-too-far branches.
4. Inspect existing C# `PreviewMercenaryNpcSkillAction` / target-range delay helpers.
5. Model one represented slice around dead/not-visible/out-of-range target behavior and 5000 ms next-skill-delay metadata.
6. Keep unsupported live `owner.canSee`, `isTargetTooFar`, packets, threading, serialization, and live-client behavior explicit.
7. Add focused tests that state what is source-derived and what remains unverified.
8. Run focused tests, then full GameServer tests.
9. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
10. Create the next handoff document and commit the unit.
