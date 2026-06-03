# Phase 6 Session 2451 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2451: Add focused Vortex removal packet dispatch executor

## Commits Made

- `[Phase 6][UOW-2451] Add Vortex removal packet dispatch executor`

## Summary

UOW-2451 added an opt-in C# socket-boundary executor for the Vortex removal system-message intents produced in UOW-2450. `VortexInvaderRemovalPacketDispatchService` can send `VortexInvaderRemovalResult.SystemMessages` through `IGameClientConnectionRegistry.SendPacketToPlayerAsync` in Java `Invasion.kickPlayer` order when explicitly enabled, and records disabled/missing/failure outcomes otherwise.

This remains partial parity. The executor is not yet registered or called by a production live Vortex path, and defender/alliance behavior in Java `Invasion.kickPlayer` is still incomplete.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/VortexInvaderRemovalPacketDispatchService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexInvaderRemovalPacketDispatchServiceTests.cs`
- `docs/Phase-6-Session-2451-Completion.md`
- `docs/Phase-6-Session-2451-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.services.vortex.Invasion`
- `com.aionemu.gameserver.services.vortex.Invasion.kickPlayer`
- `com.aionemu.gameserver.utils.PacketSendUtility`
- `com.aionemu.gameserver.utils.PacketSendUtility.sendPacket`

## C# Artifacts Touched

- `Aion.GameServer.Services.VortexInvaderRemovalPacketDispatchService`
- `Aion.GameServer.Services.VortexInvaderRemovalPacketDispatchResult`
- `Aion.GameServer.Services.VortexInvaderRemovalPacketDispatchMessageResult`
- `Aion.GameServer.Services.VortexInvaderRemovalResult` (consumed only)
- `Aion.GameServer.Network.Aion.IGameClientConnectionRegistry` (consumed only)

## Validation Completed

Validation target: Java `Invasion.kickPlayer` Vortex removal system-message sends are represented by an opt-in executor that preserves direct-send recipient and order.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexInvaderRemovalPacketDispatchServiceTests|FullyQualifiedName~VortexLocationServiceTests|FullyQualifiedName~PlayerAllianceOfflineTimeoutDispatchServiceTests|FullyQualifiedName~GamePacketTests" --no-restore
```

- Passed: 291 tests.
- Existing nullable/analyzer warnings were emitted.

```powershell
git diff --check
```

- Passed.

Java/Maven validation was skipped because no Java source or fixtures changed, and the source-of-truth behavior was reviewed directly in `Invasion.kickPlayer` and `PacketSendUtility.sendPacket`. Broad-validation trigger was `none` because the executor is disabled by default and not wired into production live Vortex paths.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.vortex.Invasion.kickPlayer` | `Aion.GameServer.Services.VortexInvaderRemovalPacketDispatchService` plus `VortexInvasionRuntime.RemoveInvaderPlayer` | Vortex packet dispatch | Partial | Unit Tested | Partial Parity | Invader removal message intents can now be sent through an opt-in C# executor in Java order. Live Vortex handler wiring, defender message `1401476`, alliance removal/disband, and sync packet fanout remain incomplete. |
| `com.aionemu.gameserver.utils.PacketSendUtility.sendPacket` | `Aion.GameServer.Network.Aion.IGameClientConnectionRegistry.SendPacketToPlayerAsync` via `VortexInvaderRemovalPacketDispatchService` | Socket boundary | Partial | Unit Tested | Partial Parity | Direct send boundary is represented for Vortex removal system messages. The executor records disabled/missing-connection/failure outcomes; no real client socket validation was run. |

## Known Gaps

- Vortex removal packet dispatch is not registered or invoked from live production Vortex paths.
- Java defender kick message `1401476` and defender prompt/alliance behavior remain unported.
- Java `Invasion.kickPlayer` alliance removal/disband behavior remains incomplete in C# Vortex runtime.
- Vortex controller `syncPassed(true)` packet fanout remains unported.
- Full Vortex start/stop spawn lifecycle and live zone handler wiring remain incomplete.

## Remaining Risks

- Future live wiring must choose whether Vortex removal dispatch belongs near zone leave, stop-invasion, alliance timeout, or a dedicated Vortex service facade.
- The C# teleport helper still models only position/movement reset, not Java's full cross-world teleport packet fanout.
- If live socket dispatch is wired in a future UOW, that will be a broad-validation trigger and should start with focused dispatch/connection tests.

## Next Recommended UOW

[Phase 6] UOW-2452: Port Vortex defender kick message metadata

The next smallest safe task is to inspect Java `Invasion.kickPlayer(player, false)` and add the defender removal message `1401476` as metadata only, without attempting defender alliance prompt/alliance lifecycle. This should likely introduce a defender-removal result or a generic participant-removal result only if the Java branch can be represented cleanly without disrupting current invader timeout paths.

Alternative safe candidate: add a Vortex service facade that calls `RemoveInvaderPlayer` and then the new packet dispatch executor, but keep it disabled/not wired to live zone handlers until the intended call path is clearer.

## Suggested Files To Inspect

- `game-server/src/com/aionemu/gameserver/services/vortex/Invasion.java`
- `game-server/src/com/aionemu/gameserver/services/VortexService.java`
- `dotnetConversion/src/Aion.GameServer/Services/VortexInvasionRuntime.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexInvaderRemovalPacketDispatchService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexInvaderRemovalPacketDispatchServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`

## Suggested Validation

For defender message metadata:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests|FullyQualifiedName~VortexInvaderRemovalPacketDispatchServiceTests|FullyQualifiedName~GamePacketTests" --no-restore
```

Java/Maven is not expected unless Java fixtures change or a narrow Java packet fixture is added. Broad-validation trigger should be `none` if the UOW only adds defender metadata and packet factory tests; it becomes present if live socket dispatch or live defender alliance mutation is enabled.
