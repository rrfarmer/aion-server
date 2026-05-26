# Phase 6 Bind-Point Teleport Kinah Send-Before-Runtime Ordering

Date: May 26, 2026
Unit of Work: UOW-1243
Scope: Non-live ordering gate for scheduled Kinah packet send before cooldown/action `3` runtime metadata.
Source of truth: Java project.

## Ordering Result

C# now has `BindPointTeleportKinahSendBeforeRuntimeOrderingService`, a pure ordering gate that records Java's successful scheduled Kinah callback sequence:

1. saved Kinah/persistence decision,
2. `SM_INVENTORY_UPDATE_ITEM` packet intent,
3. inventory update packet send result,
4. cooldown storage,
5. action `3` fanout,
6. final movement scheduling/gate metadata.

This service does not execute SQL, send packets, broadcast fanout, dispatch from `GameServerConnection`, or move the player.

## Java Facts

Java source files reviewed:

- `game-server/src/com/aionemu/gameserver/services/teleport/BindPointTeleportService.java`
- `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`

Java sends `SM_INVENTORY_UPDATE_ITEM(..., DEC_KINAH_FLY)` during the Kinah decrease path before `BindPointTeleportService` stores cooldown and broadcasts `SM_BIND_POINT_TELEPORT(action=3)`.

## C# Implementation

New C# artifacts:

- `Aion.GameServer.Services.BindPointTeleportKinahSendBeforeRuntimeOrderingStatus`
- `Aion.GameServer.Services.BindPointTeleportKinahSendBeforeRuntimeOrderingStep`
- `Aion.GameServer.Services.BindPointTeleportKinahSendBeforeRuntimeOrderingPlan`
- `Aion.GameServer.Services.BindPointTeleportKinahSendBeforeRuntimeOrderingService`

## Tests Added

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `CreatePlan_StoppedPersistenceBlocksPacketSendAndRuntime` | Stopped persistence blocks packet send and runtime metadata. | Source-derived C# staging guard before Java send point. |
| `CreatePlan_MissingSendResultBlocksRuntimeAfterPacketIntent` | Packet intent alone cannot unlock cooldown/action `3` metadata. | Source-derived from Java packet-send-before-cooldown order. |
| `CreatePlan_SendFailureBlocksCooldownFanoutAndMovement` | Failed send blocks cooldown, fanout, and final movement metadata. | Intentional C# safety gate for send failure. |
| `CreatePlan_SentPacketWaitsForRuntimeCallbackMetadata` | Successful send can wait for runtime callback metadata without implying fanout/movement. | Non-live staging contract. |
| `CreatePlan_SentPacketThenRuntimeCallbackContinuesInJavaOrder` | Successful path steps are ordered as packet send before cooldown, fanout, and movement. | Source-derived Java order; metadata-only. |

## Migration Parity Table - UOW-1243

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` scheduled callback | `Aion.GameServer.Services.BindPointTeleportKinahSendBeforeRuntimeOrderingService` | Service / Ordering Gate | Partial | Unit Tested | Needs Verification | Metadata now explicitly gates cooldown/action `3` runtime metadata behind inventory update send success. Live callback dispatch remains disabled. |
| `com.aionemu.gameserver.model.items.storage.Storage.decreaseItemCount` | send-before-runtime ordering gate plus packet/send planners | Storage / Count Mutation | Partial | Unit Tested | Intentional Difference | Java sends during mutation; C# still uses staged metadata, but now records Java send-before-runtime order. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemUpdatePacket` | `BindPointTeleportKinahInventoryUpdatePacketPlanService`; send-before-runtime ordering gate | Packet Utility / Send Boundary | Partial | Unit Tested | Needs Verification | Packet send is supplied metadata only. No live `SendPacketAsync` or golden-byte Java comparison. |
| `com.aionemu.gameserver.utils.PacketSendUtility.broadcastPacket` | send-before-runtime ordering gate consuming runtime callback metadata | Network Utility / Fanout | Partial | Unit Tested | Needs Verification | Fanout metadata cannot proceed until send success in this gate. Java known-list/self-first behavior remains unverified. |
| `com.aionemu.gameserver.services.teleport.TeleportService.teleportTo` | send-before-runtime ordering gate final movement metadata | Movement Service | Partial | Unit Tested | Needs Verification | Final movement metadata is gated behind send and runtime callback readiness. No live movement. |

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 non-live send-before-runtime ordering service plus 5 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: 1 live SQL adapter, 1 live send adapter, 1 live known-list fanout bridge, and 1 live movement path
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- Ordering gate is metadata-only and consumes supplied send/runtime results.
- Existing callback composition service still has its older runtime-before-send metadata shape; live wiring must use or honor this new ordering gate before enabling side effects.
- SQL execution, packet send, `GameServerConnection` dispatch, known-list fanout, and movement remain disabled.
- Java runtime packet/order comparison was not executed.
- Reflection behavior did not change. Date/time behavior did not change. Serialization, packet-order, dirty-state persistence, threading, known-list fanout, and movement parity remain `Needs Verification`.

## Next Recommended Unit of Work

Add a non-live final live-adapter readiness audit for the bind-point scheduled Kinah path that summarizes which gates are now satisfied and which live adapters remain blocked before any `GameServerConnection` execution can be enabled.
