# Phase 6 Bind-Point Teleport Kinah Owner Callback Bridge

Date: May 26, 2026
Unit of Work: UOW-1241
Scope: Non-live bridge from scheduled Kinah owner mutation results into callback metadata.
Source of truth: Java project.

## Bridge Result

C# now has `BindPointTeleportKinahInventoryOwnerCallbackBridgeService`, a pure bridge that adapts `BindPointTeleportKinahInventoryOwnerMutationResult` into the existing scheduled Kinah mutation plan and owner-checked persistence operation metadata.

The bridge does not execute SQL, send packets, mutate inventory beyond consuming a supplied owner result, dispatch from `GameServerConnection`, broadcast fanout, or move the player.

## Java Facts

Java source files reviewed:

- `game-server/src/com/aionemu/gameserver/services/teleport/BindPointTeleportService.java`
- `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`

Java scheduled callback behavior:

1. Failed `tryDecreaseKinah` stops before cooldown/fanout/movement.
2. Non-positive price is a successful no-mutation path.
3. Positive successful mutation prepares the inventory update packet path before cooldown/action `3`.

## C# Implementation

New C# artifacts:

- `Aion.GameServer.Services.BindPointTeleportKinahInventoryOwnerCallbackBridgeStatus`
- `Aion.GameServer.Services.BindPointTeleportKinahInventoryOwnerCallbackBridgePlan`
- `Aion.GameServer.Services.BindPointTeleportKinahInventoryOwnerCallbackBridgeService`

## Tests Added

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `CreatePlan_NotEnoughOwnerResultStopsBeforePersistence` | Owner failure maps to not-enough mutation metadata and no persistence decision. | Source-derived from Java failed `tryDecreaseKinah`. |
| `CreatePlan_NonPositiveOwnerResultContinuesWithoutPersistence` | Non-positive owner result maps to no-mutation continuation and no SQL. | Source-derived from Java `amount > 0` guard. |
| `CreatePlan_AppliedOwnerResultCreatesPersistenceOperationMetadata` | Applied owner mutation maps to update-ready persistence operation metadata with `DecreaseKinahFly`. | Source-derived order plus C# staged policy. |

## Migration Parity Table - UOW-1241

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` scheduled callback | `Aion.GameServer.Services.BindPointTeleportKinahInventoryOwnerCallbackBridgeService` | Service / Callback Boundary | Partial | Unit Tested | Needs Verification | Owner mutation results now feed scheduled callback metadata. Live callback dispatch remains disabled. |
| `com.aionemu.gameserver.model.items.storage.Storage.tryDecreaseKinah` | `BindPointTeleportKinahInventoryOwnerService`; owner callback bridge | Storage / Mutation | Partial | Unit Tested | Partial Parity | Failed, no-mutation, and applied owner results map to callback metadata. Java unsynchronized dirty lifecycle remains different. |
| `com.aionemu.gameserver.model.items.storage.Storage.decreaseItemCount` | owner callback bridge plus persistence/send planners | Storage / Count Mutation | Partial | Unit Tested | Intentional Difference | C# adapts owner result to staged persistence metadata; Java sends during mutation and persists dirty state later. |
| `com.aionemu.gameserver.dao.InventoryDAO` | `BindPointTeleportKinahPersistenceOperationPlanService` via owner callback bridge | Repository / Persistence | Partial | Unit Tested | Needs Verification | Bridge creates operation metadata only; no SQL executes. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemUpdatePacket` | owner callback bridge plus disabled send adapter | Packet Utility / Send Boundary | Partial | Unit Tested | Needs Verification | Bridge carries `DecreaseKinahFly` metadata but no live send occurs. |

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 owner-result callback bridge plus 3 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: 1 live SQL adapter, 1 live send adapter, 1 callback fanout bridge, 1 known-list parity gate, and 1 live movement path
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- Bridge is non-live and consumes supplied owner mutation results.
- SQL persistence, packet send, cooldown/action `3` fanout, final movement, and `GameServerConnection` dispatch remain disabled.
- C# staged persistence/send/rollback policy remains an intentional difference from Java dirty storage timing.
- Java runtime packet/storage comparison was not executed.
- Reflection behavior did not change. Date/time behavior did not change. Serialization, packet-order, dirty-state persistence, known-list fanout, and movement parity remain `Needs Verification`.

## Next Recommended Unit of Work

Add a non-live outcome integration test or bridge that proves the owner callback bridge can feed the full persistence-decision, packet-intent, send-decision, rollback, and callback-outcome chain for failure and success cases. Keep it pure and supplied-result based; do not execute SQL, send packets, dispatch, fanout, or move.
