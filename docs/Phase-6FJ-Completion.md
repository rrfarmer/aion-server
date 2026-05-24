# Phase 6FJ Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6FI and covers Session 654.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter GameServerConnectionPlayerStatusInfoTests`
  - Result: Passed, 58 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1131 tests.

## Recent Work Completed

### Session 654 - Command 32 Non-League-Leader Caller Behavior

- Added coverage for Java's permissive command `32` behavior where a non-league-leader alliance leader can initiate league leader change.
- Preserved actual `LeagueMember.getLeaguePosition()` values in set-leader `SM_ALLIANCE_INFO` rows, including duplicate positions.
- Added a regression where:
  - old league leader remains at position `0`,
  - target alliance also becomes position `0`,
  - caller alliance moves to the target's previous position,
  - serialized league rows include duplicate `0` positions.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_PLAYER_STATUS_INFO` | `GameServerConnection.HandlePlayerStatusInfoAsync` command `32` path | Client Packet / Handler Boundary | Partial | Regression Tested | Needs Verification | Command `32` includes non-league-leader caller behavior. |
| `com.aionemu.gameserver.model.team.common.events.TeamCommand.LEAGUE_SET_LEADER` | Command code `32` branch | Enum / Command Mapping | Partial | Regression Tested | Needs Verification | Java's permissive caller behavior is captured. |
| `com.aionemu.gameserver.model.team.league.LeagueService.setLeader` | `PlayerLeagueRuntime.SetLeader` | Service / Runtime Bridge | Partial | Regression Tested | Needs Verification | C# permits non-league-leader caller behavior like Java source. |
| `com.aionemu.gameserver.model.team.league.events.LeagueChangeLeaderEvent` | `PlayerLeagueRuntime.SetLeader` / `PlayerLeaguePacketIntent` | Event Runtime Dependency | Partial | Regression Tested | Needs Verification | Duplicate-position set-leader fanout is covered. Target-offline/no-op remains. |
| `com.aionemu.gameserver.model.team.league.League` | `PlayerLeagueRuntime` / `PlayerLeagueSnapshot` | Team State | Partial | Regression Tested | Needs Verification | Actual positions, including duplicates, are preserved for set-leader row serialization. |
| `com.aionemu.gameserver.model.team.league.LeagueMember` | Internal `PlayerLeagueRuntime.PlayerLeagueMember` / `PlayerAllianceInfoLeagueRow` | Team Member / DTO | Partial | Regression Tested | Needs Verification | Row position can now use actual league position instead of row index. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ALLIANCE_INFO` | `SmAllianceInfo` / `PlayerAllianceInfoPacketPlan` | Server Packet | Partial | Regression Tested | Needs Verification | Set-leader edge test serializes duplicate-position rows. No Java golden bytes. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `SmSystemMessage` | Server Packet | Partial | Regression Tested | Needs Verification | Force-number-him and change-leader fanout is covered for this edge. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAlliance` | `PlayerAllianceRuntime` | Team State Dependency | Partial | Regression Tested | Needs Verification | Supplies caller/target leaders and snapshots. |
| `com.aionemu.gameserver.utils.PacketSendUtility` | `IGameClientConnectionRegistry` / `GameServerConnection.SendLeaguePacketAsync` | Runtime Dependency | Partial | Regression Tested | Needs Verification | In-memory recipient order and payloads are tested. |

## Tests Added

- `GameServerConnectionPlayerStatusInfoTests.HandlePlayerStatusInfoAsync_LeagueSetLeaderByNonLeagueLeaderMatchesJavaEvent`
  - Validates non-league-leader caller behavior for command `32`.
  - Checks duplicate positions and serialized row positions.
  - Checks system-message fanout.
- Java comparison status: expectations are source-derived from Java source. No Java runtime execution, Java golden vectors, encrypted frames, packet captures, or live-client validation were run.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 10
- Total artifacts ported or partially modeled in this handoff window: 1 command `32` non-league-leader edge runtime/serialization slice plus 1 regression test.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 9
- Total blocked/not-started artifacts: Java golden byte validation, full `LeagueService`, Java static league registry, target-offline command `32` behavior, Java event queue/lock comparison, packet processor exception/log comparison, and client validation.
- Estimated overall migration completion: 64%

The percentage stays conservative. Command `32` source behavior is more accurately bounded, but wider league lifecycle parity remains open.

## Remaining Risks

- Command `32` target-offline/no-op remains untested; current runtime resolves the target alliance leader from online fixtures.
- League invite/join/loot/kinah workflows, direct disband iteration, offline recipients, Java static registry, event queue/locks, object identity, and packet processor exception/log behavior remain deferred.
- Packet-field coverage remains C# emitted-object validation only; Java golden bytes, encrypted frames, packet captures, and live-client validation remain unavailable.
- C# packet intent order is deterministic by sorted positions with stable list ordering for duplicate positions; Java `ConcurrentHashMap` ordering for duplicate-position rows remains unverified.
- Reflection, precision/rounding, and date/time handling are not involved in this unit.

## Next Recommended Unit of Work

Move from league command gates into the next smallest league lifecycle feature:

1. Source-read `LeagueJoinEvent` and `LeagueInviteEvent`.
2. Begin with a state-only join/add-alliance packet-intent slice if dependencies are manageable.
3. Alternatively source-read `LeagueLootRulesChangeEvent` if loot-rule changes are more isolated.
4. Keep every new slice source-derived with packet/state tests and explicit Java-golden/client gaps.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6FI-Completion.md`
   - this handoff
3. Inspect Java source for the selected league lifecycle feature before touching C#.
4. Implement one narrow unit with Java breadcrumbs.
5. Add focused tests that state what is source-derived and what remains unverified.
6. Run focused tests, then full GameServer tests.
7. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
8. Commit the unit.
