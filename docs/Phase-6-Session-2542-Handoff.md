# Phase 6 Session 2542 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2542: Fix CM_SPLIT_ITEM merge to use correct update types for cross-storage

## Full Session Summary (UOWs 2531–2542)

This session completed 12 UOWs focused on the item/inventory system and minor activation improvements:

| UOW | Summary |
|-----|---------|
| 2531 | `SmViewPlayerDetails` (opcode 65); `HandleViewPlayerDetailsAsync` fully live |
| 2532 | CM_MOVE_ITEM same-storage slot reorder; `InventoryItem.Slot { get; set; }` |
| 2533 | CM_UNWRAP_ITEM; `SmUnwrapItem` (opcode 289); `InventoryItem.PackCount { get; set; }` |
| 2534 | CM_SPLIT_ITEM split-to-empty-slot; `InventoryItem.Count { get; set; }`; `SaveItemSplitMutationAsync` |
| 2535 | CM_SPLIT_ITEM merge-stacks (same-storage); `SaveItemMergeMutationAsync` |
| 2536 | CM_MOVE_ITEM cross-storage (cube↔warehouse); `InventoryItem.Location { get; set; }`; `SmDeleteItem.MoveDeleteType` |
| 2537 | CM_SPLIT_ITEM cross-storage splits; item restriction checks |
| 2538 | `SmWarehouseUpdateItem` (opcode 171); warehouse-source split packet |
| 2539 | CM_CHECK_PAK audit log; CM_SHOW_MAP unknown-action warning |
| 2540 | IsTrading guards for CM_MOVE_ITEM (cross-storage) and CM_SPLIT_ITEM |
| 2541 | Kinah split path (cube↔account warehouse); `INC_KINAH_MERGE = 0x05`; `DEC_ITEM_SPLIT_MOVE = 0x0A` constants |
| 2542 | Fix CM_SPLIT_ITEM merge packets for cross-storage (INC_ITEM_COLLECT, DEC_ITEM_SPLIT_MOVE) |

## Comprehensive Item Operation Parity Matrix

| Operation | Same-cube | Same-WH | Cube→WH | WH→Cube | Legion WH | Notes |
|-----------|-----------|---------|---------|---------|-----------|-------|
| CM_MOVE_ITEM slot reorder | ✓ | ✓ | ✓ | ✓ | Deferred | IsTrading guard on cross-storage |
| CM_SPLIT_ITEM split | ✓ | ✓ | ✓ | ✓ | Deferred | IsTrading guard; kinah path live |
| CM_SPLIT_ITEM merge | ✓ | ✓ | ✓ | ✓ | Deferred | Correct INC/DEC types per storage |
| CM_SPLIT_ITEM kinah | ✓ | — | ✓ (cube↔acct) | ✓ | Deferred | INC_KINAH_MERGE + DEC_ITEM_SPLIT |
| CM_DELETE_ITEM | ✓ | — | — | — | — | |
| CM_UNWRAP_ITEM | ✓ | — | — | — | — | |
| CM_VIEW_PLAYER_DETAILS | ✓ (response) | — | — | — | — | Equipment send |

## InventoryItem Mutable Properties

| Property | Changed | Java parity |
|----------|---------|-------------|
| `Slot` | UOW-2532 | `Item.setEquipmentSlot()` |
| `PackCount` | UOW-2533 | `Item.setPackCount()` |
| `Count` | UOW-2534 | `Item.decreaseItemCount()` / `increaseItemCount()` |
| `Location` | UOW-2536 | `Item.setItemLocation()` |

## SmInventoryUpdateItem Constants (Full Set)

| Constant | Value | Java source |
|----------|-------|-------------|
| `IncreaseItemMerge` | 0x01 | INC_ITEM_MERGE |
| `IncreaseKinahMerge` | 0x05 | INC_KINAH_MERGE |
| `DecreaseItemSplit` | 0x06 | DEC_ITEM_SPLIT |
| `DecreaseItemSplitMove` | 0x0A | DEC_ITEM_SPLIT_MOVE |
| `IncreaseItemCollect` | 0x19 | INC_ITEM_COLLECT |
| `IncreaseKinahCollect` | 0x1A | INC_KINAH_COLLECT |
| `DecreaseItemUse` | 0x16 | DEC_ITEM_USE |
| `DecreaseStigmaUse` | 0x17 | DEC_STIGMA_USE |
| `DecreaseKinahBuy` | 0x1D | DEC_KINAH_BUY |
| `DecreaseKinahLearn` | 0x49 | DEC_KINAH_LEARN |
| `DecreaseKinahFly` | 0x4B | DEC_KINAH_FLY |
| `DecreaseKinahCube` | 0x5A | DEC_KINAH_CUBE |

## New Repository Methods

| Method | UOW | SQL |
|--------|-----|-----|
| `SaveInventoryItemSlotAsync` | 2532 | UPDATE inventory SET slot = ? |
| `SaveInventoryItemPackCountAsync` | 2533 | UPDATE inventory SET pack_count = ? |
| `SaveItemSplitMutationAsync` | 2534 | UPDATE count + INSERT new item |
| `SaveItemMergeMutationAsync` | 2535 | UPDATE source count + UPDATE target count |
| `SaveItemCrossStorageMoveMutationAsync` | 2536 | UPDATE item_location + slot |

## New Server Packets

| Packet | Opcode | UOW |
|--------|--------|-----|
| `SmViewPlayerDetails` | 65 | 2531 |
| `SmUnwrapItem` | 289 | 2533 |
| `SmWarehouseUpdateItem` | 171 | 2538 |

## Validation Decision for This Session

- Changed surface: production-code (item handlers, repository methods, server packets)
- Focused C# commands: multiple `--filter` runs per UOW; all passed
- Broad-validation trigger: none
- Broad-validation trigger invoked: no
- Final regression: 32 tests passed (filter covering all new test classes + NpcDialogSideEffect)

## Migration Parity Table

| Java Artifact | C# Artifact | Port Status | Parity Status |
|---|---|---|---|
| `ItemSplitService.splitItem` (full) | `HandleSplitItemAsync` | Partial | Partial Parity |
| `ItemSplitService.moveKinah` | `HandleKinahMoveAsync` | Complete (cube↔acct) | Partial Parity |
| `ItemSplitService.mergeStacks` | Inline in `HandleSplitItemAsync` | Partial (same+cross, non-legion) | Partial Parity |
| `ItemMoveService.moveItem` | `HandleMoveItemAsync` | Partial (non-legion) | Partial Parity |
| `SM_UNWRAP_ITEM` | `SmUnwrapItem` | Complete | Verified Parity |
| `SM_VIEW_PLAYER_DETAILS` | `SmViewPlayerDetails` | Complete | Verified Parity |
| `SM_WAREHOUSE_UPDATE_ITEM` | `SmWarehouseUpdateItem` | Complete | Verified Parity |
| `ItemPacketService.ItemUpdateType` constants | `SmInventoryUpdateItem` constants | Partial | Partial Parity |

## Next Recommended UOW

**UOW-2543: Assess remaining item-adjacent deferred handlers**

The item system is now comprehensive. Consider porting:

1. **Player title system completion** — `CM_TITLE_SELECT` sets active title; check if it's live.
2. **Player note persistence** — `CM_SET_NOTE` is live but does it persist to DB? Check the handler.
3. **Check any remaining item operations** — Review `HandleSplitItemAsync` for any remaining paths.

Or shift focus to a new system:
4. **Social interactions** — Check if any friend/block operations need completion.
5. **Player stats display** — Look at `SM_STATS_INFO` or similar packets sent to other players.

Focused validation recipe for UOW-2543:
- If player note persistence: `--filter "FullyQualifiedName~SetNoteTests"`
- Broad-validation trigger: none

## Remaining Risks (Item System)

- Legion warehouse operations (CM_MOVE_ITEM, CM_SPLIT_ITEM) all deferred.
- Trading check deferred for CM_MOVE_ITEM same-storage (Java doesn't check there either — parity OK).
- `SmViewPlayerDetails` sends to any world player; Java restricts to known-list (parity gap).
- `IPlayerEnterWorldRepository` test mocks must implement all 5 new slot/count/location methods.
- Kinah split: only cube↔account warehouse; regular warehouse kinah not handled.

## Context Needed By Next Session

- `InventoryItem.Count`, `.Slot`, `.PackCount`, `.Location` are all mutable setters.
- `HandleSplitItemAsync`:
  - Legion WH (storageType==3) → deferred
  - IsTrading check → sends InventorySplitDuringTrade system message
  - Kinah (itemId==182400001) → `HandleKinahMoveAsync`
  - targetItem==null, same-storage → split with DEC_ITEM_SPLIT
  - targetItem==null, cross-storage → split with DEC_ITEM_SPLIT_MOVE
  - targetItem!=null, same-item, same-storage → merge with INC_ITEM_MERGE + DEC_ITEM_SPLIT
  - targetItem!=null, same-item, cross-storage → merge with INC_ITEM_COLLECT + DEC_ITEM_SPLIT_MOVE
- `HandleMoveItemAsync`:
  - same-storage → slot update, no packet
  - cross-storage, IsTrading → item unlock packet
  - cross-storage, non-legion → restriction check, location+slot update, delete+add packets
  - cross-storage, legion → deferred
- All `IPlayerEnterWorldRepository` mocks in `PlayerEnterWorldServiceTests.cs` must implement: `SaveInventoryItemSlotAsync`, `SaveInventoryItemPackCountAsync`, `SaveItemSplitMutationAsync`, `SaveItemMergeMutationAsync`, `SaveItemCrossStorageMoveMutationAsync`.
