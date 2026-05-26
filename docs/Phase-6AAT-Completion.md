# Phase 6AAT Completion Handoff

Date: May 26, 2026
Latest Unit of Work: UOW-1210
Status: Phase 6 continues; bind-point teleport now has parser, action selection, operation/control packet intents, fanout intents, request-level composition, runtime task/cooldown state semantics, scheduled Kinah decrement/failure intent, final movement gate intent, scheduled callback composition metadata, and a source-derived `TeleportService.teleportTo` side-effect audit. Live connection dispatch, real scheduler/cooldown mutation, live inventory mutation, persistent known-list parity, and movement remain disabled.

## Session Summary

UOW-1210 added `docs/Phase-6-BindPointTeleport-TeleportTo-Audit.md`, a read-only audit of Java `TeleportService.teleportTo(Player,int,float,float,float)` as used by bind-point hotspot final movement. The audit documents the Java `TeleportAnimation.NONE` path from final movement gate through overload selection, `sendLoc`, `abortPlayerActions`, world despawn, immediate `SpawnTask`, same-instance versus map/instance-change packet ordering, player/pet position updates, protection/effect/zone callbacks, leave-map/instance callbacks, and legion refresh.

Files changed:

- `docs/Phase-6-BindPointTeleport-TeleportTo-Audit.md`
- `docs/Phase-6-BindPointTeleport-LiveFanout-Audit.md`
- `docs/Phase-6-PricesService-Consumer-Map.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6AAT-Completion.md`

## Validation

- Documentation/source-audit unit; no product code changed and no new tests were added.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport" --nologo` passed 58 tests.
- No Java runtime comparison was run, so parity remains `Needs Verification`.

## Migration Parity Table - UOW-1210

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.teleport.TeleportService.teleportTo(Player,int,float,float,float)` | `Aion.GameServer.Services.BindPointTeleportFinalMovementPlanService`; future bind-point teleport side-effect planner/live adapter | Service / Movement | Partial | Unit Tested | Needs Verification | Current C# records the bind-point destination intent, heading, `TeleportAnimation.NONE`, and target instance selection. Missing methods/behavior: live core `teleportTo` execution, dead-after-gate revive fallback, duel loss, action aborts, despawn/spawn, packet sends, pet movement, leave callbacks, and legion refresh. |
| `com.aionemu.gameserver.services.teleport.TeleportService.sendLoc` | `Aion.GameServer.Network.Aion.GameServerConnection.QueueDelayedTeleportAsync`; future bind-point immediate movement adapter | Service / Packet Ordering | Partial | Manual Only | Needs Verification | Existing C# delayed teleport helper approximates animation paths, but bind-point final movement uses `TeleportAnimation.NONE` and must not send `SM_TELEPORT_LOC`. Serialization differences remain unverified for live bind-point movement. |
| `com.aionemu.gameserver.services.teleport.TeleportService.abortPlayerActions` | future C# action-abort composition | Utility / Player State Mutation | Not Started | No Tests | Unknown | Java closes private store, cancels current skill, clears target, and unsets ride mode. C# bind-point movement has no equivalent yet. Threading and observer side effects are unverified. |
| `com.aionemu.gameserver.services.teleport.TeleportService.SpawnTask` | `Aion.GameServer.Services.PlayerTeleportService`; `Aion.GameServer.Network.Aion.GameServerConnection.SendDelayedTeleportCompletionPacketsAsync`; future bind-point side-effect planner | Runnable / Movement Task | Partial | Manual Only | Needs Verification | C# has immediate/pending movement helpers and some packet branches, but no bind-point `NONE` spawn-task adapter. Missing spawned-state no-op, delayed fallback branch, pet move, leave callbacks, protection/effect/zone callbacks, and legion refresh. |
| `com.aionemu.gameserver.services.teleport.TeleportService.spawnOnSameMap` | `Aion.GameServer.Network.Aion.ServerPackets.SmChannelInfo`; `SmPlayerInfo`; `SmStatsInfo`; `SmMotion`; future bind-point same-instance send plan | Packet / World Spawn Branch | Partial | Unit Tested for packet serializers; Manual Only for order | Needs Verification | Packet classes exist and some callers send similar packets, but Java same-map order is not yet tested for bind-point movement. C# kisk revive order differs and must not be treated as parity. |
| `com.aionemu.gameserver.world.World.despawn` / `World.spawn` | `Aion.GameServer.World.World`; `Aion.GameServer.Network.Aion.IGameClientConnectionRegistry` | World / Known-List Dependency | Partial | Manual Only | Needs Verification | C# has registry fanout and world object storage, but persistent Java known-list membership, exact despawn/spawn side effects, pet spawn, and delete animation ordering remain unverified. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` final movement task | `Aion.GameServer.Services.BindPointTeleportScheduledCallbackPlanService` plus `BindPointTeleportFinalMovementPlanService` | Service / Scheduled Movement Dependency | Partial | Unit Tested | Needs Verification | The bind-point callback and final movement gate are staged as metadata only. This audit discovered additional `TeleportService.teleportTo` dependencies that must be modeled before live movement. |

Tests added:

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| None in UOW-1210 | Documentation audit of Java side-effect order before code is written. | Manual source inspection only. |

## Summary Metrics

- Total Java artifacts discovered: 7 grouped artifact rows in this unit
- Total artifacts ported: 0 live artifacts; 1 read-only side-effect audit completed
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: 6 grouped categories: live connection dispatch, live scheduler/cooldown ownership, live inventory mutation/packets, bind-point movement side-effect planner, persistent known-list parity, and Java runtime comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- Live `GameServerConnection` still does not dispatch `CmBindPointTeleport`.
- The bind-point pipeline still has no live scheduler, inventory mutation, cooldown-map mutation, packet fanout, or final movement.
- Existing C# teleport helpers cover only pieces of Java `TeleportService`; bind-point same-instance ordering and map-change ordering need focused tests before reuse.
- Java race behavior where `TeleportService.teleportTo` revives a player who became dead after the final gate is not modeled.
- Action aborts, private-store closure, current-skill cancellation, target clearing, ride-mode removal, pet movement, world despawn/spawn, protection/effect/zone callbacks, instance/leave-map callbacks, and legion refresh remain unported or unverified.
- No Java runtime comparison was executed. Reflection behavior did not change; serialization, threading, date/time, movement, known-list, and persistence parity remain unverified for live bind-point teleport.

## Next Work Options

### Recommended Sequential Task

- Task: Add a non-live bind-point `TeleportService.teleportTo` side-effect planner.
- Why: The final movement intent is staged, but live movement needs a source-derived ordering plan for Java `sendLoc`, immediate `SpawnTask`, same-instance/map-change packets, and explicit gaps before `PlayerTeleportService` or `GameServerConnection` can be safely touched.
- Suggested files:
  - `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportTeleportToSideEffectPlanService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/BindPointTeleportTeleportToSideEffectPlanServiceTests.cs`
  - existing progress/handoff docs

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Non-live `TeleportService.teleportTo` side-effect planner | new service/test pair | Medium | Best next step; keep live movement disabled. |
| B | Concrete system-message packet support audit for bind-point failure messages | read-only packet/system-message files plus optional doc | Low/Medium | Useful before live sends. |
| C | Handler-level live-dispatch readiness checklist | new doc only | Medium | Keep read-only; no `GameServerConnection` edits yet. |
| D | Continue `SM_SELL_ITEM` live-readiness fact assembly | new/non-overlapping sell-item adapter files and tests | Medium | Avoid shared dialog routing unless owned exclusively. |

### Do Not Parallelize

- `GameServerConnection` live bind-point dispatch: wait until side-effect planner, scheduler ownership, Kinah mutation, cooldown mutation/fanout, and movement side effects are independently tested.
- Shared progress/handoff/parity docs: Orchestrator should own final edits.
- `PlayerTeleportService` live bind-point movement wiring: defer until packet order and unsupported side effects are modeled.

## Context Needed By Next Session

- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/services/teleport/BindPointTeleportService.java`
  - `game-server/src/com/aionemu/gameserver/services/teleport/TeleportService.java`
  - `game-server/src/com/aionemu/gameserver/world/World.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_CHANNEL_INFO.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_PLAYER_SPAWN.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_PLAYER_INFO.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_STATS_INFO.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_MOTION.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_TELEPORT_LOC.java`
- C# surfaces:
  - `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportFinalMovementPlanService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportScheduledCallbackPlanService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerTeleportService.cs`
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- Audit doc:
  - `docs/Phase-6-BindPointTeleport-TeleportTo-Audit.md`
- Latest completed commits:
  - `f241ff8da [Phase 6][UOW-1209] Add bind point teleport scheduled callback plan`
  - next commit should be `[Phase 6][UOW-1210] Audit bind point teleport movement side effects`
- Keep live bind-point behavior disabled until teleport side-effect ordering, cooldown mutation/fanout execution, live inventory mutation/packets, live movement packet ordering, and live known-list fanout each have focused parity slices.
