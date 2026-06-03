# Phase 6 Session 2460 Completion

## UOW

[Phase 6] UOW-2460: Add Vortex removal rift-entry update pipeline plan

## Status

Completed and validated with focused .NET tests.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/vortex/Invasion.java`
- `game-server/src/com/aionemu/gameserver/controllers/RVController.java`
- `game-server/src/com/aionemu/gameserver/services/rift/RiftInformer.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_RIFT_ANNOUNCE.java`

## C# Changes

- `dotnetConversion/src/Aion.GameServer/Services/VortexRiftEntryUpdatePipelinePlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexRiftEntryUpdatePipelinePlanServiceTests.cs`

## Implementation Notes

- Added a non-live pipeline planner for removal-side Vortex rift-entry update metadata.
- The pipeline consumes a `VortexPassedPlayerSyncPlan`, supplied `RiftPortalState`, supplied `isMasterController` flag, and supplied online-player snapshots.
- It composes the existing entry-update packet intent, world-target plan, player-target plan, and composition plan into one bridge-ready metadata result.
- Guard states cover missing sync plan, missing portal, and no target players.
- Scope remains metadata-only. This UOW does not call the dispatch bridge, enumerate production worlds, resolve active spawned portal state, or wire active Vortex removal flow.

## Tests

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `CreatePlan_ComposesRemovalSyncIntoBridgeReadyMetadataWithoutDispatching` | Unit | `Invasion.kickPlayer -> RVController.syncPassed(true) -> RiftInformer.sendRiftInfo` | Successful removal sync composes packet, worlds, player targets, and bridge-ready metadata | C# test validates used-entry sync, world ids, target ids, and ready composition | Does not dispatch or enumerate live worlds |
| `CreatePlan_NonMasterControllerTargetsSlaveOwnerWorldOnly` | Unit | `RVController.getWorldsList` | Non-master update targets only the owner world | C# test validates slave owner world target and player filter | Runtime controller ownership remains future work |
| `CreatePlan_MissingSyncPlanStillBuildsGuardMetadataWithoutApplyingPortalSync` | Unit | Java removal must produce `syncPassed(true)` before entry update | Missing sync plan blocks bridge-ready metadata while preserving non-live target planning | C# test validates no portal used-entry update and missing-entry composition | Active removal result wiring remains future work |
| `CreatePlan_MissingPortalBlocksEntryAndWorldTargets` | Unit | `SM_RIFT_ANNOUNCE(controller, false)` requires an active controller/portal | Missing portal blocks packet intent and world targets | C# test validates missing-portal entry/world guard state | Active portal ownership remains future work |
| `CreatePlan_NoMatchingPlayersCarriesWorldMetadataButIsNotBridgeReady` | Unit | Empty `WorldMapInstance.forEachPlayer` sends no packets | No matching players preserves world metadata but blocks bridge-ready state | C# test validates no target ids and retained world ids | Production player enumeration remains future work |

## Validation Decision

- Changed surface: Vortex removal rift-entry update pipeline planner and focused pipeline tests.
- Specific behavior/contract: Java `Invasion.kickPlayer` removes passed-player state, calls `RVController.syncPassed(true)`, and `RiftInformer.sendRiftInfo(getWorldsList(this))` sends `SM_RIFT_ANNOUNCE(controller, false)` to players in target worlds; C# pipeline must compose equivalent metadata without dispatching or enumerating production worlds.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexRiftEntryUpdatePipelinePlanServiceTests|FullyQualifiedName~VortexRiftEntryUpdateCompositionDispatchBridgeServiceTests|FullyQualifiedName~VortexRiftEntryUpdateCompositionPlanServiceTests|FullyQualifiedName~VortexPassedPlayerSyncRiftEntryUpdateServiceTests|FullyQualifiedName~VortexLocationServiceTests" --no-restore
```

- Result: Passed, 27 tests. Existing nullable/analyzer warnings were emitted.
- Focused Java/Maven command: skipped; no Java source or fixtures changed, and the source-of-truth behavior was reviewed directly in `Invasion.kickPlayer`, `RVController.syncPassed(true)`, `RVController.getWorldsList`, `RiftInformer.sendRiftInfo`, and `SM_RIFT_ANNOUNCE`.
- Broad-validation trigger: none. This UOW composes existing non-live planners and tests only; it does not enable production world-map enumeration, scheduler wiring, live connection dispatch, active portal lookup, or packet primitive changes.
- Broad .NET decision: skipped; focused validation built the affected project and covered the new pipeline plus adjacent entry-update, composition, dispatch-bridge, and location surfaces.
- Why this scope is sufficient: the change is isolated to deterministic metadata composition over already tested planner services.

## Additional Hygiene

```powershell
git diff --check
```

- Passed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.vortex.Invasion.kickPlayer` | `Aion.GameServer.Services.VortexRiftEntryUpdatePipelinePlanService` plus `VortexInvasionRuntime` removal results | Removal update pipeline | Partial | Unit Tested | Partial Parity | Pipeline consumes a supplied removal sync plan and portal metadata. Active removal flow still does not resolve portal state or invoke this pipeline. |
| `com.aionemu.gameserver.controllers.RVController.syncPassed` | `Aion.GameServer.Services.VortexRiftEntryUpdatePipelinePlanService` plus existing entry-update planner | Controller/runtime update pipeline | Partial | Unit Tested | Partial Parity | Pipeline applies passed-player count through supplied `RiftPortalState` and composes update metadata. Runtime controller ownership remains unported. |
| `com.aionemu.gameserver.controllers.RVController.getWorldsList` | `Aion.GameServer.Services.VortexRiftEntryUpdateWorldTargetPlanService` through pipeline | World target selection | Partial | Unit Tested | Partial Parity | Pipeline preserves master two-world and non-master one-world target metadata. Production world lookup remains unported. |
| `com.aionemu.gameserver.services.rift.RiftInformer.sendRiftInfo` | `Aion.GameServer.Services.VortexRiftEntryUpdatePipelinePlanService` plus composition plan | Rift update fanout pipeline | Partial | Unit Tested | Partial Parity | Pipeline composes packet/world/player metadata but does not call dispatch or enumerate live worlds. |

## Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported or extended in this UOW: 4
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 0
- Conservative Phase 6 parity estimate: 41%

## Remaining Risks

- Active Vortex removal flow still does not call the pipeline.
- Active Vortex runtime does not yet own or resolve spawned `RiftPortalState`.
- Production `WorldMapInstance.forEachPlayer` enumeration remains incomplete for removal-side rift updates.
- Full Vortex start/stop spawn lifecycle remains incomplete.
- Defender alliance lifecycle and live zone handler wiring remain incomplete.
- Future production fanout will be a broad-validation trigger if it invokes world-map player enumeration, `IGameClientConnectionRegistry`, or active portal state from active removal flow.
