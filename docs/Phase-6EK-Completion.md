# Phase 6EK Completion Handoff

Created: May 23, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6EJ and covers Session 629.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerStatusInfo|PlayerAllianceRuntime|PlayerAllianceMemberInfo|BaseLeavePlanner"`
  - Result: Passed, 81 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1095 tests.

## Recent Work Completed

### Session 629 - Alliance Leader Leave Fallback

- Source-read Java `TeamCommand.ALLIANCE_LEAVE`, `PlayerAllianceService.removePlayer`, `PlayerAllianceLeavedEvent`, `ChangeAllianceLeaderEvent`, and `ChangeLeaderEvent`.
- Added `PlayerAllianceRuntime.SelectFallbackLeaderObjectId` to model Java leader-leave fallback selection:
  - first online vice captain in vice-captain id order;
  - otherwise first online non-leader member in alliance member order.
- Updated `GameServerConnection.HandleAllianceLeaveAsync` so a leader using command id `14` executes the non-league leader-change packet plan before the existing alliance leave workflow.
- Added focused parsed-command coverage for a three-member non-auto alliance where the leader leaves and an online vice captain becomes the new leader before leave fanout.
- Commit: this handoff is included in `Handle alliance leader leave fallback`.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_PLAYER_STATUS_INFO` | `Aion.GameServer.Network.Aion.ClientPackets.CmPlayerStatusInfo` / `Aion.GameServer.Network.Aion.GameServerConnection.HandlePlayerStatusInfoAsync` | Client Packet / Handler Boundary | Partial | Regression Tested | Needs Verification | Command id `14` covers a non-disband leader-leave fallback path. No-online-fallback, two-member leader-leave disband, league commands, and full generic dispatch remain missing. |
| `com.aionemu.gameserver.model.team.common.events.TeamCommand.ALLIANCE_LEAVE` | Command code `14` branch in `GameServerConnection.HandlePlayerStatusInfoAsync` | Enum / Command Mapping | Partial | Regression Tested | Needs Verification | Leader leave composes a non-league leader-change wave before removal when an online fallback exists. League and no-fallback behavior remain deferred. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.removePlayer` | `GameServerConnection.HandleAllianceLeaveAsync` plus `PlayerAllianceRuntime.RemoveMemberWithLeaveWorkflow` | Service / Runtime Bridge | Partial | Regression Tested | Needs Verification | The leader branch models Java's pre-remove `ChangeAllianceLeaderEvent` for an online vice-captain fallback. Defence-team Vortex cleanup remains unmodeled. |
| `com.aionemu.gameserver.model.team.alliance.events.ChangeAllianceLeaderEvent` | `PlayerAllianceRuntime.SelectFallbackLeaderObjectId`, `PlayerAllianceRuntime.ChangeLeader`, and `GameServerConnection.HandleAllianceLeaveAsync` | Event Runtime/Socket Bridge | Partial | Regression Tested | Needs Verification | C# selects an online vice captain before the next online member and sends non-league alliance-info plus `STR_FORCE_YOU_BECOME_NEW_LEADER` before leave fanout. League union messages and Java event-lock ordering remain unverified. |
| `com.aionemu.gameserver.model.team.common.events.ChangeLeaderEvent.changeLeaderToNextAvailablePlayer` | `PlayerAllianceRuntime.SelectFallbackLeaderObjectId` | Event Helper | Partial | Regression Tested | Needs Verification | Fallback scan is source-modeled for online vice-captain preference and next online non-leader fallback. Java `applyOnMembers` iteration and offline edge cases are not runtime-compared. |
| `com.aionemu.gameserver.model.team.GeneralTeam.changeLeader` | `PlayerAllianceRuntime.ChangeLeader` | Base Team State | Partial | Regression Tested | Needs Verification | Runtime updates alliance leader id and removes the new leader from vice-captain ids before leave removal. Java object-wrapper mutation and synchronization semantics are not compared. |
| `com.aionemu.gameserver.model.team.alliance.events.PlayerAllianceLeavedEvent` | `PlayerAllianceRuntime.RemoveMemberWithLeaveWorkflow` / connection alliance leave sends | Event Runtime/Socket Bridge | Partial | Regression Tested | Needs Verification | Leader-change wave is sent before LEAVE reason fanout for a three-member non-disband path. Disband combination, league broadcast, EventService, instance kick, and live socket ordering remain missing. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAlliance` | `Aion.GameServer.Model.GameObjects.PlayerAllianceDescriptor` / `PlayerAllianceRuntime` member store | Team State | Partial | Regression Tested | Needs Verification | Runtime leader id and vice-captain ids are updated before the old leader is removed. Java static alliance registry, group internals, and synchronized event semantics are not compared. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ALLIANCE_INFO` | `Aion.GameServer.Network.Aion.ServerPackets.SmAllianceInfo` through leader-change and leave sends | Server Packet | Partial | Regression Tested | Needs Verification | Covered in the leader-change wave and subsequent leave fanout. Java golden bytes, encrypted frames, league rows, and real-client validation remain missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ALLIANCE_MEMBER_INFO` | `Aion.GameServer.Network.Aion.ServerPackets.SmAllianceMemberInfo` through alliance leave sends | Server Packet | Partial | Regression Tested | Needs Verification | Covered after the leader-change wave for each remaining member. Java golden bytes and live client validation remain missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_LEAVE_GROUP_MEMBER` | `Aion.GameServer.Network.Aion.ServerPackets.SmLeaveGroupMember` through alliance leave sends | Server Packet | Partial | Regression Tested | Needs Verification | Covered for the old leader's base leave after alliance fanout. Java registered-team instance follow-up remains deferred. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` leader-change and leave factories | Server Packet Factory | Partial | Regression Tested | Needs Verification | Covered Java ids `1300999` and `1300978`; `STR_FORCE_HE_IS_NEW_LEADER` is intentionally not sent when `eventPlayer == null`, matching Java source for this path. Golden frame/client validation remains missing. |
| `com.aionemu.gameserver.utils.PacketSendUtility` | `IGameClientConnectionRegistry.SendPacketToPlayerAsync` / direct fallback | Runtime Dependency | Partial | Regression Tested | Needs Verification | Ordered registry sends are covered for the source-derived leader-leave fallback path. Java offline-recipient behavior and live socket ordering remain unverified. |
| `com.aionemu.gameserver.services.VortexService` | Deferred C# defence-team cleanup | Service Dependency | Not Started | No Tests | Unknown | Java calls `VortexService.removeDefenderPlayer` for defence alliances. C# has no live bridge in this handler yet. |
| `com.aionemu.gameserver.model.team.league.League.broadcast` | Deferred leader-change/leave league broadcast metadata | Service Dependency | Not Started | No Tests | Unknown | Java broadcasts league state during leader change and leave. C# still uses non-league planner paths here. |
| `com.aionemu.gameserver.services.event.EventService` | Deferred `PlayerBaseLeavePlanner` metadata | Service Dependency | Not Started | No Tests | Unknown | Java calls `EventService.onLeftTeam` after base leave. C# records the boundary but has no live EventService bridge here. |
| `com.aionemu.gameserver.services.instance.InstanceService` | Deferred `PlayerBaseLeavePlanner` instance-kick metadata | Service Dependency | Not Started | No Tests | Unknown | Java schedules instance exit movement for registered-team instances. C# does not execute that delayed side effect here. |

## Tests Added

- `GameServerConnectionPlayerStatusInfoTests.HandlePlayerStatusInfoAsync_AllianceLeaveLeaderPromotesOnlineViceCaptainBeforeLeaveFanoutLikeJava`
  - Validates parsed command id `14` on a three-member non-auto alliance promotes the online vice captain, removes that vice-captain role, sends leader-change alliance-info packets and `STR_FORCE_YOU_BECOME_NEW_LEADER`, then sends the leave fanout and old-leader base leave.
  - This test is source-derived. It does not compare against Java runtime execution, Java-generated golden vectors, packet captures, encrypted frames, or a live client.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 18
- Total artifacts ported or partially modeled in this handoff window: 14
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 14
- Total blocked/not-started artifacts: no-online-fallback leader leave, two-member leader-leave disband combination, defence-team `VortexService` cleanup, league broadcast/removal, league union messages, `EventService` callback, instance kick scheduling, league commands, Java static service registry, Java event queue/lock comparison, Java runtime/threading comparison, encoded opcode/frame golden validation, packet capture comparison, and real-client validation.
- Estimated overall migration completion: 64%

The percentage stays conservative. Alliance leader leave is now partially modeled for the common non-league fallback case, but edge/league/live validation gaps remain.

## Remaining Risks

- Leader leave with no online fallback currently remains a no-op in the C# connection handler.
- Two-member leader leave plus disband replay is not explicitly covered as a combined branch.
- Java league leader-change union messages and league broadcast/removal around alliance leave remain deferred.
- Java defence-team `VortexService.removeDefenderPlayer` cleanup is not wired.
- Java `EventService.onLeftTeam`, registered-team instance kick scheduling, and `STR_MSG_LEAVE_INSTANCE_NOT_PARTY` remain deferred.
- Java invalid target/member exception policy and static alliance registry behavior remain approximated by runtime snapshots.
- Java event queue, lock, `CopyOnWriteArrayList`, alliance group internals, object iteration order, offline-recipient handling, and threading behavior remain source-derived only.
- League commands remain incomplete in `CM_PLAYER_STATUS_INFO`.
- Java golden byte vectors, encrypted opcode/frame validation, packet capture comparison, and real-client validation remain unavailable.

## Next Recommended Unit of Work

Continue `CM_PLAYER_STATUS_INFO` parity in this order:

1. Add a focused two-member leader-leave/disband regression only if the existing leader-change plus disband replay ordering can be documented accurately.
2. Otherwise move laterally to the next non-league `CM_PLAYER_STATUS_INFO` gap or harden no-online-fallback behavior.
3. Keep league commands deferred until a live league runtime bridge exists.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6EJ-Completion.md`
   - this handoff
3. Inspect Java source for the selected branch before touching C#.
4. Implement one narrow unit with Java breadcrumbs.
5. Add focused tests that state what is source-derived and what remains unverified.
6. Run focused tests, then full GameServer tests.
7. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
8. Commit the unit.
