# Phase 6EJ Completion Handoff

Created: May 23, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6EI and covers Session 628.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerStatusInfo|PlayerAllianceRuntime|PlayerAllianceMemberInfo|BaseLeavePlanner"`
  - Result: Passed, 80 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1094 tests.

## Recent Work Completed

### Session 628 - Alliance Leave Two-Member Disband Cascade

- Added focused parsed-command coverage for `ALLIANCE_LEAVE` from a two-member non-auto alliance.
- Confirmed the existing alliance disband replay support now covers the source-derived leave-triggered two-member disband order:
  - remaining leader receives `STR_FORCE_LEAVE_HIM`;
  - remaining leader receives `SM_ALLIANCE_MEMBER_INFO` and `SM_ALLIANCE_INFO`;
  - remaining leader receives `STR_PARTY_ALLIANCE_DISPERSED`;
  - remaining leader receives `SM_LEAVE_GROUP_MEMBER`;
  - original leaver receives their base `SM_LEAVE_GROUP_MEMBER`.
- Confirmed runtime cleanup clears both player memberships and removes the emptied alliance member list.
- Kept Java leader-leave fallback, defence-team `VortexService.removeDefenderPlayer`, league broadcast/removal, `EventService.onLeftTeam`, registered-team instance kick scheduling, Java event queue/lock comparison, socket ordering comparison, Java golden packet vectors, and client validation deferred.
- Commit: this handoff is included in `Cover alliance leave disband cascade`.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_PLAYER_STATUS_INFO` | `Aion.GameServer.Network.Aion.ClientPackets.CmPlayerStatusInfo` / `Aion.GameServer.Network.Aion.GameServerConnection.HandlePlayerStatusInfoAsync` | Client Packet / Handler Boundary | Partial | Regression Tested | Needs Verification | Command id `14` has explicit two-member alliance leave/disband packet-order coverage. Leader leave, league commands, and full generic dispatch remain missing. |
| `com.aionemu.gameserver.model.team.common.events.TeamCommand.ALLIANCE_LEAVE` | Command code `14` branch in `GameServerConnection.HandlePlayerStatusInfoAsync` | Enum / Command Mapping | Partial | Regression Tested | Needs Verification | Two-member non-auto alliance leave/disband ordering is covered. Leader leave currently no-ops pending Java `ChangeAllianceLeaderEvent` before-remove parity. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.removePlayer` | `GameServerConnection.HandleAllianceLeaveAsync` plus `PlayerAllianceRuntime.RemoveMemberWithLeaveWorkflow` | Service / Runtime Bridge | Partial | Regression Tested | Needs Verification | Existing non-leader leave path now has regression coverage when the leave causes Java `shouldDisband`. Defence-team Vortex cleanup and leader-change branch remain unmodeled. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.disband` | `PlayerAllianceLeavedPlanner` disband replay intents plus `PlayerAllianceRuntime.RemoveMemberWithLeaveWorkflow` cleanup | Service / Runtime Bridge | Partial | Regression Tested | Needs Verification | C# models the two-member disband cascade for a leave path. Explicit disband entry points, recruitment removal, and league-before/after behavior remain deferred. |
| `com.aionemu.gameserver.model.team.alliance.events.PlayerAllianceLeavedEvent` | `PlayerAllianceRuntime.RemoveMemberWithLeaveWorkflow` / connection alliance leave sends | Event Runtime/Socket Bridge | Partial | Regression Tested | Needs Verification | LEAVE fanout and DISBAND replay order are covered together. Leader-change, league broadcast, EventService, instance kick, and live socket ordering remain missing. |
| `com.aionemu.gameserver.model.team.alliance.events.AllianceDisbandEvent` | `PlayerAlliancePacketIntentKind.LeaveGroupMember` plus disband intents in `PlayerAllianceLeavedPlanner` | Event Runtime/Socket Bridge | Partial | Regression Tested | Needs Verification | Java replay of `PlayerAllianceLeavedEvent(DISBAND)` is source-modeled for the last remaining alliance member. Multi-member explicit disband and Java event queue behavior are not compared. |
| `com.aionemu.gameserver.model.team.common.events.PlayerLeavedEvent` | `Aion.GameServer.Services.PlayerBaseLeavePlanner` and `PlayerAlliancePacketIntentKind.LeaveGroupMember` | Base Event Dependency | Partial | Regression Tested | Needs Verification | Base leave packets are covered for both the disbanded remaining member and the original leaver. Registered-team instance message/kick and EventService callback remain deferred. |
| `com.aionemu.gameserver.model.team.GeneralTeam.shouldDisband` | `PlayerAllianceRuntime.RemoveMemberWithLeaveWorkflow` disband predicate | Base Team State | Partial | Regression Tested | Needs Verification | Leave path explicitly covers non-auto alliance plus one remaining member. Runtime/threading comparison and unusual offline cases remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ALLIANCE_MEMBER_INFO` | `Aion.GameServer.Network.Aion.ServerPackets.SmAllianceMemberInfo` through alliance leave sends | Server Packet | Partial | Regression Tested | Needs Verification | Covered before leave/disband messages. Java golden bytes, encrypted frames, and real-client validation remain missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ALLIANCE_INFO` | `Aion.GameServer.Network.Aion.ServerPackets.SmAllianceInfo` through alliance leave sends | Server Packet | Partial | Regression Tested | Needs Verification | Covered before disband replay. Java golden bytes, league rows, encrypted frames, and real-client validation remain missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_LEAVE_GROUP_MEMBER` | `Aion.GameServer.Network.Aion.ServerPackets.SmLeaveGroupMember` through alliance leave/disband sends | Server Packet | Partial | Regression Tested | Needs Verification | Covered for the remaining member during disband and for the original leaver after alliance fanout. Java registered-team instance follow-up remains deferred. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` alliance-leave/disband factories | Server Packet Factory | Partial | Regression Tested | Needs Verification | Covered Java ids `1300978` and `1300201` in combined leave/disband order. Golden frame/client validation remains missing. |
| `com.aionemu.gameserver.utils.PacketSendUtility` | `IGameClientConnectionRegistry.SendPacketToPlayerAsync` / direct fallback | Runtime Dependency | Partial | Regression Tested | Needs Verification | Ordered registry sends are covered for the source-derived two-member alliance leave/disband path. Java offline-recipient behavior and live socket ordering remain unverified. |
| `com.aionemu.gameserver.services.VortexService` | Deferred C# defence-team cleanup | Service Dependency | Not Started | No Tests | Unknown | Java calls `VortexService.removeDefenderPlayer` for defence alliances. C# has no live bridge in this handler yet. |
| `com.aionemu.gameserver.model.team.league.League.broadcast` | Deferred `PlayerAllianceLeavedPlan.WouldBroadcastLeague` metadata | Service Dependency | Not Started | No Tests | Unknown | Java broadcasts/removes league alliance state around disband. C# records no live league membership for this path yet. |
| `com.aionemu.gameserver.services.event.EventService` | Deferred `PlayerBaseLeavePlanner` metadata | Service Dependency | Not Started | No Tests | Unknown | Java calls `EventService.onLeftTeam` after each base leave. C# records the boundary but has no live EventService bridge here. |
| `com.aionemu.gameserver.services.instance.InstanceService` | Deferred `PlayerBaseLeavePlanner` instance-kick metadata | Service Dependency | Not Started | No Tests | Unknown | Java schedules instance exit movement for registered-team instances. C# does not execute that delayed side effect here. |

## Tests Added

- `GameServerConnectionPlayerStatusInfoTests.HandlePlayerStatusInfoAsync_AllianceLeaveTwoMemberAllianceDisbandsBeforeLeaverBaseLeaveLikeJava`
  - Validates parsed command id `14` on a two-member non-auto alliance clears both memberships, removes the runtime alliance, sends `STR_FORCE_LEAVE_HIM`, `SmAllianceMemberInfo`, `SmAllianceInfo`, alliance-dispersed id `1300201`, remaining member `SmLeaveGroupMember`, and original-leaver `SmLeaveGroupMember` in source-derived order.
  - This test is source-derived. It does not compare against Java runtime execution, Java-generated golden vectors, packet captures, encrypted frames, or a live client.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 18
- Total artifacts ported or partially modeled in this handoff window: 14
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 14
- Total blocked/not-started artifacts: alliance leader leave/disband, defence-team `VortexService` cleanup, league broadcast/removal, `EventService` callback, instance kick scheduling, league commands, Java static service registry, Java event queue/lock comparison, Java runtime/threading comparison, encoded opcode/frame golden validation, packet capture comparison, and real-client validation.
- Estimated overall migration completion: 64%

The percentage stays conservative. Alliance leave disband order is better covered, but leader/league/live validation gaps remain.

## Remaining Risks

- Alliance leader leave/disband behavior remains incomplete.
- Java defence-team `VortexService.removeDefenderPlayer` cleanup is not wired.
- Java league broadcast/removal around alliance leave disband remains deferred.
- Java `EventService.onLeftTeam`, registered-team instance kick scheduling, and `STR_MSG_LEAVE_INSTANCE_NOT_PARTY` remain deferred.
- Java invalid target/member exception policy and static alliance registry behavior remain approximated by runtime snapshots.
- Java event queue, lock, `CopyOnWriteArrayList`, alliance group internals, object iteration order, offline-recipient handling, and threading behavior remain source-derived only.
- League commands remain incomplete in `CM_PLAYER_STATUS_INFO`.
- Java golden byte vectors, encrypted opcode/frame validation, packet capture comparison, and real-client validation remain unavailable.

## Next Recommended Unit of Work

Continue `CM_PLAYER_STATUS_INFO` parity in this order:

1. Model alliance leader-leave fallback only after carefully source-reading Java's pre-remove `ChangeAllianceLeaderEvent` and its packet order.
2. If leader leave is still too broad, move laterally to the next non-league `CM_PLAYER_STATUS_INFO` gap with smaller missing side effects.
3. Keep league commands deferred until a live league runtime bridge exists.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6EI-Completion.md`
   - this handoff
3. Inspect Java source for the selected branch before touching C#.
4. Implement one narrow unit with Java breadcrumbs.
5. Add focused tests that state what is source-derived and what remains unverified.
6. Run focused tests, then full GameServer tests.
7. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
8. Commit the unit.
