# Phase 6 Session 2425 Handoff

## Current Phase

- Phase 6 Java-to-C# parity migration continues on branch `4.8`.
- Latest completed UOW: UOW-2425 group disconnected-event planner.

## Last Completed UOW

- UOW-2425 added `PlayerGroupDisconnectedPlanner`.
- Group logout disconnected-event behavior is now represented as non-live C# packet intents.
- Live logout dispatch remains unwired.

## Commits Made

- Pending commit for this handoff: `[Phase 6][UOW-2425] Add group disconnected planner`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerGroupDisconnectedPlanner.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerGroupDisconnectedPlannerTests.cs`
- `docs/Phase-6-Session-2425-Completion.md`
- `docs/Phase-6-Session-2425-Handoff.md`

## Java Artifacts Reviewed

- `game-server/src/com/aionemu/gameserver/model/team/group/events/PlayerDisconnectedEvent.java`
- `game-server/src/com/aionemu/gameserver/model/team/group/events/ChangeGroupLeaderEvent.java`
- `game-server/src/com/aionemu/gameserver/model/team/group/PlayerGroupService.java`
- `game-server/src/com/aionemu/gameserver/model/team/common/events/ChangeLeaderEvent.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage`
- `Aion.GameServer.Services.PlayerGroupDisconnectedPlanner`
- `Aion.GameServer.Tests.PlayerGroupDisconnectedPlannerTests`

## What Changed

- Added `SmSystemMessage.PartyHeBecomeOffline` for Java message id `1300175`.
- Added group disconnected plan/status/packet-intent records.
- Added non-live planning for:
  - missing group/member skip
  - no-online-members disband status
  - disconnected leader fallback metadata
  - `SM_GROUP_INFO`/leader-message metadata for fallback leader change
  - offline system message and `SM_GROUP_MEMBER_INFO(DISCONNECTED)` fanout
  - Java oddity that sends member-info about remaining members back to the disconnecting player
- Added focused tests for the planner.

## Tests Run

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerGroupDisconnectedPlannerTests" --no-restore`
- Result:
  - Passed: 4
  - Failed: 0
  - Skipped: 0
- Pre-existing nullable/analyzer warnings remain.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.model.team.group.events.PlayerDisconnectedEvent` | `Aion.GameServer.Services.PlayerGroupDisconnectedPlanner` | Event / Planner | Partial | Unit Tested | Partial Parity | Non-live planner covers missing-member skip, no-online disband status, leader-disconnect fallback metadata, and disconnected packet fanout. Live logout still does not invoke this planner or send packets. |
| `com.aionemu.gameserver.model.team.group.events.ChangeGroupLeaderEvent` | `Aion.GameServer.Services.PlayerGroupDisconnectedPlanner` plus `PlayerGroupRuntime.ChangeLeader` | Event / Planner | Partial | Unit Tested | Partial Parity | Planner models null-event fallback and packet metadata without mutating runtime state. Existing runtime has live-ish change helper, but logout does not compose it. |
| `com.aionemu.gameserver.model.team.common.events.ChangeLeaderEvent` | `Aion.GameServer.Services.PlayerGroupDisconnectedPlanner.SelectFallbackLeaderObjectId` | Event Base / Helper | Partial | Unit Tested | Partial Parity | Mirrors first online non-current-leader fallback for group disconnect planning. Java map iteration order is not guaranteed; C# runtime list order is deterministic. |
| `com.aionemu.gameserver.model.team.group.PlayerGroupService.onPlayerLogout` | `Aion.GameServer.Services.PlayerGroupRuntime.UpdateMemberLastOnlineTime` plus `PlayerGroupDisconnectedPlanner` | Logout Lifecycle | Partial | Unit Tested | Partial Parity | Last-online update already exists; disconnected-event planning now exists, but live `PlayerEnterWorldService.LeaveWorldAsync` does not dispatch group packets. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_PARTY_HE_BECOME_OFFLINE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage.PartyHeBecomeOffline` | Server Packet Factory | Partial | Unit Tested | Verified Parity | Message id `1300175` and string parameter asserted in focused planner tests. |

## Known Gaps

- No live group disconnected-event dispatch from logout.
- No live alliance disconnected-event dispatch from logout.
- Group disconnected disband status is non-live and does not remove find-group recruitment.
- Planner packet intents are not sent to client connections.
- Java `ConcurrentHashMap` group member iteration ordering is not deterministic; C# planner follows runtime list order for testable intent ordering.

## Remaining Risks

- Live logout integration must preserve Java order:
  - update group member last-online
  - disconnected event
  - disband or leader change
  - disconnected fanout
  - persistence/connection teardown
- Live packet fanout to the disconnecting player may no-op depending on connection teardown timing; Java attempts the send, so C# should document or reproduce that behavior intentionally.
- Find-group recruitment cleanup is tied to disband and must not be skipped when live group disband is enabled.

## Next Recommended UOW

UOW-2426: Discover logout team-disconnected live dispatch blockers for group/alliance.

Suggested scope:
- Java:
  - `game-server/src/com/aionemu/gameserver/services/player/PlayerLeaveWorldService.java`
  - `game-server/src/com/aionemu/gameserver/model/team/group/PlayerGroupService.java`
  - `game-server/src/com/aionemu/gameserver/model/team/alliance/PlayerAllianceService.java`
  - `game-server/src/com/aionemu/gameserver/utils/PacketSendUtility.java`
- C#:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerGroupRuntime.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerGroupDisconnectedPlanner.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceDisconnectedPlanner.cs`
  - connection registry / packet send adapter surfaces used by `LeaveWorldAsync`
- Goal:
  - Determine the smallest safe live-dispatch step, or add a readiness/adapter plan if direct live dispatch still lacks a packet-sending boundary.
  - Do not enable broad live dispatch until packet recipient lookup and teardown ordering are proven.

## Focused Validation Recipe

- Specific behavior/contract:
  - Java logout invokes team last-online update before disconnected-event dispatch; C# should either prove a safe live packet-dispatch boundary or document the exact blockers before enabling it.
- Focused C# command:
  - If editing only readiness/planner docs or adding a non-live readiness service:
    - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerGroupDisconnectedPlannerTests|FullyQualifiedName~PlayerAllianceMemberInfoTests|FullyQualifiedName~PlayerEnterWorldServiceTests" --no-restore`
  - If only documentation:
    - `git diff --check`
  - If enabling live logout dispatch:
    - Start with the edited logout/connection test class filter only, then add planner classes if the dispatch test composes them.
- Java/Maven:
  - Not expected unless Java source or fixtures change; Java evidence should come from source review unless a targeted Java test is found.
- Broad-validation trigger:
  - none for docs/non-live readiness.
  - live dispatch, shared runtime wiring, or connection send adapter changes are broad-validation triggers and must be documented before any unfiltered project/solution validation.

## Context Needed By Next Session

- Read `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, and the latest completion document before work.
- Do not use `docs/PHASE-6-PROGRESS.md` during normal startup.
- Java remains the source of truth.
- Current group/alliance disconnected planners are non-live; they should not be represented as full parity until logout dispatch and packet sending are objectively validated.
