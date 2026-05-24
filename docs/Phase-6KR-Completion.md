# Phase 6KR Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6KQ and covers Session 792.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerSummonCastSpellServiceTests|GameServerConnectionCastSpellTests|PlayerSummonSkillExecutionServiceTests"`
  - Result: Passed, 59 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1383 tests.

## Recent Work Completed

### Session 792 - Represented NPC Skill Spawn Template Creation Preview

- Re-inspected Java `SpawnEngine.newSingleTimeSpawn`, `SpawnEngine.newSpawn`, `SpawnTemplate`, and `SpawnGroup`.
- Added `PlayerSummonKnownObjectNpcSkillSpawnTemplatePreview` and status enum.
- Added `PlayerSummonSkillExecutionService.PreviewMercenaryNpcSkillSpawnTemplate`.
- Extended `PlayerSummonKnownObjectNpcSkillSpawnOrigin` with represented creator object id.
- Modeled:
  - missing spawn-location input;
  - non-ready location previews;
  - represented `SpawnGroup(worldId, npcId, respawnTime, eventTemplate)` inputs;
  - represented `SpawnTemplate(..., x, y, z, heading, randWalk, walkerId, staticId, creatorId, aiName)` inputs;
  - Java no-respawn single-time spawn behavior;
  - Java `creator.getSpawn().getEventTemplate()` carry behavior as explicit metadata;
  - the required future `SpawnEngine.spawnObject(template, instanceId)` call.
- Kept live `SpawnGroup`, live `SpawnTemplate`, event-template object identity, `SpawnEngine.spawnObject`, `VisibleObjectSpawner`, temporary-spawn registration, instance handler `onSpawn`, packets, persistence, threading, serialization, and live-client validation unwired.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.spawnengine.SpawnEngine.newSingleTimeSpawn(int, int, float, float, float, byte, VisibleObject, String)` | `PlayerSummonSkillExecutionService.PreviewMercenaryNpcSkillSpawnTemplate` | Spawn Template Creation Projection | Partial | Regression Tested | Needs Verification | Represents the creator overload used by `NpcSkillTemplateEntry.spawnNpc`, including creator id, null AI name, no respawn, and optional creator event-template carry. It does not instantiate Java/C# live spawn objects. |
| `com.aionemu.gameserver.spawnengine.SpawnEngine.newSpawn` | `PlayerSummonKnownObjectNpcSkillSpawnTemplatePreview` | Spawn Group/Template Input DTO | Partial | Regression Tested | Needs Verification | Exposes represented `SpawnGroup` and `SpawnTemplate` constructor inputs. Live constructor side effects, synchronized `SpawnGroup.addSpawnTemplate`, and event-template object identity remain missing. |
| `com.aionemu.gameserver.model.templates.spawns.SpawnGroup` | `PlayerSummonKnownObjectNpcSkillSpawnTemplatePreview` group fields | Spawn Group Dependency | Partial | Regression Tested | Needs Verification | Models world id, NPC id, respawn time, and event-template presence only. Pool state, difficulty id, handler type, temporary spawn, list synchronization, and JAXB/static-data behavior are not ported in this slice. |
| `com.aionemu.gameserver.model.templates.spawns.SpawnTemplate` | `PlayerSummonKnownObjectNpcSkillSpawnTemplatePreview` template fields | Spawn Template Dependency | Partial | Regression Tested | Needs Verification | Models X/Y/Z, heading, random-walk range, walker id, static id, creator id, and AI name. Live `addTemplate`, walker index, anchor, state, temporary spawn inheritance, and serialization remain missing. |
| `com.aionemu.gameserver.model.gameobjects.VisibleObject.getObjectId` creator copy | `PlayerSummonKnownObjectNpcSkillSpawnOrigin.CreatorObjectId` | Creator Metadata Projection | Partial | Regression Tested | Needs Verification | Carries represented creator id into the template preview. Live visible-object references, spawn references, event-template object references, threading, and serialization remain unverified. |
| `com.aionemu.gameserver.spawnengine.SpawnEngine.spawnObject` | `RequiresSpawnObjectCall` / `WouldSpawnObject` metadata | Spawn Execution Dependency | Not Started | Manual Only as explicit gap | Needs Verification | Records that Java would next call `spawnObject(template, npc.getInstanceId())`. Live object type selection, temporary-spawn registration, world insertion, instance handler `onSpawn`, packets, persistence, and client visibility remain missing. |

## Tests Added Or Updated

- `PlayerSummonSkillExecutionServiceTests.PreviewMercenaryNpcSkillSpawnTemplate_ProjectsJavaNewSingleTimeSpawnInputs`
  - Validates missing-location and not-ready location results.
  - Validates created template preview.
  - Validates no-respawn single-time metadata.
  - Validates required future spawn-object call.
  - Validates world id, NPC id, projected X/Y/Z, heading, instance id, creator id, random-walk `0`, walker id `null`, static id `0`, and AI name `null`.
  - Validates creator event-template carry behavior only when the represented creator has a spawn with an event template.
- These tests are source-derived from Java. They do not compare against Java runtime execution, live constructor behavior, event-template identity, synchronized `SpawnGroup` list mutation, spawn-object dispatch, `VisibleObjectSpawner`, temporary-spawn behavior, instance handler behavior, packets, persistence, XML/static-data loading, reflection behavior, threading behavior, serialization behavior, date/time runtime behavior, broad precision/rounding behavior, or live-client validation.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 6
- Total artifacts ported or partially modeled in this handoff window: 1 represented NPC skill spawn-template creation preview slice.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 6
- Total blocked/not-started artifacts: live `SpawnGroup`, live `SpawnTemplate`, constructor side effects, synchronized list mutation, event-template object identity, temporary-spawn inheritance, handler/pool/difficulty behavior, `SpawnEngine.spawnObject`, `VisibleObjectSpawner`, temporary-spawn registration, world insertion, instance handler `onSpawn`, packets, persistence, Java runtime comparison, XML/static loading, threading/serialization, and live-client validation.
- Estimated overall migration completion: 66%

## Remaining Risks

- Spawn-template creation is represented metadata only and does not instantiate live `SpawnGroup` or `SpawnTemplate` objects.
- Java constructor side effects, synchronized spawn-template list mutation, event-template object identity, temporary-spawn inheritance, walker/static spawn metadata beyond defaults, handler/pool/difficulty behavior, and live `SpawnEngine.spawnObject` remain missing.
- `VisibleObjectSpawner`, temporary-spawn registration, world insertion, instance handler `onSpawn`, packets, persistence, visibility, threading, serialization, XML/static-data loading, reflection behavior, Java runtime behavior, scheduler callback execution, geometry, precision/rounding, and live-client behavior remain unverified.

## Next Recommended Unit of Work

Continue by starting a static-data loader adapter for represented `NpcSkillSpawn` XML fields or by modeling the `SpawnEngine.spawnObject` dispatch preview for NPC/gatherable/rift/siege/vortex/ordinary NPC branches before live `VisibleObjectSpawner` integration. Keep live XML loading, `DataManager.SKILL_DATA` pruning, Java RNG runtime comparison, Java geometry, live spawn-engine execution, live AI mutation, controller execution, effects, packets, scheduler/date-time behavior, threading, serialization, and live-client validation explicit until supported.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6KQ-Completion.md`
   - this handoff
3. Inspect Java `SpawnEngine.spawnObject`, `SpawnEngine.getSpawnedObject`, `VisibleObjectSpawner`, `SpawnTemplate`, `SpawnGroup`, and `NpcSkillTemplateEntry.spawnNpc`.
4. Inspect C# post-spawn preview, execution preview, location preview, spawn-template preview records/methods, and tests.
5. Implement one narrow represented static-data loader adapter or spawn-object dispatch preview slice with Java breadcrumbs.
6. Add focused tests that state what is source-derived and what remains unverified.
7. Run focused tests, then full GameServer tests.
8. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
9. Create the next handoff document and commit the unit.
