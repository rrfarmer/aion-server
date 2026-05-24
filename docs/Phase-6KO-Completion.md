# Phase 6KO Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6KN and covers Session 789.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerSummonCastSpellServiceTests|GameServerConnectionCastSpellTests|PlayerSummonSkillExecutionServiceTests"`
  - Result: Passed, 56 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1380 tests.

## Recent Work Completed

### Session 789 - Represented NPC Skill Delayed Spawn Scheduler Boundary

- Re-inspected Java `NpcSkillTemplateEntry.fireOnEndCastEvents` delayed spawn scheduling path.
- Added `PlayerSummonKnownObjectNpcSkillPostSpawnScheduleResult` and status enum.
- Added `PlayerSummonSkillExecutionService.PreviewMercenaryNpcSkillPostSpawnSchedule`.
- Modeled:
  - missing post-spawn preview input;
  - immediate/no-spawn/owner-not-ready cases that should not schedule;
  - delayed spawn intent that would call `ThreadPoolManager.schedule`;
  - deterministic scheduled-at timestamp from supplied current time plus Java spawn delay;
  - delayed owner-alive recheck requirement.
- Kept actual `ThreadPoolManager.schedule`, background task execution, delayed owner-state recheck, cancellation, `SpawnEngine`, Java `Rnd.get`, Java heading/geometry math, packets, persistence, threading, serialization, and live-client validation unwired.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.skill.NpcSkillTemplateEntry.fireOnEndCastEvents` delayed branch | `PlayerSummonSkillExecutionService.PreviewMercenaryNpcSkillPostSpawnSchedule` | Scheduler Boundary Projection | Partial | Regression Tested | Needs Verification | Represents whether delayed post-spawn work would be scheduled and when. It does not execute a task, recheck owner state later, or call spawn engine. |
| `com.aionemu.gameserver.utils.ThreadPoolManager.schedule` | `PlayerSummonKnownObjectNpcSkillPostSpawnScheduleResult` | Scheduler Result DTO | Partial | Regression Tested | Needs Verification | Captures missing-preview, not-scheduled, and scheduled outcomes plus scheduled-at timestamp. Threading, cancellation, scheduler implementation, and date/time runtime behavior remain unverified. |
| `com.aionemu.gameserver.model.templates.npcskill.NpcSkillSpawn.getDelay` | `PlayerSummonKnownObjectNpcSkillSpawnMetadata.DelayMilliseconds` consumed by schedule preview | Static Data / Timing Dependency | Partial | Regression Tested | Needs Verification | Uses represented delay to calculate scheduled time. XML/JAXB loading, signed/overflow behavior, and Java runtime timing comparison remain missing. |
| delayed owner recheck in `NpcSkillTemplateEntry.fireOnEndCastEvents` lambda | `RequiresOwnerAliveRecheck` on schedule result | Owner State Recheck Projection | Partial | Regression Tested | Needs Verification | Records that Java would recheck owner dead/about-to-die before spawning. It does not execute delayed recheck or observe live owner state. |
| `com.aionemu.gameserver.spawnengine.SpawnEngine` after scheduled delay | not yet ported; still represented by post-spawn/schedule metadata | Spawn Engine Dependency | Not Started | Manual Only as explicit gap | Needs Verification | Scheduled result stops before live spawn engine. Spawn template creation, instance id propagation, packets, persistence, geometry, and client visibility remain missing. |

## Tests Added Or Updated

- `PlayerSummonSkillExecutionServiceTests.PreviewMercenaryNpcSkillPostSpawn_ProjectsJavaFireOnEndCastEvents`
  - Extended to validate missing-preview schedule result.
  - Validates immediate-spawn not-scheduled result.
  - Validates delayed-spawn scheduled result.
  - Validates scheduled-at time, delay metadata, and owner-alive recheck flag.
- These tests are source-derived from Java. They do not compare against Java runtime execution, scheduler execution, delayed owner state, cancellation, XML/static-data loading, reflection behavior, threading behavior, serialization behavior, date/time runtime behavior, precision/rounding behavior, spawn-engine behavior, packet behavior, persistence behavior, geometry behavior, or live-client validation.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 5
- Total artifacts ported or partially modeled in this handoff window: 1 represented NPC skill delayed-spawn scheduler boundary slice.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 5
- Total blocked/not-started artifacts: live `ThreadPoolManager` scheduling, task cancellation, delayed owner recheck execution, Java `Rnd.get`, heading conversion, distance offset math, `SpawnEngine.newSingleTimeSpawn`, `SpawnEngine.spawnObject`, instance id propagation, owner references, packets, persistence, Java runtime comparison, and live-client validation.
- Estimated overall migration completion: 66%

## Remaining Risks

- Scheduler behavior is represented metadata only; no delayed task is created or executed.
- Java delayed owner-alive recheck is recorded but not performed.
- Spawn execution remains blocked on `SpawnEngine`, Java RNG, heading/angle math, distance offset math, instance id propagation, owner references, visibility, packets, and persistence.
- XML/JAXB loading for `spawn_npc`, reflection behavior, serialization behavior, threading, date/time runtime behavior, precision/rounding, Java runtime, scheduler, geometry, and live-client behavior remain unverified.

## Next Recommended Unit of Work

Continue by starting a static-data loader adapter for represented `NpcSkillSpawn` XML fields or by modeling the spawn execution preview after the scheduled boundary, including represented world/instance/heading inputs and explicit Java random offset gaps. Keep live XML loading, `DataManager.SKILL_DATA` pruning, Java `Rnd.get`, Java geometry, live AI mutation, controller execution, effects, packets, spawn-engine execution, scheduler/date-time behavior, threading, serialization, and live-client validation explicit until supported.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6KN-Completion.md`
   - this handoff
3. Inspect Java `NpcSkillTemplateEntry.spawnNpc`, `SpawnEngine`, `PositionUtil.convertHeadingToAngle`, `Rnd`, and `NpcSkillSpawn`.
4. Inspect C# `PreviewMercenaryNpcSkillPostSpawn`, `PreviewMercenaryNpcSkillPostSpawnSchedule`, post-spawn records, and tests.
5. Implement one narrow represented static-data loader adapter or spawn execution preview slice with Java breadcrumbs.
6. Add focused tests that state what is source-derived and what remains unverified.
7. Run focused tests, then full GameServer tests.
8. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
9. Create the next handoff document and commit the unit.
