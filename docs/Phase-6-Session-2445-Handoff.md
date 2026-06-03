# Phase 6 Session 2445 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2445: Wire shared league runtime through game socket composition

## Commits Made

- `[Phase 6][UOW-2445] Wire shared league runtime through sockets`

## Summary

UOW-2445 closed the shared league runtime composition gap identified by UOW-2444. Java keeps league state in `LeagueService` static shared maps; C# now mirrors that ownership for socket-created game connections by adding a socket-owned `PlayerLeagueRuntime` and passing it into each accepted `GameServerConnection`. The find-group DI graph test verifies the socket receives the shared service-provider instance, and a socket smoke test verifies the accepted connection receives that same runtime.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientSocketServer.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupServiceCollectionExtensionsTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameClientSocketServerSmokeTests.cs`
- `docs/Phase-6-Session-2445-Completion.md`
- `docs/Phase-6-Session-2445-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.model.team.league.LeagueService`
- `com.aionemu.gameserver.model.team.league.League`
- `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameClientSocketServer`
- `Aion.GameServer.Network.Aion.GameServerConnection` (composition target)
- `Aion.GameServer.Services.PlayerLeagueRuntime`
- `Aion.GameServer.Services.FindGroupServiceCollectionExtensions` (composition source)
- `Aion.GameServer.Services.PlayerAllianceOfflineTimeoutDispatchService` (adjacent dependency)

## Validation Completed

Validation target: socket and DI composition converge on one shared `PlayerLeagueRuntime`, and socket-created `GameServerConnection` instances receive that same runtime.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupServiceCollectionExtensionsTests|FullyQualifiedName~GameClientSocketServerSmokeTests|FullyQualifiedName~PlayerAllianceOfflineTimeoutDispatchServiceTests" --no-restore
```

- Passed: 12 tests.
- Existing nullable/analyzer warnings were emitted.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionPlayerStatusInfoTests" --no-restore
```

- Passed: 69 tests.
- An earlier parallel run of this command failed with `CS2012` because another test process held `Aion.GameServer.dll`; the sequential rerun passed.

```powershell
git diff --check
```

- Passed with expected LF-to-CRLF working-tree warnings.

Java/Maven validation was skipped because no Java source or Java fixtures changed. Broad .NET validation was skipped because this UOW did not enable production scheduler startup, change persistence, alter packet serialization, or broaden live world mutation.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.model.team.league.LeagueService.leagues` | `Aion.GameServer.Services.PlayerLeagueRuntime` through socket-created connections | Runtime state/composition | Partial | Unit/Socket Tested | Partial Parity | Accepted connections now receive the socket-owned shared runtime instead of constructing per-connection league state. |
| `com.aionemu.gameserver.model.team.league.LeagueService` | `Aion.GameServer.Services.FindGroupServiceCollectionExtensions` plus `GameClientSocketServer` | DI/service graph | Partial | Unit Tested | Partial Parity | The DI shared graph reaches the socket, aligning timeout dispatch dependencies with connection-side league mutations. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.removePlayer` | `Aion.GameServer.Services.PlayerAllianceOfflineTimeoutDispatchService` | Timeout dispatch dependency | Partial | Adjacent Unit Tested | Partial Parity | Dispatcher can now rely on the same league runtime instance used by socket-created alliance commands once the live scheduler is enabled. |

## Known Gaps

- Production `Program.cs` still does not enable the alliance offline-timeout scheduler callback.
- No explicit production lifecycle owner stores/cancels the scheduled alliance timeout task.
- `VortexService.removeInvaderPlayer` is still not invoked for offence timeout removals.
- Group offline timeout checker remains separate and not yet ported to matching trigger/scheduler wiring.

## Remaining Risks

- Enabling the scheduler callback in production will cross socket registry, alliance runtime, league runtime, scheduler lifecycle, and timeout dispatch. Treat that as live composition and document the validation scope carefully.
- Offence/defence Vortex side effects still block fuller alliance timeout parity.
- Group offline timeout parity likely needs its own executor, scheduler, and one-shot trigger audit.

## Next Recommended UOW

[Phase 6] UOW-2446: Enable production alliance offline-timeout scheduler callback, or port the offence-timeout Vortex side-effect executor

The now-unblocked sequential task is to inspect `Program.cs` startup composition and decide whether to wire `AddFindGroupSingletonGraph` with the scheduler-start callback in production. That UOW should verify lifecycle behavior, ensure `ThreadPoolManager` is registered on the live path, and use a broader focused validation set because it would enable live scheduling.

Alternative safe candidate: inspect Java `VortexService.removeInvaderPlayer` and the C# Vortex/Rift services, then execute or adapt the `WouldRemoveOffenceInvader` side effect from `PlayerAllianceOfflineTimeoutDispatchService`.

## Suggested Files To Inspect

- `game-server/src/com/aionemu/gameserver/model/team/alliance/PlayerAllianceService.java`
- `game-server/src/com/aionemu/gameserver/services/VortexService.java`
- `dotnetConversion/src/Aion.GameServer/Program.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupServiceCollectionExtensions.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceRuntime.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceOfflineTimeoutScheduler.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceOfflineTimeoutDispatchService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientSocketServer.cs`

## Suggested Validation

For production scheduler callback enablement:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupServiceCollectionExtensionsTests|FullyQualifiedName~GameClientSocketServerSmokeTests|FullyQualifiedName~PlayerAllianceOfflineTimeoutSchedulerTests|FullyQualifiedName~PlayerAllianceRuntimeTests|FullyQualifiedName~PlayerAllianceOfflineTimeoutDispatchServiceTests" --no-restore
```

Add or narrow `Program`/startup composition tests if the live registration path is changed.

For an offence side-effect UOW:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerAllianceOfflineTimeoutDispatchServiceTests|FullyQualifiedName~Vortex" --no-restore
```

Narrow the `Vortex` portion to the actual located C# test class before running.

Java/Maven is not expected unless Java source or fixtures change. Broad-validation trigger is likely present if production scheduler startup is enabled; no broad trigger is expected for a planner-only Vortex side-effect audit.
