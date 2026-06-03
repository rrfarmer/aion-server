# Phase 6 Session 2477 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2477: Add Vortex start coordinator metadata

## Commits Made

- `[Phase 6][UOW-2477] Add Vortex start coordinator metadata`

## Summary

UOW-2477 added a non-live Vortex start coordinator. It composes the guarded runtime start result with start side-effect planning and optional static INVASION spawn enrichment. The selector only runs after the start guard succeeds, matching Java `VortexService.startInvasion` returning early when `activeInvasions` already contains the location id.

This remains partial parity. The C# runtime still does not execute live Vortex spawn/despawn, rift generator, defender alliance, scheduler, teleport, or dispatch behavior.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/VortexStartInvasionCoordinatorService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`
- `docs/Phase-6-Session-2477-Completion.md`
- `docs/Phase-6-Session-2477-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.services.VortexService.startInvasion`
- `com.aionemu.gameserver.services.vortex.DimensionalVortex.start`
- `com.aionemu.gameserver.services.VortexService.spawn`

## C# Artifacts Touched

- `Aion.GameServer.Services.VortexStartInvasionCoordinatorService`
- `Aion.GameServer.Services.VortexStartInvasionSnapshotRequest`
- `Aion.GameServer.Services.VortexStartInvasionCoordinatorReport`
- `Aion.GameServer.Services.VortexStartInvasionCoordinatorStatus`

## Validation Completed

Validation target: C# start coordinator metadata mirrors Java `VortexService.startInvasion` by invoking start-side planning only after the active-invasion guard succeeds and enriching static INVASION spawn snapshots without live execution.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests" --no-restore
```

- Passed: 39 tests.
- Existing nullable/analyzer warnings were emitted.

```powershell
git diff --check
git diff --cached --check
```

- Passed. Git reported expected CRLF conversion warnings for edited/new files.

Java/Maven validation was skipped because no Java source or fixtures changed and no narrow Java Vortex lifecycle/static-data fixture exists. Java source was reviewed directly. Broad-validation trigger was `none` because this UOW only adds non-live coordinator metadata and tests.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.VortexService.startInvasion` | `Aion.GameServer.Services.VortexStartInvasionCoordinatorService` | Runtime coordinator | Partial | Unit Tested | Partial Parity | C# composes guarded start metadata and INVASION spawn enrichment. Java schedule-stop timing and live side effects remain unported. |
| `com.aionemu.gameserver.services.vortex.DimensionalVortex.start` | `Aion.GameServer.Services.VortexStartInvasionCoordinatorReport` | Runtime coordinator report | Partial | Unit Tested | Partial Parity | Duplicate-start metadata returns `AlreadyStarted` and no side-effect plan. Full Java synchronized service behavior remains partial. |
| `com.aionemu.gameserver.services.VortexService.spawn` | `Aion.GameServer.Services.IVortexInvasionSpawnSnapshotSelector` | Static-data selector boundary | Partial | Unit Tested | Partial Parity | Selector is invoked only after successful start. Live rift informer and spawn engine calls are not executed. |

## Known Gaps

- Live Vortex `start`, `spawn`, `despawn`, and `stop` behavior remains unimplemented.
- Java scheduled stop timing from `VortexService.startInvasion` is not modeled.
- `RiftManager.spawnVortex`, `RiftInformer.sendRiftsInfo`, `SpawnEngine.spawnObject`, `initRiftGenerator`, and defender alliance mutation remain metadata-only.
- Production stop snapshot sourcing is still absent for invader kisks, online invaders, and spawned Vortex NPCs.
- Full production XML coverage for Vortex spawns has not been compared against Java output.

## Remaining Risks

- Future live start work will need parity for scheduler behavior, `CustomConfig.VORTEX_DURATION`, rift generator lookup by NPC ids `209486`/`209487`, and defender alliance creation/update.
- Java synchronization is represented only by C# runtime locks and guarded metadata so far.
- Static Vortex spawn metadata remains inert until live spawn execution is explicitly scoped.

## Next Recommended UOW

[Phase 6] UOW-2478: Add Vortex start scheduled-stop metadata

The next smallest safe task is to model the Java `VortexService.startInvasion` scheduled stop metadata without live scheduling. Add a schedule intent/report that records `ThreadPoolManager.schedule(() -> stopInvasion(id), getDuration(), TimeUnit.HOURS)`, the target location id, the duration hours source, and non-live execution status. It should only appear when the start coordinator reports `Planned`, not for `AlreadyStarted`.

Safe alternative candidates:

- Add start-plan metadata for rift generator NPC ids `209486`/`209487`.
- Add static-data fixture coverage for multiple Vortex location ids and PEACE/INVASION row ordering if production XML row ordering becomes a concern.

## Suggested Files To Inspect

- `game-server/src/com/aionemu/gameserver/services/VortexService.java`
- `game-server/src/com/aionemu/gameserver/services/vortex/DimensionalVortex.java`
- `game-server/src/com/aionemu/gameserver/services/vortex/Invasion.java`
- `dotnetConversion/src/Aion.GameServer/Services/VortexStartInvasionCoordinatorService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexInvasionRuntime.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`

## Suggested Validation

For scheduled-stop metadata:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests" --no-restore
```

Validation target: C# scheduled-stop metadata mirrors Java `VortexService.startInvasion` by planning a non-live stop schedule only after successful start and omitting it for duplicate-start guard reports.

Java/Maven is not expected unless Java fixtures change or a narrow Java Vortex lifecycle fixture is added. Broad-validation trigger should be `none` if the UOW only adds non-live schedule metadata and tests; it becomes present if real scheduler wiring or live stop dispatch is enabled.
