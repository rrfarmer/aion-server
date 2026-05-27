# Phase 6 Bind-Point Teleport - GeneralInfo Temporary Exchange / Seal Audit

Date: 2026-05-27
Unit of Work: UOW-1389
Status: Read-only audit complete. No serializer code changed.

## Scope

This unit audits the remaining Java `GeneralInfoBlobEntry` fields that still block byte comparison for unusual-storage `SM_WAREHOUSE_ADD_ITEM` artifacts:

- temporary-exchange remaining seconds;
- account/legion warehouse cleanup or seal flag.

Java remains the source of truth.

## Java Behavior

`GeneralInfoBlobEntry.writeThisBlob` writes:

1. `H`: `ownerItem.getItemMask()`;
2. `Q`: `ownerItem.getItemCount()`;
3. `S`: `ownerItem.getItemCreator()`;
4. `C`: zero;
5. `D`: `ownerItem.secondsUntilExpiration()`;
6. `D`: zero;
7. `D`: `ownerItem.getTemporaryExchangeTimeRemaining()`;
8. `H`: `DataManager.ITEM_CLEAN_UP.hasAccountOrLegionWhStorabilityDisabled(ownerItem.getItemId()) ? 3 : 0`;
9. `D`: zero remaining unsealing time;
10. `H`: 18.

Java `Item.getTemporaryExchangeTimeRemaining()` returns `0` when `temporaryExchangeTime == 0`; otherwise it subtracts current epoch seconds from the absolute temporary-exchange epoch.

Java `ItemRestrictionCleanupData.hasAccountOrLegionWhStorabilityDisabled(itemId)` checks the loaded `item_restriction_cleanups` entries and returns true when the matching cleanup row has `awh == 0` or `lwh == 0`.

Java `ItemData.cleanup()` also mutates item template masks from the same cleanup table, but `GeneralInfoBlobEntry` does not infer this flag from the item mask. It explicitly queries `DataManager.ITEM_CLEAN_UP`.

## Current C# State

- `InventoryItem` has `ExpireTime`, but no absolute temporary-exchange epoch field.
- C# `SmInventoryInfo.WriteGeneralInfoBlob` writes zero for temporary exchange, cleanup/seal flag, and unseal time.
- C# static-data loading has item templates and masks, but no visible `ItemRestrictionCleanupData` / cleanup-table model equivalent.
- C# armsfusion logic has a separate `temporaryExchangeItemObjectIds` guard for service validation, not a reusable item snapshot field for packet serialization.

## Future C# Ownership Recommendation

Do not add ad hoc serializer parameters to `WriteGeneralInfoBlob`.

Recommended future ownership:

1. Add an `InventoryItem.TemporaryExchangeTime` absolute epoch field, populated from DB/load/drop creation paths when those Java paths are ported.
2. Add an item cleanup/static restriction table to `StaticData` or a dedicated dataholder, preserving cleanup rows and exposing `HasAccountOrLegionWarehouseStorabilityDisabled(itemId)`.
3. Let `SmInventoryInfo.WriteGeneralInfoBlob` compute:
   - `temporaryExchangeRemaining = TemporaryExchangeTime == 0 ? 0 : TemporaryExchangeTime - current epoch seconds`;
   - cleanup/seal flag `3` through the static-data restriction table.
4. For artifact replay/byte comparison, prefer Java artifact-normalized remaining seconds over local wall-clock recomputation.

## Implementation Guardrails

- Temporary-exchange and expiration are time-dependent. Tests should use deterministic clock control or artifact-provided remaining seconds before claiming parity.
- Cleanup/seal flag must come from the cleanup table, not from the already-mutated item mask, because Java performs an explicit `DataManager.ITEM_CLEAN_UP` query.
- The serializer currently lacks a static-data dependency. Introducing one should be planned carefully because `WriteItemInfoBlob` is shared by inventory, warehouse, mail, and update packets.
- Until ownership is clear, keep warehouse-add byte comparison guarded.

## Remaining Questions

- Which C# repository/load paths should hydrate `TemporaryExchangeTime` once the database column/source is identified?
- Should cleanup data live in `StaticData` beside item templates, or in a dedicated `ItemRestrictionCleanupTable` consumed by packet construction?
- Should packet constructors carry a resolved cleanup flag snapshot to avoid adding static-data lookups inside shared packet serializers?
- How should deterministic tests inject capture/replay time without changing packet APIs too broadly?
