# Phase 6 Session 2426 Completion

Status: Phase 6 continues; UOW-2426 enabled a narrow live C# group disconnected-event dispatch from logout. Alliance disconnected dispatch and group disband cleanup remain unwired.

## Scope

- UOW: UOW-2426 group disconnected live logout dispatch.
- Reviewed Java logout ordering, group/alliance logout hooks, and `PacketSendUtility`.
- Reviewed C# logout, connection registry, group runtime, alliance runtime, and existing group/alliance disconnected planners.
- Wired group disconnected planner output into `PlayerEnterWorldService.LeaveWorldAsync`.
- Added focused logout tests for non-leader and leader group disconnect dispatch.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/player/PlayerLeaveWorldService.java`
  - Sets `player.clientConnection` to null at the start of `leaveWorld`, making the player semi-offline for `PacketSendUtility`.
  - Calls `PlayerGroupService.onPlayerLogout(player)` before `PlayerAllianceService.onPlayerLogout(player)`.
  - Team logout occurs before later common-data online/last-online and final player storage operations.
- `game-server/src/com/aionemu/gameserver/model/team/group/PlayerGroupService.java`
  - `onPlayerLogout` updates `PlayerGroupMember.lastOnlineTime`, then fires `PlayerDisconnectedEvent`.
- `game-server/src/com/aionemu/gameserver/model/team/group/events/PlayerDisconnectedEvent.java`
  - No online members: disband.
  - Leader disconnect: fire `ChangeGroupLeaderEvent`, then disconnected fanout.
  - Sends offline system message and `SM_GROUP_MEMBER_INFO(DISCONNECTED)` to each other member; also attempts member-info sends back to the disconnecting player.
- `game-server/src/com/aionemu/gameserver/model/team/group/events/ChangeGroupLeaderEvent.java`
  - Changes leader and sends `SM_GROUP_INFO` plus leader-change system messages.
- `game-server/src/com/aionemu/gameserver/model/team/alliance/PlayerAllianceService.java`
  - Alliance logout has the same last-online-before-disconnected-event shape, but live C# alliance dispatch remains gated.
- `game-server/src/com/aionemu/gameserver/utils/PacketSendUtility.java`
  - `sendPacket(Player, packet)` only sends when `player.isOnline()`, so the semi-offline logout player does not receive packets.

## C# Surface Reviewed

- `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
  - `LeaveWorldAsync` already marks the player offline, removes from world, persists logout, and updates group/alliance member last-online.
  - Now dispatches live group disconnected packets after the group last-online update.
- `dotnetConversion/src/Aion.GameServer/Network/Aion/IGameClientConnectionRegistry.cs`
  - Existing `SendPacketToPlayerAsync` is the live packet-recipient boundary.
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
  - CM_QUIT path unregisters the active player connection before calling `LeaveWorldAsync`, matching Java's no-send-to-self result.
- `dotnetConversion/src/Aion.GameServer/Services/PlayerGroupDisconnectedPlanner.cs`
  - Existing non-live planner now feeds live logout dispatch.
- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceDisconnectedPlanner.cs`
  - Still non-live; live alliance dispatch remains blocked by leader-change/disband composition.

## Implemented

- Added `DispatchGroupDisconnectedLogoutAsync` inside `PlayerEnterWorldService`.
- On group leader disconnect, applies `PlayerGroupRuntime.ChangeLeader` before sending leader-change packet intents.
- Sends leader-change `SM_GROUP_INFO` and system-message packets to remaining recipients.
- Sends disconnected offline system messages and `SM_GROUP_MEMBER_INFO(DISCONNECTED)` packets to remaining recipients.
- Skips packet intents whose recipient is the logging-out player, preserving Java's effective `PacketSendUtility` no-op after the player is semi-offline.
- Left alliance disconnected dispatch and no-online group disband cleanup unwired.

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `LeaveWorld_DispatchesGroupDisconnectedFanoutToRemainingMembersLikeJavaLogout` | Regression | Java `PlayerLeaveWorldService`, `PlayerGroupService.onPlayerLogout`, `PlayerDisconnectedEvent`, and `PacketSendUtility` source review | Non-leader group logout sends offline system messages and disconnected member-info to remaining members, and does not send to the logging-out player. | Focused C# logout/registry assertions over recipient order, packet types, and system-message id `1300175`. | Does not cover no-online disband cleanup. |
| `LeaveWorld_DispatchesGroupLeaderChangeBeforeDisconnectedFanoutLikeJavaLogout` | Regression | Java `PlayerDisconnectedEvent` and `ChangeGroupLeaderEvent` source review | Leader logout mutates C# group leader to fallback, sends leader-change packets before disconnected fanout, and skips self-recipient sends. | Focused C# logout/registry assertions over recipient order, packet types, leader-message ids, offline-message ids, and runtime leader mutation. | Java `ConcurrentHashMap` member iteration order remains not deterministic; C# uses runtime list order. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.player.PlayerLeaveWorldService.leaveWorld` | `Aion.GameServer.Services.PlayerEnterWorldService.LeaveWorldAsync` | Logout Lifecycle | Partial | Regression Tested | Partial Parity | C# now dispatches live group disconnected packets from logout. Other Java logout ordering differences remain, including exact world-removal/persistence ordering and alliance disconnected dispatch. |
| `com.aionemu.gameserver.model.team.group.PlayerGroupService.onPlayerLogout` | `Aion.GameServer.Services.PlayerEnterWorldService.DispatchGroupDisconnectedLogoutAsync` plus `PlayerGroupRuntime.UpdateMemberLastOnlineTime` | Service / Logout Hook | Partial | Regression Tested | Partial Parity | Group last-online update feeds live disconnected dispatch. No-online disband cleanup remains unwired. |
| `com.aionemu.gameserver.model.team.group.events.PlayerDisconnectedEvent` | `Aion.GameServer.Services.PlayerGroupDisconnectedPlanner` plus `PlayerEnterWorldService.DispatchGroupDisconnectedLogoutAsync` | Event / Live Dispatch | Partial | Regression Tested | Partial Parity | Live dispatch covers planned non-disband fanout and leader-disconnect ordering. Disband branch remains status-only. |
| `com.aionemu.gameserver.model.team.group.events.ChangeGroupLeaderEvent` | `Aion.GameServer.Services.PlayerGroupRuntime.ChangeLeader` plus logout dispatch | Event / Runtime Mutation | Partial | Regression Tested | Partial Parity | Leader logout now mutates runtime fallback leader before sending group-info/system-message packets to remaining members. |
| `com.aionemu.gameserver.utils.PacketSendUtility.sendPacket` | `Aion.GameServer.Network.Aion.IGameClientConnectionRegistry.SendPacketToPlayerAsync` plus logout self-recipient skip | Packet Send Utility | Partial | Regression Tested | Partial Parity | C# preserves the Java effective no-send-to-semi-offline-player result for group logout. Broader PacketSendUtility parity is not implied. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.onPlayerLogout` | `Aion.GameServer.Services.PlayerAllianceRuntime.UpdateMemberLastOnlineTime` plus `PlayerAllianceDisconnectedPlanner` | Logout Lifecycle | Partial | Unit Tested | Partial Parity | Last-online and non-live planner exist, but live alliance disconnected dispatch remains unwired. |

## Validation Decision

- Changed surface: live logout dispatch for group disconnected packets using existing group runtime/planner and connection registry.
- Specific behavior/contract: Java group logout updates member last-online before `PlayerDisconnectedEvent`; leader disconnect changes leader before disconnected fanout; semi-offline logging-out player receives no packets.
- Focused C# command:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~PlayerGroupDisconnectedPlannerTests" --no-restore`
- Result:
  - Passed: 52
  - Failed: 0
  - Skipped: 0
- Focused Java/Maven command:
  - Not run; no Java source or fixtures changed, and Java evidence came from source review.
- Broad-validation trigger:
  - live logout dispatch and connection-registry packet send boundary.
- Broad .NET decision:
  - Full project/solution validation skipped after focused evidence; the changed surface is isolated to `LeaveWorldAsync` group dispatch and the filtered command compiled affected dependencies while asserting registry sends, packet order, self-skip behavior, and runtime leader mutation.
- Why this scope is sufficient:
  - No packet primitive, serialization helper, repository contract, scheduler, or shared connection registry implementation was changed. The live effect uses existing packet classes and existing `SendPacketToPlayerAsync`; tests exercise the exact logout branch that was wired.

## Known Remaining Gaps

- Alliance disconnected-event live dispatch remains unwired.
- Group no-online disband branch remains unwired; find-group recruitment cleanup is not triggered by this UOW.
- C# `LeaveWorldAsync` still has broader ordering differences from Java around world removal and full persistence timing.
- Live group disconnected dispatch does not attempt sends to the logging-out player; this preserves Java's effective no-op after connection nulling but does not reproduce the attempted call.
- Java group member iteration uses `ConcurrentHashMap` values; C# uses runtime list order.

## Summary Metrics

- Java artifacts reviewed: 6.
- C# artifacts reviewed: 6.
- Production files changed: 1.
- Test files changed: 1.
- Focused validation commands passed: 1.
- Artifacts with verified parity: 0.
- Artifacts needing verification or partial parity: 6.
- Blocked artifacts: 0.
- Estimated Phase 6 completion: unchanged, still conservatively partial.
