# Phase 6FA Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6EZ and covers Session 645.

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

### Session 645 - League Move Fanout Intent Order

- Source-read Java `LeagueMoveEvent.handleEvent`, `SM_ALLIANCE_INFO`, and `SM_SYSTEM_MESSAGE.STR_UNION_CHANGE_FORCE_NUMBER_ME/HIM`.
- Added packet intents to `PlayerLeagueRuntime.MoveAlliance`: after a successful command `31` move, each alliance member receives `SM_ALLIANCE_INFO`, then the selected force-number message, then the target force-number message.
- Added C# `SmSystemMessage.UnionChangeForceNumberMe` and `UnionChangeForceNumberHim` factories with Java message ids `1400589` and `1400590`.
- Wired `GameServerConnection` to send command `31` league packet intents through the connection registry or active connection fallback.
- Updated the move regression to assert recipient order and packet type/message-id order. This is object-order coverage only; league `SM_ALLIANCE_INFO` byte serialization still throws until league rows are ported.
- Commit: this handoff should be included with the unit commit.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_PLAYER_STATUS_INFO` | `Aion.GameServer.Network.Aion.ClientPackets.CmPlayerStatusInfo` / `Aion.GameServer.Network.Aion.GameServerConnection.HandlePlayerStatusInfoAsync` | Client Packet / Handler Boundary | Partial | Regression Tested | Needs Verification | Command `31` now sends source-derived league move packet intents after a successful move. Byte parity remains unverified. |
| `com.aionemu.gameserver.model.team.common.events.TeamCommand.LEAGUE_ALLIANCE_MOVE` | Command code `31` branch in `GameServerConnection.HandlePlayerStatusInfoAsync` | Enum / Command Mapping | Partial | Regression Tested | Needs Verification | Position swap and object-level fanout are modeled. Global fanout order is deterministic by C# league position and not Java-runtime compared. |
| `com.aionemu.gameserver.model.team.league.LeagueService.moveAlliance` | `PlayerLeagueRuntime.MoveAlliance` plus command `31` branch | Service / Runtime Bridge | Partial | Regression Tested | Needs Verification | Leader gate, position swap, and packet intent fanout are modeled. Java static registry and runtime exception behavior are not compared. |
| `com.aionemu.gameserver.model.team.league.League` | `PlayerLeagueRuntime` / `PlayerLeagueSnapshot` | Team State | Partial | Regression Tested | Needs Verification | Minimal league state now drives move fanout. Full lifecycle and loot behavior remain unported. |
| `com.aionemu.gameserver.model.team.league.LeagueMember` | Internal `PlayerLeagueRuntime.PlayerLeagueMember` | Team Member | Partial | Regression Tested | Needs Verification | Tracks alliance id and position only. Java object/name access is approximated through alliance runtime lookups. |
| `com.aionemu.gameserver.model.team.league.events.LeagueMoveEvent` | `PlayerLeagueRuntime.MoveAlliance` / `PlayerLeaguePacketIntent` | Event Runtime Dependency | Partial | Regression Tested | Needs Verification | Per-alliance packet sequence is modeled; Java lock/event queue, iteration order, and packet bytes remain unverified. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAlliance` | `PlayerAllianceRuntime.Resolve` / `GetDescriptor` / `GetSnapshot` / `GetMemberObjectIds` | Team State Dependency | Partial | Regression Tested | Needs Verification | Supplies recipients, leader names, and alliance-info plans. No live Java league pointer exists in C#. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ALLIANCE_INFO` | `SmAllianceInfo` / `PlayerAllianceInfoPacketPlan` | Server Packet | Partial | Regression Tested | Partial Parity | Packet objects are created in Java order with a non-zero league id. League row serialization still throws, so bytes are not ported. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `SmSystemMessage` | Server Packet | Partial | Regression Tested | Needs Verification | Added `STR_UNION_CHANGE_FORCE_NUMBER_ME/HIM` factories. Parameter payload bytes are not Java-golden compared. |
| `com.aionemu.gameserver.utils.PacketSendUtility` | `IGameClientConnectionRegistry` / `GameServerConnection.SendLeaguePacketAsync` | Runtime Dependency | Partial | Regression Tested | Needs Verification | In-memory registry verifies recipient/type/message-id order. Live socket and offline-recipient behavior remain unverified. |

## Tests Updated

- `GameServerConnectionPlayerStatusInfoTests.HandlePlayerStatusInfoAsync_LeagueAllianceMoveSwapsPositionsAndFansOutJavaPacketOrder`
  - Validates command `31` swaps selected/target league positions and sends `SmAllianceInfo`, selected force-number system message, and target force-number system message to each involved alliance in the modeled order.
- The test is source-derived. It does not compare against Java runtime execution, Java-generated golden vectors, packet captures, encrypted frames, or a live client.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 10
- Total artifacts ported or partially modeled in this handoff window: 1 command `31` `LeagueMoveEvent` packet-intent fanout slice plus 2 system-message factories and 1 updated regression test.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 8
- Total blocked/not-started artifacts: league `SM_ALLIANCE_INFO` row serialization, full `LeagueService`, Java static league registry, league leave/disband/join/change-leader workflows, Java event queue/lock comparison, packet processor exception/log comparison, encoded opcode/frame golden validation, and client validation.
- Estimated overall migration completion: 64%

The percentage stays conservative. Command `31` is closer, but league packet serialization and lifecycle parity remain open.

## Remaining Risks

- `SmAllianceInfo` still cannot serialize league rows; current coverage verifies object fanout order only.
- C# uses deterministic sorted league positions for fanout order; Java iterates `ConcurrentHashMap.values()` through `league.forEach`, so global alliance order is not verified.
- `PlayerLeagueRuntime` remains a narrow bridge, not a full Java `League`/`LeagueService` port.
- Java static `LeagueService.leagues`, league leave/disband/join/invite/change-leader/loot/kinah workflows, event queue, locks, object identity, iteration ordering, and threading behavior remain deferred.
- Java packet bytes, encrypted opcode/frame validation, packet capture comparison, packet processor exception/log comparison, offline-recipient behavior, and real-client validation remain unavailable.

## Next Recommended Unit of Work

Continue Phase 6 in this order:

1. Port the league-row section of `SM_ALLIANCE_INFO` into `PlayerAllianceInfoPacketPlan` and `SmAllianceInfo`.
2. Add packet-field tests that cover league id, league loot rules, alliance positions, alliance ids, member counts, captain names, and captain world ids.
3. Only after field-level coverage is stable, add byte-golden or live-client validation for command `31` league move fanout.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6EZ-Completion.md`
   - this handoff
3. Inspect Java source for the selected branch before touching C#.
4. Implement one narrow unit with Java breadcrumbs.
5. Add focused tests that state what is source-derived and what remains unverified.
6. Run focused tests, then full GameServer tests.
7. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
8. Commit the unit.
