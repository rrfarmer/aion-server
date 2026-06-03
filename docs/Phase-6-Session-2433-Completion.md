# Phase 6 Session 2433 Completion

Status: Phase 6 continues; UOW-2433 wired standalone in-league alliance vice-captain command fanout.

## Scope

- UOW: UOW-2433 standalone `ALLIANCE_SET_VICECAPTAIN` / `ALLIANCE_UNSET_VICECAPTAIN` in-league command parity.
- Reviewed Java `PlayerTeamCommandService`, `PlayerAllianceService.changeViceCaptain`, `AssignViceCaptainEvent`, `League.broadcast`, and `SM_ALLIANCE_INFO`.
- Added a C# league-runtime fanout plan for Java `SM_ALLIANCE_INFO(alliance, messageId, message)` direct alliance packets that include league rows.
- Wired C# vice-captain command dispatch to send direct changed-alliance info with league rows, then the Java follow-up league broadcast.
- Reused the same dispatch helper for manual set-captain old-captain demotion so its direct demotion packets now include league rows like Java.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/model/team/common/service/PlayerTeamCommandService.java`
  - `ALLIANCE_SET_VICECAPTAIN` and `ALLIANCE_UNSET_VICECAPTAIN` resolve the selected member and call `PlayerAllianceService.changeViceCaptain`.
- `game-server/src/com/aionemu/gameserver/model/team/alliance/PlayerAllianceService.java`
  - `changeViceCaptain(Player, AssignType)` fires `AssignViceCaptainEvent`.
- `game-server/src/com/aionemu/gameserver/model/team/alliance/events/AssignViceCaptainEvent.java`
  - Promote/demote mutates vice-captain ids, sends `SM_ALLIANCE_INFO(team, messageId, eventPlayerName)` to every changed-alliance member, then calls `League.broadcast()` when the alliance is in a league.
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_ALLIANCE_INFO.java`
  - The message constructor includes league id, league loot rules, and league captain rows when `alliance.getLeague()` is not null.
- `game-server/src/com/aionemu/gameserver/model/team/league/League.java`
  - `broadcast()` sends `SM_ALLIANCE_INFO(targetAlliance)` to every alliance in league position order.

## Implemented

- Added `PlayerLeagueRuntime.CreateAllianceInfoFanout(...)` and `PlayerLeagueAllianceInfoFanoutPlan`.
- `GameServerConnection.HandleAllianceViceCaptainAssignmentAsync` now dispatches through `DispatchAllianceViceCaptainAssignmentAsync`.
- `DispatchAllianceViceCaptainAssignmentAsync` sends:
  - promote-limit system message when Java returns early,
  - direct changed-alliance `SM_ALLIANCE_INFO` with league rows when in a league,
  - normal direct planner intents when not in a league,
  - follow-up `League.broadcast()` when Java would broadcast league info.
- `HandleAllianceLeaderChangeAsync` now uses the same vice-captain dispatch helper for old-captain demotion, correcting direct demotion packets to include league rows.

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `HandlePlayerStatusInfoAsync_AllianceViceCaptainCommandsInLeagueFanOutLikeJavaEvent` | Regression | Java `PlayerTeamCommandService`, `PlayerAllianceService.changeViceCaptain`, `AssignViceCaptainEvent`, `SM_ALLIANCE_INFO`, and `League.broadcast` source review | In-league standalone promote and demote commands mutate vice-captain ids, send direct changed-alliance alliance-info with message ids `1300984` / `1300985` and league rows, then send the follow-up league broadcast. | Focused C# command dispatch assertion over recipient order, direct message ids, message text, vice-captain ids, and league row payloads. | Promote-limit inside a league remains covered only for no-broadcast behavior through the existing non-league-shaped limit test. |
| `HandlePlayerStatusInfoAsync_AllianceSetCaptainInLeagueFansOutLikeJavaLeaderEvent` | Regression | Java `ChangeAllianceLeaderEvent`, `AssignViceCaptainEvent`, and `SM_ALLIANCE_INFO` source review | Old-captain demotion after manual set-captain now sends direct demotion alliance-info with league rows before the follow-up league broadcast. | Existing set-captain test updated to assert league rows on the direct demotion packets. | Broader manual leader-change behavior remains limited to the covered command scenario. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.model.team.common.service.PlayerTeamCommandService` | `Aion.GameServer.Network.Aion.GameServerConnection.HandlePlayerStatusInfoAsync` | Command Service / Boundary | Partial | Regression Tested | Partial Parity | Standalone in-league vice-captain promote/demote command dispatch is now covered. Other team commands remain separately tracked. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService` | `Aion.GameServer.Services.PlayerAllianceRuntime` and `GameServerConnection.HandleAllianceViceCaptainAssignmentAsync` | Service / Runtime | Partial | Regression Tested | Partial Parity | `changeViceCaptain` command path now models direct alliance-info plus league broadcast for in-league promote/demote. Broader alliance service behavior remains partial. |
| `com.aionemu.gameserver.model.team.alliance.events.AssignViceCaptainEvent` | `Aion.GameServer.Services.PlayerAllianceViceCaptainAssignmentPlanner`, `PlayerLeagueRuntime.CreateAllianceInfoFanout`, and `GameServerConnection.DispatchAllianceViceCaptainAssignmentAsync` | Event / Role Assignment | Partial | Regression Tested | Partial Parity | Promote, demote, and set-captain demote-captain paths now carry league-row direct packets and follow-up league broadcasts. Offline event-player and promote-limit edge behavior remains only partially covered. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ALLIANCE_INFO` | `Aion.GameServer.Network.Aion.ServerPackets.SmAllianceInfo` and `PlayerAllianceInfoPacketPlan` | Packet DTO / Factory | Partial | Regression Tested | Partial Parity | Direct in-league alliance-info packets with message ids now include league rows. Broader packet constructors remain partial across other callers. |
| `com.aionemu.gameserver.model.team.league.League` | `Aion.GameServer.Services.PlayerLeagueRuntime.BroadcastAllianceInfo` and `CreateAllianceInfoFanout` | Runtime / Packet Fanout | Partial | Regression Tested | Partial Parity | `broadcast()` and direct in-league alliance-info row construction are covered through vice-captain and leader-change command paths. Other league broadcast callers remain partially covered. |

## Validation Decision

- Changed surface: live alliance/league command connection dispatch plus league runtime packet-intent planning.
- Specific behavior/contract: Java standalone `ALLIANCE_SET_VICECAPTAIN` and `ALLIANCE_UNSET_VICECAPTAIN` call `AssignViceCaptainEvent`, mutate vice-captain ids, send direct changed-alliance `SM_ALLIANCE_INFO(team, messageId, eventPlayerName)` with league rows, then call `League.broadcast()` when in a league.
- Focused C# commands:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionPlayerStatusInfoTests" --no-restore`
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionPlayerStatusInfoTests|FullyQualifiedName~PlayerAllianceMemberInfoTests" --no-restore`
- Results:
  - First command passed: 64 passed, 0 failed, 0 skipped.
  - Second command passed: 106 passed, 0 failed, 0 skipped.
- Hygiene:
  - `git diff --check`
  - Passed; only Git CRLF conversion warnings.
- Focused Java/Maven command:
  - Not run; no Java source or fixtures changed, and Java evidence came from direct source review. No targeted Java unit test for this command/event path was found.
- Broad-validation trigger:
  - Live alliance/league command connection dispatch.
- Broad .NET decision:
  - Full project/solution validation skipped after focused evidence. The filtered commands compiled affected dependencies and exercised the edited command boundary plus adjacent alliance-info packet contract; no packet primitive, serializer core, scheduler, persistence repository, or connection registry implementation changed.
- Why this scope is sufficient:
  - The scoped risk is command-side fanout ordering and `SM_ALLIANCE_INFO` payload shape for one Java event chain. The focused regression asserts exact recipient order, direct/broadcast packet payloads, message ids, message text, vice-captain ids, and league rows.

## Known Remaining Gaps

- Promote-limit in an active league has no dedicated test asserting that Java returns before direct alliance-info and league broadcast.
- Java `EventService.onLeftTeam` remains metadata-only for group and alliance disconnected disband.
- Manual group/alliance disband command parity and offline timeout disband parity still need review.
- Vortex defence/offence cleanup for alliance leave/kick/logout-adjacent flows still needs review.
- C# logout ordering is still not fully Java-identical around world removal and persistence timing.
- Remaining player alliance snapshots are not fully synchronized when league runtime removes/disbands a league.

## Summary Metrics

- Java artifacts reviewed: 5.
- C# artifacts reviewed: 5.
- Production files changed: 2.
- Test files changed: 1.
- Focused validation commands passed: 2.
- Artifacts with verified parity: 0.
- Artifacts needing verification or partial parity: 5.
- Blocked artifacts: 0.
- Estimated Phase 6 completion: unchanged, still conservatively partial.
