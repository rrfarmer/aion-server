# Phase 6 Session 2360 Handoff - Periodic Opening Messages

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2360-Completion.md`
- `docs/Phase-6-Session-2360-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Prefer production/game parity units. Tests and docs should support concrete parity work, not become standalone evidence/reporting loops.

Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger before execution.

## Current State

Last completed UOW: UOW-2360, added exact C# helpers for Java periodic registration opening messages.

Recent production parity slices:

- Autogroup instance leave cleanup and destroy workflow are wired into delayed teleport leave.
- Open-registration refresh packets after autogroup leave are planned and sent.
- Periodic registration service models Java open/close state transitions, broadcast plans, and live dispatch through the online-player registry.
- Periodic registration service now has exact Java mask-to-opening-message mapping for scheduled masks `1`, `2`, `3`, `107`, `108`, `109`, and `111`.

Still not proven or not implemented:

- Java periodic registration cron scheduling and timed close-task cancellation.
- Concrete stop-registration wiring into C# autogroup queue state.
- Java quick-entry queue refill after autogroup leave.
- Full forced-exit packet fanout for instance destruction.
- Java map leave callback parity: `ConquerorAndProtectorService.getInstance().onLeaveMap(player)`.
- Pet position update and same-map spawn parity in `TeleportService.SpawnTask.run`.

## Commits Made

- `7ef260c60 [Phase 6][UOW-2359] Dispatch periodic registration broadcasts`
- `[Phase 6][UOW-2360] Add periodic opening messages`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PeriodicInstanceRegistrationService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PeriodicInstanceRegistrationServiceTests.cs`
- `docs/Phase-6-Session-2360-Completion.md`
- `docs/Phase-6-Session-2360-Handoff.md`

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GamePacketTests.SmSystemMessage_WritesDialogTooFarMessages|FullyQualifiedName~PeriodicInstanceRegistrationServiceTests.CreateOpeningMessageForMaskId_ReturnsJavaScheduledOpeningMessages|FullyQualifiedName~PeriodicInstanceRegistrationServiceTests.OpenRegistrationAndBroadcastAsync_SendsPlannedPacketsToOnlineLevelRangeLikeJavaWorldFanout" --no-restore
```

Result: passed 3, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Documentation hygiene:

```powershell
git diff --check
```

Result: passed. Git emitted CRLF working-copy warnings only.

Focused Java/Maven validation: skipped because no targeted Java unit fixture exists for these generated system-message helpers; Java source review identified the exact IDs.

Broad-validation trigger: none. Packet serialization primitives were not changed; only helper factories and a service mapper were added.

Broad .NET decision: skipped full project/solution validation after the focused test compiled the affected project/dependencies and passed.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` periodic opening helpers | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` periodic opening helpers | Packet Helper | Partial | Unit Tested | Partial Parity | Seven helper IDs used by Java periodic registration scheduling are covered. This does not verify unrelated `SM_SYSTEM_MESSAGE` helpers. |
| `com.aionemu.gameserver.services.instance.PeriodicInstanceManager` constructor scheduled message selection | `Aion.GameServer.Services.PeriodicInstanceRegistrationService.CreateOpeningMessageForMaskId(int)` | Service Mapping | Partial | Unit Tested | Partial Parity | Java mask-to-message mapping is covered for masks `1`, `2`, `3`, `107`, `108`, `109`, and `111`. Cron scheduling remains missing. |

## Next Sequential UOW

Recommended next production scope: model Java periodic registration timed scheduling without enabling broad background behavior unexpectedly.

Java artifacts:

- `game-server/src/com/aionemu/gameserver/services/instance/PeriodicInstanceManager.java`
- `game-server/src/com/aionemu/gameserver/configs/main/AutoGroupConfig.java`
- `game-server/src/com/aionemu/gameserver/services/cron/CronService.java`
- `game-server/src/com/aionemu/gameserver/utils/ThreadPoolManager.java`

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Services/PeriodicInstanceRegistrationService.cs`
- `dotnetConversion/src/Aion.GameServer/Program.cs`
- existing scheduler/background-service abstractions under `dotnetConversion/src`
- `dotnetConversion/tests/Aion.GameServer.Tests/PeriodicInstanceRegistrationServiceTests.cs`

Focused C# validation recipe:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PeriodicInstanceRegistrationServiceTests" --no-restore
```

Behavior under validation: scheduling model should select Java mask ids, opening messages, and registration periods, and should preserve Java open-now then close-after-period behavior without starting unbounded live timers in tests.

Focused Java/Maven command: not expected unless a targeted Java fixture is added. Use Java source review for cron expressions, periods, and close scheduling behavior.

Broad-validation trigger: scheduler/background service wiring if enabled in production DI. Start with a non-live scheduling plan or disabled hosted service; document a trigger before any broad validation.

## Safe Candidates

- Add a non-live periodic registration schedule plan from Java config constants.
- Wire concrete close stop-registration behavior into C# autogroup queue state.
- Port Java quick-entry queue refill after autogroup leave.
- Port or model `ConquerorAndProtectorService.onLeaveMap` if supporting C# state exists.
- Review and port pet position update in delayed teleport completion.

Avoid:

- Evidence/reporting-only units.
- Full `.NET` project tests or solution builds without a documented broad-validation trigger.
- Updating `docs/PHASE-6-PROGRESS.md`.
