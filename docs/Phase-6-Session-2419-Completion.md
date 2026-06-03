# Phase 6 Session 2419 Completion

Status: Phase 6 continues; C# logout now updates represented alliance member last-online timestamps through `PlayerAllianceRuntime`, matching the narrow Java `PlayerAllianceService.onPlayerLogout` timestamp mutation. Alliance disconnected packet/disband/league fanout remains represented by existing planners only and is not dispatched from logout.

## Scope

- UOW: UOW-2419 alliance logout last-online bridge.
- Implemented the smallest safe modeled slice from Java alliance logout.
- Left live `PlayerDisconnectedEvent` fanout disabled because the existing C# disconnected planner is supplied-snapshot only.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/model/team/alliance/PlayerAllianceService.java`
  - `onPlayerLogout` resolves `player.getPlayerAlliance()`, updates `PlayerAllianceMember.lastOnlineTime`, then fires `PlayerDisconnectedEvent`.
- `game-server/src/com/aionemu/gameserver/model/team/alliance/events/PlayerDisconnectedEvent.java`
  - Checks that the disconnected player is still a member.
  - Changes leader when the disconnected player is leader.
  - Sends offline system message, `SM_ALLIANCE_MEMBER_INFO(... DISCONNECTED)`, and `SM_ALLIANCE_INFO` to other alliance members.
  - Disbands when no online members remain, otherwise broadcasts league state when in a league.
- `game-server/src/com/aionemu/gameserver/services/player/PlayerLeaveWorldService.java`
  - Calls alliance logout immediately after group logout in the post-persistence team cleanup band.

## C# Surface Reviewed

- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceRuntime.cs`
  - Stores `PlayerAllianceMember` wrappers with existing `UpdateLastOnlineTime(DateTimeOffset)` support.
  - Added a runtime method to resolve the player alliance and update only that member.
- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceDisconnectedPlanner.cs`
  - Already models supplied-snapshot disconnected fanout statuses and packet intents.
  - Not invoked from live logout in this unit.
- `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
  - Added optional `PlayerAllianceRuntime` dependency and invoked the timestamp update alongside group timestamp recording.

## Implemented

- Added `PlayerAllianceRuntime.UpdateMemberLastOnlineTime(Player, DateTimeOffset)`.
- Extended `PlayerEnterWorldService` to accept optional `PlayerAllianceRuntime?`.
- Extended the leave-world timestamp hook to update alliance runtime members.
- Added focused runtime tests for alliance member last-online update/no-alliance no-op.
- Added focused leave-world regression for alliance member timestamp update.

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `PlayerAllianceRuntimeTests.UpdateMemberLastOnlineTime_UpdatesAllianceMemberLikeJavaLogout` | Unit | Java `PlayerAllianceService.onPlayerLogout` source review | Runtime updates only the logging-out alliance member timestamp. | Focused C# runtime assertion. | Does not fire disconnected event fanout. |
| `PlayerAllianceRuntimeTests.UpdateMemberLastOnlineTime_ReturnsFalseForPlayerWithoutRuntimeAlliance` | Unit | Java null alliance guard source review | No runtime alliance returns false without mutation. | Focused C# runtime assertion. | No Java runtime comparison. |
| `PlayerEnterWorldServiceTests.LeaveWorld_UpdatesAllianceMemberLastOnlineAfterPersistenceBandLikeJavaLogout` | Regression | Java `PlayerLeaveWorldService.leaveWorld` and `PlayerAllianceService.onPlayerLogout` source review | Logout updates the represented alliance member from the persisted logout timestamp and leaves other members untouched. | Focused C# service-boundary/runtime test. | C# still does not dispatch `PlayerDisconnectedEvent`; placement is approximate because logout persistence is aggregated. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.player.PlayerLeaveWorldService.leaveWorld` alliance logout slice | `Aion.GameServer.Services.PlayerEnterWorldService.LeaveWorldAsync` | Logout Lifecycle | Partial | Regression Tested | Partial Parity | C# now updates represented alliance member last-online through `PlayerAllianceRuntime`. Disconnected event fanout is not live. C# placement remains after aggregate repository save. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.onPlayerLogout` | `Aion.GameServer.Services.PlayerAllianceRuntime.UpdateMemberLastOnlineTime` | Runtime Service | Partial | Unit/Regression Tested | Partial Parity | Last-online mutation and no-alliance guard are covered. Leader-change, disband, league broadcast, and packet fanout remain planner-only/unwired. |
| `com.aionemu.gameserver.model.team.alliance.events.PlayerDisconnectedEvent` | `Aion.GameServer.Services.PlayerAllianceDisconnectedPlanner` | Planner | Partial | Unit Tested | Needs Verification | Existing planner covers supplied-snapshot non-leader and deferred leader branches, but logout does not invoke it and no live packets are dispatched. |

## Validation Decision

- Changed surface: C# runtime method, C# service hook, focused tests.
- Specific behavior/contract: Java alliance logout updates `PlayerAllianceMember.lastOnlineTime` before disconnected event fanout.
- Focused C# command:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~PlayerAllianceRuntimeTests|FullyQualifiedName~PlayerAllianceMemberInfoTests" --no-restore`
- Result:
  - Passed: 116
  - Failed: 0
  - Skipped: 0
- Focused Java/Maven command:
  - Not run; no Java source or fixture changed, and Java evidence was source review.
- Broad-validation trigger: none.
- Broad .NET decision:
  - Skipped; filtered test compiled affected project/dependencies and covered edited service/runtime plus adjacent disconnected planner tests.

## Known Remaining Gaps

- Alliance `PlayerDisconnectedEvent` is not invoked from logout.
- Leader-change-on-disconnect, no-online-member disband, and league broadcast remain non-live.
- No live alliance packet fanout is sent from `LeaveWorldAsync`.
- Group disconnected event fanout remains similarly unwired.
- Legion logout warehouse/member cleanup remains unmodeled.

## Summary Metrics

- Java artifacts reviewed: 3.
- C# artifacts reviewed: 3.
- Production files changed: 2.
- Test files changed: 2.
- New focused tests: 3.
- Focused validation commands passed: 1.
