# Phase 6 Session 2351 Completion - Delayed Teleport Leave Warning

## Scope

Wired the Java `TeleportService.SpawnTask.run` leave-instance reset-warning side effect into C# delayed teleport completion.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_TELEPORT_ANIMATION_DONE.java`
- `game-server/src/com/aionemu/gameserver/services/teleport/TeleportService.java`
- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`

Java behavior used:

- `CM_TELEPORT_ANIMATION_DONE.runImpl` removes `TaskId.TELEPORT` and runs the queued `SpawnTask`.
- `TeleportService.SpawnTask.run` reads the player's current world and instance before position mutation.
- If current world or instance differs from the destination, Java calls `InstanceService.onLeaveInstance(player)` before `World.setPosition(...)`.
- `InstanceService.onLeaveInstance` sends the reset-warning system message according to the planner branch order ported in UOW-2350.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

- Added delayed-teleport completion wiring in `GameServerConnection.HandleTeleportAnimationDoneAsync(...)` to send the instance leave reset-warning packet before `PlayerTeleportService.CompletePendingTeleport(...)` mutates `player.Position`.
- Added `SendInstanceLeaveMessageIfNeededAsync(...)` to resolve the previous `WorldMapInstance`, apply `InstanceRegisteredTeamDisbandService`, and send the packet selected by `InstanceLeaveMessageService`.
- Added a focused connection-boundary regression test that verifies the packet order is `SM_TELEPORT_LOC`, leave-warning `SM_SYSTEM_MESSAGE`, `SM_CHANNEL_INFO`, `SM_PLAYER_SPAWN`.
- Extended the test fixture to accept an explicit `GameServerRuntimeContext`.

Known limitations:

- This UOW wires the reset-warning packet side effect only. Java also invokes map leave and instance/autogroup handler callbacks; those remain separate Phase 6 runtime work.
- Same-map/same-instance delayed teleports intentionally do not send the leave-warning packet, matching Java's branch guard.
- The fallback branch for dead players or destroyed destination instances remains unchanged; Java fallback does not call `InstanceService.onLeaveInstance` before respawning the player locally.

## Validation Decision

- Changed surface: live connection dispatch side effect plus one boundary test.
- Specific behavior/contract: Java delayed teleport completion sends the instance leave reset-warning before destination position mutation and spawn packets when the world or instance changes.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionInstanceCooldownTests.HandleTeleportAnimationDoneAsync_SendsLeaveInstanceResetWarningBeforeSpawnLikeJavaSpawnTask|FullyQualifiedName~InstanceLeaveMessageServiceTests" --no-restore
```

Result: passed 2, failed 0, skipped 0. The first run found a test-helper compile issue, which was fixed before the passing run. Pre-existing nullable/analyzer warnings remain.

- Documentation hygiene:

```powershell
git diff --check
```

Result: passed. Git reported line-ending normalization warnings only.

- Focused Java/Maven command: skipped. No targeted Java unit fixture exists for `TeleportService.SpawnTask.run` or `InstanceService.onLeaveInstance`; Java source review was used as source-of-truth evidence.
- Broad-validation trigger: live side-effect and connection dispatch wiring.
- Broad .NET decision: skipped full project/solution validation after the focused dispatch test passed and compiled the affected project/dependencies. The scoped risk is isolated to delayed teleport completion packet ordering; no packet primitive, serializer, scheduler primitive, persistence layer, or shared model contract changed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_TELEPORT_ANIMATION_DONE.runImpl()` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleTeleportAnimationDoneAsync(Player)` | Client Packet Handler | Partial | Regression Tested | Partial Parity | Queued delayed teleport completion now sends the leave reset-warning before C# position mutation and spawn packets. Java task removal and exception behavior are not fully modeled. |
| `com.aionemu.gameserver.services.teleport.TeleportService.SpawnTask.run()` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleTeleportAnimationDoneAsync(Player)` / `Aion.GameServer.Services.PlayerTeleportService.CompletePendingTeleport(Player)` | Teleport Runtime | Partial | Regression Tested | Partial Parity | World/instance-change reset-warning ordering is covered. Java map leave, pet position update, same-map spawn path, and instance/autogroup callbacks remain incomplete. |
| `com.aionemu.gameserver.services.instance.InstanceService.onLeaveInstance(Player)` | `Aion.GameServer.Services.InstanceLeaveMessageService.CreateLeaveMessagePlan(...)` plus `GameServerConnection.SendInstanceLeaveMessageIfNeededAsync(...)` | Service Planner / Runtime Wiring | Partial | Unit Tested / Regression Tested | Partial Parity | Reset-warning branch order is covered and live delayed teleport wiring is covered. Java instance handler and autogroup leave callbacks remain unwired. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `GameServerConnectionInstanceCooldownTests.HandleTeleportAnimationDoneAsync_SendsLeaveInstanceResetWarningBeforeSpawnLikeJavaSpawnTask` | Regression | Java source review | Delayed teleport completion sends `STR_MSG_LEAVE_INSTANCE_PARTY` before channel/spawn packets when leaving a registered group instance. | Focused C# boundary test plus Java `SpawnTask.run` source review. | Does not cover Java handler/autogroup callbacks. |
| `InstanceLeaveMessageServiceTests.CreateLeaveMessagePlan_MatchesJavaOnLeaveInstanceBranchOrder` | Unit | Java source review | Leave-warning packet branch order and minute parameter values. | Existing focused planner test plus Java `InstanceService.onLeaveInstance` source review. | Planner only; runtime callback coverage depends on boundary tests. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 3
- Total artifacts ported or extended in this UOW: 3
- Total artifacts with verified parity: 0
- Total artifacts needing verification or remaining partial: 3
- Total blocked artifacts: 0
- Estimated overall migration completion: Phase 6 remains in progress; overall completion unchanged conservatively.

## Remaining Gaps

- Java map leave callback parity: `ConquerorAndProtectorService.getInstance().onLeaveMap(player)`.
- Java instance handler and autogroup leave callbacks from `InstanceService.onLeaveInstance`.
- Pet position update and same-map spawn behavior in `TeleportService.SpawnTask.run`.
- Live forced-exit packet send and teleport mutation.
- Other runtime instance creation paths need scheduler callback review.

## Commit

Commit message:

```text
[Phase 6][UOW-2351] Send instance leave reset warning on teleport
```
