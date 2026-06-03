# Phase 6 Session 2494 Completion

## UOW

[Phase 6] UOW-2494: Compose Vortex stopInvasion coordinator kick-removal report

## Status

Completed and validated with focused .NET tests.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/vortex/Invasion.java`

## C# Changes

- `dotnetConversion/src/Aion.GameServer/Services/VortexStopInvasionCoordinatorService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`

## Implementation Notes

- Extended `VortexStopInvasionSnapshotRequest` with invader-alliance snapshots and passed-player snapshots needed by Java `Invasion.kickPlayer(player, true)` metadata.
- Threaded those snapshots through every stop coordinator overload, including request delegation and static PEACE spawn enrichment.
- Added report-level accessors for `OrderedKickRemovalPlans` and `HasKickRemovalPlans`, forwarding the side-effect planner metadata added in UOW-2493.
- Preserved Java stop ordering and no-live-execution guard. This UOW only reports intent; it does not enable packet dispatch, teleport, alliance mutation, participant mutation, passed-player mutation, sync writes, Kisk death, despawn, or spawn.

## Tests

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `StopSnapshotRequest_AppendsSelectedPeaceSpawnsWithoutReplacingSuppliedSnapshots` | Unit | `Invasion.stopInvasion` source review | Static PEACE enrichment preserves supplied alliance and passed-player snapshots | Focused C# test validates request enrichment keeps the extra stop-time kick/removal inputs | Metadata only; no production adapter |
| `StopCoordinator_ComposesRuntimeStopAndSideEffectPlanWithoutLiveExecution` | Unit | `Invasion.stopInvasion` and `Invasion.kickPlayer` source review | Direct coordinator overload reports per-invader kick/removal metadata while stopping runtime state | Focused C# test validates report-level kick/removal plan, Java message ids, passed-player sync count, and disabled live side effects | Supplied snapshots only |
| `StopCoordinator_SnapshotRequestDelegatesToExistingStopPlanPath` | Unit | `Invasion.stopInvasion` and `Invasion.kickPlayer` source review | Snapshot request path forwards alliance and passed-player metadata to the stop planner | Focused C# test validates request-delegated kick/removal metadata and no-live flags | Supplied snapshots only |
| `StopCoordinator_StaticPeaceSpawnRequestEnrichmentFeedsPlannerWithoutLiveExecution` | Unit | `Invasion.stopInvasion` source review | Static PEACE enrichment path keeps stop-time kick/removal metadata and appends PEACE spawns | Focused C# test validates kick/removal report plus PEACE spawn ordering after enrichment | Supplied snapshots and static table only |

## Validation Decision

- Changed surface: non-live Vortex stop coordinator/report metadata and focused tests.
- Specific behavior/contract: C# stop coordinator metadata reports Java-shaped stop ordering and per-online-invader kick/removal intent, including snapshot-request and static PEACE enrichment paths, while preserving disabled live side effects.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests" --no-restore
```

- Result: Passed, 90 tests. Existing nullable/analyzer warnings were emitted from unrelated files.
- Focused Java/Maven command: skipped; no Java source or fixtures changed and no narrow Java Vortex stop/removal fixture exists. Java source was reviewed directly.
- Broad-validation trigger: none. This UOW only threads non-live metadata/reporting and tests, without enabling live packet dispatch, teleport, alliance mutation, participant mutation, passed-player mutation, sync writes, Kisk death, despawn, spawn, scheduler dispatch, or zone-player/Kisk map mutation.
- Broad .NET decision: skipped; focused validation built the affected project and covered the edited coordinator/report paths.
- Why this scope is sufficient: the new code is inert coordinator metadata over supplied stop snapshots and existing side-effect plans.

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
| `com.aionemu.gameserver.services.vortex.Invasion.stopInvasion` | `Aion.GameServer.Services.VortexStopInvasionCoordinatorService` | Runtime coordinator | Partial | Unit Tested | Partial Parity | C# coordinator reports Java stop order and kick/removal metadata through direct, request, and static PEACE enrichment paths. Live side effects remain disabled. |
| `com.aionemu.gameserver.services.vortex.Invasion.kickPlayer(Player, boolean=true)` | `Aion.GameServer.Services.VortexStopInvasionCoordinatorReport` | Runtime report | Partial | Unit Tested | Partial Parity | C# exposes per-online-invader kick/removal plans from stop reports. Live packet, teleport, alliance, participant, passed-player, and sync mutation remain disabled. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAlliance` | `Aion.GameServer.Services.VortexStopInvasionSnapshotRequest` | Runtime metadata | Partial | Unit Tested | Partial Parity | C# request accepts supplied invader-alliance snapshots needed by Java kickPlayer branches. Full live alliance behavior remains absent. |
| `com.aionemu.gameserver.model.vortex.VortexStateType.PEACE` | `Aion.GameServer.Services.VortexStopInvasionSnapshotRequest.WithPeaceSpawns` | Runtime metadata | Partial | Unit Tested | Partial Parity | C# static PEACE enrichment preserves kick/removal snapshots while appending PEACE spawn rows. Live spawn remains disabled. |

## Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported or extended in this UOW: 2
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 0
- Conservative Phase 6 parity estimate: 41%

## Remaining Risks

- Live side effects remain disabled across packet dispatch, teleport, alliance mutation, participant mutation, passed-player mutation, sync writes, Kisk death, despawn, and spawn.
- Reported kick/removal metadata depends on supplied invader-alliance and passed-player snapshots.
- Production adapters still need real Vortex location, player, alliance, passed-player, Kisk, despawn, and spawn inputs before live use.
