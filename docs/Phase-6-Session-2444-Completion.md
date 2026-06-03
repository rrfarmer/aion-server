# Phase 6 Session 2444 Completion

## UOW

[Phase 6] UOW-2444: Add alliance offline-timeout scheduler composition readiness

## Status

Completed and validated with focused .NET tests.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/model/team/alliance/PlayerAllianceService.java`
- `game-server/src/com/aionemu/gameserver/model/team/alliance/PlayerAllianceService.OfflinePlayerAllianceChecker`
- `game-server/src/com/aionemu/gameserver/utils/ThreadPoolManager.java`

## C# Changes

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupServiceCollectionExtensions.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupServiceCollectionExtensionsTests.cs`

## Implementation Notes

- `AddFindGroupSingletonGraph` now registers the alliance timeout dispatcher and scheduler services plus a shared `PlayerLeagueRuntime`.
- The extension accepts an optional `createAllianceOfflineTimeoutStartCallback` factory.
- When opted in, the shared `PlayerAllianceRuntime` can receive a callback that resolves `PlayerAllianceOfflineTimeoutScheduler.Start`.
- The default extension call path remains non-scheduling; this UOW did not change production `Program.cs` or hosted startup behavior.
- Focused tests validate that the opt-in shared graph schedules exactly one Java-cadence fixed-rate task after repeated alliance creation.

## Tests

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `AddFindGroupSingletonGraph_CanWireAllianceTimeoutSchedulerCallbackOnceLikeJavaCreateAlliance` | Unit/composition | `PlayerAllianceService.createAlliance`, `initializeOfflineCheck`, and `ThreadPoolManager.scheduleAtFixedRate` source review | Opt-in shared graph wires `PlayerAllianceRuntime` to scheduler start and first alliance creation schedules one fixed-rate task | Schedule observation shows one fixed-rate task with 1 second delay and 30 second period after three creations | Default production `Program.cs` still uses the non-scheduling path |

## Validation Decision

- Changed surface: non-live service collection composition/readiness plus focused test.
- Specific behavior/contract: opted-in shared graph can construct `PlayerAllianceRuntime` with a scheduler-start callback, and Java-like first alliance creation schedules exactly one offline timeout check using the Java cadence.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupServiceCollectionExtensionsTests|FullyQualifiedName~PlayerAllianceOfflineTimeoutSchedulerTests|FullyQualifiedName~PlayerAllianceRuntimeTests" --no-restore
```

- Result: Passed, 41 tests.
- Focused Java/Maven command: skipped; no Java source or Java fixtures changed, and Java source review identified the trigger/cadence literals under test.
- Broad-validation trigger: none for this opt-in readiness path. No live startup registration or hosted service was added.
- Broad .NET decision: skipped; the focused command built the affected project and validated the composition/runtime/scheduler contract.
- Why this scope is sufficient: the UOW only added opt-in DI readiness and did not alter packet primitives, persistence, world state mutation, live startup composition, or network dispatch behavior.

## Additional Hygiene

```powershell
git diff --check
```

- Passed with expected LF-to-CRLF working-tree warnings for touched C# files.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.createAlliance` | `Aion.GameServer.Services.PlayerAllianceRuntime.CreateAlliance` composed through `FindGroupServiceCollectionExtensions` | Service/runtime composition | Partial | Unit Tested | Partial Parity | Opt-in shared graph can connect the create-alliance one-shot trigger to scheduler start. Default production graph remains non-scheduling. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.initializeOfflineCheck` | `Aion.GameServer.Services.PlayerAllianceOfflineTimeoutScheduler.Start` | Service scheduler | Partial | Unit Tested | Partial Parity | Scheduler start cadence remains tested through the opt-in graph. Lifecycle cancellation relies on `ThreadPoolManager` shutdown; no hosted registration was added. |
| `com.aionemu.gameserver.utils.ThreadPoolManager.scheduleAtFixedRate` | `Aion.GameServer.Utils.ThreadPoolManager.ScheduleAtFixedRateTask` | Scheduler utility | Partial | Adjacent Unit Tested | Partial Parity | Existing C# scheduler is used by the opt-in callback. This UOW did not change scheduler internals. |

## Metrics

- Total Java artifacts discovered in this UOW: 3
- Total artifacts ported or extended in this UOW: 2
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 3
- Total blocked artifacts: 0
- Conservative Phase 6 parity estimate: 41%

## Remaining Risks

- Production `Program.cs` still calls `AddFindGroupSingletonGraph()` without the alliance timeout scheduler callback.
- `GameClientSocketServer`/`GameServerConnection` league runtime sharing remains a broader composition gap; this UOW only registers a shared `PlayerLeagueRuntime` for the scheduler graph.
- Offence-invader VortexService removal remains a result flag and does not execute the Java side effect.
- Group offline timeout checker remains a likely parallel gap.
