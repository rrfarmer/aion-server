# Phase 6 Session 2472 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2472: Add Vortex stop static PEACE enrichment convenience path and guard coverage

## Commits Made

- `[Phase 6][UOW-2472] Add Vortex stop static PEACE guard path`

## Summary

UOW-2472 added a metadata-only `VortexStopInvasionCoordinatorService.StopInvasion(int, NpcVortexSpawnTable, ...)` convenience overload. It uses an implicit empty snapshot request, stops the runtime first, and enriches static PEACE spawn snapshots only after the Java-style active-invasion guard succeeds.

This remains partial parity. The coordinator still plans side effects instead of killing kisks, kicking players, despawning NPCs, spawning PEACE NPCs, scheduling work, teleporting players, or dispatching live behavior.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/VortexStopInvasionCoordinatorService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`
- `docs/Phase-6-Session-2472-Completion.md`
- `docs/Phase-6-Session-2472-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.services.VortexService.stopInvasion`
- `com.aionemu.gameserver.services.vortex.Invasion.stopInvasion`

## C# Artifacts Touched

- `Aion.GameServer.Services.VortexStopInvasionCoordinatorService`
- Existing adjacent: `Aion.GameServer.Services.VortexStopInvasionSnapshotRequest`
- Existing adjacent: `Aion.GameServer.Services.VortexPeaceSpawnSnapshotSelectionService`
- Existing adjacent: `Aion.GameServer.Dataholders.NpcVortexSpawnTable`

## Validation Completed

Validation target: a static-table-only stop call selects PEACE rows only after the runtime stop succeeds, and missing/repeated stops keep no-dispatch guard reports even when static PEACE rows exist.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests|FullyQualifiedName~StaticDataLoadingTests" --no-restore
```

- Passed: 53 tests.
- Existing nullable/analyzer warnings were emitted.

```powershell
git diff --check
```

- Passed. Git reported expected CRLF conversion warnings for edited C# files.

Java/Maven validation was skipped because no Java source or fixtures changed and no narrow Java Vortex lifecycle/static-data fixture exists. Java source was reviewed directly. Broad-validation trigger was `none` because this UOW only adds a metadata overload/helper and tests.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.VortexService.stopInvasion` | `Aion.GameServer.Services.VortexStopInvasionCoordinatorService.StopInvasion(int, NpcVortexSpawnTable, ...)` | Coordinator overload | Partial | Unit Tested | Partial Parity | C# exposes a static-table-only metadata path and preserves Java's early return guard by enriching PEACE spawns only after a successful stop. Live `DimensionalVortex.stop` dispatch remains unported. |
| `com.aionemu.gameserver.services.vortex.Invasion.stopInvasion` | `Aion.GameServer.Services.VortexStopInvasionCoordinatorService` | Coordinator service | Partial | Unit Tested | Partial Parity | C# plans the PEACE spawn step sequence without executing live kisk kill, invader kick, despawn, or spawn behavior. |

## Known Gaps

- Live Vortex `spawn` and `despawn` behavior remains unimplemented.
- Production stop snapshot sourcing is still absent for invader kisks, online invaders, and spawned Vortex NPCs.
- Scheduler-triggered Vortex start/stop and service-level Vortex ownership remain incomplete.
- Defender alliance lifecycle and live zone handler wiring remain incomplete.
- Full production XML coverage for Vortex spawns has not been compared against Java output.

## Remaining Risks

- Future live spawn work will need Java parity for `CustomConfig.VORTEX_ENABLED`, pool selection, `SpawnEngine.spawnObject`, and `loc.getSpawned` tracking.
- Static PEACE spawn enrichment should remain metadata-only until live spawn execution is explicitly scoped.
- The selector invocation guard is code-reviewed and indirectly covered by no-step guard assertions; there is not yet a selector test double.

## Next Recommended UOW

[Phase 6] UOW-2473: Add Vortex stop static-spawn selector invocation seam

The next smallest safe task is to introduce a tiny coordinator-level selector seam, likely by constructor injection or an internal interface, so tests can prove static PEACE spawn selection is not invoked when `VortexService.stopInvasion` would return early for missing/repeated stops. Keep the seam metadata-only and avoid broad dependency injection changes.

Safe alternative candidates:

- Add static-data fixture coverage for multiple Vortex location ids and PEACE/INVASION row ordering if production XML row ordering becomes a concern.
- Add Java-name mapping metadata for `VortexStateType` serialization if an output contract becomes relevant.

## Suggested Files To Inspect

- `game-server/src/com/aionemu/gameserver/services/VortexService.java`
- `game-server/src/com/aionemu/gameserver/services/vortex/Invasion.java`
- `dotnetConversion/src/Aion.GameServer/Services/VortexStopInvasionCoordinatorService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexStopInvasionSideEffectPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/NpcSpawnTable.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`

## Suggested Validation

For the selector invocation seam:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests" --no-restore
```

Validation target: injected or observable selector seam proves PEACE static spawn selection is skipped for missing/repeated stops and used exactly for successful static-table stop enrichment.

Java/Maven is not expected unless Java fixtures change or a narrow Java Vortex lifecycle fixture is added. Broad-validation trigger should be `none` if the UOW only adds a metadata seam/helper and tests; it becomes present if XML shape, production spawn selection, world-map enumeration, scheduler wiring, live spawn/despawn, teleport, or dispatch changes.
