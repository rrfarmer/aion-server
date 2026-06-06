# Phase 6 Session 2689 Completion

## UOW

[Phase 6] UOW-2689: Move remaining stack after partial auto-merge.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: live CM_MOVE_ITEM now covers the Java path where slot == -1 partially merges into an existing destination stack, then moves the remaining source stack normally.
- Java source/runtime path: ItemMoveService.moveItem slot == -1 merge loop, then normal cross-storage remove/add fallback; ItemPacketService.sendStorageUpdatePacket uses ItemAddType.ITEM_COLLECT.
- C# runtime artifact wired: GameServerConnection.HandleMoveItemAsync.
- Client-visible/state/persistence effect: destination stack increases, source stack decreases, remaining source item persists/moves to destination storage, and the destination add packet uses ITEM_COLLECT instead of ALL_SLOT.
- Why this is runtime progress: this changes live inventory state, persistence calls, and server packets from a client packet handler; it is not preview-only/test-only/documentation-only.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/item/ItemMoveService.java`
  - After `slot == -1` stackable merge attempts, Java only returns when source count is zero.
  - If source count remains, Java continues into `targetStorage.isFull`, source remove/delete packet, `item.setEquipmentSlot(slot)`, and target add.
- `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
  - `sendStorageUpdatePacket(player, storageType, item)` defaults to `ItemAddType.ITEM_COLLECT`.
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_WAREHOUSE_ADD_ITEM.java`
  - Writes the supplied add-type mask into the warehouse add packet.

## C# Changes

- Changed the warehouse destination add in `HandleMoveItemAsync` from `SmWarehouseAddItem.AllSlot` to `SmInventoryAddItem.ItemCollect`, matching Java's normal storage update packet for moved items.
- Added a focused regression covering partial stack auto-merge followed by normal move of the remaining source stack.
- Added a local warehouse-add packet decoder helper to assert warehouse type, add mask, item count, and object id.

## Tests Added / Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleMoveItemAsync_PartialAutoSlotMergeMovesRemainingStackLikeJava` | Unit / live connection handler | `ItemMoveService.moveItem` partial merge fallback and `ItemPacketService.sendStorageUpdatePacket` | A cube stack of 5 moved to a warehouse stack with 3 free slots merges 3, leaves 2 on the source item, persists merge plus cross-storage move, updates source/destination counts, and sends destination increase/source decrease/delete/add packets with warehouse add type `ITEM_COLLECT`. | Socket-backed connection fixture invoking live private handler through reflection, runtime-loaded storable stackable template, repository mutation counters, inventory assertions, and packet byte decoding. | Final warehouse cube-size packet contents remain only type-checked because C# warehouse item count state is not fully modeled through `player.WarehouseItems` in this handler. |

## Validation Decision

```text
- Changed surface: live CM_MOVE_ITEM normal move warehouse add mask plus focused live handler regression.
- Specific behavior/contract: Java partially merges stackable auto-slot moves into destination stacks, then moves the remaining stack and adds it to warehouse with ItemAddType.ITEM_COLLECT.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleMoveItemAsync_PartialAutoSlotMergeMovesRemainingStackLikeJava|FullyQualifiedName~HandleMoveItemAsync_StackableAutoSlotMergesDestinationStackLikeJava|FullyQualifiedName~CmMoveItemTests" --logger "console;verbosity=minimal"
- Focused Java/Maven command: not run; Java source was reviewed unchanged, and no narrow Java fixture exists for this branch in this checkout.
- Broad-validation trigger: none. The change is isolated to one live handler packet mask and directly related move-handler/parser tests.
- Broad .NET decision: skipped; the filtered command built the affected projects and proved the scoped state, persistence-call, parser, and packet-mask behavior.
- Why this scope is sufficient: the regression exercises the branch where old C# would still complete the move but emit the wrong warehouse add type and lacked coverage for merge-then-move state.
```

Result:

- Focused C# validation passed: 6/6.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `ItemMoveService.moveItem` | `GameServerConnection.HandleMoveItemAsync` | Service / live inventory mutation | Partial | Unit Tested | Partial Parity | Full-source and partial-source stackable auto-slot merge paths are now covered. Full move-item parity remains incomplete. |
| `ItemPacketService.sendStorageUpdatePacket` | `GameServerConnection.HandleMoveItemAsync` warehouse add branch | Packet service / live server packet | Partial | Unit Tested indirectly | Partial Parity | Normal cross-storage warehouse add now uses `ITEM_COLLECT` for this live handler. Other call sites are not audited in this UOW. |
| `SM_WAREHOUSE_ADD_ITEM.writeImpl` | `SmWarehouseAddItem` | Server packet | Partial | Unit Tested indirectly | Needs Verification | Test decodes the add mask in live handler output; full packet golden parity is not claimed. |

## Known Gaps

- Complete `CM_MOVE_ITEM` parity is not claimed.
- Target-storage full behavior remains unmodeled in the C# handler.
- Final warehouse cube-size packet contents are not fully verified for regular/account warehouse because this handler stores all items in `player.InventoryItems` while `SmCubeUpdate.RegularWarehouseSize` uses `player.WarehouseItems`.
- Legion warehouse history/permissions and shutdown unlock/message behavior remain deferred.
- Source restriction checks remain narrower than Java's `ItemRestrictionService.isItemRestrictedFrom`.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 3
- Total artifacts ported or extended in this UOW: 3
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 3
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 42%

## Next Runtime UOW Candidates

1. Inspect Java target-storage full handling for `CM_MOVE_ITEM` and C# storage capacity state; implement only if live capacity state is safely available.
2. Inspect `CM_SPLIT_ITEM` full-source merge/delete behavior against Java `ItemSplitService.mergeStacks` now that non-Kinah zero-count merge persistence deletes source rows.
3. Inspect `CM_REPLACE_ITEM` Java delete/add ordering and C# live packet sequence for a small confirmed mismatch.
