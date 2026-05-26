# Phase 6 Bind-Point Teleport TeleportTo Audit

Date: May 26, 2026
Unit of Work: UOW-1210
Scope: Read-only audit of Java `TeleportService.teleportTo(Player,int,float,float,float)` as used by bind-point hotspot final movement.
Source of truth: Java project.

## Audit Result

Live bind-point final movement should remain disabled until a focused C# side-effect plan exists for Java `TeleportService.teleportTo` and `sendLoc`. The current bind-point planner stack correctly records the final movement gate and destination intent, but the Java call performs broader runtime side effects than the planner currently models: action aborts, visible despawn, immediate spawn-task execution for `TeleportAnimation.NONE`, player and pet position mutation, same-instance versus map-load packet selection, leave-map/instance callbacks, protection/effect/zone callbacks, and legion world-id refresh.

This unit adds no live behavior. It documents the exact Java ordering so the next unit can model these effects without wiring `GameServerConnection` to `CmBindPointTeleport` yet.

Update after UOW-1211: C# now has `BindPointTeleportTeleportToSideEffectPlanService`, a non-live side-effect planner for the audited Java `TeleportAnimation.NONE` path. It emits ordered same-instance and map/instance-change steps, explicitly records that no `SM_TELEPORT_LOC` should be sent, and flags the remaining unsupported Java dependencies. Live movement remains disabled.

Update after UOW-1212: `BindPointTeleportScheduledCallbackPlanService` can now compose an optional `BindPointTeleportTeleportToSideEffectPlan` after the final movement intent, preserving Java callback order while still doing no live movement.

## Java Hotspot Flow

Java source files:

- `game-server/src/com/aionemu/gameserver/services/teleport/BindPointTeleportService.java`
- `game-server/src/com/aionemu/gameserver/services/teleport/TeleportService.java`
- `game-server/src/com/aionemu/gameserver/world/World.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_CHANNEL_INFO.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_PLAYER_SPAWN.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_PLAYER_INFO.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_STATS_INFO.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_MOTION.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_TELEPORT_LOC.java`

Observed Java ordering for bind-point final movement:

1. `BindPointTeleportService.teleport` schedules a final one-second task after the scheduled Kinah/cooldown/fanout callback.
2. The final task checks `!player.getLifeStats().isAboutToDie() && !player.isDead()`.
3. If both checks pass, it calls `TeleportService.teleportTo(player, hotspot.getWorldId(), hotspot.getX(), hotspot.getY(), hotspot.getZ())`.
4. The five-argument overload delegates with `player.getHeading()` and `TeleportAnimation.NONE`.
5. The overload chooses target instance id as `1` when `player.getWorldId() != worldId`; otherwise it keeps `player.getInstanceId()`.
6. The core overload revives dead players through `PlayerReviveService.revive(player, 20, 20, true, 0)` if the player became dead after the final task gate, then still continues to `sendLoc`.
7. The core overload makes dueling players lose the duel, then calls `sendLoc`.
8. `sendLoc` calls `abortPlayerActions`.
9. `abortPlayerActions` closes a private store, cancels the current skill, clears target, and unsets `PlayerMode.RIDE`.
10. `sendLoc` calls `World.getInstance().despawn(player, animation.getDefaultObjectDeleteAnimation())`; for `TeleportAnimation.NONE`, the default delete animation is used and no `SM_TELEPORT_LOC` is sent to the player.
11. `sendLoc` creates `SpawnTask` and runs it immediately because the animation is `NONE`.
12. `SpawnTask.run` returns immediately if `player.isSpawned()` is already true.
13. Because animation is `NONE`, the delayed-animation dead/instance-exists fallback and second `abortPlayerActions` branch are skipped.
14. `SpawnTask.run` snapshots current world and instance before moving.
15. If world or instance changes, it calls `ConquerorAndProtectorService.onLeaveMap(player)` and `InstanceService.onLeaveInstance(player)`.
16. It sets player and pet position to the destination.
17. It sets player port animation to `animation.getDefaultArrivalAnimation()`.
18. If world and instance did not change, it calls `spawnOnSameMap`.
19. `spawnOnSameMap` sends owner packets in this order: `SM_CHANNEL_INFO`, `SM_PLAYER_INFO`, `SM_STATS_INFO`, `SM_MOTION`.
20. `spawnOnSameMap` calls `World.spawn(player)` and `World.spawn(player.getPet())`, starts protection, updates effect icons, updates zone, and resets port animation to `ArrivalAnimation.NONE`.
21. If world or instance changed, `SpawnTask.run` sends owner `SM_CHANNEL_INFO` then `SM_PLAYER_SPAWN`; full map load continues through `CM_LEVEL_READY`.
22. For non-personal instance destinations, it also sends `STR_MSG_INSTANCE_DUNGEON_OPENED_FOR_SELF(worldId)`.
23. If the player is a legion member and the legion member world id differs from the destination world, it calls `LegionService.updateMemberInfo(player)`.

## Current C# State

C# surfaces reviewed:

- `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportFinalMovementPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportScheduledCallbackPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerTeleportService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmChannelInfo.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmPlayerSpawn.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmPlayerInfo.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmStatsInfo.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmMotion.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmTeleportLoc.cs`

Observed C# state:

- `BindPointTeleportFinalMovementPlanService` records the final about-to-die/dead gate, player-heading use, `TeleportAnimation.NONE`, same-world instance preservation, and cross-world instance `1` selection.
- `BindPointTeleportScheduledCallbackPlanService` composes that final movement plan after scheduled Kinah success, cooldown intent, and action `3` fanout intent.
- `PlayerTeleportService` can mutate a player position immediately, queue/complete pending delayed teleports, set port animation for some paths, and reset movement vectors.
- `GameServerConnection` already has reusable packet send helpers for kisk revive, portal delayed teleport completion, and same-instance portal movement. These cover parts of Java packet ordering but are caller-specific and not yet a bind-point live movement adapter.
- `QueueDelayedTeleportAsync` models `SM_TELEPORT_LOC` delayed animation paths, but bind-point hotspot final movement uses `TeleportAnimation.NONE`, so that packet must not be emitted for the hotspot final movement path.
- `SendDelayedTeleportCompletionPacketsAsync` models same-instance and map/instance-change owner packet branches, including instance-opened self message, but it is tied to pending teleport completion rather than bind-point immediate movement.
- Kisk revive sends `SM_PLAYER_SPAWN` before same-world `SM_PLAYER_INFO`/`SM_STATS_INFO`/`SM_MOTION`, while Java `spawnOnSameMap` sends `SM_CHANNEL_INFO`, `SM_PLAYER_INFO`, `SM_STATS_INFO`, and `SM_MOTION` before `World.spawn`. Do not reuse kisk ordering blindly for bind-point same-instance movement.
- `BindPointTeleportTeleportToSideEffectPlanService` now models the same-instance and map/instance-change ordering as non-live metadata and keeps unsupported side effects explicit.
- Live bind-point dispatch remains unwired in `GameServerConnection`.

## Gap Matrix

| Java Behavior | Current C# Coverage | Gap / Risk |
|---|---|---|
| Final one-second gate checks about-to-die before dead | `BindPointTeleportFinalMovementPlanService` | Source-derived unit tests exist; no live lifestat/concurrency check. |
| Overload uses current heading and `TeleportAnimation.NONE` | `BindPointTeleportFinalMovementPlanService` | Intent only; no live `PlayerTeleportService` call. |
| Cross-world target instance defaults to `1` | `BindPointTeleportFinalMovementPlanService` | Intent only; no live world-map registration or instance-state update. |
| Core `teleportTo` revives if player became dead after the final gate | Not modeled by bind-point planner | Needs explicit decision before live movement because bind-point final gate blocks dead state, but Java still has this race fallback inside `TeleportService`. |
| Duel loss before `sendLoc` | Not modeled for bind-point | Needs future duel-service dependency or documented staging gap. |
| `abortPlayerActions` closes store, cancels skill, clears target, unsets ride | Not modeled for bind-point | Missing private-store, skill-cancel, target-clear, and ride-mode side effects. |
| `World.despawn(player, deleteAnimation)` before movement | Some caller-specific registry fanout exists | Need a bind-point-specific visible delete/fanout boundary and persistent known-list parity. |
| `TeleportAnimation.NONE` runs `SpawnTask` immediately and sends no `SM_TELEPORT_LOC` | Planner records animation string | Need live adapter to avoid queueing pending teleport or sending `SmTeleportLoc`. |
| `SpawnTask.run` no-ops if player already spawned | Not modeled | Requires live spawned-state ownership before execution parity can be claimed. |
| World/instance change calls leave-map and leave-instance callbacks | Some portal/kisk comments mention pending side effects | Not implemented for bind-point movement. |
| Player and pet positions are both moved | Player position mutation helpers exist | Pet position mutation remains unverified. |
| Same-instance owner packets: `SM_CHANNEL_INFO`, `SM_PLAYER_INFO`, `SM_STATS_INFO`, `SM_MOTION` | Existing packet classes and some helper paths | Need bind-point-specific same-instance order test; kisk revive order differs. |
| Same-instance `World.spawn(player/pet)`, protection, effect icons, zone update, port animation reset | Partial zone revalidation exists in some paths | Protection/effect icon callbacks and world spawn ownership are incomplete. |
| Map/instance-change owner packets: `SM_CHANNEL_INFO`, `SM_PLAYER_SPAWN`, optional instance-opened message | `SendDelayedTeleportCompletionPacketsAsync` has similar path | Needs bind-point immediate `NONE` adapter and tests; full `CM_LEVEL_READY` continuation remains separate. |
| Legion member world refresh | Not modeled for bind-point | Needs legion service/runtime dependency. |

## Recommended Live Insertion Order

1. Add a non-live `TeleportService.teleportTo` side-effect planner for bind-point hotspot movement. It should consume final movement intent facts and emit ordered side-effect steps without mutating player state.
2. Unit test both same-instance and map/instance-change branches against the Java ordering in this audit.
3. Include explicit gap flags for dead-after-gate revive fallback, duel loss, action aborts, despawn/spawn, pet movement, protection/effect/zone callbacks, and legion refresh.
4. Only after that, add a live adapter that reuses existing packet classes and movement helpers while preserving `TeleportAnimation.NONE` semantics.
5. Keep `GameServerConnection` `CmBindPointTeleport` dispatch disabled until live scheduler ownership, Kinah mutation, cooldown mutation/fanout, and movement side effects can be run in Java order.

## Do Not Wire Yet

- Do not call `QueueDelayedTeleportAsync` for bind-point final movement; that path sends `SM_TELEPORT_LOC`, while Java hotspot movement with `TeleportAnimation.NONE` does not.
- Do not reuse kisk revive packet order as bind-point same-instance packet order without a dedicated test.
- Do not skip `abortPlayerActions`, `World.despawn`, or the same-instance/map-change packet branch just because the destination coordinates are already planned.
- Do not claim Java parity for pet movement, zone/protection callbacks, legion refresh, or instance leave behavior until those side effects have executable C# coverage.

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

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None in UOW-1210 | Documentation audit | Java `TeleportService.teleportTo`, `sendLoc`, `SpawnTask`, and `spawnOnSameMap` | Documents source-derived side-effect order before code is written. | Manual source inspection only. | No executable side-effect planner or Java runtime comparison in this unit. |

## Remaining Risks

- Live `GameServerConnection` still does not dispatch `CmBindPointTeleport`.
- The bind-point pipeline still has no live scheduler, inventory mutation, cooldown-map mutation, packet fanout, or final movement.
- Existing C# teleport helpers cover only pieces of Java `TeleportService`; bind-point same-instance ordering and map-change ordering need focused tests before reuse.
- Java race behavior where `TeleportService.teleportTo` revives a player who became dead after the final gate is not modeled.
- Action aborts, private-store closure, current-skill cancellation, target clearing, ride-mode removal, pet movement, world despawn/spawn, protection/effect/zone callbacks, instance/leave-map callbacks, and legion refresh remain unported or unverified.
- No Java runtime comparison was executed. Reflection behavior did not change; serialization, threading, date/time, movement, known-list, and persistence parity remain unverified for live bind-point teleport.

## Next Recommended Unit of Work

Add a non-live bind-point `TeleportService.teleportTo` side-effect planner that converts the final movement intent into ordered Java side-effect metadata. It should cover same-instance and map/instance-change branches, explicitly mark unsupported action-abort/despawn/spawn/pet/legion/protection gaps, and add focused unit tests without enabling live `GameServerConnection` dispatch.

## Summary Metrics

- Total Java artifacts discovered: 7 grouped artifact rows in this unit
- Total artifacts ported: 0 live artifacts; 1 read-only side-effect audit completed
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: 6 grouped categories: live connection dispatch, live scheduler/cooldown ownership, live inventory mutation/packets, bind-point movement side-effect planner, persistent known-list parity, and Java runtime comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete
