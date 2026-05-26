# Phase 6 Bind-Point Teleport Kinah Callback Composition Bridge

Date: May 26, 2026
Unit of Work: UOW-1232
Scope: Non-live callback result composition bridge for scheduled bind-point Kinah payment.
Source of truth: Java project.

## Bridge Result

C# now has `BindPointTeleportKinahCallbackResultCompositionService`, a pure composition bridge that joins three already-staged facts:

- saved/missing/failed Kinah persistence decision;
- non-sending Kinah inventory update packet plan;
- supplied runtime callback metadata for cooldown/action `3` fanout/final movement.

The bridge proves the staged order:

1. saved Kinah persistence decision;
2. `SmInventoryUpdateItem.DecreaseKinahFly` packet intent;
3. cooldown metadata;
4. action `3` fanout metadata;
5. final movement schedule/intent metadata when the movement gate passes.

It does not persist, send packets, mutate runtime cooldown state, broadcast fanout, dispatch from `GameServerConnection`, or move the player.

## Java Facts

Java source files reviewed:

- `game-server/src/com/aionemu/gameserver/services/teleport/BindPointTeleportService.java`
- `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_INVENTORY_UPDATE_ITEM.java`

Observed Java scheduled callback order:

1. `tryDecreaseKinah(price, DEC_KINAH_FLY)`.
2. On failure, send not-enough-fee and return.
3. On success, `Storage.decreaseItemCount` sends the inventory update packet during mutation and marks storage dirty.
4. `addCooldown(player, locId)`.
5. Broadcast `SM_BIND_POINT_TELEPORT(action=3)` with source included.
6. Schedule the one-second final movement gate.
7. If not dead/about-to-die, call `TeleportService.teleportTo`.

## C# Implementation

New C# artifacts:

- `Aion.GameServer.Services.BindPointTeleportKinahCallbackCompositionStatus`
- `Aion.GameServer.Services.BindPointTeleportKinahCallbackCompositionStep`
- `Aion.GameServer.Services.BindPointTeleportKinahCallbackComposition`
- `Aion.GameServer.Services.BindPointTeleportKinahCallbackResultCompositionService`

The service returns:

- `StoppedBeforePersistence` when persistence decision is not `ContinueAfterPersistence`;
- `StoppedBeforePacket` when persistence is saved but packet intent is missing;
- `StoppedBeforeRuntimeCallback` when packet intent exists but supplied runtime callback metadata is absent or incomplete;
- `ReadyWithRuntimeCallback` when saved persistence, packet intent, cooldown/fanout metadata, and final movement metadata can be represented in staged order.

## Tests Added

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `CreateComposition_StoppedPersistenceDecisionBlocksPacketCooldownFanoutAndMovement` | Failed persistence decision blocks packet/fanout/movement metadata. | Intentional C# persistence gate before send. |
| `CreateComposition_MissingPacketPlanBlocksRuntimeMetadata` | Saved persistence without packet intent stops before runtime callback metadata. | C# staging guard; Java has packet send during mutation. |
| `CreateComposition_MissingRuntimeResultKeepsPacketButBlocksCooldownFanoutAndMovement` | Packet intent can exist while cooldown/fanout/movement stay blocked without runtime metadata. | Source-derived staged order. |
| `CreateComposition_SavedPacketAndRuntimeMetadataComposeJavaOrderWithMovement` | Saved persistence, packet intent, cooldown, fanout, schedule, and movement intent appear in Java order. | Source-derived order; no Java runtime comparison. |
| `CreateComposition_RuntimeMetadataWithoutMovementKeepsFinalMovementBlocked` | Final movement intent remains absent when the final movement gate blocks. | Source-derived from Java dead/about-to-die gate. |

## Migration Parity Table - UOW-1232

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` scheduled callback | `Aion.GameServer.Services.BindPointTeleportKinahCallbackResultCompositionService` | Service / Callback Composition | Partial | Unit Tested | Needs Verification | C# now composes saved persistence, packet intent, cooldown/fanout metadata, and movement metadata in staged Java order. No live send, SQL, fanout execution, dispatch, or movement. |
| `com.aionemu.gameserver.model.items.storage.Storage.tryDecreaseKinah` | `BindPointTeleportScheduledKinahMutationPlanService`; `BindPointTeleportKinahPersistenceDecisionBridgeService` | Storage / Mutation Metadata | Partial | Unit Tested | Needs Verification | Mutation success/failure metadata feeds the composition bridge through supplied persistence decisions. Live storage mutation remains disabled. |
| `com.aionemu.gameserver.model.items.storage.Storage.decreaseItemCount` | `BindPointTeleportKinahInventoryUpdatePacketPlanService`; `BindPointTeleportKinahCallbackResultCompositionService` | Storage / Count Mutation | Partial | Unit Tested for metadata/packet order | Needs Verification | Java sends packet during mutation and marks storage dirty. C# uses saved-persistence-first staged order as an intentional policy gate. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemUpdatePacket` | `BindPointTeleportKinahInventoryUpdatePacketPlanService`; `BindPointTeleportKinahCallbackResultCompositionService` | Packet Utility / Send Boundary | Partial | Unit Tested | Needs Verification | Packet intent is composed before cooldown/fanout metadata, but no send occurs. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem` | Packet / Serialization | Partial | Unit Tested in packet-plan tests | Needs Verification | Prior test confirms update mask `0x4B`; this unit composes packet object ordering only. No Java runtime byte capture. |
| `com.aionemu.gameserver.dao.InventoryDAO` | `BindPointTeleportKinahPersistenceResult` planned adapter output | Repository / Persistence | Partial | Unit Tested with supplied result metadata | Needs Verification | No SQL adapter exists. Composition depends on supplied saved/missing/failed result metadata. |

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 1 non-live callback composition bridge plus 5 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: 1 live send adapter, 1 live repository adapter, 1 live inventory owner, and 1 live dispatch/movement path
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- The bridge composes supplied metadata only; no live callback path uses it.
- Java sends packet before dirty persistence; C# still stages saved persistence before packet intent.
- Runtime fanout metadata can be supplied, but no live fanout/send is performed by this bridge.
- Live SQL, rollback, owner/lock, `GameServerConnection` dispatch, final movement, and Java known-list parity remain separate gates.
- Java runtime packet/storage comparison was not executed.
- Reflection behavior did not change. Date/time behavior did not change. Threading, persistence, serialization, packet-order, and rollback parity remain `Needs Verification`.

## Next Recommended Unit of Work

Add a non-live callback send-result plan that models the actual inventory packet send boundary after packet intent and before cooldown/fanout metadata. It should accept a supplied send result (`Sent`, `MissingConnection`, `Failed`) and prove only `Sent` can continue to cooldown/action `3` fanout metadata. Keep it non-live: no `SendPacketAsync`, no SQL, no `GameServerConnection` dispatch, and no movement.
