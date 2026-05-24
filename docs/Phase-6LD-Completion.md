# Phase 6LD Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6LC and covers Session 804.

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

### Session 804 - Represented SignetBurst Skill Metadata Projection

- Re-inspected represented C# `SkillTemplateTable` / `SkillTemplateSummary` loading and Java `SignetBurstEffect` XML attributes.
- Added represented `SkillSignetBurstEffectSummary` for `signetburst` skill-template effects.
- Extended `StaticData.LoadFromCacheAsync` to read `<signetburst signet="..." signetlvl="..."/>` under skill-template effects.
- Extended `SkillTemplateSummary` with `SignetBurst` metadata.
- Extended pruning-aware NPC skill candidate metadata projection to carry represented skill-template signet-burst stack names for surviving NPC skill entries.
- Wired `ProjectMercenaryNpcSkillCandidate` to pass represented signet-burst stack names into carved-signet condition readiness.
- Kept full JAXB effect-template polymorphism, non-signet skill effects, live `SkillTemplate.getEffects`, live `EffectController`, signet damage/removal/sub-effects, packets, threading, serialization, and live-client validation unwired.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.skillengine.effect.SignetBurstEffect` | `Aion.GameServer.Dataholders.SkillSignetBurstEffectSummary` and `SkillTemplateSummary.SignetBurst` | Effect-template DTO | Partial | Regression Tested | Needs Verification | C# loads represented `signet` and `signetlvl` XML attributes. It does not model JAXB class polymorphism beyond this element, damage calculation, add-effect probability, sub-effects, signet removal, or runtime Java behavior. |
| `com.aionemu.gameserver.skillengine.model.SkillTemplate.getEffects` | `SkillTemplateSummary.SignetBurst` | Skill-template Dependency | Partial | Regression Tested | Needs Verification | C# projects represented signet-burst effects from static XML for candidate readiness. It does not expose full effect-template ordering, all effect types, Java effect lists, reflection behavior, threading, or serialization parity. |
| `com.aionemu.gameserver.dataholders.SkillData` | `Aion.GameServer.Dataholders.SkillTemplateTable.GetSkillTemplate` plus signet-burst metadata | Dataholder / Repository | Partial | Regression Tested | Needs Verification | C# uses represented skill-template lookup to prune NPC skills and now carry signet-burst stacks. Live `DataManager.SKILL_DATA`, cache lifecycle, JAXB schema validation, and Java runtime comparison remain unverified. |
| `com.aionemu.gameserver.model.skill.NpcSkillList` | pruning-aware `ProjectMercenaryNpcSkillCandidateMetadata(NpcSkillTable?, SkillTemplateTable, int)` | Service Adapter | Partial | Regression Tested | Needs Verification | C# now attaches represented signet-burst stack names to surviving candidate metadata. Java list mutation, warning logs, live skill-template objects, and runtime entry construction remain unverified. |
| `com.aionemu.gameserver.model.skill.NpcSkillTemplateEntry.hasCarvedSignet` | `ProjectMercenaryNpcSkillCandidate` passing candidate `SkillTemplateSignetBurstStacks` into readiness | Service | Partial | Regression Tested | Needs Verification | C# carved-signet readiness can now consume represented skill-template data through the candidate path. Live target effect-controller state, effect mutation/removal, packets, and runtime Java comparison remain missing. |

## Tests Added Or Updated

- `StaticDataNpcSkillTests.LoadFromCacheAsync_ProjectsNpcSkillSpawnXmlDefaultsAndNpcIdIndex`
  - Now also validates represented skill-template `<signetburst>` XML loading into `SkillTemplateSummary.SignetBurst`.
- `PlayerSummonSkillExecutionServiceTests.ProjectMercenaryNpcSkillCandidateMetadata_PrunesMissingSkillTemplatesLikeJavaInitSkillList`
  - Now validates signet-burst stack propagation from represented `SkillTemplateTable` into surviving NPC skill candidate metadata.
- `PlayerSummonSkillExecutionServiceTests.EvaluateMercenaryNpcSkillConditionReadiness_ProjectsCarvedSignetThresholds`
  - Now validates `ProjectMercenaryNpcSkillCandidate` passes candidate signet-burst stacks into carved-signet readiness.
- These tests are source-derived from Java. They do not compare against Java runtime JAXB execution, full effect-template polymorphism, live `DataManager.SKILL_DATA`, live `EffectController`, effect mutation/removal, reflection behavior, threading behavior, serialization behavior, date/time behavior, precision/rounding behavior, packets, or live-client validation.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 5
- Total artifacts ported or partially modeled in this handoff window: 1 represented signet-burst skill-template metadata and candidate-projection slice.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 5
- Total blocked/not-started artifacts: full JAXB effect-template polymorphism, live `SkillTemplate.getEffects`, non-signet effects, live `DataManager.SKILL_DATA`, Java cache lifecycle, live `EffectController`, effect mutation/removal, signet damage/sub-effects, target object identity, packets, threading/serialization, reflection behavior, precision/rounding, and live-client validation.
- Estimated overall migration completion: 66%

## Remaining Risks

- Only represented `signetburst` effect metadata is loaded; the full Java effect-template hierarchy is still not ported.
- Live skill-template objects, effect-controller state, signet removal/damage side effects, Java cache/JAXB lifecycle, target object identity, packets, persistence, threading, serialization, date/time behavior, precision/rounding, and live-client behavior remain missing or unverified.

## Next Recommended Unit of Work

Continue NPC skill readiness parity by designing the next bridge from represented condition-target facts to future live target/effect state, especially how current target object identity, creature/death state, active signet levels, and abnormal states will be supplied from the live AI/known-object path. Keep live `EffectController`, object identity, packets, threading, serialization, and live-client validation explicit until supported.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6LC-Completion.md`
   - this handoff
3. Inspect `PlayerSummonSkillExecutionService.ProjectMercenaryNpcSkillCandidate`, condition-target metadata, and the future/live known-object or AI selection surfaces that can provide current-target facts.
4. Choose one narrow bridge for represented target/effect state and keep unsupported live behavior explicit.
5. Add focused tests that state what is source-derived and what remains unverified.
6. Run focused tests, then full GameServer tests.
7. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
8. Create the next handoff document and commit the unit.
