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

Update after UOW-1227: runtime callback execution results now carry Kinah update item and `DecreaseKinahFly` metadata as non-sending fields. Live persistence, packet send, owner/lock, and dispatch remain blocked.

Update after UOW-1228: `docs/Phase-6-BindPointTeleport-KinahPersistenceSend-Policy.md` now records that Java sends during mutation and persists dirty items later. C# live wiring should wait for an owner-checked persistence contract and explicit rollback/send policy.

Update after UOW-1229: `docs/Phase-6-BindPointTeleport-KinahRepositoryContract-Plan.md` now records the owner-checked persistence contract plan and `Saved`/`MissingRow`/`Failed` result statuses. Live wiring should still wait for a non-live bridge that consumes those statuses before packet send, cooldown/fanout, or movement.

Update after UOW-1230: `BindPointTeleportKinahPersistenceDecisionBridgeService` now consumes those supplied persistence statuses and keeps packet/cooldown/fanout/movement metadata blocked unless status is `Saved`. Live SQL, packet send, dispatch, and movement remain disabled.

Update after UOW-1231: `BindPointTeleportKinahInventoryUpdatePacketPlanService` now creates a non-sending `SmInventoryUpdateItem` packet intent only for `ContinueAfterPersistence`. Live send and dispatch remain disabled.

Update after UOW-1232: `BindPointTeleportKinahCallbackResultCompositionService` now composes saved persistence, packet intent, cooldown/action `3` fanout metadata, and final movement metadata in staged order. Live send, SQL, dispatch, fanout execution, and movement remain disabled.

Update after UOW-1233: `BindPointTeleportKinahInventorySendResultPlanService` now models a supplied inventory packet send result. Only `Sent` with `SentPacket=true` can continue to cooldown/action `3` fanout metadata; live `SendPacketAsync` remains disabled.

Update after UOW-1234: `docs/Phase-6-BindPointTeleport-KinahMetadataChain-Readiness.md` now summarizes the completed non-live Kinah metadata chain. The next preferred prerequisite is owner/rollback refinement before any no-op or live send seam.

Update after UOW-1235: `BindPointTeleportKinahOwnerRollbackPlanService` now records original/updated Kinah snapshots plus rollback/commit policy for staged persistence/send outcomes. Live inventory owner/lock is still not implemented.

Update after UOW-1236: `BindPointTeleportKinahInventorySendAdapterPlanService` now records the future `SM_INVENTORY_UPDATE_ITEM` send boundary while disabled and returns no-send metadata without calling `SendPacketAsync`. Live inventory send remains blocked on explicit opt-in wiring, owner locking, repository persistence, and rollback execution.

Update after UOW-1237: `BindPointTeleportKinahPersistenceOperationPlanService` now models the owner-checked SQL contract and supplied row-count/exception mapping without executing SQL. Live bind-point persistence remains blocked on a real repository adapter and owner/rollback execution.

Update after UOW-1238: `BindPointTeleportKinahCallbackOutcomePlanService` now provides a pure scheduled Kinah callback verdict that composes mutation, persistence, disabled send, and rollback metadata. Live callback execution remains blocked on owner/lock, SQL adapter, live send, fanout, and movement work.

Update after UOW-1239: `docs/Phase-6-BindPointTeleport-KinahComposedCallback-Readiness.md` now audits the fully composed non-live scheduled Kinah callback chain. Read-only analysis confirmed Java has no explicit Kinah mutation lock, Java dirty `InventoryDAO` persistence ignores affected rows, and Java bind-point fanout is self-first plus known-list membership rather than C# registry/distance visibility.

Update after UOW-1240: `BindPointTeleportKinahInventoryOwnerService` now provides an in-memory scheduled Kinah owner with apply/rollback under a per-player C# lock. It is not wired into callbacks and does not execute SQL, send packets, fanout, or move.

Update after UOW-1241: `BindPointTeleportKinahInventoryOwnerCallbackBridgeService` now adapts in-memory owner mutation results into scheduled callback mutation and persistence-operation metadata. Live callback execution remains disabled.

Update after UOW-1242: `BindPointTeleportKinahOwnerCallbackOutcomeIntegrationService` now composes the in-memory owner result through persistence, packet-intent, supplied/disabled send, rollback, and callback outcome metadata. Live SQL, sends, fanout, dispatch, and movement remain disabled.

Update after UOW-1243: `BindPointTeleportKinahSendBeforeRuntimeOrderingService` now explicitly gates cooldown/action `3` runtime metadata behind inventory update packet send success, matching Java ordering as non-live metadata.

Update after UOW-1244: `docs/Phase-6-BindPointTeleport-ScheduledKinah-LiveAdapter-FinalReadiness.md` now summarizes satisfied non-live gates and remaining live blockers. Live `GameServerConnection` dispatch remains disabled; the next safe executable seam is a disabled/opt-in SQL repository adapter.

Update after UOW-1255: `docs/Phase-6-BindPointTeleport-PlayerKnownListPopulation-Design.md` now documents full Java player known-list population requirements. Live bind-point teleport dispatch remains blocked because C# still lacks Java-equivalent region-backed `World.spawn`/`updatePosition`/`despawn`, bidirectional `KnownList` population, cached visibility transitions, and controller `see`/`notSee`/`notKnow` packet side effects.

Update after UOW-1256: `PlayerKnownListRegionSnapshotService` now provides a disabled prerequisite model for owner-region plus neighbor-region player candidate selection. Live dispatch remains blocked because the model does not mutate known-list membership, compute range/`canSee`, perform two-way add/remove, or send controller packets.

Update after UOW-1257: `PlayerKnownListRegionMembershipAdapterService` now adapts region snapshots into non-live membership metadata. Live dispatch remains blocked because it is owner-side metadata only and still lacks Java two-way `KnownList.add`, live region storage, visibility recomputation, and packet side effects.

Update after UOW-1258: `PlayerKnownListTwoWayOperationPlanService` now records Java two-way add/remove/clear ordering as descriptors. Live dispatch remains blocked because the planner does not mutate live membership, run range/`canSee`, execute controller packets, or wire scheduled callbacks.

Update after UOW-1259: `PlayerKnownListTwoWayMembershipAdapterService` now applies planned membership steps to non-live metadata only when explicitly enabled. Live dispatch remains blocked because metadata mutation is not Java live world state and packet/controller side effects remain descriptors.

Update after UOW-1260: `PlayerKnownListVisibilityRangePlanService` now models Java range and caller-supplied `canSee` metadata before producing known-list operation plans. Live dispatch remains blocked because this is still descriptor-only and does not execute live region storage or controller side effects.

Update after UOW-1261: `PlayerKnownListPopulationPlanService` now composes region snapshot candidates, range plans, operation plans, and optional metadata mutation. Live dispatch remains blocked because it is not wired to world lifecycle, sockets, scheduler callbacks, movement, or controller packet execution.

Update after UOW-1262: `PlayerKnownListPlayerSideEffectPlanService` now records descriptor-only player `see`/`notSee` packet intent. Live dispatch remains blocked because descriptors are not sent, `SmPlayerInfo` lacks Java enemy/aggro flag behavior, `SmPlayerStance` and `SmAbnormalEffect` are missing, and no controller known-list callback dispatcher is wired.

Update after UOW-1263: `PlayerKnownListOperationSideEffectAttachmentService` now attaches player packet descriptors to operation-plan `see`/`notSee` steps. Live dispatch remains blocked because the attachments are still metadata only and population/fanout/runtime callbacks do not execute them.

Update after UOW-1264: `PlayerKnownListPopulationPlanService` now carries operation side-effect attachments per candidate. Live dispatch remains blocked because the population chain still uses supplied facts, does not send packets, and still lacks packet serializer parity for `SmPlayerInfo(enemy)`, `SmPlayerStance`, and `SmAbnormalEffect`.

Update after UOW-1265: `SmPlayerInfo` now supports Java's enemy creature-type byte (`0x00` vs `0x26`) behind an explicit constructor flag. Live dispatch remains blocked because active-viewer race projection, `SmPlayerStance`, `SmAbnormalEffect`, and controller packet execution are still missing.

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
| Inventory mutation | `tryDecreaseKinah(price, DEC_KINAH_FLY)` with persistence and packet order | Non-live mutation planner added in UOW-1225; callback metadata composition added in UOW-1226; runtime non-sending metadata carry-through added in UOW-1227; persistence/send policy audit added in UOW-1228; repository contract plan added in UOW-1229; persistence-result decision bridge added in UOW-1230; non-sending packet adapter added in UOW-1231; callback composition bridge added in UOW-1232; send-result plan added in UOW-1233; UOW-1222 audit completed; UOW-1223 packet mask coverage added; UOW-1224 owner design completed | Refresh readiness and pick the next prerequisite: owner/rollback refinement or a no-op send adapter seam before dispatch. |
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

## Kinah Metadata Chain Readiness - UOW-1234

The scheduled Kinah metadata chain is complete enough to stop adding more packet/order-only bridges for now:

| Stage | Artifact | Status | Remaining Gate |
|---|---|---|---|
| Mutation metadata | `BindPointTeleportScheduledKinahMutationPlanService` | Non-live unit tested | Needs owner/lock and rollback. |
| Callback metadata | `BindPointTeleportScheduledCallbackPlanService` | Non-live unit tested | Needs live owner result input. |
| Runtime carry-through | `BindPointTeleportRuntimeCallbackExecutionBridgeService` | Unit tested; can execute supplied cooldown/fanout metadata | Still not connected to inventory packet send or movement. |
| Persistence decision | `BindPointTeleportKinahPersistenceDecisionBridgeService` | Unit tested with supplied results | Needs SQL adapter and rollback policy. |
| Packet intent | `BindPointTeleportKinahInventoryUpdatePacketPlanService` | Unit tested, serializes mask `0x4B` | Needs live send adapter. |
| Callback composition | `BindPointTeleportKinahCallbackResultCompositionService` | Unit tested | Supplied metadata only. |
| Send-result gate | `BindPointTeleportKinahInventorySendResultPlanService` | Unit tested with supplied results | Needs live send adapter and owner rollback response. |

Next preferred prerequisite: refine the live owner/rollback contract before adding a send seam. The future owner must know how to restore the original Kinah item when persistence or send fails, and must block cooldown/action `3` fanout/movement on every failure status.

Update after UOW-1235: owner/rollback planning is now represented by `BindPointTeleportKinahOwnerRollbackPlanService`. The next safe prerequisite is a disabled no-op send adapter seam that returns the existing send-result shape without calling `SendPacketAsync`.

Update after UOW-1236: disabled no-op send adapter seam is complete. The next safe prerequisite is an owner-checked repository persistence contract or SQL adapter design; keep live sends and `GameServerConnection` dispatch disabled.

Update after UOW-1237: the pure owner-checked persistence operation contract is complete. The next safe prerequisite is a non-live scheduled Kinah callback outcome composer joining mutation, persistence, disabled send, and rollback metadata.

Update after UOW-1238: the non-live scheduled Kinah callback outcome composer is complete. The next safe prerequisite is a readiness audit for the composed chain before choosing live owner/lock design or SQL adapter readiness.

Update after UOW-1239: composed-chain readiness audit is complete. The next safe executable prerequisite is a pure in-memory Kinah owner contract for apply/rollback semantics, with SQL adapter and live send still disabled.

Update after UOW-1240: the in-memory Kinah owner contract is complete. The next safe prerequisite is a bridge from owner mutation/rollback results into the existing callback outcome chain, still non-live.

Update after UOW-1241: owner-result callback bridge is complete. The next safe prerequisite is a full non-live outcome integration slice proving owner results feed persistence decision, packet intent, send decision, rollback, and callback outcome.

Update after UOW-1242: full non-live owner outcome integration is complete. The next safe prerequisite is a send-before-runtime ordering adapter/design to remove the current metadata-ordering wrinkle before live packet send and cooldown/action `3` fanout are connected.

Update after UOW-1243: send-before-runtime ordering gate is complete. The next safe prerequisite is a live-adapter readiness audit for scheduled Kinah execution before enabling any `GameServerConnection` path.

Update after UOW-1244: final scheduled Kinah live-adapter readiness audit is complete. The next safe prerequisite is a disabled/opt-in owner-checked SQL repository adapter seam, still unwired from dispatch.

Update after UOW-1245: disabled/opt-in owner-checked SQL repository adapter seam is complete and remains unwired from dispatch. The next safe prerequisite is a disabled/opt-in inventory packet send adapter that consumes existing packet intent/send-result policy without enabling live movement or `GameServerConnection`.

Update after UOW-1246: disabled/opt-in inventory packet send adapter seam is complete and remains unwired from dispatch. The next safe prerequisite is known-list fanout parity design or characterization before any scheduled Kinah callback can broadcast action `3` live.

Update after UOW-1247: current C# action `3` fanout approximation is characterized with concrete same-world/95m recipients, but Java known-list parity is still not achieved. The next safe prerequisite is a non-live known-list-backed fanout plan or expected Java trace model.

Update after UOW-1248: expected Java source-first plus known-list-recipient fanout is modeled as non-live trace metadata. Live known-list membership storage/execution remains blocked, so `GameServerConnection` dispatch should still stay disabled.

Update after UOW-1249: C# now has `PlayerKnownListMembershipService` and `BindPointTeleportKnownListFanoutMembershipAdapterService` as metadata-only prerequisites for Java known-list fanout. They model owner/source exclusion, object-id deduplication, and invisible known-player retention, but they are not populated by live world visibility and do not execute socket sends, source-online checks, or per-recipient exception handling.

Update after UOW-1250: C# now has `BindPointTeleportKnownListFanoutSendPolicyService`, which models Java `PacketSendUtility.sendPacket` online gating and `CollectionUtil.forEach` log-and-continue behavior as metadata. Live socket sends, live online-state lookup, Java logging, and `GameServerConnection` dispatch remain disabled.

Update after UOW-1251: C# now has `BindPointTeleportKnownListFanoutExecutionPlanService`, a disabled source-first executor composition. It proves membership snapshots, trace metadata, and send-policy metadata compose without sending packets. Live known-list population, socket execution, movement, and dispatch remain blocked.

Update after UOW-1252: C# now has `BindPointTeleportKnownListFanoutSocketExecutorService`, a disabled-by-default opt-in socket boundary that consumes the known-list execution plan. It is not wired into `GameServerConnection`; live known-list population and scheduled callback dispatch remain blocked.

Update after UOW-1253: C# now has `PlayerKnownListMembershipRefreshService`, which can seed player known-list metadata from supplied online players and `WorldVisibility`. This is only a distance-based approximation; Java-equivalent region known-list population, controller side effects, and live wiring remain blocked.

Update after UOW-1254: C# now has `PlayerKnownListMembershipRegistryRefreshAdapterService`, a disabled adapter from online connection-registry snapshots into the refresh approximation. It remains unwired and is not Java world/region known-list parity.

## Do Not Wire Yet

- Do not add a live `GameServerConnection` branch for `CmBindPointTeleport` in the next unit.
- Do not schedule real 10 second tasks until cancel/replace ownership exists.
- Do not mutate Kinah from bind-point callbacks until failure messages, persistence, and update packets are tested.
- Do not call live `PlayerTeleportService` for final movement until the `TeleportAnimation.NONE` packet order has an executable adapter.
- Do not claim Java known-list parity from distance-based registry fanout.
- Do not claim Java known-list parity from the new metadata store until live population, source-online gating, and per-recipient send behavior are executed or objectively compared.

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

## Update After UOW-1266

`SmPlayerInfo` now has both Java's enemy creature-type flag and a scalar viewer race projection model. Live bind-point fanout remains blocked because the live adapter still needs runtime active-player facts, controller packet construction, `SmPlayerStance`, `SmAbnormalEffect`, socket dispatch ordering, scheduled task ownership, and Java runtime packet validation.

## Update After UOW-1267

`SmPlayerStance` is now available as a focused packet serializer and the known-list stance descriptor references it. Live bind-point fanout remains blocked because the live adapter still needs runtime active-player facts, descriptor-to-packet construction, `SmAbnormalEffect`, socket dispatch ordering, scheduled task ownership, and Java runtime packet validation.

## Update After UOW-1268

`SmAbnormalEffect` is now partially available as a supplied-facts serializer. Live bind-point fanout remains blocked because the live adapter still needs runtime active-player facts, effect-controller hydration, descriptor-to-packet construction, socket dispatch ordering, scheduled task ownership, and Java runtime packet validation.

## Update After UOW-1269

Descriptor-to-packet construction metadata now exists for player `see`/`notSee` side-effect plans. Live bind-point fanout remains blocked because this bridge is non-live, requires supplied runtime facts, does not send packets, and is not yet applied across population-side attachment plans.

## Update After UOW-1270

Operation attachment packet-construction metadata now exists and can preserve directional operation-step order. Live bind-point fanout remains blocked because population plans do not carry this metadata end to end, runtime facts are supplied, and no socket dispatch or Java runtime validation occurs.

## Update After UOW-1271

Population plans can now carry operation-level packet construction metadata per candidate when supplied subject facts are available. Live bind-point fanout remains blocked because runtime player/motion/effect/ride fact hydration, live known-list population, controller execution, socket dispatch, scheduled callback wiring, and Java runtime validation are still missing.

## Update After UOW-1272

The packet fact hydration audit confirms live bind-point fanout should remain disabled. C# has partial scalar sources for motions, ride info, stance, and abnormal masks, but still lacks a Java-equivalent active viewer context adapter, reusable attack-speed stat resolver for known-list packet construction, live effect-entry/timer hydration, socket dispatch ordering, and Java runtime validation.

## Update After UOW-1274

Population fact-plan composition now preserves per-direction blocked fact metadata and packet construction metadata, but live bind-point fanout remains disabled. The live adapter still lacks active viewer context, stat/effect hydration, Java-equivalent known-list population, socket execution, and runtime packet validation.
