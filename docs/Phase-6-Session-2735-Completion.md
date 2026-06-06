# Phase 6 Session 2735 Completion

## UOW

[Phase 6] UOW-2735: Record legion item move history before full-storage denial.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: live `CM_MOVE_ITEM` involving legion warehouse now records Java's legion warehouse item history immediately after movement restrictions pass.
- Java source/runtime path: `ItemMoveService.moveItem` calls `LegionService.addWHItemHistory` after `ItemRestrictionService`/trading/shutdown checks and before auto-merge and full-storage handling.
- C# runtime artifact wired: `GameServerConnection.HandleMoveItemAsync` moved `AddLegionWarehouseItemHistoryAsync` to the Java-equivalent ordering point.
- Client-visible/state/persistence effect: a move into a full legion warehouse still sends `STR_WAREHOUSE_DEPOSIT_FULL_BASKET` and leaves item state unchanged, but now also persists Java's `ITEM_DEPOSIT` legion history row.
- Why this is runtime progress: it changes persisted live legion history from a live client packet handler and keeps the packet/state branch aligned with Java runtime ordering.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/item/ItemMoveService.java`
  - `moveItem` runs restrictions, trading, and shutdown checks.
  - It then calls `LegionService.getInstance().addWHItemHistory(...)` when either source or destination storage is `LEGION_WAREHOUSE`.
  - Only after that history call does it try auto-merge and then check `targetStorage.isFull()`.
- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
  - `addWHItemHistory` writes `ITEM_WITHDRAW` when the source storage is legion warehouse and `ITEM_DEPOSIT` when the destination is legion warehouse.
- `game-server/src/com/aionemu/gameserver/services/item/ItemSplitService.java`
  - Reviewed as a contrast: split records warehouse item history after the full-destination check, so this UOW intentionally changes only `CM_MOVE_ITEM`.

## C# Changes

- Moved `AddLegionWarehouseItemHistoryAsync` in `HandleMoveItemAsync` to run after restriction/trading/shutdown denial handling and before auto-merge/full-storage handling.
- Removed the later `moveHistoryRecorded` guard because Java records exactly once before the move mutation branches.
- Updated the full legion warehouse move test to assert the persisted `ITEM_DEPOSIT` history row while item state and packets remain unchanged.

## Tests Added / Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_CubeSourceMoveToFullLegionWarehouseSendsJavaFullMessageAndUnlocksSource` | Unit / live client packet dispatch | `ItemMoveService.moveItem` + `LegionService.addWHItemHistory` | Full legion warehouse destination still denies the move and unlocks the source item, but now persists Java's pre-full-check `ITEM_DEPOSIT` history. | Dispatches opcode `156` through `ProcessPacketAsync` and inspects packets, item state, repository calls, and inserted history. | Uses C# flattened location-3 model, not a full Java `LegionWarehouse`. |
| `ProcessPacketAsync_CubeSourceMovesItemToLegionWarehouseOwnerLikeJava` | Unit / live client packet dispatch | `ItemMoveService.moveItem` + `LegionService.addWHItemHistory` | Successful cube-to-legion move still records one `ITEM_DEPOSIT` row after the ordering change. | Focused validation kept adjacent success path green. | Does not prove all auto-merge edge cases. |
| `ProcessPacketAsync_LegionWarehouseSourceMovesItemToCubeHistoryLikeJava` | Unit / live client packet dispatch | `ItemMoveService.moveItem` + `LegionService.addWHItemHistory` | Successful legion-to-cube move still records one `ITEM_WITHDRAW` row after the ordering change. | Focused validation kept adjacent withdrawal path green. | Does not prove all persistence-failure branches. |

## Validation Decision

```text
- Changed surface: live inventory packet handler ordering and persisted legion history side effect.
- Specific behavior/contract: CM_MOVE_ITEM calls Java-equivalent legion warehouse item history before full-storage denial while preserving denial packets and item state.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_CubeSourceMovesItemToLegionWarehouseOwnerLikeJava|FullyQualifiedName~ProcessPacketAsync_LegionWarehouseSourceMovesItemToCubeHistoryLikeJava|FullyQualifiedName~ProcessPacketAsync_CubeSourceMoveToFullLegionWarehouseSendsJavaFullMessageAndUnlocksSource" --logger "console;verbosity=minimal"
- Result: passed; 3 tests passed.
- Focused Java/Maven command: not run; Java source was reviewed unchanged, and no narrow Java fixture exists for this branch in the checkout.
- Broad-validation trigger: live handler ordering/persistence side effect.
- Broad .NET decision: skipped; the focused command compiled the affected project and exercised the exact live move-history branches touched by this UOW.
- Why this scope is sufficient: the edited method is proven through live packet dispatch for deposit success, withdraw success, and the newly changed full-destination denial branch.
```

Repository hygiene:

```powershell
git diff --check
```

Result: passed with only Git CRLF working-copy warnings for touched C# files.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemMoveService.moveItem` | `GameServerConnection.HandleMoveItemAsync` | Live client packet handler branch | Partial | Unit Tested | Partial Parity | Legion warehouse item history now follows Java ordering before auto-merge/full-storage handling. Other move branches remain broader than this UOW. |
| `com.aionemu.gameserver.services.LegionService.addWHItemHistory` | `GameServerConnection.AddLegionWarehouseItemHistoryAsync` | Live persistence side effect | Partial | Unit Tested through live handler | Partial Parity | Deposit/withdraw descriptions and action names are covered through live move tests; full Java `Legion` aggregate behavior is not modeled. |
| `com.aionemu.gameserver.services.item.ItemSplitService.splitItem` | `GameServerConnection.HandleSplitItemAsync` | Live client packet handler branch | Partial | Unit Tested | Partial Parity | Reviewed to avoid applying move ordering to split; Java split records history after the full-storage check. |

## Known Gaps

- C# still models legion warehouse contents as player `InventoryItems` location `3`, not a full Java `LegionWarehouse`.
- Auto-merge into/out of legion warehouse now shares Java's pre-merge history ordering, but focused tests did not add a dedicated auto-merge assertion in this UOW.
- C# persistence-failure rollback can still differ from Java's in-memory dirty-state flow; this UOW aligns the normal live ordering and full-storage branch.
- Exact Java database row output was not captured; source review and focused live repository assertions are the evidence.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 3
- Total artifacts ported or extended in this UOW: 2
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 3
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 43%

## Next Runtime UOW Candidates

1. Fix same-storage legion warehouse slot persistence owner: Java `InventoryDAO.getItemOwnerId` uses legion id for `LEGION_WAREHOUSE`, while C# `PlayerEnterWorldService.SaveInventoryItemSlotAsync` currently uses player id except account warehouse.
2. Discover whether cross-storage switch persistence owner handling for legion warehouse is complete through `SaveItemStorageSwitchMutationAsync`; wire only if a concrete runtime gap exists.
3. Persist or restore Java-equivalent legion warehouse item state from live save/logout/load paths if discovery finds a concrete runtime gap in the current C# owner/location persistence.
