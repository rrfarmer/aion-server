# Phase 6 Session 2338 Completion - Delete Non-Player Instance Objects

## Scope

Ported the smallest safe non-player visible-object deletion slice from Java `InstanceService.destroyInstance(...)`.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/VisibleObject.java`
- `game-server/src/com/aionemu/gameserver/controllers/VisibleObjectController.java`
- `game-server/src/com/aionemu/gameserver/controllers/NpcController.java`

Java behavior used:

- During `destroyInstance`, Java iterates visible objects in the destroyed `WorldMapInstance`.
- Players are sent a leave-instance packet and moved to the exit point.
- Non-player visible objects are deleted through their controller.

This UOW models only the non-player object deletion part for currently represented C# visible objects: `WorldNpc` and `WorldStaticObject`.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

Updated:

- `dotnetConversion/src/Aion.GameServer/Services/WorldNpcSpawnService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/InstanceRuntimeService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldNpcSpawnServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldMapRuntimeStateTests.cs`

Implemented:

- `WorldNpcSpawnService.DeleteNonPlayerObjectsForInstance(mapId, instanceId)` removes modeled NPCs and static objects from the target instance.
- NPC deletion uses existing `TryDespawnWorldNpc(...)` cleanup for AI/life stats, pending decay, static placeables, PVP-zone counters, ID release, and walker-plan refresh.
- Static object deletion now has a shared `TryDespawnStaticObject(...)` helper.
- `InstanceRuntimeService.DestroyInstance(...)` has an optional non-player cleanup callback and records the deleted-object count in `InstanceDestroyRuntimePlan`.

Known limitations:

- Player forced-exit packet and teleport planning remain pending.
- Live game-server destroy wiring remains incomplete.
- Other Java visible-object subclasses are not modeled in this C# slice.

## Validation Decision

- Changed surface: production world NPC/static cleanup service and modeled instance destroy service.
- Specific behavior/contract: delete non-player visible objects in the destroyed instance only, keep players for the future exit/move slice, and run object cleanup before destroy handler notification.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldNpcSpawnServiceTests.DeleteNonPlayerObjectsForInstance_RemovesNpcAndStaticObjectsButKeepsPlayersLikeJavaInstanceDestroy|FullyQualifiedName~WorldMapRuntimeStateTests.InstanceRuntimeService_DestroyInstanceRemovesThenNotifiesLikeJavaInstanceService" --no-restore
```

Result: passed 2, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

- Focused Java/Maven command: skipped. No targeted Java unit fixture exists for `InstanceService.destroyInstance(...)`; Java source review was used as source-of-truth evidence.
- Broad-validation trigger: none. This is a modeled service cleanup slice with direct tests and no live broad destroy dispatch enabled.
- Broad .NET decision: skipped full project/solution validation; the filtered test built the affected project and covered the edited behavior.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.instance.InstanceService.destroyInstance` non-player deletion branch | `Aion.GameServer.Services.WorldNpcSpawnService.DeleteNonPlayerObjectsForInstance` | Service | Partial | Unit Tested | Partial Parity | Deletes modeled NPC/static objects for a destroyed instance and preserves players for the pending forced-exit slice. Other Java visible-object subclasses are not modeled yet. |
| `com.aionemu.gameserver.controllers.VisibleObjectController.delete` / `NpcController` cleanup path | `WorldNpcSpawnService.TryDespawnWorldNpc` and `TryDespawnStaticObject` | Service Helper | Partial | Unit Tested | Partial Parity | NPC cleanup uses existing despawn side effects; static object cleanup removes world object, despawns placeable state, and releases object id. Full Java controller lifecycle remains broader. |
| `com.aionemu.gameserver.services.instance.InstanceService.destroyInstance` ordering | `Aion.GameServer.Services.InstanceRuntimeService.DestroyInstance` | Service Boundary | Partial | Unit Tested | Partial Parity | Modeled destroy order now includes map removal, temporary cleanup, non-player cleanup, and destroy handler notification. Player exit, empty-task cancellation, and walker cleanup remain pending. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `DeleteNonPlayerObjectsForInstance_RemovesNpcAndStaticObjectsButKeepsPlayersLikeJavaInstanceDestroy` | Unit | Java source review | Deletes NPC/static objects only in the target instance and leaves players plus other instances intact. | Focused C# test plus Java source review. | Does not cover player exit packet/teleport. |
| `InstanceRuntimeService_DestroyInstanceRemovesThenNotifiesLikeJavaInstanceService` | Unit | Java source review | Non-player cleanup callback runs before destroy handler and reports deleted-object count. | Focused C# test plus Java source review. | Callback remains optional until live destroy wiring is completed. |

## Remaining Gaps

- Player forced-exit packet and move-to-exit behavior during instance destroy.
- Live destroy wiring with temporary cleanup plus non-player object cleanup callbacks.
- Empty-instance checker scheduling/cancellation.
- Walker formation cleanup on instance destroy.
- Dynamic Java instance handler discovery.

## Commit

Commit message:

```text
[Phase 6][UOW-2338] Delete non-player instance objects
```

