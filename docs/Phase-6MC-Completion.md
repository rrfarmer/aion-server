# Phase 6MC Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6MB and covers Session 829.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.
- `docs/commit-conventions.md` was requested previously but is still not present in the worktree. Use the existing concise commit-message style unless that file is added later.

## Parallel Work Discovery

Selected unit ownership:

| Agent | Scope | Allowed Files | Forbidden Files | Expected Output |
|---|---|---|---|---|
| Orchestrator | Java `SkillAttackManager.chooseNextSkill` analysis, post-spawn count regression coverage, Phase 6 docs | `PlayerSummonSkillExecutionServiceTests.cs`, Phase 6 docs | Production service files, project files, unrelated tests | Tests, docs, commit |
| Hume (explorer) | Java-only `NpcSkillEntry.fireOnEndCastEvents` / `NpcSkillTemplateEntry.spawnNpc` dependency analysis | none, report only | all source/docs | Read-only report |

Safe parallel candidates for a future session:
- Java-only analysis of `SkillAttackManager.performAttack` live adapter call boundary and exact dependency order.
- Java-only analysis of `SkillAttackManager.skillAction` controller/target mutation ordering.
- Independent documentation audit of NPC skill parity tables, with exclusive docs ownership.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerSummonSkillExecutionServiceTests|StaticDataNpcSkillTests"`
  - Result: Passed, 57 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1407 tests.

## Recent Work Completed

### Session 829 - Represented Post-Spawn Count Edge Coverage

- Re-read required orchestration, parallelization, parity-verification, Phase 6 progress, and latest Phase 6 handoff docs before selecting the unit.
- Performed parallel discovery and used Hume for read-only Java post-spawn dependency analysis while the orchestrator inspected Java `SkillAttackManager.chooseNextSkill` locally.
- Confirmed existing represented C# `SelectMercenaryNextNpcSkillCandidate` coverage already models the Java queued-skill branch order: cast substate first, immediate queued skill before delay gates, delayed queued skill after gates, chain before ordinary priority.
- Added regression coverage for Java `NpcSkillTemplateEntry.spawnNpc` count behavior where random count is required only when `max_count > 1`; `max_count == 1` remains fixed to `min_count`.
- Documented that Java `is_post_spawn` auto-cast behavior and `<spawn_npc>` end-cast spawning are separate. C# currently models metadata/previews only and does not live-spawn world NPCs from that hook.
- Kept live `NpcAI`, `ThreadPoolManager.schedule`, scheduler cancellation, `SpawnEngine`, controller execution, target mutation, effects, packets, persistence, threading, serialization, date/time behavior, reflection behavior, precision/rounding, and live-client validation unwired.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.ai.manager.SkillAttackManager.chooseNextSkill` | `PlayerSummonSkillExecutionService.SelectMercenaryNextNpcSkillCandidate` / existing branch-order tests | Service / Selection | Partial | Regression Tested as represented metadata | Needs Verification | Java queued/gate/chain/priority ordering was re-analyzed. Existing C# tests represent the ordering, but live `NpcAI`, wall-clock time, mutable skill lists, and Java shuffle behavior are not executed. |
| `com.aionemu.gameserver.ai.manager.SkillAttackManager.getNpcSkillEntryIfNotTooFarAway` | `EvaluateMercenaryTargetRange` / `ApplyMercenaryTargetRangeDelay` | Service / Target Range | Partial | Regression Tested as represented metadata | Needs Verification | Java sets a 5000 ms delay and returns null for invalid targets. C# models readiness/delay metadata, but live visibility, geometry, AI callbacks, mutation timing, packets, and client behavior remain unverified. |
| `com.aionemu.gameserver.ai.manager.SkillAttackManager.isReady` | `EvaluateMercenaryNpcSkillEntryReadiness` / `EvaluateMercenaryNpcSkillConditionReadiness` | Service / Readiness | Partial | Regression Tested as represented metadata | Needs Verification | Java readiness depends on HP/timing/chance, conditions, template lookup, abnormal states, and transform blockers. C# has represented slices only; live lookup/state sources and runtime comparison remain unverified. |
| `com.aionemu.gameserver.model.skill.NpcSkillEntry.fireOnEndCastEvents` | `PreviewMercenaryNpcSkillPostSpawn` / post-spawn previews | Interface / Hook | Partial | Regression Tested as metadata only | Needs Verification | C# previews end-cast spawn intent but does not live invoke the hook, schedule callbacks, or spawn world NPCs. |
| `com.aionemu.gameserver.model.skill.NpcSkillTemplateEntry.spawnNpc` | `PlayerSummonSkillExecutionServiceTests.PreviewMercenaryNpcSkillPostSpawn_ProjectsJavaFireOnEndCastEvents` | Service / Spawn Metadata | Partial | Regression Tested | Needs Verification | New coverage validates `max_count == 1` fixed-count behavior. Live `SpawnEngine`, Java RNG, delayed owner-position sampling, and scheduler behavior remain missing. |
| `com.aionemu.gameserver.model.templates.npcskill.NpcSkillTemplate` | `NpcSkillTable` / `PlayerSummonKnownObjectNpcSkillTemplateMetadata` | DTO / Static Data | Partial | Regression Tested through existing static-data tests | Needs Verification | Metadata fields are loaded/projected, but live auto-cast and end-cast spawn behavior are not wired. XML serialization edge cases remain possible. |
| `com.aionemu.gameserver.model.templates.npcskill.NpcSkillSpawn` | `PlayerSummonKnownObjectNpcSkillSpawnMetadata` | DTO | Partial | Regression Tested | Needs Verification | Delay, distance, and count inputs are represented. Java random inclusivity, owner-relative delayed placement, precision/rounding, and spawn object creation remain unverified. |
| `com.aionemu.gameserver.skillengine.model.Skill.endCast` | no live C# equivalent; represented post-spawn previews only | Runtime Hook | Not Started | Manual Only | Needs Verification | Java calls `lastSkill.fireOnEndCastEvents(npc)` for NPC casters. No live C# skill end-cast bridge was found in this unit. |
| `com.aionemu.gameserver.ai.handler.SpawnEventHandler` | no live C# equivalent; `IsPostSpawn` metadata only | AI Handler | Not Started | Manual Only | Needs Verification | Java auto-casts `is_post_spawn` skills on NPC spawn. C# metadata exists but no live handler parity. |
| `com.aionemu.gameserver.ai.handler.ReturningEventHandler` | no live C# equivalent; `IsPostSpawn` metadata only | AI Handler | Not Started | Manual Only | Needs Verification | Java auto-casts `is_post_spawn` skills when returning home. C# has no live returning-event handler parity yet. |

## Tests Added Or Updated

- `PlayerSummonSkillExecutionServiceTests.PreviewMercenaryNpcSkillPostSpawn_ProjectsJavaFireOnEndCastEvents`
  - Now validates that `MaxCount = 1` does not require random count and resolves fixed count to `MinCount`.
  - This expectation is source-derived from Java `NpcSkillTemplateEntry.spawnNpc`.
- Existing `SelectMercenaryNextNpcSkillCandidate_ComposesJavaChooseNextSkillBranchOrder` was re-confirmed as the represented queue/gate/chain/ordinary branch coverage.
- No Java runtime execution, live scheduler comparison, RNG comparison, live spawn-engine comparison, live `NpcAI` mutation comparison, controller comparison, reflection comparison, threading comparison, serialization comparison, date/time precision comparison, packet comparison, persistence comparison, or live-client validation was run.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 10
- Total artifacts ported or partially modeled in this handoff window: 0 new production artifacts; 1 represented post-spawn count regression slice expanded.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 10
- Total blocked/not-started artifacts: 20 blocked/not-started categories, including live `SkillAttackManager.performAttack`, live `SkillAttackManager.skillAction`, live `SkillAttackManager.chooseNextSkill`, live `Skill.endCast`, live `SpawnEventHandler`, live `ReturningEventHandler`, live `ThreadPoolManager.schedule`, scheduler cancellation, live `SpawnEngine`, live `Npc`, live `NpcAI`, controller execution, target mutation, post-spawn execution, effect application, packet fanout, persistence, threading/serialization, date/time precision, and live-client validation.
- Estimated overall migration completion: 66%

## Remaining Risks

- The added coverage validates represented metadata and preview behavior only; it does not spawn NPCs, schedule callbacks, cancel tasks, mutate AI, call controllers, set targets, apply effects, persist state, or send packets.
- Java delayed post-spawn uses owner position/heading at callback execution time; C# only previews inputs and injected random/location facts.
- Java `chooseNextSkill` uses current wall-clock time, mutable NPC game stats, mutable skill lists, and Java collection shuffle ordering; C# represented selectors use explicit inputs and deterministic ordering for tests.
- `is_post_spawn` auto-cast and `<spawn_npc>` end-cast spawning remain separate live-unwired behaviors.
- Threading, serialization, reflection behavior, precision/rounding, Java RNG inclusivity, packet order, and live-client behavior remain missing or unverified.

## Next Sequential Task

Continue NPC skill action parity by adding a non-executing live adapter interface contract for the first future `SkillAttackManager.performAttack` integration boundary. Keep the contract limited to dependency readiness and operation intents unless `NpcAI`, scheduler, controller, and packet/spawn dependencies have concrete C# homes.

## Safe Parallel Candidates

| Candidate | Allowed Files | Forbidden Files | Notes |
|---|---|---|---|
| Java performAttack adapter-boundary analysis | none, report only | all source/docs | Analyze exact call order and dependencies before adding any future adapter boundary. |
| Java skillAction mutation-order analysis | none, report only | all source/docs | Analyze target setting, controller use-skill, and failure callbacks without edits. |
| NPC skill parity doc audit | `docs/PHASE-6-PROGRESS.md` only if exclusively assigned | source/test files | Check tables for missing dependency notes; orchestrator must integrate and commit. |

## Resume Checklist

1. Confirm `git status --short --branch` on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/orchestration-rules.md`
   - `docs/parallelization-strategy.md`
   - `docs/parity-verification.md`
   - `docs/commit-conventions.md` if it exists
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6MB-Completion.md`
   - this handoff
3. Perform parallel work discovery and define a file ownership map before any sub-agent work.
4. Start with the non-executing live adapter interface contract unless a safer prerequisite analysis appears.
5. Keep unsupported live `NpcAI`, scheduler, controller, packet/effect/post-spawn execution, threading, serialization, date/time precision, and live-client behavior explicit.
6. Run focused tests, then full GameServer tests for any implementation unit.
7. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
8. Create the next handoff document with next sequential task and safe parallel candidates.
9. Commit the unit.
