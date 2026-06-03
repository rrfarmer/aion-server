# Phase 6 Session 2540 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2540: Add IsTrading guards to CM_MOVE_ITEM and CM_SPLIT_ITEM

## Session Summary (UOWs 2531–2540)

This session completed 10 UOWs focused on the item/inventory system and minor handler improvements:

| UOW | Summary |
|-----|---------|
| 2531 | `SmViewPlayerDetails` (opcode 65); `HandleViewPlayerDetailsAsync` fully live |
| 2532 | CM_MOVE_ITEM same-storage slot reordering; `InventoryItem.Slot { get; set; }` |
| 2533 | CM_UNWRAP_ITEM; `SmUnwrapItem` (opcode 289); `InventoryItem.PackCount { get; set; }` |
| 2534 | CM_SPLIT_ITEM split-to-empty-slot; `InventoryItem.Count { get; set; }`; `SaveItemSplitMutationAsync` |
| 2535 | CM_SPLIT_ITEM merge-stacks; `SaveItemMergeMutationAsync`; INC_ITEM_MERGE + DEC_ITEM_SPLIT constants |
| 2536 | CM_MOVE_ITEM cross-storage (cube↔warehouse); `InventoryItem.Location { get; set; }`; `SmDeleteItem.MoveDeleteType = 0x14` |
| 2537 | CM_SPLIT_ITEM cross-storage splits (cube→warehouse); item restriction checks |
| 2538 | `SmWarehouseUpdateItem` (opcode 171); warehouse-source split packet path completed |
| 2539 | CM_CHECK_PAK audit-log activated; CM_SHOW_MAP unknown-action warning |
| 2540 | IsTrading guard added to CM_MOVE_ITEM cross-storage and CM_SPLIT_ITEM |

## Item Operation Parity Summary (After Session)

| Operation | Same-storage | Cube↔Warehouse | Legion WH | Notes |
|-----------|-------------|----------------|-----------|-------|
| CM_MOVE_ITEM slot reorder | ✓ | ✓ | Deferred | IsTrading guard for cross-storage |
| CM_SPLIT_ITEM split | ✓ | ✓ | Deferred | IsTrading guard |
| CM_SPLIT_ITEM merge | ✓ | Deferred | Deferred | |
| CM_DELETE_ITEM | ✓ | — | — | |
| CM_UNWRAP_ITEM | ✓ | — | — | |
| CM_VIEW_PLAYER_DETAILS | ✓ (response) | — | — | |

## InventoryItem Mutable Properties (All Sessions)

| Property | Changed | Java parity |
|----------|---------|-------------|
| `Slot` | UOW-2532 | `Item.setEquipmentSlot()` |
| `PackCount` | UOW-2533 | `Item.setPackCount()` |
| `Count` | UOW-2534 | `Item.decreaseItemCount()` / `increaseItemCount()` |
| `Location` | UOW-2536 | `Item.setItemLocation()` |

## New Repository Methods (This Session)

| Method | UOW |
|--------|-----|
| `SaveInventoryItemSlotAsync` | 2532 |
| `SaveInventoryItemPackCountAsync` | 2533 |
| `SaveItemSplitMutationAsync` | 2534 |
| `SaveItemMergeMutationAsync` | 2535 |
| `SaveItemCrossStorageMoveMutationAsync` | 2536 |

## New Server Packets (This Session)

| Packet | Opcode | UOW |
|--------|--------|-----|
| `SmViewPlayerDetails` | 65 | 2531 |
| `SmUnwrapItem` | 289 | 2533 |
| `SmWarehouseUpdateItem` | 171 | 2538 |

## Validation

All UOWs validated with focused `dotnet test --filter` commands. No broad-validation triggers applied. All tests pass.

## Migration Parity Table (This Session)

| Java Artifact | C# Artifact | Port Status | Parity Status |
|---|---|---|---|
| `CM_UNWRAP_ITEM.runImpl` | `HandleUnwrapItemAsync` | Complete | Verified Parity |
| `SM_UNWRAP_ITEM` | `SmUnwrapItem` | Complete | Verified Parity |
| `SM_VIEW_PLAYER_DETAILS` | `SmViewPlayerDetails` | Complete | Verified Parity |
| `ItemMoveService.moveItem` (same-storage) | `HandleMoveItemAsync` (same-storage) | Complete | Verified Parity |
| `ItemMoveService.moveItem` (cross-storage, non-legion) | `HandleMoveItemAsync` (cross-storage) | Partial | Partial Parity |
| `ItemSplitService.splitItem` (split) | `HandleSplitItemAsync` (split) | Partial | Partial Parity |
| `ItemSplitService.splitItem` (merge) | `HandleSplitItemAsync` (merge) | Partial | Partial Parity |
| `SM_WAREHOUSE_UPDATE_ITEM` | `SmWarehouseUpdateItem` | Complete | Verified Parity |
| `CM_CHECK_PAK.runImpl` | Inline in dispatch switch | Complete | Verified Parity |

## Next Recommended UOW

**UOW-2541: Port `CM_OBJECT_SEARCH` — admin/NPC object search by spawn data**

`CM_OBJECT_SEARCH` (deferred until spawn search data ported) searches `DataManager.SPAWNS_DATA`. Check if `StaticData` or `DataManager` in C# has spawn data that can serve basic NPC searches.

Alternative: Port any of the remaining simple abyss/ranking read-only packet responses.

Another option: **Port kinah split via CM_SPLIT_ITEM** — Java `ItemSplitService.splitItem` has a kinah branch (`sourceItem.getItemTemplate().isKinah()`) that moves kinah between cube and account warehouse. This is in scope now that we have cross-storage infrastructure.

Files to inspect for kinah split:
- `ItemSplitService.splitItem` — `moveKinah` private method
- `SmInventoryInfo.BuildLoginItemList` — kinah item handling

Focused validation recipe:
- Behavior: kinah item (itemId=182400001) split sends correct cube/warehouse Kinah update
- C# command: `--filter "FullyQualifiedName~ItemSplitKinah|FullyQualifiedName~CmMoveItemTests"`
- Broad-validation trigger: none

## Remaining Risks

- Legion warehouse (CM_MOVE_ITEM, CM_SPLIT_ITEM) deferred.
- Cross-storage merge-stacks deferred.
- Kinah split/move via CM_SPLIT_ITEM deferred.
- IsTrading guard: CM_MOVE_ITEM same-storage reordering does NOT have the trading check (Java parity: `moveInSameStorage` has no trading check).
- `SmViewPlayerDetails` sends to any world player vs Java known-list restriction.
- `IPlayerEnterWorldRepository` mocks must implement: `SaveInventoryItemSlotAsync`, `SaveInventoryItemPackCountAsync`, `SaveItemSplitMutationAsync`, `SaveItemMergeMutationAsync`, `SaveItemCrossStorageMoveMutationAsync`.

## Context Needed By Next Session

- `InventoryItem.Count`, `.Slot`, `.PackCount`, `.Location` are all mutable setters.
- `player.IsTrading` is checked before CM_SPLIT_ITEM and cross-storage CM_MOVE_ITEM.
- `SmWarehouseUpdateItem` (opcode 171) is now available; uses GENERAL_INFO blob only (not full blob).
- `SmInventoryInfo.WriteGeneralInfoBlobForWarehouse` is an `internal static` helper.
- CM_MOVE_ITEM: same-storage live, cross-storage (non-legion) live with restriction+trading checks.
- CM_SPLIT_ITEM: split-to-empty (same+cross-storage) live; merge-stacks (same-storage only) live; kinah path deferred.
