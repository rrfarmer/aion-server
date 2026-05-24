# Phase 6EQ Completion Handoff

Created: May 23, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6EP and covers Session 635.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerStatusInfo|PlayerAllianceRuntime|PlayerAllianceMemberInfo|BaseLeavePlanner"`
  - Result: Passed, 87 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1101 tests.

## Recent Work Completed

### Session 635 - Alliance Ban Offline Banned-Member Coverage

- Source-read Java `PlayerAllianceService.banPlayer`, `PlayerAllianceLeavedEvent`, `AllianceDisbandEvent`, base `PlayerLeavedEvent`, and `PacketSendUtility.sendPacket`.
- Added focused parsed-command coverage for `ALLIANCE_BAN_MEMBER` when an online alliance leader bans an offline second member in a two-member non-auto alliance.
- Reused the test registry unavailable-recipient simulation to model Java `PacketSendUtility.sendPacket` skipping offline recipients.
- Verified the existing C# alliance ban/disband composition clears both memberships and removes the runtime alliance while delivering only the remaining online leader's ban fanout and disband replay; the offline banned player does not receive `STR_FORCE_BAN_ME` or base `SM_LEAVE_GROUP_MEMBER`.
- No production code changed in this unit; this is regression coverage for an already-modeled source-derived path.
- Commit: this handoff is included in `Cover alliance ban offline disband`.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_PLAYER_STATUS_INFO` | `Aion.GameServer.Network.Aion.ClientPackets.CmPlayerStatusInfo` / `Aion.GameServer.Network.Aion.GameServerConnection.HandlePlayerStatusInfoAsync` | Client Packet / Handler Boundary | Partial | Regression Tested | Needs Verification | Command id `16` has explicit two-member alliance ban/disband coverage with an offline banned member. Valid league commands and full generic dispatch remain incomplete. |
| `com.aionemu.gameserver.model.team.common.events.TeamCommand.ALLIANCE_BAN_MEMBER` | Command code `16` branch in `GameServerConnection.HandlePlayerStatusInfoAsync` | Enum / Command Mapping | Partial | Regression Tested | Needs Verification | Test covers leader banning an offline two-member-alliance peer. Java invalid-member exception and warning-log policy remain deferred. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.banPlayer` | `GameServerConnection.HandleAllianceBanMemberAsync` plus `PlayerAllianceRuntime.RemoveMemberWithLeaveWorkflow` | Service / Runtime Bridge | Partial | Regression Tested | Needs Verification | Existing C# flow removes the offline banned member, disbands the remaining online leader, and clears runtime state. Java static alliance registry and Vortex cleanup are not runtime-compared. |
| `com.aionemu.gameserver.model.team.alliance.events.PlayerAllianceLeavedEvent` | `PlayerAllianceRuntime.RemoveMemberWithLeaveWorkflow` / connection alliance ban sends | Event Runtime/Socket Bridge | Partial | Regression Tested | Needs Verification | BAN fanout and DISBAND replay are covered when the banned player is offline. League broadcast, EventService, registered-team instance kick, and live socket ordering remain missing. |
| `com.aionemu.gameserver.model.team.alliance.events.AllianceDisbandEvent` | `PlayerAlliancePacketIntentKind.LeaveGroupMember` plus disband intents in `PlayerAllianceLeavedPlanner` | Event Runtime/Socket Bridge | Partial | Regression Tested | Needs Verification | Disband replay is source-modeled for the remaining leader after banning an offline member. Java event queue/lock behavior is not compared. |
| `com.aionemu.gameserver.model.team.common.events.PlayerLeavedEvent` | `Aion.GameServer.Services.PlayerBaseLeavePlanner` and `PlayerAlliancePacketIntentKind.LeaveGroupMember` | Base Event Dependency | Partial | Regression Tested | Needs Verification | Offline banned player receives no base `SM_LEAVE_GROUP_MEMBER`; online remaining leader receives disband replay base leave. Registered-team instance message/kick and EventService callback remain deferred. |
| `com.aionemu.gameserver.model.team.GeneralTeam.removeMember` | `PlayerAllianceRuntime.RemoveMemberWithLeaveWorkflow` | Base Team State | Partial | Regression Tested | Needs Verification | Runtime disband cleanup clears both memberships and removes alliance dictionaries. Java object-wrapper mutation and synchronization semantics are not compared. |
| `com.aionemu.gameserver.model.team.GeneralTeam.shouldDisband` | `PlayerAllianceRuntime.RemoveMemberWithLeaveWorkflow` disband predicate | Base Team State | Partial | Regression Tested | Needs Verification | Covers non-auto alliance plus one remaining member after banning the offline member. Runtime/threading comparison remains unverified. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAlliance` | `Aion.GameServer.Model.GameObjects.PlayerAllianceDescriptor` / `PlayerAllianceRuntime` member store | Team State | Partial | Regression Tested | Needs Verification | Runtime clears both players and removes alliance dictionaries after disband. Java static alliance registry, group internals, and synchronized event semantics are not compared. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ALLIANCE_MEMBER_INFO` | `Aion.GameServer.Network.Aion.ServerPackets.SmAllianceMemberInfo` through alliance ban sends | Server Packet | Partial | Regression Tested | Needs Verification | Delivered to the remaining online leader. Java golden bytes and live client validation remain missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ALLIANCE_INFO` | `Aion.GameServer.Network.Aion.ServerPackets.SmAllianceInfo` through alliance ban sends | Server Packet | Partial | Regression Tested | Needs Verification | Delivered to the remaining online leader before disband replay. Java golden bytes, league rows, and live client validation remain missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_LEAVE_GROUP_MEMBER` | `Aion.GameServer.Network.Aion.ServerPackets.SmLeaveGroupMember` through alliance ban/disband sends | Server Packet | Partial | Regression Tested | Needs Verification | Delivered only to the remaining online leader during disband replay; skipped for the offline banned player. Java registered-team instance follow-up remains deferred. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` alliance ban/disband factories | Server Packet Factory | Partial | Regression Tested | Needs Verification | `STR_FORCE_BAN_HIM` and `STR_PARTY_ALLIANCE_DISPERSED` are delivered to the remaining leader; `STR_FORCE_BAN_ME` is skipped for the offline banned player. Golden frame/client validation remains missing. |
| `com.aionemu.gameserver.utils.PacketSendUtility` | `IGameClientConnectionRegistry.SendPacketToPlayerAsync` / test registry unavailable-recipient simulation | Runtime Dependency | Partial | Regression Tested | Needs Verification | Test registry models Java offline send skipping. Production behavior still depends on connection presence and needs live validation. |
| `com.aionemu.gameserver.services.VortexService` | Deferred C# defence-team cleanup | Service Dependency | Not Started | No Tests | Unknown | Java calls `VortexService.removeDefenderPlayer` for defence alliances. C# has no live bridge in this handler yet. |
| `com.aionemu.gameserver.model.team.league.League.broadcast` | Deferred alliance ban/disband league broadcast metadata | Service Dependency | Not Started | No Tests | Unknown | Java broadcasts/removes league alliance state around ban/disband. C# records no live league membership for this path yet. |
| `com.aionemu.gameserver.services.event.EventService` | Deferred `PlayerBaseLeavePlanner` metadata | Service Dependency | Not Started | No Tests | Unknown | Java calls `EventService.onLeftTeam` after each base leave. C# records the boundary but has no live EventService bridge here. |
| `com.aionemu.gameserver.services.instance.InstanceService` | Deferred `PlayerBaseLeavePlanner` instance-kick metadata | Service Dependency | Not Started | No Tests | Unknown | Java schedules instance exit movement for registered-team instances. C# does not execute that delayed side effect here. |

## Tests Added

- `GameServerConnectionPlayerStatusInfoTests.HandlePlayerStatusInfoAsync_AllianceBanOfflineMemberSkipsBannedPlayerPacketsLikeJava`
  - Validates parsed command id `16` on a two-member non-auto alliance where the online leader bans an offline member, the alliance disbands, both memberships clear, the leader receives ban/disband fanout, and the offline banned player receives no packets.
  - This test is source-derived. It does not compare against Java runtime execution, Java-generated golden vectors, packet captures, encrypted frames, or a live client.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 20
- Total artifacts ported or partially modeled in this handoff window: 0 new production artifacts; 16 artifacts gained regression coverage in this path.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 16
- Total blocked/not-started artifacts: production offline-recipient validation, defence-team `VortexService` cleanup, league broadcast/removal, `EventService` callback, instance kick scheduling, Java static alliance registry, Java event queue/lock comparison, encoded opcode/frame golden validation, packet capture comparison, and real-client validation.
- Estimated overall migration completion: 64%

The percentage stays conservative. Alliance ban/disband offline behavior is better covered, but production/live validation gaps remain.

## Remaining Risks

- Production offline-recipient behavior still depends on live connection lookup rather than direct `Player.IsOnline` checks.
- Java defence-team `VortexService.removeDefenderPlayer` cleanup is not wired.
- Java league broadcast/removal around alliance ban/disband remains deferred.
- Java `EventService.onLeftTeam`, registered-team instance kick scheduling, and `STR_MSG_LEAVE_INSTANCE_NOT_PARTY` remain deferred.
- Java invalid target/member exception policy, warning log for target outside alliance, and static alliance registry behavior remain approximated by runtime snapshots.
- Java event queue, lock, object iteration order, offline-recipient handling, and threading behavior remain source-derived only.
- Valid league commands remain incomplete in `CM_PLAYER_STATUS_INFO`.
- Java golden byte vectors, encrypted opcode/frame validation, packet capture comparison, and real-client validation remain unavailable.

## Next Recommended Unit of Work

Continue `CM_PLAYER_STATUS_INFO` parity in this order:

1. Continue the offline-recipient audit for alliance leave paths.
2. Or begin source-reading league command prerequisites if moving into deferred valid `CM_PLAYER_STATUS_INFO` commands.
3. Keep league behavior out of production until a minimal league runtime bridge exists.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6EP-Completion.md`
   - this handoff
3. Inspect Java source for the selected branch before touching C#.
4. Implement one narrow unit with Java breadcrumbs.
5. Add focused tests that state what is source-derived and what remains unverified.
6. Run focused tests, then full GameServer tests.
7. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
8. Commit the unit.
