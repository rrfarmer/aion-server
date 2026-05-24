# Phase 6KA Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6JZ and covers Session 775.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerSummonCastSpellServiceTests|GameServerConnectionCastSpellTests|PlayerSummonSkillExecutionServiceTests"`
  - Result: Passed, 44 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1368 tests.

## Recent Work Completed

### Session 775 - NPC Skill Template Metadata Projection

- Re-inspected Java `NpcSkillTemplate`, `NpcSkillConditionTemplate`, `NpcSkillTargetAttribute`, and `ConjunctionType`.
- Added represented C# metadata/projection records:
  - `PlayerSummonKnownObjectNpcSkillTemplateMetadata`;
  - `PlayerSummonKnownObjectNpcSkillTemplateProjection`;
  - `PlayerSummonKnownObjectNpcSkillConditionMetadata`;
  - `PlayerSummonKnownObjectNpcSkillTargetAttribute`.
- Added projection helpers on `PlayerSummonSkillExecutionService`:
  - `ProjectMercenaryNpcSkillTemplate`;
  - `ProjectMercenaryNpcSkillEntryTiming`;
  - `ResolveMercenaryNpcSkillTargetMode`.
- Modeled Java source defaults for static NPC skill template fields and condition template fields.
- Mapped Java target attributes:
  - `NONE` -> represented `None`;
  - `MOST_HATED` -> represented `MostHated`;
  - `ME` -> represented `Self`;
  - `FRIEND`, hated-order targets, and random targets -> represented `CreatureTarget`.
- No XML/JAXB loading, live NPC skill table ownership, target selection, random target resolution, live AI scheduling, condition mutation, controller execution, effects, or packets are implemented yet.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.templates.npcskill.NpcSkillTemplate` | `PlayerSummonKnownObjectNpcSkillTemplateMetadata` / `PlayerSummonKnownObjectNpcSkillTemplateProjection` | DTO / Static Template Projection | Partial | Regression Tested | Needs Verification | Represents key static fields and defaults, but not XML/JAXB loading, `DataManager.SKILL_DATA` lookup, live AI ownership, or serialization. |
| `com.aionemu.gameserver.model.templates.npcskill.NpcSkillConditionTemplate` | `PlayerSummonKnownObjectNpcSkillConditionMetadata` | DTO / Condition Metadata | Partial | Regression Tested | Needs Verification | Represents defaults for condition type, HP threshold, range, NPC id, delay, can-die, and despawn time; behavior consumers remain mostly unwired. |
| `com.aionemu.gameserver.model.templates.npcskill.NpcSkillTargetAttribute` | `PlayerSummonKnownObjectNpcSkillTargetAttribute` and `ResolveMercenaryNpcSkillTargetMode` | Enum / Target Projection | Partial | Regression Tested | Needs Verification | Special range-skip modes are represented; live target resolver and random selection are missing. |
| `com.aionemu.gameserver.model.templates.npcskill.ConjunctionType` | `PlayerSummonKnownObjectNpcSkillConjunction` through template projection | Enum Projection | Partial | Regression Tested | Needs Verification | Static conjunction metadata now feeds timing projection; XML binding remains unverified. |
| `com.aionemu.gameserver.model.skill.NpcSkillTemplateEntry.isReady` | `ProjectMercenaryNpcSkillEntryTiming` feeding `EvaluateMercenaryNpcSkillEntryReadiness` | NPC Skill Timing Projection | Partial | Regression Tested | Needs Verification | Static HP/time/cooldown/conjunction fields can feed represented readiness; RNG and live HP/fight-time inputs remain separate. |

## Tests Added Or Updated

- `PlayerSummonSkillExecutionServiceTests.ProjectMercenaryNpcSkillTemplate_MapsJavaTemplateDefaultsAndOverrides`
  - Validates Java source-derived defaults and override mapping for timing fields, condition metadata, target mode, probability, priority, next-skill time, chain fields, max-chain time, post-spawn flag, and last-used timestamp projection.
- `PlayerSummonSkillExecutionServiceTests.ResolveMercenaryNpcSkillTargetMode_MapsJavaNpcSkillTargetAttributes`
  - Validates target attribute mapping for `NONE`, `MOST_HATED`, `ME`, `FRIEND`, hated-order targets, and random target modes.
- These tests are source-derived from Java. They do not compare against Java runtime execution, XML/JAXB parsing, live target resolution, random target selection, reflection behavior, threading behavior, serialization behavior, date/time runtime behavior, precision/rounding behavior, geometry behavior, or live-client validation.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 5
- Total artifacts ported or partially modeled in this handoff window: 1 represented static NPC skill-template metadata/projection slice.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 5
- Total blocked/not-started artifacts: XML/JAXB NPC skill loading, `DataManager.SKILL_DATA` template lookup wiring, live AI skill scheduling, RNG chance parity, HP percentage source, elapsed fight-time source, chain/priority selection, live target resolution, random target selection, help-friend target mutation, condition range geometry, world-map NPC scans, controller execution, and live-client validation.
- Estimated overall migration completion: 66%

## Remaining Risks

- Static template projection is not yet wired to a real C# NPC skill data table or XML loader.
- `SkillTemplate` lookup through Java `DataManager.SKILL_DATA` remains represented elsewhere and is not tied to this metadata.
- RNG probability is carried as data but not executed; chance readiness is still an explicit input.
- Live HP percentage source, elapsed fight-time source, last-skill-time mutation, chain selection, priority ordering, and next-skill scheduling remain missing.
- Condition metadata is represented but not consumed for help-friend target mutation, `hp_below`, `range` geometry, `npc_id` world scans, spawn delay, can-die, or despawn-time behavior.
- Reflection, threading, serialization, date/time runtime, precision/rounding, Java runtime, XML binding, geometry, and live-client behavior remain unverified.

## Next Recommended Unit of Work

Continue by consuming `PlayerSummonKnownObjectNpcSkillConditionMetadata.RangeMeters` in a source-derived `TARGET_IS_IN_RANGE` projection, or add a represented NPC skill selection unit that orders projected entries by Java `prio`, timing readiness, and condition readiness without live AI mutation. Keep XML loading, random target selection, help-friend mutation, carved signet effects, world-map NPC scans, Java geometry, effects, packets, and controller execution explicit until supported.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6JZ-Completion.md`
   - this handoff
3. Inspect Java `NpcSkillTemplate`, `NpcSkillTemplateEntry`, `NpcSkillConditionTemplate`, `NpcSkillTargetAttribute`, `SkillAttackManager.isReady`, and current C# summon skill execution service.
4. Inspect C# `PlayerSummonKnownObjectNpcSkillTemplateMetadata`, `PlayerSummonKnownObjectNpcSkillConditionMetadata`, `PlayerSummonKnownObjectNpcSkillEntryTiming`, `PlayerSummonKnownObjectNpcSkillConditionReadiness`, `PlayerSummonKnownObjectSkillReadiness`, `PlayerSummonSkillExecutionService`, and execution tests.
5. Implement one narrow NPC skill condition-range or represented entry-selection slice with Java breadcrumbs.
6. Add focused tests that state what is source-derived and what remains unverified.
7. Run focused tests, then full GameServer tests.
8. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
9. Create the next handoff document and commit the unit.
