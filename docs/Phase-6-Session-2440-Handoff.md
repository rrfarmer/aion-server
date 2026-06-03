# Phase 6 Session 2440 Handoff

## Current Phase

- Phase 6: Port Game Core.

## Last Completed UOW

- UOW-2440: Wire alliance offline-timeout dispatcher.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceInfoPlan.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceRuntime.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceOfflineTimeoutDispatchService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerAllianceOfflineTimeoutDispatchServiceTests.cs`
- `docs/Phase-6-Session-2440-Completion.md`
- `docs/Phase-6-Session-2440-Handoff.md`

## Java Artifacts Reviewed

- `game-server/src/com/aionemu/gameserver/model/team/alliance/PlayerAllianceService.java`
- `game-server/src/com/aionemu/gameserver/model/team/alliance/events/PlayerAllianceLeavedEvent.java`
- `game-server/src/com/aionemu/gameserver/model/team/common/events/PlayerLeavedEvent.java`
- `game-server/src/com/aionemu/gameserver/model/team/league/events/LeagueLeftEvent.java`
- `game-server/src/com/aionemu/gameserver/model/team/GeneralTeam.java`

## Tests Run

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerAllianceOfflineTimeoutDispatchServiceTests|FullyQualifiedName~PlayerAllianceRuntimeTests" --no-restore
```

- Result: passed, 34 tests.
- Java/Maven: not run because no Java source or fixtures changed and no narrow Java test exists for this dispatcher slice.
- Broad .NET: skipped; no broad-validation trigger applied after the focused command compiled and passed.
- Hygiene: `git diff --check` passed with line-ending normalization warnings only.

## What Changed

- Added `PlayerAllianceOfflineTimeoutDispatchService`.
- `DispatchNextExpiredAsync(...)` now consumes the runtime timeout hook and sends packets through `IGameClientConnectionRegistry`.
- `PlayerAllianceOfflineTimeoutPlan` now carries `LeagueId` captured before the timeout mutation.
- In-league timeout disband sends Java-ordered timeout fanout, league-left/disperse packets, alliance disband packets, and base leave packets.
- The dispatcher explicitly skips ordinary `League.broadcast(team)` for `LEAVE_TIMEOUT`, matching Java.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.OfflinePlayerAllianceChecker` | `Aion.GameServer.Services.PlayerAllianceOfflineTimeoutDispatchService`; `Aion.GameServer.Services.PlayerAllianceRuntime` | Scheduler/Event Source | Partial | Regression Tested | Partial Parity | Single-step dispatch exists. Hosted fixed-rate scheduler remains open. |
| `com.aionemu.gameserver.model.team.alliance.events.PlayerAllianceLeavedEvent` | `Aion.GameServer.Services.PlayerAllianceOfflineTimeoutDispatchService`; `Aion.GameServer.Services.PlayerAllianceLeaveWorkflowPlanner` | Event/Dispatch | Partial | Regression Tested | Partial Parity | Timeout fanout and in-league disband ordering are covered. |
| `com.aionemu.gameserver.model.team.league.events.LeagueLeftEvent` | `Aion.GameServer.Services.PlayerLeagueRuntime`; `Aion.GameServer.Services.PlayerAllianceOfflineTimeoutDispatchService` | Event/Dispatch | Partial | Regression Tested | Partial Parity | League-left before alliance disband is covered for timeout disband. |
| `com.aionemu.gameserver.model.team.common.events.PlayerLeavedEvent` | `Aion.GameServer.Services.PlayerBaseLeavePlanner`; `Aion.GameServer.Services.PlayerAllianceOfflineTimeoutDispatchService` | Event/Side Effect | Partial | Regression Tested | Partial Parity | Base leave follows alliance leave; offline timed-out members emit no base packet. |

## Known Gaps

- No hosted fixed-rate scheduler invokes `DispatchNextExpiredAsync(...)` yet.
- Offence `VortexService.removeInvaderPlayer` behavior is not executed.
- Startup composition does not register the dispatcher or scheduler.
- Multi-expired-member scan behavior needs either a scheduler loop or executor test.
- In-league leader leave with no online fallback remains risky.

## Next Recommended UOW

UOW-2441: Add a narrow hosted/executor bridge for alliance offline timeouts, or produce a focused composition audit if scheduler startup is not safe yet.

Suggested Java artifacts:

- `game-server/src/com/aionemu/gameserver/model/team/alliance/PlayerAllianceService.java`
- `game-server/src/com/aionemu/gameserver/configs/main/GroupConfig.java`
- `game-server/src/com/aionemu/gameserver/services/VortexService.java`
- `game-server/src/com/aionemu/gameserver/GameServer.java`

Suggested C# artifacts:

- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceOfflineTimeoutDispatchService.cs`
- `dotnetConversion/src/Aion.GameServer/Configuration/GameServerOptions.cs`
- `dotnetConversion/src/Aion.GameServer/GameServerServiceCollectionExtensions.cs` or current game-server composition file
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerAllianceOfflineTimeoutDispatchServiceTests.cs`
- Add a new scheduler/executor test only if the hook can be introduced without broad startup churn.

Suggested focused validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerAllianceOfflineTimeoutDispatchServiceTests|FullyQualifiedName~GameServerOptionsTests" --no-restore
```

Specific behavior to prove: scheduler/executor uses Java `GroupConfig.ALLIANCE_REMOVE_TIME`, loops the single-step dispatcher for expired members, and does not run broad hosted startup unless a composition trigger is explicitly named. Java/Maven is not expected unless Java source or fixtures change. Broad-validation trigger: none for an executor-only service; startup/hosted-service composition may trigger broader validation if real game-server startup wiring changes.

## Safe Alternate Candidates

- Add offence-timeout side-effect planning/execution around the C# equivalent of `VortexService.removeInvaderPlayer`.
- Add multi-expired-member loop tests at dispatcher level without hosted scheduler registration.
- Audit game-server composition for where scheduler services are currently registered.
