# Phase 6 Session 2537 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2537: Port CM_SPLIT_ITEM cross-storage splits to regular/account warehouse

## Session Summary (UOWs 2536–2537)

| UOW | Summary |
|-----|---------|
| 2536 | CM_MOVE_ITEM cross-storage cube/warehouse — `InventoryItem.Location { get; set; }`; `SaveItemCrossStorageMoveMutationAsync`; `SmDeleteItem.MoveDeleteType = 0x14`; restriction checks via `IsStorableInWarehouse`/`IsStorableInAccountWarehouse`; full packet fan-out (SmDeleteItem or SmDeleteWarehouseItem + SmCubeUpdate + SmInventoryAddItem or SmWarehouseAddItem) |
| 2537 | CM_SPLIT_ITEM cross-storage — extended split handler to non-legion cross-storage (cube→warehouse, warehouse→cube); item restriction checks at destination; correct Location on new split item; SmWarehouseAddItem for warehouse destinations |

## Current Item Operation Parity Matrix

| Operation | Same-cube | Same-warehouse | Cube→Warehouse | Warehouse→Cube | Legion WH |
|-----------|-----------|----------------|----------------|----------------|-----------|
| CM_MOVE_ITEM slot reorder | ✓ Live | ✓ Live | ✓ Live | ✓ Live | Deferred |
| CM_SPLIT_ITEM split-to-empty | ✓ Live | ✓ Live | ✓ Live | ✓ Live | Deferred |
| CM_SPLIT_ITEM merge-stacks | ✓ Live | ✓ Live | Deferred | Deferred | Deferred |
| CM_DELETE_ITEM | ✓ Live | — | — | — | — |
| CM_UNWRAP_ITEM | ✓ Live | — | — | — | — |

## New Repository/Service Methods (UOWs 2536–2537)

| Method | UOW | Notes |
|--------|-----|-------|
| `SaveItemCrossStorageMoveMutationAsync` | 2536 | UPDATE inventory SET item_location=?, slot=? |

## Files Changed (UOWs 2536–2537)

- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/InventoryItem.cs` (`Location` made settable)
- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmDeleteItem.cs` (`MoveDeleteType = 0x14`)
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`

## Validation

Validation decision for UOWs 2536–2537:
- Changed surface: production-code (cross-storage handlers + repository)
- Specific behavior/contract: item Location mutation, MOVE delete type constant, cross-storage packet fan-out
- Focused C# command: `--filter "FullyQualifiedName~SmDeleteItem_MoveDelete|FullyQualifiedName~InventoryItem_Location|FullyQualifiedName~CmMoveItemTests|FullyQualifiedName~ItemSplitService"`
- Broad-validation trigger: none
- Broad .NET decision: skipped

Results: Passed 11 tests across 2 UOWs.

## Next Recommended UOW

**UOW-2538: Port SM_WAREHOUSE_UPDATE_ITEM and complete warehouse-source split packet path**

In `HandleSplitItemAsync`, when the source is a warehouse item, we currently skip the source update packet. The Java sends `SM_WAREHOUSE_UPDATE_ITEM(player, item, storageType.getId(), DEC_ITEM_SPLIT)`.

Files to inspect:
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_WAREHOUSE_UPDATE_ITEM.java`
- Check if `SmWarehouseUpdateItem` exists in C#

Alternative safe candidates:
- Port `CM_PRIVATE_STORE_BUY` — purchasing from player shop
- Port additional abyss/social read-only packet responses
- Port player trade session setup (`CM_TRADE_REQUEST`)

Focused validation recipe for UOW-2538:
- Behavior: warehouse source split sends correct update packet; new split item correctly in warehouse
- C# command: `--filter "FullyQualifiedName~SmWarehouseUpdateItem|FullyQualifiedName~ItemSplitService"`
- Broad-validation trigger: none

## Remaining Risks

- Legion warehouse operations (CM_MOVE_ITEM, CM_SPLIT_ITEM) deferred — requires LegionService.
- Trading check (`player.isTrading()`) not ported — split/move handlers skip this guard.
- Cross-storage split warehouse-source update packet is a fallback stub — no `SM_WAREHOUSE_UPDATE_ITEM` sent.
- `SmViewPlayerDetails` sends to any world player; Java restricts to known-list players (parity gap).
- All `IPlayerEnterWorldRepository` test mocks must implement `SaveItemCrossStorageMoveMutationAsync`.

## Context Needed By Next Session

- `InventoryItem.Count`, `.Slot`, `.PackCount`, `.Location` are all mutable setters.
- `HandleMoveItemAsync`: same-storage and cross-storage (cube↔regular/account warehouse) live; legion WH deferred.
- `HandleSplitItemAsync`: same-cube split+merge live; cross-storage cube→warehouse live; warehouse-source update packet is a stub (TODO).
- `SmDeleteItem.MoveDeleteType = 0x14` added.
- `IPlayerEnterWorldRepository` has `SaveItemCrossStorageMoveMutationAsync` — all mocks must implement.
