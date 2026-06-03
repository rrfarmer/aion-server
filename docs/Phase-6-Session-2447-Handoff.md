# Phase 6 Session 2447 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2447: Port alliance timeout offence Vortex side-effect execution

## Commits Made

- `[Phase 6][UOW-2447] Execute offence Vortex timeout cleanup`

## Summary

UOW-2447 converted the alliance timeout offence Vortex behavior from a result flag into an executed side effect. Java `OfflinePlayerAllianceChecker.run` calls `VortexService.removeInvaderPlayer(member.getObject())` before firing the alliance leave timeout. C# now carries the timed-out `Player` in the timeout plan, dispatches through a new `VortexInvasionRuntime`, and clears active invader plus passed-player portal state for offence alliance timeouts.

The implementation intentionally does not claim full live Vortex kick parity. Java `Invasion.kickPlayer` also handles online packet messages and teleporting online invaders out of the invasion world, but the offline timeout checker only selects offline members. Those online behaviors remain a later Vortex runtime UOW.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Program.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceInfoPlan.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceRuntime.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceOfflineTimeoutDispatchService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexInvasionRuntime.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerAllianceOfflineTimeoutDispatchServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerAllianceRuntimeTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`
- `docs/Phase-6-Session-2447-Completion.md`
- `docs/Phase-6-Session-2447-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService`
- `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.OfflinePlayerAllianceChecker`
- `com.aionemu.gameserver.services.VortexService`
- `com.aionemu.gameserver.services.vortex.Invasion`
- `com.aionemu.gameserver.services.vortex.DimensionalVortex`
- `com.aionemu.gameserver.model.vortex.VortexLocation`

## C# Artifacts Touched

- `Aion.GameServer.Program`
- `Aion.GameServer.Services.PlayerAllianceRuntime`
- `Aion.GameServer.Services.PlayerAllianceOfflineTimeoutDispatchService`
- `Aion.GameServer.Services.VortexInvasionRuntime`
- `Aion.GameServer.Dataholders.VortexLocationSummary`

## Validation Completed

Validation target: offence alliance offline timeout dispatch executes Java-like Vortex active-invader cleanup and preserves adjacent Vortex/Rift behavior.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerAllianceOfflineTimeoutDispatchServiceTests|FullyQualifiedName~PlayerAllianceRuntimeTests|FullyQualifiedName~Vortex|FullyQualifiedName~Rift" --no-restore
```

- Passed: 109 tests.
- Existing nullable/analyzer warnings were emitted.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupServiceCollectionExtensionsTests" --no-restore
```

- Passed: 5 tests.

```powershell
git diff --check
```

- Passed with expected LF-to-CRLF working-tree warnings.

Java/Maven validation was skipped because no Java source or Java fixtures changed. Broad-validation trigger was present because production dispatch now mutates Vortex runtime state; full project/solution validation was skipped after focused validation because the mutation is isolated to timeout dispatch and covered by alliance runtime, dispatcher, Vortex/Rift, and DI composition tests.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.VortexService.removeInvaderPlayer` | `Aion.GameServer.Services.VortexInvasionRuntime.RemoveInvaderPlayer` | Service/runtime side effect | Partial | Unit Tested | Partial Parity | Active invader and passed-player cleanup are ported for timeout dispatch; online packet/teleport behavior is deferred. |
| `com.aionemu.gameserver.services.vortex.Invasion.kickPlayer` | `Aion.GameServer.Services.VortexInvasionRuntime.RemoveInvaderPlayer` | Vortex participant mutation | Partial | Unit Tested | Partial Parity | Removes invader and passed-player state; does not yet own Vortex alliance disband, defender side, zone sync, or teleport. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.OfflinePlayerAllianceChecker` | `Aion.GameServer.Services.PlayerAllianceOfflineTimeoutDispatchService` | Scheduled timeout dispatcher | Partial | Unit Tested | Partial Parity | Offence timeout now executes Vortex cleanup before dispatching leave packets. |

## Known Gaps

- Online Vortex kick behavior remains unported: Java sends Vortex kick/direct portal-out system messages and teleports online invaders home.
- Defender Vortex removal remains unported.
- Full Vortex controller sync and spawned active-invasion lifecycle are not yet represented beyond the in-memory participant runtime.
- Group offline timeout scheduling remains a likely parallel gap.

## Remaining Risks

- `VortexInvasionRuntime` is production-registered, but no production code currently populates it from a full spawned Vortex lifecycle. Tests seed it directly.
- Future Vortex spawning work must connect active invasion creation, invader joins, defender joins, passed-player tracking, controller sync, and teardown to this runtime or replace it with a fuller port.
- Alliance timeout parity remains partial until live Vortex lifecycle and group timeout parity are addressed.

## Next Recommended UOW

[Phase 6] UOW-2448: Audit and port group offline timeout scheduler/runtime parity

The next sequential task is to inspect Java group offline timeout behavior, especially any checker parallel to `PlayerAllianceService.OfflinePlayerAllianceChecker`, and compare it against current C# group runtime/scheduler coverage. Keep the UOW scoped to discovery plus the smallest scheduler/runtime hook needed for group timeout parity.

Alternative safe candidate: continue Vortex runtime parity by porting online `Invasion.kickPlayer` invader/defender packet and teleport behavior, but only after identifying the C# active Vortex lifecycle entry point that should populate `VortexInvasionRuntime`.

## Suggested Files To Inspect

- `game-server/src/com/aionemu/gameserver/model/team/group/PlayerGroupService.java`
- `game-server/src/com/aionemu/gameserver/model/team/group/PlayerGroup.java`
- `game-server/src/com/aionemu/gameserver/model/team/alliance/PlayerAllianceService.java`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerGroupRuntime.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceRuntime.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupServiceCollectionExtensions.cs`
- `dotnetConversion/src/Aion.GameServer/Program.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerGroupRuntimeTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupServiceCollectionExtensionsTests.cs`

## Suggested Validation

For group offline timeout parity:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerGroup|FullyQualifiedName~FindGroupServiceCollectionExtensionsTests|FullyQualifiedName~PlayerAllianceOfflineTimeout" --no-restore
```

Narrow the filter after discovery if the exact group timeout test classes are clearer.

Java/Maven is not expected unless Java fixtures change. Broad-validation trigger is present if production scheduling or live group state mutation is enabled; otherwise no broad trigger for pure discovery/planner work.
