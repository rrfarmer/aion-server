# Phase 6 Session 2446 Completion

## UOW

[Phase 6] UOW-2446: Enable production alliance offline-timeout scheduler callback

## Status

Completed and validated with focused .NET tests.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/model/team/alliance/PlayerAllianceService.java`
- `game-server/src/com/aionemu/gameserver/model/team/alliance/PlayerAllianceService.OfflinePlayerAllianceChecker`
- `game-server/src/com/aionemu/gameserver/utils/ThreadPoolManager.java`

## C# Changes

- `dotnetConversion/src/Aion.GameServer/Program.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupServiceCollectionExtensions.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupServiceCollectionExtensionsTests.cs`

## Implementation Notes

- Added `AddFindGroupSingletonGraphWithAllianceOfflineTimeoutScheduler` as the named production graph for Java-like alliance offline timeout scheduling.
- `Program.cs` now uses the scheduler-enabled graph instead of the non-scheduling find-group graph.
- The helper wires `PlayerAllianceRuntime` to resolve `PlayerAllianceOfflineTimeoutScheduler.Start` through the service provider on the first successful alliance creation.
- This preserves the Java `offlineCheckStarted.compareAndSet(false, true)` behavior already implemented in `PlayerAllianceRuntime`.
- The existing `ThreadPoolManager` singleton remains the task owner and cancellation/lifetime boundary; no new hosted service was introduced.

## Tests

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `AddFindGroupSingletonGraphWithAllianceOfflineTimeoutScheduler_StartsOnceLikeJavaCreateAlliance` | Unit/composition | `PlayerAllianceService.createAlliance`, `initializeOfflineCheck`, `ThreadPoolManager.scheduleAtFixedRate` source review | Production scheduler-enabled graph starts the offline checker once after repeated alliance creation | Schedule observation shows one fixed-rate task with 1 second delay and 30 second period after three alliance creations | Does not run the production host or real client |
| `PlayerAllianceRuntimeTests` scheduler-trigger cases | Unit | `offlineCheckStarted.compareAndSet(false, true)` | `PlayerAllianceRuntime` invokes the callback once only after successful alliance creation | Runtime callback count stays at one across repeated creations and duplicate creation failure | Callback target is composition-tested separately |
| `PlayerAllianceOfflineTimeoutSchedulerTests` | Unit | `OfflinePlayerAllianceChecker.run`, `GroupConfig.ALLIANCE_REMOVE_TIME`, scheduler cadence | Scheduler cadence and scan-time configured removal delay | Fixed-rate delay/period and configured removal time are asserted | Vortex offence side effect remains unexecuted |

## Validation Decision

- Changed surface: live startup composition plus service graph helper.
- Specific behavior/contract: production game-server composition enables Java-like alliance offline timeout scheduling; first successful alliance creation starts one fixed-rate checker with Java delay/period.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupServiceCollectionExtensionsTests|FullyQualifiedName~GameClientSocketServerSmokeTests|FullyQualifiedName~PlayerAllianceOfflineTimeoutSchedulerTests|FullyQualifiedName~PlayerAllianceRuntimeTests|FullyQualifiedName~PlayerAllianceOfflineTimeoutDispatchServiceTests" --no-restore
```

- Result: Passed, 48 tests. Existing nullable/analyzer warnings were emitted.
- Focused Java/Maven command: skipped; no Java source or Java fixtures changed, and the Java source-of-truth behavior was direct review of `createAlliance`, `initializeOfflineCheck`, and `OfflinePlayerAllianceChecker`.
- Broad-validation trigger: live scheduler composition was enabled.
- Broad .NET decision: full project/solution validation was skipped after the focused command passed because the changed live surface was isolated to the find-group/alliance scheduler graph, and the focused command covered the production helper, socket shared-runtime adjacency, scheduler cadence, runtime one-shot trigger, and timeout dispatch behavior.
- Why this scope is sufficient: no packet primitives, persistence mapping, database schema, crypto, serialization helpers, or broad world mutation APIs changed; the focused command built the affected project and exercised the exact Java-derived scheduling contract.

## Additional Hygiene

```powershell
git diff --check
```

- Passed with expected LF-to-CRLF working-tree warnings for touched C# files.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.createAlliance` | `Aion.GameServer.Services.PlayerAllianceRuntime.CreateAlliance` through production graph | Service/runtime composition | Partial | Unit Tested | Partial Parity | Production graph now connects the one-shot create-alliance trigger to scheduler start. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.initializeOfflineCheck` | `Aion.GameServer.Services.PlayerAllianceOfflineTimeoutScheduler.Start` via `AddFindGroupSingletonGraphWithAllianceOfflineTimeoutScheduler` | Service scheduler | Partial | Unit Tested | Partial Parity | Java cadence of 1 second initial delay and 30 second period is composition-tested through the production helper. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.OfflinePlayerAllianceChecker` | `Aion.GameServer.Services.PlayerAllianceOfflineTimeoutScheduler` and `PlayerAllianceOfflineTimeoutDispatchService` | Scheduled service/dispatcher | Partial | Unit Tested | Partial Parity | Scan and dispatch behavior are tested, but offence Vortex side effect is still a result flag rather than live execution. |
| `com.aionemu.gameserver.utils.ThreadPoolManager.scheduleAtFixedRate` | `Aion.GameServer.Utils.ThreadPoolManager.ScheduleAtFixedRateTask` | Scheduler utility | Partial | Adjacent Unit Tested | Partial Parity | Existing scheduler owns the live task and is disposed through the registered singleton lifetime; scheduler internals were not changed. |

## Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported or extended in this UOW: 3
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 0
- Conservative Phase 6 parity estimate: 41%

## Remaining Risks

- Offence-invader `VortexService.removeInvaderPlayer` remains unexecuted for alliance timeout removals.
- Group offline timeout checker remains a likely parallel gap.
- The scheduler is enabled in production composition, but no real-host lifecycle smoke was added in this UOW.
- Alliance timeout parity is still partial until the remaining Vortex side effect and any group checker equivalent are completed.
