# Phase 6 Session 2440 Completion

## Unit of Work

- UOW-2440: Wire alliance offline-timeout dispatcher.

## Status

- Completed and validated with focused tests.
- C# now has a narrow service-level dispatcher that consumes the alliance offline-timeout runtime hook and sends Java-ordered timeout, league-left, alliance-disband, and base-leave packets through `IGameClientConnectionRegistry`.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/model/team/alliance/PlayerAllianceService.java`
- `game-server/src/com/aionemu/gameserver/model/team/alliance/events/PlayerAllianceLeavedEvent.java`
- `game-server/src/com/aionemu/gameserver/model/team/common/events/PlayerLeavedEvent.java`
- `game-server/src/com/aionemu/gameserver/model/team/league/events/LeagueLeftEvent.java`
- `game-server/src/com/aionemu/gameserver/model/team/GeneralTeam.java`

## C# Artifacts Changed

- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceInfoPlan.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceRuntime.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceOfflineTimeoutDispatchService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerAllianceOfflineTimeoutDispatchServiceTests.cs`

## Implementation Notes

- Added `PlayerAllianceOfflineTimeoutDispatchService.DispatchNextExpiredAsync(...)` as the narrow dispatcher around `PlayerAllianceRuntime.RemoveNextExpiredOfflineMemberWithLeaveWorkflow(...)`.
- Extended `PlayerAllianceOfflineTimeoutPlan` with `LeagueId` so dispatch preserves the Java league context captured before timeout mutation.
- In-league timeout disband dispatch now sends direct timeout fanout, skips ordinary `League.broadcast(team)`, removes the alliance from league, completes deferred alliance disband cleanup, then sends alliance disband packets and base leave packets.
- The service is not a hosted scheduler. A future scheduler can loop the single-step dispatch method to match Java `OfflinePlayerAllianceChecker`.
- Offence VortexService removal remains represented by `WouldRemoveOffenceInvader` on the result; execution is not wired in this UOW.

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `DispatchNextExpiredAsync_InLeagueDisbandSkipsOrdinaryBroadcastLikeJavaTimeout` | Regression | Java source review of `OfflinePlayerAllianceChecker`, `PlayerAllianceLeavedEvent`, and `LeagueLeftEvent` | Timeout dispatcher sends direct `LEAVE_TIMEOUT` fanout, skips ordinary league broadcast, sends league-left/disperse packets before alliance disband packets, and omits base packets for the offline timed-out member | Focused C# packet-order and serialized packet-shape assertions | Does not wire a hosted scheduler or execute VortexService removal. |
| `DispatchNextExpiredAsync_ReturnsNullWhenNoAllianceMemberExpired` | Unit | Java source review of `OfflinePlayerAllianceChecker` expiration guard | Non-expired offline members are left untouched and no packets are sent | Focused C# runtime/dispatch assertion | Does not cover multi-expired-member loop behavior. |

## Validation Decision

- Changed surface: non-hosted dispatcher service, timeout plan metadata, focused dispatcher tests.
- Specific behavior/contract: Java `LEAVE_TIMEOUT` dispatch order for in-league disband: direct fanout, no ordinary `League.broadcast(team)`, league-left before alliance disband, then base leave side effects.
- Focused C# command: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerAllianceOfflineTimeoutDispatchServiceTests|FullyQualifiedName~PlayerAllianceRuntimeTests" --no-restore`
- Result: passed, 34 tests.
- Hygiene command: `git diff --check`
- Result: passed; Git emitted line-ending normalization warnings for touched files.
- Focused Java/Maven command: not run; Java source was reviewed and no Java source or fixtures changed.
- Broad-validation trigger: none. The change adds a non-hosted service and does not alter shared packet primitives, registry contracts, `GameServerConnection`, persistence, or scheduler infrastructure.
- Broad .NET decision: skipped; the filtered command compiled the affected project and validated serialized packet order for the edited dispatcher surface.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.OfflinePlayerAllianceChecker` | `Aion.GameServer.Services.PlayerAllianceOfflineTimeoutDispatchService`; `Aion.GameServer.Services.PlayerAllianceRuntime` | Scheduler/Event Source | Partial | Regression Tested | Partial Parity | Single-step timeout dispatch is implemented and tested. Hosted fixed-rate scheduling remains open. |
| `com.aionemu.gameserver.model.team.alliance.events.PlayerAllianceLeavedEvent` | `Aion.GameServer.Services.PlayerAllianceOfflineTimeoutDispatchService`; `Aion.GameServer.Services.PlayerAllianceLeaveWorkflowPlanner` | Event/Dispatch | Partial | Regression Tested | Partial Parity | `LEAVE_TIMEOUT` direct fanout and disband packet ordering are covered for in-league disband. |
| `com.aionemu.gameserver.model.team.league.events.LeagueLeftEvent` | `Aion.GameServer.Services.PlayerLeagueRuntime`; `Aion.GameServer.Services.PlayerAllianceOfflineTimeoutDispatchService` | Event/Dispatch | Partial | Regression Tested | Partial Parity | Timeout disband now dispatches league-left before alliance disband packets; ordinary timeout league broadcast is intentionally skipped per Java. |
| `com.aionemu.gameserver.model.team.common.events.PlayerLeavedEvent` | `Aion.GameServer.Services.PlayerBaseLeavePlanner`; `Aion.GameServer.Services.PlayerAllianceOfflineTimeoutDispatchService` | Event/Side Effect | Partial | Regression Tested | Partial Parity | Base leave ordering after alliance leave is preserved; offline timed-out member emits no base packet. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 5
- Total artifacts ported or adjusted in this UOW: 4 C# artifacts
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 41%

## Remaining Risks

- No hosted fixed-rate C# scheduler invokes the timeout dispatcher yet.
- Offence `VortexService.removeInvaderPlayer` is still represented as a result flag only.
- Multi-expired-member loop behavior needs a scheduler or executor test.
- Timeout dispatcher is not registered in game-server startup composition yet.
