# Phase 6 Session 2511 Completion

## UOW

[Phase 6] UOW-2511: Add guarded Vortex start-invasion runtime defender update report

## Status

Completed and validated with focused .NET tests.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/vortex/Invasion.java`
- `game-server/src/com/aionemu/gameserver/services/vortex/DimensionalVortex.java`
- `com.aionemu.gameserver.model.vortex.VortexLocation.getPlayers`
- `com.aionemu.gameserver.model.gameobjects.player.ResponseRequester.putRequest`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_QUESTION_WINDOW.java`

## C# Changes

- `dotnetConversion/src/Aion.GameServer/Services/VortexStartInvasionCoordinatorService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexStartInvasionSideEffectPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`

## Implementation Notes

- Added an opt-in `StartInvasionWithRuntimeDefenderUpdate` coordinator path that runs `VortexDefenderAllianceUpdateRuntimeAdapterService` only after `VortexInvasionRuntime.StartInvasionWithResult` succeeds.
- Extended `VortexStartInvasionSideEffectPlan` with optional `VortexDefenderAllianceUpdateRuntimeReport` metadata and runtime defender counts.
- Preserved Java start guard ordering: when the invasion is already active, the runtime defender update adapter is not invoked and no pending defender request is stored.
- The successful opt-in path can store pending defender invitation requests through the guarded response registry adapter, matching Java `updateDefenders` request registration ordering.
- Scope remains guarded. It does not send `SM_QUESTION_WINDOW`, execute callbacks, mutate groups, mutate alliances, mutate defender maps, schedule, spawn, despawn, or start portals.

## Tests

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `StartCoordinator_RuntimeDefenderUpdateRunsAfterStartGuardAndCarriesSideEffectMetadata` | Unit | `Invasion.startInvasion`, `Invasion.updateAlliance`, `Invasion.updateDefenders`, `ResponseRequester.putRequest`, `SM_QUESTION_WINDOW` source review | Successful start carries runtime defender update metadata into the side-effect report, selects only defender-race players, preserves occupied-slot and already-defender outcomes, stores only successful requests, and keeps live sends/mutations disabled | Focused C# test validates Java start step order through update-alliance, runtime defender counts, selected/skipped ids, request registry mutation, occupied request preservation, and disabled side-effect flags | Supplied player list stands in for production `VortexLocation.getPlayers().values()` |
| `StartCoordinator_RuntimeDefenderUpdateSkipsWhenJavaStartGuardFails` | Unit | `DimensionalVortex.start`, `Invasion.startInvasion` source review | Duplicate start guard skips update-alliance runtime work | Focused C# test validates already-started status, no side-effect steps, no runtime defender report, zero runtime counts, and no pending request mutation | Does not execute Java runtime |

## Validation Decision

- Changed surface: one C# opt-in start-invasion coordinator overload, side-effect report metadata extension, and focused tests.
- Specific behavior/contract: Java `DimensionalVortex.start` returns before `Invasion.startInvasion` when already started; otherwise `Invasion.startInvasion` runs `setActiveVortex`, `despawn`, `spawn(INVASION)`, `initRiftGenerator`, then `updateAlliance`. C# now exposes a guarded start-invasion runtime report path that invokes runtime defender update metadata only after the start guard succeeds.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests|FullyQualifiedName~QuestionResponseRegistryTests" --no-restore
```

- Result: Passed, 127 tests. Existing nullable/analyzer warnings were emitted from unrelated files and prior test lines.
- Focused Java/Maven command: skipped; no Java source or fixtures changed, and no narrow Java start-invasion/update-alliance fixture exists for this seam.
- Broad-validation trigger: none. This path is opt-in and does not enable packet dispatch, lifecycle side-effect execution, scheduler behavior, persistence, or shared gameplay-state mutation.
- Broad .NET decision: full project/solution validation skipped because the focused command covered the edited Vortex tests and adjacent response-registry contract.
- Why this scope is sufficient: focused tests cover successful start ordering through update-alliance, duplicate-start guard omission, defender selection, request-storage outcomes, occupied/already-defender outcomes, disabled live sends, and disabled gameplay mutation.

## Additional Hygiene

```powershell
git diff --check
git diff --cached --check
```

- `git diff --check` passed. Git reported expected CRLF conversion warnings for edited C# source.
- `git diff --cached --check` passed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.vortex.DimensionalVortex.start` | `Aion.GameServer.Services.VortexInvasionRuntime.StartInvasionWithResult` / `VortexStartInvasionCoordinatorService.StartInvasionWithRuntimeDefenderUpdate` | Runtime start guard | Partial | Unit Tested | Partial Parity | C# duplicate-start guard now prevents opt-in runtime defender update invocation. Java atomic/finished lifecycle and production service map wiring remain partial. |
| `com.aionemu.gameserver.services.vortex.Invasion.startInvasion` | `Aion.GameServer.Services.VortexStartInvasionCoordinatorService.StartInvasionWithRuntimeDefenderUpdate` / `VortexStartInvasionSideEffectPlan` | Runtime start-invasion report | Partial | Unit Tested | Partial Parity | C# carries runtime defender update metadata after the start guard and preserves non-live start step ordering. Live active-vortex, despawn, spawn, rift-generator, scheduler, and packet dispatch execution remain disabled. |
| `com.aionemu.gameserver.services.vortex.Invasion.updateAlliance` | `Aion.GameServer.Services.VortexDefenderAllianceUpdateRuntimeAdapterService` carried by `VortexStartInvasionSideEffectPlan` | Runtime defender update metadata | Partial | Unit Tested | Partial Parity | C# start report now exposes runtime update-alliance selected/skipped ids and registration counts from supplied players. Production `VortexLocation.getPlayers()` lookup remains unimplemented. |
| `com.aionemu.gameserver.services.vortex.Invasion.updateDefenders` | `Aion.GameServer.Services.VortexDefenderInvitationBatchRuntimeAdapterService` / `VortexDefenderUpdateDefendersRegistrationRuntimeAdapterService` | Runtime defender registration adapter | Partial | Unit Tested | Partial Parity | C# opt-in start path can store pending request metadata for selected defenders while packet sending, callback execution, and defender/alliance mutation remain disabled. |

## Metrics

- Total Java artifacts discovered in this UOW: 5
- Total artifacts ported or extended in this UOW: 4
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 0
- Conservative Phase 6 parity estimate: 45%

## Remaining Risks

- Live Vortex `startInvasion` still does not execute production active-vortex assignment, despawn, spawn, rift-generator, scheduler, or packet dispatch side effects.
- The runtime defender update path accepts supplied player candidates; it does not own production `VortexLocation.getPlayers().values()` lookup or map semantics.
- `SM_QUESTION_WINDOW` packet dispatch remains disabled.
- Vortex response-handler callbacks, acceptance flow, add-player transition, and alliance mutation remain metadata-only.
- Production world/location/alliance containers remain absent from the Vortex start path.
