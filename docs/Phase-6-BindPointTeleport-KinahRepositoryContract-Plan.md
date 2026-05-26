# Phase 6 Bind-Point Teleport Kinah Repository Contract Plan

Date: May 26, 2026
Unit of Work: UOW-1229
Scope: Non-live repository contract plan for scheduled bind-point Kinah persistence.
Source of truth: Java project.

## Plan Result

Do not add a live repository method or wire SQL for scheduled bind-point Kinah persistence yet. The current C# `IPlayerEnterWorldRepository` is already a broad enter-world/logout/player-mutation interface, and adding a bind-point callback method there before the live mutation owner exists would widen a shared contract without proving the owner, rollback, or packet-send semantics.

The next live implementation should introduce a narrow bind-point persistence boundary, owner-checked by player id and item object id, then adapt that boundary to MySQL only after the scheduled inventory owner can roll back or suppress packet/fanout side effects on failure.

Update after UOW-1230: `BindPointTeleportKinahPersistenceDecisionBridgeService` now consumes supplied persistence results and gates callback continuation. It is non-live and uses supplied `Saved`/`MissingRow`/`Failed` statuses only; no SQL adapter was added.

## Java Facts

Java source files reviewed:

- `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
- `game-server/src/com/aionemu/gameserver/model/items/storage/PlayerStorage.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
- `game-server/src/com/aionemu/gameserver/dao/InventoryDAO.java`
- `game-server/src/com/aionemu/gameserver/services/teleport/BindPointTeleportService.java`

Observed Java contract:

1. The scheduled callback calls `player.getInventory().tryDecreaseKinah(price, ItemPacketService.ItemUpdateType.DEC_KINAH_FLY)`.
2. `Storage.tryDecreaseKinah` checks current Kinah, mutates the item count only through `decreaseKinah`, and returns success/failure synchronously to the callback.
3. `Storage.decreaseItemCount` sends the inventory update packet during the storage mutation when an actor exists, then marks storage `PersistentState.UPDATE_REQUIRED`.
4. Java does not delete the Kinah item when count reaches zero.
5. Persistence is deferred to `InventoryDAO.store(player)`, which persists dirty items later through broad item-row update/delete/insert batches.
6. `InventoryDAO.updateItems` keys updates by item object id and writes the full row, including owner and count.

## C# Facts

C# surfaces reviewed:

- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/src/Aion.GameServer/Data/HouseAuctionRepository.cs`
- `dotnetConversion/src/Aion.GameServer/Data/MailRepository.cs`
- `dotnetConversion/src/Aion.GameServer/Data/BrokerRepository.cs`
- `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportScheduledKinahMutationPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportRuntimeCallbackExecutionBridgeService.cs`

Observed C# state:

- `PlayerEnterWorldRepository.SaveInventoryItemCountAsync` is a private helper used by several transaction-specific methods. Its SQL shape is owner-checked: `UPDATE inventory SET item_count = ? WHERE item_unique_id = ? AND item_owner = ?`.
- `HouseAuctionRepository.UpdateKinahAsync` uses the same owner-checked count update shape.
- `MailRepository.UpdateInventoryItemCountAsync` updates by item object id only and should not be used as the model for scheduled bind-point payment.
- `BrokerRepository.UpsertInventoryItemAsync` writes a broader row shape, but it is broker-specific and should not be reused as a bind-point dirty-storage equivalent.
- `BindPointTeleportScheduledKinahMutationPlanService` and runtime callback execution currently carry non-live mutation/update metadata only. No repository write or packet send is live.

## Recommended Contract Shape

Prefer a small bind-point-specific boundary rather than extending the large enter-world repository interface first:

```csharp
public interface IBindPointTeleportKinahPersistence
{
	Task<BindPointTeleportKinahPersistenceResult> SaveScheduledKinahAsync(
		int playerObjectId,
		InventoryItem kinahItem,
		CancellationToken cancellationToken = default);
}

public enum BindPointTeleportKinahPersistenceStatus
{
	Saved,
	MissingRow,
	Failed
}

public sealed record BindPointTeleportKinahPersistenceResult(
	BindPointTeleportKinahPersistenceStatus Status,
	int PlayerObjectId,
	int KinahObjectId,
	long KinahCount,
	bool ShouldRollbackInMemoryMutation,
	string JavaSource,
	bool IsLive);
```

This is a planned shape, not an implemented API in this unit.

Minimum SQL shape for the eventual MySQL adapter:

```sql
UPDATE inventory
SET item_count = ?
WHERE item_unique_id = ? AND item_owner = ?
```

Required result behavior:

- `Saved`: exactly one owner-checked row was updated. The caller may proceed to inventory packet send and then cooldown/action `3` fanout.
- `MissingRow`: zero rows were updated. The caller must roll back or discard the in-memory mutation and stop before packet send, cooldown, fanout, and movement.
- `Failed`: database exception or transaction failure. The caller must roll back or discard the in-memory mutation and stop before packet send, cooldown, fanout, and movement.

## Contract Rules

- Do not update by `item_unique_id` alone.
- Do not delete Kinah when count becomes zero.
- Do not broad-upsert from bind-point callbacks unless a Java-like dirty storage lifecycle is explicitly introduced.
- Do not send `SmInventoryUpdateItem.DecreaseKinahFly` until the selected persistence policy reports success.
- If C# keeps the recommended persist-before-send policy, document it as an `Intentional Difference` from Java's packet-before-later-persistence lifecycle.
- Keep the repository contract isolated from `GameServerConnection`; the network dispatch path should consume a higher-level mutation/persistence/send result.

## Migration Parity Table - UOW-1229

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.dao.InventoryDAO` | planned `Aion.GameServer.Services.IBindPointTeleportKinahPersistence` / MySQL adapter | Repository / Persistence | Not Started | No Tests | Unknown | Java persists dirty items later through `InventoryDAO.store(player)` and broad item-row batches. C# contract is planned only; no live SQL method was added. |
| `com.aionemu.gameserver.model.items.storage.Storage.tryDecreaseKinah` | `Aion.GameServer.Services.BindPointTeleportScheduledKinahMutationPlanService`; planned persistence boundary | Storage / Mutation | Partial | Unit Tested for non-live planner | Needs Verification | Non-live planner models mutation metadata, but persistence ownership and rollback remain design-only. |
| `com.aionemu.gameserver.model.items.storage.Storage.decreaseItemCount` | planned live inventory owner plus persistence result handling | Storage / Count Mutation | Partial | Manual Only | Needs Verification | Java sends packet and marks dirty during mutation. C# first live policy should persist before send unless a dirty-state lifecycle is added. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemUpdatePacket` | future send adapter gated by `BindPointTeleportKinahPersistenceResult.Saved` | Packet Utility / Send Boundary | Partial | Unit Tested for packet mask | Needs Verification | Packet send remains unwired. Repository failure must suppress packet send, cooldown, fanout, and movement. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem` | Packet / Serialization | Partial | Unit Tested | Needs Verification | C# can serialize `DecreaseKinahFly`, but no live send or Java runtime capture exists. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` scheduled callback | future C# scheduled Kinah mutation/persistence/send adapter plus runtime callback bridge | Service / Callback Boundary | Partial | Unit Tested for metadata | Needs Verification | Contract plan keeps live callback disabled until owner-checked persistence and rollback/send policy exist. |

Tests added:

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| None in UOW-1229 | Documentation-only repository contract plan. | Manual Java/C# source inspection only. |

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 0 live artifacts; 1 repository contract plan completed
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: 1 live repository adapter plus inventory owner/rollback/send policy
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- No live repository method or MySQL adapter exists for scheduled bind-point Kinah persistence.
- The planned C# persist-before-send ordering differs from Java unless a dirty-state lifecycle is introduced.
- No inventory owner/lock or rollback helper exists for scheduled callback mutation.
- Java `InventoryDAO.store` full-row dirty persistence is broader than the planned count-only SQL shape.
- Java runtime packet/storage comparison was not executed.
- Reflection behavior did not change. Date/time behavior did not change. Threading, persistence, serialization, packet-order, and rollback parity remain `Needs Verification`.

## Next Recommended Unit of Work

Add a non-live persistence result composition bridge that consumes scheduled Kinah mutation metadata plus a supplied persistence result and produces the exact next callback decision: rollback/stop on missing row or failure, and only allow inventory update packet metadata plus cooldown/action `3` fanout metadata on `Saved`. Keep it test-only/non-live: no SQL, no packet send, no `GameServerConnection` dispatch, and no movement.

Update after UOW-1230: the persistence result decision bridge is implemented and tested. Next, add a non-sending inventory update packet adapter that only produces a concrete packet intent after `Saved`.

Update after UOW-1237: `BindPointTeleportKinahPersistenceOperationPlanService` now implements the pure owner-checked count-update contract and supplied result mapper. It still does not execute SQL; live repository execution remains blocked on an explicit adapter and owner/rollback wiring.
