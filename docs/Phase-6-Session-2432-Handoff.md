# Phase 6 Session 2432 Handoff

## Current Phase

- Phase 6 Java-to-C# parity migration continues on branch `4.8`.
- Latest completed UOW: UOW-2432 manual alliance set-captain league fanout.

## Last Completed UOW

- UOW-2432 wired Java-order command-side fanout for manual `ALLIANCE_SET_CAPTAIN` when the changed alliance is in a league.
- The command path now sends:
  - leader-change league `SM_ALLIANCE_INFO` broadcast after leader mutation,
  - normal changed-alliance force leader messages,
  - `STR_UNION_CHANGE_LEADER_TIMEOUT` and `STR_UNION_YOU_BECOME_NEW_LEADER_TIMEOUT` when the changed alliance is the league leader alliance,
  - direct demote-captain-to-vice-captain alliance info,
  - follow-up demotion league broadcast.

## Commits Made

- Pending commit for this handoff: `[Phase 6][UOW-2432] Wire alliance set-captain league fanout`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceRuntime.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionPlayerStatusInfoTests.cs`
- `docs/Phase-6-Session-2432-Completion.md`
- `docs/Phase-6-Session-2432-Handoff.md`

## Java Artifacts Reviewed

- `game-server/src/com/aionemu/gameserver/model/team/common/service/PlayerTeamCommandService.java`
- `game-server/src/com/aionemu/gameserver/model/team/alliance/PlayerAllianceService.java`
- `game-server/src/com/aionemu/gameserver/model/team/alliance/events/ChangeAllianceLeaderEvent.java`
- `game-server/src/com/aionemu/gameserver/model/team/alliance/events/AssignViceCaptainEvent.java`
- `game-server/src/com/aionemu/gameserver/model/team/league/League.java`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection`
- `Aion.GameServer.Services.PlayerAllianceRuntime`
- `Aion.GameServer.Services.PlayerAllianceViceCaptainAssignmentPlanner`
- `Aion.GameServer.Services.PlayerLeagueRuntime`
- `Aion.GameServer.Tests.GameServerConnectionPlayerStatusInfoTests`

## What Changed

- `HandleAllianceLeaderChangeAsync` now dispatches league broadcast and timeout side effects for manual set-captain.
- Added `DispatchAllianceLeaderChangeSystemMessagesAsync` to preserve Java member-loop interleaving between local force messages and league timeout fanout.
- Manual old-captain demotion now sends a follow-up league broadcast when Java `AssignViceCaptainEvent` would.
- `PlayerAllianceRuntime.AssignViceCaptain` now passes `isInLeague` and league id into the planner.
- Added a command-path regression test for in-league manual set-captain fanout and expanded the alliance-info assertion helper to check group size and vice-captain ids.

## Tests Run

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionPlayerStatusInfoTests" --no-restore`
- Result:
  - Passed: 63
  - Failed: 0
  - Skipped: 0
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionPlayerStatusInfoTests|FullyQualifiedName~PlayerAllianceMemberInfoTests" --no-restore`
- Result:
  - Passed: 105
  - Failed: 0
  - Skipped: 0
- `git diff --check`
- Result:
  - Passed; only Git CRLF conversion warnings.
- Pre-existing nullable/analyzer warnings remain.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.model.team.common.service.PlayerTeamCommandService` | `Aion.GameServer.Network.Aion.GameServerConnection.HandlePlayerStatusInfoAsync` | Command Service / Boundary | Partial | Regression Tested | Partial Parity | `ALLIANCE_SET_CAPTAIN` in-league dispatch is now covered. Other team commands remain separately tracked and partially covered. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService` | `Aion.GameServer.Services.PlayerAllianceRuntime` and `GameServerConnection.HandleAllianceLeaderChangeAsync` | Service / Runtime | Partial | Regression Tested | Partial Parity | Manual `changeLeader` command path now models the Java event chain for in-league set-captain. Broader alliance service behavior remains partial. |
| `com.aionemu.gameserver.model.team.alliance.events.ChangeAllianceLeaderEvent` | `Aion.GameServer.Services.PlayerAllianceLeaderChangePlanner`, `PlayerLeagueRuntime.CreateAllianceLeaderChangeTimeoutPlan`, and `GameServerConnection.DispatchAllianceLeaderChangeSystemMessagesAsync` | Event / Live Dispatch | Partial | Regression Tested | Partial Parity | Manual and logout fallback in-league leader-change timeout fanout are now covered. Other event-player-null paths remain covered only through selected logout tests. |
| `com.aionemu.gameserver.model.team.alliance.events.AssignViceCaptainEvent` | `Aion.GameServer.Services.PlayerAllianceViceCaptainAssignmentPlanner` and `PlayerAllianceRuntime.AssignViceCaptain` | Event / Role Assignment | Partial | Regression Tested | Partial Parity | Demote-captain-to-vice-captain after manual leader change now carries league metadata and broadcasts league info. Standalone in-league promote/demote commands need targeted assertions. |
| `com.aionemu.gameserver.model.team.league.League` | `Aion.GameServer.Services.PlayerLeagueRuntime.BroadcastAllianceInfo` | Runtime / Packet Fanout | Partial | Regression Tested | Partial Parity | Manual set-captain and logout fallback leader-change broadcasts are covered. Other league broadcast callers remain partially covered by command/runtime tests. |

## Known Gaps

- Standalone in-league `ALLIANCE_SET_VICECAPTAIN` / `ALLIANCE_UNSET_VICECAPTAIN` commands need targeted coverage for Java's direct alliance-info plus follow-up league broadcast.
- Java `EventService.onLeftTeam` remains metadata-only for group and alliance disconnected disband.
- Manual group/alliance disband command parity and offline timeout disband parity still need review.
- Vortex defence/offence cleanup for alliance leave/kick/logout-adjacent flows still needs review.
- C# logout ordering is still not fully Java-identical around world removal and persistence timing.
- Remaining player alliance snapshots are not fully synchronized when league runtime removes/disbands a league.

## Remaining Risks

- `PlayerAllianceRuntime.AssignViceCaptain` now passes league metadata for every in-league assignment, which matches Java but broadens packet payload shape for promote/demote command paths that do not yet have dedicated in-league tests.
- Java collection ordering for league/alliance iteration remains implementation-defined around `ConcurrentHashMap`; C# runtime uses deterministic list order from existing tests.
- Full live client behavior for these alliance command fanouts remains un-smoked.

## Next Recommended UOW

UOW-2433: Add or wire standalone in-league alliance vice-captain command fanout parity.

Suggested scope:
- Java:
  - `game-server/src/com/aionemu/gameserver/model/team/common/service/PlayerTeamCommandService.java`
  - `game-server/src/com/aionemu/gameserver/model/team/alliance/PlayerAllianceService.java`
  - `game-server/src/com/aionemu/gameserver/model/team/alliance/events/AssignViceCaptainEvent.java`
  - `game-server/src/com/aionemu/gameserver/model/team/league/League.java`
- C#:
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceRuntime.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerLeagueRuntime.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionPlayerStatusInfoTests.cs`

Safe fallback candidate:
- Audit manual/offline-timeout alliance disband behavior.
- Java:
  - `PlayerAllianceService.OfflinePlayerAllianceChecker`
  - `PlayerAllianceLeavedEvent`
  - `AllianceDisbandEvent`
- C#:
  - `PlayerAllianceRuntime`
  - `PlayerLeagueRuntime`
  - `PlayerEnterWorldServiceTests`

## Focused Validation Recipe

- For UOW-2433:
  - Specific behavior/contract:
    - Java standalone `ALLIANCE_SET_VICECAPTAIN` and `ALLIANCE_UNSET_VICECAPTAIN` call `AssignViceCaptainEvent`, mutate vice-captain ids, send direct changed-alliance `SM_ALLIANCE_INFO(team, messageId, eventPlayerName)`, then call `League.broadcast()` when in a league.
  - Focused C# command:
    - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionPlayerStatusInfoTests|FullyQualifiedName~PlayerAllianceMemberInfoTests" --no-restore`
  - Java/Maven:
    - Not expected unless Java source or fixtures change; Java source review should be enough unless a targeted Java alliance vice-captain command test is found.
  - Broad-validation trigger:
    - live alliance/league command connection dispatch if `GameServerConnection` send ordering changes.

## Context Needed By Next Session

- Read `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, and the latest completion document before work.
- Do not use `docs/PHASE-6-PROGRESS.md` during normal startup.
- Java remains the source of truth.
- UOW-2430 made alliance disconnected league broadcast and no-online after-disband league-left behavior live.
- UOW-2431 made logout fallback alliance leader-change league broadcast and timeout behavior live.
- UOW-2432 made manual in-league set-captain command dispatch live and caused `AssignViceCaptain` to carry league metadata; standalone in-league vice-captain promote/demote command coverage is the next natural slice.
