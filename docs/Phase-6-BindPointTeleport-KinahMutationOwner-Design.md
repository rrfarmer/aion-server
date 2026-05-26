# Phase 6 Bind-Point Teleport Kinah Mutation Owner Design

Date: May 26, 2026
Unit of Work: UOW-1224
Scope: Design audit for the future live C# owner/boundary that will execute Java `Storage.tryDecreaseKinah(price, DEC_KINAH_FLY)` during the bind-point scheduled callback.
Source of truth: Java project.

## Design Result

Do not implement live bind-point Kinah mutation by copying one of the existing handler-local item-count updates. C# needs an explicit owner/boundary for this path because Java performs the scheduled payment as a single storage operation whose success controls every later callback side effect. The C# boundary should serialize per-player inventory mutation, update the in-memory Kinah item, persist the item count with owner checking, emit `SmInventoryUpdateItem.DecreaseKinahFly`, and then return a result that the existing runtime callback bridge can use before cooldown/fanout.

Update after UOW-1225: C# now has a non-live `BindPointTeleportScheduledKinahMutationPlanService` that models the future in-memory mutation result and packet intent without persistence, packet sends, or live dispatch. The live owner/persistence boundary remains blocked.

Update after UOW-1226: `BindPointTeleportScheduledCallbackPlanService` can now carry supplied mutation-plan metadata, including updated Kinah item and `DecreaseKinahFly` packet intent, before cooldown/fanout metadata. This remains non-live.

Update after UOW-1227: `BindPointTeleportRuntimeCallbackExecutionBridgeService` now carries the callback Kinah update metadata into runtime execution results without sending packets or persisting. The live owner/persistence/send boundary remains blocked.

Update after UOW-1228: `docs/Phase-6-BindPointTeleport-KinahPersistenceSend-Policy.md` now pins the persistence/send policy. Java sends the inventory update during mutation and persists dirty items later; the recommended first C# live policy is owner-checked persist-before-send as an intentional difference unless a Java-like dirty-state lifecycle is introduced.

## Java Source Facts

Java source files:

- `game-server/src/com/aionemu/gameserver/services/teleport/BindPointTeleportService.java`
- `game-server/src/com/aionemu/gameserver/model/items/storage/PlayerStorage.java`
- `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`

Observed Java contract:

1. The scheduled `TaskId.SKILL_USE` callback calls `tryDecreaseKinah(price, DEC_KINAH_FLY)` before cooldown insertion.
2. Missing or insufficient Kinah returns `false`; the callback sends `STR_CANNOT_MOVE_TO_AIRPORT_NOT_ENOUGH_FEE` and returns.
3. Exact Kinah succeeds and leaves the Kinah item at zero.
4. Kinah is not deleted at zero.
5. Positive payment decreases the same Kinah item and sends the cube inventory update packet with `DEC_KINAH_FLY`.
6. `Storage.decreaseItemCount` marks storage `PersistentState.UPDATE_REQUIRED` after the mutation/send branch.

## C# Surface Facts

C# surfaces reviewed:

- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/Player.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmInventoryUpdateItem.cs`
- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/src/Aion.GameServer/Data/HouseAuctionRepository.cs`
- `dotnetConversion/src/Aion.GameServer/Data/MailRepository.cs`
- `dotnetConversion/src/Aion.GameServer/Services/ExpirableTaskService.cs`
- existing inventory-mutating services under `dotnetConversion/src/Aion.GameServer/Services`

Observed C# state:

- `Player.InventoryItems` is a replaceable `IReadOnlyList<InventoryItem>` with no intrinsic lock or storage owner.
- Many services mutate inventory by copying `player.InventoryItems` to a list, replacing one item, then assigning a new array.
- `ExpirableTaskService` demonstrates a per-registration `SyncRoot` lock pattern for delayed player-state mutations, but that lock is local to expirable registrations and not a general inventory owner.
- `PlayerEnterWorldRepository.SaveInventoryItemCountAsync` and `HouseAuctionRepository.UpdateKinahAsync` show owner-checked SQL update shapes: `UPDATE inventory SET item_count = ? WHERE item_unique_id = ? AND item_owner = ?`.
- `MailRepository.UpdateInventoryItemCountAsync` updates by item id only and should not be used as the model for bind-point payment because this path should guard owner identity.
- `SmInventoryUpdateItem.DecreaseKinahFly = 0x4B` exists and is packet-tested after UOW-1223.

## Recommended Boundary Shape

Future code should introduce a narrow live boundary with a shape close to:

- Input facts:
  - `Player player`
  - required price
  - `ItemTemplateSummary` for Kinah
  - send delegate or packet collector
  - persistence delegate/repository method
  - cancellation token
- Owned behavior:
  - serialize mutation for the player, preferably through a keyed owner or a lock associated with the player/session;
  - find cube Kinah item by item id `182400001` and location `0`;
  - fail before mutation when missing or `Count < price`;
  - create an updated Kinah item with `Count - price`;
  - replace `player.InventoryItems` with the updated item while preserving all unrelated inventory items;
  - persist with an owner-checked `UPDATE inventory ... WHERE item_unique_id = ? AND item_owner = ?`;
  - emit `SmInventoryUpdateItem(updatedKinah, kinahTemplate, SmInventoryUpdateItem.DecreaseKinahFly)`;
  - return status and updated item metadata for the scheduled callback bridge.

## Recommended Ordering

The future live callback should follow this order:

1. Execute the Kinah mutation boundary.
2. If it returns not-enough Kinah, send `SmSystemMessage.CannotMoveToAirportNotEnoughFee()` and stop.
3. If persistence fails, stop before cooldown/fanout/movement and document the rollback/send policy in tests.
4. Send or collect `SmInventoryUpdateItem.DecreaseKinahFly` for the updated Kinah item.
5. Continue into the existing cooldown insertion and action `3` fanout bridge.
6. Keep final movement disabled until the movement side-effect gate is implemented.

## Open Policy Questions

- Persistence failure: Java marks storage dirty and persistence is later/lifecycle-driven; C# repository helpers often persist immediately. The future live adapter must choose whether to mutate in-memory before or after DB success and how to recover on DB failure.
- Lock ownership: there is no general inventory lock today. A keyed owner like `ConcurrentDictionary<int, object>` or a connection/player session lock is safer than mutating `Player.InventoryItems` directly from a scheduled callback.
- Packet send timing: Java sends during `decreaseItemCount` before cooldown/fanout. If C# persists before sending, document that as an intentional ordering difference unless tests prove the chosen order matches an accepted live policy.
- Repository placement: avoid exposing private helper methods piecemeal. Prefer a small shared inventory repository method or bind-point-specific repository boundary with owner-checked SQL.

## Migration Parity Table - UOW-1224

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.items.storage.Storage.tryDecreaseKinah` | future C# Kinah mutation owner/boundary | Storage / Mutation | Not Started | No Tests | Unknown | Design audit pins required behavior: missing/insufficient fail, exact Kinah succeeds to zero, zero Kinah item is not deleted, packet mask is `DEC_KINAH_FLY`, and storage persistence must be addressed. |
| `com.aionemu.gameserver.model.items.storage.PlayerStorage` | `Aion.GameServer.Model.GameObjects.Player.InventoryItems` plus future owner | Storage / Inventory Owner | Partial | Manual Only | Needs Verification | C# currently stores inventory as a replaceable read-only list without a Java-like storage owner or lock. Future callback mutation needs explicit synchronization. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemPacket` / `sendItemUpdatePacket` | future live boundary using `SmInventoryUpdateItem.DecreaseKinahFly` | Packet Utility / Send Boundary | Partial | Unit Tested for packet mask | Needs Verification | Packet mask prerequisite exists, but live send timing remains unimplemented. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem` | Packet / Serialization | Partial | Unit Tested | Needs Verification | C# can emit the trailing `0x4B` mask; full Java runtime packet comparison is still missing. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` scheduled callback | future C# live scheduled Kinah mutation + existing runtime callback bridge | Service / Callback Boundary | Partial | Unit Tested for surrounding metadata | Needs Verification | Design audit keeps live mutation disabled until owner, persistence, packet send, and failure-message policy are implemented. |

Tests added:

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| None in UOW-1224 | Documentation-only design audit for the future live Kinah mutation owner. | Manual Java/C# source inspection only. |

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 0 live artifacts; 1 design audit completed
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: 1 storage mutation owner plus persistence/threading policy
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- Live scheduled Kinah mutation remains blocked.
- No shared C# inventory owner/lock exists yet.
- Immediate persistence vs Java dirty-state persistence remains an unresolved policy choice.
- Failure-message send and rollback behavior are not implemented.
- Java runtime packet comparison for the full Kinah update remains missing.
- Reflection behavior did not change. Date/time behavior did not change. Threading, persistence, serialization, and packet-order parity remain `Needs Verification`.

## Next Recommended Unit of Work

Implement a non-live `BindPointTeleportScheduledKinahMutationPlanService` that consumes a `Player` inventory snapshot and returns the exact future mutation result without persistence or packet sends. Cover missing Kinah, insufficient Kinah, exact Kinah to zero, positive decrement, unrelated inventory preservation, and `SmInventoryUpdateItem.DecreaseKinahFly` packet intent metadata. Keep it non-live and do not wire `GameServerConnection`.

Update after UOW-1225: this non-live planner is implemented and tested. The next recommended unit is to compose this mutation planner into `BindPointTeleportScheduledCallbackPlanService` metadata, still without live persistence or packet sends.

Update after UOW-1226: callback metadata composition is complete. The next recommended unit is to compose that mutation metadata into the runtime callback execution result as a non-sending packet intent, still without persistence or live inventory mutation.

Update after UOW-1227: runtime non-sending carry-through is complete. The next recommended unit is a persistence/send policy audit or repository contract plan for this exact callback boundary.

Update after UOW-1228: the persistence/send policy audit is complete. Next, add a non-live repository contract plan for owner-checked Kinah count persistence.
