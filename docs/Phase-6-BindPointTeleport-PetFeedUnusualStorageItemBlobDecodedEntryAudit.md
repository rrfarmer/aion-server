# Phase 6 - Pet Feed Unusual Storage Item Blob Decoded Entry Audit

Date: May 27, 2026
Unit of Work: UOW-1355

## Scope

This unit performs a read-only parity audit for future unusual-storage `SM_WAREHOUSE_ADD_ITEM` artifact decoded item-blob fields. Java remains the source of truth.

Java source audited:

- `com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob`
- `com.aionemu.gameserver.network.aion.iteminfo.GeneralInfoBlobEntry`
- `com.aionemu.gameserver.network.aion.iteminfo.CompositeItemBlobEntry`
- `com.aionemu.gameserver.network.aion.iteminfo.EnchantInfoBlobEntry`
- `com.aionemu.gameserver.network.aion.iteminfo.ConditioningInfoBlobEntry`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_WAREHOUSE_ADD_ITEM`

C# source/tests audited:

- `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo`
- `Aion.GameServer.Network.Aion.ServerPackets.SmWarehouseAddItem`
- `Aion.GameServer.Tests.PetFeedUnusualStorageJavaVectorArtifactReaderTests`

No Java or C# source behavior was changed.

## Java Blob Entry Order

`SM_WAREHOUSE_ADD_ITEM.writeItemInfo` writes object id, item id, an unknown zero byte, localized item name, `ItemInfoBlob.getFullBlob(player, item)`, then the low 16 bits of the equipment slot. The item blob is therefore an encode-time view of the live `Item` and template state, not only construction-time packet metadata.

`ItemInfoBlob.getFullBlob` writes the total blob payload size as `H`, then writes each entry as `C entryId` followed by that entry payload.

Observed Java full-blob order:

| Condition | Entry |
|---|---|
| Fusioned item or two-hand weapon | `COMPOSITE_ITEM` (`0x0E`) |
| Valid equipment slots | `EQUIPPED_SLOT` (`0x06`) |
| Wing | `SLOTS_WING` (`0x0D`) |
| Shield | `SLOTS_SHIELD` (`0x03`) |
| Plume | `PLUME_INFO` (`0x13`) |
| Armor accessory | `SLOTS_ACCESSORY` (`0x04`) |
| Armor non-accessory | `SLOTS_ARMOR` (`0x02`) |
| Weapon | `SLOTS_WEAPON` (`0x01`) |
| Valid equipment slots | `ENCHANT_INFO` (`0x0B`) |
| `item.getConditioningInfo() != null` | `CONDITIONING_INFO` (`0x0F`) |
| Can polish | `POLISH_INFO` (`0x11`) |
| Valid equipment slots | `PREMIUM_OPTION` (`0x10`) |
| Bonus template modifier without conditions | one `STAT_BONUSES` (`0x0A`) per modifier |
| Stigma shard group | `STIGMA_SHARD` (`0x08`) |
| Always | `GENERAL_INFO` (`0x00`) |
| `item.getPackCount() != 0` | `WRAP_INFO` (`0x12`) |

Java declares but does not add `SLOTS_ARROW` (`0x05`) or `STIGMA_INFO` (`0x07`) from `getFullBlob`; `0x09` and `0x0C` are also not used by this full-blob path.

## Future Artifact Fields Needed

Future schema-v1 Java artifacts should capture enough decoded item-blob metadata to explain why warehouse-add bytes match or differ:

- Blob shape: total blob size, ordered entry ids, entry names, and payload sizes.
- Template-derived inputs: item group, valid equipment slots, armor type/accessory status, weapon/armor/wing/shield/plume flags, two-hand flag, can-polish flag, stigma-shard flag, item mask, localized name, and unconditional bonus modifiers.
- Dynamic item inputs: object id, item id, count, creator, equipment slot, storage id/ordinal, fusioned item id, fusion stones by slot, fusion optional sockets, fusion bonus stats id, soulbound flag, enchant level, optional sockets, enchant bonus, mana stones by slot, godstone id, item skin template id, idian stone id/polish number/polish charge, tempering level, random plume bonus value, amplified flag, buff skill, conditioning info presence/charge points, pack count, temporary exchange remaining seconds, account/legion warehouse cleanup restriction flag, expiration remaining seconds, dye color, and dye remaining seconds.
- Time normalization: capture timestamp and explicitly recorded remaining-second fields for item expiration, dye expiration, and temporary exchange windows.

## C# Known-Gap Alignment

The existing C# `SmInventoryInfo.WriteItemInfoBlob` follows the same broad entry order, but the unusual-storage reader correctly keeps warehouse-add byte comparison guarded because the following Java behaviors are not yet fully represented:

- `STAT_BONUSES` entries from unconditional template modifiers.
- `CompositeItemBlobEntry` writes `ownerItem.getFusionedItemBonusStatsId()`, while C# currently writes `0`.
- `GeneralInfoBlobEntry` writes temporary exchange remaining seconds and a cleanup/seal restriction flag, while C# currently writes zeroes.
- `EnchantInfoBlobEntry` writes plume tempering stat ids/values when a tempered plume is encoded; C# currently writes zeroes for that stat block.
- Java includes `CONDITIONING_INFO` only when runtime `item.getConditioningInfo() != null`; C# uses a template/charge heuristic.
- Expiration and dye values are wall-clock-derived remaining seconds.

These gaps are already named by `PetFeedUnusualStorageJavaVectorArtifactReaderTests.KnownBlobSerializerGap` and should stay explicit until Java artifacts and deterministic comparisons prove otherwise.

## Boundaries Preserved

- No artifact writer was added.
- No observer was enabled or installed.
- No packet bytes are copied or retained by this unit.
- No item-blob decoder implementation was added.
- No C# serializer behavior changed.
- No parity was promoted to verified.

## Validation

- Ran `git diff --check` for the docs-only change.
- Java compile validation was not required for this docs-only audit and remains blocked locally by missing Maven tooling.
- No Java runtime artifacts were generated.
- No .NET tests were required because no C# behavior changed.

## Migration Parity Table - UOW-1355

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteItemInfoBlob` | Serialization Helper | Partial | Manual Only | Needs Verification | Read-only audit mapped Java full-blob entry order and entry ids. No runtime artifacts, byte comparison, or C# serializer change in this unit. |
| `com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob.ItemBlobType` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo` blob entry ids | Enum / Blob Entry Ids | Partial | Manual Only | Needs Verification | Entry ids `0x00` through `0x13` were mapped. `SLOTS_ARROW` and `STIGMA_INFO` are declared but not added by Java `getFullBlob`; `0x09` and `0x0C` remain unused on this path. |
| `com.aionemu.gameserver.network.aion.iteminfo.GeneralInfoBlobEntry` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteGeneralInfoBlob` | Serialization Entry | Partial | Manual Only | Partial Parity | Java writes temporary exchange remaining seconds and cleanup/seal restriction flag. C# currently writes zeroes; expiration is time-dependent. |
| `com.aionemu.gameserver.network.aion.iteminfo.CompositeItemBlobEntry` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteCompositeItemBlob` | Serialization Entry | Partial | Manual Only | Partial Parity | Java writes fusioned item id, fusion stones by slot, optional sockets, and fusion bonus stats id. C# currently writes zero for fusion bonus stats id. |
| `com.aionemu.gameserver.network.aion.iteminfo.EnchantInfoBlobEntry` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteEnchantInfo` | Serialization Entry | Partial | Manual Only | Partial Parity | Java writes dye remaining seconds and tempered plume stat ids/values. C# has known gaps for plume tempering stats and wall-clock fields. |
| `com.aionemu.gameserver.network.aion.iteminfo.ConditioningInfoBlobEntry` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteConditioningInfoBlob` | Serialization Entry | Partial | Manual Only | Needs Verification | Java includes this entry only when runtime conditioning info exists; C# currently uses template max level or charge as a heuristic. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_WAREHOUSE_ADD_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmWarehouseAddItem` | Packet | Partial | Unit Tested | Needs Verification | Future unusual-storage artifacts must record encode-time item/template/blob facts. Current C# reader compares cube-update bytes but keeps warehouse-add byte comparison guarded by item-blob serializer gaps. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None | Read-only Audit | Java item-info packet source review | Documents required decoded item-blob fields and known C# serializer gaps before artifact output. | Source audit only. | No Java runtime artifact, item-blob decoder, warehouse-add byte comparison, or objective Java/C# byte parity validation. |

## Remaining Risks

- Java compile/runtime validation remains blocked locally by missing Maven/Java 25 tooling.
- The future Java artifact writer could under-capture encode-time item state unless it records both template-derived and dynamic item fields listed above.
- `STAT_BONUSES` may introduce multiple entries whose order follows Java template modifier order; this remains unverified against runtime data.
- Date/time fields need deterministic normalization before byte comparison can be stable.
- Runtime conditioning presence cannot be inferred safely from C# template metadata alone.
- Warehouse-add byte comparison must remain guarded until decoded Java artifacts and C# serializer gap fixes exist.

## Summary Metrics

- Total Java artifacts discovered: 7 grouped artifact rows in this unit
- Total artifacts ported: 0 source artifacts; 1 audit document
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 7 grouped rows
- Total blocked artifacts: Java runtime artifact generation, item-blob decoder, warehouse-add byte comparison, C# serializer gap closure, live unusual-storage adapter
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a disabled no-output decoded item-blob metadata shape to `PetFeedUnusualStorageArtifactCapture` snapshots. It should carry ordered entry ids/names and placeholders for template-derived/dynamic inputs, but still avoid observer installation, file output, raw byte retention, and any C# warehouse-add byte comparison.
