# Phase 6 Session 2477 Completion

## UOW

[Phase 6] UOW-2477: Add Vortex start coordinator metadata

## Status

Completed and validated with focused .NET tests.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/VortexService.java`
- `game-server/src/com/aionemu/gameserver/services/vortex/DimensionalVortex.java`
- `game-server/src/com/aionemu/gameserver/services/vortex/Invasion.java`

## C# Changes

- `dotnetConversion/src/Aion.GameServer/Services/VortexStartInvasionCoordinatorService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`

## Implementation Notes

- Added `VortexStartInvasionCoordinatorService` to compose `VortexInvasionRuntime.StartInvasionWithResult` with `VortexStartInvasionSideEffectPlanService`.
- Added `VortexStartInvasionSnapshotRequest` to carry optional existing spawned-NPC and INVASION spawn snapshots.
- Added optional static INVASION spawn enrichment through `IVortexInvasionSpawnSnapshotSelector`.
- Static INVASION spawn selection only runs after `StartInvasionWithResult` reports `Started`, matching Java `VortexService.startInvasion` returning before construction/start when `activeInvasions` already contains the id.
- Duplicate starts return `AlreadyStarted` coordinator reports, do not replace active runtime state, and do not execute selector or side-effect planning.
- Scope remains metadata-only. No live spawn/despawn, rift generator, alliance, scheduler, teleport, or packet dispatch behavior is executed.

## Tests

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `StartCoordinator_StaticInvasionSpawnsEnrichPlanOnlyAfterStartGuardSucceeds` | Unit | `VortexService.startInvasion`, `DimensionalVortex.start`, `Invasion.startInvasion`, `VortexService.spawn` source review | Successful start produces a planned coordinator report, enriches INVASION spawn rows, preserves non-live side-effect status, and carries active portal metadata | Focused C# test validates selector invocation, step order, selected INVASION spawn, report Java source, and no live side effects | Does not execute Java scheduler, rift, spawn engine, or alliance side effects |
| `StartCoordinator_DuplicateStartSkipsStaticSelectorAndPreservesRuntimeState` | Unit | `VortexService.startInvasion` active-map guard and `DimensionalVortex.start` double-start guard | Duplicate start returns guard report without selector invocation or runtime portal replacement | Focused C# test validates guard status, empty plan, zero selector calls, and preserved active portal | Full Java synchronized concurrency remains partial |

## Validation Decision

- Changed surface: non-live Vortex start coordinator metadata and focused tests.
- Specific behavior/contract: C# start coordinator metadata mirrors Java `VortexService.startInvasion` by invoking start-side planning and static INVASION spawn enrichment only after the active-invasion guard succeeds.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests" --no-restore
```

- Result: Passed, 39 tests. Existing nullable/analyzer warnings were emitted.
- Focused Java/Maven command: skipped; no Java source or fixtures changed and no narrow Java Vortex lifecycle/static-data fixture exists. Java source was reviewed directly.
- Broad-validation trigger: none. This UOW only adds non-live coordinator metadata and tests, without changing XML shape, production spawn selection, world-map enumeration, scheduler wiring, live spawn/despawn, teleport, or dispatch.
- Broad .NET decision: skipped; focused validation built the affected project and covered the edited Vortex start coordinator paths.
- Why this scope is sufficient: the coordinator is inert metadata composition around already tested runtime and planner surfaces.

## Additional Hygiene

```powershell
git diff --check
git diff --cached --check
```

- Passed. Git reported expected CRLF conversion warnings for edited/new files.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.VortexService.startInvasion` | `Aion.GameServer.Services.VortexStartInvasionCoordinatorService` | Runtime coordinator | Partial | Unit Tested | Partial Parity | C# composes guarded start metadata and INVASION spawn enrichment. Java schedule-stop timing and live `invasion.start()` side effects remain unported. |
| `com.aionemu.gameserver.services.vortex.DimensionalVortex.start` | `Aion.GameServer.Services.VortexStartInvasionCoordinatorReport` | Runtime coordinator report | Partial | Unit Tested | Partial Parity | Duplicate-start metadata returns `AlreadyStarted` and no side-effect plan. Full Java synchronized service behavior remains partial. |
| `com.aionemu.gameserver.services.VortexService.spawn` | `Aion.GameServer.Services.IVortexInvasionSpawnSnapshotSelector` | Static-data selector boundary | Partial | Unit Tested | Partial Parity | Selector is invoked only after successful start. Live `RiftManager.spawnVortex`, `RiftInformer.sendRiftsInfo`, and `SpawnEngine.spawnObject` are not executed. |

## Metrics

- Total Java artifacts discovered in this UOW: 3
- Total artifacts ported or extended in this UOW: 2
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 3
- Total blocked artifacts: 0
- Conservative Phase 6 parity estimate: 41%

## Remaining Risks

- Live Vortex start execution remains unported.
- Java `ThreadPoolManager.schedule(() -> stopInvasion(id), getDuration(), TimeUnit.HOURS)` is not modeled yet.
- `RiftManager.spawnVortex`, `RiftInformer.sendRiftsInfo`, `SpawnEngine.spawnObject`, rift generator initialization, and defender alliance mutation remain metadata-only.
- Production XML coverage for INVASION Vortex spawns has not been compared against Java runtime output.
