# Phase 6 Session 2430 Completion

Status: Phase 6 continues; UOW-2430 wired alliance disconnected logout league broadcast and no-online disband league-left parity.

## Scope

- UOW: UOW-2430 alliance disconnected league broadcast parity.
- Reviewed Java alliance disconnected, alliance leader-change, alliance disband, league broadcast, league-left, and league-disband event flow.
- Added C# league packet-intent support for Java `League.broadcast(disconnected)`.
- Added C# league-left-after-alliance-disband support for Java `PlayerAllianceService.disband(alliance, false)`.
- Wired `PlayerEnterWorldService` to dispatch these league intents during alliance logout when `PlayerLeagueRuntime` is available.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/model/team/alliance/events/PlayerDisconnectedEvent.java`
  - Sends alliance offline/member-info/alliance-info fanout first.
  - If no online members remain, calls `PlayerAllianceService.disband(alliance, false)`.
  - Otherwise, if the alliance is in a league, calls `alliance.getLeague().broadcast(disconnected)`.
- `game-server/src/com/aionemu/gameserver/model/team/alliance/events/ChangeAllianceLeaderEvent.java`
  - Broadcasts league info when an in-league alliance leader changes.
  - Existing C# leader-change parity remains partial.
- `game-server/src/com/aionemu/gameserver/model/team/alliance/PlayerAllianceService.java`
  - `disband(alliance, false)` removes recruitment, runs `AllianceDisbandEvent`, removes the alliance map entry, then fires `LeagueLeftEvent`.
- `game-server/src/com/aionemu/gameserver/model/team/alliance/events/AllianceDisbandEvent.java`
  - Removes all alliance members before the after-disband league-left event runs.
- `game-server/src/com/aionemu/gameserver/model/team/league/League.java`
  - `broadcast(Player skippedPlayer)` sends `SM_ALLIANCE_INFO(targetAlliance)` to every league alliance except the skipped player.
- `game-server/src/com/aionemu/gameserver/model/team/league/events/LeagueLeftEvent.java`
  - Removes the alliance, reorganizes positions, sends remaining alliance notifications, optionally sends new-leader timeout, then disbands the league at one remaining alliance.
- `game-server/src/com/aionemu/gameserver/model/team/league/events/LeagueDisbandEvent.java`
  - Sends league-dispersed info to the remaining alliance during the minimum-member disband cascade.

## Implemented

- Added optional `PlayerLeagueRuntime` dependency to `PlayerEnterWorldService`.
- `DispatchAllianceDisconnectedLogoutAsync` now:
  - dispatches Java `League.broadcast(disconnected)` packet intents when online members remain in a league alliance,
  - after no-online alliance disband, dispatches Java after-disband league-left/disperse intents when the disbanded alliance was in a league.
- Added `PlayerLeagueRuntime.BroadcastAllianceInfo(...)`.
- Added `PlayerLeagueRuntime.RemoveAllianceAfterAllianceDisband(...)`.
- Added `PlayerLeagueBroadcastPlan`.
- Live logout league dispatch skips missing, offline, and disconnected recipients at the socket boundary, matching Java `PacketSendUtility.sendPacket`.

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `LeaveWorld_DispatchesAllianceLeagueBroadcastAfterDisconnectedFanoutLikeJavaLogout` | Regression | Java `PlayerDisconnectedEvent` and `League.broadcast(Player)` source review | Alliance logout with online members remaining sends normal alliance disconnected fanout, then league `SM_ALLIANCE_INFO` to all online league members except the disconnected player. | Focused C# logout assertions over send order, recipients, packet types, league id, league rows, and absence of self send. | Real client connection not exercised. |
| `LeaveWorld_DisbandAllianceNotifiesLeagueAfterAllianceRemovalLikeJavaLogout` | Regression | Java `PlayerAllianceService.disband(alliance, false)`, `AllianceDisbandEvent`, `LeagueLeftEvent`, and `LeagueDisbandEvent` source review | No-online alliance logout removes the alliance, then notifies only remaining league alliance recipients with left, new-leader timeout, and dispersed packets. | Focused C# logout assertions over recipient list, `SM_ALLIANCE_INFO` message ids, league rows, system message id `1400588`, runtime alliance removal, and league disband cleanup. | Existing C# alliance snapshots on remaining players are not fully synchronized with league runtime removal. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.model.team.alliance.events.PlayerDisconnectedEvent` | `Aion.GameServer.Services.PlayerAllianceDisconnectedPlanner` plus `PlayerEnterWorldService.DispatchAllianceDisconnectedLogoutAsync` | Event / Live Dispatch | Partial | Regression Tested | Partial Parity | Alliance disconnected fanout was already live; logout now also dispatches league broadcast when online members remain and league-left after no-online disband. Leader-change league-specific system-message branches remain partial. |
| `com.aionemu.gameserver.model.team.league.League` | `Aion.GameServer.Services.PlayerLeagueRuntime.BroadcastAllianceInfo` | Runtime / Packet Fanout | Partial | Regression Tested | Partial Parity | Java `broadcast(Player skippedPlayer)` modeled for alliance logout and tested through live logout dispatch. Broader league broadcast overloads remain covered only by existing command/runtime tests. |
| `com.aionemu.gameserver.model.team.league.events.LeagueLeftEvent` | `Aion.GameServer.Services.PlayerLeagueRuntime.RemoveAllianceAfterAllianceDisband` and `RemoveAlliance` | Event / Runtime Mutation | Partial | Regression Tested | Partial Parity | Added after-alliance-disband path where the removed alliance has no packet recipients; existing normal leave/expel path remains in command tests. |
| `com.aionemu.gameserver.model.team.league.events.LeagueDisbandEvent` | `Aion.GameServer.Services.PlayerLeagueRuntime.RemoveAllianceAfterAllianceDisband` | Event / Runtime Mutation | Partial | Regression Tested | Partial Parity | Remaining one-alliance league now emits dispersed info during logout-disband cascade. Broader league disband entry points remain partial. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.disband` | `Aion.GameServer.Services.PlayerAllianceRuntime.DisbandAfterDisconnectedNoOnlineMembers` plus `PlayerLeagueRuntime.RemoveAllianceAfterAllianceDisband` | Service / Runtime Mutation | Partial | Regression Tested | Partial Parity | Logout no-online disband now removes alliance runtime and then notifies league runtime. Manual disband and offline timeout disband still need parity review. |

## Validation Decision

- Changed surface: live alliance logout connection dispatch plus league runtime packet-intent planning.
- Specific behavior/contract: Java alliance logout league behavior: `League.broadcast(disconnected)` after disconnected fanout when online members remain, and `LeagueLeftEvent`/`LeagueDisbandEvent` after `PlayerAllianceService.disband(alliance, false)` when no online members remain.
- Focused C# command:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~GameServerConnectionPlayerStatusInfoTests" --no-restore`
- Result:
  - Passed: 118
  - Failed: 0
  - Skipped: 0
- Hygiene:
  - `git diff --check`
  - Passed; only Git CRLF conversion warnings.
- Focused Java/Maven command:
  - Not run; no Java source or fixtures changed, and Java evidence came from direct source review. No targeted Java unit test for this logout league event path was found during this UOW.
- Broad-validation trigger:
  - Live league/alliance runtime mutation and connection dispatch.
- Broad .NET decision:
  - Full project/solution validation skipped after focused evidence. The filtered command compiled the affected project and exercised the edited live logout dispatch plus the existing league command/runtime contract tests. No packet primitive, serializer, scheduler, persistence repository, or shared connection registry implementation changed.
- Why this scope is sufficient:
  - The changed runtime behavior is isolated to alliance logout league packet intents and dispatch. Tests assert the exact Java-derived send order, recipient filtering, message ids, league rows, runtime removal, and disband cleanup for both affected logout branches.

## Known Remaining Gaps

- Alliance leader-change league-specific system-message branches remain partial, including Java captain league timeout messages.
- Java `EventService.onLeftTeam` remains metadata-only for group and alliance disconnected disband.
- Manual group/alliance disband command parity and offline timeout disband parity still need review.
- Vortex defence/offence cleanup for alliance leave/kick/logout-adjacent flows still needs review.
- C# logout ordering is still not fully Java-identical around world removal and persistence timing.
- Remaining player alliance snapshots are not fully synchronized when league runtime removes/disbands a league.

## Summary Metrics

- Java artifacts reviewed: 7.
- C# artifacts reviewed: 3.
- Production files changed: 2.
- Test files changed: 1.
- Focused validation commands passed: 1.
- Artifacts with verified parity: 0.
- Artifacts needing verification or partial parity: 5.
- Blocked artifacts: 0.
- Estimated Phase 6 completion: unchanged, still conservatively partial.
