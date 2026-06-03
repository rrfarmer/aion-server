# Phase 6 Session 2448 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2448: Port group offline timeout scheduler/runtime parity

## Commits Made

- `[Phase 6][UOW-2448] Enable group offline timeout scheduler`

## Summary

UOW-2448 ported the Java group offline timeout checker path. Java `PlayerGroupService.createGroup` starts one `OfflinePlayerChecker` via `offlineCheckStarted.compareAndSet(false, true)`, scheduled at 1 second initial delay and 30 second period. The checker uses `GroupConfig.GROUP_REMOVE_TIME` and fires `PlayerGroupLeavedEvent(LEAVE_TIMEOUT)` for expired offline members.

C# now has `PlayerGroupOfflineTimeoutScheduler`, `PlayerGroupOfflineTimeoutDispatchService`, runtime timeout selection, and production DI wiring through `AddFindGroupSingletonGraphWithOfflineTimeoutSchedulers`. `Program.cs` uses the combined group+alliance scheduler graph. The implementation also fixed a timeout-specific group leader branch: Java does not run `ChangeGroupLeaderEvent` for `LEAVE_TIMEOUT`, so C# no longer creates a leader-change plan for timeout removals.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Program.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupServiceCollectionExtensions.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerGroupRuntime.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerGroupReconnectPlan.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerGroupOfflineTimeoutDispatchService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerGroupOfflineTimeoutScheduler.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupServiceCollectionExtensionsTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerGroupRuntimeTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerGroupOfflineTimeoutDispatchServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerGroupOfflineTimeoutSchedulerTests.cs`
- `docs/Phase-6-Session-2448-Completion.md`
- `docs/Phase-6-Session-2448-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.model.team.group.PlayerGroupService`
- `com.aionemu.gameserver.model.team.group.PlayerGroupService.createGroup`
- `com.aionemu.gameserver.model.team.group.PlayerGroupService.initializeOfflineCheck`
- `com.aionemu.gameserver.model.team.group.PlayerGroupService.OfflinePlayerChecker`
- `com.aionemu.gameserver.model.team.group.events.PlayerGroupLeavedEvent`
- `com.aionemu.gameserver.model.team.GeneralTeam`
- `com.aionemu.gameserver.configs.main.GroupConfig`
- `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService` (adjacent scheduler composition)

## C# Artifacts Touched

- `Aion.GameServer.Program`
- `Aion.GameServer.Services.FindGroupServiceCollectionExtensions`
- `Aion.GameServer.Services.PlayerGroupRuntime`
- `Aion.GameServer.Services.PlayerGroupOfflineTimeoutDispatchService`
- `Aion.GameServer.Services.PlayerGroupOfflineTimeoutScheduler`
- `Aion.GameServer.Services.PlayerGroupOfflineTimeoutPlan`
- `Aion.GameServer.Services.PlayerAllianceOfflineTimeoutScheduler` (adjacent composition target)

## Validation Completed

Validation target: Java-like group offline checker scheduling, configured removal delay, timeout leave dispatch, and production graph one-shot scheduling for group plus alliance.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerGroupOfflineTimeoutDispatchServiceTests|FullyQualifiedName~PlayerGroupOfflineTimeoutSchedulerTests|FullyQualifiedName~PlayerGroupRuntimeTests|FullyQualifiedName~FindGroupServiceCollectionExtensionsTests|FullyQualifiedName~PlayerAllianceOfflineTimeout" --no-restore
```

- Passed: 66 tests.
- Existing nullable/analyzer warnings were emitted.

```powershell
git diff --check
```

- Passed with expected LF-to-CRLF working-tree warnings.

Java/Maven validation was skipped because no Java source or Java fixtures changed and no narrow Java test fixture exists for this checker. Broad-validation trigger was present because production scheduling and live group timeout mutation were enabled; full project/solution validation was skipped after focused validation because the edited behavior is isolated to group timeout runtime/dispatch/scheduler composition and adjacent alliance timeout tests.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.model.team.group.PlayerGroupService.createGroup` | `Aion.GameServer.Services.PlayerGroupRuntime.CreateOrUpdateGroup` through production graph | Service/runtime composition | Partial | Unit Tested | Partial Parity | C# now starts one group offline timeout checker after first successful group creation; method still also serves update-style tests unlike Java's create-only API. |
| `com.aionemu.gameserver.model.team.group.PlayerGroupService.initializeOfflineCheck` | `Aion.GameServer.Services.PlayerGroupOfflineTimeoutScheduler.Start` | Service scheduler | Partial | Unit Tested | Partial Parity | Java cadence of 1 second initial delay and 30 second period is covered. |
| `com.aionemu.gameserver.model.team.group.PlayerGroupService.OfflinePlayerChecker` | `Aion.GameServer.Services.PlayerGroupOfflineTimeoutDispatchService` and `PlayerGroupRuntime.RemoveNextExpiredOfflineMemberWithLeavePlan` | Scheduled dispatcher/runtime | Partial | Unit Tested | Partial Parity | Expired offline members are removed with configured `GROUP_REMOVE_TIME`; multi-group Java `ConcurrentHashMap` ordering is not asserted. |
| `com.aionemu.gameserver.model.team.group.events.PlayerGroupLeavedEvent` | `Aion.GameServer.Services.PlayerGroupRuntime.RemoveMemberWithLeavePlan` | Team leave event | Partial | Unit Tested | Partial Parity | Timeout fanout and no-timeout-leader-change branch are covered; full mentor/base-event fanout remains partial. |
| `com.aionemu.gameserver.configs.main.GroupConfig` | `Aion.GameServer.Configuration.GameServerGroupOptions` | Config | Partial | Unit Tested | Partial Parity | `GROUP_REMOVE_TIME` and `ALLIANCE_REMOVE_TIME` are loaded and consumed by schedulers; broader config class not fully audited in this UOW. |

## Known Gaps

- No real-host lifecycle smoke for group/alliance scheduler startup/shutdown.
- Group timeout multi-group ordering is not asserted because Java uses `ConcurrentHashMap`.
- Full group leave parity still has remaining mentor/base-event/world side-effect gaps outside the timeout slice.
- Vortex online kick packet/teleport behavior remains unported.
- `VortexInvasionRuntime` is production-registered but not populated from a full active Vortex lifecycle.

## Remaining Risks

- Production now schedules both group and alliance offline timeout checkers after their first create calls. Future socket/runtime changes should consider both schedulers together.
- The C# group create method is named `CreateOrUpdateGroup` and is broader than Java `createGroup`; the one-shot callback prevents repeated scheduling, but future tests that replace group state may still exercise create-like behavior.
- Timeout removal can expose Java's stale leader reference behavior after leader timeout; this is intentionally preserved for the timeout branch but needs real lifecycle coverage later.

## Next Recommended UOW

[Phase 6] UOW-2449: Audit Vortex active lifecycle population and online kick parity

The next sequential task is to inspect Java Vortex active lifecycle entry points (`DimensionalVortex`, `Invasion`, controller passed-player sync, rift portal use) and compare them against C# `VortexInvasionRuntime`, `VortexLocationService`, `RiftService`, and portal use services. Decide whether the smallest safe next step is wiring active invasion population into the runtime or porting online `Invasion.kickPlayer` packet/teleport behavior behind the existing runtime.

Alternative safe candidate: audit group leave mentor/base-event side effects around `PlayerGroupLeavedEvent` and compare against `PlayerGroupRuntime.RemoveMemberWithLeavePlan`, but prefer Vortex because it is the largest known gap from UOW-2447.

## Suggested Files To Inspect

- `game-server/src/com/aionemu/gameserver/services/VortexService.java`
- `game-server/src/com/aionemu/gameserver/services/vortex/Invasion.java`
- `game-server/src/com/aionemu/gameserver/services/vortex/DimensionalVortex.java`
- `game-server/src/com/aionemu/gameserver/model/vortex/VortexLocation.java`
- `game-server/src/com/aionemu/gameserver/controllers/RVController.java`
- `dotnetConversion/src/Aion.GameServer/Services/VortexInvasionRuntime.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexLocationService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/RiftService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/RiftPortalUseService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/RiftPortalUseServiceTests.cs`

## Suggested Validation

For Vortex lifecycle or online kick parity:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~Vortex|FullyQualifiedName~RiftPortalUseServiceTests|FullyQualifiedName~RiftServiceTests|FullyQualifiedName~PlayerAllianceOfflineTimeoutDispatchServiceTests" --no-restore
```

Narrow to the edited Vortex/Rift test class after discovery. Java/Maven is not expected unless Java fixtures change or a narrow Vortex Java fixture exists. Broad-validation trigger is present if production active Vortex lifecycle mutation, packet dispatch, or teleport side effects are enabled; otherwise none for a pure audit/planner UOW.
