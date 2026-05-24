# Phase 6EM Completion Handoff

Created: May 23, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6EL and covers Session 631.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerStatusInfo|PlayerAllianceRuntime|PlayerAllianceMemberInfo|BaseLeavePlanner"`
  - Result: Passed, 83 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1097 tests.

## Recent Work Completed

### Session 631 - Alliance Leader Leave No-Online-Fallback Disband

- Source-read Java `ChangeAllianceLeaderEvent`, `ChangeLeaderEvent.checkCondition`, `PlayerAllianceLeavedEvent`, `AllianceDisbandEvent`, base `PlayerLeavedEvent`, `PacketSendUtility.sendPacket`, and `GeneralTeam.removeMember`.
- Updated `GameServerConnection.HandleAllianceLeaveAsync` so a leader leave with no online fallback still proceeds through removal when the alliance has exactly two members and therefore disbands after the leader leaves.
- Kept larger no-online-fallback alliances deferred because Java can remove the old leader while retaining the old leader reference until a later event, which needs a more careful C# representation than assigning an offline fallback leader.
- Extended the status-info test registry to simulate Java offline-recipient packet sends returning no delivery.
- Added focused parsed-command coverage for a two-member non-auto alliance where the online leader leaves and the remaining offline member causes no leader-change packets, receives no packets, and the alliance disbands before the old leader's base leave packet.
- Commit: this handoff is included in `Handle alliance leader leave no fallback disband`.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_PLAYER_STATUS_INFO` | `Aion.GameServer.Network.Aion.ClientPackets.CmPlayerStatusInfo` / `Aion.GameServer.Network.Aion.GameServerConnection.HandlePlayerStatusInfoAsync` | Client Packet / Handler Boundary | Partial | Regression Tested | Needs Verification | Command id `14` covers the bounded two-member no-online-fallback disband case. Multi-member no-online-fallback, league commands, and full generic dispatch remain missing. |
| `com.aionemu.gameserver.model.team.common.events.TeamCommand.ALLIANCE_LEAVE` | Command code `14` branch in `GameServerConnection.HandlePlayerStatusInfoAsync` | Enum / Command Mapping | Partial | Regression Tested | Needs Verification | Leader leave with no fallback proceeds only when the post-leave alliance disbands. Larger no-fallback behavior remains deferred due Java stale-leader semantics. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.removePlayer` | `GameServerConnection.HandleAllianceLeaveAsync` plus `PlayerAllianceRuntime.RemoveMemberWithLeaveWorkflow` | Service / Runtime Bridge | Partial | Regression Tested | Needs Verification | The no-fallback two-member branch removes the leader and disbands. Defence-team Vortex cleanup remains unmodeled. |
| `com.aionemu.gameserver.model.team.alliance.events.ChangeAllianceLeaderEvent` | `PlayerAllianceRuntime.SelectFallbackLeaderObjectId` and bounded no-fallback branch in `GameServerConnection.HandleAllianceLeaveAsync` | Event Runtime/Socket Bridge | Partial | Regression Tested | Needs Verification | C# models no leader-change packet output when no online vice captain or online member exists, but only for the disbanding two-member case. Multi-member stale leader-reference behavior remains deferred. |
| `com.aionemu.gameserver.model.team.common.events.ChangeLeaderEvent.checkCondition` | `PlayerAllianceRuntime.SelectFallbackLeaderObjectId` / connection branch gating | Event Helper | Partial | Regression Tested | Needs Verification | Java allows `eventPlayer == null`; C# follows the resulting no-fallback path for the disbanding case. Full `applyOnMembers` iteration behavior is not runtime-compared. |
| `com.aionemu.gameserver.model.team.alliance.events.PlayerAllianceLeavedEvent` | `PlayerAllianceRuntime.RemoveMemberWithLeaveWorkflow` / connection alliance leave sends | Event Runtime/Socket Bridge | Partial | Regression Tested | Needs Verification | LEAVE fanout is planned after old leader removal; offline remaining recipient sends are skipped by the registry simulation and disband replay follows. League broadcast, EventService, instance kick, and live socket ordering remain missing. |
| `com.aionemu.gameserver.model.team.alliance.events.AllianceDisbandEvent` | `PlayerAlliancePacketIntentKind.LeaveGroupMember` plus disband intents in `PlayerAllianceLeavedPlanner` | Event Runtime/Socket Bridge | Partial | Regression Tested | Needs Verification | Disband cleanup is covered when the remaining member is offline; packet delivery is skipped by registry no-op simulation. Java event queue behavior is not compared. |
| `com.aionemu.gameserver.model.team.common.events.PlayerLeavedEvent` | `Aion.GameServer.Services.PlayerBaseLeavePlanner` and `PlayerAlliancePacketIntentKind.LeaveGroupMember` | Base Event Dependency | Partial | Regression Tested | Needs Verification | Old online leader receives base `SM_LEAVE_GROUP_MEMBER`; offline remaining member receives no base packet. Registered-team instance message/kick and EventService callback remain deferred. |
| `com.aionemu.gameserver.model.team.GeneralTeam.removeMember` | `PlayerAllianceRuntime.RemoveMemberWithLeaveWorkflow` | Base Team State | Partial | Regression Tested | Needs Verification | Runtime removal/disband clears both memberships and removes dictionaries for the two-member no-fallback path. Java stale leader reference for larger alliances remains intentionally deferred. |
| `com.aionemu.gameserver.model.team.GeneralTeam.shouldDisband` | `PlayerAllianceRuntime.RemoveMemberWithLeaveWorkflow` disband predicate | Base Team State | Partial | Regression Tested | Needs Verification | No-online-fallback leader leave covers non-auto alliance plus one remaining member after removal. Runtime/threading comparison remains unverified. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAlliance` | `Aion.GameServer.Model.GameObjects.PlayerAllianceDescriptor` / `PlayerAllianceRuntime` member store | Team State | Partial | Regression Tested | Needs Verification | Runtime clears both players and removes alliance dictionaries after disband. Java static alliance registry, group internals, and synchronized event semantics are not compared. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_LEAVE_GROUP_MEMBER` | `Aion.GameServer.Network.Aion.ServerPackets.SmLeaveGroupMember` through alliance leave/disband sends | Server Packet | Partial | Regression Tested | Needs Verification | Covered for the old online leader only; offline remaining member sends are simulated as undelivered like Java `PacketSendUtility`. Java registered-team instance follow-up remains deferred. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` leave/disband factories | Server Packet Factory | Partial | Regression Tested | Needs Verification | `STR_FORCE_LEAVE_HIM` and `STR_PARTY_ALLIANCE_DISPERSED` are planned for the offline remaining member but not delivered by registry simulation. Golden frame/client validation remains missing. |
| `com.aionemu.gameserver.utils.PacketSendUtility` | `IGameClientConnectionRegistry.SendPacketToPlayerAsync` / test registry unavailable-recipient simulation | Runtime Dependency | Partial | Regression Tested | Needs Verification | Test registry can model Java `if (player.isOnline()) sendPacket`; production registry behavior still depends on live connection presence rather than direct `Player.IsOnline` checks. |
| `com.aionemu.gameserver.services.VortexService` | Deferred C# defence-team cleanup | Service Dependency | Not Started | No Tests | Unknown | Java calls `VortexService.removeDefenderPlayer` for defence alliances. C# has no live bridge in this handler yet. |
| `com.aionemu.gameserver.model.team.league.League.broadcast` | Deferred leader-change/leave league broadcast metadata | Service Dependency | Not Started | No Tests | Unknown | Java broadcasts league state during leader change and leave/disband. C# still uses non-league planner paths here. |
| `com.aionemu.gameserver.services.event.EventService` | Deferred `PlayerBaseLeavePlanner` metadata | Service Dependency | Not Started | No Tests | Unknown | Java calls `EventService.onLeftTeam` after each base leave. C# records the boundary but has no live EventService bridge here. |
| `com.aionemu.gameserver.services.instance.InstanceService` | Deferred `PlayerBaseLeavePlanner` instance-kick metadata | Service Dependency | Not Started | No Tests | Unknown | Java schedules instance exit movement for registered-team instances. C# does not execute that delayed side effect here. |

## Tests Added

- `GameServerConnectionPlayerStatusInfoTests.HandlePlayerStatusInfoAsync_AllianceLeaveLeaderTwoMemberNoOnlineFallbackDisbandsLikeJava`
  - Validates parsed command id `14` on a two-member non-auto alliance where the online leader leaves, the remaining member is offline, no leader-change packets are sent, the alliance disbands, both memberships clear, and only the old leader receives base `SmLeaveGroupMember`.
  - This test is source-derived. It does not compare against Java runtime execution, Java-generated golden vectors, packet captures, encrypted frames, or a live client.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 19
- Total artifacts ported or partially modeled in this handoff window: 15
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 15
- Total blocked/not-started artifacts: multi-member no-online-fallback stale-leader behavior, defence-team `VortexService` cleanup, league broadcast/removal, league union messages, `EventService` callback, instance kick scheduling, league commands, Java static service registry, Java event queue/lock comparison, Java runtime/threading comparison, encoded opcode/frame golden validation, packet capture comparison, and real-client validation.
- Estimated overall migration completion: 64%

The percentage stays conservative. One more alliance leader-leave edge is modeled, but multi-member/league/live validation gaps remain.

## Remaining Risks

- Multi-member leader leave with no online fallback remains deferred because Java may remove the old leader while retaining the old leader reference until a later event.
- Java league leader-change union messages and league broadcast/removal around alliance leave/disband remain deferred.
- Java defence-team `VortexService.removeDefenderPlayer` cleanup is not wired.
- Java `EventService.onLeftTeam`, registered-team instance kick scheduling, and `STR_MSG_LEAVE_INSTANCE_NOT_PARTY` remain deferred.
- Java invalid target/member exception policy and static alliance registry behavior remain approximated by runtime snapshots.
- Java event queue, lock, `CopyOnWriteArrayList`, alliance group internals, object iteration order, offline-recipient handling, and threading behavior remain source-derived only.
- League commands remain incomplete in `CM_PLAYER_STATUS_INFO`.
- Java golden byte vectors, encrypted opcode/frame validation, packet capture comparison, and real-client validation remain unavailable.

## Next Recommended Unit of Work

Continue `CM_PLAYER_STATUS_INFO` parity in this order:

1. Move laterally to the next non-league `CM_PLAYER_STATUS_INFO` gap with smaller missing side effects.
2. Alternatively source-read whether multi-member no-online-fallback leader leave can be represented without corrupting C# leader state.
3. Keep league commands deferred until a live league runtime bridge exists.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6EL-Completion.md`
   - this handoff
3. Inspect Java source for the selected branch before touching C#.
4. Implement one narrow unit with Java breadcrumbs.
5. Add focused tests that state what is source-derived and what remains unverified.
6. Run focused tests, then full GameServer tests.
7. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
8. Commit the unit.
