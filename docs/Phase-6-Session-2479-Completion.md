# Phase 6 Session 2479 Completion

## UOW

[Phase 6] UOW-2479: Add Vortex rift generator lookup metadata

## Status

Completed and validated with focused .NET tests.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/vortex/DimensionalVortex.java`
- `game-server/src/com/aionemu/gameserver/services/vortex/Invasion.java`

## C# Changes

- `dotnetConversion/src/Aion.GameServer/Services/VortexRiftGeneratorLookupPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`

## Implementation Notes

- Added non-live `VortexRiftGeneratorLookupPlanService`.
- Java source correction: current `DimensionalVortex.initRiftGenerator` does not call `setVortexGenerator`; it scans spawned objects for NPC id `209487` or `209486` and attaches a `DeathObserver` that calls `VortexService.stopInvasion(locationId)`.
- C# metadata preserves Java's last-match behavior because Java overwrites `gen` for every matching spawned NPC and does not break.
- Missing generator produces metadata with Java exception message `No generator was found in loc:{id}` instead of throwing in this non-live planner.
- Scope remains metadata-only. No live observe-controller mutation, RiftManager mutation, or stop dispatch is executed.

## Tests

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `RiftGeneratorLookupPlan_SelectsLastJavaGeneratorNpcAndPlansDeathObserver` | Unit | `DimensionalVortex.initRiftGenerator` source review | C# selects only NPC ids `209487`/`209486`, preserves Java last-match behavior, records home world id, and disables live observer mutation | Focused C# test validates candidate list, selected generator, death-observer stop intent, and Java source | Does not attach a real `DeathObserver` |
| `RiftGeneratorLookupPlan_MissingGeneratorRecordsJavaExceptionMetadata` | Unit | `DimensionalVortex.initRiftGenerator` source review | Missing generator rows record Java exception metadata | Focused C# test validates missing status, empty candidate list, exception message, and no live stop intent | Non-live planner records metadata instead of throwing `NullPointerException` |

## Validation Decision

- Changed surface: non-live Vortex rift-generator lookup metadata and focused tests.
- Specific behavior/contract: C# rift-generator metadata mirrors Java `DimensionalVortex.initRiftGenerator` by selecting NPC id `209487` or `209486`, preserving last-match behavior, and recording the death-observer stop intent without live observer mutation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests" --no-restore
```

- Result: First run failed due to a missing C# namespace import in the new planner. After fixing it, the same command passed with 43 tests. Existing nullable/analyzer warnings were emitted.
- Focused Java/Maven command: skipped; no Java source or fixtures changed and no narrow Java Vortex lifecycle/static-data fixture exists. Java source was reviewed directly.
- Broad-validation trigger: none. This UOW only adds non-live rift-generator metadata and tests, without enabling live RiftManager mutation or spawn execution.
- Broad .NET decision: skipped; focused validation built the affected project and covered the edited Vortex rift-generator metadata paths.
- Why this scope is sufficient: the new code is an inert lookup planner over supplied spawned-NPC snapshots.

## Additional Hygiene

```powershell
git diff --check
git diff --cached --check
```

- Passed. Git reported expected CRLF conversion warnings for edited/new files.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.vortex.DimensionalVortex.initRiftGenerator` | `Aion.GameServer.Services.VortexRiftGeneratorLookupPlanService` | Runtime planner | Partial | Unit Tested | Partial Parity | C# models generator lookup and death-observer stop intent as metadata. It does not attach a live observer or call `VortexService.stopInvasion` on NPC death. |
| `com.aionemu.gameserver.services.vortex.DimensionalVortex` | `Aion.GameServer.Services.VortexRiftGeneratorLookupPlan` | Runtime metadata | Partial | Unit Tested | Partial Parity | C# records Java missing-generator exception text. The non-live planner does not throw `NullPointerException`. |

## Metrics

- Total Java artifacts discovered in this UOW: 2
- Total artifacts ported or extended in this UOW: 1
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 2
- Total blocked artifacts: 0
- Conservative Phase 6 parity estimate: 41%

## Remaining Risks

- Live generator observer attachment remains unported.
- NPC death-driven Vortex stop dispatch remains unimplemented.
- Production spawned-NPC sourcing for generator lookup is still absent.
- Defender alliance mutation remains metadata-only/unported.
