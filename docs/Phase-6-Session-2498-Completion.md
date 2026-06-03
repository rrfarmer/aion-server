# Phase 6 Session 2498 Completion

## UOW

[Phase 6] UOW-2498: Prepare Vortex startInvasion alliance update metadata

## Status

Completed and validated with focused .NET tests.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/vortex/Invasion.java`

## C# Changes

- `dotnetConversion/src/Aion.GameServer/Services/VortexStartInvasionRuntimeSnapshotCollectorService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexStartInvasionCoordinatorService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexStartInvasionSideEffectPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`

## Implementation Notes

- Added `VortexStartInvasionRuntimeSnapshotCollectorService` to snapshot pre-start spawned NPCs and current Vortex zone players.
- The collector prepares a `VortexDefenderAllianceUpdatePlan` from zone-player snapshots, preserving Java `Invasion.updateAlliance` exact defender-race filtering.
- `VortexStartInvasionSnapshotRequest` now carries optional defender-alliance update metadata alongside spawned NPC and static INVASION spawn metadata.
- The start coordinator and side-effect plan now consume prepared defender-alliance update metadata without rescanning players.
- Scope remains metadata-only. It does not mutate live active Vortex locations beyond the existing runtime start guard, send question-window packets, store request handlers, mutate alliances, mutate groups, mutate defender maps, despawn NPCs, spawn NPCs, start portals, or schedule stops.

## Tests

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `StartSnapshotCollector_PreparesRuntimeStaticRequestWithDefenderAllianceMetadata` | Unit | `Invasion.startInvasion` and `Invasion.updateAlliance` source review | Runtime/static start request includes spawned NPCs, INVASION spawns, and exact defender-race update metadata | Focused C# test validates Java-shaped inputs and disabled live alliance mutation | Does not install Java `RequestResponseHandler` callbacks |
| `StartCoordinator_PreparedRuntimeStaticRequestCarriesDefenderAllianceUpdateMetadata` | Unit | `Invasion.startInvasion` source review | Prepared start request metadata reaches the coordinator side-effect plan in Java start order | Focused C# test validates despawn/spawn counts, defender/skipped counts, preserved plan reference, and disabled live side effects | Does not execute live despawn, spawn, packet, request, alliance, or group side effects |

## Validation Decision

- Changed surface: non-live Vortex start runtime snapshot collector, start request metadata, side-effect report metadata, and focused tests.
- Specific behavior/contract: C# startInvasion metadata captures Java `startInvasion -> updateAlliance` runtime/static inputs while keeping live alliance, request, packet, spawn, despawn, scheduler, and portal side effects disabled.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests" --no-restore
```

- Result: Passed, 98 tests. Existing nullable/analyzer warnings were emitted from unrelated files.
- Focused Java/Maven command: skipped; no Java source or fixtures changed and no narrow Java Vortex start/alliance fixture exists. Java source was reviewed directly.
- Broad-validation trigger: none. This UOW only adds non-live metadata/adapters and tests, without enabling live request storage, packet dispatch, alliance mutation, group mutation, defender mutation, portal spawn, NPC despawn, NPC spawn, or scheduler dispatch.
- Broad .NET decision: skipped; focused validation built the affected project and covered the edited collector/request/coordinator path.
- Why this scope is sufficient: the new code only prepares and carries inert Java-shaped metadata for the already planned start path.

## Additional Hygiene

```powershell
git diff --check
git diff --cached --check
```

- `git diff --check` passed. Git reported expected CRLF conversion warnings for edited C# source and test files.
- `git diff --cached --check` passed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.vortex.Invasion.startInvasion` | `Aion.GameServer.Services.VortexStartInvasionRuntimeSnapshotCollectorService` | Runtime metadata adapter | Partial | Unit Tested | Partial Parity | C# prepares pre-start spawned NPCs, static INVASION spawns, and defender-alliance update metadata. Live despawn, spawn, portal, request, alliance, group, defender-map, and scheduler side effects remain disabled. |
| `com.aionemu.gameserver.services.vortex.Invasion.updateAlliance` | `Aion.GameServer.Services.VortexDefenderAllianceUpdatePlan` | Runtime metadata | Partial | Unit Tested | Partial Parity | C# preserves exact defender-race filtering from current zone players and carries the prepared plan through start coordination. Per-defender request-handler composition remains a later step. |
| `com.aionemu.gameserver.services.VortexService.spawn` | `Aion.GameServer.Services.VortexStartInvasionSnapshotRequest` | Runtime metadata | Partial | Unit Tested | Partial Parity | C# carries selected static INVASION spawn rows in the prepared request. Live spawn remains disabled. |

## Metrics

- Total Java artifacts discovered in this UOW: 3
- Total artifacts ported or extended in this UOW: 3
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 3
- Total blocked artifacts: 0
- Conservative Phase 6 parity estimate: 41%

## Remaining Risks

- Live Vortex `startInvasion` side effects remain disabled.
- Defender `RequestResponseHandler` storage, `SM_QUESTION_WINDOW` dispatch, acceptance callback, group/alliance removal, and defender participant mutation remain metadata-only.
- Prepared start requests depend on supplied runtime candidates and do not yet collect from production Vortex location/world containers automatically.
- Full production XML coverage for Vortex static INVASION spawns has not been compared against Java output.
