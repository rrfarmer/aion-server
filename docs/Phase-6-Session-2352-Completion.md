# Phase 6 Session 2352 Completion - Instance Leave Handler Hook

## Scope

Ported the Java `InstanceService.onLeaveInstance` instance-handler callback into the C# delayed teleport leave path.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`
- `game-server/src/com/aionemu/gameserver/instance/handlers/InstanceHandler.java`
- `game-server/src/com/aionemu/gameserver/instance/handlers/GeneralInstanceHandler.java`
- `game-server/src/com/aionemu/gameserver/services/teleport/TeleportService.java`

Java behavior used:

- `InstanceService.onLeaveInstance(Player)` first calls `instance.getInstanceHandler().onLeaveInstance(player)`.
- Reset-warning packet selection happens after the instance-handler callback.
- Delayed teleport `SpawnTask.run` invokes `InstanceService.onLeaveInstance(player)` before `World.setPosition(...)`, so handlers observe the old player position.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

- Added `IInstanceLifecycleHandler.OnLeaveInstance(Player)` with a no-op default.
- Added `GeneralInstanceLifecycleHandler.OnLeaveInstance(Player)` as the Java `GeneralInstanceHandler.onLeaveInstance` no-op equivalent.
- Updated delayed teleport leave handling to call `instance.InstanceHandler.OnLeaveInstance(player)` before reset-warning message planning.
- Extended the focused delayed-teleport boundary test to verify the handler receives the player before position mutation.

Known limitations:

- This UOW covers the handler callback only for the C# delayed teleport leave path.
- Java `AutoGroupService.onLeaveInstance(player)` remains unwired.
- Map leave callback, pet position update, and other `SpawnTask.run` side effects remain separate work.

## Validation Decision

- Changed surface: instance lifecycle interface plus live delayed teleport connection dispatch.
- Specific behavior/contract: Java `InstanceService.onLeaveInstance` invokes the instance handler before reset-warning message selection and before delayed teleport position mutation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionInstanceCooldownTests.HandleTeleportAnimationDoneAsync_SendsLeaveInstanceResetWarningBeforeSpawnLikeJavaSpawnTask|FullyQualifiedName~InstanceLeaveMessageServiceTests" --no-restore
```

Result: passed 2, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

- Documentation hygiene:

```powershell
git diff --check
```

Result: passed. Git reported line-ending normalization warnings only.

- Focused Java/Maven command: skipped. No targeted Java unit fixture exists for `InstanceService.onLeaveInstance` handler ordering; Java source review was used as source-of-truth evidence.
- Broad-validation trigger: live side-effect and connection dispatch wiring.
- Broad .NET decision: skipped full project/solution validation because the focused dispatch test compiled the affected project/dependencies and directly covered handler-before-position-mutation behavior. No packet primitive, scheduler primitive, persistence layer, serializer, or broad shared state contract changed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.instance.handlers.InstanceHandler.onLeaveInstance(Player)` | `Aion.GameServer.World.IInstanceLifecycleHandler.OnLeaveInstance(Player)` | Interface Hook | Partial | Regression Tested | Partial Parity | Hook is represented and covered on delayed teleport leave; broader handler runtime and implementations remain incomplete. |
| `com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler.onLeaveInstance(Player)` | `Aion.GameServer.World.GeneralInstanceLifecycleHandler.OnLeaveInstance(Player)` | Handler | Complete | Regression Tested | Verified Parity | Java and C# default handlers are no-ops; boundary test validates the hook can be invoked without side effects. |
| `com.aionemu.gameserver.services.instance.InstanceService.onLeaveInstance(Player)` | `Aion.GameServer.Network.Aion.GameServerConnection.SendInstanceLeaveMessageIfNeededAsync(...)` plus `InstanceLeaveMessageService.CreateLeaveMessagePlan(...)` | Runtime Wiring / Planner | Partial | Unit Tested / Regression Tested | Partial Parity | Handler callback and reset-warning packet selection are wired for delayed teleport. Java autogroup callback remains unwired. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `GameServerConnectionInstanceCooldownTests.HandleTeleportAnimationDoneAsync_SendsLeaveInstanceResetWarningBeforeSpawnLikeJavaSpawnTask` | Regression | Java source review | Handler `OnLeaveInstance` is invoked before player position mutation, and reset-warning packet order remains before channel/spawn packets. | Focused C# boundary test plus Java `InstanceService.onLeaveInstance` and `SpawnTask.run` review. | Does not cover autogroup leave callback. |
| `InstanceLeaveMessageServiceTests.CreateLeaveMessagePlan_MatchesJavaOnLeaveInstanceBranchOrder` | Unit | Java source review | Reset-warning packet branch order and minute parameter values remain intact. | Existing focused planner test plus Java source review. | Planner only. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 3
- Total artifacts ported or extended in this UOW: 3
- Total artifacts with verified parity: 1
- Total artifacts needing verification or remaining partial: 2
- Total blocked artifacts: 0
- Estimated overall migration completion: Phase 6 remains in progress; overall completion unchanged conservatively.

## Remaining Gaps

- Java `AutoGroupService.onLeaveInstance(player)` callback.
- Java map leave callback parity: `ConquerorAndProtectorService.getInstance().onLeaveMap(player)`.
- Pet position update and same-map spawn behavior in `TeleportService.SpawnTask.run`.
- Other runtime instance leave/creation paths outside delayed teleport.
- Live forced-exit packet send and teleport mutation.

## Commit

Commit message:

```text
[Phase 6][UOW-2352] Invoke instance leave handler on teleport
```
