# Phase 6 Session 2332 Completion - Materialize Static Handler Objects

## Scope

Added a narrow C# runtime branch for Java `StaticObjectSpawnManager.spawnTemplate(spawn, instanceId)` when spawn groups use `handler="STATIC"`.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/spawnengine/SpawnEngine.java`
- `game-server/src/com/aionemu/gameserver/spawnengine/StaticObjectSpawnManager.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/StaticObject.java`
- `game-server/src/com/aionemu/gameserver/model/templates/VisibleObjectTemplate.java`

Java behavior used:

- `SpawnEngine.spawnInstance(...)` dispatches spawn groups with handler type `STATIC` to `StaticObjectSpawnManager.spawnTemplate(spawn, instanceId)`.
- Java resolves the object template through `DataManager.ITEM_DATA.getItemTemplate(spawn.getNpcId())`; missing templates do not materialize objects.
- Java creates a `StaticObject`, stores it in `World`, and positions it at the spawn template coordinates in the target instance.
- The Java `spawnedCounter` is not incremented for this handler branch.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

Updated:

- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/WorldStaticObject.cs`
- `dotnetConversion/src/Aion.GameServer/World/World.cs`
- `dotnetConversion/src/Aion.GameServer/Services/WorldNpcSpawnService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldNpcSpawnServiceTests.cs`

Implemented:

- Added `WorldStaticObject` for Java `StaticObject` runtime projection.
- Added `World.GetStaticObjects(...)` helpers for focused world-state inspection.
- `WorldNpcSpawnService` now accepts `ItemTemplateTable` for instance/open-world startup static handler materialization.
- Fresh portal allocation passes `staticData.ItemTemplates` into the instance spawn bridge.
- Spawn groups with `handler="STATIC"` now materialize static objects from item templates into the target instance and preserve static id/placeable state.
- Static handler objects do not increment `WorldNpcSpawnResult.SpawnedCount`, matching Java's NPC spawn counter behavior.

Known limitations:

- Static object known-list visibility and packet fanout are not implemented.
- Static object interaction/controller behavior is not implemented.
- Pooled static handler spawn selection uses the existing C# random pool selection but is not separately parity-tested.
- Existing public NPC-only spawn entrypoints still skip `handler="STATIC"` unless item templates are supplied by runtime startup/instance paths.

## Validation Decision

- Changed surface: production instance-spawn branch plus new world object model.
- Specific behavior/contract: C# `handler="STATIC"` groups resolve item templates, materialize static objects in the target instance, preserve static id/placeable state, and do not count as ordinary NPC spawns.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldNpcSpawnServiceTests.SpawnWorldNpcsForInstance_MaterializesStaticHandlerObjectsLikeJavaStaticObjectSpawnManager|FullyQualifiedName~WorldNpcSpawnServiceTests.SpawnWorldNpcsForInstance_SetsStaticDoorStateLikeJavaStaticDoorSpawnManager|FullyQualifiedName~WorldNpcSpawnServiceTests.SpawnWorldNpcsForInstance_FiltersByDifficultyAndUsesInstanceIdLikeJavaSpawnInstance" --no-restore
```

Result: passed 3, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

- Hygiene command:

```powershell
git diff --check
```

Result: passed; only Git line-ending warnings were reported.

- Focused Java/Maven command: skipped. No targeted Java unit fixture exists for `StaticObjectSpawnManager.spawnTemplate(...)`; Java source review was used as source-of-truth evidence.
- Broad-validation trigger: new live world object type and instance-spawn side effect.
- Broad .NET decision: skipped full project/solution validation because the focused test compiled the affected project and directly covered static-object world insertion, static-door adjacent behavior, and existing instance NPC spawn behavior. No packet primitive, persistence schema, hosted startup lifecycle, or shared scheduler primitive changed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.spawnengine.StaticObjectSpawnManager.spawnTemplate` | `WorldNpcSpawnService` static handler branch | Service Boundary | Partial | Unit Tested | Partial Parity | C# now resolves item templates, materializes static objects, positions them in the target instance, and preserves static id/placeable state. Pool ordering, known lists, packet fanout, and controller interaction remain unverified or missing. |
| `com.aionemu.gameserver.model.gameobjects.StaticObject` | `Aion.GameServer.Model.GameObjects.WorldStaticObject` | Model | Partial | Unit Tested | Partial Parity | C# stores object id, template id, item template, position, static id, and spawn location. Java controller, object template inheritance, known-list, and visibility behavior remain missing. |
| `com.aionemu.gameserver.world.World` static object storage | `Aion.GameServer.World.World.GetStaticObjects` plus `TryAddObject` | World State | Partial | Unit Tested | Partial Parity | C# can store/query static objects separately from NPC scans. Region/known-list behavior remains missing. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `SpawnWorldNpcsForInstance_MaterializesStaticHandlerObjectsLikeJavaStaticObjectSpawnManager` | Unit | Java source review of `StaticObjectSpawnManager.spawnTemplate` | Static handler spawn resolves an item template, creates a static object in instance id `7`, stores static id/placeable state, and does not increment NPC spawn count. | Focused C# unit test plus Java source review. | Does not cover known lists, packet visibility, controller interactions, or pooled static-object parity. |
| `SpawnWorldNpcsForInstance_SetsStaticDoorStateLikeJavaStaticDoorSpawnManager` | Unit | Java source review | Adjacent static-door state seeding remains intact. | Focused adjacent regression. | Does not materialize door objects. |
| `SpawnWorldNpcsForInstance_FiltersByDifficultyAndUsesInstanceIdLikeJavaSpawnInstance` | Unit | Java source review | Existing NPC difficulty/instance behavior remains intact. | Focused adjacent regression. | Does not cover static objects. |

## Remaining Gaps

- Static object known-list visibility and packet fanout.
- Static object controller/interaction behavior.
- Java event spawns, housing spawns, walker organization, `InstanceHandler.onInstanceCreate()`, and empty-instance checker behavior.
- Real-client/encrypted socket validation for portal instance branches.

## Commit

Commit message:

```text
[Phase 6][UOW-2332] Materialize static handler objects
```

## Next Recommended UOW

Continue with Java `InstanceHandler.onInstanceCreate()` or event-specific instance spawn behavior, whichever has the smallest available C# infrastructure after Work Discovery.
