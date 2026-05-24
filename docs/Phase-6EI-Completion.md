# Phase 6EI Completion Handoff

Created: May 23, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6EH and covers Session 627.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerStatusInfo|PlayerAllianceRuntime|PlayerAllianceMemberInfo|BaseLeavePlanner"`
  - Result: Passed, 79 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1093 tests.

## Recent Work Completed

### Session 627 - Alliance Ban Two-Member Disband Cascade

- Source-read Java `PlayerAllianceLeavedEvent`, `AllianceDisbandEvent`, base `PlayerLeavedEvent`, and alliance ban/disband packet order.
- Added `PlayerAlliancePacketIntentKind.LeaveGroupMember` so alliance leave workflows can carry Java base leave packets for disbanded remaining members in order.
- Updated `PlayerAllianceLeavedPlanner` to append the one-member disband replay:
  - remaining member receives `STR_PARTY_ALLIANCE_DISPERSED`;
  - remaining online member receives `SM_LEAVE_GROUP_MEMBER`;
  - original banned player then receives `STR_FORCE_BAN_ME` and base leave.
- Updated `PlayerAllianceRuntime.RemoveMemberWithLeaveWorkflow` to clear remaining members and remove runtime alliance dictionaries when `WouldDisband` is true.
- Added focused parsed-command coverage for `ALLIANCE_BAN_MEMBER` from a two-member non-auto alliance.
- Commit: this handoff is included in `Cover alliance ban disband cascade`.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_PLAYER_STATUS_INFO` | `Aion.GameServer.Network.Aion.ClientPackets.CmPlayerStatusInfo` / `Aion.GameServer.Network.Aion.GameServerConnection.HandlePlayerStatusInfoAsync` | Client Packet / Handler Boundary | Partial | Regression Tested | Needs Verification | Command id `16` has explicit two-member alliance ban/disband packet-order coverage. Leader leave/disband, league commands, and full generic dispatch remain missing. |
| `com.aionemu.gameserver.model.team.common.events.TeamCommand.ALLIANCE_BAN_MEMBER` | Command code `16` branch in `GameServerConnection.HandlePlayerStatusInfoAsync` | Enum / Command Mapping | Partial | Regression Tested | Needs Verification | Two-member non-auto alliance ban/disband ordering is covered. Java invalid-member exception behavior remains deferred. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.banPlayer` | `GameServerConnection.HandleAllianceBanMemberAsync` plus `PlayerAllianceRuntime.RemoveMemberWithLeaveWorkflow` | Service / Runtime Bridge | Partial | Regression Tested | Needs Verification | Existing success path now has regression coverage when ban causes Java `shouldDisband`. Defence-team Vortex cleanup and warning log for missing target remain unmodeled. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.disband` | `PlayerAllianceLeavedPlanner` disband replay intents plus `PlayerAllianceRuntime.RemoveMemberWithLeaveWorkflow` cleanup | Service / Runtime Bridge | Partial | Regression Tested | Needs Verification | C# models the two-member disband cascade for a ban path. Explicit disband entry points, recruitment removal, and league-before/after behavior remain deferred. |
| `com.aionemu.gameserver.model.team.alliance.events.PlayerAllianceLeavedEvent` | `PlayerAllianceRuntime.RemoveMemberWithLeaveWorkflow` / connection alliance ban sends | Event Runtime/Socket Bridge | Partial | Regression Tested | Needs Verification | BAN fanout and DISBAND replay order are covered together. Leader-change, league broadcast, EventService, instance kick, and live socket ordering remain missing. |
| `com.aionemu.gameserver.model.team.alliance.events.AllianceDisbandEvent` | `PlayerAlliancePacketIntentKind.LeaveGroupMember` plus disband intents in `PlayerAllianceLeavedPlanner` | Event Runtime/Socket Bridge | Partial | Regression Tested | Needs Verification | Java replay of `PlayerAllianceLeavedEvent(DISBAND)` is source-modeled for the last remaining alliance member. Multi-member explicit disband and Java event queue behavior are not compared. |
| `com.aionemu.gameserver.model.team.common.events.PlayerLeavedEvent` | `Aion.GameServer.Services.PlayerBaseLeavePlanner` and `PlayerAlliancePacketIntentKind.LeaveGroupMember` | Base Event Dependency | Partial | Regression Tested | Needs Verification | Base leave packets are covered for both the disbanded remaining member and the banned player. Registered-team instance message/kick and EventService callback remain deferred. |
| `com.aionemu.gameserver.model.team.GeneralTeam.shouldDisband` | `PlayerAllianceRuntime.RemoveMemberWithLeaveWorkflow` disband predicate | Base Team State | Partial | Regression Tested | Needs Verification | Ban path explicitly covers non-auto alliance plus one remaining member. Runtime/threading comparison and unusual offline cases remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ALLIANCE_MEMBER_INFO` | `Aion.GameServer.Network.Aion.ServerPackets.SmAllianceMemberInfo` through alliance ban sends | Server Packet | Partial | Regression Tested | Needs Verification | Covered before ban and disband messages. Java golden bytes, encrypted frames, and real-client validation remain missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ALLIANCE_INFO` | `Aion.GameServer.Network.Aion.ServerPackets.SmAllianceInfo` through alliance ban sends | Server Packet | Partial | Regression Tested | Needs Verification | Covered before disband replay. Java golden bytes, league rows, encrypted frames, and real-client validation remain missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_LEAVE_GROUP_MEMBER` | `Aion.GameServer.Network.Aion.ServerPackets.SmLeaveGroupMember` through alliance ban/disband sends | Server Packet | Partial | Regression Tested | Needs Verification | Covered for the remaining member during disband and for the banned player after `STR_FORCE_BAN_ME`. Java registered-team instance follow-up remains deferred. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` alliance-ban/disband factories | Server Packet Factory | Partial | Regression Tested | Needs Verification | Covered Java ids `1300980`, `1300201`, and `1300979` in combined ban/disband order. Golden frame/client validation remains missing. |
| `com.aionemu.gameserver.utils.PacketSendUtility` | `IGameClientConnectionRegistry.SendPacketToPlayerAsync` / direct fallback | Runtime Dependency | Partial | Regression Tested | Needs Verification | Ordered registry sends are covered for the source-derived two-member alliance ban/disband path. Java offline-recipient behavior and live socket ordering remain unverified. |
| `com.aionemu.gameserver.services.VortexService` | Deferred C# defence-team cleanup | Service Dependency | Not Started | No Tests | Unknown | Java calls `VortexService.removeDefenderPlayer` for defence alliances. C# has no live bridge in this handler yet. |
| `com.aionemu.gameserver.model.team.league.League.broadcast` | Deferred `PlayerAllianceLeavedPlan.WouldBroadcastLeague` metadata | Service Dependency | Not Started | No Tests | Unknown | Java broadcasts/removes league alliance state around disband. C# records no live league membership for this path yet. |
| `com.aionemu.gameserver.services.event.EventService` | Deferred `PlayerBaseLeavePlanner` metadata | Service Dependency | Not Started | No Tests | Unknown | Java calls `EventService.onLeftTeam` after each base leave. C# records the boundary but has no live EventService bridge here. |
| `com.aionemu.gameserver.services.instance.InstanceService` | Deferred `PlayerBaseLeavePlanner` instance-kick metadata | Service Dependency | Not Started | No Tests | Unknown | Java schedules instance exit movement for registered-team instances. C# does not execute that delayed side effect here. |

## Tests Added

- `GameServerConnectionPlayerStatusInfoTests.HandlePlayerStatusInfoAsync_AllianceBanTwoMemberAllianceDisbandsBeforeBannedBaseLeaveLikeJava`
  - Validates parsed command id `16` on a two-member non-auto alliance clears both memberships, removes the runtime alliance, sends `STR_FORCE_BAN_HIM`, `SmAllianceMemberInfo`, `SmAllianceInfo`, alliance-dispersed id `1300201`, remaining member `SmLeaveGroupMember`, banned-player `STR_FORCE_BAN_ME`, and banned-player `SmLeaveGroupMember` in source-derived order.
  - This test is source-derived. It does not compare against Java runtime execution, Java-generated golden vectors, packet captures, encrypted frames, or a live client.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 19
- Total artifacts ported or partially modeled in this handoff window: 15
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 15
- Total blocked/not-started artifacts: alliance leader leave/disband, defence-team `VortexService` cleanup, missing-target warning log, league broadcast/removal, `EventService` callback, instance kick scheduling, league commands, Java static service registry, Java event queue/lock comparison, Java runtime/threading comparison, encoded opcode/frame golden validation, packet capture comparison, and real-client validation.
- Estimated overall migration completion: 64%

The percentage stays conservative. Alliance ban disband order is better covered, but leader/league/live validation gaps remain.

## Remaining Risks

- Alliance leader leave/disband behavior remains incomplete.
- Java defence-team `VortexService.removeDefenderPlayer` cleanup is not wired.
- Java warning log for banning a player outside the alliance is not modeled.
- Java league broadcast/removal around alliance ban disband remains deferred.
- Java `EventService.onLeftTeam`, registered-team instance kick scheduling, and `STR_MSG_LEAVE_INSTANCE_NOT_PARTY` remain deferred.
- Java invalid target/member exception policy and static alliance registry behavior remain approximated by runtime snapshots.
- Java event queue, lock, `CopyOnWriteArrayList`, alliance group internals, object iteration order, offline-recipient handling, and threading behavior remain source-derived only.
- League commands remain incomplete in `CM_PLAYER_STATUS_INFO`.
- Java golden byte vectors, encrypted opcode/frame validation, packet capture comparison, and real-client validation remain unavailable.

## Next Recommended Unit of Work

Continue `CM_PLAYER_STATUS_INFO` parity in this order:

1. Deepen `ALLIANCE_LEAVE` with two-member disband coverage using the same alliance disband replay support.
2. Then model alliance leader-leave fallback only after carefully source-reading Java's pre-remove `ChangeAllianceLeaderEvent`.
3. Keep league commands deferred until a live league runtime bridge exists.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6EH-Completion.md`
   - this handoff
3. Inspect Java source for the selected branch before touching C#.
4. Implement one narrow unit with Java breadcrumbs.
5. Add focused tests that state what is source-derived and what remains unverified.
6. Run focused tests, then full GameServer tests.
7. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
8. Commit the unit.
