# Phase 6EH Completion Handoff

Created: May 23, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6EG and covers Session 626.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerStatusInfo|PlayerAllianceRuntime|PlayerAllianceMemberInfo|BaseLeavePlanner"`
  - Result: Passed, 78 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1092 tests.

## Recent Work Completed

### Session 626 - Alliance Ban Status Command

- Source-read Java `TeamCommand.ALLIANCE_BAN_MEMBER`, `PlayerTeamCommandService.executeCommand`, `PlayerAllianceService.banPlayer`, `PlayerAllianceLeavedEvent`, base `PlayerLeavedEvent`, and alliance ban system-message factories.
- Added `SmSystemMessage.ForceCantBanSelf` for Java id `1400706`.
- Wired parsed `CM_PLAYER_STATUS_INFO` command id `16`:
  - self-ban sends `STR_FORCE_CANT_BAN_SELF`;
  - non-leader ban sends `STR_FORCE_ONLY_LEADER_CAN_BANISH`;
  - auto-alliance ban sends `STR_MSG_PARTY_FORCE_NO_RIGHT_TO_DECIDE`;
  - success reuses `RemoveMemberWithLeaveWorkflow` with `PlayerAllianceLeaveReason.Ban`;
  - success sends remaining-member ban fanout, banned-player `STR_FORCE_BAN_ME`, then base `SM_LEAVE_GROUP_MEMBER`.
- Added focused regression coverage for failure branches and common non-disband success fanout.
- Commit: this handoff is included in `Wire alliance ban status command`.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_PLAYER_STATUS_INFO` | `Aion.GameServer.Network.Aion.ClientPackets.CmPlayerStatusInfo` / `Aion.GameServer.Network.Aion.GameServerConnection.HandlePlayerStatusInfoAsync` | Client Packet / Handler Boundary | Partial | Regression Tested | Needs Verification | Command id `16` covers alliance ban failure messages and common non-disband success fanout. Leader leave, alliance disband, league commands, and full generic dispatch remain missing. |
| `com.aionemu.gameserver.model.team.common.events.TeamCommand.ALLIANCE_BAN_MEMBER` | Command code `16` branch in `GameServerConnection.HandlePlayerStatusInfoAsync` | Enum / Command Mapping | Partial | Regression Tested | Needs Verification | C# targets `selectedObjectId` or caller when zero, performs Java ban gate checks, and dispatches success through alliance leave workflow planning. Java invalid-member exception behavior remains deferred. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.banPlayer` | `GameServerConnection.HandleAllianceBanMemberAsync` plus `PlayerAllianceRuntime.RemoveMemberWithLeaveWorkflow` | Service / Runtime Bridge | Partial | Regression Tested | Needs Verification | Self, non-leader, auto-alliance, and common success paths are modeled. Defence-team Vortex cleanup and warning log when target is not in alliance are not modeled. |
| `com.aionemu.gameserver.model.team.alliance.events.PlayerAllianceLeavedEvent` | `PlayerAllianceRuntime.RemoveMemberWithLeaveWorkflow` / connection alliance ban sends | Event Runtime/Socket Bridge | Partial | Regression Tested | Needs Verification | BAN reason fanout sends `STR_FORCE_BAN_HIM`, member-info, alliance-info to remaining members, then `STR_FORCE_BAN_ME` and base leave to banned player. Disband replay, league broadcast, EventService, instance kick, and live socket ordering remain missing. |
| `com.aionemu.gameserver.model.team.TeamType.AUTO_ALLIANCE` | `Aion.GameServer.Model.GameObjects.PlayerAllianceTeamType.AutoAlliance` | Enum / Team Type | Partial | Regression Tested | Needs Verification | Auto-alliance ban denial uses runtime descriptor `TeamType`. Other Java auto-alliance matching behavior remains outside this unit. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ALLIANCE_MEMBER_INFO` | `Aion.GameServer.Network.Aion.ServerPackets.SmAllianceMemberInfo` through alliance ban sends | Server Packet | Partial | Regression Tested | Needs Verification | Sent to remaining members after `STR_FORCE_BAN_HIM`. Java golden bytes, encrypted frames, and real-client validation remain missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ALLIANCE_INFO` | `Aion.GameServer.Network.Aion.ServerPackets.SmAllianceInfo` through alliance ban sends | Server Packet | Partial | Regression Tested | Needs Verification | Sent to remaining members after member-info. Java golden bytes, league rows, encrypted frames, and real-client validation remain missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_LEAVE_GROUP_MEMBER` | `Aion.GameServer.Network.Aion.ServerPackets.SmLeaveGroupMember` through alliance ban sends | Server Packet | Partial | Regression Tested | Needs Verification | Sent to the banned online player after `STR_FORCE_BAN_ME`. Java registered-team instance follow-up remains deferred. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` alliance-ban factories | Server Packet Factory | Partial | Regression Tested | Needs Verification | Covered Java ids `1400706`, `1301009`, `1400749`, `1300980`, and `1300979`. Golden frame/client validation remains missing. |
| `com.aionemu.gameserver.utils.PacketSendUtility` | `IGameClientConnectionRegistry.SendPacketToPlayerAsync` / direct fallback | Runtime Dependency | Partial | Regression Tested | Needs Verification | Ordered registry sends are covered for alliance-ban failures and common success. Java offline-recipient behavior and live socket ordering remain unverified. |
| `com.aionemu.gameserver.services.VortexService` | Deferred C# defence-team cleanup | Service Dependency | Not Started | No Tests | Unknown | Java calls `VortexService.removeDefenderPlayer` for defence alliances. C# has no live bridge in this handler yet. |
| `com.aionemu.gameserver.model.team.league.League.broadcast` | Deferred `PlayerAllianceLeavedPlan.WouldBroadcastLeague` metadata | Service Dependency | Not Started | No Tests | Unknown | Java broadcasts league alliance state on normal ban. C# records no live league membership for this path yet. |
| `com.aionemu.gameserver.services.event.EventService` | Deferred `PlayerBaseLeavePlanner` metadata | Service Dependency | Not Started | No Tests | Unknown | Java calls `EventService.onLeftTeam` after base leave. C# records the boundary but has no live EventService bridge here. |
| `com.aionemu.gameserver.services.instance.InstanceService` | Deferred `PlayerBaseLeavePlanner` instance-kick metadata | Service Dependency | Not Started | No Tests | Unknown | Java schedules an instance exit movement for registered-team instances. C# does not execute that delayed side effect here. |

## Tests Added

- `GameServerConnectionPlayerStatusInfoTests.HandlePlayerStatusInfoAsync_AllianceBanFailureBranchesSendJavaMessages`
  - Validates parsed command id `16` sends Java message ids `1400706`, `1301009`, and `1400749` for self-ban, non-leader ban, and auto-alliance ban attempts without mutating alliance membership.
- `GameServerConnectionPlayerStatusInfoTests.HandlePlayerStatusInfoAsync_AllianceBanMemberSendsBanFanoutThenBaseLeaveLikeJava`
  - Validates parsed command id `16` removes a banned vice-captain from runtime alliance state, clears membership, removes vice-captain role, sends `STR_FORCE_BAN_HIM`, `SmAllianceMemberInfo`, and `SmAllianceInfo` to each remaining member, then sends `STR_FORCE_BAN_ME` and `SmLeaveGroupMember` to the banned player.
- These tests are source-derived. They do not compare against Java runtime execution, Java-generated golden vectors, packet captures, encrypted frames, or a live client.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 22
- Total artifacts ported or partially modeled in this handoff window: 18
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 18
- Total blocked/not-started artifacts: alliance ban disband cascade, defence-team `VortexService` cleanup, missing-target warning log, league broadcast, `EventService` callback, instance kick scheduling, alliance leader leave/disband, league commands, Java static service registry, Java event queue/lock comparison, Java runtime/threading comparison, encoded opcode/frame golden validation, packet capture comparison, and real-client validation.
- Estimated overall migration completion: 64%

The percentage stays conservative. Alliance ban now has failure and common success coverage, but cascade and live validation gaps remain.

## Remaining Risks

- Alliance ban disband cascade is not executed yet when ban reduces a non-auto alliance to one member.
- Java defence-team `VortexService.removeDefenderPlayer` cleanup is not wired.
- Java warning log for banning a player outside the alliance is not modeled.
- Java league broadcast after alliance ban remains deferred.
- Java `EventService.onLeftTeam`, registered-team instance kick scheduling, and `STR_MSG_LEAVE_INSTANCE_NOT_PARTY` remain deferred.
- Java invalid target/member exception policy and static alliance registry behavior remain approximated by runtime snapshots.
- Java event queue, lock, `CopyOnWriteArrayList`, alliance group internals, object iteration order, offline-recipient handling, and threading behavior remain source-derived only.
- Alliance leader leave/disband and league commands remain incomplete in `CM_PLAYER_STATUS_INFO`.
- Java golden byte vectors, encrypted opcode/frame validation, packet capture comparison, and real-client validation remain unavailable.

## Next Recommended Unit of Work

Continue `CM_PLAYER_STATUS_INFO` parity in this order:

1. Add focused two-member `ALLIANCE_BAN_MEMBER` disband coverage if the existing alliance leave workflow can be extended to replay `AllianceDisbandEvent` accurately.
2. Otherwise deepen `ALLIANCE_LEAVE` leader/disband behavior or defer to the next non-league `CM_PLAYER_STATUS_INFO` branch with fewer missing side effects.
3. Keep league commands deferred until a live league runtime bridge exists.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6EG-Completion.md`
   - this handoff
3. Inspect Java source for the selected branch before touching C#.
4. Implement one narrow unit with Java breadcrumbs.
5. Add focused tests that state what is source-derived and what remains unverified.
6. Run focused tests, then full GameServer tests.
7. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
8. Commit the unit.
