# Phase 6 Session 2424 Completion

Status: Phase 6 continues; UOW-2424 improved the non-live alliance disconnected-event planner so a leader disconnect no longer drops Java `PlayerDisconnectedEvent` packet fanout. Live logout dispatch remains unwired.

## Scope

- UOW: UOW-2424 alliance disconnected leader fanout planner.
- Reviewed Java alliance disconnected and leader-change events.
- Updated C# `PlayerAllianceDisconnectedPlanner` to continue planning offline packet fanout for disconnected leaders while flagging leader-change as a required side effect.
- Updated focused alliance member-info planner tests.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/model/team/alliance/events/PlayerDisconnectedEvent.java`
  - Requires the disconnected player to still be a member.
  - If the disconnected player is the alliance leader, fires `ChangeAllianceLeaderEvent`.
  - Sends offline system message, `SM_ALLIANCE_MEMBER_INFO(... DISCONNECTED)`, and `SM_ALLIANCE_INFO(alliance)` to every other member.
  - Disbands if no online members remain, otherwise broadcasts league state when in a league.
- `game-server/src/com/aionemu/gameserver/model/team/alliance/events/ChangeAllianceLeaderEvent.java`
  - For event-player-null cases, prefers an online vice captain, then the next online member.
  - Removes the new leader from vice-captain ids and sends/broadcasts leader-change side effects separately from disconnected-event fanout.

## C# Surface Reviewed

- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceDisconnectedPlanner.cs`
  - Previously returned `LeaderDisconnectDeferred` with no packet intents for disconnected leaders.
  - Now selects a fallback leader for metadata and still plans disconnected-event packet fanout.
- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceLeaderChangePlanner.cs`
  - Existing non-live planner models leader-change side-effect packets separately.
- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceRuntime.cs`
  - Existing runtime can select fallback leaders and update leader state, but logout does not invoke disconnected-event fanout.
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerAllianceMemberInfoTests.cs`
  - Existing disconnected planner tests were updated/extended.

## Implemented

- Removed the early leader-disconnect deferred return from `PlayerAllianceDisconnectedPlanner`.
- Added fallback leader selection mirroring Java preference:
  - first online vice captain excluding the disconnected leader
  - otherwise first online non-disconnected member
- Continued to produce disconnected-event packet intents for all non-disconnected members.
- Kept `WouldTriggerLeaderChange`, `WouldDisbandIfNoOnlineMembersRemain`, and `WouldBroadcastLeague` flags.
- Added/updated tests for leader-disconnect fanout and missing-member handling.

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `DisconnectedPlanner_PlansLeaderDisconnectFanoutAfterFallbackLeaderLikeJavaEvent` | Unit | Java `PlayerDisconnectedEvent` and `ChangeAllianceLeaderEvent` source review | Disconnected leader plans offline fanout to remaining members, flags leader-change, and uses online vice-captain fallback leader metadata. | Focused C# assertions over packet-intent order and metadata. | Does not execute live leader change or send packets. |
| `DisconnectedPlanner_ReportsMissingMemberBranch` | Unit | Java `checkCondition` source review | Missing disconnected member returns missing-member status without packets. | Focused C# assertion. | No live team event dispatch. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.model.team.alliance.events.PlayerDisconnectedEvent` | `Aion.GameServer.Services.PlayerAllianceDisconnectedPlanner` | Event / Planner | Partial | Unit Tested | Partial Parity | Non-live planner now covers non-leader and leader disconnected fanout intent ordering. Live logout still does not invoke this planner or send packets. |
| `com.aionemu.gameserver.model.team.alliance.events.ChangeAllianceLeaderEvent` | `Aion.GameServer.Services.PlayerAllianceDisconnectedPlanner.SelectFallbackLeaderObjectId` plus `PlayerAllianceLeaderChangePlanner` | Event / Planner | Partial | Unit Tested | Partial Parity | Disconnected planner now mirrors fallback leader selection for packet metadata. Actual leader-change side effects remain separate/non-live. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.onPlayerLogout` | `Aion.GameServer.Services.PlayerEnterWorldService.LeaveWorldAsync` plus `PlayerAllianceRuntime.UpdateMemberLastOnlineTime` | Logout Lifecycle | Partial | Regression Tested | Partial Parity | Last-online update is live in C# from prior UOWs, but disconnected-event fanout remains unwired from logout. |

## Validation Decision

- Changed surface: one non-live alliance disconnected planner plus focused tests in the existing alliance member-info test class.
- Specific behavior/contract: Java `PlayerDisconnectedEvent` still sends disconnected fanout after leader-change handling when the disconnected player is leader.
- Focused C# command:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerAllianceMemberInfoTests" --no-restore`
- Result:
  - Passed: 42
  - Failed: 0
  - Skipped: 0
- Focused Java/Maven command:
  - Not run; no Java source or fixture changed, and Java evidence was source review.
- Broad-validation trigger: none.
- Broad .NET decision:
  - Skipped; filtered test compiled affected project/dependencies and covered the edited non-live planner plus adjacent alliance packet planners.
- Why this scope is sufficient:
  - The unit changed only one standalone non-live planner and its tests, with no live dispatch, repository, packet primitive, scheduler, or shared runtime wiring changes.

## Known Remaining Gaps

- `PlayerEnterWorldService.LeaveWorldAsync` still does not invoke alliance disconnected-event fanout.
- C# does not yet execute live alliance disconnected packet sends from logout.
- Live ordering between last-online update, leader change, disconnected fanout, disband, league broadcast, and logout persistence remains unverified.
- Group disconnected-event planning still needs current-state discovery because standalone group disconnected planner/test files do not exist.

## Summary Metrics

- Java artifacts reviewed: 2.
- C# artifacts reviewed: 4.
- Production files changed: 1.
- Test files changed: 1.
- Focused validation commands passed: 1.
- Artifacts with verified parity: 0.
- Artifacts needing verification: 3.
- Blocked artifacts: 0.
- Estimated Phase 6 completion: unchanged, still conservatively partial.
