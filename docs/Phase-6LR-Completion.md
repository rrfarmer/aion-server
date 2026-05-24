# Phase 6LR Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6LQ and covers Session 818.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerSummonSkillExecutionServiceTests|StaticDataNpcSkillTests"`
  - Result: Passed, 53 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1403 tests.

## Recent Work Completed

### Session 818 - Represented SkillAttackManager Cycle Snapshot

- Re-inspected the represented C# preview storage boundary and Java `SkillAttackManager.chooseNextSkill` / `performAttack` / `skillAction` flow.
- Added `ProjectMercenaryNpcSkillAttackCycle` overloads for a player/mercenary object id and a represented known object.
- Added `PlayerSummonKnownObjectNpcSkillAttackCycleSnapshot` and `PlayerSummonKnownObjectNpcSkillAttackCycleSnapshotStatus`.
- Grouped retained represented skill-list projection, selection preview, action preview, post-spawn preview, action workflow preview, `performAttack` preview, execution preview, and scheduler-callback outcome metadata into one higher-level cycle snapshot for future live-AI integration.
- Kept live `SkillAttackManager.performAttack`, live `SkillAttackManager.skillAction`, live `ThreadPoolManager.schedule`, cancellation, live `NpcAI` mutation, controller execution, target mutation, packets, persistence, threading, serialization, date/time behavior, reflection behavior, precision/rounding, and live-client validation unwired.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.ai.manager.SkillAttackManager.chooseNextSkill` | `PlayerSummonKnownObjectNpcSkillAttackCycleSnapshot.SkillListProjection` / `SelectionPreview` | Service State | Partial | Regression Tested | Needs Verification | C# groups previously retained represented candidate-list and selection metadata into a cycle snapshot. It does not run Java random/priority selection live, mutate queued skills, compare Java runtime behavior, or validate live timing. |
| `com.aionemu.gameserver.ai.manager.SkillAttackManager.performAttack` | `PlayerSummonSkillExecutionService.ProjectMercenaryNpcSkillAttackCycle` / `PerformAttackPreview` / `PerformAttackExecutionPreview` | Service | Partial | Regression Tested | Needs Verification | C# groups represented pre-action and execution metadata. It does not run live `performAttack`, mutate `NpcAI` substate, schedule work, cancel tasks, abort casts, or compare runtime Java behavior. |
| `com.aionemu.gameserver.ai.manager.SkillAttackManager.skillAction` | `PlayerSummonKnownObjectNpcSkillAttackCycleSnapshot.ActionWorkflowPreview` / `SchedulerCallbackOutcome` | Service State | Partial | Regression Tested | Needs Verification | C# groups represented workflow and scheduler-callback outcome metadata, including projected action result. It does not execute live `skillAction`, controller behavior, target mutation, effects, AI events, or packets. |
| `com.aionemu.gameserver.model.skill.NpcSkillEntry.fireOnEndCastEvents` | `PlayerSummonKnownObjectNpcSkillAttackCycleSnapshot.PostSpawnPreview` | Service State | Partial | Regression Tested as metadata only | Needs Verification | C# groups represented post-spawn metadata. It does not execute summon/spawn handlers, schedule delayed spawns, serialize spawn state, or compare Java runtime event ordering. |
| `com.aionemu.gameserver.model.gameobjects.Npc` | `PlayerSummonKnownObject` cycle snapshot source state | World Object DTO / Storage | Partial | Regression Tested | Needs Verification | C# reads immutable represented known-object snapshots. Live `Npc`, `NpcGameStats`, object identity, synchronization/threading, serialization, persistence, and packet-visible behavior remain unverified. |
| `com.aionemu.gameserver.ai.NpcAI` | represented cycle snapshot metadata | AI Dependency | Not Started | Regression Tested as metadata only | Needs Verification | C# groups data needed by a future live-AI bridge only. Live AI state, substate transitions, event ordering, reflection behavior, threading, serialization, and packets remain missing. |
| `com.aionemu.gameserver.utils.ThreadPoolManager.schedule` | represented cycle `PerformAttackExecutionPreview` / `SchedulerCallbackOutcome` | Scheduler Dependency | Not Started | Regression Tested as metadata only | Needs Verification | C# groups pending/due/invoked callback metadata only. It does not enqueue work, cancel tasks, compare Java scheduler timing, validate date/time precision, or execute callbacks. |

## Tests Added Or Updated

- `PlayerSummonSkillExecutionServiceTests.ProjectMercenaryNpcSkillAttackCycle_GroupsStoredPreviewSnapshots`
  - Validates missing known-object handling.
  - Validates captured status, complete represented-cycle detection, and workflow invocation flag.
  - Validates exact reference preservation for list, selection, action, post-spawn, workflow, `performAttack`, execution, and scheduler-callback snapshots.
- These tests are source-derived from Java `SkillAttackManager.chooseNextSkill`, `performAttack`, `skillAction`, and represented preview-state boundaries. They do not compare against Java runtime execution, live scheduler behavior, cancellation, live AI mutation, live `skillAction`, controller behavior, reflection behavior, threading behavior, serialization behavior, date/time precision, packets, persistence, or live-client validation.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 7
- Total artifacts ported or partially modeled in this handoff window: 1 represented `SkillAttackManager` cycle snapshot aggregation slice.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 7
- Total blocked/not-started artifacts: live `SkillAttackManager.performAttack`, live `SkillAttackManager.skillAction`, live `SkillAttackManager.chooseNextSkill`, live `ThreadPoolManager.schedule`, scheduler cancellation, live `Npc`, live `NpcAI`, controller execution, target mutation, post-spawn execution, effect application, packet fanout, persistence, threading/serialization, date/time precision, and live-client validation.
- Estimated overall migration completion: 66%

## Remaining Risks

- The cycle snapshot is aggregation metadata only; it does not schedule, execute, cancel, mutate AI, call controllers, set targets, apply effects, persist state, or send packets.
- Java scheduler timing/cancellation, callback thread ordering, AI state/event ordering, object identity, synchronization/threading behavior, serialization, persistence, reflection behavior, precision/rounding, packet order, and live-client behavior remain missing or unverified.

## Next Recommended Unit of Work

Continue NPC skill action parity by adding a represented live-AI adapter readiness check that validates a `SkillAttackManager` cycle snapshot has the minimum retained metadata needed for future live invocation and reports missing list, selection, workflow, `performAttack`, execution, scheduler callback, and post-spawn pieces explicitly. Keep live `NpcAI`, `ThreadPoolManager`, cancellation, controller execution, packets, threading, serialization, and live-client validation explicit until supported.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6LQ-Completion.md`
   - this handoff
3. Inspect `PlayerSummonKnownObjectNpcSkillAttackCycleSnapshot`, `ProjectMercenaryNpcSkillAttackCycle`, and Java `SkillAttackManager.chooseNextSkill` / `performAttack` / `skillAction`.
4. Add represented live-AI adapter readiness metadata that reports which cycle snapshot pieces are present or missing before future live invocation.
5. Keep unsupported live `NpcAI`, `ThreadPoolManager`, cancellation, controller execution, packets, threading, serialization, date/time precision, and live-client behavior explicit.
6. Add focused tests that state what is source-derived and what remains unverified.
7. Run focused tests, then full GameServer tests.
8. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
9. Create the next handoff document and commit the unit.
