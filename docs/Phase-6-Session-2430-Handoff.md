# Phase 6 Session 2430 Handoff

## Current Phase

- Phase 6 Java-to-C# parity migration continues on branch `4.8`.
- Latest completed UOW: UOW-2430 alliance disconnected league broadcast and after-disband league-left parity.

## Last Completed UOW

- UOW-2430 wired alliance logout league side effects that were metadata-only in prior handoff notes.
- Java `League.broadcast(disconnected)` is now modeled and live-dispatched during alliance logout when online members remain.
- Java `PlayerAllianceService.disband(alliance, false)` after-disband league-left/disperse behavior is now modeled and live-dispatched for no-online alliance logout disband.

## Commits Made

- Pending commit for this handoff: `[Phase 6][UOW-2430] Wire alliance logout league fanout`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerLeagueRuntime.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`
- `docs/Phase-6-Session-2430-Completion.md`
- `docs/Phase-6-Session-2430-Handoff.md`

## Java Artifacts Reviewed

- `game-server/src/com/aionemu/gameserver/model/team/alliance/events/PlayerDisconnectedEvent.java`
- `game-server/src/com/aionemu/gameserver/model/team/alliance/events/ChangeAllianceLeaderEvent.java`
- `game-server/src/com/aionemu/gameserver/model/team/alliance/PlayerAllianceService.java`
- `game-server/src/com/aionemu/gameserver/model/team/alliance/events/AllianceDisbandEvent.java`
- `game-server/src/com/aionemu/gameserver/model/team/league/League.java`
- `game-server/src/com/aionemu/gameserver/model/team/league/events/LeagueLeftEvent.java`
- `game-server/src/com/aionemu/gameserver/model/team/league/events/LeagueDisbandEvent.java`

## C# Artifacts Touched

- `Aion.GameServer.Services.PlayerEnterWorldService`
- `Aion.GameServer.Services.PlayerLeagueRuntime`
- `Aion.GameServer.Tests.PlayerEnterWorldServiceTests`

## What Changed

- `PlayerEnterWorldService` now accepts optional `PlayerLeagueRuntime`.
- Alliance disconnected logout now dispatches league broadcast packet intents when the alliance remains online in a league.
- Alliance no-online logout disband now removes the alliance, then dispatches league-left/new-leader/dispersed packet intents to remaining online league recipients.
- `PlayerLeagueRuntime` now has:
  - `BroadcastAllianceInfo(...)`
  - `RemoveAllianceAfterAllianceDisband(...)`
  - `PlayerLeagueBroadcastPlan`
- Tests now serialize `SM_ALLIANCE_INFO` in `PlayerEnterWorldServiceTests` to assert league ids, rows, message ids, and recipient order.

## Tests Run

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~GameServerConnectionPlayerStatusInfoTests" --no-restore`
- Result:
  - Passed: 118
  - Failed: 0
  - Skipped: 0
- `git diff --check`
- Result:
  - Passed; only Git CRLF conversion warnings.
- Pre-existing nullable/analyzer warnings remain.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.model.team.alliance.events.PlayerDisconnectedEvent` | `Aion.GameServer.Services.PlayerAllianceDisconnectedPlanner` plus `PlayerEnterWorldService.DispatchAllianceDisconnectedLogoutAsync` | Event / Live Dispatch | Partial | Regression Tested | Partial Parity | Alliance disconnected fanout was already live; logout now also dispatches league broadcast when online members remain and league-left after no-online disband. Leader-change league-specific system-message branches remain partial. |
| `com.aionemu.gameserver.model.team.league.League` | `Aion.GameServer.Services.PlayerLeagueRuntime.BroadcastAllianceInfo` | Runtime / Packet Fanout | Partial | Regression Tested | Partial Parity | Java `broadcast(Player skippedPlayer)` modeled for alliance logout and tested through live logout dispatch. Broader league broadcast overloads remain covered only by existing command/runtime tests. |
| `com.aionemu.gameserver.model.team.league.events.LeagueLeftEvent` | `Aion.GameServer.Services.PlayerLeagueRuntime.RemoveAllianceAfterAllianceDisband` and `RemoveAlliance` | Event / Runtime Mutation | Partial | Regression Tested | Partial Parity | Added after-alliance-disband path where the removed alliance has no packet recipients; existing normal leave/expel path remains in command tests. |
| `com.aionemu.gameserver.model.team.league.events.LeagueDisbandEvent` | `Aion.GameServer.Services.PlayerLeagueRuntime.RemoveAllianceAfterAllianceDisband` | Event / Runtime Mutation | Partial | Regression Tested | Partial Parity | Remaining one-alliance league now emits dispersed info during logout-disband cascade. Broader league disband entry points remain partial. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.disband` | `Aion.GameServer.Services.PlayerAllianceRuntime.DisbandAfterDisconnectedNoOnlineMembers` plus `PlayerLeagueRuntime.RemoveAllianceAfterAllianceDisband` | Service / Runtime Mutation | Partial | Regression Tested | Partial Parity | Logout no-online disband now removes alliance runtime and then notifies league runtime. Manual disband and offline timeout disband still need parity review. |

## Known Gaps

- Alliance leader-change league-specific system-message branches remain partial.
- Java `EventService.onLeftTeam` remains metadata-only for group and alliance disconnected disband.
- Manual group/alliance disband command parity and offline timeout disband parity still need review.
- Vortex defence/offence cleanup for alliance leave/kick/logout-adjacent flows still needs review.
- C# logout ordering is still not fully Java-identical around world removal and persistence timing.
- Remaining player alliance snapshots are not fully synchronized when league runtime removes/disbands a league.

## Remaining Risks

- League runtime and alliance runtime are still loosely coupled; this UOW dispatches correct logout packets, but broader snapshot synchronization after command-side league removal remains a risk.
- Java `ConcurrentHashMap` iteration order remains not deterministic; C# runtime list order is deterministic.
- Live dispatch now depends on `PlayerLeagueRuntime` being passed into `PlayerEnterWorldService`; composition sites that do not provide it keep the old no-league-dispatch behavior.

## Next Recommended UOW

UOW-2431: Audit alliance leader-change league-specific system-message parity.

Suggested scope:
- Java:
  - `game-server/src/com/aionemu/gameserver/model/team/alliance/events/ChangeAllianceLeaderEvent.java`
  - `game-server/src/com/aionemu/gameserver/model/team/league/League.java`
  - `game-server/src/com/aionemu/gameserver/utils/collections/Predicates.java`
- C#:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceLeaderChangePlanner.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceRuntime.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerAllianceMemberInfoTests.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`

Safe fallback candidate:
- Audit manual/offline-timeout alliance disband behavior now that logout-disband league-left is live.
- Java:
  - `PlayerAllianceService.OfflinePlayerAllianceChecker`
  - `PlayerAllianceLeavedEvent`
  - `AllianceDisbandEvent`
- C#:
  - `PlayerAllianceRuntime`
  - `PlayerLeagueRuntime`
  - `PlayerAllianceRuntimeTests`
  - `PlayerEnterWorldServiceTests`

## Focused Validation Recipe

- For UOW-2431:
  - Specific behavior/contract:
    - Java `ChangeAllianceLeaderEvent.changeLeaderTo` sends league broadcast and, when the new alliance leader is also the league captain, sends `STR_UNION_CHANGE_LEADER_TIMEOUT` to other league members and `STR_UNION_YOU_BECOME_NEW_LEADER_TIMEOUT` to the new captain.
  - Focused C# command:
    - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerAllianceMemberInfoTests|FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~GameServerConnectionPlayerStatusInfoTests" --no-restore`
    - If only planner metadata is touched, narrow to `PlayerAllianceMemberInfoTests`.
  - Java/Maven:
    - Not expected unless Java source or fixtures change; Java source review should be enough unless a targeted Java alliance leader-change test is found.
  - Broad-validation trigger:
    - live alliance/league connection dispatch if logout/service wiring changes; otherwise none for planner-only metadata.

## Context Needed By Next Session

- Read `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, and the latest completion document before work.
- Do not use `docs/PHASE-6-PROGRESS.md` during normal startup.
- Java remains the source of truth.
- The handoff from UOW-2429 named `PlayerLeagueRuntimeTests.cs`, but that file does not exist; current league runtime coverage lives mostly in `GameServerConnectionPlayerStatusInfoTests.cs`.
- UOW-2428 made alliance disconnected logout live for non-league fanout, leader fallback, and no-online disband cleanup.
- UOW-2429 aligned group disconnected live dispatch with Java's offline-recipient send guard.
- UOW-2430 made alliance disconnected league broadcast and no-online after-disband league-left behavior live.
