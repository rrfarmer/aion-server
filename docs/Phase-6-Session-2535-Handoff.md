# Phase 6 Session 2535 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2535: Port CM_SPLIT_ITEM merge-stacks path

## Session Summary (UOWs 2531–2535)

| UOW | Summary |
|-----|---------|
| 2531 | `SmViewPlayerDetails` (opcode 65) + `HandleViewPlayerDetailsAsync` fully live — sends equipped non-stigma items to viewer |
| 2532 | CM_MOVE_ITEM same-storage slot reordering — `InventoryItem.Slot { get; set; }`; `SaveInventoryItemSlotAsync`; same-storage handler live |
| 2533 | CM_UNWRAP_ITEM — `SmUnwrapItem` (opcode 289); `InventoryItem.PackCount { get; set; }`; `SaveInventoryItemPackCountAsync`; handler live |
| 2534 | CM_SPLIT_ITEM split-to-empty-slot path — `InventoryItem.Count { get; set; }`; `SaveItemSplitMutationAsync`; DEC_ITEM_SPLIT constant (0x06); handler live |
| 2535 | CM_SPLIT_ITEM merge-stacks path — `SaveItemMergeMutationAsync`; free-count clamping logic; INC_ITEM_MERGE (0x01) + DEC_ITEM_SPLIT update packets |

## InventoryItem Mutation Model (Updated)

The following `InventoryItem` properties are now mutable (`{ get; set; }`) for Java parity:

| Property | Setter Added | Java parity |
|----------|-------------|-------------|
| `Slot` | UOW-2532 | `Item.setEquipmentSlot()` |
| `PackCount` | UOW-2533 | `Item.setPackCount()` |
| `Count` | UOW-2534 | `Item.decreaseItemCount()` / `Item.increaseItemCount()` |

## New Repository/Service Methods (UOWs 2531–2535)

| Method | UOW | Notes |
|--------|-----|-------|
| `SaveInventoryItemSlotAsync` | 2532 | UPDATE inventory SET slot = ? |
| `SaveInventoryItemPackCountAsync` | 2533 | UPDATE inventory SET pack_count = ? |
| `SaveItemSplitMutationAsync` | 2534 | UPDATE source count + INSERT new item (transaction) |
| `SaveItemMergeMutationAsync` | 2535 | UPDATE source count + UPDATE target count (transaction) |

## CM_SPLIT_ITEM Parity Status

| Path | Status |
|------|--------|
| Same-cube, empty-slot split | **Live** (UOW-2534) |
| Same-cube, merge-stacks | **Live** (UOW-2535) |
| Cross-storage | Deferred (requires ItemRestrictionService, LegionService) |
| Kinah move via split | Deferred (requires warehouse Kinah storage support) |
| Trading check | Deferred (player.isTrading() not yet ported) |

## Files Changed (UOWs 2531–2535)

- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/InventoryItem.cs`
- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmViewPlayerDetails.cs` (new)
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmUnwrapItem.cs` (new)
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmInventoryUpdateItem.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmMoveItemTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`

## Validation

Validation decision for UOW-2535:
- Changed surface: production-code (merge handler + repository)
- Specific behavior/contract: freeCount clamping to MaxStackCount, source count decrement, target count increment, INC_ITEM_MERGE packet
- Focused C# command: `--filter "FullyQualifiedName~ItemSplitService_Merge|FullyQualifiedName~SmInventoryUpdateItem_Decrease|FullyQualifiedName~InventoryItem_CountIs"`
- Focused Java/Maven command: not expected (no Java fixture for split merge path)
- Broad-validation trigger: none
- Broad .NET decision: skipped — focused filter sufficient

Results: Passed 3/3 tests.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `services/item/ItemSplitService.splitItem` | `HandleSplitItemAsync` | Service/handler | Partial | Unit Tested | Partial Parity | Same-cube split+merge live; cross-storage, kinah, trading deferred |
| `services/item/ItemSplitService.mergeStacks` | Inline in `HandleSplitItemAsync` | Service logic | Partial | Unit Tested | Partial Parity | freeCount clamping verified; cross-storage path deferred |
| `network/aion/clientpackets/CM_UNWRAP_ITEM` | `HandleUnwrapItemAsync` | Packet handler | Complete | Unit Tested | Verified Parity | PackCount negation, send SmUnwrapItem + SmInventoryUpdateItem |
| `network/aion/serverpackets/SM_UNWRAP_ITEM` | `SmUnwrapItem` (opcode 289) | Packet | Complete | Unit Tested | Verified Parity | |
| `model/gameobjects/Item.setEquipmentSlot` | `InventoryItem.Slot { get; set; }` | Model | Complete | Unit Tested | Verified Parity | |
| `model/gameobjects/Item.setPackCount` | `InventoryItem.PackCount { get; set; }` | Model | Complete | Unit Tested | Verified Parity | |
| `model/gameobjects/Item.decreaseItemCount` | `InventoryItem.Count { get; set; }` | Model | Complete | Unit Tested | Verified Parity | |

## Next Recommended UOW

**UOW-2536: Port CM_WINDSTREAM — flight state mutation and emotion/windstream packets**

`CM_WINDSTREAM` is deferred and involves flight state mutation. Let me check the Java first:
- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_WINDSTREAM.java`
- Inspect for dependencies; if flight state is self-contained, this is porteable.

Alternative safe candidates:
- Port `CM_LEGION_WH_KINAH` — kinah transfer between player and legion warehouse (requires basic kinah mutation support)
- Port further item system handlers not yet reached
- Begin porting player trade session tracking (for trading checks)
- Investigate `SM_WINDSTREAM`/`SM_EMOTION` packets if already ported (they may be)

Focused validation recipe for UOW-2536 (windstream):
- Behavior: flight state flag toggled; SmWindstream + SmEmotion sent to visible players
- C# command: `--filter "FullyQualifiedName~WindstreamTests|FullyQualifiedName~SmWindstream"` (if tests added)
- Java/Maven: not expected
- Broad-validation trigger: none

## Remaining Risks

- Cross-storage CM_SPLIT_ITEM / CM_MOVE_ITEM deferred — requires ItemRestrictionService, LegionService.
- Kinah movement between cube and account warehouse via CM_SPLIT_ITEM deferred.
- Trading check (`player.isTrading()`) not ported — split/move handlers skip this guard.
- `SmViewPlayerDetails` sends to any world player; Java restricts to known-list players.
- All `IPlayerEnterWorldRepository` test mocks in `PlayerEnterWorldServiceTests.cs` must implement new methods.

## Context Needed By Next Session

- `InventoryItem.Count`, `.Slot`, `.PackCount` are all mutable setters.
- `HandleSplitItemAsync`: same-cube split-to-empty and merge both live; cross-storage deferred.
- `HandleMoveItemAsync`: same-storage only; cross-storage deferred.
- `HandleUnwrapItemAsync`: fully live.
- `IPlayerEnterWorldRepository` has `SaveInventoryItemSlotAsync`, `SaveInventoryItemPackCountAsync`, `SaveItemSplitMutationAsync`, `SaveItemMergeMutationAsync` — all mocks must implement these.
- `SmInventoryUpdateItem` constants: `DecreaseItemSplit = 0x06`, `IncreaseItemMerge = 0x01` added.
