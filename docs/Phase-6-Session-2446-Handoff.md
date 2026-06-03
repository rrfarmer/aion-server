# Phase 6 Session 2446 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2446: Enable production alliance offline-timeout scheduler callback

## Commits Made

- `[Phase 6][UOW-2446] Enable alliance timeout scheduler graph`

## Summary

UOW-2446 enabled the production alliance offline-timeout scheduler graph. Java starts `OfflinePlayerAllianceChecker` once from `PlayerAllianceService.createAlliance` using `offlineCheckStarted.compareAndSet(false, true)`. C# now mirrors that live composition by adding a named `AddFindGroupSingletonGraphWithAllianceOfflineTimeoutScheduler` helper and using it from `Program.cs`. Focused tests prove the production helper schedules exactly one Java-cadence fixed-rate task after repeated alliance creation.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Program.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupServiceCollectionExtensions.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupServiceCollectionExtensionsTests.cs`
- `docs/Phase-6-Session-2446-Completion.md`
- `docs/Phase-6-Session-2446-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService`
- `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.createAlliance`
- `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.initializeOfflineCheck`
- `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.OfflinePlayerAllianceChecker`
- `com.aionemu.gameserver.utils.ThreadPoolManager`

## C# Artifacts Touched

- `Aion.GameServer.Program`
- `Aion.GameServer.Services.FindGroupServiceCollectionExtensions`
- `Aion.GameServer.Services.PlayerAllianceRuntime` (composition target)
- `Aion.GameServer.Services.PlayerAllianceOfflineTimeoutScheduler` (composition target)
- `Aion.GameServer.Services.PlayerAllianceOfflineTimeoutDispatchService` (scheduler dependency)
- `Aion.GameServer.Utils.ThreadPoolManager` (scheduler owner)

## Validation Completed

Validation target: production game-server composition enables Java-like alliance offline timeout scheduling, and first successful alliance creation starts one fixed-rate checker with Java delay/period.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupServiceCollectionExtensionsTests|FullyQualifiedName~GameClientSocketServerSmokeTests|FullyQualifiedName~PlayerAllianceOfflineTimeoutSchedulerTests|FullyQualifiedName~PlayerAllianceRuntimeTests|FullyQualifiedName~PlayerAllianceOfflineTimeoutDispatchServiceTests" --no-restore
```

- Passed: 48 tests.
- Existing nullable/analyzer warnings were emitted.

```powershell
git diff --check
```

- Passed with expected LF-to-CRLF working-tree warnings.

Java/Maven validation was skipped because no Java source or Java fixtures changed, and the Java source-of-truth behavior was direct source review. Broad-validation trigger was present because live scheduler composition was enabled; full project/solution validation was skipped after focused validation passed because the live change was isolated to the scheduler-enabled graph and did not alter packet primitives, persistence, crypto, serialization helpers, database schema, or broad world mutation APIs.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.createAlliance` | `Aion.GameServer.Services.PlayerAllianceRuntime.CreateAlliance` through production graph | Service/runtime composition | Partial | Unit Tested | Partial Parity | Production graph now connects the one-shot create-alliance trigger to scheduler start. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.initializeOfflineCheck` | `Aion.GameServer.Services.PlayerAllianceOfflineTimeoutScheduler.Start` via `AddFindGroupSingletonGraphWithAllianceOfflineTimeoutScheduler` | Service scheduler | Partial | Unit Tested | Partial Parity | Java cadence of 1 second initial delay and 30 second period is composition-tested through the production helper. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.OfflinePlayerAllianceChecker` | `Aion.GameServer.Services.PlayerAllianceOfflineTimeoutScheduler` and `PlayerAllianceOfflineTimeoutDispatchService` | Scheduled service/dispatcher | Partial | Unit Tested | Partial Parity | Scan and dispatch behavior are tested, but offence Vortex side effect is still a result flag rather than live execution. |
| `com.aionemu.gameserver.utils.ThreadPoolManager.scheduleAtFixedRate` | `Aion.GameServer.Utils.ThreadPoolManager.ScheduleAtFixedRateTask` | Scheduler utility | Partial | Adjacent Unit Tested | Partial Parity | Existing scheduler owns the live task and is disposed through the registered singleton lifetime; scheduler internals were not changed. |

## Known Gaps

- `VortexService.removeInvaderPlayer` is still not invoked for offence timeout removals.
- Group offline timeout checker remains separate and not yet ported to matching trigger/scheduler wiring.
- No real-host lifecycle smoke was added for scheduler startup/shutdown in this UOW.
- Full alliance timeout parity remains partial until Vortex offence/defence side effects and any parallel group checker are resolved.

## Remaining Risks

- The scheduler is now enabled on the production graph, so any future timeout-dispatch side effects should consider live socket registry, alliance runtime, league runtime, and scheduler lifetime together.
- Offence Vortex cleanup still blocks fuller parity for `OfflinePlayerAllianceChecker.run`.
- Group timeout parity likely needs an audit of Java `PlayerGroupService` or equivalent group offline checker code.

## Next Recommended UOW

[Phase 6] UOW-2447: Port alliance timeout offence Vortex side-effect execution

The next sequential task is to inspect Java `VortexService.removeInvaderPlayer` and C# Vortex/Rift equivalents, then convert the current `WouldRemoveOffenceInvader` result flag into an executed side effect where the required C# service exists. Keep the scope narrow: offence timeout side effect only, leaving group timeout scheduling for a later UOW unless the same Java source path proves they must be coupled.

Alternative safe candidate: audit Java group offline timeout scheduling and decide whether it has a matching C# scheduler/runtime gap.

## Suggested Files To Inspect

- `game-server/src/com/aionemu/gameserver/model/team/alliance/PlayerAllianceService.java`
- `game-server/src/com/aionemu/gameserver/services/VortexService.java`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceOfflineTimeoutDispatchService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexLocationService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/RiftService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/RiftManagerService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupServiceCollectionExtensions.cs`
- `dotnetConversion/src/Aion.GameServer/Program.cs`

## Suggested Validation

For offence Vortex side-effect execution:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerAllianceOfflineTimeoutDispatchServiceTests|FullyQualifiedName~Vortex|FullyQualifiedName~Rift" --no-restore
```

Narrow `Vortex|Rift` to the actual located test classes before running if the search finds many unrelated tests.

Java/Maven is not expected unless a targeted Java Vortex fixture exists or Java fixtures change. Broad-validation trigger is `none` if the UOW only adds a planner/adapter side-effect seam; a trigger is present if live Vortex/Rift state mutation is enabled in production dispatch.
