# Phase 6 Session 2473 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2473: Add Vortex stop static-spawn selector invocation seam

## Commits Made

- `[Phase 6][UOW-2473] Add Vortex stop selector invocation seam`

## Summary

UOW-2473 added `IVortexPeaceSpawnSnapshotSelector` and wired it into `VortexStopInvasionCoordinatorService` as an optional constructor dependency. The existing `VortexPeaceSpawnSnapshotSelectionService` implements the seam, so default behavior remains unchanged.

Focused tests now inject a counting selector and prove static PEACE spawn selection is invoked exactly once for a successful static-table stop, while missing and repeated stops skip selection in line with Java `VortexService.stopInvasion` returning before `invasion.stop()`.

This remains partial parity. The coordinator still plans side effects instead of killing kisks, kicking players, despawning NPCs, spawning PEACE NPCs, scheduling work, teleporting players, or dispatching live behavior.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/VortexStopInvasionCoordinatorService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexStopInvasionSideEffectPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`
- `docs/Phase-6-Session-2473-Completion.md`
- `docs/Phase-6-Session-2473-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.services.VortexService.stopInvasion`
- `com.aionemu.gameserver.services.VortexService.spawn`
- `com.aionemu.gameserver.services.vortex.Invasion.stopInvasion`

## C# Artifacts Touched

- `Aion.GameServer.Services.VortexStopInvasionCoordinatorService`
- `Aion.GameServer.Services.IVortexPeaceSpawnSnapshotSelector`
- `Aion.GameServer.Services.VortexPeaceSpawnSnapshotSelectionService`
- Existing adjacent: `Aion.GameServer.Dataholders.NpcVortexSpawnTable`

## Validation Completed

Validation target: injected or observable selector seam proves PEACE static spawn selection is skipped for missing/repeated stops and used exactly for successful static-table stop enrichment.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests" --no-restore
```

- Passed: 30 tests.
- Existing nullable/analyzer warnings were emitted.

```powershell
git diff --check
```

- Passed. Git reported expected CRLF conversion warnings for edited C# files.

Java/Maven validation was skipped because no Java source or fixtures changed and no narrow Java Vortex lifecycle/static-data fixture exists. Java source was reviewed directly. Broad-validation trigger was `none` because this UOW only adds a metadata seam/helper and tests.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.VortexService.stopInvasion` | `Aion.GameServer.Services.VortexStopInvasionCoordinatorService` | Coordinator service | Partial | Unit Tested | Partial Parity | C# has observable selector-order coverage showing static PEACE selection is skipped when Java would return before `invasion.stop()`. Live `DimensionalVortex.stop` dispatch remains unported. |
| `com.aionemu.gameserver.services.VortexService.spawn` | `Aion.GameServer.Services.IVortexPeaceSpawnSnapshotSelector` | Selector seam | Partial | Unit Tested | Partial Parity | C# selector seam preserves existing static PEACE row filtering and enables guard-order tests. It still produces metadata only, not spawned world objects. |

## Known Gaps

- Live Vortex `spawn` and `despawn` behavior remains unimplemented.
- Production stop snapshot sourcing is still absent for invader kisks, online invaders, and spawned Vortex NPCs.
- Java `invasion.isFinished()` early return is not separately modeled by the current runtime metadata.
- Scheduler-triggered Vortex start/stop and service-level Vortex ownership remain incomplete.
- Defender alliance lifecycle and live zone handler wiring remain incomplete.

## Remaining Risks

- Future live spawn work will need Java parity for `CustomConfig.VORTEX_ENABLED`, pool selection, `SpawnEngine.spawnObject`, and `loc.getSpawned` tracking.
- Static PEACE spawn enrichment should remain metadata-only until live spawn execution is explicitly scoped.
- Introducing the selector interface is a small API expansion; no production service registration was changed in this UOW.

## Next Recommended UOW

[Phase 6] UOW-2474: Add Vortex finished-invasion stop guard metadata

The next smallest safe task is to model Java `VortexService.stopInvasion`'s second guard, `invasion == null || invasion.isFinished()`, in the C# runtime/coordinator metadata. Add a focused way to mark an active runtime invasion as finished before stop, then prove stop removes the active entry but returns a no-dispatch guard report without PEACE selection or side-effect planning.

Safe alternative candidates:

- Add static-data fixture coverage for multiple Vortex location ids and PEACE/INVASION row ordering if production XML row ordering becomes a concern.
- Add Java-name mapping metadata for `VortexStateType` serialization if an output contract becomes relevant.

## Suggested Files To Inspect

- `game-server/src/com/aionemu/gameserver/services/VortexService.java`
- `game-server/src/com/aionemu/gameserver/services/vortex/DimensionalVortex.java`
- `game-server/src/com/aionemu/gameserver/services/vortex/Invasion.java`
- `dotnetConversion/src/Aion.GameServer/Services/VortexInvasionRuntime.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexStopInvasionCoordinatorService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexStopInvasionSideEffectPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`

## Suggested Validation

For finished-invasion guard metadata:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests" --no-restore
```

Validation target: C# runtime/coordinator metadata mirrors Java `VortexService.stopInvasion` by returning no-dispatch guard output when the removed active invasion is already finished, and static PEACE selection is not invoked.

Java/Maven is not expected unless Java fixtures change or a narrow Java Vortex lifecycle fixture is added. Broad-validation trigger should be `none` if the UOW only adds non-live metadata and tests; it becomes present if XML shape, production spawn selection, world-map enumeration, scheduler wiring, live spawn/despawn, teleport, or dispatch changes.
