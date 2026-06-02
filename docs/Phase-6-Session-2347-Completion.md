# Phase 6 Session 2347 Completion - Schedule Empty Instance Checker On Creation

## Scope

Wired Java `InstanceService.getNextAvailableInstance(..., autoDestroy)` scheduling semantics into the C# instance creation helpers.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`

Java behavior used:

- `getNextAvailableInstance(..., autoDestroy)` starts `EmptyInstanceCheckerTask` only when `autoDestroy` is true.
- `getNextAvailableInstance(worldId, player)` calls the base creation method first and registers the player afterward.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

- Added `autoDestroy` and `emptyInstanceScheduler` parameters to C# instance creation helpers.
- Propagated scheduling through player, registered-instance, and portal-transfer creation paths.
- Added a focused test proving scheduling happens by default, is skipped when disabled, and occurs before player registration.

Known limitations:

- Runtime callers still need to pass `InstanceEmptyInstanceCheckerService.Schedule(...)` as the scheduler callback.
- C# instance creation still does not perform Java spawn/handler-create side effects in the same method.

## Validation Decision

- Changed surface: instance creation helper scheduling hook.
- Specific behavior/contract: Java auto-destroy scheduling is requested only when `autoDestroy` is true, and player overload scheduling occurs before player registration.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldMapRuntimeStateTests.InstanceRuntimeService_SchedulesAutoDestroyOnNewInstancesLikeJavaGetNextAvailableInstance|FullyQualifiedName~InstanceEmptyInstanceCheckerServiceTests.Schedule_StoresCancellableFixedRateTaskLikeJavaGetNextAvailableInstance" --no-restore
```

Result: passed 2, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

- Focused Java/Maven command: skipped. No targeted Java unit fixture exists for `InstanceService.getNextAvailableInstance(..., autoDestroy)`; Java source review was used as source-of-truth evidence.
- Broad-validation trigger: none beyond the already-focused scheduler/instance creation surface.
- Broad .NET decision: skipped full project/solution validation because the filtered command compiled the affected project and directly covered the changed helper contract.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.instance.InstanceService.getNextAvailableInstance(..., autoDestroy)` | `Aion.GameServer.Services.InstanceRuntimeService.GetNextAvailableInstance(...)` | Service Plan | Partial | Unit Tested | Partial Parity | C# creation helpers now expose Java auto-destroy scheduling semantics and callback ordering. Runtime callers still need to supply the scheduler service callback. |
| `com.aionemu.gameserver.services.instance.InstanceService.getNextAvailableInstance(worldId, Player)` | `Aion.GameServer.Services.InstanceRuntimeService.GetNextAvailableInstanceForPlayer(...)` | Service Plan | Partial | Unit Tested | Partial Parity | C# schedules before player registration, matching Java overload order. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `InstanceRuntimeService_SchedulesAutoDestroyOnNewInstancesLikeJavaGetNextAvailableInstance` | Unit | Java source review | New instance creation requests scheduler callback by default, suppresses it when `autoDestroy=false`, and schedules before player registration. | Focused C# test plus Java source review. | Does not invoke the real timer; covered separately by checker service test. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 1
- Total artifacts ported or extended in this UOW: 2
- Total artifacts with verified parity: 0
- Total artifacts needing verification or remaining partial: 2
- Total blocked artifacts: 0
- Estimated overall migration completion: Phase 6 remains in progress; overall completion unchanged conservatively.

## Remaining Gaps

- Runtime callers need to pass the real empty-instance scheduler callback.
- Runtime registered-team disbanded lookup for checker decisions.
- Live forced-exit packet send and teleport dispatch.
- Dynamic handler/auto-group destroy call sites.

## Commit

Commit message:

```text
[Phase 6][UOW-2347] Schedule empty checker on instance creation
```
