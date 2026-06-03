# Phase 6 Session 2424 Handoff

## Current Phase

- Phase 6 Java-to-C# parity migration continues on branch `4.8`.
- Latest completed UOW: UOW-2424 alliance disconnected leader fanout planner.

## Last Completed UOW

- UOW-2424 updated `PlayerAllianceDisconnectedPlanner`.
- Disconnected leader plans now continue Java `PlayerDisconnectedEvent` offline fanout instead of returning an empty deferred plan.
- Live logout dispatch remains unwired.

## Commits Made

- Pending commit for this handoff: `[Phase 6][UOW-2424] Cover alliance leader disconnect fanout`.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceDisconnectedPlanner.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerAllianceMemberInfoTests.cs`
- `docs/Phase-6-Session-2424-Completion.md`
- `docs/Phase-6-Session-2424-Handoff.md`

## Java Artifacts Reviewed

- `game-server/src/com/aionemu/gameserver/model/team/alliance/events/PlayerDisconnectedEvent.java`
- `game-server/src/com/aionemu/gameserver/model/team/alliance/events/ChangeAllianceLeaderEvent.java`

## C# Artifacts Touched

- `Aion.GameServer.Services.PlayerAllianceDisconnectedPlanner`
- `Aion.GameServer.Tests.PlayerAllianceMemberInfoTests`

## What Changed

- Removed the early `LeaderDisconnectDeferred` empty-intent branch.
- Added fallback leader selection for leader disconnect metadata.
- Preserved disconnected packet fanout intent planning for remaining members.
- Kept the plan non-live and explicitly flagged `WouldTriggerLeaderChange`.

## Tests Run

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerAllianceMemberInfoTests" --no-restore`
- Result:
  - Passed: 42
  - Failed: 0
  - Skipped: 0
- Pre-existing nullable/analyzer warnings remain.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.model.team.alliance.events.PlayerDisconnectedEvent` | `Aion.GameServer.Services.PlayerAllianceDisconnectedPlanner` | Event / Planner | Partial | Unit Tested | Partial Parity | Non-live planner now covers non-leader and leader disconnected fanout intent ordering. Live logout still does not invoke this planner or send packets. |
| `com.aionemu.gameserver.model.team.alliance.events.ChangeAllianceLeaderEvent` | `Aion.GameServer.Services.PlayerAllianceDisconnectedPlanner.SelectFallbackLeaderObjectId` plus `PlayerAllianceLeaderChangePlanner` | Event / Planner | Partial | Unit Tested | Partial Parity | Disconnected planner now mirrors fallback leader selection for packet metadata. Actual leader-change side effects remain separate/non-live. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.onPlayerLogout` | `Aion.GameServer.Services.PlayerEnterWorldService.LeaveWorldAsync` plus `PlayerAllianceRuntime.UpdateMemberLastOnlineTime` | Logout Lifecycle | Partial | Regression Tested | Partial Parity | Last-online update is live in C# from prior UOWs, but disconnected-event fanout remains unwired from logout. |

## Known Gaps

- No live alliance disconnected-event dispatch from logout.
- No live group disconnected-event dispatch from logout.
- Group disconnected-event C# planner location needs discovery; the previously suggested standalone class names do not exist.
- Live Java ordering around team events and persistence remains partial.

## Remaining Risks

- C# leader-disconnect fallback uses supplied `Player.IsOnline` snapshots; live runtime wiring must ensure those snapshots match Java online-member state after logout.
- Java in-league leader-change behavior has additional league broadcast/system-message branches; this UOW only fixed disconnected-event fanout metadata.
- Future live dispatch needs connection-registry packet ordering evidence.

## Next Recommended UOW

UOW-2425: Discover and close the group disconnected-event planner gap.

Suggested scope:
- Java:
  - `game-server/src/com/aionemu/gameserver/model/team/group/events/PlayerDisconnectedEvent.java`
  - `game-server/src/com/aionemu/gameserver/model/team/group/events/ChangeGroupLeaderEvent.java`
  - `game-server/src/com/aionemu/gameserver/model/team/group/PlayerGroupService.java`
- C#:
  - Search current group planner/runtime surfaces.
  - If no disconnected planner exists, add a small non-live `PlayerGroupDisconnectedPlanner` and focused tests.
  - If a planner exists under another name, fix the narrow Java mismatch found by source review.
  - Do not enable live logout dispatch yet.

## Focused Validation Recipe

- Specific behavior/contract:
  - Java group disconnected event disbands when no online members remain; otherwise leader disconnect triggers leader-change and disconnected member-info/system-message fanout to remaining members, including the Java oddity that it also sends member-info about each remaining member to the disconnecting player.
- Focused C# command:
  - If adding a standalone planner:
    - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerGroupDisconnectedPlannerTests" --no-restore`
  - If editing an existing group test class:
    - Use the edited test class filter only.
- Java/Maven:
  - Not expected unless Java source or fixtures change; Java evidence should come from source review in the test/document notes.
- Broad-validation trigger:
  - none unless the unit enables live logout dispatch, shared runtime wiring, packet primitive changes, or repository changes.

## Context Needed By Next Session

- Read `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, and the latest completion document before work.
- Do not use `docs/PHASE-6-PROGRESS.md` during normal startup.
- Java remains the source of truth.
- Prefer concrete runtime-enabling work over additional metadata unless it removes a specific blocker.
