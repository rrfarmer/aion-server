# Phase 6 Session 2361 Completion - Model Periodic Registration Schedules

## Scope

Added a non-live Java schedule plan for periodic autogroup registration openings and close delays.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/services/instance/PeriodicInstanceManager.java`
- `game-server/src/com/aionemu/gameserver/configs/main/AutoGroupConfig.java`
- `game-server/src/com/aionemu/gameserver/services/cron/CronService.java`
- `game-server/src/com/aionemu/gameserver/utils/ThreadPoolManager.java`

Java behavior used:

- `PeriodicInstanceManager` schedules registration openings only when `AutoGroupConfig.AUTO_GROUP_ENABLE` is true.
- Java schedules masks `1`, `2`, and `3` with Dredgion cron `0 0 0,12,20 ? * *` and 60-minute registration period.
- Java schedules mask `107` with Kamar cron `0 0 0,20 ? * MON,WED,SAT` and 60-minute registration period.
- Java schedules mask `108` with Ophidan cron `0 0 12,19 ? * *` and 60-minute registration period.
- Java schedules mask `109` with Iron Wall cron `0 0 0,12 ? * SUN` and 60-minute registration period.
- Java schedules mask `111` with Idgel Dome cron `0 0 23 ? * *` and 60-minute registration period.
- Java close scheduling uses `registrationPeriod * 60000`, represented here as `CloseDelay`.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

- Added `PeriodicInstanceRegistrationService.CreateDefaultSchedulePlan(bool autoGroupEnabled)`.
- Added `PeriodicInstanceRegistrationSchedulePlan` and `PeriodicInstanceRegistrationScheduleEntry`.
- Added tests for Java disabled behavior and the exact seven default schedule entries.

Known limitations:

- This is a non-live plan; it does not register cron callbacks or start timers.
- C# config binding for autogroup cron/period overrides is not ported yet.
- Close task storage/cancellation remains missing.
- Concrete stop-registration wiring into C# autogroup queue state remains missing.

## Validation Decision

- Changed surface: production non-live schedule plan plus tests.
- Specific behavior/contract: schedule model should select Java mask ids, opening messages, default cron expressions, registration periods, and close delays without starting unbounded live timers.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PeriodicInstanceRegistrationServiceTests" --no-restore
```

Result: passed 9, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

- Documentation hygiene:

```powershell
git diff --check
```

Result: passed. Git emitted CRLF working-copy warnings only.

- Focused Java/Maven command: skipped. No targeted Java unit fixture exists for this constructor/config path; Java source review identified the exact defaults and scheduling calls.
- Broad-validation trigger: none. No hosted service, live scheduler, packet primitive, persistence, or shared infrastructure was changed.
- Broad .NET decision: skipped full project/solution validation after the focused test compiled the affected project/dependencies and passed.
- Why this scope is sufficient: the edited service and exact Java-derived schedule table are directly asserted.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.instance.PeriodicInstanceManager` constructor scheduling | `Aion.GameServer.Services.PeriodicInstanceRegistrationService.CreateDefaultSchedulePlan(bool)` | Service Mapping | Partial | Unit Tested | Partial Parity | Java default schedule entries, message ids, and close delays are modeled. No cron callback registration is live yet. |
| `com.aionemu.gameserver.configs.main.AutoGroupConfig` periodic registration defaults | `Aion.GameServer.Services.PeriodicInstanceRegistrationScheduleEntry` | Config Model | Partial | Unit Tested | Partial Parity | Default values are modeled from Java annotations. C# config override binding is not ported yet. |
| `com.aionemu.gameserver.services.cron.CronService` | No live C# equivalent in this UOW | Scheduler | Not Started | No Tests | Needs Verification | Cron expressions are preserved as strings only. No scheduling engine parity is claimed. |
| `com.aionemu.gameserver.utils.ThreadPoolManager.schedule(...)` close task | `PeriodicInstanceRegistrationScheduleEntry.CloseDelay` | Scheduler Intent | Partial | Unit Tested | Partial Parity | Close delay is modeled as `TimeSpan.FromMinutes(period)`. Close task storage/cancellation remains missing. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `PeriodicInstanceRegistrationServiceTests.CreateDefaultSchedulePlan_ReturnsNoEntriesWhenJavaAutoGroupDisabled` | Unit | Java source review | Disabled autogroup config produces no scheduled registration entries. | Focused schedule-plan test. | Does not cover runtime config binding. |
| `PeriodicInstanceRegistrationServiceTests.CreateDefaultSchedulePlan_ReturnsJavaPeriodicRegistrationSchedules` | Unit | Java source review | Exact Java default mask IDs, cron expressions, periods, close delays, and opening message IDs. | Focused schedule-plan test. | Does not start cron callbacks. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported or extended in this UOW: 4
- Total artifacts with verified parity: 0
- Total artifacts needing verification or remaining partial: 4
- Total blocked artifacts: 0
- Estimated overall migration completion: Phase 6 remains in progress; overall completion unchanged conservatively.

## Remaining Gaps

- Java periodic registration cron callback registration.
- C# config override binding for autogroup schedules and periods.
- Close task storage/cancellation.
- Concrete stop-registration wiring into C# autogroup queue state.
- Java quick-entry queue refill after autogroup leave.
- Full forced-exit packet fanout for instance destruction.
- `ConquerorAndProtectorService.onLeaveMap` parity.
- Pet position update and same-map spawn behavior in delayed teleport completion.

## Commit

Commit message:

```text
[Phase 6][UOW-2361] Model periodic registration schedules
```
