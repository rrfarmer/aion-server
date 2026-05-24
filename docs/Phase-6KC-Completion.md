# Phase 6KC Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6KB and covers Session 777.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerSummonCastSpellServiceTests|GameServerConnectionCastSpellTests|PlayerSummonSkillExecutionServiceTests"`
  - Result: Passed, 46 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1370 tests.

## Recent Work Completed

### Session 777 - Represented NPC Skill Candidate Selection

- Re-inspected Java `SkillAttackManager.chooseNextSkill` ordinary priority path and `NpcSkillList` priority grouping.
- Added represented selector DTOs:
  - `PlayerSummonKnownObjectNpcSkillCandidate`;
  - `PlayerSummonKnownObjectNpcSkillSelectionResult`;
  - `PlayerSummonKnownObjectNpcSkillSelectionStatus`.
- Added `SelectMercenaryNpcSkillCandidate` on `PlayerSummonSkillExecutionService`.
- Modeled deterministic priority-descending selection, ordinary chain-id skipping, timing-readiness and condition-readiness filtering, and first-ready-candidate target-range blocking.
- Deliberately did not model Java `Collections.shuffle` in priority groups; the C# selector uses source-position tie breaks for deterministic tests until RNG/shuffle parity is introduced.
- Kept queued skill handling, chain selection, initial delay, `canUseNextSkill`, `canUseNextChain`, live NPC skill-list ownership, target mutation, effects, packets, and controller execution unwired.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.ai.manager.SkillAttackManager.chooseNextSkill` ordinary priority path | `PlayerSummonSkillExecutionService.SelectMercenaryNpcSkillCandidate` | AI Skill Selection Projection | Partial | Regression Tested | Needs Verification | Models priority order, readiness filtering, chain-id skipping, and represented target-range block result. Queued and chain branches remain missing. |
| `com.aionemu.gameserver.model.skill.NpcSkillList.getPriorities` / `getSkillsByPriority` | priority ordering over `PlayerSummonKnownObjectNpcSkillCandidate.Projection.Priority` | Skill List Projection | Partial | Regression Tested | Needs Verification | Uses deterministic source-position tie breaks instead of Java `Collections.shuffle`. |
| `com.aionemu.gameserver.model.skill.NpcSkillEntry.getChainId` | `PlayerSummonKnownObjectNpcSkillTemplateProjection.ChainId` in selector | Chain Metadata Gate | Partial | Regression Tested | Needs Verification | Ordinary selection skips non-zero chain ids. Full chain branch remains missing. |
| `com.aionemu.gameserver.ai.manager.SkillAttackManager.getNpcSkillEntryIfNotTooFarAway` | `TargetRangeNotReady` selection result | Target-Range Selection Gate | Partial | Regression Tested | Needs Verification | Represents first-ready-candidate range blocking; does not mutate next-skill delay itself. |
| `java.util.Collections.shuffle` in `chooseNextSkill` | deterministic source-position tie break | RNG / Ordering Dependency | Not Started | Regression Tested as intentional deterministic behavior | Intentional Difference | Randomization parity, seeding, threading, and distribution remain unverified. |

## Tests Added Or Updated

- `PlayerSummonSkillExecutionServiceTests.SelectMercenaryNpcSkillCandidate_OrdersByPriorityAndReadiness`
  - Validates empty selection.
  - Validates ordinary chain-id skipping.
  - Validates timing-readiness and condition-readiness filtering.
  - Validates priority selection.
  - Validates target-range-not-ready behavior for the first ready candidate.
- These tests are source-derived from Java. They do not compare against Java runtime execution, RNG/shuffle behavior, queued-skill behavior, chain-skill behavior, live target resolution, next-skill delay mutation, reflection behavior, threading behavior, serialization behavior, date/time runtime behavior, precision/rounding behavior, or live-client validation.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 5
- Total artifacts ported or partially modeled in this handoff window: 1 represented ordinary-priority NPC skill selection slice.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 4
- Total blocked/not-started artifacts: queued skill timing, initial skill delay, can-use-next-skill gate, chain skill selection, `canUseNextChain`, Java shuffle/RNG parity, live NPC skill list ownership, live target resolution, target mutation, random target selection, XML loading, controller execution, Java runtime comparison, and live-client validation.
- Estimated overall migration completion: 66%

## Remaining Risks

- The selector is a deterministic represented projection, not live AI scheduling.
- Java `Collections.shuffle` behavior is intentionally not modeled in this unit.
- Queued skill handling, chain-skill selection, initial skill delay, can-use-next-skill, `canUseNextChain`, live NPC skill list ownership, and next-skill delay mutation remain separate or missing.
- Live target resolution, target mutation, random target selection, XML loading, controller execution, effects, and packets remain unwired.
- Reflection, threading, serialization, date/time runtime, precision/rounding, Java runtime, RNG, and live-client behavior remain unverified.

## Next Recommended Unit of Work

Continue by modeling the queued-skill branch and `nextSkillTime == 0` fast path as represented metadata, or add a chain-skill selection projection covering `hasChain`, `canUseNextChain`, priority/shuffle behavior, and max-chain timing. Keep Java shuffle/RNG, XML loading, live target mutation, effects, packets, controller execution, and live AI ownership explicit until supported.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6KB-Completion.md`
   - this handoff
3. Inspect Java `SkillAttackManager.chooseNextSkill`, `NpcSkillList`, `NpcSkillEntry`, `NpcSkillTemplateEntry`, and current C# summon skill execution service.
4. Inspect C# `PlayerSummonKnownObjectNpcSkillCandidate`, `PlayerSummonKnownObjectNpcSkillSelectionResult`, `PlayerSummonKnownObjectNpcSkillTemplateProjection`, timing/condition readiness records, and execution tests.
5. Implement one narrow queued-skill or chain-skill selection slice with Java breadcrumbs.
6. Add focused tests that state what is source-derived and what remains unverified.
7. Run focused tests, then full GameServer tests.
8. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
9. Create the next handoff document and commit the unit.
