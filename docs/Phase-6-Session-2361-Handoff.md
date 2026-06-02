# Phase 6 Session 2361 Handoff - Periodic Registration Schedule Model

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2361-Completion.md`
- `docs/Phase-6-Session-2361-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Prefer production/game parity units. Tests and docs should support concrete parity work, not become standalone evidence/reporting loops.

Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger before execution.

## Current State

Last completed UOW: UOW-2361, added a non-live Java schedule model for periodic autogroup registration openings.

Recent production parity slices:

- Autogroup instance leave cleanup and destroy workflow are wired into delayed teleport leave.
- Open-registration refresh packets after autogroup leave are planned and sent.
- Periodic registration service models Java open/close state transitions, broadcast plans, and live dispatch through the online-player registry.
- Periodic registration service has exact Java mask-to-opening-message mapping for scheduled masks `1`, `2`, `3`, `107`, `108`, `109`, and `111`.
- Periodic registration service now exposes default schedule entries for those masks, including Java cron strings, periods, and close delays.

Still not proven or not implemented:

- Java periodic registration cron callback registration.
- C# config override binding for autogroup schedules and periods.
- Close task storage/cancellation.
- Concrete stop-registration wiring into C# autogroup queue state.
- Java quick-entry queue refill after autogroup leave.
- Full forced-exit packet fanout for instance destruction.
- Java map leave callback parity: `ConquerorAndProtectorService.getInstance().onLeaveMap(player)`.
- Pet position update and same-map spawn parity in `TeleportService.SpawnTask.run`.

## Commits Made

- `c71e1f20c [Phase 6][UOW-2360] Add periodic opening messages`
- `[Phase 6][UOW-2361] Model periodic registration schedules`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Services/PeriodicInstanceRegistrationService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PeriodicInstanceRegistrationServiceTests.cs`
- `docs/Phase-6-Session-2361-Completion.md`
- `docs/Phase-6-Session-2361-Handoff.md`

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PeriodicInstanceRegistrationServiceTests" --no-restore
```

Result: passed 9, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Documentation hygiene:

```powershell
git diff --check
```

Result: passed. Git emitted CRLF working-copy warnings only.

Focused Java/Maven validation: skipped because no targeted Java unit fixture exists for this constructor/config path; Java source review identified the exact defaults and scheduling calls.

Broad-validation trigger: none. No hosted service, live scheduler, packet primitive, persistence, or shared infrastructure was changed.

Broad .NET decision: skipped full project/solution validation after the focused test compiled the affected project/dependencies and passed.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.instance.PeriodicInstanceManager` constructor scheduling | `Aion.GameServer.Services.PeriodicInstanceRegistrationService.CreateDefaultSchedulePlan(bool)` | Service Mapping | Partial | Unit Tested | Partial Parity | Java default schedule entries, message ids, and close delays are modeled. No cron callback registration is live yet. |
| `com.aionemu.gameserver.configs.main.AutoGroupConfig` periodic registration defaults | `Aion.GameServer.Services.PeriodicInstanceRegistrationScheduleEntry` | Config Model | Partial | Unit Tested | Partial Parity | Default values are modeled from Java annotations. C# config override binding is not ported yet. |
| `com.aionemu.gameserver.services.cron.CronService` | No live C# equivalent in this UOW | Scheduler | Not Started | No Tests | Needs Verification | Cron expressions are preserved as strings only. No scheduling engine parity is claimed. |
| `com.aionemu.gameserver.utils.ThreadPoolManager.schedule(...)` close task | `PeriodicInstanceRegistrationScheduleEntry.CloseDelay` | Scheduler Intent | Partial | Unit Tested | Partial Parity | Close delay is modeled as `TimeSpan.FromMinutes(period)`. Close task storage/cancellation remains missing. |

## Next Sequential UOW

Recommended next production scope: model periodic registration close-task storage/cancellation around the existing open/close methods, still without live cron activation.

Java artifacts:

- `game-server/src/com/aionemu/gameserver/services/instance/PeriodicInstanceManager.java`
- `game-server/src/com/aionemu/gameserver/utils/ThreadPoolManager.java`

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Services/PeriodicInstanceRegistrationService.cs`
- `dotnetConversion/src/Aion.GameServer/Utils/ThreadPoolManager.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PeriodicInstanceRegistrationServiceTests.cs`

Focused C# validation recipe:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PeriodicInstanceRegistrationServiceTests" --no-restore
```

Behavior under validation: a successful scheduled open should record close-task intent for the mask and a successful close should clear/cancel that intent, matching Java's `registrationCloseTasksByMaskId`.

Focused Java/Maven command: not expected unless a targeted Java fixture is added. Use Java source review for map storage/cancellation behavior.

Broad-validation trigger: none if implemented as a non-live task-intent model. If real `ThreadPoolManager.Schedule` calls are enabled, scheduler/live-side-effect trigger applies and must be documented before broad validation.

## Safe Candidates

- Model close-task storage/cancellation intent without starting live timers.
- Add C# config option binding for autogroup schedule/period defaults.
- Wire concrete close stop-registration behavior into C# autogroup queue state.
- Port Java quick-entry queue refill after autogroup leave.
- Port or model `ConquerorAndProtectorService.onLeaveMap` if supporting C# state exists.

Avoid:

- Evidence/reporting-only units.
- Full `.NET` project tests or solution builds without a documented broad-validation trigger.
- Updating `docs/PHASE-6-PROGRESS.md`.
