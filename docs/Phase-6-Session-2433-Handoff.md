# Phase 6 Session 2433 Handoff

## Current Phase

- Phase 6 Java-to-C# parity migration continues on branch `4.8`.
- Latest completed UOW: UOW-2433 standalone in-league alliance vice-captain command fanout.

## Last Completed UOW

- UOW-2433 wired Java-order command-side fanout for standalone `ALLIANCE_SET_VICECAPTAIN` and `ALLIANCE_UNSET_VICECAPTAIN` when the alliance is in a league.
- The command path now sends:
  - direct changed-alliance `SM_ALLIANCE_INFO(team, messageId, eventPlayerName)` including league rows,
  - follow-up league `SM_ALLIANCE_INFO` broadcast.
- The previous manual set-captain old-captain demotion path now reuses the same helper, so its direct demotion packets also include league rows like Java.

## Commits Made

- Pending commit for this handoff: `[Phase 6][UOW-2433] Wire alliance vice-captain league fanout`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerLeagueRuntime.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionPlayerStatusInfoTests.cs`
- `docs/Phase-6-Session-2433-Completion.md`
- `docs/Phase-6-Session-2433-Handoff.md`

## Java Artifacts Reviewed

- `game-server/src/com/aionemu/gameserver/model/team/common/service/PlayerTeamCommandService.java`
- `game-server/src/com/aionemu/gameserver/model/team/alliance/PlayerAllianceService.java`
- `game-server/src/com/aionemu/gameserver/model/team/alliance/events/AssignViceCaptainEvent.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_ALLIANCE_INFO.java`
- `game-server/src/com/aionemu/gameserver/model/team/league/League.java`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection`
- `Aion.GameServer.Services.PlayerLeagueRuntime`
- `Aion.GameServer.Services.PlayerAllianceRuntime`
- `Aion.GameServer.Services.PlayerAllianceViceCaptainAssignmentPlanner`
- `Aion.GameServer.Tests.GameServerConnectionPlayerStatusInfoTests`

## What Changed

- Added `PlayerLeagueRuntime.CreateAllianceInfoFanout(...)` and `PlayerLeagueAllianceInfoFanoutPlan`.
- Added `GameServerConnection.DispatchAllianceViceCaptainAssignmentAsync(...)`.
- Standalone vice-captain promote/demote commands now use the league fanout plan for direct in-league alliance-info packets with rows.
- Vice-captain assignment dispatch now calls `BroadcastAllianceInfo` after direct packet fanout when Java `AssignViceCaptainEvent` would call `League.broadcast()`.
- Manual set-captain demotion now routes through the shared vice-captain assignment dispatcher.
- Added a regression test for in-league promote/demote command fanout and updated set-captain demotion assertions for direct packet league rows.

## Tests Run

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionPlayerStatusInfoTests" --no-restore`
- Result:
  - Passed: 64
  - Failed: 0
  - Skipped: 0
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionPlayerStatusInfoTests|FullyQualifiedName~PlayerAllianceMemberInfoTests" --no-restore`
- Result:
  - Passed: 106
  - Failed: 0
  - Skipped: 0
- `git diff --check`
- Result:
  - Passed; only Git CRLF conversion warnings.
- Pre-existing nullable/analyzer warnings remain.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.model.team.common.service.PlayerTeamCommandService` | `Aion.GameServer.Network.Aion.GameServerConnection.HandlePlayerStatusInfoAsync` | Command Service / Boundary | Partial | Regression Tested | Partial Parity | Standalone in-league vice-captain promote/demote command dispatch is now covered. Other team commands remain separately tracked. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService` | `Aion.GameServer.Services.PlayerAllianceRuntime` and `GameServerConnection.HandleAllianceViceCaptainAssignmentAsync` | Service / Runtime | Partial | Regression Tested | Partial Parity | `changeViceCaptain` command path now models direct alliance-info plus league broadcast for in-league promote/demote. Broader alliance service behavior remains partial. |
| `com.aionemu.gameserver.model.team.alliance.events.AssignViceCaptainEvent` | `Aion.GameServer.Services.PlayerAllianceViceCaptainAssignmentPlanner`, `PlayerLeagueRuntime.CreateAllianceInfoFanout`, and `GameServerConnection.DispatchAllianceViceCaptainAssignmentAsync` | Event / Role Assignment | Partial | Regression Tested | Partial Parity | Promote, demote, and set-captain demote-captain paths now carry league-row direct packets and follow-up league broadcasts. Offline event-player and promote-limit edge behavior remains only partially covered. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ALLIANCE_INFO` | `Aion.GameServer.Network.Aion.ServerPackets.SmAllianceInfo` and `PlayerAllianceInfoPacketPlan` | Packet DTO / Factory | Partial | Regression Tested | Partial Parity | Direct in-league alliance-info packets with message ids now include league rows. Broader packet constructors remain partial across other callers. |
| `com.aionemu.gameserver.model.team.league.League` | `Aion.GameServer.Services.PlayerLeagueRuntime.BroadcastAllianceInfo` and `CreateAllianceInfoFanout` | Runtime / Packet Fanout | Partial | Regression Tested | Partial Parity | `broadcast()` and direct in-league alliance-info row construction are covered through vice-captain and leader-change command paths. Other league broadcast callers remain partially covered. |

## Known Gaps

- Promote-limit in an active league has no dedicated test asserting that Java returns before direct alliance-info and league broadcast.
- Java `EventService.onLeftTeam` remains metadata-only for group and alliance disconnected disband.
- Manual group/alliance disband command parity and offline timeout disband parity still need review.
- Vortex defence/offence cleanup for alliance leave/kick/logout-adjacent flows still needs review.
- C# logout ordering is still not fully Java-identical around world removal and persistence timing.
- Remaining player alliance snapshots are not fully synchronized when league runtime removes/disbands a league.

## Remaining Risks

- Java collection ordering for league/alliance iteration remains implementation-defined around `ConcurrentHashMap`; C# runtime uses deterministic list order from existing tests.
- Full live client behavior for these alliance command fanouts remains un-smoked.
- Direct in-league `SM_ALLIANCE_INFO` row construction is now centralized for vice-captain-style fanout, but other direct packet constructors may still need review.

## Next Recommended UOW

UOW-2434: Audit and add targeted coverage for in-league promote-limit early-return behavior, or move to alliance disband parity if this edge is already judged sufficiently low risk.

Suggested scope for promote-limit edge:
- Java:
  - `game-server/src/com/aionemu/gameserver/model/team/alliance/events/AssignViceCaptainEvent.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`
- C#:
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceViceCaptainAssignmentPlanner.cs`
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

- For UOW-2434 promote-limit edge:
  - Specific behavior/contract:
    - Java `AssignViceCaptainEvent` with four existing vice captains sends only `STR_FORCE_CANNOT_PROMOTE_MANAGER` to the alliance leader and returns before direct alliance-info or league broadcast, even when the alliance is in a league.
  - Focused C# command:
    - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionPlayerStatusInfoTests" --no-restore`
  - Java/Maven:
    - Not expected unless Java source or fixtures change; Java source review should be enough unless a targeted Java alliance vice-captain command test is found.
  - Broad-validation trigger:
    - none if only test coverage is added; live alliance/league command connection dispatch if production send ordering changes.

## Context Needed By Next Session

- Read `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, and the latest completion document before work.
- Do not use `docs/PHASE-6-PROGRESS.md` during normal startup.
- Java remains the source of truth.
- UOW-2430 made alliance disconnected league broadcast and no-online after-disband league-left behavior live.
- UOW-2431 made logout fallback alliance leader-change league broadcast and timeout behavior live.
- UOW-2432 made manual in-league set-captain command dispatch live and caused `AssignViceCaptain` to carry league metadata.
- UOW-2433 made standalone in-league vice-captain command dispatch live and corrected direct in-league alliance-info packets to include league rows.
