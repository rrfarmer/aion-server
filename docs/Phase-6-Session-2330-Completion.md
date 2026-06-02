# Phase 6 Session 2330 Completion - Spawn Fresh Portal Instances

## Scope

Wired fresh portal instance allocation to invoke the C# spawn-instance bridge added in UOW-2329.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`
- `game-server/src/com/aionemu/gameserver/spawnengine/SpawnEngine.java`

Java behavior used:

- `InstanceService.getNextAvailableInstance(...)` creates a `WorldMapInstance`.
- When no custom instance handler supplier is used, Java immediately calls `SpawnEngine.spawnInstance(instance, difficultyId, ownerId)`.
- `SpawnEngine.spawnInstance(...)` loads spawns by `instance.getMapId()`, skips spawn groups whose nonzero `difficultId` does not match, and spawns matching/default objects into `instance.getInstanceId()`.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated; it remains a historical archive.

## Changes

Updated:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientSocketServer.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInstanceCooldownTests.cs`

Implemented:

- Added optional `WorldNpcSpawnService` dependency to game client connection construction.
- Fresh group/alliance/league/player-object portal allocation now calls `SpawnWorldNpcsForInstance(...)` when static data and the spawn service are available.
- Kept the new optional constructor parameter at the end of long optional parameter lists to avoid shifting positional callers.
- Added a focused allocation test that loads tiny runtime-shaped `StaticData`, allocates a fresh portal instance at difficulty `2`, and proves only difficulty `2` plus default spawns materialize in allocated instance id `2`.

Known limitations:

- This UOW covers the normal NPC spawn bridge only.
- Java static door spawning, housing spawning, event spawns, walker organization, temporary spawn scheduling, object-type side effects, and `InstanceHandler.onInstanceCreate()` parity remain partial or unimplemented.
- Alliance/league live fanout remains unsupported.
- Real-client/encrypted socket bytes remain unverified.

## Validation Decision

- Changed surface: production connection allocation path with live NPC spawn side effect plus focused allocation/spawn tests.
- Specific behavior/contract: C# fresh portal allocation invokes Java-shaped instance spawning so nonmatching nonzero difficulty spawns are skipped and matching/default NPCs are placed in the newly allocated instance id.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionInstanceCooldownTests.QueuePortalContinueTransferAsync_FreshGroupAllocationSpawnsDifficultyFilteredNpcsLikeJavaInstanceService|FullyQualifiedName~GameServerConnectionInstanceCooldownTests.QueuePortalContinueTransferAsync_GroupPlanWithoutRegisteredInstanceAllocatesRegistersTeamAndTransfers|FullyQualifiedName~WorldNpcSpawnServiceTests.SpawnWorldNpcsForInstance_FiltersByDifficultyAndUsesInstanceIdLikeJavaSpawnInstance" --no-restore
```

Result: passed 3, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

First focused run failed at compile because the new test was missing the `Aion.GameServer.Utils.IdFactory` import; the import was added and the same focused command passed.

- Hygiene command:

```powershell
git diff --check
```

Result: passed; only Git line-ending warnings were reported.

- Focused Java/Maven command: skipped. No targeted Java fixture exists for this runtime branch; Java source review of `InstanceService.getNextAvailableInstance(...)` and `SpawnEngine.spawnInstance(...)` was used as source-of-truth evidence.
- Broad-validation trigger: live side-effect enabling in the portal allocation path.
- Broad .NET decision: skipped full project/solution validation because the changed side effect was isolated behind an optional injected service and the focused connection/spawn tests supplied the compile signal plus direct behavioral evidence. No shared packet primitive, serialization helper, persistence schema, scheduler primitive, or startup lifecycle contract was changed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.instance.InstanceService.getNextAvailableInstance` normal spawn call | `Aion.GameServer.Network.Aion.GameServerConnection.QueuePortalTeamContinueTransferAsync` plus `Aion.GameServer.Services.WorldNpcSpawnService.SpawnWorldNpcsForInstance` | Service Boundary | Partial | Unit Tested | Partial Parity | Fresh C# portal allocation now invokes the spawn bridge when static data and spawn service are available. Java static doors, event spawns, housing, walkers, temporary spawn scheduling, full object materialization, `onInstanceCreate`, and empty-instance task side effects remain partial or missing. |
| `com.aionemu.gameserver.spawnengine.SpawnEngine.spawnInstance` difficulty filter and instance placement | `Aion.GameServer.Services.WorldNpcSpawnService.SpawnWorldNpcsForInstance` | Service | Partial | Unit Tested | Partial Parity | Focused tests prove matching/default NPC spawn groups are placed in the allocated instance id and nonmatching nonzero difficulty groups are skipped. Static doors, housing, event, walker, temporary, and non-NPC object side effects remain gaps. |
| `com.aionemu.gameserver.network.aion.GameConnectionListener` connection construction dependency flow | `Aion.GameServer.Network.Aion.GameClientSocketServer` to `GameServerConnection` | Service Boundary | Partial | Unit Tested Through Connection Helper | Needs Verification | Optional spawn service is forwarded to per-client connections. Hosted socket startup was not broadly tested; constructor shape was kept source-compatible by appending the optional parameter. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `QueuePortalContinueTransferAsync_FreshGroupAllocationSpawnsDifficultyFilteredNpcsLikeJavaInstanceService` | Unit | Java source review of `InstanceService.getNextAvailableInstance` and `SpawnEngine.spawnInstance` | Fresh portal allocation calls the spawn bridge, filters by difficulty id `2`, includes default difficulty spawns, and places spawned NPCs into allocated instance id `2`. | Focused C# allocation test plus Java source review. | Does not cover static doors, housing, event spawns, walkers, temporary schedules, `onInstanceCreate`, alliance/league fanout, or real socket bytes. |
| `QueuePortalContinueTransferAsync_GroupPlanWithoutRegisteredInstanceAllocatesRegistersTeamAndTransfers` | Unit | Java source review of portal fresh group allocation path | Existing fresh group allocation, team registration, transfer, cooldown, and difficulty metadata behavior remained intact. | Focused adjacent regression test. | Does not inspect spawned NPCs. |
| `SpawnWorldNpcsForInstance_FiltersByDifficultyAndUsesInstanceIdLikeJavaSpawnInstance` | Unit | Java source review of `SpawnEngine.spawnInstance` | Spawn bridge filters by difficulty and uses target runtime instance id. | Focused spawn-service unit test. | Does not cover portal allocation invoking the bridge. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 3
- Total artifacts ported/extended in this UOW: 3
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 3
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: unchanged, conservatively partial.

## Remaining Gaps

- Java `SpawnEngine.spawnInstance(...)` side effects beyond basic NPC spawning remain partial.
- Java `InstanceHandler.onInstanceCreate()` and empty-instance checker behavior are not ported in this allocation path.
- Event-specific instance spawning is not wired.
- Alliance/league live fanout remains unsupported.
- Real-client/encrypted socket behavior remains unverified.

## Commit

Commit message:

```text
[Phase 6][UOW-2330] Spawn fresh portal instances
```

## Next Recommended UOW

Continue from Java `SpawnEngine.spawnInstance(...)` side effects. The smallest production candidates are to model one missing side effect at a time, preferably static door instance spawning or event-specific instance spawns if matching C# data/services already exist.
