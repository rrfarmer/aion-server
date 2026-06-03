# Phase 6 Session 2444 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2444: Add alliance offline-timeout scheduler composition readiness

## Commits Made

- `[Phase 6][UOW-2444] Add alliance timeout composition readiness`

## Summary

UOW-2444 added opt-in composition readiness for the alliance offline-timeout scheduler. `AddFindGroupSingletonGraph` now registers `PlayerAllianceOfflineTimeoutDispatchService`, `PlayerAllianceOfflineTimeoutScheduler`, and a shared `PlayerLeagueRuntime`, and it can optionally construct `PlayerAllianceRuntime` with a scheduler-start callback. Focused tests prove the opt-in shared graph schedules exactly one Java-cadence fixed-rate task after repeated alliance creation. Production `Program.cs` still uses the default non-scheduling path.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupServiceCollectionExtensions.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupServiceCollectionExtensionsTests.cs`
- `docs/Phase-6-Session-2444-Completion.md`
- `docs/Phase-6-Session-2444-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService`
- `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.createAlliance`
- `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.initializeOfflineCheck`
- `com.aionemu.gameserver.utils.ThreadPoolManager`

## C# Artifacts Touched

- `Aion.GameServer.Services.FindGroupServiceCollectionExtensions`
- `Aion.GameServer.Services.PlayerAllianceRuntime` (composition target)
- `Aion.GameServer.Services.PlayerAllianceOfflineTimeoutScheduler` (composition target)
- `Aion.GameServer.Services.PlayerAllianceOfflineTimeoutDispatchService` (registered dependency)
- `Aion.GameServer.Services.PlayerLeagueRuntime` (registered dependency)

## Validation Completed

Validation target: opt-in shared service graph can construct `PlayerAllianceRuntime` with a scheduler-start callback, and creating the first alliance schedules exactly one offline timeout check through the Java-like scheduler cadence.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupServiceCollectionExtensionsTests|FullyQualifiedName~PlayerAllianceOfflineTimeoutSchedulerTests|FullyQualifiedName~PlayerAllianceRuntimeTests" --no-restore
```

- Passed: 41 tests.
- Existing nullable/analyzer warnings were emitted.

```powershell
git diff --check
```

- Passed with expected LF-to-CRLF working-tree warnings.

Java/Maven validation was skipped because no Java source or Java fixtures changed, and the Java source-of-truth behavior was direct source review of `createAlliance`, `initializeOfflineCheck`, and `ThreadPoolManager.scheduleAtFixedRate`. Broad .NET validation was skipped because the production startup path was not changed and no live hosted registration was added.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.createAlliance` | `Aion.GameServer.Services.PlayerAllianceRuntime.CreateAlliance` via opt-in service graph | Service/runtime composition | Partial | Unit Tested | Partial Parity | Opt-in composition can connect the one-shot creation trigger to scheduler start. Production default path remains non-scheduling. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.initializeOfflineCheck` | `Aion.GameServer.Services.PlayerAllianceOfflineTimeoutScheduler.Start` | Service scheduler | Partial | Unit Tested | Partial Parity | Java cadence is observed through the opt-in graph. Hosted/lifecycle registration is still absent. |
| `com.aionemu.gameserver.utils.ThreadPoolManager.scheduleAtFixedRate` | `Aion.GameServer.Utils.ThreadPoolManager.ScheduleAtFixedRateTask` | Scheduler utility | Partial | Adjacent Unit Tested | Partial Parity | Existing scheduler was exercised by composition test; scheduler internals were not changed. |

## Known Gaps

- Production `Program.cs` still does not enable the alliance offline-timeout scheduler callback.
- No explicit production lifecycle owner stores/cancels the scheduled task; current readiness relies on `ThreadPoolManager` shutdown.
- Shared `PlayerLeagueRuntime` registration exists for the scheduler graph, but socket/connection league runtime sharing remains a broader composition gap.
- `VortexService.removeInvaderPlayer` is still not invoked for offence timeout removals.
- Group offline timeout checker remains separate and not yet ported to matching trigger/scheduler wiring.

## Remaining Risks

- Enabling the callback in production will cross runtime state, scheduler, connection registry, and timeout dispatch. Treat that as live composition and document the broad-validation trigger before wider checks.
- In-league timeout dispatch may still diverge until league runtime sharing is audited across `GameClientSocketServer` and `GameServerConnection`.
- Offence/defence Vortex side effects still block fuller alliance timeout parity.

## Next Recommended UOW

[Phase 6] UOW-2445: Audit and wire shared `PlayerLeagueRuntime` through game socket composition, or port the offence-timeout Vortex side-effect executor

The next sequential task is to inspect whether `GameClientSocketServer` should own and pass the shared `PlayerLeagueRuntime` into each `GameServerConnection`. This matters before enabling live alliance timeout scheduling because in-league timeout dispatch depends on league state.

Alternative safe candidate: inspect Java `VortexService` and C# Vortex/Rift equivalents, then execute or plan the `WouldRemoveOffenceInvader` side effect in `PlayerAllianceOfflineTimeoutDispatchService`.

## Suggested Files To Inspect

- `game-server/src/com/aionemu/gameserver/model/team/league/LeagueService.java` if present, or league event/service classes under `game-server/src/com/aionemu/gameserver/model/team/league`
- `game-server/src/com/aionemu/gameserver/model/team/alliance/PlayerAllianceService.java`
- `game-server/src/com/aionemu/gameserver/services/VortexService.java`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientSocketServer.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupServiceCollectionExtensions.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerLeagueRuntime.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceOfflineTimeoutDispatchService.cs`

## Suggested Validation

For a shared league runtime composition UOW:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupServiceCollectionExtensionsTests|FullyQualifiedName~GameServerConnectionPlayerStatusInfoTests|FullyQualifiedName~PlayerAllianceOfflineTimeoutDispatchServiceTests" --no-restore
```

Narrow `GameServerConnectionPlayerStatusInfoTests` to the specific shared-league-runtime tests if a more focused test name is added.

Behavior to prove: socket-created `GameServerConnection` instances receive the same shared `PlayerLeagueRuntime` that the alliance timeout dispatcher uses.

For an offence side-effect UOW:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerAllianceOfflineTimeoutDispatchServiceTests|FullyQualifiedName~Vortex" --no-restore
```

Narrow the `Vortex` portion to the actual located C# test class before running.

Java/Maven is not expected unless Java source or fixtures change. Broad-validation trigger is `none` for a non-live composition audit. Enabling production scheduler callback or live Vortex side-effect execution may create a broad-validation trigger and should be documented before any wider .NET validation.
