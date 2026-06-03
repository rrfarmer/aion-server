# Phase 6 Session 2431 Handoff

## Current Phase

- Phase 6 Java-to-C# parity migration continues on branch `4.8`.
- Latest completed UOW: UOW-2431 alliance leader-change league timeout fanout for logout fallback.

## Last Completed UOW

- UOW-2431 wired the logout leader-fallback portion of Java `ChangeAllianceLeaderEvent` league behavior.
- Alliance leader logout in the league leader alliance now sends:
  - league `SM_ALLIANCE_INFO` broadcast after leader mutation,
  - `STR_UNION_CHANGE_LEADER_TIMEOUT` to other league members,
  - `STR_UNION_YOU_BECOME_NEW_LEADER_TIMEOUT` to the new league captain,
  - then the normal disconnected fanout and final disconnected league broadcast.

## Commits Made

- Pending commit for this handoff: `[Phase 6][UOW-2431] Wire alliance leader league timeout fanout`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceRuntime.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerLeagueRuntime.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`
- `docs/Phase-6-Session-2431-Completion.md`
- `docs/Phase-6-Session-2431-Handoff.md`

## Java Artifacts Reviewed

- `game-server/src/com/aionemu/gameserver/model/team/alliance/events/ChangeAllianceLeaderEvent.java`
- `game-server/src/com/aionemu/gameserver/model/team/league/League.java`
- `game-server/src/com/aionemu/gameserver/utils/collections/Predicates.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage`
- `Aion.GameServer.Services.PlayerAllianceRuntime`
- `Aion.GameServer.Services.PlayerEnterWorldService`
- `Aion.GameServer.Services.PlayerLeagueRuntime`
- `Aion.GameServer.Tests.PlayerEnterWorldServiceTests`

## What Changed

- Added `SmSystemMessage.UnionYouBecomeNewLeaderTimeout()` for Java message id `1400587`.
- `PlayerAllianceRuntime.ChangeLeader` now passes `isInLeague` to the planner from runtime league membership.
- Added `PlayerLeagueRuntime.CreateAllianceLeaderChangeTimeoutPlan(...)`.
- Added `PlayerLeagueLeaderChangeTimeoutPlan` and `PlayerLeagueLeaderChangeTimeoutIntent`.
- Logout alliance leader-change dispatch now processes league broadcast and captain timeout side effects even when the triggering changed-alliance member is the disconnected player, matching Java's loop side effects while still skipping offline/self packet recipients at send time.

## Tests Run

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests" --no-restore`
- Result:
  - Passed: 57
  - Failed: 0
  - Skipped: 0
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerAllianceMemberInfoTests|FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~GameServerConnectionPlayerStatusInfoTests" --no-restore`
- Result:
  - Passed: 161
  - Failed: 0
  - Skipped: 0
- `git diff --check`
- Result:
  - Passed; only Git CRLF conversion warnings.
- Pre-existing nullable/analyzer warnings remain.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.model.team.alliance.events.ChangeAllianceLeaderEvent` | `Aion.GameServer.Services.PlayerAllianceLeaderChangePlanner`, `PlayerAllianceRuntime.ChangeLeader`, and `PlayerEnterWorldService.DispatchAllianceLeaderChangeAsync` | Event / Live Dispatch | Partial | Regression Tested | Partial Parity | Logout fallback now models league broadcast and captain timeout messages. Manual `ALLIANCE_SET_CAPTAIN` command dispatch still needs live league timeout verification. |
| `com.aionemu.gameserver.model.team.league.League` | `Aion.GameServer.Services.PlayerLeagueRuntime.BroadcastAllianceInfo` and `CreateAllianceLeaderChangeTimeoutPlan` | Runtime / Packet Fanout | Partial | Regression Tested | Partial Parity | `broadcast()` and league captain timeout fanout are covered through alliance logout. Other league broadcast callers remain partially covered by command/runtime tests. |
| `com.aionemu.gameserver.utils.collections.Predicates.Players.allExcept` | `PlayerLeagueRuntime.CreateAllianceLeaderChangeTimeoutPlan` plus `PlayerEnterWorldService.DispatchLeagueLogoutPacketsAsync` | Utility / Predicate | Partial | Regression Tested | Partial Parity | New-leader exclusion is modeled for league timeout fanout; live boundary still applies Java-style online/disconnected send guard. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` | Packet DTO / Factory | Partial | Regression Tested | Partial Parity | Added `STR_UNION_YOU_BECOME_NEW_LEADER_TIMEOUT` id `1400587`; broader system-message catalog remains partial. |

## Known Gaps

- Manual `ALLIANCE_SET_CAPTAIN` league leader-change command dispatch still needs live league timeout parity coverage.
- Java `EventService.onLeftTeam` remains metadata-only for group and alliance disconnected disband.
- Manual group/alliance disband command parity and offline timeout disband parity still need review.
- Vortex defence/offence cleanup for alliance leave/kick/logout-adjacent flows still needs review.
- C# logout ordering is still not fully Java-identical around world removal and persistence timing.
- Remaining player alliance snapshots are not fully synchronized when league runtime removes/disbands a league.

## Remaining Risks

- `PlayerAllianceRuntime.ChangeLeader` now correctly suppresses direct alliance-info intents for in-league alliances; callers must dispatch the league broadcast path when live league side effects are required.
- Manual command-side alliance leader-change has adjacent tests, but the in-league command branch still needs a scenario that marks alliance runtime league ids and asserts Java timeout ordering.
- Java `ConcurrentHashMap` iteration order remains not deterministic; C# runtime list order is deterministic.

## Next Recommended UOW

UOW-2432: Wire or explicitly gate manual `ALLIANCE_SET_CAPTAIN` in-league leader-change live dispatch.

Suggested scope:
- Java:
  - `game-server/src/com/aionemu/gameserver/model/team/alliance/events/ChangeAllianceLeaderEvent.java`
  - `game-server/src/com/aionemu/gameserver/model/team/alliance/PlayerAllianceService.java`
  - `game-server/src/com/aionemu/gameserver/services/player/PlayerTeamCommandService.java`
- C#:
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceRuntime.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerLeagueRuntime.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionPlayerStatusInfoTests.cs`

Safe fallback candidate:
- Audit manual/offline-timeout alliance disband behavior now that logout-disband league-left and leader-timeout behavior are live.
- Java:
  - `PlayerAllianceService.OfflinePlayerAllianceChecker`
  - `PlayerAllianceLeavedEvent`
  - `AllianceDisbandEvent`
- C#:
  - `PlayerAllianceRuntime`
  - `PlayerLeagueRuntime`
  - `PlayerAllianceRuntimeTests`
  - `PlayerEnterWorldServiceTests`

## Focused Validation Recipe

- For UOW-2432:
  - Specific behavior/contract:
    - Java manual `ALLIANCE_SET_CAPTAIN` dispatches `ChangeAllianceLeaderEvent(eventPlayer != null)`, sends league broadcast when in a league, sends normal force leader messages, sends union timeout messages when the changed alliance is the league leader alliance, then demotes the old leader to vice captain.
  - Focused C# command:
    - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionPlayerStatusInfoTests|FullyQualifiedName~PlayerAllianceMemberInfoTests" --no-restore`
  - Java/Maven:
    - Not expected unless Java source or fixtures change; Java source review should be enough unless a targeted Java alliance leader-change command test is found.
  - Broad-validation trigger:
    - live alliance/league command connection dispatch if `GameServerConnection` send ordering changes.

## Context Needed By Next Session

- Read `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, and the latest completion document before work.
- Do not use `docs/PHASE-6-PROGRESS.md` during normal startup.
- Java remains the source of truth.
- UOW-2430 made alliance disconnected league broadcast and no-online after-disband league-left behavior live.
- UOW-2431 made logout fallback alliance leader-change league broadcast and timeout behavior live.
- Manual alliance set-captain in-league dispatch is the next natural slice because `PlayerAllianceRuntime.ChangeLeader` now marks in-league leader-change plans as league-broadcast-driven.
