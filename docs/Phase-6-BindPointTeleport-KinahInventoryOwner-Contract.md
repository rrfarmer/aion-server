# Phase 6 Bind-Point Teleport Kinah Inventory Owner Contract

Date: May 26, 2026
Unit of Work: UOW-1240
Scope: Pure in-memory owner contract for scheduled bind-point Kinah mutation and rollback.
Source of truth: Java project.

## Contract Result

C# now has `BindPointTeleportKinahInventoryOwnerService`, an isolated in-memory owner for scheduled bind-point Kinah mutation and rollback. It applies a cube-Kinah decrement to `Player.InventoryItems` under a per-player lock and can restore the original Kinah snapshot after a later persistence or send failure.

This service is not wired into `GameServerConnection`, the scheduler callback, SQL persistence, packet sends, fanout, or movement.

## Java Facts

Java source files reviewed:

- `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
- `game-server/src/com/aionemu/gameserver/model/items/storage/PlayerStorage.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/Item.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
- `game-server/src/com/aionemu/gameserver/services/teleport/BindPointTeleportService.java`

Java behavior:

1. `tryDecreaseKinah(price, DEC_KINAH_FLY)` checks current Kinah and decrements when enough Kinah exists.
2. Missing or insufficient Kinah returns false and scheduled bind-point callback sends not-enough-fee.
3. Non-positive price succeeds and does not mutate item count because `decreaseKinah` only mutates for `amount > 0`.
4. Kinah is not deleted at zero.
5. Java has no explicit lock around the check/decrement; C# adds a per-player lock as an intentional safety boundary.

## C# Implementation

New C# artifacts:

- `Aion.GameServer.Services.BindPointTeleportKinahInventoryOwnerMutationStatus`
- `Aion.GameServer.Services.BindPointTeleportKinahInventoryOwnerRollbackStatus`
- `Aion.GameServer.Services.BindPointTeleportKinahInventoryOwnerMutationResult`
- `Aion.GameServer.Services.BindPointTeleportKinahInventoryOwnerRollbackResult`
- `Aion.GameServer.Services.BindPointTeleportKinahInventoryOwnerService`

## Tests Added

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `TryApplyScheduledDecrease_MissingKinahStopsWithFee` | Missing Kinah stops with fee metadata and no mutation. | Source-derived from Java `getKinah()==0` failure. |
| `TryApplyScheduledDecrease_InsufficientKinahStopsWithoutMutation` | Insufficient Kinah leaves count unchanged. | Source-derived from Java failed `tryDecreaseKinah`. |
| `TryApplyScheduledDecrease_NonPositivePriceContinuesWithoutMutation` | Non-positive price succeeds without mutation or packet intent. | Source-derived from Java `amount > 0` guard. |
| `TryApplyScheduledDecrease_ExactPriceKeepsZeroCountKinahItem` | Exact decrement leaves zero-count Kinah item and emits update metadata. | Source-derived from Java no-delete Kinah behavior. |
| `RollbackScheduledDecrease_RestoresOriginalKinahSnapshot` | Rollback restores original Kinah after applied mutation. | C# staged rollback policy. |
| `RollbackScheduledDecrease_NoMutationIsNoOp` | Rollback no-ops when no mutation was applied. | C# staged rollback policy. |
| `TryApplyScheduledDecrease_ConcurrentDoubleSpendAllowsOnlyOneMutation` | Per-player lock prevents double-spend in C# owner. | Intentional C# safety boundary; Java is not explicitly locked. |

## Migration Parity Table - UOW-1240

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.items.storage.Storage.tryDecreaseKinah` | `Aion.GameServer.Services.BindPointTeleportKinahInventoryOwnerService` | Storage / Mutation Owner | Partial | Unit Tested | Partial Parity | Missing, insufficient, non-positive, and success decrement branches are modeled. C# adds per-player locking as an intentional safety boundary; Java has no explicit lock. |
| `com.aionemu.gameserver.model.items.storage.Storage.decreaseKinah` | `BindPointTeleportKinahInventoryOwnerService.TryApplyScheduledDecrease` | Storage / Count Mutation | Partial | Unit Tested | Partial Parity | C# preserves `amount > 0` mutation guard and no-delete zero Kinah behavior. Java packet send/dirty-state marking remain outside this owner. |
| `com.aionemu.gameserver.model.items.storage.Storage.decreaseItemCount` | `BindPointTeleportKinahInventoryOwnerService`; send/persistence planner chain | Storage / Count Mutation | Partial | Unit Tested | Intentional Difference | C# owner mutates in memory and exposes rollback; Java sends during mutation and marks dirty storage for later persistence. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemUpdatePacket` | owner result `InventoryUpdateType=SmInventoryUpdateItem.DecreaseKinahFly`; disabled send adapter | Packet Utility / Send Boundary | Partial | Unit Tested | Needs Verification | Owner produces packet metadata only. No live packet send or Java runtime packet comparison. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` scheduled callback | future callback owner using `BindPointTeleportKinahInventoryOwnerService` plus existing outcome composer | Service / Callback Boundary | Partial | Unit Tested | Needs Verification | Owner is not wired into scheduled callback or dispatch. SQL, send, fanout, and movement remain disabled. |

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 in-memory Kinah owner contract plus 7 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 3 grouped rows
- Total blocked artifacts: 1 live SQL adapter, 1 live send adapter, 1 callback fanout bridge, 1 known-list parity gate, and 1 live movement path
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- Owner is not wired into live scheduled callbacks.
- Per-player locking is an intentional C# safety boundary, not a literal Java synchronization port.
- Java dirty storage state and packet send side effects remain separate from this owner.
- SQL persistence, packet send, cooldown/action `3` fanout, final movement, and `GameServerConnection` dispatch remain disabled.
- Java runtime packet/storage comparison was not executed.
- Reflection behavior did not change. Date/time behavior did not change. Serialization, packet-order, dirty-state persistence, known-list fanout, and movement parity remain `Needs Verification`.

## Next Recommended Unit of Work

Add a non-live bridge that adapts `BindPointTeleportKinahInventoryOwnerService` mutation results into the existing scheduled Kinah callback outcome chain. It should consume owner mutation/rollback results and existing persistence/send decisions, but still avoid SQL execution, packet sends, `GameServerConnection`, fanout, and movement.

Update after UOW-1241: `BindPointTeleportKinahInventoryOwnerCallbackBridgeService` now adapts owner mutation results into scheduled mutation and persistence-operation metadata. The next safe seam is a full non-live outcome integration test/bridge over owner, persistence decision, packet intent, send decision, rollback, and callback outcome.
