# Phase 6FG Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6FF and covers Session 651.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter GameServerConnectionPlayerStatusInfoTests`
  - Result: Passed, 54 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1127 tests.

## Recent Work Completed

### Session 651 - Command 30 Permission And Disband Coverage

- Added command `30` `LEAGUE_EXPEL` edge coverage.
- Verified invalid target alliance behavior from Java `PlayerTeamCommandService.findLeagueAlliance`.
- Verified `LeagueService.expelAlliance` permission failures:
  - caller is not the leader of their own alliance,
  - caller leads their alliance but that alliance is not the league leader alliance.
- Verified two-alliance expel-triggered disband:
  - remaining alliance receives `LEAGUE_EXPEL`,
  - expelled alliance receives `LEAGUE_EXPELLED`,
  - remaining alliance receives `LEAGUE_DISPERSED`,
  - league state is cleared.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_PLAYER_STATUS_INFO` | `GameServerConnection.HandlePlayerStatusInfoAsync` command `30` path | Client Packet / Handler Boundary | Partial | Regression Tested | Needs Verification | Command `30` covers success, invalid target, permission failures, and expel-triggered disband. |
| `com.aionemu.gameserver.model.team.common.events.TeamCommand.LEAGUE_EXPEL` | Command code `30` branch | Enum / Command Mapping | Partial | Regression Tested | Needs Verification | Edge cases validate lookup/check ordering and no-send behavior for failures. |
| `com.aionemu.gameserver.model.team.common.service.PlayerTeamCommandService` | Manual command branches in `GameServerConnection.HandlePlayerStatusInfoAsync` | Service / Dispatcher | Partial | Regression Tested | Needs Verification | C# mirrors command `30` lookup/check ordering but still lacks a generic dispatcher. |
| `com.aionemu.gameserver.model.team.league.LeagueService.expelAlliance` | `PlayerLeagueRuntime.ExpelAlliance` | Service / Runtime Bridge | Partial | Regression Tested | Needs Verification | Permission checks and disband branch are regression-tested. |
| `com.aionemu.gameserver.model.team.league.LeagueService.removeAlliance` | Shared `PlayerLeagueRuntime.RemoveAllianceCore` | Service / Runtime Bridge | Partial | Regression Tested | Needs Verification | Expel disband reuses removal core and clears league state. |
| `com.aionemu.gameserver.model.team.league.League` | `PlayerLeagueRuntime` / `PlayerLeagueSnapshot` | Team State | Partial | Regression Tested | Needs Verification | Expel without disband and with disband are tested. Java locks/object identity remain unverified. |
| `com.aionemu.gameserver.model.team.league.LeagueMember` | Internal `PlayerLeagueRuntime.PlayerLeagueMember` | Team Member | Partial | Regression Tested | Needs Verification | Invalid target lookup and expel target removal are tested by alliance id. |
| `com.aionemu.gameserver.model.team.league.events.LeagueLeftEvent` | `PlayerLeagueRuntime.ExpelAlliance` / `PlayerLeaguePacketIntent` | Event Runtime Dependency | Partial | Regression Tested | Needs Verification | EXPEL reason covers success, disband, and permission no-send behavior. |
| `com.aionemu.gameserver.model.team.league.events.LeagueDisbandEvent` | `PlayerLeagueRuntime.ExpelAlliance` disband branch | Event Runtime Dependency | Partial | Regression Tested | Needs Verification | Expel-triggered disband is covered for the two-alliance case. Direct disband remains unported. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ALLIANCE_INFO` | `SmAllianceInfo` / `PlayerAllianceInfoPacketPlan` | Server Packet | Partial | Regression Tested | Needs Verification | Two-alliance expel serializes `LEAGUE_EXPEL`, `LEAGUE_EXPELLED`, and `LEAGUE_DISPERSED`. No Java golden bytes. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAlliance` | `PlayerAllianceRuntime` | Team State Dependency | Partial | Regression Tested | Needs Verification | Supplies caller/target alliance context for command `30`. |
| `com.aionemu.gameserver.utils.PacketSendUtility` | `IGameClientConnectionRegistry` / `GameServerConnection.SendLeaguePacketAsync` | Runtime Dependency | Partial | Regression Tested | Needs Verification | Failure tests verify no packets; disband test verifies in-memory recipient order. |

## Tests Added

- `GameServerConnectionPlayerStatusInfoTests.HandlePlayerStatusInfoAsync_LeagueExpelInvalidTargetThrowsLikeJavaFindLeagueAlliance`
- `GameServerConnectionPlayerStatusInfoTests.HandlePlayerStatusInfoAsync_LeagueExpelByNonAllianceLeaderThrowsLikeJava`
- `GameServerConnectionPlayerStatusInfoTests.HandlePlayerStatusInfoAsync_LeagueExpelByNonLeagueLeaderAllianceThrowsLikeJava`
- `GameServerConnectionPlayerStatusInfoTests.HandlePlayerStatusInfoAsync_LeagueExpelLastAllianceDisbandsLikeJava`

These tests are source-derived. They do not compare against Java runtime execution, Java-generated golden vectors, encrypted frames, packet captures, or a live client.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 12
- Total artifacts ported or partially modeled in this handoff window: 1 command `30` edge-coverage regression slice.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 10
- Total blocked/not-started artifacts: Java golden byte validation, full `LeagueService`, Java static league registry, `LEAGUE_SET_LEADER`, Java event queue/lock comparison, packet processor exception/log comparison, and client validation.
- Estimated overall migration completion: 64%

The percentage stays conservative. Command `30` is better bounded, but command `32` and wider league lifecycle parity remain open.

## Remaining Risks

- Command `32` `LEAGUE_SET_LEADER` remains at missing-league prerequisite behavior only.
- League invite/join/loot/kinah workflows, direct disband iteration, offline recipients, Java static registry, event queue/locks, object identity, and packet processor exception/log behavior remain deferred.
- Packet-field coverage is C# emitted-object validation only; Java golden bytes, encrypted frames, packet captures, and live-client validation remain unavailable.
- C# packet intent order is deterministic by sorted positions; Java `ConcurrentHashMap` ordering remains unverified.
- Reflection, precision/rounding, and date/time handling are not involved in this unit.

## Next Recommended Unit of Work

Begin command `32` `LEAGUE_SET_LEADER`:

1. Source-read `LeagueService.setLeader` and `LeagueChangeLeaderEvent`.
2. Add the smallest runtime branch for a league leader changing the league leader alliance.
3. Verify state changes and packet intent order.
4. Keep permission/error cases separate unless Java source makes them unavoidable.
5. Continue marking packet/client parity as needing verification until golden/runtime proof exists.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6FF-Completion.md`
   - this handoff
3. Inspect Java source for command `32` before touching C#.
4. Implement one narrow unit with Java breadcrumbs.
5. Add focused tests that state what is source-derived and what remains unverified.
6. Run focused tests, then full GameServer tests.
7. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
8. Commit the unit.
