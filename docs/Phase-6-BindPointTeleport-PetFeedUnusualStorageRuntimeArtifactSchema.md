# Phase 6 - Pet Feed Unusual Storage Runtime Artifact Schema

Date: May 27, 2026
Unit of Work: UOW-1344

## Scope

This docs-only unit defines the Java runtime artifact schema needed before enabling live unusual-storage rejected-food unlock dispatch.

Java source of truth:

- `com.aionemu.gameserver.services.toypet.PetService.checkFeeding`
- `com.aionemu.gameserver.services.item.ItemPacketService.sendItemUnlockPacket`
- `com.aionemu.gameserver.services.item.ItemPacketService.sendStorageUpdatePacket`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_WAREHOUSE_ADD_ITEM`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE`
- `com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob`

C# consumers:

- `Aion.GameServer.Services.ToyPet.PetFeedPacketMetadataBridge`
- `Aion.GameServer.Network.Aion.ServerPackets.SmWarehouseAddItem`
- `Aion.GameServer.Network.Aion.ServerPackets.SmCubeUpdate`
- future guarded artifact reader/comparator

## Why This Schema Exists

The guarded C# bridge can now construct unusual-storage metadata for all known Java pet bag, house storage, broker, and mailbox ids.

That is still not live parity. Java queues packet objects with live references and serializes many item fields later. A runtime artifact must therefore capture both:

- construction-time routing fields, which are fixed when the packet object is queued
- encode-time item/blob fields, which Java reads when `AionConnection.writeData` serializes the packet

Without both phases, C# could compare against the wrong snapshot and miss item mutations between queue time and encode time.

## Java Timing Model

`PetService.checkFeeding`:

1. resolves the feed item from cube inventory before scheduling the delayed check
2. keeps the mutable `Item` reference
3. later calls `ItemPacketService.sendItemUnlockPacket(player, item)` on rejected food

`ItemPacketService.sendItemUnlockPacket`:

1. reads `item.getItemLocation()`
2. resolves `StorageType.getStorageTypeById`
3. sends nothing if the storage type is unknown
4. otherwise calls `sendStorageUpdatePacket(..., ItemAddType.ALL_SLOT)`

`SM_WAREHOUSE_ADD_ITEM` construction captures:

- player reference
- warehouse type integer from `StorageType.getId()`
- add type enum reference
- singleton list containing the live `Item` reference

`SM_WAREHOUSE_ADD_ITEM.writeImpl` later reads:

- `warehouseType`
- `addType.getMask()`
- item list count
- item object id
- item template id
- item template localized name
- full `ItemInfoBlob`
- item equipment slot

`SM_CUBE_UPDATE.cubeSize` construction captures:

- action `0`
- action value from `StorageType.ordinal()`
- zero count/expand fields for pet bags, house storage, broker, and mailbox

## Artifact Layout

Recommended directory:

```text
parity-artifacts/pet-feed-unusual-storage/java/
```

Recommended file name:

```text
rejected-food-unusual-storage-{storageId}-{scenario}.schema-v1.json
```

Example scenarios:

- `pet-bag-moved`
- `house-storage-moved`
- `broker-registered`
- `mailbox-attached`

## Schema V1

```json
{
  "schemaVersion": 1,
  "scenario": "pet-bag-moved",
  "javaSources": [
    "com.aionemu.gameserver.services.toypet.PetService.checkFeeding",
    "com.aionemu.gameserver.services.item.ItemPacketService.sendStorageUpdatePacket",
    "com.aionemu.gameserver.network.aion.serverpackets.SM_WAREHOUSE_ADD_ITEM",
    "com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE"
  ],
  "storage": {
    "storageId": 32,
    "storageTypeName": "PET_BAG_6",
    "storageTypeOrdinal": 4,
    "expectedReachability": "delayed-mutable-item-reference",
    "normalUiFlow": false
  },
  "timing": {
    "feedItemLookupPhase": "pre-delay-cube-inventory",
    "unlockDecisionPhase": "post-delay-rejected-food",
    "packetConstructionPhase": "sendItemUnlockPacket/sendStorageUpdatePacket",
    "packetSerializationPhase": "AionConnection.writeData/AionServerPacket.write",
    "itemReferenceIsMutable": true
  },
  "constructionSnapshot": {
    "warehouseType": 32,
    "addType": "ALL_SLOT",
    "addTypeMask": 19,
    "packetOrder": [
      "SM_WAREHOUSE_ADD_ITEM",
      "SM_CUBE_UPDATE"
    ]
  },
  "encodeSnapshot": {
    "item": {
      "objectId": 5001,
      "itemId": 188000001,
      "count": 2,
      "itemLocation": 32,
      "equipmentSlot": 4,
      "itemTemplateId": 188000001,
      "localizedName": "Odd Snack",
      "packCount": 0,
      "expireTime": 0,
      "temporaryExchangeTime": 0,
      "charge": 0,
      "enchantLevel": 0,
      "itemMask": 0,
      "color": null
    },
    "itemBlob": {
      "hex": "",
      "size": 0,
      "entryIds": [],
      "decodedEntries": [],
      "templateDerivedInputs": {
        "itemMask": 0,
        "slotGroup": null,
        "polishEligible": false,
        "conditionable": false,
        "bonusStatModifiers": []
      },
      "dynamicInputs": {
        "fusionRandomBonusStatsId": 0,
        "temporaryExchangeTime": 0,
        "cleanupSealFlag": 0,
        "accountLegionWarehouseRestrictionFlag": 0,
        "unsealTime": 0,
        "conditioningInfoPresent": false,
        "plumeTemperingStats": []
      },
      "timeNormalization": {
        "capturedAtEpochSeconds": 0,
        "expirationRemainingSeconds": 0,
        "dyeRemainingSeconds": 0
      }
    }
  },
  "packets": [
    {
      "javaClass": "SM_WAREHOUSE_ADD_ITEM",
      "opcode": 169,
      "bodyHex": "",
      "canonicalPayloadHex": "",
      "decoded": {
        "warehouseType": 32,
        "addTypeMask": 19,
        "itemCount": 1
      }
    },
    {
      "javaClass": "SM_CUBE_UPDATE",
      "opcode": 130,
      "bodyHex": "",
      "canonicalPayloadHex": "",
      "decoded": {
        "action": 0,
        "actionValue": 4,
        "itemsCount": 0,
        "npcExpands": 0,
        "questExpands": 0,
        "itemExpands": 0
      }
    }
  ],
  "notes": []
}
```

## Required Capture Rules

- Capture packet construction after `sendStorageUpdatePacket` chooses the storage type.
- Capture serialization bytes after `writeImpl` writes clear packet body bytes and before encryption.
- Store `warehouseType` as Java `StorageType.getId()`.
- Store cube-update `actionValue` as Java `StorageType.ordinal()`.
- Store item/blob fields from encode time, not just queue time.
- Record whether the item moved during the delay.
- Record whether the scenario is ordinary UI flow or defensive/source-possible flow.
- Capture blob entry ids, order, decoded fields, and full bytes. Header-only warehouse-add artifacts are not enough.
- Capture template/static-data inputs needed to reproduce name, masks, slot group, polish eligibility, conditioning eligibility, and unconditional bonus stats.
- Normalize time-sensitive expiration and dye values as remaining seconds, or include enough clock data to make comparisons deterministic.

## Blob Coverage Findings

A read-only C# vs Java coverage audit found the packet shell alignment is good: Java `SM_WAREHOUSE_ADD_ITEM` and C# `SmWarehouseAddItem` both carry warehouse type, add mask, item count, object id, template id, `itemInfo = 0`, localized name, full item blob, and equipment slot.

The high-risk area is the shared full item blob serializer. The future artifact schema and comparator must account for these known or likely gaps before claiming runtime parity:

- Java writes `STAT_BONUSES` blob entry `0x0A` for unconditional template bonus stat modifiers; C# does not currently write that entry.
- Java writes `getFusionedItemBonusStatsId()` in composite fusion data; C# currently writes fusion random bonus/stat id as `0`.
- Java derives temporary exchange time and cleanup-seal/account-legion warehouse restriction flags from runtime item state; C# currently hardcodes these related fields to `0`.
- Java plume tempering payload can include HP plus physical/magical plume stat ids and values when tempering is positive; C# currently zeroes the plume tempering stat payload.
- Java includes conditioning based on `item.getConditioningInfo() != null`; C# inclusion is template/charge-derived.
- Expiration and dye remaining seconds are wall-clock-sensitive; artifacts must normalize or capture clock context.
- Name, mask, slot group, polish eligibility, conditionability, and slot masks are template-derived in C# and require enough static-data context in artifacts.

## C# Comparator Requirements

Future C# reader/comparator should:

- reject schema versions other than `1`
- verify packet order
- verify storage id to ordinal mapping
- compare `SM_WAREHOUSE_ADD_ITEM` body/canonical bytes when `bodyHex` is present
- compare `SM_CUBE_UPDATE` body/canonical bytes when `bodyHex` is present
- compare decoded route fields even when byte hex is absent
- compare item blob entry ids/order and decoded entry fields
- flag known serializer gaps explicitly instead of treating them as generic byte mismatches
- explicitly report missing Java bytes as `Needs Verification`

## Migration Parity Table - UOW-1344

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.toypet.PetService.checkFeeding` delayed rejected-food flow | future unusual-storage artifact generator/reader | Service Flow / Artifact Schema | Not Started | Manual Only | Needs Verification | Schema records pre-delay item lookup, post-delay rejection, and mutable item reference timing. No Java artifacts generated. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendStorageUpdatePacket` default branch | `PetFeedPacketMetadataBridge`; future artifact reader | Packet Service / Artifact Schema | Partial | Manual Only | Needs Verification | Schema captures warehouse-add then cube-update order. Existing C# bridge is source-derived and non-live. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_WAREHOUSE_ADD_ITEM` | `SmWarehouseAddItem`; future artifact reader | Packet / Artifact Schema | Partial | Manual Only | Needs Verification | Schema separates construction-time warehouse type from encode-time item/blob fields. Runtime Java bytes are missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE.cubeSize` unusual storage fallback | `SmCubeUpdate`; future artifact reader | Packet / Artifact Schema | Partial | Manual Only | Needs Verification | Schema captures Java ordinal action value and zero counts for unusual storage ids. Runtime Java bytes are missing. |
| `com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob` | `SmInventoryInfo.WriteItemInfoBlob`; future item-blob comparator | Serialization Helper / Artifact Schema | Partial | Manual Only | Needs Verification | Schema includes item blob hex, size, entry ids, decoded entries, template-derived inputs, dynamic inputs, and time normalization. Known C# risks include missing `STAT_BONUSES`, fusion random bonus id, runtime temporary exchange/seal flags, plume tempering stats, runtime conditioning presence, and wall-clock expiration/dye handling. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None | Documentation / Design | Java packet and item-blob source review | Defines schema fields and capture rules for future unusual-storage runtime vectors. | Manual source review only. | No Java generator, C# reader, byte comparison, or runtime artifacts exist. |

## Remaining Risks

- Java runtime artifact generator is not implemented.
- Java observer hook is not implemented.
- Item blob byte parity remains unverified.
- Known blob serializer gaps must be separated from artifact/fixture errors: stat bonuses, fusion random bonus id, temporary exchange/seal flags, plume tempering stats, conditioning trigger differences, and time-dependent remaining seconds.
- C# uses supplied snapshots and does not model encode-time mutation of live `Item` references.
- Live unusual-storage dispatch remains disabled.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 0 code artifacts; 1 runtime artifact schema document
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: Java runtime artifact generator, Java observer hook, item blob comparator, live unusual-storage adapter, socket dispatch
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a guarded C# schema-v1 reader/comparator for future unusual-storage Java runtime artifacts. Keep it guarded on missing artifacts and do not claim runtime parity until Java bytes exist.
