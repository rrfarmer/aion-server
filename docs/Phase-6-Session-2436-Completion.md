# Phase 6 Session 2436 Completion

## Unit of Work

- UOW-2436: Implement command-side in-league alliance disband ordering.

## Status

- Completed and validated with focused tests.
- C# command-side alliance leave/ban now preserves Java `PlayerAllianceService.disband(alliance, true)` ordering when the disbanding alliance is in a league.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/model/team/alliance/PlayerAllianceService.java`
- `game-server/src/com/aionemu/gameserver/model/team/alliance/events/PlayerAllianceLeavedEvent.java`
- `game-server/src/com/aionemu/gameserver/model/team/alliance/events/AllianceDisbandEvent.java`
- `game-server/src/com/aionemu/gameserver/model/team/league/League.java`
- `game-server/src/com/aionemu/gameserver/model/team/league/events/LeagueLeftEvent.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_ALLIANCE_INFO.java`

## C# Artifacts Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceRuntime.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerLeagueRuntime.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionPlayerStatusInfoTests.cs`

## Implementation Notes

- Added `PlayerLeagueRuntime.BroadcastAllianceInfoExceptAlliance(...)` for Java `League.broadcast(skippedAlliance)` semantics.
- Updated league row creation so a skipped alliance remains in the league row list but has blank captain name and world id, matching `SM_ALLIANCE_INFO(alliance, skippedAlliance)`.
- Added deferred alliance disband cleanup in `PlayerAllianceRuntime` so command-side in-league disband can remove find-group recruitment, emit league-left packets while the remaining alliance member is still addressable, then clear the remaining member through disband cleanup.
- Updated alliance leave/ban command handling to send:
  1. ordinary alliance leave/ban fanout;
  2. Java `League.broadcast(team)` to other alliances for `LEAVE`/`BAN`;
  3. `LeagueLeftEvent(LEAVE)` fanout;
  4. remaining alliance member disband fanout;
  5. ban-me packet when applicable;
  6. base leave packets.
- Existing logout/no-online `disband(false)` behavior remains on `RemoveAllianceAfterAllianceDisband(...)`.

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `HandlePlayerStatusInfoAsync_AllianceLeaveTwoMemberInLeagueDisbandsWithLeagueLeftBeforeDisbandLikeJava` | Regression | Java source review of `PlayerAllianceLeavedEvent`, `PlayerAllianceService.disband`, `League.broadcast`, `LeagueLeftEvent`, and `SM_ALLIANCE_INFO` | Leave command on a two-member alliance in a league sends ordinary alliance leave, skipped-alliance league broadcast, league-left, leader-timeout/dispersed league packets, disband packets, then base leave in Java order | Focused C# packet-order and packet-shape assertions | Does not cover leader leave with no online fallback. |
| `HandlePlayerStatusInfoAsync_AllianceBanTwoMemberInLeagueDisbandsWithLeagueLeftBeforeDisbandLikeJava` | Regression | Java source review of `PlayerAllianceLeavedEvent` BAN branch and `PlayerAllianceService.disband(alliance, true)` | Ban command on a two-member alliance in a league preserves Java ordering, including delayed `STR_FORCE_BAN_ME` after disband | Focused C# packet-order and packet-shape assertions | Does not cover offline banned-player registry skip in league disband. |

## Validation Decision

- Changed surface: production command dispatch, alliance runtime state, league broadcast packet planning, focused tests.
- Specific behavior/contract: Java command-side `PlayerAllianceService.disband(alliance, true)` ordering for alliance `LEAVE` and `BAN` while the alliance is in a league, including skipped-alliance `SM_ALLIANCE_INFO` row blanking.
- Focused C# command: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionPlayerStatusInfoTests|FullyQualifiedName~PlayerEnterWorldServiceTests" --no-restore`
- Focused Java/Maven command: not run; Java source was reviewed and no Java source or fixtures changed.
- Broad-validation trigger: none. The change touches command dispatch and shared alliance/league runtime state, so the adjacent logout league class was included in the focused filter, but no shared primitive, packet serializer, persistence, scheduler, or release-readiness trigger applied.
- Broad .NET decision: skipped; the focused filtered test command compiled the affected project and dependencies and validated the edited command and adjacent logout league behavior.
- Result: passed, 124 tests.
- Notes: existing nullable/analyzer warnings were emitted by the project and were not introduced by this UOW.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService` | `Aion.GameServer.Services.PlayerAllianceRuntime`; `Aion.GameServer.Network.Aion.GameServerConnection` | Service | Partial | Regression Tested | Partial Parity | Command-side in-league `disband(alliance, true)` ordering for leave/ban is now covered. Logout/no-online `disband(false)` remains separately covered. Leader leave with no online fallback and timeout-driven command-equivalent disband still need focused coverage. |
| `com.aionemu.gameserver.model.team.alliance.events.PlayerAllianceLeavedEvent` | `Aion.GameServer.Services.PlayerAllianceLeavedPlanner`; `Aion.GameServer.Network.Aion.GameServerConnection` | Event/Planner | Partial | Regression Tested | Partial Parity | LEAVE and BAN two-member in-league command disband ordering is covered. Direct `SM_ALLIANCE_INFO(team)` league-row completeness for ordinary non-disband in-league leave remains a known broader packet-shape gap. |
| `com.aionemu.gameserver.model.team.alliance.events.AllianceDisbandEvent` | `Aion.GameServer.Services.PlayerAllianceRuntime.CompleteDeferredDisbandAfterLeaveWorkflow`; `Aion.GameServer.Services.PlayerAllianceLeavedPlanner` | Event/Runtime | Partial | Regression Tested | Partial Parity | Remaining-member disband fanout now occurs after league-left in command-side in-league leave/ban. Offline remaining-member branch has non-league coverage; league/offline coverage remains open. |
| `com.aionemu.gameserver.model.team.league.League` | `Aion.GameServer.Services.PlayerLeagueRuntime` | Runtime | Partial | Regression Tested | Partial Parity | `broadcast(skippedAlliance)` is now represented for command-side leave/ban disband, including blank skipped-alliance captain fields. Other League broadcast callers still rely on existing focused coverage. |
| `com.aionemu.gameserver.model.team.league.events.LeagueLeftEvent` | `Aion.GameServer.Services.PlayerLeagueRuntime.RemoveAlliance`; `RemoveAllianceAfterAllianceDisband` | Event/Runtime | Partial | Regression Tested | Partial Parity | Command-side `onBefore=true` league-left ordering is now wired for leave/ban disband. Expel and explicit league leave coverage remains unchanged. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ALLIANCE_INFO` | `Aion.GameServer.Network.Aion.ServerPackets.SmAllianceInfo`; `Aion.GameServer.Services.PlayerAllianceInfoPacketPlan` | Packet | Partial | Regression Tested | Partial Parity | Skipped-alliance league rows now preserve row membership while blanking captain name/world id. General in-league alliance-info row completeness remains a conservative partial-parity note. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 6
- Total artifacts ported or adjusted in this UOW: 4 C# artifacts
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 6
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 41%

## Remaining Risks

- Leader leave with no online fallback while in a league remains untested and may still need Java-specific handling because the Java leader reference can be awkward after removal.
- Timeout-driven `LEAVE_TIMEOUT` in-league disband should skip ordinary `League.broadcast(team)` but still use `disband(true)`; no narrow command/service path was implemented in this UOW.
- Ordinary non-disband in-league alliance leave still uses a simplified alliance-info plan path that does not yet prove full Java league-row packet shape for `SM_ALLIANCE_INFO(team)`.
- No Java/Maven test exists for this exact event ordering in the current repo; parity evidence is source review plus focused C# regression coverage.

