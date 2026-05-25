# ItemPurification Persistence Plan

Date: May 25, 2026
Unit of Work: UOW-953

## Purpose

This document maps Java `CM_ITEM_PURIFICATION` live mutation persistence to the current C# repository surfaces before automatic live handler execution is enabled.

Java remains the source of truth. This is an analysis/planning unit only; it does not claim runtime parity.

## Java Write Path

Java source breadcrumbs:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_ITEM_PURIFICATION.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemPurificationService.java`
- `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
- `game-server/src/com/aionemu/gameserver/model/items/storage/ItemStorage.java`
- `game-server/src/com/aionemu/gameserver/dao/InventoryDAO.java`
- `game-server/src/com/aionemu/gameserver/dao/AbyssRankDAO.java`

Observed Java sequence:

1. `CM_ITEM_PURIFICATION.runImpl` resolves the player and base item.
2. `ItemPurificationService.isPurificationAllowed` validates template/result/identified/enchant/AP/kinah/material counts and sends the success system message before mutation.
3. `decreaseMaterials` consumes required material stacks with `Storage.decreaseByItemId`, spends AP with `AbyssPointsService.addAp(player, -necessaryAP)`, calls `Storage.decreaseKinah(-necessaryKinah)`, then consumes the base item with `Storage.decreaseByObjectId`.
4. `upgradeItem` creates the target item, copies inherited item state, and adds it through inventory storage.

Important Java persistence facts:

- `Storage.decreaseKinah(long amount)` mutates only when `amount > 0`; the purification call passes `-necessaryKinah`, so the current source path is a no-op for kinah.
- `Storage.decreaseItemCount` marks the storage `UPDATE_REQUIRED`. If a non-kinah stack reaches zero, `delete` removes it from `ItemStorage`, marks the `Item` as `DELETED`, queues it in `deletedItems`, sends a delete packet, and fires `QuestEngine.onItemRemoved`.
- `Storage.add` sets `item_location`, marks storage `UPDATE_REQUIRED`, sends a storage update packet, and fires `QuestEngine.onItemGet` for cube storage.
- `InventoryDAO.store(Player)` collects `player.getDirtyItemsToUpdate()` and splits changed items into update/insert/delete sets using persistent state.
- `InventoryDAO.store(List<Item>, ...)` opens one connection and disables autocommit, but `deleteItems`, `insertItems`, and `updateItems` each call `con.commit()` independently. This means Java batches by category, not as one all-or-nothing transaction across delete/insert/update.
- `AbyssRankDAO.storeAbyssRank` inserts or updates based on rank persistent state and then marks the rank updated.

## C# Current Surface

C# source breadcrumbs:

- `dotnetConversion/src/Aion.GameServer/Services/ItemPurificationLiveMutationService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/ItemPurificationMutationSnapshotService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/AbyssPointsService.cs`
- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`

Current C# behavior:

- `ItemPurificationLiveMutationService.Apply` replaces `Player.InventoryItems` with generated post-mutation snapshots and spends AP through `AbyssPointsService.AddAp`.
- The service intentionally does not persist, send quest notifications, execute AP side-effect packets, perform rollback, or mutate kinah.
- `IPlayerEnterWorldRepository` has action-specific transaction methods for similar item mutations, including assembly, extraction, item charge, enchant/socket/amplification, and AP extraction.
- `MySqlPlayerEnterWorldRepository` already contains reusable private helpers for inventory insert, inventory delete including `item_stones`, inventory count/state updates, and `SaveAbyssRankAsync`.

## Proposed C# Transaction Contract

Add an ItemPurification-specific repository method rather than a broad generic inventory transaction.

Implementation status as of UOW-957: the method signature, empty-repository recording stub, MySQL repository method, pure persistence payload mapper, inserted-item `item_stones` row persistence, an explicit opt-in persistent live execution seam, and a handler-level opt-in helper exist. Automatic `HandleInfrastructurePacketAsync` invocation remains disabled, real DB integration verification is still missing, and quest/AP side-effect execution remains incomplete.

Recommended signature:

```csharp
Task<bool> SaveItemPurificationMutationAsync(
	int playerObjectId,
	IReadOnlyList<InventoryItem> materialItemUpdates,
	IReadOnlyList<int> deletedMaterialItemObjectIds,
	InventoryItem? baseItemUpdate,
	int? deletedBaseItemObjectId,
	IReadOnlyList<InventoryItem> updatedTargetItems,
	IReadOnlyList<InventoryItem> addedTargetItems,
	PlayerAbyssRank? abyssRank,
	CancellationToken cancellationToken = default);
```

Recommended write order for C#:

1. Begin a MySQL transaction.
2. Save material count updates.
3. Delete exhausted material object ids.
4. Save base item count update or delete the base item object id.
5. Save updated target stacks, if stack merge ever applies.
6. Insert added target items.
7. Save abyss rank when AP was spent.
8. Commit.

This intentionally improves atomicity over Java's category-level commits while preserving the same functional write set. Document this as an intentional C# persistence safety difference, not verified Java parity.

## Data Required From Existing Plans

The next implementation should derive repository inputs from `ItemPurificationMutationSnapshotPlan` and `AbyssPointsAddPlan`.

Needed mapping:

- Material count updates: `PostMutationInventoryItems` entries matching material object ids that survived with lower counts.
- Material deletes: material object ids removed by the preview.
- Base update/delete: usually delete object id for count-one equipment/base item; keep update path for stack-like safety.
- Target updates/adds: target snapshots from mutation preview.
- Abyss rank: `AbyssPointsAddPlan.UpdatedRank` when AP spend occurred.
- Kinah: no repository write while matching Java's current negative-amount no-op.

## Required Tests Before Enabling Automatic Handler Execution

Recommended focused tests:

- Repository fake test: live execution returns a persistence payload containing material update/delete, base delete, target add, and AP rank update.
- Repository unit/integration test: `SaveItemPurificationMutationAsync` calls update/delete/insert/save-rank paths in one transaction and returns false without commit when a required delete fails.
- Handler guard test: automatic `HandleInfrastructurePacketAsync` remains plan-only until the persistence method is wired and tested.
- Regression test: kinah remains unchanged and no kinah repository update is requested for Java's `decreaseKinah(-necessaryKinah)` behavior.
- Failure-ordering test/design: document or test what happens when live packet send/mutation succeeds but repository persistence fails before any automatic dispatch path can use the persistent seam.
- Handler opt-in helper test: explicit callers can invoke mutation+packet+persistence through `GameServerConnection.HandleItemPurificationPersistentLiveExecutionAsync` without enabling automatic packet dispatch.

## Open Gaps

- Java runtime packet/DB capture is still unavailable locally.
- C# does not model Java `PersistentState`, `Storage.deletedItems`, `QuestEngine.onItemRemoved`, or `QuestEngine.onItemGet`.
- AP side-effect execution is still only represented as `AbyssPointsAddPlan` metadata. Rank-limit equipment checks, abyss skill updates, Legion contribution, Siege callback, and packet fanout are not persisted/executed here.
- `InsertInventoryItemAsync` now inserts inherited socket/godstone/fusion/idian rows for newly copied purification targets, but this has unit coverage for row mapping only. Live DB integration and Java runtime comparison are still missing.
- Threading differs: Java `ItemStorage` uses a `ConcurrentHashMap` and storage queues; C# currently replaces immutable snapshots on the `Player`.
- Transaction behavior differs if C# commits all writes atomically; this should remain an intentional safety difference unless runtime Java behavior proves partial commits are required.
