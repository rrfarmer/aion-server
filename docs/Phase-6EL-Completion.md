# Phase 6EL Completion Handoff

Created: May 23, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6EK and covers Session 630.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerStatusInfo|PlayerAllianceRuntime|PlayerAllianceMemberInfo|BaseLeavePlanner"`
  - Result: Passed, 82 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1096 tests.

## Recent Work Completed

### Session 630 - Alliance Leader Leave Two-Member Disband Coverage

- Source-read Java `ChangeAllianceLeaderEvent`, `PlayerAllianceLeavedEvent`, `AllianceDisbandEvent`, and base `PlayerLeavedEvent`.
- Added focused parsed-command coverage for `ALLIANCE_LEAVE` when a leader leaves a two-member non-auto alliance and the remaining online vice captain is promoted before the alliance disbands.
- Verified the existing C# composition sends the source-derived order:
  - `SM_ALLIANCE_INFO` to the old leader and fallback leader from `ChangeAllianceLeaderEvent`;
  - `STR_FORCE_YOU_BECOME_NEW_LEADER` to the fallback leader;
  - `STR_FORCE_LEAVE_HIM`, `SM_ALLIANCE_MEMBER_INFO`, and `SM_ALLIANCE_INFO` to the remaining member from `PlayerAllianceLeavedEvent`;
  - `STR_PARTY_ALLIANCE_DISPERSED` and `SM_LEAVE_GROUP_MEMBER` to the remaining member from the disband replay;
  - original old-leader base `SM_LEAVE_GROUP_MEMBER` after the disband replay.
- Confirmed runtime cleanup clears both player memberships and removes the emptied alliance member list.
- Commit: this handoff is included in `Cover alliance leader leave disband`.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_PLAYER_STATUS_INFO` | `Aion.GameServer.Network.Aion.ClientPackets.CmPlayerStatusInfo` / `Aion.GameServer.Network.Aion.GameServerConnection.HandlePlayerStatusInfoAsync` | Client Packet / Handler Boundary | Partial | Regression Tested | Needs Verification | Command id `14` has explicit two-member leader-leave/disband composition coverage. No-online-fallback, league commands, and full generic dispatch remain missing. |
| `com.aionemu.gameserver.model.team.common.events.TeamCommand.ALLIANCE_LEAVE` | Command code `14` branch in `GameServerConnection.HandlePlayerStatusInfoAsync` | Enum / Command Mapping | Partial | Regression Tested | Needs Verification | Covers leader leave with online fallback plus disband replay. League and no-fallback behavior remain deferred. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.removePlayer` | `GameServerConnection.HandleAllianceLeaveAsync` plus `PlayerAllianceRuntime.RemoveMemberWithLeaveWorkflow` | Service / Runtime Bridge | Partial | Regression Tested | Needs Verification | The leader branch is covered when the post-leave team reaches Java `shouldDisband`. Defence-team Vortex cleanup remains unmodeled. |
| `com.aionemu.gameserver.model.team.alliance.events.ChangeAllianceLeaderEvent` | `PlayerAllianceRuntime.SelectFallbackLeaderObjectId`, `PlayerAllianceRuntime.ChangeLeader`, and `GameServerConnection.HandleAllianceLeaveAsync` | Event Runtime/Socket Bridge | Partial | Regression Tested | Needs Verification | Leader-change fanout before leave/disband is covered for an online vice-captain fallback. League union messages and Java event-lock ordering remain unverified. |
| `com.aionemu.gameserver.model.team.common.events.ChangeLeaderEvent.changeLeaderToNextAvailablePlayer` | `PlayerAllianceRuntime.SelectFallbackLeaderObjectId` | Event Helper | Partial | Regression Tested | Needs Verification | Online vice-captain fallback is covered before disband. Java `applyOnMembers` iteration and offline/no-fallback cases are not runtime-compared. |
| `com.aionemu.gameserver.model.team.alliance.events.PlayerAllianceLeavedEvent` | `PlayerAllianceRuntime.RemoveMemberWithLeaveWorkflow` / connection alliance leave sends | Event Runtime/Socket Bridge | Partial | Regression Tested | Needs Verification | LEAVE fanout plus DISBAND replay is covered after pre-remove leader change. League broadcast, EventService, instance kick, and live socket ordering remain missing. |
| `com.aionemu.gameserver.model.team.alliance.events.AllianceDisbandEvent` | `PlayerAlliancePacketIntentKind.LeaveGroupMember` plus disband intents in `PlayerAllianceLeavedPlanner` | Event Runtime/Socket Bridge | Partial | Regression Tested | Needs Verification | Java replay of `PlayerAllianceLeavedEvent(DISBAND)` is covered after a leader-leave fallback path. Multi-member explicit disband and Java event queue behavior are not compared. |
| `com.aionemu.gameserver.model.team.common.events.PlayerLeavedEvent` | `Aion.GameServer.Services.PlayerBaseLeavePlanner` and `PlayerAlliancePacketIntentKind.LeaveGroupMember` | Base Event Dependency | Partial | Regression Tested | Needs Verification | Base leave packets are covered for both the disbanded remaining member and old leader. Registered-team instance message/kick and EventService callback remain deferred. |
| `com.aionemu.gameserver.model.team.GeneralTeam.changeLeader` | `PlayerAllianceRuntime.ChangeLeader` | Base Team State | Partial | Regression Tested | Needs Verification | Runtime updates leader id and vice-captain ids before old leader removal. Java object-wrapper mutation and synchronization semantics are not compared. |
| `com.aionemu.gameserver.model.team.GeneralTeam.shouldDisband` | `PlayerAllianceRuntime.RemoveMemberWithLeaveWorkflow` disband predicate | Base Team State | Partial | Regression Tested | Needs Verification | Leader-leave path covers non-auto alliance plus one remaining member after old leader removal. Runtime/threading comparison and unusual offline cases remain unverified. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAlliance` | `Aion.GameServer.Model.GameObjects.PlayerAllianceDescriptor` / `PlayerAllianceRuntime` member store | Team State | Partial | Regression Tested | Needs Verification | Runtime leader id updates before removal, then disband cleanup clears both players and removes runtime dictionaries. Java static alliance registry, group internals, and synchronized event semantics are not compared. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ALLIANCE_INFO` | `Aion.GameServer.Network.Aion.ServerPackets.SmAllianceInfo` through leader-change and leave sends | Server Packet | Partial | Regression Tested | Needs Verification | Covered in leader-change and leave/disband composition. Java golden bytes, encrypted frames, league rows, and real-client validation remain missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ALLIANCE_MEMBER_INFO` | `Aion.GameServer.Network.Aion.ServerPackets.SmAllianceMemberInfo` through alliance leave sends | Server Packet | Partial | Regression Tested | Needs Verification | Covered after the leader-change wave for the remaining member. Java golden bytes and live client validation remain missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_LEAVE_GROUP_MEMBER` | `Aion.GameServer.Network.Aion.ServerPackets.SmLeaveGroupMember` through alliance leave/disband sends | Server Packet | Partial | Regression Tested | Needs Verification | Covered for the disbanded remaining member and the original old leader. Java registered-team instance follow-up remains deferred. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` leader-change, leave, and disband factories | Server Packet Factory | Partial | Regression Tested | Needs Verification | Covered Java ids `1300999`, `1300978`, and `1300201`; `STR_FORCE_HE_IS_NEW_LEADER` is intentionally not sent when `eventPlayer == null`, matching Java source for this path. Golden frame/client validation remains missing. |
| `com.aionemu.gameserver.utils.PacketSendUtility` | `IGameClientConnectionRegistry.SendPacketToPlayerAsync` / direct fallback | Runtime Dependency | Partial | Regression Tested | Needs Verification | Ordered registry sends are covered for the source-derived two-member leader-leave/disband path. Java offline-recipient behavior and live socket ordering remain unverified. |
| `com.aionemu.gameserver.services.VortexService` | Deferred C# defence-team cleanup | Service Dependency | Not Started | No Tests | Unknown | Java calls `VortexService.removeDefenderPlayer` for defence alliances. C# has no live bridge in this handler yet. |
| `com.aionemu.gameserver.model.team.league.League.broadcast` | Deferred leader-change/leave league broadcast metadata | Service Dependency | Not Started | No Tests | Unknown | Java broadcasts league state during leader change and leave/disband. C# still uses non-league planner paths here. |
| `com.aionemu.gameserver.services.event.EventService` | Deferred `PlayerBaseLeavePlanner` metadata | Service Dependency | Not Started | No Tests | Unknown | Java calls `EventService.onLeftTeam` after each base leave. C# records the boundary but has no live EventService bridge here. |
| `com.aionemu.gameserver.services.instance.InstanceService` | Deferred `PlayerBaseLeavePlanner` instance-kick metadata | Service Dependency | Not Started | No Tests | Unknown | Java schedules instance exit movement for registered-team instances. C# does not execute that delayed side effect here. |

## Tests Added

- `GameServerConnectionPlayerStatusInfoTests.HandlePlayerStatusInfoAsync_AllianceLeaveLeaderTwoMemberAllianceDisbandsAfterFallbackLikeJava`
  - Validates parsed command id `14` on a two-member non-auto alliance promotes the online vice captain, sends the leader-change fanout, sends leave fanout, replays disband for the remaining member, clears both memberships, removes the runtime alliance, and sends both base `SmLeaveGroupMember` packets in source-derived order.
  - This test is source-derived. It does not compare against Java runtime execution, Java-generated golden vectors, packet captures, encrypted frames, or a live client.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 21
- Total artifacts ported or partially modeled in this handoff window: 0 new production artifacts; 17 artifacts gained regression coverage in this combined path.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 17
- Total blocked/not-started artifacts: no-online-fallback leader leave, defence-team `VortexService` cleanup, league broadcast/removal, league union messages, `EventService` callback, instance kick scheduling, league commands, Java static service registry, Java event queue/lock comparison, Java runtime/threading comparison, encoded opcode/frame golden validation, packet capture comparison, and real-client validation.
- Estimated overall migration completion: 64%

The percentage stays conservative. Alliance leader-leave/disband ordering is better covered, but edge/league/live validation gaps remain.

## Remaining Risks

- Leader leave with no online fallback currently remains a no-op in the C# connection handler.
- Java league leader-change union messages and league broadcast/removal around alliance leave/disband remain deferred.
- Java defence-team `VortexService.removeDefenderPlayer` cleanup is not wired.
- Java `EventService.onLeftTeam`, registered-team instance kick scheduling, and `STR_MSG_LEAVE_INSTANCE_NOT_PARTY` remain deferred.
- Java invalid target/member exception policy and static alliance registry behavior remain approximated by runtime snapshots.
- Java event queue, lock, `CopyOnWriteArrayList`, alliance group internals, object iteration order, offline-recipient handling, and threading behavior remain source-derived only.
- League commands remain incomplete in `CM_PLAYER_STATUS_INFO`.
- Java golden byte vectors, encrypted opcode/frame validation, packet capture comparison, and real-client validation remain unavailable.

## Next Recommended Unit of Work

Continue `CM_PLAYER_STATUS_INFO` parity in this order:

1. Harden leader leave with no online fallback if Java event-condition/no-op behavior can be documented in a focused test.
2. Otherwise move laterally to the next non-league `CM_PLAYER_STATUS_INFO` gap with smaller missing side effects.
3. Keep league commands deferred until a live league runtime bridge exists.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6EK-Completion.md`
   - this handoff
3. Inspect Java source for the selected branch before touching C#.
4. Implement one narrow unit with Java breadcrumbs.
5. Add focused tests that state what is source-derived and what remains unverified.
6. Run focused tests, then full GameServer tests.
7. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
8. Commit the unit.
