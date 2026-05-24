# Phase 6ES Completion Handoff

Created: May 23, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6ER and covers Session 637.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerStatusInfo|PlayerAllianceRuntime|PlayerAllianceMemberInfo|BaseLeavePlanner"`
  - Result: Passed, 89 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1103 tests.

## Recent Work Completed

### Session 637 - League Leave Null-League Boundary

- Source-read Java `PlayerTeamCommandService.executeCommand`, `TeamCommand`, and `LeagueService.removeAlliance`.
- Added the Java-derived `LEAGUE_LEAVE` prerequisite boundary in `GameServerConnection.HandlePlayerStatusInfoAsync`: no current alliance remains a no-op, but an alliance with no league now fails with `League should not be null`.
- Added parsed-command regression coverage for command id `29` on a player in an alliance without a league, confirming no packets are sent and alliance membership is unchanged.
- Intentional C# difference: Java throws `NullPointerException` via `Objects.requireNonNull`; C# throws `InvalidOperationException` with the same message because the local packet-handler boundary uses C# exception idioms.
- Commit: this handoff is included in `Gate league leave missing league`.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_PLAYER_STATUS_INFO` | `Aion.GameServer.Network.Aion.ClientPackets.CmPlayerStatusInfo` / `Aion.GameServer.Network.Aion.GameServerConnection.HandlePlayerStatusInfoAsync` | Client Packet / Handler Boundary | Partial | Regression Tested | Needs Verification | Command id `29` preserves the Java no-team no-op and alliance-without-league fail-fast boundary. Actual league leave is still not ported. |
| `com.aionemu.gameserver.model.team.common.events.TeamCommand.LEAGUE_LEAVE` | Command code `29` branch in `GameServerConnection.HandlePlayerStatusInfoAsync` | Enum / Command Mapping | Partial | Regression Tested | Needs Verification | Recognized valid command has one Java prerequisite branch covered. League membership mutation, fanout, disband, and leader fallback remain deferred. |
| `com.aionemu.gameserver.model.team.common.service.PlayerTeamCommandService` | Existing `GameServerConnection.HandlePlayerStatusInfoAsync` branch dispatch | Service Dependency | Partial | Regression Tested | Needs Verification | C# still uses manual branch dispatch, but now mirrors the Java `LEAGUE_LEAVE -> LeagueService.removeAlliance(player.getPlayerAlliance())` null-league boundary. Full generic service dispatch remains unported. |
| `com.aionemu.gameserver.model.team.league.LeagueService.removeAlliance` | `GameServerConnection.HandlePlayerStatusInfoAsync` command `29` precondition branch | Service / Runtime Bridge | Partial | Regression Tested | Intentional Difference | Java throws `NullPointerException("League should not be null")` when an alliance has no league; C# throws `InvalidOperationException` with the same message. Real league removal is not implemented. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAlliance` | `Aion.GameServer.Model.GameObjects.PlayerAllianceDescriptor` / `PlayerAllianceRuntime.Resolve` | Team State Dependency | Partial | Regression Tested | Needs Verification | C# resolves current alliance state to decide whether Java would enter `LeagueService.removeAlliance`. Java static alliance registry and league pointer behavior are not runtime-compared. |
| `com.aionemu.gameserver.model.team.league.League` | Deferred C# league runtime | Team State Dependency | Not Started | No Tests | Unknown | No C# league model exists yet. This unit documents and tests only the null-league prerequisite. |
| `com.aionemu.gameserver.model.team.league.events.LeagueLeftEvent` | Deferred C# league leave event workflow | Event Runtime Dependency | Not Started | No Tests | Unknown | Java event behavior for real league leave remains unported, including member removal, broadcasts, disband, and packet ordering. |
| `com.aionemu.gameserver.utils.PacketSendUtility` | `IGameClientConnectionRegistry` / no-send assertion | Runtime Dependency | Partial | Regression Tested | Needs Verification | Regression confirms the null-league failure sends no packets. Live packet processor exception handling/logging remains unverified. |

## Tests Added

- `GameServerConnectionPlayerStatusInfoTests.HandlePlayerStatusInfoAsync_LeagueLeaveWithoutLeagueThrowsLikeJava`
  - Validates parsed command id `29` on a player in an alliance without a league throws `InvalidOperationException("League should not be null")`, leaves alliance members unchanged, and sends no packets.
  - This test is source-derived. It does not compare against Java runtime execution, Java-generated golden vectors, packet captures, encrypted frames, or a live client.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 8
- Total artifacts ported or partially modeled in this handoff window: 1 `CM_PLAYER_STATUS_INFO` / `LEAGUE_LEAVE` null-league prerequisite branch plus 1 regression test.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 5
- Total blocked/not-started artifacts: real league runtime, `LeagueLeftEvent`, league broadcast/disband/leader reassignment, Java static league registry, packet processor exception/log comparison, encoded opcode/frame golden validation, packet capture comparison, and real-client validation.
- Estimated overall migration completion: 64%

The percentage stays conservative. A league prerequisite boundary is aligned, but real league behavior remains a sizable deferred subsystem.

## Remaining Risks

- Actual C# league runtime is still absent; `LEAGUE_LEAVE`, `LEAGUE_EXPEL`, and `LEAGUE_SET_LEADER` remain only partially bounded or no-op outside non-league prerequisite behavior.
- Java `LeagueLeftEvent`, league broadcast, league disband, league leader reassignment, alliance info fanout, and static `LeagueService.leagues` registry behavior remain unported.
- Java exception type differs intentionally for the null-league branch; only the message and no-send/no-mutation behavior are mirrored.
- Java packet processor exception handling/logging for this branch has not been runtime-compared.
- Java alliance/league event queue, lock, iteration ordering, offline-recipient behavior, and threading behavior remain source-derived only.
- Java golden byte vectors, encrypted opcode/frame validation, packet capture comparison, and real-client validation remain unavailable.

## Next Recommended Unit of Work

Continue `CM_PLAYER_STATUS_INFO` parity in this order:

1. Source-read and bound `LEAGUE_EXPEL` with no active league and invalid league-alliance prerequisites.
2. Source-read `LEAGUE_SET_LEADER`; Java no-ops when the current alliance has no league, but real leader reassignment remains deferred.
3. Document `LEAGUE_ALLIANCE_MOVE`: it is recognized by `TeamCommand` but not dispatched by `PlayerTeamCommandService` in the current Java source.
4. Keep production league mutations out until a minimal league runtime bridge exists.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6ER-Completion.md`
   - this handoff
3. Inspect Java source for the selected branch before touching C#.
4. Implement one narrow unit with Java breadcrumbs.
5. Add focused tests that state what is source-derived and what remains unverified.
6. Run focused tests, then full GameServer tests.
7. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
8. Commit the unit.
