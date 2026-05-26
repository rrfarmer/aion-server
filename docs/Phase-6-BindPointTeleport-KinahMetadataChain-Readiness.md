# Phase 6 Bind-Point Teleport Kinah Metadata Chain Readiness

Date: May 26, 2026
Unit of Work: UOW-1234
Scope: Read-only readiness refresh for the completed non-live scheduled Kinah metadata chain.
Source of truth: Java project.

## Readiness Result

The scheduled bind-point Kinah path now has a complete non-live metadata chain from mutation planning through supplied send-result gating. It is still not live. Do not wire `GameServerConnection`, SQL, `SendPacketAsync`, runtime fanout, or final movement yet.

The chain is ready as a planning/test scaffold, not as executable Java parity:

1. `BindPointTeleportScheduledKinahMutationPlanService` models missing/insufficient, exact-to-zero, positive decrement, and non-positive no-mutation behavior.
2. `BindPointTeleportScheduledCallbackPlanService` carries mutation metadata before cooldown/fanout metadata.
3. `BindPointTeleportRuntimeCallbackExecutionBridgeService` carries Kinah update metadata into callback execution results without sending.
4. `BindPointTeleportKinahPersistenceDecisionBridgeService` gates packet/fanout metadata behind supplied `Saved` persistence.
5. `BindPointTeleportKinahInventoryUpdatePacketPlanService` creates a non-sending `SmInventoryUpdateItem.DecreaseKinahFly` packet intent after saved persistence.
6. `BindPointTeleportKinahCallbackResultCompositionService` composes saved persistence, packet intent, cooldown/action `3` fanout metadata, and movement metadata in staged order.
7. `BindPointTeleportKinahInventorySendResultPlanService` gates cooldown/action `3` metadata behind supplied `Sent` packet-send result.

## Java Facts

Java source files reviewed:

- `game-server/src/com/aionemu/gameserver/services/teleport/BindPointTeleportService.java`
- `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_INVENTORY_UPDATE_ITEM.java`
- `game-server/src/com/aionemu/gameserver/dao/InventoryDAO.java`

Java scheduled success order:

1. `tryDecreaseKinah(price, DEC_KINAH_FLY)`.
2. Mutate storage and send `SM_INVENTORY_UPDATE_ITEM` during `Storage.decreaseItemCount`.
3. Mark storage dirty for later `InventoryDAO.store(player)` persistence.
4. `addCooldown(player, locId)`.
5. Broadcast `SM_BIND_POINT_TELEPORT(action=3)` with source included.
6. Schedule final movement and call `TeleportService.teleportTo` only if the final gate passes.

## Current C# Chain

| Chain Step | C# Artifact | Current Evidence | Live Ready? | Notes |
|---|---|---|---|---|
| In-memory mutation metadata | `BindPointTeleportScheduledKinahMutationPlanService` | Unit tested | No | Non-live snapshot only; no owner/lock/rollback. |
| Callback order metadata | `BindPointTeleportScheduledCallbackPlanService` | Unit tested | No | Carries mutation intent before cooldown/fanout metadata. |
| Runtime metadata carry-through | `BindPointTeleportRuntimeCallbackExecutionBridgeService` | Unit tested | Partial | Can execute cooldown/fanout for supplied success metadata, but still does not send inventory packet or move. |
| Persistence decision | `BindPointTeleportKinahPersistenceDecisionBridgeService` | Unit tested with supplied results | No | No SQL adapter; C# staged persist-before-send differs from Java dirty lifecycle. |
| Packet intent | `BindPointTeleportKinahInventoryUpdatePacketPlanService` | Unit tested; update mask `0x4B` serialized | No | Creates packet object only; no send. |
| Callback composition | `BindPointTeleportKinahCallbackResultCompositionService` | Unit tested | No | Composes supplied metadata only; no effects. |
| Send-result gate | `BindPointTeleportKinahInventorySendResultPlanService` | Unit tested with supplied results | No | No `SendPacketAsync`; conservative gate before cooldown/action `3`. |

## Recommended Next Executable Prerequisite

Prefer an owner/rollback refinement before a no-op live send seam. The metadata chain has enough packet/send planning, but live mutation still lacks the hardest prerequisite: an owner that can apply, persist, roll back, and expose failure without racing other inventory updates.

Recommended next unit:

- Add a non-live owner/rollback contract refinement for scheduled bind-point Kinah mutation.
- Define how the future owner snapshots the original Kinah item, applies the updated item, rolls back on `MissingRow`, `Failed`, missing connection, or failed send, and prevents cooldown/fanout/movement from continuing.
- Keep it docs-only or pure service/test only; no SQL, no real `SendPacketAsync`, no `GameServerConnection`, no movement.

## Migration Parity Table - UOW-1234

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` scheduled callback | bind-point Kinah metadata chain services | Service / Callback Boundary | Partial | Regression Tested | Needs Verification | Non-live metadata chain covers mutation, persistence decision, packet intent, callback order, and supplied send-result gating. Live dispatch remains disabled. |
| `com.aionemu.gameserver.model.items.storage.Storage.tryDecreaseKinah` | `BindPointTeleportScheduledKinahMutationPlanService`; future owner/rollback contract | Storage / Mutation | Partial | Unit Tested | Needs Verification | Planner is non-live and snapshot-based. No C# owner/lock applies or rolls back live inventory. |
| `com.aionemu.gameserver.model.items.storage.Storage.decreaseItemCount` | packet/persistence/send-result metadata chain | Storage / Count Mutation | Partial | Unit Tested | Needs Verification | Java mutates, sends, and marks dirty in one storage path. C# stages saved-persistence and sent-packet gates as intentional safety policy. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemUpdatePacket` | `BindPointTeleportKinahInventoryUpdatePacketPlanService`; `BindPointTeleportKinahInventorySendResultPlanService` | Packet Utility / Send Boundary | Partial | Unit Tested | Needs Verification | Packet object and supplied send result are modeled, but no live send occurs. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem` | Packet / Serialization | Partial | Unit Tested | Needs Verification | C# serializes `0x4B` in tests; no Java runtime byte capture. |
| `com.aionemu.gameserver.dao.InventoryDAO` | planned owner-checked persistence adapter output | Repository / Persistence | Partial | Unit Tested with supplied metadata | Needs Verification | No SQL adapter exists. Java dirty full-row persistence remains broader than planned C# count update. |

Tests added:

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| None in UOW-1234 | Documentation-only readiness refresh. | Manual Java/C# source and existing test review only. |

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 0 live artifacts; 1 readiness refresh completed
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: 1 live inventory owner/rollback contract, 1 live repository adapter, 1 live send adapter, and 1 live dispatch/movement path
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- Metadata chain is non-live and uses supplied results.
- Java packet-before-dirty-persistence behavior still differs from the staged C# persist-before-send/send-result policy.
- No owner/lock or rollback contract has been implemented for live inventory mutation.
- Live SQL, live `SendPacketAsync`, runtime fanout, final movement, and Java known-list parity remain disabled.
- Java runtime packet/storage comparison was not executed.
- Reflection behavior did not change. Date/time behavior did not change. Threading, persistence, serialization, packet-order, and rollback parity remain `Needs Verification`.

## Next Recommended Unit of Work

Add a non-live owner/rollback contract refinement for scheduled bind-point Kinah mutation. Prefer docs-only or a pure planner/test that records original Kinah, updated Kinah, rollback requirement, and stop/continue policy for persistence and send failures. Keep live SQL, send, dispatch, fanout, and movement disabled.

Update after UOW-1235: `BindPointTeleportKinahOwnerRollbackPlanService` now records original/updated Kinah snapshots and rollback/commit policy for supplied persistence/send outcomes. Next, add a disabled no-op send adapter seam or design before any live `SendPacketAsync` call.

Update after UOW-1236: `BindPointTeleportKinahInventorySendAdapterPlanService` now consumes packet intent and returns existing send-result metadata while disabled, with tests proving `SendPacketAsync` is not called. Next, prefer owner-checked repository persistence contract work before any live send/persist callback wiring.
