# Phase 6 Bind-Point Teleport Kinah Inventory Send Result Plan

Date: May 26, 2026
Unit of Work: UOW-1233
Scope: Non-live inventory packet send-result plan for scheduled bind-point Kinah payment.
Source of truth: Java project.

## Plan Result

C# now has `BindPointTeleportKinahInventorySendResultPlanService`, a supplied-result planner for the inventory update packet send boundary. It consumes a staged callback composition plus an explicit send result and only allows cooldown/action `3` fanout metadata to continue when the send result is `Sent` and `SentPacket=true`.

The planner does not call `SendPacketAsync`, mutate cooldown state, broadcast fanout, run SQL, dispatch from `GameServerConnection`, or move the player.

## Java Facts

Java source files reviewed:

- `game-server/src/com/aionemu/gameserver/services/teleport/BindPointTeleportService.java`
- `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_INVENTORY_UPDATE_ITEM.java`

Observed Java scheduled callback order:

1. `tryDecreaseKinah(price, DEC_KINAH_FLY)`.
2. Java sends `SM_INVENTORY_UPDATE_ITEM` during `Storage.decreaseItemCount` when the actor exists.
3. Java marks storage dirty after the mutation/send path.
4. Java then stores cooldown and broadcasts action `3`.
5. C# is still staging a stricter send-result gate before cooldown/action `3` metadata because live send failure behavior is not yet implemented.

## C# Implementation

New C# artifacts:

- `Aion.GameServer.Services.BindPointTeleportKinahInventorySendStatus`
- `Aion.GameServer.Services.BindPointTeleportKinahInventorySendResult`
- `Aion.GameServer.Services.BindPointTeleportKinahInventorySendDecisionStatus`
- `Aion.GameServer.Services.BindPointTeleportKinahInventorySendDecision`
- `Aion.GameServer.Services.BindPointTeleportKinahInventorySendResultPlanService`

The service returns:

- `StoppedBeforePacketIntent` when no inventory update packet intent exists;
- `StoppedMissingConnection` when the supplied result is missing or `MissingConnection`;
- `StoppedSendFailed` when the supplied result is `Failed` or `SentPacket=false`;
- `ReadyForCooldownFanout` only when the supplied result is `Sent` and `SentPacket=true`.

## Tests Added

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `CreateDecision_CompositionWithoutPacketIntentStopsBeforeSend` | No packet intent blocks the send gate before cooldown/fanout/movement. | C# staging guard before live send. |
| `CreateDecision_MissingConnectionStopsBeforeCooldownFanoutAndMovement` | Missing connection result blocks cooldown/action `3` fanout and movement. | Intentional C# safety gate; Java actor-present send path was reviewed. |
| `CreateDecision_FailedSendStopsBeforeCooldownFanoutAndMovement` | Failed send or false sent flag blocks success metadata. | Intentional C# safety gate. |
| `CreateDecision_SentPacketAllowsCooldownFanoutAndMovementMetadata` | Successful supplied send result allows cooldown/fanout/movement metadata to continue. | Source-derived ordering; no live send. |
| `CreateDecision_SentPacketKeepsBlockedMovementBlocked` | Successful send keeps final movement blocked when movement gate already failed. | Source-derived from Java final movement gate. |

## Migration Parity Table - UOW-1233

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemUpdatePacket` | `Aion.GameServer.Services.BindPointTeleportKinahInventorySendResultPlanService` | Packet Utility / Send Boundary | Partial | Unit Tested with supplied results | Needs Verification | C# now models a supplied send-result gate before cooldown/fanout metadata. No live send occurs. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem`; send-result plan | Packet / Serialization | Partial | Unit Tested in packet/send-plan tests | Needs Verification | Packet object and send-result metadata are staged; Java runtime bytes and live send behavior are unverified. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` scheduled callback | `BindPointTeleportKinahCallbackResultCompositionService`; `BindPointTeleportKinahInventorySendResultPlanService` | Service / Callback Boundary | Partial | Unit Tested | Needs Verification | C# can now block/continue callback metadata based on supplied inventory packet send result. Live callback dispatch remains disabled. |
| `com.aionemu.gameserver.model.items.storage.Storage.decreaseItemCount` | `BindPointTeleportKinahInventorySendResultPlanService` plus prior packet/persistence planners | Storage / Count Mutation | Partial | Unit Tested for metadata gates | Needs Verification | Java sends during mutation and marks storage dirty. C# models saved-persistence and sent-packet gates as staged policy. |
| `com.aionemu.gameserver.dao.InventoryDAO` | `BindPointTeleportKinahPersistenceResult` planned adapter output | Repository / Persistence | Partial | Unit Tested with supplied result metadata | Needs Verification | No SQL adapter exists. Send-result plan assumes persistence already reached `Saved`. |

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 non-live inventory send-result planner plus 6 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: 1 live send adapter, 1 live repository adapter, 1 live inventory owner, and 1 live dispatch/movement path
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- The service consumes supplied send results only; no live packet send occurs.
- Java sends before dirty persistence; C# still stages saved persistence and sent-packet gates before cooldown/fanout.
- Missing connection and send failure handling are conservative C# gates, not Java runtime-verified behavior.
- Live SQL, rollback, owner/lock, `GameServerConnection` dispatch, actual fanout, final movement, and Java known-list parity remain separate gates.
- Java runtime packet/storage comparison was not executed.
- Reflection behavior did not change. Date/time behavior did not change. Threading, persistence, serialization, packet-order, and rollback parity remain `Needs Verification`.

## Next Recommended Unit of Work

Add a non-live live-adapter readiness update for the now-complete Kinah metadata chain, then choose the next executable prerequisite: either an owner/rollback design refinement for live mutation or a no-op live send adapter seam that consumes the send-result planner without calling `SendPacketAsync`.
