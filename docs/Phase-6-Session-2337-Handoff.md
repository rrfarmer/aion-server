# Phase 6 Session 2337 Handoff - Unregister Temporary Spawns On Instance Destroy

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2337-Completion.md`
- `docs/Phase-6-Session-2337-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger.

## Current State

Last completed UOW: UOW-2337, C# temporary spawned-object tracking now includes runtime instance id and modeled instance destroy has the Java temporary-cleanup ordering slot.

Relevant completed portal/instance/spawn slices:

- Fresh solo/group allocation paths spawn instance objects and notify `onInstanceCreate` after spawning.
- Portal difficulty is preserved on top-level allowed plans and team plans.
- Instance spawning filters by nonzero difficulty id and uses the allocated runtime instance id.
- Static doors and static handler objects are materialized for instance spawning.
- Modeled destroy removes the map instance, invokes optional temporary cleanup, then notifies `onInstanceDestroy`.
- Temporary spawn tracking is per `(spawn, instanceId)`.

Still not proven or not implemented:

- Live game-server destroy path wiring with the NPC spawn service cleanup callback.
- Player move-to-exit and visible-object deletion during destroy.
- Empty-instance checker scheduling and cancellation.
- Walker formation cleanup.
- Dynamic Java AI/instance handler discovery and per-map/per-AI handler class selection.
- Event-specific instance spawns and custom instance handler suppliers.

## Commits Made

- `[Phase 6][UOW-2337] Unregister instance temporary spawns`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Services/WorldNpcSpawnService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/InstanceRuntimeService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldNpcSpawnServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldMapRuntimeStateTests.cs`
- `docs/Phase-6-Session-2337-Completion.md`
- `docs/Phase-6-Session-2337-Handoff.md`

Unrelated dirty file to preserve:

- `docs/parity-verification.md` was already dirty and should remain unstaged unless the user explicitly asks to edit it.

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldNpcSpawnServiceTests.UnregisterTemporarySpawnsForInstance_RemovesOnlyDestroyedInstanceTrackingLikeJavaTemporarySpawnEngine|FullyQualifiedName~WorldMapRuntimeStateTests.InstanceRuntimeService_DestroyInstanceRemovesThenNotifiesLikeJavaInstanceService" --no-restore
```

Result: passed 2, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Focused Java/Maven validation: skipped because no targeted Java fixture exists for `TemporarySpawnEngine.onInstanceDestroy(...)`. Java source review was used as source-of-truth evidence.

Broad-validation trigger: none.

Broad .NET decision: skipped full project/solution validation because focused tests compiled the affected project and directly covered the changed service behavior.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.spawnengine.TemporarySpawnEngine.onInstanceDestroy` | `Aion.GameServer.Services.WorldNpcSpawnService.UnregisterTemporarySpawnsForInstance` | Service | Partial | Unit Tested | Partial Parity | C# unregisters temporary spawned objects for the destroyed map instance and preserves other instances using the same spawn. Separate Java spawn-group registration table is not independently modeled. |
| `com.aionemu.gameserver.spawnengine.TemporarySpawnEngine.registerSpawned` | `WorldNpcSpawnService` temporary spawned-object tracking | Service State | Partial | Unit Tested | Partial Parity | C# now tracks temporary spawned objects by spawn and instance id. Java stores actual visible objects in a set and separately tracks spawn-group instance ids. |
| `com.aionemu.gameserver.services.instance.InstanceService.destroyInstance` temporary cleanup ordering | `Aion.GameServer.Services.InstanceRuntimeService.DestroyInstance` | Service Boundary | Partial | Unit Tested | Partial Parity | Modeled destroy removes the instance, calls optional temporary cleanup, then notifies destroy handler. Visible-object deletion, player exit, empty-task cancellation, and walker cleanup remain pending. |

## Next Sequential UOW

Recommended next production scope: port the smallest safe visible-object deletion slice from Java `InstanceService.destroyInstance(...)`.

Java artifacts:

- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/VisibleObject.java`
- `game-server/src/com/aionemu/gameserver/controllers/VisibleObjectController.java`
- `game-server/src/com/aionemu/gameserver/controllers/NpcController.java`

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Services/InstanceRuntimeService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/WorldNpcSpawnService.cs`
- `dotnetConversion/src/Aion.GameServer/World/World.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldMapRuntimeStateTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldNpcSpawnServiceTests.cs`

Focused C# validation recipe:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldMapRuntimeStateTests.InstanceRuntimeService_DestroyInstanceRemovesThenNotifiesLikeJavaInstanceService|FullyQualifiedName~WorldNpcSpawnServiceTests" --no-restore
```

Narrow after Work Discovery to exact object-deletion tests. Do not run a full project/solution test unless a broad trigger is named first.

Focused Java/Maven command: not expected unless a targeted Java fixture is added; Java source review is likely the practical evidence.

Broad-validation trigger: none unless the unit enables live broad object deletion or shared world-state mutation beyond the modeled service tests.

## Safe Candidates

- Visible non-player object deletion during modeled instance destroy, if current `World` APIs are enough.
- Player exit/move-to-bind planning, if teleport service boundary is ready.
- Walker formation cleanup on instance destroy.
- Event-specific instance spawn branch for Java `SpawnEngine.spawnEventSpawns(...)`.

Avoid:

- Full `.NET` project tests or solution builds without a documented broad-validation trigger.
- Evidence/reporting-only units.
- Updating `docs/PHASE-6-PROGRESS.md`.

