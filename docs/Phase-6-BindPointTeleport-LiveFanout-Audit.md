# Phase 6 Bind-Point Teleport Live Fanout Audit

Date: May 26, 2026
Unit of Work: UOW-1201
Scope: Read-only insertion audit for Java `CM_BIND_POINT_TELEPORT` / `BindPointTeleportService` live fanout.
Source of truth: Java project.

## Audit Result

Live bind-point teleport wiring should remain disabled until the client packet boundary is ported. Java registers `CM_BIND_POINT_TELEPORT` at opcode `244`, but C# currently has no opcode `244` registration and no `CmBindPointTeleport` client packet class. The existing C# bind-point work is therefore correctly staged as planners plus `SmBindPointTeleport` serialization only.

Update after UOW-1202: C# now has parser-only `CmBindPointTeleport` coverage and opcode `244` registration. Live handler composition, dead-player guard execution, cooldown/task ownership, fanout, Kinah mutation, and movement remain disabled.

Update after UOW-1203: C# now has a non-live `BindPointTeleportClientActionPlanService` that models Java `CM_BIND_POINT_TELEPORT.runImpl` dispatch selection. It records dead-player no-op, action `1` teleport intent, action `2` cancel intent, and unknown-action no-op without executing live side effects.

Update after UOW-1204: C# now has a non-live `BindPointTeleportFanoutPlanService` that records Java bind-point hotspot packet fanout semantics: `broadcastPacket(player, packet, true)` and `broadcastPacketAndReceive(player, packet)` both include the source player, then Java known-list players. C# live wiring should use `BroadcastToVisiblePlayersAsync(..., includeSourcePlayer: true)` while documenting that distance-based visibility is still an approximation until persistent known-list membership is ported.

Update after UOW-1205: C# now has a non-live `BindPointTeleportRequestPlanService` that composes `CM_BIND_POINT_TELEPORT.runImpl` action planning with staged operation/control and fanout planners. It preserves Java request branching for dead players, unknown actions, action `1` teleport readiness/blocking, and action `2` cancel readiness/no-op without enabling live packet sends, scheduler callbacks, cooldown map mutation, Kinah mutation, or movement.

Update after UOW-1206: C# now has a non-live `BindPointTeleportRuntimeStatePlanService` that models Java `TaskId.SKILL_USE` task-slot semantics and bind-point cooldown map facts. It records `SKILL_USE` ordinal `16`, `addTask` replacement/cancel behavior, `cancelTask` remove-then-`cancel(false)` behavior, 10 second skill-use scheduling, 600 second cooldown insertion, and whole-second cooldown time-left truncation without scheduling real callbacks or mutating live cooldown state.

Update after UOW-1207: C# now has a non-live `BindPointTeleportScheduledKinahPlanService` that models the first branch inside Java's delayed `TaskId.SKILL_USE` callback: `tryDecreaseKinah(price, DEC_KINAH_FLY)` succeeds and continues, or fails and sends `STR_CANNOT_MOVE_TO_AIRPORT_NOT_ENOUGH_FEE` before returning. It records the Java item update mask `0x4B` without mutating inventory or sending packets.

Update after UOW-1208: C# now has a non-live `BindPointTeleportFinalMovementPlanService` that models Java's final delayed movement gate: `!player.getLifeStats().isAboutToDie() && !player.isDead()` before `TeleportService.teleportTo(player, hotspot.worldId, hotspot.x, hotspot.y, hotspot.z)`. It records same-world/current-instance versus cross-world/instance-`1` target selection without moving the player.

Update after UOW-1209: C# now has a non-live `BindPointTeleportScheduledCallbackPlanService` that composes the Java delayed `TaskId.SKILL_USE` callback order: Kinah decrement failure returns before cooldown/fanout/movement; Kinah success adds cooldown intent, cooldown fanout intent, schedules the final movement gate, and only includes movement intent when the final gate passes. It still performs no live scheduler, inventory, cooldown-map, packet-send, or movement side effects.

Update after UOW-1210: `docs/Phase-6-BindPointTeleport-TeleportTo-Audit.md` now maps Java `TeleportService.teleportTo(player, worldId, x, y, z)` side effects for hotspot final movement. The audit confirms bind-point live movement must still wait for an explicit `TeleportAnimation.NONE` side-effect plan because Java also performs action aborts, world despawn/spawn, same-instance versus map-load packet branching, pet movement, protection/effect/zone callbacks, leave-map/instance callbacks, and legion refresh.

Update after UOW-1211: C# now has a non-live `BindPointTeleportTeleportToSideEffectPlanService` that models the audited `TeleportAnimation.NONE` path as ordered side-effect metadata. It covers blocked final movement, same-instance `spawnOnSameMap` owner packet order, map/instance-change `SM_CHANNEL_INFO` + `SM_PLAYER_SPAWN` order, optional instance-opened message, and explicit gap flags for unsupported Java side effects. Live bind-point movement remains disabled.

Update after UOW-1212: `BindPointTeleportScheduledCallbackPlanService` can now carry the non-live teleport side-effect plan after the final movement intent. This composes scheduled Kinah success, cooldown insert, action `3` fanout, final movement gate, and concrete `TeleportService.teleportTo` side-effect metadata in Java order without enabling scheduler execution, packet sends, or movement.

Update after UOW-1213: `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md` now records the live-adapter gates before `GameServerConnection` dispatch. It keeps live bind-point teleport blocked until concrete failure system-message helpers, a no-op handler composition bridge, runtime task/cooldown ownership, live Kinah mutation/persistence, source-included fanout, and live movement side effects are independently handled.

Update after UOW-1214: `SmSystemMessage` now has named helpers and packet tests for Java bind-point failure messages `STR_CANNOT_MOVE_TO_AIRPORT_NOT_ENOUGH_FEE` (`1300689`), `STR_CANNOT_MOVE_TO_AIRPORT_NO_ROUTE` (`1300691`), and `STR_FLYING_TIME_NOT_READY` (`1300961`). Live dispatch remains disabled.

Update after UOW-1215: C# now has a non-live `BindPointTeleportHandlerCompositionPlanService` that composes parsed bind-point packet values with supplied operation/control/callback facts. It produces existing request/fanout/callback metadata without sending packets or mutating state. `GameServerConnection` remains unwired.

Update after UOW-1216: `docs/Phase-6-BindPointTeleport-RuntimeOwner-Design.md` documents the runtime owner required before live bind-point fanout can be wired. Action `2` fanout still depends on Java `hasTask(TaskId.SKILL_USE)` / `cancelTask(TaskId.SKILL_USE)` semantics, and action `3` login/callback fanout still depends on a shared cooldown owner.

Update after UOW-1217: C# now has an isolated `BindPointTeleportRuntimeStateOwner` that can supply the Java `hasTask`/`cancelTask` and cooldown facts required by future action `2` and action `3` fanout bridges. Fanout remains unwired, and Java known-list/source-inclusion parity remains unverified.

Update after UOW-1218: `BindPointTeleportRuntimeControlBridgeService` now consumes the runtime owner for action `2` cancel and action `3` login cooldown packet intents. It does not send packets; future fanout work can use its `ShouldSendPacket` and `SmBindPointTeleport` intent without duplicating task/cooldown lookup logic.

Update after UOW-1219: `BindPointTeleportRuntimeFanoutService` can now execute the runtime control bridge packet intents through `IGameClientConnectionRegistry.BroadcastToVisiblePlayersAsync` with `includeSourcePlayer: true`. This covers action `2` cancel and login action `3` source-inclusion tests, but it still uses the C# visible-player registry approximation rather than Java's persistent `KnownList`.

## Java Flow

Java source files:

- `game-server/src/com/aionemu/gameserver/network/aion/AionClientPacketFactory.java`
- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_BIND_POINT_TELEPORT.java`
- `game-server/src/com/aionemu/gameserver/services/teleport/BindPointTeleportService.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_BIND_POINT_TELEPORT.java`

Observed Java behavior:

1. `AionClientPacketFactory` registers opcode `244` as `CM_BIND_POINT_TELEPORT` for `State.IN_GAME`.
2. `CM_BIND_POINT_TELEPORT.readImpl` reads `action` as `readC()`.
3. Only action `1` reads `locId` with `readD()` and `kinah` with `readQ()`.
4. `runImpl` returns immediately if `player.isDead()`.
5. Action `1` calls `BindPointTeleportService.teleport(player, locId, kinah)`.
6. Action `2` calls `BindPointTeleportService.cancelTeleport(player, locId)`.
7. `BindPointTeleportService.teleport` performs hotspot lookup, price reconciliation, requirement checks, start broadcast, a 10 second `TaskId.SKILL_USE` task, scheduled Kinah decrement, cooldown insertion, cooldown broadcast, and a final 1 second delayed `TeleportService.teleportTo` if the player is not dead or about to die.
8. `cancelTeleport` only broadcasts action `2` if the player controller has `TaskId.SKILL_USE`.
9. `onLogin` uses the static cooldown map and sends action `3` through `broadcastPacketAndReceive` when time remains.

## Current C# State

C# surfaces reviewed:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientPacketFactory.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerTeleportService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportPricePlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportRequirementsPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportOperationPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportControlPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportClientActionPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportFanoutPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportRequestPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportRuntimeStatePlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportScheduledKinahPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportFinalMovementPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportScheduledCallbackPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportTeleportToSideEffectPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportHandlerCompositionPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmBindPointTeleport.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`

Observed C# state:

- `GameClientPacketFactory` registers opcode `244` for parser-only `CmBindPointTeleport`.
- `CmBindPointTeleport` reads Java action `1` fields and leaves action `2`/unknown fields at Java defaults.
- `GameServerConnection` has no bind-point teleport handler branch.
- `PlayerTeleportService` supports immediate/pending teleport helpers, but no hotspot/cooldown/task/Kinah mutation ownership.
- `ThreadPoolManager` and `ScheduledTask` can represent delayed work, and `BindPointTeleportRuntimeStatePlanService` now records the bind-point-specific `TaskId.SKILL_USE` state boundary, but no live task slot is wired.
- `IGameClientConnectionRegistry.BroadcastToVisiblePlayersAsync` can approximate Java `PacketSendUtility.broadcastPacket(..., true)` for visible-player fanout, but Java `broadcastPacketAndReceive` source-player inclusion semantics must be explicitly mapped before live use.
- `BindPointTeleportRuntimeFanoutService` now maps action `2` and login action `3` runtime control bridge packet intents to `BroadcastToVisiblePlayersAsync(..., includeSourcePlayer: true)`. Known-list membership parity remains unverified.
- `SmBindPointTeleport` exists and is source-derived unit tested for opcode `296` and action payloads.
- Price, requirements, operation, control, client action, fanout, request-composition, runtime-state, scheduled-Kinah, final-movement, scheduled-callback, and teleport side-effect planners exist; scheduled callback composition can carry side-effect metadata and remains non-live.

## Recommended Live Insertion Order

1. Add `CmBindPointTeleport` parser and opcode `244` registration, with packet-read tests for action `1`, action `2`, dead-player no-op metadata, and unknown-action no-op behavior. Done non-live in UOW-1202/UOW-1203.
2. Add a handler-level non-live composition bridge that consumes `CmBindPointTeleport` and produces existing planner/control outputs without changing world state. Request-level composition is staged in UOW-1205; live `GameServerConnection` dispatch remains unwired.
3. Add an explicit bind-point runtime state owner for cooldowns and the cancellable `TaskId.SKILL_USE` task equivalent. Non-live state semantics are staged in UOW-1206, UOW-1216 documents the owner design, and UOW-1217 adds an isolated owner; live fanout remains unwired.
4. Wire fanout only after packet parser, planner composition, cooldown state, and task ownership are independently tested.
5. Add live Kinah mutation and final movement as separate units because both affect inventory persistence, packet order, and movement/known-list fanout.
6. Before final movement, add a non-live `TeleportService.teleportTo` side-effect planner for the bind-point hotspot `TeleportAnimation.NONE` path using `docs/Phase-6-BindPointTeleport-TeleportTo-Audit.md`.

## Do Not Wire Yet

- Do not register opcode `244` directly to live teleport side effects in the same unit as parser creation.
- Do not schedule the 10 second task until cancellation ownership is tested.
- Do not mutate Kinah from a scheduled callback until the failure packet and inventory update behavior are modeled.
- Do not call `PlayerTeleportService` for the final movement until Java same-world/world-change packet order is selected for hotspot teleport.
- Do not reuse delayed `SM_TELEPORT_LOC` paths for hotspot final movement; Java bind-point movement uses `TeleportAnimation.NONE` and runs `SpawnTask` immediately.
- Do not add live `GameServerConnection` dispatch until the live-adapter readiness gates in `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md` are satisfied or intentionally staged with tests.

## Migration Parity Table - UOW-1201

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_BIND_POINT_TELEPORT` | future `Aion.GameServer.Network.Aion.ClientPackets.CmBindPointTeleport` | Client Packet / Parser | Not Started | No Tests | Unknown | Java opcode `244` reads action byte, and only action `1` reads `locId` and `kinah`. C# has no parser or packet-factory registration. Missing methods: parser, state registration, handler dispatch, dead-player guard, action no-op behavior. Serialization is not applicable because this is a client packet. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` | `Aion.GameServer.Services.BindPointTeleportOperationPlanService`; future live handler/service | Service / Movement | Partial | Unit Tested | Needs Verification | Staged price, requirements, operation order, and packet intents exist. Live hotspot lookup, packet fanout, `TaskId.SKILL_USE` scheduling, Kinah mutation, cooldown map, death/about-to-die recheck, and final movement remain unported. Threading behavior is unverified. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.cancelTeleport` | `Aion.GameServer.Services.BindPointTeleportControlPlanService`; future live handler/service | Service / Control Flow | Partial | Unit Tested | Needs Verification | Non-live cancel intent exists. Live task lookup/cancel and action `2` visible fanout remain unported. Threading/cancellation behavior is unverified. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.onLogin` | `Aion.GameServer.Services.BindPointTeleportControlPlanService`; future enter-world cooldown bridge | Service / Login Control Flow | Partial | Unit Tested | Needs Verification | Non-live login cooldown packet intent exists. Static cooldown ownership, date/time-left calculation, and `broadcastPacketAndReceive` source-player inclusion remain unported. Date/time and threading behavior are unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_BIND_POINT_TELEPORT` | `Aion.GameServer.Network.Aion.ServerPackets.SmBindPointTeleport` | Server Packet / Serialization | Partial | Unit Tested | Needs Verification | Opcode `296` and action payload branches are source-derived unit tested. No Java runtime packet capture or live broadcast comparison was run. |
| `com.aionemu.gameserver.utils.PacketSendUtility` | `Aion.GameServer.Network.Aion.IGameClientConnectionRegistry`; `GameServerConnection.SendPacketAsync` | Fanout Utility Dependency | Partial | Manual Only | Needs Verification | Existing C# fanout helpers can send to visible players and specific players, but Java `broadcastPacket(..., true)` and `broadcastPacketAndReceive` semantics need explicit source-inclusion and visibility tests for hotspot packets. |
| `com.aionemu.gameserver.model.TaskId.SKILL_USE` | future bind-point runtime task owner using `Aion.GameServer.Utils.ScheduledTask` | Scheduler Dependency | Partial | Manual Only | Needs Verification | C# has general scheduling primitives and unrelated pending item-use task patterns, but no bind-point `TaskId.SKILL_USE` slot. Cancellation and callback ordering remain unported. |
| `com.aionemu.gameserver.services.teleport.TeleportService.teleportTo` | `Aion.GameServer.Services.PlayerTeleportService` | Movement Dependency | Partial | Manual Only | Needs Verification | C# has immediate and pending teleport helpers, but no bind-point final movement path. Java death/about-to-die recheck, same-world/world-change behavior, known-list packet order, and persistence side effects remain unverified. |

## Remaining Risks

- Parser parity is currently blocked by the missing `CmBindPointTeleport` class and opcode `244` registration.
- Live fanout semantics need source-player inclusion tests before Java `broadcastPacket(..., true)` or `broadcastPacketAndReceive` can be claimed.
- Cooldown storage is a static Java map keyed by player object id; C# needs an explicit owner and concurrency policy.
- Scheduled Kinah decrement can fail after initial requirement checks; this race is still only metadata in C#.
- Death/about-to-die recheck occurs after the final 1 second delay, not at request time only.
- No Java runtime comparison was executed. Reflection behavior did not change; serialization remains source-derived for `SmBindPointTeleport`; date/time, threading, movement, and persistence parity are unverified.

## Next Recommended Unit of Work

Add `CmBindPointTeleport` parser coverage and opcode `244` registration without live side effects. The parser unit should prove Java field reads for action `1`, action `2`, and unknown/no-op actions, then route only to a non-live handler/composition placeholder or leave dispatch documented until the next unit.

## Summary Metrics

- Total Java artifacts discovered: 8 grouped artifact rows in this unit
- Total artifacts ported: 0 live artifacts; 1 read-only insertion audit completed
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 7 grouped rows
- Total blocked artifacts: 5 grouped categories: client parser/registration, live fanout, cooldown/task state, Kinah mutation, and final movement/death checks
- Estimated overall migration completion: Phase 6 remains about 71% complete
