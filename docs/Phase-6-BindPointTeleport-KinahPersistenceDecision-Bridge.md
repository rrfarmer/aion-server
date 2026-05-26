# Phase 6 Bind-Point Teleport Kinah Persistence Decision Bridge

Date: May 26, 2026
Unit of Work: UOW-1230
Scope: Non-live persistence-result decision bridge for scheduled bind-point Kinah callback continuation.
Source of truth: Java project.

## Bridge Result

C# now has a non-live `BindPointTeleportKinahPersistenceDecisionBridgeService` that consumes scheduled callback metadata plus a supplied Kinah persistence result and decides whether the callback may continue. This is still not a live repository, packet-send, or dispatch path. It only models the gate required before a future live adapter can send `SmInventoryUpdateItem.DecreaseKinahFly`, store cooldown, broadcast action `3`, or schedule final movement.

The bridge enforces the policy from UOW-1228/UOW-1229:

- not-enough Kinah stops before persistence and packet metadata;
- missing persistence result stops and requests rollback;
- `MissingRow` stops and requests rollback;
- `Failed` stops and requests rollback;
- `Saved` is the only persistence status that allows inventory update packet metadata plus cooldown/fanout/movement metadata to continue;
- non-positive price/no mutation can continue without persistence or packet metadata, matching Java's `amount > 0` guard.

Update after UOW-1231: `BindPointTeleportKinahInventoryUpdatePacketPlanService` now consumes `ContinueAfterPersistence` decisions and creates a concrete non-sending `SmInventoryUpdateItem` packet intent with mask `0x4B`. Stopped decisions still produce no packet.

Update after UOW-1232: `BindPointTeleportKinahCallbackResultCompositionService` now composes the saved persistence decision, packet intent, runtime cooldown/action `3` metadata, and final movement metadata in staged order. It still does not send or persist.

## Java Facts

Java source files reviewed:

- `game-server/src/com/aionemu/gameserver/services/teleport/BindPointTeleportService.java`
- `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
- `game-server/src/com/aionemu/gameserver/dao/InventoryDAO.java`

Observed Java contract:

1. Scheduled callback failure from `tryDecreaseKinah` sends not-enough-fee and returns before cooldown/fanout/movement.
2. Scheduled callback success sends the Kinah inventory update during mutation, then continues to cooldown/action `3` fanout.
3. Java persistence is dirty-state/lifecycle driven through `InventoryDAO.store(player)` rather than a callback-local immediate DB result.
4. C# intentionally stages an immediate persistence-result gate before packet send because this path does not yet have Java's dirty storage lifecycle.

## C# Implementation

New C# artifacts:

- `Aion.GameServer.Services.BindPointTeleportKinahPersistenceStatus`
- `Aion.GameServer.Services.BindPointTeleportKinahPersistenceResult`
- `Aion.GameServer.Services.BindPointTeleportKinahPersistenceDecisionStatus`
- `Aion.GameServer.Services.BindPointTeleportKinahPersistenceDecision`
- `Aion.GameServer.Services.BindPointTeleportKinahPersistenceDecisionBridgeService`

The bridge is pure/non-live:

- no SQL;
- no repository dependency;
- no packet send;
- no cooldown mutation;
- no fanout;
- no `GameServerConnection` dispatch;
- no movement.

## Tests Added

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `CreateDecision_NotEnoughKinahStopsBeforePersistencePacketCooldownFanoutAndMovement` | Failed scheduled Kinah metadata stops before persistence, packet, cooldown/fanout, and movement. | Source-derived from Java failed `tryDecreaseKinah` branch. |
| `CreateDecision_MissingPersistenceResultStopsAndRequiresRollback` | C# staging guard blocks packet/fanout when Kinah update metadata lacks a persistence result. | Intentional C# safety gate; Java uses dirty persistence lifecycle. |
| `CreateDecision_PersistenceFailureStopsBeforePacketCooldownFanoutAndMovement` | `MissingRow` and `Failed` persistence results both stop before success side effects and require rollback. | Intentional C# safety gate based on owner-checked persistence policy. |
| `CreateDecision_SavedPersistenceAllowsPacketMetadataAndCooldownFanoutToContinue` | `Saved` carries Kinah update metadata and allows later cooldown/fanout/movement metadata to continue. | Source-derived order, but no Java runtime comparison. |
| `CreateDecision_NonPositivePriceContinuesWithoutPersistenceOrPacket` | Non-positive price path continues without mutation/persistence/packet metadata. | Source-derived from `Storage.decreaseKinah` `amount > 0` guard. |

## Migration Parity Table - UOW-1230

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` scheduled callback | `Aion.GameServer.Services.BindPointTeleportKinahPersistenceDecisionBridgeService` | Service / Callback Gate | Partial | Unit Tested | Needs Verification | C# now gates supplied persistence results before packet/cooldown/fanout/movement metadata. No live callback, SQL, send, or movement. |
| `com.aionemu.gameserver.model.items.storage.Storage.tryDecreaseKinah` | `BindPointTeleportScheduledKinahMutationPlanService`; `BindPointTeleportKinahPersistenceDecisionBridgeService` | Storage / Mutation Metadata | Partial | Unit Tested | Needs Verification | Mutation success/failure metadata can now be combined with persistence status. In-memory mutation remains non-live. |
| `com.aionemu.gameserver.model.items.storage.Storage.decreaseItemCount` | future live inventory owner plus persistence decision bridge | Storage / Count Mutation | Partial | Unit Tested for decision metadata | Needs Verification | Java sends packet and marks dirty during mutation. C# bridge models persist-before-send as an intentional staged policy, not Java dirty lifecycle parity. |
| `com.aionemu.gameserver.dao.InventoryDAO` | `BindPointTeleportKinahPersistenceResult` planned adapter output | Repository / Persistence | Partial | Unit Tested with supplied results | Needs Verification | No MySQL adapter exists. Tests use supplied `Saved`/`MissingRow`/`Failed` result objects, not database execution. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemUpdatePacket` | future send adapter gated by `BindPointTeleportKinahPersistenceDecisionStatus.ContinueAfterPersistence` | Packet Utility / Send Boundary | Partial | Unit Tested for decision metadata | Needs Verification | Packet send remains unwired; bridge only allows packet metadata to continue after supplied `Saved`. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem` | Packet / Serialization | Partial | Unit Tested in packet/bridge tests | Needs Verification | C# can carry `DecreaseKinahFly` metadata; no live send or Java runtime byte capture. |

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 1 non-live persistence decision bridge plus 6 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: 1 live repository adapter, 1 live inventory owner, and 1 live packet-send adapter
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- The bridge consumes supplied persistence results only; no SQL adapter exists.
- The C# persist-before-send policy remains an intentional staged difference from Java's packet-before-dirty-persistence lifecycle.
- Rollback is metadata only; no in-memory owner/rollback helper is live.
- Packet send, cooldown/fanout execution, and movement remain separate live gates.
- Java runtime packet/storage comparison was not executed.
- Reflection behavior did not change. Date/time behavior did not change. Threading, persistence, serialization, packet-order, and rollback parity remain `Needs Verification`.

## Next Recommended Unit of Work

Add a non-sending inventory update packet adapter that consumes `BindPointTeleportKinahPersistenceDecisionStatus.ContinueAfterPersistence` and produces a concrete `SmInventoryUpdateItem` packet intent only after the decision bridge reports `Saved`. Keep it non-live: no `SendPacketAsync`, no SQL, no `GameServerConnection` dispatch, and no movement.

Update after UOW-1231: the non-sending packet adapter is complete. Next, compose the persistence decision and packet plan with runtime callback metadata so the full staged order is represented without live sends.

Update after UOW-1232: the composition bridge is complete. Next, model a supplied inventory packet send result so failed sends cannot continue to cooldown/action `3` fanout.
