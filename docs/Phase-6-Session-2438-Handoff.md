# Phase 6 Session 2438 Handoff

## Current Phase

- Phase 6: Port Game Core.

## Last Completed UOW

- UOW-2438: Wire in-league alliance disconnect direct packet rows.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`
- `docs/Phase-6-Session-2438-Completion.md`
- `docs/Phase-6-Session-2438-Handoff.md`

## Java Artifacts Reviewed

- `game-server/src/com/aionemu/gameserver/model/team/alliance/PlayerAllianceService.java`
- `game-server/src/com/aionemu/gameserver/model/team/alliance/events/PlayerDisconnectedEvent.java`
- `game-server/src/com/aionemu/gameserver/model/team/alliance/events/PlayerAllianceLeavedEvent.java`
- `game-server/src/com/aionemu/gameserver/model/team/league/League.java`
- `game-server/src/com/aionemu/gameserver/model/team/league/events/LeagueLeftEvent.java`

## Tests Run

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionPlayerStatusInfoTests|FullyQualifiedName~PlayerEnterWorldServiceTests" --no-restore
```

- Result: passed, 126 tests.
- Java/Maven: not run because no Java source or fixtures changed and no narrow Java test exists for this logout event ordering.
- Broad .NET: skipped; no broad-validation trigger applied after the focused command compiled and passed.

## What Changed

- In-league logout/disconnect direct alliance-info packets now use league-expanded `SM_ALLIANCE_INFO(alliance)` rows.
- Existing `League.broadcast(disconnected)` ordering remains after direct disconnect fanout.
- The existing in-league disconnect logout test now asserts the direct alliance-info packet rows, not just packet type.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.model.team.alliance.events.PlayerDisconnectedEvent` | `Aion.GameServer.Services.PlayerEnterWorldService`; `Aion.GameServer.Services.PlayerAllianceDisconnectedPlanner` | Event/Dispatch | Partial | Regression Tested | Partial Parity | In-league direct disconnect fanout rows and subsequent league broadcast ordering are covered. Timeout/offline scheduler variants remain open. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ALLIANCE_INFO` | `Aion.GameServer.Network.Aion.ServerPackets.SmAllianceInfo`; `Aion.GameServer.Services.PlayerAllianceInfoPacketPlan` | Packet | Partial | Regression Tested | Partial Parity | Direct in-league disconnect rows are asserted. Packet-golden coverage remains partial. |
| `com.aionemu.gameserver.model.team.league.League` | `Aion.GameServer.Services.PlayerLeagueRuntime` | Runtime | Partial | Regression Tested | Partial Parity | `broadcast(disconnected)` keeps existing coverage; direct fanout before it now uses real league rows. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.OfflinePlayerAllianceChecker` | No complete C# equivalent identified | Scheduler/Event Source | Not Started | Manual Only | Needs Verification | Java timeout event source remains a gap. |

## Known Gaps

- `OfflinePlayerAllianceChecker` / `LEAVE_TIMEOUT` has no identified C# scheduler equivalent yet.
- In-league `LEAVE_TIMEOUT` must skip ordinary league broadcast but still disband with `onBefore=true` when the alliance should disband.
- In-league leader leave with no online fallback remains risky.
- Additional in-league offline command skip coverage remains useful.

## Next Recommended UOW

UOW-2439: Define or scaffold the C# alliance offline-timeout event source, or produce a focused audit if no safe runtime hook exists.

Suggested Java artifacts:

- `game-server/src/com/aionemu/gameserver/model/team/alliance/PlayerAllianceService.java`
- `game-server/src/com/aionemu/gameserver/model/team/alliance/events/PlayerAllianceLeavedEvent.java`
- `game-server/src/com/aionemu/gameserver/model/team/common/events/PlayerLeavedEvent.java`
- `game-server/src/com/aionemu/gameserver/configs/main/GroupConfig.java`

Suggested C# artifacts:

- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceRuntime.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceLeavedPlanner.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceLeaveWorkflowPlanner.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionPlayerStatusInfoTests.cs`
- Add a narrow service/test only if a clean offline-timeout runtime hook can be introduced without wiring a broad scheduler.

Suggested focused validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionPlayerStatusInfoTests|FullyQualifiedName~PlayerAllianceRuntimeTests" --no-restore
```

Specific behavior to prove: Java `LEAVE_TIMEOUT` ordering removes the offline member, sends timeout leave fanout, skips ordinary `League.broadcast(team)`, disbands with `onBefore=true` when the alliance should disband, then invokes base leave effects. Java/Maven is not expected unless Java source or fixtures change or a narrow Java fixture is created. Broad-validation trigger: none unless shared scheduler/runtime infrastructure is introduced.

## Safe Alternate Candidates

- Add in-league command-side offline remaining-member disband coverage with registry unavailable-player behavior.
- Audit leader leave with no online fallback while in a league and document exact Java behavior before implementation.
- Extract shared alliance-info packet substitution helper if more logout/command branches need the same pattern.

