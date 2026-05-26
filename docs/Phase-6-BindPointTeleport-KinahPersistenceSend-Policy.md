# Phase 6 Bind-Point Teleport Kinah Persistence/Send Policy

Date: May 26, 2026
Unit of Work: UOW-1228
Scope: Read-only policy audit for the future live scheduled bind-point Kinah persistence and inventory update send boundary.
Source of truth: Java project.

## Policy Result

Do not wire a live scheduled Kinah persistence/send adapter yet. Java does not persist `Storage.tryDecreaseKinah(price, DEC_KINAH_FLY)` immediately inside the scheduled callback. It mutates storage, sends the inventory update packet during the mutation, and marks storage `PersistentState.UPDATE_REQUIRED`; persistence later flows through `InventoryDAO.store(player)` and dirty item collection. C# currently has immediate, feature-specific SQL helpers for many inventory flows, so a live bind-point adapter must document and test any intentional ordering difference before it writes to the database or sends packets.

Recommended C# policy for the first live adapter:

1. Keep the callback mutation owner atomic and in-memory first.
2. Emit/send `SmInventoryUpdateItem.DecreaseKinahFly` only after the in-memory mutation succeeds.
3. Use an owner-checked persistence contract, but stage it behind an explicit policy gate rather than silently copying handler-local SQL.
4. If C# persists before packet send, document it as an intentional difference from Java's dirty-state lifecycle and test the failure branch.
5. Never continue to cooldown/action `3` fanout when the selected persistence policy reports failure.

Update after UOW-1229: `docs/Phase-6-BindPointTeleport-KinahRepositoryContract-Plan.md` now pins a narrow owner-checked repository contract plan. No live SQL method was added; future C# work should first consume a supplied persistence result in a non-live callback decision bridge before wiring a MySQL adapter.

Update after UOW-1230: `BindPointTeleportKinahPersistenceDecisionBridgeService` now applies that supplied persistence-result gate. `Saved` can continue to packet/fanout metadata, while `MissingRow`, `Failed`, and missing result stop before packet send, cooldown/action `3` fanout, and movement.

Update after UOW-1231: `BindPointTeleportKinahInventoryUpdatePacketPlanService` now creates a concrete non-sending `SmInventoryUpdateItem` intent only after a `ContinueAfterPersistence` decision. Live send remains disabled.

## Java Facts

Java source files reviewed:

- `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
- `game-server/src/com/aionemu/gameserver/model/items/storage/PlayerStorage.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_INVENTORY_UPDATE_ITEM.java`
- `game-server/src/com/aionemu/gameserver/dao/InventoryDAO.java`
- `game-server/src/com/aionemu/gameserver/dao/PlayerDAO.java`

Observed Java behavior:

1. `Storage.tryDecreaseKinah(amount, updateType, actor)` checks `getKinah() >= amount`, then calls `decreaseKinah(amount, updateType, actor)`.
2. `Storage.decreaseKinah` mutates only when `amount > 0`.
3. `Storage.decreaseItemCount` decreases item count, sends `ItemPacketService.sendItemPacket(actor, storageType, item, updateType)` when the actor exists, then calls `setPersistentState(PersistentState.UPDATE_REQUIRED)`.
4. Kinah is not deleted when the count reaches zero.
5. `ItemPacketService.sendItemUpdatePacket` sends `SM_INVENTORY_UPDATE_ITEM(player, item, DEC_KINAH_FLY)` for cube storage.
6. `InventoryDAO.store(Player)` gathers `player.getDirtyItemsToUpdate()` and persists dirty items later.
7. `InventoryDAO.store(List<Item>, ...)` batches delete/insert/update and commits inside those helper methods. It sets every passed item's persistent state to `UPDATED` after the try/catch, even when a batch result is false.
8. `InventoryDAO.updateItems` updates the full item row, including count, owner, location, equipment, skin, charge, tune, random bonuses, and other item fields, keyed by object id.

## Current C# Facts

C# surfaces reviewed:

- `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportScheduledKinahMutationPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportScheduledCallbackPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportRuntimeCallbackExecutionBridgeService.cs`
- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/src/Aion.GameServer/Data/HouseAuctionRepository.cs`
- `dotnetConversion/src/Aion.GameServer/Data/MailRepository.cs`
- `dotnetConversion/src/Aion.GameServer/Data/BrokerRepository.cs`

Observed C# state:

- `BindPointTeleportScheduledKinahMutationPlanService` models future mutation results but remains non-live.
- `BindPointTeleportScheduledCallbackPlanService` and `BindPointTeleportRuntimeCallbackExecutionBridgeService` can carry update metadata but do not persist or send inventory packets.
- `PlayerEnterWorldRepository.SaveInventoryItemCountAsync` and `HouseAuctionRepository.UpdateKinahAsync` use owner-checked count-only SQL: `UPDATE inventory SET item_count = ? WHERE item_unique_id = ? AND item_owner = ?`.
- `MailRepository.UpdateInventoryItemCountAsync` updates only by item object id; this is not sufficient for bind-point scheduled payment because it omits owner checking.
- `BrokerRepository.UpsertInventoryItemAsync` handles broader row shape, but it belongs to broker transactions and should not be reused blindly for bind-point callback persistence.

## Recommended Repository Contract

A future live bind-point persistence contract should be narrow and owner-checked:

```csharp
Task<bool> SaveInventoryItemCountAsync(
	int playerObjectId,
	InventoryItem item,
	CancellationToken cancellationToken);
```

Minimum SQL shape:

```sql
UPDATE inventory
SET item_count = ?
WHERE item_unique_id = ? AND item_owner = ?
```

Policy notes:

- Return `false` if no row is updated.
- Do not update by `item_unique_id` alone.
- Do not delete the Kinah item when count becomes zero.
- Do not update unrelated item columns unless a broader storage persistence layer is intentionally introduced.
- Keep repository calls out of `BindPointTeleportRuntimeCallbackExecutionBridgeService` until an owner/lock boundary owns mutation and rollback semantics.

## Recommended Send/Persist Ordering For C#

The first live implementation should use this staged order:

1. Acquire the future per-player inventory mutation owner/lock.
2. Re-check current Kinah and compute the updated item.
3. Apply the in-memory player inventory update.
4. Persist the owner-checked item count.
5. If persistence fails, stop before inventory packet send, cooldown insertion, action `3` fanout, and final movement. Roll back the in-memory item count or return a result that forces caller rollback.
6. Send `SmInventoryUpdateItem(updatedKinah, kinahTemplate, SmInventoryUpdateItem.DecreaseKinahFly)`.
7. Continue to cooldown insertion and action `3` fanout.

This ordering intentionally differs from Java's packet-before-later-persistence lifecycle. The reason is C# currently lacks Java's dirty item persistence lifecycle and would otherwise send a success packet for a database write that immediately failed. This must be documented as `Intentional Difference` if/when implemented live.

## Migration Parity Table - UOW-1228

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.items.storage.Storage.tryDecreaseKinah` | `Aion.GameServer.Services.BindPointTeleportScheduledKinahMutationPlanService`; future live mutation/persistence owner | Storage / Mutation | Partial | Unit Tested for non-live planner | Needs Verification | Java sends packet during storage mutation and marks storage dirty. C# has non-live metadata only; persistence/send policy remains design-only. |
| `com.aionemu.gameserver.model.items.storage.Storage.decreaseItemCount` | future C# live mutation owner | Storage / Count Mutation | Partial | Manual Only | Needs Verification | Java decreases item count, sends update packet, and sets storage update-required. C# must choose owner/lock plus rollback policy before live mutation. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemUpdatePacket` | future C# inventory update send adapter using `SmInventoryUpdateItem.DecreaseKinahFly` | Packet Utility / Send Boundary | Partial | Unit Tested for packet mask | Needs Verification | Java sends before persistence lifecycle. Recommended first C# live policy persists before send as an intentional difference unless a Java-like dirty-state lifecycle is introduced. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem` | Packet / Serialization | Partial | Unit Tested | Needs Verification | C# can serialize trailing mask `0x4B`; no Java runtime packet capture. |
| `com.aionemu.gameserver.dao.InventoryDAO` | future owner-checked inventory item count persistence contract | Repository / Persistence | Not Started | No Tests | Unknown | Java persists dirty items later through full-row update keyed by item object id. C# needs a narrow owner-checked count persistence contract or explicit dirty-state lifecycle. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` scheduled callback | future live C# scheduled Kinah persistence/send adapter plus existing runtime callback bridge | Service / Callback Boundary | Partial | Unit Tested for metadata | Needs Verification | Live callback must not continue to cooldown/action `3` fanout if selected C# persistence policy fails. |

Tests added:

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| None in UOW-1228 | Documentation-only persistence/send policy audit. | Manual Java/C# source inspection only. |

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 0 live artifacts; 1 persistence/send policy audit completed
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: 1 repository contract plus live owner/lock/send policy
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- Live persistence and packet send remain blocked.
- Recommended first C# ordering may become an intentional difference from Java packet-before-later-persistence behavior.
- No owner/lock exists yet for scheduled inventory mutation.
- Java `InventoryDAO.store` has broad dirty-item lifecycle behavior not represented in C# for this path.
- Java runtime packet/storage comparison was not executed.
- Reflection behavior did not change. Date/time behavior did not change. Threading, persistence, serialization, and packet-order parity remain `Needs Verification`.

## Next Recommended Unit of Work

Add a non-live repository contract plan for the scheduled Kinah persistence boundary, preferably a small interface/result DTO or doc-only contract that can be implemented later by an owner-checked SQL adapter. Keep `GameServerConnection`, packet sends, and live mutation disabled.

Update after UOW-1229: the repository contract plan is complete. Next, add a non-live persistence result composition bridge that proves persistence failure stops before inventory packet send, cooldown/action `3` fanout, and movement.

Update after UOW-1230: the persistence result decision bridge is complete. Next, add a non-sending inventory update packet adapter gated by `ContinueAfterPersistence`.

Update after UOW-1231: the non-sending packet adapter is complete. Next, compose persistence decision plus packet intent into callback result metadata before live send/dispatch.
