# Phase 6 Session 2461 Completion

## UOW

[Phase 6] UOW-2461: Model Vortex active portal reference metadata

## Status

Completed and validated with focused .NET tests.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/vortex/DimensionalVortex.java`
- `game-server/src/com/aionemu/gameserver/services/vortex/Invasion.java`
- `game-server/src/com/aionemu/gameserver/model/vortex/VortexLocation.java`
- `game-server/src/com/aionemu/gameserver/services/VortexService.java`
- `game-server/src/com/aionemu/gameserver/services/rift/RiftManager.java`
- `game-server/src/com/aionemu/gameserver/controllers/RVController.java`

## C# Changes

- `dotnetConversion/src/Aion.GameServer/Services/VortexInvasionRuntime.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`

## Implementation Notes

- Added optional active `RiftPortalState` metadata to `VortexInvasionRuntime` state and snapshots.
- `StartInvasion` can now accept an active portal reference when the caller already has one.
- Added metadata-only `SetActivePortal` and `ClearActivePortal` methods to model Java `VortexLocation.vortexController` assignment and clearing.
- Vortex invader and defender removal results now carry the active portal reference when one is present, so later rift-entry update orchestration can supply the portal to the existing pipeline.
- Scope remains metadata-only. This UOW does not spawn/despawn portals, enumerate production worlds, call dispatch, or wire active removal flow into packet fanout.

## Tests

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `StartInvasion_CanCarryActivePortalReferenceLikeJavaVortexController` | Unit | `Invasion.startInvasion` plus `RiftManager.spawnRift -> VortexLocation.setVortexController` | Runtime snapshot can carry the active portal reference | C# test validates start snapshot and stored snapshot reference identity | Does not spawn portal |
| `SetAndClearActivePortal_ModelsJavaSpawnAndDespawnControllerReference` | Unit | `RiftManager.spawnRift` and `VortexService.despawn` | Metadata-only set/clear mirrors Java controller reference lifecycle | C# test validates set, clear, and unknown-location guards | Does not call Java spawn/despawn equivalents |
| `RemoveInvaderPlayer_IncludesActivePortalMetadataForRiftEntryUpdatePipeline` | Unit | `Invasion.kickPlayer -> getVortexLocation().getVortexController().syncPassed(true)` | Invader removal result carries active portal metadata for later pipeline composition | C# test validates removal result portal reference and sync plan | Pipeline not invoked by active removal flow |
| `RemoveDefenderPlayer_IncludesActivePortalMetadataForRiftEntryUpdatePipeline` | Unit | `Invasion.kickPlayer(player, false) -> getVortexLocation().getVortexController().syncPassed(true)` | Defender removal result carries active portal metadata for later pipeline composition | C# test validates removal result portal reference and sync plan | Pipeline not invoked by active removal flow |

## Validation Decision

- Changed surface: Vortex invasion runtime metadata and focused runtime/location tests.
- Specific behavior/contract: Java active Vortex lifecycle stores the active `RVController` on `VortexLocation` after rift spawn and clears it during despawn; removal code later requires that controller to call `syncPassed(true)`. C# must carry an optional `RiftPortalState` reference without enabling live spawn/despawn or dispatch.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests|FullyQualifiedName~VortexRiftEntryUpdatePipelinePlanServiceTests" --no-restore
```

- Result: Passed, 18 tests. Existing nullable/analyzer warnings were emitted.
- Focused Java/Maven command: skipped; no Java source or fixtures changed, and the source-of-truth behavior was reviewed directly in `Invasion.startInvasion`, `VortexLocation.setVortexController`, `VortexService.despawn`, `RiftManager.spawnRift`, and `Invasion.kickPlayer`.
- Broad-validation trigger: none. This UOW models nullable metadata and tests only; it does not enable production spawn/despawn, scheduler wiring, world-map enumeration, or live connection dispatch.
- Broad .NET decision: skipped; focused validation built the affected project and covered runtime metadata plus adjacent pipeline expectations.
- Why this scope is sufficient: the change is isolated to optional metadata carried on existing runtime state and removal records.

## Additional Hygiene

```powershell
git diff --check
```

- Passed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.vortex.Invasion.startInvasion` | `Aion.GameServer.Services.VortexInvasionRuntime.StartInvasion` | Runtime metadata | Partial | Unit Tested | Partial Parity | Start can now carry an externally supplied active portal reference. Java spawn/active-vortex side effects remain unported. |
| `com.aionemu.gameserver.model.vortex.VortexLocation.setVortexController` | `Aion.GameServer.Services.VortexInvasionRuntime.SetActivePortal` | Active portal reference metadata | Partial | Unit Tested | Partial Parity | C# models the active controller as nullable `RiftPortalState` metadata. It does not spawn or attach a live controller. |
| `com.aionemu.gameserver.services.VortexService.despawn` | `Aion.GameServer.Services.VortexInvasionRuntime.ClearActivePortal` | Active portal reference clearing | Partial | Unit Tested | Partial Parity | C# clears active portal metadata only. Java NPC deletion and spawned-list clearing remain unported in this runtime. |
| `com.aionemu.gameserver.services.vortex.Invasion.kickPlayer` | `Aion.GameServer.Services.VortexInvaderRemovalResult` / `VortexDefenderRemovalResult` | Removal result metadata | Partial | Unit Tested | Partial Parity | Removal results now carry active portal metadata for later pipeline use. Active removal flow still does not invoke the update pipeline or dispatch. |

## Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported or extended in this UOW: 4
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 0
- Conservative Phase 6 parity estimate: 41%

## Remaining Risks

- Active Vortex removal flow still does not call the rift-entry update pipeline.
- Active Vortex runtime still does not spawn/despawn portals or own production `WorldNpc` lifecycle.
- Production `WorldMapInstance.forEachPlayer` enumeration remains incomplete for removal-side rift updates.
- Full Vortex start/stop spawn lifecycle remains incomplete.
- Defender alliance lifecycle and live zone handler wiring remain incomplete.
- Future production fanout will be a broad-validation trigger if it invokes world-map player enumeration, `IGameClientConnectionRegistry`, active portal state, or live spawn/despawn.
