# Phase 6 Session 2355 Completion - Wire AutoGroup Leave Runtime

## Scope

Wired the C# delayed teleport leave path into the autogroup leave runtime so the prior planner/runtime work now participates in live instance-leave behavior.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/services/teleport/TeleportService.java`
- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`
- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`

Java order used:

1. `TeleportService.SpawnTask.run` detects world or instance change before `World.setPosition`.
2. `InstanceService.onLeaveInstance(player)` calls the instance handler.
3. It sends the reset-warning packet when registered instance rules require it.
4. If autogroup is enabled, it calls `AutoGroupService.onLeaveInstance(player)`.
5. The teleport then mutates position and sends channel/spawn packets.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

- Added `AutoGroupInstanceLeaveRuntimeService` as a shared dependency of `GameServerConnection`.
- Passed the shared autogroup runtime through `GameClientSocketServer`.
- Registered the shared autogroup runtime in production DI using the shared group/alliance runtimes.
- Updated delayed teleport leave handling to invoke autogroup leave cleanup after reset-warning packet selection and before completing the teleport.
- Added a focused connection test that proves a leaving autogroup player is removed from the Java PvP-style autogroup team during delayed teleport leave.

Known limitations:

- The call now applies registered-player and group/alliance cleanup, but packet fanout from group/alliance leave workflows remains limited to the existing C# runtime behavior.
- Actual Java quick-entry refill and periodic registration refresh packet sends remain planned gaps.
- Actual instance destruction through `InstanceDestroyWorkflowService` is still not invoked from autogroup leave.
- `ConquerorAndProtectorService.onLeaveMap`, pet position update, and same-map spawn parity remain separate gaps.

## Validation Decision

- Changed surface: live connection dispatch wiring plus production DI.
- Specific behavior/contract: Java `InstanceService.onLeaveInstance` order during delayed teleport leave: instance handler, reset-warning decision, autogroup cleanup, then teleport position/spawn completion.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionInstanceCooldownTests.HandleTeleportAnimationDoneAsync_InvokesAutoGroupLeaveAfterResetWarningLikeJavaInstanceService|FullyQualifiedName~GameServerConnectionInstanceCooldownTests.HandleTeleportAnimationDoneAsync_SendsLeaveInstanceResetWarningBeforeSpawnLikeJavaSpawnTask|FullyQualifiedName~AutoGroupInstanceLeaveRuntimeServiceTests|FullyQualifiedName~AutoGroupInstanceLeavePlanServiceTests" --no-restore
```

Result: passed 11, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

- Documentation hygiene:

```powershell
git diff --check
```

Result: passed. Git emitted CRLF working-copy warnings only.

- Focused Java/Maven command: skipped. No targeted Java unit fixture exists for this teleport/autogroup path; Java source review was used as source-of-truth evidence.
- Broad-validation trigger: live connection dispatch wiring.
- Broad .NET decision: skipped full project/solution validation after the focused dispatch/runtime tests passed and compiled the affected project/dependencies. The changed live surface was isolated to the delayed teleport leave branch plus optional dependency composition.
- Why this scope is sufficient: the new test exercises the edited live connection method and verifies packet order plus autogroup team cleanup; the adjacent runtime/planner tests cover the Java autogroup branches consumed by the connection.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.instance.InstanceService.onLeaveInstance(Player)` | `Aion.GameServer.Network.Aion.GameServerConnection.SendInstanceLeaveMessageIfNeededAsync(...)` | Live Adapter | Partial | Regression Tested | Partial Parity | C# now preserves handler, reset-warning packet, and autogroup cleanup order for delayed teleport leave. `ConquerorAndProtectorService.onLeaveMap` and full instance destroy side effects remain missing. |
| `com.aionemu.gameserver.services.AutoGroupService.onLeaveInstance(Player)` | `Aion.GameServer.Services.AutoGroupInstanceLeaveRuntimeService.OnLeaveInstance(...)` via `GameServerConnection` | Live Runtime Adapter | Partial | Regression Tested | Partial Parity | Live delayed teleport leave now invokes registered-player cleanup and PvP group/alliance removal. Quick-entry refill, periodic registration refresh packets, and actual instance destruction remain unwired. |
| `com.aionemu.gameserver.services.teleport.TeleportService.SpawnTask.run` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleTeleportAnimationDoneAsync(Player)` | Teleport Dispatch | Partial | Regression Tested | Partial Parity | Leave-side autogroup cleanup occurs before C# position mutation and spawn packets. Pet position update and same-map spawn branch remain separate gaps. |
| `com.aionemu.gameserver.model.autogroup.AutoPvpInstance.onLeaveInstance(Player)` | `Aion.GameServer.Services.AutoGroupInstanceLeaveRuntimeService` PvP branch via connection | Runtime Branch | Partial | Unit Tested | Partial Parity | Leaving autogroup player is removed from group/alliance runtime when delayed teleport leaves the instance. Packet fanout is not fully modeled here. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `GameServerConnectionInstanceCooldownTests.HandleTeleportAnimationDoneAsync_InvokesAutoGroupLeaveAfterResetWarningLikeJavaInstanceService` | Regression | Java source review | Delayed teleport leave calls autogroup cleanup after reset warning and before spawn packets. | Focused connection test plus Java order review. | Does not prove quick-entry refill or registration refresh packets. |
| `GameServerConnectionInstanceCooldownTests.HandleTeleportAnimationDoneAsync_SendsLeaveInstanceResetWarningBeforeSpawnLikeJavaSpawnTask` | Regression | Java source review | Existing reset-warning order still holds after the autogroup wiring. | Focused connection test. | Does not cover every instance subtype. |
| `AutoGroupInstanceLeaveRuntimeServiceTests` | Unit | Java source review | Runtime unregisters players, removes group/alliance membership, and removes registry when Java would destroy. | Focused runtime tests. | Instance destruction and packet fanout remain gaps. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported or extended in this UOW: 4
- Total artifacts with verified parity: 0
- Total artifacts needing verification or remaining partial: 4
- Total blocked artifacts: 0
- Estimated overall migration completion: Phase 6 remains in progress; overall completion unchanged conservatively.

## Remaining Gaps

- Java periodic registration refresh packet sends after autogroup leave.
- Java quick-entry queue refill after autogroup leave.
- Actual `InstanceDestroyWorkflowService` invocation when autogroup destroy is possible.
- `ConquerorAndProtectorService.onLeaveMap` parity.
- Pet position update and same-map spawn behavior in delayed teleport completion.

## Commit

Commit message:

```text
[Phase 6][UOW-2355] Wire autogroup leave into teleport
```
