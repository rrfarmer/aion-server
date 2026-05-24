# Phase 6KZ Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6KY and covers Session 800.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerSummonSkillExecutionServiceTests|StaticDataNpcSkillTests"`
  - Result: Passed, 40 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1390 tests.

## Recent Work Completed

### Session 800 - Represented NPC Skill Condition Loading

- Re-inspected Java `NpcSkillConditionTemplate`, `NpcSkillCondition`, and `NpcSkillTemplateEntry.conditionReady`.
- Added represented `NpcSkillConditionSummary`.
- Extended `StaticData.LoadFromCacheAsync` to load `npc_skill` child `<cond>` attributes into represented NPC skill template summaries.
- Extended `PlayerSummonSkillExecutionService.ProjectMercenaryNpcSkillTemplateMetadata(NpcSkillTemplateSummary)` to project represented condition summaries into existing C# `PlayerSummonKnownObjectNpcSkillConditionMetadata`.
- Added `ResolveMercenaryNpcSkillCondition` for represented Java condition enum-name conversion.
- Modeled absent condition as null, empty `<cond/>` Java defaults, explicit condition scalar overrides, and condition projection into the already-supported C# readiness metadata path.
- Kept runtime condition side effects, `HELP_FRIEND` target search/retargeting, `NPC_IS_ALIVE` world lookup, signet/effect inspection, unsupported condition execution, Java JAXB/schema enum failure behavior, Java RNG, live AI selection, controller/effect execution, spawn-engine execution, packets, scheduler/date-time behavior, threading, serialization, and live-client validation unwired.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.templates.npcskill.NpcSkillConditionTemplate` | `Aion.GameServer.Dataholders.NpcSkillConditionSummary` plus `StaticData.LoadFromCacheAsync` condition projection | DTO / XML Loader | Partial | Regression Tested | Needs Verification | Loads represented `<cond>` scalar attributes and Java defaults. Does not run JAXB/schema validation, reflection behavior, runtime condition execution, or Java enum parse failure behavior. |
| `com.aionemu.gameserver.model.templates.npcskill.NpcSkillCondition` | `PlayerSummonSkillExecutionService.ResolveMercenaryNpcSkillCondition` and `PlayerSummonKnownObjectNpcSkillCondition` | Enum Adapter | Partial | Regression Tested | Needs Verification | Maps Java enum names into the existing represented condition enum. Unknown values fall back to `None` until schema/runtime enum validation is wired; this differs from Java JAXB failure behavior and is documented as a temporary defensive placeholder. |
| `com.aionemu.gameserver.model.templates.npcskill.NpcSkillTemplate` | `NpcSkillTemplateSummary.Condition` to `PlayerSummonKnownObjectNpcSkillConditionMetadata` | DTO Adapter | Partial | Regression Tested | Needs Verification | Distinguishes absent condition from present default condition and projects condition metadata for later readiness checks. Full Java `getConditionTemplate()` object identity, JAXB lifecycle, threading, and serialization remain unverified. |
| `com.aionemu.gameserver.model.skill.NpcSkillTemplateEntry.conditionReady` | Existing `EvaluateMercenaryNpcSkillConditionReadiness` inputs fed by represented condition metadata | Service Dependency | Partial | Regression Tested | Needs Verification | Feeds represented condition metadata into existing readiness evaluation. `HELP_FRIEND`, `NPC_IS_ALIVE`, carved signet/effect checks, target retargeting, geo visibility, world lookup, and unsupported condition runtime behavior remain incomplete. |

## Tests Added Or Updated

- `StaticDataNpcSkillTests.LoadFromCacheAsync_ProjectsNpcSkillSpawnXmlDefaultsAndNpcIdIndex`
  - Now also validates empty `<cond/>` Java defaults.
  - Validates explicit condition overrides from XML.
- `PlayerSummonSkillExecutionServiceTests.ProjectMercenaryNpcSkillCandidateList_AdaptsRepresentedNpcSkillTableEntries`
  - Now validates represented condition summary projection into C# condition metadata.
  - Checks condition type, hp threshold, range, npc id, delay, `can_die`, and despawn time.
- These tests are source-derived from Java. They do not compare against Java runtime JAXB execution, schema enum failure behavior, live `HELP_FRIEND` search/retargeting, `NPC_IS_ALIVE` world lookup, signet/effect inspection, geo visibility, scheduler/date-time behavior, reflection behavior, threading behavior, serialization behavior, precision/rounding behavior, or live-client validation.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 4
- Total artifacts ported or partially modeled in this handoff window: 1 represented NPC skill condition loading/projection slice.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 4
- Total blocked/not-started artifacts: Java JAXB/schema enum failure behavior, reflection behavior, live `HELP_FRIEND` known-list search, target retargeting, `NPC_IS_ALIVE` world lookup, carved signet/effect inspection, geo visibility, live AI selection, Java RNG runtime comparison, scheduler/date-time execution, spawn-engine execution, controller/effect execution, packets, persistence, threading/serialization, precision/rounding, and live-client validation.
- Estimated overall migration completion: 66%

## Remaining Risks

- Condition loading/projection is represented data only; several Java condition behaviors remain unsupported or only partially represented by existing readiness inputs.
- Java JAXB/schema enum validation, runtime target searching, target mutation, world instance lookup, effect/signet inspection, geo visibility, live AI selection, controller/effect execution, packets, persistence, threading, serialization, date/time behavior, precision/rounding, and live-client behavior remain missing or unverified.

## Next Recommended Unit of Work

Continue NPC skill readiness parity by deepening `EvaluateMercenaryNpcSkillConditionReadiness` for one unsupported represented condition family, preferably `NPC_IS_ALIVE` with a represented world-instance NPC presence input or `HELP_FRIEND` with explicit target-search/retarget metadata. Keep Java known-list search, geo visibility, world lookup, effect/signet state, live AI target mutation, Java RNG, scheduler/date-time behavior, threading, serialization, packets, and live-client validation explicit until supported.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6KY-Completion.md`
   - this handoff
3. Inspect Java `NpcSkillTemplateEntry.conditionReady` for `NPC_IS_ALIVE` or `HELP_FRIEND`.
4. Inspect C# `EvaluateMercenaryNpcSkillConditionReadiness`, `PlayerSummonKnownObjectNpcSkillConditionTarget`, and represented condition metadata.
5. Implement one narrow condition execution/projection slice with explicit unsupported behavior metadata.
6. Add focused tests that state what is source-derived and what remains unverified.
7. Run focused tests, then full GameServer tests.
8. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
9. Create the next handoff document and commit the unit.
