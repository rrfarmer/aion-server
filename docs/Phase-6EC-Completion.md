# Phase 6EC Completion Handoff

Created: May 23, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6EB and covers Sessions 620-621.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerStatusInfo|PlayerGroupRuntime|BaseLeavePlanner"`
  - Result: Passed, 55 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1085 tests.

## Recent Work Completed

### Session 620 - Group Remove Status Command

- Source-read Java `TeamCommand.GROUP_REMOVE_MEMBER`, `PlayerTeamCommandService.executeCommand`, `PlayerGroupService.removePlayer`, `PlayerGroupLeavedEvent`, base `PlayerLeavedEvent`, `GroupDisbandEvent`, `PlayerGroup`, `GeneralTeam.shouldDisband`, and `SM_GROUP_MEMBER_INFO`.
- Added ordered group leave packet intents and `PlayerGroupRuntime.RemoveMemberWithLeavePlan`.
- Added Java group-leave system-message factories for ids `1300167`, `1300168`, `1300176`, and `1300177`.
- Wired `CM_PLAYER_STATUS_INFO` command id `6` for the common member-leave path:
  - clears the removed player's group membership;
  - sends `SM_GROUP_MEMBER_INFO(LEAVE)` plus `STR_PARTY_HE_LEAVE_PARTY` to remaining members;
  - sends base `SM_LEAVE_GROUP_MEMBER` to the removed online player.
- Commit: `0b8014020 Wire group remove status command`

### Session 621 - Group Remove Leader Fallback Coverage

- Added focused regression coverage for removing the current group leader through parsed command id `6`.
- Confirmed the source-derived Java ordering:
  - leave fanout to remaining members;
  - fallback leader `SM_GROUP_INFO` plus leader messages;
  - base `SM_LEAVE_GROUP_MEMBER` to the removed leader.
- No production code changes were needed; the Session 620 implementation already satisfied this focused path.
- Commit: `bca46b384 Cover group remove leader fallback`

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_PLAYER_STATUS_INFO` | `Aion.GameServer.Network.Aion.ClientPackets.CmPlayerStatusInfo` / `Aion.GameServer.Network.Aion.GameServerConnection.HandlePlayerStatusInfoAsync` | Client Packet / Handler Boundary | Partial | Regression Tested | Needs Verification | Command id `6` now covers normal member removal and leader-removal fallback. Group ban, alliance leave/ban, league commands, and full generic dispatch remain missing. |
| `com.aionemu.gameserver.model.team.common.events.TeamCommand.GROUP_REMOVE_MEMBER` | Command code `6` branch in `GameServerConnection.HandlePlayerStatusInfoAsync` | Enum / Command Mapping | Partial | Regression Tested | Needs Verification | C# targets `selectedObjectId` or caller when zero, clears membership, and sends leave packets. Java invalid-member exception behavior and two-member disband cascade remain incomplete. |
| `com.aionemu.gameserver.model.team.common.service.PlayerTeamCommandService` | Narrow group-remove branch in `GameServerConnection.HandlePlayerStatusInfoAsync` | Service Dependency | Partial | Regression Tested | Needs Verification | C# still bypasses full generic team-command dispatch. Group ban, alliance leave/ban, league commands, and Java exception policy remain incomplete. |
| `com.aionemu.gameserver.model.team.group.PlayerGroupService.removePlayer` | `Aion.GameServer.Services.PlayerGroupRuntime.RemoveMemberWithLeavePlan` through connection handler | Service / Runtime Bridge | Partial | Regression Tested | Needs Verification | Runtime removes members and produces ordered leave intents. Java static group registry, `group.onEvent`, and group stats recalculation are not runtime-compared. |
| `com.aionemu.gameserver.model.team.group.events.PlayerGroupLeavedEvent` | `PlayerGroupRuntime.RemoveMemberWithLeavePlan` / connection group remove sends | Event Runtime/Socket Bridge | Partial | Regression Tested | Needs Verification | Common leave and leader-removal fallback paths are covered. Full disband cascade, mentor-stop side effect, EventService, and instance kick remain missing. |
| `com.aionemu.gameserver.model.team.group.events.ChangeGroupLeaderEvent` | `PlayerGroupRuntime.ChangeLeader` / `PlayerGroupLeavePlan.LeaderChangePlan` | Event Runtime/Socket Bridge | Partial | Regression Tested | Needs Verification | Fallback leader selection after leader removal is covered for the next online member. Java event queue/lock and all-offline fallback remain unverified. |
| `com.aionemu.gameserver.model.team.common.events.PlayerLeavedEvent` | `Aion.GameServer.Services.PlayerBaseLeavePlanner` through group remove handler | Base Event Dependency | Partial | Regression Tested | Needs Verification | Base leave packet to online removed players is covered. Registered-team instance comparison, delayed kick, and `EventService.onLeftTeam` remain deferred. |
| `com.aionemu.gameserver.model.team.group.events.GroupDisbandEvent` | `PlayerGroupLeavePlan.WouldDisband` metadata / runtime cleanup boundary | Event Dependency | Partial | No Direct Tests | Needs Verification | C# records/clears disband boundary state, but Java's full two-member disband packet cascade is not implemented in the connection handler. |
| `com.aionemu.gameserver.model.team.GeneralTeam.shouldDisband` | `PlayerGroupRuntime.RemoveMemberWithLeavePlan` disband predicate | Base Team State | Partial | No Direct Tests | Needs Verification | C# source-models non-auto-group disband when one member remains. Packet fanout still needs parity work. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_GROUP_MEMBER_INFO` | `Aion.GameServer.Network.Aion.ServerPackets.SmGroupMemberInfo` through group remove sends | Server Packet | Partial | Regression Tested | Needs Verification | Sent with `PlayerGroupEvent.Leave` for member removal. Java golden bytes, encrypted frames, and real-client validation remain missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_GROUP_INFO` | `Aion.GameServer.Network.Aion.ServerPackets.SmGroupInfo` through fallback leader sends | Server Packet | Partial | Regression Tested | Needs Verification | Covered after removing the current leader. Java golden bytes, encrypted frames, and real-client validation remain missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_LEAVE_GROUP_MEMBER` | `Aion.GameServer.Network.Aion.ServerPackets.SmLeaveGroupMember` through base leave sends | Server Packet | Partial | Regression Tested | Needs Verification | Sent to the removed online player. Java registered-team instance follow-up remains deferred. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` group leave/leader factories | Server Packet Factory | Partial | Regression Tested | Needs Verification | Covered ids include `1300168`, `1300155`, and `1300154`; factories for `1300167`, `1300176`, and `1300177` are present for future leave reasons. Golden frame/client validation remains missing. |
| `com.aionemu.gameserver.services.event.EventService` | Deferred `PlayerGroupLeavePlan.WouldInvokeEventServiceOnLeftTeam` metadata | Service Dependency | Not Started | No Tests | Unknown | Java calls `EventService.onLeftTeam` after base leave. No live C# bridge exists here yet. |
| `com.aionemu.gameserver.services.instance.InstanceService` | Deferred `PlayerBaseLeavePlanner` instance-kick metadata | Service Dependency | Not Started | No Tests | Unknown | Java schedules instance exit movement for registered-team instances. C# records the boundary but does not execute it here. |
| `com.aionemu.gameserver.utils.PacketSendUtility` | `IGameClientConnectionRegistry.SendPacketToPlayerAsync` / direct fallback | Runtime Dependency | Partial | Regression Tested | Needs Verification | Ordered registry sends are covered for common removal and leader fallback. Java offline-recipient behavior and live socket ordering remain unverified. |

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 16
- Total artifacts ported or partially modeled in this handoff window: 14
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 14
- Total blocked/not-started artifacts: two-member disband cascade, mentor-stop side effect, `EventService` callback, instance kick scheduling, group ban, alliance leave/ban, league commands, Java static group registry, Java event queue/lock comparison, group stats recalculation, Java runtime/threading comparison, encoded opcode/frame golden validation, packet capture comparison, and real-client validation.
- Estimated overall migration completion: 64%

The percentage remains conservative. `GROUP_REMOVE_MEMBER` is now useful for common and leader-removal paths, but cascading leave/disband behavior still needs careful parity work.

## Remaining Risks

- Two-member group disband cascade is still incomplete at the packet fanout level.
- Java mentor stop side effects when a mentor leaves are flagged but not wired.
- Java `EventService.onLeftTeam` and registered-team instance kick scheduling remain deferred.
- Java invalid target member lookup can throw through `Objects.requireNonNull`; the C# packet handler currently no-ops missing targets.
- Java static group registry lookup is approximated by runtime snapshots attached to players.
- Java event queue, lock, group stats recalculation, object iteration order, offline-only leader fallback, and threading behavior remain source-derived only.
- Remaining `CM_PLAYER_STATUS_INFO` branches include group ban, alliance leave/ban, and league commands.
- Java golden byte vectors, encrypted opcode/frame validation, packet capture comparison, and real-client validation remain unavailable.

## Next Recommended Unit of Work

Continue `CM_PLAYER_STATUS_INFO` parity in this order:

1. Finish two-member `GROUP_REMOVE_MEMBER` disband cascade packet/order behavior.
2. Then port `GROUP_BAN_MEMBER`, starting with self-ban/no-rights/auto-group failure messages before ban-success leave fanout.
3. Keep alliance leave/ban and league commands deferred unless existing planners can cover their cascading side effects.
4. After the unit, update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics before committing.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6EB-Completion.md`
   - this handoff
3. Inspect Java source for the selected command branch before touching C#.
4. Implement one narrow unit with Java breadcrumbs.
5. Add focused tests that state what is source-derived and what remains unverified.
6. Run focused tests, then full GameServer tests.
7. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
8. Commit the unit.
