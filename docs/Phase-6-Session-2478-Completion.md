# Phase 6 Session 2478 Completion

## UOW

[Phase 6] UOW-2478: Add Vortex start scheduled-stop metadata

## Status

Completed and validated with focused .NET tests.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/VortexService.java`
- `game-server/src/com/aionemu/gameserver/configs/main/CustomConfig.java`

## C# Changes

- `dotnetConversion/src/Aion.GameServer/Services/VortexStartScheduledStopPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`

## Implementation Notes

- Added non-live `VortexStartScheduledStopPlanService`.
- Added `VortexStartScheduledStopPlan` metadata for Java `ThreadPoolManager.schedule(() -> stopInvasion(id), getDuration(), TimeUnit.HOURS)`.
- The plan records the target Vortex location id, duration hours, Java duration source, time unit, scheduled method, and Java source breadcrumb.
- Schedule intent is only produced after `VortexStartInvasionCoordinatorReport` reports `Planned`.
- Duplicate-start guard reports return `NotScheduledAlreadyStarted` and no schedule intent.
- Scope remains metadata-only. No live scheduler task or stop dispatch is created.

## Tests

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `StartScheduledStopPlan_PlansJavaDurationOnlyAfterCoordinatorStartSucceeds` | Unit | `VortexService.startInvasion` and `getDuration` source review | Successful start report produces non-live stop schedule intent with duration hours and `TimeUnit.HOURS` metadata | Focused C# test validates status, duration source, scheduled method, time unit, and disabled live scheduling | Does not execute a real scheduler task |
| `StartScheduledStopPlan_DuplicateStartOmitsScheduleIntent` | Unit | `VortexService.startInvasion` active-map guard | Duplicate-start reports do not create scheduled-stop intent | Focused C# test validates guard status, zero duration, Java source, and disabled live scheduling | Full Java synchronized concurrency remains partial |

## Validation Decision

- Changed surface: non-live Vortex scheduled-stop metadata and focused tests.
- Specific behavior/contract: C# scheduled-stop metadata mirrors Java `VortexService.startInvasion` by planning a non-live stop schedule only after successful start and omitting it for duplicate-start guard reports.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests" --no-restore
```

- Result: Passed, 41 tests. Existing nullable/analyzer warnings were emitted.
- Focused Java/Maven command: skipped; no Java source or fixtures changed and no narrow Java Vortex lifecycle/static-data fixture exists. Java source was reviewed directly.
- Broad-validation trigger: none. This UOW only adds non-live schedule metadata and tests, without changing real scheduler wiring or live stop dispatch.
- Broad .NET decision: skipped; focused validation built the affected project and covered the edited Vortex schedule metadata paths.
- Why this scope is sufficient: the new code is an inert schedule-intent planner that consumes an already tested start coordinator report.

## Additional Hygiene

```powershell
git diff --check
git diff --cached --check
```

- Passed. Git reported expected CRLF conversion warnings for edited/new files.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.VortexService.startInvasion` | `Aion.GameServer.Services.VortexStartScheduledStopPlanService` | Runtime planner | Partial | Unit Tested | Partial Parity | C# models Java scheduled-stop intent after successful start. It does not create a live `ThreadPoolManager` task or dispatch `stopInvasion`. |
| `com.aionemu.gameserver.services.VortexService.getDuration` | `Aion.GameServer.Services.VortexStartScheduledStopPlan` | Runtime metadata | Partial | Unit Tested | Partial Parity | C# records duration hours and `CustomConfig.VORTEX_DURATION` source metadata. Config binding for this specific value is not wired into live Vortex scheduling. |

## Metrics

- Total Java artifacts discovered in this UOW: 2
- Total artifacts ported or extended in this UOW: 1
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 2
- Total blocked artifacts: 0
- Conservative Phase 6 parity estimate: 41%

## Remaining Risks

- Live Vortex start scheduling remains unported.
- Real scheduler integration, cancellation, shutdown behavior, and live stop dispatch are not implemented.
- `CustomConfig.VORTEX_DURATION` is recorded as metadata source but not read from live configuration in this UOW.
- Rift generator initialization and defender alliance mutation remain metadata-only.
