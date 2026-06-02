# Phase 6 Session 2356 Completion - Invoke AutoGroup Instance Destroy Workflow

## Scope

Wired Java `AutoGroupService.destroyIfPossible` destroy decisions from the C# autogroup leave runtime into the existing C# `InstanceDestroyWorkflowService`.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`

Java behavior used:

- `AutoGroupService.destroyIfPossible(autoInstance)` removes the `autoInstances` map entry before invoking `InstanceService.destroyInstance(instance)`.
- Destroy happens only when the autogroup registered-player map is empty and no players inside are online.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

- Added an optional destroy callback to `AutoGroupInstanceLeaveRuntimeService`.
- When the autogroup leave plan says Java would destroy the instance, C# now removes the autogroup registry entry, then invokes the destroy callback outside the autogroup lock.
- Production DI now passes `InstanceDestroyWorkflowService.DestroyInstance` into the shared autogroup leave runtime.
- Extended runtime tests to prove the registry is removed before the destroy callback runs.

Known limitations:

- Forced-exit packet fanout remains limited to the existing `InstanceDestroyWorkflowService` planning behavior.
- Java quick-entry refill remains unwired.
- Java periodic registration refresh packet sends remain unwired.
- This UOW does not add new live packet fanout beyond invoking the existing destroy workflow.

## Validation Decision

- Changed surface: live side-effect enabling through the autogroup runtime and production DI.
- Specific behavior/contract: Java autogroup destroy removes the autogroup registry entry before calling instance destruction.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupInstanceLeaveRuntimeServiceTests|FullyQualifiedName~GameServerConnectionInstanceCooldownTests.HandleTeleportAnimationDoneAsync_InvokesAutoGroupLeaveAfterResetWarningLikeJavaInstanceService" --no-restore
```

Result: passed 6, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

- Focused Java/Maven command: skipped. No targeted Java unit fixture exists for `AutoGroupService.destroyIfPossible`; Java source review was used as source-of-truth evidence.
- Broad-validation trigger: live side-effect enabling.
- Broad .NET decision: skipped full project/solution validation after focused runtime/connection tests passed and compiled the affected project/dependencies.
- Why this scope is sufficient: the edited runtime test proves the Java remove-before-destroy order, and the adjacent connection test proves the live delayed teleport path still consumes the autogroup runtime.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.AutoGroupService.destroyIfPossible(AutoInstance)` | `Aion.GameServer.Services.AutoGroupInstanceLeaveRuntimeService.OnLeaveInstance(...)` plus destroy callback | Live Runtime Adapter | Partial | Unit Tested | Partial Parity | C# now removes autogroup registry state before invoking the existing instance destroy workflow when Java would destroy. Quick-entry refill remains missing. |
| `com.aionemu.gameserver.services.instance.InstanceService.destroyInstance(WorldMapInstance)` | `Aion.GameServer.Services.InstanceDestroyWorkflowService.DestroyInstance(...)` via autogroup callback | Service | Partial | Unit Tested | Partial Parity | Existing destroy workflow is now reachable from autogroup leave. Forced-exit packet sends are still modeled by existing workflow plans rather than fully live fanout here. |
| `com.aionemu.gameserver.services.AutoGroupService.onLeaveInstance(Player)` | `Aion.GameServer.Services.AutoGroupInstanceLeaveRuntimeService.OnLeaveInstance(...)` | Live Runtime Adapter | Partial | Regression Tested | Partial Parity | Registered-player cleanup, group/alliance removal, registry removal, and destroy workflow callback are wired. Registration refresh and quick-entry refill remain gaps. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `AutoGroupInstanceLeaveRuntimeServiceTests.OnLeaveInstance_InvokesDestroyWorkflowAfterRemovingAutoGroupRegistryLikeJavaDestroyIfPossible` | Unit | Java source review | Final registered autogroup player leave removes registry before destroy callback. | Focused runtime test plus Java source review. | Does not prove forced-exit packet fanout. |
| `GameServerConnectionInstanceCooldownTests.HandleTeleportAnimationDoneAsync_InvokesAutoGroupLeaveAfterResetWarningLikeJavaInstanceService` | Regression | Java source review | Live delayed teleport leave still consumes autogroup runtime in Java order. | Focused connection test. | Does not cover quick-entry refill or refresh packets. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 2
- Total artifacts ported or extended in this UOW: 3
- Total artifacts with verified parity: 0
- Total artifacts needing verification or remaining partial: 3
- Total blocked artifacts: 0
- Estimated overall migration completion: Phase 6 remains in progress; overall completion unchanged conservatively.

## Remaining Gaps

- Java periodic registration refresh packet sends after autogroup leave.
- Java quick-entry queue refill after autogroup leave.
- Full forced-exit packet fanout for instance destruction.
- `ConquerorAndProtectorService.onLeaveMap` parity.
- Pet position update and same-map spawn behavior in delayed teleport completion.

## Commit

Commit message:

```text
[Phase 6][UOW-2356] Invoke autogroup destroy workflow
```
