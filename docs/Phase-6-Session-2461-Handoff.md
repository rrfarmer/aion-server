# Phase 6 Session 2461 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2461: Model Vortex active portal reference metadata

## Commits Made

- `[Phase 6][UOW-2461] Model Vortex active portal metadata`

## Summary

UOW-2461 added metadata-only active portal reference support to `VortexInvasionRuntime`. Runtime state and snapshots can now carry a nullable `RiftPortalState`, `SetActivePortal` and `ClearActivePortal` model Java controller assignment/clearing, and invader/defender removal results carry the active portal reference when available.

This remains partial parity. The runtime still does not spawn/despawn portals, own production `WorldNpc` lifecycle, enumerate production worlds, call the rift-entry update pipeline, or dispatch packets.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/VortexInvasionRuntime.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`
- `docs/Phase-6-Session-2461-Completion.md`
- `docs/Phase-6-Session-2461-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.services.vortex.DimensionalVortex`
- `com.aionemu.gameserver.services.vortex.Invasion`
- `com.aionemu.gameserver.services.vortex.Invasion.startInvasion`
- `com.aionemu.gameserver.services.vortex.Invasion.stopInvasion`
- `com.aionemu.gameserver.services.vortex.Invasion.kickPlayer`
- `com.aionemu.gameserver.model.vortex.VortexLocation`
- `com.aionemu.gameserver.model.vortex.VortexLocation.setActiveVortex`
- `com.aionemu.gameserver.model.vortex.VortexLocation.setVortexController`
- `com.aionemu.gameserver.services.VortexService.despawn`
- `com.aionemu.gameserver.services.rift.RiftManager.spawnRift`
- `com.aionemu.gameserver.controllers.RVController`

## C# Artifacts Touched

- `Aion.GameServer.Services.VortexInvasionRuntime`
- `Aion.GameServer.Services.VortexInvasionSnapshot`
- `Aion.GameServer.Services.VortexInvaderRemovalResult`
- `Aion.GameServer.Services.VortexDefenderRemovalResult`
- Existing adjacent: `Aion.GameServer.Services.RiftPortalState`
- Existing adjacent: `Aion.GameServer.Services.VortexRiftEntryUpdatePipelinePlanService`

## Validation Completed

Validation target: active portal metadata mirrors Java's `VortexLocation.vortexController` lifecycle enough for later removal update composition: start/set records the reference, clear removes it, and removal results preserve it without live spawn/despawn or dispatch.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests|FullyQualifiedName~VortexRiftEntryUpdatePipelinePlanServiceTests" --no-restore
```

- Passed: 18 tests.
- Existing nullable/analyzer warnings were emitted.

```powershell
git diff --check
```

- Passed.

Java/Maven validation was skipped because no Java source or fixtures changed, and the source-of-truth behavior was reviewed directly in `Invasion.startInvasion`, `VortexLocation.setVortexController`, `VortexService.despawn`, `RiftManager.spawnRift`, and `Invasion.kickPlayer`. Broad-validation trigger was `none` because this UOW only models nullable metadata and tests, without production spawn/despawn, scheduler wiring, world-map enumeration, active packet fanout, or live connection dispatch.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.vortex.Invasion.startInvasion` | `Aion.GameServer.Services.VortexInvasionRuntime.StartInvasion` | Runtime metadata | Partial | Unit Tested | Partial Parity | Start can now carry an externally supplied active portal reference. Java spawn/active-vortex side effects remain unported. |
| `com.aionemu.gameserver.model.vortex.VortexLocation.setVortexController` | `Aion.GameServer.Services.VortexInvasionRuntime.SetActivePortal` | Active portal reference metadata | Partial | Unit Tested | Partial Parity | C# models the active controller as nullable `RiftPortalState` metadata. It does not spawn or attach a live controller. |
| `com.aionemu.gameserver.services.VortexService.despawn` | `Aion.GameServer.Services.VortexInvasionRuntime.ClearActivePortal` | Active portal reference clearing | Partial | Unit Tested | Partial Parity | C# clears active portal metadata only. Java NPC deletion and spawned-list clearing remain unported in this runtime. |
| `com.aionemu.gameserver.services.vortex.Invasion.kickPlayer` | `Aion.GameServer.Services.VortexInvaderRemovalResult` / `VortexDefenderRemovalResult` | Removal result metadata | Partial | Unit Tested | Partial Parity | Removal results now carry active portal metadata for later pipeline use. Active removal flow still does not invoke the update pipeline or dispatch. |

## Known Gaps

- Runtime is not wired to active spawn/despawn results.
- Removal flow does not yet invoke `VortexRiftEntryUpdatePipelinePlanService`.
- Production world-map player enumeration remains incomplete for removal-side rift updates.
- `RiftInformer.sendRiftInfo(getWorldsList(this))` live fanout remains incomplete for Vortex removals.
- Full Vortex start/stop spawn lifecycle, defender prompt/alliance behavior, and live zone handlers remain incomplete.

## Remaining Risks

- Future active-flow wiring must keep the active portal reference, passed-player count, world target selection, player enumeration, and disabled-by-default dispatch behavior synchronized.
- Live fanout will be a broad-validation trigger if it invokes world-map player enumeration, `IGameClientConnectionRegistry`, production portal state, spawn/despawn, or scheduler behavior.
- Existing metadata now enables later composition but does not itself prove runtime packet parity.

## Next Recommended UOW

[Phase 6] UOW-2462: Compose Vortex removal update report from active metadata

The next smallest safe task is to add a disabled-by-default removal update orchestration report that consumes a `VortexInvaderRemovalResult` or `VortexDefenderRemovalResult`, externally supplied online-player snapshots, and the active portal metadata now present on the removal result. It should call the existing pipeline and then the dispatch bridge in disabled mode only, reporting whether it would be ready to dispatch. Keep it metadata/report-only; do not enumerate production worlds or enable live dispatch.

Alternative safe candidate: add explicit stop-invasion metadata to clear active portal and participant state without spawn/despawn side effects. Avoid production lifecycle wiring until spawn ownership is explicit.

## Suggested Files To Inspect

- `game-server/src/com/aionemu/gameserver/services/vortex/Invasion.java`
- `game-server/src/com/aionemu/gameserver/model/vortex/VortexLocation.java`
- `game-server/src/com/aionemu/gameserver/services/rift/RiftInformer.java`
- `dotnetConversion/src/Aion.GameServer/Services/VortexInvasionRuntime.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexRiftEntryUpdatePipelinePlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexRiftEntryUpdateCompositionDispatchBridgeService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexRiftEntryUpdatePipelinePlanServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexRiftEntryUpdateCompositionDispatchBridgeServiceTests.cs`

## Suggested Validation

For a disabled-by-default removal update orchestration report:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexRemovalRiftEntryUpdateReportServiceTests|FullyQualifiedName~VortexRiftEntryUpdatePipelinePlanServiceTests|FullyQualifiedName~VortexRiftEntryUpdateCompositionDispatchBridgeServiceTests|FullyQualifiedName~VortexLocationServiceTests" --no-restore
```

Java/Maven is not expected unless Java fixtures change or a narrow Java workflow fixture is added. Broad-validation trigger should be `none` if the UOW only composes existing metadata and disabled bridge reporting; it becomes present if production world-map enumeration, spawn/despawn, scheduler wiring, or live connection dispatch is enabled.
