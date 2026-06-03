# Phase 6 Session 2476 Completion

## UOW

[Phase 6] UOW-2476: Add Vortex start side-effect plan metadata

## Status

Completed and validated with focused .NET tests.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/vortex/Invasion.java`
- `game-server/src/com/aionemu/gameserver/services/vortex/DimensionalVortex.java`
- `game-server/src/com/aionemu/gameserver/services/VortexService.java`

## C# Changes

- `dotnetConversion/src/Aion.GameServer/Services/VortexStartInvasionSideEffectPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`

## Implementation Notes

- Added non-live `VortexStartInvasionSideEffectPlanService`.
- Added ordered start step metadata for Java `Invasion.startInvasion`: `setActiveVortex`, `despawn`, `spawn(INVASION)`, `initRiftGenerator`, and `updateAlliance`.
- Added `VortexInvasionSpawnSnapshotSelectionService` for static INVASION Vortex spawn rows.
- Added start spawned-NPC and INVASION-spawn snapshots so later live work can supply production state without changing this metadata contract.
- Duplicate starts return `AlreadyStarted` guard metadata and no ordered side-effect steps.
- Scope remains metadata-only. No live spawn/despawn, rift generator, alliance, scheduler, teleport, or packet dispatch behavior is executed.

## Tests

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `StartSideEffectPlan_PreservesJavaStartOrderWithoutExecutingLiveEffects` | Unit | `Invasion.startInvasion` source review | Ordered C# start metadata preserves Java method order and stays non-live | Focused C# test validates step order, counts, INVASION spawn metadata, and `ShouldExecuteLiveSideEffects == false` | Does not execute Java side effects or compare runtime spawned objects |
| `StartSideEffectPlan_AlreadyStartedReturnsGuardMetadata` | Unit | `DimensionalVortex.start` double-start guard | Repeated starts produce no start side-effect plan | Focused C# test validates guard status and empty step list even when snapshots are supplied | Service-level active-map concurrency remains partial |
| `InvasionSpawnSelection_SelectsJavaInvasionRowsForVortexLocation` | Unit | `VortexService.spawn(loc, VortexStateType.INVASION)` source review | Static selector returns only INVASION rows for the requested Vortex location | Focused C# test validates row filtering and PEACE-row rejection | Production XML row comparison remains separate |

## Validation Decision

- Changed surface: non-live Vortex start side-effect metadata and focused tests.
- Specific behavior/contract: C# metadata mirrors Java `Invasion.startInvasion` ordering without executing live spawn/despawn/rift/alliance side effects.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests" --no-restore
```

- Result: Passed, 37 tests. Existing nullable/analyzer warnings were emitted.
- Focused Java/Maven command: skipped; no Java source or fixtures changed and no narrow Java Vortex lifecycle/static-data fixture exists. Java source was reviewed directly.
- Broad-validation trigger: none. This UOW only adds non-live metadata and tests, without changing XML shape, production spawn selection, world-map enumeration, scheduler wiring, live spawn/despawn, teleport, or dispatch.
- Broad .NET decision: skipped; focused validation built the affected project and covered the edited Vortex start metadata paths.
- Why this scope is sufficient: the new code is an inert planner and selector with no live runtime side effects.

## Additional Hygiene

```powershell
git diff --check
```

- Passed. Git reported expected CRLF conversion warnings for the edited test file.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.vortex.Invasion.startInvasion` | `Aion.GameServer.Services.VortexStartInvasionSideEffectPlanService` | Runtime planner | Partial | Unit Tested | Partial Parity | C# models Java start side-effect order as non-live metadata. Actual `setActiveVortex`, `despawn`, INVASION spawn, rift generator init, and defender alliance update execution remain unported. |
| `com.aionemu.gameserver.services.VortexService.spawn` | `Aion.GameServer.Services.VortexInvasionSpawnSnapshotSelectionService` | Static-data selector | Partial | Unit Tested | Partial Parity | C# selects INVASION Vortex spawn rows for one location id. Live `RiftManager.spawnVortex`, `RiftInformer.sendRiftsInfo`, and `SpawnEngine.spawnObject` are not executed. |
| `com.aionemu.gameserver.services.vortex.DimensionalVortex.start` | `Aion.GameServer.Services.VortexStartInvasionSideEffectPlanService.CreatePlan` | Runtime planner | Partial | Unit Tested | Partial Parity | Duplicate-start guard metadata prevents side-effect planning when `StartInvasionWithResult` reports `AlreadyStarted`. Full Java synchronized service behavior remains partial. |

## Metrics

- Total Java artifacts discovered in this UOW: 3
- Total artifacts ported or extended in this UOW: 2
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 3
- Total blocked artifacts: 0
- Conservative Phase 6 parity estimate: 41%

## Remaining Risks

- Live Vortex start execution remains unported.
- `VortexService.startInvasion` scheduling and service-level active-map ownership remain incomplete.
- `RiftManager.spawnVortex`, `RiftInformer.sendRiftsInfo`, `SpawnEngine.spawnObject`, rift generator initialization, and defender alliance mutation remain metadata-only.
- Production XML coverage for INVASION Vortex spawns has not been compared against Java runtime output.
