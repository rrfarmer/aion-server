# Phase 6 Session 2457 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2457: Add Vortex rift-entry update player-target planner

## Commits Made

- `[Phase 6][UOW-2457] Add Vortex rift entry player target planner`

## Summary

UOW-2457 added a non-live player-target planner for removal-side Vortex rift-entry updates. It consumes a `VortexRiftEntryUpdateWorldTargetPlan` plus an explicitly supplied online-player snapshot and returns target player object ids in Java world-loop order: each planned world id in order, then matching players in supplied snapshot order for that world.

This remains partial parity. The planner does not enumerate production world maps, call `IGameClientConnectionRegistry`, dispatch packets, resolve active portals from removal runtime state, or wire production fanout.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/VortexRiftEntryUpdatePlayerTargetPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexRiftEntryUpdatePlayerTargetPlanServiceTests.cs`
- `docs/Phase-6-Session-2457-Completion.md`
- `docs/Phase-6-Session-2457-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.controllers.RVController`
- `com.aionemu.gameserver.controllers.RVController.getWorldsList`
- `com.aionemu.gameserver.services.rift.RiftInformer`
- `com.aionemu.gameserver.services.rift.RiftInformer.sendRiftInfo`
- `com.aionemu.gameserver.services.rift.RiftInformer.syncRiftsState`

## C# Artifacts Touched

- `Aion.GameServer.Services.VortexRiftEntryUpdatePlayerTargetPlanService`
- `Aion.GameServer.Services.VortexRiftEntryUpdatePlayerTargetPlan`
- `Aion.GameServer.Services.VortexRiftEntryUpdateOnlinePlayerSnapshot`
- `Aion.GameServer.Services.VortexRiftEntryUpdatePlayerTargetPlanStatus`
- Existing adjacent: `Aion.GameServer.Services.VortexRiftEntryUpdateWorldTargetPlanService`
- Existing adjacent: `Aion.GameServer.Services.VortexPassedPlayerSyncRiftEntryUpdateDispatchService`
- Existing adjacent: `Aion.GameServer.Services.RiftInformerService`

## Validation Completed

Validation target: from supplied player snapshots, target ids follow Java `sendRiftInfo` order: each planned world in order, each player snapshot in order within that world, preserving duplicate world passes and avoiding live enumeration.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexRiftEntryUpdatePlayerTargetPlanServiceTests|FullyQualifiedName~VortexRiftEntryUpdateWorldTargetPlanServiceTests|FullyQualifiedName~VortexPassedPlayerSyncRiftEntryUpdateDispatchServiceTests|FullyQualifiedName~RiftInformerServiceTests" --no-restore
```

- Passed: 23 tests.
- Existing nullable/analyzer warnings were emitted.

```powershell
git diff --check
```

- Passed.

Java/Maven validation was skipped because no Java source or fixtures changed, and the source-of-truth behavior was reviewed directly in `RiftInformer.sendRiftInfo`, `RiftInformer.syncRiftsState`, and `RVController.getWorldsList`. Broad-validation trigger was `none` because this UOW added target-player metadata and tests from supplied snapshots only, without production world-map enumeration, scheduler wiring, or live connection dispatch.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.rift.RiftInformer.sendRiftInfo` | `Aion.GameServer.Services.VortexRiftEntryUpdatePlayerTargetPlanService` | Rift update target planning | Partial | Unit Tested | Partial Parity | Player-target metadata now follows Java world-loop order from supplied snapshots. Live world-map enumeration remains unported for removal-side updates. |
| `com.aionemu.gameserver.services.rift.RiftInformer.syncRiftsState` | `Aion.GameServer.Services.VortexRiftEntryUpdatePlayerTargetPlanService` | Player enumeration metadata | Partial | Unit Tested | Partial Parity | Supplied snapshots model `forEachPlayer` order within each world. C# does not yet call Java-equivalent world map instances in this removal path. |
| `com.aionemu.gameserver.controllers.RVController.getWorldsList` | `Aion.GameServer.Services.VortexRiftEntryUpdateWorldTargetPlanService` plus `VortexRiftEntryUpdatePlayerTargetPlanService` | Controller target planning | Partial | Unit Tested | Partial Parity | World-id planning is consumed by player-target planning; neither is wired to active Vortex removal runtime state. |

## Known Gaps

- Removal-side player-target planner is not wired to active Vortex removal results.
- Active Vortex runtime does not yet own or resolve spawned portal state.
- Production world-map player enumeration remains incomplete for removal-side rift updates.
- `RiftInformer.sendRiftInfo(getWorldsList(this))` live fanout remains incomplete for Vortex removals.
- Full Vortex start/stop spawn lifecycle, defender prompt/alliance behavior, and live zone handlers remain incomplete.

## Remaining Risks

- Future live fanout must keep `VortexPassedPlayerSyncPlan.PassedPlayerCount`, active `RiftPortalState.UsedEntries`, Java target-world selection, player enumeration, and `SmRiftAnnounce(portal, isMaster: false)` synchronized.
- Live fanout will be a broad-validation trigger if it invokes world-map player enumeration, `IGameClientConnectionRegistry`, or production portal state.
- Player snapshot order is caller-supplied; if production world enumeration is enabled later, revalidate ordering against C# `World`/map instance iteration.

## Next Recommended UOW

[Phase 6] UOW-2458: Compose Vortex rift-entry update plans

The next smallest safe task is to add a non-live composition service that consumes a successful `VortexPassedPlayerSyncRiftEntryUpdateResult`, a `VortexRiftEntryUpdateWorldTargetPlan`, and a `VortexRiftEntryUpdatePlayerTargetPlan`, producing one aggregate plan ready for the existing disabled-by-default dispatch adapter. Keep it metadata-only and do not call dispatch or enumerate production worlds.

Alternative safe candidate: model Vortex active portal reference metadata on start/stop state without spawning or despawning side effects. Avoid coupling it to live removal dispatch until portal ownership is explicit.

## Suggested Files To Inspect

- `game-server/src/com/aionemu/gameserver/services/rift/RiftInformer.java`
- `game-server/src/com/aionemu/gameserver/controllers/RVController.java`
- `dotnetConversion/src/Aion.GameServer/Services/VortexPassedPlayerSyncRiftEntryUpdateService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexRiftEntryUpdateWorldTargetPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexRiftEntryUpdatePlayerTargetPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexPassedPlayerSyncRiftEntryUpdateDispatchService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexPassedPlayerSyncRiftEntryUpdateServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexRiftEntryUpdateWorldTargetPlanServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexRiftEntryUpdatePlayerTargetPlanServiceTests.cs`

## Suggested Validation

For a non-live composition planner:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexRiftEntryUpdateCompositionPlanServiceTests|FullyQualifiedName~VortexPassedPlayerSyncRiftEntryUpdateServiceTests|FullyQualifiedName~VortexRiftEntryUpdateWorldTargetPlanServiceTests|FullyQualifiedName~VortexRiftEntryUpdatePlayerTargetPlanServiceTests|FullyQualifiedName~VortexPassedPlayerSyncRiftEntryUpdateDispatchServiceTests" --no-restore
```

Java/Maven is not expected unless Java fixtures change or a narrow Java composition fixture is added. Broad-validation trigger should be `none` if the UOW only composes existing metadata plans and tests; it becomes present if production world-map enumeration, scheduler wiring, or live connection dispatch is enabled.
