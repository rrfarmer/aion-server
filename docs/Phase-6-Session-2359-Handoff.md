# Phase 6 Session 2359 Handoff - Periodic Registration Broadcast Dispatch

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2359-Completion.md`
- `docs/Phase-6-Session-2359-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Prefer production/game parity units. Tests and docs should support concrete parity work, not become standalone evidence/reporting loops.

Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger before execution.

## Current State

Last completed UOW: UOW-2359, added live dispatch methods for Java-like periodic registration open/close broadcasts.

Recent production parity slices:

- Delayed teleport leave sends Java reset-warning packets before position mutation/spawn packets.
- Instance leave handler is invoked before reset-warning message selection.
- Delayed teleport leave invokes autogroup runtime cleanup after reset-warning packet selection and before teleport completion.
- Autogroup leave runtime now handles cleanup, destroy workflow, and open-registration refresh packets.
- Periodic registration service models Java open/close state transitions and broadcast packet plans.
- Periodic registration service can now dispatch those plans through the online-player connection registry and invoke a close stop-registration callback after close packets.

Still not proven or not implemented:

- Java periodic registration cron scheduling and timed close-task cancellation.
- Exact Java scheduled opening system-message helper methods.
- Concrete stop-registration wiring into C# autogroup queue state.
- Java quick-entry queue refill after autogroup leave.
- Full forced-exit packet fanout for instance destruction.
- Java map leave callback parity: `ConquerorAndProtectorService.getInstance().onLeaveMap(player)`.
- Pet position update and same-map spawn parity in `TeleportService.SpawnTask.run`.

## Commits Made

- `3d8e5e8 [Phase 6][UOW-2358] Plan periodic registration broadcasts`
- `[Phase 6][UOW-2359] Dispatch periodic registration broadcasts`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Services/PeriodicInstanceRegistrationService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PeriodicInstanceRegistrationServiceTests.cs`
- `docs/Phase-6-Session-2359-Completion.md`
- `docs/Phase-6-Session-2359-Handoff.md`

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PeriodicInstanceRegistrationServiceTests" --no-restore
```

Result: passed 6, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Documentation hygiene:

```powershell
git diff --check
```

Result: passed. Git emitted CRLF working-copy warnings only.

Focused Java/Maven validation: skipped because no targeted Java unit fixture exists for this periodic registration service path; Java source review plus focused C# dispatch tests were used as evidence.

Broad-validation trigger: live fanout adapter was added, but the edited surface is isolated to `SendPacketToPlayerAsync` dispatch and covered by a fake registry.

Broad .NET decision: skipped full project/solution validation after the focused test compiled the affected project/dependencies and passed.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.instance.PeriodicInstanceManager.openRegistration(SM_SYSTEM_MESSAGE,int,long)` | `Aion.GameServer.Services.PeriodicInstanceRegistrationService.OpenRegistrationAndBroadcastAsync(...)` | Service Adapter | Partial | Unit Tested | Partial Parity | Online-player collection and packet dispatch are covered. Cron scheduling, close task storage, and exact opening-message factory helpers remain missing. |
| `com.aionemu.gameserver.services.instance.PeriodicInstanceManager.closeRegistration(int)` | `Aion.GameServer.Services.PeriodicInstanceRegistrationService.CloseRegistrationAndBroadcastAsync(...)` | Service Adapter | Partial | Unit Tested | Partial Parity | Close packet dispatch and broadcast-before-stop callback ordering are covered. Scheduled task cancellation and concrete autogroup stop wiring remain missing. |
| `com.aionemu.gameserver.world.World.forEachPlayer(...)` | `Aion.GameServer.Network.Aion.IGameClientConnectionRegistry.ForEachOnlinePlayer(...)` | Runtime Adapter | Partial | Unit Tested | Partial Parity | Used to snapshot online players for periodic registration fanout. Existing registry implementation was not changed in this UOW. |
| `com.aionemu.gameserver.utils.PacketSendUtility.sendPacket(Player,AionServerPacket)` | `Aion.GameServer.Network.Aion.IGameClientConnectionRegistry.SendPacketToPlayerAsync(...)` | Runtime Adapter | Partial | Unit Tested | Partial Parity | Dispatch call ordering is covered with a focused fake registry. Real socket bytes are covered indirectly by existing packet serialization tests. |

## Next Sequential UOW

Recommended next production scope: add exact C# `SmSystemMessage` helpers for the Java scheduled periodic registration opening messages, then use them as inputs to the periodic registration open methods.

Java artifacts:

- `game-server/src/com/aionemu/gameserver/services/instance/PeriodicInstanceManager.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PeriodicInstanceRegistrationService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PeriodicInstanceRegistrationServiceTests.cs`

Focused C# validation recipe:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GamePacketTests|FullyQualifiedName~PeriodicInstanceRegistrationServiceTests" --no-restore
```

Narrow first if needed: prefer exact new `GamePacketTests` method names plus `PeriodicInstanceRegistrationServiceTests.OpenRegistrationAndBroadcastAsync_SendsPlannedPacketsToOnlineLevelRangeLikeJavaWorldFanout`.

Behavior under validation: scheduled periodic opening message helper IDs should match Java `PeriodicInstanceManager` usage and should be dispatched after the entry-icon packet on open.

Focused Java/Maven command: not expected unless a targeted Java packet fixture is added. Java source review should identify the exact `SM_SYSTEM_MESSAGE` helper IDs.

Broad-validation trigger: none unless packet serialization changes beyond adding system-message helpers.

## Safe Candidates

- Add exact periodic registration opening `SmSystemMessage` helpers.
- Model timed close task scheduling after exact messages are available.
- Wire concrete close stop-registration behavior into C# autogroup queue state.
- Port or model `ConquerorAndProtectorService.onLeaveMap` if supporting C# state exists.
- Review and port pet position update in delayed teleport completion.

Avoid:

- Evidence/reporting-only units.
- Full `.NET` project tests or solution builds without a documented broad-validation trigger.
- Updating `docs/PHASE-6-PROGRESS.md`.
