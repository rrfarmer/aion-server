# Phase 6 Session 2347 Handoff - Schedule Empty Checker On Instance Creation

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2347-Completion.md`
- `docs/Phase-6-Session-2347-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger.

## Current State

Last completed UOW: UOW-2347, added Java auto-destroy scheduling semantics to C# instance creation helpers.

Relevant completed instance destroy/checker slices:

- `InstanceDestroyWorkflowService` composes the concrete destroy workflow.
- `InstanceEmptyInstanceCheckerService` models checker rules and can schedule the Java 60-second fixed-rate checker.
- `WorldMapInstanceRuntimeState` stores/cancels the checker task and records last player leave time.
- `InstanceRuntimeService.GetNextAvailableInstance(...)` and related overloads now expose `autoDestroy` and scheduler callback semantics.
- `InstanceRuntimeService.DestroyInstance(...)` cancels the stored checker task before map removal.

Still not proven or not implemented:

- Runtime call sites still need to pass `InstanceEmptyInstanceCheckerService.Schedule(...)`.
- Runtime registered-team disbanded lookup for checker decisions.
- Live forced-exit packet send and teleport mutation.
- Dynamic handler/auto-group destroy call sites invoking `InstanceDestroyWorkflowService`.
- Instance-scoped walker spawn plan cache parity.

## Commits Made

- `[Phase 6][UOW-2347] Schedule empty checker on instance creation`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Services/InstanceRuntimeService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldMapRuntimeStateTests.cs`
- `docs/Phase-6-Session-2347-Completion.md`
- `docs/Phase-6-Session-2347-Handoff.md`

Unrelated dirty file to preserve:

- `docs/parity-verification.md` may already be dirty and should remain unstaged unless the user explicitly asks to edit it.

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldMapRuntimeStateTests.InstanceRuntimeService_SchedulesAutoDestroyOnNewInstancesLikeJavaGetNextAvailableInstance|FullyQualifiedName~InstanceEmptyInstanceCheckerServiceTests.Schedule_StoresCancellableFixedRateTaskLikeJavaGetNextAvailableInstance" --no-restore
```

Result: passed 2, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Focused Java/Maven validation: skipped because no targeted Java unit fixture exists for `InstanceService.getNextAvailableInstance(..., autoDestroy)`; Java source review was used as source-of-truth evidence.

Broad-validation trigger: none beyond the focused scheduler/instance creation surface.

Broad .NET decision: skipped full project/solution validation because the filtered command compiled the affected project and directly covered the changed helper contract.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.instance.InstanceService.getNextAvailableInstance(..., autoDestroy)` | `Aion.GameServer.Services.InstanceRuntimeService.GetNextAvailableInstance(...)` | Service Plan | Partial | Unit Tested | Partial Parity | C# creation helpers expose Java auto-destroy scheduling semantics and callback ordering. Runtime callers still need to supply the scheduler service callback. |
| `com.aionemu.gameserver.services.instance.InstanceService.getNextAvailableInstance(worldId, Player)` | `Aion.GameServer.Services.InstanceRuntimeService.GetNextAvailableInstanceForPlayer(...)` | Service Plan | Partial | Unit Tested | Partial Parity | C# schedules before player registration, matching Java overload order. |

## Next Sequential UOW

Recommended next production scope: connect a real runtime caller to the scheduler callback. Start with the portal instance creation path in `GameServerConnection` if it has access to service dependencies; otherwise add a small runtime adapter/facade that calls `InstanceRuntimeService.CreatePortalTransferInstance(...)` with `InstanceEmptyInstanceCheckerService.Schedule(...)`.

Java artifacts:

- `game-server/src/com/aionemu/gameserver/services/PortalService.java`
- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/InstanceRuntimeService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/InstanceEmptyInstanceCheckerService.cs`
- adjacent portal/instance transfer tests discovered during Work Discovery

Focused C# validation recipe:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldMapRuntimeStateTests.InstanceRuntimeService_SchedulesAutoDestroyOnNewInstancesLikeJavaGetNextAvailableInstance|FullyQualifiedName~InstanceEmptyInstanceCheckerServiceTests.Schedule_StoresCancellableFixedRateTaskLikeJavaGetNextAvailableInstance" --no-restore
```

Add only the nearest portal/runtime adapter test if the next unit wires a concrete caller.

Focused Java/Maven command: not expected unless a targeted Java fixture is added; Java source review is likely the practical evidence.

Broad-validation trigger: scheduler wiring plus any live runtime caller. Start focused; do not run full project/solution validation unless focused evidence exposes wider risk.

## Safe Candidates

- Wire portal instance creation to pass the real empty-instance scheduler callback.
- Add runtime registered-team disbanded lookup for checker decisions.
- Add a narrow live forced-exit adapter if packet-send and teleport mutation boundaries are ready.
- Wire an existing C# instance handler/auto-group destroy call site to `InstanceDestroyWorkflowService`.

Avoid:

- Evidence/reporting-only units.
- Full `.NET` project tests or solution builds without a documented broad-validation trigger.
- Updating `docs/PHASE-6-PROGRESS.md`.
