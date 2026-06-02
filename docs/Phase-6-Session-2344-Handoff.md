# Phase 6 Session 2344 Handoff - Cleanup Instance Walker State

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2344-Completion.md`
- `docs/Phase-6-Session-2344-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger.

## Current State

Last completed UOW: UOW-2344, added modeled walker cleanup for destroyed map instances and wired the destroy planner callback order to match Java.

Relevant completed portal/instance/spawn/destroy/teleport slices:

- Fresh solo/group allocation paths spawn instance objects and notify `onInstanceCreate` after spawning.
- Portal difficulty is preserved on top-level allowed plans and team plans.
- Instance spawning filters by nonzero difficulty id and uses the allocated runtime instance id.
- Static doors and static handler objects are materialized for instance spawning.
- Temporary spawn tracking is per `(spawn, instanceId)`.
- Modeled destroy order includes map removal, optional temporary cleanup, optional non-player object cleanup, destroy handler notification, and optional walker cleanup.
- Player forced-exit planning selects players in the destroyed instance and plans `STR_MSG_LEAVE_INSTANCE_FORCE(0)`.
- Instance-exit static data loads from cache rows and resolves destination-vs-bind-fallback plans.
- Bind-location destination resolution uses player bind point first, initial race spawn second, and Java's instance-id rule.
- Destroyed-player forced-exit teleport plans include the modeled final destination when available.
- Walker route-walking runtime state can now be cleaned for a destroyed world/instance.

Still not proven or not implemented:

- Live forced-exit packet send and teleport dispatch.
- Live game-server destroy path wiring with all cleanup callbacks.
- Empty-instance checker scheduling and cancellation.
- Instance-scoped walker spawn plan cache parity.
- Exact Java missing-initial-data failure behavior.

## Commits Made

- `[Phase 6][UOW-2344] Cleanup instance walker state`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Services/InstanceRuntimeService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/WorldNpcWalkerRouteWalkingService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldMapRuntimeStateTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldNpcWalkerRouteWalkingServiceTests.cs`
- `docs/Phase-6-Session-2344-Completion.md`
- `docs/Phase-6-Session-2344-Handoff.md`

Unrelated dirty file to preserve:

- `docs/parity-verification.md` may already be dirty and should remain unstaged unless the user explicitly asks to edit it.

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldMapRuntimeStateTests.InstanceRuntimeService_DestroyInstanceRemovesThenNotifiesLikeJavaInstanceService|FullyQualifiedName~WorldNpcWalkerRouteWalkingServiceTests.OnInstanceDestroy_RemovesOnlyDestroyedInstanceWalkerStateLikeJavaWalkerFormator" --no-restore
```

Result: passed 2, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Repository hygiene:

```powershell
git diff --check
```

Result: passed.

Focused Java/Maven validation: skipped because no targeted Java unit fixture exists for `WalkerFormator.onInstanceDestroy(...)`. Java source review was used as source-of-truth evidence.

Broad-validation trigger: none.

Broad .NET decision: skipped full project/solution validation because focused tests compiled the affected project and directly covered the changed cleanup behavior.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.spawnengine.WalkerFormator.onInstanceDestroy` | `Aion.GameServer.Services.WorldNpcWalkerRouteWalkingService.OnInstanceDestroy` | Service | Partial | Unit Tested | Partial Parity | C# removes active walker runtime state for the destroyed world/instance. Live destroy wiring still needs to pass this callback. |
| `com.aionemu.gameserver.spawnengine.InstanceWalkerFormations.onInstanceDestroy` | `Aion.GameServer.Services.WorldNpcWalkerRouteWalkingService.OnInstanceDestroy` | Cleanup | Partial | Unit Tested | Partial Parity | Java clears grouped spawn candidates and walk formations. C# cleans modeled runtime movement/formation state; map-scoped spawn plan cache remains a known limitation. |
| `com.aionemu.gameserver.services.instance.InstanceService.destroyInstance` walker branch | `Aion.GameServer.Services.InstanceRuntimeService.DestroyInstance` | Service Plan | Partial | Unit Tested | Partial Parity | Optional walker cleanup callback is invoked after instance handler notification, matching Java order. Live destroy composition remains pending. |

## Next Sequential UOW

Recommended next production scope: inspect live destroy composition points and wire the already-modeled callbacks together where the C# runtime has concrete dependencies available: temporary spawn cleanup, player forced-exit plan, non-player cleanup, destroy handler notification, and walker cleanup.

Java artifacts:

- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`
- `game-server/src/com/aionemu/gameserver/spawnengine/TemporarySpawnEngine.java`
- `game-server/src/com/aionemu/gameserver/spawnengine/WalkerFormator.java`
- `game-server/src/com/aionemu/gameserver/utils/PacketSendUtility.java`
- `game-server/src/com/aionemu/gameserver/services/teleport/TeleportService.java`

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Services/InstanceRuntimeService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/WorldNpcSpawnService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/WorldNpcWalkerRouteWalkingService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerTeleportService.cs`
- packet-send boundary discovered during Work Discovery
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldMapRuntimeStateTests.cs`
- closest live adapter/cleanup tests discovered during Work Discovery

Focused C# validation recipe:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldMapRuntimeStateTests.InstanceRuntimeService_DestroyInstanceRemovesThenNotifiesLikeJavaInstanceService|FullyQualifiedName~WorldNpcWalkerRouteWalkingServiceTests.OnInstanceDestroy_RemovesOnlyDestroyedInstanceWalkerStateLikeJavaWalkerFormator" --no-restore
```

If the next UOW enables live packet fanout or teleport mutation, document the broad-validation trigger first and add only the nearest live adapter tests to the filter.

Focused Java/Maven command: not expected unless a targeted Java fixture is added; Java source review is likely the practical evidence.

Broad-validation trigger: none for another non-live destroy composition. Live packet fanout, teleport mutation, scheduler expansion, shared runtime state mutation, or persistence is a broad-validation trigger; start focused and document it before any wider run.

## Safe Candidates

- Compose live destroy callbacks if the existing runtime objects are available.
- Add a narrow live forced-exit adapter if packet-send and teleport mutation boundaries are already modeled.
- Port empty-instance checker scheduling/cancellation.
- Improve instance-scoped walker spawn plan cache parity.

Avoid:

- Evidence/reporting-only units.
- Full `.NET` project tests or solution builds without a documented broad-validation trigger.
- Updating `docs/PHASE-6-PROGRESS.md`.

