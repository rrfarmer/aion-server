# Phase 6 Session 2344 Completion - Cleanup Instance Walker State

## Scope

Ported the destroy-path walker cleanup hook for Java `WalkerFormator.onInstanceDestroy(worldId, instanceId)` into the modeled C# destroy flow.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`
- `game-server/src/com/aionemu/gameserver/spawnengine/WalkerFormator.java`
- `game-server/src/com/aionemu/gameserver/spawnengine/WalkerFormationsCache.java`
- `game-server/src/com/aionemu/gameserver/spawnengine/WorldWalkerFormations.java`
- `game-server/src/com/aionemu/gameserver/spawnengine/InstanceWalkerFormations.java`

Java behavior used:

- `InstanceService.destroyInstance(...)` calls `WalkerFormator.onInstanceDestroy(worldId, instanceId)` after `instance.getInstanceHandler().onInstanceDestroy()`.
- `WalkerFormator.onInstanceDestroy(...)` delegates to `WalkerFormationsCache.onInstanceDestroy(...)`.
- `InstanceWalkerFormations.onInstanceDestroy()` clears grouped spawn candidates and walk formations for the destroyed instance.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

Updated:

- `dotnetConversion/src/Aion.GameServer/Services/InstanceRuntimeService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/WorldNpcWalkerRouteWalkingService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldMapRuntimeStateTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldNpcWalkerRouteWalkingServiceTests.cs`

Implemented:

- Added `WorldNpcWalkerRouteWalkingService.OnInstanceDestroy(worldId, instanceId)` to remove active walker movement state, cancel pending walker tasks, clear formation runtime state, and stop AI walking state for NPCs in the destroyed map instance.
- Added `WorldNpcWalkerInstanceDestroyCleanupResult`.
- Added an optional `walkerFormationCleanup` callback to `InstanceRuntimeService.DestroyInstance(...)`, invoked after instance handler notification.
- Extended focused tests for Java destroy order and instance-scoped walker cleanup.

Known limitations:

- C# walker spawn plan cache is still map-scoped, while Java `InstanceWalkerFormations` is instance-scoped; this UOW cleans the active route-walking runtime state without clearing the map-scoped spawn plan.
- Live server destroy wiring still needs to pass the walker cleanup callback.
- Java clears only grouped spawn candidates and walk formations; C# also stops modeled active walker runtime/AI state because that is where the current port stores moving formation state.

## Validation Decision

- Changed surface: non-live instance destroy callback ordering plus walker runtime cleanup.
- Specific behavior/contract: Java destroy order invokes walker cleanup after the instance handler; walker cleanup removes only state for the destroyed world/instance and preserves another active instance.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldMapRuntimeStateTests.InstanceRuntimeService_DestroyInstanceRemovesThenNotifiesLikeJavaInstanceService|FullyQualifiedName~WorldNpcWalkerRouteWalkingServiceTests.OnInstanceDestroy_RemovesOnlyDestroyedInstanceWalkerStateLikeJavaWalkerFormator" --no-restore
```

Result: passed 2, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

- Focused Java/Maven command: skipped. No targeted Java unit fixture exists for `WalkerFormator.onInstanceDestroy(...)`; Java source review was used as source-of-truth evidence.
- Broad-validation trigger: none. This UOW adds a focused cleanup hook and tests it directly; it does not enable live packet fanout, teleport mutation, persistence, or scheduler behavior beyond canceling already modeled pending walker tasks.
- Broad .NET decision: skipped full project/solution validation; the filtered command built the affected project and covered the edited behavior.

Repository hygiene:

```powershell
git diff --check
```

Result: passed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.spawnengine.WalkerFormator.onInstanceDestroy` | `Aion.GameServer.Services.WorldNpcWalkerRouteWalkingService.OnInstanceDestroy` | Service | Partial | Unit Tested | Partial Parity | C# removes active walker runtime state for the destroyed world/instance and records Java source. Live destroy wiring still needs to pass this callback. |
| `com.aionemu.gameserver.spawnengine.InstanceWalkerFormations.onInstanceDestroy` | `Aion.GameServer.Services.WorldNpcWalkerRouteWalkingService.OnInstanceDestroy` | Cleanup | Partial | Unit Tested | Partial Parity | Java clears grouped spawn candidates and walk formations. C# cleans modeled runtime movement/formation state; map-scoped spawn plan cache remains a known limitation. |
| `com.aionemu.gameserver.services.instance.InstanceService.destroyInstance` walker branch | `Aion.GameServer.Services.InstanceRuntimeService.DestroyInstance` | Service Plan | Partial | Unit Tested | Partial Parity | Optional walker cleanup callback is invoked after instance handler notification, matching Java order. Live destroy composition remains pending. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `InstanceRuntimeService_DestroyInstanceRemovesThenNotifiesLikeJavaInstanceService` | Unit | Java source review | Destroy callback order now includes walker cleanup after instance handler notification. | Focused C# test plus Java source review. | Does not run live server destroy. |
| `OnInstanceDestroy_RemovesOnlyDestroyedInstanceWalkerStateLikeJavaWalkerFormator` | Unit | Java source review | Active walker and formation state for the destroyed instance is removed while another active world/instance remains. | Focused C# test plus Java source review. | Does not clear map-scoped spawn plan cache. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 5
- Total artifacts ported or extended in this UOW: 3
- Total artifacts with verified parity: 0
- Total artifacts needing verification or remaining partial: 3
- Total blocked artifacts: 0
- Estimated overall migration completion: Phase 6 remains in progress; overall completion unchanged conservatively.

## Remaining Gaps

- Live forced-exit packet send and teleport dispatch.
- Live destroy wiring with temporary cleanup, player forced-exit planning, non-player cleanup, handler notification, and walker cleanup.
- Empty-instance checker scheduling/cancellation.
- Instance-scoped walker spawn plan cache parity.

## Commit

Commit message:

```text
[Phase 6][UOW-2344] Cleanup instance walker state
```

