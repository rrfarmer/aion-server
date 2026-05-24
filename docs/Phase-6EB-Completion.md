# Phase 6EB Completion Handoff

Created: May 23, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6EA and covers Sessions 617-619.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerStatusInfo|LeaderChangePlanner|ViceCaptainAssignment"`
  - Result: Passed, 19 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1083 tests.

## Recent Work Completed

### Session 617 - Alliance Vice-Captain Status Commands

- Source-read Java `TeamCommand`, `PlayerTeamCommandService.executeCommand`, `PlayerAllianceService.changeViceCaptain`, and `AssignViceCaptainEvent`.
- Added `PlayerAllianceRuntime.AssignViceCaptain` and wired `CM_PLAYER_STATUS_INFO` command ids `25` and `26`.
- Sends `SM_ALLIANCE_INFO` packets for promote/demote and `STR_FORCE_CANNOT_PROMOTE_MANAGER` when the Java four-vice-captain limit is reached.
- Commit: `cb85b99c2 Wire alliance vice captain status commands`

### Session 618 - Group Leader Status Command

- Source-read Java `TeamCommand.GROUP_SET_LEADER`, `PlayerTeamCommandService.executeCommand`, `PlayerGroupService.changeLeader`, and `ChangeGroupLeaderEvent`.
- Added group leader system-message factories for Java ids `1300154` and `1300155`.
- Added `PlayerGroupRuntime.ChangeLeader` and wired `CM_PLAYER_STATUS_INFO` command id `3`.
- Sends `SM_GROUP_INFO` followed by the matching group-leader system message per member, preserving Java per-member order.
- Commit: `d58d5b384 Wire group leader status command`

### Session 619 - Alliance Leader Status Command

- Source-read Java `TeamCommand.ALLIANCE_SET_CAPTAIN`, `PlayerTeamCommandService.executeCommand`, `PlayerAllianceService.changeLeader`, `ChangeAllianceLeaderEvent`, and the old-leader vice-captain follow-up.
- Added `PlayerAllianceRuntime.ChangeLeader` and wired `CM_PLAYER_STATUS_INFO` command id `17`.
- Sends the first `ChangeAllianceLeaderEvent` wave of `SM_ALLIANCE_INFO` plus leader system messages, then applies `DEMOTE_CAPTAIN_TO_VICECAPTAIN` for the old leader and sends the second `SM_ALLIANCE_INFO` wave.
- Commit: `6de2ad08c Wire alliance leader status command`

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_PLAYER_STATUS_INFO` | `Aion.GameServer.Network.Aion.ClientPackets.CmPlayerStatusInfo` / `Aion.GameServer.Network.Aion.GameServerConnection.HandlePlayerStatusInfoAsync` | Client Packet / Handler Boundary | Partial | Regression Tested | Needs Verification | Parser and more team command branches are wired: group leader, group LFG, group mentoring, alliance ready checks, alliance leader, alliance vice-captain, and alliance group change. Group ban/remove, alliance leave/ban, league commands, and full generic dispatch remain missing. |
| `com.aionemu.gameserver.model.team.common.events.TeamCommand.ALLIANCE_SET_VICECAPTAIN` | Command code `25` branch in `GameServerConnection.HandlePlayerStatusInfoAsync` | Enum / Command Mapping | Partial | Regression Tested | Needs Verification | Promotes selected member or caller when target id is zero. Java invalid-member exception behavior and league broadcast remain deferred. |
| `com.aionemu.gameserver.model.team.common.events.TeamCommand.ALLIANCE_UNSET_VICECAPTAIN` | Command code `26` branch in `GameServerConnection.HandlePlayerStatusInfoAsync` | Enum / Command Mapping | Partial | Regression Tested | Needs Verification | Demotes selected member or caller when target id is zero. Java invalid-member exception behavior and league broadcast remain deferred. |
| `com.aionemu.gameserver.model.team.common.events.TeamCommand.GROUP_SET_LEADER` | Command code `3` branch in `GameServerConnection.HandlePlayerStatusInfoAsync` | Enum / Command Mapping | Partial | Regression Tested | Needs Verification | Changes runtime group leader and sends per-member `SM_GROUP_INFO` plus Java group-leader system messages. Java invalid-member exception behavior remains deferred. |
| `com.aionemu.gameserver.model.team.common.events.TeamCommand.ALLIANCE_SET_CAPTAIN` | Command code `17` branch in `GameServerConnection.HandlePlayerStatusInfoAsync` | Enum / Command Mapping | Partial | Regression Tested | Needs Verification | Changes runtime alliance leader and applies old-leader vice-captain demotion follow-up. Java league broadcast and union timeout messages remain missing. |
| `com.aionemu.gameserver.model.team.common.service.PlayerTeamCommandService` | Narrow branches in `GameServerConnection.HandlePlayerStatusInfoAsync` | Service Dependency | Partial | Regression Tested | Needs Verification | C# still bypasses full generic team-command dispatch. Group ban/remove, alliance leave/ban, league commands, and Java exception policy remain incomplete. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.changeViceCaptain` | `Aion.GameServer.Services.PlayerAllianceRuntime.AssignViceCaptain` | Service / Runtime Bridge | Partial | Regression Tested | Needs Verification | Runtime applies existing planner results and refreshes snapshots. Java static registry, `alliance.onEvent`, `CopyOnWriteArrayList`, and league broadcast remain unverified. |
| `com.aionemu.gameserver.model.team.alliance.events.AssignViceCaptainEvent` | `PlayerAllianceViceCaptainAssignmentPlanner` / `PlayerAllianceRuntime.AssignViceCaptain` | Event Runtime/Socket Bridge | Partial | Regression Tested | Needs Verification | Promote/demote and old-leader demotion behaviors are modeled. Java event queue/lock ordering, league broadcast, and live socket ordering remain unverified. |
| `com.aionemu.gameserver.model.team.group.PlayerGroupService.changeLeader` | `Aion.GameServer.Services.PlayerGroupRuntime.ChangeLeader` | Service / Runtime Bridge | Partial | Regression Tested | Needs Verification | Runtime updates group descriptor leader id and emits packet intents. Java static group registry and event wrapper are not runtime-compared. |
| `com.aionemu.gameserver.model.team.group.events.ChangeGroupLeaderEvent` | `PlayerGroupRuntime.ChangeLeader` / connection group leader sends | Event Runtime/Socket Bridge | Partial | Regression Tested | Needs Verification | C# emits `SM_GROUP_INFO` then system message per member. Java live socket ordering and lock semantics remain unverified. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.changeLeader` | `Aion.GameServer.Services.PlayerAllianceRuntime.ChangeLeader` | Service / Runtime Bridge | Partial | Regression Tested | Needs Verification | Runtime updates alliance descriptor leader id, removes new leader from vice-captains, and refreshes snapshots. Java static registry and event wrapper are not runtime-compared. |
| `com.aionemu.gameserver.model.team.alliance.events.ChangeAllianceLeaderEvent` | `PlayerAllianceRuntime.ChangeLeader` / connection alliance leader sends | Event Runtime/Socket Bridge | Partial | Regression Tested | Needs Verification | C# sends non-league alliance-info and leader system messages. Java league path remains missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_GROUP_INFO` | `Aion.GameServer.Network.Aion.ServerPackets.SmGroupInfo` | Server Packet | Partial | Regression Tested | Needs Verification | Now reachable from parsed group leader command id `3`. Java golden bytes, encrypted frames, and real-client validation remain missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ALLIANCE_INFO` | `Aion.GameServer.Network.Aion.ServerPackets.SmAllianceInfo` | Server Packet | Partial | Regression Tested | Needs Verification | Now reachable from parsed alliance vice-captain and leader command ids. Java golden bytes, encrypted frames, league rows, and real-client validation remain missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` | Server Packet Factory | Partial | Regression Tested | Needs Verification | New/used Java ids include group leader `1300154`/`1300155`, alliance leader `1300998`/`1300999`, and vice-captain limit `1301061`. Golden frame/client validation remains missing. |
| `com.aionemu.gameserver.model.team.league.League.broadcast` | Deferred C# league broadcast side effects | Service Dependency | Not Started | No Tests | Unknown | Java broadcasts league updates for alliance leader/vice-captain paths and may send union timeout messages. C# runtime has no live league bridge for this packet handler yet. |
| `com.aionemu.gameserver.utils.PacketSendUtility` | `IGameClientConnectionRegistry.SendPacketToPlayerAsync` / direct fallback | Runtime Dependency | Partial | Regression Tested | Needs Verification | C# sends the newly wired packets through registry/direct fallback. Java offline-recipient behavior and live socket ordering remain unverified. |

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 17
- Total artifacts ported or partially modeled in this handoff window: 16
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 16
- Total blocked/not-started artifacts: remaining `CM_PLAYER_STATUS_INFO` group ban/remove, alliance leave/ban, league commands, Java static team registries, Java event queue/lock comparison, Java invalid-member exception behavior, league broadcast/timeout messages, live socket ordering comparison, Java runtime/threading comparison, encoded opcode/frame golden validation, packet capture comparison, and real-client validation.
- Estimated overall migration completion: 64%

The percentage remains conservative. This window connected several high-value `CM_PLAYER_STATUS_INFO` team-management branches to runtime state and packet fanout, but the generic Java team-command surface is still incomplete.

## Remaining Risks

- Group remove/ban and alliance leave/ban are still unimplemented in `CM_PLAYER_STATUS_INFO`.
- League commands and league broadcast side effects remain deferred.
- Java invalid target member lookup can throw through `Objects.requireNonNull`; the C# packet handler currently no-ops missing targets until a broader packet exception policy is ported.
- Java static group/alliance registries are approximated by runtime snapshots attached to players.
- Java event queue, lock, `CopyOnWriteArrayList`, and threading behavior remain source-derived only.
- Java golden byte vectors, encrypted opcode/frame validation, packet capture comparison, and real-client validation remain unavailable.
- Reflection, precision/rounding, and date/time handling were not materially involved. Serialization parity is limited to C# packet object reachability/order; no Java golden bytes were compared.

## Next Recommended Unit of Work

Continue `CM_PLAYER_STATUS_INFO` parity with one remaining non-league branch:

1. Preferred next unit: `GROUP_REMOVE_MEMBER`, if scoped to Java `PlayerGroupService.removePlayer` / `PlayerGroupLeavedEvent` packet and state side effects.
2. Alternate: `GROUP_BAN_MEMBER`, if the self-ban/no-rights/auto-group messages can be modeled without pulling in broader restriction systems.
3. Alternate alliance path: `ALLIANCE_LEAVE` or `ALLIANCE_BAN_MEMBER`, but only if existing alliance leave/disconnected planners can cover disband and leader fallback accurately enough to document remaining gaps.
4. Keep league commands deferred until a live league runtime bridge exists.
5. After the unit, update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics before committing.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6EA-Completion.md`
   - this handoff
3. Inspect Java source for the selected command branch before touching C#.
4. Implement one narrow unit with Java breadcrumbs.
5. Add focused tests that state what is source-derived and what remains unverified.
6. Run focused tests, then full GameServer tests.
7. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
8. Commit the unit.
