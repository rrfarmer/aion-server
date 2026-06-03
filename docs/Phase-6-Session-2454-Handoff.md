# Phase 6 Session 2454 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2454: Add Vortex removal rift-entry update planner

## Commits Made

- `[Phase 6][UOW-2454] Add Vortex rift entry update planner`

## Summary

UOW-2454 added a non-live planner for removal-side Vortex rift entry updates. It consumes `VortexPassedPlayerSyncPlan` plus a supplied `RiftPortalState`, applies Java `RVController.syncPassed(true)` entry-count semantics, and creates a `SmRiftAnnounce(portal, isMaster: false)` packet intent.

This remains partial parity. The planner does not resolve active spawned portal state, target worlds, or send packets through live connection dispatch.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/VortexPassedPlayerSyncRiftEntryUpdateService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexPassedPlayerSyncRiftEntryUpdateServiceTests.cs`
- `docs/Phase-6-Session-2454-Completion.md`
- `docs/Phase-6-Session-2454-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.controllers.RVController`
- `com.aionemu.gameserver.controllers.RVController.syncPassed`
- `com.aionemu.gameserver.services.vortex.Invasion`
- `com.aionemu.gameserver.services.vortex.Invasion.kickPlayer`
- `com.aionemu.gameserver.services.rift.RiftInformer`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_RIFT_ANNOUNCE`

## C# Artifacts Touched

- `Aion.GameServer.Services.VortexPassedPlayerSyncRiftEntryUpdateService`
- `Aion.GameServer.Services.VortexPassedPlayerSyncRiftEntryUpdateResult`
- `Aion.GameServer.Services.VortexPassedPlayerSyncRiftEntryUpdateStatus`
- Existing adjacent: `Aion.GameServer.Services.VortexPassedPlayerSyncPlan`
- Existing adjacent: `Aion.GameServer.Services.RiftPortalState.SyncPassed`
- Existing adjacent: `Aion.GameServer.Network.Aion.ServerPackets.SmRiftAnnounce`

## Validation Completed

Validation target: the planner applies `VortexPassedPlayerSyncPlan.PassedPlayerCount` to a vortex `RiftPortalState` and produces the Java-shaped `SM_RIFT_ANNOUNCE(controller, false)` entry-update packet, while null guards avoid mutation.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexPassedPlayerSyncRiftEntryUpdateServiceTests|FullyQualifiedName~VortexLocationServiceTests|FullyQualifiedName~RiftAnnouncePacketTests|FullyQualifiedName~RiftPortalUseServiceTests" --no-restore
```

- Passed: 30 tests.

```powershell
git diff --check
```

- Passed.

Java/Maven validation was skipped because no Java source or fixtures changed, and the source-of-truth behavior was reviewed directly in `RVController.syncPassed(true)`, `Invasion.kickPlayer`, `RiftInformer`, and `SM_RIFT_ANNOUNCE`. Broad-validation trigger was `none` because this UOW added a non-live planner/adapter and packet-intent tests only.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.controllers.RVController.syncPassed` | `Aion.GameServer.Services.VortexPassedPlayerSyncRiftEntryUpdateService` plus existing `RiftPortalState.SyncPassed` | Controller/runtime state update | Partial | Unit Tested | Partial Parity | Removal sync metadata can now be applied to a supplied portal state using Java `passedPlayers.size()` semantics. Live portal resolution remains unported. |
| `com.aionemu.gameserver.services.rift.RiftInformer.sendRiftInfo` | `Aion.GameServer.Services.VortexPassedPlayerSyncRiftEntryUpdateService` | Rift update planning | Partial | Unit Tested | Partial Parity | Planner creates the non-master `SmRiftAnnounce` packet intent but does not broadcast to world players. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_RIFT_ANNOUNCE` | `Aion.GameServer.Network.Aion.ServerPackets.SmRiftAnnounce` | Packet | Partial | Unit Tested | Partial Parity | New tests verify the removal-side entry-update packet payload for Vortex portals. |

## Known Gaps

- Removal-side planner is not wired to active Vortex removal results.
- Active Vortex runtime does not yet own or resolve spawned portal state.
- `RiftInformer.sendRiftInfo(getWorldsList(this))` targeting/fanout remains incomplete for Vortex removals.
- Full Vortex start/stop spawn lifecycle, defender prompt/alliance behavior, and live zone handlers remain incomplete.

## Remaining Risks

- Future live fanout must keep `VortexPassedPlayerSyncPlan.PassedPlayerCount`, active `RiftPortalState.UsedEntries`, and `SmRiftAnnounce(portal, isMaster: false)` synchronized.
- Live fanout will be a broad-validation trigger if it invokes `IGameClientConnectionRegistry` or production portal state.
- If Vortex start/stop lifecycle is ported before fanout, ensure active Vortex state has a stable portal reference or resolver.

## Next Recommended UOW

[Phase 6] UOW-2455: Add Vortex removal rift-entry update dispatch adapter

The next smallest safe task is to add an opt-in, disabled-by-default dispatch adapter that takes a successful `VortexPassedPlayerSyncRiftEntryUpdateResult` packet intent and sends it to explicitly supplied target connections. Keep world selection/resolution outside the adapter and avoid production fanout wiring.

Alternative safe candidate: model Vortex active portal reference metadata on start/stop state without spawning or despawning side effects. Avoid coupling it to live removal dispatch until portal ownership is explicit.

## Suggested Files To Inspect

- `game-server/src/com/aionemu/gameserver/controllers/RVController.java`
- `game-server/src/com/aionemu/gameserver/services/rift/RiftInformer.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_RIFT_ANNOUNCE.java`
- `dotnetConversion/src/Aion.GameServer/Services/VortexPassedPlayerSyncRiftEntryUpdateService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexInvaderRemovalPacketDispatchService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/RiftInformerService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmRiftAnnounce.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexPassedPlayerSyncRiftEntryUpdateServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexInvaderRemovalPacketDispatchServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/RiftAnnouncePacketTests.cs`

## Suggested Validation

For a disabled-by-default dispatch adapter:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexPassedPlayerSyncRiftEntryUpdateServiceTests|FullyQualifiedName~VortexInvaderRemovalPacketDispatchServiceTests|FullyQualifiedName~RiftAnnouncePacketTests|FullyQualifiedName~GamePacketTests" --no-restore
```

Java/Maven is not expected unless Java fixtures change or a narrow Java packet fixture is added. Broad-validation trigger should be `none` if the UOW only adds an opt-in adapter and tests; it becomes present if production world targeting, scheduler wiring, or live connection dispatch is enabled.
