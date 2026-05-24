# Phase 6EY Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6EX and covers Session 643.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerStatusInfo|PlayerAllianceRuntime|PlayerAllianceMemberInfo|BaseLeavePlanner"`
  - Result: Passed, 103 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1117 tests.

## Recent Work Completed

### Session 643 - League Alliance Move Direct Packet Boundary

- Re-read Java `CM_PLAYER_STATUS_INFO.runImpl` and corrected the prior assumption from Session 639: `LEAGUE_ALLIANCE_MOVE` command `31` is handled directly by the packet class through `LeagueService.moveAlliance`, outside `PlayerTeamCommandService.executeCommand`.
- Added an explicit C# command `31` branch before ready-check fallback so the packet boundary follows the Java dispatch shape.
- Added missing-alliance and missing-league prerequisite coverage. C# now fails fast with explicit `InvalidOperationException` messages instead of silently no-oping; this is an intentional diagnostic difference because Java reaches direct null dereferences in this path.
- Corrected the command `31` regression payload to use `allianceGroupId` as Java's target alliance id.
- Commit: this handoff should be included with the unit commit.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_PLAYER_STATUS_INFO` | `Aion.GameServer.Network.Aion.ClientPackets.CmPlayerStatusInfo` / `Aion.GameServer.Network.Aion.GameServerConnection.HandlePlayerStatusInfoAsync` | Client Packet / Handler Boundary | Partial | Regression Tested | Needs Verification | Command `31` now has an explicit direct packet branch like Java. Real league move mutation and packet fanout are not ported yet. |
| `com.aionemu.gameserver.model.team.common.events.TeamCommand.LEAGUE_ALLIANCE_MOVE` | Command code `31` branch in `GameServerConnection.HandlePlayerStatusInfoAsync` | Enum / Command Mapping | Partial | Regression Tested | Needs Verification | Corrected from the prior no-op assumption. Missing alliance and missing league now fail fast. |
| `com.aionemu.gameserver.model.team.league.LeagueService.moveAlliance` | Command code `31` prerequisite branch in `GameServerConnection.HandlePlayerStatusInfoAsync` | Service / Runtime Bridge | Partial | Regression Tested | Intentional Difference | Java directly dereferences alliance/league state; C# throws explicit `InvalidOperationException` messages while the league runtime is absent. |
| `com.aionemu.gameserver.model.team.league.League` | Deferred C# league runtime | Team State Dependency | Not Started | No Tests | Unknown | Needed for real leader-alliance checks, selected/target alliance lookup, position swaps, and league broadcast fanout. |
| `com.aionemu.gameserver.model.team.league.events.LeagueMoveEvent` | Deferred C# league move event workflow | Event Runtime Dependency | Not Started | No Tests | Unknown | Real position swap, `SM_ALLIANCE_INFO` broadcast, and force-number system messages remain unported. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAlliance` | `Aion.GameServer.Model.GameObjects.PlayerAllianceDescriptor` / `PlayerAllianceRuntime.Resolve` | Team State Dependency | Partial | Regression Tested | Needs Verification | C# resolves current alliance for command `31`, but has no league pointer/state yet. |
| `com.aionemu.gameserver.utils.PacketSendUtility` | `IGameClientConnectionRegistry` / no-send assertions | Runtime Dependency | Partial | Regression Tested | Needs Verification | Missing-alliance and missing-league command `31` failures send no packets in regression tests. Live Java packet processor behavior remains unverified. |

## Tests Added Or Updated

- `GameServerConnectionPlayerStatusInfoTests.HandlePlayerStatusInfoAsync_LeagueAllianceMoveWithoutAllianceThrowsLikeJavaDirectPath`
  - Validates command `31` without an alliance throws `InvalidOperationException("Player alliance should not be null")` and sends no packets.
- `GameServerConnectionPlayerStatusInfoTests.HandlePlayerStatusInfoAsync_LeagueAllianceMoveWithoutLeagueThrowsLikeJavaDirectPath`
  - Validates command `31` in an alliance without league state throws `InvalidOperationException("League should not be null")`, preserves alliance membership, and sends no packets.
- These tests are source-derived. They do not compare against Java runtime execution, Java-generated golden vectors, packet captures, encrypted frames, or a live client.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 7
- Total artifacts ported or partially modeled in this handoff window: 1 `CM_PLAYER_STATUS_INFO` / `LEAGUE_ALLIANCE_MOVE` direct prerequisite branch plus 2 regression tests.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 4
- Total blocked/not-started artifacts: real league runtime, `LeagueService.moveAlliance`, `LeagueMoveEvent`, Java static league registry, packet processor exception/log comparison, encoded opcode/frame golden validation, and client validation.
- Estimated overall migration completion: 64%

The percentage stays conservative. Command `31` dispatch is corrected, but actual league movement remains a deferred subsystem.

## Remaining Risks

- Command `31` still has no real league runtime implementation; it only models direct-path prerequisite failures while no league state exists.
- Java null-dereference exception type and exact VM-generated message are not mirrored; C# uses explicit `InvalidOperationException` diagnostics.
- Java `LeagueService.moveAlliance`, `LeagueMoveEvent`, leader-alliance permission check, selected/target position swaps, `SM_ALLIANCE_INFO` broadcast, and force-number system messages remain unported.
- Java packet processor exception handling/logging, static league registry, event queue, lock, iteration ordering, threading behavior, encrypted frame validation, packet captures, and real-client validation remain unavailable.

## Next Recommended Unit of Work

Continue Phase 6 in this order:

1. Start a minimal C# league runtime bridge with just enough state to attach alliances to a league, preserve Java league positions, and expose command `31` leader/missing-target prerequisites.
2. Keep `LeagueMoveEvent` packet fanout deferred until the state model is stable.
3. After the league state bridge exists, add the smallest command `31` real-move slice: leader alliance check, selected/target lookup, position swap, no-send/failure tests first, then packet fanout.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6EX-Completion.md`
   - this handoff
3. Inspect Java source for the selected branch before touching C#.
4. Implement one narrow unit with Java breadcrumbs.
5. Add focused tests that state what is source-derived and what remains unverified.
6. Run focused tests, then full GameServer tests.
7. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
8. Commit the unit.
