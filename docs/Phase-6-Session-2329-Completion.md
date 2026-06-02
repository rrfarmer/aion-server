# Phase 6 Session 2329 Completion - Spawn Instance Difficulty Bridge

## Scope

Added a narrow C# spawn-service entrypoint for Java `SpawnEngine.spawnInstance(instance, difficultyId, ownerId)` difficulty filtering and instance placement.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`
- `game-server/src/com/aionemu/gameserver/spawnengine/SpawnEngine.java`

Java behavior used:

- `InstanceService.getNextAvailableInstance(...)` creates the `WorldMapInstance`, then calls `SpawnEngine.spawnInstance(instance, difficultyId, ownerId)`.
- `SpawnEngine.spawnInstance(...)` loads spawns for `instance.getMapId()`.
- Spawn groups with nonzero `difficultId` are skipped unless they match the requested difficulty id.
- Spawned objects are placed into `instance.getInstanceId()`.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

Updated:

- `dotnetConversion/src/Aion.GameServer/Services/WorldNpcSpawnService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldNpcSpawnServiceTests.cs`

Implemented:

- Added `WorldNpcSpawnService.SpawnWorldNpcsForInstance(...)`.
- Reused the existing Java-shaped difficulty filter for instance spawns.
- Added an `instanceId` parameter to the internal spawn loop so instance spawns place NPCs into the allocated runtime instance id.
- Preserved open-world startup/map spawns at instance id `1`.

Known limitations:

- Fresh portal allocation does not yet invoke `SpawnWorldNpcsForInstance(...)`; this UOW adds the production bridge but not the allocation-to-spawn call.
- Static doors, housing spawn side effects, event-specific spawns, walker organization, and full Java `SpawnEngine` side effects remain partial.
- Real-client/encrypted socket bytes remain unverified.

## Validation Decision

- Changed surface: production NPC spawn service and focused spawn tests.
- Specific behavior/contract: C# instance spawning filters nonzero spawn difficulty ids like Java and stamps spawned NPC positions with the target runtime instance id.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldNpcSpawnServiceTests.SpawnWorldNpcs_FiltersByDifficultId|FullyQualifiedName~WorldNpcSpawnServiceTests.SpawnWorldNpcsForInstance_FiltersByDifficultyAndUsesInstanceIdLikeJavaSpawnInstance" --no-restore
```

Result: passed 2, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

- Hygiene command:

```powershell
git diff --check
```

Result: passed; only Git line-ending warnings were reported.

- Focused Java/Maven command: skipped. No targeted Java fixture exists for this runtime spawn branch; Java source review was used as source-of-truth evidence.
- Broad-validation trigger: none. The change is isolated to one spawn service and focused tests supplied the compile signal for the affected project/dependencies.
- Broad .NET decision: skipped full project/solution validation.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.spawnengine.SpawnEngine.spawnInstance` difficulty filter | `Aion.GameServer.Services.WorldNpcSpawnService.SpawnWorldNpcsForInstance` | Service | Partial | Unit Tested | Partial Parity | C# now filters instance spawns by nonzero difficulty id and uses the target instance id. Java static door, housing, event, walker, and object-type side effects remain partial. |
| `com.aionemu.gameserver.services.instance.InstanceService.getNextAvailableInstance` spawn call | C# portal/world allocation paths plus `WorldNpcSpawnService.SpawnWorldNpcsForInstance` | Service Boundary | Partial | No Direct Allocation Test | Needs Verification | The C# spawn bridge exists, but allocation paths do not yet invoke it. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `SpawnWorldNpcsForInstance_FiltersByDifficultyAndUsesInstanceIdLikeJavaSpawnInstance` | Unit | Java source review of `SpawnEngine.spawnInstance` | Instance spawning skips mismatched nonzero difficulty spawns, includes matching/default spawns, and places NPCs into the target instance id. | Focused C# unit test plus Java source review. | Does not cover static doors, housing, walkers, event spawns, or portal allocation invoking the bridge. |
| `SpawnWorldNpcs_FiltersByDifficultId` | Unit | Java source review of `SpawnEngine.spawnInstance` | Existing difficulty filter remains intact for normal spawn-service calls. | Focused C# unit test plus Java source review. | Does not cover instance id placement. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 2
- Total artifacts ported/extended in this UOW: 1
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 2
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: unchanged, conservatively partial.

## Remaining Gaps

- Fresh instance allocation must call `WorldNpcSpawnService.SpawnWorldNpcsForInstance(...)`.
- Java static door spawning, housing spawning, event spawns, walker organization, and full object materialization remain partial.
- Alliance/league live fanout remains unsupported.
- Real-client/encrypted socket bytes remain unverified.

## Commit

Commit message:

```text
[Phase 6][UOW-2329] Add spawn instance difficulty bridge
```

## Next Recommended UOW

Wire fresh instance allocation to call `WorldNpcSpawnService.SpawnWorldNpcsForInstance(...)` when static data and the spawn service are available, preserving focused validation and documenting any live-runtime broad trigger before broader tests.
