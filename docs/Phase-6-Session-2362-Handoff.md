# Phase 6 Session 2362 Handoff - Periodic Close Task Intent

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2362-Completion.md`
- `docs/Phase-6-Session-2362-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Prefer production/game parity units. Tests and docs should support concrete parity work, not become standalone evidence/reporting loops.

Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger before execution.

## Current State

Last completed UOW: UOW-2362, modeled Java periodic registration close-task intent storage and clearing.

Recent production parity slices:

- Autogroup instance leave cleanup and destroy workflow are wired into delayed teleport leave.
- Open-registration refresh packets after autogroup leave are planned and sent.
- Periodic registration service models Java open/close state transitions, broadcast plans, live dispatch through the online-player registry, exact opening messages, default schedule entries, and close-task intent storage.

Still not proven or not implemented:

- Java periodic registration cron callback registration.
- Real scheduled close task creation/cancellation.
- C# config override binding for autogroup schedules and periods.
- Concrete stop-registration wiring into C# autogroup queue state.
- Java quick-entry queue refill after autogroup leave.
- Full forced-exit packet fanout for instance destruction.
- Java map leave callback parity: `ConquerorAndProtectorService.getInstance().onLeaveMap(player)`.
- Pet position update and same-map spawn parity in `TeleportService.SpawnTask.run`.

## Commits Made

- `3d23aefa5 [Phase 6][UOW-2361] Model periodic registration schedules`
- `[Phase 6][UOW-2362] Model periodic close task intent`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Services/PeriodicInstanceRegistrationService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PeriodicInstanceRegistrationServiceTests.cs`
- `docs/Phase-6-Session-2362-Completion.md`
- `docs/Phase-6-Session-2362-Handoff.md`

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PeriodicInstanceRegistrationServiceTests" --no-restore
```

Result: passed 12, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Documentation hygiene:

```powershell
git diff --check
```

Result: passed. Git emitted CRLF working-copy warnings only.

Focused Java/Maven validation: skipped because no targeted Java unit fixture exists for this runtime map path; Java source review identified the map semantics.

Broad-validation trigger: none. No live scheduler, hosted service, packet primitive, persistence, or shared infrastructure changed.

Broad .NET decision: skipped full project/solution validation after the focused test compiled the affected project/dependencies and passed.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.instance.PeriodicInstanceManager.registrationCloseTasksByMaskId` | `Aion.GameServer.Services.PeriodicInstanceRegistrationService` close-task intents | Runtime State | Partial | Unit Tested | Partial Parity | Map semantics are modeled as non-live intent. Actual scheduled task storage/cancellation is not live yet. |
| `com.aionemu.gameserver.services.instance.PeriodicInstanceManager.openRegistration(...)` | `Aion.GameServer.Services.PeriodicInstanceRegistrationService.CreateScheduledOpenRegistrationBroadcastPlan(...)` | Service | Partial | Unit Tested | Partial Parity | Scheduled open records close intent and sends Java-shaped opening packets. Real delayed close scheduling remains missing. |
| `com.aionemu.gameserver.services.instance.PeriodicInstanceManager.closeRegistration(int)` | `Aion.GameServer.Services.PeriodicInstanceRegistrationService.CreateCloseRegistrationBroadcastPlan(...)` | Service | Partial | Unit Tested | Partial Parity | Close now clears close-task intent. Concrete stop-registration state and real task cancellation remain missing. |

## Next Sequential UOW

Recommended next production scope: wire concrete close `stopRegistrationsByMaskId` behavior into C# autogroup queue state if supporting state is present.

Java artifacts:

- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoGroupInstance.java`
- `game-server/src/com/aionemu/gameserver/services/instance/PeriodicInstanceManager.java`

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupInstanceLeaveRuntimeService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PeriodicInstanceRegistrationService.cs`
- existing autogroup runtime/registration state classes under `dotnetConversion/src/Aion.GameServer/Services`
- `dotnetConversion/tests/Aion.GameServer.Tests/AutoGroupInstanceLeaveRuntimeServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PeriodicInstanceRegistrationServiceTests.cs`

Focused C# validation recipe:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupInstanceLeaveRuntimeServiceTests|FullyQualifiedName~PeriodicInstanceRegistrationServiceTests" --no-restore
```

Narrow first if possible to the edited class and the single adjacent close-dispatch test.

Behavior under validation: close registration should invoke C# state cleanup equivalent to Java `AutoGroupService.stopRegistrationsByMaskId(maskId)` after close packets, while preserving duplicate-close no-op behavior.

Focused Java/Maven command: not expected unless a targeted Java fixture is added. Use Java source review for stop-registration state behavior.

Broad-validation trigger: live runtime state if concrete autogroup cleanup is enabled. Start focused; document trigger before any broad validation.

## Safe Candidates

- Wire concrete close stop-registration behavior into C# autogroup queue state.
- Add C# config option binding for autogroup schedule/period defaults.
- Port Java quick-entry queue refill after autogroup leave.
- Model real delayed close scheduling with `ThreadPoolManager` only after deciding on broad scheduler validation.
- Port or model `ConquerorAndProtectorService.onLeaveMap` if supporting C# state exists.

Avoid:

- Evidence/reporting-only units.
- Full `.NET` project tests or solution builds without a documented broad-validation trigger.
- Updating `docs/PHASE-6-PROGRESS.md`.
