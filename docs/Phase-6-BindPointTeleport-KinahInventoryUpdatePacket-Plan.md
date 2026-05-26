# Phase 6 Bind-Point Teleport Kinah Inventory Update Packet Plan

Date: May 26, 2026
Unit of Work: UOW-1231
Scope: Non-sending inventory update packet intent adapter for scheduled bind-point Kinah payment.
Source of truth: Java project.

## Plan Result

C# now has a non-sending `BindPointTeleportKinahInventoryUpdatePacketPlanService` that consumes a `BindPointTeleportKinahPersistenceDecision` and creates a concrete `SmInventoryUpdateItem` packet intent only when the decision is `ContinueAfterPersistence`. The adapter does not send packets, mutate cooldown state, broadcast fanout, dispatch from `GameServerConnection`, run SQL, or move the player.

This keeps the UOW-1228/UOW-1229/UOW-1230 gate intact: persistence must be reported as `Saved` before the Kinah inventory update packet can even be planned.

Update after UOW-1232: `BindPointTeleportKinahCallbackResultCompositionService` now composes saved persistence, packet intent, cooldown/action `3` fanout metadata, and final movement metadata in staged order without live sends.

## Java Facts

Java source files reviewed:

- `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_INVENTORY_UPDATE_ITEM.java`
- `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
- `game-server/src/com/aionemu/gameserver/services/teleport/BindPointTeleportService.java`

Observed Java contract:

1. `Storage.decreaseItemCount` sends an item packet during the scheduled Kinah mutation when an actor exists.
2. Cube Kinah update routes through `ItemPacketService.sendItemUpdatePacket`.
3. Java sends `SM_INVENTORY_UPDATE_ITEM(player, item, DEC_KINAH_FLY)`.
4. `DEC_KINAH_FLY` has update mask `0x4B`.
5. Java sends before dirty persistence, while this C# path still uses the staged persist-before-send policy because no dirty storage lifecycle exists for bind-point callbacks.

## C# Implementation

New C# artifacts:

- `Aion.GameServer.Services.BindPointTeleportKinahInventoryUpdatePacketPlanStatus`
- `Aion.GameServer.Services.BindPointTeleportKinahInventoryUpdatePacketPlan`
- `Aion.GameServer.Services.BindPointTeleportKinahInventoryUpdatePacketPlanService`

The service returns:

- `NoPacket` when the persistence decision is stopped or does not require a Kinah packet;
- `MissingTemplate` when the decision is `ContinueAfterPersistence` but the Kinah item template is unavailable;
- `PacketReady` with `SmInventoryUpdateItem(updatedKinah, kinahTemplate, SmInventoryUpdateItem.DecreaseKinahFly)` only after a saved persistence decision.

## Tests Added

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `CreatePlan_StoppedPersistenceDecisionProducesNoPacket` | `MissingRow` and `Failed` decisions produce no packet intent. | Intentional C# persistence gate before send. |
| `CreatePlan_NonPositivePriceDecisionProducesNoPacket` | No-mutation/non-positive price decision creates no packet. | Source-derived from Java `amount > 0` guard. |
| `CreatePlan_SavedDecisionWithoutTemplateProducesMissingTemplate` | Saved persistence still cannot build packet without Kinah template. | C# staging guard; Java has item template at runtime. |
| `CreatePlan_SavedDecisionCreatesDecreaseKinahFlyPacketIntent` | Saved decision creates concrete packet intent and serializes trailing update type `0x4B`. | Source-derived packet shape; no Java runtime capture. |

## Migration Parity Table - UOW-1231

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemUpdatePacket` | `Aion.GameServer.Services.BindPointTeleportKinahInventoryUpdatePacketPlanService` | Packet Utility / Send Boundary | Partial | Unit Tested | Needs Verification | C# now creates a non-sending packet intent only after saved persistence. It does not send to a client. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem` | Packet / Serialization | Partial | Unit Tested | Needs Verification | Test confirms C# packet intent serializes update mask `0x4B`; no Java runtime byte capture. |
| `com.aionemu.gameserver.model.items.storage.Storage.decreaseItemCount` | `BindPointTeleportKinahPersistenceDecisionBridgeService`; `BindPointTeleportKinahInventoryUpdatePacketPlanService` | Storage / Count Mutation | Partial | Unit Tested for metadata/packet intent | Needs Verification | Java sends packet during mutation and marks storage dirty. C# packet intent is gated by supplied saved persistence status. |
| `com.aionemu.gameserver.dao.InventoryDAO` | `BindPointTeleportKinahPersistenceResult` planned adapter output | Repository / Persistence | Partial | Unit Tested with supplied results | Needs Verification | No SQL adapter exists. Packet plan trusts supplied `Saved` decision only. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` scheduled callback | future live C# scheduled Kinah mutation/persistence/send adapter | Service / Callback Boundary | Partial | Unit Tested | Needs Verification | Non-sending packet intent exists, but live callback dispatch, actual send, cooldown/fanout ordering, and movement remain disabled. |

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 non-sending packet intent adapter plus 5 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: 1 live send adapter, 1 live repository adapter, and 1 live inventory owner
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- The adapter creates a packet object but does not send it.
- Java sends before dirty persistence; C# still gates packet planning behind supplied saved persistence.
- No Java runtime packet capture verified the full `DEC_KINAH_FLY` packet bytes.
- Live SQL, rollback, owner/lock, cooldown/fanout execution, and movement remain separate gates.
- Reflection behavior did not change. Date/time behavior did not change. Threading, persistence, serialization, packet-order, and rollback parity remain `Needs Verification`.

## Next Recommended Unit of Work

Add a non-live callback result composition bridge that combines the persistence decision and packet plan with the existing runtime callback execution metadata, proving the staged order `Saved persistence -> packet intent -> cooldown/action 3 fanout metadata -> final movement metadata` without sending packets or dispatching from `GameServerConnection`.

Update after UOW-1232: the callback result composition bridge is complete. Next, add a non-live send-result plan for the inventory packet boundary before cooldown/fanout metadata.
