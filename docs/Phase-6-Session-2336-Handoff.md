# Phase 6 Session 2336 Handoff - Add Instance Destroy Lifecycle Hook

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2336-Completion.md`
- `docs/Phase-6-Session-2336-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger.

## Current State

Last completed UOW: UOW-2336, C# modeled instance destroy now removes the map instance before invoking a one-shot `onInstanceDestroy`-style lifecycle hook.

Relevant completed portal/instance/spawn slices:

- Fresh solo/group allocation paths spawn instance objects and notify `onInstanceCreate` after spawning.
- Portal difficulty is preserved on top-level allowed plans and team plans.
- Instance destroy lifecycle hook exists for modeled runtime destroy.
- Instance spawning filters by nonzero difficulty id and uses the allocated runtime instance id.
- Static door and static handler object materialization are modeled for instance spawning.

Still not proven or not implemented:

- Temporary spawn unregister/despawn on instance destroy.
- Player move-to-exit/object deletion during destroy.
- Empty-instance checker scheduling and cancellation.
- Walker formation cleanup.
- Dynamic Java AI/instance handler discovery and per-map/per-AI handler class selection.
- Event-specific instance spawns and custom instance handler suppliers.

## Commits Made

- `bf37e6fe0 [Phase 6][UOW-2335] Carry portal difficulty through solo plans`
- `[Phase 6][UOW-2336] Add instance destroy lifecycle hook`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/World/IInstanceLifecycleHandler.cs`
- `dotnetConversion/src/Aion.GameServer/World/WorldMapInstanceRuntimeState.cs`
- `dotnetConversion/src/Aion.GameServer/Services/InstanceRuntimeService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldMapRuntimeStateTests.cs`
- `docs/Phase-6-Session-2336-Completion.md`
- `docs/Phase-6-Session-2336-Handoff.md`

Unrelated dirty file to preserve:

- `docs/parity-verification.md` was already dirty and should remain unstaged unless the user explicitly asks to edit it.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.instance.handlers.InstanceHandler.onInstanceDestroy` | `Aion.GameServer.World.IInstanceLifecycleHandler.OnInstanceDestroy` | Interface | Partial | Unit Tested | Partial Parity | C# has the destroy callback surface and one-shot notification. Other Java handler methods and dynamic handler discovery remain missing. |
| `com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler.onInstanceDestroy` | `Aion.GameServer.World.GeneralInstanceLifecycleHandler.OnInstanceDestroy` | Handler | Complete | Unit Tested | Verified Parity | Java method is a no-op; C# no-op behavior is deterministic and covered by lifecycle tests. |
| `com.aionemu.gameserver.services.instance.InstanceService.destroyInstance` remove-before-handler ordering | `InstanceRuntimeService.DestroyInstance` | Service Boundary | Partial | Unit Tested | Partial Parity | C# removes the modeled map instance before notifying destroy. Temporary spawn cleanup, object deletion/player exit, empty task cancellation, and walker cleanup remain pending. |

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldMapRuntimeStateTests.InstanceRuntimeService_DestroyInstanceRemovesThenNotifiesLikeJavaInstanceService|FullyQualifiedName~WorldMapRuntimeStateTests.WorldMapInstanceRuntimeState_NotifiesInstanceCreateOnceLikeJavaInstanceService" --no-restore
```

Result: passed 2, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Hygiene:

```powershell
git diff --check
```

Result: passed; only Git line-ending warnings were reported.

Focused Java/Maven validation: skipped because no targeted Java fixture exists for `InstanceService.destroyInstance(...)`. Java source review was used as source-of-truth evidence.

Broad-validation trigger: none.

Broad .NET decision: skipped full project/solution validation because focused tests compiled the affected project and directly covered the changed lifecycle model.

## Next Sequential UOW

Recommended next production scope: inspect Java `TemporarySpawnEngine.onInstanceDestroy(...)` and current C# temporary spawn tracking, then port the smallest unregister/despawn slice if the C# surface is ready.

Java artifacts:

- `game-server/src/com/aionemu/gameserver/spawnengine/TemporarySpawnEngine.java`
- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`
- `game-server/src/com/aionemu/gameserver/spawnengine/SpawnEngine.java`

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Services/WorldNpcSpawnService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/InstanceRuntimeService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldNpcSpawnServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldMapRuntimeStateTests.cs`

Focused C# validation recipe:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldNpcSpawnServiceTests|FullyQualifiedName~WorldMapRuntimeStateTests.InstanceRuntimeService_DestroyInstanceRemovesThenNotifiesLikeJavaInstanceService" --no-restore
```

Narrow after Work Discovery to exact temporary-spawn/destroy tests. Do not run a full project/solution test unless a broad trigger is named first.

Focused Java/Maven command: not expected unless a targeted Java fixture is added; Java source review is likely the practical evidence.

Broad-validation trigger: none unless the unit enables live scheduler cleanup or broad object deletion.

## Safe Candidates

- Temporary spawn unregister/despawn on modeled instance destroy if C# temporary spawn tracking is ready.
- Walker formation cleanup if C# walker formation state is ready.
- Event-specific instance spawn branch for Java `SpawnEngine.spawnEventSpawns(...)`.
- Static object known-list/visibility model if C# known-list infrastructure already supports non-NPC visible objects.

Avoid:

- Full `.NET` project tests or solution builds without a documented broad-validation trigger.
- Evidence/reporting-only units.
- Updating `docs/PHASE-6-PROGRESS.md`.
