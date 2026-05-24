# Phase 6EP Completion Handoff

Created: May 23, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6EO and covers Session 634.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerStatusInfo|PlayerAllianceRuntime|PlayerAllianceMemberInfo|BaseLeavePlanner"`
  - Result: Passed, 86 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1100 tests.

## Recent Work Completed

### Session 634 - Group Ban Offline Banned-Member Coverage

- Source-read Java `PlayerGroupService.banPlayer`, `PlayerGroupLeavedEvent`, group disband replay behavior, base `PlayerLeavedEvent`, and `PacketSendUtility.sendPacket`.
- Added focused parsed-command coverage for `GROUP_BAN_MEMBER` when an online leader bans an offline second member in a two-member non-auto group.
- Reused the test registry unavailable-recipient simulation to model Java `PacketSendUtility.sendPacket` skipping offline recipients.
- Verified the existing C# group ban/disband composition clears both memberships and removes the runtime group while delivering only the remaining online leader's group leave fanout and disband replay; the offline banned player does not receive `STR_PARTY_YOU_ARE_BANISHED` or base `SM_LEAVE_GROUP_MEMBER`.
- No production code changed in this unit; this is regression coverage for an already-modeled source-derived path.
- Commit: this handoff is included in `Cover group ban offline disband`.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_PLAYER_STATUS_INFO` | `Aion.GameServer.Network.Aion.ClientPackets.CmPlayerStatusInfo` / `Aion.GameServer.Network.Aion.GameServerConnection.HandlePlayerStatusInfoAsync` | Client Packet / Handler Boundary | Partial | Regression Tested | Needs Verification | Command id `2` has explicit two-member group ban/disband coverage with an offline banned member. Valid league commands and full generic dispatch remain incomplete. |
| `com.aionemu.gameserver.model.team.common.events.TeamCommand.GROUP_BAN_MEMBER` | Command code `2` branch in `GameServerConnection.HandlePlayerStatusInfoAsync` | Enum / Command Mapping | Partial | Regression Tested | Needs Verification | Test covers leader banning an offline two-member-group peer. Java invalid-member exception policy remains deferred. |
| `com.aionemu.gameserver.model.team.group.PlayerGroupService.banPlayer` | `GameServerConnection.HandleGroupBanMemberAsync` plus `PlayerGroupRuntime.RemoveMemberWithLeavePlan` | Service / Runtime Bridge | Partial | Regression Tested | Needs Verification | Existing C# flow removes the offline banned member, disbands the remaining online leader, and clears runtime state. Java static group registry behavior is not runtime-compared. |
| `com.aionemu.gameserver.model.team.group.events.PlayerGroupLeavedEvent` | `PlayerGroupRuntime.RemoveMemberWithLeavePlan` / connection group ban sends | Event Runtime/Socket Bridge | Partial | Regression Tested | Needs Verification | BAN fanout and DISBAND replay are covered when the banned player is offline. EventService, registered-team instance kick, and live socket ordering remain missing. |
| `com.aionemu.gameserver.model.team.group.events.GroupDisbandEvent` | `PlayerGroupRuntime.AppendGroupDisbandPacketIntents` | Event Runtime/Socket Bridge | Partial | Regression Tested | Needs Verification | Disband replay is source-modeled for the remaining leader after banning an offline member. Java event queue/lock behavior is not compared. |
| `com.aionemu.gameserver.model.team.common.events.PlayerLeavedEvent` | `Aion.GameServer.Services.PlayerBaseLeavePlanner` | Base Event Dependency | Partial | Regression Tested | Needs Verification | Offline banned player receives no base `SM_LEAVE_GROUP_MEMBER`; online remaining leader receives disband replay base leave. Registered-team instance message/kick and EventService callback remain deferred. |
| `com.aionemu.gameserver.model.team.GeneralTeam.removeMember` | `PlayerGroupRuntime.RemoveMemberWithLeavePlan` | Base Team State | Partial | Regression Tested | Needs Verification | Runtime disband cleanup clears both memberships and removes group dictionaries. Java object-wrapper mutation and synchronization semantics are not compared. |
| `com.aionemu.gameserver.model.team.GeneralTeam.shouldDisband` | `PlayerGroupRuntime.RemoveMemberWithLeavePlan` disband predicate | Base Team State | Partial | Regression Tested | Needs Verification | Covers non-auto group plus one remaining member after banning the offline member. Runtime/threading comparison remains unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_GROUP_MEMBER_INFO` | `Aion.GameServer.Network.Aion.ServerPackets.SmGroupMemberInfo` through group ban sends | Server Packet | Partial | Regression Tested | Needs Verification | Delivered to the remaining online leader. Java golden bytes and live client validation remain missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_LEAVE_GROUP_MEMBER` | `Aion.GameServer.Network.Aion.ServerPackets.SmLeaveGroupMember` through group ban/disband sends | Server Packet | Partial | Regression Tested | Needs Verification | Delivered only to the remaining online leader during disband replay; skipped for the offline banned player. Java registered-team instance follow-up remains deferred. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` group ban/disband factories | Server Packet Factory | Partial | Regression Tested | Needs Verification | `STR_PARTY_HE_IS_BANISHED` and `STR_PARTY_IS_DISPERSED` are delivered to the remaining leader; `STR_PARTY_YOU_ARE_BANISHED` is skipped for the offline banned player. Golden frame/client validation remains missing. |
| `com.aionemu.gameserver.utils.PacketSendUtility` | `IGameClientConnectionRegistry.SendPacketToPlayerAsync` / test registry unavailable-recipient simulation | Runtime Dependency | Partial | Regression Tested | Needs Verification | Test registry models Java offline send skipping. Production behavior still depends on connection presence and needs live validation. |

## Tests Added

- `GameServerConnectionPlayerStatusInfoTests.HandlePlayerStatusInfoAsync_GroupBanOfflineMemberSkipsBannedPlayerPacketsLikeJava`
  - Validates parsed command id `2` on a two-member non-auto group where the online leader bans an offline member, the group disbands, both memberships clear, the leader receives ban/disband fanout, and the offline banned player receives no packets.
  - This test is source-derived. It does not compare against Java runtime execution, Java-generated golden vectors, packet captures, encrypted frames, or a live client.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 14
- Total artifacts ported or partially modeled in this handoff window: 0 new production artifacts; 12 artifacts gained regression coverage in this path.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 12
- Total blocked/not-started artifacts: production offline-recipient validation, `EventService` callback, instance kick scheduling, Java static group registry, Java event queue/lock comparison, encoded opcode/frame golden validation, packet capture comparison, and real-client validation.
- Estimated overall migration completion: 64%

The percentage stays conservative. Group ban/disband offline behavior is better covered, but production/live validation gaps remain.

## Remaining Risks

- Production offline-recipient behavior still depends on live connection lookup rather than direct `Player.IsOnline` checks.
- Java `EventService.onLeftTeam`, registered-team instance kick scheduling, and `STR_MSG_LEAVE_INSTANCE_NOT_PARTY` remain deferred.
- Java invalid target/member exception policy and static group registry behavior remain approximated by runtime snapshots.
- Java event queue, lock, object iteration order, offline-recipient handling, and threading behavior remain source-derived only.
- Valid league commands remain incomplete in `CM_PLAYER_STATUS_INFO`.
- Java golden byte vectors, encrypted opcode/frame validation, packet capture comparison, and real-client validation remain unavailable.

## Next Recommended Unit of Work

Continue `CM_PLAYER_STATUS_INFO` parity in this order:

1. Continue the offline-recipient audit for alliance ban/leave paths.
2. Or begin source-reading league command prerequisites if moving into deferred valid `CM_PLAYER_STATUS_INFO` commands.
3. Keep league behavior out of production until a minimal league runtime bridge exists.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6EO-Completion.md`
   - this handoff
3. Inspect Java source for the selected branch before touching C#.
4. Implement one narrow unit with Java breadcrumbs.
5. Add focused tests that state what is source-derived and what remains unverified.
6. Run focused tests, then full GameServer tests.
7. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
8. Commit the unit.
