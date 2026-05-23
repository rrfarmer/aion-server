# Phase 6DZ Completion Handoff

Created: May 23, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6DY and covers Sessions 611-613.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerStatusInfo|ReadyCheck|ClientPacketFactory_ParsesPlayerStatusInfoPacket"`
  - Result: Passed, 5 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1075 tests.

## Recent Work Completed

### Session 611 - Show Brand Packet and Handler Boundary

- Added `CmShowBrand` parsing for Java opcode `181` and `D action`, `D brandId`, `D targetObjectId`.
- Registered opcode `181` for `InGame`.
- Wired `GameServerConnection` to dispatch through `PlayerShowBrandCommandPlanner`.
- Added shared `PlayerGroupRuntime` and `PlayerAllianceRuntime` injection through `GameClientSocketServer` into `GameServerConnection`.
- Commit: `a48d2b594 Wire show brand client packet`

### Session 612 - Alliance Entered Brand Intent

- Source-read Java `PlayerAllianceEnteredEvent.handleEvent`.
- Extended `PlayerAllianceEnteredPlan` with optional `PlayerAllianceBrandIntent`.
- Added `PlayerAllianceRuntime.CreateEnteredPlan` so runtime-created entered plans include current brand resend metadata for the invited player.
- Commit: `dcc02503c Add alliance entered brand intent`

### Session 613 - Player Status Ready-Check Packet Boundary

- Source-read Java `CM_PLAYER_STATUS_INFO`, `TeamCommand`, `PlayerTeamCommandService`, and `PlayerAllianceService.checkReady`.
- Added `CmPlayerStatusInfo` parsing for Java opcode `96` and `C/D/D/D` payload.
- Registered opcode `96` for `InGame`.
- Wired only alliance ready-check command ids `20..24` through `GameServerConnection` into `PlayerAllianceRuntime.CheckReady`.
- Kept other `CM_PLAYER_STATUS_INFO` command branches deferred.
- Commit: `b1df9c52a Wire player status ready checks`

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_SHOW_BRAND` | `CmShowBrand` / `GameServerConnection.HandleShowBrandCommandAsync` | Client Packet / Handler Boundary | Partial | Regression Tested | Needs Verification | Parser and connection handler are present. Live runtime population, Java static team identity, encrypted frames, and client validation remain missing. |
| `com.aionemu.gameserver.model.team.TemporaryPlayerTeam.updateBrand` | `PlayerShowBrandCommandPlanner` plus connection registry fanout | Runtime/Socket Bridge | Partial | Regression Tested | Needs Verification | C# sends generated brand intents through registry/direct fallback. Java `PacketSendUtility` ordering and offline-recipient behavior remain unverified. |
| `com.aionemu.gameserver.model.team.alliance.events.PlayerAllianceEnteredEvent` | `PlayerAllianceEnteredPlanner` / `PlayerAllianceRuntime.CreateEnteredPlan` | Event Runtime/Planning Bridge | Partial | Regression Tested | Needs Verification | Current-brand resend metadata is explicit. Live event dispatch, socket ordering, abyss-rank broadcast, league broadcast, and base entered event remain deferred. |
| `com.aionemu.gameserver.model.team.TemporaryPlayerTeam.sendBrands` | `PlayerAllianceBrandIntent` through `PlayerAllianceEnteredPlan.BrandIntent` | Base Team Runtime Bridge | Partial | Regression Tested | Needs Verification | Current alliance brand map is snapshotted for the invited player. Java concurrent-map behavior and live sends remain unverified. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_PLAYER_STATUS_INFO` | `CmPlayerStatusInfo` / `GameServerConnection.HandlePlayerStatusInfoAsync` | Client Packet / Handler Boundary | Partial | Regression Tested | Needs Verification | Parser exists and ready-check command ids `20..24` dispatch. Group, alliance management, and league command branches remain deferred. |
| `com.aionemu.gameserver.model.team.common.service.PlayerTeamCommandService` | Ready-check branch in `GameServerConnection` | Service Dependency | Partial | Regression Tested | Needs Verification | C# bypasses full generic team-command dispatch and handles only ready-checks. Ban, leader, mentoring, vice-captain, group-change, and league commands remain missing. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.checkReady` | `PlayerAllianceRuntime.CheckReady` through connection handler | Service / Runtime Bridge | Partial | Regression Tested | Needs Verification | Missing alliance no-ops and ready-check runtime dispatch are modeled. Java static registry and event lock wrapper remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SHOW_BRAND` | `SmShowBrand` via show-brand and entered-plan intents | Server Packet | Partial | Regression Tested | Needs Verification | Existing packet is reused by new callers. Java golden frames and client rendering remain missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ALLIANCE_READY_CHECK` | `SmAllianceReadyCheck` through ready-check packet intents | Server Packet | Partial | Regression Tested | Needs Verification | Existing packet is reachable from parsed `CM_PLAYER_STATUS_INFO`. Java golden frames and client validation remain missing. |
| `com.aionemu.gameserver.utils.PacketSendUtility` | `IGameClientConnectionRegistry.SendPacketToPlayerAsync` / direct fallback | Runtime Dependency | Partial | Regression Tested | Needs Verification | C# now has registry send paths for show-brand and ready-check packets. Java send ordering/offline-recipient behavior remains unverified. |

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 10
- Total artifacts ported or partially modeled in this handoff window: 10
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 10
- Total blocked/not-started artifacts: live group/alliance service runtime population, full `CM_PLAYER_STATUS_INFO` branch coverage, Java static/current team lookup, Java `PacketSendUtility` ordering comparison, Java `alliance.onEvent` locking comparison, abyss-rank broadcast, league broadcast, base entered-event side effects, encoded opcode/frame validation, packet capture comparison, and real-client validation.
- Estimated overall migration completion: 64%

The percentage stays conservative. This window connected several alliance/team command planners to packet boundaries, but full live service lifecycle and real-client validation are still outstanding.

## Remaining Risks

- Most `CM_PLAYER_STATUS_INFO` branches remain unimplemented.
- Live group/alliance runtime population is still incomplete, so handlers rely on future service wiring to maintain runtime ownership.
- Java static/current team object identity is approximated through C# runtime snapshots and descriptors.
- Java `PacketSendUtility` socket ordering and offline-recipient behavior have not been runtime-compared.
- Java `ConcurrentHashMap`, team lock, and `alliance.onEvent` threading behavior remain source-derived only.
- Entered-event abyss-rank broadcast, league broadcast, and base event side effects remain metadata/deferred.
- Java golden byte vectors, encrypted opcode/frame validation, packet capture comparison, and real-client validation remain unavailable.

## Next Recommended Unit of Work

Continue `CM_PLAYER_STATUS_INFO` parity with one narrow command branch:

1. Lowest-risk option: port `GROUP_SET_LFG`, matching Java `activePlayer.setLookingForGroup(selectedObjectId == 2)`, with parser/handler tests and no socket fanout.
2. Higher-value option: wire `ALLIANCE_CHANGE_GROUP` through existing `PlayerAllianceGroupChangeServicePlanner`, including system-message sends for no-alliance/no-rights and runtime dispatch for authorized callers.
3. Keep league commands and full generic `PlayerTeamCommandService` dispatch deferred.
4. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics after the unit.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6DY-Completion.md`
   - this handoff
3. Start with `GROUP_SET_LFG` or `ALLIANCE_CHANGE_GROUP` inside `CM_PLAYER_STATUS_INFO`.
4. Implement one narrow unit.
5. Add focused tests.
6. Run focused tests, then full GameServer tests.
7. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
8. Commit the unit.
