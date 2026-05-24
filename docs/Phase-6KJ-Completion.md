# Phase 6KJ Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6KI and covers Session 784.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerSummonCastSpellServiceTests|GameServerConnectionCastSpellTests|PlayerSummonSkillExecutionServiceTests"`
  - Result: Passed, 53 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1377 tests.

## Recent Work Completed

### Session 784 - Represented Static NPC Skill Candidate Adapter

- Re-inspected Java `NpcSkillTemplateEntry` and the current C# represented `chooseNextSkill` pipeline.
- Added `PlayerSummonKnownObjectNpcSkillCandidateMetadata` as a future static skill-list adapter DTO.
- Added `ProjectMercenaryNpcSkillCandidate` on `PlayerSummonSkillExecutionService`.
- Modeled:
  - template metadata to represented candidate projection;
  - Java `lastTimeUsed` state into entry timing;
  - HP/fight-time/cooldown/chance readiness composition;
  - condition readiness composition from represented condition target metadata;
  - optional target-range readiness pass-through;
  - list position preservation for selector ordering.
- Kept live `NpcSkillList`, static XML loading, missing `SkillData` pruning, Java `Rnd.chance`, Java random delay generation, live HP/fight-time ownership, live condition target lookup, Java geometry, controller execution, effects, packets, threading, and live-client validation unwired.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.skill.NpcSkillTemplateEntry` | `PlayerSummonSkillExecutionService.ProjectMercenaryNpcSkillCandidate` / `PlayerSummonKnownObjectNpcSkillCandidateMetadata` | Skill Entry Adapter Projection | Partial | Regression Tested | Needs Verification | Adapts represented template metadata and last-used state into a candidate consumable by the represented selector. Live entry ownership and Java runtime comparison remain missing. |
| `com.aionemu.gameserver.model.templates.npcskill.NpcSkillTemplate` | `PlayerSummonKnownObjectNpcSkillTemplateMetadata` consumed by candidate projection | Static Template DTO | Partial | Regression Tested | Needs Verification | Metadata fields are composed into candidate readiness. XML/JAXB loading, default-value runtime comparison, reflection, and serialization remain unverified. |
| `com.aionemu.gameserver.model.skill.NpcSkillTemplateEntry.isReady` | `EvaluateMercenaryNpcSkillEntryReadiness` consumed by `ProjectMercenaryNpcSkillCandidate` | Entry Timing/Chance Projection | Partial | Regression Tested | Needs Verification | HP, fight-time, cooldown, and explicit chance readiness are represented. Java `Rnd.chance`, live state sourcing, precision/rounding, and date/time runtime behavior remain unverified. |
| `com.aionemu.gameserver.model.skill.NpcSkillTemplateEntry.conditionReady` / `com.aionemu.gameserver.model.templates.npcskill.NpcSkillConditionTemplate` | `EvaluateMercenaryNpcSkillConditionReadiness` consumed by `ProjectMercenaryNpcSkillCandidate` | Condition Projection | Partial | Regression Tested | Needs Verification | Evaluates represented condition targets inside the adapter. Live owner/target lookup, geometry, unsupported Java behavior, and threading remain unwired. |
| `com.aionemu.gameserver.model.skill.NpcSkillList` | future callers supplying `PlayerSummonKnownObjectNpcSkillCandidateMetadata` collections | Skill List Dependency | Not Started | Manual Only as explicit DTO shape | Needs Verification | DTO gives the future static-list bridge a target shape, but no live list creation, priority grouping, chain map ownership, XML load, pruning, randomization, or scheduler integration exists yet. |

## Tests Added Or Updated

- `PlayerSummonSkillExecutionServiceTests.ProjectMercenaryNpcSkillCandidate_AdaptsStaticTemplateEntryIntoSelectableCandidate`
  - Validates template metadata and last-used timestamp adapt into projection fields.
  - Validates HP/time/cooldown entry readiness.
  - Validates condition readiness from represented condition target metadata.
  - Validates optional target-range readiness is preserved.
  - Validates the existing ordinary-priority selector can consume the projected candidate while skipping a higher-priority cooldown candidate.
- These tests are source-derived from Java. They do not compare against Java runtime execution, XML/static-data loading, `Rnd.chance`, live HP/fight-time state, live condition target lookup, Java geometry, reflection behavior, threading behavior, serialization behavior, date/time runtime behavior, precision/rounding behavior, scheduler behavior, or live-client validation.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 5
- Total artifacts ported or partially modeled in this handoff window: 1 represented static-entry candidate adapter slice.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 5
- Total blocked/not-started artifacts: live `NpcSkillList`, XML/static loading, missing skill pruning, priority grouping ownership, chain map ownership, Java `Rnd.chance`, Java random delay generation, live HP/fight-time source, live condition target lookup, Java geometry, controller execution, effects, Java runtime comparison, and live-client validation.
- Estimated overall migration completion: 66%

## Remaining Risks

- The adapter is represented service logic and is not invoked by live AI or live static skill-list loading.
- Live `NpcSkillList` creation, priority grouping ownership, chain ownership, static XML/JAXB loading, and missing `SkillData` pruning remain missing.
- Chance readiness is explicit and does not execute Java `Rnd.chance`; random next-skill delay generation remains missing.
- Live owner/target condition lookup, Java geometry, target mutation, controller execution, effects, packets, and AI scheduling remain unwired.
- Reflection, threading, serialization, date/time runtime, precision/rounding, Java runtime, scheduler, RNG, and live-client behavior remain unverified.

## Next Recommended Unit of Work

Continue by adding a collection-level adapter that converts future static NPC skill-list entries into represented candidate lists plus queued/last-skill inputs for `PreviewMercenaryNextNpcSkillSelection`, or wire one existing C# mercenary known-object path to consume the selection/action previews without executing live controller effects. Keep XML loading, Java `Rnd.chance`, random delay generation, Java geometry, controller execution, effects, packets, scheduler/date-time behavior, and live-client validation explicit until supported.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6KI-Completion.md`
   - this handoff
3. Inspect Java `SkillAttackManager.chooseNextSkill`, `NpcSkillList`, `NpcSkillEntry`, `NpcSkillTemplateEntry`, `NpcSkillTemplate`, and `NpcSkillConditionTemplate`.
4. Inspect C# `ProjectMercenaryNpcSkillCandidate`, `PlayerSummonKnownObjectNpcSkillCandidateMetadata`, `PreviewMercenaryNextNpcSkillSelection`, candidate records, and tests.
5. Implement one narrow represented collection adapter or preview consumer slice with Java breadcrumbs.
6. Add focused tests that state what is source-derived and what remains unverified.
7. Run focused tests, then full GameServer tests.
8. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
9. Create the next handoff document and commit the unit.
