# Phase 6EZ Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6EY and covers Session 644.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerStatusInfo|PlayerAllianceRuntime|PlayerAllianceMemberInfo|BaseLeavePlanner|PlayerLeagueRuntime"`
  - Result: Passed, 106 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1120 tests.

## Recent Work Completed

### Session 644 - Minimal League Runtime Bridge For Command 31

- Source-read Java `LeagueService.moveAlliance`, `League`, `LeagueMember`, and `LeagueMoveEvent`.
- Added `PlayerLeagueRuntime`, a narrow C# state bridge for league id, leader alliance id, alliance membership, and Java-style league positions.
- Wired `GameServerConnection` command `31` to use `PlayerLeagueRuntime.MoveAlliance` after resolving the caller's current alliance.
- Modeled Java's leader-gated move behavior: only the player who leads the league leader alliance dispatches the move; leaders of non-leader alliances no-op.
- Added missing target-alliance and position-swap regression coverage. Packet fanout from Java `LeagueMoveEvent` remains intentionally deferred, so successful moves currently send no packets.
- Commit: this handoff should be included with the unit commit.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_PLAYER_STATUS_INFO` | `Aion.GameServer.Network.Aion.ClientPackets.CmPlayerStatusInfo` / `Aion.GameServer.Network.Aion.GameServerConnection.HandlePlayerStatusInfoAsync` | Client Packet / Handler Boundary | Partial | Regression Tested | Needs Verification | Command `31` now delegates to league state when available. Successful move packet fanout is still unsupported. |
| `com.aionemu.gameserver.model.team.common.events.TeamCommand.LEAGUE_ALLIANCE_MOVE` | Command code `31` branch in `GameServerConnection.HandlePlayerStatusInfoAsync` | Enum / Command Mapping | Partial | Regression Tested | Needs Verification | Covers missing league, non-league-leader no-op, missing target alliance, and position swap. Real fanout remains deferred. |
| `com.aionemu.gameserver.model.team.league.LeagueService.moveAlliance` | `Aion.GameServer.Services.PlayerLeagueRuntime.MoveAlliance` plus command `31` branch | Service / Runtime Bridge | Partial | Regression Tested | Needs Verification | Leader gate and position swap are modeled. Java null-dereference behavior and packet processor behavior are not runtime-compared. |
| `com.aionemu.gameserver.model.team.league.League` | `Aion.GameServer.Services.PlayerLeagueRuntime` / `PlayerLeagueSnapshot` | Team State | Partial | Regression Tested | Needs Verification | Minimal league id, leader alliance id, member ids, and positions only. Disband, leave, join invite, loot rules, broadcasts, and player lookup remain unported. |
| `com.aionemu.gameserver.model.team.league.LeagueMember` | Internal `PlayerLeagueRuntime.PlayerLeagueMember` | Team Member | Partial | Regression Tested | Needs Verification | Tracks alliance id and league position only. Java object identity/name/alliance reference behavior is approximated by ids. |
| `com.aionemu.gameserver.model.team.league.events.LeagueMoveEvent` | `PlayerLeagueRuntime.MoveAlliance` | Event Runtime Dependency | Partial | Regression Tested | Partial Parity | Position swap and leader-gated dispatch are modeled. `SM_ALLIANCE_INFO` and force-number system messages are missing. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAlliance` | `PlayerAllianceRuntime.Resolve` / `PlayerAllianceRuntime.GetDescriptor` | Team State Dependency | Partial | Regression Tested | Needs Verification | Used for caller alliance resolution and league-leader player check. No live Java league pointer exists in C#. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ALLIANCE_INFO` | Deferred C# league fanout from command `31` | Server Packet | Not Started | No Tests | Unknown | Java sends one alliance-info packet per alliance after a move. C# sends none in this unit. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | Deferred C# force-number fanout from command `31` | Server Packet | Not Started | No Tests | Unknown | Java sends `STR_UNION_CHANGE_FORCE_NUMBER_ME/HIM` after a move. C# sends none in this unit. |
| `com.aionemu.gameserver.utils.PacketSendUtility` | `IGameClientConnectionRegistry` / no-send assertions | Runtime Dependency | Partial | Regression Tested | Needs Verification | Tests assert no packets until fanout lands. Live socket ordering remains unverified. |

## Tests Added

- `GameServerConnectionPlayerStatusInfoTests.HandlePlayerStatusInfoAsync_LeagueAllianceMoveByNonLeagueLeaderNoopsLikeJava`
  - Validates command `31` from a non-league-leader alliance leader leaves positions unchanged and sends no packets.
- `GameServerConnectionPlayerStatusInfoTests.HandlePlayerStatusInfoAsync_LeagueAllianceMoveMissingTargetThrowsLikeJavaEventBoundary`
  - Validates command `31` from the league leader with a missing target alliance throws `InvalidOperationException("League member should not be null: 88999")`, leaves positions unchanged, and sends no packets.
- `GameServerConnectionPlayerStatusInfoTests.HandlePlayerStatusInfoAsync_LeagueAllianceMoveSwapsPositionsWithoutFanoutUntilLeaguePacketsPorted`
  - Validates command `31` from the league leader swaps selected/target league positions and sends no packets while fanout is deferred.
- These tests are source-derived. They do not compare against Java runtime execution, Java-generated golden vectors, packet captures, encrypted frames, or a live client.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 10
- Total artifacts ported or partially modeled in this handoff window: 1 minimal league runtime bridge plus command `31` leader/missing-target/position-swap behavior and 3 regression tests.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 6
- Total blocked/not-started artifacts: `LeagueMoveEvent` packet fanout, full `LeagueService`, Java static league registry, league leave/disband/join/change-leader workflows, Java event queue/lock comparison, packet processor exception/log comparison, encoded opcode/frame golden validation, and client validation.
- Estimated overall migration completion: 64%

The percentage stays conservative. League state work has started, but league event and packet parity remains a substantial subsystem.

## Remaining Risks

- Successful command `31` moves currently omit Java's `SM_ALLIANCE_INFO` and `SM_SYSTEM_MESSAGE` fanout.
- `PlayerLeagueRuntime` is a narrow state bridge, not a full port of Java `League`, `LeagueService`, or league events.
- Java null-dereference exception types/messages for missing selected/target league members are not mirrored exactly.
- Java static `LeagueService.leagues`, league leave/disband/join/invite/change-leader/loot/kinah workflows, event queue, locks, object identity, iteration ordering, and threading behavior remain source-derived or deferred.
- Java packet bytes, encrypted opcode/frame validation, packet capture comparison, packet processor exception/log comparison, and real-client validation remain unavailable.

## Next Recommended Unit of Work

Continue Phase 6 in this order:

1. Add the smallest command `31` `LeagueMoveEvent` fanout planner.
2. Assert Java order: for each league alliance, send `SM_ALLIANCE_INFO`, then selected-force-number message, then target-force-number message.
3. Keep byte-golden and live-client validation deferred until the C# packet payloads for league rows are fully ported.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6EY-Completion.md`
   - this handoff
3. Inspect Java source for the selected branch before touching C#.
4. Implement one narrow unit with Java breadcrumbs.
5. Add focused tests that state what is source-derived and what remains unverified.
6. Run focused tests, then full GameServer tests.
7. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
8. Commit the unit.
