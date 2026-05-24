# Phase 6KF Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6KE and covers Session 780.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerSummonCastSpellServiceTests|GameServerConnectionCastSpellTests|PlayerSummonSkillExecutionServiceTests"`
  - Result: Passed, 49 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1373 tests.

## Recent Work Completed

### Session 780 - Composed Represented Choose-Next-Skill Projection

- Re-inspected Java `SkillAttackManager.chooseNextSkill` top-level branch order.
- Added `SelectMercenaryNextNpcSkillCandidate` to compose represented queued, chain, and ordinary-priority selectors.
- Added `InCastSubState` selection status and `ChooseNextSkillGate` source.
- Modeled explicit inputs for:
  - AI cast substate;
  - initial-delay gate;
  - can-use-next-skill gate;
  - queued candidate;
  - last skill;
  - candidate list;
  - elapsed-since-last-skill time.
- Preserved Java branch order:
  - cast-substate gate;
  - immediate queued skill before delay/can-use gates;
  - delay/can-use gate;
  - delayed queued skill;
  - chain selection;
  - ordinary priority selection.
- Kept live `NpcAI`, `Npc.getNextQueuedSkill`, `Npc.getSkillList`, `NpcGameStats`, Java shuffle/RNG, `System.currentTimeMillis`, target mutation, effects, packets, controller execution, and scheduler integration unwired.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.ai.manager.SkillAttackManager.chooseNextSkill` | `PlayerSummonSkillExecutionService.SelectMercenaryNextNpcSkillCandidate` | AI Skill Selection Projection | Partial | Regression Tested | Needs Verification | Composes represented cast-substate, queued, delay/can-use, chain, and ordinary-priority paths in Java source order. Live `NpcAI`, `Npc`, and `NpcGameStats` remain missing. |
| `com.aionemu.gameserver.ai.AISubState.CAST` gate in `chooseNextSkill` | `PlayerSummonKnownObjectNpcSkillSelectionStatus.InCastSubState` / `ChooseNextSkillGate` | AI State Gate Projection | Partial | Regression Tested | Needs Verification | Represents early null return while casting. Live AI state transitions and threading remain unwired. |
| `com.aionemu.gameserver.model.gameobjects.Npc.getNextQueuedSkill` | explicit `queuedCandidate` input | Queued Skill Dependency | Partial | Regression Tested | Needs Verification | Consumes represented queued candidate but does not own or mutate live queued-skill state. |
| `com.aionemu.gameserver.model.skill.NpcSkillList` / `getChainSkills` / `getPriorities` | explicit `IEnumerable<PlayerSummonKnownObjectNpcSkillCandidate>` input routed through chain and ordinary selectors | Skill List Projection | Partial | Regression Tested | Needs Verification | Composes represented candidate lists but does not load or prune live NPC skill lists from static data. |
| `com.aionemu.gameserver.model.gameobjects.NpcGameStats.getInitialSkillDelay` / `canUseNextSkill` / `getLastSkillTime` | explicit `initialSkillDelayElapsed`, `canUseNextSkill`, and `elapsedSinceLastSkillMilliseconds` inputs | Live AI Timing Dependency | Not Started | Manual Only as input branches | Needs Verification | Live date/time arithmetic, scheduler state, `System.currentTimeMillis`, and stat mutation remain caller-supplied. |

## Tests Added Or Updated

- `PlayerSummonSkillExecutionServiceTests.SelectMercenaryNextNpcSkillCandidate_ComposesJavaChooseNextSkillBranchOrder`
  - Validates cast-substate gate.
  - Validates immediate queued precedence before delay gates.
  - Validates delay-gate blocking.
  - Validates delayed queued precedence after gates.
  - Validates chain precedence before ordinary priority.
  - Validates ordinary-priority fallback.
- These tests are source-derived from Java. They do not compare against Java runtime execution, live AI substate, live queued-skill ownership, live skill-list ownership, date/time runtime behavior, scheduler behavior, RNG/shuffle behavior, target mutation, reflection behavior, threading behavior, serialization behavior, precision/rounding behavior, or live-client validation.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 5
- Total artifacts ported or partially modeled in this handoff window: 1 represented composed `chooseNextSkill` selection slice.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 5
- Total blocked/not-started artifacts: live `NpcAI` scheduling, live queued-skill ownership, live skill-list ownership, static XML loading, missing skill pruning, fight-start timestamp/date-time source, last-skill timestamp source, `canUseNextSkill` state, Java shuffle/RNG parity, target mutation, controller execution, Java runtime comparison, scheduler integration, and live-client validation.
- Estimated overall migration completion: 66%

## Remaining Risks

- The composed selector is still represented service logic and is not invoked by live AI.
- Live NPC ownership, queued-skill state, skill-list loading/pruning, last-skill state, fight-start timestamps, scheduler state, and next-skill delay mutation remain unwired.
- Java `Collections.shuffle`, random target selection, and `nextSkillTime == -1` random timing remain intentionally unmodeled.
- Target mutation, controller execution, effects, packets, and live `SkillTemplate` use remain missing.
- Reflection, threading, serialization, date/time runtime, precision/rounding, Java runtime, RNG, scheduler, and live-client behavior remain unverified.

## Next Recommended Unit of Work

Continue by introducing a live-adapter boundary that can build represented NPC skill candidates from future C# NPC skill-list/static data and `NpcGameStats` state, or model the remaining target-selection mutation in `SkillAttackManager.skillAction` for `NpcSkillTargetAttribute` values. Keep XML loading, Java shuffle/RNG, live controller execution, effects, packets, scheduler/date-time behavior, and live-client validation explicit until supported.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6KE-Completion.md`
   - this handoff
3. Inspect Java `SkillAttackManager.chooseNextSkill`, `SkillAttackManager.skillAction`, `NpcSkillTargetAttribute`, `NpcSkillList`, `NpcSkillEntry`, and current C# summon skill execution service.
4. Inspect C# `SelectMercenaryNextNpcSkillCandidate`, queued/chain/ordinary selector methods, `PlayerSummonKnownObjectNpcSkillCandidate`, and selection result/source/status records.
5. Implement one narrow live-adapter or target-selection-mutation slice with Java breadcrumbs.
6. Add focused tests that state what is source-derived and what remains unverified.
7. Run focused tests, then full GameServer tests.
8. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
9. Create the next handoff document and commit the unit.
