# Phase 6 Session 2436 Handoff

## Current Phase

- Phase 6: Port Game Core.

## Last Completed UOW

- UOW-2436: Implement command-side in-league alliance disband ordering.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceRuntime.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerLeagueRuntime.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionPlayerStatusInfoTests.cs`
- `docs/Phase-6-Session-2436-Completion.md`
- `docs/Phase-6-Session-2436-Handoff.md`

## Java Artifacts Reviewed

- `game-server/src/com/aionemu/gameserver/model/team/alliance/PlayerAllianceService.java`
- `game-server/src/com/aionemu/gameserver/model/team/alliance/events/PlayerAllianceLeavedEvent.java`
- `game-server/src/com/aionemu/gameserver/model/team/alliance/events/AllianceDisbandEvent.java`
- `game-server/src/com/aionemu/gameserver/model/team/league/League.java`
- `game-server/src/com/aionemu/gameserver/model/team/league/events/LeagueLeftEvent.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_ALLIANCE_INFO.java`

## Tests Run

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionPlayerStatusInfoTests|FullyQualifiedName~PlayerEnterWorldServiceTests" --no-restore
```

- Result: passed, 124 tests.
- Java/Maven: not run because no Java source or fixtures changed and no targeted Java test exists for this exact command event ordering.
- Broad .NET: skipped; no broad-validation trigger applied after the focused command compiled and passed.

## What Changed

- Command-side alliance leave/ban now defers cleanup when a two-member alliance disbands while in a league.
- The command handler sends ordinary leave/ban fanout, skipped-alliance league broadcast, league-left event packets, disband packets, optional ban-me, then base leave packets in Java order.
- `PlayerLeagueRuntime` now supports Java `League.broadcast(skippedAlliance)` semantics and blanks skipped-alliance captain fields in league rows.
- Two regression tests cover command-side in-league two-member alliance disband for leave and ban.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService` | `Aion.GameServer.Services.PlayerAllianceRuntime`; `Aion.GameServer.Network.Aion.GameServerConnection` | Service | Partial | Regression Tested | Partial Parity | Command-side in-league `disband(alliance, true)` ordering for leave/ban is covered. Leader leave with no online fallback and timeout-driven in-league disband remain candidates. |
| `com.aionemu.gameserver.model.team.alliance.events.PlayerAllianceLeavedEvent` | `Aion.GameServer.Services.PlayerAllianceLeavedPlanner`; `Aion.GameServer.Network.Aion.GameServerConnection` | Event/Planner | Partial | Regression Tested | Partial Parity | LEAVE/BAN disband branches are improved. Ordinary non-disband in-league `SM_ALLIANCE_INFO(team)` row completeness still needs audit. |
| `com.aionemu.gameserver.model.team.alliance.events.AllianceDisbandEvent` | `Aion.GameServer.Services.PlayerAllianceRuntime.CompleteDeferredDisbandAfterLeaveWorkflow`; `Aion.GameServer.Services.PlayerAllianceLeavedPlanner` | Event/Runtime | Partial | Regression Tested | Partial Parity | Command-side in-league leave/ban disband packet order is covered; offline remaining-member in-league branch is not. |
| `com.aionemu.gameserver.model.team.league.League` | `Aion.GameServer.Services.PlayerLeagueRuntime` | Runtime | Partial | Regression Tested | Partial Parity | `broadcast(skippedAlliance)` is implemented for this command path. Other broadcast callers keep existing coverage. |
| `com.aionemu.gameserver.model.team.league.events.LeagueLeftEvent` | `Aion.GameServer.Services.PlayerLeagueRuntime.RemoveAlliance`; `RemoveAllianceAfterAllianceDisband` | Event/Runtime | Partial | Regression Tested | Partial Parity | `onBefore=true` command-side leave/ban disband ordering is wired. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ALLIANCE_INFO` | `Aion.GameServer.Network.Aion.ServerPackets.SmAllianceInfo`; `Aion.GameServer.Services.PlayerAllianceInfoPacketPlan` | Packet | Partial | Regression Tested | Partial Parity | Skipped-alliance row blanking is covered. Full in-league direct alliance info row completeness remains partial. |

## Known Gaps

- In-league leader leave with no online fallback remains risky and was not added in this UOW.
- In-league `LEAVE_TIMEOUT` disband ordering still needs a targeted runtime/service path if one exists.
- Direct ordinary `SM_ALLIANCE_INFO(team)` emitted inside `PlayerAllianceLeavedEvent` for non-disband in-league leave still needs audit or row-shape implementation.
- Java/Maven parity coverage is source-review only for this event ordering; no current narrow Java unit test was found.

## Next Recommended UOW

UOW-2437: Audit and, if feasible, implement ordinary non-disband in-league alliance leave packet row completeness.

Suggested Java artifacts:

- `game-server/src/com/aionemu/gameserver/model/team/alliance/events/PlayerAllianceLeavedEvent.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_ALLIANCE_INFO.java`
- `game-server/src/com/aionemu/gameserver/model/team/league/League.java`

Suggested C# artifacts:

- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceLeavedPlanner.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceLeaveWorkflowPlanner.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceRuntime.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerLeagueRuntime.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionPlayerStatusInfoTests.cs`

Suggested focused validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionPlayerStatusInfoTests" --no-restore
```

Specific behavior to prove: non-disband in-league alliance leave/ban sends direct `SM_ALLIANCE_INFO(team)` to remaining alliance members with Java-equivalent league id, league loot rules, and league rows, then sends Java `League.broadcast(team)` to other alliances for LEAVE/BAN.

Java/Maven is not expected unless Java source or fixtures change; use Java source review unless a narrow Java packet fixture is created. Broad-validation trigger: none unless the implementation changes shared packet serialization primitives or common runtime state outside alliance/league planning.

## Safe Alternate Candidates

- Add focused in-league offline remaining-member coverage for command-side disband after the new deferred cleanup hook.
- Audit `LEAVE_TIMEOUT` alliance timeout path and determine whether C# has a narrow runtime entry point to wire `disband(true)` without ordinary league broadcast.
- Audit leader leave with no online fallback while in a league and document the exact Java behavior before implementation.

