# Phase 6 Session 2478 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2478: Add Vortex start scheduled-stop metadata

## Commits Made

- `[Phase 6][UOW-2478] Add Vortex start scheduled-stop metadata`

## Summary

UOW-2478 added non-live scheduled-stop metadata for Java `VortexService.startInvasion`. The new planner records the target location id, duration hours, `CustomConfig.VORTEX_DURATION` source, `TimeUnit.HOURS`, and the scheduled Java `stopInvasion` method. It only produces a schedule intent when the start coordinator reports `Planned`; duplicate starts produce guard metadata without scheduling intent.

This remains partial parity. No real scheduler task is created, and no live stop dispatch occurs.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/VortexStartScheduledStopPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`
- `docs/Phase-6-Session-2478-Completion.md`
- `docs/Phase-6-Session-2478-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.services.VortexService.startInvasion`
- `com.aionemu.gameserver.services.VortexService.getDuration`
- `com.aionemu.gameserver.configs.main.CustomConfig.VORTEX_DURATION`

## C# Artifacts Touched

- `Aion.GameServer.Services.VortexStartScheduledStopPlanService`
- `Aion.GameServer.Services.VortexStartScheduledStopPlan`
- `Aion.GameServer.Services.VortexStartScheduledStopPlanStatus`

## Validation Completed

Validation target: C# scheduled-stop metadata mirrors Java `VortexService.startInvasion` by planning a non-live stop schedule only after successful start and omitting it for duplicate-start guard reports.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests" --no-restore
```

- Passed: 41 tests.
- Existing nullable/analyzer warnings were emitted.

```powershell
git diff --check
git diff --cached --check
```

- Passed. Git reported expected CRLF conversion warnings for edited/new files.

Java/Maven validation was skipped because no Java source or fixtures changed and no narrow Java Vortex lifecycle/static-data fixture exists. Java source was reviewed directly. Broad-validation trigger was `none` because this UOW only adds non-live schedule metadata and tests.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.VortexService.startInvasion` | `Aion.GameServer.Services.VortexStartScheduledStopPlanService` | Runtime planner | Partial | Unit Tested | Partial Parity | C# models Java scheduled-stop intent after successful start. It does not create a live scheduler task or dispatch `stopInvasion`. |
| `com.aionemu.gameserver.services.VortexService.getDuration` | `Aion.GameServer.Services.VortexStartScheduledStopPlan` | Runtime metadata | Partial | Unit Tested | Partial Parity | C# records duration hours and `CustomConfig.VORTEX_DURATION` source metadata. Live config binding is not connected to Vortex scheduling. |

## Known Gaps

- Live Vortex `start`, `spawn`, `despawn`, and `stop` behavior remains unimplemented.
- Java scheduled stop timing is metadata-only; no real scheduler task exists.
- `RiftManager.spawnVortex`, `RiftInformer.sendRiftsInfo`, `SpawnEngine.spawnObject`, `initRiftGenerator`, and defender alliance mutation remain metadata-only.
- Full production XML coverage for Vortex spawns has not been compared against Java output.

## Remaining Risks

- Future live scheduling work must account for scheduler cancellation/shutdown behavior and exact `TimeUnit.HOURS` conversion.
- `CustomConfig.VORTEX_DURATION` is not yet loaded into a live C# Vortex configuration surface.
- Java synchronization is represented only by C# runtime locks and guarded metadata so far.

## Next Recommended UOW

[Phase 6] UOW-2479: Add Vortex rift generator lookup metadata

The next smallest safe task is to model Java `DimensionalVortex.initRiftGenerator` as metadata-only lookup output. Java scans spawned Vortex objects and selects NPC id `209487` or `209486` as the generator, then calls `RiftManager.getInstance().getRiftByWorldId(getVortexLocation().getHomeWorldId()).setVortexGenerator(gen)`. Add a plan that records candidate spawned NPCs, selected generator object/npc id, home world id, and no-live-execution status.

Safe alternative candidates:

- Add static-data fixture coverage for multiple Vortex location ids and PEACE/INVASION row ordering if production XML row ordering becomes a concern.
- Add defender alliance update metadata for `Invasion.updateAlliance` if player-zone snapshots become available.

## Suggested Files To Inspect

- `game-server/src/com/aionemu/gameserver/services/vortex/DimensionalVortex.java`
- `game-server/src/com/aionemu/gameserver/services/vortex/Invasion.java`
- `game-server/src/com/aionemu/gameserver/services/VortexService.java`
- `dotnetConversion/src/Aion.GameServer/Services/VortexStartInvasionSideEffectPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`

## Suggested Validation

For rift generator lookup metadata:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests" --no-restore
```

Validation target: C# rift generator metadata mirrors Java `DimensionalVortex.initRiftGenerator` by selecting only NPC id `209487` or `209486` from spawned Vortex NPC snapshots and recording the intended home-world rift-generator assignment without live RiftManager mutation.

Java/Maven is not expected unless Java fixtures change or a narrow Java Vortex lifecycle fixture is added. Broad-validation trigger should be `none` if the UOW only adds non-live rift-generator metadata and tests; it becomes present if live RiftManager mutation or spawn execution is enabled.
