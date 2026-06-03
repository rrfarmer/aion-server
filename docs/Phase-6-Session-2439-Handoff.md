# Phase 6 Session 2439 Handoff

## Current Phase

- Phase 6: Port Game Core.

## Last Completed UOW

- UOW-2439: Scaffold alliance offline-timeout runtime hook.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceInfoPlan.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceRuntime.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerAllianceRuntimeTests.cs`
- `docs/Phase-6-Session-2439-Completion.md`
- `docs/Phase-6-Session-2439-Handoff.md`

## Java Artifacts Reviewed

- `game-server/src/com/aionemu/gameserver/model/team/alliance/PlayerAllianceService.java`
- `game-server/src/com/aionemu/gameserver/model/team/alliance/events/PlayerAllianceLeavedEvent.java`
- `game-server/src/com/aionemu/gameserver/model/team/common/events/PlayerLeavedEvent.java`
- `game-server/src/com/aionemu/gameserver/configs/main/GroupConfig.java`
- `game-server/src/com/aionemu/gameserver/model/team/TeamType.java`

## Tests Run

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionPlayerStatusInfoTests|FullyQualifiedName~PlayerAllianceRuntimeTests" --no-restore
```

- Result: passed, 101 tests.
- Java/Maven: not run because no Java source or fixtures changed and no narrow Java test exists for this runtime hook.
- Broad .NET: skipped; no broad-validation trigger applied after the focused command compiled and passed.
- Hygiene: `git diff --check` passed with line-ending normalization warnings only.

## What Changed

- Added `PlayerAllianceRuntime.RemoveNextExpiredOfflineMemberWithLeaveWorkflow(...)` as a narrow C# event-source hook for Java `OfflinePlayerAllianceChecker`.
- Timeout eligibility now mirrors Java delay selection: `AUTO_ALLIANCE` expires after 60 seconds, other alliances use configured alliance remove time.
- The hook removes the expired offline member through the existing `LEAVE_TIMEOUT` workflow and returns a `PlayerAllianceOfflineTimeoutPlan`.
- In-league timeout disband defers final alliance cleanup so a future dispatcher can send league-left before alliance disband packets, matching Java `PlayerAllianceService.disband(team, true)`.
- Offence alliance timeout side effect is surfaced as `WouldRemoveOffenceInvader`.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.OfflinePlayerAllianceChecker` | `Aion.GameServer.Services.PlayerAllianceRuntime.RemoveNextExpiredOfflineMemberWithLeaveWorkflow` | Scheduler/Event Source | Partial | Regression Tested | Partial Parity | Runtime hook and timeout eligibility are covered. Live scheduler and dispatcher integration remain open. |
| `com.aionemu.gameserver.model.team.alliance.events.PlayerAllianceLeavedEvent` | `Aion.GameServer.Services.PlayerAllianceLeaveWorkflowPlanner`; `Aion.GameServer.Services.PlayerAllianceRuntime` | Event/Workflow | Partial | Regression Tested | Partial Parity | `LEAVE_TIMEOUT` removes the member, skips ordinary league broadcast, disbands when needed, and keeps base leave after alliance leave. |
| `com.aionemu.gameserver.model.team.TeamType` | `Aion.GameServer.Services.PlayerAllianceTeamTypeExtensions` | Enum Helper | Partial | Unit Tested | Partial Parity | Timeout-relevant `isAutoTeam` and `isOffence` helper behavior is represented. |
| `com.aionemu.gameserver.configs.main.GroupConfig` | `RemoveNextExpiredOfflineMemberWithLeaveWorkflow(..., allianceRemoveTimeSeconds)` | Config Boundary | Partial | Regression Tested | Partial Parity | Config value is accepted by the hook; no global C# config binding was introduced. |

## Known Gaps

- No scheduled C# service invokes the alliance timeout hook yet.
- Timeout workflow dispatch is not connected to `GameServerConnection` or an equivalent connection registry fanout service.
- Offence `VortexService.removeInvaderPlayer` behavior is planned but not executed.
- In-league timeout disband needs an end-to-end dispatch test for league-left before alliance disband packets.
- In-league leader leave with no online fallback remains risky.

## Next Recommended UOW

UOW-2440: Wire a narrow alliance offline-timeout dispatcher around `RemoveNextExpiredOfflineMemberWithLeaveWorkflow`, or add a focused dispatcher audit if no safe invocation point exists.

Suggested Java artifacts:

- `game-server/src/com/aionemu/gameserver/model/team/alliance/PlayerAllianceService.java`
- `game-server/src/com/aionemu/gameserver/model/team/alliance/events/PlayerAllianceLeavedEvent.java`
- `game-server/src/com/aionemu/gameserver/model/team/league/events/LeagueLeftEvent.java`
- `game-server/src/com/aionemu/gameserver/services/vortex/VortexService.java`

Suggested C# artifacts:

- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceRuntime.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerLeagueRuntime.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionPlayerStatusInfoTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerAllianceRuntimeTests.cs`

Suggested focused validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionPlayerStatusInfoTests|FullyQualifiedName~PlayerAllianceRuntimeTests" --no-restore
```

Specific behavior to prove: a timeout dispatcher sends Java `LEAVE_TIMEOUT` fanout, skips ordinary `League.broadcast(team)`, removes the alliance from league before alliance disband packets for `disband(team, true)`, then emits base leave side effects. Java/Maven is not expected unless Java source or fixtures change or a narrow Java fixture is created. Broad-validation trigger: none unless shared scheduler/runtime infrastructure is introduced.

## Safe Alternate Candidates

- Add an offence-timeout integration plan around the C# equivalent of `VortexService.removeInvaderPlayer`.
- Add command-side in-league offline recipient skip coverage.
- Audit in-league leader leave with no online fallback before changing command dispatch.
