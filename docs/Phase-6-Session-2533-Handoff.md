# Phase 6 Session 2533 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2533: Port CM_UNWRAP_ITEM handler and SM_UNWRAP_ITEM packet

## Session Summary (UOWs 2529–2533)

**Item parity / player-interaction completions:**

| UOW | Summary |
|-----|---------|
| 2529 | `InventoryItemExtensions` — runtime soulbound-aware compound checks (IsTradeable, IsStorableInAccountWarehouse, IsStorableInLegionWarehouse, IsLegionTradeable) |
| 2530 | `DeniedStatus` constants on `PlayerSettings`; wired CM_VIEW_PLAYER_DETAILS denial path (`DeniesViewDetails`, `DeniesGuildRequests`, `SmSystemMessage.RejectedWatch`) |
| 2531 | `SmViewPlayerDetails` (opcode 65) — serializes target's equipped items without stigma; `HandleViewPlayerDetailsAsync` fully live |
| 2532 | CM_MOVE_ITEM same-storage slot reordering — `InventoryItem.Slot` settable; `SaveInventoryItemSlotAsync` repository/service; `HandleMoveItemAsync` (same-storage only; cross-storage deferred) |
| 2533 | CM_UNWRAP_ITEM — `SmUnwrapItem` (opcode 289); `InventoryItem.PackCount` settable; `SaveInventoryItemPackCountAsync`; `HandleUnwrapItemAsync` fully live |

## Key Architectural Notes

- `InventoryItem.Slot` now has `{ get; set; }` — Java parity for `Item.setEquipmentSlot()`.
- `InventoryItem.PackCount` now has `{ get; set; }` — Java parity for `Item.setPackCount()`. Positive = wrapped, negative = unwrapped.
- Cross-storage CM_MOVE_ITEM (source ≠ destination) remains deferred — requires `ItemRestrictionService`, `LegionService`, stack merging.
- `SmViewPlayerDetails` reuses `SmInventoryInfo.WriteItemInfoBlob` for per-item blobs.
- `InventoryItemExtensions` in `InventoryItem.cs` provides Java-parity compound checks (template mask + instance soulbound).

## Files Changed (UOWs 2529–2533)

- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/InventoryItem.cs`
- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/PlayerSettings.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmViewPlayerDetails.cs` (new)
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmUnwrapItem.cs` (new)
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/NpcDialogSideEffectServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmMoveItemTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`

## Validation

Validation decision for UOW-2533:
- Changed surface: production-code (handler + packet + repository)
- Specific behavior/contract: PackCount negation on unwrap, SmUnwrapItem wire format, PackCount setter model parity
- Focused C# command: `--filter "FullyQualifiedName~SmUnwrapItem|FullyQualifiedName~InventoryItem_PackCount|FullyQualifiedName~CmMoveItemTests"`
- Focused Java/Maven command: not expected (no Java fixture for CM_UNWRAP_ITEM golden)
- Broad-validation trigger: none
- Broad .NET decision: skipped — focused filter sufficient
- Why this scope is sufficient: packet wire format, PackCount mutation, and slot mutation are independent of shared infrastructure

Results: Passed 6/6 tests.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `network/aion/clientpackets/CM_UNWRAP_ITEM` | `CmUnwrapItem` + `HandleUnwrapItemAsync` | Packet handler | Complete | Unit Tested | Verified Parity | Full live handler; pack count negation matches Java |
| `network/aion/serverpackets/SM_UNWRAP_ITEM` | `SmUnwrapItem` | Packet | Complete | Unit Tested | Verified Parity | Opcode 289; writeD+writeC match Java |
| `network/aion/clientpackets/CM_MOVE_ITEM` | `CmMoveItem` + `HandleMoveItemAsync` | Packet handler | Partial | Unit Tested | Partial Parity | Same-storage only; cross-storage deferred |
| `network/aion/serverpackets/SM_VIEW_PLAYER_DETAILS` | `SmViewPlayerDetails` | Packet | Complete | Unit Tested | Verified Parity | Opcode 65; reuses WriteItemInfoBlob |
| `model/gameobjects/player/DeniedStatus` | `PlayerSettings` constants | Enum/constants | Complete | Unit Tested | Verified Parity | All 6 denial status bits verified |
| `model/gameobjects/Item.setPackCount` | `InventoryItem.PackCount { get; set; }` | Model mutation | Complete | Unit Tested | Verified Parity | Positive = wrapped, negative = unwrapped |
| `model/gameobjects/Item.setEquipmentSlot` | `InventoryItem.Slot { get; set; }` | Model mutation | Complete | Unit Tested | Verified Parity | |

## Next Recommended UOW

**UOW-2534: Port CM_SPLIT_ITEM same-storage split within cube**

`CM_SPLIT_ITEM` is currently parser-only. The Java `ItemSplitService.splitItem` does:
1. Find source item in inventory by object ID
2. If stackable and count > split count:
   - Decrease source item count by `splitCount`
   - Create a new item (clone with `splitCount`, new object ID, at `slot`)
   - Send `SM_INVENTORY_UPDATE_ITEM` for the decremented source
   - Send `SM_INVENTORY_ADD_ITEM` for the new stack

Files to inspect:
- `game-server/src/com/aionemu/gameserver/services/item/ItemSplitService.java`
- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_SPLIT_ITEM.java`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmSplitItem.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmInventoryAddItem.cs` (check if exists)

Focused validation recipe:
- Behavior: source item count decremented; new item created in same cube at target slot
- C# command: `--filter "FullyQualifiedName~CmSplitItemTests|FullyQualifiedName~SmInventoryAddItem"`
- Java/Maven: not expected unless Java fixture exists
- Broad-validation trigger: none

Safe alternative candidates:
- Port CM_ABYSS_RANKING_PLAYERS — resolves race-specific ranking cache (read-only, no mutations)
- Port CM_EQUIP_ITEM — equip/unequip flow (complex but high gameplay value)
- Port cross-storage CM_MOVE_ITEM (requires ItemRestrictionService scaffold first)

## Remaining Risks

- Cross-storage CM_MOVE_ITEM deferred: requires `ItemRestrictionService`, `LegionService`, `ItemSplitService.mergeStacks`.
- `SmViewPlayerDetails` only sends equipment for the same connection's known list. Java checks `player.getKnownList().getPlayer(targetObjectId)` vs our `_world.TryGetObject`. Parity gap: Java only sends to known-list players; C# sends to any world player.
- `InventoryItem.Slot` and `PackCount` are now mutable — callers must not cache these as immutable values.

## Context Needed By Next Session

- `InventoryItem.Slot` and `PackCount` are now mutable setters (Java parity for in-place mutation).
- `HandleMoveItemAsync`: source == destination → same-storage slot update + persist. Source ≠ destination → no-op (deferred).
- `HandleUnwrapItemAsync`: if `PackCount > 0` → send SmUnwrapItem, negate PackCount, persist, send SmInventoryUpdateItem (DEC_ITEM_USE).
- `HandleViewPlayerDetailsAsync`: fully live — sends equipped non-stigma items to viewing player; defers if target denies view.
- `IPlayerEnterWorldRepository` now has `SaveInventoryItemSlotAsync` and `SaveInventoryItemPackCountAsync` — any test mocks must implement both.
