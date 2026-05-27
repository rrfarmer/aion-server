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
| Cleanup/seal warehouse restriction flag | `GeneralInfoBlobEntry` calls `DataManager.ITEM_CLEAN_UP.hasAccountOrLegionWhStorabilityDisabled(...) ? 3 : 0`. | UOW-1394 added an explicit `WriteItemInfoBlob` flag and wired enter-world inventory/warehouse login packet construction through `StaticData.ItemRestrictionCleanups`. UOW-1395 added explicit `SmWarehouseAddItem` flag input and packet coverage. UOW-1396 passed precomputed cleanup/seal flag context through pet-feed warehouse-add metadata paths. UOW-1397 added explicit inventory add/update wrapper flag input and coverage. UOW-1398 added explicit mail attached-item read packet flag input and coverage. UOW-1399 wired the live read-mail caller, UOW-1400 wired the live broker-buy inventory-add caller, UOW-1401 wired the live broker cancel-return caller, UOW-1402 wired the live broker settlement returned-item caller, UOW-1403 wired composition reward add/update callers, UOW-1404 wired house-use reward add/update callers, UOW-1405 wired assembly reward add/update callers, UOW-1406 wired XP extraction reward add/update callers, UOW-1407 wired extraction reward add/update callers, UOW-1408 wired decompose reward add/update callers, UOW-1409 wired world-loot item collection add/update callers, and UOW-1411 wired pet-feed normal-cube unlock metadata callers to compute/pass or consume the flag from `StaticData.ItemRestrictionCleanups` or precomputed context. | Login inventory/warehouse, explicit warehouse-add, pet-feed warehouse-add metadata including normal cube/warehouse/account/legion/unusual unlock metadata, explicit inventory add/update, explicit mail attached-item, live read-mail, live broker-buy, live broker-return, live broker settlement returns, live composition rewards, live house-use rewards, live assembly rewards, live XP extraction rewards, live extraction rewards, live decompose rewards, and live world-loot direct solo item collection now have focused C# coverage, but remaining live context flag sourcing and artifact comparison paths still need flag context/verification. |
| Plume tempering stats | `EnchantInfoBlobEntry` emits HP plus physical/magical plume stat pairs when tempering is positive and item group is `PLUME`. | Resolved in UOW-1391 for full item blobs where `WriteEnchantInfo` receives `ItemTemplateSummary` context. Broker direct enchant-info calls still lack template context. | No longer a known full item-blob serializer gap for inventory/warehouse paths with template context, but brokered tempered plumes and Java runtime artifact comparison still need verification. |
| Runtime conditioning presence | Java adds `CONDITIONING_INFO` only when `item.getConditioningInfo() != null`. | C# adds it when `template.ConditioningMaxLevel > 0 || item.Charge > 0`. | C# can emit conditioning metadata based on template alone even if Java item state has no conditioning info. Needs runtime artifact comparison. |
| Expiration and dye remaining seconds | Java uses `ownerItem.secondsUntilExpiration()` and `item.getColorTimeLeft()` at encode time. | C# computes `expirationEpochSeconds - DateTimeOffset.Now.ToUnixTimeSeconds()`. | Wall-clock timing can differ by capture/replay time. Future comparisons must use Java normalized remaining seconds from the artifact, not local clock, for deterministic byte checks. |

## Implemented But Still Unverified

- Entry ids and high-level order for common equipment/non-equipment cases.
- Enchant level, soul-bound flag, optional socket, enchant bonus, manastones, godstone id, idian item/polish number, tempering byte, amplification flag, and buff skill fields.
- Polish charge and wrap count.
- Plume slot info entry `0x13` for the base slot/secondary slot payload.
- Plume tempering stat pairs for full item blobs with template context.
- General item mask/count/creator/expiration fields when there is no temporary exchange or cleanup restriction.

These areas may be close to Java, but they remain `Needs Verification` until compared against generated Java artifacts or deterministic Java-derived fixtures.

## Next Comparator Requirements

Before enabling warehouse-add byte comparison, a future unit should either:

1. Add artifact-driven expected values to the C# writer path so the serializer can reproduce Java's captured remaining seconds and dynamic flags, or
2. Keep the C# byte comparison guarded and compare only decoded fields until the C# model and static-data dependencies can supply the missing Java inputs.

The safer immediate path is to add focused tests for the first two deterministic gaps:

- `STAT_BONUSES` entry generation from `ItemTemplateSummary.StatModifiers`.
- `CompositeItemBlobEntry` fusion random bonus id instead of the current zero placeholder.

The time-dependent fields and remaining cleanup/seal packet families should stay guarded until Java runtime artifacts exist.
