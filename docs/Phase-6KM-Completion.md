# Phase 6KM Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6KL and covers Session 787.

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

### Session 787 - Represented NPC Skill Post-Spawn Preview

- Re-inspected Java `NpcSkillTemplateEntry.fireOnEndCastEvents`, `spawnNpc`, `NpcSkillSpawn`, `ThreadPoolManager`, `SpawnEngine`, and `Rnd` usage.
- Added `PlayerSummonKnownObjectNpcSkillSpawnMetadata`.
- Added `SpawnTemplate` to represented NPC skill template metadata/projection.
- Added `PlayerSummonKnownObjectNpcSkillPostSpawnPreview` and status enum.
- Added `PlayerSummonSkillExecutionService.PreviewMercenaryNpcSkillPostSpawn`.
- Modeled:
  - missing spawn template;
  - owner dead/about-to-die gate;
  - immediate spawn intent when Java delay is zero;
  - delayed scheduled spawn intent when Java delay is nonzero;
  - Java random count requirement when `max_count > 1`;
  - Java random distance and angle requirements when `min_distance > 0`.
- Kept live `ThreadPoolManager.schedule`, delayed owner-alive recheck execution, `SpawnEngine.newSingleTimeSpawn`, `SpawnEngine.spawnObject`, Java `Rnd.get`, Java heading/angle math, Java geo/position correction, packets, persistence, threading, serialization, and live-client validation unwired.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.skill.NpcSkillTemplateEntry.fireOnEndCastEvents` | `PlayerSummonSkillExecutionService.PreviewMercenaryNpcSkillPostSpawn` | Post-Cast Spawn Projection | Partial | Regression Tested | Needs Verification | Represents missing-spawn, owner-not-ready, immediate-spawn, and delayed-spawn outcomes. It does not schedule tasks, recheck owner state at execution time, or call spawn engine. |
| `com.aionemu.gameserver.model.templates.npcskill.NpcSkillSpawn` | `PlayerSummonKnownObjectNpcSkillSpawnMetadata` | Static Data DTO | Partial | Regression Tested | Needs Verification | Models `npc_id`, `delay`, `min_distance`, `max_distance`, `min_count`, and `max_count` defaults. XML/JAXB loading, reflection behavior, and serialization behavior remain unverified. |
| `com.aionemu.gameserver.model.skill.NpcSkillTemplateEntry.spawnNpc` | `PlayerSummonKnownObjectNpcSkillPostSpawnPreview` derived random/count/distance flags | Spawn Intent Projection | Partial | Regression Tested | Needs Verification | Marks Java random count/distance/angle requirements and effective deterministic bounds. It does not execute `Rnd.get`, heading conversion, position math, spawn template creation, or object spawn. |
| `com.aionemu.gameserver.utils.ThreadPoolManager` delayed spawn scheduling | `ShouldScheduleSpawn` / `RequiresOwnerAliveRecheck` preview flags | Scheduler Dependency | Not Started | Regression Tested as metadata only | Needs Verification | Records that a delayed spawn would be scheduled and owner state rechecked. No task scheduling, threading, cancellation, date/time runtime comparison, or scheduler parity exists. |
| `com.aionemu.gameserver.spawnengine.SpawnEngine` | not yet ported; represented by post-spawn preview only | Spawn Engine Dependency | Not Started | Manual Only as explicit gap | Needs Verification | Live `newSingleTimeSpawn`, instance id handling, heading, owner reference, spawn object creation, packets, persistence, and client visibility remain missing. |

## Tests Added Or Updated

- `PlayerSummonSkillExecutionServiceTests.PreviewMercenaryNpcSkillPostSpawn_ProjectsJavaFireOnEndCastEvents`
  - Validates no-spawn-template return.
  - Validates owner-dead gate.
  - Validates immediate spawn intent.
  - Validates delayed spawn intent and owner recheck flag.
  - Validates Java random-count, random-distance, and random-angle flags.
  - Validates deterministic effective count/distance bounds.
- `PlayerSummonSkillExecutionServiceTests.ProjectMercenaryNpcSkillTemplate_MapsJavaTemplateDefaultsAndOverrides`
  - Updated to verify represented spawn metadata is preserved through template projection.
- These tests are source-derived from Java. They do not compare against Java runtime execution, XML/static-data loading, scheduler execution, random number output, heading/angle math, live spawn-engine behavior, geometry behavior, reflection behavior, threading behavior, serialization behavior, date/time runtime behavior, precision/rounding behavior, packet behavior, persistence behavior, or live-client validation.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 5
- Total artifacts ported or partially modeled in this handoff window: 1 represented NPC skill post-spawn preview slice.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 5
- Total blocked/not-started artifacts: live `ThreadPoolManager` scheduling, delayed owner recheck execution, Java `Rnd.get`, heading conversion, position math, `SpawnEngine.newSingleTimeSpawn`, `SpawnEngine.spawnObject`, instance id propagation, owner references, XML/static loading, Java geometry, packets, persistence, Java runtime comparison, and live-client validation.
- Estimated overall migration completion: 66%

## Remaining Risks

- Post-spawn behavior is represented metadata only and does not schedule or spawn live NPCs.
- Java delayed owner-alive recheck is only flagged, not executed.
- Java `Rnd.get`, heading conversion, angle math, distance offsets, `SpawnEngine.newSingleTimeSpawn`, `SpawnEngine.spawnObject`, instance id propagation, owner references, visibility, packets, and persistence remain missing.
- XML/JAXB loading for `spawn_npc`, static-data defaults, reflection behavior, serialization behavior, threading, date/time runtime behavior, precision/rounding, Java runtime, scheduler, geometry, and live-client behavior remain unverified.

## Next Recommended Unit of Work

Continue by adding a represented post-spawn capture field on `PlayerSummonKnownObject` or a service capture method that stores the latest `PreviewMercenaryNpcSkillPostSpawn` result beside the selection/action previews, or begin modeling the delayed spawn scheduler boundary without executing `SpawnEngine`. Keep XML loading, `DataManager.SKILL_DATA` pruning, Java `Rnd.get`, Java geometry, live AI mutation, controller execution, effects, packets, spawn-engine execution, scheduler/date-time behavior, threading, and live-client validation explicit until supported.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6KL-Completion.md`
   - this handoff
3. Inspect Java `NpcSkillTemplateEntry.fireOnEndCastEvents`, `NpcSkillSpawn`, `ThreadPoolManager`, `SpawnEngine`, and `Rnd`.
4. Inspect C# `PreviewMercenaryNpcSkillPostSpawn`, `PlayerSummonKnownObjectNpcSkillPostSpawnPreview`, preview capture fields, and tests.
5. Implement one narrow represented post-spawn capture or scheduler-boundary slice with Java breadcrumbs.
6. Add focused tests that state what is source-derived and what remains unverified.
7. Run focused tests, then full GameServer tests.
8. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
9. Create the next handoff document and commit the unit.
