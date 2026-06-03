# Phase 6 Session 2471 Completion

## UOW

[Phase 6] UOW-2471: Add Vortex stop coordinator PEACE static-spawn request enrichment

## Status

Completed and validated with focused .NET tests.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/VortexService.java`
- `game-server/src/com/aionemu/gameserver/services/vortex/Invasion.java`
- `game-server/src/com/aionemu/gameserver/model/templates/spawns/vortexspawns/VortexSpawnTemplate.java`

## C# Changes

- `dotnetConversion/src/Aion.GameServer/Services/VortexStopInvasionCoordinatorService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`

## Implementation Notes

- Added `VortexStopInvasionSnapshotRequest.WithPeaceSpawns`.
- Existing PEACE spawn snapshots are preserved, and selected static PEACE snapshots are appended in order.
- Added a coordinator overload that accepts `VortexStopInvasionSnapshotRequest` plus `NpcVortexSpawnTable`.
- The overload uses `VortexPeaceSpawnSnapshotSelectionService` to select PEACE rows for the stopped location and delegates to the existing stop-planner path.
- Scope remains metadata-only. This UOW does not execute live spawn/despawn, source live invader/kisk/spawned-NPC snapshots, schedule, teleport, or dispatch.

## Tests

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `StopSnapshotRequest_AppendsSelectedPeaceSpawnsWithoutReplacingSuppliedSnapshots` | Unit | `VortexService.spawn(loc, VortexStateType.PEACE)` row selection | Request enrichment appends selected PEACE rows while preserving caller-supplied PEACE snapshots | Focused C# test validates ordering and ignores INVASION/other-location rows | Does not execute `SpawnEngine.spawnObject` |
| `StopCoordinator_StaticPeaceSpawnRequestEnrichmentFeedsPlannerWithoutLiveExecution` | Unit | `Invasion.stopInvasion -> VortexService.spawn(VortexStateType.PEACE)` | Coordinator enriches the request from static PEACE rows and feeds `SpawnPeaceNpc` planner steps without live execution | Focused C# test validates planned status, PEACE spawn count/order, and `ShouldExecuteLiveSideEffects == false` | Does not source live registries or dispatch side effects |

## Validation Decision

- Changed surface: non-live Vortex stop coordinator request enrichment and adjacent tests.
- Specific behavior/contract: Java stop invokes PEACE spawning after invasion teardown; C# must enrich stop snapshot metadata from static PEACE rows and keep the existing no-live-execution planner boundary.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests|FullyQualifiedName~StaticDataLoadingTests" --no-restore
```

- Result: Passed, 51 tests. Existing nullable/analyzer warnings were emitted.
- Focused Java/Maven command: skipped; no Java source or fixtures changed and no narrow Java Vortex lifecycle/static-data fixture exists. Java source was reviewed directly.
- Broad-validation trigger: none. This UOW adds a metadata overload/helper and tests, without changing XML shape, production spawn selection, world-map enumeration, scheduler wiring, live spawn/despawn, teleport, or dispatch.
- Broad .NET decision: skipped; focused validation built the affected project and covered the edited coordinator/request path plus adjacent static-data contracts.
- Why this scope is sufficient: the new overload only creates inert PEACE spawn snapshots and delegates to an already tested no-live-execution planner.

## Additional Hygiene

```powershell
git diff --check
```

- Passed. Git reported expected CRLF conversion warnings for edited C# files.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.vortex.Invasion.stopInvasion` | `Aion.GameServer.Services.VortexStopInvasionCoordinatorService.StopInvasion(..., VortexStopInvasionSnapshotRequest, NpcVortexSpawnTable, ...)` | Coordinator overload | Partial | Unit Tested | Partial Parity | C# now enriches stop requests with static PEACE spawn snapshots for the stopped location and plans `SpawnPeaceNpc` steps. It does not execute live side effects. |
| `com.aionemu.gameserver.services.VortexService.spawn` | `Aion.GameServer.Services.VortexStopInvasionSnapshotRequest.WithPeaceSpawns` | Request helper | Partial | Unit Tested | Partial Parity | C# appends selected PEACE spawn metadata to caller-supplied stop snapshots while preserving order. Live spawn tracking is not ported. |

## Metrics

- Total Java artifacts discovered in this UOW: 3
- Total artifacts ported or extended in this UOW: 2
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 2
- Total blocked artifacts: 0
- Conservative Phase 6 parity estimate: 41%

## Remaining Risks

- Live Vortex `spawn` and `despawn` behavior remains unported.
- Production stop snapshot sourcing for invader kisks, live invaders, and spawned NPCs is still absent.
- The new static-spawn enrichment overload requires an explicit request object; a convenience no-snapshot overload is still absent.
- Scheduler-triggered Vortex start/stop and service-level Vortex ownership remain incomplete.
