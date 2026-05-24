# Phase 6EE Completion Handoff

Created: May 23, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6ED and covers Session 623.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerStatusInfo|PlayerGroupRuntime|BaseLeavePlanner"`
  - Result: Passed, 58 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1088 tests.

## Recent Work Completed

### Session 623 - Group Ban Status Command

- Source-read Java `TeamCommand.GROUP_BAN_MEMBER`, `PlayerTeamCommandService.executeCommand`, `PlayerGroupService.banPlayer`, `PlayerGroupLeavedEvent`, base `PlayerLeavedEvent`, and relevant `SM_SYSTEM_MESSAGE` factories.
- Added C# system-message factories for Java ids `1300166`, `1301009`, `1400705`, and `1400749`.
- Wired parsed `CM_PLAYER_STATUS_INFO` command id `2`:
  - self-ban sends `STR_PARTY_CANT_BAN_SELF`;
  - non-leader ban sends `STR_FORCE_ONLY_LEADER_CAN_BANISH`;
  - auto-group ban sends `STR_MSG_PARTY_FORCE_NO_RIGHT_TO_DECIDE`;
  - success reuses `RemoveMemberWithLeavePlan` with `PlayerGroupLeaveReason.Ban`;
  - success sends `STR_PARTY_YOU_ARE_BANISHED` to the banned online player before base `SM_LEAVE_GROUP_MEMBER`.
- Added focused regression coverage for failure branches and a successful three-member ban fanout.
- Commit: this handoff is included in `Wire group ban status command`.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_PLAYER_STATUS_INFO` | `Aion.GameServer.Network.Aion.ClientPackets.CmPlayerStatusInfo` / `Aion.GameServer.Network.Aion.GameServerConnection.HandlePlayerStatusInfoAsync` | Client Packet / Handler Boundary | Partial | Regression Tested | Needs Verification | Command id `2` covers source-derived group ban failure messages and a three-member success fanout. Alliance leave/ban, league commands, and full generic dispatch remain missing. |
| `com.aionemu.gameserver.model.team.common.events.TeamCommand.GROUP_BAN_MEMBER` | Command code `2` branch in `GameServerConnection.HandlePlayerStatusInfoAsync` | Enum / Command Mapping | Partial | Regression Tested | Needs Verification | C# targets `selectedObjectId` or caller when zero, performs Java ban gate checks, and dispatches success through group leave planning. Java invalid-member exception behavior remains deferred. |
| `com.aionemu.gameserver.model.team.group.PlayerGroupService.banPlayer` | `GameServerConnection.HandleGroupBanMemberAsync` plus `PlayerGroupRuntime.RemoveMemberWithLeavePlan` | Service / Runtime Bridge | Partial | Regression Tested | Needs Verification | Self, non-leader, auto-group, and success paths are modeled. Java warning log when target is not in group is not emitted because C# currently no-ops missing targets. |
| `com.aionemu.gameserver.model.team.group.events.PlayerGroupLeavedEvent` | `PlayerGroupRuntime.RemoveMemberWithLeavePlan` / connection group ban sends | Event Runtime/Socket Bridge | Partial | Regression Tested | Needs Verification | BAN reason fanout sends `SM_GROUP_MEMBER_INFO(LEAVE)` plus `STR_PARTY_HE_IS_BANISHED`, then banned-player `STR_PARTY_YOU_ARE_BANISHED`, then base leave packet. Mentor-stop, EventService, instance kick, and live socket ordering remain missing. |
| `com.aionemu.gameserver.model.team.common.events.PlayerLeavedEvent` | `Aion.GameServer.Services.PlayerBaseLeavePlanner` through group ban handler | Base Event Dependency | Partial | Regression Tested | Needs Verification | Banned online player receives base `SM_LEAVE_GROUP_MEMBER` after the banished self-message. Registered-team instance message/kick and EventService callback remain deferred. |
| `com.aionemu.gameserver.model.team.TeamType.AUTO_GROUP` | `Aion.GameServer.Model.GameObjects.PlayerGroupType.AutoGroup` | Enum / Team Type | Partial | Regression Tested | Needs Verification | Auto-group ban denial uses runtime descriptor `TeamType`. Other Java auto-group systems and matching behavior remain outside this unit. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_GROUP_MEMBER_INFO` | `Aion.GameServer.Network.Aion.ServerPackets.SmGroupMemberInfo` through group ban sends | Server Packet | Partial | Regression Tested | Needs Verification | Sent to remaining group members for the banned player. Java golden bytes, encrypted frames, and real-client validation remain missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_LEAVE_GROUP_MEMBER` | `Aion.GameServer.Network.Aion.ServerPackets.SmLeaveGroupMember` through group ban sends | Server Packet | Partial | Regression Tested | Needs Verification | Sent to the banned online player after `STR_PARTY_YOU_ARE_BANISHED`. Java registered-team instance follow-up remains deferred. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` group-ban factories | Server Packet Factory | Partial | Regression Tested | Needs Verification | Covered Java ids `1400705`, `1301009`, `1400749`, `1300177`, and `1300166`. Golden frame/client validation remains missing. |
| `com.aionemu.gameserver.utils.PacketSendUtility` | `IGameClientConnectionRegistry.SendPacketToPlayerAsync` / direct fallback | Runtime Dependency | Partial | Regression Tested | Needs Verification | Ordered registry sends are covered for group-ban failures and success. Java offline-recipient behavior and live socket ordering remain unverified. |
| `com.aionemu.gameserver.services.event.EventService` | Deferred `PlayerGroupLeavePlan.WouldInvokeEventServiceOnLeftTeam` metadata | Service Dependency | Not Started | No Tests | Unknown | Java calls `EventService.onLeftTeam` after ban leave. C# records the base leave boundary but has no live EventService bridge here. |
| `com.aionemu.gameserver.services.instance.InstanceService` | Deferred `PlayerBaseLeavePlanner` instance-kick metadata | Service Dependency | Not Started | No Tests | Unknown | Java schedules an instance exit movement for registered-team instances. C# does not execute that delayed side effect for banned players yet. |

## Tests Added

- `GameServerConnectionPlayerStatusInfoTests.HandlePlayerStatusInfoAsync_GroupBanFailureBranchesSendJavaMessages`
  - Validates parsed command id `2` sends Java message ids `1400705`, `1301009`, and `1400749` for self-ban, non-leader ban, and auto-group ban attempts without mutating group membership.
- `GameServerConnectionPlayerStatusInfoTests.HandlePlayerStatusInfoAsync_GroupBanMemberSendsBanFanoutAndBanishedMessageLikeJava`
  - Validates parsed command id `2` removes the banned member, leaves remaining runtime members, sends `SmGroupMemberInfo` plus Java id `1300177` to remaining members, then sends Java id `1300166` and `SmLeaveGroupMember` to the banned player.
- These tests are source-derived. They do not compare against Java runtime execution, Java-generated golden vectors, packet captures, encrypted frames, or a live client.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 20
- Total artifacts ported or partially modeled in this handoff window: 17
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 17
- Total blocked/not-started artifacts: Java invalid-member exception/logging policy, two-member ban disband regression, mentor-stop side effect, `EventService` callback, instance kick scheduling, alliance leave/ban, league commands, Java static service registry, Java event queue/lock comparison, Java runtime/threading comparison, encoded opcode/frame golden validation, packet capture comparison, and real-client validation.
- Estimated overall migration completion: 64%

The percentage stays conservative. Group command coverage improved with `GROUP_BAN_MEMBER`, but team-command parity still has important branches and validation gaps.

## Remaining Risks

- Java invalid target member lookup can throw through `Objects.requireNonNull`; C# still no-ops missing targets.
- Java warning log for attempting to ban a player outside the group is not modeled.
- Java two-member ban disband cascade is only indirectly covered by the existing ban planner and group-remove disband work; it still needs a focused packet-order regression.
- Java mentor stop side effects when a banned mentor leaves are flagged but not wired.
- Java `EventService.onLeftTeam`, registered-team instance kick scheduling, and `STR_MSG_LEAVE_INSTANCE_NOT_PARTY` remain deferred.
- Java static group registry lookup is approximated by runtime snapshots attached to players.
- Java event queue, lock, group stats recalculation, object iteration order, offline-recipient handling, and threading behavior remain source-derived only.
- Alliance leave/ban and league commands remain incomplete in `CM_PLAYER_STATUS_INFO`.
- Java golden byte vectors, encrypted opcode/frame validation, packet capture comparison, and real-client validation remain unavailable.

## Next Recommended Unit of Work

Continue `CM_PLAYER_STATUS_INFO` parity in this order:

1. Add a focused two-member `GROUP_BAN_MEMBER` disband regression to prove the combined BAN plus disband order.
2. If that test exposes an ordering gap, patch only that path and update the parity table.
3. Then move laterally to `ALLIANCE_LEAVE` or `ALLIANCE_BAN_MEMBER` only if existing alliance planners can cover disband/leader side effects accurately.
4. Keep league commands deferred until a live league runtime bridge exists.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6ED-Completion.md`
   - this handoff
3. Inspect Java source for the selected branch before touching C#.
4. Implement one narrow unit with Java breadcrumbs.
5. Add focused tests that state what is source-derived and what remains unverified.
6. Run focused tests, then full GameServer tests.
7. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
8. Commit the unit.
