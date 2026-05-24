# Phase 6EO Completion Handoff

Created: May 23, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6EN and covers Session 633.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerStatusInfo|PlayerAllianceRuntime|PlayerAllianceMemberInfo|BaseLeavePlanner"`
  - Result: Passed, 85 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1099 tests.

## Recent Work Completed

### Session 633 - Group Remove Offline Remaining Disband Coverage

- Source-read Java `PlayerGroupLeavedEvent`, disband replay behavior, base `PlayerLeavedEvent`, and `PacketSendUtility.sendPacket`.
- Added focused parsed-command coverage for `GROUP_REMOVE_MEMBER` when a two-member non-auto group disbands and the remaining member is offline.
- Reused the test registry unavailable-recipient simulation to model Java `PacketSendUtility.sendPacket` skipping offline recipients.
- Verified the existing C# group remove/disband composition clears both memberships and removes the runtime group while delivering only the online leaver's base `SM_LEAVE_GROUP_MEMBER`; initial leave fanout and disband replay packets for the offline remaining member are not delivered.
- No production code changed in this unit; this is regression coverage for an already-modeled source-derived path.
- Commit: this handoff is included in `Cover group remove offline disband`.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_PLAYER_STATUS_INFO` | `Aion.GameServer.Network.Aion.ClientPackets.CmPlayerStatusInfo` / `Aion.GameServer.Network.Aion.GameServerConnection.HandlePlayerStatusInfoAsync` | Client Packet / Handler Boundary | Partial | Regression Tested | Needs Verification | Command id `6` has explicit two-member group disband coverage with an offline remaining member. Valid league commands and full generic dispatch remain incomplete. |
| `com.aionemu.gameserver.model.team.common.events.TeamCommand.GROUP_REMOVE_MEMBER` | Command code `6` branch in `GameServerConnection.HandlePlayerStatusInfoAsync` | Enum / Command Mapping | Partial | Regression Tested | Needs Verification | Test covers self-removal from a two-member group with offline remaining recipient. Java invalid-member exception policy remains deferred. |
| `com.aionemu.gameserver.model.team.group.PlayerGroupService.removePlayer` | `GameServerConnection.HandleGroupRemoveMemberAsync` plus `PlayerGroupRuntime.RemoveMemberWithLeavePlan` | Service / Runtime Bridge | Partial | Regression Tested | Needs Verification | Existing C# flow removes the online member, disbands the remaining offline member, and clears runtime state. Java static group registry behavior is not runtime-compared. |
| `com.aionemu.gameserver.model.team.group.events.PlayerGroupLeavedEvent` | `PlayerGroupRuntime.RemoveMemberWithLeavePlan` / connection group remove sends | Event Runtime/Socket Bridge | Partial | Regression Tested | Needs Verification | LEAVE fanout and DISBAND replay are covered when the only remaining recipient is offline. EventService, registered-team instance kick, and live socket ordering remain missing. |
| `com.aionemu.gameserver.model.team.group.events.GroupDisbandEvent` | `PlayerGroupRuntime.AppendGroupDisbandPacketIntents` | Event Runtime/Socket Bridge | Partial | Regression Tested | Needs Verification | Disband replay is source-modeled and offline delivery is skipped by registry simulation. Java event queue/lock behavior is not compared. |
| `com.aionemu.gameserver.model.team.common.events.PlayerLeavedEvent` | `Aion.GameServer.Services.PlayerBaseLeavePlanner` | Base Event Dependency | Partial | Regression Tested | Needs Verification | Online leaver receives base `SM_LEAVE_GROUP_MEMBER`; offline remaining member receives no base leave packet. Registered-team instance message/kick and EventService callback remain deferred. |
| `com.aionemu.gameserver.model.team.GeneralTeam.removeMember` | `PlayerGroupRuntime.RemoveMemberWithLeavePlan` | Base Team State | Partial | Regression Tested | Needs Verification | Runtime disband cleanup clears both memberships and removes group dictionaries. Java object-wrapper mutation and synchronization semantics are not compared. |
| `com.aionemu.gameserver.model.team.GeneralTeam.shouldDisband` | `PlayerGroupRuntime.RemoveMemberWithLeavePlan` disband predicate | Base Team State | Partial | Regression Tested | Needs Verification | Covers non-auto group plus one remaining member after removal. Runtime/threading comparison remains unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_GROUP_MEMBER_INFO` | `Aion.GameServer.Network.Aion.ServerPackets.SmGroupMemberInfo` through group remove sends | Server Packet | Partial | Regression Tested | Needs Verification | Planned for the offline remaining member but not delivered by registry simulation. Java golden bytes and live client validation remain missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_LEAVE_GROUP_MEMBER` | `Aion.GameServer.Network.Aion.ServerPackets.SmLeaveGroupMember` through group remove/disband sends | Server Packet | Partial | Regression Tested | Needs Verification | Delivered only to the online leaver in this regression. Java registered-team instance follow-up remains deferred. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` group leave/disband factories | Server Packet Factory | Partial | Regression Tested | Needs Verification | Leave and disband messages are planned for the offline remaining member but not delivered by registry simulation. Golden frame/client validation remains missing. |
| `com.aionemu.gameserver.utils.PacketSendUtility` | `IGameClientConnectionRegistry.SendPacketToPlayerAsync` / test registry unavailable-recipient simulation | Runtime Dependency | Partial | Regression Tested | Needs Verification | Test registry models Java offline send skipping. Production behavior still depends on connection presence and needs live validation. |

## Tests Added

- `GameServerConnectionPlayerStatusInfoTests.HandlePlayerStatusInfoAsync_GroupRemoveTwoMemberGroupSkipsOfflineRemainingDisbandPacketsLikeJava`
  - Validates parsed command id `6` on a two-member non-auto group where the online member leaves, the remaining member is offline, the group disbands, both memberships clear, and only the online leaver receives `SmLeaveGroupMember`.
  - This test is source-derived. It does not compare against Java runtime execution, Java-generated golden vectors, packet captures, encrypted frames, or a live client.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 13
- Total artifacts ported or partially modeled in this handoff window: 0 new production artifacts; 11 artifacts gained regression coverage in this path.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 11
- Total blocked/not-started artifacts: production offline-recipient validation, `EventService` callback, instance kick scheduling, Java static group registry, Java event queue/lock comparison, encoded opcode/frame golden validation, packet capture comparison, and real-client validation.
- Estimated overall migration completion: 64%

The percentage stays conservative. Group remove/disband offline behavior is better covered, but production/live validation gaps remain.

## Remaining Risks

- Production offline-recipient behavior still depends on live connection lookup rather than direct `Player.IsOnline` checks.
- Java `EventService.onLeftTeam`, registered-team instance kick scheduling, and `STR_MSG_LEAVE_INSTANCE_NOT_PARTY` remain deferred.
- Java invalid target/member exception policy and static group registry behavior remain approximated by runtime snapshots.
- Java event queue, lock, object iteration order, offline-recipient handling, and threading behavior remain source-derived only.
- Valid league commands remain incomplete in `CM_PLAYER_STATUS_INFO`.
- Java golden byte vectors, encrypted opcode/frame validation, packet capture comparison, and real-client validation remain unavailable.

## Next Recommended Unit of Work

Continue `CM_PLAYER_STATUS_INFO` parity in this order:

1. Continue the offline-recipient audit for group ban or alliance ban/leave paths.
2. Or begin source-reading league command prerequisites if moving into deferred valid `CM_PLAYER_STATUS_INFO` commands.
3. Keep league behavior out of production until a minimal league runtime bridge exists.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6EN-Completion.md`
   - this handoff
3. Inspect Java source for the selected branch before touching C#.
4. Implement one narrow unit with Java breadcrumbs.
5. Add focused tests that state what is source-derived and what remains unverified.
6. Run focused tests, then full GameServer tests.
7. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
8. Commit the unit.
