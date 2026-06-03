# Phase 6 Session 2425 Completion

Status: Phase 6 continues; UOW-2425 added a non-live C# group disconnected-event planner for Java `PlayerDisconnectedEvent` behavior. Live logout dispatch remains unwired.

## Scope

- UOW: UOW-2425 group disconnected-event planner gap.
- Reviewed Java group logout, disconnected, and leader-change sources.
- Added a C# `PlayerGroupDisconnectedPlanner` that reads current runtime group state and plans Java-style disconnected side effects without mutating live group state.
- Added focused tests for missing-member, no-online disband, non-leader fanout, and leader-disconnect fallback metadata.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/model/team/group/events/PlayerDisconnectedEvent.java`
  - `checkCondition` requires the disconnected player to still be a group member.
  - If no online members remain, calls `PlayerGroupService.disband(group)`.
  - Otherwise, disconnected leader triggers `ChangeGroupLeaderEvent`.
  - For every other group member, sends `STR_PARTY_HE_BECOME_OFFLINE`, `SM_GROUP_MEMBER_INFO(... DISCONNECTED)` about the disconnected player, and also sends `SM_GROUP_MEMBER_INFO(... DISCONNECTED)` about that member to the disconnecting player.
- `game-server/src/com/aionemu/gameserver/model/team/group/events/ChangeGroupLeaderEvent.java`
  - Null event-player path chooses the next online non-leader member through `ChangeLeaderEvent.changeLeaderToNextAvailablePlayer`.
  - Sends `SM_GROUP_INFO` plus leader-change system messages to group members.
- `game-server/src/com/aionemu/gameserver/model/team/group/PlayerGroupService.java`
  - `onPlayerLogout` updates the member last-online time before firing `PlayerDisconnectedEvent`.
  - `disband` removes find-group recruitment and fires `GroupDisbandEvent`.
- `game-server/src/com/aionemu/gameserver/model/team/common/events/ChangeLeaderEvent.java`
  - Fallback selection iterates members and stops on the first online non-current-leader player.
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`
  - `STR_PARTY_HE_BECOME_OFFLINE(String)` maps to message id `1300175`.

## C# Surface Reviewed

- `dotnetConversion/src/Aion.GameServer/Services/PlayerGroupRuntime.cs`
  - Already has group membership, descriptor, last-online, reconnect, leave, and leader-change helpers.
  - No disconnected-event planner or live logout fanout existed.
- `dotnetConversion/src/Aion.GameServer/Services/PlayerGroupReconnectPlan.cs`
  - Existing records for group member-info, leader-change, and leave packet plans were reused where possible.
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
  - Added the missing party offline system-message factory.

## Implemented

- Added `Aion.GameServer.Services.PlayerGroupDisconnectedPlanner`.
- Added `PlayerGroupDisconnectedPlan`, status enum, packet-intent enum, and packet-intent record.
- Planner behavior:
  - Returns `MissingGroup`/`MissingMember` when Java `checkCondition` would skip.
  - Returns `NoOnlineMembersDisband` when Java would call `PlayerGroupService.disband(group)`.
  - For leader disconnect, records fallback leader id and non-mutating leader-change packet intents.
  - Plans Java disconnected fanout to remaining members and the Java oddity of member-info packets back to the disconnecting player.
- Added `SmSystemMessage.PartyHeBecomeOffline`.
- Added `PlayerGroupDisconnectedPlannerTests`.

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `PlanReturnsMissingMemberWhenJavaCheckConditionWouldSkipEvent` | Unit | Java `PlayerDisconnectedEvent.checkCondition` source review | Stale grouped metadata with no runtime member produces no packet plan. | Focused C# assertions. | Does not execute Java logger side effect. |
| `PlanFlagsDisbandWhenJavaOnlineMembersAreEmpty` | Unit | Java `PlayerDisconnectedEvent.handleEvent` source review | No online members returns disband status and no disconnected packet fanout. | Focused C# assertions. | Does not execute live `PlayerGroupService.disband` or find-group cleanup. |
| `PlanCreatesNonLeaderDisconnectedFanoutLikeJavaPlayerDisconnectedEvent` | Unit | Java `PlayerDisconnectedEvent.handleEvent` source review | Non-leader disconnect plans offline system messages, disconnected member-info to remaining members, and member-info back to the disconnecting player. | Focused C# assertions over intent order, message id, event id, and packet plan metadata. | Does not send packets over live connections. |
| `PlanCreatesLeaderChangeBeforeDisconnectedFanoutForLeaderLogout` | Unit | Java `PlayerDisconnectedEvent`, `ChangeGroupLeaderEvent`, and `ChangeLeaderEvent` source review | Leader disconnect records first online non-leader fallback, leader-change packet metadata, and disconnected fanout without mutating runtime state. | Focused C# assertions over fallback id, leader-change system messages, group-info leader id, and disconnected intents. | Java `ConcurrentHashMap` member iteration order remains not deterministic; C# preserves runtime list order for planner evidence. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.model.team.group.events.PlayerDisconnectedEvent` | `Aion.GameServer.Services.PlayerGroupDisconnectedPlanner` | Event / Planner | Partial | Unit Tested | Partial Parity | Non-live planner covers missing-member skip, no-online disband status, leader-disconnect fallback metadata, and disconnected packet fanout. Live logout still does not invoke this planner or send packets. |
| `com.aionemu.gameserver.model.team.group.events.ChangeGroupLeaderEvent` | `Aion.GameServer.Services.PlayerGroupDisconnectedPlanner` plus `PlayerGroupRuntime.ChangeLeader` | Event / Planner | Partial | Unit Tested | Partial Parity | Planner models null-event fallback and packet metadata without mutating runtime state. Existing runtime has live-ish change helper, but logout does not compose it. |
| `com.aionemu.gameserver.model.team.common.events.ChangeLeaderEvent` | `Aion.GameServer.Services.PlayerGroupDisconnectedPlanner.SelectFallbackLeaderObjectId` | Event Base / Helper | Partial | Unit Tested | Partial Parity | Mirrors first online non-current-leader fallback for group disconnect planning. Java map iteration order is not guaranteed; C# runtime list order is deterministic. |
| `com.aionemu.gameserver.model.team.group.PlayerGroupService.onPlayerLogout` | `Aion.GameServer.Services.PlayerGroupRuntime.UpdateMemberLastOnlineTime` plus `PlayerGroupDisconnectedPlanner` | Logout Lifecycle | Partial | Unit Tested | Partial Parity | Last-online update already exists; disconnected-event planning now exists, but live `PlayerEnterWorldService.LeaveWorldAsync` does not dispatch group packets. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_PARTY_HE_BECOME_OFFLINE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage.PartyHeBecomeOffline` | Server Packet Factory | Partial | Unit Tested | Verified Parity | Message id `1300175` and string parameter asserted in focused planner tests. |

## Validation Decision

- Changed surface: one non-live group disconnected planner, one system-message factory, and focused tests.
- Specific behavior/contract: Java group `PlayerDisconnectedEvent` disband/leader-change/disconnected-fanout decisions, including member-info back to the disconnecting player.
- Focused C# command:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerGroupDisconnectedPlannerTests" --no-restore`
- Result:
  - Passed: 4
  - Failed: 0
  - Skipped: 0
- Focused Java/Maven command:
  - Not run; no Java source or fixtures changed, and Java evidence came from source review.
- Broad-validation trigger: none.
- Broad .NET decision:
  - Skipped; filtered test compiled affected project/dependencies and covered the new non-live planner plus system-message factory.
- Why this scope is sufficient:
  - The unit added a standalone non-live planner and did not enable live logout dispatch, repository writes, scheduler behavior, packet primitives, or shared serialization changes.

## Known Remaining Gaps

- `PlayerEnterWorldService.LeaveWorldAsync` still does not dispatch group disconnected-event packets.
- Planner does not execute live `PlayerGroupService.disband` equivalents or find-group cleanup.
- Planner does not send packets to real connections; it only creates packet intents.
- C# planner uses runtime list ordering, while Java `ConcurrentHashMap` member iteration order is not guaranteed.
- Live ordering between last-online update, leader change, disconnected fanout, disband, find-group cleanup, and logout persistence remains unverified.

## Summary Metrics

- Java artifacts reviewed: 5.
- C# artifacts reviewed: 4.
- Production files changed: 2.
- Test files changed: 1.
- Focused validation commands passed: 1.
- Artifacts with verified parity: 1.
- Artifacts needing verification or partial parity: 4.
- Blocked artifacts: 0.
- Estimated Phase 6 completion: unchanged, still conservatively partial.
