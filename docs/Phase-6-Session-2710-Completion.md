# Phase 6 Session 2710 Completion

## UOW

[Phase 6] UOW-2710: Merge account warehouse move stacks.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: live CM_MOVE_ITEM automatic placement now merges stackable items into existing account warehouse stacks before full-storage rejection.
- Java source/runtime path: ItemMoveService.moveItem slot == -1 branch -> targetStorage.getItemsByItemId -> ItemSplitService.mergeStacks -> Storage.increaseItemCount/decreaseItemCount -> InventoryDAO.store.
- C# runtime artifact wired: GameServerConnection.HandleMoveItemAsync auto-merge branch, Player.AccountWarehouseItems lookup, PlayerEnterWorldService.SaveItemMergeMutationAsync, and MySqlPlayerEnterWorldRepository.SaveItemMergeMutationAsync.
- Client-visible/state/persistence effect: existing account warehouse stack count increases, source cube stack decreases/deletes, Java merge update/delete packets are sent, and persistence uses the source/target OwnerId values.
- Why this is runtime progress: it changes live packet handling, runtime inventory/account warehouse state, packets, and persistence for a Java-supported move path previously skipped by C#.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/item/ItemMoveService.java`
  - When `slot == -1` and the item is stackable, Java iterates `targetStorage.getItemsByItemId(item.getItemId())` for any target storage, including account warehouse.
  - Java calls `ItemSplitService.mergeStacks` before checking `targetStorage.isFull()`.
  - If the source stack reaches zero after merge, Java returns without falling through to full-storage rejection.
- `game-server/src/com/aionemu/gameserver/services/item/ItemSplitService.java`
  - `mergeStacks` increases the target count with `INC_ITEM_COLLECT` for cross-storage merge and decreases the source with `DEC_ITEM_SPLIT_MOVE`.
- `game-server/src/com/aionemu/gameserver/dao/InventoryDAO.java`
  - Merge persistence updates/deletes rows by each item's effective owner; account warehouse target rows use account id.

## C# Changes

- Removed the account warehouse exclusion from `HandleMoveItemAsync` auto-merge.
- Existing `GetMoveStorageItems` now lets the auto-merge branch find restored `Player.AccountWarehouseItems`.
- Existing merge persistence now records source and target count changes by the already-correct runtime `OwnerId` values.
- The storage filler test helper now accepts a start object id so tests can fill storages without colliding with explicitly asserted stacks.

## Tests Added / Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleMoveItemAsync_AccountWarehouseAutoMergeFillsExistingStackBeforeFullCheckLikeJava` | Unit / live connection handler | `ItemMoveService.moveItem`, `ItemSplitService.mergeStacks`, `InventoryDAO.store` source review | Cube-to-account auto-placement fills an existing account warehouse stack before full-destination rejection, deletes the consumed source stack, sends Java merge/delete packet order, and persists source player-owned plus target account-owned rows. | Socket-backed handler fixture, decoded packets, runtime list/owner/count assertions, repository capture. | Partial-merge remainder and account-source-to-cube auto-merge are enabled by the same branch but not separately covered in this UOW. |

## Validation Decision

```text
- Changed surface: live CM_MOVE_ITEM auto-merge branch and focused test helper.
- Specific behavior/contract: Java auto-merges stackable slot == -1 moves into account warehouse target stacks before checking storage fullness, deletes the source if fully consumed, and sends target increase before source delete.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleMoveItemAsync_AccountWarehouseAutoMergeFillsExistingStackBeforeFullCheckLikeJava|FullyQualifiedName~HandleMoveItemAsync_CubeSourceMovesItemToAccountWarehouseOwnerLikeJava|FullyQualifiedName~HandleMoveItemAsync_FullAccountWarehouseDestinationSendsJavaStorageFullMessageAndUnlocksSource|FullyQualifiedName~HandleMoveItemAsync_StackableAutoSlotMergesDestinationStackLikeJava|FullyQualifiedName~HandleMoveItemAsync_PartialAutoSlotMergeMovesRemainingStackLikeJava" --logger "console;verbosity=minimal"
- Result: passed; 5 tests passed.
- Focused Java/Maven command: not run; Java source was reviewed unchanged, and no narrow Java fixture exists for this branch in this checkout.
- Broad-validation trigger: none. The change removes a storage-family exclusion from an existing live branch and validates account warehouse plus adjacent regular merge behavior.
- Broad .NET decision: skipped; the filtered command built the affected project and proved the new account warehouse merge path plus adjacent move/fullness regressions.
- Why this scope is sufficient: the new test exercises the exact Java branch that was skipped and asserts packet order, runtime state, and owner-aware persistence capture.
```

Repository hygiene:

```powershell
git diff --check
```

Result: passed with only Git CRLF working-copy warnings for touched files.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemMoveService.moveItem` | `GameServerConnection.HandleMoveItemAsync` | Service / live inventory mutation | Partial | Unit Tested | Partial Parity | Account warehouse target stacks now participate in auto-merge before fullness checks. Remaining auto-merge directions and regular restored warehouse list cleanup remain partial. |
| `com.aionemu.gameserver.services.item.ItemSplitService.mergeStacks` | `GameServerConnection.HandleMoveItemAsync` auto-merge branch | Service / stack mutation | Partial | Unit Tested | Partial Parity | Cross-storage merge packet/state behavior is covered for cube-to-account full consumption. Partial merge remainder and account-source merge directions need focused follow-up if code changes. |
| `com.aionemu.gameserver.dao.InventoryDAO.store/updateItems/getItemOwnerId` | `MySqlPlayerEnterWorldRepository.SaveItemMergeMutationAsync` | Repository / persistence | Partial | Unit Tested via repository capture | Partial Parity | Existing merge persistence uses each runtime item's OwnerId, so account warehouse target updates persist against account id. Broader dirty-item store parity remains incomplete. |
| `com.aionemu.gameserver.model.gameobjects.player.Player.getStorage` | `GameServerConnection.GetMoveStorageItems` | Runtime storage lookup | Partial | Unit Tested indirectly | Partial Parity | Auto-merge now reaches restored account warehouse rows. Regular warehouse restored-list parity remains unresolved because storage 1 still uses the flattened inventory list in this handler. |

## Known Gaps

- Regular warehouse restored-list parity remains inconsistent: Java `Player.getStorage(REGULAR_WAREHOUSE)` uses regular warehouse storage, while C# move/split paths still mostly use flattened `InventoryItems` for storage 1.
- Account warehouse partial auto-merge with a remaining source stack is enabled by this branch but not specifically tested with account warehouse destination.
- Account warehouse source-to-cube auto-merge is enabled by this branch but not specifically tested.
- Account warehouse split merge-into-existing-stack should be inspected before any runtime fix.
- Account warehouse kinah split/move still needs source review for owner/list/persistence behavior.
- Legion warehouse permissions/history, pet bags, and house storage remain deferred.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported or extended in this UOW: 4
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 42%

## Next Runtime UOW Candidates

1. Scope regular warehouse restored-list move parity for a narrow live path, such as moving a restored regular warehouse row to cube.
2. Inspect account warehouse split merge-into-existing-stack and proceed only if code, not just coverage, is missing.
3. Inspect account warehouse kinah split/move and fix any live owner/list/persistence mismatch found.
