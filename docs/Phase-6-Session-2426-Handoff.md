# Phase 6 Session 2426 Handoff

## Current Phase

- Phase 6 Java-to-C# parity migration continues on branch `4.8`.
- Latest completed UOW: UOW-2426 live group disconnected logout dispatch.

## Last Completed UOW

- UOW-2426 wired group disconnected planner output into `PlayerEnterWorldService.LeaveWorldAsync`.
- Group logout now sends live group disconnected packets to remaining recipients through `IGameClientConnectionRegistry`.
- Alliance disconnected dispatch and group disband cleanup remain unwired.

## Commits Made

- Pending commit for this handoff: `[Phase 6][UOW-2426] Wire group disconnected logout dispatch`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`
- `docs/Phase-6-Session-2426-Completion.md`
- `docs/Phase-6-Session-2426-Handoff.md`

## Java Artifacts Reviewed

- `game-server/src/com/aionemu/gameserver/services/player/PlayerLeaveWorldService.java`
- `game-server/src/com/aionemu/gameserver/model/team/group/PlayerGroupService.java`
- `game-server/src/com/aionemu/gameserver/model/team/group/events/PlayerDisconnectedEvent.java`
- `game-server/src/com/aionemu/gameserver/model/team/group/events/ChangeGroupLeaderEvent.java`
- `game-server/src/com/aionemu/gameserver/model/team/alliance/PlayerAllianceService.java`
- `game-server/src/com/aionemu/gameserver/utils/PacketSendUtility.java`

## C# Artifacts Touched

- `Aion.GameServer.Services.PlayerEnterWorldService`
- `Aion.GameServer.Tests.PlayerEnterWorldServiceTests`

## What Changed

- `LeaveWorldAsync` now calls `DispatchGroupDisconnectedLogoutAsync` after updating group/alliance member last-online.
- The dispatch composes `PlayerGroupDisconnectedPlanner` output with the existing live connection registry.
- Leader logout applies `PlayerGroupRuntime.ChangeLeader` before sending leader-change packets.
- Packets sent to remaining group members:
  - `SM_GROUP_INFO` and party leader-change system messages for leader disconnects.
  - `STR_PARTY_HE_BECOME_OFFLINE` and `SM_GROUP_MEMBER_INFO(DISCONNECTED)` fanout.
- Self-recipient packet intents are skipped because Java nulls the client connection before team logout, making `PacketSendUtility.sendPacket(player, ...)` a no-op.

## Tests Run

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~PlayerGroupDisconnectedPlannerTests" --no-restore`
- Result:
  - Passed: 52
  - Failed: 0
  - Skipped: 0
- `git diff --check`
- Result:
  - Passed; only Git CRLF conversion warnings.
- Pre-existing nullable/analyzer warnings remain.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.player.PlayerLeaveWorldService.leaveWorld` | `Aion.GameServer.Services.PlayerEnterWorldService.LeaveWorldAsync` | Logout Lifecycle | Partial | Regression Tested | Partial Parity | C# now dispatches live group disconnected packets from logout. Other Java logout ordering differences remain, including exact world-removal/persistence ordering and alliance disconnected dispatch. |
| `com.aionemu.gameserver.model.team.group.PlayerGroupService.onPlayerLogout` | `Aion.GameServer.Services.PlayerEnterWorldService.DispatchGroupDisconnectedLogoutAsync` plus `PlayerGroupRuntime.UpdateMemberLastOnlineTime` | Service / Logout Hook | Partial | Regression Tested | Partial Parity | Group last-online update feeds live disconnected dispatch. No-online disband cleanup remains unwired. |
| `com.aionemu.gameserver.model.team.group.events.PlayerDisconnectedEvent` | `Aion.GameServer.Services.PlayerGroupDisconnectedPlanner` plus `PlayerEnterWorldService.DispatchGroupDisconnectedLogoutAsync` | Event / Live Dispatch | Partial | Regression Tested | Partial Parity | Live dispatch covers planned non-disband fanout and leader-disconnect ordering. Disband branch remains status-only. |
| `com.aionemu.gameserver.model.team.group.events.ChangeGroupLeaderEvent` | `Aion.GameServer.Services.PlayerGroupRuntime.ChangeLeader` plus logout dispatch | Event / Runtime Mutation | Partial | Regression Tested | Partial Parity | Leader logout now mutates runtime fallback leader before sending group-info/system-message packets to remaining members. |
| `com.aionemu.gameserver.utils.PacketSendUtility.sendPacket` | `Aion.GameServer.Network.Aion.IGameClientConnectionRegistry.SendPacketToPlayerAsync` plus logout self-recipient skip | Packet Send Utility | Partial | Regression Tested | Partial Parity | C# preserves the Java effective no-send-to-semi-offline-player result for group logout. Broader PacketSendUtility parity is not implied. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.onPlayerLogout` | `Aion.GameServer.Services.PlayerAllianceRuntime.UpdateMemberLastOnlineTime` plus `PlayerAllianceDisconnectedPlanner` | Logout Lifecycle | Partial | Unit Tested | Partial Parity | Last-online and non-live planner exist, but live alliance disconnected dispatch remains unwired. |

## Known Gaps

- Alliance disconnected-event live dispatch remains unwired.
- Group no-online disband branch remains status-only and does not remove group/find-group state.
- C# logout ordering is still not fully Java-identical:
  - C# connection path broadcasts `SmDelete` and unregisters the active player before `LeaveWorldAsync`.
  - C# `SavePlayerLogoutAsync` currently occurs before group disconnected dispatch.
- Java's attempted member-info sends back to the disconnected player are represented by planner intents but skipped in live dispatch to match the effective semi-offline no-op.

## Remaining Risks

- Live alliance dispatch needs leader-change, disband, league, and find-group cleanup composition before wiring.
- Group disband must remove recruitment and clear runtime state in Java order before being enabled.
- Packet order for group logout is now tested through the registry fake, but not yet exercised with a real client connection.
- Java `ConcurrentHashMap` member iteration order remains not deterministic; C# runtime list order is deterministic.

## Next Recommended UOW

UOW-2427: Close the group no-online disband branch from logout, or add a readiness plan if live disband cannot be made safe in one narrow step.

Suggested scope:
- Java:
  - `game-server/src/com/aionemu/gameserver/model/team/group/events/PlayerDisconnectedEvent.java`
  - `game-server/src/com/aionemu/gameserver/model/team/group/PlayerGroupService.java`
  - `game-server/src/com/aionemu/gameserver/model/team/group/events/GroupDisbandEvent.java`
  - `game-server/src/com/aionemu/gameserver/model/team/group/events/PlayerGroupLeavedEvent.java`
  - `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
- C#:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerGroupRuntime.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerGroupDisconnectedPlanner.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerGroupRuntimeTests.cs`
- Goal:
  - Either enable live logout disband when no group members remain online, preserving find-group recruitment cleanup and runtime clearing, or document the exact missing runtime operation with focused tests.

## Focused Validation Recipe

- Specific behavior/contract:
  - Java group disconnected event calls `PlayerGroupService.disband(group)` when `group.getOnlineMembers().isEmpty()`, which removes find-group recruitment, removes the group, and runs disband leave events for remaining members.
- Focused C# command:
  - If enabling or changing live group disband/logout behavior:
    - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~PlayerGroupRuntimeTests|FullyQualifiedName~PlayerGroupDisconnectedPlannerTests" --no-restore`
  - If only adding a non-live readiness/doc plan:
    - use the edited readiness test class filter, or `git diff --check` for docs-only.
- Java/Maven:
  - Not expected unless Java source or fixtures change; Java source review should be enough unless a targeted Java group test is found.
- Broad-validation trigger:
  - live runtime disband/connection dispatch/find-group cleanup is a broad-validation trigger; document it before considering unfiltered project/solution validation.

## Context Needed By Next Session

- Read `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, and the latest completion document before work.
- Do not use `docs/PHASE-6-PROGRESS.md` during normal startup.
- Java remains the source of truth.
- UOW-2426 made group disconnected non-disband logout live, but not alliance dispatch or group disband.
