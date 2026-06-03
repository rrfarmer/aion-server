# Phase 6 Session 2453 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2453: Port Vortex passed-player sync metadata

## Commits Made

- `[Phase 6][UOW-2453] Port Vortex passed sync metadata`

## Summary

UOW-2453 added removal-side metadata for Java `RVController.syncPassed(true)`. Successful invader and defender removals now return a `VortexPassedPlayerSyncPlan` with `UsePassedPlayerCount = true` and `PassedPlayerCount` equal to the remaining active `passedPlayers.size()` after cleanup.

This remains partial parity. The C# runtime does not yet resolve the live spawned Vortex `RiftPortalState`, construct removal-side `SmRiftAnnounce` packets, or dispatch `RiftInformer.sendRiftInfo` fanout.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/VortexInvasionRuntime.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`
- `docs/Phase-6-Session-2453-Completion.md`
- `docs/Phase-6-Session-2453-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.controllers.RVController`
- `com.aionemu.gameserver.controllers.RVController.syncPassed`
- `com.aionemu.gameserver.services.vortex.Invasion`
- `com.aionemu.gameserver.services.vortex.Invasion.kickPlayer`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_RIFT_ANNOUNCE`
- `com.aionemu.gameserver.services.rift.RiftInformer`

## C# Artifacts Touched

- `Aion.GameServer.Services.VortexInvasionRuntime`
- `Aion.GameServer.Services.VortexInvaderRemovalResult`
- `Aion.GameServer.Services.VortexDefenderRemovalResult`
- `Aion.GameServer.Services.VortexPassedPlayerSyncPlan`
- Existing adjacent: `Aion.GameServer.Services.RiftPortalState.SyncPassed`
- Existing adjacent: `Aion.GameServer.Network.Aion.ServerPackets.SmRiftAnnounce`

## Validation Completed

Validation target: Java removal-side `syncPassed(true)` metadata uses remaining `passedPlayers.size()`.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests|FullyQualifiedName~RiftPortalUseServiceTests|FullyQualifiedName~GamePacketTests" --no-restore
```

- Passed: 293 tests.
- Existing nullable/analyzer warnings were emitted.

```powershell
git diff --check
```

- Passed with expected LF-to-CRLF working-tree warnings.

Java/Maven validation was skipped because no Java source or fixtures changed, and the source-of-truth behavior was reviewed directly in `RVController.syncPassed(true)` and `Invasion.kickPlayer`. Broad-validation trigger was `none` because this UOW added metadata and tests only, without live socket dispatch, scheduler wiring, portal lookup, packet primitive changes, or production connection dispatch.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.controllers.RVController.syncPassed` | `Aion.GameServer.Services.VortexPassedPlayerSyncPlan` plus existing `RiftPortalState.SyncPassed` | Controller/runtime state update | Partial | Unit Tested | Partial Parity | Removal results now expose `syncPassed(true)` metadata with remaining passed-player count. Existing portal-use service mutates `RiftPortalState.UsedEntries`; removal-side live portal lookup/fanout remains unported. |
| `com.aionemu.gameserver.services.vortex.Invasion.kickPlayer` | `Aion.GameServer.Services.VortexInvasionRuntime.RemoveInvaderPlayer` and `RemoveDefenderPlayer` | Vortex participant mutation | Partial | Unit Tested | Partial Parity | Both removal paths now expose passed-player sync metadata after passed-player cleanup. Java `RiftInformer.sendRiftInfo` fanout remains incomplete. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_RIFT_ANNOUNCE` | `Aion.GameServer.Network.Aion.ServerPackets.SmRiftAnnounce` | Packet | Partial | Unit Tested | Partial Parity | Existing packet tests cover portal entry update shape; this UOW did not change packet code or add live removal fanout. |

## Known Gaps

- Removal-side `SmRiftAnnounce` packet creation is not wired to active Vortex removal results.
- `RiftInformer.sendRiftInfo(getWorldsList(this))` fanout remains incomplete for Vortex removal.
- Active Vortex runtime does not yet own or resolve spawned portal state.
- Full Vortex start/stop spawn lifecycle, defender prompt/alliance behavior, and live zone handlers remain incomplete.

## Remaining Risks

- Future removal-side rift-info fanout must keep `VortexPassedPlayerSyncPlan.PassedPlayerCount`, active `RiftPortalState.UsedEntries`, and `SmRiftAnnounce(portal, isMaster: false)` in sync.
- Live fanout will be a broad-validation trigger if it invokes `IGameClientConnectionRegistry` or production portal state.
- If start/stop lifecycle is ported before fanout, ensure active Vortex state has a stable portal reference or resolver.

## Next Recommended UOW

[Phase 6] UOW-2454: Add Vortex removal rift-entry update planner

The next smallest safe task is to add a planner that consumes `VortexPassedPlayerSyncPlan` plus a `RiftPortalState`, applies `portal.SyncPassed(usePassedPlayerCount: true, passedPlayerCount: plan.PassedPlayerCount)`, and creates a non-live `SmRiftAnnounce(portal, isMaster: false)` packet intent. Keep it as a planner/adapter only; do not broadcast to worlds yet.

Alternative safe candidate: inspect and model `VortexService.startInvasion` active state start/stop metadata without spawn/despawn side effects, but avoid full lifecycle wiring until rift-entry update planning is represented.

## Suggested Files To Inspect

- `game-server/src/com/aionemu/gameserver/controllers/RVController.java`
- `game-server/src/com/aionemu/gameserver/services/rift/RiftInformer.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_RIFT_ANNOUNCE.java`
- `dotnetConversion/src/Aion.GameServer/Services/VortexInvasionRuntime.cs`
- `dotnetConversion/src/Aion.GameServer/Services/RiftService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmRiftAnnounce.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/RiftAnnouncePacketTests.cs`

## Suggested Validation

For Vortex removal rift-entry update planning:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests|FullyQualifiedName~RiftAnnouncePacketTests|FullyQualifiedName~RiftPortalUseServiceTests" --no-restore
```

Java/Maven is not expected unless Java fixtures change or a narrow Java packet fixture is added. Broad-validation trigger should be `none` if the UOW only adds a planner/adapter and packet-intent tests; it becomes present if live world fanout, scheduler wiring, or production connection dispatch is enabled.
