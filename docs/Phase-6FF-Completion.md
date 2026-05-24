# Phase 6FF Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6FE and covers Session 650.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter GameServerConnectionPlayerStatusInfoTests`
  - Result: Passed, 50 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1123 tests.

## Recent Work Completed

### Session 650 - Command 30 Leader Expel Slice

- Source-read Java `PlayerTeamCommandService.findLeagueAlliance` and `LeagueService.expelAlliance`.
- Routed command `30` through `PlayerLeagueRuntime.ExpelAlliance`.
- Reused the league leave removal/reorganize machinery for EXPEL:
  - remaining alliances receive `SM_ALLIANCE_INFO.LEAGUE_EXPEL`,
  - expelled alliance receives `SM_ALLIANCE_INFO.LEAGUE_EXPELLED`,
  - remaining league rows compact in Java position order.
- Added `LEAGUE_EXPEL` (`1400574`) and `LEAGUE_EXPELLED` (`1400576`) message constants.
- Implemented source-derived permission boundaries in runtime:
  - target alliance lookup,
  - caller must lead their own alliance,
  - caller alliance must be the league leader alliance.
- This unit tests the successful leader-expels-nonleader path. Permission/error branches are the recommended next work.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_PLAYER_STATUS_INFO` | `GameServerConnection.HandlePlayerStatusInfoAsync` command `30` path | Client Packet / Handler Boundary | Partial | Regression Tested | Needs Verification | Command `30` reaches C# expel runtime for active leagues. |
| `com.aionemu.gameserver.model.team.common.events.TeamCommand.LEAGUE_EXPEL` | Command code `30` branch | Enum / Command Mapping | Partial | Regression Tested | Needs Verification | Successful leader-expels-nonleader branch is modeled. |
| `com.aionemu.gameserver.model.team.common.service.PlayerTeamCommandService` | Manual command branches in `GameServerConnection.HandlePlayerStatusInfoAsync` | Service / Dispatcher | Partial | Regression Tested | Needs Verification | C# mirrors lookup/check ordering inside `PlayerLeagueRuntime.ExpelAlliance`, but still lacks a generic dispatcher. |
| `com.aionemu.gameserver.model.team.league.LeagueService.expelAlliance` | `PlayerLeagueRuntime.ExpelAlliance` | Service / Runtime Bridge | Partial | Regression Tested | Needs Verification | Successful expel removes target alliance, compacts positions, and emits EXPEL/EXPELLED packet intents. |
| `com.aionemu.gameserver.model.team.league.LeagueService.removeAlliance` | Shared `PlayerLeagueRuntime.RemoveAllianceCore` | Service / Runtime Bridge | Partial | Regression Tested | Needs Verification | Normal leave and expel share removal mechanics. Java registry/event behavior remains unverified. |
| `com.aionemu.gameserver.model.team.league.League` | `PlayerLeagueRuntime` / `PlayerLeagueSnapshot` | Team State | Partial | Regression Tested | Needs Verification | Expel test covers compacting a three-alliance league after target removal. |
| `com.aionemu.gameserver.model.team.league.LeagueMember` | Internal `PlayerLeagueRuntime.PlayerLeagueMember` | Team Member | Partial | Regression Tested | Needs Verification | Target lookup and position compaction are modeled by alliance id. |
| `com.aionemu.gameserver.model.team.league.events.LeagueLeftEvent` | `PlayerLeagueRuntime.ExpelAlliance` / `PlayerLeaguePacketIntent` | Event Runtime Dependency | Partial | Regression Tested | Needs Verification | EXPEL reason is modeled for the first success path. Disband and permission branches remain. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ALLIANCE_INFO` | `SmAllianceInfo` / `PlayerAllianceInfoPacketPlan` | Server Packet | Partial | Regression Tested | Needs Verification | Expel test serializes emitted `LEAGUE_EXPEL` and `LEAGUE_EXPELLED` packets. No Java golden bytes. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAlliance` | `PlayerAllianceRuntime` | Team State Dependency | Partial | Regression Tested | Needs Verification | Supplies leader checks, names, snapshots, recipients, and map ids. |
| `com.aionemu.gameserver.utils.PacketSendUtility` | `IGameClientConnectionRegistry` / `GameServerConnection.SendLeaguePacketAsync` | Runtime Dependency | Partial | Regression Tested | Needs Verification | In-memory registry verifies recipient order and payloads. |

## Tests Added Or Updated

- `GameServerConnectionPlayerStatusInfoTests.HandlePlayerStatusInfoAsync_LeagueExpelByLeaderRemovesTargetLikeJava`
  - Validates command `30` for a three-alliance league where the league leader expels a non-leader alliance.
  - Checks remaining positions, removed target lookup, recipient order, serialized `LEAGUE_EXPEL` rows, and serialized `LEAGUE_EXPELLED`.
- Java comparison status: expectations are source-derived from Java source. No Java runtime execution, Java golden vectors, encrypted frames, packet captures, or live-client validation were run.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 11
- Total artifacts ported or partially modeled in this handoff window: 1 command `30` successful expel runtime slice plus 1 regression test.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 9
- Total blocked/not-started artifacts: Java golden byte validation, full `LeagueService`, Java static league registry, command `30` failure branches, `LEAGUE_SET_LEADER`, Java event queue/lock comparison, packet processor exception/log comparison, and client validation.
- Estimated overall migration completion: 64%

The percentage stays conservative. League expel has started, but permission/error branches and wider lifecycle parity remain open.

## Remaining Risks

- Command `30` permission failure branches are implemented but not yet tested:
  - invalid target alliance,
  - caller not their alliance leader,
  - caller alliance not the league leader alliance.
- EXPEL disband behavior and expelling the last non-leader alliance need coverage.
- `LEAGUE_SET_LEADER` command `32`, invite/join/loot/kinah workflows, direct disband iteration, offline recipients, Java static registry, event queue/locks, object identity, and packet processor exception/log behavior remain deferred.
- Packet-field coverage is C# emitted-object validation only; Java golden bytes, encrypted frames, packet captures, and live-client validation remain unavailable.
- C# packet intent order is deterministic by sorted positions; Java `ConcurrentHashMap` ordering remains unverified.
- Reflection, precision/rounding, and date/time handling are not involved in this unit.

## Next Recommended Unit of Work

Add command `30` permission and edge coverage:

1. Invalid target alliance.
2. Caller is not the leader of their own alliance.
3. Caller leads their alliance but their alliance is not the league leader alliance.
4. Two-alliance expel that triggers disband.
5. Keep each test source-derived and check no-send behavior for failures.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6FE-Completion.md`
   - this handoff
3. Inspect Java source for the selected command/event before touching C#.
4. Implement one narrow unit with Java breadcrumbs.
5. Add focused tests that state what is source-derived and what remains unverified.
6. Run focused tests, then full GameServer tests.
7. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
8. Commit the unit.
