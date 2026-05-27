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

## Update After UOW-1275

Population packet-construction diagnostics can now report complete/partial/blocked metadata across candidate plans. This improves readiness visibility only; live bind-point fanout remains disabled because the diagnostic projection does not hydrate runtime facts, execute `KnownList`/`PlayerController`, send sockets, model pet visibility, or validate Java runtime packet order.

## Update After UOW-1276

Population diagnostics can now distinguish candidate-consumed request-level packet facts from generated fact-plan facts and generated facts ignored by request precedence. This remains C# staging metadata only; Java has no request/generated distinction because it reads live player/controller/effect/stat state.

## Update After UOW-1277

The ride attack-speed audit confirms known-list ride packet construction should still require supplied or explicitly resolved attack-speed facts. C# has a partial visual-stats approximation, but no reusable Java-equivalent `PlayerGameStats.getAttackSpeed()` resolver for known-list packet facts yet.

## Update After UOW-1278

`PlayerKnownListAttackSpeedFactResolverService` can now produce disabled approximate attack-speed facts from supplied player inventory and item templates. Live bind-point fanout remains blocked because the resolver is not Java stat parity, is not wired into live known-list packet planning, and does not model `Stat2`, effects, caps, duplicate modifiers, or live stat invalidation.

## Update After UOW-1279

Known-list fact planning can now consume an explicit disabled attack-speed resolver result while preserving supplied fact precedence. Live bind-point fanout remains blocked because no live stat hydration, resolver auto-composition from runtime state, socket dispatch, Java packet capture, or Java `Stat2`/modifier parity exists yet.

## Update After UOW-1280

Population planning can now opt into disabled attack-speed resolver auto-composition from supplied subject snapshots plus item templates. Live bind-point fanout remains blocked because the composition is still snapshot/static-data based, current-speed stat parity is missing, and no live socket dispatch or Java packet capture has been performed.

## Update After UOW-1281

Population packet-construction diagnostics now report ride attack-speed fact source and resolver status. Live bind-point fanout remains blocked because this is diagnostic metadata only; Java-equivalent current-stat calculation, live stat hydration, socket dispatch, and Java packet capture are still missing.

## Update After UOW-1282

`PlayerKnownListAbnormalEffectFactResolverService` can normalize supplied abnormal-effect snapshots into packet-construction facts with no-show toggle and slot filtering metadata. Live bind-point fanout remains blocked because the resolver does not hydrate live `EffectController` maps, compute Java remaining-time values, consume resolver facts in fact planning, send sockets, or validate Java runtime packet output.

## Update After UOW-1283

Known-list fact planning can now consume an explicit disabled abnormal-effect resolver result while preserving supplied fact precedence. Live bind-point fanout remains blocked because population planning does not auto-attach abnormal-effect resolver results, diagnostics do not yet surface abnormal-effect source/status counts, live `EffectController` hydration and timer calculation remain missing, socket dispatch is disabled, and no Java runtime packet capture has been performed.

## Update After UOW-1284

Population packet-construction diagnostics now report abnormal-effect fact source and resolver status. This improves readiness visibility only; live bind-point fanout remains blocked because population planning still does not auto-attach abnormal-effect resolver results, live `EffectController` hydration and Java remaining-time calculation are missing, socket dispatch is disabled, and no Java runtime packet capture has been performed.

## Update After UOW-1285

Population planning can now opt into disabled abnormal-effect resolver auto-composition from supplied subject snapshots and supplied abnormal-effect snapshot entries. Live bind-point fanout remains blocked because this is still snapshot based, live `EffectController` hydration/ordering/no-show classification/timer calculation are missing, pet visibility and full packet-order validation are incomplete, socket dispatch is disabled, and no Java runtime packet capture has been performed.

## Update After UOW-1286

A deterministic helper now models Java `Effect.getRemainingTimeToDisplay()` from explicit duration/end-time/current-time snapshots, including permanent, NPC 24h, upper-overflow, and expired negative cases. Live bind-point fanout remains blocked because duration/end-time production is not wired to live effects, resolver callers still supply remaining-time values, live `EffectController` hydration is missing, socket dispatch is disabled, and no Java runtime packet capture has been performed.

## Update After UOW-1287

A snapshot-entry factory can now create abnormal-effect packet entries from supplied packet-facing fields, preserving explicit remaining-time values or computing them from deterministic timing snapshots. A read-only packet-order audit also confirmed Java sends player info/motion/ride/stance, then abnormal effects, then dependent pet visibility retry. Live bind-point fanout remains blocked because live `EffectController` hydration, full `SkillTargetSlot` mapping, `SM_PET`/`SM_PET_EMOTE` serializers, pet visibility side effects, socket dispatch, and Java runtime packet capture are still missing.

## Update After UOW-1288

A non-live pet visibility/order planner now captures Java's dependent pet visibility retry after the master player visibility callback, including pet spawn, optional flying emote, pet dismiss, and viewer-unspawned skip metadata. Live bind-point fanout remains blocked because `SM_PET`/`SM_PET_EMOTE` serializers, pet object/common-data models, full pet visibility predicates, live known-list pet update integration, socket dispatch, and Java runtime packet capture are still missing.

## Update After UOW-1289

A pet packet field audit now scopes the minimal known-list serializer prerequisite to `SM_PET` spawn/dismiss and `SM_PET_EMOTE` fly-start, while documenting broader Java pet action layouts as out of scope for the first slice. Live bind-point fanout remains blocked because no C# pet packet serializers, pet snapshot DTOs, golden vectors, live pet visibility integration, socket dispatch, or Java runtime packet capture exist yet.

## Update After UOW-1290

Minimal C# packet prerequisites now exist for known-list pet visibility: `SmPet` can serialize Java-shaped spawn/dismiss payloads from supplied snapshots, `SmPetEmote` can serialize the fly-start/default branch, and `PetAction`/`PetEmote` ids preserve Java unknown fallback behavior. Live bind-point fanout remains blocked because packet construction is not bridged from pet visibility descriptors, live pet/common-data hydration is missing, full pet packet coverage is incomplete, socket dispatch is disabled, and no Java runtime packet capture has been performed.

## Update After UOW-1291

Pet visibility descriptors can now be bridged to non-sending packet-construction metadata for `SmPet` spawn/dismiss and `SmPetEmote` fly-start using supplied snapshots. Live bind-point fanout remains blocked because the bridge is not attached to population/operation diagnostics, live pet/common-data hydration is missing, full pet packet coverage is incomplete, socket dispatch is disabled, and no Java runtime packet capture has been performed.

## Update After UOW-1292

Population diagnostics can now carry optional non-sending dependent pet packet-construction metadata and count constructed or blocked pet packets from supplied pet spawn snapshots. Live bind-point fanout remains blocked because active pet/common-data hydration is missing, full pet packet coverage is incomplete, live known-list pet retry execution and socket dispatch are disabled, and no Java runtime packet capture has been performed.

## Update After UOW-1293

A disabled/non-live pet spawn snapshot provider shape now validates the Java-required `SM_PET(Pet)` spawn fields before producing `SmPetSpawnSnapshot` and preserves `Player.isInFlyingState()` metadata for fly-start planning. Live bind-point fanout remains blocked because the provider consumes supplied metadata rather than active pet/common-data/template/move-controller state, full pet packet coverage is incomplete, socket dispatch is disabled, and no Java runtime packet capture has been performed.

## Update After UOW-1294

Population pet diagnostics can now consume optional provider inputs, count provider results/statuses, and surface provider blockers such as missing move-controller targets alongside blocked pet packet construction. Live bind-point fanout remains blocked because provider inputs are still supplied metadata, live pet/common-data/template/move-controller hydration is missing, full pet packet coverage is incomplete, socket dispatch is disabled, and no Java runtime packet capture has been performed.

## Update After UOW-1295

A Java pet golden-vector design note now scopes the first runtime capture batch to known-list `SM_PET` spawn, `SM_PET_EMOTE` fly-start, and `SM_PET` dismiss packets, with movement-emote and full toy-pet management packets split into later batches. Live bind-point fanout remains blocked because no Java runtime vectors were generated, C# still lacks live pet/common-data/template/move-controller hydration, movement emote serializers are incomplete, socket dispatch is disabled, and full pet packet coverage remains out of scope.

## Update After UOW-1296

`SmPetEmote` now serializes Java-shaped `MOVE_STOP` and `MOVETO` payload branches from supplied movement snapshots, while preserving the existing default branch for fly-start. Live bind-point fanout remains blocked because no Java runtime vectors were generated, C# still lacks live pet/move-controller hydration, `CM_PET_EMOTE` parser/runtime side effects are unported, and socket dispatch remains disabled.

## Update After UOW-1297

A guarded C# schema-v1 pet vector artifact reader/comparator now exists for future known-list pet Java runtime outputs. When artifacts are present under `parity-artifacts/known-list-pet/java`, it compares Java body/canonical payload hex for spawn, dismiss, fly-start, move-stop, and move-to packet scenarios against `SmPet` / `SmPetEmote`; when artifacts are absent it reports the gap without claiming parity. Live bind-point fanout remains blocked because Maven is unavailable locally for Java artifact generation, live pet/common-data/template/move-controller hydration is missing, `CM_PET_EMOTE` parser/runtime side effects are unported, full `SM_PET` action coverage is incomplete, socket dispatch is disabled, and no Java runtime packet captures have been generated.

## Update After UOW-1298

`SmPet` now supports the selected Java `SM_PET(PetAction)` action-only packets for merchant, minder, house adopt, and house abandon, with tests confirming that each writes only the action id. Live bind-point fanout remains blocked because Java runtime vectors are still missing, `EXTEND_EXPIRATION` call-site behavior is unverified, broader `SM_PET` branches still need common-data/template/feed/mood/doping snapshots, live pet management dispatch is absent, and socket dispatch remains disabled.

## Update After UOW-1299

`SmPet` now supports the Java `SM_PET(int, String)` rename packet shape as source-derived serialization: action id, pet object id, and pet name. Live bind-point fanout remains blocked because Java runtime vectors are still missing, live rename validation/persistence/dispatch is absent, C# null-name behavior is stricter than unverified Java behavior, broader `SM_PET` branches still need common-data/template/feed/mood/doping snapshots, and socket dispatch remains disabled.

## Update After UOW-1300

`SmPet` now supports the Java `SM_PET(PetCommonData, false)` surrender packet shape from a supplied `SmPetSurrenderSnapshot`, writing template id, object id, and the two Java zero placeholders. Live bind-point fanout remains blocked because Java runtime vectors are still missing, the unsafe-looking Java `SM_PET(int,int)` overload needs call-site audit, live surrender validation/persistence/dispatch is absent, `writePetData` is still unported for load/adopt, and socket dispatch remains disabled.

## Update After UOW-1301

Read-only audits confirm `SM_PET(int,int)` should remain unported because it has no direct in-repo call site and would likely fail during Java surrender serialization, while `EXTEND_EXPIRATION` should remain out of the action-only response allow-list because Java `CM_PET` handles action `15` as a silent no-op with no `SM_PET` response. A `writePetData` design audit now identifies the required future snapshot inputs for list/adopt packets. Live bind-point fanout remains blocked because Java runtime vectors are still missing, `writePetData` is unimplemented, pet common-data/template/feed/doping projections are missing, full `CM_PET` runtime behavior is unported, and socket dispatch remains disabled.

## Update After UOW-1302

The checked-in pet static data was structurally audited: 218 pet templates, max two packet-writable functions among warehouse/food/doping/loot, and zero templates exceeding Java's packet comment for writable functions. Total XML functions can be higher because non-written `BAG`/`WING` entries are common. Live bind-point fanout remains blocked because `writePetData` is still unimplemented, feed/doping/timing projections are missing, public list/adopt constructors remain disabled, Java runtime vectors are still missing, and socket dispatch remains disabled.

## Update After UOW-1303

`SmPet` now has an internal deterministic `writePetData` helper with packet-facing snapshots and source-derived tests for no, one, and two writable function cases, including Java order and doping slot padding. Public `LOAD_PETS` and `ADOPT` constructors remain disabled. Live bind-point fanout remains blocked because Java runtime vectors are still missing, live pet common-data/template/feed/doping hydration is absent, full `CM_PET` parser/runtime remains unported, and socket dispatch remains disabled.

## Update After UOW-1304

`SmPet.Adopt(SmPetDataSnapshot)` now exposes the Java `SM_PET(PetCommonData, true)` packet shape using the deterministic `writePetData` helper. Live bind-point fanout remains blocked because live adoption runtime, inventory mutation, pet list insertion, DAO persistence, expiration timer registration, Java runtime vectors, full `CM_PET`, and socket dispatch remain unported.

## Update After UOW-1305

`SmPet.LoadPets(...)` now exposes the Java `SM_PET(Collection<PetCommonData>)` packet shape from supplied ordered pet-data snapshots. Live bind-point fanout remains blocked because live pet-list hydration/DAO reads, Java collection-order confirmation, feed/doping/timing projection, Java runtime vectors, full `CM_PET`, and socket dispatch remain unported.

## Update After UOW-1306

`CmPet` now parses Java opcode `22` metadata for `ADOPT`, `SURRENDER`, `SPAWN`, `DISMISS`, `FOOD`, `RENAME`, and `MOOD`, and the client packet factory registers it as in-game only. Live bind-point fanout remains blocked because `CM_PET.runImpl` side effects, `EXTEND_EXPIRATION`, pet service/adoption/spawn/mood runtime behavior, live pet common-data hydration, Java runtime vectors, and socket dispatch remain unported.

## Update After UOW-1307

`CmPetEmote` now parses Java opcode `21` metadata for functional-pet movement/default emotes, including `MOVE_STOP`, `MOVE_POSITION_UPDATE`, `MOVETO`, default emotion-style branches, and unknown-emote fallback. Live bind-point fanout remains blocked because `CM_PET_EMOTE.runImpl`, active-pet lookup, negative-coordinate rejection, pet world position mutation, move-controller target updates, visible-player broadcast predicates, Java runtime vectors, and socket dispatch remain unported.

## Update After UOW-1308

`SmPet` now supports the Java `SPECIAL_FUNCTION` autoloot/autosell activation packet shapes and autoloot NPC notification shape from supplied snapshots, plus a C# `PetSpecialFunction` id resolver. Live bind-point fanout remains blocked because live pet service mutation, pet common-data state, NPC loot state, autosell item filtering, the dedicated doping packet branch, Java runtime vectors, and socket dispatch remain unported.

## Update After UOW-1309

`SmPet` now supports the Java dedicated doping `SPECIAL_FUNCTION` constructor shape for dope actions add, remove, switch, and use from supplied snapshots. Live bind-point fanout remains blocked because live `PetService.useDoping`, pet doping bag state, inventory lookup/mutation, cooldown/buff behavior, persistence, Java runtime vectors, and socket dispatch remain unported.

## Update After UOW-1310

A read-only pet runtime dependency map now captures the Java service/model/repository/timer dependencies behind `CM_PET` and `CM_PET_EMOTE`. Live bind-point fanout remains blocked because C# still lacks live pet common data, pet repository/list hydration, food/mood packet branches, pet service planners, scheduled feed/doping/mood behavior, pet world object mutation, Java runtime vectors, and socket dispatch.

## Update After UOW-1311

`SmPet` now supports the Java `FOOD` packet branch for subtypes `1` through `8` from supplied snapshots, preserving Java's header-only behavior for unknown subtypes. Live bind-point fanout remains blocked because C# still lacks live `PetFeedProgress` bit packing, `PetCommonData.getRefeedDelay()` date/time calculation, feed scheduler/task state, inventory/feed mutation, present/reward flow, persistence, Java runtime vectors, and socket dispatch.

## Update After UOW-1312

`SmPet` now supports the Java `MOOD` packet branch for subtypes `0`, `2`, `3`, and `4` from supplied snapshots, preserving Java's action-only behavior for unknown mood subtypes. Live bind-point fanout remains blocked because C# still lacks live mood-point timing, packet-time `PetCommonData` mutations, mood/gift cooldown date-time behavior, inventory reward flow, `PetMoodService` validation/dispatch, persistence, Java runtime vectors, and socket dispatch.

## Update After UOW-1313

A standalone C# `PetFeedProgress` helper and `PetHungryLevel` enum now preserve Java feed-progress bit packing, saved-data decode, total-point masking, unsigned regular-count view, loved-feed reset behavior, and hungry-level cycling. Live bind-point fanout remains blocked because the helper is not yet wired to live pet common data, feed calculator thresholds, DAO load/save, scheduled feed/refeed behavior, inventory mutation, Java runtime vectors, or socket dispatch.

## Update After UOW-1314

A standalone C# `PetCommonDataTiming` helper now preserves Java birthday epoch conversion, refeed delay mutation, mood-point lazy start and packet cap, Java-style millisecond-to-second rounding, mood/gift cooldown remaining-time resets, shuggle counter increment gating, and mood-stat reset behavior from supplied time inputs. Live bind-point fanout remains blocked because the helper is not yet wired to a live pet common-data model, `ThreadPoolManager`-style refeed scheduling, feed/doping template initialization, pet DAO load/save, expirable callbacks, packet dispatch, Java runtime vectors, or socket dispatch.

## Update After UOW-1315

Non-executing C# `PlayerPetsRepositoryPlan` command plans now preserve Java `PlayerPetsDAO` SQL text and positional parameter order for feed status, doping CSV, reuse time, insert, delete, load, rename, mood save, and used-id load. Live bind-point fanout remains blocked because no live repository execution, row materialization, timestamp binding verification, exception fallback behavior, doping CSV load parsing, transaction/autocommit validation, Java runtime DB comparison, or socket dispatch was enabled.

## Update After UOW-1316

A non-live C# `PlayerPetRowProjection` helper now models the deterministic row-materialization portion of Java `PlayerPetsDAO.getPlayerPets`, including supplied template-function gates, feed-progress hydration, mood/refeed timing transfer, null despawn-time fallback, and doping CSV slot projection. Live bind-point fanout remains blocked because SQL execution, Java `DataManager` template lookup, live `PetCommonData`, Java DAO outer-catch partial-list behavior, live `PetDopingBag` synchronized mutation/dirty flag, timestamp DB binding verification, Java runtime DB comparison, and socket dispatch remain disabled.

## Update After UOW-1317

A standalone C# `PetDopingBag` helper now preserves Java food/drink defaults, dynamic slot expansion, dirty flag behavior, scroll-slot views, invalid-slot rejection, and scroll-only switch semantics. Live bind-point fanout remains blocked because the helper is not yet wired into `PlayerPetRowProjection`, live pet common data, live `PetService.useDoping`, dirty-flag persistence triggers, item lookup/cooldown/skill use, Java runtime vectors, DB comparisons, or socket dispatch.

## Update After UOW-1318

`PlayerPetRowProjection` now hydrates Java `dopings` CSV into the standalone `PetDopingBag` helper, including Java `SetItem` slot order, non-zero load-time dirty behavior, and all-zero slot expansion without dirtying. Live bind-point fanout remains blocked because live pet repository execution, Java `DataManager` template lookup, full `PetCommonData`, dirty-flag persistence triggers, live `PetService.useDoping`, Java runtime DB comparison, and socket dispatch remain disabled.

## Update After UOW-1319

A non-live C# `PetFeedCalculator` helper now preserves Java feed-point table math, five-level item buckets, normal feed progress mutation, strict threshold switching, loved-feed full transition, and loved-limit no-op behavior. Live bind-point fanout remains blocked because reward selection, `PetFlavour.processFeedResult`, static data full-count generation, item-template lookup, random reward choice, live feed service mutation, DAO writes, scheduler behavior, Java runtime comparison, and socket dispatch remain disabled.

## Update After UOW-1320

`PetFeedCalculator` now has a non-live reward-selection helper with supplied point tables, reward item levels, and loved-reward selector injection, preserving Java guard/null behavior, normal reward threshold indexing, rounding clamps, loved singleton short-circuit, and highest-allowed-level filtering. Live bind-point fanout remains blocked because static feed XML full-count loading, `PetFlavour.processFeedResult`, food group lookup, live item-template lookup, Java RNG runtime comparison, live feed mutation, DAO writes, scheduler behavior, Java runtime comparison, and socket dispatch remain disabled.

## Update After UOW-1321

A non-live C# `PetFeedPlanner` helper now preserves supplied-data `PetFlavour.processFeedResult` behavior, including reward-group lookup by food type, normal-feed progress mutation and not-full null return, reward return after the calculator reaches `FULL`, loved-feed state marking, loved-limit no-op behavior, and `isLovedFood` predicate behavior. Live bind-point fanout remains blocked because `PetFlavour.getFoodType`, `DataManager.ITEM_GROUPS_DATA` lookup, XML/JAXB static data, live item-template lookup, Java RNG runtime comparison, live feed mutation, DAO writes, scheduler behavior, and socket dispatch remain disabled.

## Update After UOW-1322

A non-live C# `PetFoodTypeLookup` helper now preserves supplied-data `PetFlavour.getFoodType` and `ItemGroupsData.isFood` behavior, including ordered reward-group scanning, `EXCLUDES`/`STINKY` rejection before type checks, direct type membership, Java `MISCELLANEOUS` junk-group matching, and null result when no reward group matches. Live bind-point fanout remains blocked because XML pet food loading, `DataManager.ITEM_GROUPS_DATA`, `ItemRaceEntry` validation, live item lookup, live feed mutation, DAO writes, scheduler behavior, Java runtime comparison, and socket dispatch remain disabled.

## Update After UOW-1323

A non-live C# `PetFeedXmlProjection` helper now projects Java `pet_feed.xml` into id-keyed flavour data, preserving Java class defaults, reward groups, loved flags, duplicate-id replacement, sorted positive full-count discovery, and checked-in XML shape counts. Live bind-point fanout remains blocked because Java JAXB/schema validation, global `DataManager.PET_FEED_DATA`, item-template lookup, item-group lookup, live feed mutation, DAO writes, scheduler behavior, Java runtime comparison, and socket dispatch remain disabled.

## Update After UOW-1324

A non-live C# `PetFeedEvaluation` helper now composes projected feed data, supplied item groups, supplied item levels, full-count point tables, food-type lookup, loved-limit gating, planner mutation, and reward selection for one offline feed attempt. Live bind-point fanout remains blocked because inventory decrement/unlock, item reward creation, refeed scheduling, cooldown DAO writes, concrete feed packet ordering, global `DataManager` wiring, Java RNG/runtime comparison, and socket dispatch remain disabled.

## Update After UOW-1325

A non-live C# `PetFeedServiceOperationPlanner` now records Java `PetService.checkFeeding` feed-side operation order around the evaluator, including rejected-food unlock/end/system-message intent, accepted-food decrement intent, repeat-feed scheduling intent, reward packet/add-item/refeed/DAO/reset intent, and cancel no-op behavior. Live bind-point fanout remains blocked because these are operation intents only; concrete packet dispatch, inventory mutation, reward item creation, scheduler execution, DAO writes, localization, Java runtime packet comparison, and socket dispatch remain disabled.

## Update After UOW-1326

A non-sending C# `PetFeedPacketMetadataBridge` now converts packet-facing feed operation intents into concrete `SmPet.Food(...)` metadata for Java FOOD subtypes `2`, `5`, `6`, and `7`, while explicitly marking item unlock, end-feeding emotion context, and rejected-food system-message context as blocked gaps. Live bind-point fanout remains blocked because packet dispatch, item unlock packet construction, emotion/player hydration, localization, inventory mutation, reward creation, scheduler execution, DAO writes, Java runtime packet comparison, and socket dispatch remain disabled.

## Update After UOW-1327

`PetFeedPacketMetadataBridge` can now consume supplied supplemental context to construct non-sending end-feeding `SmEmotion` metadata and rejected-food `SmSystemMessage(1400618, petName, itemName)` metadata, while preserving blocked metadata when that context is absent. Live bind-point fanout remains blocked because item unlock storage packets, live player/pet/item hydration, localization, inventory mutation, reward creation, scheduler execution, DAO writes, Java runtime packet comparison, and socket dispatch remain disabled.

## Update After UOW-1328

`PetFeedPacketMetadataBridge` can now consume supplied normal-cube unlock context to construct the rejected-food unlock metadata pair Java sends before `SM_PET(5)`: `SmInventoryAddItem` with `ALL_SLOT = 0x13`, followed by `SmCubeUpdate`. Live bind-point fanout remains blocked because warehouse unlock packets, live storage/item/template/player hydration, inventory mutation, packet dispatch, scheduler execution, reward creation, DAO writes, Java runtime packet comparison, and socket dispatch remain disabled.

## Update After UOW-1329

`PetFeedPacketMetadataBridge` can now consume supplied regular/account warehouse unlock context to construct Java-shaped rejected-food warehouse unlock metadata: `SmWarehouseAddItem` with `ALL_SLOT = 0x13`, followed by regular warehouse `SmCubeUpdate` snapshots or the account warehouse zero cube-update payload. Live bind-point fanout remains blocked because legion warehouse item/kinah routing, live storage/item/template/player hydration, inventory mutation, packet dispatch, scheduler execution, reward creation, DAO writes, Java runtime packet comparison, and socket dispatch remain disabled.

## Update After UOW-1330

`PetFeedPacketMetadataBridge` can now consume supplied legion warehouse unlock context for both Java branches: ordinary items construct `SmWarehouseAddItem` type `3` followed by legion warehouse `SmCubeUpdate`, while kinah constructs `SmLegionEdit.WarehouseKinah(...)` followed by the same cube update. Live bind-point fanout remains blocked because live storage location mapping, item-template/legion hydration, inventory mutation, packet dispatch, scheduler execution, reward creation, DAO writes, Java runtime packet comparison, and socket dispatch remain disabled.

## Update After UOW-1331

`PetFeedUnlockPacketContextAssembler` can now map supplied item location/storage snapshots into non-live unlock packet context for cube, regular warehouse, account warehouse, and legion warehouse storage ids, while preserving Java's unknown-storage no-send boundary and blocking unmodeled pet/house storage ids. Live bind-point fanout remains blocked because live item-template/player/account/legion storage hydration, inventory mutation, packet dispatch, scheduler execution, reward creation, DAO writes, Java runtime packet comparison, and socket dispatch remain disabled.

## Update After UOW-1332

Non-live rejected-food metadata composition tests now prove the supplied operation plan, unlock context assembler, supplemental context, and packet metadata bridge compose modeled cube/warehouse/legion unlock packet families before `SmPet`, `SmEmotion`, and rejected-food `SmSystemMessage` metadata. Live bind-point fanout remains blocked because live storage ownership/hydration, inventory mutation, packet dispatch, scheduler execution, reward creation, DAO writes, Java runtime packet comparison, and socket dispatch remain disabled.

## Update After UOW-1333

A Java runtime-vector design now scopes the pet feed rewarded `SM_PET` FOOD subtype `7` ambiguity: Java queues the packet before reward item add, refeed scheduling, `setRefeedTime`, DAO persistence, and `PetFeedProgress.reset`, but the packet serializes mutable `PetCommonData` fields when written. Live bind-point fanout remains blocked because no Java subtype `7` runtime artifacts, packet observer hook, deterministic feed fixture, live common-data timing, scheduler execution, reward creation, DAO writes, inventory mutation, or socket dispatch exists.

## Update After UOW-1334

A guarded C# artifact reader now validates the future Java pet feed subtype `7` schema and can compare generated `SM_PET` body/canonical payload hex against `SmPet.Food(...)` when artifacts appear under `parity-artifacts/pet-feed-subtype7/java`. Live bind-point fanout remains blocked because no Java artifacts exist yet, `SM_EMOTION` byte comparison remains out of scope, and live scheduler execution, reward creation, DAO writes, inventory mutation, common-data serialization timing, and socket dispatch remain disabled.

## Update After UOW-1335

The guarded subtype `7` artifact reader can now validate and compare future end-feeding `SM_EMOTION` body/canonical payload hex alongside the `SM_PET` feed packets. A read-only Java send-timing audit also confirmed `PacketSendUtility.sendPacket` is queue-time only and actual serialization occurs later through `AionConnection.writeData` and `AionServerPacket.write`, so subtype `7` runtime artifacts must hook send-time serialization below `PacketSendUtility`. Live bind-point fanout remains blocked because no Java artifacts or observer hook exist yet, and scheduler execution, reward creation, DAO writes, inventory mutation, mutable common-data timing, and socket dispatch remain disabled.

## Update After UOW-1336

A docs-only Java observer implementation plan now scopes the safest future subtype `7` capture hook: a no-op-by-default serialization observer around `AionServerPacket.write` clear bytes after `writeImpl` and length stamping but before encryption, with `AionConnection.writeData` as an optional wire-byte capture point. Live bind-point fanout remains blocked because the Java hook, deterministic pet-feed fixture, generated artifacts, live scheduler execution, reward creation, DAO writes, inventory mutation, mutable common-data timing, and socket dispatch remain disabled.

## Update After UOW-1337

Rejected-food unlock context tests now explicitly preserve the conservative boundary for known but unsupported Java storage ids: pet bags, representative house cabinets, broker, and mailbox locations return `UnsupportedStorageLocation` instead of guessing generic warehouse packet shape. Live bind-point fanout remains blocked because pet/house/broker/mailbox ownership, live storage hydration, storage mutation, packet dispatch, Java runtime bytes, scheduler execution, reward creation, DAO writes, and socket dispatch remain disabled.

## Update After UOW-1338

Rejected-food unlock context tests now cover every Java house storage id `60` through `79` as known but unsupported, alongside pet bags, broker, and mailbox. A read-only snapshot timing audit also confirms future live adapter work must queue the add/unlock packet first, then immediately snapshot `SM_CUBE_UPDATE`, preserving Java's zero count/expand fallback for account warehouse, pet bags, house storage, broker, and mailbox unless an intentional difference is documented. Live bind-point fanout remains blocked because house/pet/broker/mailbox ownership, live storage hydration, storage mutation, packet dispatch, Java runtime bytes, scheduler execution, reward creation, DAO writes, and socket dispatch remain disabled.

## Update After UOW-1339

A docs-only warehouse live-adapter capture design now defines the future rejected-food unlock snapshot boundary: resolve storage id at unlock entry, snapshot item/template/player storage facts after the restore/unlock decision and storage mutation, construct add/unlock metadata first, then immediately construct `SM_CUBE_UPDATE` metadata without yielding. The design also records that `SM_WAREHOUSE_ADD_ITEM` uses `StorageType.getId()` while `SM_CUBE_UPDATE` uses `StorageType.ordinal()`, and that account warehouse/pet/house/broker/mailbox cube updates zero-fill counts/expands in Java. Live bind-point fanout remains blocked because no live adapter, unusual-storage ordinal helper, Java runtime bytes, live ownership/storage hydration, packet dispatch, scheduler execution, reward creation, DAO writes, or socket dispatch exists.

## Update After UOW-1340

`SmCubeUpdate.ZeroSizeForJavaStorageOrdinal` now provides a non-live packet helper for Java unusual-storage cube-update metadata: action `0`, supplied Java ordinal action value, and zero count/expand fields. Focused tests cover pet bag ordinals `4` through `15`, house storage ordinals `16` through `35`, broker ordinal `36`, mailbox ordinal `37`, and byte-range guards. Live bind-point fanout remains blocked because the helper does not resolve storage ids, construct unusual `SM_WAREHOUSE_ADD_ITEM` metadata, hydrate pet/house/broker/mailbox ownership, dispatch packets, compare Java runtime bytes, or mutate storage.

## Update After UOW-1341

`SmCubeUpdate.TryGetJavaStorageOrdinal` and `ZeroSizeForJavaStorageId` now resolve Java storage ids to Java enum ordinals before constructing zero-count cube-update metadata. Tests cover cube/warehouse ids `0` through `3`, pet bag ids `32` through `43`, house storage ids `60` through `79`, broker `126`, mailbox `127`, and unknown-id guards. Live bind-point fanout remains blocked because unusual `SM_WAREHOUSE_ADD_ITEM` metadata, pet/house/broker/mailbox ownership, storage mutation, packet dispatch, Java runtime bytes, scheduler execution, reward creation, DAO writes, and socket dispatch remain disabled.

## Update After UOW-1342

`PetFeedPacketMetadataBridge` now has a guarded non-live `UnusualWarehouse` path that can pair `SmWarehouseAddItem` using the Java storage id with `SmCubeUpdate.ZeroSizeForJavaStorageId` using the Java ordinal/zero-count payload. Focused tests cover representative pet bag, house storage, broker, and mailbox ids plus an unknown-id block. Live bind-point fanout remains blocked because the live assembler still rejects unusual storage ids, full unusual-id bridge coverage is not yet present, Java runtime bytes are missing, pet/house/broker/mailbox ownership and storage hydration are unsupported, and packet dispatch/storage mutation remain disabled.

## Update After UOW-1343

Guarded unusual-storage bridge tests now cover every Java pet bag id `32` through `43`, every house storage id `60` through `79`, broker `126`, and mailbox `127`, verifying warehouse-add storage id and trailing zero-count cube-update ordinal behavior. A read-only timing audit also confirms Java `SM_WAREHOUSE_ADD_ITEM` fixes the warehouse type at construction but reads most item/template/blob fields later at encode time from a live `Item` reference. Live bind-point fanout remains blocked because Java runtime bytes, encode-time mutation parity, pet/house/broker/mailbox ownership and storage hydration, live dispatch, storage mutation, scheduler execution, reward creation, DAO writes, and socket dispatch remain disabled.

## Update After UOW-1344

A docs-only schema-v1 runtime artifact format now exists for unusual-storage rejected-food unlock packets. The schema records pre-delay lookup, post-delay rejected-food decision, construction-time warehouse route fields, encode-time item/blob fields, `SM_WAREHOUSE_ADD_ITEM` bytes, and trailing `SM_CUBE_UPDATE` bytes. It also requires blob entry ids/order, decoded blob fields, template-derived inputs, dynamic item inputs, and time normalization because C# warehouse-add packet shell parity depends on the shared item blob helper. Known item-blob risks include Java `STAT_BONUSES`, fusion random bonus id, temporary exchange/seal flags, plume tempering stat payload, runtime conditioning presence, and wall-clock expiration/dye fields. Live bind-point fanout remains blocked because no Java artifact generator, observer hook, C# reader/comparator, item blob byte comparison, live unusual-storage ownership/storage hydration, packet dispatch, storage mutation, scheduler execution, reward creation, DAO writes, or socket dispatch exists.

## Update After UOW-1345

A guarded C# test-side reader/comparator now exists for future `parity-artifacts/pet-feed-unusual-storage/java` schema-v1 artifacts. It validates schema timing/storage/blob metadata, reconstructs the guarded unusual-storage unlock sequence through `PetFeedPacketMetadataBridge`, checks warehouse-add decoded route fields, and compares `SM_CUBE_UPDATE` body/canonical bytes when present. Live bind-point fanout remains blocked because no Java artifacts or observer hook exist, `SM_WAREHOUSE_ADD_ITEM` byte comparison is still guarded by item-blob serializer gaps, the live assembler still rejects unusual storage ids, and live unusual-storage ownership/storage hydration, packet dispatch, storage mutation, scheduler execution, reward creation, DAO writes, and socket dispatch remain disabled.

## Update After UOW-1346

A docs-only Java observer placement audit now scopes a two-stage disabled-by-default capture design: `ItemPacketService.sendStorageUpdatePacket` should register unusual-storage construction context, and `AionServerPacket.write` after length stamping and before `con.encrypt(...)` should capture clear serialization bytes. `PacketSendUtility.sendPacket` is confirmed queue-only, while `AionConnection.writeData` owns dequeue/connection context but delegates final clear-byte construction to `packet.write`. Live bind-point fanout remains blocked because the Java observer hook, scenario context writer, artifact writer, item-blob decoder, Java runtime artifacts, live unusual-storage ownership/storage hydration, packet dispatch, storage mutation, scheduler execution, reward creation, DAO writes, warehouse-add byte comparison, and socket dispatch remain disabled.

## Update After UOW-1347

The generic Java packet serialization observer shell now exists in `AionServerPacket.write`: a volatile observer defaults to `NoOpServerPacketCaptureObserver`, `setCaptureObserver(null)` resets to no-op, and the callback is guarded after length stamping and before encryption. The disabled path does not create the read-only buffer duplicate unless an observer reports enabled. Live bind-point fanout remains blocked because Maven/Java 25 compile validation is unavailable locally, no enabled observer, scenario context writer, artifact writer, item-blob decoder, Java runtime artifacts, live unusual-storage ownership/storage hydration, packet dispatch, storage mutation, scheduler execution, reward creation, DAO writes, warehouse-add byte comparison, or socket dispatch exists.

## Update After UOW-1348

The Java unusual-storage construction context seam now exists at `ItemPacketService.sendStorageUpdatePacket`, guarded by disabled-by-default `PetFeedUnusualStorageArtifactCapture`. It identifies Java pet bag, house warehouse, broker, and mailbox storage ids but intentionally performs no artifact writing or live behavior while disabled. Live bind-point fanout remains blocked because Java compile validation is unavailable locally, capture enable/config is missing, context-to-byte correlation is missing, artifact writing and item-blob decoding are missing, and the C# live adapter still does not hydrate, mutate, dispatch, or runtime-compare unusual storage.

## Update After UOW-1349

`PetFeedUnusualStorageArtifactCapture` now has a disabled bounded correlation registry and future observer accessor that can advance pending unusual-storage contexts across serialized `SM_WAREHOUSE_ADD_ITEM` then `SM_CUBE_UPDATE` packet observations. Live bind-point fanout remains blocked because the registry is not installed or enabled, Java compile validation is unavailable locally, file output and byte copying are missing, route/packet field validation is missing, and the C# live adapter still does not hydrate, mutate, dispatch, or runtime-compare unusual storage.

## Update After UOW-1350

The disabled unusual-storage capture registry now creates an internal no-output snapshot shape after the correlated warehouse-add/cube-update packet pair, recording storage id, storage ordinal, item object id, packet class names, clear-frame length, encoded opcode, and observer remaining-byte metadata. Live bind-point fanout remains blocked because no observer is installed or enabled, no bytes or files are retained, Java compile validation is unavailable locally, item/blob decoded fields are missing, and the C# live adapter still does not hydrate, mutate, dispatch, or runtime-compare unusual storage.

## Update After UOW-1351

The disabled unusual-storage capture registry now prunes pending contexts older than 30 seconds and records registration/completion timestamps in the no-output snapshot shape. Live bind-point fanout remains blocked because capture is still not installed or enabled, the age bound is a capture safety choice rather than verified gameplay parity, no bytes or files are retained, Java compile validation is unavailable locally, item/blob decoded fields are missing, and the C# live adapter still does not hydrate, mutate, dispatch, or runtime-compare unusual storage.

## Update After UOW-1352

A read-only Java writer/config audit now identifies the future unusual-storage artifact writer prerequisites: existing `fastjson2` dependency, static `@Property` config registration through `Config.CONFIGS`, a configurable safe output path outside production data/logs, and a dedicated bounded writer queue rather than inline packet-serialization file writes. Live bind-point fanout remains blocked because no config key, observer install, writer, byte copy, Java runtime artifact, item/blob decoder, or C# unusual-storage live adapter was enabled.

## Update After UOW-1353

A disabled Java config shell for unusual-storage artifact capture is now registered in `Config.CONFIGS`, with safe defaults for enabled=false, output directory, pending-context bounds, queued-artifact bounds, and allowed scenario. Live bind-point fanout remains blocked because the config is not consumed by capture code yet, no observer is installed, no writer or byte copy exists, Java compile validation is unavailable locally, and the C# live adapter still does not hydrate, mutate, dispatch, or runtime-compare unusual storage.

## Update After UOW-1354

`PetFeedUnusualStorageArtifactCapture` now consumes the registered disabled config for its enabled flag, pending-context bound, scenario name, and output directory while still avoiding observer installation, file output, and byte copying. Live bind-point fanout remains blocked because Java compile validation is unavailable locally, no writer or item/blob decoded fields exist, and the C# live adapter still does not hydrate, mutate, dispatch, or runtime-compare unusual storage.

## Update After UOW-1355

A read-only item-blob decoded-entry audit now maps Java `ItemInfoBlob.getFullBlob` entry order/ids and aligns them with the existing C# known serializer gaps for future unusual-storage warehouse-add artifacts. Live bind-point fanout remains blocked because no Java artifact writer, item-blob decoder, warehouse-add byte comparison, raw byte retention, or C# live adapter hydration/mutation/dispatch/runtime comparison exists.

## Update After UOW-1356

The disabled Java capture registry now carries a no-output construction-time `ItemInfoBlob` metadata snapshot with total blob payload size, ordered entry names, entry ids, and entry payload sizes. Live bind-point fanout remains blocked because encode-time item-blob metadata, artifact writing, byte copying, Java runtime artifacts, warehouse-add byte comparison, C# serializer gap closure, and live unusual-storage hydration/mutation/dispatch/runtime comparison are still missing.

## Update After UOW-1357

`SM_WAREHOUSE_ADD_ITEM` now exposes a passive first-item `ItemInfoBlob` metadata projection, and the disabled observer-side packet snapshot can recompute warehouse-add entry metadata when serialized packets are observed. Live bind-point fanout remains blocked because this is still metadata-only: no raw/canonical bytes, payload-field decoder, artifact writer, Java runtime artifacts, C# serializer gap closure, warehouse-add byte comparison, or live unusual-storage hydration/mutation/dispatch/runtime comparison exists.

## Update After UOW-1358

A read-only key blob payload audit now maps Java `GeneralInfoBlobEntry`, `CompositeItemBlobEntry`, and `EnchantInfoBlobEntry` dynamic fields against current C# serializer gaps. Live bind-point fanout remains blocked because no decoded payload DTOs, Java artifact writer, raw/canonical bytes, cleanup/seal static-data projection, C# serializer gap closure, warehouse-add byte comparison, or live unusual-storage hydration/mutation/dispatch/runtime comparison exists.

## Update After UOW-1359

A read-only small blob payload audit now maps Java conditioning, premium-option, polish, and wrap payload fields against current C# serializer behavior. Live bind-point fanout remains blocked because runtime conditioning presence, decoded payload DTOs, Java artifact writer, raw/canonical bytes, C# serializer verification, warehouse-add byte comparison, and live unusual-storage hydration/mutation/dispatch/runtime comparison are still missing.

## Update After UOW-1360

The disabled Java capture snapshot now has schema-only payload DTOs for audited item-blob fields, covering general, composite, enchant, conditioning, premium-option, polish, and wrap payload inputs. Live bind-point fanout remains blocked because no JSON artifact writer, raw/canonical bytes, C# artifact reader/schema validation, C# serializer gap closure, warehouse-add byte comparison, or live unusual-storage hydration/mutation/dispatch/runtime comparison exists.

## Update After UOW-1361

A read-only fastjson2 writer audit now documents deterministic artifact output prerequisites: dedicated DTOs, stable field ordering, explicit UTF-8, safe filenames, atomic output, and bounded non-blocking writer queue behavior. Live bind-point fanout remains blocked because no writer queue, JSON output, raw/canonical bytes, C# artifact reader/schema validation, C# serializer gap closure, warehouse-add byte comparison, or live unusual-storage hydration/mutation/dispatch/runtime comparison exists.

## Update After UOW-1362

The disabled Java capture registry now has a bounded metadata-only writer queue shell using the configured max queued artifact bound and newest-drop tracking. Live bind-point fanout remains blocked because no writer worker, JSON serialization, file output, raw/canonical bytes, C# artifact reader/schema validation, C# serializer gap closure, warehouse-add byte comparison, or live unusual-storage hydration/mutation/dispatch/runtime comparison exists.

## Update After UOW-1363

The disabled Java capture registry now has a private queue drain method and no-op writer boundary, but no worker invokes it and no JSON or file output exists. Live bind-point fanout remains blocked because no writer worker lifecycle, JSON serialization, file output, raw/canonical bytes, C# artifact reader/schema validation, C# serializer gap closure, warehouse-add byte comparison, or live unusual-storage hydration/mutation/dispatch/runtime comparison exists.

## Update After UOW-1364

The disabled Java capture registry now has private writer worker lifecycle hooks around the queue drain boundary, but the worker is not started automatically and `writeArtifact(...)` remains no-op. Live bind-point fanout remains blocked because no observer install, active worker lifecycle, JSON serialization, file output, raw/canonical bytes, C# artifact reader/schema validation, C# serializer gap closure, warehouse-add byte comparison, or live unusual-storage hydration/mutation/dispatch/runtime comparison exists.

## Update After UOW-1365

A read-only Java lifecycle audit now identifies the future disabled activation boundary for unusual-storage artifact capture: config is loaded before NIO startup in `GameServer`, `AionServerPacket` exposes a process-wide clear-byte observer after length stamping and before encryption, and shutdown should reset the observer/stop the worker before or at the beginning of NIO shutdown. Live bind-point fanout remains blocked because no public capture lifecycle API, observer install, worker start/stop wiring, JSON serialization, file output, raw/canonical bytes, C# artifact reader/schema validation, warehouse-add byte comparison, or live unusual-storage hydration/mutation/dispatch/runtime comparison exists.

## Update After UOW-1366

`PetFeedUnusualStorageArtifactCapture` now has a disabled public lifecycle API shell: `installIfEnabled()` is config-gated and can install the existing packet observer/start the private worker only when explicitly called in a future unit, while `shutdown()` resets the global observer and stops the worker. The API is not wired into `GameServer` or `ShutdownHook`, and `ENABLED` remains false by default. Live bind-point fanout remains blocked because no lifecycle caller, JSON serialization, file output, raw/canonical bytes, C# artifact reader/schema validation, warehouse-add byte comparison, or live unusual-storage hydration/mutation/dispatch/runtime comparison exists.

## Update After UOW-1367

Disabled lifecycle wiring is now present: `GameServer` calls `PetFeedUnusualStorageArtifactCapture.installIfEnabled()` after config/service initialization and before NIO startup, while `ShutdownHook` calls `PetFeedUnusualStorageArtifactCapture.shutdown()` at shutdown start. Because `ENABLED` remains false by default, startup remains no-op and shutdown only resets the observer/worker shell. Live bind-point fanout remains blocked because no JSON serialization, file output, raw/canonical bytes, C# artifact reader/schema validation, warehouse-add byte comparison, runtime lifecycle validation, or live unusual-storage hydration/mutation/dispatch/runtime comparison exists.
