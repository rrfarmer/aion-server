# Phase 6KX Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6KW and covers Session 798.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerSummonSkillExecutionServiceTests|StaticDataNpcSkillTests"`
  - Result: Passed, 39 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1389 tests.

## Recent Work Completed

### Session 798 - Represented NPC Skill Candidate Adapter

- Re-inspected Java `NpcSkillList` and `NpcSkillTemplateEntry`.
- Added `PlayerSummonSkillExecutionService.ProjectMercenaryNpcSkillTemplateMetadata(NpcSkillTemplateSummary)`.
- Added represented `NpcSkillTable` adapter overloads for `ProjectMercenaryNpcSkillCandidateMetadata` and `ProjectMercenaryNpcSkillCandidateList`.
- Modeled missing NPC skill lists, XML-order positions, scalar template projection, spawn metadata projection, target/conjunction string conversion, priority extraction, and `is_post_spawn` filtering.
- Kept live `DataManager.SKILL_DATA.getSkillTemplate` pruning/removal, missing-skill warning logs, mutable Java template-list side effects, condition-template projection, runtime enum/JAXB validation, Java RNG, live AI selection, controller/effect execution, spawn-engine execution, packets, scheduler/date-time behavior, threading, serialization, and live-client validation unwired.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.skill.NpcSkillList` | `PlayerSummonSkillExecutionService.ProjectMercenaryNpcSkillCandidateMetadata(NpcSkillTable?, int)` and `ProjectMercenaryNpcSkillCandidateList(NpcSkillTable?, int, ...)` | Service Adapter | Partial | Regression Tested | Needs Verification | Adapts represented static NPC skill lists into candidate metadata/projections by NPC id, preserving XML order positions, empty-list behavior, priorities, and post-spawn filtering. Does not remove missing live skill templates, log duplicate/missing skill warnings, mutate source lists, or execute live AI selection. |
| `com.aionemu.gameserver.model.skill.NpcSkillTemplateEntry` | `PlayerSummonSkillExecutionService.ProjectMercenaryNpcSkillTemplateMetadata(NpcSkillTemplateSummary)` | Entry Adapter | Partial | Regression Tested | Needs Verification | Maps represented template fields into existing readiness/selection metadata and carries spawn metadata. Does not instantiate live entries, mutate `lastTimeUsed`, execute `conditionReady`, `chanceReady`, `fireOnEndCastEvents`, or compare Java runtime behavior. |
| `com.aionemu.gameserver.model.templates.npcskill.NpcSkillTemplate` | `NpcSkillTemplateSummary` to `PlayerSummonKnownObjectNpcSkillTemplateMetadata` projection | DTO Adapter | Partial | Regression Tested | Needs Verification | Projects scalar fields, `is_post_spawn`, priority, chain fields, target, conjunction, and spawn child. Condition templates, `getSkillTemplate()` lookup, enum JAXB validation, reflection behavior, threading, and serialization remain unverified. |
| `com.aionemu.gameserver.model.templates.npcskill.NpcSkillSpawn` | `NpcSkillSpawnSummary` to `PlayerSummonKnownObjectNpcSkillSpawnMetadata` projection | DTO Adapter | Complete for represented scalar fields | Regression Tested | Needs Verification | Maps `npc_id`, `delay`, distances, and counts into existing spawn metadata. Spawn-engine execution, Java geometry/RNG, scheduler date-time behavior, packets, and live-client behavior remain unverified. |
| `com.aionemu.gameserver.model.templates.npcskill.NpcSkillTargetAttribute` | `ResolveMercenaryNpcSkillTargetAttribute` | Enum Adapter | Partial | Regression Tested | Needs Verification | Maps represented Java enum names needed by the current adapter. Unknown names fall back to `MostHated` instead of JAXB/schema failure; this is an intentional defensive placeholder until schema/runtime enum validation is wired. |

## Tests Added Or Updated

- `PlayerSummonSkillExecutionServiceTests.ProjectMercenaryNpcSkillCandidateList_AdaptsRepresentedNpcSkillTableEntries`
  - Validates represented table lookup by NPC id.
  - Validates XML-order candidate positions and missing NPC empty-list behavior.
  - Validates scalar template projection, conjunction conversion, target conversion, and spawn metadata projection.
  - Validates descending distinct priority extraction, readiness projection, and post-spawn filtering.
- These tests are source-derived from Java. They do not compare against Java runtime execution, live `DataManager.SKILL_DATA` pruning/removal, missing-skill warning logs, mutable source-list behavior, condition-template runtime behavior, Java RNG, scheduler/date-time behavior, reflection behavior, threading behavior, serialization behavior, precision/rounding behavior, or live-client validation.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 5
- Total artifacts ported or partially modeled in this handoff window: 1 represented NPC skill candidate adapter slice.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 5
- Total blocked/not-started artifacts: live `DataManager.SKILL_DATA` pruning/removal, missing-skill warning logs, mutable Java template-list side effects, condition-template projection, target enum schema validation, live skill-template lookup, AI selection, Java RNG runtime comparison, scheduler/date-time execution, spawn-engine execution, live AI mutation, controller/effect execution, packets, persistence, threading/serialization, reflection behavior, precision/rounding, and live-client validation.
- Estimated overall migration completion: 66%

## Remaining Risks

- The adapter feeds represented candidate projections only; it does not yet integrate with live NPC AI/controller execution.
- Missing live skill templates are not pruned from represented `NpcSkillTable` lists, and C# does not currently reproduce Java's warning log or source-list mutation.
- Condition templates, chance/random behavior, target object selection, live skill-template lookup, spawn scheduling, spawn-engine execution, controller/effect execution, packets, persistence, threading, serialization, date/time behavior, precision/rounding, and live-client behavior remain missing or unverified.

## Next Recommended Unit of Work

Continue NPC skill readiness parity by modeling Java `NpcSkillList.initSkillList` missing-skill pruning against represented `SkillTemplateTable`, including `DataManager.SKILL_DATA.getSkillTemplate(template.getSkillId()) == null` removal/warning metadata before candidate projection. Keep mutable Java list side effects, condition-template projection, Java RNG, live AI selection, controller/effect execution, spawn-engine execution, packets, scheduler/date-time behavior, threading, serialization, and live-client validation explicit until supported.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6KW-Completion.md`
   - this handoff
3. Inspect Java `NpcSkillList.initSkillList`, `NpcSkillTemplateEntry`, and `DataManager.SKILL_DATA.getSkillTemplate`.
4. Inspect C# `NpcSkillTable`, `SkillTemplateTable`, `ProjectMercenaryNpcSkillCandidateMetadata`, and `ProjectMercenaryNpcSkillCandidateList`.
5. Implement one narrow missing-skill pruning projection with explicit metadata for missing skill ids and Java warning/source-list mutation gaps.
6. Add focused tests that state what is source-derived and what remains unverified.
7. Run focused tests, then full GameServer tests.
8. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
9. Create the next handoff document and commit the unit.
