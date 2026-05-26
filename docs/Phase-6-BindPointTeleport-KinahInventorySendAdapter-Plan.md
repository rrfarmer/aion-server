# Phase 6 Bind-Point Teleport Kinah Inventory Send Adapter Plan

Date: May 26, 2026
Unit of Work: UOW-1236
Scope: Disabled no-op send adapter seam for scheduled bind-point Kinah inventory update packets.
Source of truth: Java project.

## Plan Result

C# now has `BindPointTeleportKinahInventorySendAdapterPlanService`, a disabled adapter seam that consumes `BindPointTeleportKinahInventoryUpdatePacketPlan` and returns the existing `BindPointTeleportKinahInventorySendResult` shape without calling `IGameClientConnectionRegistry.SendPacketToPlayerAsync`.

The seam records two outcomes:

- `NoPacketIntent`: no inventory update packet exists, so no send boundary would be reached.
- `DisabledNoSend`: a packet intent exists and Java would call `PacketSendUtility.sendPacket`, but the C# live send remains explicitly disabled.

This unit does not wire `GameServerConnection`, does not send a packet, does not mutate inventory, does not persist SQL, does not broadcast cooldown fanout, and does not move the player.

## Java Facts

Java source files reviewed:

- `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
- `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
- `game-server/src/com/aionemu/gameserver/services/teleport/BindPointTeleportService.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_INVENTORY_UPDATE_ITEM.java`

Java behavior:

1. Scheduled bind-point Kinah success calls `tryDecreaseKinah(price, DEC_KINAH_FLY)`.
2. `Storage.decreaseItemCount` mutates the Kinah item.
3. Java calls `ItemPacketService.sendItemUpdatePacket`, which sends `SM_INVENTORY_UPDATE_ITEM`.
4. The callback continues to cooldown/action `3` fanout only after the Kinah mutation path returns success.

## C# Implementation

New C# artifacts:

- `Aion.GameServer.Services.BindPointTeleportKinahInventorySendAdapterStatus`
- `Aion.GameServer.Services.BindPointTeleportKinahInventorySendAdapterPlan`
- `Aion.GameServer.Services.BindPointTeleportKinahInventorySendAdapterPlanService`

The disabled adapter accepts an optional `IGameClientConnectionRegistry` only to pin the future live boundary and allow tests to prove the registry is not called.

## Tests Added

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `CreateDisabledPlan_WithoutPacketIntentReturnsFailedNoSendResult` | No packet intent returns a failed no-send result and does not call the registry. | C# staging guard; Java success path would have a packet. |
| `CreateDisabledPlan_WithPacketIntentRecordsBoundaryWithoutCallingRegistry` | Packet intent is recognized as a future send boundary, but disabled C# does not call `SendPacketAsync`. | Source-derived from Java `PacketSendUtility.sendPacket`. |
| `CreateDisabledPlan_DisabledSendResultStopsCooldownFanoutGate` | The disabled send result feeds the existing send-result gate and blocks cooldown/action `3` fanout and movement. | Intentional C# safety gate until live send is implemented. |

## Migration Parity Table - UOW-1236

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemUpdatePacket` | `Aion.GameServer.Services.BindPointTeleportKinahInventorySendAdapterPlanService` | Packet Utility / Send Boundary | Partial | Unit Tested | Needs Verification | C# now has a disabled no-op seam for the Java send boundary. It returns existing send-result metadata but never calls `SendPacketAsync`. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem`; send adapter plan | Packet / Serialization | Partial | Unit Tested | Needs Verification | Packet intent is consumed by the adapter. Packet byte parity is source-derived only; no Java runtime capture was run in this unit. |
| `com.aionemu.gameserver.model.items.storage.Storage.decreaseItemCount` | `BindPointTeleportKinahInventorySendAdapterPlanService`; prior mutation/persistence planners | Storage / Count Mutation | Partial | Unit Tested | Needs Verification | Java mutates and sends inside storage. C# still separates mutation, persistence, packet intent, and disabled send metadata. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` scheduled callback | `BindPointTeleportKinahInventorySendAdapterPlanService`; `BindPointTeleportKinahInventorySendResultPlanService` | Service / Callback Boundary | Partial | Unit Tested | Needs Verification | Disabled send results block cooldown/action `3` fanout and movement. Live callback dispatch remains disabled. |
| `Aion.GameServer.Network.Aion.IGameClientConnectionRegistry` future C# dependency | `BindPointTeleportKinahInventorySendAdapterPlanService` | C# Live Boundary Dependency | Partial | Unit Tested | Needs Verification | Newly discovered dependency for future live send. Current tests use a throwing registry and verify zero `SendPacketAsync` calls. |

## Summary Metrics

- Total Java artifacts discovered: 4 Java artifact rows plus 1 newly discovered C# live-boundary dependency
- Total artifacts ported: 1 disabled no-op send adapter seam plus 3 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: 1 live `SendPacketAsync` adapter, 1 live inventory owner/lock, 1 live repository adapter, 1 live dispatch path, and 1 live movement path
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- The adapter is intentionally disabled and returns a failed no-send result for packet-ready plans.
- No live `SendPacketAsync` call occurs, so Java send behavior remains unverified.
- Java mutates and sends from storage before dirty persistence; C# still uses staged persistence/send/rollback metadata.
- Missing connection, send failure, rollback, SQL persistence, known-list fanout, and movement behavior remain separate unverified gates.
- Reflection behavior did not change. Date/time behavior did not change. Threading behavior did not change. Serialization, packet ordering, rollback, and persistence parity remain `Needs Verification`.

## Next Recommended Unit of Work

Add a repository SQL adapter design or pure contract for owner-checked Kinah count persistence. Keep it disabled from live callbacks and prove it does not mutate or write SQL unless explicitly supplied/executed in tests. Do not wire `GameServerConnection`, live packet sends, cooldown/action `3` fanout, or movement.

Update after UOW-1237: `BindPointTeleportKinahPersistenceOperationPlanService` now provides the pure owner-checked persistence operation contract. Next, compose mutation, persistence result, disabled send result, and rollback metadata into one non-live scheduled callback outcome before any live adapter work.
