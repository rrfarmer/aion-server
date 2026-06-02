# Phase 6 Session 2348 Completion - Wire Portal Empty Checker Scheduling

## Scope

Connected the C# portal instance allocation path to the real empty-instance checker scheduler and ported the Java instance destroy-delay config keys needed by that scheduler.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/configs/main/InstanceConfig.java`
- `game-server/src/com/aionemu/gameserver/config/main/instance.properties`
- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`

Java behavior used:

- `gameserver.instance.destroy_delay_seconds` defaults to `600`.
- `gameserver.instance.solo.destroy_delay_seconds` defaults to `600`.
- `InstanceService.getDestroyDelaySeconds(WorldMapInstance)` uses solo delay only when `maxPlayers == 1`; all other instances use normal delay.
- `InstanceService.getNextAvailableInstance(..., autoDestroy)` stores a 60-second fixed-rate `EmptyInstanceCheckerTask` on newly allocated instances.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

- Added Java parity config options:
  - `GameServerInstanceOptions.DestroyDelaySeconds`
  - `GameServerInstanceOptions.SoloDestroyDelaySeconds`
- Loaded both options from the existing Java `.properties` keys with Java default values.
- Threaded `InstanceEmptyInstanceCheckerService` through `GameClientSocketServer` into `GameServerConnection`.
- Updated `QueueAllocatedInstancePortalTransferAsync(...)` to pass a real scheduler callback into `InstanceRuntimeService.CreatePortalTransferInstance(...)`.
- The scheduler callback uses `InstanceServiceFormulaService.CreateDestroyDelayPlan(...)` before scheduling the checker.
- Extended the focused portal allocation test to assert that newly allocated portal instances store a fixed-rate empty-instance task.

Known limitations:

- Runtime registered-team disbanded lookup is still not wired into checker decisions.
- The focused portal test proves scheduling and task storage, while delay selection is covered by the adjacent formula tests rather than by executing the delayed checker timer.
- Other instance creation call sites beyond portal allocation still need review for real scheduler injection.

## Validation Decision

- Changed surface: live portal allocation caller plus Java config loading.
- Specific behavior/contract: portal-allocated instances receive the Java-style fixed-rate empty-instance checker task, and Java destroy-delay config keys/defaults are available to the scheduler callback.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionInstanceCooldownTests.QueueAllocatedInstancePortalTransferAsync_AllocatesRegistersSpawnsAndTransfersLikeJavaInstanceService|FullyQualifiedName~GameServerOptionsTests|FullyQualifiedName~InstanceServiceFormulaServiceTests|FullyQualifiedName~InstanceEmptyInstanceCheckerServiceTests.Schedule_StoresCancellableFixedRateTaskLikeJavaGetNextAvailableInstance" --no-restore
```

Result: passed 12, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

- Documentation hygiene:

```powershell
git diff --check
```

Result: passed. Git reported line-ending normalization warnings only.

- Focused Java/Maven command: skipped. No targeted Java unit fixture exists for `InstanceService.getNextAvailableInstance(...)` or `InstanceConfig`; Java source and `instance.properties` review were used as source-of-truth evidence.
- Broad-validation trigger: live runtime caller and scheduler wiring, but the risk was isolated by the focused portal allocation, config, formula, and checker tests.
- Broad .NET decision: skipped full project/solution validation because the filtered command compiled the affected project and directly covered the changed caller/config contracts.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.configs.main.InstanceConfig.INSTANCE_DESTROY_DELAY_SECONDS` | `Aion.GameServer.Configuration.GameServerInstanceOptions.DestroyDelaySeconds` | Config | Complete | Unit Tested | Verified Parity | Java source and `instance.properties` reviewed; default `600` and `mygs.properties` override behavior covered by `GameServerOptionsTests`. |
| `com.aionemu.gameserver.configs.main.InstanceConfig.SOLO_INSTANCE_DESTROY_DELAY_SECONDS` | `Aion.GameServer.Configuration.GameServerInstanceOptions.SoloDestroyDelaySeconds` | Config | Complete | Unit Tested | Verified Parity | Java source and `instance.properties` reviewed; default `600` and `mygs.properties` override behavior covered by `GameServerOptionsTests`. |
| `com.aionemu.gameserver.services.instance.InstanceService.getDestroyDelaySeconds(WorldMapInstance)` | `Aion.GameServer.Services.InstanceServiceFormulaService.CreateDestroyDelayPlan(...)` | Service Formula | Complete | Unit Tested | Verified Parity | Existing formula tests cover solo `maxPlayers == 1` and normal group delay selection; this UOW wires the formula into the portal scheduler callback. |
| `com.aionemu.gameserver.services.instance.InstanceService.getNextAvailableInstance(..., autoDestroy)` | `Aion.GameServer.Network.Aion.GameServerConnection.QueueAllocatedInstancePortalTransferAsync(...)` | Runtime Caller | Partial | Unit Tested | Partial Parity | Portal allocation now passes the real checker scheduler and stores the fixed-rate task. Registered-team disbanded lookup and other runtime creation call sites still need follow-up. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `GameServerOptionsTests.LoadFromJavaConfig_ReadsCoreAndNetworkDefaults` | Unit | Java `InstanceConfig` and `instance.properties` review | Default destroy-delay config values are `600`. | Focused C# test plus Java source/config review. | Does not execute Java config binder. |
| `GameServerOptionsTests.LoadFromJavaConfig_AppliesMyGsOverridesLast` | Unit | Java config override precedence already modeled by C# loader | `mygs.properties` can override both destroy-delay keys. | Focused C# test with temporary override file. | Does not execute Java config binder. |
| `QueueAllocatedInstancePortalTransferAsync_AllocatesRegistersSpawnsAndTransfersLikeJavaInstanceService` | Unit | Java `InstanceService.getNextAvailableInstance` and `PortalService.port` review | Portal allocation stores a cancellable fixed-rate empty-instance task while preserving allocation/register/spawn/transfer behavior. | Focused C# test plus Java source review. | Does not wait for delayed task execution; checker service tests cover fixed-rate metadata and cancellation. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 3
- Total artifacts ported or extended in this UOW: 4
- Total artifacts with verified parity: 3
- Total artifacts needing verification or remaining partial: 1
- Total blocked artifacts: 0
- Estimated overall migration completion: Phase 6 remains in progress; overall completion unchanged conservatively.

## Remaining Gaps

- Runtime registered-team disbanded lookup for checker decisions.
- Other runtime instance creation paths need real scheduler callback review.
- Live forced-exit packet send and teleport dispatch.
- Dynamic handler/auto-group destroy call sites.

## Commit

Commit message:

```text
[Phase 6][UOW-2348] Wire portal empty checker scheduling
```
