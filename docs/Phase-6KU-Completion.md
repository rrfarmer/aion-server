# Phase 6KU Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6KT and covers Session 795.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerSummonCastSpellServiceTests|GameServerConnectionCastSpellTests|PlayerSummonSkillExecutionServiceTests"`
  - Result: Passed, 62 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1386 tests.

## Recent Work Completed

### Session 795 - Represented World Insertion Preview

- Re-inspected Java `SpawnEngine.bringIntoWorld`, `World.storeObject`, `World.setPosition`, `World.createPosition`, and `World.spawn`.
- Added `PlayerSummonKnownObjectNpcSkillWorldInsertionPreview` and status enum.
- Added `PlayerSummonSkillExecutionService.PreviewMercenaryNpcSkillBringIntoWorld`.
- Modeled:
  - missing ordinary-NPC creation preview input;
  - not-ready creation previews, including walker-formator already-brought-into-world cases;
  - Java map lookup failure before position creation;
  - Java instance lookup failure before position creation;
  - Java region lookup failure before position creation;
  - Java already-spawned failure before `World.spawn`;
  - represented `World.storeObject`, duplicate-object check, `World.setPosition`, and `World.spawn` sequence;
  - represented controller before/after spawn callbacks, map-region add, and known-list update requirements.
- Kept live `World`, `WorldMap`, `WorldMapInstance`, `MapRegion`, `WorldPosition`, duplicate-object exception behavior, region/zone revalidation, controller callbacks, known-list updates, temporary-spawn registration, instance handler `onSpawn`, packets, persistence, threading, serialization, and live-client validation unwired.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.spawnengine.SpawnEngine.bringIntoWorld(VisibleObject, SpawnTemplate, int)` | `PlayerSummonSkillExecutionService.PreviewMercenaryNpcSkillBringIntoWorld` | World Insertion Projection | Partial | Regression Tested | Needs Verification | Represents the spawn-template overload as store/set-position/spawn metadata. It does not mutate live `World` or create live `VisibleObject` state. |
| `com.aionemu.gameserver.world.World.storeObject` | `WouldStoreObject` / `RequiresDuplicateObjectCheck` metadata | World Repository Dependency | Partial | Regression Tested as explicit metadata | Needs Verification | Records object storage and duplicate-object check requirements. It does not use `ConcurrentHashMap`, player/siege collections, or duplicate exception behavior. |
| `com.aionemu.gameserver.world.World.setPosition` | `WouldSetPosition` plus invalid map/instance/region statuses | Positioning Dependency | Partial | Regression Tested | Needs Verification | Models map, instance, and region failure gates and represented position assignment. Live `WorldPosition`, despawn-before-position, map-region lookup, exceptions, and coordinate/precision behavior remain unverified. |
| `com.aionemu.gameserver.world.World.createPosition` | invalid map/instance/region statuses | Position Factory Dependency | Partial | Regression Tested | Needs Verification | Records Java null/exception gates but does not allocate live `WorldPosition` or compare exception text/type at runtime. |
| `com.aionemu.gameserver.world.World.spawn` | `WouldSpawn`, controller callback, map-region add, and known-list metadata | Spawn Visibility Dependency | Partial | Regression Tested | Needs Verification | Records before/after spawn callbacks, spawned-state check, map-region add, and known-list update requirements. Live `AlreadySpawnedException`, region mutation, controller callbacks, visibility, packets, and threading remain missing. |
| `com.aionemu.gameserver.world.MapRegion` / zone revalidation | `RequiresMapRegionAdd` metadata only | Region / Zone Dependency | Not Started | Manual Only as explicit gap | Needs Verification | Records map-region add but does not update regions or zones. Zone revalidation, known-list deltas, packets, and live-client behavior remain missing. |
| post-`spawnObject` temporary/instance callbacks in `SpawnEngine.spawnObject` | not yet ported; tracked as remaining risk after world insertion | Post Spawn Callback Dependency | Not Started | Manual Only as explicit gap | Needs Verification | `TemporarySpawnEngine.registerSpawned` and instance handler `onSpawn` happen after `getSpawnedObject` returns. This unit keeps them explicit for the next slice. |

## Tests Added Or Updated

- `PlayerSummonSkillExecutionServiceTests.PreviewMercenaryNpcSkillBringIntoWorld_ProjectsJavaWorldInsertionSteps`
  - Validates missing creation.
  - Validates not-ready walker-brought-into-world branch.
  - Validates invalid map, invalid instance, invalid region, already-spawned, and successful would-insert metadata.
  - Validates world id, instance id, X/Y/Z, and heading propagation.
  - Validates duplicate-object check, map/instance/region lookup requirements, controller before/after spawn requirement, map-region add requirement, known-list update requirement, and may-throw statuses.
- These tests are source-derived from Java. They do not compare against Java runtime execution, live world mutation, exception type/message behavior, concurrent collection behavior, controller callbacks, map-region mutation, zone revalidation, known-list behavior, packets, persistence, XML/static-data loading, reflection behavior, threading behavior, serialization behavior, date/time runtime behavior, precision/rounding behavior, or live-client validation.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 7
- Total artifacts ported or partially modeled in this handoff window: 1 represented world-insertion preview slice.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 7
- Total blocked/not-started artifacts: live `World`, `WorldMap`, `WorldMapInstance`, `MapRegion`, `WorldPosition`, duplicate object handling, player/siege collections, controller callbacks, spawned-state mutation, region mutation, zone revalidation, known-list updates, temporary-spawn registration, instance handler `onSpawn`, packets, persistence, Java runtime comparison, XML/static loading, threading/serialization, reflection behavior, geometry/precision, and live-client validation.
- Estimated overall migration completion: 66%

## Remaining Risks

- World insertion is represented metadata only; no live object is stored, positioned, or spawned.
- Live `World`, `WorldMap`, `WorldMapInstance`, `MapRegion`, `WorldPosition`, duplicate object handling, player/siege object collections, controller callbacks, spawned-state mutation, region mutation, zone revalidation, known-list updates, `TemporarySpawnEngine`, instance handler `onSpawn`, packets, persistence, threading, serialization, XML/static-data loading, Java runtime behavior, scheduler callback execution, geometry, precision/rounding, and live-client behavior remain missing or unverified.

## Next Recommended Unit of Work

Continue by starting a static-data loader adapter for represented `NpcSkillSpawn` XML fields or by modeling the post-`SpawnEngine.spawnObject` callback preview for `TemporarySpawnEngine.registerSpawned`, `VisibleObject.isSpawned`, and instance handler `onSpawn` after a successful represented object spawn. Keep live XML loading, Java RNG runtime comparison, Java geometry, live spawn-engine execution, live AI mutation, controller execution, effects, packets, scheduler/date-time behavior, threading, serialization, and live-client validation explicit until supported.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6KT-Completion.md`
   - this handoff
3. Inspect Java `SpawnEngine.spawnObject`, `TemporarySpawnEngine.registerSpawned`, `VisibleObject.isSpawned`, instance handler `onSpawn`, and the C# world-insertion preview.
4. Inspect C# post-spawn preview, dispatch preview, ordinary NPC creation preview, world-insertion preview records/methods, and tests.
5. Implement one narrow represented static-data loader adapter or post-spawn callback preview slice with Java breadcrumbs.
6. Add focused tests that state what is source-derived and what remains unverified.
7. Run focused tests, then full GameServer tests.
8. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
9. Create the next handoff document and commit the unit.
