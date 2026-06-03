# Phase 6 Session 2465 Completion

## UOW

[Phase 6] UOW-2465: Add metadata-only Vortex stop side-effect plan

## Status

Completed and validated with focused .NET tests.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/VortexService.java`
- `game-server/src/com/aionemu/gameserver/services/vortex/DimensionalVortex.java`
- `game-server/src/com/aionemu/gameserver/services/vortex/Invasion.java`
- `game-server/src/com/aionemu/gameserver/model/vortex/VortexLocation.java`
- `game-server/src/com/aionemu/gameserver/model/vortex/VortexStateType.java`

## C# Changes

- `dotnetConversion/src/Aion.GameServer/Services/VortexStopInvasionSideEffectPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`

## Implementation Notes

- Added `VortexStopInvasionSideEffectPlanService`.
- Added metadata snapshot records for externally supplied stop inputs:
  - `VortexStopInvaderSnapshot`
  - `VortexStopInvaderKiskSnapshot`
  - `VortexStopSpawnedNpcSnapshot`
  - `VortexStopPeaceSpawnSnapshot`
- Added ordered side-effect steps for Java stop order:
  - clear active vortex
  - kill invader kisks
  - kick online invaders
  - despawn Vortex NPCs
  - spawn PEACE NPCs
- Planner returns `ShouldExecuteLiveSideEffects == false` for every status. It does not mutate runtime state, invoke `RemoveInvaderPlayer`, enumerate production worlds, schedule tasks, kill kisks, despawn/spawn NPCs, teleport players, or dispatch packets.
- Missing stop or missing stop snapshots return guard metadata without steps.

## Tests

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `StopSideEffectPlan_PreservesJavaStopOrderWithoutExecutingLiveEffects` | Unit | `Invasion.stopInvasion`, `VortexService.despawn`, `VortexService.spawn(VortexStateType.PEACE)` | Planner emits Java stop side-effect categories in order, plans only online invader kicks, marks in-world invaders for home teleport metadata, and stays no-dispatch | C# test validates Java-reviewed order and metadata flags | Does not execute live side effects or compare runtime Java output |
| `StopSideEffectPlan_MissingStopOrSnapshotsReturnsGuardMetadata` | Unit | `VortexService.stopInvasion` guard and `DimensionalVortex.stop` lifecycle | Missing stop or incomplete stop metadata returns guard status with no steps | C# guard test validates no live execution flags | No Java runtime comparison |

## Validation Decision

- Changed surface: Vortex metadata-only stop side-effect planner and focused tests.
- Specific behavior/contract: Java `Invasion.stopInvasion` clears active vortex, kills invader kisks, kicks online invaders, despawns current Vortex spawns, and spawns PEACE NPCs; C# planner must preserve those side-effect categories and order without executing them.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests|FullyQualifiedName~VortexRemovalRiftEntryUpdatePreviewServiceTests" --no-restore
```

- Result: Passed, 22 tests. Existing nullable/analyzer warnings were emitted.
- First focused run failed because the test selected a kick step only by player id, which also matched the owner id on the kisk step. The assertion was narrowed to `KickOnlineInvader`, then the same focused command passed.
- Focused Java/Maven command: skipped; no Java source or fixtures changed, and no narrow Java lifecycle fixture exists for this metadata-only planner. The source-of-truth behavior was reviewed directly in the Java files listed above.
- Broad-validation trigger: none. This UOW only adds deterministic metadata records/planner and focused tests; it does not enable production spawn/despawn, kisk death, scheduler wiring, world-map enumeration, teleport/system-message dispatch, or live connection dispatch.
- Broad .NET decision: skipped; focused validation built the affected project and covered the new planner plus adjacent Vortex runtime/preview tests.
- Why this scope is sufficient: the changed API is isolated to pure planning metadata and does not cross packet, persistence, scheduler, or live dispatch boundaries.

## Additional Hygiene

```powershell
git diff --check
```

- Passed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.vortex.Invasion.stopInvasion` | `Aion.GameServer.Services.VortexStopInvasionSideEffectPlanService` | Lifecycle side-effect planner | Partial | Unit Tested | Partial Parity | Java stop side-effect categories and ordering are modeled as metadata. Live kisk death, online kicks, despawn, and PEACE spawn remain unexecuted. |
| `com.aionemu.gameserver.services.VortexService.despawn` | `Aion.GameServer.Services.VortexStopInvasionSideEffectStepKind.DespawnVortexNpc` | Spawn lifecycle intent | Partial | Unit Tested | Partial Parity | Planner records existing Vortex NPC despawn intents from supplied snapshots. It does not call `WorldNpcSpawnService`. |
| `com.aionemu.gameserver.services.VortexService.spawn` | `Aion.GameServer.Services.VortexStopInvasionSideEffectStepKind.SpawnPeaceNpc` | Spawn lifecycle intent | Partial | Unit Tested | Partial Parity | Planner records PEACE spawn intents from supplied snapshots. It does not materialize NPCs or rift state. |
| `com.aionemu.gameserver.model.vortex.VortexStateType` | `Aion.GameServer.Services.VortexStopPeaceSpawnSnapshot` and step `VortexState` | Enum usage metadata | Partial | Unit Tested | Partial Parity | Java `PEACE` state is represented as metadata string for stop planning only; no full enum port in this UOW. |
| `com.aionemu.gameserver.model.vortex.VortexLocation.getInvadersKisks` | `Aion.GameServer.Services.VortexStopInvaderKiskSnapshot` | Kisk snapshot metadata | Partial | Unit Tested | Partial Parity | Planner consumes externally supplied kisk snapshots and creates kill intents. It does not own or enumerate production kisk maps. |

## Metrics

- Total Java artifacts discovered in this UOW: 5
- Total artifacts ported or extended in this UOW: 5
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 5
- Total blocked artifacts: 0
- Conservative Phase 6 parity estimate: 41%

## Remaining Risks

- Stop-invasion live execution remains unported.
- Kisk death is only an intent; actual `Kisk.getController().die()` equivalence still needs live-side-effect wiring.
- Invader kick is only an intent; existing runtime removal/kick behavior is not invoked by stop.
- Despawn and PEACE spawn are only intents; actual `WorldNpcSpawnService` integration remains future work.
- Production sources for invader/kisk/spawn snapshots are not wired.
- Future live stop wiring will be a broad-validation trigger if it touches world maps, NPC lifecycle, scheduler state, teleport/system-message dispatch, or packet fanout.
