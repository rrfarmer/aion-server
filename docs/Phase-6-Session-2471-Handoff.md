# Phase 6 Session 2471 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2471: Add Vortex stop coordinator PEACE static-spawn request enrichment

## Commits Made

- `[Phase 6][UOW-2471] Add Vortex stop PEACE spawn enrichment`

## Summary

UOW-2471 added metadata-only PEACE static-spawn enrichment to `VortexStopInvasionCoordinatorService`. The coordinator can now accept a `VortexStopInvasionSnapshotRequest` and `NpcVortexSpawnTable`, select PEACE rows for the stopped location through `VortexPeaceSpawnSnapshotSelectionService`, append them to the request, and delegate into the existing no-live-execution stop planner.

This remains partial parity. The path does not spawn NPCs, despawn NPCs, source live registries, schedule work, teleport players, or dispatch side effects.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/VortexStopInvasionCoordinatorService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`
- `docs/Phase-6-Session-2471-Completion.md`
- `docs/Phase-6-Session-2471-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.services.vortex.Invasion.stopInvasion`
- `com.aionemu.gameserver.services.VortexService.spawn`
- `com.aionemu.gameserver.model.templates.spawns.vortexspawns.VortexSpawnTemplate`

## C# Artifacts Touched

- `Aion.GameServer.Services.VortexStopInvasionCoordinatorService`
- `Aion.GameServer.Services.VortexStopInvasionSnapshotRequest`
- Existing adjacent: `Aion.GameServer.Services.VortexPeaceSpawnSnapshotSelectionService`
- Existing adjacent: `Aion.GameServer.Dataholders.NpcVortexSpawnTable`

## Validation Completed

Validation target: coordinator/request enrichment selects PEACE static Vortex rows for the stopped location, appends them to grouped stop snapshots, and preserves no-live-execution stop planning.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests|FullyQualifiedName~StaticDataLoadingTests" --no-restore
```

- Passed: 51 tests.
- Existing nullable/analyzer warnings were emitted.

```powershell
git diff --check
```

- Passed. Git reported expected CRLF conversion warnings for edited C# files.

Java/Maven validation was skipped because no Java source or fixtures changed and no narrow Java Vortex lifecycle/static-data fixture exists. Java source was reviewed directly. Broad-validation trigger was `none` because this UOW only adds a metadata overload/helper and tests.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.vortex.Invasion.stopInvasion` | `Aion.GameServer.Services.VortexStopInvasionCoordinatorService.StopInvasion(..., VortexStopInvasionSnapshotRequest, NpcVortexSpawnTable, ...)` | Coordinator overload | Partial | Unit Tested | Partial Parity | C# enriches stop requests with static PEACE spawn snapshots for the stopped location and plans `SpawnPeaceNpc` steps. It does not execute live side effects. |
| `com.aionemu.gameserver.services.VortexService.spawn` | `Aion.GameServer.Services.VortexStopInvasionSnapshotRequest.WithPeaceSpawns` | Request helper | Partial | Unit Tested | Partial Parity | C# appends selected PEACE spawn metadata to caller-supplied stop snapshots while preserving order. Live spawn tracking is not ported. |

## Known Gaps

- Live Vortex `spawn` and `despawn` behavior remains unimplemented.
- Production stop snapshot sourcing is still absent for invader kisks, online invaders, and spawned Vortex NPCs.
- There is no convenience coordinator overload for static PEACE enrichment with an implicit empty snapshot request.
- Scheduler-triggered Vortex start/stop and service-level Vortex ownership remain incomplete.
- Defender alliance lifecycle and live zone handler wiring remain incomplete.

## Remaining Risks

- Future live spawn work will need Java parity for `CustomConfig.VORTEX_ENABLED`, pool selection, `SpawnEngine.spawnObject`, and `loc.getSpawned` tracking.
- Static PEACE spawn enrichment should remain metadata-only until live spawn execution is explicitly scoped.
- Full production XML coverage for Vortex spawns has not been compared against Java output.

## Next Recommended UOW

[Phase 6] UOW-2472: Add Vortex stop static PEACE enrichment convenience path and guard coverage

The next smallest safe task is to add a coordinator convenience overload that accepts `NpcVortexSpawnTable` with an implicit `VortexStopInvasionSnapshotRequest.Empty`, then add focused tests showing missing/repeated stops still return no-dispatch guard reports even when static PEACE rows are available. Keep the path metadata-only; do not source invader/kisk/spawned-NPC snapshots from live registries and do not execute spawn/despawn.

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

For the convenience path and guard coverage:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests|FullyQualifiedName~StaticDataLoadingTests" --no-restore
```

Validation target: a static-table-only stop call selects PEACE rows only when the runtime stop succeeds, and missing/repeated stops keep no-dispatch guard reports even when static PEACE rows exist.

Java/Maven is not expected unless Java fixtures change or a narrow Java Vortex lifecycle fixture is added. Broad-validation trigger should be `none` if the UOW only adds a metadata overload/helper and tests; it becomes present if XML shape, production spawn selection, world-map enumeration, scheduler wiring, live spawn/despawn, teleport, or dispatch changes.
