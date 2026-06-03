# Phase 6 Session 2442 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2442: Add alliance offline-timeout scheduler wrapper

## Commits Made

- `[Phase 6][UOW-2442] Add alliance timeout scheduler wrapper`

## Summary

UOW-2442 added an unregistered `PlayerAllianceOfflineTimeoutScheduler` that wraps the UOW-2441 alliance timeout scan executor. It uses the Java fixed-rate cadence from `PlayerAllianceService.initializeOfflineCheck` and passes `GameServerOptions.Group.AllianceRemoveTimeSeconds` into the scan tick. Startup/DI registration was deliberately not added in this slice.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceOfflineTimeoutScheduler.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerAllianceOfflineTimeoutSchedulerTests.cs`
- `docs/Phase-6-Session-2442-Completion.md`
- `docs/Phase-6-Session-2442-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService`
- `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.OfflinePlayerAllianceChecker`
- `com.aionemu.gameserver.configs.main.GroupConfig`

## C# Artifacts Touched

- `Aion.GameServer.Services.PlayerAllianceOfflineTimeoutScheduler`
- `Aion.GameServer.Services.PlayerAllianceOfflineTimeoutDispatchService` (adjacent validation only)
- `Aion.GameServer.Configuration.GameServerGroupOptions` (consumed by scheduler)

## Validation Completed

Validation target: Java alliance offline checker cadence (`1000`, `30 * 1000`) and configured `GroupConfig.ALLIANCE_REMOVE_TIME` use during a scheduler scan tick.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerAllianceOfflineTimeoutSchedulerTests|FullyQualifiedName~PlayerAllianceOfflineTimeoutDispatchServiceTests" --no-restore
```

- Passed: 5 tests.
- Existing nullable/analyzer warnings were emitted.

```powershell
git diff --check
```

- Passed. Staged hygiene was run before commit.

Java/Maven validation was skipped because no Java source or Java fixtures changed, and the Java source-of-truth behavior was direct source review of scheduler literals and timeout control flow. Broad .NET validation was skipped because no hosted startup registration, shared scheduler primitive, packet primitive, persistence, or live side-effect wiring changed.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.initializeOfflineCheck` | `Aion.GameServer.Services.PlayerAllianceOfflineTimeoutScheduler.Start` | Service scheduler | Partial | Unit Tested | Partial Parity | Fixed-rate delay and period match Java literals. Startup trigger after first alliance creation remains missing. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.OfflinePlayerAllianceChecker.run` | `Aion.GameServer.Services.PlayerAllianceOfflineTimeoutScheduler.RunScanOnceAsync` | Service executor | Partial | Regression Tested | Partial Parity | Uses configured alliance timeout and existing dispatcher loop. Offence-invader side effect still not executed. |
| `com.aionemu.gameserver.configs.main.GroupConfig.ALLIANCE_REMOVE_TIME` | `Aion.GameServer.Configuration.GameServerGroupOptions.AllianceRemoveTimeSeconds` | Config | Partial | Regression Tested | Partial Parity | Consumed by the scheduler wrapper; existing config tests cover property defaults/overrides. |

## Known Gaps

- Scheduler is not registered in `Program.cs` or any startup composition.
- Java `offlineCheckStarted.compareAndSet(false, true)` trigger on first alliance creation has no C# equivalent yet.
- `VortexService.removeInvaderPlayer` is still not invoked for offence timeout removals.
- Group offline timeout checker remains separate and not yet ported to an executor/scheduler pair.

## Remaining Risks

- Registering the scheduler in startup may run scans earlier or more often than Java unless the first-alliance-created trigger is preserved.
- Offence/defence Vortex side effects need a focused C# service equivalent before timeout parity can be claimed beyond packet/team-state effects.
- Threading parity is partial: the wrapper uses the existing C# `ThreadPoolManager`, but lifecycle trigger semantics are not complete.

## Next Recommended UOW

[Phase 6] UOW-2443: Add alliance offline-timeout startup trigger guard or offence-timeout Vortex side-effect executor

The safest next sequential task is to port the Java `offlineCheckStarted.compareAndSet(false, true)` semantics as a narrow trigger/guard around the scheduler. Do this without adding broad hosted startup registration unless the UOW explicitly scopes composition validation.

Alternative safe candidate: inspect C# Vortex/Rift service equivalents and add a narrow offence-invader side-effect executor for `WouldRemoveOffenceInvader`.

## Suggested Files To Inspect

- `game-server/src/com/aionemu/gameserver/model/team/alliance/PlayerAllianceService.java`
- `game-server/src/com/aionemu/gameserver/services/VortexService.java`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceRuntime.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceOfflineTimeoutScheduler.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceOfflineTimeoutDispatchService.cs`
- `dotnetConversion/src/Aion.GameServer/Program.cs` only if choosing a composition UOW
- Any C# Vortex/Rift service equivalents under `dotnetConversion/src/Aion.GameServer`

## Suggested Validation

For a trigger-guard UOW:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerAllianceRuntimeTests|FullyQualifiedName~PlayerAllianceOfflineTimeoutSchedulerTests" --no-restore
```

Behavior to prove: scheduler start is requested once when the first alliance is created, matching Java `offlineCheckStarted.compareAndSet(false, true)`, while repeated alliance creation does not schedule duplicate offline checks.

For an offence side-effect UOW:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerAllianceOfflineTimeoutDispatchServiceTests|FullyQualifiedName~Vortex" --no-restore
```

Narrow the `Vortex` portion to the actual located C# test class before running.

Java/Maven is not expected unless Java source or fixtures change. Broad-validation trigger is `none` for an unregistered trigger guard. Adding hosted startup registration or live side-effect wiring may create a broad-validation trigger and should be documented before any wider .NET validation.
