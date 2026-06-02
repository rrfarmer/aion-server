# Phase 6 Session 2338 Handoff - Delete Non-Player Instance Objects

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2338-Completion.md`
- `docs/Phase-6-Session-2338-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger.

## Current State

Last completed UOW: UOW-2338, modeled instance destroy can now unregister temporary spawns and delete modeled non-player visible objects before notifying `onInstanceDestroy`.

Relevant completed portal/instance/spawn slices:

- Fresh solo/group allocation paths spawn instance objects and notify `onInstanceCreate` after spawning.
- Portal difficulty is preserved on top-level allowed plans and team plans.
- Instance spawning filters by nonzero difficulty id and uses the allocated runtime instance id.
- Static doors and static handler objects are materialized for instance spawning.
- Temporary spawn tracking is per `(spawn, instanceId)`.
- Modeled destroy order includes map removal, optional temporary cleanup, optional non-player object cleanup, and destroy handler notification.

Still not proven or not implemented:

- Live game-server destroy path wiring with NPC spawn service cleanup callbacks.
- Player forced-exit packet and move-to-exit behavior during destroy.
- Empty-instance checker scheduling and cancellation.
- Walker formation cleanup.
- Dynamic Java AI/instance handler discovery and per-map/per-AI handler class selection.
- Event-specific instance spawns and custom instance handler suppliers.

## Commits Made

- `[Phase 6][UOW-2338] Delete non-player instance objects`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Services/WorldNpcSpawnService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/InstanceRuntimeService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldNpcSpawnServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldMapRuntimeStateTests.cs`
- `docs/Phase-6-Session-2338-Completion.md`
- `docs/Phase-6-Session-2338-Handoff.md`

Unrelated dirty file to preserve:

- `docs/parity-verification.md` was already dirty and should remain unstaged unless the user explicitly asks to edit it.

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldNpcSpawnServiceTests.DeleteNonPlayerObjectsForInstance_RemovesNpcAndStaticObjectsButKeepsPlayersLikeJavaInstanceDestroy|FullyQualifiedName~WorldMapRuntimeStateTests.InstanceRuntimeService_DestroyInstanceRemovesThenNotifiesLikeJavaInstanceService" --no-restore
```

Result: passed 2, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Focused Java/Maven validation: skipped because no targeted Java fixture exists for `InstanceService.destroyInstance(...)`. Java source review was used as source-of-truth evidence.

Broad-validation trigger: none.

Broad .NET decision: skipped full project/solution validation because focused tests compiled the affected project and directly covered the changed service behavior.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.instance.InstanceService.destroyInstance` non-player deletion branch | `Aion.GameServer.Services.WorldNpcSpawnService.DeleteNonPlayerObjectsForInstance` | Service | Partial | Unit Tested | Partial Parity | Deletes modeled NPC/static objects for a destroyed instance and preserves players for the pending forced-exit slice. Other Java visible-object subclasses are not modeled yet. |
| `com.aionemu.gameserver.controllers.VisibleObjectController.delete` / `NpcController` cleanup path | `WorldNpcSpawnService.TryDespawnWorldNpc` and `TryDespawnStaticObject` | Service Helper | Partial | Unit Tested | Partial Parity | NPC cleanup uses existing despawn side effects; static object cleanup removes world object, despawns placeable state, and releases object id. Full Java controller lifecycle remains broader. |
| `com.aionemu.gameserver.services.instance.InstanceService.destroyInstance` ordering | `Aion.GameServer.Services.InstanceRuntimeService.DestroyInstance` | Service Boundary | Partial | Unit Tested | Partial Parity | Modeled destroy order now includes map removal, temporary cleanup, non-player cleanup, and destroy handler notification. Player exit, empty-task cancellation, and walker cleanup remain pending. |

## Next Sequential UOW

Recommended next production scope: inspect Java player forced-exit behavior in `InstanceService.destroyInstance(...)` and port the smallest C# player-exit plan or dispatch slice if the teleport/send boundary is ready.

Java artifacts:

- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`
- `game-server/src/com/aionemu/gameserver/services/teleport/TeleportService.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`
- `game-server/src/com/aionemu/gameserver/utils/PacketSendUtility.java`

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Services/InstanceRuntimeService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleport*`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldMapRuntimeStateTests.cs`
- relevant teleport/send adapter tests discovered during Work Discovery

Focused C# validation recipe:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldMapRuntimeStateTests.InstanceRuntimeService_DestroyInstanceRemovesThenNotifiesLikeJavaInstanceService|FullyQualifiedName~BindPointTeleport" --no-restore
```

Narrow after Work Discovery to exact player-exit/teleport tests. Do not run a full project/solution test unless a broad trigger is named first.

Focused Java/Maven command: not expected unless a targeted Java fixture is added; Java source review is likely the practical evidence.

Broad-validation trigger: none unless live teleport dispatch or packet fanout is enabled.

## Safe Candidates

- Player forced-exit plan for modeled instance destroy, leaving live dispatch disabled if needed.
- Live destroy wiring if all cleanup callbacks can be composed without broad side effects.
- Walker formation cleanup on instance destroy.
- Event-specific instance spawn branch for Java `SpawnEngine.spawnEventSpawns(...)`.

Avoid:

- Full `.NET` project tests or solution builds without a documented broad-validation trigger.
- Evidence/reporting-only units.
- Updating `docs/PHASE-6-PROGRESS.md`.

