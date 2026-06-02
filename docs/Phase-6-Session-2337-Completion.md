# Phase 6 Session 2337 Completion - Unregister Temporary Spawns On Instance Destroy

## Scope

Ported the narrow Java `TemporarySpawnEngine.onInstanceDestroy(WorldMapInstance)` behavior that current C# runtime state can support.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/spawnengine/TemporarySpawnEngine.java`
- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`
- `game-server/src/com/aionemu/gameserver/spawnengine/SpawnEngine.java`

Java behavior used:

- `TemporarySpawnEngine.onInstanceDestroy(instance)` removes temporary spawned objects for that exact `WorldMapInstance` from temporary-spawn tracking.
- It also removes the destroyed instance id from temporary spawn-group registration for that map.
- Java calls this after `map.removeWorldMapInstance(instanceId)` and before visible-object deletion plus `instanceHandler.onInstanceDestroy()`.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

Updated:

- `dotnetConversion/src/Aion.GameServer/Services/WorldNpcSpawnService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/InstanceRuntimeService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldNpcSpawnServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldMapRuntimeStateTests.cs`

Implemented:

- Temporary spawned-object tracking now keys by `(spawn, instanceId)` instead of only by spawn template, so the same temporary spawn can exist in multiple runtime instances.
- `WorldNpcSpawnService.UnregisterTemporarySpawnsForInstance(mapId, instanceId)` mirrors Java temporary-spawn unregister behavior for a destroyed instance.
- `InstanceRuntimeService.DestroyInstance(...)` accepts an optional temporary-spawn cleanup callback and invokes it after map removal and before the destroy handler.
- Hour-change temporary despawn now handles tracked temporary static objects as well as NPCs.

Known limitations:

- C# still does not delete all visible objects in a destroyed instance or move players to exit points.
- The optional cleanup callback is modeled and tested, but live game-server destroy wiring remains incomplete.
- Java `TemporarySpawnEngine` group registration has no separate C# public table yet; the per-instance tracking key covers the currently materialized spawned-object behavior.

## Validation Decision

- Changed surface: production world NPC spawn service and modeled instance destroy service.
- Specific behavior/contract: temporary spawned-object tracking is per runtime instance, and modeled destroy invokes temporary cleanup after instance removal but before `onInstanceDestroy`.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldNpcSpawnServiceTests.UnregisterTemporarySpawnsForInstance_RemovesOnlyDestroyedInstanceTrackingLikeJavaTemporarySpawnEngine|FullyQualifiedName~WorldMapRuntimeStateTests.InstanceRuntimeService_DestroyInstanceRemovesThenNotifiesLikeJavaInstanceService" --no-restore
```

Result: passed 2, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

- Focused Java/Maven command: skipped. No targeted Java unit fixture exists for `TemporarySpawnEngine.onInstanceDestroy(...)`; Java source review was used as source-of-truth evidence.
- Broad-validation trigger: none. This is a focused production service/model change with direct tests and no live scheduler or broad object-deletion enablement.
- Broad .NET decision: skipped full project/solution validation; the filtered test built the affected project and covered the edited behavior.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.spawnengine.TemporarySpawnEngine.onInstanceDestroy` | `Aion.GameServer.Services.WorldNpcSpawnService.UnregisterTemporarySpawnsForInstance` | Service | Partial | Unit Tested | Partial Parity | C# unregisters temporary spawned objects for the destroyed map instance and preserves other instances using the same spawn. Separate Java spawn-group registration table is not independently modeled. |
| `com.aionemu.gameserver.spawnengine.TemporarySpawnEngine.registerSpawned` | `WorldNpcSpawnService` temporary spawned-object tracking | Service State | Partial | Unit Tested | Partial Parity | C# now tracks temporary spawned objects by spawn and instance id. Java stores actual visible objects in a set and separately tracks spawn-group instance ids. |
| `com.aionemu.gameserver.services.instance.InstanceService.destroyInstance` temporary cleanup ordering | `Aion.GameServer.Services.InstanceRuntimeService.DestroyInstance` | Service Boundary | Partial | Unit Tested | Partial Parity | Modeled destroy removes the instance, calls optional temporary cleanup, then notifies destroy handler. Visible-object deletion, player exit, empty-task cancellation, and walker cleanup remain pending. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `UnregisterTemporarySpawnsForInstance_RemovesOnlyDestroyedInstanceTrackingLikeJavaTemporarySpawnEngine` | Unit | Java source review | Same temporary spawn can exist in two instances; unregistering one instance removes only that tracking entry. | Focused C# test plus Java source review. | Does not prove live visible-object deletion during destroy. |
| `InstanceRuntimeService_DestroyInstanceRemovesThenNotifiesLikeJavaInstanceService` | Unit | Java source review | Temporary cleanup callback runs after map removal and before destroy handler notification. | Focused C# test plus Java source review. | Cleanup is optional until live destroy wiring is completed. |

## Remaining Gaps

- Player move-to-exit and non-player visible-object deletion during instance destroy.
- Empty-instance checker scheduling/cancellation.
- Walker formation cleanup on instance destroy.
- Dynamic Java instance handler discovery.
- Event-specific instance spawns for custom handler-supplied instances.

## Commit

Commit message:

```text
[Phase 6][UOW-2337] Unregister instance temporary spawns
```

