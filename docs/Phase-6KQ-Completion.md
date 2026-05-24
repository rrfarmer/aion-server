# Phase 6KQ Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6KP and covers Session 791.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerSummonCastSpellServiceTests|GameServerConnectionCastSpellTests|PlayerSummonSkillExecutionServiceTests"`
  - Result: Passed, 58 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1382 tests.

## Recent Work Completed

### Session 791 - Deterministic NPC Skill Spawn Location Preview

- Re-inspected Java `PositionUtil.convertHeadingToAngle`, `PositionUtil.normalizeAngle`, `NpcSkillTemplateEntry.spawnNpc`, and `SpawnEngine.newSingleTimeSpawn`.
- Added `PlayerSummonKnownObjectNpcSkillSpawnLocationPreview` and status enum.
- Added `PlayerSummonSkillExecutionService.PreviewMercenaryNpcSkillPostSpawnLocation`.
- Modeled:
  - missing execution input;
  - non-spawnable execution results;
  - Java heading-to-angle conversion as `normalizeAngle(heading * 3f)`;
  - no-offset spawns when Java `min_distance <= 0`;
  - required injected random angle when Java `min_distance > 0`;
  - required injected random distance when Java `max_distance > 0`;
  - deterministic `Math.cos`/`Math.sin` offset projection with Java-style float casts;
  - preservation of origin Z, heading, world id, and instance id for future `SpawnEngine.newSingleTimeSpawn`.
- Kept Java RNG generation, random distribution comparison, live heading/geometry comparison, geo Z/collision correction, spawn-engine execution, packets, persistence, threading, serialization, and live-client validation unwired.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.skill.NpcSkillTemplateEntry.spawnNpc` offset path | `PlayerSummonSkillExecutionService.PreviewMercenaryNpcSkillPostSpawnLocation` | Spawn Location Projection | Partial | Regression Tested | Needs Verification | Projects deterministic no-offset and random-offset coordinates when random inputs are supplied. It does not call Java RNG, create spawn templates, or spawn objects. |
| `com.aionemu.gameserver.utils.PositionUtil.convertHeadingToAngle` | private C# heading conversion inside `PreviewMercenaryNpcSkillPostSpawnLocation` | Geometry Utility | Partial | Regression Tested | Needs Verification | Mirrors `normalizeAngle(heading * 3f)` for byte heading inputs. Java runtime comparison and broader signed-byte edge cases remain unverified. |
| `com.aionemu.gameserver.utils.PositionUtil.normalizeAngle` | private C# normalization path | Geometry Utility | Partial | Regression Tested | Needs Verification | Covers represented byte headings used by spawn origin. General negative and multi-turn normalization behavior is not fully ported. |
| `com.aionemu.commons.utils.Rnd.nextFloat(360f)` | `randomAngleDegrees` injected parameter and `MissingRandomAngle` status | Randomization Dependency | Partial | Regression Tested | Needs Verification | Requires explicit angle input rather than generating Java RNG. Random distribution, bounds, seeding, and runtime comparison remain missing. |
| `com.aionemu.commons.utils.Rnd.get(minDistance, maxDistance)` | `randomDistance` injected parameter and `MissingRandomDistance` status | Randomization Dependency | Partial | Regression Tested | Needs Verification | Requires explicit distance input when Java would call `Rnd.get`. Inclusive bounds, distribution, seeding, and runtime comparison remain missing. |
| `com.aionemu.gameserver.spawnengine.SpawnEngine.newSingleTimeSpawn` coordinate parameters | `PlayerSummonKnownObjectNpcSkillSpawnLocationPreview` projected world/instance/X/Y/Z/heading metadata | Spawn Template Input Projection | Partial | Regression Tested | Needs Verification | Prepares represented coordinates but does not create `SpawnTemplate`, carry creator/event template metadata, call `spawnObject`, emit packets, persist, or validate client visibility. |

## Tests Added Or Updated

- `PlayerSummonSkillExecutionServiceTests.PreviewMercenaryNpcSkillPostSpawnLocation_ProjectsJavaSpawnNpcOffsetsWithInjectedRandomInputs`
  - Validates missing execution and not-spawnable execution.
  - Validates no-offset projection.
  - Validates heading angle conversion for heading 90 to 270 degrees.
  - Validates owner world/instance/heading preservation.
  - Validates missing random angle and missing random distance requirements.
  - Validates fixed-distance and random-distance offset projection.
  - Validates Java-style Z preservation.
- These tests are source-derived from Java. They do not compare against Java runtime execution, RNG behavior, signed-byte edge behavior, broad normalization behavior, live geometry, geo correction, spawn-engine behavior, packets, persistence, XML/static-data loading, reflection behavior, threading behavior, serialization behavior, date/time runtime behavior, broad precision/rounding behavior, or live-client validation.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 6
- Total artifacts ported or partially modeled in this handoff window: 1 represented NPC skill spawn location projection slice.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 6
- Total blocked/not-started artifacts: Java `Rnd.nextFloat`, Java `Rnd.get`, RNG bounds/distribution/seeding, signed-byte heading edges, full angle normalization, precision/rounding, geo correction, `SpawnEngine.newSingleTimeSpawn`, `SpawnEngine.spawnObject`, owner/event-template metadata, instance id propagation, packets, persistence, Java runtime comparison, XML/static loading, scheduler callback execution, and live-client validation.
- Estimated overall migration completion: 66%

## Remaining Risks

- Spawn location projection still does not execute Java RNG; random angle and distance are supplied by the caller.
- Java `Rnd.nextFloat`, `Rnd.get`, inclusive bounds, seeding, random distributions, signed-byte heading edge cases, full normalization behavior, precision/rounding, geo Z/collision correction, `SpawnEngine.newSingleTimeSpawn`, `SpawnEngine.spawnObject`, owner/event-template metadata, packets, persistence, visibility, and live instance behavior remain missing.
- XML/JAXB loading for `spawn_npc`, `DataManager.SKILL_DATA` pruning, reflection behavior, serialization behavior, threading, date/time runtime behavior, Java runtime, scheduler, geometry, and live-client behavior remain unverified.

## Next Recommended Unit of Work

Continue by starting a static-data loader adapter for represented `NpcSkillSpawn` XML fields or by modeling a spawn-template creation preview that carries creator id/event-template gaps explicitly before live `SpawnEngine` execution. Keep live XML loading, `DataManager.SKILL_DATA` pruning, Java RNG runtime comparison, Java geometry, spawn-engine execution, live AI mutation, controller execution, effects, packets, scheduler/date-time behavior, threading, serialization, and live-client validation explicit until supported.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6KP-Completion.md`
   - this handoff
3. Inspect Java `NpcSkillTemplateEntry.spawnNpc`, `SpawnEngine.newSingleTimeSpawn`, `SpawnEngine.spawnObject`, `SpawnTemplate`, `SpawnGroup`, `PositionUtil`, `Rnd`, and `NpcSkillSpawn`.
4. Inspect C# post-spawn preview, schedule preview, execution preview, location preview records/methods, and tests.
5. Implement one narrow represented static-data loader adapter or spawn-template creation preview slice with Java breadcrumbs.
6. Add focused tests that state what is source-derived and what remains unverified.
7. Run focused tests, then full GameServer tests.
8. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
9. Create the next handoff document and commit the unit.
