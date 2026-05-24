# Phase 6KP Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6KO and covers Session 790.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerSummonCastSpellServiceTests|GameServerConnectionCastSpellTests|PlayerSummonSkillExecutionServiceTests"`
  - Result: Passed, 57 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1381 tests.

## Recent Work Completed

### Session 790 - Represented NPC Skill Spawn Execution Preview

- Re-inspected Java `NpcSkillTemplateEntry.spawnNpc`, `NpcSkillSpawn`, `SpawnEngine`, `PositionUtil`, and `Rnd` usage after the represented scheduler boundary.
- Added `PlayerSummonKnownObjectNpcSkillSpawnOrigin`.
- Added `PlayerSummonKnownObjectNpcSkillSpawnExecutionPreview` and status enum.
- Added `PlayerSummonSkillExecutionService.PreviewMercenaryNpcSkillPostSpawnExecution`.
- Modeled:
  - missing post-spawn preview input;
  - no-spawn-template results;
  - owner-dead/about-to-die gates at execution time;
  - missing represented origin data;
  - immediate or delayed spawn execution intent using owner world/instance/position/heading inputs;
  - Java random-count, random-distance, random-angle, owner-position, instance-spawn, and spawn-engine requirements.
- Kept Java `Rnd.get`, `Rnd.nextFloat`, `PositionUtil.convertHeadingToAngle`, trigonometric offset calculation, `SpawnEngine.newSingleTimeSpawn`, `SpawnEngine.spawnObject`, live instance object creation, packets, persistence, threading, serialization, geometry, and live-client validation unwired.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.skill.NpcSkillTemplateEntry.spawnNpc` | `PlayerSummonSkillExecutionService.PreviewMercenaryNpcSkillPostSpawnExecution` | Spawn Execution Projection | Partial | Regression Tested | Needs Verification | Represents missing preview, no-spawn, owner-not-ready, missing-origin, and would-spawn outcomes. It does not execute Java RNG, heading conversion, offset math, spawn-template creation, or object spawning. |
| `com.aionemu.gameserver.model.gameobjects.Npc` world/instance/position/heading inputs | `PlayerSummonKnownObjectNpcSkillSpawnOrigin` | Spawn Origin DTO | Partial | Regression Tested | Needs Verification | Models world id, instance id, X/Y/Z, and heading inputs consumed by Java `SpawnEngine.newSingleTimeSpawn`. Live `Npc` state, coordinate precision, heading conversion, synchronization, threading, and serialization remain unverified. |
| `com.aionemu.gameserver.model.templates.npcskill.NpcSkillSpawn` count/distance/random fields | `PlayerSummonKnownObjectNpcSkillSpawnExecutionPreview` derived flags and bounds | Static Data / Randomization Dependency | Partial | Regression Tested | Needs Verification | Exposes deterministic count/distance bounds and flags Java random count, random distance, and random angle requirements. It does not call `Rnd.get`, `Rnd.nextFloat`, or compare random distributions against Java. |
| `com.aionemu.gameserver.utils.PositionUtil.convertHeadingToAngle` plus Java `Math.cos`/`Math.sin` offset path | `RequiresRandomAngle` / `RequiresRandomDistance` metadata only | Geometry Dependency | Not Started | Manual Only as explicit gap | Needs Verification | Records that Java angle/distance math is needed when `min_distance > 0`, but does not calculate offsets, apply Java heading conversion, correct geo Z, or compare precision/rounding. |
| `com.aionemu.gameserver.spawnengine.SpawnEngine.newSingleTimeSpawn` / `spawnObject` | `RequiresSpawnEngine` / `RequiresInstanceSpawn` metadata only | Spawn Engine Dependency | Not Started | Manual Only as explicit gap | Needs Verification | Records that live spawn engine and instance spawn are required. Spawn template creation, owner references, object creation, packets, persistence, visibility, and client validation remain missing. |
| delayed owner recheck in `NpcSkillTemplateEntry.fireOnEndCastEvents` lambda | `PreviewMercenaryNpcSkillPostSpawnExecution(... ownerIsDead/ownerIsAboutToDie ...)` | Owner State Recheck Projection | Partial | Regression Tested | Needs Verification | Can reject represented delayed execution when owner state is not spawn-ready. It does not execute a scheduled callback or observe live owner state after delay. |

## Tests Added Or Updated

- `PlayerSummonSkillExecutionServiceTests.PreviewMercenaryNpcSkillPostSpawnExecution_ProjectsJavaSpawnNpcInputs`
  - Validates missing-preview and no-spawn outcomes.
  - Validates missing-origin rejection.
  - Validates immediate would-spawn metadata.
  - Validates delayed random would-spawn metadata.
  - Validates delayed owner-dead rejection.
  - Validates spawn-engine/instance-spawn flags, owner-origin preservation, NPC id propagation, deterministic count/distance bounds, random-count/random-distance/random-angle flags, and delayed owner-recheck metadata.
- These tests are source-derived from Java. They do not compare against Java runtime execution, random number generation, heading/angle math, trigonometric offset math, geometry, live spawn-engine behavior, live instance behavior, packets, persistence, XML/static-data loading, reflection behavior, threading behavior, serialization behavior, date/time runtime behavior, precision/rounding behavior, or live-client validation.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 6
- Total artifacts ported or partially modeled in this handoff window: 1 represented NPC skill spawn execution preview slice.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 6
- Total blocked/not-started artifacts: live `ThreadPoolManager` callback execution, delayed owner recheck execution, Java `Rnd.get`, Java `Rnd.nextFloat`, heading conversion, distance offset math, coordinate precision/rounding, geo correction, `SpawnEngine.newSingleTimeSpawn`, `SpawnEngine.spawnObject`, instance id propagation, owner references, packets, persistence, Java runtime comparison, and live-client validation.
- Estimated overall migration completion: 66%

## Remaining Risks

- Spawn execution is represented metadata only; no live NPC is created.
- Java `Rnd.get`, `Rnd.nextFloat`, heading conversion, `Math.cos`/`Math.sin` offset math, coordinate precision/rounding, geo Z/collision correction, `SpawnEngine.newSingleTimeSpawn`, `SpawnEngine.spawnObject`, instance id propagation, owner references, packets, persistence, and client visibility remain missing.
- Delayed owner-alive recheck can be modeled by input flags, but no scheduled callback or live owner-state observation exists.
- XML/JAXB loading for `spawn_npc`, `DataManager.SKILL_DATA` pruning, reflection behavior, serialization behavior, threading, date/time runtime behavior, Java runtime, scheduler, geometry, and live-client behavior remain unverified.

## Next Recommended Unit of Work

Continue by starting a static-data loader adapter for represented `NpcSkillSpawn` XML fields or by adding a deterministic Java-heading/offset calculation preview for `spawnNpc` that keeps Java `Rnd.get`/`Rnd.nextFloat` injectable and unverified until runtime comparison exists. Keep live XML loading, `DataManager.SKILL_DATA` pruning, Java geometry, spawn-engine execution, live AI mutation, controller execution, effects, packets, scheduler/date-time behavior, threading, serialization, and live-client validation explicit until supported.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6KO-Completion.md`
   - this handoff
3. Inspect Java `NpcSkillTemplateEntry.spawnNpc`, `SpawnEngine`, `PositionUtil.convertHeadingToAngle`, `Rnd`, and `NpcSkillSpawn`.
4. Inspect C# `PreviewMercenaryNpcSkillPostSpawn`, `PreviewMercenaryNpcSkillPostSpawnSchedule`, `PreviewMercenaryNpcSkillPostSpawnExecution`, spawn-origin/execution-preview records, and tests.
5. Implement one narrow represented static-data loader adapter or deterministic spawn-offset preview slice with Java breadcrumbs.
6. Add focused tests that state what is source-derived and what remains unverified.
7. Run focused tests, then full GameServer tests.
8. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
9. Create the next handoff document and commit the unit.
