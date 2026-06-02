# Phase 6 Session 2357 Handoff - Open Registration Refresh Packets

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2357-Completion.md`
- `docs/Phase-6-Session-2357-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Prefer production/game parity units. Tests and docs should support concrete parity work, not become standalone evidence/reporting loops.

Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger before execution.

## Current State

Last completed UOW: UOW-2357, added Java-like open autogroup registration refresh packets after autogroup leave.

Recent production parity slices:

- Delayed teleport leave sends Java reset-warning packets before position mutation/spawn packets.
- Instance leave handler is invoked before reset-warning message selection.
- Delayed teleport leave invokes autogroup runtime cleanup after reset-warning packet selection and before teleport completion.
- Autogroup leave runtime unregisters players, removes PvP group/alliance membership, removes registry state, invokes the existing instance destroy workflow when Java `destroyIfPossible` would destroy, and now returns open-registration refresh packets when Java would call `PeriodicInstanceManager.checkAndSendOpenRegistrations(player)`.
- Production DI registers one shared `PeriodicInstanceRegistrationService` and passes it into the shared autogroup runtime.

Still not proven or not implemented:

- Java periodic registration cron scheduling and timed open/close behavior.
- Java open/close broadcast messages for periodic registrations.
- Java quick-entry queue refill after autogroup leave.
- Full forced-exit packet fanout for instance destruction.
- Java map leave callback parity: `ConquerorAndProtectorService.getInstance().onLeaveMap(player)`.
- Pet position update and same-map spawn parity in `TeleportService.SpawnTask.run`.

## Commits Made

- `ae174019b [Phase 6][UOW-2354] Add autogroup leave runtime state`
- `64d588c7b [Phase 6][UOW-2355] Wire autogroup leave into teleport`
- `daa2f2d44 [Phase 6][UOW-2356] Invoke autogroup destroy workflow`
- `[Phase 6][UOW-2357] Refresh open autogroup registrations`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Services/PeriodicInstanceRegistrationService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupInstanceLeaveRuntimeService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmAutoGroup.cs`
- `dotnetConversion/src/Aion.GameServer/Program.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PeriodicInstanceRegistrationServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/AutoGroupInstanceLeaveRuntimeServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `docs/Phase-6-Session-2357-Completion.md`
- `docs/Phase-6-Session-2357-Handoff.md`

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PeriodicInstanceRegistrationServiceTests|FullyQualifiedName~AutoGroupInstanceLeaveRuntimeServiceTests|FullyQualifiedName~GamePacketTests.SmAutoGroup_WritesJavaEntryIconOpenAndClosePayload" --no-restore
```

Result: passed 7, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Documentation hygiene:

```powershell
git diff --check
```

Result: passed. Git emitted CRLF working-copy warnings only.

Focused Java/Maven validation: skipped because no targeted Java unit fixture exists for the periodic registration refresh path; Java source review and focused C# packet/runtime tests were used as evidence.

Broad-validation trigger: packet shape and live leave packet emission.

Broad .NET decision: skipped full project/solution validation after focused packet/runtime tests passed and compiled the affected project/dependencies.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.instance.PeriodicInstanceManager.checkAndSendOpenRegistrations(Player)` | `Aion.GameServer.Services.PeriodicInstanceRegistrationService.CreateOpenRegistrationPackets(...)` | Service | Partial | Unit Tested | Partial Parity | Open-registration refresh packet planning is ported for level and cooldown checks. Cron scheduling, open/close broadcasts, and system messages remain missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_AUTO_GROUP` | `Aion.GameServer.Network.Aion.ServerPackets.SmAutoGroup` | Packet | Partial | Unit Tested | Partial Parity | Entry-icon open/close payload is covered. Existing window-zero payload remains covered. Other windows still rely on prior implementation coverage. |
| `com.aionemu.gameserver.services.AutoGroupService.onLeaveInstance(Player)` | `Aion.GameServer.Services.AutoGroupInstanceLeaveRuntimeService.OnLeaveInstance(...)` | Live Runtime Adapter | Partial | Regression Tested | Partial Parity | Runtime now returns open-registration refresh packets when Java would call `checkAndSendOpenRegistrations`. Quick-entry refill remains missing. |
| `com.aionemu.gameserver.services.instance.InstanceService.onLeaveInstance(Player)` | `Aion.GameServer.Network.Aion.GameServerConnection.SendInstanceLeaveMessageIfNeededAsync(...)` | Live Adapter | Partial | Regression Tested | Partial Parity | Delayed teleport leave now sends autogroup refresh packets returned by the runtime before teleport completion. |

## Next Sequential UOW

Recommended next production scope: model Java `PeriodicInstanceManager.openRegistration` and `closeRegistration` broadcast decisions using the existing `PeriodicInstanceRegistrationService`.

Java artifacts:

- `game-server/src/com/aionemu/gameserver/services/instance/PeriodicInstanceManager.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_AUTO_GROUP.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`
- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Services/PeriodicInstanceRegistrationService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientSocketServer.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmAutoGroup.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PeriodicInstanceRegistrationServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`

Focused C# validation recipe:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PeriodicInstanceRegistrationServiceTests|FullyQualifiedName~GamePacketTests.SmAutoGroup_WritesJavaEntryIconOpenAndClosePayload" --no-restore
```

Behavior under validation: opening/closing a periodic registration should mutate opened-registration state once, create the Java entry-icon broadcast packet shape, and keep close/open flags aligned with Java `SM_AUTO_GROUP(maskId, WND_ENTRY_ICON, isClosed)`.

Focused Java/Maven command: not expected unless a targeted Java packet fixture is added. Java source review plus focused C# packet/service tests should be enough for this narrow slice.

Broad-validation trigger: packet shape and possible broadcast fanout if live socket broadcast is enabled. Start with focused service/packet tests; do not run full project/solution validation unless focused evidence exposes wider risk or the active notes name a concrete trigger.

## Safe Candidates

- Model `PeriodicInstanceManager.openRegistration` and `closeRegistration` broadcast plans.
- Wire periodic registration open/close broadcast to `GameClientSocketServer.BroadcastToWorldAsync` if the fanout can stay narrow.
- Port or model `ConquerorAndProtectorService.onLeaveMap` if supporting C# state exists.
- Review and port pet position update in delayed teleport completion.
- Review same-map delayed teleport spawn behavior.

Avoid:

- Evidence/reporting-only units.
- Full `.NET` project tests or solution builds without a documented broad-validation trigger.
- Updating `docs/PHASE-6-PROGRESS.md`.
