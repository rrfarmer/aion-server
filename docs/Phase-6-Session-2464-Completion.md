# Phase 6 Session 2464 Completion

## UOW

[Phase 6] UOW-2464: Add metadata-only stop-invasion snapshot clearing

## Status

Completed and validated with focused .NET tests.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/VortexService.java`
- `game-server/src/com/aionemu/gameserver/services/vortex/DimensionalVortex.java`
- `game-server/src/com/aionemu/gameserver/services/vortex/Invasion.java`
- `game-server/src/com/aionemu/gameserver/model/vortex/VortexLocation.java`

## C# Changes

- `dotnetConversion/src/Aion.GameServer/Services/VortexInvasionRuntime.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`

## Implementation Notes

- Added `VortexInvasionRuntime.StopInvasion(int vortexLocationId)`.
- Added `VortexStopInvasionStatus` and `VortexStopInvasionResult`.
- Stop removes the active runtime entry, returns a previous snapshot, clears active portal, invader, defender, and passed-player metadata, and returns the cleared stopped snapshot.
- Missing or repeated stop returns guard metadata and no snapshots, matching Java `VortexService.stopInvasion` returning when no active invasion remains.
- Scope remains metadata-only. This UOW does not kill kisks, kick online invaders, despawn NPCs, spawn PEACE NPCs, enumerate world players, schedule work, or dispatch packets.

## Tests

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `StopInvasion_ClearsActivePortalParticipantsAndRuntimeEntryLikeJavaStopMetadata` | Unit | `VortexService.stopInvasion`, `DimensionalVortex.stop`, `Invasion.stopInvasion` | Stop result captures before/after metadata, clears portal and participants, removes active runtime entry | C# test validates Java-reviewed metadata effects | Does not execute Java live side effects |
| `StopInvasion_MissingOrRepeatedStopReturnsGuardMetadata` | Unit | `VortexService.stopInvasion` active-map guard | Missing and repeated stops return guard metadata without snapshots | C# test validates no active snapshot remains after stop | No Java runtime comparison |
| `StartInvasion_AfterStopCreatesFreshRuntimeStateLikeJavaServiceRestart` | Unit | `VortexService.startInvasion` creates a new `Invasion` after prior active entry is gone | Restart after stop creates fresh participant state | C# test validates fresh snapshot and participant membership | Scheduler and spawn lifecycle remain unmodeled |

## Validation Decision

- Changed surface: Vortex runtime stop metadata and focused runtime tests.
- Specific behavior/contract: Java `VortexService.stopInvasion` removes the active invasion entry and then `Invasion.stopInvasion` clears active vortex metadata; C# must clear active portal and participant metadata while returning explicit stop snapshots without enabling live side effects.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests|FullyQualifiedName~VortexRemovalRiftEntryUpdatePreviewServiceTests" --no-restore
```

- Result: Passed, 20 tests. Existing nullable/analyzer warnings were emitted.
- Focused Java/Maven command: skipped; no Java source or fixtures changed, and the source-of-truth behavior was reviewed directly in `VortexService.stopInvasion`, `DimensionalVortex.stop`, `Invasion.stopInvasion`, and `VortexLocation.setActiveVortex`.
- Broad-validation trigger: none. This UOW only changes deterministic runtime metadata and tests; it does not enable production spawn/despawn, kisk death, scheduler wiring, world-map enumeration, or live connection dispatch.
- Broad .NET decision: skipped; focused validation built the affected project and covered the edited runtime surface plus adjacent Vortex preview tests from the handoff recipe.
- Why this scope is sufficient: the changed API is isolated to in-memory runtime metadata and does not cross packet, persistence, scheduler, or live dispatch boundaries.

## Additional Hygiene

```powershell
git diff --check
```

- Passed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.VortexService.stopInvasion` | `Aion.GameServer.Services.VortexInvasionRuntime.StopInvasion` | Service method | Partial | Unit Tested | Partial Parity | Active runtime entry removal and missing/repeated-stop guards are modeled. Java scheduler invocation and live stop side effects remain unported. |
| `com.aionemu.gameserver.services.vortex.DimensionalVortex.stop` | `Aion.GameServer.Services.VortexStopInvasionResult` | Lifecycle guard/result | Partial | Unit Tested | Partial Parity | Result models first stop versus no active invasion at runtime-service level. Java object's atomic finished flag is not separately represented. |
| `com.aionemu.gameserver.services.vortex.Invasion.stopInvasion` | `Aion.GameServer.Services.VortexInvasionRuntime.StopInvasion` | Lifecycle method | Partial | Unit Tested | Partial Parity | Active portal and participant metadata are cleared. Kisk death, online invader kicks, despawn, and PEACE spawn remain gaps. |
| `com.aionemu.gameserver.model.vortex.VortexLocation.setActiveVortex` | `Aion.GameServer.Services.VortexInvasionSnapshot` plus stop result | Location state metadata | Partial | Unit Tested | Partial Parity | Stop result exposes cleared snapshot and runtime `GetSnapshot` becomes null after active entry removal. Java `isActive` flag is represented implicitly by runtime entry presence. |

## Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported or extended in this UOW: 4
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 0
- Conservative Phase 6 parity estimate: 41%

## Remaining Risks

- Stop-invasion live side effects remain unported: kisk death, online invader kicks, despawn, and PEACE spawn.
- Vortex runtime does not yet model active defender alliance cleanup.
- Production online-player and kisk snapshots are not owned by this runtime.
- Scheduler-triggered stop remains outside this metadata slice.
- Future live stop wiring will be a broad-validation trigger if it touches world maps, NPC spawn/despawn, scheduler state, or packet dispatch.
