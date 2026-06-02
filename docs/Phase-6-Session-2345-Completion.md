# Phase 6 Session 2345 Completion - Compose Instance Destroy Workflow

## Scope

Added a concrete C# runtime workflow service for Java `InstanceService.destroyInstance(...)` using the already-ported destroy pieces.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`
- `game-server/src/com/aionemu/gameserver/spawnengine/TemporarySpawnEngine.java`
- `game-server/src/com/aionemu/gameserver/spawnengine/WalkerFormator.java`
- `game-server/src/com/aionemu/gameserver/services/teleport/TeleportService.java`

Java behavior used:

- Cancel/remove instance, unregister temporary spawns, force-exit players, delete non-player visible objects, notify the instance handler, then clean walker formations.
- `moveToInstanceExit(...)` resolves a configured instance exit when target instance `1` exists, otherwise falls back to bind-location logic.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

- Added `InstanceDestroyWorkflowService` as the injectable runtime coordinator for instance destruction.
- Registered `InstanceDestroyWorkflowService` in game-server DI.
- Extended `InstanceDestroyRuntimePlan` to carry player forced-exit teleport plans.
- Added `GameServerRuntimeContext.SetWorldMapStates(...)` for lightweight runtime-state seeding.
- Added a focused unit test that composes real `World`, `WorldNpcSpawnService`, and walker cleanup services.

Known limitations:

- The workflow records forced-exit packet and teleport plans but does not yet send packets or mutate player positions.
- Empty-instance checker cancellation is still not ported.
- Instance destroy call sites in dynamic handlers/scheduler code still need to call this workflow.

## Validation Decision

- Changed surface: runtime destroy workflow composition and shared instance/world/NPC runtime state.
- Specific behavior/contract: Java destroy order is composed through concrete C# runtime services; player forced-exit destinations are planned after map instance removal and before handler notification; non-player objects are removed while players remain available for forced exit.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldMapRuntimeStateTests.InstanceRuntimeService_DestroyInstanceRemovesThenNotifiesLikeJavaInstanceService|FullyQualifiedName~WorldMapRuntimeStateTests.InstanceDestroyWorkflowService_DestroyInstanceComposesRuntimeCallbacksLikeJavaInstanceService|FullyQualifiedName~WorldNpcWalkerRouteWalkingServiceTests.OnInstanceDestroy_RemovesOnlyDestroyedInstanceWalkerStateLikeJavaWalkerFormator" --no-restore
```

Result: passed 3, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

- Focused Java/Maven command: skipped. No targeted Java unit fixture exists for `InstanceService.destroyInstance(...)`; Java source review was used as source-of-truth evidence.
- Broad-validation trigger: shared runtime state mutation, isolated to instance/world/NPC/walker destroy composition.
- Broad .NET decision: skipped full project/solution validation because the focused command compiled the affected project and directly covered the changed runtime composition. No packet primitive, persistence, crypto, scheduler, or live connection dispatch code changed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.instance.InstanceService.destroyInstance` | `Aion.GameServer.Services.InstanceDestroyWorkflowService.DestroyInstance` | Service | Partial | Unit Tested | Partial Parity | Concrete C# runtime workflow now composes temporary-spawn cleanup, forced-exit planning, non-player deletion, handler notification, and walker cleanup. Live packet send, teleport mutation, empty-checker cancellation, and real call sites remain pending. |
| `com.aionemu.gameserver.services.instance.InstanceService.destroyInstance` | `Aion.GameServer.Services.InstanceRuntimeService.DestroyInstance` | Service Plan | Partial | Unit Tested | Partial Parity | Destroy plan now carries player forced-exit teleport plans at the Java-equivalent point after temporary cleanup and before non-player cleanup/handler notification. |
| `com.aionemu.gameserver.spawnengine.TemporarySpawnEngine.onInstanceDestroy` | `Aion.GameServer.Services.WorldNpcSpawnService.UnregisterTemporarySpawnsForInstance` | Service | Partial | Unit Tested | Partial Parity | Runtime workflow invokes the concrete cleanup callback before non-player deletion. Exact Java temporary-spawn object lifecycle remains covered by prior focused tests only. |
| `com.aionemu.gameserver.spawnengine.WalkerFormator.onInstanceDestroy` | `Aion.GameServer.Services.WorldNpcWalkerRouteWalkingService.OnInstanceDestroy` | Service | Partial | Unit Tested | Partial Parity | Runtime workflow invokes walker cleanup after handler notification. Instance-scoped walker spawn-plan cache parity remains pending. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `InstanceDestroyWorkflowService_DestroyInstanceComposesRuntimeCallbacksLikeJavaInstanceService` | Unit | Java source review | Runtime workflow removes the map instance, plans forced player exit, deletes a non-player NPC, notifies handler after removal, and invokes walker cleanup. | Focused C# test plus Java source review. | Does not send packets, mutate player position, cancel empty-instance task, or exercise dynamic handler call sites. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported or extended in this UOW: 4
- Total artifacts with verified parity: 0
- Total artifacts needing verification or remaining partial: 4
- Total blocked artifacts: 0
- Estimated overall migration completion: Phase 6 remains in progress; overall completion unchanged conservatively.

## Remaining Gaps

- Live forced-exit packet send and teleport dispatch.
- Dynamic handler/scheduler call sites for instance destruction.
- Empty-instance checker scheduling/cancellation.
- Instance-scoped walker spawn plan cache parity.

## Commit

Commit message:

```text
[Phase 6][UOW-2345] Compose instance destroy workflow
```
