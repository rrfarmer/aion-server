# Phase 6KV Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6KU and covers Session 796.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerSummonCastSpellServiceTests|GameServerConnectionCastSpellTests|PlayerSummonSkillExecutionServiceTests"`
  - Result: Passed, 63 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1387 tests.

## Recent Work Completed

### Session 796 - Represented Post-Spawn Callback Preview

- Re-inspected Java `SpawnEngine.spawnObject`, `TemporarySpawnEngine.registerSpawned`, `VisibleObject.isSpawned`, `VisibleObject.getPosition`, and instance handler `onSpawn` callback usage.
- Added `PlayerSummonKnownObjectNpcSkillPostSpawnCallbackPreview` and status enum.
- Added `PlayerSummonSkillExecutionService.PreviewMercenaryNpcSkillPostSpawnCallbacks`.
- Modeled:
  - missing world-insertion preview input;
  - not-ready world-insertion previews;
  - `getSpawnedObject` returning null so callback block is skipped;
  - temporary-spawn registration condition `visObj.getSpawn() != null && visObj.getSpawn().isTemporarySpawn()`;
  - instance handler `onSpawn` condition `visObj.isSpawned()`;
  - delayed walker/pool case where a returned object is not yet spawned and `onSpawn` is skipped.
- Kept live `VisibleObject`, `SpawnTemplate`, `TemporarySpawnEngine`, `WorldPosition`, `WorldMapInstance`, instance handler `onSpawn`, controller callbacks, known-list updates, packets, persistence, threading, serialization, and live-client validation unwired.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.spawnengine.SpawnEngine.spawnObject` post-return callback block | `PlayerSummonSkillExecutionService.PreviewMercenaryNpcSkillPostSpawnCallbacks` | Post Spawn Callback Projection | Partial | Regression Tested | Needs Verification | Represents null returned object, temporary registration, and instance `onSpawn` callback conditions. It does not call callbacks or return live objects. |
| `com.aionemu.gameserver.spawnengine.TemporarySpawnEngine.registerSpawned` | `ShouldRegisterTemporarySpawn` metadata | Temporary Spawn Dependency | Partial | Regression Tested as metadata only | Needs Verification | Models the exact non-null spawn plus temporary-spawn condition. Live synchronized set mutation, event unregistering, hourly spawn/despawn behavior, threading, and serialization remain missing. |
| `com.aionemu.gameserver.model.gameobjects.VisibleObject` | `SpawnedObjectHasSpawn`, `SpawnedObjectIsSpawned`, and `HasSpawnedObject` inputs/metadata | Visible Object Dependency | Partial | Regression Tested | Needs Verification | Treats returned object existence, `getSpawn()`, and `isSpawned()` as represented inputs. Live object identity, controller/known-list references, reflection behavior, threading, and serialization remain unverified. |
| `com.aionemu.gameserver.model.templates.spawns.SpawnTemplate` | `SpawnIsTemporary` input | Spawn Template Dependency | Partial | Regression Tested | Needs Verification | Treats temporary status as represented input. Live spawn-template object identity, group temporary-spawn inheritance, XML/static-data loading, reflection behavior, and serialization remain unverified. |
| `com.aionemu.gameserver.world.WorldPosition` | `SpawnedObjectIsSpawned` input and `ShouldInvokeInstanceOnSpawn` metadata | Spawn State Dependency | Partial | Regression Tested | Needs Verification | Models the `isSpawned` gate, including delayed walker skip behavior. Live volatile spawned state, world position mutation, threading, and runtime comparison remain missing. |
| `com.aionemu.gameserver.world.WorldMapInstance` | `RequiresWorldMapInstance` / `RequiresInstanceHandler` metadata | World Instance Dependency | Not Started | Regression Tested as explicit gap | Needs Verification | Records the future world-map-instance dependency only when object is spawned. Live map instance lookup, handler retrieval, script state, packets, persistence, threading, and live-client behavior remain missing. |
| `com.aionemu.gameserver.instance.handlers.InstanceHandler` | `ShouldInvokeInstanceOnSpawn` metadata | Instance Handler Dependency | Not Started | Regression Tested as explicit gap | Needs Verification | Records the future `onSpawn` callback only when object is spawned. Live instance handler execution, script side effects, packets, persistence, threading, and live-client behavior remain missing. |

## Tests Added Or Updated

- `PlayerSummonSkillExecutionServiceTests.PreviewMercenaryNpcSkillPostSpawnCallbacks_ProjectsJavaSpawnObjectPostProcessing`
  - Validates missing world insertion and not-ready world insertion.
  - Validates null spawned-object return.
  - Validates ordinary callback metadata.
  - Validates temporary-spawn registration condition.
  - Validates instance `onSpawn` condition.
  - Validates delayed walker/non-spawned skip behavior.
  - Validates no-spawn-template skip behavior.
- These tests are source-derived from Java. They do not compare against Java runtime execution, live temporary set mutation, live spawned-state behavior, instance handler callbacks, script callbacks, packets, persistence, XML/static-data loading, reflection behavior, threading behavior, serialization behavior, date/time runtime behavior, precision/rounding behavior, or live-client validation.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 7
- Total artifacts ported or partially modeled in this handoff window: 1 represented post-spawn callback preview slice.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 7
- Total blocked/not-started artifacts: live `TemporarySpawnEngine`, synchronized temporary set mutation, hourly spawn/despawn behavior, live `VisibleObject`, live `SpawnTemplate`, volatile spawned state, `WorldPosition`, `WorldMapInstance`, instance handlers/scripts, packets, persistence, Java runtime comparison, XML/static loading, threading/serialization, reflection behavior, scheduler/date-time behavior, and live-client validation.
- Estimated overall migration completion: 66%

## Remaining Risks

- Post-spawn callback behavior is represented metadata only; no temporary object is registered and no instance handler runs.
- Live `TemporarySpawnEngine`, synchronized temporary-spawn collections, hourly temporary spawn/despawn behavior, `VisibleObject` spawn identity, volatile spawned state, `WorldPosition`, `WorldMapInstance`, instance handlers/scripts, packets, persistence, threading, serialization, XML/static-data loading, Java runtime behavior, scheduler/date-time behavior, geometry, precision/rounding, and live-client behavior remain missing or unverified.

## Next Recommended Unit of Work

Continue by starting a static-data loader adapter for represented `NpcSkillSpawn` XML fields or by adding a represented `NpcSkillSpawn` XML adapter test around `npc_id`, `delay`, `min_distance`, `max_distance`, `min_count`, and `max_count` defaults. Keep live XML loading into `DataManager.SKILL_DATA`, Java JAXB reflection behavior, Java RNG runtime comparison, Java geometry, live spawn-engine execution, live AI mutation, controller execution, effects, packets, scheduler/date-time behavior, threading, serialization, and live-client validation explicit until supported.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6KU-Completion.md`
   - this handoff
3. Inspect Java `NpcSkillSpawn`, `NpcSkillTemplate`, XML/JAXB adapters/loaders, and current C# represented `PlayerSummonKnownObjectNpcSkillSpawnMetadata`.
4. Inspect C# post-spawn preview, spawn metadata, callback preview records/methods, and tests.
5. Implement one narrow represented static-data loader adapter or XML default projection test with Java breadcrumbs.
6. Add focused tests that state what is source-derived and what remains unverified.
7. Run focused tests, then full GameServer tests.
8. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
9. Create the next handoff document and commit the unit.
