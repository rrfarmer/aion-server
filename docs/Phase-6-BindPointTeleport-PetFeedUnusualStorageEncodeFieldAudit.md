# Phase 6 - Pet Feed Unusual Storage Encode-Time Field Audit

Date: May 27, 2026
Unit of Work: UOW-1373

## Scope

This is a read-only audit of schema-v1 encode-time item/template placeholders for future unusual-storage artifacts. It does not change Java source, add JSON serialization, write files, retain raw packet bytes, mutate storage, dispatch packets, or enable capture by default.

Java remains the source of truth. Source breadcrumbs reviewed in this unit:

- `com.aionemu.gameserver.network.aion.serverpackets.SM_WAREHOUSE_ADD_ITEM`
- `com.aionemu.gameserver.model.gameobjects.Item`
- `com.aionemu.gameserver.model.templates.item.ItemTemplate`
- `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture`

Reference schema:

- `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageRuntimeArtifactSchema.md`

## Findings

`SM_WAREHOUSE_ADD_ITEM.writeItemInfo(...)` serializes from a live `Item` reference at encode time. The capture seam already exposes `SM_WAREHOUSE_ADD_ITEM.getFirstItem()` and `getFirstItemInfoBlob()`, so the future observer-side snapshot can safely read many schema fields at the same serialization boundary without retaining packet bytes or writing JSON yet.

Fields that can be populated from the live `Item` / `ItemTemplate` at observer time:

| Schema Field | Java Source | Notes |
|---|---|---|
| `encodeSnapshot.item.objectId` | `Item.getObjectId()` | Already carried in snapshot from construction context; can also be confirmed from live item. |
| `encodeSnapshot.item.itemId` | `Item.getItemId()` / `ItemTemplate.getTemplateId()` | Same value written by `SM_WAREHOUSE_ADD_ITEM.writeItemInfo(...)`. |
| `encodeSnapshot.item.count` | `Item.getItemCount()` | Current payload snapshot already captures count under general payload; schema item section can use the same value. |
| `encodeSnapshot.item.itemLocation` | `Item.getItemLocation()` | Important for detecting movement between construction and serialization. |
| `encodeSnapshot.item.equipmentSlot` | `Item.getEquipmentSlot()` | Java writes low 16 bits in `SM_WAREHOUSE_ADD_ITEM`; schema can record the full long or the encoded ushort, but must document which. |
| `encodeSnapshot.item.itemTemplateId` | `ItemTemplate.getTemplateId()` | Same as item id for the base template. |
| `encodeSnapshot.item.localizedName` | `ItemTemplate.getL10n()` | Java writes this string directly through `writeS(...)`. |
| `encodeSnapshot.item.packCount` | `Item.getPackCount()` | Current wrap payload snapshot already captures this value. |
| `encodeSnapshot.item.expireTime` | `Item.getExpireTime()` | Absolute epoch seconds; comparisons also need normalized remaining seconds for deterministic parity. |
| `encodeSnapshot.item.temporaryExchangeTime` | `Item.getTemporaryExchangeTime()` / `getTemporaryExchangeTimeRemaining()` | Schema example names this ambiguously; future DTO should decide whether to store absolute and remaining fields separately. |
| `encodeSnapshot.item.charge` | `Item.getChargePoints()` through conditioning snapshot | Current payload snapshot captures charge points. |
| `encodeSnapshot.item.enchantLevel` | `Item.getEnchantLevel()` | Current enchant payload snapshot captures this value. |
| `encodeSnapshot.item.itemMask` | `Item.getItemMask()` | Current general payload snapshot captures this value. |
| `encodeSnapshot.item.color` | `Item.getItemColor()` | Current enchant payload snapshot carries nullable dye color when dye time is non-negative. |

Fields that need additional capture metadata:

- `storage.storageTypeName`: capture currently stores id and ordinal only. Future context should store `StorageType.name()` at registration time.
- `timeNormalization.capturedAtEpochSeconds`: capture has millis timestamps; future DTO should derive epoch seconds or add explicit captured seconds.
- `encodeSnapshot.itemBlob.hex`: requires raw blob byte retention.
- Packet `bodyHex` and `canonicalPayloadHex`: require raw/canonical packet byte retention.
- Exact packet body decode for warehouse-add item count/add mask can be represented from construction metadata, but byte parity still requires retained bytes.

## Recommended Next Source Change

Add a disabled encode-time item snapshot shell from `SM_WAREHOUSE_ADD_ITEM.getFirstItem()` in `PacketSnapshot.from(...)`. This should capture item/template fields into `PacketSnapshot` or a nested `EncodeTimeItemSnapshot` without retaining raw bytes, serializing JSON, or writing files.

## Migration Parity Table - UOW-1373

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.serverpackets.SM_WAREHOUSE_ADD_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmWarehouseAddItem` | Packet | Partial | Manual Only | Needs Verification | Read-only audit confirms live `Item` and `ItemTemplate` expose most schema encode-time item fields at observer time. Byte fields still require raw/canonical retention. |
| `com.aionemu.gameserver.model.gameobjects.Item` | future C# item model / artifact schema DTO | Model | Partial | Manual Only | Needs Verification | Provides item id, count, location, equipment slot, expire time, temporary exchange times, charge/blob-related fields, enchant level, mask, and color. Null/time behavior remains unverified in C#. |
| `com.aionemu.gameserver.model.templates.item.ItemTemplate` | future C# item template model / artifact schema DTO | Model / Static Data | Partial | Manual Only | Needs Verification | Provides template id and localized name used by `SM_WAREHOUSE_ADD_ITEM.writeItemInfo(...)`. Localization/string parity still needs runtime artifact comparison. |
| `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture` | future C# artifact/schema reader | Utility / Capture Context | Partial | Manual Only | Needs Verification | Current DTO shell leaves several item fields as placeholders; audit identifies safe Java sources for a future encode-time snapshot shell. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None | Read-only audit | Java packet, item, template, and capture source review | Maps schema-v1 encode-time placeholders to Java source getters. | Source inspection only. | No source implementation, no Java compile, no runtime artifact, no C# reader validation, and no byte comparison. |

## Remaining Risks

- Java compile validation remains blocked locally by missing Maven/Java 25 tooling.
- No encode-time item snapshot shell has been implemented yet.
- Some time fields need absolute-vs-remaining semantics documented before comparison.
- Equipment slot should document full long vs encoded low-16-bit behavior.
- Raw item blob hex and packet body/canonical payload hex remain missing.
- C# artifact reader/schema validation and warehouse-add byte comparison remain missing.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped artifact rows in this unit
- Total artifacts ported: 0 source artifacts; 1 read-only encode-time field audit
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: Java compile validation, encode-time item snapshot shell, raw byte retention, JSON writer, Java runtime artifacts, C# artifact reader/schema validation, warehouse-add byte comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a disabled encode-time item snapshot shell to `PetFeedUnusualStorageArtifactCapture.PacketSnapshot.from(...)`, using `SM_WAREHOUSE_ADD_ITEM.getFirstItem()` to capture item id, count, item location, equipment slot, template id, localized name, expire time, temporary exchange absolute/remaining values, charge, enchant level, item mask, and color. Do not serialize JSON, write files, or retain raw/canonical bytes yet.
