# Phase 6 Session 2358 Handoff - Periodic Registration Broadcast Plans

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2358-Completion.md`
- `docs/Phase-6-Session-2358-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Prefer production/game parity units. Tests and docs should support concrete parity work, not become standalone evidence/reporting loops.

Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger before execution.

## Current State

Last completed UOW: UOW-2358, added Java-like periodic registration open/close broadcast planning.

Recent production parity slices:

- Delayed teleport leave sends Java reset-warning packets before position mutation/spawn packets.
- Instance leave handler is invoked before reset-warning message selection.
- Delayed teleport leave invokes autogroup runtime cleanup after reset-warning packet selection and before teleport completion.
- Autogroup leave runtime unregisters players, removes PvP group/alliance membership, removes registry state, invokes the existing instance destroy workflow when Java `destroyIfPossible` would destroy, and returns open-registration refresh packets when Java would call `PeriodicInstanceManager.checkAndSendOpenRegistrations(player)`.
- Production DI registers one shared `PeriodicInstanceRegistrationService`.
- Periodic registration service now models Java `openRegistration` and `closeRegistration` state transitions and broadcast packet plans for level-eligible online players.

Still not proven or not implemented:

- Java periodic registration cron scheduling and timed close-task cancellation.
- Live open/close broadcast fanout through online connections.
- Exact scheduled opening system-message helper methods.
- Java quick-entry queue refill after autogroup leave.
- Full forced-exit packet fanout for instance destruction.
- Java map leave callback parity: `ConquerorAndProtectorService.getInstance().onLeaveMap(player)`.
- Pet position update and same-map spawn parity in `TeleportService.SpawnTask.run`.

## Commits Made

- `88f7fd929 [Phase 6][UOW-2357] Refresh open autogroup registrations`
- `[Phase 6][UOW-2358] Plan periodic registration broadcasts`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Services/PeriodicInstanceRegistrationService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmAutoGroup.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PeriodicInstanceRegistrationServiceTests.cs`
- `docs/Phase-6-Session-2358-Completion.md`
- `docs/Phase-6-Session-2358-Handoff.md`

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PeriodicInstanceRegistrationServiceTests|FullyQualifiedName~GamePacketTests.SmAutoGroup_WritesJavaEntryIconOpenAndClosePayload" --no-restore
```

Result: passed 5, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Documentation hygiene:

```powershell
git diff --check
```

Result: passed. Git emitted CRLF working-copy warnings only.

Focused Java/Maven validation: skipped because no targeted Java unit fixture exists for this periodic registration service path; Java source review plus focused C# service/packet tests were used as evidence.

Broad-validation trigger: none. This unit did not enable live fanout, scheduler work, packet primitive changes, persistence, or shared infrastructure.

Broad .NET decision: skipped full project/solution validation after the focused test compiled the affected project/dependencies and passed.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.instance.PeriodicInstanceManager.openRegistration(SM_SYSTEM_MESSAGE,int,long)` | `Aion.GameServer.Services.PeriodicInstanceRegistrationService.CreateOpenRegistrationBroadcastPlan(...)` | Service | Partial | Unit Tested | Partial Parity | State transition and broadcast packet planning are covered. Cron scheduling, close task storage, and exact opening-message factory helpers remain missing. |
| `com.aionemu.gameserver.services.instance.PeriodicInstanceManager.closeRegistration(int)` | `Aion.GameServer.Services.PeriodicInstanceRegistrationService.CreateCloseRegistrationBroadcastPlan(...)` | Service | Partial | Unit Tested | Partial Parity | State transition, close packet planning, and `stopRegistrationsByMaskId` intent flag are covered. Live stop-registration call and scheduled task cancellation remain missing. |
| `com.aionemu.gameserver.services.instance.PeriodicInstanceManager.broadcastRegistrationUpdate(SM_SYSTEM_MESSAGE,int,boolean)` | `Aion.GameServer.Services.PeriodicInstanceRegistrationService.CreateRegistrationBroadcastPlan(...)` | Service Helper | Partial | Unit Tested | Partial Parity | Level-range filtering, entry-icon packet, optional open message, and close flag are tested. Live `World.forEachPlayer`/`PacketSendUtility` fanout remains missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_AUTO_GROUP` | `Aion.GameServer.Network.Aion.ServerPackets.SmAutoGroup` | Packet | Partial | Unit Tested | Partial Parity | Added `IsClosed` inspection only; serialization unchanged. Entry-icon open/close payload remains covered by packet test. |

## Next Sequential UOW

Recommended next production scope: wire periodic registration open/close broadcast plans to the live online-player fanout path if it can stay narrow.

Java artifacts:

- `game-server/src/com/aionemu/gameserver/services/instance/PeriodicInstanceManager.java`
- `game-server/src/com/aionemu/gameserver/world/World.java`
- `game-server/src/com/aionemu/gameserver/utils/PacketSendUtility.java`
- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Services/PeriodicInstanceRegistrationService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientSocketServer.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/IGameClientConnectionRegistry.cs`
- `dotnetConversion/src/Aion.GameServer/Program.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PeriodicInstanceRegistrationServiceTests.cs`
- nearest connection-registry or socket broadcast tests if an existing narrow fixture is available

Focused C# validation recipe:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PeriodicInstanceRegistrationServiceTests" --no-restore
```

Behavior under validation: live fanout adapter should send each packet in the per-player broadcast plan only to the player selected by Java's level-range filter, and close should invoke or expose the `stopRegistrationsByMaskId` side effect.

Focused Java/Maven command: not expected unless a targeted Java fixture is added. Java source review should be used for this narrow adapter slice.

Broad-validation trigger: live fanout if production socket broadcast is enabled. Start with focused service/adapter tests; do not run full project/solution validation unless focused evidence exposes wider risk or the active notes name a concrete trigger.

## Safe Candidates

- Wire live periodic registration open/close broadcast fanout.
- Add exact `SmSystemMessage` helpers for the Java scheduled periodic opening messages used by `PeriodicInstanceManager`.
- Model timed close task scheduling after broadcast fanout is in place.
- Port or model `ConquerorAndProtectorService.onLeaveMap` if supporting C# state exists.
- Review and port pet position update in delayed teleport completion.

Avoid:

- Evidence/reporting-only units.
- Full `.NET` project tests or solution builds without a documented broad-validation trigger.
- Updating `docs/PHASE-6-PROGRESS.md`.
