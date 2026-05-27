# Phase 6 Bind-Point Teleport - Cleanup/Seal Static-Data Ownership Audit

Date: 2026-05-27
Unit of Work: UOW-1392
Status: Read-only cleanup/seal static-data ownership audit complete. UOW-1393 implemented the C# static-data projection; UOW-1394 through UOW-1398 added serializer, warehouse-add, pet-feed metadata, inventory add/update, and mail attached-item wrapper plumbing for the covered paths. UOW-1399 wired the live read-mail caller, UOW-1400 wired the live broker-buy caller, UOW-1401 wired the live broker cancel-return caller, and UOW-1402 wired the live broker settlement returned-item caller to compute/pass the flag from `StaticData.ItemRestrictionCleanups`.

## Scope

This unit locates Java's `item_restriction_cleanups` load path and identifies the C# ownership boundary needed before `GeneralInfoBlobEntry` cleanup/seal flags can be serialized with parity.

## Java Source Of Truth

- `game-server/data/static_data/static_data.xml` imports `items/item_restriction_cleanups.xml`.
- `game-server/data/static_data/static_data.xsd` includes `items/item_restriction_cleanups.xsd` and allows the `item_restriction_cleanups` top-level element.
- `StaticData.itemCleanup` is bound from `<item_restriction_cleanups>`.
- `DataManager.ITEM_CLEAN_UP` is assigned from `StaticData.itemCleanup`.
- `ItemRestrictionCleanupData` owns a list of `<cleanup>` entries and exposes `hasAccountOrLegionWhStorabilityDisabled(itemId)`.
- `ItemCleanupTemplate` carries `id`, `trade`, `sell`, `wh`, `awh`, and `lwh` byte attributes, defaulting each optional attribute to `-1`.
- `GeneralInfoBlobEntry.writeThisBlob` writes `3` when `DataManager.ITEM_CLEAN_UP.hasAccountOrLegionWhStorabilityDisabled(ownerItem.getItemId())` is true, otherwise `0`.
- The current Java data has one cleanup row: `id="188053996" awh="0" lwh="0"`.

Java predicate detail:

```text
exists cleanup where cleanup.id == itemId && (cleanup.awh == 0 || cleanup.lwh == 0)
```

`trade`, `sell`, and `wh` do not affect this general-info packet field.

## Current C# State

- `StaticData.LoadFromCacheAsync` walks the imported static-data XML graph and already records imported file paths and top-level element counts.
- There is no visible C# `ItemRestrictionCleanupTable`, `ItemCleanupSummary`, or `StaticData.ItemCleanup` equivalent.
- `SmInventoryInfo.WriteGeneralInfoBlob` writes the cleanup/seal `H` field as zero.
- `SmInventoryInfo.WriteItemInfoBlob` currently receives only `InventoryItem` and `ItemTemplateSummary`.
- The same item-blob helper is shared by inventory, inventory add/update, warehouse add/info, and mail attachment packets.

## Recommended Ownership

Add a small C# dataholder rather than deriving this flag from `ItemTemplateSummary.Mask`.

- `ItemRestrictionCleanupTable`: load `<cleanup>` rows from imported static-data files and expose `HasAccountOrLegionWarehouseStorabilityDisabled(int itemId)`.
- `ItemRestrictionCleanupSummary`: keep `ItemId`, `Trade`, `Sell`, `Warehouse`, `AccountWarehouse`, and `LegionWarehouse`.
- `StaticData.ItemRestrictionCleanups`: populate it in `LoadFromCacheAsync` beside existing item-related tables.
- Packet boundary: pass an explicit `generalInfoWarehouseRestrictionFlag` or bool into `WriteItemInfoBlob` / `WriteGeneralInfoBlob`; keep packet serialization deterministic and avoid global static-data reads inside encoding.

## Implementation Follow-Up

UOW-1393 added `ItemRestrictionCleanupTable`, `ItemRestrictionCleanupSummary`, `StaticData.ItemRestrictionCleanups`, loader parsing, and focused tests for Java defaults plus the `awh == 0 || lwh == 0` predicate. UOW-1394 then added an explicit cleanup/seal flag input to the item-blob serializer and wired enter-world inventory/warehouse login packet construction through the cleanup table. UOW-1395 added explicit `SmWarehouseAddItem` flag input and packet coverage. UOW-1396 passed precomputed cleanup/seal flag context through pet-feed warehouse-add metadata paths. UOW-1397 added explicit `SmInventoryAddItem` and `SmInventoryUpdateItem` wrapper flag input plus focused packet coverage. UOW-1398 added explicit `SmMailService` read-letter attached-item flag input plus focused packet coverage. UOW-1399 added a live `GameServerConnection` read-mail flag source. UOW-1400 added live broker-buy inventory-add flag sourcing. UOW-1401 added live broker cancel-return inventory-add flag sourcing. UOW-1402 added live broker settlement returned-item inventory-add flag sourcing. Remaining runtime callers still need flag source plumbing.

## Suggested Implementation Sequence

1. Add `ItemRestrictionCleanupTable` and parser coverage for the current Java XML fixture.
2. Add `StaticData.ItemRestrictionCleanups` and wire the loader.
3. Add focused tests proving missing `awh`/`lwh` default to `-1`, either `awh="0"` or `lwh="0"` triggers the flag, and `trade`/`sell`/`wh` alone do not trigger the general-info flag.
4. Extend remaining packet call sites to pass the explicit flag when a dataholder or precomputed context is available.
5. Continue wiring runtime callers to compute/pass the explicit flag from `StaticData.ItemRestrictionCleanups` where that dependency is available, starting with the next smallest full-blob inventory add/update caller.

## Migration Parity Table - UOW-1392

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.dataholders.StaticData.itemCleanup` | future `Aion.GameServer.Dataholders.StaticData.ItemRestrictionCleanups` | Static Data Root | Not Started | No Tests | Needs Verification | Java JAXB binds `<item_restriction_cleanups>` from the static-data import graph. C# loader sees the import graph but does not project this element yet. |
| `com.aionemu.gameserver.dataholders.DataManager.ITEM_CLEAN_UP` | future runtime access to `StaticData.ItemRestrictionCleanups` | Static Data Service | Not Started | No Tests | Needs Verification | Java exposes the table globally. C# should prefer explicit dependency flow into packet construction rather than serializer global lookup. |
| `com.aionemu.gameserver.dataholders.ItemRestrictionCleanupData` | future `Aion.GameServer.Dataholders.ItemRestrictionCleanupTable` | Dataholder | Not Started | No Tests | Needs Verification | Predicate is item-id match and `awh == 0 || lwh == 0`. Missing list behaves empty through `getList()`, but current Java predicate directly streams `bplist`. |
| `com.aionemu.gameserver.model.templates.restriction.ItemCleanupTemplate` | future `Aion.GameServer.Dataholders.ItemRestrictionCleanupSummary` | DTO | Not Started | No Tests | Needs Verification | Optional byte attributes default to `-1`; `trade`, `sell`, and `wh` are not used for the packet cleanup/seal flag. |
| `com.aionemu.gameserver.network.aion.iteminfo.GeneralInfoBlobEntry` cleanup/seal flag | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteGeneralInfoBlob` | Serialization Entry | Partial | Manual Only | Needs Verification | C# still writes zero. Future implementation should pass an explicit flag/boolean to avoid global static-data coupling. |

## Remaining Risks

- Static-data loader changes may touch broad `StaticData` construction and should be kept tightly scoped.
- Packet call sites without a cleanup-table path may need a default-zero overload to preserve current behavior until their runtime adapters are wired.
- Java runtime packet-byte comparison remains blocked by missing generated artifacts.
- This audit does not address temporary exchange time, runtime conditioning presence, or time-normalized expiration/dye fields.

## Next Recommended Unit Of Work

Implement the C# cleanup/seal static-data projection and focused table tests, but do not yet change live packet call sites unless the flag can be supplied deterministically. After the table exists, add a narrow packet overload/test for `WriteGeneralInfoBlob` cleanup/seal flag `3`.
