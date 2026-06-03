# Phase 6 Session 2438 Completion

## Unit of Work

- UOW-2438: Wire in-league alliance disconnect direct packet rows.

## Status

- Completed and validated with focused tests.
- C# logout/disconnect alliance fanout now sends Java-equivalent direct `SM_ALLIANCE_INFO(alliance)` rows before the existing in-league `League.broadcast(disconnected)` packets.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/model/team/alliance/PlayerAllianceService.java`
- `game-server/src/com/aionemu/gameserver/model/team/alliance/events/PlayerDisconnectedEvent.java`
- `game-server/src/com/aionemu/gameserver/model/team/alliance/events/PlayerAllianceLeavedEvent.java`
- `game-server/src/com/aionemu/gameserver/model/team/league/League.java`
- `game-server/src/com/aionemu/gameserver/model/team/league/events/LeagueLeftEvent.java`

## C# Artifacts Changed

- `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`

## Implementation Notes

- Reused `PlayerLeagueRuntime.CreateAllianceInfoFanout(...)` during logout/disconnect alliance fanout so direct `SM_ALLIANCE_INFO(alliance)` packets carry the real league id, league loot rules, and league rows.
- Preserved existing Java `PlayerDisconnectedEvent` ordering: direct offline/member/alliance-info fanout first, then `League.broadcast(disconnected)` when online members remain.
- Delivery skip behavior remains unchanged: disconnected and offline recipients are skipped by existing logout recipient filters.
- Timeout scheduler behavior was audited but not implemented in this UOW because C# does not yet expose a narrow `OfflinePlayerAllianceChecker` equivalent entry point.

## Tests Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `LeaveWorld_DispatchesAllianceLeagueBroadcastAfterDisconnectedFanoutLikeJavaLogout` | Regression | Java source review of `PlayerDisconnectedEvent` and `SM_ALLIANCE_INFO` | Direct disconnect fanout sends league-expanded alliance info before `League.broadcast(disconnected)`; both carry Java-equivalent league rows | Focused C# packet-order and packet-shape assertions | Does not cover leader-disconnect/no-online disband branch beyond existing adjacent tests. |

## Validation Decision

- Changed surface: production logout dispatch and focused test assertions.
- Specific behavior/contract: Java `PlayerDisconnectedEvent` in-league branch sends direct `SM_ALLIANCE_INFO(alliance)` with real league rows before `League.broadcast(disconnected)`.
- Focused C# command: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionPlayerStatusInfoTests|FullyQualifiedName~PlayerEnterWorldServiceTests" --no-restore`
- Focused Java/Maven command: not run; Java source was reviewed and no Java source or fixtures changed.
- Broad-validation trigger: none. The change is scoped to alliance logout packet substitution and existing league packet planners; no packet primitive, serializer, persistence, scheduler, or release-readiness trigger applied.
- Broad .NET decision: skipped; the filtered command compiled the affected project and validated the edited logout surface plus adjacent alliance command coverage.
- Result: passed, 126 tests.
- Notes: existing nullable/analyzer warnings were emitted by the project and were not introduced by this UOW.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.model.team.alliance.events.PlayerDisconnectedEvent` | `Aion.GameServer.Services.PlayerEnterWorldService`; `Aion.GameServer.Services.PlayerAllianceDisconnectedPlanner` | Event/Dispatch | Partial | Regression Tested | Partial Parity | In-league direct disconnect alliance-info rows and subsequent league broadcast ordering are covered. Timeout scheduler and some offline edge cases remain open. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ALLIANCE_INFO` | `Aion.GameServer.Network.Aion.ServerPackets.SmAllianceInfo`; `Aion.GameServer.Services.PlayerAllianceInfoPacketPlan` | Packet | Partial | Regression Tested | Partial Parity | Direct in-league disconnect rows are now asserted. Broader packet-golden coverage remains partial. |
| `com.aionemu.gameserver.model.team.league.League` | `Aion.GameServer.Services.PlayerLeagueRuntime` | Runtime | Partial | Regression Tested | Partial Parity | Existing `broadcast(disconnected)` path remains covered and direct packet fanout now uses league-expanded rows before it. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.OfflinePlayerAllianceChecker` | No complete C# equivalent identified | Scheduler/Event Source | Not Started | Manual Only | Needs Verification | Java timeout branch fires `PlayerAllianceLeavedEvent(..., LEAVE_TIMEOUT)` and skips ordinary league broadcast. C# needs a narrow scheduler/runtime entry point before implementation. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 5
- Total artifacts ported or adjusted in this UOW: 2 C# artifacts
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 41%

## Remaining Risks

- Java `OfflinePlayerAllianceChecker` timeout behavior still needs a C# scheduler/runtime entry point.
- `LEAVE_TIMEOUT` in-league behavior should skip ordinary `League.broadcast(team)` but disband with `onBefore=true` when required.
- In-league leader leave with no online fallback remains risky and unimplemented.
- Offline command-side in-league skip behavior has command tests for non-league only; additional in-league command skip coverage remains useful.

