# Phase 6 Session 2437 Handoff

## Current Phase

- Phase 6: Port Game Core.

## Last Completed UOW

- UOW-2437: Implement ordinary non-disband in-league alliance leave packet row completeness.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionPlayerStatusInfoTests.cs`
- `docs/Phase-6-Session-2437-Completion.md`
- `docs/Phase-6-Session-2437-Handoff.md`

## Java Artifacts Reviewed

- `game-server/src/com/aionemu/gameserver/model/team/alliance/events/PlayerAllianceLeavedEvent.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_ALLIANCE_INFO.java`
- `game-server/src/com/aionemu/gameserver/model/team/league/League.java`

## Tests Run

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionPlayerStatusInfoTests" --no-restore
```

- First run: failed one new BAN test because `STR_FORCE_BAN_ME` was sent before `League.broadcast(team)`.
- Final run: passed, 69 tests.
- Java/Maven: not run because no Java source or fixtures changed and no narrow Java test exists for this exact event ordering.
- Broad .NET: skipped; no broad-validation trigger applied after the focused command compiled and passed.

## What Changed

- Direct remaining-member `SM_ALLIANCE_INFO(team)` packets for in-league ordinary leave/ban now use league-expanded packet plans with real league id, league loot rules, and rows.
- Ordinary non-disband in-league leave/ban now emits Java `League.broadcast(team)` after direct alliance fanout.
- Non-disband BAN now sends `STR_FORCE_BAN_ME` after the league broadcast and before base leave, matching Java.
- Two focused regression tests cover ordinary in-league non-disband leave and ban packet shape/order.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.model.team.alliance.events.PlayerAllianceLeavedEvent` | `Aion.GameServer.Network.Aion.GameServerConnection`; `Aion.GameServer.Services.PlayerAllianceLeavedPlanner` | Event/Dispatch | Partial | Regression Tested | Partial Parity | Ordinary in-league LEAVE/BAN command fanout and broadcast order are covered. Timeout/offline/leader-fallback variants remain open. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ALLIANCE_INFO` | `Aion.GameServer.Network.Aion.ServerPackets.SmAllianceInfo`; `Aion.GameServer.Services.PlayerAllianceInfoPacketPlan` | Packet | Partial | Regression Tested | Partial Parity | Direct in-league rows and skipped-alliance broadcast rows are now covered in command leave/ban tests. Packet-golden coverage is still partial. |
| `com.aionemu.gameserver.model.team.league.League` | `Aion.GameServer.Services.PlayerLeagueRuntime` | Runtime | Partial | Regression Tested | Partial Parity | `League.broadcast(team)` is wired for ordinary non-disband LEAVE/BAN command paths. |

## Known Gaps

- In-league `LEAVE_TIMEOUT` disband behavior still needs discovery; Java skips ordinary league broadcast but still disbands with `onBefore=true` when required.
- In-league offline remaining-member and offline banned-player command behavior remains untested.
- In-league leader leave with no online fallback remains a risky Java edge case.
- No targeted Java/Maven regression exists for these alliance/league event packet orders.

## Next Recommended UOW

UOW-2438: Audit in-league offline and timeout alliance leave branches, then implement the smallest safe covered slice.

Suggested Java artifacts:

- `game-server/src/com/aionemu/gameserver/model/team/alliance/PlayerAllianceService.java`
- `game-server/src/com/aionemu/gameserver/model/team/alliance/events/PlayerAllianceLeavedEvent.java`
- `game-server/src/com/aionemu/gameserver/model/team/alliance/events/PlayerDisconnectedEvent.java`
- `game-server/src/com/aionemu/gameserver/model/team/league/League.java`
- `game-server/src/com/aionemu/gameserver/model/team/league/events/LeagueLeftEvent.java`

Suggested C# artifacts:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceRuntime.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceLeavedPlanner.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionPlayerStatusInfoTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`

Suggested focused validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionPlayerStatusInfoTests|FullyQualifiedName~PlayerEnterWorldServiceTests" --no-restore
```

Specific behavior to prove: whichever branch is selected, validate Java ordering for in-league offline/timeout alliance removal without broadening beyond alliance/league command and logout/timeout surfaces. Java/Maven is not expected unless Java source or fixtures change or a narrow Java fixture is created. Broad-validation trigger: none unless shared packet primitives or common runtime state outside alliance/league planning are changed.

## Safe Alternate Candidates

- Add focused in-league offline remaining-member coverage for command-side disband using existing registry unavailable-player behavior.
- Audit leader leave with no online fallback while in a league and document exact Java behavior before implementation.
- Extract a small non-live planner helper for alliance leave packet substitution if command handler complexity grows further.

