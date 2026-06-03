# Phase 6 Session 2465 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2465: Add metadata-only Vortex stop side-effect plan

## Commits Made

- `[Phase 6][UOW-2465] Add Vortex stop side-effect planner`

## Summary

UOW-2465 added a pure metadata planner for Vortex stop side effects. It consumes a `VortexStopInvasionResult` plus externally supplied invader, kisk, spawned-NPC, and PEACE-spawn snapshots, then returns ordered Java stop intents: clear active vortex, kill invader kisks, kick online invaders, despawn current Vortex NPCs, and spawn PEACE NPCs.

This remains partial parity. The planner does not execute side effects, does not enumerate production world/kisk/spawn maps, and does not dispatch packets.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/VortexStopInvasionSideEffectPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`
- `docs/Phase-6-Session-2465-Completion.md`
- `docs/Phase-6-Session-2465-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.services.VortexService`
- `com.aionemu.gameserver.services.VortexService.despawn`
- `com.aionemu.gameserver.services.VortexService.spawn`
- `com.aionemu.gameserver.services.vortex.DimensionalVortex`
- `com.aionemu.gameserver.services.vortex.Invasion`
- `com.aionemu.gameserver.services.vortex.Invasion.stopInvasion`
- `com.aionemu.gameserver.model.vortex.VortexLocation`
- `com.aionemu.gameserver.model.vortex.VortexLocation.getInvadersKisks`
- `com.aionemu.gameserver.model.vortex.VortexStateType`

## C# Artifacts Touched

- `Aion.GameServer.Services.VortexStopInvasionSideEffectPlanService`
- `Aion.GameServer.Services.VortexStopInvasionSideEffectPlan`
- `Aion.GameServer.Services.VortexStopInvasionSideEffectStep`
- `Aion.GameServer.Services.VortexStopInvasionSideEffectPlanStatus`
- `Aion.GameServer.Services.VortexStopInvasionSideEffectStepKind`
- `Aion.GameServer.Services.VortexStopInvaderSnapshot`
- `Aion.GameServer.Services.VortexStopInvaderKiskSnapshot`
- `Aion.GameServer.Services.VortexStopSpawnedNpcSnapshot`
- `Aion.GameServer.Services.VortexStopPeaceSpawnSnapshot`
- Existing adjacent: `Aion.GameServer.Services.VortexInvasionRuntime`
- Existing adjacent: `Aion.GameServer.Services.VortexStopInvasionResult`

## Validation Completed

Validation target: planner preserves Java stop side-effect order and metadata guards while remaining opt-in and no-dispatch.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests|FullyQualifiedName~VortexRemovalRiftEntryUpdatePreviewServiceTests" --no-restore
```

- Passed: 22 tests.
- Existing nullable/analyzer warnings were emitted.
- First focused run failed due a test assertion selecting a kick step by player id and matching the kisk owner id too; assertion was narrowed to `KickOnlineInvader`, and the same focused command passed.

```powershell
git diff --check
```

- Passed.

Java/Maven validation was skipped because no Java source or fixtures changed, and no narrow Java lifecycle fixture exists for this metadata-only planner. The source-of-truth behavior was reviewed directly in `VortexService`, `DimensionalVortex`, `Invasion`, `VortexLocation`, and `VortexStateType`. Broad-validation trigger was `none` because this UOW only adds metadata/planner records and focused tests, without production spawn/despawn, kisk death, scheduler wiring, world-map enumeration, teleport/system-message dispatch, or live connection dispatch.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.vortex.Invasion.stopInvasion` | `Aion.GameServer.Services.VortexStopInvasionSideEffectPlanService` | Lifecycle side-effect planner | Partial | Unit Tested | Partial Parity | Java stop side-effect categories and ordering are modeled as metadata. Live kisk death, online kicks, despawn, and PEACE spawn remain unexecuted. |
| `com.aionemu.gameserver.services.VortexService.despawn` | `Aion.GameServer.Services.VortexStopInvasionSideEffectStepKind.DespawnVortexNpc` | Spawn lifecycle intent | Partial | Unit Tested | Partial Parity | Planner records existing Vortex NPC despawn intents from supplied snapshots. It does not call `WorldNpcSpawnService`. |
| `com.aionemu.gameserver.services.VortexService.spawn` | `Aion.GameServer.Services.VortexStopInvasionSideEffectStepKind.SpawnPeaceNpc` | Spawn lifecycle intent | Partial | Unit Tested | Partial Parity | Planner records PEACE spawn intents from supplied snapshots. It does not materialize NPCs or rift state. |
| `com.aionemu.gameserver.model.vortex.VortexStateType` | `Aion.GameServer.Services.VortexStopPeaceSpawnSnapshot` and step `VortexState` | Enum usage metadata | Partial | Unit Tested | Partial Parity | Java `PEACE` state is represented as metadata string for stop planning only; no full enum port in this UOW. |
| `com.aionemu.gameserver.model.vortex.VortexLocation.getInvadersKisks` | `Aion.GameServer.Services.VortexStopInvaderKiskSnapshot` | Kisk snapshot metadata | Partial | Unit Tested | Partial Parity | Planner consumes externally supplied kisk snapshots and creates kill intents. It does not own or enumerate production kisk maps. |

## Known Gaps

- Stop-invasion live side effects are not enabled.
- Runtime does not source invader/kisk/spawn snapshots from production world state.
- Planner does not invoke existing invader removal logic.
- Planner does not call `WorldNpcSpawnService` for despawn or PEACE spawn.
- Scheduler-triggered stop and service-level Vortex ownership remain incomplete.
- Defender alliance lifecycle and live zone handler wiring remain incomplete.

## Remaining Risks

- Future live stop execution must preserve Java order while avoiding the UOW-2464 ordering hazard: removing the active runtime entry before invoking per-invader removal means removal needs a pre-stop snapshot or a dedicated stop-kick path.
- Kisk death and NPC despawn will cross live world-state boundaries and require broader validation.
- PEACE spawn selection must eventually come from parsed Vortex spawn templates, not ad hoc test data.

## Next Recommended UOW

[Phase 6] UOW-2466: Add service-level Vortex stop coordinator metadata

The next smallest safe task is to add a non-live coordinator that composes `VortexInvasionRuntime.StopInvasion` with `VortexStopInvasionSideEffectPlanService` using externally supplied snapshots. It should return a combined stop report with runtime stop result, side-effect plan, guard status, and no-live-execution flags. Keep it opt-in; do not enumerate production world maps, do not schedule tasks, do not invoke `RemoveInvaderPlayer`, do not call `WorldNpcSpawnService`, and do not dispatch packets.

Safe alternative candidates:

- Add a tiny `VortexStateType` C# enum for `Invasion`/`Peace` metadata and update the planner to use it instead of the current `VortexState` string.
- Add static-data filtering for supplied Vortex PEACE spawn summaries only if the existing XML parser already preserves Vortex spawn state; inspect first and keep it metadata-only.

## Suggested Files To Inspect

- `game-server/src/com/aionemu/gameserver/services/VortexService.java`
- `game-server/src/com/aionemu/gameserver/services/vortex/DimensionalVortex.java`
- `game-server/src/com/aionemu/gameserver/services/vortex/Invasion.java`
- `game-server/src/com/aionemu/gameserver/model/vortex/VortexLocation.java`
- `game-server/src/com/aionemu/gameserver/model/vortex/VortexStateType.java`
- `dotnetConversion/src/Aion.GameServer/Services/VortexInvasionRuntime.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexStopInvasionSideEffectPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`

## Suggested Validation

For service-level metadata coordination:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests|FullyQualifiedName~VortexRemovalRiftEntryUpdatePreviewServiceTests" --no-restore
```

Validation target: coordinator composes runtime stop and side-effect planning guards while staying opt-in and no-dispatch.

Java/Maven is not expected unless Java fixtures change or a narrow Java lifecycle fixture is added. Broad-validation trigger should be `none` if the UOW only composes existing metadata services and focused tests; it becomes present if production spawn/despawn, kisk death, scheduler wiring, world-map enumeration, teleport/system-message dispatch, or live connection dispatch is enabled.
