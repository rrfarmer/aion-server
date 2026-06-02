# Phase 6 Session 2336 Completion - Add Instance Destroy Lifecycle Hook

## Scope

Added a narrow C# lifecycle hook for Java `InstanceHandler.onInstanceDestroy()` and a modeled runtime destroy helper that removes the instance before notifying the handler.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`
- `game-server/src/com/aionemu/gameserver/instance/handlers/InstanceHandler.java`
- `game-server/src/com/aionemu/gameserver/instance/handlers/GeneralInstanceHandler.java`
- `game-server/src/com/aionemu/gameserver/spawnengine/TemporarySpawnEngine.java`
- `game-server/src/com/aionemu/gameserver/spawnengine/WalkerFormator.java`

Java behavior used:

- `InstanceService.destroyInstance(instance)` removes the `WorldMapInstance` from its map before calling `instance.getInstanceHandler().onInstanceDestroy()`.
- `GeneralInstanceHandler.onInstanceDestroy()` is a no-op.
- Java also cancels empty-instance tasks, unregisters temporary spawns, deletes objects/moves players, and clears walker formations; those broader side effects remain pending.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

Updated:

- `dotnetConversion/src/Aion.GameServer/World/IInstanceLifecycleHandler.cs`
- `dotnetConversion/src/Aion.GameServer/World/WorldMapInstanceRuntimeState.cs`
- `dotnetConversion/src/Aion.GameServer/Services/InstanceRuntimeService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldMapRuntimeStateTests.cs`

Implemented:

- Added `IInstanceLifecycleHandler.OnInstanceDestroy(...)` with a default no-op implementation.
- `GeneralInstanceLifecycleHandler.OnInstanceDestroy(...)` now explicitly mirrors Java's no-op general handler.
- `WorldMapInstanceRuntimeState.NotifyInstanceDestroyed()` records one-shot destroy notification and invokes the lifecycle handler.
- `InstanceRuntimeService.DestroyInstance(...)` removes the modeled map instance before calling `NotifyInstanceDestroyed()`.
- Focused tests verify remove-before-notify ordering and duplicate destroy suppression.

Known limitations:

- This does not delete spawned objects, move players, cancel empty-instance checker tasks, unregister temporary spawns, or clear walker formations.
- There is no live scheduler-driven empty-instance checker yet.
- Dynamic Java instance handler implementations remain unported.

## Validation Decision

- Changed surface: world instance lifecycle model and runtime destroy helper.
- Specific behavior/contract: C# modeled destroy should remove the map instance before notifying `onInstanceDestroy()`, and notify only once.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldMapRuntimeStateTests.InstanceRuntimeService_DestroyInstanceRemovesThenNotifiesLikeJavaInstanceService|FullyQualifiedName~WorldMapRuntimeStateTests.WorldMapInstanceRuntimeState_NotifiesInstanceCreateOnceLikeJavaInstanceService" --no-restore
```

Result: passed 2, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

- Hygiene command:

```powershell
git diff --check
```

Result: passed; only Git line-ending warnings were reported.

- Focused Java/Maven command: skipped. No targeted Java unit fixture exists for `InstanceService.destroyInstance(...)`; Java source review was used as source-of-truth evidence.
- Broad-validation trigger: none. The unit adds a non-live modeled runtime helper and does not enable scheduler cleanup or object deletion.
- Broad .NET decision: skipped full project/solution validation because focused tests compiled the affected project and directly covered the changed lifecycle model.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.instance.handlers.InstanceHandler.onInstanceDestroy` | `Aion.GameServer.World.IInstanceLifecycleHandler.OnInstanceDestroy` | Interface | Partial | Unit Tested | Partial Parity | C# has the destroy callback surface and one-shot notification. Other Java handler methods and dynamic handler discovery remain missing. |
| `com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler.onInstanceDestroy` | `Aion.GameServer.World.GeneralInstanceLifecycleHandler.OnInstanceDestroy` | Handler | Complete | Unit Tested | Verified Parity | Java method is a no-op; C# no-op behavior is deterministic and covered by lifecycle tests. |
| `com.aionemu.gameserver.services.instance.InstanceService.destroyInstance` remove-before-handler ordering | `InstanceRuntimeService.DestroyInstance` | Service Boundary | Partial | Unit Tested | Partial Parity | C# removes the modeled map instance before notifying destroy. Temporary spawn cleanup, object deletion/player exit, empty task cancellation, and walker cleanup remain pending. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `InstanceRuntimeService_DestroyInstanceRemovesThenNotifiesLikeJavaInstanceService` | Unit | Java source review | Destroy removes the instance before handler notification, fires once, and ignores duplicate destroy. | Focused C# unit test plus Java source review. | Does not cover object deletion, temporary spawns, empty checker, or walker cleanup. |
| `WorldMapInstanceRuntimeState_NotifiesInstanceCreateOnceLikeJavaInstanceService` | Unit | Java source review | Adjacent creation lifecycle remains one-shot after destroy hook additions. | Focused adjacent regression. | Does not cover dynamic handlers. |

## Remaining Gaps

- Temporary spawn unregister/despawn on instance destroy.
- Player move-to-exit/object deletion during destroy.
- Empty-instance checker scheduling and cancellation.
- Walker formation cleanup.
- Dynamic Java instance handler loading.
- Event-specific instance spawns for Java `SpawnEngine.spawnEventSpawns(...)`.

## Commit

Commit message:

```text
[Phase 6][UOW-2336] Add instance destroy lifecycle hook
```

## Next Recommended UOW

Continue with the smallest safe destroy side-effect slice:

- temporary spawn unregister/despawn on modeled instance destroy if C# temporary spawn tracking is ready; or
- walker formation cleanup if C# walker formation state is ready; or
- event-specific instance spawns if destroy side effects are not yet ready.
