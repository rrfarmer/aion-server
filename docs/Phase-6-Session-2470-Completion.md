# Phase 6 Session 2470 Completion

## UOW

[Phase 6] UOW-2470: Add Vortex PEACE spawn snapshot selection metadata

## Status

Completed and validated with focused .NET tests.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/VortexService.java`
- `game-server/src/com/aionemu/gameserver/model/templates/spawns/vortexspawns/VortexSpawnTemplate.java`

## C# Changes

- `dotnetConversion/src/Aion.GameServer/Services/VortexStopInvasionSideEffectPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`

## Implementation Notes

- Added `VortexPeaceSpawnSnapshotSelectionService`.
- The selector reads `NpcVortexSpawnTable.GetSpawnsForVortexLocation(vortexLocationId, VortexStateType.Peace)` and converts selected rows to `VortexStopPeaceSpawnSnapshot` values.
- Added `VortexStopPeaceSpawnSnapshot.FromVortexSpawn`.
- The conversion rejects non-PEACE Vortex spawn rows to prevent INVASION metadata from being used as stop PEACE spawn intents.
- Scope remains metadata-only. This UOW does not call live spawn/despawn, instantiate world NPCs, schedule, teleport, or dispatch.

## Tests

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `PeaceSpawnSelection_SelectsJavaPeaceRowsForVortexLocation` | Unit | `VortexService.spawn`, `VortexSpawnTemplate.getStateType` | Selector returns only PEACE rows for the requested Vortex location and preserves row order/metadata as planner snapshots | Focused C# test validates location and state filtering | Does not execute `SpawnEngine.spawnObject` |
| `PeaceSpawnSelection_FeedsStopPlanWithoutLiveSpawnExecution` | Unit | `Invasion.stopInvasion -> VortexService.spawn(VortexStateType.PEACE)` | Selected PEACE snapshots feed the existing stop planner as `SpawnPeaceNpc` steps with no live execution | Focused C# test validates stop-plan step shape and `ShouldExecuteLiveSideEffects == false` | Does not source live spawned NPCs or execute dispatch |
| `PeaceSpawnSnapshot_RejectsInvasionVortexRows` | Unit | `VortexService.spawn` state filter | INVASION rows cannot become stop PEACE spawn snapshots | Focused C# guard test validates state mismatch rejection | Exception type/message not compared to Java because Java never calls this conversion helper |

## Validation Decision

- Changed surface: non-live Vortex selection service and adjacent stop-planner tests.
- Specific behavior/contract: Java `VortexService.spawn(loc, PEACE)` scans Vortex spawn rows for a location and selects only `VortexStateType.PEACE`; C# must produce equivalent PEACE spawn planner snapshots without live spawning.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests|FullyQualifiedName~StaticDataLoadingTests" --no-restore
```

- Result: Passed, 49 tests. Existing nullable/analyzer warnings were emitted.
- Focused Java/Maven command: skipped; no Java source or fixtures changed and no narrow Java Vortex lifecycle/static-data fixture exists. Java source was reviewed directly.
- Broad-validation trigger: none. This UOW adds a helper over existing metadata and tests, without changing XML shape, production spawn selection, world-map enumeration, scheduler wiring, live spawn/despawn, teleport, or dispatch.
- Broad .NET decision: skipped; focused validation built the affected project and covered the edited Vortex selector plus adjacent static-data and stop-planner contracts.
- Why this scope is sufficient: selected rows are inert metadata consumed by an already no-live-execution planner.

## Additional Hygiene

```powershell
git diff --check
```

- Passed. Git reported expected CRLF conversion warnings for edited C# files.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.VortexService.spawn` | `Aion.GameServer.Services.VortexPeaceSpawnSnapshotSelectionService` | Selection service | Partial | Unit Tested | Partial Parity | C# now selects PEACE static Vortex spawn rows for a location as stop-planner snapshots. It does not spawn NPCs, update `loc.getSpawned`, or call Rift/Vortex live services. |
| `com.aionemu.gameserver.model.templates.spawns.vortexspawns.VortexSpawnTemplate` | `Aion.GameServer.Services.VortexStopPeaceSpawnSnapshot` | Spawn intent DTO | Partial | Unit Tested | Partial Parity | C# conversion preserves PEACE row spawn metadata and rejects INVASION rows for stop PEACE spawn intents. Live `VortexSpawnTemplate` behavior is not ported. |

## Metrics

- Total Java artifacts discovered in this UOW: 2
- Total artifacts ported or extended in this UOW: 2
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 2
- Total blocked artifacts: 0
- Conservative Phase 6 parity estimate: 41%

## Remaining Risks

- The selector is not yet wired into `VortexStopInvasionCoordinatorService`.
- Live `VortexService.spawn(VortexStateType.PEACE|INVASION)` behavior remains unported.
- Production stop snapshot sourcing for invader kisks, live invaders, and spawned NPCs is still absent.
- Full production XML coverage for Vortex spawns has not been compared against Java output.
