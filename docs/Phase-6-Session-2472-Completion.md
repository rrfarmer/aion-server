# Phase 6 Session 2472 Completion

## UOW

[Phase 6] UOW-2472: Add Vortex stop static PEACE enrichment convenience path and guard coverage

## Status

Completed and validated with focused .NET tests.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/VortexService.java`
- `game-server/src/com/aionemu/gameserver/services/vortex/Invasion.java`

## C# Changes

- `dotnetConversion/src/Aion.GameServer/Services/VortexStopInvasionCoordinatorService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`

## Implementation Notes

- Added a coordinator convenience overload accepting `NpcVortexSpawnTable` with an implicit `VortexStopInvasionSnapshotRequest.Empty`.
- Refactored report creation through a private helper so the static-spawn overload can stop the runtime first, then enrich PEACE snapshots only after the Java-style active-invasion guard succeeds.
- The request-plus-table overload now also performs static PEACE enrichment after a successful runtime stop.
- Scope remains metadata-only. This UOW does not execute live spawn/despawn, source live invader/kisk/spawned-NPC snapshots, schedule, teleport, or dispatch.

## Tests

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `StopCoordinator_StaticPeaceSpawnTableOnlyEnrichmentFeedsPlannerWithoutLiveExecution` | Unit | `VortexService.stopInvasion -> Invasion.stopInvasion -> VortexService.spawn(PEACE)` | Static-table-only coordinator overload selects PEACE rows after a successful stop and plans `SpawnPeaceNpc` without live execution | Focused C# test validates PEACE-only planner step shape and no-live-execution flags | Does not execute `SpawnEngine.spawnObject` |
| `StopCoordinator_StaticPeaceSpawnTableOnlyMissingOrRepeatedStopKeepsNoDispatchGuard` | Unit | `VortexService.stopInvasion` early return when no active invasion exists | Missing and repeated stops keep empty no-dispatch guard plans even when static PEACE rows are available | Focused C# test validates missing/repeated reports have no steps and no PEACE spawn count | Does not prove selector invocation count directly; code path reviewed against Java guard |

## Validation Decision

- Changed surface: non-live Vortex stop coordinator overload and adjacent tests.
- Specific behavior/contract: a static-table-only stop call selects PEACE rows only after the runtime stop succeeds, and missing/repeated stops keep no-dispatch guard reports even when PEACE rows exist.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests|FullyQualifiedName~StaticDataLoadingTests" --no-restore
```

- Result: Passed, 53 tests. Existing nullable/analyzer warnings were emitted.
- Focused Java/Maven command: skipped; no Java source or fixtures changed and no narrow Java Vortex lifecycle/static-data fixture exists. Java source was reviewed directly.
- Broad-validation trigger: none. This UOW adds a metadata overload/helper and tests, without changing XML shape, production spawn selection, world-map enumeration, scheduler wiring, live spawn/despawn, teleport, or dispatch.
- Broad .NET decision: skipped; focused validation built the affected project and covered the edited coordinator paths plus adjacent static-data contracts.
- Why this scope is sufficient: the new overload only creates inert PEACE spawn snapshots after a successful runtime stop and delegates to an already tested no-live-execution planner.

## Additional Hygiene

```powershell
git diff --check
```

- Passed. Git reported expected CRLF conversion warnings for edited C# files.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.VortexService.stopInvasion` | `Aion.GameServer.Services.VortexStopInvasionCoordinatorService.StopInvasion(int, NpcVortexSpawnTable, ...)` | Coordinator overload | Partial | Unit Tested | Partial Parity | C# now exposes a static-table-only metadata path and preserves Java's early return guard by enriching PEACE spawns only after a successful stop. Live `DimensionalVortex.stop` dispatch remains unported. |
| `com.aionemu.gameserver.services.vortex.Invasion.stopInvasion` | `Aion.GameServer.Services.VortexStopInvasionCoordinatorService` | Coordinator service | Partial | Unit Tested | Partial Parity | C# plans the PEACE spawn step sequence without executing live kisk kill, invader kick, despawn, or spawn behavior. |

## Metrics

- Total Java artifacts discovered in this UOW: 2
- Total artifacts ported or extended in this UOW: 1
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 2
- Total blocked artifacts: 0
- Conservative Phase 6 parity estimate: 41%

## Remaining Risks

- Live Vortex `spawn` and `despawn` behavior remains unported.
- Production stop snapshot sourcing for invader kisks, live invaders, and spawned NPCs is still absent.
- Selector invocation count is code-reviewed rather than proven by an injected test double because the selector service is concrete and non-live.
- Scheduler-triggered Vortex start/stop and service-level Vortex ownership remain incomplete.
