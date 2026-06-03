# Phase 6 Session 2448 Completion

## UOW

[Phase 6] UOW-2448: Port group offline timeout scheduler/runtime parity

## Status

Completed and validated with focused .NET tests.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/model/team/group/PlayerGroupService.java`
- `game-server/src/com/aionemu/gameserver/model/team/group/events/PlayerGroupLeavedEvent.java`
- `game-server/src/com/aionemu/gameserver/model/team/GeneralTeam.java`
- `game-server/src/com/aionemu/gameserver/configs/main/GroupConfig.java`
- `game-server/src/com/aionemu/gameserver/model/team/alliance/PlayerAllianceService.java`

## C# Changes

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

## Implementation Notes

- Added `PlayerGroupOfflineTimeoutDispatchService` to drain expired offline group members through the existing Java-like `PlayerGroupLeavedEvent(LEAVE_TIMEOUT)` workflow.
- Added `PlayerGroupOfflineTimeoutScheduler` with Java cadence: 1 second initial delay and 30 second period.
- `PlayerGroupRuntime.CreateOrUpdateGroup` now accepts an optional one-shot offline checker callback, mirroring `PlayerGroupService.createGroup -> offlineCheckStarted.compareAndSet(false, true)`.
- Added `RemoveNextExpiredOfflineMemberWithLeavePlan`, using `GroupConfig.GROUP_REMOVE_TIME` equivalent seconds and preserving the timed-out `Player` reference.
- `Program.cs` now uses `AddFindGroupSingletonGraphWithOfflineTimeoutSchedulers`, enabling both group and alliance offline timeout checkers in production composition.
- Preserved the existing alliance-only helper for compatibility with older tests/callers.
- Fixed the group timeout leader branch to match Java: `PlayerGroupLeavedEvent` does not run `ChangeGroupLeaderEvent` for `LEAVE_TIMEOUT`; it only disbands when the remaining non-auto group size is one.

## Tests

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `RemoveNextExpiredOfflineMemberWithLeavePlan_UsesConfiguredGroupRemoveTimeLikeJavaChecker` | Unit | `PlayerGroupService.OfflinePlayerChecker`, `GroupConfig.GROUP_REMOVE_TIME` | Runtime selects expired offline members by configured delay and removes with `LeaveTimeout` | Timed-out member is removed, waiting member remains, and plan carries the exact player | Does not compare against Java runtime output |
| `RemoveNextExpiredOfflineMemberWithLeavePlan_DoesNotChangeLeaderForTimeoutLikeJavaEvent` | Unit | `PlayerGroupLeavedEvent.handleEvent` | Timeout leader removal does not run group leader-change branch | `LeaderChangePlan` is null and descriptor leader remains the removed leader id, matching Java's lack of timeout leader-change event | Java's stale leader reference behavior remains risky until full runtime lifecycle is exercised |
| `DispatchNextExpiredAsync_RemovesExpiredOfflineMemberAndSendsTimeoutFanoutLikeJavaChecker` | Unit/dispatch | `OfflinePlayerChecker.run`, `PlayerGroupLeavedEvent.handleEvent` | Dispatch executes timeout removal and sends member-info plus `STR_PARTY_HE_BECOME_OFFLINE_TIMEOUT` fanout | Captured packets target remaining members with message id `1300176` and timed-out name | Socket registry is fake |
| `DispatchExpiredScanAsync_DrainsExpiredMembersLikeJavaCheckerRun` | Unit/dispatch | `OfflinePlayerChecker.run` | Dispatcher loops until all expired offline members in the scan are removed | Two expired members are removed and waiting member remains | Group map ordering is not claimed deterministic across multiple groups |
| `Start_SchedulesJavaOfflineGroupCheckerCadenceWithoutStartupRegistration` | Unit/scheduler | `PlayerGroupService.initializeOfflineCheck` | Scheduler uses 1 second delay and 30 second period | ThreadPool schedule observation matches Java cadence | Does not let timer elapse naturally |
| `RunScanOnceAsync_UsesConfiguredGroupRemoveTimeLikeGroupConfig` | Unit/scheduler | `GroupConfig.GROUP_REMOVE_TIME` | Scheduler passes configured group remove time to dispatch | Only the configured-expired member is removed | Uses fixed time provider |
| `AddFindGroupSingletonGraphWithOfflineTimeoutSchedulers_StartsGroupAndAllianceOnceLikeJavaCreateCalls` | Unit/composition | `PlayerGroupService.createGroup`, `PlayerAllianceService.createAlliance` | Production graph starts group and alliance checkers once from create calls | Two fixed-rate schedule observations after repeated group creation plus alliance creation | Does not run full host |

## Validation Decision

- Changed surface: production scheduler composition plus live group timeout state mutation/packet dispatch.
- Specific behavior/contract: Java `PlayerGroupService.createGroup` starts one `OfflinePlayerChecker`, which uses `GroupConfig.GROUP_REMOVE_TIME` to remove expired offline members with `PlayerGroupLeavedEvent(LEAVE_TIMEOUT)`.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerGroupOfflineTimeoutDispatchServiceTests|FullyQualifiedName~PlayerGroupOfflineTimeoutSchedulerTests|FullyQualifiedName~PlayerGroupRuntimeTests|FullyQualifiedName~FindGroupServiceCollectionExtensionsTests|FullyQualifiedName~PlayerAllianceOfflineTimeout" --no-restore
```

- Result: Passed, 66 tests. Existing nullable/analyzer warnings were emitted.
- Focused Java/Maven command: skipped; no Java source or Java fixtures changed, and the Java source-of-truth behavior was direct review of `PlayerGroupService`, `PlayerGroupLeavedEvent`, `GeneralTeam`, and `GroupConfig`.
- Broad-validation trigger: present because production scheduling and live group timeout state mutation are enabled.
- Broad .NET decision: full project/solution validation was skipped after focused validation passed because the changed live surface is isolated to group timeout dispatch/scheduler composition, and the focused command built the affected project while covering group runtime, dispatch, scheduler, production DI graph, and adjacent alliance timeout behavior.
- Why this scope is sufficient: no packet primitives, persistence mapping, database schema, crypto, socket protocol framing, or broad world lifecycle code changed. Packet fanout is covered through captured dispatch intents and existing packet constructors.

## Additional Hygiene

```powershell
git diff --check
```

- Passed with expected LF-to-CRLF working-tree warnings for touched C# files.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.model.team.group.PlayerGroupService.createGroup` | `Aion.GameServer.Services.PlayerGroupRuntime.CreateOrUpdateGroup` through production graph | Service/runtime composition | Partial | Unit Tested | Partial Parity | C# now starts one group offline timeout checker after first successful group creation; method still also serves update-style tests unlike Java's create-only API. |
| `com.aionemu.gameserver.model.team.group.PlayerGroupService.initializeOfflineCheck` | `Aion.GameServer.Services.PlayerGroupOfflineTimeoutScheduler.Start` | Service scheduler | Partial | Unit Tested | Partial Parity | Java cadence of 1 second initial delay and 30 second period is covered. |
| `com.aionemu.gameserver.model.team.group.PlayerGroupService.OfflinePlayerChecker` | `Aion.GameServer.Services.PlayerGroupOfflineTimeoutDispatchService` and `PlayerGroupRuntime.RemoveNextExpiredOfflineMemberWithLeavePlan` | Scheduled dispatcher/runtime | Partial | Unit Tested | Partial Parity | Expired offline members are removed with configured `GROUP_REMOVE_TIME`; multi-group Java `ConcurrentHashMap` ordering is not asserted. |
| `com.aionemu.gameserver.model.team.group.events.PlayerGroupLeavedEvent` | `Aion.GameServer.Services.PlayerGroupRuntime.RemoveMemberWithLeavePlan` | Team leave event | Partial | Unit Tested | Partial Parity | Timeout fanout and no-timeout-leader-change branch are covered; full mentor/base-event fanout remains partial. |
| `com.aionemu.gameserver.configs.main.GroupConfig` | `Aion.GameServer.Configuration.GameServerGroupOptions` | Config | Partial | Unit Tested | Partial Parity | `GROUP_REMOVE_TIME` and `ALLIANCE_REMOVE_TIME` are loaded and consumed by schedulers; broader config class not fully audited in this UOW. |

## Metrics

- Total Java artifacts discovered in this UOW: 5
- Total artifacts ported or extended in this UOW: 6
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 5
- Total blocked artifacts: 0
- Conservative Phase 6 parity estimate: 41%

## Remaining Risks

- Group timeout dispatch now sends packets through the socket registry, but no real-client/host lifecycle smoke was added.
- Java group iteration uses `ConcurrentHashMap`; C# dictionary ordering is not claimed as parity across multiple groups.
- Full group leave parity still has remaining mentor/base-event/world side-effect gaps outside this timeout slice.
- Vortex active lifecycle population and online Vortex kick behavior remain incomplete from the previous UOW.
