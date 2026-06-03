# Phase 6 Session 2475 Completion

## UOW

[Phase 6] UOW-2475: Add Vortex start double-start guard metadata

## Status

Completed and validated with focused .NET tests.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/VortexService.java`
- `game-server/src/com/aionemu/gameserver/services/vortex/DimensionalVortex.java`
- `game-server/src/com/aionemu/gameserver/services/vortex/Invasion.java`

## C# Changes

- `dotnetConversion/src/Aion.GameServer/Services/VortexInvasionRuntime.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`

## Implementation Notes

- Added `VortexStartInvasionStatus` and `VortexStartInvasionResult`.
- Added `StartInvasionWithResult` to expose Java-style start guard metadata.
- `StartInvasion` now delegates through `StartInvasionWithResult` and returns the result snapshot for compatibility.
- Repeated starts now return `AlreadyStarted` metadata and preserve the active runtime state instead of replacing active portal metadata.
- Scope remains metadata-only. This UOW does not execute live spawn/despawn, schedule, teleport, or dispatch.

## Tests

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `StartInvasionWithResult_RepeatedStartPreservesStateLikeJavaDoubleStartGuard` | Unit | `DimensionalVortex.start` double-start guard | First start reports `Started`; repeated start reports `AlreadyStarted` and preserves active portal, invader, and defender metadata | Focused C# test validates start statuses, Java source breadcrumbs, and preserved runtime state | Does not model Java synchronization beyond runtime lock |
| `StartInvasion_RepeatedSnapshotCallPreservesActivePortalAndParticipants` | Unit | `DimensionalVortex.start` double-start guard | Existing snapshot-returning compatibility method also preserves active state on repeated calls | Focused C# test validates portal and participant preservation | Does not execute live `Invasion.startInvasion` |

## Validation Decision

- Changed surface: non-live Vortex runtime start metadata and focused tests.
- Specific behavior/contract: C# runtime metadata mirrors Java `DimensionalVortex.start` by treating repeated starts as guarded no-ops that preserve active portal and participant metadata.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests" --no-restore
```

- Result: Passed, 34 tests. Existing nullable/analyzer warnings were emitted.
- Focused Java/Maven command: skipped; no Java source or fixtures changed and no narrow Java Vortex lifecycle/static-data fixture exists. Java source was reviewed directly.
- Broad-validation trigger: none. This UOW only adds non-live metadata and tests, without changing XML shape, production spawn selection, world-map enumeration, scheduler wiring, live spawn/despawn, teleport, or dispatch.
- Broad .NET decision: skipped; focused validation built the affected project and covered the edited Vortex runtime start path.
- Why this scope is sufficient: the new branch only affects Vortex start metadata and guarded no-op behavior.

## Additional Hygiene

```powershell
git diff --check
```

- Passed. Git reported expected CRLF conversion warnings for edited C# files.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.vortex.DimensionalVortex.start` | `Aion.GameServer.Services.VortexInvasionRuntime.StartInvasionWithResult` | Runtime service | Partial | Unit Tested | Partial Parity | C# now reports start vs already-started metadata and preserves state on repeated starts. It does not execute live `Invasion.startInvasion`. |
| `com.aionemu.gameserver.services.VortexService.startInvasion` | `Aion.GameServer.Services.VortexInvasionRuntime.StartInvasion` | Runtime service | Partial | Unit Tested | Partial Parity | Existing snapshot-returning start path now delegates to the guarded result path. Service-level scheduling and active-invasion ownership are still incomplete. |

## Metrics

- Total Java artifacts discovered in this UOW: 3
- Total artifacts ported or extended in this UOW: 2
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 2
- Total blocked artifacts: 0
- Conservative Phase 6 parity estimate: 41%

## Remaining Risks

- Live Vortex `start`, `spawn`, `despawn`, and `stop` behavior remains unported.
- Service-level `VortexService.startInvasion` active-map ownership and scheduler behavior remain incomplete.
- Java synchronization is represented by the runtime lock, but full Java service concurrency is not ported.
- Defender alliance lifecycle and live zone handler wiring remain incomplete.
