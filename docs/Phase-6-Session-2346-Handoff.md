# Phase 6 Session 2346 Handoff - Port Empty Instance Checker Lifecycle

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2346-Completion.md`
- `docs/Phase-6-Session-2346-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger.

## Current State

Last completed UOW: UOW-2346, ported empty-instance checker state, planning, scheduling cadence, and destroy-time cancellation.

Relevant completed instance destroy/checker slices:

- `InstanceDestroyWorkflowService` composes temporary-spawn cleanup, forced-exit planning, non-player deletion, handler notification, and walker cleanup.
- `WorldMapInstanceRuntimeState` stores a cancellable empty-instance task and last player leave time.
- `InstanceEmptyInstanceCheckerService` models Java `EmptyInstanceCheckerTask.canDestroyInstance()` and schedules a fixed-rate checker with Java 60-second delay/period.
- `InstanceRuntimeService.DestroyInstance(...)` cancels the stored empty-instance task before map removal.

Still not proven or not implemented:

- New C# instance creation does not yet call `InstanceEmptyInstanceCheckerService.Schedule(...)`.
- Runtime registered-team disbanded lookup for checker decisions.
- Live forced-exit packet send and teleport mutation.
- Dynamic handler/auto-group destroy call sites invoking `InstanceDestroyWorkflowService`.
- Instance-scoped walker spawn plan cache parity.

## Commits Made

- `[Phase 6][UOW-2346] Port empty instance checker lifecycle`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Utils/ThreadPoolManager.cs`
- `dotnetConversion/src/Aion.GameServer/World/WorldMapInstanceRuntimeState.cs`
- `dotnetConversion/src/Aion.GameServer/Services/InstanceRuntimeService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/InstanceEmptyInstanceCheckerService.cs`
- `dotnetConversion/src/Aion.GameServer/Program.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/InstanceEmptyInstanceCheckerServiceTests.cs`
- `docs/Phase-6-Session-2346-Completion.md`
- `docs/Phase-6-Session-2346-Handoff.md`

Unrelated dirty file to preserve:

- `docs/parity-verification.md` may already be dirty and should remain unstaged unless the user explicitly asks to edit it.

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~InstanceEmptyInstanceCheckerServiceTests|FullyQualifiedName~WorldMapRuntimeStateTests.InstanceDestroyWorkflowService_DestroyInstanceComposesRuntimeCallbacksLikeJavaInstanceService|FullyQualifiedName~InstanceServiceFormulaServiceTests" --no-restore
```

Result: passed 11, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Focused Java/Maven validation: skipped because no targeted Java unit fixture exists for `EmptyInstanceCheckerTask`; Java source review was used as source-of-truth evidence.

Broad-validation trigger: scheduler expansion plus shared instance runtime state.

Broad .NET decision: skipped full project/solution validation because focused tests compiled the affected project and covered the scheduler handle, checker rules, and destroy cancellation path.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.instance.InstanceService.EmptyInstanceCheckerTask` | `Aion.GameServer.Services.InstanceEmptyInstanceCheckerService` | Service | Partial | Unit Tested | Partial Parity | Checker rules and schedule cadence are ported. Automatic scheduling from instance creation is still pending; registered-team disbanded lookup is currently supplied as an input. |
| `com.aionemu.gameserver.world.WorldMapInstance` empty task and last leave state | `Aion.GameServer.World.WorldMapInstanceRuntimeState` | Runtime State | Partial | Unit Tested | Partial Parity | C# stores/cancels the empty-instance task and records last player leave time on player removal. Java zone/fly side effects on player removal are outside this slice. |
| `com.aionemu.gameserver.utils.ThreadPoolManager.scheduleAtFixedRate` | `Aion.GameServer.Utils.ThreadPoolManager.ScheduleAtFixedRateTask` | Scheduler | Partial | Unit Tested | Partial Parity | Added cancellable fixed-rate handle to model Java `Future<?>`; existing `ScheduleAtFixedRate` API remains. |
| `com.aionemu.gameserver.services.instance.InstanceService.destroyInstance` empty task branch | `Aion.GameServer.Services.InstanceRuntimeService.DestroyInstance` | Service Plan | Partial | Unit Tested | Partial Parity | Destroy now cancels stored empty-instance task before removing the map instance, matching Java order. |

## Next Sequential UOW

Recommended next production scope: wire automatic empty-instance checker scheduling into C# instance creation when `autoDestroy` is requested, matching Java `InstanceService.getNextAvailableInstance(..., autoDestroy)`.

Java artifacts:

- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`
- `game-server/src/com/aionemu/gameserver/world/WorldMapInstance.java`

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Services/InstanceRuntimeService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/InstanceEmptyInstanceCheckerService.cs`
- `dotnetConversion/src/Aion.GameServer/World/WorldMapInstanceRuntimeState.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldMapRuntimeStateTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/InstanceEmptyInstanceCheckerServiceTests.cs`

Focused C# validation recipe:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~InstanceEmptyInstanceCheckerServiceTests|FullyQualifiedName~WorldMapRuntimeStateTests" --no-restore
```

If this is too broad, narrow to the specific new instance-creation test plus `InstanceEmptyInstanceCheckerServiceTests.Schedule_StoresCancellableFixedRateTaskLikeJavaGetNextAvailableInstance`.

Focused Java/Maven command: not expected unless a targeted Java fixture is added; Java source review is likely the practical evidence.

Broad-validation trigger: scheduler expansion and shared instance runtime state. Start focused; do not run full project/solution validation unless focused evidence exposes wider risk.

## Safe Candidates

- Add `autoDestroy` scheduling to C# instance creation overloads using the checker service.
- Add runtime registered-team disbanded lookup once C# team runtime exposes disbanded state.
- Add a narrow live forced-exit adapter if packet-send and teleport mutation boundaries are ready.
- Wire an existing C# instance handler/auto-group destroy call site to `InstanceDestroyWorkflowService`.

Avoid:

- Evidence/reporting-only units.
- Full `.NET` project tests or solution builds without a documented broad-validation trigger.
- Updating `docs/PHASE-6-PROGRESS.md`.
