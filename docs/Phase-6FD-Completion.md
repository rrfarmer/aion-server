# Phase 6FD Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6FC and covers Session 648.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter GameServerConnectionPlayerStatusInfoTests`
  - Result: Passed, 48 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1121 tests.

## Recent Work Completed

### Session 648 - Command 29 League Leave/Disband Slice

- Source-read Java `PlayerTeamCommandService.executeCommand`, `LeagueService.removeAlliance`, `LeagueLeftEvent`, `LeagueDisbandEvent`, `League.reorganize`, `SM_ALLIANCE_INFO`, and `SM_SYSTEM_MESSAGE.STR_UNION_CHANGE_LEADER_TIMEOUT`.
- Routed command `29` through `PlayerLeagueRuntime.RemoveAlliance`.
- Added the first C# normal league-leave state transition:
  - remove the leaving alliance from league state,
  - compact remaining positions like Java `League.reorganize`,
  - update the league leader when position zero is vacated,
  - clear league state when Java `shouldDisband()` would trigger at one remaining alliance.
- Added packet intents for the covered Java order:
  - remaining alliance gets `SM_ALLIANCE_INFO.LEAGUE_LEFT_HIM`,
  - remaining alliance gets `STR_UNION_CHANGE_LEADER_TIMEOUT` if the leader alliance left,
  - leaving alliance gets `SM_ALLIANCE_INFO.LEAGUE_LEFT_ME`,
  - remaining alliance gets `SM_ALLIANCE_INFO.LEAGUE_DISPERSED` when normal leave triggers disband.
- Added `SmSystemMessage.UnionChangeLeaderTimeout` with Java message id `1400588`.
- Added league leave/disperse message constants to `PlayerAllianceInfoPacketPlan`.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_PLAYER_STATUS_INFO` | `GameServerConnection.HandlePlayerStatusInfoAsync` command `29` path | Client Packet / Handler Boundary | Partial | Regression Tested | Needs Verification | Command `29` reaches C# league leave runtime when the player has an alliance. Missing-league behavior remains covered. |
| `com.aionemu.gameserver.model.team.common.events.TeamCommand.LEAGUE_LEAVE` | Command code `29` branch | Enum / Command Mapping | Partial | Regression Tested | Needs Verification | Normal leave is modeled for the two-alliance leader-leave/disband path. |
| `com.aionemu.gameserver.model.team.common.service.PlayerTeamCommandService` | Manual command branches in `GameServerConnection.HandlePlayerStatusInfoAsync` | Service / Dispatcher | Partial | Regression Tested | Needs Verification | C# keeps explicit branches rather than a full team-command dispatcher. Behavior is tested only for covered branches. |
| `com.aionemu.gameserver.model.team.league.LeagueService.removeAlliance` | `PlayerLeagueRuntime.RemoveAlliance` | Service / Runtime Bridge | Partial | Regression Tested | Needs Verification | Removes alliance state, reorganizes positions, emits packet intents, and disbands at one remaining alliance. |
| `com.aionemu.gameserver.model.team.league.League` | `PlayerLeagueRuntime` / `PlayerLeagueSnapshot` | Team State | Partial | Regression Tested | Needs Verification | Reorganize compaction and leader update are modeled for removal. Full lifecycle remains unported. |
| `com.aionemu.gameserver.model.team.league.LeagueMember` | Internal `PlayerLeagueRuntime.PlayerLeagueMember` | Team Member | Partial | Regression Tested | Needs Verification | Tracks alliance id and league position for removal/reorganize. Java object references are approximated by ids. |
| `com.aionemu.gameserver.model.team.league.events.LeagueLeftEvent` | `PlayerLeagueRuntime.RemoveAlliance` / `PlayerLeaguePacketIntent` | Event Runtime Dependency | Partial | Regression Tested | Needs Verification | LEAVE path is modeled for the covered ordering. EXPEL and broader DISBAND behavior remain deferred. |
| `com.aionemu.gameserver.model.team.league.events.LeagueDisbandEvent` | `PlayerLeagueRuntime.RemoveAlliance` disband branch | Event Runtime Dependency | Partial | Regression Tested | Needs Verification | Normal leave can emit `LEAGUE_DISPERSED` and clear league state. Direct disband iteration remains unported. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ALLIANCE_INFO` | `SmAllianceInfo` / `PlayerAllianceInfoPacketPlan` | Server Packet | Partial | Regression Tested | Needs Verification | Command `29` tests serialize emitted `LEAGUE_LEFT_HIM`, `LEAGUE_LEFT_ME`, and `LEAGUE_DISPERSED` packets. No Java golden bytes. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `SmSystemMessage` | Server Packet | Partial | Regression Tested | Needs Verification | Added and tested `STR_UNION_CHANGE_LEADER_TIMEOUT` (`1400588`). No Java golden-byte comparison. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAlliance` | `PlayerAllianceRuntime` | Team State Dependency | Partial | Regression Tested | Needs Verification | Supplies current alliance, recipients, snapshots, leader names, and active map ids. No live Java league pointer exists. |
| `com.aionemu.gameserver.utils.PacketSendUtility` | `IGameClientConnectionRegistry` / `GameServerConnection.SendLeaguePacketAsync` | Runtime Dependency | Partial | Regression Tested | Needs Verification | In-memory registry validates recipient order and emitted payload fields. Live socket/client behavior remains unverified. |

## Tests Added Or Updated

- `GameServerConnectionPlayerStatusInfoTests.HandlePlayerStatusInfoAsync_LeagueLeaveLeaderReorganizesAndDisbandsLikeJava`
  - Validates command `29` for a two-alliance league where the leader alliance leaves.
  - Checks league state removal for both alliances.
  - Checks recipient order.
  - Serializes emitted `SmAllianceInfo` packets for `LEAGUE_LEFT_HIM`, `LEAGUE_LEFT_ME`, and `LEAGUE_DISPERSED`.
  - Decodes emitted `SmSystemMessage(1400588)` for the new leader name.
- `GameServerConnectionPlayerStatusInfoTests.HandlePlayerStatusInfoAsync_LeagueLeaveWithoutLeagueThrowsLikeJava`
  - Still validates missing-league failure and no-send behavior.
- Java comparison status: expectations are source-derived from Java source. No Java runtime execution, Java golden vectors, encrypted frames, packet captures, or live-client validation were run.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 12
- Total artifacts ported or partially modeled in this handoff window: 1 command `29` normal leave/disband runtime slice plus 1 system-message factory and 1 regression test.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 10
- Total blocked/not-started artifacts: Java golden byte validation, full `LeagueService`, Java static league registry, `LEAGUE_EXPEL`, `LEAGUE_SET_LEADER`, Java event queue/lock comparison, packet processor exception/log comparison, and client validation.
- Estimated overall migration completion: 64%

The percentage stays conservative. The league runtime is gaining command coverage, but Java/runtime proof and the broader league lifecycle remain open.

## Remaining Risks

- Command `29` coverage is strongest for the two-alliance leader-leave/disband path. Three-plus-alliance normal leave, non-leader leave, offline recipients, and direct disband iteration need more tests.
- `LEAGUE_EXPEL` command `30`, `LEAGUE_SET_LEADER` command `32`, invite/join/loot/kinah workflows, and full Java static `LeagueService.leagues` behavior remain unported.
- C# uses deterministic sorted positions for packet intents; Java `ConcurrentHashMap` iteration order is not runtime-compared.
- Packet-field coverage is C# emitted-object validation only; Java golden bytes, encrypted frames, packet captures, and live-client validation remain unavailable.
- Java locks/event queue/threading, object identity, packet processor exception/log behavior, and offline send behavior remain deferred.
- Reflection, precision/rounding, and date/time handling are not involved in this unit.

## Next Recommended Unit of Work

Continue command `29` before moving to expel:

1. Add a three-alliance non-leader leave case.
2. Verify reorganization without disband.
3. Verify remaining league rows still include the remaining alliances in compacted position order.
4. Verify no `STR_UNION_CHANGE_LEADER_TIMEOUT` is sent when the leader alliance remains at position zero.
5. Then move to command `30` `LEAGUE_EXPEL` using the same leave-event machinery.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6FC-Completion.md`
   - this handoff
3. Inspect Java source for the selected command/event before touching C#.
4. Implement one narrow unit with Java breadcrumbs.
5. Add focused tests that state what is source-derived and what remains unverified.
6. Run focused tests, then full GameServer tests.
7. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
8. Commit the unit.
