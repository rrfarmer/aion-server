# Phase 6 Session 2688 Completion

## UOW

[Phase 6] UOW-2688: Merge stackable CM_MOVE_ITEM auto-slot targets.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: live CM_MOVE_ITEM cross-storage moves now execute Java's slot == -1 stackable auto-merge path before normal move fallback.
- Java source/runtime path: ItemMoveService.moveItem -> ItemSplitService.mergeStacks -> Storage.increaseItemCount/decreaseItemCount.
- C# runtime artifact wired: GameServerConnection.HandleMoveItemAsync plus SaveItemMergeMutationAsync persistence.
- Client-visible/state/persistence effect: moving a stackable item into storage with an existing same-item stack mutates destination/source counts, persists the merge, emits destination increase then source delete/update packets, and avoids creating a separate moved stack when the source is fully consumed.
- Why this is runtime progress: this changes live inventory state, packet emission, and database persistence from a client packet handler; it is not preview-only/test-only/documentation-only.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_MOVE_ITEM.java`
  - Reads item object id, source storage, destination storage, and signed slot, then calls `ItemMoveService.moveItem`.
- `game-server/src/com/aionemu/gameserver/services/item/ItemMoveService.java`
  - For cross-storage moves with `slot == -1`, iterates destination same-item stacks for stackable items and calls `ItemSplitService.mergeStacks`.
  - Returns immediately when the source stack count reaches zero.
- `game-server/src/com/aionemu/gameserver/services/item/ItemSplitService.java`
  - `mergeStacks` sends `INC_ITEM_COLLECT` for cross-storage destination increases and `DEC_ITEM_SPLIT_MOVE` or delete-from-update-type for source decreases.
- `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
  - Deletes non-Kinah source items when count reaches zero and sends cube-size after delete.

## C# Changes

- Added Java-equivalent stackable auto-merge handling to `GameServerConnection.HandleMoveItemAsync` before the normal cross-storage move mutation.
- Emits destination `SmInventoryUpdateItem`/`SmWarehouseUpdateItem` with `IncreaseItemCollect`.
- Emits source `SmInventoryUpdateItem`/`SmWarehouseUpdateItem` with `DecreaseItemSplitMove` when partially merged, or source delete plus cube-size when fully consumed.
- Removes fully consumed source items from live player inventory before cube-size packet construction.
- Updated `MySqlPlayerEnterWorldRepository.SaveItemMergeMutationAsync` to delete fully consumed non-Kinah source rows inside the merge transaction.
- Added merge/cross-storage move counters to `EmptyPlayerEnterWorldRepository` for focused live handler assertions.

## Tests Added / Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleMoveItemAsync_StackableAutoSlotMergesDestinationStackLikeJava` | Unit / live connection handler | `ItemMoveService.moveItem` slot `-1` branch and `ItemSplitService.mergeStacks` | A cube stack of 3 storable stackable items moved to regular warehouse with a destination stack of 97 becomes one warehouse stack of 100; merge persistence is called; normal cross-storage move persistence is not called; packets are warehouse increase, source delete, cube-size. | Socket-backed connection fixture invoking the live private handler through reflection, runtime-loaded item template, repository mutation counters, player inventory assertions, and packet byte decoding. | Does not test partial merge followed by normal move, multiple destination stacks, full-storage fallback, or live MySQL integration. |

## Validation Decision

```text
- Changed surface: live CM_MOVE_ITEM handler, item merge persistence, focused connection test helper, and repository test-double counters.
- Specific behavior/contract: Java auto-merges stackable cross-storage moves with slot == -1 into destination same-item stacks before falling back to normal cross-storage move.
- Focused C# command attempted first: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleMoveItemAsync_StackableAutoSlotMergesDestinationStackLikeJava" --logger "console;verbosity=minimal"
- First result: failed at compile due to test assertion using `.Value` after xUnit nullable assertion unwrapped the tuple; product code compiled.
- Focused C# command used after correction: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleMoveItemAsync_StackableAutoSlotMergesDestinationStackLikeJava" --logger "console;verbosity=minimal"
- Adjacent focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmMoveItemTests|FullyQualifiedName~SmWarehouseUpdateItem_SerializesObjectIdTypeAndUpdateType|FullyQualifiedName~SmDeleteItem_MoveDeleteTypeMatchesJava" --logger "console;verbosity=minimal"
- Focused Java/Maven command: not run; Java source was reviewed unchanged, and no narrow Java fixture exists for this branch in this checkout.
- Broad-validation trigger: none. The change is isolated to one live handler branch, existing merge persistence, and directly adjacent packet/parser contracts.
- Broad .NET decision: skipped; passing filtered tests built the affected projects and proved the scoped runtime behavior and adjacent packet/parser contracts.
- Why this scope is sufficient: the regression exercises the exact Java-derived branch that old C# skipped and asserts state, persistence-path selection, and packet order/type.
```

Results:

- Focused live handler validation passed: 1/1.
- Adjacent parser/packet validation passed: 6/6.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CM_MOVE_ITEM.runImpl` | `CmMoveItem` plus `GameServerConnection.HandleMoveItemAsync` | Client packet / live handler | Partial | Unit Tested | Partial Parity | Parser and stackable auto-slot full-merge branch are covered. Full move-item parity is not claimed. |
| `ItemMoveService.moveItem` | `GameServerConnection.HandleMoveItemAsync` | Service / live inventory mutation | Partial | Unit Tested | Partial Parity | Slot `-1` stackable merge branch now executes before normal cross-storage move. Legion warehouse, shutdown, full-storage, partial multi-stack scenarios, and some restriction-from-source behavior remain incomplete or unverified. |
| `ItemSplitService.mergeStacks` | `GameServerConnection.HandleMoveItemAsync` merge branch and `PlayerEnterWorldService.SaveItemMergeMutationAsync` | Service / persistence | Partial | Unit Tested indirectly | Partial Parity | Full-source auto-merge packet/state behavior is covered through live handler. Generic split merge and partial merge fallback need more coverage. |
| `Storage.decreaseItemCount` | `MySqlPlayerEnterWorldRepository.SaveItemMergeMutationAsync` | Persistence behavior | Partial | Manual source review | Needs Verification | Non-Kinah source rows with count `<= 0` are now deleted inside merge transaction. No live MySQL integration test was run in this UOW. |

## Known Gaps

- Complete `CM_MOVE_ITEM` parity is not claimed.
- Partial auto-merge followed by normal cross-storage move is not covered.
- Multiple target stacks are source-reviewed but not tested.
- Target-storage full behavior is still not modeled in this C# handler.
- Legion warehouse history/permissions and shutdown unlock/message behavior remain deferred.
- Source restriction checks remain narrower than Java's `ItemRestrictionService.isItemRestrictedFrom`.
- Real MySQL merge-delete persistence was source-reviewed but not integration tested.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported or extended in this UOW: 4
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 42%

## Next Runtime UOW Candidates

1. Continue `CM_MOVE_ITEM` with the next smallest Java branch: partial auto-merge into one or more destination stacks followed by normal move of the remaining source stack.
2. Inspect Java `ItemMoveService.moveItem` target-storage full behavior and C# storage capacity state to determine whether a small live full-message/unlock UOW is safe.
3. Inspect `CM_SPLIT_ITEM` full-source merge/delete behavior against the updated merge persistence and live packet sequence, only if the next UOW changes live split handler state/packets.
