# Phase 6 Session 2431 Completion

Status: Phase 6 continues; UOW-2431 wired alliance leader-change league timeout fanout for logout fallback.

## Scope

- UOW: UOW-2431 alliance leader-change league timeout parity.
- Reviewed Java `ChangeAllianceLeaderEvent.changeLeaderTo`, `League.broadcast`, and `Predicates.Players.allExcept`.
- Added the missing C# `STR_UNION_YOU_BECOME_NEW_LEADER_TIMEOUT` system message.
- Made `PlayerAllianceRuntime.ChangeLeader` pass league membership into `PlayerAllianceLeaderChangePlanner`.
- Added C# league runtime planning for Java's alliance leader-change timeout fanout when the changed alliance is the league leader alliance.
- Wired alliance logout leader-fallback dispatch to send league alliance-info broadcast and league timeout messages before disconnected fanout.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/model/team/alliance/events/ChangeAllianceLeaderEvent.java`
  - Changes alliance leader, removes the new leader from vice captains, broadcasts league alliance-info when in a league, then iterates changed-alliance members.
  - If the new alliance leader is also `team.getLeague().getCaptain()`, each non-new-leader changed-alliance member triggers `STR_UNION_CHANGE_LEADER_TIMEOUT` to every league player except the new leader.
  - The new leader receives `STR_FORCE_YOU_BECOME_NEW_LEADER` and, when also league captain, `STR_UNION_YOU_BECOME_NEW_LEADER_TIMEOUT`.
- `game-server/src/com/aionemu/gameserver/model/team/league/League.java`
  - `broadcast()` sends `SM_ALLIANCE_INFO(targetAlliance)` to every league alliance.
  - `getCaptain()` returns the leader object of the league leader alliance.
- `game-server/src/com/aionemu/gameserver/utils/collections/Predicates.java`
  - `Predicates.Players.allExcept(player)` excludes only the provided player; online filtering still occurs at `PacketSendUtility.sendPacket`.
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`
  - `STR_UNION_YOU_BECOME_NEW_LEADER_TIMEOUT` uses message id `1400587`.

## Implemented

- Added `SmSystemMessage.UnionYouBecomeNewLeaderTimeout()`.
- `PlayerAllianceRuntime.ChangeLeader` now passes `isInLeague` based on runtime league membership.
- Added `PlayerLeagueRuntime.CreateAllianceLeaderChangeTimeoutPlan(...)`.
- Added `PlayerLeagueLeaderChangeTimeoutPlan` and `PlayerLeagueLeaderChangeTimeoutIntent`.
- `PlayerEnterWorldService.DispatchAllianceDisconnectedLogoutAsync` now:
  - sends Java `League.broadcast()` alliance-info before changed-alliance member loop,
  - sends `STR_UNION_CHANGE_LEADER_TIMEOUT` for each non-new-leader changed-alliance member trigger,
  - sends `STR_UNION_YOU_BECOME_NEW_LEADER_TIMEOUT` after the new leader's normal force leader message,
  - preserves live recipient filtering for offline/disconnected recipients.

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `LeaveWorld_DispatchesAllianceLeaderChangeLeagueTimeoutsForCaptainLikeJavaLogout` | Regression | Java `ChangeAllianceLeaderEvent`, `League.broadcast`, and `Predicates.Players.allExcept` source review | Alliance leader logout in the league leader alliance sends league info broadcast, league timeout messages, disconnected fanout, and final disconnected league broadcast in Java order. | Focused C# logout assertions over recipient order, `SM_ALLIANCE_INFO` league rows, message ids `1400588`, `1300999`, `1400587`, and absence of packets to the disconnected player. | Manual alliance leader-change command-side league timeout dispatch still needs a dedicated live test and may need ordering work. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.model.team.alliance.events.ChangeAllianceLeaderEvent` | `Aion.GameServer.Services.PlayerAllianceLeaderChangePlanner`, `PlayerAllianceRuntime.ChangeLeader`, and `PlayerEnterWorldService.DispatchAllianceLeaderChangeAsync` | Event / Live Dispatch | Partial | Regression Tested | Partial Parity | Logout fallback now models league broadcast and captain timeout messages. Manual `ALLIANCE_SET_CAPTAIN` command dispatch still needs live league timeout verification. |
| `com.aionemu.gameserver.model.team.league.League` | `Aion.GameServer.Services.PlayerLeagueRuntime.BroadcastAllianceInfo` and `CreateAllianceLeaderChangeTimeoutPlan` | Runtime / Packet Fanout | Partial | Regression Tested | Partial Parity | `broadcast()` and league captain timeout fanout are covered through alliance logout. Other league broadcast callers remain partially covered by command/runtime tests. |
| `com.aionemu.gameserver.utils.collections.Predicates.Players.allExcept` | `PlayerLeagueRuntime.CreateAllianceLeaderChangeTimeoutPlan` plus `PlayerEnterWorldService.DispatchLeagueLogoutPacketsAsync` | Utility / Predicate | Partial | Regression Tested | Partial Parity | New-leader exclusion is modeled for league timeout fanout; live boundary still applies Java-style online/disconnected send guard. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` | Packet DTO / Factory | Partial | Regression Tested | Partial Parity | Added `STR_UNION_YOU_BECOME_NEW_LEADER_TIMEOUT` id `1400587`; broader system-message catalog remains partial. |

## Validation Decision

- Changed surface: live alliance logout connection dispatch plus alliance/league runtime packet-intent planning and one system-message factory.
- Specific behavior/contract: Java alliance leader-change in a league leader alliance sends league `SM_ALLIANCE_INFO`, then `STR_UNION_CHANGE_LEADER_TIMEOUT` / `STR_UNION_YOU_BECOME_NEW_LEADER_TIMEOUT` around the changed-alliance member loop before disconnected fanout continues.
- Focused C# commands:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests" --no-restore`
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerAllianceMemberInfoTests|FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~GameServerConnectionPlayerStatusInfoTests" --no-restore`
- Results:
  - First command passed: 57 passed, 0 failed, 0 skipped.
  - Second command passed: 161 passed, 0 failed, 0 skipped.
- Hygiene:
  - `git diff --check`
  - Passed; only Git CRLF conversion warnings.
- Focused Java/Maven command:
  - Not run; no Java source or fixtures changed, and Java evidence came from direct source review. No targeted Java unit test for this alliance leader-change league timeout event path was found during this UOW.
- Broad-validation trigger:
  - Live alliance/league connection dispatch.
- Broad .NET decision:
  - Full project/solution validation skipped after focused evidence. The filtered commands compiled affected dependencies and exercised the edited logout service plus adjacent alliance planner and player-status command tests. No packet primitive, serializer core, scheduler, persistence repository, or connection registry implementation changed.
- Why this scope is sufficient:
  - The changed behavior is isolated to one system message id, alliance leader-change league metadata, league timeout packet intents, and logout dispatch ordering. The focused regression asserts exact Java-derived recipient ordering, packet types, system-message ids, and league row payloads.

## Known Remaining Gaps

- Manual `ALLIANCE_SET_CAPTAIN` league leader-change command dispatch still needs live league timeout parity coverage.
- Java `EventService.onLeftTeam` remains metadata-only for group and alliance disconnected disband.
- Manual group/alliance disband command parity and offline timeout disband parity still need review.
- Vortex defence/offence cleanup for alliance leave/kick/logout-adjacent flows still needs review.
- C# logout ordering is still not fully Java-identical around world removal and persistence timing.
- Remaining player alliance snapshots are not fully synchronized when league runtime removes/disbands a league.

## Summary Metrics

- Java artifacts reviewed: 4.
- C# artifacts reviewed: 5.
- Production files changed: 4.
- Test files changed: 1.
- Focused validation commands passed: 2.
- Artifacts with verified parity: 0.
- Artifacts needing verification or partial parity: 4.
- Blocked artifacts: 0.
- Estimated Phase 6 completion: unchanged, still conservatively partial.
