# Phase 6 Session 2459 Completion

## UOW

[Phase 6] UOW-2459: Add Vortex rift-entry update composition dispatch bridge

## Status

Completed and validated with focused .NET tests.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/controllers/RVController.java`
- `game-server/src/com/aionemu/gameserver/services/rift/RiftInformer.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_RIFT_ANNOUNCE.java`

## C# Changes

- `dotnetConversion/src/Aion.GameServer/Services/VortexRiftEntryUpdateCompositionDispatchBridgeService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexRiftEntryUpdateCompositionDispatchBridgeServiceTests.cs`

## Implementation Notes

- Added a disabled-by-default bridge from a ready `VortexRiftEntryUpdateCompositionPlan` to the existing `VortexPassedPlayerSyncRiftEntryUpdateDispatchService`.
- Disabled bridge state records the Java send boundary without calling the dispatch adapter.
- Enabled bridge state delegates the composed entry-update packet intent and target player ids to the existing dispatch adapter.
- Missing composition and not-ready composition states do not cross the socket boundary.
- Scope remains opt-in and non-live by default. This UOW does not enumerate production worlds, wire active Vortex removal flow, or enable dispatch unless the bridge is explicitly constructed with `enabled: true`.

## Tests

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `DispatchAsync_DisabledBridgeDoesNotCallDispatchAdapter` | Unit | `RiftInformer.syncRiftsState -> PacketSendUtility.sendPacket` boundary | Disabled bridge records ready targets without calling dispatch | C# guard test validates no registry send happens by default | Does not send live packets |
| `DispatchAsync_EnabledBridgeDelegatesReadyCompositionToExistingDispatchAdapter` | Unit | `PacketSendUtility.sendPacket(player, SM_RIFT_ANNOUNCE)` | Enabled bridge delegates the packet intent and target ids to the existing adapter | C# dispatch test validates packet identity and target order | Dispatch remains opt-in test wiring |
| `DispatchAsync_EnabledBridgeSurfacesMissingRegistryFromDispatchAdapter` | Unit | Java send boundary requires a live player socket | Missing registry result from adapter is surfaced by the bridge | C# test validates delegated missing-registry metadata | No production registry wiring |
| `DispatchAsync_MissingOrNotReadyCompositionDoesNotCallDispatchAdapter` | Unit | Java send requires generated packet and target players | Missing or not-ready composition does not call dispatch | C# guard test validates no registry sends | Active removal flow remains future work |

## Validation Decision

- Changed surface: Vortex rift-entry update composition dispatch bridge and focused bridge tests.
- Specific behavior/contract: Java `RiftInformer.syncRiftsState(player, packets)` sends `SM_RIFT_ANNOUNCE` through `PacketSendUtility`; C# bridge must keep that socket boundary disabled by default and delegate only explicit ready metadata to the existing dispatch adapter when enabled.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexRiftEntryUpdateCompositionDispatchBridgeServiceTests|FullyQualifiedName~VortexRiftEntryUpdateCompositionPlanServiceTests|FullyQualifiedName~VortexPassedPlayerSyncRiftEntryUpdateDispatchServiceTests|FullyQualifiedName~VortexPassedPlayerSyncRiftEntryUpdateServiceTests" --no-restore
```

- Result: Passed, 19 tests. Existing nullable/analyzer warnings were emitted.
- Focused Java/Maven command: skipped; no Java source or fixtures changed, and the source-of-truth behavior was reviewed directly in `RVController.syncPassed(true)`, `RiftInformer.sendRiftInfo`, `RiftInformer.syncRiftsState`, and `SM_RIFT_ANNOUNCE`.
- Broad-validation trigger: none. This UOW bridges existing metadata to a disabled-by-default dispatch adapter and tests only; it does not enable production world-map enumeration, scheduler wiring, live connection dispatch in active flow, portal lookup, or packet primitive changes.
- Broad .NET decision: skipped; focused validation built the affected project and covered the new bridge plus adjacent composition, dispatch, and packet-intent surfaces.
- Why this scope is sufficient: the change is isolated to an opt-in adapter bridge over already tested metadata and dispatch services.

## Additional Hygiene

```powershell
git diff --check
```

- Passed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.controllers.RVController.syncPassed` | `Aion.GameServer.Services.VortexRiftEntryUpdateCompositionDispatchBridgeService` plus existing planners/adapter | Controller/runtime update dispatch bridge | Partial | Unit Tested | Partial Parity | Bridge can hand coherent packet/target metadata to the adapter when enabled. Active removal runtime wiring remains unported. |
| `com.aionemu.gameserver.services.rift.RiftInformer.sendRiftInfo` | `Aion.GameServer.Services.VortexRiftEntryUpdateCompositionDispatchBridgeService` | Rift update fanout dispatch bridge | Partial | Unit Tested | Partial Parity | Bridges composed metadata to the disabled adapter; no world enumeration or live fanout. |
| `com.aionemu.gameserver.services.rift.RiftInformer.syncRiftsState` | `Aion.GameServer.Services.VortexRiftEntryUpdateCompositionDispatchBridgeService` plus `VortexPassedPlayerSyncRiftEntryUpdateDispatchService` | Player send dispatch bridge | Partial | Unit Tested | Partial Parity | Adapter path can send to explicit target ids when opt-in. Production `WorldMapInstance.forEachPlayer` remains unported. |

## Metrics

- Total Java artifacts discovered in this UOW: 3
- Total artifacts ported or extended in this UOW: 3
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 3
- Total blocked artifacts: 0
- Conservative Phase 6 parity estimate: 41%

## Remaining Risks

- Removal-side live fanout is still incomplete because active Vortex removal flow does not resolve a spawned `RiftPortalState` or enumerate production world players.
- The bridge is not wired to active Vortex removal results.
- Full Vortex start/stop spawn lifecycle remains incomplete.
- Defender alliance lifecycle and live zone handler wiring remain incomplete.
- Future production fanout will be a broad-validation trigger if it invokes world-map player enumeration, `IGameClientConnectionRegistry`, or active portal state from active removal flow.
