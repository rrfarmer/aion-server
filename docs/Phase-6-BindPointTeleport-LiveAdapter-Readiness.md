# Phase 6 Bind-Point Teleport Live Adapter Readiness

Date: May 26, 2026
Unit of Work: UOW-1213
Scope: Read-only readiness checklist for live `CM_BIND_POINT_TELEPORT` adapter work.
Source of truth: Java project.

## Readiness Result

Live bind-point teleport dispatch is not ready. The C# port has strong non-live metadata coverage for parser, request selection, price/requirements, packet intents, fanout semantics, runtime task/cooldown facts, scheduled Kinah branch, final movement gate, `TeleportService.teleportTo` side-effect order, and scheduled callback composition. However, Java live behavior still depends on several executable boundaries that are either missing or only represented as planner metadata.

Do not wire `GameServerConnection` to execute bind-point teleport yet. The next safest implementation work is a small prerequisite slice, not live dispatch: either add concrete system-message helpers/packet tests for bind-point failure messages or add a handler-level no-op composition bridge that consumes parsed `CmBindPointTeleport` and returns non-live plans without sending packets or mutating state.

Update after UOW-1214: C# now has named `SmSystemMessage` helpers and packet tests for the three bind-point failure messages. Live dispatch remains blocked on handler composition, runtime task/cooldown ownership, inventory mutation/persistence, source-included fanout execution, and movement side-effect execution.

Update after UOW-1215: C# now has `BindPointTeleportHandlerCompositionPlanService`, a non-live handler-level composition bridge that consumes parsed `CmBindPointTeleport` scalar values plus supplied operation/control/callback facts and returns existing request/callback metadata. It still sends no packets, schedules no tasks, mutates no state, and does not touch `GameServerConnection`.

Update after UOW-1216: `docs/Phase-6-BindPointTeleport-RuntimeOwner-Design.md` now pins the live runtime owner requirements for Java `TaskId.SKILL_USE` and the static bind-point cooldown map. It confirms the next code slice should be an isolated singleton owner, keyed by player object id, before any `GameServerConnection` dispatch or live scheduler callback is wired.

Update after UOW-1217: C# now has `BindPointTeleportRuntimeStateOwner`, an isolated live-capable owner for per-player `TaskId.SKILL_USE` schedule/replace/cancel/clear operations and player-id keyed cooldown facts. It is not wired into `GameServerConnection`, sends no packets, mutates no inventory, and does not execute bind-point teleport callbacks from the network path.

Update after UOW-1218: C# now has `BindPointTeleportRuntimeControlBridgeService`, a non-sending bridge that consumes `BindPointTeleportRuntimeStateOwner` facts for action `2` cancel and action `3` login cooldown plans. It can cancel the owner task slot and produce existing `SmBindPointTeleport` packet intents, but it still does not send packets or wire live client dispatch.

Update after UOW-1219: C# now has `BindPointTeleportRuntimeFanoutService`, an isolated adapter that can broadcast runtime control bridge packet intents through `IGameClientConnectionRegistry.BroadcastToVisiblePlayersAsync(..., includeSourcePlayer: true)`. Tests cover no-packet, action `2`, and login action `3` fanout calls. Full `GameServerConnection` dispatch remains disabled, and C# visible-distance fanout is still only an approximation of Java persistent known-list membership.

Update after UOW-1220: C# now has `BindPointTeleportRuntimeScheduledCallbackBridgeService`, a metadata-only action `1` bridge that schedules supplied callback metadata through `BindPointTeleportRuntimeStateOwner` using Java `TaskId.SKILL_USE` timing/replacement semantics. It deliberately does not mutate Kinah, insert cooldowns, broadcast action `3`, or move the player.

Update after UOW-1221: C# now has `BindPointTeleportRuntimeCallbackExecutionBridgeService`, a callback-side bridge that consumes supplied scheduled callback metadata and, only when Kinah success is already represented by that metadata, inserts the runtime cooldown and broadcasts action `3` through the existing runtime fanout adapter. It still does not mutate Kinah or execute final movement.

Update after UOW-1222: `docs/Phase-6-BindPointTeleport-ScheduledKinah-LiveBoundary-Audit.md` now pins the live scheduled Kinah boundary. The Java `tryDecreaseKinah(price, DEC_KINAH_FLY)` callback branch mutates storage, sends `SM_INVENTORY_UPDATE_ITEM` with mask `0x4B`, and marks storage update-required before cooldown/fanout. C# should first add a named packet mask/test, then a dedicated inventory mutation/persistence boundary, before any live bind-point callback dispatch.

Update after UOW-1223: C# now has named `SmInventoryUpdateItem.DecreaseKinahFly = 0x4B` packet coverage. Inventory mutation is still not live; the remaining gate is a dedicated Kinah mutation/persistence boundary that preserves Java packet ordering.

Update after UOW-1224: `docs/Phase-6-BindPointTeleport-KinahMutationOwner-Design.md` now defines the future scheduled Kinah mutation owner shape. The next safe code slice is a non-live mutation planner; live persistence and dispatch remain blocked.

Update after UOW-1225: `BindPointTeleportScheduledKinahMutationPlanService` now models the future in-memory scheduled Kinah mutation result and inventory-update packet intent. It is still non-live; live persistence, packet send, and `GameServerConnection` dispatch remain blocked.

Update after UOW-1226: `BindPointTeleportScheduledCallbackPlanService` now composes optional Kinah mutation metadata before cooldown/fanout metadata. Runtime execution still does not send the inventory update or persist the mutation.

## Java Live Flow

Java source files:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_BIND_POINT_TELEPORT.java`
- `game-server/src/com/aionemu/gameserver/services/teleport/BindPointTeleportService.java`
- `game-server/src/com/aionemu/gameserver/services/teleport/TeleportService.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_BIND_POINT_TELEPORT.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`

Java execution order for action `1`:

1. `CM_BIND_POINT_TELEPORT.runImpl` returns immediately when `player.isDead()`.
2. Action `1` dispatches `BindPointTeleportService.teleport(player, locId, kinah)`.
3. `teleport` looks up the hotspot template; missing template audits and sends `STR_CANNOT_MOVE_TO_AIRPORT_NO_ROUTE`.
4. It calculates distance/client-price reconciled price.
5. `checkRequirements` enforces start-world, race, Kinah, and cooldown order.
6. Requirement failures send only the Java messages used by that branch:
   - invalid start world -> `STR_CANNOT_MOVE_TO_AIRPORT_NO_ROUTE`
   - invalid race -> audit only, no packet
   - not enough Kinah -> `STR_CANNOT_MOVE_TO_AIRPORT_NOT_ENOUGH_FEE`
   - active cooldown -> `STR_FLYING_TIME_NOT_READY`
7. Success broadcasts `SM_BIND_POINT_TELEPORT(action=1)` with source included.
8. It stores a 10 second `TaskId.SKILL_USE` scheduled task.
9. The scheduled task tries `tryDecreaseKinah(price, ItemUpdateType.DEC_KINAH_FLY)`.
10. Scheduled Kinah failure sends `STR_CANNOT_MOVE_TO_AIRPORT_NOT_ENOUGH_FEE` and returns.
11. Scheduled Kinah success stores a 600 second cooldown, broadcasts `SM_BIND_POINT_TELEPORT(action=3)` with source included, then schedules a final one-second movement gate.
12. Final movement calls `TeleportService.teleportTo(player, hotspot.worldId, hotspot.x, hotspot.y, hotspot.z)` only if not about to die and not dead.

Java execution order for action `2`:

1. `CM_BIND_POINT_TELEPORT.runImpl` returns immediately when `player.isDead()`.
2. Action `2` dispatches `BindPointTeleportService.cancelTeleport(player, locId)`.
3. `cancelTeleport` no-ops unless `player.getController().hasTask(TaskId.SKILL_USE)`.
4. When present, it cancels `TaskId.SKILL_USE` and broadcasts `SM_BIND_POINT_TELEPORT(action=2)` with source included.

Java login flow:

1. `BindPointTeleportService.onLogin` looks up the static player-id cooldown map.
2. If time left is positive, it broadcasts-and-receives `SM_BIND_POINT_TELEPORT(action=3)` with source included.

## Current C# Coverage

| Surface | Current C# Artifact | Current Evidence | Live Ready? | Notes |
|---|---|---|---|---|
| Client parser and opcode `244` | `CmBindPointTeleport`; `GameClientPacketFactory` | Unit tested parser/registration from prior UOWs | Partially | Parser exists; `GameServerConnection` dispatch does not. |
| Client action selection | `BindPointTeleportClientActionPlanService` | Unit tested | Partially | Non-live only. |
| Price calculation | `BindPointTeleportPricePlanService` | Unit tested | Partially | Uses scalar facts; live hotspot/static-data adapter still needed. |
| Requirement order | `BindPointTeleportRequirementsPlanService` | Unit tested | Partially | Message names are recorded, but concrete `SmSystemMessage` helpers for bind-point failures are missing. |
| Operation intent | `BindPointTeleportOperationPlanService` | Unit tested | Partially | Non-live packet intents only. |
| Request composition | `BindPointTeleportRequestPlanService` | Unit tested | Partially | Non-live and requires supplied facts. |
| `SM_BIND_POINT_TELEPORT` serialization | `SmBindPointTeleport` | Source-derived packet tests | Partially | No Java runtime capture. |
| Fanout source inclusion | `BindPointTeleportFanoutPlanService` | Unit tested | Partially | Live registry still approximates known-list by distance. |
| `TaskId.SKILL_USE` state | `BindPointTeleportRuntimeStatePlanService` | Unit tested | Not ready | No live task slot keyed like Java `CreatureController`. |
| Cooldown static map | `BindPointTeleportRuntimeStatePlanService` | Unit tested | Not ready | No live shared owner/concurrency policy. |
| Scheduled Kinah branch | `BindPointTeleportScheduledKinahPlanService` | Unit tested | Not ready | No live inventory mutation, persistence, or inventory update packet. |
| Scheduled callback composition | `BindPointTeleportScheduledCallbackPlanService` | Unit tested | Partially | Carries side-effect metadata but does not execute. |
| Final movement gate | `BindPointTeleportFinalMovementPlanService` | Unit tested | Partially | Non-live gate and target intent only. |
| `TeleportService.teleportTo` side effects | `BindPointTeleportTeleportToSideEffectPlanService` | Unit tested | Not ready | Side-effect metadata only; no live action abort, despawn/spawn, packet send, or movement. |
| System messages | `SmSystemMessage` | Named helpers and packet tests exist for `1300689`, `1300691`, and `1300961` | Partially | Helpers are ready for future live failure sends, but no live bind-point send path exists. |

## Live Adapter Gates

| Gate | Required Before Live Dispatch | Current Status | Recommended Next Step |
|---|---|---|---|
| Handler boundary | A handler or adapter that consumes `CmBindPointTeleport` and assembles facts without side effects | Non-live bridge added in UOW-1215; runtime control bridge added in UOW-1218; action `1` scheduler bridge added in UOW-1220 | Keep dispatch disabled; action `1` still requires live hotspot/inventory/fanout/movement adapters. |
| Static hotspot facts | Adapter from `DataManager.HOTSPOT_DATA` equivalent to planner facts | Missing/partial | Add fact assembler from loaded hotspot static data when owned exclusively. |
| Failure system messages | Concrete `SmSystemMessage` helpers for Java bind-point failures | Helpers/tests added in UOW-1214 | Use helpers from future non-live/live adapters; do not add dispatch yet. |
| Runtime task owner | Per-player `TaskId.SKILL_USE` task slot with replace/cancel semantics | Isolated owner added in UOW-1217; metadata scheduler bridge added in UOW-1220 | Keep callback side effects disabled until inventory, cooldown, fanout, and movement gates are ready. |
| Cooldown owner | Player-id keyed cooldown storage with Java whole-second time-left behavior | Isolated owner added in UOW-1217; callback insertion bridge added in UOW-1221 | Live Kinah mutation and final movement remain disabled. |
| Inventory mutation | `tryDecreaseKinah(price, DEC_KINAH_FLY)` with persistence and packet order | Non-live mutation planner added in UOW-1225; callback metadata composition added in UOW-1226; runtime bridge still consumes supplied success/failure metadata; UOW-1222 audit completed; UOW-1223 packet mask coverage added; UOW-1224 owner design completed | Carry mutation metadata through runtime execution, then isolate live persistence/send before dispatch. |
| Fanout | Source-included visible-player broadcast matching Java known-list semantics | Runtime control fanout adapter added in UOW-1219; registry approximation exists | Known-list parity remains unverified; action `1` scheduled callback fanout is still unwired. |
| Final movement | Java `TeleportAnimation.NONE` side effects and owner packet order | Planner only | Defer live adapter until action abort/despawn/spawn/pet/callback gaps are owned. |
| Java comparison | Runtime or golden comparison for live packet/order behavior | Missing | Do not mark Verified Parity. |

## System Message Prerequisite

Java bind-point teleport uses these concrete system message IDs:

| Java Helper | Message ID | Current C# State | Readiness |
|---|---:|---|---|
| `SM_SYSTEM_MESSAGE.STR_CANNOT_MOVE_TO_AIRPORT_NOT_ENOUGH_FEE()` | `1300689` | `SmSystemMessage.CannotMoveToAirportNotEnoughFee()` | Ready as named packet helper; live send remains unwired |
| `SM_SYSTEM_MESSAGE.STR_CANNOT_MOVE_TO_AIRPORT_NO_ROUTE()` | `1300691` | `SmSystemMessage.CannotMoveToAirportNoRoute()` | Ready as named packet helper; live send remains unwired |
| `SM_SYSTEM_MESSAGE.STR_FLYING_TIME_NOT_READY()` | `1300961` | `SmSystemMessage.FlyingTimeNotReady()` | Ready as named packet helper; live send remains unwired |

## Recommended Non-Live Insertion Order

1. Add a runtime state owner for `TaskId.SKILL_USE` and cooldowns, still tested without `GameServerConnection` dispatch. Done in UOW-1217 as an isolated owner.
2. Add live fanout tests for `SM_BIND_POINT_TELEPORT` action `1`, `2`, and `3` using source-included registry behavior.
3. Add scheduled Kinah mutation/persistence only after item update packet ordering is concrete.
4. Add live final movement only after action abort/despawn/spawn/pet/zone/legion gaps are either implemented or explicitly staged out with tests.
5. Only then wire `GameServerConnection` to dispatch `CmBindPointTeleport`.

## Do Not Wire Yet

- Do not add a live `GameServerConnection` branch for `CmBindPointTeleport` in the next unit.
- Do not schedule real 10 second tasks until cancel/replace ownership exists.
- Do not mutate Kinah from bind-point callbacks until failure messages, persistence, and update packets are tested.
- Do not call live `PlayerTeleportService` for final movement until the `TeleportAnimation.NONE` packet order has an executable adapter.
- Do not claim Java known-list parity from distance-based registry fanout.

## Parallel Work Discovery

| Candidate | Scope | Files | Risk | Decision |
|---|---|---|---|---|
| A | Live-adapter readiness checklist | new doc plus shared docs | Low/Medium | Completed in UOW-1213. |
| B | Concrete bind-point system-message helpers/tests | `SmSystemMessage.cs`, `GamePacketTests.cs` | Low/Medium | Completed in UOW-1214. |
| C | Handler-level no-op composition bridge | new service/test pair | Medium | Completed in UOW-1215. |
| D | Runtime `TaskId.SKILL_USE` owner | new service/test pair, possible player/connection state | Medium/High | Recommended next design/code slice; keep isolated from dispatch. |
| E | Live `GameServerConnection` dispatch | shared connection file | High | Blocked; do not start yet. |

File ownership for this unit:

| Owner | Scope | Allowed Files | Forbidden Files | Expected Output |
|---|---|---|---|---|
| Orchestrator | Readiness checklist and shared docs | `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`; Phase 6 progress/handoff docs; bind-point audit/map docs | Production C# files; tests; `GameServerConnection.cs` | Read-only readiness checklist, parity/progress/handoff updates |

No sub-agents were spawned in this unit because the chosen scope is documentation plus shared migration state. There are no unused agents to despawn.

## Migration Parity Table - UOW-1213

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_BIND_POINT_TELEPORT` | `Aion.GameServer.Network.Aion.ClientPackets.CmBindPointTeleport`; future live adapter | Client Packet / Handler Boundary | Partial | Unit Tested | Needs Verification | Parser/opcode coverage exists, but live `runImpl` dispatch is not wired. Handler-level no-op composition bridge is recommended before live side effects. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` | existing bind-point planner stack; future live service/adapter | Service / Movement | Partial | Unit Tested | Needs Verification | Non-live metadata covers price, requirements, operation, scheduler/cooldown facts, scheduled Kinah, callback, final movement, and side effects. Live hotspot lookup, task execution, Kinah mutation, cooldown mutation, fanout, and movement remain unported. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.cancelTeleport` | `BindPointTeleportControlPlanService`; future task owner/live adapter | Service / Control Flow | Partial | Unit Tested | Needs Verification | Non-live cancel intent exists. Live `TaskId.SKILL_USE` lookup/cancel and action `2` fanout remain blocked on runtime task owner. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.onLogin` | `BindPointTeleportControlPlanService`; future cooldown owner/login bridge | Service / Login Control Flow | Partial | Unit Tested | Needs Verification | Non-live cooldown packet intent exists. Static cooldown ownership and login broadcast remain unported. Date/time and threading behavior are unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` bind-point failure helpers | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` | Packet / System Message | Partial | Manual Only | Needs Verification | Generic constructor can emit IDs, but named helpers/tests are missing for `1300689`, `1300691`, and `1300961`. Add before live failure sends. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_BIND_POINT_TELEPORT` | `Aion.GameServer.Network.Aion.ServerPackets.SmBindPointTeleport` | Packet / Serialization | Partial | Unit Tested | Needs Verification | Source-derived packet tests exist for action payloads. Live fanout and Java runtime capture remain missing. |
| `com.aionemu.gameserver.services.teleport.TeleportService.teleportTo(Player,int,float,float,float)` | `BindPointTeleportTeleportToSideEffectPlanService`; future live movement adapter | Service / Movement Dependency | Partial | Unit Tested | Needs Verification | Side-effect metadata is composed into callback planning, but live action abort, despawn/spawn, packet sends, pet move, callbacks, and movement remain unported. |

Tests added:

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None in UOW-1213 | Documentation/readiness audit | Java `CM_BIND_POINT_TELEPORT`, `BindPointTeleportService`, `TeleportService`, `SM_SYSTEM_MESSAGE` | Identifies live adapter gates and recommended prerequisite order. | Manual source/C# inspection only. | No executable code or Java runtime comparison in this unit. |

## Remaining Risks

- Live `GameServerConnection` dispatch remains disabled and should stay disabled until prerequisites are satisfied.
- The readiness checklist is manual evidence, not executable parity.
- Missing system-message helpers could cause live failure branches to use ad hoc IDs unless fixed first.
- Runtime task/cooldown ownership, Kinah mutation/persistence, known-list fanout, and movement execution remain unported.
- Reflection behavior did not change. Serialization, threading, date/time, movement, known-list, and persistence parity remain unverified for live bind-point teleport.

## Next Recommended Unit of Work

Add concrete `SmSystemMessage` helpers and packet tests for the bind-point failure messages:

- `STR_CANNOT_MOVE_TO_AIRPORT_NOT_ENOUGH_FEE` -> `1300689`
- `STR_CANNOT_MOVE_TO_AIRPORT_NO_ROUTE` -> `1300691`
- `STR_FLYING_TIME_NOT_READY` -> `1300961`

Keep the unit small: packet helpers/tests only, no `GameServerConnection` dispatch.

## Summary Metrics

- Total Java artifacts discovered: 7 grouped artifact rows in this unit
- Total artifacts ported: 0 live artifacts; 1 read-only readiness checklist completed
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 7 grouped rows
- Total blocked artifacts: 6 grouped categories: live dispatch, runtime task/cooldown ownership, system-message helper coverage, inventory mutation/persistence, known-list fanout, and live movement adapter
- Estimated overall migration completion: Phase 6 remains about 71% complete
