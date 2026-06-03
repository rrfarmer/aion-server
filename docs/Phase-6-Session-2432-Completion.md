# Phase 6 Session 2432 Completion

Status: Phase 6 continues; UOW-2432 wired manual alliance set-captain league fanout.

## Scope

- UOW: UOW-2432 manual `ALLIANCE_SET_CAPTAIN` in-league leader-change parity.
- Reviewed Java `PlayerTeamCommandService`, `PlayerAllianceService.changeLeader`, `ChangeAllianceLeaderEvent`, and `AssignViceCaptainEvent`.
- Wired C# `GameServerConnection` command dispatch to send Java-order league broadcast, leader-change system messages, league timeout fanout, direct demotion alliance info, and the demotion follow-up league broadcast.
- Passed league membership metadata into `PlayerAllianceRuntime.AssignViceCaptain` so demotion packets and `WouldBroadcastLeague` match Java event shape.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/model/team/common/service/PlayerTeamCommandService.java`
  - `ALLIANCE_SET_CAPTAIN` resolves the selected member and calls `PlayerAllianceService.changeLeader`.
- `game-server/src/com/aionemu/gameserver/model/team/alliance/PlayerAllianceService.java`
  - `changeLeader(Player)` fires `ChangeAllianceLeaderEvent(alliance, player)`.
- `game-server/src/com/aionemu/gameserver/model/team/alliance/events/ChangeAllianceLeaderEvent.java`
  - Manual leader change mutates leader, removes the new leader from vice captains, calls `League.broadcast()` when in a league, sends normal force-leader messages, sends league timeout messages when the changed alliance is the league leader alliance, then demotes the old leader to vice captain.
- `game-server/src/com/aionemu/gameserver/model/team/alliance/events/AssignViceCaptainEvent.java`
  - Demote-captain-to-vice-captain mutates vice captains, sends `SM_ALLIANCE_INFO(team, 0, oldLeaderName)` to changed-alliance members, then calls `League.broadcast()` when in a league.
- `game-server/src/com/aionemu/gameserver/model/team/league/League.java`
  - `broadcast()` sends `SM_ALLIANCE_INFO(targetAlliance)` to every alliance in league position order.

## Implemented

- `GameServerConnection.HandleAllianceLeaderChangeAsync` now captures changed-alliance member order and league id before mutation.
- Manual in-league set-captain now dispatches Java `League.broadcast()` after leader mutation.
- Manual set-captain system messages now interleave changed-alliance force messages with per-trigger league timeout fanout in Java member-loop order.
- Old-captain demotion now follows the leader-change messages and sends the Java follow-up league broadcast when applicable.
- `PlayerAllianceRuntime.AssignViceCaptain` now passes league id and `isInLeague` to the vice-captain assignment planner.

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `HandlePlayerStatusInfoAsync_AllianceSetCaptainInLeagueFansOutLikeJavaLeaderEvent` | Regression | Java `PlayerTeamCommandService`, `PlayerAllianceService.changeLeader`, `ChangeAllianceLeaderEvent`, `AssignViceCaptainEvent`, and `League.broadcast` source review | Manual alliance set-captain in the league leader alliance sends leader-change league info, force leader messages, union timeout messages, demotion alliance info, and demotion league broadcast in Java order. | Focused C# command dispatch assertion over recipient order, `SM_ALLIANCE_INFO` rows, vice-captain ids, and system-message ids `1300998`, `1300999`, `1400588`, `1400587`. | Other vice-captain command paths now carry league metadata but still need dedicated in-league promote/demote command assertions. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.model.team.common.service.PlayerTeamCommandService` | `Aion.GameServer.Network.Aion.GameServerConnection.HandlePlayerStatusInfoAsync` | Command Service / Boundary | Partial | Regression Tested | Partial Parity | `ALLIANCE_SET_CAPTAIN` in-league dispatch is now covered. Other team commands remain separately tracked and partially covered. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService` | `Aion.GameServer.Services.PlayerAllianceRuntime` and `GameServerConnection.HandleAllianceLeaderChangeAsync` | Service / Runtime | Partial | Regression Tested | Partial Parity | Manual `changeLeader` command path now models the Java event chain for in-league set-captain. Broader alliance service behavior remains partial. |
| `com.aionemu.gameserver.model.team.alliance.events.ChangeAllianceLeaderEvent` | `Aion.GameServer.Services.PlayerAllianceLeaderChangePlanner`, `PlayerLeagueRuntime.CreateAllianceLeaderChangeTimeoutPlan`, and `GameServerConnection.DispatchAllianceLeaderChangeSystemMessagesAsync` | Event / Live Dispatch | Partial | Regression Tested | Partial Parity | Manual and logout fallback in-league leader-change timeout fanout are now covered. Other event-player-null paths remain covered only through selected logout tests. |
| `com.aionemu.gameserver.model.team.alliance.events.AssignViceCaptainEvent` | `Aion.GameServer.Services.PlayerAllianceViceCaptainAssignmentPlanner` and `PlayerAllianceRuntime.AssignViceCaptain` | Event / Role Assignment | Partial | Regression Tested | Partial Parity | Demote-captain-to-vice-captain after manual leader change now carries league metadata and broadcasts league info. Standalone in-league promote/demote commands need targeted assertions. |
| `com.aionemu.gameserver.model.team.league.League` | `Aion.GameServer.Services.PlayerLeagueRuntime.BroadcastAllianceInfo` | Runtime / Packet Fanout | Partial | Regression Tested | Partial Parity | Manual set-captain and logout fallback leader-change broadcasts are covered. Other league broadcast callers remain partially covered by command/runtime tests. |

## Validation Decision

- Changed surface: live alliance/league command connection dispatch plus alliance runtime role-assignment metadata.
- Specific behavior/contract: Java manual `ALLIANCE_SET_CAPTAIN` dispatches `ChangeAllianceLeaderEvent(eventPlayer != null)`, sends league broadcast when in a league, sends normal force leader messages, sends union timeout messages when the changed alliance is the league leader alliance, demotes the old leader to vice captain, then sends the demotion league broadcast.
- Focused C# commands:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionPlayerStatusInfoTests" --no-restore`
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionPlayerStatusInfoTests|FullyQualifiedName~PlayerAllianceMemberInfoTests" --no-restore`
- Results:
  - First final command passed: 63 passed, 0 failed, 0 skipped.
  - Second final command passed: 105 passed, 0 failed, 0 skipped.
- Hygiene:
  - `git diff --check`
  - Passed; only Git CRLF conversion warnings.
- Focused Java/Maven command:
  - Not run; no Java source or fixtures changed, and Java evidence came from direct source review. No targeted Java unit test for this command/event path was found.
- Broad-validation trigger:
  - Live alliance/league command connection dispatch.
- Broad .NET decision:
  - Full project/solution validation skipped after focused evidence. The filtered commands compiled affected dependencies and exercised the edited command boundary plus adjacent alliance info packet contract; no packet primitive, serializer core, scheduler, persistence repository, or connection registry implementation changed.
- Why this scope is sufficient:
  - The scoped risk is command-side ordering and packet payload fanout for one Java event chain. The new regression asserts the Java-derived recipient order, packet types, league rows, vice-captain ids, and system-message ids.

## Known Remaining Gaps

- Standalone in-league `ALLIANCE_SET_VICECAPTAIN` / `ALLIANCE_UNSET_VICECAPTAIN` commands need targeted coverage for Java's direct alliance-info plus follow-up league broadcast.
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
