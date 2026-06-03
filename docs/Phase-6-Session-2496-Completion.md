# Phase 6 Session 2496 Completion

## UOW

[Phase 6] UOW-2496: Add Vortex stopInvasion static PEACE spawn collector composition

## Status

Completed and validated with focused .NET tests.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/vortex/Invasion.java`
- `game-server/src/com/aionemu/gameserver/services/VortexService.java`
- `game-server/src/com/aionemu/gameserver/services/vortex/DimensionalVortex.java`

## C# Changes

- `dotnetConversion/src/Aion.GameServer/Services/VortexStopInvasionRuntimeSnapshotCollectorService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`

## Implementation Notes

- Added `PrepareWithStaticPeaceSpawns` to `VortexStopInvasionRuntimeSnapshotCollectorService`.
- The method composes runtime stop/kick snapshot collection with Java-shaped static PEACE spawn selection.
- Missing runtime snapshots return the inert empty request and skip static PEACE selection, matching the stop guard shape used elsewhere in the C# coordinator path.
- Scope remains metadata-only. It does not stop the runtime, send packets, teleport players, mutate alliances, mutate participant maps, mutate passed-player maps, sync passed-player state, kill Kisks, despawn NPCs, or spawn NPCs.

## Tests

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `StopRuntimeSnapshotCollector_PreparesRuntimeSnapshotsWithStaticPeaceSpawnsWithoutLiveExecution` | Unit | `Invasion.stopInvasion`, `DimensionalVortex.spawn`, and `VortexService.spawn` source review | Runtime stop snapshots compose with static PEACE spawn selection and feed coordinator metadata | Focused C# test validates collected invader/Kisk/spawned NPC/passed-player inputs, selected PEACE spawn, Java stop ordering, kick/removal metadata, sync count, and no-live flags | Candidate objects still supplied by future adapters |
| `StopRuntimeSnapshotCollector_StaticPeacePreparationMissingSnapshotSkipsStaticSelection` | Unit | `VortexService.stopInvasion` missing-active guard source review | Missing runtime snapshot returns an empty request and does not query static PEACE spawns | Focused C# test validates no selection call and empty request | Does not exercise production runtime lookup |

## Validation Decision

- Changed surface: non-live Vortex stop request preparation metadata and focused tests.
- Specific behavior/contract: C# stopInvasion request preparation composes runtime stop/kick snapshots with Java PEACE spawn selection while preserving disabled live side effects.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests" --no-restore
```

- Result: Passed, 94 tests. Existing nullable/analyzer warnings were emitted from unrelated files.
- Focused Java/Maven command: skipped; no Java source or fixtures changed and no narrow Java Vortex stop/removal fixture exists. Java source was reviewed directly.
- Broad-validation trigger: none. This UOW only composes non-live request preparation metadata and tests, without enabling live packet dispatch, teleport, alliance mutation, participant mutation, passed-player mutation, sync writes, Kisk death, despawn, spawn, scheduler dispatch, or zone-player/Kisk map mutation.
- Broad .NET decision: skipped; focused validation built the affected project and covered the edited collector/preparation path.
- Why this scope is sufficient: the new code is an inert preparation helper over supplied snapshot, candidates, and static spawn data.

## Additional Hygiene

```powershell
git diff --check
git diff --cached --check
```

- `git diff --check` passed. Git reported expected CRLF conversion warnings for the edited source and test files.
- `git diff --cached --check` passed after staging. Git reported expected CRLF conversion warnings for edited/new files.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.vortex.Invasion.stopInvasion` | `Aion.GameServer.Services.VortexStopInvasionRuntimeSnapshotCollectorService` | Runtime collector | Partial | Unit Tested | Partial Parity | C# prepares stop-time metadata inputs plus PEACE spawn metadata. Live stop side effects remain disabled. |
| `com.aionemu.gameserver.services.vortex.DimensionalVortex.spawn` | `Aion.GameServer.Services.VortexStopInvasionRuntimeSnapshotCollectorService.PrepareWithStaticPeaceSpawns` | Runtime metadata | Partial | Unit Tested | Partial Parity | C# composes static PEACE spawn metadata only. Live spawn remains disabled. |
| `com.aionemu.gameserver.services.VortexService.spawn` | `Aion.GameServer.Services.VortexPeaceSpawnSnapshotSelectionService` via collector | Runtime metadata | Partial | Unit Tested | Partial Parity | C# selects PEACE rows for the stopped location. Full live spawn/world materialization remains absent. |
| `com.aionemu.gameserver.model.vortex.VortexStateType.PEACE` | `Aion.GameServer.Services.VortexStopPeaceSpawnSnapshot` | Runtime metadata | Partial | Unit Tested | Partial Parity | C# records PEACE spawn intent only. Live NPC spawn remains disabled. |

## Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported or extended in this UOW: 1
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 0
- Conservative Phase 6 parity estimate: 41%

## Remaining Risks

- Live side effects remain disabled across packet dispatch, teleport, alliance mutation, participant mutation, passed-player mutation, sync writes, Kisk death, despawn, and spawn.
- The preparation helper still depends on supplied candidate collections for players, invader Kisks, spawned NPCs, and alliance metadata.
- Production adapters still need to provide real Vortex location/player/world/alliance/Kisk/spawn inputs before live use.
