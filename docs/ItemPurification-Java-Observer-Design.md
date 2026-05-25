# ItemPurification Java Observer Design

Date: May 25, 2026
Unit of Work: UOW-962

## Purpose

This document defines the Java-side observation shape needed to compare ItemPurification runtime packets and database writes against the C# port before production `CM_ITEM_PURIFICATION` dispatch is enabled.

Java remains the source of truth. This design does not generate artifacts yet and does not claim runtime parity.

## Java Source Breadcrumbs

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_ITEM_PURIFICATION.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemPurificationService.java`
- `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
- `game-server/src/com/aionemu/gameserver/dao/InventoryDAO.java`
- `game-server/src/com/aionemu/gameserver/dao/ItemStoneListDAO.java`
- `game-server/src/com/aionemu/gameserver/dao/AbyssRankDAO.java`
- `game-server/src/com/aionemu/gameserver/utils/PacketSendUtility.java`

## Runtime Sequence To Observe

The observer should record one complete successful purification attempt:

1. `CM_ITEM_PURIFICATION.readImpl` input:
   - `playerObjectId`
   - `upgradedItemObjectId`
   - `resultItemId`
   - five required-material object ids
2. `CM_ITEM_PURIFICATION.runImpl` resolves the active player and base item.
3. `ItemPurificationService.isPurificationAllowed` validates template/result/identified/enchant/AP/Kinah/material counts.
4. `isPurificationAllowed` sends `STR_ITEM_UPGRADE_MSG_UPGRADE_SUCCESS` before any material/base/target mutation.
5. `decreaseMaterials` consumes required material stacks through `Storage.decreaseByItemId`.
6. `decreaseMaterials` spends AP through `AbyssPointsService.addAp` when required.
7. `decreaseMaterials` calls `player.getInventory().decreaseKinah(-necessaryKinah)` when Kinah is required; current Java `Storage.decreaseKinah` mutates only when `amount > 0`, so this path is a no-op for positive `necessaryKinah`.
8. `decreaseMaterials` consumes the base item through `Storage.decreaseByObjectId`.
9. `upgradeItem` creates a new item, inherits source state, and calls `Storage.add`.
10. Later persistence saves dirty/deleted/new inventory rows, item stones, and AP rank state.

## Packet Observation Schema

Record packets in exact send order, before encryption if possible:

| Field | Description |
|---|---|
| `sequence` | Monotonic zero-based packet index for this player. |
| `packetClass` | Java server packet class name, such as `SM_SYSTEM_MESSAGE`. |
| `opcode` | Encoded packet opcode if available from the server packet instance or serialized payload. |
| `payloadHex` | Unencrypted serialized payload bytes. |
| `semanticKey` | Stable semantic label such as `upgrade-success`, `material-update`, `material-delete`, `base-delete`, `target-add`, `cube-update`, or `ap-rank-update`. |
| `itemObjectId` | Item object id involved, if the packet is item-specific. |
| `itemId` | Item template id involved, if available. |
| `updateType` | Java `ItemUpdateType`, `ItemDeleteType`, or add type when available. |
| `messageId` | System message id for `SM_SYSTEM_MESSAGE`, if available. |
| `messageParams` | Ordered system-message parameters, if available. |

Minimum expected packet categories for a successful cube purification:

- Success system message from `isPurificationAllowed`.
- One material update or delete packet per consumed material stack.
- `SM_CUBE_UPDATE` after each delete packet produced by `ItemPacketService.sendItemDeletePacket`.
- AP packets reached by `AbyssPointsService.addAp`, if AP is required.
- Base item delete/update packet and possible cube update.
- Target add packet from `ItemPacketService.sendStorageUpdatePacket`.
- Final cube update from target add.

Do not assume this category list is exhaustive. The observer output should record every packet sent through `PacketSendUtility.sendPacket(Player, AionServerPacket)`.

## Database Observation Schema

Record database state before and after Java persistence, not only the in-memory mutation:

| Table | Required Before/After Fields |
|---|---|
| `inventory` | `item_unique_id`, `item_id`, `item_count`, `item_owner`, `item_location`, `is_equiped`, `slot`, `optional_socket`, `optional_fusion_socket`, `enchant`, `enchant_bonus`, `item_skin`, `fusioned_item`, `charge`, `rnd_bonus`, `fusion_rnd_bonus`, `temperance`, `pack_count`, `is_amplified`, `buff_skill`, `rnd_plume_bonus`, creator/color/expire fields where present. |
| `item_stones` | `item_unique_id`, `item_id`, `slot`, `category`, `polishNumber`, `polishCharge`, `proc_count`. |
| `abyss_rank` | `player_id`, `ap`, `rank`, `daily_ap`, `weekly_ap`, `last_ap`, `last_update`, `gp`, `daily_gp`, `weekly_gp`, `last_gp`. |

The artifact should include a derived write set:

- material item count updates
- exhausted material deletes
- base item update or delete
- target item insert
- inherited target `item_stones` inserts
- AP rank update or insert

## Object-Id Normalization

Java target item object ids come from `ItemFactory.newItem`, which uses Java runtime object-id allocation. C# target object ids may come from a different allocator.

The comparison artifact should include a deterministic alias map:

| Alias | Java Value | C# Value | Match Rule |
|---|---|---|---|
| `base` | Java base item object id | C# base item object id | Input object id must match. |
| `material:N` | Java consumed material object id | C# consumed material object id | Match by input object id or by `item_id` plus pre-count when Java ignores required-material object ids. |
| `target:0` | Generated Java target object id | Generated C# target object id | Match by `item_id`, count, inherited fields, and first new target insert order. |

Packet and DB comparisons should normalize generated target object ids through this alias map before comparing payloads or row sets.

## Suggested Capture Points

Preferred low-intrusion Java capture points:

- Wrap or instrument `PacketSendUtility.sendPacket(Player, AionServerPacket)` to record packet class, serialized bytes, and best-effort semantic metadata.
- Add a test-only observer around `CM_ITEM_PURIFICATION.runImpl` input values and final result.
- Snapshot `inventory`, `item_stones`, and `abyss_rank` rows immediately before packet execution and after the normal Java save boundary used by the scenario.

If byte serialization is difficult to extract at `PacketSendUtility`, record packet class and semantic fields first, then add payload bytes in a later artifact version.

## Required Scenarios

Minimum scenarios before enabling automatic C# dispatch:

| Scenario | Purpose |
|---|---|
| Material count update plus base delete plus target add | Covers common non-exhausted material stacks and base consumption. |
| Exhausted material delete | Confirms delete packet, cube update, deleted queue, and inventory delete row behavior. |
| Inherited target stones | Confirms mana, fusion, godstone, and idian rows are inserted for generated target items. |
| AP spend | Confirms AP packet fanout and `abyss_rank` write fields. |
| Kinah requirement | Confirms current Java `decreaseKinah(-necessaryKinah)` no-op or reveals a runtime behavior different from source review. |

## Comparison Rules

- Compare packet order after normalizing generated target object ids.
- Compare packet payload bytes when available; otherwise compare packet class and semantic fields and mark parity as Needs Verification.
- Compare DB row sets after normalizing generated target object ids.
- Treat Java category-level commits in `InventoryDAO`/`ItemStoneListDAO` as a known transaction-boundary difference from the C# one-transaction repository write unless runtime failure testing proves partial commits are behaviorally required.
- Do not mark `CM_ITEM_PURIFICATION`, `ItemPurificationService`, `Storage`, `InventoryDAO`, `ItemStoneListDAO`, or `AbyssRankDAO` as Verified Parity until the artifacts exist and are compared.

## Current Blockers

- Local Java runtime capture is blocked by Java 8 and missing Maven.
- The C# automatic dispatch path remains plan-only by policy.
- Quest callbacks from `Storage.decreaseItemCount`, `Storage.delete`, and `Storage.add` are not represented in the C# ItemPurification path.
- AP side effects beyond the currently modeled rank packets remain incomplete.
- Socket send failure and persistence failure ordering are not resolved for production dispatch.
