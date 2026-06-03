# Phase 6 Session 2473 Completion

## UOW

[Phase 6] UOW-2473: Add Vortex stop static-spawn selector invocation seam

## Status

Completed and validated with focused .NET tests.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/VortexService.java`
- `game-server/src/com/aionemu/gameserver/services/vortex/Invasion.java`

## C# Changes

- `dotnetConversion/src/Aion.GameServer/Services/VortexStopInvasionCoordinatorService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexStopInvasionSideEffectPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`

## Implementation Notes

- Added `IVortexPeaceSpawnSnapshotSelector`.
- `VortexPeaceSpawnSnapshotSelectionService` now implements that seam without changing selection behavior.
- `VortexStopInvasionCoordinatorService` accepts an optional selector dependency and keeps method-level selector override support.
- Focused tests now inject a counting selector to prove static PEACE selection is invoked exactly once on a successful static-table stop and skipped for missing/repeated stops.
- Scope remains metadata-only. This UOW does not execute live spawn/despawn, source live invader/kisk/spawned-NPC snapshots, schedule, teleport, or dispatch.

## Tests

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `StopCoordinator_StaticPeaceSpawnTableOnlyEnrichmentFeedsPlannerWithoutLiveExecution` | Unit | `VortexService.stopInvasion -> Invasion.stopInvasion -> VortexService.spawn(PEACE)` | Selector seam is invoked exactly once after a successful stop and PEACE snapshots feed no-live-execution planner steps | Focused C# test validates selector call count/location and planner metadata | Does not execute `SpawnEngine.spawnObject` |
| `StopCoordinator_StaticPeaceSpawnTableOnlyMissingOrRepeatedStopKeepsNoDispatchGuard` | Unit | `VortexService.stopInvasion` early return when no active invasion exists | Missing/repeated stops skip static PEACE selection and keep empty guard plans | Focused C# test validates selector call count remains one across missing, stopped, repeated sequence | Does not cover Java `invasion.isFinished()` branch separately |

## Validation Decision

- Changed surface: non-live Vortex selector seam, coordinator constructor dependency, and focused tests.
- Specific behavior/contract: injected selector evidence proves PEACE static spawn selection is skipped for missing/repeated stops and used exactly for successful static-table stop enrichment.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests" --no-restore
```

- Result: Passed, 30 tests. Existing nullable/analyzer warnings were emitted.
- Focused Java/Maven command: skipped; no Java source or fixtures changed and no narrow Java Vortex lifecycle/static-data fixture exists. Java source was reviewed directly.
- Broad-validation trigger: none. This UOW adds a metadata seam/helper and tests, without changing XML shape, production spawn selection, world-map enumeration, scheduler wiring, live spawn/despawn, teleport, or dispatch.
- Broad .NET decision: skipped; focused validation built the affected project and covered the edited Vortex coordinator paths.
- Why this scope is sufficient: the new seam is inert metadata plumbing and the injected counting selector directly proves the Java stop guard ordering.

## Additional Hygiene

```powershell
git diff --check
```

- Passed. Git reported expected CRLF conversion warnings for edited C# files.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.VortexService.stopInvasion` | `Aion.GameServer.Services.VortexStopInvasionCoordinatorService` | Coordinator service | Partial | Unit Tested | Partial Parity | C# now has observable selector-order coverage showing static PEACE selection is skipped when Java would return before `invasion.stop()`. Live `DimensionalVortex.stop` dispatch remains unported. |
| `com.aionemu.gameserver.services.VortexService.spawn` | `Aion.GameServer.Services.IVortexPeaceSpawnSnapshotSelector` | Selector seam | Partial | Unit Tested | Partial Parity | C# selector seam preserves existing static PEACE row filtering and enables guard-order tests. It still produces metadata only, not spawned world objects. |

## Metrics

- Total Java artifacts discovered in this UOW: 2
- Total artifacts ported or extended in this UOW: 2
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 2
- Total blocked artifacts: 0
- Conservative Phase 6 parity estimate: 41%

## Remaining Risks

- Live Vortex `spawn` and `despawn` behavior remains unported.
- Production stop snapshot sourcing for invader kisks, live invaders, and spawned NPCs is still absent.
- Java `invasion.isFinished()` branch is not separately modeled by the current runtime metadata.
- Scheduler-triggered Vortex start/stop and service-level Vortex ownership remain incomplete.
