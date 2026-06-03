# Phase 6 Session 2442 Completion

## UOW

[Phase 6] UOW-2442: Add alliance offline-timeout scheduler wrapper

## Status

Completed and validated with focused .NET tests.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/model/team/alliance/PlayerAllianceService.java`
- `game-server/src/com/aionemu/gameserver/model/team/group/PlayerGroupService.java`
- `game-server/src/com/aionemu/gameserver/configs/main/GroupConfig.java`

## C# Changes

- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceOfflineTimeoutScheduler.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerAllianceOfflineTimeoutSchedulerTests.cs`

## Implementation Notes

- Added `PlayerAllianceOfflineTimeoutScheduler`, a narrow wrapper around `PlayerAllianceOfflineTimeoutDispatchService.DispatchExpiredScanAsync`.
- The scheduler exposes Java-like timing constants:
  - Initial delay: `TimeSpan.FromSeconds(1)`
  - Period: `TimeSpan.FromSeconds(30)`
- `Start` schedules through the existing C# `ThreadPoolManager.ScheduleAtFixedRateTask`, matching Java `ThreadPoolManager.getInstance().scheduleAtFixedRate(new OfflinePlayerAllianceChecker(), 1000, 30 * 1000)`.
- `RunScanOnceAsync` uses `GameServerOptions.Group.AllianceRemoveTimeSeconds`, matching Java `GroupConfig.ALLIANCE_REMOVE_TIME` for normal alliances.
- Auto-alliance 60-second timeout behavior remains in `PlayerAllianceRuntime.GetOfflineKickDelaySeconds`.
- This UOW intentionally did not register the scheduler in startup/DI composition.

## Tests

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `Start_SchedulesJavaOfflineAllianceCheckerCadenceWithoutStartupRegistration` | Unit | `PlayerAllianceService.initializeOfflineCheck` source review | Scheduler uses fixed-rate delay 1 second and period 30 seconds | C# schedule observation matches Java literal values | Does not prove startup registration because none was added |
| `RunScanOnceAsync_UsesConfiguredAllianceRemoveTimeLikeGroupConfig` | Regression | `OfflinePlayerAllianceChecker.run` and `GroupConfig.ALLIANCE_REMOVE_TIME` source review | Manual scan tick passes configured alliance timeout into dispatcher | Expired member leaves, non-expired member remains under configured timeout | Does not execute Java runtime; offence side effect remains result-flag only |

## Validation Decision

- Changed surface: one C# non-live scheduler wrapper plus focused tests.
- Specific behavior/contract: Java alliance offline checker cadence and use of configured alliance removal timeout during a scan tick.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerAllianceOfflineTimeoutSchedulerTests|FullyQualifiedName~PlayerAllianceOfflineTimeoutDispatchServiceTests" --no-restore
```

- Result: Passed, 5 tests.
- Focused Java/Maven command: skipped; no Java source or Java fixtures changed, and the Java parity source for this slice was direct source review of literals/control flow.
- Broad-validation trigger: none. Startup/DI registration was intentionally not added.
- Broad .NET decision: skipped; the focused command built the affected project and validated the scheduler/dispatcher contract.
- Why this scope is sufficient: the UOW only added an unregistered scheduler wrapper and did not change shared scheduler primitives, packet serialization, persistence, world state, or live startup composition.

## Additional Hygiene

```powershell
git diff --check
```

- Passed before staging, with no tracked diff errors. Staged hygiene was run before commit.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.initializeOfflineCheck` | `Aion.GameServer.Services.PlayerAllianceOfflineTimeoutScheduler.Start` | Service scheduler | Partial | Unit Tested | Partial Parity | Java source reviewed; fixed-rate delay and period are unit tested. Startup registration is intentionally missing, so full lifecycle parity is not claimed. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.OfflinePlayerAllianceChecker.run` | `Aion.GameServer.Services.PlayerAllianceOfflineTimeoutScheduler.RunScanOnceAsync` and `PlayerAllianceOfflineTimeoutDispatchService.DispatchExpiredScanAsync` | Service executor | Partial | Regression Tested | Partial Parity | Java source reviewed; configured timeout passes through scan wrapper and dispatcher loop. Offence-invader Vortex removal is still not executed. |
| `com.aionemu.gameserver.configs.main.GroupConfig.ALLIANCE_REMOVE_TIME` | `Aion.GameServer.Configuration.GameServerGroupOptions.AllianceRemoveTimeSeconds` | Config | Partial | Regression Tested | Partial Parity | Existing config binding from UOW-2441 is consumed by the scheduler wrapper. Java property key/default coverage remains in `GameServerOptionsTests`. |

## Metrics

- Total Java artifacts discovered in this UOW: 3
- Total artifacts ported or extended in this UOW: 2
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 3
- Total blocked artifacts: 0
- Conservative Phase 6 parity estimate: 41%

## Remaining Risks

- Alliance timeout scheduler is still not registered in game-server startup composition.
- Java starts the checker only after first alliance creation; C# startup trigger semantics are still unported.
- Offence-invader VortexService removal remains a result flag and does not execute the side effect.
- Group offline timeout scheduler/executor remains a likely parallel gap.
