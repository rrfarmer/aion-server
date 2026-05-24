# Phase 6EF Completion Handoff

Created: May 23, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6EE and covers Session 624.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerStatusInfo|PlayerGroupRuntime|BaseLeavePlanner"`
  - Result: Passed, 59 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1089 tests.

## Recent Work Completed

### Session 624 - Group Ban Two-Member Disband Coverage

- Added focused regression coverage for the combined Java `GROUP_BAN_MEMBER` plus two-member group-disband cascade.
- Confirmed the existing C# implementation preserves the source-derived order:
  - remaining member receives `SM_GROUP_MEMBER_INFO(LEAVE)`;
  - remaining member receives `STR_PARTY_HE_IS_BANISHED`;
  - remaining member receives `STR_PARTY_IS_DISPERSED`;
  - remaining online member receives `SM_LEAVE_GROUP_MEMBER`;
  - banned online player receives `STR_PARTY_YOU_ARE_BANISHED`;
  - banned online player receives base `SM_LEAVE_GROUP_MEMBER`.
- No production code changes were required in this unit.
- Commit: this handoff is included in `Cover group ban disband cascade`.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_PLAYER_STATUS_INFO` | `Aion.GameServer.Network.Aion.ClientPackets.CmPlayerStatusInfo` / `Aion.GameServer.Network.Aion.GameServerConnection.HandlePlayerStatusInfoAsync` | Client Packet / Handler Boundary | Partial | Regression Tested | Needs Verification | No production change, but command id `2` now has explicit two-member ban/disband packet-order coverage. Alliance leave/ban, league commands, and full generic dispatch remain missing. |
| `com.aionemu.gameserver.model.team.common.events.TeamCommand.GROUP_BAN_MEMBER` | Command code `2` branch in `GameServerConnection.HandlePlayerStatusInfoAsync` | Enum / Command Mapping | Partial | Regression Tested | Needs Verification | Two-member non-auto group ban/disband ordering is now covered. Java invalid-member exception behavior remains deferred. |
| `com.aionemu.gameserver.model.team.group.PlayerGroupService.banPlayer` | `GameServerConnection.HandleGroupBanMemberAsync` plus `PlayerGroupRuntime.RemoveMemberWithLeavePlan` | Service / Runtime Bridge | Partial | Regression Tested | Needs Verification | Existing success path now has regression coverage when the ban causes Java `shouldDisband`. Java warning log for missing target remains unmodeled. |
| `com.aionemu.gameserver.model.team.group.events.PlayerGroupLeavedEvent` | `PlayerGroupRuntime.RemoveMemberWithLeavePlan` / connection group ban sends | Event Runtime/Socket Bridge | Partial | Regression Tested | Needs Verification | BAN reason fanout and DISBAND replay order are covered together. Mentor-stop, EventService, instance kick, and live socket ordering remain missing. |
| `com.aionemu.gameserver.model.team.group.events.GroupDisbandEvent` | `PlayerGroupLeavePacketIntentKind.LeaveGroupMember` plus disband intents in `PlayerGroupRuntime` | Event Runtime/Socket Bridge | Partial | Regression Tested | Needs Verification | Java replay of `PlayerGroupLeavedEvent(DISBAND)` is covered after a ban leaves one member. Multi-member explicit disband and Java event queue behavior are not compared. |
| `com.aionemu.gameserver.model.team.common.events.PlayerLeavedEvent` | `Aion.GameServer.Services.PlayerBaseLeavePlanner` and `PlayerGroupLeavePacketIntentKind.LeaveGroupMember` | Base Event Dependency | Partial | Regression Tested | Needs Verification | Base leave packets are covered for both the disbanded remaining member and the banned player. Registered-team instance message/kick and EventService callback remain deferred. |
| `com.aionemu.gameserver.model.team.GeneralTeam.shouldDisband` | `PlayerGroupRuntime.RemoveMemberWithLeavePlan` disband predicate | Base Team State | Partial | Regression Tested | Needs Verification | Ban path explicitly covers non-auto group plus one remaining member. Runtime/threading comparison and unusual offline cases remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_GROUP_MEMBER_INFO` | `Aion.GameServer.Network.Aion.ServerPackets.SmGroupMemberInfo` through group ban sends | Server Packet | Partial | Regression Tested | Needs Verification | Covered before ban and disband messages. Java golden bytes, encrypted frames, and real-client validation remain missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_LEAVE_GROUP_MEMBER` | `Aion.GameServer.Network.Aion.ServerPackets.SmLeaveGroupMember` through group ban/disband sends | Server Packet | Partial | Regression Tested | Needs Verification | Covered for the remaining member during disband and for the banned player after `STR_PARTY_YOU_ARE_BANISHED`. Java registered-team instance follow-up remains deferred. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` group-ban/disband factories | Server Packet Factory | Partial | Regression Tested | Needs Verification | Covered Java ids `1300177`, `1300167`, and `1300166` in combined ban/disband order. Golden frame/client validation remains missing. |
| `com.aionemu.gameserver.utils.PacketSendUtility` | `IGameClientConnectionRegistry.SendPacketToPlayerAsync` / direct fallback | Runtime Dependency | Partial | Regression Tested | Needs Verification | Ordered registry sends are covered for the source-derived two-member ban/disband path. Java offline-recipient behavior and live socket ordering remain unverified. |

## Tests Added

- `GameServerConnectionPlayerStatusInfoTests.HandlePlayerStatusInfoAsync_GroupBanTwoMemberGroupDisbandsBeforeBanishedMessageLikeJava`
  - Validates parsed command id `2` on a two-member non-auto group clears both memberships, removes the runtime group, sends `SmGroupMemberInfo`, Java id `1300177`, Java disband id `1300167`, remaining member `SmLeaveGroupMember`, Java id `1300166`, and banned-player `SmLeaveGroupMember` in source-derived order.
  - This test is source-derived. It does not compare against Java runtime execution, Java-generated golden vectors, packet captures, encrypted frames, or a live client.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 13
- Total artifacts ported or partially modeled in this handoff window: 11
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 12
- Total blocked/not-started artifacts: Java invalid-member exception/logging policy, mentor-stop side effect, `EventService` callback, instance kick scheduling, alliance leave/ban, league commands, Java static service registry, Java event queue/lock comparison, Java runtime/threading comparison, encoded opcode/frame golden validation, packet capture comparison, and real-client validation.
- Estimated overall migration completion: 64%

The percentage stays conservative. Group ban is better covered, but broader team leave/ban and live validation work remain incomplete.

## Remaining Risks

- Java invalid target member lookup can throw through `Objects.requireNonNull`; C# still no-ops missing targets.
- Java warning log for attempting to ban a player outside the group is not modeled.
- Java mentor stop side effects when a banned mentor leaves are flagged but not wired.
- Java `EventService.onLeftTeam`, registered-team instance kick scheduling, and `STR_MSG_LEAVE_INSTANCE_NOT_PARTY` remain deferred.
- Java static group registry lookup is approximated by runtime snapshots attached to players.
- Java event queue, lock, group stats recalculation, object iteration order, offline-recipient handling, and threading behavior remain source-derived only.
- Alliance leave/ban and league commands remain incomplete in `CM_PLAYER_STATUS_INFO`.
- Java golden byte vectors, encrypted opcode/frame validation, packet capture comparison, and real-client validation remain unavailable.

## Next Recommended Unit of Work

Continue `CM_PLAYER_STATUS_INFO` parity in this order:

1. Move laterally to `ALLIANCE_LEAVE` or `ALLIANCE_BAN_MEMBER` only if existing alliance planners can cover leave/disband/leader side effects accurately.
2. Otherwise, harden remaining group-ban edge behavior such as missing target exception/log parity or offline banned-player packet suppression.
3. Keep league commands deferred until a live league runtime bridge exists.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6EE-Completion.md`
   - this handoff
3. Inspect Java source for the selected branch before touching C#.
4. Implement one narrow unit with Java breadcrumbs.
5. Add focused tests that state what is source-derived and what remains unverified.
6. Run focused tests, then full GameServer tests.
7. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
8. Commit the unit.
