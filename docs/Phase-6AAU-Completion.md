# Phase 6AAU Completion Handoff

Date: May 26, 2026
Latest Unit of Work: UOW-1211
Status: Phase 6 continues; bind-point teleport now has parser, action selection, operation/control packet intents, fanout intents, request-level composition, runtime task/cooldown state semantics, scheduled Kinah decrement/failure intent, final movement gate intent, scheduled callback composition metadata, a `TeleportService.teleportTo` side-effect audit, and a non-live teleport side-effect planner. Live connection dispatch, real scheduler/cooldown mutation, live inventory mutation, persistent known-list parity, and movement remain disabled.

## Session Summary

UOW-1211 added `BindPointTeleportTeleportToSideEffectPlanService`, a source-derived non-live planner for the Java `TeleportAnimation.NONE` final movement path used by bind-point hotspot teleport. It records the Java ordering from `TeleportService.teleportTo` through `sendLoc`, action abort, world despawn, immediate `SpawnTask`, same-instance `spawnOnSameMap` packet order, map/instance-change packet order, optional instance-opened message, and legion refresh metadata while keeping unsupported dependencies explicit.

Files changed:

- `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportTeleportToSideEffectPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/BindPointTeleportTeleportToSideEffectPlanServiceTests.cs`
- `docs/Phase-6-BindPointTeleport-TeleportTo-Audit.md`
- `docs/Phase-6-BindPointTeleport-LiveFanout-Audit.md`
- `docs/Phase-6-PricesService-Consumer-Map.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6AAU-Completion.md`

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "BindPointTeleportTeleportToSideEffectPlanServiceTests" --nologo` passed 4 tests.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport" --nologo` passed 62 tests.
- No Java runtime comparison was run, so parity remains `Needs Verification`.

## Migration Parity Table - UOW-1211

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.teleport.TeleportService.teleportTo(Player,int,float,float,float)` | `Aion.GameServer.Services.BindPointTeleportTeleportToSideEffectPlanService` | Service / Movement Planner | Partial | Unit Tested | Needs Verification | Non-live planner models overload entry, player heading, `TeleportAnimation.NONE`, no `SM_TELEPORT_LOC`, same-instance versus map/instance-change branch selection, and race/gap flags. It does not mutate player/world state or send packets. |
| `com.aionemu.gameserver.services.teleport.TeleportService.sendLoc` | `Aion.GameServer.Services.BindPointTeleportTeleportToSideEffectPlanService` | Service / Packet Ordering Planner | Partial | Unit Tested | Needs Verification | Planner records `abortPlayerActions`, despawn, spawn-task creation, and immediate execution for `TeleportAnimation.NONE`. Live delayed teleport helper remains separate and must not be used for hotspot final movement. |
| `com.aionemu.gameserver.services.teleport.TeleportService.abortPlayerActions` | `Aion.GameServer.Services.BindPointTeleportTeleportToSideEffectPlanService` gap metadata | Utility / Player State Mutation | Partial | Unit Tested | Needs Verification | Planner marks private-store close, current-skill cancel, target clear, and ride-mode unset as required gaps. No live action-abort implementation exists. |
| `com.aionemu.gameserver.services.teleport.TeleportService.SpawnTask` | `Aion.GameServer.Services.BindPointTeleportTeleportToSideEffectPlanService`; `Aion.GameServer.Services.PlayerTeleportService` future adapter | Runnable / Movement Task | Partial | Unit Tested | Needs Verification | Planner records immediate spawn-task order, spawned-state no-op gap, current world/instance snapshot, player/pet position move, same-instance branch, and map/instance-change branch. Live spawned-state, pet movement, callbacks, and packet sends remain unported. |
| `com.aionemu.gameserver.services.teleport.TeleportService.spawnOnSameMap` | `Aion.GameServer.Services.BindPointTeleportTeleportToSideEffectPlanService`; packet classes `SmChannelInfo`, `SmPlayerInfo`, `SmStatsInfo`, `SmMotion` | Packet / World Spawn Branch | Partial | Unit Tested | Needs Verification | Planner pins Java owner packet order for same-instance hotspot movement. Packet serializers have existing tests, but this unit does not send live packets or runtime-compare Java bytes. |
| `com.aionemu.gameserver.world.World.despawn` / `World.spawn` | `Aion.GameServer.Services.BindPointTeleportTeleportToSideEffectPlanService` gap metadata; future `World`/registry adapter | World / Known-List Dependency | Partial | Unit Tested | Needs Verification | Planner records despawn/spawn requirement and known-list gap; persistent Java known-list membership and delete/spawn fanout remain unverified. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` final movement task | `Aion.GameServer.Services.BindPointTeleportFinalMovementPlanService`; `BindPointTeleportTeleportToSideEffectPlanService`; `BindPointTeleportScheduledCallbackPlanService` | Service / Scheduled Movement Dependency | Partial | Unit Tested | Needs Verification | Final movement gate and teleport side-effect metadata now exist separately. The scheduled callback does not yet compose side-effect plan metadata, and no live final movement executes. |

Tests added:

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `BindPointTeleportTeleportToSideEffectPlanServiceTests.CreatePlan_BlockedFinalMovementProducesNoTeleportSideEffects` | Blocked final movement produces no teleport side-effect steps. | Source-derived only. |
| `BindPointTeleportTeleportToSideEffectPlanServiceTests.CreatePlan_SameInstanceUsesJavaSpawnOnSameMapPacketOrderWithoutTeleportLoc` | Same-instance `TeleportAnimation.NONE` path records Java order without `SM_TELEPORT_LOC`. | Source-derived only. |
| `BindPointTeleportTeleportToSideEffectPlanServiceTests.CreatePlan_MapChangeUsesChannelInfoPlayerSpawnAndInstanceMessageBranch` | Cross-world path records instance `1`, leave-map/instance step, `SM_CHANNEL_INFO`, `SM_PLAYER_SPAWN`, optional instance-opened message, and legion refresh step. | Source-derived only. |
| `BindPointTeleportTeleportToSideEffectPlanServiceTests.CreatePlan_RecordsJavaRaceFallbacksForDeadAfterGateAndDuel` | Race/gap metadata records revive fallback and duel-loss branches before movement side effects. | Source-derived only. |

## Summary Metrics

- Total Java artifacts discovered: 7 grouped artifact rows in this unit
- Total artifacts ported: 1 non-live side-effect planner plus focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 7 grouped rows
- Total blocked artifacts: 6 grouped categories: live connection dispatch, live scheduler/cooldown ownership, live inventory mutation/packets, side-effect plan composition/live adapter, persistent known-list parity, and Java runtime comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- Live `GameServerConnection` still does not dispatch `CmBindPointTeleport`.
- The bind-point pipeline still has no live scheduler, inventory mutation, cooldown-map mutation, packet fanout, or final movement.
- Side-effect planning is not yet composed into the scheduled callback plan.
- Existing C# teleport helpers still cover only pieces of Java `TeleportService`; the new planner is metadata only.
- Action aborts, private-store closure, current-skill cancellation, target clearing, ride-mode removal, pet movement, world despawn/spawn, protection/effect/zone callbacks, instance/leave-map callbacks, and legion refresh remain unported or unverified.
- No Java runtime comparison was executed. Reflection behavior did not change; serialization, threading, date/time, movement, known-list, and persistence parity remain unverified for live bind-point teleport.

## Next Work Options

### Recommended Sequential Task

- Task: Compose `BindPointTeleportTeleportToSideEffectPlanService` into scheduled callback metadata.
- Why: The scheduled callback plan currently carries only the final movement gate. It should carry the concrete `TeleportService.teleportTo` side-effect plan before any live movement adapter is built.
- Suggested files:
  - `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportScheduledCallbackPlanService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/BindPointTeleportScheduledCallbackPlanServiceTests.cs`
  - existing progress/handoff docs

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Compose side-effect planner into scheduled callback metadata | scheduled callback service/test | Medium | Best next step; still non-live. |
| B | Concrete system-message packet support audit for bind-point failure messages | read-only packet/system-message files plus optional doc | Low/Medium | Useful before live sends. |
| C | Handler-level live-dispatch readiness checklist | new doc only | Medium | Keep read-only; no `GameServerConnection` edits yet. |
| D | Continue `SM_SELL_ITEM` live-readiness fact assembly | new/non-overlapping sell-item adapter files and tests | Medium | Avoid shared dialog routing unless owned exclusively. |

### Do Not Parallelize

- `GameServerConnection` live bind-point dispatch: wait until side-effect composition, scheduler ownership, Kinah mutation, cooldown mutation/fanout, and movement side effects are independently tested.
- Shared progress/handoff/parity docs: Orchestrator should own final edits.
- `PlayerTeleportService` live bind-point movement wiring: defer until packet order and unsupported side effects are modeled and composed.

## Context Needed By Next Session

- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/services/teleport/BindPointTeleportService.java`
  - `game-server/src/com/aionemu/gameserver/services/teleport/TeleportService.java`
- C# surfaces:
  - `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportScheduledCallbackPlanService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportFinalMovementPlanService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportTeleportToSideEffectPlanService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/BindPointTeleportScheduledCallbackPlanServiceTests.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/BindPointTeleportTeleportToSideEffectPlanServiceTests.cs`
- Audit doc:
  - `docs/Phase-6-BindPointTeleport-TeleportTo-Audit.md`
- Latest completed commits:
  - `e490a8320 [Phase 6][UOW-1210] Audit bind point teleport movement side effects`
  - next commit should be `[Phase 6][UOW-1211] Add bind point teleport side effect plan`
- Keep live bind-point behavior disabled until teleport side-effect ordering, cooldown mutation/fanout execution, live inventory mutation/packets, live movement packet ordering, and live known-list fanout each have focused parity slices.
