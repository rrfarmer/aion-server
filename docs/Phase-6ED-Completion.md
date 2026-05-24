# Phase 6ED Completion Handoff

Created: May 23, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6EC and covers Session 622.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerStatusInfo|PlayerGroupRuntime|BaseLeavePlanner"`
  - Result: Passed, 56 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1086 tests.

## Recent Work Completed

### Session 622 - Group Remove Two-Member Disband Cascade

- Source-read Java `PlayerGroupLeavedEvent`, `GroupDisbandEvent`, base `PlayerLeavedEvent`, and `GeneralTeam.shouldDisband`.
- Extended group leave packet intents with `LeaveGroupMember` so disband replay can emit `SM_LEAVE_GROUP_MEMBER` in the same ordered stream as the remaining member's disband message.
- Updated `PlayerGroupRuntime.RemoveMemberWithLeavePlan` so two-member non-auto group removal now matches the source-derived packet order:
  - remaining member receives `SM_GROUP_MEMBER_INFO(LEAVE)`;
  - remaining member receives `STR_PARTY_HE_LEAVE_PARTY`;
  - remaining member receives `STR_PARTY_IS_DISPERSED`;
  - remaining online member receives `SM_LEAVE_GROUP_MEMBER`;
  - originally removed online member receives base `SM_LEAVE_GROUP_MEMBER` last.
- Added regression coverage for parsed `CM_PLAYER_STATUS_INFO` command id `6` on a two-member group.
- Commit: this handoff is included in `Cover group remove disband cascade`.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_PLAYER_STATUS_INFO` | `Aion.GameServer.Network.Aion.ClientPackets.CmPlayerStatusInfo` / `Aion.GameServer.Network.Aion.GameServerConnection.HandlePlayerStatusInfoAsync` | Client Packet / Handler Boundary | Partial | Regression Tested | Needs Verification | Command id `6` covers normal member removal, leader-removal fallback, and two-member disband packet order. Group ban, alliance leave/ban, league commands, and full generic dispatch remain missing. |
| `com.aionemu.gameserver.model.team.common.events.TeamCommand.GROUP_REMOVE_MEMBER` | Command code `6` branch in `GameServerConnection.HandlePlayerStatusInfoAsync` | Enum / Command Mapping | Partial | Regression Tested | Needs Verification | Two-member non-auto group removal triggers the disband cascade packet stream. Java invalid-member exception behavior remains deferred. |
| `com.aionemu.gameserver.model.team.group.PlayerGroupService.removePlayer` | `Aion.GameServer.Services.PlayerGroupRuntime.RemoveMemberWithLeavePlan` through connection handler | Service / Runtime Bridge | Partial | Regression Tested | Needs Verification | Runtime clears the removed member, disbands the remaining member when Java `shouldDisband` would be true, and removes C# runtime dictionaries. Java static registry and stats recalculation are not runtime-compared. |
| `com.aionemu.gameserver.model.team.group.PlayerGroupService.disband` | `PlayerGroupRuntime.RemoveMemberWithLeavePlan` disband branch | Service / Runtime Bridge | Partial | Regression Tested | Needs Verification | C# models the two-member cascade by appending disband message plus remaining member leave packet. Broader explicit disband entry points are not ported. |
| `com.aionemu.gameserver.model.team.group.events.PlayerGroupLeavedEvent` | `PlayerGroupRuntime.RemoveMemberWithLeavePlan` / connection group remove sends | Event Runtime/Socket Bridge | Partial | Regression Tested | Needs Verification | Leave, leader-removal fallback, and two-member disband packet order are covered. Mentor-stop side effects, EventService, instance kick, and live socket ordering remain missing. |
| `com.aionemu.gameserver.model.team.group.events.GroupDisbandEvent` | `PlayerGroupLeavePacketIntentKind.LeaveGroupMember` plus disband intents in `PlayerGroupRuntime` | Event Runtime/Socket Bridge | Partial | Regression Tested | Needs Verification | Java replay of `PlayerGroupLeavedEvent(DISBAND)` is source-modeled for the last remaining group member. Multi-member explicit disband and Java event queue behavior are not compared. |
| `com.aionemu.gameserver.model.team.common.events.PlayerLeavedEvent` | `Aion.GameServer.Services.PlayerBaseLeavePlanner` and `PlayerGroupLeavePacketIntentKind.LeaveGroupMember` | Base Event Dependency | Partial | Regression Tested | Needs Verification | Base leave packets are sent for both the remaining disbanded online member and the originally removed online member. Registered-team instance message/kick and EventService callback remain deferred. |
| `com.aionemu.gameserver.model.team.GeneralTeam.shouldDisband` | `PlayerGroupRuntime.RemoveMemberWithLeavePlan` disband predicate | Base Team State | Partial | Regression Tested | Needs Verification | C# uses non-auto group plus one remaining member, matching Java source. Runtime/threading comparison and unusual offline cases remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_GROUP_MEMBER_INFO` | `Aion.GameServer.Network.Aion.ServerPackets.SmGroupMemberInfo` through group remove sends | Server Packet | Partial | Regression Tested | Needs Verification | Sent before disband messages for the remaining member. Java golden bytes, encrypted frames, and real-client validation remain missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_LEAVE_GROUP_MEMBER` | `Aion.GameServer.Network.Aion.ServerPackets.SmLeaveGroupMember` through group remove/disband sends | Server Packet | Partial | Regression Tested | Needs Verification | Covered for the remaining member during disband and for the originally removed player. Java registered-team instance follow-up remains deferred. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` group leave/disband factories | Server Packet Factory | Partial | Regression Tested | Needs Verification | Covered Java ids `1300168` and `1300167` in the two-member disband order. Golden frame/client validation remains missing. |
| `com.aionemu.gameserver.services.event.EventService` | Deferred `PlayerGroupLeavePlan.WouldInvokeEventServiceOnLeftTeam` metadata | Service Dependency | Not Started | No Tests | Unknown | Java calls `EventService.onLeftTeam` for both the original leave and disband replay. C# records only the original base leave boundary and has no live EventService bridge here. |
| `com.aionemu.gameserver.services.instance.InstanceService` | Deferred `PlayerBaseLeavePlanner` instance-kick metadata | Service Dependency | Not Started | No Tests | Unknown | Java schedules an instance exit movement for registered-team instances. C# does not execute that delayed side effect for either disbanded member yet. |
| `com.aionemu.gameserver.utils.PacketSendUtility` | `IGameClientConnectionRegistry.SendPacketToPlayerAsync` / direct fallback | Runtime Dependency | Partial | Regression Tested | Needs Verification | Ordered registry sends are covered for normal removal, leader fallback, and two-member disband. Java offline-recipient behavior and live socket ordering remain unverified. |

## Tests Added

- `GameServerConnectionPlayerStatusInfoTests.HandlePlayerStatusInfoAsync_GroupRemoveTwoMemberGroupDisbandsRemainingMemberLikeJava`
  - Validates parsed command id `6` on a two-member non-auto group clears both memberships, removes the group runtime entry, and sends `SmGroupMemberInfo`, Java message id `1300168`, Java disband id `1300167`, remaining member `SmLeaveGroupMember`, and originally removed player `SmLeaveGroupMember` in source-derived order.
  - This does not compare against Java runtime execution, Java-generated golden vectors, packet captures, encrypted frames, or a live client.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 14
- Total artifacts ported or partially modeled in this handoff window: 12
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 11
- Total blocked/not-started artifacts: mentor-stop side effect, `EventService` callback, instance kick scheduling, group ban, alliance leave/ban, league commands, Java static service registry, Java event queue/lock comparison, Java runtime/threading comparison, encoded opcode/frame golden validation, packet capture comparison, and real-client validation.
- Estimated overall migration completion: 64%

The percentage stays conservative. `GROUP_REMOVE_MEMBER` has stronger useful coverage now, but `GROUP_BAN_MEMBER` and broader team-command parity are still unported.

## Remaining Risks

- Java mentor stop side effects when a mentor leaves are still flagged but not wired.
- Java `EventService.onLeftTeam` and registered-team instance kick scheduling remain deferred.
- Java invalid target member lookup can throw through `Objects.requireNonNull`; the C# packet handler currently no-ops missing targets.
- Java static group registry lookup is approximated by runtime snapshots attached to players.
- Java event queue, lock, group stats recalculation, object iteration order, offline-only leader fallback, and threading behavior remain source-derived only.
- Remaining `CM_PLAYER_STATUS_INFO` branches include group ban, alliance leave/ban, and league commands.
- Java golden byte vectors, encrypted opcode/frame validation, packet capture comparison, and real-client validation remain unavailable.

## Next Recommended Unit of Work

Continue `CM_PLAYER_STATUS_INFO` parity in this order:

1. Port `GROUP_BAN_MEMBER`, starting with self-ban/no-rights/auto-group failure messages.
2. If the failure-message slice stays clean, add ban-success leave fanout by reusing `RemoveMemberWithLeavePlan` with `PlayerGroupLeaveReason.Ban`.
3. Document carefully that Java sends `STR_PARTY_YOU_ARE_BANISHED` to the banned player after the remaining-member fanout; add that packet only when the source path is inspected and tested.
4. Keep alliance leave/ban and league commands deferred unless existing planners can cover cascading effects.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6EC-Completion.md`
   - this handoff
3. Inspect Java source for `GROUP_BAN_MEMBER` before touching C#.
4. Implement one narrow unit with Java breadcrumbs.
5. Add focused tests that state what is source-derived and what remains unverified.
6. Run focused tests, then full GameServer tests.
7. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
8. Commit the unit.
