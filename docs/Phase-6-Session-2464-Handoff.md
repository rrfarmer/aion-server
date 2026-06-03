# Phase 6 Session 2464 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2464: Add metadata-only stop-invasion snapshot clearing

## Commits Made

- `[Phase 6][UOW-2464] Add Vortex stop metadata clearing`

## Summary

UOW-2464 added explicit stop-invasion metadata to `VortexInvasionRuntime`. The runtime now removes the active invasion entry, returns previous and stopped snapshots, clears active portal and participant metadata, and reports guard metadata for missing or repeated stop calls.

This remains partial parity. Java stop also kills invader kisks, kicks online invaders, despawns invasion NPCs, and respawns PEACE NPCs; those production side effects remain unimplemented.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/VortexInvasionRuntime.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`
- `docs/Phase-6-Session-2464-Completion.md`
- `docs/Phase-6-Session-2464-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.services.VortexService`
- `com.aionemu.gameserver.services.VortexService.stopInvasion`
- `com.aionemu.gameserver.services.vortex.DimensionalVortex`
- `com.aionemu.gameserver.services.vortex.DimensionalVortex.stop`
- `com.aionemu.gameserver.services.vortex.Invasion`
- `com.aionemu.gameserver.services.vortex.Invasion.stopInvasion`
- `com.aionemu.gameserver.model.vortex.VortexLocation`
- `com.aionemu.gameserver.model.vortex.VortexLocation.setActiveVortex`

## C# Artifacts Touched

- `Aion.GameServer.Services.VortexInvasionRuntime`
- `Aion.GameServer.Services.VortexInvasionRuntime.StopInvasion`
- `Aion.GameServer.Services.VortexStopInvasionStatus`
- `Aion.GameServer.Services.VortexStopInvasionResult`
- Existing adjacent: `Aion.GameServer.Services.VortexInvasionSnapshot`
- Existing adjacent: `Aion.GameServer.Services.VortexRemovalRiftEntryUpdatePreviewService`

## Validation Completed

Validation target: metadata-only stop-invasion clearing models Java active-entry removal and active-vortex clearing without enabling live side effects.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests|FullyQualifiedName~VortexRemovalRiftEntryUpdatePreviewServiceTests" --no-restore
```

- Passed: 20 tests.
- Existing nullable/analyzer warnings were emitted.

```powershell
git diff --check
```

- Passed.

Java/Maven validation was skipped because no Java source or fixtures changed, and the source-of-truth behavior was reviewed directly in `VortexService.stopInvasion`, `DimensionalVortex.stop`, `Invasion.stopInvasion`, and `VortexLocation.setActiveVortex`. Broad-validation trigger was `none` because this UOW only changes deterministic in-memory metadata and focused tests, without production spawn/despawn, kisk death, scheduler wiring, world-map enumeration, or live connection dispatch.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.VortexService.stopInvasion` | `Aion.GameServer.Services.VortexInvasionRuntime.StopInvasion` | Service method | Partial | Unit Tested | Partial Parity | Active runtime entry removal and missing/repeated-stop guards are modeled. Java scheduler invocation and live stop side effects remain unported. |
| `com.aionemu.gameserver.services.vortex.DimensionalVortex.stop` | `Aion.GameServer.Services.VortexStopInvasionResult` | Lifecycle guard/result | Partial | Unit Tested | Partial Parity | Result models first stop versus no active invasion at runtime-service level. Java object's atomic finished flag is not separately represented. |
| `com.aionemu.gameserver.services.vortex.Invasion.stopInvasion` | `Aion.GameServer.Services.VortexInvasionRuntime.StopInvasion` | Lifecycle method | Partial | Unit Tested | Partial Parity | Active portal and participant metadata are cleared. Kisk death, online invader kicks, despawn, and PEACE spawn remain gaps. |
| `com.aionemu.gameserver.model.vortex.VortexLocation.setActiveVortex` | `Aion.GameServer.Services.VortexInvasionSnapshot` plus stop result | Location state metadata | Partial | Unit Tested | Partial Parity | Stop result exposes cleared snapshot and runtime `GetSnapshot` becomes null after active entry removal. Java `isActive` flag is represented implicitly by runtime entry presence. |

## Known Gaps

- Stop-invasion live side effects are not enabled.
- Runtime does not kill invader kisks or model kisk death results.
- Runtime does not enumerate online invaders and call the existing per-player kick/removal path during stop.
- Runtime does not despawn invasion NPCs or respawn PEACE NPCs.
- Scheduler-triggered stop and VortexService ownership remain incomplete.
- Defender alliance lifecycle and live zone handler wiring remain incomplete.

## Remaining Risks

- Future stop planning must preserve Java order: remove active service entry, clear active vortex, kill kisks, kick online invaders, despawn, spawn PEACE.
- Calling existing `RemoveInvaderPlayer` from stop after removing the active entry would not work without a separate supplied snapshot or a different ordering model.
- Enabling live stop behavior will cross broad-validation boundaries: world maps, NPC lifecycle, scheduler, teleport/system-message dispatch, and potentially packet fanout.

## Next Recommended UOW

[Phase 6] UOW-2465: Add metadata-only Vortex stop side-effect plan

The next smallest safe task is to add an opt-in planner that consumes `VortexStopInvasionResult` plus externally supplied invader/kisk/spawn snapshots and returns the Java stop side-effect plan in order: clear active, kill invader kisks, kick online invaders, despawn invasion spawns, and spawn PEACE state. Keep it metadata-only; do not invoke `RemoveInvaderPlayer`, do not enumerate production world maps, do not schedule tasks, do not despawn/spawn live NPCs, and do not dispatch packets.

Alternative safe candidate: add a service-level wrapper around `VortexInvasionRuntime.StopInvasion` that exposes active/missing guard metadata for a future `VortexService` port, still without live side effects.

## Suggested Files To Inspect

- `game-server/src/com/aionemu/gameserver/services/VortexService.java`
- `game-server/src/com/aionemu/gameserver/services/vortex/DimensionalVortex.java`
- `game-server/src/com/aionemu/gameserver/services/vortex/Invasion.java`
- `game-server/src/com/aionemu/gameserver/model/vortex/VortexLocation.java`
- `dotnetConversion/src/Aion.GameServer/Services/VortexInvasionRuntime.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexRemovalRiftEntryUpdatePreviewService.cs`

## Suggested Validation

For metadata-only stop side-effect planning:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests|FullyQualifiedName~VortexRemovalRiftEntryUpdatePreviewServiceTests" --no-restore
```

Validation target: planner preserves Java stop side-effect order and metadata guards while remaining opt-in and no-dispatch.

Java/Maven is not expected unless Java fixtures change or a narrow Java lifecycle fixture is added. Broad-validation trigger should be `none` if the UOW only adds metadata/planner records and focused tests; it becomes present if production spawn/despawn, kisk death, scheduler wiring, world-map enumeration, teleport/system-message dispatch, or live connection dispatch is enabled.
