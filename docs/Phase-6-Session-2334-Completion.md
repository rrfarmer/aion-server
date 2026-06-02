# Phase 6 Session 2334 Completion - Add Instance Create Lifecycle Hook

## Scope

Added a narrow C# lifecycle hook for Java `InstanceHandler.onInstanceCreate()` and invoked it for fresh portal allocations after C# instance spawning.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`
- `game-server/src/com/aionemu/gameserver/instance/handlers/InstanceHandler.java`
- `game-server/src/com/aionemu/gameserver/instance/handlers/GeneralInstanceHandler.java`

Java behavior used:

- `InstanceService.getNextAvailableInstance(...)` allocates the instance, runs instance spawns, then calls `instance.getInstanceHandler().onInstanceCreate()`.
- `InstanceHandler.onInstanceCreate()` is a lifecycle callback with no parameters.
- `GeneralInstanceHandler.onInstanceCreate()` is a no-op.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

Updated:

- `dotnetConversion/src/Aion.GameServer/World/IInstanceLifecycleHandler.cs`
- `dotnetConversion/src/Aion.GameServer/World/WorldMapInstanceRuntimeState.cs`
- `dotnetConversion/src/Aion.GameServer/World/WorldMapRuntimeState.cs`
- `dotnetConversion/src/Aion.GameServer/World/WorldMapRuntimeStateTable.cs`
- `dotnetConversion/src/Aion.GameServer/Services/InstanceRuntimeService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldMapRuntimeStateTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInstanceCooldownTests.cs`

Implemented:

- Added `IInstanceLifecycleHandler.OnInstanceCreate(...)` and a no-op `GeneralInstanceLifecycleHandler`.
- `WorldMapInstanceRuntimeState` now stores its lifecycle handler and exposes `NotifyInstanceCreated()` as a one-shot callback.
- Runtime map/table/allocation helpers can carry an optional lifecycle handler for future dynamic handler ports.
- Fresh solo and fresh group portal allocation paths call `NotifyInstanceCreated()` after `SpawnWorldNpcsForInstance(...)` and before transfer.
- Focused tests verify one-shot notification and that the solo allocation callback sees spawned NPCs already materialized.

Known limitations:

- This does not port Java dynamic instance handler discovery or per-map handler classes.
- Only `onInstanceCreate()` is modeled; `onInstanceDestroy()` and other `InstanceHandler` methods remain missing.
- Event/custom instance handler supplier behavior remains pending.

## Validation Decision

- Changed surface: world instance lifecycle model plus fresh portal allocation side effect.
- Specific behavior/contract: C# should notify the instance creation handler exactly once, after fresh instance spawns are materialized and before portal transfer continues.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldMapRuntimeStateTests.WorldMapInstanceRuntimeState_NotifiesInstanceCreateOnceLikeJavaInstanceService|FullyQualifiedName~GameServerConnectionInstanceCooldownTests.QueueAllocatedInstancePortalTransferAsync_AllocatesRegistersSpawnsAndTransfersLikeJavaInstanceService" --no-restore
```

Result: passed 2, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

- Hygiene command:

```powershell
git diff --check
```

Result: passed; only Git line-ending warnings were reported.

- Focused Java/Maven command: skipped. No targeted Java fixture exists for `InstanceService.getNextAvailableInstance(...)` lifecycle ordering; Java source review was used as source-of-truth evidence.
- Broad-validation trigger: live portal allocation lifecycle side effect.
- Broad .NET decision: skipped full project/solution validation because focused tests compiled the affected project and directly exercised the one-shot hook plus the fresh allocation ordering. Dynamic handler loading, persistence, packet primitives, and hosted scheduler primitives were not changed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.instance.handlers.InstanceHandler.onInstanceCreate` | `Aion.GameServer.World.IInstanceLifecycleHandler.OnInstanceCreate` | Interface | Partial | Unit Tested | Partial Parity | C# has the creation callback surface and invokes it once after fresh allocation spawns. Other Java handler methods and dynamic handler discovery remain missing. |
| `com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler.onInstanceCreate` | `Aion.GameServer.World.GeneralInstanceLifecycleHandler.OnInstanceCreate` | Handler | Complete | Unit Tested | Verified Parity | Java method is a no-op; C# no-op behavior is deterministic and covered by allocation callback tests. |
| `com.aionemu.gameserver.services.instance.InstanceService.getNextAvailableInstance` lifecycle ordering | `GameServerConnection` fresh allocation paths plus `WorldMapInstanceRuntimeState.NotifyInstanceCreated` | Service Boundary | Partial | Unit Tested | Partial Parity | C# fresh portal paths now notify after spawn and before transfer. Non-portal instance creation, event/custom handler suppliers, empty checker, and destroy lifecycle remain pending. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `WorldMapInstanceRuntimeState_NotifiesInstanceCreateOnceLikeJavaInstanceService` | Unit | Java source review | Runtime state calls its lifecycle handler once and suppresses duplicate creation notifications. | Focused C# unit test plus Java source review. | Does not cover dynamic handler discovery. |
| `QueueAllocatedInstancePortalTransferAsync_AllocatesRegistersSpawnsAndTransfersLikeJavaInstanceService` | Unit | Java source review | Fresh solo allocation invokes the lifecycle handler after spawned NPCs exist and before transfer/cooldown completes. | Focused C# unit test plus Java source review. | Does not cover real-client encrypted bytes or custom handler implementations. |

## Remaining Gaps

- Dynamic Java instance handler discovery and per-map handler selection.
- `InstanceHandler.onInstanceDestroy()` and other lifecycle/event methods.
- Empty-instance checker behavior.
- Higher-level solo portal difficulty propagation for Beshmundir-style branches.
- Event-specific instance spawns for Java `SpawnEngine.spawnEventSpawns(...)`.

## Commit

Commit message:

```text
[Phase 6][UOW-2334] Add instance create lifecycle hook
```

## Next Recommended UOW

Continue with one of:

- Higher-level Beshmundir solo difficulty propagation into `PortalEntryPlanResult`.
- `InstanceHandler.onInstanceDestroy()` plus remove/destroy instance lifecycle if a small C# destroy path is available.
- Event-specific instance spawns if C# event data is ready.
