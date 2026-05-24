# Phase 6FE Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6FD and covers Session 649.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter GameServerConnectionPlayerStatusInfoTests`
  - Result: Passed, 49 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1122 tests.

## Recent Work Completed

### Session 649 - Command 29 Non-Disband Leave Coverage

- Added a three-alliance non-leader league leave regression.
- Verified reorganization without disband:
  - league leader alliance remains at position `0`,
  - later alliance compacts from position `2` to `1`,
  - leaving alliance is removed from league lookup,
  - league remains active for the other two alliances.
- Verified packet fanout:
  - remaining alliances receive `SM_ALLIANCE_INFO.LEAGUE_LEFT_HIM`,
  - leaving alliance receives `SM_ALLIANCE_INFO.LEAGUE_LEFT_ME`,
  - no `STR_UNION_CHANGE_LEADER_TIMEOUT` is emitted,
  - no `SM_ALLIANCE_INFO.LEAGUE_DISPERSED` is emitted.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_PLAYER_STATUS_INFO` | `GameServerConnection.HandlePlayerStatusInfoAsync` command `29` path | Client Packet / Handler Boundary | Partial | Regression Tested | Needs Verification | Command `29` has coverage for disband and non-disband normal leave paths. |
| `com.aionemu.gameserver.model.team.common.events.TeamCommand.LEAGUE_LEAVE` | Command code `29` branch | Enum / Command Mapping | Partial | Regression Tested | Needs Verification | Non-leader leave verifies compacted positions and absence of disband/leader-timeout packets. |
| `com.aionemu.gameserver.model.team.league.LeagueService.removeAlliance` | `PlayerLeagueRuntime.RemoveAlliance` | Service / Runtime Bridge | Partial | Regression Tested | Needs Verification | Normal leave with and without disband is tested. Java static registry/event behavior remains unverified. |
| `com.aionemu.gameserver.model.team.league.League` | `PlayerLeagueRuntime` / `PlayerLeagueSnapshot` | Team State | Partial | Regression Tested | Needs Verification | Reorganize compaction is covered when position zero remains occupied and when it is vacated. |
| `com.aionemu.gameserver.model.team.league.LeagueMember` | Internal `PlayerLeagueRuntime.PlayerLeagueMember` | Team Member | Partial | Regression Tested | Needs Verification | Middle-alliance removal and compaction are tested. Java object references are approximated by ids. |
| `com.aionemu.gameserver.model.team.league.events.LeagueLeftEvent` | `PlayerLeagueRuntime.RemoveAlliance` / `PlayerLeaguePacketIntent` | Event Runtime Dependency | Partial | Regression Tested | Needs Verification | Normal LEAVE path covers disband and non-disband branches. EXPEL remains unported. |
| `com.aionemu.gameserver.model.team.league.events.LeagueDisbandEvent` | `PlayerLeagueRuntime.RemoveAlliance` disband branch | Event Runtime Dependency | Partial | Regression Tested | Needs Verification | Test verifies disband does not trigger with two remaining alliances. Direct disband remains unported. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ALLIANCE_INFO` | `SmAllianceInfo` / `PlayerAllianceInfoPacketPlan` | Server Packet | Partial | Regression Tested | Needs Verification | Non-disband leave serializes compacted league rows and leaving-alliance no-league packet. No Java golden bytes. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `SmSystemMessage` | Server Packet | Partial | Regression Tested | Needs Verification | Absence of leader-timeout system message is asserted when leader alliance remains. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAlliance` | `PlayerAllianceRuntime` | Team State Dependency | Partial | Regression Tested | Needs Verification | Supplies three alliance snapshots and recipient ids for the no-disband path. |
| `com.aionemu.gameserver.utils.PacketSendUtility` | `IGameClientConnectionRegistry` / `GameServerConnection.SendLeaguePacketAsync` | Runtime Dependency | Partial | Regression Tested | Needs Verification | In-memory registry verifies recipient order and absence of extra packets. |

## Tests Added Or Updated

- `GameServerConnectionPlayerStatusInfoTests.HandlePlayerStatusInfoAsync_LeagueLeaveNonLeaderCompactsWithoutDisbandLikeJava`
  - Validates command `29` for a three-alliance league where the middle/non-leader alliance leaves.
  - Checks remaining positions, active/removed league lookup, recipient order, compacted league rows, and absence of disband/leader-timeout packets.
- Java comparison status: expectations are source-derived from Java source. No Java runtime execution, Java golden vectors, encrypted frames, packet captures, or live-client validation were run.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 11
- Total artifacts ported or partially modeled in this handoff window: 1 command `29` non-disband regression slice.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 9
- Total blocked/not-started artifacts: Java golden byte validation, full `LeagueService`, Java static league registry, `LEAGUE_EXPEL`, `LEAGUE_SET_LEADER`, Java event queue/lock comparison, packet processor exception/log comparison, and client validation.
- Estimated overall migration completion: 64%

The percentage stays conservative. Command `29` is better bounded, but expel and the broader league lifecycle remain open.

## Remaining Risks

- `LEAGUE_EXPEL` command `30` is still not ported beyond missing-league prerequisite behavior.
- `LEAGUE_SET_LEADER` command `32`, invite/join/loot/kinah workflows, direct disband iteration, offline recipients, Java static registry, event queue/locks, object identity, and packet processor exception/log behavior remain deferred.
- Packet-field coverage is C# emitted-object validation only; Java golden bytes, encrypted frames, packet captures, and live-client validation remain unavailable.
- C# packet intent order is deterministic by sorted positions; Java `ConcurrentHashMap` ordering remains unverified.
- Reflection, precision/rounding, and date/time handling are not involved in this unit.

## Next Recommended Unit of Work

Start command `30` `LEAGUE_EXPEL`:

1. Source-read `PlayerTeamCommandService.findLeagueAlliance` and `LeagueService.expelAlliance`.
2. Add runtime support for EXPEL using the existing remove/reorganize machinery.
3. Add a leader-expels-nonleader test first.
4. Verify permission failures:
   - caller must lead their own alliance,
   - caller alliance must be the league leader alliance,
   - target alliance must exist in the league.
5. Use EXPEL packet message ids: `SM_ALLIANCE_INFO.LEAGUE_EXPEL` and `SM_ALLIANCE_INFO.LEAGUE_EXPELLED`.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6FD-Completion.md`
   - this handoff
3. Inspect Java source for command `30` before touching C#.
4. Implement one narrow unit with Java breadcrumbs.
5. Add focused tests that state what is source-derived and what remains unverified.
6. Run focused tests, then full GameServer tests.
7. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
8. Commit the unit.
