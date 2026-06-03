# Phase 6 Session 2457 Completion

## UOW

[Phase 6] UOW-2457: Add Vortex rift-entry update player-target planner

## Status

Completed and validated with focused .NET tests.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/rift/RiftInformer.java`
- `game-server/src/com/aionemu/gameserver/controllers/RVController.java`

## C# Changes

- `dotnetConversion/src/Aion.GameServer/Services/VortexRiftEntryUpdatePlayerTargetPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexRiftEntryUpdatePlayerTargetPlanServiceTests.cs`

## Implementation Notes

- Added a non-live player-target planner for removal-side Vortex rift-entry updates.
- The planner consumes `VortexRiftEntryUpdateWorldTargetPlan` plus an explicitly supplied online-player snapshot.
- Target player object ids are produced by iterating planned world ids first and supplied player snapshots second, matching Java `sendRiftInfo -> syncRiftsState(worldId) -> forEachPlayer`.
- Duplicate world ids are preserved, so matching players are repeated for each Java world-loop pass.
- Scope remains metadata-only. This UOW does not enumerate production world maps, call `IGameClientConnectionRegistry`, or dispatch packets.

## Tests

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `CreatePlan_MasterWorldTargetsPlayersInJavaWorldLoopOrder` | Unit | `RiftInformer.sendRiftInfo` and `syncRiftsState(int, packets)` | Master world plan targets players world-by-world and preserves snapshot order inside each world | C# test validates exact player-id order | Uses supplied snapshots, not live world maps |
| `CreatePlan_NonMasterWorldTargetsOnlyPlayersInOwnerWorld` | Unit | `RVController.getWorldsList` plus `RiftInformer.syncRiftsState` | Non-master world plan targets only players in the owner/slave world | C# test validates one-world filtering | Non-master runtime portal ownership is not wired |
| `CreatePlan_PreservesDuplicateWorldTargetsLikeJavaSendRiftInfoLoop` | Unit | Java loops raw `int[] worlds` without de-duplication | Duplicate world ids repeat matching players | C# test validates repeated target ids | Same-world portal pairs are not known to occur in production data |
| `CreatePlan_NoMatchingPlayersStillProducesPlannedEmptyTargetList` | Unit | Empty world player iteration has no send targets | Planned worlds with no matching snapshots produce an empty target list | C# test validates empty target result without changing status to a failure | Does not verify Java empty-map runtime behavior |
| `CreatePlan_GuardsMissingPlanNoWorldTargetsAndNoOnlinePlayers` | Unit | `sendRiftInfo` requires world ids and online player iteration | Missing world plan, no world targets, and no online-player snapshot are explicit no-send states | C# guard test validates metadata-only states | Live world enumeration remains future work |

## Validation Decision

- Changed surface: Vortex rift-entry update player-target planner and focused planner tests.
- Specific behavior/contract: Java `RiftInformer.sendRiftInfo` loops world ids in order, then `WorldMapInstance.forEachPlayer` sends generated packets to players in that world; C# metadata planning must preserve the world-loop order from supplied snapshots without live enumeration.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexRiftEntryUpdatePlayerTargetPlanServiceTests|FullyQualifiedName~VortexRiftEntryUpdateWorldTargetPlanServiceTests|FullyQualifiedName~VortexPassedPlayerSyncRiftEntryUpdateDispatchServiceTests|FullyQualifiedName~RiftInformerServiceTests" --no-restore
```

- Result: Passed, 23 tests. Existing nullable/analyzer warnings were emitted.
- Focused Java/Maven command: skipped; no Java source or fixtures changed, and the source-of-truth behavior was reviewed directly in `RiftInformer.sendRiftInfo`, `RiftInformer.syncRiftsState`, and `RVController.getWorldsList`.
- Broad-validation trigger: none. This UOW adds target-player metadata and tests from supplied snapshots only; it does not enable production world-map enumeration, scheduler wiring, live connection dispatch, portal lookup, or packet primitive changes.
- Broad .NET decision: skipped; focused validation built the affected project and covered the new planner plus adjacent world-target, dispatch, and informer surfaces.
- Why this scope is sufficient: the change is isolated to deterministic target-id metadata derived from supplied world ids and supplied player snapshots.

## Additional Hygiene

```powershell
git diff --check
```

- Passed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.rift.RiftInformer.sendRiftInfo` | `Aion.GameServer.Services.VortexRiftEntryUpdatePlayerTargetPlanService` | Rift update target planning | Partial | Unit Tested | Partial Parity | Player-target metadata now follows Java world-loop order from supplied snapshots. Live world-map enumeration remains unported for removal-side updates. |
| `com.aionemu.gameserver.services.rift.RiftInformer.syncRiftsState` | `Aion.GameServer.Services.VortexRiftEntryUpdatePlayerTargetPlanService` | Player enumeration metadata | Partial | Unit Tested | Partial Parity | Supplied snapshots model `forEachPlayer` order within each world. C# does not yet call Java-equivalent world map instances in this removal path. |
| `com.aionemu.gameserver.controllers.RVController.getWorldsList` | `Aion.GameServer.Services.VortexRiftEntryUpdateWorldTargetPlanService` plus `VortexRiftEntryUpdatePlayerTargetPlanService` | Controller target planning | Partial | Unit Tested | Partial Parity | World-id planning is consumed by player-target planning; neither is wired to active Vortex removal runtime state. |

## Metrics

- Total Java artifacts discovered in this UOW: 3
- Total artifacts ported or extended in this UOW: 3
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 3
- Total blocked artifacts: 0
- Conservative Phase 6 parity estimate: 41%

## Remaining Risks

- Removal-side live fanout is still incomplete because active Vortex removal flow does not resolve a spawned `RiftPortalState` or enumerate production world players.
- Player snapshot order is caller-supplied; Java `WorldMapInstance.forEachPlayer` runtime order has not been independently compared.
- Full Vortex start/stop spawn lifecycle remains incomplete.
- Defender alliance lifecycle and live zone handler wiring remain incomplete.
- Future production fanout will be a broad-validation trigger if it invokes world-map player enumeration, `IGameClientConnectionRegistry`, or active portal state.
