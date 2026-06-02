# Phase 6 Session 2346 Completion - Port Empty Instance Checker Lifecycle

## Scope

Ported the Java empty-instance checker lifecycle around `InstanceService.getNextAvailableInstance(...)` and `InstanceService.destroyInstance(...)`.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`
- `game-server/src/com/aionemu/gameserver/world/WorldMapInstance.java`

Java behavior used:

- New auto-destroy instances store a `Future` from `ThreadPoolManager.scheduleAtFixedRate(..., 60000, 60000)`.
- `EmptyInstanceCheckerTask.canDestroyInstance()` blocks while players are inside.
- Empty personal instances and instances with disbanded registered teams can be destroyed immediately.
- Otherwise, destroy is allowed when `now > max(taskStartTime, lastPlayerLeaveTime) + destroyDelay - 1000ms`.
- `WorldMapInstance.removeObject(Player)` records `lastPlayerLeaveTime`.
- `InstanceService.destroyInstance(...)` cancels the stored empty-instance task before removing the map instance.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

- Added `ThreadPoolManager.ScheduleAtFixedRateTask(...)` to return a cancellable `ScheduledTask` while preserving the existing `ScheduleAtFixedRate(...)` API.
- Added `WorldMapInstanceRuntimeState.LastPlayerLeaveTime`, `SetEmptyInstanceTask(...)`, and `CancelEmptyInstanceTask()`.
- Updated player removal to record last leave time.
- Updated `InstanceRuntimeService.DestroyInstance(...)` to cancel the stored empty-instance task before map removal.
- Added `InstanceEmptyInstanceCheckerService` for Java checker planning and scheduling.
- Registered `InstanceEmptyInstanceCheckerService` in DI.
- Added focused tests for checker branches, Java 60-second cadence, stored task cancellation, and destroy-time cancellation.

Known limitations:

- Runtime registered-team disbanded lookup is still an input to the checker plan because the current C# instance state stores only the registered team id.
- The checker service is registered and schedule-capable, but new instance creation does not yet call it automatically.

## Validation Decision

- Changed surface: scheduler handle, instance runtime state, and instance destroy runtime state.
- Specific behavior/contract: Java empty-instance checker destruction rules, 60-second fixed-rate scheduling, last-player-leave timestamping, and cancellation before destroy.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~InstanceEmptyInstanceCheckerServiceTests|FullyQualifiedName~WorldMapRuntimeStateTests.InstanceDestroyWorkflowService_DestroyInstanceComposesRuntimeCallbacksLikeJavaInstanceService|FullyQualifiedName~InstanceServiceFormulaServiceTests" --no-restore
```

Result: passed 11, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

- Focused Java/Maven command: skipped. No targeted Java unit fixture exists for `EmptyInstanceCheckerTask`; Java source review was used as source-of-truth evidence.
- Broad-validation trigger: scheduler expansion plus shared instance runtime state.
- Broad .NET decision: skipped full project/solution validation because the focused command compiled the affected project and directly covered the scheduler handle, checker rules, and destroy cancellation path.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.instance.InstanceService.EmptyInstanceCheckerTask` | `Aion.GameServer.Services.InstanceEmptyInstanceCheckerService` | Service | Partial | Unit Tested | Partial Parity | Checker rules and schedule cadence are ported. Automatic scheduling from instance creation is still pending; registered-team disbanded lookup is currently supplied as an input. |
| `com.aionemu.gameserver.world.WorldMapInstance` empty task and last leave state | `Aion.GameServer.World.WorldMapInstanceRuntimeState` | Runtime State | Partial | Unit Tested | Partial Parity | C# stores/cancels the empty-instance task and records last player leave time on player removal. Java zone/fly side effects on player removal are outside this slice. |
| `com.aionemu.gameserver.utils.ThreadPoolManager.scheduleAtFixedRate` | `Aion.GameServer.Utils.ThreadPoolManager.ScheduleAtFixedRateTask` | Scheduler | Partial | Unit Tested | Partial Parity | Added cancellable fixed-rate handle to model Java `Future<?>`; existing `ScheduleAtFixedRate` API remains. |
| `com.aionemu.gameserver.services.instance.InstanceService.destroyInstance` empty task branch | `Aion.GameServer.Services.InstanceRuntimeService.DestroyInstance` | Service Plan | Partial | Unit Tested | Partial Parity | Destroy now cancels stored empty-instance task before removing the map instance, matching Java order. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `CreateCheckPlan_BlocksDestroyWhilePlayersInsideLikeJavaEmptyInstanceCheckerTask` | Unit | Java source review | Player presence blocks empty-instance destroy. | Focused C# test plus Java source review. | Does not execute live scheduled callback. |
| `CreateCheckPlan_UsesPersonalTeamAndDelayBranchesLikeJavaEmptyInstanceCheckerTask` | Unit | Java source review | Personal, disbanded-team, waiting, and elapsed-delay branches. | Focused C# test plus Java source review. | Registered-team disbanded state is supplied as an input. |
| `Schedule_StoresCancellableFixedRateTaskLikeJavaGetNextAvailableInstance` | Unit | Java source review | Checker schedule stores a cancellable task with Java 60-second initial delay and period. | Focused C# test plus Java source review. | Does not wait for the real 60-second callback. |
| `DestroyInstance_CancelsStoredEmptyInstanceTaskBeforeRemovingMapLikeJavaDestroyInstance` | Unit | Java source review | Destroy cancels the stored empty-instance task before map removal. | Focused C# test plus Java source review. | Does not invoke full destroy workflow. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 3
- Total artifacts ported or extended in this UOW: 4
- Total artifacts with verified parity: 0
- Total artifacts needing verification or remaining partial: 4
- Total blocked artifacts: 0
- Estimated overall migration completion: Phase 6 remains in progress; overall completion unchanged conservatively.

## Remaining Gaps

- Automatic checker scheduling from C# instance creation.
- Runtime registered-team disbanded lookup for the checker.
- Live forced-exit packet send and teleport dispatch.
- Dynamic handler/auto-group destroy call sites.

## Commit

Commit message:

```text
[Phase 6][UOW-2346] Port empty instance checker lifecycle
```
