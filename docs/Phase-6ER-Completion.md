# Phase 6ER Completion Handoff

Created: May 23, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6EQ and covers Session 636.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerStatusInfo|PlayerAllianceRuntime|PlayerAllianceMemberInfo|BaseLeavePlanner"`
  - Result: Passed, 88 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1102 tests.

## Recent Work Completed

### Session 636 - Alliance Leave Offline Remaining-Leader Coverage

- Source-read Java `PlayerAllianceService.removePlayer`, `PlayerAllianceLeavedEvent`, `AllianceDisbandEvent`, base `PlayerLeavedEvent`, and `PacketSendUtility.sendPacket`.
- Added focused parsed-command coverage for `ALLIANCE_LEAVE` when an online non-leader leaves a two-member non-auto alliance and the remaining leader is offline.
- Reused the test registry unavailable-recipient simulation to model Java `PacketSendUtility.sendPacket` skipping offline recipients.
- Verified the existing C# alliance leave/disband composition clears both memberships and removes the runtime alliance while delivering only the online leaver's base `SM_LEAVE_GROUP_MEMBER`; the offline remaining leader receives no leave fanout or disband replay packets.
- No production code changed in this unit; this is regression coverage for an already-modeled source-derived path.
- Commit: this handoff is included in `Cover alliance leave offline disband`.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_PLAYER_STATUS_INFO` | `Aion.GameServer.Network.Aion.ClientPackets.CmPlayerStatusInfo` / `Aion.GameServer.Network.Aion.GameServerConnection.HandlePlayerStatusInfoAsync` | Client Packet / Handler Boundary | Partial | Regression Tested | Needs Verification | Command id `14` has explicit two-member non-leader alliance leave/disband coverage with an offline remaining leader. Valid league commands and full generic dispatch remain incomplete. |
| `com.aionemu.gameserver.model.team.common.events.TeamCommand.ALLIANCE_LEAVE` | Command code `14` branch in `GameServerConnection.HandlePlayerStatusInfoAsync` | Enum / Command Mapping | Partial | Regression Tested | Needs Verification | Test covers an online non-leader leaving while the only remaining alliance member is offline. Leader-leave fallback paths remain separately covered; league leave behavior remains deferred. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.removePlayer` | `GameServerConnection.HandleAllianceLeaveAsync` plus `PlayerAllianceRuntime.RemoveMemberWithLeaveWorkflow` | Service / Runtime Bridge | Partial | Regression Tested | Needs Verification | Existing C# flow removes the online leaver, disbands the remaining offline leader, and clears runtime state. Java static alliance registry and Vortex defence-team cleanup are not runtime-compared. |
| `com.aionemu.gameserver.model.team.alliance.events.PlayerAllianceLeavedEvent` | `PlayerAllianceRuntime.RemoveMemberWithLeaveWorkflow` / connection alliance leave sends | Event Runtime/Socket Bridge | Partial | Regression Tested | Needs Verification | LEAVE fanout and DISBAND replay are covered when the only remaining recipient is offline. League broadcast, EventService, registered-team instance kick, and live socket ordering remain missing. |
| `com.aionemu.gameserver.model.team.alliance.events.AllianceDisbandEvent` | `PlayerAlliancePacketIntentKind.LeaveGroupMember` plus disband intents in `PlayerAllianceLeavedPlanner` | Event Runtime/Socket Bridge | Partial | Regression Tested | Needs Verification | Disband replay is source-modeled for the offline remaining leader after the online member leaves. Java event queue/lock behavior is not compared. |
| `com.aionemu.gameserver.model.team.common.events.PlayerLeavedEvent` | `Aion.GameServer.Services.PlayerBaseLeavePlanner` and `PlayerAlliancePacketIntentKind.LeaveGroupMember` | Base Event Dependency | Partial | Regression Tested | Needs Verification | Online leaver receives base `SM_LEAVE_GROUP_MEMBER`; offline remaining leader receives no base leave packet. Registered-team instance message/kick and EventService callback remain deferred. |
| `com.aionemu.gameserver.model.team.GeneralTeam.removeMember` | `PlayerAllianceRuntime.RemoveMemberWithLeaveWorkflow` | Base Team State | Partial | Regression Tested | Needs Verification | Runtime disband cleanup clears both memberships and removes alliance dictionaries. Java object-wrapper mutation and synchronization semantics are not compared. |
| `com.aionemu.gameserver.model.team.GeneralTeam.shouldDisband` | `PlayerAllianceRuntime.RemoveMemberWithLeaveWorkflow` disband predicate | Base Team State | Partial | Regression Tested | Needs Verification | Covers non-auto alliance plus one remaining member after non-leader leave. Runtime/threading comparison remains unverified. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAlliance` | `Aion.GameServer.Model.GameObjects.PlayerAllianceDescriptor` / `PlayerAllianceRuntime` member store | Team State | Partial | Regression Tested | Needs Verification | Runtime clears both players and removes alliance dictionaries after disband. Java static alliance registry, group internals, and synchronized event semantics are not compared. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ALLIANCE_MEMBER_INFO` | `Aion.GameServer.Network.Aion.ServerPackets.SmAllianceMemberInfo` through alliance leave sends | Server Packet | Partial | Regression Tested | Needs Verification | Planned for the offline remaining leader and skipped by registry simulation. Java golden bytes and live client validation remain missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ALLIANCE_INFO` | `Aion.GameServer.Network.Aion.ServerPackets.SmAllianceInfo` through alliance leave sends | Server Packet | Partial | Regression Tested | Needs Verification | Planned for the offline remaining leader before disband replay and skipped by registry simulation. Java golden bytes, league rows, and live client validation remain missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_LEAVE_GROUP_MEMBER` | `Aion.GameServer.Network.Aion.ServerPackets.SmLeaveGroupMember` through alliance leave/disband sends | Server Packet | Partial | Regression Tested | Needs Verification | Delivered only to the online leaver as the base leave packet; skipped for the offline remaining leader during disband replay. Java registered-team instance follow-up remains deferred. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` alliance leave/disband factories | Server Packet Factory | Partial | Regression Tested | Needs Verification | `STR_FORCE_LEAVE_HIM` and `STR_PARTY_ALLIANCE_DISPERSED` are planned for the offline remaining leader and skipped by registry simulation. Golden frame/client validation remains missing. |
| `com.aionemu.gameserver.utils.PacketSendUtility` | `IGameClientConnectionRegistry.SendPacketToPlayerAsync` / test registry unavailable-recipient simulation | Runtime Dependency | Partial | Regression Tested | Needs Verification | Test registry models Java offline send skipping. Production behavior still depends on connection presence and needs live validation. |
| `com.aionemu.gameserver.services.VortexService` | Deferred C# defence-team cleanup | Service Dependency | Not Started | No Tests | Unknown | Java calls `VortexService.removeDefenderPlayer` for defence alliances. C# has no live bridge in this handler yet. |
| `com.aionemu.gameserver.model.team.league.League.broadcast` | Deferred alliance leave/disband league broadcast metadata | Service Dependency | Not Started | No Tests | Unknown | Java broadcasts league alliance state around leave/disband. C# records no live league membership for this path yet. |
| `com.aionemu.gameserver.services.event.EventService` | Deferred `PlayerBaseLeavePlanner` metadata | Service Dependency | Not Started | No Tests | Unknown | Java calls `EventService.onLeftTeam` after each base leave. C# records the boundary but has no live EventService bridge here. |
| `com.aionemu.gameserver.services.instance.InstanceService` | Deferred `PlayerBaseLeavePlanner` instance-kick metadata | Service Dependency | Not Started | No Tests | Unknown | Java schedules instance exit movement for registered-team instances. C# does not execute that delayed side effect here. |

## Tests Added

- `GameServerConnectionPlayerStatusInfoTests.HandlePlayerStatusInfoAsync_AllianceLeaveTwoMemberAllianceSkipsOfflineRemainingDisbandPacketsLikeJava`
  - Validates parsed command id `14` on a two-member non-auto alliance where an online non-leader leaves, the remaining leader is offline, the alliance disbands, both memberships clear, and only the online leaver receives base `SmLeaveGroupMember`.
  - This test is source-derived. It does not compare against Java runtime execution, Java-generated golden vectors, packet captures, encrypted frames, or a live client.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 19
- Total artifacts ported or partially modeled in this handoff window: 0 new production artifacts; 15 artifacts gained regression coverage in this path.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 15
- Total blocked/not-started artifacts: production offline-recipient validation, defence-team `VortexService` cleanup, league broadcast/removal, `EventService` callback, instance kick scheduling, Java static alliance registry, Java event queue/lock comparison, encoded opcode/frame golden validation, packet capture comparison, and real-client validation.
- Estimated overall migration completion: 64%

The percentage stays conservative. Alliance leave/disband offline behavior is better covered, but production/live validation gaps remain.

## Remaining Risks

- Production offline-recipient behavior still depends on live connection lookup rather than direct `Player.IsOnline` checks.
- Java defence-team `VortexService.removeDefenderPlayer` cleanup is not wired.
- Java league broadcast/removal around alliance leave/disband remains deferred.
- Java `EventService.onLeftTeam`, registered-team instance kick scheduling, and `STR_MSG_LEAVE_INSTANCE_NOT_PARTY` remain deferred.
- Java invalid target/member exception policy and static alliance registry behavior remain approximated by runtime snapshots.
- Java event queue, lock, object iteration order, offline-recipient handling, and threading behavior remain source-derived only.
- Valid league commands remain incomplete in `CM_PLAYER_STATUS_INFO`.
- Java golden byte vectors, encrypted opcode/frame validation, packet capture comparison, and real-client validation remain unavailable.

## Next Recommended Unit of Work

Continue `CM_PLAYER_STATUS_INFO` parity in this order:

1. Treat the immediate group/alliance offline-recipient audit as covered for group remove, group ban, alliance ban, and alliance leave.
2. Source-read league command prerequisites for deferred valid `CM_PLAYER_STATUS_INFO` commands, or audit production connection-presence behavior against Java `Player.isOnline()`.
3. Keep league behavior out of production until a minimal league runtime bridge exists.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6EQ-Completion.md`
   - this handoff
3. Inspect Java source for the selected branch before touching C#.
4. Implement one narrow unit with Java breadcrumbs.
5. Add focused tests that state what is source-derived and what remains unverified.
6. Run focused tests, then full GameServer tests.
7. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
8. Commit the unit.
