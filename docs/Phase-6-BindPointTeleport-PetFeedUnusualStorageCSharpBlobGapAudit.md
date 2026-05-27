# Phase 6 Bind-Point Teleport - Pet Feed Unusual Storage C# Blob Gap Audit

Date: 2026-05-27
Unit of Work: UOW-1385
Status: Read-only audit complete. No warehouse-add byte comparison is enabled yet.

## Scope

This unit compares the C# `SmInventoryInfo.WriteItemInfoBlob` helper used by `SmWarehouseAddItem` against Java `ItemInfoBlob.getFullBlob` and the key Java `ItemBlobEntry` implementations that affect unusual-storage rejected-food artifact comparison.

The Java source remains authoritative. This audit does not assume packet parity and does not change runtime behavior.

## Source Review

- Java `SM_WAREHOUSE_ADD_ITEM.writeItemInfo` writes object id, template id, a zero marker byte, localized item name, full `ItemInfoBlob`, and slot.
- C# `SmWarehouseAddItem.WriteItemInfo` mirrors that packet shell for supplied snapshots and delegates the blob to `SmInventoryInfo.WriteItemInfoBlob`.
- Java `ItemInfoBlob.getFullBlob` adds entries in source-defined order: composite item when fused or two-hand weapon, equipped slot and item-group slot entry, enchant info, conditioning, polish, premium option, template bonus stat entries, stigma shard, general info, and wrap info.
- C# `WriteItemInfoBlob` implements a broad skeleton for those major entries, including composite, equipped slot, weapon/armor/shield/accessory/wing/plume slots, enchant, conditioning, polish, premium option, stigma shard, general info, and wrap.

## Confirmed Serializer Gaps

| Area | Java Source | C# Surface | Impact |
|---|---|---|---|
| Static bonus stat entries | `ItemInfoBlob.addBonusBlobEntry` + `BonusInfoBlobEntry` writes id/value/rate flag for bonus modifiers without conditions. | Resolved in UOW-1388 for condition-free bonus modifiers with known nonzero Java stat masks. | No longer a known C# serializer gap for modeled condition-free modifiers, but generated Java runtime artifact comparison is still required before claiming verified parity. |
| Fusion random bonus id | `CompositeItemBlobEntry` writes `ownerItem.getFusionedItemBonusStatsId()`. | Resolved in UOW-1386: `WriteCompositeItemBlob` now writes `InventoryItem.FusionRandomBonus` after optional fusion socket. | No longer a known C# serializer gap, but Java runtime artifact comparison is still required before claiming verified parity. |
| Temporary exchange time | `GeneralInfoBlobEntry` writes `ownerItem.getTemporaryExchangeTimeRemaining()`. | `InventoryItem` has no current temporary-exchange remaining field; `WriteGeneralInfoBlob` writes zero. | Temporary-exchange items differ. This is a known unsupported dynamic item field in the C# serializer. |
| Cleanup/seal warehouse restriction flag | `GeneralInfoBlobEntry` calls `DataManager.ITEM_CLEAN_UP.hasAccountOrLegionWhStorabilityDisabled(...) ? 3 : 0`. | No cleanup table input reaches `WriteItemInfoBlob`; it writes zero. | Items restricted from account/legion warehouse storability differ. Static data dependency is not ported into this packet writer. |
| Plume tempering stats | `EnchantInfoBlobEntry` emits HP plus physical/magical plume stat pairs when tempering is positive and item group is `PLUME`. | `InventoryItem.RandomPlumeBonus` and `TemperingTable` exist elsewhere, but `WriteEnchantInfo` writes zero stat pairs. | Tempered plume blobs differ. This affects both dynamic item state and template tempering-name logic. |
| Runtime conditioning presence | Java adds `CONDITIONING_INFO` only when `item.getConditioningInfo() != null`. | C# adds it when `template.ConditioningMaxLevel > 0 || item.Charge > 0`. | C# can emit conditioning metadata based on template alone even if Java item state has no conditioning info. Needs runtime artifact comparison. |
| Expiration and dye remaining seconds | Java uses `ownerItem.secondsUntilExpiration()` and `item.getColorTimeLeft()` at encode time. | C# computes `expirationEpochSeconds - DateTimeOffset.Now.ToUnixTimeSeconds()`. | Wall-clock timing can differ by capture/replay time. Future comparisons must use Java normalized remaining seconds from the artifact, not local clock, for deterministic byte checks. |

## Implemented But Still Unverified

- Entry ids and high-level order for common equipment/non-equipment cases.
- Enchant level, soul-bound flag, optional socket, enchant bonus, manastones, godstone id, idian item/polish number, tempering byte, amplification flag, and buff skill fields.
- Polish charge and wrap count.
- Plume slot info entry `0x13` for the base slot/secondary slot payload.
- General item mask/count/creator/expiration fields when there is no temporary exchange or cleanup restriction.

These areas may be close to Java, but they remain `Needs Verification` until compared against generated Java artifacts or deterministic Java-derived fixtures.

## Next Comparator Requirements

Before enabling warehouse-add byte comparison, a future unit should either:

1. Add artifact-driven expected values to the C# writer path so the serializer can reproduce Java's captured remaining seconds and dynamic flags, or
2. Keep the C# byte comparison guarded and compare only decoded fields until the C# model and static-data dependencies can supply the missing Java inputs.

The safer immediate path is to add focused tests for the first two deterministic gaps:

- `STAT_BONUSES` entry generation from `ItemTemplateSummary.StatModifiers`.
- `CompositeItemBlobEntry` fusion random bonus id instead of the current zero placeholder.

The time-dependent and cleanup/seal fields should stay guarded until Java runtime artifacts exist.
