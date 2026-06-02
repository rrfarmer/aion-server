# Phase 6 Session 2333 Completion - Spawn Solo Fresh Portal Instances

## Scope

Ported the solo fresh portal allocation spawn step from Java `PortalService.port(...)` / `InstanceService.getNextAvailableInstance(...)`.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/services/teleport/PortalService.java`
- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`
- `game-server/src/com/aionemu/gameserver/spawnengine/SpawnEngine.java`

Java behavior used:

- For max-player `0`/`1` portal branches, Java calls `port(player, loc, reenter, maxPlayers)` when no reusable registered instance is available.
- `InstanceService.getNextAvailableInstance(...)` allocates a new instance, calls `SpawnEngine.spawnInstance(instance, difficultyId, ownerId)`, then the transfer path teleports and applies cooldown.
- Instance spawns use the allocated runtime instance id and the selected difficulty id.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

Updated:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInstanceCooldownTests.cs`

Implemented:

- `QueueAllocatedInstancePortalTransferAsync(...)` now accepts a difficulty id and passes it into `InstanceRuntimeService.CreatePortalTransferInstance(...)`.
- When static data and `WorldNpcSpawnService` are available, solo fresh allocation now calls `SpawnWorldNpcsForInstance(...)` before queuing the transfer/cooldown.
- The focused solo allocation test now proves allocated-instance NPC spawns land in instance id `2`, filter by difficulty id `2`, and preserve the existing registration, teleport, and cooldown behavior.

Known limitations:

- Higher-level solo portal entry preparation does not yet carry Beshmundir-style difficulty selection; current difficulty propagation is still team-plan oriented.
- Java `InstanceHandler.onInstanceCreate()` is still not modeled; this unit only restores the spawn-before-transfer step.
- Real-client/encrypted socket validation for fresh solo portal branches remains pending.

## Validation Decision

- Changed surface: production connection portal allocation branch plus focused test.
- Specific behavior/contract: fresh solo portal allocation should spawn Java-shaped instance NPCs before transfer, using the allocated runtime instance id and selected difficulty id, while still registering the player and applying cooldown.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionInstanceCooldownTests.QueueAllocatedInstancePortalTransferAsync_AllocatesRegistersSpawnsAndTransfersLikeJavaInstanceService|FullyQualifiedName~WorldNpcSpawnServiceTests.SpawnWorldNpcsForInstance_FiltersByDifficultyAndUsesInstanceIdLikeJavaSpawnInstance" --no-restore
```

Result: passed 2, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

- Hygiene command:

```powershell
git diff --check
```

Result: passed; only Git line-ending warnings were reported.

- Focused Java/Maven command: skipped. No targeted Java unit fixture exists for `PortalService.port(...)` plus `InstanceService.getNextAvailableInstance(...)`; Java source review was used as source-of-truth evidence.
- Broad-validation trigger: live portal allocation side effect and world-state spawn side effect.
- Broad .NET decision: skipped full project/solution validation because the focused command compiled the affected project and directly exercised the edited connection branch plus the adjacent spawn filtering contract. No packet primitive, persistence schema, scheduler primitive, or hosted startup lifecycle changed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.teleport.PortalService.port` solo fresh allocation branch | `GameServerConnection.QueueAllocatedInstancePortalTransferAsync` | Service Boundary | Partial | Unit Tested | Partial Parity | C# now spawns instance NPCs before solo fresh transfer when static data/spawn service are available. Higher-level solo difficulty propagation and real-client branch coverage remain pending. |
| `com.aionemu.gameserver.services.instance.InstanceService.getNextAvailableInstance` spawn-before-transfer behavior | `InstanceRuntimeService.CreatePortalTransferInstance` plus `GameServerConnection.QueueAllocatedInstancePortalTransferAsync` | Service Boundary | Partial | Unit Tested | Partial Parity | Allocation carries difficulty id and the connection invokes instance spawning before transfer. Java `onInstanceCreate()` and empty-instance checker are still missing. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `QueueAllocatedInstancePortalTransferAsync_AllocatesRegistersSpawnsAndTransfersLikeJavaInstanceService` | Unit | Java source review of `PortalService.port` and `InstanceService.getNextAvailableInstance` | Fresh solo allocation registers player, carries difficulty id `2`, spawns matching/default NPCs in allocated instance id `2`, teleports, and applies cooldown. | Focused C# unit test plus Java source review. | Does not cover real-client encrypted bytes, higher-level Beshmundir solo difficulty propagation, or `onInstanceCreate()`. |
| `SpawnWorldNpcsForInstance_FiltersByDifficultyAndUsesInstanceIdLikeJavaSpawnInstance` | Unit | Java source review of `SpawnEngine.spawnInstance` | Adjacent spawn service still filters by difficulty and uses runtime instance id. | Focused adjacent regression. | Does not cover portal transfer. |

## Remaining Gaps

- Java `InstanceHandler.onInstanceCreate()` and `onInstanceDestroy()` lifecycle callbacks.
- Higher-level solo portal difficulty propagation for Beshmundir-style branches.
- Event-specific instance spawns for Java `SpawnEngine.spawnEventSpawns(...)`.
- Static object visibility/known-list and controller interactions.
- Real-client/encrypted socket validation for solo/group portal branches.

## Commit

Commit message:

```text
[Phase 6][UOW-2333] Spawn solo fresh portal instances
```

## Next Recommended UOW

Continue with the smallest production slice among:

- Add a deferred `InstanceHandler.onInstanceCreate()` lifecycle hook that fires after C# instance spawning, matching Java ordering.
- Carry Beshmundir-style difficulty selection through higher-level solo portal entry plans.
- Inspect and port event-specific instance spawns if the C# event data surface is ready.
