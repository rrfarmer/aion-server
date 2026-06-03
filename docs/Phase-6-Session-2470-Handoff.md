# Phase 6 Session 2470 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2470: Add Vortex PEACE spawn snapshot selection metadata

## Commits Made

- `[Phase 6][UOW-2470] Add Vortex PEACE spawn selection metadata`

## Summary

UOW-2470 added `VortexPeaceSpawnSnapshotSelectionService`, a metadata-only selector that mirrors Java `VortexService.spawn(loc, VortexStateType.PEACE)` selection by reading PEACE rows for a Vortex location from `NpcVortexSpawnTable` and converting them into `VortexStopPeaceSpawnSnapshot` inputs for the existing stop-side-effect planner.

This remains partial parity. The helper does not spawn NPCs, update live spawned-object lists, schedule tasks, teleport, or dispatch.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/VortexStopInvasionSideEffectPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`
- `docs/Phase-6-Session-2470-Completion.md`
- `docs/Phase-6-Session-2470-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.services.VortexService.spawn`
- `com.aionemu.gameserver.model.templates.spawns.vortexspawns.VortexSpawnTemplate`

## C# Artifacts Touched

- `Aion.GameServer.Services.VortexPeaceSpawnSnapshotSelectionService`
- `Aion.GameServer.Services.VortexStopPeaceSpawnSnapshot`
- Existing adjacent: `Aion.GameServer.Services.VortexStopInvasionSideEffectPlanService`
- Existing adjacent: `Aion.GameServer.Dataholders.NpcVortexSpawnTable`

## Validation Completed

Validation target: metadata-only helper selects Java `VortexStateType.PEACE` static spawn rows for a Vortex location and feeds the existing stop planner without live execution.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests|FullyQualifiedName~StaticDataLoadingTests" --no-restore
```

- Passed: 49 tests.
- Existing nullable/analyzer warnings were emitted.

```powershell
git diff --check
```

- Passed. Git reported expected CRLF conversion warnings for edited C# files.

Java/Maven validation was skipped because no Java source or fixtures changed and no narrow Java Vortex lifecycle/static-data fixture exists. Java source was reviewed directly. Broad-validation trigger was `none` because this UOW only adds a helper over existing metadata and tests.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.VortexService.spawn` | `Aion.GameServer.Services.VortexPeaceSpawnSnapshotSelectionService` | Selection service | Partial | Unit Tested | Partial Parity | C# now selects PEACE static Vortex spawn rows for a location as stop-planner snapshots. It does not spawn NPCs, update `loc.getSpawned`, or call Rift/Vortex live services. |
| `com.aionemu.gameserver.model.templates.spawns.vortexspawns.VortexSpawnTemplate` | `Aion.GameServer.Services.VortexStopPeaceSpawnSnapshot` | Spawn intent DTO | Partial | Unit Tested | Partial Parity | C# conversion preserves PEACE row spawn metadata and rejects INVASION rows for stop PEACE spawn intents. Live `VortexSpawnTemplate` behavior is not ported. |

## Known Gaps

- `VortexStopInvasionCoordinatorService` does not yet consume `VortexPeaceSpawnSnapshotSelectionService`.
- Live Vortex `spawn` and `despawn` behavior remains unimplemented.
- Production stop snapshot sourcing is still absent for invader kisks, online invaders, and spawned Vortex NPCs.
- Scheduler-triggered Vortex start/stop and service-level Vortex ownership remain incomplete.
- Defender alliance lifecycle and live zone handler wiring remain incomplete.

## Remaining Risks

- Wiring static PEACE spawn selection into the coordinator should remain metadata-only until live spawn execution is explicitly scoped.
- Future live spawn work will need Java parity for `CustomConfig.VORTEX_ENABLED`, pool selection, `SpawnEngine.spawnObject`, and `loc.getSpawned` tracking.
- Full production XML coverage for Vortex spawns has not been compared against Java output.

## Next Recommended UOW

[Phase 6] UOW-2471: Add Vortex stop coordinator PEACE static-spawn request enrichment

The next smallest safe task is to add a metadata-only coordinator overload or request helper that accepts `NpcVortexSpawnTable` and enriches `VortexStopInvasionSnapshotRequest` with selected PEACE spawn snapshots through `VortexPeaceSpawnSnapshotSelectionService`. Keep the existing no-live-execution behavior; do not source invader/kisk/spawned-NPC snapshots from live registries and do not execute spawn/despawn.

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
- `dotnetConversion/tests/Aion.GameServer.Tests/StaticDataLoadingTests.cs`

## Suggested Validation

For coordinator PEACE static-spawn request enrichment:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests|FullyQualifiedName~StaticDataLoadingTests" --no-restore
```

Validation target: coordinator/request enrichment selects PEACE static Vortex rows for the stopped location, appends them to grouped stop snapshots, and preserves no-live-execution stop planning.

Java/Maven is not expected unless Java fixtures change or a narrow Java Vortex lifecycle fixture is added. Broad-validation trigger should be `none` if the UOW only adds a metadata overload/helper and tests; it becomes present if XML shape, production spawn selection, world-map enumeration, scheduler wiring, live spawn/despawn, teleport, or dispatch changes.
