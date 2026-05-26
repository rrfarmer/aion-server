# Phase 6 Bind-Point Teleport Scheduled Kinah Live Boundary Audit

Date: May 26, 2026
Unit of Work: UOW-1222
Scope: Read-only audit for the Java scheduled `tryDecreaseKinah(price, DEC_KINAH_FLY)` boundary before enabling live bind-point callback inventory mutation.
Source of truth: Java project.

## Audit Result

Do not enable live bind-point scheduled Kinah mutation in the next `GameServerConnection` dispatch slice. Java `tryDecreaseKinah(price, ItemPacketService.ItemUpdateType.DEC_KINAH_FLY)` is a storage mutation, packet-send, and persistence-state boundary, not only a scalar affordability check. C# currently has source-derived planners for the branch and generic packet support for inventory updates, but no shared live inventory owner that can atomically decrement the Kinah item, persist the item count, emit the fly/teleport inventory update mask, and then hand control to cooldown/fanout in the same Java order.

Update after UOW-1223: C# now has named packet-level `SmInventoryUpdateItem.DecreaseKinahFly = 0x4B` coverage and a source-derived packet test proving the trailing update mask can be emitted for a Kinah item. This only satisfies the packet-mask prerequisite; live inventory mutation, failure-message send, persistence, and callback dispatch remain disabled.

Update after UOW-1224: `docs/Phase-6-BindPointTeleport-KinahMutationOwner-Design.md` now pins the future live owner/boundary shape for scheduled Kinah mutation. It recommends a non-live mutation planner next, before any live persistence or packet send is wired.

Update after UOW-1225: `BindPointTeleportScheduledKinahMutationPlanService` now models missing/insufficient Kinah, exact Kinah to zero, positive decrement, non-positive no-mutation success, unrelated inventory preservation, and `DecreaseKinahFly` packet intent metadata. It remains non-live and does not persist or send packets.

Update after UOW-1226: scheduled callback plans now carry optional mutation-plan metadata. Success places the Kinah update/packet intent before cooldown/fanout metadata; failure still stops before cooldown/fanout/movement. Live mutation, persistence, and packet sends remain disabled.

Update after UOW-1227: runtime callback execution results now surface the supplied Kinah update/packet-intent metadata before cooldown/action `3` fanout completes. This is still non-sending metadata only.

## Java Facts

Java source files reviewed:

- `game-server/src/com/aionemu/gameserver/services/teleport/BindPointTeleportService.java`
- `game-server/src/com/aionemu/gameserver/model/items/storage/IStorage.java`
- `game-server/src/com/aionemu/gameserver/model/items/storage/PlayerStorage.java`
- `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_INVENTORY_UPDATE_ITEM.java`

Observed Java behavior:

1. The delayed bind-point `TaskId.SKILL_USE` callback starts with `player.getInventory().tryDecreaseKinah(price, ItemPacketService.ItemUpdateType.DEC_KINAH_FLY)`.
2. Failure sends `SM_SYSTEM_MESSAGE.STR_CANNOT_MOVE_TO_AIRPORT_NOT_ENOUGH_FEE()` and returns before cooldown insertion, action `3` fanout, and final movement.
3. `PlayerStorage.tryDecreaseKinah` delegates to `Storage.tryDecreaseKinah(amount, updateType, actor)`.
4. `Storage.tryDecreaseKinah` checks `getKinah() >= amount`; success calls `decreaseKinah(amount, updateType, actor)` and returns `true`.
5. `Storage.decreaseKinah` only mutates when `amount > 0`, then calls `decreaseItemCount(kinahItem, amount, updateType, actor)`.
6. `Storage.decreaseItemCount` decreases the item count, sends an item packet when an actor exists, marks the storage `PersistentState.UPDATE_REQUIRED`, and returns the left count.
7. Kinah is not deleted when its count reaches zero because `Storage.decreaseItemCount` checks `item.getItemTemplate().isKinah()`.
8. `ItemPacketService.ItemUpdateType.DEC_KINAH_FLY` has mask `0x4B` and is sendable.
9. Cube-storage Kinah updates route through `ItemPacketService.sendItemUpdatePacket` to `SM_INVENTORY_UPDATE_ITEM(player, item, DEC_KINAH_FLY)`.
10. `SM_INVENTORY_UPDATE_ITEM.writeImpl` writes item object id, item template l10n name, a full `ItemInfoBlob` for normal sendable update types, then writes the update mask as `writeH(updateType.getMask())`.

## Current C# Facts

C# surfaces reviewed:

- `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportScheduledKinahPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportScheduledCallbackPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportRuntimeCallbackExecutionBridgeService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/TeleportTransportationPricePlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/CraftSkillUpdateService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmInventoryUpdateItem.cs`
- `dotnetConversion/src/Aion.GameServer/Data/BrokerRepository.cs`
- `dotnetConversion/src/Aion.GameServer/Data/HousingRepository.cs`
- `dotnetConversion/src/Aion.GameServer/Data/MailRepository.cs`

Observed C# state:

- `BindPointTeleportScheduledKinahPlanService` records `ItemPacketService.ItemUpdateType.DEC_KINAH_FLY` and mask `0x4B`, but `IsLive` remains `false`.
- `BindPointTeleportRuntimeCallbackExecutionBridgeService` consumes supplied Kinah-success/failure metadata and only performs cooldown/fanout when metadata already indicates success.
- `SmInventoryUpdateItem` now has constants for several Java masks, including named `DecreaseKinahFly = 0x4B`.
- `SmInventoryUpdateItem` serializes the full item info blob plus the supplied update type, matching the Java normal sendable update-type shape in source-derived form.
- Existing live or semi-live Kinah consumers copy/update `InventoryItem` values locally and emit `SmInventoryUpdateItem` in some handlers, but the repository persistence surfaces are feature-specific and not a reusable Java `Storage.tryDecreaseKinah` equivalent.
- Repository code has inventory upsert/update helpers in broker, housing, and mail areas, but there is no audited bind-point-specific transaction or shared inventory repository method guaranteeing the Java callback order.

## Required Live Boundary Before Dispatch

A live bind-point scheduled Kinah mutation should be added only after these contract points are explicit and tested:

1. Locate the player's cube Kinah item by Java Kinah item id `182400001` and cube storage id `0`.
2. Treat missing Kinah item or count below price as Java failure: send `SmSystemMessage.CannotMoveToAirportNotEnoughFee()` and stop before cooldown/fanout/movement.
3. On success, decrease the in-memory Kinah count without deleting the Kinah item when it reaches zero.
4. Emit `SmInventoryUpdateItem` with named `DecreaseKinahFly` mask `0x4B` after the mutation.
5. Persist the updated Kinah item count or explicitly stage persistence as blocked with rollback/failure ordering documented.
6. Only after the Kinah success boundary continue to `AddCooldown`, action `3` fanout, and final movement scheduling.
7. Keep the final movement side effect disabled until the existing movement audit gates are satisfied.

## Live Implementation Risks

- Threading: Java storage mutation happens inside the scheduled task against the player inventory. C# needs an owner/lock policy so the delayed callback cannot race other inventory payments.
- Persistence: Java marks storage `PersistentState.UPDATE_REQUIRED`; C# currently has feature-specific SQL helpers, so live bind-point persistence needs a clear transaction boundary and failure policy.
- Serialization: `SmInventoryUpdateItem` can carry named `DecreaseKinahFly = 0x4B`, but no Java runtime byte capture has verified the full Kinah item blob.
- Packet order: Java sends the inventory update during the Kinah decrement before cooldown/action `3` fanout.
- Date/time: no new date/time behavior in this audit, but the callback still depends on the previously staged cooldown clock.
- Reflection: no reflection behavior is involved.
- Precision/rounding: no new price math in this audit; it relies on the existing bind-point price planner.

## Migration Parity Table - UOW-1222

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` scheduled Kinah callback branch | `Aion.GameServer.Services.BindPointTeleportScheduledKinahPlanService`; `BindPointTeleportRuntimeCallbackExecutionBridgeService` | Service / Callback Boundary | Partial | Unit Tested | Needs Verification | C# records and consumes scheduled Kinah success/failure metadata, but actual live `tryDecreaseKinah`, inventory packet send, persistence, and fee-failure send remain disabled. |
| `com.aionemu.gameserver.model.items.storage.PlayerStorage` | future C# player inventory owner or adapter | Storage / Inventory Owner | Not Started | No Tests | Unknown | Java delegates player inventory Kinah decrements to `Storage.tryDecreaseKinah`. C# lacks a shared audited equivalent for bind-point scheduled callbacks. |
| `com.aionemu.gameserver.model.items.storage.Storage` | future C# live Kinah mutation boundary | Storage / Mutation | Not Started | No Tests | Unknown | Java checks current Kinah, decrements item count, never deletes the Kinah item at zero, sends item update when actor exists, and marks storage update-required. |
| `com.aionemu.gameserver.services.item.ItemPacketService.ItemUpdateType.DEC_KINAH_FLY` | `Aion.GameServer.Services.BindPointTeleportScheduledKinahPlanService.DecKinahFlyUpdateTypeMask`; future `SmInventoryUpdateItem.DecreaseKinahFly` | Enum / Packet Mask | Partial | Unit Tested | Needs Verification | Planner records mask `0x4B`; `SmInventoryUpdateItem` does not yet expose a named fly/teleport Kinah constant. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemUpdatePacket` | future C# send boundary using `SmInventoryUpdateItem` | Packet Utility / Send Boundary | Partial | Manual Only | Needs Verification | C# packet type exists and accepts an update mask, but no bind-point live send path emits it in Java order. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem` | Packet / Serialization | Partial | Unit Tested in other flows | Needs Verification | Normal update path writes item id, client name, full item blob, and update mask. No Java runtime byte capture for `DEC_KINAH_FLY`. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_CANNOT_MOVE_TO_AIRPORT_NOT_ENOUGH_FEE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage.CannotMoveToAirportNotEnoughFee` | Packet / Failure Message | Partial | Unit Tested | Needs Verification | Named helper exists, but scheduled callback live failure send is not wired. |

Tests added:

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| None in UOW-1222 | Documentation-only audit of the scheduled Kinah live mutation boundary. | Manual Java/C# source inspection only. |

## Summary Metrics

- Total Java artifacts discovered: 7 grouped artifact rows in this unit
- Total artifacts ported: 0 live artifacts; 1 live-boundary audit completed
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: 2 grouped rows plus live persistence/threading policy
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- Live bind-point scheduled Kinah mutation is still blocked.
- Failure-message send for scheduled Kinah failure remains unwired.
- Inventory update packet order and persistence behavior are not live.
- Java storage persistence-state behavior does not yet have a shared C# equivalent for this path.
- Threading/concurrency behavior for scheduled inventory mutation remains unverified.
- No Java runtime packet capture was executed, so serialization parity remains source-derived only.

## Next Recommended Unit of Work

Add the smallest executable prerequisite for the audited boundary: introduce a named `SmInventoryUpdateItem.DecreaseKinahFly` constant and a packet/unit test proving that C# can serialize a Kinah inventory update with mask `0x4B`. Keep it packet-level only: no live inventory mutation, no repository write, no `GameServerConnection` dispatch, and no movement.

Update after UOW-1223: this packet-level prerequisite is complete. The next recommended unit is a shared/live Kinah mutation owner design audit for the scheduled callback path, including threading, persistence failure policy, and Java packet ordering.

Update after UOW-1224: the owner design audit is complete. Next, add a non-live mutation planner that produces future packet/persistence intent metadata without touching live inventory from `GameServerConnection`.

Update after UOW-1225: the non-live mutation planner exists. Next, compose it into scheduled callback metadata or add the persistence-boundary contract before live mutation.

Update after UOW-1226: mutation metadata is composed into scheduled callback plans. Next, carry it through runtime callback execution as a non-sending inventory-update intent, then design persistence/send boundaries.

Update after UOW-1227: runtime callback execution now carries the non-sending inventory-update intent. Next, design the persistence/send boundary and rollback policy before live mutation.
