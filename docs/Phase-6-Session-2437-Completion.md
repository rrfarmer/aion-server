# Phase 6 Session 2437 Completion

## Unit of Work

- UOW-2437: Implement ordinary non-disband in-league alliance leave packet row completeness.

## Status

- Completed and validated with focused tests.
- Non-disband alliance `LEAVE` and `BAN` command paths now send Java-equivalent in-league direct `SM_ALLIANCE_INFO(team)` rows to remaining alliance members, then Java `League.broadcast(team)` packets to other alliances.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/model/team/alliance/events/PlayerAllianceLeavedEvent.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_ALLIANCE_INFO.java`
- `game-server/src/com/aionemu/gameserver/model/team/league/League.java`

## C# Artifacts Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionPlayerStatusInfoTests.cs`

## Implementation Notes

- Reused `PlayerLeagueRuntime.CreateAllianceInfoFanout(...)` to replace simplified direct alliance-info packets for in-league alliance leave workflow fanout.
- Reused `PlayerLeagueRuntime.BroadcastAllianceInfoExceptAlliance(...)` so ordinary non-disband `LEAVE`/`BAN` emits Java `League.broadcast(team)` to other alliances after direct remaining-member fanout.
- Split the non-disband `BAN` workflow so `STR_FORCE_BAN_ME` is sent after the league broadcast, matching Java `PlayerAllianceLeavedEvent`.
- The disband path from UOW-2436 also benefits from the direct alliance-info substitution for pre-disband fanout.

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `HandlePlayerStatusInfoAsync_AllianceLeaveNonDisbandInLeagueSendsDirectRowsThenLeagueBroadcastLikeJava` | Regression | Java source review of `PlayerAllianceLeavedEvent`, `SM_ALLIANCE_INFO`, and `League.broadcast(skippedAlliance)` | Non-disband leave sends direct remaining-member alliance info with real league id/rows, then other-alliance broadcast with skipped-alliance captain fields blanked, then base leave | Focused C# packet-order and packet-shape assertions | Does not cover offline remaining-member registry skip. |
| `HandlePlayerStatusInfoAsync_AllianceBanNonDisbandInLeagueSendsDirectRowsThenLeagueBroadcastLikeJava` | Regression | Java source review of `PlayerAllianceLeavedEvent` BAN branch | Non-disband ban sends direct rows, league broadcast, then `STR_FORCE_BAN_ME`, then base leave | Focused C# packet-order and packet-shape assertions | Does not cover offline banned-player registry skip while in a league. |

## Validation Decision

- Changed surface: production command dispatch and focused tests.
- Specific behavior/contract: Java `PlayerAllianceLeavedEvent` non-disband `LEAVE`/`BAN` while in a league: direct `SM_ALLIANCE_INFO(team)` includes actual league id, loot rules, and league rows; `League.broadcast(team)` follows direct fanout; `BAN` sends `STR_FORCE_BAN_ME` after the league broadcast.
- Focused C# command: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionPlayerStatusInfoTests" --no-restore`
- Focused Java/Maven command: not run; no Java source or fixtures changed and no narrow Java test exists for this event ordering.
- Broad-validation trigger: none. The change is scoped to command dispatch ordering and uses existing packet planners/runtime APIs; no packet primitive, serializer, persistence, scheduler, or release-readiness trigger applied.
- Broad .NET decision: skipped; the filtered command compiled the affected project and validated the changed command surface.
- First focused run: failed one new BAN test, proving `STR_FORCE_BAN_ME` was still before `League.broadcast(team)`.
- Final focused run: passed, 69 tests.
- Notes: existing nullable/analyzer warnings were emitted by the project and were not introduced by this UOW.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.model.team.alliance.events.PlayerAllianceLeavedEvent` | `Aion.GameServer.Network.Aion.GameServerConnection`; `Aion.GameServer.Services.PlayerAllianceLeavedPlanner` | Event/Dispatch | Partial | Regression Tested | Partial Parity | Non-disband in-league LEAVE/BAN direct fanout and league broadcast ordering are now covered. Timeout and offline variants remain open. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ALLIANCE_INFO` | `Aion.GameServer.Network.Aion.ServerPackets.SmAllianceInfo`; `Aion.GameServer.Services.PlayerAllianceInfoPacketPlan` | Packet | Partial | Regression Tested | Partial Parity | Direct in-league `SM_ALLIANCE_INFO(team)` rows are now covered for ordinary leave/ban command paths. Broader packet-golden coverage remains partial. |
| `com.aionemu.gameserver.model.team.league.League` | `Aion.GameServer.Services.PlayerLeagueRuntime` | Runtime | Partial | Regression Tested | Partial Parity | `League.broadcast(team)` is now wired for ordinary non-disband LEAVE/BAN command paths. Other callers retain existing coverage. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 3
- Total artifacts ported or adjusted in this UOW: 2 C# artifacts
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 3
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 41%

## Remaining Risks

- `LEAVE_TIMEOUT` in-league behavior still needs a targeted runtime/service path; Java skips ordinary `League.broadcast(team)` there.
- Offline remaining-member and offline banned-player registry skip behavior while in a league is not covered by this UOW.
- Leader leave with no online fallback while in a league remains risky and unimplemented.
- Java/Maven parity coverage remains source-review only for these event-order paths because no narrow Java test exists in the current repo.

