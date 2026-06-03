# Phase 6 Session 2476 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2476: Add Vortex start side-effect plan metadata

## Commits Made

- `[Phase 6][UOW-2476] Add Vortex start side-effect plan metadata`

## Summary

UOW-2476 added non-live metadata for Java `Invasion.startInvasion`. The new start-side planner preserves Java's method order: `setActiveVortex`, `despawn`, `spawn(INVASION)`, `initRiftGenerator`, and `updateAlliance`. It also includes an INVASION static spawn selector and guard behavior for duplicate starts.

This remains partial parity. The C# runtime still does not execute live Vortex spawn/despawn, rift generator, defender alliance, scheduler, teleport, or dispatch behavior.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/VortexStartInvasionSideEffectPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`
- `docs/Phase-6-Session-2476-Completion.md`
- `docs/Phase-6-Session-2476-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.services.vortex.Invasion.startInvasion`
- `com.aionemu.gameserver.services.vortex.DimensionalVortex.start`
- `com.aionemu.gameserver.services.VortexService.spawn`

## C# Artifacts Touched

- `Aion.GameServer.Services.VortexStartInvasionSideEffectPlanService`
- `Aion.GameServer.Services.VortexStartInvasionSideEffectPlan`
- `Aion.GameServer.Services.VortexStartInvasionSideEffectStep`
- `Aion.GameServer.Services.VortexInvasionSpawnSnapshotSelectionService`
- `Aion.GameServer.Services.VortexStartInvasionSpawnSnapshot`
- `Aion.GameServer.Services.VortexStartSpawnedNpcSnapshot`

## Validation Completed

Validation target: C# metadata mirrors Java `Invasion.startInvasion` ordering without executing live spawn/despawn/rift/alliance side effects.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests" --no-restore
```

- Passed: 37 tests.
- Existing nullable/analyzer warnings were emitted.

```powershell
git diff --check
```

- Passed. Git reported expected CRLF conversion warnings for the edited test file.

Java/Maven validation was skipped because no Java source or fixtures changed and no narrow Java Vortex lifecycle/static-data fixture exists. Java source was reviewed directly. Broad-validation trigger was `none` because this UOW only adds non-live metadata and tests.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.vortex.Invasion.startInvasion` | `Aion.GameServer.Services.VortexStartInvasionSideEffectPlanService` | Runtime planner | Partial | Unit Tested | Partial Parity | C# models Java start side-effect order as non-live metadata. Actual live execution remains unported. |
| `com.aionemu.gameserver.services.VortexService.spawn` | `Aion.GameServer.Services.VortexInvasionSpawnSnapshotSelectionService` | Static-data selector | Partial | Unit Tested | Partial Parity | C# selects INVASION Vortex spawn rows for one location id. Live rift informer and spawn engine calls are not executed. |
| `com.aionemu.gameserver.services.vortex.DimensionalVortex.start` | `Aion.GameServer.Services.VortexStartInvasionSideEffectPlanService.CreatePlan` | Runtime planner | Partial | Unit Tested | Partial Parity | Duplicate-start guard metadata prevents side-effect planning after `AlreadyStarted`. Full Java synchronized service behavior remains partial. |

## Known Gaps

- Live Vortex `start`, `spawn`, `despawn`, and `stop` behavior remains unimplemented.
- Service-level `VortexService.startInvasion` active-map ownership and scheduler behavior remain incomplete.
- `RiftManager.spawnVortex`, `RiftInformer.sendRiftsInfo`, `SpawnEngine.spawnObject`, `initRiftGenerator`, and defender alliance mutation remain metadata-only.
- Production stop snapshot sourcing is still absent for invader kisks, online invaders, and spawned Vortex NPCs.
- Full production XML coverage for Vortex spawns has not been compared against Java output.

## Remaining Risks

- Future live start work will need parity for the exact Java sequencing around `setActiveVortex`, `VortexService.despawn`, `VortexService.spawn(INVASION)`, rift generator lookup by NPC ids `209486`/`209487`, and defender alliance creation/update.
- Java synchronization is represented only by C# runtime locks and guarded metadata so far.
- Static Vortex spawn metadata remains inert until live spawn execution is explicitly scoped.

## Next Recommended UOW

[Phase 6] UOW-2477: Add Vortex start coordinator metadata

The next smallest safe task is to add a non-live start coordinator that combines `VortexInvasionRuntime.StartInvasionWithResult`, `VortexStartInvasionSideEffectPlanService`, and optional static INVASION spawn enrichment. It should call the INVASION spawn selector only after the start guard succeeds, return duplicate-start guard reports without replacing runtime state, and keep `ShouldExecuteLiveSideEffects == false`.

Safe alternative candidates:

- Add static-data fixture coverage for multiple Vortex location ids and PEACE/INVASION row ordering if production XML row ordering becomes a concern.
- Add start-plan metadata for rift generator NPC ids `209486`/`209487` if generator lookup becomes the next target.

## Suggested Files To Inspect

- `game-server/src/com/aionemu/gameserver/services/VortexService.java`
- `game-server/src/com/aionemu/gameserver/services/vortex/DimensionalVortex.java`
- `game-server/src/com/aionemu/gameserver/services/vortex/Invasion.java`
- `dotnetConversion/src/Aion.GameServer/Services/VortexInvasionRuntime.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexStartInvasionSideEffectPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`

## Suggested Validation

For start coordinator metadata:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests" --no-restore
```

Validation target: C# start coordinator metadata mirrors Java `VortexService.startInvasion` by invoking start-side planning only after the active-invasion guard succeeds and enriching static INVASION spawn snapshots without live execution.

Java/Maven is not expected unless Java fixtures change or a narrow Java Vortex lifecycle fixture is added. Broad-validation trigger should be `none` if the UOW only adds non-live coordinator metadata and tests; it becomes present if XML shape, production spawn selection, world-map enumeration, scheduler wiring, live spawn/despawn, teleport, or dispatch changes.
