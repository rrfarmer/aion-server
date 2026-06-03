# Phase 6 Session 2427 Handoff

## Current Phase

- Phase 6 Java-to-C# parity migration continues on branch `4.8`.
- Latest completed UOW: UOW-2427 group disconnected no-online disband.

## Last Completed UOW

- UOW-2427 wired the Java group disconnected disband branch into C# logout.
- When the logging-out player leaves no online group members, C# now removes team find-group recruitment, clears group runtime state, and sends no live packets.
- Alliance disconnected dispatch remains unwired.

## Commits Made

- Pending commit for this handoff: `[Phase 6][UOW-2427] Wire group disconnected disband`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerGroupReconnectPlan.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerGroupRuntime.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerGroupRuntimeTests.cs`
- `docs/Phase-6-Session-2427-Completion.md`
- `docs/Phase-6-Session-2427-Handoff.md`

## Java Artifacts Reviewed

- `game-server/src/com/aionemu/gameserver/model/team/group/events/PlayerDisconnectedEvent.java`
- `game-server/src/com/aionemu/gameserver/model/team/group/PlayerGroupService.java`
- `game-server/src/com/aionemu/gameserver/model/team/group/events/GroupDisbandEvent.java`
- `game-server/src/com/aionemu/gameserver/model/team/group/events/PlayerGroupLeavedEvent.java`
- `game-server/src/com/aionemu/gameserver/model/team/common/events/PlayerLeavedEvent.java`

## C# Artifacts Touched

- `Aion.GameServer.Services.PlayerEnterWorldService`
- `Aion.GameServer.Services.PlayerGroupRuntime`
- `Aion.GameServer.Services.PlayerGroupDisconnectedDisbandPlan`
- `Aion.GameServer.Tests.PlayerEnterWorldServiceTests`
- `Aion.GameServer.Tests.PlayerGroupRuntimeTests`

## What Changed

- Added `PlayerGroupRuntime.DisbandAfterDisconnectedNoOnlineMembers(int teamId)`.
- Added `PlayerGroupDisconnectedDisbandPlan` metadata for disconnected logout disband cleanup.
- `PlayerEnterWorldService.DispatchGroupDisconnectedLogoutAsync` now handles `NoOnlineMembersDisband` before normal disconnected fanout.
- Added regression coverage for live logout disband when all group members are offline.
- Added runtime coverage for find-group team recruitment removal and member metadata clearing.

## Tests Run

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~PlayerGroupRuntimeTests|FullyQualifiedName~PlayerGroupDisconnectedPlannerTests" --no-restore`
- Result:
  - Passed: 101
  - Failed: 0
  - Skipped: 0
- `git diff --check`
- Result:
  - Passed; only Git CRLF conversion warnings.
- Pre-existing nullable/analyzer warnings remain.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.model.team.group.events.PlayerDisconnectedEvent` | `Aion.GameServer.Services.PlayerGroupDisconnectedPlanner` plus `PlayerEnterWorldService.DispatchGroupDisconnectedLogoutAsync` | Event / Live Dispatch | Partial | Regression Tested | Partial Parity | No-online disband branch is now live for group logout. Non-disband fanout and leader-disconnect dispatch were covered in UOW-2426. |
| `com.aionemu.gameserver.model.team.group.PlayerGroupService.disband` | `Aion.GameServer.Services.PlayerGroupRuntime.DisbandAfterDisconnectedNoOnlineMembers` | Service / Runtime Mutation | Partial | Unit Tested | Partial Parity | C# removes find-group recruitment and runtime group state for the no-online disconnected branch. Broader manual disband commands and offline timeout paths are not implied. |
| `com.aionemu.gameserver.model.team.group.events.GroupDisbandEvent` | `Aion.GameServer.Services.PlayerGroupDisconnectedDisbandPlan` plus runtime cleanup | Event / Runtime Mutation | Partial | Unit Tested | Partial Parity | C# replays the effective offline cleanup result and records base leave metadata, but does not live-dispatch Java `EventService.onLeftTeam`. |
| `com.aionemu.gameserver.model.team.group.events.PlayerGroupLeavedEvent` | `Aion.GameServer.Services.PlayerGroupRuntime` / `PlayerGroupLeavePlan` / `PlayerGroupDisconnectedDisbandPlan` | Event / Leave Handling | Partial | Unit Tested | Partial Parity | Normal leave disband and disconnected no-online disband now remove recruitment. Packet attempts to offline members are intentionally no live sends. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.removeRecruitment` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.RemoveRecruitment` | Service / State Store | Partial | Unit Tested | Partial Parity | Disconnected group disband now calls the existing team recruitment removal path. Live world broadcast intent remains metadata-only in this logout branch. |
| `com.aionemu.gameserver.model.team.common.events.PlayerLeavedEvent` | `Aion.GameServer.Services.PlayerBaseLeavePlanner` | Base Event / Side Effects | Partial | Unit Tested | Partial Parity | Offline no-online disband records that base leave would notify EventService and has no packet intents. Live EventService integration is still missing. |

## Known Gaps

- Alliance disconnected-event live dispatch remains unwired.
- Java `EventService.onLeftTeam` remains metadata-only in C# group disband.
- Manual group disband command parity and offline timeout disband parity still need review.
- C# logout ordering is still not fully Java-identical around world removal and persistence.
- Find-group world broadcast intent for team recruitment removal is recorded but not dispatched from logout disband.

## Remaining Risks

- Alliance logout may need leader-change, disband, league, and find-group cleanup composition before live dispatch.
- Group disband now mutates shared runtime state; focused tests covered the logout and runtime branches, but no broad suite was run.
- Java `ConcurrentHashMap` iteration order remains not deterministic; C# runtime list order is deterministic.
- Event-service side effects for team leave are not implemented live.

## Next Recommended UOW

UOW-2428: Start alliance disconnected live dispatch, or add a readiness gate if leader-change/disband/league cleanup cannot be safely wired in one narrow step.

Suggested scope:
- Java:
  - `game-server/src/com/aionemu/gameserver/model/team/alliance/PlayerAllianceService.java`
  - `game-server/src/com/aionemu/gameserver/model/team/alliance/events/PlayerDisconnectedEvent.java`
  - `game-server/src/com/aionemu/gameserver/model/team/alliance/events/ChangeAllianceLeaderEvent.java`
  - `game-server/src/com/aionemu/gameserver/model/team/alliance/events/AllianceDisbandEvent.java`
  - `game-server/src/com/aionemu/gameserver/model/team/league/LeagueService.java`
- C#:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceRuntime.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceDisconnectedPlanner.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerAllianceRuntimeTests.cs`

## Focused Validation Recipe

- Specific behavior/contract:
  - Java alliance logout updates member last-online, then `PlayerDisconnectedEvent` either disbands when no alliance members remain online or dispatches leader-change/offline fanout to remaining online members.
- Focused C# command:
  - If enabling or changing live alliance disconnected/logout behavior:
    - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~PlayerAllianceRuntimeTests|FullyQualifiedName~PlayerAllianceDisconnectedPlannerTests" --no-restore`
  - If only adding readiness/doc planning:
    - use the edited readiness test class filter, or `git diff --check` for docs-only.
- Java/Maven:
  - Not expected unless Java source or fixtures change; Java source review should be enough unless a targeted Java alliance test is found.
- Broad-validation trigger:
  - live alliance runtime mutation/connection dispatch/find-group or league cleanup is a broad-validation trigger; document it before considering unfiltered project/solution validation.

## Context Needed By Next Session

- Read `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, and the latest completion document before work.
- Do not use `docs/PHASE-6-PROGRESS.md` during normal startup.
- Java remains the source of truth.
- UOW-2426 made group disconnected non-disband logout live.
- UOW-2427 made group disconnected no-online disband live.
- Alliance disconnected dispatch is the next obvious logout/team parity gap.
