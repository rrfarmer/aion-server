# Phase 6 Session 2350 Completion - Instance Leave Reset Messages

## Scope

Ported Java `InstanceService.onLeaveInstance` reset-warning message selection into C# planner logic and added the missing `SM_SYSTEM_MESSAGE` helpers.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`

Java behavior used:

- `onLeaveInstance` sends no reset-warning packet unless `instance.getRegisteredCount() > 0`.
- Solo instances send `STR_MSG_LEAVE_INSTANCE(getDestroyDelaySeconds(instance) / 60)`.
- Registered team instances with no team members send `STR_MSG_LEAVE_INSTANCE_PARTY(0)`.
- Group instances with `playersInside.size() <= 1` send `STR_MSG_LEAVE_INSTANCE_PARTY(getDestroyDelaySeconds(instance) / 60)`.
- Java message ids are `1400044` for `STR_MSG_LEAVE_INSTANCE`, `1400045` for `STR_MSG_LEAVE_INSTANCE_PARTY`, and `1400046` for forced leave.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

- Added `SmSystemMessage.LeaveInstance(int)` for Java message id `1400044`.
- Added `SmSystemMessage.LeaveInstanceParty(int)` for Java message id `1400045`.
- Added `InstanceLeaveMessageService.CreateLeaveMessagePlan(...)` with Java branch ordering.
- Added focused planner tests for no registration, solo, empty registered team, last/only player inside, and players-remain cases.
- Extended packet serialization assertions for leave-instance message ids and parameters.

Known limitations:

- This UOW ports the message planner and packet helpers; live teleport/leave callback invocation remains a separate wiring task.
- Registered-team empty-member status is supplied as an input, because C# team runtime lookup was added separately in UOW-2349.

## Validation Decision

- Changed surface: one planner service plus system-message packet helper methods.
- Specific behavior/contract: Java `InstanceService.onLeaveInstance` reset-warning branch order and `SM_SYSTEM_MESSAGE` ids/parameters for leave-instance messages.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~InstanceLeaveMessageServiceTests|FullyQualifiedName~GamePacketTests" --no-restore
```

Result: passed 273, failed 0, skipped 0. The `GamePacketTests` class is broad but directly contains the shared `AssertSystemMessage` packet-shape coverage and ran quickly. Pre-existing nullable/analyzer warnings remain.

- Documentation hygiene:

```powershell
git diff --check
```

Result: passed. Git reported line-ending normalization warnings only.

- Focused Java/Maven command: skipped. No targeted Java unit fixture exists for `InstanceService.onLeaveInstance`; Java source and `SM_SYSTEM_MESSAGE` source review were used as source-of-truth evidence.
- Broad-validation trigger: none. The change is isolated to a planner and packet helpers.
- Broad .NET decision: skipped full project/solution validation because the filtered command compiled the affected project and covered the planner/packet contracts.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.instance.InstanceService.onLeaveInstance(Player)` | `Aion.GameServer.Services.InstanceLeaveMessageService.CreateLeaveMessagePlan(...)` | Service Planner | Partial | Unit Tested | Partial Parity | Reset-warning message branch order is covered. Live invocation during teleport/leave remains unwired. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_MSG_LEAVE_INSTANCE(int)` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage.LeaveInstance(int)` | Packet Helper | Complete | Unit Tested | Verified Parity | Message id `1400044` and invariant integer parameter serialization covered by `GamePacketTests`. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_MSG_LEAVE_INSTANCE_PARTY(int)` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage.LeaveInstanceParty(int)` | Packet Helper | Complete | Unit Tested | Verified Parity | Message id `1400045` and invariant integer parameter serialization covered by `GamePacketTests`. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `InstanceLeaveMessageServiceTests.CreateLeaveMessagePlan_MatchesJavaOnLeaveInstanceBranchOrder` | Unit | Java source review | Reset-warning branch order and minute values for solo, team-empty, last-player, no-registration, and players-remain cases. | Focused C# test plus Java source review. | Does not invoke live teleport/leave callbacks. |
| `GamePacketTests` system-message assertions | Unit | Java `SM_SYSTEM_MESSAGE` source review | Leave-instance message ids and parameter serialization. | Packet serialization test plus Java source review. | Class-level filter includes unrelated packet assertions. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 2
- Total artifacts ported or extended in this UOW: 3
- Total artifacts with verified parity: 2
- Total artifacts needing verification or remaining partial: 1
- Total blocked artifacts: 0
- Estimated overall migration completion: Phase 6 remains in progress; overall completion unchanged conservatively.

## Remaining Gaps

- Live teleport/leave callback wiring for `InstanceService.onLeaveInstance`.
- Other runtime instance creation paths need real scheduler callback review.
- Live forced-exit packet send and teleport dispatch.
- Dynamic handler/auto-group destroy call sites.

## Commit

Commit message:

```text
[Phase 6][UOW-2350] Port instance leave reset messages
```
