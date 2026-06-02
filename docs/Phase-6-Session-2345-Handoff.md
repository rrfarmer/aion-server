# Phase 6 Session 2345 Handoff - Compose Instance Destroy Workflow

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2345-Completion.md`
- `docs/Phase-6-Session-2345-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger.

## Current State

Last completed UOW: UOW-2345, added an injectable runtime coordinator for Java `InstanceService.destroyInstance(...)`.

Relevant completed instance destroy slices:

- Modeled destroy order removes the map instance before cleanup/handler notification.
- Temporary spawn cleanup and non-player object deletion are concrete `WorldNpcSpawnService` methods.
- Player forced-exit plans include `STR_MSG_LEAVE_INSTANCE_FORCE(0)`, instance-exit resolution, and bind fallback destination resolution.
- Walker route-walking runtime state can be cleaned for a destroyed world/instance.
- `InstanceDestroyWorkflowService` now composes these concrete services and is registered in DI.

Still not proven or not implemented:

- Live packet send for forced-exit message.
- Live player teleport mutation for forced-exit destination.
- Empty-instance checker scheduling/cancellation.
- Dynamic handler/scheduler destroy call sites invoking `InstanceDestroyWorkflowService`.
- Instance-scoped walker spawn plan cache parity.

## Commits Made

- `[Phase 6][UOW-2345] Compose instance destroy workflow`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Services/InstanceDestroyWorkflowService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/InstanceRuntimeService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/GameServerRuntimeContext.cs`
- `dotnetConversion/src/Aion.GameServer/Program.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldMapRuntimeStateTests.cs`
- `docs/Phase-6-Session-2345-Completion.md`
- `docs/Phase-6-Session-2345-Handoff.md`

Unrelated dirty file to preserve:

- `docs/parity-verification.md` may already be dirty and should remain unstaged unless the user explicitly asks to edit it.

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldMapRuntimeStateTests.InstanceRuntimeService_DestroyInstanceRemovesThenNotifiesLikeJavaInstanceService|FullyQualifiedName~WorldMapRuntimeStateTests.InstanceDestroyWorkflowService_DestroyInstanceComposesRuntimeCallbacksLikeJavaInstanceService|FullyQualifiedName~WorldNpcWalkerRouteWalkingServiceTests.OnInstanceDestroy_RemovesOnlyDestroyedInstanceWalkerStateLikeJavaWalkerFormator" --no-restore
```

Result: passed 3, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Focused Java/Maven validation: skipped because no targeted Java unit fixture exists for `InstanceService.destroyInstance(...)`; Java source review was used as source-of-truth evidence.

Broad-validation trigger: shared runtime state mutation, isolated to instance/world/NPC/walker destroy composition.

Broad .NET decision: skipped full project/solution validation because focused tests compiled the affected project and directly covered the changed runtime composition.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.instance.InstanceService.destroyInstance` | `Aion.GameServer.Services.InstanceDestroyWorkflowService.DestroyInstance` | Service | Partial | Unit Tested | Partial Parity | Concrete C# runtime workflow now composes temporary-spawn cleanup, forced-exit planning, non-player deletion, handler notification, and walker cleanup. Live packet send, teleport mutation, empty-checker cancellation, and real call sites remain pending. |
| `com.aionemu.gameserver.services.instance.InstanceService.destroyInstance` | `Aion.GameServer.Services.InstanceRuntimeService.DestroyInstance` | Service Plan | Partial | Unit Tested | Partial Parity | Destroy plan carries player forced-exit teleport plans at the Java-equivalent point after temporary cleanup and before non-player cleanup/handler notification. |
| `com.aionemu.gameserver.spawnengine.TemporarySpawnEngine.onInstanceDestroy` | `Aion.GameServer.Services.WorldNpcSpawnService.UnregisterTemporarySpawnsForInstance` | Service | Partial | Unit Tested | Partial Parity | Runtime workflow invokes the concrete cleanup callback before non-player deletion. |
| `com.aionemu.gameserver.spawnengine.WalkerFormator.onInstanceDestroy` | `Aion.GameServer.Services.WorldNpcWalkerRouteWalkingService.OnInstanceDestroy` | Service | Partial | Unit Tested | Partial Parity | Runtime workflow invokes walker cleanup after handler notification. Instance-scoped walker spawn-plan cache parity remains pending. |

## Next Sequential UOW

Recommended next production scope: wire `InstanceDestroyWorkflowService` into the nearest actual instance-destroy call site that is already present in the C# runtime, or port the smallest missing call site if none exists.

Java artifacts:

- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
- `game-server/data/handlers/instance/crucible/CrucibleChallengeInstance.java`
- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`
- `game-server/src/com/aionemu/gameserver/services/instance/EmptyInstanceCheckerTask` inner class

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Services/InstanceDestroyWorkflowService.cs`
- nearest C# auto-group, instance handler, or scheduler artifact discovered during Work Discovery
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldMapRuntimeStateTests.cs`
- adjacent test for the selected handler/scheduler service

Focused C# validation recipe:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldMapRuntimeStateTests.InstanceDestroyWorkflowService_DestroyInstanceComposesRuntimeCallbacksLikeJavaInstanceService" --no-restore
```

If the next UOW enables live packet fanout, teleport mutation, or scheduler execution, document that broad-validation trigger first and add only the nearest adapter/scheduler tests to the filter.

Focused Java/Maven command: not expected unless a targeted Java fixture is added; Java source review is likely the practical evidence.

Broad-validation trigger: none for selecting or modeling the call site. Live packet fanout, teleport mutation, scheduler expansion, shared runtime state mutation, or persistence is a broad-validation trigger; start focused and document it before any wider run.

## Safe Candidates

- Wire an existing C# instance handler/scheduler destroy call site to `InstanceDestroyWorkflowService`.
- Port empty-instance checker scheduling/cancellation if no handler call site is ready.
- Add a narrow live forced-exit adapter if packet-send and teleport mutation boundaries are already modeled.
- Improve instance-scoped walker spawn plan cache parity.

Avoid:

- Evidence/reporting-only units.
- Full `.NET` project tests or solution builds without a documented broad-validation trigger.
- Updating `docs/PHASE-6-PROGRESS.md`.
